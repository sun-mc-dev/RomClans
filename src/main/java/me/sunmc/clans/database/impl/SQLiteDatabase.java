package me.sunmc.clans.database.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.sunmc.clans.RomClans;
import me.sunmc.clans.database.AbstractDatabase;

import java.io.File;

public class SQLiteDatabase extends AbstractDatabase {

    public SQLiteDatabase(RomClans plugin) {
        super(plugin);
    }

    @Override
    protected void configureHikari() {
        File dbFile = new File(plugin.getDataFolder(), plugin.getConfigManager().getSQLiteFile());
        dbFile.getParentFile().mkdirs();

        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        hc.setDriverClassName("org.sqlite.JDBC");
        hc.setMaximumPoolSize(1);   // SQLite is single-writer
        hc.setPoolName("RomClans-SQLite");
        hc.addDataSourceProperty("foreign_keys", "true");
        hc.addDataSourceProperty("journal_mode", "WAL");
        hc.addDataSourceProperty("synchronous", "NORMAL");
        ds = new HikariDataSource(hc);
    }

    @Override
    protected String ddlClans() {
        return """
                CREATE TABLE IF NOT EXISTS clans (
                    id          TEXT NOT NULL PRIMARY KEY,
                    name        TEXT NOT NULL UNIQUE,
                    tag         TEXT NOT NULL,
                    leader_uuid TEXT NOT NULL,
                    friendly_fire INTEGER NOT NULL DEFAULT 0,
                    home_world  TEXT,
                    home_x      REAL NOT NULL DEFAULT 0,
                    home_y      REAL NOT NULL DEFAULT 0,
                    home_z      REAL NOT NULL DEFAULT 0,
                    home_yaw    REAL NOT NULL DEFAULT 0,
                    home_pitch  REAL NOT NULL DEFAULT 0,
                    home_set    INTEGER NOT NULL DEFAULT 0,
                    created_at  INTEGER NOT NULL
                )""";
    }

    @Override
    protected String ddlMembers() {
        return """
                CREATE TABLE IF NOT EXISTS clan_members (
                    clan_id     TEXT NOT NULL,
                    player_uuid TEXT NOT NULL,
                    player_name TEXT NOT NULL,
                    rank        TEXT NOT NULL,
                    joined_at   INTEGER NOT NULL,
                    PRIMARY KEY (clan_id, player_uuid),
                    FOREIGN KEY (clan_id) REFERENCES clans(id) ON DELETE CASCADE
                )""";
    }

    @Override
    protected String ddlRelations() {
        return """
                CREATE TABLE IF NOT EXISTS clan_relations (
                    clan_id        TEXT NOT NULL,
                    target_clan_id TEXT NOT NULL,
                    type           TEXT NOT NULL,
                    created_at     INTEGER NOT NULL,
                    PRIMARY KEY (clan_id, target_clan_id),
                    FOREIGN KEY (clan_id)        REFERENCES clans(id) ON DELETE CASCADE,
                    FOREIGN KEY (target_clan_id) REFERENCES clans(id) ON DELETE CASCADE
                )""";
    }
}