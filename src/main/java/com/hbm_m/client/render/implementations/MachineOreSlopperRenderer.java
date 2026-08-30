package com.hbm_m.client.render.implementations;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineOreSlopperBlockEntity;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.PlatformHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Сортировщик руды на фабрике {@link MachineRenderers}: Fan — вращение по X,
 * BladesLeft/BladesRight — противофазное вращение по Z. Геометрия —
 * injected-модели (не multipart); блочного трансформа нет (легаси рисовал
 * в сырых координатах блока).
 */
public final class MachineOreSlopperRenderer {

    private static final ResourceLocation FAN_MODEL_ID = id("ore_slopper_fan");
    private static final ResourceLocation BLADES_LEFT_MODEL_ID = id("ore_slopper_blades_left");
    private static final ResourceLocation BLADES_RIGHT_MODEL_ID = id("ore_slopper_blades_right");

    // Pivots 1:1 из RenderOreSlopper (OBJ-пространство) + 0.5 компенсация baked-сдвига.
    private static final double BLADES_LEFT_PIVOT_X = 0.375 + 0.5;
    private static final double BLADES_RIGHT_PIVOT_X = -0.375 + 0.5;
    private static final double BLADES_PIVOT_Y = 2.75;
    private static final double FAN_PIVOT_Y = 1.875;
    private static final double FAN_PIVOT_Z = -1.0 + 0.5;

    public static void register() {
        MachineRenderers.machine("oreslopper", ModBlockEntities.ORE_SLOPPER_BE.get(),
                MachineOreSlopperBlockEntity.class)
            .dynamicPart("BladesLeft", MachineOreSlopperRenderer::animateBladesLeft,
                    be -> partQuads("ore_slopper/blades_left", BLADES_LEFT_MODEL_ID), be -> "bl")
            .dynamicPart("BladesRight", MachineOreSlopperRenderer::animateBladesRight,
                    be -> partQuads("ore_slopper/blades_right", BLADES_RIGHT_MODEL_ID), be -> "br")
            .dynamicPart("Fan", MachineOreSlopperRenderer::animateFan,
                    be -> partQuads("ore_slopper/fan", FAN_MODEL_ID), be -> "fan")
            .blockTransform((be, animator) -> { /* легаси рисовал в сырых координатах */ })
            .register();
    }

    private MachineOreSlopperRenderer() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/" + path);
    }

    private static List<net.minecraft.client.renderer.block.model.BakedQuad> partQuads(
            String cacheKey, ResourceLocation modelId) {
        var modelManager = Minecraft.getInstance().getModelManager();
        BakedModel model = PlatformHooks.getModel(modelManager, modelId);
        if (model == null || model == modelManager.getMissingModel()) return List.of();
        return MeshRenderCache.getOrCompilePartGeometry(cacheKey, model).solidQuads();
    }

    private static boolean animateBladesLeft(MachineOreSlopperBlockEntity be, float partialTick,
                                             long gameTime, PoseStack pose) {
        float blades = Mth.lerp(partialTick, be.prevBladesRotation, be.bladesRotation);
        pose.translate(BLADES_LEFT_PIVOT_X, BLADES_PIVOT_Y, 0.5);
        pose.mulPose(Axis.ZP.rotationDegrees(blades));
        pose.translate(-BLADES_LEFT_PIVOT_X, -BLADES_PIVOT_Y, -0.5);
        return true;
    }

    private static boolean animateBladesRight(MachineOreSlopperBlockEntity be, float partialTick,
                                              long gameTime, PoseStack pose) {
        float blades = Mth.lerp(partialTick, be.prevBladesRotation, be.bladesRotation);
        pose.translate(BLADES_RIGHT_PIVOT_X, BLADES_PIVOT_Y, 0.5);
        pose.mulPose(Axis.ZP.rotationDegrees(-blades));
        pose.translate(-BLADES_RIGHT_PIVOT_X, -BLADES_PIVOT_Y, -0.5);
        return true;
    }

    private static boolean animateFan(MachineOreSlopperBlockEntity be, float partialTick,
                                      long gameTime, PoseStack pose) {
        float fan = Mth.lerp(partialTick, be.prevFanRotation, be.fanRotation);
        pose.translate(0.5, FAN_PIVOT_Y, FAN_PIVOT_Z);
        pose.mulPose(Axis.XP.rotationDegrees(-fan));
        pose.translate(-0.5, -FAN_PIVOT_Y, -FAN_PIVOT_Z);
        return true;
    }
}
