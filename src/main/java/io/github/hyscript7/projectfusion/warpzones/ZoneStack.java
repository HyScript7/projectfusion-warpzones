package io.github.hyscript7.projectfusion.warpzones;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * An ordered sequence of linked warp zones.
 *
 * <p>A stack can be either <em>linear</em> (has a definite bottom and top, like floors
 * in a building) or <em>cyclic</em> (the last zone links back to the first, forming a
 * loop with no endpoints).
 *
 * <p>In a cyclic stack {@link #getNextZoneFor} wraps from the last zone to the first,
 * and {@link #getPreviousZoneFor} wraps from the first zone to the last.
 * In a linear stack both methods return {@code null} at the respective endpoints,
 * matching the original behaviour.
 */
public record ZoneStack(int totalZones, WarpZone bottomZone, WarpZone topZone,
                        WarpZone baseZone, List<WarpZone> stack, boolean cyclic) {

    /**
     * Returns the zone below {@code warpZone} in the stack, or {@code null} if this is
     * the bottom of a linear stack. For cyclic stacks the list wraps around.
     */
    public @Nullable WarpZone getPreviousZoneFor(WarpZone warpZone) {
        int idx = stack.indexOf(warpZone);
        if (idx == -1) throw new IllegalArgumentException("Zone is not part of this ZoneStack!");
        if (idx == 0) return cyclic ? stack.getLast() : null;
        return stack.get(idx - 1);
    }

    /**
     * Returns the zone above {@code warpZone} in the stack, or {@code null} if this is
     * the top of a linear stack. For cyclic stacks the list wraps around.
     */
    public @Nullable WarpZone getNextZoneFor(WarpZone warpZone) {
        int idx = stack.indexOf(warpZone);
        if (idx == -1) throw new IllegalArgumentException("Zone is not part of this ZoneStack!");
        if (idx == stack.size() - 1) return cyclic ? stack.getFirst() : null;
        return stack.get(idx + 1);
    }
}
