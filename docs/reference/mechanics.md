# Механики мода: что из чего состоит

Ветка `funni-stuff`, снимок от 2026-09-10, целевая платформа 1.21.1 NeoForge (вторая сборка — 1.20.1 Forge).

Документ описывает **системы**, а не отдельные станки. Перечень машин — в
[`docs/reference/machines.md`](machines.md). Задача обоих документов одна: дать точки входа,
чтобы аудит шёл по конкретной механике, а не по всему коду разом.

Для сверки с оригиналом 1.7.10 рядом лежит
[`hbm-original-progression-early-mid.md`](hbm-original-progression-early-mid.md) — выжимка из вики
по ранней и средней прогрессии.

Размер предмета аудита:

| | |
|---|---|
| предметов | 1851 |
| блоков | 1121 |
| блок-энтити (зарегистрированных типов) | 244 |
| жидкостей | 164 |
| типов рецептов | 38, суммарно 1352 сгенерированных рецепта |
| java-файлов в `src/main/java/com/hbm_m` | ~2100 |

---

## 0. Порядок серверного тика

Единственная точка, где сходятся все серверные системы — `MainRegistry`, обработчик
`TickEvent.SERVER_POST` (`main/MainRegistry.java:110`). Порядок важен, менять его нельзя без разбора:

1. `ContraptionAssemblyGuard.endServerTick()` — закрывает окно переноса контрапшена Create/Sable,
   если миксин не успел его закрыть.
2. `EnergySubscriptions.tickAll(server)` — машины подписываются на энергосети соседей.
3. `UniNodespace.updateNodespace(server)` — пересборка узлов **и тик всех сетей** (энергия и жидкости).
4. `NeutronNodeWorld.tick(...)` для каждого мира — нейтронные потоки РБМК.
5. `StructureConnectionFixProcessor.tickIfReady(server)` — отложенная починка соединений в
   заспавненных структурах.

Выгрузка мира (`LifecycleEvent.SERVER_LEVEL_UNLOAD`) чистит `UniNodespace`, `RTTYNetwork`,
`NeutronNodeWorld` и очередь починки структур; остановка сервера — их же плюс `FluidNetProvider`.

---

## 1. Энергия (HE)

**Где:** `api/energy/`, `api/network/`, `capability/`.

Единица — **HE**, форматируется `util/EnergyFormatter` (`HE`, `HE/t`).

- **Узлы и сети.** Общий каркас — `api/network/UniNodespace` + `NodeNet`/`GenNode`: карта
  «позиция + провайдер сети → узел», сети собираются из связных узлов. Провайдер энергии —
  `PowerNetProvider`, сеть — `PowerNet` (порт `PowerNetMK2`).
- **Распределение** (`PowerNet.update`): собрать доступную мощность у провайдеров
  (`min(запас, provideSpeed)`), собрать спрос получателей (`min(свободное место, receiveSpeed)`)
  с разбивкой по **пяти приоритетам** `IEnergyReceiver.Priority`, раздать сверху вниз по приоритету
  пропорционально спросу. Участник, не подтвердивший себя 3 секунды, выбрасывается из сети.
- **Подписка машин** (`EnergySubscriptions`): машины сами ищут проводник рядом, с бэкоффом
  20 тиков (ничего не нашли) / 10 тиков (подписались) / 1 тик (прямая передача соседу).
- **Проводники:** `WireBlock`/`WireBlockEntity`, `SwitchBlock` (разрыв сети), пилоны и коннекторы
  в `blockentity/network/` (`RedPylon*`, `RedConnector*`, `RedCablePaintable`, `RedCableGauge`).
- **Предметы-аккумуляторы:** `ItemEnergyAccess` — единая точка доступа к энергии в предмете
  (собственные HBM-capability плюс FE лоадера).
- **Мост в FE:** `ConverterBlockEntity` с семью ступенями лимита (1k…Integer.MAX), два блока —
  `machine_converter_he_rf` и `machine_converter_rf_he`.

