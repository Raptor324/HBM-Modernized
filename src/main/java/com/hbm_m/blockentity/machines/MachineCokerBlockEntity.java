package com.hbm_m.blockentity.machines;

import com.hbm_m.api.fluids.IFluidConnectorMK2;
import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IHeatSource;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineCokerMenu;
import com.hbm_m.item.liquids.FluidIdentifierItem;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.CokerRecipe;

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

/**
 * Coker - Port von {@code TileEntityMachineCoker} (1.7.10 Original). Keine Strom-Beteiligung (das
 * Original implementiert kein {@code IEnergyReceiverMK2}) - laeuft rein ueber Waerme von einer
 * {@link IHeatSource} darunter (identisches Pull-/Decay-Schema wie {@code
 * MachineBoilerBlockEntity}, nur mit {@code DIFFUSION=0.25}). Rezepte sind data-driven
 * ({@link CokerRecipe}, Fluid Tank 0 -> Item + optionales Byproduct-Fluid in Tank 1),
 * {@code burn = heat/100} Fortschritt pro Tick - 1:1 aus dem Original.
 * <p>
 * SCOPE-Entscheidung: Pollution (SOOT beim Laufen) entfaellt (fehlende Infrastruktur, wie bei
 * allen anderen Maschinen dieser Session dokumentiert).
 */
