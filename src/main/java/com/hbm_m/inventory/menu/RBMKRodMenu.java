package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.rbmk.RBMKRodBlockEntity;
import com.hbm_m.item.rbmk.RBMKRodItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class RBMKRodMenu extends AbstractContainerMenu {

    private final RBMKRodBlockEntity blockEntity;

    public RBMKRodMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, getBlockEntity(inv, buf));
    }

    public RBMKRodMenu(int id, Inventory inv, RBMKRodBlockEntity be) {
        super(ModMenuTypes.RBMK_ROD_MENU.get(), id);
        this.blockEntity = be;

        SimpleContainer container = new SimpleContainer(1) {
            @Override
            public void setChanged() {
                super.setChanged();
                be.fuelSlot = getItem(0).copy();
                be.setChanged();
            }
        };
        container.setItem(0, be.fuelSlot.copy());

        // Fuel rod slot — matches the real gui_rbmk_element.png layout (slot 0)
        addSlot(new Slot(container, 0, 80, 45) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof RBMKRodItem;
            }

            @Override
            public boolean mayPickup(Player player) {
                return player.isCreative() || be.coldEnoughForManual();
            }
        });

        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 162));
        }
    }

    private static RBMKRodBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof RBMKRodBlockEntity rbmk) return rbmk;
        throw new IllegalStateException("No RBMKRodBlockEntity at " + pos);
    }

    public RBMKRodBlockEntity getBlockEntity() { return blockEntity; }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.getLevel() == player.level()
            && player.distanceToSqr(blockEntity.getBlockPos().getCenter()) <= 64;
    }

    // Prevent any manual interaction with the fuel slot when the rod is too hot to handle.
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId == 0 && !player.isCreative() && !blockEntity.coldEnoughForManual()) return;
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();

            if (index == 0) {
                // Moving fuel rod out into the player inventory
                if (!blockEntity.coldEnoughForManual() && !player.isCreative()) return ItemStack.EMPTY;
                if (!this.moveItemStackTo(stackInSlot, 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving a rod from the player inventory into the fuel slot
                if (!this.moveItemStackTo(stackInSlot, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }
}
