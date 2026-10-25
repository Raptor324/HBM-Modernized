package com.hbm_m.inventory.recipes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.inventory.material.MaterialShapes;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.lib.RefStrings;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 1:1 {@code CrucibleRecipes}: die Legierungsrezepte des Schmelztiegels (Name, Takt in Ticks, Symbol, Ein- und
 * Ausgaben in Quanten). Der Tiegel waehlt ein Rezept per Name ({@code crucible.steel} usw.).
 */
public final class CrucibleRecipes {

    public static final List<CrucibleRecipe> recipeOrderedList = new ArrayList<>();
    public static final Map<String, CrucibleRecipe> recipeNameMap = new LinkedHashMap<>();

    private static boolean init = false;

    private CrucibleRecipes() { }

    private static ItemStack port(String id) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, id));
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static void register(CrucibleRecipe recipe) {
        recipeOrderedList.add(recipe);
        recipeNameMap.put(recipe.name, recipe);
    }

    @Nullable
    public static CrucibleRecipe get(String name) {
        init();
        return recipeNameMap.get(name);
    }

    public static List<CrucibleRecipe> all() {
        init();
        return recipeOrderedList;
    }

    /*
     * IMPORTANT: crucibles do not have stack size checks for the recipe's result, meaning that they can overflow if the resulting stacks are
     * bigger than the input stacks, so make sure that material doesn't "expand". very few things do that IRL when alloying anyway.
     */
    public static synchronized void init() {
        if (init) return;
        init = true;

        int n = MaterialShapes.NUGGET.q(1);
        int i = MaterialShapes.INGOT.q(1);

        register(new CrucibleRecipe("crucible.steel").setup(20, port("steel_ingot"))
                .inputs(new MaterialStack(Mats.MAT_IRON, n * 2), new MaterialStack(Mats.MAT_CARBON, n * 3), new MaterialStack(Mats.MAT_FLUX, n))
                .outputs(new MaterialStack(Mats.MAT_STEEL, n * 2)));

        register(new CrucibleRecipe("crucible.hematite").setup(6, port("stone_resource_hematite"))
                .inputs(new MaterialStack(Mats.MAT_HEMATITE, i * 2), new MaterialStack(Mats.MAT_FLUX, n * 2))
                .outputs(new MaterialStack(Mats.MAT_IRON, i), new MaterialStack(Mats.MAT_SLAG, n * 3)));

        register(new CrucibleRecipe("crucible.malachite").setup(6, port("stone_resource_malachite"))
                .inputs(new MaterialStack(Mats.MAT_MALACHITE, i * 2), new MaterialStack(Mats.MAT_FLUX, n * 2))
                .outputs(new MaterialStack(Mats.MAT_COPPER, i), new MaterialStack(Mats.MAT_SLAG, n * 3)));

        register(new CrucibleRecipe("crucible.redcopper").setup(2, port("red_copper_ingot"))
                .inputs(new MaterialStack(Mats.MAT_COPPER, n), new MaterialStack(Mats.MAT_REDSTONE, n))
                .outputs(new MaterialStack(Mats.MAT_MINGRADE, n * 2)));

        register(new CrucibleRecipe("crucible.hss").setup(9, port("dura_steel_ingot"))
                .inputs(new MaterialStack(Mats.MAT_STEEL, n * 5), new MaterialStack(Mats.MAT_TUNGSTEN, n * 3), new MaterialStack(Mats.MAT_COBALT, n * 1))
                .outputs(new MaterialStack(Mats.MAT_DURA, n * 9)));

        register(new CrucibleRecipe("crucible.ferro").setup(3, port("ferrouranium_ingot"))
                .inputs(new MaterialStack(Mats.MAT_STEEL, n * 2), new MaterialStack(Mats.MAT_U238, n))
                .outputs(new MaterialStack(Mats.MAT_FERRO, n * 3)));

        register(new CrucibleRecipe("crucible.tcalloy").setup(9, port("tcalloy_ingot"))
                .inputs(new MaterialStack(Mats.MAT_STEEL, n * 8), new MaterialStack(Mats.MAT_TECHNETIUM, n))
                .outputs(new MaterialStack(Mats.MAT_TCALLOY, i)));

        register(new CrucibleRecipe("crucible.cdalloy").setup(9, port("cdalloy_ingot"))
                .inputs(new MaterialStack(Mats.MAT_STEEL, n * 8), new MaterialStack(Mats.MAT_CADMIUM, n))
                .outputs(new MaterialStack(Mats.MAT_CDALLOY, i)));

        register(new CrucibleRecipe("crucible.bbronze").setup(9, port("bismuth_bronze_ingot"))
                .inputs(new MaterialStack(Mats.MAT_COPPER, n * 8), new MaterialStack(Mats.MAT_BISMUTH, n), new MaterialStack(Mats.MAT_FLUX, n * 3))
                .outputs(new MaterialStack(Mats.MAT_BBRONZE, i), new MaterialStack(Mats.MAT_SLAG, n * 3)));

        register(new CrucibleRecipe("crucible.abronze").setup(9, port("arsenic_bronze_ingot"))
                .inputs(new MaterialStack(Mats.MAT_COPPER, n * 8), new MaterialStack(Mats.MAT_ARSENIC, n), new MaterialStack(Mats.MAT_FLUX, n * 3))
                .outputs(new MaterialStack(Mats.MAT_ABRONZE, i), new MaterialStack(Mats.MAT_SLAG, n * 3)));

        register(new CrucibleRecipe("crucible.cmb").setup(3, port("combine_steel_ingot"))
                .inputs(new MaterialStack(Mats.MAT_MAGTUNG, n * 6), new MaterialStack(Mats.MAT_MUD, n * 3))
                .outputs(new MaterialStack(Mats.MAT_CMB, i)));

        register(new CrucibleRecipe("crucible.magtung").setup(3, port("magnetized_tungsten_ingot"))
                .inputs(new MaterialStack(Mats.MAT_TUNGSTEN, i), new MaterialStack(Mats.MAT_SCHRABIDIUM, n * 1))
                .outputs(new MaterialStack(Mats.MAT_MAGTUNG, i)));

        register(new CrucibleRecipe("crucible.bscco").setup(3, port("bscco_ingot"))
                .inputs(new MaterialStack(Mats.MAT_BISMUTH, n * 2), new MaterialStack(Mats.MAT_STRONTIUM, n * 2), new MaterialStack(Mats.MAT_CALCIUM, n * 2), new MaterialStack(Mats.MAT_COPPER, n * 3))
                .outputs(new MaterialStack(Mats.MAT_BSCCO, i)));
    }

    /** 1:1 {@code CrucibleRecipe}. */
    public static class CrucibleRecipe {

        public final String name;
        public MaterialStack[] input;
        public MaterialStack[] output;
        public int frequency = 1;
        private ItemStack icon = ItemStack.EMPTY;

        public CrucibleRecipe(String name) {
            this.name = name;
        }

        public CrucibleRecipe setup(int frequency, ItemStack icon) {
            this.frequency = frequency;
            this.icon = icon;
            return this;
        }

        public CrucibleRecipe inputs(MaterialStack... input) { this.input = input; return this; }
        public CrucibleRecipe outputs(MaterialStack... output) { this.output = output; return this; }

        public ItemStack getIcon() { return icon.copy(); }

        public int getInputAmount() {
            int content = 0;
            for (MaterialStack stack : input) content += stack.amount;
            return content;
        }

        public Component getLocalizedName() {
            return Component.translatable(name);
        }

        /** Original {@code print()}: Name, Eingaben, Ausgaben. */
        public List<Component> print(boolean showMb) {
            List<Component> list = new ArrayList<>();
            list.add(getLocalizedName().copy().withStyle(ChatFormatting.YELLOW));
            list.add(Component.translatable("gui.recipe.input").append(":").withStyle(ChatFormatting.BOLD));
            for (MaterialStack stack : input) {
                list.add(stack.material.getLocalizedName().copy().append(": " + Mats.formatAmount(stack.amount, showMb)));
            }
            list.add(Component.translatable("gui.recipe.output").append(":").withStyle(ChatFormatting.BOLD));
            for (MaterialStack stack : output) {
                list.add(stack.material.getLocalizedName().copy().append(": " + Mats.formatAmount(stack.amount, showMb)));
            }
            return list;
        }
    }
}
