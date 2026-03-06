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

    /**
     * Send a clan chat message to all online clan members, and relay via Redis.
     */
    public void sendClanChat(@NotNull Player sender, @NotNull Clan clan, Component rawMessage) {
        String plainMsg = PlainTextComponentSerializer.plainText().serialize(rawMessage);
        Component formatted = buildClanFormat("chat-format-clan", clan.getTag(), sender.getName(), plainMsg);

        // Deliver locally
        for (ClanMember m : clan.getMembers().values()) {
            Player p = plugin.getServer().getPlayer(m.getPlayerUuid());
            if (p != null) p.sendMessage(formatted);
        }

        // Relay cross-server
        if (plugin.getRedisManager().isActive()) {
            plugin.getRedisManager().publishChat(
                    "CLAN_CHAT", clan.getId().toString(),
                    sender.getName(), clan.getTag(), plainMsg
            );
        }
    }

    /**
     * Send an allay chat message to all online members of allied clans, and relay via Redis.
     */
    public void sendAllyChat(@NotNull Player sender, @NotNull Clan clan, Component rawMessage) {
        String plainMsg = PlainTextComponentSerializer.plainText().serialize(rawMessage);
        Component formatted = buildClanFormat("chat-format-ally", clan.getTag(), sender.getName(), plainMsg);

        // Send to own clan
        for (ClanMember m : clan.getMembers().values()) {
            Player p = plugin.getServer().getPlayer(m.getPlayerUuid());
            if (p != null) p.sendMessage(formatted);
        }

        // Send to allied clans
        for (UUID allyId : clan.getAllyIds()) {
            Clan ally = plugin.getClanCache().getById(allyId);
            if (ally == null) continue;
            for (ClanMember m : ally.getMembers().values()) {
                Player p = plugin.getServer().getPlayer(m.getPlayerUuid());
                if (p != null) p.sendMessage(formatted);
            }
        }

        if (plugin.getRedisManager().isActive()) {
            plugin.getRedisManager().publishChat(
                    "ALLY_CHAT", clan.getId().toString(),
                    sender.getName(), clan.getTag(), plainMsg
            );
        }
    }

    /**
     * Called by RedisSubscriber when a chat packet arrives from another server.
     */
    public void receiveRedisChat(@NotNull String type, String clanId, String senderName,
                                 String clanTag, String message) {
        String key = type.equals("CLAN_CHAT") ? "chat-format-clan" : "chat-format-ally";
        Component formatted = buildClanFormat(key, clanTag, senderName, message);
        UUID clanUuid = UUID.fromString(clanId);

        if (type.equals("CLAN_CHAT")) {
            Clan clan = plugin.getClanCache().getById(clanUuid);
            if (clan == null) return;
            clan.getMembers().values().forEach(m -> {
                Player p = plugin.getServer().getPlayer(m.getPlayerUuid());
                if (p != null) p.sendMessage(formatted);
            });
        } else {
            // Ally chat: deliver to all allied clans on this server
            Clan clan = plugin.getClanCache().getById(clanUuid);
            if (clan == null) return;
            // own members
            clan.getMembers().values().forEach(m -> {
                Player p = plugin.getServer().getPlayer(m.getPlayerUuid());
                if (p != null) p.sendMessage(formatted);
            });
            // allied clan members
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

    private Component buildClanFormat(String key, String tag, String player, String message) {
        return plugin.getMessagesManager().parse(key, Map.of(
                "tag", tag,
                "player", player,
                "message", message
        ));
    }

    public void shutdown() {
        chatModes.clear();
    }
}