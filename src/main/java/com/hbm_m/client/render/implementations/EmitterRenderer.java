package com.hbm_m.client.render.implementations;

import java.awt.Color;

import com.hbm_m.blockentity.decorations.EmitterBlockEntity;
import com.hbm_m.client.render.util.BeamPronter;
import com.hbm_m.client.render.util.BeamPronter.EnumBeamType;
import com.hbm_m.client.render.util.BeamPronter.EnumWaveType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.Vec3;

/** 1:1 {@code RenderEmitter}: gleiche Matrizenfolge, Strahl und Effekte 1-3 ueber {@link BeamPronter}. */
public class EmitterRenderer implements com.hbm_m.client.render.HbmBerBounds<EmitterBlockEntity> {

    public EmitterRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(EmitterBlockEntity emitter, float f, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        if (emitter.getLevel() == null) return;
        ps.pushPose();
        ps.translate(0.5D, 0.0D, 0.5D);
        ps.mulPose(Axis.YP.rotationDegrees(90));

        switch (emitter.facing()) {
            case DOWN -> { ps.translate(0.0D, 0.5D, -0.5D); ps.mulPose(Axis.XP.rotationDegrees(90)); }
            case UP -> { ps.translate(0.0D, 0.5D, 0.5D); ps.mulPose(Axis.XP.rotationDegrees(-90)); }
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(270));
            case EAST -> { }
        }

        ps.translate(0.0D, 0.5D, 0.5D);
        long time = emitter.getLevel().getGameTime();
        int range = emitter.beam - 1;
        int originalColor = emitter.color == 0 ? Color.HSBtoRGB(time / 50.0F, 0.5F, 0.25F) & 16777215 : emitter.color;
        float girth = emitter.girth;
        int r = (originalColor & 0xff0000) >> 16;
        int g = (originalColor & 0x00ff00) >> 8;
        int b = originalColor & 0x0000ff;
        float innerMult = 0.85F;
        float outerMult = 0.1F;
        int colorInner = ((int) (r * innerMult) << 16) | ((int) (g * innerMult) << 8) | ((int) (b * innerMult));
        int colorOuter = ((int) (r * outerMult) << 16) | ((int) (g * outerMult) << 8) | ((int) (b * outerMult));

        if (range > 0) {
            Vec3 skel = new Vec3(0, 0, range);
            int segments = (int) Math.max(Math.sqrt(girth * 50), 2);
            BeamPronter.prontBeam(ps, buffers, skel, EnumWaveType.SPIRAL, EnumBeamType.SOLID, colorOuter, colorInner, 0, 1, 0F, segments, girth);

            if (emitter.effect == 1) {
                BeamPronter.prontBeam(ps, buffers, skel, EnumWaveType.RANDOM, EnumBeamType.SOLID, colorOuter, colorInner, (int) time / 2, (int) Math.max(range / girth / 2, 1), girth * 2, 4, girth * 0.1F);
                BeamPronter.prontBeam(ps, buffers, skel, EnumWaveType.RANDOM, EnumBeamType.SOLID, colorOuter, colorInner, (int) time / 2 + 15, (int) Math.max(range / girth / 4, 1), girth * 2, 4, girth * 0.1F);
            }
            if (emitter.effect == 2) {
                int s = (int) (time + f) * -10 % 360;
                BeamPronter.prontBeam(ps, buffers, skel, EnumWaveType.SPIRAL, EnumBeamType.SOLID, colorOuter, colorInner, s, (int) Math.max(range / girth / 2, 1), girth * 2, 4, girth * 0.1F);
                BeamPronter.prontBeam(ps, buffers, skel, EnumWaveType.SPIRAL, EnumBeamType.SOLID, colorOuter, colorInner, s + 180, (int) Math.max(range / girth / 2, 1), girth * 2, 4, girth * 0.1F);
            }
            if (emitter.effect == 3) {
                int s = (int) (time + f) * -10 % 360;
                BeamPronter.prontBeam(ps, buffers, skel, EnumWaveType.SPIRAL, EnumBeamType.SOLID, colorOuter, colorInner, s, (int) Math.max(range / girth / 2, 1), girth * 2, 4, girth * 0.1F);
                BeamPronter.prontBeam(ps, buffers, skel, EnumWaveType.SPIRAL, EnumBeamType.SOLID, colorOuter, colorInner, s + 120, (int) Math.max(range / girth / 2, 1), girth * 2, 4, girth * 0.1F);
                BeamPronter.prontBeam(ps, buffers, skel, EnumWaveType.SPIRAL, EnumBeamType.SOLID, colorOuter, colorInner, s + 240, (int) Math.max(range / girth / 2, 1), girth * 2, 4, girth * 0.1F);
            }
        }

        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(EmitterBlockEntity tile) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
