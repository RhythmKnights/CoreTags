package io.rhythmknights.coretags.component.modal;

import io.rhythmknights.coreapi.CoreAPI;
import io.rhythmknights.coreapi.modal.Modal;
import io.rhythmknights.coreapi.modal.PaginatedModal;
import io.rhythmknights.coreapi.modal.item.ItemBuilder;
import io.rhythmknights.coreapi.modal.item.ModalItem;
import io.rhythmknights.coreapi.modal.navigation.NavBuilder;
import io.rhythmknights.coreapi.modal.pagination.PaginationRegion;
import io.rhythmknights.coreframework.util.TextUtility;
import io.rhythmknights.coretags.CoreTags;
import io.rhythmknights.coretags.component.data.ConfigModule;
import io.rhythmknights.coretags.component.data.PlayerDataModule;
import io.rhythmknights.coretags.component.hook.VaultHook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Central processor for handling all modal-based GUI interactions in CoreTags.
 * 
 * <p>This class serves as the primary interface between the plugin's data models
 * and the CoreAPI modal system. It handles:</p>
 * <ul>
 *   <li>Creating and managing category selection modals</li>
 *   <li>Creating and managing tag selection modals with pagination</li>
 *   <li>Processing player interactions with modal elements</li>
 *   <li>Managing GUI session state and navigation</li>
 *   <li>Handling tag purchases and unlocking</li>
 *   <li>Managing filter and sort operations</li>
 * </ul>
 * 
 * <p>The processor maintains session state for each player, allowing for
 * complex navigation patterns and stateful GUI interactions.</p>
 * 
 * @author RhythmKnights
 * @version 2.1-HORIZON
 * @since 2.0.0
 * 
 * @apiNote This class integrates tightly with CoreAPI's modal system and
 *          should be the primary way to open GUIs for players.
 * @implNote All modal operations are asynchronous and thread-safe.
 */
public final class ModalProcessor {
    /** The main plugin instance for accessing other components. */
    private final CoreTags plugin;
    
    /** CoreAPI instance for creating and managing modals. */
    private final CoreAPI coreAPI;
    
    /** TextUtility for consistent text processing across the plugin. */
    private final TextUtility textUtility;
    
    /** Configuration module for accessing plugin settings. */
    private final ConfigModule cfg;
    
    /** Category modal for accessing category definitions. */
    private final CategoryModal cats;
    
    /** Tag modal for accessing tag definitions. */
    private final TagModal tags;
    
    /** Player data module for accessing player-specific information. */
    private final PlayerDataModule data;
    
    /** Economy hook for handling tag purchases. */
    private final VaultHook eco;
    
    /** Configuration file for categories. */
    private final FileConfiguration catCfg;
    
    /** Configuration file for tags. */
    private final FileConfiguration tagCfg;
    
    /** Default view mode (category or tags). */
    private final String defaultView;
    
    /** Whether to swap back buttons with close buttons on top-level modals. */
    private final boolean swapGlobal;
    
    /** Configuration for close button command execution. */
    private final ConfigModule.CloseCmd closeCfg;
    
    /** Map of player UUIDs to their current GUI sessions. */
    private final Map<UUID, GuiSession> openSessions = new HashMap<>();
    
    /**
     * List of available color filters for tag filtering.
     * These correspond to the color values that can be assigned to tags.
     */
    private static final List<String> COLORS = List.of(
        "ALL", "MULTI", "RED", "ORANGE", "YELLOW", "GREEN", 
        "BLUE", "PURPLE", "PINK", "BROWN", "GRAY", "BLACK", "WHITE"
    );

    /**
     * Constructs a new ModalProcessor with the specified plugin instance.
     * 
     * <p>This constructor initializes all dependencies and loads configuration
     * files for categories and tags. It does not register any event listeners
     * or create any modals - those operations are handled by the individual
     * modal opening methods.</p>
     * 
     * @param plugin the CoreTags plugin instance
     * 
     * @throws IllegalStateException if required dependencies are not available
     * @implNote This constructor should only be called during plugin initialization.
     */
    public ModalProcessor(@NotNull CoreTags plugin) {
        this.plugin = plugin;
        this.coreAPI = plugin.coreAPI();
        this.textUtility = plugin.textUtility();
        this.cfg = plugin.configs();
        this.cats = plugin.categories();
        this.tags = plugin.tags();
        this.data = plugin.playerData();
        this.eco = plugin.economy();
        this.defaultView = plugin.getConfig().getString("settings.system.default-view", "category").toLowerCase(Locale.ROOT);
        this.swapGlobal = plugin.getConfig().getBoolean("settings.system.close-button-swap", true);
        this.closeCfg = this.cfg.closeCmd();
        
        File catFile = new File(plugin.getDataFolder(), "components/categories.yml");
        this.catCfg = YamlConfiguration.loadConfiguration(catFile);
        File tagFile = new File(plugin.getDataFolder(), "components/tags.yml");
        this.tagCfg = YamlConfiguration.loadConfiguration(tagFile);
    }

