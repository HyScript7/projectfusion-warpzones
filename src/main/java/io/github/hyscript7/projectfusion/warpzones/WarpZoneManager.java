package io.github.hyscript7.projectfusion.warpzones;

import io.github.hyscript7.projectfusion.warpzones.persistence.WarpZoneRepository;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class WarpZoneManager {
    private final Map<Long, Set<WarpZone>> warpZonesByChunk = new HashMap<>();
    private final Map<WarpZone, ZoneStack> zoneStackByZone = new HashMap<>();
    private final WarpZoneRepository repo;

    public WarpZoneManager(WarpZoneRepository repo) {
        this.repo = repo;
        refreshCache();
    }

    public synchronized void refreshCache() {
        warpZonesByChunk.clear();
        zoneStackByZone.clear();
        List<WarpZone> all = repo.findAll();
        for (WarpZone wz : all) {
            addToChunkCache(wz);
        }
        buildZoneStackMap(all);
    }

    public ZoneStack getWarpZoneStack(WarpZone warpZone) {
        if (!zoneStackByZone.containsKey(warpZone)) {
            rebuildZoneStack(warpZone);
        }
        return zoneStackByZone.get(warpZone);
    }

    public @Nullable WarpZone getWarpZone(UUID uuid) {
        return repo.find(uuid);
    }

    public @Nullable WarpZone getWarpZone(Location location) {
        long chunkKey = location.getChunk().getChunkKey();
        Set<WarpZone> candidates = warpZonesByChunk.get(chunkKey);
        if (candidates == null) return null;
        return candidates.stream()
                .filter(wz -> wz.containsLocation(location))
                .findFirst()
                .orElse(null);
    }

    public synchronized void updateWarpZone(WarpZone warpZone) {
        repo.save(warpZone);
        rebuildZoneStack(warpZone);
    }

    public synchronized void registerWarpZone(WarpZone warpZone) {
        repo.save(warpZone);
        addToChunkCache(warpZone);
        rebuildZoneStack(warpZone);
    }

    public synchronized void deleteWarpZone(WarpZone warpZone) {
        if (warpZone.getPreviousWarpZoneUuid() != null) {
            WarpZone previous = repo.find(warpZone.getPreviousWarpZoneUuid());
            if (previous != null) {
                previous.setNextWarpZoneUuid(null);
                updateWarpZone(previous);
            }
        }
        if (warpZone.getNextWarpZoneUuid() != null) {
            WarpZone next = repo.find(warpZone.getNextWarpZoneUuid());
            if (next != null) {
                next.setPreviousWarpZoneUuid(null);
                updateWarpZone(next);
            }
        }
        removeFromChunkCache(warpZone);
        zoneStackByZone.remove(warpZone);
        repo.delete(warpZone);
    }

    // --- Cache helpers ---

    private void addToChunkCache(WarpZone warpZone) {
        for (long chunkKey : warpZone.getChunks()) {
            warpZonesByChunk.computeIfAbsent(chunkKey, k -> new HashSet<>()).add(warpZone);
        }
    }

    private void removeFromChunkCache(WarpZone warpZone) {
        for (long chunkKey : warpZone.getChunks()) {
            Set<WarpZone> zones = warpZonesByChunk.get(chunkKey);
            if (zones != null) {
                zones.remove(warpZone);
            }
        }
    }

    // --- Zone stack helpers ---

    private void buildZoneStackMap(List<WarpZone> all) {
        Set<WarpZone> visited = new HashSet<>();
        for (WarpZone wz : all) {
            if (visited.contains(wz)) continue;
            ZoneStack stack = rebuildZoneStack(wz);
            visited.addAll(stack.stack());
        }
    }

    private ZoneStack rebuildZoneStack(WarpZone warpZone) {
        WarpZone bottom = findBottomZone(warpZone);
        List<WarpZone> ordered = buildZoneList(bottom);
        ZoneStack zoneStack = new ZoneStack(
                ordered.size(),
                ordered.getFirst(),
                ordered.getLast(),
                ordered.getFirst(),
                ordered
        );
        for (WarpZone zone : ordered) {
            zoneStackByZone.put(zone, zoneStack);
        }
        return zoneStack;
    }

    /** Walks backwards through the linked list to find the first (bottom) zone. */
    private WarpZone findBottomZone(WarpZone warpZone) {
        while (warpZone.getPreviousWarpZoneUuid() != null) {
            WarpZone previous = repo.find(warpZone.getPreviousWarpZoneUuid());
            if (previous == null) break;
            warpZone = previous;
        }
        return warpZone;
    }

    /** Walks forward through the linked list, collecting zones in order. */
    private List<WarpZone> buildZoneList(WarpZone bottom) {
        List<WarpZone> list = new ArrayList<>();
        WarpZone current = bottom;
        while (current != null) {
            list.add(current);
            UUID nextUuid = current.getNextWarpZoneUuid();
            current = (nextUuid != null) ? repo.find(nextUuid) : null;
        }
        return list;
    }
}