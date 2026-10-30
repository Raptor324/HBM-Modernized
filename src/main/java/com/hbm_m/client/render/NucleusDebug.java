package com.hbm_m.client.render;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.client.render.culling.HiZDepthPyramid;

@OnlyIn(Dist.CLIENT)
/**
 * Counters for the Nucleus render engine plus the F3 debug screen section.
 * <p>
 * Per-frame counters are reset in {@link #onFrameStart()}, called from
 * {@code InstancedRenderFrame.onBeforeBlockEntities} (start of the BE pass).
 * The F3 overlay is drawn later in the same frame, so it sees current values.
 * Registration during the shadow pass is ignored (centrally in the recording
 * entry points), otherwise machines would be counted twice under Iris.
 */
public final class NucleusDebug {

    // ── Per-frame (reset in onFrameStart) ──────────────────────────────
    private static int drawCalls;
    private static int instancesDrawn;
    private static int machinesRendered;
    private static int machinesCulled;
    private static int uploadSpans;
    private static long uploadBytes;
    /** Windows whose span collection crossed the old 48-span full-upload
     *  bound (A/B metric for the chunked-scatter change; F3 "ovf"). */
    private static int uploadOverflows;
    /** Parametric records enqueued this frame (contract counter for the
     *  parametric fleet path; F3 "Parametric records"). */
    private static int parametricRecords;
    /** Which paths drew in this frame. Separate flags rather than a "last path":
     *  chain parts (GPU-bones) always bypass the atlas and draw AFTER MDI —
     *  the mode line must not "fall back" to direct because of that. */
    private static boolean modeMdi;
    private static boolean modeDirect;
    private static boolean modeIris;
    private static boolean modeImmediate;
    /** Label of the MDI-family path (no-pack MDI coordinator or Tier-1 GPU
     *  bake) for the Mode line - set only by the modeMdi cases, never by the
     *  Iris ones, so two paths drawing in one frame cannot overwrite each
     *  other's label (the "Iris instanced + Iris instanced" mislabel). */
    private static String mdiVariant = "MDI (multi)";
    /** Label of the Iris companion path (Tier-2) - set only by the modeIris
     *  cases. Separate from mdiVariant: a frame can legitimately draw BOTH
     *  Tier-1 bake (some renderers) and Tier-2 companion (the rest), and each
     *  Mode-line term must print its own path. */
    private static String irisVariant = "";
    /** MDI clean-frame reuse: renderers this frame / reused / instances in them. */
    private static int mdiRenderersTotal;
    private static int mdiRenderersClean;
    private static int mdiInstancesClean;

    // ── GPU Culling stats ──────────────────────────────────────────────
    private static volatile boolean gpuCullActive;
    private static volatile int gpuCullCommands;
    private static volatile int gpuCullInstancesIn;
    private static volatile int gpuCullInstancesOut;
    /** IrisShadowBatchCollector.renderFrame() at the last recordGpuCull call.
     *  recordGpuCull(true) is rate-limited (2-3 s log timers), so gpuCullActive
     *  alone can stay true long after the MDI coordinator stopped running
     *  (world unload, pack toggle, empty draw list); freshness is therefore
     *  judged per frame against the current stamp in the F3 branch. */
    private static volatile long gpuCullStamp = -1L;
    /** True when in/out counts came from an actual readback (1 Hz); false when only
     *  the dispatch-side stamp is fresh (every frame) and the counts are stale. */
    private static volatile boolean gpuCullCountsFresh;
    /** Set by noteGpuCullFallback on a dispatch that failed and fell back to
     *  draw-everything; cleared by resetGpuCullStats and any later successful dispatch. */
    private static volatile boolean gpuCullFallback;

    // ── Shadow-pass stats (NOT gated by counting(): main counters sleep in
    //    shadows, these in turn live only there) ────────────────────────────
    // Reset in onShadowPassStart (mixin HEAD renderShadows): the shadow phase
    // of a frame runs BEFORE the main phase, so onFrameStart (main BE start)
    // would wipe the counters before F3 is drawn and the Shadow line would
    // never show.
    private static volatile int shadowFallbackParts;
    private static volatile int shadowRecords;
    private static volatile int shadowFlushDraws;
    private static volatile int shadowFlushInstances;
    private static volatile String shadowFlushTier = "";
    /** IrisShadowBatchCollector.renderFrame() stamp taken at record time; lets
     *  the F3 line age-mark itself instead of silently showing stale values
     *  (audit 2026-10-07: the Shadow line otherwise stuck after pack disable,
     *  because the only reset lived in the mixin on renderShadows HEAD). */
    private static volatile long shadowStatsFrame = -1L;

