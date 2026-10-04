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
 * 1:1 {@code TileEntityMachineCatalyticCracker} ("cracking_tower" in diesem Port): alle 5 Ticks bis zu zweimal
 * 100 mB Eingangsfluid (Typ per Fluidkennung am Block gesetzt) + 200 mB Dampf -> zwei Fraktionen + 2 mB Altdampf
 * ({@code CrackingRecipes}). Kein GUI im Original, nur das Blick-Overlay. Eingaenge an allen acht Anschlusszellen jeden
 * Tick, Ausgaenge alle 10 Ticks.
 */
public class MachineCrackingTowerBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    public final FluidTank[] tanks = new FluidTank[] {
            new FluidTank(ModFluids.BITUMEN.getSource(), 4000),
            new FluidTank(ModFluids.STEAM.getSource(), 8000),
            new FluidTank(ModFluids.CRUDE_OIL.getSource(), 4000),
            new FluidTank(ModFluids.PETROLEUM.getSource(), 4000),
            new FluidTank(ModFluids.SPENTSTEAM.getSource(), 800)
    };

    public MachineCrackingTowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRACKING_TOWER_BE.get(), pos, state, 0, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCrackingTowerBlockEntity be) {
        if (level.isClientSide) return;

        be.setupTanks();

        for (DirPos con : be.getConPos()) {
            be.trySubscribe(be.tanks[0].getTankType(), level, con.pos, con.dir);
            be.trySubscribe(be.tanks[1].getTankType(), level, con.pos, con.dir);
        }

        if (level.getGameTime() % 5 == 0) be.crack();

        if (level.getGameTime() % 10 == 0) {
            for (DirPos con : be.getConPos()) {
                for (int i = 2; i <= 4; i++) {
                    if (be.tanks[i].getFill() > 0) be.tryProvide(be.tanks[i], level, con.pos, con.dir);
                }
            }
        }

        be.setChanged();
        be.sendUpdateToClient();
    }

    private record DirPos(BlockPos pos, Direction dir) { }

    private DirPos[] getConPos() {
        Direction dir = getBlockState().getValue(com.hbm_m.block.machines.DummyableMachineBlock.FACING);
        Direction rot = dir.getClockWise();
        BlockPos p = worldPosition;
        return new DirPos[] {
                new DirPos(p.relative(dir, 4).relative(rot, 1), dir),
                new DirPos(p.relative(dir, 4).relative(rot, -2), dir),
                new DirPos(p.relative(dir, -4).relative(rot, 1), dir.getOpposite()),
                new DirPos(p.relative(dir, -4).relative(rot, -2), dir.getOpposite()),
                new DirPos(p.relative(dir, 2).relative(rot, 3), rot),
                new DirPos(p.relative(dir, 2).relative(rot, -4), rot),
                new DirPos(p.relative(dir, -2).relative(rot, 3), rot.getOpposite()),
                new DirPos(p.relative(dir, -2).relative(rot, -4), rot.getOpposite())
        };
    }

    private void crack() {
        CrackingTowerRecipe recipe = CrackingTowerRecipe.getRecipe(level, tanks[0].getTankType());
        if (recipe == null) return;

        int left = recipe.getOutputAMb();
        int right = recipe.hasOutputB() ? recipe.getOutputBMb() : 0;

        for (int i = 0; i < 2; i++) {
            if (tanks[0].getFill() >= 100 && tanks[1].getFill() >= 200 && hasSpace(left, right)) {
                tanks[0].setFill(tanks[0].getFill() - 100);
                tanks[1].setFill(tanks[1].getFill() - 200);
                tanks[2].setFill(tanks[2].getFill() + left);
                tanks[3].setFill(tanks[3].getFill() + right);
                tanks[4].setFill(tanks[4].getFill() + 2); // Altdampf hat die Dichte von Wasser, nicht von Dampf
            }
        }
    }

    private boolean hasSpace(int left, int right) {
        return tanks[2].getFill() + left <= tanks[2].getMaxFill() && tanks[3].getFill() + right <= tanks[3].getMaxFill() && tanks[4].getFill() + 2 <= tanks[4].getMaxFill();
    }

    private void setupTanks() {
        CrackingTowerRecipe recipe = CrackingTowerRecipe.getRecipe(level, tanks[0].getTankType());
        Fluid none = ModFluids.NONE.getSource();
        if (recipe != null) {
            tanks[1].setTankType(ModFluids.STEAM.getSource());
            tanks[2].setTankType(recipe.getOutputA());
            tanks[3].setTankType(recipe.hasOutputB() ? recipe.getOutputB() : none);
            tanks[4].setTankType(ModFluids.SPENTSTEAM.getSource());
        } else {
            tanks[2].setTankType(none);
            tanks[3].setTankType(none);
            tanks[4].setTankType(none);
        }
    }

    /** Original: 7x16x7 um den Kern. */
    //? if forge {
    @Override
    //?}
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return new net.minecraft.world.phys.AABB(worldPosition.getX() - 3, worldPosition.getY(), worldPosition.getZ() - 3,
                worldPosition.getX() + 4, worldPosition.getY() + 16, worldPosition.getZ() + 4);
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
