package me.sunmc.clans.config;

import me.sunmc.clans.RomClans;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;

public class MessagesManager {

    private final RomClans plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private FileConfiguration msgs;
    private String rawPrefix;

    public MessagesManager(RomClans plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File f = new File(plugin.getDataFolder(), "messages.yml");
        if (!f.exists()) plugin.saveResource("messages.yml", false);
        msgs = YamlConfiguration.loadConfiguration(f);
        rawPrefix = msgs.getString("prefix", "<gray>[<gold>Clans</gold>]</gray>");
    }

    /**
     * Send a message by key (no extra placeholders).
     */
    public void send(@NotNull CommandSender sender, String key) {
        sender.sendMessage(parse(key, Map.of()));
    }

    /**
     * Send a message by key with named placeholders.
     */
    public void send(@NotNull CommandSender sender, String key, Map<String, String> ph) {
        sender.sendMessage(parse(key, ph));
    }

    /**
     * Parse a message key into an Adventure Component.
     */
    public Component parse(String key, @NotNull Map<String, String> ph) {
        String raw = msgs.getString(key, "<red>Missing message: " + key);
        raw = raw.replace("{prefix}", rawPrefix);
        TagResolver.Builder b = TagResolver.builder();
        ph.forEach((k, v) -> b.resolver(Placeholder.parsed(k, v)));
        return mm.deserialize(raw, b.build());
    }

    public Component parse(String key) {
        return parse(key, Map.of());
    }

    /**
     * Deserialize arbitrary MiniMessage string (e.g. clan tag).
     */
    public Component deserialize(String miniMsg) {
        return mm.deserialize(miniMsg);
    }

    /**
     * Strip MiniMessage tags — returns plain text.
     */
    public String stripTags(String miniMsg) {
        return mm.stripTags(miniMsg);
    }

    public MiniMessage getMiniMessage() {
        return mm;
    }

    public String getRawPrefix() {
        return rawPrefix;
    }
}