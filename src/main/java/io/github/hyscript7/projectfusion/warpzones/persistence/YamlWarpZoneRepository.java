package io.github.hyscript7.projectfusion.warpzones.persistence;

import io.github.hyscript7.projectfusion.warpzones.ProjectFusionWarpzones;
import io.github.hyscript7.projectfusion.warpzones.WarpZone;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class YamlWarpZoneRepository extends InMemoryWarpZoneRepository {
    private final File dataFile;
    private final YamlConfiguration config;
    private BukkitTask autoSaver = null;

    public YamlWarpZoneRepository(Plugin plugin) {
        plugin.getDataFolder().mkdirs();
        this.dataFile = new File(plugin.getDataFolder(), "warpzones.yml");
        this.config = YamlConfiguration.loadConfiguration(dataFile);
        loadAllFromFile();
    }

    private void loadAllFromFile() {
        if (!dataFile.exists()) return;

        ConfigurationSection section = config.getConfigurationSection("warpzones");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            Object obj = section.get(key);
            if (obj instanceof WarpZone) {
                super.save((WarpZone) obj);
            }
        }
    }

    public void start(Plugin plugin) {
        if (autoSaver != null) {
            throw new IllegalStateException("Auto saver already started");
        }
        autoSaver = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                save();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save data: " + e.getMessage());
            }
        }, 60 * 20L, 60 * 20L);
    }

    public void stop() {
        autoSaver.cancel();
        try {
            save();
        } catch (IOException e) {
            ProjectFusionWarpzones.getPlugin(ProjectFusionWarpzones.class).getLogger().severe("Failed to save data: " + e.getMessage());
        }
    }

    private void save() throws IOException {
        for (Map.Entry<UUID, WarpZone> entry : map.entrySet()) {
            config.set("warpzones." + entry.getKey().toString(), entry.getValue());
        }
        config.save(dataFile);
    }
}