    /** True start of the frame's shadow phase — mixin HEAD Iris ShadowRenderer.renderShadows. */
    public static void onShadowPassStart() {
        resetShadowStats();
    }

    /** Zeroes the shadow-pass stats. Called from onShadowPassStart (per frame,
     *  pack on) and from teardown (IrisShadowBatchCollector.resetAllMemory, a
     *  registered RenderCacheManager hook), so the F3 "Shadow:" line cannot
     *  outlive the shader pack: with the pack disabled renderShadows never
     *  runs, and without a teardown reset the last pack-on values printed
     *  forever (audit 2026-10-07, confirmed stick). */
    public static void resetShadowStats() {
        shadowFallbackParts = 0;
        shadowRecords = 0;
        shadowFlushDraws = 0;
        shadowFlushInstances = 0;
        shadowFlushTier = "";
        shadowStatsFrame = -1L;
    }

    /** Zeroes the GPU-cull overlay stats (flag + counters + freshness stamp).
     *  Called when the MDI session is discarded without a dispatch: after that
     *  no recordGpuCull will re-assert the values this frame, so "GPU Cull:
     *  Active" must not keep showing the discarded session's numbers. */
    public static void resetGpuCullStats() {
        gpuCullActive = false;
        gpuCullCommands = 0;
        gpuCullInstancesIn = 0;
        gpuCullInstancesOut = 0;
        gpuCullStamp = -1L;
        gpuCullCountsFresh = false;
        gpuCullFallback = false;
    }

    /** A part went through putBulkData inside the shadow pass (poisons the global shadow batch). */
    public static void recordShadowFallback() {
        shadowFallbackParts++;
        shadowStatsFrame = IrisShadowBatchCollector.renderFrame();
    }

    /** Instance recorded into IrisShadowBatchCollector during the shadow pass. */
    public static void recordShadowRecord(int instances) {
        if (instances > 0) {
            shadowRecords += instances;
            shadowStatsFrame = IrisShadowBatchCollector.renderFrame();
        }
    }

    /** Flush result of the global shadow batch (tier + draw/instance counts). */
    public static void recordShadowFlush(String tier, int draws, int instances) {
        shadowFlushTier = tier;
        shadowFlushDraws += draws;
        shadowFlushInstances += instances;
        shadowStatsFrame = IrisShadowBatchCollector.renderFrame();
    }

    public static void recordGpuCull(boolean active, int commands, int in, int out) {
        if (!counting()) return;
        gpuCullActive = active;
        gpuCullCommands = commands;
        gpuCullInstancesIn = in;
        gpuCullInstancesOut = out;
        gpuCullCountsFresh = active;
        if (active) {
            gpuCullFallback = false;
        }
        gpuCullStamp = IrisShadowBatchCollector.renderFrame();
    }

    /**
     * Dispatch-side stamp, called on EVERY successful GPU cull dispatch (the 10-09
     * "Waiting/No depth" bug: recordGpuCull(true) fired only from the 1 Hz diagnostic
     * readback, so the F3 freshness gate — which requires the stamp to equal the
     * current frame — failed ~59 of 60 frames and a fully working culler printed
     * "Waiting"). Counts stay at the last readback (gpuCullCountsFresh=false) —
     * the true in/out split needs the glBufferSubData sync and stays rate-limited.
     */
    public static void noteGpuCullDispatch(int commands, int totalIn) {
        if (!counting()) return;
        gpuCullActive = true;
        gpuCullCommands = commands;
        gpuCullInstancesIn = totalIn;
        gpuCullCountsFresh = false;
        gpuCullFallback = false;
        gpuCullStamp = IrisShadowBatchCollector.renderFrame();
    }

    /** GPU cull was dispatched but failed (guard/compute error) and the indirect
     *  counts were rewritten to draw ALL instances — a distinct F3 state from
     *  "culling" and from "no depth" (previously invisible, 10-09). */
    public static void noteGpuCullFallback(int commands) {
        if (!counting()) return;
        gpuCullActive = false;
        gpuCullFallback = true;
        gpuCullCommands = commands;
        gpuCullStamp = IrisShadowBatchCollector.renderFrame();
    }

    // ── Lifetime ───────────────────────────────────────────────────────
    /** Total direct-path VBO across live InstancedStaticPartRenderer (vertices+indices+instance VBO). */
    private static final AtomicLong RENDERER_VRAM = new AtomicLong();

