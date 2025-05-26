package io.rhythmknights.coretags;

import io.rhythmknights.coreapi.CoreAPI;
import io.rhythmknights.coreframework.CoreFramework;
import io.rhythmknights.coreframework.component.api.FrameworkAPI;
import io.rhythmknights.coreframework.component.api.hook.HookRequirement;
import io.rhythmknights.coreframework.component.api.plugin.RegisteredPlugin;
import io.rhythmknights.coretags.component.command.CommandModule;
import io.rhythmknights.coretags.component.data.ConfigModule;
import io.rhythmknights.coretags.component.data.PlayerDataModule;
import io.rhythmknights.coretags.component.hook.LuckPermsHook;
import io.rhythmknights.coretags.component.hook.PlaceholderHook;
import io.rhythmknights.coretags.component.hook.VaultHook;
import io.rhythmknights.coretags.component.modal.CategoryModal;
import io.rhythmknights.coretags.component.modal.ModalProcessor;
import io.rhythmknights.coretags.component.modal.TagModal;

import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPerms;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CoreTags extends JavaPlugin {
    private static CoreTags INSTANCE;
    
    // Framework integration
    private RegisteredPlugin registeredPlugin;
    
    // Core components
    private LuckPermsHook luckPermsHook;
    private VaultHook vaultHook;
    private ConfigModule configModule;
    private CategoryModal categoryModal;
    private TagModal tagModal;
    private PlayerDataModule playerDataModule;
    private ModalProcessor modalProcessor;
    private CommandModule commandModule;
    private PlaceholderHook placeholderHook;

    @Override
    public void onLoad() {
        INSTANCE = this;
        
        // Register with CoreFramework
        try {
            FrameworkAPI api = CoreFramework.getAPI();
            if (api != null) {
                List<HookRequirement> hooks = List.of(
                    HookRequirement.optional("LuckPerms", "any"),
                    HookRequirement.optional("Vault", "any"),
                    HookRequirement.optional("PlaceholderAPI", "any")
                );
                
                registeredPlugin = api.registerPlugin(this, getDescription().getVersion(), "HORIZON", hooks);
                getLogger().info("Successfully registered with CoreFramework");
            } else {
                getLogger().warning("CoreFramework API not available - using fallback mode");
            }
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Failed to register with CoreFramework: " + e.getMessage(), e);
        }
        
        // Initialize CoreAPI - Note: CoreAPI.init() should exist in the CoreAPI class
        // If this method doesn't exist, you may need to create it or remove this line
        try {
            CoreAPI.init(this);
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Failed to initialize CoreAPI: " + e.getMessage(), e);
        }
    }

    @Override
    public void onEnable() {
        info("Enabling CoreTags " + getDescription().getVersion());
        
        // Initialize core modules
        this.configModule = new ConfigModule(this);
        this.luckPermsHook = new LuckPermsHook(this, getLuckPermsApi());
        this.vaultHook = new VaultHook(this, getVaultEconomy());
        this.categoryModal = new CategoryModal(this);
        this.tagModal = new TagModal(this);
        this.playerDataModule = new PlayerDataModule(this);
        this.modalProcessor = new ModalProcessor(this);
        this.commandModule = new CommandModule(this);
        
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            this.placeholderHook = new PlaceholderHook(this);
        }

        this.reloadEverything();
        info("CoreTags enabled successfully.");
        
        if (Bukkit.getPluginManager().isPluginEnabled("Nexo")) {
            this.integrateNexo();
        }
    }

    @Override
    public void onDisable() {
        info("CoreTags disabled.");
    }

    private void integrateNexo() {
        // Use CoreFramework TextUtility for console messages
        try {
            Class<?> textUtilityClass = Class.forName("io.rhythmknights.coreframework.component.utility.TextUtility");
            java.lang.reflect.Method sendConsoleMessage = textUtilityClass.getMethod("sendConsoleMessage", String.class);
            
            sendConsoleMessage.invoke(null, "{prefix} <aqua>Nex</aqua><green>o</green> <gray>detected.</gray>");
            sendConsoleMessage.invoke(null, "{prefix} <aqua>Nex</aqua><green>o</green> <gray>files installed</gray> <dark_gray>|</dark_gray>");
            
            Plugin nexo = Bukkit.getPluginManager().getPlugin("Nexo");
            File nexoData = nexo.getDataFolder();
            
            String[] paths = new String[]{
                "items/oraxen_items/coretags.yml",
                "pack/assets/minecraft/models/coretags/favoritetag.json", 
                "pack/assets/minecraft/textures/coretags/favoritetag.png"
            };
            
            for (String rel : paths) {
                File dest = new File(nexoData, rel);
                if (copyResource("nexo/" + rel, dest)) {
                    sendConsoleMessage.invoke(null, "  <aqua>+</aqua> <gold>" + rel + "</gold>");
                }
            }
            
            sendConsoleMessage.invoke(null, "{prefix} <aqua>Nex</aqua><green>o</green> <gray>configuration successfully installed.</gray>");
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Failed to use CoreFramework TextUtility, falling back to direct logging", e);
            getLogger().info("Nexo detected and files installed");
        }
    }

    private boolean copyResource(String resourcePath, File target) {
        try (InputStream in = getResource(resourcePath)) {
            if (in == null) {
                getLogger().warning("Bundled resource not found: " + resourcePath);
                return false;
            }

            Files.createDirectories(target.toPath().getParent());
            Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            getLogger().warning("Failed to install " + target.getName() + ": " + e.getMessage());
            return false;
        }
    }

    public void reloadEverything() {
        info("Reloading CoreTags…");
        reloadConfig();
        configModule.reload();
        configModule.forceReload();
        categoryModal.reload();
        tagModal.reload();
        modalProcessor.reloadFileConfigs();
        playerDataModule.reload();
        modalProcessor.refreshAll();
        
        if (placeholderHook != null) {
            placeholderHook.refreshAll();
        }

        info("Reload complete.");
    }

    public void sendReloadMessage(CommandSender sender) {
        try {
            // Use CoreFramework TextUtility for message sending
            Class<?> textUtilityClass = Class.forName("io.rhythmknights.coreframework.component.utility.TextUtility");
            java.lang.reflect.Method sendMessage = textUtilityClass.getMethod("sendMessage", CommandSender.class, String.class);
            
            FileConfiguration cfg = getConfig();
            boolean usePrefix = cfg.getBoolean("settings.messages.enable-prefix", false);
            String prefix = usePrefix ? cfg.getString("settings.messages.prefix", "") : "";
            String raw = cfg.getString("settings.messages.msg-reload", "<green>CoreTags configuration reloaded!</green>");
            
            sendMessage.invoke(null, sender, prefix + raw);
        } catch (Exception e) {
            // Fallback to legacy method
            FileConfiguration cfg = getConfig();
            boolean usePrefix = cfg.getBoolean("settings.messages.enable-prefix", false);
            String prefix = usePrefix ? cfg.getString("settings.messages.prefix", "") : "";
            String raw = cfg.getString("settings.messages.msg-reload", "CoreTags configuration reloaded!");
            sender.sendMessage(prefix + raw);
        }
    }

    // Getters for components
    public static CoreTags getInstance() {
        return INSTANCE;
    }

    public LuckPermsHook luckPerms() {
        return luckPermsHook;
    }

    public VaultHook economy() {
        return vaultHook;
    }

    public ConfigModule configs() {
        return configModule;
    }

    public CategoryModal categories() {
        return categoryModal;
    }

    public TagModal tags() {
        return tagModal;
    }

    public PlayerDataModule playerData() {
        return playerDataModule;
    }

    public ModalProcessor modalProcessor() {
        return modalProcessor;
    }

    // Helper methods
    private LuckPerms getLuckPermsApi() {
        RegisteredServiceProvider<LuckPerms> rsp = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        return rsp == null ? null : rsp.getProvider();
    }

    private Economy getVaultEconomy() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        return rsp == null ? null : rsp.getProvider();
    }

    private void info(String msg) {
        getLogger().log(Level.INFO, msg);
    }
}