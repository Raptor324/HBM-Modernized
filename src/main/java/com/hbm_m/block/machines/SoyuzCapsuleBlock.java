package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.machines.SoyuzCapsuleBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code SoyuzCapsule}: gelandete Frachtkapsel mit 19 Plaetzen (18 Fracht + die Rakete). Wird per Rechtsklick
 * geoeffnet (nicht geschlichen) und verstreut beim Abbau ihren Inhalt. {@code RUSTED} entspricht Meta 3 der
 * Strandkapseln aus der Weltgenerierung (verrostete Textur).
 */
public class SoyuzCapsuleBlock extends BaseEntityBlock {

    public static final BooleanProperty RUSTED = BooleanProperty.create("rusted");

    public SoyuzCapsuleBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(RUSTED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RUSTED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SoyuzCapsuleBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        } else if (!player.isShiftKeyDown()) {
            if (world.getBlockEntity(pos) instanceof SoyuzCapsuleBlockEntity capsule) player.openMenu(capsule);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            if (world.getBlockEntity(pos) instanceof SoyuzCapsuleBlockEntity capsule) {
                Containers.dropContents(world, pos, capsule);
                world.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, world, pos, newState, moving);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<SoyuzCapsuleBlock> CODEC = simpleCodec(SoyuzCapsuleBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
