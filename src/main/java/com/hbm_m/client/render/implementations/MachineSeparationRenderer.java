package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.block.machines.MachineChimneyBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineChimneyBlockEntity;
import com.hbm_m.blockentity.machines.MachineCompressorBlockEntity;
import com.hbm_m.blockentity.machines.MachineCompressorCompactBlockEntity;
import com.hbm_m.blockentity.machines.MachineMicrowaveBlockEntity;
import com.hbm_m.blockentity.machines.MachineSilexBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderHook;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Modern Nucleus machine renderers for isotope separation & gas handling installations:
 * - SILEX: Laser isotope separator with central excitation laser beam hook
 * - Microwave Smelter: High-frequency smelting cavity with rotating motorized turntable plate
 * - Compressor (Large & Compact): Reciprocating gas compression cylinder with high-RPM cooling fans
 * - Chimney: Industrial emissions stack with facing alignment
 */
public final class MachineSeparationRenderer {

    public static void register() {
        registerSILEX();
        registerMicrowave();
        registerCompressor();
        registerCompressorCompact();
        registerChimney();
    }

    private static void registerSILEX() {
        MachineRenderers.machine("silex", ModBlockEntities.SILEX_BE.get(),
                MachineSilexBlockEntity.class)
            .part("Cube_Cube.001")
            .blockTransform(MachineSeparationRenderer::applySilexTransform)
            .hook(new MachineRenderHook<MachineSilexBlockEntity>() {
                @Override
                public void render(MachineSilexBlockEntity silex, float partialTick, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    if (silex.progress <= 0) return;

                    ps.pushPose();
                    ps.translate(0.0, 1.5, 0.0);
                    VertexConsumer vc = buf.getBuffer(ClientRenderHandler.CustomRenderTypes.DETONATOR_LASER_GLOW);
                    Matrix4f m = ps.last().pose();
                    float w = 0.05F;
                    float len = 2.0F;

                    // Central green/cyan laser excitation beam
                    com.hbm_m.platform.RenderHooks.vertexColor(vc, m, -w, -len, 0, 0.1F, 1.0F, 0.7F, 0.9F);
                    com.hbm_m.platform.RenderHooks.vertexColor(vc, m,  w, -len, 0, 0.1F, 1.0F, 0.7F, 0.9F);
                    com.hbm_m.platform.RenderHooks.vertexColor(vc, m,  w,  len, 0, 0.1F, 1.0F, 0.7F, 0.9F);
                    com.hbm_m.platform.RenderHooks.vertexColor(vc, m, -w,  len, 0, 0.1F, 1.0F, 0.7F, 0.9F);

                    com.hbm_m.platform.RenderHooks.vertexColor(vc, m, 0, -len, -w, 0.1F, 1.0F, 0.7F, 0.9F);
                    com.hbm_m.platform.RenderHooks.vertexColor(vc, m, 0, -len,  w, 0.1F, 1.0F, 0.7F, 0.9F);
                    com.hbm_m.platform.RenderHooks.vertexColor(vc, m, 0,  len,  w, 0.1F, 1.0F, 0.7F, 0.9F);
                    com.hbm_m.platform.RenderHooks.vertexColor(vc, m, 0,  len, -w, 0.1F, 1.0F, 0.7F, 0.9F);

                    ps.popPose();
                }

                @Override
                public boolean mayDrawContent(MachineSilexBlockEntity silex) {
                    return silex.progress > 0;
                }
            })
            .register();
    }

    private static void registerMicrowave() {
        MachineRenderers.machine("microwave", ModBlockEntities.MICROWAVE_BE.get(),
                MachineMicrowaveBlockEntity.class)
            .part("mainbody_Cube.001")
            .part("window_Cube.002")
            .rotatingPart("plate_Cylinder", 0f, 1f, 0f, 0.575f, 0f, -0.45f, 0f, 0f, 0f,
                    (mic, pt) -> mic.time > 0 ? (System.currentTimeMillis() * mic.speed / 10.0) % 360.0 : 0.0)
            .blockTransform(MachineSeparationRenderer::applyMicrowaveTransform)
            .itemParts("mainbody_Cube.001", "window_Cube.002", "plate_Cylinder")
            .register();
    }