    /**
     * Reloads configuration files from disk.
     * 
     * <p>This method reloads both the main plugin configuration and the
     * component-specific configuration files (categories.yml and tags.yml).
     * It's called during plugin reload operations to ensure all settings
     * are up to date.</p>
     * 
     * @apiNote This method is safe to call while players have modals open,
     *          but they may need to reopen them to see changes.
     * @implNote Configuration parsing errors are logged but do not prevent
     *           the reload operation from completing.
     */
    public void reloadFileConfigs() {
        this.plugin.reloadConfig();
        File catFile = new File(this.plugin.getDataFolder(), "components/categories.yml");
        try {
            ((YamlConfiguration) this.catCfg).load(catFile);
        } catch (IOException e) {
            this.plugin.getLogger().severe("Could not reload components/categories.yml: " + e.getMessage());
        }
        
        File tagFile = new File(this.plugin.getDataFolder(), "components/tags.yml");
        try {
            ((YamlConfiguration) this.tagCfg).load(tagFile);
        } catch (IOException e) {
            this.plugin.getLogger().severe("Could not reload components/tags.yml: " + e.getMessage());
        }
    }

    /**
     * Closes all open modals and clears session data.
     * 
     * <p>This method is typically called during plugin shutdown or reload
     * operations to ensure all players have their inventories properly
     * closed and no session data is left in memory.</p>
     * 
     * @apiNote This method is safe to call multiple times and will not
     *          cause errors if no modals are currently open.
     * @implNote This operation is synchronous and will complete before returning.
     */
    public void refreshAll() {
        this.openSessions.keySet().forEach(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.closeInventory();
            }
        });
        this.openSessions.clear();
    }

    /**
     * Closes all currently open modals for all players.
     * 
     * <p>This is an alias for {@link #refreshAll()} provided for semantic clarity
     * when the intent is specifically to close modals rather than refresh state.</p>
     * 
     * @see #refreshAll()
     */
    public void closeAllModals() {
        this.refreshAll();
    }

    /**
     * Opens the category selection GUI for the specified player.
     * 
     * <p>This modal displays all available tag categories that the player has
     * permission to access. Categories are displayed as clickable items that
     * will open the tags GUI filtered to that specific category.</p>
     * 
     * <p>The modal includes navigation elements such as:</p>
     * <ul>
     *   <li>Reset button to clear the player's active tag</li>
     *   <li>Active tag display showing the current selection</li>
     *   <li>Close/back button depending on configuration</li>
     * </ul>
     * 
     * @param player the player to open the modal for
     * 
     * @throws IllegalArgumentException if the player is null
     * @apiNote This method will create a new GUI session for the player,
     *          replacing any existing session.
     * @implNote The modal layout is controlled by configuration settings
     *           and will adapt to the configured number of rows.
     */
    public void openCategoryGui(@NotNull Player player) {
        int rows = this.plugin.getConfig().getInt("settings.gui.category-menu.rows", 4);
        String titlePattern = this.plugin.getConfig().getString("settings.gui.layout.titles.category-gui-name", "&8Tags | Categories");
        Component title = this.textUtility.parseText(titlePattern);
        
        PaginatedModal modal = Modal.paginated()
                .rows(rows)
                .title(title)
                .create();

        // Add category items
        this.cats.all().stream()
                .filter(c -> player.hasPermission(c.permission()))
                .sorted(Comparator.comparingInt(CategoryModal.TagCategory::slot))
                .forEach(category -> {
                    ItemBuilder itemBuilder = ItemBuilder.from(category.icon())
                            .name(category.displayName())
                            .lore(category.lore());
                    
                    ModalItem modalItem = itemBuilder.asModalItem(event -> {
                        if (event.getWhoClicked() instanceof Player p) {
                            Bukkit.getScheduler().runTask(this.plugin, () -> 
                                this.openTagsGui(p, category.key(), 0));
                        }
                    });
                    
                    modal.setItem(category.slot(), modalItem);
                });

        // Add navigation buttons
        this.addCategoryNavigationButtons(modal, player);
        
        modal.open(player);
        
        String cfgSort = this.plugin.getConfig().getString("settings.system.favorites-sort", "UNSORTED").toUpperCase(Locale.ROOT);
        Sort defaultSort;
        try {
            defaultSort = Sort.valueOf(cfgSort);
        } catch (IllegalArgumentException e) {
            defaultSort = Sort.UNSORTED;
        }
        
        this.openSessions.put(player.getUniqueId(), new GuiSession(GuiType.CATEGORY, 0, "ALL", defaultSort, "ALL"));
    }

    /**
     * Opens the tags selection GUI for the specified player with filtering and pagination.
     * 
     * <p>This modal displays tags based on the specified filters and supports
     * pagination for large numbers of tags. The modal includes various filtering
     * and sorting options:</p>
     * <ul>
     *   <li>Category filtering (show only tags from specific categories)</li>
     *   <li>Status filtering (all, favorites, unlocked, locked, protected)</li>
     *   <li>Color filtering (filter by tag color)</li>
     *   <li>Sorting options (alphabetical, favorites first)</li>
     * </ul>
     * 
     * <p>Navigation controls include:</p>
     * <ul>
     *   <li>Previous/Next page buttons for pagination</li>
     *   <li>Filter control buttons</li>
     *   <li>Reset and active tag display</li>
     *   <li>Back/close buttons</li>
     * </ul>
     * 
     * @param player the player to open the modal for
     * @param categoryFilter the category to filter by, or null to use current filter
     * @param page the page number to display, or -999 to maintain current page
     * 
     * @throws IllegalArgumentException if the player is null
     * @apiNote Page numbers are zero-based and will be automatically clamped
     *          to valid ranges based on the filtered content.
     * @implNote This method will update or create a GUI session for the player
     *           with the specified parameters.
     */
    public void openTagsGui(@NotNull Player player, @Nullable String categoryFilter, int page) {
        GuiSession session = this.openSessions.computeIfAbsent(player.getUniqueId(), uuid -> {
            String cfgSort = this.plugin.getConfig().getString("settings.system.favorites-sort", "UNSORTED").toUpperCase(Locale.ROOT);
            Sort defaultSort;
            try {
                defaultSort = Sort.valueOf(cfgSort);
            } catch (IllegalArgumentException e) {
                defaultSort = Sort.UNSORTED;
            }
            return new GuiSession(GuiType.TAGS, 0, "ALL", defaultSort, "ALL");
        });
        
        session.type = GuiType.TAGS;
        if (categoryFilter != null) {
            session.filter = categoryFilter;
        }
        if (page != -999) {
            session.page = page;
        }

        List<TagModal.Tag> filteredTags = this.applyFilterAndSort(player, session);
        List<Integer> slots = this.cfg.guiSlots();
        int perPage = slots.size();
        int maxPage = Math.max(0, (filteredTags.size() - 1) / perPage);
        session.page = Math.min(Math.max(0, session.page), maxPage);

        int rows = this.plugin.getConfig().getInt("settings.gui.tags-menu.rows", 6);
        String pattern = this.plugin.getConfig().getString("settings.gui.layout.titles.tags-gui-name", "&8Tags | {category} &7({currentpage}/{totalpages})");
        String filterName = this.catCfg.getString("settings.system.category-sort.filters." + session.filter.toLowerCase(Locale.ROOT) + ".name", session.filter);
        String titleRaw = pattern.replace("{category}", filterName)
                .replace("{currentpage}", String.valueOf(session.page + 1))
                .replace("{totalpages}", String.valueOf(maxPage + 1));
        Component title = this.textUtility.parseText(titleRaw);
        
        // Create pagination region
        PaginationRegion region = PaginationRegion.fromSlots(slots);
        
        PaginatedModal modal = Modal.paginated()
                .rows(rows)
                .title(title)
                .paginationRegion(region)
                .create();

        // Add paginated tag items
        int base = session.page * perPage;
        for (int i = 0; i < perPage && base + i < filteredTags.size(); i++) {
            TagModal.Tag tag = filteredTags.get(base + i);
            ModalItem tagItem = this.buildTagItem(player, tag);
            modal.addItem(tagItem);
        }

        // Add navigation buttons
        this.addTagsNavigationButtons(modal, player, session);
        
        modal.open(player);
        this.openSessions.put(player.getUniqueId(), session);
    }

    private void addCategoryNavigationButtons(PaginatedModal modal, Player player) {
        // Reset button
        int resetSlot = this.plugin.getConfig().getInt("settings.gui.layout.items.reset-button-slot-category", -1);
        if (resetSlot >= 0) {
            ModalItem resetButton = ItemBuilder.from(Material.RED_DYE)
                    .name(this.textUtility.parseText("&cReset Tag"))
                    .asModalItem(event -> {
                        if (event.getWhoClicked() instanceof Player p) {
                            this.data.setActive(p.getUniqueId(), "none");
                            Component message = this.textUtility.parseText(this.plugin.getConfig().getString("settings.messages.tag-reset", ""));
                            p.sendMessage(message);
                            Bukkit.getScheduler().runTask(this.plugin, () -> this.openCategoryGui(p));
                        }
                    });
            modal.setItem(resetSlot, resetButton);
        }

        // Active tag display
        int activeSlot = this.plugin.getConfig().getInt("settings.gui.layout.items.active-tag-item-slot-category", -1);
        if (activeSlot >= 0) {
            String activeId = this.data.get(player.getUniqueId()).active;
            String activeName = activeId != null && !activeId.equalsIgnoreCase("none") && this.tags.byId(activeId).isPresent()
                    ? PlainTextComponentSerializer.plainText().serialize(this.tags.byId(activeId).get().name())
                    : this.tagCfg.getString("settings.system.empty-tag.name", "&7None");
            
            ModalItem activeButton = ItemBuilder.from(Material.NAME_TAG)
                    .name(this.textUtility.parseText("&6Active Tag &8• &7" + activeName))
                    .asModalItem(event -> {
                        // Maybe open a submenu or do nothing
                    });
            modal.setItem(activeSlot, activeButton);
        }

        // Close/Back button
        boolean topCategory = this.defaultView.equals("category");
        if (this.swapGlobal && topCategory) {
            int closeSlot = this.plugin.getConfig().getInt("settings.gui.layout.items.close-button-slot-category", -1);
            if (closeSlot >= 0) {
                ModalItem closeButton = ItemBuilder.from(Material.BARRIER)
                        .name(this.textUtility.parseText("&cExit"))
                        .asModalItem(event -> {
                            if (event.getWhoClicked() instanceof Player p) {
                                this.handleTopClose(p);
                            }
                        });
                modal.setItem(closeSlot, closeButton);
            }
        } else {
            int backSlot = this.plugin.getConfig().getInt("settings.gui.layout.items.back-button-slot-category", -1);
            if (backSlot >= 0) {
                ModalItem backButton = ItemBuilder.from(Material.SPECTRAL_ARROW)
                        .name(this.textUtility.parseText("&cBack"))
                        .asModalItem(event -> {
                            if (event.getWhoClicked() instanceof Player p) {
                                this.handleTopClose(p);
                            }
                        });
                modal.setItem(backSlot, backButton);
            }
        }
    }

    private void addTagsNavigationButtons(PaginatedModal modal, Player player, GuiSession session) {
        // Previous page button
        int prevSlot = this.plugin.getConfig().getInt("settings.gui.layout.items.last-page-button-slot", 48);
        ModalItem prevButton = ItemBuilder.from(Material.ARROW)
                .name(this.textUtility.parseText("&7Previous Page"))
                .asModalItem(event -> {
                    if (event.getWhoClicked() instanceof Player p) {
                        Bukkit.getScheduler().runTask(this.plugin, () -> 
                            this.openTagsGui(p, null, session.page - 1));
                    }
                });
        modal.setItem(prevSlot, prevButton);

        // Next page button
        int nextSlot = this.plugin.getConfig().getInt("settings.gui.layout.items.next-page-button-slot", 50);
        ModalItem nextButton = ItemBuilder.from(Material.ARROW)
                .name(this.textUtility.parseText("&7Next Page"))
                .asModalItem(event -> {
                    if (event.getWhoClicked() instanceof Player p) {
                        Bukkit.getScheduler().runTask(this.plugin, () -> 
                            this.openTagsGui(p, null, session.page + 1));
                    }
                });
        modal.setItem(nextSlot, nextButton);

        // Category filter button
        int catSlot = this.plugin.getConfig().getInt("settings.gui.layout.items.category-sort-button-slot", 17);
        ModalItem categoryButton = ItemBuilder.from(Material.GOLD_BLOCK)
                .name(this.categoryButtonName(session.filter))
                .asModalItem(event -> {
                    if (event.getWhoClicked() instanceof Player p) {
                        if (event.getClick() == ClickType.RIGHT) {
                            session.filter = this.prevFilter(p, session.filter);
                        } else {
                            session.filter = this.nextFilter(p, session.filter);
                        }
                        Bukkit.getScheduler().runTask(this.plugin, () -> 
                            this.openTagsGui(p, null, 0));
                    }
                });
        modal.setItem(catSlot, categoryButton);

        // Favorites sort button
        int favSlot = this.plugin.getConfig().getInt("settings.gui.layout.items.favorite-sort-button-slot", 26);
        ModalItem favButton = ItemBuilder.from(Material.NETHER_STAR)
                .name(this.favoriteButtonName(session.sort))
                .asModalItem(event -> {
                    if (event.getWhoClicked() instanceof Player p) {
                        session.sort = session.sort == Sort.UNSORTED ? Sort.SORTED : Sort.UNSORTED;
                        Bukkit.getScheduler().runTask(this.plugin, () -> 
                            this.openTagsGui(p, null, -999));
                    }
                });
        modal.setItem(favSlot, favButton);

        // Color filter button
        int colorSlot = this.plugin.getConfig().getInt("settings.gui.layout.items.color-sort-button-slot", 35);
        if (this.plugin.getConfig().getBoolean("settings.gui.layout.items.color-sort-button", false)) {
            ModalItem colorButton = ItemBuilder.from(Material.BRUSH)
                    .name(this.colorButtonTitle(session.colorFilter))
                    .asModalItem(event -> {
                        if (event.getWhoClicked() instanceof Player p) {
                            if (event.getClick() == ClickType.RIGHT) {
                                session.colorFilter = this.prevColor(session.colorFilter);
                            } else {
                                session.colorFilter = this.nextColor(session.colorFilter);
                            }
                            Bukkit.getScheduler().runTask(this.plugin, () -> 
                                this.openTagsGui(p, null, session.page));
                        }
                    });
            modal.setItem(colorSlot, colorButton);
        }

        // Reset button
        int resetSlot = this.plugin.getConfig().getInt("settings.gui.layout.items.reset-button-slot-tags", 49);
        ModalItem resetButton = ItemBuilder.from(Material.RED_DYE)
                .name(this.textUtility.parseText("&cReset Tag"))
                .asModalItem(event -> {
                    if (event.getWhoClicked() instanceof Player p) {
                        this.data.setActive(p.getUniqueId(), "none");
                        Component message = this.textUtility.parseText(this.plugin.getConfig().getString("settings.messages.tag-reset", ""));
                        p.sendMessage(message);
                        Bukkit.getScheduler().runTask(this.plugin, () -> 
                            this.openTagsGui(p, null, -999));
                    }
                });
        modal.setItem(resetSlot, resetButton);

        // Active tag display
        int activeSlot = this.plugin.getConfig().getInt("settings.gui.layout.items.active-tag-item-slot-tags", 53);
        String activeId = this.data.get(player.getUniqueId()).active;
        String activeName = activeId != null && !activeId.equalsIgnoreCase("none") && this.tags.byId(activeId).isPresent()
                ? PlainTextComponentSerializer.plainText().serialize(this.tags.byId(activeId).get().name())
                : this.tagCfg.getString("settings.system.empty-tag.name", "&7None");
        
        ModalItem activeButton = ItemBuilder.from(Material.NAME_TAG)
                .name(this.textUtility.parseText("&6Active Tag &8• &7" + activeName))
                .asModalItem(event -> {
                    // Maybe open a submenu or do nothing
                });
        modal.setItem(activeSlot, activeButton);

        // Back/Close buttons
        boolean topTags = this.defaultView.equals("tags");
        if (this.swapGlobal && topTags) {
            int closeSlot = this.plugin.getConfig().getInt("settings.gui.layout.items.close-button-slot-tags", 45);
            ModalItem closeButton = ItemBuilder.from(Material.BARRIER)
                    .name(this.textUtility.parseText("&cExit"))
                    .asModalItem(event -> {
                        if (event.getWhoClicked() instanceof Player p) {
                            this.handleTopClose(p);
                        }
                    });
            modal.setItem(closeSlot, closeButton);
        } else {
            int backSlot = this.plugin.getConfig().getInt("settings.gui.layout.items.back-button-slot-tags", 45);
            ModalItem backButton = ItemBuilder.from(Material.SPECTRAL_ARROW)
                    .name(this.textUtility.parseText("&cBack"))
                    .asModalItem(event -> {
                        if (event.getWhoClicked() instanceof Player p) {
                            Bukkit.getScheduler().runTask(this.plugin, () -> 
                                this.openCategoryGui(p));
                        }
                    });
            modal.setItem(backSlot, backButton);

            int closeSlot = this.plugin.getConfig().getInt("settings.gui.layout.items.close-button-slot-tags", 45);
            ModalItem closeButton = ItemBuilder.from(Material.BARRIER)
                    .name(this.textUtility.parseText("&cExit"))
                    .asModalItem(event -> {
                        if (event.getWhoClicked() instanceof Player p) {
                            this.handleTopClose(p);
                        }
                    });
            modal.setItem(closeSlot, closeButton);
        }
    }

    private ModalItem buildTagItem(Player player, TagModal.Tag tag) {
        PlayerDataModule.PlayerData pd = this.data.get(player.getUniqueId());
        boolean unlocked = pd.unlocked.contains(tag.id()) || tag.cost() == 0 || !this.eco.active();
        
        ConfigModule.GameState state;
        if (!player.hasPermission(tag.permission())) {
            state = ConfigModule.GameState.PROTECTED;
        } else if (tag.id().equals(pd.active)) {
            state = ConfigModule.GameState.ACTIVE;
        } else if (unlocked) {
            state = ConfigModule.GameState.UNLOCKED;
        } else {
            state = ConfigModule.GameState.LOCKED;
        }

        String basePath = "settings.gui.tags.tag-items.";
        String lorePath = switch (state) {
            case ACTIVE -> basePath + "active-lore";
            case LOCKED -> basePath + "locked-lore";
            case UNLOCKED -> basePath + "unlocked-lore";
            default -> basePath + "protected-lore";
        };

        boolean isFavorite = pd.favorites.contains(tag.id());
        String favMsg = this.tagCfg.getString("settings.system.favorite.msg." + (isFavorite ? "remove" : "add"), "");
        String favState = this.tagCfg.getString("settings.system.favorite.state." + (isFavorite ? "enabled" : "disabled"), "");
        
        List<String> loreTemplate = this.plugin.getConfig().getStringList(lorePath);
        List<Component> lore = new ArrayList<>();
        
        for (String line : loreTemplate) {
            String replaced = line
                    .replace("{display}", PlainTextComponentSerializer.plainText().serialize(tag.display()))
                    .replace("{cost}", String.valueOf(tag.cost()))
                    .replace("{status}", this.tags.statusText(state))
                    .replace("{favoritemsg}", favMsg)
                    .replace("{favoritestate}", favState);
            
            if (replaced.contains("{description}")) {
                String[] parts = replaced.split("\\{description\\}", -1);
                Component prefix = this.textUtility.parseText(parts[0]);
                Component suffix = parts.length > 1 ? this.textUtility.parseText(parts[1]) : Component.empty();
                
                for (Component desc : tag.description()) {
                    lore.add(prefix.append(desc).append(suffix));
                }
            } else {
                lore.add(this.textUtility.parseText(replaced));
            }
        }

        Material material = isFavorite ? Material.NAME_TAG : tag.icon(); // Use favorite material if favorited
        
        ItemBuilder itemBuilder = ItemBuilder.from(material)
                .name(tag.name())
                .lore(lore);

        if (isFavorite) {
            int customModelData = this.plugin.getConfig().getInt("settings.gui.layout.materials.favorite-tag-material.custom-model-data", 0);
            if (customModelData > 0) {
                itemBuilder.modelData(customModelData);
            }
        }

        if (state == ConfigModule.GameState.ACTIVE && this.plugin.getConfig().getBoolean("settings.system.active-tag-glow", true)) {
            itemBuilder.glow();
        }

        return itemBuilder.asModalItem(event -> {
            if (event.getWhoClicked() instanceof Player p) {
                this.handleTagClick(p, tag, event.getClick());
            }
        });
    }

    private void handleTagClick(Player player, TagModal.Tag tag, ClickType click) {
        PlayerDataModule.PlayerData pd = this.data.get(player.getUniqueId());
        boolean unlocked = pd.unlocked.contains(tag.id()) || tag.cost() == 0 || !this.eco.active();
        
        switch (click) {
            case LEFT:
                if (unlocked) {
                    this.data.setActive(player.getUniqueId(), tag.id());
                    String message = this.plugin.getConfig().getString("settings.messages.tag-activate", "");
                    message = message.replace("{activetag}", PlainTextComponentSerializer.plainText().serialize(tag.name()))
                            .replace("{tagdisplay}", PlainTextComponentSerializer.plainText().serialize(tag.display()));
                    message = this.replaceConditionPlaceholders(message);
                    Component msg = this.textUtility.parseText(message);
                    player.sendMessage(msg);
                } else {
                    String message = this.plugin.getConfig().getString("settings.messages.tag-locked", "");
                    message = message.replace("{tag}", PlainTextComponentSerializer.plainText().serialize(tag.name()));
                    message = this.replaceConditionPlaceholders(message);
                    Component msg = this.textUtility.parseText(message);
                    player.sendMessage(msg);
                }
                break;
            case SHIFT_LEFT:
                if (!unlocked) {
                    this.attemptPurchase(player, tag);
                }
                break;
            case RIGHT:
                this.data.toggleFavorite(player.getUniqueId(), tag.id());
                break;
            case MIDDLE:
                this.data.toggleFavorite(player.getUniqueId(), tag.id());
                if (unlocked) {
                    this.data.setActive(player.getUniqueId(), tag.id());
                }
                break;
        }

        Bukkit.getScheduler().runTask(this.plugin, () -> this.openTagsGui(player, null, -999));
    }

    private void attemptPurchase(Player player, TagModal.Tag tag) {
        double cost = tag.cost();
        if (!this.eco.canAfford(player, cost)) {
            String message = this.plugin.getConfig().getString("settings.messages.tag-balance", "&cInsufficient funds. &7You need &c{cost} &7to unlock the {tag} &7tag.")
                    .replace("{cost}", String.valueOf(cost))
                    .replace("{tag}", PlainTextComponentSerializer.plainText().serialize(tag.name()));
            Component msg = this.textUtility.parseText(message);
            player.sendMessage(msg);
        } else {
            this.eco.withdraw(player, cost);
            this.data.unlockTag(player.getUniqueId(), tag.id());
            String message = this.plugin.getConfig().getString("settings.messages.tag-unlocked", "")
                    .replace("{cost}", String.valueOf(cost))
                    .replace("{tag}", PlainTextComponentSerializer.plainText().serialize(tag.name()));
            Component msg = this.textUtility.parseText(message);
            player.sendMessage(msg);
        }
    }

    private List<TagModal.Tag> applyFilterAndSort(Player player, GuiSession session) {
        String filter = session.filter.toUpperCase(Locale.ROOT);
        List<TagModal.Tag> src = switch (filter) {
            case "ALL" -> this.accessibleTags(player);
            case "FAVORITES" -> this.accessibleTags(player).stream()
                    .filter(t -> this.data.get(player.getUniqueId()).favorites.contains(t.id()))
                    .toList();
            case "UNLOCKED" -> this.accessibleTags(player).stream()
                    .filter(t -> this.data.get(player.getUniqueId()).unlocked.contains(t.id()) || t.cost() == 0 || !this.eco.active())
                    .toList();
            case "LOCKED" -> this.accessibleTags(player).stream()
                    .filter(t -> !this.data.get(player.getUniqueId()).unlocked.contains(t.id()) && t.cost() != 0 && this.eco.active())
                    .toList();
            case "PROTECTED" -> this.cats.all().stream()
                    .filter(CategoryModal.TagCategory::isProtected)
                    .flatMap(c -> this.tags.byCategory(c.key()).stream())
                    .toList();
            default -> this.tags.byCategory(session.filter);
        };

        if (!session.colorFilter.equalsIgnoreCase("ALL")) {
            String colorFilter = session.colorFilter.equals("GREY") ? "GRAY" : session.colorFilter;
            src = src.stream()
                    .filter(tag -> tag.color().equalsIgnoreCase(colorFilter))
                    .toList();
        }

        if (session.sort == Sort.SORTED) {
            Set<String> favorites = this.data.get(player.getUniqueId()).favorites;
            src = src.stream()
                    .sorted(Comparator.comparing((TagModal.Tag t) -> !favorites.contains(t.id()))
                            .thenComparing(t -> PlainTextComponentSerializer.plainText().serialize(t.name()), String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());
        }

        return src;
    }

    private List<TagModal.Tag> accessibleTags(Player player) {
        return this.cats.all().stream()
                .filter(c -> player.hasPermission(c.permission()))
                .sorted(Comparator.comparingInt(CategoryModal.TagCategory::slot))
                .flatMap(c -> this.tags.byCategory(c.key()).stream())
                .collect(Collectors.toList());
    }

    private List<String> filterOrder(Player player) {
        List<String> order = new ArrayList<>();
        ConfigurationSection fs = this.catCfg.getConfigurationSection("settings.system.category-sort.filters");
        if (fs != null) {
            for (String key : fs.getKeys(false)) {
                String id = fs.getString(key + ".id", key).toUpperCase(Locale.ROOT);
                order.add(id);
            }
        }

        this.cats.all().forEach(c -> {
            String id = c.key().toUpperCase(Locale.ROOT);
            if (!order.contains(id) && player.hasPermission(c.permission())) {
                order.add(id);
            }
        });

        return order;
    }

    private String nextFilter(Player player, String current) {
        List<String> order = this.filterOrder(player);
        int index = order.indexOf(current.toUpperCase(Locale.ROOT));
        if (index < 0) index = 0;
        return order.get((index + 1) % order.size());
    }

    private String prevFilter(Player player, String current) {
        List<String> order = this.filterOrder(player);
        int index = order.indexOf(current.toUpperCase(Locale.ROOT));
        if (index < 0) index = 0;
        return order.get((index - 1 + order.size()) % order.size());
    }

    private String nextColor(String current) {
        int index = COLORS.indexOf(current.toUpperCase(Locale.ROOT));
        if (index < 0) index = 0;
        return COLORS.get((index + 1) % COLORS.size());
    }

    private String prevColor(String current) {
        int index = COLORS.indexOf(current.toUpperCase(Locale.ROOT));
        if (index < 0) index = 0;
        return COLORS.get((index - 1 + COLORS.size()) % COLORS.size());
    }

    private Component categoryButtonName(String filterKey) {
        String base = "settings.system.category-sort.";
        String pattern = this.catCfg.getString(base + "sort-button.name", "&7CATEGORY &8• {filter}");
        String name = this.catCfg.getString(base + "filters." + filterKey.toLowerCase(Locale.ROOT) + ".name", filterKey);
        return this.textUtility.parseText(pattern.replace("{filter}", name));
    }

    private Component favoriteButtonName(Sort sort) {
        String base = "settings.system.favorites-sort.";
        String pattern = this.catCfg.getString(base + "sort-button.name", "&7FAVORITES &8• {sort-type}");
        String key = sort.name().toLowerCase(Locale.ROOT);
        String name = this.catCfg.getString(base + "sort-type." + key + ".name", sort.name());
        return this.textUtility.parseText(pattern.replace("{sort-type}", name));
    }

    private Component colorButtonTitle(String colorKey) {
        String path = "settings.system.colors." + colorKey.toLowerCase(Locale.ROOT) + ".text";
        String raw = this.plugin.getConfig().getString(path, colorKey);
        return this.textUtility.parseText(raw);
    }

    private void handleTopClose(Player player) {
        if (this.closeCfg.enabled()) {
            if (this.closeCfg.closeGuiFirst()) {
                player.closeInventory();
                Bukkit.getScheduler().runTask(this.plugin, () -> this.runCloseCommands(player));
            } else {
                this.runCloseCommands(player);
                player.closeInventory();
            }
        } else {
            player.closeInventory();
        }
    }

    private void runCloseCommands(Player player) {
        for (String cmd : this.closeCfg.commands()) {
            if (cmd != null && !cmd.isBlank()) {
                String command = cmd.replace("%player%", player.getName());
                if (this.closeCfg.runAsConsole()) {
                    this.plugin.getServer().dispatchCommand(this.plugin.getServer().getConsoleSender(), command);
                } else {
                    this.plugin.getServer().dispatchCommand(player, command);
                }
            }
        }
    }

    private String replaceConditionPlaceholders(String message) {
        ConfigurationSection conditionSection = this.plugin.getConfig().getConfigurationSection("settings.system.conditions");
        if (conditionSection != null) {
            for (String key : conditionSection.getKeys(false)) {
                String conditionText = conditionSection.getString(key + ".text", "");
                message = message.replace("{" + key + "}", conditionText);
            }
        }
        return message;
    }

    // Inner classes
    private enum Sort {
        UNSORTED, SORTED
    }

    private enum GuiType {
        CATEGORY, TAGS
    }

    private static final class GuiSession {
        GuiType type;
        int page;
        String filter;
        Sort sort;
        String colorFilter;

        GuiSession(GuiType type, int page, String filter, Sort sort, String colorFilter) {
            this.type = type;
            this.page = page;
            this.filter = filter;
            this.sort = sort;
            this.colorFilter = colorFilter;
        }
    }
}