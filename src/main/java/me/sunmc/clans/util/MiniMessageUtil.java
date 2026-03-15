package me.sunmc.clans.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

public final class MiniMessageUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private MiniMessageUtil() {
    }

    /**
     * Granular result so callers can send the right error message.
     */
    public enum TagValidationResult {OK, FORBIDDEN_DECORATION, FORBIDDEN_SPACE, INVALID_LENGTH}

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
     * Full tag validation. Returns a specific result so CreateSubCommand and
     * RetagSubCommand can send "create-tag-forbidden" vs "create-invalid-tag".
     */
    public static @NotNull TagValidationResult validateTag(String raw, int minLen, int maxLen) {
        if (raw == null || raw.isBlank()) return TagValidationResult.INVALID_LENGTH;
        if (containsLegacyCodes(raw)) return TagValidationResult.FORBIDDEN_DECORATION;
        if (containsForbiddenDecoration(raw.toLowerCase())) return TagValidationResult.FORBIDDEN_DECORATION;
        try {
            Component c = MM.deserialize(raw);
            String plain = PlainTextComponentSerializer.plainText().serialize(c);
            if (plain.contains(" ")) return TagValidationResult.FORBIDDEN_SPACE;
            int len = plain.length();
            return (len >= minLen && len <= maxLen) ? TagValidationResult.OK : TagValidationResult.INVALID_LENGTH;
        } catch (Exception e) {
            return TagValidationResult.INVALID_LENGTH;
        }
    }

    private static boolean containsLegacyCodes(@NotNull String raw) {
        for (int i = 0; i < raw.length() - 1; i++) {
            char c = raw.charAt(i);
            if (c == '&' || c == '§') {
                char next = Character.toLowerCase(raw.charAt(i + 1));
                if ((next >= '0' && next <= '9') || (next >= 'a' && next <= 'f')
                        || next == 'k' || next == 'l' || next == 'm'
                        || next == 'n' || next == 'o' || next == 'r' || next == 'x') {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Convenience wrapper — returns true only when the tag fully passes all checks.
     */
    public static boolean isValidTag(String raw, int minLen, int maxLen) {
        return validateTag(raw, minLen, maxLen) == TagValidationResult.OK;
    }

    private static boolean containsForbiddenDecoration(@NotNull String lower) {
        if (lower.contains("<obfuscated>") || lower.contains("<magic>")) return true;
        if (lower.contains("<italic>") || lower.contains("<i>") || lower.contains("<em>")) return true;
        if (lower.contains("<underlined>") || lower.contains("<u>")) return true;
        if (lower.contains("<strikethrough>") || lower.contains("<st>") || lower.contains("<s>")) return true;
        return false;
    }
}