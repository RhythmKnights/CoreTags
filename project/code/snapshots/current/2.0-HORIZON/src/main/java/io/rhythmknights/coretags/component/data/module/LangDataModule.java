package io.rhythmknights.coretags.component.data.module;

import io.rhythmknights.coretags.CoreTags;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Level;

/**
 * Data module for handling language configuration (global.yml and custom language files)
 * Manages all text, labels, messages, and language-specific content
 */
public class LangDataModule {
    
    private final CoreTags plugin;
    private YamlConfiguration config;
    private File configFile;
    private String currentLanguage;
    
    /**
     * Constructor for LangDataModule
     * 
     * @param plugin The CoreTags plugin instance
     */
    public LangDataModule(CoreTags plugin) {
        this.plugin = plugin;
        this.currentLanguage = "GLOBAL"; // Default language
    }
    
    /**
     * Load the language configuration
     * 
     * @return true if loaded successfully
     */
    public boolean load() {
        try {
            // Determine which language file to load
            determineLanguageFile();
            
            if (!configFile.exists()) {
                plugin.getLogger().warning("Language config file not found: " + configFile.getName() + ", creating defaults");
                createDefaultConfiguration();
            }
            
            config = YamlConfiguration.loadConfiguration(configFile);
            
            // Validate and set defaults for missing keys
            validateAndSetDefaults();
            
            plugin.getLogger().info("Language configuration loaded successfully: " + configFile.getName());
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load language configuration!", e);
            createDefaultConfiguration();
            return false;
        }
    }
    
    /**
     * Reload the language configuration
     * 
     * @return true if reloaded successfully
     */
    public boolean reload() {
        return load();
    }
    
    /**
     * Get the configuration
     * 
     * @return The language configuration
     */
    public YamlConfiguration getConfig() {
        return config;
    }
    
    /**
     * Set the language and reload configuration
     * 
     * @param language The language code (e.g., "GLOBAL", "EN-US", "ES-ES")
     * @return true if language was set successfully
     */
    public boolean setLanguage(String language) {
        this.currentLanguage = language.toUpperCase();
        return load();
    }
    
    /**
     * Get the current language
     * 
     * @return The current language code
     */
    public String getCurrentLanguage() {
        return currentLanguage;
    }
    
    /**
     * Determine which language file to use
     */
    private void determineLanguageFile() {
        // Check if a specific language is configured in system settings
        // For now, default to global.yml - this can be enhanced later
        
        if ("GLOBAL".equalsIgnoreCase(currentLanguage)) {
            configFile = new File(plugin.getDataFolder(), "languages/global.yml");
        } else {
            // Custom language file (e.g., EN-US.yml, ES-ES.yml)
            configFile = new File(plugin.getDataFolder(), "languages/" + currentLanguage.toLowerCase() + ".yml");
            
            // Fallback to global.yml if custom language file doesn't exist
            if (!configFile.exists()) {
                plugin.getLogger().warning("Custom language file not found: " + configFile.getName() + ", falling back to global.yml");
                configFile = new File(plugin.getDataFolder(), "languages/global.yml");
                currentLanguage = "GLOBAL";
            }
        }
    }
    
