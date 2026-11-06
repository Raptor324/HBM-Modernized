package com.hbm_m.client.render.implementations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Shared helpers for BERs that previously were static block models: derive original metadata from
 * FACING so the original TESR rotation switches can be reused directly.
 */
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class ObjBerHelper {

    private ObjBerHelper() {}

    /** Orientation from HORIZONTAL_FACING / FACING, otherwise North. */
    public static Direction facing(BlockEntity be) {
        BlockState state = be.getBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        if (state.hasProperty(BlockStateProperties.FACING)) {
            Direction d = state.getValue(BlockStateProperties.FACING);
            return d.getAxis().isHorizontal() ? d : Direction.NORTH;
        }
        // Custom "facing" properties (e.g. BarrelTankBlock)
        for (net.minecraft.world.level.block.state.properties.Property<?> p : state.getProperties()) {
            if ("facing".equals(p.getName()) && state.getValue(p) instanceof Direction d && d.getAxis().isHorizontal()) return d;
        }
        return Direction.NORTH;
    }

    /**
     * Original direction metadata (ForgeDirection ordinal 2 North, 3 South, 4 West, 5 East; for BlockDummyable
     * {@code meta - offset}). The port sets FACING to the side facing the player, matching original.
     */
    public static int meta(BlockEntity be) {
        return switch (facing(be)) {
            case SOUTH -> 3;
            case WEST -> 4;
            case EAST -> 5;
            default -> 2;
        };
    }

    /** Like {@link #meta}, but including up/down (ForgeDirection ordinal 0 down, 1 up) for 6-sided blocks. */
    public static int meta6(BlockEntity be) {
        BlockState state = be.getBlockState();
        if (state.hasProperty(BlockStateProperties.FACING)) return state.getValue(BlockStateProperties.FACING).get3DDataValue();
        return meta(be);
    }

    /** {@code GL11.glRotatef(deg, 0, 1, 0)}. */
    public static void rotY(PoseStack ps, float deg) {
        ps.mulPose(Axis.YP.rotationDegrees(deg));
    }

    /** Push blockstate Y-rotation (north 0, east 90, south 180, west 270) onto pose stack, pivot at block center. */
    public static void rotateLikeBlockstate(PoseStack ps, Direction facing) {
        float y = (facing.toYRot() + 180F) % 360F;
        ps.mulPose(Axis.YP.rotationDegrees(-y));
    }

    /**
     * {@code RenderDecoItem} with {@code RenderItem.renderInFrame = true} (no bobbing, single piece): frame scaling
     * 0.5128205 and -0.05; blocks 1.25x, +0.05, -90 degrees, quarter size; flat items rotated 180 degrees,
     * half size, icon from -0.25 to 0.75 (center +0.25).
     */
    public static void renderDecoItemInFrame(PoseStack ps, net.minecraft.client.renderer.MultiBufferSource buf,
                                             net.minecraft.world.item.ItemStack stack, int light,
                                             @org.jetbrains.annotations.Nullable net.minecraft.world.level.Level level) {
        if (stack.isEmpty()) return;
        net.minecraft.world.item.ItemStack one = stack.copy();
        one.setCount(1);
        ps.pushPose();
        ps.scale(0.5128205F, 0.5128205F, 0.5128205F);
        ps.translate(0.0F, -0.05F, 0.0F);
        if (one.getItem() instanceof net.minecraft.world.item.BlockItem) {
            ps.scale(1.25F, 1.25F, 1.25F);
            ps.translate(0.0F, 0.05F, 0.0F);
            ps.mulPose(Axis.YP.rotationDegrees(-90.0F));
            ps.scale(0.25F, 0.25F, 0.25F);
        } else {
            ps.mulPose(Axis.YP.rotationDegrees(180.0F));
            ps.scale(0.5F, 0.5F, 0.5F);
            ps.translate(0.0F, 0.25F, 0.0F);
        }
        net.minecraft.client.Minecraft.getInstance().getItemRenderer().renderStatic(one,
                net.minecraft.world.item.ItemDisplayContext.NONE, light,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, ps, buf, level, 0);
        ps.popPose();
    }
}
