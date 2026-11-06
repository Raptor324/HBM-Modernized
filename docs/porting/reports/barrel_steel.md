# barrel_steel (Fluid Barrel) — port report

**Status: PORTED** — final judge verdict `complete = true`, `blocked = false`. Every actionable discrepancy found by the audit (22 across 4 fix rounds) is fixed; every remaining difference is an accepted, documented waiver. The `machine_porting_matrix.json` flip to `PORTED` is owned by the orchestrator (this report's agent may not edit it; a python extraction this session still read `portStatus = IN_PROGRESS` at report-writing time).

- Date: 2026-10-08
- Mode: **audit** (full-family parity audit + 4 fix rounds + independent verification per round, against NTM-NEXT)
- Matrix entry: `barrel_steel` — category `storage`, mechanics `fluid:in-out, items:in-out, fluid:buffer, redstoneoverradio, comparator, sync:packet`

## Mode used

Audit mode. A partial port already existed from the prior session (see Condensed history); this session re-audited the **whole barrel family from scratch** against NEXT — 19 parity domains re-derived from both source trees (registration/capacities, identifier semantics, pressure semantics, canister slot gates, loader chain, 4 modes, mode-button UI, container layout, pipe network, comparator, ROR, hazards, corroded behavior, NBT/persistence, rendering, recipes/acquisition, barrel_iron consistency, matrix/test wiring, GUI feedback text). The prior session's 4 open items were confirmed as findings K1–K4, six new findings N5–N10 and four waiver candidates W1–W4 were raised. All 10 actionable findings were fixed across 3 rounds, each round independently re-verified by fresh verifiers whose follow-ups were fixed in the next round; a judge pass then prescribed 3 final resolutions (LEAK-ART, INF-PRESSURE, N10 fix-or-waive), all discharged in the final round. The judge then returned `complete = true`.

## Scope + source-of-truth paths

**Family in scope** (six block entities on `BarrelTankBlock`): `barrel_steel` (16,000 mB), `barrel_plastic` (12,000 mB), `barrel_tcalloy` (24,000 mB), `barrel_antimatter` (16,000 mB), `barrel_corroded` (6,000 mB), plus the Modernized-only `barrel_iron` (16,000 mB, no NEXT anchor). Decor barrels (`barrel_yellow/red/lox/taint`) are out of scope. The shared base `MachineFluidTankBlockEntity` also serves the universal 5×3×3 tank and BAT9000; family-split behavior for those is covered where verifier feedback touched it.

**NEXT source of truth**: `C:/Projects/ntm-next/neoforge/src/main/java/com/hbm/...` — key anchors `blocks/machine/storage/BlockFluidBarrel.java`, `tileentity/machine/storage/BlockEntityBarrel.java`, `tileentity/machine/storage/BlockEntityMachineFluidTank.java`, `inventory/fluid/tank/FluidTankNTM.java`, `items/machine/ItemFluidContainerInfinite.java`, `client/gui/ScreenBarrel.java`, `inventory/container/MenuBarrel.java`, `client/render/RenderBarrel.java`, `client/model/BarrelModel.java`, `data/hbm/fluid_property/*.json`, `assets/hbm/lang/en_us.json`. (The NEXT fabric tree is a mirror; not read as separate truth.)

**Modernized working tree**: `src/main/java/com/hbm_m/...` — `block/machines/BarrelTankBlock.java`, `block/machines/MachineFluidTankBlock.java`, `blockentity/machines/MachineFluidTankBlockEntity.java` + six `Barrel*BlockEntity.java`, `inventory/fluid/tank/{FluidTank,FluidLoaderStandard,FluidLoaderFillableItem,FluidLoaderInfinite}.java`, `inventory/gui/{GUIBarrelSteel,GUIBarrelIron,GUIFluidTank}.java`, `client/render/implementations/BarrelTankRenderer.java`, `api/fluids/bootstrap/ModFluidHazardBootstrap.java`, `inventory/fluid/FluidType.java`, `block/ModBlocks.java`, `item/liquids/FluidIdentifierItem.java`, `datagen/ModLanguageProvider{En,Ru}.java`; assets under `src/main/resources/assets/hbm_m/models/block/barrel_*.json` (hand-committed) and `src/generated/resources/...` (datagen-owned).

## Discrepancies found this session, and how each was resolved

22 discrepancies fixed across 4 rounds; 8 waivers accepted/documented. Severity at discovery on the main audit list: 1 high (K1), 3 medium (K2, K3, N5), 6 low (K4, N6–N10), 4 low waiver candidates (W1–W4). Independent verification then surfaced 12 follow-up fixes (4 medium, 8 low) and 4 additional waivers — listed below.

### Fixed — main audit list

**K1 [high] — Barrel inventory silently deleted on block removal; NEXT spills it.**
NEXT: `BlockEntityMachineBase.preRemoveSideEffects` → `spillInventory(pos)` drops every slot (`BlockEntityMachineBase.java:105-118`). Modernized: `BarrelTankBlock.onRemove` (`BarrelTankBlock.java:143-148`) and `MachineFluidTankBlock.onRemove` (`MachineFluidTankBlock.java:170-176`, with a *false* comment at :172 claiming setRemoved drops the inventory) had no drop call; `BaseHbmBlockEntity.dropInventory` had zero callers in the barrel family.
**Fixed (round 1):** both `onRemove` implementations call `tank.dropInventory()` server-side on real block change, before structure teardown/super (`BarrelTankBlock.java:152`, `MachineFluidTankBlock.java:176` — re-grepped this session); the false comment replaced with the real rationale (setRemoved also fires on chunk unload → dupe). Corroded's `dropInventoryOnRemove() = false` override is honored inside `BaseHbmBlockEntity.dropInventory` (`BaseHbmBlockEntity.java:142-143`). **Round-3 fix of a round-1 regression:** the iron→corroded decay path duped the inventory (snapshot → synchronous `onRemove` spill → restore of the pre-spill snapshot); fixed by `savedTag.remove("Inventory")` before `setBlock` (`BarrelIronBlockEntity.java:70` — re-grepped this session), load side proven a no-op on a keyless tag (`PlatformHooks.java:809-818`, `ItemStackSerialization.java:22-28`; final verifier confirmed exactly one tag removal and one drop on the swap).

**K2 [medium] — `FluidLoaderStandard.emptyItem` could void the container's fluid on empty-but-typed tanks.**
NEXT probes acceptance before any real mutation (`FluidTankNTM.java:119-124`, `:374-423`). Modernized's `|| tank.getFill() <= 0` clause (`FluidLoaderStandard.java:28-29`) skipped the substance gate and the `fillMb` result was never checked — foreign fluid extracted into a conformed tank vanished (`FluidTank.java:83-88`, `:115-122`).
**Fixed (round 1, extended round 2):** unconditional `sameSubstance(tank.getTankType(), simEx.fluid())` gate (`FluidLoaderStandard.java:31` — re-grepped this session) + simulate-fill acceptance guard `tank.fillMb(..., true) <= 0 → return false` before any real mutation; round 2 added NEXT's full/partial container slot flow — fully drained/filled container ejects to the OUT slot (abort if it doesn't fit), partial transfer only from a count-1 stack and remains in the IN slot (`FluidLoaderStandard.java:44-59, :93-108` ≡ `FluidTankNTM.java:413-422, :452-460`).

