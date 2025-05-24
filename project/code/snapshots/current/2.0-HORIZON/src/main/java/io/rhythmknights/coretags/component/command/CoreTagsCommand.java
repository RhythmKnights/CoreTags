package io.rhythmknights.coretags.component.command;

import io.rhythmknights.coretags.CoreTags;
import io.rhythmknights.coretags.component.modal.ModalProcessor;
import io.rhythmknights.coreframework.component.utility.TextUtility;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Main command handler for CoreTags
 * Handles /coretags command and subcommands
 */
public class CoreTagsCommand implements CommandExecutor, TabCompleter {
    
    private final CoreTags plugin;
    private final ModalProcessor modalProcessor;
    
    /**
     * Constructor for CoreTagsCommand
     * 
     * @param plugin The CoreTags plugin instance
     */
    public CoreTagsCommand(CoreTags plugin) {
        this.plugin = plugin;
        this.modalProcessor = new ModalProcessor(plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Handle no arguments - open main modal
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                String message = plugin.getInternalConfig().getString("settings.system.error.player-only",
                    "<red>This command can only be used by players.</red>");
                TextUtility.sendConsoleMessage(plugin.replaceVariables(message));
                return true;
            }
            
            Player player = (Player) sender;
            
            // Check basic permission
            if (!player.hasPermission("coretags.use")) {
                String message = plugin.getInternalConfig().getString("settings.system.error.no-permission",
                    "<red>You don't have permission to use this command.</red>");
                TextUtility.sendPlayerMessage(player, plugin.replaceVariables(message));
                return true;
            }
            
            // Open main modal
            modalProcessor.openMainModal(player);
            return true;
        }
        
