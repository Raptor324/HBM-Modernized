# Разбор баг-репорта 1.21.1 (2026-09-14)

Ветка `funni-stuff`, активная версия `1.21.1-neoforge`. Исходник репорта — `1.21.1-bug-report.md`
(общие проблемы 1–11, двери/мультиблоки 1–7). Ниже по каждому пункту: причина, что сделано,
что осталось. Оригинал 1.7.10 сверялся по `HbmMods/Hbm-s-Nuclear-Tech-GIT` (master).

## Ключевой факт про Sable, на котором держится половина правок

`dev.ryanhcode.sable.sublevel.SubLevel` — **не** `Level`. Блоки корабля лежат в **том же**
`ServerLevel`, в plot-чанках с координатами порядка ±20 000 000 (в логе репорта видно
`20481030,128,20481034`). Отсюда:

- `BlockEntity.worldPosition` у блока на корабле — plot-координаты;
- любая **чистая математика** на этих координатах (`Vec3.distanceToSqr`, `AABB.distanceToSqr`,
  вектор до цели, координаты в своём пакете) — за 20 млн блоков от игрока;
- `player.level() == level` при этом **истинно**, размерность общая.

Sable сам транслирует ванильные пути: `Entity.distanceToSqr(...)`/`distanceTo` (**`@Overwrite`**,
`interaction_distance.EntityMixin`), `Player.canInteractWithBlock`, `PlayerList.broadcast` (звуки),
`ServerLevel.sendParticles`, `ChunkMap.getPlayers` (рассылка по трекингу чанков), клиентские
`ParticleEngine`/`SoundEngine`, хит-результаты и рендер сущностей в plot-области. Поэтому
`player.distanceToSqr(pos)` и ванильный `stillValid` на корабле **работают**, а
`Vec3.distanceToSqr`, `AABB.distanceToSqr` и свои пакеты — нет. Ревью поймало на этом регрессию
в первой версии `MenuReach` (см. пункт 10).

Для этого добавлен `compat/sable/SableCompat` (сервер/общий) и `SableClientCompat` (клиент):

- `SableCompat.toWorld(level, pos)` → `SableCompanion.INSTANCE.projectOutOfSubLevel(...)`;
- `SableClientCompat.localToView(origin, cam)` → `renderPose().bakeIntoMatrix()` корабля, в чей
  plot-чанк попадает `origin`, сложенная в double с `T(-cam)` и `T(origin)` (локальные координаты
  относительно блока → camera-relative для текущего кадра).

`sable-companion-common` уже был в `libs/` (compileOnly); в рантайме он приезжает jar-in-jar
внутри Sable, поэтому обращения завёрнуты во вложенный класс `Impl`, который не грузится без
`Platform.isModLoaded("sable")`.

---

## Общие проблемы

### 1. Нет частиц у ядерных бомб на Sable-объекте — ИСПРАВЛЕНО

`NuclearExplosionAPI.start` рассылал `AuxParticlePacket` через `ModPacketHandler.sendToPlayersNear`
с plot-координатами бомбы: дистанция не проходила, и сами координаты в пакете были plot.
Сущность взрыва (`EntityNukeExplosionMK5`) Sable сам выкидывает в мировые координаты, поэтому
кратер был на месте, а гриб — нет.

- `NuclearExplosionAPI.start` проецирует точку взрыва в мир один раз, до сущности, частиц и звука.
- `ModPacketHandler.sendToPlayersNear` меряет дистанцию до спроецированной точки (полезная нагрузка
  пакета — забота отправителя).

### 2. Ракета с Sable-объекта летит вертикально — ИСПРАВЛЕНО

`LaunchPadBaseBlockEntity.instantiateMissile` звал `initLaunch(worldPosition…)` в plot-координатах.
В `MissileBaseEntity.initLaunch` длина вектора до цели получалась астрономической,
`accelXZ = 1/len ≈ 0` — ракета не набирала горизонтальную скорость. Точка пуска (и дым, и звук)
теперь через `SableCompat.toWorld`.

### 3. 3D-дебаг радиации не виден на 1.21.1 — ИСПРАВЛЕНО

