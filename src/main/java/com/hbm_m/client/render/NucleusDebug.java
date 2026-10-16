package com.hbm_m.client.render;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
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
        drawCalls += calls;
        instancesDrawn += instances;
        switch (drawMode) {
            case "MDI (multi)":
            case "MDI (indirect loop)":
                modeMdi = true;
                mdiVariant = drawMode;
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
        if (ClientRenderFlags.enableOcclusionCulling() && machinesCulled > 0) {
            rendering.append(", §7").append(machinesCulled).append(" culled");
        }
        out.add(rendering.toString());

        out.add("Draw Calls: §b" + drawCalls);
        out.add("Instances Drawn: §7" + instancesDrawn);

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
    }

    private static String humanBytes(long b) {
        if (b < 1024L) return b + " B";
        if (b < 1024L * 1024L) return String.format(Locale.ROOT, "%.1f KB", b / 1024.0);
        if (b < 1024L * 1024L * 1024L) return String.format(Locale.ROOT, "%.1f MB", b / (1024.0 * 1024.0));
        return String.format(Locale.ROOT, "%.2f GB", b / (1024.0 * 1024.0 * 1024.0));
    }
}
