package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.rbmk.RBMKOutgasserBlockEntity;
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
 * 1:1 port of {@code ContainerRBMKOutgasser}: an input slot at (48,53) that only takes items with
 * an activation recipe, and a take-only output slot at (112,53).
 *
 * <p>The previous version had a single slot at (48,45) restricted to fuel rods, matching the
 * invented "xenon scrubber" behaviour the block entity used to have.</p>
 */
public class RBMKOutgasserMenu extends AbstractContainerMenu {

    private final RBMKOutgasserBlockEntity blockEntity;

    public RBMKOutgasserMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, getBlockEntity(inv, buf));
    }

    public RBMKOutgasserMenu(int id, Inventory inv, RBMKOutgasserBlockEntity be) {
        super(ModMenuTypes.RBMK_OUTGASSER_MENU.get(), id);
        this.blockEntity = be;

        SimpleContainer container = new SimpleContainer(2) {
            @Override
            public void setChanged() {
                super.setChanged();
                be.inputSlot  = getItem(0).copy();
                be.outputSlot = getItem(1).copy();
                be.setChanged();
            }
        };
        container.setItem(0, be.inputSlot.copy());
        container.setItem(1, be.outputSlot.copy());

        addSlot(new Slot(container, RBMKOutgasserBlockEntity.SLOT_INPUT, 48, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return be.isItemValidForSlot(RBMKOutgasserBlockEntity.SLOT_INPUT, stack);
            }
        });

        // Take-only: the activation product must not be pushed back in by hand or by a hopper.
        addSlot(new Slot(container, RBMKOutgasserBlockEntity.SLOT_OUTPUT, 112, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18 + 20));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, 8 + col * 18, 142 + 20));
    }

    private static RBMKOutgasserBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof RBMKOutgasserBlockEntity o) return o;
        throw new IllegalStateException("No RBMKOutgasserBlockEntity at " + pos);
    }

    public RBMKOutgasserBlockEntity getBlockEntity() { return blockEntity; }

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
            if (index < 2) {
                if (!moveItemStackTo(stack, 2, slots.size(), true)) return ItemStack.EMPTY;
            } else {
                // Shift-click only ever feeds the input half.
                if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
