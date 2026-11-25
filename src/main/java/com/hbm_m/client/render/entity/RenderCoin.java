package com.hbm_m.client.render.entity;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.entity.projectile.EntityCoin;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1:1 {@code RenderCoin}: renders chip.obj with chip_gold.png texture,
 * rotated by yaw around negative Y and spinning around Z.
 */
public class RenderCoin extends EntityRenderer<EntityCoin> {

    public static final SimpleObjModel coin = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/trinkets/chip.obj"));
    public static final ResourceLocation coin_tex = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/trinkets/chip_gold.png");

    public RenderCoin(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(@NotNull EntityCoin coinEntity, float yaw, float partialTicks, @NotNull PoseStack ps, @NotNull MultiBufferSource buffer, int light) {
        ps.pushPose();
        ps.mulPose(Axis.YN.rotationDegrees(Mth.lerp(partialTicks, coinEntity.yRotO, coinEntity.getYRot()) - 90.0F));
        ps.mulPose(Axis.ZP.rotationDegrees((coinEntity.tickCount + partialTicks) * 45.0F));

        float scale = 0.125F;
        ps.scale(scale, scale, scale);

        coin.renderAll(ps, buffer.getBuffer(RenderType.entityCutout(coin_tex)), light);
        ps.popPose();

        super.render(coinEntity, yaw, partialTicks, ps, buffer, light);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EntityCoin entity) {
        return coin_tex;
    }
}
