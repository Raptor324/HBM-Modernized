package com.hbm_m.multiblock;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Интерфейс для BlockEntity, которые являются частями мультиблочной структуры.
 * Он нужен, чтобы обработчик событий мог найти контроллер, наведясь на любую часть.
 */
public interface IMultiblockPart {
    
    /**
     * @return Позиция главного блока-контроллера, или null, если она не установлена.
     */
    @Nullable
    BlockPos getControllerPos();

    /**
     * Устанавливает позицию главного блока-контроллера.
     * Этот метод вызывается из MultiblockStructureHelper при постройке структуры.
     * @param pos Позиция контроллера.
     */
    void setControllerPos(BlockPos pos);
}