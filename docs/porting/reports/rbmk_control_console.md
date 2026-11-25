# RBMK Control Rods & Consoles — 1.7.10 Fidelity Audit

Matrix ids: `rbmk_control`, `rbmk_control_auto`, `rbmk_console`, `rbmk_crane_console`.
Audit date: 2026-11-22. Working-tree audit (dirty tree; no git HEAD trust). Read-only except this file.

Sources of truth:
- Original: `C:\Projects\Hbm-s-Nuclear-Tech-GIT\src\main\java\com\hbm\...` (cited as `ORIG`).
- Port: `C:\Projects\HBM-Modernized-agents\src\main\java\com\hbm_m\...` (cited as `PORT`).

Classification: `[GLUE]` framework necessity, `[COSMETIC]` naming/strings/visuals, `[DEVIATION]` semantic
difference, `[ABSENT]` original feature missing, `[EXTRA]` port-only feature.

---

## 1. rbmk_control — Manual Control Rod

Scope: ORIG `blocks/machine/rbmk/RBMKControl.java`, `tileentity/machine/rbmk/TileEntityRBMKControl.java`,
`TileEntityRBMKControlManual.java`, `inventory/gui/GUIRBMKControl.java`, `inventory/container/ContainerRBMKControl.java`,
`render/tileentity/RenderRBMKControlRod.java`, `packet/toserver/NBTControlPacket.java`;
PORT `block/machines/rbmk/RBMKControlManualBlock.java`, `blockentity/machines/rbmk/RBMKControlBlockEntity.java`,
`RBMKControlManualBlockEntity.java`, `inventory/gui/GUIRBMKControl.java`, `inventory/menu/RBMKControlMenu.java`,
`network/RBMKControlPacket.java`, rod-cap drawing in `client/render/implementations/RBMKColumnRenderer.java`.

### Per-topic verdicts

| Topic | Verdict | Evidence |
|---|---|---|
| Insertion speed constant | MATCH | ORIG `TileEntityRBMKControl.java:31` `speed = 0.00277` × `RBMKDials.getControlSpeed` (dial default 1.0, min 0); PORT `RBMKControlBlockEntity.java:28` `SPEED = 0.00277` × `RBMKDials.getControlSpeed` (`RBMKDials.java:69`, default 1.0). ~18 s full travel both sides. |
| Movement toward target, snap, power draw only while moving | MATCH | ORIG `updateEntity` L94–108; PORT `moveLevelToTarget` L91–100 (`lastLevel` captured before move, `power -= CONSUMPTION` only when `level != lastLevel` and powered). |
| ReaSim power requirement | MATCH | ORIG `isPowered()` = block is `rbmk_control_reasim(_auto)`; consumption 5 000 HE, buffer 50 000 (`consumption*10`), connect DOWN only, `ConnectionPriority.LOW` (L36–57). PORT `powered` flag set from block variant (`RBMKControlManualBlock.java:38`), `CONSUMPTION = 5_000`, `MAX_POWER = 50_000`, `canConnectEnergy` DOWN, `Priority.LOW` (`RBMKControlBlockEntity.java:44–46,111–127`). |
| GUI level presets | MATCH | ORIG `GUIRBMKControl.java:50–56`: 5 buttons, `level = 1.0 - k*0.25` (100/75/50/25/0 %) at x 118, y 26+k*11; PORT `GUIRBMKControl.java:91–97` identical hitboxes and values, now routed through `RBMKControlPacket` (server-side). |
| GUI layout | MATCH | 176×186, rod bar blit at (75,29) src (176, 56-h) 8×h, color marker (28, 26+c*11) src (184, c*10) 12×10, power icon (87,21) src (196, hasPower?16:0) — ORIG L83–96 vs PORT L47–63. Tooltips for level (71,29,16,56) and power (87,21,16,16) match. |
| Menu slots | MATCH | Player inventory only; ORIG rows at y 104/122/140 + hotbar 162 (`ContainerRBMKControl.java:14–22`); PORT `RBMKControlMenu.java:26–30` identical; `quickMoveStack` empty, `canInteractWith` true both sides. |
| Color/type variants | MATCH | 5 rod variants both sides: `rbmk_control` / `_mod` / `_auto` / `_reasim` / `_reasim_auto` (ORIG `ModBlocks.java:2117–2121`; PORT `ModBlocks.java:2307–2311`). 5 color groups RED/YELLOW/GREEN/BLUE/PURPLE (ORIG `RBMKColor` enum, `TileEntityRBMKControlManual.java:134–140`; PORT `short color` −1..4, cap textures via `getCapTexture()` `RBMKControlManualBlockEntity.java:72–81`). Rod cap textures standard/auto/5 colors: ORIG `RenderRBMKControlRod.java:17–25`; PORT `RBMKColumnRenderer.renderControlRod` L287–311 (lerp `lastLevel→level`, anchored at top block — same math as ORIG L34–67). |
| Surge ("positive scram") | MATCH | ORIG `getMult()` L51–62: `sin((1-level)^15·π)·(startingLevel−targetLevel)·surgeMod` gated by `targetLevel < startingLevel && |level−target| > 0.01`; PORT `getMult()` `RBMKControlManualBlockEntity.java:56–63` identical formula and gates; `setTarget` records `startingLevel = level` both sides (ORIG L46–49; PORT L43–46). |
| Console NBT payload | MATCH (minor) | ORIG base `getNBTForConsole` = `level` + `targetLevel`; manual adds `color` short (L148–157). PORT base = `level` + `color` short (`RBMKControlBlockEntity.java:175–180`); `targetLevel` not exported — only consumed by OpenComputers (absent, see below) and the console GUI/renderer never read it. |
| Meltdown | MATCH | ORIG `onMelt` L168–186: 2–3 GRAPHITE debris if moderated, then 2–3 ROD debris, then `standardMelt`. PORT `onMelt` `RBMKControlBlockEntity.java:161–169` identical counts (`2 + nextInt(2)`), `standardMelt` from `RBMKColumnBlockEntity`. |
| ROR (Redstone over Radio) | MATCH | ORIG `getFunctionInfo`/`provideRORValue`/`runRORFunction` (`extraction`, `setrods`, `extendrods` with clamp 0–100 / ±100) L246–230; PORT `RBMKControlBlockEntity.java:212–223` + `RBMKControlManualBlockEntity.java:131–155` 1:1. |
| Copier (screwdriver settings tool) | MATCH | color only, ORIG `getSettings/pasteSettings` L187–201; PORT `RBMKControlManualBlockEntity.java:112–127` (missing key → ungroup on both sides). |
| OpenComputers component `rbmk_control_rod` | ABSENT (environmental) | ORIG L201–244 OC callbacks. OpenComputers does not exist on 1.20.1/1.21.1; no port analog. Accepted gap. |
| Config gating | MATCH | `dialControlSpeed` (1.0, floor 0) and `dialControlSurgeMod` (1.0) gamerule-dials on both sides (ORIG `RBMKDials.java:27,30,99,102`; PORT `RBMKDials.java:29,31,69,72`). |

