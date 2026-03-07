package me.sunmc.clans.database;

import com.zaxxer.hikari.HikariDataSource;
import me.sunmc.clans.RomClans;
import me.sunmc.clans.model.Clan;
import me.sunmc.clans.model.ClanMember;
import me.sunmc.clans.model.ClanRank;
import me.sunmc.clans.model.RelationType;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractDatabase implements Database {

    protected final RomClans plugin;
    protected HikariDataSource ds;

    protected AbstractDatabase(RomClans plugin) {
        this.plugin = plugin;
    }

    protected abstract void configureHikari() throws Exception;

    protected abstract String ddlClans();

    protected abstract String ddlMembers();

    protected abstract String ddlRelations();

    protected abstract String ddlKnownPlayers();

    /**
     * SQL for upsert into known_players(uuid, name, last_seen). Dialect-specific.
     */
    protected abstract String upsertPlayerSql();

    protected <T> CompletableFuture<T> async(ThrowingSupplier<T> s) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return s.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, plugin.getDbExecutor());
    }

    protected CompletableFuture<Void> asyncVoid(ThrowingRunnable r) {
        return CompletableFuture.runAsync(() -> {
            try {
                r.run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, plugin.getDbExecutor());
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return asyncVoid(() -> {
            configureHikari();
            try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
                st.execute(ddlClans());
                st.execute(ddlMembers());
                st.execute(ddlRelations());
                st.execute(ddlKnownPlayers());
                runMigrations(c);
            }
        });
    }

    /**
     * Adds columns introduced after the initial schema. Failures are silently ignored (column exists).
     */
    protected void runMigrations(Connection c) {
        String[] stmts = {
                "ALTER TABLE clans ADD COLUMN home_server_id TEXT"
        };
        for (String sql : stmts) {
            try (Statement st = c.createStatement()) {
                st.execute(sql);
            } catch (SQLException ignored) {
            }
        }
    }

    @Override
    public CompletableFuture<Void> close() {
        return asyncVoid(() -> {
            if (ds != null && !ds.isClosed()) ds.close();
        });
    }

    @Override
    public CompletableFuture<Void> insertClan(Clan c) {
        return asyncVoid(() -> {
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO clans(id,name,tag,leader_uuid,friendly_fire,home_set,created_at) VALUES(?,?,?,?,?,?,?)")) {
                ps.setString(1, c.getId().toString());
                ps.setString(2, c.getName());
                ps.setString(3, c.getTag());
                ps.setString(4, c.getLeaderUuid().toString());
                ps.setBoolean(5, c.isFriendlyFire());
                ps.setBoolean(6, false);
                ps.setLong(7, c.getCreatedAt());
                ps.executeUpdate();
            }
        });
    }

    @Override
    public CompletableFuture<Optional<Clan>> findClanById(UUID id) {
        return async(() -> {
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM clans WHERE id=?")) {
                ps.setString(1, id.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapClan(rs)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public CompletableFuture<Optional<Clan>> findClanByName(String name) {
        return async(() -> {
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM clans WHERE name=?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapClan(rs)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public CompletableFuture<Optional<Clan>> findClanByPlayer(UUID playerUuid) {
        return async(() -> {
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT c.* FROM clans c JOIN clan_members m ON c.id=m.clan_id WHERE m.player_uuid=?")) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapClan(rs)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public CompletableFuture<List<Clan>> findAllClans() {
        return async(() -> {
            List<Clan> list = new ArrayList<>();
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM clans");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapClan(rs));
            }
            for (Clan clan : list) {
                loadMembers(clan);
                loadRelations(clan);
            }
            return list;
        });
    }

    @Override
    public CompletableFuture<Void> updateClan(Clan c) {
        return asyncVoid(() -> {
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE clans SET name=?,tag=?,leader_uuid=?,friendly_fire=? WHERE id=?")) {
                ps.setString(1, c.getName());
                ps.setString(2, c.getTag());
                ps.setString(3, c.getLeaderUuid().toString());
                ps.setBoolean(4, c.isFriendlyFire());
                ps.setString(5, c.getId().toString());
                ps.executeUpdate();
            }
        });
    }

    @Override
    public CompletableFuture<Void> updateClanHome(UUID clanId, Location loc, String serverId) {
        return asyncVoid(() -> {
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE clans SET home_world=?,home_x=?,home_y=?,home_z=?,home_yaw=?,home_pitch=?,home_set=1,home_server_id=? WHERE id=?")) {
                ps.setString(1, loc.getWorld().getName());
                ps.setDouble(2, loc.getX());
                ps.setDouble(3, loc.getY());
                ps.setDouble(4, loc.getZ());
                ps.setFloat(5, loc.getYaw());
                ps.setFloat(6, loc.getPitch());
                ps.setString(7, serverId);
                ps.setString(8, clanId.toString());
                ps.executeUpdate();
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteClan(UUID clanId) {
        return asyncVoid(() -> {
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM clans WHERE id=?")) {
                ps.setString(1, clanId.toString());
                ps.executeUpdate();
            }
        });
    }

    @Override
    public CompletableFuture<Void> insertMember(UUID clanId, ClanMember m) {
        return asyncVoid(() -> {
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO clan_members(clan_id,player_uuid,player_name,rank,joined_at) VALUES(?,?,?,?,?)")) {
                ps.setString(1, clanId.toString());
                ps.setString(2, m.getPlayerUuid().toString());
                ps.setString(3, m.getPlayerName());
                ps.setString(4, m.getRank().name());
                ps.setLong(5, m.getJoinedAt());
                ps.executeUpdate();
            }
        });
    }

    @Override
    public CompletableFuture<Void> updateMember(UUID clanId, ClanMember m) {
        return asyncVoid(() -> {
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE clan_members SET rank=?,player_name=? WHERE clan_id=? AND player_uuid=?")) {
                ps.setString(1, m.getRank().name());
                ps.setString(2, m.getPlayerName());
                ps.setString(3, clanId.toString());
                ps.setString(4, m.getPlayerUuid().toString());
                ps.executeUpdate();
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteMember(UUID clanId, UUID playerUuid) {
        return asyncVoid(() -> {
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "DELETE FROM clan_members WHERE clan_id=? AND player_uuid=?")) {
                ps.setString(1, clanId.toString());
                ps.setString(2, playerUuid.toString());
                ps.executeUpdate();
            }
        });
    }

    @Override
    public CompletableFuture<Void> insertRelation(UUID clanId, UUID targetId, RelationType type) {
        return asyncVoid(() -> {
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT OR REPLACE INTO clan_relations(clan_id,target_clan_id,type,created_at) VALUES(?,?,?,?)")) {
                ps.setString(1, clanId.toString());
                ps.setString(2, targetId.toString());
                ps.setString(3, type.name());
                ps.setLong(4, System.currentTimeMillis());
                ps.executeUpdate();
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteRelation(UUID clanId, UUID targetId) {
        return asyncVoid(() -> {
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "DELETE FROM clan_relations WHERE clan_id=? AND target_clan_id=?")) {
                ps.setString(1, clanId.toString());
                ps.setString(2, targetId.toString());
                ps.executeUpdate();
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteAllRelationsForClan(UUID clanId) {
        return asyncVoid(() -> {
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "DELETE FROM clan_relations WHERE clan_id=? OR target_clan_id=?")) {
                ps.setString(1, clanId.toString());
                ps.setString(2, clanId.toString());
                ps.executeUpdate();
            }
        });
    }

    @Override
    public CompletableFuture<Void> upsertPlayer(UUID uuid, String name) {
        return asyncVoid(() -> {
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(upsertPlayerSql())) {
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.setLong(3, System.currentTimeMillis());
                ps.executeUpdate();
            }
        });
    }

    @Override
    public CompletableFuture<Optional<UUID>> findPlayerUuidByName(String name) {
        return async(() -> {
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT uuid FROM known_players WHERE LOWER(name)=LOWER(?)")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(UUID.fromString(rs.getString("uuid"))) : Optional.empty();
                }
            }
        });
    }

    private @NotNull Clan mapClan(@NotNull ResultSet rs) throws SQLException {
        Clan c = new Clan(
                UUID.fromString(rs.getString("id")), rs.getString("name"), rs.getString("tag"),
                UUID.fromString(rs.getString("leader_uuid")), rs.getBoolean("friendly_fire"),
                rs.getLong("created_at"));
        c.setHomeSet(rs.getBoolean("home_set"));
        if (c.isHomeSet()) {
            c.setHomeWorld(rs.getString("home_world"));
            c.setHomeX(rs.getDouble("home_x"));
            c.setHomeY(rs.getDouble("home_y"));
            c.setHomeZ(rs.getDouble("home_z"));
            c.setHomeYaw(rs.getFloat("home_yaw"));
            c.setHomePitch(rs.getFloat("home_pitch"));
            c.setHomeServerId(rs.getString("home_server_id"));
        }
        return c;
    }

    private void loadMembers(@NotNull Clan clan) throws SQLException {
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM clan_members WHERE clan_id=?")) {
            ps.setString(1, clan.getId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) clan.addMember(new ClanMember(
                        UUID.fromString(rs.getString("player_uuid")), rs.getString("player_name"),
                        ClanRank.valueOf(rs.getString("rank")), rs.getLong("joined_at")));
            }
        }
    }

    private void loadRelations(@NotNull Clan clan) throws SQLException {
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM clan_relations WHERE clan_id=?")) {
            ps.setString(1, clan.getId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID target = UUID.fromString(rs.getString("target_clan_id"));
                    switch (me.sunmc.clans.model.RelationType.valueOf(rs.getString("type"))) {
                        case ALLY -> clan.addAlly(target);
                        case ENEMY -> clan.addEnemy(target);
                        case ALLY_REQUEST -> clan.addPendingAllyReq(target);
                    }
                }
            }
        }
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT clan_id FROM clan_relations WHERE target_clan_id=? AND type='ALLY_REQUEST'")) {
            ps.setString(1, clan.getId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) clan.addPendingAllyReq(UUID.fromString(rs.getString("clan_id")));
            }
        }
    }

    @FunctionalInterface
    protected interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    protected interface ThrowingRunnable {
        void run() throws Exception;
    }
}