**На что смотреть в аудите:** приоритеты получателей почти нигде не выставляются явно; `provideSpeed`
и `receiveSpeed` у машин заданы вразнобой; проверка сторонности (`canConnectEnergy`) реализована
не у всех.

---

## 2. Жидкости

**Где:** `api/fluids/`, `inventory/fluid/`.

- **Регистр:** `inventory/fluid/ModFluids` — 164 жидкости, у каждой цвет и опционально газовая форма.
- **Бак:** `inventory/fluid/tank/FluidTank` — тип, объём, **давление** (`getPressure`), плюс загрузчики
  (`FluidLoaderStandard`, `FluidLoaderFillableItem`, `FluidLoaderInfinite`) для слотов канистр.
- **Трейты жидкости** (`inventory/fluid/trait/`, 14 штук): горючесть (`FT_Flammable`),
  сгораемость в топливе (`FT_Combustible`), охлаждение (`FT_Coolable`), нагрев (`FT_Heatable`),
  коррозия (`FT_Corrosive`), яд и токсин, феромоны, загрязнение (`FT_Polluting` + `PollutionType`),
  замедлитель PWR, радиоактивный выхлоп. Трейты определяют, что жидкость делает в машине и в мире.
  Раздаются они в одном месте — `api/fluids/bootstrap/ModFluidTraitsBootstrap` (все 164 жидкости).
- **Сеть:** `FluidNetProvider` создаёт по сети **на каждый тип жидкости**; `FluidNet` раздаёт объём
  по уровням давления и приоритетам подключения (`ConnectionPriority`), с добором остатка
  до 100 итераций.
- **Участие машины:** интерфейсы `IFluidStandardReceiverMK2` / `SenderMK2` / `TransceiverMK2`
  (`getAllTanks`, `getReceivingTanks`, `getSendingTanks`, `isLoaded`) плюс вызовы
  `trySubscribe`/`tryProvide` из тика машины. **Машина без этих вызовов в сеть не входит**, даже
  если у неё есть баки, — это уже находили несколько раз.
- **Совместимость с лоадером:** `NeoForgeFluidHandlerMK2` (NeoForge) и адаптеры Forge;
  сторонность фильтруется через `BaseHbmBlockEntity.isFluidSideAllowed`.
- **Эквивалентность:** `VanillaFluidEquivalence` сопоставляет жидкости мода с ванильными.

---

## 3. Тепло

**Где:** `interfaces/IHeatSource` — три метода (`getHeatStored`, `getMaxHeatStored`, `useUpHeat`).

Отдельной сети нет: потребитель сам ищет источник у соседнего блока. Участников десять — топка,
нагревательная печь, нагреватель, теплообменник, кокер, сталелитейная печь, Гефест, лесопилка,
Стирлинг, нефтяная горелка.

---

## 4. Радиация

**Где:** `radiation/`, `capability/ModAttachments`, `client/ClientRadiationData`.

- **Хранение** — почанковое, через capability (Forge) / AttachmentType (NeoForge), доступ
  унифицирован в `ChunkRadiationAccess`.
- **Фасад** `ChunkRadiationManager` → рабочая реализация `ChunkRadiationHandlerSimple`
  (порт `updateSystem` из 1.7.10): каждый цикл делает snapshot, растекание по соседям
  **60 / 7.5 / 2.5 %** и затухание **×0.99 − 0.05** для чанков, где радиация уже была;
  свежий чанк в первый цикл не затухает. Потолок — `maxRad` из конфига.
  `ChunkRadiationHandlerPRISM` — альтернативная реализация, почти целиком закомментирована.
- **Игрок:** `radiation/PlayerHandler` держит накопленную дозу в NBT (`hbm_m_player_radiation`),
  выдаёт эффекты и достижения (`RAD_POISON`, `RAD_DEATH`).
- **Источники:** предметы через `HazardSystem` (см. ниже), блоки-источники, выхлоп реакторов,
  fallout после ядерного взрыва, газ радон.

---

## 5. Опасности предметов

**Где:** `hazard/`, `event/PlayerHazardHandler`, `event/HazardTooltipHandler`.

