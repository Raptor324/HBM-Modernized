package com.hbm_m.client.render.implementations;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.MachineMiningDrillBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineMiningDrillBlockEntity;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.PlatformHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Big mining drill on the {@link MachineRenderers} factory (1:1 port of 1.7.10 RenderExcavator):
 * Crusher1/Crusher2 are counter-phase pendulums, Drillbit spins and lowers.
 * The mine shaft segments ("Shaft") are a variable-length stack drawn by an immediate hook
 * through a per-BE VBO (as in legacy). Geometry comes from injected models (not multipart).
 * Pivots are OBJ coordinates (origin = block center) plus a 0.5 offset compensating the baked shift.
 */
public final class MachineMiningDrillRenderer {

    private static final ResourceLocation DRILLBIT_MODEL_ID = id("mining_drill_bit");
    private static final ResourceLocation SHAFT_MODEL_ID = id("mining_drill_shaft");
    private static final ResourceLocation CRUSHER1_MODEL_ID = id("mining_drill_crusher1");
    private static final ResourceLocation CRUSHER2_MODEL_ID = id("mining_drill_crusher2");

    // Original pivots (OBJ space) + 0.5 offset for our baked block space.
    private static final double CRUSHER1_PIVOT_Z = 2.8125 + 0.5;
    private static final double CRUSHER2_PIVOT_Z = 2.1875 + 0.5;
    private static final double CRUSHER_PIVOT_Y = 2.0;

    public static void register() {
        MachineRenderers.machine("miningdrill", ModBlockEntities.MINING_DRILL_BE.get(),
                MachineMiningDrillBlockEntity.class)
            .dynamicPart("Crusher1", MachineMiningDrillRenderer::animateCrusher1,
                    be -> partQuads("drill/crusher1", CRUSHER1_MODEL_ID), be -> "c1")
            .dynamicPart("Crusher2", MachineMiningDrillRenderer::animateCrusher2,
                    be -> partQuads("drill/crusher2", CRUSHER2_MODEL_ID), be -> "c2")
            .dynamicPart("Drillbit", MachineMiningDrillRenderer::animateDrillbit,
                    be -> partQuads("drill/bit", DRILLBIT_MODEL_ID), be -> "bit")
            .blockTransform(MachineMiningDrillRenderer::applyBlockTransform)
            .hook(MachineMiningDrillRenderer::renderShaftStack)
            .register();
    }

    private MachineMiningDrillRenderer() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/" + path);
    }

    private static List<net.minecraft.client.renderer.block.model.BakedQuad> partQuads(
            String cacheKey, ResourceLocation modelId) {
        BakedModel model = getModel(modelId);
        if (model == null) return List.of();
        return MeshRenderCache.getOrCompilePartGeometry(cacheKey, model).solidQuads();
    }

    @Nullable
    private static BakedModel getModel(ResourceLocation rid) {
        var modelManager = Minecraft.getInstance().getModelManager();
        BakedModel model = PlatformHooks.getModel(modelManager, rid);
        return (model == null || model == modelManager.getMissingModel()) ? null : model;
    }

    private static void applyBlockTransform(MachineMiningDrillBlockEntity be, LegacyAnimator animator) {
        Direction facing = be.getBlockState().getValue(MachineMiningDrillBlock.FACING);
        float facingYRot = switch (facing) {
            case SOUTH -> 180F;
            case WEST -> 270F;
            case EAST -> 90F;
            default -> 0F; // NORTH
        };
        animator.translate(0.5, 0.0, 0.5);
        animator.rotate(facingYRot, 0, 1, 0);
        animator.translate(-0.5, 0.0, -0.5);
    }

    private static boolean animateCrusher1(MachineMiningDrillBlockEntity be, float partialTick,
                                           long gameTime, PoseStack pose) {
        float crusher = Mth.lerp(partialTick, be.prevCrusherRotation, be.crusherRotation);
        pose.translate(0.5, CRUSHER_PIVOT_Y, CRUSHER1_PIVOT_Z);
        pose.mulPose(Axis.XP.rotationDegrees(-crusher));
        pose.translate(-0.5, -CRUSHER_PIVOT_Y, -CRUSHER1_PIVOT_Z);
        return true;
    }

    private static boolean animateCrusher2(MachineMiningDrillBlockEntity be, float partialTick,
                                           long gameTime, PoseStack pose) {
        float crusher = Mth.lerp(partialTick, be.prevCrusherRotation, be.crusherRotation);
        pose.translate(0.5, CRUSHER_PIVOT_Y, CRUSHER2_PIVOT_Z);
        pose.mulPose(Axis.XP.rotationDegrees(crusher));
        pose.translate(-0.5, -CRUSHER_PIVOT_Y, -CRUSHER2_PIVOT_Z);
        return true;
    }

    private static boolean animateDrillbit(MachineMiningDrillBlockEntity be, float partialTick,
                                           long gameTime, PoseStack pose) {
        applyDrillShaftTransform(be, partialTick, pose);
        return true;
    }

    /** Drill rotation around Y plus lowering by ext (shared by Drillbit and Shaft). */
    private static void applyDrillShaftTransform(MachineMiningDrillBlockEntity be, float partialTick, PoseStack pose) {
        float drillAngle = Mth.lerp(partialTick, be.prevDrillRotation, be.drillRotation);
        float ext = Mth.lerp(partialTick, be.prevDrillExtension, be.drillExtension);
        pose.translate(0.5, 0.0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-drillAngle));
        pose.translate(-0.5, 0.0, -0.5);
        pose.translate(0.0, -ext, 0.0);
    }

    /**
     * Shaft: 2-block segments fill the gap left by the lowered drill --
     * a variable number of instances, hence an immediate hook through a per-BE VBO.
     */
    private static void renderShaftStack(MachineMiningDrillBlockEntity be, float partialTick,
                                         PoseStack poseStack, MultiBufferSource bufferSource,
                                         int packedLight, int packedOverlay, MachineRenderApi api) {
        SingleMeshVboRenderer shaft = getModel(SHAFT_MODEL_ID) == null ? null
                : MeshRenderCache.getOrCreateRenderer("mining_drill_shaft", getModel(SHAFT_MODEL_ID));
        if (shaft == null) return;

        poseStack.pushPose();
        applyDrillShaftTransform(be, partialTick, poseStack);
        float e = Mth.lerp(partialTick, be.prevDrillExtension, be.drillExtension);
        while (e >= -1.5F) {
            shaft.render(poseStack, packedLight, be.getBlockPos(), be, bufferSource);
            poseStack.translate(0.0, 2.0, 0.0);
            e -= 2F;
        }
        poseStack.popPose();
    }
}
