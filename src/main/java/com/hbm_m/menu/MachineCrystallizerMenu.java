package com.hbm_m.menu;

import com.hbm_m.api.energy.ILongEnergyMenu;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.custom.machines.MachineCrystallizerBlockEntity;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.item.custom.fekal_electric.ItemCreativeBattery;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.packet.PacketSyncEnergy;

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
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.network.PacketDistributor;

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

        var handler = blockEntity.getInventory();

        // Original slot layout from ContainerCrystallizer
        this.addSlot(new SlotItemHandler(handler, SLOT_INPUT, 62, 45));
        this.addSlot(new SlotItemHandler(handler, SLOT_BATTERY, 152, 72) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getCapability(ForgeCapabilities.ENERGY).isPresent()
                    || stack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).isPresent()
                    || stack.getItem() instanceof ItemCreativeBattery;
            }
        });
        this.addSlot(new SlotItemHandler(handler, SLOT_OUTPUT, 113, 45) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new SlotItemHandler(handler, SLOT_FLUID_INPUT, 17, 18));
        this.addSlot(new SlotItemHandler(handler, SLOT_FLUID_OUTPUT, 17, 54) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new SlotItemHandler(handler, SLOT_UPGRADE_1, 80, 18));
        this.addSlot(new SlotItemHandler(handler, SLOT_UPGRADE_2, 98, 18));
        this.addSlot(new SlotItemHandler(handler, SLOT_FLUID_ID, 35, 72));

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
            ModPacketHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> (ServerPlayer) player),
                new PacketSyncEnergy(containerId, blockEntity.getEnergyStored(), blockEntity.getMaxEnergyStored(), 0L)
            );
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
            boolean isBattery = stack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).isPresent()
                || stack.getCapability(ForgeCapabilities.ENERGY).isPresent()
                || stack.getItem() instanceof ItemCreativeBattery;

            if (isBattery) {
                if (!moveItemStackTo(stack, SLOT_BATTERY, SLOT_BATTERY + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent()) {
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
}
