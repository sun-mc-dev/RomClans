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
import java.util.Map;
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
            plugin.getMessagesManager().send(sender, "no-permission");
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
                plugin.getMessagesManager().send(sender, "admin-reload");
            }

            case "disband" -> {
                if (args.length < 2) {
                    plugin.getMessagesManager().send(sender, "admin-disband-usage",
                            Map.of("label", label));
                    return true;
                }
                String name = args[1];
                plugin.getServer().getAsyncScheduler().runNow(plugin, t -> {
                    Clan clan = plugin.getClanCache().getByName(name);
                    if (clan == null) {
                        plugin.getMessagesManager().send(sender, "clan-not-found",
                                Map.of("clan", name));
                        return;
                    }
                    if (!(sender instanceof Player player)) {
                        plugin.getClanManager().disbandClan(clan).thenRun(() ->
                                plugin.getMessagesManager().send(sender, "admin-disband-success",
                                        Map.of("clan", clan.getName())));
                        return;
                    }
                    plugin.getFoliaScheduler().entity(player, () -> openDisbandGui(player, clan));
                });
            }

            case "info" -> {
                if (args.length < 2) {
                    plugin.getMessagesManager().send(sender, "admin-info-usage",
                            Map.of("label", label));
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    plugin.getMessagesManager().send(sender, "admin-player-only");
                    return true;
                }
                String name = args[1];
                plugin.getServer().getAsyncScheduler().runNow(plugin, t -> {
                    Clan clan = plugin.getClanCache().getByName(name);
                    if (clan == null) {
                        plugin.getMessagesManager().send(player, "clan-not-found",
                                Map.of("clan", name));
                        return;
                    }
                    plugin.getFoliaScheduler().entity(player, () ->
                            ClanInfoGUI.open(player, clan, plugin, plugin.getGuiManager()));
                });
            }

            case "fixhome" -> {
                if (args.length < 3) {
                    plugin.getMessagesManager().send(sender, "admin-fixhome-usage",
                            Map.of("label", label));
                    return true;
                }
                String clanName = args[1];
                String serverId = args[2];
                plugin.getServer().getAsyncScheduler().runNow(plugin, t -> {
                    Clan clan = plugin.getClanCache().getByName(clanName);
                    if (clan == null) {
                        plugin.getMessagesManager().send(sender, "clan-not-found",
                                Map.of("clan", clanName));
                        return;
                    }
                    if (!clan.isHomeSet()) {
                        plugin.getMessagesManager().send(sender, "admin-fixhome-no-home",
                                Map.of("clan", clanName));
                        return;
                    }
                    clan.setHomeServerId(serverId);
                    plugin.getDbExecutor().submit(() -> {
                        try (Connection conn =
                                     ((AbstractDatabase) plugin.getDatabase())
                                             .getRawDataSource().getConnection();
                             PreparedStatement ps = conn.prepareStatement(
                                     "UPDATE clans SET home_server_id=? WHERE id=?")) {
                            ps.setString(1, serverId);
                            ps.setString(2, clan.getId().toString());
                            ps.executeUpdate();
                            plugin.getMessagesManager().send(sender, "admin-fixhome-success",
                                    Map.of("clan", clan.getName(), "server", serverId));
                        } catch (SQLException e) {
                            plugin.getLogger().warning("fixhome SQL error: " + e.getMessage());
                            plugin.getMessagesManager().send(sender, "admin-fixhome-error",
                                    Map.of("error", e.getMessage()));
                        }
                    });
                });
            }

            default -> sendHelp(sender, label);
        }
        return true;
    }

    private void sendHelp(@NotNull CommandSender sender, String label) {
        sender.sendMessage(plugin.getMessagesManager().parse("admin-help",
                Map.of("label", label)));
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
                        plugin.getClanManager().disbandClan(clan, true).thenRun(() ->
                                player.sendMessage(Component.text("[RomClans] Clan " + clan.getName() + " disbanded.", NamedTextColor.GREEN)))),
                plugin.getGuiManager());
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