### Deviation list

1. `[DEVIATION]` **Color-group toggle missing.** ORIG `receiveControl` L76–86: sending the color the rod
   already has sets `color = null` (clicking the active color button un-groups the rod). PORT
   `GUIRBMKControl.java:100–105` sends `k` unconditionally and `RBMKControlPacket.java:68`
   (`ACTION_SET_COLOR -> be.color = clamp(intVal, -1, 4)`) stores it directly — there is no way to
   un-group from the rod GUI (only the copier's empty-paste path clears it).
2. `[DEVIATION]` **No `hasPermission` reach check on control packets.** ORIG `NBTControlPacket.Handler`
   enforces `IControlReceiver.hasPermission` (< 20 blocks, ORIG `TileEntityRBMKControlManual.java:65–67`)
   and fires a cancellable `PlayerInteractEvent` before `receiveControl`. PORT `RBMKControlPacket.handle`
   (L61–89) and `RBMKConsoleControlPacket.handle` (L67–74) only require the BE to exist in a resident
   chunk (`ModPacketHandler.blockEntityAt`, `ModPacketHandler.java:569`) — any player can drive any
   loaded rod/console from any distance, and protection mods cannot veto. (Shared with the console; see §3.)
3. `[COSMETIC]` Power tooltip prints raw `long`s (`GUIRBMKControl.java:77`) where ORIG uses
   `BobMathUtil.getShortNumber` (`GUIRBMKControl.java:39`) — "50000 HE" vs "50k HE".
4. `[COSMETIC]` Block resistance 18.0 vs ORIG 30.0 (`ModBlocks.java:2307` vs ORIG `RBMKBase.java:55`);
   hardness 3.0 matches. All RBMK control blocks affected.
5. `[COSMETIC]` ORIG deserialize color clamp quirk `RBMKColor.values()[clamp_int(color, 0, length)]`
   can throw on corrupt packets (ORIG `TileEntityRBMKControlManual.java:130`); PORT stores the short
   raw and clamps in the packet — port is safer, original bug not replicated (acceptable).
6. `[EXTRA]` PORT persists `lastLevel`, `power`, `hasPower` to NBT (`RBMKControlBlockEntity.java:184–208`);
   ORIG persists only `level`/`targetLevel` (+manual `startingLevel`/`mult`/`color`). Benign: fixes a
   client interpolation snap and keeps the buffer across reloads. PORT also does client-side prediction
   `be.setTarget(target)` on click (`GUIRBMKControl.java:94`) — original GUI is purely server-driven.
7. `[ABSENT]` ORIG writes `nbt.setDouble("mult", getMult())` in `writeToNBT` (L109) for the RBMK diag
   dump; PORT `writeNbtData` does not write a `mult` key, so the diag overlay lacks the live mult readout.
8. `[GLUE]` Sync transport: ORIG per-tick custom `serialize/deserialize` (level/targetLevel/power/hasPower,
   manual + startingLevel/color) via `networkPackNT(50)`; PORT sends the full `writeNbtData` tag via
   `sendBlockUpdated` every tick from `RBMKColumnBlockEntity.baseTick` (L92). Same data, same cadence.
9. `[GLUE]` Energy: ORIG power ports (DOWN) pulled each tick; PORT `EnergySubscriptions` +
   `receiveEnergy` with the same DOWN connection and buffer semantics.

### Verdict: MINOR — core simulation (speed, surge, power, colors, meltdown) is 1:1; one gameplay
deviation (no color un-group toggle) and one security deviation (no 20-block permission gate).

---

## 2. rbmk_control_auto — Automatic Control Rod

Scope: ORIG `blocks/machine/rbmk/RBMKControlAuto.java`, `tileentity/machine/rbmk/TileEntityRBMKControlAuto.java`,
`inventory/gui/GUIRBMKControlAuto.java`, `inventory/container/ContainerRBMKControlAuto.java`;
PORT `block/machines/rbmk/RBMKControlAutoBlock.java`, `blockentity/machines/rbmk/RBMKControlAutoBlockEntity.java`,
`inventory/gui/GUIRBMKControlAuto.java`, `inventory/menu/RBMKControlAutoMenu.java`.

### Per-topic verdicts

| Topic | Verdict | Evidence |
|---|---|---|
| Function set | MATCH | ORIG `RBMKFunction { LINEAR, QUAD_UP, QUAD_DOWN }` L148–152; PORT `RBMKControlAutoBlockEntity.RBMKFunction` L122 — same three, same order (icons UV `184, f*19` depend on it). |
| LINEAR formula | MATCH | ORIG L61 `(heat−heatLower)·((levelUpper−levelLower)/(heatUpper−heatLower)) + levelLower`; PORT L85 identical. |
| QUAD_UP formula | MATCH | ORIG L66 `pow((heat−heatLower)/(heatUpper−heatLower), 2)·(levelUpper−levelLower) + levelLower`; PORT L87 identical. |
| QUAD_DOWN formula | MATCH | ORIG L70 `pow((heat−heatUpper)/(heatLower−heatUpper), 2)·(levelLower−levelUpper) + levelUpper`; PORT L89 identical. |
| Out-of-band behavior | MATCH | heat below min(heatLower,heatUpper) → `levelLower`; above max → `levelUpper` (ORIG L46–55; PORT L71–77). |
| Percent → fraction | MATCH | `targetLevel = clamp(fauxLevel·0.01, 0, 1)` (ORIG L75–76; PORT L95). |
| Field defaults | MATCH | All four doubles 0.0 on both sides (ORIG uninitialised fields L24–27; PORT explicit 0.0 L33–36 — comment records that invented defaults 0/100/100/1200 were removed). |
| NBT keys | MATCH | `levelLower`, `levelUpper`, `heatLower`, `heatUpper`, `function` — ORIG L98–108; PORT L102–120 (same key strings). |
| GUI text fields | MATCH | 4 fields at (30, 27+11i) w26, maxLength 3/3/4/4, order `levelUpper, levelLower, heatUpper, heatLower`; save button (28,70,30×10); clamps 0–100 for level fields, 0–9999 for heat fields (ORIG `GUIRBMKControlAuto.java:44–59,100–126`; PORT `GUIRBMKControlAuto.java:57–69,154–194`). |
| GUI function buttons + icon | MATCH | 3 buttons (61, 48+k·11, 22×10); icon blit (59,27) src (184, f*19) 26×19; level bar at (124,29); power icon (136,21) src (210, hasPower?16:0); tooltips "Select linear/quadratic/inverse quadratic interpolation", field tooltips, "Function: Linear/Quadratic/Inverse Quadratic" — ORIG L66–89,155–164; PORT L86–146. |
| receiveControl semantics | MATCH | Function click sets function; save click sets the four params (ORIG L131–146; PORT `RBMKControlPacket` actions 2/3). |
| Copier | MATCH | ORIG copies all four params + function ordinal (L171–188); PORT L127–145 identical. |
| Power/movement base | MATCH | Inherits the same base class as the manual rod on both sides (speed, consumption, surge-free `getMult()` = level, meltdown debris). |
| Config gating | MATCH | Same control-speed dial as §1; no auto-specific config on either side. |

### Deviation list

1. `[DEVIATION]` **Zero-range guard added.** PORT `computeTarget` L79–83: when `heatUpper == heatLower`
   inside the band it returns `levelLower`; ORIG divides by zero → NaN targetLevel → every comparison
   false → rod frozen at its last level until reconfigured. Defensive fix; changes the (broken) original
   edge case. PORT-only code path, no original analog.
2. `[DEVIATION]` **Console interop widened (shared with §3):** in the port, auto rods report
   `ColumnType.CONTROL` (base `RBMKControlBlockEntity.java:66`, no override) with an `"auto"` NBT flag
   instead of a distinct `CONTROL_AUTO` type. Consequence: console "select all control rods", color
   assignment, AZ-5 and console set-level also hit auto rods — ORIG restricts those to
   `TileEntityRBMKControlManual` (console `receiveControl` L273, L314). Practical impact is small (auto
   rods overwrite `targetLevel` every tick), but color-marked auto rods on the mini-map are possible
   in the port and impossible in ORIG.
3. `[COSMETIC]` Function-ordinal robustness: ORIG `readFromNBT` leaves `function = null` on legacy saves
   (NPE risk in `switch`), and `deserialize` indexes the enum unguarded (L127); PORT always writes the
   ordinal and clamps reads (L114–115). ORIG `receiveControl` guards with `% RBMKColor.values().length`
   (=5) instead of the function enum length (L134) — an out-of-range value crashes ORIG; PORT clamps to
   0..2. Bug-fix deviations.
4. `[EXTRA]` PORT `getNBTForConsole` adds `"auto" = true` (`RBMKControlAutoBlockEntity.java:50–54`) so the
   console mini-map can draw the blue auto-dot (ORIG distinguishes via `ColumnType.CONTROL_AUTO`,
   `RenderRBMKConsole.java:85,155–157`). Required by the type collapse; renderer honors it
   (`MachineRbmkControlRenderer.java:142–143`).
5. `[GLUE]` Sync: same `sendBlockUpdated` NBT transport as §1.

### Verdict: MINOR — formulas, GUI, NBT and defaults are 1:1; only a defensive zero-range guard and the
console-interop widening deviate.

---

## 3. rbmk_console — RBMK Console

Scope: ORIG `blocks/machine/rbmk/RBMKConsole.java`, `tileentity/machine/rbmk/TileEntityRBMKConsole.java`,
`inventory/gui/GUIRBMKConsole.java`, `render/tileentity/RenderRBMKConsole.java`,
`items/tool/ItemRBMKTool.java`, `packet/toserver/NBTControlPacket.java`;
PORT `block/machines/MachineRbmkConsoleBlock.java`, `blockentity/machines/MachineRbmkConsoleBlockEntity.java`,
`inventory/gui/GUIMachineRbmkConsole.java`, `inventory/menu/MachineRbmkConsoleMenu.java`,
`network/RBMKConsoleControlPacket.java`, `item/rbmk/RBMKToolItem.java`,
console/crane drawing in `client/render/implementations/MachineRbmkControlRenderer.java`.

### Per-topic verdicts

| Topic | Verdict | Evidence |
|---|---|---|
| Scan size / pattern | MATCH | ORIG 15×15 = **225 columns** centered on the linked column (`columns[15*15]`, offsets `col%15−7`, `col/15−7`, L53, L391–423). PORT `GRID=15`, `AREA=225`, `GRID_HALF=7`, centered on `reactorOrigin` with identical rotation math (`rotatedX/rotatedZ` L142–159 vs ORIG `getXFromIndex/getZFromIndex` L391–423). "22 sync units" in the matrix notes is wrong on both sides — the unit of account is 225 columns + 60 flux samples + 6 screens. |
| Rotation (screwdriver) | MATCH | ORIG `rotate()` `rotation = (rotation+1)%4`, screwdriver via `RBMKConsole.onScrew` L133–144; PORT `rotate()` L135–139, screwdriver handled in `MachineRbmkConsoleBlock.openMenu` L153–161. `[EXTRA]` PORT prints an action-bar "Grid rotation: X°" message. |
| Column data captured | MATCH | ORIG rescan L91–115: per column `getConsoleType()` + `getNBTForConsole()` + `heat` + `maxHeat` + `indicator` byte + `moderated` flag; fuel adds `lastFluxQuantity` to flux sum. PORT `scanReactor` L162–206: same fields (`indicator` as int, plus dead `"lid"`, see EXTRA) and same flux accumulation from `RBMKRodBlockEntity.lastFluxQuantity`. |
| Flux buffer | MATCH | 60 samples (`fluxDisplayBuffer = 60`), shift-left each rescan, newest last (ORIG L49–50,117–121; PORT `FLUX_BUF = 60` L63, ring shift L201–202). |
| Screens | MATCH | 6 screens; `ScreenType { NONE, COL_TEMP, ROD_EXTRACTION, FUEL_DEPLETION, FUEL_POISON, FUEL_TEMP }` identical order (ORIG L538–551; PORT L77–81). Per-screen column lists and statistic averaging 1:1 (`prepareScreenInfo` ORIG L124–188 vs PORT `computeScreenText` L213–283), including the `"<lang key>=<value>"` readout storage. |
| Screen formulas | MATCH | COL_TEMP mean heat; DEPLETION `100 − enrichment·100`; POISON mean xenon; FUEL_TEMP mean `c_heat`; ROD_EXTRACTION mean `level·100`; one-decimal truncation `((int)(v·10))/10D`. |
| Selection & control protocol | MATCH | Level apply: `level` double + `sel_<idx>` per selected column → target manual rods, clamp 0–1 (ORIG L259–281; PORT action 0 + `sendSetLevel` — GUI applies the same 0–100 % field parse and clamp). Screen toggle: `toggle` byte → next type cyclically (ORIG L283–288; PORT action 3). Screen assign: `id` byte + `s<i>` booleans → column list (ORIG L290–302; PORT action 5). Color assign: `assignColor` byte + `cols` int[] (ORIG L304–320; PORT action 2). Compressor: `compressor` + `cols` → boiler compressor cycle (ORIG L322–336; PORT action 6). |
| AZ-5 | MATCH (minor) | ORIG GUI AZ-5: lid open click, then 3 s cooldown (`lastPress + 3000`), sound `hbm:block.shutdown`, sends `level=0` + `sel_j` for every scanned CONTROL column (L220–241) → rods **travel** down at normal speed. PORT GUI L499–511 keeps the lid + 3 s cooldown and sends `ACTION_AZ5`; server action 1 `setTarget(0)` only (no instant `level = 0` slam — the comment records this was fixed). |
| Linking via RBMK tool | MATCH | ORIG `ItemRBMKTool`: click column → store pos; click console → `console.setTarget(tx,ty,tz)` (L29–67). PORT `RBMKToolItem.storeColumnPosition/linkConsole` (`RBMKToolItem.java:46–86`), console BE `reactorOrigin` = linked pos, treated as grid center — same semantics. `[EXTRA]` PORT validates the stored position and reports coordinates in success/failure messages. |
| Guide book pickup | MATCH | ORIG `RBMKConsole.onBlockActivated` L68–89: top-face hit within 0.1875 of vec (1.375, 0.75) rotated by facing, grants RBMK guide book if not owned. PORT `MachineRbmkConsoleBlock.openMenu` L174–196 same hit test (with per-facing `yRot` mapping), same book. |
| FBI mark | MATCH | ORIG `BossSpawnHandler.markFBI(player)` L58; PORT L170. |
| Multiblock shape | MATCH | ORIG dims `{3,0,0,0,2,2}` + front `{0,0,0,1,2,2}` = 5 wide × 4 tall × 2 deep; PORT layers reproduce 5×4×2 (`MachineRbmkConsoleBlock.java:52–86`). `[GLUE]` different multiblock system (MultiblockStructureHelper vs BlockDummyable). |
| Console has no inventory | MATCH | ORIG `provideContainer` returns null; GUI is a plain `GuiScreen` (no slots). PORT menu has zero slots (`MachineRbmkConsoleMenu`). |
| 3D minimap rendering | MATCH | Column strips at kx −0.3725, ky 3.625−(i/15)·0.125, kz 0.875−(i%15)·0.125; color palette 0xFF0000/0xFFFF00/0x008000/0x0000FF/0x8000FF; heat gradient `0.65+(i%2)·0.05`; crane indicator → yellow; fuel dot green (0, 0.25+e·0.75, 0); manual dot (level, level, 0), auto dot (level, 0, level) — ORIG `RenderRBMKConsole.java` L53–90,137–182; PORT `MachineRbmkControlRenderer.java` L95–146, 281–304 (auto via `"auto"` flag). Screen text layout (−0.42, 3.5, 1.75; ±3.5 z per column pair; −0.75 y per row; scale `min(0.03, 0.8/width)`; rotate 90°; green) — ORIG L93–129; PORT `renderScreenText` L306–330. |
| Console block properties | MATCH (minor) | ORIG hardness 3 / resistance 30; PORT strength 3.0/18.0 (`ModBlocks.java:520–521`) — resistance differs (`[COSMETIC]`). |
| Meltdown | MATCH | Neither side treats the console as an RBMK column: ORIG `TileEntityRBMKConsole extends TileEntityMachineBase`; PORT `MachineRbmkConsoleBlockEntity extends BaseHbmBlockEntity` — both are outside the meltdown flood fill. |
| Config gating | MATCH | No console-specific config on either side; dials shared. |
| OpenComputers `rbmk_console` (getColumnData, getRBMKPos, setLevel, setColumnLevel, setColorLevel, setColor, pressAZ5) | ABSENT (environmental) | ORIG L564–830. No OC on modern loaders; note that ORIG's server-side `pressAZ5` also plays `hbm:block.shutdown` at the console — the port plays its AZ-5 sound client-side only (`[COSMETIC]`, see below). |

### Deviation list

1. `[DEVIATION]` **Console control actions hit auto rods too.** ORIG `receiveControl` gates every rod
   action on `instanceof TileEntityRBMKControlManual` (L273, L314), and the GUI's AZ-5/select-all filter
   `ColumnType.CONTROL` (manual, L158, L233). PORT `handleControl` gates on `RBMKControlBlockEntity`
   (manual + auto: L292, L304, L313) and the GUI filters `ColumnType.CONTROL` which now includes auto
   (`GUIMachineRbmkConsole.java:435,452`). `MachineRbmkConsoleBlockEntity.java:289–318`.
2. `[DEVIATION]` **`assignColor` value not clamped server-side.** ORIG `EnumUtil.grabEnumSafely(RBMKColor, color)`
   clamps to 0–4 (L316); PORT `rod.color = (short) iVal` raw (L314) — a crafted packet can store
   out-of-range colors (no crash, but the group filter/tooltip guard it with `>= 0 && < 5`).
3. `[DEVIATION]` **Scan/sync cadence 20 ticks vs 10.** ORIG rescans and full-syncs every
   `TotalWorldTime % 10 == 0` (L75, L194) with a light packet (screen types only) in between (L212–219);
   PORT scans and `sendBlockUpdated`-syncs every 20 ticks (`scanTimer` L110–114). Console readouts and
   minimap react twice as slowly; no light/full packet split (`[GLUE]` transport).
4. `[DEVIATION]` **3D screen readouts show raw lang keys.** ORIG renderer splits the stored text on `"="`
   and resolves `parts[0]` via `I18nUtil.resolveKey(parts[0], parts[1])` (`RenderRBMKConsole.java:110–116`),
   so the physical panel shows "Temp: 123.4°C". PORT stores the same `"rbmk.screen.temp=123.4°C"` string
   (`MachineRbmkConsoleBlockEntity.java:274–281`) and the renderer draws it verbatim
   (`MachineRbmkControlRenderer.java:317–325`) — the six screens display `rbmk.screen.temp=123.4°C` etc.
   The lang keys exist (`ModLanguageProviderEn.java:392–396`); the resolution step is missing.
5. `[DEVIATION]` **Empty screens render blank.** ORIG divides by count without a guard → `(int)NaN = 0`
   → "Temp: 0.0°C"; PORT `count == 0 → ""` (L264–267). Trivial display difference.
6. `[DEVIATION]` **No `hasPermission` gate on the console control packet** — same finding as §1 item 2
   (ORIG console `hasPermission` < 20 blocks, L252–254, enforced by `NBTControlPacket`).
7. `[COSMETIC]` AZ-5 sounds substituted: lid `hbm:block.rbmk_az5_cover` → `IRON_TRAPDOOR_OPEN`, shutdown
   `hbm:block.shutdown` → `BEACON_DEACTIVATE` (`GUIMachineRbmkConsole.java:502–508`); ORIG also plays the
   shutdown sound server-side for OC-triggered AZ-5 — no server sound in PORT.
8. `[COSMETIC]` Flux chart drawn as green bars (`g.fill`, `GUIMachineRbmkConsole.java:269–275`) where ORIG
   draws a 2-px GL line strip (`GUIRBMKConsole.java:413–429`); same 60-sample data, same min/max labels
   at y≈98/127, same green 0x00ff00.
9. `[COSMETIC]` NBT key names restructured: ORIG `tX/tY/tZ`, `t0..t5` (byte ordinals), `s0..s5` int
   arrays, `rotation`; PORT `ox/oy/oz`, `screens` list of `{t:name, s:int[]}`, `rotation`. Same
   information, not save-compatible (expected across versions).
10. `[COSMETIC]` Column-type enum collapsed (PORT `RBMKColumnBlockEntity.ColumnType` L513–515 has 11
    values vs ORIG 14): `FUEL_SIM→FUEL`, `CONTROL_AUTO→CONTROL+auto flag`, `HEATEX→HEATER`, `BREEDER`
    missing (no breeder column exists in the port yet — tracked under the breeder matrix id). GUI icon
    offsets remapped 1:1 to the same texture (`typeOffset10`, `GUIMachineRbmkConsole.java:52–66`; HEATER
    reuses ORIG's HEATEX icon at 130). Tooltip type line shows `CONTROL` for auto rods where ORIG shows
    `CONTROL_AUTO`.
11. `[COSMETIC]` Boiler steam-grade overlay driven by `steamGrade` (0–3) instead of ORIG's `type` fluid
    ordinal comparison chain (ORIG `GUIRBMKConsole.java:382–389`); same four positions/UVs. Tooltip steam
    line uses fixed steam-grade names instead of the fluid's localized name (ORIG `Fluids.fromID(type)`.
    `getLocalizedName()`, L472) — equivalent for RBMK boilers, which only ever produce steam grades.
12. `[COSMETIC]` GUI hover quirk: ORIG tooltip requires `index > 0` so column 0 has no tooltip
    (`GUIRBMKConsole.java:82`); PORT shows it (`GUIMachineRbmkConsole.java:300`). Heat-bar clamps
    (`Mth.clamp … 0..CELL`, skip when ≤ 0) are tighter than ORIG's unclamped draw — invisible in practice.
13. `[EXTRA]` `screenText` and `fluxBuffer` persisted to NBT (ORIG recomputes/loses them on reload);
    `"lid"` int written into every column's console data with no consumer (dead data, ORIG reads lid
    state client-side from block state instead); screwdriver rotate confirmation message; AZ-5 button
    tooltip; `RBMKToolItem` link validation messages.
