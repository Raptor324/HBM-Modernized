package com.hbm_m.block.network;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.RadioAutocalBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Port of {@code RadioAUTOCAL} (1.7.10 Original). */
public class RadioAutocalBlock extends BaseEntityBlock {

    public RadioAutocalBlock(Properties properties) { super(properties); }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadioAutocalBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.RADIO_AUTOCAL_BE.get(),
                (lvl, pos, st, be) -> RadioAutocalBlockEntity.tick(lvl, pos, st, (RadioAutocalBlockEntity) be));
    }

    @Override
    public boolean isSignalSource(BlockState state) { return true; }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        if (level.getBlockEntity(pos) instanceof RadioAutocalBlockEntity be) {
            return be.getRedstoneOutput(side.ordinal());
        }
        return 0;
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

        if (level.isClientSide()) {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () ->
                    com.hbm_m.client.gui.radio.RadioTorchScreenOpener.openRadioAutocal(pos));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
        }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (level.isClientSide()) {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () ->
                    com.hbm_m.client.gui.radio.RadioTorchScreenOpener.openRadioAutocal(pos));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
        }
    *///?}


    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<RadioAutocalBlock> CODEC = simpleCodec(RadioAutocalBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
