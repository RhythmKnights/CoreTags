package io.rhythmknights.coretags.component.data.module;

import io.rhythmknights.coretags.CoreTags;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Level;

/**
 * Data module for handling category configuration (category.yml)
 * Manages all category definitions and their properties
 */
public class CategoryModalDataModule {
    
    private final CoreTags plugin;
    private YamlConfiguration config;
    private final File configFile;
    
    /**
     * Constructor for CategoryModalDataModule
     * 
     * @param plugin The CoreTags plugin instance
     */
    public CategoryModalDataModule(CoreTags plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "components/category.yml");
    }
    
    /**
     * Load the category configuration
     * 
     * @return true if loaded successfully
     */
    public boolean load() {
        try {
            if (!configFile.exists()) {
                plugin.getLogger().warning("Category config file not found, creating defaults");
                createDefaultConfiguration();
            }
            
            config = YamlConfiguration.loadConfiguration(configFile);
            
            // Validate and set defaults for missing keys
            validateAndSetDefaults();
            
            plugin.getLogger().info("Category configuration loaded successfully");
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load category configuration!", e);
            createDefaultConfiguration();
            return false;
        }
    }
    
    /**
     * Reload the category configuration
     * 
     * @return true if reloaded successfully
     */
    public boolean reload() {
        return load();
    }
    
    /**
     * Get the configuration
     * 
     * @return The category configuration
     */
    public YamlConfiguration getConfig() {
        return config;
    }
    
    /**
     * Create default category configuration
     */
    private void createDefaultConfiguration() {
        config = new YamlConfiguration();
        
        // ALL category - master list of all tags
        config.set("category.all.id", "ALL");
        config.set("category.all.name", "<gold>ALL</gold>");
        config.set("category.all.filter-name", "<gold>ALL</gold>");
        config.set("category.all.lore", Arrays.asList(
            "<grey>Display tags in</grey> {all} <grey>categories,</grey>",
            "<grey>including</grey> {favorites}<grey>,</grey> {unlocked}<grey>,</grey>",
            "{locked}<grey>,</grey> {protected}<grey>, and more.</grey>",
            ""
        ));
        config.set("category.all.permission", "coretags.group.player");
        config.set("category.all.node", "category.all");
        config.set("category.all.protected", false);
        config.set("category.all.material.id", "BOOKSHELF");
        config.set("category.all.material.material-modifier", -1);
        config.set("category.all.material.custom-model-data", -1);
        config.set("category.all.material.glow", false);
        config.set("category.all.material.slot", 10);
        config.set("category.all.sort-material.id", "HOPPER");
        config.set("category.all.sort-material.material-modifier", -1);
        config.set("category.all.sort-material.custom-model-data", -1);
        config.set("category.all.sort-material.glow", false);
        config.set("category.all.sort-material.sort-order", 1);
        
        // FAVORITES category - special list for favorited tags
        config.set("category.favorites.id", "FAVORITES");
        config.set("category.favorites.name", "<aqua>FAVORITES</aqua>");
        config.set("category.favorites.filter-name", "<aqua>FAVORITES</aqua>");
        config.set("category.favorites.lore", Arrays.asList(
            "<grey>Display tags marked as</grey> {favorite}<grey>.</grey>",
            ""
        ));
        config.set("category.favorites.permission", "coretags.group.player");
        config.set("category.favorites.node", "category.favorites");
        config.set("category.favorites.protected", false);
        config.set("category.favorites.material.id", "LIGHT_BLUE_SHULKER_BOX");
        config.set("category.favorites.material.material-modifier", -1);
        config.set("category.favorites.material.custom-model-data", -1);
        config.set("category.favorites.material.glow", false);
        config.set("category.favorites.material.slot", -1);
        config.set("category.favorites.sort-material.id", "LIGHT_BLUE_SHULKER_BOX");
        config.set("category.favorites.sort-material.material-modifier", -1);
        config.set("category.favorites.sort-material.custom-model-data", -1);
        config.set("category.favorites.sort-material.glow", false);
        config.set("category.favorites.sort-material.sort-order", 2);
        
        // UNLOCKED category - special list for unlocked tags
        config.set("category.unlocked.id", "UNLOCKED");
        config.set("category.unlocked.name", "<green>UNLOCKED</green>");
        config.set("category.unlocked.filter-name", "<green>UNLOCKED</green>");
        config.set("category.unlocked.lore", Arrays.asList(
            "<grey>Display</grey> {unlocked} <grey>tags.</grey>",
            ""
        ));
        config.set("category.unlocked.permission", "coretags.group.player");
        config.set("category.unlocked.node", "category.unlocked");
        config.set("category.unlocked.protected", false);
        config.set("category.unlocked.material.id", "LIME_SHULKER_BOX");
        config.set("category.unlocked.material.material-modifier", -1);
        config.set("category.unlocked.material.custom-model-data", -1);
        config.set("category.unlocked.material.glow", false);
        config.set("category.unlocked.material.slot", -1);
        config.set("category.unlocked.sort-material.id", "LIME_SHULKER_BOX");
        config.set("category.unlocked.sort-material.material-modifier", -1);
        config.set("category.unlocked.sort-material.custom-model-data", -1);
        config.set("category.unlocked.sort-material.glow", false);
        config.set("category.unlocked.sort-material.sort-order", 3);
        
        // LOCKED category - special list for locked tags
        config.set("category.locked.id", "LOCKED");
        config.set("category.locked.name", "<red>LOCKED</red>");
        config.set("category.locked.filter-name", "<red>LOCKED</red>");
        config.set("category.locked.lore", Arrays.asList(
            "<grey>Display</grey> {locked} <grey>tags.</grey>",
            ""
        ));
        config.set("category.locked.permission", "coretags.group.player");
        config.set("category.locked.node", "category.locked");
        config.set("category.locked.protected", false);
        config.set("category.locked.material.id", "RED_SHULKER_BOX");
        config.set("category.locked.material.material-modifier", -1);
        config.set("category.locked.material.custom-model-data", -1);
        config.set("category.locked.material.glow", false);
        config.set("category.locked.material.slot", -1);
        config.set("category.locked.sort-material.id", "RED_SHULKER_BOX");
        config.set("category.locked.sort-material.material-modifier", -1);
        config.set("category.locked.sort-material.custom-model-data", -1);
        config.set("category.locked.sort-material.glow", false);
        config.set("category.locked.sort-material.sort-order", 4);
        
        // PROTECTED category - special list for protected tags
        config.set("category.protected.id", "PROTECTED");
        config.set("category.protected.name", "<dark_purple>PROTECTED</dark_purple>");
        config.set("category.protected.filter-name", "<dark_purple>PROTECTED</dark_purple>");
        config.set("category.protected.lore", Arrays.asList(
            "<grey>Display</grey> {protected} <grey>tags.</grey>",
            "<red>(protected tags cannot be purchased)</red>",
            ""
        ));
        config.set("category.protected.permission", "coretags.group.player");
        config.set("category.protected.node", "category.protected");
        config.set("category.protected.protected", true);
        config.set("category.protected.material.id", "PURPLE_SHULKER_BOX");
        config.set("category.protected.material.material-modifier", -1);
        config.set("category.protected.material.custom-model-data", -1);
        config.set("category.protected.material.glow", false);
        config.set("category.protected.material.slot", -1);
        config.set("category.protected.sort-material.id", "PURPLE_SHULKER_BOX");
        config.set("category.protected.sort-material.material-modifier", -1);
        config.set("category.protected.sort-material.custom-model-data", -1);
        config.set("category.protected.sort-material.glow", false);
        config.set("category.protected.sort-material.sort-order", 5);
        
        // DEFAULT category - basic tag category
        config.set("category.default.id", "DEFAULT");
        config.set("category.default.name", "<grey>DEFAULT</grey>");
        config.set("category.default.filter-name", "<grey>DEFAULT</grey>");
        config.set("category.default.lore", Arrays.asList(
            "<grey>Display</grey> {category.default} <grey>tags.</grey>",
            ""
        ));
        config.set("category.default.permission", "coretags.category.default");
        config.set("category.default.node", "category.default");
        config.set("category.default.protected", false);
        config.set("category.default.material.id", "LIGHT_GRAY_SHULKER_BOX");
        config.set("category.default.material.material-modifier", -1);
        config.set("category.default.material.custom-model-data", -1);
        config.set("category.default.material.glow", false);
        config.set("category.default.material.slot", -1);
        config.set("category.default.sort-material.id", "LIGHT_GRAY_SHULKER_BOX");
        config.set("category.default.sort-material.material-modifier", -1);
        config.set("category.default.sort-material.custom-model-data", -1);
        config.set("category.default.sort-material.glow", false);
        config.set("category.default.sort-material.sort-order", 6);
        
        // PREMIUM category - premium tag category
        config.set("category.premium.id", "PREMIUM");
        config.set("category.premium.name", "<yellow>PREMIUM</yellow>");
        config.set("category.premium.filter-name", "<yellow>PREMIUM</yellow>");
        config.set("category.premium.lore", Arrays.asList(
            "<grey>Display</grey> {category.premium} <grey>tags.</grey>",
            ""
        ));
        config.set("category.premium.permission", "coretags.category.premium");
        config.set("category.premium.node", "category.premium");
        config.set("category.premium.protected", false);
        config.set("category.premium.material.id", "YELLOW_SHULKER_BOX");
        config.set("category.premium.material.material-modifier", -1);
        config.set("category.premium.material.custom-model-data", -1);
        config.set("category.premium.material.glow", false);
        config.set("category.premium.material.slot", -1);
        config.set("category.premium.sort-material.id", "YELLOW_SHULKER_BOX");
        config.set("category.premium.sort-material.material-modifier", -1);
        config.set("category.premium.sort-material.custom-model-data", -1);
        config.set("category.premium.sort-material.glow", false);
        config.set("category.premium.sort-material.sort-order", 7);
        
        // SPECIAL category - special tag category
        config.set("category.special.id", "SPECIAL");
        config.set("category.special.name", "<blue>SPECIAL</blue>");
        config.set("category.special.filter-name", "<blue>SPECIAL</blue>");
        config.set("category.special.lore", Arrays.asList(
            "<grey>Display</grey> {category.special} <grey>tags.</grey>",
            ""
        ));
        config.set("category.special.permission", "coretags.category.special");
        config.set("category.special.node", "category.special");
        config.set("category.special.protected", true);
        config.set("category.special.material.id", "BLUE_SHULKER_BOX");
        config.set("category.special.material.material-modifier", -1);
        config.set("category.special.material.custom-model-data", -1);
        config.set("category.special.material.glow", false);
        config.set("category.special.material.slot", -1);
        config.set("category.special.sort-material.id", "BLUE_SHULKER_BOX");
        config.set("category.special.sort-material.material-modifier", -1);
        config.set("category.special.sort-material.custom-model-data", -1);
        config.set("category.special.sort-material.glow", false);
        config.set("category.special.sort-material.sort-order", 8);
        
        // Save the default configuration
        try {
            // Ensure parent directory exists
            if (configFile.getParentFile() != null) {
                configFile.getParentFile().mkdirs();
            }
            
            config.save(configFile);
            plugin.getLogger().info("Created default category configuration");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save default category configuration!", e);
        }
    }
    
    /**
     * Validate configuration and set defaults for missing keys
     */
    private void validateAndSetDefaults() {
        boolean changed = false;
        
        // Check for essential categories
        String[] requiredCategories = {"all", "favorites", "unlocked", "locked", "protected", "default"};
        
        for (String categoryId : requiredCategories) {
            String basePath = "category." + categoryId;
            
            if (!config.contains(basePath + ".id")) {
                config.set(basePath + ".id", categoryId.toUpperCase());
                changed = true;
            }
            
            if (!config.contains(basePath + ".name")) {
                config.set(basePath + ".name", "<grey>" + categoryId.toUpperCase() + "</grey>");
                changed = true;
            }
            
            if (!config.contains(basePath + ".filter-name")) {
                config.set(basePath + ".filter-name", "<grey>" + categoryId.toUpperCase() + "</grey>");
                changed = true;
            }
            
            if (!config.contains(basePath + ".permission")) {
                config.set(basePath + ".permission", "coretags.group.player");
                changed = true;
            }
            
            if (!config.contains(basePath + ".node")) {
                config.set(basePath + ".node", "category." + categoryId);
                changed = true;
            }
            
            if (!config.contains(basePath + ".protected")) {
                config.set(basePath + ".protected", categoryId.equals("protected") || categoryId.equals("special"));
                changed = true;
            }
            
            if (!config.contains(basePath + ".material.id")) {
                config.set(basePath + ".material.id", "BOOKSHELF");
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
            
            if (!config.contains(basePath + ".material.glow")) {
                config.set(basePath + ".material.glow", false);
                changed = true;
            }
            
            if (!config.contains(basePath + ".material.slot")) {
                config.set(basePath + ".material.slot", categoryId.equals("all") ? 10 : -1);
                changed = true;
            }
            
            if (!config.contains(basePath + ".lore")) {
                config.set(basePath + ".lore", Arrays.asList(
                    "<grey>Display</grey> {" + categoryId + "} <grey>tags.</grey>",
                    ""
                ));
                changed = true;
            }
        }
        
        // Save changes if any were made
        if (changed) {
            try {
                config.save(configFile);
                plugin.getLogger().info("Updated category configuration with missing defaults");
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to save updated category configuration", e);
            }
        }
    }
    
    // Convenience methods for commonly accessed category data
    
    /**
     * Check if a category exists
     * 
     * @param categoryId The category ID to check
     * @return true if the category exists
     */
    public boolean categoryExists(String categoryId) {
        return config.contains("category." + categoryId);
    }
    
    /**
     * Get a category's display name
     * 
     * @param categoryId The category ID
     * @return The display name
     */
    public String getCategoryName(String categoryId) {
        return config.getString("category." + categoryId + ".name", categoryId.toUpperCase());
    }
    
    /**
     * Get a category's permission requirement
     * 
     * @param categoryId The category ID
     * @return The permission string
     */
    public String getCategoryPermission(String categoryId) {
        return config.getString("category." + categoryId + ".permission", "coretags.group.player");
    }
    
    /**
     * Check if a category is protected
     * 
     * @param categoryId The category ID
     * @return true if the category is protected
     */
    public boolean isCategoryProtected(String categoryId) {
        return config.getBoolean("category." + categoryId + ".protected", false);
    }
    
    /**
     * Get a category's node
     * 
     * @param categoryId The category ID
     * @return The category node
     */
    public String getCategoryNode(String categoryId) {
        return config.getString("category." + categoryId + ".node", "category." + categoryId);
    }
    
    /**
     * Get a category's material
     * 
     * @param categoryId The category ID
     * @return The material name
     */
    public String getCategoryMaterial(String categoryId) {
        return config.getString("category." + categoryId + ".material.id", "BOOKSHELF");
    }
    
    /**
     * Get a category's slot
     * 
     * @param categoryId The category ID
     * @return The slot number
     */
    public int getCategorySlot(String categoryId) {
        return config.getInt("category." + categoryId + ".material.slot", -1);
    }
    
    /**
     * Get a category's custom model data
     * 
     * @param categoryId The category ID
     * @return The custom model data
     */
    public int getCategoryCustomModelData(String categoryId) {
        return config.getInt("category." + categoryId + ".material.custom-model-data", -1);
    }
    
    /**
     * Check if a category should glow
     * 
     * @param categoryId The category ID
     * @return true if should glow
     */
    public boolean shouldCategoryGlow(String categoryId) {
        return config.getBoolean("category." + categoryId + ".material.glow", false);
    }
    
    /**
     * Get all category IDs
     * 
     * @return Set of all category IDs
     */
    public java.util.Set<String> getAllCategoryIds() {
        if (config.contains("category")) {
            return config.getConfigurationSection("category").getKeys(false);
        }
        return new java.util.HashSet<>();
    }
}