package io.rhythmknights.coretags.component.data.module;

import io.rhythmknights.coretags.CoreTags;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Level;

/**
 * Data module for handling system configuration (config.yml)
 * Manages all system-level settings and main plugin configuration
 */
public class SystemDataModule {
    
    private final CoreTags plugin;
    private YamlConfiguration config;
    private final File configFile;
    
    /**
     * Constructor for SystemDataModule
     * 
     * @param plugin The CoreTags plugin instance
     */
    public SystemDataModule(CoreTags plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
    }
    
    /**
     * Load the system configuration
     * 
     * @return true if loaded successfully
     */
    public boolean load() {
        try {
            if (!configFile.exists()) {
                plugin.getLogger().warning("System config file not found, creating defaults");
                createDefaultConfiguration();
            }
            
            config = YamlConfiguration.loadConfiguration(configFile);
            
            // Validate and set defaults for missing keys
            validateAndSetDefaults();
            
            plugin.getLogger().info("System configuration loaded successfully");
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load system configuration!", e);
            createDefaultConfiguration();
            return false;
        }
    }
    
    /**
     * Reload the system configuration
     * 
     * @return true if reloaded successfully
     */
    public boolean reload() {
        return load();
    }
    
    /**
     * Get the configuration
     * 
     * @return The system configuration
     */
    public YamlConfiguration getConfig() {
        return config;
    }
    
    /**
     * Create default system configuration
     */
    private void createDefaultConfiguration() {
        config = new YamlConfiguration();
        
        // System Settings
        config.set("settings.system.debug", false);
        config.set("settings.system.economy.enabled", false);
        config.set("settings.system.economy.default-cost", 0);
        config.set("settings.system.economy.convert-cost", true);
        config.set("settings.system.language", "GLOBAL");
        
        // Date/Time Format Settings
        config.set("settings.system.date-time-format.timezone.server", "EST");
        config.set("settings.system.date-time-format.timezone.display.type", "CODE");
        config.set("settings.system.date-time-format.timezone.display.format", "{continent}<dark_grey>•</dark_grey>{city}");
        config.set("settings.system.date-time-format.format.display", "STANDARD");
        config.set("settings.system.date-time-format.format.standard", "<grey>{day.dd}</grey><dark_grey>•</dark_grey><grey>{month.mm}</grey><dark_grey>•</dark_grey><grey>{year.yyyy}</grey> <grey>{hour.h}</grey><dark_grey>:</dark_grey><grey>{minute}{am.pm}</grey>");
        config.set("settings.system.date-time-format.format.expanded", "<grey>{weekday.text}</grey><dark_grey>,</dark_grey> <grey>{month.text}</grey> <grey>{day.text}</grey><dark_grey>,</dark_grey> <grey>{year.yyyy}</grey> <grey>at</grey> <grey>{hour.h}</grey><dark_grey>:</dark_grey><grey>{minute}{am.pm}</grey>");
        config.set("settings.system.date-time-format.localizer.default-type", "LOCAL");
        config.set("settings.system.date-time-format.localizer.player-toggle", true);
        config.set("settings.system.date-time-format.localizer.suffix.enabled", true);
        config.set("settings.system.date-time-format.localizer.suffix.display.type", "STATIC");
        config.set("settings.system.date-time-format.localizer.suffix.display.format.static", "(LOCAL TIME)");
        config.set("settings.system.date-time-format.localizer.suffix.display.format.dynamic", "({timezone})");
        
        // Modal Settings
        config.set("settings.system.modal.default", "CATEGORY");
        config.set("settings.system.modal.category.enabled", true);
        config.set("settings.system.modal.tags.default-tag", "DEFAULT");
        config.set("settings.system.modal.tags.sort-type", "ALPHABETICAL");
        config.set("settings.system.modal.tags.favorites.sort.enabled", true);
        config.set("settings.system.modal.tags.favorites.sort.default-sort", "SORTED");
        config.set("settings.system.modal.tags.category-sort-button", "THEME");
        config.set("settings.system.modal.tags.color-sort-button", "THEME");
        config.set("settings.system.modal.tags.close-on-activation", false);
        config.set("settings.system.modal.tags.close-on-unlock", false);
        config.set("settings.system.modal.tags.close-button-swap", true);
        config.set("settings.system.modal.tags.close-button-cmd", "{gui.close}");
        
        // Save the default configuration
        try {
            config.save(configFile);
            plugin.getLogger().info("Created default system configuration");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save default system configuration!", e);
        }
    }
    