Пакеты доходят; не виден текст. В 1.21 ваниль сменила знак X у билборд-текста
(`EntityRenderer.renderNameTag`: `scale(0.025, -0.025, 0.025)` вместо `-0.025`), потому что
изменилась конвенция `Camera.rotation()`. Со старым знаком квад зеркальный, а `RenderType.text*`
собирается с `CULL` по умолчанию — задняя грань отсекается целиком.

- `RenderHooks.scaleBillboardText(poseStack, scale)` — версионный знак; применён в
  `ChunkRadiationDebugRenderer` и `VATSRenderHandler` (там та же ошибка).
- `RenderHooks.isDebugOverlayShown(mc)` — возвращён гейт по F3 (`DebugScreenOverlay.showDebugScreen()`
  на 1.21.1), раньше на 1.21.1 условие было просто выкинуто с TODO.

### 4. Краш Маскмэна при выстреле — ИСПРАВЛЕНО

`TurretBulletEntity.getDefaultItem()` читал `entityData`, а на 1.21.1 `ThrowableItemProjectile`
зовёт его из `defineSynchedData(Builder)` внутри конструктора `Entity`, когда `entityData`
ещё `null`. Добавлена защита; иконка всё равно задаётся явно в `create()`.

### 5. Краш при сохранении с эффектом Taint/Radaway — ИСПРАВЛЕНО

`MobEffect.CODEC` = `holderByNameCodec()` → `safeCastToReference`: принимает **только**
`Holder.Reference`. `PlatformHooks.addEffect/hasEffect/removeEffect/getEffect` на 1.21.1 кастили
сам architectury `RegistrySupplier` к `Holder` — он `Holder`, но не `Reference` (проверено по
`RegistrarManagerImpl$RegistrarImpl$1` в architectury-neoforge 13.0.11), и его `equals` не совпадает
с реестровым `Reference`. Отсюда «Unregistered holder» при `MobEffectInstance.save`, и
`hasEffect`/`removeEffect` не находили эффекты, загруженные из NBT. Прошлая правка BD1 в
`server-stability-review.md` исходила из ложной посылки «RegistrySupplier — это Reference».

Теперь все четыре метода резолвят `BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect.get())`.
Предмет Radaway (`ModItems`) переведён на `PlatformHooks.addEffect` — препроцессорная ветка из
`ModItems` убрана.

### 6а. Taint «накладывается два раза» — ИСПРАВЛЕНО

Та же причина плюс `BlockTaint.entityInside` вешал эффект и на клиенте (нет проверки
`isClientSide`): в клиентской `activeEffects` появлялись два ключа — свой (`RegistrySupplier`) и
синхронизированный с сервера (`Reference`). Добавлен серверный гейт.

### 6б. Tainted Creeper ломает местность — ИСПРАВЛЕНО

Оригинал: `worldObj.newExplosion(this, x, y, z, 5F, false, false)` — `isSmoking = false`, блоки не
ломаются. Порт перевёл это в `ExplosionInteraction.MOB`. Теперь `NONE`.

### 7. Нет частиц у фосгенового крипера — ИСПРАВЛЕНО

Порт `EntityMist` не имел клиентской ветки вообще. Оригинал спавнит по 2 частицы `tower`
(`ParticleCoolingTower`: lift 0.5, base 0.75, max 2, life 50–60, alpha 0.25, цвет флюида) на тик
по всему объёму облака. Добавлены `ModParticleTypes.MIST` + `MistParticle` (порт
`ParticleCoolingTower`, цвет передаётся через speed-аргументы), спавн в `EntityMist.tick`.
Заодно взрыв фосгенового крипера тоже переведён на `NONE` — в оригинале `createExplosion(…, false)`.

### 8. Урон от Taint/фосгена «пинает» — ИСПРАВЛЕНО

