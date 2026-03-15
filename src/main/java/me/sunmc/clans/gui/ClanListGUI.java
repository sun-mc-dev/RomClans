package me.sunmc.clans.gui;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.model.Clan;
import me.sunmc.clans.model.ClanMember;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class ClanListGUI {

    private static final int PAGE_SIZE = 45;

    private ClanListGUI() {}

    public static void open(Player viewer, int page, @NotNull RomClans plugin, GUIManager manager) {
        List<Clan> clans = plugin.getClanCache().getAll().stream()
                .sorted(Comparator.comparingInt(Clan::getMemberCount).reversed()
                        .thenComparing(Clan::getName))
                .toList();

        if (clans.isEmpty()) {
            plugin.getMessagesManager().send(viewer, "list-empty");
            return;
        }

        int totalPages = Math.max(1, (int) Math.ceil(clans.size() / (double) PAGE_SIZE));
        int finalPage = Math.max(0, Math.min(page, totalPages - 1));
        int start = finalPage * PAGE_SIZE;

        Component title = Component.text("All Clans (" + (finalPage + 1) + "/" + totalPages + ")", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false);
        Inventory inv = Bukkit.createInventory(null, 54, title);

        ItemStack filler = ConfirmGUI.pane(Material.GRAY_STAINED_GLASS_PANE, Component.empty());
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);

        int end = Math.min(start + PAGE_SIZE, clans.size());
        for (int i = start; i < end; i++) {
            inv.setItem(i - start, clanHead(clans.get(i), plugin));
        }

        inv.setItem(49, ConfirmGUI.pane(Material.PAPER,
                Component.text("Page " + (finalPage + 1) + " of " + totalPages + " | "
                        + clans.size() + " clans", NamedTextColor.GRAY)));

        if (finalPage > 0) {
            inv.setItem(45, navItem(Material.ARROW, Component.text("◀ Previous", NamedTextColor.YELLOW)));
        }
        if (finalPage < totalPages - 1) {
            inv.setItem(53, navItem(Material.ARROW, Component.text("Next ▶", NamedTextColor.YELLOW)));
        }

        manager.register(inv, e -> {
            int slot = e.getRawSlot();
            if (slot == 45 && finalPage > 0) {
                open(viewer, finalPage - 1, plugin, manager);
            } else if (slot == 53 && finalPage < totalPages - 1) {
                open(viewer, finalPage + 1, plugin, manager);
            } else if (slot >= 0 && slot < PAGE_SIZE) {
                int idx = start + slot;
                if (idx < clans.size()) {
                    ClanInfoGUI.open(viewer, clans.get(idx), plugin, manager);
                }
            }
        });
        viewer.openInventory(inv);
    }

    private static @NotNull ItemStack clanHead(@NotNull Clan clan, @NotNull RomClans plugin) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(clan.getLeaderUuid()));
        String ldrName = Optional.ofNullable(clan.getMember(clan.getLeaderUuid()))
                .map(ClanMember::getPlayerName).orElse("Unknown");
        meta.displayName(plugin.getMessagesManager().deserialize(clan.getTag())
                .append(Component.text(" " + clan.getName(), NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Leader: " + ldrName, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("Members: " + clan.getMemberCount(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Click to view info", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        skull.setItemMeta(meta);
        return skull;
    }

    private static @NotNull ItemStack navItem(Material mat, @NotNull Component name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        m.displayName(name.decoration(TextDecoration.ITALIC, false));
        it.setItemMeta(m);
        return it;
    }
}