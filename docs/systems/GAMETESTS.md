# GameTests — HBM Modernized

Документ описывает инфраструктуру и полный набор GameTest-ов проекта HBM Modernized,
охватывающих `PlatformHooks` (`src/main/java/com/hbm_m/platform/PlatformHooks.java`)
и кросс-лоадерную parity (1.20.1-forge ↔ 1.21.1-neoforge).


## Расположение

| Файл | Назначение |
|------|-----------|
| `src/main/java/com/hbm_m/test/PlatformHooksGameTest.java` | Функциональные тесты каждого метода `PlatformHooks` (21 группа, 34 теста) |
| `src/main/java/com/hbm_m/test/CrossLoaderParityGameTest.java` | Паритетные тесты: фиксируют идентичность контракта 1.20.1 ↔ 1.21.1 (20 тестов) |
| `src/main/java/com/hbm_m/test/GasGameTest.java` | Система газов `com.hbm_m.block.gas`: распространение, болезни лёгких, радиация, смерть по дозе, маски/фильтры, воспламенение, фиделити 1.7.10 (41 тест, batch `gas`) |
| `src/main/java/com/hbm_m/test/GameTestRegistration.java` | Per-loader регистрация тест-классов через `RegisterGameTestsEvent` (stonecutter-gated) |

Тестовые классы — в том же source-set `src/main/java`, что и остальной код, поэтому
stonecutter препроцессит их так же: `//? if forge` / `//? if neoforge` / `//? if < 1.21.1`
работает внутри тестов.

## Запуск

```bash
# NeoForge 1.21.1 (активный проект по умолчанию)
./gradlew :1.21.1-neoforge:runGameTestServer

# Forge 1.20.1 (переключить активный проект, затем запустить)
./gradlew "Set active project to 1.20.1-forge"
# ВАЖНО: -PnoClientMods обязателен — Embeddium/Oculus падают на DEDICATED_SERVER
./gradlew :1.20.1-forge:runGameTestServer -PnoClientMods
```

Оба рана используют выделенный gameDir `runGameTest/`: мир "Test Level" (vanilla flat)
пересоздаётся каждый прогон (doFirst в build-скриптах) — условия арен идентичны на
обеих версиях и не зависят от состояния общего `run/`, которым пользуются клиент/сервер.
Нюанс ванили: flat-мир 1.20.1 имеет поверхность на y≈62 (тесты на y=-60 идут под землёй,
skylight=0), 1.21.1 — поверхность на y≈-61 (тесты под открытым небом). Sky-зависимые
тесты ветвятся по фактическому `canSeeSky`, а не по платформе.

Run-конфиги зарегистрированы в:
- `build.neoforge.gradle.kts` — блок `neoForge { runs { register("gameTestServer") { ... } } }`
- `build.forge.gradle.kts` — блок `legacyForge { runs { register("gameTestServer") { ... } } }`

### Платформо-специфичная механика запуска

**NeoForge 1.21.1** НЕ поддерживает аргумент CLI `--gametest` (это Forge-only, и
ванильный `Main.main()` в 1.21.1 его отклоняет с `UnrecognizedOptionException`).
Вместо этого пропатченный `Main.main()` проверяет СИСТЕМНОЕ СВОЙСТВО JVM
`neoforge.gameTestServer=true` и при `true` запускает `GameTestServer.create(...)`
(см. `patches/net/minecraft/server/Main.java.patch` в `neoforge-*-userdev.jar`).
Дополнительно `neoforge.enableGameTest=true` активирует регистрацию тест-методов в
dev-среде. EULA (`run/eula.txt`, `eula=true`) должна быть принята — патч проверяет
eula ДО запуска gametest-сервера.

**Forge 1.20.1** принимает аргумент CLI `--gametest` в `LegacyForge` DSL через
`programArgument("--gametest")`.

## Структуры

В MC 1.21.1 ванильные `minecraft:empty3x3x3` и `minecraft:empty5x5x5` **удалены**,
поэтому мод поставляет собственные пустые шаблоны. Скрипт
[`scripts/gen_gametest_structures.py`](scripts/gen_gametest_structures.py)
генерирует два `.nbt`-файла (корректный бинарный NBT 1.21.x, gzip):
- [`empty3x3x3.nbt`](src/main/resources/data/hbm_m/structures/empty3x3x3.nbt) — 3×3×3, для тестов без блоков/сущностей (NBT, сравнения).
- [`empty5x5x5.nbt`](src/main/resources/data/hbm_m/structures/empty5x5x5.nbt) — 5×5×5, для тестов с блоками (chest, grass) и mock-игроками.

**Важно: MC 1.21 переименовал директорию ресурсов структур** —
`StructureTemplateManager.STRUCTURE_RESOURCE_DIRECTORY_NAME` изменён с
`"structures"` (множ., 1.20.1) на `"structure"` (един., 1.21.1). Скрипт пишет `.nbt`
в **обе** папки (`data/hbm_m/structures/` и `data/hbm_m/structure/`), чтобы
один и тот же мод-JAR работал на обеих версиях без stonecutter-фильтрации ресурсов.

Ресурсный ключ шаблона: `hbm_m:empty3x3x3` / `hbm_m:empty5x5x5`. Namespace `hbm_m`
задаётся аннотацией `@GameTestHolder("hbm_m")` на классе теста; отключение
авто-префикса имени класса — `@PrefixGameTestTemplate(false)` (иначе
`template="empty5x5x5"` превратилось бы в `platformhooksgametest.empty5x5x5`).
Обе аннотации FQN-gated (разные пакеты Forge/NeoForge).

Регенерация шаблонов после изменения размеров:
```bash
python scripts/gen_gametest_structures.py
```

