package me.sunmc.clans.api.event;

import me.sunmc.clans.api.IClan;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired BEFORE leadership of a clan is transferred to another member.
 * Cancelling keeps the current leader.
 */
public final class ClanLeaderTransferEvent extends ClanEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled;

    private final UUID oldLeaderUuid;
    private final UUID newLeaderUuid;

    public ClanLeaderTransferEvent(@NotNull IClan clan,
                                   @NotNull UUID oldLeaderUuid,
                                   @NotNull UUID newLeaderUuid) {

        super(clan);
        this.oldLeaderUuid = oldLeaderUuid;
        this.newLeaderUuid = newLeaderUuid;
    }

    @NotNull
    public UUID getOldLeaderUuid() {
        return oldLeaderUuid;
    }

    @NotNull
    public UUID getNewLeaderUuid() {
        return newLeaderUuid;
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