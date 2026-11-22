# Nucleus GPU-First Instanced Render Engine Architecture & Refactoring Overview

> Complete architectural reference for the grand overhaul of the machine-rendering stack into Nucleus — a declarative, GPU-first, retained-mode instanced engine across Minecraft 1.20.1 Forge and 1.21.1 NeoForge.

Grand architectural overhaul of the machine-rendering stack into Nucleus — a declarative, GPU-first, retained-mode instanced engine — accompanied by an extensive Iris/Oculus shader compatibility layer, GLSL compute and core shaders, multiloader platform plumbing, cross-version mixins, sound physics re-anchoring for moving airships, fleet-wide server synchronization and recipe caching optimizations, zero-overhead OpenGL KHR_debug instrumentation, third-party mod compatibilities (Flashback replay and Simulated diagram capture), and build toolchain modernizations.

This change is bundled deliberately as one cohesive architectural commit across both supported targets (Minecraft 1.20.1 Forge and 1.21.1 NeoForge). It replaces legacy per-frame immediate MatrixStack rendering and ad-hoc static instance caches with an integrated GPU pipeline delivering orders-of-magnitude reductions in CPU dispatch overhead, draw calls, and network synchronization spikes across large machine fleets.

---

## 1. Declarative Engine Architecture & Kinematics DSL

- **Declarative Machine Specification DSL**:
  `MachineRenderers` and `MachineSpecBuilder` provide a declarative kinematics DSL where each machine registers its entire visual configuration in a single spec file without modifying any BlockEntity classes (~25 machines migrated). The DSL declaratively derives:
  * The GPU kinematic rig and joint hierarchy.
  * An analytical CPU fallback animator guaranteeing bit-identical pose reconstruction by construction.
  * Dynamic animation epochs for automatic state stability evaluation.
  * Item-renderer part definitions for GUI and handheld rendering.
  * Base class `MachineBer` extends `AbstractPartBasedRenderer` and unifies the common rendering lifecycle across all machines.
- **Request Record Reentrancy**:
  `PartEnqueueRequest` introduces an explicit, thread-local per-enqueue request record that completely replaces former global static `pending*` fields in `InstancedStaticPartRenderer`. This eliminates the entire bug class of animated parts teleporting across the world caused by exceptions interrupting the enqueue sequence.
- **Consolidated Machine Render State**:
  `MachineRenderState` consolidates five formerly fragmented, slot-collision-prone staleness caches in `MachineBer` (roster cache, param-word cache, animation-epoch ring arrays, and animation-delta ring arrays) into a single, clean object keyed by `blockPos.asLong()`. This resolves the F3 "Frozen: 0/100%" see-saw bug and allows clean, O(1) removal on machine destruction.
- **Item Model Framing Filter**:
  `ConfiguredMultipartBakedModel.getItemRenderPartNames()` explicitly filters out model parts named `"Frame"` (`!isFramePart(name)`). This prevents multiblock scaffolding and outer framing structures from rendering inside handheld items or inventory GUI models.
- **Dynamic Hardware Tier Management**:
  `HardwareTierManager` evaluates GPU capabilities at startup and orchestrates dynamic runtime degradation without crashing headless environments or macOS:
  * Tier 1 (Modern OpenGL 4.3+): Multi-Draw Indirect (MDI), GPU compute frustum and occlusion culling, SSBO instance storage, Hi-Z depth pyramid, and persistent staging ring uploads.
  * Tier 2 (Baseline OpenGL 3.2 Core): Core instanced draws (`glDrawElementsInstanced`), CPU Gribb-Hartmann frustum culling, and dynamic streaming VBOs via `glBufferSubData`. Guaranteed zero compute or SSBO calls.
- **Lazy Per-Part GL & CPU Allocation (`InstancedStaticPartRenderer`)**:
  Defers per-renderer VAO/VBO/EBO creation (`ensureOwnGlObjects`) so pure Tier 1 MDI parts consume zero duplicate mesh VRAM outside `MdiGeometryAtlas`. Per-slot CPU metadata arrays and off-heap staging buffers start at 64 instances and grow geometrically via `ensureCpuInstanceCapacity`.

---

## 2. Retained Scene Graph, Light Sampling & Dirty Tracking

- **Retained Instance Graph**:
  `RetainedInstanceGraph` coordinates persistent GPU instance lifetimes with per-chunk spatial partitioning (`ChunkInstancePartition`), bounds tracking, and an asynchronous lock-free handoff queue (`ConcurrentLinkedQueue<Runnable>`) guaranteeing thread safety across off-thread chunk loading, network packet decoding, and block updates.
- **Instance Slot Allocator**:
  `InstanceSlotAllocator` implements a pure-CPU O(1) bitmap bitscan slot allocator (`nextClearBit` lowest-free-slot reuse instead of FIFO) with high-water mark compaction and 64-bit position keying down to Y = -64. Removed unused GPU backing/compacted buffers that previously pinned dead VRAM.
- **Multi-Part Leak Remediation**:
  `partSlotsByBlockKey` tracks all persistent slots (parts 0..N-1) per machine coordinate, ensuring multi-part machines cleanly release all allocated GPU slots upon machine removal rather than leaking secondary part slots.
- **Sparse Dirty Tracking**:
  `SparseUpdateQueue` and `DirtyMask` track granular 128-byte record slices (TRANSFORM, BOUNDS, LIGHT, UV, COLOR, GRAD, ANIM, or ALL) so that only modified instance data is streamed to the GPU.
