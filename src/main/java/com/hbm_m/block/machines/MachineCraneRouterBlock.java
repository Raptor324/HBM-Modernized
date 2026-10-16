package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.MachineCraneRouterBlockEntity;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Crane Router - Port von {@code CraneRouter} (1.7.10 Original). Kein FACING - passiver, ungerichteter
 * Sortier-Wuerfel (alle 6 Seiten funktional gleichwertig, konfiguriert ueber die GUI statt Platzierung).
 */
public class MachineCraneRouterBlock extends BaseEntityBlock implements com.hbm_m.block.network.IEnterableBlock {

    public MachineCraneRouterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineCraneRouterBlockEntity(pos, state);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof MenuProvider menuProvider) {
                MenuRegistry.openExtendedMenu((ServerPlayer) player, menuProvider, buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
        }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof MenuProvider menuProvider) {
                MenuRegistry.openExtendedMenu((ServerPlayer) player, menuProvider, buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
        }
    *///?}


    @Override
    public void onItemEnter(Level level, BlockPos pos, com.hbm_m.entity.conveyor.MovingConveyorItemEntity item) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MachineCraneRouterBlockEntity router) {
            router.onItemEnter(level, pos, item);
        }
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineCraneRouterBlock> CODEC = simpleCodec(MachineCraneRouterBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
