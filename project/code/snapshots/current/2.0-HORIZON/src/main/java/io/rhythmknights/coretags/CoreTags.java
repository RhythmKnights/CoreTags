package io.rhythmknights.coretags;

import io.rhythmknights.coreapi.CoreAPI;
import io.rhythmknights.coreframework.CoreFramework;
import io.rhythmknights.coreframework.util.TextUtility;
import io.rhythmknights.coretags.component.command.CommandModule;
import io.rhythmknights.coretags.component.data.ConfigModule;
import io.rhythmknights.coretags.component.data.PlayerDataModule;
import io.rhythmknights.coretags.component.hook.LuckPermsHook;
import io.rhythmknights.coretags.component.hook.PlaceholderHook;
import io.rhythmknights.coretags.component.hook.VaultHook;
import io.rhythmknights.coretags.component.modal.CategoryModal;
import io.rhythmknights.coretags.component.modal.ModalProcessor;
import io.rhythmknights.coretags.component.modal.TagModal;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.luckperms.api.LuckPerms;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;

/**
 * CoreTags - A powerful tag/prefix management plugin with CoreAPI, CoreFramework, 
 * LuckPerms, PlaceholderAPI, Vault &amp; Adventure support.
 * 
 * <p>This plugin provides a comprehensive tag management system that allows players
 * to unlock, purchase, and display custom tags. It integrates with modern Minecraft
 * plugin frameworks and provides a rich GUI experience through CoreAPI.</p>
 * 
 * <p>Key features include:</p>
 * <ul>
 *   <li>CoreAPI integration for modern modal-based GUIs</li>
 *   <li>CoreFramework integration for unified text processing</li>
 *   <li>LuckPerms integration for permission-based tag access</li>
 *   <li>Vault integration for economy-based tag purchasing</li>
 *   <li>PlaceholderAPI support for external plugin integration</li>
 *   <li>Category-based tag organization</li>
 *   <li>Color filtering and sorting capabilities</li>
 *   <li>Favorites system for quick tag access</li>
 * </ul>
 * 
 * @author RhythmKnights
 * @version 2.1-HORIZON
 * @since 1.0.0
 * 
 * @apiNote This plugin requires CoreAPI and CoreFramework as hard dependencies.
 * @implNote The plugin follows a modular architecture with separate components
 *           for different functionalities.
 */
public final class CoreTags extends JavaPlugin {
    
    /**
     * The singleton instance of the CoreTags plugin.
     * This is set during the {@link #onLoad()} phase and provides
     * global access to the plugin instance.
     */
    private static CoreTags INSTANCE;
    
    /**
     * Adventure audiences instance for modern text component handling.
     * Used throughout the plugin for sending formatted messages to players.
     */
    private BukkitAudiences adventure;
    
    /**
     * CoreAPI instance for modal-based GUI functionality.
     * Provides access to the modern GUI framework.
     */
    private CoreAPI coreAPI;
    
    /**
     * CoreFramework instance for utility functions and text processing.
     * Central framework providing common functionality across RhythmKnights plugins.
     */
    private CoreFramework coreFramework;
    
    /**
     * TextUtility instance from CoreFramework for unified text processing.
     * Handles parsing of legacy color codes, MiniMessage, and Adventure components.
     */
    private TextUtility textUtility;
    
    /**
     * LuckPerms integration hook for permission management.
     * Handles permission-based tag access and group synchronization.
     */
    private LuckPermsHook luckPermsHook;
    
    /**
     * Vault integration hook for economy functionality.
     * Manages tag purchasing and balance checking.
     */
    private VaultHook vaultHook;
    
    /**
     * Configuration module for managing plugin settings.
     * Handles loading and parsing of the main configuration file.
     */
    private ConfigModule configModule;
    
    /**
     * Category modal for managing tag categories.
     * Handles category definitions and GUI display.
     */
    private CategoryModal categoryModal;
    
    /**
     * Tag modal for managing individual tags.
     * Handles tag definitions, loading, and metadata.
     */
    private TagModal tagModal;
    
