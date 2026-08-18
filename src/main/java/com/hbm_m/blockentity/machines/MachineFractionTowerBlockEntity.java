package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineFractionTowerMenu;
import com.hbm_m.recipe.FractionTowerRecipe;

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
 * Fraktionsturm: Portierung der Kernrezeptlogik aus {@code TileEntityMachineFractionTower} (1.7.10 Original).
 * Spaltet alle 10 Ticks 100mB eines schweren Oel-Fluids (Tank 0) in zwei leichtere Fraktionen (Tank 1/2) auf,
 * ueber die data-driven Rezeptliste {@link FractionTowerRecipe} (Port von {@code FractionRecipes}).
 * <p>
 * Vereinfachung ggue. Original: das 1.7.10-Original ist ein 4-hohes Multiblock, das Fluid zwischen gestapelten
 * Tuermen weiterreicht (Oel steigt auf, Fraktionen sinken ab). Diese Portierung ist bewusst ein Einzelblock -
 * Ein-/Ausgabe erfolgt direkt ueber das MK2-Rohrnetz an allen 6 Seiten, analog zum bereits portierten
 * {@link MachineGasCentrifugeBlockEntity}.
 */
public class MachineFractionTowerBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    private static final int TANK_CAPACITY_MB = 4000;
    private static final int FRACTIONATE_INTERVAL = 10;
    private static final int INPUT_PER_CYCLE_MB = 100;

    private final FluidTank[] tanks = new FluidTank[3];

    public MachineFractionTowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FRACTION_TOWER_BE.get(), pos, state, 0, 0L, 0L, 0L);
        tanks[0] = new FluidTank(TANK_CAPACITY_MB) {
            @Override
            public boolean isFluidValid(Fluid fluid) {
                // Data-driven: рецепт ищется в RecipeManager (заменяет FractionTowerRecipes.has).
                return FractionTowerRecipe.hasRecipe(level, fluid);
            }
        };
        tanks[1] = new FluidTank(TANK_CAPACITY_MB);
        tanks[2] = new FluidTank(TANK_CAPACITY_MB);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineFractionTowerBlockEntity be) {
        if (level.isClientSide) return;

        be.setupTanks();

        if (level.getGameTime() % FRACTIONATE_INTERVAL == 0) {
            be.fractionate();
        }

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            be.trySubscribe(be.tanks[0].getTankType(), level, neighborPos, dir);
            be.tryProvide(be.tanks[1], level, neighborPos, dir);
            be.tryProvide(be.tanks[2], level, neighborPos, dir);
        }

        be.setChanged();
        be.sendUpdateToClient();
    }

    /** Direktport von {@code setupTanks()}: konformiert die Ausgabetanks auf den Rezepttyp, solange sie leer sind
     *  (im Original wird der Typ bedingungslos ueberschrieben, hier vorsichtiger um kein Fluid zu vernichten). */
    private void setupTanks() {
        FractionTowerRecipe recipe = FractionTowerRecipe.getRecipe(level, tanks[0].getTankType());
        if (recipe == null) return;
        if (tanks[1].isEmpty()) tanks[1].conform(recipe.getOutputA());
        if (tanks[2].isEmpty()) tanks[2].conform(recipe.getOutputB());
    }

    /** Direktport von {@code fractionate()}. */
    private boolean fractionate() {
        FractionTowerRecipe recipe = FractionTowerRecipe.getRecipe(level, tanks[0].getTankType());
        if (recipe == null) return false;
        if (tanks[0].getFill() < INPUT_PER_CYCLE_MB) return false;
        if (tanks[1].getFill() + recipe.getOutputAMb() > tanks[1].getMaxFill()) return false;
        if (tanks[2].getFill() + recipe.getOutputBMb() > tanks[2].getMaxFill()) return false;

        tanks[0].drainMb(INPUT_PER_CYCLE_MB);
        tanks[1].fillMb(recipe.getOutputA(), recipe.getOutputAMb());
        tanks[2].fillMb(recipe.getOutputB(), recipe.getOutputBMb());
        return true;
    }

    // ==================== GUI (generische GuiInfoScreen-Balken) ====================

    public int getProgress() {
        return tanks[0].getFill();
    }

    public int getMaxProgress() {
        return tanks[0].getMaxFill();
    }

    public int getProgressScaled(int scale) {
        int max = tanks[0].getMaxFill();
        return max <= 0 ? 0 : tanks[0].getFill() * scale / max;
    }

    public FluidTank[] getTanks() {
        return tanks;
    }

    // ==================== IFluidUserMK2 / MK2-Netz ====================

    @Override
    public FluidTank[] getAllTanks() {
        return tanks;
    }

    @Override
    public FluidTank[] getReceivingTanks() {
        return new FluidTank[] { tanks[0] };
    }

    @Override
    public FluidTank[] getSendingTanks() {
        return new FluidTank[] { tanks[1], tanks[2] };
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null && (FractionTowerRecipe.hasRecipe(level, fluid)
                || tanks[1].getTankType() == fluid || tanks[2].getTankType() == fluid);
    }

    // ==================== NBT ====================

    //? if < 1.21.1 {
    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].writeToNBT(tag, "tank" + i);
        }
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].writeToNBT(tag, "tank" + i);
        }
    }
    *///?}

    //? if < 1.21.1 {
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].readFromNBT(tag, "tank" + i);
        }
    }
    //?} else {
    /*@Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].readFromNBT(tag, "tank" + i);
        }
    }
    *///?}

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.fraction_tower");
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineFractionTowerMenu.create(id, inventory, this);
    }
}
