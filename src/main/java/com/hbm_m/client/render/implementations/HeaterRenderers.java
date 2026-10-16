package com.hbm_m.client.render.implementations;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.blockentity.machines.FireboxBaseBlockEntity;
import com.hbm_m.blockentity.machines.HeatingOvenBlockEntity;
import com.hbm_m.blockentity.machines.MachineFireboxBlockEntity;
import com.hbm_m.client.model.ConfiguredMultipartBakedModel;
import com.hbm_m.platform.RenderHooks;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;


/**
 * Grinder (grit box) and Heating Oven on the {@link MachineRenderers} factory -- port of
 * {@code RenderFirebox}/{@code RenderHeatingOven} (1.7.10):
 * <ul>
 *   <li>Main is baked into the chunk mesh (blockstate model -> {@code machine_parts_loader});</li>
 *   <li>Door is an animated part (grinder: 135-degree rotation around the hinge
 *       at (1.375, 0, 0.375); oven: Z shift of 0.75 * door / 135);</li>
 *   <li>Inner is a dynamic part: InnerBurning while burning, otherwise
 *       InnerEmpty (grinder) / Inner (oven).</li>
 * </ul>
 */
public final class HeaterRenderers {

    private static final RandomSource RANDOM = RandomSource.create(42);

    private HeaterRenderers() {}

    public static void register() {
        // Grinder: door rotates, firebox is InnerEmpty/InnerBurning
        MachineRenderers.machine("firebox", com.hbm_m.blockentity.ModBlockEntities.FIREBOX_BE.get(),
                MachineFireboxBlockEntity.class)
            .part("Main") // empty_world_quads: the body is drawn by the BER
            .lightOverride("Inner", be -> be.isBurning()
                    ? net.minecraft.client.renderer.LightTexture.pack(15, 15) : -1)
            .part("Door", "Door", HeaterRenderers::animateFireboxDoor)
            .dynamicPart("Inner", HeaterRenderers::fireboxInnerQuads,
                    be -> be.isBurning() ? "burning" : "idle")
            .register();

        // Heating oven: door slides, firebox is Inner/InnerBurning
        MachineRenderers.machine("heating_oven", com.hbm_m.blockentity.ModBlockEntities.HEATING_OVEN_BE.get(),
                HeatingOvenBlockEntity.class)
            .part("Main") // empty_world_quads: the body is drawn by the BER
            .lightOverride("Inner", be -> be.isBurning()
                    ? net.minecraft.client.renderer.LightTexture.pack(15, 15) : -1)
            .part("Door", "Door", HeaterRenderers::animateOvenDoor)
            .dynamicPart("Inner", HeaterRenderers::ovenInnerQuads,
                    be -> be.isBurning() ? "burning" : "idle")
            .register();
    }

    // -- Doors ------------------------------------------------------------

    /** Port of RenderFirebox: rotate around the hinge (1.375, 0, 0.375) by -Y. */
    private static boolean animateFireboxDoor(FireboxBaseBlockEntity be, float partialTick, long gameTime, PoseStack pose) {
        float door = be.getInterpolatedDoorAngle(partialTick);
        pose.translate(1.375F, 0F, 0.375F);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-door));
        pose.translate(-1.375F, 0F, -0.375F);
        return true;
    }

    /** Port of RenderHeatingOven: the door slides along Z by door * 0.75 / 135. */
    private static boolean animateOvenDoor(FireboxBaseBlockEntity be, float partialTick, long gameTime, PoseStack pose) {
        float door = be.getInterpolatedDoorAngle(partialTick);
        pose.translate(0F, 0F, door * 0.75F / 135F);
        return true;
    }

    // -- Fireboxes --------------------------------------------------------

    private static List<BakedQuad> fireboxInnerQuads(FireboxBaseBlockEntity be) {
        String part = be.isBurning() ? "InnerBurning" : "InnerEmpty";
        return innerQuads(be, part);
    }

    private static List<BakedQuad> ovenInnerQuads(FireboxBaseBlockEntity be) {
        String part = be.isBurning() ? "InnerBurning" : "Inner";
        return innerQuads(be, part);
    }

    private static List<BakedQuad> innerQuads(FireboxBaseBlockEntity be, String partName) {
        BakedModel partModel = part(be, partName);
        if (partModel == null) return List.of();

        List<BakedQuad> quads = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            quads.addAll(RenderHooks.getModelQuads(partModel, null, dir, RANDOM, null));
        }
        quads.addAll(RenderHooks.getModelQuads(partModel, null, null, RANDOM, null));
        return quads;
    }

    private static <T extends BlockEntity> BakedModel part(T be, String partName) {
        BakedModel raw = Minecraft.getInstance().getBlockRenderer().getBlockModel(be.getBlockState());
        if (raw instanceof ConfiguredMultipartBakedModel model) {
            return model.getPart(partName);
        }
        return null;
    }

}
