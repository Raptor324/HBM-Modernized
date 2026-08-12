package com.hbm_m.client.render.implementations;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.machines.MachineOreSlopperBlockEntity;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.PlatformHooks;
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
 * BER fuer den Ore Slopper - rendert nur die rotierenden Teile (Fan, BladesLeft/Right), analog
 * {@link MachineMiningDrillRenderer}. Die statischen Teile (Base/Bucket/Hydraulics/Slider) kommen
 * weiterhin ueber das normale gebackene Blockmodell (siehe {@code ore_slopper.json}, dort sind
 * Fan/BladesLeft/BladesRight per "visibility" ausgeblendet, damit sie nicht doppelt gezeichnet werden).
 * Pivot-/Achsenwerte 1:1 aus {@code RenderOreSlopper} (Original) uebernommen; die Original-Slider-/
 * Bucket-Hub-Animation (Aufzugsmechanik) wird bewusst nicht nachgebaut (siehe Aufgabenstellung -
 * nur die Rotationsanimationen wurden angefordert).
 */

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class MachineOreSlopperRenderer implements BlockEntityRenderer<MachineOreSlopperBlockEntity> {

    private static final ResourceLocation FAN_MODEL_ID = id("ore_slopper_fan");
    private static final ResourceLocation BLADES_LEFT_MODEL_ID = id("ore_slopper_blades_left");
    private static final ResourceLocation BLADES_RIGHT_MODEL_ID = id("ore_slopper_blades_right");

    // Pivots 1:1 aus RenderOreSlopper (Original, OBJ-Raum) + 0.5 Versatz fuer unseren Block-Raum.
    private static final double BLADES_LEFT_PIVOT_X = 0.375 + 0.5;
    private static final double BLADES_RIGHT_PIVOT_X = -0.375 + 0.5;
    private static final double BLADES_PIVOT_Y = 2.75;
    private static final double FAN_PIVOT_Y = 1.875;
    private static final double FAN_PIVOT_Z = -1.0 + 0.5;

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/" + path);
    }

    public MachineOreSlopperRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MachineOreSlopperBlockEntity be, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        SingleMeshVboRenderer fanRenderer = getRenderer("ore_slopper_fan", FAN_MODEL_ID);
        SingleMeshVboRenderer bladesLeftRenderer = getRenderer("ore_slopper_blades_left", BLADES_LEFT_MODEL_ID);
        SingleMeshVboRenderer bladesRightRenderer = getRenderer("ore_slopper_blades_right", BLADES_RIGHT_MODEL_ID);

        float fan = Mth.lerp(partialTick, be.prevFanRotation, be.fanRotation);
        float blades = Mth.lerp(partialTick, be.prevBladesRotation, be.bladesRotation);

        if (bladesLeftRenderer != null) {
            poseStack.pushPose();
            poseStack.translate(BLADES_LEFT_PIVOT_X, BLADES_PIVOT_Y, 0.5);
            poseStack.mulPose(Axis.ZP.rotationDegrees(blades));
            poseStack.translate(-BLADES_LEFT_PIVOT_X, -BLADES_PIVOT_Y, -0.5);
            bladesLeftRenderer.render(poseStack, packedLight, be.getBlockPos(), be, bufferSource);
            poseStack.popPose();
        }

        if (bladesRightRenderer != null) {
            poseStack.pushPose();
            poseStack.translate(BLADES_RIGHT_PIVOT_X, BLADES_PIVOT_Y, 0.5);
            poseStack.mulPose(Axis.ZP.rotationDegrees(-blades));
            poseStack.translate(-BLADES_RIGHT_PIVOT_X, -BLADES_PIVOT_Y, -0.5);
            bladesRightRenderer.render(poseStack, packedLight, be.getBlockPos(), be, bufferSource);
            poseStack.popPose();
        }

        if (fanRenderer != null) {
            poseStack.pushPose();
            poseStack.translate(0.5, FAN_PIVOT_Y, FAN_PIVOT_Z);
            poseStack.mulPose(Axis.XP.rotationDegrees(-fan));
            poseStack.translate(-0.5, -FAN_PIVOT_Y, -FAN_PIVOT_Z);
            fanRenderer.render(poseStack, packedLight, be.getBlockPos(), be, bufferSource);
            poseStack.popPose();
        }
    }

    @Nullable
    private static SingleMeshVboRenderer getRenderer(String cacheKey, ResourceLocation modelId) {
        BakedModel model = getModel(modelId);
        if (model == null) return null;
        return MeshRenderCache.getOrCreateRenderer(cacheKey, model);
    }

    @Nullable
    private static BakedModel getModel(ResourceLocation id) {
        var modelManager = Minecraft.getInstance().getModelManager();
        BakedModel model = PlatformHooks.getModel(modelManager, id);
        return (model == null || model == modelManager.getMissingModel()) ? null : model;
    }
}
