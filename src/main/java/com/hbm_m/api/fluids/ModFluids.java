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

    /** Tint colors for fluid identifier icons and GUI. Populated by registerFluid. */
    private static final Map<String, Integer> TINT_COLORS = new HashMap<>();

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
        TINT_COLORS.put(name, tintColor);
        return entryHolder[0];
    }

    // Shorthand for standard liquids (density 1000, viscosity 1000)
    private static FluidEntry registerFluid(String name, int color) {
        return registerFluid(name, 1000, 1000, color);
    }

    // Shorthand for gases (density -100, viscosity 100)
    private static FluidEntry registerGas(String name, int color) {
        return registerFluid(name, -100, 100, color);
    }

    // Shorthand for heavy liquids (density 1500, viscosity 2000)
    private static FluidEntry registerHeavyFluid(String name, int color) {
        return registerFluid(name, 1500, 2000, color);
    }

    // Shorthand for plasmas (density -50, viscosity 50)
    private static FluidEntry registerPlasma(String name, int color) {
        return registerFluid(name, -50, 50, color);
    }

    //=====================================================================================//
    // FLUID REGISTRATIONS - Based on textures in gui/fluids/
    //=====================================================================================//

    // Basic fluids (colors from HBM 1.7.10 FluidType)
    public static final FluidEntry WATER = registerFluid("water", 0x3333FF);
    public static final FluidEntry AIR = registerGas("air", 0xE7EAEB);

    // Oils and petroleum products
    public static final FluidEntry CRUDE_OIL = registerHeavyFluid("crude_oil", 0x020202);
    public static final FluidEntry PETROLEUM = registerFluid("petroleum", 0x7cb7c9);
    public static final FluidEntry DIESEL = registerFluid("diesel", 0xf2eed5);
    public static final FluidEntry DIESEL_CRACK = registerFluid("diesel_crack", 0xf2eed5);
    public static final FluidEntry DIESEL_CRACK_REFORM = registerFluid("diesel_crack_reform", 0xCDC3CC);
    public static final FluidEntry DIESEL_REFORM = registerFluid("diesel_reform", 0xCDC3C6);
    public static final FluidEntry GASOLINE = registerFluid("gasoline", 0x445772);
    public static final FluidEntry GASOLINE_LEADED = registerFluid("gasoline_leaded", 0x445772);
    public static final FluidEntry KEROSENE = registerFluid("kerosene", 0xffa5d2);
    public static final FluidEntry KEROSENE_REFORM = registerFluid("kerosene_reform", 0xFFA5F3);
    public static final FluidEntry HEAVYOIL = registerHeavyFluid("heavyoil", 0x141312);
    public static final FluidEntry HEAVYOIL_VACUUM = registerHeavyFluid("heavyoil_vacuum", 0x131214);
    public static final FluidEntry LIGHTOIL = registerFluid("lightoil", 0x8c7451);
    public static final FluidEntry LIGHTOIL_CRACK = registerFluid("lightoil_crack", 0x8c7451);
    public static final FluidEntry LIGHTOIL_DS = registerFluid("lightoil_ds", 0x63543E);
    public static final FluidEntry LIGHTOIL_VACUUM = registerFluid("lightoil_vacuum", 0x8C8851);
    public static final FluidEntry HEATINGOIL = registerFluid("heatingoil", 0x211806);
    public static final FluidEntry HEATINGOIL_VACUUM = registerFluid("heatingoil_vacuum", 0x211D06);
    public static final FluidEntry NAPHTHA = registerFluid("naphtha", 0x595744);
    public static final FluidEntry NAPHTHA_COKER = registerFluid("naphtha_coker", 0x495944);
    public static final FluidEntry NAPHTHA_CRACK = registerFluid("naphtha_crack", 0x595744);
    public static final FluidEntry NAPHTHA_DS = registerFluid("naphtha_ds", 0x63614E);
    public static final FluidEntry BITUMEN = registerHeavyFluid("bitumen", 0x1f2426);
    public static final FluidEntry LUBRICANT = registerFluid("lubricant", 0x606060);
    public static final FluidEntry CRACKOIL = registerFluid("crackoil", 0x020202);
    public static final FluidEntry CRACKOIL_DS = registerFluid("crackoil_ds", 0x2A1C11);
    public static final FluidEntry HOTCRACKOIL = registerFluid("hotcrackoil", 0x300900);
    public static final FluidEntry HOTCRACKOIL_DS = registerFluid("hotcrackoil_ds", 0x3A1A28);
    public static final FluidEntry HOTOIL = registerFluid("hotoil", 0x300900);
    public static final FluidEntry HOTOIL_DS = registerFluid("hotoil_ds", 0x3F180F);
    public static final FluidEntry OIL_BASE = registerHeavyFluid("oil_base", 0x020202);
    public static final FluidEntry OIL_COKER = registerHeavyFluid("oil_coker", 0x001802);
    public static final FluidEntry OIL_DS = registerHeavyFluid("oil_ds", 0x121212);
    public static final FluidEntry RECLAIMED = registerFluid("reclaimed", 0x332b22);
    public static final FluidEntry SLOP = registerHeavyFluid("slop", 0x929D45);
    public static final FluidEntry LPG = registerGas("lpg", 0x4747EA);
    public static final FluidEntry PETROIL = registerFluid("petroil", 0x44413d);
    public static final FluidEntry PETROIL_LEADED = registerFluid("petroil_leaded", 0x44413d);
    public static final FluidEntry REFORMATE = registerFluid("reformate", 0x835472);
    public static final FluidEntry AROMATICS = registerFluid("aromatics", 0x68A09A);
    public static final FluidEntry UNSATURATEDS = registerFluid("unsaturateds", 0x628FAE);
    public static final FluidEntry XYLENE = registerFluid("xylene", 0x5C4E76);
    public static final FluidEntry COALCREOSOTE = registerFluid("coalcreosote", 0x51694F);
    public static final FluidEntry COALOIL = registerFluid("coaloil", 0x020202);
    public static final FluidEntry WOODOIL = registerFluid("woodoil", 0x847D54);
    public static final FluidEntry FISHOIL = registerFluid("fishoil", 0x4B4A45);
    public static final FluidEntry SUNFLOWEROIL = registerFluid("sunfloweroil", 0xCBAD45);

    // Gases
    public static final FluidEntry GAS = registerGas("gas", 0xfffeed);
    public static final FluidEntry GAS_COKER = registerGas("gas_coker", 0xDEF4CA);
    public static final FluidEntry COALGAS = registerGas("coalgas", 0x445772);
    public static final FluidEntry COALGAS_LEADED = registerGas("coalgas_leaded", 0x445772);
    public static final FluidEntry SYNGAS = registerGas("syngas", 0x131313);
    public static final FluidEntry REFORMGAS = registerGas("reformgas", 0x6362AE);
    public static final FluidEntry SOURGAS = registerGas("sourgas", 0xC9BE0D);
    public static final FluidEntry BIOGAS = registerGas("biogas", 0xbfd37c);
    public static final FluidEntry HYDROGEN = registerGas("hydrogen", 0x4286f4);
    public static final FluidEntry OXYGEN = registerGas("oxygen", 0x98bdf9);
    public static final FluidEntry CARBONDIOXIDE = registerGas("carbondioxide", 0x404040);
    public static final FluidEntry OXYHYDROGEN = registerGas("oxyhydrogen", 0x483FC1);
    public static final FluidEntry SMOKE = registerGas("smoke", 0x808080);
    public static final FluidEntry SMOKE_LEADED = registerGas("smoke_leaded", 0x808080);
    public static final FluidEntry SMOKE_POISON = registerGas("smoke_poison", 0x808080);
    public static final FluidEntry WASTEGAS = registerGas("wastegas", 0xB8B8B8);
    public static final FluidEntry CHLORINE = registerGas("chlorine", 0xBAB572);
    public static final FluidEntry PHOSGENE = registerGas("phosgene", 0xCFC4A4);
    public static final FluidEntry MUSTARDGAS = registerGas("mustardgas", 0xBAB572);
    public static final FluidEntry XENON = registerGas("xenon", 0xba45e8);

    // Nuclear isotopes
    public static final FluidEntry DEUTERIUM = registerGas("deuterium", 0x0000FF);
    public static final FluidEntry TRITIUM = registerGas("tritium", 0x000099);
    public static final FluidEntry HELIUM3 = registerGas("helium3", 0xFCF0C4);
    public static final FluidEntry HELIUM4 = registerGas("helium4", 0xE54B0A);
    public static final FluidEntry UF6 = registerGas("uf6", 0xD1CEBE);
    public static final FluidEntry PUF6 = registerGas("puf6", 0x4C4C4C);

    // Plasmas
    public static final FluidEntry PLASMA_DT = registerPlasma("plasma_dt", 0xF7AFDE);
    public static final FluidEntry PLASMA_HD = registerPlasma("plasma_hd", 0xF0ADF4);
    public static final FluidEntry PLASMA_HT = registerPlasma("plasma_ht", 0xD1ABF2);
    public static final FluidEntry PLASMA_DH3 = registerPlasma("plasma_dh3", 0xFF83AA);
    public static final FluidEntry PLASMA_XM = registerPlasma("plasma_xm", 0xC6A5FF);
    public static final FluidEntry PLASMA_BF = registerPlasma("plasma_bf", 0xA7F1A3);

    // Steam variants
    public static final FluidEntry STEAM = registerGas("steam", 0xe5e5e5);
    public static final FluidEntry HOTSTEAM = registerGas("hotsteam", 0xE7D6D6);
    public static final FluidEntry SUPERHOTSTEAM = registerGas("superhotsteam", 0xE7B7B7);
    public static final FluidEntry ULTRAHOTSTEAM = registerGas("ultrahotsteam", 0xE39393);
    public static final FluidEntry SPENTSTEAM = registerGas("spentsteam", 0x445772);

    // Water variants
    public static final FluidEntry HEAVYWATER = registerFluid("heavywater", 0x00a0b0);
    public static final FluidEntry HEAVYWATER_HOT = registerFluid("heavywater_hot", 0x4D007B);
    public static final FluidEntry WATER_BASE = registerFluid("water_base", 0x3333FF);
    public static final FluidEntry WATER_OPAQUE_BASE = registerFluid("water_opaque_base", 0x3333FF);

    // Coolants
    public static final FluidEntry COOLANT = registerFluid("coolant", 0xd8fcff);
    public static final FluidEntry COOLANT_HOT = registerFluid("coolant_hot", 0x99525E);
    public static final FluidEntry CRYOGEL = registerFluid("cryogel", 0x32ffff);
    public static final FluidEntry PERFLUOROMETHYL = registerFluid("perfluoromethyl", 0xBDC8DC);
    public static final FluidEntry PERFLUOROMETHYL_COLD = registerFluid("perfluoromethyl_cold", 0x99DADE);
    public static final FluidEntry PERFLUOROMETHYL_HOT = registerFluid("perfluoromethyl_hot", 0xB899DE);

    // Acids and chemicals
    public static final FluidEntry SULFURIC_ACID = registerFluid("sulfuric_acid", 0xB0AA64);
    public static final FluidEntry NITRIC_ACID = registerFluid("nitric_acid", 0xBB7A1E);
    public static final FluidEntry NITROGLYCERIN = registerFluid("nitroglycerin", 0x92ACA6);
    public static final FluidEntry PEROXIDE = registerFluid("peroxide", 0xfff7aa);
    public static final FluidEntry LYE = registerFluid("lye", 0xFFECCC);
    public static final FluidEntry VITRIOL = registerFluid("vitriol", 0x6E5222);
    public static final FluidEntry SOLVENT = registerFluid("solvent", 0xE4E3EF);
    public static final FluidEntry FRACKSOL = registerFluid("fracksol", 0x798A6B);
    public static final FluidEntry ETHANOL = registerFluid("ethanol", 0xe0ffff);
    public static final FluidEntry BIOFUEL = registerFluid("biofuel", 0xeef274);

    // Metals
    public static final FluidEntry MERCURY = registerFluid("mercury", 13500, 1500, 0x808080);
    public static final FluidEntry LEAD = registerFluid("lead", 10000, 2000, 0x666672);
    public static final FluidEntry LEAD_HOT = registerFluid("lead_hot", 10000, 1500, 0x776563);
    public static final FluidEntry SODIUM = registerFluid("sodium", 970, 700, 0xCCD4D5);
    public static final FluidEntry SODIUM_HOT = registerFluid("sodium_hot", 850, 500, 0xE2ADC1);

    // Salt solutions
    public static final FluidEntry CALCIUM_SOLUTION = registerFluid("calcium_solution", 0x808080);
    public static final FluidEntry CALCIUM_CHLORIDE = registerFluid("calcium_chloride", 0x808080);
    public static final FluidEntry POTASSIUM_CHLORIDE = registerFluid("potassium_chloride", 0x808080);
    public static final FluidEntry CHLOROCALCITE_SOLUTION = registerFluid("chlorocalcite_solution", 0x808080);
    public static final FluidEntry CHLOROCALCITE_MIX = registerFluid("chlorocalcite_mix", 0x808080);
    public static final FluidEntry CHLOROCALCITE_CLEANED = registerFluid("chlorocalcite_cleaned", 0x808080);
    public static final FluidEntry BAUXITE_SOLUTION = registerFluid("bauxite_solution", 0xE2560F);
    public static final FluidEntry ALUMINA = registerFluid("alumina", 0xDDFFFF);
    public static final FluidEntry SODIUM_ALUMINATE = registerFluid("sodium_aluminate", 0xFFD191);
    public static final FluidEntry REDMUD = registerHeavyFluid("redmud", 0xD85638);

    // Schrabidium
    public static final FluidEntry SCHRABIDIC = registerFluid("schrabidic", 0x006B6B);
    public static final FluidEntry ASCHRAB = registerFluid("aschrab", 0xb50000);
    public static final FluidEntry SAS3 = registerFluid("sas3", 0x4ffffc);

    // Balefire
    public static final FluidEntry BALEFIRE = registerFluid("balefire", 0x28e02e);

    // Advanced Matter
    public static final FluidEntry AMAT = registerFluid("amat", 0x010101);

    // Thorium salt
    public static final FluidEntry THORIUM_SALT = registerFluid("thorium_salt", 0x7A5542);
    public static final FluidEntry THORIUM_SALT_HOT = registerFluid("thorium_salt_hot", 0x3E3627);
    public static final FluidEntry THORIUM_SALT_DEPLETED = registerFluid("thorium_salt_depleted", 0x302D1C);

    // Watz
    public static final FluidEntry WATZ = registerFluid("watz", 0x86653E);

    // Miscellaneous
    public static final FluidEntry LAVA = registerHeavyFluid("lava", 0xFF3300);
    public static final FluidEntry CONCRETE = registerHeavyFluid("concrete", 0xA2A2A2);
    public static final FluidEntry BLOOD = registerFluid("blood", 0xB22424);
    public static final FluidEntry BLOOD_HOT = registerFluid("blood_hot", 0xF22419);
    public static final FluidEntry COLLOID = registerFluid("colloid", 0x787878);
    public static final FluidEntry SMEAR = registerFluid("smear", 0x190f01);
    public static final FluidEntry WASTEFLUID = registerFluid("wastefluid", 0x544400);
    public static final FluidEntry RADIOSOLVENT = registerFluid("radiosolvent", 0xA4D7DD);
    public static final FluidEntry SALIENT = registerFluid("salient", 0x457F2D);
    public static final FluidEntry IONGEL = registerFluid("iongel", 0xB8FFFF);
    public static final FluidEntry FULLERENE = registerFluid("fullerene", 0xFF7FED);
    public static final FluidEntry NITAN = registerFluid("nitan", 0x8018ad);
    public static final FluidEntry DHC = registerFluid("dhc", 0xD2AFFF);

    // Biological
    public static final FluidEntry EGG = registerFluid("egg", 0xD2C273);
    public static final FluidEntry CHOLESTEROL = registerFluid("cholesterol", 0xD6D2BD);
    public static final FluidEntry ESTRADIOL = registerFluid("estradiol", 0xCDD5D8);
    public static final FluidEntry PHEROMONE = registerFluid("pheromone", 0x5FA6E8);
    public static final FluidEntry PHEROMONE_M = registerFluid("pheromone_m", 0x48C9B0);
    public static final FluidEntry SEEDSLURRY = registerFluid("seedslurry", 0x7CC35E);

    // Ender
    public static final FluidEntry ENDERJUICE = registerFluid("enderjuice", 0x127766);
    public static final FluidEntry XPJUICE = registerFluid("xpjuice", 0xBBFF09);
    public static final FluidEntry MUG = registerFluid("mug", 0x4B2D28);
    public static final FluidEntry MUG_HOT = registerFluid("mug_hot", 0x6B2A20);

    // Custom base textures
    public static final FluidEntry CUSTOM_WATER = registerFluid("custom_water", 0x3333FF);
    public static final FluidEntry CUSTOM_OIL = registerHeavyFluid("custom_oil", 0x020202);
    public static final FluidEntry CUSTOM_LAVA = registerHeavyFluid("custom_lava", 0xFF3300);
    public static final FluidEntry CUSTOM_TOXIN = registerFluid("custom_toxin", 0x544400);
    public static final FluidEntry TOXIN_BASE = registerFluid("toxin_base", 0x544400);

    // Special
    public static final FluidEntry NONE = registerFluid("none", 0x888888);
    public static final FluidEntry DEATH = registerFluid("death", 0x717A88);
    public static final FluidEntry PAIN = registerFluid("pain", 0x938541);
    public static final FluidEntry STELLAR_FLUX = registerFluid("stellar_flux", 0xE300FF);
    public static final FluidEntry BROMIDE = registerFluid("bromide", 0x808080);

    //=====================================================================================//
    // Helper method to get fluid by name
    //=====================================================================================//

    public static FluidEntry getEntry(String name) {
        return FLUID_ENTRIES.get(name);
    }

    public static Map<String, FluidEntry> getAllEntries() {
        return FLUID_ENTRIES;
    }

    /** Returns tint color for fluid identifier/GUI. Default 0xFFFFFF if not set. */
    public static int getTintColor(String name) {
        return TINT_COLORS.getOrDefault(name, 0xFFFFFF);
    }

    //=====================================================================================//
    // Registration method
    //=====================================================================================//

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
    }
}
