package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineMixerBlockEntity;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if forge {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
//?}

public class MachineMixerMenu extends AbstractContainerMenu {

    private static final int SLOT_BATTERY = 0;
    private static final int MACHINE_SLOTS = 1;

    private final MachineMixerBlockEntity blockEntity;
    private final ContainerData data;

    public MachineMixerMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineMixerMenu(int id, Inventory inventory, MachineMixerBlockEntity blockEntity) {
        this(id, inventory, blockEntity, new SimpleContainerData(6));
    }

    public MachineMixerMenu(int id, Inventory inventory, MachineMixerBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.MIXER_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.data = data;

        ModItemStackHandlerContainer machineInventory =
                new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        // Battery slot, positioned over the battery icon under input tank A.
        this.addSlot(new Slot(machineInventory, SLOT_BATTERY, 23, 95) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                boolean hbm = stack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER)
                        .map(provider -> provider.canExtract())
                        .orElse(false);
                if (hbm) return true;
                //? if forge {
                return stack.getCapability(ForgeCapabilities.ENERGY)
                        .map(storage -> storage.canExtract())
                        .orElse(false);
                //?} else {
                /*return false;
                *///?}
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 122 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 180));
        }

        addDataSlots(this.data);
    }

    public static MachineMixerMenu create(int id, Inventory inventory, MachineMixerBlockEntity blockEntity) {
        return new MachineMixerMenu(id, inventory, blockEntity, blockEntity.getContainerData());
    }

    private static MachineMixerBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineMixerBlockEntity mixerBlockEntity) {
            return mixerBlockEntity;
        }
        throw new IllegalStateException("No MachineMixerBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":mixer_menu");
    }

    public MachineMixerBlockEntity getBlockEntity() {
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
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack slotStack = slot.getItem();
        ItemStack copy = slotStack.copy();

        int playerInvStart = MACHINE_SLOTS;
        int playerInvEnd = this.slots.size();

        if (index < MACHINE_SLOTS) {
            if (!this.moveItemStackTo(slotStack, playerInvStart, playerInvEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean isBattery = slotStack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER)
                    .map(provider -> provider.canExtract())
                    .orElse(false);
            //? if forge {
            isBattery = isBattery || slotStack.getCapability(ForgeCapabilities.ENERGY)
                    .map(storage -> storage.canExtract())
                    .orElse(false);
            //?}

            if (isBattery) {
                if (!this.moveItemStackTo(slotStack, SLOT_BATTERY, SLOT_BATTERY + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (slotStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        slot.onTake(player, slotStack);
        return copy;
    }

    public int getProgress() {
        return data.get(0);
    }

    public int getMaxProgress() {
        return data.get(1);
    }

    public int getScaledProgress(int scale) {
        int progress = getProgress();
        int maxProgress = getMaxProgress();
        return maxProgress == 0 ? 0 : progress * scale / maxProgress;
    }

    public long getEnergyLong() {
        long lo = data.get(2) & 0xFFFFFFFFL;
        long hi = (long) data.get(3) << 32;
        return hi | lo;
    }

    public long getMaxEnergyLong() {
        long lo = data.get(4) & 0xFFFFFFFFL;
        long hi = (long) data.get(5) << 32;
        return hi | lo;
    }
}