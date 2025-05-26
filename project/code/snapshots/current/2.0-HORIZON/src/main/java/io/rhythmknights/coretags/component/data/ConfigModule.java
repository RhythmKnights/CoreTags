package io.rhythmknights.coretags.component.data;

import io.rhythmknights.coretags.CoreTags;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ConfigModule {
   private final CoreTags plugin;
   private FileConfiguration cfg;
   private boolean convertCostFree;
   private String freeCostText;
   private boolean costSystem;
   private List<Integer> guiSlots = List.of();
   private String defaultView;
   private boolean closeButtonSwap;
   private ConfigModule.CloseCmd closeCmd;
   private boolean usePrefix;
   private String messagePrefix;
   private String reloadSuccessMessage;
   private String reloadFailedMessage;

   public ConfigModule(CoreTags plugin) {
      this.plugin = plugin;
      this.saveDefault();
      this.reload();
   }

   public void reload() {
      try {
         File file = new File(this.plugin.getDataFolder(), "config.yml");
         this.cfg = new YamlConfiguration();
         this.cfg.load(file);
      } catch (IOException | InvalidConfigurationException var2) {
         this.plugin.getLogger().severe("Could not load config.yml: " + var2.getMessage());
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
      this.messagePrefix = ChatColor.translateAlternateColorCodes('&', msg.getString("prefix", "&8[CoreTags]&r "));
      this.reloadSuccessMessage = ChatColor.translateAlternateColorCodes('&', msg.getString("reload-success", "&aConfiguration reloaded!"));
      this.reloadFailedMessage = ChatColor.translateAlternateColorCodes('&', msg.getString("reload-failed", "&cReload failed!"));
   }

   private void parseCostSettings() {
      this.costSystem = this.cfg.getBoolean("cost-system", true);
      ConfigurationSection msg = this.cfg.getConfigurationSection("settings.messages");
      if (msg == null) {
         msg = this.cfg.createSection("settings.messages");
      }

      this.convertCostFree = msg.getBoolean("convert-cost.enabled", true);
      this.freeCostText = ChatColor.translateAlternateColorCodes('&', msg.getString("convert-cost.free", "&aFREE"));
   }

   private void parseGuiSlots() {
      this.guiSlots = new ArrayList();
      ConfigurationSection sec = this.cfg.getConfigurationSection("settings.gui.tags.slots");
      if (sec == null) {
         this.plugin.getLogger().warning("gui-slots section missing!");
      } else {
         sec.getKeys(false).stream().sorted().forEach((row) -> {
            String def = sec.getString(row, "");
            if (def != null && !def.isBlank()) {
               String[] var4 = def.split(",");
               int var5 = var4.length;

               for(int var6 = 0; var6 < var5; ++var6) {
                  String token = var4[var6];
                  token = token.trim();
                  if (!token.isEmpty()) {
                     if (token.contains("..")) {
                        String[] pr = token.split("\\.\\.");

                        try {
                           int from = Integer.parseInt(pr[0]);
                           int to = Integer.parseInt(pr[1]);

                           for(int i = from; i <= to; ++i) {
                              this.addSlot(i);
                           }
                        } catch (NumberFormatException var13) {
                        }
                     } else {
                        try {
                           this.addSlot(Integer.parseInt(token));
                        } catch (NumberFormatException var12) {
                        }
                     }
                  }
               }
            }

         });
      }

   }

   private void addSlot(int s) {
      if (s >= 0 && s <= 53 && !this.guiSlots.contains(s)) {
         this.guiSlots.add(s);
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
      this.closeCmd = new ConfigModule.CloseCmd(enabled, closeFirst, cmds, console);
   }

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

   public ConfigModule.CloseCmd closeCmd() {
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
      } catch (IOException | InvalidConfigurationException var2) {
         this.plugin.getLogger().severe("Could not reload config.yml: " + var2.getMessage());
      }

   }

   public static record CloseCmd(boolean enabled, boolean closeGuiFirst, List<String> commands, boolean runAsConsole) {
      public CloseCmd(boolean enabled, boolean closeGuiFirst, List<String> commands, boolean runAsConsole) {
         this.enabled = enabled;
         this.closeGuiFirst = closeGuiFirst;
         this.commands = commands;
         this.runAsConsole = runAsConsole;
      }

      public boolean enabled() {
         return this.enabled;
      }

      public boolean closeGuiFirst() {
         return this.closeGuiFirst;
      }

      public List<String> commands() {
         return this.commands;
      }

      public boolean runAsConsole() {
         return this.runAsConsole;
      }
   }

   public static enum GameState {
      ACTIVE,
      UNLOCKED,
      LOCKED,
      PROTECTED;

      private static ConfigModule.GameState[] $values() {
         return new ConfigModule.GameState[]{ACTIVE, UNLOCKED, LOCKED, PROTECTED};
      }

      // $FF: synthetic method
      private static ConfigModule.GameState[] $values$() {
         return new ConfigModule.GameState[]{ACTIVE, UNLOCKED, LOCKED, PROTECTED};
      }
   }
}
