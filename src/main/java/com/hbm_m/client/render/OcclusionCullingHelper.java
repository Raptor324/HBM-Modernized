package com.hbm_m.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import com.hbm_m.config.ModClothConfig;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Супер-быстрая система occlusion culling.
 * Цель: добавить максимум 3-5 FPS нагрузки.
 */
@OnlyIn(Dist.CLIENT)
public final class OcclusionCullingHelper {
    
    private static final ConcurrentHashMap<BlockPos, CachedResult> occlusionCache = new ConcurrentHashMap<>();
    private static long currentFrame = 0;
    
    //  Только нужные константы
    private static final double NEAR_DISTANCE = 24.0;
    private static final double MID_DISTANCE = 48.0;
    
    @Nullable
    private static TagKey<Block> transparentBlocksTag = null;
    
    private OcclusionCullingHelper() {}
    
    public static void setTransparentBlocksTag(TagKey<Block> tag) {
        transparentBlocksTag = tag;
    }
    
    private static class CachedResult {
        final boolean visible;
        final long frame;
        final double distance;
        
        CachedResult(boolean visible, long frame, double distance) {
            this.visible = visible;
            this.frame = frame;
            this.distance = distance;
        }
        
        boolean isValid(long currentFrame, double currentDistance) {
            //  ИСПОЛЬЗУЕМ distance для адаптивного TTL
            int ttl;
            if (this.distance < NEAR_DISTANCE) {
                ttl = 2; // Ближние: короткий кеш
            } else if (this.distance < MID_DISTANCE) {
                ttl = 5; // Средние: средний кеш
            } else {
                ttl = 10; // Дальние: длинный кеш
            }
            
            return currentFrame - frame < ttl;
        }
    }
    
    public static boolean shouldRender(BlockPos pos, Level level, AABB renderBounds) {
        if (level == null || pos == null || renderBounds == null) {
            return false;
        }
        
        if (!ModClothConfig.get().enableOcclusionCulling) {
            return true;
        }
        
        Minecraft mc = Minecraft.getInstance();
        var cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        Vec3 center = renderBounds.getCenter();
        
        double dx = center.x - cameraPos.x;
        double dy = center.y - cameraPos.y;
        double dz = center.z - cameraPos.z;
        double distanceSq = dx * dx + dy * dy + dz * dz;
        double distance = Math.sqrt(distanceSq);
        
        // Проверка кеша
        CachedResult cached = occlusionCache.get(pos);
        if (cached != null && cached.isValid(currentFrame, distance)) {
            return cached.visible;
        }
        
        // Адаптивная проверка
        boolean visible = !isOccludedFast(pos, level, renderBounds, cameraPos, distance, distanceSq);
        
        occlusionCache.put(pos, new CachedResult(visible, currentFrame, distance));
        
        return visible;
    }
    
    /**
     * Максимально быстрая проверка окклюзии
     */
    private static boolean isOccludedFast(BlockPos centerPos, Level level, AABB bounds, 
                                         Vec3 cameraPos, double distance, double distanceSq) {
        // Ранний выход: очень близко
        if (distance < 4.0) {
            return false;
        }
        
        // Ранний выход: очень далеко
        if (distance > 192.0) {
            return false;
        }
        
        //  Определяем количество точек по квадрату дистанции (быстрее)
        int numPoints;
        double nearSq = NEAR_DISTANCE * NEAR_DISTANCE;
        double midSq = MID_DISTANCE * MID_DISTANCE;
        
        if (distanceSq < nearSq) {
            numPoints = 3; // Ближние: центр + 2 угла
        } else if (distanceSq < midSq) {
            numPoints = 2; // Средние: центр + 1 угол
        } else {
            numPoints = 1; // Дальние: только центр
        }
        
        // Проверяем точки БЕЗ создания массива
        if (!isPointOccludedFast(cameraPos, bounds.getCenter(), level, bounds, distance)) {
            return false;
        }
        
        if (numPoints >= 2) {
            Vec3 corner1 = new Vec3(bounds.minX, bounds.minY, bounds.minZ);
            if (!isPointOccludedFast(cameraPos, corner1, level, bounds, distance)) {
                return false;
            }
        }
        
        if (numPoints >= 3) {
            Vec3 corner2 = new Vec3(bounds.maxX, bounds.maxY, bounds.maxZ);
            if (!isPointOccludedFast(cameraPos, corner2, level, bounds, distance)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Ультра-быстрый ray-casting
     */
    private static boolean isPointOccludedFast(Vec3 cameraPos, Vec3 targetPoint, Level level, 
                                              AABB structureBounds, double distance) {
        double dx = targetPoint.x - cameraPos.x;
        double dy = targetPoint.y - cameraPos.y;
        double dz = targetPoint.z - cameraPos.z;
        double rayDistance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        
        if (rayDistance < 2.0) {
            return false;
        }
        
        dx /= rayDistance;
        dy /= rayDistance;
        dz /= rayDistance;
        
        // Агрессивный шаг по дистанции
        double stepSize;
        if (distance < NEAR_DISTANCE) {
            stepSize = 1.0;
        } else if (distance < MID_DISTANCE) {
            stepSize = 1.5;
        } else {
            stepSize = 2.0;
        }
        
        int steps = (int) Math.ceil(rayDistance / stepSize);
        steps = Math.min(steps, 16);
        
        for (int i = 1; i < steps; i++) {
            double t = i / (double) steps * rayDistance;
            BlockPos checkPos = BlockPos.containing(
                cameraPos.x + dx * t,
                cameraPos.y + dy * t,
                cameraPos.z + dz * t
            );
            
            if (structureBounds.contains(checkPos.getX() + 0.5, checkPos.getY() + 0.5, checkPos.getZ() + 0.5)) {
                continue;
            }
            
            var blockState = level.getBlockState(checkPos);
            
            if (blockState.isAir()) {
                continue;
            }
            
            if (isOccludingBlockFast(blockState, level, checkPos)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Супер-быстрая проверка блока
     */
    private static boolean isOccludingBlockFast(BlockState blockState, Level level, BlockPos pos) {
        if (transparentBlocksTag != null && blockState.is(transparentBlocksTag)) {
            return false;
        }
        
        if (!blockState.isSolidRender(level, pos)) {
            return false;
        }
        
        return true;
    }
    
    public static void onFrameStart() {
        currentFrame++;
        
        if (currentFrame % 60 == 0) {
            occlusionCache.entrySet().removeIf(entry -> 
                currentFrame - entry.getValue().frame > 20
            );
        }
    }
    
    public static void clearCache() {
        occlusionCache.clear();
    }
}
