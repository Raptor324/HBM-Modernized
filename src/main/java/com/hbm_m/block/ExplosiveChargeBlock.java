package com.hbm_m.block;

import com.hbm_m.particle.ModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ExplosiveChargeBlock extends Block implements IDetonatable {

    // Более сильный взрыв для горного дела
    private static final float EXPLOSION_POWER = 15.0F;

    public ExplosiveChargeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onDetonate(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            // Удаляем блок
            level.removeBlock(pos, false);

            // Создаем мощный взрыв
            level.explode(
                    null,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    EXPLOSION_POWER,
                    Level.ExplosionInteraction.BLOCK
            );

            // Запускаем эффект дыма
            spawnSmokeColumn((ServerLevel) level, pos);

            return true;
        }
        return false;
    }

    /**
     * Создает столб дыма на 10 секунд
     */
    private void spawnSmokeColumn(ServerLevel level, BlockPos explosionPos) {
        // Создаем задачу, которая будет спавнить дым в течение 10 секунд
        int duration = 200; // 10 секунд (20 тиков = 1 секунда)

        // Запускаем повторяющуюся задачу
        scheduleSmoke(level, explosionPos, duration, 0);
    }

    /**
     * Рекурсивный метод для создания дыма
     */
    private void scheduleSmoke(ServerLevel level, BlockPos pos, int remainingTicks, int currentTick) {
        if (remainingTicks <= 0) {
            return;
        }

        // Спавним частицы каждые 2 тика
        if (currentTick % 2 == 0) {
            spawnSmokeParticles(level, pos);
        }

        // Планируем следующий тик
        level.getServer().tell(new net.minecraft.server.TickTask(
                level.getServer().getTickCount() + 1,
                () -> scheduleSmoke(level, pos, remainingTicks - 1, currentTick + 1)
        ));
    }

    /**
     * Спавнит пучок частиц дыма
     */
    private void spawnSmokeParticles(ServerLevel level, BlockPos pos) {
        double centerX = pos.getX() + 0.5;
        double centerY = pos.getY() + 0.5;
        double centerZ = pos.getZ() + 0.5;

        // Спавним 5-10 частиц за раз
        int particleCount = 5 + level.random.nextInt(6);

        for (int i = 0; i < particleCount; i++) {
            // Случайное смещение от центра взрыва
            double offsetX = (level.random.nextDouble() - 0.5) * 2.0;
            double offsetZ = (level.random.nextDouble() - 0.5) * 2.0;
            double offsetY = level.random.nextDouble() * 0.5;

            // Скорость частиц - в основном вверх
            double velocityX = (level.random.nextDouble() - 0.5) * 0.1;
            double velocityY = 0.1 + level.random.nextDouble() * 0.1;
            double velocityZ = (level.random.nextDouble() - 0.5) * 0.1;

            level.sendParticles(
                    ModParticleTypes.SMOKE_COLUMN.get(),
                    centerX + offsetX,
                    centerY + offsetY,
                    centerZ + offsetZ,
                    1, // количество
                    velocityX,
                    velocityY,
                    velocityZ,
                    0.0 // скорость
            );
        }
    }

}