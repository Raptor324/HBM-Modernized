package com.hbm_m.particle.helper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Рвота при радиационной болезни. Порт {@link com.hbm.main.ClientProxy} type {@code vomit} (1.7.10).
 */
public final class VomitParticleCreator implements IParticleCreator {

    @Override
    public void makeParticle(ClientLevel level, Player player, RandomSource rand, double x, double y, double z, CompoundTag tag) {
        if (!tag.contains("entity")) {
            return;
        }

        Entity entity = level.getEntity(tag.getInt("entity"));
        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        int particleSetting = mc.options.particles().get().getId();
        int count = tag.getInt("count") / (particleSetting + 1);
        String mode = tag.getString("mode");

        double ix = living.getX();
        // 1.7.10: posY - getYOffset() + getEyeHeight() (+1 player in original); без лишнего +1 — на уровне головы
        double iy = living.getY() + living.getEyeHeight();
        double iz = living.getZ();
        Vec3 look = living.getViewVector(1.0F);

        for (int i = 0; i < count; i++) {
            if ("smoke".equals(mode)) {
                mc.particleEngine.createParticle(
                        ParticleTypes.SMOKE,
                        ix, iy, iz,
                        (look.x + rand.nextGaussian() * 0.1D) * 0.05D,
                        (look.y + rand.nextGaussian() * 0.1D) * 0.05D,
                        (look.z + rand.nextGaussian() * 0.1D) * 0.05D);
                continue;
            }

            double vx = (look.x + rand.nextGaussian() * 0.2D) * 0.2D;
            double vy = (look.y + rand.nextGaussian() * 0.2D) * 0.2D;
            double vz = (look.z + rand.nextGaussian() * 0.2D) * 0.2D;

            BlockState state;
            if ("blood".equals(mode)) {
                state = Blocks.REDSTONE_BLOCK.defaultBlockState();
            } else {
                state = rand.nextBoolean()
                        ? Blocks.LIME_TERRACOTTA.defaultBlockState()
                        : Blocks.GREEN_TERRACOTTA.defaultBlockState();
            }

            TerrainParticle fx = new TerrainParticle(level, ix, iy, iz, vx, vy, vz, state);
            fx.setLifetime(150 + rand.nextInt(50));
            mc.particleEngine.add(fx);
        }
    }
}
