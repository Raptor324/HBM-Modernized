package com.hbm_m.particle.explosions.basic;

import com.hbm_m.particle.explosions.AbstractExplosionParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;

/**
 * ✅ Кольца ударной волны (шокволна)
 * Расширяющиеся волны давления от взрыва
 */
public class ShockwaveRingParticle extends AbstractExplosionParticle {

    public ShockwaveRingParticle(ClientLevel level, double x, double y, double z,
                                 SpriteSet sprites, double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, sprites);

        // ✅ ВРЕМЯ ЖИЗНИ: 15-25 тиков
        this.lifetime = 15 + this.random.nextInt(10);

        // ✅ ФИЗИКА: нет гравитации
        this.gravity = 0.0F;
        this.hasPhysics = false;

        // ✅ РАЗМЕР: средний-крупный (0.8-1.2)
        this.quadSize = 0.8F + this.random.nextFloat() * 0.4F;

        // ✅ ЦВЕТ: серо-белый (выглядит как воздушная волна)
        this.rCol = 0.8F + this.random.nextFloat() * 0.2F;
        this.gCol = 0.8F + this.random.nextFloat() * 0.2F;
        this.bCol = 0.9F + this.random.nextFloat() * 0.1F;

        // ✅ ПРОЗРАЧНОСТЬ: начальная
        this.alpha = 0.8F;
    }

    @Override
    public void tick() {
        super.tick();

        // ✅ Плавное исчезновение
        float fadeProgress = (float) this.age / (float) this.lifetime;
        this.alpha = 0.8F * (1.0F - fadeProgress);

        // ✅ Медленное увеличение размера (волна расширяется)
        this.quadSize *= 1.01F;
    }

    public static class Provider extends AbstractExplosionParticle.Provider<ShockwaveRingParticle> {
        public Provider(SpriteSet sprites) {
            super(sprites, ShockwaveRingParticle::new);
        }
    }
}
