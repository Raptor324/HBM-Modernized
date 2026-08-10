package com.hbm_m.blockentity.machines;

import com.hbm_m.api.fluids.FluidItemAccess;
import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineRefineryMenu;
import com.hbm_m.item.ModItems;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

//? if forge {
import org.jetbrains.annotations.Nullable;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?} else {
/*import org.jetbrains.annotations.Nullable;
*///?}
import org.jetbrains.annotations.NotNull;

/**
 * Refinery BlockEntity - processes crude oil into petroleum products.
 */
public class MachineRefineryBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    public static final int INVENTORY_SIZE = 13;
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_CANISTER_IN = 1;
    public static final int SLOT_CANISTER_OUT = 2;
    public static final int SLOT_HEAVY_IN = 3;
    public static final int SLOT_HEAVY_OUT = 4;
    public static final int SLOT_NAPHTHA_IN = 5;
    public static final int SLOT_NAPHTHA_OUT = 6;
    public static final int SLOT_LIGHT_IN = 7;
    public static final int SLOT_LIGHT_OUT = 8;
    public static final int SLOT_PETROLEUM_IN = 9;
    public static final int SLOT_PETROLEUM_OUT = 10;
    public static final int SLOT_SULFUR_OUT = 11;
    public static final int SLOT_FLUID_ID = 12;

    public static final int TANK_INPUT = 0;
    public static final int TANK_HEAVY = 1;
    public static final int TANK_NAPHTHA = 2;
    public static final int TANK_LIGHT = 3;
    public static final int TANK_PETROLEUM = 4;

    private static final int MAX_SULFUR = 10;
    private static final long ENERGY_CAPACITY = 1_000L;
    private static final long ENERGY_RECEIVE_RATE = 1_000L;
    private static final long ENERGY_PER_TICK = 5L;
    private static final int INPUT_CONSUMPTION_MB = 100;

    // Fractions from original RefineryRecipes (input is always 100 mB).
    private static final int OIL_FRAC_HEAVY = 50;
    private static final int OIL_FRAC_NAPH = 25;
    private static final int OIL_FRAC_LIGHT = 15;
    private static final int OIL_FRAC_PETRO = 10;

    private static final int CRACK_FRAC_NAPH = 40;
    private static final int CRACK_FRAC_LIGHT = 30;
    private static final int CRACK_FRAC_AROMA = 15;
    private static final int CRACK_FRAC_UNSAT = 15;

    private static final int OILDS_FRAC_HEAVY = 30;
    private static final int OILDS_FRAC_NAPH = 35;
    private static final int OILDS_FRAC_LIGHT = 20;
    private static final int OILDS_FRAC_UNSAT = 15;

    private static final int CRACKDS_FRAC_NAPH = 35;
    private static final int CRACKDS_FRAC_LIGHT = 35;
    private static final int CRACKDS_FRAC_AROMA = 15;
    private static final int CRACKDS_FRAC_UNSAT = 15;

    private final FluidTank[] tanks = new FluidTank[] {
        new FluidTank(64_000) {
            @Override
            public boolean isFluidValid(Fluid fluid) {
                return getPressure() == 0 && resolveRefineryRecipe(fluid) != null;
            }
        },
            new FluidTank(ModFluids.HEAVYOIL.getSource(), 24_000),
            new FluidTank(ModFluids.NAPHTHA.getSource(), 24_000),
            new FluidTank(ModFluids.LIGHTOIL.getSource(), 24_000),
        new FluidTank(ModFluids.PETROLEUM.getSource(), 24_000)
    };

    private int sulfurProgress = 0;
    private boolean isOn = false;

    //? if forge {
    private LazyOptional<IFluidHandler> fluidHandler = LazyOptional.empty();
    //?}

    public MachineRefineryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REFINERY_BE.get(), pos, state,
              INVENTORY_SIZE, ENERGY_CAPACITY, ENERGY_RECEIVE_RATE);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineRefineryBlockEntity be) {
        if (level.isClientSide()) return;

        boolean changed = false;

        be.chargeFromBatterySlot(SLOT_BATTERY);
        if (be.processFluidContainers()) {
            changed = true;
        }
        if (be.refine()) {
            changed = true;
        }

        if (changed) {
            be.setChanged();
            be.sendUpdateToClient();
        }

        be.ensureNetworkInitialized();
    }

    public FluidTank[] getTanks() {
        return tanks;
    }

    public FluidTank getTank(int index) {
        return (index >= 0 && index < tanks.length) ? tanks[index] : tanks[0];
    }

    // ═══════════════════════════ IFluidStandardTransceiverMK2 ════════════════════════════════
    // Делает контроллер видимым для MK2-сети жидкостных труб (как у химической установки):
    // без этого UniversalMachinePartBlockEntity не подписывает рефайнери ни как receiver, ни
    // как provider, и трубы не могут залить сырую нефть / забрать продукты переработки.

    @Override
    public FluidTank[] getReceivingTanks() {
        return new FluidTank[] { tanks[TANK_INPUT] };
    }

    @Override
    public FluidTank[] getSendingTanks() {
        return new FluidTank[] { tanks[TANK_HEAVY], tanks[TANK_NAPHTHA], tanks[TANK_LIGHT], tanks[TANK_PETROLEUM] };
    }

    @Override
    public FluidTank[] getAllTanks() {
        return tanks;
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    public boolean hasDisplayRecipe() {
        return resolveRefineryRecipe(getTank(TANK_INPUT).getTankType()) != null;
    }

    public Fluid[] getDisplayPipeFluids() {
        RefineryRecipe recipe = resolveRefineryRecipe(getTank(TANK_INPUT).getTankType());
        if (recipe == null) {
            return new Fluid[] {
                    ModFluids.NONE.getSource(),
                    ModFluids.NONE.getSource(),
                    ModFluids.NONE.getSource(),
                    ModFluids.NONE.getSource()
            };
        }
        return recipe.outputFluids();
    }

    public boolean canAcceptInputFluid(Fluid fluid) {
        return resolveRefineryRecipe(fluid) != null;
    }

    private boolean processFluidContainers() {
        ItemStack[] slots = getSlotsArray();
        boolean changed = false;

        if (tanks[TANK_INPUT].setType(SLOT_FLUID_ID, slots)) {
            changed = true;
        }
        if (tanks[TANK_INPUT].loadTank(SLOT_CANISTER_IN, SLOT_CANISTER_OUT, slots)) {
            changed = true;
        }
        if (tanks[TANK_HEAVY].unloadTank(SLOT_HEAVY_IN, SLOT_HEAVY_OUT, slots)) {
            changed = true;
        }
        if (tanks[TANK_NAPHTHA].unloadTank(SLOT_NAPHTHA_IN, SLOT_NAPHTHA_OUT, slots)) {
            changed = true;
        }
        if (tanks[TANK_LIGHT].unloadTank(SLOT_LIGHT_IN, SLOT_LIGHT_OUT, slots)) {
            changed = true;
        }
        if (tanks[TANK_PETROLEUM].unloadTank(SLOT_PETROLEUM_IN, SLOT_PETROLEUM_OUT, slots)) {
            changed = true;
        }

        if (changed) {
            applySlotsArray(slots);
        }

        return changed;
    }

    private boolean refine() {
        RefineryRecipe recipe = resolveRefineryRecipe(getTank(TANK_INPUT).getTankType());
        if (recipe == null) {
            isOn = false;
            return clearEmptyOutputTankTypes();
        }

        boolean changed = prepareOutputTypes(recipe.outputFluids());

        if (this.energy < ENERGY_PER_TICK || getTank(TANK_INPUT).getFill() < INPUT_CONSUMPTION_MB) {
            isOn = false;
            return changed;
        }

        if (!hasOutputSpace(recipe.outputFluids(), recipe.outputAmounts())) {
            isOn = false;
            return changed;
        }

        isOn = true;
        getTank(TANK_INPUT).setFill(getTank(TANK_INPUT).getFill() - INPUT_CONSUMPTION_MB);

        for (int i = 0; i < 4; i++) {
            FluidTank out = getTank(i + 1);
            out.fillMb(recipe.outputFluids()[i], recipe.outputAmounts()[i]);
        }

        sulfurProgress++;
        if (sulfurProgress >= MAX_SULFUR) {
            sulfurProgress -= MAX_SULFUR;
            changed |= emitByproduct(recipe.solidByproduct());
        }

        this.energy -= ENERGY_PER_TICK;
        return true;
    }

    private boolean clearEmptyOutputTankTypes() {
        boolean changed = false;
        for (int i = 1; i < 5; i++) {
            FluidTank out = getTank(i);
            if (out.getFill() <= 0 && !VanillaFluidEquivalence.sameSubstance(out.getTankType(), ModFluids.NONE.getSource())) {
                out.setTankType(ModFluids.NONE.getSource());
                changed = true;
            }
        }
        return changed;
    }

    private boolean hasOutputSpace(Fluid[] outputFluids, int[] amounts) {
        for (int i = 0; i < 4; i++) {
            FluidTank out = getTank(i + 1);
            if (out.getFill() > 0 && !VanillaFluidEquivalence.sameSubstance(out.getTankType(), outputFluids[i])) {
                return false;
            }
            if (out.getFill() + amounts[i] > out.getMaxFill()) {
                return false;
            }
        }
        return true;
    }

    private boolean prepareOutputTypes(Fluid[] outputFluids) {
        boolean changed = false;
        for (int i = 0; i < 4; i++) {
            FluidTank out = getTank(i + 1);
            if (out.getFill() <= 0 && !VanillaFluidEquivalence.sameSubstance(out.getTankType(), outputFluids[i])) {
                out.setTankType(outputFluids[i]);
                changed = true;
            }
        }
        return changed;
    }

    private boolean emitByproduct(ItemStack byproduct) {
        if (byproduct.isEmpty()) return false;

        ItemStack slotStack = inventory.getStackInSlot(SLOT_SULFUR_OUT);
        if (slotStack.isEmpty()) {
            inventory.setStackInSlot(SLOT_SULFUR_OUT, byproduct.copy());
            return true;
        }

        if (slotStack.is(byproduct.getItem())
            && slotStack.getCount() + byproduct.getCount() <= slotStack.getMaxStackSize()) {
            slotStack.grow(byproduct.getCount());
            inventory.setStackInSlot(SLOT_SULFUR_OUT, slotStack);
            return true;
        }

        return false;
    }

    private RefineryRecipe resolveRefineryRecipe(Fluid input) {
        if (input == null) {
            return null;
        }

        if (VanillaFluidEquivalence.sameSubstance(input, ModFluids.HOTOIL.getSource())
            || VanillaFluidEquivalence.sameSubstance(input, ModFluids.OIL_BASE.getSource())) {
            return new RefineryRecipe(
                    new Fluid[] {
                            ModFluids.HEAVYOIL.getSource(),
                            ModFluids.NAPHTHA.getSource(),
                            ModFluids.LIGHTOIL.getSource(),
                            ModFluids.PETROLEUM.getSource()
                    },
                new int[] {OIL_FRAC_HEAVY, OIL_FRAC_NAPH, OIL_FRAC_LIGHT, OIL_FRAC_PETRO},
                    new ItemStack(ModItems.SULFUR.get())
            );
        }

        if (VanillaFluidEquivalence.sameSubstance(input, ModFluids.HOTCRACKOIL.getSource())
            || VanillaFluidEquivalence.sameSubstance(input, ModFluids.CRACKOIL.getSource())) {
            // Original byproduct is oil_tar (CRACK), but oil_tar system is not ported yet.
            return new RefineryRecipe(
                new Fluid[] {
                    ModFluids.NAPHTHA_CRACK.getSource(),
                    ModFluids.LIGHTOIL_CRACK.getSource(),
                    ModFluids.AROMATICS.getSource(),
                    ModFluids.UNSATURATEDS.getSource()
                },
                new int[] {CRACK_FRAC_NAPH, CRACK_FRAC_LIGHT, CRACK_FRAC_AROMA, CRACK_FRAC_UNSAT},
                ItemStack.EMPTY
            );
        }

        if (VanillaFluidEquivalence.sameSubstance(input, ModFluids.HOTOIL_DS.getSource())
            || VanillaFluidEquivalence.sameSubstance(input, ModFluids.OIL_DS.getSource())) {
            // Original byproduct is oil_tar (PARAFFIN), but oil_tar system is not ported yet.
            return new RefineryRecipe(
                new Fluid[] {
                    ModFluids.HEAVYOIL.getSource(),
                    ModFluids.NAPHTHA_DS.getSource(),
                    ModFluids.LIGHTOIL_DS.getSource(),
                    ModFluids.UNSATURATEDS.getSource()
                },
                new int[] {OILDS_FRAC_HEAVY, OILDS_FRAC_NAPH, OILDS_FRAC_LIGHT, OILDS_FRAC_UNSAT},
                ItemStack.EMPTY
            );
        }

        if (VanillaFluidEquivalence.sameSubstance(input, ModFluids.HOTCRACKOIL_DS.getSource())
            || VanillaFluidEquivalence.sameSubstance(input, ModFluids.CRACKOIL_DS.getSource())) {
            // Original byproduct is oil_tar (PARAFFIN), but oil_tar system is not ported yet.
            return new RefineryRecipe(
                new Fluid[] {
                    ModFluids.NAPHTHA_DS.getSource(),
                    ModFluids.LIGHTOIL_DS.getSource(),
                    ModFluids.AROMATICS.getSource(),
                    ModFluids.UNSATURATEDS.getSource()
                },
                new int[] {CRACKDS_FRAC_NAPH, CRACKDS_FRAC_LIGHT, CRACKDS_FRAC_AROMA, CRACKDS_FRAC_UNSAT},
                ItemStack.EMPTY
            );
        }

        return null;
    }

    private ItemStack[] getSlotsArray() {
        ItemStack[] slots = new ItemStack[INVENTORY_SIZE];
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            slots[i] = inventory.getStackInSlot(i);
        }
        return slots;
    }

    private void applySlotsArray(ItemStack[] slots) {
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inventory.setStackInSlot(i, slots[i]);
        }
    }

    //? if < 1.21.1 {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].writeToNBT(tag, "tank_" + i);
        }
        tag.putInt("sulfurProgress", sulfurProgress);
        tag.putBoolean("isOn", isOn);
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.saveAdditional(tag, registries);
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].writeToNBT(tag, "tank_" + i);
        }
        tag.putInt("sulfurProgress", sulfurProgress);
        tag.putBoolean("isOn", isOn);
    
    }
    *///?}

    //? if < 1.21.1 {
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].readFromNBT(tag, "tank_" + i);
        }
        sulfurProgress = tag.getInt("sulfurProgress");
        isOn = tag.getBoolean("isOn");
    }
    //?} else {
    /*@Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.loadAdditional(tag, registries);
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].readFromNBT(tag, "tank_" + i);
        }
        sulfurProgress = tag.getInt("sulfurProgress");
        isOn = tag.getBoolean("isOn");
    
    }
    *///?}

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.refinery");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return MachineRefineryMenu.create(containerId, playerInventory, this);
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_BATTERY -> isEnergyProviderItem(stack);
            case SLOT_CANISTER_IN, SLOT_HEAVY_IN, SLOT_NAPHTHA_IN, SLOT_LIGHT_IN, SLOT_PETROLEUM_IN ->
                    FluidItemAccess.hasFluidHandler(stack);
            case SLOT_FLUID_ID -> stack.getItem() instanceof FluidIdentifierItem;
            default -> false;
        };
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.refinery");
    }

    //? if forge {
    @Override
    protected void setupFluidCapability() {
        fluidHandler = LazyOptional.of(() -> new RefineryFluidHandler(this));
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
    //?}

    public boolean isOn() {
        return isOn;
    }

    public int getSulfurProgress() {
        return sulfurProgress;
    }

    private record RefineryRecipe(Fluid[] outputFluids, int[] outputAmounts, ItemStack solidByproduct) {}
}
