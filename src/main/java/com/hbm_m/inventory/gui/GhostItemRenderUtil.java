package com.hbm_m.inventory.gui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Полупрозрачный рендер «призрачных» предметов-рецептов в GUI.
 *
 * ВАЖНО: vanilla {@code ItemRenderer} рисует baked-квады через
 * {@code VertexConsumer#putBulkData(PoseStack.Pose, BakedQuad, float r, float g,
 * float b, int light, int overlay[, float normalX, ...])}, который цвет кладёт
 * напрямую в вершины, минуя вызовы {@code VertexConsumer#color(int,int,int,int)}.
 * Поэтому обёртка, которая просто фильтрует {@code color()}, не влияет на
 * полупрозрачность блоков. Здесь мы оборачиваем каждый {@code VertexConsumer}
 * буфера и перехватываем {@code putBulkData}, умножая альфу в параметре {@code a},
 * плюс фильтруем {@code defaultColor} для возможных non-quad путей рендера.
 */
public final class GhostItemRenderUtil {

    private GhostItemRenderUtil() {
    }

    /**
     * Рисует стек полупрозрачным в координатах экрана (аналог renderItem).
     * Позиционирование повторяет vanilla GuiGraphics.renderItem:
     * translate(x+8, y+8, 150), flip Y, scale 16.
     *
     * @param alpha коэффициент прозрачности 0..1
     */
    public static void renderTranslucent(GuiGraphics guiGraphics, ItemStack stack,
                                         int x, int y, float alpha) {
        Minecraft mc = Minecraft.getInstance();
        BakedModel model = mc.getItemRenderer().getModel(stack, mc.level, null, 0);

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(x + 8.0F, y + 8.0F, 150.0F);
        pose.mulPoseMatrix(new org.joml.Matrix4f().scaling(1.0F, -1.0F, 1.0F));
        pose.scale(16.0F, 16.0F, 16.0F);

        MultiBufferSource tintedSource = tintBufferSource(guiGraphics.bufferSource(), alpha);

        boolean flatLight = !model.usesBlockLight();
        if (flatLight) {
            Lighting.setupForFlatItems();
        }

        mc.getItemRenderer().render(
                stack,
                ItemDisplayContext.GUI,
                false,
                pose,
                tintedSource,
                net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                model);
        // Смываем, чтобы призрак был записан до последующих декораций.
        guiGraphics.bufferSource().endBatch();

        pose.popPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Оборачивает MultiBufferSource: все вершины, записанные через вернувшиеся
     * потребители, получают альфу, умноженную на {@code alpha}.
     */
    private static MultiBufferSource tintBufferSource(MultiBufferSource delegate, float alpha) {
        return type -> tintVertexConsumer(delegate.getBuffer(type), alpha);
    }

    private static VertexConsumer tintVertexConsumer(VertexConsumer inner, float alpha) {
        // VertexConsumer временно хранит «defaultColor» между концами вершин;
        // хранение альфы в состоянии обёртки нужно, чтобы при verybadly вложенных
        // begin/end мы не потеряли исходное значение. Однако проще умножать alpha
        // на лету в putBulkData и defaultColor.
        return new VertexConsumer() {

            // ─── потоковые вызовы: просто фильтруем ───
            @Override
            public VertexConsumer vertex(double x, double y, double z) {
                return inner.vertex(x, y, z);
            }

            @Override
            public VertexConsumer color(int red, int green, int blue, int a) {
                return inner.color(red, green, blue, (int) (a * alpha));
            }

            @Override
            public VertexConsumer uv(float u, float v) {
                return inner.uv(u, v);
            }

            @Override
            public VertexConsumer overlayCoords(int u, int v) {
                return inner.overlayCoords(u, v);
            }

            @Override
            public VertexConsumer uv2(int u, int v) {
                return inner.uv2(u, v);
            }

            @Override
            public VertexConsumer normal(float x, float y, float z) {
                return inner.normal(x, y, z);
            }

            @Override
            public void endVertex() {
                inner.endVertex();
            }

            // ─── критический путь для моделей ───

            /**
             * Vanilla ItemRenderer вызывает именно этот метод для передачи
             * baked-квада с цветом и светом в буфер. Мы умножаем {@code a}
             * таким образом, чтобы итоговая полупрозрачность применялась и
             * к 3D-блокам, и к OBJ-моделям (через кастомные беры).
             */
            //? if forge {
            @Override
            public void putBulkData(PoseStack.Pose matrix, BakedQuad quad,
                                    float r, float g, float b, float a,
                                    int light, int overlay, boolean hasAmbientOcclusion) {
                inner.putBulkData(matrix, quad, r, g, b, a * alpha, light, overlay, hasAmbientOcclusion);
            }

            // Упрощённый вариант без ambient occlusion. Alpha-канал здесь не
            // передаётся явно, ванильная реализация просто вызывает полную
            // сигнатуру с a=1.0F. Затемняем цвет, чтобы у полупрозрачной
            // текстуры slot-подложки призрак «темнел» (визуальный fallback,
            // так же поступают порты 1.7.10, где GL11.glColor4f влиял и на RGB).
            @Override
            public void putBulkData(PoseStack.Pose matrix, BakedQuad quad,
                                    float r, float g, float b,
                                    int light, int overlay) {
                inner.putBulkData(matrix, quad, r * alpha, g * alpha, b * alpha, light, overlay);
            }
            //?}

            //? if fabric {
            /*public void putBulkData(PoseStack.Pose matrix, BakedQuad quad,
                                    float r, float g, float b,
                                    int light, int overlay) {
                // Fabric 1.20.x поддерживает только этот вариант, поэтому
                // приходится разом затенять цвет — при альфе=0.5 фактычески
                // получается затемнение. Это acceptable fallback на Fabric,
                // где нет альфа-пути через putBulkData.
                inner.putBulkData(matrix, quad, r * alpha, g * alpha, b * alpha, light, overlay);
            }
            *///?}

            // ─── дефолтный цвет (для путей, использующих defaultColor) ───
            @Override
            public void defaultColor(int defaultR, int defaultG, int defaultB, int defaultA) {
                inner.defaultColor(defaultR, defaultG, defaultB, (int) (defaultA * alpha));
            }

            @Override
            public void unsetDefaultColor() {
                inner.unsetDefaultColor();
            }
        };
    }
}
