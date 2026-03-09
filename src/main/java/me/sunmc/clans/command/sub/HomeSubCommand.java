package me.sunmc.clans.command.sub;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
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
        if (teleporting.contains(uuid)) return;

        int delay = plugin.getConfigManager().getHomeTeleportDelay();

        if (delay <= 0) {
            plugin.getFoliaScheduler().entity(player, () -> doTeleport(player, clan));
            return;
        }

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

    private void doTeleport(Player player, @NotNull Clan clan) {
        String homeServer = clan.getHomeServerId();
        String thisServer = plugin.getConfigManager().getServerId();

        plugin.getLogger().info("[HomeTP] clan='" + clan.getName()
                + "' homeServerId='" + homeServer + "'"
                + " thisServerId='" + thisServer + "'");

        if (homeServer == null || homeServer.isBlank()) {
            plugin.getLogger().warning("[HomeTP] home_server_id is NULL for clan '"
                    + clan.getName() + "'. Run /clanadmin fixhome <clan> <server-id>.");
            attemptLocalTeleport(player, clan, null);
            return;
        }

        boolean isCrossServer = !homeServer.equalsIgnoreCase(thisServer);
        plugin.getLogger().info("[HomeTP] isCrossServer=" + isCrossServer);

        if (isCrossServer) {
            if (plugin.getRedisManager().isActive()) {
                plugin.getMessagesManager().send(player, "home-sending",
                        Map.of("server", homeServer));

                final UUID clanId = clan.getId();
                final UUID playerUid = player.getUniqueId();
                final String target = homeServer;

                plugin.getServer().getAsyncScheduler().runNow(plugin, t -> {
                    plugin.getRedisManager().storePendingHomeTeleport(playerUid, clanId);
                    plugin.getRedisManager().publishSendToServer(playerUid.toString(), target);
                    plugin.getLogger().info("[HomeTP] Redis key stored + SEND_TO_SERVER published for "
                            + player.getName() + " → " + target);
                });
            } else {
                plugin.getMessagesManager().send(player, "home-wrong-server",
                        Map.of("server", homeServer));
            }
            return;
        }

        attemptLocalTeleport(player, clan, homeServer);
    }

    private void attemptLocalTeleport(Player player, @NotNull Clan clan, String homeServer) {
        Location dest = clan.buildHomeLocation(plugin.getServer());
        if (dest == null) {
            if (homeServer != null && !homeServer.isBlank())
                plugin.getMessagesManager().send(player, "home-wrong-server",
                        Map.of("server", homeServer));
            else
                plugin.getMessagesManager().send(player, "home-world-missing");
            return;
        }
        player.teleportAsync(dest).thenAccept(success -> {
            if (success) plugin.getMessagesManager().send(player, "home-teleported");
            else plugin.getMessagesManager().send(player, "home-teleport-failed");
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