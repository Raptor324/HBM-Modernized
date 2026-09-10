package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.dfc.DFCCoreBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1-Port von {@code ContainerCore} (1.7.10): drei Plaetze nebeneinander bei (62, 53), (80, 53)
 * und (98, 53) - aussen die beiden Katalysatoren, in der Mitte der AMS-Kern. Spielerinventar bei
 * (8, 84).
 */
public class DFCCoreMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = DFCCoreBlockEntity.INVENTORY_SIZE;

    private final DFCCoreBlockEntity blockEntity;
    private final ContainerData data;

    public DFCCoreMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, getBlockEntity(inv, extraData));
    }

    public DFCCoreMenu(int id, Inventory inv, DFCCoreBlockEntity blockEntity) {
        super(ModMenuTypes.DFC_CORE_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.data = blockEntity.getContainerData();

        checkContainerDataCount(data, 6);
        addDataSlots(data);

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        this.addSlot(new Slot(container, DFCCoreBlockEntity.SLOT_CATALYST_A, 62, 53));
        this.addSlot(new Slot(container, DFCCoreBlockEntity.SLOT_AMS_CORE, 80, 53));
        this.addSlot(new Slot(container, DFCCoreBlockEntity.SLOT_CATALYST_B, 98, 53));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
    }

    private static DFCCoreBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf data) {
        BlockPos pos = data.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof DFCCoreBlockEntity core) return core;
        throw new IllegalStateException("No DFCCoreBlockEntity at " + pos);
    }

    public DFCCoreBlockEntity getBlockEntity() { return blockEntity; }

    public int getField()       { return data.get(0); }
    public int getHeat()        { return data.get(1); }
    public int getColor()       { return data.get(2); }
    public int getConsumption() { return data.get(3); }

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
