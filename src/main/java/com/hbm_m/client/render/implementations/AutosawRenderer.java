package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.MachineAutosawBlockEntity;
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
 * 1:1 {@code RenderAutosaw}: Sockel, um die Hochachse drehender Aufbau mit wippendem Motor, dreigliedriger Arm
 * ({@code 80 - pitch}, {@code -2x}, {@code +x} um X) und das liegend drehende Saegeblatt an der Spitze.
 */
public class AutosawRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineAutosawBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/autosaw.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/autosaw.png");

    public AutosawRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineAutosawBlockEntity saw, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        double turn = saw.prevRotationYaw + (saw.rotationYaw - saw.prevRotationYaw) * interp;
        double angle = 80 - (saw.prevRotationPitch + (saw.rotationPitch - saw.prevRotationPitch) * interp);
        float spin = saw.lastSpin + (saw.spin - saw.lastSpin) * interp;
        double engine = saw.isOn && saw.getLevel() != null
                ? Math.sin(saw.getLevel().getGameTime() * 2 % (Math.PI * 2) + interp) : 0;

        renderCommon(ps, buf.getBuffer(RenderType.entityCutout(TEX)), light, turn, angle, spin, engine);

        ps.popPose();
    }

    public static void renderCommon(PoseStack ps, VertexConsumer vc, int light, double turn, double angle, double spin, double engine) {
        MODEL.renderPart("Base", ps, vc, light);

        ps.mulPose(Axis.YN.rotationDegrees((float) turn));
        MODEL.renderPart("Main", ps, vc, light);
        ps.pushPose();
        ps.translate(0, engine * 0.01, 0);
        MODEL.renderPart("Engine", ps, vc, light);
        ps.popPose();

        ps.translate(0, 1.75, 0);
        ps.mulPose(Axis.XP.rotationDegrees((float) angle));
        ps.translate(0, -1.75, 0);
        MODEL.renderPart("ArmUpper", ps, vc, light);

        ps.translate(0, 1.75, -4);
        ps.mulPose(Axis.XP.rotationDegrees((float) (angle * -2)));
        ps.translate(0, -1.75, 4);
        ps.translate(-0.01, 0, 0);
        MODEL.renderPart("ArmLower", ps, vc, light);
        ps.translate(0.01, 0, 0);

        ps.translate(0, 1.75, -8);
        ps.mulPose(Axis.XP.rotationDegrees((float) angle));
        ps.translate(0, -1.75, 8);
        MODEL.renderPart("ArmTip", ps, vc, light);

        ps.translate(0, 1.75, -10);
        ps.mulPose(Axis.YN.rotationDegrees((float) spin));
        ps.translate(0, -1.75, 10);
        MODEL.renderPart("Sawblade", ps, vc, light);
    }

    @Override public boolean shouldRenderOffScreen(MachineAutosawBlockEntity te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
