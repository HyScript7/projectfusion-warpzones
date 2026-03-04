package io.github.hyscript7.projectfusion.warpzones.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.hyscript7.projectfusion.warpzones.WarpZone;
import io.github.hyscript7.projectfusion.warpzones.WarpZoneManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * /warpzones link &lt;belowZone&gt; &lt;topZone&gt;
 *
 * <p>Establishes a directional link between two named zones so that jumping while
 * inside {@code belowZone} teleports the player to {@code topZone}, and crouching
 * while inside {@code topZone} teleports them back down.
 *
 * <p>Both arguments support tab-completion from the list of registered zone names.
 */
public class WarpZoneLinkCommand {

    private WarpZoneLinkCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build(WarpZoneManager manager) {
        return Commands.literal("link")
                .then(Commands.argument("belowZone", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            manager.findAllNames().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("topZone", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    manager.findAllNames().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    String belowName = StringArgumentType.getString(ctx, "belowZone");
                                    String topName   = StringArgumentType.getString(ctx, "topZone");

                                    if (belowName.equals(topName)) {
                                        ctx.getSource().getSender().sendMessage(Component.text(
                                                "A zone cannot be linked to itself.")
                                                .color(NamedTextColor.RED));
                                        return 0;
                                    }

                                    WarpZone belowZone = manager.getWarpZone(belowName);
                                    WarpZone topZone   = manager.getWarpZone(topName);

                                    if (belowZone == null) {
                                        ctx.getSource().getSender().sendMessage(Component.text(
                                                "No warp zone named '" + belowName + "' exists.")
                                                .color(NamedTextColor.RED));
                                        return 0;
                                    }
                                    if (topZone == null) {
                                        ctx.getSource().getSender().sendMessage(Component.text(
                                                "No warp zone named '" + topName + "' exists.")
                                                .color(NamedTextColor.RED));
                                        return 0;
                                    }

                                    // Wire the link: belowZone → topZone
                                    belowZone.setNextWarpZoneUuid(topZone.getWarpZoneUuid());
                                    topZone.setPreviousWarpZoneUuid(belowZone.getWarpZoneUuid());

                                    manager.updateWarpZone(belowZone);
                                    manager.updateWarpZone(topZone);

                                    ctx.getSource().getSender().sendMessage(Component.text(
                                            "Linked '" + belowName + "' (below) → '" + topName + "' (above).")
                                            .color(NamedTextColor.GREEN));
                                    return Command.SINGLE_SUCCESS;
                                })));
    }
}
