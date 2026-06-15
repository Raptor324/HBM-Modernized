package com.hbm_m.block.entity.machines;

import com.hbm_m.api.fluids.IFluidConnectorMK2;
import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.block.entity.BaseMachineBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineTurbineMenu;
import com.hbm_m.interfaces.IEnergyModeHolder;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.item.liquids.FluidIdentifierItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

//? if forge {
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}

public class MachineTurbineBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2, IEnergyModeHolder {

    public static final int SLOT_FLUID_ID_IN    = 0;
    public static final int SLOT_FLUID_ID_OUT   = 1;
    public static final int SLOT_INPUT_IO_IN    = 2;
    public static final int SLOT_INPUT_IO_OUT   = 3;
    public static final int SLOT_BATTERY        = 4;
    public static final int SLOT_OUTPUT_IO_IN   = 5;
    public static final int SLOT_OUTPUT_IO_OUT  = 6;
    public static final int INVENTORY_SIZE      = 7;

    private static final int    DEFAULT_MAX_PROGRESS    = 200;
    private static final int    STEAM_CONSUMPTION_RATE  = 6_000;
    private static final int    INPUT_TANK_CAPACITY     = 64_000;
    private static final int    OUTPUT_TANK_CAPACITY    = 128_000;
    private static final double EFFICIENCY              = 0.85;

    private static final long ENERGY_PER_MB_STEAM          = 80L;
    private static final long ENERGY_PER_MB_HOTSTEAM        = 160L;
    private static final long ENERGY_PER_MB_SUPERHOTSTEAM   = 320L;
    private static final long ENERGY_PER_MB_ULTRAHOTSTEAM   = 640L;

    private final FluidTank[] tanks = new FluidTank[] {
            new FluidTank(INPUT_TANK_CAPACITY),
            new FluidTank(ModFluids.SPENTSTEAM.getSource(), OUTPUT_TANK_CAPACITY)
    };

    private int progress = 0;
    private int maxProgress = DEFAULT_MAX_PROGRESS;
    private boolean active = false;

    //? if forge {
    private LazyOptional<IFluidHandler> steamInputHandler = LazyOptional.empty();
    private LazyOptional<IFluidHandler> spentOutputHandler = LazyOptional.empty();
    //?}

    private static final long ENERGY_EXTRACT_RATE = 50_000L;

