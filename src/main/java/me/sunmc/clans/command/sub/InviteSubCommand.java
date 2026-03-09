package me.sunmc.clans.command.sub;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.command.SubCommand;
import me.sunmc.clans.model.Clan;
import me.sunmc.clans.model.ClanRank;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

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

        String targetName = args[0];
        Player target = plugin.getServer().getPlayerExact(targetName);

        if (target != null) {
            inviteOnline(player, clan, target);
        } else if (plugin.getRedisManager().isActive()) {
            plugin.getDatabase().findPlayerUuidByName(targetName).thenAccept(uuidOpt -> {
                if (uuidOpt.isEmpty()) {
                    plugin.getMessagesManager().send(player, "player-not-found",
                            Map.of("player", targetName));
                    return;
                }
                UUID targetUuid = uuidOpt.get();
                if (plugin.getClanManager().getPlayerClan(targetUuid) != null) {
                    plugin.getMessagesManager().send(player, "invite-target-in-clan");
                    return;
                }
                if (plugin.getInviteManager().hasPendingInvite(targetUuid, clan.getName())) {
                    plugin.getMessagesManager().send(player, "invite-already-sent");
                    return;
                }
                plugin.getInviteManager().createInvite(clan, player.getUniqueId(),
                        targetUuid, targetName);
                plugin.getMessagesManager().send(player, "invite-sent",
                        Map.of("player", targetName));
            });
        } else {
            plugin.getMessagesManager().send(player, "player-not-found",
                    Map.of("player", targetName));
        }
    }

    private void inviteOnline(Player player, Clan clan, @NotNull Player target) {
        if (plugin.getClanManager().getPlayerClan(target.getUniqueId()) != null) {
            plugin.getMessagesManager().send(player, "invite-target-in-clan");
            return;
        }
        if (plugin.getInviteManager().hasPendingInvite(target.getUniqueId(), clan.getName())) {
            plugin.getMessagesManager().send(player, "invite-already-sent");
            return;
        }
        plugin.getInviteManager().createInvite(clan, player.getUniqueId(),
                target.getUniqueId(), target.getName());
        plugin.getMessagesManager().send(player, "invite-sent",
                Map.of("player", target.getName()));
    }

    @Override
    public List<String> tabComplete(Player p, String @NotNull [] a) {
        if (a.length != 1) return List.of();
        String partial = a[0].toLowerCase(Locale.ROOT);

        // Start with players on THIS server (excluding self)
        Set<String> names = new LinkedHashSet<>();
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (!online.equals(p))
                names.add(online.getName());
        }

        // Add players online on OTHER servers via NetworkPlayerTracker
        if (plugin.getRedisManager().isActive()) {
            names.addAll(plugin.getNetworkPlayerTracker().getOnlineNames());
        }

        return names.stream()
                .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(partial))
                .sorted(String.CASE_INSENSITIVE_ORDER)
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