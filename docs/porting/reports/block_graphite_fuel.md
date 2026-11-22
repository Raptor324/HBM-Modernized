# block_graphite_fuel — port report

**Machine:** `block_graphite_fuel` — CP-1 uranium fuel block ("Урановый топливный блок CP-1")
**Result:** COMPLETE — judge verdict COMPLETE in verification round 1 (cap 3). Matrix entry set to `PORTED`. Re-audited the same day (run `dwfrun-9dd40d12`): verdict COMPLETE again after 2 parity fixes; matrix entry `PORTED`.
**Run:** dynamic workflow `dwfrun-7d61fcd4` (port), `dwfrun-9dd40d12` (parity audit), branch `machines`, finished 2026-10-09.
**Source:** NTM NEXT `C:\Projects\ntm-next` (neoforge tree): `com/hbm/blocks/machine/pile/BlockGraphiteFuel.java`, `com/hbm/tileentity/machine/pile/BlockEntityPileFuel.java`. 1.7.10 reference: `C:\Projects\Hbm-s-Nuclear-Tech-GIT`.

## Mode

**Full port.** Pre-port probe confirmed Modernized had only a deliberate plain-`Block` stub
(`ModBlocks.BLOCK_GRAPHITE_FUEL` = `new Block(BlockProps.copy(Blocks.STONE))`, comment marks
FUEL/LITHIUM/PLUTONIUM/SOURCE/TRITIUM as stubs awaiting their own ports) plus the two textures
(`block_graphite_fuel.png`, `block_graphite_fuel_aluminum.png`). No fuel Java existed anywhere.
The port builds on the already-ported CP-1 pile infrastructure (BlockGraphiteDrilled lineage,
BlockGraphiteNeutronDetector exemplar, `IPileNeutronReceiver`, RBMK neutron world).

## Architecture (what was built)

Four layers, per plan:

1. **New neutron-transport package `com.hbm_m.handler.neutron`** — 1:1 port of NEXT
   `com.hbm.handler.neutron`: `NeutronNode`, `NeutronStream` (+ `NeutronType`), `NeutronNodeWorld`
   (+ `StreamWorld`), `NeutronHandler`, `PileNeutronHandler` (nested `PileNeutronNode`/`PileNeutronStream`).
   A parallel PILE-typed stream world, deliberately NOT a generalization of
   `com.hbm_m.handler.rbmk.NeutronNodeWorld` (which is concrete-RBMK-typed; touching the RBMK hot
   path would violate the minimal-shared-edits constraint). Documented adaptations: tags move to
   `hbm_m` namespace (`pile/flux_absorber`, `pile/flux_attenuator`); `cleanNodes` + 20-tick
   cacheClear cadence dropped (their only NEXT content prunes RBMK nodes, which live in
   `handler/rbmk` here — PILE nodes are removed eagerly via `BlockEntityPileBase.setRemoved`);
   `removeWorld(Level)` helper added for the Modernized explicit-lifecycle convention.
2. **New plain-BlockEntity CP-1 BE lineage:** `BlockEntityPileBase` (abstract `tickServer` /
   `setRemoved` / `castRay` — distinct from the pre-existing `PileBaseBlockEntity`, which is the
   unrelated 1.7.10 Meiler lineage) and `BlockEntityPileFuel`, following the committed
   `BlockEntityPileNeutronDetector` precedent (plain BE, no client sync, stonecutter-gated NBT).
3. **New block `BlockGraphiteFuel`** extends `BlockGraphiteDrilledTE` with a per-block `getTicker`
   override for the new `PILE_FUEL` type — the base's `getTicker` type-checks only
   `PILE_NEUTRONDETECTOR` and returns null otherwise, so without the override the fuel block would
   silently never tick.
4. **Minimal shared-file edits:** ModBlocks stub swap (FUEL only), ModBlockEntities `PILE_FUEL`
   registration (`"pile_fuel"`), uranium/pu239 fuel-insertion branches in
   `BlockGraphiteDrilled`/`BlockGraphiteDrilledBase.checkInteractions`, MainRegistry
   `SERVER_POST`/`SERVER_LEVEL_UNLOAD`/`SERVER_STOPPED` wiring, creative tab, datagen.

