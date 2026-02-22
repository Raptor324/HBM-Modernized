package com.hbm_m.api.fluids;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.hbm_m.main.MainRegistry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Registry for fluid identifier GUI: ordered list of fluids and lookup by ResourceLocation.
 * Fluids are in "nice order" for display (basic, oils, gases, etc.).
 */
public final class HbmFluidRegistry {

    private static final List<ModFluids.FluidEntry> ORDERED_FLUIDS = new ArrayList<>();

    static {
        // Build ordered list - same order as ModFluids registration (skip NONE for identifier)
        addAll(
            ModFluids.WATER, ModFluids.AIR,
            ModFluids.CRUDE_OIL, ModFluids.PETROLEUM, ModFluids.DIESEL, ModFluids.DIESEL_CRACK,
            ModFluids.DIESEL_CRACK_REFORM, ModFluids.DIESEL_REFORM, ModFluids.GASOLINE,
            ModFluids.GASOLINE_LEADED, ModFluids.KEROSENE, ModFluids.KEROSENE_REFORM,
            ModFluids.HEAVYOIL, ModFluids.HEAVYOIL_VACUUM, ModFluids.LIGHTOIL, ModFluids.LIGHTOIL_CRACK,
            ModFluids.LIGHTOIL_DS, ModFluids.LIGHTOIL_VACUUM, ModFluids.HEATINGOIL,
            ModFluids.HEATINGOIL_VACUUM, ModFluids.NAPHTHA, ModFluids.NAPHTHA_COKER,
            ModFluids.NAPHTHA_CRACK, ModFluids.NAPHTHA_DS, ModFluids.BITUMEN, ModFluids.LUBRICANT,
            ModFluids.CRACKOIL, ModFluids.CRACKOIL_DS, ModFluids.HOTCRACKOIL, ModFluids.HOTCRACKOIL_DS,
            ModFluids.HOTOIL, ModFluids.HOTOIL_DS, ModFluids.OIL_BASE, ModFluids.OIL_COKER,
            ModFluids.OIL_DS, ModFluids.RECLAIMED, ModFluids.SLOP, ModFluids.LPG,
            ModFluids.PETROIL, ModFluids.PETROIL_LEADED, ModFluids.REFORMATE, ModFluids.AROMATICS,
            ModFluids.UNSATURATEDS, ModFluids.XYLENE, ModFluids.COALCREOSOTE, ModFluids.COALOIL,
            ModFluids.WOODOIL, ModFluids.FISHOIL, ModFluids.SUNFLOWEROIL,
            ModFluids.GAS, ModFluids.GAS_COKER, ModFluids.COALGAS, ModFluids.COALGAS_LEADED,
            ModFluids.SYNGAS, ModFluids.REFORMGAS, ModFluids.SOURGAS, ModFluids.BIOGAS,
            ModFluids.HYDROGEN, ModFluids.OXYGEN, ModFluids.CARBONDIOXIDE, ModFluids.OXYHYDROGEN,
            ModFluids.SMOKE, ModFluids.SMOKE_LEADED, ModFluids.SMOKE_POISON, ModFluids.WASTEGAS,
            ModFluids.CHLORINE, ModFluids.PHOSGENE, ModFluids.MUSTARDGAS, ModFluids.XENON,
            ModFluids.DEUTERIUM, ModFluids.TRITIUM, ModFluids.HELIUM3, ModFluids.HELIUM4,
            ModFluids.UF6, ModFluids.PUF6,
            ModFluids.PLASMA_DT, ModFluids.PLASMA_HD, ModFluids.PLASMA_HT, ModFluids.PLASMA_DH3,
            ModFluids.PLASMA_XM, ModFluids.PLASMA_BF,
            ModFluids.STEAM, ModFluids.HOTSTEAM, ModFluids.SUPERHOTSTEAM, ModFluids.ULTRAHOTSTEAM,
            ModFluids.SPENTSTEAM,
            ModFluids.HEAVYWATER, ModFluids.HEAVYWATER_HOT, ModFluids.WATER_BASE,
            ModFluids.WATER_OPAQUE_BASE,
            ModFluids.COOLANT, ModFluids.COOLANT_HOT, ModFluids.CRYOGEL,
            ModFluids.PERFLUOROMETHYL, ModFluids.PERFLUOROMETHYL_COLD, ModFluids.PERFLUOROMETHYL_HOT,
            ModFluids.SULFURIC_ACID, ModFluids.NITRIC_ACID, ModFluids.NITROGLYCERIN, ModFluids.PEROXIDE,
            ModFluids.LYE, ModFluids.VITRIOL, ModFluids.SOLVENT, ModFluids.FRACKSOL,
            ModFluids.ETHANOL, ModFluids.BIOFUEL,
            ModFluids.MERCURY, ModFluids.LEAD, ModFluids.LEAD_HOT, ModFluids.SODIUM,
            ModFluids.SODIUM_HOT,
            ModFluids.CALCIUM_SOLUTION, ModFluids.CALCIUM_CHLORIDE, ModFluids.POTASSIUM_CHLORIDE,
            ModFluids.CHLOROCALCITE_SOLUTION, ModFluids.CHLOROCALCITE_MIX, ModFluids.CHLOROCALCITE_CLEANED,
            ModFluids.BAUXITE_SOLUTION, ModFluids.ALUMINA, ModFluids.SODIUM_ALUMINATE, ModFluids.REDMUD,
            ModFluids.SCHRABIDIC, ModFluids.ASCHRAB, ModFluids.SAS3,
            ModFluids.BALEFIRE, ModFluids.AMAT,
            ModFluids.THORIUM_SALT, ModFluids.THORIUM_SALT_HOT, ModFluids.THORIUM_SALT_DEPLETED,
            ModFluids.WATZ,
            ModFluids.LAVA, ModFluids.CONCRETE, ModFluids.BLOOD, ModFluids.BLOOD_HOT,
            ModFluids.COLLOID, ModFluids.SMEAR, ModFluids.WASTEFLUID, ModFluids.RADIOSOLVENT,
            ModFluids.SALIENT, ModFluids.IONGEL, ModFluids.FULLERENE, ModFluids.NITAN,
            ModFluids.DHC,
            ModFluids.EGG, ModFluids.CHOLESTEROL, ModFluids.ESTRADIOL, ModFluids.PHEROMONE,
            ModFluids.PHEROMONE_M, ModFluids.SEEDSLURRY,
            ModFluids.ENDERJUICE, ModFluids.XPJUICE, ModFluids.MUG, ModFluids.MUG_HOT,
            ModFluids.CUSTOM_WATER, ModFluids.CUSTOM_OIL, ModFluids.CUSTOM_LAVA,
            ModFluids.CUSTOM_TOXIN, ModFluids.TOXIN_BASE,
            ModFluids.DEATH, ModFluids.PAIN, ModFluids.STELLAR_FLUX, ModFluids.BROMIDE
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
        Fluid f = ForgeRegistries.FLUIDS.getValue(loc);
        return f != null && f != net.minecraft.world.level.material.Fluids.EMPTY
            ? Optional.of(f) : Optional.empty();
    }

    /** Gets ResourceLocation for fluid (source form). */
    public static ResourceLocation getFluidLocation(Fluid fluid) {
        return ForgeRegistries.FLUIDS.getKey(fluid);
    }

    /** Gets fluid name (without _source) for NBT storage. */
    public static String getFluidName(Fluid fluid) {
        ResourceLocation loc = ForgeRegistries.FLUIDS.getKey(fluid);
        if (loc == null) return "none";
        String path = loc.getPath();
        if (path.endsWith("_source")) {
            return path.substring(0, path.length() - 7);
        }
        return path;
    }

    /** Returns tint color for fluid (for identifier icon). */
    public static int getTintColor(String fluidName) {
        return ModFluids.getTintColor(fluidName);
    }

    /** Returns tint color for Fluid. */
    public static int getTintColor(Fluid fluid) {
        return getTintColor(getFluidName(fluid));
    }

    /** Index of fluid in ordered list, or -1. */
    public static int getIndex(Fluid fluid) {
        ResourceLocation loc = ForgeRegistries.FLUIDS.getKey(fluid);
        if (loc == null) return -1;
        for (int i = 0; i < ORDERED_FLUIDS.size(); i++) {
            if (ORDERED_FLUIDS.get(i).getSource() == fluid) {
                return i;
            }
        }
        return -1;
    }

    /** Filter fluids by search substring (case-insensitive). Matches fluid internal name. */
    public static List<ModFluids.FluidEntry> search(String query) {
        if (query == null || query.isEmpty()) {
            return new ArrayList<>(ORDERED_FLUIDS);
        }
        String lower = query.toLowerCase(Locale.US);
        List<ModFluids.FluidEntry> result = new ArrayList<>();
        for (ModFluids.FluidEntry e : ORDERED_FLUIDS) {
            String name = getFluidName(e.getSource());
            if (name.toLowerCase(Locale.US).contains(lower)) {
                result.add(e);
            }
        }
        return result;
    }
}
