package io.github.hyscript7.projectfusion.warpzones;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public record ZoneStack(int totalZones, WarpZone bottomZone, WarpZone topZone, WarpZone baseZone, List<WarpZone> stack) {
    public @Nullable WarpZone getPreviousZoneFor(WarpZone warpZone) {
        if (!stack.contains(warpZone)) {
            throw new IllegalArgumentException("The provided zone is not part of this Zone Stack!");
        }
        int idx = stack.indexOf(warpZone);
        if (idx - 1 < 0) {
            return null;
        }
        return stack.get(idx - 1);
    }

    public @Nullable WarpZone getNextZoneFor(WarpZone warpZone) {
        if (!stack.contains(warpZone)) {
            throw new IllegalArgumentException("The provided zone is not part of this Zone Stack!");
        }
        int idx = stack.indexOf(warpZone);
        if (idx + 1 == stack.size()) {
            return null;
        }
        return stack.get(idx + 1);
    }
}
