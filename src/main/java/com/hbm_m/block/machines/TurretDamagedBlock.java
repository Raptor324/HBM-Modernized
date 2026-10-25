package com.hbm_m.block.machines;

import java.util.function.Supplier;

import com.hbm_m.blockentity.machines.TurretBaseBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code TurretSentryDamaged} (Einzelblock; Howard beschaedigt ist {@code TurretMultiblockBlock.SmallDamaged}): ohne GUI
 * ({@code onBlockActivated} -> false) und ohne Drop ({@code getItemDropped} -> null, Loot-Tabelle leer).
 */
public class TurretDamagedBlock extends TurretBlock {

    public TurretDamagedBlock(Properties properties, Supplier<BlockEntityType<TurretBaseBlockEntity>> beType) {
        super(properties, beType);
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
    *///?}
}
