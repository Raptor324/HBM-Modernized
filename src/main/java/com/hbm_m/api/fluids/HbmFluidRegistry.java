package com.hbm_m.api.fluids;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

/**
 * Registry for fluid identifier GUI: ordered list of fluids and lookup by ResourceLocation.
 * Fluids are in "nice order" for display (basic, oils, gases, etc.).
 */
public final class HbmFluidRegistry {

    private static final List<ModFluids.FluidEntry> ORDERED_FLUIDS = new ArrayList<>();

    static {
        // 1:1 Fluids.metaOrder (getInNiceOrder) des Originals, ohne NONE; Port-Basistexturen (oil_base usw.) sind keine Typen
        addAll(
            // vanilla
            ModFluids.AIR, ModFluids.AIRBLAST, ModFluids.WATER, ModFluids.HEAVYWATER,
            ModFluids.HEAVYWATER_HOT, ModFluids.LAVA,
            // steams
            ModFluids.STEAM, ModFluids.HOTSTEAM, ModFluids.SUPERHOTSTEAM, ModFluids.ULTRAHOTSTEAM,
            ModFluids.SPENTSTEAM,
            // coolants
            ModFluids.CARBONDIOXIDE, ModFluids.COOLANT, ModFluids.COOLANT_HOT,
            ModFluids.PERFLUOROMETHYL, ModFluids.PERFLUOROMETHYL_COLD, ModFluids.PERFLUOROMETHYL_HOT,
            ModFluids.CRYOGEL, ModFluids.MUG, ModFluids.MUG_HOT, ModFluids.BLOOD,
            ModFluids.BLOOD_HOT, ModFluids.SODIUM, ModFluids.SODIUM_HOT, ModFluids.LEAD,
            ModFluids.LEAD_HOT, ModFluids.THORIUM_SALT, ModFluids.THORIUM_SALT_HOT,
            ModFluids.THORIUM_SALT_DEPLETED,
            // pure elements, cyogenic gasses
            ModFluids.HYDROGEN, ModFluids.DEUTERIUM, ModFluids.TRITIUM, ModFluids.HELIUM3,
            ModFluids.HELIUM4, ModFluids.OXYGEN, ModFluids.XENON, ModFluids.CHLORINE,
            ModFluids.MERCURY,
            // oils, fuels
            ModFluids.CRUDE_OIL, ModFluids.OIL_DS, ModFluids.CRACKOIL, ModFluids.CRACKOIL_DS,
            ModFluids.COALOIL, ModFluids.OIL_COKER, ModFluids.HOTOIL, ModFluids.HOTOIL_DS,
            ModFluids.HOTCRACKOIL, ModFluids.HOTCRACKOIL_DS, ModFluids.HEAVYOIL,
            ModFluids.HEAVYOIL_VACUUM, ModFluids.NAPHTHA, ModFluids.NAPHTHA_DS,
            ModFluids.NAPHTHA_CRACK, ModFluids.NAPHTHA_COKER, ModFluids.REFORMATE,
            ModFluids.LIGHTOIL, ModFluids.LIGHTOIL_DS, ModFluids.LIGHTOIL_CRACK,
            ModFluids.LIGHTOIL_VACUUM, ModFluids.BITUMEN, ModFluids.SMEAR, ModFluids.HEATINGOIL,
            ModFluids.HEATINGOIL_VACUUM, ModFluids.RECLAIMED, ModFluids.LUBRICANT, ModFluids.FLUE,
            ModFluids.GAS, ModFluids.GAS_COKER, ModFluids.PETROLEUM, ModFluids.SOURGAS,
            ModFluids.LPG, ModFluids.SYNGAS, ModFluids.OXYHYDROGEN, ModFluids.AROMATICS,
            ModFluids.UNSATURATEDS, ModFluids.XYLENE, ModFluids.REFORMGAS, ModFluids.DIESEL,
            ModFluids.DIESEL_REFORM, ModFluids.DIESEL_CRACK, ModFluids.DIESEL_CRACK_REFORM,
            ModFluids.KEROSENE, ModFluids.KEROSENE_REFORM, ModFluids.PETROIL,
            ModFluids.PETROIL_LEADED, ModFluids.GASOLINE, ModFluids.GASOLINE_LEADED,
            ModFluids.COALGAS, ModFluids.COALGAS_LEADED, ModFluids.COALCREOSOTE, ModFluids.WOODOIL,
            ModFluids.BIOGAS, ModFluids.BIOFUEL, ModFluids.ETHANOL, ModFluids.FISHOIL,
            ModFluids.SUNFLOWEROIL, ModFluids.NITAN, ModFluids.DHC, ModFluids.BALEFIRE,
            // processing fluids
            ModFluids.SALIENT, ModFluids.SEEDSLURRY, ModFluids.COLLOID, ModFluids.VITRIOL,
            ModFluids.SLOP, ModFluids.IONGEL, ModFluids.PEROXIDE, ModFluids.SULFURIC_ACID,
            ModFluids.NITRIC_ACID, ModFluids.SOLVENT, ModFluids.RADIOSOLVENT, ModFluids.SCHRABIDIC,
            ModFluids.UF6, ModFluids.PUF6, ModFluids.SAS3, ModFluids.PAIN, ModFluids.DEATH,
            ModFluids.WATZ, ModFluids.REDMUD, ModFluids.FULLERENE, ModFluids.EGG,
            ModFluids.CHOLESTEROL, ModFluids.CHLOROCALCITE_SOLUTION, ModFluids.CHLOROCALCITE_MIX,
            ModFluids.CHLOROCALCITE_CLEANED, ModFluids.POTASSIUM_CHLORIDE,
            ModFluids.CALCIUM_CHLORIDE, ModFluids.CALCIUM_SOLUTION, ModFluids.SODIUM_ALUMINATE,
            ModFluids.BAUXITE_SOLUTION, ModFluids.ALUMINA, ModFluids.TITANIUM_TETRACHLORIDE, ModFluids.CONCRETE,
            // solutions and working fluids
            ModFluids.FRACKSOL, ModFluids.LYE,
            // the fun guys
            ModFluids.PHOSGENE, ModFluids.MUSTARDGAS, ModFluids.ESTRADIOL, ModFluids.NITROGLYCERIN,
            // antimatter
            ModFluids.AMAT, ModFluids.ASCHRAB,
            // nuclear waste
            ModFluids.WASTEFLUID, ModFluids.WASTEGAS,
            // garbage
            ModFluids.XPJUICE, ModFluids.ENDERJUICE,
            // plasma-esque
            ModFluids.STELLAR_FLUX,
            // plasma
            ModFluids.PLASMA_DT, ModFluids.PLASMA_HD, ModFluids.PLASMA_HT, ModFluids.PLASMA_DH3,
            ModFluids.PLASMA_XM, ModFluids.PLASMA_BF,
            // smoke
            ModFluids.SMOKE, ModFluids.SMOKE_LEADED, ModFluids.SMOKE_POISON,
            // bug meth
            ModFluids.PHEROMONE, ModFluids.PHEROMONE_M
        );
    }

