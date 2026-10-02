package com.hbm_m.inventory.recipes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 1:1 {@code com.hbm.inventory.recipes.LemegetonRecipes}: Materialaufwertung 1 -> 1 im Lemegeton. Die OreDict-
 * Eintraege ({@code ingotX}) werden als Port-Materialgegenstand plus Forge-Tag {@code forge:ingots/x} geprueft.
 */
public final class LemegetonRecipes {

    private LemegetonRecipes() {}

    private record Entry(Predicate<ItemStack> input, Supplier<ItemStack> output) {}

    private static List<Entry> recipes;

    private static Item ingot(ModMaterials mat) {
        return ModMaterialItems.item(mat, MaterialShape.INGOT);
    }

    private static TagKey<Item> forge(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", path));
    }

    /** OreDictStack(X.ingot()): Port-Barren oder Forge-Tag ingots/x. */
    private static Predicate<ItemStack> ore(Supplier<Item> item, String tag) {
        TagKey<Item> key = forge(tag);
        return s -> s.is(item.get()) || s.is(key);
    }

    private static void put(Predicate<ItemStack> in, Supplier<Item> out) {
        recipes.add(new Entry(in, () -> new ItemStack(out.get())));
    }

    private static void register() {
        recipes = new ArrayList<>();
        put(ore(() -> Items.IRON_INGOT, "ingots/iron"), () -> ingot(ModMaterials.STEEL));
        put(ore(() -> ingot(ModMaterials.STEEL), "ingots/steel"), () -> ingot(ModMaterials.DURA_STEEL));
        put(ore(() -> ingot(ModMaterials.DURA_STEEL), "ingots/dura_steel"), () -> ingot(ModMaterials.TCALLOY));
        put(ore(() -> ingot(ModMaterials.TCALLOY), "ingots/tcalloy"), () -> ingot(ModMaterials.COMBINE_STEEL));
        put(ore(() -> ingot(ModMaterials.COMBINE_STEEL), "ingots/combine_steel"), () -> ingot(ModMaterials.DINEUTRONIUM));
        put(ore(() -> ingot(ModMaterials.TITANIUM), "ingots/titanium"), () -> ingot(ModMaterials.SATURNITE));
        put(ore(() -> ingot(ModMaterials.SATURNITE), "ingots/saturnite"), () -> ingot(ModMaterials.STARMETAL));
        put(ore(() -> Items.COPPER_INGOT, "ingots/copper"), () -> ingot(ModMaterials.RED_COPPER));
        put(ore(() -> ingot(ModMaterials.RED_COPPER), "ingots/mingrade"), () -> ingot(ModMaterials.DESH));
        put(ore(() -> ingot(ModMaterials.DESH), "ingots/desh"), () -> ingot(ModMaterials.BSCCO));
        put(ore(() -> ingot(ModMaterials.LEAD), "ingots/lead"), () -> Items.GOLD_INGOT);
        put(ore(() -> Items.GOLD_INGOT, "ingots/gold"), () -> ingot(ModMaterials.BISMUTH));
        put(ore(() -> ingot(ModMaterials.BISMUTH), "ingots/bismuth"), () -> ingot(ModMaterials.OSMIRIDIUM));
        put(ore(() -> ingot(ModMaterials.THORIUM232), "ingots/thorium232"), () -> ingot(ModMaterials.URANIUM));
        put(ore(() -> ingot(ModMaterials.URANIUM), "ingots/uranium"), () -> ingot(ModMaterials.URANIUM238));
        put(ore(() -> ingot(ModMaterials.URANIUM238), "ingots/uranium238"), () -> ingot(ModMaterials.URANIUM235));
        put(ore(() -> ingot(ModMaterials.URANIUM235), "ingots/uranium235"), () -> ingot(ModMaterials.PLUTONIUM));
        put(ore(() -> ingot(ModMaterials.PLUTONIUM), "ingots/plutonium"), () -> ingot(ModMaterials.PLUTONIUM238));
        put(ore(() -> ingot(ModMaterials.PLUTONIUM238), "ingots/plutonium238"), () -> ingot(ModMaterials.PLUTONIUM239));
        put(ore(() -> ingot(ModMaterials.PLUTONIUM239), "ingots/plutonium239"), () -> ingot(ModMaterials.PLUTONIUM240));
        put(ore(() -> ingot(ModMaterials.PLUTONIUM240), "ingots/plutonium240"), () -> ingot(ModMaterials.PLUTONIUM241));
        put(ore(() -> ingot(ModMaterials.PLUTONIUM241), "ingots/plutonium241"), () -> ingot(ModMaterials.AM241));
        put(ore(() -> ingot(ModMaterials.AM241), "ingots/americium241"), () -> ingot(ModMaterials.AM242));
        put(ore(() -> ingot(ModMaterials.RA226), "ingots/radium226"), () -> ingot(ModMaterials.POLONIUM));
        put(ore(() -> ingot(ModMaterials.POLONIUM), "ingots/polonium210"), () -> ingot(ModMaterials.TECHNETIUM));
        put(ore(() -> ingot(ModMaterials.POLYMER), "ingots/polymer"), () -> ingot(ModMaterials.POLYMER_COMPOSITE));
        put(ore(() -> ingot(ModMaterials.BAKELITE), "ingots/bakelite"), () -> ingot(ModMaterials.PVC));
        put(ore(() -> ingot(ModMaterials.BIORUBBER), "ingots/latex"), () -> ingot(ModMaterials.RUBBER));
        put(ore(() -> Items.COAL, "gems/coal"), () -> ingot(ModMaterials.GRAPHITE));
        put(ore(() -> ingot(ModMaterials.GRAPHITE), "ingots/graphite"), () -> Items.DIAMOND);
        put(ore(() -> Items.DIAMOND, "gems/diamond"), () -> ingot(ModMaterials.CFT));
        put(ore(() -> ModMaterialItems.item(ModMaterials.FLUORITE, MaterialShape.POWDER), "dusts/fluorite"), () -> ModItems.GEM_SODALITE.get());
        put(ore(() -> ModItems.GEM_SODALITE.get(), "gems/sodalite"), () -> ModItems.GEM_VOLCANIC.get());
        put(ore(() -> ModItems.GEM_VOLCANIC.get(), "gems/volcanic"), () -> ModItems.GEM_RAD.get());
        put(s -> s.is(ModItems.GEM_RAD.get()), () -> ModItems.GEM_ALEXANDRITE.get());
        put(s -> s.is(ItemTags.SAND), () -> ingot(ModMaterials.FIBERGLASS));
        put(ore(() -> ingot(ModMaterials.FIBERGLASS), "ingots/fiberglass"), () -> ingot(ModMaterials.ASBESTOS));
    }

    public static ItemStack getRecipe(ItemStack ingredient) {
        if (ingredient.isEmpty()) return ItemStack.EMPTY;
        if (recipes == null) register();
        for (Entry entry : recipes) {
            if (entry.input().test(ingredient)) {
                return entry.output().get().copy();
            }
        }
        return ItemStack.EMPTY;
    }
}
