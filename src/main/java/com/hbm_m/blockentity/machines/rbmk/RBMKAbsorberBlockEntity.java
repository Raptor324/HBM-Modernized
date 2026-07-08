package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler.RBMKType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class RBMKAbsorberBlockEntity extends RBMKColumnBlockEntity {

    public RBMKAbsorberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_ABSORBER_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RBMKAbsorberBlockEntity be) {
        baseTick(level, pos, state, be);
    }

    @Override public RBMKType getRBMKType()      { return RBMKType.ABSORBER; }
    @Override public ColumnType getConsoleType() { return ColumnType.ABSORBER; }
}

