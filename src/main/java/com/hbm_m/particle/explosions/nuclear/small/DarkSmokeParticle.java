package com.hbm_m.particle.explosions.nuclear.small;

import com.hbm_m.particle.explosions.AbstractExplosionParticle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;

/**
 *  ТЁМНЫЙ ДЫМ (средне-серый)
 * Для реалистичных взрывов и пожаров
 */
public class DarkSmokeParticle extends AbstractExplosionParticle {

    public DarkSmokeParticle(ClientLevel level, double x, double y, double z,
                             SpriteSet sprites, double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, sprites);

        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;

        //  ВРЕМЯ ЖИЗНИ: 50-90 тиков (долгий дым)
        this.lifetime = 50 + this.random.nextInt(40);

        //  ФИЗИКА: небольшая гравитация
        this.gravity = 0.05F;
        this.hasPhysics = false;

        //  РАЗМЕР: средний-крупный (0.6-1.8)
        this.quadSize = 0.5F + this.random.nextFloat() * 0.9F;

        //  ЦВЕТ: СРЕДНЕ-СЕРЫЙ (светлее чем раньше)
        float grayValue = 0.35F + this.random.nextFloat() * 0.2F; // 0.35-0.55 (было 0.2-0.4)
        this.rCol = grayValue;
        this.gCol = grayValue;
        this.bCol = grayValue;

        //  ПРОЗРАЧНОСТЬ: средняя
        this.alpha = 0.8F;
    }

    @Override
    public void tick() {
        super.tick();

        //  Плавное исчезновение
        float fadeProgress = (float) this.age / (float) this.lifetime;
        this.alpha = 0.8F * (1.0F - fadeProgress);

        //  Медленное увеличение размера (дым рассеивается)
        this.quadSize *= 1.005F;
    }

    public static class Provider extends AbstractExplosionParticle.Provider<DarkSmokeParticle> {
        public Provider(SpriteSet sprites) {
            super(sprites, DarkSmokeParticle::new);
        }
    }
}
