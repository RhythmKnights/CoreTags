package io.rhythmknights.coretags.component.data;

import io.rhythmknights.coretags.CoreTags;
import java.io.File;
import java.io.IOException;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.player.PlayerLoginProcessEvent;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

public final class PlayerDataModule implements Listener {
   private final CoreTags plugin;
   private final LuckPerms lp;
   private final File dataFile;
   private final YamlConfiguration yaml = new YamlConfiguration();
   private final Map<UUID, PlayerDataModule.PlayerData> cache = new ConcurrentHashMap();

   public PlayerDataModule(CoreTags plugin) {
      this.plugin = plugin;
      this.lp = plugin.luckPerms().api();
      File dir = new File(plugin.getDataFolder(), "playerdata");
      if (!dir.exists()) {
         dir.mkdirs();
      }

      this.dataFile = new File(dir, "players.yml");
      if (!this.dataFile.exists()) {
         try {
            this.dataFile.createNewFile();
         } catch (IOException var4) {
            plugin.getLogger().severe("Could not create players.yml");
         }
      }

      this.reload();
      Bukkit.getPluginManager().registerEvents(this, plugin);
      if (this.lp != null) {
         this.lp.getEventBus().subscribe(plugin, PlayerLoginProcessEvent.class, (e) -> {
            UUID uuid = e.getUser().getUniqueId();
            PlayerDataModule.PlayerData pd = this.loadIntoCache(uuid);
            this.mergeWithLuckPerms(uuid, pd, true);
            this.save();
         });
      }

   }

   public void reload() {
      try {
         this.yaml.load(this.dataFile);
      } catch (IOException | InvalidConfigurationException var2) {
         this.plugin.getLogger().severe("Could not load players.yml: " + var2.getMessage());
      }

      this.cache.clear();
      Bukkit.getOnlinePlayers().forEach((p) -> {
         PlayerDataModule.PlayerData pd = this.loadIntoCache(p.getUniqueId());
         this.mergeWithLuckPerms(p.getUniqueId(), pd, true);
      });
      this.save();
   }

   @EventHandler
   public void onJoin(final PlayerJoinEvent e) {
      (new BukkitRunnable() {
         public void run() {
            UUID uuid = e.getPlayer().getUniqueId();
            PlayerDataModule.PlayerData pd = PlayerDataModule.this.loadIntoCache(uuid);
            PlayerDataModule.this.mergeWithLuckPerms(uuid, pd, true);
            PlayerDataModule.this.save();
         }
      }).runTaskAsynchronously(this.plugin);
   }

   public PlayerDataModule.PlayerData get(UUID uuid) {
      return (PlayerDataModule.PlayerData)this.cache.computeIfAbsent(uuid, this::loadIntoCache);
   }

   public void unlockTag(UUID uuid, String id) {
      PlayerDataModule.PlayerData pd = this.get(uuid);
      if (pd.unlocked.add(id)) {
         this.setLpNode(uuid, id, true);
         this.markDirty(uuid);
      }

   }

   public void lockTag(UUID uuid, String id) {
      PlayerDataModule.PlayerData pd = this.get(uuid);
      if (pd.unlocked.remove(id)) {
         this.setLpNode(uuid, id, false);
         if (pd.active.equals(id)) {
            pd.active = "none";
         }

         pd.favorites.remove(id);
         this.markDirty(uuid);
      }

   }

   public void setActive(UUID uuid, String id) {
      PlayerDataModule.PlayerData pd = this.get(uuid);
      pd.active = id;
      this.markDirty(uuid);
   }

   public void toggleFavorite(UUID uuid, String id) {
      PlayerDataModule.PlayerData pd = this.get(uuid);
      if (!pd.favorites.remove(id)) {
         pd.favorites.add(id);
      }

      this.markDirty(uuid);
   }

   private PlayerDataModule.PlayerData loadIntoCache(UUID uuid) {
      ConfigurationSection sec = this.yaml.getConfigurationSection(uuid.toString());
      if (sec == null) {
         sec = this.yaml.createSection(uuid.toString());
      }

      PlayerDataModule.PlayerData pd = new PlayerDataModule.PlayerData(sec.getString("active", "none"), new HashSet(sec.getStringList("unlocked")), new HashSet(sec.getStringList("favorites")));
      this.cache.put(uuid, pd);
      return pd;
   }

   private void write(UUID uuid, PlayerDataModule.PlayerData pd) {
      String base = uuid.toString();
      this.yaml.set(base + ".active", pd.active);
      this.yaml.set(base + ".unlocked", new ArrayList(pd.unlocked));
      this.yaml.set(base + ".favorites", new ArrayList(pd.favorites));
   }

   private void save() {
      this.cache.forEach(this::write);

      try {
         this.yaml.save(this.dataFile);
      } catch (IOException var2) {
         this.plugin.getLogger().severe("Could not save players.yml");
      }

   }

   private void markDirty(UUID uuid) {
      this.write(uuid, (PlayerDataModule.PlayerData)this.cache.get(uuid));

      try {
         this.yaml.save(this.dataFile);
      } catch (IOException var3) {
      }

   }

   private void mergeWithLuckPerms(UUID uuid, PlayerDataModule.PlayerData pd, boolean syncLp) {
      if (this.lp != null) {
         User user = this.lp.getUserManager().getUser(uuid);
         if (user != null) {
            Set<String> lpUnlocked = (Set)user.getNodes().stream().filter((n) -> {
               return n.getKey().startsWith("coretags.tag.") && n.getValue();
            }).map((n) -> {
               return n.getKey().substring("coretags.tag.".length());
            }).collect(HashSet::new, HashSet::add, AbstractCollection::addAll);
            Set<String> auth = new HashSet(lpUnlocked);
            auth.addAll(pd.unlocked);
            if (!auth.equals(pd.unlocked)) {
               pd.unlocked.clear();
               pd.unlocked.addAll(auth);
               this.markDirty(uuid);
            }

            if (syncLp) {
               Iterator var7 = auth.iterator();

               String id;
               while(var7.hasNext()) {
                  id = (String)var7.next();
                  if (!lpUnlocked.contains(id)) {
                     this.setLpNode(uuid, id, true);
                  }
               }

               var7 = lpUnlocked.iterator();

               while(var7.hasNext()) {
                  id = (String)var7.next();
                  if (!auth.contains(id)) {
                     this.setLpNode(uuid, id, false);
                  }
               }
            }
         }
      }

   }

   private void setLpNode(UUID uuid, String id, boolean grant) {
      if (this.lp != null) {
         String key = "coretags.tag." + id;
         this.lp.getUserManager().modifyUser(uuid, (u) -> {
            if (grant) {
               u.data().add(Node.builder(key).value(true).build());
            } else {
               u.data().remove(Node.builder(key).build());
            }

         });
      }

   }

   public static final class PlayerData {
      public String active;
      public final Set<String> unlocked;
      public final Set<String> favorites;

      private PlayerData(String active, Set<String> unlocked, Set<String> fav) {
         this.active = active == null ? "none" : active.toLowerCase(Locale.ROOT);
         this.unlocked = unlocked;
         this.favorites = fav;
      }
   }
}