**K3 [medium] — Item/hand display transforms differed numerically from NEXT in every context.**
NEXT: full 9-context display block in `assets/hbm/models/item/barrel_steel.json` (gui [30,-45,0] ×0.625; fixed [0,90,0] ×0.625 trans [0,-0.56,0.5]; thirdperson [70,-45,0]; firstperson [0,45,0]; head [0,90,0]; ground trans [0,-4,0]; `on_shelf` [0,-90,0] ×1.25). Modernized: different numbers and no `on_shelf`; generated item models are bare parent pointers, so block-model display is what ships.
**Fixed (round 1):** NEXT's display block copied verbatim into all six hand-committed block models (`src/main/resources/assets/hbm_m/models/block/barrel_{steel,plastic,tcalloy,antimatter,corroded,iron}.json`; iron reuses NEXT-steel, which has no NEXT counterpart to copy from). Re-verified every round by float-normalized node/python JSON comparison: identical in all 9 contexts for all six, incl. NEXT's float artifacts (e.g. `2.3841858e-7`, `0.37499997`).

**K4 [low] — Extra plain-corrosive tooltip line on tcalloy/antimatter/corroded, where NEXT omits it.**
NEXT desc lines: `en_us.json:3012-3033` + `ModBlocks.java:1895-1943` (tcalloy/antimatter 3 lines, corroded 4 incl. leaky — no plain-corrosive). Modernized: `canStoreCorrosive` was a primitive boolean, line added unconditionally (`BarrelTankBlock.java:84-86, :164`), `ModBlocks.java:1231-1233/1277-1279/1289-1291` passed `true`.
**Fixed (round 1):** `canStoreCorrosive` now `@Nullable Boolean`, null hides the line (`BarrelTankBlock.java:87-88, :176-178`); `null` passed for the three variants; rendered line sets now match NEXT exactly for all five shared variants (verifier-derived per-variant comparisons). **Round-2 follow-up:** capacity grouped (`String.format("%,d", capacity)`, `BarrelTankBlock.java:172-173` ≡ NEXT `BlockFluidBarrel.java:215`). **Round-3 follow-up:** En format `'Capacity: %s mB'` → `'Capacity: %smB'` (`ModLanguageProviderEn.java:2760`, regenerated `en_us.json:5278` — re-grepped this session = NEXT `en_us.json:3025`); Ru keeps `'Объём: %s мБ'` — no NEXT anchor exists (NEXT `ru_ru.json` has no `desc.hbm.barrel_*` keys).

