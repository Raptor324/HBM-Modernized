package com.hbm_m.client.missile.track;

import com.hbm_m.client.render.LightSampleCache;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.client.render.missile.MissileRenderData;
import com.hbm_m.client.render.missile.MissileRenderHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

/**
 * Authoritative missile mesh draw for S2C tracks — independent of client entity sync and chunk gates.
 */
public final class MissileTrackWorldRender {

    private static final double FRUSTUM_SAFETY = 0.72D;
    private static final double MIN_VIRTUAL_DISTANCE_BLOCKS = 96.0D;

    private static int cachedRenderDistance = -1;
    private static double cachedMaxSafeDistance = MIN_VIRTUAL_DISTANCE_BLOCKS;

    private MissileTrackWorldRender() {}

    /**
     * Dedicated, isolated {@link MultiBufferSource} for missile track rendering.
     * MUST NOT be the shared {@link Minecraft#renderBuffers()} bufferSource: the
     * flush-all at the end of {@link #render} must only flush missile geometry, not
     * every other mod's pending shared-builder quads. Copycats' Sliding/Folding
     * door emits to {@code RenderType.translucentMovingBlock()} (a non-fixed layer
     * that lives in the shared global builder); flushing it here at AFTER_ENTITIES
     * (before the contraption solid flush and before translucent terrain/water/
     * slime) made the door write depth early and occlude everything drawn after it
     * behind the door. Isolating missile geometry onto a private source makes the
     * flush-all safe even when missiles are actually launching.
     */
    private static volatile MultiBufferSource.BufferSource missileBufferSource;

    private static MultiBufferSource.BufferSource missileBufferSource() {
        MultiBufferSource.BufferSource bs = missileBufferSource;
        if (bs == null) {
            // PlainBufferSource, а НЕ MultiBufferSource.immediate(): под
            // ImmediatelyFast фабрика подменяет источник на BatchableBufferSource,
            // падающий на пустых sortOnUpload-батчах (см. PlainBufferSource).
            //? if < 1.21.1 {
            bs = new com.hbm_m.client.render.PlainBufferSource(new com.mojang.blaze3d.vertex.BufferBuilder(256));
            //?} else {
            /*bs = new com.hbm_m.client.render.PlainBufferSource(new com.mojang.blaze3d.vertex.ByteBufferBuilder(256));
            *///?}
            missileBufferSource = bs;
        }
        return bs;
    }

    public static void render(float partialTick, PoseStack poseStack) {
        renderFiltered(partialTick, Double.NaN, false);
    }

    /**
     * Рендер треков ракет с опциональным фильтром по дистанции² до камеры.
     *
     * @param distFilterSq Double.NaN = все треки; иначе граница по дистанции²
     *                     (far: d² > distFilterSq, near: d² <= distFilterSq —
     *                     та же граница, что у NT-частиц в EngineHandler).
     *                     Примитивный параметр вместо DoublePredicate: горячий
     *                     путь кадра, лямбда с захватом splitSq аллоцировалась
     *                     на каждый вызов.
     * @param far       true = рисовать дальние (d² > границы).
     *                  Виртуализация внутри renderOne работает как раньше:
     *                  при DH — истинные координаты (дальний проход без
     *                  клипа), без DH — приближение к границе прорисовки.
     * @return true, если была отрисована хотя бы одна ракета.
     */
    public static boolean renderFiltered(float partialTick, double distFilterSq, boolean far) {
        if (!MissileTrackClient.isEnabled()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return false;
        }

        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        // Private source — never the shared global one (see missileBufferSource()).
        MultiBufferSource.BufferSource buffers = missileBufferSource();

        // Своя камера-relative стопка С ЗАПЕЧЁННЫМ ПОВОРОТОМ камеры: в чистой
        // ваниле ambient RenderSystem ModelViewMat на AFTER_WEATHER = identity
        //(поворот живёт в event-PoseStack, который мы не используем), поэтому
        // полагаться на ambient нельзя в принципе. Домножаем захваченную
        // R_cam (TLS-копия пуша EngineHandler'а) прямо в стопку — как ваниль
        // делает для энтити (Camera#render возвращает PoseStack с поворотом).
        // SingleMeshVboRenderer в track-контексте знает об этом и НЕ умножает
        // ambient повторно.
        PoseStack poseStack = new PoseStack();
        var levelRot = com.hbm_m.platform.RenderHooks.currentLevelRotation();
        if (levelRot != null) {
            // Прямое домножение в JOML-матрицу текущего pose — без
            // версионных PoseStack-API (mulPoseMatrix/mulMatrix разошлись).
            poseStack.last().pose().mul(levelRot);
        }
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        boolean drewAny = false;
        for (MissileTrackClient.TrackEntry entry : MissileTrackClient.entries()) {
            if (!MissileTrackClient.shouldUseTrackWorldRender(entry.entityId)) {
                continue;
            }
            MissileTrackClient.InterpolatedPose pose = entry.interpolate(partialTick);
            if (pose == null) {
                continue;
            }
            if (!Double.isNaN(distFilterSq)) {
                double dx = pose.x() - camera.x;
                double dy = pose.y() - camera.y;
                double dz = pose.z() - camera.z;
                boolean near = dx * dx + dy * dy + dz * dz <= distFilterSq;
                if (near == far) {
                    continue;
                }
            }
            if (renderOne(level, pose, poseStack, buffers, camera)) {
                drewAny = true;
            }
        }
        // Flush the private missile source so buffered missile aux-geometry draws
        // this frame. Safe because this source is isolated from the shared global
        // bufferSource — flush-all here cannot disturb other mods' pending
        // shared-builder quads (Copycats' translucentMovingBlock door etc.).
        if (drewAny) {
            buffers.endBatch();
        }
        return drewAny;
    }

