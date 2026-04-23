package com.hbm_m.explosion.command;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.explosives.IDetonatable;
import com.hbm_m.particle.ModExplosionParticles;
import com.hbm_m.particle.explosions.basic.ExplosionParticleUtils;
import com.hbm_m.particle.explosions.nuclear.medium.MediumNuclearMushroomCloud;
import com.hbm_m.sound.ModSounds;
import com.hbm_m.util.explosions.general.ShockwaveGenerator;
import com.hbm_m.util.explosions.nuclear.CraterGenerationFlags;
import com.hbm_m.util.explosions.nuclear.CraterGenerator;
import com.hbm_m.util.explosions.nuclear.NuclearExplosionHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.MinecraftServer;
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
 * Запуск ядерных сценариев из команд и блоков (единая точка логики).
 */
public final class NuclearScenarioLaunchers {

    private static final Random RANDOM = new Random();

    private NuclearScenarioLaunchers() {}

    // --- Nuclear charge (animated) ---
    private static final float CHARGE_EXPLOSION_POWER = 25.0F;
    private static final int CHARGE_CRATER_DELAY = 40;

    public static void launchNuclearCharge(ServerLevel serverLevel, BlockPos pos, ExplosionCommandOptions opt) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        float power = CHARGE_EXPLOSION_POWER * opt.amplifier();

        if (opt.sound()) {
            NuclearExplosionHelper.playStandardDetonationSound(serverLevel, x, y, z);
        }