В 1.21.1 `LivingEntity.hurt` вызывает `knockback(0.4, 0, 0)` даже без атакующего, если тип урона
не в теге `minecraft:no_knockback` (в 1.7.10 безадресный урон отдачи не имел). В
`ModDamageTypeTagProvider` добавлен `NO_KNOCKBACK` для всех безадресных тиковых уронов:
radiation, taint, cloud, mud_poisoning, acid, lead, enervation, electricity, exhaust, lunar,
monoxide, asbestos, blacklung, vacuum, overdose, microwave, nitan, broadcast, digamma,
euthanized_self(_2), boil. Константы `DamageTypeTags.NO_KNOCKBACK` в 1.20.1 нет (тег появился в
1.20.5), поэтому в провайдере ключ назван строкой; `runData` прогнан, `no_knockback.json` лежит в
`src/generated` и попадает в jar.

### 9. Ore Acidizer: дубликат во вкладках, невидимая вертушка — ИСПРАВЛЕНО

- `ModBlocks.MACHINE_CRYSTALLIZER` — блок-заглушка (`new Block(STONE)`) с именем «Ore Acidizer».
  В двух вкладках заменён на рабочий `ModItems.CRYSTALLIZER`; на него же переведены гейт спавна
  Маскмэна (`BossSpawnHandler`, статистика крафта/использования) и ачивка `ACIDIZER`.
  Сам блок-заглушка не удалён (регистрация/миры).
- `MachineCrystallizerRenderer.collectModelQuads` и `presentDeferredFluids` были под
  `//? if forge {` — на NeoForge квады вертушки и жидкости не собирались вовсе. Открыто для
  обоих загрузчиков.
- Вертушка регистрировалась как `dynamicPart` **без аниматора** — метод `animateSpinner`
  существовал, но не был подключён. Подключён.

### 10. GUI не открывается по дальней части / кик Flashback — ИСПРАВЛЕНО

**Дальняя часть.** 134 меню меряли дистанцию до **контроллера**: 64 меню — `distanceToSqr ≤ 64`
руками, ~38 — ванильный `stillValid(access, player, block)` (`canInteractWithBlock(pos, 4.0)`).
У высокого мультиблока часть, по которой кликнули, дальше 8 блоков от контроллера — меню
закрывается следующим тиком. Добавлен `inventory/menu/MenuReach`: `canInteractWithBlock(pos, 4.0)`
к контроллеру **и каждой части** (`helper.getAllPartPositions`), через
`PlatformHooks.canInteractWithBlock` (1.20.1 — reach-атрибут Forge). Именно ванильный вызов, а не
`AABB.distanceToSqr(eye)`: Sable хукает `canInteractWithBlock` для блоков на корабле, первая версия
с чистой математикой закрывала бы на корабле все 118 GUI следующим тиком (находка ревью).
Скриптом переведены 103 меню; вручную — 15 с переносами строк. Не тронуты: батарейки/сокет
(одиночные блоки, свой `evaluate`), нюки (делегируют в BE), книга.

**Flashback.** 89 меню бросали `IllegalStateException("No X found…")` при отсутствии BE на клиенте,
NeoForge на любое исключение из конструктора меню отвечает дисконнектом. Возвращать `null`, как
предлагает репорт, нельзя: 60 из них тут же дереференсят BE для слотов. Сделано иначе:

- `MenuBlockEntityMissingException extends IllegalStateException` — все 89 бросков + 22 варианта
  «BlockEntity is not a …» переведены на него (серверная семантика не меняется);
- клиентский миксин `MixinClientPayloadHandlerMissingTile` на
  `net.neoforged.neoforge.network.handlers.ClientPayloadHandler.handle(AdvancedOpenScreenPayload)`
  перехватывает **только** это исключение и пишет warning вместо дисконнекта. Любое другое
  исключение по-прежнему кикает; в этом случае серверу отправляется
  `ServerboundContainerClosePacket(windowId)` — иначе `player.containerMenu` на сервере остаётся
  открытым и клики по инвентарю отбрасываются до следующего закрытия экрана (находка ревью).
  Миксин только для neoforge; на forge заглушка на `AbstractContainerMenu` без инъекций.

### 11. Модификаторы брони пропадают — ИСПРАВЛЕНО

