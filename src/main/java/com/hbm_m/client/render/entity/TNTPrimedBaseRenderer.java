package com.hbm_m.client.render.entity;

import com.hbm_m.entity.item.EntityTNTPrimedBase;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.TntMinecartRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** 1:1 {@code RenderTNTPrimedBase}: Block des Sprengkoerpers, zum Ende aufblaehend, alle 5 Ticks weiss blinkend. */
public class TNTPrimedBaseRenderer extends EntityRenderer<EntityTNTPrimedBase> {

    private final BlockRenderDispatcher blockRenderer;

    public TNTPrimedBaseRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0F;
        this.blockRenderer = ctx.getBlockRenderDispatcher();
    }

    @Override
    public void render(EntityTNTPrimedBase tnt, float yaw, float partialTicks, PoseStack ps, MultiBufferSource buffers, int light) {
        ps.pushPose();
        ps.translate(0.0F, 0.5F, 0.0F);
        int fuse = tnt.fuse;
        if ((float) fuse - partialTicks + 1.0F < 10.0F) {
            float f = 1.0F - ((float) fuse - partialTicks + 1.0F) / 10.0F;
            f = Mth.clamp(f, 0.0F, 1.0F);
            f *= f;
            f *= f;
            float scale = 1.0F + f * 0.3F;
            ps.scale(scale, scale, scale);
        }
        ps.mulPose(Axis.YP.rotationDegrees(-90.0F));
        ps.translate(-0.5F, -0.5F, 0.5F);
        ps.mulPose(Axis.YP.rotationDegrees(90.0F));
        TntMinecartRenderer.renderWhiteSolidBlock(this.blockRenderer, tnt.getBlockState(), ps, buffers, light, fuse / 5 % 2 == 0);
        ps.popPose();
        super.render(tnt, yaw, partialTicks, ps, buffers, light);
    }

    @SuppressWarnings("deprecation")
    @Override
    public ResourceLocation getTextureLocation(EntityTNTPrimedBase tnt) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
