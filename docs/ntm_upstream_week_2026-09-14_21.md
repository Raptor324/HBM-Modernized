# NTM upstream: сводка за неделю (2026-09-14 … 2026-09-21)

Репозиторий: [HbmMods/Hbm-s-Nuclear-Tech-GIT](https://github.com/HbmMods/Hbm-s-Nuclear-Tech-GIT)
(снимок собран 2026-09-21)

## Общая картина

- **65 коммитов** за неделю, мержено **17 PR** (из ~23 обновлённых).
- Основные контрибьюторы: HbmMods (мейнтейнер), Boblet (порты тайлов), CreeperTyp89
  (структуры/данжи), WolfEclipses (рецепты QMAW), EXPLOSIVEGAMER (турели/CIWS),
  Xpl0itR (OpenComputers/RoR), TheVitya2127, TheMadLadRulz89 (структуры), RayzerHan (ru_RU локаль).
- Главная содержательная линия недели — **QMAW-рецепты** (серия мелких PR WolfEclipses
  и других) и **порты тайлов из старых версий** (Boblet, HbmMods).

## Смёрженные PR

| PR | Название | Автор |
|---|---|---|
| #3255 | The Door Empire (серия дверных правок) | TheVitya2127 |
| #3262 | Small NBT structure addition | spamyspamton1997 |
| #3272 | Обновление uk_UA локали | — |
| #3273 | Steam QMAW | WolfEclipses |
| #3274 | Redstone-over-Radio Designator | EXPLOSIVEGAMER |
| #3281 | Coker QMAW | WolfEclipses |
| #3286 | Mustard Gas QMAW | WolfEclipses |
| #3289 | Fix ash output for charcoal | Beplus2 |
| #3291 | First-class support for RoR/RTTY in OpenComputers | Xpl0itR |
| #3292 | Fix RoR reader in state-change mode after server restart | Xpl0itR |
| #3293 | Обновление ru_RU локали | RayzerHan |
| #3295 | QMAW для nitan-100 («high octane») | WolfEclipses |
| #3301 | Electrolysis Apotheosis | TheVitya2127 |
| #3302 | Фикс бага частиц взрыва | EXPLOSIVEGAMER |
| #3303 | Ещё одна структура (arctic station) | TheMadLadRulz89 |
| #3306 | Creosote QMAW | WolfEclipses |
| #3307 | CIWS Rework + мелкие багфиксы | EXPLOSIVEGAMER |
| #3308 | Ещё структура (military base) | TheMadLadRulz89 |
| #3310 | Скрытие RTTY-карты, когда OC не загружен | Xpl0itR |
| #3311 | Deuterium QMAW | WolfEclipses |
| #3313 | Фиксы крашей турелей | EXPLOSIVEGAMER |

Закрыты без мержа: #3290 (Plasma QMAW), #3294 (catalog hashing fix), #3297/#3298
(deco blocks — вероятно, переделываются), #3309 (дубль #3315), #3332 (Autocal).

## Открытые PR (в работе)

- **#3175** — новая «blue pill» + изменённый рецепт красной.
- **#3192** — кастомные части и материалы (давний, с 21.08).
- **#3315** — RoR-команды для станков (перевыпуск закрытого #3309).
- **#3317** — Osmium QMAW («забыл нажать push»).
- **#3318** — Camp structures (лагерные структуры).
- **#3321** — Подлодка (!).
- **#3322** — Самолёт.
- **#3323** — Biogas/fuel («its all natural»).
- **#3326** — Crash prevention в WorldGen.
- **#3330** — Артиллерия по радио.
- **#2182** — «The Decorationing» (декорации, давно висит).

## Ключевые темы по коммитам

1. **Порты тайлов.** Boblet вёл подготовку TilePort-системы («more prep for TilePort»,
   «implemented new port definition system to immediately discard it again»), мейнтейнер
   мержит пачками: «ports and ports and ports and ports». Затронуты CMB tubes и др.
2. **QMAW-волна.** ~7 смёрженных QMAW-рецептов (steam, coker, mustard gas, nitan-100,
   creosote, deuterium) — похоже на планомерное покрытие химии QMAW-рецептами.
3. **OpenComputers/RoR.** Xpl0itR: RTTY-карта регистрируется только при загруженном OC,
   фикс RoR-ридера после рестарта сервера, «first-class» интеграция RoR/RTTY в OC.
4. **Структуры.** CreeperTyp89 строил данж-контент (asbestos, арсеналы, «frickin dungeon»);
   TheMadLadRulz89 — арктическая станция и военная база.
5. **Турели/CIWS.** Реворк CIWS (#3307) и серия фиксов крашей турелей (#3313).
6. **Прочее.** Фикс зола из древесного угля (#3289), фикс частиц взрыва (#3302),
   обновления локалей ru_RU/uk_UA, «lag free» доступ к unin-массивам (Boblet).

## Issues за неделю (17 новых)

Примечательные открытые:
- **#3334** — улучшение FEI лазера на свободных электронах.
- **#3333** — вопрос о дополнительных скинах дверей (релевантно нашему порту скинов!).
- **#3331** — проблемы pneumatic storage network.
- **#3327** — новый GUI ручного дизайнатора.
- **#3316** — противоспутниковое оружие.
- **#3312** — AUTOCAL не взаимодействует с Telex.
- **#3305** — баг рецепта метеоритного меча.
- **#3300** — баг освещения декоративных блоков.

Закрытые: #3325 (отсутствующий NBT-файл), #3319 (детектор странного камня),
#3314 (нейтринная линза), #3299 (окрашенные бетонные плиты/ступени).

## Что может быть интересно нашему порту

- **#3333 (дверные скины)** — напрямую перекликается с нашим портом дверных скинов
  (11 скинов уже сделано); стоит следить, какие скины добавят в оригинале.
- **Порты тайлов (Boblet/HbmMods)** — новые портированные машины 1.7.10 → кандидаты
  в наш бэклог паритета.
- **RoR/RTTY + OC-интеграция** и **редстоун-дизайнатор (#3274)** — функционал, которого
  у нас пока нет.
- **CIWS rework и фиксы турелей** — если у нас турели уже портированы, сверить поведение.
- **Deuterium/Steam/Coker QMAW-рецепты** — пополнить датаген рецептов при актуализации.
