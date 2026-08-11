package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineArcWelderMenu;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.ArcWelderRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class MachineArcWelderBlockEntity extends BaseMachineBlockEntity {

    // ─── Slot map ─────────────────────────────────────────────────────────────
    public static final int SLOTS      = 8;
    public static final int SLOT_IN1   = 0, SLOT_IN2 = 1, SLOT_IN3 = 2;
    public static final int SLOT_OUT   = 3;
    public static final int SLOT_BAT   = 4;
    public static final int SLOT_FLUID = 5;   // Fluid-ID item (identifies accepted fluid)
    public static final int SLOT_UPG1  = 6, SLOT_UPG2 = 7;

    // ─── Energy constants ─────────────────────────────────────────────────────
    public static final long MAX_ENERGY  = 100_000L;
    public static final long CONSUMPTION =   2_000L; // HE per tick while active

    // ─── Processing state ─────────────────────────────────────────────────────
    public int  progress    = 0;
    public int  processTime = 200;      // default; will be set by recipe
    public long consumption = CONSUMPTION;

    // ─── Fluid tank ───────────────────────────────────────────────────────────
    /** Welding fluid (e.g. water, coolant). Capacity 24,000 mb as in the original. */
    public final FluidTank tank = new FluidTank(24_000);

    // ─── Constructor ──────────────────────────────────────────────────────────

    public MachineArcWelderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARC_WELDER_BE.get(), pos, state, SLOTS, MAX_ENERGY, CONSUMPTION);
    }

    /** Exposes the machine inventory for SlotItemHandler in the menu. */
    public com.hbm_m.platform.ModItemStackHandler getItemHandler() { return inventory; }

    // ─── Tick ─────────────────────────────────────────────────────────────────

    public static void tick(Level level, BlockPos pos, BlockState state, MachineArcWelderBlockEntity be) {
        if (level.isClientSide) return;

        // Charge from battery slot (extract energy from battery item in SLOT_BAT)
        com.hbm_m.api.energy.ItemEnergyAccess.getHbmProvider(
                be.inventory.getStackInSlot(SLOT_BAT)).ifPresent(provider -> {
            long want = be.capacity - be.energy;
            long got  = provider.extractEnergy(want, false);
            if (got > 0) be.energy += got;
        });

        // Data-driven поиск рецепта: итерируем ArcWelderRecipe из RecipeManager (заменяет статику ArcWelderRecipes).
        ArcWelderRecipe recipe = findArcWelderRecipe(level, be);
        if (recipe != null) {
            be.processTime  = recipe.getDuration();
            be.consumption  = recipe.getConsumption();
        }
        // matchesFluid на ArcWelderRecipe заменяет прежний recipe.fluid.satisfiedBy(tank) (теперь FluidStack-based).
        boolean hasRecipe  = recipe != null && recipe.matchesFluid(be.tank);
        boolean canProcess = hasRecipe && be.energy >= be.consumption && be.canOutput();

        if (canProcess) {
            be.progress++;
            be.energy = Math.max(0, be.energy - be.consumption);

            if (be.progress >= be.processTime) {
                be.progress = 0;
                // be.processRecipe(...) — прежний заглушка оставлена без изменений (статический BE тоже не потреблял).
                be.setChanged();
            }
        } else {
            be.progress = 0;
        }

        level.sendBlockUpdated(pos, state, state, 3);
    }

    public boolean canOutput() {
        ItemStack out = inventory.getStackInSlot(SLOT_OUT);
        // null-output is always ok; filled output must match and have room
        return out.isEmpty();
    }

    /** Data-driven поиск ArcWelderRecipe по 3 входным слотам (заменяет статический ArcWelderRecipes.getRecipe). */
    @org.jetbrains.annotations.Nullable
    private static ArcWelderRecipe findArcWelderRecipe(Level level, MachineArcWelderBlockEntity be) {
        ItemStack s0 = be.inventory.getStackInSlot(SLOT_IN1);
        ItemStack s1 = be.inventory.getStackInSlot(SLOT_IN2);
        ItemStack s2 = be.inventory.getStackInSlot(SLOT_IN3);
        for (ArcWelderRecipe recipe : RecipeHooks.getAllRecipes(level, ArcWelderRecipe.Type.INSTANCE)) {
            if (recipe.matchesInputs(s0, s1, s2)) return recipe;
        }
        return null;
    }

    // ─── Progress helpers (for GUI) ───────────────────────────────────────────

    public int getProgressScaled(int scale) {
        return processTime > 0 ? progress * scale / processTime : 0;
    }

    public int getProgress()    { return progress;    }
    public int getMaxProgress() { return processTime; }

    // ─── Slot validation ──────────────────────────────────────────────────────

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot != SLOT_OUT; // output is extraction-only
    }

    // ─── MenuProvider ─────────────────────────────────────────────────────────

    @Override
    protected Component getDefaultName() { return Component.translatable("block.hbm_m.arc_welder"); }
    @Override
    public Component getDisplayName()    { return getDefaultName(); }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return MachineArcWelderMenu.create(id, inv, this);
    }

    // ─── NBT ──────────────────────────────────────────────────────────────────

    //? if < 1.21.1 {
    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("progress",    progress);
        tag.putInt("processTime", processTime);
        tank.writeToNBT(tag, "tank");
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.saveAdditional(tag, registries);
        tag.putInt("progress",    progress);
        tag.putInt("processTime", processTime);
        tank.writeToNBT(tag, "tank");
    
    }
    *///?}

    //? if < 1.21.1 {
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progress    = tag.getInt("progress");
        processTime = tag.getInt("processTime");
        if (processTime <= 0) processTime = 200;
        tank.readFromNBT(tag, "tank");
    }
    //?} else {
    /*@Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.loadAdditional(tag, registries);
        progress    = tag.getInt("progress");
        processTime = tag.getInt("processTime");
        if (processTime <= 0) processTime = 200;
        tank.readFromNBT(tag, "tank");
    
    }
    *///?}
}
