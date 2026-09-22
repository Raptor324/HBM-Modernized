package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineOilburnerBlockEntity;
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

/**
 * Порт {@code ContainerOilburner} (1.7.10 Original): 3 слота машины —
 * In (26,17), Out = SlotTakeOnly (26,53), Fluid-ID (44,71); инвентарь игрока
 * (8,121) / хотбар (8,179); shift-клик стака игрока → слот 2, если это
 * fluid identifier, иначе слот 0.
 */
public class MachineOilburnerMenu extends AbstractContainerMenu {

    public static final int SLOT_IN = MachineOilburnerBlockEntity.SLOT_IN;
    public static final int SLOT_OUT = MachineOilburnerBlockEntity.SLOT_OUT;
    public static final int SLOT_FLUID_ID = MachineOilburnerBlockEntity.SLOT_FLUID_ID;
    public static final int MACHINE_SLOT_COUNT = MachineOilburnerBlockEntity.INVENTORY_SIZE;

    private final MachineOilburnerBlockEntity blockEntity;

    public MachineOilburnerMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineOilburnerMenu(int id, Inventory inventory, MachineOilburnerBlockEntity blockEntity) {
        super(ModMenuTypes.OILBURNER_MENU.get(), id);
        this.blockEntity = blockEntity;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        //In
        this.addSlot(new Slot(container, SLOT_IN, 26, 17));
        //Out — SlotTakeOnly: класть нельзя, забирать можно
        this.addSlot(new SlotTakeOnly(container, SLOT_OUT, 26, 53));
        //Fluid ID
        this.addSlot(new Slot(container, SLOT_FLUID_ID, 44, 71));

        // Player inventory, ported 1:1 from ContainerOilburner (offset = 37).
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 121 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 179));
        }
    }

    public static MachineOilburnerMenu create(int id, Inventory inventory, MachineOilburnerBlockEntity blockEntity) {
        return new MachineOilburnerMenu(id, inventory, blockEntity);
    }

    private static MachineOilburnerBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineOilburnerBlockEntity oilburnerBlockEntity) {
            return oilburnerBlockEntity;
        }
        throw new MenuBlockEntityMissingException("No MachineOilburnerBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":oilburner_menu");
    }

    public MachineOilburnerBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        return MenuReach.stillValid(player, blockEntity);
    }

    /**
     * Порт {@code ContainerOilburner.transferStackInSlot}: слоты машины → инвентарь
     * игрока; стаки игрока → слот 2 (Fluid-ID), если предмет — fluid identifier,
     * иначе слот 0 (In).
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            if (index < MACHINE_SLOT_COUNT) {
                if (!this.moveItemStackTo(slotStack, MACHINE_SLOT_COUNT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (slotStack.getItem() instanceof com.hbm_m.interfaces.IItemFluidIdentifier) {
                    if (!this.moveItemStackTo(slotStack, SLOT_FLUID_ID, SLOT_FLUID_ID + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!this.moveItemStackTo(slotStack, SLOT_IN, SLOT_IN + 1, false)) {
                        return ItemStack.EMPTY;
                    }
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

    /** Порт {@code SlotTakeOnly}: mayPlace = false, извлечение разрешено. */
    private static class SlotTakeOnly extends Slot {
        public SlotTakeOnly(net.minecraft.world.Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