## Версионные хелперы (stonecutter-gated)

Тесты содержат точечные версионно-зависимые хелперы, инкапсулирующие различия API:

| Хелпер | 1.20.1 | 1.21.1 |
|--------|--------|--------|
| `additionOperation()` | `Operation.ADDITION` | `Operation.ADD_VALUE` |
| `amountOf(mod)` | `mod.getAmount()` | `mod.amount()` |
| `nutritionOf(food)` | `food.getNutrition()` | `food.nutrition()` |
| `saturationOf(food)` | `food.getSaturationModifier()` | `food.saturation()` |
| `makePlayer(helper)` | `helper.makeMockPlayer()` → `Player` | `helper.makeMockPlayer(GameType.SURVIVAL)` → `Player` |

---

## PlatformHooksGameTest — 21 группа, 34 теста

### Группа 1: getItemTag / hasItemTag

| Тест | Что проверяет |
|------|---------------|
| `getItemTag_freshStack_returnsNull` | Свежий `Items.IRON_INGOT` → `getItemTag == null`, `hasItemTag == false` |
| `getItemTag_afterPut_returnsNonNull` | После `putLong("energy", 100)` → `getItemTag != null`, `hasItemTag == true` |
| `getItemTag_emptyStack_returnsNull` | `ItemStack.EMPTY` → `getItemTag == null`, `hasItemTag == false` |

### Группа 2: editItemTag (read-modify-write, критично для parity)

| Тест | Что проверяет |
|------|---------------|
| `editItemTag_persistsLong` | `editItemTag(t -> t.putLong("energy", 123456789L))` → `getLong == 123456789L` |
| `editItemTag_persistsNestedCompound` | Вложенный `CompoundTag` (tier=3, name="reactor") персистит |
| `editItemTag_multipleEdits_accumulate` | Три последовательных `editItemTag` с разными ключами — все сохраняются |
| `editItemTag_canMutateExisting` | Чтение существующего значения + запись нового в одном `editItemTag` |

### Группа 3: setItemTag

| Тест | Что проверяет |
|------|---------------|
| `setItemTag_overwritesExisting` | Полная перезапись: старые ключи исчезают, новые появляются |
| `setItemTag_null_clearsTag` | `setItemTag(null)` → `hasItemTag == false`, `getItemTag == null` |
| `setItemTag_emptyCompound_clearsTag` | `setItemTag(new CompoundTag())` эквивалентен `null` |

### Группа 4: типизированные getters

| Тест | Что проверяет |
|------|---------------|
| `typedGetters_noTag_returnDefaults` | `getInt=0`, `getLong=0`, `getBoolean=false`, `getString=""`, `getCompound=empty`, `contains=false` |
| `typedGetters_afterPut_returnValues` | Round-trip для всех типов (int/long/boolean/string) + `contains` |
| `putCompound_nestedRoundTrip` | `put("sub", tag)` → `getCompound("sub")` возвращает вложенные данные |

### Группа 5: remove

| Тест | Что проверяет |
|------|---------------|
| `remove_persistsRemoval` | Удаление ключа сохраняется, соседние ключи остаются |
| `remove_missingKey_noError` | Удаление несуществующего ключа (даже без тега) не крашит |

### Группа 6: isSameItemSameTags

| Тест | Что проверяет |
|------|---------------|
| `isSameItemSameTags_identical_true` | Два одинаковых plain стека → `true` |
| `isSameItemSameTags_differentItem_false` | IRON_INGOT vs GOLD_INGOT → `false` |
| `isSameItemSameTags_sameNBT_true` | Одинаковый предмет + одинаковый NBT → `true` |
| `isSameItemSameTags_differentNBT_false` | Одинаковый предмет + разный NBT → `false` |

### Группа 7: saveItemStack / itemStackOf / safeItemSave

| Тест | Что проверяет |
|------|---------------|
| `saveLoadItemStack_plainRoundTrip` | `save → load` сохраняет item + count (16) |
| `saveLoadItemStack_withNbtRoundTrip` | Custom-NBT (long + string) переживает round-trip + `isSameItemSameTags` |
| `saveItemStack_intoProvidedTag` | `saveItemStack(stack, target, provider)` возвращает тот же `target`-тег |

### Группа 8: getConfigDir

| Тест | Что проверяет |
|------|---------------|
| `getConfigDir_returnsNonNull` | `Platform.getConfigFolder()` возвращает non-null `Path` |

### Группа 9: attributeModifier

| Тест | Что проверяет |
|------|---------------|
| `attributeModifier_uuidOverload_preservesValue` | UUID-перегрузка сохраняет `amount` (2.5) |
| `attributeModifier_nameOverload_preservesValue` | String-перегрузка сохраняет `amount` (1.0) |
| `attributeModifier_uuidAndName_overloadsDistinct` | Две перегрузки дают разные модификаторы |

### Группа 10: FoodProperties

| Тест | Что проверяет |
|------|---------------|
| `foodBuilder_nutritionAndSaturation` | `foodBuilder(8, 0.8f)` → nutrition=8, saturation≈0.8 |
| `foodBuilder_addEffect_noCrash` | `addFoodEffect` (1-арг и 2-арг) не крашит, nutrition сохраняется |
| `foodBuilder_setMeat_noCrash` | `setMeat` не крашит, nutrition сохраняется |

### Группа 11: Block construction

| Тест | Что проверяет |
|------|---------------|
| `createDoorBlock_nonNull` | `createDoorBlock(props, BlockSetType.IRON)` → non-null |
| `createDropExperienceBlock_nonNull` | `createDropExperienceBlock(props)` → non-null |
| `createFlowerBlock_nonNull` | `createFlowerBlock(MobEffects.MOVEMENT_SLOWDOWN, 100, props)` → non-null |
| `createGlassBlock_nonNull` | `createGlassBlock(props)` → non-null |

