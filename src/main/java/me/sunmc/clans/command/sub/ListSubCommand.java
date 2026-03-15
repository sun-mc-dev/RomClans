package me.sunmc.clans.command.sub;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.command.SubCommand;
import me.sunmc.clans.gui.ClanListGUI;
import org.bukkit.entity.Player;

import java.util.List;

public class ListSubCommand implements SubCommand {

    private final RomClans plugin;

    public ListSubCommand(RomClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, String[] args) {
        plugin.getFoliaScheduler().entity(player, () ->
                ClanListGUI.open(player, 0, plugin, plugin.getGuiManager()));
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
        return false;
    }
}