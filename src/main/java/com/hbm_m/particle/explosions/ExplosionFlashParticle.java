package com.hbm_m.particle.explosions;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ExplosionFlashParticle extends TextureSheetParticle {

    private final SpriteSet spriteSet;

    protected ExplosionFlashParticle(ClientLevel level, double x, double y, double z,
                                     double xSpeed, double ySpeed, double zSpeed,
                                     SpriteSet spriteSet) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.spriteSet = spriteSet;

        // Настройки вспышки
        this.lifetime = 80; // Быстро исчезает
        this.gravity = 0.0F; // Не падает
        this.friction = 1.0F; // Не двигается

        // Большой размер для яркой вспышки
        this.quadSize = 15.0F;



        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public void tick() {
        super.tick();

        // Быстро уменьшаем яркость и размер
        this.alpha = 10.0F - ((float)this.age / (float)this.lifetime);
        this.quadSize = 15.0F * this.alpha;

        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new ExplosionFlashParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
        }
    }
}