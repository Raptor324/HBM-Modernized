package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineLargePylonMenu;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class MachineLargePylonBlockEntity extends BaseMachineBlockEntity {

    private static final int SLOT_INPUT   = 0;
    private static final int SLOT_BATTERY = 1;
    private static final int SLOT_OUTPUT  = 2;

    private static final int  SLOT_COUNT   = 3;
    private static final long MAX_POWER    = 2_000_000;
    private static final long MAX_RECEIVE  = 4_000;
    private static final int  DEFAULT_DURATION = 400;

    private int progress = 0;
    private int duration = DEFAULT_DURATION;
    private boolean isOn = false;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> getDuration();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() { return 2; }
    };

    public MachineLargePylonBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LARGE_PYLON_BE.get(), pos, state, SLOT_COUNT, MAX_POWER, MAX_RECEIVE);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineLargePylonBlockEntity entity) {
        if (level.isClientSide) return;

        entity.ensureNetworkInitialized();
        entity.chargeFromBattery();

        entity.isOn = false;
        if (entity.canProcess()) {
            entity.progress++;
            entity.setEnergyStored(entity.getEnergyStored() - entity.getPowerRequired());
            entity.isOn = true;

            if (entity.progress >= entity.getDuration()) {
                entity.progress = 0;
                entity.processItem();
            }
            entity.setChanged();
            entity.sendUpdateToClient();
        } else {
            if (entity.progress != 0) {
                entity.progress = 0;
                entity.setChanged();
            }
        }
    }

    private void chargeFromBattery() {
        ItemStack stack = inventory.getStackInSlot(SLOT_BATTERY);
        if (stack.isEmpty()) return;

        if (stack.getItem() instanceof ItemCreativeBattery) {
            setEnergyStored(getMaxEnergyStored());
            return;
        }

        com.hbm_m.api.energy.ItemEnergyAccess.getHbmProvider(stack).ifPresent(provider -> {
            long needed = getMaxEnergyStored() - getEnergyStored();
            if (needed <= 0) return;
            long extracted = provider.extractEnergy(Math.min(needed, getReceiveSpeed()), false);
            if (extracted > 0) {
                setEnergyStored(getEnergyStored() + extracted);
                setChanged();
            }
        });

        if (!com.hbm_m.api.energy.ItemEnergyAccess.getHbmProvider(stack).isPresent()) {
            com.hbm_m.api.energy.ItemEnergyAccess.getForgeEnergy(stack).ifPresent(provider -> {
                long needed = getMaxEnergyStored() - getEnergyStored();
                if (needed <= 0) return;
                int extracted = provider.extractEnergy((int) Math.min(needed, getReceiveSpeed()), false);
                if (extracted > 0) {
                    setEnergyStored(getEnergyStored() + extracted);
                    setChanged();
                }
            });
        }
    }

    private boolean canProcess() {
        if (inventory.getStackInSlot(SLOT_INPUT).isEmpty()) return false;
        if (getEnergyStored() < getPowerRequired()) return false;
        return false; // WIP – рецепты не реализованы
    }

    private void processItem() {
        // WIP
    }

    public int getPowerRequired() { return 2000; }
    public int getDuration()      { return duration; }

    public long getPowerScaled(int scale) {
        long max = getMaxEnergyStored();
        return max <= 0 ? 0 : (getEnergyStored() * scale) / max;
    }

    public int getProgressScaled(int scale) {
        int dur = getDuration();
        return dur <= 0 ? 0 : (progress * scale) / dur;
    }

    public ContainerData getContainerData() { return data; }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.large_pylon");
    }

    @Override
    public Component getDisplayName() { return getDefaultName(); }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) {
            return com.hbm_m.api.energy.ItemEnergyAccess.getForgeEnergy(stack).isPresent()
                || com.hbm_m.api.energy.ItemEnergyAccess.getHbmProvider(stack).isPresent()
                || stack.getItem() instanceof ItemCreativeBattery;
        }
        if (slot == SLOT_OUTPUT) return false;
        return true;
    }

    public boolean stillValid(Player player) {
        return !this.isRemoved() && player.distanceToSqr(this.getBlockPos().getCenter()) <= 64.0D;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineLargePylonMenu(containerId, playerInventory, this, data);
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("progress", progress);
        tag.putInt("duration", duration);
        tag.putBoolean("isOn", isOn);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        progress = tag.getInt("progress");
        duration = tag.contains("duration") ? tag.getInt("duration") : DEFAULT_DURATION;
        isOn = tag.getBoolean("isOn");
    }
}