package io.rhythmknights.coretags.component.modal;

import io.rhythmknights.coretags.CoreTags;
import io.rhythmknights.coretags.component.modal.builder.TagsBuilder;
import io.rhythmknights.coreapi.component.modal.Modal;
import io.rhythmknights.coreapi.component.modal.PaginatedModal;
import io.rhythmknights.coreframework.component.utility.TextUtility;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles the tags selection modal for CoreTags
 * Now includes sort state management for category and color filtering
 */
public class TagsModal {
    
    private final CoreTags plugin;
    private final ModalProcessor modalProcessor;
    private final TagsBuilder tagsBuilder;
    
    // Sort state management - stores per-player sort preferences
    private final Map<UUID, SortState> playerSortStates = new ConcurrentHashMap<>();
    
    /**
     * Inner class to track sort state for a player
     */
    public static class SortState {
        private String currentCategory;
        private String currentColor;
        private List<String> availableCategories;
        private List<String> availableColors;
        private int categoryIndex;
        private int colorIndex;
        private boolean persistent = false; // Flag to prevent cleanup during refresh
        
        public SortState(String initialCategory) {
            this.currentCategory = initialCategory;
            this.currentColor = "all"; // Default color filter
            this.categoryIndex = 0;
            this.colorIndex = 0;
            this.availableCategories = new ArrayList<>();
            this.availableColors = new ArrayList<>();
        }
        
        // Getters and setters
        public String getCurrentCategory() { return currentCategory; }
        public void setCurrentCategory(String currentCategory) { this.currentCategory = currentCategory; }
        
        public String getCurrentColor() { return currentColor; }
        public void setCurrentColor(String currentColor) { this.currentColor = currentColor; }
        
        public List<String> getAvailableCategories() { return availableCategories; }
        public void setAvailableCategories(List<String> availableCategories) { 
            this.availableCategories = availableCategories;
        }
        
        public List<String> getAvailableColors() { return availableColors; }
        public void setAvailableColors(List<String> availableColors) { 
            this.availableColors = availableColors;
        }
        
        public int getCategoryIndex() { return categoryIndex; }
        public void setCategoryIndex(int categoryIndex) { this.categoryIndex = categoryIndex; }
        
        public int getColorIndex() { return colorIndex; }
        public void setColorIndex(int colorIndex) { this.colorIndex = colorIndex; }
        
        public boolean isPersistent() { return persistent; }
        public void setPersistent(boolean persistent) { this.persistent = persistent; }
    }
    
    /**
     * Constructor for TagsModal
     * 
     * @param plugin The CoreTags plugin instance
     * @param modalProcessor The modal processor for navigation
     */
    public TagsModal(CoreTags plugin, ModalProcessor modalProcessor) {
        this.plugin = plugin;
        this.modalProcessor = modalProcessor;
        this.tagsBuilder = new TagsBuilder(plugin, this);
    }
    
