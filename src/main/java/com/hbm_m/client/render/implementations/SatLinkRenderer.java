package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.machines.MachineSatLinkBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code RenderSatLink}: base, slew ring (rotation rot) and dish (elevation lift around height 7.375) from
 * {@code satlink.obj}; model centered on the 2x2 surface.
 */
public class SatLinkRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineSatLinkBlockEntity> {

    private static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/machines/satlink.obj"));
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/satlink.png");

    public SatLinkRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineSatLinkBlockEntity link, float interp, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5D, 0, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(180));

        // Original: dir = Meta - 10 (= FACING toward player), rot = dir.getRotation(DOWN) (= counterclockwise);
        // offset (dir + rot) * 0.5 in already rotated coordinate system.
        Direction dir = link.getBlockState().getValue(DummyableMachineBlock.FACING);
        Direction rot = dir.getCounterClockWise();
        pose.translate((dir.getStepX() + rot.getStepX()) * 0.5, 0, (dir.getStepZ() + rot.getStepZ()) * 0.5);

        float r = link.prevRot + (link.rot - link.prevRot) * interp;
        float l = link.prevLift + (link.lift - link.prevLift) * interp;

        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(TEXTURE));
        MODEL.renderPart("Base", pose, vc, light);
        pose.mulPose(Axis.YP.rotationDegrees(r));
        MODEL.renderPart("Rotor", pose, vc, light);
        pose.translate(0, 7.375, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(l));
        pose.translate(0, -7.375, 0);
        MODEL.renderPart("Dish", pose, vc, light);

        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(MachineSatLinkBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
