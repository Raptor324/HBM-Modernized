package com.hbm_m.interfaces;

// Интерфейс для главного блока-контроллера мультиблочной структуры.
// Он нужен, чтобы обработчик событий мог найти контроллер, наведясь на любую часть.

import org.jetbrains.annotations.Nullable;

import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;


public interface IMultiblockController {
    /**
     * @return Экземпляр MultiblockStructureHelper, описывающий данную структуру.
     */
    MultiblockStructureHelper getStructureHelper();

    // Предоставляет смещение по умолчанию. 
    // Если возвращается -1, система рассчитывает смещение программно.
    default int getOffset() {
        return -1;
    }

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

    /**
     * Returns whether this controller should be destroyed when one of its machine parts is removed.
     * Override to return false for "permanent" destroyed/ruin states that should never auto-collapse.
     */
    default boolean shouldDestroyOnPartRemoved() {
        return true;
    }

    /**
     * Если вы изменили размер или компоновку структуры в новой версии мода,
     * из-за чего контроллер сместился в координатах шаблона (local grid),
     * верните здесь его СТАРЫЙ офсет.
     * Это позволит системе починки сдвинуть ядро так, чтобы вся структура осталась
     * стоять на своих старых координатах в мире.
     * Если возвращает null — считается, что смещения ядра не было.
     */
    @Nullable
    default BlockPos getLegacyControllerOffset() {
        return null;
    }

    /** 
     * Поскольку мы используем MultiblockBlockItem, блок всегда спавнится сразу в 
     * координатах ядра (уже в глубине). Проверка на свободное место теперь делается 
     * ДО установки. Этот метод можно использовать для дополнительных проверок на выживаемость.
     */
    default boolean canSurviveMultiblockPlacement(BlockState state, LevelReader level, BlockPos placedPos) {
        return true;
    }

    /**
     * Строит фантомные части мультиблока вокруг контроллера.
     * <p>
     * Вызывается из метода setPlacedBy() или onPlace() в вашем классе блока.
     * Контроллер в этот момент УЖЕ стоит на правильных координатах.
     */
    @Nullable
    default BlockPos placeMultiblockStructure(Level level, BlockPos placedPos, BlockState state) {
        MultiblockStructureHelper helper = getStructureHelper();
        
        if (helper != null && !level.isClientSide()) {
            Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
            // Спавним фантомы вокруг уже установленного ядра
            helper.placeStructure(level, placedPos, facing, this);
        }
        
        return placedPos;
    }
}