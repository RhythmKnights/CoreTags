package io.rhythmknights.coretags.component.modal;

import io.rhythmknights.coretags.CoreTags;
import io.rhythmknights.coretags.component.modal.builder.TagsBuilder;
import io.rhythmknights.coreapi.component.modal.Modal;
import io.rhythmknights.coreapi.component.modal.PaginatedModal;
import io.rhythmknights.coreframework.component.utility.TextUtility;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

/**
 * Handles the tags selection modal for CoreTags
 * Delegates building logic to TagsBuilder
 */
public class TagsModal {
    
    private final CoreTags plugin;
    private final ModalProcessor modalProcessor;
    private final TagsBuilder tagsBuilder;
    
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
        // Get title template from config
        String titleTemplate = plugin.getInternalConfig().getString("settings.system.gui.titles.tags-menu", 
            "<gold>Tags</gold> <dark_grey>|</dark_grey> {category} <dark_grey>•</dark_grey> <grey>({currentpage}/{totalpages})</grey>");
        
        // Replace category placeholder
        String categoryName = getCategoryDisplayName(categoryId);
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
        tagsBuilder.buildModal(modal, player, categoryId);
        
        // Set up close action
        modal.setCloseModalAction(event -> {
            String closeMessage = plugin.getInternalConfig().getString("messages.modal.tags-closed",
                "<grey>Tags menu closed.</grey>");
            TextUtility.sendPlayerMessage(player, plugin.replaceVariables(closeMessage));
        });
        
        // Update title with correct pagination info after modal is built
        updateModalTitle(modal, categoryId);
        
        // Open the modal
        modal.open(player);
        
        // Log if debug is enabled
        if (plugin.getInternalConfig().getBoolean("settings.system.debug", false)) {
            plugin.getLogger().info("Opened tags modal for player: " + player.getName() + " with category: " + categoryId);
        }
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
    private void handleToggleFavorite(