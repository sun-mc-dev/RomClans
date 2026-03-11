package me.sunmc.clans.api;

import me.sunmc.clans.model.ClanRank;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Read-only view of a single clan member entry.
 */
public interface IClanMember {
    /**
     * The player's UUID.
     */
    @NotNull UUID getPlayerUuid();

    /**
     * The player's last-known username.
     */
    @NotNull String getPlayerName();

    /**
     * The player's current rank inside the clan.
     */
    @NotNull ClanRank getRank();

    /**
     * Unix timestamp (ms) when the player joined the clan.
     */
    long getJoinedAt();
}