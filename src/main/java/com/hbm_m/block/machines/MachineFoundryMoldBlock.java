package com.hbm_m.block.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineFoundryMoldBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Port of the 1.7.10 FoundryMold block (extends FoundryCastingBase, same casting behaviour as the
 * basin - see {@link MachineFoundryMoldBlockEntity} - just a shallow rim-and-floor collision shape
 * instead of the basin's full raised-wall shape).
 */
public class MachineFoundryMoldBlock extends MachineFoundryBasinBlock {

    private static final VoxelShape SHAPE = Shapes.or(
            box(0, 0, 0, 16, 2, 16),
            box(0, 2, 0, 2, 8, 16),
            box(14, 2, 0, 16, 8, 16),
            box(2, 2, 0, 14, 8, 2),
            box(2, 2, 14, 14, 8, 16)
    );

    public MachineFoundryMoldBlock(Properties props) { super(props); }

    @Override public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
    @Override public VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineFoundryMoldBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.FOUNDRY_MOLD_BE.get(),
                MachineFoundryMoldBlockEntity::tick);
    }
}
