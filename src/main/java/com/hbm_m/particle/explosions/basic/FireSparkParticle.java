package com.hbm_m.particle.explosions.basic;

import java.util.List;

import com.hbm_m.particle.explosions.AbstractExplosionParticle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * ОГНЕННАЯ ИСКРА
 *
 * Оранжевые искры с физикой падения, которые:
 * - Поджигают мобов на 10 секунд при контакте
 * - Поджигают горючие блоки (ставят огонь с нужной стороны)
 */
public class FireSparkParticle extends AbstractExplosionParticle {

    private static final double MOB_IGNITE_RADIUS = 1.5; // Радиус поджога мобов
    private int igniteCheckCooldown = 0; // Кулдаун между проверками

    public FireSparkParticle(ClientLevel level, double x, double y, double z,
                             SpriteSet sprites, double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, sprites);

        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;

        //  ВРЕМЯ ЖИЗНИ: 20-35 тиков
        this.lifetime = 20 + this.random.nextInt(15);

        //  ФИЗИКА
        this.gravity = 0.3F;
        this.hasPhysics = false;

        //  ВНЕШНИЙ ВИД: размер 0.3-0.6
        this.quadSize = 0.3F + this.random.nextFloat() * 0.3F;

        //  ЦВЕТ: оранжево-желтый (горячий!)
        this.rCol = 1.0F;          // Red: максимум
        this.gCol = 0.6F + this.random.nextFloat() * 0.3F;  // Green: 0.6-0.9
        this.bCol = 0.1F;          // Blue: минимум (оранжевый оттенок)

        //  ПРОЗРАЧНОСТЬ
        this.alpha = 1.0F;
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

        // ПОДЖОГ МОБОВ И БЛОКОВ
        if (igniteCheckCooldown > 0) {
            igniteCheckCooldown--;
        } else {
            igniteMobs();
            igniteCheckCooldown = 3; // Проверяем каждые 3 тика
        }


        //  ФИЗИКА
        this.yd -= this.gravity;

        // Сохраняем старую позицию для определения направления
        double oldX = this.x;
        double oldY = this.y;
        double oldZ = this.z;

        // Применяем движение
        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;

        this.xd *= 0.98F;
        this.yd *= 0.98F;
        this.zd *= 0.98F;

        //  ПРОВЕРКА СТОЛКНОВЕНИЯ С БЛОКАМИ (для поджога)
        checkBlockCollision(oldX, oldY, oldZ);

        //  Плавное исчезновение
        float fadeProgress = (float) this.age / (float) this.lifetime;
        this.alpha = Math.max(0.6F, 1.0F - fadeProgress);

        //  Сжатие (эффект сгорания)
        this.quadSize *= 0.98F;
    }


//   ПОДЖОГ МОБОВ В РАДИУСЕ

    private void igniteMobs() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getSingleplayerServer() == null) return;

        var server = mc.getSingleplayerServer();
        var serverLevel = server.getLevel(this.level.dimension());
        if (serverLevel == null) return;

        //  Получаем мобов из СЕРВЕРНОГО уровня
        List<LivingEntity> nearbyMobs = serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                new net.minecraft.world.phys.AABB(
                        this.x - MOB_IGNITE_RADIUS, this.y - MOB_IGNITE_RADIUS, this.z - MOB_IGNITE_RADIUS,
                        this.x + MOB_IGNITE_RADIUS, this.y + MOB_IGNITE_RADIUS, this.z + MOB_IGNITE_RADIUS
                )
        );

        if (!nearbyMobs.isEmpty()) {
            server.execute(() -> {
                for (LivingEntity living : nearbyMobs) {
                    //  ПОДЖОГ НА 10 СЕКУНД (200 тиков)
                    com.hbm_m.platform.PlatformHooks.setSecondsOnFire(living, 10);
                    System.out.println("[FireSpark] 🔥 Поджог: " + living.getName().getString());
                }
            });
        }
    }

    /**
     *  ПРОВЕРКА СТОЛКНОВЕНИЯ С БЛОКАМИ (для поджога)
     *
     * Определяет сторону столкновения и ставит огонь
     */
    private void checkBlockCollision(double oldX, double oldY, double oldZ) {
        BlockPos currentPos = BlockPos.containing(this.x, this.y, this.z);
        BlockState blockState = this.level.getBlockState(currentPos);

        //  Если попали в твёрдый блок
        if (!blockState.isAir() && blockState.isSolidRender(this.level, currentPos)) {
            // Определяем сторону столкновения
            Direction hitSide = determineHitSide(oldX, oldY, oldZ, currentPos);

            //  Пытаемся поджечь с этой стороны
            if (hitSide != null) {
                igniteBlockSide(currentPos, hitSide);
            }

            // Исчезаем после столкновения
            this.remove();
        }
    }

    /**
     * ОПРЕДЕЛИТЬ СТОРОНУ СТОЛКНОВЕНИЯ
     *
     * @param oldX, oldY, oldZ - старая позиция частицы
     * @param blockPos - позиция блока
     * @return Сторона, с которой прилетела частица
     */
    private Direction determineHitSide(double oldX, double oldY, double oldZ, BlockPos blockPos) {
        // Вычисляем дельты (откуда прилетели)
        double dx = oldX - (blockPos.getX() + 0.5);
        double dy = oldY - (blockPos.getY() + 0.5);
        double dz = oldZ - (blockPos.getZ() + 0.5);

        // Находим максимальную дельту по абсолютному значению
        double absDx = Math.abs(dx);
        double absDy = Math.abs(dy);
        double absDz = Math.abs(dz);

        // Возвращаем сторону с максимальным отклонением
        if (absDx > absDy && absDx > absDz) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        } else if (absDy > absDx && absDy > absDz) {
            return dy > 0 ? Direction.UP : Direction.DOWN;
        } else {
            return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }

    /**
     *  ПОДЖЕЧЬ БЛОК С ОПРЕДЕЛЁННОЙ СТОРОНЫ
     *
     * @param blockPos Позиция блока
     * @param side Сторона, с которой прилетела искра
     */
    private void igniteBlockSide(BlockPos blockPos, Direction side) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getSingleplayerServer() == null) return;

        var server = mc.getSingleplayerServer();
        var serverLevel = server.getLevel(this.level.dimension());
        if (serverLevel == null) return;

        //  Позиция, где нужно поставить огонь (рядом с блоком)
        BlockPos firePos = blockPos.relative(side);

        //  Проверяем, можно ли поставить огонь
        if (serverLevel.isEmptyBlock(firePos) || serverLevel.getBlockState(firePos).canBeReplaced()) {
            //  Проверяем, что под огнём есть блок (если ставим сверху)
            if (side == Direction.UP) {
                BlockState belowState = serverLevel.getBlockState(blockPos);
                if (!belowState.isAir() && belowState.isSolidRender(serverLevel, blockPos)) {
                    server.execute(() -> {
                        serverLevel.setBlock(firePos, Blocks.FIRE.defaultBlockState(), 3);
                        System.out.println("[FireSpark]  Огонь поставлен: " + firePos + " (сверху)");
                    });
                }
            } else {
                // Для других сторон просто ставим огонь
                server.execute(() -> {
                    serverLevel.setBlock(firePos, Blocks.FIRE.defaultBlockState(), 3);
                    System.out.println("[FireSpark]  Огонь поставлен: " + firePos + " (с " + side + ")");
                });
            }
        }
    }

    public static class Provider extends AbstractExplosionParticle.Provider<FireSparkParticle> {
        public Provider(SpriteSet sprites) {
            super(sprites, FireSparkParticle::new);
        }
    }
}
