# Development Guide

This document covers everything a contributor needs to compile, run and
extend **HBM's Nuclear Tech Modernized** from source. It is the single
entry point for build / datagen / dev-loop knowledge — the README only
links here.

> Most narrative documentation in this repository is written in Russian.
> This guide is in English on purpose so that new contributors arriving
> via GitHub can get unstuck quickly.

## 1. Prerequisites

| Tool | Version | Notes |
| --- | --- | --- |
| JDK | **17** for CI / general dev; **21** is also OK and is what `versions/1.20.1-forge/gradle.properties` pins via `org.gradle.java.home` | Mojang's mappings and ModDevGradle both work with JDK 17+. |
| Git | any recent | Submodules are not used. |
| Disk | ~3 GB free | For dependency caches and decompiled MC sources. |
| Python | 3.10+ | Optional, only needed for the helper scripts under `scripts/` and `tools/`. |

The Gradle wrapper (`./gradlew`) downloads its own Gradle distribution
(8.7, see `.github/workflows/ci.yml`); do not install Gradle yourself.

## 2. Repository layout

```
HBM-Modernized/
├── build.fabric.gradle.kts      # platform script: Fabric / Loom
├── build.forge.gradle.kts       # platform script: Legacy Forge / ModDevGradle
├── build.neoforge.gradle.kts    # platform script: NeoForge / ModDevGradle
├── build-logic/                 # included build with the `mod-platform` convention plugin
├── settings.gradle.kts          # declares all (mc-version × loader) combinations
├── stonecutter.gradle.kts       # Stonecutter root config (active version, swaps)
├── .sc_active_version           # active Stonecutter version, e.g. `1.20.1-forge`
├── versions/<version>/          # per-target gradle.properties (deps versions)
├── src/main/java/com/hbm_m/     # shared source for every loader
├── src/main/resources/          # shared assets / data
│   ├── mixins.hbm_m.json        # mixin config
│   └── aw/<mcver>.cfg|.accesswidener  # per-MC-version AT / AW files
├── src/generated/resources/     # datagen output (regenerated, but committed)
├── scripts/                     # Python helpers run by hand or in CI
└── tools/                       # one-off generators (model JSONs, …)
```

