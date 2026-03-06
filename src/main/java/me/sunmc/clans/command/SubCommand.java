package me.sunmc.clans.command;

import org.bukkit.entity.Player;

import java.util.List;

public interface SubCommand {
    /**
     * Execute the sub-command. Always called from an async thread.
     */
    void execute(Player player, String[] args);

    /**
     * Provide tab-completion hints. Called from the command executor thread.
     */
    List<String> tabComplete(Player player, String[] args);

    /**
     * Bukkit permission node required to use this command, or null for none.
     */
    String getPermission();

    /**
     * Whether the player must be in a clan to run this command.
     */
    boolean requiresClan();
}