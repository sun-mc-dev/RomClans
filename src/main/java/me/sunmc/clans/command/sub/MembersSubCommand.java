package me.sunmc.clans.command.sub;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.command.SubCommand;
import me.sunmc.clans.model.Clan;
import me.sunmc.clans.model.ClanMember;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
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
                plugin.getMessagesManager().send(player, "clan-not-found",
                        Map.of("clan", args[0]));
                return;
            }
        } else {
            clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
            if (clan == null) {
                plugin.getMessagesManager().send(player, "not-in-clan");
                return;
            }
        }

        player.sendMessage(plugin.getMessagesManager().parse("members-header",
                Map.of("name", clan.getName())));

        // Sort: LEADER first, then CO_LEADER, OFFICER, MEMBER; alphabetical within rank
        clan.getMembers().values().stream()
                .sorted(Comparator
                        .comparingInt((ClanMember m) -> m.getRank().getLevel()).reversed()
                        .thenComparing(ClanMember::getPlayerName))
                .forEach(m -> player.sendMessage(plugin.getMessagesManager().parse("members-entry",
                        Map.of("player", m.getPlayerName(), "rank", m.getRank().getDisplay()))));
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