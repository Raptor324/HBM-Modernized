# Nucleus CPU-Path Critical Audit & CrankShaft 1.5.x Borrow List

**Date:** 2026-09-25
**Bases audited:**
- **CrankShaft** `C:\Projects\CrankShaft`, branch `26.2`, HEAD `607b263` (v1.5.3)
- **Nucleus** `HBM-Modernized`, branch `funni-stuff`, HEAD `803e89ac` + uncommitted working tree (includes dirty-skip fastpath and GPU culling implemented same week)

**Supersedes:** the roadmap status and performance chapters of `crankshaft_nucleus_comparative_study.md` (that document covered CrankShaft 1.4.0 and a Nucleus without GPU culling, Hi-Z SPD, compute scatter, or the dirty-skip roster). The architectural chapters (§2–§4) of the old study remain valid background; this file is the current-state audit.

---

## 1. Executive summary

After the September wave of work (dispatcher bypass, clean-frame reuse, span-diff uploads, GPU frustum+Hi-Z culling, dirty-skip roster assert), a **fully static scene in default mode (MDI + GPU cull, no Iris) now converges to near-zero per-machine CPU cost**. What remains is:

1. A **constant per-frame coordinator overhead** in `MdiBatchCoordinator` that does not scale with scene dirt: the fade-slot list is re-allocated and re-sorted **every frame even when nothing faded**, the whole indirect command buffer is rebuilt in Java and re-uploaded via `glBufferSubData` every frame, ~20 GL state queries run per dispatch, and the lightmap is regenerated a second time on top of vanilla's own call.
2. The **48-block animation zone**: every machine with hooks or animated parts inside it runs the full `collectRender` path each frame (animators, hook vertex writes, `recordMatchesBuffer` float-compare per part, matrix decomposition). This is now the largest per-machine CPU block.
3. **Iris tiers**: Tier 1 rebakes all instances every frame with no diffing; Tier 3 does per-instance `glDrawElements` + 3 matrix uploads; the shadow collector does an O(entries) linear scan per record and ~30 KB of native buffer churn per part renderer per frame.
4. A handful of **hard stalls and hazards**: a 1 Hz synchronous `glGetBufferSubData` readback of the indirect buffer, an up-to-~17 ms fence wait on staging ring wrap (single fence per frame), and raw `GL11/GL13` texture binds on unit 10 that bypass the `GlStateManager` bind cache — the exact recipe for the stale-binding bug CrankShaft fixed in 1.5.2.

CrankShaft 1.5.3's answer to every one of these is structural: **CPU never builds per-instance draw state, and even per-draw command *counts* are patched by a GPU apply pass**. The borrow list (§5) ranks what to port.

---

## 2. CrankShaft 1.5.3: what the CPU actually does per frame

All citations relative to `C:\Projects\CrankShaft`. The design invariant: **zero CPU readback, zero per-instance CPU work for static instances, counts patched on GPU.**

### 2.1 Off-thread by default

- The frame plan (visual `update()`, instancer page compaction, light re-collection) runs on a dedicated ForkJoinPool; the render thread launches it at `onStartLevelRender` and **only joins it later** at `afterEntities` (`VisualizationManagerImpl.java:244-256, 267-270`, flag rendezvous at `:668-673`).
- **Concurrent extraction (new in 1.5.0)**: vanilla entity/BE render-state extraction is deferred into batches and binary-split onto the worker pool (`ConcurrentExtraction.java:53-156`, `MIN_BATCH=16`, `SLICE=4`); renderers touching `Font`/map textures are excluded (`VANILLA_EXCLUDED :42-44`), and each renderer class gets one serial warm-up because NeoForge's per-class extension cache is not synchronized on first fill (`:159-174`).
- Instance add/delete/setChanged are lock-free off-thread (CAS on `InstancePage.valid`, `AtomicBitSet` dirty markers — `IndirectInstancer.java:475-575`); the render-thread `parallelUpdate` (`:236-258`) only merges/compacts pages.

### 2.2 Render-thread per-frame work — O(#instancers + #draws), never O(#instances)

`IndirectDrawManager.prepare` (`IndirectDrawManager.java:176-277`):

