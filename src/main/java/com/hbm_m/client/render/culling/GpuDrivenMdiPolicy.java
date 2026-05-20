package com.hbm_m.client.render.culling;

import com.hbm_m.client.render.MdiBatchCoordinator;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.config.ModClothConfig;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
*///?}

/** Условия включения GPU-driven MDI (cull + compact indirect без CPU readback). */
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public final class GpuDrivenMdiPolicy {

    private GpuDrivenMdiPolicy() {}

    public static boolean isActive() {
        ModClothConfig cfg;
        try {
            cfg = ModClothConfig.get();
        } catch (Throwable t) {
            return false;
        }
        if (cfg == null || !cfg.enableOcclusionCulling || !cfg.gpuDrivenMdiCulling) {
            return false;
        }
        if (!cfg.useMultiDrawIndirect || ShaderCompatibilityDetector.isExternalShaderActive()) {
            return false;
        }
        if (!MdiBatchCoordinator.isMdiAvailable()) {
            return false;
        }
        ModClothConfig.CullingMode configured = cfg.cullingMode;
        if (configured == ModClothConfig.CullingMode.LEGACY_RAYCAST) {
            return false;
        }
        if (configured == ModClothConfig.CullingMode.CPU_FRUSTUM) {
            return false;
        }
        return GpuCullingPipeline.isSupported() || GpuCullingPipeline.initialize();
    }
}
