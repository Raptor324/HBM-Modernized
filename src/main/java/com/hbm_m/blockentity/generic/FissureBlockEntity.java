package com.hbm_m.blockentity.generic;

import com.hbm_m.api.fluids.IFluidStandardSenderMK2;
import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 {@code BlockFissure.TileEntityFissure}: ein immer voller Lavatank (1000 mB), der dem Anschluss direkt darueber
 * angeboten wird. Verbindung nur nach oben (Original {@code canConnect(LAVA, DOWN)} aus Sicht des Nachbarn).
 */
public class FissureBlockEntity extends BaseHbmBlockEntity implements IFluidStandardSenderMK2 {

    private FluidTank lava;

    public FissureBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FISSURE.get(), pos, state);
    }

    private FluidTank lava() {
        if (lava == null) lava = new FluidTank(ModFluids.LAVA.getSource(), 1_000);
        return lava;
    }

    public static void serverTick(Level world, BlockPos pos, BlockState state, FissureBlockEntity te) {
        te.lava().setFill(1_000);
        te.tryProvide(te.lava(), world, pos.above(), Direction.UP);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir == Direction.UP && fluid == ModFluids.LAVA.getSource();
    }

    @Override
    public FluidTank[] getSendingTanks() {
        return new FluidTank[] { lava() };
    }

    @Override
    public FluidTank[] getAllTanks() {
        return new FluidTank[] { lava() };
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }
}
