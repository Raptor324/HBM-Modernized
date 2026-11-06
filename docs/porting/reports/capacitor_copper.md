# Porting Report — `capacitor_copper` (Old Capacitor, `MachineCapacitor`/`TileEntityCapacitor`)

**Status: PORTED**
Date: 2026-10-09 · Branch: `machines` · Verdict (orchestrator gate): `complete=true`, no blockers — both `compileJava` gates exit 0; judge returned COMPLETE.

Provenance legend used throughout:
- **[verified]** — I read or ran it in this session (post-fix working tree).
- **[ask:audit]** / **[ask:fixes]** / **[ask:verdict]** — material supplied with the ask (pre-fix audit, two fix-round reports, recorded gate verdict); not re-derivable because it describes pre-fix state or another actor's run.
- **[ask:final]** — final recorded data supplied with the closing update ask (final gate runs, fresh logic/asset verifier re-checks, judge COMPLETE); those runs were executed by the finalizing session, not re-executed by the report writer.
- All Modernized paths relative to `C:/Projects/HBM-Modernized-agents`; original-repo paths absolute under `C:/Projects/Hbm-s-Nuclear-Tech-GIT`.

---

## 1. Mode used

**Parity audit.** An implementation already existed in Modernized (all 6 blocks registered; `MachineCapacitorBlock` + `MachineCapacitorBlockEntity`; bus-chain walk present), so the round audited behavior against the 1.7.10 ground truth (`MachineCapacitor.java`) rather than porting from scratch. Per the audit: all 9 mandated claims were verified by direct reads on both sides, yielding **14 discrepancies: 6 must-fix, 6 should-fix, 2 wont-fix** [ask:audit]. Two fix rounds followed (round 1: 12 items; round 2: 2 regression blockers found in round 1's output), then the orchestrator's judge run failed compilation [ask:verdict].

Everything cited below as **[verified]** was re-checked in this session against the current tree; original-side line numbers were re-checked line-for-line and match the audit.

---

## 2. Discrepancies found

### Must-fix

**M1. Charge/discharge rates halved — `/200` + `/600` instead of `/100` + `/300`.**
- Original CODE: `getProviderSpeed() = getMaxPower()/300` and `getReceiverSpeed() = getMaxPower()/100` — `C:/Projects/Hbm-s-Nuclear-Tech-GIT/src/main/java/com/hbm/blocks/machine/MachineCapacitor.java:262-268` **[verified]**. The original's own *tooltip* claimed /200 + /600 (`MachineCapacitor.java:116-117` **[verified]**) — wrong in the original; the old port copied the tooltip numbers into behavior [ask:audit: pre-fix `MachineCapacitorBlockEntity.java:36`].
- **Fixed** [verified]: `MachineCapacitorBlockEntity.java:67` — constructor now passes `Math.max(1, capacityFor(state)/100)` receive and `Math.max(1, capacityFor(state)/300)` provide; javadoc `:28-32` states the parity facts. Tooltip lines use the corrected numbers (`MachineCapacitorBlock.java:159-161`).

**M2. Bare capacitor provided AND received on all 6 faces; original provides nothing without a bus chain and receives only on its input face.**
- Original: `updateEntity` subscribes as receiver ONLY at the metadata (input) face (`MachineCapacitor.java:205`) and calls `tryProvide` ONLY at the end of a straight `capacitor_bus` chain (`pos != null && last != null`, `MachineCapacitor.java:187-203`); `canConnect` is face-gated (`:280-283`) **[verified]**.
- Pre-fix Modernized: `ensureNetworkInitialized` passed the BE's own position to `EnergySubscriptions.update`, scanning all 6 faces for subscribe AND provide, making the `canConnectEnergy` override dead code in the network path [ask:audit: `BaseMachineBlockEntity.java:493-505`, `EnergySubscriptions.java:151-253`].
- **Fixed** [verified]: `MachineCapacitorBlockEntity.java:139-154` overrides `ensureNetworkInitialized` — one `trySubscribe` at the FACING (input) face only (`:149-153`), `tryUnsubscribe`+`tryProvide` only at the chain end when a valid straight chain exists (`:143-147`); without a chain the capacitor provides nothing (`walkBusChain` returns null, `:191-192`), matching the original.

**M3. `capacitor_bus` ported as a functional 1M capacitor instead of a passive orientation block.**
- Original: `capacitor_bus` is a TE-less `Block` that only records the player's look direction (`BlockPistonBase.determineOrientation`) and answers `canConnect` — `C:/Projects/Hbm-s-Nuclear-Tech-GIT/src/main/java/com/hbm/blocks/machine/MachineCapacitorBus.java:22-58` **[verified]**; registered at `ModBlocks.java (1.7.10):1836` **[verified]**.
- Pre-fix Modernized: registered as a full `MachineCapacitorBlock` (1M capacity, valid BE, network subscriber) and the registration comment claimed "no bus-chaining mechanic", contradicted by the BE's chain walk [ask:audit: `ModBlocks.java:2424-2428`, `ModBlockEntities.java:868-872`].
- **Fixed** [verified]: new passive `MachineCapacitorBusBlock` (`src/main/java/com/hbm_m/block/machines/MachineCapacitorBusBlock.java:28-76` — `Block` subclass, no BE, `FACING` from `getNearestLookingDirection()` `:43-46`, rotate/mirror `:48-56`, shift-desc tooltip `:59-76`); `ModBlocks.java:2442-2443` uses it; comment rewritten with the true chain description (`:2428-2433`); `CAPACITOR_BUS` removed from the BE's valid blocks — only the 5 real capacitors remain (`ModBlockEntities.java:868-875`). Note recorded in fixes: the original's `IEnergyConnectorBlock.canConnect` on the bus has no consumer in the Modernized network path (`trySubscribe`/`tryProvide` consult PowerConductor BEs only), so the passive bus exposes `FACING` only [ask:fixes].

**M4. Stored charge lost when broken — persistent-drop pattern not applied.**
- Original: `getDrops` → `IPersistentNBT.getDrops`, restore via `onBlockPlacedBy` → `restoreData` (`MachineCapacitor.java:133-141`), power/maxPower persisted (`:286-298`), charge shown in item tooltip (`:118`) **[verified]**.
- Pre-fix Modernized: plain `dropSelf` loot, no NBT copy, no restore, no stored-charge tooltip [ask:audit].
- **Fixed** [verified]: `dropCapacitorWithNbt()` for the 5 capacitors copies the BE's lowercase `energy`/`capacity` into `BlockEntityTag` (`ModBlockLootTableProvider.java:79-85`, `:592-609`); generated `capacitor_copper.json` loot contains the `copy_nbt` ops (read this session); `capacitor_bus.json` stays plain dropSelf (read this session). Restore-on-place via `setPlacedBy` (`MachineCapacitorBlock.java:107-125`); on 1.21.1 (copy_nbt removed in 1.20.5+) the charge moves via a code drop — `playerWillDestroy` + `saveToItemStack` (`MachineCapacitorBlock.java:127-142`, gated; `MachineCapacitorBlockEntity.java:262-273`, gated), mirroring the in-repo battery pattern (`MachineBatteryBlock.java:132-141`, `:174` **[verified]**). Capacitor loot tables excluded from the NeoForge build like the battery ones (`build.neoforge.gradle.kts:373-386`, filter `:383-384`) **[verified]**.

**M5. No Redstone-over-Radio values (`VAL:fill` / `VAL:fillpercent`).**
- Original: `getFunctionInfo` + `provideRORValue` return `PREFIX_VALUE+"fill"` and `+"fillpercent"` (`MachineCapacitor.java:314-327`) **[verified]**.
- Pre-fix Modernized: BE did not implement `IRORValueProvider`; javadoc declared ROR "not ported" [ask:audit: pre-fix `MachineCapacitorBlockEntity.java:29-30`].
- **Fixed** [verified]: `MachineCapacitorBlockEntity.java:51` implements `IRORValueProvider`; `ROR` array `:59-63`; `getFunctionInfo`/`provideRORValue` `:224-233` return `getEnergyStored()` and `getEnergyStored()*100/getMaxEnergyStored()` — 1:1 with the original. In-repo exemplar consumer: `RadioTorchReaderBlockEntity` (per audit).

**M6. Block properties: stone (1.5/6.0) instead of original iron-like 5.0/10.0.**
- Original: `Material.iron`, `setHardness(5.0F).setResistance(10.0F)` for all six blocks — `C:/Projects/Hbm-s-Nuclear-Tech-GIT/src/main/java/com/hbm/blocks/ModBlocks.java:1836-1841` **[verified]**.
- Pre-fix Modernized: `BlockProps.copy(Blocks.STONE).noOcclusion()` [ask:audit].
- **Fixed** [verified]: shared `capacitorProps()` = stone copy + `MapColor.METAL` + `SoundType.METAL` + `strength(5.0F, 10.0F)` + `noOcclusion()` + `requiresCorrectToolForDrops()` (`ModBlocks.java:2434-2441`); all six registrations use it (`:2442-2453`). A round-2 regression (concurrent worker's merge had re-clobbered `CAPACITOR_TANTALIUM` back to raw stone props) was found and re-fixed; verified: line 2452-2453 passes `capacitorProps()` [ask:fixes; post-fix state verified].

### Should-fix

**S1. Look-overlay showed configured speed caps instead of actual per-tick flow, and the value could be stale.**
- Original: overlay prints the real `powerReceived`/`powerSent` counters (`MachineCapacitor.java:107-108`), rolled per tick (`:209-212`) and synced via `networkPackNT(15)` (`:207`) **[verified]**.
- Pre-fix Modernized: static `getReceiveSpeed`/`getProvideSpeed` and no energy sync to the client for the ticker-less BE [ask:audit: pre-fix `MachineCapacitorBlock.java:73-76`, `BaseMachineBlockEntity.java:196-200`].
- **Fixed** [verified]: counters ported (`MachineCapacitorBlockEntity.java:53-57`), server ticker rolls them per tick and calls `sendUpdateToClient()` every 15 ticks (`:117-128`; `sendUpdateToClient` exists at `BaseMachineBlockEntity.java:343` **[verified]**); block returns the server ticker (`MachineCapacitorBlock.java:74-77`); overlay prints the live counters (`:95-99`). Counters carried through NBT (`MachineCapacitorBlockEntity.java:207-221`). Real receive-path coverage added in round 2 via the `transferPower` override (`:105-110`) because network distribution calls `transferPower`, not `receiveEnergy` [ask:fixes]; the override's first form (qualified super-call) failed the compile gate and was reworked into an inline in the final round (see §3 item 16 and §5) — the tree now compiles clean on both loaders.

**S2. Item tooltip absent (shift-desc, stats, charge line, lang keys).**
- Original: shift-desc `tile.capacitor.desc` + gold stat lines + yellow persistent-charge line (`MachineCapacitor.java:113-131`) **[verified]**.
- **Fixed** [verified]: `appendHoverText` on the capacitor (`MachineCapacitorBlock.java:148-182` — gold capacity/charge/discharge lines with corrected /100 + /300, yellow stored-charge line from `BlockEntityTag.energy`, shift-desc via `Screen.hasShiftDown()`, shared `tooltip.hbm_m.hold_shift_for_details` hint) and on the bus (`MachineCapacitorBusBlock.java:59-76`). Lang keys present in both providers and generated files: `ModLanguageProviderEn.java:3567-3575`, `ModLanguageProviderRu.java:2583-2595`; generated `en_us.json:5320-5324` + `:5411`, `ru_ru.json:3919-3923` + `:3995` **[verified]**.

**S3. RU localization gaps (5 of 6 block names + container missing → raw keys rendered).**
- Original RU lang also lacked the sibling names but 1.7.10 fell back to en_US; the port must reproduce the visible result [ask:audit].
- **Fixed** [verified]: `ru_ru.json:382-387` now defines all six block names (copper in RU, siblings as EN strings per the original's fallback), `:1398` the container key; source `ModLanguageProviderRu.java:2583-2588`.

**S4. Blockstate ignored FACING — all facings rendered the identical cube; inner textures unused.**
- Original: non-opaque custom render with per-metadata rotation incl. InnerTop/InnerSide textures [ask:audit].
- **Fixed** [verified]: datagen emits 6-way FACING variants per capacitor (`ModBlockStateProvider.java:1874-1889`, observer rotation convention, input face = `inner_top`); generated `blockstates/capacitor_copper.json` contains all six variants `facing=down/east/north/south/up/west` (read this session); `capacitor_bus_out.png` asset copied 1:1 from the 1.7.10 repo (exists: `src/main/resources/assets/hbm_m/textures/block/capacitor_bus_out.png` **[verified]**).

**S5. Bus-chain walk capped at 64 steps and stops at chunk borders (original: unbounded `while`, `MachineCapacitor.java:187-198` **[verified]**).**
- **Accepted, documented deviation** [verified]: cap at `MachineCapacitorBlockEntity.java:171` (`step < 64`) and `hasChunkAt` break `:177` with the anti-sync-load rationale (`:172-176`); declared in the class javadoc `:47-49`. Anti-sync-load guard retained as the audit's acceptable-engineering-guard option; only matters for chains >64 buses or crossing into unloaded chunks.

**S6. Capacitors missing from NightlyTab ("Ported Overnight").**
- **Fixed** [verified]: `NightlyTab.java:36` appends `add(ModBlocks.CAPACITOR_COPPER);` as the last entry (static block `:28-37`). Copper also remains in the machines tab (`CreativeModeTabEventHandler.java:1631`) and all six in the dev tab (`:2829-2834`) **[verified]**.

### Wont-fix

**W1. OpenComputers/ComputerCraft integration not ported — wont-fix.**
- Original: OC `SimpleComponent` `"capacitor"` with `getEnergy/getMaxEnergy/getEnergySent/getEnergyReceived/getInfo`, incl. the swapped names (`getEnergySent` returns `lastPowerReceived`, `MachineCapacitor.java:348-356`) and the `invoke("getInfo")` misroute to `getEnergyReceived` (`:388`) **[verified]**.
- Reason: no CC/OC compat infrastructure exists anywhere in Modernized — `find src/main -path '*computercraft*' -o -name '*Peripheral*'` returned nothing (run this session). There is no equivalent system to port into. The BE javadoc records OC as unported (`MachineCapacitorBlockEntity.java:44-45`).

**W2. Original world-gen placement of `capacitor_copper` (dungeons/ships/silos) unported — wont-fix.**
- Original references verified this session: `world/dungeon/DesertAtom003.java`, `world/dungeon/Spaceship.java`, `world/gen/component/BunkerComponents.java`, `world/gen/component/SiloComponent.java` (all grep-positive for `capacitor_copper`).
- Reason: none of the hosting structure systems are ported to Modernized (`grep -rl CAPACITOR src/main/java/com/hbm_m/worldgen/` returned nothing, run this session). Structure parity belongs to future structure-system porting; crafting/creative/machines-tab availability of the block is unaffected.

---

## 3. What was changed

Round 1 (12 audit items) [ask:fixes, post-fix state verified unless noted]:
1. Rates corrected to `/100` receive + `/300` provide; javadoc rewritten (`MachineCapacitorBlockEntity.java:67`, `:28-32`) — M1.
2. `ensureNetworkInitialized()` overridden as a 1:1 port of the original's network section: subscribe at FACING face only, provide only at chain end; the old all-6-faces behavior of `EnergySubscriptions.update` bypassed for this BE (`:139-154`) — M2.
3. `walkBusChain()` returns end position + direction (`private record BusChain`, `:279`); `getExtraEnergyPorts()` kept for unsubscribeAll cleanup only (`:201-205`); 64-step cap documented (`:47-49`, `:169-171`) — M2/S5.
4. `IRORValueProvider` implemented with `VAL:fill`/`VAL:fillpercent` (`:51`, `:59-63`, `:224-233`) — M5.
5. `powerReceived`/`powerSent`/`lastPower*` counters ported; static `tick()` rolls them and syncs the client every 15 ticks; counters carried in NBT (`:53-57`, `:117-128`, `:207-221`) — S1.
6. Block: server ticker wired (`MachineCapacitorBlock.java:74-77`); overlay shows live counters (`:95-99`); `setPlacedBy` restore (`:107-125`); 1.21.1-gated `playerWillDestroy` code-drop (`:127-142`); `appendHoverText` (`:148-182`) — S1/M4/S2.
7. New passive `MachineCapacitorBusBlock` (look-direction FACING, no BE, no energy) — M3.
8. `ModBlocks`: `CAPACITOR_BUS` switched to the passive bus; stale comment replaced; shared `capacitorProps()` (5.0F/10.0F, metal) applied to all six — M3/M6.
9. `ModBlockEntities`: `CAPACITOR_BUS` removed from `MACHINE_CAPACITOR_BE` valid blocks — M3.
10. Datagen: `dropCapacitorWithNbt()` (energy/capacity → `BlockEntityTag`); capacitor loot excluded from the NeoForge build (code-drop path) — M4.
11. Datagen: 6-way FACING blockstates/models using the previously-unreferenced `inner_top` textures; new `capacitor_bus_out.png` asset — S4.
12. Lang: EN+RU tooltip/desc/container keys; RU gains the 5 missing sibling names + container; `NightlyTab` gains `CAPACITOR_COPPER` — S2/S3/S6.
13. Datagen executed: `gradlew.bat :1.20.1-forge:runData -q --console=plain --warning-mode=none` — first attempt failed on a pre-existing KSP incremental-cache error (task `:1.20.1-forge:kspKotlin`, no Kotlin touched); after `rm -rf versions/1.20.1-forge/build/kspCaches` the rerun completed ("All providers took: 1451 ms", 20 files written) and implicitly compiled the 1.20.1 source set [ask:fixes — run by the fixer, not re-run this session; the generated outputs it produced were verified by me directly].

Round 2 (2 regression blockers in round-1 output) [ask:fixes; post-fix state verified]:
14. `CAPACITOR_TANTALIUM` re-switched to `capacitorProps()` after a concurrent worker's `ModBlocks.java` merge clobbered it back to raw stone props (`ModBlocks.java:2452-2453`; re-verified on the final tree — all six registrations, bus/copper/gold/niobium/schrabidate/tantalium `:2442-2453`, pass `capacitorProps()`) — M6.
15. `transferPower(long)` override added so the real network receive paths (PowerNet distribution, direct provision, FE bridge — all call `transferPower`) increment `powerReceived` (`MachineCapacitorBlockEntity.java:105-110`) — S1. This edit's first form introduced a compile error; reworked in the final round (item 16).
16. **Final round — `transferPower` override reworked.** The illegal qualified super-call `IEnergyReceiver.super.transferPower(power)` ("not an enclosing class": the BE inherits `IEnergyReceiver` transitively via `BaseMachineBlockEntity`, it does not directly implement the interface) was replaced by inlining the `IEnergyReceiver` default's storage logic: if the charge fits, add all and `powerReceived += power` (return 0); otherwise `powerReceived += capacity` (capacity = max − stored), fill to max and return the overshoot — semantically identical to the original's own override (`MachineCapacitor.java:233-244`; port at `MachineCapacitorBlockEntity.java:105-120`) **[verified]**.

## 4. Files touched

All paths verified to exist this session:

Capacitor-scope source:
- `src/main/java/com/hbm_m/blockentity/machines/MachineCapacitorBlockEntity.java` (rates, chain walk, ROR, counters, ticker, transferPower, gated saveToItemStack)
- `src/main/java/com/hbm_m/block/machines/MachineCapacitorBlock.java` (ticker, overlay, setPlacedBy, gated playerWillDestroy, tooltips)
- `src/main/java/com/hbm_m/block/machines/MachineCapacitorBusBlock.java` (**new**, passive bus)
- `src/main/java/com/hbm_m/block/ModBlocks.java` (bus swap, `capacitorProps()`, stale comment)
- `src/main/java/com/hbm_m/blockentity/ModBlockEntities.java` (bus removed from valid blocks)
- `src/main/java/com/hbm_m/creativetabs/NightlyTab.java` (+1 line)
- `src/main/java/com/hbm_m/datagen/ModBlockLootTableProvider.java` (`dropCapacitorWithNbt`)
- `src/main/java/com/hbm_m/datagen/assets/ModBlockStateProvider.java` (facing variants)
- `src/main/java/com/hbm_m/datagen/ModLanguageProviderEn.java`, `ModLanguageProviderRu.java` (tooltip/name/container keys)
- `build.neoforge.gradle.kts` (capacitor loot exclusion, 1.21.1)
- `src/main/resources/assets/hbm_m/textures/block/capacitor_bus_out.png` (**new**, 1:1 asset copy)

Generated outputs (verified content this session):
- `src/generated/resources/data/hbm_m/loot_tables/blocks/capacitor_{copper,gold,niobium,schrabidate,tantalium}.json` (copy_nbt energy/capacity) and `capacitor_bus.json` (plain dropSelf)
- `src/generated/resources/assets/hbm_m/blockstates/capacitor_{copper,bus}.json` (6 facing variants each)
- `src/generated/resources/assets/hbm_m/models/block/capacitor_{copper,bus}.json` + `models/item/capacitor_copper.json`
- `src/generated/resources/assets/hbm_m/lang/en_us.json` (block names `:388-393`, container `:1495`, tooltips `:5320-5324`), `ru_ru.json` (`:382-387`, `:1398`, `:3919-3923`)

Not touched (explicitly out of scope): `icf_laser_capacitor`, blast-door files, battery files (pattern source only), broadcaster_pc files.

## 5. Verification evidence

Final compile gate — the whole working tree, both targets (finalizing session, 2026-10-09 [ask:final]; runs not re-executed by the report writer):

```
gradlew.bat :1.20.1-forge:compileJava -q --console=plain --warning-mode=none
```
→ **exit 0**; 16 warnings, all pre-existing and zero capacitor-related (mixin-target/@Shadow/removal warnings in other workers' in-flight files), no errors.
```
gradlew.bat :1.21.1-neoforge:compileJava -q --console=plain --warning-mode=none
```
→ **exit 0**; no output, no errors.

**Gate = exit 0 on both loaders: PASSED — no javac errors remain on either target (warnings only).** The earlier gate listings naming the 8 `mixin/*` files, `compat/jade/HbmFluidStorageProvider.java`, `datagen/ModDamageTypeTagProvider.java`, and `compat/jei/HbmJeiPlugin.java` were javac **warnings** from other workers' in-flight files, not errors; the single real error either loader ever reported was the capacitor-scope one at `MachineCapacitorBlockEntity.java:107` (`not an enclosing class: IEnergyReceiver` — the qualified super-call), and it is fixed by the `transferPower` rework (§3 item 16), which compiles clean in common code on both loaders.

**Final verification.** After the rework, fresh logic and asset verifiers re-checked the final tree — audit outcomes M1–M5/S1–S6 spot-verified, all 6 logic-verifier findings and all 3 asset-verifier findings re-confirmed accurate (none a blocker) — and the judge returned **COMPLETE** (`complete=true`, no blockers) [ask:final].

Datagen verification (round-1 outputs, re-checked by me directly this session): loot `copy_nbt` ops present / bus dropSelf; 6 facing variants in blockstates; lang keys in both generated files (citations in §4); `capacitor_bus_out.png` present. The `runData` command itself was run by the fixer, not by me [ask:fixes].

## 6. Left unverified / wont-fix

- **OpenComputers/ComputerCraft integration — wont-fix.** No CC/OC compat infrastructure exists in Modernized (verified by `find` this session; see W1). The BE javadoc records ROR as ported and OC as out (`MachineCapacitorBlockEntity.java:43-45`).
- **Original world-gen placement of `capacitor_copper` — wont-fix.** Hosting structure systems unported (see W2).
- **Runtime behavior not tested.** No client/server run happened this session or in the fix rounds: bus-chain end-provision, input-face gating, loot charge restore, ROR readout, overlay live counters and the 15-tick client sync are source-verified only.
- **NTM NEXT corroboration unverified at the cited path.** The audit cites `fabric/src/main/java/com/hbm/tileentity/machine/storage/BlockEntityMachineCapacitor.java` (NEXT, /100+/300) [ask:audit]; a repo-wide `find` over `Hbm-s-Nuclear-Tech-GIT` plus `HBM-NTM-Rebirth`, `HBM-NTM-high-edition`, `HBMsNTM-NEO-EDITION`, `HBM-CE` found no such file. The /100 + /300 parity claim does not rest on it — the original's CODE verifies directly (`MachineCapacitor.java:262-268`) — but the NEXT citation itself could not be confirmed.
- **64-step chain cap** — accepted documented deviation, not a defect (S5); parity impact limited to chains >64 buses or crossing unloaded chunks.
- **Pre-existing battery loot quirk** — generated `machine_battery.json` copies capital-E `Energy`/`Inventory` keys that `BaseMachineBlockEntity` never writes (it writes lowercase `inventory`/`energy`), so part of the battery's 1.20.1 copy_nbt is dead. Noticed during round 1, left untouched (out of capacitor scope); the capacitor loot uses the verified lowercase keys.
