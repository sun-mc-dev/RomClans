package me.sunmc.clans.database.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.sunmc.clans.RomClans;
import me.sunmc.clans.config.ConfigManager;
import me.sunmc.clans.database.AbstractDatabase;
import me.sunmc.clans.model.RelationType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MySQLDatabase extends AbstractDatabase {

    public MySQLDatabase(RomClans plugin) {
        super(plugin);
    }

    @Override
    protected void configureHikari() {
        ConfigManager cfg = plugin.getConfigManager();
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?useSSL=%b&characterEncoding=utf8&serverTimezone=UTC",
                cfg.getMySQLHost(), cfg.getMySQLPort(), cfg.getMySQLDatabase(), cfg.getMySQLSSL()));
        hc.setUsername(cfg.getMySQLUser());
        hc.setPassword(cfg.getMySQLPassword());
        hc.setMaximumPoolSize(cfg.getMySQLPoolSize());
        hc.setPoolName("RomClans-MySQL");
        hc.addDataSourceProperty("cachePrepStmts", "true");
        hc.addDataSourceProperty("prepStmtCacheSize", "250");
        hc.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hc.addDataSourceProperty("useServerPrepStmts", "true");
        ds = new HikariDataSource(hc);
    }

    @Override
    protected String ddlClans() {
        return """
                CREATE TABLE IF NOT EXISTS clans (
                    id VARCHAR(36) NOT NULL PRIMARY KEY, name VARCHAR(32) NOT NULL UNIQUE,
                    tag VARCHAR(256) NOT NULL, leader_uuid VARCHAR(36) NOT NULL,
                    friendly_fire TINYINT(1) NOT NULL DEFAULT 0,
                    home_world VARCHAR(64), home_x DOUBLE NOT NULL DEFAULT 0,
                    home_y DOUBLE NOT NULL DEFAULT 0, home_z DOUBLE NOT NULL DEFAULT 0,
                    home_yaw FLOAT NOT NULL DEFAULT 0, home_pitch FLOAT NOT NULL DEFAULT 0,
                    home_set TINYINT(1) NOT NULL DEFAULT 0, home_server_id VARCHAR(64),
                    created_at BIGINT NOT NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""";
    }

    @Override
    protected String ddlMembers() {
        return """
                CREATE TABLE IF NOT EXISTS clan_members (
                    clan_id VARCHAR(36) NOT NULL, player_uuid VARCHAR(36) NOT NULL,
                    player_name VARCHAR(16) NOT NULL, rank VARCHAR(16) NOT NULL, joined_at BIGINT NOT NULL,
                    PRIMARY KEY (clan_id, player_uuid),
                    FOREIGN KEY (clan_id) REFERENCES clans(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""";
    }

    @Override
    protected String ddlRelations() {
        return """
                CREATE TABLE IF NOT EXISTS clan_relations (
                    clan_id VARCHAR(36) NOT NULL, target_clan_id VARCHAR(36) NOT NULL,
                    type VARCHAR(16) NOT NULL, created_at BIGINT NOT NULL,
                    PRIMARY KEY (clan_id, target_clan_id),
                    FOREIGN KEY (clan_id) REFERENCES clans(id) ON DELETE CASCADE,
                    FOREIGN KEY (target_clan_id) REFERENCES clans(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""";
    }

    @Override
    protected String ddlKnownPlayers() {
        return """
                CREATE TABLE IF NOT EXISTS known_players (
                    uuid VARCHAR(36) NOT NULL PRIMARY KEY,
                    name VARCHAR(16) NOT NULL,
                    last_seen BIGINT NOT NULL,
                    INDEX idx_kp_name (name)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""";
    }

    @Override
    protected String upsertPlayerSql() {
        return "INSERT INTO known_players(uuid,name,last_seen) VALUES(?,?,?) " +
                "ON DUPLICATE KEY UPDATE name=VALUES(name),last_seen=VALUES(last_seen)";
    }

    @Override
    public CompletableFuture<Void> insertRelation(UUID clanId, UUID targetId, RelationType type) {
        return asyncVoid(() -> {
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO clan_relations(clan_id,target_clan_id,type,created_at) VALUES(?,?,?,?) " +
                                 "ON DUPLICATE KEY UPDATE type=VALUES(type),created_at=VALUES(created_at)")) {
                ps.setString(1, clanId.toString());
                ps.setString(2, targetId.toString());
                ps.setString(3, type.name());
                ps.setLong(4, System.currentTimeMillis());
                ps.executeUpdate();
            }
        });
    }
}