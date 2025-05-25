package io.rhythmknights.coretags.component.data;

import io.rhythmknights.coretags.CoreTags;
import io.rhythmknights.coretags.component.data.module.JSONDataModule;
import io.rhythmknights.coretags.component.data.module.YMLDataModule;
import io.rhythmknights.coretags.component.data.module.LuckPermsSync;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Main controller for player data processing and synchronization.
 * Handles delegation of tasks and ensures proper backup procedures.
 */
public class PlayerDataProcessor {
    
    private final CoreTags plugin;
    private final JSONDataModule jsonModule;
    private final YMLDataModule ymlModule;
    private final LuckPermsSync luckPermsSync;
    
    private final Path playerDataPath;
    private final Path jsonDataPath;
    
    public PlayerDataProcessor(CoreTags plugin) {
        this.plugin = plugin;
        this.jsonModule = new JSONDataModule(plugin);
        this.ymlModule = new YMLDataModule(plugin);
        this.luckPermsSync = new LuckPermsSync(plugin);
        
        // Setup directory paths
        this.playerDataPath = plugin.getDataFolder().toPath().resolve("playerdata");
        this.jsonDataPath = playerDataPath.resolve("json");
        
        // Ensure directories exist
        createDirectories();
    }
    
    /**
     * Creates necessary directories for player data storage
     */
    private void createDirectories() {
        try {
            plugin.getLogger().info("Creating player data directories...");
            plugin.getLogger().info("Plugin data folder: " + plugin.getDataFolder().getAbsolutePath());
            plugin.getLogger().info("Player data path: " + playerDataPath.toAbsolutePath());
            plugin.getLogger().info("JSON data path: " + jsonDataPath.toAbsolutePath());
            
            Files.createDirectories(playerDataPath);
            Files.createDirectories(jsonDataPath);
            
            plugin.getLogger().info("Player data directories created successfully");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create player data directories", e);
        }
    }
    
