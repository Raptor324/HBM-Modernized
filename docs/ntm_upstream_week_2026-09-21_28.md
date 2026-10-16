# NTM Upstream Week Report: 2026-09-21 → 2026-09-28

Repo: [HbmMods/Hbm-s-Nuclear-Tech-GIT](https://github.com/HbmMods/Hbm-s-Nuclear-Tech-GIT) (master)
Snapshot: 2026-09-28, HEAD `456c73d1`, version **1.0.27** build **5808**.
Prev. report: `docs/ntm_upstream_week_2026-09-14_21.md`.

## Overview

A quieter week than the previous one: **36 commits**, **~13 merged PRs**, a big chunk of
activity being QMAW (quality-of-life/recipe pages) follow-ups and the start of a large
internal cleanup (old fluid API package removal, FENSU removal, weapon damage config).
Mainline is clearly in a "refactor + content polish" phase, likely post-1.0.27 release.

## Merged PRs

| PR | Title | Author | Merged | Delta |
|---|---|---|---|---|
| #3355 | FEL Remodel (new OBJ + texture) | legendarydoge30 | 09-27 | +2367/−5174 (2 files) |
| #3350 | Bookmarks in Multi Fluid Identifier (MMB click) | Xpl0itR | 09-27 | +99/−16 |
| #3348 | Pipe breaking "thingy" — breaking a duct breaks only matching fluid-type line | WolfEclipses | 09-27 | +17/−2 |
| #3343 | OC compat: radar returns entity id, LaunchPad `launchEntity` (closes #3341) | Voxelstice | 09-27 | +15/−2 |
| #3321 | Nuclear submarine wreck structure (ocean) | TheMadLadRulz89 | 09-27 | +8 (structures) |
| #3339 | New Compact Compressor model | legendarydoge30 | 09-22 | +6500/−43 |
| #3330 | Arty over radio — RoR targetting mode switch (auto/manual) + coord separator change | Xpl0itR | 09-21 | +59/−3 |
| #3326 | Crash prevention in worldgen: skip structures with missing/mismatched NBT, log instead of crash | EXPLOSIVEGAMER | 09-21 | +16/−2 |
| #3323 | Biogas/fuel QMAW page | WolfEclipses | 09-21 | +22 |
| #3317 | Osmium QMAW page | WolfEclipses | 09-21 | +11 |
| #3315 | RoR commands for more machines (take 2) | Xpl0itR | 09-21 | +216/−7, 8 files |

Note: #3339's commit `9901a2c8 "portdbg"` + `f9f92503 "*crunch*"` indicate the new
compressor renderer needed follow-up fixes right after merge.

## Notable direct commits (no PR)

- **`2393b6de` "death of the old fluid api package"** (Boblet, 09-21) — removed
  `api.hbm.fluid.IFluidConnector` / `IFluidConnectorBlock`; legacy fluid API interfaces
  are being deleted. **Relevant to our port**: `FluidDuct`/connector parity surface is
  moving upstream.
- **`649bf90b` "configurable weapon damage"** (Boblet, 09-22) — +500/−111 across 53 files;
  all `XFactory*` gun classes refactored onto a new `GunValues` class (also touched by
  `2933713e`, `XFactory357`/`Receiver` fixups). Weapon damage is now server-config driven.
- **`d0abf830` "new fel"** (09-27) — replacement `fel.obj` + texture, part of #3355.
- **`d026e0e8` "i could go for some cheesy toast right about now"** (HbmMods, 09-24) —
  **FENSU removed entirely** (`TileEntityMachineFENSU`, `MachineFENSU`, `RenderFENSU`,
  `CanneryFEnSU` deleted, 47 files, +539/−1067); battery/RTG/assembly-factory/chemical-factory
  touched — looks like a storage-machine consolidation.
- **`d6ab59b4` "hope that's over now"** (09-27) — TilePort/port-manager rework:
  new `ModulePortManPower`, changes to Soyuz/Lambda launchpads, UniNos
  `FluidNetProvider`/`PowerNetProvider`, `TileEntityLoadedBase`, chimney base. Follows the
  `portdbg` instrumentation from 09-22.
- **`f27c3599` "yeag"** (09-27) — BlastDoor (`TileEntityBlastDoor`/`BlastDoor`), `IParse`,
  `ServerConfig`, changelog — blast door changes again (we track these for door parity).
- **`456c73d1` "ok that's enough for today"** (09-27) — version/changelog/TODO bump.

## Open PRs of interest (not merged)

- **#3356** Animated Control Units (TheVitya2127) — new, 09-28
- **#3322** "its a Airplane luigi!" — crashed airliner structure (closed unmerged 09-28)
- **#3318** Camp structures (snowy biomes) — closed unmerged 09-28
- **#3347** Missiles (EXPLOSIVEGAMER)
- **#3346** Pipe anchors for pneumatic/exhaust networks (Xpl0itR)
- **#3269** Chocolate / #3336 item-turret mob filter / #3175 pills / #2413 ru_RU + I18n
  (46 files, still open) / #3162 new Electric Press model (closed unmerged 09-27)

## Relevance to HBM-Modernized

1. **Old fluid API removal (`2393b6de`)** — our fluid-duct parity work
   ([[fluid-duct-connect-parity-0927]]) referenced the legacy `IFluidConnector` contract;
   upstream is deleting it, so future parity checks should diff against the new package.
2. **Duct breaking per fluid type (#3348)** — new behavior we likely don't have; candidate
   parity gap for our `WireBlock`/fluid ducts.
3. **Multi Fluid Identifier bookmarks (#3350)** — GUI feature; check whether we ported
   the MFI at all.
4. **FEL remodel (#3355)** — new `fel.obj`/texture; we have FEL listed as a stub
   (completion audit) — new model is a free asset pickup if/when ported.
5. **FENSU removal** — if we ported FENSU, upstream has deleted it; verify our status.
6. **Configurable weapon damage (`649bf90b`)** — large Sedna weapon refactor via
   `GunValues`; will matter when we port more guns.
7. **TilePort/`ModulePortManPower` rework** — continues the TilePort pattern we noted
   as port candidates last week; API still in flux, wait for it to settle.
8. **Worldgen crash prevention (#3326)** — cheap defensive pattern worth mirroring in our
   structure gen if we have one.
