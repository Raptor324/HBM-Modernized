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
 * 1:1 port of {@code ParticleDigammaSmoke} - the dark red pall the digamma spear drags along
 * behind it and blows out when it finally lands.
 *
 * <p>A big, slow, plain quad on {@code particle_base}: scale 5, no gravity, no collision, a 0.99
 * drag on all three axes, and an alpha that walks linearly to zero over a 100-140 tick life.</p>
 */
public class DigammaSmokeParticle extends TextureSheetParticle {

    private static final double DRAG = 0.99D;

    protected DigammaSmokeParticle(ClientLevel level, double x, double y, double z,
                                   double vx, double vy, double vz, SpriteSet spriteSet) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);

        this.hasPhysics = false;
        this.setSprite(spriteSet.get(0, 1));

        this.lifetime = 100 + this.random.nextInt(40);
        this.gravity = 0F;
        this.quadSize = 5F;

        this.rCol = 0.5F + this.random.nextFloat() * 0.2F;
        this.gCol = 0F;
        this.bCol = 0F;
        this.alpha = 1F;

        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        this.alpha = 1F - ((float) this.age / (float) this.lifetime);

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        this.xd *= DRAG;
        this.yd *= DRAG;
        this.zd *= DRAG;
        this.move(this.xd, this.yd, this.zd);
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
            return new DigammaSmokeParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}
