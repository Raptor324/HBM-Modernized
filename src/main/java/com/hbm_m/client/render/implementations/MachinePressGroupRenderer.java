package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineAmmoPressBlockEntity;
import com.hbm_m.blockentity.machines.MachineConveyorPressBlockEntity;
import com.hbm_m.blockentity.machines.MachineEPressBlockEntity;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.UvShiftConsumer;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderHook;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Modern Nucleus machine renderers for industrial press installations:
 * - Electric Press (E-Press): Mechanical frame, reciprocating stamper head, workpiece hook
 * - Conveyor Press: Hydraulic press, reciprocating piston, moving conveyor belt
 * - Ammo Press: Dual reciprocating press punch and cartridge shell/bullet lifting plate
 */
public final class MachinePressGroupRenderer {

    public static void register() {
        registerEPress();
        registerConveyorPress();
        registerAmmoPress();
    }

    private static void registerEPress() {
        MachineRenderers.machine("epress", ModBlockEntities.EPRESS_BE.get(),
                MachineEPressBlockEntity.class)
            .part("Body")
            .translatingPart("Head", new Vector3f(0f, 1f, 0f), (press, pt) -> {
                double p = (press.lastPress + (press.renderPress - press.lastPress) * pt)
                        / (double) MachineEPressBlockEntity.getMaxPress();
                return (float) (Mth.clamp(1.0 - p, 0.0, 1.0) * 0.875);
            })
            .blockTransform(MachinePressGroupRenderer::applyEPressTransform)
            .hook(new MachineRenderHook<MachineEPressBlockEntity>() {
                @Override
                public void render(MachineEPressBlockEntity press, float partialTick, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    if (press.getSyncStack().isEmpty()) return;

                    ps.pushPose();
                    ps.translate(0.0, 1.0, 0.0);
                    ps.mulPose(Axis.YP.rotationDegrees(90.0F));
                    ps.mulPose(Axis.XP.rotationDegrees(-90.0F));
                    ps.translate(1.0F, 1.0F - 0.0625F * 165.0F / 100.0F, 0.0F);
                    ps.translate(-1.0F, -1.15F, 0.0F);
                    ObjBerHelper.renderDecoItemInFrame(ps, buf, press.getSyncStack(), light, press.getLevel());
                    ps.popPose();
                }

                @Override
                public boolean mayDrawContent(MachineEPressBlockEntity press) {
                    return !press.getSyncStack().isEmpty();
                }
            })
            .itemParts("Body", "Head")
            .register();
    }

    private static void registerConveyorPress() {
        MachineRenderers.machine("conveyor_press", ModBlockEntities.CONVEYOR_PRESS_BE.get(),
                MachineConveyorPressBlockEntity.class)
            .part("Press")
            .translatingPart("Piston", new Vector3f(0f, -1f, 0f), (be, pt) -> (float) (be.getRenderPress(pt) * 0.75))
            .part("Belt")
            .blockTransform(MachinePressGroupRenderer::applyConveyorPressTransform)
            .itemParts("Press", "Belt")
            .register();
    }

    private static void registerAmmoPress() {
        MachineRenderers.machine("ammo_press", ModBlockEntities.AMMO_PRESS_BE.get(),
                MachineAmmoPressBlockEntity.class)
            .part("Frame")
            .translatingPart("Press", new Vector3f(0f, -1f, 0f), (be, pt) -> {
                float press = Mth.lerp(pt, be.prevPress, be.press);
                return press * 0.25F;
            })
            .translatingPart("Shells", new Vector3f(0f, 1f, 0f), (be, pt) -> {
                float lift = Mth.lerp(pt, be.prevLift, be.lift);
                return lift * 0.5F - 0.5F;
            })
            .translatingPart("Bullets", new Vector3f(0f, 1f, 0f), (be, pt) -> {
                float lift = Mth.lerp(pt, be.prevLift, be.lift);
                return lift * 0.5F - 0.5F;
            })
            .blockTransform(MachinePressGroupRenderer::applyAmmoPressTransform)
            .itemParts("Frame", "Press", "Shells")
            .register();
    }

    private static void applyEPressTransform(MachineEPressBlockEntity press, LegacyAnimator animator) {
        animator.translate(0.5, 0.0, 0.5);
        animator.rotate(180, 0, 1, 0);
        switch (ObjBerHelper.meta(press)) {
            case 2 -> animator.rotate(270, 0, 1, 0);
            case 4 -> animator.rotate(0, 0, 1, 0);
            case 3 -> animator.rotate(90, 0, 1, 0);
            case 5 -> animator.rotate(180, 0, 1, 0);
            default -> {}
        }
    }

    private static void applyConveyorPressTransform(MachineConveyorPressBlockEntity press, LegacyAnimator animator) {
        animator.translate(0.5, 0.0, 0.5);
        switch (ObjBerHelper.meta(press)) {
            case 2 -> animator.rotate(90, 0, 1, 0);
            case 4 -> animator.rotate(180, 0, 1, 0);
            case 3 -> animator.rotate(270, 0, 1, 0);
            case 5 -> animator.rotate(0, 0, 1, 0);
            default -> {}
        }
    }

    private static void applyAmmoPressTransform(MachineAmmoPressBlockEntity tile, LegacyAnimator animator) {
        animator.translate(0.5, 0.0, 0.5);
        if (tile.getBlockState().hasProperty(DummyableMachineBlock.FACING)) {
            switch (tile.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                case NORTH -> animator.rotate(90, 0, 1, 0);
                case WEST -> animator.rotate(180, 0, 1, 0);
                case SOUTH -> animator.rotate(270, 0, 1, 0);
                default -> {}
            }
        }
    }

    private MachinePressGroupRenderer() {}
}