    /**
     * Create default language configuration
     */
    private void createDefaultConfiguration() {
        config = new YamlConfiguration();
        
        // System settings
        config.set("settings.system.hooks.luckperms.name", "<green>LuckPerms</green>");
        config.set("settings.system.hooks.playerdata.name", "<gold>playerdata</gold>");
        config.set("settings.system.hooks.playerdata-path.name", "<gold>/playerdata/</gold>");
        
        // UI conditions/labels
        config.set("settings.system.conditions.confirm", "<green>CONFIRM</green>");
        config.set("settings.system.conditions.cancel", "<red>CANCEL</red>");
        config.set("settings.system.conditions.expired", "<dark_grey>EXPIRED</dark_grey>");
        config.set("settings.system.conditions.reset", "<red>RESET</red>");
        config.set("settings.system.conditions.active", "<gold>ACTIVE</gold>");
        config.set("settings.system.conditions.unlock", "<green>UNLOCK</green>");
        config.set("settings.system.conditions.unlocked", "<green>UNLOCKED</green>");
        config.set("settings.system.conditions.lock", "<red>LOCK</red>");
        config.set("settings.system.conditions.locked", "<red>LOCKED</red>");
        config.set("settings.system.conditions.protected", "<dark_purple>PROTECTED</dark_purple>");
        config.set("settings.system.conditions.favorite", "<aqua>FAVORITE</aqua>");
        config.set("settings.system.conditions.favorited", "<aqua>FAVORITED</aqua>");
        config.set("settings.system.conditions.free", "<green>FREE</green>");
        config.set("settings.system.conditions.sorted", "<grey>SORTED</grey>");
        config.set("settings.system.conditions.unsorted", "<grey>UNSORTED</grey>");
        
        // Color settings
        createColorSettings();
        
        // Control settings
        createControlSettings();
        
        // GUI settings
        createGuiSettings();
        
        // Messages
        createMessages();
        
        // Save the default configuration
        try {
            // Ensure parent directory exists
            if (configFile.getParentFile() != null) {
                configFile.getParentFile().mkdirs();
            }
            
            config.save(configFile);
            plugin.getLogger().info("Created default language configuration: " + configFile.getName());
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save default language configuration!", e);
        }
    }
    
    /**
     * Create color settings
     */
    private void createColorSettings() {
        config.set("settings.system.colors.all.text", "<gold>ALL</gold>");
        config.set("settings.system.colors.multi.text", "<blue>M</blue><green>U</green><yellow>L</yellow><red>T</red><light_purple>I</light_purple>");
        config.set("settings.system.colors.red.text", "<red>RED</red>");
        config.set("settings.system.colors.orange.text", "<#e06101>ORANGE</#e06101>");
        config.set("settings.system.colors.yellow.text", "<yellow>YELLOW</yellow>");
        config.set("settings.system.colors.green.text", "<green>GREEN</green>");
        config.set("settings.system.colors.blue.text", "<blue>BLUE</blue>");
        config.set("settings.system.colors.purple.text", "<dark_purple>PURPLE</dark_purple>");
        config.set("settings.system.colors.pink.text", "<#d5658f>PINK</#d5658f>");
        config.set("settings.system.colors.brown.text", "<#603c20>BROWN</#603c20>");
        config.set("settings.system.colors.grey.text", "<grey>GREY</grey>");
        config.set("settings.system.colors.black.text", "<dark_grey>BLACK</dark_grey>");
        config.set("settings.system.colors.white.text", "<white>WHITE</white>");
    }
    
    /**
     * Create control settings
     */
    private void createControlSettings() {
        // Chat controls
        config.set("settings.system.controls.chat.leftclick", "<white>⸶</white> <#2ECC71>ʟᴇꜰᴛ ᴄʟɪᴄᴋ</#2ECC71>");
        config.set("settings.system.controls.chat.rightclick", "<white>⸷</white> <#2ECC71>ʀɪɢʜᴛ ᴄʟɪᴄᴋ</#2ECC71>");
        config.set("settings.system.controls.chat.shiftleftclick", "<#2ECC71>ꜱʜɪꜰᴛ</#2ECC71> <grey>+</grey> <white>⸶</white> <#2ECC71>ʟᴇꜰᴛ ᴄʟɪᴄᴋ</#2ECC71>");
        config.set("settings.system.controls.chat.shiftrightclick", "<#2ECC71>ꜱʜɪꜰᴛ</#2ECC71> <grey>+</grey> <white>⸷</white> <#2ECC71>ʀɪɢʜᴛ ᴄʟɪᴄᴋ</#2ECC71>");
        
        // GUI controls
        config.set("settings.system.controls.gui.leftclick", "<grey>⏵</grey> <white>⸶</white> <#2ECC71>ʟᴇꜰᴛ ᴄʟɪᴄᴋ</#2ECC71> • {setactive}");
        config.set("settings.system.controls.gui.rightclick", "<grey>⏵</grey> <white>⸷</white> <#2ECC71>ʀɪɢʜᴛ ᴄʟɪᴄᴋ</#2ECC71> • {togglefavorite}");
        config.set("settings.system.controls.gui.shiftleftclick", "<grey>⏵</grey> <#2ECC71>ꜱʜɪꜰᴛ</#2ECC71> <grey>+</grey> <white>⸶</white> <#2ECC71>ʟᴇꜰᴛ ᴄʟɪᴄᴋ</#2ECC71> • {unlocktag}");
        config.set("settings.system.controls.gui.shiftrightclick", "<grey>⏵</grey> <#2ECC71>ꜱʜɪꜰᴛ</#2ECC71> <grey>+</grey> <white>⸷</white> <#2ECC71>ʀɪɢʜᴛ ᴄʟɪᴄᴋ</#2ECC71> • {activefavorite}");
    }
    
