# blast_door (Бронированная дверь / Sliding Blast Door) — port report

**Status: PORTED** — judged COMPLETE by the acceptance judge after 3 verification rounds, and re-confirmed COMPLETE by a fresh independent parity re-audit on 2026-10-08 (see "Fresh parity re-audit" below).
**Mode: full port** — no blast_door Java existed in Modernized; only some assets (textures under `textures/block/`, incl. the `textures/block/doors/blast_door_*.png` set and the old placeholder `textures/block/blast_door.png`) were already present — every model file (OBJ/MTL/JSON) was created by the port. The port replaced the old decorative placeholder block `ModBlocks.BLAST_DOOR` (a plain stone cube) with the real machine under the same registry name `blast_door`.

Source of truth: NTM NEXT (`C:\Projects\ntm-next`, neoforge tree): `blocks/machine/BlastDoor.java`, `tileentity/machine/BlockEntityBlastDoor.java`, `client/render/RenderBlastDoor.java` (+ legacy `RenderSlidingBlastDoor` / flywheel `BlastDoorVisual`, not ported — Modernized's render pipeline replaces them). 1.7.10 reference used only for doubt resolution (`C:\Projects\Hbm-s-Nuclear-Tech-GIT`).

## What the machine is

A 1×7 vertical multiblock door: controller core at y0 + 5 moving leaf cells (+1..+5) + a static housing cell (+6, never retracts). 100-tick travel with 20-tick per-cell stagger (opening frees cell k+1 at tick 20k; closing re-solidifies in reverse). Rising-edge redstone (signal at core OR core.above(6)) toggles it unless locked; right-click toggles (shift passes; `ItemLock`/key_kit pass through); `IBomb` detonators toggle it unless locked (`ERROR_INCOMPATIBLE`). Toggling BFS-spreads to horizontal neighbor blast doors, skipping doors locked with different pins. Locking one door recursively locks adjacent unlocked blast doors with the same pins/mod. Sounds: `reactor_start` (pitch 0.75) at travel start, `reactor_stop` (pitch 1.0) at arrival, vol 0.5, BLOCKS. Rendered from the controller as OBJ parts (base, block, tooth, slider1..4) translated by `rise = 5 * openTicks/100`.

## Architecture decision

The port reuses the existing door framework (`DoorBlock` + `DoorBlockEntity` + `DoorDecl`/`DoorDeclRegistry` + `MachineDoorRenderer`), which already hosts 13 other doors, rather than porting NEXT's `BlockMultiblockCore`/`BlockMultiblockGeometryCell` classes (which do not exist here). NEXT's "geometry cell" blocks become the framework's phantom `UniversalMachinePartBlock` cells driven by the decl's structure patterns and retraction ranges (`getDoorOpenRanges {0,1,0,5,6,1}`, threshold `j/5`, collision progress remap `+1/100` with the click-tick guard — verified tick-exact against NEXT's due-tick tables by numeric simulation). The genuinely new mechanism — BFS cascade across door controllers — lives in `BlastDoorBlockEntity` as a line-by-line port of NEXT's `spread()`. Per-tick sync-unit streaming (NEXT units 4/5/6) is replaced by the framework's transition-sync + client-side interpolation (documented deviation; server collision is authoritative so no state can desync).

## Files touched

New (Java):
- `src/main/java/com/hbm_m/block/decorations/BlastDoorBlock.java`
- `src/main/java/com/hbm_m/block/entity/doors/BlastDoorBlockEntity.java`
- `src/main/java/com/hbm_m/block/entity/doors/BlastDoorDecl.java`

New (resources): `models/block/doors/blast_door.obj` + `.mtl` + `.json` + `_config.json` (merged from the 4 NEXT OBJ sources, cargo_door material convention), `sounds/block/reactorstart.ogg` + `reactorstop.ogg` (byte-identical copies from NEXT).

Modified (shared files, targeted edits):
- `ModBlocks.java` (placeholder deco block replaced by `BLAST_DOOR` → `BlastDoorBlock`, door props 10/1000, METAL, noOcclusion, dynamicShape), `ModItems.java` (`BLAST_DOOR` DoorBlockItem), `ModBlockEntities.java` (`DOOR_ENTITY` valid-blocks += BLAST_DOOR)
- `DoorDeclRegistry.java` (register `blast_door`), `DoorDecl.java` (+9-line BE-aware `doesRender(be, …)` overload), `DoorBlockEntity.java` (`setState` private→protected)
- `MachineDoorRenderer.java` (DOOR_DECLS += BlastDoorDecl, door type key, BE-aware doesRender), `DoorModelRegistry.java`, `GUIDoorModelSelection.java` (blast_door case), `CreativeModeTabEventHandler.java` (2 entries → `ModItems.BLAST_DOOR`, machines tab order matches NEXT: vault_door → blast_door)
- `UniversalMachinePartBlock.java`, `ScrewdriverInteractionHandler.java`, `ScrewdriverItem.java` (screwdriver click paths exclude `BlastDoorBlockEntity` so the screwdriver toggles the door like NEXT — no skin GUI)
- `ModSounds.java` + `sounds.json` (`reactor_start`/`reactor_stop`, exact NEXT ids/entries)
- Datagen: `ModBlockStateProvider`, `ModItemModelProvider`, `AssemblerRecipeGenerator` (recipe 12×steel + 6×lead plate + 2×rubber + 8×HS-bolt + 1×motor, 200t, 100p — field-for-field = NEXT `ass.blastdoor.json`), `ModLanguageProviderEn`/`Ru` (block name + `door.skin.hbm_m.blast_door.default`)
- Generated (via runData): blockstate (128 variants → `hbm_m:block/doors/blast_door`), item model, loot table (self-drop), mineable/pickaxe tag, assembler recipe JSON, lang keys in `en_us.json`/`ru_ru.json`

## Verification evidence

- Repo baseline before work: `gradlew :1.20.1-forge:compileJava` exit 0, `:1.21.1-neoforge:compileJava` exit 0.
- Workflow compile gates (world.run, exit-code-decided): initial gate and post-fix gate both ended **"1.20.1-forge and 1.21.1-neoforge compileJava both exited 0"**.
- Porter ran `:1.20.1-forge:runData` (exit 0); regenerated lang verified to contain the new keys.
- Verification round 1 found 11 issues → fixed; round 2 re-verified everything statically (including a Python numeric simulation of all 10 per-cell flip ticks in both directions — exact match with NEXT) and found the OBJ material defect (see below) → fixed; round 3 fresh verifiers re-checked logic 1:1 vs NEXT line-by-line, resources (ogg md5-identical to NEXT, sounds.json 1:1, recipe field-for-field, JSON parse of all assets, OBJ geometry = exact sums of the 4 NEXT sources with valid indices), scope discipline (every `git status` entry classified; barrel_steel worker's in-flight files untouched), and multiloader compliance (no loader imports outside `com.hbm_m.platform`, FQNs inside stonecutter gates, no fabric).
- Acceptance judge (round 3) personally re-read the ported BE/block/decl wiring against NEXT sources and returned **COMPLETE**.

## Discrepancies found and their resolution

Fixed during the rounds:
1. **HIGH — door would have rendered invisible**: merged `blast_door.obj` initially had no `mtllib`/`usemtl`, and both loaders skip material-less meshes (0 baked quads for all 7 parts). Fixed by adding `mtllib` + per-group `usemtl` + `blast_door.mtl` (`map_Kd #default` per the cargo_door convention); re-validated by script in round 3.
2. **MEDIUM — screwdriver opened the door-skin GUI instead of toggling**: all three click paths (`ScrewdriverInteractionHandler`, `ScrewdriverItem.useOn`, `UniversalMachinePartBlock`) now exclude `BlastDoorBlockEntity`, matching NEXT where a screwdriver click toggles.
3. Missing `door.skin.hbm_m.blast_door.default` lang key in both providers and generated lang → added (En "Standard" / Ru "Стандартная").
4. Porter summary under-reported two files (screwdriver edits) — reporting gap only; code was correct.

Accepted deviations (documented, not regressions):
- **Radiation sealing while closed (medium, NEXT-only)**: NEXT seals the column against its radiation system while closed. Modernized has **no per-block rad-resistance consumer at all** (radiation package is chunk-contamination only), so this cannot be expressed yet; the `DoorBlock.OPEN` hook remains in place. On record for when radiation shielding lands.
- Blast resistance 10/1000 (1.7.10 value, house convention for all 14 doors) vs NEXT's 10/600.
- Client sync is transition-based + interpolated (framework-wide) vs NEXT's per-tick unit stream; ≤1-tick visual phase difference under packet loss; server collision authoritative.
- Framework-wide cosmetic deltas inherited from shared lock code: `ItemLock` on locked target returns PASS vs FAIL; `LOCK_HANG` at block pos/BLOCKS vs player pos/PLAYERS; pick sounds PLAYERS vs BLOCKS; key_kit works on a cell click (resolves to core) vs NEXT core-only; absent-key NBT defaults for `cheesable`/`lockMod` differ (only reachable via hand-crafted NBT); detonator returns `ERROR_NO_BOMB` vs UNDEFINED for no-BE targets.
- Placement facing uses the house-wide `getOpposite()` convention (all 13 doors) rather than NEXT's raw facing; per-facing blockstate rotations are delegated to the shared door render pipeline (NEXT's nonstandard y-rotations are not byte-reproduced).
- Selection outline is the unified whole-door silhouette (framework-wide) vs NEXT's per-cell selection; collision itself is exact per-cell.
- Item model parents the block model (house pattern) vs NEXT's bespoke item display; slider group drawn 4× coplanar (visually identical).
- `STEP_TICKS`/`TRAVEL_TICKS` constants re-exported for documentation have no runtime readers (traveled via decl thresholds); placeholder texture `textures/block/blast_door.png` is now unreferenced (left on disk).

## Fresh parity re-audit (2026-10-08, second session)

The orchestrator re-opened the matrix entry to IN_PROGRESS and this session re-audited the committed port (`fbfed1fe`) from scratch with fresh eyes: two scouts (NEXT contract + Modernized tree), three parallel audit slices (gameplay logic, integration/assets, report-vs-reality drift), then compile gates, a fresh independent re-verification round, and an acceptance judge. **Verdict: COMPLETE after 1 round.** Every gameplay behavior was re-confirmed against NEXT within the documented deviations — state machine, exact per-cell tick timing (an independent Python simulation matched all 10 flip transitions tick-exact in both directions), redstone edge behavior, player/lock/pick interactions, BFS cascade, `IBomb`, NBT, sounds (ogg md5-identical to NEXT), per-cell collision, placement/breaking; all five previously-fixed items (OBJ materials, screwdriver 3-path exclusion, skin lang key, sound registration, recipe) verified intact in the committed tree.

Compile gate (world.run, exit-code-decided): **"1.20.1-forge and 1.21.1-neoforge compileJava both exited 0"**.

Findings: **6 actionable, all documentation/tracking drift — zero gameplay-code discrepancies.**

1. **Matrix status drift (medium, tracking only — root cause)**: the parallel barrel_steel worker's commit `3db888fe` swept the orchestrator's uncommitted working-tree flip (`blast_door` PORTED→IN_PROGRESS) into its own commit alongside its `barrel_steel` IN_PROGRESS→PORTED flip; the report was accurate when committed in `fbfed1fe` and no blast_door code changed `fbfed1fe..HEAD` (verified by git forensics). → Fixed: `blast_door` row restored to PORTED (uncommitted; orchestrator owns git).
2. **Report door-count error (low)**: "already hosts 12 other doors" contradicted `DoorDeclRegistry` (13 pre-existing decls, 14 incl. blast_door; the report itself said "all 14 doors" elsewhere). → Fixed to 13.
3. **Report mode-claim error (low)**: "only some assets (textures …, model JSONs) were already present" was half wrong — all four blast_door model files were created by the port; only textures pre-existed. → Fixed.

Two further low items were duplicates of #2 (drift-auditor's independent copy) or confirmed the same facts; one cosmetic residue was **left as accepted**: the stale class javadoc at `MachineDoorRenderer.java:62` still says "all 13 DoorDecl" (the array holds 14 after this port) — comment-only, code behavior unaffected; editing shared Java after the verdict would have invalidated the compile-gate evidence for zero functional gain.

One new non-actionable observation placed on record: **mid-travel mob pathfinding lags the collision** — vanilla `isPathfindable` for the phantom cells reads the `PASSABLE` blockstate (and for the controller the `OPEN` state), which only flips at arrival, while per-cell collision updates every tick; NEXT's flipped per-cell `open` blockstates made pathfinding track the opening per cell. Framework-wide (all 14 doors), pre-existing, collision parity unaffected.

## Not covered / residual risk

- **No in-game session**: no runClient/runServer, no GameTests, no screenshot verification. The static checks predict correct behavior (tick tables derived numerically; render math re-derived and compared symbolically), but the planned in-game checks remain open: bake-log (expect no "baked with 0 quads" warnings now that materials exist), visual facing in all four orientations, mid-travel visuals, live BFS/lock behavior.
- Runtime BFS cascade, redstone edge behavior, and lock-pick flows were verified by line-by-line code comparison only, not executed.