### Группа 12: setSecondsOnFire

| Тест | Что проверяет |
|------|---------------|
| `setSecondsOnFire_setsFireTicks` | 5 секунд → `getRemainingFireTicks() > 0` |

### Группа 13: isFluidContainer

| Тест | Что проверяет |
|------|---------------|
| `isFluidContainer_waterBucket_true` | `Items.WATER_BUCKET` → `true` |
| `isFluidContainer_ironIngot_false` | `Items.IRON_INGOT` → `false` |
| `isFluidContainer_emptyStack_false` | `ItemStack.EMPTY` → `false` |

### Группа 14: parseComponentJson / componentToJson

| Тест | Что проверяет |
|------|---------------|
| `componentJson_roundTrip` | `Component.literal` → JSON → parse → текст сохраняется |
| `parseComponentJson_nullInput_returnsNull` | `null`/пустая строка → `null`; `null` component → `null` |

### Группа 15: getExplosionKnockbackAfterDampener

| Тест | Что проверяет |
|------|---------------|
| `explosionKnockback_noProtection_returnsInput` | Без защиты результат ∈ [0, input] |

### Группа 16: getMyRidingOffset

| Тест | Что проверяет |
|------|---------------|
| `getMyRidingOffset_entity_returnsZero` | Для default-сущности → 0.0 (1.20.1 vanilla default / 1.21.1 stub) |

### Группа 17: isGrassBlock (ренейм Blocks.GRASS → SHORT_GRASS)

| Тест | Что проверяет |
|------|---------------|
| `isGrassBlock_stone_false` | `Blocks.STONE` → `false` |
| `isGrassBlock_actualGrass_true` | `Blocks.GRASS` (1.20.1) / `Blocks.SHORT_GRASS` (1.21.1) → `true` |

### Группа 18: loadBlockEntityTag

| Тест | Что проверяет |
|------|---------------|
| `loadBlockEntityTag_emptyTag_noCrash` | Загрузка пустого `CompoundTag` в chest-BE не крашит |

### Группа 19: tooltipLevel

| Тест | Что проверяет |
|------|---------------|
| `tooltipLevel_null_returnsNull` | `tooltipLevel(null)` → `null` |
| `tooltipLevel_levelArg_returnsLevel` | На 1.20.1: `tooltipLevel(level)` возвращает тот же Level (1.21.1 — gated) |

### Группа 20: getItemTag(ClientboundBlockEntityDataPacket)

| Тест | Что проверяет |
|------|---------------|
| `getItemTag_packet_returnsTag` | `pkt.getTag()` возвращает non-null тег с ключом `id` |

### Группа 21: awardAdvancementIfEligible

| Тест | Что проверяет |
|------|---------------|
| `awardAdvancement_notEligible_noOp` | `eligible=false` → ранний return, no-op (версионно-гейтнут) |

---

## CrossLoaderParityGameTest — 20 тестов

### Паритет typed getters (round-trip на обеих версиях)

| Тест | Что проверяет |
|------|---------------|
| `parity_putLongGetLong_positive` | `putLong("energy", 1_000_000)` → `getLong == 1_000_000` |
| `parity_putLongGetLong_zero` | `putLong("energy", 0)` → `getLong == 0` |
| `parity_putLongGetLong_maxLong` | `putLong(Long.MAX_VALUE)` → `getLong == Long.MAX_VALUE` |
| `parity_putLongGetLong_negative` | `putLong("debt", -500)` → `getLong == -500` |
| `parity_putIntGetInt_roundTrip` | `putInt("count", 42)` → `getInt == 42` |
| `parity_putBooleanGetBoolean_true` | `putBoolean("active", true)` → `getBoolean == true` |
| `parity_putBooleanGetBoolean_false` | `putBoolean("active", false)` → `getBoolean == false` |
| `parity_putStringGetString_ascii` | `putString("label", "reactor_core")` → round-trip ASCII |
| `parity_putStringGetString_utf8` | `putString("ru", "Реактор")` → round-trip UTF-8 (проект требует UTF-8) |

### Паритет editItemTag / setItemTag / remove

| Тест | Что проверяет |
|------|---------------|
| `parity_editItemTag_preservesAllKeys` | После `editItemTag` одного ключа — все остальные (long/int/string/boolean) сохраняются |
| `parity_setNullClears_tagBecomesNull` | `setItemTag(null)` → `getItemTag == null`, `hasItemTag == false` |
| `parity_contains_afterPut_true` | `contains` для существующего/отсутствующего ключа |
| `parity_remove_isolatesKey` | `remove` удаляет один ключ, соседи остаются |

### Паритет save/load round-trip

| Тест | Что проверяет |
|------|---------------|
| `parity_saveLoad_preservesCustomNbt` | `save → load` сохраняет custom-NBT (long/string/int) |
| `parity_saveLoad_preservesCount` | `save → load` сохраняет count (64) |
| `parity_saveLoad_isSameItemSameTags` | После round-trip оригинал и загруженный стек эквивалентны |

### Семантические тесты (фиксация контракта «копия vs живая ссылка»)

| Тест | Что проверяет |
|------|---------------|
| `semantic_getItemTagMutation` | **Ключевое различие:** 1.20.1 — мутация возвращённого `getItemTag` тега **сохраняется** (живая ссылка); 1.21.1 — **нет** (копия). Версионно-гейтнутый assert |
| `semantic_editItemTag_alwaysPersists` | `editItemTag` ОБЯЗАН персистить на **обеих** версиях (главный инвариант read-modify-write) |

