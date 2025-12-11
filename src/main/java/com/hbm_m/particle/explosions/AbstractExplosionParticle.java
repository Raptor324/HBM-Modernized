package com.hbm_m.particle.explosions;

import com.hbm_m.particle.LongRangeParticleRenderType;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * ✅ ИСПРАВЛЕННЫЙ БАЗОВЫЙ КЛАСС
 *
 * КЛЮЧЕВЫЕ ИСПРАВЛЕНИЯ:
 * 1. shouldCull() теперь ПРАВИЛЬНО контролирует видимость (нет инверсии логики)
 * 2. Максимальное расстояние видимости: 512+ блоков (255² = 65025 в квадрате = 2*255²)
 * 3. Правильное взаимодействие с LongRangeParticleRenderType
 * 4. Проверка null для Camera
 */
public abstract class AbstractExplosionParticle extends TextureSheetParticle {

    // ✅ МАКСИМАЛЬНАЯ ДИСТАНЦИЯ РЕНДЕРА (512 блоков)
    // 512² = 262144 в квадрате
    private static final double MAX_RENDER_DISTANCE_SQ = 1024.0 * 1024.0;

    public AbstractExplosionParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.pickSprite(sprites);

        // ✅ БАЗОВЫЕ НАСТРОЙКИ
        this.hasPhysics = false;
        this.friction = 0.98F;
    }

    /**
     * ✅ КРИТИЧЕСКОЕ ПЕРЕОПРЕДЕЛЕНИЕ!
     *
     * ИСПРАВЛЕНИЕ: Логика теперь ПРАВИЛЬНАЯ
     * - Возвращаем FALSE если частица ВИДНА (в пределах расстояния)
     * - Возвращаем TRUE если частица НЕ видна (слишком далеко)
     *
     * Это полностью обходит стандартное ограничение в 32 блока
     */
    @Override
    public boolean shouldCull() {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        if (camera == null) {
            return false; // Если камера не инициализирована, не отсекаем
        }

        // ✅ Вычисляем расстояние до частицы от камеры
        double dx = this.x - camera.getPosition().x;
        double dy = this.y - camera.getPosition().y;
        double dz = this.z - camera.getPosition().z;

        // ✅ Квадрат расстояния (без sqrt для производительности)
        double distanceSq = dx * dx + dy * dy + dz * dz;

        // ✅ ИСПРАВЛЕННАЯ ЛОГИКА:
        // TRUE = отсечь (слишком далеко)
        // FALSE = не отсекать (видна)
        return distanceSq > MAX_RENDER_DISTANCE_SQ;
    }

    /**
     * ✅ КРИТИЧЕСКОЕ ПЕРЕОПРЕДЕЛЕНИЕ!
     * Без этого будет использоваться ванильный рендер
     */
    @Override
    public ParticleRenderType getRenderType() {
        return LongRangeParticleRenderType.INSTANCE;
    }

    /**
     * ✅ ВНУТРЕННИЙ КЛАСС Provider
     * Позволяет удобно создавать частицы через простой интерфейс
     */
    public static abstract class Provider<T extends AbstractExplosionParticle> implements ParticleProvider<SimpleParticleType> {
        protected final SpriteSet sprites;
        protected final ParticleFactory<T> factory;

        public Provider(SpriteSet sprites, ParticleFactory<T> factory) {
            this.sprites = sprites;
            this.factory = factory;
        }

        @Override
        public T createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                double xSpeed, double ySpeed, double zSpeed) {
            return this.factory.create(level, x, y, z, sprites, xSpeed, ySpeed, zSpeed);
        }
    }

    /**
     * ✅ Функциональный интерфейс для создания частиц
     */
    @FunctionalInterface
    public interface ParticleFactory<T> {
        T create(ClientLevel level, double x, double y, double z, SpriteSet sprites,
                 double xSpeed, double ySpeed, double zSpeed);
    }
}
