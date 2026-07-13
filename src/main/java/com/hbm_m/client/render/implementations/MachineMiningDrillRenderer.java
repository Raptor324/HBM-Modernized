package com.hbm_m.client.render.implementations;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.machines.MachineMiningDrillBlockEntity;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * BER fuer den Large Mining Drill: rotiert nur das separat gebackene "Drillbit"-Teil
 * (siehe {@code mining_drill_bit.json}, per OBJ-"visibility" aus {@code mining_drill.obj}
 * herausgeloest).
 * <p>
 * Rendert ueber {@link MeshRenderCache}/{@link SingleMeshVboRenderer} (VBO-System) - ein direkter
 * {@code putBulkData}-Ansatz blieb mit Embeddium/Sodium unsichtbar (siehe Kommentar in
 * {@link MachineTurretRenderer}).
 */
public class MachineMiningDrillRenderer implements BlockEntityRenderer<MachineMiningDrillBlockEntity> {

    private static final ResourceLocation DRILLBIT_MODEL_ID =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/mining_drill_bit");
    private static final String DRILLBIT_CACHE_KEY = "mining_drill_bit";

    public MachineMiningDrillRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MachineMiningDrillBlockEntity be, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BakedModel bitModel = getDrillbitModel();
        if (bitModel == null) return;

        SingleMeshVboRenderer renderer = MeshRenderCache.getOrCreateRenderer(DRILLBIT_CACHE_KEY, bitModel);
        if (renderer == null) return;

        float angle = Mth.lerp(partialTick, be.prevAngle, be.angle);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));
        poseStack.translate(-0.5, 0.0, -0.5);

        renderer.render(poseStack, packedLight, be.getBlockPos(), be, bufferSource);

        poseStack.popPose();
    }

    @Nullable
    private static BakedModel getDrillbitModel() {
        var modelManager = Minecraft.getInstance().getModelManager();
        BakedModel model = modelManager.getModel(DRILLBIT_MODEL_ID);
        return (model == null || model == modelManager.getMissingModel()) ? null : model;
    }
}
