package me.sunmc.clans.util;

import me.sunmc.clans.RomClans;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Fires Bukkit events from async threads in a Folia-safe way.
 * Every event is dispatched on the global region scheduler to ensure
 * listeners that touch world state do not encounter region violations.
 */
public final class EventUtil {

    private EventUtil() {
    }

    /**
     * Schedules {@code event} on the global region thread, fires it, and
     * returns a {@link CompletableFuture} that resolves with the same event
     * instance after all handlers have run.
     */
    @NotNull
    public static <E extends Event> CompletableFuture<E> call(@NotNull RomClans plugin, @NotNull E event) {
        CompletableFuture<E> future = new CompletableFuture<>();
        plugin.getServer().getGlobalRegionScheduler().run(plugin, $ -> {
            plugin.getServer().getPluginManager().callEvent(event);
            future.complete(event);
        });
        return future;
    }

    /**
     * Returns {@code true} when the event is either not {@link Cancellable}
     * or was not cancelled by any handler.
     */
    public static boolean notCancelled(@NotNull Event event) {
        return !(event instanceof Cancellable c) || !c.isCancelled();
    }
}