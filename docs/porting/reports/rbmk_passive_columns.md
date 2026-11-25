# rbmk_blank / rbmk_absorber / rbmk_reflector / rbmk_moderator / rbmk_storage / rbmk_outgasser — fidelity audit report

**Status: AUDITED (read-only).** No code was changed in this pass. Verdicts: `rbmk_blank`, `rbmk_absorber`, `rbmk_reflector`, `rbmk_moderator` = **MATCH**; `rbmk_storage`, `rbmk_outgasser` = **MINOR** (core semantics 1:1, peripheral deviations/absences listed below). Totals: **9 DEVIATION, 4 ABSENT, 7 EXTRA** findings across the six ids.

- Date: 2026-11-22
- Mode: **read-only fidelity audit** against the authoritative 1.7.10 tree (no fixes applied, no gradle, no commits)
- Source of truth: `C:\Projects\Hbm-s-Nuclear-Tech-GIT\src\main\java\com\hbm\...` (upstream master, not the ntm-next mirror)
- Port: `C:\Projects\HBM-Modernized-agents\src\main\java\com\hbm_m\...` (working tree; matrix ids `rbmk_blank`, `rbmk_absorber`, `rbmk_reflector`, `rbmk_moderator`, `rbmk_storage`, `rbmk_outgasser`)

Files audited (original → port):

| Original | Port |
|---|---|
| `blocks/machine/rbmk/RBMK{Blank,Absorber,Reflector,Moderator,Storage,Outgasser}.java` | `block/machines/rbmk/RBMK{Blank,Absorber,Reflector,Moderator,Storage,Outgasser}Block.java` + shared `RBMKColumnBlock.java`, `RBMKColumnFillerBlock.java` |
| `blocks/machine/rbmk/RBMKBase.java`, `items/machine/ItemRBMKLid.java` | `block/machines/rbmk/RBMKColumnBlock.java`, `item/rbmk/RBMKLidItem.java` |
| `tileentity/machine/rbmk/TileEntityRBMK{Blank,Absorber,Reflector,Moderator}.java` | `blockentity/machines/rbmk/RBMK{Blank,Absorber,Reflector,Moderator}BlockEntity.java` |
| `tileentity/machine/rbmk/TileEntityRBMK{Base,ActiveBase,SlottedBase}.java` | `blockentity/machines/rbmk/RBMKColumnBlockEntity.java` (+ `BaseHbmBlockEntity`) |
| `tileentity/machine/rbmk/TileEntityRBMKStorage.java` | `blockentity/machines/rbmk/RBMKStorageBlockEntity.java` |
| `tileentity/machine/rbmk/TileEntityRBMKOutgasser.java` | `blockentity/machines/rbmk/RBMKOutgasserBlockEntity.java` |
| `tileentity/machine/rbmk/{RBMKDials,IRBMKLoadable,IRBMKFluxReceiver}.java`, `handler/neutron/{RBMKNeutronHandler,NeutronHandler}.java` | `handler/rbmk/{RBMKDials,RBMKGameRules,IRBMKLoadable,IRBMKFluxReceiver,RBMKNeutronHandler,NeutronNodeWorld}.java` |
| `inventory/recipes/OutgasserRecipes.java`, `handler/nei/OutgasserHandler.java` | `handler/rbmk/RBMKOutgasserRecipes.java`, `compat/jei/RBMKOutgasserJeiCategory.java` |
| `inventory/container/ContainerRBMK{Storage,Outgasser}.java`, `inventory/gui/GUIRBMK{Storage,Outgasser}.java` | `inventory/menu/RBMK{Storage,Outgasser}Menu.java`, `inventory/gui/GUIRBMK{Storage,Outgasser}.java` |
| `blocks/ModBlocks.java` (registration), `main/MainRegistry.java` (tabs) | `block/ModBlocks.java:2314-2322`, `main/CreativeModeTabEventHandler.java:4088-4107` |

---

## Verdict summary

| Matrix id | Verdict | One-line summary |
|---|---|---|
| `rbmk_blank` | **MATCH** | Inert structural column, `OTHER` neutron type (pass-through), 1-2 BLANK debris on melt — 1:1. |
| `rbmk_absorber` | **MATCH** | `heat += absorberHeatConversion × flux`, `efficiency==1 → stop, else flux *= absorberEfficiency` — 1:1 incl. dial defaults. |
| `rbmk_reflector` | **MATCH** | Reflect-to-origin at `efficiency==1` / attenuate-and-continue otherwise, moderation re-application loop — 1:1. |
| `rbmk_moderator` | **MATCH** | `moderatedCount++` + `fluxRatio *= (1 − moderatorEfficiency)`, 2-3 GRAPHITE debris on melt — 1:1 (port adds a redundant but neutral `isModerated()=true` override). |
| `rbmk_storage` | **MINOR** | 12 slots, rod-only, slot-11 load / slot-0 unload all 1:1; compaction timing and GUI slot gating differ; hopper/pipe item automation absent. |
| `rbmk_outgasser` | **MINOR** | All 19 default activation recipes 1:1 with correct counts, flux→progress math 1:1; GUI overlays/slots misaligned against a pixel-identical texture; crane loadability, induced-radioactivity and console-readout extras; fusionOnly/config-JSON recipe loading and OC absent. |

---

## 1. rbmk_blank

**Scope:** `RBMKBlank.java` + `TileEntityRBMKBlank.java` → `RBMKBlankBlock.java` + `RBMKBlankBlockEntity.java`.

| Topic | Verdict | Evidence |
|---|---|---|
| Neutron type | MATCH | Original does not override `getRBMKType()` → inherits `OTHER` (`TileEntityRBMKBase.java:261-263`); `RBMKType.OTHER` streams "pass right on by" (`RBMKNeutronHandler.java:297-298`). Port returns `RBMKType.OTHER` (`RBMKBlankBlockEntity.java:27`), port handler passes OTHER through (`RBMKNeutronHandler.java:236`). |
| Console type | MATCH | `ColumnType.BLANK` both sides (`TileEntityRBMKBlank.java:21-23` / `RBMKBlankBlockEntity.java:28`). |
| Meltdown | MATCH | 1-2 BLANK debris before standard melt: `1 + rand.nextInt(2)` (`TileEntityRBMKBlank.java:11-15`) = `1 + nextInt(2)` (`RBMKBlankBlockEntity.java:22-24`). |
| GUI/interaction | MATCH | No GUI on either side (no `provideGUI`/`MenuProvider`). |
| Structure | [GLUE] | Original = `BlockDummyable` core + dummy segments with `null` TEs + top extra block (`RBMKBase.getDimensions`); port = base BE + invisible `RBMKColumnFillerBlock` stack (`RBMKColumnBlock.setPlacedBy/fillFillers`, `RBMKColumnBlock.java:194-235`). Same visible/collision result; filler carries no BE, so no hopper surface (see storage §5). |

