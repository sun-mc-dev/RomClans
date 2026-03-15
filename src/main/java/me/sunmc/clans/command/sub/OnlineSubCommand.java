package me.sunmc.clans.command.sub;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.command.SubCommand;
import me.sunmc.clans.model.Clan;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class OnlineSubCommand implements SubCommand {

    private final RomClans plugin;

    public OnlineSubCommand(RomClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, String[] args) {
        record Entry(Clan clan, long count) {}

        List<Entry> results = plugin.getClanCache().getAll().stream()
                .map(clan -> {
                    long online = clan.getMembers().keySet().stream()
                            .filter(uuid -> plugin.getServer().getPlayer(uuid) != null
                                    || (plugin.getRedisManager().isActive()
                                    && plugin.getNetworkPlayerTracker().isOnline(uuid)))
                            .count();
                    return new Entry(clan, online);
                })
                .filter(e -> e.count() > 0)
                .sorted(Comparator.<Entry, Long>comparing(Entry::count).reversed()
                        .thenComparing(e -> e.clan().getName()))
                .toList();

        if (results.isEmpty()) {
            plugin.getMessagesManager().send(player, "online-empty");
            return;
        }

        player.sendMessage(plugin.getMessagesManager().parse("online-header"));
        results.forEach(e -> player.sendMessage(
                plugin.getMessagesManager().parse("online-entry",
                        Map.of("name", e.clan().getName(), "count", String.valueOf(e.count())),
                        Map.of("tag", plugin.getMessagesManager().deserialize(e.clan().getTag())))));
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