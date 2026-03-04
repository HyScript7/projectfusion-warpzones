package io.github.hyscript7.projectfusion.warpzones.listener;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import io.github.hyscript7.projectfusion.warpzones.WarpZone;
import io.github.hyscript7.projectfusion.warpzones.WarpZoneManager;
import io.github.hyscript7.projectfusion.warpzones.WarpZonePermissions;
import io.github.hyscript7.projectfusion.warpzones.ZoneStack;
import net.kyori.adventure.text.Component;
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

    /** Minimum milliseconds between zone-entry checks per player. */
    private static final long DEFAULT_ZONE_CHECK_INTERVAL_MS = 125L;

    private final WarpZoneManager warpZoneManager;
    private final Map<UUID, Long>  debounce           = new HashMap<>();
    private final Map<UUID, UUID>  playerLastSeenZone = new HashMap<>();
    private final long             zoneCheckInterval;
    private       BukkitTask       actionBarTask      = null;

    public PlayerMoveListener(WarpZoneManager warpZoneManager) {
        this(warpZoneManager, DEFAULT_ZONE_CHECK_INTERVAL_MS);
    }

    public PlayerMoveListener(WarpZoneManager warpZoneManager, long zoneCheckIntervalMs) {
        this.warpZoneManager  = warpZoneManager;
        this.zoneCheckInterval = zoneCheckIntervalMs;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public void start(Plugin plugin) {
        if (actionBarTask != null) stop();
        actionBarTask = Bukkit.getScheduler()
                .runTaskTimerAsynchronously(plugin, this::tickActionBars, 20L, 10L);
    }

    public void stop() {
        if (actionBarTask != null) {
            actionBarTask.cancel();
            actionBarTask = null;
        }
    }

    // -------------------------------------------------------------------------
    // Event handlers
    // -------------------------------------------------------------------------

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player    = event.getPlayer();
        UUID   playerId  = player.getUniqueId();
        long   now       = System.currentTimeMillis();

        // Debounce: skip if checked too recently.
        if (now - debounce.getOrDefault(playerId, 0L) < zoneCheckInterval) return;
        debounce.put(playerId, now);

        WarpZone zone = warpZoneManager.getWarpZone(player.getLocation());
        if (zone == null) {
            playerLastSeenZone.remove(playerId);
            return;
        }

        // Silently ignore zones in worlds the player is not permitted to use.
        if (!player.hasPermission(WarpZonePermissions.worldUse(zone.getWorld()))) {
            playerLastSeenZone.remove(playerId);
            return;
        }

        // Only react when the player enters a new zone.
        if (zone.getWarpZoneUuid().equals(playerLastSeenZone.getOrDefault(playerId, null))) return;
        playerLastSeenZone.put(playerId, zone.getWarpZoneUuid());
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
    }

    @EventHandler
    public void onPlayerJump(PlayerJumpEvent event) {
        changeZone(event.getPlayer(), /* directionUp */ true);
    }

    @EventHandler
    public void onPlayerCrouch(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        changeZone(event.getPlayer(), /* directionUp */ false);
    }

    // -------------------------------------------------------------------------
    // Zone transition
    // -------------------------------------------------------------------------

    private void changeZone(Player player, boolean directionIsUp) {
        UUID playerZoneUuid = playerLastSeenZone.get(player.getUniqueId());
        if (playerZoneUuid == null) return;

        WarpZone currentZone = warpZoneManager.getWarpZone(playerZoneUuid);
        if (currentZone == null) return;

        UUID destinationUuid = directionIsUp
                ? currentZone.getNextWarpZoneUuid()
                : currentZone.getPreviousWarpZoneUuid();
        if (destinationUuid == null) return;

        WarpZone destinationZone = warpZoneManager.getWarpZone(destinationUuid);
        if (destinationZone == null) {
            // Stale link — clean it up.
            if (directionIsUp) currentZone.setNextWarpZoneUuid(null);
            else               currentZone.setPreviousWarpZoneUuid(null);
            warpZoneManager.updateWarpZone(currentZone);
            return;
        }

        // When the destination is in a different world, verify the player has
        // permission to use zones there before teleporting them across.
        if (!destinationZone.getWorld().equals(currentZone.getWorld())
                && !player.hasPermission(WarpZonePermissions.worldUse(destinationZone.getWorld()))) {
            return;
        }

        Location newLocation = destinationZone.transitionFrom(currentZone, player.getLocation());

        // Pre-load destination chunks to prevent visual flicker.
        if (!newLocation.getChunk().isLoaded()) {
            for (long chunkKey : destinationZone.getChunks()) {
                Chunk chunk = newLocation.getWorld().getChunkAt(chunkKey);
                if (!chunk.isLoaded()) chunk.load();
            }
        }

        playerLastSeenZone.put(player.getUniqueId(), destinationUuid);
        player.teleport(newLocation);
    }

    // -------------------------------------------------------------------------
    // Action bar (zone HUD)
    // -------------------------------------------------------------------------

    /**
     * Renders a compact zone HUD in every in-zone player's action bar.
     *
     * <p>Example (player on floor 2 of 3, zone named "lobby_mid"):
     * <pre>
     *   [1]  ‹ sneak ›  « lobby_mid »  ‹ jump ›  [3]
     * </pre>
     */
    private void tickActionBars() {
        for (Map.Entry<UUID, UUID> entry : playerLastSeenZone.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) {
                playerLastSeenZone.remove(entry.getKey());
                continue;
            }

            WarpZone currentZone = warpZoneManager.getWarpZone(entry.getValue());
            if (currentZone == null) {
                playerLastSeenZone.remove(entry.getKey());
                continue;
            }

            ZoneStack stack = warpZoneManager.getWarpZoneStack(currentZone);
            player.sendActionBar(buildActionBar(currentZone, stack));
        }
    }

    private static Component buildActionBar(WarpZone currentZone, ZoneStack stack) {
        int currentIdx = stack.stack().indexOf(currentZone);

        if (stack.cyclic()) {
            return buildCyclicActionBar(currentZone, currentIdx, stack);
        }

        boolean hasBelow = currentIdx > 0;
        boolean hasAbove = currentIdx < stack.stack().size() - 1;

        Component bar = Component.empty();

        for (int i = 0; i < currentIdx; i++) {
            bar = bar.append(Component.text("[" + (i + 1) + "] ").color(NamedTextColor.DARK_GRAY));
        }

        bar = bar.append(
                Component.text(" ‹ ").append(Component.keybind("key.sneak")).append(Component.text(" › "))
                        .color(hasBelow ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY)
        );
        bar = bar.append(
                Component.text("« " + currentZone.getDisplayName()
                        + " [" + (currentIdx + 1) + "/" + stack.totalZones() + "] »")
                        .color(NamedTextColor.GREEN)
        );
        bar = bar.append(
                Component.text(" ‹ ").append(Component.keybind("key.jump")).append(Component.text(" › "))
                        .color(hasAbove ? NamedTextColor.GOLD : NamedTextColor.DARK_GRAY)
        );

        for (int i = currentIdx + 1; i < stack.totalZones(); i++) {
            bar = bar.append(Component.text(" [" + (i + 1) + "]").color(NamedTextColor.DARK_GRAY));
        }

        return bar;
    }

    /**
     * Action-bar variant for cyclic stacks.
     *
     * <p>Both directions are always available in a loop, so both keybind indicators are
     * always lit. A ↻ symbol is prepended to make the cyclic nature obvious at a glance.
     *
     * <p>Example (zone 2 of 4 in a loop):
     * <pre>↻  ‹sneak›  « lobby_b [2/4] »  ‹jump›</pre>
     */
    private static Component buildCyclicActionBar(WarpZone currentZone, int currentIdx, ZoneStack stack) {
        return Component.text("↻ ").color(NamedTextColor.LIGHT_PURPLE)
                .append(
                        Component.text(" ‹ ").append(Component.keybind("key.sneak")).append(Component.text(" › "))
                                .color(NamedTextColor.YELLOW)
                )
                .append(
                        Component.text("« " + currentZone.getDisplayName()
                                + " [" + (currentIdx + 1) + "/" + stack.totalZones() + "] »")
                                .color(NamedTextColor.GREEN)
                )
                .append(
                        Component.text(" ‹ ").append(Component.keybind("key.jump")).append(Component.text(" › "))
                                .color(NamedTextColor.GOLD)
                );
    }
}
