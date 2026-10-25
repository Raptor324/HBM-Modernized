package com.hbm_m.client.render.implementations;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.block.machines.MachinePyroOvenBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineElectrolyserBlockEntity;
import com.hbm_m.blockentity.machines.MachineMixerBlockEntity;
import com.hbm_m.blockentity.machines.MachinePUREXBlockEntity;
import com.hbm_m.blockentity.machines.MachinePyroOvenBlockEntity;
import com.hbm_m.blockentity.machines.MachineRadiolysisBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderHook;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Modern Nucleus machine renderers for chemical processing installations:
 * - Electrolyser: Industrial electrolysis vat
 * - Radiolysis Chamber: Radiation chemical decomposition vessel
 * - Chemical Mixer: Agitated mixing reactor with continuous impeller and dynamic fluid bath hook
 * - PUREX Extractor: Nuclear reprocessing facility with high-RPM fan and reciprocating pulse pump
 * - Pyro Oven: High-temperature pyrochemical reduction oven with slider carriage and cooling fan
 */
public final class MachineChemistryRenderer {

    public static void register() {
        registerElectrolyser();
        registerRadiolysis();
        registerMixer();
        registerPUREX();
        registerPyroOven();
    }

    private static void registerElectrolyser() {
        MachineRenderers.machine("electrolyser", ModBlockEntities.ELECTROLYSER_BE.get(),
                MachineElectrolyserBlockEntity.class)
            .part("Body")
            .blockTransform(MachineChemistryRenderer::applyElectrolyserTransform)
            .register();
    }

    private static void registerRadiolysis() {
        MachineRenderers.machine("radiolysis", ModBlockEntities.RADIOLYSIS_BE.get(),
                MachineRadiolysisBlockEntity.class)
            .part("Body")
            .blockTransform(MachineChemistryRenderer::applyRadiolysisTransform)
            .register();
    }

    private static void registerMixer() {
        MachineRenderers.machine("mixer", ModBlockEntities.MIXER_BE.get(),
                MachineMixerBlockEntity.class)
            .part("Main")
            .rotateY("Mixer", 0f, 0f, (be, pt) -> -(Mth.lerp(pt, be.prevRotation, be.rotation)))
            .blockTransform((be, animator) -> animator.translate(0.5, 0.0, 0.5))
            .hook(new MachineRenderHook<MachineMixerBlockEntity>() {
                @Override
                public void render(MachineMixerBlockEntity mixer, float partialTick, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    renderMixerFluid(mixer, ps, buf);
                }

                @Override
                public boolean mayDrawContent(MachineMixerBlockEntity mixer) {
                    for (FluidTank tank : mixer.tanks) {
                        if (FluidTank.isFluidTypeExplicitlySet(tank.getTankType()) && tank.getFill() > 0) {
                            return true;
                        }
                    }
                    return false;
                }
            })
            .itemParts("Main", "Mixer")
            .register();
    }

    private static void registerPUREX() {
        MachineRenderers.machine("purex", ModBlockEntities.PUREX_BE.get(),
                MachinePUREXBlockEntity.class)
            .part("Base")
            .conditionalPart("Frame", be -> be.frame)
            .rotatingPart("Fan", 0f, 0f, 1f, 1.5f, 1.25f, 0f, 0f, 0f, 0f,
                    (be, pt) -> Mth.lerp(pt, be.prevAnim, be.anim) * 45.0)
            .translatingPart("Pump", new Vector3f(1f, 0f, 0f), (be, pt) -> {
                float anim = Mth.lerp(pt, be.prevAnim, be.anim);
                return (float) (sps(anim * 0.25) * 0.5);
            })
            .blockTransform(MachineChemistryRenderer::applyPurexTransform)
            .itemParts("Base", "Fan", "Pump")
            .register();
    }

    private static void registerPyroOven() {
        MachineRenderers.machine("pyrooven", ModBlockEntities.PYROOVEN_BE.get(),
                MachinePyroOvenBlockEntity.class)
            .part("Oven")
            .translatingPart("Slider", new Vector3f(1f, 0f, 0f), (be, pt) -> {
                float anim = Mth.lerp(pt, be.prevAnim, be.anim);
                return (float) (sps(anim * 0.125) * 0.5 - 0.5);
            })
            .rotatingPart("Fan", 0f, 1f, 0f, 1.5f, 0f, 1.5f, 0f, 0f, 0f,
                    (be, pt) -> (Mth.lerp(pt, be.prevAnim, be.anim) * 45.0) % 360.0)
            .blockTransform(MachineChemistryRenderer::applyPyroOvenTransform)
            .itemParts("Oven", "Fan")
            .register();
    }

