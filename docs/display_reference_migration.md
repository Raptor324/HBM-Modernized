# Display-reference migration (1.7.10 poses → HBM-Modernized)

**Статус: ЗАВЕРШЕНО (2026-11-14).** Все не-стандартные предметы, существовавшие в 1.7.10,
рендерятся под machine-verified позами оригинала. Чек-лист ниже; осознанные остаточные расхождения — в конце.

| Категория | Механизм | Статус |
|---|---|---|
| Статические машины (прессы, наковальни, центрифуги, циклотрон, zirnox, ... — 54 шт.) | выпечка `finalDisplay` в datagen | ✅ завершено |
| Броня ajr/ajro/bismuth/dns/t51 (16 шт.) | патч display + снятие separate_transforms (3D в GUI) | ✅ завершено |
| Анимированные машины (17 `*_anim.json`) | не-GUI слоты из reference; 6 машин переведены с baked-моделей на `*_anim` (BEWLR был мёртв) | ✅ завершено |
| Ракеты (missile_*) | runtime `applyReferencePose` (фрейм + тело + GUI-спин); двойной meshScale устранён | ✅ завершено |
| Турели (12) | runtime `applyReferencePose`, историческая таблица — fallback | ✅ завершено |
| RBMK-колонны/autoloader | сверены численно с reference — совпадали изначально, без изменений | ✅ завершено |
| Пушки SEDNA, тринкеты, legacy-3D | порты тех же 1.7-классов + фреймы — совпадают; flat-GUI соответствует 1.7.10 | ✅ завершено |
| Advanced Assembly Machine / Machine Assembler | алиасы исправлены по геометрии (±1.5 ↔ machine_assembly_machine, ±2.5 ↔ machine_assembly_factory) | ✅ завершено |
| Precision Assembler | item через `MachinePrecAssRenderer.renderItem` — 1:1 RenderPrecAss#getRenderer: корпус + **4 манипулятора** renderArm×4 с поворотом 90°, как в 1.7.10. Внешняя поза — из reference (machine_precass) | ✅ завершено |
| Radar | прямая причина: обе части объявлены динамическими → авто-деривация item-частей давала пустой фильтр (0 квадов). Фикс: `.itemParts("Base", "Dish")` в спеке; display из reference (в 1.7.10 иконка та же, но с вращением блюда) | ✅ завершено |
| «Призраки» при установке (Nucleus) | централизованный фикс в `NucleusDispatcherBypass`: evict-by-position в `addMachine` + all-specs free в `dropMachineCaches` + per-frame presence-ревалидация в `collectMain` | ✅ завершено |
| Числовая верификация | round-trip фреймов 0 ошибок; math сверена (anvil/satellite); GameTest против дампа | ⬜ см. ниже |

Item display poses (GUI, hands, ground, item frame) for every non-standard item now come from the
machine-verified reference dump of the original 1.7.10 renderers. The system has
two halves: the **reference dump** of the 1.7.10 poses, and consumers in this repo (baked JSON
display, runtime BEWLR pose application).

## The reference dump

`src/main/resources/data/hbm_m/display_reference.json` is produced by a display-dump tool
(plain JVM, JOML + Gson, no Minecraft) from the original 1.7.10 item definitions. Per item it holds:

- `markerDisplay` — the 1.7 frame's `display{}` triples (what vanilla applies before a BEWLR);
- `body` — the body matrix per slot the legacy 1.7.10 BEWLR applies under that frame (GUI spin
  frozen at 0), column-major;
- `def` — the raw body fields (scale/yaw/pitch/itemTranslate/contextTransforms/guiSteps/...) so a
  runtime can compose the chain itself and insert the GUI spin at its real place;
- `finalDisplay` / `finalDisplayMissing` — frame × body decomposed back into triples for slots
  where that is expressible (a guiSteps per-axis scale between rotations is not);
- `bounds`, `guiSpinSpeed`, `spinStep`, `animation`, `oversized` flags.

Regeneration: rerun the display-dump tool against the 1.7.10 item definitions and copy the output
over the file. The dump round-trips every frame model through
reconstruct/decompose (0 failures = the semantics match vanilla `ItemTransform`).

Name resolution: exact item path, then the `machine_` prefix rule, then `DisplayReference.ALIASES`
(the modern names differ from the 1.7.10 names in places: `klystron` → `fusion_klystron`,
`flare_stack` → `machine_flare`, ...).

## Consumers

1. **Static machines** (`ModItemModelProvider.blockItemFromBlockModelMachine`): when the reference
   has the item and the pose is fully static, the item model's `display` is baked from
   `finalDisplay` folded around the model's rebased root transform
   (`DisplayReference.rebasedRootTransform`); the old `copyBlockModelDisplay` stays as fallback.
   Datagen logs unmatched/animated/unrepresentable items each run.
2. **Armor** (`tools/display_reference/patch_armor_display.py`): the ajr/ajro/bismuth/dns/t51
   pieces get `finalDisplay` + `gui_light: side`, and `forge:separate_transforms` (flat GUI icon)
   is unwrapped — armor icons are 3D in the GUI like 1.7.10 draws them.
3. **Animated machines** (`tools/display_reference/patch_anim_display.py`): the `*_anim.json`
   models take the reference poses for ground/fixed/head/hands; the hand-tuned GUI slot is kept
   (equal up to a spelling-level z-offset). klystron/klystron_creative/mhdt/torus/thresher/
   steam_engine item models now point at their `*_anim` models — the BEWLR was dead before
   because a baked parent model never reports `isCustomRenderer()`.
4. **Missiles** (`ItemRenderMissileGeneric`) and **turrets** (`TurretItemRenderer`): when the
   reference knows the item, the pose is applied at runtime by
   `DisplayReference.applyReferencePose` (frame triple + body chain + GUI spin where the 1.7.10
   renderer puts it); the historical chains remain as fallback for reference-less items
   (missile_topol, missile_9m723*).

## Known deltas (accepted, revisit if visible)

- **GUI slot clipping**: the 1.7.10 BEWLR scissors machine icons into the 16×16 slot unless
  `oversized_in_gui`; vanilla baked quads don't clip, so a large non-oversized machine can spill a
  few pixels past the slot here.
- **Drop hover height**: `ItemEntityRenderer` lifts the item by `0.25 · ground.scale.y` of the
  *model's* transform. Baked items carry the composed scale, the 1.7.10 chain the frame's scale —
  the bob height differs by a couple of centimetres.
- **RBMK mini panels** (rbmk_display/gauge/...): 1.7.10 poses them 3D via the machine frame;
  this repo still ships flat icons (`RBMK_FLAT_ITEM_TEXTURES`) — no reliable item geometry here yet.
- **hazmat armor**: not a machine_obj item (no reference), keeps its local display.
- The **crucible** is flagged unrepresentable (1.7.10 composes it as teisr⁻¹·weapon at runtime);
  it keeps its local display.
- **missile_n2**: the reference composes it at scale 1.5, this repo's render data says 1.0 — the
  reference wins now; if the icon looks oversized next to its in-hand size, align the render data.
- **microwave**: the baked display is byte-equal to the reference on identical geometry;
  the only known difference is the tiny `plate_Cylinder` group the 1.7.10 item excludes.
  If it still looks wrong in game, compare against the 1.7.10 icon and describe the delta.
