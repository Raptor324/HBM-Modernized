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

import java.util.Random;

/**
 * Port of 1.7.10 {@code ParticleContrail}: lingering gray condensation trail behind rocket exhaust.
 * Stays in the air much longer than {@link MissileContrailParticle}, spreads and fades slowly.
 */
public class MissileVaporContrailParticle extends TextureSheetParticle {

    public static float currentSpawnScale = 1.0F;

    private static final int SUB_QUADS = 6;

    private final float[] layerR = new float[SUB_QUADS];
    private final float[] layerG = new float[SUB_QUADS];
    private final float[] layerB = new float[SUB_QUADS];
    /** Pre-scaled gaussian sample: {@code nextGaussian() * 0.5F}. */
    private final float[] layerGaussX = new float[SUB_QUADS];
    private final float[] layerGaussY = new float[SUB_QUADS];
    private final float[] layerGaussZ = new float[SUB_QUADS];
    private final Vector3f[] cornerScratch = new Vector3f[]{
            new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()
    };

    private float cachedU0;
    private float cachedU1;
    private float cachedV0;
    private float cachedV1;
    /** {@code (pow(ageRatio * 3.5, 1.4) + 0.6) * quadSize} — screen scale applied in render. */
    private float cachedSpreadBase;
    private int cachedLight = LightTexture.FULL_BRIGHT;

    protected MissileVaporContrailParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.lifetime = 280 + this.random.nextInt(120);
        this.quadSize = currentSpawnScale;
        float gray = 0.42F + this.random.nextFloat() * 0.12F;
        this.rCol = gray;
        this.gCol = gray;
        this.bCol = gray + 0.04F;
        this.alpha = 0.7F;
        this.hasPhysics = false;
        this.pickSprite(sprites);
        this.cacheSpriteUv();
        this.precomputeRenderLayers(new Random(this.random.nextLong()));
        this.updateRenderCaches();
    }

    private void cacheSpriteUv() {
        this.cachedU0 = this.getU0();
        this.cachedU1 = this.getU1();
        this.cachedV0 = this.getV0();
        this.cachedV1 = this.getV1();
    }

    private void precomputeRenderLayers(Random subRand) {
        for (int layer = 0; layer < SUB_QUADS; layer++) {
            float mod = subRand.nextFloat() * 0.2F + 0.15F;
            this.layerR[layer] = Math.min(1.0F, this.rCol + mod);
            this.layerG[layer] = Math.min(1.0F, this.gCol + mod);
            this.layerB[layer] = Math.min(1.0F, this.bCol + mod);
            this.layerGaussX[layer] = (float) (subRand.nextGaussian() * 0.5D);
            this.layerGaussY[layer] = (float) (subRand.nextGaussian() * 0.5D);
            this.layerGaussZ[layer] = (float) (subRand.nextGaussian() * 0.5D);
        }
    }

    private void updateRenderCaches() {
        float ageRatio = Math.min((float) this.age / (float) this.lifetime, 1.0F);
        this.alpha = (float) (Math.pow(1.0F - ageRatio, 0.35D) * 0.72F);
        this.cachedSpreadBase = (float) (Math.pow(ageRatio * 3.5F, 1.4D) + 0.6F) * this.quadSize;
        BlockPos pos = BlockPos.containing(this.x, this.y, this.z);
        this.cachedLight = this.level.hasChunkAt(pos)
                ? LevelRenderer.getLightColor(this.level, pos)
                : LightTexture.FULL_BRIGHT;
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
        this.quadSize = currentSpawnScale * (0.85F + ageRatio * 2.2F);

        // Gentle drift — contrail spreads sideways like aircraft vapor (1.7.10 contrail has no motion, only render spread).
        this.xd += (this.random.nextFloat() - 0.5F) * 0.004F;
        this.yd += (this.random.nextFloat() - 0.5F) * 0.002F;
        this.zd += (this.random.nextFloat() - 0.5F) * 0.004F;
        this.xd *= 0.96D;
        this.yd *= 0.96D;
        this.zd *= 0.96D;
        this.move(this.xd, this.yd, this.zd);
        this.updateRenderCaches();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return LongRangeParticleRenderType.INSTANCE;
    }

    @Override
    public boolean shouldCull() {
        return false;
    }

    @Override
    public int getLightColor(float partialTick) {
        return this.cachedLight;
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
        float screenScale = virtual.screenScale();
        float spreadMul = this.cachedSpreadBase * screenScale;
        float scale = (this.alpha + 0.5F) * this.quadSize * screenScale;
        float alpha = this.alpha;
        int light = this.cachedLight;
        Quaternionf rotation = camera.rotation();

        for (int layer = 0; layer < SUB_QUADS; layer++) {
            float px = relX + this.layerGaussX[layer] * spreadMul;
            float py = relY + this.layerGaussY[layer] * spreadMul;
            float pz = relZ + this.layerGaussZ[layer] * spreadMul;

            this.cornerScratch[0].set(-scale, -scale, 0.0F);
            this.cornerScratch[1].set(-scale, scale, 0.0F);
            this.cornerScratch[2].set(scale, scale, 0.0F);
            this.cornerScratch[3].set(scale, -scale, 0.0F);
            for (Vector3f corner : this.cornerScratch) {
                corner.rotate(rotation);
                corner.add(px, py, pz);
            }

            float r = this.layerR[layer];
            float g = this.layerG[layer];
            float b = this.layerB[layer];
            buffer.vertex(this.cornerScratch[0].x(), this.cornerScratch[0].y(), this.cornerScratch[0].z())
                    .uv(this.cachedU1, this.cachedV1).color(r, g, b, alpha).uv2(light).endVertex();
            buffer.vertex(this.cornerScratch[1].x(), this.cornerScratch[1].y(), this.cornerScratch[1].z())
                    .uv(this.cachedU1, this.cachedV0).color(r, g, b, alpha).uv2(light).endVertex();
            buffer.vertex(this.cornerScratch[2].x(), this.cornerScratch[2].y(), this.cornerScratch[2].z())
                    .uv(this.cachedU0, this.cachedV0).color(r, g, b, alpha).uv2(light).endVertex();
            buffer.vertex(this.cornerScratch[3].x(), this.cornerScratch[3].y(), this.cornerScratch[3].z())
                    .uv(this.cachedU0, this.cachedV1).color(r, g, b, alpha).uv2(light).endVertex();
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