**N5 [medium] — Plastic barrel holding an antimatter fluid fizz-destroyed without the explosion.**
NEXT's `FT_Amat` branch runs first for every non-antimatter barrel: destroy + power-5 fire explosion (`BlockEntityBarrel.java:216-228`); the plastic fizz branch (`:230-242`) is only reached for non-amat fluids. Modernized's amat branch delegated to the overridable `handleIncompatibleFluid`, which plastic's override always consumed (`MachineFluidTankBlockEntity.java:340-347`, `BarrelPlasticBlockEntity.java:44-50`).
**Fixed (round 1):** delegation removed — destroy + power-5 fire explosion runs inline; antimatter barrel stays exempt via `canStoreAntimatter() = true` (`BarrelAntimatterBlockEntity.java:34`). **Round-2 follow-up:** family split — barrel family keeps destroy + terrain-damaging explosion; the universal tank gets NEXT `BlockEntityMachineFluidTank.tickServer` behavior (visual-only `spawnAntimatterBlast()` + repairable wreck + `setFill(0)`), with `isBarrelFamily()` reproducing NEXT's class split (BigAssTank/Orbus extend `BlockEntityBarrel`). **Round-3 follow-up:** leak-path amat explosion now passes `fire=true` (tick branch `:368` and `updateLeak` `:633-638` ≡ NEXT `:219-226`), and corroded-holding-amat now destroys the block like NEXT instead of surviving while spilling.

**N6 [low] — Identifier of the tank's own fluid stuck in slot 0.**
NEXT always moves it: `setTankTypeByIdentifier` re-pins and copies the identifier to the output slot (`FluidTankNTM.java:463-482`, `:104-113`). Modernized returned `false` when `getConfiguredFluid() == newType` (`FluidTank.java:231-250`).
**Fixed (round 1, NEXT-predicate form):** unpinned same-kind tank re-pins the exact fluid keeping fill and moves the identifier (`FluidTank.java:231-268`, gated on `isFluidTypeExplicitlySet(conformedFluid)` — 3 hits re-grepped this session); a genuinely pinned tank still returns `false` like NEXT's `unchanged` case.

**N7 [low] — Identifier right-click feedback differed.**
NEXT: silent-audio, yellow chat system message (`BlockFluidBarrel.java:115-134`). Modernized: action-bar overlay + `EXPERIENCE_ORB_PICKUP` sound (`FluidIdentifierItem.java:99-113`).
**Fixed (round 1):** `displayClientMessage(line, false)` (chat) and the sound removed (`FluidIdentifierItem.java:106-112`). Interaction semantics (plain RMB sets, sneak+RMB opens GUI) were already at parity.

**N8 [low] — GUI feedback: no click sound on the mode button; per-material window title.**
NEXT: `playClick()` on click (`ScreenBarrel.java:98`) and flat title `container.hbm.barrel` for every barrel (`BlockEntityBarrel.java:424-427`).
**Fixed (round 1):** all three screens play `SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)` after sending `FluidTankModePacket` (`GUIBarrelSteel.java:118, :125` — re-grepped this session; same in GUIBarrelIron/GUIFluidTank); all six barrel BEs return `Component.translatable("container.hbm_m.barrel")`; regenerated lang verified (`en_us.json:1423` "Barrel", `ru_ru.json:1329` "Бочка" = NEXT `:2335`/`:2064`).

