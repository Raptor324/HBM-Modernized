package com.hbm_m.block.machines.radio;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.radio.RadioTorchSenderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Port of {@code RadioTorchSender} (1.7.10 Original). */
public class RadioTorchSenderBlock extends RadioTorchBaseBlock {

    public RadioTorchSenderBlock(Properties properties) { super(properties); }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadioTorchSenderBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.RADIO_TORCH_SENDER_BE.get(),
                (lvl, pos, st, be) -> RadioTorchSenderBlockEntity.tick(lvl, pos, st, (RadioTorchSenderBlockEntity) be));
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
