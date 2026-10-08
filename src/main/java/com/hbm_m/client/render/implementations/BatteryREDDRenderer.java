package com.hbm_m.client.render.implementations;

import java.util.Random;

import org.joml.Matrix4f;

import com.hbm_m.block.machines.MachineBatteryREDDBlock;
import com.hbm_m.blockentity.machines.BatteryREDDBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.UvShiftConsumer;
import com.hbm_m.client.render.util.BeamPronter;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code RenderBatteryREDD}: Sockel, drehendes Rad mit vollhellen Lichtern, gelbe Bewegungsschlieren am Radrand
 * (Laenge je nach Tempo), Plasma mit wanderndem Funkeln und bei Drehung zufaellige Entladungsblitze.
 */
public class BatteryREDDRenderer implements BlockEntityRenderer<BatteryREDDBlockEntity> {

    private static ResourceLocation rl(String p) { return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, p); }

    public static final SimpleObjModel MODEL = new SimpleObjModel(rl("models/block/machines/fensu2.obj"));
    public static final ResourceLocation TEX = rl("textures/block/machine/fensu2.png");
    private static final ResourceLocation PLASMA_TEX = rl("textures/block/machine/plasma.png");
    private static final ResourceLocation SPARKLE_TEX = rl("textures/block/machine/plasma_sparkle.png");

    public BatteryREDDRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(BatteryREDDBlockEntity redd, float interp, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);

        switch (redd.getBlockState().getValue(MachineBatteryREDDBlock.FACING)) {
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(270));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(180));
            default -> { }
        }

        MODEL.renderPart("Base", ps, buffers.getBuffer(RenderType.entityCutout(TEX)), light);

        ps.pushPose();
        ps.translate(0, 5.5, 0);
        float speed = redd.getSpeed();
        double rot = redd.prevRotation + (redd.rotation - redd.prevRotation) * interp;
        ps.mulPose(Axis.XP.rotationDegrees((float) rot));
        ps.translate(0, -5.5, 0);

        MODEL.renderPart("Wheel", ps, buffers.getBuffer(RenderType.entityCutout(TEX)), light);
        MODEL.renderPart("Lights", ps, buffers.getBuffer(RenderType.entityCutout(TEX)), LightTexture.FULL_BRIGHT);

        ps.pushPose();
        ps.translate(0, 5.5, 0);
        renderStreaks(ps, buffers.getBuffer(ClientRenderHandler.CustomRenderTypes.ADDITIVE_TRIANGLES), speed);
        ps.popPose();

        renderSparkle(redd, ps, buffers);

        ps.popPose();

        if (speed > 0) renderZaps(redd, ps, buffers);

        ps.popPose();
    }

    /** Die gelben Schlieren: je Radseite acht Speichen, drei auslaufende Segmente ueber {@code span} Grad. */
    private static void renderStreaks(PoseStack ps, VertexConsumer vc, float speed) {
        double len = 4.25D;
        double width = 0.125D;
        double span = speed * 0.75;
        if (span <= 0) return;

        Matrix4f m = ps.last().pose();
        for (int j = -1; j <= 1; j += 2) {
            for (int i = 0; i < 8; i++) {
                double xOffset = 0.8125 * j;
                double[] v = rotX(new double[] { 1, 0 }, i * 45D);
                float[] alphas = { 0.75F, 0.5F, 0.5F, 0.25F, 0.25F, 0F };
                for (int seg = 0; seg < 3; seg++) {
                    double[] next = rotX(v, span);
                    float a0 = alphas[seg * 2], a1 = alphas[seg * 2 + 1];
                    float[][] q = {
                            { (float) xOffset, (float) (v[0] * len - v[0] * width), (float) (v[1] * len - v[1] * width), a0 },
                            { (float) xOffset, (float) (v[0] * len + v[0] * width), (float) (v[1] * len + v[1] * width), a0 },
                            { (float) xOffset, (float) (next[0] * len + next[0] * width), (float) (next[1] * len + next[1] * width), a1 },
                            { (float) xOffset, (float) (next[0] * len - next[0] * width), (float) (next[1] * len - next[1] * width), a1 } };
                    quad(vc, m, q, 0, 1, 2, 3);
                    quad(vc, m, q, 3, 2, 1, 0); // GL_CULL_FACE war aus
                    v = next;
                }
            }
        }
    }

    /** Vec3NT.rotateAroundXDeg auf (y, z). */
    private static double[] rotX(double[] yz, double deg) {
        double a = Math.toRadians(deg);
        double c = Math.cos(a), s = Math.sin(a);
        return new double[] { yz[0] * c + yz[1] * s, yz[1] * c - yz[0] * s };
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float[][] q, int a, int b, int c, int d) {
        for (int k : new int[] { a, b, c, a, c, d }) {
            vc.vertex(m, q[k][0], q[k][1], q[k][2]).color(1F, 1F, 0F, q[k][3]).endVertex();
        }
    }

    private static void renderSparkle(BatteryREDDBlockEntity redd, PoseStack ps, MultiBufferSource buffers) {
        long time = System.currentTimeMillis();
        float alpha = 0.45F + (float) (Math.sin(time / 1000D) * 0.15F);
        float alphaMult = redd.getSpeed() / 15F;
        float r = 1.0F, g = 0.25F, b = 0.75F;

        double mainOsc = sps(time / 1000D) % 1D;
        double sparkleSpin = time / 250D * -1 % 1D;
        double sparkleOsc = Math.sin(time / 1000D) * 0.5D % 1D;

        MODEL.renderPartEntity("Plasma", ps, new UvShiftConsumer(buffers.getBuffer(RenderType.eyes(PLASMA_TEX)), 0F, (float) mainOsc),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, r, g, b, alpha * alphaMult);

        // cost-cutting measure, don't render extra layers from more than 100m away
        var me = Minecraft.getInstance().player;
        if (me != null && me.distanceToSqr(redd.getBlockPos().getX() + 0.5, redd.getBlockPos().getY() + 2.5, redd.getBlockPos().getZ() + 0.5) < 100 * 100) {
            MODEL.renderPartEntity("Plasma", ps, new UvShiftConsumer(buffers.getBuffer(RenderType.eyes(SPARKLE_TEX)), (float) sparkleSpin, (float) sparkleOsc),
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, Math.min(r * 2, 1F), Math.min(g * 2, 1F), Math.min(b * 2, 1F), 0.75F * alphaMult);
        }
    }

    /** {@code BobMathUtil.sps}. */
    private static double sps(double x) {
        return Math.sin(Math.PI / 2D * Math.cos(x));
    }

    private static void renderZaps(BatteryREDDBlockEntity redd, PoseStack ps, MultiBufferSource buffers) {
        Random rand = new Random(redd.getLevel().getGameTime() / 5);
        rand.nextBoolean();

        double[][] zaps = {
                { 3.125, -1.375, 3.75 }, { -3.125, 1.375, 3.75 }, { 3.125, -1.375, -3.75 }, { -3.125, 1.375, -3.75 } };
        int start = (int) (System.currentTimeMillis() % 1000) / 50;

        for (double[] z : zaps) {
            if (rand.nextBoolean()) {
                ps.pushPose();
                ps.translate(z[0], 5.5, 0);
                Vec3 skeleton = new Vec3(z[1], -2.625, z[2]);
                BeamPronter.prontBeam(ps, buffers, skeleton, BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.SOLID, 0x404040, 0x002040, start, 15, 0.25F, 3, 0.0625F);
                BeamPronter.prontBeam(ps, buffers, skeleton, BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.SOLID, 0x404040, 0x002040, start, 1, 0, 3, 0.0625F);
                ps.popPose();
            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen(BatteryREDDBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
