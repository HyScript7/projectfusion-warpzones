package io.github.hyscript7.projectfusion.warpzones.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.hyscript7.projectfusion.warpzones.WarpZone;
import io.github.hyscript7.projectfusion.warpzones.WarpZoneManager;
import io.github.hyscript7.projectfusion.warpzones.util.LocationUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

/**
 * /warpzones list
 *
 * <p>Displays every registered warp zone with its name, bounding-box corners,
 * world, and link status.
 */
public class WarpZoneListCommand {

    private WarpZoneListCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build(WarpZoneManager manager) {
        return Commands.literal("list")
                .executes(ctx -> {
                    List<WarpZone> zones = manager.findAll();

                    if (zones.isEmpty()) {
                        ctx.getSource().getSender().sendMessage(
                                Component.text("No warp zones have been registered yet.")
                                        .color(NamedTextColor.YELLOW));
                        return Command.SINGLE_SUCCESS;
                    }

                    Component message = Component.text("=== Warp Zones (" + zones.size() + ") ===")
                            .color(NamedTextColor.GOLD);

                    for (WarpZone zone : zones) {
                        String linkInfo = buildLinkInfo(zone, manager);
                        message = message.append(Component.newline())
                                .append(Component.text("• ").color(NamedTextColor.GRAY))
                                .append(Component.text(zone.getName()).color(NamedTextColor.AQUA))
                                .append(Component.text(" in " + zone.getWorld().getName())
                                        .color(NamedTextColor.GRAY))
                                .append(Component.newline())
                                .append(Component.text("  ("
                                        + LocationUtil.prettyString(zone.getCorner1())
                                        + ") ↔ ("
                                        + LocationUtil.prettyString(zone.getCorner2())
                                        + ")").color(NamedTextColor.DARK_GRAY))
                                .append(Component.text(linkInfo).color(NamedTextColor.YELLOW));
                    }

                    ctx.getSource().getSender().sendMessage(message);
                    return Command.SINGLE_SUCCESS;
                });
    }

    /** Returns a short human-readable summary of the zone's links, or an empty string. */
    private static String buildLinkInfo(WarpZone zone, WarpZoneManager manager) {
        StringBuilder sb = new StringBuilder();

        if (zone.getPreviousWarpZoneUuid() != null) {
            WarpZone below = manager.getWarpZone(zone.getPreviousWarpZoneUuid());
            sb.append("  ↓ ").append(below != null ? below.getName() : "<missing>");
        }
        if (zone.getNextWarpZoneUuid() != null) {
            WarpZone above = manager.getWarpZone(zone.getNextWarpZoneUuid());
            sb.append("  ↑ ").append(above != null ? above.getName() : "<missing>");
        }

        return sb.toString();
    }
}
