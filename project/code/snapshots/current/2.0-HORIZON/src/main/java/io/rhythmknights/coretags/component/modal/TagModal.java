package io.rhythmknights.coretags.component.modal;

import io.rhythmknights.coretags.CoreTags;
import io.rhythmknights.coretags.component.data.ConfigModule;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

public final class TagModal {
   private final CoreTags plugin;
   private final File file;
   private YamlConfiguration tagCfg;
   private final Map<String, TagModal.Tag> tagsByKey = new HashMap();
   private final Map<String, List<TagModal.Tag>> tagsByCategory = new HashMap();
   private final EnumMap<ConfigModule.GameState, String> statusMap = new EnumMap(ConfigModule.GameState.class);
   private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

   public TagModal(CoreTags plugin) {
      this.plugin = plugin;
      this.file = new File(plugin.getDataFolder(), "components/tags.yml");
      if (!this.file.exists()) {
         plugin.saveResource("components/tags.yml", false);
      }

      this.reload();
   }

   public void reload() {
      this.tagsByKey.clear();
      this.tagsByCategory.clear();
      this.statusMap.clear();
      this.tagCfg = new YamlConfiguration();

      try {
         this.tagCfg.load(this.file);
      } catch (IOException | InvalidConfigurationException var11) {
         this.plugin.getLogger().severe("Failed to load components/tags.yml: " + var11.getMessage());
         return;
      }

      ConfigurationSection statRoot = this.tagCfg.getConfigurationSection("settings.system.status");
      String id;
      if (statRoot != null) {
         ConfigModule.GameState[] var2 = ConfigModule.GameState.values();
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            ConfigModule.GameState gs = var2[var4];
            id = statRoot.getString(gs.name().toLowerCase(Locale.ROOT), "&f" + gs.name());
            this.statusMap.put(gs, LEGACY.serialize(LEGACY.deserialize(id)));
         }
      }

      ConfigurationSection root = this.tagCfg.getConfigurationSection("settings.tags");
      if (root == null) {
         this.plugin.getLogger().warning("No 'settings.tags' section in tags.yml!");
      } else {
         Iterator it = root.getKeys(false).iterator();

         while(it.hasNext()) {
            String key = (String)it.next();
            ConfigurationSection cs = root.getConfigurationSection(key);
            if (cs != null) {
               id = key.toLowerCase(Locale.ROOT);
               String cat = cs.getString("category", "default").replace("coretags.category.", "").toLowerCase(Locale.ROOT);
               String rawColor = cs.getString("color", "ALL").toUpperCase(Locale.ROOT);
               if (rawColor.equals("GREY")) {
                  rawColor = "GRAY";
               }

               List<Component> description = cs.getStringList("description").stream().map((s) -> {
                  return (Component) LEGACY.deserialize(s);
               }).toList();
               TagModal.Tag tag = new TagModal.Tag(id, cat, this.parseMat(cs.getString("material", "PAPER")), LEGACY.deserialize(cs.getString("name", key)), LEGACY.deserialize(cs.getString("display", "[" + key + "]")), description, Math.max(0, cs.getInt("cost", 0)), cs.getString("permission", "coretags.tag." + id), rawColor);
               this.tagsByKey.put(id, tag);
               ((List)this.tagsByCategory.computeIfAbsent(cat, (k) -> {
                  return new ArrayList();
               })).add(tag);
            }
         }

         this.tagsByCategory.values().forEach((list) -> {
            list.sort(Comparator.comparing(TagModal.Tag::id));
         });
         this.plugin.getLogger().info("Loaded " + this.tagsByKey.size() + " tags.");
      }

   }

   public YamlConfiguration rawConfig() {
      return this.tagCfg;
   }

   public String statusText(ConfigModule.GameState state) {
      return (String)this.statusMap.getOrDefault(state, state.name());
   }

   public Optional<TagModal.Tag> byId(String id) {
      return Optional.ofNullable((TagModal.Tag)this.tagsByKey.get(id.toLowerCase(Locale.ROOT)));
   }

   public List<TagModal.Tag> byCategory(String cat) {
      return (List)this.tagsByCategory.getOrDefault(cat.toLowerCase(Locale.ROOT), List.of());
   }

   public Collection<TagModal.Tag> all() {
      return this.tagsByKey.values();
   }

   private Material parseMat(String raw) {
      try {
         return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException var3) {
         return Material.PAPER;
      }
   }

   public static record Tag(String id, String category, Material icon, Component name, Component display, List<Component> description, int cost, String permission, String color) {
      public Tag(String id, String category, Material icon, Component name, Component display, List<Component> description, int cost, String permission, String color) {
         color = color.toUpperCase(Locale.ROOT).replace("GREY", "GRAY");
         this.id = id;
         this.category = category;
         this.icon = icon;
         this.name = name;
         this.display = display;
         this.description = description;
         this.cost = cost;
         this.permission = permission;
         this.color = color;
      }

      public String id() {
         return this.id;
      }

      public String category() {
         return this.category;
      }

      public Material icon() {
         return this.icon;
      }

      public Component name() {
         return this.name;
      }

      public Component display() {
         return this.display;
      }

      public List<Component> description() {
         return this.description;
      }

      public int cost() {
         return this.cost;
      }

      public String permission() {
         return this.permission;
      }

      public String color() {
         return this.color;
      }
   }
}
