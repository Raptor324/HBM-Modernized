package com.hbm_m.blockentity.network.radio;

import com.hbm_m.block.machines.radio.RadioTorchBaseBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of {@code TileEntityRadioTorchSender} (1.7.10 Original). Reads the redstone signal on the
 * face it's attached to (behind the torch) and broadcasts it on {@link #channel} whenever it
 * changes (or every tick if {@link #polling} is enabled).
 */
public class RadioTorchSenderBlockEntity extends RadioTorchBaseBlockEntity {

    public RadioTorchSenderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADIO_TORCH_SENDER_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RadioTorchSenderBlockEntity be) {
        if (level.isClientSide) return;
        RTTYNetwork.tickIfNeeded(level.getGameTime());

        Direction facing = state.hasProperty(RadioTorchBaseBlock.FACING) ? state.getValue(RadioTorchBaseBlock.FACING) : Direction.UP;
        BlockPos sourcePos = pos.relative(facing.getOpposite());
        int input = level.getBlockState(sourcePos).getSignal(level, sourcePos, facing.getOpposite());

        boolean shouldSend = be.polling;
        if (input != be.lastState) {
            be.lastState = input;
            be.setChanged();
            be.syncToClient();
            shouldSend = true;
        }

        if (shouldSend && !be.channel.isEmpty()) {
            String toSend = be.customMap ? be.mapping[input] : String.valueOf(input);
            if (toSend != null && !toSend.isEmpty()) RTTYNetwork.broadcast(level, be.channel, toSend);
        }
    }
}