public class MachineCokerBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    public static final int SLOT_FLUID_ID = 0;
    public static final int SLOT_OUTPUT   = 1;
    public static final int INVENTORY_SIZE = 2;

    private static final int TANK0_CAPACITY = 16_000;
    private static final int TANK1_CAPACITY = 8_000;
    private static final int PROCESS_TIME   = 20_000;
    private static final int MAX_HEAT       = 100_000;
    private static final double DIFFUSION   = 0.25D;

    private final FluidTank tank0 = new FluidTank(TANK0_CAPACITY);
    private final FluidTank tank1 = new FluidTank(TANK1_CAPACITY);

    private int heat;
    private int progress;
    private boolean wasOn;

    public MachineCokerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COKER_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCokerBlockEntity be) {
        if (!level.isClientSide) {
            be.serverTick(level, pos);
        }
    }

    private void serverTick(Level level, BlockPos pos) {
        pullOrDecayHeat(level, pos);

        ItemStack[] slots = inventorySlotArray();
        if (tank0.setType(SLOT_FLUID_ID, slots)) applySlotsArray(slots);

        if (level.getGameTime() % 20 == 0) {
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = pos.relative(dir);
                BlockEntity neighborBe = level.getBlockEntity(neighborPos);
                if (!(neighborBe instanceof IFluidConnectorMK2)) continue;
                trySubscribe(tank0.getTankType(), level, neighborPos, dir);
            }
        }

        wasOn = false;

        if (canProcess()) {
            int burn = heat / 100;
            if (burn > 0) {
                wasOn = true;
                progress += burn;
                heat -= burn;

                if (progress >= PROCESS_TIME) {
                    progress -= PROCESS_TIME;

                    CokerRecipe recipe = findCokerRecipe();
                    if (recipe != null) {
                        ItemStack out = recipe.getOutput();
                        if (out != null && !out.isEmpty()) {
                            ItemStack current = inventory.getStackInSlot(SLOT_OUTPUT);
                            if (current.isEmpty()) {
                                inventory.setStackInSlot(SLOT_OUTPUT, out.copy());
                            } else {
                                current.grow(out.getCount());
                            }
                        }
                        if (recipe.getByproductFluid() != null) {
                            tank1.fillMb(recipe.getByproductFluid(), recipe.getByproductMb());
                        }
                        tank0.drainMb(recipe.getInputMb());
                    }
                }
            }
        }

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            if (!(neighborBe instanceof IFluidConnectorMK2)) continue;
            if (tank1.getFill() > 0) {
                tryProvide(tank1, level, neighborPos, dir);
            }
        }

        setChanged();
        sendUpdateToClient();
    }

    private void pullOrDecayHeat(Level level, BlockPos pos) {
        BlockEntity below = level.getBlockEntity(pos.below());
        if (below instanceof IHeatSource source) {
            int diff = source.getHeatStored() - heat;
            if (diff > 0) {
                int pulled = Math.min((int) Math.ceil(diff * DIFFUSION), MAX_HEAT - heat);
                if (pulled > 0) {
                    source.useUpHeat(pulled);
                    heat += pulled;
                    return;
                }
            }
        }
        heat = Math.max(heat - Math.max(heat / 1000, 1), 0);
    }

    private boolean canProcess() {
        CokerRecipe recipe = findCokerRecipe();
        if (recipe == null) return false;

        if (recipe.getByproductFluid() != null) {
            tank1.setTankType(recipe.getByproductFluid());
        }

        if (tank0.getFluidAmountMb() < recipe.getInputMb()) return false;
        if (recipe.getByproductFluid() != null && recipe.getByproductMb() + tank1.getFluidAmountMb() > tank1.getCapacityMb()) return false;

        ItemStack out = recipe.getOutput();
        if (out != null && !out.isEmpty()) {
            ItemStack current = inventory.getStackInSlot(SLOT_OUTPUT);
            if (!current.isEmpty()) {
                if (!com.hbm_m.platform.PlatformHooks.isSameItemSameTags(current, out)) return false;
                if (current.getCount() + out.getCount() > out.getMaxStackSize()) return false;
            }
        }

        return true;
    }


    /** Data-driven поиск CokerRecipe по типу жидкости бака 0 (заменяет статический CokerRecipes.get). */
    @org.jetbrains.annotations.Nullable
    private CokerRecipe findCokerRecipe() {
        if (level == null) return null;
        for (CokerRecipe recipe : RecipeHooks.getAllRecipes(level, CokerRecipe.Type.INSTANCE)) {
            if (recipe.matchesFluidType(tank0.getTankType())) return recipe;
        }
        return null;
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

    // ── IFluidStandardTransceiverMK2 ─────────────────────────────────────────

    @Override public FluidTank[] getAllTanks()      { return new FluidTank[]{ tank0, tank1 }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[]{ tank0 }; }
    @Override public FluidTank[] getSendingTanks() {
        return tank1.getFill() > 0 ? new FluidTank[]{ tank1 } : FluidTank.EMPTY_ARRAY;
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        if (fromDir == null || fluid == null || fluid == Fluids.EMPTY) return false;
        if (level == null) return false;
        for (CokerRecipe recipe : RecipeHooks.getAllRecipes(level, CokerRecipe.Type.INSTANCE)) {
            if (recipe.matchesFluidType(fluid)) return true;
        }
        return false;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public FluidTank getTank0() { return tank0; }
    public FluidTank getTank1() { return tank1; }
    public int getHeat()        { return heat; }
    public int getMaxHeat()     { return MAX_HEAT; }
    public int getProgress()    { return progress; }
    public int getMaxProgress() { return PROCESS_TIME; }
    public boolean isActive()   { return wasOn; }

    // ── NBT ─────────────────────────────────────────────────────────────────

    //? if < 1.21.1 {
    @Override
    public void saveAdditional(CompoundTag tag) {
        tag.putInt("heat", heat);
        tag.putInt("progress", progress);
        tag.putBoolean("wasOn", wasOn);
        tank0.writeToNBT(tag, "tank0");
        tank1.writeToNBT(tag, "tank1");
    }
    //?} else {
    /*@Override
    public void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
    tag.putInt("heat", heat);
    tag.putInt("progress", progress);
    tag.putBoolean("wasOn", wasOn);
    tank0.writeToNBT(tag, "tank0");
    tank1.writeToNBT(tag, "tank1");
    }
    *///?}

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        heat = tag.getInt("heat");
        progress = tag.getInt("progress");
        wasOn = tag.getBoolean("wasOn");
        tank0.readFromNBT(tag, "tank0");
        tank1.readFromNBT(tag, "tank1");
    }

    // ── Slot validation ──────────────────────────────────────────────────────

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_FLUID_ID -> stack.getItem() instanceof FluidIdentifierItem;
            default -> false;
        };
    }

    // ── Menu ────────────────────────────────────────────────────────────────

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.coker");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineCokerMenu.create(id, inventory, this);
    }
}
