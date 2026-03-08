package me.sunmc.clans.command.sub;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.command.SubCommand;
import me.sunmc.clans.model.Clan;
import me.sunmc.clans.model.ClanMember;
import me.sunmc.clans.model.ClanRank;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class AllySubCommand implements SubCommand {

    private final RomClans plugin;

    public AllySubCommand(RomClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, String @NotNull [] args) {

        if (args.length < 2) {
            plugin.getMessagesManager().send(player, "ally-usage");
            return;
        }

        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());

        if (!clan.isLeader(player.getUniqueId())) {
            plugin.getMessagesManager().send(player, "ally-not-leader");
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
            case "add" -> handleAdd(player, clan, target);
            case "accept" -> handleAccept(player, clan, target);
            case "deny" -> handleDeny(player, clan, target);
            case "remove" -> handleRemove(player, clan, target);
            default -> plugin.getMessagesManager().send(player, "ally-usage");
        }
    }

    private void handleAdd(Player player, @NotNull Clan clan, @NotNull Clan target) {
        if (clan.getId().equals(target.getId())) {
            plugin.getMessagesManager().send(player, "ally-self");
            return;
        }
        if (clan.isAlly(target.getId())) {
            plugin.getMessagesManager().send(player, "ally-already");
            return;
        }
        if (clan.getAllyIds().size() >= plugin.getConfigManager().getMaxAllies()) {
            plugin.getMessagesManager().send(player, "ally-max");
            return;
        }

        plugin.getRelationManager().sendAllyRequest(clan, target).thenRun(() -> {
            plugin.getMessagesManager().send(player, "ally-request-sent",
                    Map.of("clan", target.getName()));
            // Notify officers+ of the target clan
            notifyOfficers(target, "ally-request-received", Map.of(
                    "clan", clan.getName(),
                    "cmd", plugin.getConfigManager().getCommandAlias()
            ));
        });
    }

    private void handleAccept(Player player, @NotNull Clan clan, @NotNull Clan requester) {
        if (!clan.hasPendingAllyReq(requester.getId())) {
            plugin.getMessagesManager().send(player, "ally-no-request",
                    Map.of("clan", requester.getName()));
            return;
        }

        if (clan.getAllyIds().size() >= plugin.getConfigManager().getMaxAllies()) {
            plugin.getMessagesManager().send(player, "ally-max");
            return;
        }

        if (requester.getAllyIds().size() >= plugin.getConfigManager().getMaxAllies()) {
            plugin.getMessagesManager().send(player, "ally-max");
            return;
        }

        plugin.getRelationManager().acceptAlliance(requester, clan).thenRun(() -> {
            plugin.getMessagesManager().send(player, "ally-accept-success",
                    Map.of("clan", requester.getName()));
            notifyOfficers(requester, "ally-accept-notify", Map.of("clan", clan.getName()));
        });
    }

    private void handleDeny(Player player, @NotNull Clan clan, @NotNull Clan requester) {
        if (!clan.hasPendingAllyReq(requester.getId())) {
            plugin.getMessagesManager().send(player, "ally-no-request",
                    Map.of("clan", requester.getName()));
            return;
        }

        plugin.getRelationManager().denyAlliance(requester, clan).thenRun(() -> {
            plugin.getMessagesManager().send(player, "ally-deny-success",
                    Map.of("clan", requester.getName()));
            notifyOfficers(requester, "ally-deny-notify", Map.of("clan", clan.getName()));
        });
    }

    private void handleRemove(Player player, @NotNull Clan clan, @NotNull Clan target) {
        if (!clan.isAlly(target.getId())) {
            plugin.getMessagesManager().send(player, "ally-not-allied",
                    Map.of("clan", target.getName()));
            return;
        }

        plugin.getRelationManager().removeAlliance(clan, target).thenRun(() -> {
            plugin.getMessagesManager().send(player, "ally-remove-success",
                    Map.of("clan", target.getName()));
            notifyOfficers(target, "ally-remove-notify", Map.of("clan", clan.getName()));
        });
    }

    /**
     * Notify all online officers and above in the given clan.
     */
    private void notifyOfficers(@NotNull Clan clan, String key, Map<String, String> ph) {
        for (ClanMember m : clan.getMembers().values()) {
            if (m.getRank().getLevel() < ClanRank.OFFICER.getLevel()) continue;
            Player p = plugin.getServer().getPlayer(m.getPlayerUuid());
            if (p != null) plugin.getMessagesManager().send(p, key, ph);
        }
    }

    @Override
    public List<String> tabComplete(Player p, String @NotNull [] a) {
        if (a.length == 1) return List.of("add", "accept", "deny", "remove");
        if (a.length == 2) {
            Clan clan = plugin.getClanManager().getPlayerClan(p.getUniqueId());
            String partial = a[1].toLowerCase();
            return switch (a[0].toLowerCase()) {
                case "accept", "deny" -> {
                    if (clan == null) yield List.of();
                    yield clan.getPendingAllyReqs().stream()
                            .map(id -> plugin.getClanCache().getById(id))
                            .filter(java.util.Objects::nonNull)
                            .map(Clan::getName)
                            .filter(n -> n.toLowerCase().startsWith(partial))
                            .toList();
                }
                case "remove" -> {
                    if (clan == null) yield List.of();
                    yield clan.getAllyIds().stream()
                            .map(id -> plugin.getClanCache().getById(id))
                            .filter(java.util.Objects::nonNull)
                            .map(Clan::getName)
                            .filter(n -> n.toLowerCase().startsWith(partial))
                            .toList();
                }
                default -> plugin.getClanCache().getAll().stream()
                        .map(Clan::getName)
                        .filter(n -> n.toLowerCase().startsWith(partial))
                        .toList();
            };
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