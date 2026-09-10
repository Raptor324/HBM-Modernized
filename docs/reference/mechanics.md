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
| типов рецептов | 38 машинных (1352 рецепта) + 1 спец-крафт |
| java-файлов в `src/main/java/com/hbm_m` | ~2100 |

---

## 0. Порядок серверного тика

Серверных тик-обработчиков **семь**, и разложены они по разным классам. Полный порядок за один
тик сервера:

| Фаза | Где | Что делает |
|---|---|---|
| SERVER_PRE | `radiation/ChunkRadiationManager:62` | раз в **20 тиков** — `updateSystem()`, растекание и затухание радиации |
| SERVER_POST | `main/MainRegistry:110` | основная цепочка, см. ниже |
| SERVER_POST | `radiation/ChunkRadiationManager:72` | `handleWorldDestruction()` — разрушение мира радиацией |
| SERVER_POST | `armormod/event/ArmorModificationServerEvents:20` | подрезка здоровья после снятия брони |
| SERVER_POST | `server/missile/MissileTrackBroadcaster:45` | рассылка треков ракет клиентам |
| SERVER_LEVEL_POST | `event/HazardEventHandler:19` | опасности предметов у сущностей мира |
| SERVER_LEVEL_POST | `handler/BossSpawnHandler:61` | спавн боссов |

Основная цепочка в `MainRegistry` (порядок важен, менять нельзя без разбора):

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
  в `blockentity/network/` (`RedPylon*`, `RedConnector*`, `RedCablePaintableBlockEntity`, `RedCableGaugeBlockEntity`).
- **Предметы-аккумуляторы:** `ItemEnergyAccess` — единая точка доступа к энергии в предмете
  (собственные HBM-capability плюс FE лоадера).
- **Мост в FE:** `ConverterBlockEntity` с семью ступенями лимита (1k…Integer.MAX), два блока —
  `machine_converter_he_rf` и `machine_converter_rf_he`.

**На что смотреть в аудите:** приоритет получателя переопределён всего в пяти местах, остальные
~240 блок-энтити идут на `NORMAL`; `provideSpeed` и `receiveSpeed` заданы вразнобой; сторонность
`canConnectEnergy` почти нигде не ограничивается — база возвращает `true` для всех граней,
переопределений около двенадцати.

---

## 2. Жидкости

**Где:** `api/fluids/`, `inventory/fluid/`.

- **Регистр:** `inventory/fluid/ModFluids` — 164 жидкости, у каждой цвет и опционально газовая форма.
- **Бак:** `inventory/fluid/tank/FluidTank` — тип, объём, **давление** (`getPressure`), плюс загрузчики
  (`FluidLoaderStandard`, `FluidLoaderFillableItem`, `FluidLoaderInfinite`) для слотов канистр.
- **Трейты жидкости** — **22**: одиннадцать отдельными файлами в `inventory/fluid/trait/`
  и ещё одиннадцать вложенными классами в `FluidTraitSimple` (`FT_Gaseous`, `FT_Gaseous_ART`,
  `FT_Liquid`, `FT_Viscous`, `FT_Plasma`, `FT_Amat`, `FT_LeadContainer`, `FT_Delicious`,
  `FT_Unsiphonable`, `FT_NoID`, `FT_NoContainer` — агрегатное состояние и правила обращения).
  Отдельные задают поведение: горючесть (`FT_Flammable`),
  сгораемость в топливе (`FT_Combustible`), охлаждение (`FT_Coolable`), нагрев (`FT_Heatable`),
  коррозия (`FT_Corrosive`), яд и токсин, феромоны, загрязнение (`FT_Polluting` + `PollutionType`),
  замедлитель PWR, радиоактивный выхлоп. Трейты определяют, что жидкость делает в машине и в мире.
  Точка входа раздачи одна — `ModFluidTraitsBootstrap.registerAll()` из `MainRegistry`, но дальше
  она зовёт `ModFluidCalculatedFuel` (расчёт топливной энергии) и `ModFluidHazardBootstrap`.
