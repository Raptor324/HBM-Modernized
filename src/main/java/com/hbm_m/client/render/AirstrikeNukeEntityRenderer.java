package com.hbm_m.client.render;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.entity.grenades.AirstrikeEntity;
import com.hbm_m.entity.grenades.AirstrikeNukeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public class AirstrikeNukeEntityRenderer extends EntityRenderer<AirstrikeNukeEntity> {

    private final BlockRenderDispatcher blockRenderer;

    public AirstrikeNukeEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(AirstrikeNukeEntity entity,
                       float entityYaw,
                       float partialTicks,
                       PoseStack poseStack,
                       MultiBufferSource buffer,
                       int packedLight) {

        poseStack.pushPose(); // ← Добавьте pushPose()

        // ✅ ОДИН поворот: 180° + направление движения
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));

        // ✅ Смещение центра модели
        poseStack.translate(-0.5, 0.0, -0.5);

        // ✅ Масштаб x3
        poseStack.scale(10.0F, 10.0F, 10.0F);

        BlockState state = ModBlocks.B29.get().defaultBlockState();

        // Рисуем модель самолета
        blockRenderer.renderSingleBlock(
                state,
                poseStack,
                buffer,
                packedLight,
                OverlayTexture.NO_OVERLAY
        );

        poseStack.popPose(); // ← Соответствующий popPose()
    }


    @Override
    public ResourceLocation getTextureLocation(AirstrikeNukeEntity entity) {
        // Не используется при рендере через blockRenderer, можно вернуть что‑нибудь дефолтное
        return new ResourceLocation("minecraft", "textures/block/iron_block.png");
    }
}
