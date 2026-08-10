package com.hbm_m.client.render.implementations;



import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.hbm_m.client.model.MachineAdvancedAssemblerBakedModel;
import com.hbm_m.client.render.MeshRenderCache;
import com.mojang.blaze3d.vertex.PoseStack;

import java.util.List;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
@OnlyIn(Dist.CLIENT)
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
@OnlyIn(Dist.CLIENT)
*///?} elif fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
@Environment(EnvType.CLIENT)*///?}
public class MachineAdvancedAssemblerVboRenderer {
    private static final String BASE = "Base";
    private static final String FRAME = "Frame";

    /** Ключи {@link MeshRenderCache} для merged Base / Base+Frame (один draw на машину). */
    public static final String STATIC_CLUSTER_CACHE_BASE = "assembler:staticCluster_base";
    public static final String STATIC_CLUSTER_CACHE_BASE_FRAME = "assembler:staticCluster_base_frame";
    
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
            var r = MeshRenderCache.getOrCreateRenderer("assembler_" + BASE, part);
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
            var r = MeshRenderCache.getOrCreateRenderer("assembler_" + FRAME, part);
            if (r != null) r.render(poseStack, packedLight, blockPos, blockEntity, bufferSource);
        }      
    }

    /**
     * Один VBO-draw для заранее склеенных квадов (Base или Base+Frame).
     */
    public void renderStaticCluster(PoseStack poseStack, int packedLight, BlockPos blockPos,
            @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource,
            List<BakedQuad> quads, String cacheKey) {
        if (quads == null || quads.isEmpty()) {
            return;
        }
        var r = MeshRenderCache.getOrCreateRendererFromQuadList(cacheKey, quads);
        if (r != null) {
            r.render(poseStack, packedLight, blockPos, blockEntity, bufferSource);
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
            
            var r = MeshRenderCache.getOrCreateRenderer("assembler_" + partName, part);
            if (r != null) r.render(poseStack, packedLight, blockPos, blockEntity, bufferSource);
            
            poseStack.popPose();
        }
    }

    // Метод совместимости с существующим кодом
    public void renderPart(PoseStack poseStack, int packedLight, String partName, 
                          Matrix4f transform, BlockPos blockPos) {
        renderAnimatedPart(poseStack, packedLight, partName, transform, blockPos, null);
    }
}
