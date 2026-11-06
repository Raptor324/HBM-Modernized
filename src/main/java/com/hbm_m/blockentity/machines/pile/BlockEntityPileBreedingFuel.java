package com.hbm_m.blockentity.machines.pile;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.pile.BlockGraphiteDrilledBase;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.config.GeneralConfig;
import com.hbm_m.interfaces.IPileNeutronReceiver;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of NEXT {@code com.hbm.tileentity.machine.pile.BlockEntityPileBreedingFuel}: the CP-1
 * lithium breeding channel. tickServer runs react() first (consumes the received flux into
 * progress and, when hit, casts two flux-1 rays), then swaps the channel into the bred
 * BLOCK_GRAPHITE_TRITIUM at maxProgress. lastNeutrons is deliberately NOT persisted (NEXT
 * verbatim), so the hand-drill FLUX readout reads 0 until the BE is hit again after reload.
 *
 * <p>DEVIATION (stub-scope): NEXT's completion swap calls {@code copyShape} directly, which
 * would throw on the still-stub BLOCK_GRAPHITE_TRITIUM (a plain Block without AXIS); the swap
 * is written {@code hasProperty(AXIS)}-guarded so it auto-upgrades to copyShape once tritium
 * is properly ported (same pattern as {@link BlockEntityPileFuel}). Balance dial is NEXT
 * {@code BalanceConfig.enable528} mapped onto {@link GeneralConfig#enable528}.</p>
 */
public class BlockEntityPileBreedingFuel extends BlockEntityPileBase
        implements IPileNeutronReceiver {

    public int neutrons;
    public int lastNeutrons;
    public int progress;

    public BlockEntityPileBreedingFuel(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PILE_BREEDINGFUEL.get(), pos, state);
    }

    public static int maxProgress() {
        return GeneralConfig.enable528 ? 50000 : 30000;
    }

    @Override
    public void tickServer() {
        react();

        if (this.progress >= maxProgress()) {
            BlockState target = ModBlocks.BLOCK_GRAPHITE_TRITIUM.get().defaultBlockState();
            level.setBlockAndUpdate(
                    worldPosition,
                    // NEXT: BlockGraphiteDrilledBase.copyShape(getBlockState(), target). The
                    // hasProperty guard keeps the server alive while the tritium target is an
                    // AXIS-less stub.
                    target.hasProperty(BlockGraphiteDrilledBase.AXIS)
                            ? BlockGraphiteDrilledBase.copyShape(getBlockState(), target)
                            : target);
        }
    }

    private void react() {

        this.lastNeutrons = this.neutrons;
        this.progress += this.neutrons;

        this.neutrons = 0;

        if (lastNeutrons <= 0) return;

        for (int i = 0; i < 2; i++) this.castRay(1);
    }

    @Override
    public void receiveNeutrons(int n) {
        this.neutrons += n;
    }

    //? if < 1.21.1 {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("progress", this.progress);
        tag.putInt("neutrons", this.neutrons);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        // NEXT reads with getIntOr(..., 0)
        this.progress = tag.contains("progress") ? tag.getInt("progress") : 0;
        this.neutrons = tag.contains("neutrons") ? tag.getInt("neutrons") : 0;
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("progress", this.progress);
        tag.putInt("neutrons", this.neutrons);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // NEXT reads with getIntOr(..., 0)
        this.progress = tag.contains("progress") ? tag.getInt("progress") : 0;
        this.neutrons = tag.contains("neutrons") ? tag.getInt("neutrons") : 0;
    }
    *///?}
}
