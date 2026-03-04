package io.github.hyscript7.projectfusion.warpzones.persistence;

import io.github.hyscript7.projectfusion.warpzones.ProjectFusionWarpzones;
import io.github.hyscript7.projectfusion.warpzones.WarpZone;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * YAML-backed repository.
 *
 * <p>Extends {@link InMemoryWarpZoneRepository} so all reads remain O(1) in-memory.
 * A periodic auto-save task flushes the full in-memory state to disk every 60 s.
 *
 * <p><b>Deletion handling:</b> the YAML section is cleared and fully rewritten on each
 * save so that deleted zones never linger in the file.
 *
 * <p><b>Considering a more efficient persistence layer?</b><br>
 * {@link SqliteWarpZoneRepository} is a drop-in replacement that performs an immediate,
 * single-row {@code UPSERT}/{@code DELETE} on every change instead of serialising the
 * entire dataset to a text file. For large numbers of zones this can be orders of
 * magnitude faster and avoids the periodic-save latency spikes entirely.
 */
public class YamlWarpZoneRepository extends InMemoryWarpZoneRepository {

    private final File              dataFile;
    private final YamlConfiguration config;
    private       BukkitTask        autoSaver = null;

    public YamlWarpZoneRepository(Plugin plugin) {
        plugin.getDataFolder().mkdirs();
        this.dataFile = new File(plugin.getDataFolder(), "warpzones.yml");
        this.config   = YamlConfiguration.loadConfiguration(dataFile);
        loadAllFromFile();
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public void start(Plugin plugin) {
        if (autoSaver != null) throw new IllegalStateException("Auto-saver already started");

        // Flush to disk once a minute without blocking the main thread.
        autoSaver = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                flushToDisk();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to auto-save warp zones: " + e.getMessage());
            }
        }, 60 * 20L, 60 * 20L);
    }

    public void stop() {
        if (autoSaver != null) {
            autoSaver.cancel();
            autoSaver = null;
        }
        try {
            flushToDisk();
        } catch (IOException e) {
            ProjectFusionWarpzones.getPlugin(ProjectFusionWarpzones.class)
                    .getLogger().severe("Failed to save warp zones on shutdown: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void loadAllFromFile() {
        if (!dataFile.exists()) return;

        ConfigurationSection section = config.getConfigurationSection("warpzones");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            Object obj = section.get(key);
            if (obj instanceof WarpZone wz) {
                super.save(wz); // populate in-memory store only
            }
        }
    }

    /**
     * Rewrites the entire {@code warpzones} section from the current in-memory state.
     * Clearing the section first ensures that zones deleted since the last save are
     * not preserved in the file.
     */
    private synchronized void flushToDisk() throws IOException {
        config.set("warpzones", null); // wipe stale entries

        for (Map.Entry<UUID, WarpZone> entry : store.entrySet()) {
            config.set("warpzones." + entry.getKey(), entry.getValue());
        }

        config.save(dataFile);
    }
}
