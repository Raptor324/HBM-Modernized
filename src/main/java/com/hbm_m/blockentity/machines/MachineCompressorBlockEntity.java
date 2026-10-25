package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityMachineCompressor}: der grosse Kompressor mit Pumpenkolben (der langsam hochfaehrt und mit
 * einem Bolzenschlag herunterfaellt) und Luefter. Anschluesse links, rechts und hinten.
 */
public class MachineCompressorBlockEntity extends MachineCompressorBaseBlockEntity {

    public float fanSpin;
    public float prevFanSpin;
    public float piston;
    public float prevPiston;
    public boolean pistonDir;
    private float randSpeed = 0.1F;

    public MachineCompressorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMPRESSOR_BE.get(), pos, state);
    }

    @Override
    protected void clientTick(Level level) {

        this.prevFanSpin = this.fanSpin;
        this.prevPiston = this.piston;

        if (this.isOn) {
            this.fanSpin += 15;

            if (this.fanSpin >= 360) {
                this.prevFanSpin -= 360;
                this.fanSpin -= 360;
            }

            if (this.pistonDir) {
                this.piston -= randSpeed;
                if (this.piston <= 0) {
                    level.playLocalSound(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                            com.hbm_m.sound.HbmSoundsNT.get("hbm:item.boltgun"), SoundSource.BLOCKS, 0.5F, 0.75F, false);
                    this.pistonDir = !this.pistonDir;
                }
            } else {
                this.piston += 0.05F;
                if (this.piston >= 1) {
                    this.randSpeed = 0.085F + level.random.nextFloat() * 0.03F;
                    this.pistonDir = !this.pistonDir;
                }
            }

            this.piston = Mth.clamp(this.piston, 0F, 1F);
        }
    }

    @Override
    public DirPos[] getConPos() {
        Direction dir = dir();
        Direction rot = dir.getClockWise(); // Original: dir.getRotation(UP)

        return new DirPos[] {
                new DirPos(worldPosition.relative(rot, 2), rot),
                new DirPos(worldPosition.relative(rot, -2), rot.getOpposite()),
                new DirPos(worldPosition.relative(dir, -2), dir.getOpposite()),
        };
    }

    private AABB bb = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (bb == null) bb = new AABB(worldPosition.getX() - 2, worldPosition.getY(), worldPosition.getZ() - 2,
                worldPosition.getX() + 3, worldPosition.getY() + 9, worldPosition.getZ() + 3);
        return bb;
    }
}
