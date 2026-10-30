package com.hbm_m.client.render.implementations;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineBreederBlockEntity;
import com.hbm_m.blockentity.machines.MachineReactorResearchBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderHook;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.util.RenderSparks;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;

/**
 * Nucleus renderers for nuclear reactors:
 * 1. Breeder Reactor: static body with chamber transmutation sparks hook.
 * 2. Research Reactor: static base, vertically translating control rods via GPU
 *    parametric translation (translatingPart), and Cherenkov pool glow hook.
 */
public final class MachineReactorResearchRenderer {

    public static void register() {
        // ── Breeder Reactor ──────────────────────────────────────────────────
        // breeder.obj слит в одну группу "Cube_Cube.001"
        MachineRenderers.machine("breeder",
                ModBlockEntities.BREEDER_BE.get(),
                MachineBreederBlockEntity.class)
            .part("Cube_Cube.001")
            .hook(new MachineRenderHook<MachineBreederBlockEntity>() {
                @Override
                public void render(MachineBreederBlockEntity be, float pt, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    if (be.getProgress() <= 0) return;

                    long time = System.currentTimeMillis();
                    for (int i = 0; i < 3; i++) {
                        ps.pushPose();
                        ps.mulPose(Axis.YP.rotationDegrees((float) (Math.PI * i)));
                        RenderSparks.renderSpark(ps, buf, (int) ((time % 10000) / 100 + i),
                                0, 1.5625, 0, 0.15F, 3, 4, 0x00ff00, 0xffffff);
                        ps.popPose();
                    }
                }

                @Override
                public boolean mayDrawContent(MachineBreederBlockEntity be) {
                    return be.getProgress() > 0;
                }
            })
            .itemParts("Cube_Cube.001")
            .register();

        // ── Research Reactor ─────────────────────────────────────────────────
        MachineRenderers.machine("reactor_research",
                ModBlockEntities.REACTOR_RESEARCH_BE.get(),
                MachineReactorResearchBlockEntity.class)
            .part("Base")
            .translatingPart("Rods", new Vector3f(0f, 1f, 0f),
                    (be, pt) -> Mth.lerp(pt, (float) be.lastLevel, (float) be.level))
            .hook(new MachineRenderHook<MachineReactorResearchBlockEntity>() {
                @Override
                public void render(MachineReactorResearchBlockEntity be, float pt, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    if (be.totalFlux <= 10 || !be.isSubmerged()) return;

                    VertexConsumer vc = buf.getBuffer(ClientRenderHandler.CustomRenderTypes.RBMK_CHERENKOV);
                    Matrix4f m = ps.last().pose();

                    for (double dd = 0.285; dd < 0.7; dd += 0.025) {
                        float d = (float) dd;
                        float a = 0.025F + (float) (Math.random() * 0.015F) + (0.125F * be.totalFlux / 1000F);
                        float top = 1.375F;
                        float bottom = 1.375F;

                        quad(vc, m, a, d, bottom - d, -d, d, top + d, -d, d, top + d, d, d, bottom - d, d);
                        quad(vc, m, a, -d, bottom - d, -d, -d, top + d, -d, -d, top + d, d, -d, bottom - d, d);
                        quad(vc, m, a, -d, bottom - d, d, -d, top + d, d, d, top + d, d, d, bottom - d, d);
                        quad(vc, m, a, -d, bottom - d, -d, -d, top + d, -d, d, top + d, -d, d, bottom - d, -d);
                        quad(vc, m, a, -d, top + d, -d, -d, top + d, d, d, top + d, d, d, top + d, -d);
                        quad(vc, m, a, -d, bottom - d, -d, -d, bottom - d, d, d, bottom - d, d, d, bottom - d, -d);
                    }
                }

                @Override
                public boolean mayDrawContent(MachineReactorResearchBlockEntity be) {
                    return be.totalFlux > 10 && be.isSubmerged();
                }
            })
            .itemParts("Base", "Rods")
            .register();
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float a,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3) {
        RenderHooks.vertexColor(vc, m, x0, y0, z0, 0.4F, 0.9F, 1.0F, a);
        RenderHooks.vertexColor(vc, m, x1, y1, z1, 0.4F, 0.9F, 1.0F, a);
        RenderHooks.vertexColor(vc, m, x2, y2, z2, 0.4F, 0.9F, 1.0F, a);
        RenderHooks.vertexColor(vc, m, x3, y3, z3, 0.4F, 0.9F, 1.0F, a);
    }

    private MachineReactorResearchRenderer() {}
}
