package me.sunmc.clans.api;

import me.sunmc.clans.model.ClanRank;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Read-only view of a clan exposed by the RomClans API.
 * For mutations use {@link RomClansAPI}.
 *
 * <p>Note on method naming: {@link #findMember} and {@link #getAllMembers} use distinct
 * names to avoid a return-type clash with the internal {@code Clan} map accessors.
 */
public interface IClan {

    /**
     * Unique ID of the clan.
     */
    @NotNull UUID getId();

    /**
     * Plain-text display name.
     */
    @NotNull String getName();

    /**
     * Raw MiniMessage tag string, e.g. {@code "<red>CLN</red>"}.
     * Render with {@code MiniMessage.miniMessage().deserialize(clan.getTag())}.
     */
    @NotNull String getTag();

    /**
     * UUID of the current leader.
     */
    @NotNull UUID getLeaderUuid();

    /**
     * Whether friendly fire is on for this clan.
     */
    boolean isFriendlyFire();

    /**
     * Unix timestamp (ms) of clan creation.
     */
    long getCreatedAt();

    /**
     * Number of members currently in the clan.
     */
    int getMemberCount();

    /**
     * Whether a home has been set.
     */
    boolean isHomeSet();

    /**
     * {@code true} if {@code playerUuid} is the clan leader.
     */
    boolean isLeader(@NotNull UUID playerUuid);

    /**
     * {@code true} if {@code playerUuid} is any member of this clan.
     */
    boolean hasMember(@NotNull UUID playerUuid);

    /**
     * Returns the member record for the given UUID, or empty if not a member.
     * Named {@code findMember} to avoid a return-type conflict with the internal map accessor.
     */
    @NotNull Optional<? extends IClanMember> findMember(@NotNull UUID playerUuid);

    /**
     * Unmodifiable collection of all members.
     * Named {@code getAllMembers} to avoid a return-type conflict with the internal map accessor.
     */
    @NotNull Collection<? extends IClanMember> getAllMembers();

    /**
     * Unmodifiable set of allied clan IDs.
     */
    @NotNull Set<UUID> getAllyIds();

    /**
     * Unmodifiable set of enemy clan IDs.
     */
    @NotNull Set<UUID> getEnemyIds();

    /**
     * Unmodifiable set of pending incoming alliance-request clan IDs.
     */
    @NotNull Set<UUID> getPendingAllyReqs();

    /**
     * Rank of the given player, or {@code null} if not a member.
     */
    @Nullable ClanRank getRankOf(@NotNull UUID playerUuid);

    /**
     * {@code true} if {@code clanId} is an ally of this clan.
     */
    boolean isAlly(@NotNull UUID clanId);

    /**
     * {@code true} if {@code clanId} is an enemy of this clan.
     */
    boolean isEnemy(@NotNull UUID clanId);

    /**
     * Builds and returns the home {@link Location}, or empty if no home is set
     * or the home world is not loaded on this server.
     */
    @NotNull Optional<Location> getHomeLocation();

    /**
     * The {@code server-id} from config.yml where the home is stored, or {@code null}.
     */
    @Nullable String getHomeServerId();
}