        // Handle subcommands
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                return handleReload(sender);
            case "admin":
                return handleAdmin(sender, args);
            case "help":
                return handleHelp(sender);
            case "version":
                return handleVersion(sender);
            default:
                return handleUnknownCommand(sender, subCommand);
        }
    }
    
    /**
     * Handles the reload subcommand
     * 
     * @param sender The command sender
     * @return True if command was handled
     */
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("coretags.admin.reload")) {
            String message = plugin.getInternalConfig().getString("settings.system.error.no-permission",
                "<red>You don't have permission to use this command.</red>");
            TextUtility.sendMessage(sender, plugin.replaceVariables(message));
            return true;
        }
        
        try {
            boolean success = plugin.reloadConfigurations();
            
            if (success) {
                String message = plugin.getInternalConfig().getString("settings.system.commands.reload.complete",
                    "<green>CoreTags configuration reloaded successfully.</green>");
                TextUtility.sendMessage(sender, plugin.replaceVariables(message));
                
                // Log the reload
                if (plugin.getInternalConfig().getBoolean("settings.system.debug", false)) {
                    plugin.getLogger().info("Configuration reloaded by " + sender.getName());
                }
            } else {
                String message = plugin.getInternalConfig().getString("settings.system.commands.reload.failed",
                    "<red>Failed to reload configuration. Check console for errors.</red>");
                TextUtility.sendMessage(sender, plugin.replaceVariables(message));
            }
        } catch (Exception e) {
            String message = plugin.getInternalConfig().getString("settings.system.commands.reload.failed",
                "<red>Failed to reload configuration. Check console for errors.</red>");
            TextUtility.sendMessage(sender, plugin.replaceVariables(message));
            
            plugin.getLogger().severe("Failed to reload configuration: " + e.getMessage());
            e.printStackTrace();
        }
        
        return true;
    }
    
    /**
     * Handles admin subcommands
     * 
     * @param sender The command sender
     * @param args All command arguments
     * @return True if command was handled
     */
    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("coretags.admin")) {
            String message = plugin.getInternalConfig().getString("settings.system.error.no-permission",
                "<red>You don't have permission to use this command.</red>");
            TextUtility.sendMessage(sender, plugin.replaceVariables(message));
            return true;
        }
        
        if (args.length < 2) {
            return handleAdminHelp(sender);
        }
        
        String adminAction = args[1].toLowerCase();
        
        switch (adminAction) {
            case "unlock":
                return handleAdminUnlock(sender, args);
            case "lock":
                return handleAdminLock(sender, args);
            case "sync":
                return handleAdminSync(sender, args);
            default:
                return handleAdminHelp(sender);
        }
    }
    
    /**
     * Handles admin help display
     * 
     * @param sender The command sender
     * @return True if command was handled
     */
    private boolean handleAdminHelp(CommandSender sender) {
        List<String> helpLines = Arrays.asList(
            "<gold>CoreTags Admin Commands:</gold>",
            "<yellow>/coretags admin unlock <tagid> <player></yellow> - Unlock a tag for a player",
            "<yellow>/coretags admin lock <tagid> <player></yellow> - Lock a tag for a player",
            "<yellow>/coretags admin sync <destination> <target></yellow> - Sync player data"
        );
        
        for (String line : helpLines) {
            TextUtility.sendMessage(sender, plugin.replaceVariables(line));
        }
        
        return true;
    }
    
    /**
     * Handles admin unlock command
     * 
     * @param sender The command sender
     * @param args All command arguments
     * @return True if command was handled
     */
    private boolean handleAdminUnlock(CommandSender sender, String[] args) {
        if (args.length < 4) {
            String usage = plugin.getInternalConfig().getString("settings.system.error.invalid-args",
                "<red>Invalid arguments.</red> <grey>Usage:</grey> <yellow>/coretags admin unlock <tagid> <player></yellow>");
            TextUtility.sendMessage(sender, plugin.replaceVariables(usage));
            return true;
        }
        
        String tagId = args[2];
        String playerName = args[3];
        
        // TODO: Implement admin unlock logic
        String message = plugin.getInternalConfig().getString("settings.system.tags.tag-authorize",
            "<gold>{tag}</gold> <grey>has been</grey> <green>unlocked</green> <grey>for</grey> <gold>{player}</gold><grey>.</grey>");
        message = message.replace("{tag}", tagId).replace("{player}", playerName);
        TextUtility.sendMessage(sender, plugin.replaceVariables(message));
        
        return true;
    }
    
    /**
     * Handles admin lock command
     * 
     * @param sender The command sender
     * @param args All command arguments
     * @return True if command was handled
     */
    private boolean handleAdminLock(CommandSender sender, String[] args) {
        if (args.length < 4) {
            String usage = plugin.getInternalConfig().getString("settings.system.error.invalid-args",
                "<red>Invalid arguments.</red> <grey>Usage:</grey> <yellow>/coretags admin lock <tagid> <player></yellow>");
            TextUtility.sendMessage(sender, plugin.replaceVariables(usage));
            return true;
        }
        
        String tagId = args[2];
        String playerName = args[3];
        
        // TODO: Implement admin lock logic
        String message = plugin.getInternalConfig().getString("settings.system.tags.tag-revoke",
            "<gold>{tag}</gold> <grey>has been</grey> <red>locked</red> <grey>for</grey> <gold>{player}</gold><grey>.</grey>");
        message = message.replace("{tag}", tagId).replace("{player}", playerName);
        TextUtility.sendMessage(sender, plugin.replaceVariables(message));
        
        return true;
    }
    
    /**
     * Handles admin sync command
     * 
     * @param sender The command sender
     * @param args All command arguments
     * @return True if command was handled
     */
    private boolean handleAdminSync(CommandSender sender, String[] args) {
        if (args.length < 4) {
            String usage = plugin.getInternalConfig().getString("settings.system.error.invalid-args",
                "<red>Invalid arguments.</red> <grey>Usage:</grey> <yellow>/coretags admin sync <destination> <target></yellow>");
            TextUtility.sendMessage(sender, plugin.replaceVariables(usage));
            return true;
        }
        
        String destination = args[2];
        String target = args[3];
        
        // TODO: Implement sync logic with confirmation system
        String message = plugin.getInternalConfig().getString("settings.system.commands.sync.sync-complete",
            "<grey>Successfully synchronized playerdata from</grey> <gold>{data.src}</gold> <grey>to</grey> <gold>{data.destination}</gold><grey>.</grey>");
        message = message.replace("{data.src}", target).replace("{data.destination}", destination);
        TextUtility.sendMessage(sender, plugin.replaceVariables(message));
        
        return true;
    }
    
    /**
     * Handles the help subcommand
     * 
     * @param sender The command sender
     * @return True if command was handled
     */
    private boolean handleHelp(CommandSender sender) {
        List<String> helpLines = Arrays.asList(
            "<gold>CoreTags Commands:</gold>",
            "<yellow>/coretags</yellow> - Open the tags menu",
            "<yellow>/coretags help</yellow> - Show this help message",
            "<yellow>/coretags version</yellow> - Show plugin version",
            "<yellow>/coretags reload</yellow> - Reload configuration (admin)",
            "<yellow>/coretags admin</yellow> - Admin commands (admin)"
        );
        
        for (String line : helpLines) {
            TextUtility.sendMessage(sender, plugin.replaceVariables(line));
        }
        
        return true;
    }
    
    /**
     * Handles the version subcommand
     * 
     * @param sender The command sender
     * @return True if command was handled
     */
    private boolean handleVersion(CommandSender sender) {
        String version = plugin.getDescription().getVersion();
        String coreApiVersion = plugin.getInternalConfig().getString("system.coreframeapi-version", "Unknown");
        
        List<String> versionLines = Arrays.asList(
            "<gold>CoreTags Version Information:</gold>",
            "<grey>Plugin Version:</grey> <yellow>" + version + "</yellow>",
            "<grey>CoreFrameAPI Version:</grey> <yellow>" + coreApiVersion + "</yellow>",
            "<grey>Author:</grey> <yellow>RhythmKnights</yellow>"
        );
        
        for (String line : versionLines) {
            TextUtility.sendMessage(sender, plugin.replaceVariables(line));
        }
        
        return true;
    }
    
    /**
     * Handles unknown subcommands
     * 
     * @param sender The command sender
     * @param subCommand The unknown subcommand
     * @return True if command was handled
     */
    private boolean handleUnknownCommand(CommandSender sender, String subCommand) {
        String message = plugin.getInternalConfig().getString("settings.system.error.unknown-command",
            "<red>Unknown command:</red> <yellow>{command}</yellow><red>. Use</red> <yellow>/coretags help</yellow> <red>for available commands.</red>");
        message = message.replace("{command}", subCommand);
        TextUtility.sendMessage(sender, plugin.replaceVariables(message));
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument - subcommands
            List<String> subCommands = Arrays.asList("help", "version");
            
            if (sender.hasPermission("coretags.admin.reload")) {
                subCommands = new ArrayList<>(subCommands);
                subCommands.add("reload");
            }
            
            if (sender.hasPermission("coretags.admin")) {
                subCommands = new ArrayList<>(subCommands);
                subCommands.add("admin");
            }
            
            String partial = args[0].toLowerCase();
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(partial)) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2 && "admin".equalsIgnoreCase(args[0])) {
            // Admin subcommands
            if (sender.hasPermission("coretags.admin")) {
                List<String> adminCommands = Arrays.asList("unlock", "lock", "sync");
                String partial = args[1].toLowerCase();
                for (String adminCommand : adminCommands) {
                    if (adminCommand.startsWith(partial)) {
                        completions.add(adminCommand);
                    }
                }
            }
        } else if (args.length == 3 && "admin".equalsIgnoreCase(args[0])) {
            // Tag IDs for admin commands
            if (sender.hasPermission("coretags.admin") && 
                ("unlock".equalsIgnoreCase(args[1]) || "lock".equalsIgnoreCase(args[1]))) {
                
                // TODO: Get actual tag IDs from configuration
                List<String> tagIds = Arrays.asList("cosmicstar", "default");
                String partial = args[2].toLowerCase();
                for (String tagId : tagIds) {
                    if (tagId.startsWith(partial)) {
                        completions.add(tagId);
                    }
                }
            } else if (sender.hasPermission("coretags.admin") && "sync".equalsIgnoreCase(args[1])) {
                // Sync destinations
                List<String> destinations = Arrays.asList("luckperms", "playerdata");
                String partial = args[2].toLowerCase();
                for (String destination : destinations) {
                    if (destination.startsWith(partial)) {
                        completions.add(destination);
                    }
                }
            }
        } else if (args.length == 4 && "admin".equalsIgnoreCase(args[0])) {
            // Player names for admin commands
            if (sender.hasPermission("coretags.admin") && 
                ("unlock".equalsIgnoreCase(args[1]) || "lock".equalsIgnoreCase(args[1]))) {
                
                // Return online player names
                String partial = args[3].toLowerCase();
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(partial)) {
                        completions.add(player.getName());
                    }
                }
            } else if (sender.hasPermission("coretags.admin") && "sync".equalsIgnoreCase(args[1])) {
                // Sync targets
                List<String> targets = Arrays.asList("all", "online");
                String partial = args[3].toLowerCase();
                for (String target : targets) {
                    if (target.startsWith(partial)) {
                        completions.add(target);
                    }
                }
                
                // Also add online player names
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(partial)) {
                        completions.add(player.getName());
                    }
                }
            }
        }
        
        return completions;
    }
    
    /**
     * Gets the modal processor instance
     * 
     * @return The modal processor
     */
    public ModalProcessor getModalProcessor() {
        return modalProcessor;
    }
}