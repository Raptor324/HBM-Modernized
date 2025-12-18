package com.hbm_m.particle.explosions;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.Mth;

public class LargeDarkSmoke extends AbstractExplosionParticle {

    private final float startR = 1.0F;
    private final float startG = 0.9F;
    private final float startB = 0.0F;

    private final float midR = 0.8F;
    private final float midG = 0.05F;
    private final float midB = 0.0F;

    private final float endR = 0.1F;
    private final float endG = 0.1F;
    private final float endB = 0.1F;

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
        this.alpha = 1.0F;
    }

    @Override
    public void tick() {
        super.tick();

        float ageProgress = (float) this.age / (float) this.lifetime;

        // ФАЗА 1: ЖЕЛТЫЙ -> КРАСНЫЙ (47.5%)
        if (ageProgress < 0.475F) {
            float t = ageProgress / 0.475F;
            this.rCol = Mth.lerp(t, startR, midR);
            this.gCol = Mth.lerp(t, startG, midG);
            this.bCol = Mth.lerp(t, startB, midB);
        }
        // ФАЗА 2: КРАСНЫЙ -> ЧЕРНЫЙ (ОЧЕНЬ БЫСТРО, 5%)
        // Почти мгновенно остывает
        else if (ageProgress < 0.525F) {
            float t = (ageProgress - 0.475F) / 0.05F;
            this.rCol = Mth.lerp(t, midR, endR);
            this.gCol = Mth.lerp(t, midG, endG);
            this.bCol = Mth.lerp(t, midB, endB);
        }
        // ФАЗА 3: ЧЕРНЫЙ (47.5%)
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
