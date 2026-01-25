package com.hbm_m.particle.explosions.basic;

import com.hbm_m.particle.explosions.AbstractExplosionParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;

/**
 * ✅ Яркая белая вспышка взрыва
 * Основное освещение в центре взрыва
 */
public class ExplosionFlashParticle extends AbstractExplosionParticle {

    public ExplosionFlashParticle(ClientLevel level, double x, double y, double z,
                                  SpriteSet sprites, double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, sprites);

        // ✅ ВРЕМЯ ЖИЗНИ: 10-20 тиков (быстрая вспышка)
        this.lifetime = 10 + this.random.nextInt(10);

        // ✅ ФИЗИКА: нет гравитации для вспышки
        this.gravity = 0.0F;
        this.hasPhysics = false;

        // ✅ РАЗМЕР: крупный (1.0-1.5)
        this.quadSize = 1.0F + this.random.nextFloat() * 0.5F;

        // ✅ ЦВЕТ: белый (максимум всех каналов)
        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;

        // ✅ ПРОЗРАЧНОСТЬ: высокая в начале
        this.alpha = 1.0F;
    }

    @Override
    public void tick() {
        super.tick();

        // ✅ Быстрое исчезновение (вспышка)
        float fadeProgress = (float) this.age / (float) this.lifetime;
        this.alpha = 1.0F - fadeProgress;

        // ✅ Медленное увеличение размера
        this.quadSize *= 1.02F;
    }

    public static class Provider extends AbstractExplosionParticle.Provider<ExplosionFlashParticle> {
        public Provider(SpriteSet sprites) {
            super(sprites, ExplosionFlashParticle::new);
        }
    }
}
