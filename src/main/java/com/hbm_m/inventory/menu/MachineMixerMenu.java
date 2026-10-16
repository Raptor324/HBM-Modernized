package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineMixerBlockEntity;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.DummyItemStackHandler;

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

        // На клиенте тайл может отсутствовать (реплей Flashback) — подставляем пустую заглушку,
        // чтобы конструктор дошёл до конца и пакет открытия меню не уронил клиент
        ModItemStackHandlerContainer machineInventory =
                new ModItemStackHandlerContainer(
                        blockEntity != null ? blockEntity.getInventory() : new DummyItemStackHandler(MACHINE_SLOTS),
                        blockEntity != null ? blockEntity::setChanged : null);

        // Battery slot, positioned over the battery icon under input tank A.
        this.addSlot(new Slot(machineInventory, SLOT_BATTERY, 23, 95) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                boolean hbm = com.hbm_m.api.energy.ItemEnergyAccess.getHbmProvider(stack)
                        .map(provider -> provider.canExtract())
                        .orElse(false);
                if (hbm) return true;
                //? if forge {
                return com.hbm_m.api.energy.ItemEnergyAccess.getForgeEnergy(stack)
                        .map(storage -> storage.canExtract())
                        .orElse(false);
                //?} elif neoforge {
                /*return stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM) != null;
                *///?} else {
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
        // На клиенте тайл может отсутствовать (реплей Flashback) — не крашим пакет, возвращаем null.
        // На сервере отсутствие тайла — реальный баг, поэтому там падаем как раньше.
        if (inventory.player.level().isClientSide) {
            return null;
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
            boolean isBattery = com.hbm_m.api.energy.ItemEnergyAccess.getHbmProvider(slotStack)
                    .map(provider -> provider.canExtract())
                    .orElse(false);
            //? if forge {
            isBattery = isBattery || com.hbm_m.api.energy.ItemEnergyAccess.getForgeEnergy(slotStack)
                    .map(storage -> storage.canExtract())
                    .orElse(false);
            //?} elif neoforge {
            /*isBattery = isBattery || slotStack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM) != null;
            *///?}

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