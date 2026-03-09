package me.sunmc.clans.cache;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks players that are online on OTHER servers in the network.
 * Local players are always checked via Bukkit.getPlayer() first.
 */
public class NetworkPlayerTracker {

    private final Set<UUID> onlineUuids = ConcurrentHashMap.newKeySet();
    /**
     * Lower-cased name → original-case name, for O(1) prefix filtering.
     */
    private final ConcurrentHashMap<String, String> onlineNames = new ConcurrentHashMap<>();

    public void setOnline(UUID uuid, String name) {
        onlineUuids.add(uuid);
        if (name != null && !name.isBlank())
            onlineNames.put(name.toLowerCase(), name);
    }

    public void setOffline(UUID uuid, String name) {
        onlineUuids.remove(uuid);
        if (name != null)
            onlineNames.remove(name.toLowerCase());
    }

    /**
     * @deprecated Prefer {@link #setOnline(UUID, String)}.
     */
    @Deprecated
    public void setOnline(UUID uuid) {
        setOnline(uuid, null);
    }

    /**
     * @deprecated Prefer {@link #setOffline(UUID, String)}.
     */
    @Deprecated
    public void setOffline(UUID uuid) {
        setOffline(uuid, null);
    }

    public boolean isOnline(UUID uuid) {
        return onlineUuids.contains(uuid);
    }

    /**
     * Returns a snapshot of original-case names of players that are online on
     * other servers. Intended for tab-completion only.
     */
    public Set<String> getOnlineNames() {
        return Set.copyOf(onlineNames.values());
    }

    public void clear() {
        onlineUuids.clear();
        onlineNames.clear();
    }
}