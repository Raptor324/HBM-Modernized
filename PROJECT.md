# Project: Nucleus GPU Occlusion Culling & Shadow Pipeline Expansion

## Architecture

Nucleus is the high-performance modern rendering engine in HBM-Modernized, built to handle complex multiblock machinery with hundreds of thousands of vertices via Multi-Draw Indirect (MDI), instanced rendering, and GPU compute baking.

This project introduces two major architectural enhancements:
1. **GPU Hierarchical-Z (Hi-Z) Occlusion Culling with Safe Fallback**:
   - Depth Pyramid generator (`HiZDepthPyramid`) consuming Minecraft's main depth buffer (`RenderTarget.getDepthTextureId()`).
   - Single Pass Downsampler (SPD) compute shaders adapted to **Forward-Z** ($0.0 = \text{near}, 1.0 = \text{far}$, `GL_LEQUAL`, `max()` downsample reduction).
   - Compute cull shader (`nucleus_cull.comp`): Bounding sphere projection (`projectSphere`), screen UV AABB, 4-texel mip fetch with `max()`, Forward-Z depth test (`depthSphere <= occluderDepth`), stream compaction of visible instances into an output VBO, and atomic increment of `cmd.instanceCount` in the MDI draw indirect buffer.
   - Hardware capability probing (`GpuCullingCapability`) with multi-stage guards (headless server, datagen, uninitialized GLFW context, macOS Core Profile 4.1 cap).
   - Dynamic configuration gating (`ModClothConfig`, `ConfigScreen`) locking the `GPU` mode with red diagnostic tooltips when unsupported, safely defaulting to `OFF` or honoring user-selected `CPU` ray-marching (`OcclusionCullingHelper`).
2. **Tier 1 GPU Compute Bake Shadow Pipeline Expansion**:
   - Universal compute baking of multiblock parts directly into VRAM for shadow passes (`NucleusGpuBaker`).
   - Relaxed attribute validation for shadow passes (requiring only position and UV0; eliminating missing tangent/mid-tex failures).
   - 12-float $3 \times 4$ affine transform matrix representation supporting rotation, translation, and non-uniform scale.
   - Bone palette SSBO for dynamic skeletal bone animations.
   - Platform bridge (`ShadowFrustumBridge`) resolving the Oculus 1.20.1 (`Vector4f[]`) vs Iris 1.21.1 (`float[][]`) frustum plane discrepancy.
   - Complete bypass of fragile Tier 2 regex AST distortion parsing (`IrisShadowDistortion`), utilizing the active shaderpack's native `shadow.vsh` for distortion.

---

## Feature Inventory

Every feature derived from the survey and requirements is cataloged and assigned to a milestone:

| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| F1 | `GpuCullingCapability` Prober | OpenGL 4.3 / compute / SSBO / image load-store / texture storage prober with headless, datagen, GLFW context, and macOS guards | M1 | Survey (explorer 3) |
| F2 | Config Mode Enum & Fallback Resolver | `OcclusionCullingMode` (`GPU`, `CPU`, `OFF`) in `ModClothConfig` with `getEffectiveOcclusionCullingMode()` | M1 | Survey (explorer 3) |
| F3 | Config GUI Gating & Tooltip | `ConfigScreen.java` dynamic cycle filtering (`[CPU, OFF]` if unsupported) and red failure diagnostic tooltip | M1 | Survey (explorer 3) |
| F4 | Legacy Config Migration | Deprecate `enableOcclusionCulling` with automatic backward-compatible mapping to enum | M1 | Survey (explorer 3) |
| F5 | Forward-Z Hi-Z Depth Pyramid | `HiZDepthPyramid.java` and SPD downsampler shaders (`downsample_first.comp`, `downsample_second.comp`) using `max()` reduction | M2 | Survey (explorers 1 & 2) |
| F6 | Forward-Z Compute Cull Shader | `nucleus_cull.comp` with sphere projection, 4-texel `max()` fetch, `depthSphere <= occluderDepth`, stream compaction, and atomic `instanceCount` update | M2 | Survey (explorers 1 & 2) |
| F7 | MDI & Renderer Hi-Z Integration | Hook Hi-Z downsample and compute culling pass into `MdiBatchCoordinator.java` and `InstancedStaticPartRenderer.java` | M2 | Survey (explorer 2) |
| F8 | Safe CPU & OFF Fallback Execution | Execution dispatch routing: `GPU` -> Hi-Z compute culler; `CPU` -> `OcclusionCullingHelper`; `OFF` -> bypass culling | M2 | Survey (explorer 2) |
| F9 | Relaxed Shadow Mesh Attributes | `NucleusGpuBaker.java` relaxes attribute requirements in `MODE_SHADOW` (position + UV0 only) | M3 | Survey (explorers 2 & 3) |
| F10 | 12-Float Affine Transform & Bones | $3 \times 4$ affine transform matrix and `BonePaletteSSBO` for dynamic skeletal bone animations in compute baker | M3 | Survey (explorers 1 & 2) |
| F11 | Cross-Version Shadow Frustum Bridge | Platform bridge in `com.hbm_m.platform` handling Oculus 1.20.1 `Vector4f[]` vs Iris 1.21.1 `float[][]` planes | M3 | Survey (explorer 1) |
| F12 | Native Pack Shadow Pass Submission | Submit compute-baked shadow meshes to active pack shadow shader, eliminating static schemas and `IrisShadowDistortion` | M3 | Survey (explorers 1 & 2) |
| F13 | Dual-Target Compile Parity | Clean compilation on `:1.20.1-forge:compileJava` and `:1.21.1-neoforge:compileJava` | M4 | Requirements |
| F14 | Headless Server & Datagen Immunity | Verify datagen and server boot immunity from GL calls | M4 | Requirements |
| F15 | Adversarial Verification of Hi-Z Math | Empirical testing of Forward-Z depth math against false culling, flickering, and transparent blocks | M4 | Requirements |
| F16 | Gate Verification & Final Handoff | Forensic audit, dual-reviewer approval, and final handoff delivery | M4 | Requirements |

