package io.github.hyscript7.projectfusion.warpzones.persistence;

import io.github.hyscript7.projectfusion.warpzones.WarpZone;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class InMemoryWarpZoneRepository implements WarpZoneRepository {
    protected final Map<UUID, WarpZone> map = new HashMap<>();

    @Override
    public void save(WarpZone warpZone) {
        map.put(warpZone.getWarpZoneUuid(), warpZone);
    }

    @Override
    public void delete(WarpZone warpZone) {
        map.remove(warpZone.getWarpZoneUuid());
    }

    @Override
    public @Nullable WarpZone find(UUID uuid) {
        return map.get(uuid);
    }

    @Override
    public List<WarpZone> findAll() {
        return new ArrayList<>(map.values());
    }
}