    /**
     * Create GUI settings including enhanced sort button configurations
     */
    private void createGuiSettings() {
        // GUI titles
        config.set("settings.system.gui.titles.category-menu", "<gold>Tags</gold> <dark_grey>|</dark_grey> <gold>Categories</gold>");
        config.set("settings.system.gui.titles.tags-menu", "<gold>Tags</gold> <dark_grey>|</dark_grey> {category} <dark_grey>•</dark_grey> <grey>({currentpage}/{totalpages})</grey>");
        
        // Enhanced GUI titles for sort buttons
        config.set("settings.system.gui.titles.category-sort-button", "<grey>CATEGORY</grey> <dark_grey>•</dark_grey> {category}");
        config.set("settings.system.gui.titles.color-sort-button", "<grey>COLOR</grey> <dark_grey>•</dark_grey> {color}");
        config.set("settings.system.gui.titles.favorite-sort-button", "<grey>FAVORITES</grey>");
        
        config.set("settings.system.gui.titles.last-page-button", "<grey>Previous Page</grey>");
        config.set("settings.system.gui.titles.next-page-button", "<grey>Next Page</grey>");
        config.set("settings.system.gui.titles.reset-button", "<red>Reset Tag</red>");
        config.set("settings.system.gui.titles.active-tag", "<gold>Active Tag</gold> <dark_grey>•</dark_grey> {activetag}");
        config.set("settings.system.gui.titles.back-button", "<red>Back</red>");
        config.set("settings.system.gui.titles.close-button", "<red>Exit</red>");
        config.set("settings.system.gui.titles.empty-slot", "");
        config.set("settings.system.gui.titles.favorited-icon", "<white>✭</white>");
        
        // GUI labels
        config.set("settings.system.gui.labels.setactive", "<gold>[Set as active tag]</gold>");
        config.set("settings.system.gui.labels.togglefavorite.add", "<gold>[Set favorite]</gold>");
        config.set("settings.system.gui.labels.togglefavorite.remove", "<gold>[Remove favorite]</gold>");
        config.set("settings.system.gui.labels.unlocktag", "<gold>[Unlock tag]</gold>");
        config.set("settings.system.gui.labels.activefavorite", "<gold>[Set as active tag + favorite]</gold>");
        
        // GUI lore templates
        createGuiLoreTemplates();
    }
    
