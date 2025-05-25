package io.rhythmknights.coretags.component.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Data class representing a player's CoreTags information
 */
public class PlayerData {
    
    private UUID uuid;
    private String group;
    private List<String> categories;
    private String activeTag;
    private List<String> favoritedTags;
    private List<String> unlockedTags;
    
    /**
     * Default constructor
     */
    public PlayerData() {
        this.categories = new ArrayList<>();
        this.favoritedTags = new ArrayList<>();
        this.unlockedTags = new ArrayList<>();
    }
    
    /**
     * Constructor with UUID
     * @param uuid The player's UUID
     */
    public PlayerData(UUID uuid) {
        this();
        this.uuid = uuid;
    }
    
    /**
     * Full constructor
     * @param uuid The player's UUID
     * @param group The player's group
     * @param categories List of visible categories
     * @param activeTag Currently active tag
     * @param favoritedTags List of favorited tags
     * @param unlockedTags List of unlocked tags
     */
    public PlayerData(UUID uuid, String group, List<String> categories, String activeTag, 
                     List<String> favoritedTags, List<String> unlockedTags) {
        this.uuid = uuid;
        this.group = group;
        this.categories = categories != null ? new ArrayList<>(categories) : new ArrayList<>();
        this.activeTag = activeTag;
        this.favoritedTags = favoritedTags != null ? new ArrayList<>(favoritedTags) : new ArrayList<>();
        this.unlockedTags = unlockedTags != null ? new ArrayList<>(unlockedTags) : new ArrayList<>();
    }
    
    // Getters and Setters
    
    public UUID getUuid() {
        return uuid;
    }
    
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
    
    public String getGroup() {
        return group;
    }
    
    public void setGroup(String group) {
        this.group = group;
    }
    
    public List<String> getCategories() {
        return categories;
    }
    
    public void setCategories(List<String> categories) {
        this.categories = categories != null ? new ArrayList<>(categories) : new ArrayList<>();
    }
    
    public String getActiveTag() {
        return activeTag;
    }
    
    public void setActiveTag(String activeTag) {
        this.activeTag = activeTag;
    }
    
    public List<String> getFavoritedTags() {
        return favoritedTags;
    }
    
    public void setFavoritedTags(List<String> favoritedTags) {
        this.favoritedTags = favoritedTags != null ? new ArrayList<>(favoritedTags) : new ArrayList<>();
    }
    
    public List<String> getUnlockedTags() {
        return unlockedTags;
    }
    
    public void setUnlockedTags(List<String> unlockedTags) {
        this.unlockedTags = unlockedTags != null ? new ArrayList<>(unlockedTags) : new ArrayList<>();
    }
    
    // Utility methods
    
