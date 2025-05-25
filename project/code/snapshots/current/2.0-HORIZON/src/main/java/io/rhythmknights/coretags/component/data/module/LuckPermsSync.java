package io.rhythmknights.coretags.component.data.module;

import io.rhythmknights.coretags.CoreTags;
import io.rhythmknights.coretags.component.data.PlayerData;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PermissionNode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Handles synchronization between CoreTags player data and LuckPerms permissions
 */
public class LuckPermsSync {
    
    private final CoreTags plugin;
    private LuckPerms luckPerms;
    
    // Permission node prefixes
    private static final String GROUP_PREFIX = "coretags.group.";
    private static final String CATEGORY_PREFIX = "coretags.category.";
    private static final String TAG_PREFIX = "coretags.tag.";
    
    public LuckPermsSync(CoreTags plugin) {
        this.plugin = plugin;
        initializeLuckPerms();
    }
    
    /**
     * Initializes LuckPerms API connection
     */
    private void initializeLuckPerms() {
        try {
            this.luckPerms = LuckPermsProvider.get();
            plugin.getLogger().info("Successfully connected to LuckPerms API");
        } catch (IllegalStateException e) {
            plugin.getLogger().severe("LuckPerms API not available! Make sure LuckPerms is installed and loaded.");
            this.luckPerms = null;
        }
    }
    
    /**
     * Pulls player data from LuckPerms and converts to PlayerData object
     * @param playerUUID The player's UUID
     * @return PlayerData object or null if not found/error
     */
    public PlayerData pullPlayerDataFromLuckPerms(UUID playerUUID) {
        if (luckPerms == null) {
            plugin.getLogger().warning("LuckPerms not available for data pull");
            return null;
        }
        
        try {
            UserManager userManager = luckPerms.getUserManager();
            CompletableFuture<User> userFuture = userManager.loadUser(playerUUID);
            
            User user = userFuture.join(); // This blocks, consider making async in production
            
            if (user == null) {
                plugin.getLogger().info("No LuckPerms user found for " + playerUUID);
                return createBasicPlayerData(playerUUID);
            }
            
            PlayerData playerData = new PlayerData();
            playerData.setUuid(playerUUID);
            
            // Extract group permissions
            String group = extractGroupFromPermissions(user);
            playerData.setGroup(group);
            
            // Extract categories
            List<String> categories = extractCategoriesFromPermissions(user);
            playerData.setCategories(categories);
            
            // Extract unlocked tags
            List<String> unlockedTags = extractTagsFromPermissions(user);
            playerData.setUnlockedTags(unlockedTags);
            
            // Set default active tag
            String defaultTag = plugin.getConfig().getString("default-tag", "none");
            playerData.setActiveTag(defaultTag);
            
            // Initialize empty favorites list
            playerData.setFavoritedTags(new ArrayList<>());
            
            plugin.getLogger().info("Successfully pulled data from LuckPerms for " + playerUUID);
            return playerData;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to pull data from LuckPerms for " + playerUUID, e);
            return null;
        }
    }
    
    /**
     * Syncs PlayerData to LuckPerms permissions
     * @param playerUUID The player's UUID
     * @param playerData The data to sync
     * @return true if successful, false otherwise
     */
    public boolean syncPlayerDataToLuckPerms(UUID playerUUID, PlayerData playerData) {
        if (luckPerms == null) {
            plugin.getLogger().warning("LuckPerms not available for data sync");
            return false;
        }
        
        if (playerData == null) {
            plugin.getLogger().warning("Cannot sync null player data to LuckPerms for " + playerUUID);
            return false;
        }
        
        try {
            UserManager userManager = luckPerms.getUserManager();
            CompletableFuture<User> userFuture = userManager.loadUser(playerUUID);
            
            User user = userFuture.join();
            
            if (user == null) {
                plugin.getLogger().warning("Could not load LuckPerms user for " + playerUUID);
                return false;
            }
            
            // Clear existing CoreTags permissions
            clearCoreTagsPermissions(user);
            
            // Add group permission
            if (playerData.getGroup() != null && !playerData.getGroup().isEmpty()) {
                String groupPerm = GROUP_PREFIX + playerData.getGroup();
                user.data().add(PermissionNode.builder(groupPerm).build());
            }
            
            // Add category permissions
            if (playerData.getCategories() != null) {
                for (String category : playerData.getCategories()) {
                    String categoryPerm = CATEGORY_PREFIX + category;
                    user.data().add(PermissionNode.builder(categoryPerm).build());
                }
            }
            
            // Add tag permissions
            if (playerData.getUnlockedTags() != null) {
                for (String tag : playerData.getUnlockedTags()) {
                    String tagPerm = TAG_PREFIX + tag;
                    user.data().add(PermissionNode.builder(tagPerm).build());
                }
            }
            
            // Save changes to LuckPerms
            userManager.saveUser(user);
            
            plugin.getLogger().info("Successfully synced data to LuckPerms for " + playerUUID);
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to sync data to LuckPerms for " + playerUUID, e);
            return false;
        }
    }
    
    /**
     * Extracts group permission from user's permissions
     * @param user The LuckPerms user
     * @return Group name or "player" as default
     */
    private String extractGroupFromPermissions(User user) {
        return user.getNodes().stream()
                .filter(node -> node.getType() == NodeType.PERMISSION)
                .map(Node::getKey)
                .filter(key -> key.startsWith(GROUP_PREFIX))
                .map(key -> key.substring(GROUP_PREFIX.length()))
                .findFirst()
                .orElse("player");
    }
    
