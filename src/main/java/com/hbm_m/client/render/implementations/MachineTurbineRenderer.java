package com.hbm_m.client.render.implementations;

import org.joml.Vector3f;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineIndustrialTurbineBlockEntity;
import com.hbm_m.blockentity.machines.MachineLargeTurbineBlockEntity;
import com.hbm_m.blockentity.machines.MachineTurbineGasBlockEntity;
import com.hbm_m.client.render.machine.MachineRenderers;

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
        MachineRenderers.machine("turbine_gas", ModBlockEntities.TURBINEGAS_BE.get(),
                MachineTurbineGasBlockEntity.class)
            .part("turbineShit_withCOVER")
            .facing(ObjBerHelper::facing)
            .viewDistance(256)
            .itemParts("turbineShit_withCOVER")
            .register();
    }

    private static void registerIndustrialTurbine() {
        MachineRenderers.machine("industrial_turbine", ModBlockEntities.INDUSTRIAL_TURBINE_BE.get(),
                MachineIndustrialTurbineBlockEntity.class)
            .part("Turbine")
            .part("Gauge")
            .rotatingPart("Flywheel", new Vector3f(0.0F, 0.0F, -1.0F), new Vector3f(0.0F, 1.5F, 0.0F),
                    (be, pt) -> Mth.lerp(pt, be.lastRotor, be.rotor))
            .facing(ObjBerHelper::facing)
            .viewDistance(256)
            .itemParts("Turbine", "Flywheel")
            .register();
    }

    private static void registerLargeTurbine() {
        MachineRenderers.machine("large_turbine", ModBlockEntities.LARGE_TURBINE_BE.get(),
                MachineLargeTurbineBlockEntity.class)
            .blockTransform((be, anim) -> anim.translate(0.5, 0.0, -0.5))
            .part("Body")
            .rotatingPart("Blades", new Vector3f(0.0F, 0.0F, 1.0F), new Vector3f(0.0F, 1.0F, 0.0F),
                    (be, pt) -> Mth.lerp(pt, be.lastRotor, be.rotor))
            .facing(ObjBerHelper::facing)
            .viewDistance(256)
            .itemParts("Body", "Blades")
            .register();
    }

    private MachineTurbineRenderer() {}
}
