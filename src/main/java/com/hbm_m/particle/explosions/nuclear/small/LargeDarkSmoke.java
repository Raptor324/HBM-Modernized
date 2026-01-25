package com.hbm_m.particle.explosions.nuclear.small;

import com.hbm_m.particle.explosions.AbstractExplosionParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.Mth;

public class LargeDarkSmoke extends AbstractExplosionParticle {

    // БАЗОВЫЕ ЦВЕТА (СРЕДНИЕ ЗНАЧЕНИЯ)

    // Старт: ярко-жёлтый
    private static final float BASE_START_R = 1.0F;
    private static final float BASE_START_G = 0.9F;
    private static final float BASE_START_B = 0.0F;

    // Mid1: светлый оранжевый
    private static final float BASE_MID1_R = 0.95F;
    private static final float BASE_MID1_G = 0.55F;
    private static final float BASE_MID1_B = 0.08F;

    // Mid2: тёмный оранжевый (между mid1 и чёрным)
    private static final float BASE_MID2_R = 0.55F;
    private static final float BASE_MID2_G = 0.25F;
    private static final float BASE_MID2_B = 0.05F;

    // End: чёрный
    private static final float BASE_END_R = 0.1F;
    private static final float BASE_END_G = 0.1F;
    private static final float BASE_END_B = 0.1F;

    // Персональные цвета частицы (с рандомом)
    private final float startR, startG, startB;
    private final float mid1R, mid1G, mid1B;
    private final float mid2R, mid2G, mid2B;
    private final float endR, endG, endB;

    public LargeDarkSmoke(ClientLevel level, double x, double y, double z,
                          SpriteSet sprites, double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, sprites);

        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;

        this.lifetime = 140 + this.random.nextInt(70);
        this.gravity = 0.0F;
        this.hasPhysics = false;

        this.quadSize = 1.2F + this.random.nextFloat() * 1.0F;

        // Усиленный разброс цвета
        float startJitter = (this.random.nextFloat() - 0.5F) * 0.18F; // ~±0.09
        float mid1Jitter  = (this.random.nextFloat() - 0.5F) * 0.20F; // ~±0.10
        float mid2Jitter  = (this.random.nextFloat() - 0.5F) * 0.20F;
        float endJitter   = (this.random.nextFloat() - 0.5F) * 0.08F; // ~±0.04

        this.startR = clamp01(BASE_START_R + startJitter);
        this.startG = clamp01(BASE_START_G + startJitter);
        this.startB = clamp01(BASE_START_B);

        this.mid1R = clamp01(BASE_MID1_R + mid1Jitter);
        this.mid1G = clamp01(BASE_MID1_G + mid1Jitter * 0.8F);
        this.mid1B = clamp01(BASE_MID1_B + mid1Jitter * 0.5F);

        this.mid2R = clamp01(BASE_MID2_R + mid2Jitter);
        this.mid2G = clamp01(BASE_MID2_G + mid2Jitter * 0.8F);
        this.mid2B = clamp01(BASE_MID2_B + mid2Jitter * 0.6F);

        this.endR = clamp01(BASE_END_R + endJitter);
        this.endG = clamp01(BASE_END_G + endJitter);
        this.endB = clamp01(BASE_END_B + endJitter);

        this.rCol = startR;
        this.gCol = startG;
        this.bCol = startB;
        this.alpha = 1.0F;
    }

    private static float clamp01(float v) {
        return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
    }

    @Override
    public void tick() {
        super.tick();

        float ageProgress = (float) this.age / (float) this.lifetime;

        // ФАЗА 1: Жёлтый -> светлый оранжевый (0%–40%)
        if (ageProgress < 0.40F) {
            float t = ageProgress / 0.40F;
            this.rCol = Mth.lerp(t, startR, mid1R);
            this.gCol = Mth.lerp(t, startG, mid1G);
            this.bCol = Mth.lerp(t, startB, mid1B);
        }
        // ФАЗА 2: светлый оранжевый -> тёмный оранжевый (40%–55%)
        else if (ageProgress < 0.55F) {
            float t = (ageProgress - 0.40F) / 0.15F;
            this.rCol = Mth.lerp(t, mid1R, mid2R);
            this.gCol = Mth.lerp(t, mid1G, mid2G);
            this.bCol = Mth.lerp(t, mid1B, mid2B);
        }
        // ФАЗА 3: тёмный оранжевый -> чёрный (55%–60%)
        else if (ageProgress < 0.60F) {
            float t = (ageProgress - 0.55F) / 0.05F;
            this.rCol = Mth.lerp(t, mid2R, endR);
            this.gCol = Mth.lerp(t, mid2G, endG);
            this.bCol = Mth.lerp(t, mid2B, endB);
        }
        // ФАЗА 4: чёрный дым (60%–100%)
        else {
            this.rCol = endR;
            this.gCol = endG;
            this.bCol = endB;
        }

        if (ageProgress > 0.98F) {
            this.removed = true;
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public static class Provider extends AbstractExplosionParticle.Provider {
        public Provider(SpriteSet sprites) {
            super(sprites, LargeDarkSmoke::new);
        }
    }
}
