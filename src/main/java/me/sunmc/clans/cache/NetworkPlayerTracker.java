package me.sunmc.clans.cache;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks players that are online on OTHER servers in the network.
 * Local players are always checked via Bukkit.getPlayer() first.
 */
public class NetworkPlayerTracker {

    private final Set<UUID> online = ConcurrentHashMap.newKeySet();

    public void setOnline(UUID uuid) {
        online.add(uuid);
    }

    public void setOffline(UUID uuid) {
        online.remove(uuid);
    }

    public boolean isOnline(UUID uuid) {
        return online.contains(uuid);
    }

    public void clear() {
        online.clear();
    }
}