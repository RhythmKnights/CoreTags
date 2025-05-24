package io.rhythmknights.coretags.component.hook.module;

import io.rhythmknights.coretags.CoreTags;
import io.rhythmknights.coreframework.component.utility.TextUtility;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import net.milkbowl.vault.economy.Economy;


/**
 * Hook for the Vault API plugin.
 * Provides integration with Vault and economy plugins.
 */
public class VaultAPIHook extends BaseHook {
    private Economy economy = null;
    
    /**
     * Constructor for VaultAPIHook
     * 
     * @param plugin The CoreTags plugin instance
     */
    public VaultAPIHook(CoreTags plugin) {
        super(plugin, "Vault", "1.7.3", false);
        
        // If Vault is available, also check for economy plugin
        if (isAvailable()) {
            setupEconomy();
        }
    }
    
    @Override
    protected boolean checkVersion(Plugin plugin) {
        try {
            String version = plugin.getDescription().getVersion();
            // Check if version is at least 1.7.3
            String[] versionParts = version.split("\\.");
            int major = Integer.parseInt(versionParts[0]);
            int minor = Integer.parseInt(versionParts[1]);
            int patch = Integer.parseInt(versionParts[2]);
            
            return major > 1 || (major == 1 && minor > 7) || (major == 1 && minor == 7 && patch >= 3);
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    protected String getConfigKey() {
        return "vault";
    }
    
    /**
     * Setup economy hook
     * @return true if economy is set up, false otherwise
     */
    private boolean setupEconomy() {
        if (!isAvailable()) return false;
        
        RegisteredServiceProvider<Economy> economyProvider = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
            return economy != null;
        } else {
            // Send economy missing message
            String missingMessage = plugin.getInternalConfig().getString("messages.hook.vault.economy.missing", "Economy plugin not found.");
            TextUtility.sendConsoleMessage(plugin.replaceVariables(missingMessage));
            
            // Send economy error message
            String errorMessage = plugin.getInternalConfig().getString("messages.hook.vault.economy.error", "Economy plugin with Vault support not found.");
            TextUtility.sendConsoleMessage(plugin.replaceVariables(errorMessage));
            
            return false;
        }
    }
    
    /**
     * Get the economy instance
     * @return The economy instance or null if not available
     */
    public Economy getEconomy() {
        return economy;
    }
    
    /**
     * Check if economy is available
     * @return true if economy is available, false otherwise
     */
    public boolean hasEconomy() {
        return economy != null;
    }
}