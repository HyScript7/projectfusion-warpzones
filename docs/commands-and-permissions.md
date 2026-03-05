# Commands & Permissions

## Commands

All commands are registered under the root `/warpzones` literal.

| Command                                  | Description                                                          | Permission                          |
|------------------------------------------|----------------------------------------------------------------------|-------------------------------------|
| `/warpzones list`                        | List all registered warp zones, their bounds, and link status.      | `projectfusion.warpzones.admin.list` |
| `/warpzones create <name>`               | Create a new warp zone from your current WorldEdit selection.       | `projectfusion.warpzones.admin.create` |
| `/warpzones delete <name>`               | Delete a named warp zone and repair adjacent links.                 | `projectfusion.warpzones.admin.delete` |
| `/warpzones link <belowZone> <topZone>`  | Link two existing zones so jump/sneak moves players between them.   | `projectfusion.warpzones.admin.link` |

If no zones exist, `/warpzones list` will tell you that none have been registered yet.

## Permissions

### World-use Permissions

Players must be granted a per-world "use" permission to travel through zones in that world:

- Pattern: `projectfusion.warpzones.use.<worldname>`
- Example nodes for vanilla worlds:
  - `projectfusion.warpzones.use.world` – Overworld
  - `projectfusion.warpzones.use.world_nether` – Nether
  - `projectfusion.warpzones.use.world_the_end` – End

The world name comes from the Bukkit world name, lowercased. For a custom world named `SkyHub`, the node would be:

- `projectfusion.warpzones.use.skyhub`

### Admin Permissions

These control who can manage zones:

- `projectfusion.warpzones.admin.list` – Use `/warpzones list`.
- `projectfusion.warpzones.admin.create` – Use `/warpzones create`.
- `projectfusion.warpzones.admin.link` – Use `/warpzones link`.
- `projectfusion.warpzones.admin.delete` – Use `/warpzones delete`.

### Defaults

By default (as declared in `paper-plugin.yml`):

- All admin permissions default to OP.
- World-use permissions for `world`, `world_nether`, and `world_the_end` also default to OP.

Use your permissions plugin (e.g. LuckPerms) to grant these nodes to specific groups or players as needed.
