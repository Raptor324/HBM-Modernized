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
 * <p>The original has a 3x3 "fuel in" grid (slots 0-8, insertion restricted by
 * {@code isItemValidForSlot}) and a take-only 3x3 "spent rods out" grid (slots 9-17), plus the
 * +/- buttons in {@code GUIRBMKAutoloader} that adjust the tile entity's {@code cycle}
 * enrichment-threshold field (±5, clamped 5..95 server-side through
 * {@code RBMKAutoloaderPacket}). This menu wires all 18 slots of the matching
 * {@link RBMKAutoloaderBlockEntity} inventory 1:1 in the original layout (input grid at 17,18,
 * output grid at 107,18, player inventory at 8,100).</p>
 */
public class RBMKAutoloaderMenu extends AbstractContainerMenu {

    private final RBMKAutoloaderBlockEntity blockEntity;

    public RBMKAutoloaderMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, getBlockEntity(inv, buf));
    }

    public RBMKAutoloaderMenu(int id, Inventory inv, RBMKAutoloaderBlockEntity be) {
        super(ModMenuTypes.RBMK_AUTOLOADER_MENU.get(), id);
        this.blockEntity = be;

        // тайл может отсутствовать на клиенте (реплей Flashback) — заглушка без синхронизации с тайлом
        SimpleContainer container = new SimpleContainer(RBMKAutoloaderBlockEntity.SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                if (be == null) return;
                for (int i = 0; i < RBMKAutoloaderBlockEntity.SLOTS; i++)
                    be.slots[i] = getItem(i).copy();
                be.setChanged();
            }
        };
        if (be != null) {
            for (int i = 0; i < RBMKAutoloaderBlockEntity.SLOTS; i++)
                container.setItem(i, be.slots[i].copy());
        }

        // Original ContainerRBMKAutoloader: a 3x3 input grid at (17,18) and a 3x3 take-only
        // output grid at (107,18). Fresh rods go in on the left, spent ones come out on the right.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = col + row * 3;
                addSlot(new Slot(container, index, 17 + col * 18, 18 + row * 18) {
                    @Override public boolean mayPlace(ItemStack s) { return be != null && be.isItemValidForSlot(index, s); }
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
        // На клиенте тайл может отсутствовать (реплей Flashback) — возвращаем null.
        // На сервере отсутствие тайла — реальный баг, поэтому там падаем как раньше.
        if (inv.player.level().isClientSide) return null;
        throw new MenuBlockEntityMissingException("No RBMKAutoloaderBlockEntity at " + pos);
    }

    public RBMKAutoloaderBlockEntity getBlockEntity() { return blockEntity; }

    /**
     * The menu holds a snapshot of the slots while the block keeps ticking and changing its own
     * fields. Without the re-sync a slot click wrote the stale snapshot back into the BE. Same
     * trick as in {@code RBMKRodMenu}.
     */
    @Override
    public void broadcastChanges() {
        if (blockEntity != null) {
            for (int i = 0; i < RBMKAutoloaderBlockEntity.SLOTS; i++) {
                Slot slot = this.slots.get(i);
                if (!ItemStack.matches(slot.getItem(), blockEntity.slots[i])) {
                    slot.set(blockEntity.slots[i].copy());
                }
            }
        }
        super.broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        // тайл может отсутствовать на клиенте (реплей Flashback)
        if (blockEntity == null) {
            return false;
        }
        return MenuReach.stillValid(player, blockEntity);
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
