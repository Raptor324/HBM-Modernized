package com.hbm_m.util.explosions.nuclear;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.explosives.IDetonatable;
import com.hbm_m.particle.ModExplosionParticles;
import com.hbm_m.particle.explosions.basic.ExplosionParticleUtils;
import com.hbm_m.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Вспомогательный класс для ядерных взрывов.
 *
 * Логика основана на AirNukeBombProjectileEntity и используется как
 * авиационными бомбами, так и баллистическими ракетами.
 */
public class NuclearExplosionHelper {

    private static final float EXPLOSION_POWER = 25.0f;
    private static final float DAMAGE_RADIUS = 60.0f;
    private static final int DETONATION_RADIUS = 20;
    private static final int CRATER_GENERATION_DELAY = 30;
    private static final Random RANDOM = new Random();

    public static void explodeStandardNuke(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel) || ((ServerLevel) level).isClientSide()) {
            return;
        }

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        triggerNearbyDetonations(serverLevel, pos);
        dealExplosionDamage(serverLevel, x, y, z);
        scheduleExplosionEffects(serverLevel, x, y, z);
        playDetonationSound(serverLevel, pos);

        serverLevel.explode(null, x, y, z, EXPLOSION_POWER, Level.ExplosionInteraction.NONE);

        if (serverLevel.getServer() != null) {
            serverLevel.getServer().tell(new TickTask(CRATER_GENERATION_DELAY, () ->
                    CraterGenerator.generateCrater(
                            serverLevel,
                            pos,
                            ModBlocks.SELLAFIELD_SLAKED.get(),
                            ModBlocks.SELLAFIELD_SLAKED1.get(),
                            ModBlocks.SELLAFIELD_SLAKED2.get(),
                            ModBlocks.SELLAFIELD_SLAKED3.get(),
                            ModBlocks.WASTE_LOG.get(),
                            ModBlocks.WASTE_PLANKS.get(),
                            ModBlocks.BURNED_GRASS.get(),
                            ModBlocks.DEAD_DIRT.get()
                    )));
        }
    }

    private static void dealExplosionDamage(ServerLevel serverLevel, double x, double y, double z) {
        List<LivingEntity> entitiesNearby = serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(
                        x - DAMAGE_RADIUS, y - DAMAGE_RADIUS, z - DAMAGE_RADIUS,
                        x + DAMAGE_RADIUS, y + DAMAGE_RADIUS, z + DAMAGE_RADIUS
                )
        );

        for (LivingEntity entity : entitiesNearby) {
            entity.hurt(serverLevel.damageSources().generic(), 50.0F);
        }
    }

    private static void scheduleExplosionEffects(ServerLevel level, double x, double y, double z) {
        level.sendParticles(
                (SimpleParticleType) ModExplosionParticles.EXPLOSION_FLASH.get(),
                x, y, z, 1, 0, 0, 0, 0
        );

        ExplosionParticleUtils.spawnAirBombSparks(level, x, y, z);

        level.getServer().tell(new TickTask(3, () ->
                ExplosionParticleUtils.spawnAirBombShockwave(level, x, y, z)));

        level.getServer().tell(new TickTask(8, () ->
                ExplosionParticleUtils.spawnAirBombMushroomCloud(level, x, y, z)));
    }

    private static void playDetonationSound(ServerLevel level, BlockPos pos) {
        List<SoundEvent> candidates = new ArrayList<>();
        if (ModSounds.BOMBDET1.isPresent()) candidates.add(ModSounds.BOMBDET1.get());
        if (ModSounds.BOMBDET2.isPresent()) candidates.add(ModSounds.BOMBDET2.get());
        if (ModSounds.BOMBDET3.isPresent()) candidates.add(ModSounds.BOMBDET3.get());

        if (candidates.isEmpty()) {
            if (ModSounds.EXPLOSION_LARGE_NEAR.isPresent()) {
                level.playSound(
                        null,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        ModSounds.EXPLOSION_LARGE_NEAR.get(),
                        SoundSource.BLOCKS,
                        8.0F,
                        0.8F + RANDOM.nextFloat() * 0.2F
                );
            }
            return;
        }

        SoundEvent soundEvent = candidates.get(RANDOM.nextInt(candidates.size()));
        level.playSound(
                null,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                soundEvent,
                SoundSource.BLOCKS,
                8.0F,
                0.8F + RANDOM.nextFloat() * 0.2F
        );
    }

    private static void triggerNearbyDetonations(ServerLevel serverLevel, BlockPos pos) {
        for (int x = -DETONATION_RADIUS; x <= DETONATION_RADIUS; x++) {
            for (int y = -DETONATION_RADIUS; y <= DETONATION_RADIUS; y++) {
                for (int z = -DETONATION_RADIUS; z <= DETONATION_RADIUS; z++) {
                    double dist = Math.sqrt(x * x + y * y + z * z);

                    if (dist <= DETONATION_RADIUS && dist > 0) {
                        BlockPos checkPos = pos.offset(x, y, z);
                        BlockState checkState = serverLevel.getBlockState(checkPos);
                        Block block = checkState.getBlock();

                        if (block instanceof IDetonatable detonatable) {
                            int delay = (int) (dist * 2.0);
                            serverLevel.getServer().tell(new TickTask(delay, () ->
                                    detonatable.onDetonate(serverLevel, checkPos, checkState, null)));
                        }
                    }
                }
            }
        }
    }
}

