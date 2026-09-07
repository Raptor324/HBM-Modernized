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
**265 рецептов просто не крафтятся**. Ошибку в лог давал только `non_occluding`, потому что
тег-файл ссылается на тег жёстко.

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

## B*. Осталось решить (не регрессии, улучшения против оригинала)

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

Последствия: клиент может заставить сервер грузить/генерировать чанки в произвольной точке мира
и управлять машинами, к которым у него не открыто меню.

**Предложение.** Один общий guard (хелпер в `ModPacketHandler`): `player.level().isLoaded(pos)`,
`player.distanceToSqr(...) <= 64`, и где уместно — проверка `player.containerMenu`.

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

## C. Мелочи

- **Отладочный вывод в проде — УБРАНО.** `PlatformRecipeSerializer` печатал в `System.err`
  `[HBM DEBUG] decode CALLED/SUCCESS/FAILED` плюс `printStackTrace()` на загрузке каждого рецепта,
  и вёл два `static` счётчика, инкрементируемые без синхронизации (рецепты грузятся в пуле).
  Заменено на `MainRegistry.LOGGER.error("Failed to parse recipe", e)` — причина по-прежнему
  видна со стектрейсом, но через логгер: `RecipeManager` наружу отдаёт только `message`.
- **Нестабильный GameTest.** `gas_meltdownPumpsChunkRadiationUnderSky` в одном из четырёх прогонов
  упал (`got 3.217`, ожидалось `> baseline + 10`), в остальных прошёл. Радиация хранится **на чанк**,
  а арены GameTest расставляются по несколько в одном чанке, поэтому соседние тесты влияют друг на
  друга через общее значение ambient, плюс `updateSystem` раз в 20 тиков применяет decay ×0.99−0.05.
  Тест стоит завязать на изолированный чанк либо на дельту, а не на абсолютный порог.
- 9 warning'ов компиляции на deprecated NeoForge API, все помечены `for removal`:
  `Item#initializeClient` / `MobEffect#initializeClient` (`MissileItem`, `RangeDetonatorItem`,
  `RadawayEffect`, `TaintEffect`), `EventBusSubscriber.Bus` (`ModEntityEvents`,
  `ClientPowerArmorRenderNeoForge`), `NetworkManager.toPacket` (`RadiationGameTest`).
- `RuntimeDistCleaner: Attempted to load class net/minecraft/client/... for invalid dist
  DEDICATED_SERVER` — штатный шум dev-окружения. `mixins.hbm_m.json` разделён на секции правильно,
  клиентские миксины лежат в `client`. Не баг.
- `ChunkRadiationAccess` содержит в комментарии китайские иероглифы («AttachmentType取代 capabilities»).
