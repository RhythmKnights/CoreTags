package io.rhythmknights.coretags.component.modal;

import io.rhythmknights.coreframework.util.TextUtility;
import io.rhythmknights.coretags.CoreTags;
import io.rhythmknights.coretags.component.data.ConfigModule;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class TagModal {
    private final CoreTags plugin;
    private final TextUtility textUtility;
    private final File file;
    private YamlConfiguration tagCfg;
    private final Map<String, Tag> tagsByKey = new HashMap<>();
    private final Map<String, List<Tag>> tagsByCategory = new HashMap<>();
    private final EnumMap<ConfigModule.GameState, String> statusMap = new EnumMap<>(ConfigModule.GameState.class);

    public TagModal(CoreTags plugin) {
        this.plugin = plugin;
        this.textUtility = plugin.textUtility();
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
        } catch (IOException | InvalidConfigurationException e) {
            this.plugin.getLogger().severe("Failed to load components/tags.yml: " + e.getMessage());
            return;
        }

        this.loadStatusMap();
        this.loadTags();
        
        // Sort tags within each category
        this.tagsByCategory.values().forEach(list -> 
            list.sort(Comparator.comparing(Tag::id)));
        
        this.plugin.getLogger().info("Loaded " + this.tagsByKey.size() + " tags.");
    }

    private void loadStatusMap() {
        ConfigurationSection statusRoot = this.tagCfg.getConfigurationSection("settings.system.status");
        if (statusRoot != null) {
            for (ConfigModule.GameState gameState : ConfigModule.GameState.values()) {
                String statusText = statusRoot.getString(gameState.name().toLowerCase(Locale.ROOT), "&f" + gameState.name());
                // Parse the status text through TextUtility and then convert back to plain text for storage
                Component parsedComponent = this.textUtility.parseText(statusText);
                this.statusMap.put(gameState, this.textUtility.componentToPlainText(parsedComponent));
            }
        }
    }

    private void loadTags() {
        ConfigurationSection root = this.tagCfg.getConfigurationSection("settings.tags");
        if (root == null) {
            this.plugin.getLogger().warning("No 'settings.tags' section in tags.yml!");
            return;
        }

        for (String key : root.getKeys(false)) {
            ConfigurationSection tagSection = root.getConfigurationSection(key);
            if (tagSection != null) {
                this.loadTag(key, tagSection);
            }
        }
    }

    private void loadTag(String key, ConfigurationSection section) {
        String id = key.toLowerCase(Locale.ROOT);
        String category = section.getString("category", "default")
                .replace("coretags.category.", "")
                .toLowerCase(Locale.ROOT);
        
        String rawColor = section.getString("color", "ALL").toUpperCase(Locale.ROOT);
        if (rawColor.equals("GREY")) {
            rawColor = "GRAY";
        }

        // Parse components using TextUtility
        Component name = this.textUtility.parseText(section.getString("name", key));
        Component display = this.textUtility.parseText(section.getString("display", "[" + key + "]"));
        
        List<Component> description = section.getStringList("description").stream()
                .map(this.textUtility::parseText)
                .toList();

        Material icon = this.parseMaterial(section.getString("material", "PAPER"));
        int cost = Math.max(0, section.getInt("cost", 0));
        String permission = section.getString("permission", "coretags.tag." + id);

        Tag tag = new Tag(id, category, icon, name, display, description, cost, permission, rawColor);
        this.tagsByKey.put(id, tag);
        this.tagsByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(tag);
    }

    private Material parseMaterial(String rawMaterial) {
        try {
            return Material.valueOf(rawMaterial.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            this.plugin.getLogger().warning("Invalid material '" + rawMaterial + "', using PAPER as fallback");
            return Material.PAPER;
        }
    }

    public YamlConfiguration rawConfig() {
        return this.tagCfg;
    }

    public String statusText(ConfigModule.GameState state) {
        return this.statusMap.getOrDefault(state, state.name());
    }

    public Optional<Tag> byId(String id) {
        return Optional.ofNullable(this.tagsByKey.get(id.toLowerCase(Locale.ROOT)));
    }

    public List<Tag> byCategory(String category) {
        return this.tagsByCategory.getOrDefault(category.toLowerCase(Locale.ROOT), List.of());
    }

    public Collection<Tag> all() {
        return this.tagsByKey.values();
    }

    public record Tag(
            String id,
            String category,
            Material icon,
            Component name,
            Component display,
            List<Component> description,
            int cost,
            String permission,
            String color
    ) {
        public Tag {
            // Normalize color
            color = color.toUpperCase(Locale.ROOT).replace("GREY", "GRAY");
        }
    }
}