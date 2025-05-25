package io.rhythmknights.coretags.component.data.module;

import io.rhythmknights.coretags.CoreTags;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Level;

/**
 * Data module for handling base modal/GUI configuration (gui.yml)
 * Manages all GUI layout, materials, and display settings
 */
public class BaseModalDataModule {
    
    private final CoreTags plugin;
    private YamlConfiguration config;
    private final File configFile;
    
    /**
     * Constructor for BaseModalDataModule
     * 
     * @param plugin The CoreTags plugin instance
     */
    public BaseModalDataModule(CoreTags plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "components/gui.yml");
    }
    
    /**
     * Load the GUI configuration
     * 
     * @return true if loaded successfully
     */
    public boolean load() {
        try {
            if (!configFile.exists()) {
                plugin.getLogger().warning("GUI config file not found, creating defaults");
                createDefaultConfiguration();
            }
            
            config = YamlConfiguration.loadConfiguration(configFile);
            
            // Validate and set defaults for missing keys
            validateAndSetDefaults();
            
            plugin.getLogger().info("GUI configuration loaded successfully");
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load GUI configuration!", e);
            createDefaultConfiguration();
            return false;
        }
    }
    
    /**
     * Reload the GUI configuration
     * 
     * @return true if reloaded successfully
     */
    public boolean reload() {
        return load();
    }
    
    /**
     * Get the configuration
     * 
     * @return The GUI configuration
     */
    public YamlConfiguration getConfig() {
        return config;
    }
    
    /**
     * Create default GUI configuration
     */
    private void createDefaultConfiguration() {
        config = new YamlConfiguration();
        
        // Basic GUI settings
        config.set("gui.category-menu.rows", 4);
        config.set("gui.tags-menu.rows", 6);
        config.set("gui.tags-menu.favorited-icon.enabled", true);
        config.set("gui.tags-menu.favorited-icon.spaces", 12);
        
        // Tag slots configuration
        config.set("gui.tags.slots.row1", "10..16");
        config.set("gui.tags.slots.row2", "19..25");
        config.set("gui.tags.slots.row3", "28..34");
        config.set("gui.tags.slots.row4", "37..43");
        config.set("gui.tags.slots.row5", "");
        config.set("gui.tags.slots.row6", "");
        
        // Layout items - Empty slots
        config.set("gui.layout.items.empty-slot.category-menu.enabled", false);
        config.set("gui.layout.items.empty-slot.category-menu.slots", Arrays.asList(
            "0..7", "9", "17", "18..26", "28..34"
        ));
        config.set("gui.layout.items.empty-slot.tags-menu.enabled", false);
        config.set("gui.layout.items.empty-slot.tags-menu.slots", Arrays.asList(
            "1..7", "9", "17", "18", "26", "27", "35", "36", "44", "46", "47", "51", "52"
        ));
        config.set("gui.layout.items.empty-slot.material", "GRAY_STAINED_GLASS_PANE");
        config.set("gui.layout.items.empty-slot.material-modifier", -1);
        config.set("gui.layout.items.empty-slot.custom-model-data", -1);
        config.set("gui.layout.items.empty-slot.lore", false);
        config.set("gui.layout.items.empty-slot.glow", false);
        
        // Category sort button
        config.set("gui.layout.items.category-sort-button.enabled", true);
        config.set("gui.layout.items.category-sort-button.material", "GOLD_BLOCK");
        config.set("gui.layout.items.category-sort-button.material-modifier", -1);
        config.set("gui.layout.items.category-sort-button.custom-model-data", -1);
        config.set("gui.layout.items.category-sort-button.slot", 17);
        config.set("gui.layout.items.category-sort-button.lore", true);
        config.set("gui.layout.items.category-sort-button.glow", false);
        
        // Favorite sort button
        config.set("gui.layout.items.favorite-sort-button.enabled", true);
        config.set("gui.layout.items.favorite-sort-button.material", "NETHER_STAR");
        config.set("gui.layout.items.favorite-sort-button.material-modifier", -1);
        config.set("gui.layout.items.favorite-sort-button.custom-model-data", -1);
        config.set("gui.layout.items.favorite-sort-button.slot", 26);
        config.set("gui.layout.items.favorite-sort-button.lore", true);
        config.set("gui.layout.items.favorite-sort-button.glow", false);
        
        // Color sort button
        config.set("gui.layout.items.color-sort-button.enabled", true);
        config.set("gui.layout.items.color-sort-button.material", "BRUSH");
        config.set("gui.layout.items.color-sort-button.material-modifier", -1);
        config.set("gui.layout.items.color-sort-button.custom-model-data", -1);
        config.set("gui.layout.items.color-sort-button.slot", 35);
        config.set("gui.layout.items.color-sort-button.lore", true);
        config.set("gui.layout.items.color-sort-button.glow", false);
        
        // Navigation buttons
        config.set("gui.layout.items.last-page-button.enabled", true);
        config.set("gui.layout.items.last-page-button.material", "ARROW");
        config.set("gui.layout.items.last-page-button.material-modifier", -1);
        config.set("gui.layout.items.last-page-button.custom-model-data", -1);
        config.set("gui.layout.items.last-page-button.slot", 48);
        config.set("gui.layout.items.last-page-button.lore", false);
        config.set("gui.layout.items.last-page-button.glow", false);
        
        config.set("gui.layout.items.next-page-button.enabled", true);
        config.set("gui.layout.items.next-page-button.material", "ARROW");
        config.set("gui.layout.items.next-page-button.material-modifier", -1);
        config.set("gui.layout.items.next-page-button.custom-model-data", -1);
        config.set("gui.layout.items.next-page-button.slot", 50);
        config.set("gui.layout.items.next-page-button.lore", false);
        config.set("gui.layout.items.next-page-button.glow", false);
        
        // Utility buttons
        config.set("gui.layout.items.reset-tag-button.enabled", true);
        config.set("gui.layout.items.reset-tag-button.material", "RED_DYE");
        config.set("gui.layout.items.reset-tag-button.material-modifier", -1);
        config.set("gui.layout.items.reset-tag-button.custom-model-data", -1);
        config.set("gui.layout.items.reset-tag-button.slot.category-menu", 31);
        config.set("gui.layout.items.reset-tag-button.slot.tags-menu", 49);
        config.set("gui.layout.items.reset-tag-button.lore", true);
        config.set("gui.layout.items.reset-tag-button.glow", false);
        
        config.set("gui.layout.items.active-tag.enabled", true);
        config.set("gui.layout.items.active-tag.material", "NAME_TAG");
        config.set("gui.layout.items.active-tag.material-modifier", -1);
        config.set("gui.layout.items.active-tag.custom-model-data", -1);
        config.set("gui.layout.items.active-tag.slot.category-menu", 35);
        config.set("gui.layout.items.active-tag.slot.tags-menu", 53);
        config.set("gui.layout.items.active-tag.lore", true);
        config.set("gui.layout.items.active-tag.glow", true);
        
        // Control buttons
        config.set("gui.layout.items.back-button.enabled", true);
        config.set("gui.layout.items.back-button.material", "SPECTRAL_ARROW");
        config.set("gui.layout.items.back-button.material-modifier", -1);
        config.set("gui.layout.items.back-button.custom-model-data", -1);
        config.set("gui.layout.items.back-button.slot.category-menu", 27);
        config.set("gui.layout.items.back-button.slot.tags-menu", 45);
        config.set("gui.layout.items.back-button.lore", false);
        config.set("gui.layout.items.back-button.glow", false);
        
        config.set("gui.layout.items.close-button-material.material", "BARRIER");
        config.set("gui.layout.items.close-button-material.material-modifier", -1);
        config.set("gui.layout.items.close-button-material.custom-model-data", -1);
        config.set("gui.layout.items.close-button-material.slot.category-menu", 27);
        config.set("gui.layout.items.close-button-material.slot.tags-menu", 45);
        config.set("gui.layout.items.close-button-material.lore", false);
        config.set("gui.layout.items.close-button-material.glow", false);
        
        // Themed buttons for category sort
        createCategorySortThemedButtons();
        
        // Themed buttons for color sort
        createColorSortThemedButtons();
        
        // Save the default configuration
        try {
            // Ensure parent directory exists
            if (configFile.getParentFile() != null) {
                configFile.getParentFile().mkdirs();
            }
            
            config.save(configFile);
            plugin.getLogger().info("Created default GUI configuration");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save default GUI configuration!", e);
        }
    }
    
    /**
     * Create themed category sort button configurations
     */
    private void createCategorySortThemedButtons() {
        // Category themed buttons
        String basePath = "gui.layout.items.category-sort-button-themed.material";
        
        config.set(basePath + ".all.material", "BOOKSHELF");
        config.set(basePath + ".all.material-modifier", -1);
        config.set(basePath + ".all.custom-model-data", -1);
        config.set(basePath + ".all.lore", true);
        config.set(basePath + ".all.glow", false);
        
        config.set(basePath + ".favorites.material", "LIGHT_BLUE_SHULKER_BOX");
        config.set(basePath + ".favorites.material-modifier", -1);
        config.set(basePath + ".favorites.custom-model-data", -1);
        config.set(basePath + ".favorites.lore", true);
        config.set(basePath + ".favorites.glow", false);
        
        config.set(basePath + ".unlocked.material", "LIME_SHULKER_BOX");
        config.set(basePath + ".unlocked.material-modifier", -1);
        config.set(basePath + ".unlocked.custom-model-data", -1);
        config.set(basePath + ".unlocked.lore", true);
        config.set(basePath + ".unlocked.glow", false);
        
        config.set(basePath + ".locked.material", "RED_SHULKER_BOX");
        config.set(basePath + ".locked.material-modifier", -1);
        config.set(basePath + ".locked.custom-model-data", -1);
        config.set(basePath + ".locked.lore", true);
        config.set(basePath + ".locked.glow", false);
        
        config.set(basePath + ".protected.material", "PURPLE_SHULKER_BOX");
        config.set(basePath + ".protected.material-modifier", -1);
        config.set(basePath + ".protected.custom-model-data", -1);
        config.set(basePath + ".protected.lore", true);
        config.set(basePath + ".protected.glow", false);
        
        config.set(basePath + ".default.material", "LIGHT_GRAY_SHULKER_BOX");
        config.set(basePath + ".default.material-modifier", -1);
        config.set(basePath + ".default.custom-model-data", -1);
        config.set(basePath + ".default.lore", true);
        config.set(basePath + ".default.glow", false);
        
        config.set(basePath + ".premium.material", "YELLOW_SHULKER_BOX");
        config.set(basePath + ".premium.material-modifier", -1);
        config.set(basePath + ".premium.custom-model-data", -1);
        config.set(basePath + ".premium.lore", true);
        config.set(basePath + ".premium.glow", false);
        
        config.set(basePath + ".special.material", "BLUE_SHULKER_BOX");
        config.set(basePath + ".special.material-modifier", -1);
        config.set(basePath + ".special.custom-model-data", -1);
        config.set(basePath + ".special.lore", true);
        config.set(basePath + ".special.glow", false);
    }
    
    /**
     * Create themed color sort button configurations
     */
    private void createColorSortThemedButtons() {
        // Color themed buttons
        String basePath = "gui.layout.items.color-sort-button-themed.material";
        
        config.set(basePath + ".all.material", "BRUSH");
        config.set(basePath + ".all.material-modifier", -1);
        config.set(basePath + ".all.custom-model-data", -1);
        config.set(basePath + ".all.glow", false);
        
        config.set(basePath + ".multi.material", "TEST_BLOCK");
        config.set(basePath + ".multi.material-modifier", -1);
        config.set(basePath + ".multi.custom-model-data", -1);
        config.set(basePath + ".multi.glow", false);
        
        config.set(basePath + ".red.material", "RED_CONCRETE");
        config.set(basePath + ".red.material-modifier", -1);
        config.set(basePath + ".red.custom-model-data", -1);
        config.set(basePath + ".red.glow", false);
        
        config.set(basePath + ".orange.material", "ORANGE_CONCRETE");
        config.set(basePath + ".orange.material-modifier", -1);
        config.set(basePath + ".orange.custom-model-data", -1);
        config.set(basePath + ".orange.glow", false);
        
        config.set(basePath + ".yellow.material", "YELLOW_CONCRETE");
        config.set(basePath + ".yellow.material-modifier", -1);
        config.set(basePath + ".yellow.custom-model-data", -1);
        config.set(basePath + ".yellow.glow", false);
        
        config.set(basePath + ".green.material", "LIME_CONCRETE");
        config.set(basePath + ".green.material-modifier", -1);
        config.set(basePath + ".green.custom-model-data", -1);
        config.set(basePath + ".green.glow", false);
        
        config.set(basePath + ".blue.material", "LIGHT_BLUE_CONCRETE");
        config.set(basePath + ".blue.material-modifier", -1);
        config.set(basePath + ".blue.custom-model-data", -1);
        config.set(basePath + ".blue.glow", false);
        
        config.set(basePath + ".purple.material", "PURPLE_CONCRETE");
        config.set(basePath + ".purple.material-modifier", -1);
        config.set(basePath + ".purple.custom-model-data", -1);
        config.set(basePath + ".purple.glow", false);
        
        config.set(basePath + ".pink.material", "PINK_CONCRETE");
        config.set(basePath + ".pink.material-modifier", -1);
        config.set(basePath + ".pink.custom-model-data", -1);
        config.set(basePath + ".pink.glow", false);
        
        config.set(basePath + ".brown.material", "TERRACOTTA");
        config.set(basePath + ".brown.material-modifier", -1);
        config.set(basePath + ".brown.custom-model-data", -1);
        config.set(basePath + ".brown.glow", false);
        
        config.set(basePath + ".gray.material", "LIGHT_GRAY_CONCRETE");
        config.set(basePath + ".gray.material-modifier", -1);
        config.set(basePath + ".gray.custom-model-data", -1);
        config.set(basePath + ".gray.glow", false);
        
        config.set(basePath + ".black.material", "BLACK_CONCRETE");
        config.set(basePath + ".black.material-modifier", -1);
        config.set(basePath + ".black.custom-model-data", -1);
        config.set(basePath + ".black.glow", false);
        
        config.set(basePath + ".white.material", "WHITE_CONCRETE");
        config.set(basePath + ".white.material-modifier", -1);
        config.set(basePath + ".white.custom-model-data", -1);
        config.set(basePath + ".white.glow", false);
    }
    
    /**
     * Validate configuration and set defaults for missing keys
     */
    private void validateAndSetDefaults() {
        boolean changed = false;
        
        // Check basic GUI settings
        if (!config.contains("gui.category-menu.rows")) {
            config.set("gui.category-menu.rows", 4);
            changed = true;
        }
        
        if (!config.contains("gui.tags-menu.rows")) {
            config.set("gui.tags-menu.rows", 6);
            changed = true;
        }
        
        if (!config.contains("gui.tags-menu.favorited-icon.enabled")) {
            config.set("gui.tags-menu.favorited-icon.enabled", true);
            changed = true;
        }
        
        if (!config.contains("gui.tags-menu.favorited-icon.spaces")) {
            config.set("gui.tags-menu.favorited-icon.spaces", 12);
            changed = true;
        }
        
        // Check tag slots
        if (!config.contains("gui.tags.slots.row1")) {
            config.set("gui.tags.slots.row1", "10..16");
            changed = true;
        }
        
        if (!config.contains("gui.tags.slots.row2")) {
            config.set("gui.tags.slots.row2", "19..25");
            changed = true;
        }
        
        if (!config.contains("gui.tags.slots.row3")) {
            config.set("gui.tags.slots.row3", "28..34");
            changed = true;
        }
        
        if (!config.contains("gui.tags.slots.row4")) {
            config.set("gui.tags.slots.row4", "37..43");
            changed = true;
        }
        
        // Check essential layout items
        String[] essentialItems = {
            "empty-slot", "category-sort-button", "favorite-sort-button", "color-sort-button",
            "last-page-button", "next-page-button", "reset-tag-button", "active-tag",
            "back-button", "close-button-material"
        };
        
        for (String item : essentialItems) {
            String basePath = "gui.layout.items." + item;
            
            if (!config.contains(basePath + ".enabled") && !item.equals("close-button-material")) {
                config.set(basePath + ".enabled", true);
                changed = true;
            }
            
            if (!config.contains(basePath + ".material")) {
                String defaultMaterial = getDefaultMaterial(item);
                config.set(basePath + ".material", defaultMaterial);
                changed = true;
            }
            
            if (!config.contains(basePath + ".material-modifier")) {
                config.set(basePath + ".material-modifier", -1);
                changed = true;
            }
            
            if (!config.contains(basePath + ".custom-model-data")) {
                config.set(basePath + ".custom-model-data", -1);
                changed = true;
            }
            
            if (!config.contains(basePath + ".lore")) {
                config.set(basePath + ".lore", false);
                changed = true;
            }
            
            if (!config.contains(basePath + ".glow")) {
                config.set(basePath + ".glow", false);
                changed = true;
            }
        }
        
        // Save changes if any were made
        if (changed) {
            try {
                config.save(configFile);
                plugin.getLogger().info("Updated GUI configuration with missing defaults");
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to save updated GUI configuration", e);
            }
        }
    }
    
    /**
     * Get default material for a GUI item
     * 
     * @param itemType The item type
     * @return The default material name
     */
    private String getDefaultMaterial(String itemType) {
        switch (itemType) {
            case "empty-slot":
                return "GRAY_STAINED_GLASS_PANE";
            case "category-sort-button":
                return "GOLD_BLOCK";
            case "favorite-sort-button":
                return "NETHER_STAR";
            case "color-sort-button":
                return "BRUSH";
            case "last-page-button":
            case "next-page-button":
                return "ARROW";
            case "reset-tag-button":
                return "RED_DYE";
            case "active-tag":
                return "NAME_TAG";
            case "back-button":
                return "SPECTRAL_ARROW";
            case "close-button-material":
                return "BARRIER";
            default:
                return "STONE";
        }
    }
    
    // Convenience methods for commonly accessed GUI settings
    
    /**
     * Get the number of rows for category menu
     * 
     * @return The number of rows
     */
    public int getCategoryMenuRows() {
        return config.getInt("gui.category-menu.rows", 4);
    }
    
    /**
     * Get the number of rows for tags menu
     * 
     * @return The number of rows
     */
    public int getTagsMenuRows() {
        return config.getInt("gui.tags-menu.rows", 6);
    }
    
    /**
     * Check if favorited icon is enabled
     * 
     * @return true if favorited icon is enabled
     */
    public boolean isFavoritedIconEnabled() {
        return config.getBoolean("gui.tags-menu.favorited-icon.enabled", true);
    }
    
    /**
     * Get the number of spaces for favorited icon
     * 
     * @return The number of spaces
     */
    public int getFavoritedIconSpaces() {
        return config.getInt("gui.tags-menu.favorited-icon.spaces", 12);
    }
    
    /**
     * Check if empty slot filler is enabled for a menu
     * 
     * @param menuType The menu type (category-menu or tags-menu)
     * @return true if enabled
     */
    public boolean isEmptySlotFillerEnabled(String menuType) {
        return config.getBoolean("gui.layout.items.empty-slot." + menuType + ".enabled", false);
    }
    
    /**
     * Get empty slot filler material
     * 
     * @return The material name
     */
    public String getEmptySlotMaterial() {
        return config.getString("gui.layout.items.empty-slot.material", "GRAY_STAINED_GLASS_PANE");
    }
    
    /**
     * Get empty slot filler slots for a menu
     * 
     * @param menuType The menu type (category-menu or tags-menu)
     * @return List of slot ranges
     */
    public java.util.List<String> getEmptySlotSlots(String menuType) {
        return config.getStringList("gui.layout.items.empty-slot." + menuType + ".slots");
    }
    
    /**
     * Check if a GUI item is enabled
     * 
     * @param itemName The item name
     * @return true if enabled
     */
    public boolean isGuiItemEnabled(String itemName) {
        return config.getBoolean("gui.layout.items." + itemName + ".enabled", true);
    }
    
    /**
     * Get a GUI item's material
     * 
     * @param itemName The item name
     * @return The material name
     */
    public String getGuiItemMaterial(String itemName) {
        return config.getString("gui.layout.items." + itemName + ".material", "STONE");
    }
    
    /**
     * Get a GUI item's custom model data
     * 
     * @param itemName The item name
     * @return The custom model data
     */
    public int getGuiItemCustomModelData(String itemName) {
        return config.getInt("gui.layout.items." + itemName + ".custom-model-data", -1);
    }
    
    /**
     * Get a GUI item's slot for a specific menu
     * 
     * @param itemName The item name
     * @param menuType The menu type (category-menu or tags-menu)
     * @return The slot number
     */
    public int getGuiItemSlot(String itemName, String menuType) {
        return config.getInt("gui.layout.items." + itemName + ".slot." + menuType, -1);
    }
    
    /**
     * Get a GUI item's slot (for items with single slot)
     * 
     * @param itemName The item name
     * @return The slot number
     */
    public int getGuiItemSlot(String itemName) {
        return config.getInt("gui.layout.items." + itemName + ".slot", -1);
    }
    
    /**
     * Check if a GUI item should glow
     * 
     * @param itemName The item name
     * @return true if should glow
     */
    public boolean shouldGuiItemGlow(String itemName) {
        return config.getBoolean("gui.layout.items." + itemName + ".glow", false);
    }
    
    /**
     * Check if a GUI item should have lore
     * 
     * @param itemName The item name
     * @return true if should have lore
     */
    public boolean shouldGuiItemHaveLore(String itemName) {
        return config.getBoolean("gui.layout.items." + itemName + ".lore", false);
    }
    
    /**
     * Get tag slots for a specific row
     * 
     * @param rowNumber The row number (1-6)
     * @return The slot range string
     */
    public String getTagSlotRange(int rowNumber) {
        return config.getString("gui.tags.slots.row" + rowNumber, "");
    }
    
    /**
     * Get themed material for category sort button
     * 
     * @param categoryId The category ID
     * @return The material name
     */
    public String getCategorySortThemedMaterial(String categoryId) {
        return config.getString("gui.layout.items.category-sort-button-themed.material." + 
                               categoryId.toLowerCase() + ".material", "BOOKSHELF");
    }
    
    /**
     * Get themed material for color sort button
     * 
     * @param colorId The color ID
     * @return The material name
     */
    public String getColorSortThemedMaterial(String colorId) {
        return config.getString("gui.layout.items.color-sort-button-themed.material." + 
                               colorId.toLowerCase() + ".material", "BRUSH");
    }
    
    /**
     * Check if themed material should glow for category sort
     * 
     * @param categoryId The category ID
     * @return true if should glow
     */
    public boolean shouldCategorySortThemedGlow(String categoryId) {
        return config.getBoolean("gui.layout.items.category-sort-button-themed.material." + 
                                categoryId.toLowerCase() + ".glow", false);
    }
    
    /**
     * Check if themed material should glow for color sort
     * 
     * @param colorId The color ID
     * @return true if should glow
     */
    public boolean shouldColorSortThemedGlow(String colorId) {
        return config.getBoolean("gui.layout.items.color-sort-button-themed.material." + 
                                colorId.toLowerCase() + ".glow", false);
    }
    
    /**
     * Get themed custom model data for category sort button
     * 
     * @param categoryId The category ID
     * @return The custom model data
     */
    public int getCategorySortThemedCustomModelData(String categoryId) {
        return config.getInt("gui.layout.items.category-sort-button-themed.material." + 
                            categoryId.toLowerCase() + ".custom-model-data", -1);
    }
    
    /**
     * Get themed custom model data for color sort button
     * 
     * @param colorId The color ID
     * @return The custom model data
     */
    public int getColorSortThemedCustomModelData(String colorId) {
        return config.getInt("gui.layout.items.color-sort-button-themed.material." + 
                            colorId.toLowerCase() + ".custom-model-data", -1);
    }
}