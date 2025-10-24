package com.hbm_m.multiblock;

// Item для главного блока-контроллера мультиблочной структуры.
// Выполняет проверку структуры перед установкой блока в мир. Если что-то мешает постройке, установка не происходит. Мешающие блоки выделяются красным.

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class MultiblockBlockItem extends BlockItem {

    public MultiblockBlockItem(Block pBlock, Properties pProperties) {
        super(pBlock, pProperties);
        // Проверяем, что переданный блок действительно является контроллером.
        // Если нет, игра вылетит при запуске с понятной ошибкой.
        if (!(pBlock instanceof IMultiblockController)) {
            throw new IllegalArgumentException("MultiblockBlockItem can only be used with blocks that implement IMultiblockController!");
        }
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext pContext, BlockState pState) {
        // Мы знаем, что наш блок реализует IMultiblockController, поэтому каст безопасен.
        IMultiblockController controller = (IMultiblockController) this.getBlock();
        
        Level level = pContext.getLevel();
        Player player = pContext.getPlayer();
        BlockPos pos = pContext.getClickedPos();
        
        // Пытаемся получить направление из BlockState.
        // Это делает код устойчивым к блокам, у которых может не быть свойства FACING.
        if (!pState.hasProperty(HorizontalDirectionalBlock.FACING)) {
            // Если у блока нет направления (например, это какая-то вертикальная структура),
            // то дальнейшая логика не будет работать. Вы можете либо бросить ошибку,
            // либо использовать направление по умолчанию. Для универсальности, мы можем просто
            // прекратить выполнение, но для мультиблоков это маловероятно.
            // Для простоты оставим как есть, но в будущем это можно доработать.
        }
        
        if (!pState.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return false; // Вместо пустого if
        }
        
        Direction facing = pState.getValue(HorizontalDirectionalBlock.FACING);
        if (facing == null || facing.getAxis() == Direction.Axis.Y) {
            // Защита от вертикальных направлений для горизонтальных структур
            return false;
        }

        // Выполняем проверку ДО вызова родительского метода placeBlock
        if (controller.getStructureHelper().checkPlacement(level, pContext.getClickedPos(), facing, player)) {
            return super.placeBlock(pContext, pState);
        } else {
            return false;
        }
    }
}