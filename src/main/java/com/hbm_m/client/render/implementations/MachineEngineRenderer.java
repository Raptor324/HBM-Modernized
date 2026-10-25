package com.hbm_m.client.render.implementations;

import org.joml.Vector3f;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineCombustionEngineBlockEntity;
import com.hbm_m.blockentity.machines.MachineDieselGeneratorBlockEntity;
import com.hbm_m.blockentity.machines.MachineSteamEngineBlockEntity;
import com.hbm_m.blockentity.machines.MachineStirlingBlockEntity;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.inventory.fluid.FluidContainerDefs;
import com.mojang.math.Axis;

import net.minecraft.util.Mth;

/**
 * Modern Nucleus renderer implementation for mechanical power generators and thermal engines:
 * <ul>
 *   <li>Stirling Engine: Thermal engine with rotating cog, small cog, and translating piston;</li>
 *   <li>Diesel Generator: Internal combustion generator with engine vibration;</li>
 *   <li>Combustion Engine: Engine block, tinted fuel canister, and hinged maintenance hatch;</li>
 *   <li>Steam Engine: Reciprocating steam engine with flywheel, driveshaft, transmission link, and piston.</li>
 * </ul>
 */
public final class MachineEngineRenderer {

    public static void register() {
        registerStirling();
        registerDieselGen();
        registerCombustion();
        registerSteamEngine();
    }

    private static void registerStirling() {
        MachineRenderers.machine("stirling", ModBlockEntities.STIRLING_BE.get(),
                MachineStirlingBlockEntity.class)
            .part("Base")
            .part("Cog", (be, pt, gt, ps) -> {
                if (!be.hasCog()) return false;
                ps.translate(0.0F, 1.375F, 0.0F);
                ps.mulPose(Axis.ZP.rotationDegrees(-(Mth.lerp(pt, be.lastSpin, be.spin))));
                ps.translate(0.0F, -1.375F, 0.0F);
                return true;
            })
            .rotatingPart("CogSmall", new Vector3f(1.0F, 0.0F, 0.0F), new Vector3f(0.0F, 1.375F, 0.25F),
                    (be, pt) -> Mth.lerp(pt, be.lastSpin, be.spin) * 2.0 + 3.0)
            .translatingPart("Piston", new Vector3f(1.0F, 0.0F, 0.0F),
                    (be, pt) -> Math.sin(Mth.lerp(pt, be.lastSpin, be.spin) * Math.PI / 90.0) * 0.25 + 0.125)
            .facing(ObjBerHelper::facing)
            .viewDistance(256)
            .itemParts("Base", "Cog", "CogSmall", "Piston")
            .register();
    }

    private static void registerDieselGen() {
        MachineRenderers.machine("diesel_generator", ModBlockEntities.DIESEL_GENERATOR_BE.get(),
                MachineDieselGeneratorBlockEntity.class)
            .part("Generator")
            .part("Engine", (be, pt, gt, ps) -> {
                if (be.isOn() && be.hasAcceptableFuel() && be.getTank().getFill() > 0) {
                    double swingSide = Math.sin(System.currentTimeMillis() / 50.0) * 0.005;
                    double swingFront = Math.sin(System.currentTimeMillis() / 25.0) * 0.005;
                    ps.translate(swingFront, 0.0, swingSide);
                }
                return true;
            })
            .facing(ObjBerHelper::facing)
            .viewDistance(256)
            .itemParts("Generator", "Engine")
            .register();
    }

    private static void registerCombustion() {
        MachineRenderers.machine("combustion_engine", ModBlockEntities.COMBUSTION_ENGINE_BE.get(),
                MachineCombustionEngineBlockEntity.class)
            .tintOverride("Canister", be -> {
                if (be.tank == null) return null;
                var canister = FluidContainerDefs.getCanister(be.tank.getTankType());
                if (canister == null) return null;
                int color = canister.color();
                return new float[] {
                    ((color >> 16) & 0xFF) / 255.0F,
                    ((color >> 8) & 0xFF) / 255.0F,
                    (color & 0xFF) / 255.0F,
                    1.0F
                };
            })
            .part("Engine")
            .part("Canister")
            .rotatingPart("Hatch", new Vector3f(0.0F, -1.0F, 0.0F), new Vector3f(1.0F, 0.0F, -2.6875F),
                    (be, pt) -> Mth.lerp(pt, be.prevDoorAngle, be.doorAngle))
            .facing(ObjBerHelper::facing)
            .viewDistance(256)
            .itemParts("Engine", "Canister", "Hatch")
            .register();
    }

    private static void registerSteamEngine() {
        MachineRenderers.machine("steam_engine", ModBlockEntities.STEAM_ENGINE_BE.get(),
                MachineSteamEngineBlockEntity.class)
            .part("Base")
            .rotatingPart("Flywheel", new Vector3f(0.0F, 0.0F, -1.0F), new Vector3f(2.0F, 1.375F, 0.0F),
                    (be, pt) -> Mth.lerp(pt, be.lastRotor, be.rotor))
            .rotatingPart("Shaft", new Vector3f(1.0F, 0.0F, 0.0F), new Vector3f(0.0F, 1.375F, -0.5F),
                    (be, pt) -> Mth.lerp(pt, be.lastRotor, be.rotor) * 2.0)
            .part("Transmission", (be, pt, gt, ps) -> {
                double rot = Mth.lerp(pt, be.lastRotor, be.rotor);
                double sin = Math.sin(rot * Math.PI / 180.0) * 0.25 - 0.25;
                double cos = Math.cos(rot * Math.PI / 180.0) * 0.25;
                double ang = Math.acos(cos / 1.875);
                ps.translate(sin, cos, 0.0);
                ps.translate(2.25, 1.375, 0.0);
                ps.mulPose(Axis.ZN.rotationDegrees((float) (ang * 180.0 / Math.PI - 90.0)));
                ps.translate(-2.25, -1.375, 0.0);
                return true;
            })
            .part("Piston", (be, pt, gt, ps) -> {
                double rot = Mth.lerp(pt, be.lastRotor, be.rotor);
                double sin = Math.sin(rot * Math.PI / 180.0) * 0.25 - 0.25;
                double cos = Math.cos(rot * Math.PI / 180.0) * 0.25;
                double cath = Math.sqrt(3.515625 - (cos * cos) / 2.0);
                ps.translate(1.875 - cath + sin, 0.0, 0.0);
                return true;
            })
            .facing(ObjBerHelper::facing)
            .viewDistance(256)
            .itemParts("Base", "Flywheel", "Shaft", "Transmission", "Piston")
            .register();
    }

    private MachineEngineRenderer() {}
}
