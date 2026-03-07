package me.sunmc.clans.database.impl.redis;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import me.sunmc.clans.RomClans;
import me.sunmc.clans.model.Clan;
import me.sunmc.clans.model.ClanMember;
import me.sunmc.clans.model.ClanRank;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.logging.Level;

public class RedisSubscriber extends RedisPubSubAdapter<String, String> {

    private final RomClans plugin;
    private final String ownServerId;

    public RedisSubscriber(RomClans plugin, String ownServerId) {
        this.plugin = plugin;
        this.ownServerId = ownServerId;
    }

    @Override
    public void message(String channel, String message) {
        try {
            JsonObject j = JsonParser.parseString(message).getAsJsonObject();
            if (ownServerId.equals(j.has("serverId") ? j.get("serverId").getAsString() : "")) return;
            switch (channel) {
                case RedisManager.CHAN_CHAT -> plugin.getChatManager().receiveRedisChat(
                        j.get("type").getAsString(), j.get("clanId").getAsString(),
                        j.get("senderName").getAsString(), j.get("clanTag").getAsString(),
                        j.get("message").getAsString());
                case RedisManager.CHAN_INVITE -> plugin.getInviteManager().receiveRedisInvite(
                        j.get("clanId").getAsString(), j.get("clanName").getAsString(),
                        j.get("inviterUuid").getAsString(), j.get("inviteeUuid").getAsString(),
                        j.get("inviteeName").getAsString());
                case RedisManager.CHAN_SYNC -> handleSync(j);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Redis message error on " + channel + ": " + message, e);
        }
    }

    private void handleSync(@NotNull JsonObject j) {
        String type = j.get("type").getAsString();
        switch (type) {
            case "RELATION_UPDATE" -> plugin.getRelationManager().applyRedisRelationUpdate(
                    j.get("clanA").getAsString(), j.get("clanB").getAsString(),
                    j.get("relType").getAsString(), j.get("action").getAsString());

            case "FF_TOGGLE" -> {
                Clan c = byClanId(j);
                if (c != null) c.setFriendlyFire(j.get("ff").getAsBoolean());
            }
            case "RETAG" -> {
                Clan c = byClanId(j);
                if (c != null) c.setTag(j.get("tag").getAsString());
            }
            case "RANK_UPDATE" -> {
                Clan c = byClanId(j);
                if (c == null) return;
                ClanMember m = c.getMember(UUID.fromString(j.get("playerUuid").getAsString()));
                if (m != null) m.setRank(ClanRank.valueOf(j.get("rank").getAsString()));
            }
            case "MEMBER_ADD" -> {
                Clan c = byClanId(j);
                if (c == null) return;
                UUID pu = UUID.fromString(j.get("playerUuid").getAsString());
                if (!c.hasMember(pu)) {
                    c.addMember(new ClanMember(pu, j.get("playerName").getAsString(),
                            ClanRank.valueOf(j.get("rank").getAsString()), System.currentTimeMillis()));
                    plugin.getClanCache().addPlayerToIndex(pu, c.getId());
                }
            }
            case "MEMBER_REMOVE" -> {
                Clan c = byClanId(j);
                if (c == null) return;
                UUID pu = UUID.fromString(j.get("playerUuid").getAsString());
                c.removeMember(pu);
                plugin.getClanCache().removePlayerFromIndex(pu);
                plugin.getChatManager().resetMode(pu);
            }
            case "DISBAND" -> {
                UUID cid = UUID.fromString(j.get("clanId").getAsString());
                Clan c = plugin.getClanCache().getById(cid);
                if (c != null) c.getMembers().keySet().forEach(plugin.getChatManager()::resetMode);
                plugin.getClanCache().remove(cid);
            }
            case "TRANSFER" -> {
                Clan c = byClanId(j);
                if (c == null) return;
                UUID newL = UUID.fromString(j.get("newLeader").getAsString());
                UUID oldL = UUID.fromString(j.get("oldLeader").getAsString());
                c.setLeaderUuid(newL);
                ClanMember nm = c.getMember(newL);
                if (nm != null) nm.setRank(ClanRank.LEADER);
                ClanMember om = c.getMember(oldL);
                if (om != null) om.setRank(ClanRank.CO_LEADER);
            }
            case "HOME_SET" -> {
                Clan c = byClanId(j);
                if (c == null) return;
                c.setHomeWorld(j.get("world").getAsString());
                c.setHomeX(j.get("x").getAsDouble());
                c.setHomeY(j.get("y").getAsDouble());
                c.setHomeZ(j.get("z").getAsDouble());
                c.setHomeYaw(j.get("yaw").getAsFloat());
                c.setHomePitch(j.get("pitch").getAsFloat());
                c.setHomeSet(true);
            }
            case "CACHE_INVALIDATE" -> plugin.getClanManager().loadAll();
        }
    }

    private Clan byClanId(@NotNull JsonObject j) {
        return plugin.getClanCache().getById(UUID.fromString(j.get("clanId").getAsString()));
    }
}