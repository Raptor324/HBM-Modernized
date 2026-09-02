package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;

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
        metal(writer, "crystal_iron", ModMaterialItems.item(ModMaterials.IRON, MaterialShape.CRYSTAL),
                ingot(ModMaterials.STEEL, 6), ingot(ModMaterials.TITANIUM, 2),
                bp(ModMaterialItems.item(ModMaterials.LITHIUM, MaterialShape.POWDER_TINY), 3));
        metal(writer, "crystal_gold", ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.CRYSTAL),
                new ItemStack(Items.GOLD_INGOT, 6), ingot(ModMaterials.LEAD, 2),
                bp(ModMaterialItems.item(ModMaterials.LITHIUM, MaterialShape.POWDER_TINY), 3), bp(ModItems.NUGGET_MERCURY.get(), 2));
        metal(writer, "crystal_uranium", ModMaterialItems.item(ModMaterials.URANIUM, MaterialShape.CRYSTAL),
                ingot(ModMaterials.URANIUM, 6), new ItemStack(ModItems.RADIUM_RAW.get(), 4),
                bp(ModMaterialItems.item(ModMaterials.LITHIUM, MaterialShape.POWDER_TINY), 3));
        metal(writer, "crystal_thorium", ModMaterialItems.item(ModMaterials.THORIUM, MaterialShape.CRYSTAL),
                ingot(ModMaterials.THORIUM232, 6), ingot(ModMaterials.URANIUM, 2),
                bp(ModMaterialItems.item(ModMaterials.LITHIUM, MaterialShape.POWDER_TINY), 3));
        metal(writer, "crystal_plutonium", ModMaterialItems.item(ModMaterials.PLUTONIUM, MaterialShape.CRYSTAL),
                ingot(ModMaterials.PLUTONIUM, 6), ingot(ModMaterials.POLONIUM, 2),
                bp(ModMaterialItems.item(ModMaterials.LITHIUM, MaterialShape.POWDER_TINY), 3));
        metal(writer, "crystal_titanium", ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.CRYSTAL),
                ingot(ModMaterials.TITANIUM, 6), ingot(ModMaterials.STEEL, 2),
                bp(ModMaterialItems.item(ModMaterials.LITHIUM, MaterialShape.POWDER_TINY), 3));
        metal(writer, "crystal_copper", ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.CRYSTAL),
                new ItemStack(Items.COPPER_INGOT, 6), ingot(ModMaterials.LEAD, 1),
                bp(ModMaterialItems.item(ModMaterials.LITHIUM, MaterialShape.POWDER_TINY), 3), bp(ModItems.SULFUR.get(), 2));
        metal(writer, "crystal_tungsten", ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.CRYSTAL),
                ingot(ModMaterials.TUNGSTEN, 6), ingot(ModMaterials.STEEL, 2),
                bp(ModMaterialItems.item(ModMaterials.LITHIUM, MaterialShape.POWDER_TINY), 3));
        metal(writer, "crystal_aluminium", ModMaterialItems.item(ModMaterials.ALUMINIUM, MaterialShape.CRYSTAL),
                ingot(ModMaterials.ALUMINUM, 2), ingot(ModMaterials.STEEL, 2),
                bp(ModItems.CRYOLITE.get(), 4), bp(ModMaterialItems.item(ModMaterials.LITHIUM, MaterialShape.POWDER_TINY), 3));
        metal(writer, "crystal_beryllium", ModMaterialItems.item(ModMaterials.BERYLLIUM, MaterialShape.CRYSTAL),
                ingot(ModMaterials.BERYLLIUM, 6), ingot(ModMaterials.LEAD, 1),
                bp(ModMaterialItems.item(ModMaterials.LITHIUM, MaterialShape.POWDER_TINY), 3), bp(ModMaterialItems.item(ModMaterials.QUARTZ, MaterialShape.POWDER), 2));
        metal(writer, "crystal_lead", ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.CRYSTAL),
                ingot(ModMaterials.LEAD, 6), new ItemStack(Items.GOLD_INGOT, 2),
                bp(ModMaterialItems.item(ModMaterials.LITHIUM, MaterialShape.POWDER_TINY), 3));
        metal(writer, "crystal_schraranium", ModMaterialItems.item(ModMaterials.SCHRARANIUM, MaterialShape.CRYSTAL),
                ingot(ModMaterials.SCHRABIDIUM, 5), ingot(ModMaterials.URANIUM, 2),
                bp(ModMaterialItems.item(ModMaterials.NEPTUNIUM, MaterialShape.NUGGET), 2));
        metal(writer, "crystal_schrabidium", ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.CRYSTAL),
                ingot(ModMaterials.SCHRABIDIUM, 6), ingot(ModMaterials.PLUTONIUM, 2),
                bp(ModMaterialItems.item(ModMaterials.LITHIUM, MaterialShape.POWDER_TINY), 3));
        metal(writer, "crystal_rare", ModMaterialItems.item(ModMaterials.RARE, MaterialShape.CRYSTAL),
                ingot(ModMaterials.ZIRCONIUM, 6), ingot(ModMaterials.BORON, 2),
                bp(ModItems.POWDER_DESH_MIX.get(), 3));
        metal(writer, "crystal_trixite", ModMaterialItems.item(ModMaterials.TRIXITE, MaterialShape.CRYSTAL),
                ingot(ModMaterials.PLUTONIUM, 3), ingot(ModMaterials.COBALT, 4),
                bp(ModMaterialItems.item(ModMaterials.NIOBIUM, MaterialShape.POWDER), 4), bp(ModItems.POWDER_NITAN_MIX.get(), 2));
        metal(writer, "crystal_lithium", ModMaterialItems.item(ModMaterials.LITHIUM, MaterialShape.CRYSTAL),
                new net.minecraft.world.item.ItemStack(com.hbm_m.item.ModItems.LITHIUM.get(), 6), ingot(ModMaterials.BORON, 2),
                bp(ModMaterialItems.item(ModMaterials.QUARTZ, MaterialShape.POWDER), 2), bp(ModItems.FLUORITE.get(), 2));
        metal(writer, "crystal_starmetal", ModMaterialItems.item(ModMaterials.STARMETAL, MaterialShape.CRYSTAL),
                ingot(ModMaterials.DURA_STEEL, 4), ingot(ModMaterials.COBALT, 4),
                bp(ModMaterialItems.item(ModMaterials.ASTATINE, MaterialShape.POWDER), 3), bp(ModItems.NUGGET_MERCURY.get(), 8));
        metal(writer, "crystal_cobalt", ModMaterialItems.item(ModMaterials.COBALT, MaterialShape.CRYSTAL),
                ingot(ModMaterials.COBALT, 3), ingot(ModMaterials.STEEL, 4),
                bp(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.POWDER), 4), bp(ModMaterialItems.item(ModMaterials.LITHIUM, MaterialShape.POWDER_TINY), 3));
    }

    private static void metal(Consumer<FinishedRecipe> writer, String id, net.minecraft.world.item.Item crystal,
                               ItemStack outA, ItemStack outB, ItemStack... byproducts) {
        ElectrolyserMetalRecipeBuilder.electrolyserMetalRecipe(
                Ingredient.of(crystal), outA, outB, byproducts, DURATION)
                .save(writer, "electrolyser_metal/" + id);
    }

    private static ItemStack ingot(ModMaterials material, int count) {
        return new ItemStack(ModMaterialItems.item(material, MaterialShape.INGOT), count);
    }

    private static ItemStack bp(net.minecraft.world.item.Item item, int count) {
        return new ItemStack(item, count);
    }
}
//?}
