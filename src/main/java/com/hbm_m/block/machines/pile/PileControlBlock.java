package com.hbm_m.block.machines.pile;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.pile.PileControlBlockEntity;

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
 * Port von {@code BlockPileDevice} mit {@code BLOCK_META_CONTROL} (1.7.10): der
 * Steuerstabantrieb.
 *
 * <p>Er gehoert oben auf einen Steuerkanal. Redstone an seiner Vorderseite zieht den Stab, das
 * Wegfallen des Signals faehrt ihn ein. Der Handbohrer wirkt durch ihn hindurch nach unten auf den
 * Kanal.</p>
 */
public class PileControlBlock extends PileDeviceBlock {

    public PileControlBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PileControlBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.PILE_CONTROL_BE.get(),
                (lvl, pos, st, be) -> PileControlBlockEntity.tick(lvl, pos, st, (PileControlBlockEntity) be));
    }

    @Override
    protected InteractionResult interact(BlockState state, Level level, BlockPos pos,
                                         Player player, InteractionHand hand, BlockHitResult hit) {
        // Original: der Bohrer greift beim Steuerantrieb senkrecht nach unten durch.
        return passDrillThrough(state, level, pos, player, hand, true);
    }
}
