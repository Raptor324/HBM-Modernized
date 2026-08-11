package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.block.machines.FluidDuctBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
//?}

public class MachineSteamCondenserBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    private static final int INVENTORY_SIZE = 0;
    private static final int INPUT_CAPACITY = 8_000;
    private static final int OUTPUT_CAPACITY = 8_000;
    private final FluidTank inputSteamTank;
    private final FluidTank outputWaterTank;

    private int age = 0;
    private int waterTimer = 0;
    private int throughput = 0;

    public MachineSteamCondenserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEAM_CONDENSER_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L);
        this.inputSteamTank = new FluidTank(ModFluids.SPENTSTEAM.getSource(), INPUT_CAPACITY);
        this.outputWaterTank = new FluidTank(Fluids.WATER, OUTPUT_CAPACITY);
    }

    @Override
    protected void setupFluidCapability() {
        //? if forge {
        setFluidHandler(new UnifiedFluidHandler(this));
        //?}
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null) {
            FluidDuctBlock.refreshAdjacentDucts(level, worldPosition);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineSteamCondenserBlockEntity be) {
        if (level.isClientSide()) return;

        if (level.getGameTime() % 20L == 0L) {
            FluidDuctBlock.refreshAdjacentDucts(level, pos);
        }

        be.age = (be.age + 1) % 2;
        if (be.waterTimer > 0) {
            be.waterTimer--;
        }

        for (Direction dir : Direction.values()) {
            BlockPos pipePos = pos.relative(dir);
            BlockEntity pipeBe = level.getBlockEntity(pipePos);
            if (!(pipeBe instanceof com.hbm_m.api.fluids.IFluidConnectorMK2)) continue;

            be.trySubscribe(ModFluids.SPENTSTEAM.getSource(), level, pipePos, dir);
            if (be.outputWaterTank.getFill() > 0) {
                be.tryProvide(be.outputWaterTank, level, pipePos, dir);
            }
        }

        be.processCondensation();
    }

    private void processCondensation() {
        int convert = Math.min(inputSteamTank.getFill(), outputWaterTank.getMaxFill() - outputWaterTank.getFill());
        throughput = convert;
        if (convert <= 0) return;

        inputSteamTank.drainMb(convert);
        outputWaterTank.fillMb(Fluids.WATER, convert);
        waterTimer = 20;
        setChanged();
        sendUpdateToClient();
    }

    @Override
    public FluidTank[] getSendingTanks() {
        return outputWaterTank.getFill() > 0 ? new FluidTank[] { outputWaterTank } : FluidTank.EMPTY_ARRAY;
    }

    @Override
    public FluidTank[] getReceivingTanks() {
        return new FluidTank[] { inputSteamTank };
    }

    @Override
    public FluidTank[] getAllTanks() {
        return new FluidTank[] { inputSteamTank, outputWaterTank };
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(net.minecraft.world.level.material.Fluid fluid, Direction fromDir) {
        if (fromDir == null) return false;
        return VanillaFluidEquivalence.sameSubstance(fluid, ModFluids.SPENTSTEAM.getSource())
                || VanillaFluidEquivalence.sameSubstance(fluid, Fluids.WATER);
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

    //? if < 1.21.1 {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        inputSteamTank.writeToNBT(tag, "input_steam");
        outputWaterTank.writeToNBT(tag, "output_water");
        tag.putInt("age", age);
        tag.putInt("water_timer", waterTimer);
        tag.putInt("throughput", throughput);
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.saveAdditional(tag, registries);
        inputSteamTank.writeToNBT(tag, "input_steam");
        outputWaterTank.writeToNBT(tag, "output_water");
        tag.putInt("age", age);
        tag.putInt("water_timer", waterTimer);
        tag.putInt("throughput", throughput);
    
    }
    *///?}

    //? if < 1.21.1 {
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inputSteamTank.readFromNBT(tag, "input_steam");
        outputWaterTank.readFromNBT(tag, "output_water");
        age = tag.getInt("age");
        waterTimer = tag.getInt("water_timer");
        throughput = tag.getInt("throughput");
    }
    //?} else {
    /*@Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.loadAdditional(tag, registries);
        inputSteamTank.readFromNBT(tag, "input_steam");
        outputWaterTank.readFromNBT(tag, "output_water");
        age = tag.getInt("age");
        waterTimer = tag.getInt("water_timer");
        throughput = tag.getInt("throughput");
    
    }
    *///?}

    //? if forge {
    private static class UnifiedFluidHandler implements IFluidHandler {
        private final MachineSteamCondenserBlockEntity be;

        UnifiedFluidHandler(MachineSteamCondenserBlockEntity be) {
            this.be = be;
        }

        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            if (tank == 0) {
                if (be.inputSteamTank.getFill() <= 0) return FluidStack.EMPTY;
                return new FluidStack(be.inputSteamTank.getTankType(), be.inputSteamTank.getFill());
            }
            if (tank == 1) {
                if (be.outputWaterTank.getFill() <= 0) return FluidStack.EMPTY;
                return new FluidStack(Fluids.WATER, be.outputWaterTank.getFill());
            }
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            if (tank == 0) return be.inputSteamTank.getMaxFill();
            if (tank == 1) return be.outputWaterTank.getMaxFill();
            return 0;
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return tank == 0 && VanillaFluidEquivalence.sameSubstance(stack.getFluid(), ModFluids.SPENTSTEAM.getSource());
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !VanillaFluidEquivalence.sameSubstance(resource.getFluid(), ModFluids.SPENTSTEAM.getSource())) {
                return 0;
            }

            int space = be.inputSteamTank.getMaxFill() - be.inputSteamTank.getFill();
            int toFill = Math.min(space, resource.getAmount());
            if (toFill <= 0) return 0;

            if (action.execute()) {
                be.inputSteamTank.fillMb(resource.getFluid(), toFill);
                be.setChanged();
                be.sendUpdateToClient();
            }

            return toFill;
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || be.outputWaterTank.getFill() <= 0) {
                return FluidStack.EMPTY;
            }
            if (!VanillaFluidEquivalence.sameSubstance(resource.getFluid(), Fluids.WATER)) {
                return FluidStack.EMPTY;
            }

            int toDrain = Math.min(resource.getAmount(), be.outputWaterTank.getFill());
            FluidStack result = new FluidStack(Fluids.WATER, toDrain);

            if (action.execute()) {
                be.outputWaterTank.drainMb(toDrain);
                be.setChanged();
                be.sendUpdateToClient();
            }

            return result;
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            if (be.outputWaterTank.getFill() <= 0 || maxDrain <= 0) return FluidStack.EMPTY;

            int toDrain = Math.min(maxDrain, be.outputWaterTank.getFill());
            FluidStack result = new FluidStack(Fluids.WATER, toDrain);

            if (action.execute()) {
                be.outputWaterTank.drainMb(toDrain);
                be.setChanged();
                be.sendUpdateToClient();
            }

            return result;
        }
    }
    //?}
}
