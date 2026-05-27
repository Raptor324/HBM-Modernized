package com.hbm_m.particle.custom;

import com.hbm_m.client.missile.track.MissileTrackWorldRender;
import com.hbm_m.particle.LongRangeParticleRenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Port of 1.7.10 {@code ParticleContrail}: lingering gray condensation trail behind rocket exhaust.
 * Stays in the air much longer than {@link MissileContrailParticle}, spreads and fades slowly.
 */
public class MissileVaporContrailParticle extends TextureSheetParticle {

    public static float currentSpawnScale = 1.0F;

    private static final int SUB_QUADS = 6;

    private final long renderSeed;

    protected MissileVaporContrailParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.lifetime = 280 + this.random.nextInt(120);
        this.quadSize = currentSpawnScale;
        this.renderSeed = this.random.nextLong();
        float gray = 0.42F + this.random.nextFloat() * 0.12F;
        this.rCol = gray;
        this.gCol = gray;
        this.bCol = gray + 0.04F;
        this.alpha = 0.7F;
        this.hasPhysics = false;
        this.pickSprite(sprites);
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

        float ageRatio = (float) this.age / (float) this.lifetime;
        this.alpha = (float) (Math.pow(1.0F - ageRatio, 0.35D) * 0.72F);
        this.quadSize = currentSpawnScale * (0.85F + ageRatio * 2.2F);

        // Gentle drift — contrail spreads sideways like aircraft vapor (1.7.10 contrail has no motion, only render spread).
        this.xd += (this.random.nextFloat() - 0.5F) * 0.004F;
        this.yd += (this.random.nextFloat() - 0.5F) * 0.002F;
        this.zd += (this.random.nextFloat() - 0.5F) * 0.004F;
        this.xd *= 0.96D;
        this.yd *= 0.96D;
        this.zd *= 0.96D;
        this.move(this.xd, this.yd, this.zd);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return LongRangeParticleRenderType.INSTANCE;
    }

    @Override
    public boolean shouldCull() {
        return false;
    }

    /** Ambient light at particle position — gray vapor should dim at night (unlike hot exhaust). */
    private int sampleAmbientLight() {
        BlockPos pos = BlockPos.containing(this.x, this.y, this.z);
        if (!this.level.hasChunkAt(pos)) {
            return LightTexture.FULL_BRIGHT;
        }
        return LevelRenderer.getLightColor(this.level, pos);
    }

    @Override
    public int getLightColor(float partialTick) {
        return sampleAmbientLight();
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        Vec3 cam = camera.getPosition();
        double wx = Mth.lerp(partialTick, this.xo, this.x);
        double wy = Mth.lerp(partialTick, this.yo, this.y);
        double wz = Mth.lerp(partialTick, this.zo, this.z);
        MissileTrackWorldRender.CameraRelativePose virtual =
                MissileTrackWorldRender.virtualizeWorld(wx, wy, wz, cam);

        float relX = (float) virtual.relX();
        float relY = (float) virtual.relY();
        float relZ = (float) virtual.relZ();

        java.util.Random subRand = new java.util.Random(this.renderSeed);
        float ageRatio = Math.min((float) this.age / (float) this.lifetime, 1.0F);
        float alpha = (float) (Math.pow(1.0F - ageRatio, 0.35D) * 0.72F);
        float spreadMul = (float) (Math.pow(ageRatio * 3.5F, 1.4D) + 0.6F) * this.quadSize * virtual.screenScale();

        Quaternionf rotation = camera.rotation();

        for (int layer = 0; layer < SUB_QUADS; layer++) {
            float mod = subRand.nextFloat() * 0.2F + 0.15F;
            float r = Math.min(1.0F, this.rCol + mod);
            float g = Math.min(1.0F, this.gCol + mod);
            float b = Math.min(1.0F, this.bCol + mod);

            float scale = (alpha + 0.5F) * this.quadSize * virtual.screenScale();
            float px = relX + (float) (subRand.nextGaussian() * 0.5D * spreadMul);
            float py = relY + (float) (subRand.nextGaussian() * 0.5D * spreadMul);
            float pz = relZ + (float) (subRand.nextGaussian() * 0.5D * spreadMul);

            Vector3f[] corners = new Vector3f[]{
                    new Vector3f(-scale, -scale, 0.0F),
                    new Vector3f(-scale, scale, 0.0F),
                    new Vector3f(scale, scale, 0.0F),
                    new Vector3f(scale, -scale, 0.0F)
            };
            for (Vector3f corner : corners) {
                corner.rotate(rotation);
                corner.add(px, py, pz);
            }

            float u0 = this.getU0();
            float u1 = this.getU1();
            float v0 = this.getV0();
            float v1 = this.getV1();
            int light = sampleAmbientLight();
            buffer.vertex(corners[0].x(), corners[0].y(), corners[0].z()).uv(u1, v1)
                    .color(r, g, b, alpha).uv2(light).endVertex();
            buffer.vertex(corners[1].x(), corners[1].y(), corners[1].z()).uv(u1, v0)
                    .color(r, g, b, alpha).uv2(light).endVertex();
            buffer.vertex(corners[2].x(), corners[2].y(), corners[2].z()).uv(u0, v0)
                    .color(r, g, b, alpha).uv2(light).endVertex();
            buffer.vertex(corners[3].x(), corners[3].y(), corners[3].z()).uv(u0, v1)
                    .color(r, g, b, alpha).uv2(light).endVertex();
        }
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new MissileVaporContrailParticle(level, x, y, z, sprites);
        }
    }
}
