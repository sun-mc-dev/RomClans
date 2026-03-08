package me.sunmc.clans.command.sub;

import me.sunmc.clans.RomClans;
import me.sunmc.clans.command.SubCommand;
import me.sunmc.clans.gui.ConfirmGUI;
import me.sunmc.clans.util.MiniMessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CreateSubCommand implements SubCommand {

    private static final Set<String> RESERVED = Set.of(
            "admin", "administrator", "console", "server", "moderator", "mod", "owner",
            "operator", "op", "null", "undefined", "none", "clan", "clans", "help",
            "minecraft", "plugin", "staff", "system", "<name>", "<tag>"
    );

    private final RomClans plugin;

    public CreateSubCommand(RomClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, String @NotNull [] args) {
        if (args.length < 2) {
            plugin.getMessagesManager().send(player, "create-usage");
            return;
        }
        if (plugin.getClanManager().getPlayerClan(player.getUniqueId()) != null) {
            plugin.getMessagesManager().send(player, "already-in-clan");
            return;
        }

        String name = args[0];
        String tag = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        var cfg = plugin.getConfigManager();

        if (!name.matches("[a-zA-Z0-9_]{" + cfg.getMinNameLength() + "," + cfg.getMaxNameLength() + "}")) {
            plugin.getMessagesManager().send(player, "create-invalid-name");
            return;
        }
        if (RESERVED.contains(name.toLowerCase())) {
            plugin.getMessagesManager().send(player, "create-name-reserved");
            return;
        }

        var tagResult = MiniMessageUtil.validateTag(tag, cfg.getMinTagLength(), cfg.getMaxTagLength());
        switch (tagResult) {
            case FORBIDDEN_DECORATION, FORBIDDEN_SPACE -> {
                plugin.getMessagesManager().send(player, "create-tag-forbidden");
                return;
            }
            case INVALID_LENGTH -> {
                plugin.getMessagesManager().send(player, "create-invalid-tag",
                        Map.of("min", String.valueOf(cfg.getMinTagLength()),
                                "max", String.valueOf(cfg.getMaxTagLength())));
                return;
            }
            case OK -> {
            }
        }
        String plainTag = MiniMessageUtil.strip(tag);
        if (RESERVED.contains(plainTag.toLowerCase())) {
            plugin.getMessagesManager().send(player, "create-name-reserved");
            return;
        }

        if (plugin.getClanCache().existsByName(name)) {
            plugin.getMessagesManager().send(player, "create-name-taken");
            return;
        }

        // Open confirmation GUI on entity thread
        plugin.getFoliaScheduler().entity(player, () -> openConfirm(player, name, tag));
    }

    private void openConfirm(Player player, String name, String tag) {
        ItemStack info = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta m = info.getItemMeta();
        m.displayName(Component.text("Create Clan?", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        m.lore(List.of(
                Component.text("Name: " + name, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
                plugin.getMessagesManager().deserialize(tag).decoration(TextDecoration.ITALIC, false)
        ));
        info.setItemMeta(m);

        ConfirmGUI.open(player,
                Component.text("Create Clan: " + name, NamedTextColor.GREEN),
                info,
                () -> plugin.getServer().getAsyncScheduler().runNow(plugin, t ->
                        plugin.getClanManager().createClan(name, tag, player.getUniqueId(), player.getName())
                                .thenAccept(clan -> plugin.getMessagesManager().send(player, "create-success",
                                        Map.of("name", clan.getName())))
                                .exceptionally(ex -> {
                                    plugin.getMessagesManager().send(player, "create-name-taken");
                                    return null;
                                })),
                plugin.getGuiManager());
    }

    @Override
    public List<String> tabComplete(Player p, String @NotNull [] a) {
        if (a.length == 1) return List.of("<name>");
        if (a.length == 2) return List.of("<tag>");
        return List.of();
    }

    @Override
    public String getPermission() {
        return null;
    }

    @Override
    public boolean requiresClan() {
        return false;
    }
}