# Persistence

ProjectFusion Warpzones stores all zones using an in-memory cache backed by a persistent store. The default implementation uses SQLite for fast, crash‑resistant storage.

## Default: SQLite

By default, the plugin uses `SqliteWarpZoneRepository`:

- Data file: `plugins/ProjectFusionWarpzones/warpzones.db`
- Characteristics:
  - Single-row `UPSERT`/`DELETE` per change (no full-file rewrites).
  - Uses SQLite WAL (write-ahead logging) mode so writes do not block reads.
  - Zones are loaded into memory on startup for O(1) lookups during gameplay.

No extra configuration is required; the database file is created on first run.

## YAML (Legacy) Backend

The codebase also includes a YAML-backed repository (`YamlWarpZoneRepository`) which:

- Stores zones in `warpzones.yml` under the plugin data folder.
- Uses a periodic auto-save task (every 60 seconds) to flush in-memory state to disk.
- Rewrites the entire `warpzones` section on each save to ensure deletions are reflected.

The main plugin entry point currently wires the SQLite repository; the YAML backend is primarily kept for reference or alternative setups.

## In-Memory Repository

`InMemoryWarpZoneRepository` provides the core map-based store used by both backends:

- UUID → `WarpZone` map for primary storage.
- Name → UUID index for constant-time name lookups.

It is not intended to be used directly in production, as it does not persist data across restarts on its own.

## Backups

- To back up your zones, stop the server and copy the plugin data folder (`plugins/ProjectFusionWarpzones/`).
- Restoring is as simple as copying the folder back before starting the server.
