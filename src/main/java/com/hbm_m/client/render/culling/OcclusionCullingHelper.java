package com.hbm_m.client.render.culling;


import com.hbm_m.main.MainRegistry;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
*///?}
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public final class OcclusionCullingHelper {

    /**
     * Кэш результатов ray-march: внутри кадра + короткий reuse между кадрами
     * (см. {@link #CROSS_FRAME_TTL_TICKS} и сдвиг камеры), чтобы статичные BER
     * не пересчитывали 15 лучей каждый render-tick.
     */
    private static final Long2ObjectOpenHashMap<CachedResult> occlusionCache = new Long2ObjectOpenHashMap<>();
    private static long currentFrame = 0;

    /** Сдвиг камеры больше этого — пересчёт окклюжена для BE. */
    private static final double CAMERA_REUSE_MAX_DIST_SQ = 0.25;

    /** Максимум тиков мира между пересчётами (защита от изменений геометрии). */
    private static final int CROSS_FRAME_TTL_TICKS = 20;

    /** Не хранить записи дальше этого манхэттен-расстояния от игрока (блоки). */
    private static final int MAX_KEEP_MANHATTAN_BLOCKS = 192;

    /** Жёстный предел размера карты (путешествие без prune). */
    private static final int MAX_CACHE_ENTRIES = 16384;

    /** Глобальный счётчик: инкремент при подсказке «мир/чанк мог измениться». */
    private static long clientGeometryStamp = 0L;

    /**
     * Фрустум vanilla сразу перед циклом BER ({@code RenderLevelStageEvent.AFTER_ENTITIES}).
     * {@link CpuFrustumCuller} обновлялся после BER — во время {@link #shouldRender} плоскости
     * были чужие/устаревшие, из-за чего AABB стабильно оказывались «вне фрустума».
     */
    @Nullable
    private static volatile Frustum blockEntityPassFrustum;

    @Nullable
    private static TagKey<Block> transparentBlocksTag = null;

    /**
     * Только render-thread: {@link #isRayOccluded} вызывается из {@link #shouldRender}
     * на клиенте во время рендера.
     */
    private static final BlockPos.MutableBlockPos RAY_MARCH_SCRATCH = new BlockPos.MutableBlockPos();

    private OcclusionCullingHelper() {}

    /**
     * Ключ кэша: позиция контроллера + фаза (main vs Iris shadow). Иначе за один
     * клиентский кадр сначала отрабатывает shadow pass с одной камерой, потом main
     * с другой — оба вызывают {@link #shouldRender} с тем же {@code pos.asLong()},
     * и второй проход получает чужой результат из кэша (типичный «мигающий» BER,
     * особенно на компактных мультиблоках вроде Assembly Machine).
     */
    private static long occlusionCacheKey(BlockPos pos) {
        long pk = pos.asLong();
        return ShaderCompatibilityDetector.isRenderingShadowPass() ? (pk ^ (1L << 62)) : pk;
    }

    private static long stripShadowKeyBit(long key) {
        return key & ~(1L << 62);
    }

    public static void setTransparentBlocksTag(TagKey<Block> tag) {
        transparentBlocksTag = tag;
    }

    /**
     * Вызывать, когда клиент подозревает изменение геометрии (чанк, baked refresh и т.д.).
     * Инвалидирует cross-frame reuse окклюжена; intra-frame кэш сбрасывается {@link #onFrameStart}.
     */
    public static void onClientWorldGeometryMayHaveChanged() {
        clientGeometryStamp++;
    }

    /** Вызывать из {@code RenderLevelStageEvent.Stage.AFTER_ENTITIES} (Forge) перед циклом BER. */
    public static void captureBlockEntityPassFrustum(@Nullable Frustum frustum) {
        blockEntityPassFrustum = frustum;
    }

    /** Тот же тест AABB, что vanilla делает для BE перед {@code render}. */
    private static boolean aabbPassesBerPassFrustum(AABB renderBounds) {
        Frustum f = blockEntityPassFrustum;
        if (f == null) {
            Minecraft mc = Minecraft.getInstance();
            LevelRenderer lr = mc.levelRenderer;
            if (lr != null) {
                f = lr.getFrustum();
            }
        }
        if (f != null) {
            return f.isVisible(renderBounds);
        }
        return CpuFrustumCuller.isVisible(renderBounds);
    }

    /**
     * Окклюжен через ray-march (центр → углы AABB → центры граней).
     * Не учитывает frustum vanilla — его добавляет вызывающий при необходимости.
     */
    private static boolean legacyRaycastVisibility(Vec3 cameraPos, Level level, AABB renderBounds) {
        double centerX = (renderBounds.minX + renderBounds.maxX) * 0.5;
        double centerY = (renderBounds.minY + renderBounds.maxY) * 0.5;
        double centerZ = (renderBounds.minZ + renderBounds.maxZ) * 0.5;

        double dx = centerX - cameraPos.x;
        double dy = centerY - cameraPos.y;
        double dz = centerZ - cameraPos.z;
        double distSq = dx * dx + dy * dy + dz * dz;

        if (distSq < 16.0) {
            return true;
        }

        if (!isRayOccluded(cameraPos, centerX, centerY, centerZ, level, renderBounds)) {
            return true;
        }

        boolean visible =
                !isRayOccluded(cameraPos, renderBounds.minX, renderBounds.minY, renderBounds.minZ, level, renderBounds) ||
                !isRayOccluded(cameraPos, renderBounds.maxX, renderBounds.minY, renderBounds.minZ, level, renderBounds) ||
                !isRayOccluded(cameraPos, renderBounds.minX, renderBounds.maxY, renderBounds.minZ, level, renderBounds) ||
                !isRayOccluded(cameraPos, renderBounds.maxX, renderBounds.maxY, renderBounds.minZ, level, renderBounds) ||
                !isRayOccluded(cameraPos, renderBounds.minX, renderBounds.minY, renderBounds.maxZ, level, renderBounds) ||
                !isRayOccluded(cameraPos, renderBounds.maxX, renderBounds.minY, renderBounds.maxZ, level, renderBounds) ||
                !isRayOccluded(cameraPos, renderBounds.minX, renderBounds.maxY, renderBounds.maxZ, level, renderBounds) ||
                !isRayOccluded(cameraPos, renderBounds.maxX, renderBounds.maxY, renderBounds.maxZ, level, renderBounds);

        if (!visible) {
            visible =
                    !isRayOccluded(cameraPos, centerX, renderBounds.minY, centerZ, level, renderBounds) ||
                    !isRayOccluded(cameraPos, centerX, renderBounds.maxY, centerZ, level, renderBounds) ||
                    !isRayOccluded(cameraPos, renderBounds.minX, centerY, centerZ, level, renderBounds) ||
                    !isRayOccluded(cameraPos, renderBounds.maxX, centerY, centerZ, level, renderBounds) ||
                    !isRayOccluded(cameraPos, centerX, centerY, renderBounds.minZ, level, renderBounds) ||
                    !isRayOccluded(cameraPos, centerX, centerY, renderBounds.maxZ, level, renderBounds);
        }

        return visible;
    }

    private static final class CachedResult {
        boolean visible;
        long frame;
        long checkGameTime;
        double lastCamX;
        double lastCamY;
        double lastCamZ;
        long geometryStampAtCheck;
        String lastReason; // Для логирования

        CachedResult(boolean visible, long frame, long checkGameTime,
                     double lastCamX, double lastCamY, double lastCamZ, long geometryStampAtCheck, String reason) {
            this.visible = visible;
            this.frame = frame;
            this.checkGameTime = checkGameTime;
            this.lastCamX = lastCamX;
            this.lastCamY = lastCamY;
            this.lastCamZ = lastCamZ;
            this.geometryStampAtCheck = geometryStampAtCheck;
            this.lastReason = reason;
        }

        void setAll(boolean visible, long frame, long checkGameTime,
                    double lastCamX, double lastCamY, double lastCamZ, long geometryStampAtCheck, String reason) {
            this.visible = visible;
            this.frame = frame;
            this.checkGameTime = checkGameTime;
            this.lastCamX = lastCamX;
            this.lastCamY = lastCamY;
            this.lastCamZ = lastCamZ;
            this.geometryStampAtCheck = geometryStampAtCheck;
            this.lastReason = reason;
        }
    }

    public static long occlusionKeyForBlock(BlockPos pos) {
        return occlusionCacheKey(pos);
    }

    public static boolean shouldRender(BlockPos pos, Level level, AABB renderBounds) {
        if (!ModClothConfig.get().enableOcclusionCulling) return true;

        long posLong = occlusionCacheKey(pos);
        CachedResult cached = occlusionCache.get(posLong);

        var mc = Minecraft.getInstance();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();

        if (cached != null && cached.frame == currentFrame) {
            return cached.visible;
        }

        if (cached != null && canReuseCrossFrame(cached, level, cameraPos)) {
            cached.frame = currentFrame;
            logDecision(pos, cached.visible, "CROSS_FRAME_REUSE", cached);
            return cached.visible;
        }

        boolean frustum = aabbPassesBerPassFrustum(renderBounds);
        if (!frustum) {
            putCache(posLong, cached, false, cameraPos, level, "FRUSTUM_CULLED");
            return false;
        }

        boolean visible = legacyRaycastVisibility(cameraPos, level, renderBounds);
        putCache(posLong, cached, visible, cameraPos, level, "RAYCAST_OCCLUSION");
        return visible;
    }

    private static boolean canReuseCrossFrame(CachedResult c, Level level, Vec3 cameraPos) {
        if (!c.visible) return false;
        if (ModClothConfig.useInstancedBatching()) return false;
        if (level == null) return false;
        if (c.geometryStampAtCheck != clientGeometryStamp) return false;
        long nowTick = level.getGameTime();
        long age = nowTick - c.checkGameTime;
        if (age < 0L || age > (long) CROSS_FRAME_TTL_TICKS) return false;
        double ddx = cameraPos.x - c.lastCamX;
        double ddy = cameraPos.y - c.lastCamY;
        double ddz = cameraPos.z - c.lastCamZ;
        return ddx * ddx + ddy * ddy + ddz * ddz < CAMERA_REUSE_MAX_DIST_SQ;
    }

    private static void putCache(long key, @Nullable CachedResult existing, boolean visible,
                                 Vec3 cameraPos, Level level, String reason) {
        long tick = level == null ? 0L : level.getGameTime();
        if (existing == null) {
            occlusionCache.put(key, new CachedResult(visible, currentFrame, tick,
                    cameraPos.x, cameraPos.y, cameraPos.z, clientGeometryStamp, reason));
            logDecision(BlockPos.of(stripShadowKeyBit(key)), visible, reason, null);
        } else {
            logDecision(BlockPos.of(stripShadowKeyBit(key)), visible, reason, existing);
            existing.setAll(visible, currentFrame, tick,
                    cameraPos.x, cameraPos.y, cameraPos.z, clientGeometryStamp, reason);
        }
        trimCacheIfNeeded();
    }

    // ЛОГИРОВАНИЕ: Пишет в консоль ТОЛЬКО когда состояние видимости или причина меняются.
    private static void logDecision(BlockPos pos, boolean visible, String reason, CachedResult cached) {
        try {
            if (ModClothConfig.get().mdiDebugLogDispatch) {
                if (cached == null || cached.visible != visible || !reason.equals(cached.lastReason)) {
                    MainRegistry.LOGGER.info("[HBM-Cull] BE at {} visibility changed to {}. Reason: {}",
                            pos.toShortString(), visible, reason);
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void trimCacheIfNeeded() {
        int size = occlusionCache.size();
        if (size <= MAX_CACHE_ENTRIES) return;
        int toRemove = size - MAX_CACHE_ENTRIES + (MAX_CACHE_ENTRIES >> 3);
        var it = occlusionCache.long2ObjectEntrySet().iterator();
        while (toRemove-- > 0 && it.hasNext()) {
            it.next();
            it.remove();
        }
    }

    private static boolean isRayOccluded(Vec3 start, double endX, double endY, double endZ, Level level, AABB renderBounds) {
        double startX = start.x;
        double startY = start.y;
        double startZ = start.z;

        BlockPos.MutableBlockPos mutablePos = RAY_MARCH_SCRATCH;

        int currentX = Mth.floor(startX);
        int currentY = Mth.floor(startY);
        int currentZ = Mth.floor(startZ);

        int targetX = Mth.floor(endX);
        int targetY = Mth.floor(endY);
        int targetZ = Mth.floor(endZ);

        int stepX = Integer.signum(targetX - currentX);
        int stepY = Integer.signum(targetY - currentY);
        int stepZ = Integer.signum(targetZ - currentZ);

        if (stepX == 0 && stepY == 0 && stepZ == 0) return false;

        double dx = endX - startX;
        double dy = endY - startY;
        double dz = endZ - startZ;

        double deltaX = (stepX == 0) ? Double.MAX_VALUE : Math.abs(1.0 / dx);
        double deltaY = (stepY == 0) ? Double.MAX_VALUE : Math.abs(1.0 / dy);
        double deltaZ = (stepZ == 0) ? Double.MAX_VALUE : Math.abs(1.0 / dz);

        double maxX = (stepX == 0) ? Double.MAX_VALUE : (stepX > 0 ? (currentX + 1 - startX) * deltaX : (startX - currentX) * deltaX);
        double maxY = (stepY == 0) ? Double.MAX_VALUE : (stepY > 0 ? (currentY + 1 - startY) * deltaY : (startY - currentY) * deltaY);
        double maxZ = (stepZ == 0) ? Double.MAX_VALUE : (stepZ > 0 ? (currentZ + 1 - startZ) * deltaZ : (startZ - currentZ) * deltaZ);

        int maxSteps = 100;

        while (maxSteps-- > 0) {
            if (currentX == targetX && currentY == targetY && currentZ == targetZ) return false;

            if (currentX != Mth.floor(startX) || currentY != Mth.floor(startY) || currentZ != Mth.floor(startZ)) {
                mutablePos.set(currentX, currentY, currentZ);

                if (renderBounds != null &&
                        currentX + 1 > renderBounds.minX && currentX < renderBounds.maxX &&
                        currentY + 1 > renderBounds.minY && currentY < renderBounds.maxY &&
                        currentZ + 1 > renderBounds.minZ && currentZ < renderBounds.maxZ) {
                } else if (isOccluder(level, mutablePos)) {
                    return true;
                }
            }

            if (maxX < maxY) {
                if (maxX < maxZ) {
                    currentX += stepX;
                    maxX += deltaX;
                } else {
                    currentZ += stepZ;
                    maxZ += deltaZ;
                }
            } else {
                if (maxY < maxZ) {
                    currentY += stepY;
                    maxY += deltaY;
                } else {
                    currentZ += stepZ;
                    maxZ += deltaZ;
                }
            }
        }
        return false;
    }

    private static boolean isOccluder(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;
        if (transparentBlocksTag != null && state.is(transparentBlocksTag)) return false;
        return state.isSolidRender(level, pos);
    }

    public static void onFrameStart() {
        currentFrame++;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !occlusionCache.isEmpty()) {
            BlockPos pp = mc.player.blockPosition();
            int px = pp.getX();
            int py = pp.getY();
            int pz = pp.getZ();
            occlusionCache.long2ObjectEntrySet().removeIf(e -> {
                long raw = stripShadowKeyBit(e.getLongKey());
                BlockPos bp = BlockPos.of(raw);
                int dist = Math.abs(bp.getX() - px) + Math.abs(bp.getY() - py) + Math.abs(bp.getZ() - pz);
                return dist > MAX_KEEP_MANHATTAN_BLOCKS;
            });
        }

        if (currentFrame % 600L == 0L) {
            occlusionCache.long2ObjectEntrySet().removeIf(e -> currentFrame - e.getValue().frame > 600L);
        }
    }

    public static void clearCache() {
        occlusionCache.clear();
        InstancedRenderFrame.clear();
    }
}
