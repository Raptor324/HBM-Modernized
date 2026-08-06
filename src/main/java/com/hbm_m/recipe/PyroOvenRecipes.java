package com.hbm_m.recipe;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.ModFluids.FluidEntry;
import com.hbm_m.inventory.fluid.trait.FT_Flammable;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.industrial.ItemBedrockOreGraded;
import com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.tags_and_tiers.ModPowders;
import com.hbm_m.worldgen.BedrockOreDensity.Type;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;

/**
 * Direkter Java-Port von {@code PyroOvenRecipes} (1.7.10 Original, {@code
 * com.hbm.inventory.recipes.PyroOvenRecipes}). Feste Liste statt JSON-Rezeptsystem, analog zu
 * {@link CokerRecipes}. Rezepte werden in Registrierreihenfolge geprueft, das erste passende
 * gewinnt - 1:1 wie im Original ({@code getMatchingRecipe()}).
 * <p>
 * SCOPE-Entscheidungen (dokumentierte Luecken):
 * <ul>
 *   <li>Die ~25 "Solid Fuel"-Auto-Rezepte werden wie im Original aus der {@code
 *   FT_Flammable}-Brennwert-Eigenschaft berechnet - fehlt einem Fluid dieser Trait, entfaellt sein
 *   Rezept (statt eines Original-Crashs bei fehlendem Trait).</li>
 *   <li>Das Original nutzt OreDictionary-Sammelbegriffe ({@code COAL.gem()}, {@code ANY_COKE.gem()},
 *   {@code ANY_TAR.any()}) - dieser Port hat kein Oredict-Aequivalent, daher werden konkrete
 *   Einzelitems verwendet (z.B. nur {@link ModItems#COKE_PETROLEUM} statt "irgendein Koks", da
 *   Kohle-/Braunkohle-Koks in diesem Port noch nicht existieren). Das "Soot aus Teer"-Rezept
 *   entfaellt komplett (kein Teer-Sammelbegriff und keine Ruß-Pulver-Variante in diesem Port).</li>
 *   <li>Die 30 Bedrock-Erz-Roest-Rezepte (6 {@link Type}-Werte x 5 Grade-Paare) nutzen die bereits
 *   vorhandene {@link ItemBedrockOreGraded}-Infrastruktur per Registry-Namenslookup (Item-IDs folgen
 *   1:1 dem Muster {@code bedrock_ore_<grade>_<type>}).</li>
 * </ul>
 */
public final class PyroOvenRecipes {

    private PyroOvenRecipes() {}

    public record Recipe(
            Fluid inputFluid, int inputFluidMb,
            Item inputItem, int inputItemCount,
            ItemStack outputItem,
            Fluid outputFluid, int outputFluidMb,
            int duration) {}

    private static final List<Recipe> RECIPES = new ArrayList<>();

    // ── Builder-Helfer ───────────────────────────────────────────────────────

    private static void addFluidToItem(Fluid inFluid, int inMb, ItemStack out, int duration) {
        RECIPES.add(new Recipe(inFluid, inMb, null, 0, out, null, 0, duration));
    }

    private static void addFluidItemToFluid(Fluid inFluid, int inMb, Item inItem, int inCount, Fluid outFluid, int outMb, int duration) {
        RECIPES.add(new Recipe(inFluid, inMb, inItem, inCount, null, outFluid, outMb, duration));
    }

    private static void addFluidItemToFluidItem(Fluid inFluid, int inMb, Item inItem, int inCount, ItemStack outItem, Fluid outFluid, int outMb, int duration) {
        RECIPES.add(new Recipe(inFluid, inMb, inItem, inCount, outItem, outFluid, outMb, duration));
    }

    private static void addItemToFluid(Item inItem, int inCount, Fluid outFluid, int outMb, int duration) {
        RECIPES.add(new Recipe(null, 0, inItem, inCount, null, outFluid, outMb, duration));
    }

    private static void addItemToFluidItem(Item inItem, int inCount, ItemStack outItem, Fluid outFluid, int outMb, int duration) {
        RECIPES.add(new Recipe(null, 0, inItem, inCount, outItem, outFluid, outMb, duration));
    }

    private static void addBedrockRoast(Grade rawGrade, Grade roastedGrade, Type type) {
        ItemStack in = bedrockOre(rawGrade, type, 1);
        ItemStack out = bedrockOre(roastedGrade, type, 1);
        if (in.isEmpty() || out.isEmpty()) return;
        RECIPES.add(new Recipe(null, 0, in.getItem(), 1, out, ModFluids.VITRIOL.getSource(), 50, 10));
    }

    private static ItemStack bedrockOre(Grade grade, Type type, int count) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("hbm_m",
                "bedrock_ore_" + grade.key + "_" + type.name().toLowerCase());
        Item item = BuiltInRegistries.ITEM.get(id);
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    private static void registerSFAuto(FluidEntry fluid) {
        registerSFAuto(fluid, 1_440_000L, ModItems.SOLID_FUEL.get());
    }

    private static void registerSFAuto(FluidEntry fluid, long tuPerSF, Item output) {
        Fluid f = fluid.getSource();
        FT_Flammable flammable = FluidType.getTrait(f, FT_Flammable.class);
        if (flammable == null || flammable.getHeatEnergy() <= 0) return; // kein Trait hinterlegt - Rezept entfaellt.

        double bonus = 0.5D; // doppelte Effizienz - 1:1 aus dem Original.
        int mB = (int) (tuPerSF * 1000L * bonus / flammable.getHeatEnergy());
        if (mB > 10_000) mB -= (mB % 1000);
        else if (mB > 1_000) mB -= (mB % 100);
        else if (mB > 100) mB -= (mB % 10);
        mB = Math.max(mB, 1);

        addFluidToItem(f, mB, new ItemStack(output), 60);
    }

