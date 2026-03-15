package me.sunmc.clans.command;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.command.sub.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Level;

public class ClanCommand extends Command {

    private final RomClans plugin;
    private final LinkedHashMap<String, SubCommand> subCmds = new LinkedHashMap<>();

    public ClanCommand(String name, RomClans plugin) {
        super(name);
        this.plugin = plugin;
        setDescription("Clan management command");
        setAliases(List.of());
        register();
    }

    private void register() {
        sub("create", new CreateSubCommand(plugin));
        sub("disband", new DisbandSubCommand(plugin));
        sub("friendlyfire", new FriendlyFireSubCommand(plugin));
        sub("invite", new InviteSubCommand(plugin));
        sub("accept", new AcceptSubCommand(plugin));
        sub("deny", new DenySubCommand(plugin));
        sub("leave", new LeaveSubCommand(plugin));
        sub("kick", new KickSubCommand(plugin));
        sub("promote", new PromoteSubCommand(plugin));
        sub("demote", new DemoteSubCommand(plugin));
        sub("transfer", new TransferSubCommand(plugin));
        sub("ally", new AllySubCommand(plugin));
        sub("enemy", new EnemySubCommand(plugin));
        sub("chat", new ChatSubCommand(plugin));
        sub("info", new InfoSubCommand(plugin));
        sub("members", new MembersSubCommand(plugin));
        sub("list", new ListSubCommand(plugin));
        sub("online", new OnlineSubCommand(plugin));
        sub("retag", new RetagSubCommand(plugin));
        sub("sethome", new SetHomeSubCommand(plugin));
        sub("home", new HomeSubCommand(plugin));
    }

    private void sub(String name, SubCommand s) {
        subCmds.put(name, s);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use clan commands.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player, label);
            return true;
        }

        String subName = args[0].toLowerCase(Locale.ROOT);
        SubCommand sub = subCmds.get(subName);

        if (sub == null) {
            sendHelp(player, label);
            return true;
        }

        String perm = sub.getPermission();
        if (perm != null && !player.hasPermission(perm)) {
            plugin.getMessagesManager().send(player, "no-permission");
            return true;
        }

        if (sub.requiresClan() && plugin.getClanManager().getPlayerClan(player.getUniqueId()) == null) {
            plugin.getMessagesManager().send(player, "not-in-clan");
            return true;
        }

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        // Execute asynchronously — all sub-command logic is async-safe.
        // When world/entity state is needed, subcommands dispatch to entity scheduler.
        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            try {
                // Re-check inside the async block: the membership check on the command thread
                // and this execution have a race window where the player may have left/been kicked.
                if (sub.requiresClan() && plugin.getClanManager().getPlayerClan(player.getUniqueId()) == null) {
                    plugin.getMessagesManager().send(player, "not-in-clan");
                    return;
                }
                sub.execute(player, subArgs);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error in sub-command " + subName, e);
                plugin.getMessagesManager().send(player, "error");
            }
        });

        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String @NotNull [] args,
                                             Location location) {
        if (!(sender instanceof Player player)) return List.of();

        if (args.length == 1) {
            String partial = args[0].toLowerCase(Locale.ROOT);
            return subCmds.entrySet().stream()
                    .filter(e -> e.getKey().startsWith(partial))
                    .filter(e -> {
                        String p = e.getValue().getPermission();
                        return p == null || player.hasPermission(p);
                    })
                    .map(Map.Entry::getKey)
                    .toList();
        }

        if (args.length >= 2) {
            SubCommand sub = subCmds.get(args[0].toLowerCase(Locale.ROOT));
            if (sub != null) {
                return sub.tabComplete(player, Arrays.copyOfRange(args, 1, args.length));
            }
        }

        return List.of();
    }

    private void sendHelp(@NotNull Player player, String label) {
        player.sendMessage(plugin.getMessagesManager().parse("help",
                Map.of("label", label)));
    }
}