package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.machines.MachineCrystallizerBlockEntity;
import com.hbm_m.interfaces.ILongEnergyMenu;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.packet.PacketSyncEnergy;
import com.hbm_m.platform.ModItemStackHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MachineCrystallizerMenu extends AbstractContainerMenu implements ILongEnergyMenu {

    private static final int SLOT_INPUT = 0;
    private static final int SLOT_BATTERY = 1;
    private static final int SLOT_OUTPUT = 2;
    private static final int SLOT_FLUID_INPUT = 3;
    private static final int SLOT_FLUID_OUTPUT = 4;
    private static final int SLOT_UPGRADE_1 = 5;
    private static final int SLOT_UPGRADE_2 = 6;
    private static final int SLOT_FLUID_ID = 7;
    private static final int MACHINE_SLOTS = 8;

    private final MachineCrystallizerBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;
    private final Player player;
    private final HandlerContainer machineInventory;

    private long clientEnergy;
    private long clientMaxEnergy;

    public MachineCrystallizerMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, getBlockEntity(inv, extraData));
    }

    public MachineCrystallizerMenu(int id, Inventory inv, MachineCrystallizerBlockEntity blockEntity) {
        this(id, inv, blockEntity, blockEntity.getContainerData());
    }

    public MachineCrystallizerMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.CRYSTALLIZER_MENU.get(), id);
        this.blockEntity = (MachineCrystallizerBlockEntity) entity;
        this.level = inv.player.level();
        this.data = data;
        this.player = inv.player;

        checkContainerDataCount(data, 2);
        addDataSlots(data);

        ModItemStackHandler handler = blockEntity.getInventory();
        this.machineInventory = new HandlerContainer(handler);

        // Original slot layout from ContainerCrystallizer
        this.addSlot(new Slot(machineInventory, SLOT_INPUT, 62, 45));
        this.addSlot(new Slot(machineInventory, SLOT_BATTERY, 152, 72));
        this.addSlot(new Slot(machineInventory, SLOT_OUTPUT, 113, 45) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new Slot(machineInventory, SLOT_FLUID_INPUT, 17, 18));
        this.addSlot(new Slot(machineInventory, SLOT_FLUID_OUTPUT, 17, 54) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new Slot(machineInventory, SLOT_UPGRADE_1, 80, 18));
        this.addSlot(new Slot(machineInventory, SLOT_UPGRADE_2, 98, 18));
        this.addSlot(new Slot(machineInventory, SLOT_FLUID_ID, 35, 72));

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 122 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inv, i, 8 + i * 18, 180));
        }
    }

    private static MachineCrystallizerBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf data) {
        BlockEntity be = inv.player.level().getBlockEntity(data.readBlockPos());
        if (be instanceof MachineCrystallizerBlockEntity crystallizer) {
            return crystallizer;
        }
        throw new IllegalStateException("BlockEntity is not a Crystallizer");
    }

    public MachineCrystallizerBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int getProgress() {
        return data.get(0);
    }

    public int getMaxProgress() {
        return data.get(1);
    }

    public int getProgressScaled(int scale) {
        int max = getMaxProgress();
        return max == 0 ? 0 : getProgress() * scale / max;
    }

    @Override
    public void setEnergy(long energy, long maxEnergy, long delta) {
        this.clientEnergy = energy;
        this.clientMaxEnergy = maxEnergy;
    }

    @Override
    public long getEnergyStatic() {
        return blockEntity.getEnergyStored();
    }

    @Override
    public long getMaxEnergyStatic() {
        return blockEntity.getMaxEnergyStored();
    }

    @Override
    public long getEnergyDeltaStatic() {
        return 0;
    }

    public long getEnergyLong() {
        if (blockEntity != null && !level.isClientSide) {
            return blockEntity.getEnergyStored();
        }
        return clientEnergy;
    }

    public long getMaxEnergyLong() {
        if (blockEntity != null && !level.isClientSide) {
            return blockEntity.getMaxEnergyStored();
        }
        return clientMaxEnergy;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity != null && blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide) {
            ModPacketHandler.sendToPlayer((ServerPlayer) player, ModPacketHandler.SYNC_ENERGY,
                new PacketSyncEnergy(containerId, blockEntity.getEnergyStored(), blockEntity.getMaxEnergyStored(), 0L));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        int playerStart = MACHINE_SLOTS;
        int playerEnd = slots.size();

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Делегируем допустимость предметов слоту в BlockEntity (там loader-specific правила).
            if (machineInventory.canPlaceItem(SLOT_BATTERY, stack)) {
                if (!moveItemStackTo(stack, SLOT_BATTERY, SLOT_BATTERY + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (machineInventory.canPlaceItem(SLOT_FLUID_INPUT, stack)) {
                if (!moveItemStackTo(stack, SLOT_FLUID_INPUT, SLOT_FLUID_INPUT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!moveItemStackTo(stack, SLOT_UPGRADE_1, SLOT_UPGRADE_2 + 1, false)) {
                    if (!moveItemStackTo(stack, SLOT_FLUID_ID, SLOT_FLUID_ID + 1, false)) {
                        if (!moveItemStackTo(stack, SLOT_INPUT, SLOT_INPUT + 1, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, ModBlocks.CRYSTALLIZER.get());
    }

    /** Vanilla-адаптер для {@link ModItemStackHandler}, чтобы использовать обычные {@link Slot}. */
    private static final class HandlerContainer implements net.minecraft.world.Container {
        private final ModItemStackHandler handler;

        private HandlerContainer(ModItemStackHandler handler) {
            this.handler = handler;
        }

        @Override
        public int getContainerSize() {
            return handler.getSlots();
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < handler.getSlots(); i++) {
                if (!handler.getStackInSlot(i).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return handler.getStackInSlot(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack existing = handler.getStackInSlot(slot);
            if (existing.isEmpty() || amount <= 0) {
                return ItemStack.EMPTY;
            }

            ItemStack split = existing.split(amount);
            handler.setStackInSlot(slot, existing);
            setChanged();
            return split;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack existing = handler.getStackInSlot(slot);
            handler.setStackInSlot(slot, ItemStack.EMPTY);
            return existing;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            handler.setStackInSlot(slot, stack);
            setChanged();
        }

        @Override
        public void setChanged() {
            // Изменения уже отслеживаются в handler, но Slot ожидает этот вызов.
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < handler.getSlots(); i++) {
                handler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return handler.isItemValid(slot, stack);
        }
    }
}