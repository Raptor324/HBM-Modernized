package com.hbm_m.inventory.menu;

import org.jetbrains.annotations.NotNull;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ContainerCrateSteel} fuer die Kisten-Lore: 9x6 Plaetze, Spielerinventar ab Y 140. */
public class CartCrateMenu extends AbstractContainerMenu {

    public final Container cart;

    public CartCrateMenu(int id, Inventory inv) {
        this(id, inv, new SimpleContainer(54));
    }

    public CartCrateMenu(int id, Inventory inv, Container cart) {
        super(ModMenuTypes.CART_CRATE_MENU.get(), id);
        this.cart = cart;
        cart.startOpen(inv.player);

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(cart, j + i * 9, 8 + j * 18, 18 + i * 18));
            }
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + (18 * 3) + 2 + i * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inv, i, 8 + i * 18, 142 + (18 * 3) + 2));
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack ret = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            ret = stack.copy();

            if (index < 54) {
                if (!this.moveItemStackTo(stack, 54, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(stack, 0, 54, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }

        return ret;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return cart.stillValid(player);
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        cart.stopOpen(player);
    }
}
