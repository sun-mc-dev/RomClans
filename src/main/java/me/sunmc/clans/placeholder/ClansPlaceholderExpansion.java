package me.sunmc.clans.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.sunmc.clans.RomClans;
import me.sunmc.clans.model.Clan;
import me.sunmc.clans.model.ClanMember;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClansPlaceholderExpansion extends PlaceholderExpansion {

    private final RomClans plugin;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    public ClansPlaceholderExpansion(RomClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "romclans";
    }

    @Override
    public @NotNull String getAuthor() {
        return "SunMC";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        return switch (params) {
            case "clan_name" -> clan == null ? "None" : clan.getName();
            case "clan_tag" -> clan == null ? "" : plugin.getMessagesManager().stripTags(clan.getTag());
            // FIX: was using PlainTextSerializer (strips colors). Now uses § legacy codes that PAPI/chat plugins understand.
            case "clan_tag_colored" -> clan == null ? "" :
                    LEGACY.serialize(plugin.getMessagesManager().getMiniMessage().deserialize(clan.getTag()));
            case "clan_rank" -> {
                if (clan == null) yield "None";
                ClanMember m = clan.getMember(player.getUniqueId());
                yield m == null ? "None" : m.getRank().getDisplay();
            }
            case "clan_members" -> clan == null ? "0" : String.valueOf(clan.getMemberCount());
            case "clan_max_members" -> String.valueOf(plugin.getConfigManager().getMaxMembers());
            case "clan_allies" -> clan == null ? "0" : String.valueOf(clan.getAllyIds().size());
            case "clan_enemies" -> clan == null ? "0" : String.valueOf(clan.getEnemyIds().size());
            case "clan_friendly_fire" -> clan == null ? "false" : String.valueOf(clan.isFriendlyFire());
            case "clan_leader" -> {
                if (clan == null) yield "None";
                ClanMember l = clan.getMember(clan.getLeaderUuid());
                yield l == null ? "Unknown" : l.getPlayerName();
            }
            case "in_clan" -> clan == null ? "false" : "true";
            default -> null;
        };
    }
}