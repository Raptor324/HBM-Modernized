package com.hbm_m.client.render.implementations;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import com.hbm_m.block.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

@OnlyIn(Dist.CLIENT)
// Generic over the entity: the heavy bomber and the Agent Orange sprayer are separate Entity
// classes with their own EntityType, and the renderer only needs getYRot().
public class AirstrikeEntityRenderer<T extends net.minecraft.world.entity.Entity> extends EntityRenderer<T> {

    private final BlockRenderDispatcher blockRenderer;

    public AirstrikeEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(T entity,
                       float entityYaw,
                       float partialTicks,
                       PoseStack poseStack,
                       MultiBufferSource buffer,
                       int packedLight) {

        poseStack.pushPose();

        //  ОДИН поворот: 180° + направление движения
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot() + 180.0F));

        //  Смещение центра модели
        poseStack.translate(-0.5, 0.0, -0.5);

        //  Масштаб x3
        poseStack.scale(5.0F, 5.0F, 5.0F);

        BlockState state = ModBlocks.DORNIER.get().defaultBlockState();

        // Рисуем модель самолета
        blockRenderer.renderSingleBlock(
                state,
                poseStack,
                buffer,
                packedLight,
                OverlayTexture.NO_OVERLAY
        );

        poseStack.popPose();
    }


    @Override
    public ResourceLocation getTextureLocation(T entity) {
        // Не используется при рендере через blockRenderer, можно вернуть что‑нибудь дефолтное
        return ResourceLocation.withDefaultNamespace("textures/block/iron_block.png");
    }
}

