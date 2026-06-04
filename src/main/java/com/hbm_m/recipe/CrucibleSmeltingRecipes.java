package com.hbm_m.recipe;

import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.inventory.material.MaterialType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.*;

public class CrucibleSmeltingRecipes {

    public record SmeltingEntry(Ingredient input, MaterialType output, int amountMb) {}

    private static final List<SmeltingEntry> RECIPES = new ArrayList<>();

    public static void register(Ingredient input, MaterialType mat, int mb) {
        RECIPES.add(new SmeltingEntry(input, mat, mb));
    }

    private static void ingotTag(String forgeTag, MaterialType mat) {
        TagKey<Item> key = TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath("forge", forgeTag));
        register(Ingredient.of(key), mat, MaterialStack.MB_PER_INGOT);
    }

    private static void dustTag(String forgeTag, MaterialType mat, int mb) {
        TagKey<Item> key = TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath("forge", forgeTag));
        register(Ingredient.of(key), mat, mb);
    }

    public static void registerDefaults() {
        RECIPES.clear();

        ingotTag("ingots/iron",         MaterialType.IRON);
        ingotTag("ingots/gold",         MaterialType.GOLD);
        ingotTag("ingots/copper",       MaterialType.COPPER);
        ingotTag("ingots/titanium",     MaterialType.TITANIUM);
        ingotTag("ingots/aluminum",     MaterialType.ALUMINIUM);
        ingotTag("ingots/aluminium",    MaterialType.ALUMINIUM);
        ingotTag("ingots/tungsten",     MaterialType.TUNGSTEN);
        ingotTag("ingots/zirconium",    MaterialType.ZIRCONIUM);
        ingotTag("ingots/osmiridium",   MaterialType.OSMIRIDIUM);
        ingotTag("ingots/steel",        MaterialType.STEEL);
        ingotTag("ingots/alloy",        MaterialType.ALLOY);
        ingotTag("ingots/dura_steel",   MaterialType.DURA_STEEL);
        ingotTag("ingots/desh",         MaterialType.DESH);
        ingotTag("ingots/star_metal",   MaterialType.STAR_METAL);
        ingotTag("ingots/tcalloy",      MaterialType.TCALLOY);
        ingotTag("ingots/cdalloy",      MaterialType.CDALLOY);
        ingotTag("ingots/cmb",          MaterialType.CMB);
        ingotTag("ingots/schrabidium",  MaterialType.SCHRABIDIUM);
        ingotTag("ingots/bbronze",      MaterialType.BBRONZE);
        ingotTag("ingots/abronze",      MaterialType.ABRONZE);
        ingotTag("ingots/saturnite",    MaterialType.SATURNITE);
        ingotTag("ingots/lead",         MaterialType.LEAD);
        ingotTag("ingots/bismuth",      MaterialType.BISMUTH);
        ingotTag("ingots/beryllium",    MaterialType.BERYLLIUM);
        ingotTag("ingots/cobalt",       MaterialType.COBALT);
        ingotTag("ingots/nickel",       MaterialType.NICKEL);

        dustTag("ores/iron",    MaterialType.IRON,    MaterialStack.MB_PER_INGOT * 2);
        dustTag("ores/copper",  MaterialType.COPPER,  MaterialStack.MB_PER_INGOT * 2);
        dustTag("ores/gold",    MaterialType.GOLD,    MaterialStack.MB_PER_INGOT * 2);
        dustTag("ores/titanium",MaterialType.TITANIUM,MaterialStack.MB_PER_INGOT * 2);
    }

    public static List<SmeltingEntry> getRecipes() {
        return Collections.unmodifiableList(RECIPES);
    }

    public static MaterialStack smelt(ItemStack input) {
        for (SmeltingEntry e : RECIPES) {
            if (e.input().test(input)) {
                return new MaterialStack(e.output(), e.amountMb() * input.getCount());
            }
        }
        return null;
    }
}
