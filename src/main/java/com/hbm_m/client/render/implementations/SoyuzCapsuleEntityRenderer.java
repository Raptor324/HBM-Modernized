package com.hbm_m.client.render.implementations;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.client.model.SoyuzRocketBakedModel;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.entity.missile.SoyuzCapsuleEntity;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.LoaderHooks;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the cargo-mode descent capsule as a small scaled-down instance of the
 * Soyuz rocket mesh - there's no dedicated capsule OBJ asset ported (the legacy
 * player-pilotable capsule is a separate feature outside this launcher's scope).
 */

//? if < 1.21.1 {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} else {
/*@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
*///?}
public class SoyuzCapsuleEntityRenderer extends EntityRenderer<SoyuzCapsuleEntity> {

    /** FFA-Port: Lander-Mesh + Texturen, gemeinsam vom Cargo-Entity-Renderer und dem gelandeten BER ({@link SoyuzCapsuleRenderer}) genutzt. */
    public static final SimpleObjModel SOYUZ_LANDER = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/machines/soyuz_capsule_item.obj"));
    public static final ResourceLocation LANDER_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/soyuz_lander_item.png");
    public static final ResourceLocation LANDER_RUST_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/soyuz_capsule/soyuz_lander_rust.png");

    /** Per-skin rocket models (legacy {@code SoyuzPronter.SoyuzSkin}: SOYUZ/LUNA/AUTHENTIC). */
    private static final ResourceLocation[] ROCKET_MODEL_IDS = {
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/deco_soyuz_rocket"),
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/soyuz_rocket_luna"),
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/soyuz_rocket_authentic")
    };
    private static final ResourceLocation DUMMY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/soyuz/launcher_table.png");
    private static final float SCALE = 0.15F;

    public SoyuzCapsuleEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SoyuzCapsuleEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int packedLight) {
        BakedModel part = getRocketPart(entity.soyuz);
        if (part == null) return;

        SingleMeshVboRenderer renderer = MeshRenderCache.getOrCreateRenderer("soyuz_capsule_entity/" + entity.soyuz, "Rocket", part);
        if (renderer == null) return;

        poseStack.pushPose();
        poseStack.scale(SCALE, SCALE, SCALE);
        renderer.render(poseStack, packedLight, entity.blockPosition(), null, bufferSource);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SoyuzCapsuleEntity entity) {
        return DUMMY_TEXTURE;
    }

    @Nullable
    private static BakedModel getRocketPart(int skin) {
        int index = (skin >= 0 && skin < ROCKET_MODEL_IDS.length) ? skin : 0;
        var modelManager = Minecraft.getInstance().getModelManager();
        BakedModel model = LoaderHooks.getModel(modelManager, ROCKET_MODEL_IDS[index]);
        if (model == null || model == modelManager.getMissingModel()) return null;
        if (!(model instanceof SoyuzRocketBakedModel rocketModel)) return null;
        return rocketModel.getPart(SoyuzRocketBakedModel.ROCKET);
    }
}