        if (opt.damage()) {
            serverLevel.explode(null, x, y, z, power, Level.ExplosionInteraction.NONE);
        }
        if (opt.particles()) {
            scheduleAnimatedNuclearExplosion(serverLevel, x, y, z);
        }
        if (opt.crater()) {
            CraterGenerationFlags flags = new CraterGenerationFlags(opt.biomes(), opt.damage());
            scheduleCraterGeneration(serverLevel, pos, CHARGE_CRATER_DELAY, flags);
        }
    }

    private static void scheduleAnimatedNuclearExplosion(ServerLevel level, double x, double y, double z) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }
        level.sendParticles((SimpleParticleType) ModExplosionParticles.FLASH.get(), x, y, z, 1, 0, 0, 0, 0);
        MediumNuclearMushroomCloud.spawnBlackSphere(level, x, y, z, level.random);

        server.tell(new TickTask(level.getServer().getTickCount() + 2, () ->
                MediumNuclearMushroomCloud.spawnShockwaveRing(level, x, y, z, level.random)));

        for (int i = 0; i < 10; i++) {
            final int step = i;
            server.tell(new TickTask(level.getServer().getTickCount() + 5 + i, () -> {
                double currentY = y + (step * 2.0);
                MediumNuclearMushroomCloud.spawnStemSegment(level, x, currentY, z, level.random);
            }));
        }

        server.tell(new TickTask(level.getServer().getTickCount() + 8, () ->
                MediumNuclearMushroomCloud.spawnMushroomBase(level, x, y, z, level.random)));

        server.tell(new TickTask(level.getServer().getTickCount() + 18, () ->
                MediumNuclearMushroomCloud.spawnMushroomCap(level, x, y, z, level.random)));

        server.tell(new TickTask(level.getServer().getTickCount() + 22, () ->
                MediumNuclearMushroomCloud.spawnCondensationRing(level, x, y + 15, z, level.random)));
    }

    private static void scheduleCraterGeneration(ServerLevel level, BlockPos pos, int delayTicks, CraterGenerationFlags flags) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }
        server.tell(new TickTask(level.getServer().getTickCount() + delayTicks, () ->
                CraterGenerator.generateCrater(
                        level, pos,
                        ModBlocks.SELLAFIELD_SLAKED.get(), ModBlocks.SELLAFIELD_SLAKED1.get(),
                        ModBlocks.SELLAFIELD_SLAKED2.get(), ModBlocks.SELLAFIELD_SLAKED3.get(),
                        ModBlocks.WASTE_LOG.get(), ModBlocks.WASTE_PLANKS.get(),
                        ModBlocks.BURNED_GRASS.get(), ModBlocks.DEAD_DIRT.get(),
                        flags
                )));
    }

    // --- Dud nuke ---
    private static final float DUD_EXPLOSION_POWER = 25.0F;
    private static final int DUD_CRATER_DELAY = 30;

    public static void launchDudNuke(ServerLevel serverLevel, BlockPos pos, ExplosionCommandOptions opt) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        float power = DUD_EXPLOSION_POWER * opt.amplifier();

        if (opt.sound()) {
            NuclearExplosionHelper.playStandardDetonationSound(serverLevel, x, y, z);
        }

        if (opt.damage()) {
            serverLevel.explode(null, x, y, z, power, Level.ExplosionInteraction.NONE);
        }
        if (opt.particles()) {
            scheduleDudExplosionEffects(serverLevel, x, y, z);
        }
        if (opt.crater()) {
            CraterGenerationFlags flags = new CraterGenerationFlags(opt.biomes(), opt.damage());
            MinecraftServer server = serverLevel.getServer();
            if (server != null) {
                server.tell(new TickTask(server.getTickCount() + DUD_CRATER_DELAY, () ->
                        CraterGenerator.generateCrater(
                                serverLevel, pos,
                                ModBlocks.SELLAFIELD_SLAKED.get(),
                                ModBlocks.SELLAFIELD_SLAKED1.get(),
                                ModBlocks.SELLAFIELD_SLAKED2.get(),
                                ModBlocks.SELLAFIELD_SLAKED3.get(),
                                ModBlocks.WASTE_LOG.get(),
                                ModBlocks.WASTE_PLANKS.get(),
                                ModBlocks.BURNED_GRASS.get(),
                                ModBlocks.DEAD_DIRT.get(),
                                flags
                        )));
            }
        }
    }

    private static void scheduleDudExplosionEffects(ServerLevel level, double x, double y, double z) {
        level.sendParticles((SimpleParticleType) ModExplosionParticles.FLASH.get(), x, y, z, 1, 0, 0, 0, 0);
        ExplosionParticleUtils.spawnAirBombSparks(level, x, y, z);
        MinecraftServer server = level.getServer();
        if (server != null) {
            server.tell(new TickTask(3, () ->
                    ExplosionParticleUtils.spawnAirBombShockwave(level, x, y, z)));
            server.tell(new TickTask(8, () ->
                    ExplosionParticleUtils.spawnAirBombMushroomCloud(level, x, y, z)));
        }
    }

    // --- Mine nuke ---
    private static final float MINE_EXPLOSION_POWER = 20.0F;
    private static final int MINE_CRATER_RADIUS = 20;
    private static final int MINE_CRATER_DEPTH = 3;
    private static final float MINE_DAMAGE_RADIUS = 30.0f;
    private static final float MINE_DAMAGE_AMOUNT = 200.0f;
    private static final float MINE_MAX_DAMAGE_DISTANCE = 25.0f;

    public static void launchMineNuke(ServerLevel serverLevel, BlockPos pos, ExplosionCommandOptions opt) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        float power = MINE_EXPLOSION_POWER * opt.amplifier();

        if (opt.sound()) {
            playRandomMineSound(serverLevel, pos);
        }

        if (opt.damage()) {
            serverLevel.explode(null, x, y, z, power, false, Level.ExplosionInteraction.TNT);
            dealMineExplosionDamage(serverLevel, x, y, z, opt.amplifier());
        } else {
            serverLevel.explode(null, x, y, z, 0.0F, false, Level.ExplosionInteraction.NONE);
        }

        if (opt.particles()) {
            ExplosionParticleUtils.spawnFullNuclearExplosion(serverLevel, x, y, z);
        }

        if (opt.crater()) {
            int r = Math.max(1, Math.round(MINE_CRATER_RADIUS * opt.amplifier()));
            int d = Math.max(1, Math.round(MINE_CRATER_DEPTH * opt.amplifier()));
            MinecraftServer server = serverLevel.getServer();
            if (server != null) {
                server.tell(new TickTask(server.getTickCount() + 5, () ->
                        ShockwaveGenerator.generateCrater(
                                serverLevel, pos, r, d,
                                ModBlocks.WASTE_LOG.get(),
                                ModBlocks.WASTE_PLANKS.get(),
                                ModBlocks.BURNED_GRASS.get(),
                                opt.damage()
                        )));
            }
        }
    }

    private static void dealMineExplosionDamage(ServerLevel serverLevel, double x, double y, double z, float amplifier) {
        float damageRadius = MINE_DAMAGE_RADIUS * amplifier;
        float damageAmount = MINE_DAMAGE_AMOUNT * amplifier;
        float maxDist = Math.min(MINE_MAX_DAMAGE_DISTANCE * amplifier, damageRadius);

        List<LivingEntity> entitiesNearby = serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(x - damageRadius, y - damageRadius, z - damageRadius,
                        x + damageRadius, y + damageRadius, z + damageRadius)
        );

        for (LivingEntity entity : entitiesNearby) {
            double distanceToEntity = Math.sqrt(
                    Math.pow(entity.getX() - x, 2) +
                            Math.pow(entity.getY() - y, 2) +
                            Math.pow(entity.getZ() - z, 2)
            );

            if (distanceToEntity <= damageRadius) {
                float damage = damageAmount;
                if (distanceToEntity > maxDist && damageRadius > maxDist) {
                    float remainingDistance = damageRadius - maxDist;
                    float damageDistance = (float) distanceToEntity - maxDist;
                    damage = damageAmount * (1.0f - (damageDistance / remainingDistance)) * 0.5f;
                }
                entity.hurt(entity.damageSources().explosion(null), damage);
            }
        }
    }

    private static void playRandomMineSound(Level level, BlockPos pos) {
        SoundEvent sound = ModSounds.MUKE_EXPLOSION.orElse(null);
        if (sound != null) {
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    sound, SoundSource.BLOCKS, 4.0F, 1.0F);
        }
    }

    // --- Grenade nuc ---
    private static final float GRENADE_EXPLODE_1 = 9.0F;
    private static final int GRENADE_CRATER_RADIUS = 25;
    private static final int GRENADE_CRATER_DEPTH = 10;
    private static final float GRENADE_DAMAGE_RADIUS = 25.0f;
    private static final float GRENADE_DAMAGE_AMOUNT = 200.0f;
    private static final float GRENADE_MAX_DAMAGE_DISTANCE = 25.0f;
    private static final int GRENADE_DETONATION_RADIUS = 8;

    public static void launchGrenadeNuc(ServerLevel serverLevel, BlockPos pos, ExplosionCommandOptions opt, net.minecraft.world.entity.player.Player chainPlayer) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        if (opt.damage()) {
            serverLevel.explode(null, x, y, z, GRENADE_EXPLODE_1 * opt.amplifier(), true, Level.ExplosionInteraction.NONE);
            triggerNearbyDetonations(serverLevel, pos, chainPlayer);
            dealGrenadeExplosionDamage(serverLevel, x, y, z, opt.amplifier());
        }

        if (opt.particles()) {
            ExplosionParticleUtils.spawnFullNuclearExplosion(serverLevel, x, y, z);
        }

        if (opt.sound()) {
            playRandomGrenadeSound(serverLevel, pos);
        }

        if (opt.crater()) {
            int r = Math.max(1, Math.round(GRENADE_CRATER_RADIUS * opt.amplifier()));
            int d = Math.max(1, Math.round(GRENADE_CRATER_DEPTH * opt.amplifier()));
            final boolean damage = opt.damage();
            final float amp = opt.amplifier();
            MinecraftServer server = serverLevel.getServer();
            if (server != null) {
                server.tell(new TickTask(server.getTickCount() + 30, () -> {
                    if (damage) {
                        serverLevel.explode(null, x, y, z, GRENADE_EXPLODE_1 * amp, Level.ExplosionInteraction.NONE);
                    }
                    ShockwaveGenerator.generateCrater(
                            serverLevel, pos, r, d,
                            ModBlocks.WASTE_LOG.get(),
                            ModBlocks.WASTE_PLANKS.get(),
                            ModBlocks.BURNED_GRASS.get(),
                            damage
                    );
                }));
            }
        }
    }

    private static void dealGrenadeExplosionDamage(ServerLevel serverLevel, double x, double y, double z, float amplifier) {
        float damageRadius = GRENADE_DAMAGE_RADIUS * amplifier;
        float damageAmount = GRENADE_DAMAGE_AMOUNT * amplifier;
        float maxDist = Math.min(GRENADE_MAX_DAMAGE_DISTANCE * amplifier, damageRadius);

        List<LivingEntity> entitiesNearby = serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(x - damageRadius, y - damageRadius, z - damageRadius,
                        x + damageRadius, y + damageRadius, z + damageRadius)
        );

        for (LivingEntity entity : entitiesNearby) {
            double distanceToEntity = Math.sqrt(
                    Math.pow(entity.getX() - x, 2) +
                            Math.pow(entity.getY() - y, 2) +
                            Math.pow(entity.getZ() - z, 2)
            );

            if (distanceToEntity <= damageRadius) {
                float damage = damageAmount;
                if (distanceToEntity > maxDist && damageRadius > maxDist) {
                    float remainingDistance = damageRadius - maxDist;
                    float damageDistance = (float) distanceToEntity - maxDist;
                    damage = damageAmount * (1.0f - (damageDistance / remainingDistance)) * 0.5f;
                }
                entity.hurt(entity.damageSources().explosion(null), damage);
            }
        }
    }

    private static void triggerNearbyDetonations(ServerLevel serverLevel, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        for (int ox = -GRENADE_DETONATION_RADIUS; ox <= GRENADE_DETONATION_RADIUS; ox++) {
            for (int oy = -GRENADE_DETONATION_RADIUS; oy <= GRENADE_DETONATION_RADIUS; oy++) {
                for (int oz = -GRENADE_DETONATION_RADIUS; oz <= GRENADE_DETONATION_RADIUS; oz++) {
                    double dist = Math.sqrt(ox * ox + oy * oy + oz * oz);
                    if (dist <= GRENADE_DETONATION_RADIUS && dist > 0) {
                        BlockPos checkPos = pos.offset(ox, oy, oz);
                        BlockState checkState = serverLevel.getBlockState(checkPos);
                        Block block = checkState.getBlock();
                        if (block instanceof IDetonatable detonatable) {
                            int delay = (int) (dist * 1.5);
                            serverLevel.getServer().tell(new TickTask(delay, () ->
                                    detonatable.onDetonate(serverLevel, checkPos, checkState, player)));
                        }
                    }
                }
            }
        }
    }

    private static void playRandomGrenadeSound(Level level, BlockPos pos) {
        List<SoundEvent> sounds = Arrays.asList(
                ModSounds.MUKE_EXPLOSION.orElse(null),
                ModSounds.MUKE_EXPLOSION.orElse(null),
                ModSounds.MUKE_EXPLOSION.orElse(null)
        );
        sounds.removeIf(Objects::isNull);
        if (!sounds.isEmpty()) {
            SoundEvent sound = sounds.get(RANDOM.nextInt(sounds.size()));
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    sound, SoundSource.BLOCKS, 4.0F, 1.0F);
        }
    }
}
