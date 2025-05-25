package io.rhythmknights.coretags.component.listener;

import io.rhythmknights.coretags.CoreTags;
import io.rhythmknights.coretags.component.data.PlayerDataProcessor;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.event.user.track.UserPromoteEvent;
import net.luckperms.api.event.user.track.UserDemoteEvent;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.event.node.NodeRemoveEvent;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Listens for LuckPerms changes and automatically syncs them to CoreTags player data
 */
public class LuckPermsListener {
    
    private final CoreTags plugin;
    private final PlayerDataProcessor dataProcessor;
    private LuckPerms luckPerms;
    private EventBus eventBus;
    
    // Store event subscriptions for proper cleanup
    private final List<EventSubscription<?>> subscriptions = new ArrayList<>();
    
    // Track recently processed changes to avoid duplicate processing
    private final Set<UUID> recentlyProcessed = ConcurrentHashMap.newKeySet();
    
    // Prefixes for CoreTags permissions
    private static final String CORETAGS_PREFIX = "coretags.";
    
    public LuckPermsListener(CoreTags plugin, PlayerDataProcessor dataProcessor) {
        this.plugin = plugin;
        this.dataProcessor = dataProcessor;
        registerLuckPermsEvents();
    }
    
    /**
     * Registers LuckPerms event listeners
     */
    private void registerLuckPermsEvents() {
        try {
            this.luckPerms = LuckPermsProvider.get();
            this.eventBus = luckPerms.getEventBus();
            
            // Register event listeners and store subscriptions for cleanup
            subscriptions.add(eventBus.subscribe(plugin, NodeAddEvent.class, this::onNodeAdd));
            subscriptions.add(eventBus.subscribe(plugin, NodeRemoveEvent.class, this::onNodeRemove));
            subscriptions.add(eventBus.subscribe(plugin, UserDataRecalculateEvent.class, this::onUserDataRecalculate));
            subscriptions.add(eventBus.subscribe(plugin, UserPromoteEvent.class, this::onUserPromote));
            subscriptions.add(eventBus.subscribe(plugin, UserDemoteEvent.class, this::onUserDemote));
            
            plugin.getLogger().info("Successfully registered LuckPerms event listeners");
            
        } catch (IllegalStateException e) {
            plugin.getLogger().severe("LuckPerms API not available! Cannot register event listeners.");
            this.luckPerms = null;
            this.eventBus = null;
        }
    }
    
    /**
     * Handles node addition events
     */
    private void onNodeAdd(NodeAddEvent event) {
        if (!(event.getTarget() instanceof User)) {
            return; // Only care about user events
        }
        
        User user = (User) event.getTarget();
        String permission = event.getNode().getKey();
        
        // Only process CoreTags-related permissions
        if (!permission.startsWith(CORETAGS_PREFIX)) {
            return;
        }
        
        UUID playerUUID = user.getUniqueId();
        
        plugin.getLogger().info("LuckPerms node added for " + playerUUID + ": " + permission);
        
        // Process the change with a delay to avoid rapid-fire updates
        scheduleSync(playerUUID, "Node added: " + permission);
    }
    
    /**
     * Handles node removal events
     */
    private void onNodeRemove(NodeRemoveEvent event) {
        if (!(event.getTarget() instanceof User)) {
            return; // Only care about user events
        }
        
        User user = (User) event.getTarget();
        String permission = event.getNode().getKey();
        
        // Only process CoreTags-related permissions
        if (!permission.startsWith(CORETAGS_PREFIX)) {
            return;
        }
        
        UUID playerUUID = user.getUniqueId();
        
        plugin.getLogger().info("LuckPerms node removed for " + playerUUID + ": " + permission);
        
        // Process the change with a delay to avoid rapid-fire updates
        scheduleSync(playerUUID, "Node removed: " + permission);
    }
    
    /**
     * Handles user data recalculation events (typically after bulk changes)
     */
    private void onUserDataRecalculate(UserDataRecalculateEvent event) {
        UUID playerUUID = event.getUser().getUniqueId();
        
        plugin.getLogger().info("LuckPerms data recalculated for " + playerUUID);
        
        // This event fires when user data is recalculated, often after web editor changes
        scheduleSync(playerUUID, "Data recalculated");
    }
    
    /**
     * Handles user promotion events
     */
    private void onUserPromote(UserPromoteEvent event) {
        UUID playerUUID = event.getUser().getUniqueId();
        String track = event.getTrack().getName();
        String fromGroup = event.getGroupFrom().orElse("none");
        String toGroup = event.getGroupTo().orElse("none");
        
        plugin.getLogger().info("User " + playerUUID + " promoted on track " + track + 
            " from " + fromGroup + " to " + toGroup);
        
        // Group changes might affect CoreTags permissions
        scheduleSync(playerUUID, "Promoted: " + fromGroup + " -> " + toGroup);
    }
    
    /**
     * Handles user demotion events
     */
    private void onUserDemote(UserDemoteEvent event) {
        UUID playerUUID = event.getUser().getUniqueId();
        String track = event.getTrack().getName();
        String fromGroup = event.getGroupFrom().orElse("none");
        String toGroup = event.getGroupTo().orElse("none");
        
        plugin.getLogger().info("User " + playerUUID + " demoted on track " + track + 
            " from " + fromGroup + " to " + toGroup);
        
        // Group changes might affect CoreTags permissions
        scheduleSync(playerUUID, "Demoted: " + fromGroup + " -> " + toGroup);
    }
    