- **Amortized Ghost Reconciliation**:
  An amortized ghost-handle sweep on the render thread cleans up orphaned handles within a bounded per-frame budget (`GHOST_SWEEP_BUDGET = 64`), preventing memory leaks and stale instance retention.
- **Coordinate Origin & Anchor Rebasing**:
  `RetainedInstanceGraph.onRenderOriginChanged(int deltaX, int deltaY, int deltaZ)` and `InstancedStaticPartRenderer.rebaseAnchor` update retained instance anchor coordinates using integer delta arithmetic when the player or camera crosses world origin thresholds, eliminating the need to deallocate slots or re-bake geometry.
- **Block Light Bug Remediation & Exterior Sampler (`LightSampleCache`)**:
  * Root Cause Analysis: In earlier implementations, models and machines reacted to sky light but completely ignored block light from torches, lanterns, and glowstone, rendering pitch-black at night and underground. The audit uncovered three primary causes:
    1. Flawed `SAMPLE_INSET = 1/64`: pulled corner coordinates inward into the machine's bounding box, collapsing all 8 sample points into the interior of solid casing blocks where `BlockLightEngine` evaluates block light as 0.
    2. Opaque Block Fallback Loop: when hitting a solid block, `sample8` fell back to `be.getBlockPos()`—the same opaque machine block with 0 block light.
    3. Premature Early-Exit in `shouldSkipSpatialSampling`: when fallback block light was 0, it filled all 8 corners with flat 0 block light, discarding valid surrounding light.
    4. Chunk Section Overwrites: `RetainedInstanceGraph.processDirtyLightSections` sampled light at the geometric section center and overwrote all machines.
  * Comprehensive Remediation:
    1. Exterior Corner Probing: `LightSampleCache.sample8` probes outward by `+0.1` blocks past each bounding box corner into adjacent exterior air.
    2. Candidate Relaxation: When hitting a solid floor or wall block, the sampler tests ground-level air (`wy = bpy`), relaxes X/Z coordinates inward, and checks the machine's actual corner block cell.
    3. Exterior Face Smoothing Fallback: `sampleSmoothLightUV` probes all 6 open faces around the machine if solid blocks are encountered.
    4. Non-Darkening Invariant: Sky light is strictly prevented from collapsing below fallback sky light when exposed to sky.
    5. Elimination of Structure Traversal: Removed the expensive per-multiblock part-block aggregation loops that previously caused a 4-6x overhead spike on cache misses.
    6. Section-to-Machines Mapping: `RetainedInstanceGraph` maintains `sectionToMachines`, ensuring chunk section light updates selectively queue individual machine light rebakes (`DirtyMask.LIGHT`).

---

## 3. GPU Compute Bake Pipeline & Multi-Draw Indirect

- **Tier 1 Compute Bake**:
  `NucleusGpuBaker` provides a pack-agnostic GPU compute bake pipeline ("instancing without instancing") using OpenGL 4.3 compute shaders. It expands machine state records into the active shaderpack's full extended vertex format (transforms, 8-corner trilinear lightmap, midTexCoord, tangent, and midBlock), performing on-the-fly frustum and Hi-Z culling during expansion.
- **Draw Call Collapse**:
  Meshes render via the active shaderpack's own native program (pack shadow or gbuffers) using a shared global batch pool per pass (shared output SSBO + shared EBO + shared indirect command buffer). The flush issues a single `glMultiDrawElementsIndirect`, or `glMultiDrawElementsIndirectCount` on GL 4.6 / ARB_indirect_parameters hardware, collapsing thousands of companion draws into a handful of indirect dispatches without CPU visible-count readbacks.
- **Camera-Independent Caching**:
  Compute bake output is camera-independent: main-pass records are anchor-relative (`FrameViewState`) and shadow-pass records are light-space quaternion/position decompositions. Unchanged records skip both CPU upload and compute expansion via memcmp checks, backed by a 192 MB budgeted VRAM LRU cache (`nucleusBakeCacheMB`).
- **Multi-Draw Indirect Batch Coordination (`MdiBatchCoordinator`)**:
  `MdiBatchCoordinator` manages vanilla and core batch assembly and submission into `MdiGeometryAtlas`. It constructs `DrawElementsIndirectCommand` records, extracts the active gbuffer depth texture (`getActiveGbufferDepthTexture`) for Hi-Z occlusion passes, applies GPU distance fade ramp uniforms, and guarantees leak-free state teardown (`GlVaoSafety.bindVertexArray(0)`, `BufferUploader.invalidate()`, and `ShaderBindResync.invalidateStaticProgramCache()`).
- **Managed GL State Snapshotting & Symmetrical Restoration (`RenderStateGuard` & `GlVaoSafety`)**:
  `RenderStateGuard.snapshot()` replaces static singleton reset assumptions with dynamic, per-scope queries of active VAO, array buffer, cull, depth test/mask/func, and blend modes. Restoration routes exclusively through managed `RenderSystem` APIs to maintain `GlStateManager` cache coherence, while `GlVaoSafety.bindVertexArray` explicitly invalidates vanilla's immediate-draw cached VAO handle on zero unbind.
- **Automated Degradation Chain**:
  Rendering falls back smoothly across tiers per part: MDI -> Instanced -> Per-BE VBO -> Vanilla Immediate. NOTE: Compute bake and GPU parametric animation deliberately do not compose; bake-enabled configurations run CPU animators to guarantee correct kinematics.
