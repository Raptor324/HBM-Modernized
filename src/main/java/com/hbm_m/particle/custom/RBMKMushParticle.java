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
 * 1:1 port of {@code ParticleRBMKMush} - the mushroom cloud billow, used by the meltdown and by
 * the original's large ordnance.
 *
 * <p>The original is anchored so the sprite sits on top of its spawn point rather than centred on
 * it (it offsets the quad upward by its own scale), which is what makes the cloud appear to grow
 * out of the ground instead of through it; {@link #getQuadSize} plus the upward drift reproduce
 * that here.</p>
 */
public class RBMKMushParticle extends TextureSheetParticle {

    protected RBMKMushParticle(ClientLevel level, double x, double y, double z,
                               double scale, SpriteSet sprites) {
        super(level, x, y, z, 0, 0, 0);

        this.quadSize = (float) (scale > 0 ? scale : 10.0);
        this.lifetime = 200;
        this.gravity = 0F;
        this.friction = 1F;
        this.alpha = 1F;

        this.yd = 0.01;

        this.setSpriteFromAge(sprites);
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        this.quadSize *= 1.006F;

        int remaining = this.lifetime - this.age;
        if (remaining < 40) this.alpha = remaining / 40F;
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
            // The spawner passes the cloud's scale through the x velocity slot, the same way the
            // port's other size-carrying particles do.
            return new RBMKMushParticle(level, x, y, z, xd, sprites);
        }
    }
}
