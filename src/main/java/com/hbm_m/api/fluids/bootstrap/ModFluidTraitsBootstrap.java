package com.hbm_m.api.fluids.bootstrap;

import static com.hbm_m.api.fluids.bootstrap.ModFluidPollutionPresets.POISON_EXTREME;
import static com.hbm_m.api.fluids.bootstrap.ModFluidPollutionPresets.POISON_MINOR;
import static com.hbm_m.api.fluids.bootstrap.ModFluidPollutionPresets.P_FUEL;
import static com.hbm_m.api.fluids.bootstrap.ModFluidPollutionPresets.P_FUEL_LEADED;
import static com.hbm_m.api.fluids.bootstrap.ModFluidPollutionPresets.P_GAS;
import static com.hbm_m.api.fluids.bootstrap.ModFluidPollutionPresets.P_LIQUID_GAS;
import static com.hbm_m.api.fluids.bootstrap.ModFluidPollutionPresets.P_OIL;

import com.hbm_m.api.fluids.ModFluids;
import com.hbm_m.api.fluids.ModFluids.FluidEntry;
import com.hbm_m.inventory.fluid.trait.FT_Combustible;
import com.hbm_m.inventory.fluid.trait.FT_Combustible.FuelGrade;
import com.hbm_m.inventory.fluid.trait.FT_Coolable;
import com.hbm_m.inventory.fluid.trait.FT_Coolable.CoolingType;
import com.hbm_m.inventory.fluid.trait.FT_Corrosive;
import com.hbm_m.inventory.fluid.trait.FT_Flammable;
import com.hbm_m.inventory.fluid.trait.FT_Heatable;
import com.hbm_m.inventory.fluid.trait.FT_Heatable.HeatingType;
import com.hbm_m.inventory.fluid.trait.FT_PWRModerator;
import com.hbm_m.inventory.fluid.trait.FT_Pheromone;
import com.hbm_m.inventory.fluid.trait.FT_Poison;
import com.hbm_m.inventory.fluid.trait.FT_Polluting;
import com.hbm_m.inventory.fluid.trait.FT_Toxin;
import com.hbm_m.inventory.fluid.trait.FT_VentRadiation;
import com.hbm_m.inventory.fluid.trait.FluidTrait;
import com.hbm_m.inventory.fluid.trait.FluidTraitManager;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Amat;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Delicious;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous_ART;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_LeadContainer;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Liquid;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_NoContainer;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_NoID;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Plasma;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Unsiphonable;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Viscous;
import com.hbm_m.inventory.fluid.trait.PollutionType;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;

/**
 * Seeds {@link FluidTraitManager} from 1.7.10 {@code Fluids.init()} + heat/cool chains + spreadsheet fuels.
 */
public final class ModFluidTraitsBootstrap {

    private static final FT_Liquid LQ = new FT_Liquid();
    private static final FT_Gaseous GAS = new FT_Gaseous();
    private static final FT_Viscous VIS = new FT_Viscous();
    private static final FT_Plasma PL = new FT_Plasma();
    private static final FT_Amat ANTI = new FT_Amat();
    private static final FT_LeadContainer LEADCON = new FT_LeadContainer();
    private static final FT_NoContainer NOCON = new FT_NoContainer();
    private static final FT_NoID NOID = new FT_NoID();
    private static final FT_Delicious DEL = new FT_Delicious();
    private static final FT_Unsiphonable UNS = new FT_Unsiphonable();
    private static final FT_Gaseous_ART EVAP = new FT_Gaseous_ART();

    private ModFluidTraitsBootstrap() {}

    private static void t(FluidEntry e, Integer tempC) {
        t(e, tempC, new FluidTrait[0]);
    }

    private static void t(FluidEntry e, Integer tempC, FluidTrait... traits) {
        if (e == null) return;
        Fluid f = e.getSource();
        if (tempC != null) {
            FluidTraitManager.setTemperatureCelsius(f, tempC);
        }
        for (FluidTrait tr : traits) {
            FluidTraitManager.addTrait(f, tr);
        }
    }

