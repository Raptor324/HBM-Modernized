package com.hbm_m.particle.custom;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

import org.jetbrains.annotations.NotNull;

/**
 * Port of 1.7.10 {@code ParticleCoolingTower} with the parameters {@code EntityMist} used for its
 * "tower" puffs (lift 0.5, base 0.75, max 2, life 50..60, alpha 0.25). The speed arguments carry the
 * fluid colour (r, g, b in 0..1) - SimpleParticleType has no payload.
 */
public class MistParticle extends TextureSheetParticle {

    private static final float BASE_SCALE = 0.75F;
    private static final float MAX_SCALE = 2.0F;
    private static final float LIFT = 0.5F;
    private static final float STRAFE = 0.075F;
    private static final float ALPHA_MOD = 0.25F;

    protected MistParticle(ClientLevel level, double x, double y, double z,
                           double r, double g, double b, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.pickSprite(sprites);
        this.lifetime = 50 + this.random.nextInt(10);
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
        this.rCol = (float) r;
        this.gCol = (float) g;
        this.bCol = (float) b;
        this.alpha = ALPHA_MOD;
        this.quadSize = BASE_SCALE;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        float ageScale = (float) this.age / (float) this.lifetime;
        this.alpha = ALPHA_MOD - ageScale * ALPHA_MOD;
        this.quadSize = BASE_SCALE + (float) Math.pow(MAX_SCALE * ageScale - BASE_SCALE, 2);

        if (++this.age >= this.lifetime) {
            this.remove();
            return;
        }

        if (this.yd < LIFT) {
            this.yd += 0.01F;
        }
        this.xd += this.random.nextGaussian() * STRAFE * ageScale;
        this.zd += this.random.nextGaussian() * STRAFE * ageScale;
        // Original "wind": a constant drift, not tied to any weather.
        this.xd += 0.02 * ageScale;
        this.zd -= 0.01 * ageScale;

        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.925;
        this.yd *= 0.925;
        this.zd *= 0.925;
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
                                       double x, double y, double z, double r, double g, double b) {
            return new MistParticle(level, x, y, z, r, g, b, this.sprites);
        }
    }
}
