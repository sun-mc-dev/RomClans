package me.sunmc.clans.manager;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.model.Clan;
import me.sunmc.clans.model.ClanMember;
import me.sunmc.clans.model.ClanRank;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ClanManager {

    private final RomClans plugin;

    public ClanManager(RomClans plugin) {
        this.plugin = plugin;
    }

    /**
     * Load all clans + members + relations from DB into cache.
     */
    public CompletableFuture<Void> loadAll() {
        return plugin.getDatabase().findAllClans().thenAccept(clans -> {
            plugin.getClanCache().clear();
            clans.forEach(plugin.getClanCache()::add);
            plugin.getLogger().info("Loaded " + clans.size() + " clan(s) into cache.");
        });
    }

    public Clan getPlayerClan(UUID playerUuid) {
        return plugin.getClanCache().getByPlayer(playerUuid);
    }

    public Clan getClanByName(String name) {
        return plugin.getClanCache().getByName(name);
    }

    public Clan getClanById(UUID id) {
        return plugin.getClanCache().getById(id);
    }

    /**
     * Create a new clan; updates cache and DB.
     */
    public CompletableFuture<Clan> createClan(String name, String tag, UUID leaderUuid, String leaderName) {
        Clan clan = new Clan(
                UUID.randomUUID(), name, tag, leaderUuid,
                plugin.getConfigManager().isFriendlyFireDefault(), System.currentTimeMillis()
        );
        ClanMember leader = new ClanMember(leaderUuid, leaderName, ClanRank.LEADER, System.currentTimeMillis());
        clan.addMember(leader);

        return plugin.getDatabase().insertClan(clan)
                .thenCompose(v -> plugin.getDatabase().insertMember(clan.getId(), leader))
                .thenApply(v -> {
                    plugin.getClanCache().add(clan);
                    return clan;
                });
    }

    /**
     * Disband a clan; removes from cache and DB.
     */
    public CompletableFuture<Void> disbandClan(@NotNull Clan clan) {
        plugin.getClanCache().remove(clan.getId());
        // Notify RelationManager to clean up ally/enemy references from other clans
        plugin.getClanCache().getAll().forEach(other -> {
            other.removeAlly(clan.getId());
            other.removeEnemy(clan.getId());
            other.removePendingAllyReq(clan.getId());
        });
        return plugin.getDatabase().deleteAllRelationsForClan(clan.getId())
                .thenCompose(v -> plugin.getDatabase().deleteClan(clan.getId()));
    }

    /**
     * Add a player to a clan.
     */
    public CompletableFuture<Void> addMember(@NotNull Clan clan, UUID playerUuid, String playerName) {
        ClanMember member = new ClanMember(playerUuid, playerName, ClanRank.MEMBER, System.currentTimeMillis());
        clan.addMember(member);
        plugin.getClanCache().addPlayerToIndex(playerUuid, clan.getId());
        return plugin.getDatabase().insertMember(clan.getId(), member);
    }

    /**
     * Remove a player from a clan.
     */
    public CompletableFuture<Void> removeMember(@NotNull Clan clan, UUID playerUuid) {
        clan.removeMember(playerUuid);
        plugin.getClanCache().removePlayerFromIndex(playerUuid);
        return plugin.getDatabase().deleteMember(clan.getId(), playerUuid);
    }

    /**
     * Update a member's rank in cache and DB.
     */
    public CompletableFuture<Void> updateMemberRank(@NotNull Clan clan, UUID playerUuid, ClanRank newRank) {
        ClanMember m = clan.getMember(playerUuid);
        if (m == null) return CompletableFuture.completedFuture(null);
        m.setRank(newRank);
        return plugin.getDatabase().updateMember(clan.getId(), m);
    }

    /**
     * Toggle friendly fire.
     */
    public CompletableFuture<Void> toggleFriendlyFire(@NotNull Clan clan) {
        clan.setFriendlyFire(!clan.isFriendlyFire());
        return plugin.getDatabase().updateClan(clan);
    }

    /**
     * Transfer leadership.
     */
    public CompletableFuture<Void> transferLeadership(@NotNull Clan clan, UUID newLeader) {
        UUID oldLeader = clan.getLeaderUuid();
        // Demote old leader to CO_LEADER
        ClanMember oldLeaderMember = clan.getMember(oldLeader);
        if (oldLeaderMember != null) oldLeaderMember.setRank(ClanRank.CO_LEADER);
        // Promote new leader
        ClanMember newLeaderMember = clan.getMember(newLeader);
        if (newLeaderMember != null) newLeaderMember.setRank(ClanRank.LEADER);
        clan.setLeaderUuid(newLeader);

        return plugin.getDatabase().updateClan(clan)
                .thenCompose(v -> oldLeaderMember != null
                        ? plugin.getDatabase().updateMember(clan.getId(), oldLeaderMember)
                        : CompletableFuture.completedFuture(null))
                .thenCompose(v -> newLeaderMember != null
                        ? plugin.getDatabase().updateMember(clan.getId(), newLeaderMember)
                        : CompletableFuture.completedFuture(null));
    }

    public CompletableFuture<Void> retag(@NotNull Clan clan, String newTag) {
        clan.setTag(newTag);
        return plugin.getDatabase().updateClan(clan);
    }
}