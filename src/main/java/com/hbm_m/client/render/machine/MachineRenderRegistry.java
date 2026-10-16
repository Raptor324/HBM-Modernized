package com.hbm_m.client.render.machine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.joml.Matrix4f;

import com.hbm_m.main.MainRegistry;

/**
 * Реестр всех фабричных спек станков. Заменяет пер-renderer бойлерплейт:
 * <ul>
 *   <li>{@link #flushAll(Matrix4f)} — единый instanced-flush (InstancedRenderFrame
 *       зовёт его в AFTER_BLOCK_ENTITIES вместо N хардкодов flushInstancedBatches);</li>
 *   <li>{@link #clearAll()} — единая инвалидация GPU-кешей (вызывается из
 *       {@code com.hbm_m.client.render.cache.RenderCacheManager} на reload/disconnect).</li>
 * </ul>
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public final class MachineRenderRegistry {

    private static final List<MachineSpec<?>> SPECS = new ArrayList<>();

    private MachineRenderRegistry() {}

    static void register(MachineSpec<?> spec) {
        SPECS.add(spec);
        MainRegistry.LOGGER.info("[MachineRenderers] registered '{}' ({} parts)", spec.id(), spec.parts().size());
    }

    public static List<MachineSpec<?>> specs() {
        return Collections.unmodifiableList(SPECS);
    }

    /** Единый instanced-flush всех фабричных станков (render thread, AFTER_BLOCK_ENTITIES). */
    public static void flushAll(Matrix4f projection) {
        for (int i = 0; i < SPECS.size(); i++) {
            SPECS.get(i).flush(projection);
        }
    }

    /** Единая инвалидация GPU-кешей всех фабричных станков (render thread). */
    public static void clearAll() {
        for (int i = 0; i < SPECS.size(); i++) {
            SPECS.get(i).clear();
        }
    }
}
