package com.hbm_m.block.machines;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.LaunchpadSoyuzBlockEntity;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code LaunchpadSoyuz} (1.7.10): Multiblock-Startrampe fuer Soyuz-Raketen.
 */
public class LaunchpadSoyuzBlock extends DummyableMachineBlock {

    public LaunchpadSoyuzBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                .box(2, 0, 2, 2, 2, 2)
                .box(3, 0, 2, 1, 1, 2)
                .box(2, -2, 2, 2, -2, 10)
                .box(2, -2, 10, -2, 2, 10)
                .box(3, -3, 9, 1, 1, 9)
                .box(1, 0, 2, 2, -6, 10)
                .box(1, 0, 10, -6, 2, 2)
                .box(1, 0, 10, -6, -6, 10)
                .boxAt(2, 0, 0, 0, 0, 0, 0, -10, 58)
                .boxAt(2, 0, 0, 1, -1, 0, 0, -56, 58)
                .boxAt(-10, 0, 0, 0, 0, 0, 0, -10, 58)
                .boxAt(-10, 0, 0, 1, -1, 0, 0, -56, 58)
                .box(2, 0, 2, 1, 7, -3)
                .boxAt(-7, 0, 0, 2, 0, 2, 1, 7, -3)
                .boxAt(-4, 2, 0, 0, 0, 1, 1, 7, -3)
                .boxAt(-4, 2, 0, 1, -1, 5, 5, 7, -3)
                .boxAt(-4, 2, 0, 3, -2, 4, 4, 7, -3)
                .boxAt(-4, 2, 0, 6, -4, 3, 3, 7, -3)
                .boxAt(-4, 2, 0, 51, -7, 2, 2, 7, -3)
                .boxAt(0, 0, 5, 7, 0, -2, 3, 2, 2)
                .extra(2, 1, 1).extra(2, 1, -1).extra(2, 1, -7).extra(2, 1, -9)
                .extra(-10, 1, 1).extra(-10, 1, -1).extra(-10, 1, -7).extra(-10, 1, -9)
                .extra(1, 1, -10).extra(-1, 1, -10).extra(-7, 1, -10).extra(-9, 1, -10)
                .placementOffset(2)
                .build(() -> com.hbm_m.block.ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LaunchpadSoyuzBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return createTickerHelper(type, ModBlockEntities.LAUNCHPAD_SOYUZ_BE.get(), LaunchpadSoyuzBlockEntity::clientTick);
        return createTickerHelper(type, ModBlockEntities.LAUNCHPAD_SOYUZ_BE.get(), LaunchpadSoyuzBlockEntity::serverTick);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return open(level, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return open(level, pos, player);
    }
    *///?}

    private InteractionResult open(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide() && !player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof LaunchpadSoyuzBlockEntity pad) {
            com.hbm_m.handler.BossSpawnHandler.markFBI((ServerPlayer) player);
            MenuRegistry.openExtendedMenu((ServerPlayer) player, pad, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<LaunchpadSoyuzBlock> CODEC = simpleCodec(LaunchpadSoyuzBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
