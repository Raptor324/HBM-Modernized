package com.hbm_m.multiblock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
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
import com.hbm_m.block.UniversalMachinePartBlock;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.interfaces.IMultiblockPart;
import com.hbm_m.interfaces.IMultiblockSidedIO;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.network.HighlightBlocksPacket;

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

public class MultiblockStructureHelper {
    
    // ThreadLocal флаг для предотвращения рекурсии при разрушении структуры
    private static final ThreadLocal<Boolean> IS_DESTROYING = ThreadLocal.withInitial(() -> false);

    private static final ThreadLocal<Boolean> IS_REPAIRING = ThreadLocal.withInitial(() -> false);

    /**
     * Радиус fallback-поиска контроллера для осиротевшей части (только когда у части
     * НЕТ {@code localOffsetFromController} — legacy NBT). Должен покрывать большие
     * структуры (двери 20x20): половина диагонали такой двери ~14 блоков, берём с запасом.
     * Основной путь — детерминированный, без всякого поиска
     * ({@link #relinkOrphanedPartDeterministic}).
     */
    private static final int RELINK_SEARCH_RADIUS = 24;

    public static boolean isDestroying() {
        return IS_DESTROYING.get();
    }
    public static boolean isRepairing() {
        return IS_REPAIRING.get();
    }

    /**
     * Окно (в тиках) подавления взаимодействия со свежеустановленной структурой.
     *
     * <p>Ваниль повторяет пакет {@code UseItemOn} каждые 4 тика, пока зажата ПКМ. Из-за
     * placement-offset структура занимает точку прицела сразу после установки, поэтому
     * повторный пакет того же клика попадает уже в станок и открывает GUI «мгновенно
     * после установки». Сюда пишется время установки каждого контроллера
     * (см. {@link #placeStructure}); гард в {@code MultiblockClickGuardForge/NeoForge}
     * глушит такие клики в течение окна.
     */
    private static final int PLACEMENT_CLICK_SUPPRESS_TICKS = 10;
    private static final Map<net.minecraft.resources.ResourceKey<Level>, Map<BlockPos, Long>> RECENT_PLACEMENTS = new HashMap<>();

    public static void markRecentlyPlaced(Level level, BlockPos controllerPos) {
        if (level.isClientSide) return;
        long now = level.getGameTime();
        Map<BlockPos, Long> byDim = RECENT_PLACEMENTS.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        byDim.values().removeIf(t -> now - t > PLACEMENT_CLICK_SUPPRESS_TICKS);
        byDim.put(controllerPos.immutable(), now);
    }

    /**
     * Истинно, если клик по {@code clickedPos} (сам контроллер или любая его часть)
     * приходится на структуру, установленную в пределах {@link #PLACEMENT_CLICK_SUPPRESS_TICKS}.
     */
    public static boolean isRecentlyPlacedInteraction(Level level, BlockPos clickedPos) {
        if (RECENT_PLACEMENTS.isEmpty()) return false;
        BlockPos controllerPos = clickedPos;
        BlockEntity be = level.getBlockEntity(clickedPos);
        if (be instanceof IMultiblockPart part) {
            BlockPos stored = part.getControllerPos();
            if (stored == null) return false;
            controllerPos = stored;
        }
        if (!(level.getBlockState(controllerPos).getBlock() instanceof IMultiblockController)) return false;
        Map<BlockPos, Long> byDim = RECENT_PLACEMENTS.get(level.dimension());
        if (byDim == null) return false;
        Long placedAt = byDim.get(controllerPos);
        if (placedAt == null) return false;
        if (level.getGameTime() - placedAt > PLACEMENT_CLICK_SUPPRESS_TICKS) {
            byDim.remove(controllerPos);
            return false;
        }
        return true;
    }

