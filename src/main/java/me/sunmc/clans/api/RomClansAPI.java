package me.sunmc.clans.api;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.api.event.ClanMemberLeaveEvent;
import me.sunmc.clans.model.Clan;
import me.sunmc.clans.model.ClanRank;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Public entry-point for the RomClans plugin API.
 *
 * <h2>JitPack (Maven)</h2>
 * <pre>{@code
 * <repositories>
 *   <repository>
 *     <id>jitpack.io</id>
 *     <url>https://jitpack.io</url>
 *   </repository>
 * </repositories>
 * <dependencies>
 *   <dependency>
 *     <groupId>com.github.sun-mc-dev</groupId>
 *     <artifactId>RomClans</artifactId>
 *     <version>TAG_OR_COMMIT</version>
 *     <scope>provided</scope>
 *   </dependency>
 * </dependencies>
 * }</pre>
 *
 * <h2>Quick-start</h2>
 * <pre>{@code
 * // Query
 * Optional<IClan> clan = RomClansAPI.getPlayerClan(player.getUniqueId());
 * clan.ifPresent(c -> player.sendMessage("Clan: " + c.getName()));
 *
 * // Mutate (async)
 * RomClansAPI.createClan("Warriors", "<red>WAR</red>", player.getUniqueId(), player.getName())
 *            .thenAccept(c -> player.sendMessage("Created " + c.getName()));
 *
 * // Events
 * @EventHandler
 * public void on(ClanCreateEvent e) {
 *     Bukkit.broadcastMessage("New clan: " + e.getClan().getName());
 * }
 * }</pre>
 *
 * <p>All mutation methods are asynchronous. Never block the main thread waiting on them.
 */
public final class RomClansAPI {

    private static RomClans plugin;

    private RomClansAPI() {
    }


    /**
     * Called by {@code RomClans.onEnable()}. Do NOT call this yourself.
     */
    public static void init(@NotNull RomClans instance) {
        plugin = instance;
    }

    /**
     * Called by {@code RomClans.onDisable()}. Do NOT call this yourself.
     */
    public static void shutdown() {
        plugin = null;
    }

    /**
     * Returns {@code true} while the RomClans plugin is loaded and enabled.
     */
    public static boolean isAvailable() {
        return plugin != null;
    }

    /**
     * Returns the clan with the given ID, or empty if it does not exist in the cache.
     */
    @NotNull
    public static Optional<IClan> getClan(@NotNull UUID clanId) {
        return Optional.ofNullable(require().getClanCache().getById(clanId));
    }

    /**
     * Returns the clan with the given name (case-insensitive), or empty.
     */
    @NotNull
    public static Optional<IClan> getClanByName(@NotNull String name) {
        return Optional.ofNullable(require().getClanCache().getByName(name));
    }

    /**
     * Returns the clan the player belongs to, or empty if they are clanless.
     */
    @NotNull
    public static Optional<IClan> getPlayerClan(@NotNull UUID playerUuid) {
        return Optional.ofNullable(require().getClanManager().getPlayerClan(playerUuid));
    }

    /**
     * {@code true} if the player is currently in any clan.
     */
    public static boolean isInClan(@NotNull UUID playerUuid) {
        return require().getClanManager().getPlayerClan(playerUuid) != null;
    }

    /**
     * Snapshot of every clan in the in-memory cache.
     * The collection is unmodifiable but its contents reflect live state.
     */
    @NotNull
    public static Collection<? extends IClan> getAllClans() {
        return require().getClanCache().getAll();
    }

    /**
     * {@code true} if the two clans are mutual allies.
     */
    public static boolean areAllies(@NotNull UUID clanAId, @NotNull UUID clanBId) {
        return require().getRelationManager().areAllies(clanAId, clanBId);
    }

    /**
     * {@code true} if clan A has declared clan B an enemy.
     */
    public static boolean areEnemies(@NotNull UUID clanAId, @NotNull UUID clanBId) {
        return require().getRelationManager().areEnemies(clanAId, clanBId);
    }

    /**
     * Creates a new clan.
     * Fires {@link me.sunmc.clans.api.event.ClanCreateEvent} (cancellable).
     *
     * @throws java.util.concurrent.CompletionException wrapping
     *                                                  {@link IllegalStateException} if the event is cancelled, the name is
     *                                                  already taken, or a database error occurs.
     */
    @NotNull
    public static CompletableFuture<IClan> createClan(@NotNull String name,
                                                      @NotNull String tag,
                                                      @NotNull UUID leaderUuid,
                                                      @NotNull String leaderName) {
        return require().getClanManager()
                .createClan(name, tag, leaderUuid, leaderName)
                .thenApply(c -> (IClan) c);
    }

    /**
     * Disbands a clan, removing it from the cache and database.
     * Fires {@link me.sunmc.clans.api.event.ClanDisbandEvent} (cancellable).
     */
    @NotNull
    public static CompletableFuture<Void> disbandClan(@NotNull IClan clan) {
        return require().getClanManager().disbandClan(internal(clan), false);
    }

    /**
     * Adds a player to a clan as a regular member.
     * Fires {@link me.sunmc.clans.api.event.ClanMemberJoinEvent} (cancellable).
     */
    @NotNull
    public static CompletableFuture<Void> addMember(@NotNull IClan clan,
                                                    @NotNull UUID playerUuid,
                                                    @NotNull String playerName) {
        return require().getClanManager().addMember(internal(clan), playerUuid, playerName);
    }

    /**
     * Removes a player from a clan (API-initiated removal).
     * Fires {@link ClanMemberLeaveEvent} with reason {@link ClanMemberLeaveEvent.Reason#API}
     * (cancellable).
     */
    @NotNull
    public static CompletableFuture<Void> removeMember(@NotNull IClan clan, @NotNull UUID playerUuid) {
        return require().getClanManager()
                .removeMember(internal(clan), playerUuid, ClanMemberLeaveEvent.Reason.API);
    }

    /**
     * Changes a member's rank.
     * Fires {@link me.sunmc.clans.api.event.ClanRankChangeEvent} (cancellable).
     */
    @NotNull
    public static CompletableFuture<Void> setMemberRank(@NotNull IClan clan,
                                                        @NotNull UUID playerUuid,
                                                        @NotNull ClanRank newRank) {
        return require().getClanManager().updateMemberRank(internal(clan), playerUuid, newRank);
    }

    /**
     * Transfers clan leadership to another member.
     * Fires {@link me.sunmc.clans.api.event.ClanLeaderTransferEvent} (cancellable).
     */
    @NotNull
    public static CompletableFuture<Void> transferLeadership(@NotNull IClan clan,
                                                             @NotNull UUID newLeaderUuid) {
        return require().getClanManager().transferLeadership(internal(clan), newLeaderUuid);
    }

    @NotNull
    private static RomClans require() {
        if (plugin == null) throw new IllegalStateException(
                "RomClansAPI is not available — is the RomClans plugin enabled?");
        return plugin;
    }

    @NotNull
    private static Clan internal(@NotNull IClan clan) {
        if (clan instanceof Clan c) return c;
        throw new IllegalArgumentException(
                "IClan is not a RomClans Clan instance. Do not create your own IClan implementations.");
    }
}