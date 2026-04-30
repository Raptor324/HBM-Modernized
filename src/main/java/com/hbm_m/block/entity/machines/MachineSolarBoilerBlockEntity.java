package com.hbm_m.block.entity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.entity.BaseMachineBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.machines.MachineSolarBoilerBlock;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.items.IItemHandler;

/**
 * Solar Boiler BlockEntity - converts water to steam using sunlight.
 * Requires clear sky access and daytime to operate.
 * No RF energy consumption.
 */
public class MachineSolarBoilerBlockEntity extends BaseMachineBlockEntity {

    // Slot definitions
    public static final int SLOT_WATER_IN  = 0;
    public static final int SLOT_WATER_OUT = 1;
    public static final int SLOT_STEAM_IN  = 2;
    public static final int SLOT_STEAM_OUT = 3;
    public static final int INVENTORY_SIZE = 4;

    // Capacity constants
    private static final int WATER_CAPACITY = 16_000;        // 16,000 mB
    private static final int STEAM_CAPACITY = 1_600_000;     // 1,600,000 mB

    // Conversion constants
    private static final int STEAM_PER_WATER = 100;          // 1 mB water -> 100 mB steam
    private static final int MAX_WATER_PER_TICK = 5;         // max mB water/tick at full sun

    // Fluid tanks
    private final FluidTank waterTank;
    private final FluidTank steamTank;

    // Cached solar efficiency (0-15 sky brightness)
    private int solarBrightness = 0;

    // GUI data
    protected final ContainerData data;

    private final LazyOptional<IFluidHandler> lazyWaterHandler;
    private final LazyOptional<IFluidHandler> lazySteamHandler;

    public MachineSolarBoilerBlockEntity(BlockPos pos, BlockState state) {
        // No RF energy capacity/receive rate - solar powered
        super(ModBlockEntities.SOLAR_BOILER_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L);

        this.waterTank = new FluidTank(Fluids.WATER, WATER_CAPACITY);
        this.steamTank = new FluidTank(Fluids.EMPTY, STEAM_CAPACITY);

        this.lazyWaterHandler = LazyOptional.of(() -> new WaterFluidHandler(this));
        this.lazySteamHandler = LazyOptional.of(() -> new SteamFluidHandler(this));

        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> waterTank.getFill();
                    case 1 -> waterTank.getMaxFill();
                    case 2 -> steamTank.getFill();
                    case 3 -> steamTank.getMaxFill();
                    case 4 -> solarBrightness;
                    case 5 -> isActive() ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                if (index == 4) solarBrightness = value;
            }

