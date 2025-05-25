package io.rhythmknights.coretags.component.data.module;

import io.rhythmknights.coretags.CoreTags;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Level;

/**
 * Data module for handling tags configuration (tags.yml)
 * Manages all tag definitions and their properties
 */
public class TagsModalDataModule {
    
    private final CoreTags plugin;
    private YamlConfiguration config;
    private final File configFile;
    
    /**
     * Constructor for TagsModalDataModule
     * 
     * @param plugin The CoreTags plugin instance
     */
    public TagsModalDataModule(CoreTags plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "components/tags.yml");
    }
    
    /**
     * Load the tags configuration
     * 
     * @return true if loaded successfully
     */
    public boolean load() {
        try {
            if (!configFile.exists()) {
                plugin.getLogger().warning("Tags config file not found, creating defaults");
                createDefaultConfiguration();
            }
            
            config = YamlConfiguration.loadConfiguration(configFile);
            
            // Validate and set defaults for missing keys
            validateAndSetDefaults();
            
            plugin.getLogger().info("Tags configuration loaded successfully");
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load tags configuration!", e);
            createDefaultConfiguration();
            return false;
        }
    }
    
    /**
     * Reload the tags configuration
     * 
     * @return true if reloaded successfully
     */
    public boolean reload() {
        return load();
    }
    
    /**
     * Get the configuration
     * 
     * @return The tags configuration
     */
    public YamlConfiguration getConfig() {
        return config;
    }
    
    /**
     * Create default tags configuration
     */
    private void createDefaultConfiguration() {
        config = new YamlConfiguration();
        
        // DEFAULT tag - special tag for default state
        config.set("tags.default.id", "DEFAULT");
        config.set("tags.default.name", "<grey>NONE</grey>");
        config.set("tags.default.display", ""); // Literally blank
        config.set("tags.default.material.id", "NAME_TAG");
        config.set("tags.default.material.material-modifier", -1);
        config.set("tags.default.material.custom-model-data", -1);
        config.set("tags.default.fav-material.id", "NAME_TAG");
        config.set("tags.default.fav-material.material-modifier", -1);
        config.set("tags.default.fav-material.custom-model-data", 9288);
        config.set("tags.default.permission", "coretags.group.player");
        config.set("tags.default.node", -1); // Cannot be selected
        config.set("tags.default.color", "MULTI");
        config.set("tags.default.protected", true);
        config.set("tags.default.lore", Arrays.asList(""));
        config.set("tags.default.sort-order", 0);
        config.set("tags.default.availablity.type", "ALWAYS");
        config.set("tags.default.availablity.timeframe.start", "");
        config.set("tags.default.availablity.timeframe.end", "");
        config.set("tags.default.cost", -1);
        
        // COSMICSTAR tag - example tag
        config.set("tags.cosmicstar.id", "COSMICSTAR");
        config.set("tags.cosmicstar.name", "<gold>Cosmic Star</gold>");
        config.set("tags.cosmicstar.display", "<white>✭</white>");
        config.set("tags.cosmicstar.material.id", "NAME_TAG");
        config.set("tags.cosmicstar.material.material-modifier", -1);
        config.set("tags.cosmicstar.material.custom-model-data", -1);
        config.set("tags.cosmicstar.fav-material.id", "NAME_TAG");
        config.set("tags.cosmicstar.fav-material.material-modifier", -1);
        config.set("tags.cosmicstar.fav-material.custom-model-data", 9288);
        config.set("tags.cosmicstar.permission", "coretags.tag.cosmicstar");
        config.set("tags.cosmicstar.node", "category.default");
        config.set("tags.cosmicstar.color", "MULTI");
        config.set("tags.cosmicstar.protected", false);
        config.set("tags.cosmicstar.lore", Arrays.asList(
            "<grey>This is a test tag.</grey>",
            ""
        ));
        config.set("tags.cosmicstar.sort-order", 1);
        config.set("tags.cosmicstar.availablity.type", "ALWAYS");
        config.set("tags.cosmicstar.availablity.timeframe.start", "12.01.2025-12:00AM");
        config.set("tags.cosmicstar.availablity.timeframe.end", "12.31.2025-11:59PM");
        config.set("tags.cosmicstar.cost", 0);
        
        // Save the default configuration
        try {
            // Ensure parent directory exists
            if (configFile.getParentFile() != null) {
                configFile.getParentFile().mkdirs();
            }
            
            config.save(configFile);
            plugin.getLogger().info("Created default tags configuration");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save default tags configuration!", e);
        }
    }
    
    /**
     * Validate configuration and set defaults for missing keys
     */
    private void validateAndSetDefaults() {
        boolean changed = false;
        
        // Ensure DEFAULT tag exists
        if (!config.contains("tags.default")) {
            config.set("tags.default.id", "DEFAULT");
            config.set("tags.default.name", "<grey>NONE</grey>");
            config.set("tags.default.display", "");
            config.set("tags.default.material.id", "NAME_TAG");
            config.set("tags.default.material.material-modifier", -1);
            config.set("tags.default.material.custom-model-data", -1);
            config.set("tags.default.fav-material.id", "NAME_TAG");
            config.set("tags.default.fav-material.material-modifier", -1);
            config.set("tags.default.fav-material.custom-model-data", 9288);
            config.set("tags.default.permission", "coretags.group.player");
            config.set("tags.default.node", -1);
            config.set("tags.default.color", "MULTI");
            config.set("tags.default.protected", true);
            config.set("tags.default.lore", Arrays.asList(""));
            config.set("tags.default.sort-order", 0);
            config.set("tags.default.availablity.type", "ALWAYS");
            config.set("tags.default.availablity.timeframe.start", "");
            config.set("tags.default.availablity.timeframe.end", "");
            config.set("tags.default.cost", -1);
            changed = true;
        }
        
        // Validate all existing tags have required properties
        if (config.contains("tags")) {
            for (String tagId : config.getConfigurationSection("tags").getKeys(false)) {
                String basePath = "tags." + tagId;
                
                if (!config.contains(basePath + ".id")) {
                    config.set(basePath + ".id", tagId.toUpperCase());
                    changed = true;
                }
                
                if (!config.contains(basePath + ".name")) {
                    config.set(basePath + ".name", "<grey>" + tagId.toUpperCase() + "</grey>");
                    changed = true;
                }
                
                if (!config.contains(basePath + ".display")) {
                    config.set(basePath + ".display", "[" + tagId.toUpperCase() + "]");
                    changed = true;
                }
                
                if (!config.contains(basePath + ".material.id")) {
                    config.set(basePath + ".material.id", "NAME_TAG");
                    changed = true;
                }
                
                if (!config.contains(basePath + ".material.material-modifier")) {
                    config.set(basePath + ".material.material-modifier", -1);
                    changed = true;
                }
                
                if (!config.contains(basePath + ".material.custom-model-data")) {
                    config.set(basePath + ".material.custom-model-data", -1);
                    changed = true;
                }
                
                if (!config.contains(basePath + ".fav-material.id")) {
                    config.set(basePath + ".fav-material.id", "NAME_TAG");
                    changed = true;
                }
                
                if (!config.contains(basePath + ".fav-material.material-modifier")) {
                    config.set(basePath + ".fav-material.material-modifier", -1);
                    changed = true;
                }
                
                if (!config.contains(basePath + ".fav-material.custom-model-data")) {
                    config.set(basePath + ".fav-material.custom-model-data", 9288);
                    changed = true;
                }
                
                if (!config.contains(basePath + ".permission")) {
                    config.set(basePath + ".permission", "coretags.tag." + tagId.toLowerCase());
                    changed = true;
                }
                
                if (!config.contains(basePath + ".node")) {
                    config.set(basePath + ".node", "category.default");
                    changed = true;
                }
                
                if (!config.contains(basePath + ".color")) {
                    config.set(basePath + ".color", "MULTI");
                    changed = true;
                }
                
                if (!config.contains(basePath + ".protected")) {
                    config.set(basePath + ".protected", false);
                    changed = true;
                }
                
                if (!config.contains(basePath + ".lore")) {
                    config.set(basePath + ".lore", Arrays.asList(
                        "<grey>This is a " + tagId.toLowerCase() + " tag.</grey>",
                        ""
                    ));
                    changed = true;
                }
                
                if (!config.contains(basePath + ".sort-order")) {
                    config.set(basePath + ".sort-order", 1);
                    changed = true;
                }
                
                if (!config.contains(basePath + ".availablity.type")) {
                    config.set(basePath + ".availablity.type", "ALWAYS");
                    changed = true;
                }
                
                if (!config.contains(basePath + ".availablity.timeframe.start")) {
                    config.set(basePath + ".availablity.timeframe.start", "");
                    changed = true;
                }
                
                if (!config.contains(basePath + ".availablity.timeframe.end")) {
                    config.set(basePath + ".availablity.timeframe.end", "");
                    changed = true;
                }
                
                if (!config.contains(basePath + ".cost")) {
                    config.set(basePath + ".cost", 0);
                    changed = true;
                }
            }
        }
        
        // Save changes if any were made
        if (changed) {
            try {
                config.save(configFile);
                plugin.getLogger().info("Updated tags configuration with missing defaults");
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to save updated tags configuration", e);
            }
        }
    }
    
    // Convenience methods for commonly accessed tag data
    
    /**
     * Check if a tag exists
     * 
     * @param tagId The tag ID to check
     * @return true if the tag exists
     */
    public boolean tagExists(String tagId) {
        return config.contains("tags." + tagId);
    }
    
    /**
     * Get a tag's display name
     * 
     * @param tagId The tag ID
     * @return The display name
     */
    public String getTagName(String tagId) {
        return config.getString("tags." + tagId + ".name", tagId.toUpperCase());
    }
    
    /**
     * Get a tag's display text (what shows in chat)
     * 
     * @param tagId The tag ID
     * @return The display text
     */
    public String getTagDisplay(String tagId) {
        return config.getString("tags." + tagId + ".display", "[" + tagId.toUpperCase() + "]");
    }
    
    /**
     * Get a tag's permission requirement
     * 
     * @param tagId The tag ID
     * @return The permission string
     */
    public String getTagPermission(String tagId) {
        return config.getString("tags." + tagId + ".permission", "coretags.tag." + tagId.toLowerCase());
    }
    
    /**
     * Check if a tag is protected
     * 
     * @param tagId The tag ID
     * @return true if the tag is protected
     */
    public boolean isTagProtected(String tagId) {
        return config.getBoolean("tags." + tagId + ".protected", false);
    }
    
    /**
     * Get a tag's category node
     * 
     * @param tagId The tag ID
     * @return The category node
     */
    public String getTagNode(String tagId) {
        return config.getString("tags." + tagId + ".node", "category.default");
    }
    
    /**
     * Get a tag's color
     * 
     * @param tagId The tag ID
     * @return The color name
     */
    public String getTagColor(String tagId) {
        return config.getString("tags." + tagId + ".color", "MULTI");
    }
    
    /**
     * Get a tag's material
     * 
     * @param tagId The tag ID
     * @return The material name
     */
    public String getTagMaterial(String tagId) {
        return config.getString("tags." + tagId + ".material.id", "NAME_TAG");
    }
    
    /**
     * Get a tag's favorite material
     * 
     * @param tagId The tag ID
     * @return The favorite material name
     */
    public String getTagFavoriteMaterial(String tagId) {
        return config.getString("tags." + tagId + ".fav-material.id", "NAME_TAG");
    }
    
    /**
     * Get a tag's custom model data
     * 
     * @param tagId The tag ID
     * @return The custom model data
     */
    public int getTagCustomModelData(String tagId) {
        return config.getInt("tags." + tagId + ".material.custom-model-data", -1);
    }
    
    /**
     * Get a tag's favorite custom model data
     * 
     * @param tagId The tag ID
     * @return The favorite custom model data
     */
    public int getTagFavoriteCustomModelData(String tagId) {
        return config.getInt("tags." + tagId + ".fav-material.custom-model-data", 9288);
    }
    
    /**
     * Get a tag's sort order
     * 
     * @param tagId The tag ID
     * @return The sort order
     */
    public int getTagSortOrder(String tagId) {
        return config.getInt("tags." + tagId + ".sort-order", 1);
    }
    
    /**
     * Get a tag's cost
     * 
     * @param tagId The tag ID
     * @return The cost (-1 means free)
     */
    public int getTagCost(String tagId) {
        return config.getInt("tags." + tagId + ".cost", 0);
    }
    
    /**
     * Get a tag's availability type
     * 
     * @param tagId The tag ID
     * @return The availability type (ALWAYS or TIMEFRAME)
     */
    public String getTagAvailabilityType(String tagId) {
        return config.getString("tags." + tagId + ".availablity.type", "ALWAYS");
    }
    
    /**
     * Get a tag's availability start time
     * 
     * @param tagId The tag ID
     * @return The start time string
     */
    public String getTagAvailabilityStart(String tagId) {
        return config.getString("tags." + tagId + ".availablity.timeframe.start", "");
    }
    
    /**
     * Get a tag's availability end time
     * 
     * @param tagId The tag ID
     * @return The end time string
     */
    public String getTagAvailabilityEnd(String tagId) {
        return config.getString("tags." + tagId + ".availablity.timeframe.end", "");
    }
    
    /**
     * Get a tag's lore
     * 
     * @param tagId The tag ID
     * @return The lore lines
     */
    public java.util.List<String> getTagLore(String tagId) {
        return config.getStringList("tags." + tagId + ".lore");
    }
    
    /**
     * Get all tag IDs
     * 
     * @return Set of all tag IDs
     */
    public java.util.Set<String> getAllTagIds() {
        if (config.contains("tags")) {
            return config.getConfigurationSection("tags").getKeys(false);
        }
        return new java.util.HashSet<>();
    }
    
    /**
     * Get all tags for a specific category
     * 
     * @param categoryNode The category node to filter by
     * @return Set of tag IDs in the category
     */
    public java.util.Set<String> getTagsInCategory(String categoryNode) {
        java.util.Set<String> tagsInCategory = new java.util.HashSet<>();
        
        if (config.contains("tags")) {
            for (String tagId : config.getConfigurationSection("tags").getKeys(false)) {
                String tagNode = getTagNode(tagId);
                if (categoryNode.equals(tagNode)) {
                    tagsInCategory.add(tagId);
                }
            }
        }
        
        return tagsInCategory;
    }
    
    /**
     * Check if a tag is available (time-based availability check)
     * 
     * @param tagId The tag ID
     * @return true if the tag is currently available
     */
    public boolean isTagAvailable(String tagId) {
        String availabilityType = getTagAvailabilityType(tagId);
        
        if ("ALWAYS".equalsIgnoreCase(availabilityType)) {
            return true;
        }
        
        // TODO: Implement time-based availability checking
        // For now, return true for TIMEFRAME tags
        return true;
    }
}