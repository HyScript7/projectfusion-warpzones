package io.github.hyscript7.projectfusion.warpzones.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.hyscript7.projectfusion.warpzones.WarpZone;
import io.github.hyscript7.projectfusion.warpzones.WarpZoneManager;
import io.github.hyscript7.projectfusion.warpzones.WarpZonePermissions;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * /warpzones delete &lt;name&gt;
 *
 * <p>Deletes a named warp zone and repairs the link chain of any adjacent zones.
 *
 * <p>Requires: {@value WarpZonePermissions#ADMIN_DELETE}
 */
public class WarpZoneDeleteCommand {

    private WarpZoneDeleteCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build(WarpZoneManager manager) {
        return Commands.literal("delete")
                .requires(src -> src.getSender().hasPermission(WarpZonePermissions.ADMIN_DELETE))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            manager.findAllNames().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            WarpZone zone = manager.getWarpZone(name);

                            if (zone == null) {
                                ctx.getSource().getSender().sendMessage(Component.text(
                                        "No warp zone named '" + name + "' exists.")
                                        .color(NamedTextColor.RED));
                                return 0;
                            }

                            manager.deleteWarpZone(zone);
                            ctx.getSource().getSender().sendMessage(Component.text(
                                    "Warp zone '" + name + "' deleted.")
                                    .color(NamedTextColor.GREEN));
                            return Command.SINGLE_SUCCESS;
                        }));
    }
}
