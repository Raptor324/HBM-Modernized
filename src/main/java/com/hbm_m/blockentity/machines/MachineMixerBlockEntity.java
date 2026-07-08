package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineMixerMenu;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.recipe.MixerRecipes;
import com.hbm_m.recipe.MixerRecipes.MixerRecipe;

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
import net.minecraft.world.level.material.Fluid;
//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}

/**
 * Industrial Mixer BlockEntity.
 *
 * <p>Combines two input fluids into a single output fluid over time, consuming energy.
 * Direct port of the relevant subset of {@code com.hbm.tileentity.machine.TileEntityMachineMixer}
 * from 1.7.10, adapted to the {@link FluidTank} + {@link IFluidStandardTransceiverMK2}
 * conventions established by {@code MachineRefineryBlockEntity} / {@code MachineChemicalPlantBlockEntity}.</p>
 */
public class MachineMixerBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    private static final int SLOT_COUNT = 1;
    private static final int SLOT_BATTERY = 0;

    public static final int TANK_INPUT_A = 0;
    public static final int TANK_INPUT_B = 1;
    public static final int TANK_OUTPUT = 2;

    private static final int TANK_CAPACITY = 12_000;

    private static final long ENERGY_CAPACITY = 100_000L;
    private static final long ENERGY_RECEIVE_RATE = 1_000L;

    private static final int DEFAULT_MAX_PROGRESS = 100;

    private final FluidTank[] tanks = new FluidTank[] {
        new FluidTank(TANK_CAPACITY) {
            @Override
            public void onContentsChanged() {
                setChanged();
            }
        },
        new FluidTank(TANK_CAPACITY) {
            @Override
            public void onContentsChanged() {
                setChanged();
            }
        },
        new FluidTank(TANK_CAPACITY) {
            @Override
            public void onContentsChanged() {
                setChanged();
            }
        }
    };

    private int progress = 0;
    private int maxProgress = DEFAULT_MAX_PROGRESS;
    private boolean active = false;

    //? if forge {
    private LazyOptional<IFluidHandler> fluidHandler = LazyOptional.empty();
    //?}

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> (int) (getEnergyStored() & 0xFFFFFFFFL);
                case 3 -> (int) ((getEnergyStored() >> 32) & 0xFFFFFFFFL);
                case 4 -> (int) (getMaxEnergyStored() & 0xFFFFFFFFL);
                case 5 -> (int) ((getMaxEnergyStored() >> 32) & 0xFFFFFFFFL);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return 6;
        }
    };

    public MachineMixerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MIXER_BE.get(), pos, state, SLOT_COUNT, ENERGY_CAPACITY, ENERGY_RECEIVE_RATE);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineMixerBlockEntity blockEntity) {
        if (!level.isClientSide) {
            blockEntity.serverTick();
        }
    }

    private void serverTick() {
        ensureNetworkInitialized();
        chargeFromBattery();

        boolean changed = mix();

        if (changed) {
            setChanged();
            sendUpdateToClient();
        }
    }

    /**
     * Core mixing logic: looks up a recipe matching the two input tanks, and if there is enough
     * fluid, energy and output space, progresses the recipe over {@code duration} ticks before
     * draining the inputs and filling the output.
     */
    private boolean mix() {
        MixerRecipe recipe = MixerRecipes.findRecipe(tanks[TANK_INPUT_A].getTankType(), tanks[TANK_INPUT_B].getTankType());

        if (recipe == null) {
            if (active || progress != 0) {
                active = false;
                progress = 0;
                return true;
            }
            return false;
        }

        boolean directOrder = recipe.isDirectOrder(tanks[TANK_INPUT_A].getTankType());
        FluidTank tankA = directOrder ? tanks[TANK_INPUT_A] : tanks[TANK_INPUT_B];
        FluidTank tankB = directOrder ? tanks[TANK_INPUT_B] : tanks[TANK_INPUT_A];
        int amountA = directOrder ? recipe.amountA() : recipe.amountB();
        int amountB = directOrder ? recipe.amountB() : recipe.amountA();

        boolean hasFluids = tankA.getFill() >= amountA && tankB.getFill() >= amountB;
        boolean hasEnergy = this.energy >= recipe.energyPerTick();
        boolean hasOutputSpace = canFillOutput(recipe.output(), recipe.outputAmount());

        maxProgress = Math.max(1, recipe.duration());

        if (!hasFluids || !hasEnergy || !hasOutputSpace) {
            if (active) {
                active = false;
                return true;
            }
            return false;
        }

        active = true;
        boolean changed = false;

        if (progress < maxProgress) {
            progress++;
            this.energy -= recipe.energyPerTick();
            changed = true;
        }

        if (progress >= maxProgress) {
            tankA.drainMb(amountA);
            tankB.drainMb(amountB);
            tanks[TANK_OUTPUT].fillMb(recipe.output(), recipe.outputAmount());
            progress = 0;
            changed = true;
        }

        return changed;
    }

    private boolean canFillOutput(Fluid output, int amount) {
        FluidTank out = tanks[TANK_OUTPUT];
        Fluid configured = out.getTankType();
        if (out.getFill() > 0 && !VanillaFluidEquivalence.sameSubstance(configured, output)) {
            return false;
        }
        return out.getFill() + amount <= out.getMaxFill();
    }

    private void chargeFromBattery() {
        ItemStack stack = inventory.getStackInSlot(SLOT_BATTERY);
        if (!stack.isEmpty() && stack.getItem() instanceof ItemCreativeBattery) {
            setEnergyStored(getMaxEnergyStored());
            return;
        }
        chargeFromBatterySlot(SLOT_BATTERY);
    }

    public FluidTank[] getTanks() {
        return tanks;
    }

    public FluidTank getTank(int index) {
        return (index >= 0 && index < tanks.length) ? tanks[index] : tanks[0];
    }

    public ContainerData getContainerData() {
        return data;
    }

    // ═══════════════════════════ IFluidStandardTransceiverMK2 ════════════════════════════════

    @Override
    public FluidTank[] getReceivingTanks() {
        return new FluidTank[] { tanks[TANK_INPUT_A], tanks[TANK_INPUT_B] };
    }

    @Override
    public FluidTank[] getSendingTanks() {
        return new FluidTank[] { tanks[TANK_OUTPUT] };
    }

    @Override
    public FluidTank[] getAllTanks() {
        return tanks;
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    public int getProgressScaled(int scale) {
        if (maxProgress <= 0) {
            return 0;
        }
        return progress * scale / maxProgress;
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("progress", progress);
        tag.putInt("max_progress", maxProgress);
        tag.putBoolean("active", active);
        for (int i = 0; i < tanks.length; i++) {
            tag.put("tank_" + i, tanks[i].writeNBT(new CompoundTag()));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progress = tag.getInt("progress");
        maxProgress = tag.getInt("max_progress");
        if (maxProgress <= 0) {
            maxProgress = DEFAULT_MAX_PROGRESS;
        }
        active = tag.getBoolean("active");
        for (int i = 0; i < tanks.length; i++) {
            String key = "tank_" + i;
            if (tag.contains(key)) {
                tanks[i].readNBT(tag.getCompound(key));
            }
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.mixer");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_BATTERY -> isEnergyProviderItem(stack) || stack.getItem() instanceof ItemCreativeBattery;
            default -> false;
        };
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineMixerMenu.create(id, inventory, this);
    }

    //? if forge {
    @Override
    protected void setupFluidCapability() {
        fluidHandler = LazyOptional.of(() -> new MixerFluidHandler(this));
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidHandler.invalidate();
    }

    /**
     * Combined Forge fluid handler exposing the two input tanks (fillable) and the output tank
     * (drainable) through a single capability, mirroring {@code MachineRefineryBlockEntity}'s
     * approach for chemical plant pipes.
     */
    private static class MixerFluidHandler implements IFluidHandler {
        private final MachineMixerBlockEntity be;

        MixerFluidHandler(MachineMixerBlockEntity be) {
            this.be = be;
        }

        @Override
        public int getTanks() {
            return be.tanks.length;
        }

        @Override
        public net.minecraftforge.fluids.FluidStack getFluidInTank(int tank) {
            FluidTank t = be.getTank(tank);
            if (t.isEmpty()) return net.minecraftforge.fluids.FluidStack.EMPTY;
            return new net.minecraftforge.fluids.FluidStack(t.getStoredFluid(), t.getFluidAmountMb());
        }

        @Override
        public int getTankCapacity(int tank) {
            return TANK_CAPACITY;
        }

        @Override
        public boolean isFluidValid(int tank, net.minecraftforge.fluids.FluidStack stack) {
            return tank == TANK_INPUT_A || tank == TANK_INPUT_B;
        }

        @Override
        public int fill(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return 0;
            for (int i = TANK_INPUT_A; i <= TANK_INPUT_B; i++) {
                FluidTank tank = be.tanks[i];
                Fluid configured = tank.getConfiguredFluid();
                if (configured != ModFluids.NONE.getSource()
                        && !VanillaFluidEquivalence.sameSubstance(configured, resource.getFluid())
                        && tank.getFill() > 0) {
                    continue;
                }
                int space = tank.getCapacityMb() - tank.getFluidAmountMb();
                if (space <= 0) continue;
                int toFill = Math.min(resource.getAmount(), space);
                if (action.execute()) {
                    tank.fillMb(resource.getFluid(), toFill);
                    be.setChanged();
                }
                return toFill;
            }
            return 0;
        }

        @Override
        public net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            FluidTank out = be.tanks[TANK_OUTPUT];
            if (out.isEmpty()) return net.minecraftforge.fluids.FluidStack.EMPTY;
            if (!VanillaFluidEquivalence.sameSubstance(out.getStoredFluid(), resource.getFluid())) {
                return net.minecraftforge.fluids.FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @Override
        public net.minecraftforge.fluids.FluidStack drain(int maxDrain, FluidAction action) {
            FluidTank out = be.tanks[TANK_OUTPUT];
            if (out.isEmpty() || maxDrain <= 0) return net.minecraftforge.fluids.FluidStack.EMPTY;
            int toDrain = Math.min(maxDrain, out.getFluidAmountMb());
            Fluid fluid = out.getStoredFluid();
            if (action.execute()) {
                out.drainMb(toDrain);
                be.setChanged();
            }
            Fluid normalized = VanillaFluidEquivalence.forVanillaContainerFill(fluid);
            return new net.minecraftforge.fluids.FluidStack(normalized, toDrain);
        }
    }
    //?}
}
