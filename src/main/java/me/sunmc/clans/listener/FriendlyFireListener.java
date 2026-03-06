package me.sunmc.clans.listener;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.model.Clan;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;

public class FriendlyFireListener implements Listener {

    private final RomClans plugin;

    public FriendlyFireListener(RomClans plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDamage(@NotNull EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        Clan attackerClan = plugin.getClanManager().getPlayerClan(attacker.getUniqueId());
        if (attackerClan == null) return;

        // Same clan: block if friendly fire is disabled
        if (attackerClan.hasMember(victim.getUniqueId())) {
            if (!attackerClan.isFriendlyFire()) {
                event.setCancelled(true);
                plugin.getMessagesManager().send(attacker, "ff-blocked");
            }
            return;
        }

        // Allied clan: block if the attacker's clan has friendly fire disabled
        Clan victimClan = plugin.getClanManager().getPlayerClan(victim.getUniqueId());
        if (victimClan != null && attackerClan.isAlly(victimClan.getId())) {
            if (!attackerClan.isFriendlyFire()) {
                event.setCancelled(true);
                plugin.getMessagesManager().send(attacker, "ff-blocked");
            }
        }
    }
}