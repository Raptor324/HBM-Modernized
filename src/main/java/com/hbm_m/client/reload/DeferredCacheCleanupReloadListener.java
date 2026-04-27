package com.hbm_m.client.reload;

import com.hbm_m.client.render.GlobalMeshCache;
import com.hbm_m.client.render.implementations.DoorRenderer;
import com.hbm_m.client.render.implementations.MachineAdvancedAssemblerRenderer;
import com.hbm_m.client.render.implementations.MachineAssemblerRenderer;
import com.hbm_m.client.render.implementations.MachineChemicalPlantRenderer;
import com.hbm_m.client.render.implementations.MachineHydraulicFrackiningTowerRenderer;
import com.hbm_m.client.render.implementations.MachinePressRenderer;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.powerarmor.layer.AbstractObjArmorLayer;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Критично: очистку VBO/mesh кэшей делаем на render thread (recordRenderCall),
 * иначе при активном внешнем шейдере можно словить race во время render pass.
 */
public final class DeferredCacheCleanupReloadListener extends SimplePreparableReloadListener<Void> {
    @Override
    protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return null;
    }

    @Override
    protected void apply(Void prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
        RenderSystem.recordRenderCall(() -> {
            try {
                MachineAdvancedAssemblerRenderer.clearCaches();
                MachineAssemblerRenderer.clearCaches();
                MachineHydraulicFrackiningTowerRenderer.clearCaches();
                DoorRenderer.clearAllCaches();
                MachinePressRenderer.clearCaches();
                MachineChemicalPlantRenderer.clearCaches();
                GlobalMeshCache.clearAll();
                AbstractObjArmorLayer.clearAllCaches();
                MainRegistry.LOGGER.info("VBO cache cleanup completed (deferred to render thread)");
            } catch (Exception e) {
                MainRegistry.LOGGER.error("Error during deferred VBO cache cleanup", e);
            }
        });
    }
}

