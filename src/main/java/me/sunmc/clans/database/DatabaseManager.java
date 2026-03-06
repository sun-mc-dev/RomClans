package me.sunmc.clans.database;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.database.impl.MySQLDatabase;
import me.sunmc.clans.database.impl.SQLiteDatabase;

import java.util.logging.Level;

public class DatabaseManager {

    private final RomClans plugin;
    private Database database;

    public DatabaseManager(RomClans plugin) {
        this.plugin = plugin;
    }

    public boolean initialize() {
        String type = plugin.getConfigManager().getDatabaseType();
        database = type.equals("MYSQL") ? new MySQLDatabase(plugin) : new SQLiteDatabase(plugin);
        try {
            database.initialize().join();
            plugin.getLogger().info("Database initialised: " + type);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Database initialisation failed", e);
            return false;
        }
    }

    public void shutdown() {
        if (database != null) {
            try {
                database.close().join();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error closing database", e);
            }
        }
    }

    public Database getDatabase() {
        return database;
    }
}