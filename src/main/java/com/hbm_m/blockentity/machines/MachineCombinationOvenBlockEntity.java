package com.hbm_m.blockentity.machines;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineCombinationOvenMenu;
import com.hbm_m.recipe.CombinationOvenRecipe;
import com.hbm_m.recipe.ModRecipes;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Combination Oven - generisch rezeptgesteuerte Einzelblock-Maschine, Portierung der Kernlogik
 * aus {@code TileEntityFurnaceCombination}/{@code CombinationRecipes} (1.7.10-Original): 1 Item-
 * Eingang + N mB einer bestimmten Fluessigkeit -> 1 Item-Ausgang, feste Prozesszeit.
 * <p>
 * Rezepte kommen aus dem generischen Vanilla-Rezeptsystem ({@link CombinationOvenRecipe} /
 * {@link ModRecipes#COMBINATION_OVEN_TYPE}), analog zu {@code ChemicalPlantRecipe}/{@code PressRecipe}.
 * Es sind bewusst KEINE Rezepte im Java-Code hartkodiert - die werden separat als Daten-JSONs
 * nachgereicht.
 * <p>
 * Bewusst NICHT uebernommen (Konsistenz mit Ore Slopper in dieser Session): Energieverbrauch,
 * Hitze-Mechanik (heat/maxHeat/diffusion aus dem Original), "entzuendet Entitaeten obendrauf"-
 * Effekt, Rauch/Fluid-Ausstoss in die Umgebung, Animation/Renderer (statisches OBJ-Modell reicht).
 * Der Fluid-Tank ist generisch (kein fest zugewiesener Fluessigkeitstyp) - welche Fluessigkeit
 * gebraucht wird, entscheidet einzig das jeweils passende Rezept.
 */
public class MachineCombinationOvenBlockEntity extends BaseMachineBlockEntity {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    private static final int SLOT_COUNT = 2;

    private static final int TANK_CAPACITY_MB = 4000;

    private int progressTicks = 0;
    private int currentDuration = 1;

    private final FluidTank tank = new FluidTank(TANK_CAPACITY_MB) {
        @Override
        public void onContentsChanged() {
            setChanged();
            sendUpdateToClient();
        }
    };

    public MachineCombinationOvenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMBINATION_OVEN_BE.get(), pos, state, SLOT_COUNT, 0L, 0L, 0L);
    }

    //? if forge {
    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
            return tank.getCapability().cast();
        }
        return super.getCapability(cap, side);
    }
    //?}

    // ==================== TICK ====================

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCombinationOvenBlockEntity be) {
        if (!level.isClientSide()) {
            be.serverTick(level);
        }
    }

    private void serverTick(Level level) {
        CombinationOvenRecipe recipe = findRecipe(level);
        boolean dirty = false;

        if (recipe != null && canProcess(recipe)) {
            currentDuration = recipe.getDuration();
            progressTicks++;
            dirty = true;
            if (progressTicks >= currentDuration) {
                progressTicks = 0;
                completeCycle(recipe);
            }
        } else if (progressTicks > 0) {
            progressTicks = 0;
            dirty = true;
        }

        if (dirty) {
            setChanged();
            sendUpdateToClient();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Nullable
    private CombinationOvenRecipe findRecipe(Level level) {
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        if (input.isEmpty()) return null;

        RecipeType type = (RecipeType) CombinationOvenRecipe.Type.INSTANCE;
        List<CombinationOvenRecipe> recipes = level.getRecipeManager().getAllRecipesFor(type);
        for (CombinationOvenRecipe recipe : recipes) {
            if (recipe.matchesInput(input) && recipe.matchesFluid(tank.getStoredFluid())) {
                return recipe;
            }
        }
        return null;
    }

    private boolean canProcess(CombinationOvenRecipe recipe) {
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        if (input.getCount() < recipe.getInputCount()) return false;

        if (recipe.getFluidAmount() > 0) {
            if (tank.getFluidAmountMb() < recipe.getFluidAmount()) return false;
            if (tank.getStoredFluid() != recipe.getFluid()) return false;
        }

        ItemStack outSlot = inventory.getStackInSlot(SLOT_OUTPUT);
        ItemStack out = recipe.getOutput();
        if (!outSlot.isEmpty()) {
            if (!ItemStack.isSameItemSameTags(outSlot, out)) return false;
            if (outSlot.getCount() + out.getCount() > outSlot.getMaxStackSize()) return false;
        }

        return true;
    }

    private void completeCycle(CombinationOvenRecipe recipe) {
        ItemStack out = recipe.getOutput();
        ItemStack outSlot = inventory.getStackInSlot(SLOT_OUTPUT);
        if (outSlot.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, out);
        } else {
            outSlot.grow(out.getCount());
        }

        if (recipe.getFluidAmount() > 0) {
            tank.drainMb(recipe.getFluidAmount());
        }

        inventory.getStackInSlot(SLOT_INPUT).shrink(recipe.getInputCount());
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("progress", progressTicks);
        tag.putInt("duration", currentDuration);
        tank.writeToNBT(tag, "tank");
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progressTicks = tag.getInt("progress");
        currentDuration = tag.contains("duration") ? Math.max(1, tag.getInt("duration")) : 1;
        tank.readFromNBT(tag, "tank");
    }

    // ==================== GETTERS / MENU ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.combination_oven");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_INPUT) {
            if (level == null) return true;
            List<CombinationOvenRecipe> recipes = level.getRecipeManager().getAllRecipesFor(CombinationOvenRecipe.Type.INSTANCE);
            for (CombinationOvenRecipe recipe : recipes) {
                if (recipe.matchesInput(stack)) return true;
            }
            return false;
        }
        if (slot == SLOT_OUTPUT) {
            return false; // Nur Entnahme - wird von der Maschine befuellt.
        }
        return false;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineCombinationOvenMenu(containerId, playerInventory, this);
    }

    public FluidTank getTank() {
        return tank;
    }

    public int getProgress() {
        return progressTicks;
    }

    public int getMaxProgress() {
        return currentDuration;
    }

    public int getProgressScaled(int scale) {
        return currentDuration <= 0 ? 0 : (progressTicks * scale) / currentDuration;
    }
}
