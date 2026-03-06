package me.sunmc.clans.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.sunmc.clans.RomClans;
import me.sunmc.clans.model.ChatMode;
import me.sunmc.clans.model.Clan;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class PlayerChatListener implements Listener {

    private final RomClans plugin;

    public PlayerChatListener(RomClans plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(@NotNull AsyncChatEvent event) {
        ChatMode mode = plugin.getChatManager().getChatMode(event.getPlayer().getUniqueId());

        if (mode == ChatMode.GLOBAL) return; // pass through to normal chat pipeline

        Clan clan = plugin.getClanManager().getPlayerClan(event.getPlayer().getUniqueId());
        if (clan == null) {
            // Player's clan dissolved but their mode wasn't reset — reset silently
            plugin.getChatManager().resetMode(event.getPlayer().getUniqueId());
            return;
        }

        // Intercept the message — cancel default delivery
        event.setCancelled(true);

        if (mode == ChatMode.CLAN) {
            plugin.getChatManager().sendClanChat(event.getPlayer(), clan, event.message());
            return;
        }

        if (mode == ChatMode.ALLY) {
            if (clan.getAllyIds().isEmpty()) {
                plugin.getMessagesManager().send(event.getPlayer(), "chat-no-allies");
                // Roll back to CLAN mode
                plugin.getChatManager().cycleChatMode(
                        event.getPlayer().getUniqueId(), true, false);
            } else {
                plugin.getChatManager().sendAllyChat(event.getPlayer(), clan, event.message());
            }
        }
    }
}