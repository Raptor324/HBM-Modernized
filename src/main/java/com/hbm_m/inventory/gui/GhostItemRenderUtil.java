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

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
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
        //? if < 1.21.1 {
        pose.mulPoseMatrix(new org.joml.Matrix4f().scaling(1.0F, -1.0F, 1.0F));
        //?} else {
        /*pose.mulPose(new org.joml.Matrix4f().scaling(1.0F, -1.0F, 1.0F));
        *///?}
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

            //? if < 1.21.1 {
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

            //? if forge {
            @Override
            public void putBulkData(PoseStack.Pose matrix, BakedQuad quad, float r, float g, float b, float a, int light, int overlay, boolean hasAmbientOcclusion) {
                inner.putBulkData(matrix, quad, r, g, b, a * alpha, light, overlay, hasAmbientOcclusion);
            }

            @Override
            public void putBulkData(PoseStack.Pose matrix, BakedQuad quad, float r, float g, float b, int light, int overlay) {
                inner.putBulkData(matrix, quad, r * alpha, g * alpha, b * alpha, light, overlay);
            }
            //?}

            //? if fabric {
            /*public void putBulkData(PoseStack.Pose matrix, BakedQuad quad, float r, float g, float b, int light, int overlay) {
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
        //?} else {
            /*@Override
            public VertexConsumer addVertex(float x, float y, float z) {
                return inner.addVertex(x, y, z);
            }

            @Override
            public VertexConsumer setColor(int red, int green, int blue, int a) {
                return inner.setColor(red, green, blue, (int) (a * alpha));
            }

            @Override
            public VertexConsumer setUv(float u, float v) {
                return inner.setUv(u, v);
            }

            @Override
            public VertexConsumer setUv1(int u, int v) {
                return inner.setUv1(u, v);
            }

            @Override
            public VertexConsumer setUv2(int u, int v) {
                return inner.setUv2(u, v);
            }

            @Override
            public VertexConsumer setNormal(float x, float y, float z) {
                return inner.setNormal(x, y, z);
            }

            @Override
            public void putBulkData(PoseStack.Pose matrix, BakedQuad quad,
                                    float r, float g, float b, float a,
                                    int light, int overlay, boolean readExistingColor) {
                inner.putBulkData(matrix, quad, r, g, b, a * alpha, light, overlay, readExistingColor);
            }
        };
        *///?}
    }
}