Корень нашёл GameTest, а не чтение кода: после вставки сердца в столе NBT брони содержал
`mod_slot_7:{}` — **пустой** compound. На 1.21.1 `ItemStack.save(provider, prefix)` кодирует через
`NbtOps`, который сливает результат в **новый** compound и не трогает переданный. `applyMod`
(и ещё три места: инвентарь `NukeBaseBlockEntity`, payload `SoyuzEntity`/`SoyuzCapsuleEntity`)
собирали тег на месте и игнорировали возвращаемое значение → сохранялась пустышка. Javadoc
`PlatformHooks.saveItemStack` обещал «возвращает тот же заполненный тег на обеих версиях», а
`PlatformHooksGameTest.saveItemStack_intoProvidedTag` это расхождение сознательно прощал.

- `PlatformHooks.saveItemStack` на 1.21.1 теперь сливает результат в переданный тег и возвращает его
  (контракт 1.20.1); тест ужесточён до `returned == target`.
- Второй дефект того же пункта: атрибуты (MAX_HEALTH «сердец») пересобирались только в
  `saveTableToArmor` (закрытие стола / shift-клик), путь «взять броню курсором» их не трогал.
  Пересборка вынесена в `rebuildAttributeModifiers`, зовётся из `applyMod`/`removeMod`
  (как в 1.7.10 эффект следует за NBT сразу).
- `ArmorTableGameTest` (реальные `AbstractContainerMenu.clicked`): сердце → NBT + MAX_HEALTH после
  взятия курсором; обшивка → NBT + защита после закрытия. Итог прогона: **289/289**.

Поведение при закрытии стола оставлено как в порте (моды уходят в броню и броня возвращается
игроку); в 1.7.10 моды в столе при закрытии **снимаются** с брони и выпадают. Если нужен паритет —
отдельное решение.

---

## Двери и мультиблоки

### 3, 4. `invalid for ticking` / `Failed to create block entity … got air` — ИСПРАВЛЕНО

`LevelChunkSilentRemovalMixin` глушит `onRemove` наших блоков при переносе Sable — но ванильный
`Block.onRemove` это ещё и **единственное** место, где BE удаляется из чанка
(`level.removeBlockEntity`). В итоге BE оставался под воздухом: каждый тик
«invalid for ticking», в сейв чанка уезжал BE с воздухом, при загрузке — «Failed to create».
Sable к этому моменту уже сериализовал BE (`saveWithFullMetadata` до `setBlockState(air)`,
см. `SubLevelAssemblyHelper.moveBlocks`), так что удаление безопасно; `setRemoved` наших BE
только снимает подписки сетей. Сделано в подавленной ветке миксина.

(У Sable есть тег `sable:silent_assembly_removal` для того же, но своя правка не зависит от
версии Sable.)

### 2. Открытие/закрытие на физическом объекте: коллизия есть, анимации и звука нет — НЕ ПОДТВЕРЖДЕНО

Первая гипотеза — `DoorBlockEntity.syncToClient()` слал BE-пакет игрокам в 64 блоках от
`worldPosition` и на корабле никому не попадал — **опровергнута ревью**: `ServerPlayer.distanceToSqr`
переопределён Sable с учётом саблевелов, цикл должен был проходить. Синк всё равно переведён на
`serverLevel.getChunkSource().blockChanged(worldPosition)` (ванильная рассылка по трекингу чанков,
без ручного радиуса) — это безвредно и снимает одну переменную, но причину надо искать в игре
вместе с пунктом 1: в `debug.log` смотреть, приходит ли на клиент `onDataPacket` двери на корабле
(`isVisibleStateChange`) и срабатывает ли `handleNewState` (звук — клиентский, по переходу состояния).

### 5. Подсветка мультиблока при выделении клеем — только медоклей — ИСПРАВЛЕНО

Расширение самой группы для зелёного клея уже работало (`SuperGlueSelectionHelperMixin`); не было
клиентской обводки полного мультиблока при наведении, как у медоклея. Скан бокса вынесен в
`GlueOutlineCompat.showMultiblockClusterIn`, добавлен `SuperGlueSelectionOutlineMixin`
(`SuperGlueSelectionHandler.tick`, TAIL, `@Pseudo`).

### 6, 7. Рамка установки и красная подсветка на Sable-чанке — ИСПРАВЛЕНО