Key dependency mappings: `IToolable`/`ITooling` → version-gated use hook + direct item checks
(house convention); `IInsertable` → existing ported `insertItem`; `IBlowable` → 3-line interface
port + `applyFan` (no dispatcher yet — machine_fan unported; documented scaffold);
`BalanceConfig.enable528` → `GeneralConfig.enable528`; `ValueInput`/`ValueOutput` NBT →
stonecutter-gated `saveAdditional`/`load`; `Level.ExplosionInteraction.BLOCK` explode → identical
call shape already used in `PileCoreBlockEntity`. The progress-completion swap is written
**stub-safe**: `if (target state hasProperty(AXIS)) copyShape else defaultBlockState` — the
plutonium stub lacks AXIS so it swaps plainly today and auto-upgrades to NEXT behavior when the
plutonium block is ported.

Gameplay parity (verified line-for-line vs NEXT): per-tick order dissipate →
checkRedstone(react()) → transmute → overheat → smoke → completion; heat dissipation 5% / 6.5%
shrouded with int compound-assign truncation; `react()` formula + 12× `castRay(max(reaction*0.25,1))`;
maxHeat 1000; maxProgress 50000/75000 gated on `enable528`; PU239 seed at `maxProgress-1000`;
overheat → 4F BLOCK explosion + `GAS_RADON_DENSE`; comparator 15/(max−1000) block-side and 16/max
neighbor-update; ray march range 5, float step 0.5, absorber stop, attenuator ×0.25, rod WITHDRAWN
stop, detector WITHDRAWN pass-through, `contaminate(RADIATION, CREATIVE, flux/4)`; screwdriver
insert (uranium then pu239 order) / extract (ejects `pile_rod_uranium`/`pile_rod_pu239`,
copyShape back to drilled); shroud add/remove; hand-drill readout with verbatim lang values.

## Files touched (22)

New files:
- `src/main/java/com/hbm_m/handler/neutron/NeutronNode.java`
- `src/main/java/com/hbm_m/handler/neutron/NeutronStream.java`
- `src/main/java/com/hbm_m/handler/neutron/NeutronNodeWorld.java`
- `src/main/java/com/hbm_m/handler/neutron/NeutronHandler.java`
- `src/main/java/com/hbm_m/handler/neutron/PileNeutronHandler.java`
- `src/main/java/com/hbm_m/interfaces/IBlowable.java`
- `src/main/java/com/hbm_m/blockentity/machines/pile/BlockEntityPileBase.java`
- `src/main/java/com/hbm_m/blockentity/machines/pile/BlockEntityPileFuel.java`
- `src/main/java/com/hbm_m/block/machines/pile/BlockGraphiteFuel.java`

Edited shared files (targeted):
- `src/main/java/com/hbm_m/block/ModBlocks.java` — BLOCK_GRAPHITE_FUEL stub → real class (sibling stubs untouched)
- `src/main/java/com/hbm_m/blockentity/ModBlockEntities.java` — `PILE_FUEL` registration
- `src/main/java/com/hbm_m/block/machines/pile/BlockGraphiteDrilled.java` — uranium/pu239 insertion branches
- `src/main/java/com/hbm_m/block/machines/pile/BlockGraphiteDrilledBase.java` — same two lines in `checkInteractions`
- `src/main/java/com/hbm_m/main/MainRegistry.java` — neutron handler lifecycle hooks (FQN justified by rbmk import clash)
- `src/main/java/com/hbm_m/main/CreativeModeTabEventHandler.java` — fuel block after detector in pile section
- `src/main/java/com/hbm_m/datagen/assets/ModBlockStateProvider.java` — fuel → `pilePillarBlock(...)`
- `src/main/java/com/hbm_m/datagen/ModBlockLootTableProvider.java` — `dropPileChannel(BLOCK_GRAPHITE_FUEL, null)`
- `src/main/java/com/hbm_m/datagen/assets/ModBlockTagProvider.java` — flux_absorber/flux_attenuator tags
- `src/main/java/com/hbm_m/datagen/ModLanguageProviderEn.java` / `ModLanguageProviderRu.java` — 3 desc keys (+ ru block names)
- `src/generated/resources/assets/hbm_m/lang/en_us.json` / `ru_ru.json` — regenerated (tracked)

