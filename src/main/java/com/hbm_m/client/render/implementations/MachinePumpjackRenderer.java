package com.hbm_m.client.render.implementations;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.hbm_m.block.machines.MachinePumpjackBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachinePumpjackBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderHook;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.platform.RenderHooks;
import com.hbm_m.util.Vec3NT;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

/**
 * Modern Nucleus implementation of the walking beam Pumpjack renderer.
 * <p>
 * Driven by {@link MachineRenderers#machine}:
 * <ul>
 *   <li>Base: static foundation;</li>
 *   <li>Rotor: GPU rotation on X axis around (0, 1.5, -5.5) by {@code rot - 90};</li>
 *   <li>Head: GPU rocking oscillation on X axis around (0, 3.5, -3.5) by {@code sin(rot) * 0.25} rad;</li>
 *   <li>Carriage: GPU linear translation along Y by {@code -sin(rot)};</li>
 *   <li>Connecting rods, cables, and polish rod: immediate render hook with fast assert.</li>
 * </ul>
 */
public final class MachinePumpjackRenderer {

    public static void register() {
        MachineRenderers.machine("pumpjack", ModBlockEntities.PUMPJACK_BE.get(),
                MachinePumpjackBlockEntity.class)
            .part("Base")
            .rotatingPart("Rotor", new Vector3f(1.0F, 0.0F, 0.0F), new Vector3f(0.0F, 1.5F, -5.5F),
                    (be, pt) -> Mth.lerp(pt, be.prevRot, be.rot) - 90.0)
            .rotatingPart("Head", new Vector3f(1.0F, 0.0F, 0.0F), new Vector3f(0.0F, 3.5F, -3.5F),
                    (be, pt) -> Math.toDegrees(Math.sin(Math.toRadians(Mth.lerp(pt, be.prevRot, be.rot)))) * 0.25)
            .translatingPart("Carriage", new Vector3f(0.0F, 1.0F, 0.0F),
                    (be, pt) -> -Math.sin(Math.toRadians(Mth.lerp(pt, be.prevRot, be.rot))))
            .facing(be -> be.getBlockState().hasProperty(MachinePumpjackBlock.FACING)
                    ? be.getBlockState().getValue(MachinePumpjackBlock.FACING) : Direction.NORTH)
            .viewDistance(256)
            .itemParts("Base", "Rotor", "Head", "Carriage")
            .hook(new MachineRenderHook<MachinePumpjackBlockEntity>() {
                @Override
                public void render(MachinePumpjackBlockEntity pj, float pt, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    renderCablesAndRods(pj, pt, ps, buf);
                }

                @Override
                public boolean mayDrawContent(MachinePumpjackBlockEntity be) {
                    return true;
                }
            })
            .register();
    }

    private static void renderCablesAndRods(MachinePumpjackBlockEntity pj, float f, PoseStack ps, MultiBufferSource buf) {
        float rotation = pj.prevRot + (pj.rot - pj.prevRot) * f;

        Vec3NT backPos = Vec3NT.createVectorHelper(0, 0, -2);
        backPos.rotateAroundX(-(float) Math.sin(Math.toRadians(rotation)) * 0.25F);
        Vec3NT rot = Vec3NT.createVectorHelper(0, 0.5, 0);
        rot.rotateAroundX(-(float) Math.toRadians(rotation - 90));

        VertexConsumer lines = buf.getBuffer(ClientRenderHandler.CustomRenderTypes.SOLID_COLOR_NOCULL);
        Matrix4f m = ps.last().pose();

        int c = 128; // 0.5F
        for (int i = -1; i <= 1; i += 2) {
            quad(lines, m, c, c, c,
                    0.53125 * i, 1.5 + rot.yCoord, -5.5 + rot.zCoord - 0.0625D,
                    0.53125 * i, 1.5 + rot.yCoord, -5.5 + rot.zCoord + 0.0625D,
                    0.53125 * i, 3.5 + backPos.yCoord, -3.5 + backPos.zCoord + 0.0625D,
                    0.53125 * i, 3.5 + backPos.yCoord, -3.5 + backPos.zCoord - 0.0625D);
        }

        c = 51; // 0.2F
        double pd = 0.03125D;
        double width = 0.25D;
        double height = -Math.sin(Math.toRadians(rotation));

        for (int i = -1; i <= 1; i += 2) {
            float pRot = -(float) (Math.sin(Math.toRadians(rotation)) * 0.25);
            Vec3NT frontPos = Vec3NT.createVectorHelper(0, 0, 1);
            frontPos.rotateAroundX(pRot);
            double dist = 0.03125D;
            Vec3NT frontRad = Vec3NT.createVectorHelper(0, 0, 2.5 + dist);
            double cutlet = 360D / 32D;
            frontRad.rotateAroundX(pRot);
            frontRad.rotateAroundX(-(float) Math.toRadians(cutlet * -3));

            for (int j = 0; j < 4; j++) {
                double sumY = frontPos.yCoord + frontRad.yCoord;
                double sumZ = frontPos.zCoord + frontRad.zCoord;
                if (frontRad.yCoord < 0) sumZ = 3.5 + dist * 0.5;
                double ax = (width - pd) * i, ay = 3.5 + sumY, az = -3.5 + sumZ;
                double bx = (width + pd) * i;
                frontRad.rotateAroundX(-(float) Math.toRadians(cutlet));
                sumY = frontPos.yCoord + frontRad.yCoord;
                sumZ = frontPos.zCoord + frontRad.zCoord;
                if (frontRad.yCoord < 0) sumZ = 3.5 + dist * 0.5;
                quad(lines, m, c, c, c,
                        ax, ay, az,
                        bx, ay, az,
                        (width + pd) * i, 3.5 + sumY, -3.5 + sumZ,
                        (width - pd) * i, 3.5 + sumY, -3.5 + sumZ);
            }

            double sumY = frontPos.yCoord + frontRad.yCoord;
            double sumZ = frontPos.zCoord + frontRad.zCoord;
            if (frontRad.yCoord < 0) sumZ = 3.5 + dist * 0.5;
            quad(lines, m, c, c, c,
                    (width + pd) * i, 3.5 + sumY, -3.5 + sumZ,
                    (width - pd) * i, 3.5 + sumY, -3.5 + sumZ,
                    (width - pd) * i, 2 + height, 0,
                    (width + pd) * i, 2 + height, 0);
        }

        double p = 0.03125D;
        quad(lines, m, c, c, c,
                p, height + 1.5, p,
                -p, height + 1.5, -p,
                -p, 0.75, -p,
                p, 0.75, p);
        quad(lines, m, c, c, c,
                -p, height + 1.5, p,
                p, height + 1.5, -p,
                p, 0.75, -p,
                -p, 0.75, p);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, int r, int g, int b,
                             double x1, double y1, double z1, double x2, double y2, double z2,
                             double x3, double y3, double z3, double x4, double y4, double z4) {
        RenderHooks.vertexColor(vc, m, (float) x1, (float) y1, (float) z1, r, g, b, 255);
        RenderHooks.vertexColor(vc, m, (float) x2, (float) y2, (float) z2, r, g, b, 255);
        RenderHooks.vertexColor(vc, m, (float) x3, (float) y3, (float) z3, r, g, b, 255);
        RenderHooks.vertexColor(vc, m, (float) x1, (float) y1, (float) z1, r, g, b, 255);
        RenderHooks.vertexColor(vc, m, (float) x3, (float) y3, (float) z3, r, g, b, 255);
        RenderHooks.vertexColor(vc, m, (float) x4, (float) y4, (float) z4, r, g, b, 255);
    }

    private MachinePumpjackRenderer() {}
}
