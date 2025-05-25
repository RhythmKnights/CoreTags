package io.rhythmknights.coretags.component.data.module;

import io.rhythmknights.coretags.CoreTags;
import io.rhythmknights.coretags.component.data.PlayerData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Handles all JSON file operations for player data
 */
public class JSONDataModule {
    
    private final CoreTags plugin;
    private final Gson gson;
    
    public JSONDataModule(CoreTags plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
    }
    
    /**
     * Loads player data from JSON file
     * @param playerUUID The player's UUID
     * @return PlayerData object or null if not found/error
     */
    public PlayerData loadPlayerData(UUID playerUUID) {
        Path jsonPath = getJsonPath(playerUUID);
        
        if (!Files.exists(jsonPath)) {
            plugin.getLogger().info("No JSON file found for player " + playerUUID);
            return null;
        }
        
        try {
            String jsonContent = Files.readString(jsonPath, StandardCharsets.UTF_8);
            
            if (jsonContent.trim().isEmpty()) {
                plugin.getLogger().warning("Empty JSON file for player " + playerUUID);
                return null;
            }
            
            PlayerData playerData = gson.fromJson(jsonContent, PlayerData.class);
            
            // Validate that UUID matches filename
            if (playerData != null && !playerUUID.equals(playerData.getUuid())) {
                plugin.getLogger().warning("UUID mismatch in JSON file for " + playerUUID + 
                    ". File UUID: " + playerData.getUuid());
                // Fix the UUID to match filename
                playerData.setUuid(playerUUID);
            }
            
            return playerData;
            
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to read JSON file for player " + playerUUID, e);
            return null;
        } catch (JsonSyntaxException e) {
            plugin.getLogger().log(Level.SEVERE, "Invalid JSON syntax for player " + playerUUID + 
                ". Consider restoring from backup.", e);
            return null;
        }
    }
    
    /**
     * Saves player data to JSON file
     * @param playerUUID The player's UUID
     * @param playerData The data to save
     * @return true if successful, false otherwise
     */
    public boolean savePlayerData(UUID playerUUID, PlayerData playerData) {
        if (playerData == null) {
            plugin.getLogger().warning("Attempted to save null player data for " + playerUUID);
            return false;
        }
        
        // Ensure UUID is set correctly
        playerData.setUuid(playerUUID);
        
        Path jsonPath = getJsonPath(playerUUID);
        
        try {
            // Ensure parent directory exists
            Files.createDirectories(jsonPath.getParent());
            
            // Convert to JSON string
            String jsonString = gson.toJson(playerData);
            
            // Validate JSON before writing
            if (!isValidJson(jsonString)) {
                plugin.getLogger().severe("Generated invalid JSON for player " + playerUUID);
                return false;
            }
            
            // Write to temporary file first, then rename (atomic operation)
            Path tempPath = jsonPath.resolveSibling(jsonPath.getFileName() + ".tmp");
            Files.writeString(tempPath, jsonString, StandardCharsets.UTF_8);
            Files.move(tempPath, jsonPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            
            plugin.getLogger().fine("Successfully saved JSON data for player " + playerUUID);
            return true;
            
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save JSON file for player " + playerUUID, e);
            return false;
        }
    }
    
    /**
     * Validates if a string is valid JSON
     * @param jsonString The JSON string to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidJson(String jsonString) {
        try {
            gson.fromJson(jsonString, Object.class);
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }
    
    /**
     * Creates a deep copy of PlayerData object
     * @param original The original PlayerData
     * @return A deep copy of the PlayerData
     */
    public PlayerData clonePlayerData(PlayerData original) {
        if (original == null) {
            return null;
        }
        
        try {
            String jsonString = gson.toJson(original);
            return gson.fromJson(jsonString, PlayerData.class);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to clone PlayerData", e);
            return null;
        }
    }
    
    /**
     * Checks if a JSON file exists for the given player
     * @param playerUUID The player's UUID
     * @return true if file exists, false otherwise
     */
    public boolean hasJsonFile(UUID playerUUID) {
        return Files.exists(getJsonPath(playerUUID));
    }
    
    /**
     * Gets the file size of a player's JSON file
     * @param playerUUID The player's UUID
     * @return file size in bytes, or -1 if file doesn't exist
     */
    public long getJsonFileSize(UUID playerUUID) {
        Path jsonPath = getJsonPath(playerUUID);
        try {
            return Files.exists(jsonPath) ? Files.size(jsonPath) : -1;
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get file size for " + playerUUID, e);
            return -1;
        }
    }
    
    /**
     * Gets the last modified time of a player's JSON file
     * @param playerUUID The player's UUID
     * @return last modified time in milliseconds, or -1 if file doesn't exist
     */
    public long getJsonLastModified(UUID playerUUID) {
        Path jsonPath = getJsonPath(playerUUID);
        try {
            return Files.exists(jsonPath) ? Files.getLastModifiedTime(jsonPath).toMillis() : -1;
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get last modified time for " + playerUUID, e);
            return -1;
        }
    }
    
    /**
     * Deletes a player's JSON file
     * @param playerUUID The player's UUID
     * @return true if deleted successfully or file didn't exist, false otherwise
     */
    public boolean deleteJsonFile(UUID playerUUID) {
        Path jsonPath = getJsonPath(playerUUID);
        try {
            return Files.deleteIfExists(jsonPath);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to delete JSON file for " + playerUUID, e);
            return false;
        }
    }
    
    /**
     * Restores a player's JSON file from backup
     * @param playerUUID The player's UUID
     * @return true if restored successfully, false otherwise
     */
    public boolean restoreFromBackup(UUID playerUUID) {
        Path backupPath = getJsonBackupPath(playerUUID);
        Path jsonPath = getJsonPath(playerUUID);
        
        if (!Files.exists(backupPath)) {
            plugin.getLogger().warning("No backup file found for player " + playerUUID);
            return false;
        }
        
        try {
            Files.copy(backupPath, jsonPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Successfully restored JSON from backup for player " + playerUUID);
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to restore from backup for " + playerUUID, e);
            return false;
        }
    }
    
    /**
     * Validates the integrity of a player's JSON file
     * @param playerUUID The player's UUID
     * @return true if file is valid and readable, false otherwise
     */
    public boolean validateJsonFile(UUID playerUUID) {
        try {
            PlayerData data = loadPlayerData(playerUUID);
            return data != null && playerUUID.equals(data.getUuid());
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "JSON validation failed for " + playerUUID, e);
            return false;
        }
    }
    
    /**
     * Gets the path to a player's JSON file
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
    
    /**
     * Exports player data to a formatted JSON string for debugging
     * @param playerData The PlayerData to export
     * @return Formatted JSON string
     */
    public String exportToJsonString(PlayerData playerData) {
        if (playerData == null) {
            return "null";
        }
        
        try {
            return gson.toJson(playerData);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to export PlayerData to JSON string", e);
            return "{}";
        }
    }
    
    /**
     * Imports player data from a JSON string
     * @param jsonString The JSON string to import
     * @return PlayerData object or null if parsing failed
     */
    public PlayerData importFromJsonString(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }
        
        try {
            return gson.fromJson(jsonString, PlayerData.class);
        } catch (JsonSyntaxException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to import PlayerData from JSON string", e);
            return null;
        }
    }
}