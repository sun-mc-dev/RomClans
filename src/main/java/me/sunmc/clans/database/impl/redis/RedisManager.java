package me.sunmc.clans.database.impl.redis;

import com.google.gson.JsonObject;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import me.sunmc.clans.RomClans;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.UUID;
import java.util.logging.Level;

public class RedisManager {

    public static final String CHAN_CHAT = "romclans:chat";
    public static final String CHAN_INVITE = "romclans:invite";
    public static final String CHAN_SYNC = "romclans:sync";
    public static final String CHAN_PROXY = "romclans:proxy";

    private static final String TP_KEY_PREFIX = "romclans:pending_tp:";
    private static final long TP_TTL_SECONDS = 30L;

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

    public void storePendingHomeTeleport(UUID playerUuid, UUID clanId) {
        if (!isActive()) return;
        try {
            publishConn.sync().setex(
                    TP_KEY_PREFIX + playerUuid,
                    TP_TTL_SECONDS,
                    clanId.toString());
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Redis: failed to store pending home teleport", e);
        }
    }

    public UUID consumePendingHomeTeleport(UUID playerUuid) {
        if (!isActive()) return null;
        try {
            String key = TP_KEY_PREFIX + playerUuid;
            String val = publishConn.sync().get(key);
            if (val == null) return null;
            publishConn.async().del(key);
            return UUID.fromString(val);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Redis: failed to consume pending home teleport", e);
            return null;
        }
    }

    /**
     * Tells the Velocity proxy (RClansBridge) to transfer a player to a server.
     * This is guaranteed to happen AFTER storePendingHomeTeleport() because callers
     * must invoke this from within the same async block.
     */
    public void publishSendToServer(String playerUuid, String targetServer) {
        JsonObject o = new JsonObject();
        o.addProperty("type",   "SEND_TO_SERVER");
        o.addProperty("uuid",   playerUuid);
        o.addProperty("server", targetServer);
        publish(CHAN_PROXY, o);
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

    public void publishHomeSet(String clanId, String world, double x, double y, double z, float yaw, float pitch) {
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

    public void publishAllyAccept(String requesterClanId, String acceptorClanName) {
        JsonObject o = base("ALLY_ACCEPTED");
        o.addProperty("requesterClanId", requesterClanId);
        o.addProperty("acceptorClanName", acceptorClanName);
        publish(CHAN_SYNC, o);
    }

    public void publishAllyDeny(String requesterClanId, String denierClanName) {
        JsonObject o = base("ALLY_DENIED");
        o.addProperty("requesterClanId", requesterClanId);
        o.addProperty("denierClanName", denierClanName);
        publish(CHAN_SYNC, o);
    }

    public void publishAllyRemove(String initiatorClanName, String targetClanId) {
        JsonObject o = base("ALLY_REMOVED");
        o.addProperty("initiatorClanName", initiatorClanName);
        o.addProperty("targetClanId", targetClanId);
        publish(CHAN_SYNC, o);
    }

    public void publishPlayerOnline(String uuid, String playerName) {
        JsonObject o = base("PLAYER_ONLINE");
        o.addProperty("uuid", uuid);
        o.addProperty("name", playerName);
        publish(CHAN_SYNC, o);
    }

    public void publishPlayerOffline(String uuid, String playerName) {
        JsonObject o = base("PLAYER_OFFLINE");
        o.addProperty("uuid", uuid);
        o.addProperty("name", playerName);
        publish(CHAN_SYNC, o);
    }

    public void publishRequestOnlinePlayers() {
        publish(CHAN_SYNC, base("REQUEST_ONLINE_PLAYERS"));
    }

    private @NotNull JsonObject base(String type) {
        JsonObject o = new JsonObject();
        o.addProperty("type", type);
        o.addProperty("serverId", serverId);
        return o;
    }

    private void publish(String channel, JsonObject payload) {
        if (!isActive()) return;
        try {
            publishConn.async().publish(channel, payload.toString());
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Redis publish failed on channel " + channel, e);
        }
    }
}