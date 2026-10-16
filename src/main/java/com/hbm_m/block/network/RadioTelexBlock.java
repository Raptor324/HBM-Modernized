package com.hbm_m.block.network;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.RadioTelexBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code RadioTelex} (1.7.10): der Fernschreiber am Funknetz.
 *
 * <p>Er belegt wie im Original zwei Felder nebeneinander ({@code getDimensions {0,0,0,0,1,0}}) -
 * der Kern und eine Dummyzelle nach Westen.</p>
 *
 * <p><b>Nicht portiert:</b> die OpenComputers-Komponente ({@code ntm_telex}) - dafuer gibt es in
 * diesem Port an keiner Stelle eine Anbindung.</p>
 */
public class RadioTelexBlock extends DummyableMachineBlock {

    public RadioTelexBlock(Properties properties) { super(properties); }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        // Original: getDimensions {0,0,0,0,1,0}, getOffset 0, keine Zusatzzellen.
        return DummyableStructureBuilder.create()
                .box(0, 0, 0, 0, 1, 0)
                .placementOffset(0)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadioTelexBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.RADIO_TELEX_BE.get(),
                (lvl, pos, st, be) -> RadioTelexBlockEntity.tick(lvl, pos, st, (RadioTelexBlockEntity) be));
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

        if (level.isClientSide()) {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () ->
                    com.hbm_m.client.gui.radio.RadioTorchScreenOpener.openRadioTelex(pos));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
        }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (level.isClientSide()) {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () ->
                    com.hbm_m.client.gui.radio.RadioTorchScreenOpener.openRadioTelex(pos));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
        }
    *///?}


    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<RadioTelexBlock> CODEC = simpleCodec(RadioTelexBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