    /**
     * Schedules a sync operation with duplicate prevention
     * @param playerUUID The player's UUID
     * @param reason The reason for the sync (for logging)
     */
    private void scheduleSync(UUID playerUUID, String reason) {
        // Check if we've recently processed this player to avoid duplicate syncs
        if (recentlyProcessed.contains(playerUUID)) {
            plugin.getLogger().fine("Skipping duplicate sync for " + playerUUID + " (" + reason + ")");
            return;
        }
        
        // Mark as recently processed
        recentlyProcessed.add(playerUUID);
        
        // Schedule the sync with a delay to batch multiple rapid changes
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    plugin.getLogger().info("Auto-syncing LuckPerms changes for " + playerUUID + " (" + reason + ")");
                    
                    // Use the data processor to sync from LuckPerms to player data
                    dataProcessor.syncLuckPermsToPlayerData(playerUUID).thenAccept(success -> {
                        if (success) {
                            plugin.getLogger().info("Successfully auto-synced LuckPerms changes for " + playerUUID);
                        } else {
                            plugin.getLogger().warning("Failed to auto-sync LuckPerms changes for " + playerUUID);
                        }
                        
                        // Schedule removal from recently processed set
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                recentlyProcessed.remove(playerUUID);
                            }
                        }.runTaskLater(plugin, 200L); // Remove after 10 seconds
                    });
                    
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Error during auto-sync for " + playerUUID, e);
                    recentlyProcessed.remove(playerUUID);
                }
            }
        }.runTaskLaterAsynchronously(plugin, 60L); // 3 second delay to batch changes
    }
    
    /**
     * Forces a sync for a specific player (used by admin commands)
     * @param playerUUID The player's UUID
     * @param reason The reason for the forced sync
     */
    public void forceSync(UUID playerUUID, String reason) {
        plugin.getLogger().info("Force syncing LuckPerms changes for " + playerUUID + " (" + reason + ")");
        
        // Remove from recently processed to allow immediate sync
        recentlyProcessed.remove(playerUUID);
        
        // Schedule immediate sync
        new BukkitRunnable() {
            @Override
            public void run() {
                dataProcessor.syncLuckPermsToPlayerData(playerUUID).thenAccept(success -> {
                    if (success) {
                        plugin.getLogger().info("Successfully force-synced LuckPerms changes for " + playerUUID);
                    } else {
                        plugin.getLogger().warning("Failed to force-sync LuckPerms changes for " + playerUUID);
                    }
                });
            }
        }.runTaskAsynchronously(plugin);
    }
    
    /**
     * Forces a sync for all online players
     */
    public void forceSyncAll() {
        plugin.getLogger().info("Force syncing LuckPerms changes for all online players");
        
        Bukkit.getOnlinePlayers().forEach(player -> {
            forceSync(player.getUniqueId(), "Force sync all");
        });
    }
    
    /**
     * Checks if a specific permission change affects CoreTags
     * @param permission The permission that changed
     * @return true if it affects CoreTags, false otherwise
     */
    private boolean affectsCoreTagsData(String permission) {
        return permission.startsWith("coretags.group.") ||
               permission.startsWith("coretags.category.") ||
               permission.startsWith("coretags.tag.");
    }
    
    /**
     * Gets detailed information about a permission change
     * @param permission The permission that changed
     * @return Detailed description of what changed
     */
    private String getPermissionChangeDetails(String permission) {
        if (permission.startsWith("coretags.group.")) {
            return "Group: " + permission.substring("coretags.group.".length());
        } else if (permission.startsWith("coretags.category.")) {
            return "Category: " + permission.substring("coretags.category.".length());
        } else if (permission.startsWith("coretags.tag.")) {
            return "Tag: " + permission.substring("coretags.tag.".length());
        } else {
            return "Other CoreTags permission";
        }
    }
    
    /**
     * Unregisters all LuckPerms event listeners
     */
    public void unregister() {
        try {
            // Close all event subscriptions
            for (EventSubscription<?> subscription : subscriptions) {
                if (subscription != null) {
                    subscription.close();
                }
            }
            subscriptions.clear();
            plugin.getLogger().info("Unregistered LuckPerms event listeners");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error unregistering LuckPerms listeners", e);
        }
        
        // Clear the recently processed set
        recentlyProcessed.clear();
    }
    
    /**
     * Checks if LuckPerms events are properly registered
     * @return true if registered, false otherwise
     */
    public boolean isRegistered() {
        return luckPerms != null && eventBus != null;
    }
    
    /**
     * Gets the number of players currently in the recently processed set
     * @return Number of players being rate-limited
     */
    public int getRecentlyProcessedCount() {
        return recentlyProcessed.size();
    }
    
    /**
     * Manually clears the recently processed set (for admin use)
     */
    public void clearRecentlyProcessed() {
        int count = recentlyProcessed.size();
        recentlyProcessed.clear();
        plugin.getLogger().info("Cleared recently processed set (" + count + " players)");
    }
    
    /**
     * Checks if a player is currently in the recently processed set
     * @param playerUUID The player's UUID
     * @return true if recently processed, false otherwise
     */
    public boolean isRecentlyProcessed(UUID playerUUID) {
        return recentlyProcessed.contains(playerUUID);
    }
}