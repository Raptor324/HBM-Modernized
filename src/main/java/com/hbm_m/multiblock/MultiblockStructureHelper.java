package com.hbm_m.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class MultiblockStructureHelper {

    private final Map<BlockPos, Supplier<BlockState>> structureMap;
    private final Set<Block> replaceableBlocks;

    public MultiblockStructureHelper(Map<BlockPos, Supplier<BlockState>> structureMap) {
        this.structureMap = structureMap;
        this.replaceableBlocks = Set.of(
                Blocks.AIR, Blocks.CAVE_AIR, Blocks.VOID_AIR, Blocks.GRASS, Blocks.TALL_GRASS,
                Blocks.FERN, Blocks.LARGE_FERN, Blocks.SNOW, Blocks.VINE, Blocks.WATER, Blocks.LAVA
        );
    }

    public boolean checkPlacement(Level level, BlockPos controllerPos, Direction facing, Player player) {
        for (BlockPos relativePos : structureMap.keySet()) {
            BlockPos worldPos = getRotatedPos(controllerPos, relativePos, facing);
            BlockState existingState = level.getBlockState(worldPos);
            if (!replaceableBlocks.contains(existingState.getBlock())) {
                if (player != null && !level.isClientSide) {
                    player.displayClientMessage(Component.translatable("chat.hbm_m.structure.obstructed"), true);
                }
                return false;
            }
        }
        return true;
    }

    public void placeStructure(Level level, BlockPos controllerPos, Direction facing) {
        if (level.isClientSide) return;
        for (Map.Entry<BlockPos, Supplier<BlockState>> entry : structureMap.entrySet()) {
            BlockPos relativePos = entry.getKey();
            BlockState partState = entry.getValue().get();
            if (partState.hasProperty(HorizontalDirectionalBlock.FACING)) {
                partState = partState.setValue(HorizontalDirectionalBlock.FACING, facing);
            }
            BlockPos worldPos = getRotatedPos(controllerPos, relativePos, facing);
            level.setBlock(worldPos, partState, 3);
            
            if (level.getBlockEntity(worldPos) instanceof IMultiblockPart partBe) {
                // Если да, то просто вызываем метод из интерфейса.
                // Этот код будет работать для ЛЮБОЙ мультиблочной структуры.
                partBe.setControllerPos(controllerPos);
            }
        }
    }

    public void destroyStructure(Level level, BlockPos controllerPos, Direction facing) {
        if (level.isClientSide) return;

        // 1. Уничтожаем сам контроллер, если он еще существует
        if (level.getBlockState(controllerPos).getBlock() instanceof IMultiblockController) {
            level.setBlock(controllerPos, Blocks.AIR.defaultBlockState(), 3);
        }

        // 2. Проходимся по всем частям структуры
        for (Map.Entry<BlockPos, Supplier<BlockState>> entry : structureMap.entrySet()) {
            BlockPos relativePos = entry.getKey();
            if (relativePos.equals(BlockPos.ZERO)) continue; // Пропускаем позицию контроллера

            // 3. Получаем БЛОК, который ДОЛЖЕН БЫТЬ на этом месте, из нашей карты
            Block expectedBlock = entry.getValue().get().getBlock();
            
            BlockPos worldPos = getRotatedPos(controllerPos, relativePos, facing);
            BlockState stateInWorld = level.getBlockState(worldPos);

            // 4. Сравниваем блок в мире с ожидаемым блоком.
            if (stateInWorld.is(expectedBlock)) {
                 level.setBlock(worldPos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    /**
     * Автоматически генерирует VoxelShape для структуры, объединяя
     * кубы 1x1x1 для каждой части. Идеально для не-прямоугольных структур.
     * @param facing Направление структуры.
     * @return Сгенерированная VoxelShape.
     */
    public VoxelShape generateShapeFromParts(Direction facing) {
        VoxelShape finalShape = Shapes.empty();

        // Включаем контроллер в форму
        finalShape = Shapes.or(finalShape, Block.box(0, 0, 0, 16, 16, 16));

        for (BlockPos localOffset : structureMap.keySet()) {
            if (localOffset.equals(BlockPos.ZERO)) continue;

            BlockPos rotatedOffset = rotate(localOffset, facing);
            
            // Создаем VoxelShape для одного блока и смещаем его
            VoxelShape partShape = Block.box(0, 0, 0, 16, 16, 16)
                                        .move(rotatedOffset.getX(), rotatedOffset.getY(), rotatedOffset.getZ());
            
            // Объединяем с общей формой
            finalShape = Shapes.or(finalShape, partShape);
        }
        return finalShape;
    }


    public Set<BlockPos> getPartOffsets() {
        return this.structureMap.keySet();
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