package io.github.hyscript7.projectfusion.warpzones;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class WarpZone implements ConfigurationSerializable {
    private final UUID warpZoneUuid;
    private final String name;
    private final World world;
    private final Location corner1;
    private final Location corner2;
    private final int minXCorner;
    private final int maxXCorner;
    private final int minZCorner;
    private final int maxZCorner;
    private final double minX;
    private final double maxX;
    private final double minY;
    private final double maxY;
    private final double minZ;
    private final double maxZ;
    private final double yaw;
    private UUID nextWarpZoneUuid;     // Up
    private UUID previousWarpZoneUuid; // Down

    public WarpZone(String name, World world, Location corner1, Location corner2, double yaw) {
        this(UUID.randomUUID(), name, world, corner1, corner2, yaw);
    }

    public WarpZone(UUID warpZoneUuid, String name, World world, Location corner1, Location corner2, double yaw) {
        if (!cornerWorldsMatchZoneWorld(world, corner1, corner2)) {
            throw new IllegalArgumentException("Corners are not in the same world");
        }

        this.warpZoneUuid = warpZoneUuid;
        this.name = name;
        this.world = world;

        this.minXCorner = Math.min(corner1.getBlockX(), corner2.getBlockX());
        this.maxXCorner = Math.max(corner1.getBlockX(), corner2.getBlockX());
        this.minZCorner = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
        this.maxZCorner = Math.max(corner1.getBlockZ(), corner2.getBlockZ());

        this.minX = Math.min(corner1.getX(), corner2.getX());
        this.maxX = Math.max(corner1.getX(), corner2.getX());

        this.minY = Math.min(corner1.getY(), corner2.getY());
        this.maxY = Math.max(corner1.getY(), corner2.getY());

        this.minZ = Math.min(corner1.getZ(), corner2.getZ());
        this.maxZ = Math.max(corner1.getZ(), corner2.getZ());

        this.corner1 = new Location(world, minX, minY, minZ);
        this.corner2 = new Location(world, maxX, maxY, maxZ);

        this.yaw = yaw;
    }

    /** Full deserialization constructor — includes linked-zone UUIDs. */
    private WarpZone(UUID warpZoneUuid, String name, UUID previousWarpZoneUuid, UUID nextWarpZoneUuid,
                     World world, Location corner1, Location corner2, double yaw) {
        this(warpZoneUuid, name, world, corner1, corner2, yaw);
        this.previousWarpZoneUuid = previousWarpZoneUuid;
        this.nextWarpZoneUuid = nextWarpZoneUuid;
    }

    // -------------------------------------------------------------------------
    // Geometry helpers
    // -------------------------------------------------------------------------

    private static boolean cornerWorldsMatchZoneWorld(World world, Location corner1, Location corner2) {
        return corner1.getWorld().equals(world) && corner2.getWorld().equals(world);
    }

    public boolean containsLocation(@NotNull Location location) {
        if (location.getWorld() == null) return false;
        if (!location.getWorld().equals(world)) return false;

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        return x >= minX && x < maxX + 1
                && y >= minY && y < maxY + 1
                && z >= minZ && z < maxZ + 1;
    }

    /**
     * Returns the packed chunk keys (as used by {@link org.bukkit.Chunk#getChunkKey()})
     * for every chunk that overlaps this zone's bounding box.
     */
    public long[] getChunks() {
        int minChunkX = minXCorner >> 4;
        int maxChunkX = maxXCorner >> 4;
        int minChunkZ = minZCorner >> 4;
        int maxChunkZ = maxZCorner >> 4;

        int countX = maxChunkX - minChunkX + 1;
        int countZ = maxChunkZ - minChunkZ + 1;
        long[] chunks = new long[countX * countZ];

        int i = 0;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                chunks[i++] = (cx & 0xFFFFFFFFL) | ((cz & 0xFFFFFFFFL) << 32);
            }
        }
        return chunks;
    }

    /**
     * Maps a location inside {@code other} to the corresponding position inside this zone,
     * accounting for any yaw rotation between the two zones.
     */
    public Location transitionFrom(WarpZone other, Location loc) {
        double otherExtentX = (other.maxX + 1) - other.minX;
        double otherExtentY = (other.maxY + 1) - other.minY;
        double otherExtentZ = (other.maxZ + 1) - other.minZ;

        double relX = (loc.getX() - other.minX) / otherExtentX;
        double relY = (loc.getY() - other.minY) / otherExtentY;
        double relZ = (loc.getZ() - other.minZ) / otherExtentZ;

        double yawDelta    = this.yaw - other.yaw;
        double yawDeltaRad = Math.toRadians(yawDelta);
        double cos = Math.cos(yawDeltaRad);
        double sin = Math.sin(yawDeltaRad);

        double cx = relX - 0.5;
        double cz = relZ - 0.5;

        double newRelX = Math.max(0.0, Math.min(1.0, (cx * cos - cz * sin) + 0.5));
        double newRelZ = Math.max(0.0, Math.min(1.0, (cx * sin + cz * cos) + 0.5));
        relY           = Math.max(0.0, Math.min(1.0, relY));

        double destX = this.minX + newRelX * ((this.maxX + 1) - this.minX);
        double destY = this.minY + relY    * ((this.maxY + 1) - this.minY);
        double destZ = this.minZ + newRelZ * ((this.maxZ + 1) - this.minZ);

        float newYaw = (float) ((loc.getYaw() + yawDelta) % 360);

        return new Location(this.world, destX, destY, destZ, newYaw, loc.getPitch());
    }

    // -------------------------------------------------------------------------
    // Getters / setters
    // -------------------------------------------------------------------------

    public UUID getWarpZoneUuid()  { return warpZoneUuid; }
    public String getName()         { return name; }
    public World getWorld()         { return world; }
    public Location getCorner1()    { return corner1; }
    public Location getCorner2()    { return corner2; }
    public double getYaw()          { return yaw; }

    public UUID getNextWarpZoneUuid()     { return nextWarpZoneUuid; }
    public UUID getPreviousWarpZoneUuid() { return previousWarpZoneUuid; }

    public void setNextWarpZoneUuid(UUID nextWarpZoneUuid)         { this.nextWarpZoneUuid = nextWarpZoneUuid; }
    public void setPreviousWarpZoneUuid(UUID previousWarpZoneUuid) { this.previousWarpZoneUuid = previousWarpZoneUuid; }

    // -------------------------------------------------------------------------
    // Identity
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof WarpZone warpZone)) return false;
        return Objects.equals(warpZoneUuid, warpZone.warpZoneUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(warpZoneUuid);
    }

    // -------------------------------------------------------------------------
    // ConfigurationSerializable
    // -------------------------------------------------------------------------

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("uuid",              warpZoneUuid.toString());
        map.put("name",              name);
        map.put("previousZoneUuid",  previousWarpZoneUuid != null ? previousWarpZoneUuid.toString() : null);
        map.put("nextZoneUuid",      nextWarpZoneUuid     != null ? nextWarpZoneUuid.toString()     : null);
        map.put("world",             world.getName());
        map.put("corner1",           corner1);
        map.put("corner2",           corner2);
        map.put("yaw",               yaw);
        return map;
    }

    public static WarpZone deserialize(Map<String, Object> map) {
        UUID warpZoneUuid = UUID.fromString((String) map.get("uuid"));

        // Fall back to UUID string for zones saved before names were introduced.
        String name = map.containsKey("name") ? (String) map.get("name") : warpZoneUuid.toString();

        String prevStr = (String) map.get("previousZoneUuid");
        UUID previousZoneUuid = prevStr != null ? UUID.fromString(prevStr) : null;

        String nextStr = (String) map.get("nextZoneUuid");
        UUID nextZoneUuid = nextStr != null ? UUID.fromString(nextStr) : null;

        World world = Bukkit.getWorld((String) map.get("world"));
        double yaw  = map.get("yaw") != null ? ((Number) map.get("yaw")).doubleValue() : 0.0;

        return new WarpZone(
                warpZoneUuid, name,
                previousZoneUuid, nextZoneUuid,
                world,
                (Location) map.get("corner1"),
                (Location) map.get("corner2"),
                yaw
        );
    }
}
