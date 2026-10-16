package com.hbm_m.block.machines;

import com.hbm_m.blockentity.machines.MachineFoundrySlagtapBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Port of the 1.7.10 FoundrySlagtap block - same shape/redstone-toggle as {@link MachineFoundryOutletBlock}. */
public class MachineFoundrySlagtapBlock extends MachineFoundryOutletBlock {

    public MachineFoundrySlagtapBlock(Properties props) { super(props); }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineFoundrySlagtapBlockEntity(pos, state);
    }
}
