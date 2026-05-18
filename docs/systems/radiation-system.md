# Radiation System

This document describes the runtime architecture of the radiation
subsystem: how blocks become radioactive, how the radiation field
propagates between chunks, how a player accumulates and dissipates
radiation, and which configuration knobs control all of this.

It is meant for contributors that want to add a new radioactive item,
tweak the simulation, or debug a misbehaving chunk.

## 1. Components at a glance

```
            ┌──────────────────────┐
            │      HazardSystem    │  ← item-level radioactivity registry
            └──────────┬───────────┘
                       │ getHazardLevelFromStack(item, RADIATION)
                       ▼
┌────────────────────────────────────────────────────┐
│                ChunkRadiationManager               │  ← Forge/Fabric event hub
│ • onWorldLoad / onWorldUnload                      │
│ • onChunkLoad / onChunkUnload                      │
│ • onServerTick (every 20 ticks → updateSystem)     │
│ • onBlockPlace / onBlockBreak / onExplosionDetonate│
└──────────┬─────────────────────────────────────────┘
           │   delegates to
           ▼
┌────────────────────────────────────────────────────┐
│        ChunkRadiationHandlerSimple (active impl)   │
│ • per-dimension Set<ChunkPos> activeChunks         │
│ • 3-phase tick: READ → SPREAD → APPLY              │
│ • per-chunk IChunkRadiation capability (CCA/Forge) │
└──────────┬─────────────────────────────────────────┘
           │
           ▼
┌────────────────────────────────────────────────────┐
│   PlayerHandler (Architectury common entry point)  │
│ • tracks per-UUID accumulated rads in a HashMap    │
│ • saves to PlayerPersistentData on quit            │
│ • once per second: chunkRad + invRad − armor       │
│ • triggers MobEffects / damage at config thresholds│
└────────────────────────────────────────────────────┘
```

Files involved:

- `com.hbm_m.radiation.ChunkRadiationManager` — singleton event hub.
- `com.hbm_m.radiation.ChunkRadiationHandler` — abstract API.
- `com.hbm_m.radiation.ChunkRadiationHandlerSimple` — only enabled
  implementation today (the `PRISM` variant is parked behind a commented
  config toggle).
- `com.hbm_m.radiation.PlayerHandler` — Architectury-common
  per-player accumulation loop.
- `com.hbm_m.capability.ChunkRadiation*` — per-chunk capability
  exposing `ambientRadiation` + `blockRadiation`.
- `com.hbm_m.hazard.HazardSystem` + `HazardType` + `ModHazards` —
  item-level hazard registry (RADIATION is just one HazardType).

## 2. Where radiation comes from

`HazardSystem` is the **single source of truth** for "how radioactive is
this item or block in isolation". Rules can be registered against:

- a specific `Item` (highest priority);
- a `Block` (delegates to `block.asItem()`);
- a `TagKey<Item>` (lowest priority).

Registration is done once at mod startup from
`com.hbm_m.hazard.ModHazards.registerHazards()` — see that file for the
full table of items, tags and their `HazardEntry(HazardType.RADIATION,
value)` payload.

`HazardType` currently models five hazards:

| Type | Tooltip color | Used by |
| --- | --- | --- |
| `RADIATION` | green | chunk + player simulation |
| `DIGAMMA` | red | endgame digamma exposure |
| `HYDRO_REACTIVE` | red | reacts violently with water |
| `EXPLOSIVE_ON_FIRE` | red | gunpowder / dynamite ignition |
| `PYROPHORIC` | gold | spontaneous ignition |

Only `RADIATION` feeds the chunk simulation. To add a new radioactive
material, register it in `ModHazards` — the chunk simulation and player
inventory loop will pick it up automatically via
`HazardSystem.getHazardLevelFromStack(stack, HazardType.RADIATION)`.

> Performance: `HazardSystem` caches its lookup by `Item`. The cost of
> looking up a known radioactive item is a single `ConcurrentHashMap`
> read; designing world simulations on top of this map is safe.

## 3. Chunk-level simulation

### 3.1 Per-chunk state

Each loaded chunk carries an `IChunkRadiation` capability with two
floats:

- `blockRadiation` — sum of static `HazardSystem` contributions from
  blocks currently placed in the chunk. Recomputed from scratch in
  `recalculateChunkRadiation` on chunk load and on block place / break /
  explosion.
- `ambientRadiation` — the diffused radiation field. Driven by the
  simulation tick.

The handler also keeps an in-memory
`Map<ResourceLocation /*dimension*/, Set<ChunkPos>> activeChunksByDimension`.
A chunk is **active** while either of its two values is `> 1e-6`.
Inactive chunks are excluded from the tick loop entirely; this is the
main reason the simulation scales.

### 3.2 The 20-tick tick (`updateSystem`)

`ChunkRadiationManager.onServerTick` increments a counter and calls
`getProxy().updateSystem()` every 20 server ticks (≈ once per second)
when `enableRadiation && enableChunkRads` are true. `ModClothConfig`
gates this behind config so the system can be turned off entirely.

Each invocation operates per dimension in three phases:

