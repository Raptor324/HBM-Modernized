package com.hbm_m.blockentity.network;

import com.hbm_m.api.energy.Nodespace;
import com.hbm_m.api.energy.PowerConductor;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Blockentities der Kastenrohre: Fluessigkeits-/Abgasrohr ({@code TileEntityPipeBaseNT}/{@code TileEntityPipeExhaust}) und Kabel ({@code TileEntityCableBaseNT}). */
public final class BoxDuctBlockEntities {

    private BoxDuctBlockEntities() {}

    public static class Pipe extends PaintableDuctBlockEntity {
        public Pipe(BlockPos pos, BlockState state) { super(ModBlockEntities.FLUID_DUCT_BOX.get(), pos, state); }
    }

    public static class Cable extends BlockEntity implements PowerConductor {
        public Cable(BlockPos pos, BlockState state) { super(ModBlockEntities.RED_CABLE_BOX.get(), pos, state); }

        public static void serverTick(Level level, BlockPos pos, BlockState state, Cable te) {
            if (!(level instanceof ServerLevel sl)) return;
            Nodespace.PowerNode node = Nodespace.getNode(sl, pos);
            if (node == null || node.expired) Nodespace.createNode(sl, te.createNode(pos));
        }

        @Override public boolean canConnectEnergy(Direction side) { return true; }

        @Override
        public void setRemoved() {
            super.setRemoved();
            if (level instanceof ServerLevel sl) Nodespace.destroyNode(sl, worldPosition);
        }
    }
}
