package io.rhythmknights.coretags.component.modal;

import io.rhythmknights.coretags.CoreTags;
import io.rhythmknights.coreframework.component.utility.TextUtility;
import org.bukkit.entity.Player;

/**
 * Main controller for all CoreTags modal interactions
 * Delegates to specific modal handlers based on configuration and context
 */
public class ModalProcessor {
    
    private final CoreTags plugin;
    private final CategoryModal categoryModal;
    private final TagsModal tagsModal;
    
    /**
     * Constructor for ModalProcessor
     * 
     * @param plugin The CoreTags plugin instance
     */
    public ModalProcessor(CoreTags plugin) {
        this.plugin = plugin;
        this.categoryModal = new CategoryModal(plugin, this);
        this.tagsModal = new TagsModal(plugin, this);
    }
    
    /**
     * Opens the appropriate modal based on configuration
     * This is the main entry point for the /coretags command
     * 
     * @param player The player to open the modal for
     */
    public void openMainModal(Player player) {
        String defaultModal = plugin.getInternalConfig().getString("settings.system.modal.default", "CATEGORY");
        
        if ("TAGS".equalsIgnoreCase(defaultModal)) {
            // Skip category modal and go straight to tags
            openTagsModal(player, "all"); // Default to "all" category
        } else {
            // Check if categories are enabled
            boolean categoriesEnabled = plugin.getInternalConfig().getBoolean("settings.system.modal.category.enabled", true);
            
            if (categoriesEnabled) {
                openCategoryModal(player);
            } else {
                // Fallback to tags modal if categories are disabled
                String fallbackMessage = plugin.getInternalConfig().getString("messages.system.category-disabled-fallback", 
                    "<yellow>Categories are disabled, opening tags menu instead.</yellow>");
                TextUtility.sendPlayerMessage(player, plugin.replaceVariables(fallbackMessage));
                openTagsModal(player, "all");
            }
        }
    }
    
    /**
     * Opens the category selection modal
     * 
     * @param player The player to open the modal for
     */
    public void openCategoryModal(Player player) {
        try {
            categoryModal.open(player);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to open category modal for player " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            
            String errorMessage = plugin.getInternalConfig().getString("messages.error.modal-open-failed",
                "<red>Failed to open menu. Please try again or contact an administrator.</red>");
            TextUtility.sendPlayerMessage(player, plugin.replaceVariables(errorMessage));
        }
    }
    
    /**
     * Opens the tags modal for a specific category
     * 
     * @param player The player to open the modal for
     * @param categoryId The category ID to display tags for
     */
    public void openTagsModal(Player player, String categoryId) {
        try {
            tagsModal.open(player, categoryId);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to open tags modal for player " + player.getName() + 
                " with category " + categoryId + ": " + e.getMessage());
            e.printStackTrace();
            
            String errorMessage = plugin.getInternalConfig().getString("messages.error.modal-open-failed",
                "<red>Failed to open menu. Please try again or contact an administrator.</red>");
            TextUtility.sendPlayerMessage(player, plugin.replaceVariables(errorMessage));
        }
    }
    
    /**
     * Handles category selection from the category modal
     * Called by CategoryModal when a category is clicked
     * 
     * @param player The player who made the selection
     * @param categoryId The selected category ID
     */
    public void handleCategorySelection(Player player, String categoryId) {
        // Log the selection if debug is enabled
        if (plugin.getInternalConfig().getBoolean("settings.system.debug", false)) {
            plugin.getLogger().info("Player " + player.getName() + " selected category: " + categoryId);
        }
        
        // Transition to tags modal
        openTagsModal(player, categoryId);
    }
    
    /**
     * Handles going back from tags modal to category modal
     * Called by TagsModal when back button is clicked
     * 
     * @param player The player requesting to go back
     */
    public void handleBackToCategories(Player player) {
        // Check if we should show categories or close entirely
        String defaultModal = plugin.getInternalConfig().getString("settings.system.modal.default", "CATEGORY");
        boolean categoriesEnabled = plugin.getInternalConfig().getBoolean("settings.system.modal.category.enabled", true);
        
        if ("CATEGORY".equalsIgnoreCase(defaultModal) && categoriesEnabled) {
            openCategoryModal(player);
        } else {
            // If categories are not the default or are disabled, just close the modal
            player.closeInventory();
            
            String closeMessage = plugin.getInternalConfig().getString("messages.modal.closed",
                "<grey>Menu closed.</grey>");
            TextUtility.sendPlayerMessage(player, plugin.replaceVariables(closeMessage));
        }
    }
    
    /**
     * Gets the category modal instance
     * 
     * @return The category modal
     */
    public CategoryModal getCategoryModal() {
        return categoryModal;
    }
    
    /**
     * Gets the tags modal instance
     * 
     * @return The tags modal
     */
    public TagsModal getTagsModal() {
        return tagsModal;
    }
}