    private final Map<BlockPos, Supplier<BlockState>> structureMap;
    private final Supplier<BlockState> phantomBlockState;
    // Карта символов на роли для рецептоподобного определения структуры
    private final Map<Character, PartRole> symbolRoleMap;
    // Карта позиций на символы для определения роли при постройке
    private final Map<BlockPos, Character> positionSymbolMap;
    private final Map<Direction, VoxelShape> shapeCache = new HashMap<>();
    /**
     * Cache for {@link #getLocalRenderBoundingBox(Direction)} - the AABB enclosing
     * every part of the structure in controller-local coords with the given
     * rotation pre-applied. Independent of any single BlockEntity's world
     * position; one entry per facing direction is enough for ALL BlockEntities
     * sharing this helper. Lazily populated on first request.
     */
    private final EnumMap<Direction, AABB> localRenderBoundsCache = new EnumMap<>(Direction.class);
    private final Map<BlockPos, Set<Direction>> ladderLocalDirections = new HashMap<>();
    /** Локальные стороны энергоприёма/отдачи относительно схемы (до поворота FACING). Ключ - позиция в сетке. */
    private final Map<BlockPos, Set<Direction>> energyLocalDirections = new HashMap<>();
    /** Локальные стороны жидкостного подключения относительно схемы (до поворота FACING). Ключ - позиция в сетке. */
    private final Map<BlockPos, Set<Direction>> fluidLocalDirections = new HashMap<>();
    /** Локальные стороны подключения энергии на самом контроллере (если заданы). Пусто = все стороны. */
    private Set<Direction> controllerEnergyLocalDirections = Collections.emptySet();
    /** Локальные стороны подключения жидкостей на самом контроллере. Если {@link #controllerFluidSidesFromStructure} — множество из tuple (пусто = ни одной стороны); иначе не используется для apply (устар.). */
    private Set<Direction> controllerFluidLocalDirections = Collections.emptySet();
    /** В fluidSideMap указан символ контроллера — {@link #applyControllerSideConfig} вызывает {@link IMultiblockSidedIO#setAllowedFluidSidesFromMultiblockStructure}. */
    private boolean controllerFluidSidesFromStructure = false;
    private final Map<BlockPos, VoxelShape> partShapes;
    private final Map<BlockPos, VoxelShape> collisionShapes;
    // Позиция контроллера в структуре (относительно центра)
    private final BlockPos controllerOffset;
    /**
     * GIT {@code BlockDummyable.getOffset()}: смещение ядра от клетки клика по {@code FACING.getOpposite()}.
     * 0 — контроллер ставится прямо в точку клика.
     */
    private int placementOffset;

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
        this.placementOffset = computePlacementOffsetFromStructure(this.partOffsets, this.controllerOffset);
    }

    private static int computePlacementOffsetFromStructure(Set<BlockPos> partOffsets, BlockPos controllerOffset) {
        // Ищем минимальный Z (самую ближнюю к игроку деталь, так как NORTH это -Z)
        int minZ = controllerOffset.getZ();
        for (BlockPos offset : partOffsets) {
            minZ = Math.min(minZ, offset.getZ());
        }
        // Возвращаем дистанцию от ядра до лицевой части
        return controllerOffset.getZ() - minZ;
    }

    
    
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
     * @param ladderSideMap необязательно: для символов с ролью LADDER - boolean[4] в порядке north, south, west, east.
     *                      Если символа нет в карте - лестница со всех четырёх горизонтальных сторон.
     * @param energySideMap необязательно: для ENERGY_CONNECTOR / UNIVERSAL_CONNECTOR - boolean[6] в порядке north, south, west, east, up, down.
     *                      Если символа нет в карте - энергия со всех шести сторон.
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
            Map<Character, boolean[]> energySideMap,
            Map<Character, boolean[]> fluidSideMap) {
        
        Map<BlockPos, Supplier<BlockState>> structureMap = new HashMap<>();
        Map<BlockPos, Character> positionSymbolMap = new HashMap<>();
        Map<BlockPos, Set<Direction>> ladderDirs = new HashMap<>();
        Map<BlockPos, Set<Direction>> energyDirs = new HashMap<>();
        Map<BlockPos, Set<Direction>> fluidDirs = new HashMap<>();
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
                            Supplier<BlockState> partSupplier = phantomBlockState;
                            if (symbolMap != null) {
                                Supplier<BlockState> mapped = symbolMap.get(symbol);
                                if (mapped != null) {
                                    partSupplier = mapped;
                                }
                            }
                            structureMap.put(pos, partSupplier);
                            if (role == PartRole.LADDER && ladderSideMap != null && ladderSideMap.containsKey(symbol)) {
                                ladderDirs.put(pos, ladderTupleToLocalSet(symbol, ladderSideMap.get(symbol)));
                            }
                            if ((role == PartRole.ENERGY_CONNECTOR || role == PartRole.UNIVERSAL_CONNECTOR)
                                    && energySideMap != null && energySideMap.containsKey(symbol)) {
                                energyDirs.put(pos, energyTupleToLocalSet(symbol, energySideMap.get(symbol)));
                            }
                            if ((role == PartRole.FLUID_CONNECTOR || role == PartRole.UNIVERSAL_CONNECTOR)
                                    && fluidSideMap != null && fluidSideMap.containsKey(symbol)) {
                                fluidDirs.put(pos, fluidTupleToLocalSet(symbol, fluidSideMap.get(symbol)));
                            }
                        }
                        positionSymbolMap.put(pos, symbol);

                        // Обработка форм
                        if (shapeMap != null && shapeMap.containsKey(symbol)) {
                            specificPartShapes.put(pos, shapeMap.get(symbol));
                            
                            // Если есть в shapeMap, но нет в collisionMap - делаем прозрачным
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
        helper.fluidLocalDirections.putAll(fluidDirs);

        // Стороны контроллера (опционально): берём из sideMap по символу контроллера.
        Character controllerSymbol = positionSymbolMap.get(finalControllerPos);
        if (controllerSymbol != null) {
            if (energySideMap != null && energySideMap.containsKey(controllerSymbol)) {
                helper.controllerEnergyLocalDirections = energyTupleToLocalSet(controllerSymbol, energySideMap.get(controllerSymbol));
            }
            if (fluidSideMap != null && fluidSideMap.containsKey(controllerSymbol)) {
                helper.controllerFluidLocalDirections = fluidTupleToLocalSet(controllerSymbol, fluidSideMap.get(controllerSymbol));
                helper.controllerFluidSidesFromStructure = true;
            }
        }
        helper.placementOffset = computePlacementOffset(finalControllerPos, positionSymbolMap);
        return helper;
    }

    /**
     * Авто-расчёт offset как максимальное локальное +Z от контроллера (лицевая грань при FACING=NORTH),
     * как {@code BlockDummyable.getOffset()} у симметричных мультиблоков 1.7.10.
     */
    private static int computePlacementOffset(BlockPos controllerPos, Map<BlockPos, Character> occupiedCells) {
        int minZ = 0;
        for (BlockPos gridPos : occupiedCells.keySet()) {
            BlockPos rel = gridPos.subtract(controllerPos);
            minZ = Math.min(minZ, rel.getZ());
        }
        // Эквивалентно 0 - minZ
        return -minZ;
    }

    public MultiblockStructureHelper withPlacementOffset(int offset) {
        this.placementOffset = offset;
        return this;
    }

    private static BlockPos getMinPos(BlockPos p1, BlockPos p2) {
        return new BlockPos(Math.min(p1.getX(), p2.getX()), Math.min(p1.getY(), p2.getY()), Math.min(p1.getZ(), p2.getZ()));
    }

    private static BlockPos getMaxPos(BlockPos p1, BlockPos p2) {
        return new BlockPos(Math.max(p1.getX(), p2.getX()), Math.max(p1.getY(), p2.getY()), Math.max(p1.getZ(), p2.getZ()));
    }

    /**
     * Пытается обновить структуру до новой версии.
     * Сдвигает контроллер, если это старый сейв, перестраивает фантомы при изменении размеров или свойств,
     * а также обновляет рамку выделения.
     */
    public boolean attemptAutoRepair(Level level, BlockPos currentCtrlPos, BlockState state, IMultiblockController controller) {
        if (level.isClientSide) return false;

        // Идёт перенос блоков движком сборки (Create/Sable): структура сейчас
        // "в пути", починка/перепривязка по промежуточным состояниям даст ложные
        // срабатывания (фантомы-дубликаты). Откладываем до закрытия окна -
        // части сами перепривяжутся через retry-механизм relink.
        if (ContraptionAssemblyGuard.isMoving()) return false;

        // 1. ОТКЛАДЫВАЕМ ПОЧИНКУ, если поблизости нет игроков.
        // Это разгружает сервер во время старта/загрузки мира и гарантирует, что чанки вокруг будут активны.
        if (level.getNearestPlayer(currentCtrlPos.getX() + 0.5, currentCtrlPos.getY() + 0.5, currentCtrlPos.getZ() + 0.5, 64.0D, false) == null) {
            return false;
        }

        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        
        // 2. ВЫЧИСЛЯЕМ ОЖИДАЕМЫЕ ПОЗИЦИИ НОВОЙ СТРУКТУРЫ
        Map<BlockPos, BlockPos> expectedPosToGrid = new java.util.HashMap<>();
        for (Map.Entry<BlockPos, java.util.function.Supplier<BlockState>> entry : this.structureMap.entrySet()) {
            BlockPos gridPos = entry.getKey();
            BlockPos expectedPos = this.getRotatedPos(currentCtrlPos, gridPos, facing);
            if (!expectedPos.equals(currentCtrlPos)) {
                expectedPosToGrid.put(expectedPos, gridPos);
            }
        }
        Set<BlockPos> expectedCurrentPositions = expectedPosToGrid.keySet();

        // 3. ВЫЧИСЛЯЕМ ОЖИДАЕМЫЕ ПОЗИЦИИ СТАРОЙ СТРУКТУРЫ (ЕСЛИ БЫЛ СДВИГ ЯДРА)
        BlockPos newCtrlPos = currentCtrlPos;
        BlockPos oldOffset = controller.getLegacyControllerOffset();
        Set<BlockPos> expectedLegacyPositions = new java.util.HashSet<>();
        
        if (oldOffset != null) {
            for (BlockPos gridPos : this.structureMap.keySet()) {
                BlockPos offsetFromOld = gridPos.subtract(oldOffset);
                BlockPos expectedPos = currentCtrlPos.offset(rotate(offsetFromOld, facing));
                if (!expectedPos.equals(currentCtrlPos)) {
                    expectedLegacyPositions.add(expectedPos);
                }
            }
        }

        // 4. ОПРЕДЕЛЯЕМ ГРАНИЦЫ И ПРОВЕРЯЕМ БЕЗОПАСНОСТЬ (БЕЗ ДЕДЛОКОВ)
        int minX = currentCtrlPos.getX();
        int minY = currentCtrlPos.getY();
        int minZ = currentCtrlPos.getZ();
        int maxX = currentCtrlPos.getX();
        int maxY = currentCtrlPos.getY();
        int maxZ = currentCtrlPos.getZ();

        for (BlockPos pos : expectedCurrentPositions) {
            minX = Math.min(minX, pos.getX()); minY = Math.min(minY, pos.getY()); minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX()); maxY = Math.max(maxY, pos.getY()); maxZ = Math.max(maxZ, pos.getZ());
        }
        for (BlockPos pos : expectedLegacyPositions) {
            minX = Math.min(minX, pos.getX()); minY = Math.min(minY, pos.getY()); minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX()); maxY = Math.max(maxY, pos.getY()); maxZ = Math.max(maxZ, pos.getZ());
        }

        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;

        // БЕЗОПАСНАЯ ЗАЩИТА: Проверяем, что затронутые чанки И ИХ СОСЕДИ (радиус +1 чанк) загружены.
        // Это на 100% предотвращает каскадную загрузку чанков при neighbor updates (setBlock / removeBlock).
        for (int cx = minChunkX - 1; cx <= maxChunkX + 1; cx++) {
            for (int cz = minChunkZ - 1; cz <= maxChunkZ + 1; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    return false; // Чанк не загружен полностью, откладываем починку
                }
            }
        }

        // 5. ИЩЕМ ВСЕ СУЩЕСТВУЮЩИЕ ФАНТОМЫ, ПРИНАДЛЕЖАЩИЕ ЭТОМУ КОНТРОЛЛЕРУ
        Set<BlockPos> existingPhantoms = new java.util.HashSet<>();
        Block phantomBlock = this.phantomBlockState.get().getBlock();

        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return false;
        }

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                net.minecraft.world.level.chunk.LevelChunk chunk =
                        serverLevel.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    // Чанк ещё не полностью готов "прямо сейчас" - откладываем починку,
                    // вместо того чтобы блокировать поток сервера.
                    return false;
                }
                int chunkMinX = Math.max(minX, cx << 4);
                int chunkMaxX = Math.min(maxX, (cx << 4) + 15);
                int chunkMinZ = Math.max(minZ, cz << 4);
                int chunkMaxZ = Math.min(maxZ, (cz << 4) + 15);
                
                for (int x = chunkMinX; x <= chunkMaxX; x++) {
                    for (int z = chunkMinZ; z <= chunkMaxZ; z++) {
                        for (int y = minY; y <= maxY; y++) {
                            BlockPos pos = new BlockPos(x, y, z);
                            BlockState posState = chunk.getBlockState(pos);
                            if (posState.is(phantomBlock) || posState.getBlock() instanceof UniversalMachinePartBlock) {
                                BlockEntity be = chunk.getBlockEntity(pos);
                                if (be instanceof IMultiblockPart part && currentCtrlPos.equals(part.getControllerPos())) {
                                    existingPhantoms.add(pos);
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5а. КОНФЛИКТ С ЧУЖИМ ЖИВЫМ МУЛЬТИБЛОКОМ: в ожидаемом футпринте лежат части,
        // принадлежащие ДРУГОМУ ЖИВОМУ контроллеру (дубликат после сбоя сборки/разборки).
        // Раньше два таких контроллера бесконечно восстанавливали блоки друг друга
        // («зомби-станок», который невозможно сломать). Теперь проигравший уходит сам:
        // ломаем себя с дропом и отпускаем свои фантомы (onRemove-каскад подчистит их).
        java.util.Set<BlockPos> foreignOwned = new java.util.HashSet<>();
        for (BlockPos pos : expectedCurrentPositions) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof IMultiblockPart other) || pos.equals(currentCtrlPos)) continue;
            BlockPos otherCtrl = other.getControllerPos();
            if (otherCtrl == null || otherCtrl.equals(currentCtrlPos)) continue;
            // Чужая часть считается «живой» только если её контроллер реально существует:
            // осиротевшие части с битой ссылкой обрабатывает reclaimOrphanedParts ниже.
            if (level.getBlockState(otherCtrl).getBlock() instanceof IMultiblockController) {
                foreignOwned.add(pos);
            }
        }
        if (!foreignOwned.isEmpty()) {
            MainRegistry.LOGGER.error(
                "[HBM] attemptAutoRepair: контроллер {} конфликтует с другим живым мультиблоком ({} его частей в нашем футпринте, напр. {}). Самоуничтожение вместо войны починки.",
                currentCtrlPos.toShortString(), foreignOwned.size(),
                foreignOwned.iterator().next().toShortString());
            IS_REPAIRING.set(true);
            try {
                net.minecraft.world.level.block.Block.dropResources(level.getBlockState(currentCtrlPos), level, currentCtrlPos, level.getBlockEntity(currentCtrlPos));
                level.removeBlock(currentCtrlPos, false);
            } finally {
                IS_REPAIRING.set(false);
            }
            return false;
        }

        // 5б. ПЕРЕПРИВЯЗКА ОСИРОТЕВШИХ ЧАСТЕЙ (восстановление после Create/Sable disassembly):
        // Create при disassembly восстанавливает BlockEntity из сохранённого NBT, переписывая
        // только собственные x/y/z, но НЕ трогая "ControllerPos" части. Поэтому часть может
        // указывать на старую (теперь пустую) позицию контроллера. Здесь мы перепривязываем
        // такие части к текущему контроллеру, чтобы штатная починка ниже прошла без физической
        // перестройки блоков и без потери NBT части.
        reclaimOrphanedParts(level, currentCtrlPos, facing, controller, expectedPosToGrid, existingPhantoms);

        // 6. БЫСТРАЯ ПРОВЕРКА: ИДЕАЛЬНА ЛИ СТРУКТУРА?
        boolean isPerfect = existingPhantoms.size() == expectedCurrentPositions.size() && existingPhantoms.containsAll(expectedCurrentPositions);
        
        if (isPerfect) {
            for (BlockPos pos : expectedCurrentPositions) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof IMultiblockPart part) {
                    BlockPos gridPos = expectedPosToGrid.get(pos);
                    if (part.getPartRole() != resolvePartRole(gridPos, controller)) {
                        isPerfect = false; // Роли поменялись
                        break;
                    }
                }
            }
        }
        
        if (isPerfect) {
            MultiblockFrameHelper.updateFrameForController(level, currentCtrlPos);
            return true; // Структура в порядке, починка не требуется
        }

        // === ОПТИМИЗИРОВАННАЯ ПОЧИНКА БЕЗ ПЕРЕСТРОЙКИ (СВАП РОЛЕЙ И ПАРАМЕТРОВ IN-PLACE) ===
        boolean noNewPartsNeeded = existingPhantoms.containsAll(expectedCurrentPositions);

        if (noNewPartsNeeded) {
            IS_REPAIRING.set(true);
            try {
                // а) Удаляем лишние старые фантомы, если структура стала меньше в размерах
                for (BlockPos oldPos : existingPhantoms) {
                    if (!expectedCurrentPositions.contains(oldPos) && !oldPos.equals(currentCtrlPos)) {
                        BlockEntity be = level.getBlockEntity(oldPos);
                        if (be instanceof IMultiblockPart part) {
                            PartRole role = part.getPartRole();
                            if (role.canReceiveEnergy() || role.canSendEnergy()) {
                                com.hbm_m.api.energy.EnergySubscriptions.unsubscribeAll(be);
                            }
                        }
                        // Используем флаг 3, так как мы гарантировали загруженность соседей
                        level.removeBlock(oldPos, false);
                    }
                }

                // б) Обновляем роли оставшихся фантомов на месте без изменения BlockState
                boolean rolesChanged = false;
                for (BlockPos pos : expectedCurrentPositions) {
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof IMultiblockPart part) {
                        BlockPos gridPos = expectedPosToGrid.get(pos);
                        PartRole expectedRole = resolvePartRole(gridPos, controller);
                        if (part.getPartRole() != expectedRole) {
                            rolesChanged = true;
                            part.setPartRole(expectedRole);

                            if (expectedRole == PartRole.LADDER) {
                                Set<Direction> localSides = ladderLocalDirections.get(gridPos);
                                EnumSet<Direction> worldSides = EnumSet.noneOf(Direction.class);
                                if (localSides != null && !localSides.isEmpty()) {
                                    for (Direction localDir : localSides) {
                                        worldSides.add(rotateLocalDirectionToWorld(localDir, facing));
                                    }
                                } else {
                                    worldSides.add(Direction.NORTH); worldSides.add(Direction.SOUTH);
                                    worldSides.add(Direction.EAST); worldSides.add(Direction.WEST);
                                }
                                part.setAllowedClimbSides(worldSides);
                            } else {
                                part.setAllowedClimbSides(Collections.emptySet());
                            }

                            if (expectedRole == PartRole.ENERGY_CONNECTOR || expectedRole == PartRole.UNIVERSAL_CONNECTOR) {
                                Set<Direction> localEnergy = energyLocalDirections.get(gridPos);
                                EnumSet<Direction> worldEnergy = EnumSet.noneOf(Direction.class);
                                if (localEnergy != null && !localEnergy.isEmpty()) {
                                    for (Direction localDir : localEnergy) {
                                        worldEnergy.add(rotateLocalDirectionToWorld(localDir, facing));
                                    }
                                } else {
                                    Collections.addAll(worldEnergy, Direction.values());
                                }
                                part.setAllowedEnergySides(worldEnergy);
                            } else {
                                part.setAllowedEnergySides(Collections.emptySet());
                            }

                            if (expectedRole == PartRole.FLUID_CONNECTOR || expectedRole == PartRole.UNIVERSAL_CONNECTOR) {
                                Set<Direction> localFluid = fluidLocalDirections.get(gridPos);
                                EnumSet<Direction> worldFluid = EnumSet.noneOf(Direction.class);
                                if (localFluid != null && !localFluid.isEmpty()) {
                                    for (Direction localDir : localFluid) {
                                        worldFluid.add(rotateLocalDirectionToWorld(localDir, facing));
                                    }
                                } else {
                                    Collections.addAll(worldFluid, Direction.values());
                                }
                                part.setAllowedFluidSides(worldFluid);
                            } else {
                                part.setAllowedFluidSides(Collections.emptySet());
                            }

                            be.setChanged();
                        }
                    }
                }

                applyControllerSideConfig(level, currentCtrlPos, facing);
                MultiblockFrameHelper.updateFrameForController(level, currentCtrlPos);

                if (rolesChanged) {
                    level.sendBlockUpdated(currentCtrlPos, state, state, 3);
                }

                return true;
            } finally {
                IS_REPAIRING.set(false);
            }
        }

        // 7. ЛОГИКА МИГРАЦИИ (СДВИГА) - выполняется только при физической перестройке
        if (oldOffset != null && !existingPhantoms.isEmpty()) {
            int legacyMatches = 0;
            int currentMatches = 0;
            for (BlockPos phantomPos : existingPhantoms) {
                if (expectedLegacyPositions.contains(phantomPos)) legacyMatches++;
                if (expectedCurrentPositions.contains(phantomPos)) currentMatches++;
            }
            
            if (legacyMatches > currentMatches) {
                BlockPos localShift = this.controllerOffset.subtract(oldOffset);
                BlockPos worldShift = rotate(localShift, facing);
                newCtrlPos = currentCtrlPos.offset(worldShift);
            }
        }

        // 8. ПРОВЕРКА КОЛЛИЗИЙ ДЛЯ ТЕХ МЕСТ, ГДЕ ПРЕПЯТСТВИЙ РАНЬШЕ НЕ БЫЛО
        Set<BlockPos> finalExpectedPositions = new java.util.HashSet<>();
        for (BlockPos gridPos : this.structureMap.keySet()) {
            finalExpectedPositions.add(this.getRotatedPos(newCtrlPos, gridPos, facing));
        }

        for (BlockPos targetPos : finalExpectedPositions) {
            if (targetPos.equals(newCtrlPos) || targetPos.equals(currentCtrlPos)) continue;
            if (!existingPhantoms.contains(targetPos)) {
                if (!isBlockReplaceable(level.getBlockState(targetPos))) {
                    return false; // Починка заблокирована сторонним твердым блоком, ждем
                }
            }
        }

        // 9. ВЫПОЛНЯЕМ ПОЛНУЮ ПОЧИНКУ (ФИЗИЧЕСКАЯ ПЕРЕСТРОЙКА БЛОКОВ)
        IS_REPAIRING.set(true);
        try {
            for (BlockPos oldPos : existingPhantoms) {
                if (!oldPos.equals(currentCtrlPos) && !oldPos.equals(newCtrlPos)) {
                    BlockEntity be = level.getBlockEntity(oldPos);
                    if (be instanceof IMultiblockPart part) {
                        PartRole role = part.getPartRole();
                        if (role.canReceiveEnergy() || role.canSendEnergy()) {
                            com.hbm_m.api.energy.EnergySubscriptions.unsubscribeAll(be);
                        }
                    }
                    level.removeBlock(oldPos, false);
                }
            }

            if (!newCtrlPos.equals(currentCtrlPos)) {
                BlockEntity oldBE = level.getBlockEntity(currentCtrlPos);
                net.minecraft.nbt.CompoundTag nbt = null;
                
                if (oldBE != null) {
                    //? if < 1.21.1 {
                    nbt = oldBE.saveWithFullMetadata();
                    //?} else {
                    /*// 1.21.1: saveWithFullMetadata требует HolderLookup.Provider.
                    nbt = oldBE.saveWithFullMetadata(level.registryAccess());
                    *///?}
                    level.removeBlockEntity(currentCtrlPos);
                }
                
                level.removeBlock(currentCtrlPos, false);
                level.setBlock(newCtrlPos, state, 3);

                if (nbt != null) {
                    nbt.putInt("x", newCtrlPos.getX());
                    nbt.putInt("y", newCtrlPos.getY());
                    nbt.putInt("z", newCtrlPos.getZ());
                    BlockEntity newBE = level.getBlockEntity(newCtrlPos);
                    if (newBE != null) {
                        //? if < 1.21.1 {
                        newBE.load(nbt);
                        //?} else {
                        /*// 1.21.1: BlockEntity.load(CompoundTag) удалён — loadCustomOnly с Provider.
                        newBE.loadCustomOnly(nbt, level.registryAccess());
                        *///?}
                    }
                }
                level.updateNeighborsAt(currentCtrlPos, state.getBlock());
                level.updateNeighborsAt(newCtrlPos, state.getBlock());
            } else {
                this.placeStructure(level, newCtrlPos, facing, controller);
            }
            
            return true;
        } finally {
            IS_REPAIRING.set(false);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    //  Восстановление после Create / Sable disassembly (перепривязка осиротевших частей)
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Самолечение осиротевшей части: если сохранённый {@code ControllerPos} не указывает на
     * живой контроллер (Create/Sable disassembly восстановил BE из старого NBT, не переписав
     * {@code ControllerPos}), часть сама ищет ближайший подходящий контроллер и перепривязывается.
     *
     * <p>Безопасна для вызова из server-tick части: сканирование ограничено
     * {@link #RELINK_SEARCH_RADIUS}, выполняется один раз (gating через {@code pendingRelinkCheck}
     * в {@code UniversalMachinePartBlockEntity}) и только когда ссылка невалидна. Не импортирует
     * loader-API — только vanilla {@link Level}/{@link BlockPos}/{@link BlockEntity}/{@link BlockState}
     * и собственные интерфейсы HBM.
     *
     * @param level   уровень (на клиенте — no-op)
     * @param partPos позиция осиротевшей части
     */
    public static void relinkOrphanedPart(Level level, BlockPos partPos) {
        if (level.isClientSide) return;
        BlockEntity be = level.getBlockEntity(partPos);
        if (!(be instanceof IMultiblockPart part)) return;

        // 1. Если текущая ссылка валидна — ничего не делаем (часть не осиротевшая).
        BlockPos stored = part.getControllerPos();
        if (stored != null) {
            BlockState storedState = level.getBlockState(stored);
            if (storedState.getBlock() instanceof IMultiblockController) {
                return;
            }
        }

        // 2. Ищем ВСЕ контроллеры, в чью схему попадает partPos.
        //    Раньше брали первого попавшегося — при двух одинаковых станках рядом
        //    часть могла привязаться к ЧУЖОМУ контроллеру просто потому, что он
        //    ближе в порядке обхода. Теперь собираем всех кандидатов и решаем.
        BlockPos partPosImmutable = partPos.immutable();
        BlockPos min = partPosImmutable.offset(-RELINK_SEARCH_RADIUS, -RELINK_SEARCH_RADIUS, -RELINK_SEARCH_RADIUS);
        BlockPos max = partPosImmutable.offset(RELINK_SEARCH_RADIUS, RELINK_SEARCH_RADIUS, RELINK_SEARCH_RADIUS);

        BlockPos bestCandidate = null;
        Direction bestFacing = null;
        IMultiblockController bestController = null;
        MultiblockStructureHelper bestHelper = null;
        BlockPos bestGridPos = null;
        int candidateCount = 0;

        for (BlockPos candidate : BlockPos.betweenClosed(min, max)) {
            BlockState candState = level.getBlockState(candidate);
            if (!(candState.getBlock() instanceof IMultiblockController controller)) continue;
            if (!candState.hasProperty(HorizontalDirectionalBlock.FACING)) continue;
            Direction facing = candState.getValue(HorizontalDirectionalBlock.FACING);
            MultiblockStructureHelper helper = controller.getStructureHelper();
            if (helper == null) continue;

            // 3. Принадлежит ли partPos схеме этого контроллера?
            //    Обратное вращение: gridPos = rotateBack(partPos - candidate, facing) + controllerOffset
            BlockPos worldOffset = partPosImmutable.subtract(candidate);
            BlockPos localOffset = rotateBack(worldOffset, facing);
            BlockPos gridPos = localOffset.offset(helper.getControllerOffset());
            if (!helper.getPartOffsets().contains(gridPos)) continue;

            candidateCount++;
            if (candidateCount == 1 || bestCandidate == null) {
                bestCandidate = candidate.immutable();
                bestFacing = facing;
                bestController = controller;
                bestHelper = helper;
                bestGridPos = gridPos;
            }
        }

        // 4. Решение:
        //    - ровно один кандидат → однозначная перепривязка;
        //    - несколько кандидатов → структура-схемы перекрываются, привязаться
        //      «просто к ближайшему» опасно: часть может уйти к чужому станку.
        //      Отказываемся от перепривязки (часть останется осиротевшей и будет
        //      ломаться в один клик — getDestroyProgress), чтобы не портить чужую машину.
        if (candidateCount == 1 && bestHelper != null) {
            bestHelper.doRelinkOrphanedPart(level, partPosImmutable, bestCandidate, bestFacing, bestController, bestGridPos);
        } else if (candidateCount > 1 && !level.isClientSide) {
            MainRegistry.LOGGER.warn(
                "[HBM] relinkOrphanedPart: {} кандидатов-контроллеров для части {} — привязка отклонена (неоднозначно)",
                candidateCount, partPosImmutable.toShortString());
        }
    }

    /**
     * Детерминированное самолечение осиротевшей части: если у части сохранён
     * {@link IMultiblockPart#getLocalOffsetFromController() localOffsetFromController},
     * вычисляем позицию контроллера по формуле
     * {@code controllerPos = partPos - rotate(localOffset, facing)}
     * без радиус-поиска. Это работает для любого размера мультиблока и не путается
     * между двумя соседними одинаковыми машинами.
     *
     * <p>Если вычисленная позиция не содержит валидный контроллер (контрапшен с
     * наклоном/креном, или структура повёрнута нестандартно), откатываемся на
     * {@link #relinkOrphanedPart} (радиус-поиск).
     *
     * <p>Безопасна для вызова из server-tick части: выполняется один раз (gating
     * через {@code pendingRelinkCheck} в {@code UniversalMachinePartBlockEntity}).
     *
     * @param level уровень (на клиенте — no-op)
     * @param partPos позиция осиротевшей части
     * @param part сама часть (для чтения localOffsetFromController)
     */
    public static void relinkOrphanedPartDeterministic(Level level, BlockPos partPos, IMultiblockPart part) {
        if (level.isClientSide) return;
        if (!(part instanceof BlockEntity be)) {
            // На всякий случай: если кто-то передал не-BE, fallback к радиус-поиску.
            relinkOrphanedPart(level, partPos);
            return;
        }
        BlockPos localOffset = part.getLocalOffsetFromController();
        if (localOffset == null) {
            // Старый NBT (без localOffset) — fallback к радиус-поиску.
            relinkOrphanedPart(level, partPos);
            return;
        }

        // partPosImmutable уже подразумевается — мы работаем с BlockPos из BE.
        BlockPos partPosImmutable = partPos.immutable();

        // 1) Берём facing из blockstate части (FACING = FACING структуры, обновляется
        //    при relink, но для только-что-загруженной части из contraption — это
        //    текущий FACING фантомного блока, который Create тоже поворачивал
        //    тем же Y-поворотом, что и позицию).
        BlockState partState = level.getBlockState(partPosImmutable);
        Direction facing = Direction.NORTH;
        if (partState.hasProperty(HorizontalDirectionalBlock.FACING)) {
            facing = partState.getValue(HorizontalDirectionalBlock.FACING);
        }

        // 2) Вычисляем предполагаемую позицию контроллера: worldPos - rotate(localOffset, facing).
        BlockPos rotatedOffset = rotate(localOffset, facing);
        BlockPos expectedController = partPosImmutable.subtract(rotatedOffset);

        // 3) Проверяем, что там действительно живой контроллер с подходящим helper'ом.
        BlockState ctrlState = level.getBlockState(expectedController);
        if (!(ctrlState.getBlock() instanceof IMultiblockController controller)) {
            // Контрапшен с pitch/roll (Aeronautics) — fallback к радиус-поиску.
            relinkOrphanedPart(level, partPos);
            return;
        }
        MultiblockStructureHelper helper = controller.getStructureHelper();
        if (helper == null) {
            relinkOrphanedPart(level, partPos);
            return;
        }

        // 4) Проверяем, что partPos действительно принадлежит схеме этого контроллера.
        //    Это страховка от ложного срабатывания (например, если два разных мультиблока
        //    используют один и тот же localOffset случайно).
        BlockPos worldOffset = partPosImmutable.subtract(expectedController);
        BlockPos backLocal = rotateBack(worldOffset, facing);
        BlockPos gridPos = backLocal.offset(helper.getControllerOffset());
        if (!helper.getPartOffsets().contains(gridPos)) {
            relinkOrphanedPart(level, partPos);
            return;
        }

        // 5) Всё ОК — перепривязываем.
        helper.doRelinkOrphanedPart(level, partPosImmutable, expectedController, facing, controller, gridPos);
    }

    /**
     * Сканирует футпринт структуры контроллера и перепривязывает части, у которых
     * {@code ControllerPos} стал невалидным (осиротели после Create/Sable disassembly).
     * Части, чья ссылка указывает на другой ЖИВОЙ контроллер, не трогаются (не крадём чужие).
     *
     * <p>Вызывается из {@link #attemptAutoRepair} перед «быстрой проверкой», чтобы осиротевшие
     * части попали в {@code existingPhantoms} и штатная починка прошла без физической перестройки.
     *
     * @return количество перепривязанных частей
     */
    private int reclaimOrphanedParts(Level level, BlockPos currentCtrlPos, Direction facing,
                                     IMultiblockController controller,
                                     Map<BlockPos, BlockPos> expectedPosToGrid,
                                     Set<BlockPos> existingPhantoms) {
        if (level.isClientSide) return 0;
        if (!(level instanceof net.minecraft.server.level.ServerLevel)) return 0;

        int reclaimed = 0;
        for (Map.Entry<BlockPos, BlockPos> e : expectedPosToGrid.entrySet()) {
            BlockPos expectedPos = e.getKey();
            if (existingPhantoms.contains(expectedPos)) continue; // уже привязана к нам

            BlockState partState = level.getBlockState(expectedPos);
            if (!(partState.getBlock() instanceof UniversalMachinePartBlock)) continue;

            BlockEntity be = level.getBlockEntity(expectedPos);
            if (!(be instanceof IMultiblockPart part)) continue;

            // Часть осиротела, если ControllerPos null ИЛИ указывает на позицию, где больше нет контроллера.
            BlockPos stored = part.getControllerPos();
            boolean stale = (stored == null);
            if (!stale) {
                BlockState storedState = level.getBlockState(stored);
                stale = !(storedState.getBlock() instanceof IMultiblockController);
            }
            if (!stale) continue; // принадлежит другому живому контроллеру — не крадём

            BlockPos gridPos = e.getValue();
            doRelinkOrphanedPart(level, expectedPos, currentCtrlPos, facing, controller, gridPos);
            existingPhantoms.add(expectedPos);
            reclaimed++;
        }
        return reclaimed;
    }

    /**
     * Перепривязка одной осиротевшей части мультиблока к (новому) контроллеру.
     * Логика роли/сторон — копия per-part блока из {@link #placeStructure}, применяемая
     * к уже существующему BE на месте (без {@code setBlock}), чтобы сохранить NBT части.
     *
     * <p>Мутации обёрнуты в ThreadLocal {@link #IS_REPAIRING}, чтобы
     * {@code UniversalMachinePartBlock#onRemove} (если сработает от neighbor-update при
     * смене роли) не запустил каскадное {@code destroyStructure} — как в {@link #attemptAutoRepair}.
     *
     * @param level         уровень (на клиенте / не ServerLevel — no-op)
     * @param partPos       позиция части (immutable)
     * @param controllerPos позиция нового контроллера (immutable)
     * @param facing        направление структуры
     * @param controller    сам контроллер
     * @param gridPos       позиция части в сетке хелпера (из {@link #getPartOffsets()})
     */
    private void doRelinkOrphanedPart(Level level, BlockPos partPos, BlockPos controllerPos,
                                      Direction facing, IMultiblockController controller, BlockPos gridPos) {
        if (level.isClientSide) return;
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;
        BlockEntity be = level.getBlockEntity(partPos);
        if (!(be instanceof IMultiblockPart part)) return;

        IS_REPAIRING.set(true);
        try {
            // 1. Привязываем часть к текущему контроллеру (setControllerPos сам сделает setChanged + sendBlockUpdated).
            part.setControllerPos(controllerPos);

            // 1b. Сохраняем локальный оффсет от контроллера (вращение-инвариантный).
            //     gridPos — позиция в сетке хелпера; controllerOffset — позиция контроллера в сетке.
            //     Relative offset = gridPos - controllerOffset (в локальных коорд., до поворота facing).
            //     Этот оффсет переживает contraption disassembly: Create вращает и worldPos,
            //     и FACING одним и тем же Y-поворотом, поэтому формула
            //     controllerPos = partWorldPos - rotate(localOffset, facing) остаётся верной.
            part.setLocalOffsetFromController(gridPos.subtract(this.controllerOffset));

            // 2. Пересчитываем роль — копия placeStructure (включая переопределение ролей дверей из DoorDecl).
            PartRole role = resolvePartRole(gridPos, controller);
            if (controller instanceof com.hbm_m.block.decorations.DoorBlock doorBlock) {
                String declId = doorBlock.getDoorDeclId();
                com.hbm_m.block.entity.doors.DoorDecl decl =
                        com.hbm_m.block.entity.doors.DoorDeclRegistry.getById(declId);
                if (decl != null && decl.getStructureDefinition() != null) {
                    PartRole definedRole = decl.getStructureDefinition().getRoles().get(gridPos);
                    if (definedRole != null) {
                        role = definedRole;
                    }
                }
            }
            part.setPartRole(role);

            // 3. Стороны лестницы (LADDER) — из ladderLocalDirections или все горизонтальные.
            if (role == PartRole.LADDER) {
                Set<Direction> localSides = ladderLocalDirections.get(gridPos);
                EnumSet<Direction> worldSides = EnumSet.noneOf(Direction.class);
                if (localSides != null && !localSides.isEmpty()) {
                    for (Direction localDir : localSides) {
                        worldSides.add(rotateLocalDirectionToWorld(localDir, facing));
                    }
                } else {
                    worldSides.add(Direction.NORTH);
                    worldSides.add(Direction.SOUTH);
                    worldSides.add(Direction.EAST);
                    worldSides.add(Direction.WEST);
                }
                part.setAllowedClimbSides(worldSides);
            } else {
                part.setAllowedClimbSides(Collections.emptySet());
            }

            // 4. Стороны энергии (регистрация узлов теперь через EnergySubscriptions на тике части).
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
                part.setAllowedEnergySides(worldEnergy);
            } else {
                part.setAllowedEnergySides(Collections.emptySet());
            }

            // 5. Стороны жидкостей (FLUID_CONNECTOR / UNIVERSAL_CONNECTOR).
            if (role == PartRole.FLUID_CONNECTOR || role == PartRole.UNIVERSAL_CONNECTOR) {
                Set<Direction> localFluid = fluidLocalDirections.get(gridPos);
                EnumSet<Direction> worldFluid = EnumSet.noneOf(Direction.class);
                if (localFluid != null && !localFluid.isEmpty()) {
                    for (Direction localDir : localFluid) {
                        worldFluid.add(rotateLocalDirectionToWorld(localDir, facing));
                    }
                } else {
                    Collections.addAll(worldFluid, Direction.values());
                }
                part.setAllowedFluidSides(worldFluid);
            } else {
                part.setAllowedFluidSides(Collections.emptySet());
            }

            be.setChanged();
            level.sendBlockUpdated(partPos, level.getBlockState(partPos), level.getBlockState(partPos), Block.UPDATE_CLIENTS);
        } finally {
            IS_REPAIRING.set(false);
        }
    }

    private static final Direction[] LADDER_TUPLE_ORDER = {
        Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    private static final Direction[] ENERGY_TUPLE_ORDER = {
        Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP, Direction.DOWN
    };

    private static final Direction[] FLUID_TUPLE_ORDER = {
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

    private static EnumSet<Direction> fluidTupleToLocalSet(char symbol, boolean[] tuple) {
        if (tuple == null || tuple.length != FLUID_TUPLE_ORDER.length) {
            throw new IllegalArgumentException("fluidSideMap['" + symbol + "'] must be boolean[6] (north,south,west,east,up,down)");
        }
        EnumSet<Direction> set = EnumSet.noneOf(Direction.class);
        for (int i = 0; i < FLUID_TUPLE_ORDER.length; i++) {
            if (tuple[i]) {
                set.add(FLUID_TUPLE_ORDER[i]);
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
        return createFromLayersWithRoles(layers, symbolMap, phantomBlockState, roleMap, shapeMap, collisionMap, null, null, null);
    }

    /**
     * Как полный {@link #createFromLayersWithRoles(String[][], Map, Supplier, Map, Map, Map, Map, Map)},
     * но без {@code shapeMap}/{@code collisionMap} (они {@code null}).
     * Отдельное имя - из‑за стирания типов нельзя перегрузить по {@code Map<Character, VoxelShape>} vs {@code Map<Character, boolean[]>}.
     * Для tuple сторон см. {@link MultiblockSideTuples}.
     */
    public static MultiblockStructureHelper createFromLayersWithRolesAndSides(
            String[][] layers,
            Map<Character, Supplier<BlockState>> symbolMap,
            Supplier<BlockState> phantomBlockState,
            Map<Character, PartRole> roleMap,
            Map<Character, boolean[]> ladderSideMap,
            Map<Character, boolean[]> energySideMap) {
        return createFromLayersWithRoles(layers, symbolMap, phantomBlockState, roleMap, null, null, ladderSideMap, energySideMap, null);
    }

    /**
     * Как {@link #createFromLayersWithRolesAndSides(String[][], Map, Supplier, Map, Map, Map)},
     * но дополнительно задаёт стороны жидкостного подключения для FLUID_CONNECTOR / UNIVERSAL_CONNECTOR
     * и (опционально) для самого контроллера (если добавить символ контроллера в карту).
     */
    public static MultiblockStructureHelper createFromLayersWithRolesAndSides(
            String[][] layers,
            Map<Character, Supplier<BlockState>> symbolMap,
            Supplier<BlockState> phantomBlockState,
            Map<Character, PartRole> roleMap,
            Map<Character, boolean[]> ladderSideMap,
            Map<Character, boolean[]> energySideMap,
            Map<Character, boolean[]> fluidSideMap) {
        return createFromLayersWithRoles(layers, symbolMap, phantomBlockState, roleMap, null, null, ladderSideMap, energySideMap, fluidSideMap);
    }

    public static MultiblockStructureHelper createFromLayersWithRoles(
            String[][] layers,
            Map<Character, Supplier<BlockState>> symbolMap,
            Supplier<BlockState> phantomBlockState,
            Map<Character, PartRole> roleMap) {
        return createFromLayersWithRoles(layers, symbolMap, phantomBlockState, roleMap, null, null, null, null, null);
    }
    
    /**
     * Получает позицию контроллера в структуре (относительные координаты).
     * 
     * @return BlockPos позиции контроллера относительно центра структуры
     */
    public BlockPos getControllerOffset() {
        return this.controllerOffset;
    }

    public int getPlacementOffset() {
        return this.placementOffset;
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
        // По умолчанию - полный блок
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
        // Ванильная replaceable-проверка: воздух, трава, снег, жидкости и наши
        // блоки-газы (BlockGasBase объявлен .replaceable()). В 1.7.10 газ в
        // оригинале isReplaceable=true и установка мультиблока его просто
        // затирает (fillSpace).
        if (state.canBeReplaced()) {
            return true;
        }
        if (state.getBlock() instanceof com.hbm_m.block.gas.BlockGasBase) {
            return true;
        }

        // Теперь проверяем по тегам. Это включает все цветы, траву, саженцы и т.д.
        return state.is(BlockTags.REPLACEABLE_BY_TREES) || // Трава, папоротники
               state.is(BlockTags.FLOWERS) ||            // Все виды цветов
               state.is(BlockTags.SAPLINGS);             // Саженцы деревьев
    }

    /**
     * Проверяет, свободны ли все клетки структуры.
     *
     * @param controllerPos мировая позиция <b>ядра</b> (контроллера), не клетки клика;
     *                      для facade используйте {@link #checkPlacementFromFacade}.
     */
    public boolean checkPlacement(Level level, BlockPos controllerPos, Direction facing, Player player) {
        List<BlockPos> obstructions = new ArrayList<>();
        for (BlockPos relativePos : structureMap.keySet()) {
            BlockPos worldPos = getRotatedPos(controllerPos, relativePos, facing);
            // Structure definitions may legitimately include a part at local (0,0,0)
            // when the controller itself is offset within the pattern. Only skip the
            // controller's own world position, mirroring placeStructure().
            BlockState existingState = level.getBlockState(worldPos);

            if (worldPos.equals(controllerPos)) {
                // Клетка ядра тоже участвует в проверке: если контроллер встал в занятый
                // блок (отказ вместо сдвига — см. CorePlaceContext), подсвечиваем и его.
                if (!isBlockReplaceable(existingState)) {
                    obstructions.add(worldPos);
                }
                continue;
            }

            // Используем наш новый метод для проверки
            if (!isBlockReplaceable(existingState)) {
                obstructions.add(worldPos);
            }
        }

        if (!obstructions.isEmpty()) {
            if (player instanceof ServerPlayer serverPlayer) {
                // Проверяем, включена ли опция в конфиге, перед отправкой пакета
                if (ModClothConfig.get().obstructionHighlight.enableObstructionHighlight) {
                    HighlightBlocksPacket.sendTo(serverPlayer, obstructions);
                }
            }
            player.displayClientMessage(Component.translatable("chat.hbm_m.structure.obstructed"), true);
            return false;
        }

        return true;
    }

    /** Проверка от клетки клика (facade): сама вычисляет позицию ядра. */
    public boolean checkPlacementFromFacade(Level level, BlockPos facadePos, Direction facing, Player player) {
        return checkPlacement(level, MultiblockPlacement.getCorePos(facadePos, facing, this), facing, player);
    }

    /**
     * Тихая проверка размещения без сообщений игроку и пакетов подсветки.
     * Для клиентского предпросмотра рамки при установке (вызывается каждый кадр).
     */
    public boolean checkPlacementFromFacadeQuiet(Level level, BlockPos facadePos, Direction facing, Block controllerBlock) {
        BlockPos controllerPos = MultiblockPlacement.getCorePos(facadePos, facing, this, controllerBlock);
        for (BlockPos relativePos : structureMap.keySet()) {
            BlockPos worldPos = getRotatedPos(controllerPos, relativePos, facing);
            if (worldPos.equals(controllerPos)) {
                // Клетка ядра: занята твёрдым блоком — предпросмотр красный (согласовано
                // с отказом в MultiblockBlockItem.placeBlock).
                if (!isBlockReplaceable(level.getBlockState(worldPos))) return false;
                continue;
            }
            if (!isBlockReplaceable(level.getBlockState(worldPos))) return false;
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

        // Идёт перенос блоков движком сборки (Create/Sable): контроллер сейчас ставится
        // самим движком (setBlock -> onPlace), а части приходят из NBT контрапшена.
        // Перестройка фантомов в этот момент кладёт их поверх ещё не расставленных
        // клеток, сдвигает структуру и плодит контроллеры-дубликаты («зомби-станки»).
        // После закрытия окна осиротевшие части перепривяжутся через relink.
        if (ContraptionAssemblyGuard.isMoving()) {
            MainRegistry.LOGGER.info(
                "[HBM] placeStructure подавлен (окно сборки контрапшена), контроллер {}",
                controllerPos.toShortString());
            return;
        }

        List<BlockPos> energyConnectorPositions = new ArrayList<>();
        List<BlockPos> allPlacedPositions = new ArrayList<>();

        markRecentlyPlaced(level, controllerPos);

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
                // Локальный оффсет от контроллера в сетке структуры (вращение-инвариантный).
                // См. javadoc у IMultiblockPart.getLocalOffsetFromController().
                partBe.setLocalOffsetFromController(gridPos.subtract(this.controllerOffset));
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
                        // Нет записи в ladderSideMap - все горизонтальные стороны
                        worldSides.add(Direction.NORTH);
                        worldSides.add(Direction.SOUTH);
                        worldSides.add(Direction.EAST);
                        worldSides.add(Direction.WEST);
                    }
                    partBe.setAllowedClimbSides(worldSides);
                }
                
                // ENERGY_CONNECTOR и UNIVERSAL_CONNECTOR - стороны из energySideMap или все шесть
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

                // FLUID_CONNECTOR и UNIVERSAL_CONNECTOR - стороны из fluidSideMap или все шесть
                if (role == PartRole.FLUID_CONNECTOR || role == PartRole.UNIVERSAL_CONNECTOR) {
                    Set<Direction> localFluid = fluidLocalDirections.get(gridPos);
                    EnumSet<Direction> worldFluid = EnumSet.noneOf(Direction.class);
                    if (localFluid != null && !localFluid.isEmpty()) {
                        for (Direction localDir : localFluid) {
                            worldFluid.add(rotateLocalDirectionToWorld(localDir, facing));
                        }
                    } else {
                        Collections.addAll(worldFluid, Direction.values());
                    }
                    partBe.setAllowedFluidSides(worldFluid);
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
        
        // ОДНО сообщение со всеми координатами (ник ближайшего игрока, если удалось определить)
        String placerName = "unknown";
        Player placer = level.getNearestPlayer(
            controllerPos.getX() + 0.5, controllerPos.getY() + 0.5, controllerPos.getZ() + 0.5,
            8.0, false);
        if (placer != null) {
            placerName = placer.getGameProfile().getName();
        }
        MainRegistry.LOGGER.info("Player {} placed multiblock {} at {} with {} parts: {}", 
            placerName, controller, controllerPos, allPlacedPositions.size(), formatPositions(allPlacedPositions));

        // Настраиваем стороны подключения на самом контроллере, если он поддерживает sided IO.
        applyControllerSideConfig(level, controllerPos, facing);
        
        MultiblockFrameHelper.updateFrameForController(level, controllerPos);
        
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

    private void applyControllerSideConfig(Level level, BlockPos controllerPos, Direction facing) {
        BlockEntity be = level.getBlockEntity(controllerPos);
        if (!(be instanceof IMultiblockSidedIO sided)) {
            return;
        }

        // По умолчанию (пусто) - все стороны.
        EnumSet<Direction> worldEnergy = EnumSet.noneOf(Direction.class);
        if (controllerEnergyLocalDirections != null && !controllerEnergyLocalDirections.isEmpty()) {
            for (Direction localDir : controllerEnergyLocalDirections) {
                worldEnergy.add(rotateLocalDirectionToWorld(localDir, facing));
            }
            sided.setAllowedEnergySides(worldEnergy);
        }

        EnumSet<Direction> worldFluid = EnumSet.noneOf(Direction.class);
        if (controllerFluidSidesFromStructure) {
            for (Direction localDir : controllerFluidLocalDirections) {
                worldFluid.add(rotateLocalDirectionToWorld(localDir, facing));
            }
            sided.setAllowedFluidSidesFromMultiblockStructure(worldFluid);
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
        // Перенос блоков движком сборки (Create/Sable): разрушать структуру нельзя -
        // движок уже сохранил state+NBT и вернёт её на месте. Разрушение здесь = дюп.
        if (ContraptionAssemblyGuard.isMoving()) {
            MainRegistry.LOGGER.info(
                "[HBM] destroyStructure подавлен (окно сборки контрапшена), контроллер {}",
                controllerPos.toShortString());
            return;
        }
        IS_DESTROYING.set(true);
        try {
            for (BlockPos gridPos : structureMap.keySet()) {
                BlockPos worldPos = getRotatedPos(controllerPos, gridPos, facing);
                if (level.getBlockState(worldPos).getBlock() instanceof UniversalMachinePartBlock) {
                    BlockEntity be = level.getBlockEntity(worldPos);
                    if (be instanceof IMultiblockPart part) {
                        PartRole role = part.getPartRole();
                        if (role.canReceiveEnergy() || role.canSendEnergy()) {
                            com.hbm_m.api.energy.EnergySubscriptions.unsubscribeAll(be);
                        }
                    }
                    level.setBlock(worldPos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        } finally { IS_DESTROYING.set(false); }
    }

    public Map<BlockPos, Supplier<BlockState>> getStructureMap() {
        return structureMap;
    }

    /** @see MultiblockFrameHelper#isTopRingPart */
    public boolean isTopRingPart(BlockPos localOffset) {
        return MultiblockFrameHelper.isTopRingPart(this, localOffset);
    }

    /** @see MultiblockFrameHelper#getTopRingWorldPositions */
    public List<BlockPos> getTopRingWorldPositions(BlockPos controllerPos, Direction facing) {
        return MultiblockFrameHelper.getTopRingWorldPositions(this, controllerPos, facing);
    }

    /** @see MultiblockFrameHelper#computeFrameVisible */
    public boolean computeFrameVisible(Level level, BlockPos controllerPos, Direction facing) {
        return MultiblockFrameHelper.computeFrameVisible(level, this, controllerPos, facing);
    }

    /** @see MultiblockFrameHelper#updateFrameForController */
    public static void updateFrameForController(Level level, BlockPos controllerPos) {
        MultiblockFrameHelper.updateFrameForController(level, controllerPos);
    }

    /** @see MultiblockFrameHelper#onNeighborChangedForPart */
    public static void onNeighborChangedForPart(Level level, BlockPos partPos, BlockPos changedPos) {
        MultiblockFrameHelper.onNeighborChangedForPart(level, partPos, changedPos);
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
    /**
     * Returns the bounding AABB enclosing the entire multiblock structure in
     * controller-local coordinates (i.e. with the controller treated as origin),
     * with the given facing rotation pre-applied. Each entry is computed once
     * per direction and reused for every BlockEntity that shares this helper -
     * the result depends only on {@link #structureMap} + {@link #controllerOffset}
     * + facing, none of which vary per-BE.
     * <p>
     * Coords are inclusive on the lower edge and exclusive on the upper edge,
     * matching {@link AABB} convention - a single 1×1×1 block at the controller
     * with no other parts produces {@code AABB(0, 0, 0, 1, 1, 1)}.
     * <p>
     * Used by {@link BlockEntity#getRenderBoundingBox()} overrides so the
     * vanilla / Forge frustum + occlusion culler only skips a multiblock when
     * its FULL footprint is offscreen, instead of skipping based on the
     * controller's 1×1×1 cell while the rest of the structure is still in
     * view (which produced the "machine pops out when controller goes
     * offscreen" symptom).
     */
    public AABB getLocalRenderBoundingBox(Direction facing) {
        AABB cached = localRenderBoundsCache.get(facing);
        if (cached != null) {
            return cached;
        }
        // Always include the controller cell itself (offset 0,0,0 after the
        // controllerOffset normalisation). Seed bounds at that cell so an
        // empty structureMap still yields a valid 1×1×1 box.
        int minX = 0, minY = 0, minZ = 0;
        int maxX = 0, maxY = 0, maxZ = 0;
        for (BlockPos gridPos : structureMap.keySet()) {
            BlockPos relative = gridPos.subtract(this.controllerOffset);
            BlockPos rotated = rotate(relative, facing);
            int x = rotated.getX();
            int y = rotated.getY();
            int z = rotated.getZ();
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
            if (z < minZ) minZ = z;
            if (z > maxZ) maxZ = z;
        }
        AABB result = new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
        localRenderBoundsCache.put(facing, result);
        return result;
    }

    /**
     * World-space convenience wrapper around {@link #getLocalRenderBoundingBox}:
     * translates the cached local AABB onto the controller's actual world
     * position and inflates it by {@code inflate} blocks on every axis - useful
     * for renderers whose animated parts (rotating arms, sliding heads,
     * particle plumes) reach beyond the static structure's footprint.
     *
     * @param controllerWorldPos the world position of the multiblock controller
     * @param facing             the controller's FACING property
     * @param inflate            extra padding in blocks on every face (0 to disable)
     */
    public AABB getRenderBoundingBox(BlockPos controllerWorldPos, Direction facing, double inflate) {
        AABB local = getLocalRenderBoundingBox(facing);
        AABB moved = local.move(controllerWorldPos.getX(), controllerWorldPos.getY(), controllerWorldPos.getZ());
        return inflate > 0.0 ? moved.inflate(inflate) : moved;
    }

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