Resources: no binary assets copied (textures pre-existed). Datagen run; generated blockstate has
12 variants (pu239 × shrouded × axis) with NEXT's exact rotations — strictly-complete coverage of
NEXT's wildcarded 6; loot table = 2 pools (8× graphite_ingot + shrouded-only shell_aluminum,
both survives_explosion) — namespace-only parity with NEXT.

## Discrepancies found by verification (7: 1 medium, 6 low)

1. **[medium] Bred rays die at the plutonium stub.** In NEXT a neutron ray passes through a
   completed fuel channel (plutonium block carries `BlockEntityPileSource`, a `BlockEntityPileBase`,
   so the ray handler caches a node and skips stop checks); here `BLOCK_GRAPHITE_PLUTONIUM` is the
   ask-mandated plain stub with no BE, so every ray dies at the first bred block via
   `if (tile == null) return;`. Evidence: NEXT `PileNeutronHandler.java:84-90` +
   `BlockEntityPileSource.java:15-22` vs Modernized `PileNeutronHandler.java:90-96` +
   `ModBlocks.java:3382-3383`. Judge ruling: ask-mandated scope cut, not an unaddressed defect —
   documented in-code at `BlockEntityPileFuel.java:84-88`, self-heals (auto-upgrades to `copyShape`)
   when the plutonium port lands.
2. **[low] `needs_iron_tool` gating.** Modernized lists the whole pile family in
   `minecraft:needs_iron_tool` (generated); NEXT has only `mineable/pickaxe`, no `needs_*`.
   Pre-existing family convention, inherited by fuel.
3. **[low] Display names differ.** Modernized "Block Graphite Fuel"/"Block Graphite Plutonium" vs
   NEXT "Pile Fuel (LEGACY)"/"Pile Fuel (Bred) (LEGACY)" (generated `en_us.json:267,269` vs NEXT
   `en_us.json:322,324`).
4. **[low] Flux tags incomplete by unported-block consequence.** `flux_absorber` empty (NEXT:
   `block_boron`, unported), `flux_attenuator` 19/20 (concrete_smooth has no Modernized block);
   zero current gameplay effect, commented in `ModBlockTagProvider.java:340-348`, revisit when
   those blocks land.
5. **[low] `GeneralConfig.enable528` is a hardcoded static** with no config-file wiring, so
   maxProgress is pinned to 50000 (NEXT's is user-toggleable, same default). Default gameplay
   identical; the 75000 branch unreachable.
6. **[low] Block properties construction differs cosmetically** (`BlockProps.copy(IRON_BLOCK)` +
   explicit overrides vs NEXT `Properties.of()` pileProperties); all gameplay-relevant values
   (5.0F/6.0F, METAL, requiresCorrectToolForDrops) match; shared house pattern with siblings.
7. **[low] Pre-existing, porter-unattributed en renames** for detector/drilled siblings in
   `ModLanguageProviderEn.java` leave the pile-family naming mixed vs NEXT. Cosmetic, predates
   this port (file was already modified in the initial git status).

None of the low findings block acceptance; each is documented in code or above.

## Round 2 parity audit (2026-10-09): fixes and rulings

1. **[fixed, was low] Offhand tools dead on 1.21.1.** The 1.21.1 `useWithoutItem` hooks
   hard-coded `InteractionHand.MAIN_HAND`, so rods/screwdrivers/hand drills held only in the
   offhand could not drive insertion, extraction or readout. NEXT dispatches per hand (vanilla
   `useItemOn` order: main first, offhand on PASS). Both 1.21.1 branches (BlockGraphiteFuel,
   BlockGraphiteDrilled) now iterate `InteractionHand.values()` (MAIN_HAND, OFF_HAND) and return
   the first non-PASS result; PASS paths are side-effect-free, so the re-dispatch is inert.
   1.20.1 target unchanged (already had the real hand). The same hardcode remains in sibling
   hooks outside this port's scope (BlockGraphiteNeutronDetector.java:86, PileBlock.java:152,
   PileBrickBlock.java:173, PileDeviceBlock.java:171) — their own audits' to fix.
