package com.hbm_m.block.machines.pile;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.pile.PileLoaderBlockEntity;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Port von {@code BlockPileDevice} mit {@code BLOCK_META_LOADER} (1.7.10): die Ladevorrichtung.
 *
 * <p>Ein Rechtsklick mit einem Brennstab legt ihn ein, ein Rechtsklick mit leerer Hand stoesst ihn
 * in den Kanal. Der Handbohrer wirkt durch das Geraet hindurch auf den Kanal dahinter.</p>
 */
public class PileLoaderBlock extends PileDeviceBlock {

    public PileLoaderBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PileLoaderBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.PILE_LOADER_BE.get(),
                (lvl, pos, st, be) -> PileLoaderBlockEntity.tick(lvl, pos, st, (PileLoaderBlockEntity) be));
    }

    @Override
    protected InteractionResult interact(BlockState state, Level level, BlockPos pos,
                                         Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);

        if (held.getItem() == ModItems.HAND_DRILL.get() || held.getItem() == ModItems.HAND_DRILL_DESH.get()) {
            return passDrillThrough(state, level, pos, player, hand, false);
        }

        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (level.getBlockEntity(pos) instanceof PileLoaderBlockEntity loader) {
            loader.tryInsert(held);
        }
        return InteractionResult.CONSUME;
    }
}
