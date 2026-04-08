package com.hbm_m.multiblock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

// Утилитарный класс для управления мультиблочными структурами.
// Позволяет определять структуру, проверять возможность постройки, строить и разрушать структуру,
// а также генерировать VoxelShape для всей структуры. Ядро всей мультиблочной логики.
import com.hbm_m.api.energy.WireBlock;
import com.hbm_m.block.machines.MachineAdvancedAssemblerBlock;
import com.hbm_m.block.machines.UniversalMachinePartBlock;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.network.HighlightBlocksPacket;
import com.hbm_m.network.ModPacketHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.PacketDistributor;

public class MultiblockStructureHelper {
    
    // ThreadLocal флаг для предотвращения рекурсии при разрушении структуры
    private static final ThreadLocal<Boolean> IS_DESTROYING = ThreadLocal.withInitial(() -> false);

    private final Map<BlockPos, Supplier<BlockState>> structureMap;
    private final Supplier<BlockState> phantomBlockState;
    // Карта символов на роли для рецептоподобного определения структуры
    private final Map<Character, PartRole> symbolRoleMap;
    // Карта позиций на символы для определения роли при постройке
    private final Map<BlockPos, Character> positionSymbolMap;
    private final Map<Direction, VoxelShape> shapeCache = new HashMap<>();
    private final Map<BlockPos, Set<Direction>> ladderLocalDirections = new HashMap<>();
    /** Локальные стороны энергоприёма/отдачи относительно схемы (до поворота FACING). Ключ — позиция в сетке. */
    private final Map<BlockPos, Set<Direction>> energyLocalDirections = new HashMap<>();
    private final Map<BlockPos, VoxelShape> partShapes;
    private final Map<BlockPos, VoxelShape> collisionShapes;
    // Позиция контроллера в структуре (относительно центра)
    private final BlockPos controllerOffset;

    private final Set<BlockPos> partOffsets;
    // private final int maxY;

    // 1. Конструктор для старого способа (минимальный)
    public MultiblockStructureHelper(Map<BlockPos, Supplier<BlockState>> structureMap, 
                                   Supplier<BlockState> phantomBlockState) {
        this(structureMap, phantomBlockState, null, null, null, null, BlockPos.ZERO);
    }
    
    // 2. Конструктор с ролями (средний)
    public MultiblockStructureHelper(Map<BlockPos, Supplier<BlockState>> structureMap, 
                                    Supplier<BlockState> phantomBlockState,
                                    Map<Character, PartRole> symbolRoleMap) {
        this(structureMap, phantomBlockState, symbolRoleMap, null, null, null, BlockPos.ZERO);
    }

    // 3. Вспомогательный конструктор (без кастомных форм, но со смещением)
    public MultiblockStructureHelper(Map<BlockPos, Supplier<BlockState>> structureMap, 
            Supplier<BlockState> phantomBlockState,
            Map<Character, PartRole> symbolRoleMap,
            Map<BlockPos, Character> positionSymbolMap,
            BlockPos controllerOffset) {
        this(structureMap, phantomBlockState, symbolRoleMap, positionSymbolMap, null, null, controllerOffset);
    }
    
    // 4. ГЛАВНЫЙ КОНСТРУКТОР (Все остальные вызывают его через this(...))
    public MultiblockStructureHelper(Map<BlockPos, Supplier<BlockState>> structureMap, 
                                    Supplier<BlockState> phantomBlockState,
                                    Map<Character, PartRole> symbolRoleMap,
                                    Map<BlockPos, Character> positionSymbolMap,
                                    Map<BlockPos, VoxelShape> partShapes,
                                    Map<BlockPos, VoxelShape> collisionShapes,
                                    BlockPos controllerOffset) {
        this.structureMap = Collections.unmodifiableMap(structureMap);
        this.phantomBlockState = phantomBlockState;
        this.symbolRoleMap = symbolRoleMap != null ? Collections.unmodifiableMap(symbolRoleMap) : Collections.emptyMap();
        this.positionSymbolMap = positionSymbolMap != null ? Collections.unmodifiableMap(positionSymbolMap) : Collections.emptyMap();
        this.controllerOffset = controllerOffset != null ? controllerOffset : BlockPos.ZERO;
        this.partShapes = partShapes != null ? Collections.unmodifiableMap(partShapes) : Collections.emptyMap();
        this.collisionShapes = collisionShapes != null ? Collections.unmodifiableMap(collisionShapes) : Collections.emptyMap();
        this.partOffsets = Collections.unmodifiableSet(structureMap.keySet());
    }

