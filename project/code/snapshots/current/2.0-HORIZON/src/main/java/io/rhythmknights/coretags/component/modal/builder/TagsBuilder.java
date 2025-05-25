package io.rhythmknights.coretags.component.modal.builder;

import io.rhythmknights.coretags.CoreTags;
import io.rhythmknights.coretags.component.modal.TagsModal;
import io.rhythmknights.coreapi.component.modal.ModalItem;
import io.rhythmknights.coreapi.component.modal.PaginatedModal;
import io.rhythmknights.coreapi.component.module.PaginationRegion;
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
 * Builds the tags selection modal interface with enhanced sorting functionality
 * Uses CoreFramework's TextUtility directly for all text parsing
 */
public class TagsBuilder {
    
    private final CoreTags plugin;
    private final TagsModal tagsModal;
    
    /**
     * Constructor for TagsBuilder
     * 
     * @param plugin The CoreTags plugin instance
     * @param tagsModal The tags modal instance
     */
    public TagsBuilder(CoreTags plugin, TagsModal tagsModal) {
        this.plugin = plugin;
        this.tagsModal = tagsModal;
    }
    
    /**
     * Builds and populates the tags modal
     * 
     * @param modal The modal to populate
     * @param player The player the modal is for
     * @param categoryId The category to display tags for
     */
    public void buildModal(PaginatedModal modal, Player player, String categoryId) {
        // Set up pagination region if configured
        setupPaginationRegion(modal);
        
        // Fill empty slots if configured
        addFillerItems(modal);
        
        // Add control buttons (navigation, sorting, etc.)
        addControlButtons(modal, player, categoryId);
        
        // Add tag items (these go into the paginated area) - now with filtering
        addTagItems(modal, player, categoryId);
    }
    
    /**
     * Sets up the pagination region based on configuration
     * 
     * @param modal The modal to configure
     */
    private void setupPaginationRegion(PaginatedModal modal) {
        // Get tag slots from config
        ConfigurationSection slotsSection = plugin.getInternalConfig().getConfigurationSection("gui.tags.slots");
        if (slotsSection == null) {
            return;
        }
        
        List<Integer> tagSlots = new ArrayList<>();
        
        // Parse row configurations
        for (String rowKey : slotsSection.getKeys(false)) {
            String slotRange = slotsSection.getString(rowKey, "");
            if (slotRange.isEmpty()) {
                continue; // Skip empty rows
            }
            
            List<Integer> rowSlots = parseSlotRange(slotRange);
            tagSlots.addAll(rowSlots);
        }
        
        if (!tagSlots.isEmpty()) {
            PaginationRegion region = new PaginationRegion(tagSlots);
            modal.setPaginationRegion(region);
        }
    }
    
