package com.hbm_m.compat.create;

//? if forge || neoforge {
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.interfaces.IMultiblockPart;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Утилита для расширения набора позиций блоков до полного мультиблока HBM.
 * <p>
 * Используется в миксинах Create/Sable для Contraption и SuperGlue, чтобы
 * обеспечить захват ВСЕХ частей мультиблока, если хотя бы одна часть/контроллер
 * попала в выборку.
 * </p>
 * <p>
 * Логика: если в переданном наборе позиций есть хотя бы одна часть (IMultiblockPart)
 * или контроллер (IMultiblockController), находим контроллер, через его
 * MultiblockStructureHelper получаем все позиции структуры и добавляем их в результат.
 * </p>
 */
public final class MultiblockExpander {

    private MultiblockExpander() {}

    /**
     * Расширяет переданный набор позиций до полных мультиблоков HBM.
     *
     * @param level     уровень, на котором происходит сборка
     * @param positions исходный набор позиций (например, из клея или BFS контрапшена)
     * @return новый набор, содержащий все исходные позиции + все части найденных мультиблоков
     */
    public static Set<BlockPos> expandToFullMultiblock(Level level, Collection<BlockPos> positions) {
        Set<BlockPos> result = new HashSet<>(positions);
        // Множество уже обработанных контроллеров — чтобы не дублировать работу
        Set<BlockPos> visitedControllers = new HashSet<>();

        for (BlockPos pos : positions) {
            BlockEntity be = level.getBlockEntity(pos);
            BlockPos controllerPos = resolveControllerPos(be, pos);

            if (controllerPos != null && visitedControllers.add(controllerPos)) {
                // Сам контроллер тоже часть выделения: structureMap содержит ТОЛЬКО части,
                // поэтому getAllPartPositions контроллер не возвращает.
                result.add(controllerPos.immutable());
                // Получаем все позиции мультиблока
                Collection<BlockPos> allParts = getAllMultiblockPositions(level, controllerPos);
                if (allParts != null) {
                    result.addAll(allParts);
                }
            }
        }

        return result;
    }

    /**
     * Находит позицию контроллера мультиблока по любой его части или по самому контроллеру.
     *
     * @param be  BlockEntity на позиции pos
     * @param pos позиция блока
     * @return позиция контроллера, или null если блок не является частью мультиблока HBM
     */
    private static BlockPos resolveControllerPos(BlockEntity be, BlockPos pos) {
        if (be instanceof IMultiblockPart part) {
            // Часть знает своего контроллера
            return part.getControllerPos();
        }
        if (be instanceof IMultiblockController) {
            // Сам контроллер
            return pos;
        }
        return null;
    }

    /**
     * Возвращает все мировые позиции мультиблока по позиции его контроллера.
     *
     * @param level         уровень
     * @param controllerPos позиция контроллера
     * @return список всех позиций мультиблока, или null если контроллер не найден
     */
    public static Collection<BlockPos> getAllMultiblockPositions(Level level, BlockPos controllerPos) {
        BlockEntity be = level.getBlockEntity(controllerPos);
        if (!(be instanceof IMultiblockController controller)) {
            return null;
        }

        MultiblockStructureHelper helper = controller.getStructureHelper();
        if (helper == null) {
            return null;
        }

        BlockState state = level.getBlockState(controllerPos);
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);

        return helper.getAllPartPositions(controllerPos, facing);
    }
}
//?}