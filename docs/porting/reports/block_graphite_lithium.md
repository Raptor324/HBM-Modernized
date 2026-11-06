# block_graphite_lithium — port report

**Machine:** `block_graphite_lithium` — CP-1 lithium breeding fuel block ("Литиевый топливный блок (наработка трития)")
**Result:** COMPLETE — re-audit judge verdict COMPLETE in round 1 (cap 3). Matrix entry set to `PORTED`.
**Runs (branch `machines`):**
- Full port: dynamic workflow `dwfrun-8ea7f251-ed7e-4a36-aa25-a91868635535` (7 subagents, ~40 min), finished 2026-10-09, committed as `77992fdb`.
- Re-audit: dynamic workflow `dwfrun-24ccc94b-1585-4f08-b28c-de6095c888a8` (research → audit → fix → verify → judge, max 3 concurrent subagents), finished 2026-10-09 — this session.
**Source:** NTM NEXT `C:\Projects\ntm-next` (neoforge tree): `com/hbm/blocks/machine/pile/BlockGraphiteBreedingFuel.java`, `com/hbm/tileentity/machine/pile/BlockEntityPileBreedingFuel.java`. 1.7.10 reference repo available but NEXT is the behavior authority.

## Re-audit session (2026-10-09) — parity audit, verdict COMPLETE

**Mode: parity audit.** After the port commit landed, the orchestrator reset the matrix entry to `IN_PROGRESS`, so this session re-audited the committed state against NEXT instead of porting from scratch. The workflow ran two parallel researchers (NEXT sweep, Modernized sweep), a line-level parity auditor producing an evidence-backed discrepancy list, a scoped fixer, compile gates on both loaders, three independent fresh reviewers (game parity / resources & registration / multiloader-stonecutter), and an acceptance judge. One round sufficed.

The core machine logic (block + tile entity: tick order, `react()`/`receiveNeutrons`, maxProgress math, completion swap to `BLOCK_GRAPHITE_TRITIUM`, NBT persistence, tool dispatch, hand-drill readout, flammability 30/5, ticker wiring, neutron emission 2 × flux-1 range-5 step-0.5) was confirmed **parity-exact** — all six findings were in surrounding registration/config/lang surface.

### Findings and dispositions (6)

