package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.ForceFieldBlockEntity;
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
 * Menue des Kraftfeldgenerators. Slotkoordinaten 1:1 aus {@code ContainerForceField} (1.7.10):
 * Batterie bei (26, 53), die beiden Aufwertungen bei (89, 35) und (107, 35).
 */
public class ForceFieldMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = ForceFieldBlockEntity.INVENTORY_SIZE;

    private final ForceFieldBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    public ForceFieldMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, getBlockEntity(inv, extraData));
    }

    public ForceFieldMenu(int id, Inventory inv, ForceFieldBlockEntity blockEntity) {
        super(ModMenuTypes.FORCE_FIELD_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.level = inv.player.level();
        this.data = blockEntity.getContainerData();

        checkContainerDataCount(data, 6);
        addDataSlots(data);

        ModItemStackHandlerContainer container =
                new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        this.addSlot(new Slot(container, ForceFieldBlockEntity.SLOT_BATTERY, 26, 53));
        this.addSlot(new Slot(container, ForceFieldBlockEntity.SLOT_UPGRADE_RADIUS, 89, 35));
        this.addSlot(new Slot(container, ForceFieldBlockEntity.SLOT_UPGRADE_HEALTH, 107, 35));

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inv, i, 8 + i * 18, 142));
        }
    }

    private static ForceFieldBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf data) {
        BlockEntity be = inv.player.level().getBlockEntity(data.readBlockPos());
        if (be instanceof ForceFieldBlockEntity field) {
            return field;
        }
        throw new IllegalStateException("BlockEntity is not a Force Field");
    }

    public ForceFieldBlockEntity getBlockEntity() { return blockEntity; }

    public int getHealth()    { return data.get(0); }
    public int getMaxHealth() { return data.get(1); }
    public int getRadius()    { return data.get(2); }
    public int getCooldown()  { return data.get(3); }
    public int getPowerCons() { return data.get(4); }
    public boolean isOn()     { return data.get(5) != 0; }

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
                ModBlocks.MACHINE_FORCEFIELD.get());
    }
}
