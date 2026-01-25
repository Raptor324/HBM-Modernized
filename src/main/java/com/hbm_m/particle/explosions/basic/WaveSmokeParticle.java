package com.hbm_m.particle.explosions.basic;

import com.hbm_m.particle.explosions.AbstractExplosionParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * ✅ ВОЛНОВОЙ ДЫМ - НЕ ПРОХОДИТ СКВОЗЬ БЛОКИ
 *
 * Особенности:
 * - Проверяет столкновения с блоками
 * - "Облетает" препятствия сверху
 * - Создаёт тряску камеры при касании игрока
 * - Сила тряски зависит от расстояния частицы от эпицентра
 */
public class WaveSmokeParticle extends AbstractExplosionParticle {

    private static final double LIFT_FORCE = 0.15;
    private static final double MAX_LIFT_HEIGHT = 5.0;

    // ┌─────────────────────────────────────────────────────────────┐
    // │ НАСТРОЙКИ ЭФФЕКТА ТРЯСКИ                                    │
    // └─────────────────────────────────────────────────────────────┘

    // Радиус воздействия на игрока (в блоках)
    private static final double PLAYER_EFFECT_RADIUS = 2.0;

    // Максимальная интенсивность тряски (в эпицентре)
    private static final float MAX_SHAKE_INTENSITY = 1.0F;

    // Расстояние, после которого эффект становится минимальным (в блоках)
    private static final double MAX_DISTANCE_FOR_EFFECT = 15.0;

    // Минимальная интенсивность (для далёких частиц)
    private static final float MIN_SHAKE_INTENSITY = 0.1F;

    // Кулдаун между применениями эффекта (в тиках)
    private int effectCooldown = 0;

    // ┌─────────────────────────────────────────────────────────────┐
    // │ СОХРАНЕНИЕ ЭПИЦЕНТРА ВЗРЫВА                                 │
    // └─────────────────────────────────────────────────────────────┘

    // Начальная позиция взрыва (эпицентр)
    private final double explosionCenterX;
    private final double explosionCenterZ;

    private final double originY;

    public WaveSmokeParticle(ClientLevel level, double x, double y, double z,
                             SpriteSet sprites, double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, sprites);

        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;

        this.originY = y;

        // ✅ СОХРАНЯЕМ ЭПИЦЕНТР ВЗРЫВА
        // Предполагаем, что частица спавнится на начальном радиусе от центра
        // Вычисляем центр взрыва на основе начальной позиции и скорости
        // Центр = текущая позиция - (направление движения * начальный радиус)
        double dirLength = Math.sqrt(xSpeed * xSpeed + zSpeed * zSpeed);
        if (dirLength > 0.001) {
            // Нормализуем направление и умножаем на начальный радиус
            // Начальный радиус примерно 5-7 блоков (из ExplosionParticleUtils)
            double avgStartRadius = 6.0;
            this.explosionCenterX = x - (xSpeed / dirLength) * avgStartRadius;
            this.explosionCenterZ = z - (zSpeed / dirLength) * avgStartRadius;
        } else {
            // Если скорость ~0, считаем что частица в центре
            this.explosionCenterX = x;
            this.explosionCenterZ = z;
        }

        this.lifetime = 60 + this.random.nextInt(40);
        this.gravity = 0.08F;
        this.hasPhysics = false;

        this.quadSize = 0.4F + this.random.nextFloat() * 0.6F;

        float grayValue = 0.5F + this.random.nextFloat() * 0.3F;
        this.rCol = grayValue;
        this.gCol = grayValue;
        this.bCol = grayValue;

        this.alpha = 0.7F;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        // ┌─────────────────────────────────────────────────────────────┐
        // │ ПРОВЕРКА КАСАНИЯ ИГРОКА                                     │
        // └─────────────────────────────────────────────────────────────┘

        if (effectCooldown > 0) {
            effectCooldown--;
        } else {
            checkPlayerCollision();
        }

        // ┌─────────────────────────────────────────────────────────────┐
        // │ ПРОВЕРКА СТОЛКНОВЕНИЙ С БЛОКАМИ                             │
        // └─────────────────────────────────────────────────────────────┘

        Vec3 nextPos = new Vec3(this.x + this.xd, this.y + this.yd, this.z + this.zd);
        BlockPos blockPos = new BlockPos((int) Math.floor(nextPos.x),
                (int) Math.floor(nextPos.y),
                (int) Math.floor(nextPos.z));

        BlockState blockState = this.level.getBlockState(blockPos);
        boolean hasCollision = !blockState.isAir() && blockState.isSolidRender(this.level, blockPos);

        if (hasCollision) {
            double currentHeight = this.y - this.originY;

            if (currentHeight < MAX_LIFT_HEIGHT) {
                this.yd += LIFT_FORCE;
                this.xd *= 0.95;
                this.zd *= 0.95;
            } else {
                this.yd = Math.max(this.yd, 0);
                this.xd *= 1.05;
                this.zd *= 1.05;
            }
        } else {
            this.yd -= this.gravity;

            BlockPos belowPos = new BlockPos((int) Math.floor(this.x),
                    (int) Math.floor(this.y - 0.5),
                    (int) Math.floor(this.z));
            BlockState belowState = this.level.getBlockState(belowPos);
            boolean hasFloor = !belowState.isAir() && belowState.isSolidRender(this.level, belowPos);

            if (hasFloor && this.yd < 0) {
                this.yd = 0.02;
            }
        }

        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;

