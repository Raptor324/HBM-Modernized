package com.hbm_m.client.render.machine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Фабрика рендеров станков — единственная точка входа.
 * <p>
 * Имплементация выглядит так (полный файл):
 * <pre>{@code
 * public final class MachinePressRenderer {
 *     public static void register() {
 *         MachineRenderers.machine("press", ModBlockEntities.PRESS.get(), MachinePressBlockEntity.class)
 *             .part("Base")
 *             .part("Head", MachinePressRenderer::animateHead)
 *             .hook(MachinePressRenderer::renderStampItems)
 *             .register();
 *     }
 *     private static void animateHead(MachinePressBlockEntity be, float pt, long t, PoseStack pose) {
 *         pose.pushPose();
 *         pose.translate(0, -be.getProgress(pt) * 0.5f, 0);
 *         pose.popPose();
 *     }
 * }
 * }</pre>
 * Движок автоматически даёт: куллинг + fade, VBO на каждую часть, инстансинг,
 * MDI, Iris/Oculus-совместимость, GPU-bones для {@code .chain(...)}-групп и
 * ванильный immediate-фолбэк (автоматически при сломанном VBO или принудительно
 * через конфиг {@code forceVanillaImmediatePath}).
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public final class MachineRenderers {

    private MachineRenderers() {}

    /** Начало описания рендера станка. */
    public static <T extends BlockEntity> MachineSpecBuilder<T> machine(
            String id, BlockEntityType<T> type, Class<T> beClass) {
        return new MachineSpecBuilder<>(id, beClass, type);
    }

    // ── утилиты для билдера ─────────────────────────────────────────────

    /** Модель по умолчанию — multipart-модель blockstate (hbm_m:*_loader). */
    static BakedModel blockstateModel(BlockEntity be) {
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(be.getBlockState());
    }

    /** Facing по умолчанию — HORIZONTAL_FACING / FACING из blockstate, иначе NORTH. */
    static Direction defaultFacing(BlockEntity be) {
        var state = be.getBlockState();
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
        }
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
            return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        }
        return Direction.NORTH;
    }
}
