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
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.client.model.DoorBakedModel;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.sound.ModSounds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Декларативный класс параметров двери.
 * Модели загружаются автоматически из blockstate JSON файлов.
 */
@OnlyIn(Dist.CLIENT)
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

        try {
            ModelManager modelManager = Minecraft.getInstance().getModelManager();
            ModelResourceLocation modelLocation = new ModelResourceLocation(getBlockId(), "");
            BakedModel baseModel = modelManager.getModel(modelLocation);
            
            if (baseModel instanceof DoorBakedModel doorModel) {
                String[] partNames = getPartNames();
                cachedModelParts = new BakedModel[partNames.length];
                
                for (int i = 0; i < partNames.length; i++) {
                    String partName = partNames[i];
                    BakedModel partModel = doorModel.getPart(partName);
                    cachedModelParts[i] = partModel;
                    modelCache.put(partName, partModel);
                }
                
                return cachedModelParts;
            } else {
                MainRegistry.LOGGER.error("Expected DoorBakedModel but got: {}", 
                    baseModel != null ? baseModel.getClass().getSimpleName() : "null");
            }
            
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Error loading door model parts", e);
        }
        
        return null;
    }

    /**
     * Возвращает BakedModel для конкретной части
     */
    @Nullable
    public BakedModel getModelPart(String partName) {
        if (modelCache.containsKey(partName)) {
            return modelCache.get(partName);
        }

        // Для дверей используем DoorBakedModel напрямую
        try {
            ModelManager modelManager = Minecraft.getInstance().getModelManager();
            ModelResourceLocation modelLocation = new ModelResourceLocation(getBlockId(), "");
            BakedModel baseModel = modelManager.getModel(modelLocation);
            
            if (baseModel instanceof DoorBakedModel doorModel) {
                BakedModel partModel = doorModel.getPart(partName);
                if (partModel != null) {
                    modelCache.put(partName, partModel);
                    return partModel;
                }
            }
            
            MainRegistry.LOGGER.warn("Could not find part '{}' in door model", partName);
            return null;
            
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Error loading door part model for '{}'", partName, e);
            return null;
        }
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

    protected AABB rotateAABB(AABB aabb, Direction facing) {
        switch (facing) {
            // NORTH: базовая ориентация + разворот на 180°
            case NORTH:
                return new AABB(
                    aabb.maxX, aabb.minY, aabb.maxZ,
                    aabb.minX, aabb.maxY, aabb.minZ
                );
            
            // WEST: исходная трансформация + разворот на 180°
            case WEST:
                return new AABB(
                    -aabb.maxZ, aabb.minY, aabb.maxX,
                    -aabb.minZ, aabb.maxY, aabb.minX
                );
            
            // SOUTH: исходная трансформация + разворот на 180°
            case SOUTH:
                return new AABB(
                    aabb.maxX, aabb.minY, -aabb.maxZ,
                    aabb.minX, aabb.maxY, -aabb.minZ
                );
            
            // EAST: исходная трансформация + разворот на 180°
            case EAST:
                return new AABB(
                    aabb.maxZ, aabb.minY, -aabb.maxX,
                    aabb.minZ, aabb.maxY, -aabb.minX
                );
            
            default:
                return new AABB(
                    -aabb.maxX, aabb.minY, -aabb.maxZ,
                    -aabb.minX, aabb.maxY, -aabb.minZ
                );
        }
    }
    
    

    // ==================== Реализации ====================

    public static final DoorDecl LARGE_VEHICLE_DOOR = new DoorDecl() {
        @Override
        protected ResourceLocation getBlockId() {
            return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "large_vehicle_door");
        }

        @Override
        public String[] getPartNames() {
            return new String[] { "frame", "doorLeft", "doorRight" };
        }

        @Override public int getOpenTime() { return 60; }

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

        @Override public double[][] getClippingPlanes() {
            return new double[][] {
                { 1.0, 0.0, 0.0, 3.5 },
                { -1.0, 0.0, 0.0, 3.5 }
            };
        }

        @Override public int[][] getDoorOpenRanges() {
            return new int[][] { { 0, 0, 0, -4, 6, 2 }, { 0, 0, 0, 4, 6, 2 } };
        }

        @Override public int[] getDimensions() {
            return new int[] { 5, 0, 0, 0, 3, 3 };
        }

        @Override
        public List<AABB> getCollisionBounds(float progress, Direction facing) {
            List<AABB> bounds = new ArrayList<>();
            if (progress >= 0.99f) {
                return bounds; // Полностью открыта
            }

            // ИСПРАВЛЕНО: Базовые коллизии для EAST направления
            // Левая створка (движется влево по оси X, а не Z)
            double leftMovement = progress * 3.0; // сдвиг по ходу анимации (как у вас)
            double leftWidth = Math.max(0.0, 3.5 - leftMovement);
            if (leftWidth > 0.05) {
                // Стартуем не с -3, а с -3.5
                AABB leftDoor = new AABB(-3.5, 0.0, 0.0, -3.5 + leftWidth, 6.0, 1.0);
                bounds.add(rotateAABB(leftDoor, facing));
            }

            // Правая створка: от rightOffset до rightOffset + dynamicWidth
            double rightMovement = progress * 3.0;
            double rightOffset = rightMovement;
            double rightWidth = Math.max(0.0, 3.5 - rightOffset);
            if (rightWidth > 0.05) {
                // Увеличиваем maxX на +0.5
                AABB rightDoor = new AABB(rightOffset, 0.0, 0.0, rightOffset + rightWidth, 6.0, 1.0);
                bounds.add(rotateAABB(rightDoor, facing));
            }

            return bounds;
        }

        @Override public SoundEvent getOpenSoundEnd() { return ModSounds.GARAGE_STOP.get(); }
        @Override public SoundEvent getOpenSoundLoop() { return ModSounds.GARAGE_MOVE.get(); }
        @Override public SoundEvent getCloseSoundEnd() { return ModSounds.GARAGE_STOP.get(); }
        @Override public SoundEvent getCloseSoundLoop() { return ModSounds.GARAGE_MOVE.get(); }
        @Override public float getSoundVolume() { return 2.0f; }
    };

    public static final DoorDecl ROUND_AIRLOCK_DOOR = new DoorDecl() {
        @Override
        protected ResourceLocation getBlockId() {
            return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "round_airlock_door");
        }

        @Override
        public String[] getPartNames() {
            return new String[] { "frame", "doorLeft", "doorRight" };
        }

        @Override public int getOpenTime() { return 60; }

        @Override
        public void getTranslation(String partName, float openTicks, boolean child, float[] trans) {
            if ("doorLeft".equals(partName)) {
                // ИСПРАВЛЕНО: Движение по X, чтобы соответствовать коллизиям
                set(trans, -3.0F * getNormTime(openTicks), 0, 0); // Левая створка влево по X
            } else if ("doorRight".equals(partName)) {
                // ИСПРАВЛЕНО: Движение по X, чтобы соответствовать коллизиям
                set(trans, 3.0F * getNormTime(openTicks), 0, 0);  // Правая створка вправо по X
            } else {
                super.getTranslation(partName, openTicks, child, trans);
            }
        }

        @Override public double[][] getClippingPlanes() {
            return new double[][] {
                { 0.0, 0.0, 1.0, 2.0001 },
                { 0.0, 0.0, -1.0, 2.0001 }
            };
        }

        @Override public int[][] getDoorOpenRanges() {
            return new int[][] { { 0, 0, 0, -2, 4, 2 }, { 0, 0, 0, 3, 4, 2 } };
        }

        @Override public int[] getDimensions() {
            return new int[] { 4, 0, 0, 0, 1, 1 };
        }

        @Override
        public List<AABB> getCollisionBounds(float progress, Direction facing) {
            List<AABB> bounds = new ArrayList<>();
            if (progress >= 0.99f) {
                return bounds; // Полностью открыта
            }

            // Левая створка
            double leftMovement = progress * 1.5;
            double leftDepth = Math.max(0.0, 1.0 - leftMovement);
            if (leftDepth > 0.05) {
                AABB leftDoor = new AABB(-1.0, 0.0, 0.0, 0.0, 4.0, leftDepth);
                bounds.add(rotateAABB(leftDoor, facing));
            }

            // Правая створка
            double rightMovement = progress * 1.5;
            double rightOffset = Math.min(1.0, rightMovement);
            if (1.0 - rightOffset > 0.05) {
                AABB rightDoor = new AABB(0.0, 0.0, rightOffset, 1.0, 4.0, 1.0);
                bounds.add(rotateAABB(rightDoor, facing));
            }

            return bounds;
        }

        @Override public SoundEvent getOpenSoundEnd() { return ModSounds.GARAGE_STOP.get(); }
        @Override public SoundEvent getOpenSoundLoop() { return ModSounds.GARAGE_MOVE.get(); }
        @Override public SoundEvent getCloseSoundEnd() { return ModSounds.GARAGE_STOP.get(); }
        @Override public SoundEvent getCloseSoundLoop() { return ModSounds.GARAGE_MOVE.get(); }
        @Override public float getSoundVolume() { return 2.0f; }
    };
    
    public static final DoorDecl TRANSITION_SEAL = new DoorDecl() {
        @Override
        protected ResourceLocation getBlockId() {
            return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "transition_seal");
        }

        @Override public String[] getPartNames() { return new String[] { "frame", "door" }; }
        @Override public int getOpenTime() { return 480; }

        @Override
        public void getTranslation(String partName, float openTicks, boolean child, float[] trans) {
            if (!"frame".equals(partName)) {
                set(trans, 0, 3.5F * getNormTime(openTicks), 0);
            } else {
                super.getTranslation(partName, openTicks, child, trans);
            }
        }

        @Override public int[][] getDoorOpenRanges() {
            return new int[][] { { -9, 2, 0, 20, 20, 1 } };
        }

        @Override public int[] getDimensions() {
            return new int[] { 23, 0, 0, 0, 13, 12 };
        }

        @Override
        public List<AABB> getCollisionBounds(float progress, Direction facing) {
            List<AABB> bounds = new ArrayList<>();
            if (progress >= 0.99f) return bounds;
            
            // Огромная дверь, движется вверх
            double movement = progress * 3.5;
            AABB door = new AABB(-9.0, 0.0, 0.0, 11.0, Math.max(0.5, 20.0 - movement), 1.0);

            // Fix: Using DoorDecl's static rotateAABB if accessible, or provide local implementation as fallback
            bounds.add(rotateAABB(door, facing)); // Assuming rotateAABB exists statically, else implement here

            return bounds;
        }

        @Override public SoundEvent getOpenSoundStart() { return ModSounds.TRANSITION_SEAL_OPEN.get(); }
        @Override public float getSoundVolume() { return 6.0f; }
    };

    public static final DoorDecl FIRE_DOOR = new DoorDecl() {
        @Override
        protected ResourceLocation getBlockId() {
            return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "fire_door");
        }

        @Override public String[] getPartNames() { return new String[] { "frame", "door" }; }
        @Override public int getOpenTime() { return 160; }

        @Override
        public void getTranslation(String partName, float openTicks, boolean child, float[] trans) {
            if (!"frame".equals(partName)) {
                set(trans, 0, 3.0F * getNormTime(openTicks), 0);
            } else {
                super.getTranslation(partName, openTicks, child, trans);
            }
        }

        @Override public double[][] getClippingPlanes() {
            return new double[][] { { 0, -1, 0, 3.0001 } };
        }

        @Override public int[][] getDoorOpenRanges() {
            return new int[][] { { -1, 0, 0, 3, 4, 1 } };
        }

        @Override public int[] getDimensions() {
            return new int[] { 2, 0, 0, 0, 2, 1 };
        }

        @Override
        public List<AABB> getCollisionBounds(float progress, Direction facing) {
            List<AABB> bounds = new ArrayList<>();
            if (progress >= 0.99f) return bounds;
            
            // Противопожарная дверь, движется вверх
            double movement = progress * 3.0;
            AABB door = new AABB(-1.0, 0.0, 0.0, 2.0, Math.max(0.1, 4.0 - movement), 1.0);
            bounds.add(rotateAABB(door, facing));
            return bounds;
        }

        @Override public SoundEvent getOpenSoundEnd() { return ModSounds.WGH_STOP.get(); }
        @Override public SoundEvent getOpenSoundLoop() { return ModSounds.WGH_START.get(); }
        @Override public SoundEvent getSoundLoop2() { return ModSounds.ALARM_6.get(); }
        @Override public float getSoundVolume() { return 2.0f; }
    };

    public static final DoorDecl SLIDE_DOOR = new DoorDecl() {
        @Override
        protected ResourceLocation getBlockId() {
            return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "sliding_blast_door");
        }

        @Override public String[] getPartNames() { return new String[] { "frame", "doorLeft", "doorRight" }; }
        @Override public int getOpenTime() { return 24; }

        @Override public double[][] getClippingPlanes() {
            return new double[][] { { -1, 0, 0, 3.50001 }, { 1, 0, 0, 3.50001 } };
        }

        @Override public int[][] getDoorOpenRanges() {
            return new int[][] { { -2, 0, 0, 4, 5, 1 } };
        }

        @Override public int[] getDimensions() {
            return new int[] { 3, 0, 0, 0, 3, 3 };
        }

        @Override public boolean hasSkins() { return true; }
        @Override public int getSkinCount() { return 3; }

        @Override
        public List<AABB> getCollisionBounds(float progress, Direction facing) {
            List<AABB> bounds = new ArrayList<>();
            if (progress >= 0.99f) return bounds;
            
            // Раздвижная дверь
            double movement = progress * 1.5;
            if (movement < 1.0) {
                AABB door = new AABB(-2.0, 0.0, 0.0, 3.0, 5.0, 1.0 - movement);
                bounds.add(rotateAABB(door, facing));
            }
            return bounds;
        }

        @Override public SoundEvent getOpenSoundEnd() { return ModSounds.SLIDING_DOOR_OPENED.get(); }
        @Override public SoundEvent getCloseSoundEnd() { return ModSounds.SLIDING_DOOR_SHUT.get(); }
        @Override public SoundEvent getOpenSoundLoop() { return ModSounds.SLIDING_DOOR_OPENING.get(); }
        @Override public SoundEvent getSoundLoop2() { return ModSounds.SLIDING_DOOR_OPENING.get(); }
        @Override public float getSoundVolume() { return 2.0f; }
    };

    public static final DoorDecl SLIDING_SEAL_DOOR = new DoorDecl() {
        @Override
        protected ResourceLocation getBlockId() {
            return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "sliding_seal_door");
        }

        @Override public String[] getPartNames() { return new String[] { "frame", "door" }; }
        @Override public int getOpenTime() { return 20; }

        @Override
        public void getTranslation(String partName, float openTicks, boolean child, float[] trans) {
            if (partName.startsWith("door")) {
                set(trans, 0, 0, getNormTime(openTicks));
            } else {
                set(trans, 0, 0, 0);
            }
        }

        @Override public double[][] getClippingPlanes() {
            return new double[][] { { 0, 0, -1, 0.5001 } };
        }

        @Override public int[][] getDoorOpenRanges() {
            return new int[][] { { 0, 0, 0, 1, 2, 2 } };
        }

        @Override public int[] getDimensions() {
            return new int[] { 1, 0, 0, 0, 0, 0 };
        }

        @Override
        public List<AABB> getCollisionBounds(float progress, Direction facing) {
            List<AABB> bounds = new ArrayList<>();
            if (progress >= 0.99f) return bounds;
            
            // Герметичная раздвижная дверь
            double movement = progress * 1.0;
            if (movement < 1.0) {
                AABB door = new AABB(0.0, 0.0, 1.0 - 0.25, 1.0, 2.0, 1.0);
                bounds.add(rotateAABB(door, facing));
            }
            return bounds;
        }

        @Override public SoundEvent getOpenSoundEnd() { return ModSounds.SLIDING_DOOR_OPENED.get(); }
        @Override public SoundEvent getOpenSoundStart() { return ModSounds.SLIDING_DOOR_OPENING.get(); }
        @Override public float getSoundVolume() { return 2.0f; }
    };

    public static final DoorDecl SECURE_ACCESS_DOOR = new DoorDecl() {
        @Override
        protected ResourceLocation getBlockId() {
            return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "secure_access_door");
        }

        @Override public String[] getPartNames() { return new String[] { "frame", "door" }; }
        @Override public int getOpenTime() { return 120; }

        @Override
        public void getTranslation(String partName, float openTicks, boolean child, float[] trans) {
            if (!"frame".equals(partName)) {
                set(trans, 0, 3.5F * getNormTime(openTicks), 0);
            } else {
                super.getTranslation(partName, openTicks, child, trans);
            }
        }

        @Override public double[][] getClippingPlanes() {
            return new double[][] { { 0, -1, 0, 5 } };
        }

        @Override public int[][] getDoorOpenRanges() {
            return new int[][] { { -2, 1, 0, 4, 5, 1 } };
        }

        @Override public int[] getDimensions() {
            return new int[] { 4, 0, 0, 0, 2, 2 };
        }

        @Override
        public List<AABB> getCollisionBounds(float progress, Direction facing) {
            List<AABB> bounds = new ArrayList<>();
            if (progress >= 0.99f) return bounds;
            
            // Дверь безопасного доступа, движется вверх
            double movement = progress * 3.5;
            AABB door = new AABB(-2.0, 0.0, 0.0, 3.0, Math.max(0.0625, 5.0 - movement), 1.0);
            bounds.add(rotateAABB(door, facing));
            return bounds;
        }

        @Override public SoundEvent getCloseSoundLoop() { return ModSounds.GARAGE_MOVE.get(); }
        @Override public SoundEvent getCloseSoundEnd() { return ModSounds.GARAGE_STOP.get(); }
        @Override public SoundEvent getOpenSoundEnd() { return ModSounds.GARAGE_STOP.get(); }
        @Override public SoundEvent getOpenSoundLoop() { return ModSounds.GARAGE_MOVE.get(); }
        @Override public float getSoundVolume() { return 2.0f; }
    };

    public static final DoorDecl QE_SLIDING = new DoorDecl() {
        @Override
        protected ResourceLocation getBlockId() {
            return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "qe_sliding_door");
        }

        @Override public String[] getPartNames() { return new String[] { "frame", "left", "right" }; }
        @Override public int getOpenTime() { return 10; }

        @Override
        public void getTranslation(String partName, float openTicks, boolean child, float[] trans) {
            if (partName.startsWith("left")) {
                set(trans, 0, 0, 0.99F * getNormTime(openTicks));
            } else if (partName.startsWith("right")) {
                set(trans, 0, 0, -0.99F * getNormTime(openTicks));
            }
        }

        @Override public int[][] getDoorOpenRanges() {
            return new int[][] { { 0, 0, 0, 2, 2, 2 } };
        }

        @Override public int[] getDimensions() {
            return new int[] { 1, 0, 0, 0, 1, 0 };
        }

        @Override
        public List<AABB> getCollisionBounds(float progress, Direction facing) {
            List<AABB> bounds = new ArrayList<>();
            if (progress >= 0.99f) return bounds;
            
            // Быстрая раздвижная дверь
            double movement = progress * 0.99;
            if (movement < 0.99) {
                AABB door = new AABB(0.0, 0.0, 1.0 - 0.1875, 2.0, 2.0, 1.0);
                bounds.add(rotateAABB(door, facing));
            }
            return bounds;
        }

        @Override public SoundEvent getOpenSoundEnd() { return ModSounds.SLIDING_DOOR_OPENED.get(); }
        @Override public SoundEvent getCloseSoundEnd() { return ModSounds.SLIDING_DOOR_SHUT.get(); }
        @Override public SoundEvent getOpenSoundLoop() { return ModSounds.SLIDING_DOOR_OPENING.get(); }
        @Override public float getSoundVolume() { return 2.0f; }
    };

    public static final DoorDecl QE_CONTAINMENT = new DoorDecl() {
        @Override
        protected ResourceLocation getBlockId() {
            return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "qe_containment_door");
        }

        @Override public String[] getPartNames() { return new String[] { "frame", "door", "decal" }; }
        @Override public int getOpenTime() { return 160; }

        @Override
        public void getTranslation(String partName, float openTicks, boolean child, float[] trans) {
            if (!"frame".equals(partName)) {
                set(trans, 0, 3.0F * getNormTime(openTicks), 0);
            } else {
                super.getTranslation(partName, openTicks, child, trans);
            }
        }

        @Override public double[][] getClippingPlanes() {
            return new double[][] { { 0, -1, 0, 3.0001 } };
        }

        @Override public int[][] getDoorOpenRanges() {
            return new int[][] { { -1, 0, 0, 3, 3, 1 } };
        }

        @Override public int[] getDimensions() {
            return new int[] { 2, 0, 0, 0, 1, 1 };
        }

        @Override
        public List<AABB> getCollisionBounds(float progress, Direction facing) {
            List<AABB> bounds = new ArrayList<>();
            if (progress >= 0.99f) return bounds;
            
            // Дверь изоляционной камеры, движется вверх
            double movement = progress * 3.0;
            AABB door = new AABB(-1.0, 0.0, 0.5, 2.0, Math.max(0.1, 3.0 - movement), 1.0);
            bounds.add(rotateAABB(door, facing));
            return bounds;
        }

        @Override public SoundEvent getOpenSoundEnd() { return ModSounds.WGH_STOP.get(); }
        @Override public SoundEvent getOpenSoundLoop() { return ModSounds.WGH_START.get(); }
        @Override public float getSoundVolume() { return 2.0f; }
    };

    public static final DoorDecl WATER_DOOR = new DoorDecl() {
        @Override
        protected ResourceLocation getBlockId() {
            return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "water_door");
        }

        @Override public String[] getPartNames() { 
            return new String[] { "frame", "door", "bolt", "spinny_upper", "spinny_lower" }; 
        }
        @Override public int getOpenTime() { return 60; }

        @Override
        public void getTranslation(String partName, float openTicks, boolean child, float[] trans) {
            if ("bolt".equals(partName)) {
                set(trans, 0, 0, 0.4F * smoothstep(getNormTime(openTicks, 0, 30)));
            } else {
                set(trans, 0, 0, 0);
            }
        }

        @Override
        public void getOrigin(String partName, float[] orig) {
            if ("door".equals(partName) || "bolt".equals(partName)) {
                set(orig, 0.125F, 1.5F, 1.18F);
            } else if ("spinny_upper".equals(partName)) {
                set(orig, 0.041499F, 2.43569F, -0.587849F);
            } else if ("spinny_lower".equals(partName)) {
                set(orig, 0.041499F, 0.571054F, -0.587849F);
            } else {
                super.getOrigin(partName, orig);
            }
        }

        @Override
        public void getRotation(String partName, float openTicks, float[] rot) {
            if (partName.startsWith("spinny")) {
                set(rot, smoothstep(getNormTime(openTicks, 0, 30)) * 360, 0, 0);
            } else if ("door".equals(partName) || "bolt".equals(partName)) {
                set(rot, 0, smoothstep(getNormTime(openTicks, 30, 60)) * -134, 0);
            } else {
                super.getRotation(partName, openTicks, rot);
            }
        }

        @Override
        public boolean doesRender(String partName, boolean child) {
            return child || !partName.startsWith("spinny");
        }

        @Override
        public String[] getChildren(String partName) {
            if ("door".equals(partName))
                return new String[] { "spinny_lower", "spinny_upper" };
            return super.getChildren(partName);
        }

        @Override public float getDoorRangeOpenTime(int ticks, int idx) {
            return getNormTime(ticks, 35, 40);
        }

        @Override public int[][] getDoorOpenRanges() {
            return new int[][] { { 1, 0, 0, -3, 3, 2 } };
        }

        @Override public int[] getDimensions() {
            return new int[] { 2, 0, 0, 0, 1, 1 };
        }

        @Override
        public List<AABB> getCollisionBounds(float progress, Direction facing) {
            List<AABB> bounds = new ArrayList<>();
            if (progress >= 0.99f) return bounds;
            
            // Водонепроницаемая дверь со сложной анимацией
            AABB door = new AABB(0.0, 0.0, 0.75, 1.0, 3.0, 1.0);
            bounds.add(rotateAABB(door, facing));
            return bounds;
        }

        private float smoothstep(float t) {
            return t * t * (3.0f - 2.0f * t);
        }

        @Override public SoundEvent getOpenSoundEnd() { return ModSounds.WGH_STOP.get(); }
        @Override public SoundEvent getOpenSoundLoop() { return ModSounds.WGH_START.get(); }
        @Override public SoundEvent getOpenSoundStart() { return ModSounds.LEVER_1.get(); }
        @Override public SoundEvent getCloseSoundEnd() { return ModSounds.LEVER_1.get(); }
        @Override public float getSoundVolume() { return 2.0f; }
    };

    public static final DoorDecl SILO_HATCH = new DoorDecl() {
        @Override
        protected ResourceLocation getBlockId() {
            return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "silo_hatch");
        }

        @Override public String[] getPartNames() { return new String[] { "frame", "hatch" }; }
        @Override public int getOpenTime() { return 60; }
        @Override public boolean remoteControllable() { return true; }

        @Override
        public void getTranslation(String partName, float openTicks, boolean child, float[] trans) {
            if ("hatch".equals(partName)) {
                float smoothTime = smoothstep(getNormTime(openTicks, 0, 10));
                set(trans, 0, 0.25F * smoothTime, 0);
            } else {
                set(trans, 0, 0, 0);
            }
        }

        @Override
        public void getOrigin(String partName, float[] orig) {
            if ("hatch".equals(partName)) {
                set(orig, 0F, 0.875F, -1.875F);
            } else {
                set(orig, 0, 0, 0);
            }
        }

        @Override
        public void getRotation(String partName, float openTicks, float[] rot) {
            if ("hatch".equals(partName)) {
                float smoothTime = smoothstep(getNormTime(openTicks, 20, 100));
                set(rot, smoothTime * -240, 0, 0);
            } else {
                super.getRotation(partName, openTicks, rot);
            }
        }

        @Override public float getDoorRangeOpenTime(int ticks, int idx) {
            return getNormTime(ticks, 20, 20);
        }

        @Override public int getBlockOffset() { return 2; }

        @Override public int[][] getDoorOpenRanges() {
            return new int[][] { { 1, 0, 1, -3, 3, 0 }, { 0, 0, 1, -3, 3, 0 }, { -1, 0, 1, -3, 3, 0 } };
        }

        @Override public int[] getDimensions() {
            return new int[] { 0, 0, 2, 2, 2, 2 };
        }

        @Override
        public List<AABB> getCollisionBounds(float progress, Direction facing) {
            List<AABB> bounds = new ArrayList<>();
            if (progress >= 0.99f) return bounds;
            
            // Люк силоса
            if (progress < 0.5) { // Люк еще не открыт полностью
                AABB hatch = new AABB(-1.0, 2.0, -1.0, 2.0, 3.0, 2.0);
                bounds.add(rotateAABB(hatch, facing));
            }
            return bounds;
        }

        private float smoothstep(float t) {
            return t * t * (3.0f - 2.0f * t);
        }

        @Override public SoundEvent getOpenSoundEnd() { return ModSounds.WGH_STOP.get(); }
        @Override public SoundEvent getOpenSoundLoop() { return ModSounds.WGH_START.get(); }
        @Override public SoundEvent getCloseSoundEnd() { return ModSounds.WGH_STOP.get(); }
        @Override public float getSoundVolume() { return 2.0f; }
    };

    public static final DoorDecl SILO_HATCH_LARGE = new DoorDecl() {
        @Override
        protected ResourceLocation getBlockId() {
            return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "silo_hatch_large");
        }

        @Override public String[] getPartNames() { return new String[] { "frame", "hatch" }; }
        @Override public int getOpenTime() { return 60; }
        @Override public boolean remoteControllable() { return true; }

        @Override
        public void getTranslation(String partName, float openTicks, boolean child, float[] trans) {
            if ("hatch".equals(partName)) {
                float smoothTime = smoothstep(getNormTime(openTicks, 0, 10));
                set(trans, 0, 0.25F * smoothTime, 0);
            } else {
                set(trans, 0, 0, 0);
            }
        }

        @Override
        public void getOrigin(String partName, float[] orig) {
            if ("hatch".equals(partName)) {
                set(orig, 0F, 0.875F, -2.875F);
            } else {
                set(orig, 0, 0, 0);
            }
        }

        @Override
        public void getRotation(String partName, float openTicks, float[] rot) {
            if ("hatch".equals(partName)) {
                float smoothTime = smoothstep(getNormTime(openTicks, 20, 100));
                set(rot, smoothTime * -240, 0, 0);
            } else {
                super.getRotation(partName, openTicks, rot);
            }
        }

        @Override public float getDoorRangeOpenTime(int ticks, int idx) {
            return getNormTime(ticks, 20, 20);
        }

        @Override public int getBlockOffset() { return 3; }

        @Override public int[][] getDoorOpenRanges() {
            return new int[][] { 
                { 2, 0, 1, -3, 3, 0 }, { 1, 0, 2, -5, 3, 0 }, { 0, 0, 2, -5, 3, 0 }, 
                { -1, 0, 2, -5, 3, 0 }, { -2, 0, 1, -3, 3, 0 } 
            };
        }

        @Override public int[] getDimensions() {
            return new int[] { 0, 0, 3, 3, 3, 3 };
        }

        @Override
        public List<AABB> getCollisionBounds(float progress, Direction facing) {
            List<AABB> bounds = new ArrayList<>();
            if (progress >= 0.99f) return bounds;
            
            // Большой люк силоса
            if (progress < 0.5) { // Люк еще не открыт полностью
                AABB hatch = new AABB(-2.0, 3.0, -2.0, 3.0, 4.0, 3.0);
                bounds.add(rotateAABB(hatch, facing));
            }
            return bounds;
        }

        private float smoothstep(float t) {
            return t * t * (3.0f - 2.0f * t);
        }

        @Override public SoundEvent getOpenSoundEnd() { return ModSounds.WGH_STOP.get(); }
        @Override public SoundEvent getOpenSoundLoop() { return ModSounds.WGH_START.get(); }
        @Override public SoundEvent getCloseSoundEnd() { return ModSounds.WGH_STOP.get(); }
        @Override public float getSoundVolume() { return 2.0f; }
    };
}
