package io.github.hyscript7.projectfusion.warpzones;

import io.github.hyscript7.projectfusion.warpzones.commands.*;
import io.github.hyscript7.projectfusion.warpzones.listener.PlayerMoveListener;
import io.github.hyscript7.projectfusion.warpzones.persistence.SqliteWarpZoneRepository;
import io.github.hyscript7.projectfusion.warpzones.persistence.YamlWarpZoneRepository;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

import static org.bukkit.Bukkit.getPluginManager;

public final class ProjectFusionWarpzones extends JavaPlugin {

    private SqliteWarpZoneRepository warpZoneRepo;
    private PlayerMoveListener     playerMoveListener;

    @Override
    public void onEnable() {
        ConfigurationSerialization.registerClass(WarpZone.class);

        try {
            warpZoneRepo = new SqliteWarpZoneRepository(this);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        warpZoneRepo.start(this);

        WarpZoneManager warpZoneManager = new WarpZoneManager(warpZoneRepo);

        playerMoveListener = new PlayerMoveListener(warpZoneManager);
        playerMoveListener.start(this);
        getPluginManager().registerEvents(playerMoveListener, this);

        registerCommands(warpZoneManager);
    }

    @Override
    public void onDisable() {
        playerMoveListener.stop();
        warpZoneRepo.stop();
    }

    // -------------------------------------------------------------------------
    // Command registration
    // -------------------------------------------------------------------------

    private void registerCommands(WarpZoneManager manager) {
        var root = Commands.literal("warpzones")
                .then(WarpZoneListCommand.build(manager))
                .then(WarpZoneCreateCommand.build(manager))
                .then(WarpZoneDeleteCommand.build(manager))
                .then(WarpZoneLinkCommand.build(manager));

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(root.build()));
    }
}
