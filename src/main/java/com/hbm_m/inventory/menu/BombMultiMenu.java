package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.bomb.BombMultiBlockEntity;
import com.hbm_m.inventory.menu.ModMenuTypes;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Меню многоцелевой бомбы — 1:1 {@code ContainerBombMulti}: обычные слоты без
 * ограничений вставки (сетка 3x2: заряды ТНТ в слотах 0, 1, 3, 4, модификаторы
 * в слотах 2 и 5), shift-click переносит только из бомбы в инвентарь.
 */
public class BombMultiMenu extends AbstractContainerMenu {

    public final BombMultiBlockEntity be;

    public BombMultiMenu(int id, Inventory playerInv, FriendlyByteBuf extraData) {
        this(id, playerInv, extraData != null && playerInv.player.level().getBlockEntity(extraData.readBlockPos()) instanceof BombMultiBlockEntity bm ? bm : null);
    }

    public BombMultiMenu(int id, Inventory inventory, BombMultiBlockEntity blockEntity) {
        super(ModMenuTypes.BOMB_MULTI_MENU.get(), id);
        this.be = blockEntity;

        // Оригинал ContainerBombMulti (слоты 0-5): (44,26) (62,26) (80,26) /
        // (44,44) (62,44) (80,44) — обычный Slot, предметы кладутся через GUI.
        net.minecraft.world.Container c = blockEntity != null ? blockEntity : new net.minecraft.world.SimpleContainer(BombMultiBlockEntity.SLOTS);
        int[][] pos = {{44, 26}, {62, 26}, {80, 26}, {44, 44}, {62, 44}, {80, 44}};
        for (int slot = 0; slot < BombMultiBlockEntity.SLOTS; slot++) {
            addSlot(new Slot(c, slot, pos[slot][0], pos[slot][1]));
        }

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                addSlot(new Slot(inventory, x + y * 9 + 9, 8 + x * 18, 84 + y * 18));
            }
        }
        for (int x = 0; x < 9; x++) {
            addSlot(new Slot(inventory, x, 8 + x * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return be.stillValid(player);
    }

    /** Оригинал {@code transferStackInSlot}: только слоты бомбы -> инвентарь. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack var3 = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack var5 = slot.getItem();
            var3 = var5.copy();

            if (index <= 5) {
                if (!this.moveItemStackTo(var5, 6, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }

            if (var5.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return var3;
    }
}
