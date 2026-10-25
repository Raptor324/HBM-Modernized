package com.hbm_m.block.decorations;

import java.util.function.Supplier;

import com.hbm_m.block.generic.ReinforcedLamp;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.blocks.generic.TritiumLamp} ({@code lamp_tritium_green/blue_on/off}): Redstonelampe wie die
 * verstaerkte Lampe, die eingeschaltet in alle sechs Richtungen einen Lichtstrahl der Laenge 8 wirft (Farbe gruen/blau).
 */
public class TritiumLampBlock extends ReinforcedLamp implements ISpotlight {

    private final int color;

    public TritiumLampBlock(Properties properties, boolean isOn, int color, Supplier<Block> off, Supplier<Block> on) {
        super(properties, isOn, off, on);
        this.color = color;
    }

    @Override
    protected void check(Level world, BlockPos pos) {
        super.check(world, pos);
        if (!world.isClientSide) updateBeam(world, pos);
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, world, pos, newState, isMoving);
        if (world.isClientSide || newState.getBlock() == this) return;
        for (Direction dir : Direction.values()) SpotlightBlock.unpropagateBeam(world, pos, dir);
    }

    private void updateBeam(Level world, BlockPos pos) {
        if (!isOn) return;
        if (world.getBlockState(pos).getBlock() != this) return;
        for (Direction dir : Direction.values()) SpotlightBlock.propagateBeam(world, pos, dir, getBeamLength(), color);
    }

    @Override
    public int getBeamLength() {
        return 8;
    }
}