    public static void registerAll() {
        registerStructural();
        ModFluidCalculatedFuel.apply();
        registerHeatCoolChains();
    }

    private static void registerStructural() {
        // NONE — no traits / room temp
        t(ModFluids.NONE, null);

        t(ModFluids.WATER, null, LQ, UNS);
        t(ModFluids.WATER_BASE, null, LQ, UNS);
        t(ModFluids.WATER_OPAQUE_BASE, null, LQ, UNS);
        t(ModFluids.CUSTOM_WATER, null, LQ, UNS);

        t(ModFluids.AIR, null, GAS);

        t(ModFluids.STEAM, 100, GAS, UNS);
        t(ModFluids.HOTSTEAM, 300, GAS, UNS);
        t(ModFluids.SUPERHOTSTEAM, 450, GAS, UNS);
        t(ModFluids.ULTRAHOTSTEAM, 600, GAS, UNS);
        t(ModFluids.SPENTSTEAM, null, NOCON, GAS);

        t(ModFluids.COOLANT, null, LQ);
        t(ModFluids.COOLANT_HOT, 600, LQ);
        t(ModFluids.CRYOGEL, -170, LQ, VIS);

        t(ModFluids.CRUDE_OIL, null, LQ, VIS, P_OIL);
        t(ModFluids.HOTOIL, 350, LQ, VIS, P_OIL);
        t(ModFluids.HEAVYOIL, null, LQ, VIS, P_OIL);
        t(ModFluids.BITUMEN, null, LQ, VIS, P_OIL);
        t(ModFluids.SMEAR, null, LQ, VIS, P_OIL);
        t(ModFluids.HEATINGOIL, null, LQ, VIS, P_OIL);
        t(ModFluids.LUBRICANT, null, LQ, P_OIL);
        t(ModFluids.CRACKOIL, null, LQ, VIS, P_OIL);
        t(ModFluids.COALOIL, null, LQ, VIS, P_OIL);
        t(ModFluids.OIL_DS, null, LQ, VIS, P_OIL);
        t(ModFluids.HOTOIL_DS, 350, LQ, VIS, P_OIL);
        t(ModFluids.CRACKOIL_DS, null, LQ, VIS, P_OIL);
        t(ModFluids.OIL_COKER, null, LQ, VIS, P_OIL);
        t(ModFluids.OIL_BASE, null, LQ, VIS, P_OIL);
        t(ModFluids.CUSTOM_OIL, null, LQ, VIS, P_OIL);

        // Spreadsheet fuels: ctor flammable/combustible stripped — ModFluidCalculatedFuel applies
        t(ModFluids.DIESEL, null, LQ, P_FUEL);
        t(ModFluids.DIESEL_CRACK, null, LQ, P_FUEL);
        t(ModFluids.DIESEL_CRACK_REFORM, null, LQ, P_FUEL);
        t(ModFluids.DIESEL_REFORM, null, LQ, P_FUEL);
        t(ModFluids.GASOLINE, null, LQ, P_FUEL);
        t(ModFluids.GASOLINE_LEADED, null, LQ, P_FUEL_LEADED);
        t(ModFluids.KEROSENE, null, LQ, P_FUEL);
        t(ModFluids.KEROSENE_REFORM, null, LQ, P_FUEL);
        t(ModFluids.HEAVYOIL_VACUUM, null, LQ, VIS, P_OIL);
        t(ModFluids.LIGHTOIL, null, LQ, P_FUEL);
        t(ModFluids.LIGHTOIL_CRACK, null, LQ, P_FUEL);
        t(ModFluids.LIGHTOIL_DS, null, LQ, P_FUEL);
        t(ModFluids.LIGHTOIL_VACUUM, null, LQ, P_FUEL);
        t(ModFluids.NAPHTHA, null, LQ, VIS, P_FUEL);
        t(ModFluids.NAPHTHA_CRACK, null, LQ, VIS, P_FUEL);
        t(ModFluids.NAPHTHA_DS, null, LQ, VIS, P_FUEL);
        t(ModFluids.NAPHTHA_COKER, null, LQ, VIS, P_OIL);
        t(ModFluids.RECLAIMED, null, LQ, VIS, P_FUEL);
        t(ModFluids.PETROIL, null, LQ, P_FUEL);
        t(ModFluids.PETROIL_LEADED, null, LQ, P_FUEL_LEADED);
        t(ModFluids.GAS, null, GAS, P_GAS);
        t(ModFluids.GAS_COKER, null, GAS, P_GAS);
        t(ModFluids.PETROLEUM, null, GAS, P_GAS);
        t(ModFluids.LPG, null, P_LIQUID_GAS);
        t(ModFluids.BIOGAS, null, GAS, P_GAS);
        t(ModFluids.BIOFUEL, null, LQ, P_FUEL);
        t(ModFluids.NITAN, null, LQ, P_FUEL);
        t(ModFluids.REFORMATE, null, LQ, VIS, P_FUEL);
        t(ModFluids.AROMATICS, null, LQ, VIS, P_GAS);
        t(ModFluids.UNSATURATEDS, null, GAS, P_GAS);
        t(ModFluids.XYLENE, null, LQ, VIS, P_FUEL);
        t(ModFluids.COALGAS, null, LQ, P_FUEL);
        t(ModFluids.COALGAS_LEADED, null, LQ, P_FUEL_LEADED);
        t(ModFluids.SYNGAS, null, GAS);
        t(ModFluids.REFORMGAS, null, GAS, P_GAS);
        t(ModFluids.SOURGAS, null, GAS, new FT_Corrosive(10), new FT_Poison(false, 1), P_GAS);
        t(ModFluids.HEATINGOIL_VACUUM, null, LQ, VIS, P_OIL);
        t(ModFluids.ETHANOL, null, LQ, P_FUEL);
        t(ModFluids.WOODOIL, null, LQ, VIS, P_OIL);
        t(ModFluids.COALCREOSOTE, null, LQ, VIS, P_OIL);
        t(ModFluids.FISHOIL, null, LQ, P_FUEL);
        t(ModFluids.SUNFLOWEROIL, null, LQ, P_FUEL);
        t(ModFluids.SOLVENT, null, LQ, new FT_Corrosive(30));
        t(ModFluids.RADIOSOLVENT, null, LQ, new FT_Corrosive(50));
        t(ModFluids.OXYHYDROGEN, null, GAS);

        t(ModFluids.DEUTERIUM, null, new FT_Flammable(5_000), new FT_Combustible(FuelGrade.HIGH, 10_000), GAS);
        t(ModFluids.TRITIUM, null, new FT_Flammable(5_000), new FT_Combustible(FuelGrade.HIGH, 10_000), GAS, new FT_VentRadiation(0.001F));
        t(ModFluids.UF6, null, new FT_VentRadiation(0.2F), new FT_Corrosive(15), GAS);
        t(ModFluids.PUF6, null, new FT_VentRadiation(0.1F), new FT_Corrosive(15), GAS);
        t(ModFluids.SAS3, null, new FT_VentRadiation(1F), new FT_Corrosive(30), LQ);
        t(ModFluids.SCHRABIDIC, null, new FT_VentRadiation(1F), new FT_Corrosive(75), new FT_Poison(true, 2), LQ);
        t(ModFluids.AMAT, null, ANTI, GAS);
        t(ModFluids.ASCHRAB, null, ANTI, GAS);
        t(ModFluids.PEROXIDE, null, new FT_Corrosive(40), LQ);
        t(ModFluids.WATZ, null, new FT_Corrosive(60), new FT_VentRadiation(0.1F), LQ, VIS, new FT_Polluting().release(PollutionType.POISON, POISON_EXTREME));
        t(ModFluids.HYDROGEN, -260, new FT_Flammable(5_000), new FT_Combustible(FuelGrade.HIGH, 10_000), LQ, EVAP);
        t(ModFluids.OXYGEN, -100, LQ, EVAP);
        t(ModFluids.XENON, null, GAS);
        t(ModFluids.BALEFIRE, 1500, new FT_Corrosive(50), LQ, VIS, P_FUEL);
        t(ModFluids.MERCURY, null, LQ, new FT_Poison(false, 2));
        t(ModFluids.PAIN, 300, new FT_Corrosive(30), new FT_Poison(true, 2), LQ, VIS);
        t(ModFluids.WASTEFLUID, null, new FT_VentRadiation(0.5F), NOCON, LQ, VIS);
        t(ModFluids.WASTEGAS, null, new FT_VentRadiation(0.5F), NOCON, GAS);
        t(ModFluids.FRACKSOL, null, new FT_Corrosive(15), new FT_Poison(false, 0), LQ, VIS);
        t(ModFluids.CARBONDIOXIDE, null, GAS, new FT_Polluting().release(PollutionType.POISON, POISON_MINOR));
        t(ModFluids.HEAVYWATER, null, LQ);
        t(ModFluids.HOTCRACKOIL, 350, LQ, VIS, P_OIL);
        t(ModFluids.HOTCRACKOIL_DS, 350, LQ, VIS, P_OIL);
        t(ModFluids.SALIENT, null, DEL, LQ, VIS);
        t(ModFluids.XPJUICE, null, LQ, VIS);
        t(ModFluids.ENDERJUICE, null, LQ);
        t(ModFluids.SULFURIC_ACID, null, new FT_Corrosive(50), LQ);
        t(ModFluids.NITRIC_ACID, null, LQ, new FT_Corrosive(60), new FT_Polluting().release(PollutionType.POISON, POISON_EXTREME));
        t(ModFluids.MUG, null, DEL, LQ);
        t(ModFluids.MUG_HOT, 500, DEL, LQ);
        t(ModFluids.SEEDSLURRY, null, LQ, VIS);
        t(ModFluids.NITROGLYCERIN, null, LQ);
        t(ModFluids.CHLORINE, null, new FT_Corrosive(25), GAS, toxinChlorine());
        t(ModFluids.PHOSGENE, null, GAS, new FT_Polluting().release(PollutionType.POISON, POISON_EXTREME), toxinPhosgene());
        t(ModFluids.MUSTARDGAS, null, GAS, new FT_Polluting().release(PollutionType.POISON, POISON_EXTREME), toxinMustard());
        t(ModFluids.IONGEL, null, LQ, VIS);
        t(ModFluids.EGG, null, LQ);
        t(ModFluids.CHOLESTEROL, null, LQ);
        t(ModFluids.ESTRADIOL, null, LQ, toxinEstradiol());
        t(ModFluids.SMOKE, null, GAS, NOID, NOCON);
        t(ModFluids.SMOKE_LEADED, null, GAS, NOID, NOCON);
        t(ModFluids.SMOKE_POISON, null, GAS, NOID, NOCON);
        t(ModFluids.HEAVYWATER_HOT, 600, LQ, VIS);
        t(ModFluids.SODIUM, 400, LQ, VIS);
        t(ModFluids.SODIUM_HOT, 1200, LQ, VIS);
        t(ModFluids.THORIUM_SALT, 800, LQ, VIS, new FT_Corrosive(65));
        t(ModFluids.THORIUM_SALT_HOT, 1600, LQ, VIS, new FT_Corrosive(65));
        t(ModFluids.THORIUM_SALT_DEPLETED, 800, LQ, VIS, new FT_Corrosive(65));
        t(ModFluids.FULLERENE, null, LQ, new FT_Corrosive(65), new FT_Polluting().release(PollutionType.POISON, POISON_MINOR));
        t(ModFluids.PHEROMONE, null, LQ, new FT_Pheromone(1));
        t(ModFluids.PHEROMONE_M, null, LQ, new FT_Pheromone(2));
        t(ModFluids.REDMUD, null, LQ, VIS, LEADCON, new FT_Corrosive(60), new FT_Flammable(1_000), new FT_Polluting().release(PollutionType.POISON, POISON_EXTREME), toxinRedmud());
        t(ModFluids.CHLOROCALCITE_SOLUTION, null, LQ, NOCON, new FT_Corrosive(60));
        t(ModFluids.CHLOROCALCITE_MIX, null, LQ, NOCON, new FT_Corrosive(60));
        t(ModFluids.CHLOROCALCITE_CLEANED, null, LQ, NOCON, new FT_Corrosive(60));
        t(ModFluids.POTASSIUM_CHLORIDE, null, LQ, NOCON, new FT_Corrosive(60));
        t(ModFluids.CALCIUM_CHLORIDE, null, LQ, NOCON, new FT_Corrosive(60));
        t(ModFluids.CALCIUM_SOLUTION, null, LQ, NOCON, new FT_Corrosive(60));
        t(ModFluids.LYE, null, new FT_Corrosive(40), LQ);
        t(ModFluids.SODIUM_ALUMINATE, null, new FT_Corrosive(30), LQ);
        t(ModFluids.BAUXITE_SOLUTION, null, new FT_Corrosive(40), LQ, VIS);
        t(ModFluids.ALUMINA, null, LQ);
        t(ModFluids.CONCRETE, null, LQ);
        t(ModFluids.DHC, null, GAS);
        t(ModFluids.LAVA, 1200, LQ, VIS);
        t(ModFluids.CUSTOM_LAVA, 1200, LQ, VIS);
        t(ModFluids.BLOOD, null, LQ, VIS, DEL);
        t(ModFluids.BLOOD_HOT, 666, LQ, VIS);
        t(ModFluids.COLLOID, null, LQ, VIS);
        t(ModFluids.SLOP, null, LQ, VIS);
        t(ModFluids.VITRIOL, null, LQ, VIS);
        t(ModFluids.CUSTOM_TOXIN, null, new FT_VentRadiation(0.5F), NOCON, LQ, VIS);
        t(ModFluids.TOXIN_BASE, null, new FT_VentRadiation(0.5F), NOCON, LQ, VIS);
        t(ModFluids.DEATH, 300, new FT_Corrosive(80), new FT_Poison(true, 4), LEADCON, LQ, VIS);
        t(ModFluids.STELLAR_FLUX, null, ANTI, GAS);
        t(ModFluids.BROMIDE, null, LQ);

        t(ModFluids.HELIUM3, null, GAS);
        t(ModFluids.HELIUM4, null, GAS);
        t(ModFluids.PLASMA_DT, 3250, NOCON, NOID, PL);
        t(ModFluids.PLASMA_HD, 2500, NOCON, NOID, PL);
        t(ModFluids.PLASMA_HT, 3000, NOCON, NOID, PL);
        t(ModFluids.PLASMA_DH3, 3480, NOCON, NOID, PL);
        t(ModFluids.PLASMA_XM, 4250, NOCON, NOID, PL);
        t(ModFluids.PLASMA_BF, 8500, NOCON, NOID, PL);

        t(ModFluids.PERFLUOROMETHYL, 15, LQ);
        t(ModFluids.PERFLUOROMETHYL_COLD, -150, LQ);
        t(ModFluids.PERFLUOROMETHYL_HOT, 250, LQ);

        t(ModFluids.LEAD, 350, LQ, VIS);
        t(ModFluids.LEAD_HOT, 1500, LQ, VIS);
    }

