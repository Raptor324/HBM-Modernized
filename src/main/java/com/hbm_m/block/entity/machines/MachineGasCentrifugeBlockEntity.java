package com.hbm_m.block.entity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.entity.BaseMachineBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.inventory.menu.MachineGasCentrifugeMenu;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

/**
 * Gas Centrifuge BlockEntity.
 * Simplified machine with battery slot, input/output slots, and energy.
 */
public class MachineGasCentrifugeBlockEntity extends BaseMachineBlockEntity {

    private static final int SLOT_COUNT = 5;
    private static final int SLOT_BATTERY = 0;
    private static final int SLOT_INPUT_1 = 1;
    private static final int SLOT_INPUT_2 = 2;
    private static final int SLOT_OUTPUT_1 = 3;
    private static final int SLOT_OUTPUT_2 = 4;

    private static final long MAX_POWER = 100_000;

    private boolean didProcess = false;

    private int progress = 0;
    private int maxProgress = 100;

    private float anim = 0.0F;
    private float prevAnim = 0.0F;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return 2;
        }
    };

    public MachineGasCentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GAS_CENTRIFUGE_BE.get(), pos, state, SLOT_COUNT, MAX_POWER, MAX_POWER);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineGasCentrifugeBlockEntity entity) {
        entity.prevAnim = entity.anim;

        if (level.isClientSide) {
            if (entity.didProcess) {
                entity.anim += 0.05F;
            }
            if (entity.anim > (float) (Math.PI * 2.0)) {
                entity.anim -= (float) (Math.PI * 2.0);
            }
            return;
        }

        entity.ensureNetworkInitialized();
        entity.chargeFromBattery();

        if (level.getGameTime() % 10L == 0L) {
            entity.updateEnergyDelta(entity.getEnergyStored());
        }

        boolean dirty = false;

        // TODO: Add recipe logic here
        if (entity.didProcess) {
            entity.didProcess = false;
            dirty = true;
        }

        if (dirty) {
            entity.setChanged();
            entity.sendUpdateToClient();
        }
    }

    private void chargeFromBattery() {
        ItemStack stack = inventory.getStackInSlot(SLOT_BATTERY);
        if (stack.isEmpty()) return;

        if (stack.getItem() instanceof ItemCreativeBattery) {
            setEnergyStored(getMaxEnergyStored());
            return;
        }

        stack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).ifPresent(provider -> {
            long needed = getMaxEnergyStored() - getEnergyStored();
            if (needed <= 0) return;
            long extracted = provider.extractEnergy(Math.min(needed, getReceiveSpeed()), false);
            if (extracted > 0) {
                setEnergyStored(getEnergyStored() + extracted);
                setChanged();
            }
        });

        if (!stack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).isPresent()) {
            stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(provider -> {
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

    public void drops() {
        if (level == null) return;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
            }
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.gas_centrifuge");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) {
            return stack.getCapability(ForgeCapabilities.ENERGY).isPresent()
                || stack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).isPresent();
        }
        if (slot == SLOT_OUTPUT_1 || slot == SLOT_OUTPUT_2) {
            return false;
        }
        return true;
    }

    @Override
    protected void setupFluidCapability() {
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineGasCentrifugeMenu(containerId, playerInventory, this, data);
    }

    public ContainerData getContainerData() {
        return data;
    }

    public boolean getDidProcess() {
        return didProcess;
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    public float getAnim(float partialTicks) {
        return prevAnim + (anim - prevAnim) * partialTicks;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("didProcess", didProcess);
        tag.putInt("progress", progress);
        tag.putInt("maxProgress", maxProgress);
        tag.putFloat("anim", anim);
        tag.putFloat("prevAnim", prevAnim);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        didProcess = tag.getBoolean("didProcess");
        progress = tag.getInt("progress");
        maxProgress = tag.getInt("maxProgress");
        if (maxProgress <= 0) maxProgress = 100;
        anim = tag.getFloat("anim");
        prevAnim = tag.getFloat("prevAnim");
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return super.getCapability(cap, side);
    }
}
