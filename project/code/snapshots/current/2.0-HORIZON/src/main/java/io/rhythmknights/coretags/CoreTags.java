package io.rhythmknights.coretags;

import io.rhythmknights.coreframework.CoreFramework;
import io.rhythmknights.coreframework.component.api.hook.HookRequirement;
import io.rhythmknights.coreframework.component.api.plugin.RegisteredPlugin;
import io.rhythmknights.coreframework.component.utility.TextUtility;
import io.rhythmknights.coretags.component.command.CoreTagsCommand;
import io.rhythmknights.coretags.component.data.DataProcessor;
import io.rhythmknights.coretags.component.data.PlayerDataProcessor;
import io.rhythmknights.coretags.component.hook.CoreTagsHookProcessor;
import io.rhythmknights.coretags.component.listener.LuckPermsListener;
import io.rhythmknights.coretags.component.listener.PlayerEventListener;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

/**
 * Main class for the CoreTags plugin.
 * Handles plugin initialization, configuration loading, and CoreFramework integration.
 */
public class CoreTags extends JavaPlugin {
    private static CoreTags instance;
    
    // Core components
    private DataProcessor dataProcessor;
    private CoreTagsHookProcessor hookProcessor;
    private RegisteredPlugin registeredPlugin;
    private CoreTagsCommand commandHandler;
    
    // Player data system
    private PlayerDataProcessor playerDataProcessor;
    private LuckPermsListener luckPermsListener;
    private PlayerEventListener playerEventListener;
    
    // Plugin state
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
        
        getLogger().info("CoreTags onEnable() starting...");
        
        // Initialize data processor first
        dataProcessor = new DataProcessor(this);
        
