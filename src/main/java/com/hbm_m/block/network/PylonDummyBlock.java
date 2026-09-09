package com.hbm_m.block.network;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.network.PylonDummyBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Невидимая часть корпуса пилона (аналог dummy-блоков BlockDummyable из 1.7.10):
 * полный блок коллизии, без модели. При разрушении рушит ядро пилона.
 */
public class PylonDummyBlock extends Block implements EntityBlock {

    public PylonDummyBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(0, 0, 0, 16, 16, 16);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PylonDummyBlockEntity(pos, state);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);
        // Inside an assembly move window the dummy is being carried, not broken: taking the core
        // down here would delete the pylon the engine is about to put back.
        if (!level.isClientSide && newState.getBlock() != this && !com.hbm_m.multiblock.ContraptionAssemblyGuard.isMoving()) {
            breakCore(level, pos, false);
        }
    }

    // Upstream BlockDummyable.onBlockHarvested drops the machine whenever a non-creative player
    // breaks any dummy block; breakCore's hardcoded drop=false lost the pylon entirely.
    //? if < 1.21.1 {
    /*@Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !com.hbm_m.multiblock.ContraptionAssemblyGuard.isMoving()) {
            breakCore(level, pos, !player.getAbilities().instabuild);
        }
        super.playerWillDestroy(level, pos, state, player);
    }
    *///?} else {
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !com.hbm_m.multiblock.ContraptionAssemblyGuard.isMoving()) {
            breakCore(level, pos, !player.getAbilities().instabuild);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
    //?}

    /** При разрушении части — рушим ядро (оно в свою очередь убирает остальные части). */
    private static void breakCore(Level level, BlockPos pos, boolean drop) {
        BlockPos core = findCore(level, pos);
        if (core != null) {
            level.destroyBlock(core, drop);
        }
    }

    /** Ищет ядро по сохранённой позиции, иначе — сканирует вниз по колонне. */
    @Nullable
    public static BlockPos findCore(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof PylonDummyBlockEntity dummy && dummy.getCorePos() != null) {
            BlockState core = level.getBlockState(dummy.getCorePos());
            if (core.getBlock() instanceof RedPylonCoreBlock) {
                return dummy.getCorePos();
            }
        }
        BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int i = 0; i < 20; i++) {
            cursor.move(net.minecraft.core.Direction.DOWN);
            if (level.getBlockState(cursor).getBlock() instanceof RedPylonCoreBlock) {
                return cursor.immutable();
            }
        }
        return null;
    }
}
