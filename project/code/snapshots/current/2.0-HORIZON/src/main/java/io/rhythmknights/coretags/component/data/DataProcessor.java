package io.rhythmknights.coretags.component.data;

import io.rhythmknights.coretags.CoreTags;
import io.rhythmknights.coretags.component.data.module.*;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.util.logging.Level;

/**
 * Handles all configuration file processing and delegation to specific data modules
 * Manages the lifecycle of configuration files from extraction to loading
 */
public class DataProcessor {
    
    private final CoreTags plugin;
    
    // Data modules
    private SystemDataModule systemDataModule;
    private CategoryModalDataModule categoryModalDataModule;
    private TagsModalDataModule tagsModalDataModule;
    private BaseModalDataModule baseModalDataModule;
    private LangDataModule langDataModule;
    
    // Consolidated configuration for easy access
    private YamlConfiguration consolidatedConfig;
    
    /**
     * Constructor for DataProcessor
     * 
     * @param plugin The CoreTags plugin instance
     */
    public DataProcessor(CoreTags plugin) {
        this.plugin = plugin;
        this.consolidatedConfig = new YamlConfiguration();
    }
    
    /**
     * Initialize the data processor - extract and load all configurations
     * 
     * @return true if initialization was successful
     */
    public boolean initialize() {
        try {
            // Ensure data folder exists
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            
            // Extract default configurations
            extractDefaultConfigurations();
            
            // Initialize data modules
            initializeDataModules();
            
            // Load all configurations
            loadAllConfigurations();
            
            // Build consolidated configuration
            buildConsolidatedConfig();
            
            plugin.getLogger().info("DataProcessor initialized successfully");
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize DataProcessor!", e);
            return false;
        }
    }
    
    /**
     * Reload all configurations
     * 
     * @return true if reload was successful
     */
    public boolean reload() {
        try {
            plugin.getLogger().info("Reloading all configurations...");
            
            // Reload all data modules
            if (!systemDataModule.reload() ||
                !categoryModalDataModule.reload() ||
                !tagsModalDataModule.reload() ||
                !baseModalDataModule.reload() ||
                !langDataModule.reload()) {
                
                plugin.getLogger().severe("One or more data modules failed to reload!");
                return false;
            }
            
            // Rebuild consolidated configuration
            buildConsolidatedConfig();
            
            plugin.getLogger().info("Configuration reload completed successfully");
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to reload configurations!", e);
            return false;
        }
    }
    
    /**
     * Extract all default configuration files from JAR
     */
    private void extractDefaultConfigurations() {
        plugin.getLogger().info("Starting configuration extraction...");
        
        // Create subdirectories if they don't exist
        File componentsDir = new File(plugin.getDataFolder(), "components");
        File languagesDir = new File(plugin.getDataFolder(), "languages");
        
        if (!componentsDir.exists()) {
            boolean created = componentsDir.mkdirs();
            plugin.getLogger().info("Created components directory: " + created);
        }
        if (!languagesDir.exists()) {
            boolean created = languagesDir.mkdirs();
            plugin.getLogger().info("Created languages directory: " + created);
        }
        
        // Define config files with their JAR paths and target paths
        String[][] configFiles = {
            {"config.yml", "config.yml"},                           // Root level file
            {"components/category.yml", "components/category.yml"}, // Subdirectory file
            {"components/tags.yml", "components/tags.yml"},         // Subdirectory file
            {"components/gui.yml", "components/gui.yml"},           // Subdirectory file
            {"languages/global.yml", "languages/global.yml"}       // Subdirectory file
        };
        
        for (String[] configPair : configFiles) {
            String jarPath = configPair[0];      // Path inside JAR
            String targetPath = configPair[1];   // Path in plugin data folder
            
            extractSingleResource(jarPath, targetPath);
        }
        
        plugin.getLogger().info("Configuration extraction completed.");
    }
    
