package me.sunmc.clans.config;

import me.sunmc.clans.RomClans;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final RomClans plugin;
    private FileConfiguration cfg;

    public ConfigManager(RomClans plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        cfg = plugin.getConfig();
    }

    public String getCommandAlias() {
        return cfg.getString("command-alias", "clan");
    }

    public String getDatabaseType() {
        return cfg.getString("database.type", "SQLITE").toUpperCase();
    }

    public String getSQLiteFile() {
        return cfg.getString("database.sqlite.file", "clans.db");
    }

    public String getMySQLHost() {
        return cfg.getString("database.mysql.host", "localhost");
    }

    public int getMySQLPort() {
        return cfg.getInt("database.mysql.port", 3306);
    }

    public String getMySQLDatabase() {
        return cfg.getString("database.mysql.database", "romclans");
    }

    public String getMySQLUser() {
        return cfg.getString("database.mysql.username", "root");
    }

    public String getMySQLPassword() {
        return cfg.getString("database.mysql.password", "");
    }

    public int getMySQLPoolSize() {
        return cfg.getInt("database.mysql.pool-size", 10);
    }

    public boolean getMySQLSSL() {
        return cfg.getBoolean("database.mysql.use-ssl", false);
    }

    public boolean isRedisEnabled() {
        return cfg.getBoolean("redis.enabled", false);
    }

    public String getRedisHost() {
        return cfg.getString("redis.host", "localhost");
    }

    public int getRedisPort() {
        return cfg.getInt("redis.port", 6379);
    }

    public String getRedisPassword() {
        return cfg.getString("redis.password", "");
    }

    public int getRedisDatabase() {
        return cfg.getInt("redis.database", 0);
    }

    public String getServerId() {
        return cfg.getString("redis.server-id", "server-1");
    }

    public int getMaxNameLength() {
        return cfg.getInt("clans.max-name-length", 16);
    }

    public int getMinNameLength() {
        return cfg.getInt("clans.min-name-length", 3);
    }

    public int getMaxTagLength() {
        return cfg.getInt("clans.max-tag-length", 8);
    }

    public int getMinTagLength() {
        return cfg.getInt("clans.min-tag-length", 2);
    }

    public int getMaxMembers() {
        return cfg.getInt("clans.max-members", 50);
    }

    public int getMaxAllies() {
        return cfg.getInt("clans.max-allies", 5);
    }

    public int getMaxEnemies() {
        return cfg.getInt("clans.max-enemies", 10);
    }

    public int getHomeTeleportDelay() {
        return cfg.getInt("clans.home-teleport-delay", 3);
    }

    public boolean isFriendlyFireDefault() {
        return cfg.getBoolean("clans.friendly-fire-default", false);
    }

    public long getInviteExpiry() {
        return cfg.getLong("clans.invite-expiry", 300);
    }
}