package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.block.machines.MachineIndustrialBoilerBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Heatable;
import com.hbm_m.inventory.fluid.trait.FT_Heatable.HeatingStep;
import com.hbm_m.inventory.fluid.trait.FT_Heatable.HeatingType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}

//? if fabric {
/*import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
*///?}

/**
 * Industrial Boiler BlockEntity - converts heatable fluids to steam products.
 * 
 * Stats from screenshot:
 * - Water tank: 64,000 mB
 * - Steam tank: 6,400,000 mB
 * - TU (Thermal Units) heat buffer
 */
@SuppressWarnings("UnstableApiUsage")
public class MachineIndustrialBoilerBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    // Slot definitions
    public static final int SLOT_WATER_IN = 0;
    public static final int SLOT_WATER_OUT = 1;
    public static final int SLOT_STEAM_IN = 2;
    public static final int SLOT_STEAM_OUT = 3;
    public static final int INVENTORY_SIZE = 4;

    // Capacity constants
    private static final long ENERGY_CAPACITY = 500_000L;
    private static final long ENERGY_RECEIVE_RATE = 10_000L;
    private static final int WATER_CAPACITY = 64_000;      // 64,000 mB
    private static final int STEAM_CAPACITY = 6_400_000;    // 6,400,000 mB

    // Heat model (ported behavior)
    private static final int MAX_HEAT = 12_800_000;
    private static final double DIFFUSION = 0.1D;
    private static final int MAX_ENERGY_TO_HEAT_PER_TICK = 20_000;

    // Fluid tanks
    private final FluidTank waterTank;
    private final FluidTank steamTank;

    // Heat buffer (TU)
    private int heat = 0;
    private boolean isOn = false;

    // GUI data
    protected final ContainerData data;

    //? if forge {
    private final LazyOptional<IFluidHandler> lazyWaterHandler;
    private final LazyOptional<IFluidHandler> lazySteamHandler;
    //?}

    public MachineIndustrialBoilerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INDUSTRIAL_BOILER_BE.get(), pos, state,
              INVENTORY_SIZE, ENERGY_CAPACITY, ENERGY_RECEIVE_RATE);

        this.waterTank = new FluidTank(Fluids.WATER, WATER_CAPACITY);
        this.steamTank = new FluidTank(Fluids.EMPTY, STEAM_CAPACITY);

        //? if forge {
        this.lazyWaterHandler = LazyOptional.of(() -> new WaterFluidHandler(this));
        this.lazySteamHandler = LazyOptional.of(() -> new SteamFluidHandler(this));
        //?}

        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> waterTank.getFill();
                    case 1 -> waterTank.getMaxFill();
                    case 2 -> steamTank.getFill();
                    case 3 -> steamTank.getMaxFill();
                    case 4 -> heat;
                    case 5 -> isOn ? 1 : 0;
                    case 6 -> (int) (energy & 0xFFFFFFFF);         // Lower 32 bits
                    case 7 -> (int) ((energy >> 32) & 0xFFFFFFFF); // Upper 32 bits
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                if (index == 4) heat = Math.max(0, Math.min(MAX_HEAT, value));
            }

            @Override
            public int getCount() {
                return 8;
            }
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineIndustrialBoilerBlockEntity be) {
        if (level.isClientSide()) return;

        be.ensureNetworkInitialized();

        boolean wasActive = be.isOn;

        be.processFluidContainers();
        be.pullHeat();
        be.setupTanks();
        be.isOn = false;
        be.tryConvert();

        // MK2 network: steam out via UP, input fluid via sides/bottom.
        net.minecraft.world.level.material.Fluid requestedInput = be.getRequestedInputFluid();
        for (Direction dir : Direction.values()) {
            BlockPos pipePos = pos.relative(dir);
            BlockEntity pipeBe = level.getBlockEntity(pipePos);
            if (!(pipeBe instanceof com.hbm_m.api.fluids.IFluidConnectorMK2)) continue;

            if (dir == Direction.UP) {
                if (be.steamTank.getFill() > 0) {
                    be.tryProvide(be.steamTank, level, pipePos, dir);
                }
            } else {
                be.trySubscribe(requestedInput, level, pipePos, dir);
            }
        }

        // Update visual state
        if (wasActive != be.isOn) {
            level.setBlock(pos, state.setValue(MachineIndustrialBoilerBlock.LIT, be.isOn), 3);
        }

        be.setChanged();
    }

    // =====================================================================================
    // IFluidStandardTransceiverMK2
    // - waterTank — приёмник (вода)
    // - steamTank — поставщик (placeholder Fluids.WATER до появления реальной жидкости steam;
    //   для steam-жидкости уже сейчас можно будет включить pressure-тиры (1/2/3)).
    // =====================================================================================

    @Override
    public FluidTank[] getAllTanks() { return new FluidTank[]{ waterTank, steamTank }; }

    @Override
    public FluidTank[] getReceivingTanks() { return new FluidTank[]{ waterTank }; }

    @Override
    public FluidTank[] getSendingTanks() {
        return steamTank.getFill() > 0 ? new FluidTank[]{ steamTank } : new FluidTank[0];
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(net.minecraft.world.level.material.Fluid fluid, Direction fromDir) {
        if (fromDir == null) return false;

        if (fromDir == Direction.UP) {
            if (steamTank.getFill() <= 0) return true;
            return VanillaFluidEquivalence.sameSubstance(fluid, steamTank.getTankType());
        }

        if (waterTank.getFill() > 0) {
            return VanillaFluidEquivalence.sameSubstance(fluid, waterTank.getTankType());
        }
        return canAcceptInputFluid(fluid);
    }

    private net.minecraft.world.level.material.Fluid getRequestedInputFluid() {
        net.minecraft.world.level.material.Fluid type = waterTank.getTankType();
        return FluidTank.isFluidTypeExplicitlySet(type) ? type : Fluids.WATER;
    }

    private boolean canAcceptInputFluid(net.minecraft.world.level.material.Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY || fluid == ModFluids.NONE.getSource()) return false;
        FT_Heatable trait = FluidType.getTrait(fluid, FT_Heatable.class);
        return trait != null && trait.getEfficiency(HeatingType.BOILER) > 0 && trait.getFirstStep() != null;
    }

    private void processFluidContainers() {
        // Process water input slot - load water from containers
        ItemStack[] waterSlots = new ItemStack[INVENTORY_SIZE];
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            waterSlots[i] = inventory.getStackInSlot(i);
        }
        if (waterTank.loadTank(SLOT_WATER_IN, SLOT_WATER_OUT, waterSlots)) {
            for (int i = 0; i < INVENTORY_SIZE; i++) {
                inventory.setStackInSlot(i, waterSlots[i]);
            }
        }

        // Process steam output slot - unload steam to containers
        ItemStack[] steamSlots = new ItemStack[INVENTORY_SIZE];
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            steamSlots[i] = inventory.getStackInSlot(i);
        }
        if (steamTank.unloadTank(SLOT_STEAM_IN, SLOT_STEAM_OUT, steamSlots)) {
            for (int i = 0; i < INVENTORY_SIZE; i++) {
                inventory.setStackInSlot(i, steamSlots[i]);
            }
        }
    }

    private void pullHeat() {
        int gainedHeat = 0;

        if (energy > 0 && heat < MAX_HEAT) {
            int targetPull = (int) Math.ceil((MAX_HEAT - heat) * DIFFUSION * 0.001D);
            if (targetPull < 1) targetPull = 1;

            int pull = (int) Math.min(energy, Math.min(MAX_ENERGY_TO_HEAT_PER_TICK, targetPull));
            pull = Math.min(pull, MAX_HEAT - heat);

            if (pull > 0) {
                energy -= pull;
                heat += pull;
                gainedHeat = pull;
            }
        }

        if (gainedHeat == 0) {
            heat = Math.max(heat - Math.max(heat / 1000, 1), 0);
        }
    }

    private void setupTanks() {
        FT_Heatable trait = FluidType.getTrait(waterTank.getTankType(), FT_Heatable.class);
        if (trait != null && trait.getEfficiency(HeatingType.BOILER) > 0) {
            HeatingStep step = trait.getFirstStep();
            if (step != null) {
                if (steamTank.getFill() <= 0 || VanillaFluidEquivalence.sameSubstance(steamTank.getTankType(), step.typeProduced)) {
                    if (!VanillaFluidEquivalence.sameSubstance(steamTank.getTankType(), step.typeProduced)) {
                        steamTank.setTankType(step.typeProduced);
                    }
                }

                int req = Math.max(1, step.amountReq);
                int prod = Math.max(1, step.amountProduced);
                int targetCap = Math.max(1, waterTank.getMaxFill() * prod / req);
                steamTank.changeTankSize(targetCap);
                return;
            }
        }

        if (waterTank.getFill() <= 0) {
            waterTank.setTankType(ModFluids.NONE.getSource());
        }
        if (steamTank.getFill() <= 0) {
            steamTank.setTankType(ModFluids.NONE.getSource());
        }
    }

    private void tryConvert() {
        FT_Heatable trait = FluidType.getTrait(waterTank.getTankType(), FT_Heatable.class);
        if (trait == null) return;

        double eff = trait.getEfficiency(HeatingType.BOILER);
        if (eff <= 0) return;

        HeatingStep step = trait.getFirstStep();
        if (step == null) return;

        if (steamTank.getFill() > 0 && !VanillaFluidEquivalence.sameSubstance(steamTank.getTankType(), step.typeProduced)) {
            return;
        }

        int heatReq = (int) Math.max(Math.ceil(step.heatReq / eff), 1);
        int inputOps = waterTank.getFill() / step.amountReq;
        int outputOps = (steamTank.getMaxFill() - steamTank.getFill()) / step.amountProduced;
        int heatOps = heat / heatReq;

        int ops = Math.min(inputOps, Math.min(outputOps, heatOps));
        if (ops <= 0) return;

        waterTank.drainMb(step.amountReq * ops);
        if (steamTank.getFill() <= 0) {
            steamTank.setTankType(step.typeProduced);
        }
        steamTank.fillMb(step.typeProduced, step.amountProduced * ops);
        heat -= heatReq * ops;
        isOn = true;
    }

    public boolean isActive() {
        return isOn;
    }

    // Getters for rendering/display
    public int getWaterAmount() { return waterTank.getFill(); }
    public int getWaterCapacity() { return waterTank.getMaxFill(); }
    public int getSteamAmount() { return steamTank.getFill(); }
    public int getSteamCapacity() { return steamTank.getMaxFill(); }
    public int getThermalUnits() { return heat; }

    // --- NBT ---
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        
        CompoundTag waterTag = new CompoundTag();
        waterTank.writeToNBT(waterTag, "water");
        tag.put("WaterTank", waterTag);
        
        CompoundTag steamTag = new CompoundTag();
        steamTank.writeToNBT(steamTag, "steam");
        tag.put("SteamTank", steamTag);
        
        tag.putInt("Heat", heat);
        tag.putInt("ThermalUnits", heat);
        tag.putBoolean("IsOn", isOn);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        
        if (tag.contains("WaterTank")) {
            waterTank.readFromNBT(tag.getCompound("WaterTank"), "water");
        }
        if (tag.contains("SteamTank")) {
            steamTank.readFromNBT(tag.getCompound("SteamTank"), "steam");
        }
        if (tag.contains("Heat")) {
            heat = tag.getInt("Heat");
        } else {
            heat = tag.getInt("ThermalUnits");
        }
        isOn = tag.getBoolean("IsOn");
    }

    // --- Capabilities ---
    //? if forge {
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            // Water input from sides and bottom, steam output from top
            if (side == Direction.UP) {
                return lazySteamHandler.cast();
            }
            return lazyWaterHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyWaterHandler.invalidate();
        lazySteamHandler.invalidate();
    }
    //?}

    // --- GUI ---
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.industrial_boiler");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        // TODO: Implement menu
        return null;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.industrial_boiler");
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            //? if forge {
            case SLOT_WATER_IN -> stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
            case SLOT_STEAM_IN -> stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
            //?}
            //? if fabric {
            /*case SLOT_WATER_IN -> FluidStorage.ITEM.find(stack, null) != null;
            case SLOT_STEAM_IN -> FluidStorage.ITEM.find(stack, null) != null;
            *///?}
            case SLOT_WATER_OUT, SLOT_STEAM_OUT -> false; // Output slots
            default -> false;
        };
    }

    public void drops() {
        if (level != null) {
            SimpleContainer simpleContainer = new SimpleContainer(inventory.getSlots());
            for (int i = 0; i < inventory.getSlots(); i++) {
                simpleContainer.setItem(i, inventory.getStackInSlot(i));
            }
            Containers.dropContents(this.level, this.worldPosition, simpleContainer);
        }
    }

    // --- Fluid Handlers ---
    //? if forge {
    private static class WaterFluidHandler implements IFluidHandler {
        private final MachineIndustrialBoilerBlockEntity be;

        WaterFluidHandler(MachineIndustrialBoilerBlockEntity be) {
            this.be = be;
        }

        @Override
        public int getTanks() { return 1; }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack getFluidInTank(int tank) {
            return new net.minecraftforge.fluids.FluidStack(be.waterTank.getTankType(), be.waterTank.getFill());
        }

        @Override
        public int getTankCapacity(int tank) { return be.waterTank.getMaxFill(); }

        @Override
        public boolean isFluidValid(int tank, @NotNull net.minecraftforge.fluids.FluidStack stack) {
            return be.canAcceptInputFluid(stack.getFluid());
        }

        @Override
        public int fill(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !be.canAcceptInputFluid(resource.getFluid())) return 0;
            
            int space = be.waterTank.getMaxFill() - be.waterTank.getFill();
            int toFill = Math.min(space, resource.getAmount());
            
            if (action.execute()) {
                if (be.waterTank.getFill() <= 0) {
                    be.waterTank.setTankType(resource.getFluid());
                }
                be.waterTank.fillMb(resource.getFluid(), toFill);
            }
            return toFill;
        }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            return net.minecraftforge.fluids.FluidStack.EMPTY; // Water tank is input only
        }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack drain(int maxDrain, FluidAction action) {
            return net.minecraftforge.fluids.FluidStack.EMPTY; // Water tank is input only
        }
    }

    private static class SteamFluidHandler implements IFluidHandler {
        private final MachineIndustrialBoilerBlockEntity be;

        SteamFluidHandler(MachineIndustrialBoilerBlockEntity be) {
            this.be = be;
        }

        @Override
        public int getTanks() { return 1; }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack getFluidInTank(int tank) {
            return new net.minecraftforge.fluids.FluidStack(be.steamTank.getTankType(), be.steamTank.getFill());
        }

        @Override
        public int getTankCapacity(int tank) { return be.steamTank.getMaxFill(); }

        @Override
        public boolean isFluidValid(int tank, @NotNull net.minecraftforge.fluids.FluidStack stack) {
            return false; // Steam tank is output only
        }

        @Override
        public int fill(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            return 0; // Steam tank is output only
        }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || be.steamTank.getFill() <= 0) return net.minecraftforge.fluids.FluidStack.EMPTY;
            
            int toDrain = Math.min(be.steamTank.getFill(), resource.getAmount());
            net.minecraftforge.fluids.FluidStack drained = new net.minecraftforge.fluids.FluidStack(be.steamTank.getTankType(), toDrain);
            
            if (action.execute()) {
                be.steamTank.drainMb(toDrain);
            }
            return drained;
        }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack drain(int maxDrain, FluidAction action) {
            if (be.steamTank.getFill() <= 0) return net.minecraftforge.fluids.FluidStack.EMPTY;
            
            int toDrain = Math.min(be.steamTank.getFill(), maxDrain);
            net.minecraftforge.fluids.FluidStack drained = new net.minecraftforge.fluids.FluidStack(be.steamTank.getTankType(), toDrain);
            
            if (action.execute()) {
                be.steamTank.drainMb(toDrain);
            }
            return drained;
        }
    }
    //?}
}
