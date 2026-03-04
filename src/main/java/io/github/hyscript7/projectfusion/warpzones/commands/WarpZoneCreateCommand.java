package io.github.hyscript7.projectfusion.warpzones.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.Region;
import io.github.hyscript7.projectfusion.warpzones.WarpZone;
import io.github.hyscript7.projectfusion.warpzones.WarpZoneManager;
import io.github.hyscript7.projectfusion.warpzones.WarpZonePermissions;
import io.github.hyscript7.projectfusion.warpzones.util.YawUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /warpzones create &lt;name&gt;
 *
 * <p>Creates a new named warp zone from the player's current WorldEdit selection.
 * The player's yaw is snapped to the nearest cardinal direction and used as the
 * zone's orientation.
 *
 * <p>Requires: {@value WarpZonePermissions#ADMIN_CREATE}
 */
public class WarpZoneCreateCommand {

    private WarpZoneCreateCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build(WarpZoneManager manager) {
        return Commands.literal("create")
                .requires(src -> src.getSender().hasPermission(WarpZonePermissions.ADMIN_CREATE))
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            if (!(sender instanceof Player player)) {
                                sender.sendMessage(Component.text("Only players can create warp zones.")
                                        .color(NamedTextColor.RED));
                                return 0;
                            }

                            String name = StringArgumentType.getString(ctx, "name");

                            if (manager.nameExists(name)) {
                                player.sendMessage(Component.text(
                                        "A warp zone named '" + name + "' already exists.")
                                        .color(NamedTextColor.RED));
                                return 0;
                            }

                            Region selection = getSelectionOrNull(player);
                            if (selection == null) {
                                player.sendMessage(Component.text(
                                        "Please make a WorldEdit selection first. " +
                                        "See: https://worldedit.enginehub.org/en/latest/usage/regions/selections/")
                                        .color(NamedTextColor.RED));
                                return 0;
                            }

                            var min   = selection.getBoundingBox().getMinimumPoint();
                            var max   = selection.getBoundingBox().getMaximumPoint();
                            var world = BukkitAdapter.adapt(selection.getWorld());
                            var loc1  = new Location(world, min.x(), min.y(), min.z());
                            var loc2  = new Location(world, max.x(), max.y(), max.z());

                            var warpZone = new WarpZone(name, world, loc1, loc2,
                                    YawUtils.snapToCardinal(player.getYaw()));
                            manager.registerWarpZone(warpZone);

                            player.sendMessage(Component.text(
                                    "Warp zone '" + name + "' created!")
                                    .color(NamedTextColor.GREEN));
                            return Command.SINGLE_SUCCESS;
                        }));
    }

    private static Region getSelectionOrNull(Player player) {
        try {
            var actor        = BukkitAdapter.adapt(player);
            LocalSession session = WorldEdit.getInstance().getSessionManager().get(actor);
            if (session == null || session.getSelectionWorld() == null) return null;
            return session.getRegionSelector(session.getSelectionWorld()).getRegion();
        } catch (IncompleteRegionException e) {
            return null;
        }
    }
}
