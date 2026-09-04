package com.hbm_m.multiblock;

import com.hbm_m.interfaces.IMultiblockController;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Item для блока-контроллера мультиблока.
 * При клике автоматически подменяет координаты установки на ядро.
 */
public class MultiblockBlockItem extends BlockItem {

    public MultiblockBlockItem(Block pBlock, Properties pProperties) {
        super(pBlock, pProperties);
        if (!(pBlock instanceof IMultiblockController)) {
            throw new IllegalArgumentException("MultiblockBlockItem can only be used with blocks that implement IMultiblockController!");
        }
    }

    @Override
    @Nullable
    public BlockPlaceContext updatePlacementContext(BlockPlaceContext context) {
        BlockPlaceContext vanillaContext = super.updatePlacementContext(context);
        if (vanillaContext == null) {
            return null;
        }

        BlockPlaceContext adjusted = shiftContextToCore(vanillaContext);
        return adjusted != null ? adjusted : vanillaContext;
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        IMultiblockController controller = (IMultiblockController) this.getBlock();
        Level level = context.getLevel();
        Player player = context.getPlayer();

        if (!state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return false;
        }

        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        if (facing.getAxis() == Direction.Axis.Y) {
            return false;
        }

        BlockPos corePos = context.getClickedPos();
        
        if (!controller.getStructureHelper().checkPlacement(level, corePos, facing, player)) {
            return false;
        }

        if (!level.getBlockState(corePos).canBeReplaced()) {
            return false;
        }

        return super.placeBlock(context, state);
    }

    @Nullable
    static BlockPlaceContext shiftContextToCore(BlockPlaceContext context) {
        if (!(context.getItemInHand().getItem() instanceof BlockItem blockItem)) {
            return null;
        }
        Block block = blockItem.getBlock();
        if (!(block instanceof IMultiblockController controller)) {
            return null;
        }

        BlockState preview = block.getStateForPlacement(context);
        if (preview == null || !preview.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return null;
        }

        Direction facing = preview.getValue(HorizontalDirectionalBlock.FACING);
        if (facing.getAxis() == Direction.Axis.Y) {
            return null;
        }

        MultiblockStructureHelper helper = controller.getStructureHelper();
        if (helper == null) {
            return null;
        }

        BlockPos facadePos = context.getClickedPos();
        
        // Используем offset из контроллера с фоллбеком на хелпер
        int offset = controller.getOffset();
        if (offset < 0) {
            offset = helper.getPlacementOffset();
        }
        
        BlockPos corePos = MultiblockPlacement.getCorePos(facadePos, facing, offset);
        if (corePos.equals(facadePos)) {
            return null;
        }

        // ВАЖНО: нельзя использовать обычный BlockPlaceContext — ванильный
        // getClickedPos() при занятой (не replaceable) клетке ядра пересчитывает
        // позицию как corePos.relative(грань исходного клика). Если игрок смотрел
        // на блок сверху вниз, грань = UP, и вся структура «взмывает» на блок
        // выше вместо честного отказа (как BlockDummyable 1.7.10: checkRequirement
        // отклоняет установку). Пинним позицию ядра явно.
        return new CorePlaceContext(context, corePos);
    }

    /** Контекст установки, чей {@code getClickedPos()} всегда возвращает позицию ядра. */
    private static final class CorePlaceContext extends BlockPlaceContext {

        private final BlockPos corePos;

        CorePlaceContext(BlockPlaceContext parent, BlockPos corePos) {
            super(parent.getLevel(), parent.getPlayer(), parent.getHand(), parent.getItemInHand(),
                    new BlockHitResult(Vec3.atCenterOf(corePos), parent.getClickedFace(), corePos, parent.isInside()));
            this.corePos = corePos.immutable();
        }

        @Override
        public BlockPos getClickedPos() {
            return this.corePos;
        }
    }
}