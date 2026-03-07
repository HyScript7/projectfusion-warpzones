# Usage Guide

This page walks through creating, linking, and using warp zones on your server.

## Creating a Warp Zone

1. **Make a WorldEdit selection**
   - Use WorldEdit to select a cuboid region that will act as a single floor/zone.
2. **Stand inside the selection**
   - Face the direction you want players to face when they arrive in this zone.
3. **Run the create command**
   - Use:
     ```
     /warpzones create <name>
     ```
   - Replace `<name>` with a unique identifier for the zone (e.g. `lobby_floor1`).
   - Your current yaw is snapped to the nearest cardinal direction and stored as the zone's orientation.

## Linking Zones

Once you have multiple zones, you can link them vertically to form an elevator.

- Use:
  ```
  /warpzones link <belowZone> <topZone>
  ```
- Example:
  ```
  /warpzones link lobby_floor1 lobby_floor2
  ```
- The link is bidirectional:
  - Jumping inside `belowZone` sends players to `topZone`.
  - Sneaking inside `topZone` sends players back down to `belowZone`.

You can chain multiple zones (e.g. floor 1 → 2 → 3) and even form loops; the plugin will detect and handle cyclic stacks for display in the action bar.

## Using Zones In-Game

For players standing inside a zone and with the correct permissions:

- **Jump** (space bar) to move *up* to the next linked zone.
- **Sneak** (shift) to move *down* to the previous linked zone.

When a player enters a zone:
- They hear a short chime sound to indicate the zone was detected.
- Their action bar shows a compact HUD:
  - The current zone name.
  - Their position in the stack (e.g. `[2/3]`).
  - Indicators for available directions (jump / sneak), including special formatting for cyclic loops.

## Listing and Inspecting Zones

- Use:
  ```
  /warpzones list
  ```
- This lists all zones, including:
  - Name and world.
  - Corner coordinates of the bounding box.
  - Link information (up/down neighbors by name, or `<missing>` if a link points to a non-existent zone).

## Deleting Zones

To remove a zone:

``` 
/warpzones delete <name>
```

- The zone is removed from storage and from the in-memory cache.
- Any links from adjacent zones pointing to it are automatically cleared so that stacks remain consistent.

Players inside a deleted zone will simply no longer be considered in any warp zone and will not be able to use jump/sneak to move between floors there.
