package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineAshpitBlockEntity;
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

public class MachineAshpitMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = MachineAshpitBlockEntity.INVENTORY_SIZE;
    private static final int PLAYER_SLOT_START  = MACHINE_SLOT_COUNT;

    private final MachineAshpitBlockEntity blockEntity;

    public MachineAshpitMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineAshpitMenu(int id, Inventory inventory, MachineAshpitBlockEntity blockEntity) {
        super(ModMenuTypes.ASHPIT_MENU.get(), id);
        this.blockEntity = blockEntity;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        for (int i = 0; i < MachineAshpitBlockEntity.INVENTORY_SIZE; i++) {
            final int slotIndex = i;
            this.addSlot(new Slot(container, slotIndex, 44 + i * 18, 27) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 86 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 144));
        }
    }

    public static MachineAshpitMenu create(int id, Inventory inventory, MachineAshpitBlockEntity blockEntity) {
        return new MachineAshpitMenu(id, inventory, blockEntity);
    }

    private static MachineAshpitBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineAshpitBlockEntity ashpitBlockEntity) {
            return ashpitBlockEntity;
        }
        throw new IllegalStateException("No MachineAshpitBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":ashpit_menu");
    }

    public MachineAshpitBlockEntity getBlockEntity() {
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
            return ItemStack.EMPTY;
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
