package io.rhythmknights.coretags.component.data.module;

import io.rhythmknights.coretags.CoreTags;
import io.rhythmknights.coretags.component.data.PlayerData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.InvalidConfigurationException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Handles conversion between JSON and YAML formats for player data
 */
public class YMLDataModule {
    
    private final CoreTags plugin;
    private final JSONDataModule jsonModule;
    
    public YMLDataModule(CoreTags plugin) {
        this.plugin = plugin;
        this.jsonModule = new JSONDataModule(plugin);
    }
    
    /**
     * Converts a player's JSON data to YAML format
     * @param playerUUID The player's UUID
     * @return true if conversion successful, false otherwise
     */
    public boolean convertJsonToYml(UUID playerUUID) {
        try {
            // Load JSON data
            PlayerData playerData = jsonModule.loadPlayerData(playerUUID);
            if (playerData == null) {
                plugin.getLogger().warning("No JSON data found to convert for player " + playerUUID);
                return false;
            }
            
            // Convert to YAML
            return savePlayerDataAsYml(playerUUID, playerData);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to convert JSON to YAML for player " + playerUUID, e);
            return false;
        }
    }
    
    /**
     * Converts a player's YAML data to JSON format
     * @param playerUUID The player's UUID
     * @return true if conversion successful, false otherwise
     */
    public boolean convertYmlToJson(UUID playerUUID) {
        try {
            // Load YAML data
            PlayerData playerData = loadPlayerDataFromYml(playerUUID);
            if (playerData == null) {
                plugin.getLogger().warning("No YAML data found to convert for player " + playerUUID);
                return false;
            }
            
            // Save as JSON
            return jsonModule.savePlayerData(playerUUID, playerData);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to convert YAML to JSON for player " + playerUUID, e);
            return false;
        }
    }
    
    /**
     * Loads player data from YAML file
     * @param playerUUID The player's UUID
     * @return PlayerData object or null if not found/error
     */
    public PlayerData loadPlayerDataFromYml(UUID playerUUID) {
        Path ymlPath = getYmlPath(playerUUID);
        
        if (!Files.exists(ymlPath)) {
            plugin.getLogger().info("No YAML file found for player " + playerUUID);
            return null;
        }
        
        try {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.load(ymlPath.toFile());
            
            return convertYamlToPlayerData(yaml, playerUUID);
            
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load YAML file for player " + playerUUID, e);
            return null;
        }
    }
    
