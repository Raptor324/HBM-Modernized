package com.hbm_m.multiblock;

// Утилитарный класс для управления мультиблочными структурами.
// Позволяет определять структуру, проверять возможность постройки, строить и разрушать структуру,
// а также генерировать VoxelShape для всей структуры. Ядро всей мультиблочной логики.
import com.hbm_m.api.energy.WireBlock;
import com.hbm_m.block.custom.machines.UniversalMachinePartBlock;
import com.hbm_m.block.custom.decorations.DoorBlock;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

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
                if (ModClothConfig.get().enableObstructionHighlight) {
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
     * Получает базовые AABB частей двери из реестра (автоматически извлечены из OBJ).
     * @param doorId ID двери (например, "qe_sliding_door")
     * @return Карта partName -> AABB или пустая если не зарегистрирована
     */
    public static Map<String, AABB> getDoorPartAABBs(String doorId) {
        return DoorPartAABBRegistry.getAll(doorId);
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

        // Получаем размеры мультиблока
        int[] dims = DoorBlock.getDoorDimensions(doorId);
        double widthBlocks = dims[3] + 1.0;   // X
        double heightBlocks = dims[4] + 1.0;  // Y
        double depthBlocks = dims[5] + 1.0;   // Z
        double offsetX = dims[0];
        double offsetY = dims[1];
        double offsetZ = dims[2];

        VoxelShape finalShape = Shapes.empty();
        for (String partName : visibleParts) {
            AABB raw = allAABBs.get(partName);
            if (raw == null) continue;

            // ИСПРАВЛЕНИЕ: OBJ модели дверей ориентированы вертикально (Y - высота створки)
            // Но мультиблок large_vehicle_door горизонтальный (X - ширина створки)
            // Поэтому ПОВОРАЧИВАЕМ модель на 90°: Y_obj → X_multiblock, X_obj → Y_multiblock
            // Для вертикальных дверей (qe_sliding_door и т.д.) поворот не нужен
            
            boolean needsRotation = needsModelRotation(doorId);
            
            AABB scaled;
            if (needsRotation) {
                // Поворот: Y модели → X мультиблока, X модели → Y мультиблока
                scaled = new AABB(
                    raw.minY * widthBlocks + offsetX,    // Y_obj -> X_world
                    raw.minX * heightBlocks + offsetY,   // X_obj -> Y_world
                    raw.minZ * depthBlocks + offsetZ,
                    raw.maxY * widthBlocks + offsetX,
                    raw.maxX * heightBlocks + offsetY,
                    raw.maxZ * depthBlocks + offsetZ
                );
            } else {
                // Обычное масштабирование без поворота
                scaled = new AABB(
                    raw.minX * widthBlocks + offsetX,
                    raw.minY * heightBlocks + offsetY,
                    raw.minZ * depthBlocks + offsetZ,
                    raw.maxX * widthBlocks + offsetX,
                    raw.maxY * heightBlocks + offsetY,
                    raw.maxZ * depthBlocks + offsetZ
                );
            }

            // Поворачиваем по facing
            AABB rotated = rotateAABBByFacing(scaled, facing);

            // Конвертируем в VoxelShape (координаты в пикселях 0..16)
            VoxelShape partShape = Block.box(
                rotated.minX * 16.0, rotated.minY * 16.0, rotated.minZ * 16.0,
                rotated.maxX * 16.0, rotated.maxY * 16.0, rotated.maxZ * 16.0
            );

            finalShape = Shapes.or(finalShape, partShape);
        }

        return finalShape.optimize();
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
     * Поворачивает AABB вокруг оси Y в зависимости от facing двери.
     */
    public static AABB rotateAABBByFacing(AABB aabb, Direction facing) {
        // Используем тот же алгоритм поворота, что и у rotate(BlockPos)
        return switch (facing) {
            case SOUTH -> new AABB(-aabb.maxX, aabb.minY, -aabb.maxZ, -aabb.minX, aabb.maxY, -aabb.minZ);
            case WEST  -> new AABB(-aabb.maxZ, aabb.minY,  aabb.minX, -aabb.minZ, aabb.maxY,  aabb.maxX);
            case EAST  -> new AABB( aabb.minZ, aabb.minY, -aabb.maxX,  aabb.maxZ, aabb.maxY, -aabb.minX);
            default    -> aabb; // NORTH — без изменений
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
        
        // Используем structureMap вместо partLocations
        for (BlockPos localOffset : structureMap.keySet()) {
            BlockPos worldPos = getRotatedPos(controllerPos, localOffset, facing);
            positions.add(worldPos);
        }
        
        return positions;
    }
}
