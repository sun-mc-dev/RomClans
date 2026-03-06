package me.sunmc.clans.command.sub;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.command.SubCommand;
import me.sunmc.clans.model.Clan;
import me.sunmc.clans.model.ClanMember;
import me.sunmc.clans.model.ClanRank;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class PromoteSubCommand implements SubCommand {

    private final RomClans plugin;

    public PromoteSubCommand(RomClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, String @NotNull [] args) {
        if (args.length < 1) {
            plugin.getMessagesManager().send(player, "error");
            return;
        }

        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        ClanRank my = clan.getRankOf(player.getUniqueId());

        // Only CO_LEADER and LEADER can promote
        if (my == null || my.getLevel() < ClanRank.CO_LEADER.getLevel()) {
            plugin.getMessagesManager().send(player, "rank-no-perm");
            return;
        }

        ClanMember target = findByName(clan, args[0]);
        if (target == null) {
            plugin.getMessagesManager().send(player, "kick-not-found");
            return;
        }
        if (target.getPlayerUuid().equals(player.getUniqueId())) {
            plugin.getMessagesManager().send(player, "rank-no-perm");
            return;
        }
        if (!clan.canManage(player.getUniqueId(), target.getPlayerUuid())) {
            plugin.getMessagesManager().send(player, "rank-no-perm");
            return;
        }
        if (!target.getRank().canPromote()) {
            plugin.getMessagesManager().send(player, "promote-max-rank");
            return;
        }

        ClanRank newRank = target.getRank().promote();
        plugin.getClanManager().updateMemberRank(clan, target.getPlayerUuid(), newRank).thenRun(() -> {
            plugin.getMessagesManager().send(player, "promote-success",
                    Map.of("player", target.getPlayerName(), "rank", newRank.getDisplay()));
            Player tp = plugin.getServer().getPlayer(target.getPlayerUuid());
            if (tp != null) plugin.getMessagesManager().send(tp, "promote-notify",
                    Map.of("rank", newRank.getDisplay()));
        });
    }

    private @Nullable ClanMember findByName(@NotNull Clan clan, String name) {
        for (ClanMember m : clan.getMembers().values())
            if (m.getPlayerName().equalsIgnoreCase(name)) return m;
        return null;
    }

    @Override
    public List<String> tabComplete(@NotNull Player p, String[] a) {
        Clan clan = plugin.getClanManager().getPlayerClan(p.getUniqueId());
        if (clan == null || a.length != 1) return List.of();
        return clan.getMembers().values().stream()
                .filter(m -> m.getRank().canPromote())
                .map(ClanMember::getPlayerName)
                .filter(n -> n.toLowerCase().startsWith(a[0].toLowerCase()))
                .toList();
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