- **Pack Shader State & First-Person Hand Preservation**:
  `NucleusGpuBaker.presentMainBatch` restores `RenderSystem` projection/model-view matrices on the active pack program before calling `packShader.clear()` and `BufferUploader.invalidate()`, preventing `-relCam` anchor offsets from leaking into subsequent first-person hand and weapon renders (e.g., Superb Warfare).
- **Mass-Spawn Compute Workgroup Tiling (>65,535 Instances)**:
  `buildInstancesMain`, `buildInstancesShadow`, and `buildEbo` implement 2D/3D workgroup expansion (`rawY = gl_GlobalInvocationID.y + gl_GlobalInvocationID.z * gl_NumWorkGroups.y`) and tiled dispatch triples (`min(n, 65535u)` and `(n + 65534u) / 65535u`). This bypasses the OpenGL 65,535 Y-workgroup dispatch ceiling, preventing GPU timeouts, freezes, and device loss during massive fleet benchmarks.
- **Dynamic VRAM Pool Shrinking & Cleanup (`maybeShrinkPool`)**:
  Pool buffers scale geometrically during spawn bursts and are dynamically downsized (`maybeShrinkPool` and `shrinkOversizedBuffer`) during quiescent periods via `tickFrameCleanup` hooked to `InstancedRenderFrame.endFrame()`. Strict user budget enforcement (`cacheBudgetBytes`) eliminates artificial floors.
- **Pipeline Stall Elimination in Hi-Z Culling**:
  `GL11.glGetInteger(GL_TEXTURE_BINDING_2D)` on texture unit 10 in `NucleusGpuBaker.runCull()` is gated behind `bakeHiZPlanned(mode)`, eliminating synchronous GPU pipeline stalls when Hi-Z culling is inactive.
- **Thread Safety & Bone Palette Clamping**:
  Synchronized access to `BAKED` companion mesh registries and clamped bone matrix uploads (`BONE_PALETTE_SIZE = 64`) with buffer limit validation protect against off-thread concurrency hazards and buffer overflows.

---

## 4. GPU & CPU Culling, Hi-Z Occlusion, and Bounds Optimization

- **Instance-Major Compute Culler**:
  `nucleus_cull.comp` executes frustum and Hi-Z depth pyramid occlusion culling. Reworked from command-major to global instance-major compaction over all commands (`uTotalInstances`) with binary-searched command offsets.
- **Hardware Subgroup Ballot Compaction**:
  Supports both 32-lane warps (NVIDIA/Intel) and 64-lane wavefronts (AMD Wave64). Corrected subgroup leader election to match `subgroupBallotFindLSB`, fixing an alternating-frame slot 0 compaction race that previously caused visual flickering and part displacement.
- **Hierarchical Z-Pyramid**:
  `HiZDepthPyramid` builds an SPD-style depth pyramid from scene depth using `downsample.glsl` and `downsample_second.comp`, with sky-aware downsampling and Distant Horizons compatibility.
- **Distant Horizons Sky Discard**:
  `dh_depth_blit.fsh` discards reverse-Z sky samples beyond the far plane (`reverseZ && d <= 0.5`) to prevent degenerate depth decodes.
- **Collect-Time GPU Cull Coverage**:
  `GpuCullCoverage` signals when the GPU culler owns visibility for the frame, allowing redundant CPU frustum tests in `MachineBer` to stand down and preventing CPU-side collection bottlenecks.
- **Central Render Bounds Cache**:
  `RenderBoundsCache` provides a centralized, thread-safe cache for BlockEntity render bounding boxes, evicting stale entries across world generations and registering with `RenderCacheManager` for cleanup on world exit and resource reloads. Eliminates the 8.8% CPU time previously spent traversing multiblock structure maps.

---

## 5. Core Shaders, Compute Uploads, and Mathematical Precision

- **Compute Scatter Uploader**:
  `upload_scatter.comp` batches many small staging-ring copies into a single compute dispatch. Reorganized workgroups into 4 ops per 64-lane workgroup (16 lanes per op), eliminating lane idling on 32-word records and 4/5-word light and transform slices.
- **Direct glBufferSubData Span Uploads (`GpuSpanUploader`)**:
  `GpuSpanUploader.diffUpload` scans changed words between CPU source and shadow mirror buffers using raw off-heap integer comparisons (`MemoryUtil.memGetInt`). It coalesces adjacent modifications within an instance record (gaps <= 8 floats, e.g. scattered light words) while strictly preventing coalescing across different instance records. Span delivery uses direct `glBufferSubData` with single-bind window batching (`glBindBuffer(GL_ARRAY_BUFFER, destVbo)` once per window), completely eliminating fencing waits, staging ring sync stalls, and redundant per-span buffer re-binding.
- **Instanced Core Vertex Shaders**:
  `block_lit_instanced.vsh`, `block_lit_instanced_iris.vsh`, and `block_lit_instanced_packed.vsh` incorporate snorm16 packed quaternions, u8 corner lightmaps (decoded via `x 240.0`), safe bounding box epsilon guards, and fail-closed gradient axis evaluation.
- **Fragment Shaders & Emissive Glow**:
  `block_lit.fsh` and `block_lit_iris.fsh` flip the default glow factor to 1.0, ensuring uniformly heated machine parts retain full emissive brightness without requiring gradients. Implements vanilla-compliant directional shading.
- **IEEE 754 Binary16 Codec**:
  `HalfFloat` implements a hand-crafted binary16 half-float codec supporting round-to-nearest-even, subnormals, and NaNs on the Java 17 toolchain, packing 8 parametric joint parameters into 4 float record words (attrib 15).
