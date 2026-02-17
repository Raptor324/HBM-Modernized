package com.hbm_m.block.entity.custom.doors;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

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
     * Путь к DAE-файлу с анимациями. Если null — используются procedural-анимации (getTranslation/getRotation).
     */
    @Nullable
    public ResourceLocation getColladaAnimationSource() {
        return null;
    }

    /**
     * Маппинг имени части (OBJ/JSON) на имя объекта в DAE.
     * По умолчанию возвращает partName. Для transition_seal: Cylinder.001 → Cylinder_001.
     */
    public String getDaeObjectName(String partName) {
        return partName;
    }

    /**
     * Инвертировать время анимации DAE.
     * Если true: closed (progress=0) → последний keyframe, open (progress=1) → первый keyframe.
     * Нужно когда Blender экспортировал анимацию с rest pose = открытое состояние.
     */
    public boolean isColladaAnimationInverted() {
        return false;
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

    public int getBlockOffset() { return 0; }

    public boolean remoteControllable() { return false; }

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

    /**
     * Доп. поворот Y (градусы) для baked model — должен совпадать с doOffsetTransform,
     * чтобы BakedModel и BER были выровнены. По умолчанию 0.
     */
    public int getBakedModelRotationOffsetY() { return 0; }

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
            builder.addSymbol('M', Block.box(0, 0, 0, 16, 1, 16), PartRole.DEFAULT); // плита в нижнем положении
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
                "HMMMMMG"
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
            builder.addSymbol('M', Block.box(0, 0, 0, 16, 3, 16), PartRole.DEFAULT); // плита в нижнем положении
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
                "HMMG"
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
        public ResourceLocation getColladaAnimationSource() {
            return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/doors/transition_seal.dae");
        }
    
        @Override
        public String getDaeObjectName(String partName) {
            // DAE обычно заменяет точки на подчеркивания
            return partName.replace('.', '_');
        }
    
        @Override
        public void getTranslation(String partName, float openTicks, boolean child, float[] trans) {
            // В 1.7.10 логика была: "всё кроме базы едет вверх".
            // Здесь мы повторяем это: Рама стоит, всё остальное (дверь, шестерни, замки) едет вверх.
            if ("frame".equals(partName) || "base".equals(partName)) {
                set(trans, 0, 0, 0);
            } else {
                // Процедурный подъем на 3.5 блока
                set(trans, 0, 3.5F * getNormTime(openTicks), 0);
            }
        }
    
        @Override 
        public int getOpenTime() { 
            return 480; 
        }
    
        @Override
        public String[] getPartNames() {
            return new String[] { 
                "frame", "door", "Cylinder.001", "Cylinder.003", "Cylinder.005", "Cube.006", "Cylinder.007", "Cylinder.008", "Circle", "Cylinder.009", "Cylinder.010", "Cylinder.011", "door.005", "door.002", "door.008", "ring.001", "door.003", "door.004", "ring.002", "door.006"
            };
        }
        
        @Override
        public void doOffsetTransform(LegacyAnimator animator) {
            // Глобальное смещение всей модели (как в 1.7.10 GL11.glTranslated)
            animator.translate(0.0f, 0.0f, 0.5f);
        }
        
        @Override 
        public int[][] getDoorOpenRanges() {
            return new int[][] { { -9, 2, 0, 20, 20, 1 } };
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
        {
            DoorStructureDefinition.Builder builder = DoorStructureDefinition.create();
            
            builder.addSymbol('#', Shapes.block(), PartRole.DEFAULT);
            builder.addSymbol('C', Shapes.block(), PartRole.CONTROLLER);
            builder.addSymbol('O', Shapes.empty(), PartRole.DEFAULT);
            builder.addSymbol('H', Block.box(0, 0, 0, 8, 16, 16), PartRole.DEFAULT);    // левый вертикальный полублок
            builder.addSymbol('G', Block.box(8, 0, 0, 16, 16, 16), PartRole.DEFAULT);   // правый вертикальный полублок
            builder.addSymbol('T', Block.box(0, 12, 0, 16, 16, 16), PartRole.DEFAULT); // полублок в верхнем положении
            builder.addSymbol('M', Block.box(0, 0, 0, 16, 3, 16), PartRole.DEFAULT); // плита в нижнем положении

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
                "HMMG"  // Контроллер остается твердым, остальное проход
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

        @Override
        public ResourceLocation getColladaAnimationSource() {
            return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/doors/sliding_blast_door.dae");
        }

        /** Blender: frame 0 = open. Закрытое состояние = последний keyframe */
        @Override
        public boolean isColladaAnimationInverted() {
            return false;
        }

        @Override
        public String[] getChildren(String partName) {
            return switch (partName) {
                case "DoorLeft" -> new String[] { "DoorCircleLeft", "Window" };
                case "DoorRight" -> new String[] { "DoorCircleRight" };
                default -> super.getChildren(partName);
            };
        }

        @Override
        public boolean doesRender(String partName, boolean child) {
            if (!child && ("DoorCircleLeft".equals(partName) || "Window".equals(partName) || "DoorCircleRight".equals(partName))) {
                return false;
            }
            return true;
        }

        /** Fallback: DAE анимирует по X (влево-вправо). Если DAE недоступен — процедурно по X */
        @Override
        public void getTranslation(String partName, float openTicks, boolean child, float[] trans) {
            float progress = getNormTime(openTicks);
            switch (partName) {
                case "DoorFrame" -> set(trans, 0, 0, 0);
                case "DoorLeft" -> set(trans, 2.5F * progress, 0, 0);   // +X влево-вправо (как в DAE)
                case "DoorRight" -> set(trans, -2.5F * progress, 0, 0);  // -X
                default -> super.getTranslation(partName, openTicks, child, trans);
            }
        }

        @Override
        public String[] getPartNames() {
            return new String[] { "DoorFrame", "DoorLeft", "DoorRight", "Window", "DoorCircleLeft", "DoorCircleRight" };
        }
    
        @Override
        public int getOpenTime() {
            return 24;
        }
    
        @Override
        public void doOffsetTransform(LegacyAnimator animator) {
            animator.rotate(-90.0f, 0.0f, 1.0f, 0.0f);
        }

        @Override public int getBakedModelRotationOffsetY() { return -90; }
    
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

        // @Override
        // public void doOffsetTransform(LegacyAnimator animator) {
        //     // Поворот на 90° вокруг оси Y (как в старой версии)
        //     animator.rotate(90.0f, 0.0f, 1.0f, 0.0f);
        // }

        // @Override public int getBakedModelRotationOffsetY() { return 90; }
        
        @Override public double[][] getClippingPlanes() {
            return new double[][] { { 0, -1, 0, 5 } };
        }
    
        @Override public int[][] getDoorOpenRanges() {
            return new int[][] { { -2, 1, 0, 4, 5, 1 } };
        }
    
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
            builder.addSymbol('M', Block.box(0, 0, 0, 16, 3, 16), PartRole.DEFAULT); // плита в нижнем положении

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
                "HMG"
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
    
        @Override
        public int[][] getDoorOpenRanges() {
            // Фантомные блоки для обнаружения игроков/сущностей
            return new int[][] { { 1, 0, 0, -3, 3, 2 } };
        }
    
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
