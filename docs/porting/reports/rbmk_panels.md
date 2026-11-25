# RBMK Panel Devices — Fidelity Audit (matrix ids rbmk_display … rbmk_terminal)

Audit date: 2026-11-22. Read-only audit; nothing outside this report was modified.
Original (1.7.10): `C:\Projects\Hbm-s-Nuclear-Tech-GIT\src\main\java\com\hbm\…` — cited as `ORG`.
Port: `C:\Projects\HBM-Modernized-agents\src\main\java\com\hbm_m\…` — cited as `PORT`.
Line numbers refer to the working tree files as read on the audit date.

Classification used throughout: `[GLUE]` framework necessity (BE vs TE, Architectury, stonecutter,
NBT-vs-ByteBuf sync), `[COSMETIC]` naming/strings, `[DEVIATION]` semantic difference,
`[ABSENT]` original feature missing, `[EXTRA]` port-only feature.

---

## 1. Scope and file mapping

| Matrix id | Original block | Original TE | Port block | Port BE |
|---|---|---|---|---|
| rbmk_display | `blocks/machine/rbmk/RBMKDisplay` | `TileEntityRBMKDisplay` | `block/machines/rbmk/RBMKDisplayBlock` (`rbmk_display`) | `RBMKDisplayBlockEntity` |
| rbmk_gauge | `RBMKGauge` | `TileEntityRBMKGauge` | generic `RBMKPanelDeviceBlock` (`rbmk_gauge`, screen `gauge`, screwdriver-only) | `RBMKGaugeBlockEntity` |
| rbmk_graph | `RBMKGraph` | `TileEntityRBMKGraph` | generic `RBMKPanelDeviceBlock` (`rbmk_graph`, `graph`, screwdriver-only) | `RBMKGraphBlockEntity` |
| rbmk_indicator | `RBMKIndicator` | `TileEntityRBMKIndicator` | generic `RBMKPanelDeviceBlock` (`rbmk_indicator`, `indicator`, screwdriver-only) | `RBMKIndicatorBlockEntity` |
| rbmk_key_pad | `RBMKKeyPad` | `TileEntityRBMKKeyPad` | generic `RBMKPanelDeviceBlock` (`rbmk_key_pad`, `keypad`, primary click) | `RBMKKeyPadBlockEntity` |
| rbmk_lever | `RBMKLever` | `TileEntityRBMKLever` | generic `RBMKPanelDeviceBlock` (`rbmk_lever`, `lever`, primary click) | `RBMKLeverBlockEntity` |
| rbmk_numitron | `RBMKNumitron` | `TileEntityRBMKNumitron` | generic `RBMKPanelDeviceBlock` (`rbmk_numitron`, `numitron`, screwdriver-only) | `RBMKNumitronBlockEntity` |
| rbmk_terminal | `RBMKTerminal` | `TileEntityRBMKTerminal` | generic `RBMKPanelDeviceBlock` (`rbmk_terminal`, `terminal`, opens on any click) | `RBMKTerminalBlockEntity` |
| (base) | `RBMKMiniPanelBase` | — | `RBMKMiniPanelBlock` (abstract) + `RBMKPanelBlock`/`RBMKDisplayBlock` | `RBMKPanelBlockEntity` (blank) |

Block↔BE mapping verified in `PORT block/ModBlocks.java:2396-2456` (ids `rbmk_display`,
`rbmk_display_blank`, `rbmk_gauge`, `rbmk_indicator`, `rbmk_numitron`, `rbmk_graph`, `rbmk_lever`,
`rbmk_key_pad`, `rbmk_terminal` — all ids match ORG `blocks/ModBlocks.java:1104-1112,2133`).
`[GLUE]` One parameterized `RBMKPanelDeviceBlock` (PORT `block/machines/rbmk/RBMKPanelDeviceBlock.java`)
replaces 7 per-device block classes; per-type behavior (screwdriver-only config for gauge/indicator/
numitron/graph, primary click for lever/keypad, any-click GUI for terminal) is preserved through the
`screenId`/`requireScrewdriver`/`primaryClick` constructor arguments (L53-62, L92-104).

**Mini-panel base role**: ORG `RBMKMiniPanelBase` = `BlockContainer` + ISBRH, quarter-block recessed
body, facing from player yaw (meta 2/5/3/4), tooltip `addStandardInfo`. PORT `RBMKMiniPanelBlock`
reproduces facing property, the four voxel shapes in sixteenths (L51-54 — matches ORG
`setBlockBoundsBasedOnState` L52-61), placement via `getHorizontalDirection().getOpposite()`
(L66-69 — same yaw→meta mapping), and the standard tooltip (L110-118). `[GLUE]` the ISBRH body box
becomes the block model + `RenderShape.MODEL` (L101-104).

`rbmk_display_blank`: ORG registers `RBMKMiniPanelBase` with **no tile entity** (`ModBlocks.java:2133`);
PORT gives it a no-op `RBMKPanelBlockEntity` (`BaseEntityBlock` requires one) carrying vestigial
unused fields `linkedConsole/displayValue/channel` (PORT `RBMKPanelBlockEntity.java:16-22`). `[GLUE]`
+ `[EXTRA]` (dead fields, harmless).