1. **Read.** Snapshot `ambient` and `block` values of every active
   chunk into two temporary maps. Reading before writing prevents
   ordering-dependent simulation drift.
2. **Spread.** For each active chunk, distribute 95 % of its ambient
   value to its 3×3 chunk neighbourhood with the kernel

   | dx \ dz | -1 | 0 | +1 |
   | --- | --- | --- | --- |
   | -1 | 0.025 | 0.075 | 0.025 |
   |  0 | 0.075 | **0.60** | 0.075 |
   | +1 | 0.025 | 0.075 | 0.025 |

   (i.e. 60 % stays, 30 % spreads to the four cardinal neighbours, 10 %
   to the diagonals; the remaining 5 % is lost to leakage). Neighbour
   shares below `0.1` per-tick are dropped to avoid endlessly spreading
   numerical noise.
3. **Apply.** For every chunk that received any spread share or was
   already active, compute the new ambient value:

   ```
   newAmbient  = spreadIn
   newAmbient += generation * radSourceInfluenceFactor   // from blockRadiation
   newAmbient -= newAmbient * 0.05 + 0.1                 // decay (percent + flat)
   newAmbient *= 1 + (rand-0.5) * radRandomizationFactor // optional noise
   newAmbient  = clamp(newAmbient, 0, maxRad)
   ```

   Chunks whose ambient drops below `0.01` are reset to zero and, if
   `blockRadiation` is also zero, removed from the active set.

The handler then optionally:

- spawns the `RAD_FOG_PARTICLE` particle when
  `enableRadFogEffect && ambient > radFogThreshold` (with chance
  `1 / radFogChance`); and
- calls `handleWorldDestruction(...)` when `worldRadEffects` is on and
  ambient exceeds `worldRadEffectsThreshold` — this is what turns grass
  / leaves into their dead variants. Look at
  `ChunkRadiationHandlerSimple.handleWorldDestruction` for the full
  cell-by-cell rules.

Finally `sendDebugPackets(level)` ships a compact
`ChunkRadiationDebugBatchPacket` to operators with the debug overlay
open.

### 3.3 Recalculation triggers

`ChunkRadiationManager` listens to enough events to keep
`blockRadiation` correct without scanning every chunk every tick:

| Event | Action |
| --- | --- |
| Chunk load | `recalculateChunkRadiation(chunk)` (full scan) |
| Chunk unload | drop chunk from active set |
| Block place / break | `handleBlockChange` — adds the *delta* between the old and new block's hazard level to that chunk via `incrementBlockRadiation` |
| Explosion detonate | for every affected position, treat it as a block break (`oldState → AIR`) |
| Player log-out (Forge) | clears per-player debug overlay cache |

Block-change handling computes only the **delta** rather than re-scanning,
so individual block edits are O(1). Full recalculation is reserved for
`chunkLoad` and explicit recomputes (e.g. `BlockEvent.PLACE` on Fabric
where the previous block is not provided).

## 4. Player accumulation

`com.hbm_m.radiation.PlayerHandler` is registered from the common
Architectury entry point and runs on every `TickEvent.PLAYER_POST`:

1. Skip if the world is client-side, the player is dead, or the player
   is in creative / spectator.
2. Once every 20 ticks gather:
   - `chunkRad = ChunkRadiationManager.getRadiation(level, x, y, z)` —
     the ambient field at the player's block;
   - `invRad`  = sum of `HazardSystem.getHazardLevelFromStack(...)` over
     `inventory.items`, `inventory.armor`, and the offhand.
3. Subtract armour:
   `protectionPercent = ArmorModificationHelper.convertAbsoluteToPercent(Σ rad protection)`.
4. Add `(chunkRad + invRad) * (1 - protectionPercent)` to the player's
   stored value (`playerRads: UUID → Float`).
5. Decay by `radDecay` per second.
6. Run `applyRadiationEffects` against the current accumulated value.

`applyRadiationEffects` applies cumulative `MobEffectInstance`s using
the `radSickness`, `radWater`, `radConfusion`, `radBlindness` thresholds
from config, deals `radDamage` damage per tick above
`radDamageThreshold`, awards the `hbm_m:radiation_200` and
`hbm_m:radiation_1000` advancements, and kills the player with
`ModDamageSources.radiation(level)` when `rads ≥ maxPlayerRad`.

Persistence is handled via `PlayerPersistentData` (a thin wrapper around
`getPersistentData()` / `getAttachedData()`):

- `onPlayerJoin` reads `hbm_m_player_radiation.radiationLevel`.
- `onPlayerQuit` writes it back.
- `onPlayerRespawn` resets to zero unless the death was End-conquering.

The client receives a `RadiationDataPacket(envRad, accumulated)` once per
second and on every set call, used by the HUD / pixel-effect overlay.

## 5. Commands

Registered in `PlayerHandler.onRegisterCommands`. All require permission
level 2 (server operator) by default.

```
/hbm_m rad [<targets>] clear
/hbm_m rad [<targets>] add    <amount>
/hbm_m rad [<targets>] remove <amount>
```

When `<targets>` is omitted the command acts on the executor (`@s`).
`amount` is a float; `clear` resets the accumulated value to zero
without touching the chunk field.

