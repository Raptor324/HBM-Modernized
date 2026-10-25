package com.hbm_m.platform;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Matrix4f;

/**
 * Платформенный и версионный слой для сглаживания различий рендеринга
 * (VertexConsumer, BufferBuilder) между 1.20.1 (Forge) и 1.21.1+ (NeoForge).
 *
 * <p><b>CLIENT-ONLY:</b> дескрипторы методов ссылаются на client-only классы
 * (Minecraft, BakedModel, BufferSource) — класс запрещено загружать на
 * dedicated server (риск NoClassDefFoundError). Уведомления для общего кода —
 * в {@link CommonRenderHooks}.
 */
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class RenderHooks {
    private RenderHooks() {}

    /**
     * Начинает построение буфера.
     */
    /**
     * Scale for camera-facing text after {@code mulPose(cameraOrientation())}, as vanilla name tags
     * do it. 1.21 flipped the camera rotation convention: the X sign changed from -1 to +1, and with
     * the old sign the mirrored quad is back-face culled by RenderType.text* (CULL by default).
     */
    public static void scaleBillboardText(PoseStack poseStack, float scale) {
        //? if < 1.21.1 {
        poseStack.scale(-scale, -scale, scale);
        //?} else {
        /*poseStack.scale(scale, -scale, scale);
        *///?}
    }

    /**
     * Приводит PoseStack события RenderLevelStageEvent(AFTER_SKY) к небесному базису,
     * в котором ваниль рисует солнце/луну/звёзды.
     *
     * <p>1.20.1 Forge: poseStack события уже содержит поворот камеры (R_cam) — no-op.
     * <p>1.21.1 NeoForge: {@code dispatchRenderStage(AFTER_SKY, ..., poseStack=null, frustumMatrix, ...)}
     * отдаёт ПУСТУЮ identity-стопку ({@code getPoseStack() == new PoseStack()}), поворот камеры
     * живёт в {@code event.getModelViewMatrix()}. Ванильный {@code renderSky} 1.21.1 сам делает
     * {@code posestack.mulPose(frustumMatrix)}; здесь то же самое через Camera
     * ({@code frustumMatrix = rotation(rotation().conjugate())}). Без этого квады рисуются
     * прямо в view-space и «приклеиваются» к камере (звезда двигается вместе с камерой).
     */
    public static void applySkyViewBasis(PoseStack poseStack) {
        //? if >= 1.21.1 {
        /*poseStack.mulPose(net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera()
                .rotation().conjugate(new org.joml.Quaternionf()));
        *///?}
    }

    /**
     * Частичный тик «raw»-путей (руки/оружие, частицы, updateLightTexture, захваты кадра):
     * 1.20.1 {@code Minecraft.getFrameTime()}; 1.21.1 {@code getGameTimeDeltaPartialTick(true)} —
     * вариант, игнорирующий frozen-ветку DeltaTracker. Отличается от {@link #getPartialTick()}
     * только на 1.21.1: там BER-lerp-пути используют false-вариант.
     */
    public static float getPartialTickRaw() {
        //? if < 1.21.1 {
        return net.minecraft.client.Minecraft.getInstance().getFrameTime();
        //?} else {
        /*return net.minecraft.client.Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        *///?}
    }

    /**
     * putBulkData без alpha: 1.20.1 — 7-аргументная сигнатура; 1.21.1 — 8-аргументная
     * (alpha стала обязательной), передаём 1.0F — эквивалент старого умолчания.
     */
    public static void putBulkData(VertexConsumer consumer, PoseStack.Pose matrix, BakedQuad quad,
                                   float r, float g, float b, int packedLight, int packedOverlay) {
        //? if < 1.21.1 {
        consumer.putBulkData(matrix, quad, r, g, b, packedLight, packedOverlay);
        //?} else {
        /*consumer.putBulkData(matrix, quad, r, g, b, 1.0F, packedLight, packedOverlay);
        *///?}
    }

    /**
     * Полноценная вершина (см. {@link #vertexFull(VertexConsumer, Matrix4f, float, float, float, int, int, int, float, float, int, int, float, float, float)})
     * с float-цветом и нормалью, преобразуемой матрицей нормалей (1.20.1
     * {@code normal(Matrix3f, ...)}; 1.21.1 — ручное преобразование, см. {@link #normal}).
     */
    public static void vertexFull(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z,
                                  float r, float g, float b, float a,
                                  float u, float v,
                                  int packedOverlay, int packedLight,
                                  org.joml.Matrix3f n, float nx, float ny, float nz) {
        //? if < 1.21.1 {
        consumer.vertex(matrix, x, y, z).color(r, g, b, a).uv(u, v).overlayCoords(packedOverlay).uv2(packedLight).normal(n, nx, ny, nz).endVertex();
        //?} else {
        /*consumer.addVertex(matrix, x, y, z).setColor(r, g, b, a).setUv(u, v).setOverlay(packedOverlay).setLight(packedLight);
        org.joml.Vector3f t = n.transform(new org.joml.Vector3f(nx, ny, nz));
        consumer.setNormal(t.x(), t.y(), t.z());
        *///?}
    }

    /** Полноценная вершина с int-цветом (0..255) и нормалью, преобразуемой матрицей нормалей. */
    public static void vertexFull(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z,
                                  int r, int g, int b, int a,
                                  float u, float v,
                                  int packedOverlay, int packedLight,
                                  org.joml.Matrix3f n, float nx, float ny, float nz) {
        //? if < 1.21.1 {
        consumer.vertex(matrix, x, y, z).color(r, g, b, a).uv(u, v).overlayCoords(packedOverlay).uv2(packedLight).normal(n, nx, ny, nz).endVertex();
        //?} else {
        /*consumer.addVertex(matrix, x, y, z).setColor(r, g, b, a).setUv(u, v).setOverlay(packedOverlay).setLight(packedLight);
        org.joml.Vector3f t = n.transform(new org.joml.Vector3f(nx, ny, nz));
        consumer.setNormal(t.x(), t.y(), t.z());
        *///?}
    }

    /** Как {@link #vertexFull(VertexConsumer, Matrix4f, float, float, float, float, float, float, float, float, float, int, int, org.joml.Matrix3f, float, float, float)}, но нормаль без матрицы. */
    public static void vertexFull(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z,
                                  float r, float g, float b, float a,
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
     * {@code ModelPart.render} с цвет-перемножением: 1.20.1 — 8-аргументный float-вариант;
     * 1.21.1 — 5-аргументный с packed ARGB (квантование в 1/255 совпадает с остальным
     * 1.21.1-пайплайном; hurt-flash tint мобов сохраняется).
     */
    public static void renderPart(net.minecraft.client.model.geom.ModelPart part, PoseStack poseStack,
                                  VertexConsumer consumer, int packedLight, int packedOverlay,
                                  float r, float g, float b, float a) {
        //? if < 1.21.1 {
        part.render(poseStack, consumer, packedLight, packedOverlay, r, g, b, a);
        //?} else {
        /*int color = (int) (a * 255.0F) << 24 | (int) (r * 255.0F) << 16 | (int) (g * 255.0F) << 8 | (int) (b * 255.0F);
        part.render(poseStack, consumer, packedLight, packedOverlay, color);
        *///?}
    }

    /**
     * Окно рендера предмета в руке: на 1.21.1 ванилла ({@code GameRenderer.renderItemInHand}) кладёт
     * поворот камеры в RenderSystem model-view стек, а наш PoseStack уже содержит все трансформы —
     * на время отрисовки возвращаем model-view в identity, иначе вершины уходят в мировые оси и
     * оружие не следует за камерой. На 1.20.1 model-view в этой точке уже identity — no-op.
     */
    public static void pushHandModelView() {
        //? if >= 1.21.1 {
        /*org.joml.Matrix4fStack mvStack = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
        mvStack.pushMatrix();
        mvStack.identity();
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
        *///?}
    }

    /** Закрывает окно {@link #pushHandModelView()}. */
    public static void popHandModelView() {
        //? if >= 1.21.1 {
        /*org.joml.Matrix4fStack mvStack = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
        mvStack.popMatrix();
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
        *///?}
    }

    /** {@code PoseStack.mulPoseMatrix} (1.20.1) / {@code mulPose(Matrix4f)} (1.21.1). */
    public static void mulPoseMatrix(PoseStack poseStack, Matrix4f matrix) {
        //? if < 1.21.1 {
        poseStack.mulPoseMatrix(matrix);
        //?} else {
        /*poseStack.mulPose(matrix);
        *///?}
    }


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
     * Draws a quad in POSITION format using Tesselator and the active shader.
     * Собран из хелперов — без единого stonecutter-гейта.
     */
    public static void drawQuad(float x0, float y0, float x1, float y1, float z) {
        BufferBuilder bb = beginTesselator(Tesselator.getInstance(), VertexFormat.Mode.QUADS,
                com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION);
        vertexPos(bb, x0, y0, z);
        vertexPos(bb, x1, y0, z);
        vertexPos(bb, x1, y1, z);
        vertexPos(bb, x0, y1, z);
        drawWithShader(bb);
    }

    /**
     * Creates a PlainBufferSource for custom sortable / isolated render types.
     * Возвращаемый тип — {@link com.hbm_m.client.render.PlainBufferSource} (подкласс
     * BufferSource): конструктор {@code new PlainBufferSource(new BufferBuilder(...))}
     * версионно расходится (BufferBuilder vs ByteBufferBuilder) и должен идти через хук.
     */
    public static com.hbm_m.client.render.PlainBufferSource createPlainBufferSource(int bufferSize) {
        //? if < 1.21.1 {
        return new com.hbm_m.client.render.PlainBufferSource(
                new com.mojang.blaze3d.vertex.BufferBuilder(bufferSize));
        //?} else {
        /*return new com.hbm_m.client.render.PlainBufferSource(
                new com.mojang.blaze3d.vertex.ByteBufferBuilder(bufferSize));
        *///?}
    }

    /**
     * Cross-version vertex format for textured colored geometry without lightmap:
     * POSITION_COLOR_TEX on 1.20.1 vs POSITION_TEX_COLOR on 1.21.1.
     */
    public static VertexFormat positionColorTexFormat() {
        //? if < 1.21.1 {
        return com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR_TEX;
        //?} else {
        /*return com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX_COLOR;
        *///?}
    }

    private static final class ShardAccess extends net.minecraft.client.renderer.RenderStateShard {
        private ShardAccess(String name, Runnable setupState, Runnable clearState) {
            super(name, setupState, clearState);
        }
        //? if < 1.21.1 {
        static final ShaderStateShard POSITION_COLOR_TEX = POSITION_COLOR_TEX_SHADER;
        //?} else {
        /*static final ShaderStateShard TEXT_BACKGROUND = RENDERTYPE_TEXT_BACKGROUND_SHADER;
        *///?}
    }

    /**
     * Cross-version shader shard for textured colored geometry without lightmap:
     * POSITION_COLOR_TEX_SHADER on 1.20.1 vs RENDERTYPE_TEXT_BACKGROUND_SHADER on 1.21.1.
     */
    public static net.minecraft.client.renderer.RenderStateShard.ShaderStateShard positionColorTexShader() {
        //? if < 1.21.1 {
        return ShardAccess.POSITION_COLOR_TEX;
        //?} else {
        /*return ShardAccess.TEXT_BACKGROUND;
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
     * Формат POSITION_COLOR_TEX (1.20.1) требует строгий порядок: Position -> Color -> UV.
     */
    public static void vertexTexColor(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z, float u, float v, int r, int g, int b, int a) {
        //? if < 1.21.1 {
        consumer.vertex(matrix, x, y, z).color(r, g, b, a).uv(u, v).endVertex();
        //?} else {
        /*consumer.addVertex(matrix, x, y, z).setColor(r, g, b, a).setUv(u, v);
        *///?}
    }

    /**
     * Добавляет вершину с позицией, текстурными координатами и цветом (без матрицы).
     * Формат POSITION_COLOR_TEX (1.20.1) требует строгий порядок: Position -> Color -> UV.
     */
    public static void vertexTexColor(VertexConsumer consumer, float x, float y, float z, float u, float v, int r, int g, int b, int a) {
        //? if < 1.21.1 {
        consumer.vertex(x, y, z).color(r, g, b, a).uv(u, v).endVertex();
        //?} else {
        /*consumer.addVertex(x, y, z).setColor(r, g, b, a).setUv(u, v);
        *///?}
    }

    /**
     * Вершина: позиция + UV, без цвета — формат POSITION_TEX (beaconBeam: цвет несет сама текстура).
     */
    public static void vertexTex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z, float u, float v) {
        //? if < 1.21.1 {
        consumer.vertex(matrix, x, y, z).uv(u, v).endVertex();
        //?} else {
        /*consumer.addVertex(matrix, x, y, z).setUv(u, v);
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
     * Adds a vertex with position and float-based color components (0.0F .. 1.0F).
     */
    public static void vertexColor(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z,
                                   float r, float g, float b, float a) {
        //? if < 1.21.1 {
        consumer.vertex(matrix, x, y, z).color(r, g, b, a).endVertex();
        //?} else {
        /*consumer.addVertex(matrix, x, y, z).setColor(r, g, b, a);
        *///?}
    }

    /**
     * Adds a vertex with position only (format POSITION).
     */
    public static void vertexPos(VertexConsumer consumer, float x, float y, float z) {
        //? if < 1.21.1 {
        consumer.vertex(x, y, z).endVertex();
        //?} else {
        /*consumer.addVertex(x, y, z);
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
     * Вершина для LINES: позиция, цвет и нормаль относительно Pose (без UV/света).
     */
    public static void vertexLine(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
                                  int r, int g, int b, int a, float nx, float ny, float nz) {
        //? if < 1.21.1 {
        consumer.vertex(pose.pose(), x, y, z).color(r, g, b, a).normal(pose.normal(), nx, ny, nz).endVertex();
        //?} else {
        /*consumer.addVertex(pose.pose(), x, y, z).setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
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
     * Как {@link #vertexFull(VertexConsumer, Matrix4f, float, float, float, int, int, int, int, float, float, int, int, float, float, float)},
     * но без матрицы — для вершин, уже преобразованных вручную (напр. ClipConsumer).
     * Паковка оверлея/света соответствует 1.20.1 {@code overlayCoords(u, v)}/{@code uv2(u, v)}:
     * u — младший short, v — старший ({@code (u & 0xFFFF) | (v << 16)}); 1.21.1
     * {@code setOverlay}/{@code setLight} распаковывают так же.
     */
    public static void vertexFull(VertexConsumer consumer, float x, float y, float z,
                                  int r, int g, int b, int a,
                                  float u, float v,
                                  int packedOverlay, int packedLight,
                                  float nx, float ny, float nz) {
        //? if < 1.21.1 {
        consumer.vertex(x, y, z).color(r, g, b, a).uv(u, v).overlayCoords(packedOverlay).uv2(packedLight).normal(nx, ny, nz).endVertex();
        //?} else {
        /*consumer.addVertex(x, y, z).setColor(r, g, b, a).setUv(u, v).setOverlay(packedOverlay).setLight(packedLight).setNormal(nx, ny, nz);
        *///?}
    }

    /** Position + colour + normal (lines / untextured quads). */
    public static void vertexColorNormal(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z,
                                         float r, float g, float b, float a, float nx, float ny, float nz) {
        //? if < 1.21.1 {
        consumer.vertex(matrix, x, y, z).color(r, g, b, a).normal(nx, ny, nz).endVertex();
        //?} else {
        /*consumer.addVertex(matrix, x, y, z).setColor(r, g, b, a).setNormal(nx, ny, nz);
        *///?}
    }

    /**
     * Applies a normal transformed by a normal matrix to a vertex chain built by the caller.
     * 1.21 removed {@code VertexConsumer.normal(Matrix3f, ...)} in favour of Pose-based overloads,
     * so the transform is done manually there, matching the old vanilla implementation.
     */
    public static VertexConsumer normal(VertexConsumer consumer, org.joml.Matrix3f n, float x, float y, float z) {
        //? if < 1.21.1 {
        return consumer.normal(n, x, y, z);
        //?} else {
        /*org.joml.Vector3f t = n.transform(new org.joml.Vector3f(x, y, z));
        return consumer.setNormal(t.x(), t.y(), t.z());
        *///?}
    }

    /**
     * Sprite UV from normalized 0..1 coordinates. 1.20.1 {@code getU/getV} expect 0..16,
     * 1.21.1 expects 0..1 - the version difference is absorbed here.
     */
    public static float spriteU(TextureAtlasSprite sprite, float u) {
        //? if < 1.21.1 {
        return sprite.getU(u * 16F);
        //?} else {
        /*return sprite.getU(u);
        *///?}
    }

    /** See {@link #spriteU}. */
    public static float spriteV(TextureAtlasSprite sprite, float v) {
        //? if < 1.21.1 {
        return sprite.getV(v * 16F);
        //?} else {
        /*return sprite.getV(v);
        *///?}
    }

    /** Position + colour + UV + lightmap, camera-space (no matrix) - NT particle quads. */
    public static void vertexTexColorLight(VertexConsumer consumer, float x, float y, float z,
                                           float u, float v, int r, int g, int b, int a, int packedLight) {
        //? if < 1.21.1 {
        consumer.vertex(x, y, z).color(r, g, b, a).uv(u, v).uv2(packedLight).endVertex();
        //?} else {
        /*consumer.addVertex(x, y, z).setColor(r, g, b, a).setUv(u, v).setLight(packedLight);
        *///?}
    }

    /** Position (pose) + colour + UV. */
    public static void vertexTexColor(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
                                      float u, float v, float r, float g, float b, float a) {
        //? if < 1.21.1 {
        consumer.vertex(pose.pose(), x, y, z).color(r, g, b, a).uv(u, v).endVertex();
        //?} else {
        /*consumer.addVertex(pose, x, y, z).setColor(r, g, b, a).setUv(u, v);
        *///?}
    }

    /** Like {@link #vertexFull} but the normal is transformed by the pose (normal matrix). */
    public static void vertexFull(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
                                  int r, int g, int b, int a,
                                  float u, float v,
                                  int packedOverlay, int packedLight,
                                  float nx, float ny, float nz) {
        //? if < 1.21.1 {
        consumer.vertex(pose.pose(), x, y, z).color(r, g, b, a).uv(u, v).overlayCoords(packedOverlay).uv2(packedLight).normal(pose.normal(), nx, ny, nz).endVertex();
        //?} else {
        /*consumer.addVertex(pose.pose(), x, y, z).setColor(r, g, b, a).setUv(u, v).setOverlay(packedOverlay).setLight(packedLight).setNormal(pose, nx, ny, nz);
        *///?}
    }

    /**
     * Кросс-версионная обёртка для putBulkData.
     * 1.20.1 Forge: 9-аргументный вызов.
     * 1.21.1+ NeoForge: 8-аргументный вызов (в ванилу 1.21.1 перенесли Forge-сигнатуру без readExistingColor).
     */
    public static void putBulkData(VertexConsumer consumer, PoseStack.Pose matrix, BakedQuad quad,
                                   float r, float g, float b, float a, int packedLight, int packedOverlay, boolean readExistingColor) {
        //? if < 1.21.1 {
        consumer.putBulkData(matrix, quad, r, g, b, a, packedLight, packedOverlay, readExistingColor);
        //?} else {
        /*// 1.21.1 (vanilla/neoforge): 8-arg сигнатура (с alpha, без readExistingColor).
        consumer.putBulkData(matrix, quad, r, g, b, a, packedLight, packedOverlay);
        *///?}
    }

    /**
     * Ванильный terrain-shade по направлению квада (те же константы, что печёт
     * ModelBlockRenderer): UP 1.0, DOWN 0.5, N/S 0.8, W/E 0.6. Квады без
     * направления (getQuads(null, null, ...)) не затемняем.
     */
    public static float quadShade(net.minecraft.core.Direction direction) {
        if (direction == null) return 1.0f;
        return switch (direction) {
            case DOWN -> 0.5f;
            case UP -> 1.0f;
            case NORTH, SOUTH -> 0.8f;
            case WEST, EAST -> 0.6f;
        };
    }

    /**
     * putBulkData с запечённым shade квада в r/g/b — для immediate-путей на
     * terrain RenderTypes (cutout/solid/translucent): их шейдеры затенение из
     * нормалей не считают, без bake квады выходят плоскими.
     */
    public static void putBulkDataShaded(VertexConsumer consumer, PoseStack.Pose matrix, BakedQuad quad,
                                         float alpha, int packedLight, int packedOverlay, boolean readExistingColor) {
        float shade = quadShade(quad.getDirection());
        putBulkData(consumer, matrix, quad, shade, shade, shade, alpha, packedLight, packedOverlay, readExistingColor);
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
     * Точный байтовый офсет элемента в формате. На 1.21.1 паддинги НЕ входят
     * в getElements(), поэтому аккумуляция размеров занижает офсеты полей
     * после паддинга (напр. IrisVertexFormats.ENTITY: 54 байта, mc_midTexCoord
     * реально на 42, аккумуляция даёт 41) — берём нативный getOffset(element).
     * На 1.20.1 паддинги — обычные элементы, аккумуляция точна (нативного
     * getOffset(Element) нет).
     *
     * @return офсет в байтах или -1, если элемента в формате нет
     */
    public static int getElementOffset(VertexFormat format, VertexFormatElement element) {
        //? if < 1.21.1 {
        int offset = 0;
        for (VertexFormatElement el : format.getElements()) {
            if (el == element) return offset;
            offset += el.getByteSize();
        }
        return -1;
        //?} else {
        /*return format.getOffset(element);
        *///?}
    }

    /**
     * Кросс-платформенное получение квадов из части BakedModel (solid render type).
     * Делегирует в {@link #getModelQuads(net.minecraft.client.resources.model.BakedModel, net.minecraft.world.level.block.state.BlockState, net.minecraft.core.Direction, net.minecraft.util.RandomSource, net.minecraft.client.renderer.RenderType)}.
     */
    public static java.util.List<BakedQuad> getPartQuads(net.minecraft.client.resources.model.BakedModel model,
                                                         net.minecraft.world.level.block.state.BlockState state,
                                                         net.minecraft.core.Direction side,
                                                         net.minecraft.util.RandomSource rand) {
        return getModelQuads(model, state, side, rand, net.minecraft.client.renderer.RenderType.solid());
    }

    /**
     * Кросс-версионный пустой ModelData (для 5-аргументного getQuads).
     * Заменяет пер-файловые заглушки вида {@code static final ModelData DATA = ModelData.EMPTY}.
     */
    public static Object emptyModelData() {
        // Пакет ModelData переписывается автозаменой !loader_packages — ветки идентичны.
        return net.minecraftforge.client.model.data.ModelData.EMPTY;
    }

    /**
     * Кросс-версионный 5-аргументный вызов {@code model.getQuads} с пустым ModelData.
     * Тела forge/neoforge идентичны (отличается только пакет ModelData) — гейт живёт здесь.
     *
     * @param renderType фильтр render type (null — без фильтра)
     */
    public static java.util.List<BakedQuad> getModelQuads(net.minecraft.client.resources.model.BakedModel model,
                                                          net.minecraft.world.level.block.state.BlockState state,
                                                          net.minecraft.core.Direction side,
                                                          net.minecraft.util.RandomSource rand,
                                                          @org.jetbrains.annotations.Nullable
                                                          net.minecraft.client.renderer.RenderType renderType) {
        // Пакет ModelData переписывается автозаменой !loader_packages — ветки идентичны.
        return model.getQuads(state, side, rand,
                net.minecraftforge.client.model.data.ModelData.EMPTY, renderType);
    }

    /**
     * Рендер ванильной {@code Model} в буфер (кросс-версионно):
     * 1.20.1 требует ARGB-float цвета, в 1.21.1 сигнатура без цвета.
     */
    public static void renderModelToBuffer(net.minecraft.client.model.Model model, PoseStack poseStack,
                                           VertexConsumer consumer, int packedLight, int packedOverlay) {
        //? if < 1.21.1 {
        model.renderToBuffer(poseStack, consumer, packedLight, packedOverlay, 1f, 1f, 1f, 1f);
        //?} else {
        /*// 1.21.1: цвет задаётся через VertexConsumer/контекст, не аргументами.
        model.renderToBuffer(poseStack, consumer, packedLight, packedOverlay);
        *///?}
    }

    // =====================================================================================
    //  Vertex-запись: варианты без полной цепочки (замена дублированных гейтов vertex/endVertex)
    // =====================================================================================

    /**
     * Вершина: позиция (с матрицей) + цвет + UV + свет блока (без нормали/оверлея).
     * Формат жидкости/расплава (FOUNDRY и т.п.).
     */
    public static void vertexColorUvLight(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z,
                                          int r, int g, int b, int a, float u, float v, int light) {
        //? if < 1.21.1 {
        consumer.vertex(matrix, x, y, z).color(r, g, b, a).uv(u, v).uv2(light).endVertex();
        //?} else {
        /*consumer.addVertex(matrix, x, y, z).setColor(r, g, b, a).setUv(u, v).setLight(light);
        *///?}
    }

    /**
     * Вершина: позиция (с матрицей) + цвет + UV + свет + нормаль (без оверлея).
     */
    public static void vertexColorUvLightNormal(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z,
                                                int r, int g, int b, int a, float u, float v, int light,
                                                float nx, float ny, float nz) {
        //? if < 1.21.1 {
        consumer.vertex(matrix, x, y, z).color(r, g, b, a).uv(u, v).uv2(light).normal(nx, ny, nz).endVertex();
        //?} else {
        /*consumer.addVertex(matrix, x, y, z).setColor(r, g, b, a).setUv(u, v).setLight(light).setNormal(nx, ny, nz);
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
        copyLevelRotation(levelRotation);
        //? if < 1.21.1 {
        PoseStack stack = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
        stack.pushPose();
        stack.setIdentity();
        stack.mulPoseMatrix(levelRotation);
        //?} else {
        /*org.joml.Matrix4fStack stack = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
        stack.pushMatrix();
        stack.identity();
        stack.mul(levelRotation);
        *///?}
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
    }

    /** Копирует в закешированную per-thread матрицу БЕЗ аллокации (кадровый путь). */
    private static void copyLevelRotation(Matrix4f levelRotation) {
        CURRENT_LEVEL_ROTATION.get().set(levelRotation);
        ROTATION_ACTIVE.set(Boolean.TRUE);
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
    private static final ThreadLocal<Matrix4f> CURRENT_LEVEL_ROTATION = ThreadLocal.withInitial(Matrix4f::new);
    /** Флаг активности пуша: матрица живёт в TL постоянно, наружу отдаётся только в окне. */
    private static final ThreadLocal<Boolean> ROTATION_ACTIVE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /** Повтор последнего {@link #pushLevelModelView}; null вне окна. */
    @org.jetbrains.annotations.Nullable
    public static Matrix4f currentLevelRotation() {
        return ROTATION_ACTIVE.get() ? CURRENT_LEVEL_ROTATION.get() : null;
    }

    /** Восстанавливает ModelViewMat после pushLevelModelView. */
    public static void popLevelModelView() {
        ROTATION_ACTIVE.set(Boolean.FALSE);
        //? if < 1.21.1 {
        PoseStack stack = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
        stack.popPose();
        //?} else {
        /*org.joml.Matrix4fStack stack = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
        stack.popMatrix();
        *///?}
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
    }


    /**
     * Возвращает частичный тик текущего кадра рендера (0.0 .. 1.0).
     */
    public static float getPartialTick() {
        //? if < 1.21.1 {
        return net.minecraft.client.Minecraft.getInstance().getFrameTime();
        //?} else {
        /*// The FALSE variant - the exact value LevelRenderer feeds the BER
        // pipeline and RenderSystem.setShaderGameTime. The TRUE variant skips
        // DeltaTracker's frozen branch (frozen = the level not running
        // normally - regular on an overloaded client) and returns the raw
        // residual there, a full tick away from what every BlockEntity lerp
        // uses - our tracked rigs' uPartialTick disagreed with the arms' lerp
        // by up to a whole tick, frame to frame = the assembler ring's jerk.
        return net.minecraft.client.Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        *///?}
    }

    /**
     * Проверяет, открыт ли F3 debug экран игрока (единственная точка: 1.20.1
     * {@code Options.renderDebug}, 1.21.1 {@code DebugScreenOverlay}).
     */
    public static boolean isDebugScreenVisible() {
        //? if < 1.21.1 {
        return net.minecraft.client.Minecraft.getInstance().options.renderDebug;
        //?} else {
        /*return net.minecraft.client.Minecraft.getInstance().getDebugOverlay().showDebugScreen();
        *///?}
    }

    /**
     * Compiles shader source safely across all desktop OpenGL drivers (especially AMD Windows drivers).
     *
     * <p>Identical in function to {@code GL20.glShaderSource(int, CharSequence)} but passes
     * a null native pointer (0L) for string length to force the driver to rely exclusively
     * on the null terminator. This works around a critical flaw in AMD Windows drivers
     * that misread length pointers and trigger memory access violations.
     *
     * @credit CrankShaft / fewizz (dev.engine_room.flywheel.backend.gl.GlCompat)
     *
     * @param shaderId The OpenGL shader object ID.
     * @param source   The GLSL shader source code.
     */
    public static void safeShaderSource(int shaderId, CharSequence source) {
        try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
            java.nio.ByteBuffer sourceBuffer = org.lwjgl.system.MemoryUtil.memUTF8(source, true);
            try {
                org.lwjgl.PointerBuffer pointers = stack.mallocPointer(1);
                pointers.put(sourceBuffer);
                org.lwjgl.opengl.GL20C.nglShaderSource(shaderId, 1, pointers.address0(), 0L);
            } finally {
                // glShaderSource copies the source strings before returning
                // (docs.gl/gl4/glShaderSource), so the native copy is always safe to free here -
                // and must be freed on every exit path: MemoryStack.close() only pops the frame,
                // it never frees this malloc'd buffer.
                org.lwjgl.system.MemoryUtil.memFree(sourceBuffer);
            }
        }
    }

    // Серверно-безопасные уведомления (onMultiblockPartRemoved / onDoorSkinChanged /
    // onDoorRemoved), вызываемые из общего кода Block/BlockEntity, вынесены в
    // CommonRenderHooks: сигнатуры этого класса содержат client-only типы, его
    // загрузка на dedicated server недопустима.

    // =====================================================================================
    //  VertexFormatElement Bridge
    //
    //  В 1.21.1 класс стал record и требует `id` первым параметром. В 1.20.1 — обычный класс без id.
    // =====================================================================================

    /** Создание VertexFormatElement (мост между 1.20.1 и 1.21.1; бывший PlatformHooks-метод). */
    public static com.mojang.blaze3d.vertex.VertexFormatElement createVertexFormatElement(
            int index, com.mojang.blaze3d.vertex.VertexFormatElement.Type type,
            com.mojang.blaze3d.vertex.VertexFormatElement.Usage usage, int count) {
        //? if < 1.21.1 {
        return new com.mojang.blaze3d.vertex.VertexFormatElement(index, type, usage, count);
        //?} else {
        /*return new com.mojang.blaze3d.vertex.VertexFormatElement(0, index, type, usage, count);
        *///?}
    }
}
