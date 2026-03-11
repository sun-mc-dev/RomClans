package me.sunmc.clans.api.event;

import me.sunmc.clans.api.IClan;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired BEFORE a clan's MiniMessage tag is changed.
 * You may override the tag via {@link #setNewTag(String)}.
 * Cancelling preserves the current tag.
 */
public final class ClanTagChangeEvent extends ClanEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled;

    private final String oldTag;
    private String newTag;

    public ClanTagChangeEvent(@NotNull IClan clan,
                              @NotNull String oldTag,
                              @NotNull String newTag) {

        super(clan);
        this.oldTag = oldTag;
        this.newTag = newTag;
    }

    @NotNull
    public String getOldTag() {
        return oldTag;
    }

    @NotNull
    public String getNewTag() {
        return newTag;
    }

    /**
     * Override the tag that will be stored (must pass MiniMessage validation).
     */
    public void setNewTag(@NotNull String tag) {
        this.newTag = tag;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean c) {
        this.cancelled = c;
    }

    @Override
    @NotNull
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}