- `IndirectInstancer.update` early-outs unless the model index changed or `validityChanged` is non-empty (`IndirectInstancer.java:119-145`).
- All uploads are dirty-page-gated: instance spans (`:157-212`), descriptors via `changedFrames` BitSet (`ObjectStorage.java:52-64`), light sections (`LightStorage.java:191-196`), matrix/environments on demand.
- `sortDraws()` (CPU `UBER_DRAW_COMPARATOR`, `:64-70`) runs only when the draw list changed (`:186-193, 248-251`); instance-count changes never dirty it.
- Per-frame serialization is limited to model records + draw command **headers** into mapped staging memory (`writeCommands`/`writeModels`, `:402-423`) — O(#draws) memcpy-style writes.
- **The `instanceCount` inside each indirect command is patched by the GPU**: the cull compute writes per-model counts and `apply.glsl:16-25` writes `drawCommands[drawIndex].instanceCount = models[modelIndex].instanceCount` in VRAM. CPU never builds per-instance draw commands, and never re-serializes counts when only visibility changed.

### 2.3 Dirty-only staging + fenced FIFO reclaim

- 16 MiB persistently mapped ring (`StagingBuffer.java:20-22`); non-fitting copies are split tail/head (`:125-145`); overflow falls back to `glNamedBufferData` + copy (`:319-340`).
- `flush()` groups transfers per destination buffer into a `ScatterList` and issues **one scatter compute dispatch per VBO** (`:247-298`, `scatter.glsl:1-51`).
- Reclaim is a **FIFO of `FencedRegion`s**: `reclaim()` polls only the head with a single non-blocking `glGetSynciv(GL_SYNC_STATUS)` (`GlFence.java:15-21`) and stops at the first unsignaled fence — no sleeping, no wait loops, and capacity is freed as soon as the GPU passes it (`StagingBuffer.java:187, 193-206`; polled every frame at `IndirectDrawManager.java:212`). Slab slots additionally use two-frame deferred free (`HostSlab.java:38-64`).

### 2.4 Adaptive occlusion gate with hysteresis

`IndirectDrawManager.java:100-104, 242-246`: `OCCLUSION_VERTICES = 1<<18`. When two-phase Hi-Z occlusion is ON it turns OFF below the threshold; it turns back ON only above **2×** the threshold. When OFF, the engine elides not just the occlusion test but the **entire pyramid rebuild, the pass-2 buffer copies, and both extra dispatches** (`:310-326, 360-372`). Pyramid mips are reallocated only when the render-target size changes (`DepthPyramid.java:155-178`).

### 2.5 The 1.5.2 stale-binding bug (the lesson for Nucleus)

The Hi-Z pyramid texture is created with DSA (`GL45.glCreateTextures`, `DepthPyramid.java:168`) — so Mojang's `GlStateManager` texture cache never learned about it — but binding went through `GlStateManager._bindTexture` (`:102-105`). On window resize, delete used the raw `glDeleteTextures` while the state cache kept the dead id as "currently bound"; after re-creation the new texture was considered already bound and **the cull shader silently sampled a deleted texture**. The fix (commit `059ed3e`) is a custom `deleteTexture` helper (`DepthPyramid.java:144-153`) that raw-deletes and then **sweeps `GlStateManager.TEXTURES`, resetting any slot whose binding == id to −1**.

Nucleus has the mirror-image hazard today (§3.5, H1).

### 2.6 Subgroup compaction (current 1.5.3 shape)

`cull.glsl:7` — `local_size_x = 32`, one workgroup = one 32-instance page. Runtime verification that workgroup == subgroup (`subgroupBallot(gl_SubgroupInvocationID == gl_LocalInvocationID.x) == 0xFFFFFFFF`, `:160-176`); on mismatch (64-wide AMD wavefronts) or missing `GL_KHR_shader_subgroup_basic/_ballot` extension, falls back to `shared uint` + `atomicOr` words + `barrier()` (`:192-224`). Compaction: one `atomicAdd` per subgroup via `subgroupElect` + `subgroupBroadcastFirst` + `subgroupBallotExclusiveBitCount` (`:227-237`); per-lane `atomicAdd` otherwise (`:238-244`). Extensions are declared through `IndirectPrograms.java:125-131` only when `GlCompat` reports the KHR capability — the portable route, no AMD-specific path needed.

---

## 3. Nucleus: per-frame CPU inventory (working tree, funni-stuff + uncommitted)

All citations relative to `src/main/java/` of HBM-Modernized. Default mode = MDI + GPU cull (`mode=GPU`), no Iris, `nucleusRenderDirtySkip` on.

### 3.1 Constant per-frame cost (independent of machine count)

| Cost | Where | Notes |
|---|---|---|
| LIVE sweep + 2 `Matrix4f` allocs | `client/render/NucleusDispatcherBypass.java:126-144` | `removeIf` over the full LIVE set every frame |
| Retained-list rebuild, per-record atlas lookup | `client/render/MdiBatchCoordinator.java:777-874` | `dropUnassertedRetained` iterator + `new ArrayList` draw list each frame |
| **Fade-slot rebuild + TimSort — every frame, even fully clean** | `MdiBatchCoordinator.java:890-907` | only `partitionOpaqueFirst` is retained (856-869); the *sorted* fade list is not |
| **Command buffer rebuild + full `glBufferSubData`** | `MdiBatchCoordinator.java:1059-1091, 1191-1193` | `memAlloc(nCmd*32)` + byte fill + re-upload of the entire indirect buffer per frame |
| ~20 GL state queries (`glGetInteger`/`glIsEnabled`/`glGetBoolean`) | `MdiBatchCoordinator.java:1114-1134` | per dispatch |
| **Duplicate `updateLightTexture`** | `MdiBatchCoordinator.java:1143-1150` (also `IrisInstancedBatchRenderer.java:438`) | vanilla `GameRenderer` already rebuilds the 256×256 light texture once per frame |
| GPU cull dispatch (1 total, one workgroup per command) + 4-barrier | `client/render/culling/NucleusGpuCuller.java:264-330` | unconditional in GPU mode; cheap below the latch but not free |
| Hi-Z SPD rebuild — gated by 4096-instance latch, hysteresis 2048 | `MdiBatchCoordinator.java:1100-1111, 1200-1202`; `culling/HiZDepthPyramid.java:201-262` | frustum-only mode skips the pyramid (1200) — matches CrankShaft's gate design |
| **1 Hz `glGetBufferSubData` of the whole indirect buffer** | `MdiBatchCoordinator.java:1228-1255` | full pipeline sync stall once per second |
| Staging fence | `client/render/PersistentUploadStaging.java:197-231` | one fence per frame; `glClientWaitSync` (1 ms + 8×2 ms) only on ring overlap |
| Iris shadow collector churn | `client/render/IrisShadowBatchCollector.java:63, 371-378` | `memFree` + `memAllocFloat(256×30)` per part renderer per frame |

### 3.2 Per-machine cost by path

| Path | Cost / frame | Evidence |
|---|---|---|
| Fast path (clean, GPU mode, outside anim zone) | distSq + 2 config reads + CHM lookups + 1 long compare per part | `NucleusDispatcherBypass.java:172-203`, `machine/MachineBer.java:280-353`, `client/render/InstancedStaticPartRenderer.java:574-578` |
| TTL refresh / dirty (any distance) | matrix decomposition per part, bbox fetch, fade ×2, 8-corner light on TTL expiry, **`recordMatchesBuffer` ≈30 float compares per part**, hook vertex writes | `MachineBer.renderAll:422-624`, `InstancedStaticPartRenderer.java:379-385, 429-455` |
| **Inside animation zone (48 blocks default)** | **full `collectRender` every frame** for machines with hooks/animated parts; partial fast path only skips static-part compares | gate at `MachineBer.tryFastAssertRender:308-317`; partial path `MachineBer.java:517-524`; distance `ModClothConfig.java:174` |
| CPU-occlusion mode (opt-in) | 15-ray DDA × 100 steps per machine per frame; cross-frame reuse disabled under instancing | `culling/OcclusionCullingHelper.java:258-305, 309, 359-428` |
| Iris Tier 3 (no encoder) | per-instance 2-3 matrix inverts + 3 `glUniformMatrix4fv` + 1 `glDrawElements` | `IrisInstancedBatchRenderer.java:609-668` |
| Iris Tier 1 | full `glBufferSubData` of all instance records + rebake of **all** instances, no diffing | `client/render/NucleusGpuBaker.java:555-569, 248-332` |
| Iris shadow pass | O(entries) linear scan per `record()` + 30-float append | `IrisShadowBatchCollector.java:132-218, 140-145` |

### 3.3 Ranked hotspots (default config)

1. **Animation-zone full rebuild** — the largest remaining per-machine CPU block; the dirty-skip fastpath deliberately does not cover it (hooks and animators live only inside the zone).
2. **Coordinator per-frame overhead that ignores scene dirt** — fade-slot sort, command-buffer rebuild + full re-upload, 20 GL queries, duplicate lightmap regen. All constant, all every frame.
3. **1 Hz sync readback** — a hard pipeline stall landing mid-frame.
4. **CPU-occlusion mode** — 15-ray march per machine per frame (only when user selects CPU mode; cross-frame reuse deliberately disabled under instancing).
5. **Iris Tier 1/3 + shadow collector** — rebake-all with no diff, per-instance draws, linear scan + native churn.
6. **Snapshot copy per dirty renderer** — bounded and correct (`MdiBatchCoordinator.submit:623-639`); not a priority.

### 3.4 Mitigation status (verified in working tree)

Implemented and confirmed: dispatcher bypass; dirty-skip roster assert (TTL 15 ticks, phase spread `asLong()&7`); partial fast path inside the anim zone (static parts); `submitClean` + retained draw list + snapshot reuse; span-diff uploads + persistent staging + compute scatter (gated ≥16 spans, ≥4 KB, non-Intel); GPU frustum+Hi-Z culling with adaptive latch + hysteresis; anim-delta cache; light TTL + shared 8-corner sample; fade quantization to 1/255; world-record direct `memPutFloat` writes.

**Absent / dead:**
- `MdiBatchCoordinator.compactVisibleInstances` — empty body, zero callers (`:1453-1455`).
- `coalescePendingByRenderer` — never invoked (`:669, 1416`).
- Deferred-draw path unreachable (`endFrame(true)` never called — only `endFrame(false)` at `culling/InstancedRenderFrame.java:108`), so `scheduleDeferredDraw`, `DeferredDraw`, `presentScheduledDraw` are dead.
- Cached-redraw inter-tick path dead: `publishCachedRedraw` has no callers → `redrawCachedIfAny` always early-outs (`:414, 441`).
- `ModClothConfig.instanceVboOrphanBeforeUpload` — no consumer (orphaning removed from upload path).
- `NucleusDispatcherBypass.noteShadowPassStart` / `IrisShadowBatchCollector.stripShadowKeyBit` — identity no-ops.

### 3.5 Hazards

- **H1 — `GlStateManager` bind-cache bypass on unit 10**: `HiZDepthPyramid.java:221-222` and `NucleusGpuCuller.java:272-273` use raw `GL13.glActiveTexture(GL_TEXTURE10)` + `glBindTexture`; the previous unit-10 binding is never restored, so a stale entry persists into the rest of the frame while `GlStateManager`'s per-unit cache believes something else is bound. The pyramid texture is also delete/recreated on resize (`HiZDepthPyramid.java:147-173`) with no cache sweep. This is exactly the 1.5.2 CrankShaft bug pattern — apply their `deleteTexture` sweep + restore the previous binding.
- **H2 — 1 Hz sync readback** (§3.1) — remove or debug-flag it.
- **H3 — fence stall on ring wrap**: one fence per frame covering the whole written range; a large dirty frame followed by heavy uploads can stall ~17 ms. CrankShaft's FIFO-of-fences reclaim eliminates the pattern.
- **H4 — config split-brain**: legacy `enableOcclusionCulling` (default false) vs `occlusionCullingMode=CPU` are disjoint flags governing one subsystem; with CPU mode selected but the legacy flag off, frustum capture and cache pruning are skipped while ray marching still runs, and the cache grows to its 16384 cap with per-frame `removeIf` allocation (`InstancedRenderFrame.java:76-82`, `OcclusionCullingHelper.java:442-457`).
- **H5 — Embeddium re-fires `AFTER_BLOCK_ENTITIES`**; each present re-dispatches (dedup only via submit max-rule) (`MdiBatchCoordinator.java:1412-1414`, `InstancedRenderFrame.java:844-846`).

---

## 4. Head-to-head: why CrankShaft needs less CPU

| Concern | CrankShaft 1.5.3 | Nucleus today |
|---|---|---|
| Static instance, per frame | **Zero** CPU: page not dirty → no upload, no descriptor write; visibility + counts computed on GPU | Near-zero via dirty-skip roster + skip-write, but the coordinator still re-sorts fade slots and re-uploads the whole command buffer |
| Indirect command counts | GPU-patched by `apply.glsl`; CPU writes headers only O(#draws) | CPU rebuilds and re-uploads the full buffer every frame |
| Visibility culling | Compute, per 32-instance page, subgroup ballot or atomicOr fallback | Compute, one workgroup **per command** (coarser; 4096-instance commands processed serially by one workgroup) |
| Occlusion gating | Vertex-count latch with ×2 hysteresis; OFF elides pyramid + copies + 2 dispatches | Instance-count latch (4096/2048 hysteresis); frustum-only mode skips pyramid rebuild — equivalent design, coarser metric (instances vs vertices) |
| Staging reclaim | FIFO of fences, head-only non-blocking poll | Single fence/frame; blocking wait on ring overlap |
| Visual update | Off-thread plans + concurrent extraction | All on render thread (dirty-skip narrows the work but does not move it) |
| Depth pyramid recreate | DSA + explicit `GlStateManager.TEXTURES` sweep on delete | DSA-adjacent raw binds on unit 10, no cache sweep — the bug is one `GlStateManager`-side bind away |

---

## 5. Borrow list (ranked)

1. **GPU-patched command counts (apply-pass pattern).** Stop rebuilding the indirect buffer in Java every frame. Write command headers only when the draw set changes; let the cull compute (which already runs one workgroup per command) write `instanceCount` directly into the indirect buffer, with a `GL_COMMAND_BARRIER_BIT` before the draw. This deletes hotspot #2 (constant per-frame rebuild + upload) and makes clean frames truly zero-CPU. Files: `MdiBatchCoordinator.java:1059-1091, 1191-1193`, `NucleusGpuCuller.java:264-330`.
2. **FencedRegion FIFO reclaim** for `PersistentUploadStaging`: push one fence per flush into a FIFO, reclaim from the head with non-blocking `glGetSynciv` polls only (CrankShaft `StagingBuffer.java:187-206`, `GlFence.java:15-21`). Kills the H3 wrap stall without busy-waiting.
3. **Stale-binding defense** (1.5.2 lesson): sweep `GlStateManager.TEXTURES` on pyramid delete/recreate and restore the unit-10 binding after `HiZDepthPyramid`/`NucleusGpuCuller` use — or route binds through `GlStateManager` and invalidate the slot explicitly. Files: `HiZDepthPyramid.java:147-173, 221-222, 261`, `NucleusGpuCuller.java:272-273, 328`.
4. **Retain the sorted fade list** across clean frames (invalidate on fade-quantum change or TTL refresh), and cache the GL state query results once instead of 20 queries per dispatch. Files: `MdiBatchCoordinator.java:890-907, 1114-1134`.
5. **Subgroup ballot + page granularity in `nucleus_cull`** — keep as the previously-ranked item: switch the cull shader to one workgroup per 32-instance page with `GL_KHR_shader_subgroup` ballot compaction and the runtime workgroup==subgroup check + atomicOr fallback. Removes the 4096-serial-instance workgroup and atomic contention. (Deferred from the prior audit; unchanged verdict.)
6. **Remove the 1 Hz readback** (or `-Dhbm.mdiDiag` gate it) and the duplicate `updateLightTexture` call. Files: `MdiBatchCoordinator.java:1143-1150, 1228-1255`, `IrisInstancedBatchRenderer.java:438`.
7. **Tier 1 diffed rebake**: upload/bake only changed instance spans under Iris (mirror `GpuSpanUploader` diffing into `NucleusGpuBaker.uploadInstances`), and replace the shadow collector's linear scan with a per-renderer slot append. Files: `NucleusGpuBaker.java:555-569`, `IrisShadowBatchCollector.java:140-145, 371-378`.
8. **Concurrent extraction — the long-term answer to the anim zone.** CrankShaft moved visual update + vanilla extraction onto worker plans with a flag rendezvous. Full port is out of scope for parity constraints (vanilla `PoseStack`, light access, hooks are render-thread-bound), but the *anim-zone full rebuild* (hotspot #1) is the natural candidate for the same treatment in a limited form: TTL-refresh rebuilds already have well-defined inputs. Treat as a design study, not an immediate port.
9. **Cleanup batch (no behavior change)**: delete dead deferred-draw / cached-redraw / `compactVisibleInstances` / `coalescePendingByRenderer` code, the vestigial `instanceVboOrphanBeforeUpload` config, identity no-ops; unify `enableOcclusionCulling` vs `occlusionCullingMode` into one flag (H4); cap the `OcclusionCullingHelper.onFrameStart` per-frame `removeIf` allocation (only runs when legacy occlusion is enabled).

**Explicitly not borrowed** (unchanged from prior audit): RHI/RenderPass layer, vanilla-replay OIT, terrain/meshlet path, reversed-Z (26.2-only), mid-frame `triggerFallback`, and — as a cautionary note — CrankShaft 1.5.0 itself **deleted** its geometry-aware lighting (~1700 lines) after 1.4.0: features that couple lighting into the extraction path carry a maintenance cost their own authors eventually rejected.

---

## 6. Verification checklist for implementers

- After item 1: confirm `glMultiDrawElementsIndirect` sees GPU-patched counts on both targets; add `GL_COMMAND_BARRIER_BIT` before the draw; verify Embeddium double-present (H5) does not double-patch counts (cull compute must be idempotent per present, or gated per frame).
- After item 3: resize the window with GPU cull active and confirm the cull shader samples the fresh pyramid (temporary `glGetTexImage` debug or render pyramid mip 0 to screen).
- After item 2: stress with a 200-machine dirty storm and confirm no `glClientWaitSync` blocks >1 ms in `PersistentUploadStaging`.
- Regression guard: `-Dhbm.dirtySkip=false`, `-Dhbm.gpuCull.minInstances`, and the existing `MdiRenderDiag` counters must still function.

---

## 7. Status update (evening 0925) — quick wins implemented

Implemented same day (working tree, both targets compile through these changes; repo-level build red at audit time due to a **concurrent session's in-progress energy-API refactor** in `BatterySocketBlockEntity`/`api/energy`/`PlatformHooks` — unrelated to this work):

- **H1 fixed (stale-binding defense):** `HiZDepthPyramid` and `NucleusGpuCuller` now bind through `GlStateManager._activeTexture`/`_bindTexture` (cache-coherent, previous unit-10 state restored to cache semantics), and all pyramid/placeholder deletes go through `GlStateManager._deleteTexture`, which sweeps the per-unit cache in both supported versions (no reflection needed — unlike MC 26.2, `_deleteTexture` has no numTextures skew here).
- **H2 fixed:** the 1 Hz `glGetBufferSubData` readback is gated on actual consumers — F3 overlay open (`RenderHooks.isDebugScreenVisible()`) or `mdiDebugLogDispatch`.
- **Duplicate lightmap removed:** coordinator's inline `updateLightTexture` deleted (vanilla updates at `GameRenderer.renderLevel` start; `RenderFrameLight.ensureLightTextureUpdated()` runs at present start); `IrisInstancedBatchRenderer`'s direct call replaced with the same at-most-once guard.
- **H4 fixed:** CPU occlusion frustum capture / cache pruning now keyed on `getEffectiveOcclusionCullingMode() == CPU` (single source of truth) in `InstancedRenderFrame`, `ClientModEvents`, and the F3 culled-counter line — no longer on the mirrored legacy boolean.
- **Dead code removed:** `DeferredDraw`/`scheduledDraw`/`scheduleDeferredDraw`/`presentScheduledDraw`/`executeDeferredDraw`/`cancelScheduledDraw`, cached-redraw path (`cachedRedraw`/`redrawCachedIfAny`/`publishCachedRedraw`, `clearCachedRedraw` slimmed to `dropAllRetained`), `compactVisibleInstances`, `coalescePendingByRenderer` (max-submit rule kept inline in `submit`), `refreshDrawListAtlasSlots`, `prepareMdiDraw` boolean param, `noteShadowPassStart` + its call, vestigial `instanceVboOrphanBeforeUpload` config (+ schema registration + datagen/generated lang keys).
- **Minor:** `OcclusionCullingHelper.onFrameStart` cache prune no longer allocates a `BlockPos` per cached entry per frame (Manhattan distance from the packed key).
- **Evaluated and rejected — GL state query caching:** replacing the ~13 per-dispatch `glGet*` state reads with reflected `GlStateManager` cache reads (CrankShaft-style) is unsafe here: on Forge 1.20.1 production the cache fields are SRG-renamed (reflection by mojmap names fails), structural reflection is fragile, and GL-truth reads are the *correct* semantics for a restore block when third-party mods bind raw GL. Cost (~13 cheap state queries/frame) does not justify it.
- **OIT verdict (question raised in review):** not borrowed — our fading is *single-layer* alpha already globally sorted back-to-front (the common case OIT optimizes is rarely hit), while CrankShaft's wavelet/MLAB chains cost 2–4 fullscreen passes plus per-pixel SSBO/A-buffer memory, and their "vanilla-replay" integration relies on the MC 26.2 RHI. Under Iris, translucent ownership belongs to the shaderpack (Tiers 1–3 draw through pack programs), so an OIT composite would fight the pack for the same pixels. Revisit only if real multi-layer translucent stacks appear in Nucleus content.

---

## 8. Status update (0926) — fade-freeze bug: root cause + fix

**User-reported symptom.** With a machine farm and the player flying away, static bases disappear by a hard cutoff — no dissolve. On approach the fade "comes alive" for a moment, then sticks at an arbitrary fraction (≈half) — the base stays semi-transparent until the model is forced out of the frustum and back, after which it snaps opaque.

### 8.1 Root cause — two defects in the dirty-skip fastpath (both working-tree, 0925)

The distance fade is a **pure function of camera position** (`RenderDistanceHelper.computeStaticFade`) with no event model: it changes on every frame of movement without any dirty flag. The only mechanisms that refresh the fade baked into an instance record are (a) a full `collectRender` and (b) the periodic TTL rebuild (`RenderDirtyTracker`, 15 ticks, phase-spread) — documented in the interface as the owner of light/fade refresh precisely because fade has no event source. The 0925 fastpath broke both:

1. **The roster assert refreshed the TTL stamp every frame.** `MachineBer.tryFastAssertRender` phase 2 called `tracker.onRenderCollected(...)` on every successful assert. Since a clean machine asserts every frame, `isRenderStale` never returned true again → the TTL rebuild — the only scheduled light/fade refresh — never fired. (The `collectRender` finally-block refresh is correct: it runs on full collects, which is exactly what the stamp is for.)

2. **The roster assert was fade-blind.** `InstancedStaticPartRenderer.canAssertInstance(posKey)` compared only the roster key (`pos.asLong()`); the fade float (record offset `InstBboxSize.w`) of the buffer slot was kept as-is. `assertCleanInstance` then re-armed the stale record verbatim, `submitClean` re-used the stale coordinator snapshot, and the machine kept drawing with whatever alpha was current at its last full collect.

Why each reported symptom follows:

- **Flying away → hard pop, no dissolve:** the record kept fade = 1.0 (opaque); the machine stayed fully opaque through the entire fade zone and vanished only at `NucleusDispatcherBypass.collectOne`'s `maxDist + 8` early return (or the GPU-cull at the cutoff) — a pop.
- **Approaching → fade "revives", sticks at ≈half:** entering the render distance triggers one full collect (fresh, low alpha). Roster churn (any machine crossing the far boundary shifts slot expectations → assert misses → full rebuild of machines after it in LIVE order) produced sporadic refreshes — the "revival". Once the roster stabilized, the assert succeeded every frame again and alpha froze at the last churn value.
- **Turn away/back unfreezes:** any incidental roster churn/anchor shift (`onRenderOriginChanged` → `NucleusRenderVersion.bump()` → global miss) forced full collects. The trick was an unreliable workaround, not a mechanism.
- Latent sibling: **light froze too** (8-corner light UV + sky darken live in the same records and share the same TTL) — stale machine brightness after dusk/weather changes.

### 8.2 Fix (working tree)

- `InstancedStaticPartRenderer`:
  - new single quantization point `quantizeFade(float)` (1/255, the value `memPutInstanceRecordAtBaseFloat`/`recordMatchesBuffer` already used);
  - new `canAssertInstance(long posKey, float expectedQuantizedFade)` — roster key check **plus exact equality of the buffer slot's quantized fade**. One `memGetFloat` per part; machines at fade = 1.0 (the overwhelming majority) still pass at zero cost.
- `MachineBer.tryFastAssertRender`:
  - computes `computeStaticFade(beRaw)` once; `fade < 0` (beyond cutoff) → full collect (its standard early exit);
  - phase 1 uses the fade-aware assert — a bucket crossing (1/255 step) misses and triggers a full collect, which rewrites the record and the MDI snapshot;
  - **phase 2 no longer refreshes the tracker stamp** — the TTL rebuild is restored as the owner of light/fade refresh (the documented design; the fastpath still skips 14 of 15 frames).
- `MachineBer.renderAll` partial fastpath (anim zone): the `staticFade >= 0.99` guard replaced by the same fade-equality assert — a fading machine is rebuilt only when its quantized alpha bucket actually changes, symmetric with the full fastpath.

Perf profile of the fix: fade-zone machines full-collect only on 1/255 bucket crossings (a few per second while moving — strictly cheaper than the pre-fastpath per-frame rebuild of *all* machines); everything else keeps the zero-cost assert. Smoothness is bounded by the existing 1/255 quantization (visually continuous, chosen for span-diff convergence).

### 8.3 Verification checklist

- Fly away from a farm: bases dissolve through the fade ring, no pop; fly back: fade rises continuously, no stuck fractions.
- `-Dhbm.dirtySkip=false` still bypasses the fastpath entirely (fade was always correct there).
- F3 → machines at night refresh brightness within ≤ 15 + 0..7 ticks (TTL restored).
- Both targets compile (verified 0926).

---

## 9. Status update (0926) — movement upload churn: root cause + fix (stable atlas windows)

**User-reported symptom.** The F3 "Uploads: N spans, X KB" counter spikes every frame while moving (flying around a machine farm), instead of converging to zero for a static scene.

### 9.1 Root causes — `MdiBatchCoordinator` atlas layout was positionally unstable

1. **Fade slots re-packed every frame** (`prepareMdiDraw`): fading instances were enumerated into a flat list sorted by *current* camera `distSq`, and each slot was uploaded at a *new running offset* after all opaque windows. While moving, the sort order changes continuously → every fading instance's atlas position changed every frame → span diff saw the entire moved window as different → **all fade slots uploaded every frame**.
2. **Opaque window cascade**: opaque windows were packed contiguously in retained-list order with a running upload offset. Any renderer's `opaqueCount` change (a machine crossing the 0.99 fade threshold — which happens constantly while flying through the fade ring) shifted **every later window** by ±34 floats; the `lastOpaqueUploadOffsetFloats` skip failed for all of them → full re-upload of the entire atlas tail per crossing.

Key enabling fact (verified in `nucleus_cull.comp`): the cull shader compacts **in place per command** — a survivor's compacted position equals its input position (`copyInstance(baseInstance + i, baseInstance + slot)`), and cross-command order equals indirect command order. Therefore instance data never needs to move for fade sorting or phase ordering — only the *command order* does. The CPU-side re-packing was pure waste.

### 9.2 Fix (working tree)

- **Stable per-Pending windows** in the atlas instance VBO: `atlasWindowBase`/`atlasWindowCap`/`windowUploaded`. Capacity grows ×2 on overflow; released windows become holes (tail-adjacent holes coalesce into the watermark); any (re)allocation triggers a full repack that compacts the layout and preserves `windowUploaded` for unchanged bases. `submit()` transfers the window from the replaced `Pending`; `uninstallRetained` releases it; `dropAllRetained` (world/reload/atlas reset) resets the whole layout.
- **`uploadWindowsToAtlas`** replaces `uploadInstancesToAtlas`: each snapshot is always uploaded at the same base (diff → only actually-changed records: fade quantum, light, machine enter/leave); a window untouched since its last successful upload with valid shadow is skipped entirely. `lastOpaqueUploadOffsetFloats` deleted.
- **Fade ordering is command-order only**: `FadeSlot.baseInstance = windowBase + recordIndex` assigned at enumeration (the post-sort reassignment loop removed), `SubDraw.baseInstance = windowBase`. The per-frame sort stays (F records, Java) but triggers **zero uploads**.
- Correctness of diffing into a reused hole rests on the shadow==GPU invariant: all instance writes go through `GpuSpanUploader`; capacity growth / anchor drift / atlas reset invalidate the shadow → full re-uploads.

**Expected residual churn** (bounded, real data changes only): fade-ring records on 1/255 bucket crossings (one 136-byte record per crossing), partition re-sorts within dirty snapshots (fading records only), anchor drift every 256 m of travel (full re-upload, logged as `[Nucleus] Anchor origin shift`).

### 9.3 Borrow-list status update

- **Item 4 (retain sorted fade list) — obsolete**: fade ordering no longer touches uploads; the remaining per-frame sort is negligible. Closed by §9.2.
- **Item 5 (subgroup ballot) — partially done**: `NUCLEUS_BALLOT` tile-level compaction is implemented (runtime workgroup==subgroup check + shared-memory fallback); the workgroup-per-command granularity (page-per-workgroup, audit §2.6) remains open.
- **Item 1 (GPU-patched counts)** — still pending; with §9.2 the command-buffer rebuild + `glBufferSubData` (nCmd × 32 B) is now the dominant constant coordinator cost. Carries the Embeddium double-present caveat (§6 item 1).
- **Item 2 (FIFO fence) — DONE 0927**: `PersistentUploadStaging` keeps a FIFO of fine-grained fences (every 256 KiB of written run + exact-range tail fence at `endFrame`, wrap-safe), so `waitForRange` targets the exact oldest overlapping range instead of the whole-frame fence; no busy-wait loop changes needed (per-fence retries unchanged). `GpuSpanUploader.uploadSpan` gained `-Dhbm.stagingDirect=true` for A/B testing direct `glBufferSubData` on UMA iGPUs.

**Verification:** stand still near a farm → Uploads: 0 B; fly through it → occasional spans (bucket crossings) instead of continuous per-frame churn; window resize / 256 m travel → single full re-upload burst.


