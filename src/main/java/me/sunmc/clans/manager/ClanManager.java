package me.sunmc.clans.manager;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.model.Clan;
import me.sunmc.clans.model.ClanMember;
import me.sunmc.clans.model.ClanRank;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ClanManager {

    private final RomClans plugin;

    public ClanManager(RomClans plugin) {
        this.plugin = plugin;
    }

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

    public CompletableFuture<Clan> createClan(String name, String tag, UUID leaderUuid, String leaderName) {
        Clan clan = new Clan(UUID.randomUUID(), name, tag, leaderUuid,
                plugin.getConfigManager().isFriendlyFireDefault(), System.currentTimeMillis());
        ClanMember leader = new ClanMember(leaderUuid, leaderName, ClanRank.LEADER, System.currentTimeMillis());
        clan.addMember(leader);
        return plugin.getDatabase().insertClan(clan)
                .thenCompose(v -> plugin.getDatabase().insertMember(clan.getId(), leader))
                .thenApply(v -> {
                    plugin.getClanCache().add(clan);
                    // Broadcast to other servers, so they pick up the new clan
                    if (plugin.getRedisManager().isActive())
                        plugin.getRedisManager().publishCacheInvalidate(clan.getId().toString());
                    return clan;
                });
    }

    public CompletableFuture<Void> disbandClan(@NotNull Clan clan) {
        // Reset chat modes for members on this server before removing from cache
        clan.getMembers().keySet().forEach(plugin.getChatManager()::resetMode);
        plugin.getClanCache().remove(clan.getId());
        plugin.getClanCache().getAll().forEach(other -> {
            other.removeAlly(clan.getId());
            other.removeEnemy(clan.getId());
            other.removePendingAllyReq(clan.getId());
        });
        if (plugin.getRedisManager().isActive())
            plugin.getRedisManager().publishDisband(clan.getId().toString());
        return plugin.getDatabase().deleteAllRelationsForClan(clan.getId())
                .thenCompose(v -> plugin.getDatabase().deleteClan(clan.getId()));
    }

    public CompletableFuture<Void> addMember(@NotNull Clan clan, UUID playerUuid, String playerName) {
        ClanMember member = new ClanMember(playerUuid, playerName, ClanRank.MEMBER, System.currentTimeMillis());
        clan.addMember(member);
        plugin.getClanCache().addPlayerToIndex(playerUuid, clan.getId());
        // Clear any pending invite so the officer can re-invite if the player leaves again
        plugin.getInviteManager().clearInvitesFromClan(playerUuid, clan.getName());
        return plugin.getDatabase().insertMember(clan.getId(), member).thenRun(() -> {
            if (plugin.getRedisManager().isActive())
                plugin.getRedisManager().publishMemberAdd(clan.getId().toString(),
                        playerUuid.toString(), playerName, ClanRank.MEMBER.name());
        });
    }

    public CompletableFuture<Void> removeMember(@NotNull Clan clan, UUID playerUuid) {
        clan.removeMember(playerUuid);
        plugin.getClanCache().removePlayerFromIndex(playerUuid);
        return plugin.getDatabase().deleteMember(clan.getId(), playerUuid).thenRun(() -> {
            if (plugin.getRedisManager().isActive())
                plugin.getRedisManager().publishMemberRemove(clan.getId().toString(), playerUuid.toString());
        });
    }

    public CompletableFuture<Void> updateMemberRank(@NotNull Clan clan, UUID playerUuid, ClanRank newRank) {
        ClanMember m = clan.getMember(playerUuid);
        if (m == null) return CompletableFuture.completedFuture(null);
        m.setRank(newRank);
        return plugin.getDatabase().updateMember(clan.getId(), m).thenRun(() -> {
            if (plugin.getRedisManager().isActive())
                plugin.getRedisManager().publishRankUpdate(clan.getId().toString(),
                        playerUuid.toString(), newRank.name());
        });
    }

    public CompletableFuture<Void> toggleFriendlyFire(@NotNull Clan clan) {
        clan.setFriendlyFire(!clan.isFriendlyFire());
        return plugin.getDatabase().updateClan(clan).thenRun(() -> {
            if (plugin.getRedisManager().isActive())
                plugin.getRedisManager().publishFriendlyFireToggle(clan.getId().toString(), clan.isFriendlyFire());
        });
    }

    public CompletableFuture<Void> transferLeadership(@NotNull Clan clan, UUID newLeader) {
        UUID oldLeader = clan.getLeaderUuid();
        ClanMember om = clan.getMember(oldLeader);
        if (om != null) om.setRank(ClanRank.CO_LEADER);
        ClanMember nm = clan.getMember(newLeader);
        if (nm != null) nm.setRank(ClanRank.LEADER);
        clan.setLeaderUuid(newLeader);
        return plugin.getDatabase().updateClan(clan)
                .thenCompose(v -> om != null ? plugin.getDatabase().updateMember(clan.getId(), om) : CompletableFuture.completedFuture(null))
                .thenCompose(v -> nm != null ? plugin.getDatabase().updateMember(clan.getId(), nm) : CompletableFuture.completedFuture(null))
                .thenRun(() -> {
                    if (plugin.getRedisManager().isActive())
                        plugin.getRedisManager().publishTransfer(clan.getId().toString(),
                                newLeader.toString(), oldLeader.toString());
                });
    }

    public CompletableFuture<Void> retag(@NotNull Clan clan, String newTag) {
        clan.setTag(newTag);
        return plugin.getDatabase().updateClan(clan).thenRun(() -> {
            if (plugin.getRedisManager().isActive())
                plugin.getRedisManager().publishRetag(clan.getId().toString(), newTag);
        });
    }

    public CompletableFuture<Void> setHome(@NotNull Clan clan, @NotNull Location loc) {
        clan.setHome(loc);
        clan.setHomeServerId(plugin.getConfigManager().getServerId());
        String serverId = plugin.getConfigManager().getServerId();
        return plugin.getDatabase().updateClanHome(clan.getId(), loc, serverId).thenRun(() -> {
            if (plugin.getRedisManager().isActive())
                plugin.getRedisManager().publishHomeSet(clan.getId().toString(),
                        loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(),
                        loc.getYaw(), loc.getPitch());
        });
    }
}