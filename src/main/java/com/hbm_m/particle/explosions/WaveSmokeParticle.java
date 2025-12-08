package com.hbm_m.particle.explosions;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * ✅ ВОЛНОВОЙ ДЫМ - НЕ ПРОХОДИТ СКВОЗЬ БЛОКИ
 *
 * Цвет: Светло-серый (как обычный дым взрыва)
 * Использование: Кольцо расширяющейся волны
 */
public class WaveSmokeParticle extends AbstractExplosionParticle {

    // ┌─────────────────────────────────────────────────────────────┐
    // │ НАСТРОЙКИ ФИЗИКИ ВОЛНЫ                                      │
    // └─────────────────────────────────────────────────────────────┘

    private static final double LIFT_FORCE = 0.15;
    private static final double MAX_LIFT_HEIGHT = 5.0;

    private final double originY;

    public WaveSmokeParticle(ClientLevel level, double x, double y, double z,
                             SpriteSet sprites, double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, sprites);

        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;

        this.originY = y;

        // ✅ ВРЕМЯ ЖИЗНИ: 60-100 тиков (долгоживущая волна)
        this.lifetime = 40 + this.random.nextInt(40);

        // ✅ ФИЗИКА
        this.gravity = 0.08F;
        this.hasPhysics = false;

        // ✅ РАЗМЕР: УМЕНЬШЕН - маленький-средний (0.4-1.0) (было 1.0-2.5)
        this.quadSize = 0.4F + this.random.nextFloat() * 0.6F;

        // ✅ ЦВЕТ: СВЕТЛО-СЕРЫЙ (как обычный дым взрыва) 🌫️
        float grayValue = 0.5F + this.random.nextFloat() * 0.3F; // 0.5-0.8
        this.rCol = grayValue;
        this.gCol = grayValue;
        this.bCol = grayValue;

        // ✅ ПРОЗРАЧНОСТЬ
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
        // │ ПРОВЕРКА СТОЛКНОВЕНИЙ                                       │
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

        // ┌─────────────────────────────────────────────────────────────┐
        // │ ВИЗУАЛЬНЫЕ ЭФФЕКТЫ                                          │
        // └─────────────────────────────────────────────────────────────┘

        float fadeProgress = (float) this.age / (float) this.lifetime;
        this.alpha = 0.7F * (1.0F - fadeProgress);

        // Медленное увеличение размера
        this.quadSize *= 1.005F;
    }

    public static class Provider extends AbstractExplosionParticle.Provider<WaveSmokeParticle> {
        public Provider(SpriteSet sprites) {
            super(sprites, WaveSmokeParticle::new);
        }
    }
}
