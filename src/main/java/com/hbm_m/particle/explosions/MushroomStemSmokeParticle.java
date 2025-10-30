package com.hbm_m.particle.explosions;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MushroomStemSmokeParticle extends TextureSheetParticle {

    private final SpriteSet spriteSet;

    protected MushroomStemSmokeParticle(ClientLevel level, double x, double y, double z,
                                        double xSpeed, double ySpeed, double zSpeed,
                                        SpriteSet spriteSet) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.spriteSet = spriteSet;

        // Настройки дыма ножки
        this.lifetime = 80 + this.random.nextInt(120);
        this.gravity = -0.1F; // Медленно поднимается
        this.friction = 0.96F;

        this.quadSize = 3.0F + this.random.nextFloat() * 2.0F;

        this.xd = xSpeed + (this.random.nextDouble() - 0.5) * 0.1;
        this.yd = ySpeed + 1;
        this.zd = zSpeed + (this.random.nextDouble() - 0.5) * 0.1;



        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public void tick() {
        super.tick();

        // Расширяется и затухает
        float agePercent = (float)this.age / (float)this.lifetime;
        this.quadSize += 0.005F;
        this.alpha = 0.9F * (1.0F - agePercent);

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
            return new MushroomStemSmokeParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
        }
    }
}