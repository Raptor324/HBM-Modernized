package com.hbm_m.multiblock;

import com.hbm_m.interfaces.IMultiblockController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Утилитарный класс для расчёта смещения мультиблочных структур.
 */
public final class MultiblockPlacement {

    private MultiblockPlacement() {}

    /**
     * Возвращает позицию ядра, смещаясь от "фасада" (клетки клика) вглубь структуры.
     */
    public static BlockPos getCorePos(BlockPos facadePos, Direction facing, int placementOffset) {
        if (placementOffset <= 0) {
            return facadePos;
        }
        return facadePos.relative(facing.getOpposite(), placementOffset);
    }

    /** 
     * Обратная функция: возвращает клетку фасада по позиции ядра.
     */
    public static BlockPos getFacadePos(BlockPos corePos, Direction facing, int placementOffset) {
        if (placementOffset <= 0) {
            return corePos;
        }
        return corePos.relative(facing, placementOffset);
    }

    public static BlockPos getCorePos(BlockPos placedPos, Direction facing, MultiblockStructureHelper helper) {
        return getCorePos(placedPos, facing, helper.getPlacementOffset());
    }

    public static BlockPos getCorePos(BlockPos placedPos, Direction facing, MultiblockStructureHelper helper, net.minecraft.world.level.block.Block block) {
        int offset = helper.getPlacementOffset();
        if (block instanceof IMultiblockController controller) {
            int customOffset = controller.getOffset();
            if (customOffset >= 0) {
                offset = customOffset;
            }
        }
        return getCorePos(placedPos, facing, offset);
    }

    public static boolean canSurvive(BlockState state, LevelReader level, BlockPos placedPos, MultiblockStructureHelper helper) {
        int offset = helper.getPlacementOffset();
        if (state.getBlock() instanceof IMultiblockController controller) {
            int customOffset = controller.getOffset();
            if (customOffset >= 0) {
                offset = customOffset;
            }
        }
        if (offset <= 0) {
            return true;
        }
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        BlockPos corePos = getCorePos(placedPos, facing, offset);
        if (corePos.equals(placedPos)) {
            return true;
        }
        return level.getBlockState(corePos).canBeReplaced();
    }
}