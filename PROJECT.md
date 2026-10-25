# Project: 1:1 Forensic Parity Audit & Kinematic Corrections of Modern Machine Renderers

## Architecture
- **Rendering Framework**: Modernized Nucleus Rendering Engine (`com.hbm_m.client.render.machine.MachineRenderers`)
- **Kinematic Primitives**: Declarative GPU transforms (`JointNode`, `Track`, `rotatingPart`, `translatingPart`, `spinningPart`, `vibratingPart`) and procedural callbacks (`MachineRenderHook`, `PartAnimator`)
- **Reference Standard**: Canonical 1.7.10 reference repository at `C:\Projects\Hbm-s-Nuclear-Tech-GIT\src\main\java\com\hbm\render\tileentity\`

## Feature Inventory
| # | Feature / Machine Group | Description | Milestone | Source |
|---|-------------------------|-------------|-----------|--------|
| 1 | Double Pivot Fixes | Fix double-pivot displacement in `MachineSeparationRenderer` (Compressor, Compact Compressor, Microwave) | M1 | Survey 3 |
| 2 | Ghost Mesh De-duplication | Eliminate duplicate static/animated registrations in `MachineSawmillRenderer` (Blade) & `MachinePressGroupRenderer` (Piston, Bullets) | M1 | Survey 3 |
| 3 | OBJ Geometry Alignment | Correct non-existent OBJ part names in `MachinePileRenderers` (Base, Loader, Pipe) and `MachineDFCRenderers` (Cube_Cube.001) | M1 | Survey 3 |
| 4 | API Signature Corrections | Fix invalid 4-lambda calls in `MachineFusionComplexRenderer` (Magnet) & `MachineResearchFacilityRenderer` (Magnets) with `.rotateY` | M1 | Survey 3 |
| 5 | Accelerator & Dee Cleanup | Remove non-existent "Dees" in `MachineAcceleratorRenderer`, fix plug socket texture retention | M1 | Survey 3 |
| 6 | Turbine & Flywheel Parity | Invert Z rotation axis on Industrial Turbine Flywheel, set pivot to (0, 1.5, 0), add Gauge part, add Large Turbine (0, 0, -1) offset | M1 | Survey 3 |
| 7 | Structural Placement Heights | Fix missing `y - 4` translation in `SoyuzLauncherRenderer` | M1 | Survey 3 |
| 8 | Operational State Guards | Add `be.isOn` activity guard to Diesel Generator vibration in `MachineEngineRenderer` | M1 | Survey 3 |
| 9 | Continuous Simulation Angles | Replace synthetic snap-to-rest rigs with continuous simulation angle suppliers in Chem Plant, Chem Factory, Crystallizer, Excavator, Radar | M1 | Survey 3 |
| 10 | Aerospace LPW-2 Kinematics | Implement 8 articulated vectoring flaps and 4 dynamic suspension struts in `MachineLaserResearchRenderer` | M1 | Survey 3 |
| 11 | RBMK Console 2-Axis Rig | Implement 2-axis joystick kinematics and dynamic heat/enrichment meter needles in `MachineRbmkControlRenderer` | M1 | Survey 3 |
| 12 | Animation Parity Matrix | Compile comprehensive audit matrix `docs/reports/ANIMATION_PARITY_AUDIT.md` covering all animated machines | M2 | Survey 1, 3 |
| 13 | 236 Inventory Parity Report | Finalize and validate `docs/reports/RENDERER_PARITY_SUMMARY.md` covering all 236 original 1.7.10 renderers | M3 | Survey 2 |
| 14 | Multi-Agent Quality Gate | Execute dual Reviewers, dual Challengers, and Forensic Integrity Auditor | M4 | Governance |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Kinematic Parity Corrections | Apply drop-in code fixes to 11 Nucleus renderer classes in `com.hbm_m.client.render.implementations` | Survey Complete | PLANNED |
| M2 | Animation Parity Matrix Report | Generate comprehensive audit matrix `docs/reports/ANIMATION_PARITY_AUDIT.md` | M1 | PLANNED |
| M3 | Renderer Parity Summary Validation | Verify complete 236-row inventory in `docs/reports/RENDERER_PARITY_SUMMARY.md` | Survey Complete | DONE |
| M4 | Multi-Agent Audit & Verification | Reviewers (2), Challengers (2), Forensic Auditor (1) verification | M1, M2, M3 | PLANNED |

## Code Layout
- Target Implementation Files:
  - `src/main/java/com/hbm_m/client/render/implementations/MachineSeparationRenderer.java`
  - `src/main/java/com/hbm_m/client/render/implementations/MachineSawmillRenderer.java`
  - `src/main/java/com/hbm_m/client/render/implementations/MachinePressGroupRenderer.java`
  - `src/main/java/com/hbm_m/client/render/implementations/MachinePileRenderers.java`
  - `src/main/java/com/hbm_m/client/render/implementations/MachineDFCRenderers.java`
  - `src/main/java/com/hbm_m/client/render/implementations/MachineAcceleratorRenderer.java`
  - `src/main/java/com/hbm_m/client/render/implementations/MachineFusionComplexRenderer.java`
  - `src/main/java/com/hbm_m/client/render/implementations/MachineResearchFacilityRenderer.java`
  - `src/main/java/com/hbm_m/client/render/implementations/MachineTurbineRenderer.java`
  - `src/main/java/com/hbm_m/client/render/implementations/SoyuzLauncherRenderer.java`
  - `src/main/java/com/hbm_m/client/render/implementations/MachineEngineRenderer.java`
  - `src/main/java/com/hbm_m/client/render/implementations/MachineChemicalPlantRenderer.java`
  - `src/main/java/com/hbm_m/client/render/implementations/MachineChemicalFactoryRenderer.java`
  - `src/main/java/com/hbm_m/client/render/implementations/MachineCrystallizerRenderer.java`
  - `src/main/java/com/hbm_m/client/render/implementations/MachineMiningDrillRenderer.java`
  - `src/main/java/com/hbm_m/client/render/implementations/MachineLaserResearchRenderer.java`
  - `src/main/java/com/hbm_m/client/render/implementations/MachineRbmkControlRenderer.java`
- Documentation Artifacts:
  - `docs/reports/ANIMATION_PARITY_AUDIT.md`
  - `docs/reports/RENDERER_PARITY_SUMMARY.md`

## Interface Contracts
### Nucleus Declarative Transforms
- `.rotatingPart(name, axis, pivot, angleSupplier)`: Rotates named OBJ group about `pivot` around `axis` by `angleSupplier.applyAsDouble(be, partialTick)`.
- `.rotateY(name, pivotX, pivotZ, angleSupplier)`: Rotates named OBJ group about Y axis at `(pivotX, 0, pivotZ)`.
- `.translatingPart(name, directionVector, offsetSupplier)`: Translates named OBJ group along `directionVector` by `offsetSupplier.applyAsDouble(be, partialTick)`.
- `.blockTransform((be, anim) -> ...)`: Applies base model translation/rotation prior to part hierarchy.
- `.part(name)`: Adds static OBJ group. Must match exact group header `o <name>` in the OBJ file.