    static {
        // Solid Fuel aus brennbaren Fluiden.
        registerSFAuto(ModFluids.SMEAR);
        registerSFAuto(ModFluids.HEATINGOIL);
        registerSFAuto(ModFluids.HEATINGOIL_VACUUM);
        registerSFAuto(ModFluids.RECLAIMED);
        registerSFAuto(ModFluids.PETROIL);
        registerSFAuto(ModFluids.NAPHTHA);
        registerSFAuto(ModFluids.NAPHTHA_CRACK);
        registerSFAuto(ModFluids.DIESEL);
        registerSFAuto(ModFluids.DIESEL_REFORM);
        registerSFAuto(ModFluids.DIESEL_CRACK);
        registerSFAuto(ModFluids.DIESEL_CRACK_REFORM);
        registerSFAuto(ModFluids.LIGHTOIL);
        registerSFAuto(ModFluids.LIGHTOIL_CRACK);
        registerSFAuto(ModFluids.LIGHTOIL_VACUUM);
        registerSFAuto(ModFluids.KEROSENE);
        registerSFAuto(ModFluids.KEROSENE_REFORM);
        registerSFAuto(ModFluids.SOURGAS);
        registerSFAuto(ModFluids.REFORMGAS);
        registerSFAuto(ModFluids.SYNGAS);
        registerSFAuto(ModFluids.PETROLEUM);
        registerSFAuto(ModFluids.LPG);
        registerSFAuto(ModFluids.BIOFUEL);
        registerSFAuto(ModFluids.AROMATICS);
        registerSFAuto(ModFluids.UNSATURATEDS);
        registerSFAuto(ModFluids.REFORMATE);
        registerSFAuto(ModFluids.XYLENE);
        registerSFAuto(ModFluids.BALEFIRE, 24_000_000L, ModItems.SOLID_FUEL_BF.get());

        // Bedrock-Erz-Roestung: 6 Type-Werte x 5 Grade-Paare = 30 Rezepte.
        for (Type type : Type.values()) {
            addBedrockRoast(Grade.BASE, Grade.BASE_ROASTED, type);
            addBedrockRoast(Grade.PRIMARY, Grade.PRIMARY_ROASTED, type);
            addBedrockRoast(Grade.SULFURIC_BYPRODUCT, Grade.SULFURIC_ROASTED, type);
            addBedrockRoast(Grade.SOLVENT_BYPRODUCT, Grade.SOLVENT_ROASTED, type);
            addBedrockRoast(Grade.RAD_BYPRODUCT, Grade.RAD_ROASTED, type);
        }

        // Syngas <-> Tungstencarbid-Kreislauf (Steam:Syngas 1:2, Syngas:LPS 2:1).
        addFluidItemToFluidItem(ModFluids.SYNGAS.getSource(), 2_000,
                ModItems.getPowder(ModIngots.TUNGSTEN).get(), 1,
                new ItemStack(ModItems.INGOT_TUNGSTEN_CARBIDE.get()),
                ModFluids.SPENTSTEAM.getSource(), 1_000, 300);

        // Syngas aus Kohle.
        addItemToFluid(Items.COAL, 1, ModFluids.SYNGAS.getSource(), 1_000, 100);
        addItemToFluid(ModItems.getPowders(ModPowders.COAL).get(), 1, ModFluids.SYNGAS.getSource(), 1_000, 100);
        addItemToFluid(ModItems.COKE_PETROLEUM.get(), 1, ModFluids.SYNGAS.getSource(), 1_000, 100);

        // Syngas aus Biomasse (+ Holzkohle-Nebenprodukt).
        addItemToFluidItem(ModItems.BIOMASS.get(), 4, new ItemStack(Items.CHARCOAL), ModFluids.SYNGAS.getSource(), 1_000, 100);

        // Schweroel aus Kohle.
        addFluidItemToFluid(ModFluids.HYDROGEN.getSource(), 500, Items.COAL, 1, ModFluids.HEAVYOIL.getSource(), 1_000, 100);
        addFluidItemToFluid(ModFluids.HYDROGEN.getSource(), 500, ModItems.getPowders(ModPowders.COAL).get(), 1, ModFluids.HEAVYOIL.getSource(), 1_000, 100);

        // Kohlegas aus Kohle.
        addFluidItemToFluid(ModFluids.HEAVYOIL.getSource(), 500, Items.COAL, 1, ModFluids.COALGAS.getSource(), 1_000, 50);
        addFluidItemToFluid(ModFluids.HEAVYOIL.getSource(), 500, ModItems.getPowders(ModPowders.COAL).get(), 1, ModFluids.COALGAS.getSource(), 1_000, 50);
        addFluidItemToFluid(ModFluids.HEAVYOIL.getSource(), 500, ModItems.COKE_PETROLEUM.get(), 1, ModFluids.COALGAS.getSource(), 1_000, 50);

        // Reformgas aus Koker-Gas.
        addFluidItemToFluid(ModFluids.GAS_COKER.getSource(), 4_000, null, 0, ModFluids.REFORMGAS.getSource(), 100, 60);

        // Wasserstoff + Graphit aus Erdgas.
        addFluidItemToFluidItem(ModFluids.GAS.getSource(), 12_000, null, 0,
                new ItemStack(ModItems.getIngot(ModIngots.GRAPHITE).get()),
                ModFluids.HYDROGEN.getSource(), 8_000, 60);
    }

    public static List<Recipe> getAll() {
        return List.copyOf(RECIPES);
    }
}
