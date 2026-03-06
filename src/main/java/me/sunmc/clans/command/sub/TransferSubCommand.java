package me.sunmc.clans.command.sub;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.command.SubCommand;
import me.sunmc.clans.model.Clan;
import me.sunmc.clans.model.ClanMember;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class TransferSubCommand implements SubCommand {

    private final RomClans plugin;

    public TransferSubCommand(RomClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, String @NotNull [] args) {
        if (args.length < 1) {
            plugin.getMessagesManager().send(player, "error");
            return;
        }

        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());

        if (!clan.isLeader(player.getUniqueId())) {
            plugin.getMessagesManager().send(player, "transfer-not-leader");
            return;
        }

        ClanMember target = null;
        for (ClanMember m : clan.getMembers().values()) {
            if (m.getPlayerName().equalsIgnoreCase(args[0])) {
                target = m;
                break;
            }
        }

        if (target == null) {
            plugin.getMessagesManager().send(player, "kick-not-found");
            return;
        }
        if (target.getPlayerUuid().equals(player.getUniqueId())) {
            plugin.getMessagesManager().send(player, "rank-no-perm");
            return;
        }

        ClanMember finalTarget = target;
        plugin.getClanManager().transferLeadership(clan, target.getPlayerUuid()).thenRun(() -> {
            plugin.getMessagesManager().send(player, "transfer-success",
                    Map.of("player", finalTarget.getPlayerName()));
            Player tp = plugin.getServer().getPlayer(finalTarget.getPlayerUuid());
            if (tp != null) plugin.getMessagesManager().send(tp, "transfer-notify");
        });
    }

    @Override
    public List<String> tabComplete(@NotNull Player p, String[] a) {
        Clan clan = plugin.getClanManager().getPlayerClan(p.getUniqueId());
        if (clan == null || a.length != 1) return List.of();
        return clan.getMembers().values().stream()
                .map(ClanMember::getPlayerName)
                .filter(n -> !n.equalsIgnoreCase(p.getName()))
                .filter(n -> n.toLowerCase().startsWith(a[0].toLowerCase()))
                .toList();
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