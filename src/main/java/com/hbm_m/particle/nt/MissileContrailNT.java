package com.hbm_m.particle.nt;

import com.hbm_m.client.ClientRenderHandler.CustomRenderTypes;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * Порт {@link com.hbm_m.particle.custom.MissileContrailParticle} на NT-движок:
 * оранжевый выхлоп баллистической ракеты, гашение скорости, многослойный
 * билборд, атлас частиц + альфа-блендинг (FX layer 1 из 1.7.10).
 *
 * Отличие от ванильного предшественника: рендер идёт через ParticleEngineNT
 * (после weather), при активном DH дальние сегменты трейла не клипаются
 * far plane'ом.
 */
public class MissileContrailNT extends MissileTrailNT {

    /** SpriteSet прокидывается из Provider при регистрации (ClientParticleHandler). */
    public static volatile SpriteSet sprites;

    public static void setSprites(SpriteSet set) { sprites = set; }

    private static final int SUB_QUADS = 10;
    private static final ResourceLocation PARTICLE_ATLAS = TextureAtlas.LOCATION_PARTICLES;

    private final float[] layerAdd = new float[SUB_QUADS];
    private final float[] layerScaleRand = new float[SUB_QUADS];
    /** Пре-масштабированный гаусс: {@code (nextGaussian() - 1.0) * axisSpread}. */
    private final float[] layerGaussX = new float[SUB_QUADS];
    private final float[] layerGaussY = new float[SUB_QUADS];
    private final float[] layerGaussZ = new float[SUB_QUADS];

    private float cachedDark = 1.0F;
    private float cachedAgeRatio;
    /** {@code (pow(ageRatio * 4, 1.5) + 1) * quadSize} — разлёт слоя. */
    private float cachedSpreadBase;

    public MissileContrailNT(ClientLevel level, double x, double y, double z,
                             double dx, double dy, double dz, float scale) {
        super(level, x, y, z);
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        this.lifetime = 60 + this.random.nextInt(20);
        this.quadSize = scale;
        cacheSpriteUv(randomSprite(sprites, this.random));
        precomputeLayers(new Random(this.random.nextLong()));
        updateColorFromAge();
    }

    private void precomputeLayers(Random subRand) {
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
     * Серый конденсат появляется только после затухания горячего выхлопа —
     * продлевает след во времени, а не у самого сопла.
     */
    private void spawnVaporContinuation() {
        if (this.age <= 0) {
            return;
        }
        ParticleEngineNT.INSTANCE.add(new MissileVaporContrailNT(
                this.level, this.x, this.y, this.z, this.quadSize));
    }

    /** 1.7.10 {@code ParticleRocketFlame}: оранжевое пламя, темнеет с возрастом. */
    private void updateColorFromAge() {
        this.cachedDark = 1.0F - Math.min((float) this.age / (this.lifetime * 0.25F), 1.0F);
        this.cachedAgeRatio = Math.min((float) this.age / (float) this.lifetime, 1.0F);
        this.alpha = (float) (Math.pow(1.0F - this.cachedAgeRatio, 0.5) * 0.75F);
        this.cachedSpreadBase = (float) (Math.pow(this.cachedAgeRatio * 4.0F, 1.5D) + 1.0F) * this.quadSize;
    }

    @Override
    public RenderType getRenderType() {
        return CustomRenderTypes.nukeClouds(PARTICLE_ATLAS);
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTicks,
                       com.mojang.blaze3d.vertex.PoseStack levelPoseStack) {
        double wx = Mth.lerp(partialTicks, this.xo, this.x);
        double wy = Mth.lerp(partialTicks, this.yo, this.y);
        double wz = Mth.lerp(partialTicks, this.zo, this.z);
        Vec3 camPos = camera.getPosition();
        double rx = wx - camPos.x;
        double ry = wy - camPos.y;
        double rz = wz - camPos.z;
        // Виртуализация: при DH — истинное смещение (shrink=1), без DH —
        // приближение дальних сегментов к границе прорисовки.
        Vec3 rel = virtualize(wx, wy, wz, camera);
        float shrink = (float) (rel.length() / Math.max(1.0E-4D, Math.sqrt(rx * rx + ry * ry + rz * rz)));
        float spread = this.cachedSpreadBase * shrink;
        float dark = this.cachedDark;
        float ageRatio = this.cachedAgeRatio;
        int iAlpha = (int) (this.alpha * 255.0F);

        for (int layer = 0; layer < SUB_QUADS; layer++) {
            float add = this.layerAdd[layer];
            int ir = (int) (Math.min(1.0F, dark + add) * 255.0F);
            int ig = (int) (Math.min(1.0F, 0.6F * dark + add) * 255.0F);
            int ib = (int) (add * 255.0F);

            float scale = (this.layerScaleRand[layer] * 0.5F + 0.1F + ageRatio * 2.0F) * this.quadSize * shrink;
            float px = (float) rel.x + this.layerGaussX[layer] * spread;
            float py = (float) rel.y + this.layerGaussY[layer] * spread;
            float pz = (float) rel.z + this.layerGaussZ[layer] * spread;

            emitBillboard(consumer, camera, px, py, pz, scale, ir / 255.0F, ig / 255.0F, ib / 255.0F, iAlpha / 255.0F);
        }
    }
}