package com.hbm_m.blockentity;

import com.hbm_m.api.fluids.IFluidStandardSenderMK2;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FluidTrait.FluidReleaseType;
import com.hbm_m.inventory.fluid.trait.PollutionType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code com.hbm.tileentity.TileEntityMachinePolluting} (1.7.10): die Basis aller
 * Maschinen, die ihr Abgas nicht sofort in die Welt blasen, sondern erst in drei Rauchtanks
 * fuellen und von dort ins Rohrnetz schieben.
 *
 * <p>Die eigentliche Tanklogik steht in {@link SmokeTankSet} - als eigene Klasse, weil nicht jede
 * rauchende Maschine des Ports unter diese Basis passt (siehe dortiger Kommentar).</p>
 */
public abstract class MachinePollutingBlockEntity extends BaseMachineBlockEntity
        implements IFluidStandardSenderMK2 {

    protected final SmokeTankSet smokeTanks;

    protected final FluidTank smoke;
    protected final FluidTank smokeLeaded;
    protected final FluidTank smokePoison;

    protected MachinePollutingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                          int inventorySize, long capacity, long maxReceive, long maxExtract,
                                          int smokeBuffer) {
        super(type, pos, state, inventorySize, capacity, maxReceive, maxExtract);
        this.smokeTanks = new SmokeTankSet(smokeBuffer);
        this.smoke = smokeTanks.smoke;
        this.smokeLeaded = smokeTanks.smokeLeaded;
        this.smokePoison = smokeTanks.smokePoison;
    }

    // ═══════════════════════════════ Ausstoss ═══════════════════════════════

    /** Original: {@code pollute(PollutionType, float)}. */
    public void pollute(PollutionType type, float amount) {
        smokeTanks.pollute(level, worldPosition, type, amount);
    }

    /** Original: {@code pollute(FluidType, FluidReleaseType, float)}. */
    public void pollute(Fluid fluid, FluidReleaseType release, float amount) {
        smokeTanks.pollute(level, worldPosition, fluid, release, amount);
    }

    // ═══════════════════════════ Abgabe ans Netz ═══════════════════════════

    /** Original: {@code sendSmoke} - bietet jeden gefuellten Rauchtank in die gegebene Richtung an. */
    public void sendSmoke(Level level, BlockPos pipePos, Direction dirFromMeToPipe) {
        for (FluidTank tank : getSmokeTanks()) {
            if (tank.getFill() > 0) {
                tryProvide(tank, level, pipePos, dirFromMeToPipe);
            }
        }
    }

    /** Bietet den Rauch in alle sechs Richtungen an - der uebliche Aufruf im Maschinentick. */
    public void sendSmokeAllDirections() {
        if (level == null || level.isClientSide()) return;

        for (Direction dir : Direction.values()) {
            sendSmoke(level, worldPosition.relative(dir), dir);
        }
    }

    public FluidTank[] getSmokeTanks() {
        return smokeTanks.tanks();
    }

    @Override
    public FluidTank[] getSendingTanks() {
        return getSmokeTanks();
    }

    /**
     * Vorgabe fuer Maschinen ohne eigene Fluidtanks. Wer welche hat, muss ueberschreiben und die
     * Rauchtanks mit auflisten - sonst sind sie fuer das Netz unsichtbar.
     */
    @Override
    public FluidTank[] getAllTanks() {
        return getSmokeTanks();
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    // ═══════════════════════════════ NBT ═══════════════════════════════

    @Override
    protected void writeNbtData(@NotNull CompoundTag tag, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        smokeTanks.writeToNBT(tag);
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag tag, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        smokeTanks.readFromNBT(tag);
    }
}
