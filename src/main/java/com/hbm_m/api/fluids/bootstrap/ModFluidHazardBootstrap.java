package com.hbm_m.api.fluids.bootstrap;

import com.hbm_m.inventory.fluid.FluidHazardSymbol;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.ModFluids.FluidEntry;
import com.hbm_m.inventory.fluid.trait.FT_Corrosive;
import com.hbm_m.inventory.fluid.trait.FT_Flammable;
import com.hbm_m.inventory.fluid.trait.FT_Poison;
import com.hbm_m.inventory.fluid.trait.FT_Polluting;
import com.hbm_m.inventory.fluid.trait.FT_VentRadiation;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Amat;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous;
import com.hbm_m.inventory.fluid.trait.PollutionType;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Seeds NFPA hazard diamond data from 1.7.10 {@code Fluids} definitions.
 */
public final class ModFluidHazardBootstrap {

    private ModFluidHazardBootstrap() {}

    public static void registerAll() {
        for (FluidEntry entry : ModFluids.getAllEntries().values()) {
            applyInferredHazard(FluidType.forFluid(entry.getSource()));
        }
        applyExplicitOverrides();
    }

    private static void applyInferredHazard(FluidType type) {
        if (type.getFluid() == null || type.getFluid() == Fluids.EMPTY) {
            return;
        }

        int poison = 0;
        int flammability = 0;
        int reactivity = 0;
        FluidHazardSymbol symbol = FluidHazardSymbol.NONE;

        FT_Poison poisonTrait = type.getTrait(FT_Poison.class);
        if (poisonTrait != null) {
            poison = Math.max(poison, poisonTrait.isWithering() ? 4 : 2);
        }

        FT_Polluting poll = type.getTrait(FT_Polluting.class);
        if (poll != null && poll.releaseMap.containsKey(PollutionType.POISON)) {
            float amount = poll.releaseMap.get(PollutionType.POISON);
            poison = Math.max(poison, amount >= ModFluidPollutionPresets.POISON_EXTREME ? 4 : 1);
        }

        FT_Flammable flam = type.getTrait(FT_Flammable.class);
        if (flam != null) {
            long e = flam.getHeatEnergy();
            if (e >= 2_000_000L) {
                flammability = 4;
            } else if (e >= 400_000L) {
                flammability = 3;
            } else if (e >= 100_000L) {
                flammability = 2;
            } else if (e >= 10_000L) {
                flammability = 1;
            }
        }

        FT_Corrosive cor = type.getTrait(FT_Corrosive.class);
        if (cor != null) {
            int r = cor.getRating();
            if (r >= 75) {
                reactivity = 5;
            } else if (r >= 60) {
                reactivity = 4;
            } else if (r >= 40) {
                reactivity = 3;
            } else if (r >= 15) {
                reactivity = 2;
            } else if (r > 0) {
                reactivity = 1;
            }
        }

        FT_VentRadiation rad = type.getTrait(FT_VentRadiation.class);
        float radPerMb = rad != null ? rad.getRadPerMB() : 0.0F;
        if (radPerMb >= 1.0F) {
            reactivity = Math.max(reactivity, 5);
        } else if (radPerMb >= 0.2F) {
            reactivity = Math.max(reactivity, 4);
        } else if (radPerMb >= 0.1F) {
            reactivity = Math.max(reactivity, 3);
        } else if (radPerMb > 0.0F) {
            reactivity = Math.max(reactivity, 2);
        }

        if (type.temperature >= 1200) {
            flammability = Math.max(flammability, 4);
        } else if (type.temperature >= 350) {
            flammability = Math.max(flammability, 3);
        }

        if (type.hasTrait(FT_Amat.class)) {
            poison = Math.max(poison, 5);
            reactivity = Math.max(reactivity, 5);
            symbol = FluidHazardSymbol.ANTIMATTER;
        } else if (radPerMb >= 0.05F) {
            symbol = FluidHazardSymbol.RADIATION;
        } else if (type.temperature <= -100 && !type.hasTrait(FT_Flammable.class)) {
            symbol = FluidHazardSymbol.CROYGENIC;
        }

        type.setHazardDiamond(poison, flammability, reactivity, symbol);
    }

