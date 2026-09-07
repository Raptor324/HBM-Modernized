# Ревью серверной стабильности (1.21.1 / NeoForge)

Ветка `funni-stuff`, ревью от 2026-09-07.

Проверка велась на **настоящем dedicated server**: `./gradlew :1.21.1-neoforge:runGameTestServer`
(headless `GameTestServer`, launch target `forgeserverdev`, `Server Running: true`), а не в одиночном мире.

`:1.21.1-neoforge:compileJava` проходит чисто — все проблемы ниже рантаймовые.

**Было:** сервер падал на 3-м GameTest, 1357 рецептов.
**Стало:** `Loaded 3301 recipes`, `All 287 required tests passed`, в логе нет ни одной ошибки мода.

---

## Сделано (P0, краши сервера)

### 1. Не зарегистрирован ни один `DamageType` → краш при любом уроне мода

Реальный краш с сервера:

```
com.hbm_m.damagesource.ModDamageSources.create(ModDamageSources.java:26)
  -> Registry.getHolderOrThrow -> Optional.orElseThrow
com.hbm_m.block.gas.BlockGasMonoxide.affect(BlockGasMonoxide.java:30)

GameTest entityeffect_killat1000:
  Missing key in ResourceKey[minecraft:damage_type]: hbm_m:radiation
```

**Причина.** В 1.21 `DamageType` — datapack-реестр, на каждый ключ нужен
`data/hbm_m/damage_type/<name>.json`. `ModDamageTypes` объявляет 56 ключей, но записи создавались
только датагеном (`DataGenerators.getRegistrySetBuilder()`, рефлексия по полям класса), а датаген:

- целиком закрыт `//? if forge {` — на NeoForge его нет,
  и run-конфигурации `data` в `build.neoforge.gradle.kts` тоже нет;
- пишет в `src/generated/resources/data/`, который числится в `.gitignore`,
  то есть данных нет в репозитории и для Forge при чистом клоне.

Итог: радиация, газы, взрывы, пули, лазеры — любой источник урона ронял сервер.

**Исправление.** 56 записей положены статически в `src/main/resources/data/hbm_m/damage_type/*.json`
(`message_id` = имя ключа, `scaling: when_caused_by_living_non_player`, `exhaustion: 0.1` — ровно то,
что делал датаген). Работает на всех платформах без датагена. Конфликта с датагеном на Forge нет:
`ModPlatformPlugin` ставит `DuplicatesStrategy.EXCLUDE`.

### 2. Пять сущностей без атрибутов на NeoForge

```
Entity hbm_m:ufo has no attributes
Entity hbm_m:bot_prime_head has no attributes
Entity hbm_m:bot_prime_body has no attributes
Entity hbm_m:rad_beast has no attributes
Entity hbm_m:maskman has no attributes
```

**Причина.** В `entity/ModEntityEvents` два параллельных блока — `//? if forge` и `//? if neoforge`.
Forge-блок регистрирует атрибуты для этих пяти сущностей, NeoForge-блок — нет.
Расхождение веток при портировании. Спавн такой сущности роняет сервер.

**Исправление.** Пять `event.put(...)` добавлены в NeoForge-блок по образцу Forge.
Spawn placements в обеих ветках сверены — там расхождений нет.

### 3. Несуществующий биом в тегах структур

```
Couldn't load tag hbm_m:has_structure/coast as it is missing following references:
    minecraft:stony_beach (from mod/hbm_m)
Couldn't load tag hbm_m:has_structure/beach — то же
```

`minecraft:stony_beach` удалён ещё в 1.18, актуальное имя `minecraft:stony_shore`
(в соседнем `land.json` уже правильное). Из-за одной битой записи **не грузится весь тег**,
то есть структуры не спавнятся на побережье.

**Исправление.** `stony_beach` -> `stony_shore` в `has_structure/beach.json` и `has_structure/coast.json`.

---

## A. Главное оставшееся: datapack-контент не портирован на 1.21.1

21 упавший GameTest — все про рецепты:

```
assemblerrecipes_loaded: AssemblerRecipe list must NOT be empty
  — JSON recipes failed to load on this version!
    Check PlatformRecipeSerializer.MapCodec / ItemStack format (item: vs id:). Count=0
chemicalplantrecipes_loaded: ... Count=0
```

`src/main/resources/data/hbm_m/recipes/` содержит только `arc_furnace` (36) и `combination_oven` (30).
Всё остальное создаётся датагеном — а в `com.hbm_m.datagen.recipes.custom` около **30 генераторов**
(assembler, chemical plant, press, centrifuge, crystallizer, cyclotron, breeder, mixer, purex,
crucible, electrolyser, fraction tower, hydrotreater и т.д.).

В `src/generated/resources` сейчас лежит только `assets/hbm_m/lang` — весь `data/` отсутствует.

**Портировать датаген на NeoForge не требуется.** В `build.neoforge.gradle.kts` уже есть
полноценный конвертер: `processResources` нормализует вывод датагена (1.20.1) в формат 1.21 —
переименование директорий (`recipes`→`recipe`, `advancements`→`advancement`,
`loot_tables`→`loot_table`, `structures`→`structure`, `tags/blocks`→`tags/block` и т.д.),
теги `forge:`→`c:`, `forge/biome_modifier`→`neoforge/biome_modifier` с типом `neoforge:add_features`,
ремап silk-touch предикатов в loot-таблицах и `"result": {"item": X}` → `{"id": X}`
для ванильных рецептов. Кастомные рецепты `hbm_m:*` нормализует `RecipeHooks` в рантайме.