    /**
     * Create GUI lore templates including enhanced sort button lore
     */
    private void createGuiLoreTemplates() {
        // Active tag lore
        config.set("settings.system.gui.lore.active", Arrays.asList(
            "<dark_grey>•</dark_grey><st>                                                        </st><dark_grey>•</dark_grey>",
            "",
            "<grey>Icon</grey> <dark_grey>•</dark_grey> {tag.display}",
            "",
            "<grey>Description</grey> <dark_grey>•</dark_grey>",
            "  {tag.lore}",
            "",
            "{gui.rightclick}",
            "",
            "<grey>Status</grey> <dark_grey>•</dark_grey> {tag.status}",
            "<dark_grey>•</dark_grey><st>                                                        </st><dark_grey>•</dark_grey>"
        ));
        
        // Unlocked tag lore
        config.set("settings.system.gui.lore.unlocked", Arrays.asList(
            "<dark_grey>•</dark_grey><st>                                                        </st><dark_grey>•</dark_grey>",
            "",
            "<grey>Icon</grey> <dark_grey>•</dark_grey> {tag.display}",
            "",
            "<grey>Description</grey> <dark_grey>•</dark_grey>",
            "  {tag.lore}",
            "",
            "{gui.leftclick}",
            "{gui.rightclick}",
            "{gui.shiftrightclick}",
            "",
            "<grey>Status</grey> <dark_grey>•</dark_grey> {tag.status}",
            "<dark_grey>•</dark_grey><st>                                                        </st><dark_grey>•</dark_grey>"
        ));
        
        // Locked tag lore
        config.set("settings.system.gui.lore.locked", Arrays.asList(
            "<dark_grey>•</dark_grey><st>                                                        </st><dark_grey>•</dark_grey>",
            "",
            "<grey>Icon</grey> <dark_grey>•</dark_grey> {tag.display}",
            "",
            "<grey>Description</grey> <dark_grey>•</dark_grey>",
            "  {tag.lore}",
            "",
            "{gui.shiftleftclick}",
            "{gui.rightclick}",
            "",
            "<grey>Status</grey> <dark_grey>•</dark_grey> {tag.status}",
            "<dark_grey>•</dark_grey><st>                                                        </st><dark_grey>•</dark_grey>"
        ));
        
        // Protected tag lore
        config.set("settings.system.gui.lore.protected", Arrays.asList(
            "<dark_grey>•</dark_grey><st>                                                        </st><dark_grey>•</dark_grey>",
            "",
            "<grey>Icon</grey> <dark_grey>•</dark_grey> {tag.display}",
            "",
            "<grey>Description</grey> <dark_grey>•</dark_grey>",
            "  {tag.lore}",
            "",
            "<red>(</red>{protected} <red>tags cannot be purchased.</red>",
            "",
            "{gui.rightclick}",
            "",
            "<grey>Status</grey> <dark_grey>•</dark_grey> {tag.status}",
            "<dark_grey>•</dark_grey><st>                                                        </st><dark_grey>•</dark_grey>"
        ));
        
        // Button lore templates including enhanced sort button lore
        config.set("settings.system.gui.lore.empty-slot", Arrays.asList(""));
        
        config.set("settings.system.gui.lore.category-sort-button", Arrays.asList(
            "{chat.leftclick} <gold>[Next Category]</gold>",
            "{chat.rightclick} <gold>[Previous Category]</gold>",
            "{chat.shiftleftclick} <gold>[First Category]</gold>",
            "{chat.shiftrightclick} <gold>[Last Category]</gold>"
        ));
        
        config.set("settings.system.gui.lore.color-sort-button", Arrays.asList(
            "{chat.leftclick} <gold>[Next Color]</gold>",
            "{chat.rightclick} <gold>[Previous Color]</gold>",
            "{chat.shiftleftclick} <gold>[First Color]</gold>",
            "{chat.shiftrightclick} <gold>[Last Color]</gold>"
        ));
        
        config.set("settings.system.gui.lore.favorite-sort-button", Arrays.asList(
            "{chat.leftclick} <gold>[Toggle Favorites First]</gold>"
        ));
        
        config.set("settings.system.gui.lore.last-page-button", Arrays.asList(""));
        config.set("settings.system.gui.lore.next-page-button", Arrays.asList(""));
        config.set("settings.system.gui.lore.reset-button", Arrays.asList(
            "{chat.leftclick} <gold>[Reset Tag to Default]</gold>"
        ));
        config.set("settings.system.gui.lore.active-tag", Arrays.asList(""));
        config.set("settings.system.gui.lore.back-button", Arrays.asList(""));
    }
    
