package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.inventory.menu.MachineBreederMenu;
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
import com.hbm_m.platform.ModFluidTank;
//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?} elif neoforge {
/*import net.neoforged.neoforge.common.capabilities.Capability;
import net.neoforged.neoforge.common.capabilities.NeoForgeCapabilities;
import net.neoforged.neoforge.common.util.LazyOptional;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?}


public class MachineBreederBlockEntity extends BaseMachineBlockEntity {

    private static final int SLOT_INPUT = 0;
    private static final int SLOT_BATTERY = 1;
    private static final int SLOT_OUTPUT = 2;
    private static final int SLOT_FLUID_INPUT = 3;
    private static final int SLOT_FLUID_OUTPUT = 4;
    private static final int SLOT_FLUID_ID = 7;

    private static final int SLOT_COUNT = 8;
    private static final long MAX_POWER = 1_000_000;
    private static final long MAX_RECEIVE = 1_000;
    private static final int TANK_CAPACITY = 8_000;
    private static final int DEFAULT_DURATION = 600;

    // ModFluidTank — кросс-лоадерная (forge/neoforge/fabric) обёртка над native FluidTank.
    // isFluidValid/onContentsChanged наследуются от native FluidTank (см. ModFluidTank).
    private final ModFluidTank tank = new ModFluidTank(TANK_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };

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
        public int getCount() {
            return 2;
        }
    };

    public MachineBreederBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BREEDER_BE.get(), pos, state, SLOT_COUNT, MAX_POWER, MAX_RECEIVE);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineBreederBlockEntity entity) {
        if (level.isClientSide) {
            return;
        }

        entity.ensureNetworkInitialized();
        entity.chargeFromBattery();
        entity.transferFluidsFromItems();

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

    private void transferFluidsFromItems() {
        ItemStack fillStack = inventory.getStackInSlot(SLOT_FLUID_INPUT);
        if (fillStack.isEmpty()) return;
        if (!inventory.getStackInSlot(SLOT_FLUID_OUTPUT).isEmpty()) return;

        var result = FluidUtil.tryEmptyContainer(fillStack, tank, TANK_CAPACITY, null, false);
        if (result.isSuccess()) {
            inventory.setStackInSlot(SLOT_FLUID_INPUT, ItemStack.EMPTY);
            inventory.setStackInSlot(SLOT_FLUID_OUTPUT, result.getResult());
            setChanged();
        }
    }

    private boolean canProcess() {
        if (inventory.getStackInSlot(SLOT_INPUT).isEmpty()) return false;
        if (getEnergyStored() < getPowerRequired()) return false;
        return false;
    }

    private void processItem() {
    }

    public int getPowerRequired() {
        return 1000;
    }

    public int getDuration() {
        return duration;
    }

    public long getPowerScaled(int scale) {
        long max = getMaxEnergyStored();
        return max <= 0 ? 0 : (getEnergyStored() * scale) / max;
    }

    public int getProgressScaled(int scale) {
        int dur = getDuration();
        return dur <= 0 ? 0 : (progress * scale) / dur;
    }

    public ModFluidTank getTank() {
        return tank;
    }

    public ContainerData getContainerData() {
        return data;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.breeder");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) {
            return com.hbm_m.api.energy.ItemEnergyAccess.getForgeEnergy(stack).isPresent()
                || com.hbm_m.api.energy.ItemEnergyAccess.getHbmProvider(stack).isPresent()
                || stack.getItem() instanceof ItemCreativeBattery;
        }
        if (slot == SLOT_OUTPUT || slot == SLOT_FLUID_OUTPUT) {
            return false;
        }
        if (slot == SLOT_FLUID_INPUT) {
            return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
        }
        if (slot == SLOT_FLUID_ID) {
            return true;
        }
        return true;
    }

    public boolean stillValid(Player player) {
        return !this.isRemoved() && player.distanceToSqr(this.getBlockPos().getCenter()) <= 64.0D;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineBreederMenu(containerId, playerInventory, this, data);
    }

    //? if < 1.21.1 {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("tank", tank.writeNBT(new CompoundTag()));
        tag.putInt("progress", progress);
        tag.putInt("duration", duration);
        tag.putBoolean("isOn", isOn);
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.saveAdditional(tag, registries);
        tag.put("tank", tank.writeNBT(registries, new CompoundTag()));
        tag.putInt("progress", progress);
        tag.putInt("duration", duration);
        tag.putBoolean("isOn", isOn);
    
    }
    *///?}

    //? if < 1.21.1 {
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("tank")) {
            tank.readNBT(tag.getCompound("tank"));
        }
        progress = tag.getInt("progress");
        duration = tag.contains("duration") ? tag.getInt("duration") : DEFAULT_DURATION;
        isOn = tag.getBoolean("isOn");
    }
    //?} else {
    /*@Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.loadAdditional(tag, registries);
        if (tag.contains("tank")) {
            tank.readNBT(registries, tag.getCompound("tank"));
        }
        progress = tag.getInt("progress");
        duration = tag.contains("duration") ? tag.getInt("duration") : DEFAULT_DURATION;
        isOn = tag.getBoolean("isOn");
    
    }
    *///?}

    @Override
    protected void setupFluidCapability() {
        //? if forge {
        // ModFluidTank extends native FluidTank (IFluidHandler) — отдаём напрямую.
        setFluidHandler(tank);
        //?} elif neoforge {
        /*setFluidHandler(tank);
        *///?}
    }
}
