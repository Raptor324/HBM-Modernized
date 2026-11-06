package com.hbm_m.blockentity.machines.pile;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of NEXT {@code com.hbm.tileentity.machine.pile.BlockEntityPileSource}: the BE of BOTH
 * CP-1 source blocks - the valid-block set of {@code PILE_SOURCE} is exactly
 * BLOCK_GRAPHITE_SOURCE + BLOCK_GRAPHITE_PLUTONIUM, and the block alone decides the flux:
 * block_graphite_source casts 12 rays of flux 1 per server tick, block_graphite_plutonium
 * (the bred fuel) 12 rays of flux 2. Stateless in NEXT - no NBT, no client sync. Does NOT
 * implement IPileNeutronReceiver: it only casts rays, streams pass through its node. The
 * queued streams drain via NeutronHandler on SERVER_POST (1-tick latency is load-bearing).
 */
public class BlockEntityPileSource extends BlockEntityPileBase {

    public BlockEntityPileSource(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PILE_SOURCE.get(), pos, state);
    }

    @Override
    public void tickServer() {

        int n = getBlockState().is(ModBlocks.BLOCK_GRAPHITE_SOURCE.get()) ? 1 : 2;

        for (int i = 0; i < 12; i++) this.castRay(n);
    }
}