    private static FT_Toxin toxinChlorine() {
        return new FT_Toxin()
                .addTooltipLine(Component.translatable("fluid.hbm_m.toxin.chlorine.line1"))
                .addTooltipLine(Component.translatable("fluid.hbm_m.toxin.chlorine.line2"));
    }

    private static FT_Toxin toxinPhosgene() {
        return new FT_Toxin()
                .addTooltipLine(Component.translatable("fluid.hbm_m.toxin.phosgene.line1"))
                .addTooltipLine(Component.translatable("fluid.hbm_m.toxin.phosgene.line2"));
    }

    private static FT_Toxin toxinMustard() {
        return new FT_Toxin()
                .addTooltipLine(Component.translatable("fluid.hbm_m.toxin.mustard.line1"))
                .addTooltipLine(Component.translatable("fluid.hbm_m.toxin.mustard.line2"));
    }

    private static FT_Toxin toxinEstradiol() {
        return new FT_Toxin()
                .addTooltipLine(Component.translatable("fluid.hbm_m.toxin.estradiol.line1"))
                .addTooltipLine(Component.translatable("fluid.hbm_m.toxin.estradiol.line2"));
    }

    private static FT_Toxin toxinRedmud() {
        return new FT_Toxin()
                .addTooltipLine(Component.translatable("fluid.hbm_m.toxin.redmud.line1"))
                .addTooltipLine(Component.translatable("fluid.hbm_m.toxin.redmud.line2"));
    }

