package io.rhythmknights.coretags.component.hook.module;

import io.rhythmknights.coretags.CoreTags;
import io.rhythmknights.coreframework.component.utility.TextUtility;
import org.bukkit.plugin.Plugin;


/**
 * Hook for the Nexo plugin.
 * Handles integration with Nexo functionality.
 */
public class NexoHook extends BaseHook {
    
    /**
     * Constructor for NexoHook
     * 
     * @param plugin The CoreTags plugin instance
     */
    public NexoHook(CoreTags plugin) {
        super(plugin, "Nexo", "any", false);
    }
    
    @Override
    protected boolean checkVersion(Plugin plugin) {
        // Any version is acceptable
        return true;
    }
    
    @Override
    protected String getConfigKey() {
        return "nexo";
    }
    
}