    /**
     * Initializes a new player's data by pulling from LuckPerms and creating default JSON
     */
    public CompletableFuture<Boolean> initializeNewPlayer(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check if player already has data
                if (hasPlayerData(playerUUID)) {
                    plugin.getLogger().info("Player " + playerUUID + " already has data, skipping initialization");
                    return true;
                }
                
                plugin.getLogger().info("Initializing new player data for " + playerUUID);
                
                // Pull initial data from LuckPerms
                PlayerData initialData = luckPermsSync.pullPlayerDataFromLuckPerms(playerUUID);
                
                // If no LuckPerms data found, create minimal default
                if (initialData == null) {
                    initialData = createDefaultPlayerData(playerUUID);
                }
                
                // Save initial JSON
                boolean success = jsonModule.savePlayerData(playerUUID, initialData);
                
                if (success) {
                    // Also create initial YAML copy
                    ymlModule.convertJsonToYml(playerUUID);
                    plugin.getLogger().info("Successfully initialized player data for " + playerUUID);
                } else {
                    plugin.getLogger().warning("Failed to initialize player data for " + playerUUID);
                }
                
                return success;
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error initializing player " + playerUUID, e);
                return false;
            }
        });
    }
    
    /**
     * Processes player data changes when GUI is closed
     */
    public CompletableFuture<Boolean> processPlayerDataUpdate(UUID playerUUID, PlayerData updatedData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                plugin.getLogger().info("Processing data update for player " + playerUUID);
                
                // Step 1: Create backup of current JSON
                if (!createJsonBackup(playerUUID)) {
                    plugin.getLogger().warning("Failed to create JSON backup for " + playerUUID);
                    return false;
                }
                
                // Step 2: Save new JSON data
                if (!jsonModule.savePlayerData(playerUUID, updatedData)) {
                    plugin.getLogger().severe("Failed to save updated JSON for " + playerUUID);
                    return false;
                }
                
                // Add small delay to prevent race conditions
                Thread.sleep(500);
                
                // Step 3: Sync to LuckPerms
                if (!luckPermsSync.syncPlayerDataToLuckPerms(playerUUID, updatedData)) {
                    plugin.getLogger().warning("Failed to sync to LuckPerms for " + playerUUID);
                    // Don't return false here as JSON is still updated
                }
                
                // Step 4: Create YAML backup and update
                createYmlBackup(playerUUID);
                ymlModule.convertJsonToYml(playerUUID);
                
                plugin.getLogger().info("Successfully processed data update for " + playerUUID);
                return true;
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error processing player data update for " + playerUUID, e);
                return false;
            }
        });
    }
    
    /**
     * Syncs player data from YAML/JSON to LuckPerms
     */
    public CompletableFuture<Boolean> syncPlayerDataToLuckPerms(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                plugin.getLogger().info("Syncing playerdata to LuckPerms for " + playerUUID);
                
                // Determine which file is newer
                Path jsonPath = getJsonPath(playerUUID);
                Path ymlPath = getYmlPath(playerUUID);
                
                PlayerData dataToSync;
                
                if (Files.exists(ymlPath) && Files.exists(jsonPath)) {
                    long ymlTime = Files.getLastModifiedTime(ymlPath).toMillis();
                    long jsonTime = Files.getLastModifiedTime(jsonPath).toMillis();
                    
                    if (ymlTime > jsonTime) {
                        // YAML is newer, convert to JSON first
                        plugin.getLogger().info("YAML is newer, converting to JSON first");
                        createJsonBackup(playerUUID);
                        ymlModule.convertYmlToJson(playerUUID);
                    }
                }
                
                // Load current JSON data
                dataToSync = jsonModule.loadPlayerData(playerUUID);
                if (dataToSync == null) {
                    plugin.getLogger().warning("No player data found for " + playerUUID);
                    return false;
                }
                
                // Sync to LuckPerms
                return luckPermsSync.syncPlayerDataToLuckPerms(playerUUID, dataToSync);
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error syncing playerdata to LuckPerms for " + playerUUID, e);
                return false;
            }
        });
    }
    
    /**
     * Syncs player data from LuckPerms to JSON/YAML
     */
    public CompletableFuture<Boolean> syncLuckPermsToPlayerData(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                plugin.getLogger().info("Syncing LuckPerms to playerdata for " + playerUUID);
                
                // Pull data from LuckPerms
                PlayerData luckPermsData = luckPermsSync.pullPlayerDataFromLuckPerms(playerUUID);
                if (luckPermsData == null) {
                    plugin.getLogger().warning("No LuckPerms data found for " + playerUUID);
                    return false;
                }
                
                // Create backups
                createJsonBackup(playerUUID);
                createYmlBackup(playerUUID);
                
                // Save new JSON
                if (!jsonModule.savePlayerData(playerUUID, luckPermsData)) {
                    plugin.getLogger().severe("Failed to save JSON from LuckPerms data for " + playerUUID);
                    return false;
                }
                
                // Update YAML
                ymlModule.convertJsonToYml(playerUUID);
                
                plugin.getLogger().info("Successfully synced LuckPerms to playerdata for " + playerUUID);
                return true;
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error syncing LuckPerms to playerdata for " + playerUUID, e);
                return false;
            }
        });
    }
    
    /**
     * Handles LuckPerms changes detected by the listener
     */
    public void handleLuckPermsChange(UUID playerUUID) {
        // Add delay to ensure LuckPerms changes are fully applied
        new BukkitRunnable() {
            @Override
            public void run() {
                syncLuckPermsToPlayerData(playerUUID).thenAccept(success -> {
                    if (success) {
                        plugin.getLogger().info("Automatically synced LuckPerms changes for " + playerUUID);
                    } else {
                        plugin.getLogger().warning("Failed to auto-sync LuckPerms changes for " + playerUUID);
                    }
                });
            }
        }.runTaskLaterAsynchronously(plugin, 60L); // 3 second delay
    }
    
    /**
     * Creates a backup of the player's JSON file
     */
    private boolean createJsonBackup(UUID playerUUID) {
        try {
            Path jsonPath = getJsonPath(playerUUID);
            if (!Files.exists(jsonPath)) {
                return true; // No file to backup
            }
            
            Path backupPath = getJsonBackupPath(playerUUID);
            Files.copy(jsonPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            return true;
            
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create JSON backup for " + playerUUID, e);
            return false;
        }
    }
    
    /**
     * Creates a backup of the player's YAML file
     */
    private boolean createYmlBackup(UUID playerUUID) {
        try {
            Path ymlPath = getYmlPath(playerUUID);
            if (!Files.exists(ymlPath)) {
                return true; // No file to backup
            }
            
            Path backupPath = getYmlBackupPath(playerUUID);
            Files.copy(ymlPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            return true;
            
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create YAML backup for " + playerUUID, e);
            return false;
        }
    }
    
    /**
     * Creates default player data structure
     */
    private PlayerData createDefaultPlayerData(UUID playerUUID) {
        PlayerData data = new PlayerData();
        data.setUuid(playerUUID);
        data.setGroup("player"); // Default group
        
        // Get default tag from configuration
        String defaultTag = plugin.getConfig().getString("default-tag", "none");
        data.setActiveTag(defaultTag);
        
        // Initialize empty lists
        data.setCategories(new ArrayList<>());
        data.setFavoritedTags(new ArrayList<>());
        data.setUnlockedTags(new ArrayList<>());
        
        return data;
    }
    
    // Utility methods for file paths
    public Path getJsonPath(UUID playerUUID) {
        return jsonDataPath.resolve(playerUUID.toString() + ".json");
    }
    
    public Path getJsonBackupPath(UUID playerUUID) {
        return jsonDataPath.resolve(playerUUID.toString() + ".json.backup");
    }
    
    public Path getYmlPath(UUID playerUUID) {
        return playerDataPath.resolve(playerUUID.toString() + ".yml");
    }
    
    public Path getYmlBackupPath(UUID playerUUID) {
        return playerDataPath.resolve(playerUUID.toString() + ".yml.backup");
    }
    
    public boolean hasPlayerData(UUID playerUUID) {
        return Files.exists(getJsonPath(playerUUID));
    }
    
    // Getters for modules
    public JSONDataModule getJsonModule() {
        return jsonModule;
    }
    
    public YMLDataModule getYmlModule() {
        return ymlModule;
    }
    
    public LuckPermsSync getLuckPermsSync() {
        return luckPermsSync;
    }
}