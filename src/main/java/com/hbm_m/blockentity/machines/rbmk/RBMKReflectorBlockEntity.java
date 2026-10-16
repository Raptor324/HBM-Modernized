package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler.RBMKType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class RBMKReflectorBlockEntity extends RBMKColumnBlockEntity {

    public RBMKReflectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_REFLECTOR_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RBMKReflectorBlockEntity be) {
        baseTick(level, pos, state, be);
    }

    /** 1:1 with the original's {@code TileEntityRBMKReflector.onMelt}: 1-2 BLANK debris before the standard melt. */
    @Override
    public void onMelt(Level level, int reduce) {
        int count = 1 + level.random.nextInt(2);
        for (int i = 0; i < count; i++) spawnDebris(level, "blank");
        super.onMelt(level, reduce);
    }

    @Override public RBMKType getRBMKType()      { return RBMKType.REFLECTOR; }
    @Override public ColumnType getConsoleType() { return ColumnType.REFLECTOR; }
}

