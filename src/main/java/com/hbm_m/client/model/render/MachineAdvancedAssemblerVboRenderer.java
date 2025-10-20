package com.hbm_m.client.model.render;

import com.hbm_m.client.model.MachineAdvancedAssemblerBakedModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.BakedModel;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    private static final Map<BakedModel, PartVbo> GLOBAL_VBO_CACHE = new ConcurrentHashMap<>();
    private final Map<String, PartVbo> parts = new HashMap<>();

    public MachineAdvancedAssemblerVboRenderer(MachineAdvancedAssemblerBakedModel model) {
        put(model, BASE);
        put(model, FRAME);
        put(model, RING);
        put(model, ARM_L1);
        put(model, ARM_U1);
        put(model, HEAD_1);
        put(model, SPK_1);
        put(model, ARM_L2);
        put(model, ARM_U2);
        put(model, HEAD_2);
        put(model, SPK_2);
    }

    private void put(MachineAdvancedAssemblerBakedModel model, String name) {
        BakedModel m = model.getPart(name);
        if (m != null) {
            parts.put(name, GLOBAL_VBO_CACHE.computeIfAbsent(m, PartVbo::new));
        }
    }

    public void renderStaticBase(PoseStack poseStack, int packedLight) {
        renderPart(poseStack, packedLight, BASE, null);
    }

    public void renderStaticFrame(PoseStack poseStack, int packedLight) {
        renderPart(poseStack, packedLight, FRAME, null);
    }

    public void renderPart(PoseStack poseStack, int packedLight, String part, Matrix4f localTransform) {
        PartVbo vbo = parts.get(part);
        if (vbo == null) return;

        poseStack.pushPose();
        if (localTransform != null) {
            poseStack.last().pose().mul(localTransform);
        }
        vbo.render(poseStack, packedLight);
        poseStack.popPose();
    }

    public static void clearGlobalCache() {
        GLOBAL_VBO_CACHE.values().forEach(PartVbo::cleanup);
        GLOBAL_VBO_CACHE.clear();
    }

    private static class PartVbo extends AbstractGpuVboRenderer {
        private final BakedModel modelPart;
        PartVbo(BakedModel modelPart) { this.modelPart = modelPart; }

        @Override
        protected VboData buildVboData() {
            return ObjModelVboBuilder.buildSinglePart(modelPart);
        }
    }
}