        this.xd *= 0.98;
        this.zd *= 0.98;
        this.yd *= 0.95;

        float fadeProgress = (float) this.age / (float) this.lifetime;
        this.alpha = 0.7F * (1.0F - fadeProgress);
        this.quadSize *= 1.005F;
    }

    /**
     * ✅ ПРОВЕРКА СТОЛКНОВЕНИЯ С ИГРОКОМ
     *
     * Если игрок находится в радиусе PLAYER_EFFECT_RADIUS от частицы,
     * применяется эффект тряски камеры с учётом расстояния от эпицентра
     */
    private void checkPlayerCollision() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        // Вычисляем расстояние до игрока
        double dx = this.x - player.getX();
        double dy = this.y - player.getY();
        double dz = this.z - player.getZ();
        double distanceSq = dx * dx + dy * dy + dz * dz;

        // Если игрок в радиусе воздействия
        if (distanceSq < PLAYER_EFFECT_RADIUS * PLAYER_EFFECT_RADIUS) {
            // ✅ Применяем эффект тряски с учётом расстояния от эпицентра
            applyShockwaveEffect(player, distanceSq);

            // Устанавливаем кулдаун (10 тиков = 0.5 секунды)
            effectCooldown = 10;
        }
    }

    /**
     * ✅ ВЫЧИСЛЕНИЕ РАССТОЯНИЯ ОТ ЭПИЦЕНТРА ВЗРЫВА
     *
     * @return Горизонтальное расстояние от центра взрыва (в блоках)
     */
    private double getDistanceFromExplosionCenter() {
        // Вычисляем только горизонтальное расстояние (X и Z)
        // Игнорируем Y, так как волна распространяется по земле
        double dx = this.x - this.explosionCenterX;
        double dz = this.z - this.explosionCenterZ;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * ✅ ПРИМЕНЕНИЕ ЭФФЕКТА УДАРНОЙ ВОЛНЫ
     *
     * Сила эффекта зависит от:
     * 1. Расстояния до игрока (чем ближе - тем сильнее)
     * 2. Расстояния частицы от эпицентра (чем дальше - тем слабее)
     *
     * @param player Игрок, на которого применяется эффект
     * @param distanceSq Квадрат расстояния до игрока
     */
    private void applyShockwaveEffect(LocalPlayer player, double distanceSq) {
        // ┌─────────────────────────────────────────────────────────────┐
        // │ 1. КОЭФФИЦИЕНТ ОТ РАССТОЯНИЯ ДО ИГРОКА                     │
        // └─────────────────────────────────────────────────────────────┘
        // Чем ближе игрок к частице - тем сильнее эффект
        double distance = Math.sqrt(distanceSq);
        float playerProximity = (float) (1.0 - distance / PLAYER_EFFECT_RADIUS);

        // ┌─────────────────────────────────────────────────────────────┐
        // │ 2. КОЭФФИЦИЕНТ ОТ РАССТОЯНИЯ ОТ ЭПИЦЕНТРА                  │
        // └─────────────────────────────────────────────────────────────┘
        // Чем дальше частица от эпицентра - тем слабее её "сила"
        double distanceFromCenter = getDistanceFromExplosionCenter();

        // Вычисляем коэффициент ослабления (0.0 = очень далеко, 1.0 = в эпицентре)
        float distanceFalloff;
        if (distanceFromCenter < MAX_DISTANCE_FOR_EFFECT) {
            // Линейное ослабление от центра
            distanceFalloff = 1.0F - (float) (distanceFromCenter / MAX_DISTANCE_FOR_EFFECT);
        } else {
            // Частица слишком далеко - минимальный эффект
            distanceFalloff = 0.0F;
        }

        // ┌─────────────────────────────────────────────────────────────┐
        // │ 3. ИТОГОВАЯ ИНТЕНСИВНОСТЬ                                   │
        // └─────────────────────────────────────────────────────────────┘

        // Базовая интенсивность = MAX_SHAKE_INTENSITY * близость игрока * удалённость от центра
        float baseIntensity = MAX_SHAKE_INTENSITY * playerProximity * distanceFalloff;

        // Применяем минимальную интенсивность
        float finalIntensity = Math.max(baseIntensity, MIN_SHAKE_INTENSITY * distanceFalloff);

        // ✅ Применяем тряску камеры через CameraShakeHandler
        CameraShakeHandler.addShake(finalIntensity, 10); // 10 тиков тряски

        // ✅ Опционально: звуковой эффект с громкостью зависящей от силы
        // player.playSound(SoundEvents.GENERIC_EXPLODE, finalIntensity * 0.5F, 1.0F + distanceFalloff * 0.5F);
    }

    public static class Provider extends AbstractExplosionParticle.Provider<WaveSmokeParticle> {
        public Provider(SpriteSet sprites) {
            super(sprites, WaveSmokeParticle::new);
        }
    }
}
