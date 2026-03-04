package io.github.hyscript7.projectfusion.warpzones.persistence;

import io.github.hyscript7.projectfusion.warpzones.WarpZone;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface WarpZoneRepository {
    void save(WarpZone warpZone);
    void delete(WarpZone warpZone);

    @Nullable WarpZone find(UUID uuid);
    @Nullable WarpZone findByName(String name);

    List<WarpZone> findAll();
    List<String> findAllNames();

    /** Returns true if any zone with the given name already exists. */
    boolean nameExists(String name);
}
