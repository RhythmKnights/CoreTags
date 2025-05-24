package io.rhythmknights.coretags;

import io.rhythmknights.coreframework.CoreFramework;
import io.rhythmknights.coreframework.component.api.hook.HookRequirement;
import io.rhythmknights.coreframework.component.api.plugin.RegisteredPlugin;
import io.rhythmknights.coreframework.component.utility.TextUtility;
import io.rhythmknights.coretags.component.command.CoreTagsCommand;
import io.rhythmknights.coretags.component.hook.CoreTagsHookProcessor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

/**
 * Main class for the CoreTags plugin.
 * Handles plugin initialization, configuration loading, and CoreFramework integration.
 */
public class CoreTags extends JavaPlugin {
    private static CoreTags instance;
    private YamlConfiguration internalConfig;
    private YamlConfiguration categoryConfig;
    private YamlConfiguration tagsConfig;
    private YamlConfiguration guiConfig;
    private YamlConfiguration globalLangConfig;
    
    private CoreTagsHookProcessor hookProcessor;
    private RegisteredPlugin registeredPlugin;
    private CoreTagsCommand commandHandler;
    private boolean enabled = false;
    
    /**
     * Default constructor for CoreTags
     * Called by Bukkit when loading the plugin
     */
    public CoreTags() {
        super();
    }
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Ensure data folder exists and extract default configs
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        
        // Extract default configuration files from JAR to plugin folder
        extractDefaultConfigs();
        