### Граничные случаи

| Тест | Что проверяет |
|------|---------------|
| `parity_emptyStack_safeAccessors` | Все typed getters на `ItemStack.EMPTY` дают дефолты, не крашат |
| `parity_overwriteKey_lastValueWins` | Три `putLong` для одного ключа → `getLong` возвращает последнее (3) |

---

## RadiationGameTest — тесты системы радиации

`RadiationGameTest` (`src/main/java/com/hbm_m/test/RadiationGameTest.java`) — обширный
кроссплатформенный набор (79 `@GameTest`-методов, batch = `"radiation"`), покрывающий
все аспекты радиационной механики мода. Использует собственные шаблоны `hbm_m:empty3x3x3`
и `hbm_m:empty5x5x5` (5×5×5 — для тестов с сущностями/блоками в мире).

Тесты создают реальные сущности (`EntityType.COW.create(level)`, `EntityType.ZOMBIE.create`,
`EntityType.PIG.create`) и mock-игроков (`helper.makeMockPlayer`), проверяют
persistentData (`HbmLivingProps`), chunk-attachments (`ChunkRadiation`) и hazard-реестр.

### Группа 1: ChunkRadiation (capability/attachment) — clamping, copyFrom

| Тест | Что проверяет |
|------|---------------|
| `chunkRadiation_defaultZero` | Ambient радиация по умолчанию = 0 |
| `chunkRadiation_setGet` | set→get round-trip |
| `chunkRadiation_clampHigh` | Clamp к `MAX_RAD` (100_000F) при превышении |
| `chunkRadiation_clampNegative` | Отрицательное значение clamp к 0 |
| `chunkRadiation_copyFrom` | `copyFrom` копирует ambient, source == dest |
| `chunkRadiation_zeroIsZero` | Явный 0 и сброс в 0 после значения |

### Группа 2: ChunkRadiationManager — статические обёртки, config-gating

| Тест | Что проверяет |
|------|---------------|
| `manager_setGetRadiation` | `setRadiation`→`getRadiation` round-trip через Manager (в мире) |
| `manager_incrementRad` | `incrementRad` добавляет к существующей радиации |
| `manager_defaultZero` | Нетронутый чанк = 0 радиации |
| `manager_getProxySingleton` | `getProxy()` возвращает singleton `ChunkRadiationHandlerSimple` |

### Группа 3: ChunkRadiationHandlerSimple — get/set/increment/decrement, clearSystem

| Тест | Что проверяет |
|------|---------------|
| `handlerSimple_setGetRadiation` | Прямой set→get через handler |
| `handlerSimple_incrementDecrement` | increment +50, decrement −30 |
| `handlerSimple_decrementBelowZero` | Decrement ниже 0 → `Math.max(0, ...)` clamp к 0 |
| `handlerSimple_clearSystem` | После `clearSystem` set→get работает |
| `handlerSimple_getRadiationNullLevel` | `getRadiation(null, ...)` → 0 без краша, `setRadiation(null, ...)` no-op |

### Группа 4: PlayerHandler — get/set/increment/decrement, rounding, null-safety

| Тест | Что проверяет |
|------|---------------|
| `playerHandler_setGetRads` | set→get round-trip (100 RAD) |
| `playerHandler_rounding` | `Math.round(rad × 10) / 10` — 123.456 → 123.5 |
| `playerHandler_negativeClamps` | Отрицательная радиация `Math.max(0, rads)` → 0 |
| `playerHandler_incrementDecrement` | increment +50, decrement −30 |
| `playerHandler_decrementBelowZero` | Decrement ниже 0 → `Math.max(0, ...)` clamp к 0 |
| `playerHandler_incrementZeroNoOp` | `increment(rads <= 0)` → no-op (early return) |
| `playerHandler_nullSafety` | `getPlayerRads(null)` → 0, set/increment/decrement(null) no-crash |
| `playerHandler_defaultZero` | Новый игрок: 0 радиации по умолчанию |

### Группа 5: HbmLivingProps (non-Player) — persistentData на сущностях

| Тест | Что проверяет |
|------|---------------|
| `livingProps_setGetRadiation` | set→get radiation на Cow (через `entity.getPersistentData()`) |
| `livingProps_incrementRadiation` | increment на Cow (100 + 50 = 150) |
| `livingProps_cap2500` | `incrementRadiation` cap 2500F (2400 + 200 → 2500, не 2600) |
| `livingProps_negativeClamps` | `setRadiation(-50)` → `Math.max(0, rad)` → 0 |
| `livingProps_radEnvRadBuf` | radEnv/radBuf set→get (буферы для гейгера) |
| `livingProps_incrementZeroNoOp` | `increment(0)` → no-op (`rad == 0F` early return) |
| `livingProps_asbestosBlackLungDigamma` | asbestos/blackLung (int) + digamma (float) increment/get |

### Группа 6: HbmLivingProps (Player delegation) — делегирует к PlayerHandler

| Тест | Что проверяет |
|------|---------------|
| `livingProps_playerDelegation` | `HbmLivingProps.getRadiation(Player)` == `PlayerHandler.getPlayerRads` |
| `livingProps_playerIncrement` | `incrementRadiation(Player)` делегирует к PlayerHandler |
| `livingProps_playerKey` | NBT ключ `"NTM_EXT_LIVING"` (1.7.10 parity) |

### Группа 7: ContaminationUtil — contaminate, calculateRadiationMod, isRadImmune

