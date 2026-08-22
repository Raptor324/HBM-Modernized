package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.rbmk.RBMKAutoloaderBlockEntity;
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

/**
 * Port of {@code ContainerRBMKAutoloader}.
 *
 * <p>The original had a separate 3x3 "fuel in" grid (slots 0-8) and a take-only
 * 3x3 "spent rods out" grid (slots 9-17), plus +/- buttons that adjusted a
 * {@code cycle} enrichment-threshold field on the tile entity.
 *
 * <p>The modernized {@link RBMKAutoloaderBlockEntity} was rebuilt with a single
 * shared 3x3 buffer ({@code SLOTS = 9}) that the block entity's own tick logic
 * uses both to pull fresh rods from and to deposit spent rods into - there is no
 * separate input/output split and no {@code cycle} threshold field. This menu is
 * therefore ported 1:1 in visual layout (one 3x3 grid at the original's "input"
 * position) but wired only to the fields that actually exist; the +/- cycle
 * buttons from the original GUI have no backing field and are intentionally
 * omitted.
 */
public class RBMKAutoloaderMenu extends AbstractContainerMenu {

    private final RBMKAutoloaderBlockEntity blockEntity;

    public RBMKAutoloaderMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, getBlockEntity(inv, buf));
    }

    public RBMKAutoloaderMenu(int id, Inventory inv, RBMKAutoloaderBlockEntity be) {
        super(ModMenuTypes.RBMK_AUTOLOADER_MENU.get(), id);
        this.blockEntity = be;

        SimpleContainer container = new SimpleContainer(RBMKAutoloaderBlockEntity.SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                for (int i = 0; i < RBMKAutoloaderBlockEntity.SLOTS; i++)
                    be.slots[i] = getItem(i).copy();
                be.setChanged();
            }
        };
        for (int i = 0; i < RBMKAutoloaderBlockEntity.SLOTS; i++)
            container.setItem(i, be.slots[i].copy());

        // Original ContainerRBMKAutoloader: a 3x3 input grid at (17,18) and a 3x3 take-only
        // output grid at (107,18). Fresh rods go in on the left, spent ones come out on the right.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = col + row * 3;
                addSlot(new Slot(container, index, 17 + col * 18, 18 + row * 18) {
                    @Override public boolean mayPlace(ItemStack s) { return be.isItemValidForSlot(index, s); }
                });
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = RBMKAutoloaderBlockEntity.INPUT_SLOTS + col + row * 3;
                addSlot(new Slot(container, index, 107 + col * 18, 18 + row * 18) {
                    // Take-only: the machine fills these, the player only empties them.
                    @Override public boolean mayPlace(ItemStack s) { return false; }
                });
            }
        }

        // Player inventory (matches original's playerInv(invPlayer, 8, 100)).
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 100 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, 8 + col * 18, 158));
    }

    private static RBMKAutoloaderBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof RBMKAutoloaderBlockEntity a) return a;
        throw new IllegalStateException("No RBMKAutoloaderBlockEntity at " + pos);
    }

    public RBMKAutoloaderBlockEntity getBlockEntity() { return blockEntity; }

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
            int machineSlots = RBMKAutoloaderBlockEntity.SLOTS;
            if (index < machineSlots) {
                if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
            } else {
                // Only the input half accepts rods; the output half is machine-filled.
                if (!moveItemStackTo(stack, 0, RBMKAutoloaderBlockEntity.INPUT_SLOTS, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
