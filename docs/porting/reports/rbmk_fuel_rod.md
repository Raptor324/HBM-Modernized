# RBMK Fuel Rod — fidelity report (matrix ids `rbmk_fuel_rod`, `rbmk_fuel_rod_reasim`)

- Date: 2026-11-22. Auditor: read-only fidelity audit (agent).
- Original (source of truth): `C:\Projects\Hbm-s-Nuclear-Tech-GIT\src\main\java\com\hbm\` (branch tip `096929e40`).
- Port: `C:\Projects\HBM-Modernized-agents\src\main\java\com\hbm_m\` (working tree, uncommitted state as found).
- Classification legend: `[GLUE]` framework necessity, `[COSMETIC]` strings/naming, `[DEVIATION]` semantic difference,
  `[ABSENT]` original feature missing, `[EXTRA]` port-only addition.

## Scope

| Original | Port |
|---|---|
| `blocks/machine/rbmk/RBMKRod.java`, `RBMKRodReaSim.java` | `block/machines/rbmk/RBMKRodBlock.java` (reaSim flag) |
| `tileentity/machine/rbmk/TileEntityRBMKRod.java`, `TileEntityRBMKRodReaSim.java` | `blockentity/machines/rbmk/RBMKRodBlockEntity.java` (isReaSim branch) |
| `items/machine/ItemRBMKRod.java`, `ItemRBMKPellet.java` | `item/rbmk/RBMKRodItem.java`, `item/rbmk/RBMKPelletItem.java`, `item/RbmkFuelDrxItem.java` |
| `crafting/handlers/RBMKFuelCraftingHandler.java` | `recipe/RBMKFuelDisassemblyRecipe.java` (+ datagen `ModVanillaRecipeProvider.registerRbmkFuelRecipes`) |
| `handler/nei/RBMKRodDisassemblyHandler.java`, `RBMKWasteDecayHandler.java` | `compat/jei/RBMKDisassemblyJeiCategory.java`, `RBMKDisassemblyJeiRecipe.java`, `RBMKWasteDecayJeiCategory.java` |
| `inventory/gui/GUIRBMKRod.java`, `inventory/container/ContainerRBMKRod.java` | `inventory/gui/GUIRBMKRod.java`, `inventory/menu/RBMKRodMenu.java` |
| `crafting/RodRecipes.java` (rod assembly tables) | `datagen/recipes/ModVanillaRecipeProvider.java:393-483` |
| `hazard/HazardRegistry.java` rod rows | `hazard/HazardRegistry.java:861-958` |
| fuel stat blocks in `items/ModItems.java:3029-3340` | `item/ModItems.java:1224-1228, 2455-2496, 3843-4060` |

Fuel definitions in the port live on the pellet item (`RBMKPelletItem` carries the stats, `applyToRod` copies them) and
are re-stated per rod with builder calls in `ModItems`; both sides were compared fuel by fuel. The rod gameplay math
(`burn`/`updateHeat`/`provideHeat`/`reactivityFunc`) lives in `RBMKRodItem` on both sides.

## Verdict summary

| Matrix id | Verdict | One-line summary |
|---|---|---|
| `rbmk_fuel_rod` | **MINOR** | Fuel math, heat model, disassembly, GUI/menu, NBT keys, recipes and hazards are 1:1; three fuel stat blocks diverge numerically (DRX yield 10x, HEAUS function+heat, HEA242 melting point) and the meltdown corium fill honors `reduce` where the original filled the whole column. |
| `rbmk_fuel_rod_reasim` | **MATCH** (1 minor note) | Folded into `RBMKRodBlock(reaSim)`/`RBMKRodBlockEntity.isReaSim()` — a justified structural change; the 8-stream 75 % flux fan with jittered start angle, node-cache eviction and shared xenon/depletion model are 1:1; only the console `FUEL_SIM` column type was collapsed into `FUEL` (documented in port). |

Finding counts (both ids combined): **DEVIATION 6, ABSENT 3, EXTRA 3.**

---

## 1. Fuel item enum/types

- `EnumBurnFunc`: all 9 original constants present with identical formula strings and titles
  (`ItemRBMKRod.java:262-278` vs `RBMKRodItem.java:382-397`). Port adds `SLOW_LINEAR` — `[EXTRA]`, unused dead code.
- `EnumDepleteFunc`: all 5 original constants with identical math (`ItemRBMKRod.java:344-362` vs
  `RBMKRodItem.java:399-403, 209-218`). Port adds `CF_SLOPE` — `[EXTRA]`, unused dead code.
- Burn math `burn()`: xenon burn-then-gen order, poison gating on `RBMKDials.getXenon`, heat-coefficient sine
  multiplier, `reactivityMod` dial, depletion `y -= inFlux` clamp at 0, `coreHeat += outFlux * heat`,
  `rectify` bounds (NaN/<20 → 20, ≤1,000,000) — all 1:1 (`ItemRBMKRod.java:152-206` vs `RBMKRodItem.java:136-163, 231-234`).
- Heat math `updateHeat`/`provideHeat`: core→hull diffusion split at `mid * diffusion * diffusionMod * mod`,
  hull>meltingPoint equalization `avg = (columnHeat+hull+core)/3` with `avg - columnHeat` returned,
  provision `(hull-columnHeat)/2 * fuelHeatProvision * mod` — 1:1 (`ItemRBMKRod.java:212-260` vs `RBMKRodItem.java:165-189`).
- Enrichment/poison/durability: `getEnrichment = getYield/yield`, `getPoisonLevel = poison/100`, durability bar shows
  remaining fuel — 1:1 (`ItemRBMKRod.java:386-396, 562-569` vs `RBMKRodItem.java:318-324, 370-378`; the port documents
  why the modern bar API needs the inverted width).
- Pellets: 10 states (5 enrichment tiers × poisoned/unpoisoned), `disableXenon()` on exactly the same five pellet
  types (po210be, ra226be, balefire, balefire_gold, flashlead), same tooltip tier strings. Original encoded state in
  item damage (`ItemRBMKPellet.java:41-45, 113-119`); port uses an NBT/data-component `pellet_state` int exposed as an
  item-model property (`RBMKPelletItem.java:74-113`) — `[GLUE]` (1.13+ has no metadata).
- Original pellets extend `ItemNuclearWaste` (never despawn, custom fireproof item entity, `ItemNuclearWaste.java:1-24`);
  port pellets set `lifespan = MAX_VALUE` + invulnerable (`RBMKPelletItem.java:142-149`) — `[GLUE]` approximation, same
  observable behavior. Rods correctly have NO waste behavior on either side.
- Container item: original `setContainerItem(rbmk_fuel_empty)` (`ItemRBMKRod.java:75`); port
  `hasCraftingRemainingItem/getCraftingRemainingItem` → `RBMK_FUEL_EMPTY` (`RBMKRodItem.java:103-106`) — `[GLUE]`, same.

### Constants table (original ModItems.java:3077-3340 vs port ModItems.java:1224/3843-3972)

Yield in millions; xenon = gen/burn; heat/diffusion/melt as per item. Defaults shared by both sides: xGen 0.5, xBurn 50,
heat 1.0, diffusion 0.02, depFunc GENTLE_SLOPE, nType SLOW, rType FAST, tint 0x304825 (never used — every fuel sets one).

| Fuel | yield | reactivity(+self) | burnFunc | depFunc | xenon | heat | diff | melt | n/r type | tint | Verdict |
|---|---|---|---|---|---|---|---|---|---|---|---|
| ueu | 100M | 15 | LOG_TEN | RAISING | – | 0.65 | – | 2865 | SLOW/FAST | 868D82 | OK |
| meu | 100M | 20 | LOG_TEN | RAISING | – | 0.65 | – | 2865 | SLOW/FAST | 868D82 | OK |
| heu233 | 100M | 27.5 | LINEAR | – | – | 1.25 | – | 2865 | SLOW/FAST | 868D82 | OK |
| heu235 | 100M | 50 | SQUARE_ROOT | – | – | – | – | 2865 | SLOW/FAST | 868D82 | OK |
| uzh | 50M | 30 | LOG_TEN | GENTLE | – | 0.75 | 0.1 (+heatCoeff 1000/500) | 1845 | SLOW/FAST | 7077AF | OK |
| thmeu | 100M | 20 | PLATEU | BOOSTED | – | 0.65 | – | 3350 | SLOW/FAST | 665448 | OK |
| lep | 100M | 35 | LOG_TEN | RAISING | – | 0.75 | – | 2744 | SLOW/FAST | 656E6B | OK |
| mep | 100M | 35 | SQUARE_ROOT | – | – | – | – | 2744 | SLOW/FAST | 656E6B | OK |
| hep239 | 100M | 30 | LINEAR | – | – | 1.25 | – | 2744 | SLOW/FAST | 656E6B | OK |
| hep241 | 100M | 40 | LINEAR | – | – | 1.75 | – | 2744 | SLOW/FAST | 656E6B | OK |
| lea | 100M | 60 (+10) | SQUARE_ROOT | RAISING | – | 1.5 | – | 2386 | SLOW/FAST | A88A8F | OK |
| mea | 100M | 35 (+20) | ARCH | – | – | 1.75 | – | 2386 | SLOW/FAST | A88A8F | OK |
| hea241 | 100M | 65 (+15) | SQUARE_ROOT | – | – | 1.85 | – | 2386 | FAST/FAST | A88A8F | OK |
| hea242 | 100M | 45 | LINEAR | – | – | 2.0 | – | **2386** | SLOW/FAST | A88A8F | **DEVIATION** (port melt 3386) |
| men | 100M | 30 | SQUARE_ROOT | RAISING | – | 0.75 | – | 2800 | ANY/FAST | 757E73 | OK |
| hen | 100M | 40 | SQUARE_ROOT | – | – | – | – | 2800 | FAST/FAST | 757E73 | OK |
| mox | 100M | 40 | LOG_TEN | RAISING | – | – | – | 2815 | SLOW/FAST | 868D82 | OK |
| les | 100M | 50 | SQUARE_ROOT | – | – | 1.25 | – | 2500 | SLOW/SLOW | 2D9A94 | OK |
| mes | 100M | 75 | ARCH | – | – | 1.5 | – | 2750 | SLOW/FAST | 2D9A94 | OK |
| hes | 100M | 90 | LINEAR | LINEAR | – | 1.75 | – | 3000 | SLOW/FAST | 2D9A94 | OK |
| leaus | 100M | 30 | SIGMOID | LINEAR | 0.05/50 | 1.5 | – | 7029 | SLOW/FAST | FFEE00 | OK |
| heaus | 100M | 35 | **LINEAR** | – | 0.05/50 | **1.5** | – | 5211 | SLOW/FAST | FFEE00 | **DEVIATION** (port SQUARE_ROOT, heat 2.0) |
| po210be | 25M | 0 (+50) | PASSIVE | LINEAR | 0/50 | 0.1 | 0.05 | 1287 | SLOW/SLOW | 563A26 | OK |
| ra226be | 100M | 0 (+20) | PASSIVE | LINEAR | 0/50 | 0.035 | 0.5 | 700 | SLOW/SLOW | B3B6AD | OK |
| pu238be | 50M | 40 (+40) | SQUARE_ROOT | – | – | 0.1 | 0.05 | 1287 | SLOW/SLOW | 656E6B | OK |
| balefire_gold | 100M | 50 (+10) | ARCH | LINEAR | 0/50 | – | – | 2000 | SLOW/FAST | DC9613 | OK |
| flashlead | 250M | 40 (+50) | ARCH | LINEAR | 0/50 | – | – | 2050 | SLOW/FAST | 7B7B87 | OK |
| balefire | 100M | 100 (+35) | LINEAR | – | 0/50 | 3.0 | – | 3652 | SLOW/FAST | B2FF1B | OK |
| zfb_bismuth | 50M | 20 | SQUARE_ROOT | – | – | 1.75 | – | 2744 | SLOW/FAST | AAA36A | OK |
| zfb_pu241 | 50M | 20 | SQUARE_ROOT | – | – | – | – | 2865 | SLOW/FAST | AAA36A | OK |
| zfb_am_mix | 50M | 20 | LINEAR | – | – | 1.75 | – | 2744 | SLOW/FAST | AAA36A | OK |
| drx (digamma) | **10M** | 1000 (+10) | QUADRATIC | – | – | 0.1 | – | 100000 | SLOW/FAST | D77276 | **DEVIATION** (port yield 100M) |
| test | 1M | 100 | EXPERIMENTAL | – | – | 1.0 | – | 100000 | SLOW/FAST | default | OK (no pellet, cannot disassemble — both sides) |

All 33 original fuels are present in the port (plus `rbmk_fuel_empty`); nothing added, nothing dropped. DRX is a real,
loadable `RBMKRodItem` subclass in the port (it was previously a plain flavor item — fixed), with the original's
`trait.rbmx.*` tooltip variant, `.x` neutron-type names and "m" units preserved (`RbmkFuelDrxItem.java:33-70`).

---

## 2. Rod BE tick

`TileEntityRBMKRod.updateEntity` (`:117-197`) vs `RBMKRodBlockEntity.tick` (`:49-111`) — 1:1:

- Satellite RayScan `INFO_NUCLEAR, 300` on `fluxQuantity > 0` every 200 ticks — both (`:123-124` / `:53-55`).
- `rodColor` from item tint — both (`:127` / `:57`).
- Flux out path: special-curve rods use `fluxRatioOut`/`fluxFromRatio` + `burn`; normal rods use
  `rType == SLOW ? 0 : 1` and `fluxFromType(nType)` (SLOW = slow + 0.5·fast, FAST = fast + 0.3·slow, ANY = all) —
  both (`:134-147, 199-211` / `:60-66, 118-126`).
- `updateHeat(…, 1.0)` then `heat += provideHeat(…)` — both (`:149-150` / `:69-70`).
- Lidless channel leaks `fluxQuantity * 0.05` radiation — both (`:152-154` / `:76-78`).
- `super.updateEntity()` (column heat movement) runs before the meltdown check; meltdowns-disabled dial vents
  particles at column top instead; `heat` clamped to 10,000; flux state reset; `spreadFlux(out, ratio)`; `hasRod` —
  all 1:1 (`:156-183` / `:80-104`).
- `receiveFlux` fast-ratio blending — 1:1 (`:86-92` / `:131-136`; port adds a divide-by-zero guard — `[GLUE]`).
- Spread: 4 cardinal streams (`:213-248` / `:39-41, 162-163`).
- `coldEnoughForAutoloader` hull ≤ 1000, `coldEnoughForManual` hull ≤ 200 — 1:1 (`:94-105` / `:212-223`).
- Hot-column break → meltdown gate `hullHeat >= 1500 && !meltdownsDisabled` (original `RBMKRod.breakBlock:44-55`
  + `invalidate():108-114`; port `RBMKRodBlock.onColumnRemoved:97-111`). The original's `explodeOnBroken` is a static
  always reset to true (`TileEntityRBMKBase.java:63`, toggled only transiently in `RBMKBase.java:228-230` and
  `ItemRBMKLid.java:57-59`); the port made it a persisted per-BE field defaulting true — `[GLUE]`, same outcome.
- `onMelt`: fuelled channel → corium + FUEL debris `1 + rand(h)`; unfuelled → `standardMelt(reduce)` with
  `reduce = clamp(1..h)`, `+1` on `rand.nextInt(3)==0`; moderated → GRAPHITE debris `2 + rand(2)`; ELEMENT debris
  always; LID debris when lidded; digamma fuel sets the `digamma` flag consumed by the meltdown sweep — all 1:1
  (`TileEntityRBMKRod.java:328-373`, `TileEntityRBMKBase.java:385-412, 470-514` vs
  `RBMKRodBlockEntity.java:174-207`, `RBMKColumnBlockEntity.java:204-345`).

**[DEVIATION] Corium depth**: original fills the *entire* column with corium regardless of `reduce`
(`TileEntityRBMKRod.java:346-349`, loop `i = h..0` unconditional); the port fills corium only up to
`i <= h + 1 - reduce` and air above (`RBMKRodBlockEntity.java:184-196`). The port comment rationalizes this as the
"crater profile", but that profile belongs to `standardMelt` (non-fuel columns), not to the corium path. Meltdowns of
fuelled channels now produce a shallower corium slab when the column sits deep in the reactor. Also original places
corium with meta 5, port uses `defaultBlockState()` — `[COSMETIC]` unless corium meta carries state (see
`block_graphite`/corium report).

---

## 3. GUI / menu

- Slots: fuel slot at (80, 45); player inventory rows at y 104/122/140; hotbar at y 162 — identical coordinates
  (`ContainerRBMKRod.java:18-28` vs `RBMKRodMenu.java:41-62`).
- GUI texture `textures/gui/reactors/gui_rbmk_element.png`, size 176×186 — same
  (`GUIRBMKRod.java:17, 24-25` vs `GUIRBMKRod.java:23-33`).
- Depletion bar (bg 18×67 at 34,21; fill from tex (194,0), height `depletion*67`) and xenon bar (bottom-up, tex
  (212,58-x), `x = poison*58`) — identical UVs (`GUIRBMKRod.java:52-61` vs port `:47-55`).
- Hot-rod info panels at (-16,20) and (-16,36) with the same two tooltip strings — identical
  (`GUIRBMKRod.java:32-36, 64-65` vs port `:58-75`).
- Slot interaction lock: original `slotClick` blocks all interaction with slot 0 when rod hotter than 200 °C
  (non-creative) (`ContainerRBMKRod.java:32-49`); port `clicked()` blocks the same (`RBMKRodMenu.java:108-111`).
  Shift-click out also gated on `coldEnoughForManual` — 1:1 (`ContainerRBMKRod.java:60-64` vs `RBMKRodMenu.java:122-124`).
- **[DEVIATION]** The original fuel slot is a plain `Slot` that accepts *any* item
  (`ContainerRBMKRod.java:18`); the port restricts `mayPlace` to `RBMKRodItem` (`RBMKRodMenu.java:42-45`). Stricter
  than the original; blocks the "junk in the fuel channel" quirk. Defensible bugfix, but a behavioral difference.
- **[EXTRA]** `broadcastChanges()` re-syncs the fuel slot every tick so live heat/xenon tooltips update while the GUI
  is open (`RBMKRodMenu.java:86-95`); the original's slot only saw the snapshot taken at open time.
- Block interaction: rod insertion by right-click with a rod into an empty channel, count clamped to 1, stack
  decrement, `hbm:item.upgradePlug` sound — 1:1 (`RBMKRod.java:82-88` vs `RBMKRodBlock.java:80-88`; sound event
  renamed `hbm_m:tool.upgrade_plug` — `[COSMETIC]`). GUI opens on non-sneak use via the shared column handler.
  The original's `ItemRBMKLid`-in-hand gate (`RBMKRod.java:78-80`) is preserved by lid item/block handling in
  `RBMKColumnBlock.use` (`:93-99`) — `[GLUE]`.
- `BossSpawnHandler.markFBI` on every activation — both (`RBMKRod.java:67` / `RBMKRodBlock.java:72`).

---

## 4. ReaSim rod (`rbmk_fuel_rod_reasim`)

Original structure: `RBMKRodReaSim` block subclass only swaps the TE (`RBMKRodReaSim.java:16-20`);
`TileEntityRBMKRodReaSim` overrides name (`container.rbmkReaSim`), console type `FUEL_SIM` (`:53-55`) and
`spreadFlux` (`:26-50`): 8 streams at `flux * 0.75`, 45° apart, whole fan jittered by `rand.nextInt(4) * 9°`,
direction uncorrelated with block axes.

Port: single block/BE pair with a `reaSim` flag (`RBMKRodBlock.java:28-48`, `RBMKRodBlockEntity.java:145-169`),
registered as 4 blocks (`rbmk_element`, `rbmk_element_mod`, `rbmk_element_reasim`, `rbmk_element_reasim_mod`,
`ModBlocks.java:2300-2304`, one BE type covering all, `ModBlockEntities.java:1774-1779`). ReaSim is *not* a separate
BE class; the fan spread is an `isReaSim()` branch in `spreadFlux`.

- Fan spread: `angle = rand.nextInt(4) * 9`, 8 streams, `flux * 0.75`, `angle += 45` — 1:1
  (`TileEntityRBMKRodReaSim.java:44-49` vs `RBMKRodBlockEntity.java:151-159`).
- Per-cell moderation during the stream walk: `MODERATOR || isModerated()` increments `moderatedCount` and calls
  `moderateStream()` (`fluxRatio *= 1 - moderatorEfficiency`); reflector re-applies moderation on return; control-rod
  attenuation; absorber heat conversion; tail control-rod pass (issue #1933) — all 1:1
  (`RBMKNeutronHandler.java:205-301, 347-350` (port) vs original `RBMKNeutronHandler.java:232-426`).
- Node-cache eviction: dead normal rod evicts its 4 stream paths; dead ReaSim rod evicts the whole `fluxRange` disc
  (`x²+z² ≤ range²`); non-rod nodes evicted unless a live rod is inside the ReaSim disc or on a cardinal stream path —
  1:1 (original `RBMKNeutronHandler.java:120-190` vs port `RBMKNeutronHandler.java:93-140, 77-86`).
- Xenon/depletion model: original ReaSim rods inherit `ItemRBMKRod.burn()` unchanged — there is NO separate ReaSim
  xenon or depletion model in 1.7.10; the port shares `RBMKRodItem.burn()` the same way. Match.
- Flux range for ReaSim fan geometry: original uses `RBMKDials` `dialReasimRange` (default 10, clamped 1..100) only in
  the cache-eviction logic; the fan itself is fixed 8×45°. Port identical (`RBMKDials.java:32, 104, 282-284` vs
  `RBMKDials.java:35, 74`).
- Structure change assessment: **justified**. The original's two block classes + two BE classes differ only in the
  `spreadFlux` body and two display strings; the port's flag-based fold is behaviorally equivalent and keeps
  registration 1:1 (all four block variants exist, ReaSim is available unconditionally — no config gate added, unlike
  the ReaSim *boiler* dial which is config-gated on both sides, see §7).
- **[DEVIATION]** Console column type: original ReaSim rods report `ColumnType.FUEL_SIM` (console icon offset 90,
  `TileEntityRBMKConsole.java:463, 505`); the port's `ColumnType` enum collapsed `FUEL_SIM` away and the rod always
  reports `FUEL` (`RBMKRodBlockEntity.java:256`, `RBMKColumnBlockEntity.java:513-515`), so a ReaSim column draws with
  the normal fuel icon on consoles/displays. Documented in `GUIMachineRbmkConsole.java:47-50`. Display-only.
- **[COSMETIC]** BE display name shared (`block.hbm_m.rbmk_element`) vs original `container.rbmkRod` /
  `container.rbmkReaSim`.

---

## 5. Disassembly recipe + waste decay handler

- Disassembly (`RBMKFuelCraftingHandler.java` vs `RBMKFuelDisassemblyRecipe.java`): exactly-one-stack rule, rod must
  have a pellet, `hull < 50 && core < 50` gate in `matches`, `enrichment > 0.99` refusal, output 8 pellets,
  tier = `4 - clamp(ceil(enrichment*5 - 1), 0, 4)`, `+5` when `poisonLevel >= 0.5` — 1:1
  (`:17-61` vs `:41-78`). Port additionally suppresses the `+5` state for `disableXenon` pellets — unreachable in
  practice (their xenon is 0) — `[COSMETIC]`.
- Container item: original returns the empty casing via `setContainerItem`; port returns it via
  `getRemainingItems` (`:94-109`) plus the item-level hook — same net result (8 pellets + casing).
- JEI listing (`RBMKRodDisassemblyHandler.java` vs `RBMKDisassemblyJeiRecipe.java`): per craftable rod × 5 tiers,
  xenon variant when the pellet has one; displayed rod yield `min(1 - tier/5, 0.99) * yield`, poisoned variants carry
  50 poison, core/hull 20 — 1:1 (`:23-39, 69-82` vs `:25-56`). Original's hot-rod usage filter (`:43-49`) is a NEI
  dynamic-query artifact and has no static-listing equivalent — `[COSMETIC]`. NEI id `ntmRBMKDisassembly` → JEI
  `hbm_m:rbmk_disassembly` — `[COSMETIC]`.
- Waste decay (`RBMKWasteDecayHandler.java` vs `RBMKWasteDecayJeiCategory.java`): short/long waste × classes →
  depleted variants, per class, fresh/depleted lists kept in the same class order (8 short classes, 5 long classes —
  original `ItemWasteShort.WasteClass:44-54`, port `PartTabMetaItems.java:415-468`), plus tiny variants; machine
  icon `machine_storage_drum` — 1:1 as a listing. The actual decay simulation lives in the storage drum BE
  (out of scope here).

### Rod assembly / datagen recipes (original `crafting/RodRecipes.java` vs port `ModVanillaRecipeProvider.java:393-483`)

- Empty casing: shaped `ZRZ / Z Z / ZRZ` with zirconium ingots + `rod_quad_empty` — 1:1 (`RodRecipes.java:89` /
  `ModVanillaRecipeProvider.java:393-400`).
- Every fuel: shapeless `rbmk_fuel_empty + 8× billet` — 1:1 for all 31 billet fuels; billet mapping matches row for
  row (e.g. ueu→uranium billet, meu→uranium_fuel billet, hen→neptunium billet, heaus→australium_greater billet)
  (`RodRecipes.java:90-120, 242-247` / `ModVanillaRecipeProvider.java:403-433, 477-483`).
- DRX: shapeless `rbmk_fuel_balefire + particle_digamma` — 1:1 (`RodRecipes.java:121` / `:436-441`).
- Disassembly registered as a special recipe — matches original IRecipe-handler registration.
- Pellet→rod crafting does not exist on either side (pellets come only from disassembly/SILEX/fuel pool). Match.

---

## 6. NBT keys persisted

| Data | Original | Port | Verdict |
|---|---|---|---|
| fuel yield / xenon / core / hull | item tag `yield`, `xenon`, `core`, `hull` (`ItemRBMKRod.java:524-597`) | same keys, NBT on 1.20.1, `CUSTOM_DATA` component on 1.21.1 (`RBMKRodItem.java:269-316`) | 1:1 keys; component move = `[GLUE]` |
| fuel slot | `slots[0]` item list (`TileEntityRBMKSlottedBase.java:135-166`) | `fuelSlot` compound (`RBMKRodBlockEntity.java:299, 315-316`) | `[GLUE]` (renamed, single slot) |
| flux state | `fluxQuantity`/`fluxMod` (normal), `fluxSlow`+`fluxFast` (diag) (`:269-280`) | same keys, same diag branch (`:287-300`) | 1:1 |
| `hasRod` | boolean (`:279`) | boolean (`:296`) | 1:1 |
| `explodeOnBroken` | static field, never persisted | persisted boolean, default true (`:298, 314`) | `[GLUE]` |
| read-back of legacy `fluxFast`/`fluxSlow` saves | handled (`:254-261`) | **not handled** — port reads only `fluxQuantity`/`fluxMod` (`:303-317`) | **[ABSENT]** (minor; only matters for hand-migrated 1.7.10 saves) |
| pellet state | item damage meta 0-9 | `pellet_state` int NBT/component | `[GLUE]` |
| client sync of fuel strings (`f_yield`/`f_xenon`/`f_heat` formats) | serialize() strings + `getDiagData` (`:282-325`) | DODD diag strings identical format, full stack synced to client (`:269-280`) | 1:1 output; transport `[GLUE]` |
| console NBT keys `enrichment/xenon/c_heat/c_coreHeat/c_maxHeat` | (`:386-400`) | same (`:228-238`) | 1:1 |

---

## 7. Config gating flags

- Xenon/depletion dials: gamerules `dialDisableXenon` / `dialDisableDepletion` (original `RBMKDials.java:40-41, 113-114`)
  → port registers the same gamerule names (`RBMKGameRules.java:45-47`) and negates at the getter
  (`RBMKDials.java:84-85`). Match.
- Reactivity mod / fuel diffusion / heat provision: original gamerule doubles (`RBMKDials.java:22-23, 94-95`);
  port keeps them as server config doubles under `rbmkDials.*` (`ConfigSchema.java:309-310, 313`) with the original's
  point-of-use clamps restored (`RBMKDials.java:58-62, 94-96`). Vanilla gamerules cannot carry doubles — `[GLUE]`.
  Note the config schema allows 0..100 for `heatProvision` where the original gamerule clamped 0..1; the use-site
  clamp to 0..1 still applies, so behavior is unchanged — `[COSMETIC]`.
- Column height / flux range / meltdowns disabled / overpressure / permanent scrap: same gamerule names and clamp
  ranges (original `:24-25, 35-36, 96-107` vs port `RBMKGameRules.java:21-56`).
- ReaSim boiler config: `GeneralConfig.enable528ReasimBoilers` (`GeneralConfig.java:142`) → port
  `GeneralConfig.reasimBoilers528()` OR'd with the gamerule (`RBMKDials.java:76` on both sides). Match.
- The rods themselves are not config-gated on either side (both rod and ReaSim rod block families always registered).

---

## Consolidated findings

### DEVIATION (6)

1. **DRX yield 100,000,000 vs 10,000,000** — 10× fuel lifetime. Port comment even states the correct value.
   Port `item/ModItems.java:1226` vs original `items/ModItems.java:3320`.
2. **HEAUS burn function SQUARE_ROOT + heat 2.0 vs LINEAR + heat 1.5**. Port `item/ModItems.java:3867-3868` vs
   original `items/ModItems.java:3240-3242`.
3. **HEA242 melting point 3386 vs 2386** (port pellet for the same fuel has the correct 2386).
   Port `item/ModItems.java:3864` vs original `items/ModItems.java:3181`.
4. **Corium fill honors the `reduce` cutoff** (air above `h+1-reduce`) instead of the original's unconditional
   full-column corium. Port `blockentity/machines/rbmk/RBMKRodBlockEntity.java:190-193` vs original
   `tileentity/machine/rbmk/TileEntityRBMKRod.java:346-349`.
5. **`ColumnType.FUEL_SIM` collapsed** — ReaSim columns display as normal fuel on consoles/displays.
   Port `RBMKRodBlockEntity.java:256` + `RBMKColumnBlockEntity.java:513-515` vs original
   `TileEntityRBMKRodReaSim.java:53-55` + `TileEntityRBMKConsole.java:505`. (Filed under reasim.)
6. **Fuel slot rejects non-rod items** (`mayPlace` restriction) where the original's plain slot accepted anything.
   Port `inventory/menu/RBMKRodMenu.java:42-45` vs original `inventory/container/ContainerRBMKRod.java:18`.

### ABSENT (3) — all justified or minor

1. OpenComputers integration (SimpleComponent + ~12 callbacks, `TileEntityRBMKRod.java:429-530`) — OC does not exist
   on 1.20.1/1.21.1; justified API gap.
2. EnergyControl info provider (`IInfoProviderEC.provideExtraInfo`, `:543-552`) — same justification.
3. Legacy save migration branch reading `fluxFast`/`fluxSlow` keys (`:254-261`) — port never reads those keys;
   only affects hand-ported 1.7.10 worlds / diag round-trips.

### EXTRA (3)

1. `broadcastChanges` live fuel-slot re-sync in the menu (`RBMKRodMenu.java:86-95`) — live tooltips in GUI.
2. Broken fuel channel pops the rod as an item when it is not melting down (`RBMKRodBlock.java:106-108`); the
   original silently destroyed the slot content (`RBMKBase.breakBlock:183-196` drops only lids).
3. Unused enum entries `SLOW_LINEAR` / `CF_SLOPE` in `RBMKRodItem` (`:392, 402`) — dead code until NTM Space fuels
   arrive.

### GLUE (selected)

1.21.1 data components for rod/pellet state; pellet meta → `pellet_state` int + model property; `CustomRecipe`/
`SimpleCraftingRecipeSerializer` for the disassembly handler; Architectury menu registration + `SimpleContainer`
bridge; static `tick` + `Direction[]` instead of `updateEntity`/`ForgeDirection`; durability-bar width inversion;
`explodeOnBroken` static → persisted instance field; `EntityItemWaste` → invulnerable+lifespan on the item entity;
JEI replaces NEI; dial doubles move to mod config; container item via `getCraftingRemainingItem`.

### COSMETIC (selected)

- Rod `fullName` strings: port appends "Rod" and renames some ("High-Enriched" vs "Highly Enriched", MOX →
  "Mixed Oxide Fuel Rod", DRX flavor name → "Digamma Fuel Rod", DRX pellet loses the OBFUSCATED formatting) —
  the original rod inherits the pellet's `fullName` verbatim.
- Block registry names `rbmk_element*` vs original `rbmk_rod*` (texture path `rbmk/rbmk_element` preserved).
- Lang key `trait.rbmx.melt` "Crushing depth: %s" vs original "Crush depth: %s".
- Tooltip numbers: original truncates via `(int)` casts, port formats with `%.3f`/`%.1f` (rounding).
- Sound `hbm:item.upgradePlug` → `hbm_m:tool.upgrade_plug`; JEI category ids renamed; BE name key shared.

## Recommended fixes (in priority order)

1. `item/ModItems.java:1226` — DRX yield → `10_000_000` (match original `ModItems.java:3320`).
2. `item/ModItems.java:3867-3868` — HEAUS → `setFunction(LINEAR)`, `setHeat(1.5)`.
3. `item/ModItems.java:3864` — HEA242 `setMeltingPoint(2386)`.
4. Decide policy on the corium `reduce` cutoff (restore unconditional full-column corium or document as deliberate
   change in this report + matrix).
