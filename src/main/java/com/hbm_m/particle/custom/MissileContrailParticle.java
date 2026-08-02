package com.hbm_m.particle.custom;

import com.hbm_m.client.missile.track.MissileTrackWorldRender;
import com.hbm_m.particle.LongRangeParticleRenderType;
import com.hbm_m.particle.ModParticleTypes;
import com.mojang.blaze3d.vertex.VertexConsumer;

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
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Random;

/**
 * Port of 1.7.10 {@code ParticleRocketFlame} (missileContrail effect): orange exhaust,
 * motion damping, multi-layer billboard, particle atlas alpha blend (FX layer 1).
 */
public class MissileContrailParticle extends TextureSheetParticle {

    /** Set by {@link com.hbm_m.entity.missile.MissileBaseEntity} before spawning contrail particles. */
    public static float currentSpawnScale = 1.0F;

    private static final int SUB_QUADS = 10;
    private static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT;

    private final SpriteSet sprites;
    private boolean vaporSpawned;

    private final float[] layerAdd = new float[SUB_QUADS];
    private final float[] layerScaleRand = new float[SUB_QUADS];
    /** Pre-offset gaussian: {@code (nextGaussian() - 1.0) * axisSpread}. */
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
    private float cachedDark = 1.0F;
    private float cachedAgeRatio;
    /** {@code (pow(ageRatio * 4, 1.5) + 1) * quadSize} — screen scale applied in render. */
    private float cachedSpreadBase;

