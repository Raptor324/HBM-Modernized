package com.hbm_m.multiblock;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes; // Импорт

public interface IMultiblockController {
    /**
     * @return Экземпляр MultiblockStructureHelper, описывающий данную структуру.
     */
    MultiblockStructureHelper getStructureHelper();

    /**
     * Позволяет переопределить стандартную VoxelShape для мультиблока.
     * Используется для структур со сложной геометрией (провода, выступы и т.д.).
     * 
     * Если этот метод возвращает Shapes.empty(), система автоматически сгенерирует
     * форму на основе расположения блоков-частей.
     *
     * @param state Состояние блока-контроллера.
     * @return Кастомная VoxelShape или Shapes.empty() для использования авто-генерации.
     */
    default VoxelShape getCustomMasterVoxelShape(BlockState state) {
        // По умолчанию возвращаем ПУСТУЮ форму.
        // Это сигнал для системы: "У меня нет кастомной формы, генерируй стандартную".
        return Shapes.empty();
    }
}