    private NucleusDebug() {}

    public static void onFrameStart() {
        drawCalls = 0;
        instancesDrawn = 0;
        machinesRendered = 0;
        machinesCulled = 0;
        uploadSpans = 0;
        uploadBytes = 0;
        uploadOverflows = 0;
        parametricRecords = 0;
        epochChecks = 0;
        epochStable = 0;
        walkAttempts = 0;
        walkAsserted = 0;
        paramBranchRuns = 0;
        modeMdi = false;
        modeDirect = false;
        modeIris = false;
        modeImmediate = false;
        // Both path labels must reset with the mode flags: a value left over
        // from a previous frame/pack (e.g. after a BSL->Photon->BSL swap) would
        // keep a stale label printed while the actual path changed.
        mdiVariant = "MDI (multi)";
        irisVariant = "";
        mdiRenderersTotal = 0;
        mdiRenderersClean = 0;
        mdiInstancesClean = 0;
        // Belt-and-braces for the shadow stats (audit 2026-10-07): without a
        // pack, Iris renderShadows never runs, so neither the mixin reset nor
        // fresh records fire and a stale "Shadow:" line would print forever.
        // The per-frame teardown reset lives in IrisShadowBatchCollector.
        // resetAllMemory; this guarantees the pack-off idle state even if a
        // teardown path is ever missed.
        if (!ShaderCompatibilityDetector.isExternalShaderActive()) {
            resetShadowStats();
        }
    }

    private static boolean counting() {
        return !ShaderCompatibilityDetector.isRenderingShadowPass();
    }

    public static void recordDraw(int calls, int instances, String drawMode) {
        if (!counting()) return;
        if (calls <= 0) return;
        drawCalls += calls;
        instancesDrawn += instances;
        switch (drawMode) {
            case "MDI (multi)":
            case "MDI (indirect loop)":
                modeMdi = true;
                mdiVariant = drawMode;
                break;
            case "GPU bake main":
            case "GPU bake shadow":
                modeMdi = true;
                mdiVariant = "GPU bake (Tier-1 compute batch)";
                break;
            case "Iris batch":
            case "Iris single":
                modeIris = true;
                irisVariant = "Iris batch (Tier-2 per-renderer)";
                break;
            case "Iris instanced":
                modeIris = true;
                irisVariant = "Iris instanced (Tier-2 true instancing)";
                break;
            // Shadow-pass literals (IrisShadowBatchCollector flush tiers). Today
            // they are dropped by counting() (all fire inside renderShadows), so
            // these cases are defensive: if a cachedShadowPass desync around a
            // mid-shadow-pass pack toggle ever let one through, it would
            // otherwise fall to default and mislabel as "Instanced (direct)" -
            // the same class as the "(cached)" mislabel fixed earlier.
            case "GPU bake shadow flush":
                modeMdi = true;
                mdiVariant = "GPU bake (Tier-1 compute batch)";
                break;
            case "Iris shadow instanced":
                modeIris = true;
                irisVariant = "Iris instanced (Tier-2 true instancing)";
                break;
            case "Iris shadow batch":
            case "Iris shadow bake-fallback":
                modeIris = true;
                irisVariant = "Iris batch (Tier-2 per-renderer)";
                break;
            case "Immediate (fallback)":
                modeImmediate = true;
                break;
            case "Instanced (direct)":
            case "Instanced (single)":
            default:
                modeDirect = true;
                break;
        }
    }

    public static void recordMachineRendered() {
        if (!counting()) return;
        machinesRendered++;
    }

    public static void recordMachineCulled() {
        if (!counting()) return;
        machinesCulled++;
    }

    public static void recordUpload(int spans, long bytes) {
        if (!counting()) return;
        uploadSpans += spans;
        uploadBytes += bytes;
    }

    /** A window's span collection crossed the old 48-span full-upload bound. */
    public static void recordUploadOverflow() {
        if (!counting()) return;
        uploadOverflows++;
    }

    /** A parametric record was enqueued this frame (parametric fleet path). */
    public static void recordParametricEnqueue() {
        if (!counting()) return;
        parametricRecords++;
    }

    // Frozen-walk diagnostics (the 2026-10-07 idle-farm hunt: the walk's
    // engagement was invisible in profiles - these three counters answer it
    // directly from the F3 line).
    private static int epochChecks;
    private static int epochStable;
    private static int walkAttempts;
    private static int walkAsserted;
    private static int paramBranchRuns;

