package com.hbm_m.blockentity.machines;

import com.hbm_m.platform.PlatformHooks;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineArcFurnaceMenu;
import com.hbm_m.recipe.ArcFurnaceRecipe;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.ModRecipes;

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
import net.minecraft.world.phys.AABB;

/**
 * Arc Furnace - generisch rezeptgesteuerte Einzelblock-Maschine, vereinfachte Portierung der
 * Kernlogik aus {@code TileEntityMachineArcFurnaceLarge}/{@code ArcFurnaceRecipes} (1.7.10-
 * Original). Das Original ist ein Multiblock mit Elektroden-Hitze-Animation - hier bewusst NICHT
 * uebernommen (Konsistenz mit Ore Slopper/Combination Oven in dieser Session): keine Multiblock-
 * Struktur, kein Energieverbrauch, keine Hitze-/Animation-Mechanik.
 * <p>
 * Anders als beim Original (und dem Combination Oven) ist der primaere Output meist FLUESSIG
 * (geschmolzenes Metall) statt fest: 1 Item-Eingang -> optionaler Item-Ausgang UND/ODER bis zu 2
 * Fluid-Ausgaenge, siehe {@link ArcFurnaceRecipe}. Rezepte kommen ausschliesslich aus dem
 * generischen Vanilla-Rezeptsystem ({@link ArcFurnaceRecipe} / {@link ModRecipes#ARC_FURNACE_TYPE});
 * es sind bewusst KEINE Rezepte im Java-Code hartkodiert.
 */
public class MachineArcFurnaceBlockEntity extends BaseMachineBlockEntity {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    private static final int SLOT_COUNT = 2;

    private static final int TANK_CAPACITY_MB = 4000;

    private int progressTicks = 0;
    private int currentDuration = 1;

    private final FluidTank tank1 = new FluidTank(TANK_CAPACITY_MB) {
        @Override
        public void onContentsChanged() {
            setChanged();
            sendUpdateToClient();
        }
    };

    private final FluidTank tank2 = new FluidTank(TANK_CAPACITY_MB) {
        @Override
        public void onContentsChanged() {
            setChanged();
            sendUpdateToClient();
        }
    };

    public MachineArcFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARC_FURNACE_BE.get(), pos, state, SLOT_COUNT, 0L, 0L, 0L);
    }

    //? if forge {
    @Override
    protected void setupFluidCapability() {
        // Экспонируем главный бак (tank1) для внешних насосов/труб через базовый fluidHandlerOpt.
        setFluidHandler(tank1);
    }
    //?}

    // ==================== TICK ====================

    public static void tick(Level level, BlockPos pos, BlockState state, MachineArcFurnaceBlockEntity be) {
        if (!level.isClientSide()) {
            be.serverTick(level);
        }
    }

    private void serverTick(Level level) {
        ArcFurnaceRecipe recipe = findRecipe(level);
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
    private ArcFurnaceRecipe findRecipe(Level level) {
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        if (input.isEmpty()) return null;

        List<ArcFurnaceRecipe> recipes = RecipeHooks.getAllRecipes(level, ArcFurnaceRecipe.Type.INSTANCE);
        for (ArcFurnaceRecipe recipe : recipes) {
            if (recipe.matchesInput(input)) {
                return recipe;
            }
        }
        return null;
    }

    private boolean canProcess(ArcFurnaceRecipe recipe) {
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        if (input.getCount() < recipe.getInputCount()) return false;

        if (recipe.hasItemOutput()) {
            ItemStack outSlot = inventory.getStackInSlot(SLOT_OUTPUT);
            ItemStack out = recipe.getOutput();
            if (!outSlot.isEmpty()) {
                if (!PlatformHooks.isSameItemSameTags(outSlot, out)) return false;
                if (outSlot.getCount() + out.getCount() > outSlot.getMaxStackSize()) return false;
            }
        }

        if (recipe.hasFluidOutput1()) {
            if (!fitsInTank(tank1, recipe.getFluid1(), recipe.getFluidAmount1())) return false;
        }
        if (recipe.hasFluidOutput2()) {
            if (!fitsInTank(tank2, recipe.getFluid2(), recipe.getFluidAmount2())) return false;
        }

        return true;
    }

    private boolean fitsInTank(FluidTank tank, net.minecraft.world.level.material.Fluid fluid, int amount) {
        if (!tank.isEmpty() && tank.getStoredFluid() != fluid) return false;
        return tank.getSpaceMb() >= amount;
    }

    private void completeCycle(ArcFurnaceRecipe recipe) {
        if (recipe.hasItemOutput()) {
            ItemStack out = recipe.getOutput();
            ItemStack outSlot = inventory.getStackInSlot(SLOT_OUTPUT);
            if (outSlot.isEmpty()) {
                inventory.setStackInSlot(SLOT_OUTPUT, out);
            } else {
                outSlot.grow(out.getCount());
            }
        }

        if (recipe.hasFluidOutput1()) {
            tank1.fillMb(recipe.getFluid1(), recipe.getFluidAmount1());
        }
        if (recipe.hasFluidOutput2()) {
            tank2.fillMb(recipe.getFluid2(), recipe.getFluidAmount2());
        }

        inventory.getStackInSlot(SLOT_INPUT).shrink(recipe.getInputCount());
    }

    // ==================== NBT ====================

    //? if < 1.21.1 {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("progress", progressTicks);
        tag.putInt("duration", currentDuration);
        tank1.writeToNBT(tag, "tank1");
        tank2.writeToNBT(tag, "tank2");
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.saveAdditional(tag, registries);
        tag.putInt("progress", progressTicks);
        tag.putInt("duration", currentDuration);
        tank1.writeToNBT(tag, "tank1");
        tank2.writeToNBT(tag, "tank2");
    
    }
    *///?}

    //? if < 1.21.1 {
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progressTicks = tag.getInt("progress");
        currentDuration = tag.contains("duration") ? Math.max(1, tag.getInt("duration")) : 1;
        tank1.readFromNBT(tag, "tank1");
        tank2.readFromNBT(tag, "tank2");
    }
    //?} else {
    /*@Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.loadAdditional(tag, registries);
        progressTicks = tag.getInt("progress");
        currentDuration = tag.contains("duration") ? Math.max(1, tag.getInt("duration")) : 1;
        tank1.readFromNBT(tag, "tank1");
        tank2.readFromNBT(tag, "tank2");
    
    }
    *///?}

    // ==================== GETTERS / MENU ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.arc_furnace");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_INPUT) {
            if (level == null) return true;
            List<ArcFurnaceRecipe> recipes = RecipeHooks.getAllRecipes(level, ArcFurnaceRecipe.Type.INSTANCE);
            for (ArcFurnaceRecipe recipe : recipes) {
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
        return new MachineArcFurnaceMenu(containerId, playerInventory, this);
    }

    @Override
    public AABB getRenderBoundingBox() {
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof com.hbm_m.block.machines.MachineArcFurnaceBlock block)) {
            return new AABB(worldPosition.offset(-1, 0, -1), worldPosition.offset(2, 2, 2));
        }
        Direction facing = state.getValue(com.hbm_m.block.machines.MachineArcFurnaceBlock.FACING);
        return block.getStructureHelper().getRenderBoundingBox(worldPosition, facing, 0.0);
    }

    public FluidTank getTank1() {
        return tank1;
    }

    public FluidTank getTank2() {
        return tank2;
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

    /** Fuer den Hot/Cold-Elektroden-Zustandswechsel im Renderer (siehe MachineArcFurnaceRenderer). */
    public boolean isActive() {
        return progressTicks > 0;
    }
}
