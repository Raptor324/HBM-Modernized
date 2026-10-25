package com.hbm_m.block.network;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.PneumaticPipeAnchorBlockEntity;
import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.inventory.fluid.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** 1:1 {@code PneumoPipeAnchor}: Rohranker fuer das pneumatische Rohrnetz, Reichweite 10 m. */
public class PneumaticPipeAnchorBlock extends PipeAnchorBlock {

    public PneumaticPipeAnchorBlock(Properties p) {
        super(p);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.PASS;
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
    }
    public static final com.mojang.serialization.MapCodec<PneumaticPipeAnchorBlock> CODEC = simpleCodec(PneumaticPipeAnchorBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}

    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics g, Level level, BlockPos pos) {
        ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, new ArrayList<>());
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PneumaticPipeAnchorBlockEntity(pos, state); }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.PIPE_ANCHOR_PNEUMATIC.get(), PneumaticPipeAnchorBlockEntity::serverTick);
    }
}