    /** One REAL (non-memoized) animation-epoch stability computation. */
    public static void recordEpochCheck(boolean stable) {
        if (!counting()) return;
        epochChecks++;
        if (stable) epochStable++;
    }

    /** One frozen roster walk; {@code asserted} = it confirmed every selected part. */
    public static void recordWalkAttempt(boolean asserted) {
        if (!counting()) return;
        walkAttempts++;
        if (asserted) walkAsserted++;
    }

    /** The per-frame parametric branch of renderPartsInner executed once. */
    public static void recordParametricBranch() {
        if (!counting()) return;
        paramBranchRuns++;
    }

    /**
     * Per-frame MDI clean-reuse statistics (called once from prepareMdiDraw).
     *
     * @param total  renderers in the frame (retained draw list)
     * @param clean  of those, reused without a snapshot copy or diff
     */
    public static void recordMdiReuse(int total, int clean, int cleanInstances) {
        if (!counting()) return;
        mdiRenderersTotal += total;
        mdiRenderersClean += clean;
        mdiInstancesClean += cleanInstances;
    }

    public static void addRendererVram(long bytes) {
        RENDERER_VRAM.addAndGet(bytes);
    }

    public static void removeRendererVram(long bytes) {
        RENDERER_VRAM.addAndGet(-bytes);
    }

