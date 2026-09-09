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

**ИСПРАВЛЕНО — но не тогда, когда я отчитался.** В коммите `1fe200f31` директива была расширена до
`//? if forge || neoforge`, однако Stonecutter не переобрабатывает файлы, пока не сменить версию:
тело осталось закомментированным, и метод всё это время не компилировался. Правка подействовала
только после круга `stonecutterSwitchTo1.20.1-forge` → `stonecutterSwitchTo1.21.1-neoforge`.
Подробности и правило — в разделе S.

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

- **`PowerNet.java:74,87` — СВЕРЕНО С 1.7.10, НЕ БАГ ПОРТА.** `energyUsed` объявлен вне цикла по
  приоритетам, и `toTransfer -= energyUsed` вычитает накопленную сумму на каждом уровне, из-за чего
  при питании 1000 и спросе 400/300/300 нижний уровень получает 0 при 300 свободных. В оригинале
  `api/hbm/energymk2/PowerNetMK2.update()` — ровно то же самое, дословно. Это поведение самого HBM;
  отклоняться от него — отдельное решение по геймплею, а не исправление регрессии порта.
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
- **`CustomNukeExplosion`** — `FAT_MAN_CORE` клался в карту дважды, первая запись мертва; убрана.
  (Правка `yPos + 5` → `yPos + 0.5`, отчитанная здесь ранее, оказалась регрессией и **откачена** —
  см. раздел V.)

### N1a. Исправлено после сверки с 1.7.10

- **`EntityProcessorStandard` — потерянный `sqrt`.** Оригинал считает
  `double distanceScaled = entity.getDistance(x, y, z) / size;` — линейное расстояние. Порт делил
  `distanceToSqr(...)` на радиус, то есть эффективный радиус схлопывался до `sqrt(size)` (у золотого
  крипёра 7 → 2.65 блока). Исправлено. Там же не хватало множителя спада `(1 - distanceScaled)`
  в `knockback` — он возвращён, но с первого раза **неправильно**, разбор в разделе V.
- **`EntityProcessorCross` — центральный узел.** Подтверждено: в оригинале цикл тоже `i < 7`, но
  `ForgeDirection.getOrientation(6)` — это `UNKNOWN` со смещением (0,0,0), то есть центр взрыва.
  На 1.21.1 `Direction.from3DDataValue` заворачивается по модулю 6 и давала второй DOWN. Правка
  восстанавливает оригинальное поведение точно.
- **Кассетные ракеты — ОШИБОЧНАЯ ПРАВКА, ОТКАЧЕНА.** Здесь ранее утверждалось, что контракт
  `ExplosionChaos.cluster` — радианы, и вызовы были переведены на `Math.toRadians`. Проверка
  оригинального `EntityMissileCluster` показала обратное: он передаёт `rotationYaw`/`rotationPitch`
  в **градусах** и разброс в радианах, то есть порт совпадал с оригиналом, а правка его ломала
  (плюс знак pitch выходил неверным). Возвращено как было, разбор в разделе V.
- **`EntityMist` — `inflate` вместо `offset`.** В оригинале
  `aabb.offset(-width/2, 0, -width/2)` — это перенос углового бокса в центр. Порт строит бокс уже
  центрированным и применял `inflate`, то есть сжатие: объём схлопывался в плоскость.
- **`EntityCreeperNuclear`** — ветка `>= 1.21.1` дропала только ТНТ, а ветка `< 1.21.1` дропает ещё
  `COIN_CREEPER` и выдаёт `BOSS_CREEPER`. Комментарий «после порта соответствующих систем» устарел:
  оба символа давно существуют. Ветки приведены к паритету.
- **Самолёты авиаудара шумели каждый тик.** `playAmbientSound()` вызывался безусловно — 20 звуковых
  пакетов в секунду на громкости 6 у всех четырёх вариантов. В оригинале (`EntityBomber.onUpdate`)
  все звуки закрыты гардом `ticksExisted % bombRate == 0`. Добавлен `AMBIENT_SOUND_INTERVAL = 60`
  — значение подобрать в игре по длине сэмпла. Заодно `direction` и `hasFinishedAttack` теперь
  пишутся в NBT: без них перезагруженный самолёт с недобомблённым боезапасом имел
  `direction = ZERO`, скалярное произведение никогда не уходило в минус и он летел вечно.

### N2. Найдено, НЕ исправлено (нужен тест / решение)

- **Нестабильные идентификаторы в NBT.** `MissileABMEntity:301` сохраняет `tracking.getId()` —
  посессионный счётчик, при загрузке резолвится в произвольную сущность (та же схема в
  `EntityWormBase:275/282`, там безопаснее). `EntityMist:166` сохраняет
  `BuiltInRegistries.FLUID.getId(...)` — числовой id зависит от порядка регистрации, после
  установки/удаления мода туман становится другой жидкостью.
- **`RagingVortexEntity:22-26` — СВЕРЕНО С 1.7.10, БАГ ОРИГИНАЛА.** `if(timer <= 20) timer -= 20;`
  и `Math.sin(timer) * Math.PI / 20D` — дословная копия `EntityRagingVortex.onUpdate`. Счётчик
  уходит в минус без границы, а `π/20` применён к результату `sin`, а не к аргументу, но ровно так
  же в оригинале. Отклоняться — решение по геймплею.
- **`ExplosionBalefire:27-41` — вероятно, намеренно.** `surface` перезаписывается на каждом
  стёртом блоке при спуске сверху вниз, то есть остаётся самая нижняя позиция. Похоже, так и надо:
  цикл стирает всё в воздух, поэтому нижняя стёртая точка — это дно кратера, единственное место,
  где огонь ляжет на твёрдое. Отдельный вопрос — что в 1.20+ рельефе deepslate и гравий не
  `Blocks.STONE`, поэтому в слэйтед-селлафилд превращается меньше блоков, чем в оригинале.
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
- **`MissileTier3:51-52` и `MissileTier4:49`** — `-thrust.z` в слоте `y` у части сопел. **Сверено:
  в оригинале ровно так же, в обоих тирах.** Не баг порта; правка, сделанная было в этом месте,
  откачена (раздел V).
- **`DroneChunkLoader:26`** отпускает билет только из `EntityDeliveryDrone.remove()`, а путь
  `setRemoved(UNLOADED_TO_CHUNK)` через `remove()` не идёт.

## O. Дочистка по N2/M2 со сверкой с 1.7.10

### O1. Исправлено

- **`NeutronNodeWorld` держал `ServerLevel` до конца процесса.** Порт заменил
  `HashMap<World, StreamWorld>` оригинала на `WeakHashMap`, но значение достаёт ключ обратно через
  `StreamWorld.nodeCache` → `RBMKNeutronNode.tile` → `BlockEntity.level`, поэтому запись никогда не
  собиралась, а `removeAllWorlds()` не вызывался ниоткуда. Возвращён `HashMap` (как в оригинале) плюс
  явная очистка из `MainRegistry`: `removeWorld` на `SERVER_LEVEL_UNLOAD`, `removeAllWorlds` на
  `SERVER_STOPPED` — рядом с `UniNodespace` и RTTY.
- **`RBMKNeutronHandler.irradiateFromFlux(Level, BlockPos)` грузил чанки ради no-op.** `getHits`
  проходит `columnHeight` блоков через `Level.getBlockState`, то есть синхронно подгружает чанк из
  тикового потока, а доза после этого всё равно отбрасывается: `ChunkRadiationHandlerSimple.setRadiation`
  выходит на незагруженном чанке. Добавлен `level.isLoaded(pos)`.
- **Дрон двигался на обеих сторонах.** В оригинале весь блок наведения лежит в `else`-ветке
  `if(worldObj.isRemote)`, клиент только интерполирует. В порте `targetX/Y/Z` — обычные поля, они не
  синхронизируются, а флаг `HAS_TARGET` синхронизируется: клиент видел «цель есть» с целью в (0,0,0)
  и гнал дрона к началу координат между пакетами позиции. Движение и `loadNeighboringChunks`
  закрыты `!level().isClientSide`.
- **`TomBlastEntity` не переопределял `getChunkLoadRadius()`.** Соседи с той же базой и тем же полем
  `destructionRange` (`EntityNukeExplosionMK3`, `EntitySoliniumExplosion`, `EntityBalefireExplosion`)
  переопределяют, и javadoc самого класса говорит «Shape mirrors EntityNukeExplosionMK3». Добавлено
  по их образцу.
- **`AirNukeBombProjectileEntity.playDetonationSound`** — все три гарда проверяли
  `EXPLOSION_LARGE_NEAR` вместо собственного звука, из-за чего при его отсутствии список оставался
  пустым и `nextInt(0)` бросал. Каждая строка проверяет свой звук, добавлен выход на пустом списке.
- **`EntityMist` сохранял числовой id жидкости.** Для синхронизации это нормально (сессия одна), но
  не на диске: id зависит от порядка регистрации, и после установки/удаления мода облако становилось
  другой жидкостью. Пишется ключ реестра, старый числовой ключ читается как fallback.
- **`MissileABMEntity` сохранял `tracking.getId()`** — посессионный сетевой счётчик. Заменено на UUID
  через `ServerLevel.getEntity(UUID)`.
- **Сопла инверсионного следа — ОШИБОЧНАЯ ПРАВКА, ОТКАЧЕНА.** `-thrust.z` в слоте `y` есть и в
  оригинале, в обоих тирах; «выравнивание кольца» было отклонением от него, а не восстановлением.
- **`PowerNet`** — `energyUsed` вне цикла по приоритетам: `api/hbm/energymk2/PowerNetMK2.update()`
  дословно такой же.
- **`RagingVortexEntity`** — `if(timer <= 20) timer -= 20;` и `sin(timer) * π/20` дословно из
  `EntityRagingVortex.onUpdate`.
- **`SwitchBlockEntity` не переопределяет `createNode`** — `TileEntityCableSwitch` тоже не
  переопределяет, наследует шесть направлений от `TileEntityCableBaseNT`.
- **`RBMKNeutronHandler:292` проверяет `pos` вместо `posAfter`** — в оригинале ровно так же
  (`if(NeutronNodeWorld.getNode(worldObj, pos) == null)` при лукапе по `posAfter`).
- **`EntityCreeperTainted`/`EntityCreeperNuclear` лечатся на обеих сторонах** — в оригинале
  `onUpdate` тоже без `!worldObj.isRemote`.
- **`UniNodespace.activeNodeNets` — `HashSet`** — в оригинале тоже `HashSet<NodeNet>`, порядок
  обхода недетерминирован там же.
- **`EnergySubscriptions:132`** — `Nodespace.destroyNode` это no-op при отсутствии узла
  (`UniNodespace.destroyNode` проверяет `node != null`), поэтому батарея в небуферном режиме сеть
  не сносит; `net.destroy()` срабатывает один раз на реальную смену топологии.
- **`ExplosionBalefire`** — «нижняя» позиция для огня, похоже, намеренна: колонка стирается сверху
  вниз, поэтому нижняя стёртая точка и есть дно кратера.
- **Освобождение чанк-билетов через `remove(RemovalReason)`** — путь выгрузки чанка идёт через
  `setRemoved`, а не `remove`, но так сделано во всех восьми классах проекта, и активный радиусный
  билет сам держит чанк сущности загруженным. Менять конвенцию — отдельное решение.

### O3. Осталось в очереди

- Сканы взрывов без проверки загрузки: `ExplosionFleija.breakColumn:97`, `ExplosionSolinium:34`,
  `ExplosionTom:119-150`, `SpearEntity.descentBlast:115-156`, `EntityNukeExplosionMK5.radiate:233`,
  `EntityUFO.groundBelow:259`.
- `RBMKNeutronHandler:223` — второй вызов `getHits`, там результат влияет на затухание потока,
  поэтому гард меняет поведение: нужно решить, чем считать незагруженную колонку.
- `EntityWormBase:275/282` — тот же посессионный id, что был в ABM.
- `SpearEntity:97,101`, `VortexEntity:34` — запись `SynchedEntityData` на клиенте.
- `EntityProcessorStandard`/`EntityProcessorCross` после правок математики — проверить урон крипёров
  и боеголовок в игре.

## P. Прогон на реальном сервере (192.168.1.20, 209 модов, NeoForge 21.1.248)

Мод собран (`hbm_m-0.2.2-alpha+1.21.1-neoforge.jar`) и положен в `mods/` тестовой сборки trewa
рядом с 208 другими модами (Create, Sable, AE2, Ad Astra, Alex's Caves, Iron's Spellbooks,
Terralith, Quark, KubeJS, Dynamic Trees и т.д.).

**Результат старта: чисто.** `Done (3.594s)`, мод определился и загрузился, `commonSetup finished`,
миксин `LevelChunkSilentRemovalMixin` применён, биомы `hbm_m:crater/inner_crater/outer_crater`
зарегистрированы, наши фичи попали в генерацию (46 фич в overworld). Ни одного `ERROR` от `hbm_m`.
263 ошибки `LootDataType` и 23 `RecipeManager` в логе — чужие (`railways` и другие моды), к нам
отношения не имеют. Лагов (`Can't keep up`) нет.

### P1. Найдено на сервере и исправлено

- **`HTTPHandler.loadSoyuz`/`loadTips` уходили в сеть без таймаутов.** Оба звали
  `url.openStream()` напрямую, минуя собственный `readResponse` с
  `setConnectTimeout`/`setReadTimeout`, поэтому недоступный gist вешал поток проверки версий до
  таймаута ОС — на сервере это вылезло как `SocketTimeoutException: Read timed out` посреди
  старта. Плюс `in.close()` стоял только на успешном пути, то есть при исключении соединение
  утекало. Добавлен `readLines(URL)` с теми же настройками, что у `readResponse`, и
  try-with-resources.
- **Списки `capsule`/`tipOfTheDay` стали `volatile` и заменяются целиком.** Их пишет демон-поток
  `NTM-Version-Checker`, а читать предполагается из игрового; прежний `add()` в общий `ArrayList`
  ещё и дублировал записи при повторном вызове `loadStats`.
- **Сообщение об ошибке было ложным** — второй `catch` тоже писал «Version checker failed!», хотя
  падали загрузчики союза и подсказок.
- **Убрана отладочная диагностика рецептов.** `ModRecipes.debugRecipeSerializerRegistry()`
  вызывалась из `commonSetup` на каждом старте, печатала четыре строки `[HBM DEBUG]` в `STDERR` и
  парсила фиктивный JSON-рецепт. Остаток от диагностики, парная к уже убранной из
  `PlatformRecipeSerializer`.

### P2. Найдено на сервере, НЕ баг

- **`@Mixin target ... was not found` для шести клиентских миксинов.** Все шесть лежат в секции
  `client` файла `mixins.hbm_m.json`, конфиг разделён правильно; Mixin на выделенном сервере всё
  равно проходит по ним и пишет WARN. Некритичный шум, поведение не ломает.
- **`Reference map 'mixins.hbm_m.refmap.json' could not be read`** — штатно для сборки без
  обфускации.
- **`quark:stoneling/toretoise ... added under MONSTER for hbm_m:crater`** — в наших биомах списки
  спавна пустые (`spawners` во всех трёх json), мобов туда добавляет биом-модификатор Quark.

## Q. Зависание старта — причина найдена, она не в моде

Сервер повис на 24 минуты в фазе `constructMod`. Дамп потоков (`jstack`, `hbm_m` в нём не
встречается ни разу):

```
AdAstraNeoForge.<init> → AdAstra.init → StationLoader.init (ad_astra 1.16.24)
  → WebUtils.getJson → HttpClient.send → CompletableFuture.get   ← без таймаута
```

Цепочка целиком:
1. Ad Astra на этапе конструирования мода синхронно тянет `https://adastra.terrarium.earth/stations`
   в потоке `modloading-worker-0`.
2. Тот отвечает `307` на `https://raw.githubusercontent.com/terrarium-earth/Ad-Astra/1.20.x/stations.json`.
3. DNS контейнера отдаёт для этого хоста **только AAAA**, а наружу IPv6 не ходит: адрес ULA
   (`fdba:17c8:6c94::/64`) с дефолтным маршрутом в никуда. IPv4 работает через раз — в тесте
   прошла 1 попытка из 3.
4. `java.net.http.HttpClient.send()` вызван без таймаута → одна неудачная попытка вешает старт
   навсегда. Отсюда «иногда зависает».

**JVM-флаги это не лечат.** В `user_jvm_args.txt` уже стоят
`-Dsun.net.client.defaultConnectTimeout/ReadTimeout` и `-Djava.net.preferIPv4Stack=true`, но первые
два действуют только на легаси `URLConnection`, а не на `java.net.http.HttpClient`; глобального
системного свойства для таймаутов нового клиента не существует.

**РЕШЕНО (обе причины закрыты).** Подробности — в разделе W. Кратко: IPv6 выключен в сети Wings,
MTU VPN-туннеля поднят владельцем сервера. После этого хост и контейнер дают 10/10 и 20/20 на
HTTPS-запросах, а сервер стартует с первой попытки.

Для контраста: наш сетевой запрос живёт на демон-потоке и (после P1) с таймаутами, то есть
заблокировать старт не может в принципе. Это подтвердилось на живом сервере: при флапающей сети
загрузчик списков падал по таймауту с корректным сообщением, а старт доходил до `Done` за 3.4 с.
## R. Проверено и закрыто как «не однозначная ошибка»

- **Сканы взрывов без проверки загрузки** (`ExplosionFleija.breakColumn`, `ExplosionSolinium`,
  `ExplosionTom`). `level.setBlock` подгружает чанк синхронно, но радиус тикета сознательно
  ограничен `Math.min(12, ...)`, то есть автор уже принял, что крупные кратеры выходят за него.
  Что делать с колонкой вне тикета — дорисовывать кратер (текущее поведение) или пропускать — это
  решение по геймплею, а не описка. Более новый движок MK5 (`NukeMk5ChunkEater`) выбрал второе:
  `blockAt` возвращает null на незагруженном чанке.
- **`SpearEntity.descentBlast` → `groundHeight` → `ServerLevel.getHeight`.** Гарда нет, но и
  прецедента в репозитории нет: `BossSpawnHandler.spawnMaskMan:124` делает точно так же
  (`getHeight` по гауссову смещению ×20 без проверки). Утверждение прошлой инспекции, что там
  стоит `hasChunk`, **неверно** — проверено по коду.
- **`EntityWormBase` и посессионный id.** Утверждение прошлой инспекции, что `headID` пишется в NBT
  на строках 275/282, **неверно**: поле вообще не сериализуется (в файле только объявление,
  геттер, сеттер и `getHead()`). После перезагрузки сегменты теряют голову — это реальный дефект,
  но чинится он не заменой ключа, а решением, как сегменты заново находят голову.
- **Логи на русском** в девяти файлах (`CreateDoorRegistrar`, `HbmConfigStore`, `CraterGenerator`
  и др.) — авторские строки, к правилу «комментарии в коде на английском» отношения не имеют.

## S. Ловушка Stonecutter: правка директивы без переобработки — молчаливый no-op

**Что произошло.** В коммите `1fe200f31` шесть директив были расширены с `//? if forge {` до
`//? if forge || neoforge {`, чтобы оживить логику жидкостной сети на NeoForge. Условие стало
истинным, но **Stonecutter переписывает `src/main` только при смене версии** — тела остались
закомментированными (`/*...*///?}`), то есть код не компилировался, и правка не делала ничего:

- `FluidValveBlockEntity.onLoad` / `onChunkUnloaded`
- `FluidDuctBlockEntity.onLoad` / `onChunkUnloaded`
- `FluidExhaustBlockEntity.onChunkUnloaded`
- `UniversalMachinePartBlockEntity.onLoad` / `onChunkUnloaded`

`onLoad` клапана — это K1, отчитанный как исправленный на четыре коммита раньше, чем он реально
заработал.

**Как ловить.** Скан: строка вида `//? if <условие, истинное на активной версии> {`, а следующая
непустая строка начинается с `/*`. Ложные срабатывания дают javadoc-блоки (`/**`) — их надо
отсеивать. На момент проверки нашлось 6 настоящих случаев и 2 ложных
(`ForgeFluidHandlerAdapter:349`, `ModArmorMaterialsAccess:39`).

**Как чинить.** `stonecutterSwitchTo<активная версия>` не помогает — Gradle отвечает `UP-TO-DATE`.
Нужен круг: `stonecutterSwitchTo1.20.1-forge` → `stonecutterSwitchTo1.21.1-neoforge`. После него
переобрабатываются ровно затронутые файлы.

**Второй слой.** Расширять условие можно только после проверки всего блока: в
`UniversalMachinePartBlockEntity` под той же директивой лежал forge-only
`getCapability(Capability, LazyOptional)`, которого в NeoForge 1.21.1 нет — после оживления
компиляция упала. Такие куски выносятся в отдельный `//? if forge {`.

Оба правила добавлены в `CLAUDE.md` (пункты 5 и 6 раздела про Stonecutter).

## T. Инспекция item / armormod / worldgen / world / util / config

### T1. Исправлено

- **Установка мода брони обнуляла саму броню.** `ArmorModificationHelper:392` (ветка 1.21.1)
  начинала сборку атрибутов с `armorStack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, EMPTY)`,
  но у обычного `ArmorItem` этого компонента нет — броня и стойкость приходят из дефолтов предмета,
  а ваниль и NeoForge откатываются к дефолтам **только пока список компонента пуст**. Записав туда
  одни лишь модификаторы модов, код превращал нагрудник в «+10 сердец и 0 брони». Ветка 1.20.1 того
  же метода (`:348-358`) дефолты возвращает явно — на 1.21.1 этот шаг просто не портировали.
- **Вся броня мода была небьющейся.** В 1.21.1 `ArmorItem` больше не берёт прочность из материала
  (ванильные предметы задают её через `Item.Properties.durability`), а все 43 регистрации шли с
  голым `new Item.Properties()`; `ModArmorMaterials.getDurabilityForType` остался только в мёртвой
  ветке 1.20.1. Добавлены `ModArmorMaterials.durabilityFor(Type)` и
  `ModArmorMaterialsAccess.armorProps(...)` (с обеими ветками), регистрации переписаны.
- **`ItemAssemblyTemplate` звал клиентский метод с сервера.** `PlatformHooks.clientProvider()`
  разыменовывает `Minecraft.getInstance()`, а `getRecipeOutput`/`writeRecipeOutput` вызываются из
  `MachineAssemblerBlockEntity.serverTick` (через `getRecipeFromTemplate`) и из обработчика
  `GiveTemplateC2SPacket`. Переведено на `bestEffortProvider()`, который для этого и заведён.
  Тултип `CrateItem` рядом действительно клиентский — оставлен как есть.
- **`ModAxeItem` терял урон и скорость атаки.** Соседние `ModShovelItem`/`ModPickaxeItem`/
  `ModSwordItem` передают `properties.attributes(...)`, топор — нет, поэтому параметры конструктора
  молча выбрасывались.
