package com.hbm_m.client.render.flywheel;

import java.util.concurrent.atomic.AtomicBoolean;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.entity.machines.MachineAdvancedAssemblerBlockEntity;
import com.hbm_m.main.MainRegistry;

import dev.engine_room.flywheel.api.visual.BlockEntityVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizerRegistry;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Регистрация Flywheel-визуализатора для Advanced Assembler (загружается только при наличии мода flywheel).
 */
@OnlyIn(Dist.CLIENT)
public final class AdvancedAssemblerFlywheelSetup {

    private static final AtomicBoolean LOGGED_FACTORY_FAIL = new AtomicBoolean();

    private AdvancedAssemblerFlywheelSetup() {
    }

    public static void register() {
        SimpleBlockEntityVisualizer.<MachineAdvancedAssemblerBlockEntity>builder(ModBlockEntities.ADVANCED_ASSEMBLY_MACHINE_BE.get())
                .factory(AdvancedAssemblerFlywheelSetup::createVisual)
                .neverSkipVanillaRender()
                .apply();
        MainRegistry.LOGGER.info("[HBM-M] Registered Flywheel visualizer for Advanced Assembler");
    }

    private static BlockEntityVisual<? super MachineAdvancedAssemblerBlockEntity> createVisual(
            VisualizationContext ctx,
            MachineAdvancedAssemblerBlockEntity be,
            float partialTick) {
        try {
            return new AdvancedAssemblerFlywheelVisual(ctx, be, partialTick);
        } catch (Throwable t) {
            if (LOGGED_FACTORY_FAIL.compareAndSet(false, true)) {
                MainRegistry.LOGGER.error("[HBM-M] Advanced Assembler Flywheel visual failed; using no-op fallback per-block-entity", t);
            }
            return new FailedAdvancedAssemblerFlywheelVisual(ctx, be, partialTick);
        }
    }

    public static void clear() {
        VisualizerRegistry.setVisualizer(ModBlockEntities.ADVANCED_ASSEMBLY_MACHINE_BE.get(), null);
    }
}
