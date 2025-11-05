package com.hbm_m.particle.explosions;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ShockwaveRingParticle extends TextureSheetParticle {

    private final SpriteSet spriteSet;

    protected ShockwaveRingParticle(ClientLevel level, double x, double y, double z,
                                    double xSpeed, double ySpeed, double zSpeed,
                                    SpriteSet spriteSet) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.spriteSet = spriteSet;

        // Настройки волны
        this.lifetime = 180;
        this.gravity = 0.0F;

        // Начальный размер и движение
        this.quadSize = 2.0F;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;


        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public void tick() {
        super.tick();

        // Расширяется и затухает
        float agePercent = (float)this.age / (float)this.lifetime;
        this.alpha = 1.0F - agePercent;

        // Замедляется со временем
        this.xd *= 0.95;
        this.zd *= 0.95;

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
            return new ShockwaveRingParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
        }
    }
}