package me.sunmc.clans.command.sub;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.command.SubCommand;
import me.sunmc.clans.util.MiniMessageUtil;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CreateSubCommand implements SubCommand {
    private final RomClans plugin;

    public CreateSubCommand(RomClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, String @NotNull [] args) {
        if (args.length < 2) {
            plugin.getMessagesManager().send(player, "create-invalid-name");
            return;
        }

        if (plugin.getClanManager().getPlayerClan(player.getUniqueId()) != null) {
            plugin.getMessagesManager().send(player, "already-in-clan");
            return;
        }

        String name = args[0];
        String tag = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        var cfg = plugin.getConfigManager();

        // Name validation
        if (!name.matches("[a-zA-Z0-9_]{" + cfg.getMinNameLength() + "," + cfg.getMaxNameLength() + "}")) {
            plugin.getMessagesManager().send(player, "create-invalid-name");
            return;
        }

        // Tag validation (MiniMessage; check visible length)
        if (!MiniMessageUtil.isValidTag(tag, cfg.getMinTagLength(), cfg.getMaxTagLength())) {
            plugin.getMessagesManager().send(player, "create-invalid-tag",
                    Map.of("min", String.valueOf(cfg.getMinTagLength()),
                            "max", String.valueOf(cfg.getMaxTagLength())));
            return;
        }

        // Name uniqueness check (cache first, then DB)
        if (plugin.getClanCache().existsByName(name)) {
            plugin.getMessagesManager().send(player, "create-name-taken");
            return;
        }

        plugin.getClanManager().createClan(name, tag, player.getUniqueId(), player.getName())
                .thenAccept(clan -> plugin.getMessagesManager().send(player, "create-success",
                        Map.of("name", clan.getName())))
                .exceptionally(ex -> {
                    // Could be a DB UNIQUE constraint violation (race condition)
                    plugin.getMessagesManager().send(player, "create-name-taken");
                    return null;
                });
    }

    @Override
    public List<String> tabComplete(Player p, String[] a) {
        return List.of("<name>");
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