package me.sunmc.clans.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.sunmc.clans.RomClans;
import me.sunmc.clans.model.Clan;
import me.sunmc.clans.model.ClanMember;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClansPlaceholderExpansion extends PlaceholderExpansion {

    private final RomClans plugin;

    public ClansPlaceholderExpansion(RomClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "romclans";
    }

    @Override
    public @NotNull String getAuthor() {
        return "GigiAki";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    /**
     * Available placeholders:
     * %romclans_clan_name%            — Clan name or "None"
     * %romclans_clan_tag%             — Plain-text clan tag or ""
     * %romclans_clan_tag_colored%     — MiniMessage-parsed tag as legacy string
     * %romclans_clan_rank%            — Player's rank display name or "None"
     * %romclans_clan_members%         — Current member count
     * %romclans_clan_max_members%     — Configured max members
     * %romclans_clan_allies%          — Number of allied clans
     * %romclans_clan_enemies%         — Number of enemy clans
     * %romclans_clan_friendly_fire%   — "true" / "false"
     * %romclans_clan_leader%          — Leader name or "Unknown"
     * %romclans_in_clan%              — "true" / "false"
     */
    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());

        return switch (params) {
            case "clan_name" -> clan == null ? "None" : clan.getName();

            case "clan_tag" -> clan == null ? "" : plugin.getMessagesManager().stripTags(clan.getTag());

            case "clan_tag_colored" -> {
                if (clan == null) yield "";
                yield PlainTextComponentSerializer.plainText()
                        .serialize(plugin.getMessagesManager().getMiniMessage()
                                .deserialize(clan.getTag()));
            }

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
                ClanMember leader = clan.getMember(clan.getLeaderUuid());
                yield leader == null ? "Unknown" : leader.getPlayerName();
            }

            case "in_clan" -> clan == null ? "false" : "true";

            default -> null; // unknown placeholder, let PAPI handle it
        };
    }
}