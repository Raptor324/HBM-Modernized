# Port Report — `block_graphite_detector` (CP-1 Neutron Detector)

- **Date:** 2026-10-09
- **Status:** PORTED — parity audit passed (judge verdicts: logic COMPLETE, assets COMPLETE)
- **Port source:** NTM: NEXT (`C:/Projects/ntm-next`, canonical `fabric/src/main/java/com/hbm/...`) — behavior authority; the 1.7.10 repo (`C:/Projects/Hbm-s-Nuclear-Tech-GIT`) is semantics backup only
- **Targets:** 1.20.1-forge (active) and 1.21.1-neoforge (Architectury + stonecutter)
- **Gates (final, per session outcome):** 1.20.1-forge compile exit 0 · 1.21.1-neoforge compile exit 0 · runData exit 0

## 1. Mode

**Parity audit** of the already-landed port (detector subsystem committed in `b47cabe1`, with follow-up fix and bookkeeping commits up to `77992fdb` at audit time), not a fresh port. All 7 scope files were read line-by-line on **both** sides this session (NEXT `BlockGraphiteDrilledBase` 1–233, `BlockGraphiteDrilled` 1–171, `BlockGraphiteDrilledTE` 1–16, `BlockGraphiteRod` 1–82, `BlockGraphiteNeutronDetector` 1–127, `BlockEntityPileNeutronDetector` 1–61, `IPileNeutronReceiver` 1–9; Modernized mirrors in full). **Audit result: no behavior-affecting logic discrepancy found in the detector subsystem.** The findings below are comment/doc/asset-level; every one was fixed during the session and re-verified.

## 2. Discrepancies found this session

| # | What | NEXT path:line | Modernized path:line | Fix | Severity | Fixed? |
|---|---|---|---|---|---|---|
| 1 | Stale javadoc claimed "sole in-mod implementor is BlockEntityPileNeutronDetector"; NEXT has three implementors (detector, `BlockEntityPileFuel`, `BlockEntityPileBreedingFuel`, found by `grep -rln "implements.*IPileNeutronReceiver"` over NEXT `com/hbm`) | `fabric/src/main/java/com/hbm/interfaces/IPileNeutronReceiver.java:6-9` | `src/main/java/com/hbm_m/interfaces/IPileNeutronReceiver.java:5-7` (at finding time) | Reword javadoc | low | **Yes** — now lines 3–11 list all three implementors (re-read this session) |
| 2 | Misleading comments claimed "NEXT ejects new ItemStack(null) … vanilla discards it"; NEXT actually executes `new ItemStack(inserted)` unconditionally (would throw on null), i.e. the plain-channel eject is unreachable there; "vanilla discards it" was never verifiable (no MC sources) | `fabric/.../blocks/machine/pile/BlockGraphiteDrilledBase.java:118-124` | `src/main/java/com/hbm_m/block/machines/pile/BlockGraphiteDrilledBase.java:129-131` and `:230-236` (at finding time) | Reword both comments; defensive `ItemStack.EMPTY` behavior stays | low | **Yes** — now `:132-134` and `:251-253` (grep `would crash there` this session) |
| 3 | `pile_rod_boron`/`pile_rod_detector` were plain `Item` — no usage tooltips; NEXT renders shared + per-rod desc lines | `fabric/.../items/machine/ItemPileRod.java:16,21-27` + `en_us.json` keys `desc.item.pileRod`, `item.hbm.pile_rod_*.desc` | `src/main/java/com/hbm_m/item/ModItems.java:3284-3285` (plain `new Item` at finding time) | New `ItemPileRod` tooltip class + registration repoint + lang keys (en; ru shared key verbatim) | medium | **Yes** — `src/main/java/com/hbm_m/item/machine/ItemPileRod.java:25-46`; `ModItems.java:3284-3285`; verified by verifier slice |
| 4 | `sounds.json` `techbleep` referenced subtitle key `sounds.hbm_m.techbleep` that existed in no lang file/provider; NEXT's `tech_bleep` has no subtitle at all | `fabric/src/main/resources/assets/hbm/sounds.json:1777-1784` | `src/main/resources/assets/hbm_m/sounds.json:790` (dangling key) | Added the key to both lang providers (pre-existing entry, commit `79fb191c`) | low | **Yes** — `ModLanguageProviderEn.java:2288`, `ModLanguageProviderRu.java:3075`; generated `en_us.json:4920`, `ru_ru.json:3522` (grep this session) |
| 5 | Accepted deviation §3.6.6 claimed "same ogg audio", but `techbleep.ogg` was a different recording (pre-port file from commit `58265cca`); `upgrade_plug.ogg` was byte-identical | `fabric/.../assets/hbm/sounds/tool/techbleep.ogg` md5 `acfc03a8…`, 3256 B | `src/main/resources/assets/hbm_m/sounds/techbleep.ogg` was md5 `118d6048…`, 3336 B | Replaced the file with NEXT's; name deviation stays | low | **Yes** — `md5sum` both sides = `acfc03a83b8db863e20e907efc3ec9a2` (re-run this session) |
| 6 | DEFUSER stacked to 64; NEXT is `stacksTo(1)` (durability 100, inert here under deviation 1) | `fabric/.../items/ModItems.java:8206-8211` | `ItemDefuser.java` ctor + `ModItems.java:902-905` plain `Properties` | Apply `properties.stacksTo(1)` in the ctor | not recorded (round-2 verification) | **Yes** — `ItemDefuser.java:26`; verified live in the 1.21.1 generated tree by verifier |
| 7 | `desc.item.pileRod` in ru was a paraphrase, not NEXT's verbatim string | `fabric/.../assets/hbm/lang/ru_ru.json:2820` | `ModLanguageProviderRu.java` (paraphrase at finding time) | Set verbatim NEXT value; regeneration via pipeline runData | not recorded (round-2 verification) | **Yes** — provider `:2755-2757`; generated `ru_ru.json` byte-compare against NEXT = True (verifier; first two False prints were the comparer's own shell-escaping bug) |
| 8 | `tool.upgrade_plug` sound entry lacked `"category": "player"` (NEXT has it; the sibling `techbleep` entry already had it) | `fabric/.../assets/hbm/sounds.json` (`upgrade_plug`, category player) | `src/main/resources/assets/hbm_m/sounds.json` | Add the category line | not recorded (round-2 verification) | **Yes** — single-line diff confirmed by verifier; 3 `category` lines present in the entry region today |
| 9 | Report/javadoc drift once item-side tool dispatch landed: accepted deviation §3.6.3 ("rod sneak+screwdriver eject unreachable") ceased to be true | `fabric/.../items/tool/ItemTooling.java:39-62` (the dispatch NEXT always had) | `BlockGraphiteRod.java:25-30` javadoc + report §3.6.3/§6 | Rewrite javadoc (F-B) and report §3.6.3 → "superseded", §6 item 4 → DONE (F-B2) | docs | **Yes** — javadoc re-read this session; report rewritten the same session |

