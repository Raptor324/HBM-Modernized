package com.hbm_m.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import javax.annotation.Nullable;
import com.hbm_m.config.ModClothConfig;

@OnlyIn(Dist.CLIENT)
public final class OcclusionCullingHelper {
    
    // Мапа для кэширования результатов
    private static final Long2ObjectOpenHashMap<CachedResult> occlusionCache = new Long2ObjectOpenHashMap<>();
    private static long currentFrame = 0;
    
    // Счетчик обновлений в текущем кадре
    private static int updatesThisFrame = 0;
    
    // БАЛАНСИРОВКА:
    // Обрабатываем не более 50 машин за кадр.
    // Если машин 150, обновление видимости займет 3 кадра (50мс). Это незаметно для глаза, но спасает CPU.
    private static final int MAX_UPDATES_PER_FRAME = 50; 
    
    // Reusable mutable pos
    private static final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
    
    @Nullable
    private static TagKey<Block> transparentBlocksTag = null;
    
    private OcclusionCullingHelper() {}
    
    public static void setTransparentBlocksTag(TagKey<Block> tag) {
        transparentBlocksTag = tag;
    }
    
    private static class CachedResult {
        boolean visible;
        long frame;
        
        CachedResult(boolean visible, long frame) {
            this.visible = visible;
            this.frame = frame;
        }
        
        void update(boolean visible, long frame) {
            this.visible = visible;
            this.frame = frame;
        }
    }
    
    public static boolean shouldRender(BlockPos pos, Level level, AABB renderBounds) {
        if (!ModClothConfig.get().enableOcclusionCulling) return true;
        
        long posLong = pos.asLong();
        CachedResult cached = occlusionCache.get(posLong);
        
        // 1. Если результат свежий (обновлен в этом кадре или пару кадров назад) - берем его
        // Для дальних объектов можно увеличить TTL, но пока оставим жесткую проверку для точности
        if (cached != null && (currentFrame - cached.frame < 5)) { 
            return cached.visible;
        }
        
        // 2. БАЛАНСИРОВКА: Если лимит исчерпан, возвращаем ПОСЛЕДНИЙ известный результат.
        // Если кэша нет совсем - возвращаем true (безопасно, чтобы не мерцало при появлении).
        if (updatesThisFrame >= MAX_UPDATES_PER_FRAME) {
            return cached != null ? cached.visible : true;
        }

        updatesThisFrame++;
        
        var mc = Minecraft.getInstance();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        
        // Центр машины
        double centerX = pos.getX() + 0.5;
        double centerY = pos.getY() + 0.5;
        double centerZ = pos.getZ() + 0.5;

        // Квадрат дистанции
        double dx = centerX - cameraPos.x;
        double dy = centerY - cameraPos.y;
        double dz = centerZ - cameraPos.z;
        double distSq = dx*dx + dy*dy + dz*dz;
        
        // Всегда рисуем то, что совсем рядом (меньше 4 блоков)
        if (distSq < 16.0) {
            updateCache(posLong, cached, true);
            return true;
        }

        // Проверяем видимость центра
        boolean visible = !isRayOccluded(cameraPos, centerX, centerY, centerZ, level);
        
        // Если центр закрыт, но машина близко (< 32 блоков), проверим еще и углы,
        // чтобы большая машина не исчезала, когда её центр за столбом.
        if (!visible && distSq < 1024.0) {
             if (!isRayOccluded(cameraPos, renderBounds.minX, renderBounds.minY, renderBounds.minZ, level)) visible = true;
             else if (!isRayOccluded(cameraPos, renderBounds.maxX, renderBounds.maxY, renderBounds.maxZ, level)) visible = true;
        }

        updateCache(posLong, cached, visible);
        return visible;
    }

    private static void updateCache(long key, CachedResult existing, boolean visible) {
        if (existing == null) {
            occlusionCache.put(key, new CachedResult(visible, currentFrame));
        } else {
            existing.update(visible, currentFrame);
        }
    }
    
    /**
     * Быстрый Raycast (Voxel Traversal Algorithm).
     * Идет от камеры к цели по сетке блоков.
     * Возвращает TRUE, если луч ПЕРЕКРЫТ твердым блоком.
     */
    private static boolean isRayOccluded(Vec3 start, double endX, double endY, double endZ, Level level) {
        double startX = start.x;
        double startY = start.y;
        double startZ = start.z;

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
        // (currentX + (stepX > 0 ? 1 : 0) - startX) * stepX -> расстояние до границы
        // Делим на dx (но dx может быть < 0, так что умножаем на deltaX)
        double maxX = (stepX == 0) ? Double.MAX_VALUE : ((currentX + (stepX > 0 ? 1 : 0) - startX) / dx);
        // Исправление для отрицательных направлений: tMax должен быть положительным расстоянием по лучу
        if (maxX < 0) maxX = Math.abs(maxX); // hack fix, правильнее ниже:
        
        // Правильный расчет tMax (расстояние по лучу до первой границы)
        maxX = (stepX == 0) ? Double.MAX_VALUE : (stepX > 0 ? (currentX + 1 - startX) * deltaX : (startX - currentX) * deltaX);
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
                if (isOccluder(level, mutablePos)) {
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
    
    public static void onFrameStart() {
        currentFrame++;
        updatesThisFrame = 0;
        
        // Очистка старого кеша раз в ~10 секунд
        if (currentFrame % 600 == 0) {
            occlusionCache.long2ObjectEntrySet().removeIf(e -> currentFrame - e.getValue().frame > 600);
        }
    }
    
    public static void clearCache() {
        occlusionCache.clear();
    }
}