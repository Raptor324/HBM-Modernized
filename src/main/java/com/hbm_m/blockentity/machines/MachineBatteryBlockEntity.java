package com.hbm_m.blockentity.machines;

import com.hbm_m.api.energy.EnergyNetworkManager;
import com.hbm_m.block.machines.MachineBatteryBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IEnergyModeHolder;
import com.hbm_m.inventory.menu.MachineBatteryMenu;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Энергохранилище с настраиваемыми режимами работы.
 * Режимы: 0 = BOTH, 1 = INPUT, 2 = OUTPUT, 3 = DISABLED
 */
@SuppressWarnings("UnstableApiUsage")
public class MachineBatteryBlockEntity extends BaseMachineBlockEntity implements IEnergyModeHolder {

    private static final int SLOT_CHARGE = 0;
    private static final int SLOT_DISCHARGE = 1;
    private static final int SLOT_COUNT = 2;

    private static final long DEFAULT_CAPACITY_FALLBACK = 9_000_000_000_000_000_000L;
    private static final long TRANSFER_RATE = 100_000_000_000L;

    // Режимы работы (0 = BOTH, 1 = INPUT, 2 = OUTPUT, 3 = DISABLED)
    public int modeOnNoSignal = 0;
    public int modeOnSignal = 0;
    private Priority priority = Priority.LOW;

    private long lastEnergySample = 0;
    private long averagedEnergyDelta = 0;

    protected final ContainerData data;

    public MachineBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModBlockEntities.MACHINE_BATTERY_BE.get(),
                pos,
                state,
                SLOT_COUNT,
                getCapacityFromState(state),
                TRANSFER_RATE,
                TRANSFER_RATE
        );

        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> modeOnNoSignal;
                    case 1 -> modeOnSignal;
                    case 2 -> priority.ordinal();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> modeOnNoSignal = value;
                    case 1 -> modeOnSignal = value;
                    case 2 -> priority = Priority.values()[Math.max(0, Math.min(value, Priority.values().length - 1))];
                }
            }

            @Override
            public int getCount() {
                return 3;
            }
        };
    }

    private static long getCapacityFromState(BlockState state) {
        return state.getBlock() instanceof MachineBatteryBlock b ? b.getCapacity() : DEFAULT_CAPACITY_FALLBACK;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.machine_battery");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(this.getBlockState().getBlock().getDescriptionId());
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot != SLOT_CHARGE && slot != SLOT_DISCHARGE) return false;
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof ItemCreativeBattery) return true;
        //? if forge {
        return stack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY).isPresent()
                || stack.getCapability(com.hbm_m.capability.ModCapabilities.HBM_ENERGY_PROVIDER).isPresent()
                || stack.getCapability(com.hbm_m.capability.ModCapabilities.HBM_ENERGY_RECEIVER).isPresent();
        //?}
        //? if fabric {
        /*return team.reborn.energy.api.EnergyStorage.ITEM.find(stack, null) != null;
        *///?}
        //? if neoforge {
        /*// NeoForge: FE через Capabilities.EnergyStorage.ITEM + HBM через ItemEnergyAccess.
        return stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM) != null
                || com.hbm_m.api.energy.ItemEnergyAccess.getHbmProvider(stack).isPresent()
                || com.hbm_m.api.energy.ItemEnergyAccess.getHbmReceiver(stack).isPresent();
        *///?}
    }

    @Override
    public int getCurrentMode() {
        if (level == null) return modeOnNoSignal;
        return level.hasNeighborSignal(this.worldPosition) ? modeOnSignal : modeOnNoSignal;
    }

    public long getEnergyDeltaAveraged() {
        return this.averagedEnergyDelta;
    }

    @Override
    public Priority getPriority() {
        return this.priority;
    }

    public void setPriority(Priority p) {
        this.priority = p;
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineBatteryBlockEntity be) {
        if (level.isClientSide) return;

        be.ensureNetworkInitialized();

        long gameTime = level.getGameTime();
        if (gameTime % 10 == 0) {
            long cur = be.getEnergyStored();
            be.averagedEnergyDelta = (cur - be.lastEnergySample) / 10;
            be.lastEnergySample = cur;
        }

        int mode = be.getCurrentMode();
        if (mode == 0 || mode == 1) {
            be.chargeFromBatterySlot(SLOT_CHARGE);
        }
        if (mode == 0 || mode == 2) {
            be.chargeItemInSlot(SLOT_DISCHARGE);
        }
    }

    public void handleButtonPress(int buttonId) {
        switch (buttonId) {
            case 0 -> this.data.set(0, (this.modeOnNoSignal + 1) % 4);
            case 1 -> this.data.set(1, (this.modeOnSignal + 1) % 4);
            case 2 -> {
                Priority[] priorities = Priority.values();
                int currentIndex = this.priority.ordinal();
                int nextIndex = (currentIndex + 1) % priorities.length;
                this.data.set(2, nextIndex);
            }
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new MachineBatteryMenu(windowId, playerInventory, this, this.data);
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("modeOnNoSignal", this.modeOnNoSignal);
        tag.putInt("modeOnSignal", this.modeOnSignal);
        tag.putInt("priority", this.priority.ordinal());
        tag.putLong("lastEnergySample", this.lastEnergySample);
        tag.putLong("averagedEnergyDelta", this.averagedEnergyDelta);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        this.modeOnNoSignal = tag.getInt("modeOnNoSignal");
        this.modeOnSignal = tag.getInt("modeOnSignal");
        if (tag.contains("priority")) {
            int priorityIndex = tag.getInt("priority");
            this.priority = Priority.values()[Math.max(0, Math.min(priorityIndex, Priority.values().length - 1))];
        }
        this.lastEnergySample = tag.getLong("lastEnergySample");
        this.averagedEnergyDelta = tag.getLong("averagedEnergyDelta");
    }
}