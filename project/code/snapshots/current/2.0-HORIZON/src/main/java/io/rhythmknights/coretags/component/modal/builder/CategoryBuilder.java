package io.rhythmknights.coretags.component.modal.builder;

import io.rhythmknights.coretags.CoreTags;
import io.rhythmknights.coretags.component.modal.CategoryModal;
import io.rhythmknights.coreapi.component.modal.Modal;
import io.rhythmknights.coreapi.component.modal.ModalItem;
import io.rhythmknights.coreapi.modal.builder.item.ItemBuilder;
import io.rhythmknights.coreframework.component.utility.TextUtility;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the category selection modal interface
 * Uses CoreFramework's TextUtility directly for all text parsing
 */
public class CategoryBuilder {
    
    private final CoreTags plugin;
    private final CategoryModal categoryModal;
    
    /**
     * Constructor for CategoryBuilder
     * 
     * @param plugin The CoreTags plugin instance
     * @param categoryModal The category modal instance
     */
    public CategoryBuilder(CoreTags plugin, CategoryModal categoryModal) {
        this.plugin = plugin;
        this.categoryModal = categoryModal;
    }
    
    /**
     * Builds and populates the category modal
     * 
     * @param modal The modal to populate
     * @param player The player the modal is for
     */
    public void buildModal(Modal modal, Player player) {
        // Fill empty slots if configured
        addFillerItems(modal);
        
        // Add category items
        addCategoryItems(modal, player);
        
        // Add control buttons (close, etc.)
        addControlButtons(modal, player);
    }
    
    /**
     * Adds filler items to empty slots if configured
     * 
     * @param modal The modal to add filler items to
     */
    private void addFillerItems(Modal modal) {
        boolean fillerEnabled = plugin.getInternalConfig().getBoolean("gui.layout.items.empty-slot.category-menu.enabled", false);
        
        if (!fillerEnabled) {
            return;
        }
        
        // Get filler material configuration
        String materialName = plugin.getInternalConfig().getString("gui.layout.items.empty-slot.material", "GRAY_STAINED_GLASS_PANE");
        Material material = Material.valueOf(materialName.toUpperCase());
        
        // Get filler slots from config
        List<String> slotRanges = plugin.getInternalConfig().getStringList("gui.layout.items.empty-slot.category-menu.slots");
        List<Integer> fillerSlots = parseSlotRanges(slotRanges);
        
        // Create filler item - use TextUtility directly with proper formatting
        ItemBuilder fillerBuilder = ItemBuilder.from(material)
            .amount(1)
            .name(formatTooltipText(" ")); // Use formatting helper
        
        // Add custom model data if configured
        int customModelData = plugin.getInternalConfig().getInt("gui.layout.items.empty-slot.custom-model-data", -1);
        if (customModelData != -1) {
            fillerBuilder.model(customModelData);
        }
        
        ModalItem fillerItem = fillerBuilder.asModalItem();
        
        // Place filler items
        for (int slot : fillerSlots) {
            if (slot >= 0 && slot < modal.getRows() * 9) {
                modal.setItem(slot, fillerItem);
            }
        }
    }
    
    /**
     * Adds category selection items to the modal
     * 
     * @param modal The modal to add items to
     * @param player The player the modal is for
     */
    private void addCategoryItems(Modal modal, Player player) {
        // Get categories from the correct configuration path
        ConfigurationSection categorySection = plugin.getCategoryConfig().getConfigurationSection("category");
        if (categorySection == null) {
            plugin.getLogger().warning("No categories configured in category.yml");
            return;
        }
        
        for (String categoryId : categorySection.getKeys(false)) {
            addCategoryItem(modal, player, categoryId);
        }
    }
    
    /**
     * Adds a single category item to the modal
     * Uses TextUtility for text parsing with proper tooltip formatting
     * 
     * @param modal The modal to add the item to
     * @param player The player the modal is for
     * @param categoryId The category ID
     */
    private void addCategoryItem(Modal modal, Player player, String categoryId) {
        ConfigurationSection category = plugin.getCategoryConfig().getConfigurationSection("category." + categoryId);
        if (category == null) {
            return;
        }
        
        // Check permissions
        String permission = category.getString("permission", "");
        if (!permission.isEmpty() && !player.hasPermission(permission)) {
            return;
        }
        
        // Get slot
        int slot = category.getInt("material.slot", -1);
        if (slot == -1) {
            return; // Not shown on category modal
        }
        
        // Get material
        String materialName = category.getString("material.id", "BOOKSHELF");
        Material material = Material.valueOf(materialName.toUpperCase());
        
        // Get display name - use proper formatting
        String name = category.getString("name", categoryId);
        String processedName = plugin.replaceVariables(name);
        Component nameComponent = formatTooltipText(processedName);
        
        // Get lore - use proper formatting with label replacement
        List<String> loreStrings = category.getStringList("lore");
        List<Component> loreComponents = new ArrayList<>();
        for (String loreString : loreStrings) {
            String processedLore = plugin.replaceVariables(loreString);
            // Replace label placeholders
            processedLore = replaceLabelPlaceholders(processedLore);
            loreComponents.add(formatTooltipText(processedLore));
        }
        
        // Create item builder - everything uses proper formatting now
        ItemBuilder itemBuilder = ItemBuilder.from(material)
            .amount(1)
            .name(nameComponent)
            .lore(loreComponents);
        
        // Add custom model data if configured
        int customModelData = category.getInt("material.custom-model-data", -1);
        if (customModelData != -1) {
            itemBuilder.model(customModelData);
        }
        
        // Add glow if configured
        boolean glow = category.getBoolean("material.glow", false);
        if (glow) {
            itemBuilder.glow();
        }
        
        // Create modal item with click action
        ModalItem categoryItem = itemBuilder.asModalItem(event -> {
            if (event.getClick() == ClickType.LEFT) {
                categoryModal.handleCategoryClick(player, categoryId);
            }
        });
        
        // Place item in modal
        if (slot >= 0 && slot < modal.getRows() * 9) {
            modal.setItem(slot, categoryItem);
        }
    }
    
