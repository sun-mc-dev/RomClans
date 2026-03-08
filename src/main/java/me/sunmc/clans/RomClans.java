package me.sunmc.clans;

import me.sunmc.clans.cache.ClanCache;
import me.sunmc.clans.cache.NetworkPlayerTracker;
import me.sunmc.clans.command.ClanAdminCommand;
import me.sunmc.clans.command.ClanCommand;
import me.sunmc.clans.config.ConfigManager;
import me.sunmc.clans.config.MessagesManager;
import me.sunmc.clans.database.Database;
import me.sunmc.clans.database.DatabaseManager;
import me.sunmc.clans.database.impl.redis.RedisManager;
import me.sunmc.clans.gui.GUIManager;
import me.sunmc.clans.listener.FriendlyFireListener;
import me.sunmc.clans.listener.PlayerChatListener;
import me.sunmc.clans.listener.PlayerJoinQuitListener;
import me.sunmc.clans.manager.ChatManager;
import me.sunmc.clans.manager.ClanManager;
import me.sunmc.clans.manager.InviteManager;
import me.sunmc.clans.manager.RelationManager;
import me.sunmc.clans.placeholder.ClansPlaceholderExpansion;
import me.sunmc.clans.util.FoliaScheduler;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class RomClans extends JavaPlugin {

    private static RomClans instance;

    private ConfigManager configManager;
    private MessagesManager messagesManager;
    private DatabaseManager databaseManager;
    private RedisManager redisManager;
    private ClanCache clanCache;
    private NetworkPlayerTracker networkPlayerTracker;
    private ClanManager clanManager;
    private InviteManager inviteManager;
    private RelationManager relationManager;
    private ChatManager chatManager;
    private GUIManager guiManager;
    private FoliaScheduler foliaScheduler;
    private ExecutorService dbExecutor;

    public static RomClans getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        // Dedicated thread pool for all blocking DB I/O
        int threads = Math.max(2, Runtime.getRuntime().availableProcessors());
        dbExecutor = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "RomClans-DB");
            t.setDaemon(true);
            return t;
        });

        foliaScheduler = new FoliaScheduler(this);

        saveDefaultConfig();
        configManager = new ConfigManager(this);
        messagesManager = new MessagesManager(this);

        databaseManager = new DatabaseManager(this);
        if (!databaseManager.initialize()) {
            getLogger().severe("Failed to initialise database! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        redisManager = new RedisManager(this);
        if (configManager.isRedisEnabled()) {
            if (redisManager.initialize()) {
                // Ask other servers to re-broadcast their online players so
                // NetworkPlayerTracker is populated immediately after a restart.
                foliaScheduler.asyncDelayed(
                        () -> redisManager.publishRequestOnlinePlayers(),
                        1, TimeUnit.SECONDS
                );
            } else {
                getLogger().warning("Redis connection failed — cross-server sync disabled.");
            }
        }

        clanCache = new ClanCache();
        networkPlayerTracker = new NetworkPlayerTracker();
        clanManager = new ClanManager(this);
        inviteManager = new InviteManager(this);
        relationManager = new RelationManager(this);
        chatManager = new ChatManager(this);
        guiManager = new GUIManager();

        try {
            clanManager.loadAll().join();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to load clans from database!", e);
        }

        String alias = configManager.getCommandAlias();
        ClanCommand cmd = new ClanCommand(alias, this);
        getServer().getCommandMap().register(getName().toLowerCase(), cmd);

        ClanAdminCommand adminCmd = new ClanAdminCommand(this);
        getServer().getCommandMap().register(getName().toLowerCase(), adminCmd);

        var pm = getServer().getPluginManager();
        pm.registerEvents(guiManager, this);
        pm.registerEvents(new PlayerChatListener(this), this);
        pm.registerEvents(new PlayerJoinQuitListener(this), this);
        pm.registerEvents(new FriendlyFireListener(this), this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ClansPlaceholderExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        }

        getLogger().info("RomClans enabled. DB=" + configManager.getDatabaseType()
                + " Redis=" + configManager.isRedisEnabled());
    }

    @Override
    public void onDisable() {
        if (inviteManager != null) inviteManager.shutdown();
        if (chatManager != null) chatManager.shutdown();
        if (redisManager != null) redisManager.shutdown();
        if (databaseManager != null) databaseManager.shutdown();
        if (dbExecutor != null) dbExecutor.shutdownNow();
        getLogger().info("RomClans disabled.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessagesManager getMessagesManager() {
        return messagesManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public Database getDatabase() {
        return databaseManager.getDatabase();
    }

    public RedisManager getRedisManager() {
        return redisManager;
    }

    public ClanCache getClanCache() {
        return clanCache;
    }

    public NetworkPlayerTracker getNetworkPlayerTracker() {
        return networkPlayerTracker;
    }

    public ClanManager getClanManager() {
        return clanManager;
    }

    public InviteManager getInviteManager() {
        return inviteManager;
    }

    public RelationManager getRelationManager() {
        return relationManager;
    }

    public ChatManager getChatManager() {
        return chatManager;
    }

    public GUIManager getGuiManager() {
        return guiManager;
    }

    public FoliaScheduler getFoliaScheduler() {
        return foliaScheduler;
    }

    public ExecutorService getDbExecutor() {
        return dbExecutor;
    }
}