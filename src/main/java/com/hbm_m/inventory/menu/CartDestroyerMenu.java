package com.hbm_m.inventory.menu;

import org.jetbrains.annotations.NotNull;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code ContainerCartDestroyer}: links 3x3 exakte, rechts 3x3 Platzhalter-Filter als Geisterplaetze. Ein Klick
 * legt eine Kopie des gehaltenen Gegenstands hinein (oder leert den Platz), Shift-Klick tut nichts.
 */
public class CartDestroyerMenu extends AbstractContainerMenu {

    public final Container cart;

    public CartDestroyerMenu(int id, Inventory inv) {
        this(id, inv, new SimpleContainer(18));
    }

    public CartDestroyerMenu(int id, Inventory inv, Container cart) {
        super(ModMenuTypes.CART_DESTROYER_MENU.get(), id);
        this.cart = cart;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.addSlot(new PatternSlot(cart, j + i * 3, 10 + j * 18, 17 + i * 18));
                this.addSlot(new PatternSlot(cart, j + i * 3 + 9, 114 + j * 18, 17 + i * 18));
            }
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inv, i, 8 + i * 18, 142));
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        return ItemStack.EMPTY;
    }

    /** Original {@code slotClick}: Geisterplatz uebernimmt das gehaltene Item ohne es zu verbrauchen. */
    @Override
    public void clicked(int index, int button, @NotNull ClickType clickType, @NotNull Player player) {
        if (index < 0 || index >= 18) {
            super.clicked(index, button, clickType, player);
            return;
        }

        Slot slot = this.getSlot(index);
        slot.set(this.getCarried().copy());
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return cart.stillValid(player);
    }
}
