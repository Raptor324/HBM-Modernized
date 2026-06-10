package com.hbm_m.particle.explosions;

import com.hbm_m.client.render.ImmediateVertexWriter;
import com.hbm_m.particle.FullBrightParticleRenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 *  ИСПРАВЛЕННЫЙ БАЗОВЫЙ КЛАСС
 *
 * КЛЮЧЕВЫЕ ИСПРАВЛЕНИЯ:
 * 1. shouldCull() теперь ПРАВИЛЬНО контролирует видимость (нет инверсии логики)
 * 2. Максимальное расстояние видимости: 512+ блоков (255² = 65025 в квадрате = 2*255²)
 * 3. {@link FullBrightParticleRenderType} — без lightmap (sky flash не искажает цвет)
 * 4. Проверка null для Camera
 */
public abstract class AbstractExplosionParticle extends TextureSheetParticle {

    //  МАКСИМАЛЬНАЯ ДИСТАНЦИЯ РЕНДЕРА (512 блоков)
    // 512² = 262144 в квадрате
    private static final double MAX_RENDER_DISTANCE_SQ = 1024.0 * 1024.0;

    public AbstractExplosionParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.pickSprite(sprites);

        //  БАЗОВЫЕ НАСТРОЙКИ
        this.hasPhysics = false;
        this.friction = 0.98F;
    }

    /**
     *  КРИТИЧЕСКОЕ ПЕРЕОПРЕДЕЛЕНИЕ!
     *
     * ИСПРАВЛЕНИЕ: Логика теперь ПРАВИЛЬНАЯ
     * - Возвращаем FALSE если частица ВИДНА (в пределах расстояния)
     * - Возвращаем TRUE если частица НЕ видна (слишком далеко)
     *
     * Это полностью обходит стандартное ограничение в 32 блока
     */
    
    public boolean shouldCull() {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        if (camera == null) {
            return false; // Если камера не инициализирована, не отсекаем
        }

        //  Вычисляем расстояние до частицы от камеры
        double dx = this.x - camera.getPosition().x;
        double dy = this.y - camera.getPosition().y;
        double dz = this.z - camera.getPosition().z;

        //  Квадрат расстояния (без sqrt для производительности)
        double distanceSq = dx * dx + dy * dy + dz * dz;

        //  ИСПРАВЛЕННАЯ ЛОГИКА:
        // TRUE = отсечь (слишком далеко)
        // FALSE = не отсекать (видна)
        return distanceSq > MAX_RENDER_DISTANCE_SQ;
    }

    /**
     *  КРИТИЧЕСКОЕ ПЕРЕОПРЕДЕЛЕНИЕ!
     * Без этого будет использоваться ванильный рендер
     */
    @Override
    public ParticleRenderType getRenderType() {
        return FullBrightParticleRenderType.INSTANCE;
    }

    /** Самосветящиеся частицы — не зависят от освещения кратера (0 light → чёрные квадраты). */
    @Override
    public int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    /**
     * Явный billboard + {@link LightTexture#FULL_BRIGHT} — как у {@link com.hbm_m.particle.custom.MissileContrailParticle}.
     * Ванильный {@code TextureSheetParticle.render} с LongRangeParticleRenderType даёт зелёный/фиолетовый tint через lightmap.
     */
    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        Vec3 cam = camera.getPosition();
        float relX = (float) (Mth.lerp(partialTick, this.xo, this.x) - cam.x());
        float relY = (float) (Mth.lerp(partialTick, this.yo, this.y) - cam.y());
        float relZ = (float) (Mth.lerp(partialTick, this.zo, this.z) - cam.z());
        float scale = this.quadSize;
        Quaternionf rotation = camera.rotation();

        Vector3f[] corners = new Vector3f[]{
                new Vector3f(-scale, -scale, 0.0F),
                new Vector3f(-scale, scale, 0.0F),
                new Vector3f(scale, scale, 0.0F),
                new Vector3f(scale, -scale, 0.0F)
        };
        for (Vector3f corner : corners) {
            corner.rotate(rotation);
            corner.add(relX, relY, relZ);
        }

        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        ImmediateVertexWriter.worldQuad(buffer,
                corners[0].x(), corners[0].y(), corners[0].z(),
                corners[1].x(), corners[1].y(), corners[1].z(),
                corners[2].x(), corners[2].y(), corners[2].z(),
                corners[3].x(), corners[3].y(), corners[3].z(),
                this.rCol, this.gCol, this.bCol, this.alpha,
                u0, v0, u1, v1);
    }

    /**
     *  ВНУТРЕННИЙ КЛАСС Provider
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
     *  Функциональный интерфейс для создания частиц
     */
    @FunctionalInterface
    public interface ParticleFactory<T> {
        T create(ClientLevel level, double x, double y, double z, SpriteSet sprites,
                 double xSpeed, double ySpeed, double zSpeed);
    }
}
