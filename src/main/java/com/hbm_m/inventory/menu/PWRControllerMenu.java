package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.PWRControllerBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.item.nuclear.PWRFuelItem;
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
 * Two machine slots (fresh fuel in / hot fuel out), matching the original {@code ContainerPWR}'s
 * layout intent (see {@code PWRControllerBlockEntity} for the scope note on why this is a
 * single-block reactor instead of a flood-fill multiblock).
 */
public class PWRControllerMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = 2;
    private static final int PLAYER_SLOT_START = MACHINE_SLOT_COUNT;

    private final PWRControllerBlockEntity blockEntity;
    private final ModItemStackHandlerContainer machineContainer;

    public PWRControllerMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public PWRControllerMenu(int id, Inventory inventory, PWRControllerBlockEntity blockEntity) {
        super(ModMenuTypes.PWR_CONTROLLER_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.machineContainer = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        this.addSlot(new Slot(machineContainer, PWRControllerBlockEntity.SLOT_FUEL_IN, 44, 62));
        this.addSlot(new Slot(machineContainer, PWRControllerBlockEntity.SLOT_FUEL_OUT, 116, 62) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 147 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 205));
        }
    }

    public static PWRControllerMenu create(int id, Inventory inventory, PWRControllerBlockEntity blockEntity) {
        return new PWRControllerMenu(id, inventory, blockEntity);
    }

    private static PWRControllerBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof PWRControllerBlockEntity pwr) {
            return pwr;
        }
        throw new IllegalStateException("No PWRControllerBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":pwr_controller_menu");
    }

    public PWRControllerBlockEntity getBlockEntity() {
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
            } else if (stack.getItem() instanceof PWRFuelItem) {
                if (!this.moveItemStackTo(stack, PWRControllerBlockEntity.SLOT_FUEL_IN, PWRControllerBlockEntity.SLOT_FUEL_IN + 1, false)) {
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
