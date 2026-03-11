package me.sunmc.clans.api.event;

import me.sunmc.clans.api.IClan;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired BEFORE a player is added to a clan.
 * Cancelling prevents the player from joining.
 */
public final class ClanMemberJoinEvent extends ClanEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled;

    private final UUID playerUuid;
    private final String playerName;

    public ClanMemberJoinEvent(@NotNull IClan clan, @NotNull UUID playerUuid,
                               @NotNull String playerName) {

        super(clan);
        this.playerUuid = playerUuid;
        this.playerName = playerName;
    }

    @NotNull
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    @NotNull
    public String getPlayerName() {
        return playerName;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean c) {
        this.cancelled = c;
    }

    @Override
    @NotNull
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}