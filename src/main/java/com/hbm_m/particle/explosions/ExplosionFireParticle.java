package com.hbm_m.particle.explosions;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ExplosionFireParticle extends TextureSheetParticle {

    private final SpriteSet spriteSet;

    protected ExplosionFireParticle(ClientLevel level, double x, double y, double z,
                                    double xSpeed, double ySpeed, double zSpeed,
                                    SpriteSet spriteSet) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.spriteSet = spriteSet;

        // Настройки огня
        this.lifetime = 80 + this.random.nextInt(110);
        this.gravity = 0F; // Поднимается вверх
        this.friction = 0.9F;

        this.quadSize = 2.0F + this.random.nextFloat();

        this.xd = xSpeed;
        this.yd = ySpeed + 0.1;
        this.zd = zSpeed;


        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public void tick() {
        super.tick();

        // Затухает и становится краснее
        float agePercent = (float)this.age / (float)this.lifetime;
        this.alpha = 1.0F - agePercent;
        this.gCol = 0.6F * (1.0F - agePercent);

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
            return new ExplosionFireParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
        }
    }
}