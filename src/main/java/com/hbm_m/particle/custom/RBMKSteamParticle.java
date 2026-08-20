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
 * 1:1 port of {@code ParticleRBMKSteam} - the short, wide steam jet.
 *
 * <p>Only ten ticks long, drawn large and very faint (the original sets alpha to 0.25), and it
 * walks through its animation strip over that lifetime rather than picking one frame.</p>
 */
public class RBMKSteamParticle extends TextureSheetParticle {

    protected RBMKSteamParticle(ClientLevel level, double x, double y, double z,
                                double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, 0, 0, 0);

        this.lifetime = 10;
        this.alpha = 0.25F;
        this.quadSize = 4F;
        this.gravity = 0F;
        this.friction = 1F;

        this.xd = xd;
        this.yd = yd;
        this.zd = zd;

        this.setSpriteFromAge(sprites);
        this.pickSprite(sprites);
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
            return new RBMKSteamParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}
