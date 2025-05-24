package io.rhythmknights.coretags.component.modal;

import io.rhythmknights.coretags.CoreTags;
import io.rhythmknights.coretags.component.modal.builder.CategoryBuilder;
import io.rhythmknights.coreapi.component.modal.Modal;
import io.rhythmknights.coreframework.component.utility.TextUtility;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * Handles the category selection modal for CoreTags
 * Delegates building logic to CategoryBuilder
 */
public class CategoryModal {
    
    private final CoreTags plugin;
    private final ModalProcessor modalProcessor;
    private final CategoryBuilder categoryBuilder;
    
    /**
     * Constructor for CategoryModal
     * 
     * @param plugin The CoreTags plugin instance
     * @param modalProcessor The modal processor for navigation
     */
    public CategoryModal(CoreTags plugin, ModalProcessor modalProcessor) {
        this.plugin = plugin;
        this.modalProcessor = modalProcessor;
        this.categoryBuilder = new CategoryBuilder(plugin, this);
    }
    
    /**
     * Opens the category modal for a player
     * 
     * @param player The player to open the modal for
     */
    public void open(Player player) {
        // Get title from language config
        String titleKey = "settings.system.gui.titles.category-menu";
        String titleText = plugin.getInternalConfig().getString(titleKey, "<gold>Tags</gold> <dark_grey>|</dark_grey> <gold>Categories</gold>");
        String processedTitle = plugin.replaceVariables(titleText);
        
        // Get number of rows from config
        int rows = plugin.getInternalConfig().getInt("gui.category-menu.rows", 4);
        rows = Math.max(1, Math.min(6, rows)); // Clamp between 1-6
        
        // Create the modal using CoreAPI - parse title with TextUtility
        Modal modal;
        try {
            Component titleComponent = TextUtility.parse(processedTitle);
            modal = Modal.modal()
                .title(titleComponent)
                .rows(rows)
                .disableAllInteractions() // Prevent item manipulation
                .create();
        } catch (Exception e) {
            // Fallback with basic title
            plugin.getLogger().warning("Failed to set modal title, using fallback: " + e.getMessage());
            modal = Modal.modal()
                .rows(rows)
                .disableAllInteractions()
                .create();
        }
        
        // Let the builder populate the modal
        categoryBuilder.buildModal(modal, player);
        
        // Set up close action
        modal.setCloseModalAction(event -> {
            String closeMessage = plugin.getInternalConfig().getString("messages.modal.category-closed",
                "<grey>Category menu closed.</grey>");
            TextUtility.sendPlayerMessage(player, plugin.replaceVariables(closeMessage));
        });
        
        // Open the modal
        modal.open(player);
        
        // Log if debug is enabled
        if (plugin.getInternalConfig().getBoolean("settings.system.debug", false)) {
            plugin.getLogger().info("Opened category modal for player: " + player.getName());
        }
    }
    
    /**
     * Handles category selection
     * Called by CategoryBuilder when a category is clicked
     * 
     * @param player The player who selected the category
     * @param categoryId The ID of the selected category
     */
    public void handleCategoryClick(Player player, String categoryId) {
        // Close current modal
        player.closeInventory();
        
        // Send selection message if configured
        String selectionMessage = plugin.getInternalConfig().getString("messages.modal.category-selected",
            "<grey>Selected category:</grey> <gold>{category}</gold>");
        if (selectionMessage != null && !selectionMessage.isEmpty()) {
            String categoryName = getCategoryDisplayName(categoryId);
            selectionMessage = selectionMessage.replace("{category}", categoryName);
            TextUtility.sendPlayerMessage(player, plugin.replaceVariables(selectionMessage));
        }
        
        // Delegate to modal processor
        modalProcessor.handleCategorySelection(player, categoryId);
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
        String name = plugin.getInternalConfig().getString(nameKey, null);
        
        if (name != null) {
            return TextUtility.processMessage(name); // Process any variables
        }
        
        // Fallback to capitalized ID
        return categoryId.substring(0, 1).toUpperCase() + categoryId.substring(1).toLowerCase();
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