    private static void registerCompressor() {
        MachineRenderers.machine("compressor", ModBlockEntities.COMPRESSOR_BE.get(),
                MachineCompressorBlockEntity.class)
            .part("Compressor")
            .translatingPart("Pump", new Vector3f(0f, 1f, 0f), (comp, pt) -> {
                float lift = Mth.lerp(pt, comp.prevPiston, comp.piston);
                return lift * 3.0F - 3.0F;
            })
            .rotatingPart("Fan", 1f, 0f, 0f, 0f, 1.5f, 0f, 0f, 0f, 0f,
                    (comp, pt) -> Mth.lerp(pt, comp.prevFanSpin, comp.fanSpin))
            .blockTransform(MachineSeparationRenderer::applyCompressorTransform)
            .itemParts("Compressor", "Pump", "Fan")
            .register();
    }

    private static void registerCompressorCompact() {
        MachineRenderers.machine("compressor_compact", ModBlockEntities.COMPRESSOR_COMPACT_BE.get(),
                MachineCompressorCompactBlockEntity.class)
            .part("Condenser")
            .rotatingPart("Fan1", 1f, 0f, 0f, 0f, 1.5f, 0f, 0f, 0f, 0f,
                    (comp, pt) -> Mth.lerp(pt, comp.prevFanSpin, comp.fanSpin))
            .rotatingPart("Fan2", -1f, 0f, 0f, 0f, 1.5f, 0f, 0f, 0f, 0f,
                    (comp, pt) -> Mth.lerp(pt, comp.prevFanSpin, comp.fanSpin))
            .blockTransform(MachineSeparationRenderer::applyCompressorCompactTransform)
            .itemParts("Condenser", "Fan1", "Fan2")
            .register();
    }

    private static void registerChimney() {
        MachineRenderers.machine("chimney", ModBlockEntities.CHIMNEY_BE.get(),
                MachineChimneyBlockEntity.class)
            .part("Plane")
            .blockTransform((be, animator) -> {
                animator.translate(0.5, 0.0, 0.5);
                animator.rotate(180, 0, 1, 0);
            })
            .register();
    }

    private static void applySilexTransform(MachineSilexBlockEntity te, LegacyAnimator animator) {
        animator.translate(0.5, 0.0, 0.5);
        if (te.getBlockState().hasProperty(DummyableMachineBlock.FACING)) {
            switch (te.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                case WEST -> animator.rotate(180, 0, 1, 0);
                case SOUTH -> animator.rotate(270, 0, 1, 0);
                case NORTH -> animator.rotate(90, 0, 1, 0);
                default -> {}
            }
        }
    }

    private static void applyMicrowaveTransform(MachineMicrowaveBlockEntity mic, LegacyAnimator animator) {
        animator.translate(0.5, -0.785, 0.5);
        switch (ObjBerHelper.meta(mic)) {
            case 2 -> animator.rotate(0, 0, 1, 0);
            case 4 -> animator.rotate(90, 0, 1, 0);
            case 3 -> animator.rotate(180, 0, 1, 0);
            case 5 -> animator.rotate(270, 0, 1, 0);
            default -> {}
        }
        animator.translate(-0.5, 0.0, 0.65);
    }

    private static void applyCompressorTransform(MachineCompressorBlockEntity compressor, LegacyAnimator animator) {
        animator.translate(0.5, 0.0, 0.5);
        if (compressor.getBlockState().hasProperty(DummyableMachineBlock.FACING)) {
            Direction dir = compressor.getBlockState().getValue(DummyableMachineBlock.FACING);
            switch (dir) {
                case SOUTH -> animator.rotate(270, 0, 1, 0);
                case NORTH -> animator.rotate(90, 0, 1, 0);
                case WEST -> animator.rotate(180, 0, 1, 0);
                default -> {}
            }
        }
    }

    private static void applyCompressorCompactTransform(MachineCompressorCompactBlockEntity compressor, LegacyAnimator animator) {
        animator.translate(0.5, 0.0, 0.5);
        if (compressor.getBlockState().hasProperty(DummyableMachineBlock.FACING)) {
            Direction dir = compressor.getBlockState().getValue(DummyableMachineBlock.FACING);
            switch (dir) {
                case SOUTH -> animator.rotate(270, 0, 1, 0);
                case NORTH -> animator.rotate(90, 0, 1, 0);
                case WEST -> animator.rotate(180, 0, 1, 0);
                default -> {}
            }
        }
    }

    private MachineSeparationRenderer() {}
}