    /**
     * Create messages including enhanced sort and tag interaction messages
     */
    private void createMessages() {
        // Modal messages
        config.set("messages.modal.closed", "<grey>Menu closed.</grey>");
        config.set("messages.modal.category-closed", "<grey>Category menu closed.</grey>");
        config.set("messages.modal.tags-closed", "<grey>Tags menu closed.</grey>");
        config.set("messages.modal.closed-by-button", "<grey>Menu closed.</grey>");
        config.set("messages.modal.category-selected", "<grey>Selected category:</grey> <gold>{category}</gold>");
        config.set("messages.modal.sort-changed", "<yellow>Changed {sorttype} to {value}.</yellow>");
        config.set("messages.modal.no-previous-page", "<red>You are already on the first page.</red>");
        config.set("messages.modal.no-next-page", "<red>You are already on the last page.</red>");
        
        // Tag interaction messages
        config.set("messages.tag.activated", "<green>Set active tag to:</green> <gold>{tag}</gold>");
        config.set("messages.tag.favorite-toggled", "<yellow>Toggled favorite status for:</yellow> <gold>{tag}</gold>");
        config.set("messages.tag.unlocked", "<green>Unlocked tag:</green> <gold>{tag}</gold>");
        config.set("messages.tag.active-favorite", "<green>Set as active and favorite:</green> <gold>{tag}</gold>");
        config.set("messages.tag.reset", "<grey>Tag reset to default.</grey>");
        config.set("messages.tag.active-info", "<yellow>Your active tag:</yellow> <gold>{tag}</gold>");
        
        // Command messages
        config.set("settings.system.commands.reload.complete", "<green>Configuration reloaded successfully.</green>");
        config.set("settings.system.commands.reload.failed", "<red>Failed to reload configuration. Check console for errors.</red>");
        
        // Sync messages
        createSyncMessages();
        
        // Error messages
        config.set("settings.system.error.no-permission", "<red>Action canceled. Administrator access is required to perform this action.</red>");
        config.set("settings.system.error.player-only", "<red>Action canceled. Current action cannot be performed from console.</red>");
        config.set("settings.system.error.invalid-args", "<red>Invalid arguments.</red> <grey>Usage</grey> <dark_grey>•</dark_grey> <grey>{usage}</grey>");
        config.set("settings.system.error.player-not-found", "<red>Player</red> <gold>{player}</gold> <red>not found.</red>");
        config.set("settings.system.error.no-player-data", "<red>Player data is missing or corrupted.</red>");
        config.set("settings.system.error.sync-failed", "<red>Failed to sync player data for</red> <grey>{uuid}</grey><red>. Check console for errors.</red>");
        config.set("settings.system.error.file-corrupted", "<red>Player data for</red> <grey>{uuid}</grey> <red>is corrupted. {error}</red>");
        config.set("settings.system.error.modal-open-failed", "<red>Failed to open menu. Please try again or contact an administrator.</red>");
        config.set("settings.system.error.unknown-command", "<red>Unknown command:</red> <yellow>{command}</yellow><red>. Use</red> <yellow>/coretags help</yellow> <red>for available commands.</red>");
        
        // Tag messages
        config.set("settings.system.tags.tag-reset", "<gold>Active tag</gold> <grey>has been</grey> {reset} <grey>to default.</grey>");
        config.set("settings.system.tags.tag-activate", "{activetag} <grey>set to</grey> {active}<grey>.</grey>");
        config.set("settings.system.tags.tag-balance", "<red>Transaction failed.</red> <grey>Minimum balance of</grey> {cost} <grey>needed to</grey> {unlock} <grey>the</grey> {tag} <grey>tag.</grey>");
        config.set("settings.system.tags.tag-unlocked", "{tag} <grey>has been</grey> {unlocked}<grey>.</grey>");
        config.set("settings.system.tags.tag-locked", "{tag} <grey>is</grey> {locked} <grey>. To</grey> {unlock} <grey>the</grey> {tag} <grey>tag, hold</grey> {chat.shiftleftclick} <grey>the tag.</grey>");
        config.set("settings.system.tags.tag-authorize", "{tag} <grey>has been</grey> {unlocked} <grey>for</grey> <gold>{player}</gold><grey>.</grey>");
        config.set("settings.system.tags.tag-revoke", "{tag} <grey>has been</grey> {locked} <grey>for</grey> <gold>{player}</gold><grey>.</grey>");
    }
    
