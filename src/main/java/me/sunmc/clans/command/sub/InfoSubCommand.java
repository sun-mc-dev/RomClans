package me.sunmc.clans.command.sub;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.command.SubCommand;
import me.sunmc.clans.model.Clan;
import me.sunmc.clans.model.ClanMember;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class InfoSubCommand implements SubCommand {

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");
    private final RomClans plugin;

    public InfoSubCommand(RomClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, String @NotNull [] args) {
        Clan clan;
        if (args.length >= 1) {
            clan = plugin.getClanCache().getByName(args[0]);
            if (clan == null) {
                plugin.getMessagesManager().send(player, "clan-not-found",
                        Map.of("clan", args[0]));
                return;
            }
        } else {
            clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
            if (clan == null) {
                plugin.getMessagesManager().send(player, "not-in-clan");
                return;
            }
        }

        String leaderName = Optional.ofNullable(clan.getMember(clan.getLeaderUuid()))
                .map(ClanMember::getPlayerName).orElse("Unknown");

        String allies = clan.getAllyIds().stream()
                .map(id -> {
                    Clan a = plugin.getClanCache().getById(id);
                    return a == null ? "?" : a.getName();
                })
                .collect(Collectors.joining(", "));

        String enemies = clan.getEnemyIds().stream()
                .map(id -> {
                    Clan e = plugin.getClanCache().getById(id);
                    return e == null ? "?" : e.getName();
                })
                .collect(Collectors.joining(", "));

        player.sendMessage(plugin.getMessagesManager().parse("info-header",
                Map.of("name", clan.getName())));
        player.sendMessage(plugin.getMessagesManager().parse("info-tag",
                Map.of("tag", clan.getTag())));
        player.sendMessage(plugin.getMessagesManager().parse("info-leader",
                Map.of("leader", leaderName)));
        player.sendMessage(plugin.getMessagesManager().parse("info-members",
                Map.of("count", String.valueOf(clan.getMemberCount()),
                        "max", String.valueOf(plugin.getConfigManager().getMaxMembers()))));
        player.sendMessage(plugin.getMessagesManager().parse("info-allies",
                Map.of("allies", allies.isEmpty() ? "None" : allies)));
        player.sendMessage(plugin.getMessagesManager().parse("info-enemies",
                Map.of("enemies", enemies.isEmpty() ? "None" : enemies)));
        player.sendMessage(plugin.getMessagesManager().parse("info-ff",
                Map.of("ff", clan.isFriendlyFire() ? "Enabled" : "Disabled")));
        player.sendMessage(plugin.getMessagesManager().parse("info-created",
                Map.of("date", SDF.format(new Date(clan.getCreatedAt())))));
    }

    @Override
    public List<String> tabComplete(Player p, String @NotNull [] a) {
        if (a.length == 1) return plugin.getClanCache().getAll().stream()
                .map(Clan::getName)
                .filter(n -> n.toLowerCase().startsWith(a[0].toLowerCase()))
                .toList();
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