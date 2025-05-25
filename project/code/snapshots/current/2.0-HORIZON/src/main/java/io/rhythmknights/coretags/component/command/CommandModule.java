package io.rhythmknights.coretags.component.command;

import io.rhythmknights.coreframework.util.TextUtility;
import io.rhythmknights.coretags.CoreTags;
import io.rhythmknights.coretags.component.data.PlayerDataModule;
import io.rhythmknights.coretags.component.modal.TagModal;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Command module for handling all CoreTags commands and tab completion.
 * 
 * <p>This class implements both {@link CommandExecutor} and {@link TabCompleter}
 * to provide a comprehensive command handling system for the CoreTags plugin.
 * It processes the main {@code /coretags} command and its various sub-commands.</p>
 * 
 * <p>Supported commands include:</p>
 * <ul>
 *   <li>{@code /coretags} - Opens the category GUI for players</li>
 *   <li>{@code /coretags reload} - Reloads plugin configuration</li>
 *   <li>{@code /coretags unlock <tag> <player|group> <name|*>} - Unlocks tags</li>
 *   <li>{@code /coretags lock <tag> <player|group> <name|*>} - Locks tags</li>
 * </ul>
 * 
 * <p>The command system integrates with CoreFramework's TextUtility for consistent
 * text processing and supports both individual player operations and bulk operations
 * on all online players or LuckPerms groups.</p>
 * 
 * @author RhythmKnights
 * @version 2.1-HORIZON
 * @since 1.0.0
 * 
 * @apiNote All commands require appropriate permissions as defined in the plugin.yml
 * @implNote Tab completion is context-aware and will suggest valid options based
 *           on the current command state and available data.
 */
public final class CommandModule implements CommandExecutor, TabCompleter {
    
    /** The main plugin instance for accessing other components. */
    private final CoreTags plugin;
    
    /** Tag modal for accessing tag definitions and validation. */
    private final TagModal tagModal;
    
    /** Player data manager for handling tag unlocking and locking operations. */
    private final PlayerDataModule dataManager;
    
    /** LuckPerms API for group-based operations, may be null if not available. */
    private final LuckPerms lp;
    
    /** TextUtility for consistent text processing and message formatting. */
    private final TextUtility textUtility;
    
    /** MiniMessage instance for parsing advanced text formatting. */
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    /**
     * Constructs a new CommandModule with the specified plugin instance.
     * 
     * <p>This constructor initializes all required dependencies and registers
     * this instance as both the command executor and tab completer for the
     * main {@code coretags} command.</p>
     * 
     * @param plugin the CoreTags plugin instance
     * 
     * @throws IllegalStateException if the coretags command is not defined in plugin.yml
     * @implNote The constructor will fail if the command registration fails,
     *           preventing the plugin from loading properly.
     */
    public CommandModule(@NotNull CoreTags plugin) {
        this.plugin = plugin;
        this.tagModal = plugin.tags();
        this.dataManager = plugin.playerData();
        this.lp = plugin.luckPerms().api();
        this.textUtility = plugin.textUtility();
        
        PluginCommand cmd = Objects.requireNonNull(plugin.getCommand("coretags"), "coretags command missing from plugin.yml");
        cmd.setExecutor(this);
        cmd.setTabCompleter(this);
    }

    /**
     * Executes the coretags command with the given arguments.
     * 
     * <p>This method handles all sub-commands and routing logic. For players
     * executing the command without arguments, it opens the category GUI.
     * For console senders, it displays available sub-commands.</p>
     * 
     * @param sender the command sender (player or console)
     * @param command the command that was executed
     * @param label the alias that was used to execute the command
     * @param args the command arguments
     * @return true if the command was handled successfully, false otherwise
     * 
     * @apiNote This method performs permission checking for all sub-commands
     *          and provides appropriate error messages for insufficient permissions.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                this.plugin.modalProcessor().openCategoryGui(player);
            } else {
                sender.sendMessage("/coretags reload | unlock | lock");
            }
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        return switch (subCommand) {
            case "reload" -> this.handleReload(sender);
            case "unlock", "lock" -> this.handleGrantRevoke(sender, args, subCommand.equals("unlock"));
            default -> {
                sender.sendMessage("§cUnknown sub-command.");
                yield true;
            }
        };
    }

    /**
     * Handles the reload sub-command.
     * 
     * <p>This method checks for the required permission and then triggers
     * a complete plugin reload, including all configuration files and
     * component reinitialization.</p>
     * 
     * @param sender the command sender
     * @return true indicating the command was handled
     * 
     * @apiNote Requires the {@code coretags.reload} permission
     * @implNote The reload operation is performed synchronously and may
     *           cause a brief pause in plugin functionality.
     */
    private boolean handleReload(@NotNull CommandSender sender) {
        if (!sender.hasPermission("coretags.reload")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }

        this.plugin.reloadEverything();
        this.plugin.sendReloadMessage(sender);
        return true;
    }

