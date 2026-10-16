package com.hbm_m.platform;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.block.model.BakedQuad;
import org.joml.Matrix4f;

/**
 * Платформенный и версионный слой для сглаживания различий рендеринга 
 * (VertexConsumer, BufferBuilder) между 1.20.1 (Forge/Fabric) и 1.21.1+ (NeoForge).
 */
public final class RenderHooks {
    private RenderHooks() {}

    /**
     * Начинает построение буфера.
     */
    public static BufferBuilder beginTesselator(Tesselator tesselator, VertexFormat.Mode mode, VertexFormat format) {
        //? if < 1.21.1 {
        BufferBuilder builder = tesselator.getBuilder();
        builder.begin(mode, format);
        return builder;
        //?} else {
        /*return tesselator.begin(mode, format);
        *///?}
    }

    /**
     * Завершает построение и отрисовывает буфер через шейдер.
     */
    public static void drawWithShader(BufferBuilder buffer) {
        //? if < 1.21.1 {
        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(buffer.end());
        //?} else {
        /*com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(buffer.buildOrThrow());
        *///?}
    }

    /**
     * Кросс-версионное создание BufferSource (immediate).
     */
    public static net.minecraft.client.renderer.MultiBufferSource.BufferSource immediateBufferSource(int capacity) {
        //? if < 1.21.1 {
        return net.minecraft.client.renderer.MultiBufferSource.immediate(new com.mojang.blaze3d.vertex.BufferBuilder(capacity));
        //?} else {
        /*return net.minecraft.client.renderer.MultiBufferSource.immediate(new com.mojang.blaze3d.vertex.ByteBufferBuilder(capacity));
        *///?}
    }

    /**
     * Добавляет вершину с позицией, текстурными координатами и цветом (замена длинных чейнов).
     */
    public static void vertexTexColor(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z, float u, float v, int r, int g, int b, int a) {
        //? if < 1.21.1 {
        consumer.vertex(matrix, x, y, z).uv(u, v).color(r, g, b, a).endVertex();
        //?} else {
        /*consumer.addVertex(matrix, x, y, z).setUv(u, v).setColor(r, g, b, a);
        *///?}
    }

    /**
     * Добавляет вершину с позицией и цветом.
     */
    public static void vertexColor(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z, int r, int g, int b, int a) {
        //? if < 1.21.1 {
        consumer.vertex(matrix, x, y, z).color(r, g, b, a).endVertex();
        //?} else {
        /*consumer.addVertex(matrix, x, y, z).setColor(r, g, b, a);
        *///?}
    }

    /**
     * Добавляет вершину с позицией и цветом (без матрицы).
     */
    public static void vertexColor(VertexConsumer consumer, double x, double y, double z, int r, int g, int b, int a) {
        //? if < 1.21.1 {
        consumer.vertex(x, y, z).color(r, g, b, a).endVertex();
        //?} else {
        /*consumer.addVertex((float) x, (float) y, (float) z).setColor(r, g, b, a);
        *///?}
    }

    /**
     * Полноценная вершина: позиция, цвет, текстура, оверлей, свет, нормаль.
     */
    public static void vertexFull(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z,
                                  int r, int g, int b, int a,
                                  float u, float v,
                                  int packedOverlay, int packedLight,
                                  float nx, float ny, float nz) {
        //? if < 1.21.1 {
        consumer.vertex(matrix, x, y, z).color(r, g, b, a).uv(u, v).overlayCoords(packedOverlay).uv2(packedLight).normal(nx, ny, nz).endVertex();
        //?} else {
        /*consumer.addVertex(matrix, x, y, z).setColor(r, g, b, a).setUv(u, v).setOverlay(packedOverlay).setLight(packedLight).setNormal(nx, ny, nz);
        *///?}
    }

    /**
     * Кросс-версионная обёртка для putBulkData.
     * 1.20.1 Forge / 1.21.1+: 9-аргументный вызов (в ванилу 1.21.1 перенесли Forge-сигнатуру).
     * 1.20.1 Fabric: 7-аргументный вызов.
     */
    public static void putBulkData(VertexConsumer consumer, PoseStack.Pose matrix, BakedQuad quad,
                                   float r, float g, float b, float a, int packedLight, int packedOverlay, boolean readExistingColor) {
        //? if < 1.21.1 && fabric {
        /*consumer.putBulkData(matrix, quad, r, g, b, packedLight, packedOverlay);
        *///?} elif < 1.21.1 && forge {
        consumer.putBulkData(matrix, quad, r, g, b, a, packedLight, packedOverlay, readExistingColor);
        //?} else {
        /*// 1.21.1 (vanilla/neoforge/fabric): 8-arg сигнатура (с alpha, без readExistingColor).
        consumer.putBulkData(matrix, quad, r, g, b, a, packedLight, packedOverlay);
        *///?}
    }

    // =====================================================================================
    //  VertexFormat & VertexFormatElement Hooks
    // =====================================================================================

    public static java.util.List<VertexFormatElement> getElements(VertexFormat format) {
        // Обе версии (1.20.1 и 1.21.1) используют get-prefix; gating не нужен.
        return format.getElements();
    }

    public static int getVertexSize(VertexFormat format) {
        // Обе версии (1.20.1 и 1.21.1) используют get-prefix; gating не нужен.
        return format.getVertexSize();
    }

    public static int getGlType(VertexFormatElement element) {
        //? if < 1.21.1 {
        return element.getType().getGlType();
        //?} else {
        /*return element.type().glType();
        *///?}
    }

