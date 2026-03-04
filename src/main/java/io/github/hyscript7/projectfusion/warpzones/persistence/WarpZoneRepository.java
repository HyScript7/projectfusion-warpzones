package io.github.hyscript7.projectfusion.warpzones.persistence;

import io.github.hyscript7.projectfusion.warpzones.WarpZone;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface WarpZoneRepository {
    void save(WarpZone warpZone);
    void delete(WarpZone warpZone);
    @Nullable WarpZone find(UUID uuid);
    List<WarpZone> findAll();
}
