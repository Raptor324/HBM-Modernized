package com.hbm_m.multiblock;

// Интерфейс для части мультиблочной структуры. Позволяет частям знать позицию контроллера и свою роль в структуре.
// Используется вместе с MultiblockStructureHelper для управления мультиблочными структурами.

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;


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

    /**
     * Устанавливает роль для этой части. Вызывается контроллером при постройке.
     * @param role Роль, назначенная этой части.
     */
    void setPartRole(PartRole role);

    /**
     * Возвращает текущую роль части (может быть DEFAULT если не назначено).
     */
    PartRole getPartRole();
}