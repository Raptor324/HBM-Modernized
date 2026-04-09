package com.hbm_m.client.render.flywheel;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.entity.machines.MachineAdvancedAssemblerBlockEntity;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Пустой визуал при ошибке инициализации основного Flywheel-пути (fail-closed без краша).
 */
@OnlyIn(Dist.CLIENT)
public final class FailedAdvancedAssemblerFlywheelVisual extends AbstractBlockEntityVisual<MachineAdvancedAssemblerBlockEntity>
        implements SimpleDynamicVisual {

    public FailedAdvancedAssemblerFlywheelVisual(VisualizationContext ctx, MachineAdvancedAssemblerBlockEntity blockEntity, float partialTick) {
        super(ctx, blockEntity, partialTick);
    }

    @Override
    public void beginFrame(Context context) {
        // no-op
    }

    @Override
    public void updateLight(float partialTick) {
        // no-op
    }

    @Override
    public void collectCrumblingInstances(Consumer<@Nullable Instance> consumer) {
        // no-op
    }

    @Override
    protected void _delete() {
        // no-op
    }
}
