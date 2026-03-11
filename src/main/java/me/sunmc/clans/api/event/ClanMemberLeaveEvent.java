package me.sunmc.clans.api.event;

import me.sunmc.clans.api.IClan;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired BEFORE a player is removed from a clan.
 * Cancelling keeps the player in the clan.
 */
public final class ClanMemberLeaveEvent extends ClanEvent implements Cancellable {

    /**
     * Why the member is leaving.
     */
    public enum Reason {
        /**
         * Player used /clan leave voluntarily.
         */
        LEFT,
        /**
         * Player was kicked by an officer or leader.
         */
        KICKED,
        /**
         * The entire clan was disbanded (fired per-member).
         */
        DISBAND,
        /**
         * Removed by an external {@link me.sunmc.clans.api.RomClansAPI} call.
         */
        API
    }

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled;

    private final UUID playerUuid;
    private final String playerName;
    private final Reason reason;

    public ClanMemberLeaveEvent(@NotNull IClan clan, @NotNull UUID playerUuid,
                                @NotNull String playerName, @NotNull Reason reason) {

        super(clan);
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.reason = reason;
    }

    @NotNull
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    @NotNull
    public String getPlayerName() {
        return playerName;
    }

    @NotNull
    public Reason getReason() {
        return reason;
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