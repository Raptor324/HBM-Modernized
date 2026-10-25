package com.hbm_m.recipe;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.item.special.ItemBookLore;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;

/**
 * 1:1 {@code MKUCraftingHandler}: das MKU-Rezept (Spritze {@code syringe_mkunicorn}) ist je Weltseed eine andere
 * Anordnung von sechs Zutaten im 3x3-Gitter. Die Meteor-Buecher ({@code book_iodine}, {@code book_dust} ...) verraten
 * jeweils, in welches Feld ihre Zutat gehoert ({@link #generateBook}).
 */
public class MKUCraftingRecipe extends CustomRecipe {

    public static ItemStack[] MKURecipe;
    private static long lastSeed;

    //? if < 1.21.1 {
    public MKUCraftingRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }
    //?} else {
    /*public MKUCraftingRecipe(CraftingBookCategory category) {
        super(category);
    }
    *///?}

    private static Item powderIodine() { return ModMaterialItems.item(ModMaterials.IODINE, MaterialShape.POWDER); }

    //? if < 1.21.1 {
    @Override
    public boolean matches(CraftingContainer inventory, Level world) {
        int width = inventory.getWidth();
        int height = inventory.getHeight();
    //?} else {
    /*@Override
    public boolean matches(net.minecraft.world.item.crafting.CraftingInput inventory, Level world) {
        int width = inventory.width();
        int height = inventory.height();
    *///?}

        // Seed gibt es nur auf dem Server; das Ergebnis berechnet ohnehin der Server
        if (!(world instanceof WorldGenLevel wgl)) return false;
        if (width != 3 || height != 3) return false;

        if (MKURecipe == null || wgl.getSeed() != lastSeed)
            generateRecipe(wgl.getSeed());

        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getItem(i % 3 + (i / 3) * width);
            ItemStack recipe = MKURecipe[i];

            if (stack.isEmpty() && recipe == null)
                continue;

            if (!stack.isEmpty() && recipe != null && stack.getItem() == recipe.getItem())
                continue;

            return false;
        }

        return true;
    }

    public static void generateRecipe(long seed) {
        Random rand = new Random(seed);

        if (lastSeed == seed && MKURecipe != null)
            return;

        lastSeed = seed;

        List<ItemStack> list = Arrays.asList(new ItemStack[] {
                new ItemStack(powderIodine()),
                new ItemStack(ModItems.FIRE_POWDER.get()),
                new ItemStack(ModItems.DUST.get()),
                new ItemStack(ModItems.NUGGET_MERCURY.get()),
                new ItemStack(ModItems.MORNING_GLORY.get()),
                new ItemStack(ModItems.SYRINGE_METAL_EMPTY.get()),
                null,
                null,
                null
        });

        Collections.shuffle(list, rand);

        MKURecipe = list.toArray(new ItemStack[9]);
    }

    public static Item getMKUItem(RandomSource rand) {
        switch (rand.nextInt(6)) {
            case 0: return powderIodine();
            case 1: return ModItems.FIRE_POWDER.get();
            case 2: return ModItems.DUST.get();
            case 3: return ModItems.NUGGET_MERCURY.get();
            case 4: return ModItems.MORNING_GLORY.get();
            case 5: return ModItems.SYRINGE_METAL_EMPTY.get();
            default: return ModItems.FLAME_PONY.get();
        }
    }

    public static ItemStack generateBook(long seed, Item mkuItem) {
        generateRecipe(seed);
        ItemStack[] recipe = MKURecipe;

        if (recipe == null) return new ItemStack(ModItems.FLAME_PONY.get());

        String key = null;
        int pages = 1;
        if (mkuItem == powderIodine()) { key = "book_iodine"; pages = 3; }
        if (mkuItem == ModItems.FIRE_POWDER.get()) { key = "book_phosphorous"; pages = 2; }
        if (mkuItem == ModItems.DUST.get()) { key = "book_dust"; pages = 3; }
        if (mkuItem == ModItems.NUGGET_MERCURY.get()) { key = "book_mercury"; pages = 2; }
        if (mkuItem == ModItems.MORNING_GLORY.get()) { key = "book_flower"; pages = 2; }
        if (mkuItem == ModItems.SYRINGE_METAL_EMPTY.get()) { key = "book_syringe"; pages = 2; }

        if (key == null) return new ItemStack(ModItems.FLAME_PONY.get());

        int s = 1;
        for (int i = 0; i < 9; i++) {
            if (recipe[i] != null && recipe[i].getItem() == mkuItem) {
                s = i + 1; break;
            }
        }

        ItemStack book = ItemBookLore.createBook(key, pages, 0x271E44, 0xFBFFF4);
        ItemBookLore.addArgs(book, pages - 1, String.valueOf(s));

        return book;
    }

    //? if < 1.21.1 {
    @Override
    public ItemStack assemble(CraftingContainer inventory, RegistryAccess registries) {
    //?} else {
    /*@Override
    public ItemStack assemble(net.minecraft.world.item.crafting.CraftingInput inventory, net.minecraft.core.HolderLookup.Provider registries) {
    *///?}
        return new ItemStack(ModItems.SYRINGE_MKUNICORN.get());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.MKU_SERIALIZER.get();
    }

    public static final SimpleCraftingRecipeSerializer<MKUCraftingRecipe> SERIALIZER =
            new SimpleCraftingRecipeSerializer<>(MKUCraftingRecipe::new);
}
