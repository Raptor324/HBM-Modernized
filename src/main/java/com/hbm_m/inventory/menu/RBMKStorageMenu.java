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
                if (be == null) return;
                for (int i = 0; i < RBMKStorageBlockEntity.SLOTS; i++)
                    be.slots[i] = getItem(i).copy();
                be.setChanged();
            }
        };
        if (be != null) {
            for (int i = 0; i < RBMKStorageBlockEntity.SLOTS; i++)
                container.setItem(i, be.slots[i].copy());
        }

        // 12 slots in a 3-row x 4-column grid (1:1 port of ContainerRBMKStorage),
        // matching the vertical-column layout baked into gui_rbmk_storage.png.
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 4; j++) {
                int index = i + j * 3;
                addSlot(new Slot(container, index, 32 + 32 * j, 29 + 16 * i) {
                    @Override public boolean mayPlace(ItemStack s) { return s.getItem() instanceof RBMKRodItem; }
                });
            }
        }

        // Player inventory (3 rows) + hotbar
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, 8 + col * 18, 162));
    }

    private static RBMKStorageBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof RBMKStorageBlockEntity s) return s;
        // The tile can be missing on the client (Flashback replay) - return null there. On the
        // server a missing tile is a real bug, so it still throws. Same contract as the sibling
        // RBMK menus.
        if (inv.player.level().isClientSide) return null;
        throw new IllegalStateException("No RBMKStorageBlockEntity at " + pos);
    }

    public RBMKStorageBlockEntity getBlockEntity() { return blockEntity; }

    /**
     * The menu holds a snapshot of the slots while the block keeps ticking and changing its own
     * fields. Without the re-sync a slot click wrote the stale snapshot back into the BE. Same
     * trick as in {@code RBMKRodMenu}.
     */
    @Override
    public void broadcastChanges() {
        if (blockEntity != null) {
            for (int i = 0; i < RBMKStorageBlockEntity.SLOTS; i++) {
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
        if (blockEntity == null) return false; // the tile can be missing on the client
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