14. `[GLUE]` Sync transport (full NBT via `sendBlockUpdated` vs ORIG's light/full custom packet); menu
    opened via `MenuRegistry.openExtendedMenu` vs `FMLNetworkHandler.openGui`; sneak-click on ORIG's
    console does nothing while PORT treats it like a normal click.

### Verdict: MINOR — scan/sync/screens/protocol/layout are faithful; the visible deviation is the raw
lang key on the physical screens, plus auto rods being reachable from console actions and the missing
permission gate.

---

## 4. rbmk_crane_console — Crane Console

Scope: ORIG `blocks/machine/rbmk/RBMKCraneConsole.java`, `tileentity/machine/rbmk/TileEntityCraneConsole.java`,
`render/tileentity/RenderCraneConsole.java`, `handler/HbmKeybinds(Story).java`, `extprop/HbmPlayerProps.java`,
`packet/toserver/KeybindPacket.java`, `items/tool/ItemRBMKTool.java`;
PORT `block/machines/rbmk/RBMKCraneConsoleBlock.java`, `blockentity/machines/rbmk/RBMKCraneConsoleBlockEntity.java`,
`network/RBMKCraneControlPacket.java`, `network/RBMKCraneKeyState.java`, `config/ModConfigKeybindHandler.java`,
`item/rbmk/RBMKToolItem.linkCrane`, crane drawing in `MachineRbmkControlRenderer.java`.

### What the crane console does (verified both sides)

It is the control desk for a physical gantry crane over the reactor: after linking with the RBMK tool,
a player standing in the desk's detection AABB (±1.5 sideways, 2 forward, 2 tall) moves the crane with
the five crane keybinds and presses LOAD to lower the claw (`progress` 1→0 at 0.04/tick) and swap the
loaded item with the hovered column via `IRBMKLoadable` (`canTargetInteract` → `load`/`provideNext`+
`unload`). Both sides implement exactly this loop (ORIG `TileEntityCraneConsole.updateEntity` L72–194;
PORT `craneTick` L91–197).

### Per-topic verdicts

| Topic | Verdict | Evidence |
|---|---|---|
| Key states | MATCH | ORIG 5 binds `CRANE_UP/DOWN/LEFT/RIGHT/LOAD` (`HbmKeybinds.java:255–259`), client sends state on press AND release (`handleProps` L225–241, `KeybindPacket`), server keeps live held state in `HbmPlayerProps.keysPressed`. PORT `RBMKCraneControlPacket` sends all five booleans on change (`ModConfigKeybindHandler.java:203–212`) into `RBMKCraneKeyState` per-player map; opposite keys cancel (`craneTick` L146–149) exactly like ORIG's `up && !down` chain (L157–172). Default binds: arrows match; LOAD = Enter (ORIG) vs Right Shift (PORT) — `[COSMETIC]`. |
| Movement speed | MATCH | `SPEED = 0.05` blocks/tick both sides (ORIG L57; PORT L32); clamps `posFront ∈ [−spanB, spanF]`, `posLeft ∈ [−spanR, spanL]` (ORIG L179–180; PORT L171–172). |
| Joystick tilt | MATCH (values) | ±30 degrees while a direction is held, zeroed otherwise (ORIG L146–172; PORT L137–154). |
| Claw cycle | MATCH | `goesDown` → `progress −= 0.04` → at 0: load/unload via `IRBMKLoadable` + `canTargetInteract`; rise `+0.04` capped at 1 (ORIG L98–129; PORT L105–125). `isCraneLoading` = `progress != 1`; LOAD ignored while loading (ORIG L149; PORT L142). |
| Target column lookup | MATCH | ORIG `getColumnAtPos` computes `center − dir·posFront − left·posLeft` at `centerY − 1`, then `findCore` down to the real column TE (L224–245). PORT `getColumnAtPos` same math, resolves via `RBMKSteamInletBlockEntity.findColumnCore` (L217–223). |
| Linking & spans | MATCH | ORIG `setTarget`: `centerY = y + getColumnHeight + 1`, `girderY = centerY + 6`, four `findRoomExtent(x, girderY, z, dir, 16)` scans starting opposite the facing then rotating up, `height = 7`, `setUpCrane = true` (L298–329). PORT `setTarget` L230–249 identical (scan order F→R→B→L with `getOpposite()` + `getClockWise()` = ORIG's `getRotation(UP)` chain). |
| craneIndicator flash | MATCH | ORIG sets `aboveColumn.craneIndicator = 10` every tick (L95–96), base decrements per tick (`TileEntityRBMKBase.java:112`); console minimap renders indicator > 0 yellow (ORIG `RenderRBMKConsole.java:77`). PORT identical (`craneTick` L102–103, `RBMKColumnBlockEntity.baseTick` L87, renderer L125–127). |
| Loaded rod readouts | MATCH (minor) | heat = `ItemRBMKRod.getHullHeat`, enrichment = `getEnrichment` while an `ItemRBMKRod` is loaded (ORIG L184–190; PORT L174–181 via `RBMKRBMKRodItem`). ORIG parks both gauges at **0** when empty; PORT parks them at **20** (comment claims CE uses 20 — the source says 0). |
| Sync payload | MATCH | ORIG `serialize` sends setUpCrane + rotation offset + center + spans + height + posFront/Left + progress + hasLoaded + gauges, every tick `networkPackNT(250)` (L253–296). PORT sends the same values (plus tilt) in the BE NBT via `sendBlockUpdated` whenever any of them changes (L191–196) — `[GLUE]`. |
| NBT persistence | MATCH | ORIG keys `crane`, `craneRotationOffset`, `centerX/Y/Z`, `spanF/B/L/R`, `height`, `posFront`, `posLeft`, `held` (L336–378); PORT `setUpCrane`, `facing` (extra), `craneRotationOffset`, `centerX/Y/Z`, spans, `height`, `posFront`, `posLeft`, `progress`, `tiltFront/Left`, `loadedHeat/Enrichment`, `loadedItem` (L272–320). Core keys 1:1; extras support the sync path. |
| Block shape | MATCH | ORIG `getDimensions {1,0,0,0,1,1}` + front `{0,0,0,1,1,1}` (3 wide, 2 deep, back row 2 tall), top dummy half-height collision (`RBMKCraneConsole.java:64–82`); PORT layers `OOO/OCO` + half-height `UUU` row (`RBMKCraneConsoleBlock.java:51–81`). |
| No GUI | MATCH | ORIG `RBMKCraneConsole` has no `onBlockActivated` GUI; flown entirely by keybind. PORT `use()` handles only the RBMK tool and rotation, never opens a menu. |
| Crane renderer (gantry) | MATCH | Girder span per rotation offset (0/90/180/270 cases with same translate math), `Main`, `height−6` tubes (=1), `Carriage`, `Lift` lowered by `3.25·(1−progress)` — ORIG `RenderCraneConsole.java:94–156`; PORT `MachineRbmkControlRenderer.java` L190–250, same constants incl. 3.25. |
| OpenComputers `rbmk_crane` (move/load/getRodInfo/getDepletion/getXenonPoison/getCranePos) | ABSENT (environmental) | ORIG L391–480. No OC on modern loaders. |
| Config gating | MATCH | Uses the shared `dialColumnHeight` (4→3) via `RBMKDials.getColumnHeight` in `setTarget` (PORT L233), same as ORIG L300. No crane-specific config either side. |

### Deviation list

1. `[DEVIATION]` **20-tick direction latch instead of continuous key tracking.** ORIG re-samples the
   live held-key state every tick and stops within one tick of key release (`posFront += speed` only
   while the key is down, L157–172). PORT latches the direction every 20 ticks
   (`COOL_DOWN = 20`, `isCooledDown` + re-arm logic, `RBMKCraneConsoleBlockEntity.java:44–59,159–169`):
   movement rate while held is identical (0.05/tick), but after releasing a key the crane **coasts up to
   19 further ticks (~0.95 blocks)** until the next latch expiry. The in-code comment asserts
   "CE moves the crane in whole-block steps" — not true of the original source; this is a deliberate
   re-design (snap-to-column feel), not a 1:1 port.
2. `[DEVIATION]` **Empty-claw gauge value 20 vs 0.** ORIG sets `loadedHeat = loadedEnrichment = 0` when
   nothing is loaded (L184–190); PORT parks both at 20 (L177–181, comment misquotes CE). The desk dials
   read ambient temperature instead of zero.
3. `[DEVIATION]` **Crane console is meltable in the port.** ORIG `TileEntityCraneConsole extends
   TileEntityLoadedBase` — outside `TileEntityRBMKBase.getFF`, never melts. PORT
   `RBMKCraneConsoleBlockEntity extends RBMKColumnBlockEntity` with `participatesInHeatNetwork() = false`
   (L265–267) — but `RBMKColumnBlockEntity.meltdownReactor` flood-fills **any** `RBMKColumnBlockEntity`
   neighbor regardless of that flag (`RBMKColumnBlockEntity.java:262–270`), so a crane console (or RTTY
   panel) standing next to a melting reactor is pulled into the meltdown and `standardMelt` rewrites the
   blocks above it into debris. Edge case; the console also reports `ColumnType.BLANK`, so if it stands
   within a linked console's 15×15 grid it appears as a scanned BLANK tile where ORIG shows nothing.
4. `[DEVIATION]` **Rotation cycle input changed.** ORIG cycles `craneRotationOffset` with a
   **screwdriver** (`RBMKCraneConsole.onScrew` L85–98); PORT uses **sneak-click** on the block
   (`RBMKCraneConsoleBlock.java:162–165`); screwdriver does nothing.
5. `[DEVIATION]` `[COSMETIC]` **Console desk furniture reduced.** ORIG animates: joystick around Z
   (tiltFront) AND around X (tiltLeft) with pivot offsets (0.75, 1, 0 / −0.75, −1.015, 0), two wobbling
   dials (`Meter1` heat, `Meter2` enrichment, `135 − 270·value` with a time wobble), and two status lamps
   (`Lamp1` loading yellow / loaded green / off, `Lamp2` valid-target green / red) —
   `RenderCraneConsole.java:42–90`. PORT renders only a single-axis joystick tilt (tiltFront around Z,
   `MachineRbmkControlRenderer.java:182–183`) and static Meter1/Meter2; Lamp1/Lamp2 are not rendered at
   all. Functional state (`isAboveValidTarget` ORIG L208–210) exists in PORT only as
   `getLoadableAtPos` (`[ABSENT]` visual).
6. `[COSMETIC]` Default LOAD keybind Right Shift vs Enter; ORIG crane tilt on the client is computed from
   the client player's own key state (zero latency), PORT tilts arrive via server sync (`[GLUE]` minor
   latency); ORIG interpolates crane position over a 3-tick `turnProgress` window, PORT uses
   `lastPosFront/posFront` partial-tick lerp (`[GLUE]`).
7. `[EXTRA]` Crane can load/unload **outgasser** channels: PORT `RBMKOutgasserBlockEntity implements
   IRBMKLoadable` with real `canLoad`/`canUnload` (`RBMKOutgasserBlockEntity.java:38–40,235–237`); ORIG
   `IRBMKLoadable` implementors are only `TileEntityRBMKRod` and `TileEntityRBMKStorage`.
8. `[GLUE]` Facing source: ORIG derives directions from placement metadata; PORT keeps an explicit
   `FACING` blockstate + BE `facing` (set on place and at link time). Same axes for spans/movement.

### Verdict: MINOR — the load/unload loop, key protocol, speeds, spans and gantry rendering are 1:1;
the latch-based movement model and the meltable-column regression are the real deviations, desk
furniture (second joystick axis, dials, lamps) is cosmetic loss.

---

## Cross-cutting summary tables

### Constants comparison

| Constant | ORIG | PORT | Verdict |
|---|---|---|---|
| Rod speed | 0.00277/tick × dial(1.0) | `SPEED = 0.00277` × dial(1.0) | MATCH |
| ReaSim consumption / buffer | 5 000 / 50 000 HE | 5 000 / 50 000 HE | MATCH |
| Energy priority / side | LOW / DOWN | LOW / DOWN | MATCH |
| Surge gate / formula | `t < s && |l−t| > 0.01`; `sin((1−l)^15·π)·(s−t)·dial(1.0)` | identical | MATCH |
| Auto functions | LINEAR / QUAD_UP / QUAD_DOWN, ×0.01 then clamp | identical | MATCH |
| Colors | 5 (RED..PURPLE) | shorts 0–4, same palette/textures | MATCH |
| Console grid | 15×15 = 225, center ±7, rotation 0–3 | identical | MATCH |
| Flux buffer | 60 | 60 | MATCH |
| Screens | 6, 5 stat types, `key=value` text | identical | MATCH |
| Scan cadence | every 10 ticks | every 20 ticks | DEVIATION |
| AZ-5 cooldown | 3000 ms client-side | 3000 ms | MATCH |
| Crane speed / claw speed | 0.05 / 0.04 per tick | 0.05 / 0.04 | MATCH |
| Crane spans | `findRoomExtent(dir, 16)`, height 7, girderY = centerY+6 | identical | MATCH |
| Claw travel | 3.25 × (1−progress) | identical | MATCH |
| Tilt | ±30° | ±30° (single axis rendered) | MATCH |
| Block hardness / resistance | 3 / 30 | 3 / 18 | COSMETIC |

### NBT keys

- Control rod: `level`, `targetLevel`, `startingLevel`, `color` match; PORT adds `lastLevel`, `power`,
  `hasPower`; ORIG adds `mult` (diag) which the port lacks; `color` int→short.
- Auto rod: `levelLower`, `levelUpper`, `heatLower`, `heatUpper`, `function` — identical key strings.
- Console: ORIG `tX/tY/tZ`, `t0..t5`, `s0..s5`, `rotation` vs PORT `ox/oy/oz`, `screens[{t,s}]`,
  `rotation` (+`flux`, `screenText`, `cols`) — restructured, same content.
- Crane console: ORIG `crane`, `craneRotationOffset`, `centerX/Y/Z`, `spanF/B/L/R`, `height`,
  `posFront`, `posLeft`, `held` vs PORT `setUpCrane`, `craneRotationOffset`, `centerX/Y/Z`, spans,
  `height`, `posFront`, `posLeft`, `facing`, `progress`, `tiltFront/Left`, `loadedHeat/Enrichment`,
  `loadedItem` — core 1:1, extras feed the client sync.

### Sync packets

- ORIG: per-class `serialize/deserialize` (control: 4–6 fields every tick; console: light/full split at
  10 ticks; crane: full state every tick, range 250) + `NBTControlPacket` (C2S, permission-gated) +
  `KeybindPacket` (C2S, per-key press/release).
- PORT: `sendBlockUpdated` full-NBT sync (control every tick via `baseTick`; console every 20 ticks;
  crane on-change) + `RBMKControlPacket` / `RBMKConsoleControlPacket` / `RBMKCraneControlPacket` (C2S,
  not permission-gated). Transport differences `[GLUE]`; missing permission gate `[DEVIATION]`.

### Config gating

- `dialControlSpeed` (1.0, floor 0), `dialControlSurgeMod` (1.0), `dialColumnHeight` (4 → used as −1)
  exist on both sides with the same defaults and floors (ORIG `RBMKDials.java:24–30,96–102`;
  PORT `RBMKDials.java:23,29,31,64–72`). ORIG's client-side `getSurgeMod` quirk (returns the
  passive-cooling dial on the client, ORIG `RBMKDials.java:261–267`) is not replicated — port always
  returns the surge dial `[COSMETIC]`. No other config flags gate these four machines.