## 3. What was changed (this session)

| File | Change (one line) |
|---|---|
| `src/main/java/com/hbm_m/interfaces/IPileNeutronReceiver.java` | Javadoc now lists all three implementors (detector, fuel, breeding fuel) and the remaining pile machines. |
| `src/main/java/com/hbm_m/block/machines/pile/BlockGraphiteDrilledBase.java` | Both null-inserted-eject comments rewritten to describe NEXT's unconditional `new ItemStack` correctly; behavior unchanged. |
| `src/main/java/com/hbm_m/block/machines/pile/BlockGraphiteRod.java` | Class javadoc rewritten: the sneak+screwdriver eject is reached through the item-side dispatch, like NEXT. |
| `src/main/java/com/hbm_m/item/machine/ItemPileRod.java` | New: NEXT-equivalent tooltip item emitting `desc.item.pileRod` + per-item `.desc` lore lines. |
| `src/main/java/com/hbm_m/item/ModItems.java` | `PILE_ROD_BORON`/`PILE_ROD_DETECTOR` repointed to `ItemPileRod` (`:3284-3285`). |
| `src/main/java/com/hbm_m/item/tools_and_armor/ItemDefuser.java` | Ctor applies `properties.stacksTo(1)` (`:26`); `useOn` dispatches `onScrewForTool` (item-side tool dispatch). First forge compile attempt failed on this file (ctor arg order, exit 1) — fixed and recompiled. |
| `src/main/java/com/hbm_m/item/tools_and_armor/ScrewdriverItem.java` | Item-side `onScrew` dispatch to `BlockGraphiteDrilledBase.onScrewForTool` (door-controller check first, no sneak gate), mirroring NEXT `ItemTooling.useOn`. |
| `src/main/java/com/hbm_m/datagen/ModLanguageProviderEn.java` | Added `desc.item.pileRod` + both item `.desc` keys and `sounds.hbm_m.techbleep` = "Tech bleep" (`:2288`). |
| `src/main/java/com/hbm_m/datagen/ModLanguageProviderRu.java` | `desc.item.pileRod` set to NEXT's verbatim string (`:2755-2757`) + ru techbleep subtitle (`:3075`). |
| `src/main/resources/assets/hbm_m/sounds.json` | Added `"category": "player"` to `tool.upgrade_plug`. |
| `src/main/resources/assets/hbm_m/sounds/techbleep.ogg` | Replaced with NEXT's file (md5 now equal on both sides). |
| `docs/porting/reports/block_graphite_detector.md` (previous revision) | §3.6.3 rewritten as superseded, §6 item 4 marked DONE, §3.6.6 ogg-evidence corrected; overwritten by this report at session close. |
| `src/generated/resources/**` | Regenerated by the pipeline's runData: new lang keys, verbatim ru `desc.item.pileRod` (`ru_ru.json:1492`); asset content unchanged. |

