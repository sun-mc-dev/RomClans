package me.sunmc.clans.listener;

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
import java.util.concurrent.TimeUnit;

public class PlayerJoinQuitListener implements Listener {

    private final RomClans plugin;

    public PlayerJoinQuitListener(RomClans plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        var player = event.getPlayer();

        plugin.getServer().getAsyncScheduler().runNow(plugin, t -> {

            plugin.getDatabase().upsertPlayer(player.getUniqueId(), player.getName());

            if (plugin.getRedisManager().isActive())
                plugin.getRedisManager().publishPlayerOnline(
                        player.getUniqueId().toString(), player.getName());

            if (plugin.getRedisManager().isActive()) {
                UUID clanId = plugin.getRedisManager()
                        .consumePendingHomeTeleport(player.getUniqueId());
                if (clanId != null) {
                    final UUID finalClanId = clanId;
                    // Delay 2 s so the player's entity is fully loaded before teleporting
                    plugin.getFoliaScheduler().asyncDelayed(() -> {
                        if (!player.isOnline()) return;
                        Clan clan = plugin.getClanCache().getById(finalClanId);
                        if (clan == null || !clan.isHomeSet()) return;
                        plugin.getFoliaScheduler().entity(player, () -> {
                            Location dest = clan.buildHomeLocation(plugin.getServer());
                            if (dest == null) {
                                plugin.getMessagesManager().send(player, "home-world-missing");
                                return;
                            }
                            player.teleportAsync(dest).thenAccept(success -> {
                                if (success) plugin.getMessagesManager().send(player, "home-teleported");
                                else plugin.getMessagesManager().send(player, "home-teleport-failed");
                            });
                        });
                    }, 2, TimeUnit.SECONDS);
                }
            }
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