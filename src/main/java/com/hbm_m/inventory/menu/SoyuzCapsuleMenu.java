package com.hbm_m.inventory.menu;

import org.jetbrains.annotations.NotNull;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ContainerSoyuzCapsule}: 6x3 Fracht ab (62,18), Raketenplatz bei (17,36), Spielerinventar ab Y 104. */
public class SoyuzCapsuleMenu extends AbstractContainerMenu {

    public final Container capsule;

    public SoyuzCapsuleMenu(int id, Inventory inv) {
        this(id, inv, new SimpleContainer(19));
    }

    public SoyuzCapsuleMenu(int id, Inventory inv, Container tedf) {
        super(ModMenuTypes.SOYUZ_CAPSULE_MENU.get(), id);
        this.capsule = tedf;
        tedf.startOpen(inv.player);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 6; j++) {
                this.addSlot(new Slot(tedf, j + i * 6, 26 + j * 18 + 18 * 2, 18 + i * 18));
            }
        }

        this.addSlot(new Slot(tedf, 18, 17, 36));

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 104 + i * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inv, i, 8 + i * 18, 162));
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int par2) {
        ItemStack var3 = ItemStack.EMPTY;
        Slot var4 = this.slots.get(par2);

        if (var4 != null && var4.hasItem()) {
            ItemStack var5 = var4.getItem();
            var3 = var5.copy();

            if (par2 <= capsule.getContainerSize() - 1) {
                if (!this.moveItemStackTo(var5, capsule.getContainerSize(), this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(var5, 0, capsule.getContainerSize(), false)) {
                return ItemStack.EMPTY;
            }

            if (var5.isEmpty()) var4.set(ItemStack.EMPTY);
            else var4.setChanged();
        }

        return var3;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return capsule.stillValid(player);
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        capsule.stopOpen(player);
    }
}
