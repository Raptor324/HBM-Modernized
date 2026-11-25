# Project: HBM Modernized Firearms Pipeline to Nucleus Migration

## Architecture
- **Subsystem**: Firearms and Handheld Weapon Rendering Pipeline
- **Core Pattern**: Model loading via Nucleus `AbstractObjPartModelLoader` -> deterministic baking into named `BakedModel` parts -> pre-compilation into native `VboData` via `PartGeometry` -> direct GPU drawing via `SingleMeshVboRenderer` / `AbstractGpuMesh`.
- **Kinematics**: Declarative joint nodes in `NucleusJointSpecs` evaluated on `PoseStack` / GPU instanced vertex shaders.
- **Viewmodel & GunGL Layer**: First-person custom FOV projection matrix, 0.05 near-plane depth clear (`GL11.GL_DEPTH_BUFFER_BIT`), modelview isolation, GunGL render modes (cutout, translucent, additive emissive), color tint, fullbright.
- **Dynamic Effects**: Immediate-mode quad and ribbon rendering (`GunGL.Tess` / `MultiBufferSource`) layered on top of VBO geometry with `NO_LIGHTMAP` / `FULL_BRIGHT`.
- **Target Platforms**: Multi-loader via Stonecutter 0.9.8 targeting `1.20.1-forge` and `1.21.1-neoforge`.

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Nucleus Weapon Model Loader | `WeaponPartsModelLoader` extending `AbstractObjPartModelLoader` to load weapon OBJs via native `LoaderHooks.loadObjModel` | M1 | Survey / R1 |
| 2 | Baked Weapon Part Geometry | Deterministic `BakedModel` part splitting via `SinglePartBakingContext` without runtime `.obj` parsing | M1 | Survey / R1 |
| 3 | VBO Pre-Compilation & Registry | `PartGeometry.compile()` into native off-heap `VboData` at `ModelEvent.ModifyBakingResult`, cached in `WeaponRenderRegistry` | M1 | Survey / R1 |
| 4 | Standalone Texture UV Normalization | Normalized `[0, 1]` UV coordinate unmapping in `PartGeometry` for standalone weapon PNG samplers | M1 | Survey / R1 |
| 5 | Native Weapon VBO Renderer | `WeaponPartVboRenderer` extending `SingleMeshVboRenderer` / `AbstractGpuMesh` with 36-byte stride | M2 | Survey / R2 |
| 6 | Excision of GpuObjMesh | Complete removal of `GpuObjMesh.java` and elimination of duplicate line-by-line OBJ parser | M2 | Survey / R2 |
| 7 | Elimination of Deadlock | Populate all VBO ranges at model bake time; eliminate uninitialized vertex range cycle and CPU streaming fallback | M2 | Survey / R2 |
| 8 | GunGL Viewmodel Contract | Preserve first-person custom projection, FOV, 0.05 depth clear, GunGL modes (0, 1, 2), tint, and fullbright | M2 | Survey / R2 |
| 9 | NucleusJointSpecs Kinematics | Migrate bolts, magazines, drums, levers, chaingun barrels, chainsaw teeth, and akimbo to `NucleusJointSpecs` | M3 | Survey / R3 |
| 10 | Dynamic Effects Layering | Muzzle flashes, gap flashes, laser flares, smoke ribbons, and tracers rendered cleanly with `NO_LIGHTMAP` and `FULL_BRIGHT` | M4 | Survey / R4 |
| 11 | Cross-Platform Stonecutter Builds | Clean compilation of `:1.20.1-forge:compileJava` and `:1.21.1-neoforge:compileJava` | M5 | Survey / R5 |
| 12 | Kinematic Parity Harness Pass | 100% pass rate on `tools/kinematic_verification_harness.py` and adversarial parity suites | M5 | Survey / Acceptance |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Weapon Model Loader & VBO Compilation | `WeaponPartsModelLoader`, `WeaponMultipartBakedModel`, `PartGeometry` UV normalization, `WeaponRenderRegistry` | none | DONE |
| M2 | Native VBO Renderer & GpuObjMesh Excision | `WeaponPartVboRenderer`, delete `GpuObjMesh.java`, redirect `GunGL.renderPart`, eliminate deadlock | M1 | DONE |
| M3 | NucleusJointSpecs Kinematics Migration | Register weapon joints in `NucleusJointSpecs`, connect weapon animations (bolts, drums, barrels, chainsaw, akimbo) | M2 | DONE |
| M4 | GunGL Dynamic Effects & Viewport Preservation | First-person FOV, 0.05 depth clear, transient quad passes (flashes, smoke, tracers) | M2 | PLANNED |
| M5 | Cross-Platform Stonecutter & Final Verification | Compile 1.20.1 & 1.21.1, run kinematic harness, execute full E2E & forensic audit | M1, M2, M3, M4 | PLANNED |

## Interface Contracts
### `WeaponPartsModelLoader` ↔ `Minecraft / LoaderHooks`
- Extends `AbstractObjPartModelLoader<WeaponMultipartBakedModel>`
- Signature: `read(JsonObject, JsonDeserializationContext) -> ObjPartGeometry<WeaponMultipartBakedModel>`
- Hook: `LoaderHooks.registerGeometryLoader("weapon_obj_parts", new WeaponPartsModelLoader())`

### `WeaponRenderRegistry` ↔ `GunGL / ItemRender*`
- Signature: `WeaponRenderRegistry.getPartRenderer(ResourceLocation modelLocation, String partName) -> Optional<WeaponPartVboRenderer>`
- Signature: `WeaponRenderRegistry.renderPart(ResourceLocation modelLocation, String partName, PoseStack ps, int light, int overlay, float r, float g, float b, float a)`
- Invalidation / Reload: Cleared and repopulated on `ModelEvent.ModifyBakingResult` / resource reload.

### `WeaponPartVboRenderer` ↔ `Shader / GPU`
- Vertex Stride: 36 bytes (`pos`: 3x float, `normal`: 3x float, `uv`: 2x float, `bone_id`: 1x int)
- Shaders: `ModShaders.getWeaponLitShader()` (`weapon_lit.vsh` / `weapon_lit.fsh`)
- Samplers: Standalone weapon texture sampler or bound texture via `RenderSystem.setShaderTexture(0, ...)`.

### `NucleusJointSpecs` ↔ `Weapon Kinematics`
- Joint indexing: Dynamic or static weapon bone IDs allocated in joint texture registry.
- CPU evaluation: `MachineSpec.applyChainNodes(nodes, progress, poseStack)` or direct matrix evaluation matching 1.7.10 kinematic formulas.

## Code Layout
- `src/main/java/com/hbm_m/client/loader/WeaponPartsModelLoader.java`: Model loader for weapon OBJ models.
- `src/main/java/com/hbm_m/client/render/weapon/WeaponMultipartBakedModel.java`: Baked model wrapper for multipart weapons.
- `src/main/java/com/hbm_m/client/render/weapon/WeaponRenderRegistry.java`: Central registry of baked weapon part VBO renderers.
- `src/main/java/com/hbm_m/client/render/weapon/WeaponPartVboRenderer.java`: Concrete native VBO renderer for weapon parts.
- `src/main/java/com/hbm_m/client/weapon/GunGL.java`: Redirect `renderPart` to `WeaponRenderRegistry`.
- `src/main/java/com/hbm_m/client/weapon/WeaponResources.java`: Update model registry references.
- `src/main/java/com/hbm_m/client/render/GpuObjMesh.java`: DELETED (`git rm`).