**Deviations: none. ABSENT/EXTRA: none.**

---

## 2. rbmk_absorber

**Scope:** `RBMKAbsorber.java` + `TileEntityRBMKAbsorber.java` → `RBMKAbsorberBlock.java` + `RBMKAbsorberBlockEntity.java`.

| Topic | Verdict | Evidence |
|---|---|---|
| Flux behavior | MATCH | Original: `heat += getAbsorberHeatConversion × fluxQuantity; if (absorberEfficiency == 1) return; fluxQuantity *= absorberEfficiency` (`RBMKNeutronHandler.java:357-364`). Port is token-identical (`RBMKNeutronHandler.java:268-271`). The stream terminates on a fully-efficient absorber and attenuates otherwise — preserved. |
| Constants | MATCH | `dialAbsorberEfficiency` default 1.0, clamp [0,1] (`RBMKDials.java:38, 110`); `dialAbsorberHeatConversion` default 0.05, clamp [0,1] (`RBMKDials.java:42, 111`). Port statics `ABSORBER_EFFICIENCY = 1.0`, `ABSORBER_HEAT_CONV = 0.05` with identical clamps (`RBMKDials.java:48, 50, 81-82`). |
| Dial carriage | [GLUE] | In the original these two dials are **game rules** (`dialAbsorberEfficiency`, `dialAbsorberHeatConversion` strings via `RBMKDials.refresh`); in the port vanilla gamerules can only be boolean/int, so the float dials live in the mod config (`RBMKGameRules.java:14-16` documents the split). Tuning mechanism differs; values and clamps match. |
| Per-tick caching | MATCH | Original caches `absorberEfficiency` per server tick (`NeutronHandler.java:34-42`); port caches it in `NeutronNodeWorld.tick` (`NeutronNodeWorld.java:72-79`). Port additionally caches `absorberHeatConversion` (original reads it per hit) — same value, perf-only. |
| Meltdown | MATCH | 1-2 BLANK debris (`TileEntityRBMKAbsorber.java:12-15` / `RBMKAbsorberBlockEntity.java:22-24`). |
| Console type | MATCH | `ABSORBER` both sides. |

**Deviations: none. ABSENT/EXTRA: none.**

---

## 3. rbmk_reflector

**Scope:** `RBMKReflector.java` + `TileEntityRBMKReflector.java` → `RBMKReflectorBlock.java` + `RBMKReflectorBlockEntity.java`.

| Topic | Verdict | Evidence |
|---|---|---|
| Reflection | MATCH | Original: reflector branch — origin moderation re-count, `for(moderatedCount) moderateStream()`, `if (reflectorEfficiency != 1.0) { flux *= reflectorEfficiency; continue; }` else `origin.receiveFlux(this); return` (`RBMKNeutronHandler.java:341-356`). Port line-for-line: `RBMKNeutronHandler.java:260-267` (reflect cast replaced by an `instanceof RBMKRodBlockEntity` guard — streams only originate from rods, same behavior). |
| Constants | MATCH | `dialReflectorEfficiency` default 1.0, clamp [0,1] (`RBMKDials.java:39, 112`) = port `REFLECTOR_EFFICIENCY = 1.0` clamp [0,1] (`RBMKDials.java:49, 83`). |
| Moderated-origin semantics | MATCH | `originTE.isModerated()` consults the rod's moderated flag on both sides (original `TileEntityRBMKRod.java:76`; port `RBMKRodBlockEntity.java:254`). |
| Meltdown | MATCH | 1-2 BLANK debris (`TileEntityRBMKReflector.java:12-15` / `RBMKReflectorBlockEntity.java:22-24`). |
| Console type | MATCH | `REFLECTOR` both sides. |

**Deviations: none. ABSENT/EXTRA: none.**

---

## 4. rbmk_moderator

**Scope:** `RBMKModerator.java` + `TileEntityRBMKModerator.java` → `RBMKModeratorBlock.java` + `RBMKModeratorBlockEntity.java`.