    /**
     * F3 screen section (compacted 2026-10-07: only lines with data, the
     * section must fit the screen):
     * <pre>
     * [Nucleus] Active                      - yellow header; Active green / Not rendering red
     * Rendering: N models, P parts, M culled
     * Draws: N calls, M inst, P param       - param omitted when 0
     * GPU Cull / Bake cull (pack)           - one line per the active cull path
     * Uploads: N spans, X KB (ring) [, N ovf]
     * VRAM [bar] X / Y MiB
     * VRAM: Atlas a/b · Bake pools c/d · ...- non-zero components on ONE line
     * Mode: paths that drew this frame
     * MDI Reuse / Shadow / Bake spans       - only when they have data
     * </pre>
     */
    public static void appendDebugLines(List<String> out) {
        boolean active = drawCalls > 0;
        out.add("§e[Nucleus] §" + (active ? "aActive" : "cNot rendering"));
        // Peek without creating: F3 in the menu must not instantiate atlas/staging.
        MdiGeometryAtlas atlas = MdiGeometryAtlas.peekOrNull();
        int atlasParts = (atlas != null && atlas.isReady()) ? atlas.getRegisteredGeometryCount() : 0;
        // Count of unique part meshes in the atlas (one entry per machine-TYPE part;
        // dynamic parts get a separate entry per variant, e.g. tank per-fluid).
        // Grows lazily as types/variants first appear in a frame.
        StringBuilder rendering = new StringBuilder("Rendering: §a").append(machinesRendered).append("§r models");
        if (atlasParts > 0) {
            rendering.append(", §7").append(atlasParts).append("§r parts");
        }
        if (com.hbm_m.config.ModClothConfig.get().getEffectiveOcclusionCullingMode()
                == com.hbm_m.config.ModClothConfig.OcclusionCullingMode.CPU && machinesCulled > 0) {
            rendering.append(", §7").append(machinesCulled).append(" culled");
        }
        out.add(rendering.toString());

        // Draw + parametric counters packed on one line: the F3 section must stay
        // compact (user request 2026-10-07 - it no longer fit the screen). The
        // parametric count is the GPU-FK engagement metric and is omitted when 0
        // (a no-parametric scene should not spend a line on it).
        StringBuilder draws = new StringBuilder("Draws: §b").append(drawCalls).append("§r calls, §7").append(instancesDrawn).append("§r inst");
        if (parametricRecords > 0) {
            draws.append(", §d").append(parametricRecords).append("§r param");
        }
        out.add(draws.toString());

        // Pack branch FIRST: gpuCullActive is a sticky volatile set only by the
        // no-pack MDI coordinator and never reset in onFrameStart, so once MDI
        // had run and a pack was then enabled, the stale MDI branch swallowed
        // this whole section and every "Bake ..." line vanished. Under a pack
        // the MDI coordinator is dead anyway (MdiBatchCoordinator.beginFrame
        // returns null when a pack is active), so the MDI "Waiting/No depth"
        // line would be permanently misleading here. Report the Tier-1 bake
        // path instead: its instance-granularity
        if (ShaderCompatibilityDetector.isExternalShaderActive()) {
            // compute cull is frustum-only, or hi-z when the optional occlusion
            // phase sampled the pyramid (then "occl-culled" counts instances
            // that passed the frustum but were culled by depth) - totals are of
            // the last frame that actually baked, "idle" if none did.
            // Both passes stamp the SAME frame on frames where both ran (shadow
            // first, main after, one renderFrame), so the passes report as two
            // independent per-pass segments instead of an if/else-if chain in
            // which the shadow branch was unreachable (audit 2026-10-07 #6).
            long f = IrisShadowBatchCollector.renderFrame();
            boolean mainBaked = NucleusGpuBaker.lastBakeMainFrame() == f;
            boolean shadowBaked = NucleusGpuBaker.lastBakeShadowFrame() == f;
            if (mainBaked || shadowBaked) {
                StringBuilder bc = new StringBuilder("Bake cull (pack): ");
                if (mainBaked) {
                    boolean hiZ = NucleusGpuBaker.lastBakeMainHiZ();
                    bc.append("§d").append(hiZ ? "hi-z" : "frustum-only").append("§7, §a")
                            .append(NucleusGpuBaker.lastBakeMainVisible()).append("§r/§f")
                            .append(NucleusGpuBaker.lastBakeMainTotal()).append("§7 visible")
                            .append(hiZ ? "§7, §b" + NucleusGpuBaker.lastBakeMainOccluded() + "§7 occl-culled" : "");
                } else {
                    bc.append("§8main idle");
                }
                if (shadowBaked) {
                    boolean hiZ = NucleusGpuBaker.lastBakeShadowHiZ();
                    bc.append("§7 / §d").append(hiZ ? "hi-z" : "frustum-only").append("§7, §ashadow ")
                            .append(NucleusGpuBaker.lastBakeShadowVisible()).append("§r/§f")
                            .append(NucleusGpuBaker.lastBakeShadowTotal()).append("§7 visible")
                            .append(hiZ ? "§7, §b" + NucleusGpuBaker.lastBakeShadowOccluded() + "§7 occl-culled" : "");
                } else {
                    bc.append("§7 / §8shadow idle");
                }
                out.add(bc.toString());
            }
            // Bake cull line is omitted entirely when no pass baked this frame
            // (idle on a no-op frame is not data - F3 compaction 2026-10-07).
            // Span-level re-expansion totals, one column per pass. A pass stamps
            // its totals only on frames where it ran a PARTIAL dirty expansion
            // (a degenerate full re-bake is deliberately not counted), so
            // exact-frame matching made this line visible for exactly ONE frame
            // per dirty expansion - flicker on any animated scene. Instead the
            // line always shows the last-known values with a per-column age
            // against the current render frame stamp f. "never" on BOTH passes
            // = no data yet - the line is omitted (F3 compaction 2026-10-07).
            if (NucleusGpuBaker.lastSpanMainDirtyFrame() >= 0
                    || NucleusGpuBaker.lastSpanShadowDirtyFrame() >= 0) {
                out.add("Bake spans: §a" + NucleusGpuBaker.lastSpanMainDirtyDispatches()
                        + "§r/§f" + NucleusGpuBaker.lastSpanMainDirtyInstances() + "§7 main "
                        + spanAge(NucleusGpuBaker.lastSpanMainDirtyFrame(), f) + ", §a"
                        + NucleusGpuBaker.lastSpanShadowDirtyDispatches()
                        + "§r/§f" + NucleusGpuBaker.lastSpanShadowDirtyInstances()
                        + "§7 shadow " + spanAge(NucleusGpuBaker.lastSpanShadowDirtyFrame(), f)
                        + "§7 span-expanded");
            }
            // Global-batch pool realloc counters. reallocs is a SESSION-LIFETIME
            // monotonic counter - incremented on pool store growth, never reset
            // by pool release or world reload (fresh pools re-increment on top
            // of old totals) - so it is labeled as a cumulative total, not as
            // recent activity. A realloc ORPHANS the pass's whole output store:
            // every cache of that pass fully re-bakes (re-expands and re-uploads)
            // on the frames following it - a spike in bake work without a
            // matching realloc is unexpected.
            long pr = NucleusGpuBaker.poolMainReallocs() + NucleusGpuBaker.poolShadowReallocs();
            if (pr > 0) {
                out.add("Bake pool (session total): §a" + NucleusGpuBaker.poolMainReallocs()
                        + "§7 main, §a" + NucleusGpuBaker.poolShadowReallocs()
                        + "§7 shadow reallocs §7(each = full pass re-bake)");
            }
        } else if (gpuCullActive && gpuCullInstancesIn > 0
                // Freshness gate (audit 2026-10-07 #9): gpuCullActive is sticky,
                // so require the stamp to be the current render frame. The stamp is
                // written by noteGpuCullDispatch on every MDI dispatch that culls,
                // so a non-current stamp means the coordinator is no longer running
                // and the line correctly falls through to Waiting/Disabled.
                && gpuCullStamp == IrisShadowBatchCollector.renderFrame()) {
            if (gpuCullCountsFresh) {
                int culled = Math.max(0, gpuCullInstancesIn - gpuCullInstancesOut);
                float pct = (culled * 100.0f) / gpuCullInstancesIn;
                out.add("GPU Cull: §aActive §7(" + culled + "/" + gpuCullInstancesIn + " culled, "
                        + String.format(Locale.ROOT, "%.1f", pct) + "%, " + gpuCullCommands + " cmds)");
            } else {
                // Dispatch stamp is fresh (culled this frame); the in/out split is
                // from the last 1 Hz readback — the sync is too costly per frame.
                out.add("GPU Cull: §aActive §7(in " + gpuCullInstancesIn + ", " + gpuCullCommands
                        + " cmds, split @1Hz)");
            }
        } else if (gpuCullFallback && gpuCullStamp == IrisShadowBatchCollector.renderFrame()) {
            out.add("GPU Cull: §cFallback §7(dispatch failed, drawing all " + gpuCullCommands + " cmds)");
        } else if (com.hbm_m.config.ModClothConfig.get().getEffectiveOcclusionCullingMode() == com.hbm_m.config.ModClothConfig.OcclusionCullingMode.GPU) {
            out.add("GPU Cull: §eWaiting/No depth §7(mode=GPU)");
        } else {
            out.add("GPU Cull: §7Disabled (mode=" + com.hbm_m.config.ModClothConfig.get().getEffectiveOcclusionCullingMode() + ")");
        }

        PersistentUploadStaging staging = PersistentUploadStaging.peekOrNull();
        String channel = (staging != null) ? "ring" : "subdata";
        String ovf = (uploadOverflows > 0) ? ", §e" + uploadOverflows + "§7 ovf" : "";
        if (uploadSpans == 0) {
            out.add("Uploads: §a0 B §7(static, " + channel + ")" + ovf);
        } else {
            out.add("Uploads: §f" + uploadSpans + "§7 spans, " + humanBytes(uploadBytes) + " §7(" + channel + ")" + ovf);
        }
        // "Parametric records" is appended to the Draws: line above (line ~412) -
        // the standalone line was a duplicate.
        // Frozen-walk diagnostics (only when the epoch machinery is in play):
        // epoch = share of REAL stability checks that came out stable, walk =
        // share of frozen roster walks that confirmed every part, pbr = how
        // many machines ran the full parametric branch this frame.
        if (epochChecks > 0) {
            int epochPct = epochStable * 100 / epochChecks;
            int walkPct = (walkAttempts == 0) ? 0 : walkAsserted * 100 / walkAttempts;
            out.add("Frozen: §f" + epochPct + "%§7 epoch (" + epochChecks + "), §f"
                    + walkPct + "%§7 walk (" + walkAttempts + "), §f" + paramBranchRuns + "§7 pbr");
        }

        // VRAM bar. Usage and ceiling cover the same component set: the MDI
        // atlas (used vs allocated caps, both instance VBOs), the direct-path
        // renderer VBOs (cap-based), the staging ring, the full Tier-1 bake
        // footprint (pools at their 1.5x-headroom stores, per-cache and shared
        // SSBOs) and - since the 2026-10-07 VRAM ledger - the Iris companion
        // meshes + lightmap rings, the joint-spec texture and the Hi-Z pyramid
        // (all pure field math). The ceiling is the sum of ACTUAL allocated
        // capacities - never the bake-cache budget, which excludes the pool
        // headroom, the atlas and shared floors and the renderer VBOs, and
        // includes CPU heap snapshots.
        boolean atlasReady = atlas != null && atlas.isReady();
        long stagingCap = staging != null ? staging.getCapacityBytes() : 0L;
        long atlasUsed = atlasReady ? atlas.estimateVramBytes() : 0L;
        long atlasMax = atlasReady ? atlas.estimateAllocatedVramBytes() : 0L;
        // Mode ordinals per NucleusGpuBaker: MODE_SHADOW = 0, MODE_MAIN = 1.
        long poolMainUsed = NucleusGpuBaker.poolUsedBytes(1 /* MODE_MAIN */);
        long poolMainMax = NucleusGpuBaker.poolAllocatedBytes(1);
        long poolShadowUsed = NucleusGpuBaker.poolUsedBytes(0 /* MODE_SHADOW */);
        long poolShadowMax = NucleusGpuBaker.poolAllocatedBytes(0);
        long bakeCacheUsed = NucleusGpuBaker.cacheNonPoolUsedBytes();
        long bakeCacheMax = NucleusGpuBaker.cacheNonPoolAllocatedBytes();
        long rendererVram = RENDERER_VRAM.get();
        long companionVram = MeshRenderCache.irisCompanionVramBytes();
        long jointVram = NucleusJointSpecs.getVramBytes();
        long hiZVram = HiZDepthPyramid.getPyramidVramBytes();
        // The bar measures ONLY the components that have a real used/allocated
        // split (Atlas, bake pools, caches). The five usage-only components
        // (Renderers, Companions, Joints, Staging, Hi-Z) entered both sums with
        // identical values before the 2026-10-08 fix - their share contributed
        // ratio 1.0 by construction, pinning the bar near red permanently
        // regardless of actual headroom. They stay visible as a fixed-size
        // suffix and in the per-component ledger line below.
        long fixedVram = rendererVram + stagingCap + companionVram + jointVram + hiZVram;
        long vramUsed = atlasUsed + poolMainUsed + poolShadowUsed + bakeCacheUsed;
        long vramMax = atlasMax + poolMainMax + poolShadowMax + bakeCacheMax;
        out.add(vramBarLine(vramUsed, vramMax)
                + (fixedVram > 0 ? " §8+ §7" + humanBytes(fixedVram) + "§8 fixed" : ""));
        // Per-component breakdown (2026-10-07 VRAM ledger), compacted 2026-10-07:
        // the nine per-component lines no longer fit the F3 screen - all non-zero
        // components now share ONE line, zero-capacity components are omitted
        // entirely (user request: only lines with data).
        StringBuilder vramParts = new StringBuilder();
        appendVramComponent(vramParts, "Atlas", atlasUsed, atlasMax);
        appendVramComponent(vramParts, "Bake pools", poolMainUsed + poolShadowUsed, poolMainMax + poolShadowMax);
        appendVramComponent(vramParts, "Caches", bakeCacheUsed, bakeCacheMax);
        appendVramComponent(vramParts, "Renderers", rendererVram, -1);
        appendVramComponent(vramParts, "Companions", companionVram, -1);
        appendVramComponent(vramParts, "Joints", jointVram, -1);
        appendVramComponent(vramParts, "Staging", stagingCap, -1);
        appendVramComponent(vramParts, "Hi-Z", hiZVram, -1);
        if (vramParts.length() > 0) {
            out.add("VRAM: §7" + vramParts);
        }

        if (modeMdi && mdiRenderersTotal > 0) {
            out.add("MDI Reuse: §a" + mdiRenderersClean + "§7/§f" + mdiRenderersTotal
                    + "§7 renderers, " + mdiInstancesClean + " inst. static");
        }

        // Immediate (fallback) is a degradation: shown red and first in the list.
        StringBuilder modeLine = new StringBuilder();
        if (modeImmediate) modeLine.append("§cImmediate (fallback)§r");
        if (modeMdi) {
            if (modeLine.length() > 0) modeLine.append(" + ");
            modeLine.append("§d").append(mdiVariant);
        }
        if (modeIris) {
            if (modeLine.length() > 0) modeLine.append(" + ");
            modeLine.append("§d").append(irisVariant);
        }
        if (modeDirect) {
            if (modeLine.length() > 0) modeLine.append(" + ");
            modeLine.append("§7Instanced (direct) (vanilla path)");
        }
        out.add("Mode: " + (modeLine.length() > 0 ? modeLine : "§8idle"));

        // Shadow pass of this frame: batch records / flush / degradations in putBulkData.
        // fallback>0 shown red - the global shadow batch is poisoned, shadows fall back to CPU.
        // The line carries a frame-age suffix (audit 2026-10-07 #14): after the
        // pack-disable stick was fixed (resetShadowStats in resetAllMemory +
        // the onFrameStart pack-off self-heal), the marker keeps any future
        // stick self-describing instead of silently stale.
        if (shadowRecords > 0 || shadowFlushDraws > 0 || shadowFallbackParts > 0) {
            StringBuilder sh = new StringBuilder("Shadow: §a").append(shadowRecords)
                    .append("§r rec, ")
                    .append(shadowFlushTier.isEmpty() ? "§8no flush" : "§d" + shadowFlushTier)
                    .append("§r ").append(shadowFlushDraws).append("dr/")
                    .append(shadowFlushInstances).append("inst");
            if (shadowFallbackParts > 0) {
                sh.append(", §c").append(shadowFallbackParts).append(" FALLBACK§r");
            }
            sh.append(' ').append(spanAge(shadowStatsFrame, IrisShadowBatchCollector.renderFrame()));
            out.add(sh.toString());
        }
    }

