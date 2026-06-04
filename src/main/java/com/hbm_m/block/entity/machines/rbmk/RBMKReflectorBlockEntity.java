package com.hbm_m.block.entity.machines.rbmk;

import com.hbm_m.block.entity.ModBlockEntities;
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

    @Override public RBMKType getRBMKType()      { return RBMKType.REFLECTOR; }
    @Override public ColumnType getConsoleType() { return ColumnType.REFLECTOR; }
}

