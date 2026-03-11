package me.sunmc.clans.api.event;

import me.sunmc.clans.api.IClan;
import me.sunmc.clans.model.ClanRank;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired BEFORE a member's rank is promoted, demoted, or changed via transfer.
 * Cancelling preserves the current rank.
 */
public final class ClanRankChangeEvent extends ClanEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled;

    private final UUID playerUuid;
    private final String playerName;
    private final ClanRank oldRank;
    private final ClanRank newRank;

    public ClanRankChangeEvent(@NotNull IClan clan, @NotNull UUID playerUuid,
                               @NotNull String playerName,
                               @NotNull ClanRank oldRank, @NotNull ClanRank newRank) {

        super(clan);
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.oldRank = oldRank;
        this.newRank = newRank;
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
    public ClanRank getOldRank() {
        return oldRank;
    }

    @NotNull
    public ClanRank getNewRank() {
        return newRank;
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