package com.hbm_m.particle.nt;

import com.hbm_m.client.ClientRenderHandler.CustomRenderTypes;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * Порт {@link com.hbm_m.particle.custom.MissileVaporContrailParticle} на NT-движок:
 * серый конденсационный след за ракетой, живёт намного дольше горячего выхлопа,
 * медленно расплывается и гаснет (1.7.10 {@code ParticleContrail}).
 */
public class MissileVaporContrailNT extends MissileTrailNT {

    /** SpriteSet прокидывается из Provider при регистрации (ClientParticleHandler). */
    public static volatile SpriteSet sprites;

    public static void setSprites(SpriteSet set) { sprites = set; }

    private static final int SUB_QUADS = 6;
    private static final ResourceLocation PARTICLE_ATLAS = TextureAtlas.LOCATION_PARTICLES;

    private final float[] layerR = new float[SUB_QUADS];
    private final float[] layerG = new float[SUB_QUADS];
    private final float[] layerB = new float[SUB_QUADS];
    /** Пре-масштабированный гаусс: {@code nextGaussian() * 0.5F}. */
    private final float[] layerGaussX = new float[SUB_QUADS];
    private final float[] layerGaussY = new float[SUB_QUADS];
    private final float[] layerGaussZ = new float[SUB_QUADS];

    private float baseGray;
    private float cachedSpreadBase;
    private float lightBrightness = 1.0F;
    /** Спавновый масштаб — от него считается рост квада с возрастом. */
    private final float spawnScale;

    public MissileVaporContrailNT(ClientLevel level, double x, double y, double z, float scale) {
        super(level, x, y, z);
        this.spawnScale = scale;
        this.lifetime = 280 + this.random.nextInt(120);
        this.quadSize = scale;
        this.baseGray = 0.42F + this.random.nextFloat() * 0.12F;
        this.alpha = 0.7F;
        cacheSpriteUv(sprites.get(0, 1));
        precomputeLayers(new Random(this.random.nextLong()));
        updateRenderCaches();
    }

    private void precomputeLayers(Random subRand) {
        for (int layer = 0; layer < SUB_QUADS; layer++) {
            float mod = subRand.nextFloat() * 0.2F + 0.15F;
            // Слои ярче базового серого — как в старой версии (brighten-модификатор).
            this.layerR[layer] = Math.min(1.0F, this.baseGray + mod);
            this.layerG[layer] = Math.min(1.0F, this.baseGray + mod);
            this.layerB[layer] = Math.min(1.0F, this.baseGray + 0.04F + mod);
            this.layerGaussX[layer] = (float) (subRand.nextGaussian() * 0.5D);
            this.layerGaussY[layer] = (float) (subRand.nextGaussian() * 0.5D);
            this.layerGaussZ[layer] = (float) (subRand.nextGaussian() * 0.5D);
        }
    }

    private void updateRenderCaches() {
        float ageRatio = Math.min((float) this.age / (float) this.lifetime, 1.0F);
        this.alpha = (float) (Math.pow(1.0F - ageRatio, 0.35D) * 0.72F);
        this.cachedSpreadBase = ((float) Math.pow(ageRatio * 3.5F, 1.4D) + 0.6F) * this.quadSize;
        BlockPos pos = BlockPos.containing(this.x, this.y, this.z);
        if (this.level.hasChunkAt(pos)) {
            int light = LevelRenderer.getLightColor(this.level, pos);
            // Рендертайп без lightmap — яркость запекаем в цвет вершины.
            int sky = LightTexture.sky(light);
            int block = LightTexture.block(light);
            this.lightBrightness = Math.max(0.25F,
                    Math.max(sky, block) / 15.0F);
        } else {
            this.lightBrightness = 1.0F;
        }
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
        this.quadSize = this.spawnScale * (0.85F + ageRatio * 2.2F);

        // Лёгкий дрейф — конденсат расползается в стороны, как у авиационного следа.
        this.xd += (this.random.nextFloat() - 0.5F) * 0.004F;
        this.yd += (this.random.nextFloat() - 0.5F) * 0.002F;
        this.zd += (this.random.nextFloat() - 0.5F) * 0.004F;
        this.xd *= 0.96D;
        this.yd *= 0.96D;
        this.zd *= 0.96D;
        this.move(this.xd, this.yd, this.zd);
        updateRenderCaches();
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
        Vec3 rel = virtualize(wx, wy, wz, camera);
        Vec3 camPos = camera.getPosition();
        double rx = wx - camPos.x;
        double ry = wy - camPos.y;
        double rz = wz - camPos.z;
        float shrink = (float) (rel.length() / Math.max(1.0E-4D, Math.sqrt(rx * rx + ry * ry + rz * rz)));

        float spreadMul = this.cachedSpreadBase * shrink;
        float scale = (this.alpha + 0.5F) * this.quadSize * shrink;
        int iAlpha = (int) (this.alpha * 255.0F);
        float bright = this.lightBrightness;

        for (int layer = 0; layer < SUB_QUADS; layer++) {
            float px = (float) rel.x + this.layerGaussX[layer] * spreadMul;
            float py = (float) rel.y + this.layerGaussY[layer] * spreadMul;
            float pz = (float) rel.z + this.layerGaussZ[layer] * spreadMul;

            emitBillboard(consumer, camera, px, py, pz, scale,
                    this.layerR[layer] * bright,
                    this.layerG[layer] * bright,
                    this.layerB[layer] * bright,
                    iAlpha / 255.0F);
        }
    }
}
