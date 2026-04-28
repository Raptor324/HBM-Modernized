package com.hbm_m.block.entity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.entity.BaseMachineBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.machines.MachineIndustrialBoilerBlock;
import com.hbm_m.inventory.fluid.tank.FluidTank;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

//? if forge {
/*import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
*///?}

//? if fabric {
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
//?}

/**
 * Industrial Boiler BlockEntity - converts water to steam using heat/energy.
 * 
 * Stats from screenshot:
 * - Water tank: 64,000 mB
 * - Steam tank: 6,400,000 mB
 * - TU (Thermal Units) for heat input
 */
public class MachineIndustrialBoilerBlockEntity extends BaseMachineBlockEntity {

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

    // Conversion constants
    private static final int ENERGY_PER_MB_WATER = 10;     // Energy cost per mB of water converted
    private static final int STEAM_PER_WATER = 100;        // 1mB water = 100mB steam (expansion ratio)
    private static final int WATER_CONSUMPTION_RATE = 10;  // mB of water consumed per tick when active

    // Fluid tanks
    private final FluidTank waterTank;
    private final FluidTank steamTank;

    // Heat/thermal units (visual display)
    private int thermalUnits = 0;

    // GUI data
    protected final ContainerData data;

    //? if forge {
    /*private final LazyOptional<IFluidHandler> lazyWaterHandler;
    private final LazyOptional<IFluidHandler> lazySteamHandler;
    *///?}

    public MachineIndustrialBoilerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INDUSTRIAL_BOILER_BE.get(), pos, state,
              INVENTORY_SIZE, ENERGY_CAPACITY, ENERGY_RECEIVE_RATE);

        this.waterTank = new FluidTank(Fluids.WATER, WATER_CAPACITY);
        this.steamTank = new FluidTank(Fluids.EMPTY, STEAM_CAPACITY);

        //? if forge {
        /*this.lazyWaterHandler = LazyOptional.of(() -> new WaterFluidHandler(this));
        this.lazySteamHandler = LazyOptional.of(() -> new SteamFluidHandler(this));
        *///?}

        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> waterTank.getFill();
                    case 1 -> waterTank.getMaxFill();
                    case 2 -> steamTank.getFill();
                    case 3 -> steamTank.getMaxFill();
                    case 4 -> thermalUnits;
                    case 5 -> isActive() ? 1 : 0;
                    case 6 -> (int) (energy & 0xFFFFFFFF);         // Lower 32 bits
                    case 7 -> (int) ((energy >> 32) & 0xFFFFFFFF); // Upper 32 bits
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // Only thermal units can be set externally (for heat input from external sources)
                if (index == 4) thermalUnits = value;
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

        boolean wasActive = be.isActive();

        // Process fluid containers (fill water tank, empty steam tank)
        be.processFluidContainers();

        // Convert water to steam if we have enough energy
        be.processBoiling();

        // Update visual state
        if (wasActive != be.isActive()) {
            level.setBlock(pos, state.setValue(MachineIndustrialBoilerBlock.LIT, be.isActive()), 3);
        }

        be.setChanged();
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

    private void processBoiling() {
        // Check if we can boil water
        if (!canBoil()) {
            thermalUnits = Math.max(0, thermalUnits - 5); // Cool down when not active
            return;
        }

        // Calculate how much water we can process this tick
        int waterAvailable = waterTank.getFill();
        int waterToProcess = Math.min(WATER_CONSUMPTION_RATE, waterAvailable);

        // Check energy requirements
        long energyNeeded = (long) waterToProcess * ENERGY_PER_MB_WATER;
        if (energy < energyNeeded) {
            waterToProcess = (int) (energy / ENERGY_PER_MB_WATER);
            energyNeeded = (long) waterToProcess * ENERGY_PER_MB_WATER;
        }

        if (waterToProcess <= 0) {
            thermalUnits = Math.max(0, thermalUnits - 5);
            return;
        }

        // Calculate steam production
        int steamProduced = waterToProcess * STEAM_PER_WATER;
        int steamSpace = steamTank.getMaxFill() - steamTank.getFill();

        if (steamSpace < steamProduced) {
            // Limit by steam tank space
            steamProduced = steamSpace;
            waterToProcess = steamProduced / STEAM_PER_WATER;
            energyNeeded = (long) waterToProcess * ENERGY_PER_MB_WATER;
        }

        if (waterToProcess <= 0) {
            return;
        }

        // Consume water
        waterTank.fill(waterTank.getFill() - waterToProcess);

        // Consume energy
        energy -= energyNeeded;

        // Produce steam
        if (steamTank.getTankType() == Fluids.EMPTY) {
            steamTank.setTankType(Fluids.WATER); // Using water as placeholder for steam
        }
        steamTank.fill(steamTank.getFill() + steamProduced);

        // Update thermal units (visual heat indicator)
        thermalUnits = Math.min(1000, thermalUnits + waterToProcess);
    }

    private boolean canBoil() {
        return waterTank.getFill() > 0 && 
               energy > ENERGY_PER_MB_WATER &&
               steamTank.getFill() < steamTank.getMaxFill();
    }

    public boolean isActive() {
        return thermalUnits > 0 && canBoil();
    }

    // Getters for rendering/display
    public int getWaterAmount() { return waterTank.getFill(); }
    public int getWaterCapacity() { return waterTank.getMaxFill(); }
    public int getSteamAmount() { return steamTank.getFill(); }
    public int getSteamCapacity() { return steamTank.getMaxFill(); }
    public int getThermalUnits() { return thermalUnits; }

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
        
        tag.putInt("ThermalUnits", thermalUnits);
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
        thermalUnits = tag.getInt("ThermalUnits");
    }

    // --- Capabilities ---
    //? if forge {
    /*@Override
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
    *///?}

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
            /*case SLOT_WATER_IN -> stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
            case SLOT_STEAM_IN -> stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
            *///?}
            //? if fabric {
            case SLOT_WATER_IN -> FluidStorage.ITEM.find(stack, null) != null;
            case SLOT_STEAM_IN -> FluidStorage.ITEM.find(stack, null) != null;
            //?}
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
    /*private static class WaterFluidHandler implements IFluidHandler {
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
            return stack.getFluid() == Fluids.WATER;
        }

        @Override
        public int fill(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || resource.getFluid() != Fluids.WATER) return 0;
            
            int space = be.waterTank.getMaxFill() - be.waterTank.getFill();
            int toFill = Math.min(space, resource.getAmount());
            
            if (action.execute()) {
                be.waterTank.setTankType(Fluids.WATER);
                be.waterTank.fill(be.waterTank.getFill() + toFill);
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
                be.steamTank.fill(be.steamTank.getFill() - toDrain);
            }
            return drained;
        }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack drain(int maxDrain, FluidAction action) {
            if (be.steamTank.getFill() <= 0) return net.minecraftforge.fluids.FluidStack.EMPTY;
            
            int toDrain = Math.min(be.steamTank.getFill(), maxDrain);
            net.minecraftforge.fluids.FluidStack drained = new net.minecraftforge.fluids.FluidStack(be.steamTank.getTankType(), toDrain);
            
            if (action.execute()) {
                be.steamTank.fill(be.steamTank.getFill() - toDrain);
            }
            return drained;
        }
    }
    *///?}
}
