package me.sunmc.clans.model;

public enum ClanRank {
    MEMBER(1, "Member"),
    OFFICER(2, "Officer"),
    CO_LEADER(3, "Co-Leader"),
    LEADER(4, "Leader");

    private final int level;
    private final String display;

    ClanRank(int level, String display) {
        this.level = level;
        this.display = display;
    }

    public int getLevel() {
        return level;
    }

    public String getDisplay() {
        return display;
    }

    /**
     * Returns the next rank up, or self if already LEADER.
     */
    public ClanRank promote() {
        return switch (this) {
            case MEMBER -> OFFICER;
            case OFFICER -> CO_LEADER;
            default -> this;   // CO_LEADER & LEADER cannot be promoted via /promote
        };
    }

    /**
     * Returns the next rank down, or self if already MEMBER.
     */
    public ClanRank demote() {
        return switch (this) {
            case CO_LEADER -> OFFICER;
            case OFFICER -> MEMBER;
            default -> this;
        };
    }

    public boolean canPromote() {
        return this == MEMBER || this == OFFICER;
    }

    public boolean canDemote() {
        return this == OFFICER || this == CO_LEADER;
    }

    public boolean canInvite() {
        return level >= OFFICER.level;
    }

    public boolean canKick() {
        return level >= OFFICER.level;
    }

    public boolean isLeader() {
        return this == LEADER;
    }
}