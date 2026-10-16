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

        int[][] pos = {{44, 18}, {116, 18}, {44, 54}, {116, 54}, {71, 27}, {89, 45}};
        for (int slot = 0; slot < BombMultiBlockEntity.SLOTS; slot++) {
            final int index = slot;
            addSlot(new Slot(be, index, pos[index][0], pos[index][1]) {
                @Override
                public boolean mayPlace(ItemStack stack) { return be.canPlaceItem(index, stack); }
            });
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
        return ItemStack.EMPTY;
    }
}
