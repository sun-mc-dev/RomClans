package me.sunmc.clans.command.sub;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.command.SubCommand;
import me.sunmc.clans.gui.MembersGUI;
import me.sunmc.clans.model.Clan;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class MembersSubCommand implements SubCommand {

    private final RomClans plugin;

    public MembersSubCommand(RomClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, String @NotNull [] args) {
        Clan clan;
        if (args.length >= 1) {
            clan = plugin.getClanCache().getByName(args[0]);
            if (clan == null) {
                plugin.getMessagesManager().send(player, "clan-not-found", Map.of("clan", args[0]));
                return;
            }
        } else {
            clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
            if (clan == null) {
                plugin.getMessagesManager().send(player, "not-in-clan");
                return;
            }
        }
        Clan finalClan = clan;
        plugin.getFoliaScheduler().entity(player, () ->
                MembersGUI.open(player, finalClan, 0, plugin, plugin.getGuiManager()));
    }

    @Override
    public List<String> tabComplete(Player p, String @NotNull [] a) {
        if (a.length == 1) return plugin.getClanCache().getAll().stream()
                .map(Clan::getName)
                .filter(n -> n.toLowerCase().startsWith(a[0].toLowerCase()))
                .toList();
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