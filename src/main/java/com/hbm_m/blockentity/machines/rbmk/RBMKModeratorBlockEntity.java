package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler.RBMKType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class RBMKModeratorBlockEntity extends RBMKColumnBlockEntity {

    public RBMKModeratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_MODERATOR_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RBMKModeratorBlockEntity be) {
        baseTick(level, pos, state, be);
    }

    /** 1:1 with the original's {@code TileEntityRBMKModerator.onMelt}: 2-3 GRAPHITE debris before the standard melt. */
    @Override
    public void onMelt(Level level, int reduce) {
        int count = 2 + level.random.nextInt(2);
        for (int i = 0; i < count; i++) spawnDebris(level, "graphite");
        super.onMelt(level, reduce);
    }

    @Override public RBMKType getRBMKType()      { return RBMKType.MODERATOR; }
    @Override public boolean  isModerated()      { return true; }
    @Override public ColumnType getConsoleType() { return ColumnType.MODERATOR; }
}

