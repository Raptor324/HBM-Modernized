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

**Прямые близнецы уже исправленного — СДЕЛАНО:**

- `RBMKDisplayBlockEntity:106` — тот же скан `GRID×GRID` сырым `getBlockEntity` вокруг позиции
  из NBT, что и `MachineRbmkConsoleBlockEntity.scanReactor`. Переведён на `Compat`.
- `RBMKNeutronHandler:49` — `blockPosToTE` это единственная точка входа для `:158`, `:198`,
  `:214`, `:291`; поток нейтронов уходит на `fluxRange`, поэтому реактор на границе чанков тянул
  соседа каждый тик. Одна строка закрыла четыре места.
- `EmpPulseEntity` — `shock()` резолвил позиции, собранные `allocate()` на более раннем тике,
  когда край уже мог выгрузиться; переведён на `Compat`. Заодно `allocate()` больше не делает
  `hasChunk` + `getChunk` (двойной резолв), а берёт чанк один раз.

**Сознательно НЕ переведено — там загрузка разовая и оправданная.** `ItemWiring:62`,
`RBMKToolItem:74`, `ItemDroneLinker:54`, `MachineRadarBlockEntity:758`, `PylonBaseBlockEntity:100`
резолвят позицию из NBT **по действию игрока** (связывание двух блоков, команда радара), а не в
тике. Запрет загрузки там сломал бы сценарий «связал точку A, отошёл, кликнул B»: связь просто
перестала бы устанавливаться. Разовая подгрузка одного чанка по клику — приемлемая цена.

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

## H. Обход остальных пакетов

Проверенные подсистемы, где серьёзных проблем не нашлось, и мелкие наблюдения по ним.

### Чисто

- **Энергосеть** (`UniNodespace`, `PowerNet`, `EnergySubscriptions`): `update()` сети не создаёт
  и не уничтожает, так что CME в `updateNetworks()` невозможен; `REGISTRY` — Guava
  `MapMaker().weakKeys().weakValues()` с weakly-consistent итераторами; в `tickAll` уже стоит
  guard `!be.getLevel().isLoaded(...)` против синхронной загрузки чанков.
- **RBMK-нейтроны** (`NeutronNodeWorld`): dial-кеш перечитывается на каждый уровень отдельно,
  `WeakHashMap` по `Level` — утечки нет.
- **`StructureConnectionFixProcessor`**: drain-паттерн с `isLoaded` и лимитом попыток.
- **Меню/контейнеры** (139 файлов): `stillValid` реализован в базовых классах, крейты наследуют;
  `quickMoveStack`, возвращающие `ItemStack.EMPTY`, — намеренные заглушки, а не потеря предметов.
- **Параллельный взрыв** (`ExplosionNukeRayParallelized`): воркеры читают из `SubChunkSnapshot`,
  а не из `Level` — доступ к миру из фоновых потоков сделан корректно. Циклы `while (true)` —
  это CAS-retry (`ConcurrentBitSet`) и цикл, ограниченный `rayCount`.
- **Широкие сканы сущностей**: `EntityUFO.scanForTargets` (AABB 200×100×200) стоит на
  `scanCooldown = 50`; сканы `EntityBOTPrimeHead` — разовые, в обработчике смерти.
- **Worldgen**: фичи используют `setBlock(..., 3)` — тот же флаг, что и ванильный
  `Feature.setBlock`. `RedRoomGenerator` вызывается разово из `KeyholeBlock` по действию игрока.
- **Дебаг-пакеты радиации**: радиус 4 чанка, дельта-обновления, очистка кэша по выходу из зоны.
- **Кэш `MultiblockStructureHelper.RECENT_PLACEMENTS`**: самоочищается по времени при каждой записи.

### Наблюдения (не баги, на будущее)

- `ItemSimpleConsumable.use` не проверяет сторону сам, а полагается на то, что это сделает
  переданная лямбда. Сейчас все три действия (`RADAWAY`, `usePillHerbal`, `useSiox`) проверяют
  `isClientSide` внутри, так что двойного применения нет — но контракт держится на дисциплине,
  а не на классе. Проверку стоит поднять в сам `use`.
- `LadderClimbHandler.checkTouchingLadder` вызывает `level.getBlockEntity` для каждой позиции
  внутри хитбокса игрока (~12–18 штук) **каждый тик на каждого игрока**. Чанк там заведомо
  загружен, так что подгрузки не происходит, но при полном сервере это тысячи лукапов в секунду.
  Дешевле сначала отсеивать по `getBlockState`.

## I. Инспекция powerarmor / armormod

**Статус: I1–I4 исправлены, I5–I7 остаются.**

### I1. Дюп модификаций брони — ИСПРАВЛЕНО

**Исправление.** Правка записывается обратно через `PlatformHooks.remove` / `put` — тем же путём,
каким её пишет `applyMod`. Остальные пять мест в файле, читающие `getItemTag`, проверены: они
только читают.

`armormod/util/ArmorModificationHelper.java:152` — `removeMod` берёт тег через
`PlatformHooks.getItemTag`, который на 1.21.1 возвращает **копию** (`PlatformHooks.java:33`,
`data.copyTag()`), мутирует её и обратно не записывает. Снятие мода — тихий no-op.

Самоисправиться не может: `saveTableToArmor:317` для пустого слота таблицы подставляет
всё ещё сохранённый мод (`pryMod`).