    /**
     * Create sync messages
     */
    private void createSyncMessages() {
        // Sync timer text formats
        config.set("settings.system.commands.sync.sync-timer.text.active", "<red>{countdown}</red><grey>s</grey>");
        config.set("settings.system.commands.sync.sync-timer.text.expired", "{expired}");
        
        // Sync confirmation messages
        config.set("settings.system.commands.sync.sync-pd-confirm", Arrays.asList(
            "<red>WARNING!</red> <grey>Syncing the data manually will overwrite the data stored in</grey> {luckperms} <grey>with the data stored within the json/yaml files in</grey> {playerdata-path}<grey>.</grey>",
            "<red>These actions <bold>cannot be undone</bold>.</red>",
            "<grey>To</grey> <green>proceed</green> <grey>click</grey> {confirm} <grey>within</grey> {countdown}<grey>.</grey>",
            "<grey>To</grey> <red>cancel</red> <grey>click</grey> {cancel} <grey> or wait for the timer to expire.</grey>"
        ));
        
        config.set("settings.system.commands.sync.sync-lp-confirm", Arrays.asList(
            "<red>WARNING!</red> <grey>Syncing the data manually will overwrite the data stored within the json/yaml files in</grey> {playerdata-path} <grey>with the data stored in</grey> {luckperms}<grey>.</grey>",
            "<red>These actions <bold>cannot be undone</bold>.</red>",
            "<grey>To</grey> <green>proceed</green> <grey>click</grey> {confirm} <grey>within</grey> {countdown}<grey>.</grey>",
            "<grey>To</grey> <red>cancel</red> <grey>click</grey> {cancel} <grey> or wait for the timer to expire.</grey>"
        ));
        
        // Sync status messages
        config.set("settings.system.commands.sync.sync-complete", "<grey>Successfully synchronized playerdata from</grey> {data.src} <grey>to</grey> {data.destination}<grey>.</grey>");
        config.set("settings.system.commands.sync.sync-all-complete", Arrays.asList(
            "<grey>Successfully synchronized</grey> <gold>{count}</gold> <grey>player data entries from</grey> {data.src} <grey>to</grey> {data.destination}<grey>.</grey>"
        ));
        config.set("settings.system.commands.sync.sync-canceled", Arrays.asList(
            "<grey>Sync action(s) canceled.</grey> <red>No changes were synced.</red> <grey>To try again, run the</grey> <dark_grey>'</dark_grey><gold>/coretags admin sync <destination> <target></gold><dark_grey>'</dark_grey> <grey>command again and click</grey> {confirm}<grey>.</grey>"
        ));
    }
    