`HazardSystem` — таблица «предмет → список эффектов», по одному `HazardEntry` на тип:

- **9 типов** (`hazard/type/`): радиация, асбест, угольная пыль, дигамма, ожог, гидроактивность,
  ослепление, взрывоопасность, база.
- **5 модификаторов** (`hazard/modifier/`): радиация топлива, горячий РБМК-стержень,
  радиация РБМК-стержня, радиация РТГ.
- **4 трансформера** (`hazard/transformer/`): пересчёт радиации для контейнеров, ME-хранилищ и NBT.

Регистрация — `HazardRegistry.registerItems()` из `MainRegistry`. Тултипы опасности рисует
`HazardTooltipHandler`, применение к игроку — `PlayerHazardHandler`.

---

## 6. Газы

**Где:** `block/gas/` — 11 блоков-газов (хлор, угольная пыль, асбест, монооксид, радон обычный,
плотный и «гробничный», взрывоопасный, горючий, meltdown).

Общая база `BlockGasBase`: газ живёт как блок, растекается по `tick`/`randomTick` в направлении,
которое возвращает наследник (`getFirstDirection`), с задержкой `getDelay`, и действует на
сущность внутри (`entityInside`). Вдыхание обрабатывает `event/LungGasHandler`.

---

## 7. Взрывы

Три независимых слоя.

1. **Модульный «ванильный» взрыв** — `explosion/vanillant/ExplosionVNT`: сборка из стратегий
   (`IBlockAllocator`, `IBlockProcessor`, `IEntityProcessor`, `IExplosionSFX` + мутаторы блоков,
   дропа и радиуса). Готовые реализации — в `vanillant/standard/`.
2. **Ядерный взрыв** — `explosion/NuclearExplosionAPI` + `NuclearExplosionConfig` + сущность
   `EntityNukeExplosionMK5`, поедание чанков `NukeMk5ChunkEater`, лучевая модель
   `ExplosionNukeRayParallelized`. Гриб рисуется отдельно на клиенте.
3. **Специальные** — `ExplosionBalefire`, `ExplosionSolinium`, `ExplosionFleija`, `ExplosionTom`,
   `ExplosionChaos`, `MultiBombExplosion`, плюс общий `ExplosionNukeSmall` (мини-нюк, ядерный крипер, НЛО)
   и `MissileWarheadEffects` для боеголовок.

**Осадки:** `entity/effect/EntityFalloutRain` — после взрыва рисует зону заражения, догружая чанки
точечными тикетами с лимитом одновременно выданных, ставит кратерные биомы.

Команды отладки — `explosion/command/`.

---

## 8. Мультиблоки

**Где:** `multiblock/`, `interfaces/IMultiblockPart`, `IMultiblockController`, `IMultiblockSidedIO`.

- **Сборка и разбор:** `MultiblockStructureHelper` — постановка частей по шаблону, снос всей
  структуры одним обходом габаритов, перепривязка осиротевших частей.
- **Части:** `UniversalMachinePartBlock` + `UniversalMachinePartBlockEntity`, роль задаётся
  `PartRole` (обшивка, энергетический коннектор, жидкостный, универсальный, лестница…).
- **Стороны:** `MultiblockSideTuples` + `IMultiblockSidedIO` задают, какими гранями структура
  принимает энергию и жидкость.
- **Установка:** `MultiblockBlockItem`, `MultiblockPlacement`, подсветка места
  (`client/MultiblockPlacementHighlight`).
- **Совместимость с Create/Sable:** `ContraptionAssemblyGuard` — счётчик глубины окна переноса;
  пока окно открыто, каскады сноса и перепривязки молчат, иначе перенос структуры краном
  разбирал бы её.

---

## 9. Рецепты, апгрейды, чертежи

- **Рецепты** — data-driven, 38 типов (`recipe/`, регистрация в `recipe/ModRecipes`), JSON
  генерируется датагеном (`datagen/`, запускать только на 1.20.1-forge). Кросс-версионный доступ —
  `platform/recipe/RecipeHooks`.
