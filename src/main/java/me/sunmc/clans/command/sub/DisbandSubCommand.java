package me.sunmc.clans.command.sub;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.command.SubCommand;
import me.sunmc.clans.gui.ConfirmGUI;
import me.sunmc.clans.model.Clan;
import me.sunmc.clans.model.ClanMember;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DisbandSubCommand implements SubCommand {

    private final RomClans plugin;

    public DisbandSubCommand(RomClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(@NotNull Player player, String[] args) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (!clan.isLeader(player.getUniqueId())) {
            plugin.getMessagesManager().send(player, "disband-not-leader");
            return;
        }
        plugin.getFoliaScheduler().entity(player, () -> openConfirm(player, clan));
    }

    private void openConfirm(Player player, @NotNull Clan clan) {
        ItemStack info = new ItemStack(Material.TNT);
        ItemMeta m = info.getItemMeta();
        m.displayName(Component.text("Disband: " + clan.getName(), NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        m.lore(List.of(
                Component.text("Members: " + clan.getMemberCount(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("⚠ This cannot be undone!", NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false)
        ));
        info.setItemMeta(m);

        ConfirmGUI.open(player,
                Component.text("Disband: " + clan.getName() + "?", NamedTextColor.DARK_RED),
                info,
                () -> plugin.getServer().getAsyncScheduler().runNow(plugin, t -> {
                    // Notify online members before disbanding
                    for (ClanMember mm : clan.getMembers().values()) {
                        if (mm.getPlayerUuid().equals(player.getUniqueId())) continue;
                        Player p = plugin.getServer().getPlayer(mm.getPlayerUuid());
                        if (p != null) plugin.getMessagesManager().send(p, "disband-notify");
                    }
                    plugin.getClanManager().disbandClan(clan).thenRun(() ->
                            plugin.getMessagesManager().send(player, "disband-success"));
                }),
                plugin.getGuiManager());
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