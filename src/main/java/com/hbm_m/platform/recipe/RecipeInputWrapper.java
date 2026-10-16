package com.hbm_m.platform.recipe;

import net.minecraft.world.item.ItemStack;

//? if < 1.21.1 {
import net.minecraft.world.Container;
//?} else {
/*import net.minecraft.world.item.crafting.RecipeInput;
*///?}

/**
 * Кросс-версионный «снимок» инвентаря для матчинга рецептов.
 *
 * <p>На 1.20.1 оборачивает {@link net.minecraft.world.Container Container} (живая ссылка).
 * На 1.21.1 {@code SimpleContainer} больше НЕ реализует {@code RecipeInput}, поэтому здесь
 * хранится копия слотов в массиве {@link ItemStack} — это позволяет строить обёртку как из
 * настоящего {@link RecipeInput}, так и из {@link net.minecraft.world.SimpleContainer}.
 * Матчинг рецептов только читает слоты, поэтому копия безопасна.
 */
public class RecipeInputWrapper {
    //? if < 1.21.1 {

    private final Container container;
    public RecipeInputWrapper(Container container) { this.container = container; }
    public ItemStack getItem(int slot) { return container.getItem(slot); }
    public int size() { return container.getContainerSize(); }
    //?} else {
    /*private final ItemStack[] items;

    public RecipeInputWrapper(RecipeInput input) {
        this.items = new ItemStack[input.size()];
        for (int i = 0; i < this.items.length; i++) {
            this.items[i] = input.getItem(i);
        }
    }

    public RecipeInputWrapper(net.minecraft.world.SimpleContainer container) {
        int n = container.getContainerSize();
        this.items = new ItemStack[n];
        for (int i = 0; i < n; i++) {
            this.items[i] = container.getItem(i);
        }
    }

    public ItemStack getItem(int slot) { return this.items[slot]; }
    public int size() { return this.items.length; }
    *///?}
}
