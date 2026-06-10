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

    /** 1.7.10 per-fluid overrides (names match {@link FluidType#getName()}). */
    private static void applyExplicitOverrides() {
        h(ModFluids.NONE, 0, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.WATER, 0, 0, 0, FluidHazardSymbol.NONE);
        h(Fluids.WATER, 0, 0, 0, FluidHazardSymbol.NONE);
        h(Fluids.FLOWING_WATER, 0, 0, 0, FluidHazardSymbol.NONE);

        h(ModFluids.STEAM, 3, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.HOTSTEAM, 4, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.SUPERHOTSTEAM, 4, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.ULTRAHOTSTEAM, 4, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.LAVA, 4, 0, 0, FluidHazardSymbol.NOWATER);
        h(Fluids.LAVA, 4, 0, 0, FluidHazardSymbol.NOWATER);
        h(Fluids.FLOWING_LAVA, 4, 0, 0, FluidHazardSymbol.NOWATER);

        h(ModFluids.DEUTERIUM, 3, 4, 0, FluidHazardSymbol.NONE);
        h(ModFluids.TRITIUM, 3, 4, 0, FluidHazardSymbol.RADIATION);
        h(ModFluids.KEROSENE, 1, 2, 0, FluidHazardSymbol.NONE);
        h(ModFluids.GAS, 1, 4, 1, FluidHazardSymbol.NONE);
        h(ModFluids.PEROXIDE, 3, 0, 3, FluidHazardSymbol.OXIDIZER);
        h(ModFluids.OXYGEN, 3, 0, 0, FluidHazardSymbol.CROYGENIC);
        h(ModFluids.HYDROGEN, 3, 4, 0, FluidHazardSymbol.CROYGENIC);
        h(ModFluids.CRYOGEL, 2, 0, 0, FluidHazardSymbol.CROYGENIC);
        h(ModFluids.XENON, 0, 0, 0, FluidHazardSymbol.ASPHYXIANT);
        h(ModFluids.CARBONDIOXIDE, 3, 0, 0, FluidHazardSymbol.ASPHYXIANT);
        h(ModFluids.HELIUM3, 0, 0, 0, FluidHazardSymbol.ASPHYXIANT);
        h(ModFluids.HELIUM4, 0, 0, 0, FluidHazardSymbol.ASPHYXIANT);

        h(ModFluids.UF6, 4, 0, 2, FluidHazardSymbol.RADIATION);
        h(ModFluids.PUF6, 4, 0, 4, FluidHazardSymbol.RADIATION);
        h(ModFluids.SAS3, 5, 0, 4, FluidHazardSymbol.RADIATION);
        h(ModFluids.SCHRABIDIC, 5, 0, 5, FluidHazardSymbol.ACID);
        h(ModFluids.AMAT, 5, 0, 5, FluidHazardSymbol.ANTIMATTER);
        h(ModFluids.ASCHRAB, 5, 0, 5, FluidHazardSymbol.ANTIMATTER);
        h(ModFluids.WATZ, 4, 0, 3, FluidHazardSymbol.ACID);
        h(ModFluids.MERCURY, 2, 0, 0, FluidHazardSymbol.NONE);
        h(ModFluids.PAIN, 2, 0, 1, FluidHazardSymbol.ACID);
        h(ModFluids.WASTEFLUID, 2, 0, 1, FluidHazardSymbol.RADIATION);
        h(ModFluids.WASTEGAS, 2, 0, 1, FluidHazardSymbol.RADIATION);
        h(ModFluids.FRACKSOL, 1, 3, 3, FluidHazardSymbol.ACID);
        h(ModFluids.SULFURIC_ACID, 3, 0, 2, FluidHazardSymbol.ACID);
        h(ModFluids.NITRIC_ACID, 3, 0, 2, FluidHazardSymbol.OXIDIZER);
        h(ModFluids.DEATH, 2, 0, 1, FluidHazardSymbol.ACID);
        h(ModFluids.BALEFIRE, 4, 4, 3, FluidHazardSymbol.RADIATION);
        h(ModFluids.PLASMA_BF, 4, 5, 4, FluidHazardSymbol.ANTIMATTER);
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
