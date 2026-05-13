package com.hbm_m.client.overlay;


import com.hbm_m.client.model.variant.DoorModelRegistry;
import com.hbm_m.client.model.variant.DoorModelSelection;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
*///?}

/**
 * Рендерит 3D модель двери с выбранным скином в GUI.
 *
 * ItemOverrides.EMPTY в 1.20+: кастомные overrides недоступны,
 * поэтому NBT-based подмена модели не работает. Вместо этого
 * разрешаем BakedModel из реестра напрямую и рендерим через
 * ItemRenderer.render() с подменённой моделью.
 */
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public final class DoorModelFakeItemRenderer {

    private static final float ICON_SCALE = 0.9f;

    private DoorModelFakeItemRenderer() {}

    /**
     * Рендерит 3D модель двери с указанным выбором (legacy/modern+skin) в слот GUI.
     *
     * @param guiGraphics контекст рендеринга
     * @param selection   выбор модели и скина
     * @param doorId      ID двери (round_airlock_door и т.д.)
     * @param doorStack   ItemStack двери (базовый стек для fallback)
     * @param x           X координата слота
     * @param y           Y координата слота
     * @param size        размер слота (не используется)
     */
    public static void renderDoorModel(net.minecraft.client.gui.GuiGraphics guiGraphics, DoorModelSelection selection,
                                       String doorId, ItemStack doorStack, int x, int y, int size) {
        Minecraft mc = Minecraft.getInstance();
        DoorModelRegistry registry = DoorModelRegistry.getInstance();

        BakedModel modelToRender = null;

        if (registry.isRegistered(doorId)) {
            ResourceLocation modelPath = registry.getModelPath(doorId, selection);
            if (modelPath != null) {
                BakedModel candidate = mc.getModelManager().getModel(modelPath);
                if (candidate != null && candidate != mc.getModelManager().getMissingModel()) {
                    modelToRender = candidate;
                }
            }
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x + 8, y + 8, 0);
        guiGraphics.pose().scale(ICON_SCALE, ICON_SCALE, 1f);
        guiGraphics.pose().translate(-8, -8, 0);

        if (modelToRender != null) {
            renderModelAsItem(guiGraphics, doorStack, modelToRender);
        } else {
            guiGraphics.renderFakeItem(doorStack, 0, 0);
        }

        guiGraphics.pose().popPose();
    }

    /**
     * Рендерит произвольную BakedModel как item в позиции (0,0) текущего PoseStack.
     * Повторяет логику GuiGraphics.renderItem() но с подменённой моделью.
     */
    private static void renderModelAsItem(net.minecraft.client.gui.GuiGraphics guiGraphics,
                                           ItemStack stack, BakedModel model) {
        Minecraft mc = Minecraft.getInstance();
        PoseStack pose = guiGraphics.pose();
        MultiBufferSource.BufferSource bufferSource = guiGraphics.bufferSource();

        pose.pushPose();
        pose.translate(8.0f, 8.0f, 150.0f);
        pose.mulPoseMatrix(new org.joml.Matrix4f().scaling(1.0f, -1.0f, 1.0f));
        pose.scale(16.0f, 16.0f, 16.0f);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        boolean useFlatLight = !model.usesBlockLight();
        if (useFlatLight) {
            com.mojang.blaze3d.platform.Lighting.setupForFlatItems();
        }

        mc.getItemRenderer().render(
                stack,
                ItemDisplayContext.GUI,
                false,
                pose,
                bufferSource,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                model
        );
        bufferSource.endBatch();

        if (useFlatLight) {
            com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
        }

        pose.popPose();
    }
}
