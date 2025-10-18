package com.hbm_m.block.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.client.model.render.LegacyAnimator;
import com.hbm_m.sound.ModSounds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Декларативный класс параметров двери.
 * Модели загружаются автоматически из blockstate JSON файлов.
 */
public abstract class DoorDecl {
    // Кеш загруженных моделей
    private final Map<String, BakedModel> modelCache = new HashMap<>();
    private BakedModel[] cachedModelParts = null;
    private String[] cachedPartNames = null;

    // ==================== Основные абстрактные методы ====================

    /**
     * Время открытия в тиках
     */
    public abstract int getOpenTime();

    /**
     * Диапазоны открытия двери для мультиблока
     */
    public abstract int[][] getDoorOpenRanges();

    /**
     * Размеры мультиблока двери
     */
    public abstract int[] getDimensions();

    /**
     * УДАЛЕНО: getModelLocation() - больше не нужно!
     * УДАЛЕНО: getTextureLocation() - больше не нужно!
     */

    /**
     * ID блока для автоматической загрузки модели
     * Например: "hbm_m:large_vehicle_door"
     */
    protected abstract ResourceLocation getBlockId();

    public abstract List<AABB> getCollisionBounds(float progress, Direction facing);

    // ==================== Автоматическая загрузка моделей ====================

    /**
     * Возвращает массив имен частей модели
     * Переопределяется в подклассах для кастомных моделей
     */
    public String[] getPartNames() {
        if (cachedPartNames == null) {
            // По умолчанию загружаем из blockstate JSON
            cachedPartNames = new String[] { "main" };
        }
        return cachedPartNames;
    }

    /**
     * Загружает модель из blockstate JSON автоматически
     */
    @Nullable
    public BakedModel[] getModelParts() {
        if (cachedModelParts != null) {
            return cachedModelParts;
        }

        ModelManager modelManager = Minecraft.getInstance().getModelManager();
        String[] partNames = getPartNames();
        cachedModelParts = new BakedModel[partNames.length];
        for (int i = 0; i < partNames.length; i++) {
            String partName = partNames[i];
            // Создаем ModelResourceLocation из blockstate
            // Формат: "modid:block_name#variant"
            ModelResourceLocation modelLocation;
            if ("main".equals(partName)) {
                // Основная модель из blockstate JSON
                modelLocation = new ModelResourceLocation(getBlockId(), "");
            } else {
                // Дополнительные части как варианты
                modelLocation = new ModelResourceLocation(getBlockId(), "part=" + partName);
            }

            // Получаем BakedModel из ModelManager
            BakedModel model = modelManager.getModel(modelLocation);
            cachedModelParts[i] = model;
            modelCache.put(partName, model);
        }
        return cachedModelParts;
    }

    /**
     * Возвращает BakedModel для конкретной части
     */
    @Nullable
    public BakedModel getModelPart(String partName) {
        if (modelCache.containsKey(partName)) {
            return modelCache.get(partName);
        }

        // Ленивая загрузка
        getModelParts();
        return modelCache.get(partName);
    }

    /**
     * Возвращает текстуру для конкретной части
     * Теперь берется из BakedModel автоматически!
     */
    public ResourceLocation getTextureForPart(int skinIndex, String partName) {
        BakedModel model = getModelPart(partName);
        if (model != null && !model.getQuads(null, null, null).isEmpty()) {
            // Текстура берется из BakedModel, не нужно хардкодить
            return model.getParticleIcon().atlasLocation();
        }

        // Fallback: используем blockstate атлас
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/atlas/blocks.png");
    }

    public ResourceLocation getTextureForPart(String partName) {
        return getTextureForPart(0, partName);
    }

    // ==================== Остальные методы без изменений ====================

    public int getBlockOffset() { return 0; }

    public boolean remoteControllable() { return false; }

    public float getDoorRangeOpenTime(int currentTick, int rangeIndex) {
        return getNormTime(currentTick);
    }

    public boolean hasSkins() { return false; }

    public int getSkinCount() { return 0; }

    public boolean isLadder(boolean open) { return false; }

    public AABB getBlockBound(int x, int y, int z, boolean open, boolean forCollision) {
        if (open) return new AABB(0, 0, 0, 0, 0, 0);
        return new AABB(0, 0, 0, 1, 1, 1);
    }

    // Звуки
    @Nullable public SoundEvent getOpenSoundStart() { return null; }
    @Nullable public SoundEvent getOpenSoundEnd() { return null; }
    @Nullable public SoundEvent getOpenSoundLoop() { return null; }
    @Nullable public SoundEvent getCloseSoundStart() { return getOpenSoundStart(); }
    @Nullable public SoundEvent getCloseSoundEnd() { return getOpenSoundEnd(); }
    @Nullable public SoundEvent getCloseSoundLoop() { return getOpenSoundLoop(); }
    @Nullable public SoundEvent getSoundLoop2() { return null; }
    public float getSoundVolume() { return 1.0f; }

    public double getRenderRadius() { return 8.0; }

    public Component getLockedMessage() {
        return Component.translatable("door.locked");
    }

    public void doOffsetTransform(LegacyAnimator animator) {}
    public double[][] getClippingPlanes() { return new double[0][]; }

    // Трансформации
    public void getTranslation(String partName, float openTicks, boolean child, float[] trans) {
        trans[0] = 0; trans[1] = 0; trans[2] = 0;
    }

    public void getOrigin(String partName, float[] orig) {
        orig[0] = 0; orig[1] = 0; orig[2] = 0;
    }

