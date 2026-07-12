package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineSteamTurbineMenu;
import com.hbm_m.interfaces.IEnergyModeHolder;
import com.hbm_m.interfaces.IItemFluidIdentifier;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.item.liquids.FluidIdentifierItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}

public class MachineSteamTurbineBlockEntity extends BaseMachineBlockEntity implements IEnergyModeHolder {

    public static final int SLOT_FLUID_ID_IN = 0;
    public static final int SLOT_FLUID_ID_OUT = 1;
    public static final int SLOT_INPUT_IO_IN = 2;
    public static final int SLOT_INPUT_IO_OUT = 3;
    public static final int SLOT_BATTERY = 4;
    public static final int SLOT_OUTPUT_IO_IN = 5;
    public static final int SLOT_OUTPUT_IO_OUT = 6;
    public static final int INVENTORY_SIZE = 7;

    private static final int STEAM_CONSUMPTION_RATE = 8;
    private static final int TANK_CAPACITY = 24_000;
    private static final long ENERGY_PER_MB_STEAM = 80L;
    private static final long ENERGY_PER_MB_HOTSTEAM = 160L;
    private static final long ENERGY_PER_MB_SUPERHOTSTEAM = 320L;
    private static final long ENERGY_PER_MB_ULTRAHOTSTEAM = 640L;

    private final FluidTank[] tanks = new FluidTank[] {
            new FluidTank(TANK_CAPACITY),
            new FluidTank(ModFluids.SPENTSTEAM.getSource(), TANK_CAPACITY)
    };

    private int progress = 0;
    private static final int MAX_PROGRESS = 200;
    private boolean active = false;

    //? if forge {
    private LazyOptional<IFluidHandler> fluidHandler = LazyOptional.empty();
    //?}

