package me.sunmc.clans.command.sub;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.command.SubCommand;
import me.sunmc.clans.model.Clan;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FriendlyFireSubCommand implements SubCommand {
    private final RomClans plugin;

    public FriendlyFireSubCommand(RomClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(@NotNull Player player, String[] args) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (!clan.isLeader(player.getUniqueId())) {
            plugin.getMessagesManager().send(player, "ff-not-leader");
            return;
        }
        plugin.getClanManager().toggleFriendlyFire(clan).thenRun(() ->
                plugin.getMessagesManager().send(player, clan.isFriendlyFire() ? "ff-enabled" : "ff-disabled"));
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