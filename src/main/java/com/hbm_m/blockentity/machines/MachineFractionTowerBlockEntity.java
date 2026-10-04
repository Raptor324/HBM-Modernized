package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
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
 * 1:1 {@code TileEntityMachineFractionTower}: alle 10 Ticks 100 mB Eingangsfluid -> zwei Fraktionen
 * ({@code FractionRecipes}). Gestapelte Tuerme (Kern drei Bloecke hoeher) gleichen ihre Tanktypen an, Oel steigt nach
 * oben, die Fraktionen sinken nach unten. Eingang und Ausgaenge an den vier Anschlusszellen; Typ per Fluidkennung nur
 * am untersten Segment. Kein GUI im Original, nur das Blick-Overlay.
 */
public class MachineFractionTowerBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    public final FluidTank[] tanks = new FluidTank[] {
            new FluidTank(ModFluids.HEAVYOIL.getSource(), 4000),
            new FluidTank(ModFluids.BITUMEN.getSource(), 4000),
            new FluidTank(ModFluids.SMEAR.getSource(), 4000)
    };

    public MachineFractionTowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FRACTION_TOWER_BE.get(), pos, state, 0, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineFractionTowerBlockEntity be) {
        if (level.isClientSide) return;

        if (level.getBlockEntity(pos.above(3)) instanceof MachineFractionTowerBlockEntity frac) {

            // Typen angleichen
            for (int i = 0; i < 3; i++) frac.tanks[i].setTankType(be.tanks[i].getTankType());

            int oil = Math.min(be.tanks[0].getFill(), frac.tanks[0].getMaxFill() - frac.tanks[0].getFill());
            int left = Math.min(frac.tanks[1].getFill(), be.tanks[1].getMaxFill() - be.tanks[1].getFill());
            int right = Math.min(frac.tanks[2].getFill(), be.tanks[2].getMaxFill() - be.tanks[2].getFill());

            // Oel nach oben, Fraktionen nach unten
            be.tanks[0].setFill(be.tanks[0].getFill() - oil);
            be.tanks[1].setFill(be.tanks[1].getFill() + left);
            be.tanks[2].setFill(be.tanks[2].getFill() + right);
            frac.tanks[0].setFill(frac.tanks[0].getFill() + oil);
            frac.tanks[1].setFill(frac.tanks[1].getFill() - left);
            frac.tanks[2].setFill(frac.tanks[2].getFill() - right);
            frac.setChanged();
        }

        be.setupTanks();

        BlockPos[] cons = { pos.offset(2, 0, 0), pos.offset(-2, 0, 0), pos.offset(0, 0, 2), pos.offset(0, 0, -2) };
        Direction[] dirs = { Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH };
        for (int i = 0; i < 4; i++) be.trySubscribe(be.tanks[0].getTankType(), level, cons[i], dirs[i]);

        if (level.getGameTime() % 10 == 0) be.fractionate();

        for (int i = 0; i < 4; i++) {
            be.tryProvide(be.tanks[1], level, cons[i], dirs[i]);
            be.tryProvide(be.tanks[2], level, cons[i], dirs[i]);
        }

        be.setChanged();
        be.sendUpdateToClient();
    }

    private void setupTanks() {
        FractionTowerRecipe recipe = FractionTowerRecipe.getRecipe(level, tanks[0].getTankType());
        Fluid none = ModFluids.NONE.getSource();
        if (recipe != null) {
            tanks[1].setTankType(recipe.getOutputA());
            tanks[2].setTankType(recipe.getOutputB());
        } else {
            tanks[0].setTankType(none);
            tanks[1].setTankType(none);
            tanks[2].setTankType(none);
        }
    }

    private void fractionate() {
        FractionTowerRecipe recipe = FractionTowerRecipe.getRecipe(level, tanks[0].getTankType());
        if (recipe == null) return;

        int left = recipe.getOutputAMb();
        int right = recipe.getOutputBMb();

        if (tanks[0].getFill() >= 100 && hasSpace(left, right)) {
            tanks[0].setFill(tanks[0].getFill() - 100);
            tanks[1].setFill(tanks[1].getFill() + left);
            tanks[2].setFill(tanks[2].getFill() + right);
        }
    }

    private boolean hasSpace(int left, int right) {
        return tanks[1].getFill() + left <= tanks[1].getMaxFill() && tanks[2].getFill() + right <= tanks[2].getMaxFill();
    }

    /** Original: 3x3x3 um den Kern. */
    //? if forge {
    @Override
    //?}
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return new net.minecraft.world.phys.AABB(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
                worldPosition.getX() + 2, worldPosition.getY() + 3, worldPosition.getZ() + 2);
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
        return fromDir != null;
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
