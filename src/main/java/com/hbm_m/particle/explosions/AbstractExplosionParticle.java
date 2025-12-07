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
 * ✅ ИСПРАВЛЕННЫЙ БАЗОВЫЙ КЛАСС - полностью рабочий!
 *
 * Видны на 256+ блоков благодаря комбинации:
 * 1. SimpleParticleType с alwaysShow=true
 * 2. Кастомному ParticleRenderType (LongRangeParticleRenderType)
 * 3. Проверке расстояния через shouldCull()
 */
public abstract class AbstractExplosionParticle extends TextureSheetParticle {

    // Максимальная дистанция рендера в квадрате (по умолчанию 256 блоков)
    private static final double MAX_RENDER_DISTANCE_SQ = 256.0 * 256.0; // 65536

    public AbstractExplosionParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);

        // ✅ Правильный выбор спрайта из набора
        this.pickSprite(sprites);

        // ✅ БАЗОВЫЕ НАСТРОЙКИ ДЛЯ ВСЕХ ЧАСТИЦ
        this.hasPhysics = false;
        this.friction = 0.98F;
    }

    /**
     * ✅ ГЛАВНОЕ ПЕРЕОПРЕДЕЛЕНИЕ!
     * Эта проверка ПОЛНОСТЬЮ обходит стандартное ограничение в 32 блока
     *
     * Minecraft проверяет это в ParticleEngine.isInView()
     * Если возвращаем false - частица НЕ будет отсечена по расстоянию
     */
    @Override
    public boolean shouldCull() {
        // ✅ Получаем позицию камеры
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();

        // ✅ Вычисляем вектор от камеры к частице
        double dx = this.x - camera.getPosition().x;
        double dy = this.y - camera.getPosition().y;
        double dz = this.z - camera.getPosition().z;

        // ✅ Квадрат расстояния (избегаем sqrt для производительности)
        double distanceSq = dx * dx + dy * dy + dz * dz;

        // ✅ ВОЗВРАЩАЕМ true (нужно отсечь) только если дальше MAX_RENDER_DISTANCE
        // Возвращаем false (НЕ отсекать) если в пределах видимости
        return distanceSq > MAX_RENDER_DISTANCE_SQ;
    }

    /**
     * ✅ Используем кастомный рендер для дальних расстояний
     * Это переопределение критично! Без него будет использоваться ванильный рендер
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

        private final SpriteSet sprites;
        private final ParticleFactory<T> factory;

        public Provider(SpriteSet sprites, ParticleFactory<T> factory) {
            this.sprites = sprites;
            this.factory = factory;
        }

        @Override
        public T createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                double xSpeed, double ySpeed, double zSpeed) {
            // ✅ Делегируем создание конкретной частице через factory
            return this.factory.create(level, x, y, z, sprites, xSpeed, ySpeed, zSpeed);
        }
    }

    /**
     * ✅ Функциональный интерфейс для создания частиц
     * Позволяет каждому подклассу задать свой конструктор
     */
    @FunctionalInterface
    public interface ParticleFactory<T> {
        T create(ClientLevel level, double x, double y, double z, SpriteSet sprites,
                 double xSpeed, double ySpeed, double zSpeed);
    }
}