GUIs: ORG `GUIScreenRBMKGauge/Graph/Indicator/KeyPad/Lever/Terminal/Display` (7 screens; the Numitron
**does have a GUI** — ORG `TileEntityRBMKNumitron.provideGUI` returns `GUIScreenRBMKDisplay`,
L170). PORT mirrors 7 screens `inventory/gui/rbmk/GUIRBMK*.java`; the Numitron's screen is named
`GUIRBMKNumitron` instead of `GUIRBMKDisplay` `[COSMETIC]`. `rbmk_display` has **no GUI in either**
(screwdriver rotate + tool link only) — verified both sides.

Renderers: ORG `RenderRBMKDisplay/Gauge/Graph/Indicator/KeyPad/Lever/Numitron/Terminal` →
PORT `client/render/implementations/MachineRBMKPanelRenderers.java` (7 specs, one per BE type,
L66-75) + `MachineRbmkDisplayRenderer.java` (display mini-map). Both register per BE type with
the same facing→rotation mapping (NORTH 90°, WEST 180°, SOUTH 270°, EAST 0°) — the port relies on
the factory block transform instead of an explicit rotate `[GLUE]`.

---

## 2. RTTY backbone (shared by all 8)

| Aspect | ORG | PORT | Verdict |
|---|---|---|---|
| Bus shape | static `(World, String) → RTTYChannel` maps, write queue flipped in `updateBroadcastQueue()` each server tick (`tileentity/network/RTTYSystem.java:22-24,52-75`, tick hook `main/ModEventHandler.java:1054`) | static `(dimension, channel) → RttyChannel` maps, flip via `tickIfNeeded(gameTime)` called from radio BEs' own ticks, once-per-tick guarded (`blockentity/network/radio/RTTYNetwork.java:24-82`) | `[GLUE]` equivalent |
| Numeric merge per tick | two numeric signals on one channel in one tick are **summed** (`RTTYSystem.broadcast` L27-43) | same long-sum with `isNumber` = `Long.parseLong` (L32-45); decimals fall through to overwrite like ORG `NumberUtils` + failing `parseLong` | MATCH |
| Test channel `2012-08-06` | injected into broadcast for every world every tick, `Song of Storms` at timestamp `worldTime` (L65-73, L91-196) | produced lazily in `listen()` with timestamp `gameTime-1` (L48-57) — same fresh-note-per-tick result | `[GLUE]` equivalent |
| Listener freshness rule | each device: `chan.timeStamp < worldTime - 1 → chan = null` (signal valid for the broadcast tick + 1) | **no freshness check in any panel listener** (see D1); the controller torch does check (`RadioTorchControllerBlockEntity.java:55`) | `[DEVIATION]` |
| Cleanup | none (channels persist until overwritten) | plus `onLevelUnload` / `onServerStop` clears (L85-100) | `[GLUE]` (fixes cross-world leak, documented) |
| Redstone-over-Radio | terminal implements `IRORInteractive`; `TileEntityRadioTorchController.java:67` dispatches `FUN:…` to nearby `IRORInteractive` | terminal implements `com.hbm_m.api.redstoneoverradio.IRORInteractive`; `RadioTorchControllerBlockEntity.java:55-61` dispatches 1:1 (incl. polling + freshness + prev-dedupe, and the `selfdestruct` special) | MATCH |

The port's `RTTYNetwork.selfDestruct` (L115-122) is a faithful VNT port of the original
selfdestruct ("ohne Blockschaden") — but the **terminal does not use it** (see T-DEV-1).

---

## 3. Cross-cutting findings