Оба рендера строили боксы по абсолютным `BlockPos` — на корабле это plot-координаты.
`MultiblockPlacementHighlight` и `ClientRenderHandler.onRenderWorldLate` (красные препятствия и
фиолетовые сироты) рисуют бокс относительно своего блока и домножают на
`SableClientCompat.localToView(origin, cam)` = `T(-cam)·Pose(корабля)·T(origin)`, собранную в
**double**: plot-координаты ~2·10⁷ при касте во float теряют целые блоки (шаг float там — 2),
поэтому до GPU доходит только малый итоговый сдвиг (находка ревью).

### 1. Рендер двери пропадает после переходов, контроллер выпадает — ОТКРЫТО

Часть причин закрыта пунктами 2–4 (протухшие BE, отсутствие синка). Остальное по коду не
диагностируется — нужен `logs/debug.log` локальной сборки Prism за минуту вокруг перехода
(см. память: `hbm-ingame-diagnostics-log`). Что смотреть:

- `[HBM] contraption move window opened/closed` и `onRemove подавлен` — окно живёт весь перенос?
- `placeStructure suppressed` для контроллера двери;
- `relinkOrphanedPartDeterministic` / `attemptAutoRepair` по позиции двери — именно
  `attemptAutoRepair` «самоуничтожение вместо войны починки» роняет контроллер с дропом
  (`MultiblockStructureHelper:581`), когда в футпринте есть часть с указателем на **другой живой**
  контроллер;
- `UniversalMachinePartBlock.playerWillDestroy` → `destroyBlock(controllerPos)` — если игрок ломал
  часть.

Для рендера: `DoorRenderer` кэширует инстансы по `BlockPos`, `DoorChunkInvalidationHelper`
инвалидирует чанк только при «видимом» переходе состояния относительно предыдущего — у свежего
BE (после переноса) `prevState = 0`, закрытая дверь чанк не инвалидирует. Проверить, помогает ли
принудительный `requestModelDataUpdate()` + инвалидация в `onLoad` после переноса.

---

## Проверка

- `:1.21.1-neoforge:compileJava` — чисто; `:1.20.1-forge:compileJava` (для датагена) — чисто.
- `runGameTestServer` — **289/289** (287 старых + 2 `ArmorTableGameTest`).
- Боевой сервер 192.168.1.20 после деплоя: `Done (3.340s)`, ноль ошибок `hbm_m`,
  `LevelChunkSilentRemovalMixin` применён. Jar также положен в Prism (`trewa`), md5 совпадает.
- Попутно: NeoForge не вырезает `ClientPayloadHandler` из серверного дистрибутива, а секция
  `client` mixin-конфига на dedicated server **не соблюдается** — клиентский миксин применился на
  сервере и уронил старт («Attempted to load class net/minecraft/client/Minecraft for invalid dist»).
  Добавлен `HbmMixinConfigPlugin`: пакет `mixin.client.*` пропускается на dedicated server.
  Forge-заглушки neoforge-миксинов целятся в `AbstractContainerMenu` (аннотационный процессор
  Mixin на 1.20.1 отвергает неизвестные строковые таргеты).

## Что проверить в игре

1. Сборка ядерной бомбы на корабле → гриб на месте корабля.
2. Пуск ракеты с корабля → летит к цели.
3. F3 + `enableDebugRender` → подписи `Rad:` над чанками.
4. Маскмэн стреляет, не крашит.
5. Taint/Radaway → сохранение/выход без краша, один эффект в GUI.
6. Крипер порчи и фосгеновый — без ям, у фосгенового облако с частицами.
7. Кристаллизатор: одна позиция во вкладках, вертушка крутится, жидкость видна.
8. Hydraulic Fracturing Tower: GUI по верхней части, не закрывается.
9. Дверь на корабле: открывается с анимацией и звуком (причина не подтверждена — если нет, нужен debug.log).
9а. GUI любой машины на корабле не закрывается сам (проверка регрессии `MenuReach`).
10. Зелёный клей над мультиблоком — обводка всей конструкции.
11. Установка мультиблока на корабле — зелёная/красная рамка и красные препятствия на месте.
