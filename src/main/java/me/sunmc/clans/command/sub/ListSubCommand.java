package me.sunmc.clans.command.sub;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.command.SubCommand;
import me.sunmc.clans.model.Clan;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ListSubCommand implements SubCommand {

    private final RomClans plugin;

    public ListSubCommand(RomClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, String[] args) {
        Collection<Clan> clans = plugin.getClanCache().getAll();
        if (clans.isEmpty()) {
            plugin.getMessagesManager().send(player, "list-empty");
            return;
        }

        player.sendMessage(plugin.getMessagesManager().parse("list-header"));

        clans.stream()
                .sorted(Comparator.comparingInt(Clan::getMemberCount).reversed()
                        .thenComparing(Clan::getName))
                .forEach(clan -> player.sendMessage(
                        plugin.getMessagesManager().parse("list-entry",
                                Map.of("name", clan.getName(),
                                        "members", String.valueOf(clan.getMemberCount())),
                                Map.of("tag", plugin.getMessagesManager().deserialize(clan.getTag())))
                ));
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