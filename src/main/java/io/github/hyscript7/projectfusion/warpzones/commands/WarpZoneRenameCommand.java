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
 * /warpzones rename &lt;zone&gt; &lt;displayname&gt;
 * /warpzones rename &lt;zone&gt; --clear
 *
 * <p>Sets or clears the display name of a warp zone. The display name is the
 * human-readable label shown in the action bar; it does not affect the zone's
 * ID name used in other commands.
 *
 * <p>Use {@code --clear} as the display name argument to remove a previously
 * set display name and revert the action bar label to the zone's ID name.
 *
 * <p>Requires: {@value WarpZonePermissions#ADMIN_RENAME}
 */
public class WarpZoneRenameCommand {

    /** Sentinel value the admin can pass to remove a display name. */
    private static final String CLEAR_SENTINEL = "--clear";

    private WarpZoneRenameCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build(WarpZoneManager manager) {
        return Commands.literal("rename")
                .requires(src -> src.getSender().hasPermission(WarpZonePermissions.ADMIN_RENAME))
                .then(Commands.argument("zone", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            manager.findAllNames().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        // greedy string so display names can contain spaces
                        .then(Commands.argument("displayname", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> {
                                    builder.suggest(CLEAR_SENTINEL, () -> "Remove the display name");
                                    // Also suggest the zone's current display name as a convenience
                                    // for edits — only attempt this if the zone argument is already typed.
                                    try {
                                        String zoneName = StringArgumentType.getString(ctx, "zone");
                                        WarpZone zone   = manager.getWarpZone(zoneName);
                                        if (zone != null) builder.suggest(zone.getDisplayName());
                                    } catch (IllegalArgumentException ignored) {}
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    String zoneName   = StringArgumentType.getString(ctx, "zone");
                                    String displayName = StringArgumentType.getString(ctx, "displayname");

                                    WarpZone zone = manager.getWarpZone(zoneName);
                                    if (zone == null) {
                                        ctx.getSource().getSender().sendMessage(Component.text(
                                                "No warp zone named '" + zoneName + "' exists.")
                                                .color(NamedTextColor.RED));
                                        return 0;
                                    }

                                    if (displayName.equalsIgnoreCase(CLEAR_SENTINEL)) {
                                        zone.setDisplayName(null);
                                        manager.updateWarpZone(zone);
                                        ctx.getSource().getSender().sendMessage(Component.text(
                                                "Display name for '" + zoneName + "' cleared; "
                                                + "the action bar will now show '" + zoneName + "'.")
                                                .color(NamedTextColor.GREEN));
                                    } else {
                                        zone.setDisplayName(displayName);
                                        manager.updateWarpZone(zone);
                                        ctx.getSource().getSender().sendMessage(Component.text(
                                                "Display name for '" + zoneName + "' set to '"
                                                + displayName + "'.")
                                                .color(NamedTextColor.GREEN));
                                    }

                                    return Command.SINGLE_SUCCESS;
                                })));
    }
}
