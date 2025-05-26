package io.rhythmknights.coretags.component.command;

import io.rhythmknights.coretags.CoreTags;
import io.rhythmknights.coretags.component.data.PlayerDataModule;
import io.rhythmknights.coretags.component.modal.TagModal;

import net.kyori.adventure.text.Component;
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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public final class CommandModule implements CommandExecutor, TabCompleter {
    private final CoreTags plugin;
    private final TagModal tagModal;
    private final PlayerDataModule dataManager;
    private final LuckPerms lp;

    public CommandModule(CoreTags plugin) {
        this.plugin = plugin;
        this.tagModal = plugin.tags();
        this.dataManager = plugin.playerData();
        this.lp = plugin.luckPerms().api();
        
        PluginCommand cmd = Objects.requireNonNull(plugin.getCommand("coretags"), "coretags command missing from plugin.yml");
        cmd.setExecutor(this);
        cmd.setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                plugin.modalProcessor().openCategoryGui(p);
            } else {
                sendMessage(sender, "/coretags reload | unlock | lock");
            }
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        switch (subCommand) {
            case "reload":
                if (!sender.hasPermission("coretags.reload")) {
                    sendMessage(sender, "<red>No permission.</red>");
                    return true;
                }
                plugin.reloadEverything();
                plugin.sendReloadMessage(sender);
                return true;
                
            case "unlock":
            case "lock":
                boolean grant = args[0].equalsIgnoreCase("unlock");
                if (!sender.hasPermission("coretags.admin.*")) {
                    sendMessage(sender, "<red>No permission.</red>");
                    return true;
                }
                return handleGrantRevoke(sender, args, grant);
                
            default:
                sendMessage(sender, "<red>Unknown sub-command.</red>");
                return true;
        }
    }

    private boolean handleGrantRevoke(CommandSender sender, String[] args, boolean grant) {
        if (args.length < 4) {
            String usage = "/coretags " + (grant ? "unlock" : "lock") + " <tag> <player|group> <name|*>";
            sendMessage(sender, "<red>Usage: " + usage + "</red>");
            return true;
        }

        String tagId = args[1].toLowerCase(Locale.ROOT);
        TagModal.Tag tag = tagModal.byId(tagId).orElse(null);
        if (tag == null) {
            sendMessage(sender, "<red>Tag '" + tagId + "' not found.</red>");
            return true;
        }

        String scope = args[2].toLowerCase(Locale.ROOT);
        String target = args[3];

        if (scope.equals("player")) {
            if (target.equals("*")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    toggleForPlayer(p.getUniqueId(), tagId, grant, sender);
                }
                sendMessage(sender, "<green>Updated all online players.</green>");
            } else {
                OfflinePlayer off = Bukkit.getOfflinePlayer(target);
                if (!off.hasPlayedBefore() && !off.isOnline()) {
                    sendMessage(sender, "<red>Unknown player.</red>");
                    return true;
                }
                toggleForPlayer(off.getUniqueId(), tagId, grant, sender);
                sendMessage(sender, "<green>Updated " + off.getName() + ".</green>");
            }
        } else if (scope.equals("group")) {
            if (lp == null) {
                sendMessage(sender, "<red>LuckPerms not found.</red>");
                return true;
            }

            Group group = lp.getGroupManager().getGroup(target);
            if (group == null) {
                sendMessage(sender, "<red>Group not found.</red>");
                return true;
            }

            String node = "coretags.tag." + tagId;
            if (grant) {
                group.data().add(Node.builder(node).value(true).build());
            } else {
                group.data().remove(Node.builder(node).build());
            }
            sendMessage(sender, "<green>Group '" + group.getName() + "' updated.</green>");
        } else {
            sendMessage(sender, "<red>Scope must be player or group.</red>");
        }

        return true;
    }

    private void toggleForPlayer(UUID uuid, String tagId, boolean grant, CommandSender sender) {
        OfflinePlayer off = Bukkit.getOfflinePlayer(uuid);
        TagModal.Tag tag = tagModal.byId(tagId).orElse(null);
        
        String tagName;
        if (tag != null) {
            tagName = extractTagName(tag.name());
        } else {
            tagName = tagId;
        }

        String playerName = off.getName() != null ? off.getName() : "Unknown";

        if (grant) {
            dataManager.unlockTag(uuid, tagId);
            
            // Send message to player if online
            String playerMessage = plugin.getConfig().getString("settings.messages.tag-unlocked", 
                "<green>{tag}</green> <gray>has been unlocked.</gray>");
            playerMessage = playerMessage.replace("{tag}", tagName);
            
            if (off.isOnline() && off.getPlayer() != null) {
                sendMessage(off.getPlayer(), playerMessage);
            }

            // Send message to console/admin
            String consoleMessage = plugin.getConfig().getString("settings.messages.tag-authorize", 
                "<gold>{tag}</gold> <gray>tag</gray> <green>unlocked</green> <gray>for</gray> <blue>{player}</blue>.");
            consoleMessage = consoleMessage.replace("{tag}", tagName).replace("{player}", playerName);
            sendMessage(sender, consoleMessage);
            
        } else {
            dataManager.lockTag(uuid, tagId);
            
            // Send message to player if online
            String playerMessage = plugin.getConfig().getString("settings.messages.tag-locked", 
                "<red>{tag}</red> <gray>has been locked.</gray>");
            playerMessage = playerMessage.replace("{tag}", tagName);
            
            if (off.isOnline() && off.getPlayer() != null) {
                sendMessage(off.getPlayer(), playerMessage);
            }

            // Send message to console/admin
            String consoleMessage = plugin.getConfig().getString("settings.messages.tag-revoke", 
                "<gold>{tag}</gold> <gray>tag</gray> <red>locked</red> <gray>for</gray> <blue>{player}</blue>.");
            consoleMessage = consoleMessage.replace("{tag}", tagName).replace("{player}", playerName);
            sendMessage(sender, consoleMessage);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        switch (args.length) {
            case 1:
                return filter(args[0], List.of("reload", "unlock", "lock"));
                
            case 2:
                if (args[0].equalsIgnoreCase("unlock") || args[0].equalsIgnoreCase("lock")) {
                    return filter(args[1], tagModal.all().stream()
                        .map(TagModal.Tag::id)
                        .collect(Collectors.toList()));
                }
                break;
                
            case 3:
                return filter(args[2], List.of("player", "group"));
                
            case 4:
                if (args[2].equalsIgnoreCase("player")) {
                    return filter(args[3], playerSuggestions());
                }
                if (args[2].equalsIgnoreCase("group") && lp != null) {
                    return filter(args[3], lp.getGroupManager().getLoadedGroups().stream()
                        .map(Group::getName)
                        .collect(Collectors.toList()));
                }
                break;
        }

        return List.of();
    }

    private List<String> playerSuggestions() {
        return Arrays.stream(Bukkit.getOfflinePlayers())
            .map(OfflinePlayer::getName)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private List<String> filter(String arg, List<String> base) {
        String low = arg.toLowerCase(Locale.ROOT);
        return base.stream()
            .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(low))
            .sorted()
            .collect(Collectors.toList());
    }

    private void sendMessage(CommandSender sender, String message) {
        try {
            // Use CoreFramework TextUtility for message sending
            Class<?> textUtilityClass = Class.forName("io.rhythmknights.coreframework.component.utility.TextUtility");
            Method sendMessageMethod = textUtilityClass.getMethod("sendMessage", CommandSender.class, String.class);
            sendMessageMethod.invoke(null, sender, message);
        } catch (Exception e) {
            // Fallback to basic message sending
            sender.sendMessage(message);
        }
    }

    private String extractTagName(Component component) {
        try {
            // Try to extract plain text from Component
            Class<?> plainTextSerializerClass = Class.forName("net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer");
            Method plainTextMethod = plainTextSerializerClass.getMethod("plainText");
            Object plainTextSerializer = plainTextMethod.invoke(null);
            Method serializeMethod = plainTextSerializerClass.getMethod("serialize", Component.class);
            return (String) serializeMethod.invoke(plainTextSerializer, component);
        } catch (Exception e) {
            // Fallback - return string representation
            return component.toString();
        }
    }
}