- **Сеть:** `FluidNetProvider` создаёт по сети **на каждый тип жидкости**; `FluidNet` раздаёт объём
  по шести уровням давления (`HIGHEST_VALID_PRESSURE = 5`) и пяти приоритетам подключения
  (`ConnectionPriority`), с добором остатка до 100 итераций.
- **Обход балансировщика:** если в сети есть трансивер с `isInfiniteNetworkSource`
  или `isInfiniteNetworkSink`, весь алгоритм пропускается — сеть просто заливает всех получателей
  или осушает всех поставщиков (`forceFillAllReceivers`/`forceDrainAllProviders`), давление
  и приоритеты игнорируются.
- **Участие машины:** интерфейсы `IFluidStandardReceiverMK2` / `SenderMK2` / `TransceiverMK2`
  (`getAllTanks`, `getReceivingTanks`, `getSendingTanks`, `isLoaded`) плюс вызовы
  `trySubscribe`/`tryProvide` из тика машины. **Машина без этих вызовов в жидкостную сеть не входит**, даже
  если у неё есть баки, — это уже находили несколько раз. Для энергии иначе: `EnergySubscriptions`
  подписывает все зарегистрированные блок-энтити централизованно, без их участия.
- **Совместимость с лоадером:** `NeoForgeFluidHandlerMK2` (NeoForge) и адаптеры Forge;
  сторонность фильтруется через `BaseHbmBlockEntity.isFluidSideAllowed`.
- **Эквивалентность:** `VanillaFluidEquivalence` сопоставляет жидкости мода с ванильными.

---

## 3. Тепло

**Где:** `interfaces/IHeatSource` — три метода (`getHeatStored`, `getMaxHeatStored`, `useUpHeat`).

Отдельной сети нет: потребитель ищет источник строго **снизу** (`level.getBlockEntity(pos.below())`), а не у любого соседа.

- **Источники** (реализуют интерфейс), 4: топка, электронагреватель, теплообменник, нефтяная горелка.
- **Потребители**, 3: паровой котёл, кокер, Стирлинг.

Вся шина — семь машин. Сталелитейная печь, Гефест и лесопилка **в ней не участвуют**, хотя
`IHeatSource` в их javadoc упоминается: сталь переведена на твёрдое топливо, Гефест сканирует
блоки лавы напрямую, лесопилка работает без нагрева. Заодно комментарий
`MachineFurnaceSteelBlockEntity:36` утверждает, что системы `IHeatSource` в порту нет вовсе —
это неверно, она есть.

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

`HazardSystem` — таблица «предмет → список эффектов», по одному `HazardEntry` на тип. Правила
регистрируются не только на предмет, но и на **тег предметов** (`TAG_RULES`) и на **блок**;
результат кэшируется.

- **8 типов** (`hazard/type/` плюс база `HazardTypeBase`): радиация, асбест, угольная пыль,
  дигамма, ожог, гидроактивность, ослепление, взрывоопасность.
- **4 модификатора** (`hazard/modifier/` плюс база `HazardModifier`): радиация топлива,
  горячий РБМК-стержень, радиация РБМК-стержня, радиация РТГ.
- **3 трансформера** (`hazard/transformer/` плюс база `HazardTransformerBase`): пересчёт радиации
  для контейнеров, ME-хранилищ и NBT.

Регистрация — `HazardRegistry.registerItems()` из `MainRegistry`. Тултипы опасности рисует
`HazardTooltipHandler`, применение к игроку — `PlayerHazardHandler`.

---

## 6. Газы

**Где:** `block/gas/` — 10 блоков-газов плюс база `BlockGasBase` (хлор, угольная пыль, асбест, монооксид, радон обычный,
плотный и «гробничный», взрывоопасный, горючий, meltdown).

Общая база `BlockGasBase`: газ живёт как блок, растекается по `tick`/`randomTick` в направлении,
которое возвращает наследник (`getFirstDirection`), с задержкой `getDelay`, и действует на
сущность внутри (`entityInside`). Вдыхание обрабатывает `event/LungGasHandler`.

---

## 7. Взрывы

Три независимых слоя.

