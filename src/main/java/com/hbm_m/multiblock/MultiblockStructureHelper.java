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
        // ... Этот метод уже написан правильно и не требует изменений
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
        // ... Этот метод тоже уже исправлен и универсален
        if (level.isClientSide) return;
        for (Map.Entry<BlockPos, Supplier<BlockState>> entry : structureMap.entrySet()) {
            BlockPos relativePos = entry.getKey();
            BlockState partState = entry.getValue().get();
            if (partState.hasProperty(HorizontalDirectionalBlock.FACING)) {
                partState = partState.setValue(HorizontalDirectionalBlock.FACING, facing);
            }
            BlockPos worldPos = getRotatedPos(controllerPos, relativePos, facing);
            level.setBlock(worldPos, partState, 3);
            
            // Здесь вам нужно будет обеспечить, чтобы все ваши PartBlockEntity
            // имели общий интерфейс или родительский класс для установки контроллера,
            // либо использовать instanceof для каждого типа.
            // Например:
            if (level.getBlockEntity(worldPos) instanceof com.hbm_m.block.entity.MachineAssemblerPartBlockEntity partBe) {
                partBe.setControllerPos(controllerPos);
            } else if (level.getBlockEntity(worldPos) instanceof com.hbm_m.block.entity.AdvancedAssemblyMachinePartBlockEntity advancedPartBe) {
                advancedPartBe.setControllerPos(controllerPos);
            }
        }
    }
    
    // --- ИСПРАВЛЕННЫЙ МЕТОД УНИЧТОЖЕНИЯ ---
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
            // Эта проверка теперь универсальна и будет работать для ЛЮБОЙ структуры,
            // определенной через этот хелпер.
            if (stateInWorld.is(expectedBlock)) {
                 level.setBlock(worldPos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    // --- Остальные методы хелпера без изменений ---

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