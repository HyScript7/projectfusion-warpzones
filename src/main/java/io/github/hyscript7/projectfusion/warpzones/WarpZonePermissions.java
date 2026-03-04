package io.github.hyscript7.projectfusion.warpzones;

import org.bukkit.World;

/**
 * All permission nodes used by ProjectFusion Warpzones.
 *
 * <h3>Structure</h3>
 * <pre>
 * projectfusion.warpzones
 * ├── use
 * │   └── &lt;worldname&gt;   — allow a player to travel through zones in a specific world
 * └── admin
 *     ├── list           — /warpzones list
 *     ├── create         — /warpzones create
 *     ├── link           — /warpzones link
 *     └── delete         — /warpzones delete
 * </pre>
 *
 * <p>Permission nodes are based on the lowercased world name so that they remain
 * consistent across server restarts and case-insensitive permission plugins
 * (e.g. the world {@code MyWorld} yields {@code projectfusion.warpzones.use.myworld}).
 */
public final class WarpZonePermissions {

    private WarpZonePermissions() {}

    private static final String ROOT  = "projectfusion.warpzones";
    private static final String USE   = ROOT  + ".use";
    private static final String ADMIN = ROOT  + ".admin";

    // -------------------------------------------------------------------------
    // Admin command nodes
    // -------------------------------------------------------------------------

    public static final String ADMIN_LIST   = ADMIN + ".list";
    public static final String ADMIN_CREATE = ADMIN + ".create";
    public static final String ADMIN_LINK   = ADMIN + ".link";
    public static final String ADMIN_DELETE = ADMIN + ".delete";

    // -------------------------------------------------------------------------
    // World-use node
    // -------------------------------------------------------------------------

    /**
     * Returns the per-world use permission node for {@code world}.
     *
     * <p>Example: a world named {@code world_nether} produces
     * {@code projectfusion.warpzones.use.world_nether}.
     */
    public static String worldUse(World world) {
        return USE + "." + world.getName().toLowerCase();
    }
}
