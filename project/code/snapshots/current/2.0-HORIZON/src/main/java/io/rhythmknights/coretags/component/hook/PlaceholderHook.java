package io.rhythmknights.coretags.component.hook;

import io.rhythmknights.coretags.CoreTags;
import io.rhythmknights.coretags.component.data.ConfigModule;
import io.rhythmknights.coretags.component.data.PlayerDataModule;
import io.rhythmknights.coretags.component.modal.TagModal;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;

public final class PlaceholderHook extends PlaceholderExpansion {
    private final CoreTags plugin;
    private final TagModal tags;
    private final PlayerDataModule data;
    private final ConfigModule cfg;

    public PlaceholderHook(CoreTags plugin) {
        this.plugin = plugin;
        this.tags = plugin.tags();
        this.data = plugin.playerData();
        this.cfg = plugin.configs();
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
        return plugin.getDescription().getVersion();
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
            return activeDisplay(player);
        }

        String[] split = params.split("_", 2);
        if (split.length < 2) {
            return "";
        }

        String prefix = split[0].toLowerCase(Locale.ROOT);
        String id = split[1];

        return switch (prefix) {
            case "tag" -> rawId(id);
            case "tagname" -> formattedName(id);
            case "tagcost" -> costString(id);
            case "tagstate" -> stateString(player, id);
            default -> "";
        };
    }

    private String rawId(String id) {
        return tags.byId(id).map(TagModal.Tag::id).orElse("");
    }

    private String formattedName(String id) {
        return tags.byId(id).map(tag -> componentToLegacyString(tag.name())).orElse("");
    }

    private String costString(String id) {
        Optional<TagModal.Tag> opt = tags.byId(id);
        if (opt.isEmpty()) {
            return "";
        }

        int cost = opt.get().cost();
        return (cost == 0 || cost == -1) && cfg.convertCostFree() ? cfg.freeCostText() : String.valueOf(cost);
    }

    private String stateString(OfflinePlayer player, String id) {
        TagModal.Tag tag = tags.byId(id).orElse(null);
        if (tag == null) {
            return "";
        }

        PlayerDataModule.PlayerData pd = data.get(player.getUniqueId());
        boolean unlocked = pd.unlocked.contains(id) || tag.cost() == 0 || !plugin.economy().active();
        boolean hasPerm = player.isOnline() && player.getPlayer() != null && player.getPlayer().hasPermission(tag.permission());

        ConfigModule.GameState state;
        if (!player.isOp() && !hasPerm) {
            state = ConfigModule.GameState.PROTECTED;
        } else if (id.equals(pd.active)) {
            state = ConfigModule.GameState.ACTIVE;
        } else if (unlocked) {
            state = ConfigModule.GameState.UNLOCKED;
        } else {
            state = ConfigModule.GameState.LOCKED;
        }

        return tags.statusText(state);
    }

    private String activeDisplay(OfflinePlayer player) {
        String id = data.get(player.getUniqueId()).active;
        if (id == null || id.equalsIgnoreCase("none")) {
            return "";
        }

        return tags.byId(id).map(tag -> componentToLegacyString(tag.display())).orElse("");
    }

    private String componentToLegacyString(Component component) {
        try {
            // Use CoreFramework TextUtility to convert Component to legacy string
            Class<?> textUtilityClass = Class.forName("io.rhythmknights.coreframework.component.utility.TextUtility");
            
            // Try to find a method that can serialize Components to legacy format
            // First try using Legacy utility class
            try {
                Class<?> legacyClass = Class.forName("io.rhythmknights.coreapi.component.utility.Legacy");
                Object serializer = legacyClass.getField("SERIALIZER").get(null);
                Method serializeMethod = serializer.getClass().getMethod("serialize", Component.class);
                return (String) serializeMethod.invoke(serializer, component);
            } catch (Exception ignored) {
                // Fallback to LegacyComponentSerializer
                Class<?> legacySerializerClass = Class.forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer");
                Method legacyAmpersandMethod = legacySerializerClass.getMethod("legacyAmpersand");
                Object legacySerializer = legacyAmpersandMethod.invoke(null);
                Method serializeMethod = legacySerializerClass.getMethod("serialize", Component.class);
                return (String) serializeMethod.invoke(legacySerializer, component);
            }
        } catch (Exception e) {
            // Final fallback - return string representation
            return component.toString();
        }
    }
}