2. **[fixed, was medium] Flammability.** NEXT sets `FlammableBlocks.set(BLOCK_GRAPHITE_FUEL, 30, 5)`
   → `Services.PLATFORM.setFlammable` → FireBlock odds maps (FlammableBlocks.java:18,
   NeoForgePlatformHelper.java:290-292). Modernized equivalent: `BlockGraphiteFuel` now overrides
   the block-state extension pair — `getFireSpreadSpeed` = encouragement 30, `getFlammability` =
   burn-away odds 5 — which is exactly what vanilla fire queries on both targets
   (1.20.1-forge FireBlock.java:224,262; 1.21.1-neoforge FireBlock.java:263,300; established
   house convention: `BlockCoke.java`). `PlatformHooks.setFlammable` was NOT used because
   `FireBlock#setFlammable` is `private` on 1.20.1-forge (Forge patch moved the odds maps to
   instance fields; FireBlock.java:290) — the literal NEXT idiom would need an AccessTransformer,
   machinery beyond the house convention. Family remainder of NEXT FlammableBlocks.java:15-26
   (block_graphite, _drilled, _rod, _plutonium, _source, _lithium, _tritium, _detector,
   pile_brick — all 30/5) deliberately left for their own ports' scopes per the
   do-not-touch-sibling-blocks constraint.
3. **[accepted deviation, low] Creative tab entry.** Modernized lists the block in
   NTM_MACHINE_TAB (`CreativeModeTabEventHandler.populateMachinesTab`, in-code note
   "NEXT: keine Tab-Zuordnung"); NEXT gives it no creative-tab entry. Kept as the documented
   Modernized-wide acquisition convention (every ported block gets a tab entry);
   creative-menu-only, no gameplay effect.
