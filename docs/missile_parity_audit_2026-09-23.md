# Аудит паритета: штатные баллистические ракеты (Modernized vs HBM 1.7.10) — 2026-09-23

Метод: два параллельных саб-агента (полная инвентаризация оригинала `C:\Projects\Hbm-s-Nuclear-Tech-GIT` и порта `C:\Projects\HBM-Modernized`) + ручное сведение и точечные проверки спорных мест. Кастомные ракеты (EntityMissileCustom / ItemCustomMissile / модульная система) — известное отклонение, из аудита исключены.

## Вердикт

Каталог **28/28** предметов перенесён (Tier 0–4, ABM, stealth, shuttle, doomsday_rusted). Физика полёта, константы запуска/топлива/энергии, ABM-перехватчик и числовые параметры радара — **1:1**. Найдено: **1 незакрытая боеголовка (volcano), 1 мёртвый предмет (doomsday_rusted), 1 заменённая боеголовка (test), 1 отсутствующая машина (большой пусковой стол), 4 пробела в спутниковой ветке**, далее минорные.

## Каталог

| id | Тир | Форма | Топливо (мБ ×2 бака) | Боеголовка 1.7.10 | Боеголовка Modernized | Статус |
|---|---|---|---|---|---|---|
| missile_test | 0 | MICRO | SOLID | сфера r=50 → `sellafield_slaked`, без взрыва | стандартный взрыв 25F / dmg r=60 + цепная детонация (`NuclearExplosionHelper`) | **DEVIATION** |
| missile_micro | 0 | MICRO | SOLID | `ExplosionNukeSmall.PARAMS_HIGH`, r=fatmanRadius(35) | MK5 `startFatMan`, r=manRadius(35) | minor (класс взрыва) |
| missile_schrabidium | 0 | MICRO | SOLID | MK3 Fleija + CloudFleija(20) | `FleijaExplosionAPI` (MK3) | PARITY |
| missile_bhole | 0 | MICRO | SOLID | 1.5F + BlackHole(1.5); rare drop black_hole | 1.5F + BlackHoleEntity; rare drop BLACK_HOLE | PARITY |
| missile_taint | 0 | MICRO | SOLID | 5F + 100 taint (±5) | 5F + 100 taint (11³) | PARITY |
| missile_emp | 0 | MICRO | SOLID | empBlast(50) + EntityEMPBlast(50) | empBlast(50) | PARITY |
| missile_generic | 1 | V2 | 4000 | VNT 15F/24 без огня | warheadTier1 15F/24 | PARITY |
| missile_decoy | 1 | V2 | 4000 | 4F; **на радаре Tier 4** | 4F; радар MISSILE_TIER4 | PARITY |
| missile_incendiary | 1 | V2 | 4000 | VNT 15F/24 fire | то же + flameDeath 24 | PARITY |
| missile_cluster | 1 | V2 | 4000 | airburst 5F + cluster(25) | airburst 5F + cluster 25 | PARITY |
| missile_buster | 1 | V2 | 4000 | 15×5F вниз + обломки | 15×5F + shrapnel 10 + rubble 5 | PARITY |
| missile_stealth | 1 | STRONG | 8000 | VNT 20F/24; невидим радару/ABM | warheadStealth 20F/24; невидим | PARITY |
| missile_anti_ballistic | 1 | ABM | SOLID | перехватчик | перехватчик (все константы 1:1) | PARITY |
| missile_strong | 2 | STRONG | 8000 | VNT 30F/32 | 30F/32 | PARITY |
| missile_incendiary_strong | 2 | STRONG | 8000 | 30F/32 fire | то же + flameDeath 25 | PARITY |
| missile_cluster_strong | 2 | STRONG | 8000 | 15F + cluster(50) | 15F + 50 | PARITY |
| missile_buster_strong | 2 | STRONG | 8000 | 20×7.5F | 20×7.5F + shrapnel 15 + rubble 8 | PARITY |
| missile_emp_strong | 2 | STRONG | 8000 | EntityEMP | EmpPulseEntity | PARITY |
| missile_burst | 3 | HUGE | 12000 | VNT 50F/48 (имя «Spare Missile») | 50F/48 («Burst Missile») | PARITY (имя) |
| missile_inferno | 3 | HUGE | 12000 | 50F/48 fire + ignite | + burn 10 + flameDeath 25 | PARITY |
| missile_rain | 3 | HUGE | 12000 | 25F + cluster(100) | 25F + 100 | PARITY |
| missile_drill | 3 | HUGE | 12000 | 30×10F c **ExAttrib.ERRODE** + jolt(10,50,1) | 30×10F **без ERRODE** + jolt | minor |
| missile_shuttle | 3 | OTHER | 8000 | 20F/64 + mush + звук robin_explosion | то же, звук robin отсутствует → generic | известное |
| missile_nuclear | 4 | ATLAS | 16000 | MK5(100) + torex(100) | MK5(missileRadius=100) | PARITY |
| missile_nuclear_cluster | 4 | ATLAS | 16000 | MK5(200), **без MIRV-сплита** | ×2 | PARITY |
| missile_volcano | 4 | ATLAS | 16000 | 10F + 3×3×3 volcanic_lava_block + volcano_core | **заглушка 10F, `// TODO: volcano warhead`** (MissileTier4.java:100) | **GAP** |
| missile_doomsday | 4 | ATLAS | 16000 | MK5(200) + moreFallout(100) | ×2 + extraFallout 100 | PARITY |
| missile_doomsday_rusted | 4 | ATLAS | 16000 | **только** с Rusted Pad, расход launch_code+launch_key; MK5(100)+fallout 100 | `notLaunchable()` → **мёртвый предмет**, пада с кодами нет | **DEAD** |

