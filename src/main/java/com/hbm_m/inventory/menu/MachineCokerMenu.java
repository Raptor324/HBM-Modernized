package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineCokerBlockEntity;
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

public class MachineCokerMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = MachineCokerBlockEntity.INVENTORY_SIZE;
    private static final int PLAYER_SLOT_START  = MACHINE_SLOT_COUNT;

    private final MachineCokerBlockEntity blockEntity;

    public MachineCokerMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineCokerMenu(int id, Inventory inventory, MachineCokerBlockEntity blockEntity) {
        super(ModMenuTypes.COKER_MENU.get(), id);
        this.blockEntity = blockEntity;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        this.addSlot(new Slot(container, MachineCokerBlockEntity.SLOT_FLUID_ID, 35, 72));
        this.addSlot(new Slot(container, MachineCokerBlockEntity.SLOT_OUTPUT, 97, 27) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 122 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 180));
        }
    }

    public static MachineCokerMenu create(int id, Inventory inventory, MachineCokerBlockEntity blockEntity) {
        return new MachineCokerMenu(id, inventory, blockEntity);
    }

    private static MachineCokerBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineCokerBlockEntity cokerBlockEntity) {
            return cokerBlockEntity;
        }
        throw new IllegalStateException("No MachineCokerBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":coker_menu");
    }

    public MachineCokerBlockEntity getBlockEntity() {
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
        } else if (stack.getItem() instanceof com.hbm_m.item.liquids.FluidIdentifierItem) {
            if (!this.moveItemStackTo(stack, MachineCokerBlockEntity.SLOT_FLUID_ID, MachineCokerBlockEntity.SLOT_FLUID_ID + 1, false)) {
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
