package me.sunmc.clans.util;

import me.sunmc.clans.RomClans;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * Thin wrapper around Folia's scheduler APIs.
 * Never uses the deprecated BukkitScheduler.
 */
public class FoliaScheduler {

    private final RomClans plugin;

    public FoliaScheduler(RomClans plugin) {
        this.plugin = plugin;
    }

    /**
     * Run a task on the async scheduler (no specific region needed).
     */
    public void async(Runnable task) {
        plugin.getServer().getAsyncScheduler().runNow(plugin, $ -> task.run());
    }

    /**
     * Run a delayed task on the async scheduler.
     */
    public void asyncDelayed(Runnable task, long delay, TimeUnit unit) {
        plugin.getServer().getAsyncScheduler().runDelayed(plugin, $ -> task.run(), delay, unit);
    }

    /**
     * Run on the global region scheduler (world-independent).
     */
    public void global(Runnable task) {
        plugin.getServer().getGlobalRegionScheduler().run(plugin, $ -> task.run());
    }

    /**
     * Run on the region owning the given location.
     */
    public void region(Location loc, Runnable task) {
        plugin.getServer().getRegionScheduler().run(plugin, loc, $ -> task.run());
    }

    /**
     * Run a delayed task on the region owning the given location.
     */
    public void regionDelayed(Location loc, Runnable task, long delayTicks) {
        plugin.getServer().getRegionScheduler().runDelayed(plugin, loc, $ -> task.run(), delayTicks);
    }

    /**
     * Run on the entity's owning region thread. retiredCallback fires if entity is removed.
     */
    public void entity(@NotNull Entity entity, Runnable task) {
        entity.getScheduler().run(plugin, $ -> task.run(), null);
    }

    /**
     * Run a delayed task on the entity's owning region thread.
     */
    public void entityDelayed(@NotNull Entity entity, Runnable task, Runnable retired, long delayTicks) {
        entity.getScheduler().runDelayed(plugin, $ -> task.run(), retired, delayTicks);
    }
}