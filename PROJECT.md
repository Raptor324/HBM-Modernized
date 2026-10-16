# Project: Nucleus Retained GPU-Driven Rendering Engine Overhaul

## Architecture

Nucleus is the high-performance modern rendering engine in HBM-Modernized, built to render complex multiblock machinery with hundreds of thousands of vertices via Multi-Draw Indirect (MDI), instanced rendering, and GPU compute baking.

This architectural overhaul transitions Nucleus from legacy immediate-mode BlockEntityRenderer traversal with fragile order-dependent slot assertions to a true **Retained GPU-Driven Engine**:
1. **Persistent Slot Allocation & Robust Instance Handles (M1)**:
   - Migration from volatile linear index matching (`instanceOcclusionKeys[instanceCount] == posKey`) to persistent 64-bit coordinate handles managed by `InstanceSlotAllocator`.
   - Slots remain stable and do not invalidate or shift when the camera rotates, frustum culls adjacent blocks, or alters chunk traversal order.
   - Elimination of hybrid multipart dynamic fallbacks: `Advanced Assembler` `Frame` converted to a first-class instanced mesh with visibility gating (`hasDynamicParts = false`).
2. **True Retained Scene Graph Integration (M2)**:
   - `RetainedInstanceGraph` populated on chunk load/unload and machine placement/break events.
   - Bypassing per-frame CPU BlockEntity iteration (`collectMain` flat/bucket loops) for static machines in steady state (`isMachineStaticAndRetained`).
   - `SparseUpdateQueue` for fine-grained dirty state and light invalidation updates without full buffer diffing (`diffUpload`) or native memory float comparisons (`recordMatchesBuffer`).
   - Section light updates routed through `sectionToMachines` mapping, triggering per-machine `DirtyMask.LIGHT` refreshes instead of coarse chunk-center overwrites.
3. **GPU-Side Distance Fading and Kinematics Offloading (M3)**:
   - Elimination of CPU `computeWindowMinFade` per-frame scanning loop; evaluate distance fading (`gpuFadeAlpha`) in vertex/fragment shaders.
   - Universal screen-door dithering (`nucleusDitherFade`) implemented via interleaved gradient noise in fragment shader; **disabled by default** (`nucleusDitherFade = false`) for smooth visual alpha fading.
   - Autonomous continuous rotation (Turbofan rotors) and reciprocating motion (Industrial Press head strokes) evaluated via shader kinematics from camera uniform time (`u_Time` / `uAnimTicks`), cutting `PoseStack.pushPose()` and `Matrix3f` constructor churn.
   - Shader kinematics parity in `block_lit_instanced_iris.vsh` under active shaderpacks.
4. **Universal 2-Tier Hardware Compatibility & Iris Interop (M4)**:
   - **Tier 1 (Modern Desktop GL 4.3+)**: SSBO instance storage, GPU compute occlusion/frustum culling (`nucleus_cull.comp` + Hi-Z depth pyramid), and Multi-Draw Indirect (`glMultiDrawElementsIndirect`).
   - **Tier 2 (Universal Baseline GL 3.2 Core Profile)**: Strictly zero compute shaders, zero SSBOs, and zero MDI; CPU frustum culling via `CpuFrustumCuller`, and instanced array drawing (`glDrawElementsInstanced`). Universal compatibility on macOS (Apple Silicon/Intel) and legacy Intel HD Graphics.
   - Iris/Oculus shaderpack interoperability preserving deferred PBR attachments and companion mesh fallback without black models.
5. **Robust Exterior 3D Light Sampling (M5 Remediation)**:
   - 8-corner trilinear lattice sampling (`LightSampleCache.sample8`) rewritten to probe exterior air blocks adjacent to machine corners rather than collapsing inside opaque solid blocks.
   - Multi-candidate exterior probing with vertical floor relaxation and smoothed face lighting fallbacks (`sampleSmoothLightUV`).
   - Retained GPU VBO records baked and updated with full 8-corner light data via `SparseUpdateQueue.sampleMachineLight`.

---

## Feature Inventory