    /**
     * Handles the unlock and lock sub-commands.
     * 
     * <p>This method processes tag unlock/lock operations for both individual
     * players and LuckPerms groups. It supports bulk operations using the
     * wildcard {@code *} for all online players.</p>
     * 
     * @param sender the command sender
     * @param args the command arguments
     * @param grant true for unlock operations, false for lock operations
     * @return true indicating the command was handled
     * 
     * @apiNote Requires the {@code coretags.admin.*} permission
     * @implNote Group operations require LuckPerms to be installed and available.
     */
    private boolean handleGrantRevoke(@NotNull CommandSender sender, @NotNull String[] args, boolean grant) {
        if (!sender.hasPermission("coretags.admin.*")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage("§cUsage: /coretags " + (grant ? "unlock" : "lock") + " <tag> <player|group> <name|*>");
            return true;
        }

        String tagId = args[1].toLowerCase(Locale.ROOT);
        TagModal.Tag tag = this.tagModal.byId(tagId).orElse(null);
        if (tag == null) {
            sender.sendMessage("§cTag '" + tagId + "' not found.");
            return true;
        }

        String scope = args[2].toLowerCase(Locale.ROOT);
        String target = args[3];

        if (scope.equals("player")) {
            return this.handlePlayerScope(sender, target, tagId, grant);
        } else if (scope.equals("group")) {
            return this.handleGroupScope(sender, target, tagId, grant);
        } else {
            sender.sendMessage("§cScope must be player or group.");
            return true;
        }
    }

