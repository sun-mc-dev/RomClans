package me.sunmc.clans.command.sub;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.api.event.ClanMemberLeaveEvent;
import me.sunmc.clans.command.SubCommand;
import me.sunmc.clans.model.Clan;
import me.sunmc.clans.model.ClanMember;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class LeaveSubCommand implements SubCommand {
    private final RomClans plugin;

    public LeaveSubCommand(RomClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(@NotNull Player player, String[] args) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan.isLeader(player.getUniqueId())) {
            plugin.getMessagesManager().send(player, "leave-leader");
            return;
        }
        String clanName = clan.getName();
        plugin.getClanManager().removeMember(clan, player.getUniqueId(),
        ClanMemberLeaveEvent.Reason.LEFT).thenRun(() -> {
            plugin.getMessagesManager().send(player, "leave-success", Map.of("clan", clanName));
            for (ClanMember m : clan.getMembers().values()) {
                Player p = plugin.getServer().getPlayer(m.getPlayerUuid());
                if (p != null) plugin.getMessagesManager().send(p, "leave-notify",
                        Map.of("player", player.getName()));
            }
        });
        plugin.getChatManager().resetMode(player.getUniqueId());
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
        return true;
    }
}