    /**
     * Saves player data as YAML file
     * @param playerUUID The player's UUID
     * @param playerData The data to save
     * @return true if successful, false otherwise
     */
    public boolean savePlayerDataAsYml(UUID playerUUID, PlayerData playerData) {
        if (playerData == null) {
            plugin.getLogger().warning("Attempted to save null player data as YAML for " + playerUUID);
            return false;
        }
        
        Path ymlPath = getYmlPath(playerUUID);
        
        try {
            // Ensure parent directory exists
            Files.createDirectories(ymlPath.getParent());
            
            YamlConfiguration yaml = new YamlConfiguration();
            convertPlayerDataToYaml(playerData, yaml);
            
            // Save to temporary file first, then rename (atomic operation)
            Path tempPath = ymlPath.resolveSibling(ymlPath.getFileName() + ".tmp");
            yaml.save(tempPath.toFile());
            Files.move(tempPath, ymlPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            
            plugin.getLogger().fine("Successfully saved YAML data for player " + playerUUID);
            return true;
            
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save YAML file for player " + playerUUID, e);
            return false;
        }
    }
    
    /**
     * Converts PlayerData object to YamlConfiguration
     * @param playerData The PlayerData to convert
     * @param yaml The YamlConfiguration to populate
     */
    private void convertPlayerDataToYaml(PlayerData playerData, YamlConfiguration yaml) {
        // Add header comment
        yaml.options().header("CoreTags Player Data\nPlayer UUID: " + playerData.getUuid() + 
            "\nLast Updated: " + new java.util.Date().toString() + 
            "\n\nThis file is automatically generated from JSON data.\n" +
            "Changes made here will be synced back to JSON and LuckPerms.");
        
        // Basic player info
        yaml.set("uuid", playerData.getUuid().toString());
        yaml.set("group", playerData.getGroup());
        yaml.set("active-tag", playerData.getActiveTag());
        
        // Categories
        if (playerData.getCategories() != null && !playerData.getCategories().isEmpty()) {
            yaml.set("categories", playerData.getCategories());
        } else {
            yaml.set("categories", new ArrayList<String>());
        }
        
        // Favorited tags
        if (playerData.getFavoritedTags() != null && !playerData.getFavoritedTags().isEmpty()) {
            yaml.set("favorited-tags", playerData.getFavoritedTags());
        } else {
            yaml.set("favorited-tags", new ArrayList<String>());
        }
        
        // Unlocked tags
        if (playerData.getUnlockedTags() != null && !playerData.getUnlockedTags().isEmpty()) {
            yaml.set("unlocked-tags", playerData.getUnlockedTags());
        } else {
            yaml.set("unlocked-tags", new ArrayList<String>());
        }
        
        // Add metadata
        yaml.set("metadata.created", System.currentTimeMillis());
        yaml.set("metadata.source", "json-conversion");
    }
    
    /**
     * Converts YamlConfiguration to PlayerData object
     * @param yaml The YamlConfiguration to read from
     * @param playerUUID The expected player UUID
     * @return PlayerData object
     */
    private PlayerData convertYamlToPlayerData(YamlConfiguration yaml, UUID playerUUID) {
        PlayerData playerData = new PlayerData();
        
        // UUID - validate it matches the filename
        String yamlUuidStr = yaml.getString("uuid");
        UUID yamlUuid = null;
        try {
            yamlUuid = UUID.fromString(yamlUuidStr);
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid UUID in YAML file for " + playerUUID + ": " + yamlUuidStr);
        }
        
        if (yamlUuid != null && !yamlUuid.equals(playerUUID)) {
            plugin.getLogger().warning("UUID mismatch in YAML file for " + playerUUID + 
                ". File UUID: " + yamlUuid);
        }
        
        playerData.setUuid(playerUUID); // Always use filename UUID
        
        // Group
        playerData.setGroup(yaml.getString("group", "player"));
        
        // Active tag
        playerData.setActiveTag(yaml.getString("active-tag", 
            plugin.getConfig().getString("default-tag", "none")));
        
        // Categories
        List<String> categories = yaml.getStringList("categories");
        playerData.setCategories(categories != null ? categories : new ArrayList<>());
        
        // Favorited tags
        List<String> favoritedTags = yaml.getStringList("favorited-tags");
        playerData.setFavoritedTags(favoritedTags != null ? favoritedTags : new ArrayList<>());
        
        // Unlocked tags
        List<String> unlockedTags = yaml.getStringList("unlocked-tags");
        playerData.setUnlockedTags(unlockedTags != null ? unlockedTags : new ArrayList<>());
        
        return playerData;
    }
    
    /**
     * Checks if a YAML file exists for the given player
     * @param playerUUID The player's UUID
     * @return true if file exists, false otherwise
     */
    public boolean hasYmlFile(UUID playerUUID) {
        return Files.exists(getYmlPath(playerUUID));
    }
    
    /**
     * Gets the last modified time of a player's YAML file
     * @param playerUUID The player's UUID
     * @return last modified time in milliseconds, or -1 if file doesn't exist
     */
    public long getYmlLastModified(UUID playerUUID) {
        Path ymlPath = getYmlPath(playerUUID);
        try {
            return Files.exists(ymlPath) ? Files.getLastModifiedTime(ymlPath).toMillis() : -1;
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get last modified time for YAML " + playerUUID, e);
            return -1;
        }
    }
    
    /**
     * Deletes a player's YAML file
     * @param playerUUID The player's UUID
     * @return true if deleted successfully or file didn't exist, false otherwise
     */
    public boolean deleteYmlFile(UUID playerUUID) {
        Path ymlPath = getYmlPath(playerUUID);
        try {
            return Files.deleteIfExists(ymlPath);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to delete YAML file for " + playerUUID, e);
            return false;
        }
    }
    
    /**
     * Restores a player's YAML file from backup
     * @param playerUUID The player's UUID
     * @return true if restored successfully, false otherwise
     */
    public boolean restoreYmlFromBackup(UUID playerUUID) {
        Path backupPath = getYmlBackupPath(playerUUID);
        Path ymlPath = getYmlPath(playerUUID);
        
        if (!Files.exists(backupPath)) {
            plugin.getLogger().warning("No YAML backup file found for player " + playerUUID);
            return false;
        }
        
        try {
            Files.copy(backupPath, ymlPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Successfully restored YAML from backup for player " + playerUUID);
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to restore YAML from backup for " + playerUUID, e);
            return false;
        }
    }
    
    /**
     * Validates the integrity of a player's YAML file
     * @param playerUUID The player's UUID
     * @return true if file is valid and readable, false otherwise
     */
    public boolean validateYmlFile(UUID playerUUID) {
        try {
            PlayerData data = loadPlayerDataFromYml(playerUUID);
            return data != null && playerUUID.equals(data.getUuid());
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "YAML validation failed for " + playerUUID, e);
            return false;
        }
    }
    
    /**
     * Compares timestamps to determine which file is newer
     * @param playerUUID The player's UUID
     * @return "json" if JSON is newer, "yaml" if YAML is newer, "equal" if same, "none" if neither exists
     */
    public String compareFileTimestamps(UUID playerUUID) {
        long jsonTime = jsonModule.getJsonLastModified(playerUUID);
        long yamlTime = getYmlLastModified(playerUUID);
        
        if (jsonTime == -1 && yamlTime == -1) {
            return "none";
        } else if (jsonTime == -1) {
            return "yaml";
        } else if (yamlTime == -1) {
            return "json";
        } else if (jsonTime > yamlTime) {
            return "json";
        } else if (yamlTime > jsonTime) {
            return "yaml";
        } else {
            return "equal";
        }
    }
    
    /**
     * Creates a backup copy of both JSON and YAML files for a player
     * @param playerUUID The player's UUID
     * @return true if both backups were successful (or files didn't exist), false otherwise
     */
    public boolean createFullBackup(UUID playerUUID) {
        boolean jsonBackup = true;
        boolean yamlBackup = true;
        
        // Backup JSON if it exists
        if (jsonModule.hasJsonFile(playerUUID)) {
            try {
                Path jsonPath = getJsonPath(playerUUID);
                Path jsonBackupPath = getJsonBackupPath(playerUUID);
                Files.copy(jsonPath, jsonBackupPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to backup JSON for " + playerUUID, e);
                jsonBackup = false;
            }
        }
        
        // Backup YAML if it exists
        if (hasYmlFile(playerUUID)) {
            try {
                Path yamlPath = getYmlPath(playerUUID);
                Path yamlBackupPath = getYmlBackupPath(playerUUID);
                Files.copy(yamlPath, yamlBackupPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to backup YAML for " + playerUUID, e);
                yamlBackup = false;
            }
        }
        
        return jsonBackup && yamlBackup;
    }
    
    /**
     * Synchronizes the newer file format to the older one
     * @param playerUUID The player's UUID
     * @return true if synchronization was successful, false otherwise
     */
    public boolean syncToNewerFile(UUID playerUUID) {
        String comparison = compareFileTimestamps(playerUUID);
        
        switch (comparison) {
            case "json":
                plugin.getLogger().info("JSON is newer, converting to YAML for " + playerUUID);
                return convertJsonToYml(playerUUID);
            case "yaml":
                plugin.getLogger().info("YAML is newer, converting to JSON for " + playerUUID);
                return convertYmlToJson(playerUUID);
            case "equal":
                plugin.getLogger().info("Files are equal timestamp, no sync needed for " + playerUUID);
                return true;
            case "none":
                plugin.getLogger().warning("No files exist for " + playerUUID);
                return false;
            default:
                return false;
        }
    }
    
    /**
     * Gets the path to a player's YAML file
     * @param playerUUID The player's UUID
     * @return Path to the YAML file
     */
    private Path getYmlPath(UUID playerUUID) {
        return plugin.getDataFolder().toPath()
                .resolve("playerdata")
                .resolve(playerUUID.toString() + ".yml");
    }
    
    /**
     * Gets the path to a player's YAML backup file
     * @param playerUUID The player's UUID
     * @return Path to the YAML backup file
     */
    private Path getYmlBackupPath(UUID playerUUID) {
        return plugin.getDataFolder().toPath()
                .resolve("playerdata")
                .resolve(playerUUID.toString() + ".yml.backup");
    }
    
    /**
     * Gets the path to a player's JSON file (for backup operations)
     * @param playerUUID The player's UUID
     * @return Path to the JSON file
     */
    private Path getJsonPath(UUID playerUUID) {
        return plugin.getDataFolder().toPath()
                .resolve("playerdata")
                .resolve("json")
                .resolve(playerUUID.toString() + ".json");
    }
    
    /**
     * Gets the path to a player's JSON backup file
     * @param playerUUID The player's UUID
     * @return Path to the JSON backup file
     */
    private Path getJsonBackupPath(UUID playerUUID) {
        return plugin.getDataFolder().toPath()
                .resolve("playerdata")
                .resolve("json")
                .resolve(playerUUID.toString() + ".json.backup");
    }
}