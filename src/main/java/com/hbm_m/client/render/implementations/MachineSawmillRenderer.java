package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.block.machines.MachineThresherBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineAutosawBlockEntity;
import com.hbm_m.blockentity.machines.MachineSawmillBlockEntity;
import com.hbm_m.blockentity.machines.MachineThresherBlockEntity;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Modern Nucleus machine renderers for timber processing & sawmill machinery:
 * - Sawmill: High-RPM circular saw blade with counter-rotating reduction gears
 * - Autosaw: Multi-joint articulating cutting arm with rotating base and spinning head
 * - Thresher: Harvesting thresher head with oscillating cutting arms and rotating cylinder
 */
public final class MachineSawmillRenderer {

    public static void register() {
        registerSawmill();
        registerAutosaw();
        registerThresher();
    }

    private static void registerSawmill() {
        MachineRenderers.machine("sawmill", ModBlockEntities.SAWMILL_BE.get(),
                MachineSawmillBlockEntity.class)
            .part("Main")
            .rotatingPart("Blade", 0f, 0f, 1f, 0f, 1.375f, 0f, 0f, 0f, 0f,
                    (be, pt) -> -(Mth.lerp(pt, be.lastSpin, be.spin) * 2.0))
            .rotatingPart("GearLeft", 0f, 0f, 1f, 0.5625f, 1.375f, 0f, 0f, 0f, 0f,
                    (be, pt) -> Mth.lerp(pt, be.lastSpin, be.spin))
            .rotatingPart("GearRight", 0f, 0f, 1f, -0.5625f, 1.375f, 0f, 0f, 0f, 0f,
                    (be, pt) -> -(Mth.lerp(pt, be.lastSpin, be.spin)))
            .blockTransform(MachineSawmillRenderer::applySawmillTransform)
            .itemParts("Main", "GearLeft", "GearRight")
            .register();
    }

    private static void registerAutosaw() {
        MachineRenderers.machine("autosaw", ModBlockEntities.AUTOSAW_BE.get(),
                MachineAutosawBlockEntity.class)
            .part("Base")
            .part("Main", (be, pt, gt, ps) -> {
                double turn = Mth.lerp(pt, be.prevRotationYaw, be.rotationYaw);
                ps.mulPose(Axis.YN.rotationDegrees((float) turn));
                return true;
            })
            .part("Engine", (be, pt, gt, ps) -> {
                double turn = Mth.lerp(pt, be.prevRotationYaw, be.rotationYaw);
                double engine = be.isOn && be.getLevel() != null
                        ? Math.sin(be.getLevel().getGameTime() * 2 % (Math.PI * 2) + pt) : 0;
                ps.mulPose(Axis.YN.rotationDegrees((float) turn));
                ps.translate(0, engine * 0.01, 0);
                return true;
            })
            .part("ArmUpper", (be, pt, gt, ps) -> {
                double turn = Mth.lerp(pt, be.prevRotationYaw, be.rotationYaw);
                double angle = 80 - Mth.lerp(pt, be.prevRotationPitch, be.rotationPitch);
                ps.mulPose(Axis.YN.rotationDegrees((float) turn));
                ps.translate(0, 1.75, 0);
                ps.mulPose(Axis.XP.rotationDegrees((float) angle));
                ps.translate(0, -1.75, 0);
                return true;
            })
            .part("ArmLower", (be, pt, gt, ps) -> {
                double turn = Mth.lerp(pt, be.prevRotationYaw, be.rotationYaw);
                double angle = 80 - Mth.lerp(pt, be.prevRotationPitch, be.rotationPitch);
                ps.mulPose(Axis.YN.rotationDegrees((float) turn));
                ps.translate(0, 1.75, 0);
                ps.mulPose(Axis.XP.rotationDegrees((float) angle));
                ps.translate(0, -1.75, 0);

                ps.translate(0, 1.75, -4);
                ps.mulPose(Axis.XP.rotationDegrees((float) (angle * -2)));
                ps.translate(0, -1.75, 4);
                return true;
            })
            .part("ArmTip", (be, pt, gt, ps) -> {
                double turn = Mth.lerp(pt, be.prevRotationYaw, be.rotationYaw);
                double angle = 80 - Mth.lerp(pt, be.prevRotationPitch, be.rotationPitch);
                ps.mulPose(Axis.YN.rotationDegrees((float) turn));
                ps.translate(0, 1.75, 0);
                ps.mulPose(Axis.XP.rotationDegrees((float) angle));
                ps.translate(0, -1.75, 0);

                ps.translate(0, 1.75, -4);
                ps.mulPose(Axis.XP.rotationDegrees((float) (angle * -2)));
                ps.translate(0, -1.75, 4);

                ps.translate(0, 1.75, -8);
                ps.mulPose(Axis.XP.rotationDegrees((float) angle));
                ps.translate(0, -1.75, 8);
                return true;
            })
            .part("Sawblade", (be, pt, gt, ps) -> {
                double turn = Mth.lerp(pt, be.prevRotationYaw, be.rotationYaw);
                double angle = 80 - Mth.lerp(pt, be.prevRotationPitch, be.rotationPitch);
                float spin = Mth.lerp(pt, be.lastSpin, be.spin);
                ps.mulPose(Axis.YN.rotationDegrees((float) turn));
                ps.translate(0, 1.75, 0);
                ps.mulPose(Axis.XP.rotationDegrees((float) angle));
                ps.translate(0, -1.75, 0);

                ps.translate(0, 1.75, -4);
                ps.mulPose(Axis.XP.rotationDegrees((float) (angle * -2)));
                ps.translate(0, -1.75, 4);

                ps.translate(0, 1.75, -8);
                ps.mulPose(Axis.XP.rotationDegrees((float) angle));
                ps.translate(0, -1.75, 8);

                ps.translate(0, 1.75, -10);
                ps.mulPose(Axis.YN.rotationDegrees(spin));
                ps.translate(0, -1.75, 10);
                return true;
            })
            .blockTransform((be, animator) -> animator.translate(0.5, 0.0, 0.5))
            .itemParts("Base", "Main", "ArmUpper")
            .register();
    }

