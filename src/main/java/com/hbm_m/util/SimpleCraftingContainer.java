package com.hbm_m.util;

import java.util.List;

import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;

/**
 * Minimaler {@link CraftingContainer}-Adapter ueber eine feste {@link NonNullList} von
 * {@link ItemStack}s - fuer Maschinen, die Vanilla-Crafting-Rezepte (shaped/shapeless) ausserhalb
 * eines echten Spieler-{@code AbstractContainerMenu} matchen muessen (z.B. {@code
 * MachineAutocrafterBlockEntity}, {@code MachineFunnelBlockEntity}).
 */
public class SimpleCraftingContainer implements CraftingContainer {

    private final NonNullList<ItemStack> items;
    private final int width;
    private final int height;

    public SimpleCraftingContainer(NonNullList<ItemStack> items, int width, int height) {
        this.items = items;
        this.width = width;
        this.height = height;
    }

    @Override public int getWidth() { return width; }
    @Override public int getHeight() { return height; }
    @Override public int getContainerSize() { return items.size(); }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) if (!stack.isEmpty()) return false;
        return true;
    }

    @Override public ItemStack getItem(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return ContainerHelper.removeItem(items, slot, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); }
    @Override public void setChanged() { }
    @Override public boolean stillValid(Player player) { return true; }
    @Override public void clearContent() { items.clear(); }

    @Override
    public List<ItemStack> getItems() {
        return items;
    }

    @Override
    public void fillStackedContents(StackedContents contents) {
        for (ItemStack stack : items) contents.accountStack(stack);
    }

    //? if >= 1.21.1 {
    /*// 1.21.1: CraftingRecipe.assemble/getRecipeFor требуют CraftingInput вместо CraftingContainer.
    public net.minecraft.world.item.crafting.CraftingInput toCraftingInput() {
        return net.minecraft.world.item.crafting.CraftingInput.of(width, height, new java.util.ArrayList<>(items));
    }
    *///?}
}
