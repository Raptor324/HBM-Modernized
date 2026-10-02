package com.hbm_m.block.machines;

import com.hbm_m.entity.logic.EntityNukeExplosionMK3;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** 1:1 {@code MachineFieldDisturber}: meldet sich alle 10 Ticks fuer 100 Ticks als Stoerer gegen F.L.E.I.J.A.-Explosionen. */
public class FieldDisturberBlock extends Block {

    public FieldDisturberBlock(Properties p) { super(p); }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState old, boolean moved) {
        if (!world.isClientSide) world.scheduleTick(pos, this, 10);
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        world.scheduleTick(pos, this, 10);
        EntityNukeExplosionMK3.at.put(new EntityNukeExplosionMK3.ATEntry(world.dimension().location().toString(), pos.getX(), pos.getY(), pos.getZ()), world.getGameTime() + 100);
    }
}
