package com.hbm_m.util;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Декларативный класс параметров двери. 
 * Определяет время анимации, звуки и коллизию.
 */
public abstract class DoorDecl {
    public abstract int getOpenTime();
    
    // Добавить методы для модели и текстуры
    public abstract ResourceLocation getModelLocation();
    public abstract ResourceLocation getTextureLocation();
    
    @Nullable
    public SoundEvent getOpenSoundStart() { return SoundEvents.IRON_DOOR_OPEN; }
    
    @Nullable
    public SoundEvent getOpenSoundEnd() { return null; }
    
    @Nullable
    public SoundEvent getCloseSoundStart() { return SoundEvents.IRON_DOOR_CLOSE; }
    
    @Nullable
    public SoundEvent getCloseSoundEnd() { return null; }
    
    @Nullable
    public SoundEvent getOpenSoundLoop() { return null; }
    
    @Nullable
    public SoundEvent getCloseSoundLoop() { return null; }
    
    @Nullable
    public SoundEvent getSoundLoop2() { return null; }
    
    public abstract int[][] getDoorOpenRanges();
    
    public float getDoorRangeOpenTime(int currentTick, int rangeIndex) {
        return (float) currentTick / getOpenTime();
    }
    
    public float getSoundVolume() { return 1.0f; }
    
    public Component getLockedMessage() { return Component.translatable("door.locked"); }
    
    public double getRenderRadius() { return 8.0; }
    
    public abstract List<AABB> getCollisionBounds(float progress, Direction facing);
    
    // ==================== Реализации ====================
    
    public static final DoorDecl LARGE_VEHICLE_DOOR = new DoorDecl() {
        @Override
        public int getOpenTime() {
            return 60;
        }
        
        @Override
        public ResourceLocation getModelLocation() {
            return ResourceLocation.fromNamespaceAndPath("hbm_m", "models/block/door_large_vehicle.obj");
        }
        
        @Override
        public ResourceLocation getTextureLocation() {
            return ResourceLocation.fromNamespaceAndPath("hbm_m", "textures/block/door_large_vehicle.png");
        }
        
        @Override
        public int[][] getDoorOpenRanges() {
            return new int[][] {
                {-3, 0, 0, 7, 6, 0}
            };
        }
        
        @Override
        public List<AABB> getCollisionBounds(float progress, Direction facing) {
            List<AABB> bounds = new ArrayList<>();
            if (progress < 1.0f) {
                double width = 7.0 * (1.0 - progress);
                double centerX = 3.5 - (width / 2.0);
                bounds.add(new AABB(
                    centerX, 0, 0.4,
                    centerX + width, 6, 0.6
                ));
            }
            return bounds;
        }
    };
}