        // Load all configurations
        try {
            loadConfigurations();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to load configurations!", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Check if CoreFramework plugin is loaded
        Plugin coreFrameworkPlugin = Bukkit.getPluginManager().getPlugin("CoreFramework");
        if (coreFrameworkPlugin == null) {
            getLogger().severe("CoreFramework plugin is not loaded! CoreTags requires CoreFramework to function.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        if (!coreFrameworkPlugin.isEnabled()) {
            getLogger().severe("CoreFramework plugin is not enabled! CoreTags requires CoreFramework to be enabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Register with CoreFramework using direct method
        if (registerWithCoreFramework()) {
            // Registration successful, continue with initialization
            completeInitialization();
        } else {
            // Registration failed
            getLogger().severe("Failed to register with CoreFramework - disabling plugin!");
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    /**
     * Extracts default configuration files from the JAR to the plugin data folder
     */
    private void extractDefaultConfigs() {
        getLogger().info("Starting configuration extraction...");
        
        // Create subdirectories if they don't exist
        File componentsDir = new File(getDataFolder(), "components");
        File languagesDir = new File(getDataFolder(), "languages");
        
        if (!componentsDir.exists()) {
            boolean created = componentsDir.mkdirs();
            getLogger().info("Created components directory: " + created);
        }
        if (!languagesDir.exists()) {
            boolean created = languagesDir.mkdirs();
            getLogger().info("Created languages directory: " + created);
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
        
        getLogger().info("Configuration extraction completed.");
    }

    /**
     * Extract a single resource with enhanced error handling and debugging
     */
    private void extractSingleResource(String jarPath, String targetPath) {
        File targetFile = new File(getDataFolder(), targetPath);
        
        // Only extract if file doesn't exist
        if (targetFile.exists()) {
            getLogger().info("Configuration file already exists: " + targetPath);
            return;
        }
        
        // Check if resource exists in JAR
        try (InputStream resourceStream = getResource(jarPath)) {
            if (resourceStream == null) {
                getLogger().warning("Resource not found in JAR at path: " + jarPath);
                createDefaultConfigFile(targetPath);
                return;
            }
            
            // Try to save the resource using the JAR path
            try {
                saveResource(jarPath, false);
                getLogger().info("Successfully extracted: " + jarPath + " -> " + targetPath);
                
                // For subdirectory files, we need to move them to the correct location
                // because saveResource() puts them at the root level
                if (jarPath.contains("/")) {
                    File extractedFile = new File(getDataFolder(), jarPath.substring(jarPath.lastIndexOf('/') + 1));
                    if (extractedFile.exists() && !extractedFile.equals(targetFile)) {
                        // Move the file to the correct subdirectory
                        if (targetFile.getParentFile() != null) {
                            targetFile.getParentFile().mkdirs();
                        }
                        boolean moved = extractedFile.renameTo(targetFile);
                        getLogger().info("Moved " + extractedFile.getName() + " to " + targetPath + ": " + moved);
                    }
                }
                
            } catch (IllegalArgumentException e) {
                getLogger().warning("Failed to save resource " + jarPath + ": " + e.getMessage());
                createDefaultConfigFile(targetPath);
            }
            
        } catch (Exception e) {
            getLogger().severe("Error extracting resource " + jarPath + ": " + e.getMessage());
            e.printStackTrace();
            createDefaultConfigFile(targetPath);
        }
    }

    /**
     * Create a default config file when extraction fails
     */
    private void createDefaultConfigFile(String targetPath) {
        File targetFile = new File(getDataFolder(), targetPath);
        
        try {
            // Ensure parent directory exists
            if (targetFile.getParentFile() != null) {
                targetFile.getParentFile().mkdirs();
            }
            
            // Get just the filename for the switch statement
            String fileName = targetFile.getName();
            YamlConfiguration defaultConfig = createDefaultConfig(fileName);
            defaultConfig.save(targetFile);
            
            getLogger().info("Created default configuration file: " + targetPath);
            
        } catch (Exception e) {
            getLogger().severe("Failed to create default config file " + targetPath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Loads all configuration files from the plugin data folder
     * Modified to NOT merge configurations but keep them separate
     * 
     * @throws Exception if any configuration fails to load
     */
    private void loadConfigurations() throws Exception {
        // Load main config from plugin data folder
        internalConfig = loadConfigFromDataFolder("config.yml");
        if (internalConfig == null) {
            throw new Exception("Failed to load config.yml from plugin data folder");
        }
        
        // Load separate config files
        categoryConfig = loadConfigFromDataFolder("components/category.yml");
        if (categoryConfig == null) {
            throw new Exception("Failed to load components/category.yml from plugin data folder");
        }
        
        tagsConfig = loadConfigFromDataFolder("components/tags.yml");
        if (tagsConfig == null) {
            throw new Exception("Failed to load components/tags.yml from plugin data folder");
        }
        
        guiConfig = loadConfigFromDataFolder("components/gui.yml");
        if (guiConfig == null) {
            throw new Exception("Failed to load components/gui.yml from plugin data folder");
        }
        
        globalLangConfig = loadConfigFromDataFolder("languages/global.yml");
        if (globalLangConfig == null) {
            throw new Exception("Failed to load languages/global.yml from plugin data folder");
        }
        
        // Merge global language config into internal config for easy access to labels
        mergeGlobalLangIntoInternal();
        
        getLogger().info("Successfully loaded all configuration files separately");
    }
    
    /**
     * Merges only the global language configuration into internal config
     * This allows easy access to labels and system settings while keeping other configs separate
     */
    private void mergeGlobalLangIntoInternal() {
        // Only merge language/system settings from global.yml into internal config
        for (String key : globalLangConfig.getKeys(true)) {
            internalConfig.set(key, globalLangConfig.get(key));
        }
    }
    
    /**
     * Loads a configuration file from the plugin data folder
     * 
     * @param fileName The name of the file to load
     * @return The loaded configuration, or null if file not found
     */
    private YamlConfiguration loadConfigFromDataFolder(String fileName) {
        try {
            File configFile = new File(getDataFolder(), fileName);
            
            if (!configFile.exists()) {
                getLogger().warning("Configuration file not found in data folder: " + fileName);
                getLogger().warning("Creating default configuration for: " + fileName);
                return createDefaultConfig(new File(fileName).getName());
            }
            
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            getLogger().info("Loaded configuration from data folder: " + fileName);
            return config;
            
        } catch (Exception e) {
            getLogger().severe("Failed to load configuration file from data folder: " + fileName);
            e.printStackTrace();
            return createDefaultConfig(new File(fileName).getName());
        }
    }
    
    /**
     * Creates a default configuration when files are missing
     * Enhanced with complete default configurations
     * 
     * @param fileName The configuration file name
     * @return A comprehensive default configuration
     */
    private YamlConfiguration createDefaultConfig(String fileName) {
        YamlConfiguration config = new YamlConfiguration();
        
        switch (fileName) {
            case "config.yml":
                createDefaultMainConfig(config);
                break;
            case "category.yml":
                createDefaultCategoryConfig(config);
                break;
            case "tags.yml":
                createDefaultTagsConfig(config);
                break;
            case "gui.yml":
                createDefaultGuiConfig(config);
                break;
            case "global.yml":
                createDefaultGlobalConfig(config);
                break;
        }
        
        getLogger().info("Created comprehensive default configuration for: " + fileName);
        return config;
    }

    private void createDefaultMainConfig(YamlConfiguration config) {
        config.set("settings.system.debug", false);
        config.set("settings.system.modal.default", "CATEGORY");
        config.set("settings.system.modal.category.enabled", true);
        config.set("settings.system.modal.tags.close-on-activation", false);
        config.set("settings.system.modal.tags.close-on-unlock", false);
        config.set("settings.system.modal.tags.close-button-swap", true);
    }

    private void createDefaultCategoryConfig(YamlConfiguration config) {
        // ALL category
        config.set("category.all.id", "ALL");
        config.set("category.all.name", "<gold>ALL</gold>");
        config.set("category.all.filter-name", "<gold>ALL</gold>");
        config.set("category.all.permission", "coretags.group.player");
        config.set("category.all.node", "category.all");
        config.set("category.all.protected", false);
        config.set("category.all.material.id", "BOOKSHELF");
        config.set("category.all.material.material-modifier", -1);
        config.set("category.all.material.custom-model-data", -1);
        config.set("category.all.material.glow", false);
        config.set("category.all.material.slot", 10);
        
        List<String> allLore = Arrays.asList(
            "<grey>Display tags in</grey> <gold>ALL</gold> <grey>categories.</grey>",
            ""
        );
        config.set("category.all.lore", allLore);
        
        // DEFAULT category
        config.set("category.default.id", "DEFAULT");
        config.set("category.default.name", "<grey>DEFAULT</grey>");
        config.set("category.default.filter-name", "<grey>DEFAULT</grey>");
        config.set("category.default.permission", "coretags.category.default");
        config.set("category.default.node", "category.default");
        config.set("category.default.protected", false);
        config.set("category.default.material.id", "LIGHT_GRAY_SHULKER_BOX");
        config.set("category.default.material.slot", 11);
        
        List<String> defaultLore = Arrays.asList(
            "<grey>Display</grey> <grey>DEFAULT</grey> <grey>tags.</grey>",
            ""
        );
        config.set("category.default.lore", defaultLore);
    }

    private void createDefaultTagsConfig(YamlConfiguration config) {
        // Default tag
        config.set("tags.default.id", "DEFAULT");
        config.set("tags.default.name", "<grey>NONE</grey>");
        config.set("tags.default.display", "");
        config.set("tags.default.permission", "coretags.group.player");
        config.set("tags.default.node", "category.default");
        config.set("tags.default.color", "MULTI");
        config.set("tags.default.protected", true);
        config.set("tags.default.material.id", "NAME_TAG");
        config.set("tags.default.material.material-modifier", -1);
        config.set("tags.default.material.custom-model-data", -1);
        config.set("tags.default.sort-order", 0);
        config.set("tags.default.cost", -1);
        
        List<String> defaultTagLore = Arrays.asList("");
        config.set("tags.default.lore", defaultTagLore);
        
        // Example tag
        config.set("tags.cosmicstar.id", "COSMICSTAR");
        config.set("tags.cosmicstar.name", "<gold>Cosmic Star</gold>");
        config.set("tags.cosmicstar.display", "<white>✭</white>");
        config.set("tags.cosmicstar.permission", "coretags.tag.cosmicstar");
        config.set("tags.cosmicstar.node", "category.default");
        config.set("tags.cosmicstar.color", "MULTI");
        config.set("tags.cosmicstar.protected", false);
        config.set("tags.cosmicstar.material.id", "NAME_TAG");
        config.set("tags.cosmicstar.sort-order", 1);
        config.set("tags.cosmicstar.cost", 0);
        
        List<String> starLore = Arrays.asList(
            "<grey>This is a test tag.</grey>",
            ""
        );
        config.set("tags.cosmicstar.lore", starLore);
    }

    private void createDefaultGuiConfig(YamlConfiguration config) {
        config.set("gui.category-menu.rows", 4);
        config.set("gui.tags-menu.rows", 6);
        
        // Layout items
        config.set("gui.layout.items.empty-slot.category-menu.enabled", false);
        config.set("gui.layout.items.empty-slot.tags-menu.enabled", false);
        config.set("gui.layout.items.empty-slot.material", "GRAY_STAINED_GLASS_PANE");
        
        // Close button
        config.set("gui.layout.items.close-button-material.enabled", true);
        config.set("gui.layout.items.close-button-material.material", "BARRIER");
        config.set("gui.layout.items.close-button-material.slot.category-menu", 27);
        config.set("gui.layout.items.close-button-material.slot.tags-menu", 45);
        
        // Back button
        config.set("gui.layout.items.back-button.enabled", true);
        config.set("gui.layout.items.back-button.material", "SPECTRAL_ARROW");
        config.set("gui.layout.items.back-button.slot.category-menu", 27);
        config.set("gui.layout.items.back-button.slot.tags-menu", 45);
        
        // Tag slots
        config.set("gui.tags.slots.row1", "10..16");
        config.set("gui.tags.slots.row2", "19..25");
        config.set("gui.tags.slots.row3", "28..34");
        config.set("gui.tags.slots.row4", "37..43");
    }

    private void createDefaultGlobalConfig(YamlConfiguration config) {
        // System settings
        config.set("settings.system.prefix.text", "<blue>CoreTags</blue>");
        
        // GUI titles
        config.set("settings.system.gui.titles.category-menu", "<gold>Tags</gold> <dark_grey>|</dark_grey> <gold>Categories</gold>");
        config.set("settings.system.gui.titles.tags-menu", "<gold>Tags</gold> <dark_grey>|</dark_grey> {category} <dark_grey>•</dark_grey> <grey>({currentpage}/{totalpages})</grey>");
        config.set("settings.system.gui.titles.close-button", "<red>Exit</red>");
        config.set("settings.system.gui.titles.back-button", "<red>Back</red>");
        
        // Conditions/labels
        config.set("settings.system.conditions.unlocked", "<green>UNLOCKED</green>");
        config.set("settings.system.conditions.locked", "<red>LOCKED</red>");
        config.set("settings.system.conditions.protected", "<dark_purple>PROTECTED</dark_purple>");
        config.set("settings.system.conditions.favorite", "<aqua>FAVORITE</aqua>");
        config.set("settings.system.conditions.active", "<gold>ACTIVE</gold>");
        
        // Controls
        config.set("settings.system.controls.gui.leftclick", "<grey>⏵</grey> <white>⸶</white> <#2ECC71>ʟᴇꜰᴛ ᴄʟɪᴄᴋ</#2ECC71> • {setactive}");
        config.set("settings.system.controls.gui.rightclick", "<grey>⏵</grey> <white>⸷</white> <#2ECC71>ʀɪɢʜᴛ ᴄʟɪᴄᴋ</#2ECC71> • {togglefavorite}");
        
        // Labels  
        config.set("settings.system.gui.labels.setactive", "<gold>[Set as active tag]</gold>");
        config.set("settings.system.gui.labels.togglefavorite.add", "<gold>[Set favorite]</gold>");
        config.set("settings.system.gui.labels.unlocktag", "<gold>[Unlock tag]</gold>");
        
        // Colors
        config.set("settings.system.colors.all.text", "<gold>ALL</gold>");
        config.set("settings.system.colors.multi.text", "<blue>M</blue><green>U</green><yellow>L</yellow><red>T</red><light_purple>I</light_purple>");
        config.set("settings.system.colors.red.text", "<red>RED</red>");
        config.set("settings.system.colors.green.text", "<green>GREEN</green>");
        config.set("settings.system.colors.blue.text", "<blue>BLUE</blue>");
        
        // Messages
        config.set("messages.modal.closed", "<grey>Menu closed.</grey>");
        config.set("messages.modal.category-closed", "<grey>Category menu closed.</grey>");
        config.set("messages.modal.tags-closed", "<grey>Tags menu closed.</grey>");
        config.set("messages.error.modal-open-failed", "<red>Failed to open menu. Please try again.</red>");
    }
    
    /**
     * Register this plugin with CoreFramework using direct method
     * @return true if registration was successful
     */
    private boolean registerWithCoreFramework() {
        try {
            // Get CoreFramework instance directly from Bukkit's plugin manager
            Plugin plugin = Bukkit.getPluginManager().getPlugin("CoreFramework");
            if (!(plugin instanceof CoreFramework)) {
                getLogger().severe("CoreFramework plugin is not the correct type!");
                return false;
            }
            
            CoreFramework coreFramework = (CoreFramework) plugin;
            
            // Define hook requirements for CoreTags
            List<HookRequirement> hookRequirements = Arrays.asList(
                HookRequirement.required("PlaceholderAPI", "2.11.6+"),
                HookRequirement.required("LuckPerms", "5.4+"),
                HookRequirement.optional("VaultAPI", "1.7.3+"),
                HookRequirement.optional("Nexo", "any")
            );
            
            // Register with CoreFramework using the direct method
            registeredPlugin = coreFramework.registerPluginDirect(
                this, "2.0-HORIZON", "HORIZON", hookRequirements
            );
            
            if (registeredPlugin == null) {
                getLogger().severe("CoreFramework returned null for plugin registration!");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to register with CoreFramework!", e);
            return false;
        }
    }
    
    /**
     * Complete initialization after successful registration
     */
    private void completeInitialization() {
        // Initialize hook processor and process hooks
        hookProcessor = new CoreTagsHookProcessor(this);
        boolean hookSuccess = hookProcessor.processHooks(registeredPlugin);
        
        // Update the registered plugin with hook results
        registeredPlugin.setAllRequiredHooksSuccessful(hookSuccess);
        
        // Continue with plugin initialization if hooks succeeded
        if (hookSuccess) {
            initializePlugin();
            enabled = true;
        } else {
            getLogger().severe("Required dependencies missing - CoreTags will not function properly!");
            // Don't disable the plugin, let CoreFramework handle the display
            // The plugin will be marked as failed in the CoreFramework display
        }
    }
    
    /**
     * Initialize the plugin features after successful hook processing
     */
    private void initializePlugin() {
        // Initialize command handler
        commandHandler = new CoreTagsCommand(this);
        
        // Register commands
        if (getCommand("coretags") != null) {
            getCommand("coretags").setExecutor(commandHandler);
            getCommand("coretags").setTabCompleter(commandHandler);
        } else {
            getLogger().warning("Failed to register /coretags command - command not found in plugin.yml!");
        }
        
        // Initialize tag system components
        // TODO: Initialize tag managers, listeners, etc.
        initializeTagSystem();
        
        // Log successful initialization if debug enabled
        if (internalConfig.getBoolean("settings.system.debug", false)) {
            getLogger().info("CoreTags initialized successfully with modal system!");
        }
    }
    
    /**
     * Initialize the tag system components
     */
    private void initializeTagSystem() {
        // TODO: Initialize player data manager
        // TODO: Initialize tag manager
        // TODO: Initialize economy integration (if enabled)
        // TODO: Register event listeners
        
        // For now, just log that the system is ready
        if (internalConfig.getBoolean("settings.system.debug", false)) {
            getLogger().info("Tag system components initialized (placeholder)");
        }
    }

    /**
     * Reloads all configuration files from the plugin data folder
     * Used by the reload command
     * 
     * @return True if reload was successful
     */
    public boolean reloadConfigurations() {
        try {
            getLogger().info("Reloading CoreTags configurations...");
            
            // Reload all configurations with correct paths
            internalConfig = loadConfigFromDataFolder("config.yml");
            categoryConfig = loadConfigFromDataFolder("components/category.yml");
            tagsConfig = loadConfigFromDataFolder("components/tags.yml");
            guiConfig = loadConfigFromDataFolder("components/gui.yml");
            globalLangConfig = loadConfigFromDataFolder("languages/global.yml");
            
            // Merge global language config into internal
            mergeGlobalLangIntoInternal();
            
            getLogger().info("Configuration reload completed successfully");
            return true;
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to reload configurations!", e);
            return false;
        }
    }
    
    @Override
    public void onDisable() {
        // Send shutdown message if we were properly enabled
        if (enabled) {
            String shutdownMessage = internalConfig.getString("messages.shutdown.complete", "CoreTags disabled!");
            TextUtility.sendConsoleMessage(replaceVariables(shutdownMessage));
        }
        
        // Cleanup resources
        hookProcessor = null;
        registeredPlugin = null;
        commandHandler = null;
        instance = null;
        
        getLogger().info("CoreTags disabled.");
    }
    
    /**
     * Replace variables in messages with their actual values
     * Enhanced to handle all label placeholders
     * @param message The message to process
     * @return The processed message
     */
    public String replaceVariables(String message) {
        if (message == null) return "";
        
        // Replace prefix variable
        String prefix = internalConfig.getString("settings.system.prefix.text", "<blue>CoreTags</blue>");
        message = message.replace("{prefix}", prefix);
        
        // Replace version variables
        String coreframeapiVersion = internalConfig.getString("system.coreframeapi-version", "2.0-HORIZON");
        message = message.replace("{coreframeapiversion}", coreframeapiVersion);
        
        // Replace plugin version
        message = message.replace("{version}", getDescription().getVersion());
        
        // Replace GUI control placeholders from global.yml
        message = replaceControlPlaceholders(message);
        
        // Replace color placeholders
        message = replaceColorPlaceholders(message);
        
        // Replace category placeholders
        message = replaceCategoryPlaceholders(message);
        
        // Replace condition/label placeholders
        message = replaceLabelPlaceholders(message);
        
        return message;
    }

    /**
     * Replace control placeholders like {gui.leftclick}, {chat.rightclick}, etc.
     */
    private String replaceControlPlaceholders(String message) {
        // GUI controls
        message = message.replace("{gui.leftclick}", 
            internalConfig.getString("settings.system.controls.gui.leftclick", "<grey>⏵</grey> <white>⸶</white> <#2ECC71>ʟᴇꜰᴛ ᴄʟɪᴄᴋ</#2ECC71> • {setactive}"));
        message = message.replace("{gui.rightclick}", 
            internalConfig.getString("settings.system.controls.gui.rightclick", "<grey>⏵</grey> <white>⸷</white> <#2ECC71>ʀɪɢʜᴛ ᴄʟɪᴄᴋ</#2ECC71> • {togglefavorite}"));
        message = message.replace("{gui.shiftleftclick}", 
            internalConfig.getString("settings.system.controls.gui.shiftleftclick", "<grey>⏵</grey> <#2ECC71>ꜱʜɪꜰᴛ</#2ECC71> <grey>+</grey> <white>⸶</white> <#2ECC71>ʟᴇꜰᴛ ᴄʟɪᴄᴋ</#2ECC71> • {unlocktag}"));
        message = message.replace("{gui.shiftrightclick}", 
            internalConfig.getString("settings.system.controls.gui.shiftrightclick", "<grey>⏵</grey> <#2ECC71>ꜱʜɪꜰᴛ</#2ECC71> <grey>+</grey> <white>⸷</white> <#2ECC71>ʀɪɢʜᴛ ᴄʟɪᴄᴋ</#2ECC71> • {activefavorite}"));
        
        // Chat controls
        message = message.replace("{chat.leftclick}", 
            internalConfig.getString("settings.system.controls.chat.leftclick", "<white>⸶</white> <#2ECC71>ʟᴇꜰᴛ ᴄʟɪᴄᴋ</#2ECC71>"));
        message = message.replace("{chat.rightclick}", 
            internalConfig.getString("settings.system.controls.chat.rightclick", "<white>⸷</white> <#2ECC71>ʀɪɢʜᴛ ᴄʟɪᴄᴋ</#2ECC71>"));
        
        // Action labels
        message = message.replace("{setactive}", 
            internalConfig.getString("settings.system.gui.labels.setactive", "<gold>[Set as active tag]</gold>"));
        message = message.replace("{togglefavorite}", 
            internalConfig.getString("settings.system.gui.labels.togglefavorite.add", "<gold>[Set favorite]</gold>"));
        message = message.replace("{unlocktag}", 
            internalConfig.getString("settings.system.gui.labels.unlocktag", "<gold>[Unlock tag]</gold>"));
        message = message.replace("{activefavorite}", 
            internalConfig.getString("settings.system.gui.labels.activefavorite", "<gold>[Set as active tag + favorite]</gold>"));
        
        return message;
    }

    /**
     * Replace color placeholders like {colors.red}, {colors.blue}, etc.
     */
    private String replaceColorPlaceholders(String message) {
        message = message.replace("{colors.all}", 
            internalConfig.getString("settings.system.colors.all.text", "<gold>ALL</gold>"));
        message = message.replace("{colors.multi}", 
            internalConfig.getString("settings.system.colors.multi.text", "<blue>M</blue><green>U</green><yellow>L</yellow><red>T</red><light_purple>I</light_purple>"));
        message = message.replace("{colors.red}", 
            internalConfig.getString("settings.system.colors.red.text", "<red>RED</red>"));
        message = message.replace("{colors.green}", 
            internalConfig.getString("settings.system.colors.green.text", "<green>GREEN</green>"));
        message = message.replace("{colors.blue}", 
            internalConfig.getString("settings.system.colors.blue.text", "<blue>BLUE</blue>"));
        // Add more colors as needed...
        
        return message;
    }

    /**
     * Replace category placeholders like {category.default}, {category.premium}, etc.
     */
    private String replaceCategoryPlaceholders(String message) {
        // Look for {category.X} patterns and replace with category names
        if (message.contains("{category.")) {
            String[] parts = message.split("\\{category\\.");
            for (int i = 1; i < parts.length; i++) {
                int endIndex = parts[i].indexOf('}');
                if (endIndex > 0) {
                    String categoryId = parts[i].substring(0, endIndex);
                    String categoryName = categoryConfig.getString("category." + categoryId + ".name", 
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
    private String replaceLabelPlaceholders(String message) {
        message = message.replace("{unlocked}", 
            internalConfig.getString("settings.system.conditions.unlocked", "<green>UNLOCKED</green>"));
        message = message.replace("{locked}", 
            internalConfig.getString("settings.system.conditions.locked", "<red>LOCKED</red>"));
        message = message.replace("{protected}", 
            internalConfig.getString("settings.system.conditions.protected", "<dark_purple>PROTECTED</dark_purple>"));
        message = message.replace("{favorite}", 
            internalConfig.getString("settings.system.conditions.favorite", "<aqua>FAVORITE</aqua>"));
        message = message.replace("{favorites}", 
            internalConfig.getString("settings.system.conditions.favorite", "<aqua>FAVORITE</aqua>"));
        message = message.replace("{active}", 
            internalConfig.getString("settings.system.conditions.active", "<gold>ACTIVE</gold>"));
        message = message.replace("{all}", 
            internalConfig.getString("settings.system.colors.all.text", "<gold>ALL</gold>"));
        
        return message;
    }
    
    /**
     * Get the plugin instance
     * @return The CoreTags instance
     */
    public static CoreTags getInstance() {
        return instance;
    }
    
    /**
     * Get the internal configuration (includes merged global language config)
     * @return The internal configuration
     */
    public YamlConfiguration getInternalConfig() {
        return internalConfig;
    }
    
    /**
     * Get the category configuration (separate)
     * @return The category configuration
     */
    public YamlConfiguration getCategoryConfig() {
        return categoryConfig;
    }
    
    /**
     * Get the tags configuration (separate)
     * @return The tags configuration
     */
    public YamlConfiguration getTagsConfig() {
        return tagsConfig;
    }
    
    /**
     * Get the GUI configuration (separate)
     * @return The GUI configuration
     */
    public YamlConfiguration getGuiConfig() {
        return guiConfig;
    }
    
    /**
     * Get the global language configuration (separate)
     * @return The global language configuration
     */
    public YamlConfiguration getGlobalLangConfig() {
        return globalLangConfig;
    }
    
    /**
     * Get the hook processor
     * @return The hook processor
     */
    public CoreTagsHookProcessor getHookProcessor() {
        return hookProcessor;
    }
    
    /**
     * Get the registered plugin instance
     * @return The registered plugin
     */
    public RegisteredPlugin getRegisteredPlugin() {
        return registeredPlugin;
    }
    
    /**
     * Get the command handler
     * @return The command handler
     */
    public CoreTagsCommand getCommandHandler() {
        return commandHandler;
    }
    
    /**
     * Check if the plugin is properly enabled with all dependencies
     * @return True if enabled and all required hooks successful
     */
    public boolean isFullyEnabled() {
        return enabled && registeredPlugin != null && registeredPlugin.areAllRequiredHooksSuccessful();
    }
}