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
 * 1:1 port of {@code ParticleRBMKFlame} - the long-lived flame plume rising off burning and
 * radiating RBMK rubble.
 *
 * <p>The original picks a random scale between 1 and 2, lives for the age the spawner asks for
 * (300 ticks for debris), drifts upward while wobbling sideways, and fades out over its final
 * 20 ticks. The quad is drawn twice as tall as it is wide, which is what gives the plume its
 * stretched, licking look rather than a round puff.</p>
 */
public class RBMKFlameParticle extends TextureSheetParticle {

    private static final int FADE_TICKS = 20;

    protected RBMKFlameParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, 0, 0, 0);

        this.quadSize = this.random.nextFloat() + 1F;
        this.lifetime = 300;
        this.gravity = 0F;
        this.friction = 1F;
        this.alpha = 1F;

        this.xd = (this.random.nextDouble() - 0.5) * 0.01;
        this.yd = 0.02 + this.random.nextDouble() * 0.02;
        this.zd = (this.random.nextDouble() - 0.5) * 0.01;

        this.setSpriteFromAge(sprites);
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();

        // Slight sideways wander so a column of flames never looks like a straight pipe.
        this.xd += (this.random.nextDouble() - 0.5) * 0.002;
        this.zd += (this.random.nextDouble() - 0.5) * 0.002;

        int remaining = this.lifetime - this.age;
        if (remaining < FADE_TICKS) this.alpha = remaining / (float) FADE_TICKS;
    }

    /** The original stretches the flame quad to double height. */
    @Override
    public float getQuadSize(float partial) {
        return this.quadSize * 2F;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z, double xd, double yd, double zd) {
            return new RBMKFlameParticle(level, x, y, z, sprites);
        }
    }
}
