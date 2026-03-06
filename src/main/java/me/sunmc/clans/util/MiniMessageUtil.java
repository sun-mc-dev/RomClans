package me.sunmc.clans.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

public final class MiniMessageUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private MiniMessageUtil() {
    }

    public static @NotNull Component parse(String raw) {
        return MM.deserialize(raw);
    }

    public static @NotNull String strip(String raw) {
        return MM.stripTags(raw);
    }

    public static @NotNull String plainText(Component c) {
        return PlainTextComponentSerializer.plainText().serialize(c);
    }

    /**
     * Validate a MiniMessage tag string.
     * Returns true if parseable and visible-length is within [minLen, maxLen].
     */
    public static boolean isValidTag(String raw, int minLen, int maxLen) {
        try {
            Component c = MM.deserialize(raw);
            String plain = PlainTextComponentSerializer.plainText().serialize(c);
            int len = plain.length();
            return len >= minLen && len <= maxLen;
        } catch (Exception e) {
            return false;
        }
    }
}