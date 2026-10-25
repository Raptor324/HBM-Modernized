package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.MachineThresherBlock;
import com.hbm_m.blockentity.machines.MachineThresherBlockEntity;
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
 * 1:1 {@code RenderThresher}: Sockel, wippender Motor (nur im Betrieb) und der dreigliedrige Arm, dessen Glieder mit
 * {@code 82.5 - angle}, {@code -2x} und wieder {@code +x} um die X-Achse knicken, vorne das drehende Messerrad.
 */
public class ThresherRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineThresherBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/thresher.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/thresher.png");

    public ThresherRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineThresherBlockEntity thresher, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        // Original-Metadaten 3/5/2/4 = Port-FACING NORTH/WEST/SOUTH/EAST (FACING = meta.getOpposite())
        switch (thresher.getBlockState().getValue(MachineThresherBlock.FACING)) {
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(270));
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(90));
            default -> { }
        }

        double angle = thresher.prevAngle + (thresher.angle - thresher.prevAngle) * interp;
        double spin = thresher.lastSpin + (thresher.spin - thresher.lastSpin) * interp;
        double engine = thresher.isOn && thresher.getLevel() != null
                ? Math.sin(thresher.getLevel().getGameTime() * 2 % (Math.PI * 2) + interp) : 0;

        renderCommon(ps, buf.getBuffer(RenderType.entityCutout(TEX)), light, 82.5 - angle, spin, engine);

        ps.popPose();
    }

    public static void renderCommon(PoseStack ps, VertexConsumer vc, int light, double angle, double spin, double engine) {
        MODEL.renderPart("Base", ps, vc, light);

        ps.pushPose();
        ps.translate(0, engine * 0.01, 0);
        MODEL.renderPart("Engine", ps, vc, light);
        ps.popPose();

        ps.translate(0, 0.5, -1);
        ps.mulPose(Axis.XP.rotationDegrees((float) angle));
        ps.translate(0, -0.5, 1);
        MODEL.renderPart("ArmUpper", ps, vc, light);

        ps.translate(0, 0.5, -5);
        ps.mulPose(Axis.XP.rotationDegrees((float) (angle * -2)));
        ps.translate(0, -0.5, 5);
        ps.translate(-0.01, 0, 0);
        MODEL.renderPart("ArmLower", ps, vc, light);
        ps.translate(0.01, 0, 0);

        ps.translate(0, 0.5, -9);
        ps.mulPose(Axis.XP.rotationDegrees((float) angle));
        ps.translate(0, -0.5, 9);
        ps.translate(0.01, 0, 0);
        MODEL.renderPart("Front", ps, vc, light);

        ps.translate(0, 0.5, -11);
        ps.mulPose(Axis.XP.rotationDegrees((float) -spin));
        ps.translate(0, -0.5, 11);
        MODEL.renderPart("Wheel", ps, vc, light);
    }

    @Override public boolean shouldRenderOffScreen(MachineThresherBlockEntity te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
