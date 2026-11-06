package com.hbm_m.block.machines.pile;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.pile.BlockEntityPileNeutronDetector;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of NEXT {@code com.hbm.blocks.machine.pile.BlockGraphiteDrilledTE}: marker subclass
 * joining the pile base with server ticking. NEXT routes this through {@code ITickingBlock}
 * (which extends EntityBlock)/{@code Reg.ticker}; Modernized wires block tickers directly, so
 * this class implements {@link EntityBlock} (NEXT's ITickingBlock shape - 1.20.1 Block does
 * NOT implement EntityBlock on its own) and supplies the {@code getTicker} override for the
 * detector block entity (server side only, as in NEXT - the detector BE registers no client
 * ticker).
 */
public abstract class BlockGraphiteDrilledTE extends BlockGraphiteDrilledBase implements EntityBlock {

    protected BlockGraphiteDrilledTE(Properties props) {
        super(props);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        if (type == ModBlockEntities.PILE_NEUTRONDETECTOR.get()) {
            // Hand-written ticker check: createTickerHelper lives on BaseEntityBlock, these blocks extend Block.
            return (BlockEntityTicker<T>) (BlockEntityTicker<BlockEntityPileNeutronDetector>) (lvl, pos, st, be) -> be.tickServer();
        }
        return null;
    }
}
