package com.hbm_m.block.entity.custom.doors;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.client.loader.ColladaAnimationParser;
// import com.hbm_m.client.model.DoorBakedModel;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.lib.RefStrings;
// import com.hbm_m.main.MainRegistry;
import com.hbm_m.multiblock.PartRole;
import com.hbm_m.sound.ModSounds;

// import net.minecraft.client.Minecraft;
// import net.minecraft.client.resources.model.BakedModel;
// import net.minecraft.client.resources.model.ModelManager;
// import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
// import net.minecraft.server.packs.resources.Resource;
// import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Декларативный класс параметров двери.
 * Модели загружаются автоматически из blockstate JSON файлов.
 */

public abstract class DoorDecl {
    // Кеш загруженных моделей
    // private final Map<String, BakedModel> modelCache = new HashMap<>();
    // private BakedModel[] cachedModelParts = null;
    // private String[] cachedPartNames = null;

    protected DoorStructureDefinition structureDefinition;


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
    public int[][] getDoorOpenRanges() { return new int[0][]; }

    /**
     * Инициализация схемы. Должна вызываться в конструкторе конкретной двери.
     */
    protected void defineStructure(DoorStructureDefinition definition) {
        this.structureDefinition = definition;
    }

    public DoorStructureDefinition getStructureDefinition() {
        return structureDefinition;
    }

    @Nullable
    public VoxelShape getDynamicOutline(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context, float progress) {
        // Если дверь открыта больше чем на 90%, можно уменьшить рамку выделения,
        // чтобы она не мешала кликать по объектам за дверью.
        if (progress > 0.9f) return Shapes.empty(); 
        return null; 
    }

    /**
     * Размеры мультиблока двери
     */
    public int[] getDimensions() {
        // 1. Если есть схема - берем размеры из неё
        if (structureDefinition != null) {
            return structureDefinition.getDimensions();
        }

        // 2. Fallback для старых дверей без схем
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

    public boolean isDynamicShape() {
        return false;
    }

    /**
     * ID блока для автоматической загрузки модели
     */
    public abstract ResourceLocation getBlockId();

    public abstract String[] getPartNames();

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
    // @Nullable
    // public BakedModel getModelPart(String partName) {
    //     if (modelCache.containsKey(partName)) {
    //         return modelCache.get(partName);
    //     }

    //     // Для дверей используем DoorBakedModel напрямую
    //     try {
    //         ModelManager modelManager = Minecraft.getInstance().getModelManager();
    //         ModelResourceLocation modelLocation = new ModelResourceLocation(getBlockId(), "");
    //         BakedModel baseModel = modelManager.getModel(modelLocation);
            
    //         if (baseModel instanceof DoorBakedModel doorModel) {
    //             BakedModel partModel = doorModel.getPart(partName);
    //             if (partModel != null) {
    //                 modelCache.put(partName, partModel);
    //                 return partModel;
    //             }
    //         }
            
    //         MainRegistry.LOGGER.warn("Could not find part '{}' in door model", partName);
    //         return null;
            
    //     } catch (Exception e) {
    //         MainRegistry.LOGGER.error("Error loading door part model for '{}'", partName, e);
    //         return null;
    //     }
    // }

    /**
     * Возвращает текстуру для конкретной части
     * Теперь берется из BakedModel автоматически!
     */
    // public ResourceLocation getTextureForPart(int skinIndex, String partName) {
    //     BakedModel model = getModelPart(partName);
    //     if (model != null && !model.getQuads(null, null, null).isEmpty()) {
    //         // Текстура берется из BakedModel, не нужно хардкодить
    //         return model.getParticleIcon().atlasLocation();
    //     }

    //     // Fallback: используем blockstate атлас
    //     return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/atlas/blocks.png");
    // }

    // public ResourceLocation getTextureForPart(String partName) {
    //     return getTextureForPart(0, partName);
    // }

    public int getBlockOffset() { return 0; }

    public boolean remoteControllable() { return false; }

    // public float getDoorRangeOpenTime(int currentTick, int rangeIndex) {
    //     return getNormTime(currentTick);
    // }

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
            
            switch (facing) {
                case SOUTH -> { rx = -x; rz = -z; }
                case WEST  -> { rx = z; rz = -x; }
                case EAST  -> { rx = -z; rz = x; }
                default    -> { rx = x; rz = z; }   // NORTH
            }
            
            if (rx < minX) minX = rx; if (rx > maxX) maxX = rx;
            if (rz < minZ) minZ = rz; if (rz > maxZ) maxZ = rz;
        }
        
