package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.machines.MachineLpw2BlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.UvShiftConsumer;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code RenderLPW2}: das Schiffstriebwerk mit schwankendem Hauptaufbau, Rotor und Turbinen, kardanisch
 * pendelnder Duese samt Klappenring, federnden Aufhaengungen, zitternden Servern und dem Fehlerbildschirm.
 */
public class Lpw2Renderer implements com.hbm_m.client.render.HbmBerBounds<MachineLpw2BlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/lpw2.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/lpw2.png");
    public static final ResourceLocation ERROR_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/lpw2_term_error.png");

    public Lpw2Renderer(BlockEntityRendererProvider.Context ctx) { }

    private static double sps(double x) {
        return Math.sin(Math.PI / 2D * Math.cos(x));
    }

    private static void part(String name, PoseStack ps, VertexConsumer vc, int light) {
        MODEL.renderPart(name, ps, vc, light);
    }

    @Override
    public void render(MachineLpw2BlockEntity te, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        // Original: Metadaten 2/4/3/5 -> 90/180/270/0 Grad
        switch (te.getBlockState().getValue(DummyableMachineBlock.FACING)) {
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }

        long time = te.getLevel() == null ? 0 : te.getLevel().getGameTime() % 1000000;

        double swayTimer = ((time + interp) / 3D) % (Math.PI * 4);
        double sway = (Math.sin(swayTimer) + Math.sin(swayTimer * 2) + Math.sin(swayTimer * 4) + 2.23255D) * 0.5;

        double bellTimer = ((time + interp) / 5D) % (Math.PI * 4);
        double h = (Math.sin(bellTimer + Math.PI) + Math.sin(bellTimer * 1.5D)) / 1.90596D;
        double v = (Math.sin(bellTimer) + Math.sin(bellTimer * 1.5D)) / 1.90596D;

        double pistonTimer = ((time + interp) / 5D) % (Math.PI * 2);
        double piston = sps(pistonTimer);
        double rotorTimer = ((time + interp) / 5D) % (Math.PI * 16);
        double rotor = (sps(rotorTimer) + rotorTimer / 2D - 1) / 25.1327412287D;
        double turbine = ((time + interp) % 100) / 100D;

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(TEX));
        part("Frame", ps, vc, light);

        renderMainAssembly(ps, vc, light, sway, h, v, piston, rotor, turbine);

        ps.pushPose();
        ps.translate(-2.9375, 0, 2.375);
        ps.mulPose(Axis.YP.rotationDegrees((float) (sway * 10)));
        ps.translate(2.9375, 0, -2.375);
        part("WireLeft", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(2.9375, 0, 2.375);
        ps.mulPose(Axis.YP.rotationDegrees((float) (sway * -10)));
        ps.translate(-2.9375, 0, -2.375);
        part("WireRight", ps, vc, light);
        ps.popPose();

        double coverTimer = ((time + interp) / 5D) % (Math.PI * 4);
        double cover = (Math.sin(coverTimer) + Math.sin(coverTimer * 2) + Math.sin(coverTimer * 4)) * 0.5;

        ps.pushPose();
        ps.translate(0, 0, -cover * 0.125);
        part("Cover", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, 0, 3.5);
        ps.scale(1, 1, (float) ((3 + cover * 0.125) / 3));
        ps.translate(0, 0, -3.5);
        part("SuspensionCoverFront", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, 0, -5.5);
        ps.scale(1, 1, (float) ((1.5 - cover * 0.125) / 1.5));
        ps.translate(0, 0, 5.5);
        part("SuspensionCoverBack", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, 0, -9);
        ps.scale(1, 1, (float) ((1.25 - sway * 0.125) / 1.25));
        ps.translate(0, 0, 9);
        part("SuspensionBackOuter", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, 0, -9.5);
        ps.scale(1, 1, (float) ((1.75 - sway * 0.125) / 1.75));
        ps.translate(0, 0, 9.5);
        part("SuspensionBackCenter", ps, vc, light);
        ps.popPose();

        double serverTimer = ((time + interp) / 2D) % (Math.PI * 4);
        double sx = (Math.sin(serverTimer + Math.PI) + Math.sin(serverTimer * 1.5D)) / 1.90596D;
        double sy = (Math.sin(serverTimer) + Math.sin(serverTimer * 1.5D)) / 1.90596D;

        double serverSway = 0.0625D * 0.25D;

        ps.pushPose();
        ps.translate(sx * serverSway, 0, sy * serverSway);
        part("Server1", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(-sy * serverSway, 0, sx * serverSway);
        part("Server2", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(sy * serverSway, 0, -sx * serverSway);
        part("Server3", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(-sx * serverSway, 0, -sy * serverSway);
        part("Server4", ps, vc, light);
        ps.popPose();

        double errorTimer = ((time + interp) / 3D);

        ps.pushPose();
        ps.translate(sy * serverSway, 0, sx * serverSway);

        part("Monitor", ps, vc, light);

        float scroll = (float) ((sps(errorTimer) + errorTimer / 2D) % 1);
        MODEL.renderPart("Screen", ps, new UvShiftConsumer(buf.getBuffer(RenderType.entityCutout(ERROR_TEX)), 0F, scroll), light);

        ps.popPose();

        ps.popPose();
    }

    public static void renderMainAssembly(PoseStack ps, VertexConsumer vc, int light, double sway, double h, double v, double piston, double rotor, double turbine) {
        ps.pushPose();
        ps.translate(0, 0, -sway * 0.125);
        part("Center", ps, vc, light);

        ps.pushPose();
        ps.translate(0, 3.5, 0);

        ps.pushPose();
        ps.mulPose(Axis.ZN.rotationDegrees((float) (rotor * 360)));
        ps.translate(0, -3.5, 0);
        part("Rotor", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.mulPose(Axis.ZP.rotationDegrees((float) (turbine * 360)));
        ps.translate(0, -3.5, 0);
        part("TurbineFront", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.mulPose(Axis.ZN.rotationDegrees((float) (turbine * 360)));
        ps.translate(0, -3.5, 0);
        part("TurbineBack", ps, vc, light);
        ps.popPose();

        ps.popPose();

        ps.pushPose();
        ps.translate(0, 0, piston * 0.375D + 0.375D);
        part("Piston", ps, vc, light);
        ps.popPose();

        renderBell(ps, vc, light, h, v);
        ps.popPose();

        renderShroud(ps, vc, light, h, v);
    }

    public static void renderBell(PoseStack ps, VertexConsumer vc, int light, double h, double v) {
        ps.pushPose();
        ps.translate(0, 3.5, 2.75);
        double magnitude = 2D;
        ps.mulPose(Axis.YP.rotationDegrees((float) (v * magnitude)));
        ps.mulPose(Axis.XP.rotationDegrees((float) (h * magnitude)));
        ps.translate(0, -3.5, -2.75);
        part("Engine", ps, vc, light);
        ps.popPose();
    }

    public static void renderShroud(PoseStack ps, VertexConsumer vc, int light, double h, double v) {

        double magnitude = 0.125D;
        double rotation = 5D;
        double offset = 10D;

        ps.pushPose();
        ps.translate(0, -h * magnitude, 0);
        part("ShroudH", ps, vc, light);

        renderFlap(ps, vc, light, 90 + 22.5D, rotation * v + offset);
        renderFlap(ps, vc, light, 90 - 22.5D, rotation * v + offset);
        renderFlap(ps, vc, light, 270 + 22.5D, rotation * -v + offset);
        renderFlap(ps, vc, light, 270 - 22.5D, rotation * -v + offset);

        ps.popPose();

        ps.pushPose();
        ps.translate(v * magnitude, 0, 0);
        part("ShroudV", ps, vc, light);

        renderFlap(ps, vc, light, 22.5D, rotation * h + offset);
        renderFlap(ps, vc, light, -22.5D, rotation * h + offset);
        renderFlap(ps, vc, light, 180 + 22.5D, rotation * -h + offset);
        renderFlap(ps, vc, light, 180 - 22.5D, rotation * -h + offset);

        ps.popPose();

        double length = 0.6875D;

        ps.pushPose();
        ps.translate(-2.625D, 0, 0);
        ps.scale((float) ((length + v * magnitude) / length), 1, 1);
        ps.translate(2.625D, 0, 0);
        part("SuspensionLeft", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(2.625D, 0, 0);
        ps.scale((float) ((length - v * magnitude) / length), 1, 1);
        ps.translate(-2.625D, 0, 0);
        part("SuspensionRight", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, 6.125D, 0);
        ps.scale(1, (float) ((length + h * magnitude) / length), 1);
        ps.translate(0, -6.125D, 0);
        part("SuspensionTop", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, 0.875D, 0);
        ps.scale(1, (float) ((length - h * magnitude) / length), 1);
        ps.translate(0, -0.875D, 0);
        part("SuspensionBottom", ps, vc, light);
        ps.popPose();
    }

    public static void renderFlap(PoseStack ps, VertexConsumer vc, int light, double position, double rotation) {
        ps.pushPose();

        ps.translate(0, 3.5D, 0);
        ps.mulPose(Axis.ZP.rotationDegrees((float) position));
        ps.translate(0, -3.5D, 0);

        ps.translate(0, 6.96875D, 8.5D);
        ps.mulPose(Axis.XP.rotationDegrees((float) rotation));
        ps.translate(0, -6.96875D, -8.5D);

        part("Flap", ps, vc, light);
        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(MachineLpw2BlockEntity te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
