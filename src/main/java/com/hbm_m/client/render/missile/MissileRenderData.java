package com.hbm_m.client.render.missile;

import com.hbm_m.client.model.MissileBakedModel;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class MissileRenderData {

    private final ResourceLocation itemId;
    private final ResourceLocation texture;
    private final float scale;

    public MissileRenderData(ResourceLocation itemId, ResourceLocation texture, float scale) {
        this.itemId = itemId;
        this.texture = texture;
        this.scale = scale;
    }

    public ResourceLocation itemId() {
        return itemId;
    }

    public ResourceLocation texture() {
        return texture;
    }

    public float scale() {
        return scale;
    }

    public void render(PoseStack poseStack, int packedLight, BlockPos lightPos) {
        render(poseStack, packedLight, lightPos, null);
    }

    public void render(PoseStack poseStack, int packedLight, BlockPos lightPos,
                       @Nullable MultiBufferSource bufferSource) {
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        bindAtlas();
        MissileBakedModel model = MissileRenderHelper.resolveMissileModel(itemId);
        if (model != null) {
            MissileRenderHelper.drawVboParts(model, poseStack, packedLight, lightPos, bufferSource);
        }
        poseStack.popPose();
    }

    private static void bindAtlas() {
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        Minecraft.getInstance().getTextureManager().bindForSetup(TextureAtlas.LOCATION_BLOCKS);
    }

    public static MissileRenderData standard(ResourceLocation itemId, ResourceLocation texture) {
        return new MissileRenderData(itemId, texture, 1.0F);
    }

    public static MissileRenderData large(ResourceLocation itemId, ResourceLocation texture) {
        return new MissileRenderData(itemId, texture, 1.5F);
    }

    @Nullable
    public static MissileRenderData stealth(ResourceLocation itemId) {
        return new MissileRenderData(itemId, MissileTextures.MISSILE_STEALTH, 1.0F);
    }
}
