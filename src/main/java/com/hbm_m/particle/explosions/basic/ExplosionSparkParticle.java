package com.hbm_m.particle.explosions.basic;

import com.hbm_m.particle.explosions.AbstractExplosionParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;

/**
 * ✅ Оранжевые искры с физикой падения
 */
public class ExplosionSparkParticle extends AbstractExplosionParticle {

    public ExplosionSparkParticle(ClientLevel level, double x, double y, double z,
                                  SpriteSet sprites, double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, sprites);

        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;

        // ✅ ВРЕМЯ ЖИЗНИ: 20-40 тиков
        this.lifetime = 20 + this.random.nextInt(15);

        // ✅ ФИЗИКА
        this.gravity = 0.3F;
        this.hasPhysics = false;

        // ✅ ВНЕШНИЙ ВИД: размер 0.3-0.6
        this.quadSize = 0.3F + this.random.nextFloat() * 0.3F;

        // ✅ ЦВЕТ: оранжево-желтый
        this.rCol = 1.0F;          // Red: максимум
        this.gCol = 0.6F + this.random.nextFloat() * 0.3F;  // Green: 0.6-0.9
        this.bCol = 0.1F;          // Blue: минимум (оранжевый оттенок)

        // ✅ ПРОЗРАЧНОСТЬ
        this.alpha = 1.0F;
    }

    @Override
    public void tick() {
        super.tick();

        // ✅ Плавное исчезновение (fade out)
        float fadeProgress = (float) this.age / (float) this.lifetime;
        this.alpha = Math.max(0.6F, 1.0F - fadeProgress);

        // ✅ Сжатие (эффект сгорания)
        this.quadSize *= 0.98F;
    }

    public static class Provider extends AbstractExplosionParticle.Provider<ExplosionSparkParticle> {
        public Provider(SpriteSet sprites) {
            super(sprites, ExplosionSparkParticle::new);
        }
    }
}