| Topic | Verdict | Evidence |
|---|---|---|
| Moderation | MATCH | Original: `if (type == MODERATOR \|\| nodeTE.isModerated()) { moderatedCount++; moderateStream(); }` and `moderateStream(): fluxRatio *= (1 − moderatorEfficiency)` (`RBMKNeutronHandler.java:307-310, 424-426`). Port identical (`RBMKNeutronHandler.java:245-248, 347-349`). Fast→slow ratio reduction (which drives the outgasser's `1 − ratio × 0.8` efficiency) preserved. |
| Constants | MATCH | `dialModeratorEfficiency` default 1.0, clamp [0,1] (`RBMKDials.java:37, 109`) = port `MODERATOR_EFFICIENCY = 1.0` clamp [0,1] (`RBMKDials.java:47, 80`). |
| `isModerated()` override | [GLUE] | Port adds `isModerated() → true` on the moderator BE (`RBMKModeratorBlockEntity.java:28`); the original moderator does not override it (base returns false). In the port's own handler this is a single `if` with an OR — the column is still counted once — so behavior is unchanged; it is structurally redundant, kept as harmless. |
| Meltdown | MATCH | 2-3 **GRAPHITE** debris (`2 + rand.nextInt(2)`, `TileEntityRBMKModerator.java:12-15` / `RBMKModeratorBlockEntity.java:22-24`) — unlike the other passives which drop BLANK. |
| Console type | MATCH | `MODERATOR` both sides. |

**Deviations: none. ABSENT/EXTRA: none.**

---

## 5. rbmk_storage

**Scope:** `RBMKStorage.java` + `TileEntityRBMKStorage.java` + `ContainerRBMKStorage.java` + `GUIRBMKStorage.java` → `RBMKStorageBlock.java` + `RBMKStorageBlockEntity.java` + `RBMKStorageMenu.java` + `GUIRBMKStorage.java`.

| Topic | Verdict | Evidence |
|---|---|---|
| Slot count / layout | MATCH | 12 slots, 3×4 grid at `(32 + 32·j, 29 + 16·i)` (original `ContainerRBMKStorage.java:18-22`; port `RBMKStorageMenu.java:44-51`). Player inv rows at `104 + row·18`, hotbar at 162 — both sides. |
| Accepted items | MATCH (GUI stricter, see D5-STO) | Hopper rule: `ItemRBMKRod` (`TileEntityRBMKStorage.java:54-56`); port menu `mayPlace = RBMKRodItem` (`RBMKStorageMenu.java:48`). |
| IRBMKLoadable | MATCH | `canLoad = slot 11 empty` (`TileEntityRBMKStorage.java:69-71` / `RBMKStorageBlockEntity.java:62-66`), `load → slots[11] = copy` (:74-76 / :69-72), `canUnload = slots[0] != null` (:79-81 / :75-77), `provideNext → slots[0]` (:84-86 / :80-82), `unload → slots[0] = null` (:89-91 / :85-88). |
| Neutron type | MATCH | `RBMKType.OTHER` — streams pass through the storage column (`TileEntityRBMKStorage.java:44-46` / `RBMKStorageBlockEntity.java:92`). |
| FBI easter egg | MATCH | `BossSpawnHandler.markFBI(player)` on every activation (`RBMKStorage.java:29` / `RBMKStorageBlock.java:33-34`). |
| NBT | [COSMETIC] | Original `"items"` list with `"slot"` byte + `"name"` custom name (`TileEntityRBMKSlottedBase.java:135-174`); port `"slots"` list with `"s"` byte + `"item"` (`RBMKStorageBlockEntity.java:99-124`). No world-compat requirement between versions; key naming only. |

**Findings:**

- **D5-STO-1 [DEVIATION, minor] — compaction cadence.** Original: bubble-shift one position per pass, every **10 ticks** (`worldObj.getTotalWorldTime() % 10 == 0`, `TileEntityRBMKStorage.java:29-38`). Port: full single-pass compaction **every tick** (`RBMKStorageBlockEntity.java:44-52`), and its comment credits "CE" with the every-tick behavior — which the authoritative 1.7.10 tree does **not** have. Converges to the same invariant (no gaps), but gap-closing speed differs; the crane can see a mid-gap in the original during the 10-tick window that the port never has. Likely sourced from a Community-Edition fork rather than the audited tree — the port's "CE" comments should be re-based on this tree.
- **D5-STO-2 [DEVIATION, minor] — GUI slot gating.** Original container slots are plain `Slot`s: the **GUI** accepts any item into the 12 rack slots (hopper restriction lives in `canInsertItem` only). Port menu restricts `mayPlace` to `RBMKRodItem` (`RBMKStorageMenu.java:47-49`), so hand-inserting a non-rod item into the rack is no longer possible.
- **A5-STO-1 [ABSENT] — item automation.** Original implements `ISidedInventory`: slots `{0..11}` accessible from **every side and every dummy segment** (the dummies are `TileEntityProxyCombo(true,false,false)` inventory proxies, `RBMKStorage.java:19`), insert = rods only, `canExtractItem = true` for all (`TileEntityRBMKStorage.java:54-66`). The port BE exposes no item capability (`BaseHbmBlockEntity.getItemHandler` default `null` is not overridden — `RBMKStorageBlockEntity.java` has no override), and the filler blocks carry no BE at all → hoppers/pipes cannot insert or extract rods. Rod loading/unloading still works via crane/autoloader and GUI.
- **D5-STO-3 [DEVIATION, trivial] — menu `stillValid` reach.** Original `canInteractWith → true` unconditionally (`ContainerRBMKStorage.java:63-65`); port enforces vanilla 4-block reach via `MenuReach.stillValid` (`RBMKStorageMenu.java:93-96`). Vanilla-conventional; noted for completeness.
- **X5-STO-1 [EXTRA, cosmetic] — heat readout.** Port GUI draws `Heat: %.1f°C` (`GUIRBMKStorage.java:38-40`); the original storage GUI draws nothing but the title.
- **W5-STO-1 [GLUE] — broadcast re-sync.** Port menu keeps a snapshot and re-syncs from the BE in `broadcastChanges` (`RBMKStorageMenu.java:80-90`) because the BE mutates its own `ItemStack[]` while open — modern-container necessity, no original analog (original `Slot`s wrap the TE directly).

**Verdict: MINOR** — inventory semantics, load/unload contract, and neutron pass-through are 1:1; the gaps are automation access and two behavioral nuances.

---

## 6. rbmk_outgasser

**Scope:** `RBMKOutgasser.java` + `TileEntityRBMKOutgasser.java` + `OutgasserRecipes.java` + `OutgasserHandler.java` (NEI) + `ContainerRBMKOutgasser.java` + `GUIRBMKOutgasser.java` → `RBMKOutgasserBlock.java` + `RBMKOutgasserBlockEntity.java` + `RBMKOutgasserRecipes.java` + `RBMKOutgasserJeiCategory.java` + `RBMKOutgasserMenu.java` + `GUIRBMKOutgasser.java`.

### 6.1 Recipe list — 1:1 comparison (original `OutgasserRecipes.registerDefaults`, `OutgasserRecipes.java:32-63`)

Original recipe source location: **`com/hbm/inventory/recipes/OutgasserRecipes.java`** (class `SerializableRecipe`; runtime-configurable via `hbmIrradiation.json`). JEI/NEI exposure: `com/hbm/handler/nei/OutgasserHandler.java` (machines = `{rbmk_outgasser, fusion_breeder}`, `fusionOnly` recipes overridden to show the breeder).

| # | Original (input → output) | Port (`RBMKOutgasserRecipes.init`, :55-108) | Match |
|---|---|---|---|
| 1 | `LI.block()` → TRITIUM 10 000 | `BLOCK_LITHIUM` → tritium 10 000 (:58) | ✓ |
| 2 | `LI.ingot()` → TRITIUM 1 000 | `LITHIUM` ingot → 1 000 (:59) | ✓ |
| 3 | `LI.dust()` → TRITIUM 1 000 | `LITHIUM` POWDER → 1 000 (:60) | ✓ |
| 4 | `LI.dustTiny()` → TRITIUM 100 | `LITHIUM` POWDER_TINY → 100 (:61) | ✓ |
| 5 | `GOLD.ingot()` → `ingot_au198` | `GOLD_INGOT` → AU198 ingot (:64) | ✓ |
| 6 | `GOLD.nugget()` → `nugget_au198` | `GOLD_NUGGET` → AU198 nugget (:65) | ✓ |
| 7 | `GOLD.dust()` → `powder_au198` | GOLD POWDER → AU198 POWDER (:66-67) | ✓ |
| 8 | `TH232.ingot()` → `ingot_thorium_fuel` | THORIUM232 INGOT → THORIUM_FUEL INGOT (:80-81) | ✓ |
| 9 | `TH232.nugget()` → `nugget_thorium_fuel` | THORIUM232 NUGGET → NUGGET (:82-83) | ✓ |
| 10 | `TH232.billet()` → `billet_thorium_fuel` | THORIUM232 BILLET → BILLET (:84-85) | ✓ |
| 11 | brown_mushroom → `mush` | `BROWN_MUSHROOM` → MUSH (:88) | ✓ |
| 12 | red_mushroom → `mush` | `RED_MUSHROOM` → MUSH (:89) | ✓ |
| 13 | mushroom_stew → `glowing_stew` | `MUSHROOM_STEW` → GLOWING_STEW (:90) | ✓ |
| 14 | `COAL.gem()` → coal tar ×1 + SYNGAS 50 | `Items.COAL` → OIL_TAR_COAL ×1 + syngas 50 (:96) | ✓ |
| 15 | `COAL.dust()` → coal tar ×1 + SYNGAS 50 | COAL POWDER → tar ×1 + syngas 50 (:97) | ✓ |
| 16 | `COAL.block()` → coal tar ×9 + SYNGAS 500 | `COAL_BLOCK` → tar ×9 + syngas 500 (:98-99) | ✓ |
| 17 | `PVC.ingot()` → `ingot_c4` + COLLOID 250 | PVC INGOT → C4 INGOT + colloid 250 (:102-103) | ✓ |
| 18 | coal tar (`oil_tar`/COAL) → COALOIL 100 | `OIL_TAR_COAL` → COALOIL 100 (:106) | ✓ |
| 19 | wax tar (`oil_tar`/WAX) → RADIOSOLVENT 100 | `OIL_TAR_WAX` → RADIOSOLVENT 100 (:107) | ✓ |

All 19 defaults present, no port-only additions among defaults, counts identical.

- **D6-OUT-1 [DEVIATION, minor] — OreDictionary generality lost.** Original keys are ore-dict entries (`LI.block()`, `GOLD.ingot()`, `COAL.gem()` …) and `getOutput` falls back to the stack's dict keys (`OutgasserRecipes.java:65-83`), so third-party ore-dict items (any mod's gold ingot, any lithium block variant) activate. Port keys on concrete `Item`s (`getRecipe → RECIPES.get(input.getItem())`, `RBMKOutgasserRecipes.java:114-118`). Canon items covered 1:1; genericity reduced.
- **A6-OUT-1 [ABSENT, low impact] — `fusionOnly` flag and `hbmIrradiation.json` loading.** Original recipes are `SerializableRecipe`-loadable (runtime JSON overrides, `OutgasserRecipes.java:103-155`) and carry a `fusionOnly` flag honored by `canProcess` (`TileEntityRBMKOutgasser.java:92`) and by the NEI machine override (`OutgasserHandler.java:21-27`). The port table is hardcoded, has no fusionOnly concept, and its JEI category has no breeder override (`RBMKOutgasserJeiCategory.java:43-48` shows the breeder as a static second machine). Defaults contain no fusion-only recipes, so out-of-the-box behavior is identical; the loss only matters for user-supplied `hbmIrradiation.json` data.
- **A6-OUT-2 [ABSENT, justified] — OpenComputers `SimpleComponent`.** Original exposes `rbmk_outgasser` OC callbacks (`getGas/getGasMax/getGasType/getProgress/getCrafting/getCoordinates/getInfo`, `TileEntityRBMKOutgasser.java:219-272`). Not ported (OC does not exist for 1.20.1/1.21.1; project-wide absence).
- **X6-OUT-1 [EXTRA, dormant] — induced radioactivity on unrecipeable items.** Port `receiveFlux` has an `else` branch calling `ContaminationUtil.neutronActivateItem(input, flux × eff × 0.001, 1F)` (`RBMKOutgasserBlockEntity.java:110-116`). On both sides the handler only calls `receiveFlux` **when `canProcess()` is true** (original `RBMKNeutronHandler.java:323-330`; port `RBMKNeutronHandler.java:253-255`), so this branch is unreachable from the reactor path — dead code today, a semantic change if ever wired up.
- **X6-OUT-2 [EXTRA] — outgasser is crane-loadable.** Port implements `IRBMKLoadable` (load → input slot, unload ← output slot, `RBMKOutgasserBlockEntity.java:235-239`). The original outgasser does **not** implement `IRBMKLoadable`, so the original crane skips it (`TileEntityCraneConsole.java:106` targets `IRBMKLoadable` only). Consequence: the port crane can insert recipe inputs and extract outputs (including non-rod items) from an outgasser column; the original required hopper/GUI access.
- **X6-OUT-3 [EXTRA] — progress reset on input swap.** Original resets `progress` only when `!canProcess()` (`TileEntityRBMKOutgasser.java:62`); port additionally resets when the input item type changed (`!ItemStack.isSameItem(previousStack, inputSlot)`, `RBMKOutgasserBlockEntity.java:73-75`). This closes an original exploit (carry half-accumulated progress across item swaps); original behavior allowed it.
- **X6-OUT-4 [EXTRA, cosmetic] — GUI text readouts.** Port outgasser GUI adds a `Flux` label, a green `progress/duration` SI-formatted readout, and hover tooltips (`GUIRBMKOutgasser.java:68-71, 84-95`); the original draws only the title and progress/tank bars.
- **X6-OUT-5 [EXTRA] — Space-fork recipes.** Cobalt→Co-60 and bismuth→Po-210 (ingot/nugget/powder ×2 shapes sets) gated behind `SpaceApi.forkActive()` (`RBMKOutgasserRecipes.java:69-77`). No analog in the audited 1.7.10 tree (the comment credits an E8 fork's Space add-on). Inactive unless the space fork is on.

### 6.2 Tick processing and flux math

| Topic | Verdict | Evidence |
|---|---|---|
| receiveFlux | MATCH | `efficiency = min(1 − fluxRatio × 0.8, 1)`; `progress += fluxQuantity × efficiency × getOutgasserMod`; `if (progress > duration) process()` — identical (`TileEntityRBMKOutgasser.java:69-82` / `RBMKOutgasserBlockEntity.java:89-108`). Outgasser speed dial default 1.0 both sides (`RBMKDials.java:29, 101` / `RBMKDials.java:30, 71`). |
| duration | MATCH | `10000` (`TileEntityRBMKOutgasser.java:38` / `RBMKOutgasserBlockEntity.java:52`). |
| canProcess | MATCH | input empty → false; no recipe → false; fluid-type mismatch while tank non-empty → false; set tank type; capacity check; solid output same-item + count ≤ max — 1:1 (`TileEntityRBMKOutgasser.java:84-108` / `RBMKOutgasserBlockEntity.java:123-141`). Note: the side effect of **setting the gas tank type inside the check** is preserved on both sides. Output comparison: original `getItem()/getItemDamage()` vs port `sameItemSameTags` — [GLUE] (modern component equivalence). |
| process | MATCH | consume 1 input, `progress = 0`, add fluid fill, stack solid output — same (`TileEntityRBMKOutgasser.java:110-129` / `RBMKOutgasserBlockEntity.java:143-157`). Port clamps the gas fill to `maxFill` where the original doesn't — unreachable difference because `canProcess` already guaranteed capacity. |
| Progress reset | MATCH + EXTRA | `if (!canProcess()) progress = 0` in tick — both (`TileEntityRBMKOutgasser.java:62` / port :73-75); the item-swap reset is X6-OUT-3. |
| Heat interaction | MATCH | The outgasser adds **no heat** to the column in either version; it participates in the column heat network purely via `TileEntityRBMKBase`/`RBMKColumnBlockEntity.baseTick` (heat diffusion + passive cooling + ReaSim boil, all 1:1 — see §8). |
| Gas tank | MATCH | 64 000 mB TRITIUM initial (`TileEntityRBMKOutgasser.java:44` / `RBMKOutgasserBlockEntity.java:49`). |
| Fluid sending | MATCH (one branch gap) | Original ports: top `POS_Y` + base `NEG_Y`; close loader (y−1): top + loader all dirs; classic loader (y−2): top + base `NEG_Y` + loader all dirs (`TileEntityRBMKBase.getPorts*`, :612-644). Port `getOutputPos` (`RBMKOutgasserBlockEntity.java:177-199`): top `above(h+1)` + loader dirs / else bottom DOWN — the no-loader and close-loader branches are equivalent, but the **classic-loader branch omits the base `NEG_Y` connection** the original keeps. D6-OUT-2 [DEVIATION, minor]: with a loader two blocks below, a pipe directly under the base can no longer pull gas. |
| Melt | MATCH | 4-5 BLANK debris (`4 + rand.nextInt(2)`) before standard melt (`TileEntityRBMKOutgasser.java:132-141` / `RBMKOutgasserBlockEntity.java:163-169`). |
| Console NBT | MATCH minus one key | Original sends `gas`, `maxGas`, `type` (short fluid ID), `progress` (`TileEntityRBMKOutgasser.java:154-161`); port sends `gas`, `maxGas`, `progress`, plus port-only `maxProgress`/`usedFlux`, but **drops `type`** (`RBMKOutgasserBlockEntity.java:247-256`). D6-OUT-3 [DEVIATION, minor]: the RBMK console cannot read the gas type from the outgasser column anymore. |
| Fluid proxy placement | [GLUE] | Original connects gas via the top extra block being a `TileEntityProxyCombo(true,false,true)` fluid proxy (`RBMKOutgasser.java:18-19`); port exposes the fluid capability on the base BE (`getCapability` FLUID_HANDLER override, :214-224) — attachment point differs, send-targets identical. |

### 6.3 GUI / menu

- **D6-OUT-4 [DEVIATION, cosmetic-but-visible] — overlay geometry vs texture.** The port ships a **pixel-identical copy** of the original `gui_rbmk_outgasser.png` (full 256×256 RGBA pixel diff = 0 bytes; the md5 difference is PNG-container metadata). Yet the port draws: progress bar at GUI `(66,58)` up to **45 px** wide from texture source `(190,0)` (`GUIRBMKOutgasser.java:49-53`), and the gas gauge at `(143, 82−gas)` **14 px** wide, up to **58 px** tall, from source `(176, 58−gas)` (:55-60). The original draws the progress bar at `(82,50)` up to **13 px** from source `(176,0)` and the gas gauge at `(115, 66−gas)` **10 px** wide, up to **42 px** tall, from source `(188, 42−gas)` (`GUIRBMKOutgasser.java:48-52`). Texture analysis of the identical PNG shows the yellow gauge strip lives at ≈x 188-203, y 0-45: the original's source rects hit it exactly, while the port's gas source column (x 176-190, y < 58) contains almost no gauge pixels (5 yellow px in y 0-58 vs 49 in the original's column) and its progress source rect (x 190-235) runs off the artwork past x≈203. Expected in-game result: misplaced progress bar and a gray/short tank gauge. The coordinates look copied from a Community-Edition texture that has extra art regions this texture does not have — same "CE" sourcing issue as D5-STO-1.
- **D6-OUT-5 [COSMETIC] — slot coordinates.** Original container: input `(48,45)`, output `(112,69)` (`ContainerRBMKOutgasser.java:19-20`); port menu: input `(48,53)`, output `(112,53)` (`RBMKOutgasserMenu.java:48,56`). With the identical texture the port's slots will not sit on the texture's slot boxes.
- **[GLUE]** output slot is take-only on both sides (`SlotCraftingOutput` vs `mayPlace → false`); shift-click feeds only the input slot (`mergeItemStack(var5, 0, 1)` vs `moveItemStackTo(stack, 0, 1)`).
- **[GLUE]** original GUI slot is a plain `Slot` (any item placeable by hand; only hopper insertion was recipe-gated via `isItemValidForSlot`, `TileEntityRBMKOutgasser.java:194-196`); port `mayPlace` = `isItemValidForSlot` (recipe-gated, `RBMKOutgasserMenu.java:50-52`) — same strictness class as D5-STO-2.

### 6.4 JEI

| Topic | Verdict | Evidence |
|---|---|---|
| Category | MATCH | Machines `{rbmk_outgasser, fusion_breeder}` (`OutgasserHandler.java:17-18` / `RBMKOutgasserJeiCategory.java:44-48`); fluid output rendered as a fluid icon like `ItemFluidIcon.make` (JEI category :91-97); recipe enumeration 1:1 with the table above. |
| fusionOnly override | [ABSENT] | See A6-OUT-1. |

**Verdict: MINOR** — the recipe table and flux→progress math are exact; the deviations are fluid-connection edge case, console type key, GUI geometry, and surrounding extras.

---

## 7. Cross-cutting: meltdown, NBT, sync

### Meltdown behavior (all six ids)

| Topic | Verdict | Evidence |
|---|---|---|
| Reactor flood-fill | MATCH | 4-neighbor flood fill over connected columns, `onMelt(minDist+1)` per column (`TileEntityRBMKBase.meltdown/getFF`, :439-475, 575-591) = port `meltdownReactor` BFS (`RBMKColumnBlockEntity.java:227-279`). |
| standardMelt | MATCH | `reduce = clamp(reduce,1,h)`, +1 on 1-in-3, layers `i = h..0`: boundary layer `pribris_burning`, below `pribris`, above air (`TileEntityRBMKBase.java:393-416`) = port (`RBMKColumnBlockEntity.java:407-430`), with real pribris-equivalent blocks both sides. |
| Per-type debris | MATCH | blank/absorber/reflector 1-2 BLANK; moderator 2-3 GRAPHITE; outgasser 4-5 BLANK (§1-§6). Concrete-lid columns additionally spawn a LID debris (`TileEntityRBMKBase.onMelt`, :385-391 / `RBMKColumnBlockEntity.onMelt`, :402-405); glass lids vanish silently on both sides; item lid drops are suppressed during meltdown by the `dropLids=false` bracket (original `RBMKBase.dropLids` toggle, :441,568; port try/finally :233, 343-346). |
| Debris physics | **D7-1 [DEVIATION, minor]** | Initial motion: original `motionX/Z = gaussian × 0.25`, `motionY = 0.25 + rand × 1.25` (range 0.25-1.50) (`TileEntityRBMKBase.spawnDebris`, :421-423); port `vx/vz = gaussian × 0.25` ✓ but `vy = 0.5 + rand × 1.5` (range 0.50-2.00) (`RBMKColumnBlockEntity.java:453-455`). LID adjustment (`×0.5` horizontal, `+0.5` up) matches (:425-429 / :456-460). Spawn origin `y + 4.0` matches. Port debris flies visibly higher/faster. |
| Corium infection | MATCH | Rod columns sitting on corium infect their 3×3×3 neighborhood 1-in-3 into `pribris_radiating`/`pribris_digamma` (`TileEntityRBMKBase.java:477-498` / `RBMKColumnBlockEntity.java:284-305`). Port accepts two corium block ids (RBMK_CORIUM + CORIUM_BLOCK) vs original's single `corium_block` — same-block aliasing, [GLUE]. |
| Overpressure | MATCH | `min(pipes/5, 100)` pipe nodes erased, receivers: `IOverpressurable.explode` or air + 5-power explosion, gated on the `dialEnableMeltdownOverpressure` rule (`TileEntityRBMKBase.java:501-539` / `RBMKColumnBlockEntity.handleOverpressure`, :360-400). |
| Cloud / sound / achievement / digamma spear | **D7-2 [DEVIATION, cosmetic]** + MATCH | Sound (`hbm:block.rbmk_explosion`, vol 50, pitch 1), 50-block advancement grant, digamma EntitySpear at y+100 all 1:1 (:545-566 / :313-341). The mushroom-cloud payload differs: original NBT `scale = min(maxX−minX, maxZ−minZ)` via `rbmkmush` AuxParticle (:541-549); port sends `max(...) × 2` as the particle's xd arg through vanilla `sendParticles` (:313-322). Different particle channel and different scale formula — visually a different-sized cloud; cosmetic. |
| Flood-fill safety | **X7-1 [EXTRA]** | Port caps the flood fill at 50 000 columns and skips unloaded chunks (`RBMKColumnBlockEntity.java:251-266`); the authoritative 1.7.10 `getFF` has **no cap and no isLoaded guard** (plain recursion, :575-591). The port comment claims "CE's getFF" has these — not true for this tree. Defensive port-only behavior. |
| `explodeOnBroken` | [GLUE] | Original static flag exists to keep lid metadata writes from triggering rod-block explosions (`RBMKBase.java:228-230`, read only in `RBMKRod.java:47`). Not applicable to the six passive columns; the port handles the equivalent concern inside `setLidState`/filler teardown. |

### NBT keys

| Data | Original | Port | Verdict |
|---|---|---|---|
| heat / reasimWater / reasimSteam | `"heat"`, `"reasimWater"`, `"reasimSteam"` (`TileEntityRBMKBase.java:268-289`) | same keys (`RBMKColumnBlockEntity.java:565-597`) | MATCH |
| Lid state | block **metadata** (`DIR_*_LID.ordinal() + offset`, `RBMKBase.java:161-165`) | BE NBT `"lidState"` int + `setLidState` updating the neutron node (`RBMKColumnBlockEntity.java:500-508`) | [GLUE] (modern blockstate/BE plumbing; legacy worlds fall back to lidded, documented at :45-58) |
| Storage slots | `"items"` + `"slot"` + `"name"` | `"slots"` + `"s"` + `"item"` | [COSMETIC] |
| Outgasser | slots under `"items"`, `"progress"`, tank `"gas"` | `"inputSlot"`/`"outputSlot"`, `"progress"`, `"gas"`, port-only `"lastUsedFlux"` | [COSMETIC] + X6-OUT-4 |
| DODD diagnostic | `getDiagData` static-`diag` bracket (`TileEntityRBMKBase.java:307-311`) | same `diag` mechanism (`RBMKColumnBlockEntity.java:574-588`) | MATCH |

### Sync packets

| Topic | Verdict | Evidence |
|---|---|---|
| Frequency | MATCH | Original packs an NT sync packet **every server tick** with `trackingRange 15` (`TileEntityRBMKBase.updateEntity`, :108-123); port calls `level.sendBlockUpdated` every server tick in `baseTick` (`RBMKColumnBlockEntity.java:85-93`), and `BaseHbmBlockEntity.getUpdateTag` serializes `writeNbtData` — equivalent per-tick cadence. |
| Payload | MATCH/superset | Original per-tick payload: heat, reasimWater, reasimSteam, craneIndicator (+ gas tank, progress for the outgasser; slots are NOT in the packet, `TileEntityRBMKSlottedBase` has no serialize override). Port payload: heat, water, steam, lidState, slots, gas, progress, lastUsedFlux — a superset. craneIndicator is not in the port's per-column payload, but its consumers (console/display NBT collectors) read it server-side on both sides — [GLUE], no behavioral loss found. |

---

## 8. Shared column integration (IRBMKLoadable / IRBMKFluxReceiver / placement / lid & tool interaction)

| Topic | Verdict | Evidence |
|---|---|---|
| IRBMKFluxReceiver | MATCH | Only the outgasser implements it on both sides (`TileEntityRBMKOutgasser.java:34` / `RBMKOutgasserBlockEntity.java:38-40`); storage is `OTHER` and streams pass through. Handler termination rules match: rod (hasRod) → absorb; outgasser (canProcess) → absorb; otherwise pass (`RBMKNeutronHandler.java:312-330` / port :250-255). |
| IRBMKLoadable | MATCH (storage) / X6-OUT-2 (outgasser) | Storage load/unload contract 1:1 (§5). Outgasser loadability is port-only. |
| Open-column radiation leak | MATCH | `if (!hasLid) ChunkRadiationManager.incrementRad(... flux × 0.05)` per streamed column (`RBMKNeutronHandler.java:304-305` / port :241-243); port `hasLid` reproduces the `!isLidRemovable() → always lidded` short-circuit (`RBMKColumnBlockEntity.java:488` ≡ `TileEntityRBMKBase.java:65-75`). |
| Stream shadowing | [GLUE] | Original `getHits` counts `Block.isOpaqueCube()` over `columnHeight` blocks (`RBMKNeutronHandler.java:400-414`; `columnHeight = getColumnHeight+1` set per tick, `NeutronHandler.java:41`); port uses `isSolidRender` with an explicit allowlist for `RBMKColumnBlock`/`RBMKColumnFillerBlock` because modern columns carry `noOcclusion()` (`RBMKNeutronHandler.java:326-331`). `dialColumnHeight` conversion preserved exactly (default 4 → 3 → +1, both sides). |
| Lid placement | MATCH | `ItemRBMKLid.onItemUse`: click column, refuse if lidded, set concrete/glass meta, consume item, glass/concrete placement sound (`ItemRBMKLid.java:22-66`) ≡ `RBMKLidItem.useOn` via `findColumnCore` + `setLidState` (`RBMKLidItem.java:25-47`); the block-use path can also place lids (`RBMKColumnBlock.java:101-105`) — [EXTRA, trivial] convenience over the original which only accepted the item's own use. Creative-mode non-consumption differs from the original (original always decrements) — [DEVIATION, trivial]. |
| Lid removal (screwdriver) | MATCH | Screwdriver on lidded+removable column → drop lid item, clear lid, update neutron node (`RBMKBase.onScrew`, :199-239 ≡ `RBMKColumnBlock.hbmOnUse`, :94-98 + `dropLid`, :266-270). Lid item drops at column top `y + getColumnHeight` on break both sides (`RBMKBase.breakBlock`, :183-196 ≡ `onColumnRemoved`, :259-264). |
| GUI open rules | MATCH | Sneak suppresses GUI; lid-in-hand on a lidless column blocks the original GUI (`RBMKBase.openInv`, :105-125); port: tool → link, screwdriver → remove lid, lid → place, else non-sneak GUI via `MenuProvider` (`RBMKColumnBlock.java:78-114`). Column lid collision (+0.25 on top segment) preserved via the filler (`RBMKBase.getCollisionBoundingBoxFromPool`, :128-148 ≡ `RBMKColumnFillerBlock.getCollisionShape`, :72-86). |
| Column placement | [GLUE] | Original `BlockDummyable` fills `getColumnHeight` dummy segments + top extra (`RBMKBase.fillSpace`, :168-171); port `setPlacedBy`/deferred tick fills `RBMKColumnFillerBlock` for the same height, breaks cascade to the base (`RBMKColumnFillerBlock.playerWillDestroy/onRemove`, :111-169) — matches the dummyable feel. |
| Heat network | MATCH | `moveHeat` neighbor averaging with `columnHeatFlow` step, passive cooling `inner + (max−inner) × (4−neighbors)/4`, 20° floor, ReaSim boil at ≥100° (`TileEntityRBMKBase.java:91-95, 159-233, 256-259, 128-145` ≡ `RBMKColumnBlockEntity.java:122-199`). **D7-3 [DEVIATION, minor]**: ReaSim water/steam rounding remainder — original adds the whole remainder to **self** (`this.reasimWater += rWater`, :222-226); port distributes it one unit per neighbor first (:160-176). Original runs at `min(max−min)` — wait, original `passiveCooling` inner dial has no upper clamp (`getDoubleMinimum`, `RBMKDials.java:92`); port clamps `getPassiveCoolingInner` to [0,1] (`RBMKDials.java:59`) — irrelevant at the 0.1 default, [DEVIATION, trivial]. |

---

## 9. Config gating flags

| Topic | Verdict | Evidence |
|---|---|---|
| Registration gating | MATCH | Neither side gates any of the six blocks behind a config flag — unconditional registration, `machineTab` creative tab (`ModBlocks.java:2122-2128` / `ModBlocks.java:2314-2322`; port tab order 131-150 mirrors `creative_tabs/machineTab.md` rows 130-148 exactly, `CreativeModeTabEventHandler.java:4088-4107`). |
| Dial gamerules | MATCH | Original 25 dial gamerule keys; port registers the boolean/int keys verbatim with identical defaults: `dialReasimBoilers`(false), `dialEnablePermaScrap`(true), `dialDisableMeltdowns`, `dialEnableMeltdownOverpressure`, `dialDisableDepletion`, `dialDisableXenon`, `dialColumnHeight`(4), `dialFluxRange`(5), `dialReasimRange`(10) (`RBMKDials.RBMKKeys`, :17-42 ≡ `RBMKGameRules.register`, :37-57). `dialSaveDials` has no port analog — [GLUE] (modern gamerules persist natively). |
| Float dials | [GLUE] | moderator/absorber/reflector efficiency + absorberHeatConversion + outgasser mod live in the port's config instead of string gamerules (see §2). Defaults and [0,1] clamps preserved. |
| X528 ReaSim boilers | MATCH | `KEY_REASIM_BOILERS \|\| GeneralConfig.enable528ReasimBoilers` (`RBMKDials.java:105`) ≡ `rule(REASIM_BOILERS) \|\| GeneralConfig.reasimBoilers528()` (`RBMKDials.java:76`). |
| Hardness/resistance | MATCH | `setHardness(3F).setResistance(30F)` (1.7.10 stores ×3 → 90; modern scale ÷5 → 18) ≡ `strength(3.0F, 18.0F)` (`RBMKBase.java:52-56` ≡ `ModBlocks.java:2314-2322`). Correct unit conversion. |

---

## Consolidated finding list

| Id | Class | Finding | Severity |
|---|---|---|---|
| D5-STO-1 | DEVIATION | Storage compaction every tick (full pass) vs every 10 ticks (bubble pass); "CE" provenance not in this tree | minor |
| D5-STO-2 | DEVIATION | Storage GUI slots reject non-rod items (original GUI accepted anything) | minor |
| D5-STO-3 | DEVIATION | Menu reach enforced vs original `canInteractWith = true` | trivial |
| D6-OUT-1 | DEVIATION | Ore-dict recipe keys replaced by concrete items; third-party dict items no longer activate | minor |
| D6-OUT-2 | DEVIATION | Classic-loader fluid branch omits the base `NEG_Y` gas connection | minor |
| D6-OUT-3 | DEVIATION | Console NBT dropped the `type` (gas fluid id) key | minor |
| D6-OUT-4 | DEVIATION | GUI progress bar / tank gauge coordinates changed against a pixel-identical texture → expected visual misalignment (port cites "CE" coordinates) | cosmetic (verify in game) |
| D6-OUT-5 | COSMETIC | Menu slot coordinates `(48,45)/(112,69)` → `(48,53)/(112,53)` | cosmetic |
| D7-1 | DEVIATION | Debris initial `motionY` 0.5+1.5·rand vs original 0.25+1.25·rand (shared base) | minor |
| D7-2 | DEVIATION | Meltdown cloud scale `max × 2` via particle args vs original `min` via NBT; different particle channel | cosmetic |
| D7-3 | DEVIATION | ReaSim remainder distribution (neighbors-first) vs original self-takes-all; inner-cooling upper clamp added | minor/trivial |
| A5-STO-1 | ABSENT | Hopper/pipe item automation for storage (original `ISidedInventory` + inventory proxy dummies) | medium |
| A6-OUT-1 | ABSENT | `fusionOnly` flag + `hbmIrradiation.json` runtime recipe loading + NEI/JEI fusionOnly machine override | low (defaults unaffected) |
| A6-OUT-2 | ABSENT | OpenComputers `SimpleComponent` callbacks | low (OC unported project-wide) |
| A6-OUT-3 | ABSENT | Hopper/pipe item automation for outgasser slots `{0,1}` | medium |
| X5-STO-1 | EXTRA | Storage GUI heat readout | cosmetic |
| X6-OUT-1 | EXTRA | `neutronActivateItem` induced-radioactivity branch in outgasser `receiveFlux` (currently unreachable) | low |
| X6-OUT-2 | EXTRA | Outgasser implements `IRBMKLoadable` → crane can service it (original crane skips it) | low |
| X6-OUT-3 | EXTRA | Progress reset on input item type change (closes an original exploit) | low |
| X6-OUT-4 | EXTRA | Console NBT `maxProgress`/`usedFlux`; GUI `Flux` label + SI readout + tooltips | cosmetic |
| X6-OUT-5 | EXTRA | Space-fork Co-60 / Po-210 recipes, gated by `SpaceApi.forkActive()` | low (inactive by default) |
| X7-1 | EXTRA | Meltdown flood-fill 50 000-column cap + unloaded-chunk skip (no analog in this tree) | low (defensive) |
| X7-2 | EXTRA | Lid can also be placed via the block right-click path (original: item use only) | trivial |

Tally: **11 DEVIATION** (incl. 2 cosmetic-severity, 3 trivial), **4 ABSENT**, **8 EXTRA** (incl. 3 cosmetic/trivial). Shared-column findings (D7-1, D7-2, D7-3, X7-1, X7-2) count once each but touch all six ids.

## Recommended follow-ups (not applied — read-only audit)

1. **A5-STO-1 / A6-OUT-3** — wire item automation: override `BaseHbmBlockEntity.getItemHandler` on storage (insert rods-only, extract-all) and outgasser (slot 0 recipe-gated in, slot 1 extract-only), matching original `ISidedInventory` rules; storage dummies originally allowed access from any column segment.
2. **D6-OUT-4 / D6-OUT-5** — either restore the original overlay/slot geometry (`(82,50)` 13 px bar, `(115,66−gas)` 10×42 gauge, slots `(48,45)/(112,69)`) or ship a reworked texture that actually contains the CE-style regions; today the code and the texture disagree.
3. **D7-1** — restore `motionY = 0.25 + rand × 1.25` in `RBMKColumnBlockEntity.spawnDebris`.
4. **D6-OUT-2 / D6-OUT-3** — add the base `NEG_Y` gas connection to the classic-loader branch and the `type` key to `getNBTForConsole`.
5. **D5-STO-1** — decide policy on "CE" provenance: either re-base on this tree (compaction every 10 ticks) or document the deliberate divergence in this report family.
