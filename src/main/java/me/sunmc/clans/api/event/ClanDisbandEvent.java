package me.sunmc.clans.api.event;

import me.sunmc.clans.api.IClan;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired BEFORE a clan is removed from the cache and database.
 * The clan's member list is still intact when this event fires.
 * Cancelling prevents to disband.
 */
public final class ClanDisbandEvent extends ClanEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled;

    private final boolean adminDisband;

    public ClanDisbandEvent(@NotNull IClan clan, boolean adminDisband) {

        super(clan);
        this.adminDisband = adminDisband;
    }

    /**
     * {@code true} if triggered by {@code /clanadmin disband} rather than the leader.
     */
    public boolean isAdminDisband() {
        return adminDisband;
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