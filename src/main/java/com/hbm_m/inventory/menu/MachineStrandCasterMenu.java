package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineStrandCasterBlockEntity;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 {@code ContainerMachineStrandCaster}: Formplatz (57,62), sechs Ausgaben 2x3 ab (125,26), Spielerinventar ab
 * (8,132). Shift-Klick setzt eine Form ein.
 */
public class MachineStrandCasterMenu extends AbstractContainerMenu {

    private final MachineStrandCasterBlockEntity caster;

    /** Sicht auf das Slot-Feld des Block-Entities. */
    private static final class CasterContainer implements Container {
        private final MachineStrandCasterBlockEntity be;
        CasterContainer(MachineStrandCasterBlockEntity be) { this.be = be; }
        @Override public int getContainerSize() { return be.slots.length; }
        @Override public boolean isEmpty() { for (ItemStack s : be.slots) if (!s.isEmpty()) return false; return true; }
        @Override public ItemStack getItem(int i) { return be.slots[i]; }
        @Override public ItemStack removeItem(int i, int n) {
            ItemStack s = be.slots[i];
            if (s.isEmpty()) return ItemStack.EMPTY;
            ItemStack out = s.split(n);
            if (s.isEmpty()) be.slots[i] = ItemStack.EMPTY;
            be.setChanged();
            return out;
        }
        @Override public ItemStack removeItemNoUpdate(int i) { ItemStack s = be.slots[i]; be.slots[i] = ItemStack.EMPTY; return s; }
        @Override public void setItem(int i, ItemStack stack) { be.slots[i] = stack; be.setChanged(); }
        @Override public void setChanged() { be.setChanged(); }
        @Override public boolean stillValid(Player player) { return true; }
        @Override public void clearContent() { for (int i = 0; i < be.slots.length; i++) be.slots[i] = ItemStack.EMPTY; }
        @Override public boolean canPlaceItem(int i, ItemStack stack) { return be.isItemValidForSlot(i, stack); }
    }

    public MachineStrandCasterMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineStrandCasterMenu(int id, Inventory invPlayer, MachineStrandCasterBlockEntity caster) {
        super(ModMenuTypes.STRAND_CASTER_MENU.get(), id);
        this.caster = caster;

        Container container = caster != null ? new CasterContainer(caster) : new SimpleContainer(7);

        this.addSlot(new Slot(container, 0, 57, 62) {
            @Override public int getMaxStackSize() { return 1; }
            @Override public boolean mayPlace(ItemStack stack) { return container.canPlaceItem(0, stack); }
        });

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 2; j++) {
                this.addSlot(new Slot(container, j + i * 2 + 1, 125 + j * 18, 26 + i * 18) {
                    @Override public boolean mayPlace(ItemStack stack) { return false; }
                });
            }
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 132 + i * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(invPlayer, i, 8 + i * 18, 190));
        }
    }

    private static MachineStrandCasterBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        if (be instanceof MachineStrandCasterBlockEntity caster) return caster;
        if (inventory.player.level().isClientSide) return null;
        throw new IllegalStateException("No MachineStrandCasterBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":strand_caster_menu");
    }

    public MachineStrandCasterBlockEntity getBlockEntity() {
        return caster;
    }

    @Override
    public boolean stillValid(Player player) {
        if (caster == null || caster.getLevel() != player.level()) return false;
        BlockPos pos = caster.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 128.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack originalStack = slot.getItem();
        ItemStack stack = originalStack.copy();

        if (index <= 6) {
            if (!this.moveItemStackTo(originalStack, 7, this.slots.size(), true)) return ItemStack.EMPTY;
            slot.onQuickCraft(originalStack, stack);
        } else if (!this.moveItemStackTo(originalStack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }

        if (originalStack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        return stack;
    }
}
