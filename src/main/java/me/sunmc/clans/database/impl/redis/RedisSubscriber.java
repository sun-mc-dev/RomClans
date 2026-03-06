package me.sunmc.clans.database.impl.redis;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import me.sunmc.clans.RomClans;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public class RedisSubscriber extends RedisPubSubAdapter<String, String> {

    private final RomClans plugin;
    private final String ownServerId;

    public RedisSubscriber(RomClans plugin, String ownServerId) {
        this.plugin = plugin;
        this.ownServerId = ownServerId;
    }

    /**
     * Called by Lettuce's Netty event loop thread whenever a message arrives.
     * Keep this method fast — do not block here.
     */
    @Override
    public void message(String channel, String message) {
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            String serverId = json.has("serverId") ? json.get("serverId").getAsString() : "";
            // Ignore our own broadcasts
            if (ownServerId.equals(serverId)) return;

            switch (channel) {
                case RedisManager.CHAN_CHAT -> handleChat(json);
                case RedisManager.CHAN_INVITE -> handleInvite(json);
                case RedisManager.CHAN_SYNC -> handleSync(json);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Error processing Redis message on channel " + channel + ": " + message, e);
        }
    }

    private void handleChat(@NotNull JsonObject j) {
        plugin.getChatManager().receiveRedisChat(
                j.get("type").getAsString(),
                j.get("clanId").getAsString(),
                j.get("senderName").getAsString(),
                j.get("clanTag").getAsString(),
                j.get("message").getAsString()
        );
    }

    private void handleInvite(@NotNull JsonObject j) {
        plugin.getInviteManager().receiveRedisInvite(
                j.get("clanId").getAsString(),
                j.get("clanName").getAsString(),
                j.get("inviterUuid").getAsString(),
                j.get("inviteeUuid").getAsString(),
                j.get("inviteeName").getAsString()
        );
    }

    private void handleSync(@NotNull JsonObject j) {
        String type = j.get("type").getAsString();
        switch (type) {
            case "RELATION_UPDATE" -> plugin.getRelationManager().applyRedisRelationUpdate(
                    j.get("clanA").getAsString(),
                    j.get("clanB").getAsString(),
                    j.get("relType").getAsString(),
                    j.get("action").getAsString()
            );
            case "CACHE_INVALIDATE" -> plugin.getClanManager().loadAll();
        }
    }
}