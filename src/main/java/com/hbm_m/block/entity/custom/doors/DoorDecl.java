package com.hbm_m.block.entity.custom.doors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.AABB;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.client.loader.ColladaAnimationParser;
import com.hbm_m.client.model.DoorBakedModel;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.sound.ModSounds;

import java.util.ArrayList;
import java.util.Collections;
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

    // Кэш извлечённых AABB частей из OBJ модели (клиентский доступ)
    private Map<String, AABB> cachedPartAABBs = new HashMap<>();

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
    public int[] getDimensions() {
        return switch (getBlockId().getPath()) {
            case "large_vehicle_door" -> new int[] { -3, 0, 0, 6, 5, 0 };
            case "round_airlock_door" -> new int[] { -1, 0, 0, 3, 3, 0 };
            case "transition_seal" -> new int[] { -12, 0, 0, 25, 23, 0 };
            case "fire_door" -> new int[] { -1, 0, 0, 3, 2, 0 };
            case "sliding_blast_door" -> new int[] { -3, 0, 0, 6, 3, 0 };
            case "sliding_seal_door" -> new int[] { 0, 0, 0, 0, 1, 0 };
            case "secure_access_door" -> new int[] { -2, 0, 0, 4, 4, 0 };
            case "qe_sliding_door" -> new int[] { 0, 0, 0, 1, 1, 0 };
            case "qe_containment_door" -> new int[] { -1, 0, 0, 2, 2, 0 };
            case "water_door" -> new int[] { -1, 0, 0, 2, 2, 0 };
            case "silo_hatch" -> new int[] { -2, 0, -2, 4, 0, 4 };
            case "silo_hatch_large" -> new int[] { -3, 0, -3, 6, 0, 6 };
            default -> new int[] { 0, 0, 0, 0, 1, 0 };
        };
    }

    /**
     * ID блока для автоматической загрузки модели
     */
    public abstract ResourceLocation getBlockId();

    public abstract List<AABB> getCollisionBounds(float progress, Direction facing);

    // ==================== Автоматическая загрузка моделей ====================

    public void setPartsFromModel(String[] partNames, Map<String, BakedModel> parts) {
        this.cachedPartNames = partNames;
        this.modelCache.putAll(parts);
        
        cachedModelParts = new BakedModel[partNames.length];
        for (int i = 0; i < partNames.length; i++) {
            cachedModelParts[i] = parts.get(partNames[i]);
        }
        
        MainRegistry.LOGGER.debug("DoorDecl: Set {} parts from DoorBakedModel for door {}", 
            partNames.length, getBlockId());
    }

    /* 
     * Возвращает массив имен частей модели
     * Автоматически загружается из JSON файла модели
     */
    /**
     * Возвращает массив имен частей модели
     * Автоматически загружается из DoorBakedModel при первом рендере
     */
    public String[] getPartNames() {
        if (cachedPartNames == null) {
            // Fallback на дефолтные части, пока модель не загружена
            MainRegistry.LOGGER.warn("DoorDecl.getPartNames() called before model loaded for {}, using fallback", 
                getBlockId());
            return new String[] { "frame", "door" };
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
     * Сохраняет извлечённые AABB (вызывается из DoorBakedModel).
     */
    public void setPartAABBs(Map<String, AABB> aabbs) {
        this.cachedPartAABBs = new HashMap<>(aabbs);
    }

    /**
     * Получает базовый AABB для части (клиентский доступ).
     */
    @Nullable
    public AABB getPartAABB(String partName) {
        return cachedPartAABBs.get(partName);
    }

    /**
     * Получает все базовые AABB (клиентский доступ).
     */
    public Map<String, AABB> getAllPartAABBs() {
        return Collections.unmodifiableMap(cachedPartAABBs);
    }

    /**
     * Проверяет, загружены ли AABB для этой двери.
     */
    public boolean hasPartAABBs() {
        return !cachedPartAABBs.isEmpty();
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

    protected double[] getDoorCenter() {
        int[] dims = getDimensions();

        int offsetX = dims[0];
        int offsetZ = dims[2];
        int sizeX = dims[3];
        int sizeZ = dims[5];
        
        // Вычисляем центр на основе реального диапазона структуры
        double centerX = (offsetX + (offsetX + sizeX)) / 2.0;
        double centerZ = (offsetZ + (offsetZ + sizeZ)) / 2.0;
        
        return new double[] { centerX, centerZ };
    }    

    protected static AABB rotateAABB(AABB bb, Direction facing) {
        double[][] pts = {
            {bb.minX, bb.minZ}, {bb.maxX, bb.minZ},
            {bb.minX, bb.maxZ}, {bb.maxX, bb.maxZ}
        };
        double minX = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        
        for (double[] p : pts) {
            double x = p[0], z = p[1];
            double rx, rz;
            
            // ИСПРАВЛЕНО: используем ТУ ЖЕ логику, что и в MultiblockStructureHelper.rotate()
            switch (facing) {
                case SOUTH -> { rx = -x; rz = -z; }
                case WEST  -> { rx = z; rz = -x; }  // ИСПРАВЛЕНО
                case EAST  -> { rx = -z; rz = x; }  // ИСПРАВЛЕНО
                default    -> { rx = x; rz = z; }   // NORTH
            }
            
            if (rx < minX) minX = rx; if (rx > maxX) maxX = rx;
            if (rz < minZ) minZ = rz; if (rz > maxZ) maxZ = rz;
        }
        
        return new AABB(minX, bb.minY, minZ, maxX, bb.maxY, maxZ);
    }
    

    // Возвращает локальные AABB створок относительно контроллера (без поворота/переноса)
    public List<AABB> getLocalDynamicBoxes(float progress) {
        int[] d = getDimensions();
        final double ox = d[0], oy = d[1], oz = d[2];
        final int sx = d[3], sy = d[4], sz = d[5];
        
        // ИСПРАВЛЕНО: учитываем, что структура создается с <=
        final double minX = ox;
        final double maxX = ox + sx + 1;  // +1 из-за <= в createStructureForDoor
        final double minY = oy;
        final double maxY = oy + sy + 1;  // +1 из-за <=
        final double minZ = oz;
        final double maxZ = oz + (sz == 0 ? 1 : sz + 1);  // +1 для толщины или из-за <=
        
        final double totalWidth = maxX - minX;
        final double half = totalWidth / 2.0;
        final double centerX = minX + half;
        
        final double move = Math.min(Math.max(progress, 0f), 1f) * half;
        
        // Пороги разъезда створок
        final double cutLeft = centerX - move;
        final double cutRight = centerX + move;
        
        List<AABB> boxes = new ArrayList<>();
        
        // Создаем коллижн боксы для створок
        for (double x = minX; x < maxX; x += 1.0) {
            double cellMinX = x;
            double cellMaxX = x + 1.0;
            
            // Левая створка: часть ячейки слева от cutLeft
            double leftPartMaxX = Math.min(cellMaxX, cutLeft);
            if (leftPartMaxX - cellMinX > 1e-6) {
                boxes.add(new AABB(cellMinX, minY, minZ, leftPartMaxX, maxY, maxZ));
            }
            
            // Правая створка: часть ячейки справа от cutRight
            double rightPartMinX = Math.max(cellMinX, cutRight);
            if (cellMaxX - rightPartMinX > 1e-6) {
                boxes.add(new AABB(rightPartMinX, minY, minZ, cellMaxX, maxY, maxZ));
            }
        }
        
        return boxes;
    }    

    // ==================== Реализации ====================

    public static final DoorDecl LARGE_VEHICLE_DOOR = new DoorDecl() {
        @Override
        public ResourceLocation getBlockId() {
            return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "large_vehicle_door");
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

        @Override
        public List<AABB> getCollisionBounds(float progress, Direction facing) {
            List<AABB> bounds = new ArrayList<>();
            if (progress >= 0.99f) {
                return bounds;
            }
            
            // ИСПРАВЛЕНО: используем правильные границы структуры
            int[] d = getDimensions();
            double minX = d[0];           // -3
            double minY = d[1];           // 0
            double minZ = d[2];           // 0
            double maxX = d[0] + d[3];    // -3 + 6 = 3 (но реально 7 блоков из-за <=)
            double maxY = d[1] + d[4];    // 0 + 5 = 5
            double maxZ = d[2] + (d[5] == 0 ? 1 : d[5]); // 0 + 1 = 1
            
            // ВАЖНО: структура создается с <=, поэтому добавляем +1 к maxX
            maxX += 1;  // теперь maxX = 4, что соответствует реальным блокам [-3..3]
            
            double totalWidth = maxX - minX;  // 7 блоков
            double half = totalWidth / 2.0;   // 3.5
            double centerX = minX + half;     // -3 + 3.5 = 0.5
            
            double move = Math.min(progress, 1.0) * half;
            
            // Левая створка: от minX до (centerX - move)
            double leftMaxX = centerX - move;
            if (leftMaxX > minX + 0.05) {
                AABB leftDoor = new AABB(minX, minY, minZ, leftMaxX, maxY, maxZ);
                bounds.add(rotateAABB(leftDoor, facing));
            }
            
            // Правая створка: от (centerX + move) до maxX
            double rightMinX = centerX + move;
            if (rightMinX < maxX - 0.05) {
                AABB rightDoor = new AABB(rightMinX, minY, minZ, maxX, maxY, maxZ);
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
        public ResourceLocation getBlockId() {
            return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "round_airlock_door");
        }

        @Override public int getOpenTime() { return 60; }

        @Override
        public void getTranslation(String partName, float openTicks, boolean child, float[] trans) {
            if ("doorLeft".equals(partName)) {
                set(trans, 0, 0, 1.5F * getNormTime(openTicks));  // Движение по Z вперед
            } else if ("doorRight".equals(partName)) {
                set(trans, 0, 0, -1.5F * getNormTime(openTicks)); // Движение по Z назад
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

        @Override
        public List<AABB> getCollisionBounds(float progress, Direction facing) {
            List<AABB> bounds = new ArrayList<>();
            if (progress >= 0.99f) {
                return bounds; // Полностью открыта
            }

            // Получаем РЕАЛЬНЫЕ dimensions из DoorBlock
            int[] d = getDimensions(); // [-1, 0, 0, 3, 3, 0]
            double ox = d[0];  // -1
            double oy = d[1];  // 0
            double oz = d[2];  // 0
            double sx = d[3];  // 3 (итого 4 блока: -1, 0, 1, 2)
            double sy = d[4];  // 3 (итого 4 блока высоты)
            double sz = d[5];  // 0 (итого 1 блок глубины)

            double half = sx / 2.0;  // 1.5
            double move = Math.min(progress, 1.0) * half;  // максимум 1.5 блока

            // Левая створка: X от ox до (ox + half - move)
            double leftMaxX = Math.max(ox, ox + half - move);
            if (leftMaxX - ox > 0.05) {
                // X: от -1.0 до (-1.0 + 1.5 - move)
                // Y: от 0.0 до 4.0
                // Z: от 0.0 до 1.0
                AABB leftDoor = new AABB(ox, oy, oz, leftMaxX, oy + sy, oz + sz + 1.0);
                bounds.add(rotateAABB(leftDoor, facing));
            }

            // Правая створка: X от (ox + half + move) до (ox + sx)
            double rightMinX = Math.min(ox + sx, ox + half + move);
            if (ox + sx - rightMinX > 0.05) {
                // X: от (0.5 + move) до 2.0
                // Y: от 0.0 до 4.0
                // Z: от 0.0 до 1.0
                AABB rightDoor = new AABB(rightMinX, oy, oz, ox + sx, oy + sy, oz + sz + 1.0);
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
        public ResourceLocation getBlockId() {
            return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "transition_seal");
        }
    
        @Override 
        public int getOpenTime() { 
            return 480; 
        }
        
        private Map<String, List<ColladaAnimationParser.AnimationChannel>> animations = Collections.emptyMap();
        private boolean animationsInitialized = false;
    
        private void initializeAnimations() {
            if (animationsInitialized) return;
            animationsInitialized = true;
            
            try {
                // Проверяем, доступен ли Minecraft (не доступен во время генерации данных)
                if (Minecraft.getInstance() == null) {
                    animations = Collections.emptyMap();
                    return;
                }
                
                ResourceManager rm = Minecraft.getInstance().getResourceManager();
                Resource resource = rm.getResource(
                    ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/sliding_blast_door.dae")
                ).orElse(null);

                if (resource == null) {
                    MainRegistry.LOGGER.error("DoorDecl: missing sliding_blast_door.dae for {}", getBlockId());
                    animations = Collections.emptyMap();
                } else {
                    animations = ColladaAnimationParser.parse(resource.open());
                }
            } catch (Exception e) {
                e.printStackTrace();
                animations = Collections.emptyMap();
            }
        }
        
        @Override
        public void getTranslation(String partName, float openTicks, boolean child, float[] trans) {
            initializeAnimations();
            if (!animations.containsKey(partName)) {
                super.getTranslation(partName, openTicks, child, trans);
                return;
            }
            
            float progress = getNormTime(openTicks);
            float time = progress * (getOpenTime() / 20.0f); // Конвертируем в секунды
            
            for (ColladaAnimationParser.AnimationChannel channel : animations.get(partName)) {
                if (channel.property.startsWith("location")) {
                    float value = channel.getValue(time);
                    
                    if (channel.property.endsWith(".X")) trans[0] = value;
                    else if (channel.property.endsWith(".Y")) trans[1] = value;
                    else if (channel.property.endsWith(".Z")) trans[2] = value;
                }
            }
        }
        
        @Override
        public void getRotation(String partName, float openTicks, float[] rot) {
            initializeAnimations();
            if (!animations.containsKey(partName)) {
                super.getRotation(partName, openTicks, rot);
                return;
            }
            
            float progress = getNormTime(openTicks);
            float time = progress * (getOpenTime() / 20.0f);
            
            for (ColladaAnimationParser.AnimationChannel channel : animations.get(partName)) {
                if (channel.property.startsWith("rotation")) {
                    float value = (float) Math.toDegrees(channel.getValue(time)); // COLLADA в радианах
                    
                    if (channel.property.endsWith(".X")) rot[0] = value;
                    else if (channel.property.endsWith(".Y")) rot[1] = value;
                    else if (channel.property.endsWith(".Z")) rot[2] = value;
                }
            }
        }
        
        @Override
        public void doOffsetTransform(LegacyAnimator animator) {
            animator.translate(0.0f, 0.0f, 0.5f);
        }
        
        @Override 
        public int[][] getDoorOpenRanges() {
            return new int[][] { { -9, 2, 0, 20, 20, 1 } };
        }
    
        @Override
        public List<AABB> getCollisionBounds(float progress, Direction facing) {
            List<AABB> bounds = new ArrayList<>();
            if (progress >= 0.99f) return bounds;
            
            double movement = progress * 3.5;
            AABB door = new AABB(-9.0, 0.0, 0.0, 11.0, Math.max(0.5, 20.0 - movement), 1.0);
            bounds.add(rotateAABB(door, facing));
            return bounds;
        }
    
        @Override 
        public SoundEvent getOpenSoundStart() { 
            return ModSounds.TRANSITION_SEAL_OPEN.get(); 
        }
        
        @Override 
        public float getSoundVolume() { 
            return 6.0f; 
        }
    };
    

    public static final DoorDecl FIRE_DOOR = new DoorDecl() {
        @Override
        public ResourceLocation getBlockId() {
            return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "fire_door");
        }

        @Override public int getOpenTime() { return 160; }

        @Override
        public void getTranslation(String partName, float openTicks, boolean child, float[] trans) {
            if ("door".equals(partName) || !"frame".equals(partName)) {
                set(trans, 0, 3.0F * getNormTime(openTicks), 0);
            } else {
                set(trans, 0, 0, 0);  // frame остаётся на месте
            }
        }

        @Override public double[][] getClippingPlanes() {
            return new double[][] { { 0, -1, 0, 3.0001 } };
        }

        @Override public int[][] getDoorOpenRanges() {
            return new int[][] { { -1, 0, 0, 3, 4, 1 } };
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
        public ResourceLocation getBlockId() {
            return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "sliding_blast_door");
        }
    
        // Кеш загруженных анимаций - ПОТОКОБЕЗОПАСНЫЙ
        private volatile Map<String, List<ColladaAnimationParser.AnimationChannel>> animations = null;
        private final Object animationLock = new Object();
        private volatile Float cachedAnimationDuration = null;
    
        private Map<String, List<ColladaAnimationParser.AnimationChannel>> getAnimations() {
            if (animations == null) {
                synchronized (animationLock) {
                    if (animations == null) {
                        try {
                            ResourceManager rm = Minecraft.getInstance().getResourceManager();
                            Resource resource = rm.getResource(
                                ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/sliding_blast_door.dae")
                            ).orElseThrow();
                            animations = ColladaAnimationParser.parse(resource.open());
                            
                            // Вычисляем максимальную длительность
                            float maxTime = 0f;
                            for (List<ColladaAnimationParser.AnimationChannel> channels : animations.values()) {
                                for (ColladaAnimationParser.AnimationChannel channel : channels) {
                                    if (channel.times != null && channel.times.length > 0) {
                                        maxTime = Math.max(maxTime, channel.times[channel.times.length - 1]);
                                    }
                                }
                            }
                            cachedAnimationDuration = maxTime > 0 ? maxTime : 1.25f;
                            
                            MainRegistry.LOGGER.info("Loaded animations for SLIDE_DOOR: {} parts, duration: {}s", 
                                animations.size(), cachedAnimationDuration);
                        } catch (Exception e) {
                            MainRegistry.LOGGER.error("Failed to load SLIDE_DOOR animations", e);
                            animations = Collections.emptyMap();
                            cachedAnimationDuration = 1.25f;
                        }
                    }
                }
            }
            return animations;
        }
    
        @Override
        public int getOpenTime() {
            Map<String, List<ColladaAnimationParser.AnimationChannel>> anims = getAnimations();
            if (cachedAnimationDuration != null && cachedAnimationDuration > 0) {
                return Math.round(cachedAnimationDuration * 20f); // 1.25s * 20 = 25 тиков
            }
            return 25;
        }
    
        @Override
        public void getTranslation(String partName, float openTicks, boolean child, float[] trans) {
            Map<String, List<ColladaAnimationParser.AnimationChannel>> anims = getAnimations();
            
            // Инициализируем нулями (КРИТИЧНО: закрытое состояние по умолчанию!)
            trans[0] = 0f;
            trans[1] = 0f;
            trans[2] = 0f;
            
            if (anims.isEmpty() || !anims.containsKey(partName)) {
                return;
            }
    
            float progress = getNormTime(openTicks);
            float time = progress * cachedAnimationDuration;
    
            // Ищем канал transform (матричная анимация)
            for (ColladaAnimationParser.AnimationChannel channel : anims.get(partName)) {
                if ("transform".equals(channel.property)) {
                    // Извлекаем translation из матрицы
                    float[] translation = channel.getTranslationFromMatrix(time);
                    trans[0] = translation[0];
                    trans[1] = translation[1];
                    trans[2] = translation[2];
                    
                    MainRegistry.LOGGER.debug("Part {} at time {}: translation = [{}, {}, {}]",
                        partName, time, trans[0], trans[1], trans[2]);
                    return;
                }
                
                // Fallback: если есть отдельные каналы location
                if (channel.property.startsWith("location")) {
                    float value = channel.getValue(time);
                    if (channel.property.endsWith(".X")) trans[0] = value;
                    else if (channel.property.endsWith(".Y")) trans[1] = value;
                    else if (channel.property.endsWith(".Z")) trans[2] = value;
                }
            }
        }
    
        @Override
        public void doOffsetTransform(LegacyAnimator animator) {
            animator.rotate(-90.0f, 0.0f, 1.0f, 0.0f);
        }
    
        @Override
        public double[][] getClippingPlanes() {
            return new double[][] { 
                { -1, 0, 0, 3.50001 }, 
                { 1, 0, 0, 3.50001 } 
            };
        }
    
        @Override
        public int[][] getDoorOpenRanges() {
            return new int[][] { { -2, 0, 0, 4, 5, 1 } };
        }
    
        @Override
        public boolean hasSkins() {
            return true;
        }
    
        @Override
        public int getSkinCount() {
            return 3;
        }
    
        @Override
        public List<AABB> getCollisionBounds(float progress, Direction facing) {
            List<AABB> bounds = new ArrayList<>();
            if (progress >= 0.99f) return bounds;
            
            double movement = progress * 1.5;
            if (movement < 1.0) {
                AABB door = new AABB(-2.0, 0.0, 0.0, 3.0, 5.0, 1.0 - movement);
                bounds.add(rotateAABB(door, facing));
            }
            return bounds;
        }
    
        @Override
        public SoundEvent getOpenSoundEnd() {
            return ModSounds.SLIDING_DOOR_OPENED.get();
        }
    
        @Override
        public SoundEvent getCloseSoundEnd() {
            return ModSounds.SLIDING_DOOR_SHUT.get();
        }
    
        @Override
        public SoundEvent getOpenSoundLoop() {
            return ModSounds.SLIDING_DOOR_OPENING.get();
        }
    
        @Override
        public float getSoundVolume() {
            return 2.0f;
        }
    };
    
    public static final DoorDecl SLIDING_SEAL_DOOR = new DoorDecl() {
        @Override
        public ResourceLocation getBlockId() {
            return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "sliding_seal_door");
        }

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
        public ResourceLocation getBlockId() {
            return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "secure_access_door");
        }
    
        @Override public int getOpenTime() { return 120; }
        
        @Override
        public void getTranslation(String partName, float openTicks, boolean child, float[] trans) {
            // ИСПРАВЛЕНО: "base" вместо "frame" - соответствует старой логике
            if(!partName.equals("base")) {
                set(trans, 0, 3.5F * getNormTime(openTicks), 0);
            } else {
                super.getTranslation(partName, openTicks, child, trans);
            }
        }
        
        // ДОБАВЛЕНО: отсутствующий метод трансформации координат
        @Override
        public void doOffsetTransform(LegacyAnimator animator) {
            // Поворот на 90° вокруг оси Y (как в старой версии)
            animator.rotate(90.0f, 0.0f, 1.0f, 0.0f);
        }
        
        @Override public double[][] getClippingPlanes() {
            return new double[][] { { 0, -1, 0, 5 } };
        }
    
        @Override public int[][] getDoorOpenRanges() {
            return new int[][] { { -2, 1, 0, 4, 5, 1 } };
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
        public ResourceLocation getBlockId() {
            return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "qe_sliding_door");
        }

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
        public ResourceLocation getBlockId() {
            return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "qe_containment_door");
        }
    
        @Override
        public ResourceLocation getTextureForPart(int skinIndex, String partName) {
            if ("decal".equals(partName)) {
                // Возвращаем текстуру декали
                return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, 
                    "block/qe_containment_decal");
            }
            // Основная текстура для остальных частей
            return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, 
                "block/qe_containment_door");
        }
    
        @Override 
        public int getOpenTime() { 
            return 160; 
        }
    
        @Override
        public void getTranslation(String partName, float openTicks, boolean child, float[] trans) {
            // Декаль движется вместе с дверью
            if ("door".equals(partName) || "decal".equals(partName)) {
                set(trans, 0, 3.0F * getNormTime(openTicks), 0);
            } else {
                super.getTranslation(partName, openTicks, child, trans);
            }
        }
    
        @Override 
        public double[][] getClippingPlanes() {
            return new double[][] { { 0, -1, 0, 3.0001 } };
        }
    
        @Override 
        public int[][] getDoorOpenRanges() {
            return new int[][] { { -1, 0, 0, 3, 3, 1 } };
        }
    
        @Override
        public List<AABB> getCollisionBounds(float progress, Direction facing) {
            List<AABB> bounds = new ArrayList<>();
            if (progress >= 0.99f) return bounds;
            
            double movement = progress * 3.0;
            AABB door = new AABB(-1.0, 0.0, 0.5, 2.0, Math.max(0.1, 3.0 - movement), 1.0);
            bounds.add(rotateAABB(door, facing));
            return bounds;
        }
    
        @Override 
        public SoundEvent getOpenSoundEnd() { 
            return ModSounds.WGH_STOP.get(); 
        }
        
        @Override 
        public SoundEvent getOpenSoundLoop() { 
            return ModSounds.WGH_START.get(); 
        }
        
        @Override 
        public float getSoundVolume() { 
            return 2.0f; 
        }
    };    

    public static final DoorDecl WATER_DOOR = new DoorDecl() {
        @Override
        public ResourceLocation getBlockId() {
            return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "water_door");
        }
    
        @Override
        public int getOpenTime() { 
            return 60; 
        }
    
        @Override
        public void getTranslation(String partName, float openTicks, boolean child, float[] trans) {
            // bolt движется вперед (Z направление) в первой фазе (0-30 тиков)
            if ("bolt".equals(partName)) {
                float boltProgress = Math.min(1.0f, openTicks / 30.0f);
                set(trans, 0, 0, 0.4f * smoothstep(boltProgress));
            } else {
                set(trans, 0, 0, 0);
            }
        }
    
        @Override
        public void getOrigin(String partName, float[] orig) {
            if ("door".equals(partName) || "bolt".equals(partName)) {
                set(orig, 0.125F, 1.5F, 1.18F); // bolt вращается вокруг той же оси, что и дверь
                return;
            } else if ("spinny_upper".equals(partName)) {
                set(orig, 0.041499F, 2.43569F, -0.587849F);
                return;
            } else if ("spinny_lower".equals(partName)) {
                set(orig, 0.041499F, 0.571054F, -0.587849F);
                return;
            }
            super.getOrigin(partName, orig);
        }
    
        @Override
        public void getRotation(String partName, float openTicks, float[] rot) {
            // Вентили вращаются ТОЛЬКО в первой фазе (0-30 тиков)
            if (partName.startsWith("spinny")) {
                float spinnyProgress = Math.min(1.0f, openTicks / 30.0f);
                set(rot, smoothstep(spinnyProgress) * 360.0f, 0.0f, 0.0f);
                return;
            } 
            // Дверь и bolt вращаются ВО ВТОРОЙ фазе (30-60 тиков)
            else if ("door".equals(partName) || "bolt".equals(partName)) {
                float doorProgress = Math.max(0.0f, Math.min(1.0f, (openTicks - 30.0f) / 30.0f));
                set(rot, 0.0f, smoothstep(doorProgress) * -134.0f, 0.0f);
                return;
            }
            super.getRotation(partName, openTicks, rot);
        }
    
        @Override
        public boolean doesRender(String partName, boolean child) {
            return child || !partName.startsWith("spinny");
        }
    
        @Override
        public String[] getChildren(String partName) {
            if ("door".equals(partName)) {
                return new String[] { "spinny_lower", "spinny_upper" };
            }
            return super.getChildren(partName);
        }
    
        @Override
        public float getDoorRangeOpenTime(int ticks, int idx) {
            return getNormTime(ticks, 35, 40);
        }
    
        @Override
        public int[][] getDoorOpenRanges() {
            // Фантомные блоки для обнаружения игроков/сущностей
            return new int[][] { { 1, 0, 0, -3, 3, 2 } };
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
            // Плавная интерполяция (кубический Эрмит)
            return t * t * (3.0f - 2.0f * t);
        }

        @Override 
        public SoundEvent getOpenSoundEnd() { 
            return ModSounds.WGH_STOP.get();
        }
        
        @Override 
        public SoundEvent getOpenSoundLoop() { 
            return ModSounds.WGH_START.get();
        }
        
        @Override 
        public SoundEvent getOpenSoundStart() { 
            return ModSounds.LEVER_1.get(); 
        }
        
        @Override 
        public SoundEvent getCloseSoundEnd() { 
            return ModSounds.LEVER_1.get(); 
        }
        
        @Override 
        public float getSoundVolume() { 
            return 2.0f; 
        }
    };    

    public static final DoorDecl SILO_HATCH = new DoorDecl() {
        @Override
        public ResourceLocation getBlockId() {
            return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "silo_hatch");
        }
    
        @Override public int getOpenTime() { return 60; }
        @Override public boolean remoteControllable() { return true; }
        
        @Override
        public void getTranslation(String partName, float openTicks, boolean child, float[] trans) {
            if ("Hatch".equals(partName)) {
                float smoothTime = smoothstep(getNormTime(openTicks, 0, 10));
                set(trans, 0, 0.25F * smoothTime, 0);
            } else {
                set(trans, 0, 0, 0);
            }
        }
        
        @Override
        public void getOrigin(String partName, float[] orig) {
            if ("Hatch".equals(partName)) {
                set(orig, 0F, 0.875F, -1.875F);
            } else {
                set(orig, 0, 0, 0);
            }
        }
        
        @Override
        public void getRotation(String partName, float openTicks, float[] rot) {
            if ("Hatch".equals(partName)) {
                float smoothTime = smoothstep(getNormTime(openTicks, 20, 100));
                set(rot, smoothTime * -240, 0, 0);
            } else {
                super.getRotation(partName, openTicks, rot);
            }
        }
        
        @Override public float getDoorRangeOpenTime(int ticks, int idx) { 
            return getNormTime(ticks, 0, 20);   // ✓ от 0 до 20 тиков
        }        
        
        @Override public int getBlockOffset() { return 2; }
        
        @Override public int[][] getDoorOpenRanges() {
            return new int[][] { { 1, 0, 1, -3, 3, 0 }, { 0, 0, 1, -3, 3, 0 }, { -1, 0, 1, -3, 3, 0 } };
        }
        
        @Override
        public List<AABB> getCollisionBounds(float progress, Direction facing) {
            List<AABB> bounds = new ArrayList<>();
            if (progress >= 0.99f) return bounds;
            
            if (progress < 0.5) {
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
        public ResourceLocation getBlockId() {
            return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "silo_hatch_large");
        }

        @Override public int getOpenTime() { return 60; }

        @Override public boolean remoteControllable() { return true; }

        @Override
        public void getTranslation(String partName, float openTicks, boolean child, float[] trans) {
            if ("Hatch".equals(partName)) {
                float smoothTime = smoothstep(getNormTime(openTicks, 0, 10));
                set(trans, 0, 0.25F * smoothTime, 0);
            } else {
                set(trans, 0, 0, 0);
            }
        }

        @Override
        public void getOrigin(String partName, float[] orig) {
            if ("Hatch".equals(partName)) {
                set(orig, 0F, 0.875F, -2.875F);
            } else {
                set(orig, 0, 0, 0);
            }
        }

        @Override
        public void getRotation(String partName, float openTicks, float[] rot) {
            if ("Hatch".equals(partName)) {
                float smoothTime = smoothstep(getNormTime(openTicks, 20, 100));
                set(rot, smoothTime * -240, 0, 0);
            } else {
                super.getRotation(partName, openTicks, rot);
            }
        }

        @Override public float getDoorRangeOpenTime(int ticks, int idx) { 
            return getNormTime(ticks, 0, 20);   // ✓ от 0 до 20 тиков
        }        

        @Override public int getBlockOffset() { return 3; }

        @Override public int[][] getDoorOpenRanges() {
            return new int[][] { 
                { 2, 0, 1, -3, 3, 0 }, { 1, 0, 2, -5, 3, 0 }, { 0, 0, 2, -5, 3, 0 }, 
                { -1, 0, 2, -5, 3, 0 }, { -2, 0, 1, -3, 3, 0 } 
            };
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
