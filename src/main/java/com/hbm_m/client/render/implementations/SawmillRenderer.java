package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.machines.MachineSawmillBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code RenderSawmill}: Gehaeuse, das Blatt (doppelt so schnell, nur wenn eingebaut) und die zwei
 * gegenlaeufigen Zahnraeder, Drehung aus der Hitze des letzten Ticks.
 */
public class SawmillRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineSawmillBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/sawmill.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/sawmill.png");

    public SawmillRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineSawmillBlockEntity sawmill, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        // Original: Metadaten 3/5/2/4 -> 0/90/180/270 Grad
        switch (sawmill.getBlockState().getValue(DummyableMachineBlock.FACING)) {
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }

        float rot = sawmill.lastSpin + (sawmill.spin - sawmill.lastSpin) * interp;
        renderCommon(ps, buf.getBuffer(RenderType.entityCutout(TEX)), light, rot, sawmill.hasBlade);

        ps.popPose();
    }

    public static void renderCommon(PoseStack ps, VertexConsumer vc, int light, float rot, boolean hasBlade) {

        MODEL.renderPart("Main", ps, vc, light);

        if (hasBlade) {
            ps.pushPose();
            ps.translate(0, 1.375, 0);
            ps.mulPose(Axis.ZP.rotationDegrees(-rot * 2));
            ps.translate(0, -1.375, 0);
            MODEL.renderPart("Blade", ps, vc, light);
            ps.popPose();
        }

        ps.pushPose();
        ps.translate(0.5625, 1.375, 0);
        ps.mulPose(Axis.ZP.rotationDegrees(rot));
        ps.translate(-0.5625, -1.375, 0);
        MODEL.renderPart("GearLeft", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(-0.5625, 1.375, 0);
        ps.mulPose(Axis.ZP.rotationDegrees(-rot));
        ps.translate(0.5625, -1.375, 0);
        MODEL.renderPart("GearRight", ps, vc, light);
        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(MachineSawmillBlockEntity te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