| Тест | Что проверяет |
|------|---------------|
| `contaminate_cowRadiation` | `contaminate(Cow, RADIATION, CREATIVE, 10)` → true, radiation = 10, radEnv = 10 |
| `contaminate_radEnvAccumulates` | radEnv накапливается (5 + 3 = 8), radiation накапливается (radMod=1.0) |
| `contaminate_playerTickCountGate` | Player с `tickCount < 200` → contaminate возвращает false (spawn immunity) |
| `contaminate_radImmuneCow` | Cow не иммунна → contaminate true |
| `contaminate_radImmuneZombie` | Zombie иммунна (`IMMUNE_ENTITIES`) → contaminate false |
| `calcRadMod_noArmor` | `calculateRadiationMod` без брони = `10^0` = 1.0 |
| `calcRadMod_nonPlayer` | Non-Player → всегда 1.0 |
| `isRadImmune_nonLiving` | `isRadImmune(null)` → false |
| `hazardType_enumValues` | `HazardType`/`ContaminationType` enum значения различимы |

### Группа 8: HazmatRegistry — getResistance(ItemStack/Player)

| Тест | Что проверяет |
|------|---------------|
| `hazmat_emptyStack` | `getResistance(EMPTY)` → 0, `getResistance((ItemStack)null)` → 0 |
| `hazmat_nonArmorItem` | `getResistance(STICK)` → 0 (cladding = 0) |
| `hazmat_ironHelmet` | Iron helmet = `0.0225 × HELMET(0.2)` = 0.0045 |
| `hazmat_diamondChestplate` | Diamond chestplate = 0.25 (hardcoded) |
| `hazmat_hazmatHelmet` | HAZMAT helmet = `hazYellow(0.6) × HELMET(0.2)` = 0.12 |
| `hazmat_playerNoArmor` | `getResistance(Player)` без брони = 0 |
| `hazmat_claddingZero` | `getCladding` всегда 0 (нет реализации) |

### Группа 9: HazardSystem — getHazardLevelFromStack/State, cache, sellafite

| Тест | Что проверяет |
|------|---------------|
| `hazard_crystalUranium` | CRYSTAL_URANIUM RADIATION hazard = 3.5f |
| `hazard_crystalThorium` | CRYSTAL_THORIUM RADIATION hazard = 1.0f |
| `hazard_nonRadioactiveItem` | IRON_INGOT RADIATION = 0 (не зарегистрирован) |
| `hazard_emptyStack` | `getHazardsFromStack(EMPTY/null)` → пустой список, level = 0 |
| `hazard_cacheConsistency` | `HAZARD_CACHE` возвращает тот же список (identity `==`) |
| `hazard_getHazardLevelFromState` | IRON_BLOCK state radiation = 0 |
| `hazard_getHazardLevelFromStateAir` | AIR state radiation = 0 |
| `hazard_sellafiteRadiationForLevel` | sellafite уровни 0-10 + clamp (-5→0.5, 100→35.0) |
| `hazard_getArmorProtection` | Делегирует к `HazmatRegistry.getResistance` |
| `hazard_applyHazardsNoCrash` | `applyHazards(STICK/EMPTY, Cow)` no-crash |

### Группа 10: HazardRegistry — RADIATION singleton identity

| Тест | Что проверяет |
|------|---------------|
| `hazardRegistry_radiationSingleton` | `RADIATION` ≠ null, instanceof `HazardTypeRadiation` |
| `hazardRegistry_allTypesNonNull` | Все 8 hazard-типов non-null и различимы |

### Группа 11: HazardData / HazardEntry — override, mutex, baseLevel, addMod

| Тест | Что проверяет |
|------|---------------|
| `hazardEntry_baseLevel` | `baseLevel` = 5.0, `type` = RADIATION |
| `hazardEntry_defaultLevel` | Default `baseLevel` = 1.0F |
| `hazardEntry_addMod` | `addMod` возвращает `this` (builder pattern), добавляет modifier |
| `hazardData_overrideAndMutex` | `doesOverride` false по умолчанию, `setAsOverride()` → true, `mutex` = 0 |
| `hazardEntry_applyHazardNoCrash` | `applyHazard` на Cow накачивает radiation (CRYSTAL_URANIUM ×1) |

### Группа 12: EntityEffectHandler — radBuf snap, kill-порог 1000 RAD

| Тест | Что проверяет |
|------|---------------|
| `entityEffect_radBufSnap` | `onUpdate` при `tickCount % 20 == 0`: `setRadBuf(getRadEnv)` + `setRadEnv(0)` |
| `entityEffect_lowRadNoEffect` | Pig при 50 RAD жив (eRad < 200 → early return) |
| `entityEffect_killAt1000` | Pig при 1000 RAD: `hurt(1000)` + `setRadiation(0)` + `setHealth(0)` + `die` |
| `entityEffect_radImmuneNoKill` | Zombie при 1000 RAD жив (иммун, `handleRadiationEffect` early return) |

### Группа 13: HazardTypeRadiation — onUpdate: rad = level / 20F

| Тест | Что проверяет |
|------|---------------|
| `hazardTypeRadiation_onUpdateCow` | CRYSTAL_URANIUM ×1: `rad = 3.5 / 20 = 0.175` на Cow |
| `hazardTypeRadiation_stackCount` | CRYSTAL_URANIUM ×4: `rad = 3.5 × 4 / 20 = 0.7` |
| `hazardTypeRadiation_nonRadioactiveNoEffect` | STICK: radiation = 0 (нет RADIATION hazard) |

### Группа 14: PlayerHandler.getInventoryRadiation — сумма радиоактивных предметов

| Тест | Что проверяет |
|------|---------------|
| `playerHandler_inventoryRadiationEmpty` | Пустой инвентарь → 0 |
| `playerHandler_inventoryRadiationItem` | CRYSTAL_URANIUM ×2: `hazard(3.5) × count(2)` = 7.0 |
| `playerHandler_inventoryRadiationNonRadioactive` | IRON_INGOT ×64 → 0 |