    /**
     * Создаёт MultiblockStructureHelper из слоёв, заданных строками (рецептоподобный способ).
     * Каждый слой задаётся как массив строк, где каждая строка - это ряд блоков по оси Z,
     * а каждый символ в строке - блок по оси X.
     * Слои идут снизу вверх (слой 0 = y=0, слой 1 = y=1, и т.д.).
     * 
     * Структура автоматически центрируется так, чтобы контроллер был в позиции (0, 0, 0).
     * 
     * @param layers Массив слоёв, где каждый слой - массив строк (снизу вверх)
     * @param symbolMap Карта символов на Supplier<BlockState>. Символы, отсутствующие в карте, игнорируются (пустота)
     * @param phantomBlockState Состояние блока для фантомных частей
     * @param controllerOffset Смещение контроллера относительно центра структуры (по умолчанию 0,0,0)
     * @return Новый экземпляр MultiblockStructureHelper
     * 
     * @example
     * String[] layer0 = {"XXX", "XXX", "XXX"};
     * String[] layer1 = {"XXX", "XXX", "XXX"};
     * String[] layer2 = {"OOO", "OXO", "OOO"};
     * Map<Character, Supplier<BlockState>> symbols = Map.of('X', () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
     * MultiblockStructureHelper helper = MultiblockStructureHelper.createFromLayers(
     *     new String[][]{layer0, layer1, layer2}, 
     *     symbols,
     *     () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState()
     * );
     */
    // public static MultiblockStructureHelper createFromLayers(
    //         String[][] layers,
    //         Map<Character, Supplier<BlockState>> symbolMap,
    //         Supplier<BlockState> phantomBlockState,
    //         BlockPos controllerOffset) {
        
    //     Map<BlockPos, Supplier<BlockState>> structureMap = new HashMap<>();
        
    //     if (layers == null || layers.length == 0) {
    //         return new MultiblockStructureHelper(Collections.emptyMap(), phantomBlockState);
    //     }
        
    //     // Находим максимальную ширину и глубину для центрирования
    //     int maxWidth = 0;
    //     int maxDepth = layers[0].length;
        
    //     for (String[] layer : layers) {
    //         if (layer != null) {
    //             maxDepth = Math.max(maxDepth, layer.length);
    //             for (String row : layer) {
    //                 if (row != null) {
    //                     maxWidth = Math.max(maxWidth, row.length());
    //                 }
    //             }
    //         }
    //     }
        
    //     // Вычисляем смещения для центрирования (контроллер должен быть в центре)
    //     // Для нечётных размеров центр находится точно посередине
    //     // Для чётных размеров центр смещён влево/вверх
    //     int centerX = (maxWidth - 1) / 2;
    //     int centerZ = (maxDepth - 1) / 2;
        
    //     // Обрабатываем каждый слой
    //     for (int y = 0; y < layers.length; y++) {
    //         String[] layer = layers[y];
    //         if (layer == null) continue;
            
    //         // Обрабатываем каждую строку слоя (Z координата)
    //         for (int z = 0; z < layer.length; z++) {
    //             String row = layer[z];
    //             if (row == null) continue;
                
    //             // Обрабатываем каждый символ в строке (X координата)
    //             for (int x = 0; x < row.length(); x++) {
    //                 char symbol = row.charAt(x);
                    
    //                 // Вычисляем относительные координаты (центрированные)
    //                 int relX = x - centerX + controllerOffset.getX();
    //                 int relY = y + controllerOffset.getY();
    //                 int relZ = z - centerZ + controllerOffset.getZ();
                    
    //                 BlockPos pos = new BlockPos(relX, relY, relZ);
                    
    //                 // Пропускаем позицию контроллера
    //                 BlockPos controllerPos = new BlockPos(
    //                     controllerOffset.getX(), 
    //                     controllerOffset.getY(), 
    //                     controllerOffset.getZ()
    //                 );
    //                 if (pos.equals(controllerPos)) {
    //                     continue;
    //                 }
                    
    //                 // Если символ есть в карте, добавляем блок
    //                 Supplier<BlockState> blockStateSupplier = symbolMap.get(symbol);
    //                 if (blockStateSupplier != null) {
    //                     structureMap.put(pos, blockStateSupplier);
    //                 }
    //             }
    //         }
    //     }
        
    //     return new MultiblockStructureHelper(structureMap, phantomBlockState);
    // }
    
    // /**
    //  * Перегрузка метода createFromLayers с контроллером в позиции (0, 0, 0).
    //  */
    // public static MultiblockStructureHelper createFromLayers(
    //         String[][] layers,
    //         Map<Character, Supplier<BlockState>> symbolMap,
    //         Supplier<BlockState> phantomBlockState) {
    //     return createFromLayers(layers, symbolMap, phantomBlockState, BlockPos.ZERO);
    // }
    
    // === НОВЫЕ МЕТОДЫ С ПОДДЕРЖКОЙ РОЛЕЙ ===
    