    /**
     * 1:1 Gefahrenraute (p/f/r/Symbol) aus den {@code new FluidType(...)}-Konstruktoren von {@code Fluids.init()},
     * alle 156 Original-Typen in Original-Reihenfolge. Die Schaetzung oben bleibt nur fuer Port-eigene Basistexturen.
     */
    private static void applyExplicitOverrides() {
        h(ModFluids.NONE, 0, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.WATER, 0, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.STEAM, 3, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.HOTSTEAM, 4, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.SUPERHOTSTEAM, 4, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.ULTRAHOTSTEAM, 4, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.COOLANT, 1, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.LAVA, 4, 0, 0, FluidHazardSymbol.NOWATER);
        h(ModFluids.DEUTERIUM, 3, 4, 0, FluidHazardSymbol.NONE);
        h(ModFluids.TRITIUM, 3, 4, 0, FluidHazardSymbol.RADIATION);
        h(ModFluids.CRUDE_OIL, 2, 1, 0, FluidHazardSymbol.NONE);
        h(ModFluids.HOTOIL, 2, 3, 0, FluidHazardSymbol.NONE);
        h(ModFluids.HEAVYOIL, 2, 1, 0, FluidHazardSymbol.NONE);
        h(ModFluids.BITUMEN, 2, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.SMEAR, 2, 1, 0, FluidHazardSymbol.NONE);
        h(ModFluids.HEATINGOIL, 2, 2, 0, FluidHazardSymbol.NONE);
        h(ModFluids.RECLAIMED, 2, 2, 0, FluidHazardSymbol.NONE);
        h(ModFluids.PETROIL, 1, 3, 0, FluidHazardSymbol.NONE);
        h(ModFluids.LUBRICANT, 2, 1, 0, FluidHazardSymbol.NONE);
        h(ModFluids.NAPHTHA, 2, 1, 0, FluidHazardSymbol.NONE);
        h(ModFluids.DIESEL, 1, 2, 0, FluidHazardSymbol.NONE);
        h(ModFluids.LIGHTOIL, 1, 2, 0, FluidHazardSymbol.NONE);
        h(ModFluids.KEROSENE, 1, 2, 0, FluidHazardSymbol.NONE);
        h(ModFluids.GAS, 1, 4, 1, FluidHazardSymbol.NONE);
        h(ModFluids.PETROLEUM, 1, 4, 1, FluidHazardSymbol.NONE);
        h(ModFluids.LPG, 1, 3, 1, FluidHazardSymbol.NONE);
        h(ModFluids.BIOGAS, 1, 4, 1, FluidHazardSymbol.NONE);
        h(ModFluids.BIOFUEL, 1, 2, 0, FluidHazardSymbol.NONE);
        h(ModFluids.NITAN, 2, 4, 1, FluidHazardSymbol.NONE);
        h(ModFluids.UF6, 4, 0, 2, FluidHazardSymbol.RADIATION);
        h(ModFluids.PUF6, 4, 0, 4, FluidHazardSymbol.RADIATION);
        h(ModFluids.SAS3, 5, 0, 4, FluidHazardSymbol.RADIATION);
        h(ModFluids.SCHRABIDIC, 5, 0, 5, FluidHazardSymbol.ACID);
        h(ModFluids.AMAT, 5, 0, 5, FluidHazardSymbol.ANTIMATTER);
        h(ModFluids.ASCHRAB, 5, 0, 5, FluidHazardSymbol.ANTIMATTER);
        h(ModFluids.PEROXIDE, 3, 0, 3, FluidHazardSymbol.OXIDIZER);
        h(ModFluids.WATZ, 4, 0, 3, FluidHazardSymbol.ACID);
        h(ModFluids.CRYOGEL, 2, 0, 0, FluidHazardSymbol.CROYGENIC);
        h(ModFluids.HYDROGEN, 3, 4, 0, FluidHazardSymbol.CROYGENIC);
        h(ModFluids.OXYGEN, 3, 0, 0, FluidHazardSymbol.CROYGENIC);
        h(ModFluids.XENON, 0, 0, 0, FluidHazardSymbol.ASPHYXIANT);
        h(ModFluids.BALEFIRE, 4, 4, 3, FluidHazardSymbol.RADIATION);
        h(ModFluids.MERCURY, 2, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.PAIN, 2, 0, 1, FluidHazardSymbol.ACID);
        h(ModFluids.WASTEFLUID, 2, 0, 1, FluidHazardSymbol.RADIATION);
        h(ModFluids.WASTEGAS, 2, 0, 1, FluidHazardSymbol.RADIATION);
        h(ModFluids.GASOLINE, 1, 2, 0, FluidHazardSymbol.NONE);
        h(ModFluids.COALGAS, 1, 2, 0, FluidHazardSymbol.NONE);
        h(ModFluids.SPENTSTEAM, 2, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.FRACKSOL, 1, 3, 3, FluidHazardSymbol.ACID);
        h(ModFluids.PLASMA_DT, 0, 4, 0, FluidHazardSymbol.RADIATION);
        h(ModFluids.PLASMA_HD, 0, 4, 0, FluidHazardSymbol.RADIATION);
        h(ModFluids.PLASMA_HT, 0, 4, 0, FluidHazardSymbol.RADIATION);
        h(ModFluids.PLASMA_XM, 0, 4, 1, FluidHazardSymbol.RADIATION);
        h(ModFluids.PLASMA_BF, 4, 5, 4, FluidHazardSymbol.ANTIMATTER);
        h(ModFluids.CARBONDIOXIDE, 3, 0, 0, FluidHazardSymbol.ASPHYXIANT);
        h(ModFluids.PLASMA_DH3, 0, 4, 0, FluidHazardSymbol.RADIATION);
        h(ModFluids.HELIUM3, 0, 0, 0, FluidHazardSymbol.ASPHYXIANT);
        h(ModFluids.DEATH, 2, 0, 1, FluidHazardSymbol.ACID);
        h(ModFluids.ETHANOL, 2, 3, 0, FluidHazardSymbol.NONE);
        h(ModFluids.HEAVYWATER, 1, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.CRACKOIL, 2, 1, 0, FluidHazardSymbol.NONE);
        h(ModFluids.COALOIL, 2, 1, 0, FluidHazardSymbol.NONE);
        h(ModFluids.HOTCRACKOIL, 2, 3, 0, FluidHazardSymbol.NONE);
        h(ModFluids.NAPHTHA_CRACK, 2, 1, 0, FluidHazardSymbol.NONE);
        h(ModFluids.LIGHTOIL_CRACK, 1, 2, 0, FluidHazardSymbol.NONE);
        h(ModFluids.DIESEL_CRACK, 1, 2, 0, FluidHazardSymbol.NONE);
        h(ModFluids.AROMATICS, 1, 4, 1, FluidHazardSymbol.NONE);
        h(ModFluids.UNSATURATEDS, 1, 4, 1, FluidHazardSymbol.NONE);
        h(ModFluids.SALIENT, 0, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.XPJUICE, 0, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.ENDERJUICE, 0, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.PETROIL_LEADED, 1, 3, 0, FluidHazardSymbol.NONE);
        h(ModFluids.GASOLINE_LEADED, 1, 2, 0, FluidHazardSymbol.NONE);
        h(ModFluids.COALGAS_LEADED, 1, 2, 0, FluidHazardSymbol.NONE);
        h(ModFluids.SULFURIC_ACID, 3, 0, 2, FluidHazardSymbol.ACID);
        h(ModFluids.COOLANT_HOT, 1, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.MUG, 0, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.MUG_HOT, 0, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.WOODOIL, 2, 2, 0, FluidHazardSymbol.NONE);
        h(ModFluids.COALCREOSOTE, 3, 2, 0, FluidHazardSymbol.NONE);
        h(ModFluids.SEEDSLURRY, 0, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.NITRIC_ACID, 3, 0, 2, FluidHazardSymbol.OXIDIZER);
        h(ModFluids.SOLVENT, 2, 3, 0, FluidHazardSymbol.NONE);
        h(ModFluids.BLOOD, 0, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.BLOOD_HOT, 3, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.SYNGAS, 1, 4, 2, FluidHazardSymbol.NONE);
        h(ModFluids.OXYHYDROGEN, 0, 4, 2, FluidHazardSymbol.NONE);
        h(ModFluids.RADIOSOLVENT, 3, 3, 0, FluidHazardSymbol.NONE);
        h(ModFluids.CHLORINE, 3, 0, 0, FluidHazardSymbol.OXIDIZER);
        h(ModFluids.HEAVYOIL_VACUUM, 2, 1, 0, FluidHazardSymbol.NONE);
        h(ModFluids.REFORMATE, 2, 2, 0, FluidHazardSymbol.NONE);
        h(ModFluids.LIGHTOIL_VACUUM, 1, 2, 0, FluidHazardSymbol.NONE);
        h(ModFluids.SOURGAS, 4, 4, 0, FluidHazardSymbol.ACID);
        h(ModFluids.XYLENE, 2, 3, 0, FluidHazardSymbol.NONE);
        h(ModFluids.HEATINGOIL_VACUUM, 2, 2, 0, FluidHazardSymbol.NONE);
        h(ModFluids.DIESEL_REFORM, 1, 2, 0, FluidHazardSymbol.NONE);
        h(ModFluids.DIESEL_CRACK_REFORM, 1, 2, 0, FluidHazardSymbol.NONE);
        h(ModFluids.KEROSENE_REFORM, 1, 2, 0, FluidHazardSymbol.NONE);
        h(ModFluids.REFORMGAS, 1, 4, 1, FluidHazardSymbol.NONE);
        h(ModFluids.COLLOID, 0, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.PHOSGENE, 4, 0, 1, FluidHazardSymbol.NONE);
        h(ModFluids.MUSTARDGAS, 4, 1, 1, FluidHazardSymbol.NONE);
        h(ModFluids.IONGEL, 1, 0, 4, FluidHazardSymbol.NONE);
        h(ModFluids.OIL_COKER, 2, 1, 0, FluidHazardSymbol.NONE);
        h(ModFluids.NAPHTHA_COKER, 2, 1, 0, FluidHazardSymbol.NONE);
        h(ModFluids.GAS_COKER, 1, 4, 0, FluidHazardSymbol.NONE);
        h(ModFluids.EGG, 0, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.CHOLESTEROL, 0, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.ESTRADIOL, 0, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.FISHOIL, 0, 1, 0, FluidHazardSymbol.NONE);
        h(ModFluids.SUNFLOWEROIL, 0, 1, 0, FluidHazardSymbol.NONE);
        h(ModFluids.NITROGLYCERIN, 0, 4, 0, FluidHazardSymbol.NONE);
        h(ModFluids.REDMUD, 3, 0, 4, FluidHazardSymbol.NONE);
        h(ModFluids.CHLOROCALCITE_SOLUTION, 0, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.CHLOROCALCITE_MIX, 0, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.CHLOROCALCITE_CLEANED, 0, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.POTASSIUM_CHLORIDE, 0, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.CALCIUM_CHLORIDE, 0, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.CALCIUM_SOLUTION, 0, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.SMOKE, 0, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.SMOKE_LEADED, 0, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.SMOKE_POISON, 0, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.HELIUM4, 0, 0, 0, FluidHazardSymbol.ASPHYXIANT);
        h(ModFluids.HEAVYWATER_HOT, 1, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.SODIUM, 1, 2, 3, FluidHazardSymbol.NONE);
        h(ModFluids.SODIUM_HOT, 1, 2, 3, FluidHazardSymbol.NONE);
        h(ModFluids.THORIUM_SALT, 2, 0, 3, FluidHazardSymbol.NONE);
        h(ModFluids.THORIUM_SALT_HOT, 2, 0, 3, FluidHazardSymbol.NONE);
        h(ModFluids.THORIUM_SALT_DEPLETED, 2, 0, 3, FluidHazardSymbol.NONE);
        h(ModFluids.FULLERENE, 3, 3, 3, FluidHazardSymbol.NONE);
        h(ModFluids.PHEROMONE, 0, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.PHEROMONE_M, 0, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.OIL_DS, 2, 1, 0, FluidHazardSymbol.NONE);
        h(ModFluids.HOTOIL_DS, 2, 3, 0, FluidHazardSymbol.NONE);
        h(ModFluids.CRACKOIL_DS, 2, 1, 0, FluidHazardSymbol.NONE);
        h(ModFluids.HOTCRACKOIL_DS, 2, 3, 0, FluidHazardSymbol.NONE);
        h(ModFluids.NAPHTHA_DS, 2, 1, 0, FluidHazardSymbol.NONE);
        h(ModFluids.LIGHTOIL_DS, 1, 2, 0, FluidHazardSymbol.NONE);
        h(ModFluids.STELLAR_FLUX, 0, 4, 4, FluidHazardSymbol.ANTIMATTER);
        h(ModFluids.VITRIOL, 2, 0, 1, FluidHazardSymbol.NONE);
        h(ModFluids.SLOP, 0, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.LEAD, 4, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.LEAD_HOT, 4, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.PERFLUOROMETHYL, 1, 0, 1, FluidHazardSymbol.NONE);
        h(ModFluids.PERFLUOROMETHYL_COLD, 1, 0, 1, FluidHazardSymbol.NONE);
        h(ModFluids.PERFLUOROMETHYL_HOT, 1, 0, 1, FluidHazardSymbol.NONE);
        h(ModFluids.LYE, 3, 0, 1, FluidHazardSymbol.ACID);
        h(ModFluids.SODIUM_ALUMINATE, 3, 0, 1, FluidHazardSymbol.ACID);
        h(ModFluids.BAUXITE_SOLUTION, 3, 0, 3, FluidHazardSymbol.ACID);
        h(ModFluids.ALUMINA, 0, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.AIR, 0, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.CONCRETE, 0, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.DHC, 0, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.AIRBLAST, 0, 3, 0, FluidHazardSymbol.NONE);
        h(ModFluids.FLUE, 1, 4, 1, FluidHazardSymbol.NONE);

        // Vanilla-Gegenstuecke wie die HBM-Typen
        h(Fluids.WATER, 0, 0, 0, FluidHazardSymbol.NONE);
        h(Fluids.FLOWING_WATER, 0, 0, 0, FluidHazardSymbol.NONE);
        h(Fluids.LAVA, 4, 0, 0, FluidHazardSymbol.NOWATER);
        h(Fluids.FLOWING_LAVA, 4, 0, 0, FluidHazardSymbol.NOWATER);
    }

    private static void h(FluidEntry entry, int p, int f, int r, FluidHazardSymbol symbol) {
        if (entry != null) {
            FluidType.setHazardDiamond(entry.getSource(), p, f, r, symbol);
        }
    }

    private static void h(Fluid fluid, int p, int f, int r, FluidHazardSymbol symbol) {
        if (fluid != null) {
            FluidType.setHazardDiamond(fluid, p, f, r, symbol);
        }
    }
}
