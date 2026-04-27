# PlayerInteractionsUtils

PlayerInteractionsUtils tags player-caused interactions on Spigot servers so other plugins can attribute blocks, entities, and damage back to the responsible player. The plugin prioritizes lightweight PersistentDataContainer (PDC) tags and seamlessly falls back to disk-backed storage on platforms where PDC persistence is unreliable.

## Highlights
- **Block ownership tracking:** Records who placed a block and propagates ownership through growth and transformations.
- **Entity spawn attribution:** Tags natural, egg, spawn-egg, and breeding spawns with the player that triggered them, respecting entity/tag whitelists.
- **Damage accounting:** Stores the last player hitter and short-lived per-player damage tallies for non-player entities.
- **Player counters:** Maintains on-disk counters for key actions (placements, spawn causes, damage updates) for auditing or achievements.
- **Resilient storage:** Uses chunk/entity PDC when available and transparently switches to YAML files under the plugin data folder when PDC persistence is disabled.

## Installation and configuration
1. Drop the plugin JAR into your server's `plugins/` directory and start the server once to generate `config.yml`.
2. Toggle features in `config.yml` under `features.*` to match your needs. For example, disable block tagging if you only want entity attribution.
3. If your platform wipes PDC on restart, set `storage.chunk-pdc-enabled` or `storage.entity-pdc-enabled` to `false` to enable disk-backed storage (`plugins/PlayerInteractionsUtils/blocks/` and `entities/`).
4. Restrict which entities are tagged by adding vanilla or namespaced IDs (e.g., `minecraft:cow`) or tag keys (e.g., `#minecraft:animals`) to `whitelists.entity-tags`.

## Usage examples
- **Find who placed a block (storage-agnostic):**
  ```java
  BlockTagStorage tags = new BlockTagStorage(
      new DataKeys(plugin),
      plugin.getConfig().getBoolean("storage.chunk-pdc-enabled", true),
      new BlockDataManager(plugin.getDataFolder(), plugin.getLogger())
  );
  Optional<UUID> owner = tags.getOwner(block);
  ```
- **Attribute a spawned mob:**
  ```java
  PersistentDataContainer pdc = entity.getPersistentDataContainer();
  UUID breeder = UUID.fromString(pdc.get(dataKeys.breedingSpawnPlayer, PersistentDataType.STRING));
  ```
- **Check the last player who hit an entity:**
  ```java
  UUID lastHit = UUID.fromString(
      entity.getPersistentDataContainer().get(dataKeys.lastHitBy, PersistentDataType.STRING)
  );
  long hitAt = entity.getPersistentDataContainer().getOrDefault(dataKeys.lastHitAt, PersistentDataType.LONG, 0L);
  ```
- **Read damage tallies:**
  ```java
  String serialized = entity.getPersistentDataContainer()
      .get(dataKeys.damageByPlayer, PersistentDataType.STRING);
  Map<UUID, DamageTallySerializer.DamageEntry> damage =
      DamageTallySerializer.deserialize(serialized, Instant.now());
  ```

> Tip: When PDC storage is disabled, block ownership is persisted under `plugins/PlayerInteractionsUtils/blocks/<block-id>.yml` and chunk mappings under `plugins/PlayerInteractionsUtils/chunk-blocks/<chunk-key>.yml`.

## Querying player-placed blocks by area
`BlockTagStorage` now exposes several overloads to enumerate blocks with a stored owner tag.

### 1) By chunk
```java
Set<Block> placedInChunk = tags.getPlayerPlacedBlocks(chunk);
Set<Block> placedInChunkCoords = tags.getPlayerPlacedBlocks(world, chunkX, chunkZ);
```

### 2) By radius around a location or player
```java
Set<Block> sphericalAroundLoc = tags.getPlayerPlacedBlocks(centerLocation, 16.0);
Set<Block> sphericalAroundPlayer = tags.getPlayerPlacedBlocks(player, 16.0);
```

### 3) By axis radii around a location (box-shaped query)
```java
Set<Block> aroundLocAxes = tags.getPlayerPlacedBlocks(centerLocation, 16.0, 8.0, 16.0);
```

### 4) By explicit world-space bounds
```java
Set<Block> byNumericBounds = tags.getPlayerPlacedBlocks(
    world,
    minX, minY, minZ,
    maxX, maxY, maxZ
);

BoundingBox box = BoundingBox.of(minLocation, maxLocation);
Set<Block> byBoundingBox = tags.getPlayerPlacedBlocks(box, world);
```