    /**
     * Создаёт MultiblockStructureHelper из слоёв с картой ролей.
     * 
     * ВАЖНО: Контроллер должен быть обозначен символом с ролью PartRole.CONTROLLER.
     * В структуре ОБЯЗАТЕЛЬНО должен быть ровно ОДИН контроллер.
     * Структура автоматически центрируется так, чтобы контроллер находился в позиции (0, 0, 0).
     * 
     * @param layers Массив слоёв
     * @param symbolMap Карта символов на Supplier<BlockState>
     * @param phantomBlockState Состояние фантомного блока
     * @param roleMap Карта символов на PartRole. ОБЯЗАТЕЛЬНО должен содержать символ с ролью CONTROLLER.
     * @param shapeMap необязательно: кастомные VoxelShape по символу
     * @param collisionMap необязательно: коллизии по символу
     * @param ladderSideMap необязательно: для символов с ролью LADDER — boolean[4] в порядке north, south, west, east.
     *                      Если символа нет в карте — лестница со всех четырёх горизонтальных сторон.
     * @param energySideMap необязательно: для ENERGY_CONNECTOR / UNIVERSAL_CONNECTOR — boolean[6] в порядке north, south, west, east, up, down.
     *                      Если символа нет в карте — энергия со всех шести сторон.
     * @return Новый экземпляр MultiblockStructureHelper
     *
     * @example
     * {@code createFromLayersWithRolesAndSides(layers, symbolMap, phantom, roleMap, Map.of('L', MultiblockSideTuples.ladder(true, false, false, false)), null)}
     */
    public static MultiblockStructureHelper createFromLayersWithRoles(
            String[][] layers,
            Map<Character, Supplier<BlockState>> symbolMap,
            Supplier<BlockState> phantomBlockState,
            Map<Character, PartRole> roleMap,
            Map<Character, VoxelShape> shapeMap,
            Map<Character, VoxelShape> collisionMap,
            Map<Character, boolean[]> ladderSideMap,
            Map<Character, boolean[]> energySideMap) {
        
        Map<BlockPos, Supplier<BlockState>> structureMap = new HashMap<>();
        Map<BlockPos, Character> positionSymbolMap = new HashMap<>();
        Map<BlockPos, Set<Direction>> ladderDirs = new HashMap<>();
        Map<BlockPos, Set<Direction>> energyDirs = new HashMap<>();
        Map<BlockPos, VoxelShape> specificPartShapes = new HashMap<>();
        Map<BlockPos, VoxelShape> specificCollisionShapes = new HashMap<>();
        
        // Список для хранения ВСЕХ найденных позиций контроллеров
        List<BlockPos> foundControllerPositions = new ArrayList<>();
        
        // roleMap ОБЯЗАТЕЛЕН
        if (roleMap == null || roleMap.isEmpty()) {
            throw new IllegalArgumentException("roleMap cannot be null or empty");
        }
        
        // Проверяем, что есть символ для контроллера
        boolean hasControllerRole = roleMap.containsValue(PartRole.CONTROLLER);
        if (!hasControllerRole) {
            throw new IllegalArgumentException("roleMap must contain a symbol with the role PartRole.CONTROLLER");
        }
        
        if (layers == null || layers.length == 0) {
            throw new IllegalArgumentException("layers cannot be null or empty");
        }
        
        // Находим максимальную ширину и глубину для центрирования
        int maxDepth = 0;
        int maxWidth = 0;
        for (String[] layer : layers) {
            if (layer == null) continue;
            maxDepth = Math.max(maxDepth, layer.length);
            for (String row : layer) {
                if (row != null) {
                    maxWidth = Math.max(maxWidth, row.length());
                }
            }
        }
        
        int centerX = (maxWidth - 1) / 2;
        int centerZ = (maxDepth - 1) / 2;
        // int centerY = 0;
        
        // Обрабатываем каждый слой
        for (int y = 0; y < layers.length; y++) {
            String[] layer = layers[y];
            if (layer == null) continue;
            for (int z = 0; z < layer.length; z++) {
                String row = layer[z];
                if (row == null) continue;

                for (int gridX = 0; gridX < row.length(); gridX++) {
                    char symbol = row.charAt(gridX);

                    PartRole role = roleMap.get(symbol);
                    int relX = gridX - centerX;
                    int relZ = z - centerZ;
                    BlockPos pos = new BlockPos(relX, y, relZ);
                    
                    if (role != null) {
                        if (role == PartRole.CONTROLLER) {
                            foundControllerPositions.add(pos);
                        } else {
                            structureMap.put(pos, symbolMap.get(symbol));
                            if (role == PartRole.LADDER && ladderSideMap != null && ladderSideMap.containsKey(symbol)) {
                                ladderDirs.put(pos, ladderTupleToLocalSet(symbol, ladderSideMap.get(symbol)));
                            }
                            if ((role == PartRole.ENERGY_CONNECTOR || role == PartRole.UNIVERSAL_CONNECTOR)
                                    && energySideMap != null && energySideMap.containsKey(symbol)) {
                                energyDirs.put(pos, energyTupleToLocalSet(symbol, energySideMap.get(symbol)));
                            }
                        }
                        positionSymbolMap.put(pos, symbol);

                        // Обработка форм
                        if (shapeMap != null && shapeMap.containsKey(symbol)) {
                            specificPartShapes.put(pos, shapeMap.get(symbol));
                            
                            // Если есть в shapeMap, но нет в collisionMap — делаем прозрачным
                            if (collisionMap != null && collisionMap.containsKey(symbol)) {
                                specificCollisionShapes.put(pos, collisionMap.get(symbol));
                            } else {
                                specificCollisionShapes.put(pos, Shapes.empty());
                            }
                        }
                    }
                }
            }
        }
        
        // --- ВАЛИДАЦИЯ КОНТРОЛЛЕРОВ ПОСЛЕ СБОРА ---

        if (foundControllerPositions.isEmpty()) {
            throw new IllegalArgumentException(
                "Structure definition error: NO controller found! " +
                "You must designate exactly one symbol as PartRole.CONTROLLER."
            );
        }

        if (foundControllerPositions.size() > 1) {
            StringBuilder sb = new StringBuilder();
            sb.append("Structure definition error: Multiple controllers found (");
            sb.append(foundControllerPositions.size());
            sb.append("). There can only be ONE controller per structure! Found at: ");
            
            for (int i = 0; i < foundControllerPositions.size(); i++) {
                BlockPos p = foundControllerPositions.get(i);
                sb.append(String.format("[%d, %d, %d]", p.getX(), p.getY(), p.getZ()));
                if (i < foundControllerPositions.size() - 1) {
                    sb.append(", ");
                }
            }
            
            throw new IllegalArgumentException(sb.toString());
        }

        // Если мы здесь, значит контроллер ровно один
        BlockPos finalControllerPos = foundControllerPositions.get(0);

        MultiblockStructureHelper helper = new MultiblockStructureHelper(
            structureMap, phantomBlockState, roleMap, positionSymbolMap, 
            specificPartShapes, specificCollisionShapes, finalControllerPos
        );
        helper.ladderLocalDirections.putAll(ladderDirs);
        helper.energyLocalDirections.putAll(energyDirs);
        return helper;
    }

