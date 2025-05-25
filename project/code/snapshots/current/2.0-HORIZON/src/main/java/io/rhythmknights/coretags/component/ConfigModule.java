package io.rhythmknights.coretags.component.data;

import io.rhythmknights.coreframework.util.TextUtility;
import io.rhythmknights.coretags.CoreTags;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class ConfigModule {
    private final CoreTags plugin;
    private final TextUtility textUtility;
    private FileConfiguration cfg;
    private boolean convertCostFree;
    private String freeCostText;
    private boolean costSystem;
    private List<Integer> guiSlots = List.of();
    private String defaultView;
    private boolean closeButtonSwap;
    private CloseCmd closeCmd;
    private boolean usePrefix;
    private String messagePrefix;
    private String reloadSuccessMessage;
    private String reloadFailedMessage;

    public ConfigModule(CoreTags plugin) {
        this.plugin = plugin;
        this.textUtility = plugin.textUtility();
        this.saveDefault();
        this.reload();
    }

    public void reload() {
        try {
            File file = new File(this.plugin.getDataFolder(), "config.yml");
            this.cfg = new YamlConfiguration();
            this.cfg.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            this.plugin.getLogger().severe("Could not load config.yml: " + e.getMessage());
            return;
        }

        this.parseMessageSettings();
        this.parseCostSettings();
        this.parseGuiSlots();
        this.parseSystemBlock();
        this.plugin.getLogger().info("Config loaded. (" + this.guiSlots.size() + " GUI slots)");
    }

    private void parseMessageSettings() {
        ConfigurationSection msg = this.cfg.getConfigurationSection("settings.messages");
        if (msg == null) {
            msg = this.cfg.createSection("settings.messages");
        }

        this.usePrefix = msg.getBoolean("enable-prefix", true);
        this.messagePrefix = this.parseColorCodes(msg.getString("prefix", "&8[CoreTags]&r "));
        this.reloadSuccessMessage = this.parseColorCodes(msg.getString("reload-success", "&aConfiguration reloaded!"));
        this.reloadFailedMessage = this.parseColorCodes(msg.getString("reload-failed", "&cReload failed!"));
    }

    private void parseCostSettings() {
        this.costSystem = this.cfg.getBoolean("cost-system", true);
        ConfigurationSection msg = this.cfg.getConfigurationSection("settings.messages");
        if (msg == null) {
            msg = this.cfg.createSection("settings.messages");
        }

        this.convertCostFree = msg.getBoolean("convert-cost.enabled", true);
        this.freeCostText = this.parseColorCodes(msg.getString("convert-cost.free", "&aFREE"));
    }

    private void parseGuiSlots() {
        this.guiSlots = new ArrayList<>();
        ConfigurationSection sec = this.cfg.getConfigurationSection("settings.gui.tags.slots");
        if (sec == null) {
            this.plugin.getLogger().warning("gui-slots section missing!");
            return;
        }

        sec.getKeys(false).stream().sorted().forEach(row -> {
            String definition = sec.getString(row, "");
            if (definition != null && !definition.isBlank()) {
                this.parseSlotDefinition(definition);
            }
        });
    }

    private void parseSlotDefinition(String definition) {
        String[] tokens = definition.split(",");
        for (String token : tokens) {
            token = token.trim();
            if (token.isEmpty()) continue;

            if (token.contains("..")) {
                this.parseSlotRange(token);
            } else {
                this.parseSingleSlot(token);
            }
        }
    }

    private void parseSlotRange(String token) {
        String[] parts = token.split("\\.\\.");
        try {
            int from = Integer.parseInt(parts[0]);
            int to = Integer.parseInt(parts[1]);
            for (int i = from; i <= to; i++) {
                this.addSlot(i);
            }
        } catch (NumberFormatException ignored) {
            // Invalid range format, skip
        }
    }

    private void parseSingleSlot(String token) {
        try {
            this.addSlot(Integer.parseInt(token));
        } catch (NumberFormatException ignored) {
            // Invalid slot number, skip
        }
    }

    private void addSlot(int slot) {
        if (slot >= 0 && slot <= 53 && !this.guiSlots.contains(slot)) {
            this.guiSlots.add(slot);
        }
    }

    private void parseSystemBlock() {
        ConfigurationSection sys = this.cfg.getConfigurationSection("settings.system");
        if (sys == null) {
            sys = this.cfg.createSection("settings.system");
        }

        this.defaultView = sys.getString("default-view", "category").toLowerCase(Locale.ROOT);
        this.closeButtonSwap = sys.getBoolean("close-button-swap", true);
        
        ConfigurationSection cbc = sys.getConfigurationSection("close-button-cmd");
        if (cbc == null) {
            cbc = sys.createSection("close-button-cmd");
        }

        boolean enabled = cbc.getBoolean("enabled", false);
        boolean closeFirst = cbc.getBoolean("close-gui", false);
        List<String> cmds = cbc.getStringList("command");
        if (cmds == null) {
            cmds = List.of();
        }
        boolean console = "console".equalsIgnoreCase(cbc.getString("command-type", "player"));
        this.closeCmd = new CloseCmd(enabled, closeFirst, cmds, console);
    }

    private String parseColorCodes(String text) {
        if (text == null) return "";
        // Use TextUtility for parsing if available, otherwise fallback to legacy method
        try {
            return this.textUtility.stripColors(this.textUtility.parseText(text).toString());
        } catch (Exception e) {
            // Fallback to legacy color code parsing
            return text.replace('&', '§');
        }
    }

    // Getters
    public boolean convertCostFree() {
        return this.convertCostFree;
    }

    public String freeCostText() {
        return this.freeCostText;
    }

    public boolean costSystemEnabled() {
        return this.costSystem;
    }

    public List<Integer> guiSlots() {
        return Collections.unmodifiableList(this.guiSlots);
    }

    public String defaultView() {
        return this.defaultView;
    }

    public boolean closeButtonSwap() {
        return this.closeButtonSwap;
    }

    public CloseCmd closeCmd() {
        return this.closeCmd;
    }

    public boolean usePrefix() {
        return this.usePrefix;
    }

    public String messagePrefix() {
        return this.messagePrefix;
    }

    public String reloadSuccessMessage() {
        return this.reloadSuccessMessage;
    }

    public String reloadFailedMessage() {
        return this.reloadFailedMessage;
    }

    public FileConfiguration raw() {
        return this.cfg;
    }

    private void saveDefault() {
        File file = new File(this.plugin.getDataFolder(), "config.yml");
        if (!file.exists()) {
            this.plugin.saveResource("config.yml", false);
        }
    }

    public void forceReload() {
        try {
            File file = new File(this.plugin.getDataFolder(), "config.yml");
            this.cfg = new YamlConfiguration();
            this.cfg.load(file);
            this.parseMessageSettings();
            this.parseCostSettings();
            this.parseGuiSlots();
            this.parseSystemBlock();
            this.plugin.getLogger().info("Config reloaded from disk. (" + this.guiSlots.size() + " GUI slots)");
        } catch (IOException | InvalidConfigurationException e) {
            this.plugin.getLogger().severe("Could not reload config.yml: " + e.getMessage());
        }
    }

    // Record classes
    public record CloseCmd(boolean enabled, boolean closeGuiFirst, List<String> commands, boolean runAsConsole) {
    }

    public enum GameState {
        ACTIVE,
        UNLOCKED,
        LOCKED,
        PROTECTED
    }
}