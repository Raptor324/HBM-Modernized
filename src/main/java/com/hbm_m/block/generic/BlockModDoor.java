package com.hbm_m.block.generic;

import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code com.hbm.blocks.generic.BlockModDoor} ({@code door_metal}, {@code door_office}, {@code door_bunker},
 * {@code door_red}): Eisentuer, die sich trotzdem per Hand oeffnen laesst; Klang {@code hbm:block.openDoor}
 * (Tonhoehe 0.9-1.0) beim Oeffnen und Schliessen, auch per Redstone.
 */
public class BlockModDoor extends DoorBlock {

    public BlockModDoor(Properties props) {
        //? if < 1.21.1 {
        super(props, BlockSetType.IRON);
        //?} else {
        /*super(BlockSetType.IRON, props);
        *///?}
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    //?} else {
    /*@Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack hbmHeld, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return com.hbm_m.platform.BlockUseHooks.item(hbmUse(state, world, pos, player, hand, hit));
    }
    private InteractionResult hbmUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    *///?}
        state = state.cycle(OPEN);
        world.setBlock(pos, state, 10);
        playOpenSound(world, pos);
        world.gameEvent(player, this.isOpen(state) ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
        return InteractionResult.sidedSuccess(world.isClientSide);
    }

    @Override
    public void setOpen(Entity entity, Level world, BlockState state, BlockPos pos, boolean open) {
        if (state.is(this) && state.getValue(OPEN) != open) {
            world.setBlock(pos, state.setValue(OPEN, open), 10);
            playOpenSound(world, pos);
            world.gameEvent(entity, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
        }
    }

    /** Wie DoorBlock.neighborChanged, aber mit dem Klang des Originals ({@code func_150014_a}). */
    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, net.minecraft.world.level.block.Block block, BlockPos fromPos, boolean moving) {
        boolean flag = world.hasNeighborSignal(pos) || world.hasNeighborSignal(pos.relative(
                state.getValue(HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER ? net.minecraft.core.Direction.UP : net.minecraft.core.Direction.DOWN));
        if (!this.defaultBlockState().is(block) && flag != state.getValue(POWERED)) {
            if (flag != state.getValue(OPEN)) {
                playOpenSound(world, pos);
                world.gameEvent(null, flag ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
            }
            world.setBlock(pos, state.setValue(POWERED, flag).setValue(OPEN, flag), 2);
        }
    }

    private static void playOpenSound(Level world, BlockPos pos) {
        world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), HbmSoundsNT.get("hbm:block.openDoor"), SoundSource.BLOCKS, 1.0F, world.random.nextFloat() * 0.1F + 0.9F);
    }
}
