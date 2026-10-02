package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineAnnihilatorBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.DummyItemStackHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 {@code ContainerMachineAnnihilator}: Muell, Fluidkennung, sechs Auszahlungsslots, Monitor, Auszahlungsanfrage
 * und deren Ausgabe.
 */
public class MachineAnnihilatorMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = 11;

    private final MachineAnnihilatorBlockEntity blockEntity;

    public MachineAnnihilatorMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineAnnihilatorMenu(int id, Inventory inventory, MachineAnnihilatorBlockEntity blockEntity) {
        super(ModMenuTypes.ANNIHILATOR_MENU.get(), id);
        this.blockEntity = blockEntity;

        var container = new ModItemStackHandlerContainer(
                blockEntity != null ? blockEntity.getInventory() : new DummyItemStackHandler(MACHINE_SLOT_COUNT),
                blockEntity != null ? blockEntity::setChanged : null);

        // Muell
        this.addSlot(new Slot(container, 0, 17, 45));
        // Fluidkennung
        this.addSlot(new Slot(container, 1, 35, 45));
        // Ausgabe (addOutputSlots 2, 80, 36, 2 Reihen, 3 Spalten)
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new OutputSlot(container, 2 + row * 3 + col, 80 + col * 18, 36 + row * 18));
            }
        }
        // Monitor
        this.addSlot(new Slot(container, 8, 152, 18));
        // Auszahlungsanfrage
        this.addSlot(new Slot(container, 9, 152, 62));
        // Auszahlung
        this.addSlot(new OutputSlot(container, 10, 152, 80));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 126 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 126 + 58));
        }
    }

    private static final class OutputSlot extends Slot {
        OutputSlot(net.minecraft.world.Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
    }

    private static MachineAnnihilatorBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineAnnihilatorBlockEntity annihilator) {
            return annihilator;
        }
        if (inventory.player.level().isClientSide) return null;
        throw new IllegalStateException("No MachineAnnihilatorBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":annihilator_menu");
    }

    public MachineAnnihilatorBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null || blockEntity.getLevel() != player.level()) {
            return false;
        }
        BlockPos pos = blockEntity.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            if (index <= MACHINE_SLOT_COUNT - 1) {
                if (!this.moveItemStackTo(slotStack, MACHINE_SLOT_COUNT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (result.getItem() instanceof com.hbm_m.interfaces.IItemFluidIdentifier) {
                    if (!this.moveItemStackTo(slotStack, 1, 2, false)) return ItemStack.EMPTY;
                } else {
                    if (!this.moveItemStackTo(slotStack, 0, 1, false)) return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            slot.onTake(player, slotStack);
        }
        return result;
    }
}
