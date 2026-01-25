package com.hbm_m.multiblock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

// Утилитарный класс для управления мультиблочными структурами.
// Позволяет определять структуру, проверять возможность постройки, строить и разрушать структуру,
// а также генерировать VoxelShape для всей структуры. Ядро всей мультиблочной логики.
import com.hbm_m.api.energy.WireBlock;
import com.hbm_m.block.custom.decorations.DoorBlock;
import com.hbm_m.block.custom.machines.UniversalMachinePartBlock;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.PacketDistributor;

public class MultiblockStructureHelper {

    private final Map<BlockPos, Supplier<BlockState>> structureMap;
    private final Supplier<BlockState> phantomBlockState;

    private final Set<BlockPos> partOffsets;
    private final int maxY;

    public MultiblockStructureHelper(Map<BlockPos, Supplier<BlockState>> structureMap, 
                                   Supplier<BlockState> phantomBlockState) {
        // Используем unmodifiableMap вместо copyOf для экономии памяти
        this.structureMap = Collections.unmodifiableMap(structureMap);
        this.phantomBlockState = phantomBlockState;
        
        // Предвычисляем значения
        this.partOffsets = Collections.unmodifiableSet(structureMap.keySet());
        this.maxY = computeMaxY();
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

    private int computeMaxY() {
        return structureMap.keySet().stream()
            .mapToInt(BlockPos::getY)
            .max()
            .orElse(0);
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
        List<BlockPos> allPlacedPositions = new ArrayList<>();  // Собираем все координаты
        
        for (Map.Entry<BlockPos, Supplier<BlockState>> entry : structureMap.entrySet()) {
            BlockPos relativePos = entry.getKey();
            if (relativePos.equals(BlockPos.ZERO)) continue;
            
            BlockPos worldPos = getRotatedPos(controllerPos, relativePos, facing);
            BlockState partState = phantomBlockState.get().setValue(HorizontalDirectionalBlock.FACING, facing);
            
            // Ставим блок с флагом 2 (не отправлять обновления соседям)
            level.setBlock(worldPos, partState, 2);
            
            // Добавляем в список для логирования
            allPlacedPositions.add(worldPos);
            
            BlockEntity be = level.getBlockEntity(worldPos);
            if (be instanceof IMultiblockPart partBe) {
                partBe.setControllerPos(controllerPos);
                PartRole role = controller.getPartRole(relativePos);
                partBe.setPartRole(role);
                
                if (role == PartRole.ENERGY_CONNECTOR) {
                    energyConnectorPositions.add(worldPos);
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
        if (level.isClientSide) return;

        // Destroy the controller itself if it still exists
        if (level.getBlockState(controllerPos).getBlock() instanceof IMultiblockController) {
            level.setBlock(controllerPos, Blocks.AIR.defaultBlockState(), 3);
        }

        // Iterate through all parts of the structure
        for (BlockPos relativePos : structureMap.keySet()) {
            if (relativePos.equals(BlockPos.ZERO)) continue; // Skip controller position

            BlockPos worldPos = getRotatedPos(controllerPos, relativePos, facing);
            BlockState stateInWorld = level.getBlockState(worldPos);

            // If the block in the world is a phantom block part, remove it.
            if (stateInWorld.getBlock() instanceof UniversalMachinePartBlock) {
                level.setBlock(worldPos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
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

        // Применяем изменения через интерфейс BlockEntity
        frameSupportable.setFrameVisible(visible);
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
    private static BlockPos rotateBack(BlockPos pos, Direction facing) {
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
        VoxelShape finalShape = Shapes.empty();

        // Include the controller in the shape
        finalShape = Shapes.or(finalShape, Block.box(0, 0, 0, 16, 16, 16));

        for (BlockPos localOffset : structureMap.keySet()) {
            if (localOffset.equals(BlockPos.ZERO)) continue;

            BlockPos rotatedOffset = rotate(localOffset, facing);

            // Create a VoxelShape for a single block and move it
            VoxelShape partShape = Block.box(0, 0, 0, 16, 16, 16)
                    .move(rotatedOffset.getX(), rotatedOffset.getY(), rotatedOffset.getZ());

            // Combine it with the main shape
            finalShape = Shapes.or(finalShape, partShape);
        }

        return finalShape.optimize();
    }

    /**
     * Валидация параметров в getDoorPartAABBs()
     * Получает базовые AABB частей двери из реестра с проверкой на null и Infinity
     */
    public static Map<String, AABB> getDoorPartAABBs(String doorId) {
        Map<String, AABB> result = DoorPartAABBRegistry.getAll(doorId);

        // Проверка на null
        if (result == null) {
            MainRegistry.LOGGER.error(
                    "DoorPartAABBRegistry returned null for doorId: '{}'",
                    doorId
            );
            return Collections.emptyMap();
        }

        // Валидация каждого AABB в реестре
        Map<String, AABB> validatedResult = new HashMap<>(result);

        for (Map.Entry<String, AABB> entry : validatedResult.entrySet()) {
            AABB aabb = entry.getValue();

            if (aabb == null) {
                MainRegistry.LOGGER.warn(
                        "DoorPartAABBRegistry contains null AABB for part '{}' in door '{}'",
                        entry.getKey(), doorId
                );
                validatedResult.put(entry.getKey(), new AABB(0, 0, 0, 1, 1, 1));
                continue;
            }

            // Проверка на Infinity и NaN
            if (!isValidAABB(aabb, "getDoorPartAABBs")) {
                MainRegistry.LOGGER.error(
                        "DoorPartAABBRegistry contains invalid AABB for part '{}' in door '{}': {}",
                        entry.getKey(), doorId, formatAABB(aabb)
                );
                validatedResult.put(entry.getKey(), new AABB(0, 0, 0, 1, 1, 1));
            }
        }

        return validatedResult;
    }

    /**
     * Генерирует VoxelShape для двери на основе зарегистрированных AABB частей.
     * @param doorId ID двери
     * @param visibleParts Список имён видимых частей
     * @param facing Направление двери
     * @return Объединённая VoxelShape или пустая если нет данных
     */
    public static VoxelShape generateShapeFromDoorParts(String doorId,
                                                         java.util.List<String> visibleParts,
                                                         Direction facing) {
        Map<String, AABB> allAABBs = getDoorPartAABBs(doorId);
        if (allAABBs.isEmpty()) return Shapes.empty();

        // Получаем размеры с валидацией
        int[] dims = DoorBlock.getDoorDimensions(doorId);

        // Проверка на null и длину массива
        if (dims == null || dims.length < 6) {
            MainRegistry.LOGGER.error(
                    "Invalid door dimensions for doorId '{}': dims is null or too short (length={})",
                    doorId, dims != null ? dims.length : -1
            );
            return Shapes.empty();
        }

        // Извлечение параметров с проверкой на NaN/Infinity
        double widthBlocks = dims[3] + 1.0;
        double heightBlocks = dims[4] + 1.0;
        double depthBlocks = dims[5] + 1.0;
        double offsetX = dims[0];
        double offsetY = dims[1];
        double offsetZ = dims[2];

        // Полная валидация всех параметров
        if (!isValidDoorDimension(widthBlocks, "widthBlocks", doorId) ||
            !isValidDoorDimension(heightBlocks, "heightBlocks", doorId) ||
            !isValidDoorDimension(depthBlocks, "depthBlocks", doorId) ||
            !isValidDoorDimension(offsetX, "offsetX", doorId) ||
            !isValidDoorDimension(offsetY, "offsetY", doorId) ||
            !isValidDoorDimension(offsetZ, "offsetZ", doorId)) {
            return Shapes.empty();
        }

        // Гарантировать минимальные размеры (избежать нулевых значений)
        widthBlocks = Math.max(widthBlocks, 0.01);
        heightBlocks = Math.max(heightBlocks, 0.01);
        depthBlocks = Math.max(depthBlocks, 0.01);

        VoxelShape finalShape = Shapes.empty();

        for (String partName : visibleParts) {
            AABB raw = allAABBs.get(partName);
            if (raw == null) continue;

            // Проверка на Infinity перед масштабированием
            if (!isValidAABB(raw, "generateShapeFromDoorParts")) {
                MainRegistry.LOGGER.warn(
                        "Skipping invalid AABB for part '{}' in door '{}'",
                        partName, doorId
                );
                continue;
            }

            boolean needsRotation = needsModelRotation(doorId);
            AABB scaled;

            if (needsRotation) {
                scaled = new AABB(
                        raw.minY * widthBlocks + offsetX,
                        raw.minX * heightBlocks + offsetY,
                        raw.minZ * depthBlocks + offsetZ,
                        raw.maxY * widthBlocks + offsetX,
                        raw.maxX * heightBlocks + offsetY,
                        raw.maxZ * depthBlocks + offsetZ
                );
            } else {
                scaled = new AABB(
                        raw.minX * widthBlocks + offsetX,
                        raw.minY * heightBlocks + offsetY,
                        raw.minZ * depthBlocks + offsetZ,
                        raw.maxX * widthBlocks + offsetX,
                        raw.maxY * heightBlocks + offsetY,
                        raw.maxZ * depthBlocks + offsetZ
                );
            }

            // Проверка результата масштабирования
            if (!isValidAABB(scaled, "generateShapeFromDoorParts (after scaling)")) {
                MainRegistry.LOGGER.error(
                        "Scaled AABB is invalid for part '{}' in door '{}': {}",
                        partName, doorId, formatAABB(scaled)
                );
                continue;
            }

            AABB rotated = rotateAABBByFacing(scaled, facing);

            VoxelShape partShape = Block.box(
                    rotated.minX * 16.0, rotated.minY * 16.0, rotated.minZ * 16.0,
                    rotated.maxX * 16.0, rotated.maxY * 16.0, rotated.maxZ * 16.0
            );

            finalShape = Shapes.or(finalShape, partShape);
        }

        return finalShape.optimize();
    }

    // Вспомогательный метод #1: Проверка размеров на корректность
    private static boolean isValidDoorDimension(double value, String name, String doorId) {
        if (Double.isNaN(value)) {
            MainRegistry.LOGGER.error(
                    "Door dimension '{}' is NaN for doorId '{}'",
                    name, doorId
            );
            return false;
        }

        if (Double.isInfinite(value)) {
            MainRegistry.LOGGER.error(
                    "Door dimension '{}' is Infinity for doorId '{}': value={}",
                    name, doorId, value
            );
            return false;
        }

        return true;
    }

    // Вспомогательный метод #2: Проверка AABB на валидность
    private static boolean isValidAABB(AABB aabb, String caller) {
        if (aabb == null) {
            MainRegistry.LOGGER.warn("{}: AABB is null", caller);
            return false;
        }

        double[] coords = {
                aabb.minX, aabb.minY, aabb.minZ,
                aabb.maxX, aabb.maxY, aabb.maxZ
        };

        for (int i = 0; i < coords.length; i++) {
            if (Double.isNaN(coords[i])) {
                MainRegistry.LOGGER.error(
                        "{}: AABB coordinate at index {} is NaN",
                        caller, i
                );
                return false;
            }

            if (Double.isInfinite(coords[i])) {
                MainRegistry.LOGGER.error(
                        "{}: AABB coordinate at index {} is Infinity: {}",
                        caller, i, coords[i]
                );
                return false;
            }
        }

        return true;
    }


    //  Вспомогательный метод #3: Форматирование AABB для логирования

    private static String formatAABB(AABB aabb) {
        if (aabb == null) return "null";
        return String.format(
                "AABB[%.3f, %.3f, %.3f - %.3f, %.3f, %.3f]",
                aabb.minX, aabb.minY, aabb.minZ,
                aabb.maxX, aabb.maxY, aabb.maxZ
        );
    }

    /**
     * Определяет, нужно ли поворачивать OBJ модель на 90° для соответствия мультиблоку.
     * Горизонтальные двери (large_vehicle_door и т.д.) требуют поворота.
     */
    private static boolean needsModelRotation(String doorId) {
        return switch (doorId) {
            case "large_vehicle_door", 
                "sliding_blast_door",
                "secure_access_door",
                "transition_seal",
                "round_airlock_door",
                "fire_door" -> true; // Горизонтальные двери
            default -> false; // Вертикальные двери
        };
    }

    /**
     * Валидация в rotateAABBByFacing()
     * Поворачивает AABB с проверкой на null и Infinity координаты
     */
    public static AABB rotateAABBByFacing(AABB aabb, Direction facing) {
        // Валидация входного AABB
        if (aabb == null) {
            MainRegistry.LOGGER.warn(
                    "Attempting to rotate null AABB in rotateAABBByFacing, returning default block bounds"
            );
            return new AABB(0, 0, 0, 1, 1, 1);
        }

        // Проверка на Infinity и NaN перед инверсией
        if (!isValidAABB(aabb, "rotateAABBByFacing")) {
            MainRegistry.LOGGER.warn(
                    "Cannot rotate invalid AABB in rotateAABBByFacing: {}, returning default block bounds",
                    formatAABB(aabb)
            );
            return new AABB(0, 0, 0, 1, 1, 1);
        }

        return switch (facing) {
            case SOUTH -> new AABB(-aabb.maxX, aabb.minY, -aabb.maxZ, -aabb.minX, aabb.maxY, -aabb.minZ);
            case WEST -> new AABB(-aabb.maxZ, aabb.minY, aabb.minX, -aabb.minZ, aabb.maxY, aabb.maxX);
            case EAST -> new AABB(aabb.minZ, aabb.minY, -aabb.maxX, aabb.maxZ, aabb.maxY, -aabb.minX);
            default -> aabb;  // NORTH
        };
    }


    public BlockPos getRotatedPos(BlockPos controllerPos, BlockPos localOffset, Direction facing) {
        return controllerPos.offset(rotate(localOffset, facing));
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
