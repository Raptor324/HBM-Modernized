package com.hbm_m.client.render;


import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;

import com.mojang.blaze3d.systems.RenderSystem;

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
    private static final int MAX_CACHE_ENTRIES = 4096;

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

    private static final class CachedResult {
        boolean visible;
        /** Последний {@link #currentFrame}, в котором считали или переиспользовали результат. */
        long frame;
        long checkGameTime;
        double lastCamX;
        double lastCamY;
        double lastCamZ;
        long geometryStampAtCheck;

        CachedResult(boolean visible, long frame, long checkGameTime,
                     double lastCamX, double lastCamY, double lastCamZ, long geometryStampAtCheck) {
            this.visible = visible;
            this.frame = frame;
            this.checkGameTime = checkGameTime;
            this.lastCamX = lastCamX;
            this.lastCamY = lastCamY;
            this.lastCamZ = lastCamZ;
            this.geometryStampAtCheck = geometryStampAtCheck;
        }

        void setAll(boolean visible, long frame, long checkGameTime,
                    double lastCamX, double lastCamY, double lastCamZ, long geometryStampAtCheck) {
            this.visible = visible;
            this.frame = frame;
            this.checkGameTime = checkGameTime;
            this.lastCamX = lastCamX;
            this.lastCamY = lastCamY;
            this.lastCamZ = lastCamZ;
            this.geometryStampAtCheck = geometryStampAtCheck;
        }
    }

    public static boolean shouldRender(BlockPos pos, Level level, AABB renderBounds) {
        if (!ModClothConfig.get().enableOcclusionCulling) return true;

        long posLong = occlusionCacheKey(pos);
        CachedResult cached = occlusionCache.get(posLong);

        var mc = Minecraft.getInstance();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();

        // 1. Уже обслуживали этот BE в этом render-кадре — тот же результат.
        if (cached != null && cached.frame == currentFrame) {
            return cached.visible;
        }

        // 2. Переиспользование между кадрами: статичная сцена, камера почти не двигалась.
        if (cached != null && canReuseCrossFrame(cached, level, cameraPos)) {
            cached.frame = currentFrame;
            return cached.visible;
        }

        // Новый путь: GPU compute / CPU AABB frustum.
        // Под Iris/Oculus оставляем legacy raycast (другой агент кеширует его
        // через canReuseCrossFrame), чтобы не ломать существующую логику теней
        // и shadow-pass совместимость.
        ModClothConfig cfg = ModClothConfig.get();
        ModClothConfig.CullingMode mode = cfg.cullingMode;
        boolean irisActive = ShaderCompatibilityDetector.isExternalShaderActive();
        if (!irisActive && mode != ModClothConfig.CullingMode.LEGACY_RAYCAST) {
            boolean useGpu = cfg.useGpuCulling
                    && (mode == ModClothConfig.CullingMode.GPU_COMPUTE
                        || mode == ModClothConfig.CullingMode.AUTO)
                    && GpuCullingPipeline.isSupported();
            // Видимость = тот же Frustum, что LevelRenderer (после vanilla-отсечения BE).
            // Результат compute из GpuCullingPipeline не используем здесь: матрица dispatch
            // собиралась после BER и расходилась с миром; лаг кадра + ложные «culled».
            boolean visible = aabbPassesBerPassFrustum(renderBounds);
            if (useGpu) {
                GpuCullingPipeline.submit(posLong, renderBounds);
            }
            putCache(posLong, cached, visible, cameraPos, level);
            return visible;
        }

        // Центр СТРУКТУРЫ (по AABB), а не центр блока контроллера. Для
        // мультиблоков контроллер часто стоит на краю/в углу всей структуры,
        // и raycast только до его центра отвергал ВСЮ машину когда контроллер
        // оказывался за тонкой стеной - даже если 90% структуры (например
        // вышка фрекинга 7×7×24 или сборочная 3×3×3) торчало в открытом виде.
        // Используем центроид renderBounds, чтобы базовая точка лежала в массе
        // структуры.
        double centerX = (renderBounds.minX + renderBounds.maxX) * 0.5;
        double centerY = (renderBounds.minY + renderBounds.maxY) * 0.5;
        double centerZ = (renderBounds.minZ + renderBounds.maxZ) * 0.5;

        // Квадрат дистанции от камеры до центра структуры
        double dx = centerX - cameraPos.x;
        double dy = centerY - cameraPos.y;
        double dz = centerZ - cameraPos.z;
        double distSq = dx * dx + dy * dy + dz * dz;

        // Всегда рисуем то, что совсем рядом (меньше 4 блоков от центра структуры).
        // Особенно важно для крупных мультиблоков - игрок может стоять буквально
        // ВНУТРИ structure'ы (вышка фрекинга), и raycast от глаз "наружу" даст
        // ложное окклюжен.
        if (distSq < 16.0) {
            putCache(posLong, cached, true, cameraPos, level);
            return true;
        }

        // Этап 1: дешёвая проверка центра.
        if (!isRayOccluded(cameraPos, centerX, centerY, centerZ, level, renderBounds)) {
            putCache(posLong, cached, true, cameraPos, level);
            return true;
        }

        // Этап 2: если центр закрыт, обходим 8 углов AABB. Любой видимый
        // угол означает, что часть структуры на экране, и культить нельзя.
        boolean visible =
                !isRayOccluded(cameraPos, renderBounds.minX, renderBounds.minY, renderBounds.minZ, level, renderBounds) ||
                !isRayOccluded(cameraPos, renderBounds.maxX, renderBounds.minY, renderBounds.minZ, level, renderBounds) ||
                !isRayOccluded(cameraPos, renderBounds.minX, renderBounds.maxY, renderBounds.minZ, level, renderBounds) ||
                !isRayOccluded(cameraPos, renderBounds.maxX, renderBounds.maxY, renderBounds.minZ, level, renderBounds) ||
                !isRayOccluded(cameraPos, renderBounds.minX, renderBounds.minY, renderBounds.maxZ, level, renderBounds) ||
                !isRayOccluded(cameraPos, renderBounds.maxX, renderBounds.minY, renderBounds.maxZ, level, renderBounds) ||
                !isRayOccluded(cameraPos, renderBounds.minX, renderBounds.maxY, renderBounds.maxZ, level, renderBounds) ||
                !isRayOccluded(cameraPos, renderBounds.maxX, renderBounds.maxY, renderBounds.maxZ, level, renderBounds);

        // Этап 3: если все 8 углов закрыты, проверяем 6 центров граней AABB.
        // Для коротких/плоских мультиблоков (напр. Assembly Machine 4×2×4)
        // лучи к нижним углам часто проходят через землю, а к верхним — через
        // потолок/рельеф. Один твёрдый блок вне AABB рядом с гранью может
        // одновременно заблокировать 4 угла этой грани. 6 дополнительных
        // точек на гранях делают ложно-отрицательное окклюжен значительно
        // менее вероятным (нужно закрыть 15 лучей, а не 9).
        if (!visible) {
            visible =
                !isRayOccluded(cameraPos, centerX, renderBounds.minY, centerZ, level, renderBounds) || // bottom face
                !isRayOccluded(cameraPos, centerX, renderBounds.maxY, centerZ, level, renderBounds) || // top face
                !isRayOccluded(cameraPos, renderBounds.minX, centerY, centerZ, level, renderBounds) || // west face
                !isRayOccluded(cameraPos, renderBounds.maxX, centerY, centerZ, level, renderBounds) || // east face
                !isRayOccluded(cameraPos, centerX, centerY, renderBounds.minZ, level, renderBounds) || // north face
                !isRayOccluded(cameraPos, centerX, centerY, renderBounds.maxZ, level, renderBounds);   // south face
        }

        putCache(posLong, cached, visible, cameraPos, level);
        return visible;
    }

    private static boolean canReuseCrossFrame(CachedResult c, Level level, Vec3 cameraPos) {
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
                                 Vec3 cameraPos, Level level) {
        long tick = level == null ? 0L : level.getGameTime();
        if (existing == null) {
            occlusionCache.put(key, new CachedResult(visible, currentFrame, tick,
                    cameraPos.x, cameraPos.y, cameraPos.z, clientGeometryStamp));
        } else {
            existing.setAll(visible, currentFrame, tick,
                    cameraPos.x, cameraPos.y, cameraPos.z, clientGeometryStamp);
        }
        trimCacheIfNeeded();
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

    /**
     * Быстрый Raycast (Voxel Traversal Algorithm).
     * Идет от камеры к цели по сетке блоков.
     * Возвращает TRUE, если луч ПЕРЕКРЫТ твердым блоком.
     */
    private static boolean isRayOccluded(Vec3 start, double endX, double endY, double endZ, Level level, AABB renderBounds) {
        double startX = start.x;
        double startY = start.y;
        double startZ = start.z;

        BlockPos.MutableBlockPos mutablePos = RAY_MARCH_SCRATCH;

        // Позиция "курсора" в сетке блоков
        int currentX = Mth.floor(startX);
        int currentY = Mth.floor(startY);
        int currentZ = Mth.floor(startZ);

        int targetX = Mth.floor(endX);
        int targetY = Mth.floor(endY);
        int targetZ = Mth.floor(endZ);

        // Направление шага (+1 или -1)
        int stepX = Integer.signum(targetX - currentX);
        int stepY = Integer.signum(targetY - currentY);
        int stepZ = Integer.signum(targetZ - currentZ);

        if (stepX == 0 && stepY == 0 && stepZ == 0) return false; // Мы уже внутри целевого блока

        // Дельты (насколько нужно пройти по лучу, чтобы пересечь границу блока по оси)
        double dx = endX - startX;
        double dy = endY - startY;
        double dz = endZ - startZ;

        // Избегаем деления на ноль
        double deltaX = (stepX == 0) ? Double.MAX_VALUE : Math.abs(1.0 / dx);
        double deltaY = (stepY == 0) ? Double.MAX_VALUE : Math.abs(1.0 / dy);
        double deltaZ = (stepZ == 0) ? Double.MAX_VALUE : Math.abs(1.0 / dz);

        // Max (насколько далеко мы уже прошли до следующей границы)
        double maxX = (stepX == 0) ? Double.MAX_VALUE : (stepX > 0 ? (currentX + 1 - startX) * deltaX : (startX - currentX) * deltaX);
        double maxY = (stepY == 0) ? Double.MAX_VALUE : (stepY > 0 ? (currentY + 1 - startY) * deltaY : (startY - currentY) * deltaY);
        double maxZ = (stepZ == 0) ? Double.MAX_VALUE : (stepZ > 0 ? (currentZ + 1 - startZ) * deltaZ : (startZ - currentZ) * deltaZ);

        // Ограничитель (чтобы не улететь в бесконечность, если что-то пойдет не так)
        int maxSteps = 100;

        while (maxSteps-- > 0) {
            // Если мы пришли в целевой блок - значит препятствий не было
            if (currentX == targetX && currentY == targetY && currentZ == targetZ) {
                return false;
            }

            // Проверяем текущий блок на непрозрачность
            // Исключаем стартовый блок (где камера), чтобы не клипаться головой
            if (currentX != Mth.floor(startX) || currentY != Mth.floor(startY) || currentZ != Mth.floor(startZ)) {
                mutablePos.set(currentX, currentY, currentZ);

                // Игнорируем блоки, которые пересекаются с AABB самой структуры!
                // Иначе мультиблок (например, его dummy-блоки) будет перекрывать сам себя.
                if (renderBounds != null &&
                    currentX + 1 > renderBounds.minX && currentX < renderBounds.maxX &&
                    currentY + 1 > renderBounds.minY && currentY < renderBounds.maxY &&
                    currentZ + 1 > renderBounds.minZ && currentZ < renderBounds.maxZ) {
                    // Это блок самой структуры (или внутри её AABB), пропускаем
                } else if (isOccluder(level, mutablePos)) {
                    return true; // Нашли стену!
                }
            }

            // Шагаем к следующему блоку по оси, до границы которой ближе всего
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

        return false; // Дошли до лимита шагов, считаем что видно
    }

    private static boolean isOccluder(Level level, BlockPos pos) {
        // Быстрая проверка чанка (опционально, level.getBlockState само проверит, но это может сэкономить время)
        if (!level.hasChunkAt(pos)) return false;

        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;

        if (transparentBlocksTag != null && state.is(transparentBlocksTag)) return false;

        // Основная проверка: перекрывает ли блок обзор
        return state.isSolidRender(level, pos);
    }

    /**
     * После BER и (опционально) MDI: readback предыдущего compute, {@link GpuCullingPipeline#dispatch},
     * затем {@link GpuCullingPipeline#beginFrame} — иначе {@code beginFrame} очищает staging до
     * {@code dispatch}, и compute никогда не видит AABB с BER. Вызывается из
     * {@link com.hbm_m.event.ClientModEvents} после {@code MdiBatchCoordinator.endFrame()}.
     * <p>Вызывать на render-thread до {@link #onFrameStart()}.
     */
    public static void runGpuCullingAfterBlockEntities(Matrix4f projectionMatrix, Vec3 cameraPos) {
        try {
            ModClothConfig cfg = ModClothConfig.get();
            if (!cfg.enableOcclusionCulling
                    || cfg.cullingMode == ModClothConfig.CullingMode.LEGACY_RAYCAST
                    || ShaderCompatibilityDetector.isExternalShaderActive()) {
                return;
            }
            Matrix4f viewProj = new Matrix4f(projectionMatrix).mul(new Matrix4f(RenderSystem.getModelViewMatrix()));
            // Актуальные плоскости для fallback {@link CpuFrustumCuller} (раньше только в неиспользуемом ClientRenderHandlerForge).
            CpuFrustumCuller.updateFrustum(viewProj);
            if (cfg.useGpuCulling) {
                // isSupported() до initialize() может быть true по caps при initialized==false;
                // dispatch() требует реальной инициализации GL.
                GpuCullingPipeline.initialize();
                if (GpuCullingPipeline.isSupported()) {
                    // #region agent log
                    MdiDebugNdjson.log("H_GPU_ORDER", "OcclusionCullingHelper.runGpuCullingAfterBlockEntities", "gpu path enter",
                            "{\"stagingBeforeReadback\":" + GpuCullingPipeline.debugStagingEntryCount() + "}");
                    // #endregion agent log
                    GpuCullingPipeline.tryReadback();
                    GpuCullingPipeline.dispatch(viewProj, cameraPos);
                    GpuCullingPipeline.beginFrame();
                }
            }
        } catch (Throwable ignored) {
        }
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

        // Резервная очистка «залипших» записей (например, без игрока в меню)
        if (currentFrame % 600L == 0L) {
            occlusionCache.long2ObjectEntrySet().removeIf(e -> currentFrame - e.getValue().frame > 600L);
        }
    }

    public static void clearCache() {
        occlusionCache.clear();
    }
}
