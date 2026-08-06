package com.hbm_m.blockentity.machines;

import com.hbm_m.api.fluids.IFluidConnectorMK2;
import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Coolable;
import com.hbm_m.inventory.fluid.trait.FT_Coolable.CoolingType;
import com.hbm_m.inventory.menu.MachineLargeTurbineMenu;
import com.hbm_m.interfaces.IEnergyModeHolder;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.item.liquids.FluidIdentifierItem;

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

/**
 * Large Turbine - Port von {@code TileEntityMachineLargeTurbine} (1.7.10 Original, dort selbst
 * bereits als "(LEGACY)" markiert - abgeloest durch die bereits vorhandene {@code
 * MachineIndustrialTurbineBlockEntity}, aber im Original weiterhin voll funktionsfaehig, daher
 * hier ebenfalls 1:1 nutzbar portiert). Gleiche Slot-Aufteilung wie das ebenfalls vorhandene
 * {@link MachineTurbineBlockEntity} (kleine Turbine), aber ueber die {@code FT_Coolable}
 * Fluid-Eigenschaft (CoolingType.TURBINE) statt eines festen Tier-Mapping - 1:1 wie {@code
 * MachineSteamEngineBlockEntity}, nur mit den großen Tankgroessen/Leistungswerten des Originals
 * und der zusaetzlichen "maximal 20% Tankinhalt pro Tick"-Drossel ({@code cap}).
 * <p>
 * Das Original dekrementiert seinen Energiepuffer zusaetzlich jeden Tick um 5% (bevor neue
 * Energie erzeugt wird) - dieses Verhalten wird hier 1:1 uebernommen.
 */
public class MachineLargeTurbineBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2, IEnergyModeHolder {

    public static final int SLOT_FLUID_ID_IN    = 0;
    public static final int SLOT_FLUID_ID_OUT   = 1;
    public static final int SLOT_INPUT_IO_IN    = 2;
    public static final int SLOT_INPUT_IO_OUT   = 3;
    public static final int SLOT_BATTERY        = 4;
    public static final int SLOT_OUTPUT_IO_IN   = 5;
    public static final int SLOT_OUTPUT_IO_OUT  = 6;
    public static final int INVENTORY_SIZE      = 7;

    private static final int    INPUT_TANK_CAPACITY  = 512_000;
    private static final int    OUTPUT_TANK_CAPACITY  = 10_240_000;
    private static final long   MAX_POWER             = 100_000_000L;
    private static final double EFFICIENCY             = 1.0D;
    private static final long   ENERGY_EXTRACT_RATE    = 500_000L;

    private final FluidTank[] tanks = new FluidTank[] {
            new FluidTank(INPUT_TANK_CAPACITY),
            new FluidTank(ModFluids.SPENTSTEAM.getSource(), OUTPUT_TANK_CAPACITY)
    };

    private boolean active = false;

    //? if forge {
    private LazyOptional<IFluidHandler> steamInputHandler = LazyOptional.empty();
    private LazyOptional<IFluidHandler> spentOutputHandler = LazyOptional.empty();
    //?}

