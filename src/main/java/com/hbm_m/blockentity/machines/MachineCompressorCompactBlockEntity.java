package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityMachineCompressorCompact}: der kompakte Kompressor im Kondensatorgehaeuse, zwei gegenlaeufige
 * Luefter, sechs Anschluesse auf Hoehe 1.
 */
public class MachineCompressorCompactBlockEntity extends MachineCompressorBaseBlockEntity {

    public float fanSpin;
    public float prevFanSpin;

    public MachineCompressorCompactBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMPRESSOR_COMPACT_BE.get(), pos, state);
    }

    @Override
    protected void clientTick(Level level) {

        this.prevFanSpin = this.fanSpin;

        if (this.isOn) {
            this.fanSpin += 45;

            if (this.fanSpin >= 360) {
                this.prevFanSpin -= 360;
                this.fanSpin -= 360;
            }
        }
    }

    @Override
    public DirPos[] getConPos() {
        Direction dir = dir();
        Direction rot = dir.getClockWise(); // Original: dir.getRotation(UP)
        BlockPos p = worldPosition.above();

        return new DirPos[] {
                new DirPos(p.relative(rot, 4), rot),
                new DirPos(p.relative(rot, -4), rot.getOpposite()),
                new DirPos(p.relative(dir, 2).relative(rot, -1), dir),
                new DirPos(p.relative(dir, 2).relative(rot, 1), dir),
                new DirPos(p.relative(dir, -2).relative(rot, -1), dir.getOpposite()),
                new DirPos(p.relative(dir, -2).relative(rot, 1), dir.getOpposite())
        };
    }

    private AABB bb = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (bb == null) bb = new AABB(worldPosition.getX() - 3, worldPosition.getY(), worldPosition.getZ() - 3,
                worldPosition.getX() + 4, worldPosition.getY() + 3, worldPosition.getZ() + 4);
        return bb;
    }
}
