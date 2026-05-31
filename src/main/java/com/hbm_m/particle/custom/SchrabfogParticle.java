package com.hbm_m.particle.custom;

import org.jetbrains.annotations.NotNull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Клиентская «schrabfog». Порт {@code ClientProxy#effectNT} type {@code schrabfog} + {@code EntityAuraFX} (1.7.10).
 */
public class SchrabfogParticle extends TextureSheetParticle {

    private static final double DRAG = 0.99D;

    protected SchrabfogParticle(ClientLevel level, double x, double y, double z,
                                double vx, double vy, double vz, SpriteSet spriteSet) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);

        this.hasPhysics = false;
        this.setSprite(spriteSet.get(0, 1));

        this.rCol = 0.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;
        this.alpha = 0.35F + this.random.nextFloat() * 0.25F;
        this.quadSize = 0.1F * (this.random.nextFloat() * 0.5F + 0.5F);

        this.xd = vx;
        this.yd = vy;
        this.zd = vz;

        this.lifetime = 48 + this.random.nextInt(24);
        if (vx == 0.0D && vy == 0.0D && vz == 0.0D) {
            this.xd = (this.random.nextDouble() - 0.5D) * 0.002D;
            this.yd = 0.001D + this.random.nextDouble() * 0.004D;
            this.zd = (this.random.nextDouble() - 0.5D) * 0.002D;
        }
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        this.move(this.xd, this.yd, this.zd);
        this.xd *= DRAG;
        this.yd *= DRAG;
        this.zd *= DRAG;

        if (this.age > this.lifetime / 2) {
            this.alpha = Math.max(0.0F, this.alpha - 0.02F);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        public Particle createParticle(@NotNull SimpleParticleType particleType, @NotNull ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new SchrabfogParticle(level, x, y, z, dx, dy, dz, this.spriteSet);
        }
    }
}
