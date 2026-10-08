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
 * Меню многоцелевой бомбы: 4 заряда по углам + 2 модификатора в центре.
 */
public class BombMultiMenu extends AbstractContainerMenu {

    public final BombMultiBlockEntity be;

    public BombMultiMenu(int id, Inventory playerInv, FriendlyByteBuf extraData) {
        this(id, playerInv, extraData == null ? null
                : (BombMultiBlockEntity) playerInv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public BombMultiMenu(int id, Inventory inventory, BombMultiBlockEntity blockEntity) {
        super(ModMenuTypes.BOMB_MULTI_MENU.get(), id);
        this.be = blockEntity;

        // Original ContainerBombMulti: 2x3-Raster
        int[][] pos = {{44, 26}, {62, 26}, {80, 26}, {44, 44}, {62, 44}, {80, 44}};
        for (int slot = 0; slot < BombMultiBlockEntity.SLOTS; slot++) {
            addSlot(new Slot(be, slot, pos[slot][0], pos[slot][1]));
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

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack var3 = ItemStack.EMPTY;
        Slot var4 = this.slots.get(index);

        if (var4 != null && var4.hasItem()) {
            ItemStack var5 = var4.getItem();
            var3 = var5.copy();

            if (index <= 5) {
                if (!this.moveItemStackTo(var5, 6, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }

            if (var5.isEmpty()) {
                var4.set(ItemStack.EMPTY);
            } else {
                var4.setChanged();
            }
        }

        return var3;
    }
}