The build is driven by [Stonecutter](https://stonecutter.kikugie.dev/).
`settings.gradle.kts` enumerates the supported targets:

| MC version | Loader(s) | Subproject path |
| --- | --- | --- |
| 1.20.1   | `forge`,    `fabric`        | `:1.20.1-forge`,   `:1.20.1-fabric` |
| 1.21.1   | `neoforge`, `fabric`        | `:1.21.1-neoforge`, `:1.21.1-fabric` |
| 1.21.11  | `neoforge`, `fabric`        | `:1.21.11-neoforge`, `:1.21.11-fabric` |

`stonecutter.gradle.kts` registers `mod_version`, `mod_id`, `mod_name`,
`minecraft` swaps plus loader constants (`forge`, `fabric`, `neoforge`).
This drives the comment-based conditionals you will see throughout the
Java sources (see [§7](#7-stonecutter-cheat-sheet)).

## 3. Picking an active version

Most Gradle tasks dispatch to **one** target at a time. The current
target lives in `.sc_active_version`. To switch:

```bash
# Switch the active version (writes .sc_active_version for you)
./gradlew "Reset active project" "Set active project to 1.21.1-neoforge"
# …or just edit .sc_active_version manually and re-import the project.
```

Stonecutter's "Set active project to …" tasks are listed under the
`stonecutter` group in `./gradlew tasks`.

The vcs / "canonical" version used for fresh checkouts is set in
`settings.gradle.kts`:

```kotlin
vcsVersion = "1.20.1-forge"
```

## 4. Common Gradle commands

All commands below assume you have set the active version (or you pass
the subproject explicitly with `:1.20.1-forge:<task>` etc.).

```bash
# Build the jar for the active target
./gradlew build

# Run the client / server / data generator in the dev environment
./gradlew runClient
./gradlew runServer
./gradlew runData      # writes into src/generated/resources

# Build every (mc × loader) target at once (slow!)
./gradlew chiseledBuild

# Generate IDE run configurations (Fabric only; Forge/NeoForge use ModDevGradle)
./gradlew genSources
```

Forge / NeoForge run configurations are declared inside the platform
scripts (`runs { register("client") { … } }`) and surface in IntelliJ
once `./gradlew :…:tasks` has been executed at least once. Fabric uses
Loom's `ideConfigGenerated(true)`.

## 5. Data generation

Recipes, tags, item models, blockstates and language files are produced
by `com.hbm_m.datagen.DataGenerators` (Forge-only entry point — Fabric
excludes `com/hbm_m/datagen/**` from its source set).

Workflow:

1. Add or change builders under `src/main/java/com/hbm_m/datagen/…`.
2. Run `./gradlew runData` (or `./gradlew :1.20.1-forge:runData`).
3. Commit the changes under `src/generated/resources/`.

The repository deliberately commits the generated `assets` and `data`
folders so non-Forge targets and IDE tooling can resolve them without
running datagen first. CI runs datagen automatically on `push` to
`main` via [`.github/workflows/datagen.yml`](../.github/workflows/datagen.yml).

If you add a new language key, **add it in both `ru_ru` and `en_us`
blocks** of `ModLanguageProvider`. Run `python scripts/compare_lang.py`
to confirm the two blocks stay in sync before opening a PR.

## 6. Access transformers / wideners

Per-MC-version configuration lives under `src/main/resources/aw/`:

- `1.20.1.cfg`, `1.21.1.cfg`, `1.21.11.cfg` — Forge / NeoForge AT files.
- `1.20.1.accesswidener`, `1.21.1.accesswidener`,
  `1.21.11.accesswidener` — Fabric access wideners.

The platform scripts wire the right file in via
`accessTransformers.from(...)` (Forge) and
`loom.accessWidenerPath` (Fabric). When you need to expose a new
vanilla member, edit **all** files that need to see it; otherwise the
non-active target will fail to compile in CI.

## 7. Stonecutter cheat sheet

Stonecutter rewrites the source per target by toggling commented-out
regions. The common patterns used in this codebase:

```java
//? if forge {
@SubscribeEvent
public void onWorldLoad(LevelEvent.Load event) { … }
//?}

//? if fabric {
/* ServerWorldEvents.LOAD.register((server, level) -> { … });
*///?}
```

Rules:

- The `//? if <constant> {` and `//?}` markers are mandatory delimiters.
- The body for an inactive target is wrapped in a block comment so it
  remains valid Java that simply compiles to nothing.
- `constants["release"]`, the loader constants and version swaps are
  declared in `stonecutter.gradle.kts`.
- `replacements.regex(...)` in `build.fabric.gradle.kts` rewrites a few
  symbols (`ResourceLocation` ↔ `Identifier`, etc.) when targeting
  MC ≥ 1.21.11.

After editing Stonecutter-conditional code, run
`./gradlew stonecutterGenerate` (or any `runXxx` task — they depend on
it) to refresh the processed sources.

## 8. Helper scripts

| Script | What it does |
| --- | --- |
| [`scripts/compare_lang.py`](../scripts/compare_lang.py) | Reads `ModLanguageProvider.java`, parses the `ru_ru` and `en_us` blocks, and prints keys missing from one side. |
| [`scripts/check_registry_usage.py`](../scripts/check_registry_usage.py) | Verifies every `RegistrySupplier` declared in `ModBlocks` / `ModItems` is accepted exactly once in `MainRegistry`. Use `--fail-on-issues` in CI. |
| [`scripts/serve.py`](../scripts/serve.py) | Bare-bones static HTTP server on `127.0.0.1:8000` for inspecting source files in the browser. |
| [`tools/gen_fluid_duct_pipe_models.py`](../tools/gen_fluid_duct_pipe_models.py) | Regenerates the multipart model JSONs for the three fluid-pipe styles under `src/main/resources/assets/hbm_m/`. Re-run it whenever you touch the pipe model templates. |

These are intentionally simple `python` scripts — no virtualenv or
dependencies required.

## 9. Branches & PRs

- `main` is release-tracking; CI builds run against it.
- `Test-multi` is the active integration branch where most multi-loader
  work lands. PRs that touch the multi-version pipeline usually target
  `Test-multi` first and are merged into `main` only after stabilising.
- Feature branches typically follow the upstream HBM tradition of being
  named after the feature being added (`Acidizer`, `FMC`,
  `flywheel-test`, …). Keep them rebased on `Test-multi`.

## 10. Common pitfalls

- **Forgot to run datagen.** Symptom: missing block models / recipes
  / language strings in the dev client. Fix: `./gradlew runData` and
  commit the regenerated files under `src/generated/resources/`.
- **Edited only one access transformer / widener file.** Symptom: the
  Forge build is green but Fabric / NeoForge cannot resolve a vanilla
  field. Fix: add the entry to every file under `src/main/resources/aw/`
  that targets a relevant MC version.
- **Stonecutter conditional broken into the wrong shape.** Symptom:
  cryptic compile error pointing at a `//?}` line. Fix: ensure the
  inactive branch is fully wrapped in `/* … */` and the opening
  comment includes the `if <constant>` predicate.
- **Cloth Config crash on first launch.** The mod hard-requires
  [Cloth Config API](https://modrinth.com/mod/cloth-config) — its
  version is pinned via `deps.cloth-config` in `gradle.properties`.
  Install it alongside the mod, including in the dev runtime if you
  add a fresh `run/mods/` directory.
- **Iris/Oculus rendering quirks.** Most rendering issues with shader
  packs are tracked in `docs/systems/rendering_system_new.md`. Toggle
  `useIrisExtendedShaderPath` / `useInstancedStaticRendering` in
  `ModClothConfig` before filing a bug.

## 11. Further reading

- [`docs/systems/rendering_system_new.md`](systems/rendering_system_new.md)
  — exhaustive walkthrough of the OBJ rendering pipeline and shader
  compatibility paths.
- [`docs/systems/radiation-system.md`](systems/radiation-system.md) —
  chunk-radiation simulation, hazard registry, and the player tick loop.
- [`docs/systems/fluid-pipe-system.md`](systems/fluid-pipe-system.md) —
  MK2 fluid network model, ducts, valves and Forge-adapter bridging.
- [`docs/fluid-pipe-parity-test-matrix.md`](fluid-pipe-parity-test-matrix.md)
  — manual QA matrix for fluid network behaviour.