    /**
     * Validate configuration and set defaults for missing keys
     */
    private void validateAndSetDefaults() {
        boolean changed = false;
        
        // Check and set missing system settings
        if (!config.contains("settings.system.debug")) {
            config.set("settings.system.debug", false);
            changed = true;
        }
        
        if (!config.contains("settings.system.economy.enabled")) {
            config.set("settings.system.economy.enabled", false);
            changed = true;
        }
        
        if (!config.contains("settings.system.economy.default-cost")) {
            config.set("settings.system.economy.default-cost", 0);
            changed = true;
        }
        
        if (!config.contains("settings.system.economy.convert-cost")) {
            config.set("settings.system.economy.convert-cost", true);
            changed = true;
        }
        
        if (!config.contains("settings.system.language")) {
            config.set("settings.system.language", "GLOBAL");
            changed = true;
        }
        
        // Check modal settings
        if (!config.contains("settings.system.modal.default")) {
            config.set("settings.system.modal.default", "CATEGORY");
            changed = true;
        }
        
        if (!config.contains("settings.system.modal.category.enabled")) {
            config.set("settings.system.modal.category.enabled", true);
            changed = true;
        }
        
        if (!config.contains("settings.system.modal.tags.default-tag")) {
            config.set("settings.system.modal.tags.default-tag", "DEFAULT");
            changed = true;
        }
        
        if (!config.contains("settings.system.modal.tags.sort-type")) {
            config.set("settings.system.modal.tags.sort-type", "ALPHABETICAL");
            changed = true;
        }
        
        if (!config.contains("settings.system.modal.tags.favorites.sort.enabled")) {
            config.set("settings.system.modal.tags.favorites.sort.enabled", true);
            changed = true;
        }
        
        if (!config.contains("settings.system.modal.tags.favorites.sort.default-sort")) {
            config.set("settings.system.modal.tags.favorites.sort.default-sort", "SORTED");
            changed = true;
        }
        
        if (!config.contains("settings.system.modal.tags.category-sort-button")) {
            config.set("settings.system.modal.tags.category-sort-button", "THEME");
            changed = true;
        }
        
        if (!config.contains("settings.system.modal.tags.color-sort-button")) {
            config.set("settings.system.modal.tags.color-sort-button", "THEME");
            changed = true;
        }
        
        if (!config.contains("settings.system.modal.tags.close-on-activation")) {
            config.set("settings.system.modal.tags.close-on-activation", false);
            changed = true;
        }
        
        if (!config.contains("settings.system.modal.tags.close-on-unlock")) {
            config.set("settings.system.modal.tags.close-on-unlock", false);
            changed = true;
        }
        
        if (!config.contains("settings.system.modal.tags.close-button-swap")) {
            config.set("settings.system.modal.tags.close-button-swap", true);
            changed = true;
        }
        
        if (!config.contains("settings.system.modal.tags.close-button-cmd")) {
            config.set("settings.system.modal.tags.close-button-cmd", "{gui.close}");
            changed = true;
        }
        
        // Check date-time format settings
        if (!config.contains("settings.system.date-time-format.timezone.server")) {
            config.set("settings.system.date-time-format.timezone.server", "EST");
            changed = true;
        }
        
        if (!config.contains("settings.system.date-time-format.timezone.display.type")) {
            config.set("settings.system.date-time-format.timezone.display.type", "CODE");
            changed = true;
        }
        
        if (!config.contains("settings.system.date-time-format.timezone.display.format")) {
            config.set("settings.system.date-time-format.timezone.display.format", "{continent}<dark_grey>•</dark_grey>{city}");
            changed = true;
        }
        
        if (!config.contains("settings.system.date-time-format.format.display")) {
            config.set("settings.system.date-time-format.format.display", "STANDARD");
            changed = true;
        }
        
        if (!config.contains("settings.system.date-time-format.format.standard")) {
            config.set("settings.system.date-time-format.format.standard", "<grey>{day.dd}</grey><dark_grey>•</dark_grey><grey>{month.mm}</grey><dark_grey>•</dark_grey><grey>{year.yyyy}</grey> <grey>{hour.h}</grey><dark_grey>:</dark_grey><grey>{minute}{am.pm}</grey>");
            changed = true;
        }
        
        if (!config.contains("settings.system.date-time-format.format.expanded")) {
            config.set("settings.system.date-time-format.format.expanded", "<grey>{weekday.text}</grey><dark_grey>,</dark_grey> <grey>{month.text}</grey> <grey>{day.text}</grey><dark_grey>,</dark_grey> <grey>{year.yyyy}</grey> <grey>at</grey> <grey>{hour.h}</grey><dark_grey>:</dark_grey><grey>{minute}{am.pm}</grey>");
            changed = true;
        }
        
        if (!config.contains("settings.system.date-time-format.localizer.default-type")) {
            config.set("settings.system.date-time-format.localizer.default-type", "LOCAL");
            changed = true;
        }
        
        if (!config.contains("settings.system.date-time-format.localizer.player-toggle")) {
            config.set("settings.system.date-time-format.localizer.player-toggle", true);
            changed = true;
        }
        
        if (!config.contains("settings.system.date-time-format.localizer.suffix.enabled")) {
            config.set("settings.system.date-time-format.localizer.suffix.enabled", true);
            changed = true;
        }
        
        if (!config.contains("settings.system.date-time-format.localizer.suffix.display.type")) {
            config.set("settings.system.date-time-format.localizer.suffix.display.type", "STATIC");
            changed = true;
        }
        
        if (!config.contains("settings.system.date-time-format.localizer.suffix.display.format.static")) {
            config.set("settings.system.date-time-format.localizer.suffix.display.format.static", "(LOCAL TIME)");
            changed = true;
        }
        
        if (!config.contains("settings.system.date-time-format.localizer.suffix.display.format.dynamic")) {
            config.set("settings.system.date-time-format.localizer.suffix.display.format.dynamic", "({timezone})");
            changed = true;
        }
        
        // Save changes if any were made
        if (changed) {
            try {
                config.save(configFile);
                plugin.getLogger().info("Updated system configuration with missing defaults");
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to save updated system configuration", e);
            }
        }
    }
    