    /**
     * Adds filler items to empty slots if configured
     * 
     * @param modal The modal to add filler items to
     */
    private void addFillerItems(PaginatedModal modal) {
        boolean fillerEnabled = plugin.getInternalConfig().getBoolean("gui.layout.items.empty-slot.tags-menu.enabled", false);
        
        if (!fillerEnabled) {
            return;
        }
        
        // Get filler material configuration
        String materialName = plugin.getInternalConfig().getString("gui.layout.items.empty-slot.material", "GRAY_STAINED_GLASS_PANE");
        Material material = Material.valueOf(materialName.toUpperCase());
        
        // Get filler slots from config
        List<String> slotRanges = plugin.getInternalConfig().getStringList("gui.layout.items.empty-slot.tags-menu.slots");
        List<Integer> fillerSlots = parseSlotRanges(slotRanges);
        
        // Create filler item - use proper formatting
        ItemBuilder fillerBuilder = ItemBuilder.from(material)
            .amount(1)
            .name(formatTooltipText(" "));
        
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
     * Adds control buttons (navigation, sorting, etc.) to the modal
     * 
     * @param modal The modal to add buttons to
     * @param player The player the modal is for
     * @param categoryId The current category
     */
    private void addControlButtons(PaginatedModal modal, Player player, String categoryId) {
        // Add navigation buttons
        addNavigationButtons(modal, player);
        
        // Add sorting buttons
        addSortingButtons(modal, player, categoryId);
        
        // Add utility buttons
        addUtilityButtons(modal, player, categoryId);
        
        // Add back/close button
        addBackButton(modal, player);
    }
    
    /**
     * Adds pagination navigation buttons
     * 
     * @param modal The modal to add buttons to
     * @param player The player the modal is for
     */
    private void addNavigationButtons(PaginatedModal modal, Player player) {
        // Previous page button
        boolean prevEnabled = plugin.getInternalConfig().getBoolean("gui.layout.items.last-page-button.enabled", true);
        if (prevEnabled) {
            int slot = plugin.getInternalConfig().getInt("gui.layout.items.last-page-button.slot", 48);
            addNavigationButton(modal, player, slot, "previous", "ARROW", 
                plugin.getInternalConfig().getString("settings.system.gui.titles.last-page-button", "<grey>Previous Page</grey>"));
        }
        
        // Next page button
        boolean nextEnabled = plugin.getInternalConfig().getBoolean("gui.layout.items.next-page-button.enabled", true);
        if (nextEnabled) {
            int slot = plugin.getInternalConfig().getInt("gui.layout.items.next-page-button.slot", 50);
            addNavigationButton(modal, player, slot, "next", "ARROW", 
                plugin.getInternalConfig().getString("settings.system.gui.titles.next-page-button", "<grey>Next Page</grey>"));
        }
    }
    
    /**
     * Adds a single navigation button
     * Uses proper tooltip formatting
     * 
     * @param modal The modal to add the button to
     * @param player The player the modal is for
     * @param slot The slot to place the button in
     * @param action The navigation action (previous/next)
     * @param defaultMaterial Default material if not configured
     * @param title The button title
     */
    private void addNavigationButton(PaginatedModal modal, Player player, int slot, String action, String defaultMaterial, String title) {
        String materialName = plugin.getInternalConfig().getString("gui.layout.items." + 
            (action.equals("previous") ? "last-page-button" : "next-page-button") + ".material", defaultMaterial);
        Material material = Material.valueOf(materialName.toUpperCase());
        
        // Process title through DataProcessor for centralized variable replacement
        String processedTitle = plugin.getDataProcessor().replaceVariables(title);
        Component titleComponent = formatTooltipText(processedTitle);
        
        ItemBuilder itemBuilder = ItemBuilder.from(material)
            .amount(1)
            .name(titleComponent);
        
        ModalItem navItem = itemBuilder.asModalItem(event -> {
            if (action.equals("previous")) {
                if (modal.previous()) {
                    // Successfully went to previous page
                } else {
                    String message = plugin.getInternalConfig().getString("messages.modal.no-previous-page",
                        "<red>You are already on the first page.</red>");
                    TextUtility.sendPlayerMessage(player, plugin.getDataProcessor().replaceVariables(message));
                }
            } else {
                if (modal.next()) {
                    // Successfully went to next page
                } else {
                    String message = plugin.getInternalConfig().getString("messages.modal.no-next-page",
                        "<red>You are already on the last page.</red>");
                    TextUtility.sendPlayerMessage(player, plugin.getDataProcessor().replaceVariables(message));
                }
            }
        });
        
        if (slot >= 0 && slot < modal.getRows() * 9) {
            modal.setItem(slot, navItem);
        }
    }
    
    /**
     * Adds sorting buttons (category, favorite, color)
     * 
     * @param modal The modal to add buttons to
     * @param player The player the modal is for
     * @param categoryId The current category
     */
    private void addSortingButtons(PaginatedModal modal, Player player, String categoryId) {
        // Category sort button
        boolean categoryEnabled = plugin.getInternalConfig().getBoolean("gui.layout.items.category-sort-button.enabled", true);
        if (categoryEnabled) {
            int slot = plugin.getInternalConfig().getInt("gui.layout.items.category-sort-button.slot", 17);
            addSortButton(modal, player, slot, "category", categoryId);
        }
        
        // Favorite sort button
        boolean favoriteEnabled = plugin.getInternalConfig().getBoolean("gui.layout.items.favorite-sort-button.enabled", true);
        if (favoriteEnabled) {
            int slot = plugin.getInternalConfig().getInt("gui.layout.items.favorite-sort-button.slot", 26);
            addSortButton(modal, player, slot, "favorite", categoryId);
        }
        
        // Color sort button
        boolean colorEnabled = plugin.getInternalConfig().getBoolean("gui.layout.items.color-sort-button.enabled", true);
        if (colorEnabled) {
            int slot = plugin.getInternalConfig().getInt("gui.layout.items.color-sort-button.slot", 35);
            addSortButton(modal, player, slot, "color", categoryId);
        }
    }
    
    /**
     * Adds a sorting button with current state display
     * Uses proper tooltip formatting and shows current filter state
     * 
     * @param modal The modal to add the button to
     * @param player The player the modal is for
     * @param slot The slot to place the button in
     * @param sortType The type of sorting (category/favorite/color)
     * @param categoryId The current category
     */
    private void addSortButton(PaginatedModal modal, Player player, int slot, String sortType, String categoryId) {
        // Get current sort state
        TagsModal.SortState sortState = tagsModal.getPlayerSortState(player);
        
        // Get material based on sort type and theme settings
        String materialName = getSortButtonMaterial(sortType, sortState, categoryId);
        Material material = Material.valueOf(materialName.toUpperCase());
        
        // Get title with current state information
        String title = getSortButtonTitle(sortType, sortState);
        String processedTitle = plugin.getDataProcessor().replaceVariables(title);
        Component titleComponent = formatTooltipText(processedTitle);
        
        // Get lore with current state and controls
        List<String> loreStrings = getSortButtonLore(sortType, sortState);
        List<Component> loreComponents = new ArrayList<>();
        for (String loreString : loreStrings) {
            String processedLore = plugin.getDataProcessor().replaceVariables(loreString);
            loreComponents.add(formatTooltipText(processedLore));
        }
        
        ItemBuilder itemBuilder = ItemBuilder.from(material)
            .amount(1)
            .name(titleComponent)
            .lore(loreComponents);
        
        // Add custom model data and glow if configured
        addSortButtonEffects(itemBuilder, sortType, sortState);
        
        ModalItem sortItem = itemBuilder.asModalItem(event -> {
            handleSortButtonClick(player, sortType, event.getClick());
        });
        
        if (slot >= 0 && slot < modal.getRows() * 9) {
            modal.setItem(slot, sortItem);
        }
    }
    
    /**
     * Gets the material for a sort button based on type, state, and theme
     * 
     * @param sortType The sort type
     * @param sortState The current sort state
     * @param categoryId The current category (for category button theming)
     * @return Material name
     */
    private String getSortButtonMaterial(String sortType, TagsModal.SortState sortState, String categoryId) {
        String buttonType = plugin.getInternalConfig().getString("settings.system.modal.tags." + sortType + "-sort-button", "BASIC");
        
        if ("THEME".equalsIgnoreCase(buttonType) && sortState != null) {
            String themePath = "gui.layout.items." + sortType + "-sort-button-themed.material.";
            
            if ("category".equals(sortType)) {
                // Use current category for theming
                themePath += sortState.getCurrentCategory().toLowerCase();
            } else if ("color".equals(sortType)) {
                // Use current color for theming
                themePath += sortState.getCurrentColor().toLowerCase();
            } else {
                themePath += "all"; // Default theme for favorite button
            }
            
            return plugin.getInternalConfig().getString(themePath + ".material", getDefaultSortMaterial(sortType));
        } else {
            // Use basic material
            return plugin.getInternalConfig().getString("gui.layout.items." + sortType + "-sort-button.material", getDefaultSortMaterial(sortType));
        }
    }
    
    /**
     * Gets the default material for a sort button type
     * 
     * @param sortType The sort type
     * @return Default material name
     */
    private String getDefaultSortMaterial(String sortType) {
        switch (sortType) {
            case "category":
                return "GOLD_BLOCK";
            case "favorite":
                return "NETHER_STAR";
            case "color":
                return "BRUSH";
            default:
                return "STONE";
        }
    }
    
    /**
     * Gets the title for a sort button showing current state
     * 
     * @param sortType The sort type
     * @param sortState The current sort state
     * @return The button title
     */
    private String getSortButtonTitle(String sortType, TagsModal.SortState sortState) {
        String baseTitle;
        String currentValue = "";
        
        switch (sortType) {
            case "category":
                baseTitle = plugin.getInternalConfig().getString("settings.system.gui.titles.category-sort-button", 
                    "<grey>CATEGORY</grey> <dark_grey>•</dark_grey> {category}");
                if (sortState != null) {
                    currentValue = getCategoryDisplayName(sortState.getCurrentCategory());
                }
                return baseTitle.replace("{category}", currentValue);
                
            case "color":
                baseTitle = plugin.getInternalConfig().getString("settings.system.gui.titles.color-sort-button", 
                    "<grey>COLOR</grey> <dark_grey>•</dark_grey> {color}");
                if (sortState != null) {
                    currentValue = getColorDisplayName(sortState.getCurrentColor());
                }
                return baseTitle.replace("{color}", currentValue);
                
            case "favorite":
            default:
                return plugin.getInternalConfig().getString("settings.system.gui.titles.favorite-sort-button", 
                    "<grey>FAVORITES</grey>");
        }
    }
    
    /**
     * Gets the lore for a sort button with controls and current state
     * 
     * @param sortType The sort type
     * @param sortState The current sort state
     * @return List of lore strings
     */
    private List<String> getSortButtonLore(String sortType, TagsModal.SortState sortState) {
        List<String> lore = new ArrayList<>();
        
        // Get base lore from config
        List<String> baseLore = plugin.getInternalConfig().getStringList("settings.system.gui.lore." + sortType + "-sort-button");
        
        if ("category".equals(sortType) || "color".equals(sortType)) {
            // Add current state info
            if (sortState != null) {
                String currentFilter = "category".equals(sortType) ? 
                    getCategoryDisplayName(sortState.getCurrentCategory()) : 
                    getColorDisplayName(sortState.getCurrentColor());
                
                lore.add("<grey>Current " + sortType + ":</grey> " + currentFilter);
                lore.add("");
            }
            
            // Add controls
            lore.addAll(baseLore);
        } else {
            // Favorite button - simpler lore
            lore.addAll(baseLore);
        }
        
        return lore;
    }
    
    /**
     * Adds visual effects to sort buttons based on configuration
     * 
     * @param itemBuilder The item builder to modify
     * @param sortType The sort type
     * @param sortState The current sort state
     */
    private void addSortButtonEffects(ItemBuilder itemBuilder, String sortType, TagsModal.SortState sortState) {
        String buttonType = plugin.getInternalConfig().getString("settings.system.modal.tags." + sortType + "-sort-button", "BASIC");
        
        if ("THEME".equalsIgnoreCase(buttonType) && sortState != null) {
            String effectPath = "gui.layout.items." + sortType + "-sort-button-themed.material.";
            
            if ("category".equals(sortType)) {
                effectPath += sortState.getCurrentCategory().toLowerCase();
            } else if ("color".equals(sortType)) {
                effectPath += sortState.getCurrentColor().toLowerCase();
            } else {
                effectPath += "all";
            }
            
            // Add custom model data
            int customModelData = plugin.getInternalConfig().getInt(effectPath + ".custom-model-data", -1);
            if (customModelData != -1) {
                itemBuilder.model(customModelData);
            }
            
            // Add glow
            boolean glow = plugin.getInternalConfig().getBoolean(effectPath + ".glow", false);
            if (glow) {
                itemBuilder.glow();
            }
        } else {
            // Use basic effects
            int customModelData = plugin.getInternalConfig().getInt("gui.layout.items." + sortType + "-sort-button.custom-model-data", -1);
            if (customModelData != -1) {
                itemBuilder.model(customModelData);
            }
            
            boolean glow = plugin.getInternalConfig().getBoolean("gui.layout.items." + sortType + "-sort-button.glow", false);
            if (glow) {
                itemBuilder.glow();
            }
        }
    }
    
    /**
     * Handles sort button clicks with proper navigation logic
     * 
     * @param player The player who clicked
     * @param sortType The sort type
     * @param clickType The click type
     */
    private void handleSortButtonClick(Player player, String sortType, ClickType clickType) {
        String direction;
        
        switch (clickType) {
            case LEFT:
                direction = "next";
                break;
            case RIGHT:
                direction = "previous";
                break;
            case SHIFT_LEFT:
                direction = "first";
                break;
            case SHIFT_RIGHT:
                direction = "last";
                break;
            default:
                return; // Ignore other click types
        }
        
        // Delegate to TagsModal for navigation handling
        tagsModal.handleSortNavigation(player, sortType, direction);
    }
    
    /**
     * Adds utility buttons (reset tag, active tag display)
     * 
     * @param modal The modal to add buttons to
     * @param player The player the modal is for
     * @param categoryId The current category
     */
    private void addUtilityButtons(PaginatedModal modal, Player player, String categoryId) {
        // Reset tag button
        boolean resetEnabled = plugin.getInternalConfig().getBoolean("gui.layout.items.reset-tag-button.enabled", true);
        if (resetEnabled) {
            int slot = plugin.getInternalConfig().getInt("gui.layout.items.reset-tag-button.slot.tags-menu", 49);
            addUtilityButton(modal, player, slot, "reset");
        }
        
        // Active tag display
        boolean activeEnabled = plugin.getInternalConfig().getBoolean("gui.layout.items.active-tag.enabled", true);
        if (activeEnabled) {
            int slot = plugin.getInternalConfig().getInt("gui.layout.items.active-tag.slot.tags-menu", 53);
            addUtilityButton(modal, player, slot, "active");
        }
    }
    
    /**
     * Adds a utility button
     * Uses proper tooltip formatting
     * 
     * @param modal The modal to add the button to
     * @param player The player the modal is for
     * @param slot The slot to place the button in
     * @param buttonType The button type (reset/active)
     */
    private void addUtilityButton(PaginatedModal modal, Player player, int slot, String buttonType) {
        String materialName = plugin.getInternalConfig().getString("gui.layout.items." + buttonType + "-tag-button.material", 
            buttonType.equals("reset") ? "RED_DYE" : "NAME_TAG");
        Material material = Material.valueOf(materialName.toUpperCase());
        
        String titleKey = "settings.system.gui.titles." + buttonType + "-tag";
        String title = plugin.getInternalConfig().getString(titleKey, 
            buttonType.equals("reset") ? "<red>Reset Tag</red>" : "<gold>Active Tag</gold>");
        String processedTitle = plugin.getDataProcessor().replaceVariables(title);
        Component titleComponent = formatTooltipText(processedTitle);
        
        ItemBuilder itemBuilder = ItemBuilder.from(material)
            .amount(1)
            .name(titleComponent);
        
        // Add glow for active tag
        if ("active".equals(buttonType)) {
            boolean glow = plugin.getInternalConfig().getBoolean("gui.layout.items.active-tag.glow", true);
            if (glow) {
                itemBuilder.glow();
            }
        }
        
        ModalItem utilityItem = itemBuilder.asModalItem(event -> {
            if ("reset".equals(buttonType)) {
                handleResetTag(player);
            } else {
                handleActiveTagDisplay(player);
            }
        });
        
        if (slot >= 0 && slot < modal.getRows() * 9) {
            modal.setItem(slot, utilityItem);
        }
    }
    
    /**
     * Adds back/close button
     * Uses proper tooltip formatting
     * 
     * @param modal The modal to add the button to
     * @param player The player the modal is for
     */
    private void addBackButton(PaginatedModal modal, Player player) {
        boolean backEnabled = plugin.getInternalConfig().getBoolean("gui.layout.items.back-button.enabled", true);
        if (!backEnabled) {
            return;
        }
        
        int slot = plugin.getInternalConfig().getInt("gui.layout.items.back-button.slot.tags-menu", 45);
        
        // Check if we should use close button instead
        boolean useCloseButton = plugin.getInternalConfig().getBoolean("settings.system.modal.tags.close-button-swap", true);
        String defaultModal = plugin.getInternalConfig().getString("settings.system.modal.default", "CATEGORY");
        boolean categoriesEnabled = plugin.getInternalConfig().getBoolean("settings.system.modal.category.enabled", true);
        
        String materialName;
        String title;
        
        if (useCloseButton && (!"CATEGORY".equalsIgnoreCase(defaultModal) || !categoriesEnabled)) {
            // Use close button
            materialName = plugin.getInternalConfig().getString("gui.layout.items.close-button-material.material", "BARRIER");
            title = plugin.getInternalConfig().getString("settings.system.gui.titles.close-button", "<red>Exit</red>");
        } else {
            // Use back button
            materialName = plugin.getInternalConfig().getString("gui.layout.items.back-button.material", "SPECTRAL_ARROW");
            title = plugin.getInternalConfig().getString("settings.system.gui.titles.back-button", "<red>Back</red>");
        }
        
        Material material = Material.valueOf(materialName.toUpperCase());
        String processedTitle = plugin.getDataProcessor().replaceVariables(title);
        Component titleComponent = formatTooltipText(processedTitle);
        
        ItemBuilder itemBuilder = ItemBuilder.from(material)
            .amount(1)
            .name(titleComponent);
        
        ModalItem backItem = itemBuilder.asModalItem(event -> {
            handleBackClick(player);
        });
        
        if (slot >= 0 && slot < modal.getRows() * 9) {
            modal.setItem(slot, backItem);
        }
    }
    
    /**
     * Handles back button click
     * 
     * @param player The player who clicked
     */
    private void handleBackClick(Player player) {
        // Check if we should show categories or close entirely
        String defaultModal = plugin.getInternalConfig().getString("settings.system.modal.default", "CATEGORY");
        boolean categoriesEnabled = plugin.getInternalConfig().getBoolean("settings.system.modal.category.enabled", true);
        
        if ("CATEGORY".equalsIgnoreCase(defaultModal) && categoriesEnabled) {
            // Go back to category modal - delegate to modal processor
            tagsModal.getModalProcessor().handleBackToCategories(player);
        } else {
            // Just close the modal
            player.closeInventory();
            
            String closeMessage = plugin.getInternalConfig().getString("messages.modal.closed",
                "<grey>Menu closed.</grey>");
            TextUtility.sendPlayerMessage(player, plugin.getDataProcessor().replaceVariables(closeMessage));
        }
    }
    
    /**
     * Adds tag items to the paginated area with filtering based on current sort state
     * 
     * @param modal The modal to add items to
     * @param player The player the modal is for
     * @param categoryId The category to display tags for
     */
    private void addTagItems(PaginatedModal modal, Player player, String categoryId) {
        ConfigurationSection tagsSection = plugin.getTagsConfig().getConfigurationSection("tags");
        if (tagsSection == null) {
            plugin.getLogger().warning("No tags configured in tags.yml");
            return;
        }
        
        for (String tagId : tagsSection.getKeys(false)) {
            // Skip the DEFAULT tag - it should never be displayed
            if ("default".equalsIgnoreCase(tagId)) {
                continue;
            }
            
            // Check if tag should be shown based on permissions and filters
            if (shouldShowTag(player, tagId, categoryId)) {
                ModalItem tagItem = createTagItem(player, tagId);
                if (tagItem != null) {
                    modal.addItem(tagItem);
                }
            }
        }
    }
    
    /**
     * Checks if a tag should be shown for the given category and player with current filters
     * 
     * @param player The player
     * @param tagId The tag ID
     * @param categoryId The category ID
     * @return True if the tag should be shown
     */
    private boolean shouldShowTag(Player player, String tagId, String categoryId) {
        ConfigurationSection tag = plugin.getTagsConfig().getConfigurationSection("tags." + tagId);
        if (tag == null) {
            return false;
        }
        
        // Skip the DEFAULT tag completely
        if ("default".equalsIgnoreCase(tagId)) {
            return false;
        }
        
        // Check permissions
        String permission = tag.getString("permission", "");
        if (!permission.isEmpty() && !player.hasPermission(permission)) {
            return false;
        }
        
        // Check availability
        String availabilityType = tag.getString("availablity.type", "ALWAYS");
        if (!"ALWAYS".equalsIgnoreCase(availabilityType)) {
            // TODO: Implement time-based availability checking
            // For now, show all non-ALWAYS tags
        }
        
        // Apply current sort filters
        return tagsModal.shouldShowTagWithFilters(player, tagId);
    }
    
    /**
     * Creates a tag item for display
     * Uses proper tooltip formatting
     * 
     * @param player The player the item is for
     * @param tagId The tag ID
     * @return The created modal item, or null if creation failed
     */
    private ModalItem createTagItem(Player player, String tagId) {
        ConfigurationSection tag = plugin.getTagsConfig().getConfigurationSection("tags." + tagId);
        if (tag == null) {
            return null;
        }
        
        // Get material
        String materialName = tag.getString("material.id", "NAME_TAG");
        Material material = Material.valueOf(materialName.toUpperCase());
        
        // Get display name with proper formatting
        String name = tag.getString("name", tagId);
        String processedName = plugin.getDataProcessor().replaceVariables(name);
        Component nameComponent = formatTooltipText(processedName);
        
        // Get lore (combine description with controls) with proper formatting
        List<String> loreStrings = buildTagLore(player, tagId, tag);
        List<Component> loreComponents = new ArrayList<>();
        for (String loreString : loreStrings) {
            String processedLore = plugin.getDataProcessor().replaceVariables(loreString);
            loreComponents.add(formatTooltipText(processedLore));
        }
        
        // Create item builder with proper formatting
        ItemBuilder itemBuilder = ItemBuilder.from(material)
            .amount(1)
            .name(nameComponent)
            .lore(loreComponents);
        
        // Add custom model data if configured
        int customModelData = tag.getInt("material.custom-model-data", -1);
        if (customModelData != -1) {
            itemBuilder.model(customModelData);
        }
        
        // TODO: Check if tag is favorited and use fav-material if so
        
        // Create modal item with click actions
        return itemBuilder.asModalItem(event -> {
            String clickType = getClickTypeString(event.getClick());
            tagsModal.handleTagClick(player, tagId, clickType);
        });
    }
    
    /**
     * Builds the lore for a tag item
     * 
     * @param player The player the item is for
     * @param tagId The tag ID
     * @param tag The tag configuration section
     * @return List of lore strings (pre-processed, ready for TextUtility parsing)
     */
    private List<String> buildTagLore(Player player, String tagId, ConfigurationSection tag) {
        List<String> lore = new ArrayList<>();
        
        // TODO: Determine tag status (active, unlocked, locked, protected)
        String tagStatus = "unlocked"; // Placeholder
        
        // Get styled status label from language config
        String styledStatus = plugin.getInternalConfig().getString("settings.system.conditions." + tagStatus, tagStatus.toUpperCase());
        
        // Get lore template from language config
        String loreKey = "settings.system.gui.lore." + tagStatus;
        List<String> loreTemplate = plugin.getInternalConfig().getStringList(loreKey);
        
        for (String loreLine : loreTemplate) {
            // Replace tag-specific variables
            String processedLine = loreLine
                .replace("{tag.display}", tag.getString("display", ""))
                .replace("{tag.lore}", String.join(" ", tag.getStringList("lore")))
                .replace("{tag.status}", styledStatus);
            
            // Will be processed again by DataProcessor for all other variables
            lore.add(processedLine);
        }
        
        return lore;
    }
    
    /**
     * Converts ClickType to string for processing
     * 
     * @param clickType The click type
     * @return String representation
     */
    private String getClickTypeString(ClickType clickType) {
        switch (clickType) {
            case LEFT:
                return "left";
            case RIGHT:
                return "right";
            case SHIFT_LEFT:
                return "shift_left";
            case SHIFT_RIGHT:
                return "shift_right";
            default:
                return "unknown";
        }
    }
    
    /**
     * Gets the display name for a category
     * 
     * @param categoryId The category ID
     * @return The display name for the category
     */
    private String getCategoryDisplayName(String categoryId) {
        String nameKey = "category." + categoryId + ".name";
        String name = plugin.getCategoryConfig().getString(nameKey, null);
        
        if (name != null) {
            return TextUtility.processMessage(name);
        }
        
        return categoryId.substring(0, 1).toUpperCase() + categoryId.substring(1).toLowerCase();
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
     * Handles reset tag button click
     * 
     * @param player The player who clicked
     */
    private void handleResetTag(Player player) {
        // TODO: Implement tag reset logic
        String message = plugin.getInternalConfig().getString("messages.tag.reset",
            "<grey>Tag reset to default.</grey>");
        TextUtility.sendPlayerMessage(player, plugin.getDataProcessor().replaceVariables(message));
    }
    
    /**
     * Handles active tag display click
     * 
     * @param player The player who clicked
     */
    private void handleActiveTagDisplay(Player player) {
        // TODO: Implement active tag info display
        String message = plugin.getInternalConfig().getString("messages.tag.active-info",
            "<yellow>Your active tag:</yellow> <gold>{tag}</gold>");
        message = message.replace("{tag}", "None"); // Placeholder
        TextUtility.sendPlayerMessage(player, plugin.getDataProcessor().replaceVariables(message));
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
     * Parses a slot range string into individual slots
     * Supports formats like "10..16" or single numbers
     * 
     * @param slotRange The slot range string
     * @return List of slot numbers
     */
    private List<Integer> parseSlotRange(String slotRange) {
        List<Integer> slots = new ArrayList<>();
        
        if (slotRange.contains("..")) {
            String[] parts = slotRange.split("\\.\\.");
            if (parts.length == 2) {
                try {
                    int start = Integer.parseInt(parts[0]);
                    int end = Integer.parseInt(parts[1]);
                    for (int i = start; i <= end; i++) {
                        slots.add(i);
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid slot range format: " + slotRange);
                }
            }
        } else {
            try {
                slots.add(Integer.parseInt(slotRange));
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid slot number: " + slotRange);
            }
        }
        
        return slots;
    }
    
    /**
     * Parses multiple slot range strings
     * 
     * @param slotRanges List of slot range strings
     * @return List of individual slot numbers
     */
    private List<Integer> parseSlotRanges(List<String> slotRanges) {
        List<Integer> slots = new ArrayList<>();
        
        for (String range : slotRanges) {
            slots.addAll(parseSlotRange(range));
        }
        
        return slots;
    }
}