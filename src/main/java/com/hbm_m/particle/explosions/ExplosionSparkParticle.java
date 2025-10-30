package com.hbm_m.particle.explosions;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

public class ExplosionSparkParticle extends TextureSheetParticle {

    protected ExplosionSparkParticle(ClientLevel level, double x, double y, double z,
                                     double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);

        this.lifetime = 20 + this.random.nextInt(20); // 1-2 секунды
        this.gravity = 0.3F;
        this.hasPhysics = false;

        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;

        this.quadSize = 0.3F + this.random.nextFloat() * 0.3F;

        // Оранжево-жёлтый цвет
        this.rCol = 1.0F;
        this.gCol = 0.6F + this.random.nextFloat() * 0.3F;
        this.bCol = 0.1F;
        this.alpha = 1.0F;
    }

    @Override
    public void tick() {
        super.tick();

        // Затухание
        float fadeProgress = (float) this.age / (float) this.lifetime;
        this.alpha = 1.0F - fadeProgress;

        // Уменьшение размера
        this.quadSize *= 0.96F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            ExplosionSparkParticle particle = new ExplosionSparkParticle(
                    level, x, y, z, xSpeed, ySpeed, zSpeed
            );
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
}