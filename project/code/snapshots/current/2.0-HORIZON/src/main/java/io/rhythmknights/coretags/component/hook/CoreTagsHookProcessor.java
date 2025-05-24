package io.rhythmknights.coretags.component.hook;

import io.rhythmknights.coreframework.component.api.hook.CoreHookProcessor;
import io.rhythmknights.coreframework.component.api.hook.HookResult;
import io.rhythmknights.coreframework.component.api.plugin.RegisteredPlugin;
import io.rhythmknights.coretags.CoreTags;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * Hook processor for CoreTags plugin
 * Handles checking and initializing all required and optional dependencies
 */
public class CoreTagsHookProcessor implements CoreHookProcessor {
    
    private final CoreTags plugin;
    private HookResult hookResult;
    
    /**
     * Constructor for CoreTagsHookProcessor
     * @param plugin The CoreTags plugin instance
     */
    public CoreTagsHookProcessor(CoreTags plugin) {
        this.plugin = plugin;
        this.hookResult = new HookResult();
    }
    
    @Override
    public boolean processHooks(RegisteredPlugin registeredPlugin) {
        //plugin.getLogger().info("Processing hooks for CoreTags..."); - STARTUP LOGGER
        
        boolean allRequiredSuccessful = true;
        
        // Process each hook requirement
        for (var hookRequirement : registeredPlugin.getHookRequirements()) {
            boolean success = processIndividualHook(hookRequirement.getPluginName(), 
                                                  hookRequirement.getMinVersion(), 
                                                  hookRequirement.isRequired());
            
            hookResult.addResult(hookRequirement.getPluginName(), success, 
                               success ? "Successfully hooked" : "Failed to hook");
            
            // If a required hook fails, mark overall as failed
            if (hookRequirement.isRequired() && !success) {
                allRequiredSuccessful = false;
            }
        }
        
        hookResult.setAllRequiredSuccessful(allRequiredSuccessful);
        
        if (allRequiredSuccessful) {
            // plugin.getLogger().info("All required hooks processed successfully!"); - STARTUP LOGGER
        } else {
            // plugin.getLogger().warning("One or more required hooks failed!"); - STARTUP LOGGER
        }
        
        return allRequiredSuccessful;
    }
    
    /**
     * Process an individual hook
     * @param pluginName The name of the plugin to hook
     * @param minVersion The minimum version required
     * @param required Whether this hook is required
     * @return True if the hook was successful
     */
    private boolean processIndividualHook(String pluginName, String minVersion, boolean required) {
        Plugin targetPlugin = Bukkit.getPluginManager().getPlugin(pluginName);
        
        if (targetPlugin == null || !targetPlugin.isEnabled()) {
            if (required) {
                // plugin.getLogger().warning("Required plugin " + pluginName + " is not available!"); - STARTUP LOGGER
            } else {
                // plugin.getLogger().info("Optional plugin " + pluginName + " is not available - skipping."); - STARTUP LOGGER
            }
            return false;
        }
        
        // Check version compatibility
        if (!isVersionCompatible(targetPlugin.getDescription().getVersion(), minVersion)) {
            if (required) {
                /* plugin.getLogger().warning("Required plugin " + pluginName + " version " + 
                                         targetPlugin.getDescription().getVersion() + 
                                         " is below minimum required version " + minVersion); */
                // STARTUP LOGGER
            } else {
                /* plugin.getLogger().info("Optional plugin " + pluginName + " version " + 
                                      targetPlugin.getDescription().getVersion() + 
                                      " is below recommended version " + minVersion); */
                // STARTUP LOGGER
            }
            return false;
        }
        
        // Perform specific hook initialization based on plugin
        boolean hookSuccess = initializeSpecificHook(pluginName, targetPlugin);
        
        if (hookSuccess) {
            /* plugin.getLogger().info("Successfully hooked into " + pluginName + " v" + 
                                  targetPlugin.getDescription().getVersion());*/
            // STARTUP LOGGER
        } else {
            // plugin.getLogger().warning("Failed to hook into " + pluginName); - STARTUP LOGGER
        }
        
        return hookSuccess;
    }
    
    /**
     * Initialize specific hooks for different plugins
     * @param pluginName The name of the plugin
     * @param targetPlugin The plugin instance
     * @return True if initialization was successful
     */
    private boolean initializeSpecificHook(String pluginName, Plugin targetPlugin) {
        switch (pluginName) {
            case "PlaceholderAPI":
                return initializePlaceholderAPIHook(targetPlugin);
            case "LuckPerms":
                return initializeLuckPermsHook(targetPlugin);
            case "VaultAPI":
            case "Vault":
                return initializeVaultHook(targetPlugin);
            case "Nexo":
                return initializeNexoHook(targetPlugin);
            default:
                plugin.getLogger().warning("Unknown plugin hook: " + pluginName);
                return false;
        }
    }
    
    /**
     * Initialize PlaceholderAPI hook
     */
    private boolean initializePlaceholderAPIHook(Plugin placeholderAPI) {
        try {
            // TODO: Initialize PlaceholderAPI integration
            // Example: Register placeholders, etc.
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to initialize PlaceholderAPI hook", e);
            return false;
        }
    }
    
    /**
     * Initialize LuckPerms hook
     */
    private boolean initializeLuckPermsHook(Plugin luckPerms) {
        try {
            // TODO: Initialize LuckPerms integration
            // Example: Get LuckPerms API, setup permission checking, etc.
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to initialize LuckPerms hook", e);
            return false;
        }
    }
    
    /**
     * Initialize Vault hook
     */
    private boolean initializeVaultHook(Plugin vault) {
        try {
            // TODO: Initialize Vault integration
            // Example: Setup economy, permissions, etc.
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to initialize Vault hook", e);
            return false;
        }
    }
    
    /**
     * Initialize Nexo hook
     */
    private boolean initializeNexoHook(Plugin nexo) {
        try {
            // TODO: Initialize Nexo integration
            // Example: Custom item integration, etc.
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to initialize Nexo hook", e);
            return false;
        }
    }
    
    /**
     * Simple version compatibility check
     * @param actualVersion The actual version of the plugin
     * @param minVersion The minimum required version
     * @return True if compatible
     */
    private boolean isVersionCompatible(String actualVersion, String minVersion) {
        if (minVersion.equals("any")) return true;
        
        try {
            // Remove any non-numeric suffixes for comparison
            String cleanActual = actualVersion.replaceAll("[^0-9.]", "");
            String cleanMin = minVersion.replaceAll("[^0-9.]", "");
            
            String[] actualParts = cleanActual.split("\\.");
            String[] minParts = cleanMin.split("\\.");
            
            for (int i = 0; i < Math.max(actualParts.length, minParts.length); i++) {
                int actualPart = i < actualParts.length ? Integer.parseInt(actualParts[i]) : 0;
                int minPart = i < minParts.length ? Integer.parseInt(minParts[i]) : 0;
                
                if (actualPart > minPart) return true;
                if (actualPart < minPart) return false;
            }
            
            return true; // Equal versions
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Failed to parse version numbers: " + actualVersion + " vs " + minVersion);
            return true; // Assume compatible if we can't parse
        }
    }
    
    @Override
    public HookResult getHookResults() {
        return hookResult;
    }
}