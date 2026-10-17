package com.hbm_m.block.machines.pile;

import java.util.function.Supplier;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.pile.BlockEntityPileSource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Port of NEXT {@code com.hbm.blocks.machine.pile.BlockGraphiteSource}: the CP-1 neutron
 * source / bred-plutonium channel. One class serves BOTH blocks (NEXT ModBlocks.java:5052-5061)
 * - the inserted rod ({@code PILE_ROD_SOURCE} vs {@code PILE_ROD_PLUTONIUM}) is the only
 * difference and is carried as a {@link Supplier} just like in NEXT.
 *
 * <p>Carries its own {@link #getTicker} override on purpose: NEXT drives pile ticking via
 * {@code ITickingBlock}/{@code Reg.ticker} tables, while the Modernized base
 * {@link BlockGraphiteDrilledTE#getTicker} type-checks {@code PILE_NEUTRONDETECTOR} and returns
 * null for any other type - without this override the source would SILENTLY NEVER TICK (no
 * rays). Server side only, as in NEXT (the BE registers no client ticker).</p>
 */
public class BlockGraphiteSource extends BlockGraphiteDrilledTE {

    private final Supplier<Item> rod;

    public BlockGraphiteSource(Properties props, Supplier<Item> rod) {
        super(props);
        this.rod = rod;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityPileSource(pos, state);
    }

    @Override
    public Item getInsertedItem() {
        return rod.get();
    }

    /**
     * The base class' ticker type-guard only knows the detector; the source needs its own
     * mapping or the block silently never ticks (see class javadoc). One BE type serves both
     * blocks (source flux 1, plutonium flux 2 - decided in {@code BlockEntityPileSource}).
     */
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        if (type == ModBlockEntities.PILE_SOURCE.get()) {
            // Hand-written ticker check: createTickerHelper lives on BaseEntityBlock, these blocks extend Block.
            return (BlockEntityTicker<T>) (BlockEntityTicker<BlockEntityPileSource>) (lvl, pos, st, be) -> be.tickServer();
        }
        return null;
    }

    /**
     * NEXT lists the whole graphite family in {@code FlammableBlocks.register()} at 30/5
     * (FlammableBlocks.java:21-23, source and plutonium alike). The direct
     * {@code FireBlock#setFlammable} call is not portable - it is private on 1.20.1-forge - so
     * the family convention (BlockGraphiteFuel) overrides the vanilla fire query pair instead.
     */
    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return 30;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return 5;
    }
}
