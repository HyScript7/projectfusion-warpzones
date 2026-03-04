package io.github.hyscript7.projectfusion.warpzones.persistence;

import io.github.hyscript7.projectfusion.warpzones.WarpZone;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * SQLite-backed warp-zone repository.
 *
 * <h3>Why this is faster than the YAML implementation</h3>
 * <ul>
 *   <li>Every {@code save()} and {@code delete()} call issues a single SQL
 *       {@code UPSERT}/{@code DELETE} statement — O(1) I/O regardless of how many
 *       zones exist.</li>
 *   <li>No periodic batch-save is needed, so there are no latency spikes and no
 *       risk of losing up to 60 s of changes on a hard crash.</li>
 *   <li>The in-memory cache (inherited from {@link InMemoryWarpZoneRepository}) means
 *       all reads remain O(1) without ever hitting the DB at runtime.</li>
 *   <li>SQLite's WAL journal mode keeps writes non-blocking for concurrent readers.</li>
 * </ul>
 *
 * <h3>Dependency</h3>
 * Add {@code org.xerial:sqlite-jdbc:3.47.x} (or newer) to your build file and
 * shade it into your plugin JAR. No additional setup is required.
 *
 * <h3>Usage</h3>
 * Replace the {@link YamlWarpZoneRepository} instantiation in your plugin's
 * {@code onEnable} with:
 * <pre>{@code
 *   SqliteWarpZoneRepository repo = new SqliteWarpZoneRepository(this);
 *   repo.start();
 *   // ... no periodic auto-save task is needed
 * }</pre>
 */
public class SqliteWarpZoneRepository extends InMemoryWarpZoneRepository {

    private static final String CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS warpzones (
                uuid          TEXT PRIMARY KEY,
                name          TEXT UNIQUE NOT NULL,
                world         TEXT NOT NULL,
                min_x         REAL NOT NULL,
                min_y         REAL NOT NULL,
                min_z         REAL NOT NULL,
                max_x         REAL NOT NULL,
                max_y         REAL NOT NULL,
                max_z         REAL NOT NULL,
                yaw           REAL NOT NULL,
                previous_uuid TEXT,
                next_uuid     TEXT
            );
            """;

    private static final String UPSERT = """
            INSERT INTO warpzones
                (uuid, name, world, min_x, min_y, min_z, max_x, max_y, max_z, yaw, previous_uuid, next_uuid)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT(uuid) DO UPDATE SET
                name          = excluded.name,
                world         = excluded.world,
                min_x         = excluded.min_x,
                min_y         = excluded.min_y,
                min_z         = excluded.min_z,
                max_x         = excluded.max_x,
                max_y         = excluded.max_y,
                max_z         = excluded.max_z,
                yaw           = excluded.yaw,
                previous_uuid = excluded.previous_uuid,
                next_uuid     = excluded.next_uuid;
            """;

    private static final String DELETE_BY_UUID = "DELETE FROM warpzones WHERE uuid = ?;";
    private static final String SELECT_ALL      = "SELECT * FROM warpzones;";

    // -------------------------------------------------------------------------

    private final Logger     log;
    private final Connection connection;

    public SqliteWarpZoneRepository(Plugin plugin) throws SQLException {
        this.log = plugin.getLogger();

        plugin.getDataFolder().mkdirs();
        File dbFile = new File(plugin.getDataFolder(), "warpzones.db");

        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

        // WAL mode: writes don't block reads and vice-versa.
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL;");
            st.execute(CREATE_TABLE);
        }

        loadAllFromDb();
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Call this from {@code onEnable} after construction.
     * Unlike {@link YamlWarpZoneRepository#start(Plugin)}, no background task is
     * scheduled — writes are immediate and synchronous.
     */
    public void start(Plugin plugin) {
        log.info("SQLite warp-zone repository ready.");
    }

    /** Closes the database connection. Call from {@code onDisable}. */
    public void stop() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                log.info("SQLite warp-zone repository closed.");
            }
        } catch (SQLException e) {
            log.severe("Failed to close SQLite connection: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // WarpZoneRepository overrides — write through to DB
    // -------------------------------------------------------------------------

    @Override
    public synchronized void save(WarpZone zone) {
        super.save(zone); // update in-memory cache first

        try (PreparedStatement ps = connection.prepareStatement(UPSERT)) {
            Location c1 = zone.getCorner1();
            Location c2 = zone.getCorner2();

            ps.setString(1,  zone.getWarpZoneUuid().toString());
            ps.setString(2,  zone.getName());
            ps.setString(3,  zone.getWorld().getName());
            ps.setDouble(4,  c1.getX());
            ps.setDouble(5,  c1.getY());
            ps.setDouble(6,  c1.getZ());
            ps.setDouble(7,  c2.getX());
            ps.setDouble(8,  c2.getY());
            ps.setDouble(9,  c2.getZ());
            ps.setDouble(10, zone.getYaw());
            ps.setString(11, zone.getPreviousWarpZoneUuid() != null
                    ? zone.getPreviousWarpZoneUuid().toString() : null);
            ps.setString(12, zone.getNextWarpZoneUuid() != null
                    ? zone.getNextWarpZoneUuid().toString() : null);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.severe("Failed to persist warp zone '" + zone.getName() + "': " + e.getMessage());
        }
    }

    @Override
    public synchronized void delete(WarpZone zone) {
        super.delete(zone); // update in-memory cache first

        try (PreparedStatement ps = connection.prepareStatement(DELETE_BY_UUID)) {
            ps.setString(1, zone.getWarpZoneUuid().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.severe("Failed to delete warp zone '" + zone.getName() + "': " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /** Populates the in-memory cache from the DB at startup. */
    private void loadAllFromDb() throws SQLException {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(SELECT_ALL)) {

            while (rs.next()) {
                WarpZone zone = rowToWarpZone(rs);
                if (zone != null) super.save(zone);
            }
        }
    }

    private @Nullable WarpZone rowToWarpZone(ResultSet rs) {
        try {
            UUID   uuid  = UUID.fromString(rs.getString("uuid"));
            String name  = rs.getString("name");
            World  world = Bukkit.getWorld(rs.getString("world"));

            if (world == null) {
                log.warning("Skipping warp zone '" + name + "': world not loaded.");
                return null;
            }

            Location c1 = new Location(world, rs.getDouble("min_x"),
                    rs.getDouble("min_y"), rs.getDouble("min_z"));
            Location c2 = new Location(world, rs.getDouble("max_x"),
                    rs.getDouble("max_y"), rs.getDouble("max_z"));

            String prevStr = rs.getString("previous_uuid");
            String nextStr = rs.getString("next_uuid");

            WarpZone zone = new WarpZone(uuid, name, world, c1, c2, rs.getDouble("yaw"));
            if (prevStr != null) zone.setPreviousWarpZoneUuid(UUID.fromString(prevStr));
            if (nextStr != null) zone.setNextWarpZoneUuid(UUID.fromString(nextStr));

            return zone;
        } catch (SQLException e) {
            log.severe("Failed to deserialise a warp zone row: " + e.getMessage());
            return null;
        }
    }
}
