package com.hbm_m.blockentity.machines.albion;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * 1:1-Port von {@code TileEntityCooledBase} (1.7.10): die gemeinsame Basis aller Bauteile des
 * Teilchenbeschleunigers.
 *
 * <p>Jedes Bauteil heizt sich pro Tick um {@link #TEMP_PASSIVE_HEATING} auf und kuehlt sich mit
 * kaltem Perfluormethyl herunter - ein Millibucket senkt die Temperatur um
 * {@link #TEMP_CHANGE_PER_MB}. Erst unterhalb von {@link #TEMPERATURE_TARGET} gilt das Bauteil als
 * betriebsbereit; ein Teilchen, das ein zu warmes Bauteil erreicht, stuerzt ab.</p>
 */
public abstract class CooledMachineBlockEntity extends BaseMachineBlockEntity
        implements IFluidStandardTransceiverMK2 {

    public static final float KELVIN = 273F;
    /** Original: {@code temperature_target = KELVIN - 150F}. */
    public static final float TEMPERATURE_TARGET = KELVIN - 150F;
    /** Original: {@code temp_change_per_mb = 0.5F}. */
    public static final float TEMP_CHANGE_PER_MB = 0.5F;
    /** Original: {@code temp_passive_heating = 2.5F}. */
    public static final float TEMP_PASSIVE_HEATING = 2.5F;
    /** Original: {@code temp_change_max = 5F + temp_passive_heating}. */
    public static final float TEMP_CHANGE_MAX = 5F + TEMP_PASSIVE_HEATING;
    /** Original: Ausgangstemperatur ist Raumtemperatur. */
    public static final float TEMPERATURE_AMBIENT = KELVIN + 20F;

    private static final int COOLANT_CAPACITY_MB = 4_000;

    /** 0 = kaltes Perfluormethyl (Eingang), 1 = erwaermtes (Ausgang). */
    protected final FluidTank[] coolantTanks = new FluidTank[2];

    protected float temperature = TEMPERATURE_AMBIENT;

    protected CooledMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                       int inventorySize, long capacity, long maxReceive) {
        super(type, pos, state, inventorySize, capacity, maxReceive, 0L);
        coolantTanks[0] = new FluidTank(ModFluids.PERFLUOROMETHYL_COLD.getSource(), COOLANT_CAPACITY_MB);
        coolantTanks[1] = new FluidTank(ModFluids.PERFLUOROMETHYL.getSource(), COOLANT_CAPACITY_MB);
    }

    /**
     * Original: der Rumpf von {@code TileEntityCooledBase.updateEntity} - Kuehlmittel anfordern,
     * warmes abgeben, Temperatur nachfuehren.
     */
    protected void tickCooling(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            trySubscribe(coolantTanks[0].getTankType(), level, neighbor, dir);
            tryProvide(coolantTanks[1], level, neighbor, dir);
        }

        temperature += TEMP_PASSIVE_HEATING;
        if (temperature > TEMPERATURE_AMBIENT) temperature = TEMPERATURE_AMBIENT;

        if (temperature > TEMPERATURE_TARGET) {
            int cyclesTemp = (int) Math.ceil(
                    Math.min(temperature - TEMPERATURE_TARGET, TEMP_CHANGE_MAX) / TEMP_CHANGE_PER_MB);
            int cyclesCool = coolantTanks[0].getFill();
            int cyclesHot = coolantTanks[1].getMaxFill() - coolantTanks[1].getFill();
            int cycles = Math.min(cyclesTemp, Math.min(cyclesCool, cyclesHot));

            if (cycles > 0) {
                coolantTanks[0].drainMb(cycles);
                coolantTanks[1].fillMb(ModFluids.PERFLUOROMETHYL.getSource(), cycles);
                temperature -= TEMP_CHANGE_PER_MB * cycles;
            }
        }
    }

    /** Original: {@code isCool()} - erst unterhalb der Zieltemperatur laeuft das Bauteil. */
    public boolean isCool() {
        return temperature <= TEMPERATURE_TARGET;
    }

    public float getTemperature() {
        return temperature;
    }

    public FluidTank[] getCoolantTanks() {
        return coolantTanks;
    }

    // ── IFluidStandardTransceiverMK2 ────────────────────────────────────────

    @Override
    public FluidTank[] getAllTanks() {
        return coolantTanks;
    }

    @Override
    public FluidTank[] getReceivingTanks() {
        return new FluidTank[] { coolantTanks[0] };
    }

    @Override
    public FluidTank[] getSendingTanks() {
        return new FluidTank[] { coolantTanks[1] };
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        if (fromDir == null || fluid == null || fluid == Fluids.EMPTY) return false;
        return VanillaFluidEquivalence.sameSubstance(fluid, ModFluids.PERFLUOROMETHYL_COLD.getSource())
                || VanillaFluidEquivalence.sameSubstance(fluid, ModFluids.PERFLUOROMETHYL.getSource());
    }

    // ── NBT ─────────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putFloat("temperature", temperature);
        coolantTanks[0].writeToNBT(tag, "coolantCold");
        coolantTanks[1].writeToNBT(tag, "coolantHot");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        temperature = tag.contains("temperature") ? tag.getFloat("temperature") : TEMPERATURE_AMBIENT;
        coolantTanks[0].readFromNBT(tag, "coolantCold");
        coolantTanks[1].readFromNBT(tag, "coolantHot");
    }
}
