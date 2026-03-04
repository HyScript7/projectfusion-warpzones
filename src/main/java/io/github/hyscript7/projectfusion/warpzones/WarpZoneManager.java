package io.github.hyscript7.projectfusion.warpzones;

import io.github.hyscript7.projectfusion.warpzones.persistence.WarpZoneRepository;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Coordinates all runtime interactions with warp zones.
 *
 * <p>An in-memory chunk-based spatial index is kept for fast {@link #getWarpZone(Location)}
 * queries (the hottest path — called on every player-move event). All other lookups
 * delegate directly to the repository.
 */
public class WarpZoneManager {

    private final Map<Long, Set<WarpZone>> zonesByChunk     = new HashMap<>();
    private final Map<WarpZone, ZoneStack> stackByZone      = new HashMap<>();
    private final WarpZoneRepository       repo;

    public WarpZoneManager(WarpZoneRepository repo) {
        this.repo = repo;
        refreshCache();
    }

    // -------------------------------------------------------------------------
    // Cache management
    // -------------------------------------------------------------------------

    /** Rebuilds the entire spatial cache from the repository. */
    public synchronized void refreshCache() {
        zonesByChunk.clear();
        stackByZone.clear();

        List<WarpZone> all = repo.findAll();
        all.forEach(this::addToChunkCache);
        buildAllZoneStacks(all);
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /** Returns the zone that contains {@code location}, or {@code null}. */
    public @Nullable WarpZone getWarpZone(Location location) {
        long chunkKey = location.getChunk().getChunkKey();
        Set<WarpZone> candidates = zonesByChunk.get(chunkKey);
        if (candidates == null) return null;
        return candidates.stream()
                .filter(wz -> wz.containsLocation(location))
                .findFirst()
                .orElse(null);
    }

    /** Returns the zone with the given UUID, or {@code null}. */
    public @Nullable WarpZone getWarpZone(UUID uuid) {
        return repo.find(uuid);
    }

    /** Returns the zone with the given name, or {@code null}. */
    public @Nullable WarpZone getWarpZone(String name) {
        return repo.findByName(name);
    }

    /** Returns all zone names — useful for tab-completion suggestions. */
    public List<String> findAllNames() {
        return repo.findAllNames();
    }

    /** Returns all zones. */
    public List<WarpZone> findAll() {
        return repo.findAll();
    }

    public boolean nameExists(String name) {
        return repo.nameExists(name);
    }

    /** Returns the {@link ZoneStack} for the given zone, building it if necessary. */
    public ZoneStack getWarpZoneStack(WarpZone warpZone) {
        return stackByZone.computeIfAbsent(warpZone, z -> rebuildZoneStack(z));
    }

    // -------------------------------------------------------------------------
    // Mutations
    // -------------------------------------------------------------------------

    public synchronized void registerWarpZone(WarpZone warpZone) {
        repo.save(warpZone);
        addToChunkCache(warpZone);
        rebuildZoneStack(warpZone);
    }

    public synchronized void updateWarpZone(WarpZone warpZone) {
        repo.save(warpZone);
        rebuildZoneStack(warpZone);
    }

    /**
     * Deletes a zone and repairs any links to adjacent zones so the remaining
     * zones do not reference a stale UUID.
     */
    public synchronized void deleteWarpZone(WarpZone warpZone) {
        if (warpZone.getPreviousWarpZoneUuid() != null) {
            WarpZone below = repo.find(warpZone.getPreviousWarpZoneUuid());
            if (below != null) {
                below.setNextWarpZoneUuid(null);
                updateWarpZone(below);
            }
        }
        if (warpZone.getNextWarpZoneUuid() != null) {
            WarpZone above = repo.find(warpZone.getNextWarpZoneUuid());
            if (above != null) {
                above.setPreviousWarpZoneUuid(null);
                updateWarpZone(above);
            }
        }
        removeFromChunkCache(warpZone);
        stackByZone.remove(warpZone);
        repo.delete(warpZone);
    }

    // -------------------------------------------------------------------------
    // Chunk-cache helpers
    // -------------------------------------------------------------------------

    private void addToChunkCache(WarpZone zone) {
        for (long key : zone.getChunks()) {
            zonesByChunk.computeIfAbsent(key, k -> new HashSet<>()).add(zone);
        }
    }

    private void removeFromChunkCache(WarpZone zone) {
        for (long key : zone.getChunks()) {
            Set<WarpZone> bucket = zonesByChunk.get(key);
            if (bucket != null) bucket.remove(zone);
        }
    }

    // -------------------------------------------------------------------------
    // Zone-stack helpers
    // -------------------------------------------------------------------------

    private void buildAllZoneStacks(List<WarpZone> all) {
        Set<WarpZone> visited = new HashSet<>();
        for (WarpZone zone : all) {
            if (!visited.contains(zone)) {
                ZoneStack stack = rebuildZoneStack(zone);
                visited.addAll(stack.stack());
            }
        }
    }

    private ZoneStack rebuildZoneStack(WarpZone zone) {
        WarpZone       start   = findChainStart(zone);
        List<WarpZone> ordered = collectChain(start);

        // A cyclic stack is one where the last zone's "next" points back to the first.
        UUID lastNext = ordered.getLast().getNextWarpZoneUuid();
        boolean cyclic = lastNext != null
                && lastNext.equals(ordered.getFirst().getWarpZoneUuid());

        ZoneStack stack = new ZoneStack(
                ordered.size(),
                ordered.getFirst(),
                ordered.getLast(),
                ordered.getFirst(),
                ordered,
                cyclic
        );
        ordered.forEach(z -> stackByZone.put(z, stack));
        return stack;
    }

    /**
     * Walks backwards to find the natural start of a linear chain.
     *
     * <p>For a <em>cyclic</em> chain there is no true start, so as soon as a repeated
     * UUID is encountered (meaning we have gone all the way around) we stop and return
     * the original {@code zone}. This ensures cyclic stacks always have a stable,
     * deterministic entry point — the zone the rebuild was triggered from.
     */
    private WarpZone findChainStart(WarpZone zone) {
        Set<UUID> visited = new HashSet<>();
        visited.add(zone.getWarpZoneUuid());

        WarpZone current = zone;
        while (current.getPreviousWarpZoneUuid() != null) {
            if (visited.contains(current.getPreviousWarpZoneUuid())) {
                // We would revisit a node — this is a cycle.  The original zone is as
                // good a starting point as any, so return it directly.
                return zone;
            }
            WarpZone below = repo.find(current.getPreviousWarpZoneUuid());
            if (below == null) break; // Stale link — treat this as the start.
            visited.add(below.getWarpZoneUuid());
            current = below;
        }
        return current; // Reached the true bottom of a linear chain.
    }

    /**
     * Walks forward from {@code start}, collecting every zone exactly once.
     *
     * <p>Stops when the next pointer is {@code null} (linear end), points to an
     * already-visited UUID (cycle closing), or cannot be resolved (stale link).
     */
    private List<WarpZone> collectChain(WarpZone start) {
        List<WarpZone> list    = new ArrayList<>();
        Set<UUID>      visited = new HashSet<>();
        WarpZone       current = start;

        while (current != null && !visited.contains(current.getWarpZoneUuid())) {
            visited.add(current.getWarpZoneUuid());
            list.add(current);
            UUID nextUuid = current.getNextWarpZoneUuid();
            current = (nextUuid != null) ? repo.find(nextUuid) : null;
        }
        return list;
    }
}
