package com.hbm_m.client.render.implementations;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.client.model.SoyuzRocketBakedModel;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.entity.missile.SoyuzEntity;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.PlatformHooks;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the ascending {@link SoyuzEntity} by reusing the already-built
 * {@link SoyuzRocketBakedModel} VBO path (same mesh as the decorative
 * {@code deco_soyuz_rocket} block / the launcher's mounted-rocket preview).
 */

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class SoyuzEntityRenderer extends EntityRenderer<SoyuzEntity> {

    private static final ResourceLocation ROCKET_MODEL_ID =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/deco_soyuz_rocket");
    private static final ResourceLocation DUMMY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/soyuz/launcher_table.png");

    public SoyuzEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SoyuzEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int packedLight) {
        BakedModel part = getRocketPart();
        if (part == null) return;

        SingleMeshVboRenderer renderer = MeshRenderCache.getOrCreateRenderer("soyuz_entity", "Rocket", part);
        if (renderer == null) return;

        poseStack.pushPose();
        renderer.render(poseStack, packedLight, entity.blockPosition(), null, bufferSource);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SoyuzEntity entity) {
        return DUMMY_TEXTURE;
    }

    @Nullable
    private static BakedModel getRocketPart() {
        var modelManager = Minecraft.getInstance().getModelManager();
        BakedModel model = PlatformHooks.getModel(modelManager, ROCKET_MODEL_ID);
        if (model == null || model == modelManager.getMissingModel()) return null;
        if (!(model instanceof SoyuzRocketBakedModel rocketModel)) return null;
        return rocketModel.getPart(SoyuzRocketBakedModel.ROCKET);
    }
}
