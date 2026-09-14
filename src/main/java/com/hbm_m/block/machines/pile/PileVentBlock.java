package com.hbm_m.block.machines.pile;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.pile.PileVentBlockEntity;

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

/**
 * Port von {@code BlockPileDevice} mit {@code BLOCK_META_VENT} (1.7.10): das Geblaese.
 *
 * <p>Es hat keine eigene Bedienung - man schliesst nur Druckluft an. Der Handbohrer wirkt durch das
 * Geraet hindurch auf den Lueftungskanal dahinter.</p>
 */
public class PileVentBlock extends PileDeviceBlock {

    public PileVentBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PileVentBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.PILE_VENT_BE.get(),
                (lvl, pos, st, be) -> PileVentBlockEntity.tick(lvl, pos, st, (PileVentBlockEntity) be));
    }

    @Override
    protected InteractionResult interact(BlockState state, Level level, BlockPos pos,
                                         Player player, InteractionHand hand, BlockHitResult hit) {
        return passDrillThrough(state, level, pos, player, hand, false);
    }
}
