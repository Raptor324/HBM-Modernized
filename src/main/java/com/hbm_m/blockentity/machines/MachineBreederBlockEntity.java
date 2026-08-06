package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.inventory.menu.MachineBreederMenu;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.recipe.BreederRecipes;
import com.hbm_m.recipe.FluidBreederRecipes;

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
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

/**
 * Breeder - true multiblock port of the original 1.7.10 {@code MachineReactorBreeding}/
 * {@code TileEntityMachineReactorBreeding}. In the original, the reactor drew "neutron flux" from
 * an adjacent {@code TileEntityReactorResearch} (Research Reactor) which was never ported to this
 * codebase, so that mechanic is replaced here with this port's standard battery-slot/FE energy
 * system (see {@link #getPowerRequired()} - reuses the original's per-recipe "flux" balance numbers
 * 1:1 as an FE-per-tick draw). Item breeding recipes are ported from
 * {@code com.hbm.inventory.recipes.BreederRecipes} (see {@link BreederRecipes} for the exact
 * material substitutions, since the original's {@code ItemBreedingRod} meta-item system does not
 * exist in this port). The fluid tank additionally drives {@link FluidBreederRecipes}, ported from
 * {@code com.hbm.inventory.recipes.FluidBreederRecipes} (unused by the original tile entity, but
 * kept here since this port's tank/GUI already exist for it).
 */
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
    /** GIT original: progress += 0.0025F per tick at the minimum required flux -> 1.0F / 0.0025F = 400 ticks. */
    private static final int DEFAULT_DURATION = 400;
    /** Energy cost for one fluid-breeding conversion batch (not present in the original - see {@link FluidBreederRecipes}). */
    private static final long FLUID_ENERGY_COST = 2000;

    private final FluidTank tank = new FluidTank(TANK_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };
    private final LazyOptional<IFluidHandler> tankHandler = LazyOptional.of(() -> tank);

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
        entity.tickFluidBreeding();

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

    /**
     * In-place fluid-to-fluid conversion driven by {@link FluidBreederRecipes}. Batch-converts the
     * whole recipe amount at once (mirrors the original's threshold-based {@code canProcess()}
     * rather than a gradual drain, since a single-fluid {@link FluidTank} cannot hold input and
     * output simultaneously mid-conversion).
     */
    private void tickFluidBreeding() {
        FluidStack current = tank.getFluid();
        if (current.isEmpty()) return;

        FluidBreederRecipes.FluidBreederRecipe recipe = FluidBreederRecipes.getOutput(current.getFluid());
        if (recipe == null) return;
        if (current.getAmount() < recipe.amount) return;
        if (getEnergyStored() < FLUID_ENERGY_COST) return;

        tank.drain(recipe.amount, IFluidHandler.FluidAction.EXECUTE);
        tank.fill(recipe.output.copy(), IFluidHandler.FluidAction.EXECUTE);
        setEnergyStored(getEnergyStored() - FLUID_ENERGY_COST);
        setChanged();
        sendUpdateToClient();
    }

    private void chargeFromBattery() {
        ItemStack stack = inventory.getStackInSlot(SLOT_BATTERY);
        if (stack.isEmpty()) return;

        if (stack.getItem() instanceof ItemCreativeBattery) {
            setEnergyStored(getMaxEnergyStored());
            return;
        }

        stack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).ifPresent(provider -> {
            long needed = getMaxEnergyStored() - getEnergyStored();
            if (needed <= 0) return;
            long extracted = provider.extractEnergy(Math.min(needed, getReceiveSpeed()), false);
            if (extracted > 0) {
                setEnergyStored(getEnergyStored() + extracted);
                setChanged();
            }
        });

        if (!stack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).isPresent()) {
            stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(provider -> {
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
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        if (input.isEmpty()) return false;

        BreederRecipes.BreederRecipe recipe = BreederRecipes.getOutput(input);
        if (recipe == null) return false;

        if (getEnergyStored() < recipe.energyPerTick) return false;

        ItemStack output = inventory.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) return true;

        if (!ItemStack.isSameItemSameTags(output, recipe.output)) return false;
        return output.getCount() < output.getMaxStackSize();
    }

    private void processItem() {
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        BreederRecipes.BreederRecipe recipe = BreederRecipes.getOutput(input);
        if (recipe == null) return;

        ItemStack output = inventory.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, recipe.output.copy());
        } else if (ItemStack.isSameItemSameTags(output, recipe.output)) {
            output.grow(recipe.output.getCount());
        }

        input.shrink(1);
    }

    /** Reuses the current recipe's "flux" balance number 1:1 as an FE-per-tick draw (see class javadoc). */
    public int getPowerRequired() {
        BreederRecipes.BreederRecipe recipe = BreederRecipes.getOutput(inventory.getStackInSlot(SLOT_INPUT));
        return recipe != null ? recipe.energyPerTick : 0;
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

    public FluidTank getTank() {
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
            return stack.getCapability(ForgeCapabilities.ENERGY).isPresent()
                || stack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).isPresent()
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
        if (slot == SLOT_INPUT) {
            return BreederRecipes.getOutput(stack) != null;
        }
        return true;
    }

    @Override
    public AABB getRenderBoundingBox() {
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof com.hbm_m.block.machines.MachineBreederBlock block)) {
            return super.getRenderBoundingBox();
        }
        Direction facing = state.getValue(com.hbm_m.block.machines.MachineBreederBlock.FACING);
        return block.getStructureHelper().getRenderBoundingBox(worldPosition, facing, 0.0);
    }

    public boolean stillValid(Player player) {
        return !this.isRemoved() && player.distanceToSqr(this.getBlockPos().getCenter()) <= 64.0D;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineBreederMenu(containerId, playerInventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("tank", tank.writeToNBT(new CompoundTag()));
        tag.putInt("progress", progress);
        tag.putInt("duration", duration);
        tag.putBoolean("isOn", isOn);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("tank")) {
            tank.readFromNBT(tag.getCompound("tank"));
        }
        progress = tag.getInt("progress");
        duration = tag.contains("duration") ? tag.getInt("duration") : DEFAULT_DURATION;
        isOn = tag.getBoolean("isOn");
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return tankHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        tankHandler.invalidate();
    }
}
