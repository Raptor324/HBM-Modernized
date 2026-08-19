package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineCrackingTowerMenu;
import com.hbm_m.recipe.CrackingTowerRecipe;

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
 * Katalytischer Cracker: Portierung von {@code TileEntityMachineCatalyticCracker} (1.7.10 Original) - der
 * "MachineCrackingTower" in diesem Port ist eine Umbenennung dieser Original-TE, NICHT von
 * {@code TileEntityMachineFractionTower}. Wandelt 100mB eines Oel-Fluids (Tank 0) + 200mB Dampf (Tank 1) alle
 * 5 Ticks (bis zu 2x pro Intervall, wie im Original) in zwei leichtere Fraktionen (Tank 2/3) + 2mB Restdampf
 * (Tank 4) um, ueber die data-driven Rezeptliste {@link CrackingTowerRecipe} (Port von {@code CrackingRecipes}).
 * <p>
 * Vereinfachung ggue. Original: Einzelblock statt mehrstoeckigem Multiblock, MK2-Rohrnetz an allen 6 Seiten statt
 * der festen Multiblock-Anschlusspunkte - analog zu {@link MachineFractionTowerBlockEntity}.
 */
public class MachineCrackingTowerBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    private static final int OIL_CAPACITY_MB = 4000;
    private static final int STEAM_CAPACITY_MB = 8000;
    private static final int SPENTSTEAM_CAPACITY_MB = 800;

    private static final int CRACK_INTERVAL = 5;
    private static final int MAX_CRACKS_PER_INTERVAL = 2;
    private static final int OIL_PER_CRACK_MB = 100;

    private final FluidTank[] tanks = new FluidTank[5];

    public MachineCrackingTowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRACKING_TOWER_BE.get(), pos, state, 0, 0L, 0L, 0L);
        tanks[0] = new FluidTank(ModFluids.BITUMEN.getSource(), OIL_CAPACITY_MB) {
            @Override
            public boolean isFluidValid(Fluid fluid) {
                // Data-driven: рецепт ищется в RecipeManager (заменяет CrackingTowerRecipes.has).
                return CrackingTowerRecipe.hasRecipe(level, fluid);
            }
        };
        tanks[1] = new FluidTank(ModFluids.STEAM.getSource(), STEAM_CAPACITY_MB) {
            @Override
            public boolean isFluidValid(Fluid fluid) {
                return fluid == ModFluids.STEAM.getSource();
            }
        };
        tanks[2] = new FluidTank(OIL_CAPACITY_MB);
        tanks[3] = new FluidTank(OIL_CAPACITY_MB);
        tanks[4] = new FluidTank(ModFluids.SPENTSTEAM.getSource(), SPENTSTEAM_CAPACITY_MB);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCrackingTowerBlockEntity be) {
        if (level.isClientSide) return;

        be.setupTanks();

        if (level.getGameTime() % CRACK_INTERVAL == 0) {
            for (int i = 0; i < MAX_CRACKS_PER_INTERVAL; i++) {
                if (!be.crack()) break;
            }
        }

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            be.trySubscribe(be.tanks[0].getTankType(), level, neighborPos, dir);
            be.trySubscribe(be.tanks[1].getTankType(), level, neighborPos, dir);
            be.tryProvide(be.tanks[2], level, neighborPos, dir);
            be.tryProvide(be.tanks[3], level, neighborPos, dir);
            be.tryProvide(be.tanks[4], level, neighborPos, dir);
        }

        be.setChanged();
        be.sendUpdateToClient();
    }

    /** Direktport von {@code setupTanks()}. */
    private void setupTanks() {
        CrackingTowerRecipe recipe = CrackingTowerRecipe.getRecipe(level, tanks[0].getTankType());
        if (recipe == null) return;
        if (tanks[2].isEmpty()) tanks[2].conform(recipe.getOutputA());
        if (tanks[3].isEmpty() && recipe.hasOutputB()) tanks[3].conform(recipe.getOutputB());
    }

    /** Direktport von {@code crack()}: verbraucht Oel + Dampf, produziert zwei Fraktionen + Restdampf. */
    private boolean crack() {
        CrackingTowerRecipe recipe = CrackingTowerRecipe.getRecipe(level, tanks[0].getTankType());
        if (recipe == null) return false;

        int steamNeeded = CrackingTowerRecipe.STEAM_PER_100_INPUT;
        int spentSteamProduced = CrackingTowerRecipe.SPENTSTEAM_PRODUCED;

        if (tanks[0].getFill() < OIL_PER_CRACK_MB) return false;
        if (tanks[1].getFill() < steamNeeded) return false;
        if (tanks[2].getFill() + recipe.getOutputAMb() > tanks[2].getMaxFill()) return false;
        if (recipe.hasOutputB() && tanks[3].getFill() + recipe.getOutputBMb() > tanks[3].getMaxFill()) return false;
        if (tanks[4].getFill() + spentSteamProduced > tanks[4].getMaxFill()) return false;

        tanks[0].drainMb(OIL_PER_CRACK_MB);
        tanks[1].drainMb(steamNeeded);
        tanks[2].fillMb(recipe.getOutputA(), recipe.getOutputAMb());
        if (recipe.hasOutputB()) {
            tanks[3].fillMb(recipe.getOutputB(), recipe.getOutputBMb());
        }
        tanks[4].fillMb(ModFluids.SPENTSTEAM.getSource(), spentSteamProduced);
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
        return new FluidTank[] { tanks[0], tanks[1] };
    }

    @Override
    public FluidTank[] getSendingTanks() {
        return new FluidTank[] { tanks[2], tanks[3], tanks[4] };
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null && (CrackingTowerRecipe.hasRecipe(level, fluid)
                || fluid == ModFluids.STEAM.getSource()
                || tanks[2].getTankType() == fluid || tanks[3].getTankType() == fluid || tanks[4].getTankType() == fluid);
    }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].writeToNBT(tag, "tank" + i);
        }
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].readFromNBT(tag, "tank" + i);
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.cracking_tower");
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
        return MachineCrackingTowerMenu.create(id, inventory, this);
    }
}