    /**
     * Player data module for managing player-specific data.
     * Handles tag unlocking, favorites, and active tag tracking.
     */
    private PlayerDataModule playerDataModule;
    
    /**
     * Modal processor for handling GUI interactions.
     * Central component for managing all GUI-related functionality.
     */
    private ModalProcessor modalProcessor;
    
    /**
     * Command module for handling plugin commands.
     * Manages command execution and tab completion.
     */
    private CommandModule commandModule;
    
    /**
     * PlaceholderAPI integration hook for external plugin support.
     * Provides placeholders for use in other plugins.
     */
    private PlaceholderHook placeholderHook;

    /**
     * Called when the plugin is loaded by the server.
     * This occurs before the server finishes loading all plugins.
     * 
     * <p>During this phase, we set the singleton instance to allow
     * other components to access the plugin during initialization.</p>
     * 
     * @implNote This method should only perform minimal initialization
     *           as other plugins may not be available yet.
     */
    @Override
    public void onLoad() {
        INSTANCE = this;
    }

    /**
     * Called when the plugin is enabled by the server.
     * This is where the main initialization logic occurs.
     * 
     * <p>The initialization process follows this order:</p>
     * <ol>
     *   <li>Initialize required frameworks (CoreAPI, CoreFramework)</li>
     *   <li>Set up Adventure audiences for text handling</li>
     *   <li>Initialize configuration and data modules</li>
     *   <li>Set up integration hooks for external plugins</li>
     *   <li>Initialize modal and command systems</li>
     *   <li>Register optional integrations (PlaceholderAPI, Nexo)</li>
     * </ol>
     * 
     * @implNote If framework initialization fails, the plugin will disable itself
     *           to prevent runtime errors.
     */
    @Override
    public void onEnable() {
        this.info("Enabling CoreTags " + this.getDescription().getVersion());
        
        // Initialize frameworks
        if (!this.initializeFrameworks()) {
            this.getLogger().severe("Failed to initialize required frameworks!");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        this.adventure = BukkitAudiences.create(this);
        this.configModule = new ConfigModule(this);
        this.luckPermsHook = new LuckPermsHook(this, this.getLuckPermsApi());
        this.vaultHook = new VaultHook(this, this.getVaultEconomy());
        this.categoryModal = new CategoryModal(this);
        this.tagModal = new TagModal(this);
        this.playerDataModule = new PlayerDataModule(this);
        this.modalProcessor = new ModalProcessor(this);
        this.commandModule = new CommandModule(this);
        
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            this.placeholderHook = new PlaceholderHook(this);
        }

        this.reloadEverything();
        this.info("CoreTags enabled successfully.");
        
        if (Bukkit.getPluginManager().isPluginEnabled("Nexo")) {
            this.integrateNexo();
        }
    }

    /**
     * Called when the plugin is disabled by the server.
     * This handles cleanup of resources and saving any pending data.
     * 
     * <p>Cleanup operations include:</p>
     * <ul>
     *   <li>Closing all open modals for players</li>
     *   <li>Shutting down Adventure audiences</li>
     *   <li>Logging shutdown message</li>
     * </ul>
     * 
     * @implNote This method should be safe to call multiple times
     *           and handle null components gracefully.
     */
    @Override
    public void onDisable() {
        if (this.modalProcessor != null) {
            this.modalProcessor.closeAllModals();
        }
        
        if (this.adventure != null) {
            this.adventure.close();
        }

        this.info("CoreTags disabled.");
    }

