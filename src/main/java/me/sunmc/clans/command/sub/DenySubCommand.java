package me.sunmc.clans.command.sub;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.command.SubCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class DenySubCommand implements SubCommand {

    private final RomClans plugin;

    public DenySubCommand(RomClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, String @NotNull [] args) {
        if (args.length < 1) {
            Set<String> pending = plugin.getInviteManager().getAllPendingClanNames(player.getUniqueId());
            if (pending.isEmpty()) {
                plugin.getMessagesManager().send(player, "invite-none-pending");
            } else {
                plugin.getMessagesManager().send(player, "invite-list-header");
                pending.forEach(cn -> plugin.getMessagesManager().send(player, "invite-list-entry",
                        Map.of("clan", cn, "cmd", plugin.getConfigManager().getCommandAlias())));
            }
            return;
        }

        if ("all".equalsIgnoreCase(args[0])) {
            int count = plugin.getInviteManager().denyAll(player.getUniqueId());
            if (count == 0) plugin.getMessagesManager().send(player, "invite-none-pending");
            else plugin.getMessagesManager().send(player, "deny-all-success", Map.of("count", String.valueOf(count)));
            return;
        }

        String clanName = args[0];
        if (!plugin.getInviteManager().hasPendingInvite(player.getUniqueId(), clanName)) {
            plugin.getMessagesManager().send(player, "invite-no-pending", Map.of("clan", clanName));
            return;
        }
        UUID[] inviterOut = new UUID[1];
        plugin.getInviteManager().denyInvite(player.getUniqueId(), clanName, inviterOut);
        plugin.getMessagesManager().send(player, "deny-success", Map.of("clan", clanName));
        if (inviterOut[0] != null) {
            Player inv = plugin.getServer().getPlayer(inviterOut[0]);
            if (inv != null) plugin.getMessagesManager().send(inv, "deny-notify", Map.of("player", player.getName()));
        }
    }

    @Override
    public List<String> tabComplete(Player p, String @NotNull [] a) {
        if (a.length == 1) {
            List<String> opts = new ArrayList<>();
            opts.add("all");
            plugin.getInviteManager().getAllPendingClanNames(p.getUniqueId()).stream()
                    .filter(cn -> cn.toLowerCase().startsWith(a[0].toLowerCase()))
                    .forEach(opts::add);
            return opts;
        }
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