4. **[fixed, parity] Shared sound bytes + rod-tooltip lang keys (bundled by the audit's coder).**
   The fuel block's screwdriver/shroud interactions play UPGRADE_PLUG (1.0F/1.0F) and
   TECH_BLEEP (1.0F/0.85F) — pitch/volume parity verified — but the `techbleep.ogg` audio
   content itself differed from NEXT (Modernized's pre-port file md5 `118d6048f0d5659d268f53fd7ef5c57a`,
   3336 B vs NEXT `acfc03a83b8db863e20e907efc3ec9a2`, 3256 B). Replaced with NEXT's byte-exact
   file (md5 re-verified post-swap); `upgrade_plug.ogg` confirmed already byte-identical
   (md5 `7ce591640c70abc0cf69b37d6a60f4b5` both sides). Shared-asset consequence: the detector
   report's finding 6 line was corrected in `block_graphite_detector.md` (it claimed "same ogg
   audio" — true for upgrade_plug only); the name deviation there stays accepted. Also added
   NEXT-verbatim tooltip keys in both providers (`desc.item.pileRod`,
   `item.hbm_m.pile_rod_boron.desc`, `item.hbm_m.pile_rod_detector.desc`, +
   `sounds.hbm_m.techbleep` subtitle; ru best-effort — NEXT ships no ru for them); generated
   lang regenerated. Keys render once the ItemPileRod item port lands (in-code comment).

Files touched by the audit session (working tree, uncommitted — orchestrator owns git):
- `src/main/java/com/hbm_m/block/machines/pile/BlockGraphiteFuel.java` — 1.21.1 hand-loop
  dispatch; `getFireSpreadSpeed`/`getFlammability` overrides (30/5)
- `src/main/java/com/hbm_m/block/machines/pile/BlockGraphiteDrilled.java` — 1.21.1 hand-loop dispatch
- `src/main/java/com/hbm_m/interfaces/IPileNeutronReceiver.java` — javadoc implementor list (doc-only)
- `src/main/java/com/hbm_m/datagen/ModLanguageProviderEn.java` / `ModLanguageProviderRu.java` — 4 new keys per side
- `src/generated/resources/assets/hbm_m/lang/en_us.json` / `ru_ru.json` — regenerated (runData)
- `src/main/resources/assets/hbm_m/sounds/techbleep.ogg` — replaced with NEXT's byte-exact file
- `docs/porting/reports/block_graphite_detector.md` — one-line factual correction (finding 6 ogg claim)
- `docs/porting/reports/block_graphite_fuel.md` — this report

## Verification evidence

- **Parity audit re-run (2026-10-09, `dwfrun-9dd40d12`):** 2 researchers (42 NEXT files,
  47 Modernized files) → 2 auditors (logic 22 parity checks, integration 16) → 9 differences
  (7 pre-documented accepted cuts verified intact; 3 open items). Coder resolved the open
  items (fixes 1–2 above; item 3 ruled an accepted deviation) and bundled fix 4. Compile
  gates run by the workflow: `gradlew.bat :1.20.1-forge:compileJava` → exit 0;
  `gradlew.bat :1.21.1-neoforge:compileJava` → exit 0 (judge corroborated via fresh post-fix
  class files in both target build trees and processed stonecutter outputs reflecting the
  fixed code: the hand-loop is ACTIVE on 1.21.1, the real-hand `use(...)` stays ACTIVE on
  1.20.1). Fresh verification round 1: logic slice PASSED (12 checks, 0 issues), integration
  slice PASSED (17 checks, 0 issues, incl. scope discipline via read-only
  `git status`/`git diff --stat`). Judge re-read both codebases line-by-line — including the
  vanilla `FireBlock` mechanics in the real 1.20.1/1.21.1 target sources to confirm the
  flammability override pair is queried exactly where NEXT's map registration is — and ruled
  COMPLETE.
- **Compile gates (deterministic, run by the first workflow via gradle):**
  `gradlew.bat :1.20.1-forge:compileJava` → exit 0;
  `gradlew.bat :1.21.1-neoforge:compileJava` → exit 0. Judge independently re-ran both → exit 0.
- **Datagen:** `gradlew.bat :1.20.1-forge:runData` → BUILD SUCCESSFUL (first attempt failed with
  4 compile errors — public comparator override widening, @Override signature, missing import —
  all fixed; second run green). Generated outputs verified on disk: blockstate (12 variants,
  exact NEXT rotations), block/item models, loot table (2 pools, exact parity), tags
  (pickaxe hit; flux_absorber/attenuator as documented), new lang keys present in BOTH generated
  lang files with verbatim NEXT en values.
- **Stonecutter:** 1.21.1 processed output read directly for both gated files
  (`versions/1.21.1-neoforge/...`): `<1.21.1` NBT branch commented, `HolderLookup.Provider`
  variant active, FQNs inside gates, no loader imports in common code.
- **Verification round 1** (fresh read-only verifiers, path:line evidence both sides):
  logic slice — 22 checks (receive semantics, thresholds/config gate, transmute, overheat,
  comparator, ray march, tick order vs SERVER_POST drain, NBT incl. stonecutter output, ticker
  wiring, screwdriver insert/extract, readout lang, lifecycle); 6 findings (all listed above).
  integration slice — 16 checks (scope discipline via `git status`/`git diff --stat`,
  registrations, blockstate/models/textures, loot, tags, lang, creative tab, handler parity vs
  all 5 NEXT handler files, platform/stonecutter compliance grep, compile corroboration via
  fresh class files in both target build trees); 1 finding (the medium above).
- **Judge:** COMPLETE — personally re-read both codebases side-by-side and re-ran the compile
  gates; confirmed the tick-order parity, the load-bearing per-block `getTicker` override, the
  four remaining pile stubs untouched, and generated-resource parity.
- **Scope:** `git status` at judge time showed only orchestrator-owned
  `docs/porting/machine_porting_matrix.json` modified (port code was committed by the
  orchestrator inside `b47cabe1`, whose message names block_graphite_detector — commit
  organization is orchestrator-owned; fuel attribution is recorded here and in the report).

## Not covered / unverified

- No in-game `runClient`/`runServer` session: runtime behavior verified by line-by-line code
  comparison against NEXT and compilation only. The neutron flux chain
  (fuel `castRay` → `PileNeutronStream` → `NeutronHandler.SERVER_POST` drain →
  `receiveNeutrons` → detector/fuel `tickServer`) is verified statically end-to-end and
  `receiveNeutrons` now has live callers on both sides.
- GameTests were not written or run.
- Particles, comparator readout and the overheat explosion were not screenshot-tested in a
  live world.
- `IBlowable.applyFan` is unreachable scaffold until machine_fan is ported (documented).
