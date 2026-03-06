package me.sunmc.clans.database.impl.redis;

import com.google.gson.JsonObject;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import me.sunmc.clans.RomClans;

import java.time.Duration;
import java.util.logging.Level;

public class RedisManager {

    public static final String CHAN_CHAT = "romclans:chat";
    public static final String CHAN_INVITE = "romclans:invite";
    public static final String CHAN_SYNC = "romclans:sync";

    private final RomClans plugin;

    private RedisClient redisClient;
    // Thread-safe command connection used exclusively for publish()
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

            RedisURI.Builder uriBuilder = RedisURI.builder()
                    .withHost(cfg.getRedisHost())
                    .withPort(cfg.getRedisPort())
                    .withDatabase(cfg.getRedisDatabase())
                    .withTimeout(Duration.ofSeconds(5));

            String password = cfg.getRedisPassword();
            if (!password.isBlank()) {
                uriBuilder.withPassword(password.toCharArray());
            }

            redisClient = RedisClient.create(uriBuilder.build());

            publishConn = redisClient.connect();

            // Test connection
            publishConn.sync().ping();

            pubSubConn = redisClient.connectPubSub();
            pubSubConn.addListener(new RedisSubscriber(plugin, serverId));
            // subscribe() call is non-blocking in Lettuce (async under the hood)
            pubSubConn.sync().subscribe(CHAN_CHAT, CHAN_INVITE, CHAN_SYNC);

            active = true;
            plugin.getLogger().info("Redis (Lettuce) connected: "
                    + cfg.getRedisHost() + ":" + cfg.getRedisPort());
            return true;

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Redis connection failed", e);
            return false;
        }
    }

    public void shutdown() {
        active = false;
        if (pubSubConn != null) {
            try {
                pubSubConn.sync().unsubscribe(CHAN_CHAT, CHAN_INVITE, CHAN_SYNC);
                pubSubConn.close();
            } catch (Exception ignored) {
            }
        }
        if (publishConn != null) {
            try {
                publishConn.close();
            } catch (Exception ignored) {
            }
        }
        if (redisClient != null) {
            try {
                redisClient.shutdown();
            } catch (Exception ignored) {
            }
        }
    }

    public boolean isActive() {
        return active && publishConn != null && publishConn.isOpen();
    }

    public void publishChat(String type, String clanId, String senderName,
                            String clanTag, String message) {
        JsonObject o = new JsonObject();
        o.addProperty("type", type);
        o.addProperty("clanId", clanId);
        o.addProperty("senderName", senderName);
        o.addProperty("clanTag", clanTag);
        o.addProperty("message", message);
        o.addProperty("serverId", serverId);
        publish(CHAN_CHAT, o.toString());
    }

    public void publishInvite(String clanId, String clanName, String inviterUuid,
                              String inviteeUuid, String inviteeName) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "INVITE");
        o.addProperty("clanId", clanId);
        o.addProperty("clanName", clanName);
        o.addProperty("inviterUuid", inviterUuid);
        o.addProperty("inviteeUuid", inviteeUuid);
        o.addProperty("inviteeName", inviteeName);
        o.addProperty("serverId", serverId);
        publish(CHAN_INVITE, o.toString());
    }

    public void publishRelationUpdate(String clanAId, String clanBId,
                                      String relType, String action) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "RELATION_UPDATE");
        o.addProperty("clanA", clanAId);
        o.addProperty("clanB", clanBId);
        o.addProperty("relType", relType);
        o.addProperty("action", action);
        o.addProperty("serverId", serverId);
        publish(CHAN_SYNC, o.toString());
    }

    public void publishCacheInvalidate(String clanId) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "CACHE_INVALIDATE");
        o.addProperty("clanId", clanId);
        o.addProperty("serverId", serverId);
        publish(CHAN_SYNC, o.toString());
    }

    private void publish(String channel, String payload) {
        if (!isActive()) return;
        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            try {
                publishConn.async().publish(channel, payload);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Redis publish failed", e);
            }
        });
    }
}