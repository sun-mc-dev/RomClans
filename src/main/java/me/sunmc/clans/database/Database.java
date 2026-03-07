package me.sunmc.clans.database;

import me.sunmc.clans.model.Clan;
import me.sunmc.clans.model.ClanMember;
import me.sunmc.clans.model.RelationType;
import org.bukkit.Location;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface Database {
    CompletableFuture<Void> initialize();

    CompletableFuture<Void> insertClan(Clan clan);

    CompletableFuture<Optional<Clan>> findClanById(UUID id);

    CompletableFuture<Optional<Clan>> findClanByName(String name);

    CompletableFuture<Optional<Clan>> findClanByPlayer(UUID playerUuid);

    CompletableFuture<List<Clan>> findAllClans();

    CompletableFuture<Void> updateClan(Clan clan);

    /**
     * serverId is the config server-id of the server where home was set.
     */
    CompletableFuture<Void> updateClanHome(UUID clanId, Location loc, String serverId);

    CompletableFuture<Void> deleteClan(UUID clanId);

    CompletableFuture<Void> insertMember(UUID clanId, ClanMember member);

    CompletableFuture<Void> updateMember(UUID clanId, ClanMember member);

    CompletableFuture<Void> deleteMember(UUID clanId, UUID playerUuid);

    CompletableFuture<Void> insertRelation(UUID clanId, UUID targetId, RelationType type);

    CompletableFuture<Void> deleteRelation(UUID clanId, UUID targetId);

    CompletableFuture<Void> deleteAllRelationsForClan(UUID clanId);

    /**
     * Upsert a player record so cross-server invite can resolve UUID by name.
     */
    CompletableFuture<Void> upsertPlayer(UUID uuid, String name);

    /**
     * Returns empty if the player has never connected to any server in the network.
     */
    CompletableFuture<Optional<UUID>> findPlayerUuidByName(String name);

    CompletableFuture<Void> close();
}