    /**
     * Handles tag operations for players (individual or bulk).
     * 
     * <p>This method can operate on a specific player by name or on all
     * online players when the target is {@code *}.</p>
     * 
     * @param sender the command sender
     * @param target the target player name or {@code *} for all online players
     * @param tagId the ID of the tag to unlock/lock
     * @param grant true for unlock, false for lock
     * @return true indicating the operation was handled
     * 
     * @implNote Unknown players are rejected to prevent data corruption.
     *           The {@code *} wildcard only affects currently online players.
     */
    private boolean handlePlayerScope(@NotNull CommandSender sender, @NotNull String target, @NotNull String tagId, boolean grant) {
        if (target.equals("*")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                this.toggleForPlayer(player.getUniqueId(), tagId, grant, sender);
            }
            sender.sendMessage("§aUpdated all online players.");
        } else {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(target);
            if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
                sender.sendMessage("§cUnknown player.");
                return true;
            }
            this.toggleForPlayer(offlinePlayer.getUniqueId(), tagId, grant, sender);
            sender.sendMessage("§aUpdated " + offlinePlayer.getName() + ".");
        }
        return true;
    }

    /**
     * Handles tag operations for LuckPerms groups.
     * 
     * <p>This method adds or removes permission nodes from LuckPerms groups
     * to control tag access. The permission node format is
     * {@code coretags.tag.<tagId>}.</p>
     * 
     * @param sender the command sender
     * @param target the name of the LuckPerms group
     * @param tagId the ID of the tag to unlock/lock
     * @param grant true for unlock (add permission), false for lock (remove permission)
     * @return true indicating the operation was handled
     * 
     * @apiNote Requires LuckPerms to be installed and available
     * @implNote Group operations are performed directly through the LuckPerms API
     *           and take effect immediately for all group members.
     */
    private boolean handleGroupScope(@NotNull CommandSender sender, @NotNull String target, @NotNull String tagId, boolean grant) {
        if (this.lp == null) {
            sender.sendMessage("§cLuckPerms not found.");
            return true;
        }

        Group group = this.lp.getGroupManager().getGroup(target);
        if (group == null) {
            sender.sendMessage("§cGroup not found.");
            return true;
        }

        String node = "coretags.tag." + tagId;
        if (grant) {
            group.data().add(Node.builder(node).value(true).build());
        } else {
            group.data().remove(Node.builder(node).build());
        }

        sender.sendMessage("§aGroup '" + group.getName() + "' updated.");
        return true;
    }

    /**
     * Toggles tag access for a specific player.
     * 
     * <p>This method handles the core logic for unlocking or locking tags
     * for individual players. It also sends appropriate messages to both
     * the affected player (if online) and the command sender.</p>
     * 
     * @param uuid the UUID of the player
     * @param tagId the ID of the tag to toggle
     * @param grant true to unlock the tag, false to lock it
     * @param sender the command sender to receive feedback messages
     * 
     * @implNote This method extracts tag names from components for message
     *           formatting and handles both TextComponent and other component types.
     */
    private void toggleForPlayer(@NotNull UUID uuid, @NotNull String tagId, boolean grant, @NotNull CommandSender sender) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        TagModal.Tag tag = this.tagModal.byId(tagId).orElse(null);
        
        Component tagNameComponent = tag != null ? tag.name() : Component.text(tagId);
        String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";
        String tagName = "";
        
        if (tagNameComponent instanceof TextComponent textComponent) {
            tagName = textComponent.content().trim();
        } else {
            tagName = PlainTextComponentSerializer.plainText().serialize(tagNameComponent);
        }
        
        if (grant) {
            this.dataManager.unlockTag(uuid, tagId);
            this.sendPlayerMessage(offlinePlayer, "settings.messages.tag-unlocked", tagName);
            this.sendConsoleMessage(sender, "settings.messages.tag-authorize", tagName, playerName);
        } else {
            this.dataManager.lockTag(uuid, tagId);
            this.sendPlayerMessage(offlinePlayer, "settings.messages.tag-locked", tagName);
            this.sendConsoleMessage(sender, "settings.messages.tag-revoke", tagName, playerName);
        }
    }

    /**
     * Sends a formatted message to a player if they are online.
     * 
     * <p>This method retrieves the message template from configuration,
     * replaces placeholders, and sends it to the player using CoreFramework's
     * TextUtility for consistent formatting.</p>
     * 
     * @param player the player to send the message to
     * @param configPath the configuration path for the message template
     * @param tagName the tag name to substitute in the message
     * 
     * @implNote Messages are only sent if the player is currently online.
     *           Offline players will not receive the notification.
     */
    private void sendPlayerMessage(@NotNull OfflinePlayer player, @NotNull String configPath, @NotNull String tagName) {
        if (player.isOnline() && player.getPlayer() != null) {
            String message = this.plugin.getConfig().getString(configPath, "");
            message = message.replace("{tag}", tagName);
            Component parsedMessage = this.textUtility.parseText(message);
            player.getPlayer().sendMessage(parsedMessage);
        }
    }

    /**
     * Sends a formatted message to the command sender.
     * 
     * <p>This method is used for sending feedback messages to administrators
     * who execute tag unlock/lock commands. It supports both tag name and
     * player name placeholders.</p>
     * 
     * @param sender the command sender to receive the message
     * @param configPath the configuration path for the message template
     * @param tagName the tag name to substitute in the message
     * @param playerName the player name to substitute in the message
     * 
     * @implNote All messages are processed through CoreFramework's TextUtility
     *           for consistent formatting and color support.
     */
    private void sendConsoleMessage(@NotNull CommandSender sender, @NotNull String configPath, @NotNull String tagName, @NotNull String playerName) {
        String message = this.plugin.getConfig().getString(configPath, "");
        message = message.replace("{tag}", tagName).replace("{player}", playerName);
        Component parsedMessage = this.textUtility.parseText(message);
        sender.sendMessage(parsedMessage);
    }

    /**
     * Provides tab completion for the coretags command.
     * 
     * <p>This method offers context-aware tab completion based on the current
     * argument position and previously entered arguments. It supports completion
     * for sub-commands, tag IDs, scope types, and target names.</p>
     * 
     * @param sender the command sender
     * @param command the command being completed
     * @param alias the alias used to execute the command
     * @param args the current command arguments
     * @return a list of possible completions, or an empty list if none are available
     * 
     * @apiNote Tab completion is case-insensitive and will filter results based
     *          on the partially typed argument.
     * @implNote Player suggestions include all players who have ever joined the server,
     *           while group suggestions only include currently loaded LuckPerms groups.
     */
    @Override
    @NotNull
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return switch (args.length) {
            case 1 -> this.filter(args[0], List.of("reload", "unlock", "lock"));
            case 2 -> {
                if (args[0].equalsIgnoreCase("unlock") || args[0].equalsIgnoreCase("lock")) {
                    yield this.filter(args[1], this.tagModal.all().stream().map(TagModal.Tag::id).toList());
                }
                yield List.of();
            }
            case 3 -> this.filter(args[2], List.of("player", "group"));
            case 4 -> {
                if (args[2].equalsIgnoreCase("player")) {
                    yield this.filter(args[3], this.playerSuggestions());
                }
                if (args[2].equalsIgnoreCase("group") && this.lp != null) {
                    yield this.filter(args[3], this.lp.getGroupManager().getLoadedGroups().stream()
                            .map(Group::getName).toList());
                }
                yield List.of();
            }
            default -> List.of();
        };
    }

    /**
     * Generates a list of player name suggestions for tab completion.
     * 
     * <p>This method returns the names of all players who have ever joined
     * the server, providing a comprehensive list for administrative commands.</p>
     * 
     * @return a list of player names, excluding any null values
     * 
     * @implNote This method may return a large list on servers with many
     *           historical players. The filtering is done client-side.
     */
    @NotNull
    private List<String> playerSuggestions() {
        return Arrays.stream(Bukkit.getOfflinePlayers())
                .map(OfflinePlayer::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Filters a list of strings based on a prefix match.
     * 
     * <p>This utility method is used throughout the tab completion system
     * to filter available options based on what the user has already typed.</p>
     * 
     * @param arg the partial argument typed by the user
     * @param base the base list of possible completions
     * @return a filtered and sorted list of matching completions
     * 
     * @implNote The filtering is case-insensitive and results are sorted
     *           alphabetically for consistent presentation.
     */
    @NotNull
    private List<String> filter(@NotNull String arg, @NotNull List<String> base) {
        String lowercaseArg = arg.toLowerCase(Locale.ROOT);
        return base.stream()
                .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(lowercaseArg))
                .sorted()
                .toList();
    }
}