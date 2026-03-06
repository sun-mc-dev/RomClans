package me.sunmc.clans.manager;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.model.Clan;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class InviteManager {

    private final RomClans plugin;
    // inviteeUuid → Map<clanName (lowercase), PendingInvite>
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, PendingInvite>> pending
            = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "RomClans-InviteCleaner");
        t.setDaemon(true);
        return t;
    });

    public InviteManager(@NotNull RomClans plugin) {
        this.plugin = plugin;
        long expirySeconds = plugin.getConfigManager().getInviteExpiry();
        // Run cleaner every half the expiry period
        cleaner.scheduleAtFixedRate(this::cleanExpired,
                expirySeconds / 2, expirySeconds / 2, TimeUnit.SECONDS);
    }

    public void createInvite(@NotNull Clan clan, UUID inviterUuid, UUID inviteeUuid, String inviteeName) {
        pending.computeIfAbsent(inviteeUuid, k -> new ConcurrentHashMap<>())
                .put(clan.getName().toLowerCase(), new PendingInvite(inviterUuid, System.currentTimeMillis()));

        Player target = plugin.getServer().getPlayer(inviteeUuid);
        if (target != null) {
            String inviterName = Optional.ofNullable(plugin.getServer().getPlayer(inviterUuid))
                    .map(Player::getName).orElse("Unknown");
            plugin.getMessagesManager().send(target, "invite-received", Map.of(
                    "player", inviterName,
                    "clan", clan.getName(),
                    "cmd", plugin.getConfigManager().getCommandAlias()
            ));
        } else if (plugin.getRedisManager().isActive()) {
            plugin.getRedisManager().publishInvite(
                    clan.getId().toString(), clan.getName(),
                    inviterUuid.toString(), inviteeUuid.toString(), inviteeName
            );
        }
    }

    public boolean hasPendingInvite(UUID inviteeUuid, String clanName) {
        ConcurrentHashMap<String, PendingInvite> map = pending.get(inviteeUuid);
        if (map == null) return false;
        PendingInvite inv = map.get(clanName.toLowerCase());
        if (inv == null) return false;
        // Check not expired
        long expiryMs = plugin.getConfigManager().getInviteExpiry() * 1000L;
        if (System.currentTimeMillis() - inv.createdAt() > expiryMs) {
            map.remove(clanName.toLowerCase());
            if (map.isEmpty()) pending.remove(inviteeUuid);
            return false;
        }
        return true;
    }

    /**
     * Accept and clear the invite. Returns the Clan or null if not found / expired.
     */
    public Clan acceptInvite(UUID inviteeUuid, String clanName) {
        if (!hasPendingInvite(inviteeUuid, clanName)) return null;
        ConcurrentHashMap<String, PendingInvite> map = pending.get(inviteeUuid);
        if (map == null) return null;
        map.remove(clanName.toLowerCase());
        if (map.isEmpty()) pending.remove(inviteeUuid);
        return plugin.getClanCache().getByName(clanName);
    }

    /**
     * Deny and clear the invite. Populates inviterOut[0] with the inviter UUID if provided.
     */
    public void denyInvite(UUID inviteeUuid, String clanName, UUID[] inviterOut) {
        ConcurrentHashMap<String, PendingInvite> map = pending.get(inviteeUuid);
        if (map == null) return;
        PendingInvite inv = map.remove(clanName.toLowerCase());
        if (inv != null && inviterOut != null && inviterOut.length > 0) inviterOut[0] = inv.inviterUuid();
        if (map.isEmpty()) pending.remove(inviteeUuid);
    }

    /**
     * Called by RedisSubscriber when another server relays an invitation to a player on this server.
     */
    public void receiveRedisInvite(String clanId, String clanName, String inviterUuid,
                                   String inviteeUuid, String inviteeName) {
        UUID targetUuid = UUID.fromString(inviteeUuid);
        Player target = plugin.getServer().getPlayer(targetUuid);
        if (target == null) return;
        pending.computeIfAbsent(targetUuid, k -> new ConcurrentHashMap<>())
                .put(clanName.toLowerCase(), new PendingInvite(UUID.fromString(inviterUuid), System.currentTimeMillis()));
        plugin.getMessagesManager().send(target, "invite-received", Map.of(
                "player", inviteeName,
                "clan", clanName,
                "cmd", plugin.getConfigManager().getCommandAlias()
        ));
    }

    private void cleanExpired() {
        long expiryMs = plugin.getConfigManager().getInviteExpiry() * 1000L;
        long now = System.currentTimeMillis();
        pending.forEach((uuid, map) -> {
            map.entrySet().removeIf(e -> now - e.getValue().createdAt() > expiryMs);
            if (map.isEmpty()) pending.remove(uuid);
        });
    }

    public void shutdown() {
        cleaner.shutdownNow();
    }

    /**
     * Stores a single pending invite from a clan to a player.
     */
    private record PendingInvite(UUID inviterUuid, long createdAt) {
    }
}