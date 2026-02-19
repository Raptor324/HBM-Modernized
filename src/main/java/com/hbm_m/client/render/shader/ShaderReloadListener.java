package com.hbm_m.client.render.shader;

import com.hbm_m.main.MainRegistry;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Слушатель перезагрузки ресурсов для детектирования изменений шейдеров
 * Срабатывает при F3+T, смене шейдерпака или загрузке мира
 */
@OnlyIn(Dist.CLIENT)
public class ShaderReloadListener extends SimplePreparableReloadListener<Void> {
    
    /**
     * Фаза подготовки (асинхронная)
     */
    @Override
    protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        profiler.push("hbm_shader_detection");
        MainRegistry.LOGGER.debug("ShaderReloadListener: prepare phase");
        profiler.pop();
        return null;
    }
    
    /**
     * Фаза применения (синхронная, главный поток)
     * Здесь безопасно обращаться к игровым объектам
     */
    @Override
    protected void apply(Void prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
        profiler.push("hbm_shader_detection_apply");
        
        try {
            MainRegistry.LOGGER.debug("ShaderReloadListener: Resource reload complete");
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Error during shader reload", e);
        } finally {
            profiler.pop();
        }
    }
}
