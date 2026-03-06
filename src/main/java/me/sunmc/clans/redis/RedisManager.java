package me.sunmc.clans.redis;

import com.google.gson.JsonObject;
import me.sunmc.clans.RomClans;
import redis.clients.jedis.*;
import redis.clients.jedis.providers.PooledConnectionProvider;

import java.util.logging.Level;

public class RedisManager {

    public static final String CHAN_CHAT = "romclans:chat";
    public static final String CHAN_INVITE = "romclans:invite";
    public static final String CHAN_SYNC = "romclans:sync";
    private final RomClans plugin;
    private UnifiedJedis jedisClient;
    // Dedicated raw Jedis connection for the blocking subscribe call
    private Jedis subscriberJedis;
    private RedisSubscriber subscriber;
    private Thread subThread;
    private volatile boolean active = false;
    private String serverId;

    public RedisManager(RomClans plugin) {
        this.plugin = plugin;
    }

    public boolean initialize() {
        try {
            var cfg = plugin.getConfigManager();
            serverId = cfg.getServerId();

            HostAndPort hostAndPort = new HostAndPort(cfg.getRedisHost(), cfg.getRedisPort());

            // Build client config
            DefaultJedisClientConfig.Builder clientBuilder = DefaultJedisClientConfig.builder()
                    .timeoutMillis(2000)
                    .database(cfg.getRedisDatabase());

            String password = cfg.getRedisPassword();
            if (!password.isBlank()) {
                clientBuilder.password(password);
            }

            JedisClientConfig clientConfig = clientBuilder.build();

            ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
            poolConfig.setMaxTotal(8);
            poolConfig.setMaxIdle(4);
            poolConfig.setMinIdle(1);
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestWhileIdle(true);

            PooledConnectionProvider provider =
                    new PooledConnectionProvider(hostAndPort, clientConfig, poolConfig);
            jedisClient = new UnifiedJedis(provider);

            // Test connection
            jedisClient.ping();

            // Dedicated blocking connection for pub/sub — subscribe() blocks its thread
            subscriberJedis = new Jedis(hostAndPort, clientConfig);
            subscriber = new RedisSubscriber(plugin, serverId);

            subThread = new Thread(() -> {
                try {
                    // This call blocks until unsubscribed or the connection drops
                    subscriberJedis.subscribe(subscriber, CHAN_CHAT, CHAN_INVITE, CHAN_SYNC);
                } catch (Exception e) {
                    if (active) plugin.getLogger().log(Level.WARNING, "Redis subscriber disconnected", e);
                } finally {
                    try {
                        subscriberJedis.close();
                    } catch (Exception ignored) {
                    }
                }
            }, "RomClans-Redis-Sub");
            subThread.setDaemon(true);
            subThread.start();

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
        // Unsubscribe first so the subscriber thread exits cleanly
        if (subscriber != null && subscriber.isSubscribed()) {
            try {
                subscriber.unsubscribe();
            } catch (Exception ignored) {
            }
        }
        if (subscriberJedis != null) {
            try {
                subscriberJedis.close();
            } catch (Exception ignored) {
            }
        }
        if (jedisClient != null) {
            try {
                jedisClient.close();
            } catch (Exception ignored) {
            }
        }
    }

    public boolean isActive() {
        return active && jedisClient != null;
    }

    public void publishChat(String type, String clanId, String senderName, String clanTag, String message) {
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

    public void publishRelationUpdate(String clanAId, String clanBId, String relType, String action) {
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
                jedisClient.publish(channel, payload);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Redis publish failed", e);
            }
        });
    }
}