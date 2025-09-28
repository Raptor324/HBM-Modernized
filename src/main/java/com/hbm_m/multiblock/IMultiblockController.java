package com.hbm_m.multiblock;

// Интерфейс для главного блока-контроллера мультиблочной структуры.
// Он нужен, чтобы обработчик событий мог найти контроллер, наведясь на любую часть.

import javax.annotation.Nullable;


import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;


public interface IMultiblockController {
    /**
     * @return Экземпляр MultiblockStructureHelper, описывающий данную структуру.
     */
    MultiblockStructureHelper getStructureHelper();

    /**
     * Returns the specific role of a multiblock part based on its local offset from the controller.
     * This is where you define which parts are energy connectors, item inputs/outputs, etc.
     *
     * @param localOffset The part's position relative to the controller (e.g., new BlockPos(1, 0, 0)).
     * @return The {@link PartRole} for that part.
     */
    PartRole getPartRole(BlockPos localOffset);

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
    @Nullable
    default VoxelShape getCustomMasterVoxelShape(BlockState state) {
        // По умолчанию возвращаем ПУСТУЮ форму.
        // Это сигнал для системы: "У меня нет кастомной формы, генерируй стандартную".
        return null;
    }
}