Итог: игрок вынимает мод из стола — получает **и предмет, и броню, у которой мод остался**
(NBT, модификаторы атрибутов, множитель ёмкости). Повторяется неограниченно.

Проверка: надеть мод батареи, запомнить `getMaxCharge`, снять мод, `/data get entity @s Inventory` —
`hbm_armor_mods.mod_slot_8` на месте.

### I2. Система защиты силовой брони мертва на NeoForge — ИСПРАВЛЕНО

**Исправление.** Добавлена neoforge-ветка обработчиков: `LivingAttackEvent` заменён на
`LivingIncomingDamageEvent`, `LivingHurtEvent` — на `LivingDamageEvent.Pre`, `LivingFallEvent`
существует и там. Класс получил `(modid = ...)` (на NeoForge это по умолчанию
игровая шина). Логика та же: отражение снарядов на HIGHEST, пересчёт DT/DR на LOWEST, обнуление
падения для сетов с `hasHardLanding`.

**Осталось:** `DamageResistanceHandler.initArmorStats()` по-прежнему не регистрирует DNT.

`powerarmor/PowerArmorHandlers.java:82`, `:126`, `:403` — `onLivingAttack`, `onLivingHurt`
и `onLivingFall` целиком внутри `//? if forge {`. `register()` (`:68`) вешает только
`TickEvent.PLAYER_PRE/POST`. Ничто в проекте не зовёт `DamageResistanceHandler.calculateDamage`
или `shouldDeflectProjectile`, миксинов на `LivingEntity.hurt` нет.

Следствие на выпускаемой сборке: **нет DT против взрывов, нет DR против огня, нет отражения стрел
и нет иммунитета к падению**, хотя каждый комплект объявляет `drFall = 1.0`. При этом
`handleHardLanding` (`:253`) продолжает работать — игрок получает и AOE-удар о землю, и полный
урон от падения. Тултип рекламирует несуществующие резисты.

Рядом: `DamageResistanceHandler.initArmorStats()` регистрирует T-51, AJR и Bismuth, но **не DNT**.

### I3. Запасная броня разряжается в рюкзаке — ИСПРАВЛЕНО

**Исправление.** В `inventoryTick` добавлены обе проверки, которые есть у соседних
переопределений: `world.isClientSide()` и `stackIsEquippedArmor` (метод стал `protected`).

`powerarmor/ModArmorFSBPowered.java:172-177` — `inventoryTick` не проверяет ни
`world.isClientSide()`, ни то, что предмет надет (у соседних переопределений обе проверки есть).
Любая заряженная запасная часть в инвентаре расходует энергию (DNT — 115 EU/тик), пока игрок носит
любой комплект; клиент параллельно переписывает `CUSTOM_DATA` своей копии каждый тик.

### I4. Пассивные эффекты на обеих сторонах, 4 раза за тик — ИСПРАВЛЕНО

**Исправление.** Эффекты вешаются только на серверной стороне и только на проходе по нагруднику
(`stack == chest`), плюс добавлен anti-flicker по длительности. Глобальный guard тут не годится:
звук шагов в том же методе намеренно клиентский.

`powerarmor/ModArmorFSB.java:307-308` — `tickFsbArmor` без `isClientSide`-guard, выполняется
на каждую надетую часть. Каждый `addEffect` сбрасывает длительность, поэтому сервер шлёт
`ClientboundUpdateMobEffectPacket`: для DNT-комплекта это **8 пакетов эффектов на игрока в тик**.
Правильный anti-flicker guard есть в `ModPowerArmorItem.applyPassiveEffects:311`, но
неохраняемый путь перебивает его.

### I5. Чтение заряда деп-копирует NBT 5–7 раз на предмет за тик

`ModArmorFSBPowered.java:122-124` — `getMaxCharge` → `getBatteryCapacityMultiplier` → `pryMod` →
`getItemTag` (полная `copyTag()` вместе с сериализованными модами) + `itemStackOf`
(полный `ItemStack.parseOptional`). `tickPoweredDrain` зовёт эту цепочку несколько раз за тик,
×4 части, ×каждый игрок. Побочно: NBT заряда меняется каждый тик, поэтому
`AbstractContainerMenu.broadcastChanges` пересылает все четыре слота брони целиком каждый тик.

### I6. Флаг hard-landing пишется на сервере, читается на клиенте

Пишется `PowerArmorHandlers.java:370`/`:395`, читается `PowerArmorSounds.java:226`, а
`player.getPersistentData()` между сторонами не синхронизируется. Клиент всегда видит `false`,
поэтому поверх звука удара о землю играет обычный звук приземления.

### I7. Мелочи

- `armormod/menu/ArmorSidePanelSlot.java:42-52` — звук экипировки играет и на клиенте, и на сервере
  (broadcast без «кроме игрока»), плюс срабатывает на `initializeContents` при открытии стола:
  до четырёх звуков просто за открытие GUI.
- `ModPowerArmorItem.java:377` пишет тик как `putInt`, а читает `getLong` (`:371`); после
  `gameTime > Integer.MAX_VALUE` кэш гейгера отключится навсегда. Там же
  `Inventory.contains(getDefaultInstance())` сравнивает компоненты — гейгер с любым NBT не найдётся.
