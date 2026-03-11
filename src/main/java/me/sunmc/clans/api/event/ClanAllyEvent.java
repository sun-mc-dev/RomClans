package me.sunmc.clans.api.event;

import me.sunmc.clans.api.IClan;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired for every alliance lifecycle action between two clans.
 * Cancellable for REQUEST, ACCEPT, and REMOVE; DENY is informational only.
 * Extends {@link Event} directly (not {@link ClanEvent}) since two clans are involved.
 */
public final class ClanAllyEvent extends Event implements Cancellable {

    public enum Action {
        /**
         * Clan A sent an alliance request to clan B.
         */
        REQUEST,
        /**
         * Clan B accepted an alliance request from clan A.
         */
        ACCEPT,
        /**
         * Clan B denied an alliance request from clan A.
         */
        DENY,
        /**
         * A clan broke an existing alliance.
         */
        REMOVE
    }

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled;

    private final IClan initiator;
    private final IClan target;
    private final Action action;

    public ClanAllyEvent(@NotNull IClan initiator,
                         @NotNull IClan target,
                         @NotNull Action action) {

        this.initiator = initiator;
        this.target = target;
        this.action = action;
    }

    /**
     * The clan performing the action.
     */
    @NotNull
    public IClan getInitiator() {
        return initiator;
    }

    /**
     * The clan on the receiving end.
     */
    @NotNull
    public IClan getTarget() {
        return target;
    }

    @NotNull
    public Action getAction() {
        return action;
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