- **`ColorUtil`** — таблица ключуется именами из оригинала (`lightblue`, от OreDict `dyeLightBlue`),
  а поиск шёл по `DyeColor.getName()` → `light_blue`/`light_gray`. Два красителя давали 0.
  Сверено с оригиналом: таблица верна, ошибка в формате ключа.
- **`CrateItem.makeGroupingKey`** — `keyTag.remove("Count")` на 1.21.1 no-op: `ItemStack.CODEC`
  пишет `count`. Одинаковые предметы разного размера стака шли отдельными строками тултипа.
  Строка лежит **вне** версионных веток, а ключ пишется по-разному, поэтому чистятся оба написания
  (первая версия правки чинила 1.21.1 и ломала 1.20.1 — см. раздел V).
- **`BedrockOilOreFeature:41`** — `pos.getY() <= getMinBuildHeight()` при `baseY = getMinBuildHeight()`
  и `dy` с нуля отсекал весь нижний слой октаэдра — самый широкий и единственный с бедроком.
  Сверено с оригиналом `MapGenBedrockOil` (`y = 0..4` включительно).
- **`ShockwaveGenerator:118`** — `BlockTags.LEAVES` в общей цепочке `else if` делал ветку зоны 4
  недостижимой: листва за `ZONE_3` всегда сносилась вместо 40 % снос / 12 % поджог.
- **`BlastExplosionGenerator.noise`** — `random.setSeed(...)` вызывался на переданном
  `RandomSource`, а это `level.random`: после каждой воронки C4 RNG всего мира оставался засеян
  последней ячейкой шума. Вынесено в собственный генератор.
- **`CraterBiomeHelper:207`** — пакеты перестроенных чанков слались всем игрокам сервера без
  проверки измерения; рядом `WorldUtil.flushChunk` фильтрует и по измерению, и по дальности.
- **`StructureConnectionFixProcessor`** — статическая `PENDING` по измерениям не чистилась;
  добавлены `onLevelUnload`/`onServerStop` рядом с `UniNodespace`, RTTY и `NeutronNodeWorld`.

### T2. Сверено с 1.7.10 — НЕ баги порта

- `ItemDosimeter:278` — `if (x >= 1F && x >= 2F)`: в оригинале ровно то же.
- `ItemGeigerCounter:361-365` — дважды `list.add(0)`: в оригинале то же.
- Износ фильтра у маски-аттачмента не сохраняется (`IGasMask.damageFilter` → `ArmorUtil:67`) — тот
  же потерянный write-back есть в оригинальном `ArmorUtil.damageGasMaskFilter`. **Не путать с T3**,
  где оригинал write-back делает.
- `ArmorModificationHelper.pryMods` возвращает десериализованные копии — как `ArmorModHandler.pryMods`.
- `FalloutConfigJSON.chooseRandomOutcome` (первая запись получает двойной вес) и формула затухания —
  посимвольно из оригинала.
- `OilDepositFeature` — `radiusSqr = r*r/2.0` и `dy^2*3` воспроизводят `MapGenBubble`, включая
  комментарий про деление на 2.
- `SatelliteManager` — схема ключей `SavedData` совпадает с `SatelliteSavedData`, `setDirty()` на
  месте.

### T3. Найдено, НЕ исправлено (нужен тест / решение)

- **Фильтр противогаза: потеря и дюп.** `ItemGasMaskFilter:54` ставит фильтр в прирученную **копию**
  из `pryMods` и не пишет обратно, но `shrink(1)` выполняет — фильтр исчезает. Зеркально
  `ItemModGasmask:56` вынимает фильтр из копии и кладёт игроку — **дюп на каждый клик**. В оригинале
  `ItemFilter` вызывает `ArmorModHandler.applyMod`, а `ItemModGasmask` работает с held-стеком, то
  есть оба случая — отклонения порта. Правка затрагивает контракт `pryMods`/`applyMod`, поэтому
  нужен отдельный аккуратный проход по всем путям установки/снятия модов.
- `ItemModGasmask:70-84` — ветка недостижима: `player.getItemInHand(hand)` это тот же стек, что и
  `stack`, то есть сам `ItemModGasmask`, а проверяется `instanceof ItemGasMaskFilter`.
- ~~`RBMKColumnBlockItem:20-25`~~ — исправлено, см. AS.
- ~~`FlavouredRecordItem`~~ — исправлено, см. AQ5.
- `ModShovelItem` — поле `fortuneLevel` объявлено, клампится и нигде не читается, при этом сплавовая
  лопата зарегистрирована с `fortuneLevel = 2`. У кирки тот же параметр даёт переключаемый режим
  удачи (тултип, toggle, apply/clear через `setEnchantmentLevel`); у лопаты нет ничего. Это не
  однострочная правка, а перенос механики режима, поэтому в очередь.
- ~~`BlockExplosionDefense:272`~~ — исправлено, см. AQ1.
- ~~`ConfigSchema.register()`~~ — исправлено, см. AQ3.
- ~~`NuclearExplosionHelper:75`~~ — исправлено, см. AQ4.
- ~~`ModConfigKeybindHandler`~~ — исправлено, см. AQ2.
- `StructureFoundationProcessor:30` — `processBlock(LevelAccessor, ...)` это перегрузка, а не
  переопределение (база принимает `LevelReader`), `@Override` отсутствует; процессор инертен и ни
  в одном `processor_list` не упомянут. Чинить сигнатуру без разбора, куда он должен быть подключён,
  бессмысленно — плюс `(ServerLevel) level` на строке 38 бросит `ClassCastException` на
  `WorldGenRegion`.
- `WasteBlastGenerator:458` — третьим аргументом `calculateSurvivalChance` передаётся манхэттенское
  расстояние блока вместо `maxRadius` (в мгновенном варианте на `:153` передаётся `radius`).
- `WasteBlastGenerator:127,416,444` — сортировка по убыванию расстояния ломает модель затенения,
  рассчитанную «от центра к краям», а `protectedBlocks` пересоздаётся внутри каждого батча.
- `CraterGenerator:883` — зонные циклы читают `getBlockState` до ~200 блоков без проверки загрузки,
  тогда как `processDamageChunkBatch` рядом проверяет `level.hasChunk`.
- `ArmorUtil:28-42` — `checkForHaz2`/`checkForDigamma`/`checkForDigamma2`/`checkForFaraday` всегда
  `false`, то есть эти типы защиты не работают нигде (по javadoc — намеренно частичный порт).
- Мёртвый код в сборке: `util/CraterGenerator`, `util/CraterBiomeApplier`, `util/BlockExplosionDefense`
  (живой путь — `util/explosions/nuclear/*`), `util/explosions/trash_that_i_forgot_to_delete/*`,
  `worldgen/OilClasterSurroundedFeature` (в `ModWorldGen` есть, configured feature нет).

## U. Совместимость с Sable: дюп мультиблоков при сборке/разборке

**Репорт:** при сборке/разборке Sable-структур мультиблоки HBM дюпались.

**Причина.** Sable 2.0.5 объявляет обе точки входа статическими:

```
public static ServerSubLevel assembleBlocks(ServerLevel, BlockPos, Iterable<BlockPos>, BoundingBox3ic)
public static void          moveBlocks(ServerLevel, AssemblyTransform, Iterable<BlockPos>)
```

А во всех трёх наших Sable-миксинах обработчики были объявлены **нестатическими**
(`private void`). Mixin для статического таргета требует статический обработчик, поэтому
**ни один из них не применялся**. Дополнительно:

- инъекции в `assembleBlocks` использовали `CallbackInfo`, хотя метод возвращает значение —
  нужен `CallbackInfoReturnable`;
- `SubLevelAssembleExpansionMixin` не принимал четвёртый параметр `BoundingBox3ic`;
- `@Redirect` в `SubLevelRelinkMixin` возвращал `void`, тогда как перехватываемый
  `LevelChunk.setBlockState` возвращает `BlockState`.

Сверено по jar Sable 2.0.5 с тестового сервера (`javap`) и по эталону — мод `waystonessable`
миксинит тот же метод и объявляет
`(ServerLevel, BlockPos, Iterable<BlockPos>, BoundingBox3ic, CallbackInfoReturnable<ServerSubLevel>)`.

**Механизм дюпа** описан в javadoc самого `ContraptionAssemblyGuard`: внутри окна удаление наших
блоков — это перенос, движок уже сохранил state+NBT. Без окна на `onRemove` срабатывают каскад
`destroyStructure`, дроп станка по лут-таблице и дроп содержимого инвентаря — при том, что Sable
одновременно воссоздаёт блок на корабле. Окно не открывалось никогда, отсюда и дубликаты.
`SubLevelRelinkMixin` тоже не работал, поэтому перенесённые части сохраняли устаревший
`ControllerPos`.

### U1. Исправлено

- Все четыре обработчика окна сделаны статическими; пара на `assembleBlocks` переведена на
  `CallbackInfoReturnable<?>`.
- `@Redirect` перепривязки сделан статическим и возвращает предыдущий `BlockState`.

### U2. Расширение до полного мультиблока — ИСПРАВЛЕНО

`SubLevelAssembleExpansionMixin` приведён к реальной сигнатуре: `private static`, четвёртый
параметр `BoundingBox3ic`, `CallbackInfoReturnable`. Проверено по байткоду собранного jar.

Чтобы назвать `BoundingBox3ic`, нужен Sable на classpath компиляции. Артефакт
`maven.modrinth:sable:2.0.5+mc1.21.1`, который уже был в `compileOnly`, этих классов **не отдаёт** —
они лежат во вложенном jar-in-jar `sable-companion-common-1.21.1-1.6.0.jar` (35 КБ). Он положен в
`libs/` и подключён `compileOnly`. Полный jar Sable не понадобился: `ServerSubLevel` фигурировал
только в дженерике `CallbackInfoReturnable`, а дженерики стираются — достаточно `<?>`.

`libs/` был целиком в `.gitignore`, из-за чего сборка сломалась бы у всех остальных. Паттерн
изменён на `libs/*` с исключением для этого файла: git не позволяет вернуть файл из исключённого
каталога, поэтому именно так, а не добавлением `!`-строки.

## V. Ревью собственных правок: найденные и откаченные регрессии

Ревью шести коммитов (`96850a004`..`990bf5d8a`) нашло, что часть «исправлений» была изменениями
**против** оригинала, а не к нему. Все проверены по 1.7.10 повторно и откачены в `0c8d3d3b4`.

### V1. Откачено полностью

- **Формула урона взрыва (`EntityProcessorStandard`).** Возвращая спад по расстоянию, я вдобавок
  затёр `knockback` затухнутым значением **до** `calculateDamage`, а формула
  `(k² + k) / 2 * 8 * size + 1` возводит его в квадрат — Blast Protection начал резать урон второй
  раз и квадратично (примерно на треть при Blast Prot IV). В оригинале и в соседнем
  `EntityProcessorCross:129-147` это два разных значения: `knockback` идёт в урон, `enchKnockback`
  (после дампенера) — только в вектор отброса. Разделено обратно. `sqrt` при этом остаётся
  исправленным — та половина правки была верной.
- **Кассетные ракеты.** Оригинальный `EntityMissileCluster` вызывает
  `ExplosionChaos.cluster(..., this.rotationYaw, this.rotationPitch, (float) Math.PI * 0.25F, ...)`
  — то есть **градусы** для углов и радианы для разброса, ровно как было в порте. Перевод вызовов
  на `Math.toRadians` был отклонением от оригинала, причём с неверным знаком pitch: у пикирующей
  ракеты (`xRot ≈ +85°`) суббоеприпасы полетели бы вверх. Возвращено, поведение оригинала описано
  в javadoc метода.
- **`CustomNukeExplosion`, высота ядерного подрыва.** `yCoord + 5` в оригинальном `NukeCustom` —
  намеренный воздушный подрыв: водородная и тротиловая ветки идут с `+ 0.5`, ядерная нет.
  Возвращено. Дедуп `FAT_MAN_CORE` в том же месте корректен и оставлен.
- **Сопла инверсионного следа `MissileTier3`/`MissileTier4`.** `-thrust.z` в слоте `y` есть и в
  оригинале, в обоих тирах. «Выравнивание кольца» было новым поведением, а не восстановлением.

### V2. Доисправлено (правка была неполной, а не неверной)

- **`CrateItem.makeGroupingKey`** — строка `keyTag.remove(...)` лежит вне версионных веток, а ключ
  пишется по-разному (`Count` на 1.20.1, `count` на 1.21.1). Правка чинила 1.21.1 и ломала 1.20.1;
  теперь чистятся оба написания.
- **`GrenadeIfProjectileEntity`** — новый accessor фактически не читался.
  `ThrowableItemProjectile.defineSynchedData` зовёт `getDefaultItem()` **до** присвоения
  `entityData`, исключение глушилось `catch`, и `grenadeType` фиксировался в `GRENADE_IF` навсегда;
  трёхаргументный конструктор чинил поле, но не звал `setItem()`, поэтому синхронизируемый
  `DATA_ITEM_STACK` оставался обычной IF-гранатой. Убрано кеширование фолбэка, тип выставляется
  через `setItem()`.
- **`CraterBiomeHelper`** — был скопирован только половинный фильтр `WorldUtil.flushChunk`:
  добавлена проверка измерения, но не дальности, хотя комментарий обещал паритет. Дописана проверка
  по view distance.
- **`NukeMk5ChunkEater`** — на сохранении, сделанном до появления ключа `fluidClearCursorZ`,
  `getInt` вернул бы 0 и текущая колонка пересканировалась бы с `z = 0`. Теперь дефолт —
  начало колонки.
- **Комментарий про интервал звука самолётов** утверждал паритет с оригиналом, которого нет: в
  `EntityBomber` вообще нет фонового гула двигателя, звуки только в момент сброса. Значение 60
  тиков честно помечено как подбираемое.

### V3. Принято как есть, зафиксировано

- **Прочность поножей 16 против ванильных 15.** `BASE_DURABILITY = {11, 16, 16, 13}` — таблица
  самого мода, на 1.20.1 работала так же; правка лишь довела её до предметов на 1.21.1. Расхождение
  с ванилью ~6.7 %, менять — отдельное решение по контенту.
- **`steel_chestplate` зарегистрирован на материале `TITANIUM`** — опечатка авторов; теперь оттуда
  берётся и прочность, а не только защита. Маскировать не стал.
- **`bestEffortProvider` на физическом клиенте** уходит в `clientProvider()`, то есть для
  интегрированного сервера в одиночной игре проблема `ItemAssemblyTemplate` не закрыта. Для
  выделенного сервера — закрыта.

## W. Сеть тестового сервера — РЕШЕНО

Диагностика (только чтение, ничего не менялось на этом этапе) установила две складывающиеся причины
и, главное, что **сбои host-wide, а не в контейнере**: один и тот же тест 10 HTTPS-попыток к
фиксированному IPv4 давал 2/10 и из контейнера, и с хоста напрямую, с идентичной сигнатурой —
TCP-коннект за ~30 мс, зависание на TLS-хендшейке.

### W1. Причина 1 — PMTU black hole

Реальный MTU пути = 1280 (VPN на роутере `192.168.1.1`), а `ens18`, `pelican0` и `eth0` контейнера
стоят 1500. MSS-clamp отсутствует везде (`iptables -t mangle`, `ip6tables -t mangle`, `nft` пусты),
`net.ipv4.tcp_mtu_probing = 0`.

Механизм: MSS берётся из MTU маршрута, в SYN уходит 1460, удалённая сторона шлёт полноразмерные
сегменты, они дохнут. Первый крупный входящий флайт в HTTPS — ServerHello + Certificate, отсюда
«коннект есть, TLS висит». Доказательство причинности: после прогрева PMTU-кеша
(`ip route get` → `mtu 1280`) те же 10 попыток давали 10/10.

**Устранено владельцем сервера поднятием MTU туннеля.** Проверка: `ping -M do` проходит вплоть до
`-s 1472` (полный кадр 1500), раньше на `-s 1300` приходило `Frag needed and DF set (mtu = 1280)`.

### W2. Причина 2 — IPv6 настроен, но ведёт в никуда

Глобального IPv6 на хосте нет (у `ens18` только `fe80::`, дефолтного IPv6-маршрута нет), но Wings
поднимал `pelican_nw` с `EnableIPv6: true` и ULA `fdba:17c8:6c94::/64` плюс дефолтным маршрутом
внутри контейнера. Java не реализует Happy Eyeballs: попав на AAAA, ждёт 4+ с, а без таймаута —
бесконечно.

**Устранено:** в `/etc/pelican/config.yml` (бэкап `config.yml.bak-20260908-032440`) флаг
`IPv6: true` → `false`, сеть `pelican_nw` пересоздана. Проверка:
`EnableIPv6=false`, `subnets=172.18.0.0/16`, в контейнере ноль inet6-адресов и нет IPv6-маршрута.

### W3. Снятые обвинения

- **DNS исправен**, резолвер Docker не ломается. `getent hosts` печатает только AAAA **и на хосте
  тоже**, где IPv6 заведомо нет — это особенность вывода утилиты, а не результат резолва;
  `getent ahosts` отдаёт IPv4 первыми.
- Conntrack: 27 из 262144, в `dmesg` ничего. `ip -s link show ens18`: ноль ошибок и потерь.
- `/etc/docker/daemon.json` отсутствует, сеть целиком описана в Wings.

### W4. Результат

| Тест | Было | Стало |
|---|---|---|
| хост → `185.199.109.133` | 2/10 | 20/20 |
| хост → `adastra.terrarium.earth` | — | 10/10 |
| контейнер → `raw.githubusercontent.com` | 2/10 | 10/10 |

PMTU-кеш при этом пуст (`ip route get` не содержит записи `mtu`), то есть результат не эффект
прогрева. Зависания старта на `AdAstra.init → HttpClient.send` должны прекратиться: обе причины
закрыты.

Опционально на будущее: `net.ipv4.tcp_mtu_probing = 1` на хосте — страховка, если MTU туннеля
когда-нибудь снова просядет. Сейчас не требуется.

## X. Открытая очередь (сводка на 2026-09-08)

Всё, что найдено, но осознанно не исправлено. Разделы-источники указаны в скобках.

### Приоритет 1 — влияет на игру прямо сейчас

- ~~Дюп и потеря фильтра противогаза~~ — исправлено, см. Y1.
- ~~18 полей конфига вне `ConfigSchema`~~ — исправлено, см. AQ3.
- ~~Армированный бетон защищает в 46 раз хуже обычного~~ — исправлено, см. AQ1.
- ~~Пять клавиш крана RBMK не регистрируются на NeoForge~~ — исправлено, см. AQ2.

Приоритет 1 пуст: все известные точные ошибки этого уровня закрыты.

### Приоритет 2 — заметно, но не ломает прогресс

- ~~Пластинки не играются~~ — исправлено, см. AQ5 (`jukebox_playable` + записи `jukebox_song`).
- ~~У колонн RBMK плоские иконки~~ — исправлено, см. AS.
- ~~`enableCraterBiomes` не читается~~ — исправлено, см. AQ4.
- ~~`ItemModGasmask:70-84` — недостижимая ветка~~ — удалена вместе с правкой Y1.
- ~~`ModShovelItem.fortuneLevel` нигде не читается~~ — исправлено, см. AV: режим удачи
  перенесён с кирки, у сплавной лопаты он был заявлен и недоступен.
- `ArmorUtil` — `checkForHaz2/Digamma/Digamma2/Faraday` всегда `false`, эти типы защиты не работают
  нигде (T3, по javadoc — намеренно частичный порт).
- Скорости приёма/выдачи батарей плоские (100 Г/т) вместо `maxPower/200` и `maxPower/600`
  оригинала — тултип показывает формулу оригинала, батарея её не применяет (AY8).

### Приоритет 3 — производительность и живучесть на сервере

- Сканы взрывов без проверки загрузки чанка: `ExplosionFleija.breakColumn`, `ExplosionSolinium`,
  `ExplosionTom`, `SpearEntity.descentBlast`, `EntityUFO.groundBelow`, `CraterGenerator:883` (R, T3).
  Решение не однозначное: радиус тикета сознательно ограничен 12 чанками, и «дорисовывать кратер
  или пропускать» — выбор по геймплею. Более новый движок MK5 выбрал пропускать.
- `RBMKNeutronHandler:223` — второй вызов `getHits`, там результат влияет на затухание потока (O3).
- 18 блок-сущностей шлют `sendUpdateToClient()` каждый тик (F).
- ~~`WasteBlastGenerator`~~ — исправлено, см. раздел AG.

### Приоритет 4 — гигиена и мелочи

- ~~`EntityWormBase`~~ — утверждение было неверным, см. AH2: поля сохраняются, проблема в самой природе сетевого id (та же в оригинале).
- ~~`SynchedEntityData` пишется на клиенте~~ — исправлено, см. AH1.
- `StructureFoundationProcessor` инертен: подтверждено, см. AI. Второе утверждение было неверным —
  процессор указан в 23 файлах `worldgen`, то есть структуры на него рассчитывают.
- Мёртвый код в сборке: `util/CraterGenerator`, `util/CraterBiomeApplier`,
  `util/BlockExplosionDefense`, `util/explosions/trash_that_i_forgot_to_delete/*`,
  `worldgen/OilClasterSurroundedFeature` (T3).
- `bestEffortProvider` на физическом клиенте уходит в `clientProvider()` — для интегрированного
  сервера в одиночной игре `ItemAssemblyTemplate` всё ещё уязвим (V3).
- Нестабильный GameTest `flowSwitchCutsPowerMidRun` (C) — после исправления M1 не воспроизводился,
  но причина недетерминизма отдельно не подтверждена.
- 47 неопределённых `c:`-тегов и 66 мёртвых рецептов (A).

## Y. Противогазы, compat/create, multiblock

### Y1. Дюп фильтра противогаза — ИСПРАВЛЕНО

`ArmorModificationHelper.pryMods` декодирует прицепленную к шлему маску из NBT в **новый** стек.
Всё, что порт писал в такую маску, уходило в эту копию и терялось. Оригинальный
`ItemFilter.onItemRightClick` после установки зовёт `ArmorModHandler.applyMod(helmet, mask)` —
этой записи обратно в порту не было.

Последствия при маске-модификации на шлеме:
- фильтра в маске не было → `filterStack.shrink(1)`, фильтр **исчезал**, маска оставалась пустой;
- фильтр был → старый выдавался игроку в руку **и оставался в шлеме**, новый уничтожался. Дюп.

Сделано:
- `GasMaskUtil.WornMask(mask, helmet)` + `resolveWornMaskRef` — маска вместе со шлемом, `commit()`
  пишет её обратно через `applyMod`. Для шлема-маски и слота лица Curios `commit()` — no-op
  (там стек живой). `resolveWornMask` оставлен для чтения.
