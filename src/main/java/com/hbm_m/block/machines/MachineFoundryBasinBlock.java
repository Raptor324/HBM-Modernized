package com.hbm_m.block.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineFoundryBasinBlockEntity;
import com.hbm_m.item.material.ItemCastMold;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class MachineFoundryBasinBlock extends BaseEntityBlock {

    private static final VoxelShape SHAPE = Shapes.or(
            box(0, 0, 0, 16, 2, 16),
            box(0, 2, 0, 2, 16, 16),
            box(14, 2, 0, 16, 16, 16),
            box(2, 2, 0, 14, 16, 2),
            box(2, 2, 14, 14, 16, 16)
    );

    public MachineFoundryBasinBlock(Properties props) { super(props); }

    @Override public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
    @Override public VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MachineFoundryBasinBlockEntity basin)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);

        // Take cast output first (priority)
        ItemStack output = basin.getOutputSlot();
        if (!output.isEmpty()) {
            if (player.addItem(output.copy())) {
                basin.takeOutput();
                return InteractionResult.SUCCESS;
            }
        }

        // Insert mold
        if (!held.isEmpty() && held.getItem() instanceof ItemCastMold) {
            if (basin.insertMold(held)) {
                if (!player.isCreative()) held.shrink(1);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
        }

        // Remove mold with empty hand
        if (held.isEmpty() && !basin.getMoldSlot().isEmpty()) {
            ItemStack mold = basin.takeMold();
            player.addItem(mold);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineFoundryBasinBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.FOUNDRY_BASIN_BE.get(),
                MachineFoundryBasinBlockEntity::tick);
    }
}
