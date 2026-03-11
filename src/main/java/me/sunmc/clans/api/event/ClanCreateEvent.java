package me.sunmc.clans.api.event;

import me.sunmc.clans.api.IClan;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired BEFORE a new clan is written to the database.
 * Cancelling this event aborts the creation entirely.
 */
public final class ClanCreateEvent extends ClanEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled;

    private final UUID creatorUuid;
    private final String creatorName;

    public ClanCreateEvent(@NotNull IClan clan, @NotNull UUID creatorUuid,
                           @NotNull String creatorName) {

        super(clan);
        this.creatorUuid = creatorUuid;
        this.creatorName = creatorName;
    }

    /**
     * UUID of the player who triggered /clan create (or the API caller).
     */
    @NotNull
    public UUID getCreatorUuid() {
        return creatorUuid;
    }

    /**
     * Username of the creating player.
     */
    @NotNull
    public String getCreatorName() {
        return creatorName;
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