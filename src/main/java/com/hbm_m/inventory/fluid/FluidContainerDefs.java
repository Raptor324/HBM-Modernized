package com.hbm_m.inventory.fluid;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 {@code FluidType.addContainers(CD_Canister / CD_Gastank)} aus {@code Fluids.init()}: welche Fluessigkeiten es als
 * Kanister ({@code canister_full}, Farbe des Aufdrucks) bzw. Gasflasche ({@code gas_full}, Flaschen- und Etikettfarbe)
 * gibt. Schluessel sind die Port-Namen der Fluessigkeiten (OIL = crude_oil).
 */
public final class FluidContainerDefs {

    private FluidContainerDefs() {}

    public record CDCanister(int color) {}
    public record CDGastank(int bottleColor, int labelColor) {}

    private static final Map<String, CDCanister> CANISTERS = new HashMap<>();
    private static final Map<String, CDGastank> GASTANKS = new HashMap<>();

    private static void canister(String fluid, int color) { CANISTERS.put(fluid, new CDCanister(color)); }
    private static void gastank(String fluid, int bottle, int label) { GASTANKS.put(fluid, new CDGastank(bottle, label)); }

    static {
        gastank("deuterium", 0x0000FF, 0xFFFFFF);
        gastank("tritium", 0x000099, 0xE9FFAA);
        canister("crude_oil", 0x424242);
        canister("heavyoil", 0x513F39);
        canister("bitumen", 0x5A5877);
        canister("smear", 0x624F3B);
        canister("heatingoil", 0x694235);
        canister("reclaimed", 0xF65723);
        canister("petroil", 0x2369F6);
        canister("lubricant", 0xF1CC05);
        canister("naphtha", 0x5F6D44);
        canister("diesel", 0xFF2C2C);
        canister("lightoil", 0xB46B52);
        canister("kerosene", 0xFF377D);
        gastank("gas", 0xFF4545, 0xFFE97F);
        gastank("petroleum", 0x5E7CFF, 0xFFE97F);
        gastank("biogas", 0xC8FF1F, 0x303030);
        canister("biofuel", 0x9EB623);
        canister("nitan", 0x6B238C);
        gastank("hydrogen", 0x4286f4, 0xffffff);
        gastank("oxygen", 0x98bdf9, 0xffffff);
        gastank("xenon", 0x8C21FF, 0x303030);
        canister("gasoline", 0x2F7747);
        canister("coalgas", 0x2E155F);
        canister("fracksol", 0x4F887F);
        gastank("helium3", 0xFD631F, 0xffffff);
        canister("ethanol", 0xEAFFF3);
        canister("crackoil", 0x424242);
        canister("coaloil", 0x424242);
        canister("naphtha_crack", 0x5F6D44);
        canister("lightoil_crack", 0xB46B52);
        canister("diesel_crack", 0xFF2C2C);
        gastank("aromatics", 0x68A09A, 0xEDCF27);
        gastank("unsaturateds", 0x628FAE, 0xEDCF27);
        canister("petroil_leaded", 0x2331F6);
        canister("gasoline_leaded", 0x2F775A);
        canister("coalgas_leaded", 0x1E155F);
        canister("woodoil", 0xBF7E4F);
        canister("coalcreosote", 0x285A3F);
        canister("seedslurry", 0x7CC35E);
        canister("solvent", 0xE4E3EF);
        gastank("syngas", 0xFFFFFF, 0x131313);
        gastank("chlorine", 0xBAB572, 0x887B34);
        canister("heavyoil_vacuum", 0x513F39);
        canister("reformate", 0xD180D6);
        canister("lightoil_vacuum", 0xB46B52);
        gastank("sourgas", 0xC9BE0D, 0x303030);
        canister("xylene", 0xA380D6);
        canister("heatingoil_vacuum", 0x694235);
        canister("diesel_reform", 0xFFC500);
        canister("diesel_crack_reform", 0xFFC500);
        canister("kerosene_reform", 0xFF377D);
        gastank("reformgas", 0x9392FF, 0xFFB992);
        gastank("phosgene", 0xCFC4A4, 0x361414);
        gastank("mustardgas", 0xBAB572, 0x361414);
        gastank("helium4", 0xFD631F, 0xffff00);
        canister("oil_ds", 0x424242);
        canister("crackoil_ds", 0x424242);
        canister("naphtha_ds", 0x5F6D44);
        canister("lightoil_ds", 0xB46B52);
        gastank("flue", 0xFF4545, 0xFFE97F);
    }

    private static String name(Fluid fluid) {
        return FluidType.forFluid(fluid).getName();
    }

    @Nullable
    public static CDCanister getCanister(Fluid fluid) {
        return fluid == null ? null : CANISTERS.get(name(fluid));
    }

    @Nullable
    public static CDGastank getGastank(Fluid fluid) {
        return fluid == null ? null : GASTANKS.get(name(fluid));
    }
}
