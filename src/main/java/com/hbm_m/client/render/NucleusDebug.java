package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;

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
    /** Which paths drew in this frame. Separate flags rather than a "last path":
     *  chain parts (GPU-bones) always bypass the atlas and draw AFTER MDI —
     *  the mode line must not "fall back" to direct because of that. */
    private static boolean modeMdi;
    private static boolean modeDirect;
    private static boolean modeIris;
    private static boolean modeImmediate;
    private static String mdiVariant = "MDI (multi)";
    /** MDI clean-frame reuse: renderers this frame / reused / instances in them. */
    private static int mdiRenderersTotal;
    private static int mdiRenderersClean;
    private static int mdiInstancesClean;

    // ── GPU Culling stats ──────────────────────────────────────────────
    private static volatile boolean gpuCullActive;
    private static volatile int gpuCullCommands;
    private static volatile int gpuCullInstancesIn;
    private static volatile int gpuCullInstancesOut;

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

    /** True start of the frame's shadow phase — mixin HEAD Iris ShadowRenderer.renderShadows. */
    public static void onShadowPassStart() {
        shadowFallbackParts = 0;
        shadowRecords = 0;
        shadowFlushDraws = 0;
        shadowFlushInstances = 0;
        shadowFlushTier = "";
    }

    /** A part went through putBulkData inside the shadow pass (poisons the global shadow batch). */
    public static void recordShadowFallback() {
        shadowFallbackParts++;
    }

    /** Instance recorded into IrisShadowBatchCollector during the shadow pass. */
    public static void recordShadowRecord(int instances) {
        if (instances > 0) shadowRecords += instances;
    }

    /** Flush result of the global shadow batch (tier + draw/instance counts). */
    public static void recordShadowFlush(String tier, int draws, int instances) {
        shadowFlushTier = tier;
        shadowFlushDraws += draws;
        shadowFlushInstances += instances;
    }

    public static void recordGpuCull(boolean active, int commands, int in, int out) {
        if (!counting()) return;
        gpuCullActive = active;
        gpuCullCommands = commands;
        gpuCullInstancesIn = in;
        gpuCullInstancesOut = out;
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
        modeMdi = false;
        modeDirect = false;
        modeIris = false;
        modeImmediate = false;
        mdiRenderersTotal = 0;
        mdiRenderersClean = 0;
        mdiInstancesClean = 0;
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
                modeMdi = true;
                mdiVariant = "GPU bake";
                break;
            case "Iris batch":
            case "Iris single":
                modeIris = true;
                break;
            case "Iris instanced":
                modeIris = true;
                mdiVariant = "Iris instanced";
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
     * F3 screen section. Format:
     * <pre>
     * [Nucleus] Active                      - yellow header; Active green / Not rendering red
     * Geometry Parts in Atlas: N            - unique part meshes (not machines); grows lazily
     * Rendering: N models, M culled         - culled only when culling is enabled
     * Draw Calls: N
     * Instances Drawn: N
     * Uploads: 0 B (static) | N spans, X KB
     * VRAM Used: X.X MB
     * Mode: MDI (multi) | Instanced (direct) | Iris batch | ...
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
        out.add("Geometry Parts in Atlas: §7" + atlasParts);

        StringBuilder rendering = new StringBuilder("Rendering: §a").append(machinesRendered).append("§r models");
        if (com.hbm_m.config.ModClothConfig.get().getEffectiveOcclusionCullingMode()
                == com.hbm_m.config.ModClothConfig.OcclusionCullingMode.CPU && machinesCulled > 0) {
            rendering.append(", §7").append(machinesCulled).append(" culled");
        }
        out.add(rendering.toString());

        out.add("Draw Calls: §b" + drawCalls);
        out.add("Instances Drawn: §7" + instancesDrawn);

        if (gpuCullActive && gpuCullInstancesIn > 0) {
            int culled = Math.max(0, gpuCullInstancesIn - gpuCullInstancesOut);
            float pct = (culled * 100.0f) / gpuCullInstancesIn;
            out.add("GPU Cull: §aActive §7(" + culled + "/" + gpuCullInstancesIn + " culled, "
                    + String.format(Locale.ROOT, "%.1f", pct) + "%, " + gpuCullCommands + " cmds)");
        } else if (com.hbm_m.config.ModClothConfig.get().getEffectiveOcclusionCullingMode() == com.hbm_m.config.ModClothConfig.OcclusionCullingMode.GPU) {
            out.add("GPU Cull: §eWaiting/No depth §7(mode=GPU)");
        } else {
            out.add("GPU Cull: §7Disabled (mode=" + com.hbm_m.config.ModClothConfig.get().getEffectiveOcclusionCullingMode() + ")");
        }

        PersistentUploadStaging staging = PersistentUploadStaging.peekOrNull();
        String channel = (staging != null) ? "ring" : "subdata";
        if (uploadSpans == 0) {
            out.add("Uploads: §a0 B §7(static, " + channel + ")");
        } else {
            out.add("Uploads: §f" + uploadSpans + "§7 spans, " + humanBytes(uploadBytes) + " §7(" + channel + ")");
        }

        long vram = ((atlas != null && atlas.isReady()) ? atlas.estimateVramBytes() : 0L)
                + RENDERER_VRAM.get()
                + (staging != null ? staging.getCapacityBytes() : 0L);
        out.add("VRAM Used: §7" + humanBytes(vram));

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
            modeLine.append("§d").append("Iris instanced".equals(mdiVariant) ? mdiVariant : "Iris batch");
        }
        if (modeDirect) {
            if (modeLine.length() > 0) modeLine.append(" + ");
            modeLine.append("§7Instanced (direct)");
        }
        out.add("Mode: " + (modeLine.length() > 0 ? modeLine : "§8idle"));

        // Shadow pass of this frame: batch records / flush / degradations in putBulkData.
        // fallback>0 shown red - the global shadow batch is poisoned, shadows fall back to CPU.
        if (shadowRecords > 0 || shadowFlushDraws > 0 || shadowFallbackParts > 0) {
            StringBuilder sh = new StringBuilder("Shadow: §a").append(shadowRecords)
                    .append("§r rec, ")
                    .append(shadowFlushTier.isEmpty() ? "§8no flush" : "§d" + shadowFlushTier)
                    .append("§r ").append(shadowFlushDraws).append("dr/")
                    .append(shadowFlushInstances).append("inst");
            if (shadowFallbackParts > 0) {
                sh.append(", §c").append(shadowFallbackParts).append(" FALLBACK§r");
            }
            out.add(sh.toString());
        }
    }

    private static String humanBytes(long b) {
        if (b < 1024L) return b + " B";
        if (b < 1024L * 1024L) return String.format(Locale.ROOT, "%.1f KB", b / 1024.0);
        if (b < 1024L * 1024L * 1024L) return String.format(Locale.ROOT, "%.1f MB", b / (1024.0 * 1024.0));
        return String.format(Locale.ROOT, "%.2f GB", b / (1024.0 * 1024.0 * 1024.0));
    }
}
