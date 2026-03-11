package me.sunmc.clans.api.event;

import me.sunmc.clans.api.IClan;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired BEFORE the friendly-fire flag is toggled.
 * Cancelling preserves the current state.
 */
public final class ClanFriendlyFireToggleEvent extends ClanEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled;

    private final boolean newState;

    public ClanFriendlyFireToggleEvent(@NotNull IClan clan, boolean newState) {

        super(clan);
        this.newState = newState;
    }

    /**
     * The state that will be applied if not cancelled ({@code true} = FF on).
     */
    public boolean getNewState() {
        return newState;
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