То есть архитектура сознательная: **датаген 1.20.1-only + нормализация вывода на NeoForge**.

Настоящая причина пустоты — датаген просто **никогда не запускался**: в
`src/generated/resources` есть только `assets/hbm_m/lang`, а `src/generated/resources/data/`
числится в `.gitignore`, поэтому контент не переносится между машинами.

**Порядок действий:**

```bash
./gradlew stonecutterSwitchTo1.20.1-forge
./gradlew :1.20.1-forge:runData -PnoClientMods   # флаг обязателен: Embeddium падает в data-режиме
./gradlew stonecutterSwitchTo1.21.1-neoforge
```

**Результат прогона (сделано).** Датаген выдал **4242 файла данных**, которых не было вообще:

| категория | файлов |
|---|---|
| recipes | 1941 |
| loot_tables | 1187 |
| advancements | 651 |
| worldgen | 116 |
| damage_type | 56 |
| tags | 25 |

По типам рецептов: mold_casting 252, **assembler 239**, shredding 107, centrifuge 101,
pyro_oven 69, **chemical_plant 56**, arc_welder 53, crucible_smelting 39, cyclotron 37 и т.д.

После этого на сервере: `Loaded 3299 recipes` (было 1357) и **все 287 GameTest проходят**.

Побочно подтвердилась корректность п.1: сгенерированные `damage_type` совпали со статическими
один-в-один по набору файлов и семантике (отличается только порядок ключей). Оба набора оставлены —
статические работают страховкой, если датаген не прогоняли; `DuplicatesStrategy.EXCLUDE` снимает конфликт.

**Открытый вопрос для команды.** Раз `src/generated/resources/data/` в `.gitignore`, каждый
разработчик и каждая чистая сборка обязаны прогонять `runData` вручную, иначе мод едет без
рецептов и без damage types. Стоит либо коммитить генерат, либо вынести `runData` в
обязательный шаг сборки/CI.

### 4. `ammo_press`: `null` в `ingredients` ронял парсинг рецепта

```
Parsing error loading recipe hbm_m:ammo_press/tau_uranium
com.google.gson.JsonParseException: Failed to parse recipe:
  Cannot invoke "JsonElement.getAsJsonPrimitive()" because "input" is null
```

Датаген пишет пустые слоты сетки 3x3 как `null`. Затрагивало `tau_uranium` и `coil_tungsten` —
оба рецепта ammo press не грузились.

Настоящая причина оказалась глубже, чем «кодек не любит JsonNull». По стектрейсу:

```
NullPointerException at JsonOps.convertTo
  <- DynamicOps.convertList <- JsonOps.createList <- DynamicOps.convertMap
  <- PlatformRecipeSerializer.decode
```

Падало **до** вызова `readJson`, внутри `dynamic.convert(JsonOps.INSTANCE)`:
`PlatformRecipeSerializer.decode` прогонял через `Dynamic.convert` данные, которые **и так уже
`JsonElement`**, а `convertList` обходит массив поэлементно и получает Java `null` на месте
`JsonNull`.

**Исправление (два уровня).**

1. `PlatformRecipeSerializer.decode` больше не конвертирует, если `ops.createMap(...)` уже вернул
   `JsonElement` — лишний проход через DFU убран. Это общий фикс для **всех** кастомных рецептов мода,
   а не только ammo press.
2. `AmmoPressRecipe.Serializer.readJson` пропускает `null`-элементы, оставляя `Ingredient.EMPTY`
   (что и означает пустой слот).

Результат: `Loaded 3301 recipes` вместо 3299 — оба рецепта на месте.

### 5. Ссылки на `forge:` теги внутри JSON не переписывались в `c:`

```
Couldn't load tag hbm_m:non_occluding as it is missing following references:
    #forge:glass (from mod/hbm_m)
    #forge:glass_panes (from mod/hbm_m)
```

Конвертер в `build.neoforge.gradle.kts` переносил **файлы** тегов из `data/forge/` в `data/c/`,
но не переписывал **ссылки** на них внутри JSON. Масштаб: **265 файлов рецептов** и 151 уникальный
тег (`ingots/*` 34, `storage_blocks/*` 31, `nuggets/*` 31, `ores/*` 27, `powders/*` 18, `dyes/*` 5,
`wires_fine/lead`, `glass`, `glass_panes`).

Последствие тихое и потому неприятное: рецепт парсится без ошибки, но матчит пустой тег —
рецепт просто не крафтится. Ошибку в лог давал только `non_occluding`, потому что
тег-файл ссылается на тег жёстко.

**Уточнение по факту (проверено на собранных ресурсах).** Ремап чинит **181 файл из 272**.
Оставшийся **91 рецепт** ссылается на **72 тега, которых не существует нигде** — ни среди
сгенерированных модом, ни в `neoforge-21.1.248.jar` (там 245 конвенциональных тегов):

