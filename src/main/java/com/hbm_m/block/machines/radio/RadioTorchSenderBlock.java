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
public class RadioTorchSenderBlock extends RadioTorchBaseBlock implements com.hbm_m.interfaces.ILookOverlay {

    public RadioTorchSenderBlock(Properties properties) { super(properties); }

    /**
     * Port of {@code RadioTorchRWBase.printHook} (the original Sender/Receiver parent class):
     * "Freq: <channel>" (aqua, only when set) and "Signal: <lastState>" (red).
     */
    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics guiGraphics, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof RadioTorchSenderBlockEntity radio)) return;
        java.util.List<net.minecraft.network.chat.Component> text = new java.util.ArrayList<>();
        if (radio.channel != null && !radio.channel.isEmpty()) {
            text.add(net.minecraft.network.chat.Component.literal("Freq: " + radio.channel)
                    .withStyle(net.minecraft.ChatFormatting.AQUA));
        }
        text.add(net.minecraft.network.chat.Component.literal("Signal: " + radio.lastState)
                .withStyle(net.minecraft.ChatFormatting.RED));
        com.hbm_m.interfaces.ILookOverlay.printGeneric(guiGraphics,
                net.minecraft.network.chat.Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadioTorchSenderBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.RADIO_TORCH_SENDER_BE.get(),
                (lvl, pos, st, be) -> RadioTorchSenderBlockEntity.tick(lvl, pos, st, (RadioTorchSenderBlockEntity) be));
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () ->
                    com.hbm_m.client.gui.radio.RadioTorchScreenOpener.openSenderReceiver(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () ->
                    com.hbm_m.client.gui.radio.RadioTorchScreenOpener.openSenderReceiver(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
    *///?}

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<RadioTorchSenderBlock> CODEC = simpleCodec(RadioTorchSenderBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
