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

/**
 * Port of 1.7.10 {@code ParticleRocketFlame} (missileContrail effect): orange exhaust,
 * motion damping, multi-layer billboard, particle atlas alpha blend (FX layer 1).
 */
public class MissileContrailParticle extends TextureSheetParticle {

    /** Set by {@link com.hbm_m.entity.missile.MissileBaseEntity} before spawning contrail particles. */
    public static float currentSpawnScale = 1.0F;

    private static final int SUB_QUADS = 10;
    private final SpriteSet sprites;
    private final long renderSeed;
    private boolean vaporSpawned;

    protected MissileContrailParticle(ClientLevel level, double x, double y, double z,
                                      double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z, dx, dy, dz);
        this.sprites = sprites;
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        this.lifetime = 60 + this.random.nextInt(20);
        this.quadSize = currentSpawnScale;
        this.renderSeed = this.random.nextLong();
        this.pickSprite(sprites);
        this.rCol = 1.0F;
        this.gCol = 0.6F;
        this.bCol = 0.0F;
        this.alpha = 0.75F;
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
        float dark = 1.0F - Math.min((float) this.age / (this.lifetime * 0.25F), 1.0F);
        this.rCol = dark;
        this.gCol = 0.6F * dark;
        this.bCol = 0.0F;
        float ageRatio = Math.min((float) this.age / (float) this.lifetime, 1.0F);
        this.alpha = (float) (Math.pow(1.0F - ageRatio, 0.5) * 0.75F);
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
        return LightTexture.FULL_BRIGHT;
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
        float dark = 1.0F - Math.min(this.age / (this.lifetime * 0.25F), 1.0F);
        float alpha = (float) (Math.pow(1.0F - ageRatio, 0.5) * 0.75F);
        float spread = (float) (Math.pow(ageRatio * 4.0F, 1.5) + 1.0F) * this.quadSize * virtual.screenScale();

        Quaternionf rotation = camera.rotation();

        for (int layer = 0; layer < SUB_QUADS; layer++) {
            float add = subRand.nextFloat() * 0.3F;
            float r = Math.min(1.0F, dark + add);
            float g = Math.min(1.0F, 0.6F * dark + add);
            float b = add;

            float scale = (subRand.nextFloat() * 0.5F + 0.1F + ageRatio * 2.0F) * this.quadSize * virtual.screenScale();
            float px = relX + (float) ((subRand.nextGaussian() - 1.0D) * 0.2F * spread);
            float py = relY + (float) ((subRand.nextGaussian() - 1.0D) * 0.5F * spread);
            float pz = relZ + (float) ((subRand.nextGaussian() - 1.0D) * 0.2F * spread);

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
            int light = LightTexture.FULL_BRIGHT;
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
            return new MissileContrailParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }
}
