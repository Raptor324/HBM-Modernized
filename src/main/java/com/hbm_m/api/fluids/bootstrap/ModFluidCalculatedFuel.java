package com.hbm_m.api.fluids.bootstrap;

import com.hbm_m.api.fluids.ModFluids;
import com.hbm_m.inventory.fluid.trait.FT_Combustible;
import com.hbm_m.inventory.fluid.trait.FT_Combustible.FuelGrade;
import com.hbm_m.inventory.fluid.trait.FT_Flammable;
import com.hbm_m.inventory.fluid.trait.FluidTraitManager;

import net.minecraft.world.level.material.Fluid;

/**
 * Ports 1.7.10 {@code Fluids.init()} spreadsheet ({@code registerCalculatedFuel}) for tooltip energies.
 */
public final class ModFluidCalculatedFuel {

    private static final long BASELINE = 100_000L;

    private ModFluidCalculatedFuel() {}

    private static long round(long l) {
        if (l > 10_000_000L) return l - (l % 100_000L);
        if (l > 1_000_000L) return l - (l % 10_000L);
        if (l > 100_000L) return l - (l % 1_000L);
        if (l > 10_000L) return l - (l % 100L);
        if (l > 1_000L) return l - (l % 10L);
        return l;
    }

    private static void reg(Fluid fluid, double base, double combustMult, FuelGrade grade) {
        long flammable = round((long) base);
        long combustible = round((long) (base * combustMult));
        FluidTraitManager.addTrait(fluid, new FT_Flammable(flammable));
        if (combustible > 0 && grade != null) {
            FluidTraitManager.addTrait(fluid, new FT_Combustible(grade, combustible));
        }
    }

