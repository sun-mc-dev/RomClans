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
     * Validate a MiniMessage tag string for use as a clan tag.
     * <p>
     * Rejects:
     * <ul>
     *   <li>Obfuscated / magic text</li>
     *   <li>Italic, underlined, or strikethrough decoration</li>
     *   <li>Spaces in the visible (plain-text) portion</li>
     *   <li>Visible length outside [minLen, maxLen]</li>
     * </ul>
     */
    public static boolean isValidTag(String raw, int minLen, int maxLen) {
        if (raw == null || raw.isBlank()) return false;
        // Bug 3: block forbidden decorations before even parsing
        if (containsForbiddenDecoration(raw.toLowerCase())) return false;
        try {
            Component c = MM.deserialize(raw);
            String plain = PlainTextComponentSerializer.plainText().serialize(c);
            if (plain.contains(" ")) return false;
            int len = plain.length();
            return len >= minLen && len <= maxLen;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns true if the raw (lower-cased) MiniMessage string contains any tag
     * that produces obfuscated, italic, underlined, or strikethrough text.
     */
    private static boolean containsForbiddenDecoration(@NotNull String lower) {
        // Obfuscated / magic
        if (lower.contains("<obfuscated>") || lower.contains("<magic>")) return true;
        // Italic (all aliases)
        if (lower.contains("<italic>") || lower.contains("<i>") || lower.contains("<em>")) return true;
        // Underlined
        if (lower.contains("<underlined>") || lower.contains("<u>")) return true;
        // Strikethrough
        if (lower.contains("<strikethrough>") || lower.contains("<st>") || lower.contains("<s>")) return true;
        return false;
    }
}