1. **Модульный «ванильный» взрыв** — `explosion/vanillant/ExplosionVNT`: сборка из стратегий
   (`IBlockAllocator`, `IBlockProcessor`, `IEntityProcessor`, `IExplosionSFX` + мутаторы блоков,
   дропа и радиуса). Готовые реализации — в `explosion/vanillant/standard/`.
2. **Ядерный взрыв** — `explosion/NuclearExplosionAPI` + `NuclearExplosionConfig` + сущность
   `EntityNukeExplosionMK5`, поедание чанков `NukeMk5ChunkEater`, лучевая модель
   `ExplosionNukeRayParallelized`. Гриб рисуется отдельно на клиенте.
3. **Специальные** — `ExplosionBalefire`, `ExplosionSolinium`, `ExplosionFleija`, `ExplosionTom`,
   `ExplosionChaos`, `MultiBombExplosion`, `CustomNukeExplosion`, `FleijaExplosionAPI`,
   `ExplosionNukeGeneric` (урон и радиация), плюс `ExplosionNukeSmall` — его зовут только ядерный
   крипер и НЛО — и `MissileWarheadEffects` для боеголовок.

**Осадки:** `entity/effect/EntityFalloutRain` — после взрыва рисует зону заражения, догружая чанки
региональными тикетами радиусом 2 с лимитом 64 одновременно выданных, ставит кратерные биомы.

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

- **Рецепты** — data-driven, 38 типов машин (`recipe/`, регистрация в `recipe/ModRecipes`),
  1352 записи; плюс один спец-крафт `rbmk_fuel_disassembly`, у которого зарегистрирован только
  сериализатор, без `RecipeType` — итого по полю `"type"` в файлах 39 идентификаторов и 1353 записи
  (считать надо по полю `"type"`, а не по имени файла или папки: имя папки с типом расходится —
  `shredder/` даёт `hbm_m:shredding`, `chemplant/` — `hbm_m:chemical_plant`, — а имя блока
  встречается ещё и в его собственном крафте).
  Лежат в **двух местах**: сгенерированные датагеном в `src/generated/resources/data/hbm_m/recipes/`
  (`datagen/`, запускать только на 1.20.1-forge) и рукописные в `src/main/resources/data/hbm_m/recipes/`
  (там весь `arc_furnace` — 36 — и весь `combination_oven` — 30; в датагене их нет вовсе).
  При подсчётах надо смотреть оба каталога. Кросс-версионный доступ — `platform/recipe/RecipeHooks`.
- **Апгрейды** — `inventory/UpgradeManager`: считает уровень по предметам `ItemMachineUpgrade`
  в заданном диапазоне слотов, с потолком на тип. Типов **восемь**: `SPEED`, `EFFECT`, `POWER`,
  `FORTUNE`, `AFTERBURN`, `OVERDRIVE`, `STACK`, `EJECTOR` (по 3 тира, 24 предмета). `FORTUNE`
  не читает ни одна машина. Каждая машина сама решает, как уровень влияет на время цикла и потребление —
  **единой формулы нет**, это отдельная тема для аудита.
- **Чертежи и шаблоны** — `ItemAssemblyTemplate`, папка чертежей, `module/machine/` (модули
  сборщика, химзавода, химфабрики) — логика крафта, вынесенная из блок-энтити.
- **Показ рецептов** — JEI (`compat/jei/`), 41 категория; расплавленные материалы рисуются
  цветным свотчем, потому что JEI-ингредиента для них нет.

---

## 10. Логистика предметов

Три независимые системы.

- **Краны и конвейеры** (`blockentity/network/MachineCrane*`, `entity/conveyor/`):
  извлечение, захват с ленты, вставка, сортировка, разветвление, упаковка и распаковка — семь
  блоков; предметы едут сущностями `MovingConveyorItemEntity` / `MovingConveyorPackageEntity`.
- **Request-сеть** (`blockentity/network/request/RequestNetwork`): узлы-предложения и
  узлы-запросы с временем жизни 2 секунды, поиск достижимых узлов в радиусе.
- **Дроны** (`entity/drone/`, `blockentity/network/MachineDrone*`): док, провайдер, реквестер,
  путевые точки, ящик; дрон везёт груз по цепочке точек, держит чанки через `DroneChunkLoader`.