| # | Feature | Description | Milestone | Status |
|---|---------|-------------|-----------|--------|
| F1 | `InstanceSlotAllocator` Integration in Part Renderer | Connect `InstanceSlotAllocator` to `InstancedStaticPartRenderer` using persistent 64-bit coordinate keys | M1 | COMPLETED |
| F2 | Persistent Slot Handle Assertion Logic | Replace fragile `instanceOcclusionKeys[instanceCount] == posKey` linear assertion with persistent slot handle lookup | M1 | COMPLETED |
| F3 | Universal Instanced Conditional Parts | Convert conditional parts (Frame across Advanced Assembler, Chemical Plant, Fluid Tanks, etc.) from dynamic uninstanced quads to 100% instanced meshes with slot or visibility gating | M1 | COMPLETED |
| F4 | Chunk Lifecycle Hooks in Retained Graph | Hook chunk load/unload events (`onChunkLoaded`, `onChunkUnloaded`) to `RetainedInstanceGraph` | M2 | COMPLETED |
| F5 | Steady-State Static Traversal Short-Circuit | Bypass static chunk buckets and machines in `NucleusDispatcherBypass.collectMain()` | M2 | COMPLETED |
| F6 | CPU `computeWindowMinFade` Elimination | Delete per-frame `computeWindowMinFade` linear instance scanning loop from `MdiBatchCoordinator` | M3 | COMPLETED |
| F7 | Universal Screen-Door Dithering | Jimenez interleaved gradient noise dithering in fragment shaders; disabled by default (`nucleusDitherFade = false`) | M3 | COMPLETED |
| F8 | Autonomous Continuous Rotor Shader Kinematics | Evaluate Turbofan continuous rotations via `u_Time` and Rodrigues formula in vertex shader | M3 | COMPLETED |
| F9 | Iris Shader Kinematics Parity | Port ANIM TRACK v2 spline and joint chain uniforms to `block_lit_instanced_iris.vsh` | M3 | COMPLETED |
| F10 | Tier 2 GL 3.2 Baseline Enforcement | Guarantee zero compute/SSBO/MDI execution on macOS and legacy Intel iGPUs via `HardwareTierManager` | M4 | COMPLETED |
| F11 | Iris/Oculus Interop & Black Model Prevention | AST reflection layout verification with companion mesh fallback for non-packed shaderpacks | M4 | COMPLETED |
| F12 | Dual-Target Compilation Parity | Clean compilation on `:1.20.1-forge:compileJava` and `:1.21.1-neoforge:compileJava` | M5 | COMPLETED |
| F13 | Exterior Block Light & 8-Corner Sampler Remediation | Multi-candidate exterior probing, solid fallback elimination, section light update overhaul | M5 | COMPLETED |

---

## Milestones

| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Persistent Slot Allocation & Universal Conditional Parts Instancing | F1, F2, F3 | none | COMPLETED |
| M2 | True Retained Scene Graph & Event-Driven Dirty Tracking | F4, F5 | M1 | COMPLETED |
| M3 | GPU-Side Distance Fading & Shader Kinematics Parity | F6, F7, F8, F9 | M1 | COMPLETED |
| M4 | Universal 2-Tier Hardware Engine & Iris Interoperability | F10, F11 | M2, M3 | COMPLETED |
| M5 | Dual-Target Parity & Block Light Sampler Verification | F12, F13 | M1, M2, M3, M4 | COMPLETED |

---

## Root-Cause Investigation: Block Light Bug & Light Sampler Audit

### The Defect
Models and machines previously reacted to sky light, but completely ignored block light from torches, lanterns, and glowstone. At night or underground, machines appeared pitch-black even when surrounded by torches.

### Root Causes Uncovered
1. **Flawed `SAMPLE_INSET` in `LightSampleCache.sample8`**:
   - `sample8` applied `SAMPLE_INSET = 1/64` pulling each corner inward toward the bounding box center.
   - For a 1-block machine (`minX = 0, maxX = 1`), `minXs = 0.015625` and `maxXs = 0.984375`. Both floored to `0`!
   - All 8 corners collapsed to `(bpx, bpy, bpz)` inside the machine's own solid block.
   - For multiblocks, all corners landed inside the occupied casing blocks.