- **Cardinal Orientation Math**:
  `CardinalOrientation` precomputes cardinal quaternions, rotation matrices, and packed snorm16 quaternion words (`CARDINAL_PACKED_QUAT_WORDS`) for all 6 directions, completely eliminating runtime trigonometry and PoseStack churn.
- **Zero-Allocation Geometry Processing (`ClusterSubmunitionMesh`)**:
  Refactors cluster submunition OBJ rendering to emit vertices via `RenderHooks.vertexFull` and replaces heap `org.joml.Vector3f` cross-product allocations in `faceNormal` with unboxed scalar float arithmetic, eliminating per-frame GC pressure during missile detonations.

---

## 6. Iris / Oculus Compatibility Layer & Shadow Engine

- **1.21.1 Shadow Dispatch Restoration**:
  `IrisShadowBeFunction121` restores missing block entity shadow dispatch on Iris 1.21.1 + Sodium. Because upstream Iris abandoned the setter for `BlockEntityRenderFunction` on 1.21.1, this class reflectively installs a dynamic `Proxy` wrapper on the first shadow pass, re-enabling shadow casting for all Nucleus machines.
- **NT Direct Particle Pass & Re-entrant Nesting**:
  `NtDirectPass` overcomes shaderpack translucent pipeline overrides (such as Photon disabling blending on translucent gbuffers) for nuclear mushroom clouds (`NukeTorex`, `NukeExplosionRings`), gas flames, cooling tower steam, and ash particles. Implements re-entrant `NtDirectPass.run(Runnable)` nesting with an internal counter, guaranteeing that intermediate batch flushes issued by vanilla `PlainBufferSource.endCurrentBatch()` when switching particle render types still execute with unmasked buffers and pack FBO attachments.
- **Reflective DepthColorStorage Unlocking (`ShaderBindResync`)**:
  `ShaderBindResync` and `IrisReflectionCompatProbe` reflectively interface with Iris/Oculus `DepthColorStorage` (`isDepthColorLocked`, `unlockDepthColor`, and `depthColorLocked` field access). When Oculus's `MixinShaderInstance.onTail` locks depth/color writes (`glColorMask(false, false, false, false)`), `forceIrisDepthColorEnabledQuiet()` unlocks the state without clobbering the `RenderType`'s configured depth mask, restoring full particle and mushroom cloud visibility.
- **Streamlined Particle Shaders (`nuke_cloud` & `nuke_add`)**:
  Stripped out obsolete Distant Horizons depth sampling logic, uniforms, and samplers (`Sampler1`, `DhProjMat`, `Fog*`, `DhDepthTest`, `DhViewport`) from `nuke_cloud.fsh`, `nuke_cloud.vsh`, and associated json descriptors, preventing texture binding collisions and pipeline stalls.
- **Anchor-Space Shadow Activation & O(1) Bypass**:
  Activated anchor-space shadow prebakes on 1.20.1 via `ensureAnchorSpaceShadowFromBePose()`. `IrisShadowBatchCollector.tryConsumePrebakedShadow()` leverages `NucleusTrackedBlockEntity` O(1) registration and `RenderDirtyTracker` checks for non-dynamic machines, eliminating 3 per-machine map lookups. Idle collector entries are shrunk (`shrinkOversizedBuffer`) and stale `PREBAKES` (>24 epochs) are cleanly freed via `MemoryUtil.memFree`.
- **Fast Rigid-Body Matrix Math & Instanced Shadow Fallback**:
  `IrisInstancedBatchRenderer` and `IrisRenderBatch` use `invertAffine()` and dot-product translation updates when rotation is static, avoiding full matrix inversions. `IrisShadowBatchCollector` adds a high-performance instanced shadow bake fallback (`bake-fallback-instanced`) before falling back to companion batches.
- **Core Shader `nt_color_tex_lightmap`**:
  A namespaced clone of vanilla `position_color_tex_lightmap` allowing NT particles to bind a recognized mod shader instance that survives Iris masking.
- **Shader Compiler Diagnostics**:
  `ShaderCompileDiagnostics` recovers the real GLSL compilation and link logs when Oculus 1.8.x destroys driver diagnostic text via an invalid `CancellationException` throw.
- **Reflective Hook Validation**:
  `IrisReflectionCompatProbe` performs a one-shot startup audit of all reflective Iris and Oculus internal hooks, emitting clear diagnostic logs identifying any degraded compatibility hooks.
- **MRT G-Buffer Encoder**:
  `IrisInstancedEncoders` and `IrisInstancedBatchRenderer` classify shaderpacks and route conventional packs through a 4-target MRT G-buffer encoder.
- **SHADOW-EPOCH Basis Calibration Fix**:
  Corrected column-major rotation multiplication (`PREBAKE_R_CAND.transform`) in `IrisShadowBatchCollector.solveShadowBasis` and enforced per-flush reset of `slotPosQuatVerified` flags in `InstancedStaticPartRenderer.flush`, eliminating shadow displacement when multi-part animated machines calibrate the shadow basis. Static poses retain verified status to allow shadow basis calibration.

---

## 7. Zero-Overhead OpenGL KHR_debug Instrumentation & RenderDoc Manifests

- **Zero-Overhead Production Elimination**:
  `com.hbm_m.client.render.debug.GlDebugMarkers` gates all debug markers behind `public static final boolean ENABLED = Boolean.getBoolean("hbm.glDebug");`, guaranteeing complete JIT dead-code elimination and zero JVM/driver overhead in standard production builds.
