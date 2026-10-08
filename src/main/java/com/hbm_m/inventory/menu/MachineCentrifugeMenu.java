package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachineCentrifugeBlockEntity;
import com.hbm_m.interfaces.ILongEnergyMenu;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.packet.PacketSyncEnergy;
import com.hbm_m.platform.DummyItemStackHandler;
import com.hbm_m.platform.ModItemStackHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MachineCentrifugeMenu extends AbstractContainerMenu implements ILongEnergyMenu {

    private final MachineCentrifugeBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;
    private final Player player;
    private final HandlerContainer machineInventory;

    private long clientEnergy;
    private long clientMaxEnergy;

    private static final int BATTERY_SLOT = 0;
    private static final int INPUT_SLOT = 1;
    private static final int OUTPUT_SLOT_START = 2;
    private static final int OUTPUT_SLOTS = 4;
    private static final int UPGRADE_SLOT_START = 6;
    private static final int MACHINE_SLOTS = 8;

    // Slot positions (Original ContainerCentrifuge)
    private static final int SLOT_BATTERY_X = 8;
    private static final int SLOT_BATTERY_Y = 57;
    private static final int SLOT_INPUT_X = 44;
    private static final int SLOT_INPUT_Y = 57;
    private static final int SLOT_OUTPUT_X0 = 70;
    private static final int SLOT_OUTPUT_Y = 57;
    private static final int SLOT_OUTPUT_X_STEP = 20;

    public MachineCentrifugeMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, extraData), new SimpleContainerData(2));
    }

    public MachineCentrifugeMenu(int containerId, Inventory playerInventory, MachineCentrifugeBlockEntity blockEntity) {
        // На клиенте тайл может отсутствовать (реплей Flashback) — подставляем пустые данные
        this(containerId, playerInventory, blockEntity, blockEntity != null ? blockEntity.getContainerData() : new SimpleContainerData(2));
    }

    public MachineCentrifugeMenu(int containerId, Inventory playerInventory, MachineCentrifugeBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.CENTRIFUGE_MENU.get(), containerId);

        checkContainerDataCount(data, 2);

        this.blockEntity = blockEntity;
        this.level = playerInventory.player.level();
        this.data = data;
        this.player = playerInventory.player;

        addDataSlots(data);

        // На клиенте тайл может отсутствовать (реплей Flashback) — подставляем пустую заглушку,
        // чтобы конструктор дошёл до конца и пакет открытия меню не уронил клиент
        ModItemStackHandler itemHandler = this.blockEntity != null
                ? this.blockEntity.getInventory()
                : new DummyItemStackHandler(MACHINE_SLOTS);
        this.machineInventory = new HandlerContainer(itemHandler);

        // Reihenfolge wie Original ContainerCentrifuge: Menue-Slot 0 = Eingang, 1 = Batterie (BE-Slots bleiben 1/0)
        // input
        this.addSlot(new Slot(machineInventory, INPUT_SLOT, SLOT_INPUT_X, SLOT_INPUT_Y));

        // battery
        this.addSlot(new Slot(machineInventory, BATTERY_SLOT, SLOT_BATTERY_X, SLOT_BATTERY_Y));

        // outputs (4 slots)
        int outputY = SLOT_OUTPUT_Y;
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            this.addSlot(new Slot(machineInventory, OUTPUT_SLOT_START + i, SLOT_OUTPUT_X0 + i * SLOT_OUTPUT_X_STEP, outputY) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }

        // Upgrades (Original: SlotUpgrade bei 156/31 und 156/49)
        for (int i = 0; i < 2; i++) {
            this.addSlot(new Slot(machineInventory, UPGRADE_SLOT_START + i, 156, 31 + i * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof com.hbm_m.item.industrial.ItemMachineUpgrade;
                }
            });
        }

        // Player inventory (Original: x=11, y=107)
        int playerInvX = 11;
        int playerInvY = 107;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        playerInvX + col * 18, playerInvY + row * 18));
            }
        }

        int hotbarY = 165;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col,
                    playerInvX + col * 18, hotbarY));
        }
    }

    private static MachineCentrifugeBlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf data) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(data.readBlockPos());
        if (blockEntity instanceof MachineCentrifugeBlockEntity centrifuge) {
            return centrifuge;
        }
        // На клиенте тайл может отсутствовать (реплей Flashback) — не крашим пакет, возвращаем null.
        // На сервере отсутствие тайла — реальный баг, поэтому там падаем как раньше.
        if (playerInventory.player.level().isClientSide) {
            return null;
        }
        throw new IllegalStateException("BlockEntity is not a Centrifuge");
    }

    public boolean isProcessing() {
        return data.get(0) > 0;
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

    @Override
    public void setEnergy(long energy, long maxEnergy, long delta) {
        this.clientEnergy = energy;
        this.clientMaxEnergy = maxEnergy;
    }

    @Override
    public long getEnergyStatic() {
        // тайл может отсутствовать на клиенте (реплей Flashback)
        return blockEntity != null ? blockEntity.getEnergyStored() : 0L;
    }

    @Override
    public long getMaxEnergyStatic() {
        return blockEntity != null ? blockEntity.getMaxEnergyStored() : 0L;
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
            ModPacketHandler.sendToPlayer((ServerPlayer) this.player, ModPacketHandler.SYNC_ENERGY,
                new PacketSyncEnergy(
                    this.containerId,
                    blockEntity.getEnergyStored(),
                    blockEntity.getMaxEnergyStored(),
                    0L
                ));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack slotStack = slot.getItem();
        ItemStack copy = slotStack.copy();

        // 1:1 ContainerCentrifuge.transferStackInSlot (Menue-Slots: 0 Eingang, 1 Batterie, 2-5 Ausgaenge, 6-7 Upgrades)
        if (index <= 7) {
            if (!this.moveItemStackTo(slotStack, 8, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }

            slot.onQuickCraft(slotStack, copy);
        } else {
            // Original: IBatteryItem || battery_creative; Port-Akkus laufen ueber die Energie-Capability (canPlaceItem)
            if (copy.getItem() instanceof com.hbm_m.api.item.IBatteryItem
                    || copy.getItem() == com.hbm_m.item.ModItems.CREATIVE_BATTERY.get()
                    || machineInventory.canPlaceItem(BATTERY_SLOT, copy)) {
                if (!this.moveItemStackTo(slotStack, 1, 2, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (copy.getItem() instanceof com.hbm_m.item.industrial.ItemMachineUpgrade) {
                if (!this.moveItemStackTo(slotStack, 6, 8, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(slotStack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (slotStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        // w16b: Original isUseableByPlayer (TileEntityMachineBase) = 128 vom Kern, dazu Huelle der Maschine (MultiblockMenuReach)
        return MultiblockMenuReach.stillValidCore(blockEntity, player, 128.0D);
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