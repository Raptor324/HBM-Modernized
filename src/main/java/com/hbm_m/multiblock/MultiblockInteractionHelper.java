package com.hbm_m.multiblock;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.interfaces.IMultiblockPart;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Общий хелпер взаимодействия с мультиблочными структурами.
 *
 * Любые предметы/инструменты, которые должны работать с КОНТРОЛЛЕРОМ структуры
 * (radar linker, детонатор для Fat Man, fluid identifier и т.п.), обязаны
 * прогонять кликнутую позицию через {@link #resolveControllerPos(BlockGetter, BlockPos)}:
 * если клик пришёлся на блок-часть ({@link IMultiblockPart}) с известным контроллером,
 * возвращается позиция контроллера, иначе — исходная позиция.
 *
 * Так детонатор, кликнутый по любой части бомбы, взрывает контроллер,
 * а radar linker, кликнутый по любой части пусковой, пишет координаты контроллера.
 */
public final class MultiblockInteractionHelper {

    private MultiblockInteractionHelper() {
    }

    /**
     * Резолвит позицию контроллера для кликнутого блока.
     *
     * @param level мир (клиент или сервер)
     * @param pos   позиция кликнутого блока
     * @return позиция контроллера, если клик по части мультиблока, иначе {@code pos}
     */
    public static BlockPos resolveControllerPos(BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof IMultiblockPart part) {
            BlockPos controllerPos = part.getControllerPos();
            if (controllerPos != null) {
                return controllerPos;
            }
        }
        return pos;
    }

    /**
     * Возвращает BlockEntity контроллера для кликнутого блока
     * (или BlockEntity самого блока, если он не часть мультиблока).
     */
    @Nullable
    public static BlockEntity resolveControllerBlockEntity(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(resolveControllerPos(level, pos));
    }
}
