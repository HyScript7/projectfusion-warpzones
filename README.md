## ProjectFusion Warpzones

ProjectFusion Warpzones is a Paper 1.21.11 plugin that lets admins turn arbitrary 3D regions into stacked “warp zones”. Players inside a zone can jump to go up, or sneak to go down, creating smooth, non-traditional elevators between floors or even between worlds.

### Features
- Define zones using your existing WorldEdit selections.
- Name zones and link them into vertical stacks (or loops).
- Per-world permissions for who may use zones.
- Action-bar HUD that shows the current floor and available directions.
- SQLite-backed storage by default for fast, crash‑resistant persistence.

### Requirements
- Paper or compatible fork targeting Minecraft 1.21.11.
- WorldEdit installed on the server (required dependency).

### Installation
1. Build the plugin with `./gradlew build` or download a prebuilt JAR.
2. Place the JAR into your server's `plugins` directory.
3. Ensure WorldEdit is also installed.
4. Start the server; the plugin will create its data folder and SQLite database (`warpzones.db`).

### Basic Usage
1. Use WorldEdit to create a cuboid selection for the first floor.
2. Stand inside the selection and face the direction players should be oriented when they arrive.
3. Run `/warpzones create <name>`.
4. Repeat for additional floors.
5. Link floors with `/warpzones link <belowZone> <topZone>`.
6. Players standing inside a zone can jump to go up, or sneak to go down, if they have the appropriate permissions.

### Commands
- `/warpzones list` – List all zones, their bounds, and links.
- `/warpzones create <name>` – Create a zone from your current WorldEdit selection.
- `/warpzones delete <name>` – Delete a zone and repair adjacent links.
- `/warpzones link <belowZone> <topZone>` – Link two zones into a vertical pair.

### Permissions
World-use nodes (per world):
- `projectfusion.warpzones.use.<worldname>` – Allow using zones in that world (e.g. `projectfusion.warpzones.use.world_nether`).

Admin nodes:
- `projectfusion.warpzones.admin.list` – Use `/warpzones list`.
- `projectfusion.warpzones.admin.create` – Use `/warpzones create`.
- `projectfusion.warpzones.admin.link` – Use `/warpzones link`.
- `projectfusion.warpzones.admin.delete` – Use `/warpzones delete`.

By default, all permissions are granted to server operators; configure finer control via your permissions plugin.

### Further Documentation
Additional usage and configuration guides live in the `docs/` folder:
- Installation and setup
- Commands and permissions
- Persistence (SQLite/YAML) details
See `docs/` in this repository for more.