- `armormod/client/ArmorModificationClientEvents.init()` и `ModTooltipHandler.init()` зовутся только
  из forge-веток: на NeoForge тултипы модов брони и резистов не отображаются вовсе.
- Мёртвый код: `ModArmorFSB.steppy/handleJump/handleFall` без вызывающих
  (в `handleFall:270` есть `entity.hurt` без side-check — безвредно только потому, что мёртв).

## J. Инспекция hazard-системы (найдено, НЕ исправлено)

### J1. Полный обход инвентаря каждый тик без гейта (offhand — ИСПРАВЛЕН)

`event/PlayerHazardHandler.java:34` — `TickEvent.PLAYER_POST` без интервала: 36 слотов + 4 брони
на игрока **каждый тик** (сравни `radiation/PlayerHandler.java:193`, где гейт `< 20` есть).
Это 800 вызовов `getHazardsFromStack` в секунду на игрока.

Там же **функциональный баг**: `inventory.offhand` не сканируется вовсе — радиоактивный предмет
в левой руке не облучает. При этом гейгер его учитывает (`radiation/PlayerHandler.java:250`
включает `getOffhandItem`), то есть счётчик показывает дозу, которую сервер не применяет.

### J2. Главная горячая точка: O(инвентарь²) + аллокации

`hazard/type/HazardTypeRadiation.java:36`

```java
reacher = player.getInventory().contains(new ItemStack(ModItems.REACHER.get()));
```

`Inventory.contains(ItemStack)` обходит все 41 слот, а `new ItemStack(ItemLike)` аллоцирует два
объекта. И это выполняется **на каждый опасный стек**, внутри цикла из J1, хотя результат для
всех итераций одинаков — это свойство игрока, вычисляемое заново 40 раз.

При полном инвентаре урана: ~33 000 сравнений и 1 600 мусорных объектов в секунду на игрока.

### J3. Обход всех сущностей измерения каждый тик

`event/HazardEventHandler.java:23` — `level.getAllEntities()` без гейта, чтобы найти `ItemEntity`.
На 4 000 сущностей в трёх измерениях это 240 000 итераций в секунду.

### J4. `RBMKRodItem.readTag` мутировал стек при чтении — ИСПРАВЛЕНО

`item/rbmk/RBMKRodItem.java:285-296` — при отсутствии компонента метод **записывает**
`CUSTOM_DATA` в стек. Вызывается из чисто читающего пути хазардов
(`HazardModifierRBMKHot`, `HazardModifierRBMKRadiation`). Следствия: сервер пишет NBT в предмет
просто оттого, что он лежит в инвентаре (лишний слот-синк и пересохранение), а на клиенте то же
происходит **при отрисовке тултипа** — клиентская копия расходится с серверной. Плюс три полные
deep-копии NBT на стержень за тик.

### J5. `HAZARD_CACHE` никогда не инвалидируется

`hazard/HazardSystem.java:43` — кэш `Item → List<HazardEntry>` без хука на перезагрузку датапаков,
хотя правила вешаются на **datapack-driven** теги (`HazardRegistry.java:323`, `forge:ingots/uranium`).
После `/reload` предмет сохраняет старую опасность до перезапуска сервера. Кэш вдобавок отдаёт
свой изменяемый `ArrayList`, а `HazardTransformerBase.transformPost` спроектирован дописывать
в этот список — сейчас трансформеры мертвы, но при их подключении это станет утечкой.

### J6. Гонка на `HashMap` между потоками — ИСПРАВЛЕНО

`radiation/PlayerHandler.java:44` — `playerRads` / `tickCounters` это обычные `HashMap`.
Пишет серверный поток (до 40 раз за тик на игрока через `ContaminationUtil.contaminate`),
а читает **клиентский рендер-поток**: `particle/helper/ParticleEffectClient.java:46`.
В одиночной игре это классический рецепт зависания в `HashMap.getNode`.

Побочно: на выделенном сервере клиент никогда не заполняет `playerRads` (реальное значение живёт
в `OverlayGeiger.clientPlayerRadiation`), поэтому аура радиации выше 600 RAD в мультиплеере
не рендерится вообще.

## K. Инспекция block / blockentity (найдено, НЕ исправлено)

**Общая тема раздела: логика, живущая только в `//? if forge {`.** Тот же класс проблем, что
с системой защиты брони (раздел I2). На NeoForge эти ветки не выполняются, neoforge-аналога нет.

**Статус: K1, K2, K3, K4 — ИСПРАВЛЕНЫ.** `onLoad` / `onChunkUnloaded` существуют и на NeoForge
(`IBlockEntityExtension`), поэтому хватило расширить условие до `//? if forge || neoforge {`
в `FluidDuctBlockEntity`, `FluidValveBlockEntity`, `FluidExhaustBlockEntity` и
`UniversalMachinePartBlockEntity`.

Для K2 расширения условия было **недостаточно**: `IFluidHandler` в forge-ветке — это
`net.minecraftforge.fluids.capability.IFluidHandler`, на NeoForge пакет другой. Правильный путь
нашёлся в самом базовом классе: `BaseMachineBlockEntity.getFluidHandler` (`:626-638`) отдаёт
`NeoForgeFluidHandlerMK2`, **если блок реализует `IFluidUserMK2`** — именно так устроены машины,
у которых жидкости работают. Поэтому все семь подключены к MK2-сети, а не продублированы под
вторую платформу:

