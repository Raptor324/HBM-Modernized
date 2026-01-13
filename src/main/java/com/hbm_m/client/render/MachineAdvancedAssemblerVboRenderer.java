package com.hbm_m.client.render;

import com.hbm_m.client.model.MachineAdvancedAssemblerBakedModel;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;


@OnlyIn(Dist.CLIENT)
public class MachineAdvancedAssemblerVboRenderer {
    private static final String BASE = "Base";
    private static final String FRAME = "Frame";
    
    private final MachineAdvancedAssemblerBakedModel model;

    public MachineAdvancedAssemblerVboRenderer(MachineAdvancedAssemblerBakedModel model) {
        this.model = model;
    }

    // ИСПРАВЛЕНИЕ: Статические части без трансформаций
    public void renderStaticBase(PoseStack poseStack, int packedLight, BlockPos blockPos, 
                                @Nullable BlockEntity blockEntity) {
        BakedModel part = model.getPart(BASE);
        if (part != null) {
            GlobalMeshCache.getOrCreateRenderer("assembler_" + BASE, part)
                    .render(poseStack, packedLight, blockPos, blockEntity);
        }
    }

    public void renderStaticFrame(PoseStack poseStack, int packedLight, BlockPos blockPos,
                                 @Nullable BlockEntity blockEntity) {
        BakedModel part = model.getPart(FRAME);
        if (part != null) {
            GlobalMeshCache.getOrCreateRenderer("assembler_" + FRAME, part)
                    .render(poseStack, packedLight, blockPos, blockEntity);
        }      
    }

    public void renderAnimatedPart(PoseStack poseStack, int packedLight, String partName,
            Matrix4f transform, BlockPos blockPos, @Nullable BlockEntity blockEntity) {

        BakedModel part = model.getPart(partName);
        if (part == null) {
            MainRegistry.LOGGER.warn("Part {} not found in model", partName);
            return;
        }

        poseStack.pushPose();
        if (transform != null) {
            poseStack.last().pose().mul(transform);
        }

        AbstractGpuVboRenderer renderer = GlobalMeshCache.getOrCreateRenderer(
            "assembler_" + partName, part
        );
        
        if (renderer != null) {
            renderer.render(poseStack, packedLight, blockPos, blockEntity);
        } else {
            MainRegistry.LOGGER.warn(
                "Failed to create renderer for part {}, model has no renderable quads", 
                partName
            );
        }
        
        poseStack.popPose();
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