    /**
     * Extract a single resource with enhanced error handling and debugging
     */
    private void extractSingleResource(String jarPath, String targetPath) {
        File targetFile = new File(plugin.getDataFolder(), targetPath);
        
        // Only extract if file doesn't exist
        if (targetFile.exists()) {
            plugin.getLogger().info("Configuration file already exists: " + targetPath);
            return;
        }
        
        // Check if resource exists in JAR
        try (InputStream resourceStream = plugin.getResource(jarPath)) {
            if (resourceStream == null) {
                plugin.getLogger().warning("Resource not found in JAR at path: " + jarPath);
                createDefaultConfigFile(targetPath);
                return;
            }
            
            // Try to save the resource using the JAR path
            try {
                plugin.saveResource(jarPath, false);
                plugin.getLogger().info("Successfully extracted: " + jarPath + " -> " + targetPath);
                
                // For subdirectory files, we need to move them to the correct location
                // because saveResource() puts them at the root level
                if (jarPath.contains("/")) {
                    File extractedFile = new File(plugin.getDataFolder(), jarPath.substring(jarPath.lastIndexOf('/') + 1));
                    if (extractedFile.exists() && !extractedFile.equals(targetFile)) {
                        // Move the file to the correct subdirectory
                        if (targetFile.getParentFile() != null) {
                            targetFile.getParentFile().mkdirs();
                        }
                        boolean moved = extractedFile.renameTo(targetFile);
                        plugin.getLogger().info("Moved " + extractedFile.getName() + " to " + targetPath + ": " + moved);
                    }
                }
                
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Failed to save resource " + jarPath + ": " + e.getMessage());
                createDefaultConfigFile(targetPath);
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error extracting resource " + jarPath + ": " + e.getMessage());
            e.printStackTrace();
            createDefaultConfigFile(targetPath);
        }
    }
    
    /**
     * Create a default config file when extraction fails
     */
    private void createDefaultConfigFile(String targetPath) {
        File targetFile = new File(plugin.getDataFolder(), targetPath);
        
        try {
            // Ensure parent directory exists
            if (targetFile.getParentFile() != null) {
                targetFile.getParentFile().mkdirs();
            }
            
            // Get just the filename for the switch statement
            String fileName = targetFile.getName();
            YamlConfiguration defaultConfig = createDefaultConfig(fileName);
            defaultConfig.save(targetFile);
            
            plugin.getLogger().info("Created default configuration file: " + targetPath);
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create default config file " + targetPath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Creates a minimal default configuration when files are missing
     * Each data module will handle its own comprehensive defaults
     * 
     * @param fileName The configuration file name
     * @return A minimal default configuration
     */
    private YamlConfiguration createDefaultConfig(String fileName) {
        YamlConfiguration config = new YamlConfiguration();
        
        switch (fileName) {
            case "config.yml":
                config.set("settings.system.debug", false);
                config.set("settings.system.modal.default", "CATEGORY");
                config.set("settings.system.modal.category.enabled", true);
                break;
            case "category.yml":
                config.set("category.all.id", "ALL");
                config.set("category.all.name", "<gold>ALL</gold>");
                config.set("category.all.permission", "coretags.group.player");
                config.set("category.all.node", "category.all");
                break;
            case "tags.yml":
                config.set("tags.default.id", "DEFAULT");
                config.set("tags.default.name", "<grey>NONE</grey>");
                config.set("tags.default.display", "");
                config.set("tags.default.permission", "coretags.group.player");
                break;
            case "gui.yml":
                config.set("gui.category-menu.rows", 4);
                config.set("gui.tags-menu.rows", 6);
                break;
            case "global.yml":
                config.set("settings.system.prefix.text", "<blue>CoreTags</blue>");
                config.set("settings.system.gui.titles.category-menu", "<gold>Tags</gold> <dark_grey>|</dark_grey> <gold>Categories</gold>");
                break;
        }
        
        plugin.getLogger().info("Created minimal default configuration for: " + fileName);
        return config;
    }
    
    /**
     * Initialize all data modules
     */
    private void initializeDataModules() {
        systemDataModule = new SystemDataModule(plugin);
        categoryModalDataModule = new CategoryModalDataModule(plugin);
        tagsModalDataModule = new TagsModalDataModule(plugin);
        baseModalDataModule = new BaseModalDataModule(plugin);
        langDataModule = new LangDataModule(plugin);
        
        plugin.getLogger().info("Data modules initialized");
    }
    
    /**
     * Load all configurations through their respective data modules
     */
    private void loadAllConfigurations() throws Exception {
        plugin.getLogger().info("Loading all configurations...");
        
        // Load each module - they handle their own error checking
        if (!systemDataModule.load()) {
            throw new Exception("Failed to load system configuration");
        }
        
        if (!categoryModalDataModule.load()) {
            throw new Exception("Failed to load category configuration");
        }
        
        if (!tagsModalDataModule.load()) {
            throw new Exception("Failed to load tags configuration");
        }
        
        if (!baseModalDataModule.load()) {
            throw new Exception("Failed to load GUI configuration");
        }
        
        if (!langDataModule.load()) {
            throw new Exception("Failed to load language configuration");
        }
        
        plugin.getLogger().info("All configurations loaded successfully");
    }
    
    /**
     * Build the consolidated configuration for easy access
     * Merges all module configurations into one accessible config
     */
    private void buildConsolidatedConfig() {
        consolidatedConfig = new YamlConfiguration();
        
        // Merge all configurations - language takes priority for labels
        mergeConfiguration(systemDataModule.getConfig());
        mergeConfiguration(categoryModalDataModule.getConfig());
        mergeConfiguration(tagsModalDataModule.getConfig());
        mergeConfiguration(baseModalDataModule.getConfig());
        mergeConfiguration(langDataModule.getConfig()); // Language last to override labels
        
        plugin.getLogger().info("Consolidated configuration built successfully");
    }
    
    /**
     * Merge a configuration into the consolidated config
     * 
     * @param config The configuration to merge
     */
    private void mergeConfiguration(YamlConfiguration config) {
        if (config == null) return;
        
        for (String key : config.getKeys(true)) {
            consolidatedConfig.set(key, config.get(key));
        }
    }
    
    // Getters for consolidated configuration (for backward compatibility)
    
    /**
     * Get the consolidated configuration
     * This provides access to all configuration values in one place
     * 
     * @return The consolidated configuration
     */
    public YamlConfiguration getConsolidatedConfig() {
        return consolidatedConfig;
    }
    
    // Getters for individual data modules
    
    /**
     * Get the system data module
     * 
     * @return The system data module
     */
    public SystemDataModule getSystemDataModule() {
        return systemDataModule;
    }
    
    /**
     * Get the category modal data module
     * 
     * @return The category modal data module
     */
    public CategoryModalDataModule getCategoryModalDataModule() {
        return categoryModalDataModule;
    }
    
    /**
     * Get the tags modal data module
     * 
     * @return The tags modal data module
     */
    public TagsModalDataModule getTagsModalDataModule() {
        return tagsModalDataModule;
    }
    
    /**
     * Get the base modal data module
     * 
     * @return The base modal data module
     */
    public BaseModalDataModule getBaseModalDataModule() {
        return baseModalDataModule;
    }
    
    /**
     * Get the language data module
     * 
     * @return The language data module
     */
    public LangDataModule getLangDataModule() {
        return langDataModule;
    }
    
    // Convenience getters for individual configurations (for direct access when needed)
    
    /**
     * Get the system configuration
     * 
     * @return The system configuration
     */
    public YamlConfiguration getSystemConfig() {
        return systemDataModule != null ? systemDataModule.getConfig() : null;
    }
    
    /**
     * Get the category configuration
     * 
     * @return The category configuration
     */
    public YamlConfiguration getCategoryConfig() {
        return categoryModalDataModule != null ? categoryModalDataModule.getConfig() : null;
    }
    
    /**
     * Get the tags configuration
     * 
     * @return The tags configuration
     */
    public YamlConfiguration getTagsConfig() {
        return tagsModalDataModule != null ? tagsModalDataModule.getConfig() : null;
    }
    
    /**
     * Get the GUI configuration
     * 
     * @return The GUI configuration
     */
    public YamlConfiguration getGuiConfig() {
        return baseModalDataModule != null ? baseModalDataModule.getConfig() : null;
    }
    
    /**
     * Get the language configuration
     * 
     * @return The language configuration
     */
    public YamlConfiguration getLangConfig() {
        return langDataModule != null ? langDataModule.getConfig() : null;
    }
    
    /**
     * Replace all variables in messages with their actual values
     * Centralized method for all variable replacement logic
     * @param message The message to process
     * @return The processed message
     */
    public String replaceVariables(String message) {
        if (message == null) return "";

        YamlConfiguration consolidatedConfig = getConsolidatedConfig();

        // Replace prefix variable
        String prefix = consolidatedConfig.getString("system.prefix.text", "<blue>CoreTags</blue>");
        message = message.replace("{prefix}", prefix);

        // Replace version variables
        String coreframeapiVersion = consolidatedConfig.getString("system.coreframeapi-version", "2.0-HORIZON");
        message = message.replace("{coreframeapiversion}", coreframeapiVersion);

        // Replace plugin version
        message = message.replace("{version}", plugin.getDescription().getVersion());

        // Replace simple placeholders first (these don't contain other placeholders)
        message = replaceSimplePlaceholders(message, consolidatedConfig);

        // Replace condition/label placeholders
        message = replaceLabelPlaceholders(message, consolidatedConfig);

        // Replace color placeholders
        message = replaceColorPlaceholders(message, consolidatedConfig);

        // Replace category placeholders
        message = replaceCategoryPlaceholders(message, consolidatedConfig);

        // Replace GUI control placeholders (these may contain other placeholders, so do them last)
        message = replaceControlPlaceholders(message, consolidatedConfig);

        // Do a second pass to resolve any nested placeholders
        message = replaceControlPlaceholders(message, consolidatedConfig);

        return message;
    }

    /**
     * Replace simple placeholders that don't contain other placeholders
     */
    private String replaceSimplePlaceholders(String message, YamlConfiguration config) {
        // Action labels (these are referenced by control placeholders)
        message = message.replace("{setactive}", 
            config.getString("settings.system.gui.labels.setactive", "<gold>[Set as active tag]</gold>"));
        message = message.replace("{togglefavorite}", 
            config.getString("settings.system.gui.labels.togglefavorite.add", "<gold>[Set favorite]</gold>"));
        message = message.replace("{unlocktag}", 
            config.getString("settings.system.gui.labels.unlocktag", "<gold>[Unlock tag]</gold>"));
        message = message.replace("{activefavorite}", 
            config.getString("settings.system.gui.labels.activefavorite", "<gold>[Set as active tag + favorite]</gold>"));

        // Pagination labels - TODO: Get actual values from modal context
        message = message.replace("{currentpage}", "1"); 
        message = message.replace("{totalpages}", "1");  
        message = message.replace("{activetag}", "None"); // TODO: Get actual active tag

        return message;
    }

    /**
     * Replace control placeholders like {gui.leftclick}, {chat.rightclick}, etc.
     */
    private String replaceControlPlaceholders(String message, YamlConfiguration config) {
        // Chat controls (simple ones first)
        message = message.replace("{chat.leftclick}", 
            config.getString("settings.system.controls.chat.leftclick", "<white>⸶</white> <#2ECC71>ʟᴇꜰᴛ ᴄʟɪᴄᴋ</#2ECC71>"));
        message = message.replace("{chat.rightclick}", 
            config.getString("settings.system.controls.chat.rightclick", "<white>⸷</white> <#2ECC71>ʀɪɢʜᴛ ᴄʟɪᴄᴋ</#2ECC71>"));
        message = message.replace("{chat.shiftleftclick}", 
            config.getString("settings.system.controls.chat.shiftleftclick", "<#2ECC71>ꜱʜɪꜰᴛ</#2ECC71> <grey>+</grey> <white>⸶</white> <#2ECC71>ʟᴇꜰᴛ ᴄʟɪᴄᴋ</#2ECC71>"));
        message = message.replace("{chat.shiftrightclick}", 
            config.getString("settings.system.controls.chat.shiftrightclick", "<#2ECC71>ꜱʜɪꜰᴛ</#2ECC71> <grey>+</grey> <white>⸷</white> <#2ECC71>ʀɪɢʜᴛ ᴄʟɪᴄᴋ</#2ECC71>"));

        // GUI controls (these contain action labels)
        message = message.replace("{gui.leftclick}", 
            config.getString("settings.system.controls.gui.leftclick", "<grey>⏵</grey> <white>⸶</white> <#2ECC71>ʟᴇꜰᴛ ᴄʟɪᴄᴋ</#2ECC71> • {setactive}"));
        message = message.replace("{gui.rightclick}", 
            config.getString("settings.system.controls.gui.rightclick", "<grey>⏵</grey> <white>⸷</white> <#2ECC71>ʀɪɢʜᴛ ᴄʟɪᴄᴋ</#2ECC71> • {togglefavorite}"));
        message = message.replace("{gui.shiftleftclick}", 
            config.getString("settings.system.controls.gui.shiftleftclick", "<grey>⏵</grey> <#2ECC71>ꜱʜɪꜰᴛ</#2ECC71> <grey>+</grey> <white>⸶</white> <#2ECC71>ʟᴇꜰᴛ ᴄʟɪᴄᴋ</#2ECC71> • {unlocktag}"));
        message = message.replace("{gui.shiftrightclick}", 
            config.getString("settings.system.controls.gui.shiftrightclick", "<grey>⏵</grey> <#2ECC71>ꜱʜɪꜰᴛ</#2ECC71> <grey>+</grey> <white>⸷</white> <#2ECC71>ʀɪɢʜᴛ ᴄʟɪᴄᴋ</#2ECC71> • {activefavorite}"));

        return message;
    }

    /**
     * Replace color placeholders like {colors.red}, {colors.blue}, etc.
     */
    private String replaceColorPlaceholders(String message, YamlConfiguration config) {
        message = message.replace("{colors.all}", 
            config.getString("settings.system.colors.all.text", "<gold>ALL</gold>"));
        message = message.replace("{colors.multi}", 
            config.getString("settings.system.colors.multi.text", "<blue>M</blue><green>U</green><yellow>L</yellow><red>T</red><light_purple>I</light_purple>"));
        message = message.replace("{colors.red}", 
            config.getString("settings.system.colors.red.text", "<red>RED</red>"));
        message = message.replace("{colors.green}", 
            config.getString("settings.system.colors.green.text", "<green>GREEN</green>"));
        message = message.replace("{colors.blue}", 
            config.getString("settings.system.colors.blue.text", "<blue>BLUE</blue>"));
        message = message.replace("{color}", 
            config.getString("settings.system.colors.multi.text", "<blue>M</blue><green>U</green><yellow>L</yellow><red>T</red><light_purple>I</light_purple>"));

        return message;
    }

    /**
     * Replace category placeholders like {category.default}, {category.premium}, etc.
     */
    private String replaceCategoryPlaceholders(String message, YamlConfiguration config) {
        // Look for {category.X} patterns and replace with category names
        if (message.contains("{category.")) {
            String[] parts = message.split("\\{category\\.");
            for (int i = 1; i < parts.length; i++) {
                int endIndex = parts[i].indexOf('}');
                if (endIndex > 0) {
                    String categoryId = parts[i].substring(0, endIndex);
                    String categoryName = config.getString("category." + categoryId + ".name", 
                        categoryId.toUpperCase());
                    message = message.replace("{category." + categoryId + "}", categoryName);
                }
            }
        }

        return message;
    }

    /**
     * Replace label/condition placeholders like {unlocked}, {locked}, etc.
     */
    private String replaceLabelPlaceholders(String message, YamlConfiguration config) {
        message = message.replace("{unlocked}", 
            config.getString("settings.system.conditions.unlocked", "<green>UNLOCKED</green>"));
        message = message.replace("{locked}", 
            config.getString("settings.system.conditions.locked", "<red>LOCKED</red>"));
        message = message.replace("{protected}", 
            config.getString("settings.system.conditions.protected", "<dark_purple>PROTECTED</dark_purple>"));
        message = message.replace("{favorite}", 
            config.getString("settings.system.conditions.favorite", "<aqua>FAVORITE</aqua>"));
        message = message.replace("{favorites}", 
            config.getString("settings.system.conditions.favorite", "<aqua>FAVORITE</aqua>"));
        message = message.replace("{active}", 
            config.getString("settings.system.conditions.active", "<gold>ACTIVE</gold>"));
        message = message.replace("{all}", 
            config.getString("settings.system.colors.all.text", "<gold>ALL</gold>"));

        return message;
    }
}