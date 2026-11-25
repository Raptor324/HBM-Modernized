package com.hbm_m.client.render.implementations;

import org.joml.Vector3f;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineIndustrialTurbineBlockEntity;
import com.hbm_m.blockentity.machines.MachineLargeTurbineBlockEntity;
import com.hbm_m.blockentity.machines.MachineTurbineGasBlockEntity;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.machine.MachineRenderers;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

/**
 * Modern Nucleus renderer implementation for high-speed steam and gas turbines:
 * <ul>
 *   <li>Gas Turbine: High-RPM internal rotor spindle;</li>
 *   <li>Industrial Turbine: Heavy industrial multi-stage turbine with dynamic flywheel rotation;</li>
 *   <li>Large Steam Turbine: Heavy power turbine with spinning multi-blade rotor assembly.</li>
 * </ul>
 */
public final class MachineTurbineRenderer {

    public static void register() {
        registerTurbineGas();
        registerIndustrialTurbine();
        registerLargeTurbine();
    }

    private static void registerTurbineGas() {
        MachineRenderers.machine("turbinegas", ModBlockEntities.TURBINEGAS_BE.get(),
                MachineTurbineGasBlockEntity.class)
            .blockTransform(ObjBerHelper::setupBakedBlockTransform)
            .part("turbineShit_withCOVER")
            .facing(ObjBerHelper::facing)
            .viewDistance(256)
            .itemParts("turbineShit_withCOVER")
            .register();
    }

    private static void registerIndustrialTurbine() {
        MachineRenderers.machine("industrial_turbine", ModBlockEntities.INDUSTRIAL_TURBINE_BE.get(),
                MachineIndustrialTurbineBlockEntity.class)
            .blockTransform(MachineTurbineRenderer::applyIndustrialTurbineTransform)
            .part("Turbine")
            .part("Gauge", (be, pt, gt, ps) -> {
                float angle = (float) (135.0 - (ChungusRenderer.steamId(be.getSteamTank().getTankType()) - 2) * 90.0);
                ps.translate(0.5F, 1.5F, 0.5F);
                ps.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(angle));
                ps.translate(-0.5F, -1.5F, -0.5F);
                return true;
            })
            .rotatingPart("Flywheel", new Vector3f(0.0F, 0.0F, -1.0F), new Vector3f(0.5F, 1.5F, 0.5F),
                    (be, pt) -> Mth.lerp(pt, be.lastRotor, be.rotor))
            .animationEpoch(be -> ((long) Float.floatToRawIntBits(be.lastRotor) * 31L
                    + Float.floatToRawIntBits(be.rotor)) * 31L
                    + ChungusRenderer.steamId(be.getSteamTank().getTankType()))
            .facing(ObjBerHelper::facing)
            .viewDistance(256)
            .itemParts("Turbine", "Flywheel", "Gauge")
            .register();
    }

    private static void applyIndustrialTurbineTransform(MachineIndustrialTurbineBlockEntity turbine, LegacyAnimator animator) {
        animator.translate(0.5, 0.0, 0.5);
        Direction facing = ObjBerHelper.facing(turbine);
        switch (facing) {
            case NORTH -> animator.rotate(180, 0, 1, 0);
            case WEST  -> animator.rotate(270, 0, 1, 0);
            case SOUTH -> animator.rotate(0, 0, 1, 0);
            case EAST  -> animator.rotate(90, 0, 1, 0);
            default    -> {}
        }
        animator.translate(-0.5, 0.0, -0.5);
    }

    private static void registerLargeTurbine() {
        MachineRenderers.machine("machine_large_turbine", ModBlockEntities.LARGE_TURBINE_BE.get(),
                MachineLargeTurbineBlockEntity.class)
            .blockTransform(MachineTurbineRenderer::applyLargeTurbineTransform)
            .part("Body")
            .rotatingPart("Blades", new Vector3f(0.0F, 0.0F, 1.0F), new Vector3f(0.5F, 1.0F, 0.5F),
                    (be, pt) -> Mth.lerp(pt, be.lastRotor, be.rotor))
            .facing(ObjBerHelper::facing)
            .viewDistance(256)
            .itemParts("Body", "Blades")
            .register();
    }

    private static void applyLargeTurbineTransform(MachineLargeTurbineBlockEntity turbine, LegacyAnimator animator) {
        animator.translate(0.5, 0.0, 0.5);
        Direction facing = ObjBerHelper.facing(turbine);
        switch (facing) {
            case NORTH -> animator.rotate(180, 0, 1, 0);
            case WEST  -> animator.rotate(270, 0, 1, 0);
            case SOUTH -> animator.rotate(0, 0, 1, 0);
            case EAST  -> animator.rotate(90, 0, 1, 0);
            default    -> {}
        }
        animator.translate(-0.5, 0.0, -1.5);
    }

    private MachineTurbineRenderer() {}
}