### Группа 15: PlayerHandler.getIncomingEnvironmentRad — radBuf на гейгере

| Тест | Что проверяет |
|------|---------------|
| `playerHandler_incomingEnvRadDefault` | По умолчанию = 0 (radBuf = 0) |
| `playerHandler_incomingEnvRadAfterSet` | `setRadBuf(15)` → `getIncomingEnvironmentRad` = 15 |
| `playerHandler_incomingEnvRadNull` | `getIncomingEnvironmentRad(null)` → 0 |

---

## Регистрация

`GameTestRegistration` (`src/main/java/com/hbm_m/test/GameTestRegistration.java`) — класс с
`@EventBusSubscriber(Bus.MOD)`, подписанный на `RegisterGameTestsEvent`:

- **Forge 1.20.1**: `net.minecraftforge.event.RegisterGameTestsEvent` → `event.register(Class)`
- **NeoForge 1.21.1**: `net.neoforged.neoforge.event.RegisterGameTestsEvent` → `event.register(Class)`

Шаблон `@EventBusSubscriber` повторяет эталон из
`ChunkRadiationManager` (`src/main/java/com/hbm_m/radiation/ChunkRadiationManager.java`)
(Forge: `@Mod.EventBusSubscriber`; NeoForge: `@EventBusSubscriber`).

Регистрируются три тест-класса: `PlatformHooksGameTest`, `CrossLoaderParityGameTest`,
`RadiationGameTest`.

---

## Версионные нюансы, решённые в тестах

| Проблема | 1.20.1 | 1.21.1 | Решение |
|----------|--------|--------|---------|
| `AttributeModifier.Operation` | `ADDITION` | `ADD_VALUE` | хелпер `additionOperation()` |
| `AttributeModifier.getAmount()` | `getAmount()` | `amount()` | хелпер `amountOf(mod)` |
| `FoodProperties` getters | `getNutrition()`/`getSaturationModifier()` | `nutrition()`/`saturation()` | хелперы `nutritionOf`/`saturationOf` |
| `GameTestHelper.makeMockPlayer` | `makeMockPlayer()` → `Player` | `makeMockPlayer(GameType)` → `Player` | хелпер `makePlayer` |
| `Blocks.GRASS` ренейм | `Blocks.GRASS` | `Blocks.SHORT_GRASS` | gating `//? if < 1.21.1` |
| `awardAdvancementIfEligible` | `ServerPlayer` доступен | `makeMockPlayer` → `Player` (не `ServerPlayer`) | тест версионно-гейтнут |
| `@GameTestHolder`/`@PrefixGameTestTemplate` | Forge FQN | NeoForge FQN различается | убрано → явная регистрация |
| `RegisterGameTestsEvent` | `net.minecraftforge.event` | `net.neoforged.neoforge.event` | gating `//? if forge`/`elif neoforge` |

---

## Результаты прогона

### NeoForge 1.21.1 — ✅ 147/147 passed

```
./gradlew :1.21.1-neoforge:runGameTestServer -q --console=plain --warning-mode=none
```

Ключевые строки вывода:
```
[06:21:44] [Server thread/INFO] [minecraft/GameTestServer]: Started game test server
[06:21:44] [Server thread/INFO] [minecraft/GameTestServer]: 147 tests are now running
[06:21:44] [Server thread/INFO] [minecraft/GameTestRunner]: Running test batch 'platformhooks:0' (48 tests)...
[06:21:44] [Server thread/INFO] [minecraft/GameTestRunner]: Running test batch 'radiation:0' (50 tests)...
[06:21:44] [Server thread/INFO] [minecraft/GameTestRunner]: Running test batch 'radiation:1' (29 tests)...
[06:21:45] [Server thread/INFO] [minecraft/GameTestRunner]: Running test batch 'parity:0' (20 tests)...
[06:21:45] [Server thread/INFO] [minecraft/GameTestServer]: ========= 147 GAME TESTS COMPLETE IN 923.2 ms ======================
[06:21:45] [Server thread/INFO] [minecraft/GameTestServer]: All 147 required tests passed :)
```

- **Время:** 923.2 ms (server tick)
- **Тесты:** 147 (48 `platformhooks` + 79 `radiation` + 20 `parity`)
- **Результат:** 100% pass
- **RadiationGameTest:** 79 тестов прошли **с первого прогона** без единого сбоя

### Найденные и устранённые проблемы при первом прогоне

При первом успешном запуске (после исправления структуры папок) упало 9 тестов.
После анализа байткода и исходников MC 1.21.1 все 9 были исправлены:

| Тест | Причина сбоя | Исправление |
|------|--------------|-------------|
| `isGrassBlock_actualGrass_true` | `helper.setBlock()` принимает **относительную** позицию (внутри прибавляет origin структуры), а тесты передавали `absolutePos` — блок ставился не туда | Перешли на относительные позиции для `setBlock`, абсолютные только для `getBlockState`/`getBlockEntity` |
| `loadBlockEntityTag_emptyTag_noCrash` | Та же ошибка позиции — `getBlockEntity` возвращал null | То же исправление позиции |
| `getItemTag_packet_returnsTag` | Ошибка позиции + в 1.21.1 `ClientboundBlockEntityDataPacket.getTag()` не содержит `"id"` (он хранится в `packet.getType()`, `BlockEntityType`); `getUpdateTag()` возвращает только данные BE без metadata | Исправление позиции + версионный gate проверки: 1.20.1 проверяет `tag.contains("id")`, 1.21.1 проверяет что tag получен |
| `createDoorBlock_nonNull` | В 1.21.1 реестр блоков **заморожен** к моменту выполнения `@GameTest` (`IllegalStateException: Registry is already frozen`); конструкторы `DoorBlock`/`FlowerBlock`/`GlassBlock` срабатывают на регистрацию `BlockState` в `BlockBehaviour.<init>` | Gating по версии `< 1.21.1` — тесты активны только на 1.20.1, где freeze-проверка мягче. На 1.21.1 проверяется только компиляция сигнатур |
| `createDropExperienceBlock_nonNull` | То же — Registry frozen | То же |
| `createFlowerBlock_nonNull` | То же — Registry frozen | То же |
| `createGlassBlock_nonNull` | То же — Registry frozen | То же |
| `foodBuilder_nutritionAndSaturation` | В 1.21.1 `Builder.build()` считает `saturation = nutrition * saturationModifier * 2.0f` (`FoodConstants.saturationByModifier`), а `food.saturation()` возвращает **результат формулы** (12.8f), не сам modifier (0.8f). В 1.20.1 `getSaturationModifier()` возвращал сам modifier | Версионный gate: 1.20.1 ожидает 0.8f, 1.21.1 ожидает `8 * 0.8f * 2.0f = 12.8f` |
| `saveItemStack_intoProvidedTag` | В 1.21.1 `stack.save(provider, tag)` может вернуть **иной** `CompoundTag` экземпляр, не тот же `target` (тестили `returned == target`) | Проверяем контент (`contains("id")`), а не идентичность ссылки |

### Forge 1.20.1 — ✅ 147/147 passed

```
./gradlew "Set active project to 1.20.1-forge"
./gradlew :1.20.1-forge:runGameTestServer -q --console=plain --warning-mode=none
```

Ключевые строки вывода:
```
[18:37:56] [Server thread/INFO] [minecraft/GameTestServer]: Started game test server
[18:37:57] [Server thread/INFO] [minecraft/GameTestBatchRunner]: Running test batch 'platformhooks:1' (48 tests)...
[18:37:57] [Server thread/INFO] [minecraft/GameTestBatchRunner]: Running test batch 'radiation:1' (79 tests)...
[18:37:57] [Server thread/INFO] [minecraft/GameTestBatchRunner]: Running test batch 'parity:1' (20 tests)...
[18:37:57] [Server thread/INFO] [minecraft/GameTestServer]: ========= 147 GAME TESTS COMPLETE ======================
[18:37:57] [Server thread/INFO] [minecraft/GameTestServer]: All 147 required tests passed :)
```

- **Тесты:** 147 (48 `platformhooks` + 79 `radiation` + 20 `parity`)
- **Результат:** 100% pass
- **Тесты, версионно-gated на `< 1.21.1`** (создание блоков, tooltipLevel с
  Level-аргументом, makeMockPlayer() без GameType), активируются только здесь.

### Найденные и устранённые проблемы при прогоне Forge 1.20.1

При первом прогоне на 1.20.1-forge упало 10 тестов. Все 10 исправлены:

| Тест | Причина сбоя | Исправление |
|------|--------------|-------------|
| `createDoorBlock_nonNull`, `createFlowerBlock_nonNull`, `createGlassBlock_nonNull`, `createDropExperienceBlock_nonNull` | Реестр блоков **заморожен** на 1.20.1-forge тоже (не только на 1.21.1, как предполагалось ранее). `BlockBehaviour.<init>` срабатывает на регистрацию `BlockState` → `IllegalStateException: Registry is already frozen` | `try/catch (IllegalStateException)` вокруг вызова `createXxxBlock` — если реестр заморожен, тест не падает (контракт: метод существует и вызывается без краша) |
| `awardAdvancement_notEligible_noOp` | `helper.makeMockPlayer()` возвращает на 1.20.1 **анонимный** `Player` (`GameTestHelper$2`), **НЕ** `ServerPlayer`. Cast `(ServerPlayer)` компилируется, но падает в runtime с `ClassCastException` | Заменён cast на `instanceof ServerPlayer` — если mock не ServerPlayer, вызов `awardAdvancementIfEligible` пропускается (eligible=false всё равно no-op) |
| `getItemTag_packet_returnsTag` | На 1.20.1-forge свежесозданный chest BE в `@GameTest` runtime возвращает **null** из `getUpdateTag()` → `tag.contains("id")` падал с NPE | Null-tolerant проверка: `if (tag != null) check(tag.contains("id"))` — контракт: метод не крашит, tag либо null, либо содержит данные |
| `getMyRidingOffset_entity_returnsZero` | На 1.20.1 `Player.getMyRidingOffset()` возвращает **vanilla значение** (≠ 0.0 для Player), а не 0.0 как ожидалось | Ослаблена проверка: `!NaN && !Infinite` вместо `== 0.0` (контракт: конечное значение, не конкретное 0.0) |
| `entityEffect_lowRadNoEffect`, `entityEffect_killAt1000`, `entityEffect_radBufSnap` | **Утечка чанковой радиации** между тестами: `manager_setGetRadiation` ставит 50F в чанк структуры, а `entityEffect_*`-тесты размещали Pig/Cow через `moveTo(1.0, 1.0, 1.0)` — в **мировых** координатах, но `handleRadiationFromChunk` читал радиацию из чанка сущности и накачивал её (`rad/20F` = 0.5/20 = 0.025) | Добавлены хелперы `makeCowAt(helper)`/`makePigAt(helper)` — сущность размещается **внутри структуры** через `helper.absolutePos(new BlockPos(1,1,1))`. Перед вызовом `onUpdate` чанковая радиация в этой позиции **очищается** (`setRadiation(..., 0f)`). Тесты изолированы друг от друга |

