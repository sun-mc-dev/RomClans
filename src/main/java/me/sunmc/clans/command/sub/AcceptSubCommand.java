package me.sunmc.clans.command.sub;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.command.SubCommand;
import me.sunmc.clans.model.Clan;
import me.sunmc.clans.model.ClanMember;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class AcceptSubCommand implements SubCommand {
    private final RomClans plugin;

    public AcceptSubCommand(RomClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, String @NotNull [] args) {
        if (args.length < 1) {
            plugin.getMessagesManager().send(player, "error");
            return;
        }
        if (plugin.getClanManager().getPlayerClan(player.getUniqueId()) != null) {
            plugin.getMessagesManager().send(player, "already-in-clan");
            return;
        }

        String clanName = args[0];
        if (!plugin.getInviteManager().hasPendingInvite(player.getUniqueId(), clanName)) {
            plugin.getMessagesManager().send(player, "invite-no-pending",
                    Map.of("clan", clanName));
            return;
        }

        Clan clan = plugin.getInviteManager().acceptInvite(player.getUniqueId(), clanName);
        if (clan == null) {
            plugin.getMessagesManager().send(player, "invite-expired");
            return;
        }

        if (clan.getMemberCount() >= plugin.getConfigManager().getMaxMembers()) {
            plugin.getMessagesManager().send(player, "invite-clan-full");
            return;
        }

        plugin.getClanManager().addMember(clan, player.getUniqueId(), player.getName())
                .thenRun(() -> {
                    plugin.getMessagesManager().send(player, "accept-success",
                            Map.of("clan", clan.getName()));
                    // Notify online members
                    for (ClanMember m : clan.getMembers().values()) {
                        if (m.getPlayerUuid().equals(player.getUniqueId())) continue;
                        Player p = plugin.getServer().getPlayer(m.getPlayerUuid());
                        if (p != null) plugin.getMessagesManager().send(p, "accept-notify",
                                Map.of("player", player.getName()));
                    }
                });
    }

    @Override
    public List<String> tabComplete(Player p, String @NotNull [] a) {
        if (a.length == 1) return plugin.getClanCache().getAll().stream()
                .map(Clan::getName).filter(n -> n.startsWith(a[0])).toList();
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