## Совпадения 1:1 (ключевые константы)

- **Полёт** (`EntityMissileBaseNT` ↔ `MissileBaseEntity`): `motionY=2`; `accelXZ=1/len`, `decelY=2/len`; рампа `velocity += clamp(t/60·0.05, 0, 0.05)` до 4; горизонтальная тяга только на подъёме (реверс на спуске); кластерный абстракт при `motionY < −1.5`; без тяги: drag 0.99, гравитация −0.05 до −1.5; health 50; server-only физика, клиент интерполирует.
- **Запуск**: 75 000 HE за пуск (буфер 100 000); два бака по 24 000 мБ, списание `fuelCap` с каждого; SOLID-ракеты (Tier 0 + ABM) без жидкостей; матрица топлив 4 000/8 000/12 000/16 000 (ethanol+peroxide, kerosene+peroxide, kerosene+oxygen, reformate+oxygen); кулдаун 100 тиков; redstone — только по фронту; дизайнатор обязателен кроме ABM; радар может удалённо пускать (blip→координаты, blip→пре-лок ABM).
- **ABM**: 40 тиков вертикально @1.5; рампа 0.1→6.0; поиск 1000 блоков; упреждение `d/(1.5·v)`; kill-радиус 10 → 15F; промах в землю 20F; деспаун 600 тиков или y>2000; стелс-иммунитет; 3×3 чанка форслоада (в порту — region-тикет радиуса 3).
- **Радар**: 1000 / 3000 (large); цель минимум на +30 выше радара; радар ≥ y55; 500 HE/тик; карта 200×200; режимы редстоуна; линкер-команды; экран-«доска».
- **Дебрис** по тирам и rare drops (bhole→black hole, shuttle→generic); facing-ориентация для рендера и контртейлов tier 3/4; дизайнаторы click / raytrace-300 / manual GUI (+sat).
- Балеfire/N2-боеголовок среди штатных ракет **нет ни в одной из версий** — не дыра.

## Расхождения

