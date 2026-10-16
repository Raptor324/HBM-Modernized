package com.hbm_m.client.render;

import com.hbm_m.config.ModClothConfig;

/**
 * Per-render-frame snapshot of hot {@link ModClothConfig} rendering flags.
 * Updated once at the start of the block-entity pass to avoid thousands of
 * {@link ModClothConfig#get()} calls when instancing large machine fields.
 * <p>
 * Инстансинг/MDI/GPU-bone skinning включаются автоматически и переключателей
 * в конфиге больше не имеют; единственный ручной резерв —
 * {@link #forceVanillaImmediate()} (ванильный immediate-путь целиком).
 */
public final class ClientRenderFlags {

    private static boolean enableOcclusionCulling;
    /** Same default as {@link ModClothConfig#maxInstancedInstancesPerPart} until {@link #onFrameStart()}. */
    private static int maxInstances = 4096;
    private static boolean forceVanillaImmediate;
    private static boolean mdiCleanFrameReuse = true;

    private ClientRenderFlags() {}

    /** Call from {@link com.hbm_m.client.render.culling.InstancedRenderFrame#onBeforeBlockEntities}. */
    public static void onFrameStart() {
        ModClothConfig cfg = ModClothConfig.get();
        enableOcclusionCulling = cfg.enableOcclusionCulling;
        maxInstances = cfg.maxInstancedInstancesPerPart;
        forceVanillaImmediate = cfg.forceVanillaImmediatePath;
        mdiCleanFrameReuse = cfg.mdiCleanFrameReuse;
        // Один опрос Iris API за кадр — isExternalShaderActive() дальше читает кеш.
        com.hbm_m.client.render.shader.ShaderCompatibilityDetector.updateState();
    }

    /**
     * Instanced batching is always enabled unless the user forces the vanilla
     * immediate path via {@link ModClothConfig#forceVanillaImmediatePath}.
     */
    public static boolean useInstancedBatching() {
        return !forceVanillaImmediate;
    }

    /**
     * GPU bone skinning (per-vertex bone id + per-instance base×part matrix) is
     * applied automatically wherever the engine uses it; no user toggle.
     */
    public static boolean gpuBoneSkinning() {
        return true;
    }

    public static boolean enableOcclusionCulling() {
        return enableOcclusionCulling;
    }

    /**
     * MDI clean-frame reuse (submitClean/retained draw list). Kill-switch
     * {@link ModClothConfig#mdiCleanFrameReuse}; default on.
     */
    public static boolean mdiCleanFrameReuse() {
        return mdiCleanFrameReuse;
    }

    /**
     * Истинный {@code glDrawElementsInstanced} под Iris/Oculus через наш
     * ExtendedShader. ВКЛЮЧАЕТСЯ автоматически, когда у активного пака
     * распознана схема gbuffer, для которой есть энкодер
     * ({@link com.hbm_m.client.render.shader.IrisInstancedEncoders} — детект
     * по исходнику gbuffer-программы пака, без имён паков; схема "packed" —
     * gbuffer пакуется парами unorm8: albedo/normal/light).
     * <p>
     * Без распознанной схемы режим уводит main-проход на companion
     * per-instance через pack-программу BLOCK_ENTITY, shadow-батч — на
     * pack-программу SHADOW_*: корректно под любым паком. Однотаргетный
     * «ванильный» FSH под deferred-паком даёт чёрные машины (композит
     * декодирует мусор), поэтому вслепую инстансинг не включается.
     */
    public static boolean irisTrueInstancing() {
        return com.hbm_m.client.render.shader.ShaderCompatibilityDetector.isExternalShaderActive()
                && com.hbm_m.client.render.shader.IrisInstancedEncoders.hasEncoder();
    }

    /**
     * Force the vanilla immediate (putBulkData) path for every machine render.
     * Read live (also safe before {@link #onFrameStart}).
     */
    public static boolean forceVanillaImmediate() {
        try {
            return ModClothConfig.get().forceVanillaImmediatePath;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Per-part instance cap. Safe before {@link #onFrameStart()} (renderer construction, buffer sizing).
     */
    public static int maxInstances() {
        if (maxInstances > 0) {
            return maxInstances;
        }
        try {
            int cfg = ModClothConfig.get().maxInstancedInstancesPerPart;
            return cfg > 0 ? cfg : 4096;
        } catch (Throwable ignored) {
            return 4096;
        }
    }
}
