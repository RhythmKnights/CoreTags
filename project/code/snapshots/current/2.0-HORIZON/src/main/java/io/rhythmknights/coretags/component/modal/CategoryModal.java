package io.rhythmknights.coretags.component.modal;

import io.rhythmknights.coretags.CoreTags;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
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

public final class CategoryModal {
   private final CoreTags plugin;
   private final Map<String, CategoryModal.TagCategory> byKey = new HashMap();
   private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
   private final File ymlFile;

   public CategoryModal(CoreTags plugin) {
      this.plugin = plugin;
      this.ymlFile = new File(plugin.getDataFolder(), "components/categories.yml");
      plugin.saveResource("components/categories.yml", false);
      this.reload();
   }

   public void reload() {
      this.byKey.clear();
      YamlConfiguration yaml = new YamlConfiguration();

      try {
         yaml.load(this.ymlFile);
      } catch (IOException | InvalidConfigurationException var12) {
         this.plugin.getLogger().severe("Failed to load components/categories.yml: " + var12.getMessage());
         return;
      }

      ConfigurationSection root = yaml.getConfigurationSection("settings.categories");
      if (root == null) {
         this.plugin.getLogger().warning("No 'settings.categories' root found in categories.yml.");
      } else {
         Iterator var3 = root.getKeys(false).iterator();

         while(var3.hasNext()) {
            String key = (String)var3.next();
            ConfigurationSection cs = root.getConfigurationSection(key);
            if (cs != null) {
               int slot = cs.getInt("slot", -1);
               if (slot >= 0 && slot <= 53) {
                  Material icon = this.parseMaterial(cs.getString("material", "STONE"));
                  Component displayName = LEGACY.deserialize(cs.getString("name", key));
                  List<Component> lore = cs.getStringList("lore").stream().map((line) -> {
                     return (Component) LEGACY.deserialize(line);
                  }).toList();
                  String perm = cs.getString("permission", "coretags.category." + key.toLowerCase(Locale.ROOT));
                  boolean isProtected = cs.getBoolean("protected", false);
                  this.byKey.put(key.toLowerCase(Locale.ROOT), new CategoryModal.TagCategory(key, slot, icon, displayName, lore, perm, isProtected));
               }
            }
         }

         this.plugin.getLogger().info("Loaded " + this.byKey.size() + " tag categories.");
      }

   }

   private Material parseMaterial(String raw) {
      try {
         return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException var3) {
         return Material.STONE;
      }
   }

   public Collection<CategoryModal.TagCategory> all() {
      return Collections.unmodifiableCollection(this.byKey.values());
   }

   public Optional<CategoryModal.TagCategory> byKey(String key) {
      return Optional.ofNullable((CategoryModal.TagCategory)this.byKey.get(key.toLowerCase(Locale.ROOT)));
   }

   public static record TagCategory(String key, int slot, Material icon, Component displayName, List<Component> lore, String permission, boolean isProtected) {
      public TagCategory(String key, int slot, Material icon, Component displayName, List<Component> lore, String permission, boolean isProtected) {
         this.key = key;
         this.slot = slot;
         this.icon = icon;
         this.displayName = displayName;
         this.lore = lore;
         this.permission = permission;
         this.isProtected = isProtected;
      }

      public String key() {
         return this.key;
      }

      public int slot() {
         return this.slot;
      }

      public Material icon() {
         return this.icon;
      }

      public Component displayName() {
         return this.displayName;
      }

      public List<Component> lore() {
         return this.lore;
      }

      public String permission() {
         return this.permission;
      }

      public boolean isProtected() {
         return this.isProtected;
      }
   }
}
