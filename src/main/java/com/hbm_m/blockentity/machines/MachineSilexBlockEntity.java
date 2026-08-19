package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineSilexMenu;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.SilexRecipe;
import com.hbm_m.recipe.SilexRecipe.WeightedOutput;

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

/**
 * SILEX: vereinfachte Portierung von {@code TileEntitySILEX} (1.7.10 Original) - Uran/Plutonium/Americium-
 * Anreicherung per Peroxid-Laser-Kaskade.
 * <p>
 * Vereinfachung ggue. Original (siehe Aufgabenstellung): das Original ist eine mehrstufige Kaskade mit
 * Laser-Wellenlaengen-Gating ({@code EnumWavelengths}/{@code hasLaser}, extern von einem Laserblock gesetzt)
 * und einer vom Item-Input entkoppelten Fluid-"Ladeleiste" ({@code currentFill}/{@code maxFill}). Diese Portierung
 * ist eine einstufige Version: Item + Peroxid-Fluid werden direkt verbraucht, keine Laser-Gating-Mechanik,
 * kein Warteschlangen-System (Original: Slots 5-10) - stattdessen ein einzelner Ausgabeslot mit
 * Item-Stack-Akkumulation, analog zu allen anderen einfachen Item-Ausgabemaschinen in diesem Port.
 * Die Rezeptliste {@link SilexRecipe} ist ein Direktport der Gewichtsverteilungen aus {@code SILEXRecipes}
 * (U -> U235/U238, Pu-Mix -> Pu239/Pu240, Am-Mix -> Am241/Am242).
 */
public class MachineSilexBlockEntity extends BaseMachineBlockEntity implements IFluidStandardReceiverMK2 {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_BATTERY = 2;
    private static final int SLOT_COUNT = 3;

    private static final long CAPACITY = 100_000L;
    private static final long MAX_RECEIVE = 1_000L;
    private static final long ENERGY_PER_TICK = 200L;

    private static final int TANK_CAPACITY_MB = 16_000;

    private final FluidTank tank;
    private int progress = 0;
    private boolean active = false;

    public MachineSilexBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SILEX_BE.get(), pos, state, SLOT_COUNT, CAPACITY, MAX_RECEIVE, 0L);
        tank = new FluidTank(ModFluids.PEROXIDE.getSource(), TANK_CAPACITY_MB) {
            @Override
            public boolean isFluidValid(Fluid fluid) {
                return fluid == ModFluids.PEROXIDE.getSource();
            }
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineSilexBlockEntity be) {
        if (level.isClientSide) return;

        be.chargeFromBatterySlot(SLOT_BATTERY);

        if (level.getGameTime() % 10 == 0) {
            for (Direction dir : Direction.values()) {
                be.trySubscribe(be.tank.getTankType(), level, pos.relative(dir), dir);
            }
        }

        boolean dirty = false;
        boolean wasActive = be.active;
        SilexRecipe recipe = be.currentRecipe();
        be.active = be.canProcess(recipe);

        if (be.active != wasActive) dirty = true;

        if (be.active) {
            be.setEnergyStored(be.getEnergyStored() - ENERGY_PER_TICK);
            be.progress++;
            dirty = true;
            if (be.progress >= recipe.getDuration()) {
                be.progress = 0;
                be.completeCycle(recipe, level);
            }
        } else if (be.progress > 0) {
            be.progress = 0;
            dirty = true;
        }

        if (dirty) {
            be.setChanged();
            be.sendUpdateToClient();
        }
    }

    private SilexRecipe currentRecipe() {
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        if (input.isEmpty()) return null;
        return findRecipe(input);
    }

    /** Data-driven поиск SilexRecipe по входному стаку (заменяет статический SilexRecipes.get/has). */
    private SilexRecipe findRecipe(ItemStack input) {
        Level level = getLevel();
        if (level == null || input.isEmpty()) return null;
        for (SilexRecipe recipe : RecipeHooks.getAllRecipes(level, SilexRecipe.Type.INSTANCE)) {
            if (recipe.matches(input)) return recipe;
        }
        return null;
    }

    private boolean canProcess(SilexRecipe recipe) {
        if (recipe == null) return false;
        if (getEnergyStored() < ENERGY_PER_TICK) return false;
        if (tank.getFill() < recipe.getPeroxideMb()) return false;
        return hasOutputSpace(recipe);
    }

    private boolean hasOutputSpace(SilexRecipe recipe) {
        ItemStack existing = inventory.getStackInSlot(SLOT_OUTPUT);
        if (existing.isEmpty()) return true;
        for (WeightedOutput out : recipe.getOutputs()) {
            if (!com.hbm_m.platform.PlatformHooks.isSameItemSameTags(existing, out.stack())) return false;
        }
        return existing.getCount() < existing.getMaxStackSize();
    }

    private void completeCycle(SilexRecipe recipe, Level level) {
        inventory.extractItem(SLOT_INPUT, 1, false);
        tank.drainMb(recipe.getPeroxideMb());

        int totalWeight = Math.max(1, recipe.getTotalWeight());
        int roll = level.getRandom().nextInt(totalWeight);
        int weight = 0;
        for (WeightedOutput out : recipe.getOutputs()) {
            weight += out.weight();
            if (roll < weight) {
                insertOutput(out.stack().copy());
                break;
            }
        }
    }

    private void insertOutput(ItemStack stack) {
        ItemStack existing = inventory.getStackInSlot(SLOT_OUTPUT);
        if (existing.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, stack);
        } else if (com.hbm_m.platform.PlatformHooks.isSameItemSameTags(existing, stack)) {
            existing.grow(stack.getCount());
        }
    }

    // ==================== GUI (generische GuiInfoScreen-Balken) ====================

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        SilexRecipe recipe = currentRecipe();
        return recipe != null ? recipe.getDuration() : 100;
    }

    public int getProgressScaled(int scale) {
        int max = getMaxProgress();
        return max <= 0 ? 0 : progress * scale / max;
    }

    public boolean isActive() {
        return active;
    }

    public FluidTank getTank() {
        return tank;
    }

    // ==================== IFluidUserMK2 / MK2-Netz ====================

    @Override
    public FluidTank[] getAllTanks() {
        return new FluidTank[] { tank };
    }

    @Override
    public FluidTank[] getReceivingTanks() {
        return new FluidTank[] { tank };
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null && fluid == ModFluids.PEROXIDE.getSource();
    }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("progress", progress);
        tag.putBoolean("active", active);
        tank.writeToNBT(tag, "tank");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        progress = tag.getInt("progress");
        active = tag.getBoolean("active");
        tank.readFromNBT(tag, "tank");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.silex");
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_INPUT) {
            return findRecipe(stack) != null;
        }
        if (slot == SLOT_BATTERY) {
            return isEnergyProviderItem(stack);
        }
        return false;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineSilexMenu.create(id, inventory, this);
    }
}