**N9 [low] — Dropped barrel's "x/y mB \<fluid\>" tooltip never rendered.**
NEXT's `PersistentInfoBlockItem` gates on the block implementing `IPersistentInfoProvider` (`PersistentInfoBlockItem.java:30-31`; `BlockFluidBarrel.java:57, :224-229`); `BarrelTankBlock` implemented only `IMultiblockController` despite the persistent tag (incl. `tank_max`) being attached correctly.
**Fixed (round 1):** `BarrelTankBlock` implements `com.hbm_m.block.IPersistentInfoProvider` and renders the line from the persistent tag with `tank_max` capacity (`BarrelTankBlock.java:297-315`, 2 hits re-grepped this session); the item gate now passes.

**N10 [low] — NFPA hazard diamonds drawn for fluids with no NTM hazard data.**
NEXT gates on `NTMFluidProperties.get(type) != null` with an early return (`RenderBarrel.java:175-179, :194`); Modernized gated on "a fluid is set" and `FluidType.forFluid` computeIfAbsent-creates a zeroed type for any fluid (`BarrelTankRenderer.java:86-96`; `FluidType.java:64-66`) — plain water drew an all-zero diamond.
**Fixed (round 1):** diamonds gated on `type.hasHazardDiamond()` (`BarrelTankRenderer.java:105`, `FluidType.java:109-111` — both re-grepped this session). **Round-2 data follow-up:** vanilla `Fluids.LAVA`/`FLOWING_LAVA` explicit overrides removed from `ModFluidHazardBootstrap` (NEXT has no lava fluid_property), and heavywater parity added (`h(HEAVYWATER[_HOT], 1,0,0,NONE)` per NEXT `heavywater*.json`); the verifier's 153-file scan confirms all 34 explicit overrides match NEXT numerically. The residual all-zero-property nuance is formally waived — see Waived.

### Fixed — verifier-driven corrections (rounds 2–4)

Found by the independent verifiers re-deriving from both trees; all fixed and re-verified:

- **[medium] Corroded leak rate ~450× too fast** — was `max(1, fill/3)` once per second (full 6,000 mB in ~10 s); NEXT leaks 1 mB at p=1/3 **per tick** ≈ 6.7 mB/s (~15 min) with `FluidTrait.onRelease(SPILL, 1)` world effects (`BlockEntityBarrel.java:244-251`). Now matches exactly, including the SPILL dispatch over the fluid's traits (`BarrelCorrodedBlockEntity.java:55-79`).
- **[medium] Universal-tank fluidmk2 speeds unbounded** — shared the barrel's whole-tank-per-pass speed; now `isBarrelFamily()` splits: universal tank/BAT9000 return NEXT's `max(500, fill/100)` / `max(500, (max-fill)/100)` (2 `Math.max(500L` sites re-grepped this session ≡ `BlockEntityMachineFluidTank.java:484-488, :507-511`); barrels keep the Long.MAX_VALUE-equivalent dynamic speed ≡ `BlockEntityBarrel.java:361-385`.
- **[medium] Universal-tank amat explosion destroyed the block** — now visual blast + repairable wreck + emptied tank per NEXT `tickServer` (`spawnAntimatterBlast()` ≡ `BlockEntityMachineFluidTank.java:157-198`); corroded-holding-amat destroys like NEXT (see N5).
- **[medium] K1 regression — iron→corroded decay duped the inventory** — fixed via the `Inventory` tag removal above.
- **[low] Corroded destroy roll gated behind the leak roll** — fired with p=(1/3)·(1/3600) per tick ≈ 540 s; now runs every tick independent of the leak roll, expected 180 s as NEXT (`BarrelCorrodedBlockEntity.java:60-80` ≡ `BlockEntityBarrel.java:250`).
- **[low] Canister partial-transfer slot handling** — round-2 restructure of `FluidLoaderStandard.emptyItem/fillItem` to NEXT's slot flow (covered under K2).
- **[low] Corroded/wreck accepted items it would silently delete** — `canPlaceItem` returns `false` when `hasExploded` (`MachineFluidTankBlockEntity.java:744-748`); extraction untouched, so nothing inside a wreck becomes unreachable.
- **[low] LEAK-ART — wreck leak rate for `FT_Gaseous_ART` fluids (hydrogen, oxygen) 100× too slow** — `calculateLeakAmount` and the smoke-particle branch counted only `FT_Gaseous`; the two traits are sibling classes (`FluidTraitSimple.java:9,15`), so the exact-class `hasTrait` lookup missed. Both checks now test `FT_Gaseous || FT_Gaseous_ART` (`MachineFluidTankBlockEntity.java:621, :657` — re-grepped this session), 1:1 with NEXT `BlockEntityMachineFluidTank.gaseous()` (`:182-185`, feeding leak amount `:166-173` and smoke branch `:252`); precedent `MachineFlareStackBlockEntity.java:129-130`.
- **[low] INF-PRESSURE — infinite-item unload ignored tank pressure** — `FluidLoaderInfinite.fillItem` now refuses when `!inf.isInstantNetwork() && tank.getPressure() != 0` (`FluidLoaderInfinite.java:21` — re-grepped this session, placed before any mutation), reproducing NEXT `FluidTankNTM.unloadTank:306-312` + `ItemFluidContainerInfinite.allowPressure:43-45`; the load direction was already covered by the wrapper gate `FluidTank.loadTank:213-214` ≡ NEXT `loadTank:277-278`, so no bypass exists (final verifier re-walked the handler chain).