### 5) By bounds relative to a player
```java
// Asymmetric offsets relative to player's current location
Set<Block> byRelativeOffsets = tags.getPlayerPlacedBlocks(
    player,
    -10, -5, -10,
     20, 15,  20
);

// Symmetric X/Z and Y radius shortcut
Set<Block> byRelativeRadii = tags.getPlayerPlacedBlocks(player, 16, 8);
```

## Integration surface for hook plugins
The following classes and members are most relevant when integrating with PlayerInteractionsUtils:

- **`DataKeys`** (constructor: `DataKeys(JavaPlugin)`)
  - `blockOwner`, `blockGrownFromPlayer`, `blockTransformedFromPlayer`: Namespaced keys for block ownership, growth, and transformation tags in chunk PDC.
  - `eggSpawnPlayer`, `spawnEggSpawnPlayer`, `breedingSpawnPlayer`: Keys for entity spawn attribution stored on the spawned entity PDC.
  - `lastHitBy`, `lastHitAt`, `damageByPlayer`: Keys for last-hit metadata and serialized damage tallies on entity PDC.
- **`BlockTagStorage`** (constructor: `BlockTagStorage(DataKeys, boolean chunkPdcEnabled, BlockDataManager)`) — unified API for reading/writing block ownership data regardless of storage backend.
  - `setOwner(Block, UUID)`, `getOwner(Block)` — write/read the placing player.
  - `setGrownFromPlayer(Block, UUID)`, `getGrownFromPlayer(Block)` — propagate ownership through growth (e.g., crops, saplings).
  - `setTransformedFromPlayer(Block, UUID)`, `getTransformedFromPlayer(Block)` — track transformations (e.g., logs -> stripped logs).
  - `getPlayerPlacedBlocks(Chunk)`, `getPlayerPlacedBlocks(World, int, int)` — query player-placed blocks in a chunk.
  - `getPlayerPlacedBlocks(Location, double)` — spherical query around a location.
  - `getPlayerPlacedBlocks(Location, double, double, double)` — axis-radius box query around a location.
  - `getPlayerPlacedBlocks(BoundingBox, World)`, `getPlayerPlacedBlocks(World, double, double, double, double, double, double)` — world-space bound queries.
  - `getPlayerPlacedBlocks(Player, int, int, int, int, int, int)`, `getPlayerPlacedBlocks(Player, int, int)`, `getPlayerPlacedBlocks(Player, double)` — player-relative bound/radius queries.
- **`BlockDataManager`** (constructor: `BlockDataManager(File dataFolder, Logger logger)`) — access YAML-backed block metadata when chunk PDC is disabled.
  - `get(Block)` — obtain mutable `BlockData` for a block.
  - `load(Block)` / `save(Block)` / `saveAllTracked()` — lifecycle helpers for disk persistence.
  - `getOwnedBlocksInChunk(Chunk)` — retrieve owner-tagged block records for a chunk from memory and/or disk.
- **`BlockData`** — disk model for block metadata when PDC is unavailable.
  - `ownerId()`, `grownFromPlayerId()`, `transformedFromPlayerId()` — optional ownership values.
  - `setOwner(UUID)`, `setGrownFromPlayerId(UUID)`, `setTransformedFromPlayerId(UUID)` — mutation helpers used by listeners or hook plugins.
- **`NonPlayerEntityDataManager`** (constructor: `NonPlayerEntityDataManager(File dataFolder, Logger logger)`) — YAML-backed metadata for entities when entity PDC is disabled.
  - `get(UUID)`, `load(UUID)`, `save(UUID)`, `saveAll(Collection<UUID>)`, `saveAllTracked()`, `clear(UUID)` — retrieve and persist entity metadata.
- **`NonPlayerEntityData`** — disk model for spawn attribution and damage when PDC is unavailable.
  - `setSpawnOwner(CreatureSpawnEvent.SpawnReason, UUID)`, `spawnOwner(reason)` — store/resolve the player responsible for a spawn.
  - `recordLastHit(UUID, Instant)`, `lastHitBy()`, `lastHitAt()` — last-hit tracking.
  - `addDamage(UUID, double, Instant)`, `damageTallies(Instant)` — in-memory damage tallies with TTL handling.
- **`PlayerDataManager`** (constructor: `PlayerDataManager(File dataFolder, Logger logger)`) and **`PlayerData`** — per-player counters and damage totals.
  - `PlayerData.increment(CounterType)` — increments counters for placements, spawn causes, tagging operations, and damage records.
  - `PlayerData.addDamage(double)` — accumulates total damage to non-player entities.

These APIs and keys let hook plugins reliably read attribution, enforce protections, or augment their own logging/analytics using the data produced by PlayerInteractionsUtils.
