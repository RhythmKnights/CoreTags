package io.rhythmknights.coretags.component.modal;

import io.rhythmknights.coreapi.component.modal.BaseModal;
import io.rhythmknights.coreapi.component.modal.ModalItem;
import io.rhythmknights.coreapi.modal.builder.item.ItemBuilder;
import io.rhythmknights.coretags.CoreTags;
import io.rhythmknights.coretags.component.data.ConfigModule;
import io.rhythmknights.coretags.component.data.PlayerDataModule;
import io.rhythmknights.coretags.component.hook.VaultHook;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ModalProcessor implements Listener {
    private final CoreTags plugin;
    private final ConfigModule cfg;
    private final CategoryModal cats;
    private final TagModal tags;
    private final PlayerDataModule data;
    private final VaultHook eco;
    private final FileConfiguration catCfg;
    private final FileConfiguration tagCfg;
    private final String defaultView;
    private final boolean swapGlobal;
    private final ConfigModule.CloseCmd closeCfg;
    private final Map<UUID, GuiSession> open = new HashMap<>();

    // Button configurations
    private Btn catBtn;
    private Btn favBtn;
    private Btn prevBtn;
    private Btn nextBtn;
    private Btn resetBtn;
    private Btn backBtn;
    private Btn closeBtn;
    private Btn activeBtn;
    private Btn colorSortBtn;
    private boolean colorSwitchMaterial;
    private boolean categorySwitchMaterial;

    private static final List<String> COLORS = List.of("ALL", "MULTI", "RED", "ORANGE", "YELLOW", 
        "GREEN", "BLUE", "PURPLE", "PINK", "BROWN", "GRAY", "BLACK", "WHITE");

    public ModalProcessor(CoreTags pl) {
        this.plugin = pl;
        this.cfg = pl.configs();
        this.cats = pl.categories();
        this.tags = pl.tags();
        this.data = pl.playerData();
        this.eco = pl.economy();
        this.defaultView = plugin.getConfig().getString("settings.system.default-view", "category").toLowerCase(Locale.ROOT);
        this.swapGlobal = plugin.getConfig().getBoolean("settings.system.close-button-swap", true);
        this.closeCfg = cfg.closeCmd();

        File catFile = new File(plugin.getDataFolder(), "components/categories.yml");
        this.catCfg = YamlConfiguration.loadConfiguration(catFile);
        File tagFile = new File(plugin.getDataFolder(), "components/tags.yml");
        this.tagCfg = YamlConfiguration.loadConfiguration(tagFile);

        loadButtonMeta();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void loadButtonMeta() {
        String base = "settings.gui.layout.items.";
        this.catBtn = new Btn(mat("category-sort-button-material"), slot(base + "category-sort-button-slot"));
        this.favBtn = new Btn(mat("favorite-sort-button-material"), slot(base + "favorite-sort-button-slot"));
        this.prevBtn = new Btn(mat("last-page-button-material"), slot(base + "last-page-button-slot"));
        this.nextBtn = new Btn(mat("next-page-button-material"), slot(base + "next-page-button-slot"));
        this.resetBtn = new Btn(mat("reset-button-material"), slot(base + "reset-button-slot-tags"));
        this.backBtn = new Btn(mat("back-button-material"), slot(base + "back-button-slot-tags"));
        this.closeBtn = new Btn(mat("close-button-material"), slot(base + "close-button-slot-tags"));
        this.activeBtn = new Btn(mat("active-tag-material"), slot(base + "active-tag-item-slot-tags"));
        this.colorSortBtn = new Btn(mat("color-sort-button-material"), slot(base + "color-sort-button-slot"));
        
        this.colorSwitchMaterial = plugin.getConfig().getBoolean("settings.gui.layout.materials.color-sort-button-material.material-switch", false);
        this.categorySwitchMaterial = plugin.getConfig().getBoolean("settings.gui.layout.materials.category-sort-button-material.material-switch", false);
    }

    public void reloadFileConfigs() {
        plugin.reloadConfig();
        File catFile = new File(plugin.getDataFolder(), "components/categories.yml");
        try {
            ((YamlConfiguration) catCfg).load(catFile);
        } catch (InvalidConfigurationException | IOException e) {
            plugin.getLogger().severe("Could not reload components/categories.yml: " + e.getMessage());
        }

        File tagFile = new File(plugin.getDataFolder(), "components/tags.yml");
        try {
            ((YamlConfiguration) tagCfg).load(tagFile);
        } catch (InvalidConfigurationException | IOException e) {
            plugin.getLogger().severe("Could not reload components/tags.yml: " + e.getMessage());
        }

        loadButtonMeta();
    }

    public void refreshAll() {
        for (UUID id : open.keySet()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                p.closeInventory();
            }
        }
        open.clear();
    }

    public void openCategoryGui(Player player) {
        try {
            int rows = plugin.getConfig().getInt("settings.gui.category-menu.rows", 4);
            String titlePattern = plugin.getConfig().getString("settings.gui.layout.titles.category-gui-name", "Tags | Categories");
            
            // Use CoreFramework TextUtility directly
            Component title = parseText(titlePattern);
            
            // Use BaseModal instead of Modal
            BaseModal modal = io.rhythmknights.coreapi.component.modal.Modal.modal()
                .title(title)
                .rows(rows)
                .disableAllInteractions()
                .create();

            // Add category items
            cats.all().stream()
                .filter(c -> player.hasPermission(c.permission()))
                .sorted(Comparator.comparingInt(CategoryModal.TagCategory::slot))
                .forEach(c -> {
                    // Use components directly - they're already parsed in CategoryModal
                    Component name = c.displayName();
                    List<Component> lore = c.lore();
                    
                    ModalItem item = ItemBuilder.from(c.icon())
                        .name(name)
                        .lore(lore)
                        .asModalItem(event -> {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                openTagsGui(player, c.key(), 0);
                            });
                        });
                    
                    modal.setItem(c.slot(), item);
                });

            // Add reset button
            int resetSlotCat = plugin.getConfig().getInt("settings.gui.layout.items.reset-button-slot-category", -1);
            if (resetSlotCat >= 0) {
                ModalItem resetItem = buildNavButton("reset-button", resetBtn.mat, Map.of());
                resetItem.setAction(event -> {
                    data.setActive(player.getUniqueId(), "none");
                    sendMessage(player, plugin.getConfig().getString("settings.messages.tag-reset", ""));
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        openCategoryGui(player);
                    });
                });
                modal.setItem(resetSlotCat, resetItem);
            }

            // Add active tag display
            int activeSlotCat = plugin.getConfig().getInt("settings.gui.layout.items.active-tag-item-slot-category", -1);
            if (activeSlotCat >= 0) {
                String activeId = data.get(player.getUniqueId()).active;
                String activeName = activeId != null && !activeId.equalsIgnoreCase("none") && !tags.byId(activeId).isEmpty() 
                    ? componentToLegacyString(tags.byId(activeId).get().name())
                    : tagCfg.getString("settings.system.empty-tag.name", "None");
                
                ModalItem activeItem = buildNavButton("active-tag", activeBtn.mat, Map.of("tag", activeName));
                modal.setItem(activeSlotCat, activeItem);
            }

            // Add close/back button
            boolean topCat = defaultView.equals("category");
            int buttonSlot = swapGlobal && topCat 
                ? plugin.getConfig().getInt("settings.gui.layout.items.close-button-slot-category", -1)
                : plugin.getConfig().getInt("settings.gui.layout.items.back-button-slot-category", -1);
                
            if (buttonSlot >= 0) {
                String buttonType = swapGlobal && topCat ? "close-button" : "back-button";
                Material buttonMat = swapGlobal && topCat ? closeBtn.mat : backBtn.mat;
                ModalItem buttonItem = buildNavButton(buttonType, buttonMat, Map.of());
                
                if (swapGlobal && topCat) {
                    buttonItem.setAction(event -> handleTopClose(player));
                }
                modal.setItem(buttonSlot, buttonItem);
            }

            modal.open(player);

            String cfgSort = plugin.getConfig().getString("settings.system.favorites-sort", "UNSORTED").toUpperCase(Locale.ROOT);
            Sort defaultSort;
            try {
                defaultSort = Sort.valueOf(cfgSort);
            } catch (IllegalArgumentException e) {
                defaultSort = Sort.UNSORTED;
            }

            open.put(player.getUniqueId(), new GuiSession(GuiType.CATEGORY, 0, "ALL", defaultSort, "ALL"));
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to open category GUI for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void openTagsGui(Player player, String categoryFilter, int page) {
        try {
            GuiSession session = open.computeIfAbsent(player.getUniqueId(), u -> {
                String cfgSort = plugin.getConfig().getString("settings.system.favorites-sort", "UNSORTED").toUpperCase(Locale.ROOT);
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

            List<TagModal.Tag> src = applyFilterAndSort(player, session);
            List<Integer> slots = cfg.guiSlots();
            int perPage = slots.size();
            int maxPage = Math.max(0, (src.size() - 1) / perPage);
            session.page = Math.min(Math.max(0, session.page), maxPage);

            int rows = plugin.getConfig().getInt("settings.gui.tags-menu.rows", 6);
            String pattern = plugin.getConfig().getString("settings.gui.layout.titles.tags-gui-name", "Tags | {category} ({currentpage}/{totalpages})");
            String filterName = catCfg.getString("settings.system.category-sort.filters." + session.filter.toLowerCase(Locale.ROOT) + ".name", session.filter);
            String titleRaw = pattern.replace("{category}", filterName)
                .replace("{currentpage}", String.valueOf(session.page + 1))
                .replace("{totalpages}", String.valueOf(maxPage + 1));

            Component title = parseText(titleRaw);

            // Use BaseModal instead of Modal
            BaseModal modal = io.rhythmknights.coreapi.component.modal.Modal.modal()
                .title(title)
                .rows(rows)
                .disableAllInteractions()
                .create();

            // Add tag items
            int base = session.page * perPage;
            for (int i = 0; i < perPage && base + i < src.size(); i++) {
                TagModal.Tag tag = src.get(base + i);
                ModalItem tagItem = buildTagItem(player, tag);
                modal.setItem(slots.get(i), tagItem);
            }

            // Add navigation buttons
            addNavigationButtons(modal, session, player);

            modal.open(player);
            open.put(player.getUniqueId(), session);

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to open tags GUI for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addNavigationButtons(BaseModal modal, GuiSession session, Player player) {
        // Add previous page button
        if (prevBtn.slot >= 0) {
            ModalItem prevItem = buildNavButton("last-page-button", prevBtn.mat, Map.of());
            prevItem.setAction(event -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    openTagsGui(player, null, session.page - 1);
                });
            });
            modal.setItem(prevBtn.slot, prevItem);
        }

        // Add next page button  
        if (nextBtn.slot >= 0) {
            ModalItem nextItem = buildNavButton("next-page-button", nextBtn.mat, Map.of());
            nextItem.setAction(event -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    openTagsGui(player, null, session.page + 1);
                });
            });
            modal.setItem(nextBtn.slot, nextItem);
        }

        // Add category sort button
        if (catBtn.slot >= 0) {
            String currentFilter = session.filter;
            Material catMaterial = categorySwitchMaterial ? getMaterialForFilter(currentFilter) : catBtn.mat;
            
            // Get filter display name from categories.yml
            String filterDisplayName = getFilterDisplayName(currentFilter);
            
            ModalItem catItem = buildNavButton("category-sort-button", catMaterial, Map.of("filter", filterDisplayName, "category", filterDisplayName));
            catItem.setAction(event -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    String nextFilter = getNextCategoryFilter(currentFilter);
                    session.filter = nextFilter;
                    openTagsGui(player, null, 0); // Reset to page 0 when changing filter
                });
            });
            modal.setItem(catBtn.slot, catItem);
        }

        // Add favorites sort button
        if (favBtn.slot >= 0) {
            String sortType = session.sort == Sort.SORTED ? "sorted" : "unsorted";
            String sortDisplayName = getSortDisplayName(sortType);
            ModalItem favItem = buildNavButton("favorite-sort-button", favBtn.mat, Map.of("sort", sortDisplayName, "sorting", sortDisplayName));
            favItem.setAction(event -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    session.sort = session.sort == Sort.SORTED ? Sort.UNSORTED : Sort.SORTED;
                    openTagsGui(player, null, -999); // Keep current page
                });
            });
            modal.setItem(favBtn.slot, favItem);
        }

        // Add color sort button
        if (colorSortBtn.slot >= 0) {
            String currentColor = session.colorFilter;
            Material colorMaterial = colorSwitchMaterial ? getMaterialForColor(currentColor) : colorSortBtn.mat;
            
            // Get color display name from tags.yml
            String colorDisplayName = getColorDisplayName(currentColor);
            
            ModalItem colorItem = buildNavButton("color-sort-button", colorMaterial, Map.of("color", colorDisplayName));
            colorItem.setAction(event -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    String nextColor = getNextColor(currentColor);
                    session.colorFilter = nextColor;
                    openTagsGui(player, null, 0); // Reset to page 0 when changing filter
                });
            });
            modal.setItem(colorSortBtn.slot, colorItem);
        }

        // Add reset button
        if (resetBtn.slot >= 0) {
            ModalItem resetItem = buildNavButton("reset-button", resetBtn.mat, Map.of());
            resetItem.setAction(event -> {
                data.setActive(player.getUniqueId(), "none");
                sendMessage(player, plugin.getConfig().getString("settings.messages.tag-reset", ""));
                Bukkit.getScheduler().runTask(plugin, () -> {
                    openTagsGui(player, null, -999); // Keep current page
                });
            });
            modal.setItem(resetBtn.slot, resetItem);
        }

        // Add back button
        if (backBtn.slot >= 0) {
            ModalItem backItem = buildNavButton("back-button", backBtn.mat, Map.of());
            backItem.setAction(event -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (defaultView.equals("category")) {
                        openCategoryGui(player);
                    } else {
                        player.closeInventory();
                    }
                });
            });
            modal.setItem(backBtn.slot, backItem);
        }

        // Add close button
        if (closeBtn.slot >= 0) {
            ModalItem closeItem = buildNavButton("close-button", closeBtn.mat, Map.of());
            closeItem.setAction(event -> handleTopClose(player));
            modal.setItem(closeBtn.slot, closeItem);
        }

        // Add active tag display
        if (activeBtn.slot >= 0) {
            String activeId = data.get(player.getUniqueId()).active;
            String activeName = activeId != null && !activeId.equalsIgnoreCase("none") && !tags.byId(activeId).isEmpty() 
                ? componentToLegacyString(tags.byId(activeId).get().name())
                : tagCfg.getString("settings.system.empty-tag.name", "None");
            
            ModalItem activeItem = buildNavButton("active-tag", activeBtn.mat, Map.of("tag", activeName));
            modal.setItem(activeBtn.slot, activeItem);
        }
    }

    private ModalItem buildTagItem(Player player, TagModal.Tag tag) {
        PlayerDataModule.PlayerData pd = data.get(player.getUniqueId());
        boolean unlocked = pd.unlocked.contains(tag.id()) || tag.cost() == 0 || !eco.active();
        
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

        // Use component directly - it's already parsed in TagModal
        Component name = tag.name();
        List<Component> lore = buildTagLore(tag, state, pd);

        ModalItem item = ItemBuilder.from(tag.icon())
            .name(name)
            .lore(lore)
            .asModalItem(event -> handleTagClick(player, tag, event.getClick()));

        return item;
    }

    private List<Component> buildTagLore(TagModal.Tag tag, ConfigModule.GameState state, PlayerDataModule.PlayerData pd) {
        String base = "settings.gui.tags.tag-items.";
        String lorePath = switch (state) {
            case ACTIVE -> base + "active-lore";
            case LOCKED -> base + "locked-lore";
            case UNLOCKED -> base + "unlocked-lore";
            default -> base + "protected-lore";
        };

        boolean fav = pd.favorites.contains(tag.id());
        String fmsg = tagCfg.getString("settings.system.favorite.msg." + (fav ? "remove" : "add"), "");
        String fstate = tagCfg.getString("settings.system.favorite.state." + (fav ? "enabled" : "disabled"), "");

        List<String> template = plugin.getConfig().getStringList(lorePath);
        List<Component> lore = new ArrayList<>();

        for (String line : template) {
            String processed = line
                .replace("{display}", componentToLegacyString(tag.display()))
                .replace("{cost}", String.valueOf(tag.cost()))
                .replace("{status}", tags.statusText(state))
                .replace("{favoritemsg}", fmsg)
                .replace("{favoritestate}", fstate);

            if (processed.contains("{description}")) {
                String[] parts = processed.split("\\{description\\}", -1);
                Component before = parseText(parts[0]);
                Component after = parts.length > 1 ? parseText(parts[1]) : Component.empty();

                for (Component desc : tag.description()) {
                    lore.add(before.append(desc).append(after));
                }
            } else {
                lore.add(parseText(processed));
            }
        }

        return lore;
    }

    private void handleTagClick(Player player, TagModal.Tag tag, ClickType click) {
        PlayerDataModule.PlayerData pd = data.get(player.getUniqueId());
        boolean unlocked = pd.unlocked.contains(tag.id()) || tag.cost() == 0 || !eco.active();

        switch (click) {
            case LEFT:
                if (unlocked) {
                    data.setActive(player.getUniqueId(), tag.id());
                    String message = plugin.getConfig().getString("settings.messages.tag-activate", "");
                    message = message.replace("{activetag}", componentToLegacyString(tag.name()))
                        .replace("{tagdisplay}", componentToLegacyString(tag.display()));
                    sendMessage(player, message);
                } else {
                    String message = plugin.getConfig().getString("settings.messages.tag-locked", "");
                    message = message.replace("{tag}", componentToLegacyString(tag.name()));
                    sendMessage(player, message);
                }
                break;
            case SHIFT_LEFT:
                if (!unlocked) {
                    attemptPurchase(player, tag);
                }
                break;
            case RIGHT:
            case MIDDLE:
                data.toggleFavorite(player.getUniqueId(), tag.id());
                if (click == ClickType.MIDDLE && unlocked) {
                    data.setActive(player.getUniqueId(), tag.id());
                }
                break;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            openTagsGui(player, null, -999);
        });
    }

    private void attemptPurchase(Player player, TagModal.Tag tag) {
        double cost = tag.cost();
        if (!eco.canAfford(player, cost)) {
            String message = plugin.getConfig().getString("settings.messages.tag-balance", 
                "Insufficient funds. You need {cost} to unlock the {tag} tag.")
                .replace("{cost}", String.valueOf(cost))
                .replace("{tag}", componentToLegacyString(tag.name()));
            sendMessage(player, message);
        } else {
            eco.withdraw(player, cost);
            data.unlockTag(player.getUniqueId(), tag.id());
            String message = plugin.getConfig().getString("settings.messages.tag-unlocked", "")
                .replace("{cost}", String.valueOf(cost))
                .replace("{tag}", componentToLegacyString(tag.name()));
            sendMessage(player, message);
        }
    }

    private ModalItem buildNavButton(String key, Material mat, Map<String, String> vars) {
        String rawTitle = "";
        
        // Handle special cases for category and favorite sort buttons
        if (key.equals("category-sort-button")) {
            rawTitle = catCfg.getString("settings.system.category-sort.sort-button.name", "&7CATEGORY &8• {filter}");
        } else if (key.equals("favorite-sort-button")) {
            rawTitle = catCfg.getString("settings.system.favorites-sort.sort-button.name", "&7FAVORITES &8• {sort}");
        } else {
            // Get title from config.yml for other buttons
            String titlePath = "settings.gui.layout.titles." + key + "-name";
            rawTitle = plugin.getConfig().getString(titlePath, "MISSING_TITLE_" + key);
        }

        // Replace all variables in the title
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            rawTitle = rawTitle.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        
        // Also handle alternate variable names
        if (key.equals("favorite-sort-button")) {
            rawTitle = rawTitle.replace("{sort-type}", vars.getOrDefault("sort", ""));
        }

        // Log for debugging
        plugin.getLogger().info("Building nav button '" + key + "' with title: '" + rawTitle + "' and material: " + mat.name());

        // Use CoreFramework TextUtility directly
        Component title = parseText(rawTitle);
        
        ItemBuilder builder = ItemBuilder.from(mat).name(title);

        // Get lore from config.yml
        String lorePath = "settings.gui.layout.lore." + key + "-lore";
        ConfigurationSection loreSec = plugin.getConfig().getConfigurationSection(lorePath);
        if (loreSec != null && loreSec.getBoolean("enabled", false)) {
            List<Component> lore = loreSec.getStringList("lore").stream()
                .map(line -> {
                    // Replace variables in lore lines too
                    String processedLine = line;
                    for (Map.Entry<String, String> entry : vars.entrySet()) {
                        processedLine = processedLine.replace("{" + entry.getKey() + "}", entry.getValue());
                    }
                    // Use CoreFramework TextUtility directly
                    return parseText(processedLine);
                })
                .collect(Collectors.toList());
            builder.lore(lore);
        }

        // Check for enchantment glint
        String matKey = key + "-material";
        boolean glint = plugin.getConfig().getBoolean("settings.gui.layout.materials." + matKey + ".enchantment-glint", false);
        if (glint) {
            builder.glow();
        }

        return builder.asModalItem();
    }

    private List<TagModal.Tag> applyFilterAndSort(Player player, GuiSession session) {
        String filter = session.filter.toUpperCase(Locale.ROOT);
        List<TagModal.Tag> src;

        switch (filter) {
            case "ALL":
                src = accessibleTags(player);
                break;
            case "FAVORITES":
                src = accessibleTags(player).stream()
                    .filter(t -> data.get(player.getUniqueId()).favorites.contains(t.id()))
                    .collect(Collectors.toList());
                break;
            case "UNLOCKED":
                src = accessibleTags(player).stream()
                    .filter(t -> data.get(player.getUniqueId()).unlocked.contains(t.id()) || t.cost() == 0 || !eco.active())
                    .collect(Collectors.toList());
                break;
            case "LOCKED":
                src = accessibleTags(player).stream()
                    .filter(t -> !data.get(player.getUniqueId()).unlocked.contains(t.id()) && t.cost() != 0 && eco.active())
                    .collect(Collectors.toList());
                break;
            case "PROTECTED":
                src = cats.all().stream()
                    .filter(CategoryModal.TagCategory::isProtected)
                    .flatMap(c -> tags.byCategory(c.key()).stream())
                    .collect(Collectors.toList());
                break;
            default:
                src = tags.byCategory(session.filter);
        }

        if (!session.colorFilter.equalsIgnoreCase("ALL")) {
            String cf = session.colorFilter.equals("GREY") ? "GRAY" : session.colorFilter;
            src = src.stream()
                .filter(tag -> tag.color().equalsIgnoreCase(cf))
                .collect(Collectors.toList());
        }

        if (session.sort == Sort.SORTED) {
            Set<String> favs = data.get(player.getUniqueId()).favorites;
            src = src.stream()
                .sorted(Comparator.comparing((TagModal.Tag t) -> !favs.contains(t.id()))
                    .thenComparing(t -> componentToLegacyString(t.name()), String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
        }

        return src;
    }

    private List<TagModal.Tag> accessibleTags(Player player) {
        return cats.all().stream()
            .filter(c -> player.hasPermission(c.permission()))
            .sorted(Comparator.comparingInt(CategoryModal.TagCategory::slot))
            .flatMap(c -> tags.byCategory(c.key()).stream())
            .collect(Collectors.toList());
    }

    private void handleTopClose(Player player) {
        if (closeCfg.enabled()) {
            if (closeCfg.closeGuiFirst()) {
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    runCloseCommands(player);
                });
            } else {
                runCloseCommands(player);
                player.closeInventory();
            }
        } else {
            player.closeInventory();
        }
    }

    private void runCloseCommands(Player player) {
        for (String cmd : closeCfg.commands()) {
            if (cmd != null && !cmd.isBlank()) {
                String command = cmd.replace("%player%", player.getName());
                if (closeCfg.runAsConsole()) {
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
                } else {
                    plugin.getServer().dispatchCommand(player, command);
                }
            }
        }
    }

    private String getFilterDisplayName(String filter) {
        // Get display name from categories.yml using the correct path structure
        String configPath = "settings.system.category-sort.filters." + filter.toLowerCase() + ".name";
        String displayName = catCfg.getString(configPath);
        if (displayName != null) {
            return displayName;
        }
        
        // If it's a category name, get from categories section
        if (!filter.equalsIgnoreCase("ALL") && !filter.equalsIgnoreCase("FAVORITES") && 
            !filter.equalsIgnoreCase("UNLOCKED") && !filter.equalsIgnoreCase("LOCKED") && 
            !filter.equalsIgnoreCase("PROTECTED")) {
            String categoryPath = "settings.categories." + filter.toLowerCase() + ".name";
            displayName = catCfg.getString(categoryPath);
            if (displayName != null) {
                return displayName;
            }
        }
        
        // Fallback to the filter name itself
        return filter;
    }
    
    private String getSortDisplayName(String sortType) {
        // Get display name from categories.yml (not tags.yml) using correct path
        String configPath = "settings.system.favorites-sort.sort-type." + sortType.toLowerCase() + ".name";
        String displayName = catCfg.getString(configPath);
        return displayName != null ? displayName : sortType;
    }
    
    private String getColorDisplayName(String color) {
        // Get display name from config.yml using the correct path
        String configPath = "settings.system.colors." + color.toLowerCase() + ".text";
        String displayName = plugin.getConfig().getString(configPath);
        return displayName != null ? displayName : color;
    }

    private String getNextCategoryFilter(String current) {
        // Get available category filters from config
        List<String> filters = new ArrayList<>();
        filters.add("ALL");
        filters.add("FAVORITES");
        filters.add("UNLOCKED");
        filters.add("LOCKED");
        filters.add("PROTECTED");
        
        // Add actual category names
        cats.all().forEach(cat -> filters.add(cat.key().toUpperCase(Locale.ROOT)));
        
        int currentIndex = filters.indexOf(current.toUpperCase(Locale.ROOT));
        if (currentIndex == -1) currentIndex = 0;
        
        int nextIndex = (currentIndex + 1) % filters.size();
        return filters.get(nextIndex);
    }

    private String getNextColor(String current) {
        int currentIndex = COLORS.indexOf(current.toUpperCase(Locale.ROOT));
        if (currentIndex == -1) currentIndex = 0;
        
        int nextIndex = (currentIndex + 1) % COLORS.size();
        return COLORS.get(nextIndex);
    }

    private Material getMaterialForFilter(String filter) {
        // First try to get material from category-sort-button-switch-material section
        String switchMaterialPath = "settings.gui.layout.materials.category-sort-button-switch-material.material." + filter.toLowerCase() + ".material";
        String materialName = plugin.getConfig().getString(switchMaterialPath);
        
        plugin.getLogger().info("Looking for filter switch material at path: " + switchMaterialPath + " = " + materialName);
        
        if (materialName != null) {
            try {
                Material result = Material.valueOf(materialName.toUpperCase(Locale.ROOT));
                plugin.getLogger().info("Found filter switch material: " + result.name() + " for filter: " + filter);
                return result;
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid switch material '" + materialName + "' for filter '" + filter + "'");
            }
        }
        
        // If it's a category, try to get material from category definition
        if (!filter.equalsIgnoreCase("ALL") && !filter.equalsIgnoreCase("FAVORITES") && 
            !filter.equalsIgnoreCase("UNLOCKED") && !filter.equalsIgnoreCase("LOCKED") && 
            !filter.equalsIgnoreCase("PROTECTED")) {
            String categoryPath = "settings.categories." + filter.toLowerCase() + ".material";
            materialName = catCfg.getString(categoryPath);
            plugin.getLogger().info("Looking for category material at path: " + categoryPath + " = " + materialName);
            if (materialName != null) {
                try {
                    Material result = Material.valueOf(materialName.toUpperCase(Locale.ROOT));
                    plugin.getLogger().info("Found category material: " + result.name() + " for filter: " + filter);
                    return result;
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material '" + materialName + "' for category '" + filter + "'");
                }
            }
        }
        
        plugin.getLogger().info("Using fallback material: " + catBtn.mat.name() + " for filter: " + filter);
        return catBtn.mat; // Fallback to default material
    }

    private Material getMaterialForColor(String color) {
        // Get material from config.yml using the correct path for color switch materials
        String switchMaterialPath = "settings.gui.layout.materials.color-sort-button-switch-material.material." + color.toLowerCase() + ".material";
        String materialName = plugin.getConfig().getString(switchMaterialPath);
        
        plugin.getLogger().info("Looking for color switch material at path: " + switchMaterialPath + " = " + materialName);
        
        if (materialName != null) {
            try {
                Material result = Material.valueOf(materialName.toUpperCase(Locale.ROOT));
                plugin.getLogger().info("Found color switch material: " + result.name() + " for color: " + color);
                return result;
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid switch material '" + materialName + "' for color '" + color + "'");
            }
        }
        
        plugin.getLogger().info("Using fallback material: " + colorSortBtn.mat.name() + " for color: " + color);
        return colorSortBtn.mat; // Fallback to default material
    }

    private String componentToLegacyString(Component component) {
        try {
            // Use LegacyComponentSerializer to convert back to legacy format
            return LegacyComponentSerializer.legacyAmpersand().serialize(component);
        } catch (Exception e) {
            return component.toString();
        }
    }

    private Component parseText(String text) {
        try {
            // Use CoreFramework TextUtility for text parsing
            Class<?> textUtilityClass = Class.forName("io.rhythmknights.coreframework.component.utility.TextUtility");
            Method parseMethod = textUtilityClass.getMethod("parse", String.class);
            return (Component) parseMethod.invoke(null, text);
        } catch (Exception e) {
            // Fallback to basic Component creation
            return Component.text(text);
        }
    }

    private void sendMessage(Player player, String message) {
        try {
            // Use CoreFramework TextUtility for message sending
            Class<?> textUtilityClass = Class.forName("io.rhythmknights.coreframework.component.utility.TextUtility");
            Method sendPlayerMessage = textUtilityClass.getMethod("sendPlayerMessage", Player.class, String.class);
            sendPlayerMessage.invoke(null, player, message);
        } catch (Exception e) {
            // Fallback to basic message sending
            player.sendMessage(message);
        }
    }

    private Material mat(String key) {
        String base = "settings.gui.layout.materials.";
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection(base + key);
        if (sec == null) {
            plugin.getLogger().warning("Missing config for '" + base + key + "'");
            return Material.STONE;
        }

        String raw = sec.getString("material");
        try {
            return Material.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid material '" + raw + "' at '" + base + key + "'");
            return Material.STONE;
        }
    }

    private int slot(String path) {
        return plugin.getConfig().getInt(path, 0);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        open.remove(event.getPlayer().getUniqueId());
    }

    // Helper classes
    private static record Btn(Material mat, int slot) {}

    private static enum Sort {
        UNSORTED, SORTED
    }

    private static final class GuiSession {
        GuiType type;
        int page;
        String filter;
        Sort sort;
        String colorFilter;

        GuiSession(GuiType t, int p, String f, Sort s, String c) {
            this.type = t;
            this.page = p;
            this.filter = f;
            this.sort = s;
            this.colorFilter = c;
        }
    }

    private static enum GuiType {
        CATEGORY, TAGS
    }
}