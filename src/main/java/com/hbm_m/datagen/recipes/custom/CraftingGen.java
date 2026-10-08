package com.hbm_m.datagen.recipes.custom;

import java.util.function.Consumer;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.lib.RefStrings;

import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

/**
 * Gemeinsame Helfer der Restport-Rezeptgeneratoren: {@code CraftingManager.addRecipeAuto} /
 * {@code addShapelessAuto} des Originals. Rezept-IDs: {@code <prefix>/<ausgabe>_<laufnummer>}.
 */
public final class CraftingGen {

    private final Consumer<FinishedRecipe> w;
    private final String prefix;
    private int counter = 0;
    /** Konfig-Schalter fuer das naechste Rezept (Original if(GeneralConfig...)-Block), siehe {@link #when}. */
    private String[] pending = new String[0];

    public CraftingGen(Consumer<FinishedRecipe> w, String prefix) {
        this.w = w;
        this.prefix = prefix;
    }

    public static Item m(ModMaterials mat, MaterialShape shape) {
        if (shape == MaterialShape.BLOCK && ModBlocks.hasIngotBlock(mat)) return ModBlocks.getIngotBlock(mat).get().asItem();
        Item i = ModMaterialItems.item(mat, shape);
        if (i == null) throw new IllegalStateException("Material fehlt: " + mat + " / " + shape);
        return i;
    }

    public static Ingredient ing(Object o) {
        if (o instanceof Ingredient in) return in;
        if (o instanceof ItemLike il) return Ingredient.of(il);
        if (o instanceof dev.architectury.registry.registries.RegistrySupplier<?> rs) return Ingredient.of((ItemLike) rs.get());
        throw new IllegalArgumentException(String.valueOf(o));
    }

    public static ItemLike like(Object o) {
        if (o instanceof ItemLike il) return il;
        if (o instanceof dev.architectury.registry.registries.RegistrySupplier<?> rs) return (ItemLike) rs.get();
        throw new IllegalArgumentException(String.valueOf(o));
    }

    public static String[] p(String... rows) {
        return rows;
    }

    /** Das naechste Rezept existiert nur bei diesen Konfig-Schaltern ({@link ConfigRecipes#when}). */
    public CraftingGen when(String... flags) {
        this.pending = flags;
        return this;
    }

    private Consumer<FinishedRecipe> writer() {
        Consumer<FinishedRecipe> out = pending.length == 0 ? w : ConfigRecipes.when(w, pending);
        pending = new String[0];
        return out;
    }

    private ResourceLocation id(Object out) {
        String name = BuiltInRegistries.ITEM.getKey(like(out).asItem()).getPath();
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, prefix + "/" + name + "_" + (counter++));
    }

    public void shaped(Object out, int count, String[] pattern, Object... keys) {
        ShapedRecipeBuilder b = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, like(out), count);
        for (String row : pattern) b.pattern(row);
        String joined = String.join("", pattern);
        for (int i = 0; i < keys.length; i += 2) {
            char c = (Character) keys[i];
            if (joined.indexOf(c) < 0) continue;
            b.define(c, ing(keys[i + 1]));
        }
        b.unlockedBy("has_item", InventoryChangeTrigger.TriggerInstance.hasItems(like(out)));
        b.save(writer(), id(out));
    }

    public void shapeless(Object out, int count, Object... inputs) {
        ShapelessRecipeBuilder b = ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, like(out), count);
        for (Object o : inputs) b.requires(ing(o));
        b.unlockedBy("has_item", InventoryChangeTrigger.TriggerInstance.hasItems(like(out)));
        b.save(writer(), id(out));
    }
}
