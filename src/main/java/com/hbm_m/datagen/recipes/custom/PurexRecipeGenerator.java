package com.hbm_m.datagen.recipes.custom;

import java.util.function.Consumer;

import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;

/**
 * Port von {@code PUREXRecipes} (1.7.10 Original, ~40 Rezepte quer durch mehrere unzusammen-
 * haengende Chemie-/Abfall-Ketten - siehe Klassenkommentar in {@code MachinePUREXBlockEntity}).
 * <p>
 * Von den ~40 Original-Rezepten sind hier NUR die Zirnox-Abfall-Rezepte uebernommen (Waste-Item
 * + Kerosin + Salpetersaeure -&gt; Nugget + winziger Atommuell), da nur fuer diese Kette sowohl alle
 * Input- als auch alle Output-Items zweifelsfrei im Item-Register dieses Ports existieren. Die
 * uebrigen Original-Rezepte (PWR-Abfall mit {@code EnumPWRFuel}-Subtypen, Watz-Pellet-Abfall mit
 * {@code EnumWatzType}-Subtypen, Antimaterie-Verarbeitung, ICF-Pellet-Wiederaufbereitung,
 * Thorium-Salz-Regeneration, diverse Schrabidium-Extraktionen) haengen von Items ab, deren Existenz
 * unter diesem Namen nicht zweifelsfrei verifiziert werden konnte (siehe Rechercheergebnis) - sie
 * hier zu raten wuerde gegen die "keine erfundenen Ersatz-Items"-Regel dieser Portierung verstossen.
 * Weitere PUREX-Rezepte koennen in einem spaeteren Durchgang ergaenzt werden, sobald die jeweiligen
 * Fuellstoff-/Abfall-Item-Ketten (PWR, Watz) im Detail verifiziert sind.
 */
public class PurexRecipeGenerator {

    private static final int DURATION = 200;
    private static final int POWER = 400;
    private static final int KEROSENE_MB = 500;
    private static final int NITRIC_ACID_MB = 500;

    public static void generate(Consumer<FinishedRecipe> writer) {

        wasteRecipe(writer, "waste_uranium", ModItems.WASTE_URANIUM.get(), ModItems.NUGGET_URANIUM_FUEL.get());
        wasteRecipe(writer, "waste_plutonium", ModItems.WASTE_PLUTONIUM.get(), ModItems.NUGGET_PLUTONIUM.get());
        wasteRecipe(writer, "waste_thorium", ModItems.WASTE_THORIUM.get(), ModItems.NUGGET_THORIUM_FUEL.get());
        wasteRecipe(writer, "waste_mox", ModItems.WASTE_MOX.get(), ModItems.NUGGET_MOX_FUEL.get());
        wasteRecipe(writer, "waste_schrabidium", ModItems.WASTE_SCHRABIDIUM.get(), ModItems.NUGGET_SCHRABIDIUM.get());
        wasteRecipe(writer, "waste_zfb_mox", ModItems.WASTE_ZFB_MOX.get(), ModItems.NUGGET_PU_MIX.get());
    }

    /** 1:1 aus dem Original-Muster: 1x Waste-Item + Kerosin + Salpetersaeure -&gt; 1x Nugget + 1x winziger Atommuell. */
    private static void wasteRecipe(Consumer<FinishedRecipe> writer, String name,
                                     net.minecraft.world.item.Item wasteItem, net.minecraft.world.item.Item nuggetItem) {
        PurexRecipeBuilder.purexRecipe(DURATION, POWER)
                .addItemInput(wasteItem, 1)
                .addFluidInput(ModFluids.KEROSENE.getSource(), KEROSENE_MB)
                .addFluidInput(ModFluids.NITRIC_ACID.getSource(), NITRIC_ACID_MB)
                .addItemOutput(new ItemStack(nuggetItem, 1))
                .addItemOutput(new ItemStack(ModItems.NUCLEAR_WASTE_TINY.get(), 1))
                .save(writer, "purex/" + name);
    }
}
