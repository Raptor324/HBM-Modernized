# RBMK Core Neutron Simulation — Fidelity Audit (1.7.10 vs port)

- Date: 2026-11-22
- Auditor: read-only fidelity sweep, RBMK core neutron simulation & shared RBMK infrastructure
- Original: `C:\Projects\Hbm-s-Nuclear-Tech-GIT\src\main\java\com\hbm\` (1.7.10, branch tip 096929e40)
- Port: `C:\Projects\HBM-Modernized-agents\src\main\java\com\hbm_m\` (working tree, dirty — HEAD ignored)
- Verdict classes: `[GLUE]` framework necessity, `[COSMETIC]` naming/strings, `[DEVIATION]` semantic difference, `[ABSENT]` missing feature, `[EXTRA]` port-only addition.

## Scope

| Original | Port |
|---|---|
| `handler/neutron/RBMKNeutronHandler.java` | `handler/rbmk/RBMKNeutronHandler.java` |
| `handler/neutron/NeutronHandler.java`, `NeutronNodeWorld.java`, `NeutronNode.java`, `NeutronStream.java` | `handler/rbmk/NeutronNodeWorld.java` (+ `handler/neutron/*` kept for CP-1 pile) |
| `tileentity/machine/rbmk/RBMKDials.java` | `handler/rbmk/RBMKDials.java`, `handler/rbmk/RBMKGameRules.java` |
| `tileentity/machine/rbmk/TileEntityRBMKBase.java` | `blockentity/machines/rbmk/RBMKColumnBlockEntity.java` (shared column core) |
| `tileentity/machine/rbmk/TileEntityRBMKActiveBase.java`, `TileEntityRBMKSlottedBase.java` | absorbed into `RBMKColumnBlockEntity` + per-BE `MenuProvider`/slot fields |
| `tileentity/machine/rbmk/IRBMKFluxReceiver.java`, `IRBMKLoadable.java` | same names under `blockentity/machines/rbmk/` |
| `hazard/modifier/HazardModifierRBMKHot.java`, `HazardModifierRBMKRadiation.java` | same names under `hazard/modifier/` |
| `particle/ParticleRBMKFlame/Mush/Steam.java` | `particle/custom/RBMKFlameParticle.java`, `RBMKMushParticle.java`, `RBMKSteamParticle.java` |
| (context) `TileEntityRBMKRod.java`, `TileEntityRBMKRodReaSim.java`, `TileEntityRBMKOutgasser.java`, `TileEntityRBMKControl.java`, `blocks/machine/rbmk/RBMKBase.java`, `RBMKRod.java`, `items/machine/ItemRBMKLid.java` | `RBMKRodBlockEntity.java`, `RBMKOutgasserBlockEntity.java`, `RBMKControlBlockEntity.java`, `block/machines/rbmk/RBMKColumnBlock.java`, `RBMKRodBlock.java`, `item/rbmk/RBMKLidItem.java` |

Out of scope (audited only where the shared core touches them): per-column-type internals of boiler/heater/cooler/storage/console.

---

## 1. Stream propagation

**Verdict: MATCH** (queue mechanics, lifetime, distances, flux application, moderation, radiation leak all 1:1; phase-of-tick differs, [GLUE]).

| Aspect | Original | Port | Verdict |
|---|---|---|---|
| Stream creation | `TileEntityRBMKRod.spreadFlux` (rod:246) creates 4 streams N/E/S/W, `flux`, `ratio` | `RBMKRodBlockEntity.spreadFlux` (rod:162-163), `FLUX_DIRS` identical order | MATCH |
| ReaSim fan | `TileEntityRBMKRodReaSim.spreadFlux` (ReaSim:44-49): `Vec3NT(1,0,0)` rotated `rand.nextInt(4)*9°`, 8 streams at 45° steps, `flux*0.75` | `RBMKRodBlockEntity.spreadFlux` (rod:151-159): `angle = nextInt(4)*9`, `cos/sin`, 8 × 45°, ×0.75 | MATCH (mirror handedness of the rotation is statistically irrelevant given per-tick jitter) |
| Zero-flux eviction | `if(flux == 0) removeNode` (rod:227-231, ReaSim:30-34) | `isReaSim() ? flux == 0 : flux <= 0` (rod:146) | MATCH (port adds `<= 0` for legacy path — guard only) |
| Queue lifecycle | 4-arg `RBMKNeutronStream` ctor self-registers into `StreamWorld.streams`; `NeutronHandler.onServerTick` runs interactions then `removeAllStreams()` | `sw.addStream(...)`; `NeutronNodeWorld.tick` runs then `streams.clear()` (NeutronNodeWorld:81-82) | MATCH |
| Tick cadence | `NeutronHandler.onServerTick` on FML bus, `ServerTickEvent.Phase.START` (NeutronHandler.java:17-19) — streams queued by tick N are consumed at START of tick N+1 | `MainRegistry` SERVER_POST hook (MainRegistry.java:144-156) — streams queued by tick N consumed at POST of tick N, i.e. before tick N+1's BE ticks | [GLUE] — the spread→burn loop still spans exactly one world tick on both sides (rod receives in tick N, burns in tick N+1); the known deliberate 1-tick latency is preserved |
| Dial caching | per-world statics refreshed each tick in the world loop (NeutronHandler:34-42) | per-level statics refreshed in `NeutronNodeWorld.tick` (NeutronNodeWorld:72-79) | MATCH |
| `cleanNodes` sweep | every 20 server ticks (`cacheTime = 20`), per world (NeutronHandler:22-24,47) | `CACHE_TIME = 20`, `advanceTick()` once per server tick, per level (NeutronNodeWorld:23-24,59-64,84) | MATCH |
| Step math | `x = floor(0.5 + v.x*i)`, i = 1..fluxRange (NeutronStream:67-68, RBMKNeutronHandler:220-221) | identical (`RBMKNeutronHandler.java:152-153,210-211`) | MATCH |
| `columnHeight` +1 | `RBMKNeutronHandler.columnHeight = RBMKDials.getColumnHeight(world) + 1` (NeutronHandler:41, "IT'S NOT THE TOTAL HEIGHT" comment) | same, `NeutronNodeWorld.java:78` with explanatory comment | MATCH |
| Moderation | `type == MODERATOR || nodeTE.isModerated()` → `moderatedCount++`, `fluxRatio *= 1 - moderatorEfficiency` per pass (RBMKNeutronHandler:307-309,424-426) | identical (`RBMKNeutronHandler.java:245-248,347-349`) | MATCH |
| Reflector | moderated echo re-applied, `fluxQuantity *= reflectorEfficiency` unless 1.0, else return flux to origin rod (341-356) | identical incl. echo loop (`RBMKNeutronHandler.java:260-267`; adds `instanceof` guard where CE casts) | MATCH |
| Absorber | `heat += getAbsorberHeatConversion * fluxQuantity`; `fluxQuantity *= absorberEfficiency` unless 1 (357-364) | identical (`RBMKNeutronHandler.java:268-272`) | MATCH |
| Control rod | `level > 0` → `fluxQuantity *= getMult()`, else stream dies (332-340); tail step re-attenuation + cache/irradiate decision checks **origin pos** — CE's own `posAfter` bug kept (379-397) | identical incl. the `sw.getNode(pos)` origin-pos quirk (`RBMKNeutronHandler.java:256-259,286-300`) | MATCH (bug-for-bug) |
| Open-column leak | `!hasLid` → `incrementRad(fluxQuantity * 0.05F)` (304-305) | identical (`RBMKNeutronHandler.java:241-243`) | MATCH |
| Blocking | non-RBMK TE: `getHits(pos)` over `columnHeight`; full block → return; partial → `irradiate(pos, hits)` + `flux *= 1 - hits/columnHeight` (275-293) | identical (`RBMKNeutronHandler.java:215-232`) | MATCH |
| `getHits` opacity | `block.isOpaqueCube()`; `RBMKBase` overrides to `true` (RBMKBase.java:58) | `isNeutronOpaque`: explicit `RBMKColumnBlock`/`RBMKColumnFillerBlock` → true, else `isSolidRender` (`RBMKNeutronHandler.java:326-331`) | [GLUE] — modern columns carry `noOcclusion()`, explicit check restores CE's override; documented in-code |
| Tail irradiation | `irradiateFromFlux`: `flux * 0.05F * (1 - hits/columnHeight)` (416-422) | identical (`RBMKNeutronHandler.java:333-345`); adds `!level.isLoaded(pos)` no-op guard | [EXTRA] (guard only fires on unloaded chunks where the port's radiation backend drops the dose anyway) |
| Node cache eviction (`checkNode`) | dead legacy rod → evict 4 stream paths; dead ReaSim rod → evict disc; non-rod without live rod in disc/cardinal paths → evict self (102-197) | same three branches, same conditions (`RBMKNeutronHandler.java:93-140`) | MATCH |
| ReaSim disc | square walk with `x²+z² <= range²` reject, radius = `fluxRange` (70-100) | same filter, radius = `fluxRange` (`RBMKNeutronHandler.java:77-86`) | MATCH |
| `removeEmptyWorlds` | drops world when `streams.isEmpty()` (NeutronNodeWorld:39-43) | drops when `streams.isEmpty() && nodeCache.isEmpty()` (NeutronNodeWorld:50-52) | [DEVIATION minor] — port also reclaims worlds holding only stale nodes; behavior converges at the next `cleanNodes` anyway |
| Stale node access | cache returns node even if TE gone (relies on `invalidate()` + sweep) | `StreamWorld.getNode` evicts `tile.isRemoved()` on access (NeutronNodeWorld:121-128) | [EXTRA] — strictly fresher than CE, same steady-state |
| CP-1/pile | `PileNeutronHandler` + DUMMY/PILE types in same world map | port keeps a separate `handler/neutron` package + world map for the pile; RBMK path is RBMK-only | [GLUE] |

---

## 2. RBMKDials

**Verdict: MATCH** for values; storage location changed for doubles [GLUE]; two minor clamps/quirks.

All 24 original keys (`RBMKDials.RBMKKeys`, RBMKDials.java:17-42) accounted for:

| Dial key | Original default | Port home | Port default | Verdict |
|---|---|---|---|---|
| dialSaveDials | true (gamerule) | — | — | [ABSENT benign] — original used it only to decide whether `createDials` force-resets dials; port registers real vanilla gamerules with defaults, no reset path needed |
| dialPassiveCooling | 2.5 | `ModClothConfig.RBMKDialSettings.passiveCooling` | 2.5 | MATCH |
| dialPassiveCoolingInner | 0.1 | config `passiveCoolingInner` | 0.1 | MATCH value; **port clamps [0,1], original clamps min 0 only** (RBMKDials.java:92 vs RBMKDials.java:59) → [DEVIATION minor] |
| dialColumnHeatFlow | 0.2 [0,1] | config `columnHeatFlow` [0,1] | 0.2 | MATCH |
| dialDiffusionMod | 1.0 ≥0 | config `fuelDiffusionMod` ≥0 | 1.0 | MATCH |
| dialHeatProvision | 0.2 [0,1] | config `heatProvision` [0,1] | 0.2 | MATCH |
| dialColumnHeight | 4, clamped [2,16] then **−1** at read (RBMKDials.java:96) | gamerule `dialColumnHeight` 4, same clamp−1 (`RBMKDials.java:64-66`) | 4 | MATCH |
| dialEnablePermaScrap | true | gamerule `dialEnablePermaScrap` true | true | MATCH |
| dialBoilerHeatConsumption | 0.1 ≥0 | config `boilerHeatConsumption` | 0.1 | MATCH |
| dialControlSpeed | 1.0 ≥0 | config `controlSpeedMod` | 1.0 | MATCH |
| dialReactivityMod | 1.0 ≥0 | config `reactivityMod` | 1.0 | MATCH |
| dialOutgasserSpeedMod | 1.0 ≥0 | config `outgasserMod` | 1.0 | MATCH |
| dialControlSurgeMod | 1.0 ≥0 | config `surgeMod` | 1.0 | MATCH value; CE's `getSurgeMod` returns `getPassiveCooling` **on the client** (a CE bug, RBMKDials.java:262-264) — port returns the real value → [COSMETIC] (client diag only) |
| dialFluxRange | 5 [1,100] | gamerule `dialFluxRange` 5, clamp [1,100] | 5 | MATCH |
| dialReasimRange | 10 [1,100] | gamerule `dialReasimRange` 10 | 10 | MATCH (note: CE's `getReaSimNodes` uses `fluxRange`, not this dial — port replicates that, so both dials behave identically) |
| dialReasimBoilers | false ‖ `GeneralConfig.enable528ReasimBoilers` (gamerule OR config, RBMKDials.java:105; config flag zeroed when `!enable528`, GeneralConfig.java:175-177) | gamerule `dialReasimBoilers` false ‖ `GeneralConfig.reasimBoilers528()` = `enable528 && enable528ReasimBoilers` (`RBMKDials.java:76`, `GeneralConfig.java:75-77`) | false | MATCH (effective condition identical) |
| dialReasimBoilerSpeed | 0.05 [0,1] | config `reasimBoilerSpeed` [0,1] | 0.05 | MATCH |
| dialDisableMeltdowns | false | gamerule `dialDisableMeltdowns` false | false | MATCH |
| dialEnableMeltdownOverpressure | false | gamerule `dialEnableMeltdownOverpressure` false | false | MATCH |
| dialModeratorEfficiency | 1.0 [0,1] | config `moderatorEfficiency` [0,1] | 1.0 | MATCH |
| dialAbsorberEfficiency | 1.0 [0,1] | config `absorberEfficiency` [0,1] | 1.0 | MATCH |
| dialReflectorEfficiency | 1.0 [0,1] | config `reflectorEfficiency` [0,1] | 1.0 | MATCH |
| dialDisableDepletion | false (inverted at read) | gamerule `dialDisableDepletion` false, inverted | false | MATCH |
| dialDisableXenon | false (inverted) | gamerule `dialDisableXenon` false, inverted | false | MATCH |
| dialAbsorberHeatConversion | 0.05 [0,1] | config `absorberHeatConversion` [0,1] | 0.05 | MATCH |

Notes:
- Vanilla gamerules can't hold doubles, so the 15 float dials moved to `ModClothConfig.RBMKDialSettings` (schema-registered SERVER/LIVE, `ConfigSchema.java:306-320`). Values and clamps preserved; per-world `/gamerule` tuning for floats is replaced by server config. [GLUE]
- CE cached gamerule values per world with a `refresh()` map; port reads live (floats = config field read). [GLUE]

---

## 3. TileEntityRBMKBase → RBMKColumnBlockEntity (shared column core)

**Verdict: MINOR** — core math 1:1; several small deviations in the meltdown presentation layer.

| Aspect | Original | Port | Verdict |
|---|---|---|---|
| Fields | `heat`, `reasimWater/Steam`, `maxWater/maxSteam = 16000`, `craneIndicator` (Base:55-62) | same (`RBMKColumnBlockEntity.java:39-44`) | MATCH |
| `maxHeat()` | 1500 (Base:82-84) | 1500 (`RBMKColumnBlockEntity.java:201`) | MATCH |
| `passiveCooling(n)` | `min + (max-min) * ((4 - clamp(n,0,4))/4D)`, min/max from dials (Base:91-95) | identical (`RBMKColumnBlockEntity.java:194-198`) | MATCH |
| `coolPassively` | `heat -= passiveCooling; if(<20) =20` (Base:256-259) | identical (194-198,180) | MATCH |
| Base tick order | `craneIndicator--` → `moveHeat()` → (reasim) `boilWater()` → `networkPackNT` (Base:108-123) | `baseTick`: same order, `sendBlockUpdated` instead of packet (85-93) | MATCH, sync transport [GLUE] |
| `boilWater` | `heat<100` return; `available = min((heat-100)/hc, water, maxSteam-steam)`; `processed = floor(available * clamp(speed,0,1))` (Base:128-145) | identical (183-192) | MATCH |
| Neighbor cache | 4 cardinal slots, invalidated on `isInvalid()`, filled via `Compat.getTileStandard` (Base:154,170-185) | 4 slots, invalidated on `isRemoved()` (120-137) | MATCH |
| Heat equalization | `target = heatTot/members`; each member `heat += (target-heat)*stepSize`; reasim water/steam set to floor averages; **whole remainder added to self** (Base:199-228) | same target/step; remainder handed to neighbors first, leftover to self (151-178) | [DEVIATION minor] — both conserve totals; the ±(members−1) mB placement differs |
| Reasim gating | water/steam totals only summed when `getReasimBoilers` (Base:161,192-195,216-226) | identical (123,144,155-167) | MATCH |
| `markDirty` scope | self only (Base:228) | self + each neighbor `setChanged()` (169,177) | [EXTRA minor] (more chunk saves, no behavior change) |
| `getRBMKType` defaults | OTHER (Base:261-263) | OTHER (510) | MATCH |
| `isModerated` | false default (Base:593-595) | false default (511); rod reads block variant (`RBMKRodBlockEntity.moderated`) | MATCH [GLUE] |
| Type mapping | ROD/MODERATOR/CONTROL_ROD/REFLECTOR/ABSORBER/OUTGASSER explicit; blank/boiler/cooler/heater/storage OTHER | identical mapping (grep: all 19 port BEs listed in §code) | MATCH |
| Node eviction on removal | `invalidate()`/`onChunkUnload()` → `NeutronNodeWorld.removeNode` (Base:237-246) | `setRemoved()` → removeNode (77-83) | MATCH [GLUE] |
| Meltdown trigger | rod: `heat > maxHeat()` in update → `meltdown()` or, if `dialDisableMeltdowns`, `ParticleUtil.spawnGasFlame` at column top + flux reset (rod:158-169) | identical incl. reset (rod BE:82-97); flame uses `RBMK_FLAME` particle instead of gas flame | [COSMETIC] |
| Flood fill | recursive `getFF` over 4 neighbors, static `columns` set, **no cap, no loaded-chunk guard** (Base:575-591) | BFS with `visited` set, `isLoaded` guard, **50 000-column cap** (244-271) | [EXTRA] — differs only for world-edited mega-structures; port comment wrongly attributes the cap to CE |
| `onMelt(minDist+1)` | bounds min/max over columns; `minDist = min(dists to 4 edges)`; `onMelt(minDist+1)` (Base:447-475) | identical (273-279) | MATCH |
| `standardMelt` | clamp `reduce` [1,h]; 1/3 chance `reduce++`; layers `i <= h+1-reduce` → pribris (top boundary layer burning when `reduce>1`), above → air (Base:393-416) | identical incl. burning boundary (407-430); port adds explicit "no per-column explosion" note | MATCH |
| Rod corium fill | **entire column `h..0` → `corium_block`, ignores `reduce`** (rod:346-349) | respects the reduce cutoff: corium only below `h+1-reduce`, air above (`RBMKRodBlockEntity.java:190-194`) | **[DEVIATION]** — crater profile over corium channels differs from CE |
| Fuel debris count | `1 + rand.nextInt(getColumnHeight)` FUEL debris (rod:351-355) | `1 + nextInt(h)` (195-196) | MATCH |
| Graphite debris | moderated → `2 + rand.nextInt(2)` (rod:360-367) | identical (201-204) | MATCH |
| Lid debris | concrete lid metadata → `DebrisType.LID` (rod:371-372, Base:389-391) | `lidState == 1` → "lid" (206, `RBMKColumnBlockEntity.java:404`) | MATCH (glass lid state 2 correctly excluded) |
| Digamma | `rbmk_fuel_drx` → static flag; after melt, corium-adjacent pribris (1/3 chance) → `pribris_digamma` else `pribris_radiating`; flag reset; `EntitySpear` at y+100 (rod:339, Base:479-498,560-569) | `RBMKColumnBlockEntity.digamma` static; same 3×3×3 1/3 pass with RBMK_DEBRIS variants; flag + spear in `try/finally` (209,284-305,334-346) | MATCH (try/finally = [EXTRA] hardening) |
| Overpressure | `dialOverpressure && !pipes.isEmpty()`; pipes: destroy `min(pipeBlocks/5, 100)` TE positions; receivers: `IOverpressurable.explode` or air + 5-power explosion; nets collected from boiler `onMelt` (Base:500-539, boiler:212) | identical caps and split (`handleOverpressure`, 360-400); nets registered in boiler melt (`RBMKBoilerBlockEntity.java:364`) | MATCH |
| Mushroom + sound | `"rbmkmush"` NBT particle at `avgX+0.5, y+1, avgZ+0.5`, scale `smallDim = min(maxX-minX, maxZ-minZ)`; sound `hbm:block.rbmk_explosion` vol 50 pitch 1; achievement within 50 of **triggering column** (Base:541-558) | `sendParticles(RBMK_MUSH)` scale `max(dims)*2`; sound `RBMK_EXPLOSION` 50/1; achievement `grantNearby(avg, 50)` (313-329) | **[DEVIATION]** — cloud scale formula (`min` → `max*2`) and achievement center (trigger column → group center); particle/sound positions match |
| Lid handling | `hasLid()`: `!isLidRemovable()` → true, else metadata ≠ DIR_NO_LID (Base:65-71); placement forces `DIR_NO_LID` (`RBMKBase.getDirModified`, RBMKBase.java:174-176); lid item sets metadata, screwdriver removes (drops item, node `addLid/removeLid`) | `hasLid()` identical short-circuit (488); `lidState` 0/1/2 default 0 (58); `setLidState` updates cached node (500-508); lid item/screwdriver (`RBMKLidItem.java:36`, `RBMKColumnBlock.java:94-103`) | MATCH [GLUE] (metadata → BE field) |
| Render bbox | fixed `y+17` (Base:608-610) | `COLUMN_HEIGHT+1` dial-driven (522-530) | [COSMETIC] |
| `explodeOnBroken` | **static** flag, transiently false around metadata writes; breakBlock meltdown = flag && fuel hull ≥1500 && !dialDisableMeltdowns (`RBMKRod.breakBlock`, RBMKRod.java:41-52); also `invalidate()` melts on hull ≥1500 (rod:108-114) | per-BE boolean, default true, persisted; `onColumnRemoved` meltdown = flag && fueled && hull ≥ maxHeat && !dialDisabled (`RBMKRodBlock.java:98-111`); nothing ever writes false → effectively always true, like CE's static | MATCH (effectively); port [EXTRA] pops fuel as item on non-melting break (CE rod slot is destroyed/lost on break without loader) |

---

## 4. ActiveBase / SlottedBase roles

**Verdict: MATCH with [GLUE]**; GUI reach changed.

- `TileEntityRBMKActiveBase` (getName, `isUseableByPlayer` ≤ 128 sq ≈ 11.3 blocks): port BEs are `MenuProvider`s; reach via `MenuReach.stillValid` → `MobHooks.canInteractWithBlock(player, pos, 4.0)` = vanilla 8-block reach (`inventory/menu/MenuReach.java:42-45`). **[DEVIATION minor]**: openable distance ~8 blocks vs ~11.3. `coldEnoughForManual`/`coldEnoughForAutoloader` gates (1000/200) ported 1:1 (`RBMKRodBlockEntity.java:212-223` vs rod:94-105), plus a menu-side `clicked()` guard against touching a hot rod `[GLUE/EXTRA]`.
- `TileEntityRBMKSlottedBase` (ISidedInventory, slots array, NBT `items` list + `name`): port uses plain `ItemStack[]` per BE (e.g. `RBMKStorageBlockEntity.slots[12]`, compacted toward slot 0 like CE) and single-slot `fuelSlot` for rods, persisted as `slots`/`fuelSlot` tags. `[GLUE]` (no sided-inventory concept in 1.20.1; hoppers interact via the menus/cranes as in CE).
- Control-room devices (panels, crane console, autoloader) are **not** `TileEntityRBMKBase` in CE; the port hosts them on `RBMKColumnBlockEntity` but opts them out of the heat network via `participatesInHeatNetwork() = false` (`RBMKPanelBlockEntity.java:33`, `RBMKPanelDeviceBlockEntity.java:109`, `RBMKCraneConsoleBlockEntity.java:267`, `RBMKAutoloaderBlockEntity.java:174`). `[GLUE]` — restores CE's class hierarchy split.

## 5. NBT persistence

**Verdict: MATCH** (keys and semantics), with `[GLUE]` additions.

| Key | Original | Port | Verdict |
|---|---|---|---|
| `heat`, `reasimWater`, `reasimSteam` | Base read/write (Base:268-289) | identical keys (`RBMKColumnBlockEntity.java:564-597`) | MATCH |
| `lidState` | — (lid lives in block metadata `DIR_*_LID`) | port-only int 0/1/2 | [GLUE] |
| `fluxQuantity` (= lastFluxQuantity), `fluxMod` (= lastFluxRatio), `hasRod` | rod:269-280 | identical incl. last-flux semantics (rod BE:286-317) | MATCH |
| `fluxSlow`/`fluxFast` diag + legacy read branch (`hasKey("fluxFast")` recalc, rod:251-266) | rod:272-278 writes only in diag mode | port writes in diag mode; **read branch for legacy `fluxFast`/`fluxSlow` saves not ported** | [ABSENT benign] (pre-port saves only) |
| `items` slot list + `name` (SlottedBase:135-173) | — | `fuelSlot` / `slots` list | [GLUE] |
| `explodeOnBroken` | static, not persisted | persisted per BE | [GLUE] |
| `diag` guard | skips super/slots while dumping | identical pattern (`RBMKColumnBlockEntity.java:570,574-588`) | MATCH |

## 6. Hazard modifiers

**Verdict: MATCH** (rod paths exact).

- `HazardModifierRBMKHot`: `level = 0; if rod: min(ceil((hullHeat-100)/10), 60)` — identical (`HazardModifierRBMKHot.java` both sides). Registered for RBMK rod items in both `HazardRegistry`s (orig:588, port:954).
- `HazardModifierRBMKRadiation` rod branch: `depletion = linear ? 1-enrich : 1-enrich²; level += (target-level)*depletion; level += HazardRegistry.xe135 * poison` — identical (orig HazardModifierRBMKRadiation.java vs port same name). `xe135` value carried (port `HazardRegistry.java:38`).
- Pellet branch (`ItemRBMKPellet`, item-damage-driven depletion + flat xenon): **[ABSENT]** — port pellets are static crafting ingredients with no damage-encoded state; omission documented in the port file header. No rod-sim impact.

## 7. Particles

**Verdict: MATCH** constants; presentation glue + 2 cosmetic extras.

| Particle | Original | Port | Verdict |
|---|---|---|---|
| Flame | `ParticleRBMKFlame`: maxAge from packet (`maxAge=300` from debris, Flame.java:23-28); scale `rand+1`; alpha fade-in 20, fade-out 20, ×0.5; strip 14 cols, `age*5 % 14`, sample `% 5`; brightness 240; yaw-only billboard | `RBMKFlameParticle`: lifetime 300 (Provider fallback; spawners send xd=0 → 300); quadSize `rand+1`; same fades; `frameColumn` = `(age*5%14)%5` (43-84); FULL_BRIGHT; yaw-only (96-116) | MATCH; [EXTRA] port adds slight upward drift + wander (CE particle is static); [GLUE] atlas UV addressing |
| Spawn sites | `RBMKDebrisBurning.updateTick` 1/5 per 100-120 ticks + `onBlockAdded` 1/3, pos `x+0.25+rand*0.5, y+1.75, z+…`, range 75; `RBMKDebrisRadiating` similar; boiler vent | `RBMKDebrisBurningBlock.spawnFlame` same pos/chance (118-127, tick delay fixed to 100-120 per comment); `RBMKDebrisRadiatingBlock.java:136` | MATCH |
| Steam | `ParticleRBMKSteam`: maxAge 10, alpha 0.25, scale 4, 20-col strip `(age/maxAge*20)%20-1`; spawn at `x+0.25+rand(2)*0.5, y+columnHeight`, ventDelay `20+rand(10)`, sound `steamEngineOperate` 2F pitch `1+rand*0.25` (boiler:107-120) | `RBMKSteamParticle`: lifetime 10, alpha 0.25, quadSize 4, 20 cols (identical quad offsets `scale*-0.25-0.9375` etc., 84-85); `RBMKBoilerBlockEntity.vent` same pos/cooldown/sound (128-141); first-frame −1 clamped for atlas | MATCH; clamp [COSMETIC] |
| Mush | `ParticleRBMKMush`: maxAge 50, 30-frame flipbook, scale = packet `scale`; spawned by meltdown (`scale = smallDim`), WATZ, missiles, arty | `RBMKMushParticle`: lifetime 50, 30 segments, scale via xd slot (120-133); spawned by meltdown + WATZ port (`MachineWatzPowerplantBlockEntity.java:266`) | MATCH; scale value formula deviation → see §3 |
| Rod overheat (meltdowns off) | `ParticleUtil.spawnGasFlame` (generic gas flame) at `y + columnHeight + 0.5` | `sendParticles(RBMK_FLAME, …, 0, 0.0, 0.2, 0.0, 1)` same position/motion | [COSMETIC] (different particle texture, same visual intent) |

## 8. GameRules

**Verdict: MATCH** for names/defaults; one key absent.

- Port registers 9 vanilla gamerules with **verbatim CE key names and defaults** (`RBMKGameRules.java:33-58`): `dialReasimBoilers`=false, `dialEnablePermaScrap`=true, `dialDisableMeltdowns`=false, `dialEnableMeltdownOverpressure`=false, `dialDisableDepletion`=false, `dialDisableXenon`=false, `dialColumnHeight`=4, `dialFluxRange`=5, `dialReasimRange`=10. Registration runs in `MainRegistry.init` (MainRegistry.java:75); CE registers via `RBMKDials.createDials` at `FMLServerStartingEvent` (MainRegistry.java:672). [GLUE]
- `dialSaveDials` not registered [ABSENT benign] — see §2.
- Float dials not gamerules (see §2) [GLUE].

## 9. Config gating

**Verdict: MATCH.**

- Original RBMK-relevant `GeneralConfig` flags: only the 528 group touches RBMK (`enable528ReasimBoilers`, zeroed when `!enable528`, GeneralConfig.java:175-177). Port carries `enable528`, `enable528ReasimBoilers = true` and composes them identically (`GeneralConfig.java:75-77`). No other rbmk/reasim flags exist on either side.
- Port `ConfigSchema.java:304-320` registers all 15 double dials as SERVER/LIVE fields with use-site-matching bounds; `ModClothConfig.RBMKDialSettings` defaults match CE exactly (verified field-by-field, `ModClothConfig.java:35-66`).

---

## Deviation list (consolidated)

| # | Class | Finding | Original evidence | Port evidence | Impact |
|---|---|---|---|---|---|
| D1 | [DEVIATION] | Meltdown mushroom scale: CE `smallDim = min(width, depth)`; port `max(width, depth) * 2` | `TileEntityRBMKBase.java:541` | `RBMKColumnBlockEntity.java:313` | Cloud visibly larger than CE for rectangular reactors |
| D2 | [DEVIATION] | Rod corium fill respects `reduce` crater cutoff; CE fills the full column height with corium regardless | `TileEntityRBMKRod.java:346-349` | `RBMKRodBlockEntity.java:184-194` | Melted fuel channels leave partial corium columns instead of CE's solid slab |
| D3 | [DEVIATION minor] | ReaSim water/steam equalization remainder: CE gives whole remainder to self; port distributes to neighbors first | `TileEntityRBMKBase.java:222-226` | `RBMKColumnBlockEntity.java:160-176` | Conserved totals; ±few mB placement per tick |
| D4 | [DEVIATION minor] | `dialPassiveCoolingInner` clamped [0,1] in port; CE clamps min 0 only | `RBMKDials.java:92` | `RBMKDials.java:59` | Only differs for user values > 1 |
| D5 | [DEVIATION minor] | Meltdown achievement/sound/particle centered on group center; CE scans achievement players around the triggering column | `TileEntityRBMKBase.java:553-554` | `RBMKColumnBlockEntity.java:314-329` | Cosmetic-adjacent; sound/particle position matches CE |
| D6 | [DEVIATION minor] | GUI reach 8 blocks (vanilla) vs CE 128 sq (~11.3) | `TileEntityRBMKActiveBase.java:14-20` | `MenuReach.java:42-45` | Menus close slightly sooner |
| D7 | [DEVIATION minor] | `removeEmptyWorlds` also requires empty node cache | `NeutronNodeWorld.java:39-43` | `NeutronNodeWorld.java:50-52` | Idle stream worlds reclaimed one sweep earlier |
| D8 | [DEVIATION low] | Control-room panels/gauges hosted on `RBMKColumnBlockEntity` become OTHER-type neutron nodes (streams pass through); in CE those blocks are not `TileEntityRBMKBase`, so a stream crossing them is shadowed by `getHits` | `RBMKBase.java` class hierarchy | `RBMKPanelBlockEntity.java:14`, `RBMKNeutronHandler.java:216-236` | Only observable if a panel is placed inside a stream path — unusual |
| D9 | [DEVIATION low] | Port ReaSim fan handedness (`cos/sin`) mirrors CE's `Vec3NT.rotateAroundYDeg` | `TileEntityRBMKRodReaSim.java:44-49` | `RBMKRodBlockEntity.java:151-159` | Statistically identical (random per-tick jitter) |
| A1 | [ABSENT] | `dialSaveDials` gamerule (force-reset of dials on world start) | `RBMKDials.java:18,66-69` | no equivalent | Benign: port's gamerules persist natively |
| A2 | [ABSENT] | Legacy `fluxFast`/`fluxSlow` save recalculation branch | `TileEntityRBMKRod.java:254-260` | `RBMKRodBlockEntity.java:303-317` | Pre-port saves only |
| A3 | [ABSENT] | Hazard radiation pellet branch (item-damage-driven) | `HazardModifierRBMKRadiation.java` pellet arm | port omits (documented) | Pellets are static ingredients in port |
| E1 | [EXTRA] | Flood-fill cap (50 000) + `isLoaded` guard in meltdown; try/finally for `dropLids`/`digamma` | CE `getFF` uncapped | `RBMKColumnBlockEntity.java:252-271,343-346` | Server protection; port comment wrongly attributes cap to CE |
| E2 | [EXTRA] | Unloaded-chunk guard in tail irradiation; stale-node eviction on access; neighbor `setChanged()` | — | `RBMKNeutronHandler.java:336-340`, `NeutronNodeWorld.java:121-128`, `RBMKColumnBlockEntity.java:169` | Perf/persistence hygiene |
| E3 | [EXTRA] | Flame particle drift/wander; rod-break fuel `popResource`; menu hot-rod click guard | CE particle static; CE fuel destroyed on break | `RBMKFlameParticle.java:57-70`, `RBMKRodBlock.java:107`, `RBMKRodMenu.clicked` | Cosmetic/QoL |
| G | [GLUE] | Double dials in config not gamerules; lid state in BE NBT not metadata; `participatesInHeatNetwork` gate; sync via `sendBlockUpdated` vs `networkPackNT`; RBMK-column opacity explicit check; SERVER_POST vs ServerTick.START phase; separate pile handler package | — | — | Accepted framework necessity |

## Constants quick-reference (all verified equal)

heat cap 10 000 (rod), maxHeat 1 500, passive floor 20, maxWater/maxSteam 16 000, boiler onset 100, boiler heat-per-MB dial 0.1, ReaSim boiler speed 0.05, column heat flow 0.2, passive cooling 2.5/0.1, flux range 5, reasim range 10, column height dial 4 (→3 offset), absorber heat conversion 0.05, open-column leak 0.05×flux, irradiation 0.05×flux×(1−hits/height), outgasser efficiency `min(1 − fluxRatio*0.8, 1)`, outgasser duration 10 000, ReaSim fan ×0.75 at 45° ×8 with `nextInt(4)*9°` jitter, debris flame maxAge 300 chance 1/5 (cycle 100-120) and 1/3 on place, steam vent delay 20+rand(10) sound 2F/1+rand·0.25, mush lifetime 50/30 frames, steam particle 10 ticks/alpha 0.25/scale 4, flame fade 20+20 ×0.5, meltdown sound vol 50 pitch 1, achievement radius 50, overpressure `min(pipes/5, 100)` and 5F explosion, corium infection 1/3 in 3×3×3, melt depth +1/3 chance, cold gates 1 000 (autoloader) / 200 (manual), fire hazard `ceil((hull−100)/10)` cap 60.
