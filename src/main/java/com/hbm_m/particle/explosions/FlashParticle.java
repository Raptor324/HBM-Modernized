package com.hbm_m.particle.explosions;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

public class FlashParticle extends TextureSheetParticle {

    protected FlashParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, 0, 0, 0);
        this.lifetime = 8; // 0.4 секунды при 20 тиках
        this.gravity = 0;
        this.hasPhysics = false;

        // Огромный размер для вспышки
        this.quadSize = 3.0F;

        // Яркий белый цвет
        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;
        this.alpha = 1.0F;
    }

    @Override
    public void tick() {
        super.tick();

        // Быстрое затухание
        this.alpha = 1.0F - ((float) this.age / (float) this.lifetime);

        // Увеличение размера
        this.quadSize += 2.0F;
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
            FlashParticle particle = new FlashParticle(level, x, y, z);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
}