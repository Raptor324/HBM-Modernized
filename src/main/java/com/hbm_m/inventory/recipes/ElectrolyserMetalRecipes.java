package com.hbm_m.inventory.recipes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.inventory.material.MaterialShapes;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.industrial.BedrockOreType;
import com.hbm_m.item.industrial.BedrockOreType.BedrockOreOutput;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code ElectrolyserMetalRecipes}: Kristalle und Grundgestein-Erze werden in zwei Schmelzen plus feste
 * Nebenprodukte zerlegt. Die Grundgestein-Erzrezepte entstehen wie im Original aus den Primaerausgaben der
 * sechs Erztypen ({@code makeBedrockOreProduct}).
 */
public final class ElectrolyserMetalRecipes {

    public static final Map<Item, ElectrolysisMetalRecipe> recipes = new LinkedHashMap<>();
    private static boolean loaded;

    private ElectrolyserMetalRecipes() { }

    public static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        registerDefaults();
    }

    public static Map<Item, ElectrolysisMetalRecipe> all() {
        ensureLoaded();
        return recipes;
    }

    private static Item crystal(ModMaterials mat) { return ModMaterialItems.item(mat, MaterialShape.CRYSTAL); }
    private static ItemStack lithiumTiny() { return new ItemStack(ModMaterialItems.item(ModMaterials.LITHIUM, MaterialShape.POWDER_TINY), 3); }

    private static void registerDefaults() {

        recipes.put(crystal(ModMaterials.IRON), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_IRON, MaterialShapes.INGOT.q(6)),
                new MaterialStack(Mats.MAT_TITANIUM, MaterialShapes.INGOT.q(2)),
                lithiumTiny()));

        recipes.put(crystal(ModMaterials.GOLD), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_GOLD, MaterialShapes.INGOT.q(6)),
                new MaterialStack(Mats.MAT_LEAD, MaterialShapes.INGOT.q(2)),
                lithiumTiny(),
                new ItemStack(ModItems.NUGGET_MERCURY.get(), 2)));

        recipes.put(crystal(ModMaterials.URANIUM), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_URANIUM, MaterialShapes.INGOT.q(6)),
                new MaterialStack(Mats.MAT_RADIUM, MaterialShapes.NUGGET.q(4)),
                lithiumTiny()));

        recipes.put(crystal(ModMaterials.THORIUM), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_THORIUM, MaterialShapes.INGOT.q(6)),
                new MaterialStack(Mats.MAT_URANIUM, MaterialShapes.INGOT.q(2)),
                lithiumTiny()));

        recipes.put(crystal(ModMaterials.PLUTONIUM), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_PLUTONIUM, MaterialShapes.INGOT.q(6)),
                new MaterialStack(Mats.MAT_POLONIUM, MaterialShapes.INGOT.q(2)),
                lithiumTiny()));

        recipes.put(crystal(ModMaterials.TITANIUM), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_TITANIUM, MaterialShapes.INGOT.q(6)),
                new MaterialStack(Mats.MAT_IRON, MaterialShapes.INGOT.q(2)),
                lithiumTiny()));

        recipes.put(crystal(ModMaterials.COPPER), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_COPPER, MaterialShapes.INGOT.q(6)),
                new MaterialStack(Mats.MAT_LEAD, MaterialShapes.NUGGET.q(4)),
                lithiumTiny(),
                new ItemStack(ModItems.SULFUR.get(), 2)));

        recipes.put(crystal(ModMaterials.TUNGSTEN), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_TUNGSTEN, MaterialShapes.INGOT.q(6)),
                new MaterialStack(Mats.MAT_IRON, MaterialShapes.INGOT.q(2)),
                lithiumTiny()));

        recipes.put(crystal(ModMaterials.ALUMINIUM), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_ALUMINIUM, MaterialShapes.INGOT.q(2)),
                new MaterialStack(Mats.MAT_IRON, MaterialShapes.INGOT.q(2)),
                new ItemStack(ModItems.CRYOLITE.get(), 4),
                lithiumTiny()));

        recipes.put(crystal(ModMaterials.BERYLLIUM), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_BERYLLIUM, MaterialShapes.INGOT.q(6)),
                new MaterialStack(Mats.MAT_LEAD, MaterialShapes.NUGGET.q(4)),
                lithiumTiny(),
                new ItemStack(ModMaterialItems.item(ModMaterials.QUARTZ, MaterialShape.POWDER), 2)));

        recipes.put(crystal(ModMaterials.LEAD), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_LEAD, MaterialShapes.INGOT.q(6)),
                new MaterialStack(Mats.MAT_GOLD, MaterialShapes.INGOT.q(2)),
                lithiumTiny()));

        recipes.put(crystal(ModMaterials.SCHRARANIUM), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_SCHRABIDIUM, MaterialShapes.NUGGET.q(5)),
                new MaterialStack(Mats.MAT_URANIUM, MaterialShapes.NUGGET.q(2)),
                new ItemStack(ModMaterialItems.item(ModMaterials.NEPTUNIUM, MaterialShape.NUGGET), 2)));

        recipes.put(crystal(ModMaterials.SCHRABIDIUM), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_SCHRABIDIUM, MaterialShapes.INGOT.q(6)),
                new MaterialStack(Mats.MAT_PLUTONIUM, MaterialShapes.INGOT.q(2)),
                lithiumTiny()));

        recipes.put(crystal(ModMaterials.RARE), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_ZIRCONIUM, MaterialShapes.NUGGET.q(6)),
                new MaterialStack(Mats.MAT_BORON, MaterialShapes.NUGGET.q(2)),
                new ItemStack(ModItems.POWDER_DESH_MIX.get(), 3)));

        recipes.put(crystal(ModMaterials.TRIXITE), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_PLUTONIUM, MaterialShapes.INGOT.q(3)),
                new MaterialStack(Mats.MAT_COBALT, MaterialShapes.INGOT.q(4)),
                new ItemStack(ModMaterialItems.item(ModMaterials.NIOBIUM, MaterialShape.POWDER), 4),
                new ItemStack(ModItems.POWDER_NITAN_MIX.get(), 2)));

        recipes.put(crystal(ModMaterials.LITHIUM), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_LITHIUM, MaterialShapes.INGOT.q(6)),
                new MaterialStack(Mats.MAT_BORON, MaterialShapes.INGOT.q(2)),
                new ItemStack(ModMaterialItems.item(ModMaterials.QUARTZ, MaterialShape.POWDER), 2),
                new ItemStack(ModItems.FLUORITE.get(), 2)));

        recipes.put(crystal(ModMaterials.STARMETAL), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_DURA, MaterialShapes.INGOT.q(4)),
                new MaterialStack(Mats.MAT_COBALT, MaterialShapes.INGOT.q(4)),
                new ItemStack(ModMaterialItems.item(ModMaterials.ASTATINE, MaterialShape.POWDER), 3),
                new ItemStack(ModItems.NUGGET_MERCURY.get(), 8)));

        recipes.put(crystal(ModMaterials.COBALT), new ElectrolysisMetalRecipe(
                new MaterialStack(Mats.MAT_COBALT, MaterialShapes.INGOT.q(3)),
                new MaterialStack(Mats.MAT_IRON, MaterialShapes.INGOT.q(4)),
                new ItemStack(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.POWDER), 4),
                lithiumTiny()));

        for (BedrockOreType type : BedrockOreType.values()) {
            Item crumbs = type.item("crumbs");

            List<Object[]> productsF = new ArrayList<>();
            productsF.add(new Object[] { type.primary1, 8 });
            productsF.add(new Object[] { type.primary2, 4 });
            if (crumbs != null) productsF.add(new Object[] { new ItemStack(crumbs), 1 });
            Item first = type.item("primary_first");
            if (first != null) recipes.put(first, makeBedrockOreProduct(productsF));

            List<Object[]> productsS = new ArrayList<>();
            productsS.add(new Object[] { type.primary1, 4 });
            productsS.add(new Object[] { type.primary2, 8 });
            if (crumbs != null) productsS.add(new Object[] { new ItemStack(crumbs), 1 });
            Item second = type.item("primary_second");
            if (second != null) recipes.put(second, makeBedrockOreProduct(productsS));

            List<Object[]> productsC = new ArrayList<>();
            productsC.add(new Object[] { type.primary1, 2 });
            productsC.add(new Object[] { type.primary2, 2 });
            if (crumbs != null) recipes.put(crumbs, makeBedrockOreProduct(productsC));
        }
    }

    /** 1:1 {@code makeBedrockOreProduct}. */
    public static ElectrolysisMetalRecipe makeBedrockOreProduct(List<Object[]> products) {
        List<MaterialStack> moltenProducts = new ArrayList<>();
        List<ItemStack> solidProducts = new ArrayList<>();

        for (Object[] product : products) {
            Object key = product[0];
            int value = (Integer) product[1];
            if (moltenProducts.size() < 2 && key instanceof BedrockOreOutput out) {
                MaterialStack melt = BedrockOreType.toFluid(out, value);
                if (melt != null) {
                    moltenProducts.add(melt);
                    continue;
                }
            }

            if (key instanceof BedrockOreOutput out) solidProducts.add(BedrockOreType.extract(out, value));
            if (key instanceof ItemStack stack) solidProducts.add(stack.copy());
        }
        if (moltenProducts.isEmpty()) moltenProducts.add(new MaterialStack(Mats.MAT_SLAG, MaterialShapes.INGOT.q(2)));

        return new ElectrolysisMetalRecipe(
                moltenProducts.get(0),
                moltenProducts.size() > 1 ? moltenProducts.get(1) : null,
                20,
                solidProducts.toArray(new ItemStack[0]));
    }

    /** Original {@code getRecipe}: Vergleich nach Item (ComparableStack, Menge 1). */
    @Nullable
    public static ElectrolysisMetalRecipe getRecipe(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        ensureLoaded();
        return recipes.get(stack.getItem());
    }

    public static class ElectrolysisMetalRecipe {

        @Nullable public MaterialStack output1;
        @Nullable public MaterialStack output2;
        public ItemStack[] byproduct;
        public int duration;

        public ElectrolysisMetalRecipe(@Nullable MaterialStack output1, @Nullable MaterialStack output2, ItemStack... byproduct) {
            this(output1, output2, 600, byproduct);
        }

        public ElectrolysisMetalRecipe(@Nullable MaterialStack output1, @Nullable MaterialStack output2, int duration, ItemStack... byproduct) {
            this.output1 = output1;
            this.output2 = output2;
            this.byproduct = byproduct;
            this.duration = duration;
        }
    }
}