    public MachineLargeTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LARGE_TURBINE_BE.get(), pos, state, INVENTORY_SIZE, MAX_POWER, 0L, ENERGY_EXTRACT_RATE);
    }

    @Override
    public int getCurrentMode() {
        return 2; // OUTPUT only, so the energy network treats this as a generator.
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineLargeTurbineBlockEntity blockEntity) {
        if (!level.isClientSide) {
            blockEntity.serverTick();
        }
    }

    private void serverTick() {
        ensureNetworkInitialized();

        ItemStack[] slots = inventorySlotArray();
        boolean slotsChanged = false;
        if (tanks[0].setType(SLOT_FLUID_ID_IN, SLOT_FLUID_ID_OUT, slots))    slotsChanged = true;
        if (tanks[0].loadTank(SLOT_INPUT_IO_IN, SLOT_INPUT_IO_OUT, slots))   slotsChanged = true;
        if (tanks[1].unloadTank(SLOT_OUTPUT_IO_IN, SLOT_OUTPUT_IO_OUT, slots)) slotsChanged = true;
        if (slotsChanged) applySlotsArray(slots);

        chargeItemInSlot(SLOT_BATTERY);

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(dir);
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            if (!(neighborBe instanceof IFluidConnectorMK2)) continue;

            if (tanks[1].getFill() > 0) {
                tryProvide(tanks[1], level, neighborPos, dir);
            }
            trySubscribe(tanks[0].getTankType(), level, neighborPos, dir);
        }

        // 1:1 aus dem Original: der Energiepuffer verliert jeden Tick 5%, bevor neue Energie erzeugt wird.
        setEnergyStored((long) (getEnergyStored() * 0.95D));

        boolean wasActive = active;
        active = processSteam();

        if (wasActive != active || active) {
            setChanged();
            sendUpdateToClient();
        }
    }

    private boolean processSteam() {
        FT_Coolable trait = FluidType.getTrait(tanks[0].getStoredFluid(), FT_Coolable.class);
        if (trait == null || trait.amountReq <= 0 || trait.amountProduced <= 0) {
            tanks[1].setTankType(ModFluids.NONE.getSource());
            return false;
        }

        double eff = trait.getEfficiency(CoolingType.TURBINE) * EFFICIENCY;
        if (eff <= 0) {
            tanks[1].setTankType(ModFluids.NONE.getSource());
            return false;
        }

        tanks[1].setTankType(trait.coolsTo);

        int inputOps  = tanks[0].getFluidAmountMb() / trait.amountReq;
        int outputOps = (tanks[1].getCapacityMb() - tanks[1].getFluidAmountMb()) / trait.amountProduced;
        int cap       = (int) Math.ceil(tanks[0].getFluidAmountMb() / (double) trait.amountReq / 5D);
        int ops       = Math.min(inputOps, Math.min(outputOps, cap));
        if (ops <= 0) return false;

        tanks[0].drainMb(ops * trait.amountReq);
        tanks[1].fillMb(trait.coolsTo, ops * trait.amountProduced);

        long output = (long) (ops * trait.heatEnergy * eff);
        setEnergyStored(Math.min(getMaxEnergyStored(), getEnergyStored() + output));
        return true;
    }

    // ── IFluidStandardTransceiverMK2 ─────────────────────────────────────────

    @Override public FluidTank[] getAllTanks()       { return tanks; }
    @Override public FluidTank[] getReceivingTanks()  { return new FluidTank[]{ tanks[0] }; }
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
        return FluidType.getTrait(fluid, FT_Coolable.class) != null;
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

    public boolean isActive() { return active; }

    // ── NBT ─────────────────────────────────────────────────────────────────

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("active", active);
        tanks[0].writeToNBT(tag, "input");
        tanks[1].writeToNBT(tag, "output");
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
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
        return Component.translatable("container.hbm_m.machine_large_turbine");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineLargeTurbineMenu.create(id, inventory, this);
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
        private final MachineLargeTurbineBlockEntity be;
        SteamInputHandler(MachineLargeTurbineBlockEntity be) { this.be = be; }

        @Override public int getTanks() { return 1; }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack getFluidInTank(int tank) {
            return new net.minecraftforge.fluids.FluidStack(be.tanks[0].getTankType(), be.tanks[0].getFill());
        }

        @Override public int getTankCapacity(int tank) { return be.tanks[0].getMaxFill(); }

        @Override
        public boolean isFluidValid(int tank, @NotNull net.minecraftforge.fluids.FluidStack stack) {
            return FluidType.getTrait(stack.getFluid(), FT_Coolable.class) != null;
        }

        @Override
        public int fill(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || FluidType.getTrait(resource.getFluid(), FT_Coolable.class) == null) return 0;
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
        private final MachineLargeTurbineBlockEntity be;
        SpentOutputHandler(MachineLargeTurbineBlockEntity be) { this.be = be; }

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
