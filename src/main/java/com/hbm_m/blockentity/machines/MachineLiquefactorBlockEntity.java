package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardSenderMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineLiquefactorMenu;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.LiquefactorRecipe;

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
 * Liquefactor: Portierung der Kernlogik aus {@code TileEntityMachineLiquefactor} (1.7.10
 * Original). Verfluessigt 1 Item pro Zyklus (Slot 0) zu einem Fluid (Tank), ueber die data-driven
 * Rezeptliste {@link LiquefactorRecipe} (JSON; Port von {@code LiquefactionRecipes}, siehe Generator
 * fuer ausgelassene Rezepte). Upgrade-Slots (Speed/Power) aus dem Original wurden NICHT
 * uebernommen - Konsistenz mit dem "Funktion vor Politur"-Ansatz dieser Session; die Basiswerte
 * (250 HE/Tick, 100 Ticks/Zyklus) entsprechen dem Original ohne Upgrades.
 */
public class MachineLiquefactorBlockEntity extends BaseMachineBlockEntity implements IFluidStandardSenderMK2 {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_BATTERY = 1;
    private static final int SLOT_COUNT = 2;

    private static final long MAX_POWER = 100_000L;
    private static final long USAGE_PER_TICK = 250L;
    private static final int PROCESS_TIME = 100;
    private static final int TANK_CAPACITY_MB = 2000;

    private final FluidTank tank = new FluidTank(TANK_CAPACITY_MB);
    private int progress = 0;

    public MachineLiquefactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LIQUEFACTOR_BE.get(), pos, state, SLOT_COUNT, MAX_POWER, MAX_POWER, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineLiquefactorBlockEntity be) {
        if (level.isClientSide) return;

        be.chargeFromBatterySlot(SLOT_BATTERY);

        if (be.canProcess()) {
            be.process();
        } else {
            be.progress = 0;
        }

        for (Direction dir : Direction.values()) {
            be.tryProvide(be.tank, level, pos.relative(dir), dir);
        }

        be.setChanged();
        be.sendUpdateToClient();
    }

    private boolean canProcess() {
        if (getEnergyStored() < USAGE_PER_TICK) return false;
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        if (input.isEmpty()) return false;

        LiquefactorRecipe recipe = findLiquefactorRecipe(input);
        if (recipe == null) return false;
        if (!tank.isEmpty() && tank.getTankType() != recipe.getOutput().getFluid()) return false;
        return tank.getFill() + recipe.getOutputAmountMb() <= tank.getMaxFill();
    }

    private void process() {
        setEnergyStored(getEnergyStored() - USAGE_PER_TICK);
        progress++;

        if (progress >= PROCESS_TIME) {
            LiquefactorRecipe recipe = findLiquefactorRecipe(inventory.getStackInSlot(SLOT_INPUT));
            if (recipe != null) {
                tank.conform(recipe.getOutput().getFluid());
                tank.fillMb(recipe.getOutput().getFluid(), recipe.getOutputAmountMb());
                inventory.getStackInSlot(SLOT_INPUT).shrink(1);
            }
            progress = 0;
        }
    }

    /**
     * Data-driven поиск LiquefactorRecipe по входному предмету
     * (заменяет статический LiquefactorRecipes.getOutput).
     */
    @Nullable
    private LiquefactorRecipe findLiquefactorRecipe(ItemStack input) {
        if (level == null) return null;
        for (LiquefactorRecipe recipe : RecipeHooks.getAllRecipes(level, LiquefactorRecipe.Type.INSTANCE)) {
            if (recipe.matchesInput(input)) return recipe;
        }
        return null;
    }

    // ==================== GUI ====================

    public FluidTank getTank() {
        return tank;
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return PROCESS_TIME;
    }

    public int getProgressScaled(int scale) {
        return progress * scale / PROCESS_TIME;
    }

    // ==================== IFluidStandardSenderMK2 / MK2-Netz ====================

    @Override
    public FluidTank[] getAllTanks() {
        return new FluidTank[] { tank };
    }

    @Override
    public FluidTank[] getSendingTanks() {
        return new FluidTank[] { tank };
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null && tank.getTankType() == fluid;
    }

    // ==================== NBT ====================

    //? if < 1.21.1 {
    @Override
    public void saveAdditional(CompoundTag tag) {
        tank.writeToNBT(tag, "tank");
        tag.putInt("progress", progress);
    }
    //?} else {
    /*@Override
    public void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
    tank.writeToNBT(tag, "tank");
    tag.putInt("progress", progress);
    }
    *///?}

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        tank.readFromNBT(tag, "tank");
        progress = tag.getInt("progress");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.liquefactor");
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        // level==null (нет доступа к RecipeManager) — разрешаем; серверная валидация идёт дальше в canProcess.
        if (slot == SLOT_INPUT) return level == null || findLiquefactorRecipe(stack) != null;
        if (slot == SLOT_BATTERY) return isEnergyProviderItem(stack);
        return false;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineLiquefactorMenu.create(id, inventory, this);
    }
}