    private static void registerThresher() {
        MachineRenderers.machine("thresher", ModBlockEntities.THRESHER_BE.get(),
                MachineThresherBlockEntity.class)
            .part("Base")
            .part("Engine", (be, pt, gt, ps) -> {
                double engine = be.isOn && be.getLevel() != null
                        ? Math.sin(be.getLevel().getGameTime() * 2 % (Math.PI * 2) + pt) : 0;
                ps.translate(0, engine * 0.01, 0);
                return true;
            })
            .part("ArmUpper", (be, pt, gt, ps) -> {
                double angle = 82.5 - Mth.lerp(pt, be.prevAngle, be.angle);
                ps.translate(0, 0.5, -1);
                ps.mulPose(Axis.XP.rotationDegrees((float) angle));
                ps.translate(0, -0.5, 1);
                return true;
            })
            .part("ArmLower", (be, pt, gt, ps) -> {
                double angle = 82.5 - Mth.lerp(pt, be.prevAngle, be.angle);
                ps.translate(0, 0.5, -1);
                ps.mulPose(Axis.XP.rotationDegrees((float) angle));
                ps.translate(0, -0.5, 1);

                ps.translate(0, 0.5, -5);
                ps.mulPose(Axis.XP.rotationDegrees((float) (angle * -2)));
                ps.translate(0, -0.5, 5);
                return true;
            })
            .part("Front", (be, pt, gt, ps) -> {
                double angle = 82.5 - Mth.lerp(pt, be.prevAngle, be.angle);
                ps.translate(0, 0.5, -1);
                ps.mulPose(Axis.XP.rotationDegrees((float) angle));
                ps.translate(0, -0.5, 1);

                ps.translate(0, 0.5, -5);
                ps.mulPose(Axis.XP.rotationDegrees((float) (angle * -2)));
                ps.translate(0, -0.5, 5);

                ps.translate(0, 0.5, -9);
                ps.mulPose(Axis.XP.rotationDegrees((float) angle));
                ps.translate(0, -0.5, 9);
                return true;
            })
            .part("Wheel", (be, pt, gt, ps) -> {
                double angle = 82.5 - Mth.lerp(pt, be.prevAngle, be.angle);
                float spin = Mth.lerp(pt, be.lastSpin, be.spin);
                ps.translate(0, 0.5, -1);
                ps.mulPose(Axis.XP.rotationDegrees((float) angle));
                ps.translate(0, -0.5, 1);

                ps.translate(0, 0.5, -5);
                ps.mulPose(Axis.XP.rotationDegrees((float) (angle * -2)));
                ps.translate(0, -0.5, 5);

                ps.translate(0, 0.5, -9);
                ps.mulPose(Axis.XP.rotationDegrees((float) angle));
                ps.translate(0, -0.5, 9);

                ps.translate(0, 0.5, -11);
                ps.mulPose(Axis.XP.rotationDegrees(-spin));
                ps.translate(0, -0.5, 11);
                return true;
            })
            .blockTransform(MachineSawmillRenderer::applyThresherTransform)
            .itemParts("Base", "ArmUpper", "Front")
            .register();
    }

    private static void applySawmillTransform(MachineSawmillBlockEntity sawmill, LegacyAnimator animator) {
        animator.translate(0.5, 0.0, 0.5);
        if (sawmill.getBlockState().hasProperty(DummyableMachineBlock.FACING)) {
            switch (sawmill.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                case EAST -> animator.rotate(90, 0, 1, 0);
                case NORTH -> animator.rotate(180, 0, 1, 0);
                case WEST -> animator.rotate(270, 0, 1, 0);
                default -> {}
            }
        }
    }

    private static void applyThresherTransform(MachineThresherBlockEntity thresher, LegacyAnimator animator) {
        animator.translate(0.5, 0.0, 0.5);
        if (thresher.getBlockState().hasProperty(MachineThresherBlock.FACING)) {
            switch (thresher.getBlockState().getValue(MachineThresherBlock.FACING)) {
                case NORTH -> animator.rotate(180, 0, 1, 0);
                case WEST -> animator.rotate(270, 0, 1, 0);
                case EAST -> animator.rotate(90, 0, 1, 0);
                default -> {}
            }
        }
    }

    private MachineSawmillRenderer() {}
}
