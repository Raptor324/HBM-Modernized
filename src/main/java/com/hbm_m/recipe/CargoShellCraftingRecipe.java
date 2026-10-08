package com.hbm_m.recipe;

import com.hbm_m.item.weapon.ItemAmmoArty;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code CargoShellCraftingHandler}: leere Frachtgranate ({@code ammo_arty} Meta 8 ohne NBT) + genau ein weiterer
 * Gegenstand ohne Rueckgabe-Gegenstand = Frachtgranate mit diesem Gegenstand (eins) als {@code cargo}.
 */
public class CargoShellCraftingRecipe extends CustomRecipe {

    //? if < 1.21.1 {
    public CargoShellCraftingRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }
    //?} else {
    /*public CargoShellCraftingRecipe(CraftingBookCategory category) {
        super(category);
    }
    *///?}

    private static boolean isEmptyCargoShell(ItemStack stack) {
        return ItemAmmoArty.typeOf(stack) == ItemAmmoArty.CARGO && !PlatformHooks.hasItemTag(stack);
    }

    //? if < 1.21.1 {
    @Override
    public boolean matches(CraftingContainer inventory, Level level) {
        int size = inventory.getContainerSize();
    //?} else {
    /*@Override
    public boolean matches(net.minecraft.world.item.crafting.CraftingInput inventory, Level level) {
        int size = inventory.size();
    *///?}

        int itemCount = 0;
        int shellCount = 0;

        for (int i = 0; i < size; i++) {
            ItemStack stack = inventory.getItem(i);

            if (!stack.isEmpty()) {

                if (stack.hasCraftingRemainingItem())
                    return false;

                itemCount++;

                if (isEmptyCargoShell(stack)) {
                    shellCount++;
                }
            }
        }

        return itemCount == 2 && shellCount == 1;
    }

    //? if < 1.21.1 {
    @Override
    public ItemStack assemble(CraftingContainer inventory, RegistryAccess registries) {
        int size = inventory.getContainerSize();
    //?} else {
    /*@Override
    public ItemStack assemble(net.minecraft.world.item.crafting.CraftingInput inventory, net.minecraft.core.HolderLookup.Provider registries) {
        int size = inventory.size();
    *///?}

        ItemStack shell = null;
        ItemStack cargo = null;

        for (int i = 0; i < size; i++) {
            ItemStack stack = inventory.getItem(i);

            if (stack.isEmpty())
                continue;

            if (isEmptyCargoShell(stack)) {
                ItemStack copy = stack.copy();
                copy.setCount(1);
                shell = copy;
            } else {
                ItemStack copy = stack.copy();
                copy.setCount(1);
                cargo = copy;
            }
        }

        if (shell == null || cargo == null)
            return ItemStack.EMPTY;

        CompoundTag cargoTag = PlatformHooks.saveItemStack(cargo, new CompoundTag(), PlatformHooks.bestEffortProvider());
        PlatformHooks.put(shell, "cargo", cargoTag);

        return shell;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CARGO_SHELL_SERIALIZER.get();
    }

    public static final SimpleCraftingRecipeSerializer<CargoShellCraftingRecipe> SERIALIZER =
            new SimpleCraftingRecipeSerializer<>(CargoShellCraftingRecipe::new);
}
