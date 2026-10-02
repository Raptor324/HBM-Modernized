package com.hbm_m.api.fluids;

import com.hbm_m.blockentity.machines.FluidDuctBlockEntity;
import com.hbm_m.blockentity.machines.FluidValveBlockEntity;
import com.hbm_m.blockentity.network.PaintableDuctBlockEntity;
import com.hbm_m.blockentity.network.PipeAnchorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 {@code IBlockFluidDuct.changeTypeRecursively} ({@code FluidDuctBase}, {@code FluidPipeAnchor}): mit dem
 * Identifikator schleichend angeklickt, faerbt ein Rohr bis zu 64 Schritte weit alle angrenzenden Rohre der alten
 * Fluessigkeit um; Anker folgen ihrer Anschlussseite und ihren Luftleitungen.
 */
public final class PipeTypeChanger {

    private PipeTypeChanger() {}

    public static final int LOOPS = 64;

    public static Fluid typeOf(BlockEntity be) {
        if (be instanceof PaintableDuctBlockEntity p && p.isExhaust()) return null;
        return be instanceof IFluidPipeMK2 pipe ? pipe.getFluidType() : null;
    }

    public static void setType(BlockEntity be, Fluid fluid) {
        if (be instanceof FluidDuctBlockEntity d) d.setFluidType(fluid);
        else if (be instanceof FluidValveBlockEntity v) v.setFluidType(fluid);
        else if (be instanceof PaintableDuctBlockEntity p) p.setFluidType(fluid);
        else if (be instanceof PipeAnchorBlockEntity a) a.setType(fluid);
    }

    public static void changeTypeRecursively(Level world, BlockPos pos, Fluid prevType, Fluid type, int loopsRemaining) {
        BlockEntity te = world.getBlockEntity(pos);
        Fluid cur = te == null ? null : typeOf(te);
        if (cur == null || cur != prevType || cur == type) return;
        setType(te, type);
        if (loopsRemaining <= 0) return;

        if (te instanceof PipeAnchorBlockEntity anchor) {
            Direction dir = te.getBlockState().getValue(BlockStateProperties.FACING).getOpposite();
            changeTypeRecursively(world, pos.relative(dir), prevType, type, loopsRemaining - 1);
            for (BlockPos p : anchor.getConnected()) changeTypeRecursively(world, p, prevType, type, loopsRemaining - 1);
            return;
        }
        for (Direction dir : Direction.values()) changeTypeRecursively(world, pos.relative(dir), prevType, type, loopsRemaining - 1);
    }

    /** {@code FluidDuctBase.onBlockActivated} mit Identifikator: schleichend rekursiv, sonst nur dieses Rohr. */
    public static void apply(Level world, BlockPos pos, Fluid type, boolean sneaking) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te == null) return;
        Fluid cur = typeOf(te);
        if (cur == null) return;
        if (!sneaking) {
            if (cur != type) setType(te, type);
        } else {
            changeTypeRecursively(world, pos, cur, type, LOOPS);
        }
    }
}
