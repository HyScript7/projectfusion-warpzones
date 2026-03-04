package io.github.hyscript7.projectfusion.warpzones.persistence;

import io.github.hyscript7.projectfusion.warpzones.WarpZone;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class InMemoryWarpZoneRepository implements WarpZoneRepository {

    protected final Map<UUID, WarpZone>   store     = new HashMap<>();
    /** Secondary index: name → UUID for O(1) by-name lookups. */
    protected final Map<String, UUID>     nameIndex = new HashMap<>();

    @Override
    public void save(WarpZone warpZone) {
        // If we are replacing an existing zone, remove its old name from the index first.
        WarpZone existing = store.get(warpZone.getWarpZoneUuid());
        if (existing != null && !existing.getName().equals(warpZone.getName())) {
            nameIndex.remove(existing.getName());
        }
        store.put(warpZone.getWarpZoneUuid(), warpZone);
        nameIndex.put(warpZone.getName(), warpZone.getWarpZoneUuid());
    }

    @Override
    public void delete(WarpZone warpZone) {
        nameIndex.remove(warpZone.getName());
        store.remove(warpZone.getWarpZoneUuid());
    }

    @Override
    public @Nullable WarpZone find(UUID uuid) {
        return store.get(uuid);
    }

    @Override
    public @Nullable WarpZone findByName(String name) {
        UUID uuid = nameIndex.get(name);
        return uuid != null ? store.get(uuid) : null;
    }

    @Override
    public List<WarpZone> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<String> findAllNames() {
        return new ArrayList<>(nameIndex.keySet());
    }

    @Override
    public boolean nameExists(String name) {
        return nameIndex.containsKey(name);
    }
}