| машина | интерфейс | приём | отдача |
|---|---|---|---|
| `MachineSteamTurbine` | Transceiver | пар | отработанный пар |
| `MachineFrackingTower` | Transceiver | фраксол | нефть, газ |
| `MachineArcFurnace` | Sender | — | tank1, tank2 |
| `MachineCoreInjector` | Receiver | дейтерий, тритий | — |
| `MachineCombinationOven` | Receiver | tank | — |
| `MachineMiningDrill` | Receiver | кислота | — |
| `MachineOreSlopper` | Receiver | вода | — |

Роли танков определены по фактическому использованию в коде (`fillMb` — отдача, `drainMb` — приём),
а не по названиям.

### K1. Жидкостные клапаны не восстанавливают узел сети после перезагрузки

`blockentity/machines/FluidValveBlockEntity.java:149-155` — `FluidNode` создаётся только в
`onLoad()` (`//? if forge`) и `setLevel()` (`//? if fabric`, `:157-163`). Клапан к тому же не
тикает: `FluidValveBlock.java:44-48` возвращает `null` из `getTicker`.

`ensureNode` зовётся лишь из `updateRedstone` (`:88`, причём с ранним выходом `if (newOpen == open) return;`)
и `setFluidType`. Итог: **после рестарта сервера открытый клапан не создаёт узел, и трубопровод
остаётся разорванным**, пока кто-нибудь не дёрнет редстоун.

### K2. Семь машин без fluid-капабилити на NeoForge

Привязка обработчика жидкости заключена в `//? if forge {`, а класс не реализует `IFluidUserMK2`,
поэтому `getFluidHandler` вернёт `null` и `ModCapabilities.java:73-77` зарегистрирует null-провайдер.
Ни одна из семи не участвует и в MK2-сети:

| машина | строка |
|---|---|
| `MachineSteamTurbineBlockEntity` | `:199-203` — **паровая турбина не принимает пар из труб** |
| `MachineFrackingTowerBlockEntity` | `:520-525` |
| `MachineCoreInjectorBlockEntity` | `:50-59` |
| `MachineArcFurnaceBlockEntity` | `:73-79` |
| `MachineCombinationOvenBlockEntity` | `:66-71` |
| `MachineMiningDrillBlockEntity` | `:118-123` |
| `MachineOreSlopperBlockEntity` | `:70-75` |

Работает только ручная заливка канистрами там, где есть слоты.

### K3. `onChunkUnloaded` мёртв — узлы и энергоподписки не освобождаются

`FluidDuctBlockEntity:215-225`, `FluidValveBlockEntity:172-178`, `FluidExhaustBlockEntity:117-130`,
`UniversalMachinePartBlockEntity:590-597` (там же `EnergySubscriptions.unsubscribeAll`).
Все — `//? if forge {`. Что метод доступен на NeoForge, видно по соседнему `onLoad` без обёртки
(`MachineFluidTankBlockEntity:169-176`).

Следствие: при выгрузке чанка узлы остаются в неймспейсе со ссылкой на удалённый BE, подписки
не снимаются — рост памяти и работа сети «через выгруженные чанки» на долгоживущем сервере.

### K4. Прочее из того же класса

- `UniversalMachinePartBlockEntity.onLoad:573-588` — уведомление соседей при загрузке потеряно,
  соединения труб вокруг мультиблока не пересчитываются до первого блок-апдейта.
- `FluidDuctBlockEntity.onLoad:168-193` — пересчёт соединений при загрузке мёртв (узел спасает
  `tick`, состояние соединений — нет).

### K5. Сканы без проверки загрузки — ИСПРАВЛЕНО

- `MachineHephaestusBlockEntity:83-88` → `:107-109` — `level.getFluidState` по окну 15×15
  (`SCAN_RANGE = 7`) **каждый тик**, без `isLoaded`; окно гарантированно пересекает границы чанков.
  4500 обращений/сек на машину, на краю симуляции — синхронная генерация. Корректный образец
  рядом: `MachineSolarBoilerBlockEntity:256`.
- `OilDrillBaseBlockEntity:127-139`, `:146-148` — два прохода по колонне с `getBlockState` по
  четырём сторонам, на границе чанка тянет соседа.
- `MachineAutosawBlockEntity:107` и flood fill в `fell()` — радиус 15 без единой проверки.

### K6. Мелочи — ИСПРАВЛЕНО

- `RBMKColumnBlockEntity:232` — глобальный флаг `dropLids = false` без `try/finally`,
  восстановление только линейным путём на `:338`. Исключение в мелтдауне оставит флаг сброшенным
  до перезапуска процесса — крышки RBMK перестанут дропаться. **Исправлено** (`try/finally`).
- `blockentity/network/radio/RTTYNetwork.java` — из `BROADCAST` записи никогда не удалялись,
  а карты и `lastProcessedTick` статические. В одиночной игре канал из предыдущего мира
  доживал до следующего, и слушатели (`RadioTorchReceiver`, `RadioTorchController`, `RadioTelex`)
  сравнивали его `timeStamp` с `gameTime` нового мира и срабатывали на чужой сигнал.
  **Исправлено:** `onLevelUnload` чистит каналы измерения, `onServerStop` — всё; оба повешены
  в `MainRegistry` рядом с `UniNodespace`.

