package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineVacuumDistillBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Slot-Koordinaten 1:1 aus {@code ContainerMachineVacuumDistill} (1.7.10 Original) uebernommen,
 *  wo relevant - die Fluid-Ein/Ausgabe-Slots des Originals entfallen, weil das MK2-Rohrnetz die
 *  Fluidverbindung uebernimmt (siehe {@link MachineVacuumDistillBlockEntity}). */
public class MachineVacuumDistillMenu extends AbstractContainerMenu {

    private final MachineVacuumDistillBlockEntity blockEntity;
    private static final int SLOT_BATTERY = MachineVacuumDistillBlockEntity.SLOT_BATTERY;
    private static final int MACHINE_SLOT_COUNT = 1;
    private static final int PLAYER_INV_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INV_END = MACHINE_SLOT_COUNT + 36;

    public MachineVacuumDistillMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineVacuumDistillMenu(int id, Inventory inventory, MachineVacuumDistillBlockEntity blockEntity) {
        super(ModMenuTypes.VACUUM_DISTILL_MENU.get(), id);
        this.blockEntity = blockEntity;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);
        this.addSlot(new Slot(container, SLOT_BATTERY, 26, 90));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 156 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 214));
        }
    }

    public static MachineVacuumDistillMenu create(int id, Inventory inventory, MachineVacuumDistillBlockEntity blockEntity) {
        return new MachineVacuumDistillMenu(id, inventory, blockEntity);
    }

    private static MachineVacuumDistillBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineVacuumDistillBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("No MachineVacuumDistillBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":vacuum_distill_menu");
    }

    public MachineVacuumDistillBlockEntity getBlockEntity() {
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

            if (index < MACHINE_SLOT_COUNT) {
                if (!this.moveItemStackTo(slotStack, PLAYER_INV_START, PLAYER_INV_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(slotStack, SLOT_BATTERY, SLOT_BATTERY + 1, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (slotStack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, slotStack);
        }
        return result;
    }
}