    protected MissileContrailParticle(ClientLevel level, double x, double y, double z,
                                      double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z, dx, dy, dz);
        this.sprites = sprites;
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        this.lifetime = 60 + this.random.nextInt(20);
        this.quadSize = currentSpawnScale;
        this.pickSprite(sprites);
        this.rCol = 1.0F;
        this.gCol = 0.6F;
        this.bCol = 0.0F;
        this.alpha = 0.75F;
        this.cacheSpriteUv();
        this.precomputeRenderLayers(new Random(this.random.nextLong()));
        this.updateColorFromAge();
    }

    private void cacheSpriteUv() {
        this.cachedU0 = this.getU0();
        this.cachedU1 = this.getU1();
        this.cachedV0 = this.getV0();
        this.cachedV1 = this.getV1();
    }

    private void precomputeRenderLayers(Random subRand) {
        for (int layer = 0; layer < SUB_QUADS; layer++) {
            this.layerAdd[layer] = subRand.nextFloat() * 0.3F;
            this.layerScaleRand[layer] = subRand.nextFloat();
            this.layerGaussX[layer] = (float) ((subRand.nextGaussian() - 1.0D) * 0.2D);
            this.layerGaussY[layer] = (float) ((subRand.nextGaussian() - 1.0D) * 0.5D);
            this.layerGaussZ[layer] = (float) ((subRand.nextGaussian() - 1.0D) * 0.2D);
        }
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            spawnVaporContinuation();
            this.remove();
            return;
        }
        this.xd *= 0.91D;
        this.yd *= 0.91D;
        this.zd *= 0.91D;
        this.move(this.xd, this.yd, this.zd);
        updateColorFromAge();
    }

    /**
     * Gray vapor appears only after hot exhaust dies — extends the trail in time, not at the nozzle.
     */
    private void spawnVaporContinuation() {
        if (this.vaporSpawned || this.age <= 0) {
            return;
        }
        this.vaporSpawned = true;
        MissileVaporContrailParticle.currentSpawnScale = this.quadSize;
        try {
            this.level.addParticle(
                    ModParticleTypes.MISSILE_VAPOR_CONTRAIL.get(),
                    true,
                    this.x, this.y, this.z,
                    0.0D, 0.0D, 0.0D);
        } finally {
            MissileVaporContrailParticle.currentSpawnScale = 1.0F;
        }
    }

    /** 1.7.10 {@code ParticleRocketFlame}: orange flame early, darkens with age. */
    private void updateColorFromAge() {
        this.cachedDark = 1.0F - Math.min((float) this.age / (this.lifetime * 0.25F), 1.0F);
        this.rCol = this.cachedDark;
        this.gCol = 0.6F * this.cachedDark;
        this.bCol = 0.0F;
        this.cachedAgeRatio = Math.min((float) this.age / (float) this.lifetime, 1.0F);
        this.alpha = (float) (Math.pow(1.0F - this.cachedAgeRatio, 0.5) * 0.75F);
        this.cachedSpreadBase = (float) (Math.pow(this.cachedAgeRatio * 4.0F, 1.5D) + 1.0F) * this.quadSize;
    }

    /**
     * 1.7.10 FX layer 1 used particle atlas + alpha blend (not emissive LIT shader).
     */
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
        return FULL_BRIGHT;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        Vec3 cam = camera.getPosition();
        double wx = Mth.lerp(partialTick, this.xo, this.x);
        double wy = Mth.lerp(partialTick, this.yo, this.y);
        double wz = Mth.lerp(partialTick, this.zo, this.z);
        double rx = wx - cam.x;
        double ry = wy - cam.y;
        double rz = wz - cam.z;
        double distSq = rx * rx + ry * ry + rz * rz;
        float screenScale = 1.0F;
        if (distSq > 9216.0D) {
            double max = MissileTrackWorldRender.maxSafeRenderDistanceBlocks();
            if (distSq > max * max) {
                double dist = Math.sqrt(distSq);
                screenScale = (float) (max / dist);
                rx *= screenScale;
                ry *= screenScale;
                rz *= screenScale;
            }
        }

        float relX = (float) rx;
        float relY = (float) ry;
        float relZ = (float) rz;
        float spread = this.cachedSpreadBase * screenScale;
        float dark = this.cachedDark;
        float ageRatio = this.cachedAgeRatio;
        int iAlpha = (int) (this.alpha * 255.0F);
        int lightU = FULL_BRIGHT & 0xFFFF;
        int lightV = FULL_BRIGHT >> 16 & 0xFFFF;
        float u0 = this.cachedU0;
        float u1 = this.cachedU1;
        float v0 = this.cachedV0;
        float v1 = this.cachedV1;
        Quaternionf rotation = camera.rotation();

        this.cornerScratch[0].set(-1.0F, -1.0F, 0.0F);
        this.cornerScratch[1].set(-1.0F, 1.0F, 0.0F);
        this.cornerScratch[2].set(1.0F, 1.0F, 0.0F);
        this.cornerScratch[3].set(1.0F, -1.0F, 0.0F);
        for (int i = 0; i < 4; i++) {
            this.cornerScratch[i].rotate(rotation);
        }

        float cx0 = this.cornerScratch[0].x(), cy0 = this.cornerScratch[0].y(), cz0 = this.cornerScratch[0].z();
        float cx1 = this.cornerScratch[1].x(), cy1 = this.cornerScratch[1].y(), cz1 = this.cornerScratch[1].z();
        float cx2 = this.cornerScratch[2].x(), cy2 = this.cornerScratch[2].y(), cz2 = this.cornerScratch[2].z();
        float cx3 = this.cornerScratch[3].x(), cy3 = this.cornerScratch[3].y(), cz3 = this.cornerScratch[3].z();

        for (int layer = 0; layer < SUB_QUADS; layer++) {
            float add = this.layerAdd[layer];
            int ir = (int) (Math.min(1.0F, dark + add) * 255.0F);
            int ig = (int) (Math.min(1.0F, 0.6F * dark + add) * 255.0F);
            int ib = (int) (add * 255.0F);

            float scale = (this.layerScaleRand[layer] * 0.5F + 0.1F + ageRatio * 2.0F) * this.quadSize * screenScale;
            float px = relX + this.layerGaussX[layer] * spread;
            float py = relY + this.layerGaussY[layer] * spread;
            float pz = relZ + this.layerGaussZ[layer] * spread;

            buffer.vertex(px + cx0 * scale, py + cy0 * scale, pz + cz0 * scale)
                    .uv(u1, v1).color(ir, ig, ib, iAlpha).uv2(lightU, lightV).endVertex();
            buffer.vertex(px + cx1 * scale, py + cy1 * scale, pz + cz1 * scale)
                    .uv(u1, v0).color(ir, ig, ib, iAlpha).uv2(lightU, lightV).endVertex();
            buffer.vertex(px + cx2 * scale, py + cy2 * scale, pz + cz2 * scale)
                    .uv(u0, v0).color(ir, ig, ib, iAlpha).uv2(lightU, lightV).endVertex();
            buffer.vertex(px + cx3 * scale, py + cy3 * scale, pz + cz3 * scale)
                    .uv(u0, v1).color(ir, ig, ib, iAlpha).uv2(lightU, lightV).endVertex();
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
            return new MissileContrailParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }
}
