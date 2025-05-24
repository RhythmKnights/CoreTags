package io.rhythmknights.coretags.component.hook.module;

import io.rhythmknights.coretags.CoreTags;
import io.rhythmknights.coreframework.component.utility.TextUtility;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * Base abstract class for all plugin hook implementations.
 * Provides common functionality for plugin dependency checking and messaging.
 */
public abstract class BaseHook {
    /** Reference to the main plugin instance */
    protected final CoreTags plugin;
    
    /** Name of the plugin this hook depends on */
    protected final String pluginName;
    
    /** Required minimum version of the dependent plugin */
    protected final String requiredVersion;
    
    /** Whether this dependency is required for the plugin to function */
    protected final boolean required;
    
    /** Whether the dependent plugin is available and meets version requirements */
    protected boolean available = false;
    
    /**
     * Constructor for BaseHook
     * 
     * @param plugin The CoreTags plugin instance
     * @param pluginName The name of the plugin to hook into
     * @param requiredVersion The minimum required version of the plugin
     * @param required Whether this plugin is required for CoreTags to function
     */
    public BaseHook(CoreTags plugin, String pluginName, String requiredVersion, boolean required) {
        this.plugin = plugin;
        this.pluginName = pluginName;
        this.requiredVersion = requiredVersion;
        this.required = required;
        
        // Check availability on construction
        checkAvailability();
    }
    
    /**
     * Check if the plugin is available
     */
    protected void checkAvailability() {
        Plugin targetPlugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (targetPlugin != null && targetPlugin.isEnabled()) {
            // Version check can be implemented by subclasses
            if (checkVersion(targetPlugin)) {
                available = true;
                sendSuccessMessage();
            } else {
                available = false;
                sendFailedMessage();
            }
        } else {
            available = false;
            sendFailedMessage();
        }
    }
    
    /**
     * Check if the plugin version is compatible
     * @param plugin The plugin to check
     * @return true if version is compatible, false otherwise
     */
    protected abstract boolean checkVersion(Plugin plugin);
    
    /**
     * Send success message to console
     */
    protected void sendSuccessMessage() {
        String path = "messages.hook." + getConfigKey() + ".success";
        String message = plugin.getInternalConfig().getString(path, pluginName + " hooked successfully.");
        TextUtility.sendConsoleMessage(plugin.replaceVariables(message));
    }
    
    /**
     * Send failed message to console
     */
    protected void sendFailedMessage() {
        String path = "messages.hook." + getConfigKey() + ".failed";
        String message = plugin.getInternalConfig().getString(path, "Failed to hook " + pluginName);
        TextUtility.sendConsoleMessage(plugin.replaceVariables(message));
    }
    
    /**
     * Get the configuration key for this hook
     * @return The configuration key
     */
    protected abstract String getConfigKey();
    
    /**
     * Check if the plugin is available
     * @return true if available, false otherwise
     */
    public boolean isAvailable() {
        return available;
    }
    
    /**
     * Check if the plugin is required
     * @return true if required, false otherwise
     */
    public boolean isRequired() {
        return required;
    }
}