- **D13 `[DEVIATION]`** No permission gate on configuration packets. ORG `NBTControlPacket.java:86`
  enforces `IControlReceiver.hasPermission` (all 8 panels: 15-block radius, e.g.
  `TileEntityRBMKGauge.java:187-189`). PORT `RadioTorchControlPacket.handle` (L36-46) →
  `ModPacketHandler.blockEntityAt` (`network/ModPacketHandler.java:566-581`, explicitly "not an
  access check") — any player anywhere can reconfigure any loaded panel or run terminal commands.
- **A1 `[ABSENT]`** OpenComputers integration absent for all devices (ORG: `SimpleComponent` +
  callbacks on gauge/graph/indicator/keypad/lever/numitron/terminal). Port has no OC anywhere;
  terminal javadoc documents the trim (`RBMKTerminalBlockEntity.java:22-23`). Includes terminal
  `ocMode` (ORG L40-43, L55-59, L186-252).
- **[GLUE]** Per-tick sync: ORG `networkPackNT(50)` NT-ByteBuf every tick; PORT every panel's
  `tickPanel` → `RBMKColumnBlockEntity.baseTick` ends in `level.sendBlockUpdated(...)` every server
  tick (`RBMKColumnBlockEntity.java:85-96`), shipping the full `writeNbtData` payload. Same cadence,
  different transport; heavier but equivalent.
- **Config gating flags**: none on either side. ORG panel TEs reference no `GeneralConfig`; PORT
  `ConfigSchema` carries only unrelated `rbmkDials.*`. MATCH (no gating).
- **[COSMETIC]** Channel-name max length: ORG GUI caps at `MAX_CHAN_LENGTH = 15`
  (`GUIScreenRadioTorch.java:24`); PORT EditBoxes use 24 (`GUIRBMKGauge.java:57` etc.).
- **[COSMETIC]** Shared unit config NBT keys renamed `label+i/color+i` → `ulabel+i/ucolor+i`
  (`RBMKPanelDeviceBlockEntity.java:56-78`); bitmask `active/polling` bytes → per-index booleans.
  World saves don't carry over; wire format is private.

---

## 4. rbmk_display (7×7 reactor mini-map)

Original: `TileEntityRBMKDisplay.java` (rescan L69-99, rotate L121-147, NBT L101-119),
`RBMKDisplay.java` (screwdriver rotate L17-28), `ItemRBMKTool.java:87-100` (target link),
`RenderRBMKDisplay.java` (heat tint L59-62, color groups L51-57, crane indicator L64, dots L68-74).
Port: `RBMKDisplayBlockEntity.java` (scan L90-120, rotate L40-44, NBT L122-173),
`RBMKDisplayBlock.java:62-78`, `RBMKToolItem.linkDisplay` (`item/rbmk/RBMKToolItem.java:110-122`),
`MachineRbmkDisplayRenderer.java`.

| Topic | Verdict | Evidence |
|---|---|---|
| 7×7 grid, rescan every 10 ticks | MATCH | ORG L28 (`% 10 == 0`) vs PORT L68 (`SCAN_INTERVAL = 10`) |
| Target set by RBMK linking tool stored position | MATCH | ORG `ItemRBMKTool` L87-100 vs PORT `RBMKToolItem` L110-122 (port adds a red "no position" message — `[COSMETIC]`) |
| Rotation byte, screwdriver quarter-turn, rotated index math | MATCH | ORG L121-147 vs PORT L40-64 — the four `switch` formulas are identical |
| Payload per cell: heat, maxHeat, crane indicator | MATCH | ORG L82-84 vs PORT L110-112 |
| Manual control color group → panel tint | MATCH | ORG rescan L86-93 sets `"color"`; PORT manual control writes `"color"` in its own `getNBTForConsole` (`RBMKControlManualBlockEntity.java:115`, only when set — same conditional); renderer maps the same 5 swatches (`MachineRbmkDisplayRenderer.java:41` = ORG `RenderRBMKDisplay.java:53-57`) |
| CONTROL vs CONTROL_AUTO dot colors | `[GLUE]` | PORT folds both types into one `CONTROL` + `"auto"` bool in the console payload (`MachineRbmkDisplayRenderer.java:105-116`) — same yellow-vs-purple result |
| Geometry (8/7 scale, kx/ky/kz, cell quads, octagonal dots) | MATCH | ORG L31-33, 47-49, 83-128 vs PORT L74-78, 84-86, 134-152 |
| Client sync | `[GLUE]` | ORG networkPackNT(50) on rescan tick; PORT `syncToClient()` every rescan tick + per-tick baseTick sync |

Findings: **[EXTRA]** columns are persisted to NBT (`"cols"` list, PORT L129-137) — ORG keeps
`columns` transient and rebuilds by rescan. Harmless. No DEVIATION/ABSENT found.

**Verdict: MATCH** — no semantic differences found.

---

## 5. rbmk_gauge (4 needle gauges)

Original: `TileEntityRBMKGauge.java` (update L119-136 incl. freshness L126, GaugeUnit defaults
L105-111, NBT L160-180, receiveControl L192-209), `RenderRBMKGauge.java` (needle angle L50-58,
clamp 0-80 L58, min/max labels L77-92).
Port: `RBMKGaugeBlockEntity.java`, gauge hook `MachineRBMKPanelRenderers.java:217-281`.

| Topic | Verdict | Evidence |
|---|---|---|
| 4 units; polling/label/channel/min/max/value/color/active | MATCH (fields) | ORG L85-103 vs PORT L16-29 |
| Constructor colors 0x800000/0x804000/0x808000/0x000080, "Gauge N" | MATCH | ORG L106-110 vs PORT L40-47, L35 |
| Value read: accept fresh signal, else polling → 0 | `[DEVIATION]` | ORG L126 `timeStamp < worldTime-1 → chan=null`, L129-135 `value = sigVal` (0 on parse failure); PORT L59-64 uses `parseNum(signal, value[i])` with **no freshness check and previous-value fallback** |
| Polling semantics (drop to 0 when channel goes silent) | `[DEVIATION]` | documented intent PORT L24-29, but without D1's freshness check a silent channel keeps serving its stale entry, so a polling gauge never zeros |
| Needle interpolation | `[GLUE]` | ORG client-side `renderValue += delta*0.1` (L113-117); PORT renderer lerps `lastValue→value` (`MachineRBMKPanelRenderers.java:234`) fed by per-tick NBT sync — equivalent feel |
| Needle math (range normalize, inverted min>max, clamp 0-80) | MATCH | ORG L51-58 vs PORT L235-240 |
| min/max type | `[GLUE]` | long → double (PORT L17-18); GUI parses doubles (`GUIRBMKGauge.java:69-77,117-118`) |
| GUI fields (channel/label/color hex/min/max/active/polling) | MATCH | ORG `GUIScreenRBMKGauge.java:28-67,121-142` vs `GUIRBMKGauge.java:31-104,110-123` |
| NBT keys | `[COSMETIC]` | ORG `active0/polling0/color0/label0/rtty0/min0/max0/value0` vs PORT `active0/ulabel0/ucolor0/channel0/min0/max0/value0/polling0` (PORT L89-98) |
| OC component `rbmk_gauge` | `[ABSENT]` | ORG L211-313 |

Findings: **G-DEV-1** `[DEVIATION]` missing freshness check + parse-failure keeps old value
(PORT `RBMKGaugeBlockEntity.java:59-64` vs ORG `TileEntityRBMKGauge.java:126-135`). Consequences:
polling gauges never zero on a dead channel; an unparseable fresh signal leaves the old value
instead of forcing 0. **G-DEV-2** `[DEVIATION]` (same root) — a latching gauge in ORG also latches,
so normal latching behavior is unaffected; only polling/parse edge cases diverge.
No other deviations.

**Verdict: MINOR** — 2 DEVIATION findings (one root cause), 1 ABSENT (OC).

---

## 6. rbmk_graph (2 traces × 30 samples)

Original: `TileEntityRBMKGraph.java` (update every 10 ticks L43-51, freshness L109, pushValue
L121-127, min/maxBound swap L216-220, NBT L156-178), `RenderRBMKGraph.java` (bounds L55-56, polyline
L62-74, labels L77-87).
Port: `RBMKGraphBlockEntity.java`, graph hook `MachineRBMKPanelRenderers.java:496-561`.

| Topic | Verdict | Evidence |
|---|---|---|
| 2 units, 30-sample rolling buffer, 10-tick sampling ("2 values/s for 15 s") | MATCH | ORG L36, L88 comment, L47 vs PORT L18-19, L37-49 |
| Long.parseLong signal → push; polling pushes 0 on silence | `[DEVIATION]` (same root D1) | ORG L106-118 vs PORT L59-64 — no freshness check; polling never sees "silence" |
| Inactive / empty-channel units skip (trace freezes, stays blank) | MATCH | ORG L103-104 vs PORT L56-57 (port comment explicitly documents restoring CE behavior) |
| min/max fixed bounds, else auto-scale to data | MATCH | ORG L55-56 vs PORT L512-513 |
| GUI: channel/label/min/max/active/polling; empty min field = unbounded | `[GLUE]` | ORG uses empty text field = no key = unbound (`GUIScreenRBMKGraph.java:130-131` + TE L203-214); PORT uses explicit fixed/auto buttons sending `gmin/gmax` or `gminOff/gmaxOff` (`GUIRBMKGraph.java:87-91,127-129` + BE L80-84) — same result, different affordance |
| min>max swap on receive | `[DEVIATION]` | ORG L216-220 swaps; PORT `receiveControl` (L77-91) does not — a user-entered inverted bound makes the polyline degenerate instead of auto-correcting |
| Renderer: green `0x00FF00` line strip, clamped, dx/dy/dz formula, bound labels | MATCH | ORG L59-87 vs PORT L516-531 (`graphPoint` L543-548 = ORG L66-71 verbatim) |
| NBT keys | `[COSMETIC]` | ORG `minBound0/min0/.../value0_29` vs PORT `gmin0/gmax0/gminB0/gmaxB0/history0` (PORT L98-109) |
| OC component `rbmk_graph` (incl. getGraphMin/Max/Avg, pushGraphValue) | `[ABSENT]` | ORG L226-328 |

**Verdict: MINOR** — 2 DEVIATION findings (freshness/polling root D1; missing min/max swap), 1 ABSENT (OC).

---

## 7. rbmk_indicator (6 lamps)

Original: `TileEntityRBMKIndicator.java` (defaults L97-103 — `max = 100`, colors alternate
red/yellow; update L105-122; `decideLight` L125-131 with inverted-bounds rule; receiveControl
L187-204 — absent min key resets to `Integer.MIN_VALUE`/`MAX_VALUE`; NBT L155-175 incl. `light`),
`RenderRBMKIndicator.java` (lit multiplier 1/0.35, fullbright L45-49).
Port: `RBMKIndicatorBlockEntity.java`, indicator hook `MachineRBMKPanelRenderers.java:285-314`.

| Topic | Verdict | Evidence |
|---|---|---|
| 6 units; in-band lights (or out-of-band when max<min) | MATCH (logic) | ORG L125-131 vs PORT `decideLight` L72-77 — the inversion is reproduced verbatim |
| Lamp state synced to client and rendered | **`[DEVIATION]` — core break** | ORG serializes `light` every tick (L136, L147) and NBT-persists it (L163, L174); PORT `writeNbtData` (L101-110) omits `state[]` entirely and `readNbtData` never sets it, so the client BE's `state[]` stays `false` forever → lamps never light up in world even though the server side decides correctly (`renderIndicator` reads `be.state[i]`, `MachineRBMKPanelRenderers.java:300-305`) |
| Colors alternate 0xff0000/0xffff00 | MATCH | ORG L98-101 vs PORT L41-43 |
| Default max | `[DEVIATION]` | ORG L91 `max = 100` vs PORT L35 `Arrays.fill(max, 1.0)` — fresh lamp lights for 0..1 instead of 0..100 |
| Empty min/max fields = always-light (MIN/MAX_VALUE) | `[DEVIATION]` | ORG L201-202 resets to `Integer.MIN_VALUE/MAX_VALUE` when the GUI omits the key (empty field); PORT L86-87 keeps the previous value when the key is absent |
| GUI: color/label/channel/min/max/active/polling | MATCH | ORG `GUIScreenRBMKIndicator.java` vs PORT `GUIRBMKIndicator.java:105-112` (same field set) |
| `invert` toggle | `[EXTRA]` | PORT-only per-unit invert flag + GUI checkbox (`RBMKIndicatorBlockEntity.java:22`, `GUIRBMKIndicator.java:63-65,111`), applied on top of CE's result (PORT L63-77 javadoc admits it) |
| OC component `rbmk_indicator` | `[ABSENT]` | ORG L206-282 |

Findings: **I-DEV-1** `[DEVIATION]` lamp state never reaches the client (PORT
`RBMKIndicatorBlockEntity.java:101-110` missing `state` vs ORG `TileEntityRBMKIndicator.java:163,174`
— the device's whole purpose is the lit lamp, so this is the most serious panel defect).
**I-DEV-2** `[DEVIATION]` default `max=1.0` vs `100` (PORT L35 vs ORG L91).
**I-DEV-3** `[DEVIATION]` freshness/polling root D1 (PORT L54-60 vs ORG L112-121).
**I-DEV-4** `[DEVIATION]` absent-key min/max reset semantics (ORG L201-202).
**I-EXTRA-1** `[EXTRA]` invert toggle.

**Verdict: MISMATCH** — the lamp never visibly lights in world.

---

## 8. rbmk_key_pad (4 buttons)

Original: `TileEntityRBMKKeyPad.java` (KeyUnit defaults L99-103, click L106-119, update L121-133,
`canSend` L135-137, NBT L159-177 incl. `isPressed`, receiveControl L189-205), block
`RBMKKeyPad.java:32-55` (2×2 quadrant hit-test using stored facing meta, edge/face exclusion),
`RenderRBMKKeyPad.java` (pressed glow + button offset L38-58, clickTimer visual).
Port: `RBMKKeyPadBlockEntity.java`, `ModBlocks.java:2438-2446` (primaryClick), keypad hook
`MachineRBMKPanelRenderers.java:352-385`, `RBMKPanelDeviceBlock.java:92-104`.

| Topic | Verdict | Evidence |
|---|---|---|
| Non-polling click: send once, stay lit 7 ticks (`clickTimer`), pitch 1.0 | MATCH | ORG L109-118 vs PORT L90-101 |
| Polling click: latch toggle, re-broadcast every tick while pressed | MATCH | ORG L113-116 + L124-126 vs PORT L94-97 + L63-65 |
| Colors 0xff0000/0xffff00/0x0080ff/0x00ff00, "Button N" | MATCH | ORG L99-103 vs PORT L33-42, L35 |
| Send guard: non-empty channel AND non-empty command | MATCH (`[DEVIATION]` in default) | ORG `canSend` L135-137 vs PORT `broadcast` L104-110; but PORT seeds `command = "1"` (L53, L143) where ORG seeds `""` — a keypad with a channel set but empty command broadcasts `"1"` in the port, nothing in the original |
| Click sound | `[COSMETIC]` | ORG `random.click` 1.0/0.75 pitch vs PORT `STONE_BUTTON_CLICK_ON` same pitch logic (L99-100) |
| Pressed state visible in world | **`[DEVIATION]`** | ORG syncs `isPressed` every tick (L142, L152) — button offset + fullbright glow; PORT `writeNbtData` (L128-136) omits `isPressed` and the client never learns it → the 7-tick press animation/glow never renders (`renderKeyPad` reads `be.isPressed[i]`, `MachineRBMKPanelRenderers.java:362-375`) |
| Quadrant → unit mapping | `[DEVIATION]` | ORG `RBMKKeyPad.java:41-50` maps left/right using the stored facing meta (4 branches) and excludes edge hits and top/bottom faces (L36); PORT `unitFromHit` (L73-79) uses raw world X/Y with no facing and no exclusions → left/right halves are mirrored on WEST/EAST-facing panels, and top/bottom-face clicks trigger a button |
| GUI: color/label/channel/command/active/polling | MATCH | ORG `GUIScreenRBMKKeyPad.java:28-75,128-147` vs PORT `GUIRBMKKeyPad.java:91-96` (`cmd` key renamed to `command+i` — `[COSMETIC]`) |
| OC component `rbmk_keypad` (pressKey/getKeyPressed) | `[ABSENT]` | ORG L207-308 |

Findings: **K-DEV-1** `[DEVIATION]` pressed state not synced (PORT L128-136 vs ORG L142/152).
**K-DEV-2** `[DEVIATION]` hit-test not facing-aware, no edge/face exclusion (PORT L73-79 vs ORG
`RBMKKeyPad.java:36-50`). **K-DEV-3** `[DEVIATION]` command default `"1"` vs `""` (PORT L53/L143
vs ORG L92 + `canSend` L135-137). **K-DEV-4** freshness/polling root D1 does not apply here
(keypad only sends) — n/a.

**Verdict: MINOR** — 3 DEVIATION findings, 1 ABSENT (OC).

---

## 9. rbmk_lever (2 flip switches)

Original: `TileEntityRBMKLever.java` (`FLIP_SPEED = 1/10` L83, click L90-98, update L100-152:
polling broadcast at rest L106-109, non-polling broadcast **on arrival** L112-131, LEVER_START on
click / LEVER_STOP + SPARK + tau arc flash on arrival L134-151, turnProgress client interp
L154-164, NBT L189-209 incl. `isTurningOn`/`flipProgress`), block `RBMKLever.java:32-53`,
`RenderRBMKLever.java` (−180°·progress L44-50).
Port: `RBMKLeverBlockEntity.java`, `ModBlocks.java:2427-2436`, lever hook
`MachineRBMKPanelRenderers.java:318-348`.

| Topic | Verdict | Evidence |
|---|---|---|
| Two commands (on/off), `canSend`-style blank skip | MATCH | ORG L166 vs PORT `send` L65-69 |
| Polling: re-send at whichever end the lever rests at | MATCH | ORG L106-109 vs PORT L53-56 |
| Non-polling: broadcast timing | `[DEVIATION]` | ORG broadcasts `commandOn` only when `flipProgress` **reaches 1** and `commandOff` when it **returns to 0** (L112-131; ~0.5 s after the click, after any mid-flip reversal); PORT `flipLever` broadcasts `state ? on : off` **immediately on click** (L100-104), even mid-flip, and never on animation completion |
| Flip animation | `[DEVIATION]` | ORG 0.1/tick (0.5 s), synced (`flipProgress` L171/181-186, `turnProgress` interp) and rendered; PORT `FLIP_SPEED = 0.2` (L25) animates server-side only — `writeNbtData` (L128-137) omits `flip`/`prevFlip` and `readNbtData` forces `flip = state?1:0` (L147), so the in-world lever snaps instantly instead of animating |
| Sounds / arc flash | `[ABSENT]` + `[DEVIATION]` | ORG LEVER_START on click, LEVER_STOP + SPARK on arrival, 2× tau `AuxParticlePacketNT` arc flash (L94, L118, L130, L134-151, `NTMSounds.java:108-109`); PORT plays a single vanilla `LEVER_CLICK` on click (L98) and emits no particles |
| Mid-flip click behavior | `[DEVIATION]` | ORG toggles `isTurningOn` (reverses direction, sends nothing until it lands); PORT flips `state` and broadcasts at once (L92-105) |
| Quadrant → unit mapping | `[DEVIATION]` (documented) | ORG `RBMKLever.java:41-50` splits left/right by stored facing meta; PORT `unitFromHit` derives the tangent from the clicked face (`L82-90`, javadoc admits the deviation) — correct per clicked side but not the same table |
| NBT | `[COSMETIC]`/`[DEVIATION]` | ORG persists `isTurningOn`+`flipProgress`; PORT persists `state` and reconstructs `flip` (mid-flip not preserved) — PORT L128-149 vs ORG L189-209 |
| GUI: channel/label/cmdOn/cmdOff/active/polling | MATCH | ORG `GUIScreenRBMKLever.java:53-65,118-137` vs PORT `GUIRBMKLever.java:95-100` (`lpolling` key — `[COSMETIC]`); ORG GUI arrays sized 4 but only 2 used (dead entries L28-31) — PORT uses 2 `[GLUE]` |
| OC component `rbmk_lever` | `[ABSENT]` | ORG L241-284 |

Findings: **L-DEV-1** `[DEVIATION]` broadcast at click instead of at animation end; no mid-flip
hold (PORT L92-105 vs ORG L112-131). **L-DEV-2** `[DEVIATION]` in-world animation snaps (PORT
L128-149 vs ORG L171, L189-209). **L-DEV-3** `[DEVIATION]` flip speed 0.2 vs 0.1 (PORT L25 vs ORG
L83) — moot client-side given L-DEV-2 but wrong on the server clock. **L-ABSENT-1** `[ABSENT]`
lever start/stop sounds + spark arc-flash tau particles (ORG L94, L118, L130, L134-151).

**Verdict: MINOR** — commands still fire correctly on both edges; timing and feedback diverge.

---

## 10. rbmk_numitron (2 seven-segment readouts)

Original: `TileEntityRBMKNumitron.java` (DisplayUnit defaults L98-103 — digits mask `0b01111111`,
shorten true, leading zeroes true; update L105-122 incl. freshness L112; NBT L146-166;
receiveControl L178-196; GUI = `GUIScreenRBMKDisplay` L170), `RenderRBMKNumitron.java`
(clamp/overflow L59-73, negative zero-padding L77-84, digit mask walk L88-115, char→UV table).
Port: `RBMKNumitronBlockEntity.java`, numitron hook `MachineRBMKPanelRenderers.java:387-490`,
GUI `GUIRBMKNumitron.java`.

| Topic | Verdict | Evidence |
|---|---|---|
| 2 units; value from numeric RTTY channel; polling → 0 on silence | `[DEVIATION]` (root D1) | ORG L105-122 vs PORT L42-55 — same freshness/parse-fallback gap as the gauge |
| Defaults (mask, shorten, leading zeroes, "Display N" → "Numitron N") | MATCH (`[COSMETIC]` label) | ORG L98-103 vs PORT L32-39 |
| Rendering: 7 digits, mask `0x40>>j`, SI shorten, raw clamp ±9999999/−999999, pad after minus sign, char→UV incl. k/M/G/T/P/E | MATCH | ORG `RenderRBMKNumitron.java:53-115` vs PORT `numitronFormat` L458-480 + `numitronCell` L440-452 (same table; unknown char falls back to '-' like ORG L109) |
| GUI toggles shorten/leading-zeroes | MATCH | ORG `GUIScreenRBMKDisplay.java:82-141` vs PORT `GUIRBMKNumitron.java:71-89,130-136` |
| Digit-mask editing in GUI | `[EXTRA]` | ORG exposes `active_digits` only via OC (`setDisplayActiveDigits` L283-293); PORT adds 7 per-tube toggle buttons (`GUIRBMKNumitron.java:93-103`) |
| NBT keys | `[COSMETIC]` | ORG `shorten_number0/active_digits0/leading_zeroes0/...` vs PORT `short0/digits0/zeroes0/...` (PORT L81-91) |
| OC component `rbmk_numitron` | `[ABSENT]` | ORG L198-304 |

**Verdict: MINOR** — 1 DEVIATION (freshness/polling root D1), 1 EXTRA (digit toggles), 1 ABSENT (OC).

---

## 11. rbmk_terminal (17-line console)

Original: `TileEntityRBMKTerminal.java` (repeat broadcast L45-46, `eval` L52-123: `chan`/`chan <x>`
L64-76, `start` L78-83, `stop` L85-90, `send` L92-100 incl. empty-payload send, `horse` L102-105,
`selfdestruct` L107-115, `clear` L117-120; `push` L125-130 loop from `length-2`; NBT L147-167 with
`" "` blank-trick; ROR L254-294; ocMode L40-43, L55-59, L186-252), `GUIScreenRBMKTerminal.java`
(help text L45-51, in-world working line via `getWorkingLine` L79-89, invalid-terminal close L39-43),
`RenderRBMKTerminal.java` (18 lines, `> ` prefix, width clamp 172, amber when repeating L58-91).
Port: `RBMKTerminalBlockEntity.java`, `GUIRBMKTerminal.java`, terminal hook
`MachineRBMKPanelRenderers.java:563-612`.

| Topic | Verdict | Evidence |
|---|---|---|
| History: 17 lines, shift-and-insert, newest first | MATCH (`[DEVIATION]` in loop) | ORG `push` L125-130 starts at `length-2` (bottom line never scrolls — original quirk); PORT `pushHistory` L46-49 starts at `length-1` and documents the deliberate fix |
| Repeat broadcast every tick while started | MATCH | ORG L45-46 vs PORT L57-61 |
| `chan <x>` set / bare `chan` clears | MATCH | ORG L64-76 vs PORT L122-127 (split-based arg ≈ substring for single spaces — `[COSMETIC]`) |
| `send <v>` | `[DEVIATION]` | ORG broadcasts `cmd.substring(5)` verbatim, including `""` (L92-99); PORT substitutes `"1"` for an empty arg (`RBMKTerminalBlockEntity.java:130`) |
| `start <v>` | `[DEVIATION]` | ORG records `repeatCmd` even with no channel and broadcasts once a channel is set later (L78-83 + L45-46); PORT rejects `start` when the channel is empty (L133-138) |
| `stop` | MATCH | ORG L85-90 vs PORT L139-142; `doesRepeat` drives amber text both sides (ORG serialize L135 vs PORT L164) |
| `clear` | MATCH | ORG L117-120 (push-then-wipe nets to empty history) vs PORT L144-147 + push-skip L160 |
| `horse` | MATCH | ORG L102-105 vs PORT L148 |
| `selfdestruct` | **`[DEVIATION]`** | ORG: destroy block, then `ExplosionVNT(5)` with **no block processor** (no terrain damage), `EntityProcessorCrossSmooth(1,50).setupPiercing(5F,0.5F)`, `PlayerProcessorStandard`, `ExplosionEffectWeapon(10,2.5F,1F)` (L107-115); PORT uses vanilla `level.explode(..., 5.0F, Level.ExplosionInteraction.BLOCK)` (L149-153) — terrain-damaging, no piercing, no weapon SFX. The faithful port already exists as `RTTYNetwork.selfDestruct` (L115-122) but is not called here |
| Empty input line | `[DEVIATION]` | ORG pushes a blank line into history (L61-62 push before the isEmpty check); PORT returns without pushing (L114-117) |
| GUI | `[GLUE]`/`[COSMETIC]` | ORG: full-screen transparent overlay, help text, typed line echoed **in world** with blinking cursor (`GUIScreenRBMKTerminal.java:45-51`, `RenderRBMKTerminal.java:52-56`); PORT: vanilla EditBox screen with local echo + BE scrollback (`GUIRBMKTerminal.java:60-116`); in-world working line is never drawn (PORT hook renders `""` for line 0, L587) |
| Redstone-over-Radio functions (`FUN:clear/write/set<n>/submit`) | MATCH | ORG `getFunctionInfo` L254-263 + `runRORFunction` L264-294 vs PORT L65-110 — same 4 functions, same `set` parse range 1..17 |
| Control packet | MATCH + `[EXTRA]` | ORG `receiveControl` only handles `cmd` (L175-180); PORT also accepts a `channel` key (L181-182) |
| NBT | `[COSMETIC]` | ORG writes `" "` for blanks and strips it on load (L150-157, L163-166); PORT stores `""` directly; channel/running/doesRepeat/history keys match |
| ocMode + OC callbacks (`write/writeln/readInput/getAllHistory/clearScreen/enableOCMode`) | `[ABSENT]` | ORG L186-252; no OC in port, documented L22-23 |

Findings: **T-DEV-1** `[DEVIATION]` selfdestruct explosion type (PORT L149-153 vs ORG L107-115;
cf. `RTTYNetwork.selfDestruct` L115-122 which is the correct implementation). **T-DEV-2**
`[DEVIATION]` `send ""` becomes `"1"` (PORT L130 vs ORG L97). **T-DEV-3** `[DEVIATION]` `start`
without channel rejected (PORT L133-138 vs ORG L78-83). **T-DEV-4** `[DEVIATION]` push loop
diverges deliberately (PORT L46-49 vs ORG L125-130) and empty lines no longer echo.
**T-GLUE-1** in-world working line/cursor not rendered. **T-EXTRA-1** `channel` control key.

**Verdict: MINOR** — core command set and ROR surface match; selfdestruct and empty-payload
semantics diverge.

---

## 12. Summary

| Matrix id | Verdict | DEVIATION | ABSENT | EXTRA | Most important finding |
|---|---|---|---|---|---|
| rbmk_display | **MATCH** | 0 | 0 | 1 | columns persisted as `[EXTRA]`; everything else 1:1 |
| rbmk_gauge | **MINOR** | 2 | 1 | 0 | no RTTY freshness check → polling gauge never zeros on a dead channel |
| rbmk_graph | **MINOR** | 2 | 1 | 0 | same freshness root; min>max swap missing |
| rbmk_indicator | **MISMATCH** | 4 | 1 | 1 | lamp state (`light`) is never synced to the client — lamps never light in world |
| rbmk_key_pad | **MINOR** | 3 | 1 | 0 | pressed state never synced — press glow invisible; hit-test ignores facing |
| rbmk_lever | **MINOR** | 3 | 1 | 0 | broadcast fires at click instead of at end of flip; no arc flash/sounds; in-world animation snaps |
| rbmk_numitron | **MINOR** | 1 | 1 | 1 | freshness/polling gap only |
| rbmk_terminal | **MINOR** | 4 | 1 | 1 | selfdestruct uses a terrain-damaging vanilla explosion instead of the no-block-damage ExplosionVNT |

Cross-cutting: **D13** — no `hasPermission` (15-block) check on control packets (all 7 config
GUIs + terminal commands are usable from any distance); **A1** — OpenComputers integration absent
(scope decision, documented); RTTY bus itself (`RTTYNetwork`) is a faithful port.

Totals: **15 DEVIATION** findings, **7 ABSENT** (6× OC component + 1 lever arc-flash/sound set),
**4 EXTRA**.

### Highest-priority fixes
1. **I-DEV-1**: add `state[]`/`light` to `RBMKIndicatorBlockEntity.writeNbtData` (and `isPressed`
   for the keypad, `flip` for the lever) so per-tick panel visuals reach the client — these already
   ride the every-tick `baseTick` update.
2. **D1**: restore the ORG listener freshness rule (`timeStamp < gameTime - 1 → treat as no
   signal`) in gauge/graph/indicator/numitron `onPanelTick` so `polling` zeros on silent channels.
3. **T-DEV-1**: make the terminal's `selfdestruct` call the already-faithful
   `RTTYNetwork.selfDestruct` instead of vanilla `level.explode(..., BLOCK)`.
