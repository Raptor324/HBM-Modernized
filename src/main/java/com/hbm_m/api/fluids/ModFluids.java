package com.hbm_m.api.fluids;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.hbm_m.main.MainRegistry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, MainRegistry.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, MainRegistry.MOD_ID);

    // Storage for fluid entries
    private static final Map<String, FluidEntry> FLUID_ENTRIES = new HashMap<>();

    // Fluid holder class
    public static class FluidEntry {
        public final RegistryObject<FluidType> type;
        public final RegistryObject<Fluid> source;
        public final RegistryObject<Fluid> flowing;
        private FlowingFluid sourceFluid;
        private FlowingFluid flowingFluid;

        public FluidEntry(RegistryObject<FluidType> type, RegistryObject<Fluid> source, RegistryObject<Fluid> flowing) {
            this.type = type;
            this.source = source;
            this.flowing = flowing;
        }

        public Fluid getSource() {
            return source.get();
        }

        public Fluid getFlowing() {
            return flowing.get();
        }
    }

    // Helper method to register a fluid
    private static FluidEntry registerFluid(String name, int density, int viscosity, int tintColor) {
        final FluidEntry[] entryHolder = new FluidEntry[1];

        // Register FluidType
        RegistryObject<FluidType> type = FLUID_TYPES.register(name, () -> new FluidType(
                FluidType.Properties.create()
                        .descriptionId("fluid.hbm_m." + name)
                        .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                        .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                        .density(density)
                        .viscosity(viscosity)
        ) {
            @Override
            public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                consumer.accept(new IClientFluidTypeExtensions() {
                    private final ResourceLocation STILL = new ResourceLocation(MainRegistry.MOD_ID, "block/fluids/" + name);
                    private final ResourceLocation FLOW = new ResourceLocation(MainRegistry.MOD_ID, "block/fluids/" + name);

                    @Override
                    public ResourceLocation getStillTexture() {
                        return STILL;
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return FLOW;
                    }

                    @Override
                    public int getTintColor() {
                        return tintColor;
                    }
                });
            }
        });

        // Create properties supplier
        Supplier<ForgeFlowingFluid.Properties> propsSupplier = () -> new ForgeFlowingFluid.Properties(
                type,
                () -> entryHolder[0].sourceFluid,
                () -> entryHolder[0].flowingFluid
        );

        // Register source fluid
        RegistryObject<Fluid> source = FLUIDS.register(name + "_source", () -> {
            entryHolder[0].sourceFluid = new ForgeFlowingFluid.Source(propsSupplier.get());
            entryHolder[0].flowingFluid = new ForgeFlowingFluid.Flowing(propsSupplier.get());
            return entryHolder[0].sourceFluid;
        });

        // Register flowing fluid
        RegistryObject<Fluid> flowing = FLUIDS.register(name + "_flowing", () -> {
            return entryHolder[0].flowingFluid;
        });

        entryHolder[0] = new FluidEntry(type, source, flowing);
        FLUID_ENTRIES.put(name, entryHolder[0]);
        return entryHolder[0];
    }

    // Shorthand for standard liquids (density 1000, viscosity 1000)
    private static FluidEntry registerFluid(String name) {
        return registerFluid(name, 1000, 1000, 0xFFFFFFFF);
    }

    // Shorthand for gases (density -100, viscosity 100)
    private static FluidEntry registerGas(String name) {
        return registerFluid(name, -100, 100, 0xFFFFFFFF);
    }

    // Shorthand for heavy liquids (density 1500, viscosity 2000)
    private static FluidEntry registerHeavyFluid(String name) {
        return registerFluid(name, 1500, 2000, 0xFFFFFFFF);
    }

    // Shorthand for plasmas (density -50, viscosity 50)
    private static FluidEntry registerPlasma(String name) {
        return registerFluid(name, -50, 50, 0xFFFFFFFF);
    }

    //=====================================================================================//
    // FLUID REGISTRATIONS - Based on textures in gui/fluids/
    //=====================================================================================//

    // Basic fluids
    public static final FluidEntry WATER = registerFluid("water");
    public static final FluidEntry AIR = registerGas("air");

    // Oils and petroleum products
    public static final FluidEntry CRUDE_OIL = registerHeavyFluid("crude_oil");
    public static final FluidEntry PETROLEUM = registerFluid("petroleum");
    public static final FluidEntry DIESEL = registerFluid("diesel");
    public static final FluidEntry DIESEL_CRACK = registerFluid("diesel_crack");
    public static final FluidEntry DIESEL_CRACK_REFORM = registerFluid("diesel_crack_reform");
    public static final FluidEntry DIESEL_REFORM = registerFluid("diesel_reform");
    public static final FluidEntry GASOLINE = registerFluid("gasoline");
    public static final FluidEntry GASOLINE_LEADED = registerFluid("gasoline_leaded");
    public static final FluidEntry KEROSENE = registerFluid("kerosene");
    public static final FluidEntry KEROSENE_REFORM = registerFluid("kerosene_reform");
    public static final FluidEntry HEAVYOIL = registerHeavyFluid("heavyoil");
    public static final FluidEntry HEAVYOIL_VACUUM = registerHeavyFluid("heavyoil_vacuum");
    public static final FluidEntry LIGHTOIL = registerFluid("lightoil");
    public static final FluidEntry LIGHTOIL_CRACK = registerFluid("lightoil_crack");
    public static final FluidEntry LIGHTOIL_DS = registerFluid("lightoil_ds");
    public static final FluidEntry LIGHTOIL_VACUUM = registerFluid("lightoil_vacuum");
    public static final FluidEntry HEATINGOIL = registerFluid("heatingoil");
    public static final FluidEntry HEATINGOIL_VACUUM = registerFluid("heatingoil_vacuum");
    public static final FluidEntry NAPHTHA = registerFluid("naphtha");
    public static final FluidEntry NAPHTHA_COKER = registerFluid("naphtha_coker");
    public static final FluidEntry NAPHTHA_CRACK = registerFluid("naphtha_crack");
    public static final FluidEntry NAPHTHA_DS = registerFluid("naphtha_ds");
    public static final FluidEntry BITUMEN = registerHeavyFluid("bitumen");
    public static final FluidEntry LUBRICANT = registerFluid("lubricant");
    public static final FluidEntry CRACKOIL = registerFluid("crackoil");
    public static final FluidEntry CRACKOIL_DS = registerFluid("crackoil_ds");
    public static final FluidEntry HOTCRACKOIL = registerFluid("hotcrackoil");
    public static final FluidEntry HOTCRACKOIL_DS = registerFluid("hotcrackoil_ds");
    public static final FluidEntry HOTOIL = registerFluid("hotoil");
    public static final FluidEntry HOTOIL_DS = registerFluid("hotoil_ds");
    public static final FluidEntry OIL_BASE = registerHeavyFluid("oil_base");
    public static final FluidEntry OIL_COKER = registerHeavyFluid("oil_coker");
    public static final FluidEntry OIL_DS = registerHeavyFluid("oil_ds");
    public static final FluidEntry RECLAIMED = registerFluid("reclaimed");
    public static final FluidEntry SLOP = registerHeavyFluid("slop");
    public static final FluidEntry LPG = registerGas("lpg");
    public static final FluidEntry PETROIL = registerFluid("petroil");
    public static final FluidEntry PETROIL_LEADED = registerFluid("petroil_leaded");
    public static final FluidEntry REFORMATE = registerFluid("reformate");
    public static final FluidEntry AROMATICS = registerFluid("aromatics");
    public static final FluidEntry UNSATURATEDS = registerFluid("unsaturateds");
    public static final FluidEntry XYLENE = registerFluid("xylene");
    public static final FluidEntry COALCREOSOTE = registerFluid("coalcreosote");
    public static final FluidEntry COALOIL = registerFluid("coaloil");
    public static final FluidEntry WOODOIL = registerFluid("woodoil");
    public static final FluidEntry FISHOIL = registerFluid("fishoil");
    public static final FluidEntry SUNFLOWEROIL = registerFluid("sunfloweroil");

    // Gases
    public static final FluidEntry GAS = registerGas("gas");
    public static final FluidEntry GAS_COKER = registerGas("gas_coker");
    public static final FluidEntry COALGAS = registerGas("coalgas");
    public static final FluidEntry COALGAS_LEADED = registerGas("coalgas_leaded");
    public static final FluidEntry SYNGAS = registerGas("syngas");
    public static final FluidEntry REFORMGAS = registerGas("reformgas");
    public static final FluidEntry SOURGAS = registerGas("sourgas");
    public static final FluidEntry BIOGAS = registerGas("biogas");
    public static final FluidEntry HYDROGEN = registerGas("hydrogen");
    public static final FluidEntry OXYGEN = registerGas("oxygen");
    public static final FluidEntry CARBONDIOXIDE = registerGas("carbondioxide");
    public static final FluidEntry OXYHYDROGEN = registerGas("oxyhydrogen");
    public static final FluidEntry SMOKE = registerGas("smoke");
    public static final FluidEntry SMOKE_LEADED = registerGas("smoke_leaded");
    public static final FluidEntry SMOKE_POISON = registerGas("smoke_poison");
    public static final FluidEntry WASTEGAS = registerGas("wastegas");
    public static final FluidEntry CHLORINE = registerGas("chlorine");
    public static final FluidEntry PHOSGENE = registerGas("phosgene");
    public static final FluidEntry MUSTARDGAS = registerGas("mustardgas");
    public static final FluidEntry XENON = registerGas("xenon");

    // Nuclear isotopes
    public static final FluidEntry DEUTERIUM = registerGas("deuterium");
    public static final FluidEntry TRITIUM = registerGas("tritium");
    public static final FluidEntry HELIUM3 = registerGas("helium3");
    public static final FluidEntry HELIUM4 = registerGas("helium4");
    public static final FluidEntry UF6 = registerGas("uf6");
    public static final FluidEntry PUF6 = registerGas("puf6");

    // Plasmas
    public static final FluidEntry PLASMA_DT = registerPlasma("plasma_dt");
    public static final FluidEntry PLASMA_HD = registerPlasma("plasma_hd");
    public static final FluidEntry PLASMA_HT = registerPlasma("plasma_ht");
    public static final FluidEntry PLASMA_DH3 = registerPlasma("plasma_dh3");
    public static final FluidEntry PLASMA_XM = registerPlasma("plasma_xm");
    public static final FluidEntry PLASMA_BF = registerPlasma("plasma_bf");

    // Steam variants
    public static final FluidEntry STEAM = registerGas("steam");
    public static final FluidEntry HOTSTEAM = registerGas("hotsteam");
    public static final FluidEntry SUPERHOTSTEAM = registerGas("superhotsteam");
    public static final FluidEntry ULTRAHOTSTEAM = registerGas("ultrahotsteam");
    public static final FluidEntry SPENTSTEAM = registerGas("spentsteam");

    // Water variants
    public static final FluidEntry HEAVYWATER = registerFluid("heavywater");
    public static final FluidEntry HEAVYWATER_HOT = registerFluid("heavywater_hot");
    public static final FluidEntry WATER_BASE = registerFluid("water_base");
    public static final FluidEntry WATER_OPAQUE_BASE = registerFluid("water_opaque_base");

    // Coolants
    public static final FluidEntry COOLANT = registerFluid("coolant");
    public static final FluidEntry COOLANT_HOT = registerFluid("coolant_hot");
    public static final FluidEntry CRYOGEL = registerFluid("cryogel");
    public static final FluidEntry PERFLUOROMETHYL = registerFluid("perfluoromethyl");
    public static final FluidEntry PERFLUOROMETHYL_COLD = registerFluid("perfluoromethyl_cold");
    public static final FluidEntry PERFLUOROMETHYL_HOT = registerFluid("perfluoromethyl_hot");

    // Acids and chemicals
    public static final FluidEntry SULFURIC_ACID = registerFluid("sulfuric_acid");
    public static final FluidEntry NITRIC_ACID = registerFluid("nitric_acid");
    public static final FluidEntry NITROGLYCERIN = registerFluid("nitroglycerin");
    public static final FluidEntry PEROXIDE = registerFluid("peroxide");
    public static final FluidEntry LYE = registerFluid("lye");
    public static final FluidEntry VITRIOL = registerFluid("vitriol");
    public static final FluidEntry SOLVENT = registerFluid("solvent");
    public static final FluidEntry FRACKSOL = registerFluid("fracksol");
    public static final FluidEntry ETHANOL = registerFluid("ethanol");
    public static final FluidEntry BIOFUEL = registerFluid("biofuel");

    // Metals
    public static final FluidEntry MERCURY = registerFluid("mercury", 13500, 1500, 0xFFFFFFFF);
    public static final FluidEntry LEAD = registerFluid("lead", 10000, 2000, 0xFFFFFFFF);
    public static final FluidEntry LEAD_HOT = registerFluid("lead_hot", 10000, 1500, 0xFFFFFFFF);
    public static final FluidEntry SODIUM = registerFluid("sodium", 970, 700, 0xFFFFFFFF);
    public static final FluidEntry SODIUM_HOT = registerFluid("sodium_hot", 850, 500, 0xFFFFFFFF);

    // Salt solutions
    public static final FluidEntry CALCIUM_SOLUTION = registerFluid("calcium_solution");
    public static final FluidEntry CALCIUM_CHLORIDE = registerFluid("calcium_chloride");
    public static final FluidEntry POTASSIUM_CHLORIDE = registerFluid("potassium_chloride");
    public static final FluidEntry CHLOROCALCITE_SOLUTION = registerFluid("chlorocalcite_solution");
    public static final FluidEntry CHLOROCALCITE_MIX = registerFluid("chlorocalcite_mix");
    public static final FluidEntry CHLOROCALCITE_CLEANED = registerFluid("chlorocalcite_cleaned");
    public static final FluidEntry BAUXITE_SOLUTION = registerFluid("bauxite_solution");
    public static final FluidEntry ALUMINA = registerFluid("alumina");
    public static final FluidEntry SODIUM_ALUMINATE = registerFluid("sodium_aluminate");
    public static final FluidEntry REDMUD = registerHeavyFluid("redmud");

    // Schrabidium
    public static final FluidEntry SCHRABIDIC = registerFluid("schrabidic");
    public static final FluidEntry ASCHRAB = registerFluid("aschrab");
    public static final FluidEntry SAS3 = registerFluid("sas3");

    // Balefire
    public static final FluidEntry BALEFIRE = registerFluid("balefire");

    // Advanced Matter
    public static final FluidEntry AMAT = registerFluid("amat");

    // Thorium salt
    public static final FluidEntry THORIUM_SALT = registerFluid("thorium_salt");
    public static final FluidEntry THORIUM_SALT_HOT = registerFluid("thorium_salt_hot");
    public static final FluidEntry THORIUM_SALT_DEPLETED = registerFluid("thorium_salt_depleted");

    // Watz
    public static final FluidEntry WATZ = registerFluid("watz");

    // Miscellaneous
    public static final FluidEntry LAVA = registerHeavyFluid("lava");
    public static final FluidEntry CONCRETE = registerHeavyFluid("concrete");
    public static final FluidEntry BLOOD = registerFluid("blood");
    public static final FluidEntry BLOOD_HOT = registerFluid("blood_hot");
    public static final FluidEntry COLLOID = registerFluid("colloid");
    public static final FluidEntry SMEAR = registerFluid("smear");
    public static final FluidEntry WASTEFLUID = registerFluid("wastefluid");
    public static final FluidEntry RADIOSOLVENT = registerFluid("radiosolvent");
    public static final FluidEntry SALIENT = registerFluid("salient");
    public static final FluidEntry IONGEL = registerFluid("iongel");
    public static final FluidEntry FULLERENE = registerFluid("fullerene");
    public static final FluidEntry NITAN = registerFluid("nitan");
    public static final FluidEntry DHC = registerFluid("dhc");

    // Biological
    public static final FluidEntry EGG = registerFluid("egg");
    public static final FluidEntry CHOLESTEROL = registerFluid("cholesterol");
    public static final FluidEntry ESTRADIOL = registerFluid("estradiol");
    public static final FluidEntry PHEROMONE = registerFluid("pheromone");
    public static final FluidEntry PHEROMONE_M = registerFluid("pheromone_m");
    public static final FluidEntry SEEDSLURRY = registerFluid("seedslurry");

    // Ender
    public static final FluidEntry ENDERJUICE = registerFluid("enderjuice");
    public static final FluidEntry XPJUICE = registerFluid("xpjuice");
    public static final FluidEntry MUG = registerFluid("mug");
    public static final FluidEntry MUG_HOT = registerFluid("mug_hot");

    // Custom base textures
    public static final FluidEntry CUSTOM_WATER = registerFluid("custom_water");
    public static final FluidEntry CUSTOM_OIL = registerHeavyFluid("custom_oil");
    public static final FluidEntry CUSTOM_LAVA = registerHeavyFluid("custom_lava");
    public static final FluidEntry CUSTOM_TOXIN = registerFluid("custom_toxin");
    public static final FluidEntry TOXIN_BASE = registerFluid("toxin_base");

    // Special
    public static final FluidEntry NONE = registerFluid("none");
    public static final FluidEntry DEATH = registerFluid("death");
    public static final FluidEntry PAIN = registerFluid("pain");
    public static final FluidEntry STELLAR_FLUX = registerFluid("stellar_flux");
    public static final FluidEntry BROMIDE = registerFluid("bromide");

    //=====================================================================================//
    // Helper method to get fluid by name
    //=====================================================================================//

    public static FluidEntry getEntry(String name) {
        return FLUID_ENTRIES.get(name);
    }

    public static Map<String, FluidEntry> getAllEntries() {
        return FLUID_ENTRIES;
    }

    //=====================================================================================//
    // Registration method
    //=====================================================================================//

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
    }
}