    public static int getCount(VertexFormatElement element) {
        //? if < 1.21.1 {
        return element.getCount();
        //?} else {
        /*return element.count();
        *///?}
    }

    public static VertexFormatElement.Usage getUsage(VertexFormatElement element) {
        //? if < 1.21.1 {
        return element.getUsage();
        //?} else {
        /*return element.usage();
        *///?}
    }

    public static int getIndex(VertexFormatElement element) {
        //? if < 1.21.1 {
        return element.getIndex();
        //?} else {
        /*return element.index();
        *///?}
    }

     public static int getByteSize(VertexFormatElement element) {
        //? if < 1.21.1 {
        return element.getByteSize();
        //?} else {
        /*return element.byteSize();
        *///?}
    }

    /**
     * Кросс-платформенное получение квадов из части BakedModel.
     * Forge/NeoForge: 5-аргументный вызов (ModelData.EMPTY + RenderType.solid()).
     * Fabric: ванильный 3-аргументный вызов.
     */
    public static java.util.List<BakedQuad> getPartQuads(net.minecraft.client.resources.model.BakedModel model,
                                                         net.minecraft.world.level.block.state.BlockState state,
                                                         net.minecraft.core.Direction side,
                                                         net.minecraft.util.RandomSource rand) {
        //? if forge {
        return model.getQuads(state, side, rand,
                net.minecraftforge.client.model.data.ModelData.EMPTY,
                net.minecraft.client.renderer.RenderType.solid());
        //?} elif neoforge {
        /*return model.getQuads(state, side, rand,
                net.neoforged.neoforge.client.model.data.ModelData.EMPTY,
                net.minecraft.client.renderer.RenderType.solid());
        *///?} else {
        /*return model.getQuads(state, side, rand);
        *///?}
    }

    /**
     * Кросс-версионная вершина для частиц (PARTICLE format).
     */
    public static void particleVertex(VertexConsumer consumer, float x, float y, float z, float u, float v, int r, int g, int b, int a, int packedLight) {
        //? if < 1.21.1 {
        consumer.vertex(x, y, z).uv(u, v).color(r, g, b, a).uv2(packedLight).endVertex();
        //?} else {
        /*consumer.addVertex(x, y, z).setUv(u, v).setColor(r, g, b, a).setLight(packedLight);
        *///?}
    }

    // =====================================================================================
    //  RenderSystem ModelView (AFTER_WEATHER-проход дальнего контента)
    // =====================================================================================

    /**
     * Гарантированно выставляет RenderSystem ModelViewMat в матрицу поворота
     * камеры уровня (frustumMatrix) на время кастомного прохода рендера.
     *
     * ЗАЧЕМ: ваниль пушит frustumMatrix в modelViewStack на время renderLevel,
     * но к моменту наших проходов (AFTER_WEATHER + флаш батчей) состояние
     * может быть загрязнено чужими хуками (DH/Iris и т.п.). Если там окажется
     * identity, camera-relative вершины рисуются «зеркально» движению камеры
     * («гриб улетает при движении игрока»). Метод ЗАМЕНЯЕТ вершину стека
     * переданной матрицей (не умножает!) — поведение детерминировано на обеих
     * версиях; если ваниль уже пушила ту же матрицу, это no-op.
     *
     * 1.20.1: стек — PoseStack (pushPose/setIdentity/mulPoseMatrix);
     * 1.21.1: стек — org.joml.Matrix4fStack (pushMatrix/identity/mul).
     */
    public static void pushLevelModelView(Matrix4f levelRotation) {
        //? if < 1.21.1 {
        CURRENT_LEVEL_ROTATION.set(new Matrix4f(levelRotation));
        PoseStack stack = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
        stack.pushPose();
        stack.setIdentity();
        stack.mulPoseMatrix(levelRotation);
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
        //?} else {
        /*org.joml.Matrix4fStack stack = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
        CURRENT_LEVEL_ROTATION.set(new org.joml.Matrix4f(levelRotation));
        stack.pushMatrix();
        stack.identity();
        stack.mul(levelRotation);
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
        *///?}
    }

    /**
     * Копия матрицы поворота камеры уровня, переданная в последний
     * {@link #pushLevelModelView} на этом потоке ({@code null} вне окна пуша).
     *
     * ЗАЧЕМ НУЖНА ОТДЕЛЬНО: под Oculus (даже с выключенным шейдерпаком) ambient
     * {@code RenderSystem.ModelViewMat} внутри нашего окна AFTER_WEATHER бывает
     * перезаписан в identity чужим bookkeeping'ом (диагностика «vbo.mvm»:
     * rsMV=identity в кадрах с мешем). Рендеры, которым критичен поворот
     * камеры (меш ракет), должны брать его отсюда — детерминированно.
     */
    private static final ThreadLocal<Matrix4f> CURRENT_LEVEL_ROTATION = new ThreadLocal<>();

    /** Повтор последнего {@link #pushLevelModelView}; null вне окна. */
    @org.jetbrains.annotations.Nullable
    public static Matrix4f currentLevelRotation() {
        return CURRENT_LEVEL_ROTATION.get();
    }

    /** Восстанавливает ModelViewMat после pushLevelModelView. */
    public static void popLevelModelView() {
        CURRENT_LEVEL_ROTATION.remove();
        //? if < 1.21.1 {
        PoseStack stack = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
        stack.popPose();
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
        //?} else {
        /*org.joml.Matrix4fStack stack = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
        stack.popMatrix();
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
        *///?}
    }
}