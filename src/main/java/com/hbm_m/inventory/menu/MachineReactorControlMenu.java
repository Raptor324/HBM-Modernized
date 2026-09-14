package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachineReactorControlBlockEntity;
import com.hbm_m.blockentity.machines.MachineReactorControlBlockEntity.RodFunction;
import com.hbm_m.inventory.ModItemStackHandlerContainer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Menue des Reaktorsteuerpults. Ein Steckplatz fuer den Reaktorfuehler; die Koordinate stammt aus
 * {@code ContainerReactorControl} (1.7.10).
 */
public class MachineReactorControlMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = MachineReactorControlBlockEntity.INVENTORY_SIZE;

    private final MachineReactorControlBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    public MachineReactorControlMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, getBlockEntity(inv, extraData));
    }

    public MachineReactorControlMenu(int id, Inventory inv, MachineReactorControlBlockEntity blockEntity) {
        this(id, inv, blockEntity, blockEntity.getContainerData());
    }

    public MachineReactorControlMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.REACTOR_CONTROL_MENU.get(), id);
        this.blockEntity = (MachineReactorControlBlockEntity) entity;
        this.level = inv.player.level();
        this.data = data;

        checkContainerDataCount(data, 9);
        addDataSlots(data);

        ModItemStackHandlerContainer machineInventory =
                new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        // Koordinaten 1:1 aus ContainerReactorControl (1.7.10).
        this.addSlot(new Slot(machineInventory, MachineReactorControlBlockEntity.SLOT_SENSOR, 92, 38));

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inv, i, 8 + i * 18, 142));
        }
    }

    private static MachineReactorControlBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf data) {
        BlockEntity be = inv.player.level().getBlockEntity(data.readBlockPos());
        if (be instanceof MachineReactorControlBlockEntity control) {
            return control;
        }
        throw new IllegalStateException("BlockEntity is not a Reactor Control");
    }

    public MachineReactorControlBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public boolean isLinked()     { return data.get(0) != 0; }
    public int getHeat()          { return data.get(1); }
    public int getFlux()          { return data.get(2); }
    /** Stabstellung in Prozent. */
    public int getRodPercent()    { return data.get(3); }
    public int getLevelLower()    { return data.get(4); }
    public int getLevelUpper()    { return data.get(5); }
    public int getHeatLower()     { return data.get(6); }
    public int getHeatUpper()     { return data.get(7); }

    public RodFunction getFunction() {
        RodFunction[] values = RodFunction.values();
        int ordinal = data.get(8);
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : RodFunction.LINEAR;
    }

    /** Original: {@code getDisplayData()[2]} - die Waerme als Temperatur in Grad Celsius. */
    public int getTemperature() {
        return isLinked() ? (int) Math.round(getHeat() * 0.00002D * 980D + 20D) : 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player,
                ModBlocks.MACHINE_CONTROLLER.get());
    }
}