    /**
     * Opens the tags modal for a specific category
     * 
     * @param player The player to open the modal for
     * @param categoryId The category to display tags for
     */
    public void open(Player player, String categoryId) {
        // Initialize or update sort state for this player
        initializePlayerSortState(player, categoryId);
        
        // Get current sort state
        SortState sortState = playerSortStates.get(player.getUniqueId());
        String actualCategory = sortState.getCurrentCategory();
        
        // Get title template from config
        String titleTemplate = plugin.getInternalConfig().getString("settings.system.gui.titles.tags-menu", 
            "<gold>Tags</gold> <dark_grey>|</dark_grey> {category} <dark_grey>•</dark_grey> <grey>({currentpage}/{totalpages})</grey>");
        
        // Replace category placeholder
        String categoryName = getCategoryDisplayName(actualCategory);
        String titleText = titleTemplate.replace("{category}", categoryName);
        
        // Replace pagination placeholders (will be updated later)
        titleText = titleText.replace("{currentpage}", "1");
        titleText = titleText.replace("{totalpages}", "1");
        
        String processedTitle = plugin.replaceVariables(titleText);
        
        // Get number of rows from config
        int rows = plugin.getInternalConfig().getInt("gui.tags-menu.rows", 6);
        rows = Math.max(1, Math.min(6, rows)); // Clamp between 1-6
        
        // Create paginated modal using CoreAPI with proper formatting
        PaginatedModal modal;
        try {
            Component titleComponent = formatTooltipText(processedTitle);
            modal = Modal.paginated()
                .title(titleComponent)
                .rows(rows)
                .disableAllInteractions() // Prevent item manipulation
                .create();
        } catch (Exception e) {
            // Fallback with basic title
            plugin.getLogger().warning("Failed to set paginated modal title, using fallback: " + e.getMessage());
            modal = Modal.paginated()
                .rows(rows)
                .disableAllInteractions()
                .create();
        }
        
        // Let the builder populate the modal
        tagsBuilder.buildModal(modal, player, actualCategory);
        
        // Set up close action
        modal.setCloseModalAction(event -> {
            if (plugin.getInternalConfig().getBoolean("settings.system.debug", false)) {
                plugin.getLogger().info("Modal closing for player: " + player.getName());
            }
            
            // Check if this is a refresh operation (persistent state)
            SortState currentSortState = playerSortStates.get(player.getUniqueId());
            if (currentSortState != null && currentSortState.isPersistent()) {
                // This is a refresh - reset the persistent flag but don't clean up
                currentSortState.setPersistent(false);
                if (plugin.getInternalConfig().getBoolean("settings.system.debug", false)) {
                    plugin.getLogger().info("Modal close is part of refresh - preserving sort state");
                }
                return; // Don't send close message or clean up state
            }
            
            // Normal close - clean up sort state
            if (plugin.getInternalConfig().getBoolean("settings.system.debug", false)) {
                plugin.getLogger().info("Normal modal close - cleaning up sort state");
            }
            
            playerSortStates.remove(player.getUniqueId());
            
            String closeMessage = plugin.getInternalConfig().getString("messages.modal.tags-closed",
                "<grey>Tags menu closed.</grey>");
            TextUtility.sendPlayerMessage(player, plugin.replaceVariables(closeMessage));
        });
        
        // Update title with correct pagination info after modal is built
        updateModalTitle(modal, actualCategory);
        
        // Open the modal
        modal.open(player);
        
        // Log if debug is enabled
        if (plugin.getInternalConfig().getBoolean("settings.system.debug", false)) {
            plugin.getLogger().info("Opened tags modal for player: " + player.getName() + 
                " with category: " + actualCategory + ", color filter: " + sortState.getCurrentColor());
        }
    }
    
    /**
     * Initializes the sort state for a player
     * 
     * @param player The player
     * @param initialCategory The initial category to display
     */
    private void initializePlayerSortState(Player player, String initialCategory) {
        SortState sortState = new SortState(initialCategory);
        
        // Build available categories list (player has permission to see)
        List<String> availableCategories = buildAvailableCategoriesList(player);
        sortState.setAvailableCategories(availableCategories);
        
        // Set category index based on initial category
        int categoryIndex = availableCategories.indexOf(initialCategory);
        if (categoryIndex == -1) {
            categoryIndex = 0; // Fallback to first available
            if (!availableCategories.isEmpty()) {
                sortState.setCurrentCategory(availableCategories.get(0));
            }
        }
        sortState.setCategoryIndex(categoryIndex);
        
        // Build available colors list from language config
        List<String> availableColors = buildAvailableColorsList();
        sortState.setAvailableColors(availableColors);
        sortState.setColorIndex(0); // Start with "all"
        
        playerSortStates.put(player.getUniqueId(), sortState);
    }
    
