package com.hbm_m.client.model.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Matrix4f;

public abstract class AbstractPartBasedRenderer<T extends BlockEntity, M extends BakedModel>
        implements BlockEntityRenderer<T> {

    protected abstract M getModelType(BakedModel rawModel);
    protected abstract Direction getFacing(T blockEntity);
    protected abstract void renderParts(T blockEntity, M model, LegacyAnimator animator, float partialTick,
                                        int packedLight, int packedOverlay, PoseStack poseStack, MultiBufferSource bufferSource);

    private static Matrix4f currentModelViewMatrix = new Matrix4f();
    private static boolean gpuStateSetup = false;
    private static final net.minecraft.client.renderer.RenderType RT_SOLID = net.minecraft.client.renderer.RenderType.solid();

    public static Matrix4f getCurrentModelViewMatrix() {
        return new Matrix4f(currentModelViewMatrix);
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        beginBatch();
        currentModelViewMatrix = poseStack.last().pose();
        
        if (!isInViewFrustum(blockEntity)) {
            return;
        }

        if (!gpuStateSetup) {
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
            gpuStateSetup = true;
        }
        
        BakedModel rawModel = Minecraft.getInstance().getBlockRenderer()
            .getBlockModel(blockEntity.getBlockState());
        M model = getModelType(rawModel);
        
        if (model == null) return;

        LegacyAnimator animator = LegacyAnimator.create(poseStack, bufferSource,
                packedLight, packedOverlay);

        poseStack.pushPose();
        animator.setupBlockTransform(getFacing(blockEntity));

        renderParts(blockEntity, model, animator, partialTick, packedLight, packedOverlay, poseStack, bufferSource);

        poseStack.popPose();
        endBatch();
    }

    public static void beginBatch() {
        if (!gpuStateSetup) {
            com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS);
            RT_SOLID.setupRenderState();
            gpuStateSetup = true;
        }
    }
    
    public static void endBatch() {
        if (gpuStateSetup) {
            RT_SOLID.clearRenderState();
            net.minecraft.client.Minecraft.getInstance().gameRenderer.lightTexture().turnOffLightLayer();
            gpuStateSetup = false;
        }
    }

    protected final Minecraft getMinecraft() {
        return Minecraft.getInstance();
    }

    protected boolean isInViewFrustum(T blockEntity) {
        return true;
    }
}