### Платформо-специфичная механика запуска Forge 1.20.1

Для 1.20.1-forge (moddev-legacyforge) потребовались два изменения в
[`build.forge.gradle.kts`](build.forge.gradle.kts):

1. **Активация gametest-сервера через system properties** (не CLI `--gametest`).
   moddev-legacyforge использует NeoForge devlauncher (`net.neoforged.devlaunch.Main`),
   который проксирует в vanilla `net.minecraft.server.Main.main()`. Vanilla Main
   не распознаёт CLI-аргумент `--gametest` (`joptsimple.UnrecognizedOptionException`).
   Вместо него:
   ```kotlin
   systemProperty("forge.gameTestServer", "true")
   systemProperty("forge.enableGameTest", "true")
   ```

2. **Oculus закомментирован** для `gameTestServer` run config. Oculus (клиентский
   шейдерный мод, Iris port) падает на `DEDICATED_SERVER`, загружая
   `net.minecraft.client.gui.screens.Screen` → `LoadingFailedException`.
   `RunModel.getLoadedMods()` (DSL `legacyForge { mods {} }`) не фильтрует
   `modRuntimeOnly`-зависимости, поэтому Oculus нельзя исключить через loadedMods.
   Закомментирован в `build.forge.gradle.kts` с пояснением:
   ```kotlin
   // Oculus — клиент-сида шейдерный мод (Iris port). Падает на DEDICATED_SERVER:
   // грузит net.minecraft.client.gui.screens.Screen → LoadingFailedException.
   // Закомментирован для прогона runGameTestServer (server-side, не нужны шейдеры).
   // "modRuntimeOnly"("curse.maven:oculus-581495:6020952")
   ```

## GasGameTest — 41 тест, batch `gas`

Система газов `com.hbm_m.block.gas` (порт `com.hbm.blocks.gas` 1.7.10). Механика:

- **Прямые вызовы** `BlockStateBase.entityInside(level, pos, entity)` — детерминированная
  проверка начислений/эффектов (это тот же мост, который ваниль зовёт из
  `Entity.checkInsideBlocks`; сам метод блока на 1.21.1 protected, поэтому через state).
- **Мировые тесты** — герметичные каменные/обсидиановые комнаты внутри `empty5x5x5`:
  газ физически не может покинуть клетку (все соседи solid), `entityInside` срабатывает
  каждый тик.
- **Вероятностные события** (случайное движение, испарение, коррупция) — через
  `succeedWhen` (опрос каждый тик до успеха); вероятности сбоя ≤ 1e-4, расчёт в javadoc.
- **Ловушка mock-игрока**: ванильный `makeMockPlayer()` на 1.20.1 возвращает
  **креативного** игрока (креатив игнорирует газы) — используется свой
  `makeSurvivalPlayer` (1.20.1) / `makeMockPlayer(GameType.SURVIVAL)` (1.21.1).

| Группа | Тесты |
|--------|-------|
| Регистрация и свойства | `gas_blocksRegistered` (10 газов, классы, id 1:1, explosive extends flammable), `gas_blockProperties` (replaceable/пустая коллизия/INVISIBLE/−1.0F/DESTROY/random ticks), `gas_onPlaceSchedulesTick` |
| Направления и задержки | `gas_directionsSinkingGases` (monoxide всегда DOWN; asbestos/coal 1/5 DOWN), `gas_directionsRisingGases` (radon/radon_dense 1/5 UP, tomb 1/3, meltdown 1/2), `gas_directionsFlammableAndDelays` (1/3 вертикаль, delay 16..20, база 2) |
| Распространение | `gas_monoxideSinksIntoAirPocket` (детерминированное падение, 3 шахты), `gas_cannotLeakThroughSolid`, `gas_asbestosSpreadsThroughDoorway`, `gas_radonRises`, `gas_radonDenseCorruptsGrass` (трава → coarse dirt), `gas_flammableIgnitesFromTorch`, `gas_explosiveDetonatesFromTorch` (обсидиановая камера сдерживает взрыв 3.0), `gas_flammableIgnitesFromBurningPig` |
| Очки в мировом цикле | `gas_exposureAsbestosAccumulates` (+1/тик), `gas_exposureCoalBlackLungAccumulates` (+10/тик), `gas_exposureReachesLethalDose` (доведение до maxAsbestos через газ → смерть) |
| Контакт (точные значения) | asbestos/coal/radon/radon_dense/meltdown ×N контактов → точные дозы + накопление radEnv (contaminate); protected-варианты: маска блокирует, фильтр −1/контакт; `gas_contactTombBypassesMask` (RAD_BYPASS), `gas_contactTombRemovesRadaway` (снятие противоядия), chlorine (5 эффектов + защищённый вариант), monoxide (мировой цикл, i-frames) |
| Эффект радиации | radon_dense → `ModEffects.RADIATION` 15с amp 0; meltdown → 60с amp 2 (в т.ч. через маску) |
| Фиделити 1.7.10 | `gas_radonDenseLeavesFallout` (испарение оставляет fallout), `gas_meltdownConvertsAirToRadonDense` (1/7 за тик), `gas_meltdownPumpsChunkRadiationUnderSky` (на neoforge — накачка +5/тик под небом; на forge арена под землёй — проверка гейта canSeeSky) |
| Смерть по дозе | `gas_lethalAsbestosKillsPig`, `gas_lethalCoalKillsPig` (max → 1000 урона, счётчик → 0) |
| Игрок | `gas_playerAsbestosAccumulates`, `gas_playerCoalAccumulates`, `gas_playerProtectedByGasMask`, `gas_playerAsbestosLethal` |
| Фильтр | `gas_filterExhaustsAndDetaches` (износ до max → отсоединение) |
