package com.hbm_m.blockentity.machines;

import java.util.List;

import com.hbm_m.api.block.IBlowable;
import com.hbm_m.block.machines.MachineFanBlock;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 1:1 {@code MachineFan.TileEntityFan}. */
public class MachineFanBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity {

    public float spin;
    public float prevSpin;
    public boolean falloff = true;
    public boolean suck = false;

    public MachineFanBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FAN_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineFanBlockEntity be) {
        be.update(level, pos, state);
    }

    private void update(Level world, BlockPos pos, BlockState state) {

        this.prevSpin = this.spin;

        if (world.hasNeighborSignal(pos)) {
            Direction dir = state.getValue(MachineFanBlock.FACING);

            int range = 10;
            int effRange = 0;
            double push = 0.1;

            for (int i = 1; i <= range; i++) {
                BlockPos p = pos.relative(dir, i);
                BlockState bs = world.getBlockState(p);
                Block block = bs.getBlock();
                boolean blowable = block instanceof IBlowable;

                if (bs.isRedstoneConductor(world, p) || blowable) {
                    if (!world.isClientSide && blowable)
                        ((IBlowable) block).applyFan(world, p, dir, i);

                    break;
                }

                effRange = i;
            }

            int x = dir.getStepX() * effRange;
            int y = dir.getStepY() * effRange;
            int z = dir.getStepZ() * effRange;

            List<Entity> affected = world.getEntitiesOfClass(Entity.class, new AABB(
                    pos.getX() + 0.5 + Math.min(x, 0), pos.getY() + 0.5 + Math.min(y, 0), pos.getZ() + 0.5 + Math.min(z, 0),
                    pos.getX() + 0.5 + Math.max(x, 0), pos.getY() + 0.5 + Math.max(y, 0), pos.getZ() + 0.5 + Math.max(z, 0)).inflate(0.5, 0.5, 0.5));

            for (Entity e : affected) {

                double coeff = push;

                if (falloff) {
                    double dist = Math.sqrt(e.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
                    coeff *= 1.5 * (1 - dist / range / 2);
                }
                if (suck) coeff *= -1;

                Vec3 m = e.getDeltaMovement();
                e.setDeltaMovement(m.x + dir.getStepX() * coeff, m.y + dir.getStepY() * coeff, m.z + dir.getStepZ() * coeff);
                e.hurtMarked = true;
            }

            if (world.isClientSide && world.random.nextInt(30) == 0) {
                double speed = suck ? -0.2 : 0.2;
                world.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.5 + dir.getStepX() * 0.5, pos.getY() + 0.5 + dir.getStepY() * 0.5, pos.getZ() + 0.5 + dir.getStepZ() * 0.5,
                        dir.getStepX() * speed, dir.getStepY() * speed, dir.getStepZ() * speed);
            }

            this.spin += 30;
        }

        if (this.spin >= 360) {
            this.prevSpin -= 360;
            this.spin -= 360;
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        nbt.putBoolean("falloff", falloff);
        nbt.putBoolean("suck", suck);
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        this.falloff = nbt.getBoolean("falloff");
        this.suck = nbt.getBoolean("suck");
    }
}
