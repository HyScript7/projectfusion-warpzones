# Installation

## Requirements

- **Server:** Paper (or a compatible fork) targeting Minecraft 1.21.11.
- **Dependencies:**
  - [WorldEdit](https://enginehub.org/worldedit/) – required for region selection.

## Installing the Plugin

1. **Download or build** the plugin JAR.
   - To build from source, run:
     ```bash
     ./gradlew build
     ```
   - The JAR will be in `build/libs/`.
2. **Install dependencies**
   - Ensure WorldEdit is installed on the server.
3. **Deploy the plugin**
   - Place the ProjectFusion Warpzones JAR into your server's `plugins` directory.
4. **Start the server**
   - On first run, the plugin will create its data folder under `plugins/ProjectFusionWarpzones/`.
   - A SQLite database file (`warpzones.db`) will be created there to store zones.

## Upgrading

- Stop the server.
- Replace the old ProjectFusion Warpzones JAR with the new version.
- Start the server again.
- Existing zones stored in the database will be migrated automatically by the plugin code if needed.

## Uninstalling

1. Remove the plugin JAR from the `plugins` folder.
2. (Optional) Delete the plugin data folder (`plugins/ProjectFusionWarpzones/`) if you no longer need stored zones.
