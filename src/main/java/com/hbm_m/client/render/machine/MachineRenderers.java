package com.hbm_m.client.render.machine;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

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
 * Machine render factory - the single entry point.
 * <p>
 * An implementation looks like this (full file):
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
 * The engine provides automatically: culling + fade, a VBO per part, instancing,
 * MDI, Iris/Oculus compatibility and
 * a vanilla immediate fallback (automatic on broken VBO, or forced via the
 * {@code forceVanillaImmediatePath} config).
 */
@OnlyIn(Dist.CLIENT)
public final class MachineRenderers {

    private MachineRenderers() {}

    /** Starts a machine render description. */
    public static <T extends BlockEntity> MachineSpecBuilder<T> machine(
            String id, BlockEntityType<T> type, Class<T> beClass) {
        return new MachineSpecBuilder<>(id, beClass, type);
    }

    // -- builder utilities ------------------------------------------------

    /** Default model - the blockstate multipart model (hbm_m:*_loader). */
    static BakedModel blockstateModel(BlockEntity be) {
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(be.getBlockState());
    }

    /** Default facing - HORIZONTAL_FACING / FACING from the blockstate, otherwise NORTH. */
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
