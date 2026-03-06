package me.sunmc.clans.command.sub;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.command.SubCommand;
import me.sunmc.clans.model.ChatMode;
import me.sunmc.clans.model.Clan;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ChatSubCommand implements SubCommand {

    private final RomClans plugin;

    public ChatSubCommand(RomClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(@NotNull Player player, String[] args) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        boolean inClan = clan != null;
        boolean hasAllies = inClan && !clan.getAllyIds().isEmpty();

        ChatMode next = plugin.getChatManager().cycleChatMode(player.getUniqueId(), inClan, hasAllies);

        // Ally mode requires both being in a clan AND having allies
        if (next == ChatMode.ALLY && !inClan) {
            plugin.getMessagesManager().send(player, "chat-not-in-clan-for-ally");
            return;
        }
        if (next == ChatMode.ALLY && !hasAllies) {
            // Skip to global
            plugin.getChatManager().cycleChatMode(player.getUniqueId(), false, false);
            next = ChatMode.GLOBAL;
        }

        String msgKey = switch (next) {
            case CLAN -> "chat-mode-clan";
            case ALLY -> "chat-mode-ally";
            case GLOBAL -> "chat-mode-global";
        };
        plugin.getMessagesManager().send(player, msgKey);
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
        return false;
    }
}