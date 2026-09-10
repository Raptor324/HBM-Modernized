package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.icf.MachineICFPressBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1-Port von {@code ContainerICFPress} (1.7.10): leere Kapsel bei (98, 18), fertige bei (98, 54),
 * Myonenbehaelter bei (8, 18) und leer bei (8, 54), die beiden festen Brennstoffe bei (62, 54) und
 * (134, 54), die beiden Fluessigkeitskennungen darueber bei (62, 18) und (134, 18).
 */
public class MachineICFPressMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = MachineICFPressBlockEntity.INVENTORY_SIZE;

    private final MachineICFPressBlockEntity blockEntity;
    private final ContainerData data;

    public MachineICFPressMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, getBlockEntity(inv, extraData));
    }

    public MachineICFPressMenu(int id, Inventory inv, MachineICFPressBlockEntity blockEntity) {
        super(ModMenuTypes.MACHINE_ICF_PRESS_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.data = blockEntity.getContainerData();

        checkContainerDataCount(data, 3);
        addDataSlots(data);

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        this.addSlot(new Slot(container, MachineICFPressBlockEntity.SLOT_EMPTY_CAPSULE, 98, 18));
        this.addSlot(new TakeOnlySlot(container, MachineICFPressBlockEntity.SLOT_FILLED_CAPSULE, 98, 54));
        this.addSlot(new Slot(container, MachineICFPressBlockEntity.SLOT_MUON_FULL, 8, 18));
        this.addSlot(new TakeOnlySlot(container, MachineICFPressBlockEntity.SLOT_MUON_EMPTY, 8, 54));
        this.addSlot(new Slot(container, MachineICFPressBlockEntity.SLOT_SOLID_A, 62, 54));
        this.addSlot(new Slot(container, MachineICFPressBlockEntity.SLOT_SOLID_B, 134, 54));
        this.addSlot(new Slot(container, MachineICFPressBlockEntity.SLOT_FLUID_ID_A, 62, 18));
        this.addSlot(new Slot(container, MachineICFPressBlockEntity.SLOT_FLUID_ID_B, 134, 18));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 97 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 155));
        }
    }

    /** Ausgabeplatz: hineinlegen geht nicht. */
    private static class TakeOnlySlot extends Slot {
        TakeOnlySlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

    private static MachineICFPressBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf data) {
        BlockPos pos = data.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof MachineICFPressBlockEntity press) return press;
        throw new IllegalStateException("No MachineICFPressBlockEntity at " + pos);
    }

    public MachineICFPressBlockEntity getBlockEntity() { return blockEntity; }

    public int getMuon()  { return data.get(0); }
    public int getFillA() { return data.get(1); }
    public int getFillB() { return data.get(2); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null || blockEntity.getLevel() != player.level()) return false;
        BlockPos pos = blockEntity.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }
}
