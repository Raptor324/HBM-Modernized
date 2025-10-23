package com.hbm_m.particle.explosions;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MushroomCapSmokeParticle extends TextureSheetParticle {

    private final SpriteSet spriteSet;

    protected MushroomCapSmokeParticle(ClientLevel level, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed,
                                       SpriteSet spriteSet) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.spriteSet = spriteSet;

        // Настройки дыма шляпки - более густой и медленный
        this.lifetime = 80 + this.random.nextInt(130);
        this.gravity = -0.005F; // Почти не поднимается
        this.friction = 0.98F;

        this.quadSize = 4.0F + this.random.nextFloat() * 3.0F;

        this.xd = xSpeed + (this.random.nextDouble() - 0.5) * 0.05;
        this.yd = ySpeed + 0.02;
        this.zd = zSpeed + (this.random.nextDouble() - 0.5) * 0.05;


        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public void tick() {
        super.tick();

        // Медленно расширяется и затухает
        float agePercent = (float)this.age / (float)this.lifetime;
        this.quadSize += 0.03F;
        this.alpha = 1.0F - (agePercent * agePercent); // Квадратичное затухание

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
            return new MushroomCapSmokeParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
        }
    }
}