    /**
     * Builds the list of categories this player can see
     * 
     * @param player The player
     * @return List of category IDs the player has permission to see
     */
    private List<String> buildAvailableCategoriesList(Player player) {
        List<String> categories = new ArrayList<>();
        
        // Always add "all" first
        categories.add("all");
        
        // Get categories from config and check permissions
        if (plugin.getCategoryConfig().contains("category")) {
            for (String categoryId : plugin.getCategoryConfig().getConfigurationSection("category").getKeys(false)) {
                // Skip "all" as we already added it
                if ("all".equalsIgnoreCase(categoryId)) {
                    continue;
                }
                
                // Check permission
                String permission = plugin.getCategoryConfig().getString("category." + categoryId + ".permission", "");
                if (permission.isEmpty() || player.hasPermission(permission)) {
                    categories.add(categoryId);
                }
            }
        }
        
        return categories;
    }
    
    /**
     * Builds the list of available colors from language config
     * 
     * @return List of color IDs in the order they appear in config
     */
    private List<String> buildAvailableColorsList() {
        List<String> colors = new ArrayList<>();
        
        // Get colors from language config in the order they're defined
        if (plugin.getInternalConfig().contains("settings.system.colors")) {
            for (String colorKey : plugin.getInternalConfig().getConfigurationSection("settings.system.colors").getKeys(false)) {
                colors.add(colorKey);
            }
        } else {
            // Fallback order if config is missing
            colors.addAll(Arrays.asList("all", "multi", "red", "orange", "yellow", "green", 
                "blue", "purple", "pink", "brown", "grey", "black", "white"));
        }
        
        return colors;
    }
    
    /**
     * Handles sort button clicks with navigation logic
     * 
     * @param player The player who clicked
     * @param sortType The sort type ("category" or "color")
     * @param direction The navigation direction
     */
    public void handleSortNavigation(Player player, String sortType, String direction) {
        if (plugin.getInternalConfig().getBoolean("settings.system.debug", false)) {
            plugin.getLogger().info("Sort navigation: " + player.getName() + " clicked " + sortType + " " + direction);
        }
        
        SortState sortState = playerSortStates.get(player.getUniqueId());
        if (sortState == null) {
            plugin.getLogger().warning("No sort state found for player " + player.getName());
            return;
        }
        
        boolean changed = false;
        String oldValue = "";
        String newValue = "";
        
        if ("category".equals(sortType)) {
            oldValue = sortState.getCurrentCategory();
            changed = navigateCategorySort(sortState, direction);
            newValue = sortState.getCurrentCategory();
        } else if ("color".equals(sortType)) {
            oldValue = sortState.getCurrentColor();
            changed = navigateColorSort(sortState, direction);
            newValue = sortState.getCurrentColor();
        }
        
        if (plugin.getInternalConfig().getBoolean("settings.system.debug", false)) {
            plugin.getLogger().info("Sort result: changed=" + changed + ", old=" + oldValue + ", new=" + newValue);
        }
        
        if (changed) {
            // Send feedback message before refreshing
            String sortTypeName = "category".equals(sortType) ? "category" : "color";
            String currentValue = "category".equals(sortType) ? 
                getCategoryDisplayName(sortState.getCurrentCategory()) : 
                getColorDisplayName(sortState.getCurrentColor());
            
            String message = plugin.getInternalConfig().getString("messages.modal.sort-changed",
                "<yellow>Changed {sorttype} to {value}.</yellow>");
            message = message.replace("{sorttype}", sortTypeName).replace("{value}", currentValue);
            TextUtility.sendPlayerMessage(player, plugin.replaceVariables(message));
            
            // Refresh the modal with new filters
            refreshModal(player);
        } else {
            if (plugin.getInternalConfig().getBoolean("settings.system.debug", false)) {
                plugin.getLogger().info("Sort navigation resulted in no change - not refreshing modal");
            }
        }
    }
    
