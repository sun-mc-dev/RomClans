package me.sunmc.clans.command.sub;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.command.SubCommand;
import me.sunmc.clans.model.Clan;
import me.sunmc.clans.util.MiniMessageUtil;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class RetagSubCommand implements SubCommand {

    private final RomClans plugin;

    public RetagSubCommand(RomClans plugin) {
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
            plugin.getMessagesManager().send(player, "retag-not-leader");
            return;
        }

        String newTag = String.join(" ", args);
        var cfg = plugin.getConfigManager();

        if (!MiniMessageUtil.isValidTag(newTag, cfg.getMinTagLength(), cfg.getMaxTagLength())) {
            plugin.getMessagesManager().send(player, "create-invalid-tag", Map.of(
                    "min", String.valueOf(cfg.getMinTagLength()),
                    "max", String.valueOf(cfg.getMaxTagLength())
            ));
            return;
        }

        plugin.getClanManager().retag(clan, newTag).thenRun(() ->
                plugin.getMessagesManager().send(player, "retag-success", Map.of("tag", newTag)));
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