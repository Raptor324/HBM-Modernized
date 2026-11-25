# launchpad_soyuz (Soyuz Launch Pad) — port report

**Status: PORTED (1:1)** — the full pad machine (TE state machine, container, GUI, renderer,
sounds, control packet) ported side-by-side against the 1.7.10 original. The companion legacy
machine `soyuz_launcher` (Soyuz Launch Platform, `@Deprecated` tower table) was re-verified 1:1
in the same pass; its existing port needed no code changes.

- Date: 2026-11-22
- Mode: **port + cross-check** (full machine port from `Hbm-s-Nuclear-Tech-GIT`, every constant
  and method flow diffed against the original file)
- Original sources:
  - `com/hbm/tileentity/machine/TileEntityLaunchpadSoyuz.java` (819 L)
  - `com/hbm/inventory/container/ContainerLaunchpadSoyuz.java`
  - `com/hbm/inventory/gui/GUILaunchpadSoyuz.java`
  - `com/hbm/render/tileentity/RenderLaunchpadSoyuz.java` + `render/util/SoyuzPronter.java`
  - `com/hbm/blocks/machine/LaunchpadSoyuz.java` (structure — already ported)
  - `com/hbm/entity/missile/EntityRocketSoyuz.java` + `EntityRocketBase.java`
  - legacy machine: `TileEntitySoyuzLauncher` / `ContainerSoyuzLauncher` / `GUISoyuzLauncher` / `SoyuzLauncher.java`
- Port files (new/rewritten):
  - `blockentity/machines/LaunchpadSoyuzBlockEntity.java` — full TE port (was a transitional
    subclass stub sharing the legacy logic; now a sibling of `SoyuzLauncherBlockEntity`, mirroring
    the original class structure)
  - `inventory/menu/LaunchpadSoyuzMenu.java`, `inventory/gui/GUILaunchpadSoyuz.java`
  - `network/LaunchpadSoyuzControlPacket.java` (typed replacement of the original generic
    `NBTControlPacket {cargo, launch}`)
  - `client/render/implementations/LaunchpadSoyuzRenderer.java` (full animation spec,
    replaces a static-parts-only stub)
  - `client/machine/LaunchpadSoyuzClientTicker.java` (3 looped sounds, `AudioWrapper[3]` port)
  - `models/block/machines/launchpad_soyuz.json` (`machine_parts_loader`, single-OBJ, 25 named
    parts) + assets copied from the original: `models/machines/launchpad_soyuz.obj`,
    `textures/block/machine/launchpad_soyuz.png`, `textures/gui/machine/gui_launchpad_soyuz.png`

## What the pad does (verified 1:1)

- 27 slots (0 rocket, 1 designator, 2 satellite, 3 lander, 4-7 tank items, 8 battery, 9-26 cargo),
  2 tanks KEROSENE_REFORM/OXYGEN 128k, 1 M power, 10 k/tick consumption for the animatics.
- State machine `SoyuzStatus` ABSENT → LOADING → FUELING (15 s) → READY → LAUNCHING (30 s) with
  8 animated components (5 struts ext 4.5/3.0, carriage 19.5, rotor ±180°, tilt), driven by
  `positions[]/speed[]/target[]` exactly like the original (`move()`, `setTarget()`, `updateStates()`).
