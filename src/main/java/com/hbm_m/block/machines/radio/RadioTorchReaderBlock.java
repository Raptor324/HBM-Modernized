package com.hbm_m.block.machines.radio;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.radio.RadioTorchReaderBlockEntity;
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

/** Port of {@code RadioTorchReader} (1.7.10 Original). */
public class RadioTorchReaderBlock extends RadioTorchBaseBlock {

    public RadioTorchReaderBlock(Properties properties) { super(properties); }

    /** Original {@code canBlockStay}: haelt nur an einem Funk-Wertgeber ({@code IRORValueProvider}). */
    @Override
    protected boolean canBlockStay(net.minecraft.world.level.LevelReader level, BlockPos pos, net.minecraft.core.Direction facing) {
        return com.hbm_m.api.redstoneoverradio.IRORInfo.resolve(level, pos.relative(facing)) instanceof com.hbm_m.api.redstoneoverradio.IRORValueProvider;
    }

    /** Original {@code printHook}: alle belegten Kanaele mit ihrem Wertnamen. */
    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics g, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof RadioTorchReaderBlockEntity radio)) return;
        java.util.List<net.minecraft.network.chat.Component> text = new java.util.ArrayList<>();
        for (int i = 0; i < 8; i++) {
            if (radio.channels[i] == null || radio.channels[i].isEmpty()) continue;
            if (radio.names[i] == null || radio.names[i].isEmpty()) continue;
            text.add(net.minecraft.network.chat.Component.literal(radio.channels[i] + ": " + radio.names[i]).withStyle(net.minecraft.ChatFormatting.AQUA));
        }
        com.hbm_m.interfaces.ILookOverlay.printGeneric(g, getName(), 0xffff00, 0x404000, text);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadioTorchReaderBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.RADIO_TORCH_READER_BE.get(),
                (lvl, pos, st, be) -> RadioTorchReaderBlockEntity.tick(lvl, pos, st, (RadioTorchReaderBlockEntity) be));
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS; // Original: geschlichen false
        if (level.isClientSide()) {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () ->
                    com.hbm_m.client.gui.radio.RadioTorchScreenOpener.openReader(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS; // Original: geschlichen false
        if (level.isClientSide()) {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () ->
                    com.hbm_m.client.gui.radio.RadioTorchScreenOpener.openReader(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
    *///?}

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<RadioTorchReaderBlock> CODEC = simpleCodec(RadioTorchReaderBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
