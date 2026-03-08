package me.sunmc.clans.command.sub;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.command.SubCommand;
import me.sunmc.clans.model.Clan;
import me.sunmc.clans.model.ClanMember;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class EnemySubCommand implements SubCommand {

    private final RomClans plugin;

    public EnemySubCommand(RomClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, String @NotNull [] args) {

        if (args.length < 2) {
            plugin.getMessagesManager().send(player, "enemy-usage");
            return;
        }

        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());

        if (!clan.isLeader(player.getUniqueId())) {
            plugin.getMessagesManager().send(player, "enemy-not-leader");
            return;
        }

        String action = args[0].toLowerCase();
        String targetName = args[1];

        Clan target = plugin.getClanCache().getByName(targetName);
        if (target == null) {
            plugin.getMessagesManager().send(player, "clan-not-found", Map.of("clan", targetName));
            return;
        }

        switch (action) {
            case "add" -> {
                if (clan.getId().equals(target.getId())) {
                    plugin.getMessagesManager().send(player, "enemy-self");
                    return;
                }
                if (clan.isEnemy(target.getId())) {
                    plugin.getMessagesManager().send(player, "enemy-already");
                    return;
                }
                if (clan.getEnemyIds().size() >= plugin.getConfigManager().getMaxEnemies()) {
                    plugin.getMessagesManager().send(player, "enemy-max");
                    return;
                }
                plugin.getRelationManager().addEnemy(clan, target).thenRun(() -> {
                    plugin.getMessagesManager().send(player, "enemy-add-success",
                            Map.of("clan", target.getName()));
                    notifyAll(target, "enemy-add-notify", Map.of("clan", clan.getName()));
                });
            }
            case "remove" -> {
                if (!clan.isEnemy(target.getId())) {
                    plugin.getMessagesManager().send(player, "enemy-not",
                            Map.of("clan", target.getName()));
                    return;
                }
                plugin.getRelationManager().removeEnemy(clan, target).thenRun(() ->
                        plugin.getMessagesManager().send(player, "enemy-remove-success",
                                Map.of("clan", target.getName())));
            }
            default -> plugin.getMessagesManager().send(player, "enemy-usage");
        }
    }

    private void notifyAll(@NotNull Clan clan, String key, Map<String, String> ph) {
        for (ClanMember m : clan.getMembers().values()) {
            Player p = plugin.getServer().getPlayer(m.getPlayerUuid());
            if (p != null) plugin.getMessagesManager().send(p, key, ph);
        }
    }

    @Override
    public List<String> tabComplete(Player p, String @NotNull [] a) {
        if (a.length == 1) return List.of("add", "remove");
        if (a.length == 2) {
            Clan clan = plugin.getClanManager().getPlayerClan(p.getUniqueId());
            String partial = a[1].toLowerCase();
            if ("remove".equalsIgnoreCase(a[0]) && clan != null) {
                return clan.getEnemyIds().stream()
                        .map(id -> plugin.getClanCache().getById(id))
                        .filter(java.util.Objects::nonNull)
                        .map(Clan::getName)
                        .filter(n -> n.toLowerCase().startsWith(partial))
                        .toList();
            }
            return plugin.getClanCache().getAll().stream()
                    .map(Clan::getName)
                    .filter(n -> n.toLowerCase().startsWith(partial))
                    .toList();
        }
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