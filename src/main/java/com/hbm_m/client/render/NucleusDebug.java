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
 * Счётчики движка рендера Nucleus + секция F3-экрана.
 * <p>
 * Per-frame счётчики сбрасываются в {@link #onFrameStart()} из
 * {@code InstancedRenderFrame.onBeforeBlockEntities} (начало BE-прохода) —
 * F3-оверлей рисуется позже в том же кадре, поэтому видит актуальные значения.
 * Регистрация в shadow-проходе игнорируется (централизованно в точках записи),
 * иначе под Iris машины считались бы дважды.
 */
public final class NucleusDebug {

    // ── Per-frame (сброс в onFrameStart) ───────────────────────────────
    private static int drawCalls;
    private static int instancesDrawn;
    private static int machinesRendered;
    private static int machinesCulled;
    private static int uploadSpans;
    private static long uploadBytes;
    /** Какие пути рисовали в кадре. Отдельные флаги, а не «последний путь»:
     *  chain-части (GPU-bones) всегда идут мимо атласа и рисуются ПОСЛЕ MDI —
     *  строка mode не должна от этого «падать» на direct. */
    private static boolean modeMdi;
    private static boolean modeDirect;
    private static boolean modeIris;
    private static boolean modeImmediate;
    private static String mdiVariant = "MDI (multi)";
    /** MDI clean-frame reuse: всего рендереров кадра / переиспользованных / инстансов в них. */
    private static int mdiRenderersTotal;
    private static int mdiRenderersClean;
    private static int mdiInstancesClean;

    // ── GPU Culling stats ──────────────────────────────────────────────
    private static volatile boolean gpuCullActive;
    private static volatile int gpuCullCommands;
    private static volatile int gpuCullInstancesIn;
    private static volatile int gpuCullInstancesOut;

    // ── Shadow-pass stats (НЕ гейтятся counting(): main-счётчики в тенях спят,
    //    эти наоборот живут только там) ─────────────────────────────────────
    // Сброс — в onShadowPassStart (миксин HEAD renderShadows): теневая фаза кадра
    // идёт ДО main-фазы, поэтому onFrameStart (main BE-старт) затирал бы счётчики
    // до отрисовки F3, и строка Shadow никогда не показывалась.
    private static volatile int shadowFallbackParts;
    private static volatile int shadowRecords;
    private static volatile int shadowFlushDraws;
    private static volatile int shadowFlushInstances;
    private static volatile String shadowFlushTier = "";

    /** Истинный старт теневой фазы кадра — миксин HEAD Iris ShadowRenderer.renderShadows. */
    public static void onShadowPassStart() {
        shadowFallbackParts = 0;
        shadowRecords = 0;
        shadowFlushDraws = 0;
        shadowFlushInstances = 0;
        shadowFlushTier = "";
    }

    /** Часть ушла в putBulkData внутри shadow-прохода (поражает глобальный shadow-батч). */
    public static void recordShadowFallback() {
        shadowFallbackParts++;
    }

    /** Инстанс записан в IrisShadowBatchCollector в shadow-проходе. */
    public static void recordShadowRecord(int instances) {
        if (instances > 0) shadowRecords += instances;
    }

    /** Итог флаша глобального shadow-батча (tier + draw/instance counts). */
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
    /** Сумма VBO прямого пути по живым InstancedStaticPartRenderer (вершины+индексы+instance VBO). */
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
     * Per-frame MDI clean-reuse статистика (вызывается один раз из prepareMdiDraw).
     *
     * @param total  рендереров в кадре (retained draw list)
     * @param clean  из них переиспользованных без снапшот-копии и диффа
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
     * Секция F3. Формат:
     * <pre>
     * [Nucleus] Active                      — жёлтый заголовок; Active зелёный / Not rendering красный
     * Geometry Parts in Atlas: N            — уникальные part-меши (не машины); растёт лениво
     * Rendering: N models, M culled         — culled только при включённом куллинге
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
        // peek без создания: F3 в меню не должен инстанцировать атлас/staging.
        MdiGeometryAtlas atlas = MdiGeometryAtlas.peekOrNull();
        int atlasParts = (atlas != null && atlas.isReady()) ? atlas.getRegisteredGeometryCount() : 0;
        // Число уникальных part-мешей в атласе (одна запись на часть ТИПА станка;
        // динамические части — отдельная запись на вариант, напр. танк per-флюид).
        // Растёт лениво по мере первого появления типов/вариантов в кадре.
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

        // Immediate (fallback) — деградация: красным и первым в списке.
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

        // Shadow-проход этого кадра: запись в батч / флаш / деградации в putBulkData.
        // fallback>0 красным — глобальный shadow-батч поражён, тени считает CPU.
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
