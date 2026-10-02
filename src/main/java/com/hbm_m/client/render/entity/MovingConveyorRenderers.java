package com.hbm_m.client.render.entity;

import java.util.Random;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.entity.conveyor.MovingConveyorItemEntity;
import com.hbm_m.entity.conveyor.MovingConveyorPackageEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code RenderMovingItem} und {@code RenderMovingPackage}. */
public final class MovingConveyorRenderers {

    private MovingConveyorRenderers() {}

    /**
     * {@code RenderMovingItem}: das Teil liegt als ruhendes Boden-Item auf dem Band, flache Items flach hingelegt; jede
     * Entitaet hat einen kleinen festen Hoehenversatz aus ihrer ID.
     */
    public static class Item extends EntityRenderer<MovingConveyorItemEntity> {
        private final ItemRenderer itemRenderer;

        public Item(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.itemRenderer = ctx.getItemRenderer();
        }

        @Override
        public void render(MovingConveyorItemEntity entity, float yaw, float pt, PoseStack pose, MultiBufferSource buffer, int light) {
            ItemStack stack = entity.getItem();
            if (stack.isEmpty()) return;

            pose.pushPose();
            Random rand = new Random(entity.getId());
            pose.translate(0, rand.nextDouble() * 0.0625, 0);

            BakedModel model = itemRenderer.getModel(stack, entity.level(), null, entity.getId());
            if (!model.isGui3d()) {
                pose.mulPose(Axis.XP.rotationDegrees(90F));
                pose.translate(0.0, -0.1875, 0.0);
                if (Minecraft.getInstance().options.graphicsMode().get() == net.minecraft.client.GraphicsStatus.FAST) {
                    pose.mulPose(Axis.YP.rotationDegrees(180F));
                }
            }

            itemRenderer.render(stack, ItemDisplayContext.GROUND, false, pose, buffer, light, OverlayTexture.NO_OVERLAY, model);
            pose.popPose();
            super.render(entity, yaw, pt, pose, buffer, light);
        }

        @Override
        public ResourceLocation getTextureLocation(MovingConveyorItemEntity entity) { return TextureAtlas.LOCATION_BLOCKS; }
    }

    /** {@code RenderMovingPackage}: eine Kiste ({@code crate}) als doppelt grosses Boden-Item. */
    public static class Package extends EntityRenderer<MovingConveyorPackageEntity> {
        private final ItemRenderer itemRenderer;
        private ItemStack dummy;

        public Package(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.itemRenderer = ctx.getItemRenderer();
        }

        @Override
        public void render(MovingConveyorPackageEntity entity, float yaw, float pt, PoseStack pose, MultiBufferSource buffer, int light) {
            if (dummy == null) dummy = new ItemStack(ModBlocks.CRATE.get());

            pose.pushPose();
            pose.translate(0, -0.0125, 0);
            float scale = 2F;
            pose.scale(scale, scale, scale);
            BakedModel model = itemRenderer.getModel(dummy, entity.level(), null, entity.getId());
            itemRenderer.render(dummy, ItemDisplayContext.GROUND, false, pose, buffer, light, OverlayTexture.NO_OVERLAY, model);
            pose.popPose();
            super.render(entity, yaw, pt, pose, buffer, light);
        }

        @Override
        public ResourceLocation getTextureLocation(MovingConveyorPackageEntity entity) { return TextureAtlas.LOCATION_BLOCKS; }
    }
}
