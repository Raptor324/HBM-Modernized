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
import com.hbm_m.platform.LoaderHooks;
import com.hbm_m.client.render.machine.MachineRenderApi;

/**
 * Ore slopper on the {@link MachineRenderers} factory: Fan rotates around X,
 * BladesLeft/BladesRight rotate around Z in counter-phase. Geometry comes from
 * injected models (not multipart); no block transform is applied (the legacy
 * renderer drew in raw block coordinates).
 */
public final class MachineOreSlopperRenderer {

    private static final ResourceLocation FAN_MODEL_ID = id("ore_slopper_fan");
    private static final ResourceLocation BLADES_LEFT_MODEL_ID = id("ore_slopper_blades_left");
    private static final ResourceLocation BLADES_RIGHT_MODEL_ID = id("ore_slopper_blades_right");

    // Pivots copied 1:1 from RenderOreSlopper (OBJ space) plus 0.5 to compensate the baked offset.
    private static final double BLADES_LEFT_PIVOT_X = 0.375 + 0.5;
    private static final double BLADES_RIGHT_PIVOT_X = -0.375 + 0.5;
    private static final double BLADES_PIVOT_Y = 2.75;
    private static final double FAN_PIVOT_Y = 1.875;
    private static final double FAN_PIVOT_Z = -1.0 + 0.5;

    public static void register() {
        MachineRenderers.machine("oreslopper", ModBlockEntities.ORE_SLOPPER_BE.get(),
                MachineOreSlopperBlockEntity.class)
            // Статичный корпус из multipart-модели; подвижные части — dynamic parts
            // из отдельных моделей (как в legacy-рендере).
            // FFA merge loss restore: Slider/Hydraulics/Bucket снова анимированы (1:1 RenderOreSlopper),
            // плюс парящий кусок руды в ковше при LIFTING (hook).
            .part("Base")
            .part("Slider", MachineOreSlopperRenderer::animateSlider)
            .part("Hydraulics", MachineOreSlopperRenderer::animateHydraulics)
            .part("Bucket", MachineOreSlopperRenderer::animateBucket)
            .dynamicPart("BladesLeft", MachineOreSlopperRenderer::animateBladesLeft,
                    be -> partQuads("ore_slopper/blades_left", BLADES_LEFT_MODEL_ID), be -> "bl")
            .dynamicPart("BladesRight", MachineOreSlopperRenderer::animateBladesRight,
                    be -> partQuads("ore_slopper/blades_right", BLADES_RIGHT_MODEL_ID), be -> "br")
            .dynamicPart("Fan", MachineOreSlopperRenderer::animateFan,
                    be -> partQuads("ore_slopper/fan", FAN_MODEL_ID), be -> "fan")
            .blockTransform((be, animator) -> { /* legacy renderer drew in raw block coordinates */ })
            .hook(MachineOreSlopperRenderer::renderOreChunk)
            .register();
    }

    private MachineOreSlopperRenderer() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/" + path);
    }

    private static List<net.minecraft.client.renderer.block.model.BakedQuad> partQuads(
            String cacheKey, ResourceLocation modelId) {
        var modelManager = Minecraft.getInstance().getModelManager();
        BakedModel model = LoaderHooks.getModel(modelManager, modelId);
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

    // --- FFA merge loss restore: 1:1 RenderOreSlopper анимации ковша и кусок руды ---

    private static float sliderPos(MachineOreSlopperBlockEntity be, float pt) {
        return Mth.lerp(pt, be.prevSlider, be.slider);
    }

    private static float bucketExt(MachineOreSlopperBlockEntity be, float pt) {
        return (Mth.lerp(pt, be.prevBucket, be.bucket)) * 1.5F;
    }

    /** Слайд едет на 3 блока вперёд по мере цикла. */
    private static boolean animateSlider(MachineOreSlopperBlockEntity be, float pt, long gt, PoseStack pose) {
        pose.translate(0, 0, sliderPos(be, pt) * -3);
        return true;
    }

    /** Гидравлика опускается до 1.25 (с зазором 0.25), поверх трансформа слайда. */
    private static boolean animateHydraulics(MachineOreSlopperBlockEntity be, float pt, long gt, PoseStack pose) {
        pose.translate(0, 0, sliderPos(be, pt) * -3);
        pose.translate(0, -Mth.clamp(bucketExt(be, pt) - 0.25F, 0, 1.25F), 0);
        return true;
    }

    /** Ковш опускается до 1.25, поверх трансформа гидравлики. */
    private static boolean animateBucket(MachineOreSlopperBlockEntity be, float pt, long gt, PoseStack pose) {
        pose.translate(0, 0, sliderPos(be, pt) * -3);
        pose.translate(0, -Mth.clamp(bucketExt(be, pt) - 0.25F, 0, 1.25F), 0);
        pose.translate(0, -Mth.clamp(bucketExt(be, pt), 0, 1.25F), 0);
        return true;
    }

    /**
     * 1:1 RenderOreSlopper: во время LIFTING в ковше висит кусок бедроковой руды
     * (оригинал: ItemStack(ModItems.bedrock_ore, 1, 0) = BASE/LIGHT).
     */
    private static void renderOreChunk(MachineOreSlopperBlockEntity be, float pt,
                                       PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buf,
                                       int packedLight, int packedOverlay, MachineRenderApi api) {
        if (be.animation != MachineOreSlopperBlockEntity.SlopperAnimation.LIFTING) return;
        pose.pushPose();
        pose.translate(0, 0, sliderPos(be, pt) * -3);
        pose.translate(0, -Mth.clamp(bucketExt(be, pt) - 0.25F, 0, 1.25F), 0);
        pose.translate(0, -Mth.clamp(bucketExt(be, pt), 0, 1.25F), 0);
        pose.translate(0.0625D, 4.3125D, 2D);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.XP.rotationDegrees(-90));
        pose.scale(1.75F, 1.75F, 1.75F);
        Minecraft.getInstance().getItemRenderer().renderStatic(new net.minecraft.world.item.ItemStack(com.hbm_m.item.ModItems.BEDROCK_ORE_BASE_LIGHT.get()),
                net.minecraft.world.item.ItemDisplayContext.FIXED, packedLight, packedOverlay, pose, buf, be.getLevel(), 0);
        pose.popPose();
    }
}