- 12 port cells (`makeExtra`) subscribe energy + both fluid lines (port `%20` throttle per the
  port's machine convention; original updated its port modules every tick).
- Lift-off spawns `SoyuzEntity` (the port of `EntityRocketSoyuz` — both original machines spawn
  the SAME entity) at `core − dir·4 − rot·4, y+4` with the rocket skin; drains exactly 100 000 of
  each tank; satellite vs cargo payload handling 1:1 (`orbital()` lander logic included).
- NBT: `p/s/t0..7`, all four status enums, `cargoMode`, `fuelCountdown`, `countdown`, `loadedType`
  (the original does not ship `loadedType` in NBT; the port does so the client renderer has the
  skin even before slot sync settles).
- Client: turnProgress interpolation, tower steam particles (3 spawns per tick), rocket-plume
  smoke, 3 looped sounds (`soyuzReady`/`garage_move`/`wgh_big_start`) with moving sound positions
  and one-shot stop sounds (`garage_stop`/`wgh_big_stop`), strut stop sound server-side.
- GUI: pixel-faithful 194x244, status label (`drawConstrainedLabel` port), countdown mm:ss,
  LED/lamp set (`hasJetFuel`/`hasOxidizer`/`power >= CONSUMPTION`, orbital, rocket, satellite,
  designator), launch button only from READY.
- Menu: slot layout identical to `ContainerSoyuzLauncher` (the originals share coordinates);
  shift-click into the rocket slot only.

## Renderer

`MachineRenderers` spec (id `launchpadsoyuz`): static `Launchpad`, 5 struts (`part` + animator),
`Carriage`, 16 `Wheel_i_j` (carriage chain + roll about the vertical axle), `Rotor`, `Mount`
(carriage chain + flip transforms — original GL accumulation reproduced step by step), `Rocket`
as `dynamicPart` from the per-skin `SoyuzRocketBakedModel` (cache key `rocket<skin>`) that rides
the mount when idle and stands world-aligned at `(0,4,0)` during the countdown (counter-rotation
`-R_y(padRotation)` 1:1). Block transform `T(0.5)·R_y(pad)·T(-4,0,-4)` — the original
`renderTileEntityAt` sequence; facing rotation table `S→90/W→270/N→180/E→0` derived from the
original metadata table + `BlockDummyable.onBlockPlacedBy` yaw mapping + the structure helper
mapping, centralized in `MultipartFacingTransforms.launchpadPoseRotationYDegrees`. Item icon:
`itemParts("Launchpad", "Strut1..5")` per the original `ItemRenderBase`.

## Legacy machine `soyuz_launcher` cross-check (no changes needed)

`SoyuzLauncherBlockEntity` vs `TileEntitySoyuzLauncher`: maxPower 1M, KEROSENE/OXYGEN 128k, 27
slots, mode byte, 600-tick countdown, `canLaunch` (including the original's duplicated
`hasRocket()` — kept, commented), `getFuelRequired` (cargo `min(5000+dist, 128k)`), lift-off
position `+0.5/+1`, `getConPos` (13 cells per side at distance 7, core level and one below),
NBT key set, GUI `container.soyuzLauncher`. All 1:1; the earlier temporary protected
type-constructor added to this class was reverted (the pad is a sibling class now, not a subclass).

## Accepted deviations (documented)

1. **Port modules `%20` throttle** instead of per-tick `ModulePortMan` updates — the port's
   established machine convention (no `ModulePortMan` port); original semantics preserved
   (subscription set identical).
2. **ROR (`IRORValueProvider`/`IRORInteractive`) not ported** — the port has no ROR layer;
   `launch()`/`setCargoMode()` remain as the Java API used by the control packet.
3. **`EnumUtil.grabEnumSafely`** replaced by a 3-line private helper (the util class was not
   ported; smallest glue).
4. **Designator read via `IDesignatorItem.getCoords`** instead of raw `stackTagCompound.xCoord`
   (same data, null-safe; same convention as the legacy table port).
5. **Item icon struts offset**: the original `ItemRenderBase` drew the struts displaced
   `+3.25 Z` above the base in the icon; the port icon shows Launchpad + Strut1-5 at their
   natural positions (no per-part item transforms in the pipeline).
6. **`syncIfStateChanged` gate** instead of unconditional `networkPackNT(300)`: the signature
   covers positions/`soyuzStatus`/countdown/`cargoMode`/energy/tank fills/dedicated slots, so
   while animating the pad syncs every tick (as the original did) and stays silent when idle.
7. **`loadedType` also shipped in NBT** (see above) — additive, improves client-side rocket skin.
8. **Loop sound volume ramp** `base·max(0, 1 − d/range)` — matches the established
   `SoyuzReadyLoopSoundFactory` interpretation of the NTM `AudioWrapper` params.

## Verification

- `:1.20.1-forge:compileJava` and `:1.21.1-neoforge:compileJava` — both `BUILD SUCCESSFUL`.
- `:1.20.1-forge:runData` — lang JSONs (container + 4 status keys), blockstate
  (`customMachineBlock` → `block/machines/launchpad_soyuz`), item model via
  `blockItemFromBlockModelMachine` regenerated.
- In-game: runClient smoke test of place → load rocket → deploy → fuel → launch pending.
