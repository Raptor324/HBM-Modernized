package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineArcWelderBlockEntity;
import com.hbm_m.blockentity.machines.MachineSolderingStationBlockEntity;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderHook;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/**
 * Modern Nucleus machine renderers for crafting workbenches:
 * Arc Welder and Soldering Station.
 * 
 * Features:
 * - Block transform matching legacy workbench orientation and offset
 * - Immediate-mode render hook displaying active recipe craft item flat on table
 * - Welding arc spark / glow hook during active crafting operations
 */
public final class MachineWorkbenchRenderer {

    public static void register() {
        registerArcWelder();
        registerSolderingStation();
    }

    private static void registerArcWelder() {
        MachineRenderers.machine("arc_welder", ModBlockEntities.ARC_WELDER_BE.get(),
                MachineArcWelderBlockEntity.class)
            .part("Plane")
            .blockTransform(MachineWorkbenchRenderer::applyArcWelderTransform)
            .hook(new MachineRenderHook<MachineArcWelderBlockEntity>() {
                @Override
                public void render(MachineArcWelderBlockEntity welder, float partialTick, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    ItemStack display = welder.display;
                    if (!display.isEmpty()) {
                        ps.pushPose();
                        ps.translate(0.0625D * 2.5D, 1.125D, 0.0D);
                        ps.mulPose(Axis.YP.rotationDegrees(90.0F));
                        ps.mulPose(Axis.XP.rotationDegrees(-90.0F));
                        ps.scale(1.5F, 1.5F, 1.5F);
                        ObjBerHelper.renderDecoItemInFrame(ps, buf, display, light, welder.getLevel());
                        ps.popPose();
                    }

                    if (welder.progress > 0) {
                        renderWeldingArc(ps, buf, welder);
                    }
                }

                @Override
                public boolean mayDrawContent(MachineArcWelderBlockEntity welder) {
                    return !welder.display.isEmpty() || welder.progress > 0;
                }
            })
            .register();
    }

    private static void registerSolderingStation() {
        MachineRenderers.machine("soldering_station", ModBlockEntities.SOLDERING_STATION_BE.get(),
                MachineSolderingStationBlockEntity.class)
            .part("Plane")
            .blockTransform(MachineWorkbenchRenderer::applySolderingStationTransform)
            .hook(new MachineRenderHook<MachineSolderingStationBlockEntity>() {
                @Override
                public void render(MachineSolderingStationBlockEntity solderer, float partialTick, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    ItemStack display = solderer.display;
                    if (!display.isEmpty()) {
                        ps.pushPose();
                        ps.translate(0.0625D * 2.5D, 1.125D, 0.0D);
                        ps.mulPose(Axis.YP.rotationDegrees(90.0F));
                        ps.mulPose(Axis.XP.rotationDegrees(-90.0F));
                        ps.scale(1.5F, 1.5F, 1.5F);
                        ObjBerHelper.renderDecoItemInFrame(ps, buf, display, light, solderer.getLevel());
                        ps.popPose();
                    }
                }

                @Override
                public boolean mayDrawContent(MachineSolderingStationBlockEntity solderer) {
                    return !solderer.display.isEmpty();
                }
            })
            .register();
    }

    private static void applyArcWelderTransform(MachineArcWelderBlockEntity welder, LegacyAnimator animator) {
        animator.translate(0.5, 0.0, 0.5);
        switch (ObjBerHelper.meta(welder)) {
            case 2 -> animator.rotate(90, 0, 1, 0);
            case 4 -> animator.rotate(180, 0, 1, 0);
            case 3 -> animator.rotate(270, 0, 1, 0);
            case 5 -> animator.rotate(0, 0, 1, 0);
            default -> {}
        }
        animator.translate(-0.5, 0.0, 0.0);
    }

    private static void applySolderingStationTransform(MachineSolderingStationBlockEntity solderer, LegacyAnimator animator) {
        animator.translate(0.5, 0.0, 0.5);
        switch (ObjBerHelper.meta(solderer)) {
            case 2 -> animator.rotate(90, 0, 1, 0);
            case 4 -> animator.rotate(180, 0, 1, 0);
            case 3 -> animator.rotate(270, 0, 1, 0);
            case 5 -> animator.rotate(0, 0, 1, 0);
            default -> {}
        }
        animator.translate(-0.5, 0.0, 0.5);
    }

    private static void renderWeldingArc(PoseStack ps, MultiBufferSource buf, MachineArcWelderBlockEntity welder) {
        long time = welder.getLevel() != null ? welder.getLevel().getGameTime() : 0L;
        if ((time & 1L) == 0L) return; // Flickering arc flash

        ps.pushPose();
        ps.translate(0.15, 1.15, 0.0);
        Matrix4f m = ps.last().pose();
        VertexConsumer vc = buf.getBuffer(RenderType.lightning());
        float s = 0.05F;
        float r = 0.7F, g = 0.85F, b = 1.0F, a = 0.9F;

        // Small glowing cross quad at the welding tip
        com.hbm_m.platform.RenderHooks.vertexColor(vc, m, -s, 0, -s, r, g, b, a);
        com.hbm_m.platform.RenderHooks.vertexColor(vc, m,  s, 0, -s, r, g, b, a);
        com.hbm_m.platform.RenderHooks.vertexColor(vc, m,  s, 0,  s, r, g, b, a);
        com.hbm_m.platform.RenderHooks.vertexColor(vc, m, -s, 0,  s, r, g, b, a);

        com.hbm_m.platform.RenderHooks.vertexColor(vc, m, 0, -s, -s, r, g, b, a);
        com.hbm_m.platform.RenderHooks.vertexColor(vc, m, 0,  s, -s, r, g, b, a);
        com.hbm_m.platform.RenderHooks.vertexColor(vc, m, 0,  s,  s, r, g, b, a);
        com.hbm_m.platform.RenderHooks.vertexColor(vc, m, 0, -s,  s, r, g, b, a);

        ps.popPose();
    }

    private MachineWorkbenchRenderer() {}
}