    public MachineTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURBINE_BE.get(), pos, state, INVENTORY_SIZE, 500_000L, 10_000L, ENERGY_EXTRACT_RATE);
    }

    @Override
    public int getCurrentMode() {
        return 2; // OUTPUT only, so the energy network treats this as a generator.
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineTurbineBlockEntity blockEntity) {
        if (!level.isClientSide) {
            blockEntity.serverTick();
        }
    }

    private void serverTick() {
        ensureNetworkInitialized();

        // Slot-driven operations (fluid type selection, IO container loading, battery)
        ItemStack[] slots = inventorySlotArray();
        boolean slotsChanged = false;
        if (tanks[0].setType(SLOT_FLUID_ID_IN, SLOT_FLUID_ID_OUT, slots))  slotsChanged = true;
        if (tanks[0].loadTank(SLOT_INPUT_IO_IN, SLOT_INPUT_IO_OUT, slots))  slotsChanged = true;
        if (tanks[1].unloadTank(SLOT_OUTPUT_IO_IN, SLOT_OUTPUT_IO_OUT, slots)) slotsChanged = true;
        if (slotsChanged) applySlotsArray(slots);

        chargeFromBatterySlot(SLOT_BATTERY);

        // MK2 fluid network: provide spent steam and subscribe to steam in all directions
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(dir);
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            if (!(neighborBe instanceof IFluidConnectorMK2)) continue;

            if (tanks[1].getFill() > 0) {
                tryProvide(tanks[1], level, neighborPos, dir);
            }

            Fluid tankType = tanks[0].getTankType();
            if (FluidTank.isFluidTypeExplicitlySet(tankType)) {
                trySubscribe(tankType, level, neighborPos, dir);
            } else {
                trySubscribe(ModFluids.STEAM.getSource(),         level, neighborPos, dir);
                trySubscribe(ModFluids.HOTSTEAM.getSource(),      level, neighborPos, dir);
                trySubscribe(ModFluids.SUPERHOTSTEAM.getSource(), level, neighborPos, dir);
                trySubscribe(ModFluids.ULTRAHOTSTEAM.getSource(), level, neighborPos, dir);
            }
        }

        boolean wasActive = active;
        active = processSteam();

        if (active && maxProgress > 0) {
            progress = (progress + 1) % maxProgress;
        } else {
            progress = 0;
        }

        if (wasActive != active || active) {
            setChanged();
            sendUpdateToClient();
        }
    }

    private boolean processSteam() {
        Fluid input = tanks[0].getTankType();
        long baseEnergyPerMb = getBaseEnergyPerMb(input);
        if (baseEnergyPerMb <= 0 || tanks[0].getFill() <= 0) {
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

        long energyPerMb = (long)(baseEnergyPerMb * EFFICIENCY);

        int maxByInput  = Math.min(STEAM_CONSUMPTION_RATE, tanks[0].getFill());
        int maxByOutput = tanks[1].getMaxFill() - tanks[1].getFill();
        long energySpace = getMaxEnergyStored() - getEnergyStored();
        int maxByEnergy = energyPerMb > 0 ? (int) Math.min(Integer.MAX_VALUE, energySpace / energyPerMb) : 0;

        int ops = Math.min(maxByInput, Math.min(maxByOutput, maxByEnergy));
        if (ops <= 0) return false;

        tanks[0].drainMb(ops);
        tanks[1].fillMb(ModFluids.SPENTSTEAM.getSource(), ops);
        setEnergyStored(getEnergyStored() + ops * energyPerMb);
        return true;
    }

    private long getBaseEnergyPerMb(Fluid steam) {
        if (VanillaFluidEquivalence.sameSubstance(steam, ModFluids.STEAM.getSource()))         return ENERGY_PER_MB_STEAM;
        if (VanillaFluidEquivalence.sameSubstance(steam, ModFluids.HOTSTEAM.getSource()))       return ENERGY_PER_MB_HOTSTEAM;
        if (VanillaFluidEquivalence.sameSubstance(steam, ModFluids.SUPERHOTSTEAM.getSource()))  return ENERGY_PER_MB_SUPERHOTSTEAM;
        if (VanillaFluidEquivalence.sameSubstance(steam, ModFluids.ULTRAHOTSTEAM.getSource()))  return ENERGY_PER_MB_ULTRAHOTSTEAM;
        return 0L;
    }

    // ── IFluidStandardTransceiverMK2 ─────────────────────────────────────────

    @Override public FluidTank[] getAllTanks()      { return tanks; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[]{ tanks[0] }; }
    @Override public FluidTank[] getSendingTanks() {
        return tanks[1].getFill() > 0 ? new FluidTank[]{ tanks[1] } : FluidTank.EMPTY_ARRAY;
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        if (fromDir == null || fluid == null || fluid == Fluids.EMPTY) return false;
        if (VanillaFluidEquivalence.sameSubstance(fluid, ModFluids.SPENTSTEAM.getSource())) return true;
        return getBaseEnergyPerMb(fluid) > 0;
    }

    // ── Inventory helpers ────────────────────────────────────────────────────

    private ItemStack[] inventorySlotArray() {
        ItemStack[] arr = new ItemStack[INVENTORY_SIZE];
        for (int i = 0; i < INVENTORY_SIZE; i++) arr[i] = inventory.getStackInSlot(i);
        return arr;
    }

    private void applySlotsArray(ItemStack[] arr) {
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inventory.setStackInSlot(i, arr[i] == null ? ItemStack.EMPTY : arr[i]);
        }
        setChanged();
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public FluidTank[] getTanks() { return tanks; }

    public int getPowerScaled(int scale) {
        long max = Math.max(getMaxEnergyStored(), 1L);
        return (int) Math.min(scale, getEnergyStored() * scale / max);
    }

    public int getProgressScaled(int scale) {
        return maxProgress > 0 ? progress * scale / maxProgress : 0;
    }

    public int getProgress()    { return progress; }
    public int getMaxProgress() { return maxProgress; }
    public boolean isActive()   { return active; }

    // ── NBT ─────────────────────────────────────────────────────────────────

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("progress", progress);
        tag.putInt("max_progress", maxProgress);
        tag.putBoolean("active", active);
        tanks[0].writeToNBT(tag, "input");
        tanks[1].writeToNBT(tag, "output");
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progress    = tag.getInt("progress");
        maxProgress = tag.getInt("max_progress");
        if (maxProgress <= 0) maxProgress = DEFAULT_MAX_PROGRESS;
        active = tag.getBoolean("active");
        tanks[0].readFromNBT(tag, "input");
        tanks[1].readFromNBT(tag, "output");
    }

    // ── Slot validation ──────────────────────────────────────────────────────

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_FLUID_ID_IN   -> stack.getItem() instanceof FluidIdentifierItem;
            case SLOT_FLUID_ID_OUT,
                 SLOT_INPUT_IO_OUT,
                 SLOT_OUTPUT_IO_OUT -> false;
            case SLOT_INPUT_IO_IN,
                 SLOT_OUTPUT_IO_IN  -> {
                //? if forge {
                yield stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
                //?}
                //? if fabric {
                /*yield net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.ITEM.find(stack, null) != null;
                *///?}
            }
            case SLOT_BATTERY -> stack.getItem() instanceof ItemCreativeBattery
                                  || isEnergyProviderItem(stack)
                                  || isEnergyReceiverItem(stack);
            default -> false;
        };
    }

    // ── Menu ────────────────────────────────────────────────────────────────

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.turbine");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineTurbineMenu.create(id, inventory, this);
    }

    // ── Forge fluid capabilities ─────────────────────────────────────────────

    //? if forge {
    @Override
    protected void setupFluidCapability() {
        steamInputHandler  = LazyOptional.of(() -> new SteamInputHandler(this));
        spentOutputHandler = LazyOptional.of(() -> new SpentOutputHandler(this));
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            if (side == Direction.UP) return spentOutputHandler.cast();
            return steamInputHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        steamInputHandler.invalidate();
        spentOutputHandler.invalidate();
    }

    private static class SteamInputHandler implements IFluidHandler {
        private final MachineTurbineBlockEntity be;
        SteamInputHandler(MachineTurbineBlockEntity be) { this.be = be; }

        @Override public int getTanks() { return 1; }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack getFluidInTank(int tank) {
            return new net.minecraftforge.fluids.FluidStack(be.tanks[0].getTankType(), be.tanks[0].getFill());
        }

        @Override public int getTankCapacity(int tank) { return be.tanks[0].getMaxFill(); }

        @Override
        public boolean isFluidValid(int tank, @NotNull net.minecraftforge.fluids.FluidStack stack) {
            return be.getBaseEnergyPerMb(stack.getFluid()) > 0;
        }

        @Override
        public int fill(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || be.getBaseEnergyPerMb(resource.getFluid()) <= 0) return 0;
            int space = be.tanks[0].getMaxFill() - be.tanks[0].getFill();
            int toFill = Math.min(space, resource.getAmount());
            if (toFill <= 0) return 0;
            if (action.execute()) be.tanks[0].fillMb(resource.getFluid(), toFill);
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

    private static class SpentOutputHandler implements IFluidHandler {
        private final MachineTurbineBlockEntity be;
        SpentOutputHandler(MachineTurbineBlockEntity be) { this.be = be; }

        @Override public int getTanks() { return 1; }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack getFluidInTank(int tank) {
            return new net.minecraftforge.fluids.FluidStack(be.tanks[1].getTankType(), be.tanks[1].getFill());
        }

        @Override public int getTankCapacity(int tank) { return be.tanks[1].getMaxFill(); }
        @Override public boolean isFluidValid(int tank, @NotNull net.minecraftforge.fluids.FluidStack stack) { return false; }
        @Override public int fill(net.minecraftforge.fluids.FluidStack resource, FluidAction action) { return 0; }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || be.tanks[1].getFill() <= 0) return net.minecraftforge.fluids.FluidStack.EMPTY;
            if (!VanillaFluidEquivalence.sameSubstance(resource.getFluid(), be.tanks[1].getTankType()))
                return net.minecraftforge.fluids.FluidStack.EMPTY;
            int toDrain = Math.min(resource.getAmount(), be.tanks[1].getFill());
            net.minecraftforge.fluids.FluidStack drained = new net.minecraftforge.fluids.FluidStack(be.tanks[1].getTankType(), toDrain);
            if (action.execute()) be.tanks[1].drainMb(toDrain);
            return drained;
        }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0 || be.tanks[1].getFill() <= 0) return net.minecraftforge.fluids.FluidStack.EMPTY;
            int toDrain = Math.min(maxDrain, be.tanks[1].getFill());
            net.minecraftforge.fluids.FluidStack drained = new net.minecraftforge.fluids.FluidStack(be.tanks[1].getTankType(), toDrain);
            if (action.execute()) be.tanks[1].drainMb(toDrain);
            return drained;
        }
    }
    //?}
}
