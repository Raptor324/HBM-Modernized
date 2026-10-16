package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.MachineDroneDockBlockEntity;

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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Drone Dock - Port von {@code DroneDock} (1.7.10 Original, siehe Klassenkommentar an {@link MachineDroneProviderBlock}). */
public class MachineDroneDockBlock extends BaseEntityBlock {

    public MachineDroneDockBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineDroneDockBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.DRONE_DOCK_BE.get(),
                (lvl, pos, st, be) -> MachineDroneDockBlockEntity.tick(lvl, pos, st, (MachineDroneDockBlockEntity) be));
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


    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineDroneDockBlock> CODEC = simpleCodec(MachineDroneDockBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
