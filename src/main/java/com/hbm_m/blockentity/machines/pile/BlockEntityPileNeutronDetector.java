package com.hbm_m.blockentity.machines.pile;

import com.hbm_m.block.machines.pile.BlockGraphiteDrilledBase;
import com.hbm_m.block.machines.pile.BlockGraphiteNeutronDetector;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IPileNeutronReceiver;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of NEXT {@code com.hbm.tileentity.machine.pile.BlockEntityPileNeutronDetector}: the
 * CP-1 detector's brain. Each server tick it compares the neutrons received this tick against
 * {@code maxNeutrons} and triggers the rod run (withdraw on flux rise, re-insert after a quiet
 * tick), then rolls the count over. NEXT is a plain BlockEntity with no client sync - nothing
 * here needs getUpdateTag/getUpdatePacket.
 */
public class BlockEntityPileNeutronDetector extends BlockEntity implements IPileNeutronReceiver {

    public int lastNeutrons;
    public int neutrons;
    public int maxNeutrons = 10;

    public BlockEntityPileNeutronDetector(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PILE_NEUTRONDETECTOR.get(), pos, state);
    }

    public void tickServer() {

        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof BlockGraphiteNeutronDetector detector)) return;
        boolean withdrawn = state.getValue(BlockGraphiteDrilledBase.WITHDRAWN);

        if (this.neutrons >= this.maxNeutrons && withdrawn) {
            detector.triggerRods(level, worldPosition);
        }
        if (this.neutrons < this.maxNeutrons
                && this.lastNeutrons < this.maxNeutrons
                && !withdrawn) {
            detector.triggerRods(level, worldPosition);
        }

        this.lastNeutrons = this.neutrons;
        this.neutrons = 0;
    }

    @Override
    public void receiveNeutrons(int n) {
        this.neutrons += n;
    }

    //? if < 1.21.1 {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("maxNeutrons", this.maxNeutrons);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        // NEXT reads with getIntOr("maxNeutrons", 10)
        this.maxNeutrons = tag.contains("maxNeutrons") ? tag.getInt("maxNeutrons") : 10;
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("maxNeutrons", this.maxNeutrons);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // NEXT reads with getIntOr("maxNeutrons", 10)
        this.maxNeutrons = tag.contains("maxNeutrons") ? tag.getInt("maxNeutrons") : 10;
    }
    *///?}
}
