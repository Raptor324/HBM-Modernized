package com.hbm_m.client.render.missile;

import com.hbm_m.client.model.MissileBakedModel;
import com.hbm_m.client.render.item.ItemRenderMissileGeneric.RenderMissileType;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public final class MissileRenderData {

    private final ResourceLocation itemId;
    private final ResourceLocation texture;
    private final float scale;
    private final RenderMissileType renderType;

    public MissileRenderData(ResourceLocation itemId, ResourceLocation texture, float scale,
                             RenderMissileType renderType) {
        this.itemId = itemId;
        this.texture = texture;
        this.scale = scale;
        this.renderType = renderType;
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

    public RenderMissileType renderType() {
        return renderType;
    }

    public void render(PoseStack poseStack, int packedLight, BlockPos lightPos) {
        render(poseStack, packedLight, lightPos, null, null);
    }

    public void render(PoseStack poseStack, int packedLight, BlockPos lightPos,
                       @Nullable MultiBufferSource bufferSource) {
        render(poseStack, packedLight, lightPos, bufferSource, null);
    }

    public void render(PoseStack poseStack, int packedLight, BlockPos lightPos,
                       @Nullable MultiBufferSource bufferSource, @Nullable BlockEntity blockEntity) {
        MissileBakedModel model = MissileRenderHelper.resolveMissileModel(itemId);
        if (model == null) {
            return;
        }
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        MissileRenderHelper.drawMissileMesh(new ItemStack(item), model, poseStack, packedLight, lightPos,
                bufferSource, blockEntity);
        poseStack.popPose();
    }

    public static MissileRenderData standard(ResourceLocation itemId, ResourceLocation texture,
                                             RenderMissileType renderType) {
        return new MissileRenderData(itemId, texture, 1.0F, renderType);
    }

    public static MissileRenderData large(ResourceLocation itemId, ResourceLocation texture,
                                          RenderMissileType renderType) {
        return new MissileRenderData(itemId, texture, 1.5F, renderType);
    }

    @Nullable
    public static MissileRenderData stealth(ResourceLocation itemId) {
        return new MissileRenderData(itemId, MissileTextures.MISSILE_STEALTH, 1.0F, RenderMissileType.TYPE_STEALTH);
    }

    /** Pad / in-flight fallback; reuses {@link RenderMissileType} from registry when the item is registered. */
    public static MissileRenderData withPadScale(ResourceLocation itemId, ResourceLocation texture, float padScale) {
        RenderMissileType type = RenderMissileType.TYPE_TIER1;
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item != null) {
            MissileRenderData registered = MissileRenderRegistry.get(item);
            if (registered != null) {
                type = registered.renderType();
            }
        }
        return new MissileRenderData(itemId, texture, padScale, type);
    }
}