    /**
     * Frame-age suffix for a pass's last dirty-expansion stamp (stamps init to
     * -1 in NucleusGpuBaker): "now" for the current render frame, "(Nf ago)"
     * for a stale last-known value, "never" before the pass's first dirty
     * expansion.
     */
    private static String spanAge(long stamp, long f) {
        if (stamp < 0) return "§8never";
        long age = f - stamp;
        return age == 0 ? "§anow" : "§7(" + age + "f ago)";
    }

    private static String humanBytes(long b) {
        if (b < 1024L) return b + " B";
        if (b < 1024L * 1024L) return String.format(Locale.ROOT, "%.1f KB", b / 1024.0);
        if (b < 1024L * 1024L * 1024L) return String.format(Locale.ROOT, "%.1f MB", b / (1024.0 * 1024.0));
        return String.format(Locale.ROOT, "%.2f GB", b / (1024.0 * 1024.0 * 1024.0));
    }

    /** Fixed width of the F3 VRAM progress bar. */
    private static final int VRAM_BAR_CELLS = 20;

    /**
     * The F3 VRAM line: a fixed-width progress bar of usage against the
     * allocated-capacity ceiling, numbers to the right in MiB:
     * <pre>VRAM [████████░░░░░░░░░░░░] 312.4 / 512.0 MiB</pre>
     * The bar is colored by fill ratio (green below 60%, yellow below 85%,
     * red at or above); the numbers stay neutral. Nothing allocated shows
     * "§8idle"; a transient usage overshoot clamps the bar to full while the
     * numbers keep the true values.
     */
    private static String vramBarLine(long used, long maxAlloc) {
        if (maxAlloc <= 0L) {
            return "VRAM §8idle";
        }
        double ratio = (double) used / (double) maxAlloc;
        int cells = (int) Math.min(VRAM_BAR_CELLS, Math.round(ratio * VRAM_BAR_CELLS));
        if (cells < 0) cells = 0;
        String color = ratio < 0.60 ? "§a" : ratio < 0.85 ? "§e" : "§c";
        StringBuilder bar = new StringBuilder("VRAM ").append(color).append('[');
        for (int i = 0; i < VRAM_BAR_CELLS; i++) {
            bar.append(i < cells ? '█' : '░');
        }
        bar.append("] §7")
                .append(String.format(Locale.ROOT, "%.1f", used / (1024.0 * 1024.0)))
                .append(" / ")
                .append(String.format(Locale.ROOT, "%.1f", maxAlloc / (1024.0 * 1024.0)))
                .append(" MiB");
        return bar.toString();
    }

    /**
     * One breakdown line under the VRAM bar: "  Label: used / alloc" in MiB,
     * same number formatting as the bar. Components without over-allocation
     * (used == allocated) print the pair once-worth-looking-at anyway, so a
     * sudden slack column on the pool lines is the visible shrink signal.
     */
    /**
     * Compact single-line VRAM component: appends "Label used/alloc" (or
     * "Label used" when {@code alloc < 0}, usage-only components) to the
     * shared VRAM parts line, skipping components with no data at all
     * (user request 2026-10-07: only lines with data, the section must fit
     * the screen).
     */
    private static void appendVramComponent(StringBuilder sb, String label, long used, long alloc) {
        if (used <= 0L && alloc <= 0L) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(" §8· §7");
        }
        sb.append(label).append(" §f");
        if (alloc >= 0L) {
            sb.append(String.format(Locale.ROOT, "%.0f/%.0f", used / (1024.0 * 1024.0), alloc / (1024.0 * 1024.0)));
            sb.append("§7 MiB");
        } else {
            sb.append(humanBytes(used));
        }
    }
}