    /**
     * Validate configuration and set defaults for missing keys
     */
    private void validateAndSetDefaults() {
        boolean changed = false;
        
        // Check essential system settings
        if (!config.contains("settings.system.conditions.unlocked")) {
            config.set("settings.system.conditions.unlocked", "<green>UNLOCKED</green>");
            changed = true;
        }
        
        if (!config.contains("settings.system.conditions.locked")) {
            config.set("settings.system.conditions.locked", "<red>LOCKED</red>");
            changed = true;
        }
        
        if (!config.contains("settings.system.conditions.protected")) {
            config.set("settings.system.conditions.protected", "<dark_purple>PROTECTED</dark_purple>");
            changed = true;
        }
        
        if (!config.contains("settings.system.conditions.favorite")) {
            config.set("settings.system.conditions.favorite", "<aqua>FAVORITE</aqua>");
            changed = true;
        }
        
        if (!config.contains("settings.system.conditions.active")) {
            config.set("settings.system.conditions.active", "<gold>ACTIVE</gold>");
            changed = true;
        }
        
        // Check color settings
        if (!config.contains("settings.system.colors.all.text")) {
            config.set("settings.system.colors.all.text", "<gold>ALL</gold>");
            changed = true;
        }
        
        if (!config.contains("settings.system.colors.multi.text")) {
            config.set("settings.system.colors.multi.text", "<blue>M</blue><green>U</green><yellow>L</yellow><red>T</red><light_purple>I</light_purple>");
            changed = true;
        }
        
        // Check GUI titles
        if (!config.contains("settings.system.gui.titles.category-menu")) {
            config.set("settings.system.gui.titles.category-menu", "<gold>Tags</gold> <dark_grey>|</dark_grey> <gold>Categories</gold>");
            changed = true;
        }
        
        if (!config.contains("settings.system.gui.titles.tags-menu")) {
            config.set("settings.system.gui.titles.tags-menu", "<gold>Tags</gold> <dark_grey>|</dark_grey> {category} <dark_grey>•</dark_grey> <grey>({currentpage}/{totalpages})</grey>");
            changed = true;
        }
        
        if (!config.contains("settings.system.gui.titles.close-button")) {
            config.set("settings.system.gui.titles.close-button", "<red>Exit</red>");
            changed = true;
        }
        
        if (!config.contains("settings.system.gui.titles.back-button")) {
            config.set("settings.system.gui.titles.back-button", "<red>Back</red>");
            changed = true;
        }
        
        // Check enhanced GUI titles for sort buttons
        if (!config.contains("settings.system.gui.titles.category-sort-button")) {
            config.set("settings.system.gui.titles.category-sort-button", "<grey>CATEGORY</grey> <dark_grey>•</dark_grey> {category}");
            changed = true;
        }
        
        if (!config.contains("settings.system.gui.titles.color-sort-button")) {
            config.set("settings.system.gui.titles.color-sort-button", "<grey>COLOR</grey> <dark_grey>•</dark_grey> {color}");
            changed = true;
        }
        
        if (!config.contains("settings.system.gui.titles.favorite-sort-button")) {
            config.set("settings.system.gui.titles.favorite-sort-button", "<grey>FAVORITES</grey>");
            changed = true;
        }
        
        // Check GUI labels
        if (!config.contains("settings.system.gui.labels.setactive")) {
            config.set("settings.system.gui.labels.setactive", "<gold>[Set as active tag]</gold>");
            changed = true;
        }
        
        if (!config.contains("settings.system.gui.labels.togglefavorite.add")) {
            config.set("settings.system.gui.labels.togglefavorite.add", "<gold>[Set favorite]</gold>");
            changed = true;
        }
        
        if (!config.contains("settings.system.gui.labels.unlocktag")) {
            config.set("settings.system.gui.labels.unlocktag", "<gold>[Unlock tag]</gold>");
            changed = true;
        }
        
        // Check controls
        if (!config.contains("settings.system.controls.gui.leftclick")) {
            config.set("settings.system.controls.gui.leftclick", "<grey>⏵</grey> <white>⸶</white> <#2ECC71>ʟᴇꜰᴛ ᴄʟɪᴄᴋ</#2ECC71> • {setactive}");
            changed = true;
        }
        
        if (!config.contains("settings.system.controls.gui.rightclick")) {
            config.set("settings.system.controls.gui.rightclick", "<grey>⏵</grey> <white>⸷</white> <#2ECC71>ʀɪɢʜᴛ ᴄʟɪᴄᴋ</#2ECC71> • {togglefavorite}");
            changed = true;
        }
        
        // Check sort button lore
        if (!config.contains("settings.system.gui.lore.category-sort-button")) {
            config.set("settings.system.gui.lore.category-sort-button", Arrays.asList(
                "{chat.leftclick} <gold>[Next Category]</gold>",
                "{chat.rightclick} <gold>[Previous Category]</gold>",
                "{chat.shiftleftclick} <gold>[First Category]</gold>",
                "{chat.shiftrightclick} <gold>[Last Category]</gold>"
            ));
            changed = true;
        }
        
        if (!config.contains("settings.system.gui.lore.color-sort-button")) {
            config.set("settings.system.gui.lore.color-sort-button", Arrays.asList(
                "{chat.leftclick} <gold>[Next Color]</gold>",
                "{chat.rightclick} <gold>[Previous Color]</gold>",
                "{chat.shiftleftclick} <gold>[First Color]</gold>",
                "{chat.shiftrightclick} <gold>[Last Color]</gold>"
            ));
            changed = true;
        }
        
        if (!config.contains("settings.system.gui.lore.favorite-sort-button")) {
            config.set("settings.system.gui.lore.favorite-sort-button", Arrays.asList(
                "{chat.leftclick} <gold>[Toggle Favorites First]</gold>"
            ));
            changed = true;
        }
        
        // Check messages
        if (!config.contains("messages.modal.closed")) {
            config.set("messages.modal.closed", "<grey>Menu closed.</grey>");
            changed = true;
        }
        
        if (!config.contains("messages.modal.category-closed")) {
            config.set("messages.modal.category-closed", "<grey>Category menu closed.</grey>");
            changed = true;
        }
        
        if (!config.contains("messages.modal.tags-closed")) {
            config.set("messages.modal.tags-closed", "<grey>Tags menu closed.</grey>");
            changed = true;
        }
        
        if (!config.contains("settings.system.error.modal-open-failed")) {
            config.set("settings.system.error.modal-open-failed", "<red>Failed to open menu. Please try again or contact an administrator.</red>");
            changed = true;
        }
        
        // Check sort messages
        if (!config.contains("messages.modal.sort-changed")) {
            config.set("messages.modal.sort-changed", "<yellow>Changed {sorttype} to {value}.</yellow>");
            changed = true;
        }
        
        // Check tag interaction messages
        if (!config.contains("messages.tag.activated")) {
            config.set("messages.tag.activated", "<green>Set active tag to:</green> <gold>{tag}</gold>");
            changed = true;
        }
        
        if (!config.contains("messages.tag.favorite-toggled")) {
            config.set("messages.tag.favorite-toggled", "<yellow>Toggled favorite status for:</yellow> <gold>{tag}</gold>");
            changed = true;
        }
        
        if (!config.contains("messages.tag.unlocked")) {
            config.set("messages.tag.unlocked", "<green>Unlocked tag:</green> <gold>{tag}</gold>");
            changed = true;
        }
        
        if (!config.contains("messages.tag.active-favorite")) {
            config.set("messages.tag.active-favorite", "<green>Set as active and favorite:</green> <gold>{tag}</gold>");
            changed = true;
        }
        
        if (!config.contains("messages.tag.reset")) {
            config.set("messages.tag.reset", "<grey>Tag reset to default.</grey>");
            changed = true;
        }
        
        if (!config.contains("messages.tag.active-info")) {
            config.set("messages.tag.active-info", "<yellow>Your active tag:</yellow> <gold>{tag}</gold>");
            changed = true;
        }
        
        // Save changes if any were made
        if (changed) {
            try {
                config.save(configFile);
                plugin.getLogger().info("Updated language configuration with missing defaults");
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to save updated language configuration", e);
            }
        }
    }
    
