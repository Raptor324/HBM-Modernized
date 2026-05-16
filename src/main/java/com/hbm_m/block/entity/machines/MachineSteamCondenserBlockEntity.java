package com.hbm_m.block.entity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.entity.BaseMachineBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

public class MachineSteamCondenserBlockEntity extends BaseMachineBlockEntity {

    private static final int INVENTORY_SIZE = 0;
    private static final int INPUT_CAPACITY = 8_000;
    private static final int OUTPUT_CAPACITY = 8_000;
    private static final int MB_PER_SECOND = 100;
    private static final int TICKS_PER_SECOND = 20;

    private final FluidTank inputSteamTank;
    private final FluidTank outputWaterTank;

    private final LazyOptional<IFluidHandler> lazyInputHandler;
    private final LazyOptional<IFluidHandler> lazyOutputHandler;

    private int conversionTimer = 0;

    public MachineSteamCondenserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEAM_CONDENSER_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L);
        this.inputSteamTank = new FluidTank(ModFluids.SPENTSTEAM.getSource(), INPUT_CAPACITY);
        this.outputWaterTank = new FluidTank(Fluids.WATER, OUTPUT_CAPACITY);

        this.lazyInputHandler = LazyOptional.of(() -> new InputFluidHandler(this));
        this.lazyOutputHandler = LazyOptional.of(() -> new OutputFluidHandler(this));
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineSteamCondenserBlockEntity be) {
        if (level.isClientSide()) return;

        be.conversionTimer++;
        if (be.conversionTimer >= TICKS_PER_SECOND) {
            be.conversionTimer = 0;
            be.processCondensation();
        }
    }

    private void processCondensation() {
        if (inputSteamTank.getFill() < MB_PER_SECOND) return;

        int outputSpace = outputWaterTank.getMaxFill() - outputWaterTank.getFill();
        if (outputSpace < MB_PER_SECOND) return;

        inputSteamTank.fill(inputSteamTank.getFill() - MB_PER_SECOND);
        outputWaterTank.setTankType(Fluids.WATER);
        outputWaterTank.fill(outputWaterTank.getFill() + MB_PER_SECOND);
        setChanged();
        sendUpdateToClient();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.steam_condenser");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return null;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.steam_condenser");
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        inputSteamTank.writeToNBT(tag, "input_steam");
        outputWaterTank.writeToNBT(tag, "output_water");
        tag.putInt("conversion_timer", conversionTimer);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inputSteamTank.readFromNBT(tag, "input_steam");
        outputWaterTank.readFromNBT(tag, "output_water");
        conversionTimer = tag.getInt("conversion_timer");
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            if (side == Direction.UP) return lazyOutputHandler.cast();
            return lazyInputHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyInputHandler.invalidate();
        lazyOutputHandler.invalidate();
    }

    private static class InputFluidHandler implements IFluidHandler {
        private final MachineSteamCondenserBlockEntity be;

        InputFluidHandler(MachineSteamCondenserBlockEntity be) {
            this.be = be;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            if (be.inputSteamTank.getFill() <= 0) return FluidStack.EMPTY;
            return new FluidStack(ModFluids.SPENTSTEAM.getSource(), be.inputSteamTank.getFill());
        }

        @Override
        public int getTankCapacity(int tank) {
            return be.inputSteamTank.getMaxFill();
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return stack.getFluid() == ModFluids.SPENTSTEAM.getSource();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || resource.getFluid() != ModFluids.SPENTSTEAM.getSource()) return 0;

            int space = be.inputSteamTank.getMaxFill() - be.inputSteamTank.getFill();
            int toFill = Math.min(space, resource.getAmount());
            if (toFill <= 0) return 0;

            if (action.execute()) {
                be.inputSteamTank.setTankType(ModFluids.SPENTSTEAM.getSource());
                be.inputSteamTank.fill(be.inputSteamTank.getFill() + toFill);
                be.setChanged();
            }

            return toFill;
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }

    private static class OutputFluidHandler implements IFluidHandler {
        private final MachineSteamCondenserBlockEntity be;

        OutputFluidHandler(MachineSteamCondenserBlockEntity be) {
            this.be = be;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            if (be.outputWaterTank.getFill() <= 0) return FluidStack.EMPTY;
            return new FluidStack(Fluids.WATER, be.outputWaterTank.getFill());
        }

        @Override
        public int getTankCapacity(int tank) {
            return be.outputWaterTank.getMaxFill();
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || resource.getFluid() != Fluids.WATER || be.outputWaterTank.getFill() <= 0) {
                return FluidStack.EMPTY;
            }

            int toDrain = Math.min(resource.getAmount(), be.outputWaterTank.getFill());
            FluidStack result = new FluidStack(Fluids.WATER, toDrain);

            if (action.execute()) {
                be.outputWaterTank.fill(be.outputWaterTank.getFill() - toDrain);
                be.setChanged();
            }

            return result;
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            if (be.outputWaterTank.getFill() <= 0 || maxDrain <= 0) return FluidStack.EMPTY;

            int toDrain = Math.min(maxDrain, be.outputWaterTank.getFill());
            FluidStack result = new FluidStack(Fluids.WATER, toDrain);

            if (action.execute()) {
                be.outputWaterTank.fill(be.outputWaterTank.getFill() - toDrain);
                be.setChanged();
            }

            return result;
        }
    }
}