**Проверено и чисто:** симметрия NBT по всем 573 файлам; мутации `ItemStack`/DataComponents на
пути чтения (только два места, оба на пути дропа); `entityInside`/`randomTick` газов и фоллаута
корректно закрыты `isClientSide`.

## L. Инспекция inventory

### L1. Дюп ×2 при shift-клике в теплообменнике — ИСПРАВЛЕНО

`inventory/menu/MachineHeatexMenu.java` — меню состоит **только** из 36 слотов инвентаря игрока
(машинных слотов у теплообменника нет), а `quickMoveStack` звал
`moveItemStackTo(slotStack, 0, this.slots.size(), true)`, то есть диапазон включал сам слот-источник.
При слиянии `moveItemStackTo` сравнивает стек сам с собой: `j = count + count`, затем
`stack.setCount(0)` и `itemstack.setCount(j)` — над одним и тем же объектом. **Стек удваивался
при каждом shift-клике.**

Исправлено разделением на «основной инвентарь ↔ хотбар», как в ванильных меню.

### L2. Устаревший снимок в трёх RBMK-меню — ИСПРАВЛЕНО

`RBMKOutgasserMenu`, `RBMKStorageMenu`, `RBMKAutoloaderMenu` строят собственный `SimpleContainer`
один раз в конструкторе и в `setChanged()` пишут снимок целиком обратно в блок. Блок при этом
продолжает тикать и менять те же поля, а ре-синка не было (он есть только в `RBMKRodMenu:85-94`).

Итог: подержать GUI открытым до конца обработки и кликнуть по любому слоту —
**израсходованный вход возвращался (дюп), готовый выход затирался (потеря)**.

Исправлено добавлением `broadcastChanges()` с ре-синком по образцу `RBMKRodMenu`; заодно в
`RBMKOutgasserMenu` добавлены null-проверки `be`, которых там не хватало (`RBMKAutoloaderMenu`
и `RBMKRodMenu` их имеют).

### L3. Битый shift-click ID-слота в пяти баках — ИСПРАВЛЕНО

`BarrelIronMenu:167`, `BarrelSteelMenu:167`, `Bat9000Menu:168`, `FluidTankMenu:166`,
`MachineFluidTankMenu:176` целились в `MACHINE_SLOTS + 0 .. +1`, но `MACHINE_SLOTS` равен
`PLAYER_INVENTORY_START` — то есть в первый слот игрока, а не в ID-слот машины. Соседние ветки
в тех же методах используют константы блока корректно, так что это опечатка.

Дюпа не давало только потому, что `FluidIdentifierItem` имеет `stacksTo(1)`; со стакающимся
идентификатором это стало бы дюпом класса L1. Исправлено на диапазон `[0, 1)`.

### L4. Кнопки кранов и null-контракт меню — ИСПРАВЛЕНО

- **Кнопки кранов не шли на сервер.** Шесть действий в пяти экранах (`GUIMachineCraneExtractor`,
  `GUIMachineCraneGrabber`, `GUIMachineCraneBoxer`, `GUIMachineCraneInserter`,
  `GUIMachineCraneRouter`) звали мутаторы на **клиентском** BE: режим фильтра, whitelist,
  maxEject, режим упаковщика, destroyer инсертера и режим стороны роутера никогда не доходили
  до сервера — фильтрация кранов в мультиплеере не работала вовсе.
  **Исправлено:** добавлен `network/CraneControlPacket` (pos + action + index, индекс валидируется
  по размеру), зарегистрирован как `CRANE_CONTROL`; экраны шлют пакет, а мутаторы BE после
  `setChanged()` зовут `sendUpdateToClient()`, иначе клиент не увидел бы результат.
- **NPE-риски в меню.** `RBMKStorageMenu.getBlockEntity` бросал `IllegalStateException` даже на
  клиенте, а конструктор разыменовывал `be` без проверок — приведено к контракту соседних
  RBMK-меню (null на клиенте, исключение на сервере) с guard'ами в конструкторе и `stillValid`.
  Заодно закрыт NPE в `mayPlace` у `RBMKOutgasserMenu:51` и `RBMKAutoloaderMenu:65`: остальной
  null-protection там был, а эти две строки его пропустили.

### L5. Найдено, НЕ исправлено (нужен тест / решение)

- **Побочный эффект в `Slot.set` сбрасывает режимы фильтров.** `MachineCraneExtractorMenu:46-49`
  и ещё четыре меню зовут `initPattern`, который безусловно ставит `MODE_EXACT`. При открытии GUI
  `initializeContents` дёргает `set` для каждого слота — все девять фильтров сбрасываются в EXACT
  на клиенте. Оригинал 1.7.10 вёл себя так же, а после L4 состояние приходит с сервера следующим
  `sendUpdateToClient`; нужно проверить в игре, виден ли сброс, прежде чем менять контракт слота.
- **Латентный дюп жидкости.** `inventory/fluid/tank/FluidLoaderFillableItem.java:25` мутирует
  массив из `pryMods` и не пишет обратно (метода записи вообще нет) — при сливе бак наполняется,
  а мод остаётся полным. Сейчас не стреляет: модов брони с баком не существует.
