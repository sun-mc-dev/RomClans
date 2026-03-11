package me.sunmc.clans.model;

import me.sunmc.clans.api.IClanMember;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ClanMember implements IClanMember {

    private final UUID playerUuid;
    private final long joinedAt;
    private String playerName;
    private ClanRank rank;

    public ClanMember(UUID playerUuid,
                      String playerName,
                      ClanRank rank,
                      long joinedAt) {

        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.rank = rank;
        this.joinedAt = joinedAt;
    }

    @Override
    @NotNull
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    @Override
    @NotNull
    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String n) {
        this.playerName = n;
    }

    @Override
    @NotNull
    public ClanRank getRank() {
        return rank;
    }

    public void setRank(ClanRank r) {
        this.rank = r;
    }

    @Override
    public long getJoinedAt() {
        return joinedAt;
    }
}