## 4. Accepted Modernized-convention deviations — re-confirmed

1. **No tool durability loss** — zero `hurtAndBreak` in M pile code (javadoc mention only, `BlockGraphiteDrilledBase.java:110`); NEXT hurts the tool in `ItemTooling.useOn:52-58`.
2. **Non-tool clicks PASS instead of consume** — `BlockGraphiteNeutronDetector.java:108`, `BlockGraphiteDrilled.java:148`, `BlockGraphiteDrilledBase.java:120` vs NEXT's unconditional `onScrew` true.
3. **Rod sneak+screwdriver ejection** — originally unreachable; **superseded this session** by the item-side dispatch (`ScrewdriverItem.java:56-67` / `ItemDefuser.useOn` → `onScrewForTool` → base `onScrew:115-139`, never sneak-gated). The deviation no longer exists.
4. **Screwdriver on a drilled non-channel face consumes via the block hook** (`sidedSuccess`, no `hurtAndBreak`) where NEXT consumes via `onScrew` true — same accepted family.
5. **Tags** — all three ids in generated `mineable/pickaxe.json` **and** `needs_iron_tool.json` (project convention); NEXT ships only the pickaxe tag.
6. **Sound registry names** `techbleep` / `tool.upgrade_plug` vs NEXT `tech_bleep` / `upgrade_plug` (`ModSounds.java:74/:99`); both ogg files now byte-identical; all five play-site volume/pitch pairs equal.
7. **En display names drop NEXT's " (LEGACY)"** suffix; behavior-driving `desc.shared.cp1FuelAssembly`/`desc.shared.flux` byte-identical.
8. **BE registry name `"pile_neutrondetector"`** with the deliberate convention-breaking comment (`ModBlockEntities.java:812-816`), valid block = detector only — == NEXT `ModBlockEntities.java:966-969`.
9. **DEFUSER-only tool set** — NEXT's `DEFUSER_GOLD` (`ItemModDefuser extends ItemArmorMod`) never dispatches `onScrew`, so M's set is behaviorally equivalent.

## 5. Out of scope — left for other workers

