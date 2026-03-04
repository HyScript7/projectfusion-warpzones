package io.github.hyscript7.projectfusion.warpzones.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class LocationUtil {
    private LocationUtil() {}

    public static String prettyString(Location location) {
        return "X: " + location.getBlockX() + ", Y: " +  location.getBlockY() + ", Z: " + location.getBlockZ();
    }

    public static String serialize(Location loc) {
        return loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
    }

    public static Location deserialize(String locStr) {
        String[] split = locStr.split("_");
        String worldName = split[0];
        int x = Integer.parseInt(split[1]);
        int y = Integer.parseInt(split[2]);
        int z = Integer.parseInt(split[3]);
        World world = Bukkit.getWorld(worldName);
        return new Location(world, x, y, z);
    }
}
