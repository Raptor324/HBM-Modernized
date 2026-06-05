package com.hbm_m.inventory.menu;

import com.hbm_m.block.entity.bomb.NukePrototypeBlockEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class NukePrototypeMenu extends AbstractContainerMenu {

    public final NukePrototypeBlockEntity be;

    public NukePrototypeMenu(int id, Inventory playerInv, FriendlyByteBuf extraData) {
        this(id, playerInv, (NukePrototypeBlockEntity) playerInv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public NukePrototypeMenu(int id, Inventory inventory, NukePrototypeBlockEntity blockEntity) {
        super(ModMenuTypes.NUKE_PROTOTYPE_MENU.get(), id);
        this.be = blockEntity;

        addSlot(new Slot(be,  0,   8, 35));
        addSlot(new Slot(be,  1,  26, 35));
        addSlot(new Slot(be,  2,  44, 26));
        addSlot(new Slot(be,  3,  44, 44));
        addSlot(new Slot(be,  4,  62, 26));
        addSlot(new Slot(be,  5,  62, 44));
        addSlot(new Slot(be,  6,  80, 26));
        addSlot(new Slot(be,  7,  80, 44));
        addSlot(new Slot(be,  8,  98, 26));
        addSlot(new Slot(be,  9,  98, 44));
        addSlot(new Slot(be, 10, 116, 26));
        addSlot(new Slot(be, 11, 116, 44));
        addSlot(new Slot(be, 12, 134, 35));
        addSlot(new Slot(be, 13, 152, 35));

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
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();

        if (index <= 13) {
            if (!this.moveItemStackTo(stack, 14, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return result;
    }
}
