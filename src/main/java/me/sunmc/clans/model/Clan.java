package me.sunmc.clans.model;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Clan {

    private final UUID id;
    private final long createdAt;
    private final Map<UUID, ClanMember> members = new ConcurrentHashMap<>();
    private final Set<UUID> allyIds = ConcurrentHashMap.newKeySet();
    private final Set<UUID> enemyIds = ConcurrentHashMap.newKeySet();
    private final Set<UUID> pendingAllyReqs = ConcurrentHashMap.newKeySet(); // clans that sent US a request
    private String name;
    private String tag;           // raw MiniMessage string
    private UUID leaderUuid;
    private boolean friendlyFire;
    private String homeServerId;
    private String homeWorld;
    private double homeX, homeY, homeZ;
    private float homeYaw, homePitch;
    private boolean homeSet;

    public Clan(UUID id, String name, String tag, UUID leaderUuid,
                boolean friendlyFire, long createdAt) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.leaderUuid = leaderUuid;
        this.friendlyFire = friendlyFire;
        this.createdAt = createdAt;
    }

    public void addMember(ClanMember m) {
        members.put(m.getPlayerUuid(), m);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public boolean hasMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    public ClanMember getMember(UUID uuid) {
        return members.get(uuid);
    }

    public Map<UUID, ClanMember> getMembers() {
        return Collections.unmodifiableMap(members);
    }

    public int getMemberCount() {
        return members.size();
    }

    public boolean isLeader(UUID uuid) {
        return leaderUuid.equals(uuid);
    }

    public ClanRank getRankOf(UUID uuid) {
        ClanMember m = members.get(uuid);
        return m == null ? null : m.getRank();
    }

    /**
     * Returns true if actor has a strictly higher rank than target
     * and can therefore manage (kick/promote/demote) them.
     */
    public boolean canManage(UUID actor, UUID target) {
        ClanRank ar = getRankOf(actor);
        ClanRank tr = getRankOf(target);
        return ar != null && tr != null && ar.getLevel() > tr.getLevel();
    }

    public void addAlly(UUID clanId) {
        allyIds.add(clanId);
    }

    public void removeAlly(UUID clanId) {
        allyIds.remove(clanId);
    }

    public boolean isAlly(UUID clanId) {
        return allyIds.contains(clanId);
    }

    public Set<UUID> getAllyIds() {
        return Collections.unmodifiableSet(allyIds);
    }

    public Set<UUID> getPendingAllyReqs() {
        return Collections.unmodifiableSet(pendingAllyReqs);
    }

    public void addEnemy(UUID clanId) {
        enemyIds.add(clanId);
    }

    public void removeEnemy(UUID clanId) {
        enemyIds.remove(clanId);
    }

    public boolean isEnemy(UUID clanId) {
        return enemyIds.contains(clanId);
    }

    public Set<UUID> getEnemyIds() {
        return Collections.unmodifiableSet(enemyIds);
    }

    public void addPendingAllyReq(UUID clanId) {
        pendingAllyReqs.add(clanId);
    }

    public void removePendingAllyReq(UUID clanId) {
        pendingAllyReqs.remove(clanId);
    }

    public boolean hasPendingAllyReq(UUID clanId) {
        return pendingAllyReqs.contains(clanId);
    }

    public void setHome(@NotNull Location loc) {
        this.homeWorld = loc.getWorld().getName();
        this.homeX = loc.getX();
        this.homeY = loc.getY();
        this.homeZ = loc.getZ();
        this.homeYaw = loc.getYaw();
        this.homePitch = loc.getPitch();
        this.homeSet = true;
    }

    /**
     * Uses case-insensitive world name lookup as a fallback so that
     * world names that differ only in capitalisation still resolve correctly.
     */
    public Location buildHomeLocation(Server srv) {
        if (!homeSet || homeWorld == null) return null;
        // Primary lookup (exact match)
        World w = srv.getWorld(homeWorld);
        // Fallback: case-insensitive scan
        if (w == null) {
            for (World candidate : srv.getWorlds()) {
                if (candidate.getName().equalsIgnoreCase(homeWorld)) {
                    w = candidate;
                    break;
                }
            }
        }
        return w == null ? null : new Location(w, homeX, homeY, homeZ, homeYaw, homePitch);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String n) {
        this.name = n;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String t) {
        this.tag = t;
    }

    public UUID getLeaderUuid() {
        return leaderUuid;
    }

    public void setLeaderUuid(UUID u) {
        this.leaderUuid = u;
    }

    public boolean isFriendlyFire() {
        return friendlyFire;
    }

    public void setFriendlyFire(boolean ff) {
        this.friendlyFire = ff;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isHomeSet() {
        return homeSet;
    }

    public void setHomeSet(boolean v) {
        this.homeSet = v;
    }

    public String getHomeWorld() {
        return homeWorld;
    }

    public void setHomeWorld(String v) {
        this.homeWorld = v;
    }

    public double getHomeX() {
        return homeX;
    }

    public void setHomeX(double v) {
        this.homeX = v;
    }

    public double getHomeY() {
        return homeY;
    }

    public void setHomeY(double v) {
        this.homeY = v;
    }

    public double getHomeZ() {
        return homeZ;
    }

    public void setHomeZ(double v) {
        this.homeZ = v;
    }

    public float getHomeYaw() {
        return homeYaw;
    }

    public void setHomeYaw(float v) {
        this.homeYaw = v;
    }

    public float getHomePitch() {
        return homePitch;
    }

    public void setHomePitch(float v) {
        this.homePitch = v;
    }

    public String getHomeServerId() {
        return homeServerId;
    }

    public void setHomeServerId(String v) {
        this.homeServerId = v;
    }
}