| категория | нет тегов |
|---|---|
| `c:nuggets/*` | 29 |
| `c:storage_blocks/*` | 27 |
| `c:ingots/*` | 10 |
| `c:ores/*` | 4 |
| `c:powders/*` | 2 |

Эти 91 рецепт были нерабочими и **до** ремапа (`forge:nuggets/*` на NeoForge не существует
подавно), так что регрессии нет — но и «починены все 265» сказать нельзя.
Причина в датагене: `ModItemTagProvider` эмитит `ingots/*`, `powders/*`, `wires_fine/lead`,
`storage_blocks/{uranium,plutonium}`, `ores/uranium` — генератора `nuggets/*` и общего
`storage_blocks/*` там нет вообще, поэтому `runData` это не исправит.

**Сделано частично: 72 → 47 недостающих тегов, 91 → 66 мёртвых рецептов.**

Причина оказалась в датагене: `ModItemTagProvider` перебирал `ModMaterials` и генерировал теги
для `INGOT`, `POWDER` и `POWDER_TINY`, но для `NUGGET` генерации не было вообще, а
`storage_blocks/*` перечислялись вручную — только `uranium` и `plutonium`. При этом обе формы
в `MaterialShape` есть: `NUGGET` у 51 материала, `BLOCK` у 48.

Добавлено:

- генерация `forge:nuggets/<материал>` в `ModItemTagProvider` по образцу ingots — **51 тег**;
- цикл по всем `storage_blocks/<материал>` в `ModBlockTagProvider` вместо двух ручных записей
  (`ModBlocks.URANIUM_BLOCK` — тот же объект из `INGOT_BLOCKS`, дублей не возникает) — **48 тегов**;
- соответствующий `copy` block→item для всех `storage_blocks/*`.

**Что осталось (47 тегов / 66 рецептов) — это уже не генерация, а содержание.**
Теги не создаются потому, что у материала нет нужной формы либо материала нет вовсе:

| причина | примеры |
|---|---|
| материала нет в `ModMaterials` | `nickel`, `mingrade` |
| материал есть, формы `NUGGET` нет | `copper`, `steel`, `titanium`, `tungsten`, `saturnite`, `cadmium` |
| материал есть, формы `BLOCK` нет | `arsenic`, `osmiridium`, `technetium` |
| нет генерации `ores/*` (кроме `uranium`) | `cobalt`, `lithium`, `niter`, `plutonium` |
| нет формы `POWDER` | `phosphorus`, `sulfur` |

Отдельный случай — **два разных материала с похожими именами**:
`ALUMINUM("aluminum")` имеет `INGOT`/`BLOCK`/`PLATE`/`POWDER`, а `ALUMINIUM("aluminium")` —
только `CRYSTAL`/`PLATE_CAST`/`PLATE_WELDED`/`WIRE`/`WIRE_DENSE`. Рецепты ссылаются на оба
написания (5 файлов на британское, 3 на американское), поэтому `c:ingots/aluminium`,
`c:nuggets/aluminium` и `c:storage_blocks/aluminium` пустые. Нужно решить, это намеренное
разделение или рецепты должны ссылаться на `aluminum`.

Закрывать остаток стоит осознанно: либо дописывать формы материалам, либо править рецепты —
это правки контента, а не инфраструктуры.

**Исправление.** В конвертер добавлен ремап ссылок: `"forge:` → `"c:` и `"#forge:` → `"#c:`
по всем JSON в `data/`, плюс словарь переименований конвенций (`glass` → `glass_blocks`,
так как в NeoForge 1.21 это `c:glass_blocks`). `"neoforge:add_features"` не задевается —
паттерн включает открывающую кавычку.

---

## B. Сверка с оригиналом 1.7.10