- **Апгрейды** — `inventory/UpgradeManager`: считает уровень по предметам `ItemMachineUpgrade`
  в заданном диапазоне слотов, с потолком на тип. Типы: скорость, энергоэффективность, овердрайв,
  афтербёрнер. Каждая машина сама решает, как уровень влияет на время цикла и потребление —
  **единой формулы нет**, это отдельная тема для аудита.
- **Чертежи и шаблоны** — `ItemAssemblyTemplate`, папка чертежей, `module/machine/` (модули
  сборщика, химзавода, химфабрики) — логика крафта, вынесенная из блок-энтити.
- **Показ рецептов** — JEI (`compat/jei/`), 41 категория; расплавленные материалы рисуются
  цветным свотчем, потому что JEI-ингредиента для них нет.

---

## 10. Логистика предметов

Три независимые системы.

- **Краны и конвейеры** (`blockentity/network/MachineCrane*`, `entity/conveyor/`):
  вставка, извлечение, сортировка, разветвление, упаковка и распаковка; предметы едут
  сущностями `MovingConveyorItemEntity` / `MovingConveyorPackageEntity`.
- **Request-сеть** (`blockentity/network/request/RequestNetwork`): узлы-предложения и
  узлы-запросы с временем жизни 2 секунды, поиск достижимых узлов в радиусе.
- **Дроны** (`entity/drone/`, `blockentity/network/MachineDrone*`): док, провайдер, реквестер,
  путевые точки, ящик; дрон везёт груз по цепочке точек, держит чанки через `DroneChunkLoader`.

---

## 11. Радио и сигналы

**Где:** `blockentity/network/radio/`, `api/redstoneoverradio/`.

`RTTYNetwork` — именованные каналы на мир: `broadcast(level, channel, signal)` и `listen(...)`.
Абоненты — радиофакелы: отправитель, приёмник, логический, счётчик, ридер, контроллер
(`RadioTorch*BlockEntity`), плюс телекс и автокалибратор. Каналы чистятся при выгрузке мира
и остановке сервера.

---

## 12. Ракеты, площадки, спутники, радар

- **Ракеты:** `entity/missile/` — базовая `MissileBaseEntity` и тиры 0–4, ABM (перехватчик),
  stealth, shuttle, «Союз» с капсулой. Боеголовки — через `MissileWarheadEffects`.
- **Площадки:** `LaunchPadBaseBlockEntity` (проверка и расход топлива по `fuelCap` ракеты),
  сборка ракеты — `MissileAssemblyBlockEntity`, «Союз» — `SoyuzLauncherBlockEntity`.
- **Слежение:** `server/missile/MissileTrackBroadcaster` + `missile/track/` — трек ракеты на клиенте.
- **Спутники:** `satellite/Satellite` (реестр типов), `SatelliteManager` (SavedData: частота → спутник
  на орбите), `SatelliteHorizons`. Зарегистрирован пока один тип — Gerald.
- **Радар:** `MachineRadarBlockEntity` + экран, приём команд — `IRadarCommandReceiver`.

---

## 13. Реакторы

- **РБМК** — самая большая подсистема. Параметры вынесены в `handler/rbmk/RBMKDials`
  (пассивное охлаждение 2.5 / 0.1, поток тепла между колоннами 0.2, высота колонны 3,
  дальность потока 5, ReaSim 10, выключаемые расплавления, перегрев и т. д.), часть из них
  проброшена в геймрулы (`RBMKGameRules`). Нейтронные потоки считает `NeutronNodeWorld` +
  `RBMKNeutronHandler` раз в серверный тик. Колонны, консоль, кран и панельные приборы —
  в `blockentity/machines/rbmk/` (31 класс), подробности в документе по машинам.
- **PWR** — `PWRControllerBlockEntity` + части, замедлитель задаётся трейтом жидкости `FT_PWRModerator`.
- **ZIRNOX** — `MachineZirnoxBlockEntity` и его разрушенная версия.
- **Исследовательский реактор** — `MachineReactorResearchBlockEntity`.

---

## 14. Броня

