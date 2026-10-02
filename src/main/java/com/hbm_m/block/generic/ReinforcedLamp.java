package com.hbm_m.block.generic;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@link com.hbm.blocks.generic.ReinforcedLamp}: Redstonelampe aus zwei Bloecken (an/aus). Geht sofort an, wenn
 * Strom anliegt, und 4 Ticks nach Wegfall wieder aus. Abbau und Mittelklick geben immer die ausgeschaltete Lampe.
 */
public class ReinforcedLamp extends Block {

    protected final boolean isOn;
    private final Supplier<Block> off;
    private final Supplier<Block> on;

    public ReinforcedLamp(Properties properties, boolean isOn, Supplier<Block> off, Supplier<Block> on) {
        super(isOn ? properties.lightLevel(s -> 15) : properties);
        this.isOn = isOn;
        this.off = off;
        this.on = on;
    }

    protected Block getOff() { return off.get(); }
    protected Block getOn() { return on.get(); }

    protected void check(Level world, BlockPos pos) {
        if (world.isClientSide) return;
        if (this.isOn && !world.hasNeighborSignal(pos)) {
            world.scheduleTick(pos, this, 4);
        } else if (!this.isOn && world.hasNeighborSignal(pos)) {
            world.setBlock(pos, getOn().defaultBlockState(), 2);
        }
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, world, pos, oldState, isMoving);
        check(world, pos);
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, world, pos, block, fromPos, isMoving);
        check(world, pos);
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        if (this.isOn && !world.hasNeighborSignal(pos)) {
            world.setBlock(pos, getOff().defaultBlockState(), 2);
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter world, BlockPos pos, BlockState state) {
        return new ItemStack(getOff());
    }
}