    /**
     * Navigates the category sort
     * 
     * @param sortState The sort state to modify
     * @param direction The navigation direction
     * @return true if the sort changed, false otherwise
     */
    private boolean navigateCategorySort(SortState sortState, String direction) {
        List<String> categories = sortState.getAvailableCategories();
        if (categories.isEmpty()) {
            return false;
        }
        
        int currentIndex = sortState.getCategoryIndex();
        int newIndex = currentIndex;
        
        switch (direction.toLowerCase()) {
            case "next":
                newIndex = (currentIndex + 1) % categories.size();
                break;
            case "previous":
                newIndex = (currentIndex - 1 + categories.size()) % categories.size();
                break;
            case "first":
                newIndex = 0;
                break;
            case "last":
                newIndex = categories.size() - 1;
                break;
        }
        
        if (newIndex != currentIndex) {
            sortState.setCategoryIndex(newIndex);
            sortState.setCurrentCategory(categories.get(newIndex));
            return true;
        }
        
        return false;
    }
    
    /**
     * Navigates the color sort
     * 
     * @param sortState The sort state to modify
     * @param direction The navigation direction
     * @return true if the sort changed, false otherwise
     */
    private boolean navigateColorSort(SortState sortState, String direction) {
        List<String> colors = sortState.getAvailableColors();
        if (colors.isEmpty()) {
            return false;
        }
        
        int currentIndex = sortState.getColorIndex();
        int newIndex = currentIndex;
        
        switch (direction.toLowerCase()) {
            case "next":
                newIndex = (currentIndex + 1) % colors.size();
                break;
            case "previous":
                newIndex = (currentIndex - 1 + colors.size()) % colors.size();
                break;
            case "first":
                newIndex = 0;
                break;
            case "last":
                newIndex = colors.size() - 1;
                break;
        }
        
        if (newIndex != currentIndex) {
            sortState.setColorIndex(newIndex);
            sortState.setCurrentColor(colors.get(newIndex));
            return true;
        }
        
        return false;
    }
    