### Finding counts

| Id | DEVIATION | ABSENT | EXTRA | Verdict |
|---|---|---|---|---|
| rbmk_control | 2 (color toggle; permission gate) | 2 (OC component; `mult` diag key) | 2 | **MINOR** |
| rbmk_control_auto | 2 (zero-range guard; console interop widened) | 0 | 1 (`auto` flag) | **MINOR** |
| rbmk_console | 6 (auto rods in console actions; unclamped color; 20-tick cadence; raw lang key on 3D screens; empty-screen text; permission gate) | 1 (OC) | 5 | **MINOR** |
| rbmk_crane_console | 4 (movement latch; empty-gauge 20; meltable column; sneak-click rotation) | 1 (OC) + desk lamps/second tilt axis/dial needles as `[ABSENT]` visual | 1 (outgasser loadable) | **MINOR** |

### Top 3 most important findings

1. **3D console screens print raw lang keys** (`rbmk.screen.temp=123.4°C`): PORT
   `MachineRbmkControlRenderer.java:317–325` never resolves the `key=value` pair that the BE stores
   (ORIG `RenderRBMKConsole.java:110–116` splits and resolves). Highest-visibility fidelity break.
2. **No 20-block `hasPermission` gate on the three C2S control packets** (ORIG `NBTControlPacket.Handler`
   enforces `IControlReceiver.hasPermission` + cancellable interact event; PORT packets only check BE
   presence — `RBMKControlPacket.java:61–89`, `RBMKConsoleControlPacket.java:67–74`). Any player can
   drive any loaded control rod/console/AZ-5 from anywhere.
3. **Crane movement latching** (`RBMKCraneConsoleBlockEntity.java:44–59,159–169`): 20-tick direction
   resample makes the crane coast ~1 block after key release; the original tracks held keys every tick
   (`TileEntityCraneConsole.java:157–172` + press/release `KeybindPacket`). Changes crane feel/precision.
