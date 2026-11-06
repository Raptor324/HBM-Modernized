package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.inventory.material.MaterialShapes;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.inventory.recipes.ElectrolyserMetalRecipes;
import com.hbm_m.inventory.recipes.ElectrolyserMetalRecipes.ElectrolysisMetalRecipe;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.industrial.BedrockOreType;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Генератор JSON-рецептов электролизёра, Metal-режим ({@code hbm_m:electrolyser_metal}).
 *
 * <p>1:1-порт статической таблицы {@code ElectrolyserMetalRecipes.registerDefaults}: все 18 кристалл-рецептов
 * (600 тиков) плюс 18 Grundgestein-Erz-рецепpte, die wie im Original aus den Primaerausgaben der sechs Erztypen
 * entstehen ({@code ElectrolyserMetalRecipes.makeBedrockOreProduct}, 20 Ticks). Шлак-выходы (Schmelzen) als
 * Formen-Items (Barren/Pulver/Klumpen) — идентичное количество квант, рантайм конвертирует обратно через
 * {@code Mats.getShape}. Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для
 * 1.20.1-forge.</p>
 */
public final class ElectrolyserMetalRecipeGenerator {

    private static final int DURATION = 600;

    private ElectrolyserMetalRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        crystal(writer, "crystal_iron", ModMaterials.IRON,
                new ItemStack(Items.IRON_INGOT, 6), ingot(ModMaterials.TITANIUM, 2),
                lithiumTiny());
        crystal(writer, "crystal_gold", ModMaterials.GOLD,
                new ItemStack(Items.GOLD_INGOT, 6), ingot(ModMaterials.LEAD, 2),
                lithiumTiny(), item(ModItems.NUGGET_MERCURY.get(), 2));
        crystal(writer, "crystal_uranium", ModMaterials.URANIUM,
                ingot(ModMaterials.URANIUM, 6), nugget(ModMaterials.RA226, 4),
                lithiumTiny());
        crystal(writer, "crystal_thorium", ModMaterials.THORIUM,
                ingot(ModMaterials.THORIUM232, 6), ingot(ModMaterials.URANIUM, 2),
                lithiumTiny());
        crystal(writer, "crystal_plutonium", ModMaterials.PLUTONIUM,
                ingot(ModMaterials.PLUTONIUM, 6), ingot(ModMaterials.POLONIUM, 2),
                lithiumTiny());
        crystal(writer, "crystal_titanium", ModMaterials.TITANIUM,
                ingot(ModMaterials.TITANIUM, 6), new ItemStack(Items.IRON_INGOT, 2),
                lithiumTiny());
        crystal(writer, "crystal_copper", ModMaterials.COPPER,
                new ItemStack(Items.COPPER_INGOT, 6), nugget(ModMaterials.LEAD, 4),
                lithiumTiny(), item(ModItems.SULFUR.get(), 2));
        crystal(writer, "crystal_tungsten", ModMaterials.TUNGSTEN,
                ingot(ModMaterials.TUNGSTEN, 6), new ItemStack(Items.IRON_INGOT, 2),
                lithiumTiny());
        crystal(writer, "crystal_aluminium", ModMaterials.ALUMINIUM,
                ingot(ModMaterials.ALUMINUM, 2), new ItemStack(Items.IRON_INGOT, 2),
                item(ModItems.CRYOLITE.get(), 4), lithiumTiny());
        crystal(writer, "crystal_beryllium", ModMaterials.BERYLLIUM,
                ingot(ModMaterials.BERYLLIUM, 6), nugget(ModMaterials.LEAD, 4),
                lithiumTiny(), powder(ModMaterials.QUARTZ, 2));
        crystal(writer, "crystal_lead", ModMaterials.LEAD,
                ingot(ModMaterials.LEAD, 6), new ItemStack(Items.GOLD_INGOT, 2),
                lithiumTiny());
        // Schraranium: beide Ausgaenge als Klumpen (5/2), Nebenprodukt Neptunium-Klumpen — ohne Lithium.
        crystal(writer, "crystal_schraranium", ModMaterials.SCHRARANIUM,
                nugget(ModMaterials.SCHRABIDIUM, 5), nugget(ModMaterials.URANIUM, 2),
                nugget(ModMaterials.NEPTUNIUM, 2));
        crystal(writer, "crystal_schrabidium", ModMaterials.SCHRABIDIUM,
                ingot(ModMaterials.SCHRABIDIUM, 6), ingot(ModMaterials.PLUTONIUM, 2),
                lithiumTiny());
        // Rare: Zirkonium-/Bor-Klumpen (Bor ohne Klumpen-Item als 2 Mini-Pulver = gleiche Quanten).
        crystal(writer, "crystal_rare", ModMaterials.RARE,
                nugget(ModMaterials.ZIRCONIUM, 6), tiny(ModMaterials.BORON, 2),
                item(ModItems.POWDER_DESH_MIX.get(), 3));
        crystal(writer, "crystal_trixite", ModMaterials.TRIXITE,
                ingot(ModMaterials.PLUTONIUM, 3), ingot(ModMaterials.COBALT, 4),
                powder(ModMaterials.NIOBIUM, 4), item(ModItems.POWDER_NITAN_MIX.get(), 2));
        // Lithium hat kein Barren-Item: 6 Lithium-Pulver = 6 Barren in Quanten (432).
        crystal(writer, "crystal_lithium", ModMaterials.LITHIUM,
                powder(ModMaterials.LITHIUM, 6), ingot(ModMaterials.BORON, 2),
                powder(ModMaterials.QUARTZ, 2), item(ModItems.FLUORITE.get(), 2));
        crystal(writer, "crystal_starmetal", ModMaterials.STARMETAL,
                ingot(ModMaterials.DURA_STEEL, 4), ingot(ModMaterials.COBALT, 4),
                powder(ModMaterials.ASTATINE, 3), item(ModItems.NUGGET_MERCURY.get(), 8));
        crystal(writer, "crystal_cobalt", ModMaterials.COBALT,
                ingot(ModMaterials.COBALT, 3), new ItemStack(Items.IRON_INGOT, 4),
                powder(ModMaterials.COPPER, 4), lithiumTiny());

