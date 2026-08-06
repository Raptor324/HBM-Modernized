package com.hbm_m.block.machines.radio;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.radio.RadioTorchReceiverBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Port of {@code RadioTorchReceiver} (1.7.10 Original) - outputs the received RTTY value as redstone power. */
public class RadioTorchReceiverBlock extends RadioTorchBaseBlock {

    public RadioTorchReceiverBlock(Properties properties) { super(properties); }

    @Override
    public boolean isSignalSource(BlockState state) { return true; }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof RadioTorchReceiverBlockEntity be ? be.lastState : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadioTorchReceiverBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.RADIO_TORCH_RECEIVER_BE.get(),
                (lvl, pos, st, be) -> RadioTorchReceiverBlockEntity.tick(lvl, pos, st, (RadioTorchReceiverBlockEntity) be));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () ->
                    com.hbm_m.client.gui.radio.RadioTorchScreenOpener.openSenderReceiver(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