- **Off-Heap String Passing & Lifecycle Guards**:
  Debug strings pass through LWJGL `MemoryStack` off-heap buffers with UTF-8 conversions, label length truncations, and strictly balanced push/pop debug group lifecycles protecting against stack unbalanced errors.
- **Component-Level Debug Tagging**:
  Explicit debug tags attached to `IrisCompanionMesh`, `NucleusGpuBaker.BakedMesh`, and `IrisShadowBatchCollector.Entry` (`crucible#body`, `heating_oven#door`, `press#ram`, etc.).
- **Comprehensive GPU Object Labelling**:
  `glObjectLabel` applied across all major GL objects:
  * `NucleusGpuBaker`: compute programs, uniform buffers, SSBO instance tables, VAOs, and shared indirect pools.
  * `MdiGeometryAtlas`: VAOs, vertex buffers, element buffers, instance buffers, compacted instance buffers, and indirect command buffers.
  * `PersistentUploadStaging`: persistent mapped staging rings.
  * `HiZDepthPyramid`: depth pyramid mip texture arrays and framebuffers.
- **Hierarchical RenderDoc Groups & Sub-Draw Markers**:
  Structured RenderDoc groupings (`HBM_ShadowPass_Flush`, `HBM_Shadow_BakeCollect`, `HBM_Main_BakeEntry_<tag>`, `HBM_Vanilla_MDI_Dispatch`, `HBM_MDI_GpuHiZCull`) with pre-draw manifest markers (`insertMarker`) embedding mesh names, instance counts, vertex/EBO offsets, dirty spans, and buffer generation stamps directly before MDI and indirect dispatches.
- **Synchronous Driver Message Probing & Rate Limiting (`GlDebugProbe`)**:
  `com.hbm_m.client.render.GlDebugProbe` installs a synchronous `GLDebugMessageCallback` for OpenGL 4.3+ KHR_debug environments, enabled via `ModClothConfig.get().glDebugOutput` and toggleable via `-Dhbm.glDebug=0` or `HBM_GL_DEBUG=0`. Implements high-rate storm suppression via `highRateAllow` (limiting repeated HIGH-severity error logs to 1 per second with 5-second periodic summaries to prevent driver log freezes) and captures call-site exceptions alongside runtime snapshots of active VAO, shader program, and draw framebuffer bindings (`describeGlState`).

---

## 8. Complete Mixin Architecture & Injections

All 11 new client mixin registrations in `mixins.hbm_m.json` and modified mixins are implemented and accounted for:
1. `client.VertexBufferMixin`:
   Injects into `VertexBuffer._drawWithShader` immediately following `ShaderInstance.apply()`. Re-binds the active pack pipeline FBO captured by `NtDirectPass`, forces `GL11.glColorMask(true, true, true, true)` to override Oculus `DepthColorStorage` masking, invokes `ShaderBindResync.forceIrisDepthColorEnabledQuiet()`, and verifies the active GL program against `ShaderInstanceAccessor.hbm$getProgramId()`, recovering from zeroed program states.
2. `client.ShaderInstanceAccessor`:
   Mixin accessor exposing private `programId` from `ShaderInstance` for runtime program binding validation in `VertexBufferMixin`.
3. `client.MixinLevelChunkSetBlockEntity`:
   Injects at the tail of `LevelChunk.setBlockEntity` to trigger `NucleusDispatcherBypass.onMachineAdded(blockEntity)`, providing transactional presence tracking on block placement and chunk loading.
4. `client.MixinBlockEntitySetRemoved`:
   Injects at the tail of `BlockEntity.setRemoved` to trigger `NucleusDispatcherBypass.onMachineRemoved(be)`, providing transactional eviction on block destruction and chunk unloading.
5. `client.MixinBlockEntityType`:
   Implements duck interface `NucleusManagedType` on `BlockEntityType`, providing an O(1) field check (`hbm_m$isNucleusManaged()`) that replaces per-BE-per-frame HashSet lookups in the dispatcher bypass.
6. `client.MixinBlockEntity`:
   Implements duck interface `NucleusTrackedBlockEntity` on `BlockEntity`, attaching an O(1) registration flag (`hbm_m$isNucleusRegistered()`).
7. `client.MixinClientChunkCacheLight`:
   Injects at the head of `ClientChunkCache.onLightUpdate(LightLayer, SectionPos)`, pushing dirty section coordinates to `LightSampleCache.onSectionLightUpdate()` and `RetainedInstanceGraph.onSectionLightDirty()` for event-driven light propagation.
8. `client.SodiumRenderBoundsMixin`:
   `@Pseudo` mixin targeting `me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer`, redirecting `IForgeBlockEntity.getRenderBoundingBox()` in `renderBlockEntities` to `RenderBoundsCache.get(be)`, eliminating structure map traversals.
9. `client.SodiumWorldRendererMixin`:
   1.20.1 Forge / Embeddium early cancellation in `SodiumWorldRenderer.renderBlockEntity`. Enforces pass-aware culling for Embeddium 0.3.x. On 1.21.1 and for all other loaders/renderers, `MixinBlockEntityRenderDispatcher` handles universal, portable cancellation at the vanilla `BlockEntityRenderDispatcher.render()` level without fragile internal Sodium method hooks.
10. `client.MixinClientLevelChunkLifecycle`:
    Injects into `ClientLevel.onChunkLoaded` and `unload`, dispatching asynchronous chunk population (`onChunkLoadedAsync`) and eviction (`onChunkUnloadedAsync`) to `RetainedInstanceGraph`.