Источник правды по логике — [HbmMods/Hbm-s-Nuclear-Tech-GIT](https://github.com/HbmMods/Hbm-s-Nuclear-Tech-GIT)
(ветка `master`). Сверка разделила прежние подозрения на **регрессии порта** (чинить) и
**свойства оригинала** (не трогать).

| Пункт | Оригинал | Наш порт | Вывод |
|---|---|---|---|
| Доступ к радиации | `world.blockExists(x, 0, z)` перед записью, чтение из памяти | `level.getChunk(...)` — форс-загрузка | **регрессия**, исправлено |
| EMP `emp()` | `Compat.getTileStandard` — «without loading chunks» | `level.getBlockEntity(...)` без guard | **регрессия**, исправлено |
| Пакет взрыва | 3 signed byte на позицию, count `int`, без cap, радиус 250 | идентично | соответствует, не трогать |
| `empBlast` цикл | такой же тройной цикл по сфере | идентично | соответствует |
| `HTTPHandler` | `ArrayList` без синхронизации, без таймаутов, вызов в `PreLoad` **без side-check** | идентично | соответствует |

### B1. Радиация форсировала загрузку и генерацию чанков — ИСПРАВЛЕНО

Оригинал (`ChunkRadiationHandlerSimple`):

```java
public void setRadiation(World world, int x, int y, int z, float rad) {
    SimpleRadiationPerWorld radWorld = perWorld.get(world);
    if(radWorld != null) {
        if(world.blockExists(x, 0, z)) {              // <-- guard
            ChunkCoordIntPair coords = new ChunkCoordIntPair(x >> 4, z >> 4);
            radWorld.radiation.put(coords, MathHelper.clamp_float(rad, 0, maxRad));
```

Оригинал держит радиацию в собственной `Map` и при чтении **вообще не трогает чанк**;
на запись стоит `blockExists` — запись в невыгруженный чанк молча отбрасывается.
Наш порт хранит значение на самом чанке (attachment), и поэтому звал `level.getChunk(x >> 4, z >> 4)`,
который грузит чанк до статуса FULL, **генерируя его при необходимости**, в серверном потоке.

Бьёт это так: `ExplosionNukeGeneric.incrementRad` трогает 21 чанк за вызов, а `incrementRad` =
`getRadiation` + `setRadiation`, то есть до **42 форсированных загрузок**; вызывается в цикле аварии
реакторов (`MachineWatzPowerplantBlockEntity:279`, `PWRControllerBlockEntity:382`,
`MachineZirnoxBlockEntity:339`). Сервер встаёт на секунды — клиентов выбивает по таймауту.

**Исправление.** Оба метода перешли на неблокирующий `level.getChunkSource().getChunk(cx, cz, false)`
с ранним выходом при `null`, плюс восстановлен `Mth.clamp(rad, 0, MAX_RAD)`, который в оригинале
есть в обоих методах, а в порту потерялся.

### B2. EMP-взрыв дёргал чанки — ИСПРАВЛЕНО

Оригинал не зовёт `getTileEntity` напрямую, а идёт через хелпер:

```java
public static boolean isPositionLoaded(World world, int x, int z) {
    return world.getChunkProvider().chunkExists(x >> 4, z >> 4);
}
/** A standard implementation of safely grabbing a tile entity without loading chunks */
public static TileEntity getTileStandard(World world, int x, int y, int z) {
    if(!isPositionLoaded(world, x, z)) return null;
    return world.getTileEntity(x, y, z);
}
```

Наш `emp()` звал `level.getBlockEntity(new BlockPos(...))` без всякой защиты, а `empBlast`
вызывается с `bombStartStrength = 50` (`MissileTier0:96`) и обходит сферу ~35 блоков —
это ~180 000 обращений за один тик, каждое из которых могло тянуть чанк.

**Исправление.** Добавлен `com.hbm_m.util.Compat` — прямой порт оригинального хелпера
(`isPositionLoaded` + `getTileStandard`), `emp()` переведён на него.
Показательно, что `EmpPulseEntity.allocate()` в этом же проекте уже сделан правильно
(проверяет `hasChunk` и идёт по `chunk.getBlockEntities()`) — до `empBlast` подход просто не довели.

### B3. Пакет взрыва — соответствует оригиналу, чинить нечего

Раннее подозрение («переполнение байта» + «пейлоад > 1 МБ») **не подтвердилось как баг порта**.
Оригинальный пакет кодирует ровно так же: 3 signed byte на позицию (то есть то же ограничение
±127), count как `int`, без cap, рассылка в радиусе 250.

Практический риск на 1.21 тоже отсутствует: SFX-пакет шлют только `LandmineBlock` (3F/10F/25F) и
крипперы; крупные ракетные взрывы идут через `standardExplode` **без `setSFX`**, то есть пакет
не отправляют вовсе. Максимальный размер 25F даёт порядка сотни килобайт — до лимита 1 МБ далеко.

Оставить как есть. Риск появится, только если кто-то поставит SFX на взрыв радиусом больше ~60.

### B5. `HTTPHandler` — соответствует оригиналу

Оригинал тоже держит `capsule` / `tipOfTheDay` как обычные `ArrayList` без синхронизации,
тоже не ставит таймауты на `openStream()` и тоже зовёт `HTTPHandler.loadStats()` в `PreLoad`
**без side-check**, то есть и на выделенном сервере.

Технически замечания остаются в силе (гонка данных, поток-демон может висеть, HTTP с сервера),
но это не регрессия порта — менять стоит осознанно, как улучшение против оригинала.

---

## B*. Улучшения против оригинала (в 1.7.10 этого не было)

### B4. C2S-пакеты доверяют произвольному `BlockPos` от клиента

Из 24 C2S-пакетов с `level.getBlockEntity(msg.pos)` проверку дистанции имеют только пять:
`RBMKBoilerPacket`, `ServerboundDoorModelPacket`, `SetAssemblerRecipeC2SPacket`,
`SetChemFactoryRecipeC2SPacket`, `SetChemPlantRecipeC2SPacket`.

Остальные берут позицию как есть: `UpdateBatteryC2SPacket`, `ZirnoxControlPacket`,
`WatzControlPacket`, `PWRControlPacket`, `TurretControlPacket`, `FunnelModeC2SPacket`,
`MicrowaveSpeedC2SPacket`, `MiningDrillToggleC2SPacket`, `SolderingStationControlPacket`,
`SoyuzLauncherControlPacket`, `RadioTorchControlPacket`, `RBMKConsoleControlPacket`,
`RBMKControlPacket`, `ToggleWoodBurnerPacket`, `UpdateRadarC2SPacket`, `ItemDesignatorPacket`,
`BuildMissilePacket`, `FluidTankModePacket`, `AnnihilatorPoolC2SPacket`.

Последствия: клиент может заставить сервер грузить и генерировать чанки в произвольной точке мира.

**Исправлено.** В `ModPacketHandler` добавлен guard, применённый к 18 пакетам
(`ItemDesignatorPacket` не тронут — он берёт block entity из открытого меню, а не из позиции):

```java
public static boolean isPosUsable(ServerPlayer player, BlockPos pos) {
    if (pos == null) return false;
    Level level = player.level();
    return level.isInWorldBounds(pos) && level.isLoaded(pos);
}
```

**Проверки дистанции здесь намеренно нет.** В проекте закладывается совместимость с
Sable / Create Aeronautics, где блоки живут в contraption sub-level'ах и штатно управляются
на расстоянии от игрока (в `build.neoforge.gradle.kts` уже есть `compileOnly` на Sable, а в
`mixins.hbm_m.json` — `SubLevelMoveWindowMixin` / `SubLevelRelinkMixin` /
`SubLevelAssembleExpansionMixin`). Радиус-guard ломал бы такие симуляции.
Границы мира и загруженность чанка закрывают собственно серверную проблему —
генерацию terrain по запросу клиента — и при этом безопасны для sub-level'ов.

Проверка `player.containerMenu` тоже не добавлялась: часть пакетов (RBMK-консоль, радиоторч)
управляет блоками без открытого меню мода.

**Важная оговорка (по итогам ревью).** Обоснование «без дистанции из-за Sable» **не подтверждается
для этих 18 пакетов** — ни одна из этих машин в contraption не живёт, а единственный
contraption-aware C2S (`ServerboundDoorModelPacket`) как раз имеет жёсткий reach 8 блоков и не
трогался. Поэтому честная формулировка такая:

- `isPosUsable` закрывает **только** подгрузку/генерацию чанков с клиентской координаты;
- **проверки права доступа он не даёт и не заменяет.** Модифицированный клиент по-прежнему может
  управлять любой *загруженной* машиной в измерении: выключить чужие турели
  (`TurretControlPacket`), нажать АЗ-5 на чужом реакторе (`RBMKConsoleControlPacket`),
  дёрнуть `SoyuzLauncherControlPacket` / `BuildMissilePacket` / `WatzControlPacket`.

Дыра предсуществующая, но новый guard делает обработчики *похожими* на проверенные, чем маскирует
её — это стоит держать в голове. Канонический фикс — `player.containerMenu instanceof <нужное Menu>`
+ `stillValid`, как уже сделано в `AnvilCraftC2SPacket`. Не сделан здесь потому, что часть пакетов
управляет блоками без открытого меню и нужен разбор каждого случая.

**Отдельно на заметку по Sable/Aeronautics.** Пять пакетов, у которых проверка дистанции уже стояла
до ревью, как раз и есть потенциальная проблема для симуляций:

| пакет | лимит |
|---|---|
| `RBMKBoilerPacket` | `distanceToSqr > 400` (20 блоков) |
| `SetAssemblerRecipeC2SPacket` | `> 64` (8 блоков) |
| `SetChemPlantRecipeC2SPacket` | `> 64` (8 блоков) |
| `SetChemFactoryRecipeC2SPacket` | `> 64` (8 блоков) |
| `ServerboundDoorModelPacket` | по дистанции |

Если такая машина окажется на движущемся contraption'е в sub-level, координаты блока и позиция
игрока считаются в разных системах, и жёсткий радиус отклонит легитимный пакет.
Эти guard'ы я не трогал — менять их стоит вместе с решением, как мод будет адресовать
блоки внутри sub-level'ов.

Хорошая новость: **все** обработчики корректно используют `context.queue(...)`, логика не исполняется
в сетевом потоке — этот класс гонок закрыт.


## D. Багфиксы геймплея (сделано)

### D1. Dash силовой брони не работал вообще

`ModConfigKeybindHandler` по нажатию клавиши звал напрямую:

```java
// TODO: Отправить пакет на сервер для выполнения dash
PowerArmorHandlers.performDash(mc.player);
```

А сам метод начинается с

```java
public static void performDash(Player player) {
    if (!(player instanceof ServerPlayer serverPlayer)) return;
```

`mc.player` — это `LocalPlayer`, поэтому метод **молча выходил на первой строке**.
Рывок не срабатывал ни в одиночной игре, ни на сервере: энергия не тратилась, кулдаун не ставился,
импульс не применялся — но тост «dash perform» показывался всегда, создавая впечатление, что
способность сработала и просто «не докрутили баланс».

Показательно, что серверная половина функции полностью готова и корректна: проверяет броню,
энергию и кулдаун, применяет импульс, рассылает `PowerArmorDashPacket` (S2C) соседям и
синхронизирует энергию. Отсутствовало только звено «клиент → сервер»:
`POWER_ARMOR_DASH` был зарегистрирован **только как S2C**.

**Исправление.** Добавлен `PowerArmorDashC2SPacket` + ID `power_armor_dash_request`,
зарегистрирован через `registerC2S`. Клавиша теперь отправляет запрос, сервер выполняет
`performDash` и перепроверяет все условия у себя. Существующий S2C-пакет продолжает
синхронизировать импульс остальным игрокам — его роль не менялась.

## E. Ревью на деградации (проверка собственных правок)

Правки были отревьюены отдельно. Ниже — то, что нашлось **в моих же фиксах**, и что исправлено.

### Деградации, внесённые правками (исправлены)

1. **Dash стал хуже, чем был.** В `PowerArmorHandlers.performDash` не выставлялся
   `player.hurtMarked = true`. Движение игрока клиент-авторитетно: без флага `ServerEntity`
   не шлёт владельцу `ClientboundSetEntityMotionPacket`, и импульс затирается следующим
   пакетом движения. S2C-пакет не помогает — он явно пропускает локального игрока.
   Итог: до правки метод выходил на первой строке и не тратил ничего; после — списывал энергию
   и ставил кулдаун без эффекта. В этом же файле `hurtMarked` стоит в трёх других местах.
   **Исправлено.**

2. **`Compat` резолвил чанк дважды.** `hasChunk` (с аллокацией `ChunkPos`) + `getBlockEntity`
   резолвили один и тот же чанк. На пути `empBlast` это ~185k позиций, где чанки почти всегда
   загружены, то есть guard почти не экономил, а стоимость удваивал. Плюс `ClientLevel.hasChunk`
   безусловно возвращает `true`, так что на клиенте guard был бы фиктивным.
   **Исправлено:** один `getChunkNow`, плюс `isOutsideBuildHeight` и тот же
   `EntityCreationType.IMMEDIATE`, что у `Level#getBlockEntity`.

3. **Фикс GameTest ослаблял тест.** Газ не только рассеивается по броску, но и **уезжает вверх**
   (`BlockGasMeltdown.getFirstDirection` даёт `UP` в половине случаев, `BlockGasBase.tryMove`
   переносит блок). Первая версия фикса штамповала новый источник на месте, пока старый жив выше,
   и тест начинал мерять до трёх источников — порог становился тривиальным.
   **Исправлено:** пересев только когда пуста вся воздушная колонка (y=1..3).

4. **Неточные комментарии.** Формулировка «matching the original» в `getRadiation` неверна:
   в 1.7.10 чтение шло из in-memory map и данные не теряло, у нас выгруженный чанк читается как 0 —
   это осознанный размен, а не соответствие. **Переформулировано.**

### Найдено рядом и исправлено

- **OOM в `RBMKConsoleControlPacket.decode`**: `int len = buf.readInt(); int[] sel = new int[len];`
  без ограничения, на netty-треде до `context.queue` — крафтовый пакет с `len = Integer.MAX_VALUE`
  ронял сервер запросом гигабайтного массива. Заклампено по `AREA` (225).
- **`RBMKBoilerPacket`** брал block entity до проверки дистанции — единственный из семейства
  без guard. Закрыт.
- **RBMK-консоль** сканировала сетку 15×15 сырыми `getBlockEntity` по `reactorOrigin`, который
  задаёт клиент — ровно тот путь генерации чанков, ради которого делался `Compat`.
  Все 5 мест переведены на `Compat.getTileStandard`.
- **`MachineAnnihilatorBlockEntity`** писал радиацию мимо менеджера: без гейта конфига, без
  попадания в active-набор (нет распространения и затухания), без `setUnsaved`.
  Переведён на `ChunkRadiationManager.incrementRad`.
- **Теги damage_type** лежали только в gitignored `src/generated`. Фикс из п.1 раздела «Сделано»
  был половинчатым: типы урона грузились, а `bypasses_armor` / `is_explosion` / `is_projectile` /
  `is_fall` / `bypasses_invulnerability` — нет, из-за чего 29 типов переставали игнорировать броню
  (это читает и живой код: `EntityCreeperPhosgene`, `EntityMaskMan`).
  Пять файлов положены в `src/main/resources/data/minecraft/tags/damage_type/`.
- `isPosUsable` сведён к единому `Compat.isPositionLoaded`, отказы логируются под
  `-Dhbm_m.netDebugPackets=true`.

### Проверено и подтверждено корректным

- 56 `damage_type` совпадают с датагеном по именам и семантике (различается только порядок ключей);
  схема соответствует кодеку 1.21.1, `when_caused_by_living_non_player` — ровно дефолт
  двухаргументного `new DamageType(msgId, exhaustion)`.
- Все 55 `minecraft:*` id в тегах биомов сверены с `Biomes` 1.21.1 — расхождений больше нет.
- Все 18 вставок guard'а стоят в правильном месте и работают с тем же уровнем, из которого потом
  читается block entity; штатной работы с невыгруженными чанками у этих пакетов нет.
- Пустой пейлоад dash-пакета безопасен: Architectury кодирует его как length-prefixed `byte[0]`.
- `MAX_RAD` не может быть 0 или отрицательным — `ConfigSchema` клампит в `[1, 10^7]`.
- `empBlast` (включая эффективный радиус `r/√2` и асимметрию цикла) совпадает с оригиналом 1:1 —
  это не баг порта.
- Быстрый путь в `PlatformRecipeSerializer` срабатывает на 100 % штатных загрузок:
  `RecipeManager` использует `RegistryOps<JsonElement>`, чей `createMap` делегируется в `JsonOps`
  и всегда возвращает `JsonObject`. Fallback через `Dynamic.convert` остаётся для `NbtOps`.
- `Ingredient.EMPTY` матчит только пустой стек, поэтому пропущенный слот ammo press требует
  пустоты — рецепт лишнего не матчит.
- 1/350 в `BlockGasMeltdown` — верный порт оригинала, фикс теста ошибку порта не маскирует.
- Ремап `grass` → `short_grass` уже есть в конвертере (`build.neoforge.gradle.kts:315`),
  рецепт liquefactor в собранных ресурсах корректен.

### Открытые вопросы (нужно решение)

1. **Рукописные `damage_type` затеняют датаген.** `DuplicatesStrategy.EXCLUDE` берёт файл из
   `src/main/resources`, поэтому правка `DataGenerators` (например смена `exhaustion`) в сборку
   не попадёт, а *новый* ключ приедет из датагена — набор станет смесью двух источников.
   Либо считать `src/main/resources` источником истины и убрать `DAMAGE_TYPE` из датагена,
   либо наоборот — убрать рукописные файлы и сделать `runData` обязательным шагом CI.
2. ~~50 из 56 типов урона без перевода `death.attack.<id>`~~ — **сделано.** Добавлены в
   `ModLanguageProviderEn`; 45 формулировок взяты дословно из `assets/hbm/lang/en_US.lang`
   оригинала (там ключи в camelCase — `acidPlayer`, `nuclearBlast`, `subAtomic1`, — у нас
   snake_case по `message_id`). Пять типов, которых в оригинале нет (`blast`, `boltgun`,
   `enervation`, `nitan`, `vacuum`), дописаны в его стиле. Применяется через `runData`.
3. **Registry-контекст теряется** в `RecipeHooks`: `Ingredient.CODEC.parse(JsonOps.INSTANCE, ...)`
   вместо переданного `RegistryOps`. Сейчас безвредно, но любой рецепт с `components`,
   требующими динамического реестра (зачарования, зелья), упадёт с `Not a registry ops`.
4. **Хрупкость ремапа тегов**: `.replace("\"forge:", "\"c:")` применяется ко всем значениям
   в `data/`. Сегодня безопасно (проверено), но будущие `"forge:conditions"` / `"type": "forge:not"`
   от Forge-датагена молча превратятся в `c:`. Стоит ограничить позициями тегов или завести
   blacklist префиксов.

## F. Производительность сервера: 18 машин синхронизируются каждый тик

Найдено при глобальном разборе, **не исправлено** — нужно решение, потому что затрагивает
18 блок-сущностей и видимое поведение GUI.

Эти машины в своём `tick` безусловно вызывают `sendUpdateToClient()`:

`MachineAnnihilator`, `MachineBreeder`, `MachineCatalyticReformer`, `MachineCompressor`,
`MachineCrackingTower`, `MachineElectricFurnace`, `MachineElectrolyser`, `MachineExposureChamber`,
`MachineFlareStack`, `MachineFrackingTower`, `MachineFractionTower`, `MachineHydrotreater`,
`MachineLargePylon`, `MachineLiquefactor`, `MachineMicrowave`, `MachineRadiolysis`,
`MachineVacuumDistill`, `OilDrillBase`.

Цепочка: `sendUpdateToClient()` → `level.sendBlockUpdated(...)` → `ChunkSource.blockChanged(pos)` →
в конце тика `ChunkHolder.broadcastChanges` → `broadcastBlockEntityIfNeeded` →
`blockEntity.getUpdatePacket()` → `ClientboundBlockEntityDataPacket.create(this)` →
`getUpdateTag(...)` → **полный `writeNbtData`**: весь инвентарь, энергия, все танки.

Два следствия:

1. **CPU тратится даже без игроков рядом.** `broadcastBlockEntity` вызывает `getUpdatePacket()`
   до того, как проверит список получателей — то есть полный NBT сериализуется 20 раз в секунду
   на каждую такую машину независимо от того, смотрит ли кто-нибудь.
2. **Трафик.** У `MachineElectrolyser` это 10 слотов инвентаря и 4 танка; у
   `MachineCrackingTower` — 5 танков; у `MachineCatalyticReformer` — 4. На базе с десятками
   машин это десятки полных NBT-снимков в секунду каждому наблюдателю.

**Варианты решения (нужно выбрать):**

1. Слать только при фактическом изменении: держать в `BaseMachineBlockEntity` флаг
   «состояние поменялось» и дёргать `sendUpdateToClient()` из тика лишь когда он взведён.
   Самый правильный путь, но требует расставить пометки во всех местах, меняющих состояние.
2. Дросселировать: не чаще раза в N тиков (например 5). Дёшево и безопасно, но прогресс-бары
   в GUI станут обновляться реже.
3. Оставить как есть для машин с маленьким состоянием и починить только тяжёлые
   (электролизёр, крекинг-башня, реформер, фракционная башня).

Отдельно стоит проверить, не дублируется ли синхронизация: у части машин рядом с
`sendUpdateToClient()` в том же тике стоит ещё и `setChanged()`.

## G. Осталось после симплификации (не сделано осознанно)

Ревью качества показало, что chunk-safe доступ применён **поточечно, а не к механизму**.
Ниже — места того же класса, оставшиеся нетронутыми; фикс каждого выходит за границы правок,
но список стоит держать под рукой.

**Прямые близнецы уже исправленного:**

- `RBMKDisplayBlockEntity:106` — тот же скан `GRID×GRID` сырым `getBlockEntity` вокруг позиции
  из NBT, что и `MachineRbmkConsoleBlockEntity.scanReactor`, который переведён на `Compat`.
  Две копии одного скана, конвертирована одна.
- `RBMKNeutronHandler:49` — `blockPosToTE` это единственная точка входа для `:158`, `:198`,
  `:214`, `:291`. Одна строка закрывает четыре места; поток нейтронов уходит на `fluxRange`,
  поэтому реактор на границе чанков тянет соседа каждый тик.
- `EmpPulseEntity:75` — `allocate()` проверяет `hasChunk`, но `shock()` резолвит собранные позиции
  на более поздних тиках, когда край уже мог выгрузиться. Ровно тот случай, ради которого
  `Compat` и появился.

**Хуже — принудительная генерация, а не только загрузка:**

- `CraterBiomeApplier:80` и `WorldUtil:91` зовут `level.getChunk(cx, cz)` (load-and-generate)
  по всему боксу кратера, без guard, только try/catch.

**Позиции из NBT/линкеров без проверки:** `PylonBaseBlockEntity:100`, `MachineRadarBlockEntity:758`,
`ItemWiring:62`, `RBMKToolItem:74`, `ItemDroneLinker:54`.

**Широкие сканы взрывов** (`ShockwaveGenerator`, `BlastExplosionGenerator`, `CraterGenerator`,
`BlockProcessorStandard:96` и др.) полагаются на region-тикет `EntityExplosionChunkloading`,
который ограничен 12 чанками (MK3/Balefire/Solinium) и 31 (MK5) — радиус сверх этого кольца
уходит за тикет. Для них правильная гранулярность — **один `getChunkNow` на чанк снаружи
внутреннего цикла**, а не per-position guard.

Отдельно не сделано, потому что это отдельные задачи:

- слияние четырёх проходов `processResources` в один (сейчас каждый JSON в `data/` читается
  ~2.7 раза, дерево обходится 4 раза: 12.7k чтений на 4757 файлов);
- вынос повторяющихся stonecutter-веток `new ResourceLocation` / `fromNamespaceAndPath`
  в один хелпер `rl(ns, path)` — 14 одинаковых блоков в двух провайдерах датагена;
- `incrementRad` резолвит чанк дважды (`get` + `set`); один резолв требует смены контракта
  `ChunkRadiationHandler`;
- `ChunkRadiationAccess.get` возвращает `Optional`, который никогда не пуст (у атрибута есть
  дефолтный поставщик) — две лишние аллокации на каждое чтение радиации, то есть на каждую
  сущность каждый тик. Смена на nullable-аксессор меняет публичный API.

## C. Мелочи

- **Отладочный вывод в проде — УБРАНО.** `PlatformRecipeSerializer` печатал в `System.err`
  `[HBM DEBUG] decode CALLED/SUCCESS/FAILED` плюс `printStackTrace()` на загрузке каждого рецепта,
  и вёл два `static` счётчика, инкрементируемые без синхронизации (рецепты грузятся в пуле).
  Заменено на `MainRegistry.LOGGER.error("Failed to parse recipe", e)` — причина по-прежнему
  видна со стектрейсом, но через логгер: `RecipeManager` наружу отдаёт только `message`.
- **Нестабильный GameTest — ИСПРАВЛЕНО.** `gas_meltdownPumpsChunkRadiationUnderSky` падал примерно
  в трети прогонов с разбросом значений (`got 0.825`, `got 3.217` при ожидании `> baseline + 10`),
  причём на неизменном коде: соседние прогоны давали то падение, то `All 287 passed`.

  Причина в самом источнике, а не в радиации. `BlockGasMeltdown.tick`:

  ```java
  // 1/350 рассеивание
  if (random.nextInt(350) == 0) {
      level.removeBlock(pos, false);
      return;
  }
  ```

  За 160 тиков теста вероятность, что газ рассеется раньше времени, около 37 % — накачка
  прекращается, и тест меряет бросок кубика, а не механику.

  **Исправление.** Тест переведён с `thenExecuteAfter(160, ...)` на `thenExecuteFor(160, ...)`,
  который каждый тик восстанавливает блок газа, если тот рассеялся. Порог остался строгим.
- 9 warning'ов компиляции на deprecated NeoForge API, все помечены `for removal`:
  `Item#initializeClient` / `MobEffect#initializeClient` (`MissileItem`, `RangeDetonatorItem`,
  `RadawayEffect`, `TaintEffect`), `EventBusSubscriber.Bus` (`ModEntityEvents`,
  `ClientPowerArmorRenderNeoForge`), `NetworkManager.toPacket` (`RadiationGameTest`).
- `RuntimeDistCleaner: Attempted to load class net/minecraft/client/... for invalid dist
  DEDICATED_SERVER` — штатный шум dev-окружения. `mixins.hbm_m.json` разделён на секции правильно,
  клиентские миксины лежат в `client`. Не баг.
- `ChunkRadiationAccess` содержит в комментарии китайские иероглифы («AttachmentType取代 capabilities»).