### Существенные
1. **Volcano warhead** — TODO, заглушка 10F вместо лавы 3×3×3 + volcano_core (`MissileTier4.java:100`).
2. **missile_doomsday_rusted мёртв**: `notLaunchable()` (ModItems.java:1761), Rusted Pad не реализует код/ключ; `launch_code`/`launch_code_piece`/`launch_key` — инертные предметы. В оригинале `TileEntityLaunchPadRusted` тратит code+key за пуск.
3. **missile_test**: заменена боеголовка (в ориг — сфера sellafield_slaked r=50 без взрыва; в порту — взрыв 25F/60 с цепной детонацией); в порту она также крафтится и переименована, в ориг — скрытый тестовый предмет.
4. **Большой пусковой стол** (TileEntityLaunchPadLarge: эректор 0–90°, лифт, венты окислителя, offset +2, замедленные анимации для HUGE/ATLAS) отсутствует — `LaunchPadLargeBlockEntity` мёртвый код без блока; все ракеты идут с малого пада (offset +1).
5. **Спутниковая ветка**: нет LaunchpadLambda (autolaunch+countdown), нет EntitySatellitePod (miner lander с посадочными ногами — лунные спутники доставляются капсулой), нет EntityBobmazon (курьерская капсула), капсула — стенд-ин без посадки игрока и парашютной анимации. Soyuz-лаунчер при этом полный (режимы, 128k баки, 1M HE, обратный отсчёт, ачивки).

### Минорные / фиделити
6. micro: MK5 вместо ExplosionNukeSmall (радиус 35 совпадает по конфигу manRadius/fatmanRadius).
7. drill: атрибут эрозии ERRODE не применяется.
8. Детект радара: ориг сканирует сущности (чанки должны быть загружены); порт берёт глобальный реестр `MissileTrackBroadcaster` — работает и в незагруженных чанках (фактически сильнее оригинала). Детект снарядов — по имени типа (в коде есть признание, что интерфейса нет).
9. Тултип MissileItem «Pad fluid tanks are temporarily not required for launch (WIP)» противоречит коду: `hasFuel()` требует и списывает баки (комментарий в коде прямо ссылается на GIT-паритет). Текст тултипа протух.
10. Tracking range 512 против 1000 в ориг (смягчено network-track экстраполяцией).
11. Рецепты: в ориг Tier-0 и doomsday **не крафтятся**, Tier 1–4 + ABM собираются дугосваркой (боеголовка+бак+двигатель); в порту у всех 27 есть assembler-рецепты (датаген) — «щедрее» оригинала.
12. Имена расходятся с en_US.lang оригинала (Burst vs «Spare», Generic vs «High Explosive», Atlas Nuclear vs «Nuclear») — косметика.
13. Устаревшие комментарии: «запуск заглушен» в LaunchPadRustedBlockEntity:18; «орбитальной симуляции нет» в SoyuzLauncherBlockEntity:39 против `SatelliteManager.orbit` в SoyuzEntity.

### Улучшения порта (в оригинале нет)
- Network-track рендер (экстраполяция, пауза/реплей-aware, отрисовка за 96+ блоков), flyby-звук с эффектом Допплера, nozzle-flare, контртейл с учётом ParticleStatus и DH-совместимостью, дубли-гварды UUID, Sable-совместимость (`SableCompat.toWorld`), конфиг трека (interval/range/on-off).
- Chunk-тикет ракеты радиуса 3 (7×7 чанков) против 1 чанка в ориг — грузит больше, но совместимо с механикой тикетов.

## Рекомендации (по убыванию)
1. Закрыть volcano warhead (порт лавы + volcano_core из оригинала).
2. Rusted pad: расход launch_code+launch_key, разрешить doomsday_rusted только с него — либо честно пометить NOT_IMPLEMENTED.
3. Решить судьбу большого пускового стола: перенести анимацию или удалить мёртвый `LaunchPadLargeBlockEntity`.
4. missile_test: вернуть sellafield-сферу или задокументировать отклонение (это dev-предмет — можно оставить).
5. Мелочи: ERRODE для drill, протухший тултип топлива, выравнивание рецептов Tier-0.
