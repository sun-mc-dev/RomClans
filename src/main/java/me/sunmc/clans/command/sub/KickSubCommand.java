package me.sunmc.clans.command.sub;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.api.event.ClanMemberLeaveEvent;
import me.sunmc.clans.command.SubCommand;
import me.sunmc.clans.model.Clan;
import me.sunmc.clans.model.ClanMember;
import me.sunmc.clans.model.ClanRank;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class KickSubCommand implements SubCommand {

    private final RomClans plugin;

    public KickSubCommand(RomClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, String @NotNull [] args) {
        if (args.length < 1) {
            plugin.getMessagesManager().send(player, "error");
            return;
        }

        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());

        ClanRank myRank = clan.getRankOf(player.getUniqueId());
        if (myRank == null || !myRank.canKick()) {
            plugin.getMessagesManager().send(player, "rank-no-perm");
            return;
        }

        // Locate target by name within the clan
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
            plugin.getMessagesManager().send(player, "kick-self");
            return;
        }
        if (!clan.canManage(player.getUniqueId(), target.getPlayerUuid())) {
            plugin.getMessagesManager().send(player, "kick-no-perm");
            return;
        }

        UUID targetUuid = target.getPlayerUuid();
        String targetName = target.getPlayerName();

        plugin.getClanManager().removeMember(clan, targetUuid,
        ClanMemberLeaveEvent.Reason.KICKED).thenRun(() -> {
            plugin.getMessagesManager().send(player, "kick-success", Map.of("player", targetName));
            Player targetPlayer = plugin.getServer().getPlayer(targetUuid);
            if (targetPlayer != null) {
                plugin.getMessagesManager().send(targetPlayer, "kick-notify");
                plugin.getChatManager().resetMode(targetUuid);
            }
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