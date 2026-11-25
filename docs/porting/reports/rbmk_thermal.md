# RBMK Thermal Machines + Autoloader — Fidelity Audit (rbmk_boiler, rbmk_cooler, rbmk_heater, rbmk_steam_inlet, rbmk_steam_outlet, rbmk_autoloader)

- Date: 2026-11-22
- Original authority: `C:\Projects\Hbm-s-Nuclear-Tech-GIT\src\main\java\com\hbm\` (upstream master tip `096929e40`; note: this tree is the *current* upstream with `fluidmk2` + `ModulePortManFluidAdaptive`, not the classic 1.7.10 fluid release — all comparisons below are against what is actually on disk there).
- Port: `C:\Projects\HBM-Modernized-agents\src\main\java\com\hbm_m\`
- Method: side-by-side read of every constant, tick flow, GUI geometry, NBT key, meltdown path and config gate. Working tree only, git HEAD not trusted.
- Classification: `[GLUE]` framework necessity (Architectury/BE/Modern-Fluid API), `[COSMETIC]` naming/strings only, `[DEVIATION]` semantic difference, `[ABSENT]` original feature missing, `[EXTRA]` port-only feature.

---

## 1. rbmk_boiler (TileEntityRBMKBoiler ↔ RBMKBoilerBlockEntity)

**Scope**: `blocks/machine/rbmk/RBMKBoiler.java`, `tileentity/machine/rbmk/TileEntityRBMKBoiler.java`, `inventory/gui/GUIRBMKBoiler.java`, `inventory/container/ContainerRBMKGeneric.java` ↔ `block/machines/rbmk/RBMKBoilerBlock.java`, `blockentity/machines/rbmk/RBMKBoilerBlockEntity.java`, `inventory/gui/GUIRBMKBoiler.java`, `inventory/menu/RBMKBoilerMenu.java`, `network/RBMKBoilerPacket.java`.

### Heat → steam conversion — MATCH

| Constant | Original (`TileEntityRBMKBoiler.java`) | Port (`RBMKBoilerBlockEntity.java`) | Verdict |
|---|---|---|---|
| Feed tank | `FluidTank(Fluids.WATER, 10000)` L55 | `waterTank = FluidTank(WATER, 10_000)` L47 | MATCH |
| Steam tank | `FluidTank(Fluids.STEAM, 1000000)` L56 | `steamTank = FluidTank(STEAM, 1_000_000)` L48 | MATCH |
| Heat threshold / mB | STEAM 100, HOTSTEAM 300, SUPERHOTSTEAM 450, ULTRAHOTSTEAM 600 (L127–133) | `HEAT_THRESHOLD = {100, 300, 450, 600}` L42, indexed by grade | MATCH |
| Steam factor | 1 / 10 / 100 / 1000 (L135–141) | `STEAM_FACTOR = {1, 10, 100, 1000}` L43 | MATCH |
| Heat per mB | `RBMKDials.getBoilerHeatConsumption` (gamerule, default 0.1) L82 | `RBMKDials.getBoilerHeatConsumption(level)` L88 (default 0.1, `RBMKDials.java:26,68`) | MATCH |
| Non-ultra branch | `waterUsed = floor(heatProvided / HPM)`, min feed; `steamProduced = floor(waterUsed*100/factor)` L96–99 | identical L100–101 | MATCH |
| Ultra-hot branch | `steamProduced = floor((heatProvided/HPM)*100/factor)`; `waterUsed = floor(steamProduced/100*factor)`; re-derive when feed short L87–94 | identical L92–98 | MATCH |
| Heat subtraction | `heat -= waterUsed * HEAT_PER_MB_WATER` L120 | `heat -= waterUsed * heatPerMb` L116 | MATCH |
| `consumption`/`output` | tracked per tick L74–75,101–102 | `lastConsumption`/`lastOutput` L80–81,105–106 | MATCH |

### Vent (overpressure blow-off) — MATCH

Original L107–118: steam clamped to max; if `ventDelay <= 0` → AuxParticle `type=rbmksteam` at `(x+0.25+rand(0..1)*0.5, y+columnHeight, z+...)`, TargetPoint r=100, `ventDelay = 20+rand(10)`, sound `hbm:block.steamEngineOperate` vol 2F pitch `1+rand*0.25`.
Port `vent()` L128–143: identical formula, `ModParticleTypes.RBMK_STEAM` via `sendParticles`, `ModSounds.STEAM_ENGINE_OPERATE` vol 2F, same pitch, same 20–30 tick cooldown. `[GLUE]` AuxParticle→vanilla particle + threaded packet→`sendParticles`; `[COSMETIC]` sound registry name.

### Compression button (`cyceCompressor`) — 1 DEVIATION

Original L186–197: guard `if(this.heat > 50 && this.feed.getFill() > 0) return;`, then STEAM→HOTSTEAM /10, HOTSTEAM→SUPERHOTSTEAM /10, SUPERHOTSTEAM→ULTRAHOTSTEAM /10, ULTRAHOTSTEAM→STEAM `min(fill*1000, max)`; `markDirty()`.
Port `cycleCompressor()` L332–345: identical type ladder and /10 ×1000 math (`[COSMETIC]` grade derived from tank type instead of separate field — cannot drift), **but the `heat > 50 && feed > 0` refusal guard is missing** — the port compresses regardless of column heat/feed state.
- Evidence: original `TileEntityRBMKBoiler.java:188`; port `RBMKBoilerBlockEntity.java:332` (no guard).
- Transport: original `NBTControlPacket("compression")` from GUI click region `33..53 × 21..85` (`GUIRBMKBoiler.java:45–52`) + `receiveControl` L179–184; port `GUIRBMKBoiler.mouseClicked` L114–125 → `RBMKBoilerPacket` (`ACTION_CYCLE_COMPRESSOR`) with the original's 20-block `hasPermission` (`lengthVector() < 20` = `distanceToSqr > 400` — `RBMKBoilerPacket.java:46`). MATCH incl. permission range.

### Flush channels / fluid connectivity — 1 DEVIATION

Original: `ModulePortManFluidAdaptive.setInputTanks(feed).setOutputTanks(steam)` + `ports.update(getPortsNoLoader/CloseLoader/ClassicLoader())` (TE L58, L71–72). The module **combines every PortDef into one unified shape and subscribes *both* tanks at *all* port positions** (`ModulePortManFluidAdaptive.update`, portPositions = `PortDef.combine(ports)`): shapes are top-of-column `(x, y+h+1, UP)` + bottom `(x, y-1, NEG_Y)`; classic loader adds all 6 faces around `y-2`; close loader replaces the bottom with all 6 faces around the loader at `y-1` (`TileEntityRBMKBase.java:612–644`, `TilePort.PortDef.make` offsets the dir).
Port `exchangeFluids` L179–187 + `getOutputPos` L151–170: water subscribed **only** at `pos.below()` (DOWN); steam provided **only** at top `pos.above(h+1)` (UP) + the 5 non-up faces around a loader (if `rbmk_loader` at `below`/`below(2)`).
- Net effect: in the original, water can enter via the roof port and steam can leave via the bottom port; in the port they cannot. The common layout (water pipe into the floor, steam drawn from the roof) still works. Loader-side routing matches.
- Evidence: original `ModulePortManFluidAdaptive.java:80–100` (unified positions for in *and* out) + `TileEntityRBMKBase.java:613–644`; port `RBMKBoilerBlockEntity.java:179–187`.
- `[DEVIATION]` connectivity subset (applies identically to cooler and heater, see below).

### Meltdown — MATCH (see §7 for the shared-path findings)

`onMelt` L200–218: 1–2 `DebrisType.BLANK` debris; when the overpressure dial is on, every steam network the out-ports feed is added to the meltdown's pipe set. Port L353–370: same debris count, `overpressureNets` from `getOutputPos` targets, then `super.onMelt` (standard melt + lid debris when lidState==1, matching original's `DIR_NORMAL_LID`-only lid debris — glass lid spawns none in both).

### GUI / container — MATCH

176×186; water gauge `+126/+82-i` UV `176,58-i,14,i`; steam mini-column `+91/65-j` UV `190,24-j,4,j` with `j>0 → j++`, `j>22 → j++`; grade icons UV `194/208/222/236, 0, 14, 58`. Port `GUIRBMKBoiler.renderBg` L61–79 copies all of it. Menu: player inv `104+row*18` / hotbar `162` = original `84+i*18+20` / `142+20`; `quickMoveStack` returns EMPTY like original `transferStackInSlot` (returns null). `[COSMETIC]` tooltips are hand-written literals instead of `renderTankInfo`.

### NBT / sync / console — MATCH (cosmetic deltas)

NBT: `feed`→`water` (deliberate, port save-compat, commented L403), `steam` same key; base `heat`/`reasimWater`/`reasimSteam` identical (`RBMKColumnBlockEntity.writeNbtData` L565–571 vs original `TileEntityRBMKBase` L281–289); port adds `ventDelay` `[EXTRA]` (trivial). Console: type BOILER both; console NBT `water/maxWater/steam/maxSteam` identical, `type` (fluid ID) → `steamGrade` 0–3 `[COSMETIC]` (consumed by the port's own console renderer). ROR values `feed/steam/consumption` 1:1 (L339–345 vs L418–433). Client sync via update tag instead of `serialize(ByteBuf)` `[GLUE]`.

**Verdict: MINOR** — conversion/vent/GUI 1:1; compression guard missing; fluid connectivity is a subset.

---

## 2. rbmk_cooler (TileEntityRBMKCooler ↔ RBMKCoolerBlockEntity)

**Scope**: `blocks/machine/rbmk/RBMKCooler.java`, `tileentity/machine/rbmk/TileEntityRBMKCooler.java` ↔ `block/machines/rbmk/RBMKCoolerBlock.java`, `blockentity/machines/rbmk/RBMKCoolerBlockEntity.java`.

### Cooling cycle — MATCH

| Constant | Original | Port | Verdict |
|---|---|---|---|
| Cold tank | `FluidTank(PERFLUOROMETHYL_COLD, 4_000)` L35 | `coldTank = FluidTank(PERFLUOROMETHYL_COLD, 4_000)` L31 | MATCH |
| Warm tank | `FluidTank(PERFLUOROMETHYL, 4_000)` L36 | `hotTank = FluidTank(PERFLUOROMETHYL, 4_000)` L32 | MATCH |
| Transfer gate | cold ≥ 50 && hot space ≥ 50, then −50/+50 L67–69 | identical L64–68 | MATCH |
| Heat removal | every cached neighbor `heat -= 200`, floor 20 L71–76 | `HEAT_REDUCTION = 200`, floor 20 L41,74–75 | MATCH |
| Neighbor cache | 5×5 at own Y, rebuilt every 60 ticks, `x = x-2 + i/5`, `z = z-2 + i%5` L49–65 | same interval, same index math (`dx = -2 + i/5`, `dz = -2 + i%5`) L52–62 | MATCH |

Port additionally skips `isRemoved()` entries in the cooling loop — the original would write heat into a stale TE object; harmless `[GLUE]`.

### Fluid connectivity — 1 DEVIATION (same class as boiler)

Original: input PERFLUOROMETHYL_COLD + output PERFLUOROMETHYL both bound to the unified port shape (all positions). Port L96–125: cold in **only** from below, warm out top + loader sides. `[DEVIATION]` subset.

### Meltdown / console / NBT

No `onMelt` override in either (standard melt + concrete-lid debris). NBT keys `t0`/`t1` identical (L87–96 vs L186–196). Console type COOLER both. Port adds console readout `cooled/cryo/maxCryo/hot/maxHot` (`getNBTForConsole` L170–178) — the original cooler returns `null` (shows bare heat) → `[EXTRA]` benign readout.

**Verdict: MATCH** (with the shared connectivity-subset note) — core cycle constants and cadence are 1:1; one `[EXTRA]` console readout.

---

## 3. rbmk_heater (TileEntityRBMKHeater ↔ RBMKHeaterBlockEntity)

**Scope**: `blocks/machine/rbmk/RBMKHeater.java`, `tileentity/machine/rbmk/TileEntityRBMKHeater.java`, `inventory/gui/GUIRBMKHeater.java`, `inventory/container/ContainerRBMKHeater.java` ↔ port counterparts.

Note on the task phrasing "energy→heat": in the current upstream the heater is a **column-heat → fluid** exchanger (COOLANT → COOLANT_HOT via `FT_Heatable`), not an energy consumer; the port implements exactly that.

### Conversion — MATCH

| Constant | Original | Port | Verdict |
|---|---|---|---|
| Tanks | `COOLANT 16_000` / `COOLANT_HOT 16_000` L41–42 | identical L43–44 | MATCH |
| Trait data | `COOLANT.addTraits(FT_Heatable … setEff(HEATEXCHANGER, 1.0) … addStep(300, 1, COOLANT_HOT, 1))`, `COOLANT_HOT.setTemp(600)` (`Fluids.java:337,652`) | `ModFluidTraitsBootstrap.java:410–417` identical; temp 600 (`ModFluidTraitsBootstrap.java:126`) | MATCH |
| Product type | `steam.setTankType(step.typeProduced)` L65 | `outputTank.setTankType(step.typeProduced)` L92 | MATCH |
| Temp range | `heat - steam.temperature` L66 | `heat - FluidType.getTemperatureCelsius(...)` L94 | MATCH |
| TU_PER_DEGREE | `2_000D * eff` L70 | `2_000D * eff` L98 | MATCH |
| ops | `min(feed/req, space/prod, floor(tempRange*TUPD/heatReq))` L71–74 | identical, plus `heatReq > 0` guard L100–103 (original would divide 0 → Infinity → `Integer.MAX_VALUE`) — `[GLUE]` safety, unreachable with shipped traits | MATCH |
| Heat draw | `heat -= (step.heatReq * ops / TUPD) * eff` L78 | identical L108 | MATCH |
| eff ≤ 0 / no trait | both tanks → NONE L81–89 | `clearTankTypes()` L87–89,112–116 | MATCH |
| Fluid-id slot | `if(heat <= 50 || feed empty) feed.setType(0, slots)` L60 | `applyFluidIdentifier()` every tick L63,69–75 | **[DEVIATION]** original refuses retyping while hot *and* fed; port retypes anytime. Minor (same consumed-item semantics via `FluidTank.setType`, port `FluidTank.java:234–270` ≙ original `FluidTank.java` `setType`, both empty the tank on retype). |

### GUI / container — MATCH, 1 minor DEVIATION

176×186; fluid-id slot at `(41,45)` both (`ContainerRBMKHeater.java:18` vs `RBMKHeaterMenu.java:36`); tanks rendered via `renderTank(+68/+82, 14×58)` and `(+126/+82)` + static 10×10 icons at `(72,72)/(130,72)` UV `176,0`/`186,0` — port L66–73 identical. Player inv `104/162` ≙ `84+20/162`. **No buttons in either GUI** (task asked about "GUI buttons/modes": the heater has none in the original, port matches). `[DEVIATION]` port `quickMoveStack` returns EMPTY (`RBMKHeaterMenu.java:69`) while the original shift-clicks between the TE slot and player inv (`ContainerRBMKHeater.java:32–56`).

### Meltdown / console / NBT

`onMelt` 1–2 BLANK + standard both (L126–135 vs L154–159). Console `HEATEX` → port enum `HEATER` `[COSMETIC]`; console NBT `water/maxWater/steam/maxSteam` identical; `type`/`hottype` short fluid IDs → translation keys `[COSMETIC]` (noted in code L220–224). NBT `feed/steam` → `input/output`, `items` → `inventory` `[COSMETIC]`. ROR `in/out` 1:1 (L250–254 vs L273–278). Fluid connectivity: same unified-port → split-face subset deviation as boiler/cooler. `[EXTRA]` port exposes an item handler for automation (`RBMKHeaterBlockEntity.java:187–200`); the original exposes no accessible slots (`TileEntityRBMKSlottedBase` returns `{}`).

**Verdict: MATCH** — heat-exchanger math and trait data are 1:1; deviations are the retyping gate, shift-click, connectivity subset.

---

## 4. rbmk_steam_inlet / rbmk_steam_outlet (ReaSim boiler plumbing)

**Scope**: `blocks/machine/rbmk/RBMKInlet.java`/`RBMKOutlet.java`, `tileentity/machine/rbmk/TileEntityRBMKInlet.java`/`TileEntityRBMKOutlet.java` ↔ `block/machines/rbmk/RBMKSteamInletBlock.java`/`RBMKSteamOutletBlock.java`, `blockentity/machines/rbmk/RBMKSteamInletBlockEntity.java`/`RBMKSteamOutletBlockEntity.java`.

### Inlet — MATCH

- Tank `WATER 32_000` (original L24, port L22); NBT key `tank` both.
- Transfer gated by `RBMKDials.getReasimBoilers(worldObj)` (original L38) / `RBMKDials.getReasimBoilers(level)` (port L40) — same gate, see §8.
- Pushes into the 4 cardinal neighbors' `reasimWater` up to `maxWater` 16,000 (`min(rbmk.maxWater - rbmk.reasimWater, tank)` — original L51–53, port L45–52; `MAX_WATER = 16_000` port base L42 ≙ original `maxWater` L58).
- Neighbor resolution: original `RBMKBase.findCore` (dummyable proxy); port `findColumnCore` walks down through `RBMKColumnFillerBlock` up to column height (L61–73) — same result, `[GLUE]`.
- Intake: original `TilePortShapes.around` (all 6 faces, `setupFluidPorts`/`updatePortFIFO` — outside the reasim gate); port subscribes all 6 faces every tick (L34–36). MATCH.

### Outlet — MATCH

- Tank `SUPERHOTSTEAM 32_000` (original L24, port L29–30 — port javadoc even documents the original's superhot typing).
- Drains the 4 cardinal columns' `reasimSteam` (`min(space, reasimSteam)`) under the same reasim gate (original L38–56, port L40–51).
- Emission: original `setupAllPorts`/`updatePortPIFIFO` always active; port `tryProvide` to all 6 faces whenever fill > 0 (L57–61). MATCH.
- Fluid-id difference (none — both superhot). NBT `tank` both.

### Blocks

Plain container blocks in the original (no placement gating — the gate lives in the TEs/dial); port blocks are plain column-family blocks with `addStandardInfo` tooltips (≙ original `ITooltipProvider.addInformation`), no GUI (neither has one in the original). MATCH.

### Meltdown

Neither is an RBMK column: original `getFF` collects only `TileEntityRBMKBase`; port inlet/outlet extend plain `BaseHbmBlockEntity`, never `RBMKColumnBlockEntity` — excluded from `meltdownReactor` in both. MATCH.

**Verdict (both): MATCH** — 1:1 including the ReaSim gate.

---

## 5. rbmk_autoloader (TileEntityRBMKAutoloader ↔ RBMKAutoloaderBlockEntity)

**Scope**: `blocks/machine/rbmk/RBMKAutoloader.java`, `tileentity/machine/rbmk/TileEntityRBMKAutoloader.java`, `inventory/gui/GUIRBMKAutoloader.java`, `inventory/container/ContainerRBMKAutoloader.java` ↔ port counterparts.

### Fuel-rod cycle — MATCH

| Topic | Original | Port | Verdict |
|---|---|---|---|
| Slots | 18: 0–8 input, 9–17 spent (`super(18)` L44) | `SLOTS = 18`, `INPUT_SLOTS = 9` L32–34 | MATCH |
| Cycle default / clamp | 50; ±5 per button, clamp 5–95 (L262–264) | `cycle = 50` L41; clamp 5–95 in `pasteSettings` L248 | MATCH (button path ABSENT, below) |
| Piston | `speed = 0.005`, retract/extend with 40-tick end delays, `isRetracting` init true (L37–39, 59–90) | `SPEED = 0.005D`, `END_DELAY = 40`, same state machine L52–56, 99–123 | MATCH |
| Start-cycle scan | every 20 ticks, `hasFuel() && hasSpace()`, column directly below via `RBMKBase.findCore`, `coldEnoughForAutoloader()`, rod empty or enrichment×100 < cycle (L68–82) | `level.getGameTime() % 20 == 0 && hasFuel() && hasSpace()`, `targetRod` = column below via `findColumnCore`, same cold/spent conditions L109–118 | MATCH |
| Swap at full extension | take old rod → first free slot 9–17; insert first input rod with enrichment×100 ≥ cycle (L93–128) | unload via `canUnload()/provideNext()/unload()` then insert first rich-enough rod via `load()` L128–160; rod accessors ≙ direct `slots[0]` manipulation (`RBMKRodBlockEntity.java:249–255`) | MATCH |
| `coldEnoughForAutoloader` | hull heat ≤ 1,000 (`TileEntityRBMKRod.java:94–100`) | hull heat ≤ 1_000 (`RBMKRodBlockEntity.java:212–217`) | MATCH |
| Slot validation | rods ≥ cycle, input half only (`isItemValidForSlot` L208–210) | same L78–81, wired into menu `mayPlace` (`RBMKAutoloaderMenu.java:64–66`) | MATCH |
| Menu layout | input 3×3 at `(17,18)`, take-only output at `(107,18)`, player inv `(8,100)` (`ContainerRBMKAutoloader.java:18–20`) | identical positions (`RBMKAutoloaderMenu.java:59–84`); `broadcastChanges` re-sync snapshot `[GLUE]` | MATCH |
| Settings copy | `getSettings/pasteSettings("cycle")` clamp 5–95 (L280–291) | identical L239–251 | MATCH |
| Column reach | **only the column directly below** (original `x, y-1, z`) | `targetRod` = `findColumnCore(pos.below())` | MATCH |

### ABSENT (4 findings)

1. **GUI +/- cycle buttons and % readout.** Original `GUIRBMKAutoloader.mouseClicked` L37–53 sends `minus`/`plus` NBTControlPacket; `receiveControl` L261–266 adjusts cycle; % drawn at center top L62–63. Port GUI has neither (`GUIRBMKAutoloader.java` — no mouseClicked, no percent), and no packet exists. The BE still owns `cycle` (and copies settings), so the threshold is now only reachable via the settings-paste tool. Highest-impact gap in this audit.
2. **Operating sounds.** Original loops `hbm:door.wgh_start` @0.75 vol while the piston moves and plays `hbm:door.wgh_stop` @2F at the ends (L142–155). Port client tick only sets `lastPiston` (L91–93); `ModSounds.WGH_START/WGH_STOP` exist but are used only by doors — autoloader is silent.
3. **"Tower" steam particles at full extension** (original L157–171, `type=tower` AuxParticle with lift/base/max/alphaMod parameters) — no port equivalent.
4. **Hopper/pipe automation.** Original `getAccessibleSlotsFromSide` exposes all 18 slots while `piston <= 0` and `canExtractItem` allows output half only (L212–220). Port BE exposes no item capability at all (`RBMKAutoloaderBlockEntity` has no `getItemHandler`), so external automation cannot load/unload it.

### DEVIATION (1)

**Physical footprint.** Original is a 9-block tower: `BlockDummyable` dims `{8,0,0,0,0,0}` with bounding `(±0.125, 0..4)` pillar + `(±0.5, 4..9)` head (`RBMKAutoloader.java:15–31`), render AABB `y..y+9` (TE L243–247). Port is a 1-block column-family block with a BER piston translating 4 blocks *down* (`MachineRbmkControlRenderer.java:261–269`), render box `y-5..y+2` (`RBMKAutoloaderBlockEntity.java:184–188`). Build footprint, silhouette and occlusion differ; the load/unload semantics are identical. The original's piston descent direction is not visible from the TE code alone (render-only), so this is classified as footprint/render deviation, gameplay impact low.

### GLUE / cosmetic notes

- Client piston interpolation: original `turnProgress` smoothing + `networkPackNT(100)` per tick; port update-tag sync + renderer-side interpolation (`MachineRbmkControlRenderer` `(be, pt) -> 4.0 - (lastPiston + (piston-lastPiston)*pt) * 4.0`). `[GLUE]`
- `participatesInHeatNetwork() = false` replicates the original's class hierarchy (autoloader derives from `TileEntityMachineBase`, not `RBMKBase`) — correct opt-out from column heat equalization. `[GLUE]`
- `RBMKAutoloaderMenu` javadoc (L22–29) is **stale**: it claims a 9-slot shared buffer and "no cycle field", contradicting the actual 18-slot / `cycle` code — doc-only bug.
- Console: port returns `ColumnType.STORAGE` for the console registry `[GLUE]` (original autoloader has no console type at all).

**Verdict: MINOR** — cycle logic 1:1, but the GUI threshold buttons, sounds, particles and automation are missing.

---

## 6. Shared infrastructure checks (all six)

### 5×5 / 4-neighbor caches
- Cooler 5×5 cache: identical scan geometry and 60-tick cadence (§2). MATCH.
- Column 4-neighbor cache: original caches in `moveHeat` and invalidates on `isInvalid()` (`TileEntityRBMKBase.java:170–185`); port same with `isRemoved()` (`RBMKColumnBlockEntity.java:130–146`), remainder distribution changed from "self keeps whole remainder" (original L223–226) to "neighbors first, self keeps the rest" (port L160–176) — totals identical, only which column holds the spare units differs. `[GLUE]`-grade rounding detail.
- Heat equalization: `targetHeat`, `stepSize = getColumnHeatFlow` (0.2), passive cooling `min + (max-min)*((4-clamp(n))/4)` with floor 20, `maxHeat()` 1500 — all 1:1 (original L159–234, 91–95 vs port L122–199).

### Meltdown (applies to boiler/cooler/heater and the column under the autoloader)
1:1: flood fill over 4-neighborhood, `minDist + 1` severity, `reduce` clamp 1..h with 1-in-3 bump, boundary layer `pribris_burning`, radiating/digamma infection sweep, overpressure destruction (`min(pipes/5, 100)` pipe blocks + receiver explosions 5F), mushroom cloud + `hbm:block.rbmk_explosion` @50F, achievement. Two findings:

- **[DEVIATION] Mushroom-cloud scale parameter**: original `smallDim = Math.min(maxX-minX, maxZ-minZ)` (`TileEntityRBMKBase.java:541`); port `smallDim = Math.max(maxX-minX, maxZ-minZ) * 2` (`RBMKColumnBlockEntity.java:313`). Affects only the cloud's visual scale factor.
- **[DEVIATION] Meltdown flood fill over-captures non-column devices.** Original collects only `TileEntityRBMKBase` (`getFF`, L575–591) — the autoloader, panels and crane console are *not* melted. Port collects any `RBMKColumnBlockEntity` (`meltdownReactor` L267) and `RBMKAutoloaderBlockEntity` extends that base (L24), so a meltdown adjacent to an autoloader standard-melts the autoloader block and the stack above it (its `participatesInHeatNetwork=false` flag gates only heat movement, not the flood fill). Same risk for any other device derived from `RBMKColumnBlockEntity`. Port-only destruction path.
- Port adds a 50,000-column flood-fill cap and unloaded-chunk skip `[GLUE]`/`[EXTRA]` (safety, original recurses unbounded but on loaded chunk graph).

### Config gating (dials) — MATCH

| Dial | Original (`RBMKDials.java`) | Port (`RBMKDials.java`) | Verdict |
|---|---|---|---|
| `dialBoilerHeatConsumption` | default 0.1, min 0 (L26, L98, L225) | `BOILER_HEAT_CONSUMPTION = 0.1`, `max(..., 0.0)` (L26, L68) | MATCH |
| `dialReasimBoilers` | gamerule default false `‖ GeneralConfig.enable528ReasimBoilers` (L33, L105); `enable528ReasimBoilers` defaults true but is force-false when `!enable528` (`GeneralConfig.java:53,176`) → effective `enable528 && flag` | `REASIM_BOILERS = false ‖ GeneralConfig.reasimBoilers528()` with `reasimBoilers528() = enable528 && enable528ReasimBoilers` (`RBMKDials.java:76`, `GeneralConfig.java:75–77`, defaults `enable528=false`, `enable528ReasimBoilers=true`) | MATCH |
| `dialReasimBoilerSpeed` | default 0.05, clamp 0–1 (L34, L106, L300) | `0.05`, `clamp(..., 0.0, 1.0)` (L39, L77) | MATCH |
| `dialColumnHeight` | clamp 2–16, minus 1 (L24, L96, L207) | clamp 2–16 minus 1 (L65) | MATCH |
| passive cooling / heat flow | 2.5 / 0.1 / 0.2 (clamped/min 0) | identical (L20–22, L61–63) | MATCH |
| Gamerule names | `dialReasimBoilers`, `dialEnablePermaScrap`, … | same names (`RBMKGameRules.java:37–46`) | MATCH |

### Packets / sync
- Boiler vent: original threaded `AuxParticlePacketNT`; port `ServerLevel.sendParticles` `[GLUE]`.
- Boiler compression: original `NBTControlPacket` → `receiveControl`; port dedicated `RBMKBoilerPacket` (C2S) with the same 20-block permission — semantics preserved, `[GLUE]`.
- Tank/state sync: original `serialize(ByteBuf)` (+ `networkPackNT` ranges 15/100); port update-tag from `writeNbtData` refreshed by per-tick `sendBlockUpdated` in `baseTick` (`RBMKColumnBlockEntity.java:92`) `[GLUE]` (higher network load, same data).
- Autoloader cycle buttons would have needed a C2S packet — missing together with the GUI (§5).
- OpenComputers / EnergyControl / ROR: OC + EnergyControl integration dropped `[GLUE]` (mod-compat scope, standard for this port); ROR value providers ported 1:1 on boiler and heater; cooler/autoloader had no ROR in the original either.

---

## 7. Finding index

| # | Class | Matrix id | Finding | Evidence (original → port) |
|---|---|---|---|---|
| D1 | DEVIATION | rbmk_boiler | Compression refuses only when `heat > 50 && feed > 0` in original; port compresses unconditionally | `TileEntityRBMKBoiler.java:188` → `RBMKBoilerBlockEntity.java:332` |
| D2 | DEVIATION | boiler/cooler/heater | Fluid port geometry is a subset: original binds every tank to every unified port position (top, bottom, loader faces); port restricts feed to bottom-only, output to top+loader-sides | `ModulePortManFluidAdaptive.java:80–100` + `TileEntityRBMKBase.java:613–644` → `RBMKBoilerBlockEntity.java:179–187`, `RBMKCoolerBlockEntity.java:96–125`, `RBMKHeaterBlockEntity.java:122–151` |
| D3 | DEVIATION | rbmk_boiler | Tick order: original converts heat→steam before column heat equalization; port equalizes first (`baseTick`) | `TileEntityRBMKBoiler.java:67–125` → `RBMKBoilerBlockEntity.java:76–78` + `RBMKColumnBlockEntity.java:85–93` |
| D4 | DEVIATION | rbmk_heater | Fluid-identifier retyping: original gated on `heat <= 50 ‖ feed empty`; port applies every tick | `TileEntityRBMKHeater.java:60` → `RBMKHeaterBlockEntity.java:63,69–75` |
| D5 | DEVIATION | rbmk_heater | GUI shift-click transfer missing (quickMoveStack EMPTY) | `ContainerRBMKHeater.java:32–56` → `RBMKHeaterMenu.java:69` |
| D6 | DEVIATION | all columns | Meltdown mushroom-cloud scale `max(dx,dz)*2` vs original `min(dx,dz)` | `TileEntityRBMKBase.java:541` → `RBMKColumnBlockEntity.java:313` |
| D7 | DEVIATION | all columns / autoloader | Meltdown flood fill captures any `RBMKColumnBlockEntity` — melts adjacent autoloaders (and panel/console devices sharing the base); original melts only `TileEntityRBMKBase` | `TileEntityRBMKBase.java:575–591` → `RBMKColumnBlockEntity.java:267` + `RBMKAutoloaderBlockEntity.java:24` |
| D8 | DEVIATION | rbmk_autoloader | Physical footprint: 9-block dummyable tower vs 1-block cap + downward BER piston | `RBMKAutoloader.java:15–31`, TE L243–247 → `RBMKAutoloaderBlockEntity.java:184–188`, `MachineRbmkControlRenderer.java:261–269` |
| A1 | ABSENT | rbmk_autoloader | GUI +/- cycle buttons + % readout (threshold now only settable via settings paste) | `GUIRBMKAutoloader.java:37–53,62–63` → `GUIRBMKAutoloader.java` (none), no packet |
| A2 | ABSENT | rbmk_autoloader | Looped `wgh_start` / `wgh_stop` operating sounds | `TileEntityRBMKAutoloader.java:142–155` → port client tick silent |
| A3 | ABSENT | rbmk_autoloader | "tower" steam particle burst at full extension | `TileEntityRBMKAutoloader.java:157–171` → none |
| A4 | ABSENT | rbmk_autoloader | Hopper/pipe automation (sided slots gated by piston) | `TileEntityRBMKAutoloader.java:212–220` → no item capability on BE |
| E1 | EXTRA | rbmk_cooler | Console readout `cooled/cryo/maxCryo/hot/maxHot` (original cooler sends no console NBT) | `TileEntityRBMKCooler.java` (no override) → `RBMKCoolerBlockEntity.java:170–178` |
| E2 | EXTRA | rbmk_heater | Item handler exposed for automation (original slot not hopper-accessible) | `TileEntityRBMKSlottedBase.java:130–132` → `RBMKHeaterBlockEntity.java:187–200` |
| E3 | EXTRA | rbmk_boiler | `ventDelay` persisted in NBT (trivial) | — → `RBMKBoilerBlockEntity.java:405,413` |
| G1 | GLUE | all | AuxParticle → vanilla particles, ByteBuf sync → update tags, MK2 fluid API, OC/EnergyControl dropped, `HEAT_PER_MB=0`-division guard in heater | various |

---

## 8. Verdict summary

| Matrix id | Verdict | One-liner |
|---|---|---|
| rbmk_boiler | **MINOR** | Conversion math, vent, compression ladder, GUI and meltdown are 1:1; compression heat/feed guard missing, fluid connectivity subset. |
| rbmk_cooler | **MATCH** | 50 mB / −200 heat / 5×5@60t cycle identical; only cosmetic console EXTRA + shared connectivity note. |
| rbmk_heater | **MATCH** | Trait-driven 2000·eff TU/°C exchanger is 1:1 incl. trait data; retyping gate and shift-click differ trivially. |
| rbmk_steam_inlet | **MATCH** | Water 32k tank, reasim-dial gate, 4-column push to 16k — 1:1. |
| rbmk_steam_outlet | **MATCH** | Superhot-steam 32k tank, reasim-dial gate, 4-column drain — 1:1. |
| rbmk_autoloader | **MINOR** | Rod swap state machine and menu layout 1:1; GUI threshold buttons, sounds, particles and automation missing; meltdown can destroy it (D7). |