    private static final Direction[] LADDER_TUPLE_ORDER = {
        Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    private static final Direction[] ENERGY_TUPLE_ORDER = {
        Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP, Direction.DOWN
    };

    private static EnumSet<Direction> ladderTupleToLocalSet(char symbol, boolean[] tuple) {
        if (tuple == null || tuple.length != LADDER_TUPLE_ORDER.length) {
            throw new IllegalArgumentException("ladderSideMap['" + symbol + "'] must be boolean[4] (north,south,west,east)");
        }
        EnumSet<Direction> set = EnumSet.noneOf(Direction.class);
        for (int i = 0; i < LADDER_TUPLE_ORDER.length; i++) {
            if (tuple[i]) {
                set.add(LADDER_TUPLE_ORDER[i]);
            }
        }
        return set;
    }

    private static EnumSet<Direction> energyTupleToLocalSet(char symbol, boolean[] tuple) {
        if (tuple == null || tuple.length != ENERGY_TUPLE_ORDER.length) {
            throw new IllegalArgumentException("energySideMap['" + symbol + "'] must be boolean[6] (north,south,west,east,up,down)");
        }
        EnumSet<Direction> set = EnumSet.noneOf(Direction.class);
        for (int i = 0; i < ENERGY_TUPLE_ORDER.length; i++) {
            if (tuple[i]) {
                set.add(ENERGY_TUPLE_ORDER[i]);
            }
        }
        return set;
    }

    /** Поворот локального направления схемы в мировое при ориентации структуры {@code facing}. */
    private static Direction rotateLocalDirectionToWorld(Direction localDir, Direction facing) {
        BlockPos rotatedVec = rotate(new BlockPos(localDir.getNormal()), facing);
        return Direction.getNearest(rotatedVec.getX(), rotatedVec.getY(), rotatedVec.getZ());
    }

    public static MultiblockStructureHelper createFromLayersWithRoles(
            String[][] layers,
            Map<Character, Supplier<BlockState>> symbolMap,
            Supplier<BlockState> phantomBlockState,
            Map<Character, PartRole> roleMap,
            Map<Character, VoxelShape> shapeMap,
            Map<Character, VoxelShape> collisionMap) {
        return createFromLayersWithRoles(layers, symbolMap, phantomBlockState, roleMap, shapeMap, collisionMap, null, null);
    }

    /**
     * Как полный {@link #createFromLayersWithRoles(String[][], Map, Supplier, Map, Map, Map, Map, Map)},
     * но без {@code shapeMap}/{@code collisionMap} (они {@code null}).
     * Отдельное имя — из‑за стирания типов нельзя перегрузить по {@code Map<Character, VoxelShape>} vs {@code Map<Character, boolean[]>}.
     * Для tuple сторон см. {@link MultiblockSideTuples}.
     */
    public static MultiblockStructureHelper createFromLayersWithRolesAndSides(
            String[][] layers,
            Map<Character, Supplier<BlockState>> symbolMap,
            Supplier<BlockState> phantomBlockState,
            Map<Character, PartRole> roleMap,
            Map<Character, boolean[]> ladderSideMap,
            Map<Character, boolean[]> energySideMap) {
        return createFromLayersWithRoles(layers, symbolMap, phantomBlockState, roleMap, null, null, ladderSideMap, energySideMap);
    }

    public static MultiblockStructureHelper createFromLayersWithRoles(
            String[][] layers,
            Map<Character, Supplier<BlockState>> symbolMap,
            Supplier<BlockState> phantomBlockState,
            Map<Character, PartRole> roleMap) {
        return createFromLayersWithRoles(layers, symbolMap, phantomBlockState, roleMap, null, null, null, null);
    }
    
    /**
     * Получает позицию контроллера в структуре (относительные координаты).
     * 
     * @return BlockPos позиции контроллера относительно центра структуры
     */
    public BlockPos getControllerOffset() {
        return this.controllerOffset;
    }

    /**
     * Возвращает форму конкретной части в мировом пространстве (но без смещения к позиции).
     * @param gridPos Координаты блока в сетке хелпера (из структуры)
     * @param facing Направление всей структуры
     */
    public VoxelShape getSpecificPartShape(BlockPos gridPos, Direction facing) {
        // Если для этого блока в сетке задана кастомная форма
        if (partShapes.containsKey(gridPos)) {
            return rotateShape(partShapes.get(gridPos), facing);
        }
        // По умолчанию — полный блок
        return Shapes.block();
    }

    public VoxelShape getSpecificCollisionShape(BlockPos gridPos, Direction facing) {
        if (collisionShapes.containsKey(gridPos)) {
            return rotateShape(collisionShapes.get(gridPos), facing);
        }
        return Shapes.block(); // По умолчанию твердый
    }
    
    /**
     * Получает роль части по её символу из карты структуры.
     * Использует symbolRoleMap для определения роли.
     * 
     * @param localOffset Локальная позиция части относительно контроллера
     * @param symbol Символ, который был использован для этой позиции в структуре
     * @return PartRole для данной позиции
     */
    // public PartRole getRoleForPosition(BlockPos localOffset, char symbol) {
    //     PartRole role = symbolRoleMap.get(symbol);
    //     return role != null ? role : PartRole.DEFAULT;
    // }
    
    // /**
    //  * Получает символ структуры для данной локальной позиции.
    //  * 
    //  * @param localOffset Локальная позиция части относительно контроллера
    //  * @return Символ структуры или null если позиция не найдена
    //  */
    // public Character getSymbolForPosition(BlockPos localOffset) {
    //     return positionSymbolMap.get(localOffset);
    // }
    
    // /**
    //  * Проверяет, использует ли эта структура рецептоподобный способ определения ролей.
    //  */
    // public boolean hasSymbolRoleMap() {
    //     return !symbolRoleMap.isEmpty();
    // }

    /**
     * Проверяет, является ли коллизия блока в этой позиции полным кубом.
     * Если это не полный куб, игра не должна выталкивать из него игрока.
     */
    public boolean isFullBlock(BlockPos gridPos, Direction facing) {
        VoxelShape shape = getSpecificCollisionShape(gridPos, facing);
        if (shape.isEmpty()) return false;
        
        // Проверяем, совпадает ли граница формы с границами полного блока (0..1)
        // Мы используем небольшой допуск (0.01), чтобы формы вроде 0.05..15.95 считались неполными
        AABB bounds = shape.bounds();
        return bounds.minX <= 0.001 && bounds.minY <= 0.001 && bounds.minZ <= 0.001 &&
            bounds.maxX >= 0.999 && bounds.maxY >= 0.999 && bounds.maxZ >= 0.999;
    }
    
    /**
     * Получает роль для части с учётом обоих способов определения.
     * Если есть symbolRoleMap, использует её; иначе вызывает controller.getPartRole().
     * 
     * @param localOffset Локальная позиция части
     * @param controller Контроллер мультиблока
     * @return PartRole для данной позиции
     */
    public PartRole resolvePartRole(BlockPos gridPos, IMultiblockController controller) {
        Character symbol = positionSymbolMap.get(gridPos);
        if (symbol != null && symbolRoleMap.containsKey(symbol)) return symbolRoleMap.get(symbol);
        return controller.getPartRole(gridPos);
    }

    private final Set<Block> replaceableBlocks = Set.of(
            Blocks.AIR, Blocks.CAVE_AIR, Blocks.VOID_AIR, Blocks.SNOW, Blocks.VINE, Blocks.WATER, Blocks.LAVA
    );

    // Новый приватный метод для проверки, можно ли заменить блок
    private boolean isBlockReplaceable(BlockState state) {
        // Сначала проверяем наш базовый Set
        if (replaceableBlocks.contains(state.getBlock())) {
            return true;
        }

        // Теперь проверяем по тегам. Это включает все цветы, траву, саженцы и т.д.
        return state.is(BlockTags.REPLACEABLE_BY_TREES) || // Трава, папоротники
               state.is(BlockTags.FLOWERS) ||            // Все виды цветов
               state.is(BlockTags.SAPLINGS);             // Саженцы деревьев
    }

    public boolean checkPlacement(Level level, BlockPos controllerPos, Direction facing, Player player) {
        List<BlockPos> obstructions = new ArrayList<>();
        for (BlockPos relativePos : structureMap.keySet()) {
            if (relativePos.equals(BlockPos.ZERO)) continue;
            BlockPos worldPos = getRotatedPos(controllerPos, relativePos, facing);
            BlockState existingState = level.getBlockState(worldPos);

            // Используем наш новый метод для проверки
            if (!isBlockReplaceable(existingState)) {
                obstructions.add(worldPos);
            }
        }

        if (!obstructions.isEmpty()) {
            if (player instanceof ServerPlayer serverPlayer) {
                // Проверяем, включена ли опция в конфиге, перед отправкой пакета
                if (ModClothConfig.get().obstructionHighlight.enableObstructionHighlight) {
                    ModPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new HighlightBlocksPacket(obstructions));
                }
            }
            player.displayClientMessage(Component.translatable("chat.hbm_m.structure.obstructed"), true);
            return false;
        }

        return true;
    }

