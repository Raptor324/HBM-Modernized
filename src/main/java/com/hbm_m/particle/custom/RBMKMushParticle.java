package com.hbm_m.particle.custom;

import com.hbm_m.client.render.ImmediateVertexWriter;
import com.hbm_m.particle.AdditiveParticleRenderType;

import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * 1:1 port of {@code ParticleRBMKMush} - the mushroom cloud thrown up by a meltdown.
 *
 * <p>{@code rbmk_mush.png} is an 80x2880 vertical filmstrip: thirty frames of 80x96 stacked on
 * top of each other. Handing the whole column to the engine as a single sprite is what produced
 * the tall black slab with the cloud smeared down it. The original steps through the thirty
 * frames over a fifty-tick life and draws them additively, both reproduced here.</p>
 *
 * <p>The quad is anchored so it sits on top of the spawn point rather than through it (the
 * original adds {@code particleScale} to the interpolated Y), and its horizontal UVs run 1 to 0,
 * i.e. the frame is mirrored.</p>
 */
public class RBMKMushParticle extends TextureSheetParticle {

    private static final int SEGMENTS = 30;

    protected RBMKMushParticle(ClientLevel level, double x, double y, double z,
                               double scale, SpriteSet sprites) {
        super(level, x, y, z, 0, 0, 0);

        this.quadSize = (float) (scale > 0 ? scale : 10.0);
        this.lifetime = 50; // the original's maxAge, and the divisor for the frame counter
        this.gravity = 0F;
        this.friction = 1F;
        this.alpha = 1F;

        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        // The original's onUpdate only advances the age - the cloud neither drifts nor grows.
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) this.remove();
    }

    /** {@code int prog = age * segs / maxAge;} */
    private int frameRow() {
        return Math.min(this.age * SEGMENTS / this.lifetime, SEGMENTS - 1);
    }

    @Override
    public int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTick) {
        Vec3 cam = camera.getPosition();
        float relX = (float) (Mth.lerp(partialTick, this.xo, this.x) - cam.x());
        float relY = (float) (Mth.lerp(partialTick, this.yo, this.y) - cam.y()) + this.quadSize;
        float relZ = (float) (Mth.lerp(partialTick, this.zo, this.z) - cam.z());

        // Unlike the flame and the jet, the mushroom uses the full camera-facing basis.
        Quaternionf rotation = camera.rotation();
        float s = this.quadSize;
        Vector3f[] corners = new Vector3f[]{
                new Vector3f(-s, -s, 0F),
                new Vector3f(-s, s, 0F),
                new Vector3f(s, s, 0F),
                new Vector3f(s, -s, 0F)
        };
        for (Vector3f corner : corners) {
            corner.rotate(rotation);
            corner.add(relX, relY, relZ);
        }

        float spriteV0 = this.sprite.getV0();
        float spriteSpan = this.sprite.getV1() - spriteV0;
        int row = frameRow();
        float vMin = spriteV0 + spriteSpan * (row / (float) SEGMENTS);
        float vMax = spriteV0 + spriteSpan * ((row + 1) / (float) SEGMENTS);

        // Original UVs are (1, vMax), (1, vMin), (0, vMin), (0, vMax) - horizontally mirrored,
        // which worldQuad's (u1,v1),(u1,v0),(u0,v0),(u0,v1) corner order reproduces with
        // u1 = the sprite's right edge.
        ImmediateVertexWriter.worldQuad(buffer,
                corners[0].x(), corners[0].y(), corners[0].z(),
                corners[1].x(), corners[1].y(), corners[1].z(),
                corners[2].x(), corners[2].y(), corners[2].z(),
                corners[3].x(), corners[3].y(), corners[3].z(),
                1F, 1F, 1F, this.alpha,
                this.sprite.getU0(), vMin, this.sprite.getU1(), vMax);
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return AdditiveParticleRenderType.INSTANCE;
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
