package io.rhythmknights.coretags.component.hook;

import io.rhythmknights.coreframework.util.TextUtility;
import io.rhythmknights.coretags.CoreTags;
import io.rhythmknights.coretags.component.data.ConfigModule;
import io.rhythmknights.coretags.component.data.PlayerDataModule;
import io.rhythmknights.coretags.component.modal.TagModal;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;

public final class PlaceholderHook extends PlaceholderExpansion {
    private final CoreTags plugin;
    private final TagModal tags;
    private final PlayerDataModule data;
    private final ConfigModule cfg;
    private final TextUtility textUtility;

    public PlaceholderHook(CoreTags plugin) {
        this.plugin = plugin;
        this.tags = plugin.tags();
        this.data = plugin.playerData();
        this.cfg = plugin.configs();
        this.textUtility = plugin.textUtility();
        this.register();
    }

    @NotNull
    @Override
    public String getIdentifier() {
        return "coretag";
    }

    @NotNull
    @Override
    public String getAuthor() {
        return "Rhythm_Knights";
    }

    @NotNull
    @Override
    public String getVersion() {
        return this.plugin.getDescription().getVersion();
    }

    public void refreshAll() {
        // Placeholder refresh logic if needed
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        String key = params.toLowerCase(Locale.ROOT);
        if (key.isEmpty() || key.equals("coretag") || key.equals("tag")) {
            return this.activeDisplay(player);
        }

        String[] split = params.split("_", 2);
        if (split.length < 2) {
            return "";
        }

        String prefix = split[0].toLowerCase(Locale.ROOT);
        String id = split[1];

        return switch (prefix) {
            case "tag" -> this.rawId(id);
            case "tagname" -> this.formattedName(id);
            case "tagcost" -> this.costString(id);
            case "tagstate" -> this.stateString(player, id);
            default -> "";
        };
    }

    private String rawId(String id) {
        return this.tags.byId(id).map(TagModal.Tag::id).orElse("");
    }

    private String formattedName(String id) {
        return this.tags.byId(id).map(tag -> this.componentToLegacyString(tag.name())).orElse("");
    }

    private String costString(String id) {
        Optional<TagModal.Tag> tagOpt = this.tags.byId(id);
        if (tagOpt.isEmpty()) {
            return "";
        }

        int cost = tagOpt.get().cost();
        return (cost == 0 || cost == -1) && this.cfg.convertCostFree() 
            ? this.cfg.freeCostText() 
            : String.valueOf(cost);
    }

    private String stateString(OfflinePlayer player, String id) {
        TagModal.Tag tag = this.tags.byId(id).orElse(null);
        if (tag == null) {
            return "";
        }

        PlayerDataModule.PlayerData playerData = this.data.get(player.getUniqueId());
        boolean unlocked = playerData.unlocked.contains(id) || tag.cost() == 0 || !this.plugin.economy().active();
        boolean hasPermission = player.isOnline() && player.getPlayer() != null && player.getPlayer().hasPermission(tag.permission());

        ConfigModule.GameState state;
        if (!player.isOp() && !hasPermission) {
            state = ConfigModule.GameState.PROTECTED;
        } else if (id.equals(playerData.active)) {
            state = ConfigModule.GameState.ACTIVE;
        } else if (unlocked) {
            state = ConfigModule.GameState.UNLOCKED;
        } else {
            state = ConfigModule.GameState.LOCKED;
        }

        return this.tags.statusText(state);
    }

    private String activeDisplay(OfflinePlayer player) {
        String id = this.data.get(player.getUniqueId()).active;
        if (id == null || id.equalsIgnoreCase("none")) {
            return "";
        }

        return this.tags.byId(id).map(tag -> {
            String displayText = this.componentToLegacyString(tag.display());
            return displayText + ChatColor.RESET;
        }).orElse("");
    }

    private String componentToLegacyString(Component component) {
        // First try to use CoreFramework's TextUtility if it has a method for this
        try {
            // Assuming TextUtility might have a method to convert to legacy format
            return PlainTextComponentSerializer.plainText().serialize(component);
        } catch (Exception e) {
            // Fallback to legacy serializer
            return LegacyComponentSerializer.legacyAmpersand().serialize(component);
        }
    }
}