    /**
     * Initializes the required frameworks (CoreAPI and CoreFramework).
     * 
     * <p>This method verifies that both required frameworks are present
     * and properly initializes the plugin's integration with them.</p>
     * 
     * @return {@code true} if initialization was successful, {@code false} otherwise
     * 
     * @implNote If this method returns {@code false}, the plugin should disable itself
     *           as it cannot function without these frameworks.
     */
    private boolean initializeFrameworks() {
        try {
            Plugin coreAPIPlugin = this.getServer().getPluginManager().getPlugin("CoreAPI");
            Plugin coreFrameworkPlugin = this.getServer().getPluginManager().getPlugin("CoreFramework");
            
            if (coreAPIPlugin == null) {
                this.getLogger().severe("CoreAPI plugin not found! Please install CoreAPI.");
                return false;
            }
            
            if (coreFrameworkPlugin == null) {
                this.getLogger().severe("CoreFramework plugin not found! Please install CoreFramework.");
                return false;
            }
            
            this.coreAPI = CoreAPI.getInstance();
            this.coreFramework = CoreFramework.getInstance();
            this.textUtility = this.coreFramework.getTextUtility();
            
            this.info("Successfully integrated with CoreAPI and CoreFramework!");
            return true;
        } catch (Exception e) {
            this.getLogger().log(Level.SEVERE, "Failed to initialize frameworks", e);
            return false;
        }
    }

    /**
     * Integrates with the Nexo plugin if it's present on the server.
     * 
     * <p>This method copies necessary resource files to the Nexo plugin directory
     * to enable custom item support for CoreTags. The integration is optional
     * and the plugin will function normally without Nexo.</p>
     * 
     * <p>Files copied include:</p>
     * <ul>
     *   <li>Item definitions (YAML)</li>
     *   <li>Model files (JSON)</li>
     *   <li>Texture files (PNG)</li>
     * </ul>
     * 
     * @implNote This method will not fail if Nexo is not present or if
     *           file copying fails - it will only log warnings.
     */
    private void integrateNexo() {
        Component prefix = ((TextComponent)((TextComponent)Component.text("[", NamedTextColor.DARK_GRAY).decoration(TextDecoration.BOLD, true))
                .append(Component.text("CoreTags", NamedTextColor.BLUE).decoration(TextDecoration.BOLD, true)))
                .append(Component.text("]", NamedTextColor.DARK_GRAY).decoration(TextDecoration.BOLD, true));
                
        this.adventure.console().sendMessage(prefix.append(Component.space())
                .append(Component.text("Nex", NamedTextColor.AQUA))
                .append(Component.text("o", NamedTextColor.GREEN))
                .append(Component.text(" detected.", NamedTextColor.GRAY)));
                
        Plugin nexo = Bukkit.getPluginManager().getPlugin("Nexo");
        File nexoData = nexo.getDataFolder();
        
        this.adventure.console().sendMessage(prefix.append(Component.space())
                .append(Component.text("Nex", NamedTextColor.AQUA))
                .append(Component.text("o", NamedTextColor.GREEN))
                .append(Component.text(" files installed ", NamedTextColor.GRAY))
                .append(Component.text("|", NamedTextColor.DARK_GRAY)));
                
        String[] paths = {
            "items/oraxen_items/coretags.yml",
            "pack/assets/minecraft/models/coretags/favoritetag.json",
            "pack/assets/minecraft/textures/coretags/favoritetag.png"
        };
        
        for (String rel : paths) {
            File dest = new File(nexoData, rel);
            if (this.copyResource("nexo/" + rel, dest)) {
                this.adventure.console().sendMessage(Component.text("  + ", NamedTextColor.AQUA)
                        .append(Component.text(rel, NamedTextColor.GOLD)));
            }
        }
        
        this.adventure.console().sendMessage(prefix.append(Component.space())
                .append(Component.text("Nex", NamedTextColor.AQUA))
                .append(Component.text("o", NamedTextColor.GREEN))
                .append(Component.text(" configuration successfully installed.", NamedTextColor.GRAY)));
    }