    public static void apply() {
        double demandVeryLow = 0.5D;
        double demandLow = 1.0D;
        double demandMedium = 1.5D;
        double demandHigh = 2.0D;
        double complexityRefinery = 1.1D;
        double complexityFraction = 1.05D;
        double complexityCracking = 1.25D;
        double complexityCoker = 1.25D;
        double complexityChemplant = 1.1D;
        double complexityLubed = 1.15D;
        double complexityLeaded = 1.5D;
        double complexityVacuum = 3.0D;
        double complexityReform = 2.5D;
        double complexityHydro = 2.0D;
        double flammabilityLow = 0.25D;
        double flammabilityNormal = 1.0D;
        double flammabilityHigh = 2.0D;

        reg(ModFluids.CRUDE_OIL.getSource(), BASELINE / 1D * flammabilityLow * demandLow, 0, null);
        reg(ModFluids.OIL_DS.getSource(), BASELINE / 1D * flammabilityLow * demandLow * complexityHydro, 0, null);
        reg(ModFluids.CRACKOIL.getSource(), BASELINE / 1D * flammabilityLow * demandLow * complexityCracking, 0, null);
        reg(ModFluids.CRACKOIL_DS.getSource(), BASELINE / 1D * flammabilityLow * demandLow * complexityCracking * complexityHydro, 0, null);
        reg(ModFluids.OIL_COKER.getSource(), BASELINE / 1D * flammabilityLow * demandLow * complexityCoker, 0, null);
        reg(ModFluids.GAS.getSource(), BASELINE / 1D * flammabilityNormal * demandVeryLow, 1.5, FuelGrade.GAS);
        reg(ModFluids.GAS_COKER.getSource(), BASELINE / 1D * flammabilityNormal * demandVeryLow * complexityCoker, 1.5, FuelGrade.GAS);
        reg(ModFluids.HEAVYOIL.getSource(), BASELINE / 0.5 * flammabilityLow * demandLow * complexityRefinery, 1.25D, FuelGrade.LOW);
        reg(ModFluids.SMEAR.getSource(), BASELINE / 0.35 * flammabilityLow * demandLow * complexityRefinery * complexityFraction, 1.25D, FuelGrade.LOW);
        reg(ModFluids.RECLAIMED.getSource(), BASELINE / 0.28 * flammabilityLow * demandLow * complexityRefinery * complexityFraction * complexityChemplant, 1.25D, FuelGrade.LOW);
        reg(ModFluids.PETROIL.getSource(), BASELINE / 0.28 * flammabilityLow * demandLow * complexityRefinery * complexityFraction * complexityChemplant * complexityLubed, 1.5D, FuelGrade.MEDIUM);
        reg(ModFluids.PETROIL_LEADED.getSource(), BASELINE / 0.28 * flammabilityLow * demandLow * complexityRefinery * complexityFraction * complexityChemplant * complexityLubed * complexityLeaded, 1.5D, FuelGrade.MEDIUM);
        reg(ModFluids.HEATINGOIL.getSource(), BASELINE / 0.31 * flammabilityNormal * demandLow * complexityRefinery * complexityFraction * complexityFraction, 1.25D, FuelGrade.LOW);
        reg(ModFluids.NAPHTHA.getSource(), BASELINE / 0.25 * flammabilityLow * demandLow * complexityRefinery, 1.5D, FuelGrade.MEDIUM);
        reg(ModFluids.NAPHTHA_DS.getSource(), BASELINE / 0.25 * flammabilityLow * demandLow * complexityRefinery * complexityHydro, 1.5D, FuelGrade.MEDIUM);
        reg(ModFluids.NAPHTHA_CRACK.getSource(), BASELINE / 0.40 * flammabilityLow * demandLow * complexityRefinery * complexityCracking, 1.5D, FuelGrade.MEDIUM);
        reg(ModFluids.NAPHTHA_COKER.getSource(), BASELINE / 0.25 * flammabilityLow * demandLow * complexityCoker, 1.5D, FuelGrade.MEDIUM);
        reg(ModFluids.GASOLINE.getSource(), BASELINE / 0.20 * flammabilityNormal * demandLow * complexityRefinery * complexityChemplant, 2.5D, FuelGrade.HIGH);
        reg(ModFluids.GASOLINE_LEADED.getSource(), BASELINE / 0.20 * flammabilityNormal * demandLow * complexityRefinery * complexityChemplant * complexityLeaded, 2.5D, FuelGrade.HIGH);
        reg(ModFluids.DIESEL.getSource(), BASELINE / 0.21 * flammabilityNormal * demandLow * complexityRefinery * complexityFraction, 2.5D, FuelGrade.HIGH);
        reg(ModFluids.DIESEL_CRACK.getSource(), BASELINE / 0.28 * flammabilityNormal * demandLow * complexityRefinery * complexityCracking * complexityFraction, 2.5D, FuelGrade.HIGH);
        reg(ModFluids.LIGHTOIL.getSource(), BASELINE / 0.15 * flammabilityNormal * demandHigh * complexityRefinery, 1.5D, FuelGrade.MEDIUM);
        reg(ModFluids.LIGHTOIL_DS.getSource(), BASELINE / 0.15 * flammabilityNormal * demandHigh * complexityRefinery * complexityHydro, 1.5D, FuelGrade.MEDIUM);
        reg(ModFluids.LIGHTOIL_CRACK.getSource(), BASELINE / 0.30 * flammabilityNormal * demandHigh * complexityRefinery * complexityCracking, 1.5D, FuelGrade.MEDIUM);
        reg(ModFluids.KEROSENE.getSource(), BASELINE / 0.09 * flammabilityNormal * demandHigh * complexityRefinery * complexityFraction, 1.5D, FuelGrade.AERO);
        reg(ModFluids.PETROLEUM.getSource(), BASELINE / 0.10 * flammabilityNormal * demandMedium * complexityRefinery, 1.5, FuelGrade.GAS);
        reg(ModFluids.AROMATICS.getSource(), BASELINE / 0.15 * flammabilityLow * demandHigh * complexityRefinery * complexityCracking, 0, null);
        reg(ModFluids.UNSATURATEDS.getSource(), BASELINE / 0.15 * flammabilityHigh * demandHigh * complexityRefinery * complexityCracking, 0, null);
        reg(ModFluids.LPG.getSource(), BASELINE / 0.1 * flammabilityNormal * demandMedium * complexityRefinery * complexityChemplant, 2.5, FuelGrade.HIGH);

        FT_Flammable keroseneFlam = FluidTraitManager.getTrait(ModFluids.KEROSENE.getSource(), FT_Flammable.class);
        long keroseneHeat = keroseneFlam != null ? keroseneFlam.getHeatEnergy() : 0L;
        reg(ModFluids.NITAN.getSource(), keroseneHeat * 25L, 2.5, FuelGrade.HIGH);
        reg(ModFluids.BALEFIRE.getSource(), keroseneHeat * 100L, 2.5, FuelGrade.HIGH);

        reg(ModFluids.HEAVYOIL_VACUUM.getSource(), BASELINE / 0.4 * flammabilityLow * demandLow * complexityVacuum, 1.25D, FuelGrade.LOW);
        reg(ModFluids.REFORMATE.getSource(), BASELINE / 0.25 * flammabilityNormal * demandHigh * complexityVacuum, 2.5D, FuelGrade.HIGH);
        reg(ModFluids.LIGHTOIL_VACUUM.getSource(), BASELINE / 0.20 * flammabilityNormal * demandHigh * complexityVacuum, 1.5D, FuelGrade.MEDIUM);
        reg(ModFluids.SOURGAS.getSource(), BASELINE / 0.15 * flammabilityLow * demandVeryLow * complexityVacuum, 0, null);
        reg(ModFluids.XYLENE.getSource(), BASELINE / 0.15 * flammabilityNormal * demandMedium * complexityVacuum * complexityFraction, 2.5D, FuelGrade.HIGH);
        reg(ModFluids.HEATINGOIL_VACUUM.getSource(), BASELINE / 0.24 * flammabilityNormal * demandLow * complexityVacuum * complexityFraction, 1.25D, FuelGrade.LOW);

        FT_Flammable dieselFlam = FluidTraitManager.getTrait(ModFluids.DIESEL.getSource(), FT_Flammable.class);
        long dieselHeat = dieselFlam != null ? dieselFlam.getHeatEnergy() : 0L;
        FT_Flammable dieselCrackFlam = FluidTraitManager.getTrait(ModFluids.DIESEL_CRACK.getSource(), FT_Flammable.class);
        long dieselCrackHeat = dieselCrackFlam != null ? dieselCrackFlam.getHeatEnergy() : 0L;
        FT_Flammable keroseneFlam2 = FluidTraitManager.getTrait(ModFluids.KEROSENE.getSource(), FT_Flammable.class);
        long keroseneHeat2 = keroseneFlam2 != null ? keroseneFlam2.getHeatEnergy() : 0L;

        reg(ModFluids.DIESEL_REFORM.getSource(), dieselHeat * complexityReform, 2.5D, FuelGrade.HIGH);
        reg(ModFluids.DIESEL_CRACK_REFORM.getSource(), dieselCrackHeat * complexityReform, 2.5D, FuelGrade.HIGH);
        reg(ModFluids.KEROSENE_REFORM.getSource(), keroseneHeat2 * complexityReform, 1.5D, FuelGrade.AERO);
        reg(ModFluids.REFORMGAS.getSource(), BASELINE / 0.06 * flammabilityHigh * demandLow * complexityVacuum * complexityFraction, 1.5D, FuelGrade.GAS);

        int coalHeat = 400_000;
        reg(ModFluids.COALOIL.getSource(), (coalHeat * (1000 / 100) * flammabilityLow * demandLow * complexityChemplant), 0, null);
        FT_Flammable coaloilFlam = FluidTraitManager.getTrait(ModFluids.COALOIL.getSource(), FT_Flammable.class);
        long coaloil = coaloilFlam != null ? coaloilFlam.getHeatEnergy() : 0L;
        reg(ModFluids.COALGAS.getSource(), (coaloil / 0.3 * flammabilityNormal * demandMedium * complexityChemplant * complexityFraction), 1.5, FuelGrade.MEDIUM);
        reg(ModFluids.COALGAS_LEADED.getSource(), (coaloil / 0.3 * flammabilityNormal * demandMedium * complexityChemplant * complexityFraction * complexityLeaded), 1.5, FuelGrade.MEDIUM);

        reg(ModFluids.ETHANOL.getSource(), 275_000D, 2.5D, FuelGrade.HIGH);
        reg(ModFluids.BIOGAS.getSource(), 250_000D * flammabilityLow, 1.25, FuelGrade.GAS);
        reg(ModFluids.BIOFUEL.getSource(), 500_000D, 2.5D, FuelGrade.HIGH);
        reg(ModFluids.WOODOIL.getSource(), 110_000, 0, null);
        reg(ModFluids.COALCREOSOTE.getSource(), 250_000, 0, null);
        reg(ModFluids.FISHOIL.getSource(), 75_000, 0, null);
        reg(ModFluids.SUNFLOWEROIL.getSource(), 50_000, 0, null);
        reg(ModFluids.SOLVENT.getSource(), 100_000, 0, null);
        reg(ModFluids.RADIOSOLVENT.getSource(), 150_000, 0, null);
        reg(ModFluids.SYNGAS.getSource(), (coalHeat * (1000 / 100) * flammabilityLow * demandLow * complexityChemplant) * 1.5, 1.25, FuelGrade.GAS);
        reg(ModFluids.OXYHYDROGEN.getSource(), 5_000, 3, FuelGrade.GAS);
    }
}
