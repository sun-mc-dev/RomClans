package me.sunmc.clans.command;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.database.AbstractDatabase;
import me.sunmc.clans.gui.ClanInfoGUI;
import me.sunmc.clans.gui.ConfirmGUI;
import me.sunmc.clans.model.Clan;
import me.sunmc.clans.model.ClanMember;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

public class ClanAdminCommand extends Command {

    private static final String PERM = "romclans.admin";
    private final RomClans plugin;

    public ClanAdminCommand(RomClans plugin) {
        super("clanadmin");
        this.plugin = plugin;
        setDescription("RomClans admin command");
        setPermission(PERM);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, String @NotNull [] args) {
        if (!sender.hasPermission(PERM)) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.getConfigManager().reload();
                plugin.getMessagesManager().reload();
                sender.sendMessage(Component.text("[RomClans] Reloaded config & messages.", NamedTextColor.GREEN));
            }

            case "disband" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /" + label + " disband <clan>", NamedTextColor.RED));
                    return true;
                }
                String name = args[1];
                plugin.getServer().getAsyncScheduler().runNow(plugin, t -> {
                    Clan clan = plugin.getClanCache().getByName(name);
                    if (clan == null) {
                        sender.sendMessage(Component.text("Clan not found.", NamedTextColor.RED));
                        return;
                    }
                    if (!(sender instanceof Player player)) {
                        plugin.getClanManager().disbandClan(clan).thenRun(() ->
                                sender.sendMessage(Component.text("[RomClans] Clan " + clan.getName() + " disbanded.", NamedTextColor.GREEN)));
                        return;
                    }
                    plugin.getFoliaScheduler().entity(player, () -> openDisbandGui(player, clan));
                });
            }

            case "info" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /" + label + " info <clan>", NamedTextColor.RED));
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Console: use /clan info instead.", NamedTextColor.YELLOW));
                    return true;
                }
                String name = args[1];
                plugin.getServer().getAsyncScheduler().runNow(plugin, t -> {
                    Clan clan = plugin.getClanCache().getByName(name);
                    if (clan == null) {
                        player.sendMessage(Component.text("Clan not found.", NamedTextColor.RED));
                        return;
                    }
                    plugin.getFoliaScheduler().entity(player, () ->
                            ClanInfoGUI.open(player, clan, plugin, plugin.getGuiManager()));
                });
            }

            // Sets home_server_id in the DB + in-memory cache without moving the
            // home coordinates. Run this from the console of any server.
            case "fixhome" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text(
                            "Usage: /" + label + " fixhome <clan> <server-id>", NamedTextColor.RED));
                    return true;
                }
                String clanName = args[1];
                String serverId = args[2];
                plugin.getServer().getAsyncScheduler().runNow(plugin, t -> {
                    Clan clan = plugin.getClanCache().getByName(clanName);
                    if (clan == null) {
                        sender.sendMessage(Component.text("Clan '" + clanName + "' not found.", NamedTextColor.RED));
                        return;
                    }
                    if (!clan.isHomeSet()) {
                        sender.sendMessage(Component.text("Clan '" + clanName + "' has no home set.", NamedTextColor.RED));
                        return;
                    }
                    // Update in-memory
                    clan.setHomeServerId(serverId);
                    Location fakeLoc = new Location(
                            null, clan.getHomeX(), clan.getHomeY(), clan.getHomeZ(),
                            clan.getHomeYaw(), clan.getHomePitch());

                    plugin.getDbExecutor().submit(() -> {
                        try (Connection conn =
                                     ((AbstractDatabase) plugin.getDatabase())
                                             .getRawDataSource().getConnection();
                             PreparedStatement ps = conn.prepareStatement(
                                     "UPDATE clans SET home_server_id=? WHERE id=?")) {
                            ps.setString(1, serverId);
                            ps.setString(2, clan.getId().toString());
                            ps.executeUpdate();
                            sender.sendMessage(Component.text(
                                    "[RomClans] home_server_id for '" + clan.getName()
                                            + "' set to '" + serverId + "'.", NamedTextColor.GREEN));
                        } catch (SQLException e) {
                            plugin.getLogger().warning("fixhome SQL error: " + e.getMessage());
                            sender.sendMessage(Component.text("SQL error: " + e.getMessage(), NamedTextColor.RED));
                        }
                    });
                });
            }

            default -> sendHelp(sender, label);
        }
        return true;
    }

    private void openDisbandGui(Player player, @NotNull Clan clan) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta sm = (SkullMeta) head.getItemMeta();
        sm.setOwningPlayer(plugin.getServer().getOfflinePlayer(clan.getLeaderUuid()));
        String ldrName = Optional.ofNullable(clan.getMember(clan.getLeaderUuid()))
                .map(ClanMember::getPlayerName).orElse("Unknown");
        sm.displayName(Component.text("Disband: " + clan.getName(), NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        sm.lore(List.of(
                Component.text("Leader: " + ldrName, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("Members: " + clan.getMemberCount(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("⚠ This cannot be undone!", NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false)
        ));
        head.setItemMeta(sm);
        ConfirmGUI.open(player,
                Component.text("Admin Disband: " + clan.getName(), NamedTextColor.DARK_RED),
                head,
                () -> plugin.getServer().getAsyncScheduler().runNow(plugin, t ->
                        plugin.getClanManager().disbandClan(clan).thenRun(() ->
                                player.sendMessage(Component.text("[RomClans] Clan " + clan.getName() + " disbanded.", NamedTextColor.GREEN)))),
                plugin.getGuiManager());
    }

    private void sendHelp(@NotNull CommandSender s, String label) {
        s.sendMessage(Component.text()
                .append(Component.text("[RomClans Admin]\n", NamedTextColor.GOLD))
                .append(Component.text("/" + label + " reload\n", NamedTextColor.YELLOW))
                .append(Component.text("/" + label + " disband <clan>\n", NamedTextColor.YELLOW))
                .append(Component.text("/" + label + " info <clan>\n", NamedTextColor.YELLOW))
                .append(Component.text("/" + label + " fixhome <clan> <server-id>", NamedTextColor.YELLOW))
                .build());
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias,
                                             String @NotNull [] args, Location loc) {
        if (!sender.hasPermission(PERM)) return List.of();
        if (args.length == 1) return Stream.of("reload", "disband", "info", "fixhome")
                .filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        if (args.length == 2 && !args[0].equalsIgnoreCase("reload"))
            return plugin.getClanCache().getAll().stream()
                    .map(Clan::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        if (args.length == 3 && args[0].equalsIgnoreCase("fixhome"))
            return List.of(plugin.getConfigManager().getServerId());
        return List.of();
    }
}