    /**
     * Refreshes the modal content with current sort filters
     * 
     * @param player The player whose modal to refresh
     */
    private void refreshModal(Player player) {
        SortState sortState = playerSortStates.get(player.getUniqueId());
        if (sortState == null) {
            plugin.getLogger().warning("No sort state found for player " + player.getName() + " during refresh");
            return;
        }
        
        // Store the current category to reopen with
        String currentCategory = sortState.getCurrentCategory();
        
        if (plugin.getInternalConfig().getBoolean("settings.system.debug", false)) {
            plugin.getLogger().info("Refreshing modal for " + player.getName() + " with category: " + currentCategory);
        }
        
        // Instead of removing sort state during close, mark it as persistent
        sortState.setPersistent(true);
        
        // Close current modal and schedule reopening
        player.closeInventory();
        
        // Use a slightly longer delay to ensure the close event processes completely
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Double-check the player is still online and the sort state still exists
            if (player.isOnline() && playerSortStates.containsKey(player.getUniqueId())) {
                if (plugin.getInternalConfig().getBoolean("settings.system.debug", false)) {
                    plugin.getLogger().info("Reopening modal for " + player.getName());
                }
                open(player, currentCategory);
            } else {
                if (plugin.getInternalConfig().getBoolean("settings.system.debug", false)) {
                    plugin.getLogger().warning("Failed to reopen modal - player offline or sort state missing");
                }
            }
        }, 3L); // Increased delay from 1 tick to 3 ticks
    }
    
    /**
     * Gets the current sort state for a player
     * 
     * @param player The player
     * @return The sort state, or null if not found
     */
    public SortState getPlayerSortState(Player player) {
        return playerSortStates.get(player.getUniqueId());
    }
    
    /**
     * Checks if a tag should be shown based on current sort filters
     * 
     * @param player The player
     * @param tagId The tag ID
     * @return true if the tag should be shown
     */
    public boolean shouldShowTagWithFilters(Player player, String tagId) {
        SortState sortState = playerSortStates.get(player.getUniqueId());
        if (sortState == null) {
            return true; // No filters active
        }
        
        // Check category filter
        String currentCategory = sortState.getCurrentCategory();
        if (!"all".equalsIgnoreCase(currentCategory)) {
            String tagNode = plugin.getTagsConfig().getString("tags." + tagId + ".node", "");
            String expectedNode = "category." + currentCategory.toLowerCase();
            if (!expectedNode.equals(tagNode)) {
                return false;
            }
        }
        
        // Check color filter
        String currentColor = sortState.getCurrentColor();
        if (!"all".equalsIgnoreCase(currentColor)) {
            String tagColor = plugin.getTagsConfig().getString("tags." + tagId + ".color", "multi");
            if (!currentColor.equalsIgnoreCase(tagColor)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Updates the modal title with correct pagination information
     * 
     * @param modal The paginated modal
     * @param categoryId The category ID for the title
     */
    private void updateModalTitle(PaginatedModal modal, String categoryId) {
        try {
            // Get title template from config
            String titleTemplate = plugin.getInternalConfig().getString("settings.system.gui.titles.tags-menu", 
                "<gold>Tags</gold> <dark_grey>|</dark_grey> {category} <dark_grey>•</dark_grey> <grey>({currentpage}/{totalpages})</grey>");
            
            // Replace category placeholder
            String categoryName = getCategoryDisplayName(categoryId);
            String titleText = titleTemplate.replace("{category}", categoryName);
            
            // Replace pagination placeholders with actual values
            titleText = titleText.replace("{currentpage}", String.valueOf(modal.getCurrentPageNum()));
            titleText = titleText.replace("{totalpages}", String.valueOf(modal.getPagesNum()));
            
            String processedTitle = plugin.replaceVariables(titleText);
            Component titleComponent = formatTooltipText(processedTitle);
            
            // Update the modal title
            modal.updateTitle(titleComponent);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update modal title with pagination info: " + e.getMessage());
        }
    }
    
    /**
     * Gets the display name for a color
     * 
     * @param colorId The color ID
     * @return The display name for the color
     */
    private String getColorDisplayName(String colorId) {
        String colorText = plugin.getInternalConfig().getString("settings.system.colors." + colorId + ".text", colorId.toUpperCase());
        return TextUtility.processMessage(colorText);
    }
    
    /**
     * Handles tag selection
     * Called by TagsBuilder when a tag is clicked
     * 
     * @param player The player who selected the tag
     * @param tagId The ID of the selected tag
     * @param clickType The type of click (left, right, shift+left, etc.)
     */
    public void handleTagClick(Player player, String tagId, String clickType) {
        // Process the tag interaction based on click type
        switch (clickType.toLowerCase()) {
            case "left":
                handleSetActiveTag(player, tagId);
                break;
            case "right":
                handleToggleFavorite(player, tagId);
                break;
            case "shift_left":
                handleUnlockTag(player, tagId);
                break;
            case "shift_right":
                handleSetActiveFavorite(player, tagId);
                break;
            default:
                // Log unknown click type if debug enabled
                if (plugin.getInternalConfig().getBoolean("settings.system.debug", false)) {
                    plugin.getLogger().warning("Unknown click type for tag interaction: " + clickType);
                }
                break;
        }
    }
    
    /**
     * Handles setting a tag as active
     * 
     * @param player The player
     * @param tagId The tag ID
     */
    private void handleSetActiveTag(Player player, String tagId) {
        // TODO: Implement tag activation logic
        String message = plugin.getInternalConfig().getString("messages.tag.activated",
            "<green>Set active tag to:</green> <gold>{tag}</gold>");
        String tagName = getTagDisplayName(tagId);
        message = message.replace("{tag}", tagName);
        TextUtility.sendPlayerMessage(player, plugin.replaceVariables(message));
        
        // Close modal if configured
        boolean closeOnActivation = plugin.getInternalConfig().getBoolean("settings.system.modal.tags.close-on-activation", false);
        if (closeOnActivation) {
            player.closeInventory();
        }
    }
    
    /**
     * Handles toggling tag favorite status
     * 
     * @param player The player
     * @param tagId The tag ID
     */
    private void handleToggleFavorite(Player player, String tagId) {
        // TODO: Implement favorite toggle logic
        String message = plugin.getInternalConfig().getString("messages.tag.favorite-toggled",
            "<yellow>Toggled favorite status for:</yellow> <gold>{tag}</gold>");
        String tagName = getTagDisplayName(tagId);
        message = message.replace("{tag}", tagName);
        TextUtility.sendPlayerMessage(player, plugin.replaceVariables(message));
    }
    
    /**
     * Handles unlocking a tag
     * 
     * @param player The player
     * @param tagId The tag ID
     */
    private void handleUnlockTag(Player player, String tagId) {
        // TODO: Implement tag unlock logic
        String message = plugin.getInternalConfig().getString("messages.tag.unlocked",
            "<green>Unlocked tag:</green> <gold>{tag}</gold>");
        String tagName = getTagDisplayName(tagId);
        message = message.replace("{tag}", tagName);
        TextUtility.sendPlayerMessage(player, plugin.replaceVariables(message));
        
        // Close modal if configured
        boolean closeOnUnlock = plugin.getInternalConfig().getBoolean("settings.system.modal.tags.close-on-unlock", false);
        if (closeOnUnlock) {
            player.closeInventory();
        }
    }
    
    /**
     * Handles setting a tag as active and favorite
     * 
     * @param player The player
     * @param tagId The tag ID
     */
    private void handleSetActiveFavorite(Player player, String tagId) {
        // TODO: Implement combined active + favorite logic
        String message = plugin.getInternalConfig().getString("messages.tag.active-favorite",
            "<green>Set as active and favorite:</green> <gold>{tag}</gold>");
        String tagName = getTagDisplayName(tagId);
        message = message.replace("{tag}", tagName);
        TextUtility.sendPlayerMessage(player, plugin.replaceVariables(message));
        
        // Close modal if configured
        boolean closeOnActivation = plugin.getInternalConfig().getBoolean("settings.system.modal.tags.close-on-activation", false);
        if (closeOnActivation) {
            player.closeInventory();
        }
    }
    
    /**
     * Gets the display name for a tag
     * 
     * @param tagId The tag ID
     * @return The display name for the tag
     */
    private String getTagDisplayName(String tagId) {
        // Try to get the name from tags config
        String nameKey = "tags." + tagId + ".name";
        String name = plugin.getTagsConfig().getString(nameKey, null);
        
        if (name != null) {
            return TextUtility.processMessage(name); // Process any variables
        }
        
        // Fallback to capitalized ID
        return tagId.substring(0, 1).toUpperCase() + tagId.substring(1).toLowerCase();
    }
    
    /**
     * Gets the display name for a category
     * 
     * @param categoryId The category ID
     * @return The display name for the category
     */
    private String getCategoryDisplayName(String categoryId) {
        // Try to get the name from category config
        String nameKey = "category." + categoryId + ".name";
        String name = plugin.getCategoryConfig().getString(nameKey, null);
        
        if (name != null) {
            return TextUtility.processMessage(name); // Process any variables
        }
        
        // Fallback to capitalized ID
        return categoryId.substring(0, 1).toUpperCase() + categoryId.substring(1).toLowerCase();
    }
    
    /**
     * Formats text for tooltips - removes italic and sets to white, then applies custom formatting
     * 
     * @param text The text to format
     * @return Properly formatted Component for tooltips
     */
    private Component formatTooltipText(String text) {
        // Parse the text with TextUtility first to get all the formatting
        Component component = TextUtility.parse(text);

        // Override italic to false and set default color to white
        return component.decoration(TextDecoration.ITALIC, false)
                       .colorIfAbsent(net.kyori.adventure.text.format.NamedTextColor.WHITE);
    }
    
    /**
     * Gets the modal processor
     * 
     * @return The modal processor
     */
    public ModalProcessor getModalProcessor() {
        return modalProcessor;
    }
    
    /**
     * Gets the plugin instance
     * 
     * @return The CoreTags plugin instance
     */
    public CoreTags getPlugin() {
        return plugin;
    }
}