    public void getRotation(String partName, float openTicks, float[] rot) {
        rot[0] = 0; rot[1] = 0; rot[2] = 0;
    }

    public boolean doesRender(String partName, boolean child) { return true; }

    public String[] getChildren(String partName) { return new String[0]; }

    protected float getNormTime(float time) {
        return getNormTime(time, 0, getOpenTime());
    }

    protected float getNormTime(float time, float min, float max) {
        if (max - min == 0) return 0;
        return Math.max(0, Math.min(1, (time - min) / (max - min)));
    }

    protected void set(float[] array, float x, float y, float z) {
        array[0] = x; array[1] = y; array[2] = z;
    }

    // ==================== Пример реализации ====================

    public static final DoorDecl LARGE_VEHICLE_DOOR = new DoorDecl() {
        @Override
        protected ResourceLocation getBlockId() {
            return ResourceLocation.fromNamespaceAndPath("hbm_m", "large_vehicle_door");
        }
        
        @Override
        public String[] getPartNames() {
            return new String[] { "frame", "doorLeft", "doorRight" };
        }
        
        @Override
        public int getOpenTime() { return 60; }
        
        @Override
        public void getTranslation(String partName, float openTicks, boolean child, float[] trans) {
            if ("doorLeft".equals(partName)) {
                set(trans, 0, 0, 3.0F * getNormTime(openTicks));
            } else if ("doorRight".equals(partName)) {
                set(trans, 0, 0, -3.0F * getNormTime(openTicks));
            } else {
                super.getTranslation(partName, openTicks, child, trans);
            }
        }
        
        @Override
        public double[][] getClippingPlanes() {
            return new double[][] {
                { 1.0, 0.0, 0.0, 3.5 },
                { -1.0, 0.0, 0.0, 3.5 }
            };
        }
        
        @Override
        public int[][] getDoorOpenRanges() {
            return new int[][] { { 0, 0, 0, -4, 6, 2 }, { 0, 0, 0, 4, 6, 2 } };
        }
        
        @Override
        public int[] getDimensions() {
            return new int[] { 5, 0, 0, 0, 3, 3 };
        }
        
        @Override
    public List<AABB> getCollisionBounds(float progress, Direction facing) {
        List<AABB> bounds = new ArrayList<>();
        
        if (progress >= 0.99f) {
            return bounds; // Полностью открыта
        }
        
        // ==================== ЛЕВАЯ СТВОРКА (3 блока шириной) ====================
        // Координаты ОТНОСИТЕЛЬНО контроллера
        // Занимает блоки X=-3,-2,-1 (3 блока влево от центра)
        // Высота Y=0..6 (6 блоков вверх)
        
        double leftMovement = progress * 3.0;
        double leftDepth = Math.max(0.0, 1.0 - leftMovement);
        
        if (leftDepth > 0.05) {
            // Один большой AABB для всей левой створки
            AABB leftDoor = new AABB(
                -3.0, 0.0, 0.0,
                0.0, 6.0, leftDepth
            );
            bounds.add(rotateAABB(leftDoor, facing));
        }
        
        // ==================== ПРАВАЯ СТВОРКА (3 блока шириной) ====================
        // Занимает блоки X=0,1,2 (3 блока вправо от центра)
        
        double rightMovement = progress * 3.0;
        double rightOffset = Math.min(1.0, rightMovement);
        
        if (1.0 - rightOffset > 0.05) {
            // Один большой AABB для всей правой створки
            AABB rightDoor = new AABB(
                0.0, 0.0, rightOffset,
                3.0, 6.0, 1.0
            );
            bounds.add(rotateAABB(rightDoor, facing));
        }
        
        return bounds;
    }

        /**
         * Поворачивает AABB относительно центра блока в соответствии с направлением
         */
        private AABB rotateAABB(AABB aabb, Direction facing) {
            double minX = aabb.minX;
            double minY = aabb.minY;
            double minZ = aabb.minZ;
            double maxX = aabb.maxX;
            double maxY = aabb.maxY;
            double maxZ = aabb.maxZ;
            
            return switch (facing) {
                case NORTH -> aabb; // Без поворота
                case SOUTH -> new AABB(-maxX, minY, -maxZ, -minX, maxY, -minZ);
                case WEST -> new AABB(-maxZ, minY, minX, -minZ, maxY, maxX);
                case EAST -> new AABB(minZ, minY, -maxX, maxZ, maxY, -minX);
                default -> aabb;
            };
        }
        
        @Override
        public SoundEvent getOpenSoundStart() {
            return null; // НЕ ИСПОЛЬЗУЕМ start звук для открытия
        }
        
        @Override
        public SoundEvent getOpenSoundEnd() {
            return ModSounds.GARAGE_STOP.get();
        }
        
        @Override
        public SoundEvent getOpenSoundLoop() {
            return ModSounds.GARAGE_MOVE.get(); // ТОЛЬКО loop звук
        }
        
        @Override
        public SoundEvent getCloseSoundStart() {
            return null; // НЕ ИСПОЛЬЗУЕМ start звук для закрытия
        }
        
        @Override
        public SoundEvent getCloseSoundEnd() {
            return ModSounds.GARAGE_STOP.get();
        }
        
        @Override
        public SoundEvent getCloseSoundLoop() {
            return ModSounds.GARAGE_MOVE.get(); // ТОЛЬКО loop звук
        }
        
        @Override
        public float getSoundVolume() {
            return 2.0f;
        }
    };
}
