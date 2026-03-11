package me.sunmc.clans.api.event;

import me.sunmc.clans.api.IClan;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when an enemy relationship is added or removed by the declaring clan.
 * Cancellable for both ADD and REMOVE.
 */
public final class ClanEnemyEvent extends Event implements Cancellable {

    public enum Action {ADD, REMOVE}

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled;

    private final IClan declarer;
    private final IClan target;
    private final Action action;

    public ClanEnemyEvent(@NotNull IClan declarer,
                          @NotNull IClan target,
                          @NotNull Action action) {

        this.declarer = declarer;
        this.target = target;
        this.action = action;
    }

    @NotNull
    public IClan getDeclarer() {
        return declarer;
    }

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