### Waived (accepted, documented)

- **W1 — ComputerCraft `BarrelPeripheral`** (`Peripherals.java:266,278`): Modernized has zero ComputerCraft integration anywhere; porting would mean inventing a new subsystem.
- **W2 — water-barrel firestorm explosion** (`BlockEntityBarrel.java:253-264`): no `TomSaveData`/firestorm infrastructure exists in Modernized (grep: zero files).
- **W3 — starter-kit grant of 16× barrel_steel** (`ItemStarterKit.java:261`): Modernized's `NUKE_STARTER_KIT` is a plain item with no grant behavior (`ModItems.java:3152`).
- **W4 — generated barrel blockstates/item-models/recipes not in version control**: project-wide datagen policy (`.gitignore:102` excludes `src/generated/resources/assets/hbm_m/*` with a lang negation at `:105`; recipes under `:38`); NEXT commits its equivalents. Orchestrator-owned repo decision; the parity-relevant hand-committed block models were fixed under K3.
- **N10 residual — `ModFluids.LAVA` diamond** (`ModFluidHazardBootstrap.java:128`, 4/0/0/NOWATER): a barrel holding NTM `hbm_m:lava_source` draws a diamond NEXT would not draw; zeroing it is a cross-family data change (inference would re-create it) — accepted residual.
- **Amat blast entity damage (universal tank)** — `spawnAntimatterBlast` uses `EntityProcessorStandard().withRangeMod(2F)` with standard damage instead of NEXT's `CustomDamageHandlerAmat(50F)`; that class has no Modernized analog and inventing it is a new damage subsystem. The flagged block-damage behavior is fixed exactly; documented in the method comment.
- **Connector nozzles extra vs NEXT**: Modernized draws dynamic connector nozzles (1:1 with the 1.7.10 fidelity source this port follows; NEXT dropped them and hides the Connector part). Deliberate polish, documented in the `BarrelTankRenderer.java:52-56` class javadoc with instructions for strict-NEXT visuals.
- **N10 all-zero-property diamond nuance** — NEXT draws a `0 0 0` diamond for fluids whose datapack property exists with all-zero values and no symbol (27 of 153 files: blood, slop, seedslurry, air, … — re-measured by the final verifier's python scan) because its gate is property-presence (`RenderBarrel.java:176-194`); Modernized's data-content gate draws nothing for them. Formally waived as a deliberate NEXT-quirk divergence in the `hasHazardDiamond()` javadoc (`FluidType.java:99-108` — read this session; the round's diff there is javadoc-only). Hazard-carrying fluids and no-data fluids (vanilla water) are at parity.

## Files touched

All changes are in the working tree, uncommitted:

- Blocks/BEs: `block/machines/BarrelTankBlock.java`, `block/machines/MachineFluidTankBlock.java`, `blockentity/machines/MachineFluidTankBlockEntity.java`, `blockentity/machines/Barrel{Steel,Plastic,Corroded,Iron,Tcalloy,Antimatter}BlockEntity.java`, `blockentity/machines/BarrelCorrodedBlockEntity.java`, `block/ModBlocks.java`
- Fluid transfer: `inventory/fluid/tank/FluidLoaderStandard.java`, `inventory/fluid/tank/FluidLoaderInfinite.java`, `inventory/fluid/tank/FluidTank.java`, `inventory/fluid/FluidType.java` (javadoc waiver)
- Items/GUI: `item/liquids/FluidIdentifierItem.java`, `inventory/gui/GUIBarrelSteel.java`, `inventory/gui/GUIBarrelIron.java`, `inventory/gui/GUIFluidTank.java`
- Rendering/data: `client/render/implementations/BarrelTankRenderer.java`, `api/fluids/bootstrap/ModFluidHazardBootstrap.java`
- Datagen providers: `datagen/ModLanguageProviderEn.java`, `datagen/ModLanguageProviderRu.java`
- Assets (hand-committed): `src/main/resources/assets/hbm_m/models/block/barrel_{steel,plastic,tcalloy,antimatter,corroded,iron}.json`
- Regenerated by runData: `src/generated/resources/assets/hbm_m/lang/en_us.json`, `ru_ru.json` (`container.hbm_m.barrel`, `Capacity: %smB`, mode labels, tooltip keys)

`docs/porting/machine_porting_matrix.json` was not touched by the fix agents (the orchestrator owns it; its dirty state predates this work per session-start git status).

## Verification evidence

**Compile + datagen gates** (run by the workflow after each round; gradle is forbidden to the worker agents and was not run by this report):

| Round | Gate | Exit |
|---|---|---|
| 1 attempt 1 | `:1.20.1-forge:compileJava` | 0 |
| 1 attempt 1 | `:1.21.1-neoforge:compileJava` | 0 |
| 1 attempt 1 | `:1.20.1-forge:runData` | 0 |
| 2 attempt 1 | `:1.20.1-forge:compileJava` | 0 |
| 2 attempt 1 | `:1.21.1-neoforge:compileJava` | 0 |
| 2 attempt 1 | `:1.20.1-forge:runData` | 0 |
| 3 attempt 1 | `:1.20.1-forge:compileJava` | 0 |
| 3 attempt 1 | `:1.21.1-neoforge:compileJava` | 0 |
| 3 attempt 1 | `:1.20.1-forge:runData` | 0 |
| final attempt 1 | `:1.20.1-forge:compileJava` | 0 |
| final attempt 1 | `:1.21.1-neoforge:compileJava` | 0 |
| final attempt 1 | `:1.20.1-forge:runData` | 0 |

**Current-state marker greps (run by this report, this session):** `dropInventory()` at `BarrelTankBlock.java:152` + `MachineFluidTankBlock.java:176`; `sameSubstance` gate at `FluidLoaderStandard.java:31`; `isInstantNetwork() && tank.getPressure() != 0` at `FluidLoaderInfinite.java:21`; `remove("Inventory")` at `BarrelIronBlockEntity.java:70`; `FT_Gaseous_ART` disjunctions at `MachineFluidTankBlockEntity.java:621, :657`; `playClick` at `GUIBarrelSteel.java:118/:125`; `container.hbm_m.barrel` at generated `en_us.json:1423` / `ru_ru.json:1329`; `Capacity: %smB` at `en_us.json:5278`; `hasHazardDiamond` at `FluidType.java:109` + `BarrelTankRenderer.java:105`; `IPersistentInfoProvider` ×2 in `BarrelTankBlock`; `Math.max(500L` ×2 in `MachineFluidTankBlockEntity`. Matrix extraction: `portStatus = IN_PROGRESS` (orchestrator flip pending).

**Verifier coverage per round** (fresh independent verifiers each round, re-deriving from both trees rather than trusting fix reports):

- **Round 1** — two slices. Logic: K1 spill, K2 voiding, N6 predicate, comparator formula + wiring, ROR values/`function:setmode` (incl. byte-identical `RorModes`), 4-mode gating + automation slot exposure `{2,3,4,5}`, canister transfer amounts, N5 amat branch, capacities (12k/6k/16k/24k/16k all matching NEXT `ModBlocks.java:1891-1938`), client sync, NBT round-trip. Presentation: K3 display transforms (6× true), model geometry (barrel.obj byte-identical modulo CRLF; textures cmp/md5-identical), K4 per-variant line sets, GUI layout/hit rects (vs `ScreenBarrel.java:32-68`), N8 title, renderer slice, creative tabs (`CreativeModeTabEventHandler.java:1702-1706` ≡ NEXT `CreativeContents.java:2644-2647`), recipes, lang coverage, W4 premise (`git check-ignore`/`ls-files`). Surfaced the corroded-leak-rate, universal-speeds and universal-amat mediums plus K4 formatting and vanilla-lava/hydrogen follow-ups.
- **Round 2** — two slices. Logic: re-verified all round-1 fixes intact via marker greps; found the K1 iron→corroded dupe regression (proven statically incl. `javap` of mapped 1.20.1 `LevelChunk.setBlockState` showing `onRemove` fires before `removeBlockEntity`), the corroded destroy-roll gating, the leak-path fire flag, the corroded item-loss path. Presentation: re-verified K3/K4/N8/N9/N10 (md5 of gui textures, python 153-file `fluid_property` scan — all 34 explicit overrides match); found the connector-nozzle extra and the `ModFluids.LAVA` residual.
- **Round 3** — logic slice: re-verified the K1 regression fix (including the Forge `ItemStackHandler.deserializeNBT` no-op on a keyless tag), the destroy-roll fix, the fire flag, the corroded/wreck item gate, and the `%smB` En string in both provider and generated lang; re-confirmed all prior medium fixes intact; surfaced **LEAK-ART** and **INF-PRESSURE**. Render slice: confirmed N10 gate + data, K3 float-exact, K4/GUI/lang/recipes/tabs; found the N10 all-zero nuance.
- **Final round** — two slices. Logic: LEAK-ART verified on both sides (leak amount + particle branch, branch order untouched); INF-PRESSURE verified in both directions incl. the no-bypass handler-chain walk; K1 proven exactly-once; regression scans clean (`git diff` bounds the round's collateral to the three prescribed files). Render: N10 waiver premise re-measured (27/153 all-zero files, blood.json example), waiver javadoc confirmed javadoc-only by `git diff HEAD`, gate unshared (repo-wide `hasHazardDiamond` = exactly 2 code hits: definition + `BarrelTankRenderer` caller), display transforms float-exact, GUI md5/pixel layout, recipes, lang.

**Judge verdict (final round): `complete = true`, `blocked = false`.** The judge re-derived from source: LEAK-ART fixed (both the leak rate and the smoke branch, family scope correct — only `BarrelCorrodedBlockEntity` overrides, calling `super`), INF-PRESSURE fixed (both directions, no bypass), N10 formally waived via the pre-accepted documented-waiver option (javadoc-only diff, accurate NEXT facts), K1 regression fix intact, and no new medium/high issue standing (both final regression scans clean; all residual findings low and non-blocking, listed under caveats). Compile/datagen gates: both targets and `runData` exit 0 on the final round.

**Static tooling used by verifiers this session:** node/JSON numeric comparisons (display transforms), md5/cmp texture and OBJ comparisons, python scans (matrix extraction, 153-file fluid_property audit), marker greps with counts, `javap` on the mapped 1.20.1 Forge jar, `git check-ignore`/`git ls-files`/`git diff`.

## Not covered / caveats

- **No runtime testing.** No game client, GameTest, or in-game visual check ran. All parity evidence is static: source reads, bytecode inspection, and file/JSON comparisons. The workflow owns the compile gates; this report did not run gradle.
- `TurbofanIntegrationGameTest.java:141-151` exercises `barrel_steel` on the MK2 network (fresh-barrel mode == 0), but no barrel-specific gametest suite exists beyond it; it was not run this session.
- **Client-sync cadence is a disclosed adaptation**: NEXT syncs mode+tank every tick (`networkPackNT(50)`); Modernized re-sends on change at most once per second (delta-check + `sendBlockUpdated`). GUI is unaffected (live `ContainerData`); only in-world render state can lag ≤1 s. End state converges.
- **Corroded barrel is a documented intentional deviation**: NEXT's `barrel_corroded` has no block entity (plain `BlockFluidBarrel`, `ModBlocks.java:1899-1909`), so its leak branch is dead code there; Modernized keeps a live corroded BE as the iron-decay endpoint with capacity anchored to NEXT's 6,000 mB. Consequences that are *not* parity bugs: corroded accepts no canisters (item gate) and spills nothing on break (`dropInventoryOnRemove()=false`).
- **barrel_iron is Modernized-only** (NEXT has a dead lang pair but no block): registered, 16,000 mB, creative tab, corrosive→corroded decay carrying fluid/mode, no recipe — internally consistent but unverifiable against NEXT beyond the lang names.
- **Judge's non-blocking low informational notes** (explicitly out of barrel scope, for follow-up owners): (1) the shared base `updateLeak` approximates NEXT's wreck-leak visuals and omits its `FluidTrait.onFluidRelease` world effects on the universal-tank surface (barrel impact nil — corroded wires its own SPILL path, the amat barrel path destroys the block); (2) this report file's staleness, now resolved by this rewrite; (3) the universal-tank/BAT9000 renderer (`MachineFluidTankRenderer.java:193-220`) draws an all-zero diamond for any non-NONE fluid incl. water — machine_fluid_tank family audit item, zero barrel impact (barrel renderer is gated); (4) Modernized skips all slot processing incl. `unloadTank` while a tank is wrecked, where NEXT still unloads damaged tanks — deliberate structural adaptation, unload-UX only.
- Working tree carries parallel orchestrator changes (doors, sounds, ModItems, generated assets) unrelated to barrels; none were touched by barrel work.
- NEXT-side quirks deliberately not copied (additive coverage, not parity breaks): NEXT lacks a `tankMode.disabled` label (renders the raw key for mode 3), has no Russian barrel desc/mode translations, and keeps `block.hbm.barrel_iron` lang without registering the block.

## Condensed history of the prior session(s)

The prior session (workflow run `dwfrun-bb9afe4b-4fa7-4ce6-b6ce-7a7d6faa85ec`, "barrel_steel parity port", continuation of `dwfrun-1638f125` after a mid-run concurrency change; ~1 h 24 m) took the family from a stub to near-parity. Its substance, condensed:

- **Starting point:** `BarrelSteelBlockEntity` was a 41-line thin subclass of `MachineFluidTankBlockEntity` — a generic 16,000 mB tank with menu/GUI/registrations; NEXT's original is a 495-line family TE (`BlockEntityBarrel`) + `BlockFluidBarrel` with canister handling, 4 modes, pipe-network node, comparator, ROR and hazards.
- **Audit result:** 14 discrepancies (5 medium — D1 comparator output dead, D2 ROR values/`setmode` missing, D3 no crafting recipes (barrels unobtainable in survival), D4 steel barrel wrongly destroyed by highly corrosive fluids, D5 amat explosion left a repairable shell instead of removing the block; 9 low), 20 aspects already at parity, 2 out-of-scope candidates.
- **Round 1 (D1–D13, 15 files):** comparator overrides; ROR implementation + `RorModes`; shaped recipes for the four obtainable barrels; steel stores highly corrosive fluids; amat branch destroy + power-5 fire explosion; identifier gating un-inverted; automation restricted to slots {2,3,4,5}; MK2 speeds uncapped; corroded capacity 6,000 mB; block properties `strength(2.0,3.0)`/METAL/correct-tool; shipped `gui_barrel.png`; En mode-1 label + Ru display names; connector detection fallback. Checkpointed by the nightly supervisor as `d9170496` (and later `89bc3117` "wip(barrel_steel): rounds 2-3, 13/14 discrepancies fixed"); the checkpoint briefly reset the matrix entry to UNPORTED and it was restored to IN_PROGRESS.
- **Round 2 (9 low issues):** canister-slot content gating in BE + menus; MK2-driven client sync (20-tick cadence); ROR `type` → `"NONE"` for empty; canister partial transfers via `FluidTank.loadingHandlers`; renderer shadow-pass gate; LMB-only mode cycling; FRAPI unwrap; `barrel_iron` added to creative tab.
- **Round 3 (3 issues):** steel recipe shell ingredient corrected to `hbm_m:plate_steel` (end-to-end verified in the regenerated recipe JSON); unset identifier now clears the tank type (null/NONE → `ModFluids.NONE`, matching NEXT `BlockFluidBarrel.java:122-123`); GUI mode-button hit-Y (35) separated from blit-Y (34).
- **Waivers carried from that run** (re-verified and re-accepted this session as W1–W4): ComputerCraft peripheral, firestorm explosion, starter-kit grant, generated-assets gitignore.
- **That run ended BLOCKED** with 4 open issues — inventory deleted on block removal (medium), `emptyItem` fluid voiding, display-transform mismatch, extra tooltip line (lows) — which this session's audit re-derived as K1–K4 and fixed. Its verification regime was the same shape: compile on both targets exit 0 after every round, `runData` exit 0, fresh logic+presentation verifiers per round (R1 16+22 areas, R2 17+20, R3 16+22), judge re-derivation line-level.
