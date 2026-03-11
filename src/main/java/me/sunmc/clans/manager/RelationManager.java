package me.sunmc.clans.manager;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.api.event.ClanAllyEvent;
import me.sunmc.clans.api.event.ClanEnemyEvent;
import me.sunmc.clans.model.Clan;
import me.sunmc.clans.model.RelationType;
import me.sunmc.clans.util.EventUtil;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RelationManager {

    private final RomClans plugin;

    public RelationManager(RomClans plugin) {
        this.plugin = plugin;
    }

    public boolean areAllies(UUID a, UUID b) {
        Clan ca = plugin.getClanCache().getById(a);
        return ca != null && ca.isAlly(b);
    }

    public boolean areEnemies(UUID a, UUID b) {
        Clan ca = plugin.getClanCache().getById(a);
        return ca != null && ca.isEnemy(b);
    }

    public CompletableFuture<Void> sendAllyRequest(@NotNull Clan requester, @NotNull Clan target) {
        return EventUtil.call(plugin,
                        new ClanAllyEvent(requester, target, ClanAllyEvent.Action.REQUEST))
                .thenCompose(ev -> {
                    if (ev.isCancelled())
                        return CompletableFuture.failedFuture(
                                new IllegalStateException("Ally request was cancelled by a plugin."));
                    target.addPendingAllyReq(requester.getId());
                    return plugin.getDatabase()
                            .insertRelation(requester.getId(), target.getId(),
                                    RelationType.ALLY_REQUEST)
                            .thenRun(() -> {
                                if (plugin.getRedisManager().isActive())
                                    plugin.getRedisManager().publishAllyRequest(
                                            requester.getId().toString(),
                                            requester.getName(),
                                            target.getId().toString());
                            });
                });
    }

    public CompletableFuture<Void> acceptAlliance(@NotNull Clan requester, @NotNull Clan acceptor) {
        return EventUtil.call(plugin,
                        new ClanAllyEvent(acceptor, requester, ClanAllyEvent.Action.ACCEPT))
                .thenCompose(ev -> {
                    if (ev.isCancelled())
                        return CompletableFuture.failedFuture(
                                new IllegalStateException("Alliance acceptance was cancelled by a plugin."));
                    acceptor.removePendingAllyReq(requester.getId());
                    requester.addAlly(acceptor.getId());
                    acceptor.addAlly(requester.getId());
                    return plugin.getDatabase()
                            .deleteRelation(requester.getId(), acceptor.getId())
                            .thenCompose(v -> plugin.getDatabase().insertRelation(
                                    requester.getId(), acceptor.getId(), RelationType.ALLY))
                            .thenCompose(v -> plugin.getDatabase().insertRelation(
                                    acceptor.getId(), requester.getId(), RelationType.ALLY))
                            .thenRun(() -> {
                                if (plugin.getRedisManager().isActive()) {
                                    plugin.getRedisManager().publishRelationUpdate(
                                            requester.getId().toString(),
                                            acceptor.getId().toString(), "ALLY", "ADD");
                                    plugin.getRedisManager().publishRelationUpdate(
                                            acceptor.getId().toString(),
                                            requester.getId().toString(), "ALLY", "ADD");
                                    plugin.getRedisManager().publishAllyAccept(
                                            requester.getId().toString(), acceptor.getName());
                                }
                            });
                });
    }

    /**
     * Denial is informational; the event fires but cancellation is ignored.
     */
    public CompletableFuture<Void> denyAlliance(@NotNull Clan requester, @NotNull Clan denier) {
        return EventUtil.call(plugin,
                        new ClanAllyEvent(denier, requester, ClanAllyEvent.Action.DENY))
                .thenCompose(ev -> {
                    denier.removePendingAllyReq(requester.getId());
                    return plugin.getDatabase()
                            .deleteRelation(requester.getId(), denier.getId())
                            .thenRun(() -> {
                                if (plugin.getRedisManager().isActive())
                                    plugin.getRedisManager().publishAllyDeny(
                                            requester.getId().toString(), denier.getName());
                            });
                });
    }

    public CompletableFuture<Void> removeAlliance(@NotNull Clan a, @NotNull Clan b) {
        return EventUtil.call(plugin,
                        new ClanAllyEvent(a, b, ClanAllyEvent.Action.REMOVE))
                .thenCompose(ev -> {
                    if (ev.isCancelled())
                        return CompletableFuture.failedFuture(
                                new IllegalStateException("Alliance removal was cancelled by a plugin."));
                    a.removeAlly(b.getId());
                    b.removeAlly(a.getId());
                    return plugin.getDatabase().deleteRelation(a.getId(), b.getId())
                            .thenCompose(v -> plugin.getDatabase().deleteRelation(b.getId(), a.getId()))
                            .thenRun(() -> {
                                if (plugin.getRedisManager().isActive()) {
                                    plugin.getRedisManager().publishRelationUpdate(
                                            a.getId().toString(), b.getId().toString(), "ALLY", "REMOVE");
                                    plugin.getRedisManager().publishRelationUpdate(
                                            b.getId().toString(), a.getId().toString(), "ALLY", "REMOVE");
                                    plugin.getRedisManager().publishAllyRemove(
                                            a.getName(), b.getId().toString());
                                }
                            });
                });
    }

    public CompletableFuture<Void> addEnemy(@NotNull Clan declarer, @NotNull Clan target) {
        return EventUtil.call(plugin,
                        new ClanEnemyEvent(declarer, target, ClanEnemyEvent.Action.ADD))
                .thenCompose(ev -> {
                    if (ev.isCancelled())
                        return CompletableFuture.failedFuture(
                                new IllegalStateException("Enemy declaration was cancelled by a plugin."));
                    declarer.addEnemy(target.getId());
                    return plugin.getDatabase().insertRelation(
                            declarer.getId(), target.getId(), RelationType.ENEMY);
                });
    }

    public CompletableFuture<Void> removeEnemy(@NotNull Clan declarer, @NotNull Clan target) {
        return EventUtil.call(plugin,
                        new ClanEnemyEvent(declarer, target, ClanEnemyEvent.Action.REMOVE))
                .thenCompose(ev -> {
                    // REMOVE is informational; cancellation silently ignored here
                    declarer.removeEnemy(target.getId());
                    return plugin.getDatabase().deleteRelation(
                            declarer.getId(), target.getId());
                });
    }

    public void applyRedisRelationUpdate(String clanAId, String clanBId,
                                         String relType, String action) {
        Clan a = plugin.getClanCache().getById(UUID.fromString(clanAId));
        if (a == null) return;
        UUID bId = UUID.fromString(clanBId);
        boolean add = "ADD".equals(action);
        switch (relType) {
            case "ALLY" -> {
                if (add) a.addAlly(bId);
                else a.removeAlly(bId);
            }
            case "ENEMY" -> {
                if (add) a.addEnemy(bId);
                else a.removeEnemy(bId);
            }
        }
    }
}