    public MachineSteamTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEAM_TURBINE_BE.get(), pos, state, INVENTORY_SIZE, 1_000_000L, 0L, 50_000L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineSteamTurbineBlockEntity be) {
        if (level.isClientSide()) return;
        be.ensureNetworkInitialized();
        boolean wasActive = be.active;

        be.active = be.processSteam();
        if (be.active) {
            be.progress = (be.progress + 1) % MAX_PROGRESS;
        } else {
            be.progress = 0;
        }

        if (be.energy > 0L && level.getGameTime() % 10L == 0L) {
            be.updateEnergyDelta(be.getEnergyStored());
        }

        if (wasActive != be.active || be.active) {
            be.setChanged();
            be.sendUpdateToClient();
        }
    }

    private boolean processSteam() {
        Fluid input = tanks[0].getTankType();
        long energyPerMb = getEnergyPerMb(input);

        if (energyPerMb <= 0 || tanks[0].getFill() <= 0) {
            if (tanks[0].getFill() > 0) {
                tanks[1].setTankType(ModFluids.NONE.getSource());
            } else if (tanks[1].getFill() <= 0) {
                tanks[1].setTankType(ModFluids.SPENTSTEAM.getSource());
            }
            return false;
        }

        if (tanks[1].getFill() > 0 && !VanillaFluidEquivalence.sameSubstance(tanks[1].getTankType(), ModFluids.SPENTSTEAM.getSource())) {
            return false;
        }

        int maxByInput = Math.min(STEAM_CONSUMPTION_RATE, tanks[0].getFill());
        int maxByOutput = tanks[1].getMaxFill() - tanks[1].getFill();
        long energySpace = getMaxEnergyStored() - getEnergyStored();
        int maxByEnergy = (int) Math.min(Integer.MAX_VALUE, energySpace / energyPerMb);

        int ops = Math.min(maxByInput, Math.min(maxByOutput, maxByEnergy));
        if (ops <= 0) return false;

        tanks[0].drainMb(ops);
        tanks[1].fillMb(ModFluids.SPENTSTEAM.getSource(), ops);
        setEnergyStored(getEnergyStored() + ops * energyPerMb);
        return true;
    }

    private long getEnergyPerMb(Fluid steam) {
        if (VanillaFluidEquivalence.sameSubstance(steam, ModFluids.STEAM.getSource())) return ENERGY_PER_MB_STEAM;
        if (VanillaFluidEquivalence.sameSubstance(steam, ModFluids.HOTSTEAM.getSource())) return ENERGY_PER_MB_HOTSTEAM;
        if (VanillaFluidEquivalence.sameSubstance(steam, ModFluids.SUPERHOTSTEAM.getSource())) return ENERGY_PER_MB_SUPERHOTSTEAM;
        if (VanillaFluidEquivalence.sameSubstance(steam, ModFluids.ULTRAHOTSTEAM.getSource())) return ENERGY_PER_MB_ULTRAHOTSTEAM;
        return 0L;
    }

    public FluidTank[] getTanks() {
        return tanks;
    }

    public int getPowerScaled(int scale) {
        long max = Math.max(getMaxEnergyStored(), 1L);
        return (int) Math.min(scale, getEnergyStored() * scale / max);
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return MAX_PROGRESS;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public int getCurrentMode() {
        return 2; // OUTPUT only, so the energy network treats this as a generator.
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("progress", progress);
        tag.putBoolean("active", active);
        tanks[0].writeToNBT(tag, "input");
        tanks[1].writeToNBT(tag, "output");
    }

    @Override
    public void load(net.minecraft.nbt.CompoundTag tag) {
        super.load(tag);
        progress = tag.getInt("progress");
        active = tag.getBoolean("active");
        tanks[0].readFromNBT(tag, "input");
        tanks[1].readFromNBT(tag, "output");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.steam_turbine");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return MachineSteamTurbineMenu.create(id, inv, this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.steam_turbine");
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_FLUID_ID_IN -> stack.getItem() instanceof IItemFluidIdentifier || stack.getItem() instanceof FluidIdentifierItem;
            case SLOT_FLUID_ID_OUT, SLOT_INPUT_IO_OUT, SLOT_OUTPUT_IO_OUT -> false;
            case SLOT_INPUT_IO_IN, SLOT_OUTPUT_IO_IN -> {
                //? if forge {
                yield stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
                //?}
                //? if fabric {
                /*yield net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.ITEM.find(stack, null) != null;
                *///?}
            }
            case SLOT_BATTERY -> stack.getItem() instanceof ItemCreativeBattery || isEnergyProviderItem(stack) || isEnergyReceiverItem(stack);
            default -> false;
        };
    }

    //? if forge {
    @Override
    protected void setupFluidCapability() {
        fluidHandler = LazyOptional.of(() -> new UnifiedFluidHandler(this));
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return fluidHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidHandler.invalidate();
    }

    private static class UnifiedFluidHandler implements IFluidHandler {
        private final MachineSteamTurbineBlockEntity be;

        UnifiedFluidHandler(MachineSteamTurbineBlockEntity be) {
            this.be = be;
        }

        @Override
        public int getTanks() { return 2; }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack getFluidInTank(int tank) {
            if (tank == 0) {
                return new net.minecraftforge.fluids.FluidStack(be.tanks[0].getTankType(), be.tanks[0].getFill());
            }
            if (tank == 1) {
                return new net.minecraftforge.fluids.FluidStack(be.tanks[1].getTankType(), be.tanks[1].getFill());
            }
            return net.minecraftforge.fluids.FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            if (tank == 0) return be.tanks[0].getMaxFill();
            if (tank == 1) return be.tanks[1].getMaxFill();
            return 0;
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull net.minecraftforge.fluids.FluidStack stack) {
            return tank == 0 && be.getEnergyPerMb(stack.getFluid()) > 0;
        }

        @Override
        public int fill(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || be.getEnergyPerMb(resource.getFluid()) <= 0) return 0;
            int space = be.tanks[0].getMaxFill() - be.tanks[0].getFill();
            int toFill = Math.min(space, resource.getAmount());
            if (toFill <= 0) return 0;
            if (action.execute()) {
                be.tanks[0].fillMb(resource.getFluid(), toFill);
            }
            return toFill;
        }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || be.tanks[1].getFill() <= 0) return net.minecraftforge.fluids.FluidStack.EMPTY;
            if (!VanillaFluidEquivalence.sameSubstance(resource.getFluid(), be.tanks[1].getTankType())) {
                return net.minecraftforge.fluids.FluidStack.EMPTY;
            }
            int toDrain = Math.min(resource.getAmount(), be.tanks[1].getFill());
            net.minecraftforge.fluids.FluidStack drained = new net.minecraftforge.fluids.FluidStack(be.tanks[1].getTankType(), toDrain);
            if (action.execute()) {
                be.tanks[1].drainMb(toDrain);
            }
            return drained;
        }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0 || be.tanks[1].getFill() <= 0) return net.minecraftforge.fluids.FluidStack.EMPTY;
            int toDrain = Math.min(maxDrain, be.tanks[1].getFill());
            net.minecraftforge.fluids.FluidStack drained = new net.minecraftforge.fluids.FluidStack(be.tanks[1].getTankType(), toDrain);
            if (action.execute()) {
                be.tanks[1].drainMb(toDrain);
            }
            return drained;
        }
    }
    //?}
}