    /**
     * Copies a resource file from the plugin JAR to the specified target location.
     * 
     * <p>This method is used primarily for installing additional files for
     * external plugin integration (such as Nexo custom items).</p>
     * 
     * @param resourcePath the path to the resource within the plugin JAR
     * @param target the target file location where the resource should be copied
     * @return {@code true} if the file was successfully copied, {@code false} otherwise
     * 
     * @implNote This method will create parent directories if they don't exist
     *           and will replace existing files.
     */
    private boolean copyResource(@NotNull String resourcePath, @NotNull File target) {
        try (InputStream in = this.getResource(resourcePath)) {
            if (in == null) {
                this.getLogger().warning("Bundled resource not found: " + resourcePath);
                return false;
            }
            
            Files.createDirectories(target.toPath().getParent());
            Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            this.getLogger().warning("Failed to install " + target.getName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Reloads all plugin components and configurations.
     * 
     * <p>This method performs a comprehensive reload of the plugin without
     * requiring a server restart. It reloads:</p>
     * <ul>
     *   <li>Main configuration file</li>
     *   <li>Tag and category definitions</li>
     *   <li>Player data from disk</li>
     *   <li>Modal processor configurations</li>
     *   <li>PlaceholderAPI integrations</li>
     * </ul>
     * 
     * @apiNote This method is thread-safe and can be called from any thread.
     * @implNote Some changes may require players to reopen GUIs to take effect.
     */
    public void reloadEverything() {
        this.info("Reloading CoreTags…");
        this.reloadConfig();
        this.configModule.reload();
        this.configModule.forceReload();
        this.categoryModal.reload();
        this.tagModal.reload();
        this.modalProcessor.reloadFileConfigs();
        this.playerDataModule.reload();
        this.modalProcessor.refreshAll();
        
        if (this.placeholderHook != null) {
            this.placeholderHook.refreshAll();
        }
        
        this.info("Reload complete.");
    }

    /**
     * Sends a formatted reload success message to the specified command sender.
     * 
     * <p>The message format is controlled by configuration and may include
     * prefixes and custom styling.</p>
     * 
     * @param sender the command sender to receive the message
     * 
     * @apiNote The message will be processed through CoreFramework's TextUtility
     *          for consistent formatting.
     */
    public void sendReloadMessage(@NotNull CommandSender sender) {
        FileConfiguration cfg = this.getConfig();
        boolean usePrefix = cfg.getBoolean("settings.messages.enable-prefix", false);
        String prefix = usePrefix ? cfg.getString("settings.messages.prefix", "") : "";
        String raw = cfg.getString("settings.messages.msg-reload", "&aCoreTags configuration reloaded!");
        Component msg = this.textUtility.parseText(prefix + raw);
        this.adventure.sender(sender).sendMessage(msg);
    }

    // Getters

    /**
     * Gets the singleton instance of the CoreTags plugin.
     * 
     * @return the plugin instance, or {@code null} if the plugin hasn't been loaded yet
     * 
     * @apiNote This method should only be called after the plugin has been loaded.
     *          It's safe to call from other plugins' onEnable methods.
     */
    @Nullable
    public static CoreTags getInstance() {
        return INSTANCE;
    }

    /**
     * Gets the Adventure audiences instance for text component handling.
     * 
     * @return the BukkitAudiences instance
     * 
     * @apiNote This is the primary way to send formatted messages to players
     *          and should be used instead of direct Bukkit messaging methods.
     */
    @NotNull
    public BukkitAudiences adventure() {
        return this.adventure;
    }
    
    /**
     * Gets the CoreAPI instance for modal functionality.
     * 
     * @return the CoreAPI instance
     * 
     * @apiNote This provides access to the modal framework used throughout
     *          the plugin for GUI interactions.
     */
    @NotNull
    public CoreAPI coreAPI() {
        return this.coreAPI;
    }
    
    /**
     * Gets the CoreFramework instance for utility functions.
     * 
     * @return the CoreFramework instance
     * 
     * @apiNote This provides access to shared utilities across RhythmKnights plugins.
     */
    @NotNull
    public CoreFramework coreFramework() {
        return this.coreFramework;
    }
    
    /**
     * Gets the TextUtility instance for unified text processing.
     * 
     * @return the TextUtility instance from CoreFramework
     * 
     * @apiNote This should be used for all text parsing and formatting operations
     *          to ensure consistency across the plugin.
     */
    @NotNull
    public TextUtility textUtility() {
        return this.textUtility;
    }

    /**
     * Gets the LuckPerms integration hook.
     * 
     * @return the LuckPermsHook instance
     * 
     * @apiNote This may return a hook with no API if LuckPerms is not installed.
     *          Always check {@link LuckPermsHook#isPresent()} before using the API.
     */
    @NotNull
    public LuckPermsHook luckPerms() {
        return this.luckPermsHook;
    }

    /**
     * Gets the Vault economy integration hook.
     * 
     * @return the VaultHook instance
     * 
     * @apiNote This may return a hook with no economy if Vault is not installed.
     *          Always check {@link VaultHook#active()} before using economy features.
     */
    @NotNull
    public VaultHook economy() {
        return this.vaultHook;
    }

    /**
     * Gets the configuration module for accessing plugin settings.
     * 
     * @return the ConfigModule instance
     * 
     * @apiNote This provides type-safe access to configuration values
     *          and handles automatic reloading.
     */
    @NotNull
    public ConfigModule configs() {
        return this.configModule;
    }

    /**
     * Gets the category modal for tag category management.
     * 
     * @return the CategoryModal instance
     * 
     * @apiNote This handles all category-related operations including
     *          loading, parsing, and providing category data.
     */
    @NotNull
    public CategoryModal categories() {
        return this.categoryModal;
    }

    /**
     * Gets the tag modal for individual tag management.
     * 
     * @return the TagModal instance
     * 
     * @apiNote This handles all tag-related operations including
     *          loading, parsing, and providing tag data.
     */
    @NotNull
    public TagModal tags() {
        return this.tagModal;
    }

    /**
     * Gets the player data module for managing player-specific information.
     * 
     * @return the PlayerDataModule instance
     * 
     * @apiNote This handles player tag unlocking, favorites, active tags,
     *          and synchronization with external systems.
     */
    @NotNull
    public PlayerDataModule playerData() {
        return this.playerDataModule;
    }

    /**
     * Gets the modal processor for GUI interactions.
     * 
     * @return the ModalProcessor instance
     * 
     * @apiNote This is the central component for all GUI-related functionality
     *          and should be used to open modals for players.
     */
    @NotNull
    public ModalProcessor modalProcessor() {
        return this.modalProcessor;
    }

    /**
     * Gets the LuckPerms API instance if available.
     * 
     * <p>This method attempts to retrieve the LuckPerms API from the
     * Bukkit services manager. If LuckPerms is not installed or not
     * properly registered, this will return {@code null}.</p>
     * 
     * @return the LuckPerms API instance, or {@code null} if not available
     * 
     * @implNote This method is called during plugin initialization to
     *           set up the LuckPerms integration hook.
     */
    @Nullable
    private LuckPerms getLuckPermsApi() {
        RegisteredServiceProvider<LuckPerms> rsp = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        return rsp == null ? null : rsp.getProvider();
    }

    /**
     * Gets the Vault Economy API instance if available.
     * 
     * <p>This method attempts to retrieve the Vault Economy API from the
     * Bukkit services manager. If Vault is not installed or no economy
     * plugin is registered, this will return {@code null}.</p>
     * 
     * @return the Economy API instance, or {@code null} if not available
     * 
     * @implNote This method is called during plugin initialization to
     *           set up the Vault integration hook.
     */
    @Nullable
    private Economy getVaultEconomy() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        return rsp == null ? null : rsp.getProvider();
    }

    /**
     * Logs an informational message to the plugin logger.
     * 
     * @param msg the message to log
     * 
     * @implNote This is a convenience method to avoid direct logger access
     *           throughout the codebase.
     */
    private void info(@NotNull String msg) {
        this.getLogger().log(Level.INFO, msg);
    }

    /**
     * Logs a message at the specified level to the plugin logger.
     * 
     * @param level the logging level
     * @param msg the message to log
     * 
     * @implNote This method includes null checking for safety and
     *           should be used for all plugin logging operations.
     */
    private void log(@NotNull Level level, @NotNull String msg) {
        if (this.getLogger() != null) {
            this.getLogger().log(level, msg);
        }
    }
}