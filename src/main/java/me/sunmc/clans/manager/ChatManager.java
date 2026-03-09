package me.sunmc.clans.manager;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.model.ChatMode;
import me.sunmc.clans.model.Clan;
import me.sunmc.clans.model.ClanMember;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatManager {

    private final RomClans plugin;
    private final ConcurrentHashMap<UUID, ChatMode> chatModes = new ConcurrentHashMap<>();

    public ChatManager(RomClans plugin) {
        this.plugin = plugin;
    }

    public ChatMode getChatMode(UUID playerUuid) {
        return chatModes.getOrDefault(playerUuid, ChatMode.GLOBAL);
    }

    public ChatMode cycleChatMode(UUID playerUuid, boolean inClan, boolean hasAllies) {
        ChatMode current = getChatMode(playerUuid);
        ChatMode next = switch (current) {
            case GLOBAL -> inClan ? ChatMode.CLAN : ChatMode.GLOBAL;
            case CLAN -> inClan && hasAllies ? ChatMode.ALLY : ChatMode.GLOBAL;
            case ALLY -> ChatMode.GLOBAL;
        };
        chatModes.put(playerUuid, next);
        return next;
    }

    public void resetMode(UUID playerUuid) {
        chatModes.remove(playerUuid);
    }

    public void sendClanChat(@NotNull Player sender, @NotNull Clan clan, Component rawMessage) {
        String plainMsg = PlainTextComponentSerializer.plainText().serialize(rawMessage);
        Component formatted = buildClanFormat("chat-format-clan", clan.getTag(), sender.getName(), plainMsg);
        for (ClanMember m : clan.getMembers().values()) {
            Player p = plugin.getServer().getPlayer(m.getPlayerUuid());
            if (p != null) p.sendMessage(formatted);
        }
        if (plugin.getRedisManager().isActive())
            plugin.getRedisManager().publishChat("CLAN_CHAT", clan.getId().toString(),
                    sender.getName(), clan.getTag(), plainMsg);
    }

    public void sendAllyChat(@NotNull Player sender, @NotNull Clan clan, Component rawMessage) {
        String plainMsg = PlainTextComponentSerializer.plainText().serialize(rawMessage);
        Component formatted = buildClanFormat("chat-format-ally", clan.getTag(), sender.getName(), plainMsg);
        for (ClanMember m : clan.getMembers().values()) {
            Player p = plugin.getServer().getPlayer(m.getPlayerUuid());
            if (p != null) p.sendMessage(formatted);
        }
        for (UUID allyId : clan.getAllyIds()) {
            Clan ally = plugin.getClanCache().getById(allyId);
            if (ally == null) continue;
            for (ClanMember m : ally.getMembers().values()) {
                Player p = plugin.getServer().getPlayer(m.getPlayerUuid());
                if (p != null) p.sendMessage(formatted);
            }
        }
        if (plugin.getRedisManager().isActive())
            plugin.getRedisManager().publishChat("ALLY_CHAT", clan.getId().toString(),
                    sender.getName(), clan.getTag(), plainMsg);
    }

    public void receiveRedisChat(@NotNull String type, String clanId, String senderName,
                                 String clanTag, String message) {
        String key = type.equals("CLAN_CHAT") ? "chat-format-clan" : "chat-format-ally";
        Component formatted = buildClanFormat(key, clanTag, senderName, message);
        UUID clanUuid = UUID.fromString(clanId);
        Clan clan = plugin.getClanCache().getById(clanUuid);
        if (clan == null) return;
        clan.getMembers().values().forEach(m -> {
            Player p = plugin.getServer().getPlayer(m.getPlayerUuid());
            if (p != null) p.sendMessage(formatted);
        });
        if (!type.equals("CLAN_CHAT")) {
            clan.getAllyIds().forEach(allyId -> {
                Clan ally = plugin.getClanCache().getById(allyId);
                if (ally == null) return;
                ally.getMembers().values().forEach(m -> {
                    Player p = plugin.getServer().getPlayer(m.getPlayerUuid());
                    if (p != null) p.sendMessage(formatted);
                });
            });
        }
    }

    /**
     * Builds a chat line. The tag is deserialized to a Component first so its
     * styles are fully isolated — an unclosed tag such as {@code <green>CLN}
     * will not bleed colour into the player name or message text
     */
    private Component buildClanFormat(String key, String tag, String player, String message) {
        return plugin.getMessagesManager().parse(key,
                Map.of("player", player, "message", message),
                Map.of("tag", plugin.getMessagesManager().deserialize(tag)));
    }

    public void shutdown() {
        chatModes.clear();
    }
}