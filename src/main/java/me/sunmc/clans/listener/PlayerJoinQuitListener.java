package me.sunmc.clans.listener;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.model.Clan;
import me.sunmc.clans.model.ClanMember;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerJoinQuitListener implements Listener {

    private final RomClans plugin;

    public PlayerJoinQuitListener(RomClans plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        var player = event.getPlayer();
        // Persist UUID↔name for cross-server invite lookup
        plugin.getServer().getAsyncScheduler().runNow(plugin,
                t -> plugin.getDatabase().upsertPlayer(player.getUniqueId(), player.getName()));

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
        plugin.getChatManager().resetMode(event.getPlayer().getUniqueId());
    }
}