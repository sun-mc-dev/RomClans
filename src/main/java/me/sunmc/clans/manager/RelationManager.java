package me.sunmc.clans.manager;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.model.Clan;
import me.sunmc.clans.model.RelationType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RelationManager {

    private final RomClans plugin;

    public RelationManager(RomClans plugin) {
        this.plugin = plugin;
    }

    public boolean areAllies(UUID clanAId, UUID clanBId) {
        Clan a = plugin.getClanCache().getById(clanAId);
        return a != null && a.isAlly(clanBId);
    }

    public boolean areEnemies(UUID clanAId, UUID clanBId) {
        Clan a = plugin.getClanCache().getById(clanAId);
        return a != null && a.isEnemy(clanBId);
    }

    /**
     * Clan {@code requester} sends an alliance request to clan {@code target}.
     * Persists to DB and notifies target-clan officers on remote servers via Redis. (Bug 8)
     */
    public CompletableFuture<Void> sendAllyRequest(@NotNull Clan requester, @NotNull Clan target) {
        target.addPendingAllyReq(requester.getId());
        return plugin.getDatabase()
                .insertRelation(requester.getId(), target.getId(), RelationType.ALLY_REQUEST)
                .thenRun(() -> {
                    if (plugin.getRedisManager().isActive()) {
                        plugin.getRedisManager().publishAllyRequest(
                                requester.getId().toString(),
                                requester.getName(),
                                target.getId().toString());
                    }
                });
    }

    /**
     * Clan {@code acceptor} accepts an alliance request that came FROM {@code requester}.
     * Precondition: acceptor.hasPendingAllyReq(requester.getId()) == true.
     */
    public CompletableFuture<Void> acceptAlliance(@NotNull Clan requester, @NotNull Clan acceptor) {
        // Update in-memory
        acceptor.removePendingAllyReq(requester.getId());
        requester.addAlly(acceptor.getId());
        acceptor.addAlly(requester.getId());

        // DB: delete the ALLY_REQUEST row, insert two ALLY rows (bidirectional)
        return plugin.getDatabase().deleteRelation(requester.getId(), acceptor.getId())
                .thenCompose(v -> plugin.getDatabase().insertRelation(
                        requester.getId(), acceptor.getId(), RelationType.ALLY))
                .thenCompose(v -> plugin.getDatabase().insertRelation(
                        acceptor.getId(), requester.getId(), RelationType.ALLY))
                .thenRun(() -> {
                    if (plugin.getRedisManager().isActive()) {
                        plugin.getRedisManager().publishRelationUpdate(
                                requester.getId().toString(), acceptor.getId().toString(), "ALLY", "ADD");
                        plugin.getRedisManager().publishRelationUpdate(
                                acceptor.getId().toString(), requester.getId().toString(), "ALLY", "ADD");
                    }
                });
    }

    /**
     * Clan {@code denier} denies a request from {@code requester}.
     */
    public CompletableFuture<Void> denyAlliance(@NotNull Clan requester, @NotNull Clan denier) {
        denier.removePendingAllyReq(requester.getId());
        return plugin.getDatabase().deleteRelation(requester.getId(), denier.getId());
    }

    /**
     * Break an existing alliance (bidirectional).
     */
    public CompletableFuture<Void> removeAlliance(@NotNull Clan a, @NotNull Clan b) {
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
                    }
                });
    }

    public CompletableFuture<Void> addEnemy(@NotNull Clan declarer, @NotNull Clan target) {
        declarer.addEnemy(target.getId());
        return plugin.getDatabase().insertRelation(declarer.getId(), target.getId(), RelationType.ENEMY);
    }

    public CompletableFuture<Void> removeEnemy(@NotNull Clan declarer, @NotNull Clan target) {
        declarer.removeEnemy(target.getId());
        return plugin.getDatabase().deleteRelation(declarer.getId(), target.getId());
    }

    public void applyRedisRelationUpdate(String clanAId, String clanBId, String relType, String action) {
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