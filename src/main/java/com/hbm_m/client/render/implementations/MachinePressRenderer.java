package com.hbm_m.client.render.implementations;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.hbm_m.blockentity.machines.MachinePressBlockEntity;
import com.hbm_m.client.model.PressBakedModel;
import com.hbm_m.client.render.AbstractPartBasedRenderer;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Пресс на фабрике {@link MachineRenderers}: Base — статическая часть, Head —
 * анимированная; штамп и заготовка — immediate-хук (предметы, не OBJ).
 * Геометрия берётся из multipart-модели blockstate (hbm_m:press_loader).
 */
public final class MachinePressRenderer {

    private static final String HEAD_PART = "Head";

    private static final float PIXEL = 1.0F / 16.0F;
    private static final float HEAD_SCALE = 0.983F;
    private static final float WORKPIECE_HEIGHT = 1.125F + PIXEL;
    private static final float WORKPIECE_SCALE = 0.55F;
    private static final float STAMP_SCALE = 0.65F;

    public static void register() {
        MachineRenderers.machine("press", com.hbm_m.blockentity.ModBlockEntities.PRESS_BE.get(),
                MachinePressBlockEntity.class)
            .part("Base")
            .part(HEAD_PART, MachinePressRenderer::animateHead)
            .hook(MachinePressRenderer::renderItems)
            .register();
    }

    private MachinePressRenderer() {}

    // ── Анимация головы: смещение по прогрессу прессования ─────────────

    private static boolean animateHead(MachinePressBlockEntity blockEntity, float partialTick,
                                       long gameTime, PoseStack pose) {
        PressBakedModel model = pressModel(blockEntity);
        if (model == null) return false;
        Vector3f rest = model.getHeadRestOffset();
        float travel = model.getHeadTravelDistance();
        float progress = blockEntity.getPressAnimationProgress(partialTick);
        float effectiveTravel = Math.max(0.0F, travel - PIXEL);
        float offsetY = rest.y() - (progress * effectiveTravel);
        pose.translate(rest.x(), offsetY, rest.z());
        pose.scale(HEAD_SCALE, HEAD_SCALE, HEAD_SCALE);
        return true;
    }

    private static @Nullable PressBakedModel pressModel(MachinePressBlockEntity blockEntity) {
        BakedModel raw = Minecraft.getInstance().getBlockRenderer()
                .getBlockModel(blockEntity.getBlockState());
        return AbstractPartBasedRenderer.unwrapFabricForwardingModels(raw) instanceof PressBakedModel p ? p : null;
    }

    // ── Хук: штамп на голове + заготовка на столе (предметы) ───────────

    private static void renderItems(MachinePressBlockEntity blockEntity, float partialTick,
                                    PoseStack poseStack, MultiBufferSource bufferSource,
                                    int packedLight, int packedOverlay, MachineRenderApi api) {
        renderWorkpiece(blockEntity, poseStack, bufferSource, packedLight, packedOverlay);

        Matrix4f headTransform = api.partTransform(HEAD_PART);
        renderStampItem(blockEntity, poseStack, bufferSource, packedLight, packedOverlay, headTransform);
    }

    private static void renderWorkpiece(MachinePressBlockEntity blockEntity, PoseStack poseStack,
                                        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ItemStack stack = blockEntity.getMaterialStack();
        if (stack.isEmpty()) {
            return;
        }

        var mc = Minecraft.getInstance();

        poseStack.pushPose();
        // Локальное (0, 0) здесь - центр блока после блочного трансформа
        poseStack.translate(0.32F, WORKPIECE_HEIGHT, 0.32F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));

        poseStack.pushPose();
        poseStack.scale(WORKPIECE_SCALE, WORKPIECE_SCALE, WORKPIECE_SCALE);
        poseStack.translate(-0.5F, -0.5F, 0.32F);

        mc.getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                packedLight,
                packedOverlay,
                poseStack,
                bufferSource,
                blockEntity.getLevel(),
                (int) blockEntity.getBlockPos().asLong()
        );

        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderStampItem(MachinePressBlockEntity blockEntity, PoseStack poseStack,
                                        MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                        @Nullable Matrix4f headTransform) {
        ItemStack stamp = blockEntity.getStampStack();
        if (stamp.isEmpty() || headTransform == null) {
            return;
        }

        var mc = Minecraft.getInstance();

        // Достаём только трансляцию головы
        Vector3f headPos = new Vector3f();
        headTransform.getTranslation(headPos);

        poseStack.pushPose();
        // Центр блока по X/Z, высота – как у головы, плюс небольшой отступ
        poseStack.translate(0.32F, headPos.y() + 0.98F, 0.32F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));

        poseStack.pushPose();
        poseStack.scale(STAMP_SCALE, STAMP_SCALE, STAMP_SCALE);
        poseStack.translate(-0.5F, -0.5F, 0.0F);

        mc.getItemRenderer().renderStatic(
                stamp,
                ItemDisplayContext.FIXED,
                packedLight,
                packedOverlay,
                poseStack,
                bufferSource,
                blockEntity.getLevel(),
                (int) blockEntity.getBlockPos().asLong()
        );

        poseStack.popPose();
        poseStack.popPose();
    }
}