    // Convenience methods for commonly accessed language settings
    
    /**
     * Get a condition/status label
     * 
     * @param condition The condition name (unlocked, locked, protected, etc.)
     * @return The formatted label
     */
    public String getConditionLabel(String condition) {
        return config.getString("settings.system.conditions." + condition, condition.toUpperCase());
    }
    
    /**
     * Get a color label
     * 
     * @param color The color name
     * @return The formatted color label
     */
    public String getColorLabel(String color) {
        return config.getString("settings.system.colors." + color + ".text", color.toUpperCase());
    }
    
    /**
     * Get a GUI title
     * 
     * @param titleKey The title key (category-menu, tags-menu, etc.)
     * @return The title text
     */
    public String getGuiTitle(String titleKey) {
        return config.getString("settings.system.gui.titles." + titleKey, titleKey);
    }
    
    /**
     * Get a GUI label
     * 
     * @param labelKey The label key
     * @return The label text
     */
    public String getGuiLabel(String labelKey) {
        return config.getString("settings.system.gui.labels." + labelKey, labelKey);
    }
    
    /**
     * Get a control text
     * 
     * @param controlKey The control key (gui.leftclick, chat.rightclick, etc.)
     * @return The control text
     */
    public String getControlText(String controlKey) {
        return config.getString("settings.system.controls." + controlKey, controlKey);
    }
    
    /**
     * Get a message
     * 
     * @param messageKey The message key
     * @return The message text
     */
    public String getMessage(String messageKey) {
        return config.getString("messages." + messageKey, messageKey);
    }
    
    /**
     * Get a system message
     * 
     * @param messageKey The message key
     * @return The message text
     */
    public String getSystemMessage(String messageKey) {
        return config.getString("settings.system." + messageKey, messageKey);
    }
    
    /**
     * Get GUI lore template
     * 
     * @param loreKey The lore key (active, unlocked, locked, etc.)
     * @return The lore lines
     */
    public java.util.List<String> getGuiLore(String loreKey) {
        return config.getStringList("settings.system.gui.lore." + loreKey);
    }
    
    /**
     * Check if a configuration key exists
     * 
     * @param key The configuration key
     * @return true if the key exists
     */
    public boolean hasKey(String key) {
        return config.contains(key);
    }
    
    /**
     * Get a raw configuration value
     * 
     * @param key The configuration key
     * @param defaultValue The default value if key doesn't exist
     * @return The configuration value
     */
    public String getString(String key, String defaultValue) {
        return config.getString(key, defaultValue);
    }
    
    /**
     * Get a list configuration value
     * 
     * @param key The configuration key
     * @return The configuration list
     */
    public java.util.List<String> getStringList(String key) {
        return config.getStringList(key);
    }
}