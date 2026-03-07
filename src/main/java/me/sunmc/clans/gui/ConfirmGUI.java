package me.sunmc.clans.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public final class ConfirmGUI {

    private ConfirmGUI() {
    }

    /**
     * Opens a 27-slot confirmation/cancel GUI.
     * onConfirm runs on the async executor — dispatch to entity thread if needed.
     */
    public static void open(Player p, Component title, ItemStack info,
                            Runnable onConfirm, GUIManager manager) {
        Inventory inv = Bukkit.createInventory(null, 27, title);
        ItemStack border = pane(Material.GRAY_STAINED_GLASS_PANE, Component.empty());
        for (int i = 0; i < 27; i++) inv.setItem(i, border);
        inv.setItem(11, pane(Material.RED_STAINED_GLASS_PANE,
                Component.text("✗ Cancel", NamedTextColor.RED)));
        inv.setItem(13, info);
        inv.setItem(15, pane(Material.GREEN_STAINED_GLASS_PANE,
                Component.text("✓ Confirm", NamedTextColor.GREEN)));
        manager.register(inv, e -> {
            int s = e.getRawSlot();
            if (s == 11) p.closeInventory();
            else if (s == 15) {
                p.closeInventory();
                onConfirm.run();
            }
        });
        p.openInventory(inv);
    }

    public static @NotNull ItemStack pane(Material mat, @NotNull Component name) {
        ItemStack i = new ItemStack(mat);
        ItemMeta m = i.getItemMeta();
        m.displayName(name.decoration(TextDecoration.ITALIC, false));
        i.setItemMeta(m);
        return i;
    }
}