---

## 11. Радио и сигналы

**Где:** `blockentity/network/radio/`, `api/redstoneoverradio/`.

`RTTYNetwork` — именованные каналы на мир: `broadcast(level, channel, signal)` и `listen(...)`,
без ограничения дальности. Абоненты: шесть радиофакелов (отправитель, приёмник, логический,
счётчик, ридер, контроллер), телекс, автокалибратор, FM-радио и **вся приборная панель РБМК** —
рычаг, клавиатура и терминал шлют, шкала, лампа, табло и график слушают. Консоль реактора в этом
обмене не участвует, она читает колонны напрямую. Каналы чистятся при выгрузке мира и остановке
сервера.

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

- **РБМК** — самая большая подсистема (29 классов и 2 интерфейса в `blockentity/machines/rbmk/`).
  Параметры вынесены в `handler/rbmk/RBMKDials`
  (пассивное охлаждение 2.5 / 0.1, поток тепла между колоннами 0.2, высота колонны 3,
  дальность потока 5, ReaSim 10, выключаемые расплавления, перегрев и т. д.), часть из них
  проброшена в геймрулы (`RBMKGameRules`). Нейтронные потоки считает `NeutronNodeWorld` +
  `RBMKNeutronHandler` раз в серверный тик. Колонны, консоль, кран и панельные приборы —
  подробности в документе по машинам.
- **PWR** — `PWRControllerBlockEntity` + части, замедлитель задаётся трейтом жидкости `FT_PWRModerator`.
- **ZIRNOX** — `MachineZirnoxBlockEntity` и его разрушенная версия.
- **Исследовательский реактор** — `MachineReactorResearchBlockEntity`.

---

## 14. Броня

- **Силовая броня** (`powerarmor/`): `ModPowerArmorItem` / `ModArmorFSB` / `ModArmorFSBPowered`,
  характеристики в `PowerArmorSpecs` (ёмкость, приём, пассивный расход, расход на единицу урона),
  комплекты T51, AJR, AJRO, DNT, висмутовый. Плюс шаги, звуки, VATS-оверлей и HUD.
- **Модификации брони** (`armormod/`): 9 модулей плюс база `ItemArmorMod` — батарея, кладдинг, противогаз, здоровье,
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

Самый большой пакет мода — 279 файлов, больше даже `blockentity/` (260).

- **Оверлеи:** `client/overlay/` — счётчик Гейгера, противогаз, радиация, тосты и четыре дверных
  хелпера; оверлей силовой брони и VATS живут отдельно, в `powerarmor/overlay/`.
- **Рендер машин:** `client/render/implementations/` (49), `client/render/machine/`, `client/render/rbmk/`,
  запечённые модели `client/model/` (33) поверх `AbstractMultipartBakedModel` — модель собирается
  из именованных частей OBJ.
- **Загрузчики моделей:** `client/loader/` (33) — OBJ, DAE и специальные загрузчики машин.
- **Батчинг и производительность:** `client/render/MdiBatchCoordinator`, `SingleMeshVboRenderer`,
  `InstancedStaticPartRenderer`, `client/render/culling/` — своя система инстансинга и отсечения.
- **Шейдеры:** `client/render/shader/` (14) — совместимость с Iris/Oculus, `IrisRenderBatch`,
  детектор внешнего шейдера, модификации шейдеров.
- **Частицы:** `particle/` (62 файла) — своя система `nt` с собственным движком (`ParticleEngineNT`),
  взрывные кольца, гриб (`NukeTorex`), дым, туман.
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

- **GameTest:** `com.hbm_m.test` — 13 классов с тестами (плюс `GameTestRegistration`),
  **287 методов, все обязательные** (`required = false` не встречается ни разу). Покрытие по
  батчам: радиация 91, хуки платформы 45, газы 41, крафты машин 40, энергосеть 27 (три батча),
  кабели 14, кросс-лоадерный паритет 20. Подробности — `docs/systems/GAMETESTS.md`.
- **Отладка:** свойства `-Dhbm_m.netDebugPackets`, `-Dhbm_m.disableS2C`, команды взрывов,
  отладочный рендер радиации.
