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

    CompletableFuture<Void> updateClanHome(UUID clanId, Location loc);

    CompletableFuture<Void> deleteClan(UUID clanId);

    CompletableFuture<Void> insertMember(UUID clanId, ClanMember member);

    CompletableFuture<Void> updateMember(UUID clanId, ClanMember member);

    CompletableFuture<Void> deleteMember(UUID clanId, UUID playerUuid);

    CompletableFuture<Void> insertRelation(UUID clanId, UUID targetId, RelationType type);

    CompletableFuture<Void> deleteRelation(UUID clanId, UUID targetId);

    CompletableFuture<Void> deleteAllRelationsForClan(UUID clanId);

    CompletableFuture<Void> close();
}