2. **Opaque Block Fallback Loop**:
   - When `state.isSolidRender(level, samplePos)` was true, `sample8` fell back to `packedLightFallback`.
   - `packedLightFallback` was queried at `be.getBlockPos()`—the same opaque machine block.
   - In Minecraft's `BlockLightEngine`, solid blocks have `blockLight == 0`. Hence, block light was always 0.
3. **Premature Early-Exit in `shouldSkipSpatialSampling`**:
   - When `packedLightFallback` had `blockLight == 0`, it evaluated `Math.round(PRECHECK_UV[0]) <= 0` and filled all 8 corners with `packedLightFallback` (zero block light), throwing away valid exterior lighting.
4. **Section Center Light Overwrite in `RetainedInstanceGraph.processDirtyLightSections`**:
   - When a block light update occurred, `processDirtyLightSections` sampled light at `(secX*16 + 8, secY*16 + 8, secZ*16 + 8)` (the section geometric center) and forcibly overwrote all machines in the chunk section with this single point's light.
5. **Uniform Flat Light Baking in `SparseUpdateQueue`**:
   - Initial bakes and updates used `getOrSamplePacked(be, 0)` and duplicated `bu, sv` uniformly across all corners, inheriting the solid block's 0 block light.

### Remediation Applied
- **Exterior Corner Probing (`LightSampleCache.sample8`)**: Probes outward by `0.1` blocks past each corner into exterior air. If a corner touches a solid floor or wall, it performs candidate relaxation (checking floor-level air `wy = bpy`, then relaxing X/Z axes) and falls back to smoothed exterior face light (`sampleSmoothLightUV`).
- **Exterior Face Fallback in `getOrSamplePacked`**: If `be.getBlockPos()` is solid and returns `blockLight == 0`, `getOrSamplePacked` immediately samples the 6 exterior open faces around the machine.
- **Section-to-Machines Mapping (`RetainedInstanceGraph`)**: Replaced `sectionToSlots` with `sectionToMachines`. When a chunk section experiences a light update, every machine in that section is individually refreshed via `sparseQueue.enqueueUpdate(handle, be, DirtyMask.LIGHT)`.
- **Full 8-Corner Light Baking (`SparseUpdateQueue.sampleMachineLight`)**: Retained instance records now compute and bake all 8 trilinear light corners during initial chunk load and on dirty light updates.

---

## Code Layout

```
src/main/java/com/hbm_m/client/render/
├── InstancedStaticPartRenderer.java       # Persistent slot allocator integration & instance records
├── MdiBatchCoordinator.java               # Batch submission, dither fade integration
├── HardwareTierManager.java               # Tier 1 (GL 4.3) vs Tier 2 (GL 3.2 Core) probing & enforcement
├── NucleusDispatcherBypass.java           # Static chunk/BE bypass in collectMain
├── LightSampleCache.java                  # 8-corner trilinear & 6-face exterior smoothed light sampling
├── retained/
│   ├── RetainedInstanceGraph.java         # Persistent retained scene graph, chunk lifecycle & section light updates
│   ├── ChunkInstancePartition.java       # Chunk-partitioned instance mapping
│   ├── DirtyMask.java                     # Bitmask for fine-grained instance record slices
│   ├── alloc/
│   │   └── InstanceSlotAllocator.java     # Slab slot allocator & free-list
│   └── upload/
│       └── SparseUpdateQueue.java         # Event-driven dirty span coalescing & 8-corner light baking
├── machine/
│   ├── MachineBer.java                    # BlockEntityRenderer fast assert & roster handling
│   ├── MachineSpec.java                   # Machine specification & part definitions
│   └── MachineSpecBuilder.java            # Declarative kinematics DSL
├── implementations/
│   ├── MachineAdvancedAssemblerRenderer.java # First-class instanced Frame definition
│   ├── MachineTurbofanRenderer.java          # Shader-evaluated continuous rotor
│   └── MachinePressRenderer.java             # Shader-evaluated reciprocating stroke
└── culling/
    ├── CpuFrustumCuller.java              # Tier 2 CPU Gribb-Hartmann frustum culling
    ├── NucleusGpuCuller.java              # Tier 1 compute frustum/occlusion culling
    └── HiZDepthPyramid.java               # Forward-Z Hi-Z depth downsampler
```