- **Фиктивный BE в `AnvilMenu:40`.** При отсутствии BE подставляется новый на `BlockPos.ZERO`,
  и `stillValid` проверяет блок в (0,0,0). На сервере путь недостижим (BE приходит из
  `createMenu`), на клиенте это тот же fallback «реплей Flashback», что и null в других меню.
  Менять — только вместе с общим решением по этому fallback.
- **`DamageResistanceHandler.initArmorStats()` не регистрирует DNT.** Зарегистрированы T-51, AJR
  и Bismuth; для DNT нужны значения из оригинала — это порт контента, не опечатка.

**Проверено и чисто:** все 134 `quickMoveStack` (кроме L1) не теряют и не дублируют предметы;
размеры инвентарей BE и меню совпадают; `FluidTank` клампит fill и корректно защищает мутации;
клиентские классы вне `gui/` есть только в `FluidTank`, но все помечены `@OnlyIn(Dist.CLIENT)`.

## C. Мелочи

- **Нестабильный GameTest `flowSwitchCutsPowerMidRun` — НЕ ИСПРАВЛЕНО.** `EnergyNetworkGameTest:537`
  падает изредка на самом первом шаге (`thenWaitUntil`: `Power flows through the closed switch
  (got 0)`) при `timeoutTicks = 400`, то есть энергия не дошла до печи за 400 тиков, хотя обычно
  доходит за единицы. Повтор того же коммита проходит 287/287. Значит, момент, когда узел печи
  подхватывается сетью, недетерминирован — вероятный источник: порядок обхода `HashMap` в
  `UniNodespace`/`FluidNetProvider`. Нужен прогон в цикле, чтобы поймать закономерность.

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

## M. Инспекция api/energy, api/network, handler

### M1. Реестр подписок самоуничтожался при первой же сборке мусора — ИСПРАВЛЕНО

`api/energy/EnergySubscriptions.java:41` — `MapMaker().weakKeys().weakValues()`. `BackoffState`
нигде, кроме самой карты, сильной ссылкой не держится, поэтому запись слабо достижима **с момента
создания**, и первый же GC вычищает весь реестр, хотя все ключи-BlockEntity живы. После этого
`tickAll` не итерирует ничего.

Восстановить запись может только `update()` через `computeIfAbsent`, а его зовут лишь машины, чей
собственный тикер дергает `ensureNetworkInitialized()`. **Из 115 наследников
`BaseMachineBlockEntity` таких нет у 71** (`MachineArcFurnace`, `MachineCompressor`,
`MachineElectrolyser`, `MachinePress`, `MachineTurbofan`, все `MachineCrane*`/`MachineDrone*`,
`PWRControllerBlockEntity`, `MachineDieselGenerator`, `MachineIndustrialGenerator`, электропечь).
Такая машина после GC навсегда выпадает из драйвера подписок и перестаёт получать энергию.

Это же объясняет флейк `flowSwitchCutsPowerMidRun` (см. C): печь подписывается только к ~20-му
тику (первая попытка проваливается — узлы проводов ещё без сети, `receiverDelay = DELAY_NOTHING`),
и если в это окно попадает young GC, энергия не приходит до конца таймаута. Батарея не страдает,
потому что переregистрируется каждый тик через собственный `ensureNetworkInitialized()`.

Исправлено удалением `.weakValues()`. Семантика, описанная в комментарии рядом (выгрузка чанка/GC
сами убирают запись), обеспечивается одним `weakKeys()`.

### M2. Найдено, НЕ исправлено (нужен тест / решение)

- **`PowerNet.java:74,87` — `energyUsed` не сбрасывается между уровнями приоритета.** Объявлен вне
  цикла по приоритетам, а `toTransfer -= energyUsed` выполняется на каждом уровне, поэтому со
  второго уровня вычитается накопленная сумма. Питание 1000, спрос 400/300/300 → NORMAL получает 0
  при 300 свободных. Возможно, это калька с `PowerNetMK2` 1.7.10 — нужна сверка с оригиналом
  перед правкой, потому что затрагивает распределение мощности во всех сетях.
- **`NeutronNodeWorld.java:12` — `WeakHashMap<Level, StreamWorld>` не работает.** Значение сильно
  ссылается на ключ (`StreamWorld.nodeCache` → `RBMKNeutronNode.tile` → `BlockEntity.level`),
  классический self-reference-leak. `removeAllWorlds()` не вызывается ниоткуда: в `MainRegistry`
  нет хука на `SERVER_LEVEL_UNLOAD`/`SERVER_STOPPED` для `NeutronNodeWorld`, в отличие от
  `UniNodespace` и `FluidNetProvider`. Итог: `ServerLevel` со всеми чанками живёт до конца процесса.
  Правится тем же приёмом, что RTTY (K6), но нужно понять, что делать с `nodeCache`.
- **`RBMKNeutronHandler.java:305-311` — `getHits` форсирует загрузку чанков каждый тик.**
  `level.getBlockState` идёт в `getChunk(create = true)`; вызывается из `runStreamInteraction:223,230`
  именно в ветке, куда попадаем, когда `blockPosToTE` вернул null, то есть когда чанк не загружен.
  Гард `Compat.getTileStandard` строкой выше обходится.
- **`RBMKNeutronHandler.java:292` — проверяется `pos` вместо `posAfter`.** Узел для `pos` создаётся
  в начале того же метода, поэтому условие почти всегда false и ветка мертва.