- **`checkInteractions` plutonium/source/tritium rows** and the matching `BlockGraphiteDrilled` insertion branches (in-code TODOs); owned by those machine ports. The lithium row landed with the lithium port (`77992fdb`).
- **`insertItem` caller + `IInsertable` decision** — piston-inserter port. The method body itself is line-equivalent to NEXT `:160-231`.
- **Neutron transport feeding `receiveNeutrons`** — neutron-system port. The detector-facing seam already in-tree is faithful (`handler/neutron/PileNeutronHandler.java:106-115` == NEXT `:98-104`).
- **Stale stub-era generated loot/tags for `block_graphite_plutonium/source/tritium`** — must be repointed/regenerated when those machines land (NEXT loot for them is not drop-self).
- **`hbm_m:tags/blocks/pile/flux_absorber.json` empty** vs NEXT `["hbm:block_boron"]` — block_boron port.
- **NEXT `ItemDefuser.interactLivingEntity`** (creeper/glyphid behavior, NEXT `ItemDefuser.java:32-69`) — defuser item port.
- **`getTicker` forward-risk** — `BlockGraphiteDrilledTE` gates on `PILE_NEUTRONDETECTOR` only; a future `DrilledTE` subclass forgetting its own `getTicker` override compiles clean and never ticks (fuel `:81` and breeding fuel `:57` already override) — carry into the next pile machine's plan.
- **Missing ru item names** for `pile_rod_boron`/`pile_rod_detector` — parity-neutral (NEXT's `ru_ru.json` lacks them too).
- **Commit bookkeeping** — `b47cabe1` "port(block_graphite_detector)" bundled the fuel subsystem's files; `77992fdb` "port(block_graphite_lithium)" swept this session's fix files under a lithium message. Orchestrator owns git; verifier classified every hunk by content, all within scope.

## 6. Verification evidence

### 6.1 Gates

| Gate | Command | Result |
|---|---|---|
| 1.20.1-forge compile | `./gradlew :1.20.1-forge:compileJava` | **exit 0** (BUILD SUCCESSFUL, ~45 s, run by the fix round itself; its first attempt was exit 1 — javac ctor arg-order error in `ItemDefuser.java:26` — fixed, recompiled) |
| 1.21.1-neoforge compile | `./gradlew :1.21.1-neoforge:compileJava` | **exit 0** (BUILD SUCCESSFUL, ~33 s, run by the fix round) |
| runData (datagen) | pipeline-run | **exit 0** (deliberately not run by the fix/audit rounds — parallel-worker datagen races on this checkout; the regenerated `ru_ru.json` was afterwards byte-verified equal to NEXT) |

Final-round gates per the session outcome: 1.20.1-forge compile exit 0 · 1.21.1-neoforge compile exit 0 · runData exit 0. The final audit/verification rounds themselves ran **no gradle** — their conclusions are static (reads, JSON deep-equals, md5sums, git reads, generated stonecutter-output checks).

### 6.2 Verifier slices (all clean, zero open issues)

1. **Logic slice:** BE tick/NBT/`receiveNeutrons` line-for-line vs NEXT (both trigger conditions, rollover, `maxNeutrons` default 10, per-target stonecutter NBT gates verified in both generated trees); `DrilledTE` ticker path; drilled/rod/detector use hooks branch-by-branch; 1.21.1 main-hand-then-offhand emulation active in generated output; screwdriver/defuser dispatch chain (`ScrewdriverItem.java:59-68`, door-null fallback, no sneak gate); all five sound play sites equal; `ejectItem`/`insertItem`/`getInsertedItem` line-equivalent incl. SHROUDED preservation and BE-NBT carry via `PlatformHooks`; `checkInteractions` order preserved; `ItemPileRod` tooltip semantics vs NEXT `I18nUtil`; registration/lang/datagen wiring; `77992fdb` hunk-by-hunk scope classification.
2. **Assets slice:** blockstates 3/3 and block models 10+10 deep-equal after `hbm:`→`hbm_m:` normalization (all rotations); item defs/models equivalent across 1.21.4/1.20.1 formats; 13/13 textures md5-identical; loot 3/2/3 pools semantically identical (condition order, counts, sequences); en+ru lang parsed and compared (verbatim `desc.shared.*`, `desc.item.pileRod`, names minus "(LEGACY)"); tags counted; sounds.json entries + subtitle resolution in both locales; ogg md5s; recipes negative on both trees; registration counts; creative-tab placement; no hand-written shadow assets.
3. **Loader/stonecutter slice:** loader imports gated everywhere in compiled scope (the raw forge `LanguageProvider` import in datagen is the established convention, excluded from the neoforge source set and shipped jar); stonecutter gates resolve correctly in the generated trees of **both** targets (use hooks, BE NBT); gate-free files byte-identical raw↔generated; compile not run by the verifier (script owns compiles) — gate resolution verified statically.

### 6.3 Checks re-run in this final write-up round

Matrix entry state (`sed -n '222,238p'` worktree + `git show HEAD:…`), fixed-comment sites (`grep -n "would crash there"` → `BlockGraphiteDrilledBase.java:133/:252`; read of `IPileNeutronReceiver.java`), `ItemDefuser.java:26` `stacksTo(1)`, `ModItems.java:3284-3285` `ItemPileRod` registrations, subtitle keys in providers and generated langs (`ModLanguageProviderEn.java:2288`, `ModLanguageProviderRu.java:3075`, generated `en_us.json:4920`, `ru_ru.json:3522`), and `md5sum` of both `techbleep.ogg` copies (equal).

### 6.4 Not run / not verifiable

- **No in-game runtime test exists or was run** — not in this session, not in the port session. Interaction, NBT round-trip, tick, and sound behavior rest entirely on static line-by-line reads, JSON deep-equals, md5sums, git reads, and generated stonecutter-output checks, plus the compile/datagen gates. This is explicitly not a substitute for launching the game.
- **runData was not run by the fix or audit rounds** (parallel-worker datagen races on this checkout); it is covered by the pipeline's exit-0 gate, with the regenerated outputs spot-verified instead.
- **Minecraft engine internals** (use-hook vs item-`useOn` dispatch order; vanilla discard semantics) were not verifiable here — no MC sources exist in this environment; the relevant facts rely on earlier rounds' decompiled-MC evidence.

## 7. Matrix bookkeeping note

`docs/porting/machine_porting_matrix.json` — the `block_graphite_detector` entry reads `"portStatus": "UNPORTED"` both at HEAD (`77992fdb`) and in the worktree (line 238), **not** `"IN_PROGRESS"`; per the task's explicit condition no edit was made. The only uncommitted matrix hunk is the `block_graphite_lithium` `PORTED→IN_PROGRESS` flip. The port itself is complete and audit-passed; the orchestrator owns git and should reconcile the flip.
