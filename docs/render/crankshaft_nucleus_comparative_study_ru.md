# CrankShaft 26.2 против Nucleus: Сравнительное архитектурное исследование и план модернизации

**Версия документа:** 1.0.0-PROD  
**Целевые кодовые базы:**  
- **CrankShaft:** `C:\Projects\CrankShaft` (ветка `26.2`, v1.3.1 / v1.4.0, Minecraft 26.2 / NeoForge 26.2.0.88 / Sodium 0.9.2+mc26.2)  
- **Nucleus:** `c:\Projects\HBM-Modernized` (HBM: Nuclear Tech Mod Modernized, активные ветки `1.20.1-forge` и `1.21.1-neoforge`)  
**Область исследования:** Клиентские подсистемы рендеринга, Multi-Draw Indirect (MDI), вычислительные конвейеры GPU (Compute Shaders), организация промежуточной памяти (Staging Buffers), интероперабельность с шейдерными модами (Iris / Oculus) и кросс-версионный уровень абстракции платформ (`com.hbm_m.platform`).  
**Автор:** Специалист по технической документации (Teamwork Preview Deliverable Worker M6)  
**Дата:** 2026-09-18 (Обновлено: 2026-09-24)  

---

## Содержание

1. [Вводный обзор и архитектурные концепции](#1-вводный-обзор-и-архитектурные-концепции)
   - 1.1 Контекст и цели исследования
   - 1.2 CrankShaft 26.2: Видение и область применения
   - 1.3 Nucleus: Видение и область применения
   - 1.4 Высокоуровневая сравнительная диаграмма архитектурных конвейеров
2. [Детальный разбор архитектуры CrankShaft 26.2](#2-детальный-разбор-архитектуры-crankshaft-262)
   - 2.1 Проходы рендеринга и жизненный цикл кадра
   - 2.2 Поток диспетчеризации и иерархия классов
   - 2.3 Планирование взаимодействия CPU-GPU и модель барьеров
   - 2.4 Мешлеты и представление геометрии (`:meshlet`)
   - 2.5 Структуры вершинных буферов и разбиение ландшафта
   - 2.6 Конвейеры Multi-Draw Indirect (MDI)
   - 2.7 Вычислительный GPU-куллинг и пирамиды глубин Hi-Z
   - 2.8 Архитектура Task / Mesh шейдеров NVIDIA
   - 2.9 Управление памятью: промежуточные буферы (Staging), мульти-привязка SSBO и страничные слэбы
   - 2.10 Убер-шейдеры, независимая от порядка прозрачность (OIT) и политика отката
   - 2.11 Указатель ключевых файлов и классов
3. [Детальный разбор архитектуры Nucleus (com.hbm_m.client.render)](#3-детальный-разбор-архитектуры-nucleus-comhbm_mclientrender)
   - 3.1 Базовая архитектура и последовательность вызовов жизненного цикла кадра
   - 3.2 Координатор батчинга MDI (`MdiBatchCoordinator`)
   - 3.3 Подсистема геометрического атласа (`MdiGeometryAtlas`)
   - 3.4 Рендереры составных частей и модель памяти (`InstancedStaticPartRenderer`)
   - 3.5 Персистентный стейджинг и дифференциальная загрузка диапазонов 2-го уровня (Span-Diff)
   - 3.6 4-уровневый конвейер интероперабельности с шейдерами Iris / Oculus
   - 3.7 Вычислительный GPU-бейкер (`NucleusGpuBaker`)
   - 3.8 Глобальный сборщик теневого батча и компенсация дисторсии теней
   - 3.9 Архитектура волюметрического кэширования карт освещения (`LightSampleCache`)
   - 3.10 Иерархия отсечения и видимости (Frustum, Ray-March, Fade LOD)
   - 3.11 Рабочий процесс составных OBJ-моделей и обход стандартного диспетчера
   - 3.12 Указатель ключевых файлов и классов
4. [Комплексный сравнительный анализ по 7 доменам](#4-комплексный-сравнительный-анализ-по-7-доменам)
   - 4.1 Главная сравнительная матрица
   - 4.2 Домен 1: Архитектура конвейера и поток выполнения
   - 4.3 Домен 2: Multi-Draw Indirect (MDI) и стратегия батчинга
   - 4.4 Домен 3: Управление памятью GPU и промежуточная загрузка (Staging)
   - 4.5 Домен 4: Стратегия отсечения и окклюзионное тестирование
   - 4.6 Домен 5: Интероперабельность с шейдерными модами (Iris / Oculus / Vanilla)
   - 4.7 Домен 6: Обработка динамической и анимированной геометрии
   - 4.8 Домен 7: Профиль производительности и накладные расходы драйверов/оборудования
5. [Конкретные архитектурные рекомендации для Nucleus](#5-конкретные-архитектурные-рекомендации-для-nucleus)
   - 5.1 Стратегический вердикт: Обоснование отказа от полного переноса движка
   - 5.2 Практическое проектное предложение 1: Окклюзионный куллинг по пирамиде глубин GPU Hi-Z (адаптированный под Forward-Z)
   - 5.3 Практическое проектное предложение 2: Адаптивное пороговое переключение для промежуточной загрузки Compute Scatter
   - 5.4 Практическое проектное предложение 3: Инстансинг с субгрупповой компактизацией
   - 5.5 Практическое проектное предложение 4: Безопасность компиляции шейдеров под драйверы AMD (`safeShaderSource`)
   - 5.6 Сопоставление с платформенным слоем (`com.hbm_m.platform` для 1.20.1 и 1.21.1)
   - 5.7 Приоритизированная дорожная карта реализации и этапы
6. [Приложения](#6-приложения)
   - Приложение A: Исчерпывающий каталог цитирования исходного кода
   - Приложение B: Технический глоссарий и акронимы
7. [Приложение C: Глубокий анализ подсистемы шейдерпаков Iris в CrankShaft 1.4.0 и сравнительная переоценка](#7-приложение-c-глубокий-анализ-подсистемы-шейдерпаков-iris-в-crankshaft-140-и-сравнительная-переоценка)
   - 7.1 Архитектурный обзор и масштаб релиза (CrankShaft 1.4.0 / коммит 635b7b0)
   - 7.2 Гостевой движок и конвейер Multi-Draw Indirect (GuestEngine, GuestIndirectDrawManager)
   - 7.3 Компиляция шейдеров и динамическая AST-трансформация через glsl-transformer (GuestShaders, GuestPipelines)
   - 7.4 Архитектура прохода теней и Frustum Culling на GPU (GuestShadows, GuestShadowCull, полупрозрачные тени)
   - 7.5 Расширенные геометрические атрибуты и тегирование Entity/BlockEntity (GuestVertexExtras, GuestDrawTags)
   - 7.6 Контракт шейдерпаков Colorwheel против встроенных адаптеров (ContractProperties, ContractProgram, встроенные адаптеры)
   - 7.7 Интеграция с ландшафтом чанков Sodium и экспериментальные NV Mesh Shaders под Iris (IrisTerrainRasterizer)
   - 7.8 Независимая от порядка прозрачность (OIT) под шейдерпаками: профили вейвлетов и отложенных слоев
   - 7.9 Сравнительная переоценка бок о бок: гостевой патчинг CrankShaft 1.4.0 против 4-уровневого конвейера Nucleus
   - 7.10 Переоценка стратегического вердикта: портирование движка целиком против заимствования компонентов (переоценка Раздела 5.1)
   - 7.11 Первичный указатель цитирования исходного кода подсистемы CrankShaft 1.4.0
   - 7.12 Навигационный указатель сносок

---

## 1. Вводный обзор и архитектурные концепции

### 1.1 Контекст и цели исследования

Промышленные комплексы в модифицированном Minecraft — типичным примером которых является HBM: Nuclear Tech Mod (NTM) — создают экстремальную нагрузку на графическую подсистему. В развитой технологической зоне могут одновременно находиться десятки многоблочных конструкций (например, продвинутые сборщики (Advanced Assemblers), химические комбинаты (Chemical Plants), ядерные реакторы, центрифуги, машины непрерывного литья (Strand Casters), турбины). Каждый такой механизм состоит из множества сложных составных полигональных сеток Wavefront OBJ, вращающихся механических кинематических звеньев и генерирует частые обновления состояний. В рамках стандартной модели диспетчеризации `BlockEntityRenderer` (BER) в ванильном Minecraft каждая отдельная деталь отрисовывается изолированным вызовом (`draw call`), требующим повторных вычислений матриц модели-вида на CPU, пространственной выборки освещения для каждой детали и немедленной загрузки буферов. В крупных промышленных базах с сотнями машин производительность упирается не в скорость растеризации на GPU, а в задержки драйверов CPU и узкое горлышко огромного числа вызовов отрисовки.

Для полного устранения этих ограничений современные движки рендеринга переходят на GPU-ориентированные (GPU-driven) архитектуры. В настоящем исследовании сопоставляются две передовые реализации:
1. **CrankShaft (ветка 26.2)**: Неофициальный форк и порт движка Flywheel 1.x под Minecraft 26.2 (JDK 25+, переработка Blaze3D на базе Vulkan/RHI, Sodium 0.9.2), оснащенный передовой растеризацией через меш-шейдеры NVIDIA (`:meshlet`), двухфазным окклюзионным куллингом по пирамиде Hi-Z и независимой от порядка прозрачностью (OIT).
2. **Nucleus (`com.hbm_m.client.render`)**: Высокопроизводительный специализированный движок рендеринга, разработанный непосредственно внутри HBM-Modernized для Minecraft 1.20.1 (Forge 47.4.20) и 1.21.1 (NeoForge 21.1.248), использующий монолитный геометрический атлас Multi-Draw Indirect (MDI), персистентный когерентный стейджинг с дифференциальной загрузкой изменений (Level-2 Span Diffing), обход стандартного диспетчера BER и оригинальный 4-уровневый конвейер совместимости с шейдерными модами Iris/Oculus.

Цель исследования — всесторонне оценить оба движка по семи ключевым архитектурным доменам, выявить технологические преимущества, проанализировать возможность переноса наработок CrankShaft в Nucleus и сформулировать приоритизированную дорожную карту развития Nucleus.

### 1.2 CrankShaft 26.2: Видение и область применения

CrankShaft 26.2 спроектирован как универсальный высокопроизводительный диспетчер визуальных сцен на GPU для сущностей, тайловых сущностей и ландшафта Minecraft. Его фундаментальные принципы:
- **Полная автономность GPU**: Исключение любых точек синхронизации CPU-GPU с блокирующим чтением данных. Отсечение инстансов, проекция ограничивающих сфер, построение командных буферов и подсчет параметров косвенной отрисовки выполняются целиком в вычислительных шейдерах (Compute Shaders).
- **Двухфазный окклюзионный конвейер Hi-Z**: Использование иерархического буфера глубины предыдущего кадра (пирамиды Hi-Z) для отсечения скрытой геометрии перед основным проходом непрозрачных объектов, быстрое построение новой пирамиды глубин в Single Pass Downsampler (SPD) и запуск второй фазы отрисовки исключительно для объектов, ставших видимыми в текущем кадре.
- **Растеризация через мешлеты (Meshlets)**: Отказ от традиционного фиксированного считывания вершин на современном оборудовании в пользу конвейеров Task/Mesh шейдеров (`NV_mesh_shader`), использующих внутригрупповые перестановки (SIMD butterfly shuffles) и голосование субгрупп для отсечения примитивов субпиксельного размера.
- **Универсальная экосистема**: Обеспечение рендеринга всех сущностей мира, тайлов, эффектов разрушения блоков и чанков ландшафта на уровне всего игрового клиента.

### 1.3 Nucleus: Видение и область применения

Nucleus спроектирован специально для сложных, составных многоблочных механизмов индустриального мода в реальных условиях тяжелых сборок модов и активных шейдерпаков. Его принципы:
- **Составной геометрический атлас (Multipart Geometry Atlas)**: Объединение гетерогенных OBJ-моделей с динамической иерархией костей в единый разделяемый вершинный/индексный буфер GPU (`MdiGeometryAtlas`), что позволяет отрисовывать сотни разнородных многоблочных механизмов за один-единственный вызов `glMultiDrawElementsIndirect`.
- **Минимизация трафика шины (Zero-Copy Upload)**: Связка персистентных когерентных кольцевых буферов (`PersistentUploadStaging`) с теневыми буферами на CPU (`GpuSpanUploader`). Статичные машины генерируют нулевой трафик по шине PCIe между кадрами; вращающиеся детали обновляют только точный 16-байтный срез кватерниона вращения.
- **Глубокая интероперабельность с шейдерпаками**: Вместо отключения оптимизаций при активных шейдерах Nucleus предоставляет 4-уровневый адаптивный конвейер. Он включает вычислительный GPU-бейкер на GLSL 4.3 (`NucleusGpuBaker`), трансформирующий вершины инстансов прямо в видеопамяти и отправляющий их на отрисовку через активные программы G-buffer или теней используемого шейдерпака со 100% сохранением геометрии и специфических кривых дисторсии теней.
- **Кросс-версионный паритет загрузчиков**: Идентичное функционирование на Forge 1.20.1 и NeoForge 1.21.1 через строгую систему платформенных хуков (`com.hbm_m.platform`).

### 1.4 Высокоуровневая сравнительная диаграмма архитектурных конвейеров

```
+----------------------------------------------------------------------------------------------------+
|                                    ОБЗОР АРХИТЕКТУРНЫХ КОНВЕЙЕРОВ                                  |
+--------------------------------------------------+-------------------------------------------------+
| CRANKSHAFT 26.2 (Flywheel / Meshlet)[^iris-update-1]            | NUCLEUS (HBM-Modernized)                        |
+--------------------------------------------------+-------------------------------------------------+
|  События тика LevelRenderer / Visual Tick        |  RenderLevelStageEvent (AFTER_ENTITIES)         |
|         │                                        |         │                                       |
|         ▼                                        |         ▼                                       |
|  [Параллельный план кадра: ForEachPlan]          |  [NucleusDispatcherBypass: плоский перебор LIVE]|
|  - Запись инстансов в стейджинг CPU-слэбов       |  - Обход диспетчера BER в чанках Sodium         |
|         │                                        |  - LightSampleCache: трилинейный свет (8 углов) |
|         ▼                                        |         │                                       |
|  [Персистентный стейджинг + Compute Scatter]     |  [GpuSpanUploader + PersistentUploadStaging]    |
|  - Персистентный кольцевой буфер 16 МБ           |  - Дифференцирование теневого буфера (16 байт)  |
|  - scatter.glsl копирует стейджинг в SSBO        |  - Персистентное кольцо 4 МБ (GL 4.4 / SubData) |
|         │                                        |         │                                       |
|         ▼                                        |         ▼                                       |
|  [Фаза 1 GPU-куллинга: cull.glsl]                |  [Видимость и отсечение]                        |
|  - SIMD-тест фрустума (6-FMA)                    |  - CpuFrustumCuller: 6 плоскостей Грибба/Хартмана|
|  - Окклюзия по пирамиде Hi-Z предыдущего кадра   |  - OcclusionCullingHelper: марш 15 лучей + кэш  |
|  - Атомики через subgroup ballot (1 на варп)     |  - RenderDistanceHelper: пропорц. фейд (30%)    |
|         │                                        |         │                                       |
|         ▼                                        |         ▼                                       |
|  [Построитель команд: apply.glsl]                |  [MdiBatchCoordinator]                          |
|  - Запись instanceCount в косвенные команды      |  - Двухфазное разделение по варианту G          |
|         │                                        |  - Повторное использование списков в чистых кадр|
|         ▼                                        |         │                                       |
|  [Непрозрачный проход 1: glMultiDrawElementsInd.]|  [Ветвление конвейера выполнения]               |
|  - Либо glMultiDrawMeshTasksIndirectNV           |    ├─► Ванильный проход:                        |
|         │                                        |    │   Мульти-отрисовка MdiGeometryAtlas        |
|         ▼                                        |    │   (glMultiDrawElementsIndirect)            |
|  [Даунсемплинг Hi-Z: Single Pass Downsampler]    |    │                                            |
|  - Построение пирамиды MIP-уровней глубины       |    └─► Активен Iris / Oculus (4 уровня):        |
|         │                                        |        ├─ Уровень 1: NucleusGpuBaker (Compute)  |
|         ▼                                        |        ├─ Уровень 2: Инстансинг ExtendedShader  |
|  [Фаза 2 GPU-куллинга и отправка прохода 2]      |        ├─ Уровень 3: IrisRenderBatch (персист.) |
|  - Отрисовка объектов, открывшихся в этом кадре  |        └─ Уровень 4: Немедленный откат к BER    |
|         │                                        |         │                                       |
|         ▼                                        |         ▼                                       |
|  [Независимая прозрачность: Wavelet / MLAB]      |  [Фаза 2: Отправка затухающих объектов]         |
|  - Проходы накопления прозрачности и резолв      |  - Глобальная сортировка от дальних к ближним   |
+--------------------------------------------------+-------------------------------------------------+
```

---

## 2. Детальный разбор архитектуры CrankShaft 26.2

### 2.1 Проходы рендеринга и жизненный цикл кадра

CrankShaft выстраивает исполнение кадра в виде дискретных, последовательно защищенных барьерами фаз GPU, координируемых через `IndirectDrawManager.java` (`dev.engine_room.flywheel.backend.engine.indirect.IndirectDrawManager`):

```
[Инициализация кадра]
       │
       ├─► 1. Синхронизация uniform-буферов и начала координат (`EngineImpl.render`)
       │      - Проверка смещения камеры относительно `sqrMaxOriginDistance` (256 блоков).
       │      - Рецентрирование начала координат рендеринга для предотвращения джиттера float32.
       │      - Сброс `EnvironmentStorage` и передача матриц вида/проекции на GPU.
       │
       ├─► 2. Промежуточная загрузка стейджинга (`StagingBuffer.flush`)
       │      - Сброс записанных на CPU диапазонов отображаемой памяти (`GL30C.glFlushMappedBufferRange`).
       │      - Запуск вычислительного шейдера копирования `scatter.glsl` для потоковой передачи в SSBO.
       │      - `glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT | GL_BUFFER_UPDATE_BARRIER_BIT)`.
       │
       ├─► 3. Фаза 1 окклюзионного куллинга на GPU (`IndirectDrawManager.dispatchCull`)
       │      - Привязка текстуры пирамиды глубин Hi-Z предыдущего кадра (`depthPyramid.bindForCull()`).
       │      - Запуск вычислительного шейдера `cull.glsl` (размер локальной рабочей группы 64).
       │      - Проверка ограничивающих сфер относительно 6 плоскостей фрустума и пирамиды глубин.
       │      - Запись битовой маски видимости в SSBO `_flw_visWords`.
       │      - `glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT)`.
       │
       ├─► 4. Построение и применение команд (`IndirectDrawManager.dispatchApply`)
       │      - Запуск вычислительного шейдера `apply.glsl`.
       │      - Запись счетчиков выживших инстансов в косвенные буферы команд `MeshDrawCommand`.
       │      - `glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT)`.
       │
       ├─► 5. Отправка Фазы 1 непрозрачной геометрии (`IndirectDrawManager.submitSolid`)
       │      - Привязка мастер-пулов вершин/индексов и убер-шейдеров материалов.
       │      - Отправка видимых батчей через `glMultiDrawElementsIndirect` или `glMultiDrawMeshTasksIndirectNV`.
       │      - Буфер глубины заполняется ближайшими непрозрачными окклюдерами.
       │
       ├─► 6. Построение пирамиды глубин Hi-Z (Downsampling) (`DepthPyramid.generate`)
       │      - Mip-уровень 0 копируется из основного буфера глубины через `downsample_first.glsl`.
       │      - Mip-уровни с 1 по 6 генерируются за один запуск compute-шейдера через `downsample_second.glsl`
       │        с использованием барьеров локальной разделяемой памяти воркгруппы (LDS).
       │
       ├─► 7. Фаза 2 окклюзионного куллинга на GPU (`IndirectDrawManager.dispatchCullPass2`)
       │      - Привязка вновь сформированной пирамиды глубин текущего кадра.
       │      - Повторная проверка только тех инстансов, что были скрыты в Фазе 1 (бит `_flw_visWords` был 0).
       │      - `glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT)`.
       │
       ├─► 8. Применение команд 2 и Фаза 2 непрозрачной отрисовки (`submitPass2IfPending`)
       │      - Обновление счетчиков команд косвенной отрисовки через `apply.glsl`.
       │      - `glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT | GL_COMMAND_BARRIER_BIT)`.
       │      - Дополнительный вызов косвенной отрисовки для объектов, вышедших из окклюзии в текущем кадре.
       │
       ├─► 9. Проход разрушения блоков (`IndirectDrawManager.renderCrumbling`)
       │      - Привязка текстур стадий разрушения и отправка косвенных команд из scratch-потока.
       │
       └─► 10. Независимая от порядка прозрачность (`IndirectDrawManager.renderOit`)
              - Wavelet OIT (4-проходное накопление тригонометрических моментов и полноэкранный резолв)
              - Либо MLAB OIT (попиксельно точные связные списки A-buffer/K-buffer со вставкой в SSBO).
```

### 2.2 Поток диспетчеризации и иерархия классов

CrankShaft отделяет высокоуровневое представление визуальных объектов от низкоуровневых примитивов GPU посредством иерархии наследования и композиции:

- **`EngineImpl`** (`dev.engine_room.flywheel.backend.engine.EngineImpl`):
  Корневой менеджер визуализаций Flywheel. Управляет `DrawManager`, `LightStorage` и `EnvironmentStorage`. Контролирует привязку начала координат: при удалении игрока более чем на 256 блоков от `renderOrigin` вызывает глобальный сдвиг базы координат для обеспечения числовой точности float32 в шейдерах.
- **`DrawManager<N extends AbstractInstancer<?>>`** (`dev.engine_room.flywheel.backend.engine.DrawManager`):
  Потокобезопасный реестр активных инстансеров, отображаемых по `InstancerKey<?>`. Инстансеры, создаваемые при тике визуалов вне основного потока, помещаются в `initializationQueue` и лениво инициализируются в потоке рендеринга внутри `createFramePlan()`.
- **`IndirectDrawManager`** (`dev.engine_room.flywheel.backend.engine.indirect.IndirectDrawManager`):
  Наследник `DrawManager` для косвенного бэкенда OpenGL 4.6 / Vulkan. Управляет буферами косвенных команд, сортировкой, группами куллинга и вызовами MDI.
- **`IndirectCullingGroup<I extends Instance>`** (`dev.engine_room.flywheel.backend.engine.indirect.IndirectCullingGroup`):
  Агрегирует инстансеры с одинаковой структурой инстанса (например, стандартный трансформированный визуал против визуала только со светом). Управляет выделением SSBO для данных инстансов и метаданных моделей внутри группы.
- **`MeshVisualDrawManager`** (`dev.engine_room.flywheel.backend.engine.indirect.MeshVisualDrawManager`):
  Расширяет косвенную диспетчеризацию для поддержки бэкенда `gl_mesh_shader`. Преобразует вызовы отрисовки в вызовы task/mesh через `command_builder.comp` и выполняет `glMultiDrawMeshTasksIndirectNV`.

### 2.3 Планирование взаимодействия CPU-GPU и модель барьеров

CrankShaft устраняет простои конвейера CPU-GPU благодаря соблюдению инварианта нулевого обратного чтения (Zero-Readback):
1. **Отсутствие обратного чтения на CPU**: Вычислительные шейдеры GPU проверяют видимость инстансов и записывают параметры команд отрисовки непосредственно в SSBO и буферы косвенных команд. CPU никогда не запрашивает число видимых объектов через `glGetBufferSubData`, что предотвращает сбросы конвейера драйвером.
2. **Точная расстановка барьеров памяти**:
   - `GL_SHADER_STORAGE_BARRIER_BIT | GL_BUFFER_UPDATE_BARRIER_BIT`: выставляется после выполнения копирования через compute scatter промежуточного буфера, гарантируя видимость данных инстансов вычислительным блокам куллинга.
   - `GL_SHADER_STORAGE_BARRIER_BIT`: помещается между `cull.glsl` и `apply.glsl`, обеспечивая полную запись битовых масок видимости и счетчиков.
   - `GL_COMMAND_BARRIER_BIT`: выставляется строго перед `glMultiDrawElementsIndirect`, гарантируя завершение записи GPU в буфер косвенных команд (`GL_DRAW_INDIRECT_BUFFER`).
3. **Утилизация кольцевых буферов через фенсы**:
   `StagingBuffer` отслеживает продвижение GPU через синхропримитивы OpenGL (`GL32.glFenceSync`). Выделенная область памяти удерживается до момента, пока фенс соответствующего кадра не перейдет в сигнальное состояние: `GL32.glClientWaitSync(sync, 0, 0) == GL32.GL_ALREADY_SIGNALED`.

### 2.4 Мешлеты и представление геометрии (`:meshlet`)

CrankShaft реализует кластеризацию примитивов и генерацию ограничивающих иерархий непосредственно в `MeshPool.java` (`dev.engine_room.flywheel.backend.engine.MeshPool`):

#### Логика разбиения на мешлеты
- Константа: `static final int MESHLET_TRIS = 64;` (строка 22).
- Геометрия разбивается на мешлеты, содержащие до 64 треугольников (192 индексные ссылки).
- Расчет числа мешлетов:
  $$\text{meshletCount} = \left\lfloor \frac{\text{indexCount} / 3 + \text{MESHLET\_TRIS} - 1}{\text{MESHLET\_TRIS}} \right\rfloor$$

#### Консервативное построение ограничивающей сферы
Реализовано в `MeshPool.writeMeshletBounds(PooledMesh mesh, long vertsPtr, long outPtr, long idxScratchPtr)` (строки 184–228):
1. Считывает тройки индексов $(i_0, i_1, i_2)$ для каждого треугольника в пределах окна из 64 треугольников.
2. Вычисляет локальные экстремумы по осям координат:
   $$\mathbf{v}_{\min} = [\min(x), \min(y), \min(z)], \quad \mathbf{v}_{\max} = [\max(x), \max(y), \max(z)]$$
3. Вычисляет центр $C$ как среднюю точку:
   $$C = \frac{\mathbf{v}_{\min} + \mathbf{v}_{\max}}{2}$$
4. Вычисляет консервативный радиус границы $R$:
   $$R = \|\mathbf{v}_{\max} - C\|_2 = \sqrt{(x_{\max} - C_x)^2 + (y_{\max} - C_y)^2 + (z_{\max} - C_z)^2}$$
5. Записывает упакованный 16-байтный дескриптор для каждого мешлета в буфер `meshletBounds`:
   ```
   [ Float32 CenterX ] [ Float32 CenterY ] [ Float32 CenterZ ] [ Float32 Radius ]
   ```
6. Базовое смещение `meshletBase` передается непосредственно в структуру команды `MeshDrawCommand`.

### 2.5 Структуры вершинных буферов и разбиение ландшафта

#### Базовый формат вершин Flywheel (`InternalVertex.java`)
Страйд: **36 байт**, строгое выравнивание по границам 4-байтных машинных слов:

| Атрибут | Формат OpenGL | Тип данных | Размер (байт) | Смещение | Примечания |
|---|---|---|---|---|---|
| `Position` | `RGB32_FLOAT` | `vec3` | 12 | 0 | Координаты в пространстве модели $(x, y, z)$ |
| `Color` | `RGBA8_UNORM` | `vec4` | 4 | 12 | Нормализованный цвет вершины (RGBA) |
| `UV0` | `RG32_FLOAT` | `vec2` | 8 | 16 | Текстурные координаты блока/предмета $(u, v)$ |
| `UV1` | `RG16_SINT` | `ivec2` | 4 | 24 | Координаты наложения (Overlay UV) |
| `UV2` | `RG16_UINT` | `uvec2` | 4 | 28 | Координаты карты света (Lightmap UV) |
| `Normal` | `RGB8_SNORM` | `vec3` | 3 | 32 | Вектор нормали к поверхности |
| *Выравнивание* | — | — | 1 | 35 | Байт дополнения до 36 байт |

#### Секционирование ландшафта Sodium
Для геометрии чанков (`CompactChunkVertex`, страйд 20 байт):
- Позиция упаковывается в 16-битные целые числа относительно секции чанка.
- CrankShaft перехватывает аллокации секций чанков Sodium и делит их на 7 поддиапазонов граней (`DOWN`, `UP`, `NORTH`, `SOUTH`, `WEST`, `EAST`, `UNASSIGNED`), что позволяет отсекать тыльные полигоны целых под-мешей ландшафта на уровне Task-шейдера.

### 2.6 Конвейеры Multi-Draw Indirect (MDI)

#### A. Визуалы (Visual Instances — `IndirectDrawManager.java`)
- Активные вызовы отрисовки сортируются компаратором `UBER_DRAW_COMPARATOR`, группирующим команды по bias, индексу меша, конвейеру материала, состоянию embedded, привязке текстур и типу инстанса.
- Сгруппированные диапазоны упаковываются в `UberDraw(Material material, boolean embedded, int start, int end)`.
- Структура буфера команд (`MeshDrawCommand` в `draw_command.glsl`, страйд **48 байт**):
  ```glsl
  struct MeshDrawCommand {
      // Стандартная DrawElementsIndirectCommand OpenGL (20 байт)
      uint indexCount;
      uint instanceCount;
      uint firstIndex;
      uint vertexOffset;
      uint baseInstance;

      // Расширенные метаданные CrankShaft (28 байт)
      uint modelIndex;
      uint matrixIndex;
      uint packedFogAndCutout;
      uint packedMaterialProperties;
      uint vertexCount;         // Число уникальных вершин для декодирования сварки
      uint meshletBase;         // Базовое смещение в SSBO meshletBounds
      uint packedTexIndices;    // lo16: биндлесс-слот текстуры, hi16: тип инстанса
  };
  ```
- Выполняется через вызов:
  ```java
  glMultiDrawElementsIndirect(GL_TRIANGLES, GL_UNSIGNED_INT, indirectOffset, drawCount, 48);
  ```

#### B. Диспетчеризация мешей ландшафта чанков (`TerrainDrawDispatcher.java`)
- Работает через расширение OpenGL 4.6 `glMultiDrawElementsIndirectCountARB` (`GL_ARB_indirect_parameters`):
  ```java
  GlCompat.multiDrawElementsIndirectCount(
      GL11.GL_TRIANGLES,
      GL11.GL_UNSIGNED_INT,
      run * COMMAND_BYTES_PER_REGION,
      (long) run * 8L,
      (runEnd - run) * MAX_COMMANDS_PER_REGION,
      20
  );
  ```
- Фактическое количество вызовов считывается GPU напрямую из `GL_PARAMETER_BUFFER`, заполняемого вычислительным шейдером `command_builder.comp`.

### 2.7 Вычислительный GPU-куллинг и пирамиды глубин Hi-Z

#### SIMD-пересечение с 6 плоскостями фрустума
Реализовано в `cull.glsl` (строки 35–40):
```glsl
bool _flw_testSphere(vec3 center, float radius) {
    bvec4 xyInside = greaterThanEqual(
        fma(flw_frustumPlanes.xyX, center.xxxx,
        fma(flw_frustumPlanes.xyY, center.yyyy,
        fma(flw_frustumPlanes.xyZ, center.zzzz, flw_frustumPlanes.xyW))),
        -radius.xxxx
    );
    bvec2 zInside = greaterThanEqual(
        fma(flw_frustumPlanes.zX, center.xx,
        fma(flw_frustumPlanes.zY, center.yy,
        fma(flw_frustumPlanes.zZ, center.zz, flw_frustumPlanes.zW))),
        -radius.xx
    );
    return all(xyInside) && all(zInside);
}
```
*Техническое примечание:* Благодаря упаковке коэффициентов плоскостей фрустума в транспонированные векторы (`xyX`, `xyY`, `xyZ`, `xyW` и `zX`, `zY`, `zZ`, `zW`), тест проверяет все 6 плоскостей фрустума ровно за 6 инструкций Fused Multiply-Add (FMA).

#### Проекция в пространство экрана и окклюзионное тестирование Hi-Z
Реализовано в `_flw_hizOccluder`:
1. **Консервативная проекция ограничивающей сферы**: Строит описывающий прямоугольник в пространстве экрана $[X_{\min}, Y_{\min}, X_{\max}, Y_{\max}]$ путем проецирования конуса силуэта сферы на ближнюю плоскость отсечения.
2. **Расчет MIP-уровня**:
   ```glsl
   ivec2 extent = rect.zw - rect.xy;
   int level = max(findMSB(max(extent.x, extent.y)), 0);
   level += any(greaterThan((rect.zw >> level) - (rect.xy >> level), ivec2(1))) ? 1 : 0;
   level = min(level, _flw_cullData.pyramidLevels);
   ```
3. **Сравнение окклюзии при Reversed-Z (CrankShaft / Minecraft 26.2)**:
   Считывает 4 текселя глубины, покрывающие проекцию на вычисленном уровне MIP через `texelFetch`:
   ```glsl
   float occluderDepth = min(min(depth00, depth01), min(depth10, depth11));
   float depthSphere = -_flw_cullData.znear / (center.z + radius);
   isVisible = isVisible && (depthSphere >= occluderDepth);
   ```
   *Архитектурное примечание о соглашениях по глубине:* Даунсемплинг с операцией `min()` и проверка `depthSphere >= occluderDepth` в CrankShaft строго опираются на конвейер проекции **Reversed-Z**, внедренный в Minecraft 26.2 (где $1.0$ соответствует ближней плоскости, а $0.0$ — дальней). В Minecraft 1.20.1 и 1.21.1 конвейер функционирует по стандарту **Forward-Z** ($0.0$ — ближняя, $1.0$ — дальняя, функция `GL_LEQUAL`). Как детально описано в Разделе 5.2, адаптация данного алгоритма для Nucleus требует инверсии редукции на `max()` и проверки `depthSphere_near > occluderDepth_max`, иначе видимость объектов окажется полностью инвертированной.

#### Субгрупповая поварповая компактизация
В `cull.glsl` (строки 185–242) CrankShaft устраняет конкуренцию потоков за атомики с помощью расширения `GL_KHR_shader_subgroup_ballot`:
```glsl
uvec4 ballot = subgroupBallot(visible);
uint count = subgroupBallotBitCount(ballot);
uint base = 0u;
if (count != 0u && subgroupElect()) {
    base = atomicAdd(_flw_models[modelIndex].instanceCount, count);
}
base = subgroupBroadcastFirst(base);
if (visible) {
    uint targetIndex = _flw_models[modelIndex].baseInstance + base + subgroupBallotExclusiveBitCount(ballot);
    _flw_instanceIndices[targetIndex] = objectUint;
}
```
*Прирост эффективности:* Вместо того чтобы 32 потока слали индивидуальные запросы `atomicAdd` в глобальную видеопамять VRAM, лидер варпа (`subgroupElect`) объединяет выжившие инстансы и выполняет **ровно одно атомарное сложение** на весь варп.

#### Пирамида глубин Single Pass Downsampler (SPD)
Класс `DepthPyramid.java` и шейдер `downsample_second.glsl`:
- Генерирует 6 MIP-уровней пирамиды глубин за один запуск compute-шейдера.
- Потоки сохраняют промежуточные выборки в разделяемой памяти LDS (`shared float[16][16] intermediate_memory`).
- Барьеры исполнения (`barrier()`) синхронизируют этапы внутри воркгруппы, сокращая нагрузку на полосу пропускания текстур на 80% по сравнению с рекурсивным многопроходным копированием (blitting).

### 2.8 Архитектура Task / Mesh шейдеров NVIDIA

При активации модуля `:meshlet` на видеокартах NVIDIA CrankShaft полностью обходит фиксированный конвейер выборки вершин:

```
[ Команда DrawMeshTasksIndirect ]
              │
              ▼
    [ Task Shader: task.task ]
    - Воркгруппа: 1 вызов на секцию чанка ландшафта
    - Декодирует метаданные секции и ограничивающие параллелепипеды граней
    - Оценивает направление взгляда относительно нормалей граней секции
    - Отсекает целиком кластеры тыльных граней
    - Вычисляет число видимых квадов и динамически генерирует воркгруппы мешей:
      gl_TaskCountNV = (quadCount * 2 + 31) / 32;
              │
              ▼ (Данные TerrainTask Payload)
    [ Mesh Shader: mesh.mesh ]
    - Воркгруппа: 32 потока на вызов (обрабатывает 16 квадов = 32 треугольника)
    - Кооперативная выборка вершин по 2 линиям:
        Линия 0 выбирает вершины 0 и 1
        Линия 1 выбирает вершины 2 и 3
    - Перестановка "бабочка" (subgroupShuffleXor) вычисляет границы кластера в регистрах
    - Субпиксельное отсечение вырожденных примитивов (< 1 пикселя в пространстве экрана)
    - Компактизация через subgroup ballot записывает выжившие вершины и примитивы:
      gl_PrimitiveIndicesNV[...] = ...
              │
              ▼
    [ Fragment Shader: frag.frag ]
    - Опциональный MESHLET_BARYCENTRIC: выборка атрибутов напрямую через 64-битные
      GPU Buffer Device Addresses (BDA) и gl_BaryCoordNV!
```

### 2.9 Управление памятью: промежуточные буферы (Staging), мульти-привязка SSBO и страничные слэбы

#### Персистентно отображаемый промежуточный буфер (`StagingBuffer.java`)
- Выделяет арену памяти размером 16 МиБ с использованием Direct State Access в OpenGL 4.5:
  ```java
  vbo = GL45C.glCreateBuffers();
  GL45C.glNamedBufferStorage(vbo, capacity, GL44C.GL_MAP_PERSISTENT_BIT | GL30C.GL_MAP_WRITE_BIT | GL44C.GL_CLIENT_STORAGE_BIT);
  map = GL45C.nglMapNamedBufferRange(vbo, 0, capacity, GL44C.GL_MAP_PERSISTENT_BIT | GL30C.GL_MAP_WRITE_BIT | GL30C.GL_MAP_FLUSH_EXPLICIT_BIT);
  ```
- **Загрузка через Compute Scatter**: Запросы на передачу фиксируются в `ScatterList` и исполняются на GPU шейдером `scatter.glsl` (строки 1–51), считывающим данные из SSBO отображаемого буфера и записывающим их в целевые VBO/SSBO.

#### Мульти-привязка OpenGL 4.4 (`IndirectBuffers.java`)
- Привязывает все необходимые буферы SSBO за один вызов драйвера через `nglBindBuffersRange`:
  - Привязка 0: `PAGE_FRAME_DESCRIPTOR`
  - Привязка 1: `INSTANCE` (данные атрибутов инстансов)
  - Привязка 2: `DRAW_INSTANCE_INDEX` (индексы выживших инстансов)
  - Привязка 3: `MODEL` (дескрипторы отрисовки моделей)
  - Привязка 4: `DRAW` (поток команд косвенной отрисовки)
  - Привязка 6: `VIS_WORDS` (битовая маска видимости)
  - Привязка 7: `MATRICES` (матрицы трансформации)
- Нативный блок размером 72 байта хранит дескрипторы, смещения и размеры, минимизируя накладные расходы JNI при обращении к драйверу.

#### Страничное хранилище инстансов (`ObjectStorage.java`, `GlSlab.java`)
- Инстансы группируются в страницы по 32 штуки (`PAGE_SIZE = 32`).
- Страницы по 32 инстанса идеально отображаются на 32-битные целочисленные битовые маски (`validBits`) и 32-поточные варпы GPU.
- Свободные слоты контролируются через пул целочисленных слэбов, а изменившиеся кадры отслеживаются через `BitSet changedFrames`.

#### Безопасность выбывания буферов (`BufferRetirement.java`)
- Предотвращает падения драйвера OpenGL, вызванные удалением буферов, на которые все еще ссылаются исполняемые кадры GPU или биндлесс-указатели. Удаление буферов ставится в очередь и выполняется строго после того, как фенсы соответствующих кадров перейдут в сигнальное состояние.

### 2.10 Убер-шейдеры, независимая от порядка прозрачность (OIT) и политика отката

#### Убер-шейдеры и динамическая сортировка
- CrankShaft упаковывает конфигурацию материала (порог альфа-отсечения cutout, уравнение тумана, отсечение тыльных граней, размытие, поведение мипмапов) в битовые поля структуры `MeshDrawCommand` (`packedFogAndCutout`, `packedMaterialProperties`).
- Убер-шейдеры динамически оценивают эти свойства, предотвращая переключения конвейеров.

#### Экосистема независимой от порядка прозрачности (OIT)
1. **Wavelet / Moment OIT (`WaveletOitChain.java`)**:
   - Представляет функцию пропускания света через тригонометрические моменты.
   - Выполняется за 4 прохода: `DEPTH_RANGE`, `GENERATE_COEFFICIENTS`, `EVALUATE` и `COMPOSITE`.
2. **Multi-Layer Alpha Blending (MLAB / A-Buffer / K-Buffer) (`GlInsertOitChain.java`)**:
   - Фрагменты вставляются в попиксельные связные списки в SSBO посредством атомарных операций и сортируются/резолвятся в `mlab_resolve.frag`.

#### Политика отката при наличии шейдерных модов
- Обрабатывалась в `ShadersModHelper.java` (`dev.engine_room.flywheel.lib.util.ShadersModHelper`, строки 11–32):
  Запрашивала `net.irisshaders.iris.api.v0.IrisApi.getInstance().isShaderPackInUse()`.
- При активном шейдерпаке Iris или OptiFine:
  `INSTANCING.supported` и `INDIRECT.supported` возвращали `false` (`Backends.java`, строки 35, 53).
- **CrankShaft полностью отключал свой движок**[^iris-update-2], переходя в режим `OFF_BACKEND` (`"flywheel:off"`), и передавал весь рендеринг ванильным непакетированным диспетчерам BER в режиме непосредственного исполнения.

### 2.11 Указатель ключевых файлов и классов

| Подсистема | Точный путь к файлу | Ключевые классы и методы |
|---|---|---|
| **Ядро конвейера** | `common/src/backend/java/dev/engine_room/flywheel/backend/engine/EngineImpl.java` | `EngineImpl.render`, `renderOrigin`, `sqrMaxOriginDistance` |
| **Координация отрисовки** | `common/src/backend/java/dev/engine_room/flywheel/backend/engine/DrawManager.java` | `DrawManager.createFramePlan`, `initializationQueue` |
| **Косвенный конвейер** | `common/src/backend/java/dev/engine_room/flywheel/backend/engine/indirect/IndirectDrawManager.java` | `dispatchCull`, `dispatchApply`, `submitSolid`, `UberDraw` (строки 239–281, 714–720) |
| **Пул мешлетов** | `common/src/backend/java/dev/engine_room/flywheel/backend/engine/MeshPool.java` | `MESHLET_TRIS = 64`, `writeMeshletBounds` (строки 22, 184–228) |
| **Стейджинг-арена** | `common/src/backend/java/dev/engine_room/flywheel/backend/engine/indirect/StagingBuffer.java` | `glNamedBufferStorage`, `nglMapNamedBufferRange`, `STORAGE_FLAGS` (строки 20–22, 73–76) |
| **Мульти-привязка SSBO** | `common/src/backend/java/dev/engine_room/flywheel/backend/engine/indirect/IndirectBuffers.java` | `nglBindBuffersRange`, `multiBindBlock` (строки 150–154) |
| **Пирамида глубин Hi-Z**| `common/src/backend/java/dev/engine_room/flywheel/backend/engine/indirect/DepthPyramid.java` | `DepthPyramid.generate`, `bindForCull` |
| **Диспетчер ландшафта** | `common/src/backend/java/dev/engine_room/flywheel/backend/engine/terrain/TerrainDrawDispatcher.java` | `multiDrawElementsIndirectCount`, `COMMAND_BYTES_PER_REGION` (строки 725–727) |
| **Шейдер куллинга** | `common/src/backend/resources/assets/flywheel/flywheel/internal/indirect/cull.glsl` | `_flw_testSphere`, `_flw_hizOccluder`, `subgroupBallot` (строки 35–40, 185–242) |
| **Шейдер скаттера** | `common/src/backend/resources/assets/flywheel/flywheel/internal/indirect/scatter.glsl` | Вычислительное копирование стейджинга в SSBO (строки 1–51) |
| **Меш-шейдеры** | `meshlet/src/main/resources/assets/meshlet/flywheel/terrain/gl/mesh.mesh` | `gl_TaskCountNV`, `subgroupShuffleXor`, `gl_PrimitiveIndicesNV` |
| **Интероп с шейдерами** | `common/src/lib/java/dev/engine_room/flywheel/lib/util/ShadersModHelper.java` | `isShaderPackInUse`, рефлексия к Iris API (строки 11–32) |
| **Конфиг бэкендов** | `common/src/backend/java/dev/engine_room/flywheel/backend/Backends.java` | Проверка `!ShadersModHelper.isShaderPackInUse()` (строки 35, 53) |

## 3. Детальный разбор архитектуры Nucleus (com.hbm_m.client.render)

### 3.1 Базовая архитектура и последовательность вызовов жизненного цикла кадра

Nucleus перехватывает фазы рендеринга мира Minecraft через события `RenderLevelStageEvent` (Forge / NeoForge), управляя полным жизненным циклом кадра через класс `InstancedRenderFrame.java`:

```
[LevelRenderer / События стадий рендеринга]
       │
       ├─► Стадия: AFTER_ENTITIES (`InstancedRenderFrame.onBeforeBlockEntities`)
       │      ├─► RenderFrameLight.onFrameStart() -> Гарантирует обновление LightTexture не более 1 раза за кадр
       │      ├─► ClientRenderFlags.onFrameStart() -> Захватывает состояние отладки и камеры
       │      ├─► IrisShadowBatchCollector.onMainPassFrameStart() -> Сбрасывает сборщик теней
       │      ├─► OcclusionCullingHelper.captureBlockEntityPassFrustum() -> Захватывает фрустум камеры
       │      └─► NucleusDispatcherBypass.collectMain()
       │             └─► Перебирает плоский список `LinkedHashSet<BlockEntity> LIVE`
       │             └─► Проверяет дистанцию отсечения, готовит позы, пушит инстансы
       │
       ├─► Диспетчеризация BlockEntity: MachineBer.render() / collectRender()
       │      ├─► Если обработан в обходе: немедленный выход (mainCollected == true)
       │      ├─► OcclusionCullingHelper.shouldRender() -> Трассировка 15 лучей / временной кэш
       │      ├─► RenderDistanceHelper.computeStaticFade() -> Расчет альфы затухания (30% зона)
       │      └─► InstancedStaticPartRenderer.addInstance()
       │             ├─► Разложение матрицы modelview на мировые координаты и вращение
       │             ├─► LightSampleCache: трилинейная выборка света по 8 углам (сдвиг 1.5 см)
       │             └─► recordMatchesBuffer() -> Проверка неизменности для пропуска записи
       │
       └─► Стадия: AFTER_BLOCK_ENTITIES (`InstancedRenderFrame.presentAfterBlockEntities`)
              ├─► MdiBatchCoordinator.beginFrame()
              ├─► MachineRenderRegistry.flushAll()
              │      └─► InstancedStaticPartRenderer.flush()
              │             ├─► Ванильный конвейер: MdiBatchCoordinator.submit()
              │             │      └─► Запасной путь: VanillaInstancedBatchRenderer.drawInstanceRange()
              │             └─► Конвейер Iris: IrisInstancedBatchRenderer.flushBatchIris()
              ├─► MdiBatchCoordinator.endFrame()
              │      └─► GpuSpanUploader.uploadInstancesToAtlas() -> Загрузка измененных срезов памяти
              │      └─► glMultiDrawElementsIndirect() -> Отрисовка непрозрачных и затухающих батчей
              ├─► Фаза 2: InstancedRenderFrame.flushAllInstancedFading() -> Сортированный проход прозрачности
              ├─► PersistentUploadStaging.endFrame() -> Установка фенса синхронизации GL
              └─► MdiRenderFrameGate.advanceAfterPresent()
```

### 3.2 Координатор батчинга MDI (`MdiBatchCoordinator`)
**Файл:** `src/main/java/com/hbm_m/client/render/MdiBatchCoordinator.java`

`MdiBatchCoordinator` объединяет вызовы отрисовки всех зарегистрированных рендереров частей:

#### Определение возможностей GPU и иерархия деградации
- Вычисляется в методе `ensureCapsResolved()` (строки 264–310).
- Проверяет `GLCapabilities` на поддержку `glMultiDrawElementsIndirect` / `GL_ARB_multi_draw_indirect` и `glDrawElementsInstancedBaseVertexBaseInstance` (`hasBaseInstance`).
- **Многоуровневая деградация**:
  1. Полный MDI-атлас (`glMultiDrawElementsIndirect`)
  2. Цикл единичных команд косвенной отрисовки (`glDrawElementsIndirect` в цикле на CPU)
  3. Прямые инстансированные массивы (`glDrawElementsInstanced` в `VanillaInstancedBatchRenderer`)
  4. Немедленный откат к ванильному режиму (Blaze3D `BufferBuilder`)

#### Структура и страйд косвенных команд
- Размер упакованной команды: `INDIRECT_CMD_PACKED_BYTES = 20`
- Аппаратный страйд выравнивания: `INDIRECT_CMD_STRIDE_BYTES = 32` (строки 89–94)
- Структура команды в `ByteBuffer cmdBuf` (строки 1021–1050):
  - `count` (uint32, 4 байта): Количество индексов детали
  - `instanceCount` (uint32, 4 байта): Общее число инстансов (или 1 для индивидуальных слотов затухания)
  - `firstIndex` (uint32, 4 байта): Смещение элемента в индексном буфере (`firstIndexBytes >>> 2`)
  - `baseVertex` (int32, 4 байта): Базовое смещение вершин в атласе
  - `baseInstance` (uint32, 4 байта): Начальный индекс в массиве инстансов
  - *Аппаратный паддинг*: 12 байт нулей для сохранения 32-байтного выравнивания.

#### Двухфазное разделение затухания по варианту G (Variant G)
Для предотвращения ситуации, когда полупрозрачная затухающая геометрия преждевременно пишет в буфер глубины и перекрывает непрозрачные части:
1. `partitionOpaqueFirst(Pending p)` делит инстансы на `[0, opaqueCount)` с $\text{fade} \ge 0.99$ и `[opaqueCount, instanceCount)` с $\text{fade} < 0.99$.
2. Все непрозрачные команды (`opaqueSubs`) отправляются первыми, записывая геометрию в буфер глубины с включенной записью: `glDepthMask(true)`.
3. Затухающие инстансы (`fadeSlots`) собираются, сортируются глобально от дальних к ближним по квадрату расстояния до камеры (`distSq`) и отправляются следом.

#### Повторное использование чистых кадров (`submitClean`)
- Строки 698–718: Если детали механизма не изменили позицию, угол поворота или освещение, вызывается `submitClean(renderer, indexCount, instanceCount)`.
- Проверяется условие `renderer.mdiBufferSynced && !renderer.mdiRecordWriteHappened`.
- Если кадр «чистый», снапшот и косвенные команды предыдущего кадра переиспользуются напрямую. Побайтовое дифференцирование памяти, копирование и передача на GPU полностью исключаются.

#### Межкадровый кэшированный перерендер (`redrawCachedIfAny`)
- Строки 409–480: Тайловые сущности обновляются с частотой 20 Гц, однако мониторы рендерят картинку на 144+ Гц.
- `publishCachedRedraw` кэширует полное состояние команд MDI. В промежуточных кадрах, когда тайлы не тикали, `redrawCachedIfAny` повторно отправляет кэшированный командный буфер с обновленной матрицей проекции камеры, устраняя затраты CPU на повторную подготовку отрисовки.

### 3.3 Подсистема геометрического атласа (`MdiGeometryAtlas`)
**Файл:** `src/main/java/com/hbm_m/client/render/MdiGeometryAtlas.java`

`MdiGeometryAtlas` управляет разделяемыми буферами GPU для всех деталей многоблочных машин:

#### Начальное выделение и типы буферов
- Вершинный VBO (`vertexVboId`): начальный размер 256 КиБ, `GL_STATIC_DRAW`
- Индексный EBO (`indexEboId`): начальный размер 64 КиБ, `GL_STATIC_DRAW`
- VBO данных инстансов (`instanceVboId`): 4096 инстансов (30 float $\times$ 4 байта = 491 520 байт), `GL_STREAM_DRAW`
- Буфер косвенных команд (`indirectBufId`): 4096 команд $\times$ 32 байта = 131 072 байта, `GL_STREAM_DRAW`
- Мастер-VAO (`vaoId`): инкапсулирует все привязки атрибутов.

#### Спецификация компоновки вершин и атрибутов инстанса
- **Формат вершин (Страйд = 36 байт)**:
  - Локация 0: `Position` (`vec3`, `GL_FLOAT`, смещение 0)
  - Локация 1: `Normal` (`vec3`, `GL_FLOAT`, смещение 12)
  - Локация 2: `UV0` (`vec2`, `GL_FLOAT`, смещение 24)
  - Локация 3: `BoneId` (`int`, `GL_INT`, смещение 32 через `glVertexAttribIPointer`)
- **Формат инстанса (Страйд = 120 байт / 30 float, Divisor = 1)**:
  - Локация 4: `InstPos` (`vec3`, смещение 0)
  - Локация 5: `InstRot` (`vec4` кватернион, смещение 12)
  - Локация 6: `InstBboxMin` (`vec3`, смещение 28)
  - Локация 7: `InstBboxSize` (`vec4`: $xyz = \text{размер}, w = \text{альфа затухания}$, смещение 40)
  - Локация 8: `InstLightC01` (`vec4`: UV углов 0 и 1, смещение 56)
  - Локация 9: `InstLightC23` (`vec4`: UV углов 2 и 3, смещение 72)
  - Локация 10: `InstLightC45` (`vec4`: UV углов 4 и 5, смещение 88)
  - Локация 11: `InstLightC67` (`vec4`: UV углов 6 и 7, смещение 104)

#### Динамический рост и переупаковка геометрии
- Строки 522–567: При загрузке новых моделей машин метод `repackGeometryAndRefreshSlots()` удваивает емкость буферов.
- Он выполняет обход зарегистрированных рендереров в `geometryByRenderer` (`LinkedHashMap`), непрерывно перезагружает геометрию сеток и обновляет дескриптор `Slot(baseVertex, firstIndexBytes, indexCount)` каждого рендерера.

### 3.4 Рендереры составных частей и модель памяти (`InstancedStaticPartRenderer`)
**Файл:** `src/main/java/com/hbm_m/client/render/InstancedStaticPartRenderer.java`

Каждая составная деталь OBJ-модели управляется экземпляром `InstancedStaticPartRenderer`:

#### Безопасность нативной памяти и Java Cleaner
- Память выделяется вне кучи (off-heap) через `MemoryUtil.memAllocFloat(maxInstances * 30)`.
- Связан с механизмом `java.lang.ref.Cleaner` (`instanceBufferCleanable`), гарантирующим освобождение при сборке мусора и предотвращающим утечки нативной памяти.

#### Преобразование в мировое пространство (`convertToWorldRecord`)
- Строки 377–382: Преобразует текущую матрицу из `PoseStack` в абсолютные мировые координаты:
  ```java
  tmpWorldMat.set(FrameViewState.inverseViewRotation()).mul(composed);
  tmpWorldMat.getTranslation(posTmp);
  posTmp.add(FrameViewState.camX(), FrameViewState.camY(), FrameViewState.camZ());
  tmpWorldMat.getNormalizedRotation(rotTmp);
  ```
- Хранение абсолютных мировых координат делает записи инстансов инвариантными к движению камеры. Когда игрок поворачивает голову или идет, данные буфера инстансов остаются идентичными, что делает возможным повторное использование кадров с нулевыми затратами на загрузку.

#### Пропуск перезаписи с учетом допусков (`recordMatchesBuffer`)
- Строки 426–455: При добавлении инстанса новые значения сравниваются с уже хранящимися в `instanceBuffer`:
  - Допуск по позиции: $\epsilon_{\text{pos}} = 10^{-4}$
  - Допуск по кватерниону: $\epsilon_{\text{rot}} = 10^{-5}$
  - Альфа затухания: квантуется до 8 бит ($\text{round}(\alpha \cdot 255) / 255$)
  - Допуск по карте света: $\epsilon_{\text{light}} = 0.5$
- Если изменения не превышают допусков, физическая запись в память пропускается, а флаг `mdiRecordWriteHappened` остается `false`.

### 3.5 Персистентный стейджинг и дифференциальная загрузка диапазонов 2-го уровня (Span-Diff)

#### Персистентный кольцевой буфер (`PersistentUploadStaging.java`)
- Строки 47, 106–110: Выделяет 4 МиБ персистентно отображаемой кольцевой памяти:
  ```java
  GL44.glBufferStorage(GL15.GL_ARRAY_BUFFER, capacityBytes,
      GL44.GL_MAP_WRITE_BIT | GL44.GL_MAP_PERSISTENT_BIT | GL44.GL_MAP_COHERENT_BIT);
  ByteBuffer mapped = GL30.glMapBufferRange(GL15.GL_ARRAY_BUFFER, 0, capacityBytes,
      GL30.GL_MAP_WRITE_BIT | GL44.GL_MAP_PERSISTENT_BIT | GL44.GL_MAP_COHERENT_BIT);
  baseAddr = MemoryUtil.memAddress(mapped);
  ```
- **Нулевые накладные расходы на сброс**: Благодаря флагу `GL_MAP_COHERENT_BIT` записи CPU автоматически становятся видимыми для GPU без явного вызова `glFlushMappedBufferRange`.
- **Синхронизация через фенсы**: В `endFrame()` выставляется `GL32.glFenceSync`. Перед записью в следующий сегмент кольца метод `waitForRange` опрашивает фенс через `glClientWaitSync`.

#### Дифференциация диапазонов 2-го уровня (`GpuSpanUploader.java`)
- Строки 34–36, 56–102: Содержит теневой буфер на CPU (`vboShadow`).
- При загрузке метод `diffUpload()` сравнивает входящие данные инстансов с теневой копией:
  - Находит непрерывные измененные («грязные») интервалы.
  - Объединяет неизмененные промежутки меньше `MERGE_GAP_FLOATS = 24` (96 байт) в единый интервал загрузки.
  - Если общее число диапазонов превышает `MAX_SPANS = 48`, переключается на загрузку всего окна целиком.
  - Для механизмов с вращающимися частями (например, паровых турбин) **загружается только 16-байтный кватернион вращения**, что снижает нагрузку на шину памяти на 86.7% на каждый инстанс.

```
Входящие данные: [ Позиция: 12B ] [ Поворот: 16B (ИЗМЕНЕН) ] [ Bbox: 28B ] [ Свет: 64B ]
Теневой буфер:   [ Позиция: 12B ] [ Поворот: 16B (СТАРЫЙ)  ] [ Bbox: 28B ] [ Свет: 64B ]
                         │                     │                     │            │
Сравнение:           СОВПАДАЕТ            РАЗЛИЧАЕТСЯ            СОВПАДАЕТ    СОВПАДАЕТ
                         │                     │                     │            │
Действие:             Пропуск              ЗАГРУЗКА 16B           Пропуск      Пропуск
```

### 3.6 4-уровневый конвейер интероперабельности с шейдерами Iris / Oculus

При включении шейдерпаков Nucleus переключается на `IrisInstancedBatchRenderer.java`:

```
+----------------------------------------------------------------------------------------------------+
|                                  4-УРОВНЕВАЯ АРХИТЕКТУРА ИНТЕРОПЕРАБЕЛЬНОСТИ                       |
+--------+-----------------------+-----------------------------+-------------------------------------+
| Уровень| Подсистема            | Целевые шейдерпаки          | Механизм исполнения                 |
+--------+-----------------------+-----------------------------+-------------------------------------+
| Tier 1 | GPU Compute Bake      | Универсальный (BSL, Photon, | Вычислительный шейдер GLSL 4.3      |
|        | (NucleusGpuBaker)     | Complementary, Bliss)       | запекает вершины в VRAM; отрисовка  |
|        |                       |                             | через нативную программу шейдерпака.|
+--------+-----------------------+-----------------------------+-------------------------------------+
| Tier 2 | Instanced Extended-   | Распознанные схемы          | Кастомный ExtendedShader с 30-float |
|        | Shader (IrisInstanced)| (напр., Photon unorm)       | атрибутами + дисторсия теней.       |
+--------+-----------------------+-----------------------------+-------------------------------------+
| Tier 3 | Persistent Batch Loop | Сложные нераспознанные паки | Персистентный цикл IrisRenderBatch; |
|        | (IrisRenderBatch)     |                             | единичный вызов shader.apply().     |
+--------+-----------------------+-----------------------------+-------------------------------------+
| Tier 4 | Немедленный откат BER | Неподдерживаемое железо     | Традиционная поштучная отрисовка    |
|        |                       | (macOS GL 4.1, старые iGPU) | через ванильный конвейер.           |
+--------+-----------------------+-----------------------------+-------------------------------------+
```

### 3.7 Вычислительный GPU-бейкер (`NucleusGpuBaker`)
**Файл:** `src/main/java/com/hbm_m/client/render/NucleusGpuBaker.java`

`NucleusGpuBaker` выполняет вычислительный шейдер GLSL 4.3 для запекания геометрических трансформаций инстансов непосредственно в видеопамяти:

#### Конфигурация привязок SSBO
- Привязка 0 (`MeshBuf`): буфер вершин сетки-компаньона (read-only) в формате `IrisVertexFormats.ENTITY`.
- Привязка 1 (`InstBuf`): буфер 30-float дескрипторов инстансов (read-only).
- Привязка 2 (`OutBuf`): буфер трансформированных вершин (write-only), готовый к растеризации.

#### Вычислительный конвейер GLSL (`строки 643–750`)
```glsl
#version 430 core
layout(local_size_x = 64) in;

layout(std430, binding = 0) readonly restrict buffer MeshBuf { float meshData[]; };
layout(std430, binding = 1) readonly restrict buffer InstBuf { float instData[]; };
layout(std430, binding = 2) writeonly restrict buffer OutBuf { float outData[]; };

uint gid = gl_GlobalInvocationID.x;
uint inst = gid / uint(uVertCount);
uint lv = gid - inst * uint(uVertCount);

// 1. Трансформация позиции вершины
vec3 pos = vec3(meshData[mBase + uOffPos + 0], meshData[mBase + uOffPos + 1], meshData[mBase + uOffPos + 2]);
vec4 rot = vec4(instData[iBase + 3], instData[iBase + 4], instData[iBase + 5], instData[iBase + 6]);
vec3 outPos = quatRotate(rot, pos) + vec3(instData[iBase + 0], instData[iBase + 1], instData[iBase + 2]);
if (uBakeMode == 1) outPos -= uCamPos; // Относительное смещение камеры для основного прохода

// 2. Трансформация вектора нормали
vec3 nrm = decodeNormal(meshData[mBase + uOffNormal]);
vec3 nOut = normalize(quatRotate(rot, nrm));
outData[oBase + uOffNormal] = packNormal(nOut);

// 3. Трилинейная интерполяция карты света по 8 углам
vec3 w = clamp((pos - bboxMin) / bboxSize, 0.0, 1.0);
vec2 lm = trilinearInterpolate(w, instLightCorners);
outData[oBase + uOffUv2] = packUv2(lm);
```

#### Синхронизация памяти и отрисовка
- Барьер памяти: `GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT | GL43.GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT)`.
- Индексный буфер размножается для адресации смещенных вершинных интервалов.
- Отрисовка выполняется одним вызовом `glDrawElements` через **активную нативную программу шейдерпака**, что обеспечивает 100% совместимость с авторскими моделями освещения.

### 3.8 Глобальный сборщик теневого батча и компенсация дисторсии теней

#### Глобальный сборщик теней (`IrisShadowBatchCollector.java`)
- Строки 117–166: Во время прохода теней метод `record()` захватывает позы инстансов и значения освещения в предварительно выделенные нативные буферы.
- Точка внедрения: миксин Iris перед вызовом `ShadowRenderer.copyPreTranslucentDepth`.

#### Компенсация дисторсии теней (`IrisShadowDistortion.java`)
Шейдерпаки накладывают нелинейные искажения на карту теней для оптимизации разрешения вблизи игрока. Отрисовка инстансированной геометрии без учета этих искажений приводит к отрыву теней от объектов:
- Строки 59–105: Nucleus анализирует исходный код вершинного шейдера теней Iris для определения типа дисторсии:
  - **Квартичная дисторсия** (семейство Photon):
    $$f(r) = r^4 \cdot D + (1 - D), \quad z' = z \cdot S_{\text{depth}}$$
  - **Линейная дисторсия** (семейство BSL / Complementary):
    $$f(r) = r \cdot \text{bias} + (1 - \text{bias}), \quad z' = z \cdot 0.2$$
  - **Без искажений**: стандартное ортографическое пространство источника света.
- На Уровне 2 `IrisInstancedShaders` применяет выявленную формулу в своем вершинном шейдере, идеально совмещая тени с геометрией мира.

#### Защита от переиспользования Program ID драйверами NVIDIA
В `IrisExtendedShaderAccess.java` (строки 114–181):
- При перезагрузке шейдеров драйверы NVIDIA часто повторно назначают освободившиеся целочисленные идентификаторы программ OpenGL (`programId`) новым скомпилированным шейдерам.
- Кэширование локаций uniform-переменных исключительно по `programId` приводит к несоответствию типов и падению драйвера, если новая программа имеет другую структуру переменных.
- Nucleus инкрементирует атомарный счетчик `pipelineGeneration` при каждой перезагрузке шейдеров, объединяя его с `programId` для безопасной инвалидации кэша локаций.

### 3.9 Архитектура волюметрического кэширования карт освещения (`LightSampleCache`)
**Файл:** `src/main/java/com/hbm_m/client/render/LightSampleCache.java`

Наивная пространственная выборка света для сложных многоблочных структур (до 11 деталей на один Advanced Assembler) способна отнимать до 17% процессорного времени кадра:

- **Быстрый путь последнего блока (`lastQueriedBE`)**: Детали одного механизма отрисовываются последовательно. Nucleus кэширует ссылку на последний запрошенный BlockEntity, возвращая готовые значения без повторного вычисления хэшей.
- **Сглаживание по 6 кардинальным направлениям**: Выборка производится в 6 точках снаружи ограничивающего объема с отбрасыванием точек, попавших в непрозрачные блоки, что предотвращает затемнение оснований машин, заглубленных в пол.
- **Внутренний сдвиг (`SAMPLE_INSET = 1.0f / 64.0f`)**: Когда габариты машины совпадают с границами целых блоков, погрешности чисел float приводят к мерцанию освещения между воздухом и соседним твердым блоком. Сдвиг точек выборки на 1.5 см внутрь блока полностью устраняет артефакты мерцания.
- **`RenderFrameLight.java`**: Гарантирует выполнение `LightTexture.updateLightTexture` не более одного раза за кадр.

### 3.10 Иерархия отсечения и видимости (Frustum, Ray-March, Fade LOD)

- **`CpuFrustumCuller.java`**: Высокоскоростное отсечение по 6 плоскостям фрустума алгоритмом Грибба/Хартмана. Извлекает уравнения плоскостей из матрицы $P \cdot V$ и проверяет вершины AABB через 6 скалярных произведений.
- **`OcclusionCullingHelper.java`**: Трассирует до 15 лучей (1 центр, 8 углов, 6 центров граней) алгоритмом Брезенхема в 3D против `BlockState.isSolidRender()`. Кэширует результат видимости в `Long2ObjectOpenHashMap<CachedResult>` с межкадровым переиспользованием при смещении камеры менее $0.25\text{ м}^2$ за 20 тиков. Автоматически отключает куллинг для движущихся конструкций Create и во время прохода теней.
- **`RenderDistanceHelper.java`**: Рассчитывает плавное дистанционное затухание (LOD Fade) в пределах пропорциональной 30% зоны затухания (`FADE_ZONE_FRACTION = 0.3`, диапазон от 16 до 64 блоков).

### 3.11 Рабочий процесс составных OBJ-моделей и обход стандартного диспетчера

- **`PartGeometry.java`**: Конвертирует составные части OBJ-моделей в нативные вершинные буферы GPU с детерминированным сидом запекания (`BAKE_SEED = 42L`). Разворачивает обертки Fabric Rendering API (FRAPI) и светящиеся слои Continuity.
- **`LegacyAnimator.java` и `MachineBer.java`**: Сохраняют API анимаций оригинального мода 1.7.10, управляя матрицами `PoseStack`. Кэшируют разностные матрицы кадров (`animDelta`), переиспользуя их между основным проходом и проходом теней.
- **`NucleusDispatcherBypass.java`**: Полностью исключает затратный поблочный обход BER в Sodium/Embeddium. Активные машины регистрируются в плоском множестве `LinkedHashSet<BlockEntity> LIVE`. Во время события `AFTER_ENTITIES` метод `collectMain()` обходит этот список напрямую и помечает механизмы как собранные, благодаря чему миксин отменяет визиты Sodium к чанкам. Это сокращает процессорное время кадра на **25.4%** на крупных индустриальных базах.

### 3.12 Указатель ключевых файлов и классов

| Подсистема | Точный путь к файлу | Ключевые классы и методы |
|---|---|---|
| **Координатор MDI** | `src/main/java/com/hbm_m/client/render/MdiBatchCoordinator.java` | `submitClean`, `partitionOpaqueFirst`, `redrawCachedIfAny`, `executeMdiGlDraw` |
| **Геометрический атлас** | `src/main/java/com/hbm_m/client/render/MdiGeometryAtlas.java` | `INSTANCE_FLOATS = 30`, `repackGeometryAndRefreshSlots`, `Slot` |
| **Рендерер деталей** | `src/main/java/com/hbm_m/client/render/InstancedStaticPartRenderer.java` | `convertToWorldRecord`, `recordMatchesBuffer`, `instanceBufferCleanable` |
| **Запасной батчер** | `src/main/java/com/hbm_m/client/render/VanillaInstancedBatchRenderer.java` | `flushBatchVanilla`, `cachedPipelineGeneration`, `applyCommonUniforms` |
| **Стейджинг загрузки**| `src/main/java/com/hbm_m/client/render/PersistentUploadStaging.java` | `glBufferStorage`, `glMapBufferRange`, `GL_MAP_COHERENT_BIT`, `glFenceSync` |
| **Загрузчик срезов** | `src/main/java/com/hbm_m/client/render/GpuSpanUploader.java` | `diffUpload`, `MAX_SPANS = 48`, `MERGE_GAP_FLOATS = 24`, `vboShadow` |
| **Сетка-компаньон** | `src/main/java/com/hbm_m/client/render/IrisCompanionMesh.java` | Синтез `IrisVertexFormats.ENTITY`, веса 8 углов, динамический VBO света |
| **Инстансинг Iris** | `src/main/java/com/hbm_m/client/render/IrisInstancedBatchRenderer.java` | `flushBatchIris`, 4-уровневая логика делегирования |
| **GPU Compute Baker** | `src/main/java/com/hbm_m/client/render/NucleusGpuBaker.java` | Вычислительный шейдер GLSL 4.3, привязки SSBO 0/1/2, `bakeAndDrawMain` |
| **Сборщик теней** | `src/main/java/com/hbm_m/client/render/IrisShadowBatchCollector.java` | `flushGlobalShadowBatch`, сохранение матриц, `record` |
| **Дисторсия теней** | `src/main/java/com/hbm_m/client/render/shader/IrisShadowDistortion.java` | Определение квартичной (Photon) и линейной (BSL) дисторсии |
| **Кэш карт освещения**| `src/main/java/com/hbm_m/client/render/LightSampleCache.java` | `lastQueriedBE`, `SAMPLE_INSET = 1/64`, трилинейная выборка по 8 углам |
| **Куллинг фрустума** | `src/main/java/com/hbm_m/client/render/culling/CpuFrustumCuller.java` | Извлечение 6 плоскостей Грибба/Хартмана и проверка AABB |
| **Окклюзия лучами** | `src/main/java/com/hbm_m/client/render/culling/OcclusionCullingHelper.java` | Марш 15 лучей, временной кэш, защита конструкций Create и теней |
| **Дистанционный LOD** | `src/main/java/com/hbm_m/client/render/RenderDistanceHelper.java` | `FADE_ZONE_FRACTION = 0.3`, расчет зоны затухания |
| **Обход диспетчера** | `src/main/java/com/hbm_m/client/render/NucleusDispatcherBypass.java` | Итерация по плоскому `LIVE`, отмена обхода BER в Sodium, экономия 25.4% CPU |
| **Платформенные хуки**| `src/main/java/com/hbm_m/platform/RenderHooks.java` | Кросс-версионная абстракция `BufferBuilder`, `MeshData`, матричных стеков |

---

## 4. Комплексный сравнительный анализ по 7 доменам

### 4.1 Главная сравнительная матрица

```
+-----------------------------------------------------------------------------------------------------------------------------------+
|                                            КОМПЛЕКСНАЯ СРАВНИТЕЛЬНАЯ МАТРИЦА ПО 7 ДОМЕНАМ                                         |
+----------------------+----------------------------------------------------+-------------------------------------------------------+
| Домен                | CrankShaft 26.2 (Flywheel / Meshlet)               | Nucleus (HBM-Modernized)                              |
+----------------------+----------------------------------------------------+-------------------------------------------------------+
| 1. Конвейер и поток  | - Двухфазный GPU compute-куллинг и проходы MDI     | - Скоординированный CPU одиночный проход MDI + фейд   |
|    выполнения        | - Нулевое обратное чтение на CPU, синхр. фенсами   | - Переиспользование списков в чистых кадрах           |
|                      | - Построен на Blaze3D RHI (Minecraft 26.2)         | - Межкадровый кэшированный перерендер на частоте монитора|
|                      |                                                    | - Построен на OpenGL 3.2 Core (Forge 1.20 / Neo 1.21) |
+----------------------+----------------------------------------------------+-------------------------------------------------------+
| 2. MDI и батчинг     | - glMultiDrawElementsIndirect (команда 48 байт)    | - glMultiDrawElementsIndirect (команда 32 байта)      |
|                      | - glMultiDrawElementsIndirectCount (ландшафт)      | - Запасной цикл с glDrawElementsIndirect              |
|                      | - Буфер косвенных команд управляется compute GPU   | - Монолитный MdiGeometryAtlas (базовый пул 256 КиБ)   |
|                      | - Динамическая сортировка UBER_DRAW_COMPARATOR     | - Двухфазное разделение по варианту G (Opaque / Fade) |
+----------------------+----------------------------------------------------+-------------------------------------------------------+
| 3. Память и стейджинг| - Персистентный кольцевой буфер 16 МБ              | - Персистентный когерентный кольцевой буфер 4 МБ (GL 4.4)|
|                      | - Жестко завязан на OpenGL 4.5 DSA                 | - Плавная деградация до glBufferSubData               |
|                      | - Вычислительный шейдер scatter.glsl для копирования| - Дифференциация диапазонов 2 уровня (срезы по 16 байт)|
|                      | - Мульти-привязка SSBO (7 буферов за 1 вызов)      | - Теневой буфер CPU (vboShadow), 0 байт на статике    |
|                      | - Страничная память слэбов (32 инстанса / страница)| - Неразрезанные 30-float атрибуты VBO на инстанс      |
+----------------------+----------------------------------------------------+-------------------------------------------------------+
| 4. Система куллинга  | - Чистый вычислительный GPU-куллинг (cull.glsl)    | - Легковесный 6-плоскостной фрустум Грибба/Хартмана   |
|                      | - SIMD-пересечение сфера-фрустум (6-FMA)           | - Марш 15 лучей воксельной окклюзии на CPU            |
|                      | - Пирамида глубин Hi-Z (Single Pass Downsampler)   | - Межкадровый временной кэш (смещение <0.25 м²)       |
|                      | - Компактизация через subgroup ballot (1 на варп)  | - Пропорциональный 30% расчет дистанционного фейда    |
+----------------------+----------------------------------------------------+-------------------------------------------------------+
| 5. Интероперабельность| - СТРОГИЙ ОТКАЗ: Деактивация движка при активных   | - НАТИВНАЯ 4-УРОВНЕВАЯ АРХИТЕКТУРА: Полная поддержка  |
|    с шейдерами       |   шейдерпаках (isShaderPackInUse() == true)[^iris-update-3]| - Уровень 1: NucleusGpuBaker GLSL 4.3 compute-бейк|
|    (Iris / Oculus)   | - Нулевая поддержка кастомных теней и G-buffer[^iris-update-4]| - Уровень 2: Инстансинг ExtendedShader + дисторсия |
|                      | - Откат к поштучной ванильной отрисовке через BER  | - Уровень 3: Персистентный IrisRenderBatch (1 apply)  |
|                      |                                                    | - Уровень 4: Немедленный откат к BER                  |
|                      |                                                    | - Защита от переиспользования Program ID на NVIDIA    |
+----------------------+----------------------------------------------------+-------------------------------------------------------+
| 6. Динамическая      | - Универсальные визуалы с матричными индексами     | - Составные деревья моделей Wavefront OBJ (PartGeom.) |
|    геометрия         | - Построение плана обновления на CPU (ForEachPlan) | - Фасад LegacyAnimator (паритет с API NTM 1.7.10)     |
|    и анимации        | - Task/Mesh шейдеры мешлетов (NV_mesh_shader)      | - Кэширование дельта-матриц PoseStack между проходами |
|                      | - Сваривание вершин и барицентрическая выборка     | - NucleusDispatcherBypass: пропуск обхода BER в Sodium|
|                      |                                                    |   (снижение нагрузки на CPU до 25.4% на мега-базах)   |
+----------------------+----------------------------------------------------+-------------------------------------------------------+
| 7. Производительность| - Минимальные накладные расходы драйвера на NVIDIA | - Исключительная разгрузка CPU на сложных многоблоках |
|    и оборудование    | - Серьезная деградация производительности на Intel | - Сбалансированная кросс-вендорная стабильность       |
|                      | - Несовместим с Apple / macOS (требуется GL 4.5)   | - Полная поддержка macOS и Intel через деградацию     |
|                      | - Катастрофическое падение FPS с Iris (сброс в BER)| - Нулевой трафик шины PCIe на статичных сценах        |
+----------------------+----------------------------------------------------+-------------------------------------------------------+
```

### 4.2 Домен 1: Архитектура конвейера и поток выполнения

Конвейер CrankShaft построен вокруг асинхронной многофазной модели диспетчеризации на GPU. Разделяя куллинг на два вычислительных прохода, разделенных отрисовкой непрозрачной геометрии и пирамидальной редукцией Single Pass Downsampler (SPD), CrankShaft разрешает окклюзию без обратных чтений на CPU и без межкадровых задержек. Однако эта архитектура жестко требует поддержки вычислительных шейдеров OpenGL 4.3+, прямого доступа к состоянию OpenGL 4.5 DSA и глубокой интеграции с абстракциями современного Blaze3D RHI (`RenderPipeline`, `RenderPass`).

Nucleus оптимизирует рендеринг под высокую плотность многоблочных конструкций в рамках контекстов OpenGL 3.2+ / 4.4+. Вместо создания межпроходных зависимостей по даунсемплингу на GPU, Nucleus максимизирует эффективность взаимодействия CPU-GPU: он группирует инстансы по моделям в общий геометрический атлас, выполняет проверку видимости на CPU с временным кэшированием и отправляет непрозрачные и затухающие инстансы за один проход косвенной отрисовки. Механизм переиспользования чистых кадров (`submitClean`) полностью устраняет подготовку кадра для статичных машин, обеспечивая минимальные накладные расходы процессора на индустриальных комплексах.

### 4.3 Домен 2: Multi-Draw Indirect (MDI) и стратегия батчинга

Оба движка используют вызов `glMultiDrawElementsIndirect`, однако форматы команд, потоки данных и принципы привязки текстур кардинально различаются:
- **Размер и упаковка команды**: CrankShaft использует расширенную 48-байтную структуру команды (`MeshDrawCommand`), содержащую ID моделей, указатели матриц, флаги материалов и смещения мешлетов. Nucleus использует компактный 32-байтный страйд (20 байт стандартных параметров, дополненных нулями до 32 байт для аппаратного выравнивания), передавая данные инстансов через инстансированные вершинные атрибуты (локации 4–11).
- **Объединение границ батчей**: CrankShaft выполняет динамическую сортировку на CPU (`UBER_DRAW_COMPARATOR`) для группировки инстансов в непрерывные срезы с общими материалами. Nucleus опирается на монолитный геометрический атлас (`MdiGeometryAtlas`), позволяющий объединять в одном вызове multi-draw совершенно разные модели машин.
- **Связывание с текстурным атласом против сортировки по текстурам**:
  Фундаментальное проектное расхождение между движками заключается в привязке текстур:
  - **Монолитный атлас Nucleus**: Nucleus достигает минимальной нагрузки на CPU за счет привязки единственной диффузной текстуры перед вызовом косвенной отрисовки: `RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);` (`MdiBatchCoordinator.java:1221`). Это позволяет отрисовывать сотни разнородных механизмов **за один вызов draw call**. Однако возникает строгое ограничение: **все детали машин в MDI-батче должны быть упакованы в `TextureAtlas.LOCATION_BLOCKS`**. Любые детали, требующие отдельных текстур (динамические экраны осциллографов, радары, отдельные PNG), не могут участвовать в монолитном MDI-батче и должны рендериться во вторичном или небатчированном проходе.
  - **Динамическое разделение диапазонов CrankShaft**: Компаратор `UBER_DRAW_COMPARATOR` сортирует команды по материалам и текстурам, на лету разделяя буфер команд на непрерывные диапазоны отрисовки. Это позволяет использовать произвольные текстуры, но дробит выполнение на множество вызовов косвенной отрисовки и переключений состояния пайплайна.
- **Подсчет числа вызовов**: CrankShaft полагается на `glMultiDrawElementsIndirectCount` для чанков ландшафта, считывая счетчик из буфера параметров GPU. Nucleus рассчитывает счетчик на CPU, что гарантирует совместимость со старыми драйверами и macOS.

### 4.4 Домен 3: Управление памятью GPU и промежуточная загрузка (Staging)

```
+----------------------------------------------------------------------------------------------------+
|                                СРАВНЕНИЕ АРХИТЕКТУР ПРОМЕЖУТОЧНЫХ БУФЕРОВ                          |
+------------------------------------+---------------------------------------------------------------+
| CRANKSHAFT 26.2                    | NUCLEUS (HBM-Modernized)                                      |
+------------------------------------+---------------------------------------------------------------+
| [Арена 16 МБ (GL45C DSA)]          | [Кольцевой буфер 4 МБ (GL44 glBufferStorage)]                 |
| Память: PERSISTENT | CLIENT        | Память: PERSISTENT | COHERENT                                 |
| Маппинг: PERSISTENT | FLUSH_EXPL   | Маппинг: PERSISTENT | COHERENT                                |
|        │                           |        │                                                      |
|        ▼                           |        ▼                                                      |
| Копирование CPU в область стейджинга| GpuSpanUploader: теневое дифференцирование CPU (vboShadow)   |
|        │                           | - Выявляет измененные срезы; объединяет зазоры < 24 float    |
|        ▼                           | - Загружает 16-байтный срез вращения вместо 120 байт инстанса|
| glFlushMappedBufferRange           |        │                                                      |
|        │                           |        ▼                                                      |
|        ▼                           | Копирование CPU в когерентное кольцо (явный флаш не нужен)   |
| Вычислительный шейдер scatter.glsl |        │                                                      |
| Параллельное копирование из        |        ▼                                                      |
| стейджинг-SSBO в целевые SSBO      | glCopyBufferSubData переносит грязный срез в VBO атласа      |
|        │                           |        │                                                      |
|        ▼                           |        ▼                                                      |
| GL_SHADER_STORAGE_BARRIER_BIT      | glFenceSync выставляет фенс завершения                       |
+------------------------------------+---------------------------------------------------------------+
```

- **Механизм стейджинга**: `StagingBuffer` в CrankShaft сочетает персистентный маппинг с вычислительным шейдером `scatter.glsl`. Это эффективно для массовой потоковой передачи, но требует OpenGL 4.5 DSA и вычислительных шейдеров. `PersistentUploadStaging` в Nucleus использует персистентный когерентный маппинг OpenGL 4.4 со связкой с дифференцированием в `GpuSpanUploader`. На статичных сценах Nucleus передает **ноль байт**, тогда как CrankShaft повторно пересылает неизмененные страницы инстансов.
- **Компоновка инстансов**: CrankShaft размещает инстансы в SSBO и индексирует их через `gl_BaseInstance` или массивы видимости. Nucleus использует стандартные массивы инстансированных атрибутов (локации 4–11, divisor 1), гарантируя совместимость с любыми драйверами и шейдерпаками.

### 4.5 Домен 4: Стратегия отсечения и окклюзионное тестирование

- **Куллинг фрустума**: CrankShaft выполняет куллинг на GPU в `cull.glsl` с помощью 6-FMA SIMD-теста сферы. Nucleus проверяет фрустум на CPU в `CpuFrustumCuller` по плоскостям Грибба/Хартмана.
- **Окклюзионный куллинг**: CrankShaft использует аппаратную пирамиду глубин Hi-Z с Single Pass Downsampler (SPD) и двухфазную диспетчеризацию. Это позволяет отсекать скрытую геометрию любой сложности без нагрузки на CPU. Nucleus использует воксельный марш 15 лучей на CPU (`OcclusionCullingHelper`) с межкадровым кэшированием. Для разреженных сцен это эффективно, однако нагрузка на CPU растет с числом машин, делая пирамиду Hi-Z в CrankShaft предпочтительной для плотных баз.
- **Компактизация и атомики**: CrankShaft задействует `GL_KHR_shader_subgroup_ballot` для компактизации инстансов внутри 32-поточных варпов, выполняя всего 1 атомик на варп. Nucleus в настоящее время не использует субгрупповые операции в вычислительных шейдерах.

### 4.6 Домен 5: Интероперабельность с шейдерными модами (Iris / Oculus / Vanilla)

Различие в совместимости с шейдерными модами исторически являлось наиболее заметным контрастом между движками:

```
+----------------------------------------------------------------------------------------------------+
|                                    СПЕКТР СОВМЕСТИМОСТИ С ШЕЙДЕРПАКАМИ                             |
+------------------------------------+---------------------------------------------------------------+
| CRANKSHAFT 26.2                    | NUCLEUS (HBM-Modernized)                                      |
+------------------------------------+---------------------------------------------------------------+
| Философия: Полный отказ            | Философия: Нативная многоуровневая интеграция                 |
|                                    |                                                               |
| ShadersModHelper.isShaderPackInUse | Уровень 1: GPU Compute Bake (NucleusGpuBaker)                 |
|   -> INSTANCING.supported = false  |         - Запекание трансформаций из SSBO в VBO               |
|   -> INDIRECT.supported = false   |         - Единая отрисовка через нативную программу шейдерпака|
|   -> BackendManager: "flywheel:off"|         - 100% паритет с дисторсией, G-buffer и MRT           |
|                                    |                                                               |
| Визуальный результат под шейдерами:| Уровень 2: Инстансинг ExtendedShader (IrisInstancedShaders)   |
| - Движок полностью отключается     |         - Воспроизведение дисторсии (IrisShadowDistortion)    |
| - Сброс в небатчированный BER      |                                                               |
| - Рост draw calls в 10–50 раз      | Уровень 3: Персистентный батч (IrisRenderBatch)               |
| - Жесткие просадки CPU на базах    |         - Оплата shader.apply() один раз за проход            |
|                                    |                                                               |
|                                    | Уровень 4: Немедленный откат BER                              |
|                                    |         - Плавная деградация на слабом оборудовании           |
+------------------------------------+---------------------------------------------------------------+
```

Архитектура CrankShaft предполагает полный контроль над состоянием OpenGL, конвейерами шейдеров и форматами вершин. Поскольку шейдерпаки внедряют собственные теневые проходы, отложенные G-буферы и модели освещения, переопределяющие конвейер Minecraft, CrankShaft полностью отключался для предотвращения графических артефактов и падений.[^iris-update-5]

Nucleus изначально создавался для сохранения высокой производительности при включенных шейдерпаках. Вычислительный GPU-бейкер (`NucleusGpuBaker`, Уровень 1) трансформирует вершины и нормали в VRAM, после чего отправляет геометрию через **нативную активную программу** шейдерпака. Это сохраняет все возможности шейдеров (кастомные тени, PBR-карты, POM, колыхание листвы), обеспечивая скорость пакетного рендеринга.

### 4.7 Домен 6: Обработка динамической и анимированной геометрии

- **Обработка анимаций**: CrankShaft обновляет дескрипторы инстансов на CPU (`ForEachPlan`) и перезагружает измененные страницы на GPU. Для чанков и сеток используются Task/Mesh шейдеры с отсечением обратных граней.
- **Иерархия составных частей**: Nucleus специально адаптирован под составные модели Wavefront OBJ со сложной кинематикой (руки манипуляторов сборочных автоматов, роторы центрифуг, створки доменных печей). Фасад `LegacyAnimator` транслирует анимационный код NTM 1.7.10 в операции `PoseStack`. Nucleus кэширует дельта-матрицы кадра (`animDelta`), переиспользуя их между основным проходом и проходом теней в пределах тика.
- **Обход диспетчера BER**: Nucleus предоставляет подсистему `NucleusDispatcherBypass`, регистрирующую машины в плоском списке (`LIVE`) и исключающую поблочный обход чанков в Sodium во время стадии `AFTER_ENTITIES`. Это экономит 25.4% процессорного времени на индустриальных базах — функционал, отсутствующий в Flywheel/CrankShaft.

### 4.8 Домен 7: Профиль производительности и накладные расходы драйверов/оборудования

- **Оборудование NVIDIA**: CrankShaft демонстрирует максимальную отдачу на современных GPU NVIDIA (семейство RTX), используя `NV_mesh_shader`, голосование варпов и OpenGL 4.5 DSA. Nucleus работает эффективно на всех поколениях NVIDIA, дополнительно защищаясь от сбоев переиспользования Program ID при перезагрузке шейдеров.
- **Драйверы AMD / Radeon**: CrankShaft содержит обход бага драйверов AMD (`safeShaderSource`) и корректно учитывает размер волнового фронта 64 потока. Nucleus надежно работает на драйверах AMD, используя стандарты OpenGL 3.3/4.4.
- **Интегрированная графика Intel**: CrankShaft понижает приоритет бэкенда Indirect ниже Instancing на драйверах Intel из-за ошибок драйверов с буферами параметров. Инстансированные массивы Nucleus стабильно работают на встроенной графике Intel и дискретных Arc.
- **Apple Silicon / macOS**: CrankShaft не может работать на macOS из-за жесткой привязки к OpenGL 4.5 DSA и 4.6 Indirect (macOS ограничена OpenGL 4.1). Nucleus определяет доступные расширения в рантайме и плавно деградирует до OpenGL 3.3 Instancing и ванильного Blaze3D, обеспечивая 100% работоспособность на компьютерах Apple.

---

## 5. Конкретные архитектурные рекомендации для Nucleus

### 5.1 Стратегический вердикт: Обоснование отказа от полного переноса движка

Ключевой вопрос данного исследования: целесообразно ли для HBM-Modernized отказаться от Nucleus или заменить его путем полного переноса (whole-engine port) кодовой базы CrankShaft 26.2?

**Архитектурный вердикт: однозначный и категорический ОТКАЗ от полного переноса движка.**

#### Детальное обоснование
1. **Разрушение поддержки модов на шейдеры (Iris / Oculus)**:
   Принятие архитектуры движка CrankShaft разрушило бы специализированный конвейер шейдеров HBM-Modernized. При включенных шейдерпаках CrankShaft отключает весь батчинг и откатывается к непакетированным непосредственным вызовам BER. В условиях промышленного комплекса с 60 машинами (более 660 деталей) частота кадров упала бы катастрофически. Архитектура вычислений на GPU (`NucleusGpuBaker`) и персистентный координатор батчинга (`IrisRenderBatch`) в Nucleus абсолютно необходимы пользователям шейдерпаков.[^iris-update-6]
2. **Версионная и архитектурная несовместимость (Minecraft 26.2 против 1.20.1 и 1.21.1)**:
   CrankShaft написан под Minecraft 26.2 и напрямую зависит от разработанных Mojang Vulkan-подобных абстракций графического конвейера (`com.mojang.blaze3d.vulkan.VulkanCommandEncoder`, `RenderPipeline`, `RenderPass`, `GpuBuffer`). Данных классов не существует в Minecraft 1.20.1 или 1.21.1, где рендеринг жестко опирается на конечные автоматы OpenGL 3.2 Core profile. Бэкпортирование CrankShaft потребовало бы полной переписки всего слоя отправки команд.
3. **Утрата спецификаций составных машин и механизма обхода диспетчера**:
   Компоненты Nucleus (`MachineSpec`, `PartGeometry`, `LegacyAnimator` и `NucleusDispatcherBypass`) созданы специально под индустриальные многоблоки HBM-Modernized. В CrankShaft полностью отсутствуют концепции деревьев составных OBJ-моделей, 8-точечной трилинейной выборки освещения и пропуска обхода BER в Sodium.
4. **Хрупкость платформенного уровня и аппаратная привязка**:
   CrankShaft жестко привязан к Direct State Access OpenGL 4.5 (`GL45C`) в своих промежуточных буферах и опирается на эксклюзивные расширения NVIDIA (`NV_mesh_shader`). Его полное внедрение лишило бы мод поддержки macOS, встроенных видеокарт Intel и более старого оборудования.

**Рекомендация:** Сохранить Nucleus в качестве основной утвержденной архитектуры рендеринга, выборочно перенеся точечные алгоритмические решения из CrankShaft.

---

### 5.2 Практическое проектное предложение 1: Окклюзионный куллинг по пирамиде глубин GPU Hi-Z (адаптированный под Forward-Z)

#### Постановка задачи
В настоящее время Nucleus выполняет окклюзионный куллинг на CPU внутри `OcclusionCullingHelper.java`, используя марш 15 лучей по вокселям с проверкой `BlockState.isSolidRender()`. Хотя это эффективно при малом числе машин, трассировка сотен лучей за кадр создает избыточную нагрузку на CPU в плотных промышленных комплексах и требует сложных межкадровых эвристик кэширования (`CAMERA_REUSE_MAX_DIST_SQ`).

Более того, хотя CrankShaft 26.2 демонстрирует окклюзионный куллинг GPU Hi-Z, его реализация написана под современный **Reversed-Z** ($1.0$ — ближняя плоскость, $0.0$ — дальняя, функция `GL_GEQUAL`). Прямой перенос его шейдеров в Minecraft 1.20.1 и 1.21.1, функционирующие по стандартному **Forward-Z** ($0.0$ — ближняя, $1.0$ — дальняя, функция `GL_LEQUAL`), полностью инвертирует окклюзионный тест и сделает машины невидимыми.

#### Математический контраст: Hi-Z куллинг Forward-Z против Reversed-Z

| Атрибут / Операция | CrankShaft 26.2 (Конвейер Reversed-Z) | Nucleus 1.20.1 / 1.21.1 (Конвейер Forward-Z) |
|---|---|---|
| **Диапазон глубин** | $Z_{\text{near}} = 1.0$, $Z_{\text{far}} = 0.0$ | $Z_{\text{near}} = 0.0$, $Z_{\text{far}} = 1.0$ |
| **Функция сравнения глубин** | `GL_GEQUAL` или `GL_GREATER` | `GL_LEQUAL` (`RenderHooks.depthFunc(GL_LEQUAL)`) |
| **Числовое направление** | Большее $Z$ — *ближе*; меньшее $Z$ — *дальше* | Меньшее $Z$ — *ближе*; большее $Z$ — *дальше* |
| **Консервативный даунсемплинг окклюдеров** | $\min(d_{00}, d_{01}, d_{10}, d_{11})$ (ближайшая поверхность) | $\mathbf{\max(d_{00}, d_{01}, d_{10}, d_{11})}$ (самая далекая поверхность в проекции) |
| **Ближняя глубина ограничивающей сферы** | $Z_{\text{sphere}} = -Z_{\text{near}} / (Z_{\text{center}} + R)$ | $Z_{\text{sphere\_near}} = \text{clamp}((P_{22} \cdot Z_{\text{view\_near}} + P_{32}) / (-Z_{\text{view\_near}}) \cdot 0.5 + 0.5, 0.0, 1.0)$ |
| **Условие окклюзии** | $Z_{\text{sphere}} < \text{occluderDepth}_{\min}$ | $\mathbf{Z_{\text{sphere\_near}} > \text{occluderDepth}_{\max}}$ |
| **Условие видимости** | $Z_{\text{sphere}} \ge \text{occluderDepth}_{\min}$ | $\mathbf{Z_{\text{sphere\_near}} \le \text{occluderDepth}_{\max}}$ |

*Обоснование консервативного даунсемплинга:* Для безопасного отсечения объекта без визуальных мерцаний каждый пиксель в его описывающем прямоугольнике в пространстве экрана должен быть перекрыт окклюдером. В условиях Forward-Z окклюдеры имеют большие значения $Z$ по мере удаления. Если ограничивающий параллелепипед покрывает четыре текселя глубины, объект считается окклюдированным только в том случае, если его ближайшая точка глубже (больше по значению), чем *самый далекий* окклюдер среди этих четырех текселей. Выборка максимальной глубины `max()` по каждому блоку текселей $2 \times 2$ при построении пирамиды гарантирует консервативность отсечения.

#### Предлагаемая архитектура: `NucleusDepthPyramid`

```
[ Активная текстура глубины мира ]
(RenderHooks.getActiveLevelDepthTextureId() — перехватывает FBO G-буфера Iris)
                       │
                       ▼
         [ NucleusDepthPyramid.downsample() ]
         - Mip 0: downsample_first.comp (копирование глубины, редукция max())
         - Mips 1-6: downsample_second.comp (Single Pass Downsampler в LDS с max())
                       │
                       ▼
         [ Вычислительный куллинг: nucleus_cull.comp ]
         - Вход: SSBO 0 (Ограничивающие сферы машин: center.xyz, radius)
         - Вход: Sampler2D (Цепочка MIP пирамиды глубин Hi-Z)
         - 6-FMA SIMD-тест фрустума
         - Проекция описывающего прямоугольника в экранное пространство и выбор MIP-уровня
         - 4-тексельный тест окклюзии Forward-Z: isOccluded = (depthSphere_near > occluderDepth_max)
         - Выход: SSBO 1 (Битовая маска видимости инстансов / Компактизованный индекс отрисовки)
                       │
                       ▼
         [ MdiBatchCoordinator / NucleusGpuBaker ]
         - Отправка команд отрисовки только для видимых инстансов
```

#### Спецификация реализации шейдеров Forward-Z на GLSL

1. **Шейдер даунсемплинга глубины (`downsample_first.comp` / `downsample_second.comp`)**:
   ```glsl
   #version 430 core
   layout(local_size_x = 16, local_size_y = 16) in;

   uniform sampler2D uSourceDepth;
   layout(r32f, binding = 0) uniform writeonly image2D uTargetMip;
   uniform int uSourceLevel;

   void main() {
       ivec2 targetCoords = ivec2(gl_GlobalInvocationID.xy);
       ivec2 baseCoords = targetCoords * 2;

       // Выборка 4 исходных текселей, покрывающих область 2x2
       float d00 = texelFetch(uSourceDepth, baseCoords + ivec2(0, 0), uSourceLevel).r;
       float d01 = texelFetch(uSourceDepth, baseCoords + ivec2(0, 1), uSourceLevel).r;
       float d10 = texelFetch(uSourceDepth, baseCoords + ivec2(1, 0), uSourceLevel).r;
       float d11 = texelFetch(uSourceDepth, baseCoords + ivec2(1, 1), uSourceLevel).r;

       // КРИТИЧЕСКИ ВАЖНО ДЛЯ FORWARD-Z: глубина MAX сохраняет самый далекий окклюдер
       float maxDepth = max(max(d00, d01), max(d10, d11));

       imageStore(uTargetMip, targetCoords, vec4(maxDepth, 0.0, 0.0, 0.0));
   }
   ```

2. **Оценка окклюзии в шейдере куллинга (`nucleus_cull.comp`)**:
   ```glsl
   // 1. Расчет ближайшей точки ограничивающей сферы к ближней плоскости в пространстве вида
   // В пространстве вида Minecraft: камера направлена вдоль -Z, поэтому Z отрицателен.
   float centerDist = -centerView.z;
   float nearDist = max(centerDist - radius, uNearPlane);
   float viewZ_near = -nearDist;

   // 2. Проецирование ближайшей точки в оконную глубину Forward-Z [0.0, 1.0]
   float clipZ = uProj[2][2] * viewZ_near + uProj[3][2];
   float clipW = -viewZ_near;
   float ndcZ = clipZ / clipW;
   float depthSphere_near = clamp(ndcZ * 0.5 + 0.5, 0.0, 1.0);

   // 3. Выборка 4 текселей глубины, покрывающих описывающий прямоугольник на вычисленном уровне MIP
   float occluder00 = texelFetch(uHiZPyramid, rect.xy, mipLevel).r;
   float occluder01 = texelFetch(uHiZPyramid, ivec2(rect.x, rect.w), mipLevel).r;
   float occluder10 = texelFetch(uHiZPyramid, ivec2(rect.z, rect.y), mipLevel).r;
   float occluder11 = texelFetch(uHiZPyramid, rect.zw, mipLevel).r;

   // В условиях Forward-Z: находим самый далекий окклюдер в проекции
   float occluderDepth_max = max(max(occluder00, occluder01), max(occluder10, occluder11));

   // 4. Тест окклюзии Forward-Z: Объект отсекается ТОГДА И ТОЛЬКО ТОГДА, когда его ближайшая точка строго глубже окклюдера
   bool isOccluded = (depthSphere_near > occluderDepth_max);
   bool isVisible = !isOccluded;
   ```

#### Спецификации реализации и интероперабельность с модами на шейдеры

1. **Получение текстуры глубины через `RenderHooks.getActiveLevelDepthTextureId()`**:
   В ванильном Minecraft глубина уровня находится в `getMainRenderTarget().getDepthTextureId()`. Однако при активных шейдерпаках Iris или Oculus растеризация ландшафта перенаправляется во внутренние кадровые буферы отложенных G-буферов Iris (`iris:gbuffers` или `gcolor`/`gdepth`). Ванильные буферы глубины остаются незаполненными и содержат недостоверные значения ($1.0$).
   
   Для обеспечения совместимости с шейдерпаками получение текстуры глубины должно осуществляться через платформенный хук `RenderHooks`:
   ```java
   public static int getActiveLevelDepthTextureId() {
       // 1. Проверяем, активен ли шейдерпак Iris / Oculus
       if (IrisApiHelper.isShaderPackInUse()) {
           int irisDepthTex = IrisApiHelper.getActiveGbufferDepthTexture();
           if (irisDepthTex > 0) {
               return irisDepthTex;
           }
       }
       // 2. Откатываемся к стандартной глубине ванильного RenderTarget
       RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
       return mainTarget != null ? mainTarget.getDepthTextureId() : -1;
   }
   ```

2. **Автоматический пропуск Hi-Z в теневых проходах**:
   Во время теневых проходов Iris (`IrisShadowBatchCollector`) геометрия рендерится из пространства источника света с использованием ортографических проекций с нелинейным искривлением координат (например, квартичная дисторсия Photon). Пирамида глубин Hi-Z из пространства камеры геометрически неприменима в пространстве теней.
   Класс `NucleusDepthPyramid` обязан проверять флаг `IrisShadowBatchCollector.isShadowPassActive()` и **безусловно отключать окклюзионный куллинг GPU Hi-Z во время теневых проходов**, полностью полагаясь на отсечение по фрустуму на CPU.

3. **Корректная деградация при отсутствии глубины или неподдерживаемых compute-шейдерах**:
   Если `getActiveLevelDepthTextureId() <= 0`, если к текстурам глубины G-буфера Iris невозможно получить доступ через рефлексию, либо если вычислительные шейдеры OpenGL 4.3 не поддерживаются (например, на macOS или старых драйверах), `NucleusDepthPyramid` автоматически отключается на текущий кадр. Nucleus в этом случае прозрачно переключается на существующий марш 15 лучей в `OcclusionCullingHelper` на CPU, гарантируя полное отсутствие визуальных артефактов.

---

### 5.3 Практическое проектное предложение 2: Адаптивное пороговое переключение для промежуточной загрузки Compute Scatter

#### Постановка задачи и аппаратный анализ
Класс `PersistentUploadStaging.java` в настоящее время использует вызов `MemoryUtil.memCopy` на CPU для записи обновлений инстансов в отображаемую память, после чего вызывает `GL31.glCopyBufferSubData` для переноса модифицированных диапазонов (срезов) в целевые буферы VBO.

Хотя CrankShaft использует вычислительный шейдер разброса (`scatter.glsl`) для всех загрузок буферов, его архитектура передает тысячи новых визуальных инстансов по целым чанкам. В Nucleus же класс `GpuSpanUploader` применяет дифференциацию срезов памяти уровня 2 через теневой буфер CPU (`vboShadow`):
- **Статичные сцены**: 0 измененных диапазонов, **0 переданных байт**.
- **Типичные промышленные сцены**: Обновляются только активные подвижные узлы механизмов (например, вращающиеся турбины, вибрирующие центрифуги), передавая всего **16 байт кватерниона вращения** на машину. База с 10 работающими машинами передает всего 160 байт в 10 диапазонах.

Запуск вычислительного шейдера (`scatter.comp`) сопряжен с фиксированными накладными расходами: привязка программы шейдера, привязка 3 буферов SSBO (`CopyOps`, `SrcData`, `DstData`), передача uniform-значений, запуск рабочих групп и выставление барьера исполнения (`glMemoryBarrier(GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT | GL_BUFFER_UPDATE_BARRIER_BIT)`). Более того:
1. Современные драйверы GPU (NVIDIA, AMD, Intel) исполняют вызовы `glCopyBufferSubData` через выделенные асинхронные аппаратные **движки DMA-копирования**, функционирующие параллельно с 3D-очередью графики без вытеснения шейдерных исполнительных блоков.
2. При небольшом количестве диапазонов ($< 32$ срезов) запуск compute-шейдера создает **большую латентность драйвера и накладные расходы вызова GPU**, чем прямое копирование через DMA.
3. На встроенной графике Intel (архитектура UMA) частые запуски мелких вычислительных шейдеров приводят к остановкам синхронизации кольцевой шины исполнения.

#### Предлагаемая архитектура: Адаптивная двухуровневая схема (`NucleusComputeScatter`)
Внедрение модели адаптивного порогового переключения в `PersistentUploadStaging.java`, которая динамически выбирает между аппаратным DMA-копированием буферов и вычислительным разбросом на GPU:

```
[ GpuSpanUploader: Обнаружение грязных диапазонов и дифференциация vboShadow ]
                       │
                       ▼
        [ Оценщик условий адаптивной диспетчеризации ]
        Условие: (spanCount > 32) И (totalBytes >= 4096) И (!isIntelUma)?
              │                               │
             ДА                               НЕТ
              │                               │
              ▼                               ▼
 [ Путь Compute Scatter при высокой нагрузке ] [ Быстрый путь аппаратного DMA ]
 - Накопление операций в SSBO CopyOps          - Цикл по диапазонам
 - Единый запуск scatter.comp                  - Прямые вызовы glCopyBufferSubData
 - glMemoryBarrier(VERTEX_ATTRIB)              - Асинхронная передача через DMA
              │                               │
              └───────────────┬───────────────┘
                              ▼
                [ Завершение по glFenceSync ]
```

#### Логика адаптивной диспетчеризации в `PersistentUploadStaging.java`
```java
public final class PersistentUploadStaging {
    private static final int SCATTER_THRESHOLD_SPANS = 32;
    private static final int SCATTER_THRESHOLD_BYTES = 4096; // 4 КиБ
    private static final boolean IS_INTEL_GPU = GlCompat.isIntel();

    public void flushSpans(List<GpuSpanUploader.Span> dirtySpans, int totalBytes, int srcVbo, int dstVbo) {
        int spanCount = dirtySpans.size();
        if (spanCount == 0) {
            return; // Чистый кадр: нулевые накладные расходы на загрузку
        }

        // Включаем compute scatter ТОЛЬКО при масштабных обновлениях на оборудовании, отличном от Intel
        if (spanCount >= SCATTER_THRESHOLD_SPANS 
                && totalBytes >= SCATTER_THRESHOLD_BYTES 
                && !IS_INTEL_GPU 
                && RenderHooks.supportsComputeShaders()) {
            dispatchComputeScatter(dirtySpans, srcVbo, dstVbo);
        } else {
            // Основной быстрый путь: аппаратный DMA-движок обрабатывает мелкие и средние передачи
            dispatchHardwareDmaCopies(dirtySpans, srcVbo, dstVbo);
        }
    }

    private void dispatchHardwareDmaCopies(List<GpuSpanUploader.Span> dirtySpans, int srcVbo, int dstVbo) {
        for (int i = 0; i < dirtySpans.size(); i++) {
            GpuSpanUploader.Span span = dirtySpans.get(i);
            GL31.glCopyBufferSubData(GL31.GL_COPY_READ_BUFFER, GL31.GL_COPY_WRITE_BUFFER,
                    span.srcOffsetBytes(), span.dstOffsetBytes(), span.lengthBytes());
        }
    }
}
```

#### Шейдер Compute Scatter при высокой нагрузке (`scatter.comp`)
При активации во время масштабных обновлений мега-баз ($> 32\text{--}48$ срезов), `scatter.comp` выполняет параллельные передачи, выровненные по 4-байтным словам:
```glsl
#version 430 core
layout(local_size_x = 64) in;

struct CopyOp {
    uint srcOffsetWords;
    uint dstOffsetWords;
    uint wordCount;
    uint pad;
};

layout(std430, binding = 0) readonly buffer CopyOpsBuffer { CopyOp ops[]; };
layout(std430, binding = 1) readonly buffer SourceBuffer { uint srcData[]; };
layout(std430, binding = 2) writeonly buffer DestinationBuffer { uint dstData[]; };

uniform uint uTotalCopyWords;

void main() {
    uint gid = gl_GlobalInvocationID.x;
    if (gid >= uTotalCopyWords) return;

    // Двоичный поиск или выборка по префиксной сумме отображает глобальный поток на операцию копирования
    uint opIndex = findCopyOp(gid);
    CopyOp op = ops[opIndex];
    uint localOffset = gid - op.srcOffsetWords;
    dstData[op.dstOffsetWords + localOffset] = srcData[op.srcOffsetWords + localOffset];
}
```

#### Стратегическая выгода
- **Типичный геймплей (<32 диапазонов, <4 КиБ)**: Сохраняются прямые DMA-передачи с нулевыми накладными расходами, исключая простои конвейера compute-шейдеров.
- **Масштабные пиковые нагрузки (>48 диапазонов, перестроения мегабайтов данных)**: Сотни вызовов драйвера `glCopyBufferSubData` объединяются в единый запуск вычислительного шейдера на GPU.
- **Безопасность интегрированных GPU Intel**: Защищает архитектуры с объединенной памятью от зависаний кольцевой шины, жестко закрепляя устройства Intel за DMA-копированием.

---

### 5.4 Практическое проектное предложение 3: Инстансинг с субгрупповой компактизацией

#### Постановка задачи
В `NucleusGpuBaker.java` трансформированные вершины инстансов записываются в выходные буферы SSBO. В проходах куллинга на compute-шейдерах запись индексов видимых инстансов в буферы отрисовки через наивные вызовы `atomicAdd` приводит к тяжелой сериализации атомарных операций в контроллерах памяти GPU, когда тысячи потоков одновременно конкурируют за один счетчик.

#### Предлагаемая архитектура: Субгрупповая поварповая компактизация
Интеграция поварповой компактизации через ballot в вычислительные шейдеры Nucleus (`nucleus_cull.comp` и `NucleusGpuBaker.java`) с сохранением структуры расширений, примененной в `IndirectPrograms.java:129–130` CrankShaft:

```glsl
#version 430 core

// Расширения субгрупп: Basic предоставляет subgroupElect() и subgroupBroadcastFirst();
// Ballot предоставляет subgroupBallot(), подсчет бит и исключающий подсчет бит.
#extension GL_KHR_shader_subgroup_basic : enable
#extension GL_KHR_shader_subgroup_ballot : enable
#extension GL_KHR_shader_subgroup_vote : enable

layout(local_size_x = 64) in;

layout(std430, binding = 0) readonly buffer InstanceData {
    // Данные границ и трансформаций инстансов
    ...
};

layout(std430, binding = 1) writeonly buffer VisibleIndices {
    uint outputIndices[];
};

layout(std430, binding = 2) buffer DrawCommand {
    uint count;
    uint instanceCount;
    uint firstIndex;
    uint baseVertex;
    uint baseInstance;
};

void main() {
    uint instanceId = gl_GlobalInvocationID.x;
    bool isVisible = evaluateVisibility(instanceId);

#if defined(GL_KHR_shader_subgroup_basic) && defined(GL_KHR_shader_subgroup_ballot)
    // 1. Поварповый ballot формирует битовую маску всех видимых инстансов в данной SIMD-волне
    uvec4 ballot = subgroupBallot(isVisible);
    uint warpCount = subgroupBallotBitCount(ballot);
    uint warpBase = 0u;

    // 2. Выбранный лидер варпа выполняет ровно ОДНО атомарное сложение на всю волну (32 или 64 потока)
    if (warpCount > 0u && subgroupElect()) {
        warpBase = atomicAdd(instanceCount, warpCount);
    }

    // 3. Рассылка выделенного базового смещения всем линиям в варпе
    warpBase = subgroupBroadcastFirst(warpBase);

    // 4. Каждый видимый поток записывает свой индекс в непрерывную позицию без коллизий
    if (isVisible) {
        uint index = warpBase + subgroupBallotExclusiveBitCount(ballot);
        outputIndices[index] = instanceId;
    }
#else
    // Запасной путь для оборудования или драйверов без поддержки GL_KHR_shader_subgroup_basic/ballot:
    if (isVisible) {
        uint index = atomicAdd(instanceCount, 1u);
        outputIndices[index] = instanceId;
    }
#endif
}
```

#### Валидация расширений и гарантии безопасности
1. **Проверки возможностей на стороне Java**:
   В `IndirectPrograms.java` / `RenderHooks.java` проверять наличие обоих расширений перед включением ветвей с субгруппами:
   ```java
   boolean hasSubgroups = GL.getCapabilities().GL_KHR_shader_subgroup_basic
                       && GL.getCapabilities().GL_KHR_shader_subgroup_ballot;
   ```
2. **Совместимость с компиляторами**:
   Явное включение `#extension GL_KHR_shader_subgroup_basic : enable` гарантирует корректную сборку на строгих препроцессорах GLSL драйверов AMD Adrenalin и Mesa без ошибок вида `'subgroupElect' : no matching overloaded function found`.
3. **Снижение конкуренции**:
   Сокращает число атомарных транзакций по шине памяти до **32 раз на NVIDIA** (32-поточные варпы) и до **64 раз на AMD** (64-поточные волновые фронты wavefront), предотвращая насыщение контроллеров памяти при масштабном куллинге многоблочных конструкций.

---

### 5.5 Практическое проектное предложение 4: Безопасность компиляции шейдеров под драйверы AMD (`safeShaderSource`)

#### Постановка задачи
В некоторых драйверах AMD для Windows (`atio6axx.dll` / `atig6pxx.dll`) присутствует давний дефект уровня драйвера при компиляции шейдеров: когда приложение вызывает `glShaderSource` с явным указателем на массив длин строк, препроцессор шейдеров драйвера некорректно интерпретирует параметр длины либо производит чтение за пределами буфера, вызывая спорадические сбои нарушения прав доступа к памяти (`EXCEPTION_ACCESS_VIOLATION`).

Стандартные биндинги LWJGL 3 (`GL20.glShaderSource(int, CharSequence)`) усугубляют эту уязвимость, поскольку они выделяют на стеке целое число, содержащее `source.remaining()`, и передают его адрес в качестве нативного указателя `length`:
```java
// Стандартная реализация LWJGL 3 внутренне передает явный указатель на длину:
nglShaderSource(shader, 1, pointers.address0(), stack.ints(source.remaining()).address());
```
Таким образом, вызов стандартного метода `GL20.glShaderSource` не защищает от сбоев на подверженных данной проблеме конфигурациях AMD Radeon.

#### Предлагаемая архитектура: `GlCompatHelper.safeShaderSource`
Интеграция точной реализации `safeShaderSource` из CrankShaft (`dev.engine_room.flywheel.backend.gl.GlCompat:94–102`) в конвейер компиляции шейдеров Nucleus (`com.hbm_m.client.render.shader.ModShaders` и `com.hbm_m.platform.RenderHooks`):

```java
package com.hbm_m.client.render.shader;

import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public final class GlCompatHelper {
    private GlCompatHelper() {}

    /**
     * Безопасная компиляция исходного кода шейдера во всех драйверах OpenGL для ПК.
     *
     * <p>Функционально идентичен {@link GL20C#glShaderSource(int, CharSequence)}, но передает
     * нулевой нативный указатель (0L) для длины строки, принуждая драйвер полагаться исключительно
     * на null-терминатор строки. Это обходит критический дефект в драйверах AMD Windows,
     * некорректно считывающих указатели длины и вызывающих нарушение доступа к памяти.
     *
     * <p>Авторство: fewizz и CrankShaft GlCompat.java:94-102.
     *
     * @param shaderId Идентификатор объекта шейдера OpenGL.
     * @param source   Исходный код шейдера на GLSL.
     */
    public static void safeShaderSource(int shaderId, CharSequence source) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Выделение null-терминированного байтового буфера UTF-8
            ByteBuffer sourceBuffer = MemoryUtil.memUTF8(source, true);
            PointerBuffer pointers = stack.mallocPointer(1);
            pointers.put(sourceBuffer);
            
            // 0L принудительно задает нативный указатель длины в NULL (Спецификация OpenGL:
            // "If length is NULL, each string is assumed to be null terminated.")
            GL20C.nglShaderSource(shaderId, 1, pointers.address0(), 0L);
            
            MemoryUtil.memFree(sourceBuffer);
        }
    }
}
```

#### Точки интеграции и гарантии стабильности
- **Точки интеграции**: Перевести все процедуры компиляции GLSL в `ModShaders.java`, `IrisInstancedShaders.java` и `NucleusGpuBaker.java` на вызов `GlCompatHelper.safeShaderSource`.
- **Влияние на производительность**: Нулевые накладные расходы во время кадра (метод вызывается исключительно при инициализации шейдеров и перезагрузке конвейеров).
- **Стабильность драйверов**: Полностью ликвидирует аварийные падения при компиляции шейдеров как на устаревших, так и на современных системах с видеокартами AMD Radeon под Windows.

---

### 5.6 Сопоставление с платформенным слоем (`com.hbm_m.platform` для 1.20.1 и 1.21.1)

Все новые возможности рендеринга должны направляться через платформенный уровень `com.hbm_m.platform.RenderHooks` без внедрения разрозненных блоков условий `//? if` в основном коде:

```
+----------------------------------------------------------------------------------------------------+
|                                АРХИТЕКТУРА ПЛАТФОРМЕННЫХ ХУКОВ РЕНДЕРИНГА                          |
+---------------------------------------+----------------------------------+-------------------------+
| Возможность / Операция                | Реализация 1.20.1-forge          | Реализация 1.21.1-neoforge|
+---------------------------------------+----------------------------------+-------------------------+
| RenderHooks                           | При Iris: запрос FBO глубины     | При Iris: запрос FBO    |
|   .getActiveLevelDepthTextureId()     |   через рефлексию;               |   глубины Iris;         |
|                                       | Иначе: Minecraft.getInstance()   | Иначе: Minecraft...     |
|                                       |   .getMainRenderTarget()         |   .getMainRenderTarget()|
|                                       |   .getDepthTextureId()           |   .getDepthTextureId()  |
+---------------------------------------+----------------------------------+-------------------------+
| RenderHooks.getPartialTick()          | Minecraft.getInstance()          | Minecraft.getInstance() |
|                                       |   .getFrameTime()                |   .getTimer()           |
|                                       |                                  |   .getGameTimeDelta...  |
+---------------------------------------+----------------------------------+-------------------------+
| RenderHooks.pushLevelModelView()      | PoseStack.pushPose()             | PoseStack.pushPose() *  |
+---------------------------------------+----------------------------------+-------------------------+
| RenderHooks.popLevelModelView()       | PoseStack.popPose()              | PoseStack.popPose() *   |
+---------------------------------------+----------------------------------+-------------------------+
| RenderHooks.drawWithShader()          | BufferUploader.drawWithShader(   | BufferUploader          |
|                                       |   builder.end())                 |   .drawWithShader(mesh) |
+---------------------------------------+----------------------------------+-------------------------+
```

*\* Примечание:* В Minecraft 1.21.1 методы рендеринга мира и тайловых сущностей продолжают использовать `com.mojang.blaze3d.vertex.PoseStack`. Низкоуровневый стек `RenderSystem.getModelViewStack()` возвращает `Matrix4fStack` (инкапсулирован внутри `RenderHooks.java`), а полный переход аргументов API на `Matrix4fStack` происходит только в Minecraft 1.21.2+.

---

### 5.7 Приоритизированная дорожная карта реализации и этапы

Предлагаемая дорожная карта модернизации разбита на четыре последовательных этапа, приоритизированных по соотношению технического эффекта и уровня риска:

```
[Этап 1: Стабильность драйверов и безопасность шейдеров] ──► Немедленно (Нулевой риск)
       │
       ▼
[Этап 2: Субгрупповая компактизация в NucleusGpuBaker]  ──► Низкий риск / Высокое ускорение Compute
       │
       ▼
[Этап 3: GPU Hi-Z пирамида глубин (Forward-Z)]         ──► Средний риск / Мощная разгрузка CPU
       │
       ▼
[Этап 4: Промежуточная загрузка Compute Scatter]        ──► Оптимизация под масштабные мега-базы
```

#### Этап 1: Стабильность драйверов и безопасность шейдеров
- **Цель**: Ликвидация сбоев компиляции на видеокартах AMD под Windows.
- **Задачи**:
  1. Добавить `GlCompatHelper.safeShaderSource()` в `com.hbm_m.client.render.shader` и метод `RenderHooks.safeShaderSource()`.
  2. Реализовать вызов через `GL20C.nglShaderSource(shaderId, 1, pointers.address0(), 0L)` с передачей null-указателя длины строки.
  3. Обновить `ModShaders.java`, `IrisInstancedShaders.java` и `NucleusGpuBaker.java`, переведя все процедуры компиляции шейдеров на вызов `safeShaderSource`.
- **Критерии верификации**: Чистая компиляция на таргетах `:1.20.1-forge` и `:1.21.1-neoforge`. Подтверждение устранения сбоев на оборудовании AMD Radeon.

#### Этап 2: Субгрупповая компактизация в NucleusGpuBaker
- **Цель**: Ускорение работы вычислительного GPU-бейкера Уровня 1 под активными шейдерпаками.
- **Задачи**:
  1. Добавить проверки возможностей `GL_KHR_shader_subgroup_basic` и `GL_KHR_shader_subgroup_ballot` в `IndirectPrograms.java` / `RenderHooks.java`.
  2. Внедрить директивы `#extension GL_KHR_shader_subgroup_basic : enable` и `#extension GL_KHR_shader_subgroup_ballot : enable` в шейдеры вычислительного куллинга и бейкинга.
  3. Реализовать логику лидера варпа через `subgroupElect()` и `subgroupBroadcastFirst()` для генерации ровно одного атомарного запроса на 32/64 потока.
  4. Провести бенчмаркинг времени выполнения compute-диспатча при 50+ активных многоблочных механизмах под Iris/Oculus.
- **Критерии верификации**: Замеры времени кадра под шейдерпаками Complementary и Photon.

#### Этап 3: Окклюзионный куллинг по пирамиде глубин GPU Hi-Z (Forward-Z)
- **Цель**: Замена марша 15 лучей на CPU аппаратным окклюзионным куллингом по буферу глубины на GPU.
- **Задачи**:
  1. Реализовать класс `NucleusDepthPyramid.java` и вычислительные шейдеры (`downsample_first.comp`, `downsample_second.comp`) с **редукцией глубины по `max()`** для Forward-Z (`GL_LEQUAL`).
  2. Направить получение текстуры глубины через `RenderHooks.getActiveLevelDepthTextureId()` с перехватом FBO G-буфера Iris.
  3. Внедрить экран защиты теневых проходов Iris: автоматический обход Hi-Z в теневых проходах (`IrisShadowBatchCollector.isShadowPassActive()`).
  4. Разработать вычислительный шейдер `nucleus_cull.comp` с проверкой теста окклюзии Forward-Z: `depthSphere_near > occluderDepth_max`.
  5. Подключить выходную битовую маску видимости к `MdiBatchCoordinator` для пропуска отсеченных вызовов.
  6. Сохранить `OcclusionCullingHelper` в качестве автоматического запасного пути при недоступности FBO глубины или отсутствии поддержки compute-шейдеров.
- **Критерии верификации**: Профилирование сцены с более чем 200 машинами за сплошными взрывозащитными стенами; проверка снижения процессорного времени кадра без визуальных артефактов и инверсии видимости.

#### Этап 4: Промежуточная загрузка Compute Scatter (адаптивная схема)
- **Цель**: Оптимизация передачи буферов при пиковых всплесках обновления анимаций на масштабных базах.
- **Задачи**:
  1. Разработать вычислительный шейдер `scatter.comp` для параллельного копирования буферов с выравниванием по словам.
  2. Обновить `PersistentUploadStaging.java` с внедрением адаптивного порога: активация `scatter.comp` только при количестве грязных диапазонов $> 32\text{--}48$ и размере данных $\ge 4$ КиБ.
  3. Сохранить аппаратную передачу через DMA `glCopyBufferSubData` для небольших и средних обновлений ($\le 32$ срезов), а также для встроенной графики Intel (`GlCompat.isIntel()`).
- **Критерии верификации**: Замеры накладных расходов CPU на уровне драйвера и времени передачи по шине данных при частых анимациях составных многоблоков.

## 6. Приложения

### Приложение A: Исчерпывающий каталог цитирования исходного кода

#### Репозиторий CrankShaft 26.2 (`C:\Projects\CrankShaft`)

| Путь к файлу (относительно `C:\Projects\CrankShaft`) | Диапазон строк | Архитектурная область и назначение |
|---|---|---|
| `gradle.properties` | 5 | Декларирует `minecraft_version=26.2` и зависимости NeoForge |
| `common/src/backend/java/dev/engine_room/flywheel/backend/Backends.java` | 34–36, 52–54 | Гейты поддержки бэкендов; отключает `INSTANCING` и `INDIRECT` при активных шейдерпаках[^iris-update-7] |
| `common/src/backend/java/dev/engine_room/flywheel/backend/engine/EngineImpl.java` | 42–85 | Рецентрирование начала координат рендеринга (`renderOrigin`), исполнение плана кадра |
| `common/src/backend/java/dev/engine_room/flywheel/backend/engine/MeshPool.java` | 22, 184–228 | Гранулярность мешлетов (`MESHLET_TRIS = 64`), генерация ограничивающих сфер (`writeMeshletBounds`) |
| `common/src/backend/java/dev/engine_room/flywheel/backend/engine/indirect/IndirectDrawManager.java` | 239–281, 714–720 | Двухфазный конвейер отсечения, барьеры памяти, непрямая отрисовка `UberDraw.submitRaw` |
| `common/src/backend/java/dev/engine_room/flywheel/backend/engine/indirect/StagingBuffer.java` | 20–22, 73–76 | 16 МБ кольцевой стейджинг-буфер, OpenGL 4.5 DSA (`glCreateBuffers`, `glNamedBufferStorage`) |
| `common/src/backend/java/dev/engine_room/flywheel/backend/engine/indirect/IndirectBuffers.java` | 150–154 | Блок дескрипторов мульти-привязки SSBO в OpenGL 4.4 (`nglBindBuffersRange`) |
| `common/src/backend/java/dev/engine_room/flywheel/backend/engine/indirect/DepthPyramid.java` | 54–100 | Выделение и выполнение пирамиды глубин через однопроходный даунсэмплер (SPD) |
| `common/src/backend/java/dev/engine_room/flywheel/backend/engine/terrain/TerrainDrawDispatcher.java` | 725–727 | Диспетчеризация MDI чанков Sodium через `glMultiDrawElementsIndirectCountARB` |
| `common/src/backend/resources/assets/flywheel/flywheel/internal/indirect/cull.glsl` | 35–40, 185–242 | SIMD-тест усеченного конуса на 6 FMA, тест окклюзии Hi-Z, компактизация голосованием подгрупп |
| `common/src/backend/resources/assets/flywheel/flywheel/internal/indirect/scatter.glsl` | 1–51 | Вычислительный шейдер scatter-копирования из стейджинг-буфера в целевые SSBO |
| `common/src/lib/java/dev/engine_room/flywheel/lib/util/ShadersModHelper.java` | 11–32 | Рефлексия в `net.irisshaders.iris.api.v0.IrisApi` для обнаружения активных шейдерпаков[^iris-update-8] |
| `meshlet/src/main/resources/assets/meshlet/flywheel/terrain/gl/mesh.mesh` | 18–19, 107–185 | NV Mesh Shader: кооперативная выборка вершин, тасовочные инструкции «бабочка», компактизация примитивов |

#### Репозиторий Nucleus (`c:\Projects\HBM-Modernized`)

| Путь к файлу (относительно `c:\Projects\HBM-Modernized`) | Диапазон строк | Архитектурная область и назначение |
|---|---|---|
| `src/main/java/com/hbm_m/client/render/MdiBatchCoordinator.java` | 89–94, 698–718, 1152–1175 | Шаг команды (32 байта), повторное использование чистого кадра (`submitClean`), непрямое исполнение |
| `src/main/java/com/hbm_m/client/render/MdiGeometryAtlas.java` | 61–68, 237–242, 522–567 | Шаг вершины 36 байт, шаг экземпляра 30 float, расширение буфера (`repackGeometryAndRefreshSlots`) |
| `src/main/java/com/hbm_m/client/render/InstancedStaticPartRenderer.java` | 289–305, 377–382, 426–455 | Преобразование в мировое пространство (`convertToWorldRecord`), допуск пропуска записи, безопасность Cleaner |
| `src/main/java/com/hbm_m/client/render/PersistentUploadStaging.java` | 47, 106–110, 142–148 | 4 МБ персистентное когерентное кольцо (`glBufferStorage`, `glFenceSync`, запасной путь) |
| `src/main/java/com/hbm_m/client/render/GpuSpanUploader.java` | 34–36, 56–102 | Обнаружение грязных диапазонов Уровня 2, слияние зазоров (`MERGE_GAP_FLOATS = 24`), теневой массив `vboShadow` |
| `src/main/java/com/hbm_m/client/render/IrisCompanionMesh.java` | 40–49, 113–129, 143–155 | Синтез формата `IrisVertexFormats.ENTITY`, попиксельные 8-точечные веса, динамический VBO освещения |
| `src/main/java/com/hbm_m/client/render/IrisInstancedBatchRenderer.java` | 344–370, 381–486, 494–550 | 4-уровневое исполнение Iris: Уровень 1 compute bake, Уровень 2 инстансинг, Уровень 3 цикл сопутствующих мешей |
| `src/main/java/com/hbm_m/client/render/NucleusGpuBaker.java` | 34–57, 643–750 | Вычислительный шейдер GLSL 4.3 трансформирует вершины и нормали в VRAM, нативная отрисовка шейдером пака |
| `src/main/java/com/hbm_m/client/render/IrisShadowBatchCollector.java` | 117–166, 180–298 | Сохранение матриц теневого прохода, сброс миксином перед копированием полупрозрачной глубины |
| `src/main/java/com/hbm_m/client/render/shader/IrisShadowDistortion.java` | 59–105 | Распознавание квартичной (Photon), линейной (BSL) и неискаженной проекций теней |
| `src/main/java/com/hbm_m/client/render/shader/IrisExtendedShaderAccess.java` | 114–181, 204–220 | Отслеживание поколений конвейера для предотвращения сбоев при повторном использовании ID программ в NVIDIA |
| `src/main/java/com/hbm_m/client/render/shader/IrisRenderBatch.java` | 40–70 | Персистентная пакетизация проходов, единичная оплата `shader.apply()` за проход |
| `src/main/java/com/hbm_m/client/render/LightSampleCache.java` | 70–94, 383–391 | Быстрый путь одного слота (`lastQueriedBE`), отступ от границы 1.5 см (`SAMPLE_INSET = 1/64`) |
| `src/main/java/com/hbm_m/client/render/culling/CpuFrustumCuller.java` | 45–65 | Извлечение 6 плоскостей пирамиды видимости по Гриббу/Хартманну и тестирование AABB |
| `src/main/java/com/hbm_m/client/render/culling/OcclusionCullingHelper.java` | 49–60, 177–216, 278–302 | Трассировка 15 воксельных лучей, темпоральный кэш, защита контрапций Create и теневого прохода |
| `src/main/java/com/hbm_m/client/render/RenderDistanceHelper.java` | 42–49 | Пропорциональный расчет LOD-затухания на последних 30% дистанции прорисовки |
| `src/main/java/com/hbm_m/client/render/NucleusDispatcherBypass.java` | 55–62 | Плоская итерация по `LIVE`, отмена обхода BER в Sodium, сокращение нагрузки на CPU на 25.4% |
| `src/main/java/com/hbm_m/platform/RenderHooks.java` | 22–40, 145–183 | Слой платформенных хуков: межверсионные `BufferBuilder`, `MeshData`, стеки матриц |

---

### Приложение B: Технический глоссарий и акронимы

- **AABB (Axis-Aligned Bounding Box / Ограничивающий параллелепипед, ориентированный по осям)**: Габаритный объем, грани которого параллельны координатным плоскостям, задаваемый минимальными и максимальными координатами $[X_{\min}, Y_{\min}, Z_{\min}]$ и $[X_{\max}, Y_{\max}, Z_{\max}]$.
- **BDA (Buffer Device Address / Прямой адрес буфера на устройстве)**: 64-битный адрес виртуальной памяти GPU, позволяющий шейдерам адресовать буфер напрямую (`GL_NV_shader_buffer_load` или Vulkan BDA) в обход традиционных дескрипторных сетов и точек привязки.
- **BER (BlockEntityRenderer / Рендерер сущностей блоков)**: Клиентский интерфейс Minecraft, отвечающий за динамическую отрисовку блочных сущностей. В стандартном подходе каждый BER инициирует индивидуальный вызов отрисовки (draw call) каждый кадр.
- **DSA (Direct State Access / Прямой доступ к состоянию)**: Расширение OpenGL 4.5 (`GL_ARB_direct_state_access`), позволяющее модифицировать объекты буферов, текстур и фреймбуферов напрямую по их дескриптору (handle) без связывания с контекстными таргетами (например, `glNamedBufferStorage` вместо связки `glBindBuffer` + `glBufferData`).
- **FMA (Fused Multiply-Add / Совмещенное умножение-сложение)**: Аппаратная инструкция GPU, вычисляющая $\text{fma}(a, b, c) = (a \cdot b) + c$ за одну операцию с единичным шагом округления, обеспечивая повышенную производительность и точность вычислений.
- **Forward-Z (Прямой Z)**: Традиционная конвенция проекции глубины OpenGL, при которой ближняя плоскость отсечения отображается в $Z = 0.0$, а дальняя — в $Z = 1.0$ в сочетании с тестом глубины `GL_LEQUAL`. Используется в Minecraft 1.20.1 и 1.21.1. В отсечении окклюзии Hi-Z под Forward-Z консервативная редукция требует операции `max()` по блоку $2 \times 2$ текселей, а объекты признаются перекрытыми при $Z_{\text{sphere\_near}} > Z_{\text{occluder\_max}}$.
- **FRAPI (Fabric Rendering API)**: Современный API конвейера клиентского рендеринга, обеспечивающий динамическую генерацию вершинных четырехугольников (quads), смешивание материалов и перенаправление моделей.
- **Hi-Z (Hierarchical-Z / Пирамида глубин)**: MIP-текстурная пирамида буфера глубины, где каждый пиксель на уровне $N+1$ содержит консервативный экстремум глубины соответствующих $2 \times 2$ текселей уровня $N$ (`max()` глубины для Forward-Z; `min()` глубины для Reversed-Z). Применяется для ультрабыстрого аппаратного тестирования перекрытия ограничивающих объемов на GPU.
- **LDS (Local Data Share) / Shared Memory (Разделяемая память)**: Встроенная в кристалл GPU высокоскоростная статическая память (SRAM), разделяемая между потоками внутри одной рабочей группы вычислительного шейдера (Compute Workgroup), обеспечивающая обмен данными с субнаносекундными задержками.
- **MDI (Multi-Draw Indirect / Непрямая множественная отрисовка)**: Функциональность OpenGL 4.3+ (`glMultiDrawElementsIndirect`), позволяющая запускать пакетное выполнение множества индексированных вызовов отрисовки за единичный вызов CPU, считывая параметры команд напрямую из буфера GPU (`GL_DRAW_INDIRECT_BUFFER`).
- **MLAB (Multi-Layer Alpha Blending / Многослойное альфа-смешивание)**: Аппаратно-ускоренный алгоритм независимой от порядка прозрачности (OIT), сохраняющий полупрозрачные фрагменты в попиксельных связных списках или ограниченных K-буферах в SSBO, за которыми следует сортирующий проход разрешения.
- **MRT (Multiple Render Targets / Множественные цели рендеринга)**: Возможность GPU выводить данные одного фрагментного шейдера одновременно в несколько цветовых текстур фреймбуфера (например, раздельный вывод альбедо, нормалей и параметров материалов в G-буфер отложенного рендеринга).
- **OIT (Order-Independent Transparency / Независимая от порядка прозрачность)**: Семейство алгоритмов растеризации, позволяющих корректно отображать перекрывающиеся полупрозрачные поверхности независимо от порядка передачи полигонов на отрисовку, исключая дорогостоящую сортировку на CPU.
- **Persistent Coherent Mapping (Персистентное когерентное отображение)**: Механизм OpenGL 4.4+ (`GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT`), позволяющий закрепить память буфера GPU в адресном пространстве CPU на все время работы приложения; изменения, записанные CPU, становятся автоматически видимыми для GPU без явных вызовов сброса кэша (`glFlushMappedBufferRange`).
- **Reversed-Z (Обратный Z)**: Конвенция буфера глубины, в которой ближняя плоскость отсечения проецируется в $Z = 1.0$, а дальняя — в $Z = 0.0$ при тесте глубины `GL_GEQUAL`. Широко применяется в современных графических движках (включая Minecraft 26.2 RHI) в сочетании с 32-битной плавающей запятой (`GL_DEPTH_COMPONENT32F`) для обеспечения равномерной точности глубины на любых дистанциях.
- **RHI (Render Hardware Interface / Аппаратный интерфейс рендеринга)**: Слой программной абстракции, изолирующий конвейеры и проходы рендеринга уровня движка от специфических графических API (Vulkan, DirectX 12, Metal, OpenGL).
- **SPD (Single Pass Downsampler / Однопроходный даунсэмплер)**: Алгоритм генерации полной цепочки MIP-уровней текстуры за единичный запуск вычислительного шейдера с использованием атомарных счетчиков рабочих групп и разделяемой памяти LDS.
- **SSBO (Shader Storage Buffer Object / Буферный объект хранения шейдера)**: Буферный объект OpenGL 4.3+ (`GL_SHADER_STORAGE_BUFFER`), доступный для произвольного чтения и записи из шейдеров, поддерживающий массивы неограниченного размера и атомарные операции.
- **Subgroup / Warp / Wavefront (Подгруппа / Варп / Вейвфронт)**: Базовая неделимая единица параллельного SIMD-исполнения на GPU (32 потока на NVIDIA, 32 или 64 потока на AMD). Инструкции подгрупп позволяют потокам внутри одного варпа обмениваться данными, голосовать и выполнять битовые перестановки без обращения к общей памяти LDS.
- **UBO (Uniform Buffer Object / Буферный объект юниформов)**: Буферный объект OpenGL (`GL_UNIFORM_BUFFER`), предназначенный исключительно для чтения постоянных данных (юниформов), оптимизированный для одновременной широковещательной рассылки во все потоки блока шейдеров.
- **VAO (Vertex Array Object / Объект массива вершин)**: Объект OpenGL, инкапсулирующий спецификацию форматов вершинных атрибутов и привязки базовых буферов VBO.

---

## 7. Приложение C: Глубокий анализ подсистемы шейдерпаков Iris в CrankShaft 1.4.0 и сравнительная переоценка

### 7.1 Архитектурный обзор и масштаб релиза (CrankShaft 1.4.0 / коммит `635b7b0`)

24 сентября 2026 года состоялся релиз CrankShaft версии **1.4.0** (коммит `635b7b0`: *"feat: release CrankShaft 1.4.0 with Iris shaderpacks and geometry-aware lighting"*), включающий **14 816 добавленных строк в 294 файлах**. Данный релиз кардинально трансформирует стратегию взаимодействия CrankShaft с шейдерными модификациями.

До версии 1.4.0 (как проанализировано в Разделах 2.10, 4.1 и 4.6), CrankShaft придерживался жесткой политики отказа: при активации любого шейдерпака Iris или OptiFine (`isShaderPackInUse() == true`) бэкенд Flywheel полностью отключался (`"flywheel:off"`), и игра откатывалась к непакетированной непосредственной отрисовке через стандартные `BlockEntityRenderer` (BER).

В CrankShaft 1.4.0 эта политика заменена специализированной **гостевой подсистемой конвейеров Iris** (`dev.engine_room.flywheel.iris.*`). Вместо отключения инстансинга или непрямой множественной отрисовки (MDI), CrankShaft 1.4.0 компилирует и привязывает специализированные **гостевые конвейеры** (`GuestPipelines.java`, `GuestProgram.java`), исполняемые непосредственно внутри стадий рендеринга активного шейдерпака. Визуальные экземпляры и чанки ландшафта Sodium растеризуются через собственные программы G-буфера, теней и полупрозрачности шейдерпака.

```
+----------------------------------------------------------------------------------------------------+
|                         ПОТОК ВЫПОЛНЕНИЯ ПОДСИСТЕМЫ IRIS В CRANKSHAFT 1.4.0                        |
+----------------------------------------------------------------------------------------------------+
|                                                                                                    |
|  [Загрузка шейдерпака Iris / Построение конвейера]                                                 |
|         │                                                                                          |
|         ├─► AST-анализ через glsl-transformer (SingleASTTransformer)                               |
|         │   - Сканирование вершинных и фрагментных шейдеров (TransformPatcher.patchVanilla)        |
|         │   - Сдвиг привязок SSBO: layout(binding = N + 8) buffer (резерв 0..7 для Flywheel)       |
|         │   - Снятие квалификатора in: iris_Position, mc_Entity, at_midBlock -> глобальные перем.    |
|         │   - Перенаправление main() -> _flw_irisMain(); инъекция пролога _flw_guestVertex()       |
|         │                                                                                          |
|         └─► Разрешение контракта шейдерпака (ContractProperties)                                   |
|             - Проверка контракта Colorwheel (программы clrwl_*, colorwheel.properties)             |
|             - Оценка встроенных адаптеров JSON (bsl, complementary, solas, sundial и др.)          |
|             - Сопоставление по SHA-256 сигнатурам AST/исходников (защита от смены версий)         |
|                                                                                                    |
|  [Жизненный цикл покадрового рендеринга]                                                           |
|         │                                                                                          |
|         ├─► Фаза 1: Теневой проход (Хук Iris ShadowRenderer)                                       |
|         │   - GuestShadowCull: Загрузка плоскостей пирамиды (до 13) в UBO (_FlwShadowCull: 13)     |
|         │   - Вычислительное отсечение в буфер Фазы 2 через shadow_cull.glsl                       |
|         │   - Отправка MDI теневого прохода (GuestIndirectDrawManager.drawShadow)                  |
|         │   - Вторичный полупрозрачный теневой проход (drawShadowTranslucent)                      |
|         │                                                                                          |
|         ├─► Фаза 2: Непрозрачный проход G-буфера (Стык пост-непрозрачности)                        |
|         │   - Подготовка движка: Uniforms.update, сброс окружения, валидация vertex extras         |
|         │   - Объединенная отправка Фазы 1 и Фазы 2 MDI на стыке пост-непрозрачности               |
|         │     (Предотвращает попадание Фазы 2 после отложенных композитных проходов Iris)          |
|         │                                                                                          |
|         ├─► Фаза 3: Полупрозрачность и независимая от порядка прозрачность (OIT)                   |
|         │   - Если активен Colorwheel OIT: Многопроходный вейвлетный OIT тригонометрических моментов |
|         │     (oit_depth_range -> oit_coefficients -> oit_evaluate -> oit_composite)               |
|         │   - Если активен профиль Sundial Deferred: Послойный захват фрагментов и разрешение      |
|         │   - Воспроизведение полупрозрачных чанков Sodium сливается в OIT-проходы экземпляров     |
|         │                                                                                          |
|         └─► Фаза 4: Восстановление состояния и отвязка SSBO                                        |
|             - GuestSsbos.restore(pipeline): Отвязка гостевых буферов, восстановление памяти Iris   |
+----------------------------------------------------------------------------------------------------+
```

---

### 7.2 Гостевой движок и конвейер Multi-Draw Indirect (`GuestEngine`, `GuestIndirectDrawManager`)

Архитектура гостевого исполнения базируется на двух ключевых классах: `GuestEngine` и `GuestIndirectDrawManager`, регистрируемых в `IrisBackends.java`:

- **`flywheel:iris_indirect`** (Приоритет 890, либо Приоритет 1 на видеокартах Intel): Активируется при условии `GlCompat.SUPPORTS_INDIRECT && IndirectPrograms.allLoaded() && IrisGate.isPackInUse()`.
- **`flywheel:iris_instancing`** (Приоритет 490): Запасной путь массивов инстансинга, выбираемый на драйверах без полноценной поддержки непрямой отрисовки или вычислительных шейдеров под Iris.

#### 7.2.1 Координация жизненного цикла кадра в `GuestEngine.java`
`GuestEngine` расширяет `EngineImpl` и реализует мост жизненного цикла конвейера Iris:
1. **Асинхронный вход в проходы**: Iris выполняет теневой проход *до* основного прохода непрозрачного ландшафта и сущностей Minecraft. `GuestEngine` отслеживает состояние инициализации через `preparedForMainPass`. Какой бы проход ни выполнялся первым (теневой или непрозрачный), вызывается `prepare(context)`:
   - Сбрасывается `EnvironmentStorage`.
   - Синхронизируются блоки юниформов через `Uniforms.update(context)`.
   - Проверяется `vertexExtras.blockIdsChanged()`; если Iris перезагрузил сопоставления ID состояний блоков, метод `meshPool.invalidateExtras()` сбрасывает кэшированные атрибуты мешей пула.
2. **Оркестрация теневого прохода**:
   - `renderShadow(...)` проверяет директивы `PackShadowDirectives` и `ContractProperties.shadowEnabled()`.
   - Вычисляет модельно-видовые координаты относительно начала отсчета:
     $$\mathbf{MV}_{\text{rel}} = \mathbf{T}(\text{origin} - \text{camera}) \cdot \mathbf{MV}_{\text{shadow}}$$
   - Делегирует выполнение в `guest.drawShadow(...)`. Если активный шейдерпак требует полупрозрачных теней (`directives.shouldRenderTranslucent()`), он кэширует матрицу `translucentShadowModelView` для последующего вызова в `renderShadowTranslucent(...)` после того, как Iris скопирует текстуру глубины непрозрачных теней.
3. **Изоляция привязок SSBO**: Все блоки гостевого исполнения обернуты в конструкцию `try ... finally { GuestSsbos.restore(pipeline); }`, гарантируя, что внутренние SSBO Flywheel никогда не утекут в стадии постобработки или композитные шейдеры Iris.

#### 7.2.2 Механика выполнения в `GuestIndirectDrawManager.java`
`GuestIndirectDrawManager` переопределяет `IndirectDrawManager`, адаптируя диспетчеризацию MDI под требования шейдерпаков:
- **Консолидация стыка двухфазного Hi-Z**: В ванильном Flywheel Фаза 1 (видимые объекты из пирамиды глубин прошлого кадра) и Фаза 2 (объекты, открывшиеся в текущем кадре) разделены между непрозрачным проходом и даунсэмплингом глубины. Под Iris отложенные композитные шейдеры начинают работу сразу после непрозрачного прохода. Отправка Фазы 2 после композитных проходов привела бы к потере отложенного освещения, свечения (bloom) и ambient occlusion для вновь открывшихся объектов. Поэтому `drawOpaque(...)` отправляет обе фазы последовательно на стыке пост-непрозрачности:
  ```java
  submitMain(renderModelView);
  submitPass2IfPending();
  ```
- **Разделение отрисовки Entity и BlockEntity**: Шейдерпаки часто используют принципиально разные шейдеры и юниформы для сущностей и блоков (например, `gbuffers_entities` против `gbuffers_block`). Метод `submitUberPass` разделяет пакеты отрисовки на `blockScratch` и `entityScratch` с использованием `TaggedEnvironment.isEntity(batch.drawTag())`. Блочные вызовы отправляются с `pipelineFor`, а вызовы сущностей — под именем `label + "_entities"` с `role.forEntities()`.
- **Гейтинг несовместимости супер-пакетов (Uber-Batch)**: В дополнение к совместимости материалов и текстур, `incompatibleUber` устанавливает границы пакетов при переходе между тегами сущностей и блоков, а также между режимами альфа-отсечения:
  ```java
  @Override
  protected boolean incompatibleUber(IndirectDraw a, IndirectDraw b) {
      return super.incompatibleUber(a, b)
          || a.material().cutout() != b.material().cutout()
          || TaggedEnvironment.isEntity(a.drawTag()) != TaggedEnvironment.isEntity(b.drawTag());
  }
  ```

---

### 7.3 Компиляция шейдеров и динамическая AST-трансформация через `glsl-transformer` (`GuestShaders`, `GuestPipelines`)

CrankShaft 1.4.0 не внедряет жестко закодированные шейдеры GLSL для шейдерпаков. Вместо этого он динамически трансформирует нативный исходный код активного шейдерпака во время выполнения с помощью библиотеки `io.github.douira.glsl_transformer.ast.transform.SingleASTTransformer`.

#### 7.3.1 Конвейер трансформации
1. **Ванильный патчинг Iris**: Исходный код шейдерпака сначала проходит через `TransformPatcher.patchVanilla(...)` в Iris для привязки стандартных структур юниформов, сэмплеров текстур и логики альфа-теста.
2. **Сдвиг привязок SSBO (`shiftBufferBindings`)**:
   Шейдерпаки, использующие вычислительные шейдеры или SSBO (современные PBR и path-tracing паки), часто объявляют буферы хранения начиная с индекса `layout(binding = 0)`. Непрямой движок Flywheel резервирует точки привязки с 0 по 7 под собственные нужды:
   - Привязка 0: `IndirectBuffers` (Параметры команд отрисовки)
   - Привязка 1: `LightBuffers` (Таблицы LUT и уровни света секций)
   - Привязка 2: `MatrixBuffer` (Модельно-видовые матрицы и матрицы нормалей)
   - Привязки 3–7: Слэбы данных экземпляров и вспомогательное хранилище
   
   Метод `GuestShaders.shiftBufferBindings` парсит абстрактное синтаксическое дерево (AST) GLSL, находит все узлы `InterfaceBlockDeclaration` с типом `StorageQualifier.StorageType.BUFFER` и увеличивает их индексы привязок на `GuestSsbos.BINDING_OFFSET = 8`. Если у SSBO отсутствует явный квалификатор привязки, синтезируется `layout(binding = 8)`.
3. **Деградация вершинных входов и перенаправление точки входа (`rewriteInputs`)**:
   В стандартных конвейерах Iris вершинные данные поступают через фиксированные атрибуты (`iris_Position`, `mc_Entity`, `at_tangent` и т.д.). В непрямом движке Flywheel координаты вершин и матрицы экземпляров загружаются из объединенных буферов вершин и массивов экземпляров в SSBO.
   
   Для преодоления этого несоответствия без изменения логики шейдерпака:
   - `rewriteInputs` обходит AST и находит объявления известных вершинных входов Iris.
   - **Снимает с них квалификатор хранения `in`**, преобразуя их в глобальные мутабельные переменные.
   - Переименовывает исходную функцию `main()` шейдерпака в `_flw_irisMain()`.
   - Находит инициализаторы глобальных переменных, ссылающиеся на эти входы (например, смещения мировых координат), и переносит их вычисление внутрь сгенерированной `main()`.
   - Синтезирует новую функцию `main()`:
     ```glsl
     #version 460 core
     // ... смещенные объявления шейдерпака ...
     void main() {
         _flw_guestVertex(); // Вычисление трансформации Flywheel и распаковка экземпляра
         iris_Position = vec4(flw_vertexPos.xyz, 1.0);
         iris_Color = flw_vertexColor;
         iris_Normal = vec4(flw_vertexNormal, 0.0);
         iris_UV0 = vec4(flw_vertexTexCoord, 0.0, 1.0);
         iris_UV1 = ivec4(flw_vertexOverlay, 0, 0);
         iris_UV2 = vec4(round(flw_vertexLight * 256.0 - 8.0), 0.0, 1.0);
         iris_Entity = _flw_guestIrisEntity();
         iris_LineWidth = vec4(1.0);
         mc_Entity = ivec4(_flw_aIrisEntity, 0, 1);
         mc_midTexCoord = vec4(_flw_irisMidTexCoord, 0.0, 1.0);
         at_tangent = _flw_guestTangent();
         at_midBlock = _flw_irisMidBlock;
         // ... отложенные инициализаторы ...
         _flw_irisMain(); // Выполнение исходной вершинной логики шейдерпака
     }
     ```
4. **Деградация выходов фрагментного шейдера (`demoteOutputs`)**:
   Для обеспечения независимой от порядка прозрачности (OIT) выходные переменные фрагментного шейдера (например, `layout(location = 0) out vec4 fragColor`) лишаются квалификатора `out` и преобразуются в переменные уровня модуля. Исходная `main()` переименовывается в `_flw_irisMain()`. Синтезированная точка входа вызывает `_flw_irisMain()`, а затем направляет полученные значения цвета и глубины в процедуры накопления `GuestOitCodegen.producer`.

---

### 7.4 Архитектура прохода теней и Frustum Culling на GPU (`GuestShadows`, `GuestShadowCull`, полупрозрачные тени)

В шейдерпаках Minecraft проход теней визуализирует сцену с точки зрения направленного источника света (солнца или луны) в закадровую карту глубины и цвета теней.

#### 7.4.1 Реплицированный GPU Shadow Frustum Culling (`GuestShadowCull.java`)
Iris рассчитывает отсечение теней на CPU с помощью `ShadowRenderer.FRUSTUM`, реализующего комплексные геометрические проверки:
- До 13 плоскостей отсечения (`AdvancedShadowCullingFrustum`).
- Дистанционные отсекатели ограничивающих параллелепипедов (`BoxCuller`).
- Безопасные зоны ортографической проекции света (`SafeZoneCullingFrustum`).

Для предотвращения задержек драйвера на CPU и исключения обратного чтения видимости экземпляров, `GuestShadowCull` полностью реплицирует пирамиду теней Iris на GPU:
- Буфер юниформов `_FlwShadowCull` (Привязка 13, 232 байта в раскладке std140):
  ```glsl
  layout(std140, binding = 13) uniform _FlwShadowCull {
      vec4 planes[13];
      vec3 originOffset;
      int planeCount;
      float maxDistance;
      float safeZone;
  };
  ```
- Вычислительный шейдер `shadow_cull.glsl` (Размер локальной группы 64):
  Проверяет пересечение ограничивающих сфер экземпляров с 13 плоскостями теней:
  $$\mathbf{P}_{\text{world}} = \mathbf{P}_{\text{sphere}} + \mathbf{O}_{\text{origin}}$$
  $$\forall i \in [0, \text{planeCount}-1]: \quad \mathbf{n}_i \cdot \mathbf{P}_{\text{world}} + w_i \ge -R$$
  Экземпляры, прошедшие тест, записываются напрямую во вторичный буфер непрямых команд (`cullIntoPass2`).

#### 7.4.2 Проход полупрозрачных теней и подавление стандартных теней сущностей
- **Полупрозрачный теневой проход**: Цветные тени (например, от витражного стекла или силовых полей) требуют рендеринга полупрозрачной геометрии в карту теней. Метод `GuestEngine.renderShadowTranslucent` выполняет специализированный проход с использованием шейдеров `clrwl_shadow_translucent` или нативных шейдеров воды после того, как Iris скопирует карту глубины непрозрачных теней.
- **Подавление стандартных круглых теней сущностей (`GuestEntityShadows.java`)**: Шейдерпаки с динамическими мягкими тенями или контактными тенями отключают ванильные круглые тени-диски под мобами. Когда флаг `pipeline.shouldDisableVanillaEntityShadows()` активен, `GuestIndirectDrawManager` перехватывает пакеты отрисовки с текстурой `textures/misc/shadow.png` и исключает их из списка непрямых вызовов.

---

### 7.5 Расширенные геометрические атрибуты и тегирование Entity/BlockEntity (`GuestVertexExtras`, `GuestDrawTags`)

Современные шейдерпаки требуют детальных попиксельных и повершинных геометрических дескрипторов для реализации PBR-материалов, Parallax Occlusion Mapping (POM), колыхания листвы на ветру и подповерхностного рассеяния света.

#### 7.5.1 Расширенный формат вершин Iris (`GuestVertexExtras.java`)
`GuestVertexExtras` внедряет дополнительный 20-байтный блок атрибутов в объединенные полигональные сетки Flywheel:

| Атрибут | Имя в GLSL | Формат | Шаг / Смещение | Описание |
|---|---|---|---|---|
| **IrisEntity** | `mc_Entity` | `GpuFormat.RG16_SINT` | 4 байта (смещение 0) | ID блока или ID сущности; тип рендера (0 = ландшафт, 1 = сущность) |
| **MidTexCoord** | `mc_midTexCoord` | `GpuFormat.RG32_FLOAT` | 8 байт (смещение 4) | Центроидные UV-координаты $(u_{\text{mid}}, v_{\text{mid}})$ родительского четырехугольника |
| **Tangent** | `at_tangent` | `GpuFormat.RGBA8_SNORM` | 4 байта (смещение 12) | Тангентный вектор поверхности $\mathbf{T}_{xyz}$ и знаковый бит хиральности $w$ |
| **MidBlock** | `at_midBlock` | `GpuFormat.RGBA8_SNORM` | 4 байта (смещение 16) | Относительное смещение от вершины к центру блока $[x - 0.5, y - 0.5, z - 0.5]$ |

- **Вычисление касательных (Tangent)**: Рассчитывается для каждого полигона через `NormalHelper.computeTangent`, обеспечивая корректное рельефное текстурирование нормалей на повернутых объектах.
- **Динамическая инвалидация**: Если игрок меняет пакет ресурсов или Iris обновляет таблицы идентификаторов блоков (`WorldRenderingSettings.INSTANCE.getBlockStateIds()`), метод `blockIdsChanged()` возвращает true, инвалидируя срезы атрибутов в пуле мешей без необходимости повторной компиляции базовой геометрии.

#### 7.5.2 Разрешение материалов и тегов отрисовки (`GuestDrawTags.java`)
- Сопоставляет состояния блочных сущностей с целочисленными идентификаторами Iris (`WorldRenderingSettings.INSTANCE.getBlockStateIds()`).
- Сопоставляет экземпляры сущностей с реестром сущностей Iris (`WorldRenderingSettings.INSTANCE.getEntityIds()`), включая особые преобразования (например, зомби-жителей).
- Извлекает идентификаторы моделей предметов для рамок и удерживаемых в руках предметов.

---

### 7.6 Контракт шейдерпаков Colorwheel против встроенных адаптеров (`ContractProperties`, `ContractProgram`, встроенные адаптеры)

Чтобы исключить хрупкие эвристики и добиться безупречной интеграции, CrankShaft внедряет и расширяет **контракт шейдерпаков Colorwheel**, сохраняя при этом набор **встроенных адаптеров** для популярных паков.

#### 7.6.1 Контракт шейдерпаков Colorwheel
Шейдерпаки, поддерживающие спецификацию Colorwheel, поставляют специализированные программы с префиксом `clrwl_` и файл дескриптора `colorwheel.properties`:
- **Программы контракта (`ContractProgram.java`)**:
  - `clrwl_gbuffers`: Основной проход непрозрачного ландшафта и блоков.
  - `clrwl_gbuffers_entities`: Проход непрозрачных сущностей (расширение CrankShaft к Colorwheel).
  - `clrwl_gbuffers_translucent`: Проход полупрозрачности с поддержкой независимой от порядка сортировки.
  - `clrwl_gbuffers_additive`, `clrwl_gbuffers_glint`, `clrwl_gbuffers_lightning`, `clrwl_gbuffers_damagedblock`.
  - `clrwl_shadow`, `clrwl_shadow_translucent`, `clrwl_shadow_additive`.
- **Интерфейс фрагментного шейдера**:
  Программы контракта не пишут напрямую в массивы `gl_FragData[N]`. Вместо этого они экспортируют стандартизированную сигнатуру функции:
  ```glsl
  void clrwl_computeFragment(
      vec4 sampleColor,
      out vec4 fragColor,
      out vec2 fragLight,
      out float ao,
      out vec4 fragOverlay
  );
  ```
  Flywheel оборачивает этот вызов собственной логикой расчета диффузного освещения, масштабирования ambient occlusion и альфа-отсечения.
- **Конфигурация дескриптора (`ContractProperties.java`)**:
  Парсит `colorwheel.properties` для считывания параметров:
  - `shadow.enabled = true | false`
  - `oit = true | false` (включение/отключение OIT для каждого прохода)
  - `oit.gbuffers.coefficientRanks = 1, 2, 3` (ранги вейвлетных моментов)
  - `blend.<program> = srcRgb dstRgb srcAlpha dstAlpha` (пользовательские режимы блендинга фреймбуфера)

#### 7.6.2 Встроенные адаптеры шейдерпаков (`assets/flywheel/iris/patches/`)
Для широко распространенных шейдерпаков, пока не внедривших нативный контракт Colorwheel, CrankShaft поставляет JSON-адаптеры Схемы 2:

| Файл адаптера | Целевой шейдерпак | Эвристика сопоставления | Применяемый механизм |
|---|---|---|---|
| `complementary.json` | Complementary Reimagined & Unbound (r5.9.3) | Совпадение исходного кода: `gbuffers_entities.glsl`, `lib/common.glsl` | Патчит `clrwl_gbuffers_entities` через `complementary.patch`; включает полный OIT |
| `bsl.json` | BSL Shaders (v10.1.5) | SHA-256 хеш-сигнатура: `a2e283ad...` файла `gbuffers_water.glsl` | Внедряет `forwardOit` в `gbuffers_water`; валидирует альфа-смешивание source-over |
| `solas.json` | Solas Shaders (V3.7b) | Совпадение прагм в заголовках Solas | Патчит стадии сущностей и полупрозрачности через `solas.patch` |
| `sundial.json` | Sundial (Alpha Build 2026-08-28) | Совпадение кода в `Water.frag` и `Textured.frag` | Оборачивает стадии полупрозрачности и теней; активирует `-Dcrankshaft.iris.oit.deferred=true` |
| `makeup.json` | MakeUp UltraFast (9.5e) | Проверка строковых маркеров в шейдере | Синтезирует контракты прямого OIT |
| `iteration.json` | IterationRP (Alpha 0.8.28) | Совпадение заголовка | Создает мост геометрических стадий; явно отключает OIT по директивам пака |

**Принцип антихрупкости:** Адаптеры валидируют внутренние определения функций GLSL и криптографические контрольные суммы SHA-256 фрагментов кода, а не имена файлов или версии в строках. Если автор шейдерпака выпускает обновление, ломающее смещения патча, адаптер корректно отключается, возвращая систему к стандартному базовому рендерингу без сбоев игры.

---

### 7.7 Интеграция с ландшафтом чанков Sodium и экспериментальные NV Mesh Shaders под Iris (`IrisTerrainRasterizer`)

Помимо визуальных сущностей, CrankShaft 1.4.0 распространяет GPU-отсечение и растеризацию мешлетов на **блоки ландшафта чанков Sodium** при включенных шейдерпаках через `IrisTerrainRasterizer.java`.

#### 7.7.1 Механика растеризации ландшафта
При включении через флаг запуска (`-Dcrankshaft.iris.terrain=true`), CrankShaft перехватывает конвейер отрисовки ландшафта Sodium:
1. **Конверсия команд (`convert.comp`)**: Преобразует команды отрисовки регионов Sodium в цепочки исполнения мешлетов.
2. **Восстановление окклюзии Фазы 2 (`recovery.comp` / `recovery_cached.comp`)**: Повторно тестирует перекрытые мешлеты ландшафта по обновленной текстуре глубины `GuestTerrainGate.meshDepthTexture`.
3. **Компактизация команд (`compact.comp`)**: Уплотняет непрерывные потоки команд с помощью атомарных счетчиков рабочих групп GPU.

#### 7.7.2 Экспериментальный конвейер NVIDIA Mesh Shader (`-Dcrankshaft.iris.mesh=true`)
На графических процессорах NVIDIA (микроархитектура Turing и новее), CrankShaft способен полностью обойти этап аппаратного сборщика вершин (fixed-function vertex fetching):
- **Выбор бэкенда**: Автоматически активирует режим `flywheel:iris_mesh_shader`.
- **Конвейер шейдеров (`GuestTerrainMeshShaders.java`)**:
  - `task.glsl`: Task shader, выполняющий отсечение по усеченному конусу и проверку по пирамиде глубин Hi-Z на уровне рабочих групп.
  - `mesh.glsl`: Mesh shader, извлекающий упакованные буферы вершин, декодирующий нормали и генерирующий до 64 вершин и 126 треугольников на мешлет.
  - Фрагментный шейдер: Прямое вычисление G-буфера или карт теней шейдерпака.
- **Опция прямой отправки (`-Dcrankshaft.iris.mesh.direct=true`)**: Отключает отсечение окклюзии в task shader для непрозрачного ландшафта, снижая накладные расходы на вызовы compute, когда узким местом выступает скорость заполнения (fillrate).

---

### 7.8 Независимая от порядка прозрачность (OIT) под шейдерпаками: профили вейвлетов и отложенных слоев

Рендеринг полупрозрачных поверхностей под шейдерпаками подвержен артефактам сортировки полигонов. CrankShaft 1.4.0 интегрирует две раздельные архитектуры OIT:

#### 7.8.1 Вейвлетный OIT тригонометрических моментов
Применяется для прямых проходов полупрозрачности (`clrwl_gbuffers_translucent` и паков, поддерживающих `forwardOit`):
1. **Проход 1 (`DEPTH_RANGE`)**: Фиксирует минимальную и максимальную глубину $[Z_{\text{near}}, Z_{\text{far}}]$ всех прозрачных поверхностей, пересекающих каждый пиксель экрана.
2. **Проход 2 (`COEFFICIENTS`)**: Накапливает коэффициенты ряда тригонометрических моментов:
   $$b_k = \int_0^1 \cos(k \pi z) \, \alpha(z) \, dz, \quad c_k = \int_0^1 \sin(k \pi z) \, \alpha(z) \, dz$$
   Коэффициенты сохраняются в массивах 2D-текстур (`_flw_coefficients0..N`).
3. **Проход 3 (`EVALUATE`)**: Восстанавливает непрерывную функцию пропускания света $T(z)$, оценивает оптическое поглощение, накладывает пространственный синий шум (`_flw_blueNoise`) для дизеринга и аккумулирует освещенный цвет в буферы `_flw_accumulate0..N`.
4. **Проход 4 (`COMPOSITE`)**: Полноэкранный проход композитинга (`oitComposite`) накладывает накопленный свет в целевой фреймбуфер активного шейдерпака.

#### 7.8.2 Многослойное альфа-смешивание (MLAB / K-буфер) и профиль отложенного OIT Sundial (`-Dcrankshaft.iris.oit.deferred=true`)
В то время как прямой рендеринг полупрозрачности опирается на математическую аппроксимацию моментов (вейвлетный OIT) во избежание ограничений по объему VRAM на пиксель, отложенные PBR-шейдерпаки (такие как Sundial) производят вычисления отражений, преломлений и атмосферного рассеяния на пост-геометрических композитных стадиях (`composite1` – `composite6`). Для подобных конвейеров в CrankShaft реализована дискретная архитектура многослойного альфа-смешивания (Multi-Layer Alpha Blending, MLAB):
- **Попиксельные связные списки (A-Buffer / K-Buffer)**: Во время растеризации полупрозрачной геометрии атомарный счетчик выделяет узлы фрагментов из глобального пула хранения SSBO (`layer_storage.glsl`). Каждый узел сохраняет цвет RGBA, нормаль в мировых координатах, линейную глубину и параметры шероховатости/зеркальности материала в односвязный список, привязанный к координате пикселя.
- **Вычислительная битоническая сортировка слиянием**: До выполнения композитного затенения `DeferredOitRenderer.java` запускает вычислительный проход, который обходит список фрагментов каждого пикселя, сохраняет до $K$ слоев по глубине (по умолчанию $K=8$) и выполняет регистровую битоническую сортировку слиянием вдоль луча зрения.
- **Реконструкция отложенного G-буфера**: Отсортированные $K$ слоев разрешаются и записываются обратно в промежуточные текстуры G-буфера. Это позволяет отложенным проходам освещения точно рассчитывать зеркальные отражения и коэффициенты Френеля для каждого полупрозрачного слоя в отдельности, полностью устраняя схлопывание цветовых градаций, характерное для однослойного альфа-смешивания.

---

### 7.9 Сравнительная переоценка бок о бок: гостевой патчинг CrankShaft 1.4.0 против 4-уровневого конвейера Nucleus

С выходом CrankShaft 1.4.0 **Домен 5 (Интероперабельность с модами на шейдеры)** требует фундаментального пересмотра. CrankShaft больше не является движком с «политикой отказа»; теперь он представляет собой сложную систему гостевого патчинга на базе вычислительных шейдеров.

#### Обновленная сводная матрица: Домен 5 (Интероперабельность с модами на шейдеры)

| Архитектурное измерение | CrankShaft 1.4.0 (Гостевой конвейер перезаписи AST) | Nucleus (Нативная 4-уровневая вычислительная интероперабельность) |
|---|---|---|
| **Базовая философия** | **Перезапись AST на лету**: Модифицирует исходный GLSL-код шейдерпака через `glsl-transformer` для приема вершинных и инстанс-буферов Flywheel. | **Трансформация вершин в VRAM**: Вычислительный запекатель GLSL 4.3 трансформирует геометрию в VBO, отрисовывая ее через нетронутые нативные шейдеры пака. |
| **Совместимость с шейдерпаками** | Высокая для протестированных паков (контракт Colorwheel или встроенные JSON-адаптеры); хрупкая для неизвестных и экспериментальных паков. | Абсолютная (100% математическая точность на любых шейдерпаках без необходимости адаптеров или специальных контрактов). |
| **Интеграция теневого прохода** | MDI-отрисовка с отсечением на GPU через `GuestShadowCull` и `shadow_cull.glsl` (13 плоскостей пирамиды в UBO); поддержка полупрозрачных теней. | Глобальный коллектор теневых пакетов (`IrisShadowBatchCollector`) + математическая компенсация дисторсии (`IrisShadowDistortion`). |
| **Обработка дисторсии теней** | Неявная: Модифицированные вершинные шейдеры исполняют оригинальные функции дисторсии самого шейдерпака. | Явная: Детектирует квартичную (Photon) и линейную (BSL) функции искажения, воспроизводя их в инстанс-шейдерах Уровня 2. |
| **Пригодность архитектуры моделей** | Оптимизирована под однородные блочные и простые модели сущностей (ванильные кубы, механизмы Create, ландшафт Sodium). | Специально спроектирована под неоднородные многосоставные иерархии OBJ-мультиблоков с 8-точечной трилинейной интерполяцией света. |
| **Накладные расходы на компиляцию** | Высокие: Парсинг AST, лексический анализ, сдвиг привязок SSBO и повторная компиляция десятков вариантов шейдеров при каждой смене пака. | Практически нулевые: Заранее скомпилированный вычислительный шейдер запекания (`NucleusGpuBaker`); ноль дополнительных компиляций при смене пака. |
| **Независимая от порядка прозрачность (OIT)** | Многопроходный вейвлетный OIT (тригонометрические моменты) + отложенный послойный OIT MLAB (`DeferredOitRenderer`). | Глобально отсортированный проход альфа-смешивания сзади наперед + глубинная сортировка запекания Уровня 1. |
| **Требования к оборудованию и драйверам** | Высокие: OpenGL 4.5 Direct State Access (`GL45C`), GLSL 4.60 Core, вычислительные шейдеры, RHI Blaze3D под Java 25+. | Гибкие: Работает в диапазоне от OpenGL 3.2 Core до 4.4+ (персистентные когерентные буферы); полная совместимость с macOS и графикой Intel. |

#### 7.9.1 Патчинг программ по AST против запекания в VRAM на Compute-шейдерах и инстансинга Уровней 2/3
- **Конвейер патчинга AST в CrankShaft (`glsl-transformer`)**:
  CrankShaft перехватывает код шейдерпаков на уровне абстрактного синтаксического дерева (AST). Мутируя исходный код шейдера перед компиляцией в видеодрайвере, движок инжектирует структуры вершинных макетов Flywheel и переназначает привязки ресурсов (например, понижает глобальные uniform-переменные и сдвигает индексы привязок SSBO). Это позволяет инстансированным и MDI-потокам Flywheel исполняться непосредственно во фрагментных стадиях шейдерпака без дублирования геометрии в VRAM. Однако подобный подход порождает существенные задержки компиляции при запуске и перезагрузке шейдеров, а также крайне уязвим к нестандартным синтаксическим конструкциям в сильно обфусцированных сторонних паках.
- **GPU Compute Baker Уровня 1 в Nucleus (`NucleusGpuBaker`)**:
  Nucleus полностью изолирует интероперабельность с шейдерпаками на стороне геометрии. Вместо синтаксического разбора или модификации исходного кода сторонних шейдеров `NucleusGpuBaker` выполняет специализированный вычислительный шейдер GLSL 4.3 непосредственно в VRAM. Шейдер трансформирует анимированные матрицы костей составных частей, вычисляет 8-точечные волюметрические карты освещения (`LightSampleCache`) и записывает стандартные ванильные вершинные форматы прямо в клиентские VBO. Полученная геометрия отрисовывается через **абсолютно неизмененные оригинальные шейдеры пака** (`gbuffers_terrain`, `gbuffers_entities`). Это гарантирует 100% стабильность работы любых шейдерпаков и полное отсутствие задержек на повторную компиляцию шейдеров во время игры.
- **Инстансированная интероперабельность Nucleus на Уровнях 2 и 3**:
  Для более простых инстансированных объектов Уровень 2 в Nucleus задействует `ExtendedShader` с аналитическим внедрением функций дисторсии теней (`IrisShadowDistortion`). Уровень 3 (`IrisRenderBatch`) координирует состояния пакетов, обновляя uniform-переменные и текстурные привязки лишь один раз за проход рендеринга вместо отдельных переключений на каждую отрисовку сетки, что обеспечивает колоссальную пропускную способность при большом числе динамических объектов.

#### 7.9.2 Конвейер теневого прохода: отсечение пирамиды теней на GPU против компенсации кривых дисторсии
- **Frustum Culling теней на GPU в CrankShaft (`GuestShadowCull`)**:
  CrankShaft оптимизирует теневой проход, выгружая 13-плоскостную ограничивающую пирамиду (6 плоскостей основной камеры, 6 направленных плоскостей теневого объема и 1 ближняя плоскость отсечения) в SSBO/UBO. Вычислительный шейдер `shadow_cull.glsl` параллельно проверяет ограничивающие сферы инстансов против всех 13 плоскостей, записывая смещения активных инстансов в выходные массивы и обновляя счетчик indirect-команд через `glMultiDrawElementsIndirectCountARB`. Избавляя CPU от накладных расходов на куллинг, CrankShaft при этом полностью делегирует расчет искажений теней пропатченному вершинному шейдеру пака.
- **Компенсация дисторсии в Nucleus (`IrisShadowDistortion`)**:
  Nucleus задействует глобальный коллектор теней CPU/GPU (`IrisShadowBatchCollector`). Поскольку нелинейные искажения карт теней (такие как полиномиальное искажение в BSL или квартичное искажение в Photon) неравномерно искривляют координаты пространства теней, Nucleus анализирует исходный код шейдерпака для извлечения коэффициентов дисторсии. Затем он производит расчет обратной кривой дисторсии внутри `IrisShadowDistortion`, корректируя границы ограничивающих объемов. Это предотвращает артефакты акне теней, клиппинг и отрыв теней на стыках крупногабаритных промышленных мультиблочных конструкций.

#### 7.9.3 Структурная специализация моделей: однородный инстансинг против неоднородных мультиблоков
- **Фокус CrankShaft на однородных моделях**:
  Гостевой конвейер CrankShaft оптимизирован под однородные блоки, тайловые сущности и блочные чанки (механизмы Create, стандартные кубы Minecraft). Все инстансы обладают идентичной топологией вершин и общим пространством координат карт освещения.
- **Архитектура неоднородных мультиблоков Nucleus**:
  Специфика HBM-Modernized требует отрисовки масштабных индустриальных установок (центрифуги, химические заводы, ядерные турбины, ускорители частиц), включающих десятки динамических составных деталей, шарнирных кинематических связей и независимых осей вращения. Nucleus эффективно управляет ими с помощью `MdiGeometryAtlas` (запекание разнообразных подсеток в монолитную память), трилинейной 8-точечной интерполяции карт освещения и механизма `NucleusDispatcherBypass` (обход итерации тайловых сущностей в Sodium при рендеринге, экономящий более 25% процессорного времени на кадр). CrankShaft принципиально не рассчитан на многокостные иерархии деталей и обход стандартных диспетчеров.

#### 7.9.4 Аппаратные возможности, переносимость драйверов и ограничения RHI
- **Жесткая привязка CrankShaft к современному RHI**:
  CrankShaft 1.4.0 неразрывно связан с архитектурой Blaze3D RHI в Minecraft 26.2 (`com.mojang.blaze3d.pipeline.RenderPipeline`, `GpuBuffer`, `RenderPass`), требуя Java 25+, OpenGL 4.5 Direct State Access (`GL45C`) и профиль GLSL 4.60 Core. Его запуск в среде Minecraft 1.20.1 или 1.21.1 невозможен без полной программной эмуляции всей архитектуры Blaze3D RHI.
- **Кросс-версионная переносимость Nucleus**:
  Nucleus ориентирован на Forge 1.20.1 и NeoForge 1.21.1, абстрагируя различия аппаратных API через платформенный слой `com.hbm_m.platform`. Движок плавно масштабируется от базового профиля OpenGL 3.2 Core (с откатом на клиентские массивы или инстансированные VBO) до OpenGL 4.5 MDI с персистентными отображаемыми буферами, обеспечивая надежную работу на Windows, Linux и macOS (через Metal/MoltenVK).

---

### 7.10 Переоценка стратегического вердикта: портирование движка целиком против заимствования компонентов (переоценка Раздела 5.1)

Меняет ли появление полноценной поддержки шейдерпаков Iris в CrankShaft 1.4.0 стратегический вердикт Раздела 5.1?

**Стратегический вердикт остается неизменным: ПОЛНЫЙ И БЕЗОГОВОРОЧНЫЙ ОТКАЗ от портирования движка целиком.**

#### Техническое обоснование подтверждения отказа
1. **Несовместимость аппаратного интерфейса рендеринга (Blaze3D RHI против Core Profile 1.20.1/1.21.1)**:
   CrankShaft 1.4.0 спроектирован исключительно для платформы Minecraft 26.2 и фундаментально завязан на современные абстракции Vulkan/RHI от Mojang:
   `com.mojang.blaze3d.pipeline.RenderPipeline`, `RenderPass`, `GpuBuffer`, `ColorTargetState` и `DepthStencilState`.
   Этих абстракций физически не существует в Minecraft 1.20.1 (Forge 47.4.20) и 1.21.1 (NeoForge 21.1.248), где рендеринг управляется классическим конечным автоматом OpenGL 3.2 Core (`RenderSystem`, `BufferBuilder`, `ShaderInstance`). Портирование CrankShaft потребовало бы создания и поддержки полноценного эмуляционного слоя RHI 26.2 внутри HBM-Modernized.
2. **Иерархия составных OBJ-мультиблоков и обход диспетчера (Dispatcher Bypass)**:
   Нагрузка рендеринга в HBM-Modernized формируется массивными промышленными установками (усовершенствованные сборщики, химические заводы, центрифуги, ядерные турбины), которые характеризуются:
   - Динамическими древовидными иерархиями составных костей (parts) с вращающимися механическими передачами.
   - Трилинейной интерполяцией объемного освещения по 8 углам габаритного объема (`LightSampleCache`).
   - Монолитной сшивкой геометрии в едином атласе `MdiGeometryAtlas`.
   - Механизмом `NucleusDispatcherBypass`, который полностью исключает блочные сущности мода из почанкового цикла обхода Sodium, экономя **25.4% процессорного времени кадра**.
   CrankShaft не имеет абстракций для составных деревьев OBJ и обхода диспетчера; его движок ориентирован на плоские системы координат блоков и сущностей.
3. **Надежность запекания геометрии в VRAM против хрупкости AST-трансформации**:
   Трансформация AST в CrankShaft опирается на строковые паттерны и допущения о структуре кода через `glsl-transformer`. Хотя это работает на проверенных паках, манипуляции с AST регулярно дают сбои на обфусцированных, макросоемких или нестандартных конструкциях GLSL. Вычислительный запекатель Nucleus (`NucleusGpuBaker`) работает непосредственно с полигональной сеткой в VRAM, передавая полученные вершины в **абсолютно немодифицированную оригинальную программу шейдерпака**. Это обеспечивает 100% визуальную идентичность и нулевой риск ошибок компиляции драйвером.

#### Рекомендуемая дорожная карта заимствования компонентов
Несмотря на невозможность переноса движка целиком, конкретные алгоритмы из CrankShaft 1.4.0 заслуживают интеграции в Nucleus:
- **Заимствование реплицированного теневого отсечения на GPU (`GuestShadowCull`)**:
  В настоящее время Nucleus собирает все активные детали механизмов в буферы теней на CPU. Внедрение структуры UBO на 13 плоскостей и вычислительного шейдера отсечения (`shadow_cull.glsl`) позволит Nucleus отсекать детали мультиблоков, не отбрасывающие видимых теней, прямо на GPU, исключая затраты CPU на сборку теневых пакетов на крупных базах.
- **Заимствование SHA-256 сигнатур исходного кода шейдеров (`ContractPatches`)**:
  Модуль `IrisShadowDistortion` в Nucleus сканирует исходники шейдеров для поиска формул дисторсии. Переход на модель JSON-адаптеров Схемы 2 с валидацией контрольных сумм AST позволит гарантировать стабильность распознавания дисторсий при любых обновлениях шейдерпаков.

---

### 7.11 Первичный указатель цитирования исходного кода подсистемы CrankShaft 1.4.0

Все цитаты приведены относительно репозитория CrankShaft по пути `C:\Projects\CrankShaft` (ветка `26.2`, коммит `635b7b0`):

| Путь к файлу (относительно `C:\Projects\CrankShaft`) | Диапазон строк | Архитектурная область и назначение |
|---|---|---|
| `iris/src/main/java/dev/engine_room/flywheel/iris/IrisBackends.java` | 28–58 | Регистрация бэкендов `iris_instancing` (490) и `iris_indirect` (890 / 1 Intel) |
| `iris/src/main/java/dev/engine_room/flywheel/iris/engine/GuestEngine.java` | 41–65, 99–139 | Подготовка кадра, сдвиг начала отсчета теней, жизненный цикл восстановления SSBO |
| `iris/src/main/java/dev/engine_room/flywheel/iris/engine/GuestIndirectDrawManager.java` | 70–97, 112–155 | Двухфазная отправка на стыке непрозрачности, GPU-диспетчеризация теней, координация OIT |
| `iris/src/main/java/dev/engine_room/flywheel/iris/engine/GuestShadowCull.java` | 36–58, 62–103 | Загрузка в UBO 13 плоскостей усеченного конуса теней, отсекатель дистанции и безопасные зоны |
| `iris/src/main/java/dev/engine_room/flywheel/iris/engine/GuestEntityShadows.java` | 18–28 | Определение подавления стандартных теней сущностей (`textures/misc/shadow.png`) |
| `iris/src/main/java/dev/engine_room/flywheel/iris/engine/GuestVertexExtras.java` | 24–30, 48–97 | 20-байтный расширенный формат вершин Iris (`mc_Entity`, `mc_midTexCoord`, `at_tangent`, `at_midBlock`) |
| `iris/src/main/java/dev/engine_room/flywheel/iris/engine/GuestDrawTags.java` | 30–61 | Сопоставление типов BlockState и Entity с идентификаторами материалов Iris |
| `iris/src/main/java/dev/engine_room/flywheel/iris/compile/GuestPipelines.java` | 88–130, 560–605 | Компиляция гостевых конвейеров, кэширование программ, привязка целей фреймбуфера |
| `iris/src/main/java/dev/engine_room/flywheel/iris/compile/GuestShaders.java` | 78–92, 134–199, 203–247 | Деградация входов AST в `glsl-transformer`, сдвиг привязок SSBO (+8), синтез main() |
| `iris/src/main/java/dev/engine_room/flywheel/iris/compile/ContractProgram.java` | 12–25, 40–68 | Перечисление программ Colorwheel (`clrwl_gbuffers`, `clrwl_shadow`, `clrwl_gbuffers_entities`) |
| `iris/src/main/java/dev/engine_room/flywheel/iris/compile/ContractProperties.java` | 41–100, 108–174 | Парсер `colorwheel.properties`, ранги вейвлетных моментов, пользовательские режимы блендинга |
| `meshlet/src/main/java/me/mlbv/meshlet/mesh/gl/IrisTerrainRasterizer.java` | 37–62, 63–162 | MDI-растеризация ландшафта чанков Sodium, запуск NV mesh shader под Iris |
| `iris/src/main/resources/assets/flywheel/flywheel/iris/shadow_cull.glsl` | 1–87 | Вычислительный шейдер проверки пересечения сфер с 13 плоскостями теней |
| `iris/src/main/resources/assets/flywheel/iris/patches/bsl.json` | 1–30 | Адаптер прямого OIT для BSL с верификацией по контрольной сумме SHA-256 |
| `iris/src/main/resources/assets/flywheel/iris/patches/complementary.json` | 1–31 | Адаптер контракта сущностей для Complementary Reimagined & Unbound |
| `iris/src/main/resources/assets/flywheel/iris/patches/sundial.json` | 1–74 | Адаптер профиля многослойного отложенного OIT для Sundial |

---

### 7.12 Навигационный указатель сносок

Данный указатель разрешает все сноски, расставленные по тексту Разделов с 1 по 6 настоящего документа:

- **`[^iris-update-1]`** (Ссылка из Раздела 1.4 *Высокоуровневая сравнительная диаграмма архитектур*):  
  Уточнено и дополнено в [Разделах 7.1](#71-архитектурный-обзор-и-масштаб-релиза-crankshaft-140--коммит-635b7b0) и [7.2](#72-гостевой-движок-и-конвейер-multi-draw-indirect-guestengine-guestindirectdrawmanager). CrankShaft 1.4.0 внедряет специализированные ветви исполнения `iris_indirect` и `iris_instancing`, которые продолжают активно функционировать при включенных шейдерпаках Iris вместо полного отключения движка.
- **`[^iris-update-2]`** (Ссылка из Раздела 2.10 *Политика отката при наличии шейдерных модов*):  
  Уточнено и дополнено в [Разделах 7.1](#71-архитектурный-обзор-и-масштаб-релиза-crankshaft-140--коммит-635b7b0) и [7.2](#72-гостевой-движок-и-конвейер-multi-draw-indirect-guestengine-guestindirectdrawmanager). CrankShaft 1.4.0 больше не деактивирует движок и не возвращается к ванильной непосредственной отрисовке BER при активации шейдерпаков; рендеринг перенаправляется в `GuestEngine` и `GuestIndirectDrawManager`.
- **`[^iris-update-3]`** (Ссылка из Раздела 4.1 *Сводная сравнительная матрица: Домен 5*):  
  Уточнено и дополнено в [Разделах 7.1](#71-архитектурный-обзор-и-масштаб-релиза-crankshaft-140--коммит-635b7b0), [7.3](#73-компиляция-шейдеров-и-динамическая-ast-трансформация-через-glsl-transformer-guestshaders-guestpipelines) и [7.9](#79-сравнительная-переоценка-бок-о-бок-гостевой-патчинг-crankshaft-140-против-4-уровневого-конвейера-nucleus). CrankShaft 1.4.0 заменяет политику жесткого отказа на динамическую модификацию AST шейдеров через `glsl-transformer`.
- **`[^iris-update-4]`** (Ссылка из Раздела 4.1 *Сводная сравнительная матрица: Домен 5*):  
  Уточнено и дополнено в [Разделе 7.4](#74-архитектура-прохода-теней-и-frustum-culling-на-gpu-guestshadows-guestshadowcull-полупрозрачные-тени). В CrankShaft 1.4.0 добавлена полноценная поддержка теневого прохода, включая отсечение усеченного конуса теней на вычислительных шейдерах GPU (`GuestShadowCull`) и рендеринг полупрозрачных теней.
- **`[^iris-update-5]`** (Ссылка из Раздела 4.6 *Домен 5: Интероперабельность с модами на шейдеры*):  
  Уточнено и дополнено в [Разделах 7.2](#72-гостевой-движок-и-конвейер-multi-draw-indirect-guestengine-guestindirectdrawmanager), [7.3](#73-компиляция-шейдеров-и-динамическая-ast-трансформация-через-glsl-transformer-guestshaders-guestpipelines) и [7.6](#76-контракт-шейдерпаков-colorwheel-против-встроенных-адаптеров-contractproperties-contractprogram-встроенные-адаптеры). CrankShaft 1.4.0 адаптируется к конвейерам шейдерпаков с помощью контракта Colorwheel (программы `clrwl_`) и набора встроенных JSON-адаптеров (`bsl.json`, `complementary.json` и др.).
- **`[^iris-update-6]`** (Ссылка из Раздела 5.1 *Стратегический вердикт: обоснование отказа от портирования движка целиком*):  
  Переоценено в [Разделе 7.10](#710-переоценка-стратегического-вердикта-портирование-движка-целиком-против-заимствования-компонентов-переоценка-раздела-51). Несмотря на появление поддержки шейдерпаков Iris в CrankShaft 1.4.0, портирование движка целиком остается невозможным из-за несовместимости архитектуры Blaze3D RHI, отсутствия поддержки атласа составных OBJ-мультиблоков и механизма обхода диспетчера.
- **`[^iris-update-7]`** (Ссылка из Приложения A *Указатель репозитория CrankShaft 26.2: Backends.java*):  
  Уточнено и дополнено в [Разделах 7.1](#71-архитектурный-обзор-и-масштаб-релиза-crankshaft-140--коммит-635b7b0) и [7.11](#711-первичный-указатель-цитирования-исходного-кода-подсистемы-crankshaft-140). Класс `IrisBackends.java` регистрирует бэкенды `iris_indirect` и `iris_instancing`, имеющие безусловный приоритет над стандартными бэкендами при истинном значении `isPackInUse()`.
- **`[^iris-update-8]`** (Ссылка из Приложения A *Указатель репозитория CrankShaft 26.2: ShadersModHelper.java*):  
  Уточнено и дополнено в [Разделах 7.1](#71-архитектурный-обзор-и-масштаб-релиза-crankshaft-140--коммит-635b7b0) и [7.11](#711-первичный-указатель-цитирования-исходного-кода-подсистемы-crankshaft-140). Запросы к методу `IrisApi.getInstance().isShaderPackInUse()` теперь инициируют запуск `GuestEngine` вместо принудительного перехода на `OFF_BACKEND`.

[^iris-update-1]: Уточнено и дополнено в [Разделах 7.1](#71-архитектурный-обзор-и-масштаб-релиза-crankshaft-140--коммит-635b7b0) и [7.2](#72-гостевой-движок-и-конвейер-multi-draw-indirect-guestengine-guestindirectdrawmanager). CrankShaft 1.4.0 внедряет специализированные ветви исполнения `iris_indirect` и `iris_instancing`, которые продолжают активно функционировать при включенных шейдерпаках Iris вместо полного отключения движка.
[^iris-update-2]: Уточнено и дополнено в [Разделах 7.1](#71-архитектурный-обзор-и-масштаб-релиза-crankshaft-140--коммит-635b7b0) и [7.2](#72-гостевой-движок-и-конвейер-multi-draw-indirect-guestengine-guestindirectdrawmanager). CrankShaft 1.4.0 больше не деактивирует движок и не возвращается к ванильной непосредственной отрисовке BER при активации шейдерпаков; рендеринг перенаправляется в `GuestEngine` и `GuestIndirectDrawManager`.
[^iris-update-3]: Уточнено и дополнено в [Разделах 7.1](#71-архитектурный-обзор-и-масштаб-релиза-crankshaft-140--коммит-635b7b0), [7.3](#73-компиляция-шейдеров-и-динамическая-ast-трансформация-через-glsl-transformer-guestshaders-guestpipelines) и [7.9](#79-сравнительная-переоценка-бок-о-бок-гостевой-патчинг-crankshaft-140-против-4-уровневого-конвейера-nucleus). CrankShaft 1.4.0 заменяет политику жесткого отказа на динамическую модификацию AST шейдеров через `glsl-transformer`.
[^iris-update-4]: Уточнено и дополнено в [Разделе 7.4](#74-архитектура-прохода-теней-и-frustum-culling-на-gpu-guestshadows-guestshadowcull-полупрозрачные-тени). В CrankShaft 1.4.0 добавлена полноценная поддержка теневого прохода, включая отсечение усеченного конуса теней на вычислительных шейдерах GPU (`GuestShadowCull`) и рендеринг полупрозрачных теней.
[^iris-update-5]: Уточнено и дополнено в [Разделах 7.2](#72-гостевой-движок-и-конвейер-multi-draw-indirect-guestengine-guestindirectdrawmanager), [7.3](#73-компиляция-шейдеров-и-динамическая-ast-трансформация-через-glsl-transformer-guestshaders-guestpipelines) и [7.6](#76-контракт-шейдерпаков-colorwheel-против-встроенных-адаптеров-contractproperties-contractprogram-встроенные-адаптеры). CrankShaft 1.4.0 адаптируется к конвейерам шейдерпаков с помощью контракта Colorwheel (программы `clrwl_`) и набора встроенных JSON-адаптеров (`bsl.json`, `complementary.json` и др.).
[^iris-update-6]: Переоценено в [Разделе 7.10](#710-переоценка-стратегического-вердикта-портирование-движка-целиком-против-заимствования-компонентов-переоценка-раздела-51). Несмотря на появление поддержки шейдерпаков Iris в CrankShaft 1.4.0, портирование движка целиком остается невозможным из-за несовместимости архитектуры Blaze3D RHI, отсутствия поддержки атласа составных OBJ-мультиблоков и механизма обхода диспетчера.
[^iris-update-7]: Уточнено и дополнено в [Разделах 7.1](#71-архитектурный-обзор-и-масштаб-релиза-crankshaft-140--коммит-635b7b0) и [7.11](#711-первичный-указатель-цитирования-исходного-кода-подсистемы-crankshaft-140). Класс `IrisBackends.java` регистрирует бэкенды `iris_indirect` и `iris_instancing`, имеющие безусловный приоритет над стандартными бэкендами при истинном значении `isPackInUse()`.
[^iris-update-8]: Уточнено и дополнено в [Разделах 7.1](#71-архитектурный-обзор-и-масштаб-релиза-crankshaft-140--коммит-635b7b0) и [7.11](#711-первичный-указатель-цитирования-исходного-кода-подсистемы-crankshaft-140). Запросы к методу `IrisApi.getInstance().isShaderPackInUse()` теперь инициируют запуск `GuestEngine` вместо принудительного перехода на `OFF_BACKEND`.

---
*Конец сравнительного исследования архитектур и дорожной карты модернизации (обновлено с учетом CrankShaft 1.4.0).*
