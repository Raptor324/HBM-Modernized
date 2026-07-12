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

    @Override public RBMKType getRBMKType()      { return RBMKType.MODERATOR; }
    @Override public boolean  isModerated()      { return true; }
    @Override public ColumnType getConsoleType() { return ColumnType.MODERATOR; }
}

