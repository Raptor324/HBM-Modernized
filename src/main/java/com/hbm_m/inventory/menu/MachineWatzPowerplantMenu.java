package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineWatzPowerplantBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.item.nuclear.WatzPelletItem;
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
 * 24 pellet slots laid out in the same diamond/octagon shape as the original
 * {@code ContainerWatz} (i, j in [0,6) with the corners clipped), reflecting the reactor's
 * cross-shaped footprint even though the multiblock casing itself is a simplified 5x5x3 box
 * (see class doc on {@code MachineWatzPowerplantBlock}).
 */
public class MachineWatzPowerplantMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = MachineWatzPowerplantBlockEntity.PELLET_SLOTS;
    private static final int PLAYER_SLOT_START = MACHINE_SLOT_COUNT;

    private final MachineWatzPowerplantBlockEntity blockEntity;
    private final ModItemStackHandlerContainer machineContainer;

    public MachineWatzPowerplantMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineWatzPowerplantMenu(int id, Inventory inventory, MachineWatzPowerplantBlockEntity blockEntity) {
        super(ModMenuTypes.WATZ_POWERPLANT_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.machineContainer = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        int index = 0;
        for (int j = 0; j < 6; j++) {
            for (int i = 0; i < 6; i++) {
                if (i + j > 1 && i + j < 9 && 5 - i + j > 1 && i + 5 - j > 1) {
                    this.addSlot(new Slot(machineContainer, index, 17 + i * 18, 8 + j * 18));
                    index++;
                }
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 147 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 205));
        }
    }

    public static MachineWatzPowerplantMenu create(int id, Inventory inventory, MachineWatzPowerplantBlockEntity blockEntity) {
        return new MachineWatzPowerplantMenu(id, inventory, blockEntity);
    }

    private static MachineWatzPowerplantBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineWatzPowerplantBlockEntity watz) {
            return watz;
        }
        throw new IllegalStateException("No MachineWatzPowerplantBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":watz_powerplant_menu");
    }

    public MachineWatzPowerplantBlockEntity getBlockEntity() {
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
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index < MACHINE_SLOT_COUNT) {
                if (!this.moveItemStackTo(stack, PLAYER_SLOT_START, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.getItem() instanceof WatzPelletItem) {
                if (!this.moveItemStackTo(stack, 0, MACHINE_SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            slot.onTake(player, stack);
        }

        return result;
    }
}