11. `client.MixinIrisShadowRenderer`:
    Maintains `ShaderCompatibilityDetector.setRenderingShadowPass(true/false)` state across shadow passes and triggers `IrisShadowBeFunction121.onShadowPassStart()` on 1.21.1.
12. `LevelChunkSilentRemovalMixin`:
    Redirects `Collection.forEach` inside `LevelChunk.clearAllBlockEntities()` to iterate over `collection.toArray()`, preventing client-side `ConcurrentModificationException` during chunk unloads and mass crater block entity teardowns (where multiblock `setRemoved()` triggers controller removals from chunk maps).

---

## 9. Server Synchronization, Recipe Resolution, and Performance

- **128-Bit Anti-Sync-Spam Gates**:
  `BaseHbmBlockEntity` implements `syncIfStateChanged(long lo, long hi)` and `syncIfChangedOrEvery(long lo, long hi, int intervalTicks)`, comparing 128-bit bitpacked signatures of rendered state to eliminate unnecessary network packet spam across ~100 machine BEs.
- **Positional Phase Spreading**:
  `probeDue(int intervalTicks)` uses positional coordinate hashing to distribute periodic server scans across 5, 10, 20, and 40 tick intervals for network subscriptions and periodic scans (while keeping continuous airflow/damage zones and afterburner particle emitters at full 20 Hz tick rate), eliminating synchronized network spikes across large automated machine arrays.
- **Event-Driven Safety Horizon**:
  `NUCLEUS_RENDER_REFRESH_TICKS` extended from 15 to 1200 ticks in `BaseHbmBlockEntity` and `LightSampleCache`, as lighting is now fully event-driven and distance fade is evaluated GPU-side.
- **Indexed Recipe Lookups & Candidate Pruning**:
  `ModRecipeIndex` expands `TypeIndex` with a `byIngredient` map for O(1) item lookups, `byDynamicIngredient` lists for tag/wildcard recipes, and an identity-based `byRecipe` reverse map to `ResourceLocation`. `MachineModuleAdvancedAssembler` prunes recipe candidates using `ModRecipeIndex.getCandidatesContaining`, while `CrackingTowerRecipe.getRecipe` queries the shared index directly without defensive list allocations.
- **Server-Side Recipe Cache Invalidation**:
  `RecipeCacheInvalidationHooks` listens on `MinecraftForge.EVENT_BUS` and `NeoForge.EVENT_BUS` for `AddReloadListenerEvent`, `OnDatapackSyncEvent`, and `TagsUpdatedEvent` to increment `ModRecipeIndex.epoch()`. Avoids client-only recipe events on dedicated servers.
- **Cached Recipe Resolver**:
  `CachedRecipeResolver` pairs 64-bit input fingerprints (`PlatformHooks.stackFingerprint` and `fluidFingerprint`) with index epochs, evaluating recipe matches only when inputs or datapacks actually change.
- **Recipe Index Reverse Lookups & Invalidation (`RecipeHooks`)**:
  `RecipeHooks.getAllRecipesById` returns cached unmodifiable maps from `ModRecipeIndex.of(manager).getByIdMap(type)`, eliminating repeated `LinkedHashMap` allocations. `RecipeHooks.recipeId` provides O(1) reverse recipe ID resolution via `ModRecipeIndex.getIdOf(type, recipe)` using an `IdentityHashMap`, avoiding entry set iteration on 1.21.1.
- **Input-Signature-Gated Recipe Ticking (`MachineModuleBase`)**:
  `MachineModuleBase.computeInputSignature` computes a 64-bit input fingerprint across input slots and blueprints each tick, completely gating recipe resolution and blueprint validation so expensive scans are bypassed when inputs are stationary. `isItemValidForSlot` memoizes validity per `(inputSignature, slot, itemFingerprint, epoch)`.
- **Energy Subscription Cost Cuts & Port Caching**:
  * Lazy Port Suppliers: `EnergySubscriptions.update` introduces a `Supplier<BlockPos[]>` overload so expensive multiblock structure port arrays are not materialized until after deduplication and backoff checks pass.
  * TTL Port Caching: `BaseMachineBlockEntity.getCachedExtraPorts()` caches extra energy ports with a 100-tick TTL (5 seconds), invalidating on `setBlockState`, `setLevel`, and `neighborChanged` via `MachineCapacitorBlock`.
  * Safe Unsubscribe Ordering: In `BaseMachineBlockEntity.setRemoved()`, cached ports are retrieved and passed to `unsubscribeAll` before invalidating the cache, preventing leaked energy receivers.
- **Fluid MK2 Redundant Lookups & Direct Push**:
  * Overloaded `trySubscribe` and `tryProvide` in `IFluidReceiverMK2` and `IFluidStandardSenderMK2` accept a pre-resolved `@Nullable BlockEntity`, skipping redundant `level.getBlockEntity(pipePos)` calls in tick loops.
  * `UniversalMachinePartBlockEntity` introduces `provideSendingTanksPerTick`, separating direct tank pushes to adjacent pipes from the 4 Hz connector network subscription gate and restoring full 1-tick fluid throughput.
  * `MachineDroneCrateBlockEntity` and `OilDrillBaseBlockEntity` decouple 1-tick pushes from 20-tick subscription intervals and skip processing when tanks are empty.
- **Batched Fleet Benchmark Spawning & Rollback (`NucleusStressManager`)**:
  `NucleusStressManager` introduces tick-interleaved scheduling (`scheduleBatchedSpawn` and `scheduleBatchedRestore` with `BATCH_SIZE = 100`), distributing stress machine placement and demolition across server ticks to avoid watchdog timeouts and memory stalls during 10,000-machine benchmark runs, with persistent crash recovery.

