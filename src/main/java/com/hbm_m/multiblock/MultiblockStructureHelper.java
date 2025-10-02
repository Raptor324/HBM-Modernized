package com.hbm_m.multiblock;

// Утилитарный класс для управления мультиблочными структурами.
// Позволяет определять структуру, проверять возможность постройки, строить и разрушать структуру,
// а также генерировать VoxelShape для всей структуры. Ядро всей мультиблочной логики.
import com.hbm_m.block.WireBlock;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class MultiblockStructureHelper {

    private final Map<BlockPos, Supplier<BlockState>> structureMap;
    private final Supplier<BlockState> phantomBlockState;

    public MultiblockStructureHelper(Map<BlockPos, Supplier<BlockState>> structureMap, Supplier<BlockState> phantomBlockState) {
        this.structureMap = structureMap;
        this.phantomBlockState = phantomBlockState;
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
        return this.structureMap.keySet();
    }

    public void placeStructure(Level level, BlockPos controllerPos, Direction facing, IMultiblockController controller) {
        if (level.isClientSide) return;

        MainRegistry.LOGGER.debug("[STRUCTURE] Начинаем установку структуры с контроллером на " + controllerPos);

        for (Map.Entry<BlockPos, Supplier<BlockState>> entry : structureMap.entrySet()) {
            BlockPos relativePos = entry.getKey();
            if (relativePos.equals(BlockPos.ZERO)) continue;

            BlockPos worldPos = getRotatedPos(controllerPos, relativePos, facing);
            BlockState partState = phantomBlockState.get().setValue(HorizontalDirectionalBlock.FACING, facing);

            // ИСПОЛЬЗУЕМ ФЛАГ 2: Только обновление соседей, без отправки пакета клиенту внутри цикла.
            // Это предотвратит лишние нагрузки во время постройки.
            level.setBlock(worldPos, partState, 2);

            BlockEntity be = level.getBlockEntity(worldPos);
            if (be instanceof IMultiblockPart partBe) {
                partBe.setControllerPos(controllerPos);
                partBe.setPartRole(controller.getPartRole(relativePos));
            }
        }
        
        // ВАЖНО: После того как ВСЯ структура построена, мы один раз проверяем состояние рамки.
        // Этой проверки достаточно, не нужно делать ее для каждой части.
        updateFrameForController(level, controllerPos);
        MainRegistry.LOGGER.debug("[STRUCTURE] Установка завершена, рамка проверена.");
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
            if (stateInWorld.getBlock() instanceof com.hbm_m.block.UniversalMachinePartBlock) {
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
        int maxY = getMaxY(); // Вызываем ОДИН раз

        MainRegistry.LOGGER.debug("[FRAME COMPUTE] Проверка рамки для контроллера на " + controllerPos);
        MainRegistry.LOGGER.debug("[FRAME COMPUTE] Направление: " + facing);
        MainRegistry.LOGGER.debug("[FRAME COMPUTE] Максимальная локальная Y: " + maxY);
        MainRegistry.LOGGER.debug("[FRAME COMPUTE] Найдено " + topRingWorld.size() + " блоков верхнего пояса");

        for (BlockPos p : topRingWorld) {
            BlockPos above = p.above();
            BlockState stateAbove = level.getBlockState(above);
            MainRegistry.LOGGER.debug("[FRAME COMPUTE] Блок верхнего пояса: " + p);
            MainRegistry.LOGGER.debug("[FRAME COMPUTE] Над ним (" + above + "): " + stateAbove.getBlock());
            MainRegistry.LOGGER.debug("[FRAME COMPUTE] Пустой? " + level.isEmptyBlock(above));

            if (!level.isEmptyBlock(above)) {
                MainRegistry.LOGGER.debug("[FRAME COMPUTE] ✓ Найден непустой блок! Рамка ВИДИМА");
                return true;
            }
        }

        MainRegistry.LOGGER.debug("[FRAME COMPUTE] ✗ Все блоки над структурой пусты, рамка СКРЫТА");
        return false;
    }


    /**
     * Обновляет состояние рамки для контроллера с синхронизацией клиента
     */
    public static void updateFrameForController(Level level, BlockPos controllerPos) {
        if (level.isClientSide()) return;

        MainRegistry.LOGGER.debug("[FRAME] === НАЧАЛО ПРОВЕРКИ РАМКИ ===");
        MainRegistry.LOGGER.debug("[FRAME] Контроллер на позиции: " + controllerPos);

        BlockState state = level.getBlockState(controllerPos);
        Block block = state.getBlock(); // Получаем сам блок

        // Проверяем, что БЛОК является контроллером
        if (!(block instanceof IMultiblockController controller)) {
            MainRegistry.LOGGER.debug("[FRAME ERROR] Блок " + block.asItem() + " НЕ является IMultiblockController!");
            return;
        }
        
        BlockEntity be = level.getBlockEntity(controllerPos);
        // Проверяем, что BlockEntity поддерживает рамку
        if (!(be instanceof IFrameSupportable frameSupportable)) {
            MainRegistry.LOGGER.debug("[FRAME ERROR] BlockEntity НЕ IFrameSupportable!");
            return;
        }

        MainRegistry.LOGGER.debug("[FRAME] Контроллер и BlockEntity найдены, получаем параметры...");
        
        if (!state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            MainRegistry.LOGGER.debug("[FRAME ERROR] У BlockState нет свойства FACING!");
            return;
        }

        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        MainRegistry.LOGGER.debug("[FRAME] Направление структуры: " + facing);

        // Получаем хелпер из КОНТРОЛЛЕРА (блока)
        MultiblockStructureHelper helper = controller.getStructureHelper();
        if (helper == null) {
            MainRegistry.LOGGER.debug("[FRAME ERROR] getStructureHelper() вернул NULL!");
            return;
        }

        MainRegistry.LOGGER.debug("[FRAME] StructureHelper получен успешно");
        // Вычисляем видимость рамки
        boolean visible = helper.computeFrameVisible(level, controllerPos, facing);
        MainRegistry.LOGGER.debug("[FRAME] Результат computeFrameVisible: " + visible);

        // Применяем изменения через интерфейс BlockEntity
        frameSupportable.setFrameVisible(visible);
        MainRegistry.LOGGER.debug("[FRAME] === КОНЕЦ ПРОВЕРКИ РАМКИ ===");
    }


    /**
     * Вызывается из фантомной части при изменении соседа
     * ИСПРАВЛЕНО: теперь правильно вычисляет локальный оффсет
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
            MainRegistry.LOGGER.debug("[FRAME PART] Изменение НАД частью! Планируем проверку на следующий тик.");
            
            // Вместо прямого вызова, планируем задачу на следующий тик сервера.
            level.getServer().execute(() -> {
                MainRegistry.LOGGER.debug("[FRAME PART TICK] Выполняется отложенная проверка рамки для контроллера на {}.", ctrlPos);
                // Проверяем, что контроллер все еще на месте, перед выполнением
                if (level.getBlockState(ctrlPos).is(controllerBlock)) {
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
}
