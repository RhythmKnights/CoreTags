package io.rhythmknights.coretags.component.listener;

import io.rhythmknights.coretags.CoreTags;
import io.rhythmknights.coretags.component.data.PlayerDataProcessor;
import io.rhythmknights.coreframework.component.utility.TextUtility;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Handles player join/leave events for CoreTags data initialization
 */
public class PlayerEventListener implements Listener {
    
    private final CoreTags plugin;
    private final PlayerDataProcessor dataProcessor;
    
    public PlayerEventListener(CoreTags plugin, PlayerDataProcessor dataProcessor) {
        this.plugin = plugin;
        this.dataProcessor = dataProcessor;
    }
    
    /**
     * Handle player join - initialize their data if needed
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        plugin.getLogger().info("PlayerJoinEvent fired for: " + player.getName());
        
        // Run asynchronously to avoid blocking the main thread
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    plugin.getLogger().info("Processing join for player: " + player.getName() + " (" + player.getUniqueId() + ")");
                    
                    // Check if player has existing data, if not initialize from LuckPerms
                    if (!dataProcessor.hasPlayerData(player.getUniqueId())) {
                        plugin.getLogger().info("No existing data found, initializing data for new player: " + player.getName());
                        
                        dataProcessor.initializeNewPlayer(player.getUniqueId()).thenAccept(success -> {
                            if (success) {
                                plugin.getLogger().info("Successfully initialized data for " + player.getName());
                            } else {
                                plugin.getLogger().warning("Failed to initialize data for " + player.getName());
                            }
                        });
                    } else {
                        plugin.getLogger().info("Player " + player.getName() + " already has existing data");
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Error processing player join for " + player.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }
    
    /**
     * Handle player quit - optional cleanup or final data saves
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Optional: perform any cleanup or final data validation
        if (plugin.isDebugEnabled()) {
            plugin.getLogger().info("Player " + player.getName() + " disconnected");
        }
        
        // Note: We don't need to save data here as it's saved immediately when GUI closes
        // But we could add cleanup logic if needed
    }
}