    /**
     * Extracts category permissions from user's permissions
     * @param user The LuckPerms user
     * @return List of category names
     */
    private List<String> extractCategoriesFromPermissions(User user) {
        return user.getNodes().stream()
                .filter(node -> node.getType() == NodeType.PERMISSION)
                .map(Node::getKey)
                .filter(key -> key.startsWith(CATEGORY_PREFIX))
                .map(key -> key.substring(CATEGORY_PREFIX.length()))
                .collect(Collectors.toList());
    }
    
    /**
     * Extracts tag permissions from user's permissions
     * @param user The LuckPerms user
     * @return List of tag names
     */
    private List<String> extractTagsFromPermissions(User user) {
        return user.getNodes().stream()
                .filter(node -> node.getType() == NodeType.PERMISSION)
                .map(Node::getKey)
                .filter(key -> key.startsWith(TAG_PREFIX))
                .map(key -> key.substring(TAG_PREFIX.length()))
                .collect(Collectors.toList());
    }
    
    /**
     * Clears all CoreTags-related permissions from a user
     * @param user The LuckPerms user
     */
    private void clearCoreTagsPermissions(User user) {
        // Get all nodes that start with coretags prefixes
        List<Node> nodesToRemove = user.getNodes().stream()
                .filter(node -> node.getType() == NodeType.PERMISSION)
                .filter(node -> {
                    String key = node.getKey();
                    return key.startsWith(GROUP_PREFIX) || 
                           key.startsWith(CATEGORY_PREFIX) || 
                           key.startsWith(TAG_PREFIX);
                })
                .collect(Collectors.toList());
        
        // Remove all found nodes
        for (Node node : nodesToRemove) {
            user.data().remove(node);
        }
        
        plugin.getLogger().fine("Cleared " + nodesToRemove.size() + " CoreTags permissions for user " + user.getUniqueId());
    }
    
    /**
     * Creates basic player data with default values
     * @param playerUUID The player's UUID
     * @return Basic PlayerData object
     */
    private PlayerData createBasicPlayerData(UUID playerUUID) {
        PlayerData playerData = new PlayerData();
        playerData.setUuid(playerUUID);
        playerData.setGroup("player");
        
        String defaultTag = plugin.getConfig().getString("default-tag", "none");
        playerData.setActiveTag(defaultTag);
        
        playerData.setCategories(new ArrayList<>());
        playerData.setFavoritedTags(new ArrayList<>());
        playerData.setUnlockedTags(new ArrayList<>());
        
        return playerData;
    }
    
    /**
     * Checks if a player has a specific CoreTags permission in LuckPerms
     * @param playerUUID The player's UUID
     * @param permission The permission to check
     * @return true if player has permission, false otherwise
     */
    public boolean hasPermission(UUID playerUUID, String permission) {
        if (luckPerms == null) {
            return false;
        }
        
        try {
            UserManager userManager = luckPerms.getUserManager();
            CompletableFuture<User> userFuture = userManager.loadUser(playerUUID);
            User user = userFuture.join();
            
            if (user == null) {
                return false;
            }
            
            return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to check permission " + permission + " for " + playerUUID, e);
            return false;
        }
    }
    
    /**
     * Adds a specific permission to a player in LuckPerms
     * @param playerUUID The player's UUID
     * @param permission The permission to add
     * @return true if successful, false otherwise
     */
    public boolean addPermission(UUID playerUUID, String permission) {
        if (luckPerms == null) {
            return false;
        }
        
        try {
            UserManager userManager = luckPerms.getUserManager();
            CompletableFuture<User> userFuture = userManager.loadUser(playerUUID);
            User user = userFuture.join();
            
            if (user == null) {
                return false;
            }
            
            user.data().add(PermissionNode.builder(permission).build());
            userManager.saveUser(user);
            
            plugin.getLogger().fine("Added permission " + permission + " to player " + playerUUID);
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to add permission " + permission + " to " + playerUUID, e);
            return false;
        }
    }
    
    /**
     * Removes a specific permission from a player in LuckPerms
     * @param playerUUID The player's UUID
     * @param permission The permission to remove
     * @return true if successful, false otherwise
     */
    public boolean removePermission(UUID playerUUID, String permission) {
        if (luckPerms == null) {
            return false;
        }
        
        try {
            UserManager userManager = luckPerms.getUserManager();
            CompletableFuture<User> userFuture = userManager.loadUser(playerUUID);
            User user = userFuture.join();
            
            if (user == null) {
                return false;
            }
            
            user.data().remove(PermissionNode.builder(permission).build());
            userManager.saveUser(user);
            
            plugin.getLogger().fine("Removed permission " + permission + " from player " + playerUUID);
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to remove permission " + permission + " from " + playerUUID, e);
            return false;
        }
    }
    
    /**
     * Gets all CoreTags permissions for a player
     * @param playerUUID The player's UUID
     * @return List of CoreTags permissions
     */
    public List<String> getCoreTagsPermissions(UUID playerUUID) {
        if (luckPerms == null) {
            return new ArrayList<>();
        }
        
        try {
            UserManager userManager = luckPerms.getUserManager();
            CompletableFuture<User> userFuture = userManager.loadUser(playerUUID);
            User user = userFuture.join();
            
            if (user == null) {
                return new ArrayList<>();
            }
            
            return user.getNodes().stream()
                    .filter(node -> node.getType() == NodeType.PERMISSION)
                    .map(Node::getKey)
                    .filter(key -> key.startsWith("coretags."))
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get CoreTags permissions for " + playerUUID, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Checks if LuckPerms is available and connected
     * @return true if LuckPerms is available, false otherwise
     */
    public boolean isLuckPermsAvailable() {
        return luckPerms != null;
    }
    
    /**
     * Reloads the LuckPerms connection
     */
    public void reloadLuckPerms() {
        initializeLuckPerms();
    }
}