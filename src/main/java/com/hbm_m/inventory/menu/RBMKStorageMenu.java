package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.rbmk.RBMKStorageBlockEntity;
import com.hbm_m.item.rbmk.RBMKRodItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class RBMKStorageMenu extends AbstractContainerMenu {

    private final RBMKStorageBlockEntity blockEntity;

    public RBMKStorageMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, getBlockEntity(inv, buf));
    }

    public RBMKStorageMenu(int id, Inventory inv, RBMKStorageBlockEntity be) {
        super(ModMenuTypes.RBMK_STORAGE_MENU.get(), id);
        this.blockEntity = be;

        SimpleContainer container = new SimpleContainer(RBMKStorageBlockEntity.SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                for (int i = 0; i < RBMKStorageBlockEntity.SLOTS; i++)
                    be.slots[i] = getItem(i).copy();
                be.setChanged();
            }
        };
        for (int i = 0; i < RBMKStorageBlockEntity.SLOTS; i++)
            container.setItem(i, be.slots[i].copy());

        // 12 slots in 2 rows of 6, centred
        for (int i = 0; i < 12; i++) {
            int row = i / 6;
            int col = i % 6;
            addSlot(new Slot(container, i, 8 + col * 18, 18 + row * 18) {
                @Override public boolean mayPlace(ItemStack s) { return s.getItem() instanceof RBMKRodItem; }
            });
        }

        // Player inventory (3 rows) + hotbar
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 76 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, 8 + col * 18, 134));
    }

    private static RBMKStorageBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof RBMKStorageBlockEntity s) return s;
        throw new IllegalStateException("No RBMKStorageBlockEntity at " + pos);
    }

    public RBMKStorageBlockEntity getBlockEntity() { return blockEntity; }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.getLevel() == player.level()
            && player.distanceToSqr(blockEntity.getBlockPos().getCenter()) <= 64;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int machineSlots = RBMKStorageBlockEntity.SLOTS;
            if (index < machineSlots) {
                if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(stack, 0, machineSlots, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