        return new AABB(minX, bb.minY, minZ, maxX, bb.maxY, maxZ);
    }


    // Вспомогательный класс для определения структуры двери

    public static class DoorStructureDefinition {
        // Карта: Локальная позиция -> Коллизия в ЗАКРЫТОМ состоянии
        private final Map<BlockPos, VoxelShape> closedShapes = new HashMap<>();
        // Карта: Локальная позиция -> Коллизия в ОТКРЫТОМ состоянии
        private final Map<BlockPos, VoxelShape> openShapes = new HashMap<>();
        // Карта: Локальная позиция -> Роль части
        private final Map<BlockPos, PartRole> roles = new HashMap<>();
        
        // Размеры (вычисляются автоматически)
        private int minX, minY, minZ, maxX, maxY, maxZ;
        private BlockPos controllerOffset = BlockPos.ZERO;

        private DoorStructureDefinition() {}

        public static Builder create() { return new Builder(); }

        public Map<BlockPos, VoxelShape> getClosedShapes() { return closedShapes; }
        public Map<BlockPos, VoxelShape> getOpenShapes() { return openShapes; }
        public Map<BlockPos, PartRole> getRoles() { return roles; }
        public BlockPos getControllerOffset() { return controllerOffset; }
        
        // Возвращает массив [offsetX, offsetY, offsetZ, sizeX, sizeY, sizeZ] для DoorBlock
        public int[] getDimensions() {
            return new int[] { 
                minX, minY, minZ, 
                maxX - minX, maxY - minY, maxZ - minZ 
            };
        }

        public static class Builder {
            private final Map<Character, VoxelShape> symbolShapes = new HashMap<>();
            private final Map<Character, PartRole> symbolRoles = new HashMap<>();
            
            public Builder() {
                // Символ по умолчанию для воздуха/прохода
                addSymbol(' ', Shapes.empty(), PartRole.DEFAULT);
                // Символ для обычного полного блока
                addSymbol('#', Shapes.block(), PartRole.DEFAULT);
            }

            public Builder addSymbol(char symbol, VoxelShape shape, PartRole role) {
                symbolShapes.put(symbol, shape);
                symbolRoles.put(symbol, role);
                return this;
            }
            
            // Упрощенное добавление символа (дефолтная роль)
            public Builder addSymbol(char symbol, VoxelShape shape) {
                return addSymbol(symbol, shape, PartRole.DEFAULT);
            }

            /**
             * Парсит вертикальную структуру (Вид СБОКУ / Спереди).
             * Y меняется по строкам (сверху вниз или снизу вверх - см. invertY), X по символам.
             * Z = 0 (плоская дверь).
             * 
             * @param closedPattern Схема закрытой двери
             * @param openPattern Схема открытой двери (должна совпадать по размерам или быть null)
             * @param controllerChar Символ, обозначающий центр (0,0,0). Если не найден, центр будет (0,0,0) по координатам.
             */
            public DoorStructureDefinition parseVertical(String[] closedPattern, String[] openPattern, char controllerChar) {
                return parse(closedPattern, openPattern, controllerChar, true);
            }

            /**
             * Парсит горизонтальную структуру (Вид СВЕРХУ).
             * Z меняется по строкам, X по символам.
             * Y = 0 (плоский люк).
             */
            public DoorStructureDefinition parseHorizontal(String[] closedPattern, String[] openPattern, char controllerChar) {
                return parse(closedPattern, openPattern, controllerChar, false);
            }

            private DoorStructureDefinition parse(String[] closed, String[] open, char ctrlChar, boolean isVertical) {
                DoorStructureDefinition def = new DoorStructureDefinition();
                
                // 1. Сначала ищем контроллер, чтобы вычислить смещение
                int ctrlRow = -1;
                int ctrlCol = -1;
                
                // Ищем в closed схеме
                for (int r = 0; r < closed.length; r++) {
                    int idx = closed[r].indexOf(ctrlChar);
                    if (idx != -1) {
                        ctrlRow = r;
                        ctrlCol = idx;
                        break;
                    }
                }
                
                // Если контроллер не указан в схеме, считаем что он в 0,0,0 (относительно схемы это может быть край)
                // Но лучше требовать его наличие для корректного центрирования.
                int cR = ctrlRow == -1 ? 0 : ctrlRow;
                int cC = ctrlCol == -1 ? 0 : ctrlCol;

                def.minX = Integer.MAX_VALUE; def.minY = Integer.MAX_VALUE; def.minZ = Integer.MAX_VALUE;
                def.maxX = Integer.MIN_VALUE; def.maxY = Integer.MIN_VALUE; def.maxZ = Integer.MIN_VALUE;

                // 2. Парсим блоки
                // vertical: row=Y (top to bottom usually), col=X
                // horizontal: row=Z, col=X
                
                int height = closed.length;
                
                for (int row = 0; row < height; row++) {
                    String line = closed[row];
                    String openLine = (open != null && row < open.length) ? open[row] : null;
                    
                    for (int col = 0; col < line.length(); col++) {
                        char cSym = line.charAt(col);
                        if (cSym == ' ') continue; // Пропускаем пустоту, если она не часть структуры
                        
                        // Вычисляем координаты относительно контроллера
                        int x = col - cC;
                        int y, z;
                        
                        if (isVertical) {
                            // Строки идут сверху вниз (обычно в коде так пишут массивы)
                            // Значит row 0 это самый верх.
                            // Чтобы контроллер был на Y=0, нужно инвертировать
                            y = (height - 1 - row) - (height - 1 - cR); 
                            z = 0;
                        } else {
                            // Horizontal (Top-down view)
                            // Row = Z, Col = X
                            y = 0;
                            z = row - cR;
                        }

                        BlockPos pos = new BlockPos(x, y, z);
                        
                        // Сохраняем границы
                        def.minX = Math.min(def.minX, x); def.maxX = Math.max(def.maxX, x);
                        def.minY = Math.min(def.minY, y); def.maxY = Math.max(def.maxY, y);
                        def.minZ = Math.min(def.minZ, z); def.maxZ = Math.max(def.maxZ, z);

                        // Коллизия CLOSED
                        VoxelShape shape = symbolShapes.getOrDefault(cSym, Shapes.block());
                        def.closedShapes.put(pos, shape);
                        
                        // Роль
                        PartRole role = symbolRoles.getOrDefault(cSym, PartRole.DEFAULT);
                        // Если это символ контроллера, форсируем роль
                        if (cSym == ctrlChar) role = PartRole.CONTROLLER;
                        def.roles.put(pos, role);
                        
                        // Коллизия OPEN
                        if (openLine != null && col < openLine.length()) {
                            char oSym = openLine.charAt(col);
                            def.openShapes.put(pos, symbolShapes.getOrDefault(oSym, Shapes.empty()));
                        } else {
                            // По дефолту открытая = пустая, если не задана схема
                            def.openShapes.put(pos, Shapes.empty());
                        }
                    }
                }
                
                // Смещение контроллера относительно min координат (нужно для MultiblockHelper)
                def.controllerOffset = new BlockPos(-def.minX, -def.minY, -def.minZ);
                
                return def;
            }
        }
    }
    

    // Возвращает локальные AABB створок относительно контроллера (без поворота/переноса)
    // public List<AABB> getLocalDynamicBoxes(float progress) {
    //     int[] d = getDimensions();
    //     final double ox = d[0], oy = d[1], oz = d[2];
    //     final int sx = d[3], sy = d[4], sz = d[5];
        
    //     // ИСПРАВЛЕНО: учитываем, что структура создается с <=
    //     final double minX = ox;
    //     final double maxX = ox + sx + 1;  // +1 из-за <= в createStructureForDoor
    //     final double minY = oy;
    //     final double maxY = oy + sy + 1;  // +1 из-за <=
    //     final double minZ = oz;
    //     final double maxZ = oz + (sz == 0 ? 1 : sz + 1);  // +1 для толщины или из-за <=
        
    //     final double totalWidth = maxX - minX;
    //     final double half = totalWidth / 2.0;
    //     final double centerX = minX + half;
        
    //     final double move = Math.min(Math.max(progress, 0f), 1f) * half;
        
    //     // Пороги разъезда створок
    //     final double cutLeft = centerX - move;
    //     final double cutRight = centerX + move;
        
    //     List<AABB> boxes = new ArrayList<>();
        
    //     // Создаем коллижн боксы для створок
    //     for (double x = minX; x < maxX; x += 1.0) {
    //         double cellMinX = x;
    //         double cellMaxX = x + 1.0;
            
    //         // Левая створка: часть ячейки слева от cutLeft
    //         double leftPartMaxX = Math.min(cellMaxX, cutLeft);
    //         if (leftPartMaxX - cellMinX > 1e-6) {
    //             boxes.add(new AABB(cellMinX, minY, minZ, leftPartMaxX, maxY, maxZ));
    //         }
            
    //         // Правая створка: часть ячейки справа от cutRight
    //         double rightPartMinX = Math.max(cellMinX, cutRight);
    //         if (cellMaxX - rightPartMinX > 1e-6) {
    //             boxes.add(new AABB(rightPartMinX, minY, minZ, cellMaxX, maxY, maxZ));
    //         }
    //     }
        
    //     return boxes;
    // }    

    // ==================== Реализации ====================

    public static final DoorDecl LARGE_VEHICLE_DOOR = new DoorDecl() {
        {
            // Настройка схемной структуры
            DoorStructureDefinition.Builder builder = DoorStructureDefinition.create();
            
            // Легенда символов
            builder.addSymbol('C', Shapes.block(), PartRole.CONTROLLER); // Контроллер
            builder.addSymbol('X', Shapes.block(), PartRole.DEFAULT);
            builder.addSymbol('O', Shapes.empty(), PartRole.DEFAULT);
            builder.addSymbol('H', Block.box(0, 0, 0, 8, 16, 16), PartRole.DEFAULT);    // левый вертикальный полублок
            builder.addSymbol('G', Block.box(8, 0, 0, 16, 16, 16), PartRole.DEFAULT);   // правый вертикальный полублок
            builder.addSymbol('T', Block.box(0, 10, 0, 16, 16, 16), PartRole.DEFAULT); // полублок в верхнем положении
            // Схема ЗАКРЫТОЙ двери (Вид спереди, Y сверху вниз)
            String[] closed = {
                "XXXXXXX",
                "XXXXXXX",
                "XXXXXXX",
                "XXXXXXX",
                "XXXXXXX",
                "XXXCXXX"
            };

            // Схема ОТКРЫТОЙ двери
            String[] open = {
                "XTTTTTX",
                "HOOOOOG",
                "HOOOOOG",
                "HOOOOOG",
                "HOOOOOG",
                "HOOOOOG"
            };
            
            // Регистрируем структуру (C - центр/контроллер)
            defineStructure(builder.parseVertical(closed, open, 'C'));
        }

        @Override
        public String[] getPartNames() {
            return new String[] { "frame", "door" };
        }

        @Override
        public ResourceLocation getBlockId() {
            return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "large_vehicle_door");
        }

        @Override 
        public int getOpenTime() { 
            return 60; 
        }

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
        public boolean isDynamicShape() {
            return true;
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
            return new int[0][];
        }

        @Override public SoundEvent getOpenSoundEnd() { return ModSounds.GARAGE_STOP.get(); }
        @Override public SoundEvent getOpenSoundLoop() { return ModSounds.GARAGE_MOVE.get(); }
        @Override public SoundEvent getCloseSoundEnd() { return ModSounds.GARAGE_STOP.get(); }
        @Override public SoundEvent getCloseSoundLoop() { return ModSounds.GARAGE_MOVE.get(); }
        @Override public float getSoundVolume() { return 2.0f; }
    };

    public static final DoorDecl ROUND_AIRLOCK_DOOR = new DoorDecl() {
        {
            // Настройка схемной структуры
            DoorStructureDefinition.Builder builder = DoorStructureDefinition.create();
            
            // Легенда символов
            builder.addSymbol('C', Shapes.block(), PartRole.CONTROLLER);
            builder.addSymbol('O', Shapes.empty(), PartRole.DEFAULT);
            builder.addSymbol('H', Block.box(0, 0, 0, 8, 16, 16), PartRole.DEFAULT);    // левый вертикальный полублок
            builder.addSymbol('G', Block.box(8, 0, 0, 16, 16, 16), PartRole.DEFAULT);   // правый вертикальный полублок
            builder.addSymbol('T', Block.box(0, 10, 0, 16, 16, 16), PartRole.DEFAULT); // полублок в верхнем положении
            String[] closed = {
                "####",
                "####",
                "####",
                "#C##"
            };

            String[] open = {
                "HTTG",
                "HOOG",
                "HOOG",
                "HOOG"
            };
            
            // Регистрируем структуру (C - центр/контроллер)
            defineStructure(builder.parseVertical(closed, open, 'C'));
        }
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
        public String[] getPartNames() {
            return new String[] { "frame", "door" };
        }

        // @Override
        // public boolean isDynamicShape() {
        //     return true;
        // }

        // @Override
        // public List<AABB> getCollisionBounds(float progress, Direction facing) {
        //     List<AABB> bounds = new ArrayList<>();
        //     if (progress >= 0.99f) {
        //         return bounds; // Полностью открыта
        //     }

        //     // Получаем РЕАЛЬНЫЕ dimensions из DoorBlock
        //     int[] d = getDimensions(); // [-1, 0, 0, 3, 3, 0]
        //     double ox = d[0];  // -1
        //     double oy = d[1];  // 0
        //     double oz = d[2];  // 0
        //     double sx = d[3];  // 3 (итого 4 блока: -1, 0, 1, 2)
        //     double sy = d[4];  // 3 (итого 4 блока высоты)
        //     double sz = d[5];  // 0 (итого 1 блок глубины)

        //     double half = sx / 2.0;  // 1.5
        //     double move = Math.min(progress, 1.0) * half;  // максимум 1.5 блока

        //     // Левая створка: X от ox до (ox + half - move)
        //     double leftMaxX = Math.max(ox, ox + half - move);
        //     if (leftMaxX - ox > 0.05) {
        //         // X: от -1.0 до (-1.0 + 1.5 - move)
        //         // Y: от 0.0 до 4.0
        //         // Z: от 0.0 до 1.0
        //         AABB leftDoor = new AABB(ox, oy, oz, leftMaxX, oy + sy, oz + sz + 1.0);
        //         bounds.add(rotateAABB(leftDoor, facing));
        //     }

        //     // Правая створка: X от (ox + half + move) до (ox + sx)
        //     double rightMinX = Math.min(ox + sx, ox + half + move);
        //     if (ox + sx - rightMinX > 0.05) {
        //         // X: от (0.5 + move) до 2.0
        //         // Y: от 0.0 до 4.0
        //         // Z: от 0.0 до 1.0
        //         AABB rightDoor = new AABB(rightMinX, oy, oz, ox + sx, oy + sy, oz + sz + 1.0);
        //         bounds.add(rotateAABB(rightDoor, facing));
        //     }

        //     return bounds;
        // }

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

        @Override
        public String[] getPartNames() {
            return new String[] { "frame", "door" };
        }
        
        private Map<String, List<ColladaAnimationParser.AnimationChannel>> animations = Collections.emptyMap();
        private boolean animationsInitialized = false;
    
        // private void initializeAnimations() {
        //     if (animationsInitialized) return;
        //     animationsInitialized = true;
            
        //     try {
        //         // Проверяем, доступен ли Minecraft (не доступен во время генерации данных)
        //         if (Minecraft.getInstance() == null) {
        //             animations = Collections.emptyMap();
        //             return;
        //         }
                
        //         ResourceManager rm = Minecraft.getInstance().getResourceManager();
        //         Resource resource = rm.getResource(
        //             ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/sliding_blast_door.dae")
        //         ).orElse(null);

        //         if (resource == null) {
        //             MainRegistry.LOGGER.error("DoorDecl: missing sliding_blast_door.dae for {}", getBlockId());
        //             animations = Collections.emptyMap();
        //         } else {
        //             animations = ColladaAnimationParser.parse(resource.open());
        //         }
        //     } catch (Exception e) {
        //         e.printStackTrace();
        //         animations = Collections.emptyMap();
        //     }
        // }
        
        @Override
        public void getTranslation(String partName, float openTicks, boolean child, float[] trans) {
            // initializeAnimations();
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
            // initializeAnimations();
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
    
        // @Override
        // public List<AABB> getCollisionBounds(float progress, Direction facing) {
        //     List<AABB> bounds = new ArrayList<>();
        //     if (progress >= 0.99f) return bounds;
            
        //     double movement = progress * 3.5;
        //     AABB door = new AABB(-9.0, 0.0, 0.0, 11.0, Math.max(0.5, 20.0 - movement), 1.0);
        //     bounds.add(rotateAABB(door, facing));
        //     return bounds;
        // }
    
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
        {
            DoorStructureDefinition.Builder builder = DoorStructureDefinition.create();
            
            builder.addSymbol('#', Shapes.block(), PartRole.DEFAULT);
            builder.addSymbol('C', Shapes.block(), PartRole.CONTROLLER);
            builder.addSymbol('O', Shapes.empty(), PartRole.DEFAULT);
            builder.addSymbol('H', Block.box(0, 0, 0, 8, 16, 16), PartRole.DEFAULT);    // левый вертикальный полублок
            builder.addSymbol('G', Block.box(8, 0, 0, 16, 16, 16), PartRole.DEFAULT);   // правый вертикальный полублок
            builder.addSymbol('T', Block.box(0, 12, 0, 16, 16, 16), PartRole.DEFAULT); // полублок в верхнем положении

            // Вид спереди (Y сверху вниз)
            // Дверь 4 блока в ширину, 3 в высоту
            // Контроллер находится в левом нижнем углу (относительно центра)
            String[] closed = {
                "####",
                "####",
                "#C##"
            };

            String[] open = {
                "HTTG", // Верхняя рама остается? Если нет, ставь OOOO
                "HOOG", // Проход
                "HOOG"  // Контроллер остается твердым, остальное проход
            };

            // Если C - это контроллер, метод parseVertical сам посчитает координаты остальных блоков
            defineStructure(builder.parseVertical(closed, open, 'C'));
        }

        @Override
        public String[] getPartNames() {
            return new String[] { "frame", "door" };
        }

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
                set(trans, 0, 0, 0);
            }
        }

        @Override public double[][] getClippingPlanes() {
            return new double[][] { { 0, -1, 0, 3.0001 } };
        }

        @Override public int[][] getDoorOpenRanges() {
            return new int[][] { { -1, 0, 0, 3, 4, 1 } };
        }

        // @Override
        // public List<AABB> getCollisionBounds(float progress, Direction facing) {
        //     List<AABB> bounds = new ArrayList<>();
        //     if (progress >= 0.99f) return bounds;
            
        //     // Противопожарная дверь, движется вверх
        //     double movement = progress * 3.0;
        //     AABB door = new AABB(-1.0, 0.0, 0.0, 2.0, Math.max(0.1, 4.0 - movement), 1.0);
        //     bounds.add(rotateAABB(door, facing));
        //     return bounds;
        // }

        @Override public SoundEvent getOpenSoundEnd() { return ModSounds.WGH_STOP.get(); }
        @Override public SoundEvent getOpenSoundLoop() { return ModSounds.WGH_START.get(); }
        @Override public SoundEvent getSoundLoop2() { return ModSounds.ALARM_6.get(); }
        @Override public float getSoundVolume() { return 1.5f; }
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
    
        // private Map<String, List<ColladaAnimationParser.AnimationChannel>> getAnimations() {
        //     if (animations == null) {
        //         synchronized (animationLock) {
        //             if (animations == null) {
        //                 try {
        //                     ResourceManager rm = Minecraft.getInstance().getResourceManager();
        //                     Resource resource = rm.getResource(
        //                         ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/sliding_blast_door.dae")
        //                     ).orElseThrow();
        //                     animations = ColladaAnimationParser.parse(resource.open());
                            
        //                     // Вычисляем максимальную длительность
        //                     float maxTime = 0f;
        //                     for (List<ColladaAnimationParser.AnimationChannel> channels : animations.values()) {
        //                         for (ColladaAnimationParser.AnimationChannel channel : channels) {
        //                             if (channel.times != null && channel.times.length > 0) {
        //                                 maxTime = Math.max(maxTime, channel.times[channel.times.length - 1]);
        //                             }
        //                         }
        //                     }
        //                     cachedAnimationDuration = maxTime > 0 ? maxTime : 1.25f;
                            
        //                     MainRegistry.LOGGER.info("Loaded animations for SLIDE_DOOR: {} parts, duration: {}s", 
        //                         animations.size(), cachedAnimationDuration);
        //                 } catch (Exception e) {
        //                     MainRegistry.LOGGER.error("Failed to load SLIDE_DOOR animations", e);
        //                     animations = Collections.emptyMap();
        //                     cachedAnimationDuration = 1.25f;
        //                 }
        //             }
        //         }
        //     }
        //     return animations;
        // }

        @Override
        public String[] getPartNames() {
            return new String[] { "frame", "door" };
        }
    
        @Override
        public int getOpenTime() {
            // Map<String, List<ColladaAnimationParser.AnimationChannel>> anims = getAnimations();
            // if (cachedAnimationDuration != null && cachedAnimationDuration > 0) {
            //     return Math.round(cachedAnimationDuration * 20f); // 1.25s * 20 = 25 тиков
            // }
            return 25;
        }
    
        @Override
        public void getTranslation(String partName, float openTicks, boolean child, float[] trans) {
            // Map<String, List<ColladaAnimationParser.AnimationChannel>> anims = getAnimations();
            
            // Инициализируем нулями (КРИТИЧНО: закрытое состояние по умолчанию!)
            trans[0] = 0f;
            trans[1] = 0f;
            trans[2] = 0f;
            
            // if (anims.isEmpty() || !anims.containsKey(partName)) {
            //     return;
            // }
    
            float progress = getNormTime(openTicks);
            float time = progress * cachedAnimationDuration;
    
            // Ищем канал transform (матричная анимация)
            // for (ColladaAnimationParser.AnimationChannel channel : anims.get(partName)) {
            //     if ("transform".equals(channel.property)) {
            //         // Извлекаем translation из матрицы
            //         float[] translation = channel.getTranslationFromMatrix(time);
            //         trans[0] = translation[0];
            //         trans[1] = translation[1];
            //         trans[2] = translation[2];
                    
            //         MainRegistry.LOGGER.debug("Part {} at time {}: translation = [{}, {}, {}]",
            //             partName, time, trans[0], trans[1], trans[2]);
            //         return;
            //     }
                
            //     // Fallback: если есть отдельные каналы location
            //     if (channel.property.startsWith("location")) {
            //         float value = channel.getValue(time);
            //         if (channel.property.endsWith(".X")) trans[0] = value;
            //         else if (channel.property.endsWith(".Y")) trans[1] = value;
            //         else if (channel.property.endsWith(".Z")) trans[2] = value;
            //     }
            // }
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
    
        // @Override
        // public List<AABB> getCollisionBounds(float progress, Direction facing) {
        //     List<AABB> bounds = new ArrayList<>();
        //     if (progress >= 0.99f) return bounds;
            
        //     double movement = progress * 1.5;
        //     if (movement < 1.0) {
        //         AABB door = new AABB(-2.0, 0.0, 0.0, 3.0, 5.0, 1.0 - movement);
        //         bounds.add(rotateAABB(door, facing));
        //     }
        //     return bounds;
        // }
    
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
        public SoundEvent getSoundLoop2() {
            return ModSounds.SLIDING_DOOR_OPENING.get();
        }
    
        @Override
        public float getSoundVolume() {
            return 2.0f;
        }
    };
    
    public static final DoorDecl SLIDING_SEAL_DOOR = new DoorDecl() {
        {
            DoorStructureDefinition.Builder builder = DoorStructureDefinition.create();
            
            VoxelShape thinDoorShape = Block.box(0, 0, 6, 16, 16, 10);
            
            builder.addSymbol('#', thinDoorShape, PartRole.DEFAULT);
            builder.addSymbol('C', thinDoorShape, PartRole.CONTROLLER);

            String[] closed = {
                "#",
                "C"
            };

            // В системе Builder.parse пробел по умолчанию означает Shapes.empty()
            String[] open = {
                " ",
                " "
            };

            // Регистрируем структуру
            defineStructure(builder.parseVertical(closed, open, 'C'));
        }
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

        @Override
        public String[] getPartNames() {
            return new String[] { "frame", "door" };
        }

        @Override public double[][] getClippingPlanes() {
            return new double[][] { { 0, 0, -1, 0.5001 } };
        }

        @Override public int[][] getDoorOpenRanges() {
            return new int[][] { { 0, 0, 0, 1, 2, 2 } };
        }

        // @Override
        // public List<AABB> getCollisionBounds(float progress, Direction facing) {
        //     List<AABB> bounds = new ArrayList<>();
        //     if (progress >= 0.99f) return bounds;
            
        //     // Герметичная раздвижная дверь
        //     double movement = progress * 1.0;
        //     if (movement < 1.0) {
        //         AABB door = new AABB(0.0, 0.0, 1.0 - 0.25, 1.0, 2.0, 1.0);
        //         bounds.add(rotateAABB(door, facing));
        //     }
        //     return bounds;
        // }

        @Override public SoundEvent getOpenSoundEnd() { return ModSounds.METAL_STOP_1.get(); }
        @Override public SoundEvent getOpenSoundStart() { return ModSounds.DOOR_MOVE_2.get(); }
        @Override public float getSoundVolume() { return 2.0f; }
    };

    public static final DoorDecl SECURE_ACCESS_DOOR = new DoorDecl() {

        {
            // Настройка схемной структуры
            DoorStructureDefinition.Builder builder = DoorStructureDefinition.create();
            
            // Легенда символов
            builder.addSymbol('C', Shapes.block(), PartRole.CONTROLLER); // Контроллер
            builder.addSymbol('X', Shapes.block(), PartRole.DEFAULT);
            builder.addSymbol('O', Shapes.empty(), PartRole.DEFAULT);
            builder.addSymbol('H', Block.box(0, 0, 0, 8, 16, 16), PartRole.DEFAULT);    // левый вертикальный полублок
            builder.addSymbol('G', Block.box(8, 0, 0, 16, 16, 16), PartRole.DEFAULT);   // правый вертикальный полублок
            builder.addSymbol('T', Block.box(0, 6, 0, 16, 16, 16), PartRole.DEFAULT); // полублок в верхнем положении
            // Схема ЗАКРЫТОЙ двери (Вид спереди, Y сверху вниз)
            String[] closed = {
                "XXXXX",
                "XXXXX",
                "XXXXX",
                "XXXXX",
                "XXCXX"
            };

            // Схема ОТКРЫТОЙ двери
            String[] open = {
                "TTTTT",
                "OOOOO",
                "OOOOO",
                "OOOOO",
                "XXXXX"
            };
            
            // Регистрируем структуру (C - центр/контроллер)
            defineStructure(builder.parseVertical(closed, open, 'C'));
        }

        @Override
        public ResourceLocation getBlockId() {
            return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "secure_access_door");
        }

        @Override
        public String[] getPartNames() {
            return new String[] { "frame", "door" };
        }
    
        @Override public int getOpenTime() { return 120; }
        
        @Override
        public void getTranslation(String partName, float openTicks, boolean child, float[] trans) {
            if(!partName.equals("base")) {
                set(trans, 0, 3.5F * getNormTime(openTicks), 0);
            } else {
                super.getTranslation(partName, openTicks, child, trans);
            }
        }

        @Override
        public boolean isDynamicShape() {
            return true;
        }

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
    
        // @Override
        // public List<AABB> getCollisionBounds(float progress, Direction facing) {
        //     List<AABB> bounds = new ArrayList<>();
        //     if (progress >= 0.99f) return bounds;
            
        //     // Дверь безопасного доступа, движется вверх
        //     double movement = progress * 3.5;
        //     AABB door = new AABB(-2.0, 0.0, 0.0, 3.0, Math.max(0.0625, 5.0 - movement), 1.0);
        //     bounds.add(rotateAABB(door, facing));
        //     return bounds;
        // }
    
        @Override public SoundEvent getCloseSoundLoop() { return ModSounds.GARAGE_MOVE.get(); }
        @Override public SoundEvent getCloseSoundEnd() { return ModSounds.GARAGE_STOP.get(); }
        @Override public SoundEvent getOpenSoundEnd() { return ModSounds.GARAGE_STOP.get(); }
        @Override public SoundEvent getOpenSoundLoop() { return ModSounds.GARAGE_MOVE.get(); }
        @Override public float getSoundVolume() { return 2.0f; }
    };    

    public static final DoorDecl QE_SLIDING = new DoorDecl() {

        {
            DoorStructureDefinition.Builder builder = DoorStructureDefinition.create();
            
            // Создаем форму: толщина 2 пикселя в центре блока (от 7 до 9 по оси Z)
            // Если блок 16x16x16, то центр — это 8. 7-9 дает нам ровно 2 пикселя толщины.
            VoxelShape thinDoorShape = Block.box(0, 0, 7, 16, 16, 9);
            
            // Регистрируем символы с нашей тонкой формой
            builder.addSymbol('#', thinDoorShape, PartRole.DEFAULT);
            builder.addSymbol('C', thinDoorShape, PartRole.CONTROLLER);

            // Схема 2x2
            // C - контроллер (0, 0, 0)
            String[] closed = {
                "##", // Y = 1
                "C#"  // Y = 0
            };

            // В открытом состоянии используем пробелы. 
            // В нашей системе Builder.parse пробел по умолчанию означает Shapes.empty()
            String[] open = {
                "  ",
                "  "
            };

            // Регистрируем структуру
            defineStructure(builder.parseVertical(closed, open, 'C'));
        }

        @Override
        public ResourceLocation getBlockId() {
            return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "qe_sliding_door");
        }

        @Override
        public String[] getPartNames() {
            return new String[] { "frame", "door" };
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

        // @Override
        // public List<AABB> getCollisionBounds(float progress, Direction facing) {
        //     List<AABB> bounds = new ArrayList<>();
        //     if (progress >= 0.99f) return bounds;
            
        //     // Быстрая раздвижная дверь
        //     double movement = progress * 0.99;
        //     if (movement < 0.99) {
        //         AABB door = new AABB(0.0, 0.0, 1.0 - 0.1875, 2.0, 2.0, 1.0);
        //         bounds.add(rotateAABB(door, facing));
        //     }
        //     return bounds;
        // }

        @Override public SoundEvent getOpenSoundEnd() { return ModSounds.SLIDING_DOOR_OPENED.get(); }
        @Override public SoundEvent getCloseSoundEnd() { return ModSounds.SLIDING_DOOR_SHUT.get(); }
        @Override public SoundEvent getOpenSoundLoop() { return ModSounds.SLIDING_DOOR_OPENING.get(); }
        @Override public float getSoundVolume() { return 2.0f; }
    };

    public static final DoorDecl QE_CONTAINMENT = new DoorDecl() {
        {
            DoorStructureDefinition.Builder builder = DoorStructureDefinition.create();
            
            // Создаем форму: толщина 2 пикселя в центре блока (от 7 до 9 по оси Z)
            // Если блок 16x16x16, то центр — это 8. 7-9 дает нам ровно 2 пикселя толщины.
            VoxelShape thinDoorShape = Block.box(0, 0, 4, 16, 16, 12);
            
            // Регистрируем символы с нашей тонкой формой
            builder.addSymbol('#', thinDoorShape, PartRole.DEFAULT);
            builder.addSymbol('C', thinDoorShape, PartRole.CONTROLLER);
            builder.addSymbol('H', Block.box(0, 0, 4, 4, 16, 12), PartRole.DEFAULT);    // левый вертикальный полублок
            builder.addSymbol('G', Block.box(12, 0, 4, 16, 16, 12), PartRole.DEFAULT);   // правый вертикальный полублок
            builder.addSymbol('T', Block.box(0, 6, 0, 16, 16, 16), PartRole.DEFAULT); // полублок в верхнем положении

            // Схема 2x2
            // C - контроллер (0, 0, 0)
            String[] closed = {
                "###",
                "###",
                "#C#"
            };

            // В открытом состоянии используем пробелы. 
            // В нашей системе Builder.parse пробел по умолчанию означает Shapes.empty()
            String[] open = {
                "TTT",
                "H G",
                "H G"
            };

            // Регистрируем структуру
            defineStructure(builder.parseVertical(closed, open, 'C'));
        }

        @Override
        public ResourceLocation getBlockId() {
            return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "qe_containment_door");
        }

        @Override
        public String[] getPartNames() {
            return new String[] { "frame", "door" };
        }
    
        // @Override
        // public ResourceLocation getTextureForPart(int skinIndex, String partName) {
        //     if ("decal".equals(partName)) {
        //         // Возвращаем текстуру декали
        //         return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, 
        //             "block/qe_containment_decal");
        //     }
        //     // Основная текстура для остальных частей
        //     return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, 
        //         "block/qe_containment_door");
        // }
    
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
    
        // @Override
        // public List<AABB> getCollisionBounds(float progress, Direction facing) {
        //     List<AABB> bounds = new ArrayList<>();
        //     if (progress >= 0.99f) return bounds;
            
        //     double movement = progress * 3.0;
        //     AABB door = new AABB(-1.0, 0.0, 0.5, 2.0, Math.max(0.1, 3.0 - movement), 1.0);
        //     bounds.add(rotateAABB(door, facing));
        //     return bounds;
        // }
    
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

        {
            DoorStructureDefinition.Builder builder = DoorStructureDefinition.create();
            
            // Создаем форму: толщина 2 пикселя в центре блока (от 7 до 9 по оси Z)
            // Если блок 16x16x16, то центр — это 8. 7-9 дает нам ровно 2 пикселя толщины.
            VoxelShape thinDoorShape = Block.box(0, 0, 6, 16, 16, 10);
            
            // Регистрируем символы с нашей тонкой формой
            builder.addSymbol('#', thinDoorShape, PartRole.DEFAULT);
            builder.addSymbol('C', thinDoorShape, PartRole.CONTROLLER);
            builder.addSymbol('H', Block.box(0, 0, 6, 8, 16, 10), PartRole.DEFAULT);    // левый вертикальный полублок
            builder.addSymbol('G', Block.box(11, 0, 6, 16, 16, 10), PartRole.DEFAULT);   // правый вертикальный полублок
            builder.addSymbol('T', Block.box(0, 13, 0, 16, 16, 16), PartRole.DEFAULT); // полублок в верхнем положении

            // Схема 2x2
            // C - контроллер (0, 0, 0)
            String[] closed = {
                "###",
                "###",
                "#C#"
            };

            // В открытом состоянии используем пробелы. 
            // В нашей системе Builder.parse пробел по умолчанию означает Shapes.empty()
            String[] open = {
                "TTT",
                "H G",
                "H G"
            };

            // Регистрируем структуру
            defineStructure(builder.parseVertical(closed, open, 'C'));
        }

        @Override
        public ResourceLocation getBlockId() {
            return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "water_door");
        }
    
        @Override
        public int getOpenTime() { 
            return 60; 
        }

        @Override
        public String[] getPartNames() {
            return new String[] { "frame", "door" };
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
    
        // @Override
        // public float getDoorRangeOpenTime(int ticks, int idx) {
        //     return getNormTime(ticks, 35, 40);
        // }
    
        @Override
        public int[][] getDoorOpenRanges() {
            // Фантомные блоки для обнаружения игроков/сущностей
            return new int[][] { { 1, 0, 0, -3, 3, 2 } };
        }
    
        // @Override
        // public List<AABB> getCollisionBounds(float progress, Direction facing) {
        //     List<AABB> bounds = new ArrayList<>();
        //     if (progress >= 0.99f) return bounds;
            
        //     // Водонепроницаемая дверь со сложной анимацией
        //     AABB door = new AABB(0.0, 0.0, 0.75, 1.0, 3.0, 1.0);
        //     bounds.add(rotateAABB(door, facing));
        //     return bounds;
        // }
    
        private float smoothstep(float t) {
            // Плавная интерполяция (кубический Эрмит)
            return t * t * (3.0f - 2.0f * t);
        }

        @Override 
        public SoundEvent getOpenSoundEnd() { 
            return ModSounds.DOOR_WGH_BIG_STOP.get();
        }
        
        @Override 
        public SoundEvent getOpenSoundLoop() { 
            return ModSounds.DOOR_WGH_BIG_START.get();
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
        {
            DoorStructureDefinition.Builder builder = DoorStructureDefinition.create();
            // Хатч обычно плоский, используем полную коллизию для закрытого состояния
            builder.addSymbol('#', Shapes.block(), PartRole.DEFAULT);
            builder.addSymbol('C', Shapes.block(), PartRole.CONTROLLER);

            // Вид сверху (Z меняется по строкам, X по символам)
            String[] closed = {
                "#####",
                "#####",
                "##C##",
                "#####",
                "#####"
            };

            // В открытом состоянии центральная часть 3x3 становится пустой
            String[] open = {
                "#####",
                "#   #",
                "#   #",
                "#   #",
                "#####"
            };

            defineStructure(builder.parseHorizontal(closed, open, 'C'));
        }

        @Override public ResourceLocation getBlockId() { return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "silo_hatch"); }
        @Override public String[] getPartNames() { return new String[] { "frame", "door" }; }
        @Override public int getOpenTime() { return 60; }
        @Override public boolean remoteControllable() { return true; }
        @Override public int getBlockOffset() { return 2; }

        @Override
        public void getTranslation(String partName, float openTicks, boolean child, float[] trans) {
            if ("Hatch".equals(partName)) {
                float smoothTime = smoothstep(getNormTime(openTicks, 0, 10));
                set(trans, 0, 0.25F * smoothTime, 0);
            }
        }

        @Override
        public boolean isDynamicShape() {
            return true;
        }

        @Override
        public void getOrigin(String partName, float[] orig) {
            if ("Hatch".equals(partName)) set(orig, 0F, 0.875F, -1.875F);
        }

        @Override
        public void getRotation(String partName, float openTicks, float[] rot) {
            if ("Hatch".equals(partName)) {
                float smoothTime = smoothstep(getNormTime(openTicks, 20, 100));
                set(rot, smoothTime * -240, 0, 0);
            }
        }

        private float smoothstep(float t) { return t * t * (3.0f - 2.0f * t); }

        @Override public SoundEvent getOpenSoundEnd() { return ModSounds.DOOR_WGH_BIG_STOP.get(); }
        @Override public SoundEvent getOpenSoundLoop() { return ModSounds.DOOR_WGH_BIG_START.get(); }
        @Override public SoundEvent getCloseSoundEnd() { return ModSounds.DOOR_WGH_BIG_STOP.get(); }
        @Override public float getSoundVolume() { return 2.0f; }
    };

    public static final DoorDecl SILO_HATCH_LARGE = new DoorDecl() {
        {
            DoorStructureDefinition.Builder builder = DoorStructureDefinition.create();
            builder.addSymbol('#', Shapes.block(), PartRole.DEFAULT);
            builder.addSymbol('C', Shapes.block(), PartRole.CONTROLLER);

            // 7x7 структура
            String[] closed = {
                "#######",
                "#######",
                "#######",
                "###C###",
                "#######",
                "#######",
                "#######"
            };

            // Проем 5x5
            String[] open = {
                "#######",
                "##   ##",
                "#     #",
                "#     #",
                "#     #",
                "##   ##",
                "#######"
            };

            defineStructure(builder.parseHorizontal(closed, open, 'C'));
        }

        @Override public ResourceLocation getBlockId() { return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "silo_hatch_large"); }
        @Override public String[] getPartNames() { return new String[] { "frame", "door" }; }
        @Override public int getOpenTime() { return 60; }
        @Override public boolean remoteControllable() { return true; }
        @Override public int getBlockOffset() { return 3; }

        @Override
        public void getTranslation(String partName, float openTicks, boolean child, float[] trans) {
            if ("Hatch".equals(partName)) {
                float smoothTime = smoothstep(getNormTime(openTicks, 0, 10));
                set(trans, 0, 0.25F * smoothTime, 0);
            }
        }

        @Override
        public boolean isDynamicShape() {
            return true;
        }

        @Override
        public void getOrigin(String partName, float[] orig) {
            if ("Hatch".equals(partName)) set(orig, 0F, 0.875F, -2.875F);
        }

        @Override
        public void getRotation(String partName, float openTicks, float[] rot) {
            if ("Hatch".equals(partName)) {
                float smoothTime = smoothstep(getNormTime(openTicks, 20, 100));
                set(rot, smoothTime * -240, 0, 0);
            }
        }

        private float smoothstep(float t) { return t * t * (3.0f - 2.0f * t); }

        @Override public SoundEvent getOpenSoundEnd() { return ModSounds.DOOR_WGH_BIG_STOP.get(); }
        @Override public SoundEvent getOpenSoundLoop() { return ModSounds.DOOR_WGH_BIG_START.get(); }
        @Override public SoundEvent getCloseSoundEnd() { return ModSounds.DOOR_WGH_BIG_STOP.get(); }
        @Override public float getSoundVolume() { return 2.0f; }
    };
}