            @Override
            public int getCount() { return 6; }
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineSolarBoilerBlockEntity be) {
        if (level.isClientSide()) return;

        boolean wasActive = be.isActive();

        // Update solar brightness (sky brightness at block above this one)
        be.solarBrightness = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos.above());

        // Process fluid containers
        be.processFluidContainers();

        // Convert water to steam using solar energy
        be.processSolarBoiling(level, pos);

        // Update visual state
        boolean nowActive = be.isActive();
        if (wasActive != nowActive) {
            level.setBlock(pos, state.setValue(MachineSolarBoilerBlock.LIT, nowActive), 3);
        }

        be.setChanged();
    }

    private void processFluidContainers() {
        ItemStack[] slots = new ItemStack[INVENTORY_SIZE];
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            slots[i] = inventory.getStackInSlot(i);
        }

        if (waterTank.loadTank(SLOT_WATER_IN, SLOT_WATER_OUT, slots)) {
            for (int i = 0; i < INVENTORY_SIZE; i++) inventory.setStackInSlot(i, slots[i]);
        }

        // Re-read slots after potential modification
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            slots[i] = inventory.getStackInSlot(i);
        }

        if (steamTank.unloadTank(SLOT_STEAM_IN, SLOT_STEAM_OUT, slots)) {
            for (int i = 0; i < INVENTORY_SIZE; i++) inventory.setStackInSlot(i, slots[i]);
        }
    }

    private void processSolarBoiling(Level level, BlockPos pos) {
        if (!canBoil(level, pos)) return;

        // Scale water consumption by solar brightness (0-15)
        int waterToProcess = (MAX_WATER_PER_TICK * solarBrightness) / 15;
        if (waterToProcess <= 0) return;

        waterToProcess = Math.min(waterToProcess, waterTank.getFill());
        if (waterToProcess <= 0) return;

        int steamProduced = waterToProcess * STEAM_PER_WATER;
        int steamSpace = steamTank.getMaxFill() - steamTank.getFill();

        if (steamSpace < steamProduced) {
            steamProduced = steamSpace;
            waterToProcess = steamProduced / STEAM_PER_WATER;
        }

        if (waterToProcess <= 0) return;

        // Consume water
        waterTank.fill(waterTank.getFill() - waterToProcess);

        // Produce steam
        if (steamTank.getTankType() == Fluids.EMPTY) {
            steamTank.setTankType(Fluids.WATER); // water as steam placeholder
        }
        steamTank.fill(steamTank.getFill() + steamProduced);
    }

    private boolean canBoil(Level level, BlockPos pos) {
        // Needs clear sky view and minimum brightness
        return level.canSeeSky(pos.above())
                && solarBrightness > 0
                && waterTank.getFill() > 0
                && steamTank.getFill() < steamTank.getMaxFill();
    }

    public boolean isActive() {
        return solarBrightness > 0
                && waterTank.getFill() > 0
                && steamTank.getFill() < steamTank.getMaxFill();
    }

    // Getters
    public int getWaterAmount()    { return waterTank.getFill(); }
    public int getWaterCapacity()  { return waterTank.getMaxFill(); }
    public int getSteamAmount()    { return steamTank.getFill(); }
    public int getSteamCapacity()  { return steamTank.getMaxFill(); }
    public int getSolarBrightness() { return solarBrightness; }

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

        tag.putInt("SolarBrightness", solarBrightness);
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
        solarBrightness = tag.getInt("SolarBrightness");
    }

    // --- Capabilities ---
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            if (side == Direction.UP) return lazySteamHandler.cast();
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

    // --- GUI ---
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.solar_boiler");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        // TODO: Implement menu
        return null;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.solar_boiler");
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_WATER_IN  -> stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
            case SLOT_STEAM_IN  -> stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
            case SLOT_WATER_OUT, SLOT_STEAM_OUT -> false;
            default -> false;
        };
    }

    public void drops() {
        if (level != null) {
            SimpleContainer container = new SimpleContainer(inventory.getSlots());
            for (int i = 0; i < inventory.getSlots(); i++) {
                container.setItem(i, inventory.getStackInSlot(i));
            }
            Containers.dropContents(level, worldPosition, container);
        }
    }

    // --- Inner fluid handler classes ---

    private static class WaterFluidHandler implements IFluidHandler {
        private final MachineSolarBoilerBlockEntity be;
        WaterFluidHandler(MachineSolarBoilerBlockEntity be) { this.be = be; }

        @Override
        public int getTanks() { return 1; }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack getFluidInTank(int tank) {
            if (be.waterTank.getFill() > 0)
                return new net.minecraftforge.fluids.FluidStack(Fluids.WATER, be.waterTank.getFill());
            return net.minecraftforge.fluids.FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) { return be.waterTank.getMaxFill(); }

        @Override
        public boolean isFluidValid(int tank, @NotNull net.minecraftforge.fluids.FluidStack stack) {
            return stack.getFluid() == Fluids.WATER;
        }

        @Override
        public int fill(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            if (resource.getFluid() != Fluids.WATER) return 0;
            int space = be.waterTank.getMaxFill() - be.waterTank.getFill();
            int toFill = Math.min(space, resource.getAmount());
            if (action.execute()) {
                be.waterTank.fill(be.waterTank.getFill() + toFill);
                be.setChanged();
            }
            return toFill;
        }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            return net.minecraftforge.fluids.FluidStack.EMPTY;
        }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack drain(int maxDrain, FluidAction action) {
            return net.minecraftforge.fluids.FluidStack.EMPTY;
        }
    }

    private static class SteamFluidHandler implements IFluidHandler {
        private final MachineSolarBoilerBlockEntity be;
        SteamFluidHandler(MachineSolarBoilerBlockEntity be) { this.be = be; }

        @Override
        public int getTanks() { return 1; }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack getFluidInTank(int tank) {
            if (be.steamTank.getFill() > 0)
                return new net.minecraftforge.fluids.FluidStack(Fluids.WATER, be.steamTank.getFill());
            return net.minecraftforge.fluids.FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) { return be.steamTank.getMaxFill(); }

        @Override
        public boolean isFluidValid(int tank, @NotNull net.minecraftforge.fluids.FluidStack stack) {
            return false; // output only
        }

        @Override
        public int fill(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            return 0; // output only
        }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            if (be.steamTank.getFill() <= 0) return net.minecraftforge.fluids.FluidStack.EMPTY;
            int toDrain = Math.min(resource.getAmount(), be.steamTank.getFill());
            net.minecraftforge.fluids.FluidStack result = new net.minecraftforge.fluids.FluidStack(Fluids.WATER, toDrain);
            if (action.execute()) {
                be.steamTank.fill(be.steamTank.getFill() - toDrain);
                be.setChanged();
            }
            return result;
        }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack drain(int maxDrain, FluidAction action) {
            if (be.steamTank.getFill() <= 0) return net.minecraftforge.fluids.FluidStack.EMPTY;
            int toDrain = Math.min(maxDrain, be.steamTank.getFill());
            net.minecraftforge.fluids.FluidStack result = new net.minecraftforge.fluids.FluidStack(Fluids.WATER, toDrain);
            if (action.execute()) {
                be.steamTank.fill(be.steamTank.getFill() - toDrain);
                be.setChanged();
            }
            return result;
        }
    }
}
