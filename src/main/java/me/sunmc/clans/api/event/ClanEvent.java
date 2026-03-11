package me.sunmc.clans.api.event;

import me.sunmc.clans.api.IClan;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for all RomClans Bukkit events.
 * All events are fired on the global region thread (Folia-safe).
 */
public abstract class ClanEvent extends Event {

    private final IClan clan;

    protected ClanEvent(@NotNull IClan clan) {
        this.clan = clan;
    }

    /**
     * The clan involved in this event.
     */
    @NotNull
    public IClan getClan() {
        return clan;
    }
}