package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineKeyforgeBlockEntity;
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

/** Port of {@code ContainerMachineKeyForge} (1.7.10 Original). */
public class MachineKeyforgeMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = MachineKeyforgeBlockEntity.INVENTORY_SIZE;
    private static final int PLAYER_SLOT_START = MACHINE_SLOT_COUNT;

    private final MachineKeyforgeBlockEntity blockEntity;

    public MachineKeyforgeMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineKeyforgeMenu(int id, Inventory inventory, MachineKeyforgeBlockEntity blockEntity) {
        super(ModMenuTypes.MACHINE_KEYFORGE_MENU.get(), id);
        this.blockEntity = blockEntity;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);
        this.addSlot(new Slot(container, MachineKeyforgeBlockEntity.SLOT_SOURCE, 44, 36));
        this.addSlot(new Slot(container, MachineKeyforgeBlockEntity.SLOT_DUPE, 80, 36));
        this.addSlot(new Slot(container, MachineKeyforgeBlockEntity.SLOT_CUT, 116, 36));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 162));
        }
    }

    public static MachineKeyforgeMenu create(int id, Inventory inventory, MachineKeyforgeBlockEntity blockEntity) {
        return new MachineKeyforgeMenu(id, inventory, blockEntity);
    }

    private static MachineKeyforgeBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineKeyforgeBlockEntity keyforgeBlockEntity) {
            return keyforgeBlockEntity;
        }
        throw new IllegalStateException("No MachineKeyforgeBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":machine_keyforge_menu");
    }

    public MachineKeyforgeBlockEntity getBlockEntity() {
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
        if (slot == null || !slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index < MACHINE_SLOT_COUNT) {
            if (!this.moveItemStackTo(stack, PLAYER_SLOT_START, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!this.moveItemStackTo(stack, 0, MACHINE_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return result;
    }
}
