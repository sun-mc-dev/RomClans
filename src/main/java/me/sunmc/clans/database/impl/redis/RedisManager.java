package me.sunmc.clans.database.impl.redis;

import com.google.gson.JsonObject;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import me.sunmc.clans.RomClans;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.logging.Level;

public class RedisManager {

    public static final String CHAN_CHAT = "romclans:chat";
    public static final String CHAN_INVITE = "romclans:invite";
    public static final String CHAN_SYNC = "romclans:sync";

    private final RomClans plugin;
    private RedisClient redisClient;
    private StatefulRedisConnection<String, String> publishConn;
    private StatefulRedisPubSubConnection<String, String> pubSubConn;
    private volatile boolean active = false;
    private String serverId;

    public RedisManager(RomClans plugin) {
        this.plugin = plugin;
    }

    public boolean initialize() {
        try {
            var cfg = plugin.getConfigManager();
            serverId = cfg.getServerId();
            RedisURI.Builder b = RedisURI.builder()
                    .withHost(cfg.getRedisHost()).withPort(cfg.getRedisPort())
                    .withDatabase(cfg.getRedisDatabase()).withTimeout(Duration.ofSeconds(5));
            if (!cfg.getRedisPassword().isBlank()) b.withPassword(cfg.getRedisPassword().toCharArray());
            redisClient = RedisClient.create(b.build());
            publishConn = redisClient.connect();
            publishConn.sync().ping();
            pubSubConn = redisClient.connectPubSub();
            pubSubConn.addListener(new RedisSubscriber(plugin, serverId));
            pubSubConn.sync().subscribe(CHAN_CHAT, CHAN_INVITE, CHAN_SYNC);
            active = true;
            plugin.getLogger().info("Redis connected: " + cfg.getRedisHost() + ":" + cfg.getRedisPort());
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Redis connection failed", e);
            return false;
        }
    }

    public void shutdown() {
        active = false;
        try {
            if (pubSubConn != null) {
                pubSubConn.sync().unsubscribe(CHAN_CHAT, CHAN_INVITE, CHAN_SYNC);
                pubSubConn.close();
            }
        } catch (Exception ignored) {
        }
        try {
            if (publishConn != null) publishConn.close();
        } catch (Exception ignored) {
        }
        try {
            if (redisClient != null) redisClient.shutdown();
        } catch (Exception ignored) {
        }
    }

    public boolean isActive() {
        return active && publishConn != null && publishConn.isOpen();
    }

    public void publishChat(String type, String clanId, String senderName, String clanTag, String message) {
        JsonObject o = base(type);
        o.addProperty("clanId", clanId);
        o.addProperty("senderName", senderName);
        o.addProperty("clanTag", clanTag);
        o.addProperty("message", message);
        publish(CHAN_CHAT, o);
    }

    public void publishInvite(String clanId, String clanName, String inviterUuid,
                              String inviteeUuid, String inviteeName) {
        JsonObject o = base("INVITE");
        o.addProperty("clanId", clanId);
        o.addProperty("clanName", clanName);
        o.addProperty("inviterUuid", inviterUuid);
        o.addProperty("inviteeUuid", inviteeUuid);
        o.addProperty("inviteeName", inviteeName);
        publish(CHAN_INVITE, o);
    }

    public void publishRelationUpdate(String clanA, String clanB, String relType, String action) {
        JsonObject o = base("RELATION_UPDATE");
        o.addProperty("clanA", clanA);
        o.addProperty("clanB", clanB);
        o.addProperty("relType", relType);
        o.addProperty("action", action);
        publish(CHAN_SYNC, o);
    }

    public void publishFriendlyFireToggle(String clanId, boolean ff) {
        JsonObject o = base("FF_TOGGLE");
        o.addProperty("clanId", clanId);
        o.addProperty("ff", ff);
        publish(CHAN_SYNC, o);
    }

    public void publishRetag(String clanId, String tag) {
        JsonObject o = base("RETAG");
        o.addProperty("clanId", clanId);
        o.addProperty("tag", tag);
        publish(CHAN_SYNC, o);
    }

    public void publishRankUpdate(String clanId, String playerUuid, String rank) {
        JsonObject o = base("RANK_UPDATE");
        o.addProperty("clanId", clanId);
        o.addProperty("playerUuid", playerUuid);
        o.addProperty("rank", rank);
        publish(CHAN_SYNC, o);
    }

    public void publishMemberAdd(String clanId, String playerUuid, String playerName, String rank) {
        JsonObject o = base("MEMBER_ADD");
        o.addProperty("clanId", clanId);
        o.addProperty("playerUuid", playerUuid);
        o.addProperty("playerName", playerName);
        o.addProperty("rank", rank);
        publish(CHAN_SYNC, o);
    }

    public void publishMemberRemove(String clanId, String playerUuid) {
        JsonObject o = base("MEMBER_REMOVE");
        o.addProperty("clanId", clanId);
        o.addProperty("playerUuid", playerUuid);
        publish(CHAN_SYNC, o);
    }

    public void publishDisband(String clanId) {
        JsonObject o = base("DISBAND");
        o.addProperty("clanId", clanId);
        publish(CHAN_SYNC, o);
    }

    public void publishTransfer(String clanId, String newLeaderUuid, String oldLeaderUuid) {
        JsonObject o = base("TRANSFER");
        o.addProperty("clanId", clanId);
        o.addProperty("newLeader", newLeaderUuid);
        o.addProperty("oldLeader", oldLeaderUuid);
        publish(CHAN_SYNC, o);
    }

    public void publishHomeSet(String clanId, String world, double x, double y, double z,
                               float yaw, float pitch) {
        JsonObject o = base("HOME_SET");
        o.addProperty("clanId", clanId);
        o.addProperty("world", world);
        o.addProperty("x", x);
        o.addProperty("y", y);
        o.addProperty("z", z);
        o.addProperty("yaw", yaw);
        o.addProperty("pitch", pitch);
        publish(CHAN_SYNC, o);
    }

    public void publishCacheInvalidate(String clanId) {
        JsonObject o = base("CACHE_INVALIDATE");
        o.addProperty("clanId", clanId);
        publish(CHAN_SYNC, o);
    }
    
    public void publishAllyRequest(String requesterClanId, String requesterClanName, String targetClanId) {
        JsonObject o = base("ALLY_REQUEST");
        o.addProperty("requesterClanId", requesterClanId);
        o.addProperty("requesterClanName", requesterClanName);
        o.addProperty("targetClanId", targetClanId);
        publish(CHAN_SYNC, o);
    }

    public void publishPlayerOnline(String uuid) {
        JsonObject o = base("PLAYER_ONLINE");
        o.addProperty("uuid", uuid);
        publish(CHAN_SYNC, o);
    }

    public void publishPlayerOffline(String uuid) {
        JsonObject o = base("PLAYER_OFFLINE");
        o.addProperty("uuid", uuid);
        publish(CHAN_SYNC, o);
    }

    private @NotNull JsonObject base(String type) {
        JsonObject o = new JsonObject();
        o.addProperty("type", type);
        o.addProperty("serverId", serverId);
        return o;
    }

    /**
     * Non-blocking: Lettuce's async publish returns immediately without blocking the caller.
     */
    private void publish(String channel, JsonObject payload) {
        if (!isActive()) return;
        try {
            publishConn.async().publish(channel, payload.toString());
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Redis publish failed on channel " + channel, e);
        }
    }
}