- **Силовая броня** (`powerarmor/`): `ModPowerArmorItem` / `ModArmorFSB` / `ModArmorFSBPowered`,
  характеристики в `PowerArmorSpecs` (ёмкость, приём, пассивный расход, расход на единицу урона),
  комплекты T51, AJR, AJRO, DNT, висмутовый. Плюс шаги, звуки, VATS-оверлей и HUD.
- **Модификации брони** (`armormod/`): 10 модулей — батарея, кладдинг, противогаз, здоровье,
  кевлар, нож, защита от радиации, сервоприводы, дополнительный слот. Установка через свой стол,
  подсказки — `ModTooltipHandler`.
- **Защита от опасностей:** `handler/ArmorRegistry` и `HazmatRegistry` — какой комплект от чего спасает.

---

## 15. Эффекты, урон, свойства игрока

- `effect/ModEffects` — радиация, тайнт, радавэй; рендер иконок в `effect/render/`.
- `damagesource/ModDamageTypes` + `ModDamageSources` — типы урона мода (нужны data-файлы, иначе
  сервер падал на старте — это уже чинили).
- `extprop/HbmLivingProps` — дополнительные свойства сущности (доза, дигамма и т. п.).
- `event/` — обработчики: разминирование бомб, ломание ящиков, газ в лёгких, отвёртка, опасности.

---

## 16. Мир

`worldgen/`: руды в коренной породе (`BedrockOreFeature`, `BedrockOreDensity`, нефтяная
разновидность), нефтяные месторождения (`OilDepositFeature`, `OilClasterSurroundedFeature`),
«красная комната», процессоры структур (фундамент, лут, починка соединений).
`world/biome` — кратерные биомы, которые ставит fallout.

---

## 17. Клиент

Самая объёмная часть после блок-энтити (279 файлов).

- **Рендер машин:** `client/render/implementations/` (49), `render/machine/`, `render/rbmk/`,
  запечённые модели `client/model/` (33) поверх `AbstractMultipartBakedModel` — модель собирается
  из именованных частей OBJ.
- **Загрузчики моделей:** `client/loader/` (33) — OBJ, DAE и специальные загрузчики машин.
- **Батчинг и производительность:** `render/MdiBatchCoordinator`, `SingleMeshVboRenderer`,
  `InstancedStaticPartRenderer`, `render/culling/` — своя система инстансинга и отсечения.
- **Шейдеры:** `render/shader/` (14) — совместимость с Iris/Oculus, `IrisRenderBatch`,
  детектор внешнего шейдера, модификации шейдеров.
- **Частицы:** `particle/` (62 файла) — своя система `nt` с собственным движком (`ParticleEngineNT`),
  взрывные кольца, гриб (`NukeTorex`), дым, туман.
- **Оверлеи:** `client/overlay/` — счётчик Гейгера, силовая броня, радиация, противогаз, тосты.
- **Звук:** `sound/` + `client/sound/` — зацикленные звуки машин, пролёт ракеты.

---

## 18. Платформа и совместимость

- **Stonecutter** — один `src/main` на две версии; конвенция: все ветвления в `platform/*Hooks`,
  в машинах чистая логика. Текущее состояние и оставшийся долг — в
  [`docs/platform-hooks-audit.md`](../platform-hooks-audit.md).
- **Architectury** — общие события, реестры, сеть (`network/ModPacketHandler`).
- **Совместимость:** JEI (`compat/jei/`), Create и Sable (`compat/create/`, миксины `SubLevel*`),
  Curios (`compat/curios/`), Distant Horizons (`compat/dh/`), Iris.

---

## 19. Проверка

- **GameTest:** `com.hbm_m.test` — 14 классов, 308 методов, 287 обязательных. Покрытие: радиация (85),
  хуки платформы (49), газы (42), крафты машин (42), энергосеть (28), кросс-лоадерный паритет (23).
  Подробности — `docs/systems/GAMETESTS.md`.
- **Отладка:** свойства `-Dhbm_m.netDebugPackets`, `-Dhbm_m.disableS2C`, команды взрывов,
  отладочный рендер радиации.
