package me.sunmc.clans.command.sub;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.command.SubCommand;
import me.sunmc.clans.model.Clan;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class HomeSubCommand implements SubCommand {

    private final RomClans plugin;
    /**
     * Players currently in a teleport countdown. Prevents double-triggering.
     */
    private final Set<UUID> teleporting = ConcurrentHashMap.newKeySet();

    public HomeSubCommand(RomClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(@NotNull Player player, String[] args) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());

        if (!clan.isHomeSet()) {
            plugin.getMessagesManager().send(player, "home-not-set");
            return;
        }

        UUID uuid = player.getUniqueId();
        if (teleporting.contains(uuid)) return; // silently ignore re-trigger

        int delay = plugin.getConfigManager().getHomeTeleportDelay();

        if (delay <= 0) {
            // Instant — dispatch teleport from entity thread
            plugin.getFoliaScheduler().entity(player, () -> doTeleport(player, clan));
            return;
        }

        // Delayed teleport with movement detection
        plugin.getFoliaScheduler().entity(player, () -> {
            Location start = player.getLocation().clone();
            teleporting.add(uuid);

            plugin.getMessagesManager().send(player, "home-teleporting",
                    Map.of("seconds", String.valueOf(delay)));

            plugin.getFoliaScheduler().asyncDelayed(() -> {
                if (!player.isOnline()) {
                    teleporting.remove(uuid);
                    return;
                }
                // Re-enter the entity thread to check position and teleport
                plugin.getFoliaScheduler().entity(player, () -> {
                    teleporting.remove(uuid);
                    Location current = player.getLocation();
                    if (!sameWorld(start, current) || current.distanceSquared(start) > 0.5) {
                        plugin.getMessagesManager().send(player, "home-moved");
                        return;
                    }
                    doTeleport(player, clan);
                });
            }, delay, TimeUnit.SECONDS);
        });
    }

    /**
     * Resolves the home location and initiates the async teleport. Must be on entity thread.
     * properly handles teleportAsync returning false.
     */
    private void doTeleport(Player player, @NotNull Clan clan) {
        Location dest = clan.buildHomeLocation(plugin.getServer());
        if (dest == null) {
            String homeServer = clan.getHomeServerId();
            String thisServer = plugin.getConfigManager().getServerId();
            if (homeServer != null && !homeServer.equals(thisServer)) {
                plugin.getMessagesManager().send(player, "home-wrong-server",
                        Map.of("server", homeServer));
            } else {
                plugin.getMessagesManager().send(player, "home-world-missing");
            }
            return;
        }
        player.teleportAsync(dest).thenAccept(success -> {
            if (success) {
                plugin.getMessagesManager().send(player, "home-teleported");
            } else {
                plugin.getMessagesManager().send(player, "home-teleport-failed");
            }
        });
    }

    private boolean sameWorld(@NotNull Location a, Location b) {
        return a.getWorld() != null && b.getWorld() != null
                && a.getWorld().getUID().equals(b.getWorld().getUID());
    }

    @Override
    public List<String> tabComplete(Player p, String[] a) {
        return List.of();
    }

    @Override
    public String getPermission() {
        return null;
    }

    @Override
    public boolean requiresClan() {
        return true;
    }
}