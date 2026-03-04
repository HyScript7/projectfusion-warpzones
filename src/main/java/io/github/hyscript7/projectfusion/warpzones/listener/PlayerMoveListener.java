package io.github.hyscript7.projectfusion.warpzones.listener;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import io.github.hyscript7.projectfusion.warpzones.WarpZone;
import io.github.hyscript7.projectfusion.warpzones.WarpZoneManager;
import io.github.hyscript7.projectfusion.warpzones.ZoneStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.KeybindComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerMoveListener implements Listener {
    private static final long DEFAULT_ZONE_CHECK_INTERVAL = 125L; // Every half a second

    private final WarpZoneManager warpZoneManager;
    private final Map<UUID, Long> debounce = new HashMap<>();
    private final long zoneCheckInterval;
    private final Map<UUID, UUID> playerLastSeenZone = new HashMap<>();

    public PlayerMoveListener(WarpZoneManager warpZoneManager) {
        this(warpZoneManager, DEFAULT_ZONE_CHECK_INTERVAL);
    }

    public PlayerMoveListener(WarpZoneManager warpZoneManager, long zoneCheckInterval) {
        this.warpZoneManager = warpZoneManager;
        this.zoneCheckInterval = zoneCheckInterval;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (now - debounce.getOrDefault(playerUuid, 0L) < zoneCheckInterval) return;
        debounce.put(playerUuid, System.currentTimeMillis());

        // If the manager returns a null, the player is not standing in any WarpZone
        WarpZone warpZone = warpZoneManager.getWarpZone(player.getLocation());
        if (warpZone == null) {
            playerLastSeenZone.remove(playerUuid);
            return;
        }

        if (warpZone.getWarpZoneUuid().equals(playerLastSeenZone.getOrDefault(playerUuid, null))) return;
        playerLastSeenZone.put(playerUuid, warpZone.getWarpZoneUuid());
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
    }

    private void changeZones(Player player, boolean directionIsUp) {
        UUID playerZoneUuid = playerLastSeenZone.getOrDefault(player.getUniqueId(), null);
        if (playerZoneUuid == null) return;
        WarpZone currentZone = warpZoneManager.getWarpZone(playerZoneUuid);
        if (currentZone == null) return;
        UUID nextZoneUuid = directionIsUp ? currentZone.getNextWarpZoneUuid() : currentZone.getPreviousWarpZoneUuid();
        if (nextZoneUuid == null) return;
        WarpZone destinationZone = warpZoneManager.getWarpZone(nextZoneUuid);
        if (destinationZone == null) {
            if (directionIsUp) {
                currentZone.setNextWarpZoneUuid(null);
            } else {
                currentZone.setPreviousWarpZoneUuid(null);
            }
            warpZoneManager.updateWarpZone(currentZone);
            return;
        }
        Location newLocation = destinationZone.transitionFrom(currentZone, player.getLocation());
        if (!newLocation.getChunk().isLoaded()) {
            // Load destination to prevent flashing of the WarpZone interior if it is far away or in another world
            for (long chunkKey : destinationZone.getChunks()) {
                Chunk chunk = newLocation.getWorld().getChunkAt(chunkKey);
                if (!chunk.isLoaded()) {
                    chunk.load();
                }
            }
        }
        playerLastSeenZone.put(player.getUniqueId(), nextZoneUuid);
        player.teleport(newLocation);
    }

    @EventHandler
    public void onPlayerJump(PlayerJumpEvent event) {
        Player player = event.getPlayer();
        changeZones(player, true);
    }

    @EventHandler
    public void onPlayerCrouch(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) {
            return;
        }
        changeZones(event.getPlayer(), false);
    }

    private BukkitTask actionBarTask = null;

    public void start(Plugin plugin) {
        if (actionBarTask != null) {
            stop();
        }
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::showActionBarTask, 20L, 10L);
    }

    public void stop() {
        if (actionBarTask != null) {
            actionBarTask.cancel();
            actionBarTask = null;
        }
    }

    private void showActionBarTask() {
        for (Map.Entry<UUID, UUID> entry : playerLastSeenZone.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) {
                playerLastSeenZone.remove(entry.getKey());
                continue;
            }
            WarpZone warpZone = warpZoneManager.getWarpZone(entry.getValue());
            if (warpZone == null) {
                playerLastSeenZone.remove(entry.getValue());
                continue;
            }
            ZoneStack stack = warpZoneManager.getWarpZoneStack(warpZone);
            Component actionBarComponent = Component.empty();
            int idx = stack.stack().indexOf(warpZone);
            boolean hasFloorsAbove = !(idx+1 >= stack.stack().size());
            boolean hasFloorsBelow = idx != 0;
            for (WarpZone floor : stack.stack()) {
                String position = "[" + (stack.stack().indexOf(floor) + 1) + "]";
                if (floor.equals(warpZone)) {
                    actionBarComponent = actionBarComponent
                            .append(
                                    Component.text(" <").append(Component.keybind("key.sneak")).append(Component.text("> ")).color(hasFloorsBelow ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY)
                            ).append(
                                    Component.text(position).color(NamedTextColor.GREEN)
                            ).append(
                                    Component.text(" <").append(Component.keybind("key.jump")).append(Component.text("> ")).color(hasFloorsAbove ? NamedTextColor.GOLD : NamedTextColor.DARK_GRAY)
                            );
                } else {
                    actionBarComponent = actionBarComponent.append(
                            Component.text(position).color(NamedTextColor.GRAY)
                    );
                }
            }
            player.sendActionBar(actionBarComponent);
        }
    }
}
