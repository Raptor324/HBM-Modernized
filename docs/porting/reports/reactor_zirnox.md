# Fidelity Audit — `reactor_zirnox` (ZIRNOX Reactor)

- **Date:** 2026-11-22
- **Auditor:** read-only fidelity audit, working-tree based (git HEAD not trusted)
- **Original (1.7.10 canon):** `C:\Projects\Hbm-s-Nuclear-Tech-GIT\src\main\java\com\hbm\`
- **Port:** `C:\Projects\HBM-Modernized-agents\src\main\java\com\hbm_m\`
- **Verdict: MINOR** — core mechanics, meltdown choreography, GUI, tilt, debris, hazards and constants are 1:1; a handful of peripheral deviations (destroyed-core radiation tick rate, wreck drops, ROR slot introspection, fluid-port generalization) and justified integrations absent.

## Scope

| Topic | Original | Port |
|---|---|---|
| Block + structure | `blocks/machine/ReactorZirnox.java`, `blocks/machine/ZirnoxDestroyed.java` | `block/machines/MachineZirnoxBlock.java`, `MachineZirnoxDestroyedBlock.java` |
| Tile entity logic | `tileentity/machine/TileEntityReactorZirnox.java` (703 L), `TileEntityZirnoxDestroyed.java` (105 L) | `blockentity/machines/MachineZirnoxBlockEntity.java` (867 L), `MachineZirnoxDestroyedBlockEntity.java` (136 L) |
| GUI / container | `inventory/gui/GUIReactorZirnox.java`, `inventory/container/ContainerReactorZirnox.java` | `inventory/gui/GUIMachineZirnox.java`, `inventory/menu/MachineZirnoxMenu.java` |
| Debris entity | `entity/projectile/EntityZirnoxDebris.java` (+ `EntityDebrisBase`) | `entity/projectile/ZirnoxDebrisEntity.java` |
| Fuel rod item | `items/machine/ItemZirnoxRod.java` (metadata item, 11 `EnumZirnoxType`s) | `item/industrial/ZirnoxRodItem.java` (11 separate items) |
| Renderers | `render/tileentity/RenderZirnox.java`, `RenderZirnoxDestroyed.java`, `render/entity/projectile/RenderZirnoxDebris.java` | `client/render/implementations/MachineZirnoxRenderer.java`, `MachineZirnoxDestroyedRenderer.java`, `ZirnoxDebrisRenderer.java` |
| Control packet | `packet/toserver/NBTControlPacket` ("control"/"vent" keys via `IControlReceiver`) | `network/ZirnoxControlPacket.java` (ACTION_CONTROL / ACTION_VENT) |
| Cross-cutting | `ContaminationUtil`, `BossSpawnHandler`, `ExplosionNukeGeneric.waste`, `SatelliteRayScan`, `RBMKDials`, `HazardRegistry`, `MobConfig`, achievements, loot, lang | respective `hbm_m` classes verified side-by-side |

## 1. Multiblock structure

**Intact structure — [DEVIATION] geometry differs.**
- Original: core box `{1,0,2,2,2,2}` = 5×5, y 0..1 relative to core (`MultiblockHandlerXR.checkSpace` decodes dim = {yUp, yDown, zNeg, zPos, xNeg, xPos}), plus three chimney fills applied at core+dir·offset(2): `{4,-2,1,1,1,1}` (3×3 column y −2..4), `{4,-2,0,0,2,-2}` and `{4,-2,0,0,-2,2}` (two 1×1 columns y −2..4) — `ReactorZirnox.java:57-99`. Total 7-tall including 2 below-floor cells; offset 2.
- Port: layer-scripted 5×5×5 — y0 = 5×5 all O (C center), y1/y2 = 5×5 with F connectors on the E/W edge of the middle row, y3/y4 = plus-shape (3×3 + middle row) with F at y3 — `MachineZirnoxBlock.java:54-105`. Height 5, no below-floor cells.
- Visual model is OBJ-driven (`zirnox.obj`, part `Plane`) in both, so appearance is not affected; only placement volume/collision differ. Port is self-consistent (structure = controller-centered, renderer centered on controller), matching the original TESR anchor math (`RenderZirnox` translate x+0.5/z+0.5 → port `T(0.5)·R(facing)·T(−0.5)` with the JSON root `R(90)` baked in).

**Meltdown shell rebuild — MATCH.** Original meltdown sets controller → `zirnox_destroyed` and `MultiblockHandlerXR.fillSpace` with core dims `{1,0,2,2,2,2}` (5×5×2, controller-centered, `TileEntityReactorZirnox.java:411-413`). Port: `setBlock(..., 3)` → `onPlace` → `placeStructure` of the destroyed definition, which is exactly two 5×5 layers (y0 C-centered, y1 full 5×5) — `MachineZirnoxBlockEntity.java:364-370`, `MachineZirnoxDestroyedBlock.java:49-78,115-135`. Note the original meltdown deliberately does **not** rebuild the chimney fills — the port's destroyed definition likewise contains no chimney cells. Match.

**Destroyed-block `updateTick` — MATCH.** Original `ZirnoxDestroyed.updateTick` per placed cell: gas_meltdown above air at 1/10 (rolled twice, quirk preserved), foam extinguish at 1/25, tickRate 100+rand(20), `onBlockAdded` 1/4 flame+sound per cell. Port replicates all four, looping `allCells` — `MachineZirnoxDestroyedBlock.java:115-176` (comment "audit10: 1:1"). The port ticks only the controller but applies the logic per-cell; net behavior equal.

## 2. Core mechanics

All constants verified equal:

| Constant | Original | Port | Ref |
|---|---|---|---|
| `maxHeat` / `maxPressure` | 100000 / 100000 | `MAX_HEAT`/`MAX_PRESSURE` = 100_000 | TE:61,64 ↔ BE:60-61 |
| Tanks | steam SUPERHOTSTEAM 8000, CO2 CARBONDIOXIDE 16000, water WATER 32000 | `STEAM_MAX 8_000`, `CO2_MAX 16_000`, `WATER_MAX 32_000`, same fluid types (vanilla water) | TE:94-96 ↔ BE:63-65,106-108 |
| Slots | 28 (24 rods + CO2 in/out 24/26, water in/out 25/27) | `INVENTORY_SIZE 28`, `SLOT_CO2_IN/OUT 24/26`, `SLOT_WATER_IN/OUT 25/27` | TE:75,224-225 ↔ BE:53-58,345-346 |
| Pressure formula | `(co2Fill*2) + (int)(heat*co2Fill/maxFill)` | `computePressure()` = `co2*2L + (long)(heat*(co2/CO2_MAX double))`, clamp ≥0 | TE:240 ↔ BE:315-320 (double instead of float — equal result) |
| Heat loss w/ steam gen | `heat -= (int)(heat*pressure/1_000_000F)` | identical cast chain | TE:246 ↔ BE:149 |
| Heat loss w/o coolant | `heat -= 10` | identical | TE:248 ↔ BE:151 |
| Steam cycle | `heat > 10256`: `((heat-10256)/maxHeat) * min(co2/14000,1) * 25 * 7.5F` ("was 5 originally" comment present in **both**) | identical incl. the comment | TE:295-298 ↔ BE:324-327 |
| Meltdown trigger | `pressure > maxPressure || heat > maxHeat` | identical | TE:359 ↔ BE:352 |
| Rod decay | pulses = fuel-rod neighbours (mask table 0..23) + 1 if not breeding; per pulse `heat += type.heat`, `life++`; swap via fuelMap when `life > maxLife` | identical (`NEIGHBOR_MASK_*` L67-90 byte-equal to `getNeighbouringSlots` L161-191; `decay` L447-463) | TE:340-356 ↔ BE:447-493 |
| Meteorite sword | `meteorite_sword_bred` → `meteorite_sword_irradiated` | identical | TE:233-234 ↔ BE:137-138 |
| Redstone | `redstonePowered` forces `isOn=true`; drop of pulse sets `isOn=false` | identical (`setRedstonePowered` both) | TE:98-103,219-221 ↔ BE:131,306-311 |
| Satellite ray scan | every 100 ticks when `0 < heat < maxHeat`, `INFO_NUCLEAR`, lifetime 200 | identical (`RayScanEvents.INFO_NUCLEAR = "NEUTRON_EMISSION"` matches original constant) | TE:251-252 ↔ BE:154-156 |
| Slot IO rules | accessible 0..23; insert = rod only; extract = non-rod only | identical (`SidedItemAccess` rules) | TE:111-123 ↔ BE:793-798 (forge branch) |

**Fuel rod types — MATCH.** `EnumZirnoxType` (ItemZirnoxRod.java:101-129) vs port `RodSpec` table (BE:465-479):

| Type | maxLife | heat | breeding | product | Port |
|---|---|---|---|---|---|
| NATURAL_URANIUM_FUEL | 250_000 | 30 | no | depleted | equal |
| URANIUM_FUEL | 200_000 | 50 | no | depleted | equal |
| TH232 | 20_000 | 0 | **yes** | THORIUM_FUEL | equal |
| THORIUM_FUEL | 200_000 | 40 | no | depleted | equal |
| MOX_FUEL | 165_000 | 75 | no | depleted | equal |
| PLUTONIUM_FUEL | 175_000 | 65 | no | depleted | equal |
| U233_FUEL | 150_000 | 100 | no | depleted | equal |
| U235_FUEL | 165_000 | 85 | no | depleted | equal |
| LES_FUEL | 150_000 | 150 | no | depleted | equal |
| LITHIUM | 20_000 | 0 | **yes** | TRITIUM | equal |
| ZFB_MOX | 50_000 | 35 | no | depleted | equal |

Item NBT key `life` identical; lifetime 1..maxLife bar and tooltip (`trait.rbmk.depletion`, `desc.item.zirnoxRod`/`zirnoxBreedingRod`, `$` splitting) replicated — `ZirnoxRodItem.java:21,46-57,84-99`; lang keys present in generated `en_us.json`. Rod stack size 1 both. Container item `rod_zirnox_empty` preserved (`craftRemainder`/`ContainerItem`) — original ModItems.java:2971-2976 ↔ port ModItems.java:1229-1285.

**Hazards — MATCH.** `HazardRegistry` per-type radiation `wst(15)·mult` and bases u=0.35/uf=0.5/th232=0.1/thf=1.75/mox=2.5/puf=4.25/u233=5.0/u235=1.0/saf=5.85 (`HazardRegistry.java:86-128,232-253` ↔ port `HazardRegistry.java:378-399,555+`); LES depleted additionally BLINDING 20; tritium 0.001.

**Graphite moderation / control rods:** the original has no separate graphite block or control-rod item for Zirnox — moderation is purely the neighbour-pulse model and "breeding" rods above; port matches (no invented control rods).

## 3. Redstone-over-Radio

Core values/functions — **MATCH**, keys and math identical (`provideRORValue`/`runRORFunction`, prefix constants `VAL:`/`FUN:`/`!` identical):
- `VAL:heat` = round(heat·1e-5·780+20), `VAL:pressure` = round(pressure·1e-5·30), `VAL:water/steam/co2` raw fills, `VAL:state` 0/1 — TE:648-653 ↔ BE:839-844.
- `FUN:setstate` (rejected while redstonePowered), `FUN:ventco2` (−1000 clamp 0) — TE:686-700 ↔ BE:850-864.
- `getFunctionInfo` advertising lists match for the shared entries — TE:629-643 ↔ BE:825-835.

**[ABSENT] Slot introspection:** original additionally exposes `VAL:slottype`, `VAL:slotdep`, `VAL:slotmaxdep` and `FUN:setslot` backed by a persisted `rorSlot` selector (TE:73,637-640,655-671,678-684). The port has no `rorSlot` and omits all four entries (BE:824-866). Functional impact limited to RoR-driven automation reading per-slot depletion.

**[ABSENT, justified] OpenComputers component** (`zirnox_reactor`, getTemp/getPressure/getWater/getSteam/getCarbonDioxide/ventCarbonDioxide/getFuel/isActive/setActive/getInfo, TE:490-606) and **Energy Control info provider** (TE:620-626): no OC/EC mod exists on 1.20.1/1.21.1 in this project's scope; port has no equivalent. Justified gap.

## 4. GUI / menu — MATCH

- Texture `textures/gui/reactors/gui_zirnox.png` and 203×256 size identical (GUIReactorZirnox.java:23,30-31 ↔ GUIMachineZirnox.java:19-28).
- Gauges: 3 smooth linear (steam 162, CO2 144, water 180 @ y114; ratio, params 2,5,0.75,14,0, color 0x7F0000) + 2 needles (heat 169, pressure 187 @ y42; 5,2,1, 0x7F0000) — GUIReactorZirnox.java:96-101 ↔ GUIMachineZirnox.java:47-52.
- `isOn` indicator lights (4×4, 3×3 checkerboards + top lamp) identical — GUIReactorZirnox.java:103-111 ↔ GUIMachineZirnox.java:56-64.
- Buttons: control 14×14 @(144,35), vent 36×36 @(151,51), both playing `hbm:block.rbmk_az5_cover` at pitch 0.5 — GUIReactorZirnox.java:67-79 ↔ GUIMachineZirnox.java:122-140.
- Tooltips/panels: temperature/pressure hover text identical formulas; coolant/pressure info panels; water-empty warning (≤0); CO2 warning — original panel `<= 4000` but tooltip `< 4000` inconsistency **preserved** with explicit comment (GUIReactorZirnox.java:55,119 ↔ GUIMachineZirnox.java:100-113). Lang keys `desc.gui.zirnox.*` present.
- Menu: 24 rod slots at original coordinates, CO2 in/out (143,124)/(143,142), water in/out (179,124)/(179,142) with TakeOnly outs, player inv at y174, hotbar y232 — ContainerReactorZirnox.java:23-62 ↔ MachineZirnoxMenu.java:46-85.
- Shift-click routing: CO2 containers → CO2-in (menu idx 24), water → water-in (menu idx 26), rods → 0..23 reverse — semantics equal (the original's merge ranges 24-26/26-28 hit the TakeOnly outs and are rejected there, so the effective targets are the same; port documents the menu/handler index divergence, MachineZirnoxMenu.java:115-161).
- Fluid container recognition via `FluidContainerRegistry.getFluidContent` + water-bucket special case — both sides.
- GUI placement validation: original 1.7.10 `Slot.isItemValid` is hardcoded `true` (accepts anything anywhere); port uses plain `Slot` over a `Container` adapter with default `canPlaceItem=true` — same open behavior. Hopper-facing rules (rod-only insert, non-rod extract, slots 0-23) match via the sided handler.

## 5. Meltdown choreography

Sequence comparison (original `TileEntityReactorZirnox.meltdown` L405-431 ↔ port `MachineZirnoxBlockEntity.meltdown` L355-391):

| Step | Original | Port | Verdict |
|---|---|---|---|
| Clear 28 slots | yes | yes | MATCH |
| Block swap at controller, flag 3 | `zirnox_destroyed`, same meta | `ZIRNOX_DESTROYED` default state, properties copied | MATCH ([GLUE]: FACING property) |
| Rebuild destroyed shell (5×5×2) | explicit `fillSpace` | via `onPlace`→`placeStructure` | MATCH |
| Sound `hbm:block.rbmk_explosion` vol 10 pitch 1 at y+2 | yes | yes (`HbmSoundsNT.get`, registered) | MATCH |
| Explosion 12.0 at y+3 | `createExplosion(null, ..., 12.0F, true)` — **flaming** | `level.explode(..., 12.0F, ExplosionInteraction.BLOCK)` — **non-flaming** | [DEVIATION] D2 |
| Debris: 2 EXCHANGER, 20 CONCRETE, 20 BLANK, 10 ELEMENT, 10 GRAPHITE, 10 SHRAPNEL; motion gauss·0.75 / 0.01+rand·1.25, CONCRETE ×0.25 + extra Y, EXCHANGER +0.5/×0.1/+0.5 | yes | byte-equal | MATCH |
| `ExplosionNukeGeneric.waste(world, x, y, z, 35)` | yes | yes, `waste` 1:1 (loop bounds, jitter `r22/5`) | MATCH |
| Advancement to players within 100 | `achZIRNOXBoom` (icon `debris_element`, special, parent centrifuge) | `grantNearby(..., 100, ZIRNOX_BOOM)`; `zirnox_boom.json` = challenge icon `debris_element`, parent `centrifuge` | MATCH |
| `radMark` for elementals (gated `MobConfig.enableElementals`) | persisted NBT `radMark` | `BossSpawnHandler.markForRadBeasts` (`RAD_MARK` key, same 100-radius loop) | MATCH ([GLUE] key rename) |

**Post-meltdown wreck TE** (`TileEntityZirnoxDestroyed` ↔ `MachineZirnoxDestroyedBlockEntity`):
- onFire default true, 1/5000/tick extinguish, flame every 50 ticks at x+0.25..0.75 / y+1.75, sound fire.fire with same pitch formula — MATCH.
- Flames: original custom `rbmkflame` AuxParticle maxAge 90 (also used by onBlockAdded, which the port replicates 1:1 via `IParticleCreator`); port uses vanilla FLAME×12 + SMOKE×6 particles — [GLUE] visual approximation (HBM AuxParticle pipeline absent), original parameters (75-range packet, maxAge 90) mirrored where the port's particle helper supports them.
- Radiation: base 500,000 onFire / 75,000 cooled, range 100, block-resistance ray march (`res<1→1`, `/len²`), `ContaminationUtil.contaminate(RADIATION, CREATIVE, ...)` — formulas **1:1** (port `ContaminationUtil` is a faithful port incl. creative/spectator/200-tick guards). **But original runs every tick; port runs `probeDue(10)` = every 10 ticks with no ×10 compensation → dose rate 1/10 of original** — [DEVIATION] D1.
- Fire damage: original `DamageSource.onFire, 2` every tick within 5 blocks while burning; port `ModDamageSources.radiation(level), 2F` every 10 ticks + `setRemainingFireTicks(40)` — rate 1/10, source changed, ignition added — [DEVIATION] D1 / [EXTRA] E1.
- **[ABSENT] A1: wreck drops.** Original `ZirnoxDestroyed.getDrops` (L107-116): `concrete_smooth`×6, `pipe` steel×4, `steel_grate`×2, `debris_metal`×6, `debris_graphite`×2, `fallout`×4 — mining the wreck was a salvage mechanic. Port: generated loot table `data/hbm_m/loot_tables/blocks/zirnox_destroyed.json` is an empty pool (chance 0.0, no entries), no handling in `MachineDrops`/`BlockDropUtil`/`ModBlockLootTableProvider`; the structure's `universal_machine_part` cells also drop nothing.
- Original destroyed-TE NBT bug (writes `"onFire"`, reads `"fire"` → `onFire` resets to true on every chunk load) is fixed in the port (symmetric `"onFire"`) — [DEVIATION] D6, an intentional improvement; behavior delta: extinguished wrecks stay out in the port.

## 6. Tilt mechanic — MATCH

`checkTilt(TiltType.CONFIG, true)` called in both update paths (TE:214 ↔ BE:121). Port `BaseHbmBlockEntity.checkTilt` (L231-275) is a faithful port: 3×3 floor (`getFloorCount()==9`, `standardFloor5x5`), 95% validity threshold, staggered per-second checks via position identity, `metalImpact` sound on first tilt, extra-heavy rules (solid+opaque+not sand/cloth/ground + stone-explosion-resistance floor). Config gating equivalent: original `checkTilt` reads `enableMachineGravity || enable528MachineGravity` but `GeneralConfig.load()` zeroes `enable528MachineGravity` when `enable528` is off (GeneralConfig.java:175-181) — the port's `enable528 && enable528MachineGravity` reproduces exactly that. Tilted state blocks fluid I/O in both (original `getProviderSpeed/getReceiverSpeed → 0`, TE:261-262; port gates `tryProvide/trySubscribe` and the capability handler on `!tilted`, BE:166,641,661,676). Render tilt (translate −0.5 y, rotZ 10, rotY 5 before facing) 1:1 — RenderZirnox.java:18-21 ↔ MachineZirnoxRenderer.java:36-40.

## 7. NBT keys, sync, control packets

- **NBT:** scalar keys `heat`, `pressure`, `isOn`, `redstonePowered` identical. Tanks renamed/nested: original flat `steam`/`carbondioxide`/`water` → port `SteamTank{steam}`/`Co2Tank{co2}`/`WaterTank{water}` + legacy migration for its own older long format — [COSMETIC]/[GLUE] (1.7.10 saves are not migratable anyway). Port extras: `output`, `AllowedFluidSides`, `FluidSidesFromMbStructure` — [EXTRA] E3, justified (side-config persistence fixes restart reopening closed faces).
- **Sync:** original `networkPackNT(150)` every tick; port `sendUpdateToClient()` on state change only — [GLUE].
- **Control packet:** original `NBTControlPacket` with `control`/`vent` keys handled via `receiveControl`, guarded by `hasPermission` (20-block distance, TE:451-470); port `ZirnoxControlPacket` (pos + action int) → `blockEntityAt` resolves the BE with **no distance check** (ModPacketHandler.java:569-581) — [DEVIATION] D5, minor: a modified client could toggle/vent the reactor from arbitrary distance; original capped it at 20 blocks.
- Vent semantics identical (−1000 mB CO2, clamp 0); vent recomputes pressure immediately in the port (original next tick) — cosmetic.
- GUI open flow: sneak-less right click → `markFBI` → open GUI at core; port identical (`BossSpawnHandler.markFBI`, extended menu) — MachineZirnoxBlock.java:209-219 ↔ ReactorZirnox.java:38-54.
- Redstone scan: 5×5×5 surface scan (dy 0..4, boundary cells only) → `setRedstonePowered` — ReactorZirnox.java:106-142 ↔ MachineZirnoxBlock.java:156-186, [GLUE] signal-API mapping.

## 8. Config gating

| Flag | Original | Port | Verdict |
|---|---|---|---|
| `MobConfig.enableElementals` (meltdown elementals) | config bool, default true | `MobConfig.enableElementals()` | MATCH |
| `GeneralConfig.enableMachineGravity` / 528 gravity | defaults false/true, zeroed without `enable528` | Cloth fields, port checkTilt gates 528 variant by `enable528` | MATCH (equivalent) |
| `RBMKDials.getPermaScrap` (debris despawn) | game rule `KEY_PERMANENT_SCRAP` | `RBMKDials.getPermaScrap` → `RBMKGameRules.PERMANENT_SCRAP` | MATCH |
| `ModClothConfig` tips/gravity fields | n/a | registered in schema | [GLUE] |

## Deviation list (evidence)

- **D1 [DEVIATION] Wreck radiation/fire tick rate ÷10, fire source changed.** Original `TileEntityZirnoxDestroyed.updateEntity` (L40-55: `radiate` every tick; L91-93: `DamageSource.onFire, 2` per tick) ↔ port `MachineZirnoxDestroyedBlockEntity.tick` (L56-58: `probeDue(10)`; L117-120: `ModDamageSources.radiation(level), 2F` + `setRemainingFireTicks(40)`). Radiation math 1:1 but applied 1/10 as often → 10× lower dose rate and 10× lower burn DPS. Performance-motivated, not compensated.
- **D2 [DEVIATION] Meltdown explosion not flaming.** TE:415 `createExplosion(null, x, y+3, z, 12.0F, true)` (isFlaming=true) ↔ BE:373-374 `level.explode(null, ..., 12.0F, Level.ExplosionInteraction.BLOCK)` (no fire). Fires after meltdown missing.
- **D3 [DEVIATION] Fluid connection surface generalized.** Original: 5 discrete port cells (`getAllPorts`: {±2,1,0}/{±2,3,0} side faces + {0,4,0} top, ReactorZirnox.java:75-83; TE `getPorts` TE:195-208; `TileEntityProxyCombo` port cells). Port: fluid allowed on N/S/W/E+UP of controller and every `F` connector part (`fluidSideMap` = `fluid(true,true,true,true,true,false)`, MachineZirnoxBlock.java:90-94); the verbatim `getAllPorts` port (L266-274) is used only for the placement highlight. Net: more connectable faces than original; tank roles/limits unchanged.
- **D4 [DEVIATION] Intact structure geometry.** 5×5×2 core + 3×3 chimney (y−2..4) + 2 columns (original, 7 tall incl. 2 below-floor) vs 5-layer 5×5→plus-shape y0..4 (port, 5 tall). Placement volume and collision differ; renderer/model unaffected.
- **D5 [DEVIATION] No 20-block permission gate on control packets.** TE:451-454 `hasPermission` ↔ ModPacketHandler.java:569-581 (chunk-residency only).
- **D6 [DEVIATION, improvement] Original NBT asymmetry bug fixed.** Original reads `"fire"` / writes `"onFire"` (TileEntityZirnoxDestroyed.java:30,36) → always reignites on load; port symmetric `"onFire"` (MachineZirnoxDestroyedBlockEntity.java:126-135).
- **A1 [ABSENT] Wreck salvage drops** (concrete_smooth×6, pipe steel×4, steel_grate×2, debris_metal×6, debris_graphite×2, fallout×4) — empty generated loot table, no code path. Debris-entity pickup loot is unaffected and matches.
- **A2 [ABSENT] ROR slot introspection** (`VAL:slottype/slotdep/slotmaxdep`, `FUN:setslot`, `rorSlot`) — omitted.
- **A3/A4 [ABSENT, justified] OpenComputers component and Energy Control info provider** — integration mods unavailable on modern targets.
- **E1 [EXTRA] `setRemainingFireTicks(40)`** on entities near a burning wreck (port-only ignition).
- **E2 [EXTRA] Meltdown state resets:** port drains all three tanks, `isOn=false`, `pressure=0`, `heat=max(heat, MAX_HEAT)` (BE:357-362). Harmless — the destroyed BE ignores heat; original simply let the TE die with the tanks.
- **E3 [EXTRA] Extra NBT keys** (`output`, `AllowedFluidSides`, `FluidSidesFromMbStructure`) + legacy long-format migration (BE:261-302).
- **[GLUE] items:** metadata rod (11 metas) → 11 items; container items via `craftRemainder`; `ISidedInventory`→`SidedItemAccess`; `PortDef`/FIFO fluid ports → adjacent-block `tryProvide/trySubscribe` with side config; per-tick forced sync → on-change sync; signal APIs; facing metadata → FACING property; tilt ground-material checks mapped to modern state checks (`isFaceSturdy`/`isRedstoneConductor`/tags, BaseHbmBlockEntity.java:252-275); 1.7.10 `getExplosionResistance(null)` → modern no-arg call (loses the entity-arg variant, no behavior impact here).
- **[COSMETIC] Debris renderer:** block-state stand-ins + per-type scales 0.9..1.2 instead of the original per-type OBJs; tumble = interpolated Y-rot + X·0.5 instead of `entityId%360` around (1,1,1) (graphite path keeps `entityId%360`, RenderZirnoxDebris.java:27-29 ↔ ZirnoxDebrisRenderer.java:53-56,63-64); debris spawn rotation starts at 0 instead of random 0..360 (EntityDebrisBase.entityInit ↔ ZirnoxDebrisEntity — no randomized init). Model/texture set (zirnox.obj/zirnox_destroyed.obj, deb OBJs, gui_zirnox.png) shipped and referenced.

## Coverage statement

Covered line-by-line on both trees: both TE/BE classes, both block classes, GUI, container/menu, rod item, debris entity (+ base class), control packet, all three renderers, meltdown/debris/vent/ROR/redstone/tilt/radiation/hazard/advancement/loot/lang/sound/config cross-checks, plus registration points (ModBlockEntities, ModMenuTypes, ClientSetup screen+renderer lines, ModEntities, creative tab batches, model JSONs, sounds.json).

Not covered (out of audit scope, honest gaps): OBJ mesh/UV geometry fidelity and texture PNG content (part names and transform conventions only), recipe content for rods/PUREX/assembler (separate matrix rows; spot-checked existence only), JEI/NEI category internals beyond the fuelMap mention, and no runtime/gameplay testing.
