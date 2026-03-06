package me.sunmc.clans.command.sub;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.command.SubCommand;
import me.sunmc.clans.model.Clan;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SetHomeSubCommand implements SubCommand {

    private final RomClans plugin;

    public SetHomeSubCommand(RomClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(@NotNull Player player, String[] args) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());

        if (!clan.isLeader(player.getUniqueId())) {
            plugin.getMessagesManager().send(player, "home-set-not-leader");
            return;
        }

        // Player location must be read on their owning region thread
        plugin.getFoliaScheduler().entity(player, () -> {
            Location loc = player.getLocation();
            clan.setHome(loc);
            plugin.getDatabase().updateClanHome(clan.getId(), loc).thenRun(() ->
                    plugin.getMessagesManager().send(player, "home-set"));
        });
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