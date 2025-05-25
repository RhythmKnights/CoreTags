package io.rhythmknights.coretags.component.modal;

import io.rhythmknights.coreframework.util.TextUtility;
import io.rhythmknights.coretags.CoreTags;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class CategoryModal {
    private final CoreTags plugin;
    private final TextUtility textUtility;
    private final Map<String, TagCategory> byKey = new HashMap<>();
    private final File ymlFile;

    public CategoryModal(CoreTags plugin) {
        this.plugin = plugin;
        this.textUtility = plugin.textUtility();
        this.ymlFile = new File(plugin.getDataFolder(), "components/categories.yml");
        plugin.saveResource("components/categories.yml", false);
        this.reload();
    }

    public void reload() {
        this.byKey.clear();
        YamlConfiguration yaml = new YamlConfiguration();

        try {
            yaml.load(this.ymlFile);
        } catch (IOException | InvalidConfigurationException e) {
            this.plugin.getLogger().severe("Failed to load components/categories.yml: " + e.getMessage());
            return;
        }

        ConfigurationSection root = yaml.getConfigurationSection("settings.categories");
        if (root == null) {
            this.plugin.getLogger().warning("No 'settings.categories' root found in categories.yml.");
            return;
        }

        for (String key : root.getKeys(false)) {
            ConfigurationSection categorySection = root.getConfigurationSection(key);
            if (categorySection != null) {
                this.loadCategory(key, categorySection);
            }
        }

        this.plugin.getLogger().info("Loaded " + this.byKey.size() + " tag categories.");
    }

    private void loadCategory(String key, ConfigurationSection section) {
        int slot = section.getInt("slot", -1);
        if (slot < -1 || slot > 53) {
            this.plugin.getLogger().warning("Invalid slot " + slot + " for category " + key + ", skipping.");
            return;
        }

        Material icon = this.parseMaterial(section.getString("material", "STONE"));
        
        // Parse display name and lore using TextUtility
        Component displayName = this.textUtility.parseText(section.getString("name", key));
        List<Component> lore = section.getStringList("lore").stream()
                .map(this.textUtility::parseText)
                .toList();

        String permission = section.getString("permission", "coretags.category." + key.toLowerCase(Locale.ROOT));
        boolean isProtected = section.getBoolean("protected", false);

        TagCategory category = new TagCategory(key, slot, icon, displayName, lore, permission, isProtected);
        this.byKey.put(key.toLowerCase(Locale.ROOT), category);
    }

    private Material parseMaterial(String rawMaterial) {
        try {
            return Material.valueOf(rawMaterial.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            this.plugin.getLogger().warning("Invalid material '" + rawMaterial + "' for category, using STONE as fallback");
            return Material.STONE;
        }
    }

    public Collection<TagCategory> all() {
        return Collections.unmodifiableCollection(this.byKey.values());
    }

    public Optional<TagCategory> byKey(String key) {
        return Optional.ofNullable(this.byKey.get(key.toLowerCase(Locale.ROOT)));
    }

    public record TagCategory(
            String key,
            int slot,
            Material icon,
            Component displayName,
            List<Component> lore,
            String permission,
            boolean isProtected
    ) {
    }
}