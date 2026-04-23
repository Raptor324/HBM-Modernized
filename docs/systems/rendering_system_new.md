# Система рендеринга OBJ-моделей в HBM Modernized

> Полный технический анализ всех путей рендеринга, шейдерной инфраструктуры и совместимости с Iris/Oculus/Sodium/Embeddium.
>
> *Версия документа: 2026-04-19 (актуально для последнего commit'а)*

---

## Содержание

1. [Концептуальный обзор](#1-концептуальный-обзор)
2. [Регистрация и инициализация (ClientSetup)](#2-регистрация-и-инициализация-clientsetup)
3. [Три пути рендеринга (Decision Matrix)](#3-три-пути-рендеринга-decision-matrix)
4. [GPU-инфраструктура: AbstractGpuMesh, VBO/EBO/VAO](#4-gpu-инфраструктура-abstractgpumesh-vboeboVAO)
5. [SingleMeshVboRenderer — базовый рендер одной сетки](#5-singlemeshvborenderer--базовый-рендер-одной-сетки)
6. [InstancedStaticPartRenderer — батчинг через инстансинг](#6-instancedstaticpartrenderer--батчинг-через-инстансинг)
7. [GlobalMeshCache, PartGeometry, ObjModelVboBuilder](#7-globalmeshcache-partgeometry-objmodelvbobuilder)
8. [Кастомные GLSL-шейдеры (block_lit)](#8-кастомные-glsl-шейдеры-block_lit)
9. [Регистрация шейдеров и preprocessor-инжекция](#9-регистрация-шейдеров-и-preprocessor-инжекция)
10. [Совместимость с Iris/Oculus](#10-совместимость-с-irisoculus)
11. [BakedModel-инфраструктура](#11-bakedmodel-инфраструктура)
12. [BlockEntityRenderer'ы машин](#12-blockentityrenderery-машин)
13. [Дверная подсистема](#13-дверная-подсистема)
14. [Вспомогательные сервисы](#14-вспомогательные-сервисы)
15. [Жизненный цикл кадра и события](#15-жизненный-цикл-кадра-и-события)
16. [Перезагрузка ресурсов и cleanup](#16-перезагрузка-ресурсов-и-cleanup)
17. [Power Armor (OBJ-броня) — отдельный pipeline](#17-power-armor-obj-броня--отдельный-pipeline)
18. [Производительность: профайлерные оптимизации](#18-производительность-профайлерные-оптимизации)
19. [Известные проблемы и обходные пути](#19-известные-проблемы-и-обходные-пути)

---

## 1. Концептуальный обзор

Система рендеринга машин в HBM Modernized — это **гибридный pipeline**, балансирующий между тремя принципиально разными подходами:

1. *Vanilla VBO + кастомный шейдер `block_lit_instanced`* — путь по умолчанию, когда внешний шейдер-пак НЕ активен. Все статические части машин накапливаются в один батч и рисуются одним `glDrawElementsInstanced` на тип детали. `**BakedModel.getQuads(...)` для блок-state'а возвращает пустой список** — chunk mesh не получает геометрию, всё рисует BER.
2. **Iris ExtendedShader + companion-mesh** — путь под шейдер-паками (BSL, Complementary, Photon, Solas, RV и т.д.) при включённом `ModClothConfig.useIrisExtendedShaderPath = true` (значение по умолчанию). Используется отдельная компаньонная VAO/VBO в формате `IrisVertexFormats.ENTITY` (15-attribute layout с `iris_Entity`, `mc_midTexCoord`, `at_tangent` и т.д.), а рендер идёт через `ShaderInstance`, который Iris подменил на `ExtendedShader` через ShaderMap. Так же как в пути 1, `**BakedModel.getQuads(...)` возвращает пустой список** — вся геометрия идёт через BER.
3. **Legacy путь "baked + putBulkData"** — fallback под шейдер-паками, когда `ModClothConfig.useIrisExtendedShaderPath = false`. **Только в этом режиме** статическая геометрия (Base, Frame, Body) запекается в chunk mesh Sodium/Embeddium через обычный `BakedModel.getQuads(...)`. Анимированные части в режиме idle (`render_active = false`) тоже идут в chunk mesh в дефолтных позах — нулевая нагрузка на CPU при рендере. BER рендерит только активные подвижные части (`render_active = true`), эмитя их через `MultiBufferSource.putBulkData(...)` в обычный буфер Iris'а — тот сам всё интегрирует в свой G-buffer. Это совместимый, но визуально менее богатый путь (нет правильного нормал-mapping'а для активных частей, простое освещение).

### Исключения из общей системы

Несколько машин/блоков НЕ имеют переключения по `useVboGeometry()` и работают всегда одинаково независимо от шейдеров и конфига:

- `**MachineFluidTankBakedModel`** — **полностью статическая структура, не имеет BER**. Геометрия (Frame + Tank) **ВСЕГДА** запекается в chunk mesh через `BakedModel.getQuads(...)` с применением `ModelData`. Текстура жидкости передаётся через Forge `ModelProperty<ResourceLocation>` (`FLUID_TEXTURE_PROPERTY`). При смене жидкости в инвентаре `BlockEntity` вызывает `requestModelDataUpdate()` + `level.sendBlockUpdated(..., flag 8)`, и Sodium/Embeddium перезапекает chunk с новой текстурой через `retextureAndFixUV(...)`. **У этого блока в принципе нет VBO/instancing/Iris пути** — производительность достигается чисто статической природой и стандартной chunk-render системой Sodium.
- `**PressBakedModel`** — **Base всегда** запекается в chunk mesh независимо от `useVboGeometry()`; BER рендерит только Head с анимацией движения вниз. Гибрид: статическая часть всегда через baked, анимированная всегда через BER, без переключения.
- `**MachineHydraulicFrackiningTowerBakedModel`** — наоборот, **никогда** не запекается в chunk mesh для блок-state'а (24 блока в высоту → переполнение 16-bit vertex coordinates Sodium'а). BER рендерит ВСЁ независимо от наличия шейдера или режима.

**Решение, какой путь выбрать** для основных многокомпонентных машин (Advanced Assembler, Assembler, Chemical Plant, Door), принимается на лету в `ShaderCompatibilityDetector` — единая точка истины с детектом наличия Iris/Oculus, состояния shadow-pass'а, доступности reflection-API и пользовательских настроек (`ModClothConfig.useIrisExtendedShaderPath`, `useInstancedStaticRendering`).

### Высокоуровневая схема

```
RegisterShadersEvent → ClientSetup.onRegisterShaders
   ↓
   ├─ ModShaders.setBlockLitSimpleShader (Position/Normal/UV0)
   └─ ModShaders.setBlockLitInstancedShader (с InstPos/InstRot/InstBrightness, USE_INSTANCING define)

FMLClientSetupEvent → BlockEntityRenderers.register(...)
   ↓
   ChunkSection rebuild → BakedModel.getQuads(state, side, rand, modelData, type):
     ├─ FluidTank: ВСЕГДА Frame+Tank с ModelData (нет BER)
     ├─ Press: ВСЕГДА Base (Head через BER)
     ├─ HydraulicTower: ВСЕГДА empty (всё через BER)
     └─ Остальные машины (AdvAssembler, Assembler, ChemPlant, Door):
         ├─ useVboGeometry()=true  → empty list (BER рендерит всё через VBO)
         └─ useVboGeometry()=false → Base/Frame + idle animated parts (legacy путь)
   ↓
   Каждый кадр:
     → BlockEntityRendererDispatcher → AbstractPartBasedRenderer.render
        → unwrap FRAPI → setupBlockTransform → renderParts(...)
           → OcclusionCullingHelper.shouldRender → ?
           → if (useVboGeometry) → InstancedStaticPartRenderer / SingleMeshVboRenderer
              → if (Iris active && useIrisExtendedShaderPath) → IrisCompanionMesh + ExtendedShader
              → else → ModShaders.blockLitInstanced (vanilla VBO путь)
           → else → putBulkData() через MultiBufferSource (только активные части)

RenderLevelStageEvent.AFTER_BLOCK_ENTITIES (main pass):
   → ClientModEvents.onRenderLevelStage
     → flushInstancedBatches() для каждого BER-типа (drawElementsInstanced)
     → IrisExtendedShaderAccess.tickPass()  (инвалидация кэша shader lookup)
     → LightSampleCache.onFrameStart()      (инвалидация кэша освещения)

RenderLevelStageEvent.AFTER_LEVEL:
   → IrisRenderBatch.closePersistentIfActive()  (страховка при пустом main pass)
```

---

## 2. Регистрация и инициализация (ClientSetup)

Файл: `com.hbm_m.client.ClientSetup`

### 2.1 Регистрация BER

В обработчике `FMLClientSetupEvent` через `event.enqueueWork(...)` (для безопасного выполнения на клиентском потоке):

```208:213:src/main/java/com/hbm_m/client/ClientSetup.java
BlockEntityRenderers.register(ModBlockEntities.ADVANCED_ASSEMBLY_MACHINE_BE.get(), MachineAdvancedAssemblerRenderer::new);
BlockEntityRenderers.register(ModBlockEntities.MACHINE_ASSEMBLER_BE.get(), MachineAssemblerRenderer::new);
BlockEntityRenderers.register(ModBlockEntities.DOOR_ENTITY.get(), DoorRenderer::new);
BlockEntityRenderers.register(ModBlockEntities.PRESS_BE.get(), MachinePressRenderer::new);
BlockEntityRenderers.register(ModBlockEntities.CHEMICAL_PLANT_BE.get(), ChemicalPlantRenderer::new);
BlockEntityRenderers.register(ModBlockEntities.HYDRAULIC_FRACKINING_TOWER_BE.get(), MachineHydraulicFrackiningTowerRenderer::new);
```

Регистрируются **6 BER-классов** (Advanced Assembler, Assembler, Door, Press, Chemical Plant, Hydraulic Fracking Tower), использующих общую базу `AbstractPartBasedRenderer<TBE, TModel>`.

`**MachineFluidTankBlockEntity` принципиально НЕ имеет BER** — он рендерится исключительно через свою `BakedModel` с динамической текстурой через `ModelData` (см. §11.3.7). Это единственный машинный блок в моде без `BlockEntityRenderer`.

### 2.2 Геометрические лоадеры (custom BakedModel)

В `ModelEvent.RegisterGeometryLoaders` регистрируются кастомные `IGeometryLoader`'ы для каждой машины:

```360:368:src/main/java/com/hbm_m/client/ClientSetup.java
event.register("procedural_wire", new ProceduralWireLoader());
event.register("advanced_assembly_machine_loader", new MachineAdvancedAssemblerModelLoader());
event.register("chemical_plant_loader", new ChemicalPlantModelLoader());
event.register("machine_assembler_loader", new MachineAssemblerModelLoader());
event.register("hydraulic_frackining_tower_loader", new MachineHydraulicFrackiningTowerModelLoader());
event.register("fluid_tank_loader", new MachineFluidTankModelLoader());
event.register("door", new DoorModelLoader());
event.register("template_loader", new TemplateModelLoader());
event.register("press_loader", new PressModelLoader());
```

Эти лоадеры читают JSON-конфиги моделей и собирают `Map<String, BakedModel>` для частей (Base, Frame, ArmLower1, ...), оборачивая всё в подкласс `AbstractMultipartBakedModel`.

### 2.3 Continuity FRAPI unwrap

Critical: при наличии **Continuity** (через Connector/FFAPI) все blockstate-модели оборачиваются в `CtmBakedModel extends ForwardingBakedModel`. Это ломает skin-switching у дверей и transform'ы JSON-моделей.

Решение — в `EventPriority.LOWEST` (после Continuity) обходим все модели нашего mod'а и **разворачиваем форвардинг-обёртки**:

```267:296:src/main/java/com/hbm_m/client/ClientSetup.java
@SubscribeEvent(priority = EventPriority.LOWEST)
public static void onModelBakeUnwrapContinuity(ModelEvent.ModifyBakingResult event) {
    ...
    BakedModel unwrapped = com.hbm_m.client.render.AbstractPartBasedRenderer
            .unwrapFabricForwardingModels(original);
    if (unwrapped != original) {
        replacements.put(entry.getKey(), unwrapped);
    }
    ...
}
```

### 2.4 ClientReloadListenerEvent

При перезагрузке ресурсов (F3+T, смена ресурс-пака, смена шейдер-пака) регистрируется reload-listener, который через `RenderSystem.recordRenderCall(...)` (на render thread) очищает все VBO-кэши:

```405:432:src/main/java/com/hbm_m/client/ClientSetup.java
event.registerReloadListener(...) {
    return preparationBarrier.wait(null).thenRunAsync(() -> {
        // КРИТИЧНО: на render thread, иначе EXCEPTION_ACCESS_VIOLATION
        com.mojang.blaze3d.systems.RenderSystem.recordRenderCall(() -> {
            MachineAdvancedAssemblerRenderer.clearCaches();
            MachineAssemblerRenderer.clearCaches();
            MachineHydraulicFrackiningTowerRenderer.clearCaches();
            DoorRenderer.clearAllCaches();
            MachinePressRenderer.clearCaches();
            ChemicalPlantRenderer.clearCaches();
            GlobalMeshCache.clearAll();
            AbstractObjArmorLayer.clearAllCaches();
        });
    }, gameExecutor);
}
```

Дополнительно регистрируется специальный `ShaderReloadListener` (см. §16) для invalidation Iris-кэшей.

### 2.5 onClientDisconnect

При отключении от сервера (`ClientPlayerNetworkEvent.LoggingOut`) тот же набор `clearCaches()` вызывается, чтобы не таскать VBO между мирами.

---

## 3. Три пути рендеринга (Decision Matrix)

Решение принимается через `**ShaderCompatibilityDetector**` — единая точка истины. Все методы:


| Флаг                         | Реальное определение                                                                                              |
| ---------------------------- | ----------------------------------------------------------------------------------------------------------------- |
| `isExternalShaderActive()`   | Iris/Oculus загружен И в данный момент рендерит мир через свой pipeline                                           |
| `canUseIrisExtendedShader()` | `isExternalShaderActive() && IrisExtendedShaderAccess.isReflectionAvailable()`                                    |
| `useVboGeometry()`           | `**!isExternalShaderActive()                                                                                      |
| `useNewIrisVboPath()`        | `isExternalShaderActive() && ModClothConfig.useIrisExtendedShaderPath()` — сахар «под шейдерами через новый путь» |
| `isRenderingShadowPass()`    | Iris сейчас в shadow pass (рефлексия `ShadowRenderingState`)                                                      |


**Конфиг `ModClothConfig.useIrisExtendedShaderPath` по умолчанию = `true`**, то есть в стандартной поставке третий путь (legacy baked + putBulkData) активируется только если пользователь явно его включил в настройках.

### Раскрытая истинностная таблица


| `isExternalShaderActive` | `useIrisExtendedShaderPath` | `useVboGeometry()` | Что делает BakedModel.getQuads | Что рендерит BER                                    |
| ------------------------ | --------------------------- | ------------------ | ------------------------------ | --------------------------------------------------- |
| `false` (нет шейдера)    | n/a                         | `**true**`         | empty list                     | ВСЁ (Base + Frame + animated через VBO/instancing)  |
| `true` (шейдер активен)  | `true` (по умолчанию)       | `**true**`         | empty list                     | ВСЁ через Iris ExtendedShader path (companion mesh) |
| `true` (шейдер активен)  | `false` (legacy)            | `**false**`        | Base + Frame + idle animated   | Только active animated через putBulkData            |


### Decision tree (на каждый кадр, на каждый BE главных машин)

```
                     ┌──────────────────────────────────────┐
                     │ ShaderCompatibilityDetector           │
                     │   useVboGeometry() ?                  │
                     └─────────────┬────────────────────────┘
                                   │
        FALSE — только под шейдером + useIrisExtendedShaderPath=OFF
                                   │
                                   ▼                       TRUE (нет шейдера ИЛИ useIrisExtendedShaderPath=ON)
                ┌──────────────────┴────────────────────────────────────┐
                │                                                       │
                ▼                                                       ▼
   ┌─────────────────────────────────┐         ┌──────────────────────────────────────┐
   │ Legacy baked + putBulkData      │         │ VBO путь — BakedModel пуст для блока│
   │                                  │         │  - InstancedStaticPartRenderer      │
   │ BakedModel.getQuads возвращает  │         │  - SingleMeshVboRenderer            │
   │  Base/Frame + idle animated     │         │                                      │
   │  → попадают в chunk mesh.       │         │  isExternalShaderActive() ?          │
   │                                  │         │    ┌───────────────────────────┐   │
   │ BER эмитит только активные      │         │    │ TRUE: Iris extended path  │   │
   │ части через                     │         │    │  (IrisCompanionMesh +     │   │
   │  MultiBufferSource              │         │    │   ExtendedShader из       │   │
   │  .getBuffer(RenderType.solid)   │         │    │   ShaderMap)              │   │
   │  .putBulkData(...)              │         │    │  + IrisRenderBatch        │   │
   │                                  │         │    │  + IrisCompanionMesh      │   │
   │ Iris сам интегрирует            │         │    └───────────────────────────┘   │
   │ результат в свой G-buffer.      │         │    ┌───────────────────────────┐   │
   │                                  │         │    │ FALSE: vanilla VBO        │   │
   │ Анимированные части в idle —    │         │    │  ModShaders               │   │
   │ сразу в baked в дефолтных       │         │    │   .blockLitInstanced      │   │
   │ позах.                          │         │    │  glDrawElementsInstanced  │   │
   └─────────────────────────────────┘         │    └───────────────────────────┘   │
                                                └──────────────────────────────────────┘
```

### Машины со статической логикой (не следуют дереву)

- `**MachineFluidTankBakedModel` (Fluid Tank)** — НЕ имеет BER. **ВСЕГДА** запекается в chunk mesh (Frame + Tank). `getQuads(...)` всегда возвращает реальные квады с retexturing'ом по `ModelData`. Под шейдерами всё интегрируется как обычная chunk-геометрия. Никаких VBO/Iris путей у этого блока нет.
- `**PressBakedModel` (Press)** — `shouldSkipWorldRendering = false` всегда; `getQuads(...)` всегда отдаёт **Base** в chunk mesh. BER `MachinePressRenderer` рендерит **только Head** (через VBO/instancing/Iris по основному дереву).
- `**MachineHydraulicFrackiningTowerBakedModel` (Hydraulic Tower)** — `shouldSkipWorldRendering = true` всегда для блок-state'а; `getQuads(...)` для блока всегда пустой. BER `MachineHydraulicFrackiningTowerRenderer` рендерит **всю** геометрию по основному дереву (без legacy fallback'а).

### Важные нюансы

- `**isRenderingShadowPass = true`** автоматически переключает все instanced flush'и в режим **per-instance draw** через `drawSingleWithIrisExtended`, потому что `RenderLevelStageEvent.AFTER_BLOCK_ENTITIES` НЕ срабатывает в shadow pass. Без этого: либо машины не отбрасывают тени, либо «призраки в небе» (геометрия с координатами shadow-камеры рисуется в main pass).
- **Renderer'ы открывают `IrisRenderBatch`** для амортизации `shader.apply()/clear()` (тяжёлые вызовы под Iris) когда либо инстансинг выключен, либо мы в shadow pass.
- **Hydraulic Fracking Tower** не имеет вообще никакого baked-пути, потому что Sodium не справляется с 24-блочной моделью (16-bit vertex coordinates переполняются).
- **Fluid Tank** наоборот не имеет VBO-пути — он простой статический блок и хорошо работает через стандартную chunk-render систему. Реактивность достигается через `requestModelDataUpdate()` + chunk re-bake.

---

## 4. GPU-инфраструктура: AbstractGpuMesh, VBO/EBO/VAO

Файл: `com.hbm_m.client.render.AbstractGpuMesh`

Базовый класс для всего, что хранит геометрию на GPU. Содержит четыре GL-handle:

```java
protected int vaoId = -1;   // Vertex Array Object - конфигурация атрибутов
protected int vboId = -1;   // Vertex Buffer Object - сами вершины
protected int eboId = -1;   // Element Buffer Object - индексы (uint)
protected int indexCount = 0;
protected boolean initialized = false;
```

Ключевая ответственность — **чистый cleanup на render thread**:

- `cleanup()` записывает удаление через `RenderSystem.recordRenderCall(() -> glDeleteBuffers/glDeleteVertexArrays)`. Это критически важно: попытка удалить GL-объект с не-render потока ведёт к undefined behavior.
- Для очистки нативной памяти (`MemoryUtil.memAlloc`*) используется `java.lang.ref.Cleaner` с регистрацией lambda, освобождающей через `MemoryUtil.nmemFree(addr)` (а не `memFree(buffer)`, чтобы избежать гонки с GC, обнуляющим reference на буфер).

Подклассы:

- `SingleMeshVboRenderer` (один меш, один draw call)
- `InstancedStaticPartRenderer` (один меш + instance VBO, instanced rendering)
- `IrisCompanionMesh` (компаньон в формате IrisVertexFormats.ENTITY, не наследник, но идейно похож)

---

## 5. SingleMeshVboRenderer — базовый рендер одной сетки

Файл: `com.hbm_m.client.render.SingleMeshVboRenderer`

Абстрактный класс, унаследованный от `AbstractGpuMesh`. Используется для **частей машин, которые анимируются, но не дублируются массово** (например, ArmLower у ассемблера; Slider/Spinner у chemical plant'а).

### 5.1 VboData (DTO)

```java
public static final class VboData implements AutoCloseable {
    public final ByteBuffer byteBuffer;   // вершины: x,y,z (12), nx,ny,nz (12), u,v (8) = 32 байт/вершина
    public final IntBuffer indices;       // uint индексы
    public final int vertexCount;
    public final int indexCount;
}
```

Закрывается через `MemoryUtil.memFree(...)` после загрузки на GPU.

### 5.2 Жизненный цикл

1. `buildVboData()` — абстрактный метод, подкласс собирает данные (обычно делегирует в `ObjModelVboBuilder`).
2. `init()` — генерирует VAO/VBO/EBO, конфигурирует attrib pointers (location 0/1/2 для Position/Normal/UV0), загружает данные. Сохраняет/восстанавливает `previousVao`/`previousArrayBuffer` чтобы не сломать внешнее состояние GL.
3. `render(...)` — три подпути:
  - **Vanilla VBO path** (`!isExternalShaderActive`) — устанавливает `ModShaders.blockLitSimpleShader`, грузит uniforms (`ProjMat`, `ModelViewMat`, `Brightness`, `FogStart/End/Color`), биндит block atlas, делает `glDrawElements`.
  - **Iris extended path** (`canUseIrisExtendedShader`) — делегирует в companion mesh через `IrisExtendedShaderAccess.getBlockShader(shadowPass)` + `IrisCompanionMesh.prepareForShader()` + `glDrawElements`. Использует `IrisPhaseGuard.pushBlockEntities()` для установки `WorldRenderingPhase.BLOCK_ENTITIES`.
  - **Fallback putBulkData** — если ни VBO, ни Iris path не сработали, эмитит квады из `getQuadsForIrisPath()` через `MultiBufferSource.getBuffer(RenderType.solid())`.

### 5.3 TextureBinder

Внутренний помощник, который через `RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS)` гарантирует, что атлас блоков биндится перед `shader.apply()`. Без этого Iris при `ExtendedShader.apply()` может прочитать из `RenderSystem.getShaderTexture(0)` мусор от предыдущего рендера (например, lightmap), и модель станет «оранжевой» (sampling из lightmap-текстуры).

### 5.4 Кэширование

`SingleMeshVboRenderer` сам не кэширует ничего — за это отвечает `GlobalMeshCache.getOrCreateRenderer(...)`, который держит `Map<String, SingleMeshVboRenderer>` и переиспользует одну и ту же сетку для всех машин одного типа (Body, Frame и т.д.).

---

## 6. InstancedStaticPartRenderer — батчинг через инстансинг

Файл: `com.hbm_m.client.render.InstancedStaticPartRenderer`

**Самый важный класс системы для производительности.** Накапливает per-instance data (position, rotation, brightness, smoothed-lightmap UV) для всех машин одного типа и рисует их одним `glDrawElementsInstanced`.

### 6.1 Layout

```
Static (location 0..2):       Position(3) + Normal(3) + UV0(2)        = 32 байт/вершина
Per-instance (location 3..5): InstPos(3) + InstRot(4 quaternion) + InstBrightness(1) = 32 байт/инстанс
```

`glVertexAttribDivisor(loc, 1)` для атрибутов 3..5 — обновлять раз в инстанс.

`MAX_INSTANCES = 1024`. При переполнении логируется warning и оставшиеся инстансы пропускаются до следующего flush'а.

### 6.2 Накопление инстансов: addInstance

```java
public void addInstance(PoseStack poseStack, int packedLight, BlockPos blockPos,
                        BlockEntity blockEntity, MultiBufferSource bufferSource);
```

Логика:

1. **Shadow pass detection**: если `isRenderingShadowPass()` — НЕ накапливать в общий буфер, а сразу рисовать через `drawSingleWithIrisExtended`. Причина — flush event срабатывает только в main pass; накопленные shadow-pass инстансы потом нарисовались бы main-pass-проекцией = «призраки в небе».
2. **Извлечение transform'а**: `poseStack.last().pose()` → `getTranslation(posTmp)` + `getNormalizedRotation(rotTmp)` (кватернион).
3. **Сэмплирование освещения** через `LightSampleCache.getOrSample(blockEntity, packedLight, instanceLightUV, sampleBase)`. Это **критично**: без кэша 11 деталей одного assembler'а делали бы 11 одинаковых сэмплов по 6 face-центрам — прямой 17% frame time по профайлеру.
4. **Кэш `batchSkyDarken`**: значение `level.getSkyDarken(1.0f)` сэмплируется один раз на батч (на первом `instanceCount == 0`).
5. **Конвертация UV → brightness** через `brightnessFromUV(blockU, skyV, batchSkyDarken)` — даёт sub-integer smoothing для vanilla path'а тоже.

### 6.3 Flush — vanilla path (`flushBatchVanilla`)

Под обычным Minecraft (без Iris):

- Устанавливает `ModShaders.blockLitInstancedShader`.
- Загружает identity ModelView (потому что инстанс-шейдер сам собирает MV из Position/Quat per-instance).
- Биндит texture atlas, делает `shader.apply()`.
- `glDrawElementsInstanced(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0, instanceCount)`.

Сохранение/восстановление GL-state (cull face, depth test/mask/func, current VAO/VBO) для безопасности.

### 6.4 Flush — Iris path (`flushBatchIris`)

**Это самый сложный код в системе.** Разберём по шагам.

**Подготовка:**

1. `getOrBuildIrisCompanion()` — лениво строит `IrisCompanionMesh` в формате `IrisVertexFormats.ENTITY` из `quadsForIris` (передан в конструктор).
2. `IrisExtendedShaderAccess.getBlockShader(shadowPass)` — рефлексивно достаёт ExtendedShader для main или shadow pass'а из ShaderMap.
3. Сохранение GL state (VAO, ARRAY_BUFFER, cull, depth, etc.).
4. `IrisExtendedShaderAccess.setCurrentRenderedBlockEntity(0)` — устанавливает «нейтральный» blockEntityId = 0. Без этого pack-шейдеры (особенно BSL) читают `blockEntityId / 100` и попадают в спец-ветки: 155 → EMISSIVE_RECOLOR (солидно-красная текстура), 252 → DrawEndPortal. Iris обновляет этот uniform от последнего рендеренного BE — наш raw-GL батч получил бы случайный leftover от чужого рендера.
5. `IrisPhaseGuard.pushBlockEntities()` — рефлексивно ставит `WorldRenderingPhase.BLOCK_ENTITIES`.

**Apply (один раз на батч):**
6. `RenderSystem.setShader(() -> shader)` + `applyCommonUniforms(shader, projection, IDENTITY)` — заливает projection-матрицу, fog, sampler.
7. `RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS)` + `lightTexture.turnOnLightLayer()` + `overlayTexture.setupOverlayColor()` — гарантирует, что все 3 sampler-слота указывают на правильные текстуры.
8. `**shader.apply()`** — самый дорогой вызов: внутри `ExtendedShader.apply()` происходит `GlFramebuffer.bind()`, `Iris CustomUniforms.push()`, `uploadIfNotNull` для каждого uniform. По профайлеру — 63% frame time на больших количествах машин в кадре. Поэтому он вынесен **из per-instance loop'а**.

**Per-instance loop:**
9. `companion.prepareForShader(shader.getId())` — биндит iris_Entity / mc_midTexCoord / at_tangent на их linker-resolved locations.

1. Для каждого инстанса:
  - Извлекает position и quaternion из `instanceBuffer`.
    - Собирает `tmpInstanceMat = translationRotate(p, q)`.
    - **Прямой `GL20.glUniformMatrix4fv(locModelView, false, mvFloats)`** — обходит Mojang'овский `Uniform.upload()` proxy stack (на 5% быстрее).
    - **Только если rotation ИЛИ position изменилось** — пересчитывает `iris_ModelViewMatInverse` (вызов `.invert()`).
    - **Только если rotation изменилось** — пересчитывает `iris_NormalMat` (transpose(inverse(MV)) для 3×3, переиспользуя уже вычисленный inverse).
    - `glVertexAttribI2i(uv2Loc, blockU, skyV)` — устанавливает per-draw lightmap UV2 как «current value» для disabled vertex attribute (целочисленный ipipeline, важно для ivec2 в pack-шейдерах).
    - `glDrawElements(GL_TRIANGLES, ...)` — рисует один инстанс.
2. `shader.clear()` — один раз в конце.

**Cleanup:**
12. Восстановление всех GL state'ов в `finally`.
13. Восстановление `RenderSystem.setShaderTexture(0, BLOCKS)` — чтобы наша возможная пыль от texture pollution не утекла дальше.
14. Восстановление `blockEntityId` через `IrisExtendedShaderAccess.restoreCurrentRenderedBlockEntity(prev)`.

### 6.5 Кэши локаций uniform'ов

`cachedLocModelViewInverse`, `cachedLocNormalMat`, `irisLocationsResolved` — `glGetUniformLocation` это синхронный driver round-trip. Кэшируется до смены `cachedShader`.

### 6.6 Микро-оптимизации в loop'е

Sentinel-NaN trick:

```java
float lastQx = Float.NaN, lastQy = Float.NaN, ...;
boolean rotChanged = qx != lastQx || ...;  // NaN != x всегда true для первой итерации
```

Без отдельного boolean `firstIteration` — экономия одной branch'и на 1024 итерации.

### 6.7 renderSingle и drawSingleWithIrisExtended

`renderSingle` — отдельный путь когда вызывают БЕЗ накопления (например, frame-rendering у дверей): подготавливает один инстанс в `instanceBuffer[0]` и делает `glDrawElementsInstanced(..., 1)`.

`drawSingleWithIrisExtended` — один проход через Iris ExtendedShader для одной машины. Важная оптимизация — если есть активный `IrisRenderBatch`, переиспользует его apply()/clear() через `batch.drawCompanion(companion, mat, packedSmoothLight)`.

### 6.8 Cleanup

`cleanup()` корректно удаляет:

- VAO/VBO/EBO через `super.cleanup()`.
- Instance VBO (`glDeleteBuffers(instanceVboId)`).
- Native память instance buffer'а через `Cleaner.Cleanable.clean()`.
- Companion mesh через `companionToDestroy.destroy()`.

Всё это записывается в `RenderSystem.recordRenderCall(...)` для render thread.

---

## 7. GlobalMeshCache, PartGeometry, ObjModelVboBuilder

### 7.1 GlobalMeshCache

Файл: `com.hbm_m.client.render.GlobalMeshCache`

Центральный кэш для всех VBO-объектов. Структуры:

```java
ConcurrentHashMap<String, PartGeometry>            quadsCache;        // BakedQuad'ы
ConcurrentHashMap<String, WeakReference<VertexBuffer>>  vertexBuffers; // Mojang VertexBuffer
ConcurrentHashMap<String, SingleMeshVboRenderer>   rendererCache;     // наши VBO рендереры
ConcurrentHashMap<String, IrisCompanionMesh>       irisCompanionCache;
```

Лимит `MAX_CACHE_SIZE = 256` для каждой коллекции — защита от утечек при долгой сессии.

API:

- `getOrCompilePartGeometry(key, model)` → `PartGeometry`
- `getOrCompile(key, model)` → `List<BakedQuad>`
- `getOrCreateRenderer(key, model)` → `SingleMeshVboRenderer`
- `clearAll()` — массовое удаление всех VBO (на render thread).

Все рендереры машин используют `GlobalMeshCache` для **одного экземпляра геометрии на тип машины** (а не по экземпляру BE) — критично для памяти.

### 7.2 PartGeometry

Файл: `com.hbm_m.client.render.PartGeometry`

Хранит **скомпилированный список квадов** одной части модели:

```java
public record PartGeometry(List<BakedQuad> solidQuads) {
    public static final RandomSource BAKE_SEED = RandomSource.create(42L);  // детерминизм

    public static PartGeometry compile(BakedModel partModel);
    public SingleMeshVboRenderer.VboData toVboData(String partName);
    public boolean isEmpty();
}
```

`compile(model)`:

1. Разворачивает FRAPI ForwardingBakedModel'ы.
2. Сэмплирует все 6 directions + null direction с `BAKE_SEED` для определённости.
3. Возвращает immutable список.

`toVboData(partName)` собирает `ByteBuffer` (вершины × 32 байта) и `IntBuffer` (индексы) из квадов через unpack `int[] vertexData` каждого `BakedQuad`. Учитывает packed normals (если 0 — берёт face normal). Защита от NaN в координатах/UV.

### 7.3 ObjModelVboBuilder

Файл: `com.hbm_m.client.render.ObjModelVboBuilder`

Тонкий wrapper:

```java
public static SingleMeshVboRenderer.VboData buildSinglePart(BakedModel part);
public static SingleMeshVboRenderer.VboData buildSinglePart(BakedModel part, String partName);
```

Внутри: `PartGeometry.compile(part).toVboData(partName)`. Возвращает `null` если у части нет квадов.

---

## 8. Кастомные GLSL-шейдеры (block_lit)

### 8.1 block_lit.vsh (вершинный)

Файл: `src/main/resources/assets/hbm_m/shaders/core/block_lit.vsh`

```glsl
#version 330 core

layout(location = 0) in vec3 Position;
layout(location = 1) in vec3 Normal;
layout(location = 2) in vec2 UV0;

#ifdef USE_INSTANCING
layout(location = 3) in vec3 InstPos;        // Position
layout(location = 4) in vec4 InstRot;        // Quaternion (x, y, z, w)
layout(location = 5) in float InstBrightness; // Light
#endif

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float Brightness;

out vec2 texCoord;
out float brightness;
out float vertexDistance;
out vec3 fragNormal;
```

Ключевая особенность — **один и тот же исходник** компилируется в два разных шейдера через `#define USE_INSTANCING`:

- Без define → используются `ModelViewMat` + `Brightness` (классический per-draw путь).
- С define → собирает MV-матрицу из `InstPos` + кватерниона `InstRot` через `quatToMat4` и берёт `bright = InstBrightness`.

```glsl
void main() {
    mat4 modelView;
    float bright;

#ifdef USE_INSTANCING
    mat4 rotMatrix = quatToMat4(InstRot);
    mat4 translation = mat4(1.0);
    translation[3] = vec4(InstPos, 1.0);
    modelView = translation * rotMatrix;
    bright = InstBrightness;
    fragNormal = mat3(rotMatrix) * Normal;
#else
    modelView = ModelViewMat;
    bright = Brightness;
    fragNormal = mat3(modelView) * Normal;
#endif

    vec4 viewPos = modelView * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPos;
    texCoord = UV0;
    brightness = bright;
    vertexDistance = length(viewPos.xyz);
}
```

`quatToMat4` — стандартная конверсия кватерниона в 4×4 rotation matrix (без translate, чисто rotation).

### 8.2 block_lit.fsh (фрагментный, общий)

```glsl
#version 330 core

in vec2 texCoord;
in float brightness;
in float vertexDistance;

uniform sampler2D Sampler0;
uniform vec4 FogColor;
uniform float FogStart;
uniform float FogEnd;

out vec4 fragColor;

void main() {
    vec4 baseColor = texture(Sampler0, texCoord);
    vec3 lit = baseColor.rgb * brightness;
    lit *= 0.6;                              // глобальное затемнение машин
    if (baseColor.a < 0.1) discard;          // alpha-cutout
    float fogFactor = clamp((FogEnd - vertexDistance) / (FogEnd - FogStart), 0.0, 1.0);
    vec3 colorWithFog = mix(FogColor.rgb, lit, fogFactor);
    fragColor = vec4(colorWithFog, baseColor.a);
}
```

Простой shader: alpha-cutout 0.1, multiplicative brightness, экспоненциальный туман.

### 8.3 JSON-конфиги шейдеров

`block_lit_simple.json`:

```json
{
    "vertex": "hbm_m:block_lit",
    "fragment": "hbm_m:block_lit",
    "samplers": [{ "name": "Sampler0" }],
    "uniforms": [
        { "name": "ModelViewMat", "type": "matrix4x4", "count": 16, "values": [...identity...] },
        { "name": "ProjMat",      "type": "matrix4x4", "count": 16, "values": [...identity...] },
        { "name": "FogStart",     "type": "float", "count": 1, "values": [ 0.0 ] },
        { "name": "FogEnd",       "type": "float", "count": 1, "values": [ 1.0 ] },
        { "name": "FogColor",     "type": "float", "count": 4, "values": [ 0.0, 0.0, 0.0, 0.0 ] },
        { "name": "Brightness",   "type": "float", "count": 1, "values": [ 1.0 ] }
    ]
}
```

`block_lit_instanced.json`: то же самое, но `"vertex": "hbm_m:block_lit_instanced"` (виртуальный путь, см. §9).

---

## 9. Регистрация шейдеров и preprocessor-инжекция

### 9.1 Проблема: vanilla кэширует Program по имени

`net.minecraft.client.renderer.ShaderInstance` под капотом кэширует скомпилированную GL-программу через `Program.getOrCreate("hbm_m:block_lit")`. Если оба JSON указывают на один и тот же `"vertex": "hbm_m:block_lit"`, **второй ShaderInstance переиспользует уже скомпилированный без `#define USE_INSTANCING`**, и наш инстанс-шейдер молча не работает.

### 9.2 Решение: ShaderPreDefinitions.wrapRedirect

Файл: `com.hbm_m.client.render.shader.modification.ShaderPreDefinitions`

Оборачивает `ResourceProvider`:

- Если запрашивают `virtualTarget` (например, `hbm_m:shaders/core/block_lit_instanced.vsh`) — лезет за `realSource` (`hbm_m:shaders/core/block_lit.vsh`), пропускает через `ShaderModification.apply(...)`, возвращает синтетический Resource.

В `ClientSetup.onRegisterShaders`:

```java
ResourceLocation realVsh =
    ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "shaders/core/block_lit.vsh");
ResourceLocation virtualInstancedVsh =
    ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "shaders/core/block_lit_instanced.vsh");

ShaderModification instancingDefine =
    ShaderModification.builder().define("USE_INSTANCING");

ResourceProvider instancedProvider =
    ShaderPreDefinitions.wrapRedirect(event.getResourceProvider(),
                                       virtualInstancedVsh, realVsh, instancingDefine);

event.registerShader(
    new ShaderInstance(instancedProvider, "hbm_m:block_lit_instanced", blockLitInstancedFormat),
    ModShaders::setBlockLitInstancedShader
);
```

Так как **virtual file имени `block_lit_instanced.vsh`** на диске НЕТ, vanilla `Program.getOrCreate` не найдёт коллизий и компилирует отдельную программу с инжектированным `#define USE_INSTANCING`.

### 9.3 ShaderModification

Файл: `com.hbm_m.client.render.shader.modification.ShaderModification`

Лёгкий GLSL-модификатор (вдохновлён Veil), без `glslprocessor`-зависимости. Поддерживает:

- `define(key, value)` — вставляет `#define KEY VALUE` после строки `#version`.
- `insertBefore(regex, text)`, `insertAfter(regex, text)` — вставка по regex-маркеру.
- `replace(regex, replacement)`.

Применяется один раз при загрузке шейдера, не используется на render-loop.

### 9.4 Vertex format'ы

В `ClientSetup.onRegisterShaders`:

```java
// Simple variant
VertexFormat blockLitSimpleFormat = new VertexFormat(ImmutableMap.<String, VertexFormatElement>builder()
    .put("Position", DefaultVertexFormat.ELEMENT_POSITION)
    .put("Normal",   DefaultVertexFormat.ELEMENT_NORMAL)
    .put("UV0",      DefaultVertexFormat.ELEMENT_UV0)
    .build());

// Instanced variant — расширен InstPos/InstRot/InstBrightness как GENERIC
VertexFormat blockLitInstancedFormat = new VertexFormat(ImmutableMap.<String, VertexFormatElement>builder()
    .put("Position",       DefaultVertexFormat.ELEMENT_POSITION)
    .put("Normal",         DefaultVertexFormat.ELEMENT_NORMAL)
    .put("UV0",            DefaultVertexFormat.ELEMENT_UV0)
    .put("InstPos",        new VertexFormatElement(0, FLOAT, GENERIC, 3))
    .put("InstRot",        new VertexFormatElement(0, FLOAT, GENERIC, 4))
    .put("InstBrightness", new VertexFormatElement(0, FLOAT, GENERIC, 1))
    .build());
```

### 9.5 ModShaders

Файл: `com.hbm_m.client.render.ModShaders`

Простое хранилище ссылок:

```java
private static ShaderInstance dynamicCutoutShader;
private static ShaderInstance blockLitSimpleShader;
private static ShaderInstance blockLitInstancedShader;
private static ShaderInstance thermalVisionShader;

// + сеттеры для регистрации
public static void setBlockLitSimpleShader(ShaderInstance s) { blockLitSimpleShader = s; }
// + геттеры
public static ShaderInstance getBlockLitInstancedShader() { return blockLitInstancedShader; }
```

---

## 10. Совместимость с Iris/Oculus

Это **самая сложная подсистема** в коде, занимает ~6 классов. Существует чисто из-за того, что Iris перехватывает рендер мира и оборачивает все ShaderInstance в `ExtendedShader`'ы со своими uniforms (`iris_*`), и для корректной работы под пак-шейдером нужно играть по их правилам.

### 10.1 ShaderCompatibilityDetector

Файл: `com.hbm_m.client.render.shader.ShaderCompatibilityDetector`

Главный «выключатель». Через `ModList.get().isLoaded("oculus")` и кэшированный набор статических методов даёт ответы на:

- `**isExternalShaderActive()`** — пользователь активировал шейдер-пак прямо сейчас (а не просто установлен Iris/Oculus). Кэшируется для безопасного вызова с background threads (Sodium chunk-builder'ов).
- `**isRenderingShadowPass()`** — рефлексивный вызов `ShadowRenderingState.areShadowsCurrentlyBeingRendered()`.
- `**canUseIrisExtendedShader()**` — `isExternalShaderActive() && IrisExtendedShaderAccess.isReflectionAvailable()`.
- `**useVboGeometry()**` — `!isExternalShaderActive() || ModClothConfig.useIrisExtendedShaderPath()`. **Главный switch** для `BakedModel.getQuads(...)` — если `true`, baked возвращает empty list, BER рендерит всё.
- `**useNewIrisVboPath()`** — `isExternalShaderActive() && ModClothConfig.useIrisExtendedShaderPath()`. Сахар «под шейдерами через новый путь».

Также там же находится **deferred chunk invalidation** — при смене шейдер-пака запрашивает инвалидацию чанков (через `ClientModEvents.onClientTick → processPendingChunkInvalidation`), чтобы Sodium перезапёк chunk mesh'и (потому что `BakedModel.getQuads(...)` теперь должен возвращать другой набор квадов).

### 10.2 IrisCompanionMesh

Файл: `com.hbm_m.client.render.IrisCompanionMesh`

Companion-меш в формате `IrisVertexFormats.ENTITY` (15-attribute layout: position, color, uv, uv1, uv2, normal, **iris_Entity, mc_midTexCoord, at_tangent**, и т.д.).

Зачем отдельный меш? Потому что под Iris **vertex format расширяется** — обычный `NEW_ENTITY` (8 атрибутов) дополняется до `IrisVertexFormats.ENTITY` (15). Если мы просто отдадим Iris свой VBO в формате Position/Normal/UV0 и попросим прочитать как extended — будут читаться байты за границами (broken geometry).

#### 10.2.1 Dynamic attribute binding

Главная фишка — **динамический биндинг location'ов**. Vanilla `BufferBuilder` подменяется Iris'ом и пишет данные в нужный layout, но GLSL-линкер может присвоить attribute'ам произвольные locations. `IrisCompanionMesh.prepareForShader(programId)`:

1. Запрашивает через `glGetAttribLocation(programId, "iris_Entity")`, `"mc_midTexCoord"`, `"at_tangent"`, `"vaUV2"`.
2. Кэширует locations per-program.
3. Привязывает каждый атрибут на своё место в companion VBO через `glVertexAttribPointer(loc, size, type, normalized, stride, offset)`.

Это обходит баг Mojang'а, где attribute names биндятся через хардкод layout(location=...) при компиляции.

#### 10.2.2 vaUV2 (lightmap UV)

`vaUV2` намеренно НЕ enabled в VAO — это позволяет передавать per-draw lightmap через `glVertexAttribI2i(uv2Loc, blockU, skyV)` как «current value» disabled-attribute'а. Pack-шейдер читает `ivec2` и применяет к каждой вершине.

Целочисленный pipeline (`I2i`, не `2f`) — обязателен, потому что pack-шейдеры объявляют `vaUV2 ivec2`, а float-bank они не читают.

#### 10.2.3 Жизненный цикл

- `ensureBuilt()` — лениво строит VBO из `quadsForIris`, преобразуя их в IrisVertexFormats.ENTITY layout. Использует `BufferBuilder` с обработкой через `IrisBufferHelper.beginWithoutExtending(...)` (см. ниже) и затем читает финальный buffer.
- `getVaoId()`, `getIndexCount()`, `getUv2Location()`, `prepareForShader(programId)` — runtime API.
- `destroy()` — удаление через `glDeleteBuffers/glDeleteVertexArrays` (на render thread).

### 10.3 IrisBufferHelper

Файл: `com.hbm_m.client.render.shader.IrisBufferHelper`

Reflection-helper для вызова `iris$beginWithoutExtending` на `BufferBuilder` (через MixinExtensions от Iris/Connector). Без этого `bufferBuilder.begin(...)` под Iris **автоматически расширил бы** vertex format до extended-формы, ломая stride. Нам нужен исходный un-extended формат, чтобы потом контролировать конверсию вручную.

Метод-handle кэшируется, чтобы избежать повторных reflection lookup'ов.

### 10.4 IrisExtendedShaderAccess

Файл: `com.hbm_m.client.render.shader.IrisExtendedShaderAccess`

Reflection-API к внутренней Iris-кухне. Позволяет:

- `**getBlockShader(boolean shadowPass)`** — достаёт `ExtendedShader` из `ShaderMap.getShader(BLOCK)` или `ShaderMap.getShader(SHADOW_BLOCK)` соответственно.
- `**setCurrentRenderedBlockEntity(int id)`** / `**restoreCurrentRenderedBlockEntity(int prev)`** — управляет глобальным uniform'ом `blockEntityId`. Обнуляем чтобы pack-шейдеры не интерпретировали наши draw'ы как special-case (155 = красный, 252 = portal и т.д.).
- `**tickPass()`** — bump'ает counter, инвалидирующий per-pass shader cache. Вызывается раз в кадр.

Все reflection'ы через `MethodHandles.publicLookup()` для скорости (быстрее, чем `Method.invoke`).

### 10.5 IrisPhaseGuard

Файл: `com.hbm_m.client.render.shader.IrisPhaseGuard`

`AutoCloseable` try-with-resources guard, устанавливающий `WorldRenderingPhase.BLOCK_ENTITIES` (рефлексивно) на входе и сбрасывающий в `NONE` на выходе. Важно для pack-шейдеров, которые рендерят BlockEntity по-другому, чем terrain (например, отключают ssao, добавляют outline и т.д.).

```java
try (IrisPhaseGuard ignored = IrisPhaseGuard.pushBlockEntities()) {
    // ... наш рендер ...
}
```

### 10.6 IrisRenderBatch

Файл: `com.hbm_m.client.render.shader.IrisRenderBatch`

Per-BlockEntity batching session. Существует чтобы амортизировать `**shader.apply()**` и `**shader.clear()**` — самые дорогие вызовы под Iris (60%+ frame time на больших фермах).

#### 10.6.1 begin / close

```java
try (IrisRenderBatch batch = IrisRenderBatch.begin(shadowPass, RenderSystem.getProjectionMatrix())) {
    // ... все детали машины через batch.drawCompanion(...) или просто render(...)
    // shader.apply() и clear() вызывается АВТОМАТИЧЕСКИ один раз на батч.
}
```

Паттерн открытия в каждом BER:

```java
boolean useIrisBatch = useNewIrisVboPath() && (!useBatching || shadowPass);
if (useIrisBatch) {
    try (IrisRenderBatch batch = IrisRenderBatch.begin(shadowPass, projection)) {
        renderInternal(...);
    }
} else {
    renderInternal(...);
}
```

#### 10.6.2 Persistent shadow batch

Iris не выдаёт нам callback'ов на конец shadow pass'а, поэтому batch для shadow становится **persistent** — может пережить несколько BE-рендеров, закрываясь:

- Когда `begin(true)` вызван следующим BE с pass change → закрытие старого, открытие нового.
- В `RenderLevelStageEvent.AFTER_LEVEL` через `closePersistentIfActive()` — safety net на случай, если main pass пустой (все BE отбракованы frustum culling'ом, а shadow pass их видел).

#### 10.6.3 drawCompanion

```java
public void drawCompanion(IrisCompanionMesh companion, Matrix4f mvMat, int packedSmoothLight);
```

Внутри:

- Загружает `ModelViewMat` через `glUniformMatrix4fv` напрямую (минуя Mojang Uniform proxy).
- Пересчитывает `iris_ModelViewMatInverse`, `iris_NormalMat` если изменились.
- `glVertexAttribI2i(uv2Loc, blockU, skyV)`.
- `glDrawElements`.

### 10.7 ShaderReloadListener

Файл: `com.hbm_m.client.render.shader.ShaderReloadListener`

`SimplePreparableReloadListener<Void>`, который при F3+T или смене шейдер-пака:

- Инвалидирует кэши `IrisExtendedShaderAccess` и `IrisRenderBatch`.
- Гарантирует, что новые shader instances будут заново подняты через ShaderMap.

Регистрируется в `ClientSetup.onResourceReload`.

---

## 11. BakedModel-инфраструктура

### 11.1 AbstractMultipartBakedModel

Файл: `com.hbm_m.client.model.AbstractMultipartBakedModel`

Базовый класс для всех BakedModel'ей машин с несколькими частями. Поля:

```java
protected final Map<String, BakedModel> parts;
private final ItemTransforms itemTransforms;
```

API:

- `getPart(name)` — достать BakedModel'ь конкретной части.
- `getParticleIcon(ModelData)` — приоритетный выбор particle (какая часть «лицо» машины).
- `getQuads(state, side, rand, modelData, renderType)` — главный метод, см. ниже.
- `getItemRenderPartNames()` — порядок частей для item-render'а (инвентарь/рука).
- `shouldSkipWorldRendering(state)` — `true` → BakedModel НЕ запекает квады в chunk mesh, всё делает BER.

Подклассы переопределяют `shouldSkipWorldRendering` и `getQuads` под свою логику.

### 11.2 ItemTransforms

Все multipart-модели поддерживают item-transforms (для GUI, ground, fixed display и т.д.). Передаются из JSON через лоадер.

### 11.3 Конкретные модели машин

Все модели делятся на **три категории** по поведению `getQuads(...)` для блок-state'а:


| Категория                                     | Модели                                            | Поведение                                                  |
| --------------------------------------------- | ------------------------------------------------- | ---------------------------------------------------------- |
| **Динамический switch по `useVboGeometry()`** | AdvancedAssembler, Assembler, ChemicalPlant, Door | Переключение между chunk mesh (legacy) и BER (VBO) на лету |
| **Всегда полностью в chunk mesh**             | FluidTank                                         | Нет BER, чистая статика с реактивностью через ModelData    |
| **Гибрид Base+BER**                           | Press                                             | Base всегда в chunk mesh, Head всегда в BER                |
| **Никогда в chunk mesh**                      | HydraulicFrackingTower                            | Sodium не справляется с 24-блочной высотой                 |


Подробное описание каждой модели:

#### 11.3.1 MachineAdvancedAssemblerBakedModel — динамический switch

- `cachedPartNames`: Base, Frame, Ring, ArmLower1, ArmUpper1, Head1, Spike1, ArmLower2, ArmUpper2, Head2, Spike2 (priority sorted).
- `shouldSkipWorldRendering`: `false` всегда (но `getQuads` сам делает switch).
- `**getQuads(state, ...)`** при `state != null`:
  - Если `useVboGeometry() == true` → возвращает `List.of()` (пустой). BER рендерит всё через VBO/Iris.
  - Если `useVboGeometry() == false` (legacy путь под шейдером):
    - Base всегда (с поворотом по FACING).
    - Frame если `state.getValue(FRAME) == true`.
    - Ring + 8 анимированных частей **только при `RENDER_ACTIVE = false`** (idle pose). При активной работе их рисует BER через `putBulkData`.
- `getQuads(state == null, ...)` (item render): кэшированные квады всех частей для inventory/GUI.

#### 11.3.2 MachineAssemblerBakedModel — динамический switch

- Parts: Body, Slider, Arm, Cog (priority sorted).
- `shouldSkipWorldRendering`: `false` всегда.
- `**getQuads**` аналогично Advanced Assembler:
  - VBO режим → empty.
  - Legacy режим → Body + (idle Slider/Arm/Cog при `RENDER_ACTIVE = false`).
- **Особенность Cog**: в idle-режиме рендерит **4 копии** в позициях из `COG_IDLE_OFFSETS` (т.к. BER при работе анимирует 4 шестерни в 4 слотах).
- `**bakedOffsetZ = -1.0f`**: применяется через `ModelHelper.translateQuads` для компенсации extra offset в +Z из baked pipeline'а — чтобы baked совпадал с VBO позиционированием.
- `getRenderTypes` → `ChunkRenderTypeSet.of(RenderType.cutoutMipped())` — для прозрачных текстур (стекло, решётки).

#### 11.3.3 ChemicalPlantBakedModel — динамический switch

- Parts: Base, Frame, Slider, Spinner, Fluid (priority sorted).
- `shouldSkipWorldRendering`: `false` всегда.
- `getRenderTypes` → `ChunkRenderTypeSet.of(RenderType.cutout())` — только cutout (исключает попытки запекать translucent).
- `**getQuads`**:
  - VBO режим → empty.
  - Legacy режим:
    - Base всегда + Frame если `state.getValue(FRAME) == true`.
    - При `RENDER_ACTIVE = false` (idle) → добавляются Slider и Spinner с soft-peak sine оффсетом.
- **Fluid НИКОГДА не запекается** в chunk mesh — translucent слой не поддерживается корректно в baked-mesh'е (порядок, смешивание). Fluid рендерится только через BER `ChemicalPlantRenderer` через отдельный `RenderType.translucent` pass (см. §12.3).
- `chemicalSps(0) = 1.0` — soft peak sine для idle-оффсета слайдера: при anim=0 даёт 1.0, оффсет рассчитывается через `cos/sin(rotationY)` для проекции на локальную X ось.

#### 11.3.4 DoorBakedModel — динамический switch (через shouldSkipWorldRendering)

- Parts variable: frame/Frame/DoorFrame/Base, door, doorLeft, doorRight, spinny_upper/lower, и т.д.
- `**shouldSkipWorldRendering(state)` = `state != null && useVboGeometry()`** — единственная модель, которая делает switch через этот метод (а не внутри getQuads).
- Поддерживает skin variants через `DoorModelSelection` (legacy/modern + skin id).
- Работает с `ColladaAnimationData` для DAE-анимаций (см. §13).
- `getQuads`: complex — учитывает `state` (movement state), `useVboGeometry`, isMoving. В static состоянии (закрыта/открыта без движения) всё в baked, при movement только frame в baked, остальное — BER.

#### 11.3.5 MachineHydraulicFrackiningTowerBakedModel — никогда в chunk mesh

- `**shouldSkipWorldRendering(state)` всегда возвращает `true`** для не-null state — модель 24 блока в высоту. Sodium использует 16-bit vertex coordinates, и на такой высоте они переполняются («модель выворачивается наизнанку»).
- `getQuads(state, ...)` для не-null state всегда возвращает пустой список — Sodium ничего не запечёт.
- `getQuads(state == null, ...)` (item) → возвращает квады для предмета в инвентаре.
- BER `MachineHydraulicFrackiningTowerRenderer` рендерит ВСЕ части вышки сам через VBO + 32-bit float координаты, **независимо от наличия шейдера**.

#### 11.3.6 PressBakedModel — гибрид Base+BER

- Parts: Base, Head.
- `**shouldSkipWorldRendering`: `false` всегда** — Base **всегда запекается** в chunk mesh.
- `**getQuads(state, ...)`** для не-null state **всегда** возвращает Base, перенесённый через `ModelHelper.translateQuads(..., 0.5f, 0f, 0.5f)` — независимо от наличия шейдера или конфига.
- BER `MachinePressRenderer` рендерит **только Head** (с анимацией движения вниз).
- Поддерживает `getHeadRestOffset()`, `getHeadTravelDistance()` для расчёта анимации.

#### 11.3.7 MachineFluidTankBakedModel — статика с реактивным ModelData

- Parts: Frame, Tank.
- `**shouldSkipWorldRendering`: `false` всегда**.
- **НЕТ соответствующего BER** — `MachineFluidTankBlockEntity` не зарегистрирован в `BlockEntityRenderers.register(...)`. Это **единственный машинный блок без BER**.
- `**getQuads(...)`** **всегда** возвращает Frame + Tank, независимо от наличия шейдера или `useVboGeometry()`. У этого блока **в принципе нет VBO/Iris/instancing пути**.
- **Реактивная текстура жидкости через `ModelData`**:
  - `MachineFluidTankBlockEntity` определяет `ModelProperty<ResourceLocation> FLUID_TEXTURE_PROPERTY`.
  - `getModelData()` возвращает builder с текущей текстурой жидкости (`getTankTextureLocation()`).
  - При смене жидкости BE вызывает `requestModelDataUpdate()` + `level.sendBlockUpdated(getBlockState(), getBlockState(), 8)` — флаг 8 (`Block.UPDATE_CLIENTS`) заставляет клиентскую сторону **пересобрать chunk mesh**.
- В `getQuads(...)`:
  - Берётся текстура из `modelData.get(FLUID_TEXTURE_PROPERTY)` (или `DEFAULT_TEX = "hbm_m:block/tank/tank_none"` если null).
  - Для Tank-части квады **ретекстурируются** через `retextureAndFixUV(...)` — извлекаются normalized UV из старого спрайта, пересчитываются на новый. Кэш `quadCache` хранит результаты по `(textureLocation, side)`.
  - Применяется `ModelHelper.transformQuadsByFacing(...)` для поворота по FACING.
- **Под шейдер-паками** работает как обычная chunk-геометрия — Iris интегрирует её в свой G-buffer без дополнительной обработки. Нет проблем с extended shader, vaUV2, blockEntityId и прочей Iris-кухней.

### 11.4 ModelHelper.translateQuads и transformQuadsByFacing

Утилиты для манипуляций списком `BakedQuad`:

- `translateQuads(quads, dx, dy, dz)` — нужна когда root transform из JSON смещает модель, но мы рендерим часть через chunk mesh, который этого transform'а не учитывает (например, в `MachineAssemblerBakedModel` с `bakedOffsetZ = -1.0f`).
- `transformQuadsByFacing(quads, rotationY)` — поворот по горизонтальной оси для отображения по `BlockState.FACING`. Используется во всех multipart-моделях, кроме DoorBakedModel (там через DAE).

---

## 12. BlockEntityRenderer'ы машин

Все BER наследуют `AbstractPartBasedRenderer<TBE, TModel>`. **Зарегистрированы 6 BER**: Advanced Assembler, Assembler, Door, Press, Chemical Plant, Hydraulic Fracking Tower. `**MachineFluidTankBlockEntity` НЕ имеет BER** — он рендерится исключительно через свою BakedModel (см. §11.3.7), потому что является полностью статическим блоком с реактивностью только через `ModelData`.

### 12.1 AbstractPartBasedRenderer

Файл: `com.hbm_m.client.render.AbstractPartBasedRenderer`

Шаблонный метод `render(...)`:

```java
public void render(TBE be, float partialTick, PoseStack poseStack,
                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
    // 1. Frustum culling check
    // 2. getModel(be) → unwrap FRAPI → getModelType(model) cast
    // 3. setupBlockTransform(animator, be) — поворот/смещение под facing
    // 4. renderParts(be, model, animator, partialTick, packedLight, packedOverlay, poseStack, bufferSource)
}
```

Подклассы переопределяют:

- `getModelType(BakedModel raw)` — cast/проверка типа модели.
- `getFacing(TBE be)` — куда смотрит блок.
- `setupBlockTransform(animator, be)` — кастомный transform (по умолчанию translate(0.5, 0, 0.5)).
- `renderParts(...)` — вся логика.
- `getViewDistance()` — макс дистанция рендера (по умолчанию 64; машины ставят 128).
- `shouldRenderOffScreen(be)` — true для очень больших BE (вышка).

Также содержит `unwrapFabricForwardingModels(BakedModel)` — рекурсивно разворачивает `ForwardingBakedModel`'ы (Continuity, Embeddium emissive и т.д.).

### 12.2 MachineAdvancedAssemblerRenderer

Файл: `com.hbm_m.client.render.implementations.MachineAdvancedAssemblerRenderer`

Самый сложный BER. Рисует:

- Base (статика, instanced).
- Frame (статика, instanced, опциональный по blockstate `FRAME`).
- Ring (анимация — вращение Y).
- 2× руки (ArmLower, ArmUpper, Head, Spike) с 4-х координатной анимацией (a0/a1/a2/a3 углы).
- Recipe icon — item floating над машиной.

#### 12.2.1 Instanced рендереры

11 статических `InstancedStaticPartRenderer` (по одному на каждую часть), создаются через `createInstancedForPart(model, partName)` → `GlobalMeshCache.getOrCompilePartGeometry → toVboData → new InstancedStaticPartRenderer(data, geo.solidQuads())`.

#### 12.2.2 Логика renderParts

```java
1. Occlusion check (OcclusionCullingHelper)
2. Если !useVboGeometry && !renderActive → return (всё в baked)
3. dynamicLight = LightTexture.pack(blockLight, skyLight)
4. renderWithVBO(...)
```

#### 12.2.3 renderWithVBO

```java
1. Lazy init instancers
2. Open IrisRenderBatch если нужно (см. §10.6.2)
3. renderPartsInternal:
   a. Если useVboPath → рендер Base/Frame instanced/single
   b. shouldSkipAnimationUpdate(blockPos)?
      - Нет → renderAnimated(ring + 2 arms with 4 segments each)
      - Да → renderStaticLOD(только Ring без анимации)
```

#### 12.2.4 Reusable matrices

`matRing`, `matLower`, `matUpper`, `matHead`, `matSpike` — поля класса (НЕ локальные), переиспользуются через `.set(other)` и `.identity()`. Один renderer instance shared между всеми BE через BlockEntityRenderers.register, так что safe только потому что render идёт строго на render thread.

#### 12.2.5 Recipe icon

Отдельный override `render(...)`:

```java
@Override
public void render(...) {
    visibleThisFrame = false;
    super.render(...);  // → renderParts → flipping visibleThisFrame to true if culling passes
    if (visibleThisFrame) {
        renderRecipeIconDirect(...);
    }
}
```

Защита от рендеринга item для off-screen машин (один из самых дорогих CPU pigs на 400-машинных фермах).

### 12.3 ChemicalPlantRenderer

Файл: `com.hbm_m.client.render.implementations.ChemicalPlantRenderer`

Похоже на Advanced Assembler, но 2 анимированные части:

- Slider — горизонтальное движение по X (sin-волна).
- Spinner — вращение по Y.

Особенность: **fluid rendering** через `RenderType.translucent`:

```java
private static void renderFluid(MachineChemicalPlantBlockEntity be, ...,
                                 FluidStack fluid) {
    var sprite = textureAtlas.apply(fluid.getStillTexture());
    int tintColor = ext.getTintColor(fluid);
    
    float y0 = 0.10F;
    float y1 = y0 + 0.60F * fluidFillFraction;  // динамический уровень
    
    boolean scrollActive = be.getDidProcess();
    float scroll = (gameTime + partialTick) * 0.02F;  // UV scroll если работает
    
    VertexConsumer vc = buffer.getBuffer(RenderType.translucent());
    // ... 6 face cube квадов с правильными UV+normal+tint+light ...
}
```

Жидкость **никогда не запекается в chunk mesh** (translucency requires per-frame sort), всегда через BER.

### 12.4 MachinePressRenderer

Файл: `com.hbm_m.client.render.implementations.MachinePressRenderer`

Press с одной анимированной частью (Head):

- Base запекается в chunk mesh всегда (`shouldSkipWorldRendering = false`).
- Head движется вниз по Y по `blockEntity.getPressAnimationProgress(partialTick)`.
- Item rendering (workpiece + stamp) через `mc.getItemRenderer().renderStatic(...)`.

`buildHeadTransform`:

```java
Vector3f rest = model.getHeadRestOffset();  // из JSON
float travel = model.getHeadTravelDistance();
float progress = freezeAnimation ? 0.0F : be.getPressAnimationProgress(partialTick);
float effectiveTravel = Math.max(0.0F, travel - PIXEL);
float offsetY = rest.y() - (progress * effectiveTravel);
return new Matrix4f().translate(rest.x(), offsetY, rest.z()).scale(HEAD_SCALE);
```

### 12.5 MachineHydraulicFrackiningTowerRenderer

Файл: `com.hbm_m.client.render.implementations.MachineHydraulicFrackiningTowerRenderer`

24-блочная вышка. Особенности:

- `shouldRenderOffScreen = true` всегда — иначе обрезается Vanilla frustum culling.
- Только **одна часть**: `Cube_Cube.001` (имя из OBJ).
- Под legacy путь (под шейдер-паком + `useIrisExtendedShaderPath = false`): использует `RenderType.cutoutMipped` напрямую (один draw без инстансинга), т.к. base baked-путь недоступен из-за 16-bit overflow Sodium'а.
- Под VBO путь (нет шейдера ИЛИ `useIrisExtendedShaderPath = true`): либо instanced, либо single через `MachineHydraulicFrackiningTowerVboRenderer.render(...)`.

### 12.6 MachineAssemblerRenderer

Файл: `com.hbm_m.client.render.implementations.MachineAssemblerRenderer`

Похож на Advanced Assembler, но проще — части: Body, Slider, Arm, Cog (×4).

Анимация через `System.currentTimeMillis()`-based ping-pong (1.7.10-style):

```java
long t = (time % 5000) / 5;
int offset = (int) (t > 500 ? 500 - (t - 500) : t);
float sliderX = offset * 0.003f - 0.75f;
```

Особенность — **есть отдельный `renderAnimatedWithBulkData`** для случая «старый шейдер + idle». В этом режиме animated части (Slider, Arm, Cog) рендерятся через `putBulkData` поверх baked Base. Это нужно когда useVboGeometry=false (старая Iris ветка) и машина работает (renderActive=true).

`renderRecipeIconDirect` — аналог advanced assembler'а, рисует `getRecipeOutput(be)` (item template из inventory slot 4).

---

## 13. Дверная подсистема

Файл: `com.hbm_m.client.render.implementations.DoorRenderer`

Самый сложный renderer в моде. Поддерживает:

- 13 разных типов дверей (large_vehicle_door, fire_door, qe_containment_door, water_door, vault_door, и т.д.).
- 2 модельные ветки (legacy/modern) + множественные skin variants.
- DAE (Collada) animation parsing для динамических анимаций (water_door со spinny упорами).
- Procedural transformations (origin → rotation → translation).

### 13.1 Архитектура

```
DoorBakedModel (JSON) ──→ DoorRenderer.renderParts
                              │
                              ├─ getDoorTypeKey(decl) → "large_vehicle_door" / etc.
                              ├─ DoorModelSelection (variant: legacy/modern + skin)
                              ├─ partNames из model.getPartNames()
                              ├─ detectFramePart(...) → "frame"/"Frame"/"DoorFrame"/"base"/"Base"
                              │
                              ├─ if (!useVboGeometry && !isMoving) return  // всё в baked
                              │
                              └─ renderWithVBO(...)
                                  ├─ animData = ColladaAnimationData.getOrLoad(...) если включено
                                  ├─ if (Collada Z-up) → poseStack.mulPoseMatrix(Z_UP_TO_Y_UP)
                                  ├─ Если шейдер активен и !useVboGeometry:
                                  │    Для каждой анимированной части → renderAnimatedPartForIris (putBulkData)
                                  ├─ Иначе:
                                  │    Open IrisRenderBatch если нужно
                                  │    └─ renderDoorVboParts:
                                  │        ├─ Frame через InstancedStaticPartRenderer
                                  │        └─ Для каждой анимированной части → renderHierarchyVbo
                                  │            ├─ doPartTransform (origin → rotation → translation [+ DAE matrix])
                                  │            ├─ Если есть геометрия:
                                  │            │    Instanced (если включено) или DoorVboRenderer
                                  │            └─ Рекурсия в детей (DoorDecl + DAE children)
```

### 13.2 doPartTransform

```java
void doPartTransform(PoseStack poseStack, DoorDecl doorDecl, String partName,
                     float openTicks, boolean child, ColladaAnimationData animData,
                     DoorModelSelection selection) {
    // 1. Процедурный transform (origin → rotation → translation)
    doorDecl.getOrigin(partName, origin, selection);
    poseStack.translate(origin[0], origin[1], origin[2]);
    
    doorDecl.getRotation(partName, openTicks, rotation, selection);
    if (rotation[0] != 0) poseStack.mulPose(Axis.XP.rotationDegrees(rotation[0]));
    if (rotation[1] != 0) poseStack.mulPose(Axis.YP.rotationDegrees(rotation[1]));
    if (rotation[2] != 0) poseStack.mulPose(Axis.ZP.rotationDegrees(rotation[2]));
    
    doorDecl.getTranslation(partName, openTicks, child, translation, selection);
    poseStack.translate(-origin[0] + translation[0], ...);
    
    // 2. Полная DAE matrix (rotation + translation, без обнуления — пивоты в DAE важны)
    if (animData != null) {
        String daeObjectName = doorDecl.getDaeObjectName(partName);
        float normProgress = Math.min(1f, openTicks / doorDecl.getOpenTime());
        float timeSec = (doorDecl.isColladaAnimationInverted() ? (1f - normProgress) : normProgress) 
                        * animData.getDurationSeconds();
        Matrix4f matrix = animData.getTransformMatrix(daeObjectName, timeSec);
        if (matrix != null) {
            poseStack.mulPoseMatrix(matrix);
        }
    }
}
```

Применяются ОБА источника (процедурный + DAE) — они дополняют друг друга, обеспечивая корректные пивоты.

### 13.3 ColladaAnimationData

Поддерживает:

- Иерархию объектов (parent/children).
- Per-bone keyframe interpolation.
- Z-up → Y-up конверсия (Blender → Minecraft).
- Кэшируется глобально через `ColladaAnimationData.getOrLoad(...)`.

### 13.4 DoorVboRenderer

Файл: `com.hbm_m.client.render.implementations.DoorVboRenderer`

Подкласс `SingleMeshVboRenderer` для одной анимированной части одной двери. Кэшируется через `getOrCreate(model, partName, doorType, selection)` с ключом, включающим тип двери, model variant и skin.

Защита от использования для статических частей через `isStaticPart(partName)` — если попытаться создать VBO renderer для frame, кинется `IllegalArgumentException`.

### 13.5 detectFramePart / Frame instancer

Frame часть всегда обрабатывается отдельно через свой `InstancedStaticPartRenderer` в `instancedFrameCache`. Имя поиска: "frame", "Frame", "DoorFrame", "base", "Base" (в этом порядке).

Кэш ключ: `"frame_" + doorType + "_" + selection.getModelType().getId() + "_" + selection.getSkin().getId()` — отдельный экземпляр для каждой комбинации type+modelVariant+skin.

### 13.6 PARTS_WITHOUT_GEOMETRY

`Set<String>` — кэш частей без геометрии (например, чистые pivot-bone'ы для DAE-анимации). Заполняется один раз через `partHasGeometry()` — пробуем все 6 directions + null с детерминированным random. Если 0 квадов — добавляем в blacklist, не пытаемся создавать renderer.

### 13.7 Animation delay queue

`DoorAnimationDelayHelper` — отдельная утилита для отложенного перехода с моделью «движения» обратно на baked model после окончания анимации (избегает мерцания в момент смены).

### 13.8 Chunk invalidation

`DoorChunkInvalidationHelper` — после изменения BlockState двери (открыта/закрыта) запрашивает `levelRenderer.blockChanged(blockPos)` через `ConcurrentLinkedQueue` (на client tick), чтобы Sodium/Embeddium перезапёк chunk. Используется dedup через `ConcurrentHashMap`.

---

## 14. Вспомогательные сервисы

### 14.1 OcclusionCullingHelper

Файл: `com.hbm_m.client.render.OcclusionCullingHelper`

Raycast-based occlusion culling для BlockEntity. API:

```java
public static boolean shouldRender(BlockPos pos, Level level, AABB bounds);
public static void onFrameStart();  // инвалидация кэша
public static void setTransparentBlocksTag(TagKey<Block> tag);
```

Логика:

1. Per-frame `Long2ObjectOpenHashMap<Boolean>` cache (ключ = `pos.asLong()`).
2. Special handling для блоков очень близко к камере (`< 8 блоков` всегда видим).
3. Center test → 8 corner tests AABB через `Level.clip(ClipContext)`.
4. Учитывает Forge tag для прозрачных блоков (стекло, листья и т.д.) — они не occlude.

### 14.2 LightSampleCache

Файл: `com.hbm_m.client.render.LightSampleCache`

Per-frame кэш smoothed lightmap UVs. API:

```java
public static void getOrSample(BlockEntity be, int packedLight, float[] outUV, int outOffset);
public static void onFrameStart();
```

Логика:

1. Fast-path single-slot cache (последний BE, ключ — `pos.asLong()`).
2. На miss — сэмплирует `Level.getBlockState` + `LevelRenderer.getLightColor` в 6 точках face centers вокруг рендер-bounding box, усредняет.
3. Кладёт в `Long2ObjectOpenHashMap<float[2]>` для возможных повторных вызовов в этом кадре.

Это **критическая оптимизация**: на 11-парт ассемблере без кэша — 11 одинаковых сэмплов = ~17% frame time.

### 14.3 DoorChunkInvalidationHelper

См. §13.8.

### 14.4 LegacyAnimator

Файл: `com.hbm_m.client.render.LegacyAnimator`

Wrapper над `PoseStack`, эмулирующий API из 1.7.10 GL11 (push/pop/translate/rotate/scale + интерполяция между prev/curr). Используется внутри `setupBlockTransform` и `renderAnimatedWithBulkData` (legacy path для assembler'а).

### 14.5 EmptyEntityRenderer

Stub-renderer для entity, которые не должны иметь визуала (например, AIRSTRIKE_AGENT_ENTITY или невидимые ракеты).

### 14.6 RayVisualizationRenderer

Дебаг-renderer для визуализации raycast'ов (используется в режиме отладки).

---

## 15. Жизненный цикл кадра и события

Файл: `com.hbm_m.event.ClientModEvents`

### 15.1 RenderLevelStageEvent.AFTER_BLOCK_ENTITIES (main pass)

```java
@SubscribeEvent
public static void onRenderLevelStage(RenderLevelStageEvent event) {
    if (event.getStage() == Stage.AFTER_BLOCK_ENTITIES) {
        if (ModClothConfig.useInstancedBatching()) {
            MachineAdvancedAssemblerRenderer.flushInstancedBatches(event);
            MachineHydraulicFrackiningTowerRenderer.flushInstancedBatches(event);
            MachineAssemblerRenderer.flushInstancedBatches(event);
            DoorRenderer.flushInstancedBatches(event);
            MachinePressRenderer.flushInstancedBatches(event);
            ChemicalPlantRenderer.flushInstancedBatches(event);
        }
        IrisExtendedShaderAccess.tickPass();
        LightSampleCache.onFrameStart();
    }
}
```

Здесь:

1. **flushInstancedBatches** для каждого BER-типа (один `glDrawElementsInstanced` на тип).
2. `**IrisExtendedShaderAccess.tickPass()`** — bump'ает per-pass counter, инвалидирующий кэш shader lookup'а. Это самая большая CPU-экономия (~8.78% по profile trace).
3. `**LightSampleCache.onFrameStart()`** — bump'ает per-frame counter для invalidation освещения.

### 15.2 RenderLevelStageEvent.AFTER_LEVEL

```java
} else if (event.getStage() == Stage.AFTER_LEVEL) {
    IrisRenderBatch.closePersistentIfActive();
}
```

Safety net для persistent shadow batch'а (см. §10.6.2).

### 15.3 TickEvent.ClientTickEvent

```java
@SubscribeEvent
public static void onClientTick(TickEvent.ClientTickEvent event) {
    if (event.phase == Phase.START) {
        OcclusionCullingHelper.onFrameStart();  // инвалидация culling кэша
    } else if (event.phase == Phase.END) {
        DoorAnimationDelayHelper.processQueue();
        DoorChunkInvalidationHelper.processPendingInvalidations();
        ShaderCompatibilityDetector.processPendingChunkInvalidation();
    }
}
```

---

## 16. Перезагрузка ресурсов и cleanup

### 16.1 ShaderReloadListener

`SimplePreparableReloadListener`, регистрируется в `ClientSetup.onResourceReload`. При F3+T или смене ресурс-пака:

- Вызывает `IrisExtendedShaderAccess.invalidateShaderCache()`.
- Вызывает `IrisRenderBatch.invalidateAll()`.

Это нужно потому что shader instances пересоздаются (новые program IDs), и наши кэши (cachedShader, cachedLocations) станут invalid.

### 16.2 RegisterClientReloadListenersEvent (общий)

Дополнительный listener, который через `RenderSystem.recordRenderCall(...)` (на render thread!):

- `MachineAdvancedAssemblerRenderer.clearCaches()` — очистка 11 instanced renderer'ов.
- `MachineAssemblerRenderer.clearCaches()` — 4 renderer'а.
- `MachineHydraulicFrackiningTowerRenderer.clearCaches()` — 1 renderer.
- `DoorRenderer.clearAllCaches()` — frame caches + part caches + ColladaAnimationData cache + DoorVboRenderer cache + PARTS_WITHOUT_GEOMETRY.
- `MachinePressRenderer.clearCaches()` — Head instancer.
- `ChemicalPlantRenderer.clearCaches()` — Base + Frame instancers.
- `GlobalMeshCache.clearAll()` — все кэши геометрии.
- `AbstractObjArmorLayer.clearAllCaches()` — кэши брони.

**Критично**: всё на render thread, иначе `EXCEPTION_ACCESS_VIOLATION` (попытка удалить GL-объект с другого треда).

### 16.3 onClientDisconnect

Тот же набор `clearCaches()` при `ClientPlayerNetworkEvent.LoggingOut`. Чтобы не таскать VBO между мирами.

---

## 17. Power Armor (OBJ-броня) — отдельный pipeline

Файл: `com.hbm_m.powerarmor.layer.AbstractObjArmorLayer`

Параллельный pipeline для рендеринга OBJ-моделей брони на entity. Использует ту же `AbstractMultipartBakedModel`-инфраструктуру, но рендерит через `RenderType.armorCutoutNoCull(material.atlasLocation())` напрямую, без VBO/instancing.

Архитектура:

```
LivingEntityRenderer
  ↓
  RenderLayer<T, M> (наш AbstractObjArmorLayer)
    ↓
    renderSlot(HEAD/CHEST/LEGS/FEET)
      ↓
      ModelManager.getModel(modelLocation) → AbstractMultipartBakedModel
        ↓
        getCachedPart(multipart, location, "Helmet"/"Chest"/...) → BakedModel
          ↓
          renderPart(poseStack, buffer, light, partModel, parentBone, partName, crouching)
            ↓
            ВРУЧНУЮ применяет transform от bone (pivot delta + rotation)
            ↓
            emitAllQuads → putQuadManual для каждой квадры
```

`putQuadManual` — кастомная имплементация `putBulkData` с защитой от NaN (важно для broken OBJ-моделей) и нормализацией нормалей.

**Особенности:**

- Нет VBO — модели брони слишком мало частей и слишком много инстансов (по одной на entity slot).
- Нет Iris-special handling — `armorCutoutNoCull` rendering type Iris уже обрабатывает корректно.
- `MODEL_CACHE` + `PART_CACHE` — кэши модельных lookup'ов (избежать повторных `getModelManager().getModel(...)`).
- Pivot caching через `LinkedHashMap` с LRU-eviction (max 100 entries).

---

## 18. Производительность: профайлерные оптимизации

Этот раздел документирует все оптимизации, упомянутые в комментариях кода (большинство было обнаружено через профайлинг на 400-машинных фермах под BSL).

### 18.1 IrisRenderBatch (амортизация apply/clear)

**Проблема:** `ExtendedShader.apply()` это `GlFramebuffer.bind() + Iris CustomUniforms.push() + uploadIfNotNull` для каждого uniform — суммарно 63% frame time на больших фермах.

**Решение:** один `apply()/clear()` per BlockEntity вместо per-part. Открывается через try-with-resources:

```java
try (IrisRenderBatch batch = IrisRenderBatch.begin(shadowPass, projection)) {
    // 11 частей машины здесь — все шарят один apply/clear
}
```

**Эффект:** 3-6× FPS improvement под BSL.

### 18.2 Кэш shader locations

**Проблема:** `glGetUniformLocation` — синхронный driver round-trip. 22 запросов на кадр для `iris_ModelViewMatInverse` + `iris_NormalMat` × 11 part types.

**Решение:** `cachedLocModelViewInverse`, `cachedLocNormalMat`, `irisLocationsResolved` в `InstancedStaticPartRenderer`. Invalidation на смену shader instance.

### 18.3 Прямой glUniformMatrix4fv

**Проблема:** Mojang `Uniform.upload()` идёт через `RenderSystem.glUniformMatrix4 → GlStateManager._glUniformMatrix4 → GL20C.nglUniformMatrix4fv` — 8.67% frame time.

**Решение:** в hot loop `flushBatchIris` обходим Mojang stack: `GL20.glUniformMatrix4fv(loc, false, mvFloats)` напрямую.

**Эффект:** ~5% saved.

### 18.4 Skip recompute если ничего не изменилось

**Проблема:** `iris_NormalMat = transpose(inverse(MV))` — самая дорогая joml-операция в loop'е. Зависит ТОЛЬКО от rotation. Соседние машины часто шарят rotation (вся ферма смотрит север).

**Решение:** sentinel-check `lastQx/y/z/w`, recompute и upload только если rotation изменился.

**Эффект:** 5.53% на dense same-orientation фермах.

### 18.5 Skip glVertexAttribI2i при том же освещении

**Проблема:** `glVertexAttribI2i` для UV2 — driver call. Соседние машины часто имеют одинаковое освещение после smoothing.

**Решение:** `lastBlockU`, `lastSkyV` sentinel.

**Эффект:** 5.93% в dense fields.

### 18.6 LightSampleCache

**Проблема:** на ассемблере 11 частей — каждая делает `LevelRenderer.getLightColor` × 6 face centers = 66 lookup'ов на BE. На 400-машинной ферме = 26400 lookup'ов на кадр.

**Решение:** per-frame cache (`Long2ObjectOpenHashMap<float[2]>`) + fast-path single-slot cache.

**Эффект:** 17% frame time на dense scene.

### 18.7 OcclusionCullingHelper

**Проблема:** raycast-based culling — относительно дорого. Многие BE рендерятся **по нескольку раз за кадр** (например, AbstractPartBasedRenderer.render на каждую часть).

**Решение:** per-frame cache (`Long2ObjectOpenHashMap<Boolean>`). Один cull check на BE.

**Эффект:** 6.6% по profile.

### 18.8 visibleThisFrame для recipe icon

**Проблема:** `mc.getItemRenderer().renderStatic(...)` для recipe icon — full ItemRenderer pipeline на каждую видимую машину, даже если она потом будет occluded culling'ом.

**Решение:** flag `visibleThisFrame`, set'ится только после прохождения OcclusionCullingHelper. Icon рисуется только при `true`.

### 18.9 Batch sky darken

**Проблема:** `level.getSkyDarken(1.0f)` per-instance lookup.

**Решение:** один lookup per batch (на первом instance) → cached.

### 18.10 Lazy IrisCompanionMesh

Companion mesh строится только при первой попытке Iris rendering, чтобы не платить за конверсию формата если шейдер-пак не активен.

### 18.11 Reusable matrices

`matRing`, `matLower`, `matUpper`, `matHead`, `matSpike` — поля класса. `Matrix4f.identity()`, `.set(other)` вместо `new Matrix4f()` каждый кадр.

### 18.12 Reusable scratch buffers (Iris loop)

`irisMvFloats[16]`, `irisMvInverseFloats[16]`, `irisNormalMatFloats[9]`, `irisMvInverseTmp`, `irisNormalTmp`, `irisQuatTmp`, `irisSingleUV[2]` — все scratch буферы как поля. Zero allocation в hot loop.

### 18.13 DEG_TO_RAD constant

Заменяет `(float) Math.toRadians(deg)`. Math.toRadians — `double` operation + double→float cast на каждый call site.

### 18.14 Skip animation on far distance (LOD)

`shouldSkipAnimationUpdate(blockPos)`:

```java
double thresholdBlocks = ModClothConfig.modelUpdateDistance * 16.0;
return distanceSquared > thresholdBlocks * thresholdBlocks;
```

Дальние машины:

- В Advanced Assembler — рисуют только Ring без вращения.
- В Press — Head в rest позиции.
- В Chemical Plant — animation = 0.

### 18.15 RandomSource BAKE_SEED

`PartGeometry.BAKE_SEED = RandomSource.create(42L)` — детерминизм для квадов BakedModel. Без этого порядок может скакать между кадрами.

### 18.16 PARTS_WITHOUT_GEOMETRY blacklist

Кэш частей без квадов в `DoorRenderer` — не пытаемся создать VBO renderer для pivot-bone'ов.

---

## 19. Известные проблемы и обходные пути

### 19.1 Sodium 16-bit vertex coordinates

**Проблема:** Sodium хранит вершины с 16-bit integer координатами в chunk mesh. Модели > ~16 блоков в одном измерении могут переполниться.

**Решение:** `MachineHydraulicFrackiningTowerBakedModel.shouldSkipWorldRendering` всегда `true`. Вышка рисуется только через BER. Также `shouldRenderOffScreen = true` чтобы Vanilla frustum не обрезала.

### 19.2 Continuity FRAPI ForwardingBakedModel

**Проблема:** Continuity оборачивает все blockstate models в `CtmBakedModel extends ForwardingBakedModel`. Это:

1. Не передаёт `ModelData` в FRAPI emitBlockQuads → DoorBakedModel не видит skin selection.
2. На некоторых версиях Connector ломает blockstate-rotation.

**Решение:** `ClientSetup.onModelBakeUnwrapContinuity` (priority LOWEST) разворачивает обёртки для всех HBM-моделей через `unwrapFabricForwardingModels(...)`.

### 19.3 BSL/Complementary blockEntityId poisoning

**Проблема:** Pack-шейдеры читают `blockEntityId / 100` и переключаются в спец-ветки (155 = EMISSIVE_RECOLOR → solid red, 252 = DrawEndPortal). Iris обновляет этот uniform от последнего рендеренного BE — наш raw-GL батч получает leftover.

**Решение:** `IrisExtendedShaderAccess.setCurrentRenderedBlockEntity(0)` перед каждым `shader.apply()` + `restoreCurrentRenderedBlockEntity(prev)` в finally.

### 19.4 vaUV2 ivec2 attribute

**Проблема:** Pack-шейдеры объявляют `vaUV2 ivec2`. `glVertexAttrib2f` пишет в float-bank, который ivec2 attribute не читает → каждая машина получает leftover из int-bank (обычно 0 → pitch black).

**Решение:** `glVertexAttribI2i(loc, blockU, skyV)` (integer pipeline).

### 19.5 ExtendedShader iris_NormalMat / iris_ModelViewMatInverse

**Проблема:** Iris в `apply()` derives `iris_ModelViewMatInverse` и `iris_NormalMat` из `MODEL_VIEW_MATRIX` uniform в момент apply'я. Если мы вызываем apply() с identity и потом мутируем только `ModelViewMat` per-instance, derived matrices остаются identity → нормали трансформируются неправильно → broken geometry под BSL/Complementary/RV/Solas. Photon не использует NormalMat так же → там бага не было видно.

**Решение:** per-instance вычисление и upload через `iris`_* uniform names напрямую.

### 19.6 Texture pollution на shader.apply()

**Проблема:** `ExtendedShader.apply()` читает `RenderSystem.getShaderTexture(0..2)` и биндит к IrisSamplers. Если что-то ранее в кадре оставило не-atlas в slot 0 (Embeddium chunk re-bake, redstone particles), pack-шейдер сэмплит лажу → модель оранжевая (sampling из lightmap-текстуры).

**Решение:** перед apply() явно `RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS)` + `lightTexture.turnOnLightLayer()` + `overlayTexture.setupOverlayColor()`. И в finally восстанавливаем slot 0 в BLOCKS чтобы не загрязнять следующих.

### 19.7 Shadow pass без flush event

**Проблема:** `RenderLevelStageEvent.AFTER_BLOCK_ENTITIES` срабатывает только в main pass. Iris shadow renderer вызывает `addInstance` через Sodium injector, но flush никогда не приходит → инстансы потом рисуются в main pass с main projection = «призраки в небе».

**Решение:** `addInstance` детектит shadow pass через `ShaderCompatibilityDetector.isRenderingShadowPass()` и сразу делает `drawSingleWithIrisExtended` (через open IrisRenderBatch родительского BER для амортизации).

### 19.8 BufferBuilder iris$beginWithoutExtending

**Проблема:** Iris автоматически расширяет vertex format на `BufferBuilder.begin(...)`. Это ломает stride когда нам нужен исходный un-extended формат для контролируемой конверсии.

**Решение:** `IrisBufferHelper.beginWithoutExtending(...)` через reflection вызывает Iris-специфичный `iris$beginWithoutExtending`.

### 19.9 Cleaner для нативной памяти

**Проблема:** `MemoryUtil.memAlloc`* создаёт нативный буфер. Если просто хранить reference и надеяться на GC + finalize, можно словить утечку (Reference cleared, но native не освобождён).

**Решение:** `Cleaner.register(this, () -> MemoryUtil.nmemFree(addr))` где `addr` сохранён ДО регистрации (иначе lambda захватит buffer reference, что defeats Cleaner). Освобождение через `nmemFree(long addr)`, а не `memFree(buffer)`, чтобы избежать гонки с GC.

### 19.10 Door chunk invalidation

**Проблема:** При смене BlockState двери (открыта/закрыта) Sodium/Embeddium НЕ перезапекает chunk автоматически если `BakedModel.getQuads(...)` теперь возвращает другой набор. Дверь визуально остаётся в старом состоянии.

**Решение:** `DoorChunkInvalidationHelper.scheduleInvalidation(blockPos)` в момент state change → `levelRenderer.blockChanged(pos)` на следующем client tick.

### 19.11 EXCEPTION_ACCESS_VIOLATION при clearCaches

**Проблема:** Если `clearCaches()` вызывается во время активного render pass'а (например, при F3+T в момент когда машина рисуется), `glDeleteBuffers/VertexArrays` происходит в момент использования → SIGSEGV.

**Решение:** все `clearCaches()` обёрнуты в `RenderSystem.recordRenderCall(...)` — Mojang сам queue'ит выполнение на render thread в безопасный момент.

### 19.12 Animation delay при смене baked-rendering

**Проблема:** При окончании движения двери instant-switch на baked model вызывает мерцание (кадр между концом анимации и появлением baked geometry чанк не успевает перезапечься).

**Решение:** `DoorAnimationDelayHelper` откладывает фактический switch на 1-2 тика, чтобы Sodium успел подготовить chunk.

### 19.13 GLSL кэш Program.getOrCreate

**Проблема:** Mojang кэширует скомпилированную program по имени из JSON. Два ShaderInstance с одинаковым "vertex" получают одну и ту же программу — наш USE_INSTANCING молча игнорируется.

**Решение:** `ShaderPreDefinitions.wrapRedirect(provider, virtualName, realSource, modification)` — синтетический «virtual file», которого нет на диске, поэтому Program.getOrCreate видит уникальное имя и компилирует свежую программу.

### 19.14 FRAPI emitBlockQuads без ModelData

**Проблема:** Continuity (FRAPI) вызывает `emitBlockQuads()` без передачи Forge `ModelData`, поэтому DoorBakedModel.getPartsForModelData() не видит выбранного скина.

**Решение:** Continuity unwrap (см. §19.2).

---

## Приложения

### A. Полный список классов

#### Core rendering

- `com.hbm_m.client.render.AbstractGpuMesh`
- `com.hbm_m.client.render.AbstractPartBasedRenderer<TBE, TModel>`
- `com.hbm_m.client.render.GlobalMeshCache`
- `com.hbm_m.client.render.PartGeometry`
- `com.hbm_m.client.render.SingleMeshVboRenderer`
- `com.hbm_m.client.render.InstancedStaticPartRenderer`
- `com.hbm_m.client.render.IrisCompanionMesh`
- `com.hbm_m.client.render.ObjModelVboBuilder`

#### Shader infrastructure

- `com.hbm_m.client.render.ModShaders`
- `com.hbm_m.client.render.shader.ShaderCompatibilityDetector`
- `com.hbm_m.client.render.shader.IrisExtendedShaderAccess`
- `com.hbm_m.client.render.shader.IrisPhaseGuard`
- `com.hbm_m.client.render.shader.IrisRenderBatch`
- `com.hbm_m.client.render.shader.IrisBufferHelper`
- `com.hbm_m.client.render.shader.ShaderReloadListener`
- `com.hbm_m.client.render.shader.modification.ShaderModification`
- `com.hbm_m.client.render.shader.modification.ShaderPreDefinitions`

#### Helpers

- `com.hbm_m.client.render.OcclusionCullingHelper`
- `com.hbm_m.client.render.LightSampleCache`
- `com.hbm_m.client.render.DoorChunkInvalidationHelper`
- `com.hbm_m.client.render.LegacyAnimator`
- `com.hbm_m.client.overlay.DoorAnimationDelayHelper`

#### BakedModels

- `com.hbm_m.client.model.AbstractMultipartBakedModel`
- `com.hbm_m.client.model.MachineAdvancedAssemblerBakedModel`
- `com.hbm_m.client.model.MachineAssemblerBakedModel`
- `com.hbm_m.client.model.MachineHydraulicFrackiningTowerBakedModel`
- `com.hbm_m.client.model.ChemicalPlantBakedModel`
- `com.hbm_m.client.model.PressBakedModel`
- `com.hbm_m.client.model.DoorBakedModel`
- `com.hbm_m.client.model.MachineFluidTankBakedModel` *(без BER, статика с реактивным ModelData)*
- `com.hbm_m.client.model.LeavesModelWrapper`
- `com.hbm_m.client.model.ModelHelper`

#### BlockEntityRenderers

- `com.hbm_m.client.render.implementations.MachineAdvancedAssemblerRenderer`
- `com.hbm_m.client.render.implementations.MachineAdvancedAssemblerVboRenderer`
- `com.hbm_m.client.render.implementations.MachineAssemblerRenderer`
- `com.hbm_m.client.render.implementations.MachineAssemblerVboRenderer`
- `com.hbm_m.client.render.implementations.MachineHydraulicFrackiningTowerRenderer`
- `com.hbm_m.client.render.implementations.MachineHydraulicFrackiningTowerVboRenderer`
- `com.hbm_m.client.render.implementations.ChemicalPlantRenderer`
- `com.hbm_m.client.render.implementations.ChemicalPlantVboRenderer`
- `com.hbm_m.client.render.implementations.MachinePressRenderer`
- `com.hbm_m.client.render.implementations.MachinePressVboRenderer`
- `com.hbm_m.client.render.implementations.DoorRenderer`
- `com.hbm_m.client.render.implementations.DoorVboRenderer`

#### Model loaders (geometry loaders)

- `com.hbm_m.client.loader.MachineAdvancedAssemblerModelLoader`
- `com.hbm_m.client.loader.MachineAssemblerModelLoader`
- `com.hbm_m.client.loader.MachineHydraulicFrackiningTowerModelLoader`
- `com.hbm_m.client.loader.ChemicalPlantModelLoader`
- `com.hbm_m.client.loader.PressModelLoader`
- `com.hbm_m.client.loader.DoorModelLoader`
- `com.hbm_m.client.loader.MachineFluidTankModelLoader`
- `com.hbm_m.client.loader.TemplateModelLoader`
- `com.hbm_m.client.loader.ProceduralWireLoader`
- `com.hbm_m.client.loader.ColladaAnimationData`
- `com.hbm_m.client.loader.ColladaAnimationParser`

#### Power Armor

- `com.hbm_m.powerarmor.layer.AbstractObjArmorLayer`
- `com.hbm_m.powerarmor.layer.IArmorLayerConfig`

#### Setup & Events

- `com.hbm_m.client.ClientSetup`
- `com.hbm_m.client.ClientRenderHandler`
- `com.hbm_m.event.ClientModEvents`

### B. GLSL шейдеры

- `assets/hbm_m/shaders/core/block_lit.vsh` — общий vertex shader (с/без USE_INSTANCING).
- `assets/hbm_m/shaders/core/block_lit.fsh` — общий fragment shader.
- `assets/hbm_m/shaders/core/block_lit_simple.json` — config для non-instanced (vertex name "hbm_m:block_lit").
- `assets/hbm_m/shaders/core/block_lit_instanced.json` — config для instanced (vertex name "hbm_m:block_lit_instanced", виртуальный → подменяется через wrapRedirect).
- `assets/hbm_m/shaders/core/thermal_vision.vsh/.fsh/.json` — для thermal vision overlay (вне фокуса этого документа).

### C. Конфиг-флаги (ModClothConfig)


| Имя поля                      | Имя метода-аксессора          | Default    | Что делает                                                                                                                                                                                             |
| ----------------------------- | ----------------------------- | ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `useIrisExtendedShaderPath`   | `useIrisExtendedShaderPath()` | `**true`** | Под активным шейдер-паком рендерить через нашу VBO/Iris ExtendedShader-систему. При `false` — fallback на legacy путь baked + putBulkData. **Сам по себе ничего не делает без активного шейдер-пака.** |
| `useInstancedStaticRendering` | `useInstancedBatching()`      | `true`     | Включает batching через `InstancedStaticPartRenderer`. При `false` — рендер per-call через `SingleMeshVboRenderer`.                                                                                    |
| `modelUpdateDistance`         | (поле)                        | (?)        | Порог в чанках, после которого включается LOD-skipping animation.                                                                                                                                      |
| `useColladaDoorAnimations`    | (поле)                        | `true`     | Включает DAE-анимации дверей.                                                                                                                                                                          |
| `useColladaZUpConversion`     | (поле)                        | `true`     | Конверсия Blender Z-up → Minecraft Y-up.                                                                                                                                                               |
| `doorAnimatedPartBrightness`  | (поле, 50..100)               | `88`       | Яркость анимированных частей двери в legacy-shader пути (множитель / 100).                                                                                                                             |
| `vatsRenderDistanceChunks`    | (поле, 1..32)                 | `7`        | Дистанция рендера VATS overlay в чанках (вне фокуса этого документа).                                                                                                                                  |
| `enableDebugLogging`          | (поле)                        | `false`    | Verbose логи для render-системы.                                                                                                                                                                       |


**Метод-удобство `ShaderCompatibilityDetector.useNewIrisVboPath()`** — это **НЕ** имя конфига, это вычисляемое значение `isExternalShaderActive() && ModClothConfig.useIrisExtendedShaderPath()`. Используется в коде BER для проверки «мы под шейдером и идём через новый Iris extended путь».

### D. Decision Matrix (полная)

Для основных машин (Advanced Assembler, Assembler, Chemical Plant, Door):


| Состояние                                          | `BakedModel.getQuads` для блок-state            | BER рендерит                                                                        | Шейдер геометрии                               |
| -------------------------------------------------- | ----------------------------------------------- | ----------------------------------------------------------------------------------- | ---------------------------------------------- |
| Нет шейдера, idle                                  | empty                                           | Base/Frame через `InstancedStaticPartRenderer` (анимированные пропускаются)         | `block_lit_instanced`                          |
| Нет шейдера, active                                | empty                                           | Base/Frame + animated через `InstancedStaticPartRenderer` / `SingleMeshVboRenderer` | `block_lit_instanced`                          |
| Iris + `useIrisExtendedShaderPath = true`, idle    | empty                                           | Base/Frame через инстансинг (анимированные пропускаются)                            | Iris `ExtendedShader` + `IrisCompanionMesh`    |
| Iris + `useIrisExtendedShaderPath = true`, active  | empty                                           | Base/Frame + animated через инстансинг                                              | Iris `ExtendedShader` + `IrisCompanionMesh`    |
| Iris + `useIrisExtendedShaderPath = false`, idle   | Base + Frame + idle animated parts → chunk mesh | НИЧЕГО                                                                              | (Iris через chunk render)                      |
| Iris + `useIrisExtendedShaderPath = false`, active | Base + Frame → chunk mesh                       | Только active animated через `putBulkData` (RenderType.solid)                       | (Iris через chunk render + Sodium/Iris bridge) |


Особые случаи:


| Машина / случай                            | `BakedModel.getQuads` для блок-state                                   | BER рендерит                                                                       | Шейдер геометрии                                                |
| ------------------------------------------ | ---------------------------------------------------------------------- | ---------------------------------------------------------------------------------- | --------------------------------------------------------------- |
| **Fluid Tank (любой режим)**               | **ВСЕГДА** Frame + Tank с retexturing по `ModelData`                   | НЕТ BER                                                                            | Стандартный chunk render Sodium/Iris                            |
| **Press (любой режим)**                    | **ВСЕГДА** Base                                                        | Только Head (через основное дерево решений)                                        | Стандартный chunk render для Base, основное дерево для Head     |
| **Hydraulic Fracking Tower (любой режим)** | **ВСЕГДА** empty                                                       | ВСЁ (Cube_Cube.001) через основное дерево                                          | По основному дереву (нет legacy fallback'а)                     |
| **Chemical Plant fluid (любой режим)**     | empty (часть `Fluid` намеренно НЕ запекается)                          | Через `RenderType.translucent` с UV scrolling                                      | Стандартный translucent pass                                    |
| **Door movement**                          | Только frame в chunk mesh / либо empty в зависимости от useVboGeometry | Все остальные части через VBO + DAE/процедурные transform'ы                        | По основному дереву                                             |
| **Shadow pass**                            | n/a (chunk render не идёт в shadow)                                    | per-instance draw через `drawSingleWithIrisExtended` (избегаем «призраков в небе») | Iris shadow `ExtendedShader` через persistent `IrisRenderBatch` |


---

*Этот документ описывает систему рендеринга OBJ-моделей в HBM Modernized по состоянию на 19 апреля 2026. Любые добавления новых машин должны следовать тем же паттернам: AbstractMultipartBakedModel + AbstractPartBasedRenderer + регистрация в ClientSetup + добавление в `ClientModEvents.flushInstancedBatches`.*