    /**
     * Adds control buttons (close, etc.) to the modal
     * 
     * @param modal The modal to add buttons to
     * @param player The player the modal is for
     */
    private void addControlButtons(Modal modal, Player player) {
        // Add close button if configured
        addCloseButton(modal, player);
    }
    
    /**
     * Adds a close button to the modal
     * Uses proper tooltip formatting
     * 
     * @param modal The modal to add the button to
     * @param player The player the modal is for
     */
    private void addCloseButton(Modal modal, Player player) {
        boolean closeEnabled = plugin.getInternalConfig().getBoolean("gui.layout.items.close-button-material.enabled", true);
        if (!closeEnabled) {
            return;
        }
        
        int slot = plugin.getInternalConfig().getInt("gui.layout.items.close-button-material.slot.category-menu", -1);
        if (slot == -1) {
            return;
        }
        
        String materialName = plugin.getInternalConfig().getString("gui.layout.items.close-button-material.material", "BARRIER");
        Material material = Material.valueOf(materialName.toUpperCase());
        
        String titleKey = "settings.system.gui.titles.close-button";
        String title = plugin.getInternalConfig().getString(titleKey, "<red>Exit</red>");
        String processedTitle = plugin.replaceVariables(title);
        Component titleComponent = formatTooltipText(processedTitle);
        
        ItemBuilder itemBuilder = ItemBuilder.from(material)
            .amount(1)
            .name(titleComponent);
        
        // Add custom model data if configured
        int customModelData = plugin.getInternalConfig().getInt("gui.layout.items.close-button-material.custom-model-data", -1);
        if (customModelData != -1) {
            itemBuilder.model(customModelData);
        }
        
        ModalItem closeItem = itemBuilder.asModalItem(event -> {
            player.closeInventory();
            
            String closeMessage = plugin.getInternalConfig().getString("messages.modal.closed-by-button",
                "<grey>Menu closed.</grey>");
            String processedMessage = plugin.replaceVariables(closeMessage);
            TextUtility.sendPlayerMessage(player, processedMessage);
        });
        
        if (slot >= 0 && slot < modal.getRows() * 9) {
            modal.setItem(slot, closeItem);
        }
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
        
        // Override italic to false (remove default italic) and ensure proper formatting
        return component.decoration(TextDecoration.ITALIC, false);
    }
    
    /**
     * Replace label placeholders with their actual values from config
     * 
     * @param text The text containing placeholders
     * @return Text with placeholders replaced
     */
    private String replaceLabelPlaceholders(String text) {
        // Status labels
        text = text.replace("{unlocked}", plugin.getInternalConfig().getString("settings.system.conditions.unlocked", "<green>UNLOCKED</green>"));
        text = text.replace("{locked}", plugin.getInternalConfig().getString("settings.system.conditions.locked", "<red>LOCKED</red>"));
        text = text.replace("{protected}", plugin.getInternalConfig().getString("settings.system.conditions.protected", "<dark_purple>PROTECTED</dark_purple>"));
        text = text.replace("{favorite}", plugin.getInternalConfig().getString("settings.system.conditions.favorite", "<aqua>FAVORITE</aqua>"));
        text = text.replace("{favorites}", plugin.getInternalConfig().getString("settings.system.conditions.favorite", "<aqua>FAVORITE</aqua>"));
        text = text.replace("{all}", plugin.getInternalConfig().getString("settings.system.colors.all.text", "<gold>ALL</gold>"));
        
        // Colors
        text = text.replace("{colors.all}", plugin.getInternalConfig().getString("settings.system.colors.all.text", "<gold>ALL</gold>"));
        text = text.replace("{colors.multi}", plugin.getInternalConfig().getString("settings.system.colors.multi.text", "<blue>M</blue><green>U</green><yellow>L</yellow><red>T</red><light_purple>I</light_purple>"));
        
        return text;
    }
    
    /**
     * Parses slot range strings into a list of slot numbers
     * Supports formats like "0..7", "9", "17", "18..26"
     * 
     * @param slotRanges List of slot range strings
     * @return List of individual slot numbers
     */
    private List<Integer> parseSlotRanges(List<String> slotRanges) {
        List<Integer> slots = new ArrayList<>();
        
        for (String range : slotRanges) {
            if (range.contains("..")) {
                // Range format: "0..7"
                String[] parts = range.split("\\.\\.");
                if (parts.length == 2) {
                    try {
                        int start = Integer.parseInt(parts[0]);
                        int end = Integer.parseInt(parts[1]);
                        for (int i = start; i <= end; i++) {
                            slots.add(i);
                        }
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("Invalid slot range format: " + range);
                    }
                }
            } else {
                // Single slot: "9"
                try {
                    slots.add(Integer.parseInt(range));
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid slot number: " + range);
                }
            }
        }
        
        return slots;
    }
}