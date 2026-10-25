package com.hbm_m.api.energy;

import javax.annotation.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** 1:1 {@code CableSwitch} (Hand) und {@code CableDetector} (Redstone): Kabelbloecke, die das Netz trennen. */
public final class CableSwitchBlocks {

    private CableSwitchBlocks() {}

    public static final BooleanProperty ON = BooleanProperty.create("on");

    public abstract static class Base extends BaseEntityBlock {
        protected Base(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(ON, false));
        }

        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(ON); }
        @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

        @Nullable @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CableSwitchBlockEntity(pos, state); }

        @Nullable @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.CABLE_SWITCH.get(), CableSwitchBlockEntity::tick);
        }

        protected void set(Level world, BlockPos pos, BlockState state, boolean on) {
            world.setBlock(pos, state.setValue(ON, on), 2);
            world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), HbmSoundsNT.get("block.reactorStart"), SoundSource.BLOCKS, 1.0F, on ? 1.0F : 0.85F);
            if (world.getBlockEntity(pos) instanceof CableSwitchBlockEntity te) te.updateState();
        }
    }

    /** 1:1 {@code CableSwitch}: Rechtsklick (nicht schleichend) schaltet um. */
    public static class Switch extends Base {
        public Switch(Properties p) { super(p); }

        //? if < 1.21.1 {
        @Override
        public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return toggle(state, world, pos, player);
        }
        //?} else {
        /*@Override
        protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
            return toggle(state, world, pos, player);
        }
        public static final com.mojang.serialization.MapCodec<Switch> CODEC = simpleCodec(Switch::new);
        @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        *///?}

        private InteractionResult toggle(BlockState state, Level world, BlockPos pos, Player player) {
            if (world.isClientSide) return InteractionResult.SUCCESS;
            if (player.isShiftKeyDown()) return InteractionResult.PASS;
            set(world, pos, state, !state.getValue(ON));
            return InteractionResult.SUCCESS;
        }
    }

    /** 1:1 {@code CableDetector}: folgt dem Redstone-Signal. */
    public static class Detector extends Base {
        public Detector(Properties p) { super(p); }

        @Override
        public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos from, boolean moving) {
            if (world.isClientSide) return;
            boolean on = world.hasNeighborSignal(pos);
            if (on != state.getValue(ON)) set(world, pos, state, on);
        }

        //? if >1.20.1 {
        /*public static final com.mojang.serialization.MapCodec<Detector> CODEC = simpleCodec(Detector::new);
        @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        *///?}
    }
}