- **`SwitchBlockEntity` проводит по всем шести граням.** Не переопределяет `PowerConductor.createNode`,
  а `UniNodespace.checkConnection` не консультируется с `canConnectEnergy`. Рубильник, стоящий
  поперёк, сшивает две независимые линии в одну сеть. В оригинале `CableSwitch` переопределяет
  `createNode` двумя направлениями.
- **`EnergySubscriptions.java:132` — смена режима буферной батареи сносит всю сеть.** В любом
  небуферном режиме `update()` каждый тик зовёт `Nodespace.destroyNode`, а `popNode` делает
  `node.net.destroy()`. Батарея под редстоун-клоком с `modeOnSignal = 0` держит грид в перманентном
  браунауте.
- **Недетерминированная итерация `activeNodeNets` (`UniNodespace.java:69`).** `HashSet<NodeNet>` без
  `hashCode` → порядок по identity hash. Машина между двумя независимыми сетями получает питание
  от того генератора, чья сеть обошлась первой.
- **Мелочи:** `PowerNet.java:33-34` ранние `return` до очистки протухших записей (ретенция памяти);
  `PowerNet.java:102-112` «козёл отпущения» для округления выбирается из `HashMap` (расхождение в
  единицы); `PowerNet.java:129-139` `sendPowerDiode` без `isBadLink` и без гарда `rec > 0`
  (NaN при нулевом спросе, вызывается только из GameTest); `HTTPHandler.java:29-30` статические
  `ArrayList` мутируются из демон-потока без синхронизации (читателей пока нет);
  `LongEnergyWrapper.java:37` `getLow` возвращает знаковый `int` (недостижимо: максимум 200 млн).

## N. Инспекция entity / explosion

### N1. Исправлено

- **Обломки RBMK не исчезали никогда.** `RBMKDebrisEntity:93` переопределяет `tick()` и не зовёт
  `super.tick()`/`baseTick()` — единственное место, где растёт `tickCount`, который читает проверка
  на строке 103. С выключенным дайлом perma-scrap обломки всё равно жили вечно.
- **Слой фоллаута не клался на нормальный рельеф.** `EntityFalloutRain:470` — `return` вместо
  `break` внутри цикла по колонке, из-за чего пропускался `tryPlaceFalloutLayer` в конце метода.
  `depth` доходит до 3 в любой обычной колонке (трава-земля-камень), то есть слой не появлялся
  практически нигде.
- **Утечка форс-чанков на каждом грузовом запуске «Союза».** `SoyuzEntity:162` брал
  `TicketType.FORCED` и не отпускал его никогда: N запусков = N навсегда загруженных регионов 5×5,
  причём `FORCED`-билеты не видны в `/forceload query`. Билет передан капсуле (собственный
  `TicketType` с UUID, как в `MissileBaseEntity`) и снимается в `remove()`.
- **Курсор очистки жидкостей в кратере мог не двигаться.** `NukeMk5ChunkEater:524` — `break outer`
  минует инкремент `fluidClearCursorX`, а внутренний цикл по z начинался заново. Колонка, не
  влезающая в `processTimeMs`, пересканировалась вечно: `fluidsCleared` не выставлялся,
  `releaseAllTickets()` не вызывался. Добавлен курсор по z (с сохранением в NBT).
- **Граната IF читала `EntityDataAccessor` чужого класса.** `GrenadeIfProjectileEntity` читала
  `GrenadeProjectileEntity.GRENADE_TYPE_ID`, не определённый на этой сущности; исключение глушил
  `catch (Exception)`, поэтому тип гранаты никогда не синхронизировался и клиент всегда рисовал
  обычную IF. Заведён собственный accessor, он же выставляется в конструкторе и при чтении NBT.
- **Туман наносил урон в нулевом объёме.** `EntityMist:117` — бокс уже шириной `width`, а
  `inflate(-width/2, 0, -width/2)` сжимал его по width/2 с каждой стороны, схлопывая в плоскость
  через центр: облако задевало только того, кто стоит ровно на оси.
- **Несбалансированные push/pop профайлера.** `BlockProcessorStandard:63` — `push` внутри
  `canDropFromExplosion`, `pop` снаружи; любой блок без дропа от взрыва (ТНТ) выталкивал
  вышестоящую секцию тика.
- **`EntityProcessorCross` терял центральный узел видимости.** Массив на 7 элементов заполнялся
  циклом `i < 7` через `Direction.from3DDataValue`, который заворачивается по модулю 6: узел 6 был
  вторым DOWN, а центра взрыва не было вовсе. Сущность с прямой видимостью на эпицентр получала
  density 0, если все шесть смещённых узлов оказывались в блоках. Используется всеми боеголовками.
- **`BlockMutatorBulkie:24` проверял не тот стейт.** Гард смотрел на поле `blockState` (замену), а
  не на параметр `state` (заменяемый блок), то есть был инвариантен: листва, стекло и трава во
  внешней оболочке взрыва тоже превращались в целевой блок.
- **Первый импульс радиации MK5 не срабатывал.** `EntityNukeExplosionMK5:118` требовал
  `explosion != null`, а движок создаётся ниже в том же тике — на `tickCount == 1` он ещё null, и
  самая большая доза рампы пропускалась.
