package com.hbm_m.client.render;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.hbm_m.client.model.MachineAdvancedAssemblerBakedModel;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


@OnlyIn(Dist.CLIENT)
public class MachineAdvancedAssemblerVboRenderer {
    private static final String BASE = "Base";
    private static final String FRAME = "Frame";
    
    private final MachineAdvancedAssemblerBakedModel model;

    public MachineAdvancedAssemblerVboRenderer(MachineAdvancedAssemblerBakedModel model) {
        this.model = model;
    }

    // Статические части без трансформаций
    public void renderStaticBase(PoseStack poseStack, int packedLight, BlockPos blockPos, 
                                @Nullable BlockEntity blockEntity) {
        renderStaticBase(poseStack, packedLight, blockPos, blockEntity, null);
    }

    public void renderStaticBase(PoseStack poseStack, int packedLight, BlockPos blockPos, 
                                @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource) {
        BakedModel part = model.getPart(BASE);
        if (part != null) {
            var r = GlobalMeshCache.getOrCreateRenderer("assembler_" + BASE, part);
            if (r != null) r.render(poseStack, packedLight, blockPos, blockEntity, bufferSource);
        }
    }

    public void renderStaticFrame(PoseStack poseStack, int packedLight, BlockPos blockPos,
                                 @Nullable BlockEntity blockEntity) {
        renderStaticFrame(poseStack, packedLight, blockPos, blockEntity, null);
    }

    public void renderStaticFrame(PoseStack poseStack, int packedLight, BlockPos blockPos,
                                 @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource) {
        BakedModel part = model.getPart(FRAME);
        if (part != null) {
            var r = GlobalMeshCache.getOrCreateRenderer("assembler_" + FRAME, part);
            if (r != null) r.render(poseStack, packedLight, blockPos, blockEntity, bufferSource);
        }      
    }

    // КЛЮЧЕВОЕ ИСПРАВЛЕНИЕ: Применяем трансформации ЧЕРЕЗ PoseStack (как в двери)
    public void renderAnimatedPart(PoseStack poseStack, int packedLight, String partName, 
                                  Matrix4f transform, BlockPos blockPos, 
                                  @Nullable BlockEntity blockEntity) {
        renderAnimatedPart(poseStack, packedLight, partName, transform, blockPos, blockEntity, null);
    }

    public void renderAnimatedPart(PoseStack poseStack, int packedLight, String partName, 
                                  Matrix4f transform, BlockPos blockPos, 
                                  @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource) {
        BakedModel part = model.getPart(partName);
        if (part != null) {
            poseStack.pushPose();
            
            // КРИТИЧНО: Применяем трансформацию к PoseStack ПЕРЕД рендерингом
            if (transform != null) {
                poseStack.last().pose().mul(transform);
            }
            
            var r = GlobalMeshCache.getOrCreateRenderer("assembler_" + partName, part);
            if (r != null) r.render(poseStack, packedLight, blockPos, blockEntity, bufferSource);
            
            poseStack.popPose();
        }
    }

    // Метод совместимости с существующим кодом
    public void renderPart(PoseStack poseStack, int packedLight, String partName, 
                          Matrix4f transform, BlockPos blockPos) {
        renderAnimatedPart(poseStack, packedLight, partName, transform, blockPos, null);
    }

    public static void clearGlobalCache() {
        GlobalMeshCache.clearAll();
    }
}