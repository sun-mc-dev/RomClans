package me.sunmc.clans.manager;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.api.event.*;
import me.sunmc.clans.model.Clan;
import me.sunmc.clans.model.ClanMember;
import me.sunmc.clans.model.ClanRank;
import me.sunmc.clans.util.EventUtil;
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

    public CompletableFuture<Clan> createClan(String name, String tag,
                                              UUID leaderUuid, String leaderName) {
        Clan clan = new Clan(UUID.randomUUID(), name, tag, leaderUuid,
                plugin.getConfigManager().isFriendlyFireDefault(), System.currentTimeMillis());
        ClanMember leader = new ClanMember(leaderUuid, leaderName, ClanRank.LEADER,
                System.currentTimeMillis());
        clan.addMember(leader);

        return EventUtil.call(plugin, new ClanCreateEvent(clan, leaderUuid, leaderName))
                .thenCompose(ev -> {
                    if (ev.isCancelled())
                        return CompletableFuture.failedFuture(
                                new IllegalStateException("Clan creation was cancelled by a plugin."));
                    return plugin.getDatabase().insertClan(clan)
                            .thenCompose(v -> plugin.getDatabase().insertMember(clan.getId(), leader))
                            .thenApply(v -> {
                                plugin.getClanCache().add(clan);
                                if (plugin.getRedisManager().isActive())
                                    plugin.getRedisManager().publishCacheInvalidate(
                                            clan.getId().toString());
                                return clan;
                            });
                });
    }

    /**
     * Disband triggered by the leader (adminDisband = false).
     */
    public CompletableFuture<Void> disbandClan(@NotNull Clan clan) {
        return disbandClan(clan, false);
    }

    /**
     * @param adminDisband pass {@code true} when called from /clanadmin disband.
     */
    public CompletableFuture<Void> disbandClan(@NotNull Clan clan, boolean adminDisband) {
        return EventUtil.call(plugin, new ClanDisbandEvent(clan, adminDisband))
                .thenCompose(ev -> {
                    if (ev.isCancelled()) return CompletableFuture.completedFuture(null);
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
                });
    }

    public CompletableFuture<Void> addMember(@NotNull Clan clan,
                                             UUID playerUuid, String playerName) {
        return EventUtil.call(plugin, new ClanMemberJoinEvent(clan, playerUuid, playerName))
                .thenCompose(ev -> {
                    if (ev.isCancelled())
                        return CompletableFuture.failedFuture(
                                new IllegalStateException("Member join was cancelled by a plugin."));
                    ClanMember member = new ClanMember(playerUuid, playerName,
                            ClanRank.MEMBER, System.currentTimeMillis());
                    clan.addMember(member);
                    plugin.getClanCache().addPlayerToIndex(playerUuid, clan.getId());
                    plugin.getInviteManager().clearInvitesFromClan(playerUuid, clan.getName());
                    return plugin.getDatabase().insertMember(clan.getId(), member).thenRun(() -> {
                        if (plugin.getRedisManager().isActive())
                            plugin.getRedisManager().publishMemberAdd(clan.getId().toString(),
                                    playerUuid.toString(), playerName, ClanRank.MEMBER.name());
                    });
                });
    }

    /**
     * Backward-compatible overload (defaults to {@link ClanMemberLeaveEvent.Reason#LEFT}).
     */
    public CompletableFuture<Void> removeMember(@NotNull Clan clan, UUID playerUuid) {
        return removeMember(clan, playerUuid, ClanMemberLeaveEvent.Reason.LEFT);
    }

    public CompletableFuture<Void> removeMember(@NotNull Clan clan, UUID playerUuid,
                                                @NotNull ClanMemberLeaveEvent.Reason reason) {
        ClanMember m = clan.getMember(playerUuid);
        String name = m != null ? m.getPlayerName() : "Unknown";
        return EventUtil.call(plugin, new ClanMemberLeaveEvent(clan, playerUuid, name, reason))
                .thenCompose(ev -> {
                    if (ev.isCancelled())
                        return CompletableFuture.failedFuture(
                                new IllegalStateException("Member removal was cancelled by a plugin."));
                    clan.removeMember(playerUuid);
                    plugin.getClanCache().removePlayerFromIndex(playerUuid);
                    return plugin.getDatabase().deleteMember(clan.getId(), playerUuid).thenRun(() -> {
                        if (plugin.getRedisManager().isActive())
                            plugin.getRedisManager().publishMemberRemove(
                                    clan.getId().toString(), playerUuid.toString());
                    });
                });
    }

    public CompletableFuture<Void> updateMemberRank(@NotNull Clan clan,
                                                    UUID playerUuid, ClanRank newRank) {
        ClanMember m = clan.getMember(playerUuid);
        if (m == null) return CompletableFuture.completedFuture(null);
        ClanRank old = m.getRank();
        return EventUtil.call(plugin,
                        new ClanRankChangeEvent(clan, playerUuid, m.getPlayerName(), old, newRank))
                .thenCompose(ev -> {
                    if (ev.isCancelled())
                        return CompletableFuture.failedFuture(
                                new IllegalStateException("Rank change was cancelled by a plugin."));
                    m.setRank(newRank);
                    return plugin.getDatabase().updateMember(clan.getId(), m).thenRun(() -> {
                        if (plugin.getRedisManager().isActive())
                            plugin.getRedisManager().publishRankUpdate(clan.getId().toString(),
                                    playerUuid.toString(), newRank.name());
                    });
                });
    }

    public CompletableFuture<Void> toggleFriendlyFire(@NotNull Clan clan) {
        boolean next = !clan.isFriendlyFire();
        return EventUtil.call(plugin, new ClanFriendlyFireToggleEvent(clan, next))
                .thenCompose(ev -> {
                    if (ev.isCancelled()) return CompletableFuture.completedFuture(null);
                    clan.setFriendlyFire(next);
                    return plugin.getDatabase().updateClan(clan).thenRun(() -> {
                        if (plugin.getRedisManager().isActive())
                            plugin.getRedisManager().publishFriendlyFireToggle(
                                    clan.getId().toString(), clan.isFriendlyFire());
                    });
                });
    }

    public CompletableFuture<Void> transferLeadership(@NotNull Clan clan, UUID newLeader) {
        UUID oldLeader = clan.getLeaderUuid();
        return EventUtil.call(plugin, new ClanLeaderTransferEvent(clan, oldLeader, newLeader))
                .thenCompose(ev -> {
                    if (ev.isCancelled())
                        return CompletableFuture.failedFuture(
                                new IllegalStateException("Leadership transfer was cancelled by a plugin."));
                    ClanMember om = clan.getMember(oldLeader);
                    if (om != null) om.setRank(ClanRank.CO_LEADER);
                    ClanMember nm = clan.getMember(newLeader);
                    if (nm != null) nm.setRank(ClanRank.LEADER);
                    clan.setLeaderUuid(newLeader);
                    return plugin.getDatabase().updateClan(clan)
                            .thenCompose(v -> om != null
                                    ? plugin.getDatabase().updateMember(clan.getId(), om)
                                    : CompletableFuture.completedFuture(null))
                            .thenCompose(v -> nm != null
                                    ? plugin.getDatabase().updateMember(clan.getId(), nm)
                                    : CompletableFuture.completedFuture(null))
                            .thenRun(() -> {
                                if (plugin.getRedisManager().isActive())
                                    plugin.getRedisManager().publishTransfer(
                                            clan.getId().toString(),
                                            newLeader.toString(), oldLeader.toString());
                            });
                });
    }

    public CompletableFuture<Void> retag(@NotNull Clan clan, String newTag) {
        String oldTag = clan.getTag();
        return EventUtil.call(plugin, new ClanTagChangeEvent(clan, oldTag, newTag))
                .thenCompose(ev -> {
                    if (ev.isCancelled())
                        return CompletableFuture.failedFuture(
                                new IllegalStateException("Tag change was cancelled by a plugin."));
                    String tag = ev.getNewTag(); // allow event handler to override
                    clan.setTag(tag);
                    return plugin.getDatabase().updateClan(clan).thenRun(() -> {
                        if (plugin.getRedisManager().isActive())
                            plugin.getRedisManager().publishRetag(clan.getId().toString(), tag);
                    });
                });
    }

    public CompletableFuture<Void> setHome(@NotNull Clan clan, @NotNull Location loc) {
        return EventUtil.call(plugin, new ClanHomeSetEvent(clan, loc.clone()))
                .thenCompose(ev -> {
                    if (ev.isCancelled())
                        return CompletableFuture.failedFuture(
                                new IllegalStateException("Home set was cancelled by a plugin."));
                    Location dest = ev.getNewHome(); // allow event handler to override
                    clan.setHome(dest);
                    clan.setHomeServerId(plugin.getConfigManager().getServerId());
                    String sid = plugin.getConfigManager().getServerId();
                    return plugin.getDatabase().updateClanHome(clan.getId(), dest, sid).thenRun(() -> {
                        if (plugin.getRedisManager().isActive())
                            plugin.getRedisManager().publishHomeSet(clan.getId().toString(),
                                    dest.getWorld().getName(),
                                    dest.getX(), dest.getY(), dest.getZ(),
                                    dest.getYaw(), dest.getPitch());
                    });
                });
    }
}