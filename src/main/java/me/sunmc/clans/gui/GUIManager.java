package me.sunmc.clans.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class GUIManager implements Listener {

    private final Map<Inventory, Consumer<InventoryClickEvent>> handlers = new ConcurrentHashMap<>();

    public void register(Inventory inv, Consumer<InventoryClickEvent> h) {
        handlers.put(inv, h);
    }

    @EventHandler
    public void onClick(@NotNull InventoryClickEvent e) {
        Consumer<InventoryClickEvent> h = handlers.get(e.getInventory());
        if (h == null) return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null || e.getClickedInventory() == null) return;
        if (!e.getClickedInventory().equals(e.getInventory())) return;
        h.accept(e);
    }

    @EventHandler
    public void onClose(@NotNull InventoryCloseEvent e) {
        handlers.remove(e.getInventory());
    }
}