    private static void registerHeatCoolChains() {
        double effSteamBoil = 1.0D;
        double effSteamHeatex = 0.25D;
        double effSteamTurbine = 1.0D;
        double effSteamCool = 0.5D;

        Fluid w = ModFluids.WATER.getSource();
        Fluid st = ModFluids.STEAM.getSource();
        Fluid hst = ModFluids.HOTSTEAM.getSource();
        Fluid sst = ModFluids.SUPERHOTSTEAM.getSource();
        Fluid ust = ModFluids.ULTRAHOTSTEAM.getSource();
        Fluid spent = ModFluids.SPENTSTEAM.getSource();

        FluidTraitManager.addTrait(w, new FT_Heatable()
                .setEff(HeatingType.BOILER, effSteamBoil)
                .setEff(HeatingType.HEATEXCHANGER, effSteamHeatex)
                .addStep(200, 1, st, 100)
                .addStep(220, 1, hst, 10)
                .addStep(238, 1, sst, 1)
                .addStep(2500, 10, ust, 1));

        FluidTraitManager.addTrait(st, new FT_Heatable()
                .setEff(HeatingType.BOILER, effSteamBoil)
                .setEff(HeatingType.HEATEXCHANGER, effSteamHeatex)
                .addStep(2, 10, hst, 1));
        FluidTraitManager.addTrait(hst, new FT_Heatable()
                .setEff(HeatingType.BOILER, effSteamBoil)
                .setEff(HeatingType.HEATEXCHANGER, effSteamHeatex)
                .addStep(18, 10, sst, 1));
        FluidTraitManager.addTrait(sst, new FT_Heatable()
                .setEff(HeatingType.BOILER, effSteamBoil)
                .setEff(HeatingType.HEATEXCHANGER, effSteamHeatex)
                .addStep(120, 10, ust, 1));

        FluidTraitManager.addTrait(st, new FT_Coolable(spent, 100, 1, 200)
                .setEff(CoolingType.TURBINE, effSteamTurbine)
                .setEff(CoolingType.HEATEXCHANGER, effSteamCool));
        FluidTraitManager.addTrait(hst, new FT_Coolable(st, 1, 10, 2)
                .setEff(CoolingType.TURBINE, effSteamTurbine)
                .setEff(CoolingType.HEATEXCHANGER, effSteamCool));
        FluidTraitManager.addTrait(sst, new FT_Coolable(hst, 1, 10, 18)
                .setEff(CoolingType.TURBINE, effSteamTurbine)
                .setEff(CoolingType.HEATEXCHANGER, effSteamCool));
        FluidTraitManager.addTrait(ust, new FT_Coolable(sst, 1, 10, 120)
                .setEff(CoolingType.TURBINE, effSteamTurbine)
                .setEff(CoolingType.HEATEXCHANGER, effSteamCool));

        Fluid oil = ModFluids.CRUDE_OIL.getSource();
        Fluid hotoil = ModFluids.HOTOIL.getSource();
        Fluid oilDs = ModFluids.OIL_DS.getSource();
        Fluid hotoilDs = ModFluids.HOTOIL_DS.getSource();
        Fluid crack = ModFluids.CRACKOIL.getSource();
        Fluid hotcrack = ModFluids.HOTCRACKOIL.getSource();
        Fluid crackDs = ModFluids.CRACKOIL_DS.getSource();
        Fluid hotcrackDs = ModFluids.HOTCRACKOIL_DS.getSource();

        FluidTraitManager.addTrait(oil, new FT_Heatable()
                .setEff(HeatingType.BOILER, 1.0D)
                .setEff(HeatingType.HEATEXCHANGER, 1.0D)
                .addStep(10, 1, hotoil, 1));
        FluidTraitManager.addTrait(oilDs, new FT_Heatable()
                .setEff(HeatingType.BOILER, 1.0D)
                .setEff(HeatingType.HEATEXCHANGER, 1.0D)
                .addStep(10, 1, hotoilDs, 1));
        FluidTraitManager.addTrait(crack, new FT_Heatable()
                .setEff(HeatingType.BOILER, 1.0D)
                .setEff(HeatingType.HEATEXCHANGER, 1.0D)
                .addStep(10, 1, hotcrack, 1));
        FluidTraitManager.addTrait(crackDs, new FT_Heatable()
                .setEff(HeatingType.BOILER, 1.0D)
                .setEff(HeatingType.HEATEXCHANGER, 1.0D)
                .addStep(10, 1, hotcrackDs, 1));

        FluidTraitManager.addTrait(hotoil, new FT_Coolable(oil, 1, 1, 10).setEff(CoolingType.HEATEXCHANGER, 1.0D));
        FluidTraitManager.addTrait(hotoilDs, new FT_Coolable(oilDs, 1, 1, 10).setEff(CoolingType.HEATEXCHANGER, 1.0D));
        FluidTraitManager.addTrait(hotcrack, new FT_Coolable(crack, 1, 1, 10).setEff(CoolingType.HEATEXCHANGER, 1.0D));
        FluidTraitManager.addTrait(hotcrackDs, new FT_Coolable(crackDs, 1, 1, 10).setEff(CoolingType.HEATEXCHANGER, 1.0D));

        Fluid cool = ModFluids.COOLANT.getSource();
        Fluid coolHot = ModFluids.COOLANT_HOT.getSource();
        FluidTraitManager.addTrait(cool, new FT_Heatable()
                .setEff(HeatingType.HEATEXCHANGER, 1.0D)
                .setEff(HeatingType.PWR, 1.0D)
                .setEff(HeatingType.ICF, 1.0D)
                .addStep(300, 1, coolHot, 1));
        FluidTraitManager.addTrait(coolHot, new FT_Coolable(cool, 1, 1, 300).setEff(CoolingType.HEATEXCHANGER, 1.0D));

        Fluid pfc = ModFluids.PERFLUOROMETHYL_COLD.getSource();
        Fluid pf = ModFluids.PERFLUOROMETHYL.getSource();
        Fluid pfh = ModFluids.PERFLUOROMETHYL_HOT.getSource();
        FluidTraitManager.addTrait(pfc, new FT_Heatable().setEff(HeatingType.PA, 1.0D).addStep(300, 1, pf, 1));
        FluidTraitManager.addTrait(pf, new FT_Heatable()
                .setEff(HeatingType.HEATEXCHANGER, 1.0D)
                .setEff(HeatingType.PWR, 1.0D)
                .setEff(HeatingType.ICF, 1.0D)
                .addStep(300, 1, pfh, 1));
        FluidTraitManager.addTrait(pfh, new FT_Coolable(pf, 1, 1, 300).setEff(CoolingType.HEATEXCHANGER, 1.0D));

        Fluid mug = ModFluids.MUG.getSource();
        Fluid mugH = ModFluids.MUG_HOT.getSource();
        FluidTraitManager.addTrait(mug, new FT_Heatable()
                .setEff(HeatingType.HEATEXCHANGER, 1.0D)
                .setEff(HeatingType.PWR, 1.0D)
                .setEff(HeatingType.ICF, 1.25D)
                .addStep(400, 1, mugH, 1));
        FluidTraitManager.addTrait(mug, new FT_PWRModerator(1.15D));
        FluidTraitManager.addTrait(mugH, new FT_Coolable(mug, 1, 1, 400).setEff(CoolingType.HEATEXCHANGER, 1.0D));

        Fluid blood = ModFluids.BLOOD.getSource();
        Fluid bloodH = ModFluids.BLOOD_HOT.getSource();
        FluidTraitManager.addTrait(blood, new FT_Heatable()
                .setEff(HeatingType.HEATEXCHANGER, 1.0D)
                .setEff(HeatingType.ICF, 1.25D)
                .addStep(500, 1, bloodH, 1));
        FluidTraitManager.addTrait(bloodH, new FT_Coolable(blood, 1, 1, 500).setEff(CoolingType.HEATEXCHANGER, 1.0D));

        Fluid hw = ModFluids.HEAVYWATER.getSource();
        Fluid hwH = ModFluids.HEAVYWATER_HOT.getSource();
        FluidTraitManager.addTrait(hw, new FT_Heatable().setEff(HeatingType.PWR, 1.0D).addStep(300, 1, hwH, 1));
        FluidTraitManager.addTrait(hw, new FT_PWRModerator(1.25D));
        FluidTraitManager.addTrait(hwH, new FT_Coolable(hw, 1, 1, 300).setEff(CoolingType.HEATEXCHANGER, 1.0D));

        Fluid na = ModFluids.SODIUM.getSource();
        Fluid naH = ModFluids.SODIUM_HOT.getSource();
        FluidTraitManager.addTrait(na, new FT_Heatable()
                .setEff(HeatingType.PWR, 2.5D)
                .setEff(HeatingType.ICF, 3D)
                .addStep(400, 1, naH, 1));
        FluidTraitManager.addTrait(naH, new FT_Coolable(na, 1, 1, 400).setEff(CoolingType.HEATEXCHANGER, 1.0D));

        Fluid pb = ModFluids.LEAD.getSource();
        Fluid pbH = ModFluids.LEAD_HOT.getSource();
        FluidTraitManager.addTrait(pb, new FT_Heatable()
                .setEff(HeatingType.PWR, 0.75D)
                .setEff(HeatingType.ICF, 4D)
                .addStep(800, 1, pbH, 1));
        FluidTraitManager.addTrait(pb, new FT_PWRModerator(0.75D));
        FluidTraitManager.addTrait(pbH, new FT_Coolable(pb, 1, 1, 680).setEff(CoolingType.HEATEXCHANGER, 1.0D));

        Fluid th = ModFluids.THORIUM_SALT.getSource();
        Fluid thH = ModFluids.THORIUM_SALT_HOT.getSource();
        Fluid thD = ModFluids.THORIUM_SALT_DEPLETED.getSource();
        FluidTraitManager.addTrait(th, new FT_Heatable().setEff(HeatingType.PWR, 1.0D).addStep(400, 1, thH, 1));
        FluidTraitManager.addTrait(th, new FT_PWRModerator(2.5D));
        FluidTraitManager.addTrait(thH, new FT_Coolable(thD, 1, 1, 400).setEff(CoolingType.HEATEXCHANGER, 1.0D));
    }
}