---

## Milestones

| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Hardware Capability Probing & Config Gating | F1, F2, F3, F4 | none | DONE |
| M2 | Forward-Z Hi-Z GPU Occlusion Culling | F5, F6, F7, F8 | M1 | DONE |
| M3 | Tier 1 GPU Compute Bake Shadow Pipeline Expansion | F9, F10, F11, F12 | M1 | DONE |
| M4 | Dual-Target Parity & Adversarial Gate Verification | F13, F14, F15, F16 | M2, M3 | DONE |

---

## Interface Contracts

### 1. `GpuCullingCapability` ↔ `ModClothConfig` / `ConfigScreen`
```java
public final class GpuCullingCapability {
    public static boolean isSupported();
    public static String getUnsupportedReason();
}
```
- `isSupported()` must never throw exceptions or invoke OpenGL if `Platform.getEnvironment() != Env.CLIENT` or `GLFW.glfwGetCurrentContext() == 0L`.
- Returns `false` on macOS (`Minecraft.ON_OSX`) with reason `"macOS OpenGL is capped at 4.1 without compute shader support."`.

### 2. `ModClothConfig` ↔ `ClientRenderFlags` / `MdiBatchCoordinator`
```java
public enum OcclusionCullingMode {
    GPU, CPU, OFF;
}
public OcclusionCullingMode getEffectiveOcclusionCullingMode();
```
- If configured as `GPU` but `!GpuCullingCapability.isSupported()`, returns `OFF`.
- If configured as `CPU`, returns `CPU` unconditionally.

### 3. `HiZDepthPyramid` ↔ `MdiBatchCoordinator`
```java
public final class HiZDepthPyramid {
    public void regenerate(int sourceDepthTextureId, int width, int height);
    public int getPyramidTextureId();
    public int getMipLevels();
    public void destroy();
}
```
- Takes raw depth texture handle from `RenderTarget.getDepthTextureId()`.
- Dispatches SPD downsampler with Forward-Z `max()` reduction into mip levels 0..N.

### 4. `ShadowFrustumBridge` ↔ `IrisShadowBatchCollector` / `NucleusGpuBaker`
```java
public final class ShadowFrustumBridge {
    // Forge 1.20.1: reads Vector4f[] planes
    // NeoForge 1.21.1: reads float[][] planes
    public static boolean extractShadowPlanes(Object frustum, float[] out13Planes);
}
```

---

## Code Layout

```
src/main/java/com/hbm_m/
├── config/
│   ├── ModClothConfig.java          # OcclusionCullingMode enum & getEffectiveOcclusionCullingMode
│   └── ConfigSchema.java            # Field registration
├── client/
│   ├── gui/
│   │   └── ConfigScreen.java        # Enum cycle gating & tooltip rendering
│   └── render/
│       ├── culling/
│       │   ├── GpuCullingCapability.java   # OpenGL 4.3 capability prober
│       │   ├── HiZDepthPyramid.java        # Depth pyramid texture & SPD dispatch
│       │   ├── NucleusGpuCuller.java       # Compute cull dispatch & stream compaction
│       │   └── OcclusionCullingHelper.java # Preserved CPU ray-marching fallback
│       ├── MdiBatchCoordinator.java        # Hi-Z depth binding, culling pass execution
│       ├── InstancedStaticPartRenderer.java # Compacted instance buffer binding
│       ├── NucleusGpuBaker.java            # Relaxed shadow attributes, 3x4 affine matrices, bone palette
│       └── IrisShadowBatchCollector.java   # Universal shadow submission
└── platform/
    └── client/
        └── ShadowFrustumBridge.java        # Cross-version Oculus / Iris plane extraction
```