    private static void applyElectrolyserTransform(MachineElectrolyserBlockEntity te, LegacyAnimator animator) {
        animator.translate(0.5, 0.0, 0.5);
        if (te.getBlockState().hasProperty(DummyableMachineBlock.FACING)) {
            switch (te.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                case WEST -> animator.rotate(90, 0, 1, 0);
                case SOUTH -> animator.rotate(180, 0, 1, 0);
                case EAST -> animator.rotate(270, 0, 1, 0);
                default -> {}
            }
        }
        animator.rotate(180, 0, 1, 0);
    }

    private static void applyRadiolysisTransform(MachineRadiolysisBlockEntity te, LegacyAnimator animator) {
        animator.translate(0.5, 0.0, 0.5);
        if (te.getBlockState().hasProperty(DummyableMachineBlock.FACING)) {
            switch (te.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                case NORTH -> animator.rotate(180, 0, 1, 0);
                case WEST -> animator.rotate(90, 0, 1, 0);
                case EAST -> animator.rotate(270, 0, 1, 0);
                default -> {}
            }
        }
    }

    private static void applyPurexTransform(MachinePUREXBlockEntity purex, LegacyAnimator animator) {
        animator.translate(0.5, 0.0, 0.5);
        animator.rotate(90, 0, 1, 0);
        if (purex.getBlockState().hasProperty(DummyableMachineBlock.FACING)) {
            switch (purex.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                case WEST -> animator.rotate(90, 0, 1, 0);
                case SOUTH -> animator.rotate(180, 0, 1, 0);
                case EAST -> animator.rotate(270, 0, 1, 0);
                default -> {}
            }
        }
    }

    private static void applyPyroOvenTransform(MachinePyroOvenBlockEntity pyro, LegacyAnimator animator) {
        animator.translate(0.5, 0.0, 0.5);
        if (pyro.getBlockState().hasProperty(MachinePyroOvenBlock.FACING)) {
            switch (pyro.getBlockState().getValue(MachinePyroOvenBlock.FACING)) {
                case NORTH -> animator.rotate(180, 0, 1, 0);
                case WEST -> animator.rotate(270, 0, 1, 0);
                case EAST -> animator.rotate(90, 0, 1, 0);
                default -> {}
            }
        }
    }

    private static void renderMixerFluid(MachineMixerBlockEntity mixer, PoseStack ps, MultiBufferSource buf) {
        int totalFill = 0;
        int totalMax = 0;
        for (FluidTank tank : mixer.tanks) {
            if (FluidTank.isFluidTypeExplicitlySet(tank.getTankType())) {
                totalFill += tank.getFill();
                totalMax += tank.getMaxFill();
            }
        }
        if (totalFill <= 0 || totalMax <= 0) return;

        int color = HbmFluidRegistry.getTintColor(mixer.tanks[2].getTankType());
        float r = ((color >> 16) & 255) / 255.0F;
        float g = ((color >> 8) & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        float a = 0.75F;

        ps.pushPose();
        float fillRatio = (float) ((double) totalFill / (double) totalMax * 0.99);
        float fluidHeight = 0.2F + fillRatio * 0.8F;

        VertexConsumer vc = buf.getBuffer(ClientRenderHandler.CustomRenderTypes.TRANSLUCENT_COLOR_NOCULL);
        Matrix4f m = ps.last().pose();
        float s = 0.4F;

        com.hbm_m.platform.RenderHooks.vertexColor(vc, m, -s, fluidHeight, -s, r, g, b, a);
        com.hbm_m.platform.RenderHooks.vertexColor(vc, m, -s, fluidHeight,  s, r, g, b, a);
        com.hbm_m.platform.RenderHooks.vertexColor(vc, m,  s, fluidHeight,  s, r, g, b, a);
        com.hbm_m.platform.RenderHooks.vertexColor(vc, m,  s, fluidHeight, -s, r, g, b, a);

        ps.popPose();
    }

    private static double sps(double x) {
        return Math.sin(Math.PI / 2.0 * Math.cos(x));
    }

    private MachineChemistryRenderer() {}
}
