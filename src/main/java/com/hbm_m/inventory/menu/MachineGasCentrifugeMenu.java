package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachineGasCentrifugeBlockEntity;
import com.hbm_m.interfaces.ILongEnergyMenu;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.item.liquids.FluidIdentifierItem;
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
//? if forge {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
//?}

public class MachineGasCentrifugeMenu extends AbstractContainerMenu implements ILongEnergyMenu {

    private final MachineGasCentrifugeBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;
    private final Player player;
    private final ModItemStackHandlerContainer machineInventory;

    private long clientEnergy;
    private long clientMaxEnergy;

    private static final int OUTPUT_SLOT_0 = 0;
    private static final int OUTPUT_SLOT_3 = 3;
    private static final int BATTERY_SLOT = 4;
    private static final int FLUID_ID_SLOT = 5;
    private static final int UPGRADE_SLOT = 6;
    private static final int MACHINE_SLOTS = 7;

    public MachineGasCentrifugeMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, extraData), new SimpleContainerData(2));
    }

    public MachineGasCentrifugeMenu(int containerId, Inventory playerInventory, MachineGasCentrifugeBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.GAS_CENTRIFUGE_MENU.get(), containerId);

        checkContainerDataCount(data, 2);

        this.blockEntity = blockEntity;
        this.level = playerInventory.player.level();
        this.data = data;
        this.player = playerInventory.player;

        addDataSlots(data);

        this.machineInventory = new ModItemStackHandlerContainer(this.blockEntity.getInventory(), this.blockEntity::setChanged);

        // Enrichment product outputs (2x2 grid), auto-filled only.
        int[][] outputPos = { {71, 53}, {89, 53}, {71, 71}, {89, 71} };
        for (int i = 0; i < 4; i++) {
            int x = outputPos[i][0];
            int y = outputPos[i][1];
            this.addSlot(new Slot(machineInventory, OUTPUT_SLOT_0 + i, x, y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }

        // Battery slot (matches the original 1.7.10 ContainerMachineGasCent layout)
        this.addSlot(new Slot(machineInventory, BATTERY_SLOT, 182, 71) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                boolean hbm = stack.getCapability(com.hbm_m.capability.ModCapabilities.HBM_ENERGY_PROVIDER)
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

        // Fluid identifier slot (sets which gas the tank accepts)
        this.addSlot(new Slot(machineInventory, FLUID_ID_SLOT, 91, 15) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof FluidIdentifierItem;
            }
        });

        // Speed upgrade slot
        this.addSlot(new Slot(machineInventory, UPGRADE_SLOT, 69, 15) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ItemMachineUpgrade upgrade
                        && upgrade.getUpgradeType() == ItemMachineUpgrade.UpgradeType.SPEED;
            }
        });

        // Player inventory (matches the original 1.7.10 ContainerMachineGasCent layout)
        int playerInvX = 8;
        int playerInvY = 122;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        playerInvX + col * 18, playerInvY + row * 18));
            }
        }

        // Hotbar
        int hotbarY = 180;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col,
                    playerInvX + col * 18, hotbarY));
        }
    }

    private static MachineGasCentrifugeBlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(extraData.readBlockPos());
        if (blockEntity instanceof MachineGasCentrifugeBlockEntity gasCentrifuge) {
            return gasCentrifuge;
        }
        throw new IllegalStateException("BlockEntity is not a Gas Centrifuge");
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

    public MachineGasCentrifugeBlockEntity getBlockEntity() {
        return blockEntity;
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
        if (blockEntity != null && !level.isClientSide()) {
            return blockEntity.getEnergyStored();
        }
        return clientEnergy;
    }

    public long getMaxEnergyLong() {
        if (blockEntity != null && !level.isClientSide()) {
            return blockEntity.getMaxEnergyStored();
        }
        return clientMaxEnergy;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        if (blockEntity != null && blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide()) {
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

        int playerInventoryStart = MACHINE_SLOTS;
        int playerInventoryEnd = this.slots.size();

        if (index < MACHINE_SLOTS) {
            if (!this.moveItemStackTo(slotStack, playerInventoryStart, playerInventoryEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean isBattery = slotStack.getCapability(com.hbm_m.capability.ModCapabilities.HBM_ENERGY_PROVIDER)
                    .map(provider -> provider.canExtract())
                    .orElse(false);
            //? if forge {
            isBattery = isBattery || slotStack.getCapability(ForgeCapabilities.ENERGY)
                    .map(storage -> storage.canExtract())
                    .orElse(false);
            //?}

            if (isBattery) {
                if (!this.moveItemStackTo(slotStack, BATTERY_SLOT, BATTERY_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotStack.getItem() instanceof FluidIdentifierItem) {
                if (!this.moveItemStackTo(slotStack, FLUID_ID_SLOT, FLUID_ID_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotStack.getItem() instanceof ItemMachineUpgrade) {
                if (!this.moveItemStackTo(slotStack, UPGRADE_SLOT, UPGRADE_SLOT + 1, false)) {
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

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, ModBlocks.GAS_CENTRIFUGE.get());
    }
}
