package com.hbm_m.particle.explosions;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.Mth;

/**
 * ✅ ЖЕЛТЫЙ, ДОЛГО ОСТАЕТСЯ ГОРЯЧИМ
 */
public class LargeDarkSmoke extends AbstractExplosionParticle {

    // 1. ИСТИННО ЖЕЛТЫЙ
    private final float startR = 1.0F;
    private final float startG = 0.95F;
    private final float startB = 0.0F;
    private final float startAlpha = 1.0F;

    // 2. ОРАНЖЕВЫЙ (К концу горячей фазы)
    private final float midR = 1.0F;
    private final float midG = 0.5F;
    private final float midB = 0.0F;
    private final float midAlpha = 0.98F;

    // 3. ЧЕРНЫЙ
    private final float endR = 0.1F;
    private final float endG = 0.1F;
    private final float endB = 0.1F;
    private final float endAlpha = 0.95F;

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

        this.rCol = startR;
        this.gCol = startG;
        this.bCol = startB;
        this.alpha = startAlpha;
    }

    @Override
    public void tick() {
        super.tick();

        float ageProgress = (float) this.age / (float) this.lifetime;

        // 🔥 ДОЛГАЯ ГОРЯЧАЯ ФАЗА (0% -> 30%)
        // Теперь дым остается светящимся треть своей жизни
        if (ageProgress < 0.3F) {
            float t = ageProgress / 0.3F;
            this.rCol = Mth.lerp(t, startR, midR);
            this.gCol = Mth.lerp(t, startG, midG);
            this.bCol = Mth.lerp(t, startB, midB);
            this.alpha = Mth.lerp(t, startAlpha, midAlpha);
        } else {
            // 🌑 ОСТЫВАНИЕ (30% -> 100%)
            // Медленно превращается в черный дым
            float t = (ageProgress - 0.3F) / 0.7F;
            if (t > 1.0F) t = 1.0F;
            this.rCol = Mth.lerp(t, midR, endR);
            this.gCol = Mth.lerp(t, midG, endG);
            this.bCol = Mth.lerp(t, midB, endB);
            this.alpha = Mth.lerp(t, midAlpha, endAlpha);
        }

        if (ageProgress > 0.9F) {
            float fadeOut = (ageProgress - 0.9F) / 0.1F;
            this.alpha = endAlpha * (1.0F - fadeOut);
        }
    }

    public static class Provider extends AbstractExplosionParticle.Provider {
        public Provider(SpriteSet sprites) {
            super(sprites, LargeDarkSmoke::new);
        }
    }
}