---

## 10. Multiblock Teardown Invariants & Phantom Visual Elimination

- **Controller Fallback Reconstruction**:
  `UniversalMachinePartBlock.onRemove` and `destroyControllerOnHarvest` reconstruct the controller position using `partBe.getLocalOffsetFromController()` and block facing if `partBe.getControllerPos()` is null or unlinked, resolving visual ghosting during 1.20.1 -> 1.21.1 world migrations.
- **Multi-Channel Client Eviction**:
  * Channel A: Server demolition cascade via `destroyStructure` and `destroyBlock(controllerPos, true)`.
  * Channel B: Client-side part removal via `RenderHooks.onMultiblockPartRemoved` -> `NucleusDispatcherBypass.onMachineControllerRemoved`.
  * Channel B2: Door removal via `DoorBlockEntity.setRemoved` -> `RenderHooks.onDoorRemoved`.
- **In-Code Teardown Diagnostics & Demolition Tracing**:
  `TeardownTrace` provides an in-code tracing switch (`TeardownTrace.TRACE`) logging teardown cascades with `[HBM-M Teardown]` tags without requiring JVM system properties. `MultiblockStructureHelper` logs the demolishing player's identity and traces individual part clearance sequences through `TeardownTrace`.
- **Door Animation State Cleanup**:
  `DoorAnimationDelayHelper` ensures door open/close animation progress and sound delays are cleanly wiped when a door skin changes or the door is demolished.

---

## 11. Audio Physics & Moving Airship (Sable/Aeronautics) Support

- **World-Space Sound Re-Anchoring**:
  `SableSoundPosition` projects block entity positions into world space every tick via `SableCompat.toWorld(level, x, y, z)`. On Create Aeronautics (Sable) moving airships, block entities reside in distant plot-grid chunks; without per-tick re-anchoring, linear sound attenuation mutes loops or leaves sounds detached in empty space.
- **Machine Sound Instances Updated**:
  Applied per-tick world-space re-anchoring to 7 machine sound instances: `AdvancedAssemblerSoundInstance`, `AssemblerSoundInstance`, `BroadcastSoundInstance`, `ChemicalPlantSoundInstance`, `CrystallizerSoundInstance`, `ElectricHeaterHumSoundInstance`, and `ShredderSoundInstance`.
- **Loop Sound Factories Updated**:
  `ChungusLoopSoundFactory`, `DoorLoopSoundFactory`, `MachineLoopSoundFactory`, and `TurbineLoopSoundFactory`.

---

## 12. Third-Party Mod Compatibility: Flashback Replay & Simulated Schematic Capture

- **Flashback Replay Video Export (`ClientWorldFreeze`)**:
  * Root Cause: During Flashback video export, `replayPaused` is permanently locked to `true` while the replay plays forward frame by frame. Treating export as a world freeze caused missile tracks to freeze in mid-air and halted particle emission across the entire exported video.
  * Export Detection: `ClientWorldFreeze.isFlashbackExporting()` reflectively probes `Flashback.isExporting()`, exempting video exports from pause freeze.
  * Diagnostic Logging: `MissileTrackClient.logFlashbackExportStart()` logs missile track counts, positions, velocities, and chunk boundary states when an export begins.
  * Rewind Purge Protection: `ParticleEngineNT.onWorldGameTime(gameTime, rewindPurgeAllowed)` disables rewind particle purging during export setup rewinds, ensuring pre-loaded visual effects survive export initialization.
- **Simulated Schematic Capture Memoization (`DiagramRenderCompat`)**:
  * Root Cause: `DiagramRenderCompat.isRenderingDiagram()` was called once per machine part per frame (tens of thousands of calls on large factories). Reflective `Field.getBoolean` lookups consumed 1.9% of frame time on 5,000-machine fleets.
  * Signature Memoization: Memoized the result across the frame signature `(memoStamp, memoOrtho, memoShadowPass)`, eliminating reflective overhead while preventing stale cache leaks across same-frame schematic capture flips.

---

## 13. Platform Bridges & Multiloader Infrastructure

- **Platform Fingerprints & Compound Fallback**:
  `PlatformHooks` adds 64-bit `stackFingerprint` and `fluidFingerprint` hashing; `readBlockPos(CompoundTag)` adds backwards-compatible decoding for legacy `{X, Y, Z}` integer compounds; `isPauseMenuOpen()` checks for `PauseScreen` in singleplayer.
- **Partial Tick Alignment (1.21.1)**:
  `RenderHooks.getPartialTick()` switched on 1.21.1 to `getGameTimeDeltaPartialTick(false)`. The previous `true` argument skipped DeltaTracker's frozen branch, returning raw residuals desynced by up to 1 tick from `LevelRenderer` and BER lerps, causing jerky animation on overloaded clients.
- **Native Memory Leak Remediation**:
  `RenderHooks.safeShaderSource` wraps native string allocation in a `try ... finally` block, ensuring `MemoryUtil.memFree(sourceBuffer)` executes unconditionally on all exit paths.
- **Server Hang Watchdog Enhancements**:
  `ServerHangWatchdog` suspends stall detection when the singleplayer pause menu is open and filters out parked `ForkJoinPool.awaitWork` threads from stack traces to eliminate log clutter.

---

## 14. Configuration, Datagen & Build Toolchain