        if (!dataProcessor.initialize()) {
            getLogger().severe("Failed to initialize DataProcessor - disabling plugin!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        getLogger().info("DataProcessor initialized successfully");
        
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
        
        getLogger().info("CoreFramework dependency verified");
        
        // Register with CoreFramework using direct method
        if (registerWithCoreFramework()) {
            // Registration successful, continue with initialization
            getLogger().info("Successfully registered with CoreFramework");
            completeInitialization();
        } else {
            // Registration failed
            getLogger().severe("Failed to register with CoreFramework - disabling plugin!");
            getServer().getPluginManager().disablePlugin(this);
        }
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
        getLogger().info("Starting plugin initialization...");
        
        // Initialize hook processor and process hooks
        hookProcessor = new CoreTagsHookProcessor(this);
        getLogger().info("Processing dependency hooks...");
        boolean hookSuccess = hookProcessor.processHooks(registeredPlugin);
        
        // Update the registered plugin with hook results
        registeredPlugin.setAllRequiredHooksSuccessful(hookSuccess);
        
        // Continue with plugin initialization regardless of hook success for now
        // This allows the plugin to work even if some optional dependencies are missing
        getLogger().info("Initializing plugin features...");
        initializePlugin();
        enabled = true;
        
        if (hookSuccess) {
            getLogger().info("All required dependencies loaded successfully");
        } else {
            getLogger().warning("Some dependencies missing - plugin may have limited functionality");
        }
        
        getLogger().info("CoreTags initialization completed");
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
        
        // Initialize player data system
        initializePlayerDataSystem();
        
        // Initialize tag system components
        initializeTagSystem();
        
        // Log successful initialization if debug enabled
        if (isDebugEnabled()) {
            getLogger().info("CoreTags initialized successfully with modal system!");
        }
    }
    
    /**
     * Initialize the player data processing system
     */
    private void initializePlayerDataSystem() {
        try {
            getLogger().info("Initializing player data system...");
            
            // Initialize the player data processor
            playerDataProcessor = new PlayerDataProcessor(this);
            getLogger().info("PlayerDataProcessor initialized");
            
            // Initialize LuckPerms listener for automatic sync
            luckPermsListener = new LuckPermsListener(this, playerDataProcessor);
            getLogger().info("LuckPermsListener initialized");
            
            // Initialize player event listener for join/quit handling
            playerEventListener = new PlayerEventListener(this, playerDataProcessor);
            getLogger().info("PlayerEventListener created");
            
            // Register the player event listener with Bukkit
            getServer().getPluginManager().registerEvents(playerEventListener, this);
            getLogger().info("PlayerEventListener registered with Bukkit");
            
            getLogger().info("Player data system initialized successfully");
            
        } catch (Exception e) {
            getLogger().severe("Failed to initialize player data system: " + e.getMessage());
            e.printStackTrace();
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
        if (isDebugEnabled()) {
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
            
            if (dataProcessor == null) {
                getLogger().severe("DataProcessor is null - cannot reload configurations!");
                return false;
            }
            
            boolean success = dataProcessor.reload();
            
            if (success) {
                getLogger().info("Configuration reload completed successfully");
            } else {
                getLogger().severe("Configuration reload failed!");
            }
            
            return success;
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to reload configurations!", e);
            return false;
        }
    }
    
    @Override
    public void onDisable() {
        // Send shutdown message if we were properly enabled
        if (enabled) {
            String shutdownMessage = getConsolidatedConfig().getString("messages.shutdown.complete", "CoreTags disabled!");
            TextUtility.sendConsoleMessage(replaceVariables(shutdownMessage));
        }
        
        // Cleanup player data system
        if (luckPermsListener != null) {
            luckPermsListener.unregister();
        }
        
        // Cleanup resources
        hookProcessor = null;
        registeredPlugin = null;
        commandHandler = null;
        dataProcessor = null;
        playerDataProcessor = null;
        luckPermsListener = null;
        playerEventListener = null;
        instance = null;
        
        getLogger().info("CoreTags disabled.");
    }
    
    /**
     * Replace variables in messages with their actual values
     * Delegates to DataProcessor for centralized handling
     * @param message The message to process
     * @return The processed message
     */
    public String replaceVariables(String message) {
        if (message == null) return "";

        // Delegate to DataProcessor for centralized variable replacement
        return dataProcessor != null ? dataProcessor.replaceVariables(message) : message;
    }
    
    /**
     * Get the plugin instance
     * @return The CoreTags instance
     */
    public static CoreTags getInstance() {
        return instance;
    }
    
    /**
     * Get the data processor
     * @return The data processor instance
     */
    public DataProcessor getDataProcessor() {
        return dataProcessor;
    }
    
    /**
     * Get the player data processor
     * @return The player data processor
     */
    public PlayerDataProcessor getPlayerDataProcessor() {
        return playerDataProcessor;
    }
    
    /**
     * Get the consolidated configuration (backward compatibility)
     * @return The consolidated configuration
     */
    public YamlConfiguration getConsolidatedConfig() {
        return dataProcessor != null ? dataProcessor.getConsolidatedConfig() : new YamlConfiguration();
    }
    
    /**
     * Get the internal configuration (backward compatibility)
     * @return The consolidated configuration (acts as internal config)
     */
    public YamlConfiguration getInternalConfig() {
        return getConsolidatedConfig();
    }
    
    /**
     * Get the category configuration
     * @return The category configuration
     */
    public YamlConfiguration getCategoryConfig() {
        return dataProcessor != null ? dataProcessor.getCategoryConfig() : new YamlConfiguration();
    }
    
    /**
     * Get the tags configuration
     * @return The tags configuration
     */
    public YamlConfiguration getTagsConfig() {
        return dataProcessor != null ? dataProcessor.getTagsConfig() : new YamlConfiguration();
    }
    
    /**
     * Get the GUI configuration
     * @return The GUI configuration
     */
    public YamlConfiguration getGuiConfig() {
        return dataProcessor != null ? dataProcessor.getGuiConfig() : new YamlConfiguration();
    }
    
    /**
     * Get the global language configuration
     * @return The language configuration
     */
    public YamlConfiguration getGlobalLangConfig() {
        return dataProcessor != null ? dataProcessor.getLangConfig() : new YamlConfiguration();
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
    
    // Convenience methods using DataProcessor modules
    
    /**
     * Check if debug mode is enabled
     * @return true if debug is enabled
     */
    public boolean isDebugEnabled() {
        return dataProcessor != null && 
               dataProcessor.getSystemDataModule() != null && 
               dataProcessor.getSystemDataModule().isDebugEnabled();
    }
    
    /**
     * Check if economy is enabled
     * @return true if economy is enabled
     */
    public boolean isEconomyEnabled() {
        return dataProcessor != null && 
               dataProcessor.getSystemDataModule() != null && 
               dataProcessor.getSystemDataModule().isEconomyEnabled();
    }
    
    /**
     * Get the default modal type
     * @return The default modal type
     */
    public String getDefaultModalType() {
        return dataProcessor != null && dataProcessor.getSystemDataModule() != null ? 
               dataProcessor.getSystemDataModule().getDefaultModalType() : "CATEGORY";
    }
    
    /**
     * Check if categories are enabled
     * @return true if categories are enabled
     */
    public boolean areCategoriesEnabled() {
        return dataProcessor != null && dataProcessor.getSystemDataModule() != null ? 
               dataProcessor.getSystemDataModule().areCategoriesEnabled() : true;
    }
    
    /**
     * Check if modal should close on tag activation
     * @return true if should close on activation
     */
    public boolean shouldCloseOnActivation() {
        return dataProcessor != null && dataProcessor.getSystemDataModule() != null ? 
               dataProcessor.getSystemDataModule().shouldCloseOnActivation() : false;
    }
    
    /**
     * Check if modal should close on tag unlock
     * @return true if should close on unlock
     */
    public boolean shouldCloseOnUnlock() {
        return dataProcessor != null && dataProcessor.getSystemDataModule() != null ? 
               dataProcessor.getSystemDataModule().shouldCloseOnUnlock() : false;
    }
    
    /**
     * Check if back button should be swapped with close button
     * @return true if should swap buttons
     */
    public boolean shouldSwapCloseButton() {
        return dataProcessor != null && dataProcessor.getSystemDataModule() != null ? 
               dataProcessor.getSystemDataModule().shouldSwapCloseButton() : true;
    }
    
    /**
     * Get a category's display name
     * @param categoryId The category ID
     * @return The display name
     */
    public String getCategoryDisplayName(String categoryId) {
        return dataProcessor != null && dataProcessor.getCategoryModalDataModule() != null ? 
               dataProcessor.getCategoryModalDataModule().getCategoryName(categoryId) : categoryId.toUpperCase();
    }
    
    /**
     * Get a tag's display name
     * @param tagId The tag ID
     * @return The display name
     */
    public String getTagDisplayName(String tagId) {
        return dataProcessor != null && dataProcessor.getTagsModalDataModule() != null ? 
               dataProcessor.getTagsModalDataModule().getTagName(tagId) : tagId.toUpperCase();
    }
    
    /**
     * Get the number of rows for category menu
     * @return The number of rows
     */
    public int getCategoryMenuRows() {
        return dataProcessor != null && dataProcessor.getBaseModalDataModule() != null ? 
               dataProcessor.getBaseModalDataModule().getCategoryMenuRows() : 4;
    }
    
    /**
     * Get the number of rows for tags menu
     * @return The number of rows
     */
    public int getTagsMenuRows() {
        return dataProcessor != null && dataProcessor.getBaseModalDataModule() != null ? 
               dataProcessor.getBaseModalDataModule().getTagsMenuRows() : 6;
    }
    
    /**
     * Get a condition/status label from language data
     * @param condition The condition name
     * @return The formatted label
     */
    public String getConditionLabel(String condition) {
        return dataProcessor != null && dataProcessor.getLangDataModule() != null ? 
               dataProcessor.getLangDataModule().getConditionLabel(condition) : condition.toUpperCase();
    }
    
    /**
     * Get a GUI title from language data
     * @param titleKey The title key
     * @return The title text
     */
    public String getGuiTitle(String titleKey) {
        return dataProcessor != null && dataProcessor.getLangDataModule() != null ? 
               dataProcessor.getLangDataModule().getGuiTitle(titleKey) : titleKey;
    }
    
    /**
     * Get a message from language data
     * @param messageKey The message key
     * @return The message text
     */
    public String getMessage(String messageKey) {
        return dataProcessor != null && dataProcessor.getLangDataModule() != null ? 
               dataProcessor.getLangDataModule().getMessage(messageKey) : messageKey;
    }
}