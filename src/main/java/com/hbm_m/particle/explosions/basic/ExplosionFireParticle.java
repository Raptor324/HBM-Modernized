package com.hbm_m.particle.explosions.basic;

import com.hbm_m.particle.explosions.AbstractExplosionParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;

/**
 * ✅ Огонь взрыва (красно-оранжевый пламя)
 * Основание взрыва
 */
public class ExplosionFireParticle extends AbstractExplosionParticle {

    public ExplosionFireParticle(ClientLevel level, double x, double y, double z,
                                 SpriteSet sprites, double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, sprites);

        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;

        // ✅ ВРЕМЯ ЖИЗНИ: 30-50 тиков
        this.lifetime = 30 + this.random.nextInt(20);

        // ✅ ФИЗИКА: средняя гравитация (пламя поднимается медленнее чем искры)
        this.gravity = 0.1F;
        this.hasPhysics = false;

        // ✅ РАЗМЕР: крупный (0.6-1.0)
        this.quadSize = 0.6F + this.random.nextFloat() * 0.4F;

        // ✅ ЦВЕТ: красно-оранжевый огонь
        this.rCol = 1.0F;              // Red: максимум
        this.gCol = 0.4F + this.random.nextFloat() * 0.3F;  // Green: 0.4-0.7
        this.bCol = 0.0F;              // Blue: минимум (красный огонь)

        // ✅ ПРОЗРАЧНОСТЬ
        this.alpha = 0.9F;
    }

    @Override
    public void tick() {
        super.tick();

        // ✅ Плавное исчезновение
        float fadeProgress = (float) this.age / (float) this.lifetime;
        this.alpha = 0.9F * (1.0F - fadeProgress);

        // ✅ Медленное сжатие
        this.quadSize *= 0.97F;
    }

    public static class Provider extends AbstractExplosionParticle.Provider<ExplosionFireParticle> {
        public Provider(SpriteSet sprites) {
            super(sprites, ExplosionFireParticle::new);
        }
    }
}