    /**
     * @return A Set of all local offsets for the multiblock parts, relative to the controller.
     */
    public Set<BlockPos> getPartOffsets() {
        return this.partOffsets; // Возвращаем уже созданный Set
    }

    public synchronized void placeStructure(Level level, BlockPos controllerPos, Direction facing, IMultiblockController controller) {
        if (level.isClientSide) return;

        List<BlockPos> energyConnectorPositions = new ArrayList<>();
        List<BlockPos> allPlacedPositions = new ArrayList<>();
        
        for (Map.Entry<BlockPos, Supplier<BlockState>> entry : structureMap.entrySet()) {
            BlockPos gridPos = entry.getKey();
            BlockPos worldPos = getRotatedPos(controllerPos, gridPos, facing);

            if (worldPos.equals(controllerPos)) {
                continue;
            }
            
            BlockState partState = phantomBlockState.get().setValue(HorizontalDirectionalBlock.FACING, facing);
            level.setBlock(worldPos, partState, 2);
            allPlacedPositions.add(worldPos);
            
            BlockEntity be = level.getBlockEntity(worldPos);
            if (be instanceof IMultiblockPart partBe) {
                partBe.setControllerPos(controllerPos);
                PartRole role = resolvePartRole(gridPos, controller);
                if (controller instanceof com.hbm_m.block.decorations.DoorBlock doorBlock) {
                    // Получаем ID декларации
                    String declId = doorBlock.getDoorDeclId();
                    // Получаем саму декларацию из реестра
                    com.hbm_m.block.entity.doors.DoorDecl decl = 
                        com.hbm_m.block.entity.doors.DoorDeclRegistry.getById(declId);
                    
                    if (decl != null && decl.getStructureDefinition() != null) {
                        // Берем роль прямо из карты схемы
                        PartRole definedRole = decl.getStructureDefinition().getRoles().get(gridPos);
                        if (definedRole != null) {
                            role = definedRole;
                        }
                    }
                }
                partBe.setPartRole(role);

                if (role == PartRole.LADDER) {
                    Set<Direction> localSides = ladderLocalDirections.get(gridPos);
                    EnumSet<Direction> worldSides = EnumSet.noneOf(Direction.class);
                    
                    if (localSides != null && !localSides.isEmpty()) {
                        for (Direction localDir : localSides) {
                            worldSides.add(rotateLocalDirectionToWorld(localDir, facing));
                        }
                    } else {
                        // Нет записи в ladderSideMap — все горизонтальные стороны
                        worldSides.add(Direction.NORTH);
                        worldSides.add(Direction.SOUTH);
                        worldSides.add(Direction.EAST);
                        worldSides.add(Direction.WEST);
                    }
                    partBe.setAllowedClimbSides(worldSides);
                }
                
                // ENERGY_CONNECTOR и UNIVERSAL_CONNECTOR — стороны из energySideMap или все шесть
                if (role == PartRole.ENERGY_CONNECTOR || role == PartRole.UNIVERSAL_CONNECTOR) {
                    Set<Direction> localEnergy = energyLocalDirections.get(gridPos);
                    EnumSet<Direction> worldEnergy = EnumSet.noneOf(Direction.class);
                    if (localEnergy != null && !localEnergy.isEmpty()) {
                        for (Direction localDir : localEnergy) {
                            worldEnergy.add(rotateLocalDirectionToWorld(localDir, facing));
                        }
                    } else {
                        Collections.addAll(worldEnergy, Direction.values());
                    }
                    partBe.setAllowedEnergySides(worldEnergy);
                    energyConnectorPositions.add(worldPos);
                }
                
                // === ЗАГЛУШКА ДЛЯ CONVEYOR СИСТЕМЫ ===
                // UNIVERSAL_CONNECTOR является точкой подключения conveyor системы,
                // но сам по себе НЕ передаёт и не принимает items/fluids.
                // Передача происходит через связанные conveyor блоки (извлекающие/подающие),
                // которые будут реализованы позже.
                if (role == PartRole.UNIVERSAL_CONNECTOR) {
                    // TODO: Реализовать conveyor логику
                    // - Проверить соседние блоки на наличие conveyor системы
                    // - Получить направление извлечения/подачи от conveyor блока
                    // - Передать items/fluids через UNIVERSAL_CONNECTOR
                }
            }
        }
        
        // ОДНО сообщение со всеми координатами
        MainRegistry.LOGGER.info("Player {} placed multiblock at {} with {} parts: {}", 
            controller, controllerPos, allPlacedPositions.size(), formatPositions(allPlacedPositions));
        
        updateFrameForController(level, controllerPos);
        
        // ОДНО массовое обновление в конце вместо 156
        for (BlockPos connectorPos : energyConnectorPositions) {
            for (Direction dir : Direction.values()) {
                BlockPos wirePos = connectorPos.relative(dir);
                BlockState wireState = level.getBlockState(wirePos);
                if (wireState.getBlock() instanceof WireBlock) {
                    level.updateNeighborsAt(wirePos, wireState.getBlock());
                }
            }
        }
    }
    
