package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.TeslaBlockEntity;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;

/**
 * 1:1-Port des Blitzteils von {@code RenderTesla} (1.7.10): die Spule selbst steckt im
 * Blockmodell, hier gehen von ihrer Spitze die Entladungen zu den getroffenen Zielen -
 * {@code BeamPronter.prontBeam(RANDOM, SOLID, 0x404040, ..., length * 5, 0.125, 2, 0.03125)}:
 * eine zufaellig gezackte Linie mit fuenf Stuetzpunkten je Block, Zacken bis 1/8 Block,
 * 1/32 Block dick, die Zufallsfolge wechselt mit jedem Tick.
 */
public class TeslaRenderer implements com.hbm_m.client.render.HbmBerBounds<TeslaBlockEntity> {

    private static final float THICKNESS = 0.03125F;
    private static final double JITTER = 0.125D;
    private static final int R = 0x40, G = 0x40, B = 0x40;

    public TeslaRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public boolean shouldRenderOffScreen(TeslaBlockEntity be) {
        return true;
    }

    @Override
    public void render(TeslaBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (be.getTargets().isEmpty() || be.getLevel() == null) return;
        VertexConsumer vc = buffer.getBuffer(RenderType.debugQuads());
        pose.pushPose();
        pose.translate(0.5D, TeslaBlockEntity.ORIGIN_OFFSET_Y, 0.5D);
        Matrix4f m = pose.last().pose();
        long seed = be.getLevel().getGameTime() % 1000 + 1;
        int index = 0;
        for (Vec3 target : be.getTargets()) {
            arc(vc, m, target, seed + index++ * 7919L);
        }
        pose.popPose();
    }

    private static void arc(VertexConsumer vc, Matrix4f m, Vec3 target, long seed) {
        double length = target.length();
        int segments = Math.max(1, (int) (length * 5D));
        RandomSource rand = RandomSource.create(seed);
        Vec3 prev = Vec3.ZERO;
        for (int i = 1; i <= segments; i++) {
            Vec3 point = target.scale((double) i / segments);
            if (i < segments) {
                point = point.add((rand.nextDouble() - 0.5D) * JITTER, (rand.nextDouble() - 0.5D) * JITTER,
                        (rand.nextDouble() - 0.5D) * JITTER);
            }
            segment(vc, m, prev, point);
            prev = point;
        }
    }

    /** Two crossed quads along the segment - the "SOLID" beam of the original. */
    private static void segment(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b) {
        Vec3 dir = b.subtract(a);
        if (dir.lengthSqr() < 1.0E-8D) return;
        Vec3 up = Math.abs(dir.normalize().y) > 0.9D ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 side1 = dir.cross(up).normalize().scale(THICKNESS);
        Vec3 side2 = dir.cross(side1).normalize().scale(THICKNESS);
        quad(vc, m, a.add(side1), b.add(side1), b.subtract(side1), a.subtract(side1));
        quad(vc, m, a.add(side2), b.add(side2), b.subtract(side2), a.subtract(side2));
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3) {
        for (Vec3 p : new Vec3[] {p0, p1, p2, p3}) {
            RenderHooks.vertexColor(vc, m, (float) p.x, (float) p.y, (float) p.z, R, G, B, 255);
        }
        // Back face, so the strip is visible from both sides.
        for (Vec3 p : new Vec3[] {p3, p2, p1, p0}) {
            RenderHooks.vertexColor(vc, m, (float) p.x, (float) p.y, (float) p.z, R, G, B, 255);
        }
    }
}
