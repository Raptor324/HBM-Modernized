package com.hbm_m.client.model.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Базовый рендерер для всех BlockEntity с OBJ моделями.
 */
public abstract class AbstractPartBasedRenderer<T extends BlockEntity, M>
    implements BlockEntityRenderer<T> {
    
    private static Minecraft cachedMinecraft;
    private static BlockRenderDispatcher cachedRenderer;
    
    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        
        if (cachedMinecraft == null) {
            cachedMinecraft = Minecraft.getInstance();
            cachedRenderer = cachedMinecraft.getBlockRenderer();
        }
        
        BakedModel rawModel = cachedRenderer.getBlockModel(blockEntity.getBlockState());
        M model = getModelType(rawModel);
        if (model == null) return;
        
        LegacyAnimator animator = LegacyAnimator.create(poseStack, bufferSource,
                                                        packedLight, packedOverlay);
        animator.push();
        
        Direction facing = getFacing(blockEntity);
        animator.setupBlockTransform(facing);
        
        renderParts(blockEntity, model, animator, partialTick, packedLight, packedOverlay,
                   poseStack, bufferSource);
        
        animator.pop();
    }
    
    protected abstract M getModelType(BakedModel rawModel);
    protected abstract Direction getFacing(T blockEntity);
    protected abstract void renderParts(T blockEntity, M model, LegacyAnimator animator,
                                       float partialTick, int packedLight, int packedOverlay,
                                       PoseStack poseStack, MultiBufferSource bufferSource);
    
    protected static Minecraft getMinecraft() { return cachedMinecraft; }
    protected static BlockRenderDispatcher getRenderer() { return cachedRenderer; }
}