    private static double sqr(double v) {
        return v * v;
    }

    private static boolean renderOne(ClientLevel level, MissileTrackClient.InterpolatedPose pose,
                                  PoseStack poseStack, MultiBufferSource.BufferSource buffers, Vec3 camera) {
        MissileRenderData data = MissileRenderHelper.resolveFromTrack(pose.current());
        if (data == null) {
            return false;
        }

        CameraRelativePose virtual = virtualizeWorld(pose.x(), pose.y(), pose.z(), camera);
        double drawX = camera.x + virtual.relX();
        double drawY = camera.y + virtual.relY();
        double drawZ = camera.z + virtual.relZ();

        BlockPos lightPos = BlockPos.containing(virtual.trueX(), virtual.trueY(), virtual.trueZ());
        // Свет по ПОЗИЦИИ ракеты: меш в небе/далеко от игрока освещается
        // своим окружением, а не чанком камеры. Если чанк ракеты не загружен
        // (дальние ступени трека, DH) — fallback на свет камеры: он стабилен
        // на границе прогрузки и не даёт скачков яркости.
        BlockPos samplePos = lightPos;
        if (!level.hasChunkAt(lightPos)) {
            samplePos = BlockPos.containing(camera.x, camera.y, camera.z);
        }
        int packedLight = LightTexture.pack(
                level.getBrightness(LightLayer.BLOCK, samplePos),
                level.getBrightness(LightLayer.SKY, samplePos));

        Direction launchFacing = pose.current().launchFacing();
        float yaw = pose.yaw();
        float pitch = pose.pitch();

        poseStack.pushPose();
        poseStack.translate(drawX, drawY, drawZ);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.YN.rotationDegrees(yaw - 90.0F));
        MissileRenderHelper.applyLaunchFacingRotation(poseStack, launchFacing);

        if (virtual.screenScale() < 1.0F) {
            float s = virtual.screenScale();
            poseStack.scale(s, s, s);
        }

        LightSampleCache.BASE_POSE.set(poseStack.last().pose());
        LightSampleCache.BASE_POSE_SET.set(true);

        // ВНИМАНИЕ: НЕ трогаем RenderSystem.setShaderFog*! Раньше здесь глобально
        // раздвигали туман кадра (fogEnd = max(prev, dist+512)) ради дальнего
        // меша — это ОКАЗЫВАЛОСЬ ЧЁРНЫМ ЭКРАНОМ: значение утекало в следующие
        // кадры, и мир дальше ~460 блоков заливался туманом fogColor (в мире на
        // y<0 он почти чёрный). Дальний меш в тумане не нуждается: блок_lit
        // глушит туман собственными юниформами (cachedFogStartU=1.0E8 при
        // entityMissileDepthBias), а NT-частицы не фоггатся вовсе.
        SingleMeshVboRenderer.setEntityMissileDepthBias(true);
        try {
            data.render(poseStack, packedLight, lightPos, buffers);
            return true;
        } finally {
            SingleMeshVboRenderer.setEntityMissileDepthBias(false);
            LightSampleCache.BASE_POSE_SET.set(false);
            poseStack.popPose();
        }
    }

    public static double maxSafeRenderDistanceBlocks() {
        Minecraft mc = Minecraft.getInstance();
        int chunks = mc.options.getEffectiveRenderDistance();
        if (chunks != cachedRenderDistance) {
            cachedRenderDistance = chunks;
            cachedMaxSafeDistance = Math.max(MIN_VIRTUAL_DISTANCE_BLOCKS, chunks * 16.0D * FRUSTUM_SAFETY);
        }
        return cachedMaxSafeDistance;
    }

    public static CameraRelativePose virtualizeWorld(double worldX, double worldY, double worldZ, Vec3 camera) {
        double relX = worldX - camera.x;
        double relY = worldY - camera.y;
        double relZ = worldZ - camera.z;
        double dist = Math.sqrt(relX * relX + relY * relY + relZ * relZ);
        // DH РЕАЛЬНО рендерит этот кадр → не виртуализируем: дальний контент
        // идёт через проход EngineHandler с удлинённой проекцией (нет клипа).
        // DH не рендерит (не установлен / выключен / завис — isActive с
        // 500мс-грейсом стабилен между кадрами, «двойных колец» нет) →
        // старый виртуальный рендер: приближаем объект к границе прорисовки.
        if (com.hbm_m.client.compat.dh.DhClientState.isActive()) {
            return new CameraRelativePose(relX, relY, relZ, 1.0F, worldX, worldY, worldZ);
        }
        double max = maxSafeRenderDistanceBlocks();
        float scale = 1.0F;
        if (dist > max && dist >= 1.0E-4D) {
            scale = (float) (max / dist);
            relX *= scale;
            relY *= scale;
            relZ *= scale;
        }
        return new CameraRelativePose(relX, relY, relZ, scale, worldX, worldY, worldZ);
    }

    public record CameraRelativePose(
            double relX, double relY, double relZ,
            float screenScale,
            double trueX, double trueY, double trueZ
    ) {}
}
