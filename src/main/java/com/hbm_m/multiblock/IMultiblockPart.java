package com.hbm_m.multiblock;

import org.jetbrains.annotations.Nullable;

// Интерфейс для части мультиблочной структуры. Позволяет частям знать позицию контроллера и свою роль в структуре.
// Используется вместе с MultiblockStructureHelper для управления мультиблочными структурами.

import net.minecraft.core.BlockPos;


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

    // Метод для работы с направлениями лестниц.
    void setAllowedClimbSides(java.util.Set<net.minecraft.core.Direction> sides);
    
    java.util.Set<net.minecraft.core.Direction> getAllowedClimbSides();

    /**
     * Стороны, с которых часть-коннектор принимает/отдаёт энергию (мировые направления после постройки).
     * Для частей без роли энергоконнектора можно не вызывать; по умолчанию - см. default-реализацию.
     */
    default void setAllowedEnergySides(java.util.Set<net.minecraft.core.Direction> sides) {
        // no-op для частей без хранения (например двери)
    }

    /**
     * @return разрешённые стороны энергии; пустой набор на коннекторе трактуется как «все стороны» (совместимость).
     */
    default java.util.Set<net.minecraft.core.Direction> getAllowedEnergySides() {
        return java.util.EnumSet.allOf(net.minecraft.core.Direction.class);
    }
}