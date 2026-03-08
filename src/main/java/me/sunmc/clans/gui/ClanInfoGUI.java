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

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ClanInfoGUI {

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");

    private ClanInfoGUI() {
    }

    /**
     * Must be called from the entity's region thread (Folia).
     */
    public static void open(Player viewer,
                            @NotNull Clan clan,
                            RomClans plugin,
                            GUIManager manager) {

        Component title = Component.text("✦ Clan: " + clan.getName(), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false);
        Inventory inv = Bukkit.createInventory(null, 54, title);

        ItemStack border = ConfirmGUI.pane(Material.GRAY_STAINED_GLASS_PANE, Component.empty());
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);

        // Slot 1 — Tag
        inv.setItem(1, stat(Material.NAME_TAG,
                Component.text("Tag", NamedTextColor.YELLOW),
                plugin.getMessagesManager().deserialize(clan.getTag())));

        // Slot 2 — Leader head
        String ldrName = Optional.ofNullable(clan.getMember(clan.getLeaderUuid()))
                .map(ClanMember::getPlayerName).orElse("Unknown");
        ItemStack ldrHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta lm = (SkullMeta) ldrHead.getItemMeta();
        lm.setOwningPlayer(Bukkit.getOfflinePlayer(clan.getLeaderUuid()));
        lm.displayName(Component.text("Leader: " + ldrName, NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        ldrHead.setItemMeta(lm);
        inv.setItem(2, ldrHead);

        // Slot 3 — Members
        inv.setItem(3, stat(Material.PLAYER_HEAD,
                Component.text("Members", NamedTextColor.AQUA),
                Component.text(clan.getMemberCount() + "/" + plugin.getConfigManager().getMaxMembers(),
                        NamedTextColor.WHITE)));

        // Slot 4 — Allies
        List<Component> allyLore = clan.getAllyIds().isEmpty()
                ? List.of(Component.text("None", NamedTextColor.GRAY))
                : clan.getAllyIds().stream().map(id -> {
            Clan a = plugin.getClanCache().getById(id);
            return Component.text("• " + (a == null ? "?" : a.getName()), NamedTextColor.GREEN);
        }).collect(Collectors.toList());
        inv.setItem(4, statLore(Material.LIME_DYE,
                Component.text("Allies (" + clan.getAllyIds().size() + ")", NamedTextColor.GREEN), allyLore));

        // Slot 5 — Enemies
        List<Component> enemyLore = clan.getEnemyIds().isEmpty()
                ? List.of(Component.text("None", NamedTextColor.GRAY))
                : clan.getEnemyIds().stream().map(id -> {
            Clan e = plugin.getClanCache().getById(id);
            return Component.text("• " + (e == null ? "?" : e.getName()), NamedTextColor.RED);
        }).collect(Collectors.toList());
        inv.setItem(5, statLore(Material.RED_DYE,
                Component.text("Enemies (" + clan.getEnemyIds().size() + ")", NamedTextColor.RED), enemyLore));

        // Slot 6 — Friendly Fire
        boolean ff = clan.isFriendlyFire();
        inv.setItem(6, stat(ff ? Material.FIRE_CHARGE : Material.SNOWBALL,
                Component.text("Friendly Fire", NamedTextColor.YELLOW),
                Component.text(ff ? "Enabled" : "Disabled", ff ? NamedTextColor.RED : NamedTextColor.GREEN)));

        // Slot 7 — Created
        inv.setItem(7, stat(Material.CLOCK,
                Component.text("Founded", NamedTextColor.GRAY),
                Component.text(SDF.format(new Date(clan.getCreatedAt())), NamedTextColor.WHITE)));

        // Slot 8 — Home
        inv.setItem(8, stat(clan.isHomeSet() ? Material.COMPASS : Material.BARRIER,
                Component.text("Clan Home", NamedTextColor.AQUA),
                Component.text(clan.isHomeSet() ? "Set" : "Not Set",
                        clan.isHomeSet() ? NamedTextColor.GREEN : NamedTextColor.RED)));

        // Member heads — slots 9-44
        clan.getMembers().values().stream()
                .sorted(Comparator.comparingInt((ClanMember m) -> m.getRank().getLevel()).reversed()
                        .thenComparing(ClanMember::getPlayerName))
                .limit(36)
                .forEach(m -> {
                    for (int s = 9; s < 45; s++) {
                        if (inv.getItem(s) == null) {
                            inv.setItem(s, head(m, plugin));
                            break;
                        }
                    }
                });

        manager.register(inv, e -> {
        }); // read-only
        viewer.openInventory(inv);
    }

    private static @NotNull ItemStack head(@NotNull ClanMember m, RomClans plugin) {

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(m.getPlayerUuid()));

        boolean online = Bukkit.getPlayer(m.getPlayerUuid()) != null
                || (plugin.getRedisManager().isActive()
                && plugin.getNetworkPlayerTracker().isOnline(m.getPlayerUuid()));

        meta.displayName(Component.text(m.getPlayerName(),
                        online ? NamedTextColor.GREEN : NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Rank: ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(m.getRank().getDisplay(), NamedTextColor.GOLD))
                        .decoration(TextDecoration.ITALIC, false),
                Component.text(online ? "◆ Online" : "◇ Offline",
                                online ? NamedTextColor.GREEN : NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        skull.setItemMeta(meta);
        return skull;
    }

    private static @NotNull ItemStack stat(Material mat,
                                           @NotNull Component name,
                                           @NotNull Component lore) {

        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        m.displayName(name.decoration(TextDecoration.ITALIC, false));
        m.lore(List.of(lore.decoration(TextDecoration.ITALIC, false)));
        it.setItemMeta(m);
        return it;
    }

    private static @NotNull ItemStack statLore(Material mat,
                                               @NotNull Component name,
                                               @NotNull List<Component> lore) {

        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        m.displayName(name.decoration(TextDecoration.ITALIC, false));
        m.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
        it.setItemMeta(m);
        return it;
    }
}