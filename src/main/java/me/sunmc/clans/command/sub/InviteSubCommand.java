package me.sunmc.clans.command.sub;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.command.SubCommand;
import me.sunmc.clans.model.Clan;
import me.sunmc.clans.model.ClanRank;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class InviteSubCommand implements SubCommand {
    private final RomClans plugin;

    public InviteSubCommand(RomClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, String @NotNull [] args) {
        if (args.length < 1) {
            plugin.getMessagesManager().send(player, "error");
            return;
        }
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());

        ClanRank rank = clan.getRankOf(player.getUniqueId());
        if (rank == null || !rank.canInvite()) {
            plugin.getMessagesManager().send(player, "invite-not-officer");
            return;
        }

        if (clan.getMemberCount() >= plugin.getConfigManager().getMaxMembers()) {
            plugin.getMessagesManager().send(player, "invite-clan-full");
            return;
        }

        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null) {
            plugin.getMessagesManager().send(player, "player-not-found",
                    Map.of("player", args[0]));
            return;
        }

        if (plugin.getClanManager().getPlayerClan(target.getUniqueId()) != null) {
            plugin.getMessagesManager().send(player, "invite-target-in-clan");
            return;
        }

        if (plugin.getInviteManager().hasPendingInvite(target.getUniqueId(), clan.getName())) {
            plugin.getMessagesManager().send(player, "invite-already-sent");
            return;
        }

        plugin.getInviteManager().createInvite(clan, player.getUniqueId(), target.getUniqueId(), target.getName());
        plugin.getMessagesManager().send(player, "invite-sent", Map.of("player", target.getName()));
    }

    @Override
    public List<String> tabComplete(Player p, String @NotNull [] a) {
        if (a.length == 1) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(a[0].toLowerCase()))
                    .toList();
        }
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