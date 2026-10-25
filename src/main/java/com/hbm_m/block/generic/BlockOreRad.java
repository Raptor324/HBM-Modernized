package com.hbm_m.block.generic;

import com.hbm_m.radiation.ChunkRadiationManager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code BlockOre(Material, rad, max)} (nur {@code ore_schrabidium}): setTickRandomly(true), alle
 * {@code tickRate} = 20 Ticks erhoeht der Block die Chunkstrahlung um {@code rad} und plant sich neu ein.
 * Zufallsticks laufen wie im Original ueber dieselbe Tick-Methode. Erbt das BlockOre-Verhalten (Oelerz-Nachruecken).
 */
public class BlockOreRad extends BlockOre {

    private final float rad;

    public BlockOreRad(Properties properties, float rad, float max) {
        super(properties.randomTicks());
        this.rad = rad;
    }

    /** Original {@code tickRate}: 20 mit Strahlung, sonst 100. */
    public int tickRate() {
        return this.rad > 0 ? 20 : 100;
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, world, pos, oldState, isMoving);

        if (this.rad > 0 && !world.isClientSide)
            world.scheduleTick(pos, this, this.tickRate());
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        if (this.rad > 0) {
            ChunkRadiationManager.incrementRad(world, pos.getX(), pos.getY(), pos.getZ(), rad);
            world.scheduleTick(pos, this, this.tickRate());
        }
    }
}
