package com.hbm_m.client.model.render;

import com.hbm_m.block.entity.DoorBlockEntity;
import com.hbm_m.util.DoorDecl;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public class DoorRenderer implements BlockEntityRenderer<DoorBlockEntity> {
    
    public DoorRenderer(BlockEntityRendererProvider.Context context) {
    }
    
    @Override
    public void render(DoorBlockEntity be, float partialTick, PoseStack poseStack,
                      MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!be.isController()) return;
        
        DoorDecl doorDecl = be.getDoorDecl();
        float progress = be.getOpenProgress(partialTick);
        Direction facing = be.getFacing();
        
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        
        // Получаем модель и текстуру из DoorDecl
        ResourceLocation modelLoc = doorDecl.getModelLocation();
        ResourceLocation textureLoc = doorDecl.getTextureLocation();
        
        renderDoorModel(poseStack, bufferSource, modelLoc, textureLoc, progress,
                       packedLight, packedOverlay);
        
        poseStack.popPose();
    }
    
    private void renderDoorModel(PoseStack poseStack, MultiBufferSource bufferSource,
                                ResourceLocation modelLoc, ResourceLocation textureLoc,
                                float progress, int light, int overlay) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutout(textureLoc));
        
        // Левая дверь сдвигается влево
        poseStack.pushPose();
        poseStack.translate(-3.5 * progress, 0, 0);
        renderDoorPart(poseStack, consumer, light, overlay, true);
        poseStack.popPose();
        
        // Правая дверь сдвигается вправо
        poseStack.pushPose();
        poseStack.translate(3.5 * progress, 0, 0);
        renderDoorPart(poseStack, consumer, light, overlay, false);
        poseStack.popPose();
    }
    
    private void renderDoorPart(PoseStack poseStack, VertexConsumer consumer, 
                               int light, int overlay, boolean isLeft) {
        // Рендерим простой куб 3.5x6x0.2 как placeholder
        // В production загружай OBJ модель через IModelCustom или ModelLoader
        PoseStack.Pose pose = poseStack.last();
        
        float width = 3.5f;
        float height = 6.0f;
        float depth = 0.2f;
        
        float x1 = isLeft ? -width : 0;
        float x2 = isLeft ? 0 : width;
        float y1 = 0;
        float y2 = height;
        float z1 = -depth / 2;
        float z2 = depth / 2;
        
        // Передняя грань
        addQuad(consumer, pose, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, 
               0, 0, 1, light, overlay);
        // Задняя грань
        addQuad(consumer, pose, x2, y1, z1, x1, y1, z1, x1, y2, z1, x2, y2, z1, 
               0, 0, -1, light, overlay);
        // Левая грань
        addQuad(consumer, pose, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, 
               -1, 0, 0, light, overlay);
        // Правая грань
        addQuad(consumer, pose, x2, y1, z2, x2, y1, z1, x2, y2, z1, x2, y2, z2, 
               1, 0, 0, light, overlay);
        // Верхняя грань
        addQuad(consumer, pose, x1, y2, z2, x2, y2, z2, x2, y2, z1, x1, y2, z1, 
               0, 1, 0, light, overlay);
        // Нижняя грань
        addQuad(consumer, pose, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, 
               0, -1, 0, light, overlay);
    }
    
    private void addQuad(VertexConsumer consumer, PoseStack.Pose pose,
                        float x1, float y1, float z1, float x2, float y2, float z2,
                        float x3, float y3, float z3, float x4, float y4, float z4,
                        float nx, float ny, float nz, int light, int overlay) {
        consumer.vertex(pose.pose(), x1, y1, z1).color(255, 255, 255, 255)
                .uv(0, 0).overlayCoords(overlay).uv2(light)
                .normal(pose.normal(), nx, ny, nz).endVertex();
        consumer.vertex(pose.pose(), x2, y2, z2).color(255, 255, 255, 255)
                .uv(1, 0).overlayCoords(overlay).uv2(light)
                .normal(pose.normal(), nx, ny, nz).endVertex();
        consumer.vertex(pose.pose(), x3, y3, z3).color(255, 255, 255, 255)
                .uv(1, 1).overlayCoords(overlay).uv2(light)
                .normal(pose.normal(), nx, ny, nz).endVertex();
        consumer.vertex(pose.pose(), x4, y4, z4).color(255, 255, 255, 255)
                .uv(0, 1).overlayCoords(overlay).uv2(light)
                .normal(pose.normal(), nx, ny, nz).endVertex();
    }
    
    @Override
    public int getViewDistance() {
        return 128;
    }
}
