# broadcaster_pc — parity audit report

- **Machine:** `broadcaster_pc` — "Corrupted Broadcaster" / Ретранслятор розового облака (pink-cloud loudspeaker: looping broadcast track, nausea within 25 blocks, up to 10 damage within 15; no GUI, no energy, no recipes).
- **Date:** 2026-10-09
- **Mode:** parity audit (a full port already existed: `BroadcasterPcBlock` + `BroadcasterPcBlockEntity` + assets)
- **Result:** **COMPLETE** — acceptance judge returned COMPLETE after 1 fix round; 11 findings dispositioned (all fixed, 0 rejected); both loader compile gates green (re-verified by the session agent on the final tree).
- **Behavior authority:** NTM NEXT (`C:\Projects\ntm-next`, neoforge tree); the ORIGINAL 1.7.10 repo (`C:\Projects\Hbm-s-Nuclear-Tech-GIT`) as ground truth for gameplay numbers and translations.

## Findings and fixes (11)

| # | id | sev | what | fix |
|---|----|-----|------|-----|
| 1 | `broadcast-damage-not-bypassing-armor` | high | Broadcast damage did not bypass armor/protection/resistance; NEXT puts `hbm:broadcast` in `bypasses_armor` + `bypasses_effects` (NEXT `data/minecraft/tags/damage_type/bypasses_armor.json:19`, `bypasses_effects.json:14`; original `ModDamageSource.java:29` `setDamageIsAbsolute().setDamageBypassesArmor()`). | `ModDamageTypeTagProvider.java:66-76`: added `BROADCAST` to `BYPASSES_ARMOR` and a new `BYPASSES_EFFECTS` section; generated `bypasses_armor.json:32` and new `bypasses_effects.json:3` both contain `hbm_m:broadcast` (NEXT puts broadcast in no other damage-type tags — all 7 checked). |
| 2 | `facing-property-and-shapes-missing` | medium | No `FACING`, no placement rotation, no rotate/mirror, no voxel shapes — full cube for outline AND collision (NEXT `PinkCloudBroadcaster.java:27,37,46-70`; original metadata-dependent bounds incl. collision). | `BroadcasterPcBlock.java`: `HorizontalDirectionalBlock.FACING` (default NORTH), `createBlockStateDefinition`, `getStateForPlacement(ctx.getHorizontalDirection())`, rotate/mirror, `getShape` with NEXT-exact `SHAPE_NS box(1,0,4,15,10,12)` / `SHAPE_WE box(4,0,1,12,10,15)` by facing axis (collision = shape, as in NEXT). `ModBlockStateProvider.java:1846-1857` emits 4 facing variants with NEXT-verbatim y-rotations (south=0, west=90, north=180, east=270) — verified in generated blockstate. |
| 3 | `nausea-refresh-threshold-60-vs-100` | medium | Nausea re-applied only below 60 ticks remaining vs original/NEXT threshold 100 (NEXT `BlockEntityBroadcaster.java:34,54`; original `TileEntityBroadcaster.java:34`). | `BroadcasterPcBlockEntity.java:30` adds `CONFUSION_REFRESH_BELOW = 100`, used at the refresh check `:51`. |
| 4 | `block-props-stone-copy` | medium | Registered with copied stone props (hardness 1.5 / blast 6.0 / stone map color) vs original 5.0/15.0 with Material.iron (original `ModBlocks.java:1631`); NEXT rebalanced resistance to 9.0 but kept hardness 5.0 + `mapColor(METAL)` (`ModBlocks.java:7218-7227`). | `ModBlocks.java:2419-2424`: `.mapColor(MapColor.METAL).strength(5.0F, 15.0F)` (original ground truth; NEXT's 9.0 resistance noted in comment), kept `noOcclusion`, added `requiresCorrectToolForDrops` (NEXT-verbatim). |
| 5 | `broadcast-track-rerolled-per-start` | medium | Played `block.broadcast_random` → fresh random track every (re)start; original/NEXT pick ONE track deterministically per position (`Random(x+y+z)`, original `TileEntityBroadcaster.java:76-78`; NEXT `BlockEntityBroadcaster.java:72-87`). | `BroadcastSoundInstance.java:33-38`: picks one of `BROADCAST1..3` via `new Random(pos.getX()+pos.getY()+pos.getZ()).nextInt(3)` at construction; dead `BROADCAST_RANDOM` registration (`ModSounds.java:173-177`) and its `sounds.json` entry removed (no other references). |
| 6 | `broadcast-audible-range-16-not-25` | medium | Loop audible only ~16 blocks (default volume 1.0, LINEAR attenuation 16) vs NEXT's custom falloff to silence at 25 (`AudioDynamic.java:29-30,119-121`); creation gate used the default 32.0 range instead of the machine's 25. | `BroadcasterPcBlockEntity.java:39` passes `CONFUSION_RADIUS` (25) via the 4-arg `ClientSoundBootstrap.updateSound`; `BroadcastSoundInstance.java:47` sets volume `25/16 = 1.5625F` — decompiled SoundEngine (identical on 1.20.1 and 1.21.1: `max(volume,1) * attenuationDistance(16)`, gain clamped to 1.0) then fades to silence exactly at 25 blocks. See adaptation note below. |
| 7 | `broadcaster-needs-iron-tool` | low | Gated behind iron pickaxe (`needs_iron_tool` via the datagen default branch, `ModBlockTagProvider.java:263-268`); original/NEXT allow any pickaxe tier. | `ModBlockTagProvider.java`: `anyPickaxeBlocks = Set.of(BROADCASTER_PC)` routed to pickaxe-without-needs-tier; verified `needs_iron_tool.json` no longer lists it, `mineable/pickaxe.json:671` still does. |
| 8 | `ru-death-message-broadcast-missing` | low | `death.attack.broadcast` absent from ru_ru → RU clients see English death text (NEXT `ru_ru.json:2264`; original `ru_RU.lang:897`). | `ModLanguageProviderRu.java:3610`: `"%1$s растворил свой мозг."` (byte-identical to original); verified in generated `ru_ru.json:1477`. |
| 9 | `broadcast-subtitle-key-undefined` | low | All four broadcast `sounds.json` entries referenced `sounds.hbm_m.subtitle.broadcast`, defined nowhere (NEXT defines no subtitle at all). | Key added to both providers (house subtitle convention kept): en `"Corrupted broadcast"`, ru `"Повреждённая трансляция"` — generated `en_us.json:4915` / `ru_ru.json:3521`. See adaptation note below. |
| 10 | `broadcast-damage-json-fields` | low | Damage type JSON used vanilla defaults (exhaustion 0.1, scaling `when_caused_by_living_non_player`) vs NEXT's `exhaustion 0.0 / scaling never` (`data/hbm/damage_type/broadcast.json`); hand-written copy also shipped twice. | `DataGenerators.java:83-94` special-cases broadcast to `DamageType(path, DamageScaling.NEVER, 0.0F)`; deleted hand-written `src/main/resources/data/hbm_m/damage_type/broadcast.json`; generated copy verified `{exhaustion: 0.0, scaling: never}`. |
| 11 | `broadcast-model-ao-and-uv-swap` | low | Model omitted NEXT's `"ambientocclusion": false`; body element's north/south face UVs swapped (NEXT `models/block/broadcaster_pc.json`). | `models/block/machines/broadcaster_pc.json:3` adds the flag; N/S UVs restored to NEXT's mapping (north `[2,4,5.5,9]`, south `[7.5,4,11,9]`). |

Finding 11's evidence also confirmed parity elsewhere: all 4 model elements' geometry match (mast to y=24), `model_broadcaster.png`/`broadcaster.png` are md5-identical to NEXT's textures, item models render untransformed from the block model.

## Accepted adaptations (documented, not NEXT-verbatim)

- **Sound volume mapping (finding 6):** NEXT's literal `VOLUME = 25.0F` is not directly translatable — decompiled SoundEngine clamps channel gain to 1.0 and computes linear rolloff as `max(volume,1) * 16`, so literal 25 would give a 400-block rolloff. Used `25/16` so vanilla's rolloff ends exactly at the intended 25-block radius (flat at max gain, then linear to silence at 25) — the closest vanilla-native approximation of NEXT's clamped curve. Documented in `BroadcastSoundInstance.java`.
- **Subtitle strings (finding 9):** invented EN/RU strings (NEXT defines no subtitle), keeping the Modernized house convention of entry-level subtitles.
- **Removed invented `block.broadcast_random`** sound event after the per-position track fix (dead code; no analog in original/NEXT).

## Deliberate quirks verified and kept (do not "fix" later)

1. **AABB superset, net-identical:** `new AABB(pos).inflate(25)` (51-wide, corner-anchored) vs NEXT's center ±25 (50-wide); the `dist > 25` skip filters every extra candidate — behavior identical.
2. **`damage > 0` guard, net-identical:** only diverges at exactly `dist == 15.0` (amount 0.0) — a measure-zero float case.
3. **Creative-tab order follows the ORIGINAL, not NEXT:** broadcaster_pc first in the MACHINE tab (`CreativeModeTabEventHandler.java:1566`), matching original registration order (`ModBlocks.java:1631` before `:2273` machine_siren); NEXT reordered.
4. **`MobEffects.CONFUSION` vs NEXT `NAUSEA`:** same effect, cross-version API naming only.
5. **Sound lifecycle via `setRemoved` only — verified safe on both loaders from bytecode/decompile:** 1.20.1 Forge client chunk unload → `clearAllBlockEntities` → `onChunkUnloaded()` AND `setRemoved()` (Forge 47.4.10 binpatched `LevelChunk`); 1.21.1 → `BlockEntity::setRemoved` (vanilla decompile). Loop stops on unload and removal on both; `updateSound` recreates it on reload. `ClientSoundManager.clearAll()` has zero call sites but stale entries are harmless (`isAudible` false when player==null; recreation guarded).
6. **`sounds.json` omits NEXT's `stream:true`** and adds entry-level subtitles — infra-level divergence; the three oggs are present on both sides with identical sizes. NEXT fabric tree is byte-identical to neoforge for the block/BE/sounds.json (diff verified).

## Files touched

- `src/main/java/com/hbm_m/block/machines/BroadcasterPcBlock.java`
- `src/main/java/com/hbm_m/blockentity/machines/BroadcasterPcBlockEntity.java`
- `src/main/java/com/hbm_m/block/ModBlocks.java`
- `src/main/java/com/hbm_m/sound/BroadcastSoundInstance.java`
- `src/main/java/com/hbm_m/sound/ModSounds.java`
- `src/main/resources/assets/hbm_m/sounds.json`
- `src/main/java/com/hbm_m/datagen/ModDamageTypeTagProvider.java`
- `src/main/java/com/hbm_m/datagen/DataGenerators.java`
- `src/main/resources/data/hbm_m/damage_type/broadcast.json` (deleted — datagen is now the only source)
- `src/main/java/com/hbm_m/datagen/assets/ModBlockStateProvider.java`
- `src/main/java/com/hbm_m/datagen/assets/ModBlockTagProvider.java`
- `src/main/java/com/hbm_m/datagen/ModLanguageProviderEn.java`
- `src/main/java/com/hbm_m/datagen/ModLanguageProviderRu.java`
- `src/main/resources/assets/hbm_m/models/block/machines/broadcaster_pc.json`
- `src/main/java/com/hbm_m/creativetabs/NightlyTab.java` (one line: `add(ModBlocks.BROADCASTER_PC);`)
- `docs/porting/machine_porting_matrix.json` (this entry's `portStatus` → `PORTED` only)
- regenerated outputs under `src/generated/resources/` (blockstate facing variants, `bypasses_effects.json`, lang, damage type)

## Verification evidence

Run by the workflow (authoritative gates, round 1):
- `gradlew :1.20.1-forge:compileJava` — exit 0
- `gradlew :1.21.1-neoforge:compileJava` — exit 0

Re-run by the session agent on the FINAL tree (after the NightlyTab one-liner):
- `./gradlew --console=plain :1.20.1-forge:compileJava` — BUILD SUCCESSFUL, exit 0
- `./gradlew --console=plain :1.21.1-neoforge:compileJava` — BUILD SUCCESSFUL, exit 0; generated stonecutter tree (`versions/1.21.1-neoforge/build/generated/stonecutter/.../NightlyTab.java:35`) confirmed to contain the new tab line
- datagen re-run by the fixer (`gradlew :1.20.1-forge:runData`); generated outputs verified on disk per each finding above.

Verification quality: 3 independent fresh reviewers (game parity / resources+data / sound+lifecycle+multiloader) passed; findings were independently confirmed with path:line evidence on both sides (NEXT, Modernized, and the 1.7.10 original where it is ground truth).

## Not covered

- In-game runtime behavior — no runClient/runServer was launched; tick, effect, damage, and sound logic are verified by line-level code comparison and compile gates only.
- Audible sound range was not measured in a running client; judged from the volume/attenuation math in code (see adaptation note).
- `runData` regenerates lang/assets for the whole mod; only this machine's outputs were audited.
- Model UV micro-divergences beyond finding 11 (sub-texture-region 180° rotations / mirrored mappings on small faces: body `up`/`down`, `antenna_base`/`mast`/`bracket` N/S) were noted and left untouched — same texture regions, cosmetic.
- Infra-level issues noted, out of machine scope: ~57 hand-written `data/hbm_m/damage_type/*.json` duplicate datagen output (only the stale `broadcast.json` was deleted); Modernized `sounds.json` omits NEXT's `stream:true` flags.
- Transient note from fix round 1: `:1.21.1-neoforge:compileJava` briefly failed on the parallel door/render worker's in-flight files (`DoorPartClipper`, `MachinePartRenderer`, `MachineCapacitorBlockEntity`) mid-round; zero errors in any broadcaster_pc file, and the final-tree gates above are green — the parallel worker landed its fixes.
