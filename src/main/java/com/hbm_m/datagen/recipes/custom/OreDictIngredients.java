package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.FluidContainerIngredient;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.crafting.PartialNBTIngredient;

/**
 * Hilfen fuer die automatisch uebersetzten Original-Rezepte: Gegenstaende ueber ihre Registry-ID (unbekannte IDs
 * brechen den Datagen-Lauf ab, statt still ein leeres Rezept zu schreiben), OreDict-Schluessel als Item-Tags
 * {@code hbm_m:oredict/...} und {@code Fluids.X.getDict(menge)} als Fluessigkeitsbehaelter-Zutat.
 */
public final class OreDictIngredients {

    private OreDictIngredients() {}

    /** Unbekannte IDs eines Laufs: werden gesammelt und am Ende von {@link #checkMissing} gemeldet. */
    private static final java.util.Set<String> MISSING = new java.util.TreeSet<>();

    public static Item item(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        Item item = rl == null ? Items.AIR : BuiltInRegistries.ITEM.get(rl);
        if (item == Items.AIR) {
            MISSING.add(id);
            return Items.BARRIER;
        }
        return item;
    }

    /** Bricht den Datagen-Lauf ab, wenn eine uebersetzte Zuordnung auf einen nicht registrierten Gegenstand zeigt. */
    public static void checkMissing(String where) {
        if (!MISSING.isEmpty()) throw new IllegalStateException("Rezept-Uebersetzung (" + where + "): unbekannte Gegenstaende " + MISSING);
    }

    public static ItemStack stack(String id, int count) {
        return new ItemStack(item(id), count);
    }

    public static TagKey<Item> oreTag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, path));
    }

    /** {@code OreDictStack(KEY)} des Originals. */
    public static Ingredient ore(String path) {
        if (!OreDictTagData.TAGS.containsKey(path)) throw new IllegalStateException("OreDict-Tag ohne Inhalt: " + path);
        return Ingredient.of(oreTag(path));
    }

    /** {@code Fluids.X.getDict(menge)}: jeder volle Behaelter mit genau dieser Menge. */
    public static Ingredient container(Fluid fluid, int amount) {
        return FluidContainerIngredient.of(fluid, amount);
    }

    /** {@code ComparableStack(behaelter, n, Fluids.X.getID())}: genau dieser Behaelter mit dieser Fluessigkeit. */
    public static Ingredient filled(String id, Fluid fluid) {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", BuiltInRegistries.FLUID.getKey(fluid).toString());
        return PartialNBTIngredient.of(item(id), tag);
    }
}
//?}
