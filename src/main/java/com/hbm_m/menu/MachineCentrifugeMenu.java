package com.hbm_m.menu;

import com.hbm_m.api.energy.ILongEnergyMenu;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.custom.machines.MachineCentrifugeBlockEntity;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.packet.PacketSyncEnergy;
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
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.network.PacketDistributor;

public class MachineCentrifugeMenu extends AbstractContainerMenu implements ILongEnergyMenu {

    private final MachineCentrifugeBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;
    private final Player player;

    private long clientEnergy;
    private long clientMaxEnergy;

    private static final int BATTERY_SLOT = 0;
    private static final int INPUT_SLOT = 1;
    private static final int OUTPUT_SLOT_START = 2;
    private static final int OUTPUT_SLOTS = 4;
    private static final int MACHINE_SLOTS = OUTPUT_SLOT_START + OUTPUT_SLOTS;

    // Slot positions (relative to GUI left/top)
    private static final int SLOT_BATTERY_X = 8;
    private static final int SLOT_BATTERY_Y = 54;
    private static final int SLOT_INPUT_X = 44;
    private static final int SLOT_INPUT_Y = 35;
    private static final int SLOT_OUTPUT_X0 = 65;
    private static final int SLOT_OUTPUT_Y = 54;
    private static final int SLOT_OUTPUT_X_STEP = 20;

    public MachineCentrifugeMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, extraData), new SimpleContainerData(2));
    }

    public MachineCentrifugeMenu(int containerId, Inventory playerInventory, MachineCentrifugeBlockEntity blockEntity) {
        this(containerId, playerInventory, blockEntity, blockEntity.getContainerData());
    }

    public MachineCentrifugeMenu(int containerId, Inventory playerInventory, MachineCentrifugeBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.CENTRIFUGE_MENU.get(), containerId);

        checkContainerDataCount(data, 2);

        this.blockEntity = blockEntity;
        this.level = playerInventory.player.level();
        this.data = data;
        this.player = playerInventory.player;

        addDataSlots(data);

        ItemStackHandler itemHandler = this.blockEntity.getInventory();

        // battery
        this.addSlot(new SlotItemHandler(itemHandler, BATTERY_SLOT, SLOT_BATTERY_X, SLOT_BATTERY_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                boolean hbm = stack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER)
                        .map(provider -> provider.canExtract())
                        .orElse(false);
                if (hbm) {
                    return true;
                }
                return stack.getCapability(ForgeCapabilities.ENERGY)
                        .map(storage -> storage.canExtract())
                        .orElse(false);
            }
        });

        // input
        this.addSlot(new SlotItemHandler(itemHandler, INPUT_SLOT, SLOT_INPUT_X, SLOT_INPUT_Y));

        // outputs (4 slots)
        int outputY = SLOT_OUTPUT_Y;
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            this.addSlot(new SlotItemHandler(itemHandler, OUTPUT_SLOT_START + i, SLOT_OUTPUT_X0 + i * SLOT_OUTPUT_X_STEP, outputY) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }

        // Player inventory (standard)
        int playerInvX = 8;
        int playerInvY = 84;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        playerInvX + col * 18, playerInvY + row * 18));
            }
        }

        int hotbarY = 142;
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
            ModPacketHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> (ServerPlayer) this.player),
                    new PacketSyncEnergy(
                            this.containerId,
                            blockEntity.getEnergyStored(),
                            blockEntity.getMaxEnergyStored(),
                            0L
                    )
            );
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

        int containerSlots = MACHINE_SLOTS;
        int playerInventoryStart = containerSlots;
        int playerInventoryEnd = this.slots.size();

        if (index < containerSlots) {
            if (!this.moveItemStackTo(slotStack, playerInventoryStart, playerInventoryEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean isBattery = slotStack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER)
                    .map(provider -> provider.canExtract())
                    .orElse(false) ||
                    slotStack.getCapability(ForgeCapabilities.ENERGY)
                            .map(storage -> storage.canExtract())
                            .orElse(false);

            if (isBattery) {
                if (!this.moveItemStackTo(slotStack, BATTERY_SLOT, BATTERY_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(slotStack, INPUT_SLOT, INPUT_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
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

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, ModBlocks.CENTRIFUGE.get());
    }
}
