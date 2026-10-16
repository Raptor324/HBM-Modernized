package com.hbm_m.inventory.menu.radio;

import com.hbm_m.blockentity.network.radio.RadioTorchCounterBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.inventory.menu.ModMenuTypes;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class RadioTorchCounterMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = RadioTorchCounterBlockEntity.SLOT_COUNT;
    private static final int PLAYER_SLOT_START = MACHINE_SLOT_COUNT;

    private final RadioTorchCounterBlockEntity blockEntity;

    public RadioTorchCounterMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public RadioTorchCounterMenu(int id, Inventory inventory, RadioTorchCounterBlockEntity blockEntity) {
        super(ModMenuTypes.RADIO_TORCH_COUNTER_MENU.get(), id);
        this.blockEntity = blockEntity;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        for (int i = 0; i < MACHINE_SLOT_COUNT; i++) {
            final int idx = i;
            this.addSlot(new Slot(container, i, 44 + i * 30, 20) {
                @Override
                public void set(ItemStack stack) {
                    super.set(stack);
                    blockEntity.getMatcher().initPattern(idx, stack);
                }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 103 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 161));
        }
    }

    public static RadioTorchCounterMenu create(int id, Inventory inventory, RadioTorchCounterBlockEntity blockEntity) {
        return new RadioTorchCounterMenu(id, inventory, blockEntity);
    }

    private static RadioTorchCounterBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof RadioTorchCounterBlockEntity counterBlockEntity) {
            return counterBlockEntity;
        }
        throw new IllegalStateException("No RadioTorchCounterBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":radio_torch_counter_menu");
    }

    public RadioTorchCounterBlockEntity getBlockEntity() {
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
