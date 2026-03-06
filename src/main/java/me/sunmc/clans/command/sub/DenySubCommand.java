package me.sunmc.clans.command.sub;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.command.SubCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DenySubCommand implements SubCommand {
    private final RomClans plugin;

    public DenySubCommand(RomClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, String @NotNull [] args) {
        if (args.length < 1) {
            plugin.getMessagesManager().send(player, "error");
            return;
        }
        String clanName = args[0];
        if (!plugin.getInviteManager().hasPendingInvite(player.getUniqueId(), clanName)) {
            plugin.getMessagesManager().send(player, "invite-no-pending",
                    Map.of("clan", clanName));
            return;
        }
        UUID[] inviterOut = new UUID[1];
        plugin.getInviteManager().denyInvite(player.getUniqueId(), clanName, inviterOut);
        plugin.getMessagesManager().send(player, "deny-success", Map.of("clan", clanName));
        if (inviterOut[0] != null) {
            Player inv = plugin.getServer().getPlayer(inviterOut[0]);
            if (inv != null) plugin.getMessagesManager().send(inv, "deny-notify",
                    Map.of("player", player.getName()));
        }
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