    /**
     * Adds a category to the player's visible categories
     * @param category The category to add
     * @return true if added, false if already exists
     */
    public boolean addCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return false;
        }
        
        if (!categories.contains(category)) {
            categories.add(category);
            return true;
        }
        return false;
    }
    
    /**
     * Removes a category from the player's visible categories
     * @param category The category to remove
     * @return true if removed, false if not found
     */
    public boolean removeCategory(String category) {
        return categories.remove(category);
    }
    
    /**
     * Checks if the player has access to a specific category
     * @param category The category to check
     * @return true if player has access, false otherwise
     */
    public boolean hasCategory(String category) {
        return categories.contains(category);
    }
    
    /**
     * Adds a tag to the player's favorited tags
     * @param tag The tag to favorite
     * @return true if added, false if already favorited
     */
    public boolean addFavoritedTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return false;
        }
        
        if (!favoritedTags.contains(tag)) {
            favoritedTags.add(tag);
            return true;
        }
        return false;
    }
    
    /**
     * Removes a tag from the player's favorited tags
     * @param tag The tag to unfavorite
     * @return true if removed, false if not found
     */
    public boolean removeFavoritedTag(String tag) {
        return favoritedTags.remove(tag);
    }
    
    /**
     * Checks if a tag is favorited by the player
     * @param tag The tag to check
     * @return true if favorited, false otherwise
     */
    public boolean isFavoritedTag(String tag) {
        return favoritedTags.contains(tag);
    }
    
    /**
     * Unlocks a tag for the player
     * @param tag The tag to unlock
     * @return true if unlocked, false if already unlocked
     */
    public boolean unlockTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return false;
        }
        
        if (!unlockedTags.contains(tag)) {
            unlockedTags.add(tag);
            return true;
        }
        return false;
    }
    
    /**
     * Locks a tag for the player (removes from unlocked)
     * @param tag The tag to lock
     * @return true if locked, false if not found
     */
    public boolean lockTag(String tag) {
        boolean removed = unlockedTags.remove(tag);
        
        // If we're locking the currently active tag, reset to default
        if (removed && tag.equals(activeTag)) {
            activeTag = "none"; // or get default from config
        }
        
        return removed;
    }
    
    /**
     * Checks if a tag is unlocked for the player
     * @param tag The tag to check
     * @return true if unlocked, false otherwise
     */
    public boolean hasUnlockedTag(String tag) {
        return unlockedTags.contains(tag);
    }
    
    /**
     * Checks if the player can use a specific tag (must be unlocked)
     * @param tag The tag to check
     * @return true if can use, false otherwise
     */
    public boolean canUseTag(String tag) {
        return hasUnlockedTag(tag);
    }
    
    /**
     * Sets the active tag if the player has it unlocked
     * @param tag The tag to set as active
     * @return true if set successfully, false if tag not unlocked
     */
    public boolean setActiveTagIfUnlocked(String tag) {
        if (tag == null || tag.equals("none") || hasUnlockedTag(tag)) {
            this.activeTag = tag;
            return true;
        }
        return false;
    }
    
    /**
     * Gets the count of unlocked tags
     * @return Number of unlocked tags
     */
    public int getUnlockedTagCount() {
        return unlockedTags.size();
    }
    
    /**
     * Gets the count of favorited tags
     * @return Number of favorited tags
     */
    public int getFavoritedTagCount() {
        return favoritedTags.size();
    }
    
    /**
     * Gets the count of accessible categories
     * @return Number of accessible categories
     */
    public int getCategoryCount() {
        return categories.size();
    }
    
    /**
     * Checks if the player has admin privileges
     * @return true if admin, false otherwise
     */
    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(group);
    }
    
    /**
     * Checks if the player has basic player privileges
     * @return true if player or higher, false otherwise
     */
    public boolean isPlayer() {
        return "player".equalsIgnoreCase(group) || isAdmin();
    }
    
    /**
     * Creates a copy of this PlayerData object
     * @return A new PlayerData object with the same values
     */
    public PlayerData copy() {
        return new PlayerData(uuid, group, categories, activeTag, favoritedTags, unlockedTags);
    }
    
    /**
     * Validates the integrity of the player data
     * @return true if data is valid, false otherwise
     */
    public boolean isValid() {
        return uuid != null && 
               group != null && !group.trim().isEmpty() &&
               categories != null &&
               favoritedTags != null &&
               unlockedTags != null;
    }
    
    /**
     * Clears all player data except UUID
     */
    public void clear() {
        this.group = "player";
        this.categories.clear();
        this.activeTag = "none";
        this.favoritedTags.clear();
        this.unlockedTags.clear();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerData that = (PlayerData) o;
        return Objects.equals(uuid, that.uuid) &&
               Objects.equals(group, that.group) &&
               Objects.equals(categories, that.categories) &&
               Objects.equals(activeTag, that.activeTag) &&
               Objects.equals(favoritedTags, that.favoritedTags) &&
               Objects.equals(unlockedTags, that.unlockedTags);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(uuid, group, categories, activeTag, favoritedTags, unlockedTags);
    }
    
    @Override
    public String toString() {
        return "PlayerData{" +
                "uuid=" + uuid +
                ", group='" + group + '\'' +
                ", categories=" + categories +
                ", activeTag='" + activeTag + '\'' +
                ", favoritedTags=" + favoritedTags +
                ", unlockedTags=" + unlockedTags +
                '}';
    }
    
    /**
     * Returns a formatted string representation for debugging
     * @return Formatted string with player data details
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PlayerData Details:\n");
        sb.append("  UUID: ").append(uuid).append("\n");
        sb.append("  Group: ").append(group).append("\n");
        sb.append("  Active Tag: ").append(activeTag).append("\n");
        sb.append("  Categories (").append(categories.size()).append("): ").append(categories).append("\n");
        sb.append("  Favorited Tags (").append(favoritedTags.size()).append("): ").append(favoritedTags).append("\n");
        sb.append("  Unlocked Tags (").append(unlockedTags.size()).append("): ").append(unlockedTags);
        return sb.toString();
    }
}