    // Convenience methods for commonly accessed settings
    
    /**
     * Check if debug mode is enabled
     * 
     * @return true if debug is enabled
     */
    public boolean isDebugEnabled() {
        return config.getBoolean("settings.system.debug", false);
    }
    
    /**
     * Check if economy is enabled
     * 
     * @return true if economy is enabled
     */
    public boolean isEconomyEnabled() {
        return config.getBoolean("settings.system.economy.enabled", false);
    }
    
    /**
     * Get the default modal type
     * 
     * @return The default modal type
     */
    public String getDefaultModalType() {
        return config.getString("settings.system.modal.default", "CATEGORY");
    }
    
    /**
     * Check if categories are enabled
     * 
     * @return true if categories are enabled
     */
    public boolean areCategoriesEnabled() {
        return config.getBoolean("settings.system.modal.category.enabled", true);
    }
    
    /**
     * Get the default tag sort type
     * 
     * @return The sort type (ALPHABETICAL or NUMBERED)
     */
    public String getTagSortType() {
        return config.getString("settings.system.modal.tags.sort-type", "ALPHABETICAL");
    }
    
    /**
     * Check if modal should close on tag activation
     * 
     * @return true if should close on activation
     */
    public boolean shouldCloseOnActivation() {
        return config.getBoolean("settings.system.modal.tags.close-on-activation", false);
    }
    
    /**
     * Check if modal should close on tag unlock
     * 
     * @return true if should close on unlock
     */
    public boolean shouldCloseOnUnlock() {
        return config.getBoolean("settings.system.modal.tags.close-on-unlock", false);
    }
    
    /**
     * Check if back button should be swapped with close button
     * 
     * @return true if should swap buttons
     */
    public boolean shouldSwapCloseButton() {
        return config.getBoolean("settings.system.modal.tags.close-button-swap", true);
    }
    
    /**
     * Get the configured language
     * 
     * @return The language code
     */
    public String getLanguage() {
        return config.getString("settings.system.language", "GLOBAL");
    }
}