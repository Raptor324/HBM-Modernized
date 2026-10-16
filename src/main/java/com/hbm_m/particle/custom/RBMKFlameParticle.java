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
 * 1:1 port of {@code ParticleRBMKFlame} - the long-lived flame plume rising off burning and
 * radiating RBMK rubble.
 *
 * <p>{@code rbmk_fire.png} is a 448x64 animation strip, not a single frame. The previous version
 * handed the whole strip to the particle engine as one sprite, so every flame rendered as a wide
 * black rectangle with the entire filmstrip painted across it. The original walks the strip by
 * UV instead: it treats the texture as fourteen columns and cycles through the first five of
 * them, which is what {@link #frameColumn()} reproduces.</p>
 *
 * <p>The sheet is also fully opaque with a black backdrop, so it only looks like fire under
 * additive blending - see {@link AdditiveParticleRenderType}.</p>
 */
public class RBMKFlameParticle extends TextureSheetParticle {

    /** The original's {@code f0 = 1F / 14F}: the strip is addressed as fourteen columns. */
    private static final int STRIP_COLUMNS = 14;
    /** ...but {@code texIndex % 5} means only the first five are ever sampled. */
    private static final int USED_COLUMNS = 5;
    private static final int FADE_TICKS = 20;

    protected RBMKFlameParticle(ClientLevel level, double x, double y, double z, int maxAge, SpriteSet sprites) {
        super(level, x, y, z, 0, 0, 0);

        this.quadSize = this.random.nextFloat() + 1F;
        this.lifetime = maxAge;
        this.gravity = 0F;
        this.friction = 1F;
        this.alpha = 1F;

        this.xd = (this.random.nextDouble() - 0.5) * 0.01;
        this.yd = 0.02 + this.random.nextDouble() * 0.02;
        this.zd = (this.random.nextDouble() - 0.5) * 0.01;

        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();

        // Slight sideways wander so a column of flames never looks like a straight pipe.
        this.xd += (this.random.nextDouble() - 0.5) * 0.002;
        this.zd += (this.random.nextDouble() - 0.5) * 0.002;

        // The original fades in as well as out, then halves the result across the whole life.
        int clamped = Math.min(this.age, this.lifetime);
        float a = 1F;
        if (clamped < FADE_TICKS) a = clamped / (float) FADE_TICKS;
        if (clamped > this.lifetime - FADE_TICKS) a = (this.lifetime - clamped) / (float) FADE_TICKS;
        this.alpha = a * 0.5F;
    }

    /** {@code int texIndex = this.particleAge * 5 % 14;} then {@code texIndex % 5}. */
    private int frameColumn() {
        int clamped = Math.min(this.age, this.lifetime);
        return (clamped * USED_COLUMNS % STRIP_COLUMNS) % USED_COLUMNS;
    }

    @Override
    public int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT; // tess.setBrightness(240)
    }

    /**
     * The original rotates the quad by the camera yaw only, never by pitch, so the plume always
     * stands upright instead of tipping over to face a player looking down at it.
     */
    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTick) {
        Vec3 cam = camera.getPosition();
        float relX = (float) (Mth.lerp(partialTick, this.xo, this.x) - cam.x());
        float relY = (float) (Mth.lerp(partialTick, this.yo, this.y) - cam.y());
        float relZ = (float) (Mth.lerp(partialTick, this.zo, this.z) - cam.z());

        Quaternionf yawOnly = new Quaternionf().rotationY(-camera.getYRot() * Mth.DEG_TO_RAD);

        // addVertexWithUV: x spans -(scale+1)..(scale-1), y spans +-scale*2.
        float s = this.quadSize;
        float xMin = -s - 1F, xMax = s - 1F, yMin = -s * 2F, yMax = s * 2F;
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

        // worldQuad emits (u1,v1),(u1,v0),(u0,v0),(u0,v1) in corner order, and the original's
        // first corner carries uMax - so uMax goes in the u1 slot.
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
            // The spawner passes maxAge through the x velocity slot; debris uses 300.
            int maxAge = xd > 0 ? (int) xd : 300;
            return new RBMKFlameParticle(level, x, y, z, maxAge, sprites);
        }
    }
}
