package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Consumer;

/**
 * Генератор JSON-рецептов электролизёра, Metal-режим ({@code hbm_m:electrolyser_metal}).
 *
 * <p>Порт 18 кристалл/руда-рецептов из удалённого статического {@code ElectrolyserRecipes}
 * (Metal-часть). Длительность у всех рецептов — 600 тиков, 1:1 из оригинала.
 * Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 */
public final class ElectrolyserMetalRecipeGenerator {

    private static final int DURATION = 600;

    private ElectrolyserMetalRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        // Упрощение порта (как в оригинальном статическом файле): оба материальных выхода
        // выдаются сразу как реальные слитки, вторичные выходы — как побочные предметы.
        metal(writer, "crystal_iron", ModItems.CRYSTAL_IRON.get(),
                ingot(ModIngots.STEEL, 6), ingot(ModIngots.TITANIUM, 2),
                bp(ModItems.LITHIUM_POWDER_TINY.get(), 3));
        metal(writer, "crystal_gold", ModItems.CRYSTAL_GOLD.get(),
                new ItemStack(Items.GOLD_INGOT, 6), ingot(ModIngots.LEAD, 2),
                bp(ModItems.LITHIUM_POWDER_TINY.get(), 3), bp(ModItems.NUGGET_MERCURY.get(), 2));
        metal(writer, "crystal_uranium", ModItems.CRYSTAL_URANIUM.get(),
                ingot(ModIngots.URANIUM, 6), new ItemStack(ModItems.RADIUM_RAW.get(), 4),
                bp(ModItems.LITHIUM_POWDER_TINY.get(), 3));
        metal(writer, "crystal_thorium", ModItems.CRYSTAL_THORIUM.get(),
                ingot(ModIngots.THORIUM232, 6), ingot(ModIngots.URANIUM, 2),
                bp(ModItems.LITHIUM_POWDER_TINY.get(), 3));
        metal(writer, "crystal_plutonium", ModItems.CRYSTAL_PLUTONIUM.get(),
                ingot(ModIngots.PLUTONIUM, 6), ingot(ModIngots.POLONIUM, 2),
                bp(ModItems.LITHIUM_POWDER_TINY.get(), 3));
        metal(writer, "crystal_titanium", ModItems.CRYSTAL_TITANIUM.get(),
                ingot(ModIngots.TITANIUM, 6), ingot(ModIngots.STEEL, 2),
                bp(ModItems.LITHIUM_POWDER_TINY.get(), 3));
        metal(writer, "crystal_copper", ModItems.CRYSTAL_COPPER.get(),
                new ItemStack(Items.COPPER_INGOT, 6), ingot(ModIngots.LEAD, 1),
                bp(ModItems.LITHIUM_POWDER_TINY.get(), 3), bp(ModItems.SULFUR.get(), 2));
        metal(writer, "crystal_tungsten", ModItems.CRYSTAL_TUNGSTEN.get(),
                ingot(ModIngots.TUNGSTEN, 6), ingot(ModIngots.STEEL, 2),
                bp(ModItems.LITHIUM_POWDER_TINY.get(), 3));
        metal(writer, "crystal_aluminium", ModItems.CRYSTAL_ALUMINIUM.get(),
                ingot(ModIngots.ALUMINUM, 2), ingot(ModIngots.STEEL, 2),
                bp(ModItems.CRYOLITE.get(), 4), bp(ModItems.LITHIUM_POWDER_TINY.get(), 3));
        metal(writer, "crystal_beryllium", ModItems.CRYSTAL_BERYLLIUM.get(),
                ingot(ModIngots.BERYLLIUM, 6), ingot(ModIngots.LEAD, 1),
                bp(ModItems.LITHIUM_POWDER_TINY.get(), 3), bp(ModItems.QUARTZ_POWDER.get(), 2));
        metal(writer, "crystal_lead", ModItems.CRYSTAL_LEAD.get(),
                ingot(ModIngots.LEAD, 6), new ItemStack(Items.GOLD_INGOT, 2),
                bp(ModItems.LITHIUM_POWDER_TINY.get(), 3));
        metal(writer, "crystal_schraranium", ModItems.CRYSTAL_SCHRARANIUM.get(),
                ingot(ModIngots.SCHRABIDIUM, 5), ingot(ModIngots.URANIUM, 2),
                bp(ModItems.NUGGET_NEPTUNIUM.get(), 2));
        metal(writer, "crystal_schrabidium", ModItems.CRYSTAL_SCHRABIDIUM.get(),
                ingot(ModIngots.SCHRABIDIUM, 6), ingot(ModIngots.PLUTONIUM, 2),
                bp(ModItems.LITHIUM_POWDER_TINY.get(), 3));
        metal(writer, "crystal_rare", ModItems.CRYSTAL_RARE.get(),
                ingot(ModIngots.ZIRCONIUM, 6), ingot(ModIngots.BORON, 2),
                bp(ModItems.POWDER_DESH_MIX.get(), 3));
        metal(writer, "crystal_trixite", ModItems.CRYSTAL_TRIXITE.get(),
                ingot(ModIngots.PLUTONIUM, 3), ingot(ModIngots.COBALT, 4),
                bp(ModItems.getPowder(ModIngots.NIOBIUM).get(), 4), bp(ModItems.POWDER_NITAN_MIX.get(), 2));
        metal(writer, "crystal_lithium", ModItems.CRYSTAL_LITHIUM.get(),
                ingot(ModIngots.LITHIUM_INGOT, 6), ingot(ModIngots.BORON, 2),
                bp(ModItems.QUARTZ_POWDER.get(), 2), bp(ModItems.FLUORITE.get(), 2));
        metal(writer, "crystal_starmetal", ModItems.CRYSTAL_STARMETAL.get(),
                ingot(ModIngots.DURA_STEEL, 4), ingot(ModIngots.COBALT, 4),
                bp(ModItems.getPowder(ModIngots.ASTATINE).get(), 3), bp(ModItems.NUGGET_MERCURY.get(), 8));
        metal(writer, "crystal_cobalt", ModItems.CRYSTAL_COBALT.get(),
                ingot(ModIngots.COBALT, 3), ingot(ModIngots.STEEL, 4),
                bp(ModItems.COPPER_POWDER.get(), 4), bp(ModItems.LITHIUM_POWDER_TINY.get(), 3));
    }

    private static void metal(Consumer<FinishedRecipe> writer, String id, net.minecraft.world.item.Item crystal,
                               ItemStack outA, ItemStack outB, ItemStack... byproducts) {
        ElectrolyserMetalRecipeBuilder.electrolyserMetalRecipe(
                Ingredient.of(crystal), outA, outB, byproducts, DURATION)
                .save(writer, "electrolyser_metal/" + id);
    }

    private static ItemStack ingot(ModIngots material, int count) {
        return new ItemStack(ModItems.getIngot(material).get(), count);
    }

    private static ItemStack bp(net.minecraft.world.item.Item item, int count) {
        return new ItemStack(item, count);
    }
}
//?}
