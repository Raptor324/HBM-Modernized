package com.hbm_m.multiblock;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.interfaces.IFrameSupportable;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.interfaces.IMultiblockPart;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Общая логика рамки (frame) для мультиблоков с {@code BooleanProperty} {@code frame} на контроллере.
 * Рамка видна, если над любой частью верхнего пояса структуры есть непустой блок.
 */
public final class MultiblockFrameHelper {

    public static final String FRAME_PROPERTY_NAME = "frame";

    private MultiblockFrameHelper() {}

    @Nullable
    public static BooleanProperty findFrameProperty(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (FRAME_PROPERTY_NAME.equals(property.getName()) && property instanceof BooleanProperty frame) {
                return frame;
            }
        }
        return null;
    }

    public static int getMaxY(MultiblockStructureHelper structure) {
        int maxY = Integer.MIN_VALUE;
        for (BlockPos local : structure.getStructureMap().keySet()) {
            if (local.getY() > maxY) {
                maxY = local.getY();
            }
        }
        return maxY;
    }

    public static boolean isTopRingPart(MultiblockStructureHelper structure, BlockPos localOffset) {
        return localOffset.getY() == getMaxY(structure);
    }

    public static List<BlockPos> getTopRingWorldPositions(
            MultiblockStructureHelper structure, BlockPos controllerPos, Direction facing) {
        List<BlockPos> topRing = new ArrayList<>();
        int maxY = getMaxY(structure);
        for (BlockPos localOffset : structure.getStructureMap().keySet()) {
            if (localOffset.getY() == maxY) {
                topRing.add(structure.getRotatedPos(controllerPos, localOffset, facing));
            }
        }
        return topRing;
    }

    /**
     * Рамка видна, если над любой клеткой верхнего слоя мультиблока стоит непустой блок.
     */
    public static boolean computeFrameVisible(
            Level level, MultiblockStructureHelper structure, BlockPos controllerPos, Direction facing) {
        for (BlockPos worldPos : getTopRingWorldPositions(structure, controllerPos, facing)) {
            if (!level.isEmptyBlock(worldPos.above())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Записывает видимость рамки в BlockState контроллера (свойство {@code frame}).
     *
     * @return true, если BlockState изменился
     */
    public static boolean applyFrameToBlockState(Level level, BlockPos controllerPos, boolean visible) {
        BlockState state = level.getBlockState(controllerPos);
        BooleanProperty frameProp = findFrameProperty(state);
        if (frameProp == null || state.getValue(frameProp) == visible) {
            return false;
        }
        level.setBlock(controllerPos, state.setValue(frameProp, visible), 3);
        return true;
    }

    /**
     * Обновляет рамку для контроллера: пересчёт по верхнему поясу + запись в BlockState.
     */
    public static void updateFrameForController(Level level, BlockPos controllerPos) {
        if (level.isClientSide()) {
            return;
        }

        BlockState state = level.getBlockState(controllerPos);
        if (!(state.getBlock() instanceof IMultiblockController controller)) {
            return;
        }

        BlockEntity be = level.getBlockEntity(controllerPos);
        if (!(be instanceof IFrameSupportable)) {
            return;
        }

        if (!state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return;
        }

        MultiblockStructureHelper helper = controller.getStructureHelper();
        if (helper == null) {
            return;
        }

        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        boolean visible = computeFrameVisible(level, helper, controllerPos, facing);
        applyFrameToBlockState(level, controllerPos, visible);
    }

    /**
     * Вызывается из фантомной части при изменении соседа над верхним поясом.
     */
    public static void onNeighborChangedForPart(Level level, BlockPos partPos, BlockPos changedPos) {
        if (level.isClientSide() || level.getServer() == null) {
            return;
        }

        BlockEntity partBe = level.getBlockEntity(partPos);
        if (!(partBe instanceof IMultiblockPart part)) {
            return;
        }

        BlockPos ctrlPos = part.getControllerPos();
        if (ctrlPos == null) {
            return;
        }

        BlockState controllerState = level.getBlockState(ctrlPos);
        if (!(controllerState.getBlock() instanceof IMultiblockController controller)) {
            return;
        }

        MultiblockStructureHelper helper = controller.getStructureHelper();
        if (helper == null) {
            return;
        }

        BlockPos worldOffset = partPos.subtract(ctrlPos);
        Direction facing = controllerState.getValue(HorizontalDirectionalBlock.FACING);
        BlockPos localOffset = MultiblockStructureHelper.rotateBack(worldOffset, facing);

        if (isTopRingPart(helper, localOffset) && changedPos.equals(partPos.above())) {
            level.getServer().execute(() -> {
                BlockEntity be = level.getBlockEntity(ctrlPos);
                if (be != null && !be.isRemoved()
                        && level.getBlockState(ctrlPos).is(controllerState.getBlock())) {
                    updateFrameForController(level, ctrlPos);
                }
            });
        }
    }

    public static boolean isFrameVisible(BlockState state) {
        BooleanProperty frameProp = findFrameProperty(state);
        return frameProp != null && state.getValue(frameProp);
    }
}