- **`EntityMaskMan:223`** — `nextInt(len - 1)` даёт шаг 0, из-за чего фаза лазера повторялась в
  половине случаев, вопреки комментарию строкой выше.
- **`CustomNukeExplosion`** — `FAT_MAN_CORE` клался в карту дважды (первая запись мертва), и ядерная
  ветка передавала `yPos + 5` вместо `yPos + 0.5`, как все остальные ветки.

### N2. Найдено, НЕ исправлено (нужен тест / решение)

- **`EntityProcessorStandard:59` сравнивает квадрат расстояния с линейным радиусом.**
  `entity.distanceToSqr(...) / size` вместо `Math.sqrt(...) / size`, то есть эффективный радиус
  становится `sqrt(size)`. Плюс `knockback` для не-живых равен `density` без множителя
  `(1 - distanceScaled)`, поэтому урон вообще не спадает с расстоянием. Живые пользователи:
  `EntityCreeperGold:67`, `EntityCreeperVolatile:68`. Правка меняет баланс урона крипёров —
  сверить с оригиналом и проверить в игре.
- **`ExplosionChaos.cluster:83-93` путает градусы и радианы.** `yawDeg = yawRad * 180/π`, затем
  `Mth.sin(yawDeg * π/180)` — конверсии взаимно уничтожаются, а вызывающие (`MissileTier1:130`,
  `MissileTier2:125`, `MissileTier3:135`) передают градусы. Разлёт кассетных суббоеприпасов не
  связан с курсом ракеты.
- **`RagingVortexEntity:22-26`** — условие таймера инвертировано (`<= 20` вместо `>= 20`, счётчик
  уходит в минус без границы) и `π/20` применён к результату `sin`, а не к аргументу.
- **`ExplosionBalefire:27-41`** — `surface` перезаписывается на каждом не-воздушном блоке при спуске
  вниз, поэтому огненный след кладётся в самой нижней найденной точке. В 1.20+ рельефе deepslate и
  гравий не `Blocks.STONE`, так что «след на уровне земли» оказывается глубоко под землёй.
- **`EntityCreeperNuclear:96-99`** — живая ветка `>= 1.21.1` дропает только ТНТ; мёртвая ветка
  `< 1.21.1` дропает ещё `COIN_CREEPER` и выдаёт достижение `BOSS_CREEPER`. Оба символа существуют.
- **Нестабильные идентификаторы в NBT.** `MissileABMEntity:301` сохраняет `tracking.getId()` —
  посессионный счётчик, при загрузке резолвится в произвольную сущность (та же схема в
  `EntityWormBase:275/282`, там безопаснее). `EntityMist:166` сохраняет
  `BuiltInRegistries.FLUID.getId(...)` — числовой id зависит от порядка регистрации, после
  установки/удаления мода туман становится другой жидкостью.
- **Самолёты авиаудара.** `AirstrikeEntity:178` (и три его варианта) зовут `playAmbientSound()`
  каждый тик без интервала — 20 звуковых пакетов в секунду на громкости 6. Плюс `direction` и
  `hasFinishedAttack` не пишутся в `addAdditionalSaveData`, так что после перезагрузки самолёт с
  недобомблённым боезапасом летит вечно.
- **`TomBlastEntity`** не переопределяет `getChunkLoadRadius()` (по умолчанию 3 чанка = 48 блоков),
  тогда как `EntityNukeExplosionMK3`, `EntityBalefireExplosion` и `EntityNukeExplosionMK5` это
  делают, а `TomEntity:28` задаёт `DESTRUCTION_RANGE = 600` и `ExplosionTom.breakColumn` работает
  без проверки загрузки.
- **Сканы взрывов без проверки загрузки:** `ExplosionFleija.breakColumn:97`, `ExplosionSolinium:34`,
  `ExplosionTom:119-150`, `ExplosionBalefire:27-41`, `SpearEntity.descentBlast:115-156`,
  `EntityNukeExplosionMK5.radiate:233`, `EntityUFO.groundBelow:259`. Все — в тиковых циклах, число
  итераций растёт каждый тик.
- **`SynchedEntityData` пишется на клиенте:** `EntityCreeperTainted:44`, `EntityCreeperNuclear:79`
  (`heal` в `tick()`), `SpearEntity:97,101`, `VortexEntity:34`, `RagingVortexEntity:37`.
  Эффект косметический, но сторона объективно не та.
- **`EntityDroneBase`** — `targetX/Y/Z` не синхронизированы, а флаг `HAS_TARGET` синхронизирован, и
  `tick()` двигает дрона на обеих сторонах: клиент гонит его к (0,0,0) между пакетами позиции.
- **`AirNukeBombProjectileEntity:189-191`** — три гарда проверяют `EXPLOSION_LARGE_NEAR.isPresent()`,
  добавляя `BOMBDET1/2/3`; при отсутствии звука будет `nextInt(0)`. Метод сейчас не вызывается.
- **`MissileTier3:51-52` и `MissileTier4:49`** — смещения сопел для инверсионного следа используют
  `-thrust.z` там, где нужен `thrust.y` (визуальное, только клиент).
- **`DroneChunkLoader:26`** отпускает билет только из `EntityDeliveryDrone.remove()`, а путь
  `setRemoved(UNLOADED_TO_CHUNK)` через `remove()` не идёт.
