package com.hbm_m.inventory;

import java.util.Objects;
import java.util.function.Supplier;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * Port von {@code RecipesCommon.ComparableStack} fuer die Waffen-/Rezeptlogik: ein Gegenstand mit Menge, verglichen
 * nur ueber den Gegenstand (Metadaten gibt es im Port nicht mehr - jede Original-Meta ist ein eigener Gegenstand).
 * Der Gegenstand wird erst beim ersten Zugriff aufgeloest, damit Konfigurationen schon waehrend der Registrierung
 * auf noch nicht erzeugte Gegenstaende zeigen duerfen.
 */
public class ComparableStack {

    private final Supplier<? extends ItemLike> supplier;
    private Item item;
    public int stacksize;

    public ComparableStack(Supplier<? extends ItemLike> item, int stacksize) {
        this.supplier = item;
        this.stacksize = stacksize;
    }

    public ComparableStack(Supplier<? extends ItemLike> item) {
        this(item, 1);
    }

    public ComparableStack(ItemLike item, int stacksize) {
        this.supplier = null;
        this.item = item == null ? Items.AIR : item.asItem();
        this.stacksize = stacksize;
    }

    public ComparableStack(ItemLike item) {
        this(item, 1);
    }

    public ComparableStack(ItemStack stack) {
        this(stack.isEmpty() ? Items.AIR : stack.getItem(), stack.isEmpty() ? 0 : stack.getCount());
    }

    public Item getItem() {
        if (item == null) {
            ItemLike il = supplier != null ? supplier.get() : null;
            item = il == null ? Items.AIR : il.asItem();
        }
        return item;
    }

    public ItemStack toStack() {
        return new ItemStack(getItem(), stacksize);
    }

    public ComparableStack makeSingular() {
        return new ComparableStack(getItem(), 1);
    }

    public ComparableStack copy() {
        return new ComparableStack(getItem(), stacksize);
    }

    public ComparableStack copy(int stacksize) {
        return new ComparableStack(getItem(), stacksize);
    }

    public boolean matchesRecipe(ItemStack stack, boolean ignoreSize) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getItem() != getItem()) return false;
        return ignoreSize || stack.getCount() >= stacksize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ComparableStack other)) return false;
        return getItem() == other.getItem() && stacksize == other.stacksize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getItem(), stacksize);
    }
}
