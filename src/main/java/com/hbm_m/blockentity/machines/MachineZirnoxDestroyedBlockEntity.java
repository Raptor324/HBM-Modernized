package com.hbm_m.blockentity.machines;

import java.util.List;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.util.ContaminationUtil;
import com.hbm_m.util.ContaminationUtil.ContaminationType;
import com.hbm_m.util.ContaminationUtil.HazardType;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;

public class MachineZirnoxDestroyedBlockEntity extends BlockEntity {

    private static final float RADS_ON_FIRE = 500_000F;
    private static final float RADS_COOLED = 75_000F;
    private static final double RANGE = 100D;

    public boolean onFire = true;

    public MachineZirnoxDestroyedBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ZIRNOX_DESTROYED_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineZirnoxDestroyedBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }

        blockEntity.radiate(level, pos);

        RandomSource random = level.getRandom();
        if (random.nextInt(5000) == 0) {
            blockEntity.onFire = false;
        }

        if (blockEntity.onFire && level.getGameTime() % 50L == 0L) {
            blockEntity.spawnFlameEffects((ServerLevel) level, pos, random);
        }

        blockEntity.setChanged();
    }

    private void spawnFlameEffects(ServerLevel level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.25D + random.nextDouble() * 0.5D;
        double y = pos.getY() + 1.75D;
        double z = pos.getZ() + 0.25D + random.nextDouble() * 0.5D;

        level.sendParticles(ParticleTypes.FLAME, x, y, z, 12, 0.2D, 0.2D, 0.2D, 0.01D);
        level.sendParticles(ParticleTypes.SMOKE, x, y, z, 6, 0.2D, 0.15D, 0.2D, 0.01D);
        level.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 1.0F + random.nextFloat(), 0.3F + random.nextFloat() * 0.7F);
    }

    private void radiate(Level level, BlockPos pos) {
        float baseRads = onFire ? RADS_ON_FIRE : RADS_COOLED;

        List<LivingEntity> entities = level.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                        pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D).inflate(RANGE));

        for (LivingEntity entity : entities) {
            Vec3 source = new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
            Vec3 target = new Vec3(entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ());
            Vec3 diff = target.subtract(source);
            double len = diff.length();
            if (len <= 0.001D) {
                continue;
            }

            Vec3 dir = diff.normalize();
            float resistance = 0F;
            int steps = (int) Math.floor(len);
            for (int i = 1; i < steps; i++) {
                int ix = (int) Math.floor(source.x + dir.x * i);
                int iy = (int) Math.floor(source.y + dir.y * i);
                int iz = (int) Math.floor(source.z + dir.z * i);
                resistance += level.getBlockState(new BlockPos(ix, iy, iz)).getBlock().getExplosionResistance();
            }

            if (resistance < 1F) {
                resistance = 1F;
            }

            float effectiveRads = (float) (baseRads / resistance / (len * len));
            ContaminationUtil.contaminate(entity, HazardType.RADIATION, ContaminationType.CREATIVE, effectiveRads);

            if (onFire && len < 5D) {
                entity.hurt(ModDamageSources.radiation(level), 2F);
                entity.setRemainingFireTicks(Math.max(entity.getRemainingFireTicks(), 40));
            }
        }
    }

    //? if < 1.21.1 {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("onFire", onFire);
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.saveAdditional(tag, registries);
        tag.putBoolean("onFire", onFire);
    
    }
    *///?}

    //? if < 1.21.1 {
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        onFire = tag.getBoolean("onFire");
    }
    //?} else {
    /*@Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.loadAdditional(tag, registries);
        onFire = tag.getBoolean("onFire");
    
    }
    *///?}
}