- `ItemGasMaskFilter.use` — `commit()` после установки.
- `IGasMask.installFilter(ItemStack mask, ItemStack filter)` — сохраняет износ фильтра. Прежняя
  перегрузка по `Item` сбрасывала повреждение в 0, то есть «выкрутил-вкрутил» = вечный фильтр.
  Оригинал кладёт в NBT `filter.copy()`, то есть с износом. Перегрузка по `Item` осталась для
  свежих фильтров (`MobGearHandler`, GameTest'ы).
- `ArmorUtil.damageGasMaskFilter`, `BlockGasBase.damageWornFilter` — износ пишется обратно в шлем.
  **Расхождение с оригиналом осознанное:** 1.7.10 `damageGasMaskFilter` тоже теряет износ
  прицепленной маски (пишет `installGasMaskFilter` в копию из `pryMods`, а во второй ветке — вообще
  в шлем, а не в маску), из-за чего фильтр в прицепленной маске не расходуется никогда.
- `ItemModGasmask.use` приведён к оригиналу: работает со стеком **в руке**. Ветка установки фильтра
  (строки 70-84 старой версии) была недостижима — она проверяла руку на `instanceof
  ItemGasMaskFilter`, а в руке всегда сам противогаз. Побочный эффект: шифт+ПКМ больше не выкручивает
  фильтр из уже надетой прицепленной маски (в оригинале этого и не было; фильтр возвращается вместе
  с маской при снятии модификации в столе).
- `ArmorGasMaskItem.use` — `takeFilter` больше не мутирует стек на клиенте.

### Y2. Диагностическое логирование на INFO в горячих путях — ИСПРАВЛЕНО

Остатки отладки времён разбора дюпа мультиблоков, все на INFO и частью по-русски:

- `HbmMultiblockMovementChecks.register` — лог на **каждом** кандидате в
  `registerMovementAllowedCheck`, и `debug(...)` на каждой проверке привязанности. Create зовёт их
  для каждого блока по каждому направлению во время BFS сборки. Убрано / понижено до `debug`.
- `ContraptionAssemblyGuard.push/pop` — две строки INFO на каждую сборку и разборку контрапшена.
  Понижено до `debug`.
- `MultiblockStructureHelper` строки 1408 и 1589 — INFO на каждое подавленное
  `placeStructure`/`destroyStructure` внутри окна переноса, то есть по строке на машину на сборку.
  Понижено до `debug`.

### Y3. `ContraptionAssemblyGuard`: счётчик окон рассогласовывался — ИСПРАВЛЕНО

`push()` переставал инкрементировать глубину на `MAX_DEPTH = 8`, а `pop()` декрементировал всегда.
При вложенности глубже восьми окно закрывалось **раньше**, чем внешний движок заканчивал перенос
блоков, — то есть ровно в тот момент, когда `onRemove` снова начинает ронять станок лут-таблицей.
Это и есть дюп, ради которого гард написан. Ограничение снято: глубина вложенности не может
переполнить int, а страховкой от утечки остаётся таймаут окна (30 с).

### Y4. Проверено, чисто

- **Ядро RBMK** против 1.7.10: `moveHeat` (усреднение по 4 соседям, `stepSize`, раздача остатка
  reasim-воды/пара, `coolPassively`), `boilWater`, пороги и множители пара кипятильника
  (100/300/450/600 и 1/10/100/1000), `getOutputPos` с загрузчиком под колонной, тик стержня
  (порядок burn → updateHeat → provideHeat → радиация → теплообмен → проверка мелтдауна → сброс
  потока), `fluxFromType`, `receiveFlux`, `meltdown` с заражением корием 3×3×3. Расхождений нет;
  порт местами строже оригинала (лимит 50 000 колонн, проверка загруженности чанка, try/finally
  вокруг флага `dropLids`, защита от NaN в `receiveFlux`).
- **`recipe/`**: 38 типов, сериализаторы и типы зарегистрированы попарно, без дублей имён и без
  перекрёстных ссылок generic↔реализация; ни один сериализатор не остался незарегистрированным.
  Ручные сетевые кодеки (`readNetwork`/`writeNetwork`) симметричны во всех файлах.
- Ссылки на `net.minecraft.client.*` вне клиентских пакетов (`FluidDuctBlock`, `RedCableGaugeBlock`,
  `MissileBaseEntity`, `SoyuzEntity`, `ExplosionEffectStandard`, `FluidTank`, `ParticleTestBlock`)
  либо закрыты проверкой `instanceof ClientLevel`, либо лежат в методах, которые зовёт только GUI.
  Загрузку класса на выделенном сервере они не ломают.
- Скан расхождений ключей NBT (записано/прочитано) по всему `src/main` дал только ложные
  срабатывания: ключи через константы и через `PlatformHooks.writeBlockPos/readBlockPos`.
  `c_coreHeat`/`c_maxHeat` в `getNBTForConsole` стержня совпадают с оригиналом дословно.

### Y5. Найдено, НЕ исправлено (в очередь)

- `MultiblockStructureHelper:1524` — на каждую установку мультиблока пишется INFO со **списком всех
  позиций** (до 156 штук) и ради этого же зовётся `level.getNearestPlayer(..., 8.0, false)`.
  Аргументы вычисляются жадно, даже когда INFO выключен. Похоже на намеренный аудит-лог, поэтому не
  трогал; стоит либо завернуть в `isInfoEnabled()`, либо вынести список позиций в `debug`.
- Семантика `matches()` у 38 рецептов (учёт количеств ингредиентов, `getRemainingItems`) не
  проверялась — это отдельный проход с запуском машин, механической сверкой не берётся.
- `compat/` (JEI, Create, Curios) прямого прототипа в 1.7.10 не имеет, сверка по оригиналу к нему
  неприменима; ревью там нужно вести от поведения, а не от исходника.
- Непроверенными остаются `client/` (297 файлов), `particle/` (62), `datagen/` (85) и не покрытые
  разделом K части `block/` и `blockentity/`.

### Y6. Ядерный крипер спавнил не ту частицу — ИСПРАВЛЕНО

`EntityCreeperNuclear.sendMukeParticle` писал `data.putString("type", "nuke")`, хотя метод называется
`sendMukeParticle`, рядом играет `MUKE_EXPLOSION`, а оригинальный `EntityCreeperNuclear.func_146077_cc`
шлёт `"muke"`. Оба типа зарегистрированы в `ParticleCreators`, поэтому вместо вспышки мини-ньюка
(`MukeWaveParticle` + `MukeFlashParticle` + встряска игрока) рисовался полноразмерный торекс
`NukeTorexCreator`. Опечатка, исправлено на `"muke"`.

Сверка всех `putString("type", ...)` с ключами `ParticleCreators.CREATORS` после правки расхождений
не даёт (`dock`/`pattern`/`pos`/`unload` — это программа дрона, не частицы).

### Y7. Тридцать ключей локализации отсутствовали — ЧАСТИЧНО ИСПРАВЛЕНО

Диff «ключи, используемые в коде» против сгенерированного `en_us.json` (5080 записей) дал 30
отсутствующих. Из них 17 — заголовки GUI машин: игрок видел в шапке контейнера сырой ключ
(`container.hbm_m.mixer` и т.д.). Добавлены в `ModLanguageProviderEn` и `ModLanguageProviderRu`
по уже существующим названиям блоков:

`container.frackingTower` (единственный без неймспейса `hbm_m.` — так в коде BE и GUI),
`ashpit`, `coker`, `condenser_powered`, `conveyor_press`, `electric_furnace`, `furnace_brick`,
`machine_large_turbine`, `machine_satlinker`, `missile_assembly`, `mixer`, `pyrooven`, `radgen`,
`reactor_research`, `refinery`, `solidifier`, `soyuz_launcher`, `turbinegas`,
плюс `gui.hbm_m.assembler_recipe_selector`, `gui.hbm_m.fluid_identifier`,
`jei.hbm_m.crucible_smelting` и `tooltip.hbm_m.requires` (последний был только в русском провайдере).

`container.hbm_m.radgen` привязан к `MACHINE_RADGEN` (см. `ModBlockEntities:211`), поэтому взято
название «Radiation-Powered Engine», а не одноимённого блока `radgen`.

**Осталось (нужна формулировка, а не копия имени блока):** `armor.fsb.dashAbility`,
`armor.fsb.enhancedMobility` (`ArmorTooltipHandler`), `door.locked` (`DoorDecl`),
`message.hbm_m.detonator.no_saved_position` (`DetonatorItem`, ветка «нет сохранённой позиции»),
`msg.hbm_m.rbmk_tool.set`, `tooltip.hbm_m.mods` (`ArmorTooltipHandler`),
`trait.hlParticle`, `trait.hlPlayer` (`ItemDigamma`).

**Важно:** `src/generated/` в `.gitignore`, поэтому правка провайдеров вступит в силу только после
датагена (`stonecutterSwitchTo1.20.1-forge` → `runData -PnoClientMods` → обратно). Сами провайдеры
лежат под `//? if forge {`, то есть на активной версии закомментированы и `compileJava` их не
проверяет; добавленные строки структурно идентичны соседним (включая экранированные кавычки,
которые в файле уже встречаются 18 раз).

Стоит отдельно прогнать полный диф ключей между `ModLanguageProviderEn` и `ModLanguageProviderRu`:
`tooltip.hbm_m.requires` показал, что провайдеры разошлись, и это вряд ли единственный случай.

Полный диф провайдеров сделан: в сгенерированном `en_us.json` 5080 ключей, в `ru_ru.json` — 3781,
то есть **1312 ключей без русского перевода** (блоки-конденсаторы, графитовые блоки, вся серия
`concrete_colored_ext_*`, части `container.hbm_m.*` — краны, дроны, RTG, телепортер, PWR и др.).
Это не баг кода, а пробел контента: `en_us`/`ru_ru` генерируются провайдерами, остальные 90+ локалей
приходят из Crowdin. Отдельная задача на перевод, полный список получается командой

```
comm -23 <(ключи en_us) <(ключи ru_ru)
```

### Y8. Ревью раздела Y: что нашлось в собственных правках

- **Запись в шлем каждый тик.** `commit()` в `ArmorUtil.damageGasMaskFilter` и
  `BlockGasBase.damageWornFilter` вызывался безусловно, а эти методы исполняются каждый тик, пока
  игрок стоит в газе. `applyMod` копирует тег шлема, сериализует стек маски и переписывает
  CUSTOM_DATA — то есть слот брони помечался изменённым и пересылался клиенту ежетиково даже когда
  фильтра в маске нет и `damageFilter` ничего не делал. Исправлено: `damageFilter` теперь возвращает
  `boolean` «что-то изменилось», и `commit()` вызывается только по нему. При надетом рабочем фильтре
  запись всё равно ежетиковая — иначе накопленный износ теряется вместе с копией маски; батчинг
  потребовал бы отдельного счётчика на сущность, вынесено в очередь ниже.
- **Дубликат ключа локализации + потерянный перевод.** Вставка блока в `ModLanguageProviderEn`
  затёрла строку `add(ModItems.NUKE_PROTOTYPE.get(), "Prototype")` и продублировала
  `add("container.hbm_m.nuke_prototype", ...)`. Forge'овский `LanguageProvider` на дублирующемся
  ключе бросает исключение, то есть датаген упал бы целиком. Восстановлено, дублей в обоих
  провайдерах больше нет (проверено `uniq -d`).
- **Мёртвый метод.** Добавленный `WornMask.isEmpty()` не использовался — убран.
- **Неиспользуемый импорт** `EquipmentSlot` в `ItemModGasmask` после переписывания `use()` — убран.
- **Изменение поведения, оставлено осознанно.** У `ArmorGasMaskItem` шифт+ПКМ по маске **без**
  фильтра теперь проваливается в `super.use()` (то есть надевает маску), раньше возвращался `pass`
  и не происходило ничего. Оригинальный `ItemModGasmask.onItemRightClick` всегда зовёт `super`,
  и ваниль надевает броню по ПКМ независимо от приседания, так что это ближе к оригиналу.
- Полный аудит удалённых строк по всему рабочему дереву (`git diff | grep '^-'`) — все удаления
  осознанные, потерянных строк, кроме описанной выше, нет.

Побочно: `repl.pl` работает с LF, а часть файлов в CRLF, из-за чего в них теперь смешанные
переводы строк. На коммит не влияет (`core.autocrlf` нормализует), но нумерация строк после правки
сдвигается — именно на этом и произошла ошибка с дублем ключа. Правило: после каждой правки по
номерам строк перечитывать окрестность, а не только результат вставки.

### Y9. В очередь (после ревью)

- Износ фильтра прицепленной маски пишет CUSTOM_DATA шлема каждый тик в газе. Дешевле было бы
  копить износ в поле на сущности и сбрасывать в шлем раз в секунду и при исчерпании фильтра.

### Y10. Механические сверки по всей кодовой базе — чисто

Все проверки ниже прогнаны скриптами по `src/main`, находок нет; фиксирую, чтобы не повторять.

- **Лут-таблицы блоков.** 1020 зарегистрированных блоков против 1187 сгенерированных таблиц: без
  таблицы остались ровно 11 — `chlorine_gas`, девять `gas_*` и `taint`, то есть газовые объёмы и
  порча, которые и не должны ничего ронять.
- **Звуки.** 128 `registerSoundEvents` против 137 записей `sounds.json` — расхождений нет (лишние
  записи в json это структурные ключи и файлы `bounce1..3`). Все 108 файлов, на которые ссылается
  `sounds.json`, физически есть в `assets/hbm_m/sounds`.
- **Привязка BlockEntity к блокам.** Каждая запись `BLOCK_ENTITIES.register` разобрана отдельно и
  сверена с классом BE и списком `ModBlocks.*` в `Builder.of`. Все расхождения — только порядок слов
  в имени (`iron_crate_be` ↔ `CRATE_IRON`, `drone_provider_be` ↔ `DRONE_CRATE_PROVIDER`), ни одной
  ошибочной привязки.
- **`setChanged()` при мутации сохраняемого состояния.** Четыре BE не зовут его вовсе
  (`BaseHbmBlockEntity`, `RBMKControlManualBlockEntity`, `RBMKPanelDeviceBlockEntity`,
  `OreBedrockBlockEntity`) — во всех случаях пометка делается вызывающей стороной
  (`receiveControl` в семи наследниках панели зовёт `setChanged(); syncToClient();`) либо
  родительским классом.
- **`TileEntityRBMKControlManual`.** `getMult` (позитивный скрам графитового наконечника) совпадает
  с оригиналом дословно; порядок присваиваний в `setTarget` эквивалентен, а добавленный в порту
  кламп `targetLevel` в [0,1] — ужесточение, а не расхождение.

Использованные скрипты (одноразовые, лежат в `/tmp`): сверка ключей NBT записано/прочитано,
симметрия сетевых кодеков рецептов, диф ключей локализации против сгенерированного `en_us.json`,
кросс-проверка `putString("type", ...)` против `ParticleCreators.CREATORS`.

## Z. Частицы и модули машин

### Z1. Обломки взрыва летели вниз — ИСПРАВЛЕНО

`ExplosionClientCreator` строил вектор разлёта как `angle = -toRadians(45..70)` и брал
`vy = velocity * sin(angle)` — то есть **отрицательную** вертикальную скорость.

Оригинал делает это через `Vec3(velocity,0,0).rotateAroundZ(-pitch)`, а `Vec3.rotateAroundZ`
в 1.7.10 считает `y' = y*cos - x*sin`; при `y = 0` и отрицательном угле получается
`y' = +velocity*sin(pitch)`, то есть **вверх**. Порт подставил тот же отрицательный угол прямо в
`sin()` и перевернул знак: все искры уходили в землю. Горизонтальные компоненты при этом совпадали
(знак `vz` отличается от оригинала, но yaw равномерен по [0,2π), так что распределение то же).

Заодно сверено и совпадает: `SPEED_OF_SOUND = 17.15*0.5`, все три пресета
(`composeEffectSmall/Standard/Large`), радиус пакета `max(300, soundRange)`, ближний/дальний звук
с задержкой по скорости звука.

`debrisSize`/`debrisRetry` отправитель пишет, а клиент не читает — это не баг: оригинал строит из
них воксельный `WorldInAJar` для каждого обломка, порт заменил обломки на искры. Так же не портирован
`SkeletonCreator.composeEffectGib` (ключи `gib`/`force`) — заготовка, вызовов нет.

### Z2. Прогресс крафта терялся при перезагрузке чанка — ИСПРАВЛЕНО

`MachineModuleBase.writeToNBT` сохраняет `progress`, но `currentRecipe` — поле не сохраняемое.
На первом же тике после загрузки `currentRecipe == null`, модуль перевыбирал рецепт и **обнулял
прогресс**, то есть сохранение прогресса было бессмысленным: любая машина начинала крафт заново
после каждого перезахода в мир или выгрузки чанка.

Оригинальный `ModuleMachineBase` сохраняет и `progress`, и имя рецепта (`recipe<index>`), а в
`update` прогресс сбрасывается только когда `canProcess` вернул false, — то есть переживает
перезагрузку. Теперь прогресс обнуляется только при **настоящей** смене рецепта (предыдущий был не
null). Смена рецепта из GUI идёт через `setSelectedRecipeId` → `resetProgress`, так что там
поведение прежнее; дюпа не появляется, потому что перед `processCraft` всё равно проверяется
`canProcess`.

### Z3. Химзавод не стартовал без энергии на весь цикл — ИСПРАВЛЕНО

`MachineModuleChemplant.requiresFullEnergyBufferToStart()` возвращал `true`, из-за чего машина ждала
накопления `energyPerTick × duration` перед началом крафта — а если это больше её буфера, не
запускалась вообще никогда.

Три независимых источника говорят, что должно быть `false`: javadoc базового класса прямо называет
химмашину примером варианта «достаточно энергии на текущий тик»; родственный
`MachineModuleChemFactoryLane` уже возвращает `false` с ссылкой на 1.7.10; и сам оригинальный
`ModuleMachineBase.canProcess` проверяет только `battery.getPower() >= recipe.power`, то есть расход
одного тика. Исправлено.

Подтверждение достижимости: `MachineChemicalPlantBlockEntity` растит ёмкость буфера до
`max(MAX_POWER, recipe.getPowerConsumption() * 100)`. То есть буфера хватает ровно на 100 тиков
крафта, и любой рецепт длительностью больше 100 тиков не мог запуститься в принципе.

**В очередь:** значение по умолчанию `requiresFullEnergyBufferToStart() == true` в базовом классе —
изобретение порта, в оригинале ни одна машина не ждёт энергии на весь цикл. Менять умолчание
означает трогать все машины сразу, поэтому это отдельное решение, а не точечная правка.

## AA. Система дронов

### AA1. Дроны самоуничтожались от любого урона — ИСПРАВЛЕНО

`EntityDeliveryDrone.hurt` и `EntityRequestDrone.hurt` разрушали дрон при **любом** источнике урона.
В `EntityDeliveryDrone` это особенно наглядно: `hurt` звал `hitByEntity(source.getEntity())`, а сам
`hitByEntity` параметр `attacker` не использовал вообще — проверка, потерянная при портировании.

Оригинальный `EntityDroneBase.hitByEntity` срабатывает только если `attacker instanceof
EntityPlayer`, а `attackEntityFrom` он не переопределяет — то есть в 1.7.10 дрон неуязвим для огня,
лавы, кактуса, взрывов и мобов. В порту любой такой урон ронял дрон вместе с грузом на землю
посреди маршрута.

Теперь `hurt` разрушает дрон только когда `source.getEntity() instanceof Player`, иначе возвращает
`false` (урон не принят). Небольшое расхождение в безопасную сторону: выстрел игрока из лука тоже
считается (у стрелы `getEntity()` — стрелок), в оригинале стрела не делала ничего.

### AA2. Сверено, расхождений нет

- `MachineDroneProviderBlockEntity.extractMatching` и `MachineDroneRequesterBlockEntity.depositStock`
  — ни дюпа, ни потери: забор идёт из первого подходящего слота, остаток груза возвращается дрону,
  раскладка по стоковым слотам умеет добивать неполные стеки.
- Программа дрона (список шагов, шаги `UNLOAD`/`DOCK`, рейтрейс на 4 блока вниз, `nextActionTimer`)
  соответствует оригинальному `onUpdate`.
- Система апгрейдов: множители химзавода совпадают с оригиналом дословно
  (`speed = 1 + min(SPEED,3)/3 + min(OVERDRIVE,3)`,
  `pow = 1 − 0.25·min(POWER,3) + min(SPEED,3) + 10/3·min(OVERDRIVE,3)`), как и рост буфера
  до `recipe.power * 100` при минимуме `100 000`. Ни одна машина не читает уровень апгрейда,
  которого нет в её `VALID_UPGRADES` (проверено скриптом по всем BE).

### AA3. Найдено, НЕ исправлено

- **Груз теряется при стыковке.** `MachineDroneDockBlockEntity.dockDrone` кладёт груз в слот `i+1`
  только если тот пуст; иначе `dockDrone` всё равно возвращает `true`, `tryDock` делает `discard()`
  и груз уничтожается. Это дословный порт поведения оригинала (там ровно та же дыра), поэтому
  трогать не стал — но чинится дёшево: возвращать из `dockDrone` признак «груз не влез» и ронять
  его предметом.
- **Правый клик по дрону разрушает его.** `interact` в обоих дронах — добавка порта; в оригинале
  дрон разбирается только ударом. Скорее удобство, чем ошибка, но расхождение зафиксировано.
- **`readAdditionalSaveData`** читает `entry.getIntArray("pos")` и сразу берёт `p[0..2]`. На битом
  или обрезанном теге это `ArrayIndexOutOfBoundsException` при загрузке сущности, то есть падение
  загрузки чанка. В оригинале ровно так же, но там это 1.7.10; здесь дешевле проверить длину.
- **Мьютекс-апгрейды не портированы.** В `UpgradeManagerNT` тип с флагом `mutex` вытесняет ранее
  установленный мьютекс-апгрейд с меньшим ordinal и всегда даёт уровень 1; в порту у `UpgradeType`
  флага `mutex` нет вообще и уровни просто суммируются. Это непортированная механика, а не баг —
  но балансно машины в порту могут комбинировать то, что в оригинале взаимоисключающе.
- **`UpgradeManager.checkSlots` считает апгрейды, которых нет в `VALID_UPGRADES`** (лимит по
  умолчанию `Integer.MAX_VALUE`), тогда как оригинал такие предметы игнорирует полностью. Сейчас
  не проявляется (см. AA2), но станет ловушкой, когда машина начнёт читать новый тип апгрейда.
- **Кэша слотов нет.** `UpgradeManagerNT` пересчитывает уровни только при изменении содержимого
  слотов, порт — каждый тик. На 4–8 слотах это ничто, но у машин с большим диапазоном стоит учесть.

## AB. Краны и конвейеры

### AB1. Пять машин не работали на NeoForge вообще — ИСПРАВЛЕНО

Логика доступа к чужому инвентарю у пяти блоков целиком лежала в `//? if forge {` **без**
neoforge-ветки, то есть на 1.21.1 была закомментирована. Это ровно ловушка из раздела S, но не с
директивой, а с отсутствующим портом ветки:

| Блок | Что не работало на NeoForge |
|---|---|
| `MachineCraneExtractorBlockEntity` | не забирал предметы из блока, на который смотрит, — то есть не делал ничего вообще; и не клал в инвентарь на выходе, а ронял на пол |
| `MachineCraneInserterBlockEntity` | не вставлял ничего в целевой инвентарь — вся его функция |
| `MachineCraneGrabberBlockEntity` | мог отдать предмет только на конвейер, но не в инвентарь |
| `MachineCraneRouterBlockEntity` | ронял предметы на пол вместо вставки в инвентарь на соответствующей стороне |
| `RadioTorchCounterBlockEntity` | не читал инвентарь под собой, то есть никогда ничего не транслировал |

Кросс-платформенный хелпер `com.hbm_m.api.item.ItemHandlerAccess` для этого уже был написан, но
**не использовался ни в одном месте** — его добавили и не подключили. Теперь все пять блоков ходят
через него.

Ключевой приём: `var handler = ItemHandlerAccess.getItemHandler(...)` компилируется на обеих
платформах, потому что `var` выводит платформенный тип `IItemHandler`, а имена методов
(`getSlots`, `getStackInSlot`, `insertItem`, `extractItem`) совпадают. Ветка Stonecutter нужна
только там, где тип пишется явно. Для замены платформенно-разного `ItemHandlerHelper.insertItem`
в `ItemHandlerAccess` добавлен `insert(level, pos, side, stack, simulate)`.

### AB2. Боксер выбрасывал буфер на пол — ИСПРАВЛЕНО

`MachineCraneBoxerBlockEntity` собирал предметы из слотов и **потом** проверял, есть ли на выходе
конвейер; если конвейера нет — ронял коробку на землю. Оригинальный `TileEntityCraneBoxer`
проверяет `belt != null` **до** очистки слотов и просто ничего не делает. Боксер, повёрнутый в
стену, вываливал содержимое каждые два тика. Проверка перенесена вперёд.

Там же: `setChanged()` вызывался каждый тик безусловно; теперь только при смене редстоун-сигнала
и при реальной отправке коробки.

### AB3. Сверено, расхождений нет

- **Сплиттер**: упрощение (один `ratio` вместо независимых left/right) документировано; чередование
  проверено вручную — при ratio=2 получается left,left,right, как и должно.
- **Разбоксер**: задержки эжектора (20/10/5/2), размеры пачки стека (1/4/16/64), отключение
  редстоуном, выдача одного предмета за раз — всё совпадает. Сторона выдачи: оригинал шлёт в
  `getInputSide()`, порт — в `facing.getOpposite()`; это согласуется, потому что в порту `FACING`
  означает сторону **выхода**, а в оригинале метадата — сторону входа.
- **Роутер**: выбор направления (`MODE_NONE`/`WHITELIST`/`BLACKLIST`/`WILDCARD`, фолбэк на
  wildcard, случайный выбор из подходящих, сброс на пол при отсутствии направления) совпадает с
  `CraneRouter.getOutputDir` дословно. `Direction.values()[side]` даёт тот же порядок, что и
  `ForgeDirection.getOrientation(side)` (DOWN, UP, NORTH, SOUTH, WEST, EAST).

### AB4. Доменная печь: посторонняя автоматизация на NeoForge не ограничена — ИСПРАВЛЕНО

`BlastFurnaceBlockEntity.getItemHandler(side)` на NeoForge возвращал сырой `itemHandler`, игнорируя
сторону. Ограничения `canInsertFromDirection`/`canExtractFromDirection` при этом есть **и на Forge**
(`DirectionalItemHandler`), **и на Fabric** (`FilteringStorage` в `buildStorageForSide`) — не было
только на целевой платформе. Воронка под печью могла вытаскивать несплавленное сырьё из входных
слотов, а воронка сбоку — класть что угодно в выходной.

Добавлена neoforge-ветка `DirectionalItemHandler` (зеркало forge-версии) с кэшом по сторонам в
`EnumMap`. Ветка `//?} else {` заодно разделена на `elif neoforge` и `else`, иначе fabric-сборка
ссылалась бы на несуществующий там класс.

Проверено, что это единственный такой случай: `DirectionalItemHandler`/`canInsertFromDirection`
встречаются только в этом файле, у остальных машин посторонний доступ разруливает
`BaseMachineBlockEntity`.

### AB5. Скан forge-only блоков без neoforge-ветки

Прогнан скрипт по всему `src/main`: блоки `//? if forge {` в файлах, где слово `neoforge` не
встречается вообще, с фильтром «есть цикл или больше 12 строк». Из ~30 попаданий все, кроме
исправленных выше, легитимны:

- целиком forge-only файлы (`*FluidHandler.java`, `ChunkRadiationProvider`, `*Forge.java`) — на
  NeoForge их роль выполняет другой механизм;
- переопределения `getCapability` — на NeoForge всё регистрируется централизованно в
  `ModCapabilities.register`, включая обёртку энергии `ConverterBlockEntity`.

Отдельно: класс `ArmorModificationServerEvents` целиком forge-only (подрезает здоровье игрока после
смены брони). На NeoForge его нет, но и не нужно — ваниль сама зажимает здоровье в
`LivingEntity.onAttributeUpdated` при падении `MAX_HEALTH`. То есть класс избыточен и на Forge.

## AC. ЛЭП, пилоны и скан «написано, но не подключено»

### AC1. Пилоны — сверено, расхождений нет

`PylonBaseBlockEntity` соответствует `TileEntityPylonBase`: `canConnect` (тип → сам с собой →
дальность), `tryConnect` вешает связь с **обеих** сторон, `disconnectAll` вычищает обратные ссылки
и рушит узлы на обоих концах, `getRenderBoundingBox` бесконечный. Порт местами строже оригинала —
`addConnection` проверяет узел на null, оригинал зовёт `node.recentlyChanged` без проверки.

**В очередь (паритет с оригиналом):** ни `canConnect`, ни `addConnection` не проверяют, что пара
уже соединена. Повторное протягивание провода между теми же пилонами дублирует запись в `connected`
и ребро в энергетическом графе, и так неограниченно. В 1.7.10 ровно та же дыра.

`ItemWiring`: `clearStart` вызывается только на сервере, поэтому клиентская копия стека держит
старую точку до следующей синхронизации слота — визуальный рассинхрон тултипа, не более.

### AC2. Скан неиспользуемых публичных хелперов

Скрипт прошёл по `api/`, `util/`, `handler/`, `platform/` и нашёл публичные статические методы, на
которые нет ни одной ссылки вне их собственного файла. Так был найден `ItemHandlerAccess` (раздел
AB1) — хелпер, написанный ради пяти сломанных машин и ни разу не подключённый.

Остальные попадания разобраны и багами не являются: `HazmatRegistry.registerHazmat` зовётся из
`registerHazmats()` в том же файле (ложное срабатывание скрипта), прочее — геттеры для GUI,
отладочные помощники и API ещё не портированных систем (`ConfettiUtil.pulverize/cremate`,
`ContaminationUtil.neutronActivateInventory`, `SellafitSolidificationTracker.*`).

**Закрыто (сверка с оригиналом).** `RBMKDials.getReaSimRange` не читается нигде — но и в оригинале
тоже: `NeutronHandler:41-42` присваивает `columnHeight = getColumnHeight+1` и `fluxRange =
getFluxRange`, а `getReaSimRange` не вызывает никто, несмотря на javadoc «how far the flux of a
ReaSim fuel rod reaches». Порт повторяет оригинал один в один (`NeutronNodeWorld:81-82`), сам
разброс ReaSim-канала тоже дословный (8 потоков через 45°, 0.75 потока, доворот кратно 9°).
Отклонения порта нет, дайл мёртв у обоих.

### AC3. Скан асимметрии платформенных веток

Сравнил длину веток `forge` и `neoforge` в каждой паре `//? if forge { … } elif neoforge { … }` по
всему `src/main`. Единственное расхождение — `ChunkRadiationAccess` (neoforge длиннее, потому что
attachment требует больше кода, чем capability). Обратных случаев, где neoforge получил заглушку,
а forge — логику, нет.

## AD. Границы отрисовки и структурные сверки

### AD1. Провода ЛЭП и автозагрузчик РБМК куллились по одному блоку — ИСПРАВЛЕНО

На NeoForge 1.21.1 у `BlockEntity` нет метода `getRenderBoundingBox()` — в `IBlockEntityExtension`
21.1.248 его просто нет, куллинг идёт через `BlockEntityRenderer#getRenderBoundingBox(be)`
(по умолчанию один блок). Порт это учёл: есть интерфейс `RenderBoundsProvider`, который реализует
`BaseHbmBlockEntity`, и мост `HbmBerBounds`, который рендерер должен подключить.

Два рендерера мост не подключили и наследовали дефолт в один блок:

- `RedPylonWireRenderer` — `PylonBaseBlockEntity.getRenderBoundingBox()` возвращает AABB ±1e7
  именно ради того, чтобы стометровые провода не пропадали; рендерер это значение отбрасывал,
  и провода исчезали, как только сам пилон уходил за границу кадра.
- `RBMKAutoloaderRenderer` — то же самое для автозагрузчика.

Оба переведены на `HbmBerBounds`. Проверено, что это все случаи: каждый BER сопоставлен со своим
BE, и только у этих двух BE есть собственный `getRenderBoundingBox`, который рендерер не читал
(остальные либо уже на `HbmBerBounds`, либо наследуют `AbstractPartBasedRenderer`, который сам
делегирует в `RenderBoundsProvider`).

### AD2. Структурные сверки — чисто

- **Тикеры.** У всех BE с `public static void tick(...)` есть блок, который вешает тикер
  (через `createTickerHelper` с лямбдой). Незапускаемых машин нет.
- **Меню и экраны.** 143 типа меню — 143 зарегистрированных экрана, расхождений нет
  (открытие GUI без экрана роняет клиент).
- **Сущности.** Все 84 типа сущностей упоминаются в клиентском коде, то есть без рендерера
  не осталась ни одна.
- **Сигнатуры ванильных хуков.** Скан методов с именами ванильных хуков, объявленных **без**
  `@Override` (учитывая, что аннотация не обязательна, но её отсутствие скрывает дрейф сигнатуры,
  и с фильтрацией строк внутри Stonecutter-комментариев): три попадания, все — собственные хелперы
  с совпавшими именами (`MultiblockPlacement.canSurvive`, `MultiblockStructureHelper.getRenderBoundingBox`,
  `ContraptionDoorState.getShape`). Настоящего дрейфа сигнатур нет.

## AE. Конвейеры

### AE1. Груз на ленте сбрасывался от любого урона — ИСПРАВЛЕНО

`MovingConveyorItemEntity.hurt` и `MovingConveyorPackageEntity.hurt` роняли предмет/посылку с ленты
при **любом** источнике урона. Оригинальный `EntityMovingConveyorObject.hitByEntity` реагирует
только на игрока и не переопределяет `attackEntityFrom`, то есть огонь, лава и взрывы груз на ленте
не трогают. В порту конвейер, проложенный через горячий участок, рассыпал бы весь поток.

Теперь `hurt` срабатывает только при `source.getEntity() instanceof Player`. Удар игрока при этом
по-прежнему работает и через `skipAttackInteraction` (правильный ванильный хук для не-живых
сущностей), который уже был на месте. Порт при этом мягче оригинала — он **роняет** груз предметом,
тогда как оригинальный `hitByEntity` делает `setDead()` без дропа; это осознанное улучшение.

### AE2. Защита от затора могла снести ленту сразу после появления предмета — ИСПРАВЛЕНО

Уничтожение блока ленты при заторе не было закрыто проверкой возраста. Оригинал требует
`ticksExisted > 400`; в порту проверка затора идёт при `(tickCount + getId()) % 400 == 0`, что при
подходящем id срабатывает уже на шестом тике жизни предмета. Добавлено `tickCount > 400`.

### AE3. Найдено, НЕ исправлено

- **Два конфига оригинала не портированы.** `CONVEYOR_CRAM_MAX` (порог затора) захардкожен как 25,
  а `CONVEYOR_CRAM_EXPLODE` (разрешено ли сносить саму ленту) отсутствует вовсе — порт сносит всегда.
  Добавление конфигов — отдельная задача, см. пункт про 18 недостающих полей `ConfigSchema`.
- **Затор считается по одному типу груза.** Оригинал считает `EntityMovingConveyorObject`, то есть
  предметы и посылки вместе; в порту это два независимых класса без общего предка, поэтому
  смешанный затор не достигает порога ни по одному из них, а у `MovingConveyorPackageEntity`
  проверки затора нет вообще. Следствие упрощения, задокументированного в javadoc класса.

## AF. Система опасностей (hazard)

### AF1. Ослепляющие предметы игнорировали защиту головы — ИСПРАВЛЕНО

`HazardTypeBlinding.onUpdate` накладывал слепоту **безусловно**. Оригинал закрывает это проверкой
`ArmorRegistry.hasProtection(target, 3, HazardClass.LIGHT)` — то есть противогаз или очки должны
защищать. Проверка при портировании потерялась, и защитная экипировка от ослепления не работала.
`HazardClass.LIGHT` и `ArmorRegistry.hasProtection` в порту уже были — не хватало только вызова.

### AF2. Поиск ричера строил стек каждый тик — ИСПРАВЛЕНО

`HazardTypeRadiation.onUpdate` определял наличие ричера через
`player.getInventory().contains(new ItemStack(ModItems.REACHER.get()))`. Два дефекта:

- `Inventory.contains(ItemStack)` на 1.21.1 сравнивает **и компоненты** (`isSameItemSameComponents`),
  а оригинал зовёт `inventory.hasItem(ModItems.reacher)` — сравнение по предмету. Переименованный
  ричер переставал считаться;
- аллокация `new ItemStack` на пути, который исполняется **каждый тик для каждого опасного стека**
  в инвентаре — ровно та горячая точка, что описана в J2.

Заменено обходом слотов с `stack.is(REACHER)`.

### AF3. Сверено с оригиналом, расхождений нет

`HazardTypeRadiation` (включая порядок `level *= count` до нормализации и обе ветки reacher —
`rad / 49` в режиме 528 и `BobMathUtil.squirt` иначе), `HazardTypeCoal` (в том числе шанс
`nextInt(max(65 - count, 1))` для забивания фильтра), `HazardTypeAsbestos` (асимметрия с коалом —
у асбеста действительно **нет** умножения на размер стека, так в оригинале), `HazardTypeHot`
(`isWet()` → `isInWaterOrRain()`), `HazardTypeDigamma` (`level / 20`), `HazardTypeHydroactive`
(обнуление стека и взрыв с разрушением блоков).

Мелкое расхождение, оставлено: в `HazardTypeHydroactive` оригинал передаёт источником взрыва
`null`, порт — саму сущность; влияет только на атрибуцию урона в сообщении о смерти.

### AF4. Вся защитная экипировка, кроме фильтров, не была зарегистрирована — ИСПРАВЛЕНО

`ArmorRegistryInit.init()` регистрировал классы защиты только для пяти фильтров и двух тряпичных
масок. Оригинальный `ArmorUtil.register()` регистрирует ещё тринадцать предметов, и все они в порту
уже существуют — просто не были внесены в реестр:

| Предмет | Классы (по оригиналу) |
|---|---|
| `gas_mask` | SAND, LIGHT |
| `gas_mask_m65`, `attachment_mask` | SAND |
| `goggles`, `ashglasses` | LIGHT, SAND |
| `asbestos_helmet` | SAND, LIGHT |
| `hazmat_helmet`, `hazmat_helmet_red`, `hazmat_helmet_grey` | SAND |
| `hazmat_paa_helmet`, `liquidator_helmet` | LIGHT, SAND |
| `schrabidium_helmet`, `euphemium_helmet` | FULL_PACKAGE (всё) |

Следствие было прямым: `ArmorRegistry.hasProtection(target, 3, HazardClass.LIGHT)` не находил
ничего, поэтому даже после восстановления проверки в AF1 (раздел выше) очки всё равно не спасали бы
от ослепления; та же история с SAND (песчаные бури) для всех шлемов. Добавлено вместе с константой
`FULL_PACKAGE` из оригинала.

**Осталось на будущее:** оригинал не регистрирует `gas_mask_mono` и `attachment_mask_mono` (в порту
такие предметы есть). Скопировано как есть, чтобы не расходиться с оригиналом, но выглядит его
собственным пропуском — стоит решить отдельно. Также не портирован механизм `ArmorUtil.external`
(регистрация классов защиты для предметов из других модов).

## AG. Воронка от взрыва отходов (`WasteBlastGenerator`)

Три дефекта из очереди T3/P3 проверены и исправлены — все три бьют по одной и той же модели
затенения, из-за чего анимированная воронка получалась совсем не такой, как мгновенная.

### AG1. Нормировка расстояния делилась сама на себя

`calculateSurvivalChance(resistance, distanceFromCenter, maxRadius)` считает
`normalizedDistance = distanceFromCenter / maxRadius`. Анимированный путь передавал третьим
аргументом `centerPos.distManhattan(pos)` — то есть расстояние того же самого блока, только в другой
метрике (значение в карте — округлённое **евклидово** расстояние). В результате для блоков на осях
отношение равнялось единице, и они считались стоящими ровно на краю взрыва, то есть с максимальным
шансом уцелеть; для диагональных выходило ~0.58. Спад по расстоянию фактически отсутствовал.
Теперь, как и в мгновенном варианте, передаётся `radius` — он протянут в метод параметром.

### AG2. Множество защищённых блоков пересоздавалось каждый тик

`Set<BlockPos> protectedBlocks` создавался **внутри** `processBlocksAnimated`, а метод вызывает сам
себя раз в тик через `TickTask`. То есть накопленное затенение сбрасывалось на каждом батче, и
`isBlockShielded`/`protectNearbyBlocks` работали только в пределах одного тика. Множество вынесено
наружу и протянуто через рекурсию рядом с `removedBlocks`.

### AG3. Сортировка шла от краёв к центру

Оба пути сортировали `Integer.compare(b.getValue(), a.getValue())`, то есть по **убыванию**
расстояния, при комментарии «от центра к краям» и при модели, где блок может быть прикрыт только
тем, что уже обработано ближе к эпицентру. Заменено на `Map.Entry.comparingByValue()`.

Остаток на будущее: в мгновенном пути ветка `if (protectedBlocks.contains(pos)) { if
(isBlockShielded(...)) { protectedBlocks.add(pos); ... } }` проверяет принадлежность множеству и
затем добавляет тот же элемент — выглядит как недописанная логика, но это уже не порт-ошибка
(у `WasteBlastGenerator` нет прямого прототипа в 1.7.10), поэтому не трогал.

## AH. Клиентские записи в SynchedEntityData и уточнение по червю

### AH1. `SpearEntity` и `VortexEntity` писали синхронизируемые данные на клиенте — ИСПРАВЛЕНО

`SynchedEntityData` авторитетна на сервере: запись на клиенте даёт значение, которое перетирается
следующей синхронизацией, а до неё расходится с сервером.

- `SpearEntity.tick` вызывал `entityData.set(TICKS_IN_GROUND, ...)` в обеих ветках без проверки
  стороны. Обе закрыты; ветка «копьё воткнулось» целиком переведена на `instanceof ServerLevel`,
  благо `discharge` там и так требовал сервер.
- `VortexEntity.tick` уменьшал размер и звал `discard()` на обеих сторонах — на клиенте вихрь
  пропадал локально до следующего пакета. Мутация и удаление закрыты серверной проверкой,
  `super.tick()` продолжает выполняться на обеих сторонах.

### AH2. Пункт очереди про `EntityWormBase` был неверен — СНЯТ

В плане значилось, что `EntityWormBase` «не сериализует `headID` вовсе». Это не так:
`addAdditionalSaveData` пишет `wormID` и `partID`, `readAdditionalSaveData` их читает.

Настоящая слабость другая и **та же, что в оригинале**: `headID` — это сетевой id сущности
(`level.getEntity(int)`), который не сохраняется между запусками. После перезагрузки мира
сохранённое число указывает в никуда или на постороннюю сущность. Чинится только переходом на UUID,
то есть это уже отход от оригинала, а не исправление порта.

## AI. `StructureFoundationProcessor` — диагноз подтверждён, правка отложена осознанно

**Подтверждено:** ванильный `StructureProcessor.processBlock` первым параметром принимает
`LevelReader` (проверено по `neoforge-21.1.248-sources.jar`), а порт объявляет `LevelAccessor`.
Это перегрузка, а не переопределение, поэтому метод не вызывается никогда. `@Override` на нём нет —
именно поэтому компилятор промолчал.

**Уточнение к прежней записи:** утверждение «ни в одном `processor_list` не упомянут» неверно —
`hbm_m:foundation_processor` указан как `processors` в 23 файлах `data/hbm_m/worldgen/`, то есть
структуры на него рассчитывают, и фундаменты под ними сейчас просто не строятся.

**Почему не исправлено сразу.** Наивное «поменять тип параметра и добавить `@Override`» приведёт к
двум проблемам, каждая из которых хуже текущего бездействия:

1. В теле стоит `(ServerLevel) level`. Во время генерации мира процессор получает `WorldGenRegion`,
   который `ServerLevel` **не является** — будет `ClassCastException` на генерации чанков.
2. `fillFoundationBelow` пишет блоки **вниз от структуры**, то есть потенциально в соседние чанки.
   Запись в `WorldGenRegion` за пределами текущей области генерации падает с «Detected setBlock in
   a far chunk».

То есть включение процессора — это не точечная правка, а перенос заливки фундамента в место, где
запись блоков легальна (например в `StructurePiece.postProcess` или отложенной задачей после
генерации). Требует проверки в мире, поэтому вынесено сюда, а не сделано вслепую.

## AJ. Сверка списка инициализации с оригиналом

Список вызовов `X.register()/init()` из `MainRegistry` порта сопоставлен с оригинальным. Так была
найдена дыра с `ArmorUtil.register()` (раздел AF4). Остальные расхождения разобраны:

- **`HazardRegistry.registerTrafos()`** в порту не вызывается — и **правильно**. Оригинал
  регистрирует три трансформера опасности; в порту все три класса существуют, но
  `HazardTransformerRadiationContainer` и `HazardTransformerRadiationME` — пустые заглушки с
  javadoc «wired when storage items / ME compat is ported», а `HazardTransformerRadiationNBT` читает
  ключ `hfrHazRadiation`, который **никто в порту не пишет**. Регистрировать нечего; когда появятся
  хранилища и AE2-совместимость, понадобится и список `trafos` в `HazardSystem`, и учёт того, что
  `HAZARD_CACHE` кэширует по `Item`, а трансформеры зависят от конкретного стека.
- `BedrockOre.init()` — в порту не нужен: `BedrockOreDensity` работает от enum и шума, без реестра.
- `DamageResistanceHandler.init()` → `initArmorStats()`, переименовано.
- `HbmPotion.init()` → `ModEffects.init()`, `Fluids.init()`/`FluidContainerRegistry.register()` →
  `ModFluidTraitsBootstrap.registerAll()`, `OreDictManager.*` → теги.
- `CellularDungeonFactory`, `BobmazonOfferFactory`, `LemegetonRecipes`, `MagicRecipes`,
  `LogicBlock*`, `MicroBlocksCompatHandler`, `CommandWikiRender` — контент/совместимость, которых в
  порту нет вовсе.

## AK. Генерация нефтяных залежей

Проверено по `neoforge-21.1.248-sources.jar`: шаг генерации `FEATURES` объявляет
`blockStateWriteRadius(1)`, то есть признак может писать блоки **только в свой чанк и восемь
соседних**; `WorldGenRegion.setBlock` вне этого радиуса молча возвращает `false`. Чтение при этом
разрешено до 8 чанков (зависимость `STRUCTURE_STARTS`), но чанки дальше первого кольца отдаются в
статусе `STRUCTURE_STARTS`, то есть без рельефа и без построенной карты высот.

### AK1. `place()` рапортовал успех, ничего не поставив — ИСПРАВЛЕНО

`placed++` считался независимо от результата `setBlock`, поэтому `place()` возвращал `true` даже
когда все блоки были отброшены радиусом записи. Теперь считаются только удавшиеся установки.

### AK2. Поверхностные пятна читали карту высот чужого чанка — ИСПРАВЛЕНО

`addSurfaceSpot` берёт смещение как `nextGaussian() * 7`, то есть примерно у 2–3 % из 60 попыток
модуль больше 16 блоков. Для таких позиций `level.getHeight(WORLD_SURFACE_WG, ...)` спрашивает
чанк, который ещё не прошёл карверы, и возвращает бессмысленную высоту, после чего `setBlock` всё
равно отбрасывается. Смещения зажаты в ±15.

### AK3. Осталось: крупные залежи обрезаются радиусом записи

`OilDepositFeature.sand(...)` создаётся с `minSize = 16, maxSize = 48`, то есть радиус до 47 блоков
при разрешённых 16 от центра чанка. Всё, что дальше, не ставится вообще — залежь получается
обрезанной по прямоугольнику вокруг чанка происхождения, причём асимметрично, в зависимости от того,
где внутри чанка оказался origin. В 1.7.10 такого ограничения не было (`MapGenBubble` писал в мир
напрямую).

Это не точечная правка: нужно либо уменьшить радиус до совместимого с ванильным шагом генерации,
либо раскладывать залежь несколькими признаками по соседним чанкам. Требует проверки в мире.

## AL. Оверлей противогаза и семантика чёрного списка маски

### AL1. Оверлей брал ёмкость фильтра у маски — ИСПРАВЛЕНО

`OverlayGasMask.render` вычислял стадию замутнения так:

```java
int max = maskStack.getItem() instanceof ItemGasMaskFilter f ? f.maxFilterDamage : DEFAULT_MAX_DAMAGE;
```

`maskStack` — это **маска**, она никогда не `ItemGasMaskFilter`, поэтому `max` всегда был
дефолтным. Оба тултипа (`ArmorGasMaskItem` и `ItemModGasmask`) резолвят ёмкость правильно — через
`getFilterItem(getFilterId(stack))`. Приведено к тому же виду.

Сейчас видимого эффекта нет: все пять фильтров создаются одноаргументным конструктором и имеют
ровно `DEFAULT_MAX_DAMAGE`. Ошибка проявится в тот момент, когда появится фильтр с другой ёмкостью.

### AL2. Чёрный список маски вето́ит больше, чем в оригинале — в очередь

Оригинальный `ArmorRegistry.getProtectionFromItem` вычитает чёрный список маски **только из
классов фильтра**:

```java
prot.addAll(hazardClasses.get(item));            // собственные классы маски — без фильтрации
...
filProt.removeAll(mask.getBlacklist(...));       // вычитание только здесь
prot.addAll(filProt);
```

В порту проверка стоит раньше и обрывает весь метод:

```java
if (mask.getBlacklist().contains(clazz)) return false;
```

Отличия три, и все сейчас латентные: (1) собственные классы маски тоже вето́ятся; (2) досрочный
`return` пропускает рекурсию по модификациям брони, где защита могла найтись; (3) в оригинале
блокировка действует только на конкретную маску, а не на весь предмет.

Наблюдаемой разницы сегодня нет: ни у одной маски её собственные зарегистрированные классы не
пересекаются с её же чёрным списком (`gas_mask` — SAND/LIGHT против GAS_BLISTERING, у моно-масок
собственных классов нет вовсе). Поэтому не трогал: правка меняет структуру метода, а проверить её
нечем до появления такого пересечения.

### AL3. Сверено, расхождений нет

- Все семь газовых блоков проверяют защиту головы нужного класса; хлор совпадает с оригиналом
  вплоть до набора эффектов и длительностей (слепота 5 с, отравление 20 с ур. 2, иссушение 1 с ур. 1,
  замедление 30 с ур. 1, усталость 30 с ур. 2). Оригинал зовёт `hasAllProtection` с одним классом,
  что тождественно `hasProtection`.
- Список типов опасностей в порту покрывает все девять из оригинала.

## AM. Данные живых сущностей (`HbmLivingProps`)

### AM1. Дигамма росла без предела — ИСПРАВЛЕНО

Оригинальный `incrementDigamma` зажимает результат в `[0, 10]`, порт не ограничивал вообще. Всё,
что считается от дигаммы, ехало вместе с ней: тултип `ItemDigamma` показывает `1000/digamma` как
период полураспада, а `HazardTypeDigamma` каждый тик добавляет `level / 20`. Добавлен кламп и
константа `maxDigamma`, плюс ранний выход на клиенте — как в оригинальном `setDigamma`.

### AM2. Проверено, расхождений нет

`maxAsbestos` (60 мин в тиках) и `maxBlackLung` (120 мин) совпадают с оригиналом. `livingTag`
возвращает **живой** вложенный тег: `CompoundTag.getCompound` отдаёт сам объект, а не копию, так
что записи в него сохраняются (в отличие от истории с `pryMods` в разделе Y1). Порог смерти
(значение достигло максимума → 1000 урона в обход брони, счётчик обнуляется) и обнуление совпадают.

### AM3. Не портирован эффект дигаммы на здоровье — в очередь

Оригинальный `setDigamma` вешает на сущность модификатор максимального здоровья
`pow(0.5, digamma) - 1`, то есть каждая единица дигаммы **вдвое** режет максимум здоровья. В порту
такой связи нет нигде: `getDigamma` не читается ни одним потребителем, кроме тултипа и самого
`HbmLivingProps`. То есть дигамма сейчас накапливается и показывается, но ничего не делает.

Это не ошибка портирования, а непортированная механика: нужен модификатор атрибута с сохранением
между сессиями и снятием при лечении. Отдельная задача.

## AN. Паровая турбина против таблицы трейтов — в очередь

Порт содержит полную цепочку `FT_Coolable` из оригинала
(`ModFluidTraitsBootstrap:372-381`):

| Пар | Во что | Расход → выход | Энергия |
|---|---|---|---|
| STEAM | SPENTSTEAM | 100 → 1 | 200 |
| HOTSTEAM | STEAM | 1 → 10 | 2 |
| SUPERHOTSTEAM | HOTSTEAM | 1 → 10 | 18 |
| ULTRAHOTSTEAM | SUPERHOTSTEAM | 1 → 10 | 120 |

`MachineBoilerBlockEntity`, `MachineHeatexBlockEntity` и `MachineHephaestusBlockEntity` этой
таблицей пользуются (`step.amountReq` / `step.amountProduced`). А
`MachineSteamTurbineBlockEntity` — **нет**: он хардкодит `ENERGY_PER_MB` 80/160/320/640 и
превращает пар любого сорта в spent steam **один к одному**, тогда как оригинальный
`TileEntityMachineTurbine` берёт `amountReq`/`amountProduced` из трейта.

Практическая разница: в замкнутом контуре объём теплоносителя не сходится с тем, что рассчитывают
котёл и теплообменник, а ступенчатое охлаждение (ультра → супер → горячий → обычный) заменено
одношаговым сбросом в spent steam.

Не правил: это не потерянная строчка, а другая модель турбины, и её замена меняет баланс всех
паровых сборок сразу. Нужно решение владельца и проверка в мире.

## AO. Тридцать три машины уничтожали содержимое при разрушении — ИСПРАВЛЕНО

Скан «блок с инвентарным BE, у которого при снятии блока никто не высыпает содержимое» дал
33 блока. Проверено по оригиналу — там это делается всегда:

- `BlockDummyable.breakBlock` (база почти всех машин) обходит `ISidedInventory` тайла и выбрасывает
  каждый стек с разбросом по случайным координатам;
- `BlockCraneBase.breakBlock` делает ровно то же для всего семейства кранов.

В порту механизм есть — `BaseMachineBlockEntity.dropInventoryContents()`, — но применялся выборочно:
часть блоков зовёт его из `onRemove`, часть переопределяет `onRemove` только ради разрушения
мультиблока, а часть не переопределяет вовсе. Лут-таблицы содержимое не сохраняют: у всех
пострадавших это обычный дроп самого блока.

Пострадавшие: всё семейство кранов (боксер, разбоксер, экстрактор, инсертер, граббер, роутер),
дроновые ящик/док/провайдер/реквестер, сварщик, бридер, кристаллизатор, циклотрон, фрекинговая
вышка, миксер, силекс, паяльная станция, турбина, ЗИРНОКС, тигель, радар, менеджер спутников,
хранилища (mass storage, storage drum, waste drum), топка, кузница ключей, RTG-печь, сирена,
большой пилон, батарея, счётчик радио-факела, пусковая «Союза».

Сделано: общий хелпер `com.hbm_m.block.MachineDrops.dropInventory(level, pos)`, вызываемый из
`onRemove` каждого из них. Он идёт через `dropInventoryContents()` там, где BE наследует
`BaseMachineBlockEntity`, и через item-capability (`ItemHandlerAccess`) для двух машин на
`BaseHbmBlockEntity`, где инвентарь приватный и называется по-разному. Окно переноса контрапшена
уважается в обеих ветках — иначе высыпание во время сборки Sable дало бы дюп.

Попутно: `dropInventoryContents` писал в лог на INFO при подавлении высыпания — понижено до `debug`
и переведено на английский, как остальные диагностические строки из раздела Y2.

## AP. Сохранение NBT и выдача результата крафта

### AP1. Три машины теряли инвентарь и энергию при перезагрузке — ИСПРАВЛЕНО

`MachineIndustrialGeneratorBlockEntity`, `MachineLargeTurbineBlockEntity` и
`PWRControllerBlockEntity` переопределяют `writeNbtData`/`readNbtData` **без вызова `super`**.
Инвентарь, `energy`, `capacity`, `lastEnergy` и `energyDelta` пишет именно
`BaseMachineBlockEntity`, поэтому у этих трёх они не сохранялись вовсе: после перезахода машина
возвращалась пустой и разряженной.

Скан по всем BE нашёл 31 такой пропуск `super`, но остальные 28 наследуют `BaseHbmBlockEntity`,
где `writeNbtData` — пустая заглушка (в javadoc прямо сказано «переопределяется дочерними
классами»), так что там пропуск безвреден. Затронуты были ровно три класса на
`BaseMachineBlockEntity`.

### AP2. Продвинутый сборщик съедал ингредиенты и не выдавал результат — ИСПРАВЛЕНО

`MachineModuleAdvancedAssembler.processCraft` сначала списывает ингредиенты, а затем пишет
результат через `itemHandler.insertItem(outputSlots[0], result, false)` **без учёта остатка**.

`ItemStackHandler.insertItem` проверяет `isItemValid`, а `BaseMachineBlockEntity` заворачивает эту
проверку в `isItemValidForSlot`, который у продвинутого сборщика для выходных слотов возвращает
**`false`** (чтобы воронки не пихали туда предметы). То есть вставка отвергалась целиком, остаток
игнорировался — ингредиенты исчезали, результат не появлялся.

Внутренняя запись обязана обходить фильтр слота, как это делает `MachineModuleBase.OutputPlacement`
(через `setStackInSlot`). Приведено к тому же виду; `canProcess` при этом уже проверяет, что
результат помещается.

Проверено, что случай единственный: два других модуля (`MachineModuleChemplant`,
`MachineModuleChemFactoryLane`) выдают результат через `placeAllItemOutputs`, а у
`MachineOreSlopperBlockEntity` выходные слоты принимают ровно свой предмет, так что `insertItem`
там проходит. В `MachineAssemblerBlockEntity:601` остаток тоже безопасен — вставка предварительно
симулируется, и наружу извлекается ровно столько, сколько поместилось.

## AQ. Два явных пункта из очереди закрыты

### AQ1. Армированный бетон не держал взрыв — ИСПРАВЛЕНО

`BlockExplosionDefense.isSpecialConcreteBlock` перечисляет восемь блоков усиленного бетона
(`CONCRETE_SUPER`, четыре маркированных варианта, `CONCRETE_SUPER_BROKEN`, `CONCRETE_REBAR`,
`CONCRETE_REBAR_ALT`) и по javadoc должен давать им защиту 400 — но метод не вызывался **ниоткуда**:
ни из `getBlockDefenseValue`, ни из `getDefenseValueForBlock`, ни из `isModularBlock`. Списки
`isConcreteBlock` и `isSpecialConcreteBlock` не пересекаются, так что армированный бетон уходил в
общую ветку и получал ванильную взрывоустойчивость вместо 400 — то есть держал ядерный взрыв
заметно хуже обычного бетона (250).

Проверка добавлена во все три места. Порядок не важен: множества блоков не пересекаются.

### AQ2. Пять клавиш крана РБМК не регистрировались — ИСПРАВЛЕНО

`ModConfigKeybindHandler.registerAll` (путь Forge/NeoForge) регистрировал 5 биндов из 10, тогда как
fabric-ветка через `KeyMappingRegistry` регистрирует все 10. Пять клавиш управления краном РБМК
(вверх/вниз/влево/вправо/загрузка) не появлялись в настройках управления, и `consumeClick` по ним
не срабатывал. Добавлены.

### AQ3. Восемнадцать полей конфига не существовали для файла — ИСПРАВЛЕНО

Диагноз подтверждён и уточнён. `HbmConfigStore` пишет JSON как
`ConfigSchema.snapshotForJson(cfg, side)` и читает через `ConfigSchema.applyAll` — то есть схема не
просто описывает GUI, она **единственный** мост между объектом конфига и файлом. Поле, не
зарегистрированное в схеме, в файл не попадает и из файла не читается: оно навсегда остаётся на
значении, зашитом в `ModClothConfig`.

Пострадали 18 полей, из них 15 — весь набор дайлов РБМК (`passiveCooling`, `passiveCoolingInner`,
`columnHeatFlow`, `fuelDiffusionMod`, `heatProvision`, `boilerHeatConsumption`, `controlSpeedMod`,
`reactivityMod`, `outgasserMod`, `surgeMod`, `reasimBoilerSpeed`, `moderatorEfficiency`,
`absorberEfficiency`, `reflectorEfficiency`, `absorberHeatConversion`). То есть реактор нельзя было
настроить вообще никак — ни через GUI, ни правкой файла. Плюс `netherAmbientRad`,
`basaltDeltasRadMult` и клиентский `enableDhRenderBridge`.

Все 18 зарегистрированы; границы взяты из `RBMKDials`, который клампит те же диапазоны. После
правки диф «поля `ModClothConfig` против ключей схемы» пуст.

### AQ4. Конфиг биомов кратера не действовал на ядерном пути — ИСПРАВЛЕНО

`enableCraterBiomes` проверялся только в `EntityFalloutRain`. Ядерный путь
(`NuclearExplosionHelper`, `NuclearScenarioLaunchers`, `NuclearExplosionAPI`) смотрел исключительно
на флаг команды `opt.biomes()`, а обычный подрыв идёт с `ExplosionCommandOptions.DEFAULT`, где этот
флаг `true`. То есть настройка сервера не работала вовсе: биомы переписывались всегда.

Проверка добавлена в сам аксессор `ExplosionCommandOptions.biomes()` — одна точка на все четыре
места вызова. Флаг команды по-прежнему может **выключить** смену биомов, но включить её вопреки
серверной настройке уже не может, как и в оригинале, где `WorldConfig.enableCraterBiomes`
проверяется внутри самой генерации.

### AQ5. Ни одну музыкальную пластинку нельзя было проиграть — ИСПРАВЛЕНО

На 1.21.1 ванильный `RecordItem` убран: воспроизведение задаётся компонентом `jukebox_playable`,
который ссылается на запись `JukeboxSong` в датапаке. `PlatformHooks.createRecordItem` на этой
версии возвращал `new Item(properties)` — без компонента, а записей `jukebox_song` в моде не было
вовсе. Все три пластинки (`bunker`, `glass`, `ch`) в проигрыватель не вставлялись, хотя тег
`minecraft:music_discs` для них генерируется.

Сделано:
- `data/hbm_m/jukebox_song/{bunker,glass,ch}.json` с `sound_event`, `description` (существующие
  ключи `item.hbm_m.music_disc_*.desc`), `length_in_seconds` и `comparator_output` 1/2/3 — теми же
  значениями, что были у регистраций;
- `PlatformHooks.jukeboxProperties(properties, songId)` вешает компонент на 1.21.1 и ничего не
  делает на 1.20.1; `createRecordItem` принимает `songId` и идёт через него, `MUSIC_DISC_CH`
  (у неё свой класс ради строки флейвора) оборачивает `Properties` тем же хелпером.

Попутно найдена и исправлена ошибка единиц: `MUSIC_DISC_BUNKER` передавала в параметр
`lengthInSeconds` значение `20 * 120`, а ветка 1.20.1 умножает его на 20 ещё раз — то есть на Forge
пластинка считалась сорокаминутной вместо двухминутной. Теперь везде секунды, как и называется
параметр.

## AR. Проверка на боевом сервере после серии правок

После 24 коммитов (разделы Y–AQ) собранный jar развёрнут на тестовый сервер
(192.168.1.20, Pelican + Wings, 209 модов вместе с Create, Sable, Create Aeronautics, AE2,
Ad Astra, Terralith):

- `Done (6.867s)`;
- ошибок и исключений `hbm_m` — **0**;
- несработавших миксинов — **0** (варнинги «@Mixin target … was not found» относятся к клиентским
  миксинам на выделенном сервере и присутствовали всегда);
- новые датапак-записи `jukebox_song` загрузились без ошибок парсинга;
- регистрация 18 новых полей `ConfigSchema` прошла без замечаний.

Затронуто было много живого кода — 33 блока получили `onRemove`, переписан вывод результата
продвинутого сборщика, добавлены записи в реестр защиты и в схему конфига, — поэтому чистый старт
на сборке такого размера важнее, чем прогон GameTest'ов.

## AS. Плоские иконки колонн РБМК — ИСПРАВЛЕНО

Дефект оказался двойным, и первая половина скрывала вторую:

1. `RBMKColumnBlockItem.initializeClient` (хук, который отдаёт предмету кастомный BEWLR) был
   объявлен только под `//? if forge {`, ветки neoforge не было — сравни `MissileItem`, где есть обе.
2. Сам `RBMKColumnItemRenderer` **целиком** закрыт `//? if forge {`, то есть на 1.21.1 класса не
   существовало вовсе. Это выяснилось только после добавления хука — компилятор не нашёл символ.

Платформенного кода внутри рендерера нет (только `Minecraft.getInstance()` и
`getBlockEntityRenderDispatcher()`), поэтому директива расширена до `//? if forge || neoforge {`,
как у соседнего `ItemRenderMissileGeneric`. Обязательный по правилу 5 из `CLAUDE.md` круг
переключения версий (`1.20.1-forge` → `1.21.1-neoforge`) выполнен, иначе тело осталось бы
закомментированным и правка ничего бы не дала.

## AT. Клиент, экраны и datagen — сверки без находок

Прогнаны по областям, которые до этого не читались (`client/render`, `client/model`,
`inventory/gui`, `datagen`). Находок нет, фиксирую результат, чтобы не повторять.

- **Разыменование клиентских синглтонов.** Все четыре обращения к
  `Minecraft.getInstance().player/level` вне клиентских пакетов закрыты проверками на null.
- **BlockEntity в экранах.** Шесть GUI обращаются к `menu.getBlockEntity()`; меню намеренно
  допускают отсутствующий тайл (в конструкторах подставляется `DummyItemStackHandler` с
  комментарием про реплей Flashback). Проверено построчно с учётом закомментированных TODO-блоков:
  каждый путь разыменования закрыт проверкой в начале своего метода. Незакрытых нет.
- **Баланс `pushPose`/`popPose`.** Девять файлов формально несбалансированы, но это шаблон
  `RBMKPanelRenderer.begin()`: базовый класс открывает общий трансформ, наследник закрывает его сам.
  Отдельно проверены ранние `return` между открытием и закрытием — два попадания оказались
  локальными `new PoseStack()` (утечка невозможна) и одно ложным (совпало имя метода `begin`).
- **Ссылки на текстуры из datagen.** 1051 уникальная ссылка сверена с файлами
  `assets/hbm_m/textures` и сгенерированными моделями. Неразрешённых 13, и все — либо ванильные
  родительские модели (`item/generated`, `block/stone`, `item/music_disc_13`), либо префиксы для
  конкатенации (`item/scraps_`, `block/rad_absorber_`, `item/wire_dense/wire_dense_`).
- **Целостность датапака.** Проверена не статикой, а логом боевого сервера: из 39 ошибок парсинга
  рецептов и тегов ни одна не относится к `hbm_m` (все — `tf_dnv`, `oritech`, `Tracks`).
  Загружено 16774 рецепта без единого замечания к моду.

## AU. Ревью собственных правок разделов Y–AT

Прогон независимого ревью по всему диффу (89 файлов, 26 коммитов) плюс собственная проверка.
Найдено шесть настоящих дефектов, внесённых мной, и один блокер, найденный по дороге.

### AU1. Сужение видимости `onRemove` ломало сборку на 1.20.1 — ИСПРАВЛЕНО

Шаблон, которым скрипт добавил `onRemove` в 20 блоков, объявлял метод `protected`. На 1.21.1
`BlockBehaviour.onRemove` действительно `protected`, а на **1.20.1 он `public`** — сужение видимости
при переопределении, то есть 20 классов не компилировались на Forge. Заменено на `public`, что
законно на обеих версиях и совпадает с блоками, у которых `onRemove` был изначально.

### AU2. Датаген не запускался вообще — ИСПРАВЛЕНО (дефект не мой)

Именно из-за этого AU1 и не был замечен: `:1.20.1-forge:compileJava` падал на
`SubLevelAssembleExpansionMixin`, который называет `BoundingBox3ic` в сигнатуре обработчика.
Companion-jar с этим классом собран под Java 21 и физически не может лежать в классpath 1.20.1
(отсюда и версионный guard в build-скрипте). Датаген падал вместе с компиляцией — поэтому
`src/generated` не пересобирался с 7 сентября, и **все** добавленные ключи локализации никуда не
попадали.

Тип параметра разветвлён по версиям: настоящий на 1.21.1, `Object` на 1.20.1, где миксин всё равно
не применяется (целевого класса нет). Датаген прогнан, языковые файлы пересобраны (5102 ключа),
21 добавленный ключ проверен в результате.

### AU3. Выброс инвентаря дублировал уже существующий — ИСПРАВЛЕНО

У `MachineRadarBlock` и `MachineCrucibleBlock` собственный выброс уже был — через
`Containers.dropItemStack`, а мой фильтр при сканировании искал только `dropInventoryContents`,
`drops()` и `dropContents`. У тигля дубликата не возникало (обе ветки используют `extractItem`),
у радара — только благодаря тому, что `dropInventoryContents` опустошает те же самые объекты
`ItemStack`, которые потом читает старый цикл. Такая безопасность держится на совпадении, поэтому
мои вызовы из обоих блоков убраны.

### AU4. У менеджера спутников выброс не работал вовсе — ИСПРАВЛЕНО

`MachineSatLinkerBlockEntity` наследует `BaseHbmBlockEntity`, держит инвентарь в приватном поле и,
в отличие от тигля, **не переопределял** `getItemHandler`. Значит `ModCapabilities` публиковал
`null`, запасная ветка `MachineDrops` ничего не находила и содержимое по-прежнему уничтожалось.
Добавлено переопределение — заодно блок стал доступен для воронок.

### AU5. Сохранённый прогресс мог достаться другому рецепту — ИСПРАВЛЕНО

Правка из AP1 («не терять прогресс при выгрузке чанка») открыла лазейку: `currentRecipe` не
сохраняется, поэтому на первом тике после загрузки `prev == null` и прогресс удерживался **даже
если теперь подходит другой, более короткий рецепт**. `maxProgress` тут же переписывался его
длительностью, и цикл овердрайва завершал до 64 крафтов за один тик при списании энергии за один.
Теперь прогресс удерживается только если длительность нового рецепта совпадает с той, против
которой он был сохранён.

### AU6. Боксер уничтожал коробку на недостижимом пути — ИСПРАВЛЕНО

`sendPackage` при отсутствии ленты делал `return`, а слоты к этому моменту уже очищены. Путь
недостижим (`serverTick` проверяет ленту заранее), но безопасный отказ — это выбросить предметы,
а не уничтожить. Заменено.

### AU7. Описание пластинки печаталось дважды — ИСПРАВЛЕНО

После подключения `jukebox_playable` ваниль сама рисует описание `JukeboxSong`, а
`FlavouredRecordItem.appendHoverText` продолжал добавлять строку `.desc` вручную. На 1.21.1 ручная
строка убрана, флейвор-строка осталась.

### AU8. Проверено ревью и оставлено как есть

- **Боковой доступ к доменной печи (AB4).** После правки все стороны идут через
  `DirectionalItemHandler`, а `sideUpper`/`sideLower`/`sideFuel` по умолчанию `UP`. В уже
  существующих мирах на NeoForge подача сбоку или снизу перестанет работать до перенастройки
  сторон в GUI. Это ровно то поведение, что и на Forge и на Fabric, но пользователей стоит
  предупредить в changelog — правка не «чинит невидимое», а меняет работающие сборки.
- **Снятие фильтра с надетой маски (Y1).** Ревью верно отмечает, что для маски-шлема старый путь
  дюпа не давал (там живой стек) и что `worn.commit()` сохранил бы обе возможности. Оставлено как
  есть: оригинальный `ItemModGasmask.onItemRightClick` работает со стеком **в руке**, и расхождение
  с ним было бы отходом от 1.7.10.
- `ItemHandlerAccess.getItemHandler` требует наличия BlockEntity, поэтому краны не увидят блоки,
  публикующие item-capability без тайла (ванильный компостер). В оригинале работа шла только с
  `IInventory`-тайлами, так что это паритет.
- Воронки взрыва отходов после AG станут заметно меньше и «пятнистее» — прямое следствие того, что
  затенение теперь работает как задумано. Нужен игровой прогон, чтобы решить, устраивает ли баланс.

## AV. Лопата: режим удачи существовал только на бумаге — ИСПРАВЛЕНО

`ModShovelItem` — усечённая копия `ModPickaxeItem`. Конструктор принимает `fortuneLevel`, поле
объявлено и клампится, `NBT_FORTUNE` объявлен, а дальше — ничего: ни в тултипе, ни в цикле режимов
(ПКМ), ни в `getToolForDrops`. Кирка тот же режим реализует полностью (`toggleFortune`,
`applyModeFortune`/`clearModeFortune`, `getEffectiveFortune`, `NBT_PRE_FORTUNE`).

Практическое следствие: **сплавная лопата** (`ModItems:273`, `new ModShovelItem(..., 3, 0, 2)`) —
единственная, где `fortuneLevel > 0`. У неё заявлена удача II, но включить её было нельзя, и в
тултипе строки про удачу не было вовсе. Цикл ПКМ на ней сводился к «жила → выкл».

**Исправление.** Режим зеркально перенесён с кирки: строка тултипа, `isFoil`, шаги цикла
(жила → шёлк → удача → выкл), сброс удачи в `disableAllAbilities` и во взаимоисключающих
переключателях, `applyModeFortune`/`clearModeFortune` c сохранением ванильного уровня в
`PreModeFortune`, учёт удачи в `getToolForDrops` (дроп жильной добычи). Ключи локализации
(`tooltip.hbm_m.fortune`, `message.hbm_m.fortune.*`) уже были — их использует кирка.

Топор (`ModAxeItem`) параметра удачи не имеет вовсе, у него расхождения нет.

## AW. Отклонение в РБМК-потоке нейтронов: где именно оседает радиация

Сверка `RBMKNeutronHandler.NeutronStream` с оригиналом
(`com/hbm/handler/neutron/RBMKNeutronHandler.java:281-291`).

Оригинал при попадании потока в не-колонну облучает **исходную** позицию потока:

```java
int hits = getHits(targetPos);
if(hits == columnHeight) return;
else if(hits > 0) { irradiateFromFlux(pos, hits); ... }   // pos — origin, не targetPos
else { irradiateFromFlux(pos, 0); }
```

Порт (`RBMKNeutronHandler.java:223-231`) облучает `targetPos` — ту клетку, через которую поток
прошёл. Число `hits` считается один раз и передаётся внутрь, лишнего прохода по колонне нет
(старое замечание O3 про второй `getHits` закрыто отдельной правкой с `level.isLoaded`).

Это **осознанное отклонение, а не ошибка**: физически радиация вдоль луча правдоподобнее, чем
куча у самой колонны. Но распределение фона вокруг любого РБМК отличается от оригинала, поэтому
пункт не «чинится» вслепую — решение по геймплею и проверка в мире. Оставлено как есть.

## AX. Жидкостная подсистема: сверка с оригиналом

Прошёл `api/fluids/**`, баки, загрузчики контейнеров и жидкостные машины (НПЗ, крекинг,
фракционирование, компрессор, электролизёр, радиолиз, насосы, бойлеры, буровые) против 1.7.10.

### AX1. Дюп жидкости на стыке с чужими модами — ИСПРАВЛЕНО

`ForgeFluidHandlerAdapter.useUpFluid` имел ветки `//? if forge` и `//? if fabric` и **не имел
neoforge-ветки** — на 1.21.1 метод был пустым. Все соседние методы того же класса
(`getFluidAvailable`, `getDemand`, `transferFluid`) neoforge-ветку имеют, то есть это пропуск,
а не решение.

Адаптер создаётся `FluidDuctBlockEntity.tick` для любого соседа-не-MK2, который отдаёт
`Capabilities.FluidHandler.BLOCK` — то есть для бака любого чужого мода рядом с трубой HBM.
`FluidNet.transferFluid` заполняет приёмники, а потом зовёт `provider.useUpFluid(...)`, который
ничего не делал: **источник не опустошался, жидкость размножалась каждый тик**. Контракт
`useUpFluid` в оригинале — `FluidNetMK2:114-137` (`scapegoat.useUpFluid(type, p, toUse)`).

Добавлена neoforge-ветка, зеркальная forge: `drain(SIMULATE)` → проверка вещества → `drain(EXECUTE)`.

### AX2. Фрекинговая башня уничтожала налитую канистру — ИСПРАВЛЕНО

`processContainerPair` сливал бак **до** того, как проверял, куда положить наполненный контейнер.
`FluidHooks.insertFluidIntoItem` работает с `stack.copy()`, поэтому наполненная канистра существует
только как `result.remainder()`; если выходной слот занят несовместимым предметом, ветки «положить»
не было — и `remainder` терялся вместе с уже слитой жидкостью. Пустая канистра при этом не тратилась,
так что цикл повторялся, пока бак на 128 000 мБ не опустеет.

Порядок исправлен на «проверить размещение → слить → потратить входную канистру» (тот же приём, что
в `FluidLoaderStandard:76-95`).

### AX3. Паровой насос не сохранял бак воды — ИСПРАВЛЕНО

`MachinePumpSteamBlockEntity.writeNbtData/readNbtData` не звали `super`, а именно `PumpBlockEntity`
хранит бак воды на 100 000 мБ, `isOn` и `onGround`. Соседний `MachinePumpElectricBlockEntity` зовёт.
Добавлены `super`-вызовы.

### AX4. Машины с нетипизированным входным баком не подключались к трубе — ИСПРАВЛЕНО

`trySubscribe(tank.getTankType(), ...)` при `Fluids.EMPTY` не находит узел: `canConnect` трубы
сравнивает вещества, а узла с ключом EMPTY не существует. При этом сам приём жидкости
нетипизированным баком разрешён (`IFluidStandardReceiverMK2.receiverTankMatches:28-30`), то есть
машину можно было накормить только **вплотную стоящим** отправителем, но не через трубу.

Бак без типа при старте — у 17 машин: фракционная колонна, каталитический реформер, гидроочистка,
вакуумная перегонка, компрессор, радиолиз, коксовая печь, пиролизная печь, большая турбина,
солидификатор, аннигилятор, молотилка, ротационная печь, факел, дымоход, автопила, PUREX.

Добавлена перегрузка `IFluidReceiverMK2.trySubscribe(FluidTank, ...)`: если у бака нет типа и он
пуст — тип берётся у самой трубы (`IFluidPipeMK2.getFluidType`) и проверяется через
`tank.isFluidValid`. Для типизированного бака поведение прежнее. Все 48 вызовов переведены на неё.

### AX5. Слот идентификатора жидкости ничего не делал у трёх машин — ИСПРАВЛЕНО

`MachineCompressorBlockEntity`, `MachineElectrolyserBlockEntity`, `MachineRadiolysisBlockEntity`
объявляли `SLOT_FLUID_ID`, принимали в него предмет и **никогда** не звали `FluidTank.setType`.
В оригинале это `tanks[0].setType(0, slots)` / `setType(3, 4, slots)` / `setType(10, 11, slots)`.
Вместе с AX4 это значило, что входной бак вообще нечем было типизировать.

Вызовы добавлены по образцу `MachineCokerBlockEntity:72` (`inventorySlotArray`/`applySlotsArray`).
Электролизёру вдобавок задан дефолтный тип входного бака `Fluids.WATER`, как в оригинале
(`TileEntityElectrolyser:88`).

### AX6. Компрессор без рецепта уничтожал 1000 мБ за цикл — ИСПРАВЛЕНО

`process(recipe == null)` сливал 1000 мБ из `tanks[0]` и лил `tanks[1].fillMb(tanks[1].getTankType(),
1000)`, а тип выходного бака никогда не задавался — `fillMb` с `Fluids.EMPTY` возвращает 0.
`setupOutputTank` правил только давление. В оригинале (`TileEntityMachineCompressorBase:167-175`)
`setupTanks` копирует и тип: без рецепта — тип входа, с рецептом — тип выхода рецепта. Сделано так же.

### AX7. Заливка модификаций брони писала в копию — ИСПРАВЛЕНО

`FluidLoaderFillableItem.fillOrEmptyArmor` мутировал массив из `pryMods` (стеки, декодированные из
NBT) и не писал его обратно: слив дал бы дюп, залив — потерю. Сегодня недостижимо (на NeoForge
`Capabilities.FluidHandler.ITEM` есть только у бочек, которые в броню не ставятся), но дефект тот же
класса, что и Y1. Добавлен `applyMod` после успешной операции.

### AX8. Проверено и чисто

`FluidNet`/`FluidNetProvider`/`FluidNode` (включая сохранённую особенность оригинала
`totalAvailable -= received[p]`), ядро `FluidTank`, `FluidLoaderStandard`, `FluidBarrelItem`,
`MachineFluidTankBlockEntity`, узлы труб и вентилей, NBT всех 82 держателей баков (кроме AX3).

### AX9. В очередь (нужна проверка в мире)

**Закрыто:** `FluidPumpBlockEntity` теперь помечает себя грязным, когда сеть меняет его буфер —
до этого 100 мБ могли потеряться при выгрузке чанка.

- `UniversalMachinePartBlockEntity.collectControllerFluidTypes` и `FluidDuctBlock.canConnectTo` имеют
  ветки forge/fabric без neoforge — но затронуты только контроллеры мультиблока, которые не
  `IFluidUserMK2` и не цистерна; такой в сборке не найден.
- `FluidPumpBlockEntity` не зовёт `setChanged()` после сетевых переносов (буфер 100 мБ).
- Расхождения баланса: таблица фракций НПЗ, порог взрыва бойлера, отсутствующий `changeTankSize`,
  горизонтальный скан бура.

## AY. Энергетическая подсистема: сверка с оригиналом

Прошёл `api/energy/**`, `UniNodespace`, кабели/трансформаторы/батареи/пилоны, генераторы и
энергоприём машин.

### AY1. Нижняя грань любой машины меняла 1 FE на 4 294 967 296 HE — ИСПРАВЛЕНО

`BaseMachineBlockEntity.getEnergyStorage(side)` отдавал `LongEnergyWrapper` в режиме `BitMode.HIGH`
для `Direction.DOWN` и `LOW` для остальных пяти сторон. В HIGH-режиме враппер работал со **старшими
32 битами** long: `receiveEnergy(N)` списывал у источника N FE и записывал `N << 32` HE, `extract`
симметрично уничтожал `N * 2^32` HE в обмен на N FE (и мимо лимита `maxExtract` машины, через
`setEnergyStored`).

Практически: любой FE-кабель под батареей (`machine_battery_schrabidium` 25 Г,
`machine_battery_dineutronium` 1 Т, `machine_fensu` `Long.MAX_VALUE`) — это бесконечная энергия
за один тик. Идея «упаковать long в две int-грани» задокументирована в `PackedEnergyCapabilityProvider`
как фича, но обмен между модами с двумя разными курсами на разных гранях одного блока защитить
нечем: в оригинале никакого масштабирования нет, `IEnergyReceiverMK2.transferPower` — это просто
`setPower(power + getPower())`.

Режим HIGH удалён целиком (и на Forge-ветке тоже, `PackedEnergyCapabilityProvider` теперь отдаёт
один и тот же враппер на все стороны). Заодно `getEnergyStored`/`getMaxEnergyStored` больше не
режут значение по младшим битам, а насыщаются на `Integer.MAX_VALUE` — до этого FEnSU показывал
чужим модам `-1`, а любая машина выше 2^31 HE — отрицательный заряд.

### AY2. Силовую броню нельзя было зарядить на NeoForge — ИСПРАВЛЕНО

Единственная регистрация энергокапабилити брони — `ModArmorFSBPowered.initCapabilities` — лежит в
`//? if forge`. NeoForge-замена `ModCapabilities.registerBatteryItemCaps` перебирает `ModItems.ITEMS`
и делает `continue` на всём, что не `ModBatteryItem`, а броня (`T51Armor`, `AJRArmor`, `AJROArmor`,
`DNTArmor`, `BismuthArmor` → `ModPowerArmorItem` → `ModArmorFSBPowered`) им не является.

Следствие: зарядный слот дровяной печи явно пускает силовую броню
(`MachineWoodBurnerMenu:71`), но `chargeItemInSlot` получал `null` на обеих капабилити и выходил —
броня лежала в слоте вечно. `isEnergyReceiverItem` тоже возвращал false, поэтому батарейные блоки
броню не принимали вовсе. Единственным способом зарядки оставался ПКМ по `HevBatteryBlock`.
Ветка `ItemEnergyStorage.isPoweredArmor()`, написанная ровно под этот случай, была недостижима.

Регистрация брони добавлена в `registerBatteryItemCaps` (провайдер, приёмник и FE-обёртка).

### AY3. Проводники не уничтожали свой узел при сносе — ИСПРАВЛЕНО

`PylonBaseBlockEntity` (все пилоны и оба коннектора), `RedCableGaugeBlockEntity` и
`RedCablePaintableBlockEntity` создают узел в тике и **не удаляли его никогда**. `WireBlockEntity`
и `SwitchBlockEntity` делают это в `setRemoved()`, то есть это пропуск. В оригинале узел рвётся в
`TileEntityCableBaseNT.invalidate()`, общем предке всех трёх.

`UniNodespace.updateNodespace` пересчитывает узел только при `!hasValidNet() || recentlyChanged`, а
сборщик убирает лишь `expired`-ссылки, причём `expired` ставит только `popNode`. Поэтому сеть
оставалась склеенной через снесённый блок, а поставленный на то же место проводник молча наследовал
призрачный узел.

Добавлен `setRemoved()` с `Nodespace.destroyNode` во все три (он же срабатывает при выгрузке чанка).
Отдельно `RedConnectorBlock` не имел `onRemove` — у пилонов он есть (`RedPylonCoreBlock:117`) и зовёт
`disconnectAll()`, чтобы порвать связи на **обоих** концах провода; коннектору такой же добавлен.

### AY4. FEnSU дюпился при сносе — ИСПРАВЛЕНО

`build.neoforge.gradle.kts` удаляет лут-таблицы батарей (на 1.21.1 состояние переносит код через
`playerWillDestroy`), но фильтрует по `name.startsWith("machine_battery")`. `machine_fensu`
зарегистрирован тем же `registerBattery`, но называется иначе — его таблица уезжала в сборку и
срабатывала **вдобавок** к `playerWillDestroy`. То есть FEnSU при сносе давал две штуки, вместе с
запасённой энергией. Фильтр расширен.

### AY5. Тултип заряда батареи всегда показывал ноль — ИСПРАВЛЕНО

BE пишет `energy` (long, с маленькой буквы), а `MachineBatteryBlock.appendHoverText` читал `Energy` —
ключ из старой `copy_nbt`-таблицы, которого в NBT нет. `MachineBatteryBlockItem` читал `Energy`, а
в запасном варианте `getInt("energy")` — то есть обрезал long до 32 бит. Оба читателя переведены на
`energy` как long с сохранением `Energy` для старых сохранений.

### AY6. `setupFluidCapability()` не вызывался на NeoForge — закрыто (латентная дыра)

Единственный вызов стоял в `onLoad()`, а `onLoad` целиком лежит в `//? if forge` (на NeoForge у
`BlockEntity` нет `getCapability`/`invalidateCaps`, и весь блок был закомментирован вместе с
безобидным `onLoad`). То есть `fluidHandlerNeo` всегда оставался `null`.

**Проверил каждое из 23 переопределений** — на активной neoforge-ветке живых тел нет ни одного:
у 19 машин весь метод внутри `//? if forge`, а у четырёх «живых» (`MachineAdvancedAssembler`,
`MachineSteamCondenser`, `MachineGasCentrifuge`, база) тело либо пустое, либо целиком в
forge-ветке. Значит **сегодня это ничего не ломает**: все эти обработчики (`CombinedChemPlantFluidHandler`,
`UnifiedFluidHandler`, `RefineryFluidHandler`, …) — Forge-only, на `LazyOptional`, и на NeoForge
жидкость и так идёт через `NeoForgeFluidHandlerMK2` из `BaseHbmBlockEntity`.

Тем не менее вызов восстановлен (`//? if neoforge` вариант `onLoad`): иначе следующая машина,
которая позовёт `setFluidHandler` из neoforge-кода, молча останется без капабилити — ровно та
ловушка, что уже описана в комментарии `MachineCoreInjectorBlockEntity:59-62`.

### AY7. Проверено и чисто

`PowerNet` (точный порт `PowerNetMK2.update`, включая накопление `toTransfer` между приоритетами и
цикл добора остатков), `UniNodespace`/`NodeNet`/`GenNode`, `trySubscribe`/`tryProvide`,
`EnergySubscriptions` (константы отката 20/10/1 = `EnumTransferAction`), энергогейт
`MachineModuleBase`, все генераторы (создание/уничтожение энергии не найдено), `ConverterBlockEntity`
(1:1 HE↔FE с лимитом), узлы `WireBlockEntity`/`SwitchBlockEntity`.

### AY8. В очередь (решение по балансу)

Скорости батарей: порт держит плоские `TRANSFER_RATE = 100 Г/т` в обе стороны на всех тирах, тогда
как оригинал даёт `maxPower/200` на приём и `maxPower/600` на выдачу
(`TileEntityMachineBattery:259-268`). Тултип предмета показывает **оригинальные** числа
(`MachineBatteryBlockItem:34-35`), а `MachineCapacitorBlockEntity:329-330` формулу оригинала уже
реализует — то есть в коде она есть, батарея её просто не использует. Батарея сейчас не ограничивает
сеть вовсе; правка меняет баланс, поэтому нужна отдельная команда.

## AZ. Проверка на боевом сервере после правок AX/AY

Сборка `:1.21.1-neoforge:build -x test`, jar разложен в `mods/`, контейнер перезапущен.

```
Done (3.282s)! For help, type "help"
```

Ноль строк `hbm_m` с ERROR/Exception, ноль сбоев применения миксинов. Дополнительно:
`:1.20.1-forge:compileJava` после круга переключения версий — чисто (12 warning'ов, все про
unchecked-операции), `runGameTestServer` — `All 287 required tests passed`.

## BA. Оружие, бомбы и ракеты: сверка с оригиналом

### BA1. Детонаторы не подрывали девять из одиннадцати бомб — ИСПРАВЛЕНО

Порт разделил единый контракт оригинала `IBomb` на два: заряды реализуют `IDetonatable`, бомбы —
`IBomb`. Все четыре точки подрыва (`DetonatorItem`, `MultiDetonatorItem`, `RangeDetonatorItem`,
`DetonateAllPacket`) проверяли **только** `IDetonatable`, который есть лишь у Fat Man, «Прототипа»
и двух пусковых площадок. `LargeNukeBlock` (Gadget/Little Boy/Ivy Mike/Царь), `NukeCustomBlock`,
`NukeFleijaBlock`, `NukeSoliniumBlock`, `NukeN2Block`, `NukeFstbmbBlock`, `BombMultiBlock` и
`LandmineBlock` подрывались только редстоуном. Оригинал:
`if(world.getBlock(x,y,z) instanceof IBomb) ((IBomb)…).explode(...)` (`ItemDetonator:75-78`).

Добавлен `com.hbm_m.api.bomb.BombDetonation`: `isTriggerable` + `trigger` (сначала `IDetonatable`,
затем `IBomb.explode(...).wasSuccessful()`). Все четыре точки переведены на него. Цепную детонацию
зарядов **намеренно не расширял** до `IBomb` — в оригинале радиусной цепочки нет вовсе, и C4 не
должен поджигать соседнюю ядерную бомбу.

### BA2. Цепная детонация подрывала уже снесённые блоки — ИСПРАВЛЕНО (краш)

Девять мест (`C4Block`, `DetMinerBlock`, `ExplosiveChargeBlock`, `SmokeBombBlock`,
`WasteChargeBlock`, `AirBombProjectileEntity`, `AirNukeBombProjectileEntity`,
`NuclearScenarioLaunchers`, `NuclearExplosionHelper`) планировали `onDetonate` через `TickTask`
с задержкой по расстоянию и **захватывали устаревший `BlockState`**, не перепроверяя блок. Каждый
заряд планирует каждого соседа, поэтому один и тот же заряд подрывался несколько раз, а для
авиабомбы это краш: `AirBombBlock.onDetonate` читает `getBlockState(pos).getValue(FACING)` — на
втором вызове там воздух, `IllegalArgumentException` в `MinecraftServer.pollTask`.

Все девять переведены на `BombDetonation.triggerDetonatableLater(level, pos, expectedBlock, …)`,
который перечитывает состояние и молча выходит, если блок сменился.

### BA3. Детонирующий майнер дюпал добычу — ИСПРАВЛЕНО

`DetMinerBlock.onDetonate` сносил радиус **до** того, как убирал сам майнер. Каждый
`setBlockAndUpdate` шлёт neighbour-update на позицию майнера, тот всё ещё стоял и всё ещё видел
редстоун-сигнал → `neighborChanged` заходил в `onDetonate` повторно и собирал дропы оставшихся
блоков ещё раз. Порядок исправлен на оригинальный: сначала убрать майнер, потом сносить радиус.

### BA4. Бомба возвращала свою начинку при подрыве — ИСПРАВЛЕНО

`NukeBaseBlock.explode` (и то же в `LargeNukeBlock`, `NukeCustomBlock`, `NukeFatManBlock`,
`NukePrototypeBlock`) звал `Containers.dropContents` **перед** `clearContent()`, то есть подрыв
выкидывал ядро, взрывчатку и топливо обратно игроку. Оригинал (`NukeGadget:179-182`) очищает слоты
и убирает блок, ничего не роняя. `NukeFleijaBlock`/`NukeFstbmbBlock` приходили к тому же через
`setBlock(AIR)` без очистки — там теперь `clearContent()` перед сносом.

### BA5. Самолёты авиаудара не исчезали никогда — ИСПРАВЛЕНО

Ветка `else if (bombsDropped >= TOTAL_BOMBS)` не была защищена `!hasFinishedAttack`, поэтому после
сброса боезапаса она срабатывала каждый тик и обнуляла `despawnTimer`; счётчик никогда не доходил
до `DESPAWN_DELAY`. Самолёт летел по прямой вечно, каждые ~3 с играл звук бомбардировщика и
сохранялся в регион-файл. Правка в трёх классах (`AirstrikeEntity`, `AirstrikeHeavyEntity`,
`AirstrikeNukeEntity`).

### BA6. Два самолёта создавались с чужим `EntityType` — ИСПРАВЛЕНО

`AirstrikeHeavyEntity` и `AirstrikeAgentEntity` звали `super(ModEntities.AIRSTRIKE_ENTITY.get(), …)`.
Типа `airstrikeheavy` не существовало вовсе, а зарегистрированный `AIRSTRIKE_AGENT_ENTITY` не
использовался. После перезахода в мир тяжёлый бомбардировщик пересоздавался фабрикой обычного
(4 авиабомбы → 10 случайных гранат), а распылитель Agent Orange начинал сбрасывать бомбы.

Добавлен `AIRSTRIKE_HEAVY_ENTITY`, оба класса переведены на свои типы, `AirstrikeEntityRenderer`
сделан generic по сущности (он использует только `getYRot`) и зарегистрирован для обоих — раньше
у распылителя стоял `EmptyEntityRenderer`.

### BA7. Кнопка «Start» балефайр-бомбы работала только на клиенте — ИСПРАВЛЕНО

`GUINukeFstbmb` звал `be.startCountdown()` прямо из лямбды кнопки: на сервере `started` оставался
`false`, `serverTick` выходил сразу, бомба не взрывалась никогда. Добавлен C2S
`NukeFstbmbControlPacket` (по образцу `SoyuzLauncherControlPacket`) плюс синхронизация таймера на
клиент раз в секунду — иначе в GUI не было бы и отсчёта.

### BA8. «Союз» стартовал с пустым баком окислителя — ИСПРАВЛЕНО

`canLaunch()` проверял только `hasFuel()` (керосин), а `liftOff()` безусловно списывал
`tanks[1].drainMb(fuelReq)`. Метод `hasOxy()` существовал и питал только лампочку в GUI.
В оригинале `canLaunch` требует `hasAllFuel() = hasJetFuel() && hasOxidizer()`.

### BA9. Снаряды турелей рисовались чужой иконкой — ИСПРАВЛЕНО

`TurretBulletEntity`/`TurretRocketEntity` ставили `ICON_ITEM_ID` в `create()`, но
`ThrowableItemProjectile` заполняет синхронизированный стек из `getDefaultItem()` ещё в
конструкторе — то есть до присвоения. Добавлен `setItem(...)`, как в `GrenadeIfProjectileEntity`.

### BA10. Мелочи — ИСПРАВЛЕНО

- `launchToEntity` не присваивал `tracking` перехватчику ABM — ракета игнорировала цель, выбранную
  оператором радара, и через 40 тиков сама захватывала ближайшую (оригинал присваивает).

- `DudFugasBlock`/`DudNukeBlock`: `appendHoverText` объявлен со старой сигнатурой `BlockGetter` без
  `@Override` — на 1.21.1 не переопределял ничего, подсказки не выводились. Обе версии разведены
  директивами, как у `GigaDetBlock`.
- `TurretBaseBlockEntity.fritzFuelTicks` не сохранялся в NBT — огнемётная турель после перезагрузки
  тратила лишнюю канистру.
- `AirBombProjectileEntity.dealExplosionDamage` — мёртвая заглушка: собирала сущности в AABB и
  ничего с ними не делала. Урон и так наносят два `serverLevel.explode` следом, так что заглушка
  и константа `DAMAGE_RADIUS` просто удалены (агент ошибочно счёл, что урона нет вовсе).
- Подсказка о взрывоустойчивости блоков жила только в `//? if forge` — добавлен NeoForge-слушатель
  `ItemTooltipEvent`, регистрируется из `ClientSetup`.

### BA11. Проверено, отклонения нет

`LandMineBlockEntity.waitingForPlayer` нигде не выставляется в `true` — **но и в оригинале тоже**
(`TileEntityLandmine` только читает поле; его выставляет генерация структур через NBT самой мины).
Не дефект порта.

### BA12. В очередь

- Ржавая пусковая шахта нефункциональна: слот 0 объявлен только на выход (`mayPlace == false`),
  а база читает ракету именно из него; механики `launch code`/`launch key` нет вовсе, оба предмета
  не упоминаются в игровом коде. Это отсутствующая фича, а не точечная ошибка.
- Топливная система пусковой площадки закомментирована (`hasFuel()` возвращает true по одной
  энергии, списание топлива отключено), при этом шкала GUI считает по бакам и показывает «пусто».
  Нужно решение: доделать или убрать шкалу.
- `BuildMissilePacket` не проверяет доступ к станции сборки (общая проблема с `blockEntityAt`).
- `TomBlastEntity` потерял защиту `!flag` вокруг урона и грома.

## BB. Генерация мира и фоллаут: сверка с оригиналом

### BB1. Вся коренная руда в мире была первого тира — ИСПРАВЛЕНО

`BedrockOreDensity.valueNoise` — одна октава, значение в `[-1, 1]`. Формула оригинала
`|level * type| * 0.05` при этом не могла превысить **0.05**, поэтому `getTier` всегда возвращал 1,
`getBoreFluid` — `EMPTY`, а все ветки выше 0.05 (тиры 2-4, вода/серная кислота/растворитель,
1000/2000 мБ) были недостижимы. Последствия: буровая установка никогда не требовала жидкости —
три её тира были мёртвым контентом; сканер плотности везде печатал «Very Poor»; шлам-машина
накапливала по ≤0.05 за руду вместо ~1.0, то есть на один предмет уходило под сотню руд.

В оригинале это `NoiseGeneratorPerlin(seed, 4)`, где `func_151601_a` складывает 4 октавы как
`d2 += noise(x*d3, z*d3) / d3; d3 /= 2` — амплитуды 1/2/4/8 при падающей частоте, диапазон ≈ ±15.

Добавлен такой же октавный сумматор. Проверил распределение на 78 548 точках сетки 40000×40000:
средняя плотность 0.52, тир 1 — 74.4 %, тир 2 — 12.4 %, тир 3 — 11.5 %, тир 4 — 1.7 %, зажатие
в потолок 2.0 — 9 точек из 78 548.

### BB2. Одиннадцать элементов пулов метеоритного данжа указывали в никуда — ИСПРАВЛЕНО

`meteor_10room.json` (8 из 8), `meteor_default.json` (2 записи весом 3 и 4) и `meteor_roomback.json`
(1 из 1) ссылались на `hbm_m:meteor/room-*`, тогда как файлы лежат в
`data/hbm_m/structures/meteor/room10/`. `StructureTemplateManager.getOrCreate` на отсутствующий id
возвращает **пустой** шаблон без ошибки в логе, поэтому данж молча генерировался с пустотами на
месте комнат. Пути исправлены на `hbm_m:meteor/room10/room-*`.

### BB3. Урановые слитки не фонили — ИСПРАВЛЕНО

`HazardRegistry` спрашивает теги `forge:ingots/uranium` и `forge:ingots/sodium`, но
`build.neoforge.gradle.kts` переносит сгенерированные `data/forge/tags/**` в `data/c/tags/**`
(на 1.21 конвенциональные теги живут в `c:`), а `forge:`-тегов на NeoForge не существует вовсе.
`registerItems()` при этом не имеет ветки `case URANIUM`, а `frame()` пропускает `INGOT`, так что
обычный урановый слиток не давал **никакой** радиации. Введена константа `CONVENTIONAL_NS`
(`c` на 1.21+, `forge` ниже), оба тега берут её.

### BB4. `biomes:false` у команды взрыва не действовал — ИСПРАВЛЕНО

`EntityNukeExplosionMK5.applyCraterBiomes` записывался (из `NuclearExplosionAPI`) и сохранялся в
NBT, но **не читался нигде**: биомы меняет `EntityFalloutRain`, который смотрел только на конфиг
`enableCraterBiomes`. Флаг проброшен в `EntityFalloutRain` (с NBT и дефолтом `true` для старых
сущностей) и учитывается во всех точках смены биома.

### BB5. Правила фоллаута сравнивали `BlockState` по ссылке — ИСПРАВЛЕНО

`FalloutEntry.eval` проверял `state != matchesBlockState`, а все 20 правил записаны через
`X.defaultBlockState()`. Для блоков со свойствами это никогда не совпадает: у выросшего гриба все
шесть граней `true` только в дефолтном состоянии, у травы — `snowy`, у снега — `layers`. То есть
огромные грибы, заснеженная трава и снег толще одного слоя фоллаутом не затрагивались. Оригинал
сравнивал по **блоку** (`matchesMeta = -1` — «любое состояние»); сделано так же.

### BB6. Тултип сырой коренной руды всегда показывал нули — ИСПРАВЛЕНО

`ItemBedrockOreBase.appendHbmTooltip` не смотрел на стек и печатал `typeLine(type, 0)` шесть раз,
хотя буровая пишет в NBT реальные плотности по шести ключам (их же читает шлам-машина). Оригинал
берёт значения из стека. Тултип теперь читает NBT, для стека без тега остаётся 0.

### BB7. Диапазон высоты морской мины был пустым — ИСПРАВЛЕНО

`navalmine.json`: `min_inclusive = {absolute: 3}`, `max_inclusive = {above_bottom: 64}`, что в
Оверворлде разрешается в `y = 0`, то есть min > max. `UniformHeight.sample` в таком случае пишет
в лог `Empty height range` и возвращает min. Заменено на `constant: 3` — поведение в мире то же,
предупреждение уходит. Настоящий разброс глубины — вопрос геймплея, в очереди.

### BB8. Проверено, отклонения нет

Привязка биом-модификаторов и фич (58 placed features, все ссылки резолвятся), remap
`forge:add_features → neoforge:add_features`, JSON биомов кратера, целостность 54 структур и их
наборов, `StructureLootProcessor` (20 лут-таблиц), `ContaminationUtil.contaminate`,
`HbmLivingProps`, `EntityEffectHandler`, NBT и тикеты `EntityFalloutRain`.
Отдельно: перекос весов в `chooseRandomOutcome` (`r -= w; if (r <= 0)`) унаследован из оригинала
дословно — не отклонение порта.

### BB9. В очередь

- `StructureFoundationProcessor.processBlock` объявлен с `LevelAccessor` — это перегрузка, а не
  переопределение (`StructureProcessor` принимает `LevelReader`), плюс сам процессор не включён ни
  в один `processor_list` (в `foundation_processor.json` только loot и connection_fix). Починка не
  точечная: на 1.21 процессоры структур read-only, и заливку фундамента надо делать иначе.
- `hbm_m:oilclaster_surrounded` — зарегистрированная фича, на которую нет ни одного
  `configured_feature`, а её `place()` вообще ничего не ставит (тело закомментировано).
- `sand_oil_deposit` с радиусом до 48 блоков выходит за `blockStateWriteRadius(1)` шага FEATURES —
  залежь молча обрезается; нужен пересмотр радиуса или переход на другой шаг.
- Девять `structure_set` имеют `salt` больше `Integer.MAX_VALUE` (обрезается при декоде).
- Мёртвые дубликаты `util/CraterGenerator`, `util/CraterBiomeApplier`, `util/BlockExplosionDefense`
  (~816 строк) — живые копии лежат в `util/explosions/nuclear/`.

## BC. Проверка на боевом сервере после правок BA/BB

```
Done (3.304s)! For help, type "help"
```

Ноль строк `hbm_m` с ERROR/Exception, ноль сбоев миксинов, ноль предупреждений `Empty height range`
(правка BB7 подтверждена в рантайме). До этого: `:1.20.1-forge:compileJava` после круга переключения
версий — чисто (12 warning'ов, deprecated/unchecked), `runGameTestServer` — 287/287.

## BD. Мобы, боссы и эффекты: сверка с оригиналом

### BD1. Блок порчи ронял сервер при первом же сохранении — ИСПРАВЛЕНО

`BlockTaint.entityInside` вешал эффект через `new MobEffectInstance(Holder.direct(ModEffects.TAINT.get()), …)`.
`Holder.Direct` — незарегистрированный holder: `MobEffect.CODEC` идёт через `Registry.safeCastToReference`,
и `MobEffectInstance.save()` бросает «Unregistered holder». Этот вызов делает
`LivingEntity.addAdditionalSaveData`, то есть автосейв, выгрузка чанка, выход игрока или `/stop`
после контакта с порчей роняли сервер с крешрепортом. Плюс эффект не находился ни `hasEffect`,
ни `removeEffect`, а иконка в HUD резолвилась в «missing texture».

Правильный приём в проекте уже был — `PlatformHooks.addEffect` кастит `RegistrySupplier`, который
сам является `Holder.Reference`. Переведено на него. Заодно тот же `Holder.direct` убран у зачарования
удачи в `BlockProcessorStandard` (там ещё и `Optional.orElse(null).value()` — NPE, если зачарования нет).

### BD2. Ядерный крипер взрывался как обычный — ИСПРАВЛЕНО

`CreeperMixin` импортирует `EntityCreeperNuclear`, но ветки для него не имел: `nuclearExplode()`
(мьюк-частица, звук, `EntityNukeExplosionMK5` у заряженного, `ExplosionNukeSmall` у обычного,
радиация вокруг) не вызывался ниоткуда. Крипер спавнится яйцом и через `EntityEffectHandler`,
то есть путь живой. Ветка добавлена.

### BD3. Таймер MaskMan и метка RAD-зверей стирались при смерти — ИСПРАВЛЕНО

`BossSpawnHandler` писал `hbm_maskManTimer`/`hbm_radMark` в корень `getPersistentData()`, а
`ServerPlayer.restoreFrom` переносит через респавн **только** подтег `PlayerPersisted`. Игрок,
умирающий чаще раза в 20 минут, MaskMan-а не увидел бы никогда. Оригинал читает именно
`getCompoundTag(PERSISTED_NBT_TAG)`. Хранилище перенесено в этот подтег.

### BD4. Ни один моб мода не спавнился естественно — ИСПРАВЛЕНО

`ModEntityEvents` регистрирует `RegisterSpawnPlacementsEvent` для трёх криперов, но `SpawnPlacement` —
только фильтр: кандидатов спавнер берёт из `MobSpawnSettings` биома, а ни один biome-модификатор
записей спавна не добавлял (в сборке только `add_*_ores`, `add_sand_oil`, `add_*_features`).
Фосгеновый, нестабильный и золотой криперы существовали лишь в виде яиц, а сами правила спавна
(порт `getCanSpawnHere`) были инертны.

Добавлен `add_spawns` (вес 5/10/1, `#minecraft:is_overworld`, как `EntityMappings.addSpawn` в
оригинале) и ремап типа `forge:add_spawns` в `neoforge:add_spawns` в `build.neoforge.gradle.kts`
рядом с существующим для `add_features`.

### BD5. Проверено, отклонения нет

`ArmorModificationServerEvents` не существует как тип на 1.21.1 (весь класс в forge/fabric-ветках),
но кламп здоровья теперь делает сам `LivingEntity.onAttributeUpdated` — класс просто устарел.
Атрибуты зарегистрированы для всех живых типов, рендереры — для всех одиннадцати, `MobGearHandler`
доходит до `FinalizeSpawnEvent`, NBT `EntityWormBase` корректен, боссбары не текут.

### BD6. В очередь

- Лидер RAD-зверя недостижим: `markForRadBeasts` не вызывается ниоткуда, `createLeader` — тоже,
  а яйцо спавнит обычного. Значит `BOSS_MELTDOWN` и дроп `COIN_RADIATION` невыдаваемы. Рейд
  сознательно спящий (комментарий в классе), но про недостижимость лидера там не сказано.
- `EntityCreeperGold` считает дроп по `looting`, который на 1.21 всегда 0 (та же дыра, что
  задокументирована у `EntityRADBeast`) — нужен переход на `LootContext`.
- `PlayerHandler` сбрасывает накопленные RAD в NBT только в `PLAYER_QUIT`: чистый выход и `/stop`
  безопасны, жёсткий креш теряет дозу за сессию.

## BE. Крафт, рецепты и хранение: сверка с оригиналом

### BE1. Наковальня выдавала бесплатный результат — ИСПРАВЛЕНО (дюп)

`updateCrafting()` пишет результат рецепта в слот 2 **ничего не списывая** — оплата происходит
только в `AnvilMenu.SmithingOutputSlot.onTake`. При этом слот 2 сохранялся в NBT, дропался из
`drops()` при сносе и был доступен автоматике: `getItemHandler(side)` отдавал сырой хендлер, а
любая вставка в слоты 0/1 через капабилити пересчитывала выход. То есть труба плюс слом блока — дюп.
GUI-путь был безопасен только потому, что `AnvilMenu.removed()` чистит слот 2 при закрытии.

Слот 2 больше не попадает в NBT (превью восстанавливается при открытии меню), не дропается,
а капабилити не отдаётся вовсе — в оригинале у наковальни нет block entity, инвентарь живёт в
контейнере. Побочный эффект: автоматическая подача в наковальню больше не работает (её и не было
в оригинале).

### BE2. Три подсистемы ассемблера были мертвы на NeoForge — ИСПРАВЛЕНО

`pullIngredientsForOneCraft`, `pushOutputToNeighbors` и `chargeFromEnergySlot` имели только ветки
`//? if forge` и `//? if fabric`. Следствия на 1.21.1: ассемблер не забирал ингредиенты из соседних
инвентарей, не отдавал результат (слот забивался и машина вставала), и обычная батарея в слоте
энергии не передавала ничего.

Первые две переписаны как общие `//? if forge || neoforge` через `ItemHandlerAccess` (тот же приём
с `var`, что применён для кранов), третья делегирована в `BaseMachineBlockEntity.chargeFromBatterySlot`.

### BE3. Порты мультиблока ассемблера теряли ролевое ограничение — ИСПРАВЛЕНО

`getItemHandlerForPart(role)` (вставка только во входные слоты, извлечение только из выходного)
существовал лишь на Forge; neoforge-ветка `UniversalMachinePartBlockEntity` отдавала сырой хендлер
контроллера. Труба на порту ITEM_INPUT могла вытащить шаблон, батарею и готовый продукт, а на
ITEM_OUTPUT — засунуть что угодно во входные слоты. Добавлены neoforge-прокси на `RangedWrapper`.

### BE4. Ящики: четыре дефекта — ИСПРАВЛЕНО

- **Не отдавали item-капабилити вообще** (в коде так и написано: «Forge item handler capabilities
  removed for Fabric compilation»): воронки, трубы, AE2 и `ItemHandlerAccess` видели пустоту.
  В оригинале `TileEntityCrateBase` — `ISidedInventory`, открытый со всех сторон.
- **Любое неигровое разрушение уничтожало ящик вместе с содержимым**: лут-таблица пустая
  (`dropEmptyTable`), а упаковка в предмет делалась только в `playerWillDestroy`. TNT, ядерный взрыв,
  крипер или карьер стирали до 104 стеков. Теперь `onRemove` роняет содержимое россыпью — ровно как
  `BlockStorageCrate.breakBlock` в оригинале. Сам ящик при этом не дропается: это осознанное правило
  порта, закреплённое тестом `nonPlayerDestructionDoesNotDropPortableCrate` (защита от дюпа).
  Игровой путь дополнительно чистит block entity, чтобы два пути не сработали разом.
- **Структурный ящик, добытый до первого открытия, терял лут**: содержимое материализуется при
  открытии, а `isEmpty()` смотрел только на слоты и возвращал true. Теперь ожидающая лут-таблица
  тоже считается непустой.
- **Подсказка содержимого была мертва**: писалось в `BLOCK_ENTITY_DATA`, читалось из `CUSTOM_DATA`
  по ключу `BlockEntityTag`, которого никто не пишет.

Плюс: содержимое ящиков больше не уезжает в чанковый пакет (`getUpdateTag` был унаследован и
сериализовал все слоты каждому клиенту в радиусе прорисовки).

### BE5. Ещё две машины потеряли item-капабилити тем же способом — ИСПРАВЛЕНО

`MachineFluidTankBlockEntity` (слоты идентификатора жидкости и канистр) и `RBMKHeaterBlockEntity`
(слот и посторонняя раздача жидкости: приём снизу, выдача с остальных сторон) отдавали их только из
forge-ветки `getCapability`. Добавлены полиморфные `getItemHandler`/`getFluidHandler`.

### BE6. Пресс боеприпасов уменьшал общий `ItemStack.EMPTY` — ИСПРАВЛЕНО

`for (i < GRID_SIZE) inventory.getStackInSlot(i).shrink(1)` на пустой ячейке уменьшает счётчик
синглтона `ItemStack.EMPTY` (рецепты оставляют 6-8 ячеек пустыми). Спасало только то, что
`isEmpty()` для синглтона проверяет ссылку. Добавлена проверка.

### BE7. Проверено, отклонения нет

Все 42 сериализатора рецептов (сверка порядка чтения и записи), наличие JSON у всех
зарегистрированных типов, резолв всех 622 ссылок на жидкости, пресс, э-пресс и конвейерный пресс,
шредер (построчно совпадает с оригиналом), химзавод и химфабрика (учёт `count`), разборка ТВС РБМК,
станция пайки, дуговая печь, центрифуга, литейный ковш и формы, NBT-цикл ящиков и все шесть
`quickMoveStack`.

### BE8. В очередь

- Пул чертежей: недостижим в выживании (`writeBlueprintPool` зовётся только из креатив-вкладки, JEI
  и тестов; лут даёт папку без NBT) и одновременно необязателен — `requiresBlueprint()` не проверяется
  нигде вне JEI, а `GiveTemplateC2SPacket` выдаёт шаблон за бумагу и краситель без валидации.
  В оригинале проверку делает сам модуль машины. Это отсутствующая подсистема, а не точечная ошибка.
- Наполненный стальной ящик принимается как ингредиент в `crate_desh` и `rbmk_storage` — крафт
  съедает содержимое. На 1.21 обычный ингредиент игнорирует компоненты; нужен свой ингредиент.
- У ящиков нет выхода компаратора (в оригинале был) и `requiresCorrectToolForDrops` не проверяется
  при упаковке.
- `MachineAssemblerBlockEntity.craftItem` не прерывается, если ингредиент не найден: проверка идёт
  через `StackedContents` (по id), а списание — через `Ingredient.test`. Для ванильных ингредиентов
  эквивалентно, для `ICustomIngredient` — нет.
- Тигель не сбрасывает прогресс при подмене входа; `ItemCastMold.MoldType.getCostMb` возвращает 0
  для дефолтной ветки (сегодня недостижимо, все 252 рецепта используют формы с ненулевой ценой).

## BF. Проверка на боевом сервере после правок BD/BE

```
Done (3.347s)! For help, type "help"
```

Ноль строк `hbm_m` с ERROR/Exception, ноль сбоев миксинов. В собранном jar лежит
`data/hbm_m/neoforge/biome_modifier/add_spawns.json` с типом `neoforge:add_spawns` — ремап
из `forge:add_spawns` отработал, датапак принят без ошибок.

Перед этим: `:1.20.1-forge:compileJava` после круга переключения версий — чисто (12 warning'ов),
`runGameTestServer` — `All 287 required tests passed`.

Отдельно: первая версия правки ящиков роняла тест `nonPlayerDestructionDoesNotDropPortableCrate`.
Тест кодирует осознанное правило порта «сам ящик при неигровом разрушении не дропается» (защита от
дюпа), поэтому подход изменён на дроп содержимого россыпью — как `BlockStorageCrate.breakBlock`
в оригинале. Тест снова зелёный.

## BG. Дубли рецептов ассемблера — ИСПРАВЛЕНО

Три пары рецептов ассемблера имели одинаковый выход и разные ингредиенты, а
`getRecipeFromTemplate` выбирает `findFirst()` по выходу из `RecipeManager`, то есть победителя
определял порядок в `HashMap`: в GUI выбора показывались две записи, крафтилась произвольная, JEI
показывал не те ингредиенты, что списывались.

| выход | порт 1.7.10 | придуманный дубль |
|---|---|---|
| `exposure_chamber` | `exposurechamber`, 200 тиков / 100 HE | `exposure_chamber`, 150 / 80 |
| `strand_caster` | `strandcaster`, 200 / 100 | `strand_caster`, 200 / 120 |
| `fat_man_core` | `mancore`, 1200 / 100 | `man_core`, 160 / 250 |

В оригинале (`AssemblyMachineRecipes:400, 514, 715`) каждого рецепта ровно по одному, и параметры
`setup(...)` совпадают именно с портированными вариантами. Дубли и были помечены в генераторе
комментарием «no ass.* recipe existed in the original 1.7.10 either; invented here» — удалены они,
портированные оставлены.

## BH. Реакторы (кроме РБМК): сверка с оригиналом

### BH1. Watz уничтожал стек гранул при выгорании — ИСПРАВЛЕНО

`WatzPelletItem` стекается до 16, слот РБМК-подобного реактора хранил их пачкой, но вся пачка имеет
**один** NBT-тег выработки: 16 гранул горели как одна (16-кратная плата за тот же выход), а при
обнулении выработки слот заменялся на **один** истощённый предмет — 15 гранул исчезали.
В оригинале это закрыто на уровне инвентаря: `TileEntityWatz.getInventoryStackLimit()` возвращает 1.

Сделано так же: `getSlotLimit` хендлера равен 1, и слоты GUI переопределяют `getMaxStackSize`
(контейнер-обёртка по умолчанию сообщает 99).

### BH2. Гранулы не превращались в истощённые, пока реактор выключен — ИСПРАВЛЕНО

`updateReaction` выходил до цикла истощения; в оригинале цикл идёт вне ветки «включён»
(`TileEntityWatz:269-283`). Гранула, обнулившаяся на последнем тике перед выключением, оставалась
«свежей» с нулевой выработкой.

### BH3. ROR-интерфейс PWR переворачивал стержни — ИСПРАВЛЕНО

`provideRORValue("rods")` возвращает `100 - rodLevel`, а `runRORFunction("setrods")` ставил
значение как есть. Оригинал инвертирует обе стороны (`rodTarget = 100 - percent`). Любой контур
«прочитать rods и записать обратно» переставлял стержни на противоположный конец; GUI при этом
использует неинвертированное соглашение, то есть GUI и ROR понимали «rods» по-разному.

### BH4. Конфигурация сторон ZIRNOX и Watz не переживала перезагрузку — ИСПРАВЛЕНО

`allowedFluidSides` и `fluidSidesFromMultiblockStructure` выставляются только из
`MultiblockStructureHelper.applyControllerSideConfig` (постановка или пересборка структуры) и не
писались в NBT. После рестарта сервера обе машины «раскрывались»: сознательно закрытая грань
контроллера снова принимала и отдавала жидкость. Сохранение сделано маской, как в
`MachineFluidTankBlockEntity`.

### BH5. PWR слал всю структуру каждый тик — ИСПРАВЛЕНО

`writeNbtData` пишет списки `ports`/`rods` (реактор до 4096 блоков), а `sendUpdateToClient()`
вызывался безусловно каждый тик — то есть сотни `BlockPos` уходили каждому смотрящему клиенту
20 раз в секунду. В оригинале структура серверная, а на клиент идёт `networkPackNT(150)` с
десятком скаляров.

Списки исключены из `getUpdateTag`, а сам синк проредён до раза в 5 тиков.

### BH6. Проверено, отклонения нет

Капабилити реакторов на NeoForge не мертвы (жидкости раздаёт `ModCapabilities.register` +
`BaseHbmBlockEntity.getFluidHandler` для любого `IFluidUserMK2`), все три управляющих пакета
зарегистрированы, `PWRPartBlockEntity.kind` намеренно не сохраняется (восстанавливается из
состояния блока), сборка структуры PWR и математика связей — точный порт, у ZIRNOX сходятся все 24
маски соседей, `decay()`, теги стержней, давление и условие расплавления, у исследовательского
реактора — граф потока и таблица отходов, селекционный реактор чист.
`MachineZirnoxDestroyedBlockEntity` даже исправляет ошибку оригинала (там пишется `onFire`,
а читается `fire`).

### BH7. В очередь (решения по балансу и объёму работ)

- **Слот выбора теплоносителя PWR не портирован.** Баки жёстко приведены к `COOLANT`, поэтому
  `FT_PWRModerator` (мьюг, тяжёлая вода, свинец, ториевая соль) и шесть из семи теплоносителей —
  мёртвый код, а множитель замедлителя всегда 1.0. В оригинале это слот 2 с
  `IItemFluidIdentifier` и `setupTanks()`. Инструменты в порту есть (`FluidIdentifierItem`,
  `FluidTank.setType`), не хватает слота, GUI и логики — это отдельная задача.
- **Watz: конверсия теплоносителя примерно в 150 раз дешевле оригинала.** Константа
  `COOLANT_HEAT_PER_MB = 2.0` против `step.heatReq = 300` у `FT_Heatable`. Обоснование в javadoc
  («такой системы трейтов в порту нет») сегодня неверно — трейт есть и PWR им уже пользуется.
  Но сам класс Watz задокументирован как осознанно упрощённый порт (5 архетипов гранул вместо 12,
  упрощённое расплавление, другое управление), поэтому правка меняет баланс целой машины и
  требует решения владельца.
- **ZIRNOX: множитель пара 5.0 против 7.5 в актуальном оригинале** (там комментарий «was 5
  originally»), плюс другие размеры баков. Похоже на сознательный выбор старых значений.
- **Исследовательский реактор после аварии остаётся навсегда «сломанным» блоком** (липкий флаг
  `exploded`), а топливо дропается, а не уничтожается. В оригинале блок сносится и заменяется
  колонной `deco_steel`/`corium`. Обычно самовзрыв 18F и так убирает блок.
- **PWR: выход BFB-топлива 250 млн против 1 млрд.** В оригинале конструктор `EnumPWRFuel`
  **теряет** параметр `yield`, то есть у него всегда 1 млрд — порт «исправил» ошибку оригинала.
  Отклонение осознанное, но зафиксировать стоит.

## BI. Мультиблоки и сетевой слой: сверка

### BI1. Вся интеграция с Create/Sable была мертва — ИСПРАВЛЕНО

`IMultiblockController` реализуют **80 классов блоков и ноль block entity**, а
`MultiblockExpander.resolveControllerPos` и `getAllMultiblockPositions` проверяли
`level.getBlockEntity(pos) instanceof IMultiblockController` — всегда `false`. Значит
`getAllMultiblockPositions` всегда возвращал `null`, и все четыре вызывающих миксина
(`SubLevelAssembleExpansionMixin`, `SuperGlueSelectionHelperMixin`, `ContraptionMixin`,
`HoneyGlueSelectionExpansionMixin`) уходили в `continue`. При склейке или сборке машины на
контрапшен уезжал только контроллер, а части оставались фантомами — ровно то, что эти миксины и
должны предотвращать. Та же ошибка была в `ContraptionMixin:94` и `HoneyGlueSelectionExpansionMixin:90`.

Зеркальная ошибка в `LevelChunkSilentRemovalMixin`: `IMultiblockPart` реализует **block entity**,
а проверялся `Block` — половина защиты от фантомных частей не работала (дюпа не было только
потому, что `UniversalMachinePartBlock.onRemove` несёт собственную проверку
`ContraptionAssemblyGuard.isMoving()`).

Все проверки приведены к правильному приёмнику: контроллер — по блоку, часть — по block entity.

### BI2. `SuperGlueSelectionHelperMixin` не мог применить расширение — ИСПРАВЛЕНО

`cir.setReturnValue(...)` в `@Inject` без `cancellable = true` бросает `CancellationException`,
которую `catch (Throwable)` превращал в предупреждение. Даже после BI1 расширение не сработало бы
ни разу. Единственный `setReturnValue` в пакете без `cancellable`.

### BI3. Меню станции пайки и радиолиза были пусты на NeoForge — ИСПРАВЛЕНО

Все 11 слотов станции пайки и оба слота радиолиза объявлены внутри `//? if forge {` без
neoforge-ветки (использовался forge-класс `SlotItemHandler`). На 1.21.1 меню состояло только из 36
слотов игрока, а `quickMoveStack` продолжал перекладывать в индексы 0-10 — то есть **по инвентарю
самого игрока**; станция была неработоспособна. Переведены на `ModItemStackHandlerContainer` +
ванильный `Slot` — приём, который уже используется в других меню порта.

### BI4. Меню тигля падало без block entity — ИСПРАВЛЕНО

При отсутствующем BE девять слотов машины не добавляются, но `quickMoveStack` продолжал считать
диапазоны с ними: `moveItemStackTo(stack, 9, 45, …)` на меню из 36 слотов бросало
`IndexOutOfBoundsException`. Добавлена ранняя проверка.

### BI5. Пакет точек детонатора принимал любой индекс — ИСПРАВЛЕНО

`SyncPointPacket` проверял только `pointIndex < 0`, а цикл добивки списка аллоцирует по
`CompoundTag` на каждую недостающую запись — произвольный индекс от клиента означал произвольный
объём аллокаций. Ограничено `MultiDetonatorItem.MAX_POINTS`, как во всех остальных путях детонатора.

### BI6. В очередь

- **Проверки доступа у C2S-пакетов.** `blockEntityAt` намеренно без проверки дистанции
  (в javadoc объяснено: саблевелы Sable/Create ведут block entity далеко от игрока), и это
  единственный guard у ~20 пакетов. В оригинале эквивалентный путь закрыт
  `IControlReceiver.hasPermission(player)` (`NBTControlPacket:81-89`). Проверки есть только у
  `RBMKBoilerPacket` (400), рецептурных пакетов и двери (64), а также у пакетов наковальни (они
  сверяют открытое меню — правильный образец). Нужен разбор по списку: какие пакеты — действия из
  GUI (им нужна дистанция или проверка открытого меню), а какие обслуживают автоматику.
  Свой пакет балефайр-бомбы я уже закрыл проверкой дистанции.
- **Диапазоны `quickMoveStack`.** Семь меню держат выходной слот внутри диапазона «игрок → машина»
  (ванильная ветка слияния не спрашивает `mayPlace`), у `MachineWoodBurnerMenu` константы слотов
  посчитаны для другого порядка добавления, у `MachineZirnoxMenu` вода вставляется в выход CO2,
  у `MachineFurnaceSteelMenu` диапазон накрывает выход. Ошибки не дюпят, но перекладывают предметы
  не туда; правка требует сверки каждой раскладки с её GUI.
- **`relinkOrphanedPart` сканирует 49³ позиций** без проверки загрузки чанка, до 60 попыток на
  часть; `SubLevelRelinkMixin` при этом инжектится **до** загрузки NBT block entity, то есть его
  работа затирается — рабочий путь уже есть в `UniversalMachinePartBlockEntity.pendingRelinkCheck`.
- **NaN проходит все клампы** (`Math.min/max` его пропускают): `RBMKControlPacket`/`PWRControlPacket`
  могут записать `Double.NaN` в NBT и заморозить стержень.
- **`destroyStructure` сносит части по ожидаемым позициям без проверки `getControllerPos()`** —
  при пересечении футпринтов ломает чужую машину.
- **Программная установка контроллера (`/setblock`, датапак, другой мод) затирает блоки футпринта
  без дропа** — `checkPlacement` есть только на пути предмета.
- **`UniversalMachinePartBlock.getOcclusionShape` на NeoForge всегда `Shapes.empty()`**, ветка
  1.20.1 учитывает `isFullBlockInGrid` — недокументированное расхождение.

## BJ. Проверка на боевом сервере после правок BG/BH/BI

```
Done (3.195s)! For help, type "help"
```

Ноль строк `hbm_m` с ERROR/Exception, ноль сбоев миксинов, ноль ошибок разбора рецептов (после
удаления трёх дублей ассемблера и перегенерации датагена — 10 967 файлов).

Перед этим: `:1.20.1-forge:compileJava` после круга переключения версий — чисто (12 warning'ов),
`runGameTestServer` — `All 287 required tests passed`.

## BK. Раскладки меню, NaN и мультиблочные мелочи — ИСПРАВЛЕНО

Разбор пунктов из очереди BI6, которые не требуют решений по балансу.

### BK1. Дровяная печь: константы слотов для другого порядка добавления

Слоты машины добавляются **первыми** (0-2), затем инвентарь игрока, но константы были записаны как
`PLAYER_INV_START = 0, FUEL_SLOT = 36`. То есть `FUEL_SLOT` указывал на 6-й слот хотбара: топливо по
shift-клику уезжало в инвентарь игрока, а shift-клик по хотбару 6-8 уходил в ветку машины с
диапазоном `[0,36)`, куда попадают и настоящие слоты машины. Константы приведены к фактическому
порядку, обмен «инвентарь ↔ хотбар» переписан в терминах этого меню.

### BK2. ZIRNOX: вода вставлялась в выход CO2

Порядок добавления слотов — CO2-вход, CO2-выход, вода-вход, вода-выход, а block entity нумерует их
CO2-вход, вода-вход, CO2-выход, вода-выход. Код передавал в `moveItemStackTo` **хендлерную**
константу `SLOT_WATER_IN = 25`, что в меню соответствует CO2-выходу (`TakeOnlySlot`): воду нельзя
было закинуть shift-кликом вовсе, а ванильная ветка слияния могла сложить предметы в выход.
Введены отдельные константы индексов меню.

### BK3. Стальная печь: диапазон накрывал выход

Линии чередуются (вход, выход, вход, выход, вход, выход, топливо, топливо), поэтому хендлерный
диапазон `[SLOT_INPUT_0, SLOT_INPUT_0 + 3)` в меню — это вход линии 0, **выход линии 0** и вход
линии 1: руда сливалась в выход, а вход линии 2 был недостижим. Добавлен перебор входов по линиям.

### BK4. NaN от клиента больше не проходит клампы

`Math.min/max` пропускают NaN, а любое сравнение с NaN ложно — стержень замирал навсегда, и
значение уезжало в NBT. Закрыты `RBMKControlBlockEntity.setTarget`, четыре параметра автоматики в
`RBMKControlPacket` и `PWRControllerBlockEntity.setRodTarget`.

### BK5. `destroyStructure` сносил чужие части

Сносились все `UniversalMachinePartBlock` на ожидаемых позициях без проверки владельца, хотя
пересечение футпринтов — состояние, которое код сам обрабатывает при постановке (самоуничтожение
контроллера). Теперь часть с чужим `getControllerPos()` пропускается.

### BK6. `getOcclusionShape` на NeoForge всегда пустой

Ветка 1.20.1 учитывает контрапшен-мир, двери/переходные шлюзы и `isFullBlockInGrid`, а
neoforge-вариант безусловно возвращал `Shapes.empty()` — `isFullBlockInGrid` был мёртвым кодом, и
сплошные части мультиблока не перекрывали грани соседей. Логика перенесена один в один.

### BK7. Выходные слоты внутри диапазона «игрок → машина» — ИСПРАВЛЕНО

Ванильный `moveItemStackTo` **не спрашивает `mayPlace`**, когда складывает предмет в уже занятый
слот с таким же предметом, поэтому take-only слот защищён только пока он пуст. Диапазоны сужены:

- `BlastFurnaceMenu` — вставка теперь только в топливо и два входа.
- `MachinePressMenu` — топливо, штамп, материал.
- `MachineEPressMenu` — батарея, штамп, материал, плюс отдельным шагом слот апгрейда (он идёт
  после выхода, поэтому одним диапазоном не покрывается).
- `MachineDerrickMenu` — газовый выход убран из `[3,5)`.
- `MachineAssemblerMenu` — приоритетные перемещения были верны, но fallback «весь диапазон машины»
  накрывал выходной слот; заменён на три слота апгрейдов.
- `LaunchPadLargeMenu`, `LaunchPadRustedMenu` — входы и выходы чередуются, поэтому вместо диапазона
  сделан перебор слотов с проверкой `mayPlace`.

**Отклонено (проверил сам):** у `MachineRefineryMenu` и `MachineFrackingTowerMenu` цепочка
начинается со слота батареи, но это не значит, что туда уедет любой предмет:
`ModItemStackHandlerContainer.canPlaceItem` делегирует в `isItemValid` машины, а ветка вставки в
пустой слот `mayPlace` спрашивает. Правка не нужна.

### BK8. Перепривязка осиротевших частей — ИСПРАВЛЕНО

- `relinkOrphanedPart` сканирует куб 49³ = 117 649 позиций через `Level.getBlockState`, который
  **загружает чанк**, в который попал. Скан повторяется до 60 раз на часть, а после разборки
  контрапшена осиротевших частей бывают сотни. Загруженные колонки чанков теперь определяются один
  раз, остальные позиции пропускаются — тот же приём уже применён в `attemptAutoRepair`.
- `SubLevelRelinkMixin` делал этот скан впустую: он перехватывает `LevelChunk.setBlockState`, то
  есть срабатывает **до** загрузки NBT block entity, поэтому `localOffsetFromController` всегда
  `null`, детерминированный путь скатывался в радиусный скан, а его результат затирала загрузка
  NBT сорока инструкциями позже. Теперь миксин выходит, если оффсет ещё не загружен; штатный
  `pendingRelinkCheck` части делает перепривязку после загрузки — как и задумано.

### BK9. Асимметричный NBT части мультиблока — ИСПРАВЛЕНО

Три маски сторон (`ClimbSides`, `EnergySides`, `FluidSides`) записывались только когда набор
непустой, а читатель перезаписывал их только при наличии ключа. Любая загрузка в **живой**
экземпляр (клиентский update-тег или `loadCustomOnly` при миграции контроллера) оставляла старые
стороны вместо того, чтобы очистить их. То же для `ControllerPos` — теперь при отсутствии тега поле
явно обнуляется.

### BK10. Программная установка контроллера и расширение набора сборки — ИСПРАВЛЕНО

- `placeStructure` перезаписывал занятые клетки футпринта через `setBlock`, а `checkPlacement`
  висит только на пути предмета (`MultiblockBlockItem`). Установка контроллера через `/setblock`,
  датапаком или другим модом тихо стирала сундуки вместе с содержимым. Теперь непустой и
  незаменяемый блок сначала роняется через `Block.dropResources`.
- `SubLevelAssembleExpansionMixin` делал `set.addAll(...)` по коллекции **вызывающего кода**: если
  движок когда-нибудь передаст неизменяемый набор, исключение оборвёт саму сборку. Обёрнуто в
  `try/catch` с логом — расширение это улучшение, а не повод сломать перенос (та же политика, что
  у миксина клея).