    private static void addAll(ModFluids.FluidEntry... entries) {
        for (ModFluids.FluidEntry e : entries) {
            if (e != null) {
                ORDERED_FLUIDS.add(e);
            }
        }
    }

    private HbmFluidRegistry() {}

    /** Returns fluids in display order for GUI. Excludes NONE. */
    public static List<ModFluids.FluidEntry> getOrderedFluids() {
        return ORDERED_FLUIDS;
    }

    /** Returns fluid by index in ordered list, or empty. */
    public static Optional<Fluid> getFluidByIndex(int index) {
        if (index < 0 || index >= ORDERED_FLUIDS.size()) {
            return Optional.empty();
        }
        return Optional.of(ORDERED_FLUIDS.get(index).getSource());
    }

    /** Returns FluidEntry by index. */
    public static Optional<ModFluids.FluidEntry> getEntryByIndex(int index) {
        if (index < 0 || index >= ORDERED_FLUIDS.size()) {
            return Optional.empty();
        }
        return Optional.of(ORDERED_FLUIDS.get(index));
    }

    /** Resolves fluid by ResourceLocation (e.g. "hbm_m:water_source"). */
    public static Optional<Fluid> getFluidByLocation(ResourceLocation loc) {
        Fluid f = BuiltInRegistries.FLUID.get(loc);
        return f != null && f != net.minecraft.world.level.material.Fluids.EMPTY
            ? Optional.of(f) : Optional.empty();
    }

    /** Gets ResourceLocation for fluid (source form). */
    public static ResourceLocation getFluidLocation(Fluid fluid) {
        return BuiltInRegistries.FLUID.getKey(fluid);
    }

    /**
     * Имя жидкости (без суффикса {@code _source}) для хранения в NBT.
     * Делегирует в {@link FluidType#getName()} — единый источник правды.
     */
    public static String getFluidName(Fluid fluid) {
        return FluidType.forFluid(fluid).getName();
    }

    /** Тинт-цвет жидкости по её внутреннему имени; единственное место хранения — {@link ModFluids}. */
    public static int getTintColor(String fluidName) {
        return ModFluids.getTintColor(fluidName);
    }

    /**
     * Fluidfarbe ({@code FluidType.getColor()}, Rohr-/Textfarbe). Der GUI-Tint des Originals ist {@link FluidType#getTint()}.
     */
    public static int getTintColor(Fluid fluid) {
        return FluidType.forFluid(fluid).getColor();
    }

    /** Index of fluid in ordered list, or -1. */
    public static int getIndex(Fluid fluid) {
        ResourceLocation loc = BuiltInRegistries.FLUID.getKey(fluid);
        if (loc == null) return -1;
        for (int i = 0; i < ORDERED_FLUIDS.size(); i++) {
            if (ORDERED_FLUIDS.get(i).getSource() == fluid) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Filter fluids by search substring (case-insensitive). Matches fluid internal name.
     * {@link ModFluids#NONE} is prepended when the query is empty or when the internal name {@code none} matches.
     */
    public static List<ModFluids.FluidEntry> search(String query) {
        if (query == null || query.isEmpty()) {
            List<ModFluids.FluidEntry> result = new ArrayList<>(ORDERED_FLUIDS.size() + 1);
            result.add(ModFluids.NONE);
            result.addAll(ORDERED_FLUIDS);
            return result;
        }
        String lower = query.toLowerCase(Locale.US);
        List<ModFluids.FluidEntry> result = new ArrayList<>();
        String noneName = getFluidName(ModFluids.NONE.getSource());
        if (noneName.toLowerCase(Locale.US).contains(lower)) {
            result.add(ModFluids.NONE);
        }
        for (ModFluids.FluidEntry e : ORDERED_FLUIDS) {
            String name = getFluidName(e.getSource());
            if (name.toLowerCase(Locale.US).contains(lower)) {
                result.add(e);
            }
        }
        return result;
    }
}
