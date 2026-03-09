package me.sunmc.clans.listener;

import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import me.sunmc.clans.RomClans;
import me.sunmc.clans.model.Clan;
import me.sunmc.clans.model.ClanMember;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PlayerJoinQuitListener implements Listener {

    private final RomClans plugin;

    public PlayerJoinQuitListener(RomClans plugin) {
        this.plugin = plugin;
    }

    /**
     * Fires asynchronously during the configuration phase — before the player
     * entity is placed in any world. This is the earliest possible interception
     * point, guaranteeing zero ghost-location flicker on cross-server home TPs.
     * <p>
     * Because the event is already async we can call Redis directly without
     * submitting to dbExecutor and blocking.
     * <p>
     * NOTE: @Experimental — monitor Paper changelogs for API changes.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpawnLocation(@NotNull AsyncPlayerSpawnLocationEvent event) {
        if (!plugin.getRedisManager().isActive()) return;

        UUID playerUuid = event.getConnection().getProfile().getId();

        try {
            UUID clanId = plugin.getRedisManager().consumePendingHomeTeleport(playerUuid);
            if (clanId == null) return;

            Clan clan = plugin.getClanCache().getById(clanId);
            if (clan == null || !clan.isHomeSet()) return;

            Location dest = clan.buildHomeLocation(plugin.getServer());
            if (dest == null) return;

            // Player spawns here directly — client receives exactly one position.
            event.setSpawnLocation(dest);

        } catch (Exception e) {
            plugin.getLogger().warning("[HomeTP] Failed to check pending teleport on spawn: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        var player = event.getPlayer();

        plugin.getServer().getAsyncScheduler().runNow(plugin, t -> {
            plugin.getDatabase().upsertPlayer(player.getUniqueId(), player.getName());

            if (plugin.getRedisManager().isActive())
                plugin.getRedisManager().publishPlayerOnline(
                        player.getUniqueId().toString(), player.getName());

            // NOTE: pending home teleport is handled in onSpawnLocation above.
            // No delay, no ghost location.
        });

        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) return;
        ClanMember member = clan.getMember(player.getUniqueId());
        if (member == null) return;
        if (!member.getPlayerName().equals(player.getName())) {
            member.setPlayerName(player.getName());
            plugin.getServer().getAsyncScheduler().runNow(plugin,
                    t -> plugin.getDatabase().updateMember(clan.getId(), member));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        var player = event.getPlayer();
        plugin.getChatManager().resetMode(player.getUniqueId());

        if (plugin.getRedisManager().isActive())
            plugin.getServer().getAsyncScheduler().runNow(plugin, t ->
                    plugin.getRedisManager().publishPlayerOffline(
                            player.getUniqueId().toString(), player.getName()));
    }
}