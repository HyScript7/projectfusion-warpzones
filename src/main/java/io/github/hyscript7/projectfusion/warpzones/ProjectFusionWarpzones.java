package io.github.hyscript7.projectfusion.warpzones;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.regions.Region;
import io.github.hyscript7.projectfusion.warpzones.listener.PlayerMoveListener;
import io.github.hyscript7.projectfusion.warpzones.persistence.YamlWarpZoneRepository;
import io.github.hyscript7.projectfusion.warpzones.util.LocationUtil;
import io.github.hyscript7.projectfusion.warpzones.util.YawUtils;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import com.sk89q.worldedit.WorldEdit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.bukkit.Bukkit.getPluginManager;

public final class ProjectFusionWarpzones extends JavaPlugin {
    private YamlWarpZoneRepository warpZoneRepo;
    private PlayerMoveListener playerMoveListener;

    private final Map<UUID, UUID> playerWarpZoneSelections = new HashMap<>();

    private static Region getPlayerSelection(Actor actor) throws CommandException {
        LocalSession localSession = WorldEdit.getInstance().getSessionManager().get(actor);
        try {
            if (localSession == null || localSession.getSelectionWorld() == null) {
                throw new IncompleteRegionException();
            }
            return localSession.getRegionSelector(localSession.getSelectionWorld()).getRegion();
        } catch (IncompleteRegionException e) {
            throw new CommandException("Please select an area first. " +
                    "Use WorldEdit to make a selection! " +
                    "(see: https://worldedit.enginehub.org/en/latest/usage/regions/selections/).");
        }
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        ConfigurationSerialization.registerClass(WarpZone.class);

        warpZoneRepo = new YamlWarpZoneRepository(this);
        warpZoneRepo.start(this);
        WarpZoneManager warpZoneManager = new WarpZoneManager(warpZoneRepo);

        playerMoveListener = new PlayerMoveListener(warpZoneManager);
        playerMoveListener.start(this);

        getPluginManager().registerEvents(playerMoveListener, this);

        var warpZoneCommands = Commands.literal("warpzones");
        var warpZoneListCommand = Commands.literal("list").executes(ctx -> {
            CommandSender sender = ctx.getSource().getSender();
            Component accumulator = Component.empty();
            var formatted = warpZoneRepo.findAll().stream().map(warpZone -> {
                        return Component.newline().append(Component.text("(" + LocationUtil.prettyString(warpZone.getCorner1()) + ") <-> " + "(" + LocationUtil.prettyString(warpZone.getCorner2()) + ") in " + warpZone.getWorld().getName()));
                    }).toList();
            for (Component component : formatted) {
                accumulator = accumulator.append(component);
            }
            sender.sendMessage(accumulator);
            return Command.SINGLE_SUCCESS;
        });
        var warpZoneSelectCommand =  Commands.literal("sel").executes(ctx -> {
            CommandSender sender = ctx.getSource().getSender();
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("You must be a player to execute this command!").color(NamedTextColor.RED));
                return 0;
            }
            WarpZone standingIn = warpZoneManager.getWarpZone(player.getLocation());
            if (standingIn != null) {
                playerWarpZoneSelections.put(player.getUniqueId(), standingIn.getWarpZoneUuid());
                player.sendMessage(Component.text("WarpZone selected!").color(NamedTextColor.GREEN));
                return Command.SINGLE_SUCCESS;
            } else {
                player.sendMessage(Component.text("You must be standing in a Warp Zone!").color(NamedTextColor.RED));
                return 0;
            }
        });
        var warpZoneLinkCommand =  Commands.literal("link").then(Commands.argument("positionAbove", BoolArgumentType.bool()).executes(ctx -> {
            CommandSender sender = ctx.getSource().getSender();
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("You must be a player to execute this command!").color(NamedTextColor.RED));
                return 0;
            }
            boolean positionAbove = ctx.getArgument("positionAbove", boolean.class);
            UUID selected = playerWarpZoneSelections.get(player.getUniqueId());
            if (selected == null) {
                player.sendMessage(Component.text("You must have a warp zone selected!").color(NamedTextColor.RED));
                return 0;
            }
            WarpZone selectedWarpZone = warpZoneManager.getWarpZone(selected);
            if (selectedWarpZone == null) {
                player.sendMessage(Component.text("You must have a warp zone selected!").color(NamedTextColor.RED));
                return 0;
            }
            WarpZone standingIn = warpZoneManager.getWarpZone(player.getLocation());
            if (standingIn != null) {
                if (positionAbove) {
                    standingIn.setPreviousWarpZoneUuid(selectedWarpZone.getWarpZoneUuid());
                    selectedWarpZone.setNextWarpZoneUuid(standingIn.getWarpZoneUuid());
                    warpZoneManager.updateWarpZone(standingIn);
                    warpZoneManager.updateWarpZone(selectedWarpZone);
                }
                player.sendMessage(Component.text("WarpZones linked!").color(NamedTextColor.GREEN));
                return Command.SINGLE_SUCCESS;
            } else {
                player.sendMessage(Component.text("You must be standing in a Warp Zone!").color(NamedTextColor.RED));
                return 0;
            }
        }));
        var warpZoneCreateCommand = Commands.literal("create").executes(ctx -> {
            CommandSender sender = ctx.getSource().getSender();
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("You must be a player to execute this command!").color(NamedTextColor.RED));
                return 0;
            }
            Region selection = getPlayerSelection(BukkitAdapter.adapt(player));
            var min = selection.getBoundingBox().getMinimumPoint();
            var max = selection.getBoundingBox().getMaximumPoint();
            var weWorld = selection.getWorld();
            assert weWorld != null;
            var world = BukkitAdapter.adapt(weWorld);
            var loc1 = new Location(world, min.x(), min.y(), min.z());
            var loc2 = new Location(world, max.x(), max.y(), max.z());
            var warpZone = new WarpZone(world, loc1, loc2, YawUtils.snapToCardinal(player.getYaw()));
            warpZoneManager.registerWarpZone(warpZone);
            player.sendMessage(Component.text("WarpZone created and selected!").color(NamedTextColor.GREEN));
            playerWarpZoneSelections.put(player.getUniqueId(), warpZone.getWarpZoneUuid());
            return Command.SINGLE_SUCCESS;
        });

        warpZoneCommands = warpZoneCommands.then(warpZoneListCommand).then(warpZoneSelectCommand).then(warpZoneCreateCommand).then(warpZoneLinkCommand);

        final var finalWarpZoneCommands = warpZoneCommands;
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(finalWarpZoneCommands.build());
        });
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        playerMoveListener.stop();
        warpZoneRepo.stop();
    }
}