        // Grundgestein-Erze: identisch zur statischen Schleife (productsF/productsS/productsC ->
        // makeBedrockOreProduct), Eingang primary_first/primary_second/crumbs je Erztyp.
        for (BedrockOreType type : BedrockOreType.values()) {
            Item crumbs = type.item("crumbs");

            List<Object[]> productsF = new ArrayList<>();
            productsF.add(new Object[] { type.primary1, 8 });
            productsF.add(new Object[] { type.primary2, 4 });
            if (crumbs != null) productsF.add(new Object[] { new ItemStack(crumbs), 1 });
            Item first = type.item("primary_first");
            if (first != null) bedrock(writer, first, productsF);

            List<Object[]> productsS = new ArrayList<>();
            productsS.add(new Object[] { type.primary1, 4 });
            productsS.add(new Object[] { type.primary2, 8 });
            if (crumbs != null) productsS.add(new Object[] { new ItemStack(crumbs), 1 });
            Item second = type.item("primary_second");
            if (second != null) bedrock(writer, second, productsS);

            List<Object[]> productsC = new ArrayList<>();
            productsC.add(new Object[] { type.primary1, 2 });
            productsC.add(new Object[] { type.primary2, 2 });
            if (crumbs != null) bedrock(writer, crumbs, productsC);
        }
    }

    private static void crystal(Consumer<FinishedRecipe> writer, String id, ModMaterials material,
                                 ItemStack outA, ItemStack outB, ItemStack... byproducts) {
        ElectrolyserMetalRecipeBuilder.electrolyserMetalRecipe(
                Ingredient.of(ModMaterialItems.item(material, MaterialShape.CRYSTAL)), outA, outB, byproducts, DURATION)
                .save(writer, "electrolyser_metal/" + id);
    }

    /** Grundgestein-Erz: Ausgänge aus {@code makeBedrockOreProduct}, Schmelzen als Formen-Items. */
    private static void bedrock(Consumer<FinishedRecipe> writer, Item input, List<Object[]> products) {
        ElectrolysisMetalRecipe rec = ElectrolyserMetalRecipes.makeBedrockOreProduct(products);
        ItemStack outA = shapeStack(rec.output1);
        ItemStack outB = shapeStack(rec.output2);
        if (outA == null) throw new IllegalStateException("electrolyser_metal: unausdrückbarer Schmelzausgang für " + BuiltInRegistries.ITEM.getKey(input));
        if (rec.output2 != null && outB == null) throw new IllegalStateException("electrolyser_metal: unausdrückbarer zweiter Schmelzausgang für " + BuiltInRegistries.ITEM.getKey(input));
        ElectrolyserMetalRecipeBuilder.electrolyserMetalRecipe(
                Ingredient.of(input), outA, outB, rec.byproduct, rec.duration)
                .save(writer, "electrolyser_metal/" + BuiltInRegistries.ITEM.getKey(input).getPath());
    }

    /** Schmelzmenge (Quanten) als Formen-Item: Barren/Pulver (72), sonst Klumpen/Mini-Pulver (8); null = nicht darstellbar. */
    private static ItemStack shapeStack(MaterialStack molten) {
        if (molten == null) return null;
        for (MaterialShapes shape : new MaterialShapes[] { MaterialShapes.INGOT, MaterialShapes.DUST, MaterialShapes.NUGGET, MaterialShapes.DUSTTINY }) {
            if (molten.amount % shape.q(1) != 0) continue;
            Item item = Mats.getItemForShape(molten.material, shape);
            if (item != null && item != Items.AIR) return new ItemStack(item, molten.amount / shape.q(1));
        }
        return null;
    }

    private static ItemStack ingot(ModMaterials material, int count) {
        return new ItemStack(ModMaterialItems.item(material, MaterialShape.INGOT), count);
    }

    private static ItemStack nugget(ModMaterials material, int count) {
        return new ItemStack(ModMaterialItems.item(material, MaterialShape.NUGGET), count);
    }

    private static ItemStack powder(ModMaterials material, int count) {
        return new ItemStack(ModMaterialItems.item(material, MaterialShape.POWDER), count);
    }

    /** 3 Mini-Pulver Lithium = литий-крошка статической таблицы (24 кванта). */
    private static ItemStack lithiumTiny() {
        return tiny(ModMaterials.LITHIUM, 3);
    }

    private static ItemStack tiny(ModMaterials material, int count) {
        return new ItemStack(ModMaterialItems.item(material, MaterialShape.POWDER_TINY), count);
    }

    private static ItemStack item(Item item, int count) {
        return new ItemStack(item, count);
    }
}
//?}
