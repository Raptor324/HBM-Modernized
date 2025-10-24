package com.hbm_m.client.model.render;

import com.hbm_m.client.model.MachineAdvancedAssemblerBakedModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class MachineAdvancedAssemblerVboRenderer {
    private static final String BASE = "Base";
    private static final String FRAME = "Frame";
    private static final String RING = "Ring";
    private static final String ARM_L1 = "ArmLower1";
    private static final String ARM_U1 = "ArmUpper1";
    private static final String HEAD_1 = "Head1";
    private static final String SPK_1 = "Spike1";
    private static final String ARM_L2 = "ArmLower2";
    private static final String ARM_U2 = "ArmUpper2";
    private static final String HEAD_2 = "Head2";
    private static final String SPK_2 = "Spike2";

    // Глобальный кеш VBO по частям, чтобы не дублировать GPU буферы

    private final MachineAdvancedAssemblerBakedModel model;

    public MachineAdvancedAssemblerVboRenderer(MachineAdvancedAssemblerBakedModel model) {
        this.model = model;
    }

    public void renderStaticBase(PoseStack poseStack, int packedLight, BlockPos blockPos) {
        BakedModel part = model.getPart(BASE);
        if (part != null) {
            GlobalMeshCache.getOrCreateRenderer("assembler_" + BASE, part).render(poseStack, packedLight, blockPos);
        }
    }

    public void renderStaticFrame(PoseStack poseStack, int packedLight, BlockPos blockPos) {
        BakedModel part = model.getPart(FRAME);
        if (part != null) {
            GlobalMeshCache.getOrCreateRenderer("assembler_" + FRAME, part).render(poseStack, packedLight, blockPos);
        }
    }

    public void renderPart(PoseStack poseStack, int packedLight, String partName, Matrix4f transform, BlockPos blockPos) {
        BakedModel part = model.getPart(partName);
        if (part != null) {
            poseStack.pushPose();
            poseStack.last().pose().mul(transform);
            GlobalMeshCache.getOrCreateRenderer("assembler_" + partName, part).render(poseStack, packedLight, blockPos);
            poseStack.popPose();
        }
    }

    public static void clearGlobalCache() {
        GlobalMeshCache.clearAll();
    }
}