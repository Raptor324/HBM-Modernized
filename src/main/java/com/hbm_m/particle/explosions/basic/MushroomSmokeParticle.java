package com.hbm_m.particle.explosions.basic;

import com.hbm_m.particle.explosions.AbstractExplosionParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;

/**
 * ✅ Дым грибовидного облака (серый дым)
 * Основной элемент визуала взрыва
 */
public class MushroomSmokeParticle extends AbstractExplosionParticle {

    public MushroomSmokeParticle(ClientLevel level, double x, double y, double z,
                                 SpriteSet sprites, double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, sprites);

        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;

        // ✅ ВРЕМЯ ЖИЗНИ: 40-80 тиков (долгоживущий дым)
        this.lifetime = 40 + this.random.nextInt(40);

        // ✅ ФИЗИКА: небольшая гравитация (дым медленно опускается)
        this.gravity = 0.05F;
        this.hasPhysics = false;

        // ✅ РАЗМЕР: средний (0.5-1.5)
        this.quadSize = 0.5F + this.random.nextFloat() * 1.0F;

        // ✅ ЦВЕТ: серый (дым)
        float grayValue = 0.5F + this.random.nextFloat() * 0.3F;
        this.rCol = grayValue;
        this.gCol = grayValue;
        this.bCol = grayValue;

        // ✅ ПРОЗРАЧНОСТЬ: средняя
        this.alpha = 0.7F;
    }

    @Override
    public void tick() {
        super.tick();

        // ✅ Плавное исчезновение (медленнее чем другие эффекты)
        float fadeProgress = (float) this.age / (float) this.lifetime;
        this.alpha = 0.7F * (1.0F - fadeProgress);

        // ✅ Медленное увеличение размера (дым рассеивается)
        this.quadSize *= 1.005F;
    }

    public static class Provider extends AbstractExplosionParticle.Provider<MushroomSmokeParticle> {
        public Provider(SpriteSet sprites) {
            super(sprites, MushroomSmokeParticle::new);
        }
    }
}
