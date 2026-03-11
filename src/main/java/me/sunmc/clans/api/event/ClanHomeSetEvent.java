package me.sunmc.clans.api.event;

import me.sunmc.clans.api.IClan;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired BEFORE the clan home is stored.
 * You may override the destination via {@link #setNewHome(Location)}.
 * Cancelling aborts the home-set operation.
 */
public final class ClanHomeSetEvent extends ClanEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled;
    private Location newHome;

    public ClanHomeSetEvent(@NotNull IClan clan,
                            @NotNull Location newHome) {

        super(clan);
        this.newHome = newHome;
    }

    @NotNull
    public Location getNewHome() {
        return newHome;
    }

    /**
     * Replace the location that will be stored.
     */
    public void setNewHome(@NotNull Location loc) {
        this.newHome = loc;
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