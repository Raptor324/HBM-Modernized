package com.hbm_m.particle.explosions;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

public class ShockwaveParticle extends TextureSheetParticle {

    protected ShockwaveParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, 0, 0, 0);
        this.lifetime = 25; // 1.25 секунды
        this.gravity = 0;
        this.hasPhysics = false;

        this.quadSize = 1.0F;

        // Серо-белый цвет
        this.rCol = 0.8F;
        this.gCol = 0.8F;
        this.bCol = 0.8F;
        this.alpha = 0.9F;
    }

    @Override
    public void tick() {
        super.tick();

        // Расширение волны
        this.quadSize += 5.5F;

        // Постепенное затухание
        float fadeProgress = (float) this.age / (float) this.lifetime;
        this.alpha = 0.5F * (1.0F - fadeProgress);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
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
            ShockwaveParticle particle = new ShockwaveParticle(level, x, y, z);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
}