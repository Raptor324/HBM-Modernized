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
 * 1:1 port of {@code ParticleRBMKSteam} - the short, wide steam jet vented by an overfilled
 * boiler column.
 *
 * <p>Like the flame, {@code rbmk_jet_steam.png} is an animation strip (640x64, twenty columns)
 * on an opaque black backdrop, so it needs per-frame UVs and additive blending rather than being
 * handed to the engine as one sprite.</p>
 */
public class RBMKSteamParticle extends TextureSheetParticle {

    private static final int STRIP_COLUMNS = 20;

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

        this.pickSprite(sprites);
    }

    /**
     * {@code (int)((age / maxAge) * 20) % 20 - 1}. At age 0 the original lands on -1, which on a
     * standalone 1.7.10 texture simply wrapped; on a stitched atlas it would bleed into whatever
     * sprite happens to sit to the left, so the first frame is clamped instead.
     */
    private int frameColumn() {
        int clamped = Math.min(this.age, this.lifetime);
        int index = (int) (((double) clamped / (double) this.lifetime) * STRIP_COLUMNS) % STRIP_COLUMNS - 1;
        return Math.max(index, 0);
    }

    @Override
    public int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTick) {
        Vec3 cam = camera.getPosition();
        float relX = (float) (Mth.lerp(partialTick, this.xo, this.x) - cam.x());
        float relY = (float) (Mth.lerp(partialTick, this.yo, this.y) - cam.y());
        float relZ = (float) (Mth.lerp(partialTick, this.zo, this.z) - cam.z());

        Quaternionf yawOnly = new Quaternionf().rotationY(-camera.getYRot() * Mth.DEG_TO_RAD);

        // The jet hangs off to one side and grows upward from just below the spawn point:
        // x spans scale*-0.25-0.9375 .. scale*0.25-0.9375, y spans -0.25 .. scale-0.25.
        float s = this.quadSize;
        float xMin = s * -0.25F - 0.9375F, xMax = s * 0.25F - 0.9375F;
        float yMin = -0.25F, yMax = s - 0.25F;
        Vector3f[] corners = new Vector3f[]{
                new Vector3f(xMin, yMin, 0F),
                new Vector3f(xMin, yMax, 0F),
                new Vector3f(xMax, yMax, 0F),
                new Vector3f(xMax, yMin, 0F)
        };
        for (Vector3f corner : corners) {
            corner.rotate(yawOnly);
            corner.add(relX, relY, relZ);
        }

        float spriteU0 = this.sprite.getU0();
        float spriteSpan = this.sprite.getU1() - spriteU0;
        int column = frameColumn();
        float uMin = spriteU0 + spriteSpan * (column / (float) STRIP_COLUMNS);
        float uMax = spriteU0 + spriteSpan * ((column + 1) / (float) STRIP_COLUMNS);

        ImmediateVertexWriter.worldQuad(buffer,
                corners[0].x(), corners[0].y(), corners[0].z(),
                corners[1].x(), corners[1].y(), corners[1].z(),
                corners[2].x(), corners[2].y(), corners[2].z(),
                corners[3].x(), corners[3].y(), corners[3].z(),
                1F, 1F, 1F, this.alpha,
                uMin, this.sprite.getV0(), uMax, this.sprite.getV1());
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
            return new RBMKSteamParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}
