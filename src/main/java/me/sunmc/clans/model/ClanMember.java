package me.sunmc.clans.model;

import java.util.UUID;

public class ClanMember {

    private final UUID playerUuid;
    private final long joinedAt;
    private String playerName;
    private ClanRank rank;

    public ClanMember(UUID playerUuid, String playerName, ClanRank rank, long joinedAt) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.rank = rank;
        this.joinedAt = joinedAt;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String n) {
        this.playerName = n;
    }

    public ClanRank getRank() {
        return rank;
    }

    public void setRank(ClanRank r) {
        this.rank = r;
    }

    public long getJoinedAt() {
        return joinedAt;
    }
}