1. **`rod-item-class` (medium) — FIXED.** NEXT registers `PILE_ROD_LITHIUM` as an `ItemPileRod` (`ModItems.java:2154`, factory `rod()` :11061-11065) whose tooltip renders `desc.item.pileRod` + `<descriptionId>.desc` (`ItemPileRod.java:29-32`), with NEXT `en_us.json:7099` carrying `"§a[Breedable Fuel]\n§eUse hand drill to inspect core (LEGACY)"`. Modernized still registered a plain `Item` (`ModItems.java:3286`) even though `ItemPileRod` existed in-tree and was already used by the boron/detector rods (`:3284-3285`); both lore keys were absent. Fixed: registration now uses `com.hbm_m.item.machine.ItemPileRod` and the NEXT-verbatim lore key `item.hbm_m.pile_rod_lithium.desc` was added in `ModLanguageProviderEn`; verified in generated `en_us.json:3950-3951` after `runData`.
2. **`rod-name-legacy-suffix` (low) — FIXED.** NEXT `en_us.json:7098` names the rod `"Chicago Pile Lithium Cell (LEGACY)"`; Modernized lacked the suffix while the block name kept it. Fixed: `" (LEGACY)"` appended in `ModLanguageProviderEn` (`:5127`). Sibling legacy rods (boron/detector) deliberately untouched — other machines' scope.
3. **`enable528-dial-dead` (medium) — FIXED.** NEXT's `BalanceConfig.enable528` is user-settable (`BalanceConfig.java:8`, loaded from its config schema `ConfigSchema.java:178`); Modernized's `GeneralConfig.enable528` was a bare `false` field with **no assignment site anywhere** — the 50000-progress 528-mode branch in `BlockEntityPileBreedingFuel.java:38` was dead code. Fixed end to end: `ModClothConfig.enable528` POJO field, `ConfigSchema` registration (`ConfigField.bool("enable528", SERVER, LIVE, "general")` with NEXT's exact comment), and `GeneralConfig.syncFrom(ModClothConfig)` invoked from `ConfigSchema.applyAll` — the single funnel all four application paths (JSON store load, config screen, `ConfigEditC2SPacket`, `ConfigSyncS2CPacket`) go through. Assignment now at `GeneralConfig.java:21`; config-GUI name+tooltip lang keys added in both providers.
4. **`creative-tab-placement` (low) — REJECTED (documented house decision).** NEXT ships both block and rod in no creative tab (1.7.10 `setCreativeTab(null)` legacy items); Modernized lists them in the machines/dev tabs. Accepted as adaptation #8 below: the whole CP-1 pile family sits in the same tab behind an inline divergence comment; dropping only the lithium lines would split it from its family.
5. **`tool-durability` (low) — REJECTED (project-wide convention, out of machine scope).** NEXT's `ItemTooling.onScrew` (`ItemTooling.java:55-59`) hurts the tool by 1 after every consumed interaction; Modernized never damages tools, documented project-wide at `BlockGraphiteDrilledBase.java:112-113`. Applying it only to this block would make tool behavior inconsistent within the pile family; the real fix is a project-wide convention change.
6. **`ru-readout-translations-extra` (low) — KEPT (deliberate improvement).** Modernized `ru_ru` adds `desc.shared.cp1FuelAssembly`/`desc.shared.flux` entries NEXT's `ru_ru` lacks (NEXT falls back to en_us). Kept as adaptation #9 below: they are shared keys whose surrounding best-effort CP-1 ru section belongs to sibling machines' workers; deleting them mid-round would churn other machines' locale display.

### Files touched this session

- `src/main/java/com/hbm_m/item/ModItems.java` — `PILE_ROD_LITHIUM` → `ItemPileRod` (one line).
- `src/main/java/com/hbm_m/datagen/ModLanguageProviderEn.java` — rod display name `(LEGACY)` suffix, `pile_rod_lithium.desc` lore key, config-GUI keys for `enable528`.
- `src/main/java/com/hbm_m/datagen/ModLanguageProviderRu.java` — config-GUI keys for `enable528`.
- `src/main/java/com/hbm_m/config/ModClothConfig.java` — `enable528` POJO field.
- `src/main/java/com/hbm_m/config/schema/ConfigSchema.java` — `enable528` field registration.
- `src/main/java/com/hbm_m/config/GeneralConfig.java` — `syncFrom` assignment site.
- `src/generated/resources/assets/hbm_m/lang/en_us.json`, `ru_ru.json` — regenerated via `runData` (exit 0).
- `docs/porting/reports/block_graphite_lithium.md` — this report.

### Deliberate quirks re-confirmed (NEXT-verbatim, NOT fixed — by design)

- `lastNeutrons` is deliberately NOT persisted (NEXT saves only `progress` + `neutrons`), so the hand-drill FLUX readout reads 0 after save/reload until the BE is hit again.
- `react()` emits exactly 2 rays of flux 1, and only when `lastNeutrons > 0`; directions are a `Vec3(1,0,0)` rotated by three uniform random 2π Euler angles. The 1-tick stream latency (rays queued during BE ticks, drained by `NeutronHandler` after all BE ticks) is load-bearing for critical masses.
- A SHROUDED breeding fuel cannot be un-shrouded in place: screwdriver extraction carries `SHROUDED=true` onto `BLOCK_GRAPHITE_DRILLED` and ejects only the lithium rod; the aluminum shell is recovered only by later un-shrouding or by breaking (loot pool 2 gated on `shrouded=true`).
- The completion swap fires the same tickServer in which `react()` pushes progress past the threshold, with no drops; the swap is `hasProperty(AXIS)`-guarded for the AXIS-less tritium stub (auto-upgrades when tritium lands).
- `ru_ru` intentionally has NO block/item display names for this machine (NEXT parity) so both locales fall back to `en_us`.
- `hbm_m` `pile/flux_absorber` tag intentionally empty (NEXT lists only `block_boron`, which has no Modernized analog) and `pile/flux_attenuator` omits `concrete_smooth` (block does not exist in Modernized).
- The screwdriver/hand-drill click is consumed (`sidedSuccess`) for any recognized tool on any face, matching NEXT's unconditional `return true`; non-tool items PASS (net-equal).

### Verification evidence (this session)

- `gradlew :1.20.1-forge:compileJava` — exit 0 (round 1, run by the workflow as a deterministic gate).
- `gradlew :1.21.1-neoforge:compileJava` — exit 0 (round 1, run by the workflow as a deterministic gate).
- `gradlew :1.20.1-forge:runData` — exit 0 (run by the fixer; generated lang outputs confirmed on disk).
- Three independent fresh reviewers (round 1): **game parity** PASS, **resources/registration** PASS, **multiloader/stonecutter-rules** PASS — each read both trees with path:line evidence.
- Acceptance judge (fresh, round 1): **COMPLETE**, zero blocking issues.

## Original full port (run `dwfrun-8ea7f251`, committed `77992fdb`)

**Full port.** Pre-port probe (researcher greps over all of `src/`: java, resources, generated) found zero breeding-fuel code in Modernized: no `BreedingFuel`/`PILE_BREEDINGFUEL` classes, and `ModBlocks.BLOCK_GRAPHITE_LITHIUM` was a deliberate plain-`Block` stub (`new Block(BlockProps.copy(Blocks.STONE))`) alongside bare textures. Everything the machine needs on the Modernized side already existed from earlier pile ports: the neutron-transport system (`com.hbm_m.handler.neutron.*`), `IPileNeutronReceiver`, the `BlockEntityPileBase` abstract (tickServer/castRay), the drilled-channel block lineage, and the registered-but-plain `PILE_ROD_LITHIUM` item.

### New files (2)

| File | Content |
|---|---|
| `src/main/java/com/hbm_m/block/machines/pile/BlockGraphiteBreedingFuel.java` | 1:1 with NEXT `BlockGraphiteBreedingFuel`: `newBlockEntity` → breeding BE; screwdriver on a channel face reverts to `BLOCK_GRAPHITE_DRILLED` via `copyShape` + ejects `PILE_ROD_LITHIUM` (`ejectItem`); hand drill prints `desc.shared.cp1FuelAssembly` (gold) + literal `DEPLETION: progress/maxProgress()` (yellow) + `desc.shared.flux` (yellow); `getInsertedItem()` = `PILE_ROD_LITHIUM`; flammable 30/5 per NEXT `FlammableBlocks.java:22` (override pair, as the fuel port — direct `FireBlock#setFlammable` is not portable). Own `getTicker` override mapping `PILE_BREEDINGFUEL` → `tickServer` (the base `BlockGraphiteDrilledTE` ticker type-checks only the detector and would silently never tick — same as `BlockGraphiteFuel`). Version-gated use hook: `use` on <1.21.1, `useWithoutItem` hand loop on 1.21.1 (NEXT dispatches per hand; the 1.21.1 hook has no hand parameter, so both hands are tried on PASS). |
| `src/main/java/com/hbm_m/blockentity/machines/pile/BlockEntityPileBreedingFuel.java` | 1:1 with NEXT `BlockEntityPileBreedingFuel`: public `neutrons`/`lastNeutrons`/`progress`; `maxProgress()` = `enable528 ? 50000 : 30000`; `tickServer` = `react()` then completion swap to `BLOCK_GRAPHITE_TRITIUM`; `react()` = `lastNeutrons = neutrons; progress += neutrons; neutrons = 0;` then 2 × `castRay(1)` when `lastNeutrons > 0`; `receiveNeutrons` accumulates; NBT persists exactly `progress` + `neutrons` (NOT `lastNeutrons`, NEXT-verbatim), stonecutter-gated `saveAdditional`/`load` (<1.21.1 `CompoundTag`) vs `saveAdditional`/`loadAdditional` (`Provider`) on 1.21.1. |

### Shared-file edits (9, all surgical)

- `ModBlocks.java` — `BLOCK_GRAPHITE_LITHIUM` stub swapped to the real class with the pile-family properties (`IRON_BLOCK` copy, strength 5.0F/6.0F, METAL, `requiresCorrectToolForDrops`).
- `ModBlockEntities.java` — `PILE_BREEDINGFUEL` registered with NEXT's exact registry string `"pile_breedingfuel"`, valid block = `BLOCK_GRAPHITE_LITHIUM`.
- `BlockGraphiteDrilled.java` — lithium insertion branch in the use hook (`PILE_ROD_LITHIUM` → `copyShape` to `BLOCK_GRAPHITE_LITHIUM`), mirroring the fuel/boron/detector branches (NEXT :83-84).
- `BlockGraphiteDrilledBase.java` — `checkInteractions` mapping `PILE_ROD_LITHIUM` → `BLOCK_GRAPHITE_LITHIUM.defaultBlockState()` (NEXT :151-152).
- `interfaces/IPileNeutronReceiver.java` — javadoc implementor list updated.
- `datagen/assets/ModBlockStateProvider.java` — `block_graphite_lithium` moved to `pilePillarBlock(...)` like the rest of the family (normal + SHROUDED `_aluminum` variants).
- `datagen/ModBlockLootTableProvider.java` — `dropPileChannel(BLOCK_GRAPHITE_LITHIUM, PILE_ROD_LITHIUM)` reproducing NEXT's 3-pool loot table.
- `datagen/ModLanguageProviderEn.java` — block name set to NEXT's exact en string `"Pile Lithium Fuel (LEGACY)"`.
- `main/CreativeModeTabEventHandler.java` — creative-tab accept line next to the fuel block.

## Documented house adaptations (each with a NEXT analog or a stated API-gap reason)

1. **Tool dispatch** — NEXT `IToolable.onScrew` → version-gated use hook with direct item comparison (house convention, as `BlockGraphiteFuel`). Non-tool clicks PASS where NEXT returned `true` unconditionally; net behavior equal via `sidedSuccess`. Sneak-click dispatch for screwdriver/defuser goes through the item-side `onScrewForTool` bridge in `BlockGraphiteDrilledBase`.
2. **Ticker wiring** — mandatory per-block `getTicker` override (base class guards only the detector type).
3. **NBT** — NEXT's 1.21.5-only `ValueInput`/`ValueOutput` → house stonecutter-gated `CompoundTag` pattern, `getIntOr(...,0)` semantics preserved.
4. **Completion swap stub-safety** — NEXT calls `copyShape` directly on `BLOCK_GRAPHITE_TRITIUM`, which is still an AXIS-less plain-Block stub (other worker's scope); the swap is `hasProperty(AXIS)`-guarded so it does a plain swap now and auto-upgrades to NEXT behavior when tritium lands (same pattern as `BlockEntityPileFuel`).
5. **Config mapping** — `BalanceConfig.enable528` → `GeneralConfig.enable528` (now fully user-settable — see re-audit finding 3).
6. **No ru_ru block name** — NEXT itself has no Russian entry for this block; the port does not invent one, so `ru_ru` falls back to `en_us` exactly as NEXT does.
7. **No recipes** — NEXT ships none for `block_graphite_lithium`/`pile_rod_lithium` (three-way grep: JSON, `data/hbm/recipe`, Java — all negative); none added. No GUI, no container, no BER exist in NEXT for this machine either.
8. **Creative-tab placement kept** *(added by re-audit)* — the block and rod stay in the machines/dev creative tabs although NEXT ships both in no tab (1.7.10 `setCreativeTab(null)` legacy items). House-wide decision: the whole CP-1 pile family sits in the same tab with an inline divergence note (`CreativeModeTabEventHandler.java` "NEXT: keine Tab-Zuordnung; hier neben dem uebrigen Pile-Inhalt"); dropping only the lithium lines would split it from its family.
9. **Extra `ru_ru` readout translations kept** *(added by re-audit)* — `desc.shared.cp1FuelAssembly`/`desc.shared.flux` have ru entries (`ModLanguageProviderRu`, CP-1 section) that NEXT's `ru_ru.json` lacks (NEXT falls back to en_us). Kept as a deliberate improvement consistent with the surrounding best-effort CP-1 ru section, which belongs to sibling machines' workers.

## Verification evidence (both sessions)

Full port (`dwfrun-8ea7f251`):
- `gradlew :1.20.1-forge:compileJava` — exit 0 (attempt 1).
- `gradlew :1.21.1-neoforge:compileJava` — exit 0 (attempt 1).
- `gradlew :1.20.1-forge:runData` — exit 0; generated lang/blockstate/loot outputs confirmed on disk.
- Three independent fresh reviewers (round 1): **parity** PASS, **resources/registration** PASS, **multiloader/stonecutter-rules** PASS.
- Acceptance judge (fresh, round 1): **COMPLETE**.

Re-audit (`dwfrun-24ccc94b`): see "Verification evidence (this session)" above — both compile gates exit 0, `runData` exit 0, 3/3 fresh reviewers PASS, judge **COMPLETE** in round 1.

## Accepted reviewer notes (low severity, non-blocking)

1. **Sneak + hand-drill readout unreachable** (`BlockGraphiteBreedingFuel.java:104-105`): NEXT's hand drill is an `ItemTooling` whose `useOn` fires even on sneak-clicks; vanilla skips block use hooks when sneaking with a non-empty hand, and Modernized's `HAND_DRILL`/`HAND_DRILL_DESH` are plain `Item`s with no item-side bridge. Non-sneak readout works; screwdriver extraction is fully covered (block hook + `ScrewdriverItem` bridge). Family-wide pre-existing gap — `BlockGraphiteFuel.java:127-128` has the identical shape; fixing it for real means a hand-drill item bridge, owed by the tool/item worker.
2. **NEXT quirks ported verbatim by design:** `lastNeutrons` not persisted (flux readout shows 0 after reload until re-hit); `onScrew`-equivalent consumes the click for any recognized tool; breeding rays are 2 × flux-1 into fully random directions (range 5, step 0.5) — all NEXT-identical.

*(The re-audit's original note 2 — `PILE_ROD_LITHIUM` as a plain `Item` — was resolved by re-audit finding 1.)*

## Not covered / left unverified

- **In-game runtime behavior** — no `runClient`/`runServer` was launched; tick, neutron, and swap logic are verified by line-level code comparison plus compile gates only.
- **Gameplay balance** — actual tritium production rate inside a real CP-1 structure was not measured.
- **Tree-wide generated output** — `runData` regenerated lang for the whole mod (including the parallel `block_graphite_source` worker's in-flight entries); only this machine's outputs were audited. The worktree also carries the parallel worker's unrelated in-flight changes (source/plutonium block classes, registrations) that are outside this port's scope and were not reviewed here, though the compile gates ran against the combined tree.
- **`block_graphite_tritium`** is still a stub (deliberately — other worker's machine); the completion swap is stub-safe and parity-complete once that port lands.
