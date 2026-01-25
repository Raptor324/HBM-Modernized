package com.hbm_m.particle.explosions.nuclear.small;

import com.hbm_m.particle.explosions.AbstractExplosionParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;

/**
 * ✅ УВЕЛИЧЕННЫЕ ИСКРЫ ДЛЯ ЯДЕРНОГО ВЗРЫВА
 * 
 * Параметры:
 * - Размер: 0.6-1.2 (в 2x раза больше обычных)
 * - Время жизни: 30-60 тиков (дольше видны)
 * - Цвет: ярко-оранжевый с красными оттенками
 * - Физика: падение с гравитацией 0.2
 * - Эффект: более впечатляющий взрыв
 */
public class LargeExplosionSpark extends AbstractExplosionParticle {

    public LargeExplosionSpark(ClientLevel level, double x, double y, double z,
            SpriteSet sprites, double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, sprites);
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;

        // ✅ ВРЕМЯ ЖИЗНИ: 30-60 тиков (дольше обычных)
        this.lifetime = 30 + this.random.nextInt(30);

        // ✅ ФИЗИКА: меньше гравитация (медленнее падают)
        this.gravity = 0.2F;
        this.hasPhysics = false;

        // ✅ ВНЕШНИЙ ВИД: размер 0.6-1.2 (ДВЕ СКОРОСТИ)
        this.quadSize = 0.6F + this.random.nextFloat() * 0.6F;

        // ✅ ЦВЕТ: ярко-оранжево-красный
        this.rCol = 1.0F; // Red: максимум
        this.gCol = 0.5F + this.random.nextFloat() * 0.3F; // Green: 0.5-0.8
        this.bCol = 0.0F; // Blue: практически нет (яркий оранжевый-красный)

        // ✅ ПРОЗРАЧНОСТЬ
        this.alpha = 1.0F;
    }

    @Override
    public void tick() {
        super.tick();

        // ✅ Плавное исчезновение (fade out)
        float fadeProgress = (float) this.age / (float) this.lifetime;
        this.alpha = Math.max(0.7F, 1.0F - fadeProgress);

        // ✅ Медленное сжатие (эффект сгорания)
        this.quadSize *= 0.97F;

        // ✅ Легкое вращение/колебание размера для динамики
        if (this.age % 2 == 0) {
            this.quadSize *= 1.01F;
        }
    }

    public static class Provider extends AbstractExplosionParticle.Provider {
        public Provider(SpriteSet sprites) {
            super(sprites, LargeExplosionSpark::new);
        }
    }
}