## 6. Configuration reference

All knobs live in `com.hbm_m.config.ModClothConfig` and are exposed via
the Cloth Config / Mod Menu UI.

### `general`

| Key | Default | Effect |
| --- | --- | --- |
| `enableRadiation`  | `true` | Master switch. Disables player and chunk simulation when false. |
| `enableChunkRads`  | `true` | Independent switch for the chunk simulation; player only feels inventory radiation when off. |

### `player`

| Key | Default | Effect |
| --- | --- | --- |
| `maxPlayerRad`        | `100`  | Lethal threshold (causes death + reset). |
| `radDecay`            | `0.01` | Per-second natural decay of accumulated rads. |
| `radDamage`           | `0.05` | Damage applied per second above `radDamageThreshold`. |
| `radDamageThreshold`  | `200`  | Above this, the player takes magic damage. |
| `radSickness`         | `300`  | Adds `HUNGER` + `POISON`. |
| `radWater`            | `500`  | Adds `WEAKNESS` III. |
| `radConfusion`        | `700`  | Adds `CONFUSION`. |
| `radBlindness`        | `900`  | Adds `BLINDNESS`. |

> Note: the defaults above are read straight from `ModClothConfig`.
> Some thresholds therefore sit *above* `maxPlayerRad`, which means
> with default settings the player will die at 100 RAD before reaching
> the sickness/poison effects. Lower `radDecay` / raise `maxPlayerRad`
> if you want to actually exercise those tiers.

### `chunk`

| Key | Default | Effect |
| --- | --- | --- |
| `maxRad`                  | `100_000` | Hard clamp on ambient per chunk. |
| `radChunkDecay`           | `0.1`     | Reserved (current handler uses fixed 5 % + 0.1). |
| `radChunkSpreadFactor`    | `0.2`     | Legacy spread factor (handler currently uses the 0.95 kernel above). |
| `radSpreadThreshold`      | `0.01`    | Below this an ambient value is dropped to zero. |
| `radSourceInfluenceFactor`| `0.08`    | How strongly `blockRadiation` injects into `ambient` per tick. |
| `radRandomizationFactor`  | `1.0`     | Noise multiplier on the post-decay value. |

### `world_effects`

| Key | Default | Effect |
| --- | --- | --- |
| `worldRadEffects`              | `true` | Master switch for grass/leaf mutation. |
| `worldRadEffectsThreshold`     | `500`  | Minimum ambient before mutation can occur. |
| `worldRadEffectsBlockChecks`   | `10`   | How many candidates inspected per tick (1–100). |
| `worldRadEffectsMaxScaling`    | `4.0`  | Probability scaling cap. |
| `worldRadEffectsMaxDepth`      | `5`    | Vertical reach below the surface (1–16). |
| `enableRadFogEffect`           | `true` | Spawns `RAD_FOG_PARTICLE` clouds. |
| `radFogThreshold`              | `50`   | Minimum ambient for fog. |
| `radFogChance`                 | `10`   | `1/N` per-chunk roll per tick. |

## 7. Adding new radioactive content

Common recipes:

- **A radioactive ingot/item.** Add a `HazardSystem.register(item,
  new HazardData(new HazardEntry(HazardType.RADIATION, level)))` call in
  `ModHazards.registerHazards()`. Inventory radiation, tooltips and
  chunk block-radiation will all pick it up automatically.
- **A whole tag of items (e.g. all uranium ingots from any mod).**
  Register against a `TagKey<Item>` instead. Tag rules have lower
  priority than per-item rules, so per-item overrides still win.
- **A radioactive block.**
  `HazardSystem.register(block, new HazardData(...))` — this internally
  delegates to `block.asItem()`. After placement,
  `ChunkRadiationManager.onBlockPlace` will pick up the delta.
- **Radiation-resistant armour.** Call
  `HazardSystem.registerArmorProtection(armorItem, absoluteProtection)`
  during init. Protection values stack across slots and are converted
  to a percentage by `ArmorModificationHelper.convertAbsoluteToPercent`.

## 8. Troubleshooting

- **Player not gaining radiation in a hot chunk.** Confirm
  `enableRadiation` and `enableChunkRads` are both on, that the player
  is not creative/spectator, and that no armour is providing 100 %
  resistance. Toggle `enableDebugLogging` in config to surface the
  per-tick log line `Add total radiation to player … chunk=… inv=… …`.
- **Chunk ambient never drops.** Check that
  `radChunkDecay`/`worldRadEffectsThreshold` etc. were not lowered to
  the point where new injection outpaces decay. The simulator also
  prints `[RadSim] Tick update for chunk […]` lines under debug logging.
- **New radioactive block has no effect.** Make sure the block is
  registered as a `BlockItem` (i.e. accepted into `MainRegistry`).
  Blocks without an item form cannot be addressed by `HazardSystem`.
- **Fabric quirk.** `BlockEvent.PLACE` on Architectury does not
  expose the pre-place state; the Fabric path falls back to a full
  `recalculateChunkRadiation` for the chunk. Heavy chunk edits there
  will therefore be slightly more expensive than on Forge.