    // Вспомогательный метод для красивого форматирования координат
    private String formatPositions(List<BlockPos> positions) {
        if (positions.isEmpty()) return "[]";
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < positions.size(); i++) {
            BlockPos pos = positions.get(i);
            sb.append(String.format("(%d,%d,%d)", pos.getX(), pos.getY(), pos.getZ()));
            if (i < positions.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }    
    
    public void destroyStructure(Level level, BlockPos controllerPos, Direction facing) {
        if (level.isClientSide || IS_DESTROYING.get()) return;
        IS_DESTROYING.set(true);
        try {
            for (BlockPos gridPos : structureMap.keySet()) {
                BlockPos worldPos = getRotatedPos(controllerPos, gridPos, facing);
                if (level.getBlockState(worldPos).getBlock() instanceof UniversalMachinePartBlock) {
                    level.setBlock(worldPos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        } finally { IS_DESTROYING.set(false); }
    }

    /**
     * Получает максимальную Y-координату структуры (высоту верхнего пояса)
     */
    private int getMaxY() {
        int maxY = Integer.MIN_VALUE;
        for (BlockPos local : structureMap.keySet()) {
            if (local.getY() > maxY) {
                maxY = local.getY();
            }
        }
        return maxY;
    }

    public Map<BlockPos, Supplier<BlockState>> getStructureMap() {
        return structureMap;
    }

    /**
     * Проверяет, является ли данная локальная позиция частью верхнего пояса структуры
     * @param localOffset Локальная позиция относительно контроллера (БЕЗ поворота)
     * @return true если блок находится на максимальной высоте структуры
     */
    public boolean isTopRingPart(BlockPos localOffset) {
        int maxY = getMaxY();
        return localOffset.getY() == maxY;
    }

    /**
     * Получает список мировых позиций всех частей верхнего пояса структуры
     * @param controllerPos Позиция контроллера в мире
     * @param facing Направление структуры
     * @return Список мировых позиций блоков верхнего пояса
     */
    public List<BlockPos> getTopRingWorldPositions(BlockPos controllerPos, Direction facing) {
        List<BlockPos> topRing = new ArrayList<>();
        int maxY = getMaxY();
        for (BlockPos localOffset : structureMap.keySet()) {
            if (localOffset.getY() == maxY) {
                BlockPos worldPos = getRotatedPos(controllerPos, localOffset, facing);
                topRing.add(worldPos);
            }
        }
        return topRing;
    }


    /**
     * Вычисляет, должна ли рамка быть видимой для данной структуры
     * Рамка видна, если над любой частью верхнего пояса есть непустой блок
     */
    public boolean computeFrameVisible(Level level, BlockPos controllerPos, Direction facing) {
        List<BlockPos> topRingWorld = getTopRingWorldPositions(controllerPos, facing);
        for (BlockPos p : topRingWorld) {
            BlockPos above = p.above();
            if (!level.isEmptyBlock(above)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Обновляет состояние рамки для контроллера с синхронизацией клиента
     */
    public static void updateFrameForController(Level level, BlockPos controllerPos) {
        if (level.isClientSide()) return;

        BlockState state = level.getBlockState(controllerPos);
        Block block = state.getBlock(); // Получаем сам блок

        // Проверяем, что БЛОК является контроллером
        if (!(block instanceof IMultiblockController controller)) {
            return;
        }
        
        BlockEntity be = level.getBlockEntity(controllerPos);
        // Проверяем, что BlockEntity поддерживает рамку
        if (!(be instanceof IFrameSupportable frameSupportable)) {
            return;
        }
        
        if (!state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return;
        }

        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);

        // Получаем хелпер из КОНТРОЛЛЕРА (блока)
        MultiblockStructureHelper helper = controller.getStructureHelper();
        if (helper == null) {
            return;
        }

        // Вычисляем видимость рамки
        boolean visible = helper.computeFrameVisible(level, controllerPos, facing);

        // MachineAdvancedAssemblerBlock: храним FRAME в BlockState для запекания в чанк (Embeddium/Sodium)
        if (block instanceof MachineAdvancedAssemblerBlock) {
            BlockState currentState = level.getBlockState(controllerPos);
            if (currentState.hasProperty(MachineAdvancedAssemblerBlock.FRAME)
                    && currentState.getValue(MachineAdvancedAssemblerBlock.FRAME) != visible) {
                level.setBlock(controllerPos, currentState.setValue(MachineAdvancedAssemblerBlock.FRAME, visible), 3);
            }
            return;
        }

        // Остальные контроллеры (двери и т.д.): применяем через интерфейс BlockEntity
        if (be instanceof IFrameSupportable fs) {
            fs.setFrameVisible(visible);
        }
    }


    /**
     * Вызывается из фантомной части при изменении соседа
     */
    public static void onNeighborChangedForPart(Level level, BlockPos partPos, BlockPos changedPos) {
        if (level.isClientSide() || level.getServer() == null) {
            return;
        }

        BlockEntity partBe = level.getBlockEntity(partPos);
        if (!(partBe instanceof IMultiblockPart part)) return;

        BlockPos ctrlPos = part.getControllerPos();
        if (ctrlPos == null) return;

        BlockState controllerState = level.getBlockState(ctrlPos);
        Block controllerBlock = controllerState.getBlock();

        if (!(controllerBlock instanceof IMultiblockController controller)) {
            // Эта проверка важна, если контроллер был разрушен
            return;
        }

        // Проверяем, что изменение произошло НАД частью верхнего пояса.
        // Эту проверку делаем сразу, чтобы не планировать лишних задач.
        MultiblockStructureHelper helper = controller.getStructureHelper();
        BlockPos worldOffset = partPos.subtract(ctrlPos);
        Direction facing = controllerState.getValue(HorizontalDirectionalBlock.FACING);
        BlockPos localOffset = rotateBack(worldOffset, facing);

        if (helper.isTopRingPart(localOffset) && changedPos.equals(partPos.above())) {
            
            // Вместо прямого вызова, планируем задачу на следующий тик сервера.
            level.getServer().execute(() -> {
                BlockEntity be = level.getBlockEntity(ctrlPos);
                if (be != null && be.isRemoved() == false && 
                    level.getBlockState(ctrlPos).is(controllerBlock)) {
                    updateFrameForController(level, ctrlPos);
                }
            });
        }
    }

    /**
     * Обратное вращение для получения локального оффсета из мировой позиции
     */
    public static BlockPos rotateBack(BlockPos pos, Direction facing) {
        return switch (facing) {
            case SOUTH -> new BlockPos(-pos.getX(), pos.getY(), -pos.getZ());
            case WEST -> new BlockPos(-pos.getZ(), pos.getY(), pos.getX());
            case EAST -> new BlockPos(pos.getZ(), pos.getY(), -pos.getX());
            default -> pos; // NORTH
        };
    }


    /**
     * Automatically generates a VoxelShape for the structure by combining
     * 1x1x1 cubes for each part. Ideal for non-rectangular structures.
     *
     * @param facing The direction of the structure.
     * @return The generated VoxelShape.
     */
    public VoxelShape generateShapeFromParts(Direction facing) {
        return shapeCache.computeIfAbsent(facing, f -> {
            VoxelShape finalShape = Shapes.empty();

            // 1. Добавляем контроллер
            // Если для контроллера задана форма, используем её (с вращением), иначе полный блок
            VoxelShape controllerShape = partShapes.getOrDefault(this.controllerOffset, Block.box(0, 0, 0, 16, 16, 16));
            // Вращаем форму контроллера (если структура повернута)
            controllerShape = rotateShape(controllerShape, f);
            finalShape = Shapes.or(finalShape, controllerShape);

            // 2. Добавляем остальные части
            for (BlockPos gridPos : structureMap.keySet()) {
                BlockPos relative = gridPos.subtract(this.controllerOffset);
                BlockPos rotatedPos = rotate(relative, f); // Позиция блока в мире относительно контроллера

                // Получаем форму части (или полный куб по умолчанию)
                VoxelShape rawShape = partShapes.getOrDefault(gridPos, Block.box(0, 0, 0, 16, 16, 16));
                
                // ВАЖНО: Вращаем саму форму (например, если блок сплющен по Z, при повороте он должен сплющиться по X)
                VoxelShape rotatedShape = rotateShape(rawShape, f);
                
                // Сдвигаем форму на позицию блока
                VoxelShape placedShape = rotatedShape.move(rotatedPos.getX(), rotatedPos.getY(), rotatedPos.getZ());

                finalShape = Shapes.or(finalShape, placedShape);
            }
            return finalShape.optimize();
        });
    }

    public static VoxelShape rotateShape(VoxelShape shape, Direction facing) {
        if (facing == Direction.NORTH) return shape;
        
        // Создаем пустую форму
        VoxelShape[] buffer = { Shapes.empty() };
        
        // Проходим по всем AABB внутри формы
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            double newMinX = minX, newMinZ = minZ, newMaxX = maxX, newMaxZ = maxZ;
            
            switch (facing) {
                case SOUTH:
                    newMinX = 1.0 - maxX; newMaxX = 1.0 - minX;
                    newMinZ = 1.0 - maxZ; newMaxZ = 1.0 - minZ;
                    break;
                case WEST:
                    newMinX = minZ; newMaxX = maxZ;
                    newMinZ = 1.0 - maxX; newMaxZ = 1.0 - minX;
                    break;
                case EAST:
                    newMinX = 1.0 - maxZ; newMaxX = 1.0 - minZ;
                    newMinZ = minX; newMaxZ = maxX;
                    break;
                default: break;
            }
            
            // Объединяем повернутые коробки
            buffer[0] = Shapes.joinUnoptimized(buffer[0], Shapes.box(newMinX, minY, newMinZ, newMaxX, maxY, newMaxZ), BooleanOp.OR);
        });
        
        return buffer[0];
    }

    public BlockPos getRotatedPos(BlockPos controllerWorldPos, BlockPos partLocalPos, Direction facing) {
        // ВАЖНО: сначала находим смещение части ОТНОСИТЕЛЬНО контроллера в сетке хелпера
        BlockPos offsetFromController = partLocalPos.subtract(this.controllerOffset);
        
        // Затем поворачиваем это смещение и прибавляем к мировой позиции контроллера
        return controllerWorldPos.offset(rotate(offsetFromController, facing));
    }

    public static BlockPos rotate(BlockPos pos, Direction facing) {
        return switch (facing) {
            case SOUTH -> new BlockPos(-pos.getX(), pos.getY(), -pos.getZ());
            case WEST -> new BlockPos(pos.getZ(), pos.getY(), -pos.getX());
            case EAST -> new BlockPos(-pos.getZ(), pos.getY(), pos.getX());
            default -> pos; // NORTH
        };
    }

    public List<BlockPos> getAllPartPositions(BlockPos controllerPos, Direction facing) {
        List<BlockPos> positions = new ArrayList<>();
        
        for (BlockPos localOffset : structureMap.keySet()) {
            BlockPos worldPos = getRotatedPos(controllerPos, localOffset, facing);
            positions.add(worldPos);
        }
        
        return positions;
    }
}
