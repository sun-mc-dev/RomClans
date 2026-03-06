package me.sunmc.clans.cache;

import me.sunmc.clans.model.Clan;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory cache of all clans.
 * All reads/writes use ConcurrentHashMap, so they are safe from any thread.
 */
public class ClanCache {

    // id → Clan
    private final ConcurrentHashMap<UUID, Clan> byId = new ConcurrentHashMap<>();
    // lowercase name → Clan
    private final ConcurrentHashMap<String, Clan> byName = new ConcurrentHashMap<>();
    // playerUuid → clanId
    private final ConcurrentHashMap<UUID, UUID> playerIndex = new ConcurrentHashMap<>();

    public void add(Clan clan) {
        byId.put(clan.getId(), clan);
        byName.put(clan.getName().toLowerCase(), clan);
        clan.getMembers().forEach((uuid, m) -> playerIndex.put(uuid, clan.getId()));
    }

    public void remove(UUID clanId) {
        Clan c = byId.remove(clanId);
        if (c == null) return;
        byName.remove(c.getName().toLowerCase());
        c.getMembers().keySet().forEach(playerIndex::remove);
    }

    public void addPlayerToIndex(UUID playerUuid, UUID clanId) {
        playerIndex.put(playerUuid, clanId);
    }

    public void removePlayerFromIndex(UUID playerUuid) {
        playerIndex.remove(playerUuid);
    }

    public Clan getById(UUID id) {
        return byId.get(id);
    }

    public Clan getByName(@NotNull String name) {
        return byName.get(name.toLowerCase());
    }

    public Clan getByPlayer(UUID playerUuid) {
        UUID clanId = playerIndex.get(playerUuid);
        return clanId == null ? null : byId.get(clanId);
    }

    public boolean existsByName(@NotNull String name) {
        return byName.containsKey(name.toLowerCase());
    }

    public Collection<Clan> getAll() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public void clear() {
        byId.clear();
        byName.clear();
        playerIndex.clear();
    }
}