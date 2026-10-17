package com.hbm_m.blockentity.machines.pile;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.pile.BlockGraphiteDrilledBase;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.config.GeneralConfig;
import com.hbm_m.interfaces.IPileNeutronReceiver;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of NEXT {@code com.hbm.tileentity.machine.pile.BlockEntityPileFuel}: the CP-1 uranium
 * fuel channel. tickServer runs in the exact NEXT order - heat dissipation, reaction (progress +
 * heat + 12 neutron rays), transmute (uranium breeds into PU-239 near depletion), overheat
 * (radon gas explosion), smoke particles, completion swap into the bred fuel block.
 *
 * <p>DEVIATION (historical): NEXT's completion swap calls {@code copyShape} directly. The swap
 * carries a {@code hasProperty(AXIS)} guard added while BLOCK_GRAPHITE_PLUTONIUM was still an
 * AXIS-less stub; the block_graphite_source port has since un-stubbed plutonium, so the guard
 * now takes the NEXT copyShape path (kept as a cheap defensive check). Balance dial is NEXT
 * {@code BalanceConfig.enable528} mapped onto {@link GeneralConfig#enable528}.</p>
 */
public class BlockEntityPileFuel extends BlockEntityPileBase implements IPileNeutronReceiver {

    public static final int maxHeat = 1000;
    public int heat;
    public int neutrons;
    public int lastNeutrons;
    public int progress;

    public BlockEntityPileFuel(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PILE_FUEL.get(), pos, state);
    }

    public static int maxProgress() {
        return GeneralConfig.enable528 ? 75000 : 50000;
    }

    @Override
    public void tickServer() {

        dissipateHeat();
        checkRedstone(react());
        transmute();

        if (this.heat >= maxHeat) {
            level.explode(
                    null,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5,
                    4F,
                    true,
                    Level.ExplosionInteraction.BLOCK);
            level.setBlockAndUpdate(
                    worldPosition, ModBlocks.GAS_RADON_DENSE.get().defaultBlockState());
        }

        if (level.getRandom().nextFloat() * 2F <= this.heat / (float) maxHeat) {

            ((ServerLevel) level)
                    .sendParticles(
                            ParticleTypes.SMOKE,
                            worldPosition.getX() + 0.25 + level.getRandom().nextDouble() * 0.5,
                            worldPosition.getY() + 1,
                            worldPosition.getZ() + 0.25 + level.getRandom().nextDouble() * 0.5,
                            0,
                            0D,
                            0.05D,
                            0D,
                            1D);
        }

        if (this.progress >= maxProgress()) {
            BlockState target = ModBlocks.BLOCK_GRAPHITE_PLUTONIUM.get().defaultBlockState();
            level.setBlockAndUpdate(
                    worldPosition,
                    // NEXT: BlockGraphiteDrilledBase.copyShape(...). The hasProperty guard keeps
                    // the server alive while the plutonium target is an AXIS-less stub.
                    target.hasProperty(BlockGraphiteDrilledBase.AXIS)
                            ? BlockGraphiteDrilledBase.copyShape(getBlockState(), target)
                            : target);
        }
    }

    // NEXT verbatim: int compound assignment keeps the truncation.
    private void dissipateHeat() {

        this.heat -=
                getBlockState().getValue(BlockGraphiteDrilledBase.SHROUDED)
                        ? heat * 0.065
                        : heat * 0.05;
    }

    private int react() {

        int reaction =
                (int) (this.neutrons * (1D - ((double) this.heat / (double) maxHeat) * 0.5D));

        this.lastNeutrons = this.neutrons;
        this.neutrons = 0;

        int lastProgress = this.progress;

        this.progress += reaction;

        if (reaction <= 0) return lastProgress;

        this.heat += reaction;

        for (int i = 0; i < 12; i++) this.castRay((int) Math.max(reaction * 0.25, 1));

        return lastProgress;
    }

    private void checkRedstone(int lastProgress) {
        int lastLevel = Mth.clamp((lastProgress * 16) / maxProgress(), 0, 15);
        int newLevel = Mth.clamp((progress * 16) / maxProgress(), 0, 15);
        if (lastLevel != newLevel)
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
    }

    private void transmute() {

        BlockState state = getBlockState();

        if (state.getValue(BlockGraphiteDrilledBase.PU239)) {
            if (this.progress < maxProgress() - 1000) this.progress = maxProgress() - 1000;
        } else if (this.progress >= maxProgress() - 1000) {
            level.setBlock(worldPosition, state.setValue(BlockGraphiteDrilledBase.PU239, true), 3);
        }
    }

    @Override
    public void receiveNeutrons(int n) {
        this.neutrons += n;
    }

    //? if < 1.21.1 {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("heat", this.heat);
        tag.putInt("progress", this.progress);
        tag.putInt("neutrons", this.neutrons);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        // NEXT reads with getIntOr(..., 0)
        this.heat = tag.contains("heat") ? tag.getInt("heat") : 0;
        this.progress = tag.contains("progress") ? tag.getInt("progress") : 0;
        this.neutrons = tag.contains("neutrons") ? tag.getInt("neutrons") : 0;
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("heat", this.heat);
        tag.putInt("progress", this.progress);
        tag.putInt("neutrons", this.neutrons);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // NEXT reads with getIntOr(..., 0)
        this.heat = tag.contains("heat") ? tag.getInt("heat") : 0;
        this.progress = tag.contains("progress") ? tag.getInt("progress") : 0;
        this.neutrons = tag.contains("neutrons") ? tag.getInt("neutrons") : 0;
    }
    *///?}
}