- **Configuration & Localization Datagen**:
  * Registered configuration options in `ModClothConfig` and `ConfigSchema`: `nucleusBakeHiZ` (Hi-Z occlusion during GPU bake), `nucleusBakeCacheMB` (VRAM cache budget in MB), and `occlusionCullingMode` (GPU/CPU/OFF).
  * Generated complete English (`en_us.json`) and Russian (`ru_ru.json`) localization entries via `ModLanguageProviderEn` and `ModLanguageProviderRu`.
- **Stonecutter Upgrade**:
  Stonecutter upgraded from 0.8 to 0.9.8 in `gradle/libs.versions.toml` and `settings.gradle.kts`.
- **Developer Build Decoupling**:
  `ModPlatformPlugin.kt` decouples `sourcesJar` and `javadocJar` tasks from `assemble`, saving 15-20 seconds per development build while maintaining explicit dependencies for `PublishModTask`.
- **KSP Annotation Processing Retention**:
  Retains `kspKotlin` task dependency in `processResources` to ensure mixin annotation processing and refmap generation succeed under ModDevGradle 2.x.
- **Profiler JVM Arguments**:
  Added `-XX:+UnlockDiagnosticVMOptions` and `-XX:+DebugNonSafepoints` to Forge and NeoForge `runClient` configurations.
- **Gradle Test Tasks**:
  Registered `runMilestone1RemedTest` and `runSpatialLightingTest` as `JavaExec` tasks in `build.forge.gradle.kts`.

---

## 15. Comprehensive Verification & Test Suite

- **GameTests (`src/main/java/com/hbm_m/test/`)**:
  * `MachineCraftingGameTest`: 4 new `@GameTest` methods verifying Advanced Assembler blueprint folder mechanics:
    1. `assemblerModule_folderAutoPick_ironPlates_craftsContinuously`: Verifies automated recipe selection and continuous multi-cycle crafting.
    2. `assemblerModule_folderAutoPick_tooFewIngots_noPick`: Verifies recipes are rejected when ingredient counts are insufficient.
    3. `assemblerModule_folderAutoPick_reinsert_recovers`: Tests removal and re-insertion of items mid-craft, ensuring crafting resumes automatically.
    4. `assemblerBE_folderAutoPick_reinsert`: Full BlockEntity integration test with energy buffers and inventories.
  * `MultiblockTeardownGameTest`: Registered via `GameTestRegistration` across both Forge and NeoForge, verifies that destroying any single casing or part of a multiblock structure (e.g. Turbofan) triggers a clean cascade destroying the entire multiblock, controller, and phantom parts.
- **Empirical Test Suites (`src/test/java/com/hbm_m/test/`)**:
  * `MultiblockSpatialLightingTest`: Validates physical bounds vs frustum culling radius (fixing the 8-block underground probe bug), non-darkening torch invariants, part-block trilinear weight partition of unity, cardinal facing parity, and GLSL trilinear reconstruction parity.
  * `Milestone1RemediationTest`: Adversarial stress tests for slot lifecycle, native memory zeroing, fadeMode sentinels (-1.0f invisible), high-water mark adjustment, coordinate rebase deltas, and 10,000 randomized churn operations.
  * `Milestone1SlotAllocatorTest`: Validates FIFO slot recycling and allocation.
  * `Milestone2And3VerificationTest`: Tests Hermite spline analytical stroke smoothness and symmetry, continuous rotation kinematics, and joint bitmasks.
  * `SmokeTest`: Basic slot allocator and sparse update queue smoke test.
- **Engine Adversarial Suites (`src/main/java/com/hbm_m/test/`)**:
  * `Milestone2RetainedSceneGraphTest`: Tests retained scene graph partition bounds tracking, slot persistence, and sparse update batching.
  * `RetainedAllocAdversarialTest`: 120,000+ slot allocation and free stress test.
  * `WorkerDefects15Test`: Tests keyed slot allocation at negative Y coordinates (Y <= -51 down to -64), part base offset math cancellation (0.5 - 0.5 = 0.0), and GL state guards.

---

## 16. Known Open Limitations & Behavioral Notes

- **Iris-Path Distance Fade**:
  Distance fade remains non-functional under Iris shaderpacks because the Iris vertex shader interprets the fade slot as raw alpha; instances pop at the cutoff distance rather than smoothly dissolving.
- **Non-Dither MDI Blending**:
  MDI blending is inactive in non-dither mode because `cachedMinFade` is not written.
- **ShaderBlockIdDisguises**:
  `ShaderBlockIdDisguises` is non-functional / dead-on-arrival in practice and is retained as a placeholder for a future rework.
- **Behind-Camera Collect Cost**:
  In GPU culling mode, machines behind the camera are collected without a CPU frustum gate by design, incurring a measurable collection overhead (~36 ms on extreme test scenes) before being culled GPU-side.
- **Bake and GPU Kinematics Non-Composition**:
  Tier 1 GPU compute bake deliberately does not compose with GPU parametric animation; configurations with compute bake enabled run CPU animators.
- **Transitional MRT G-Buffer Path**:
  The 4-target MRT G-buffer encoder in `IrisInstancedEncoders` remains transitional.
- **Mass-Spawn & VRAM Pool Scaling (RESOLVED)**:
  Previous driver timeouts/GPU crashes at >65,535 instances have been resolved via 2D/3D compute workgroup addressing (`rawY`) and tiled dispatch triples (`min(n, 65535u)`). VRAM leaks and pool over-allocations are resolved through dynamic periodic shrinking (`maybeShrinkPool`, `shrinkOversizedBuffer`, and `tickFrameCleanup`).
