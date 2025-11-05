package com.hbm_m.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ExplosiveChargeBlock extends Block implements IDetonatable {

    private static final float EXPLOSION_POWER = 15.0F;

    public ExplosiveChargeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onDetonate(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            // Удаляем блок
            level.removeBlock(pos, false);

            // Запускаем последовательность эффектов взрыва
            createExplosionEffects((ServerLevel) level, pos);

            // Создаем мощный взрыв
            level.explode(
                    null,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    EXPLOSION_POWER,
                    Level.ExplosionInteraction.BLOCK
            );

            return true;
        }
        return false;
    }

    /**
     * Создает последовательность визуальных эффектов взрыва
     */
    private void createExplosionEffects(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        // 1. ЯРКАЯ ВСПЫШКА (мгновенно)
        createFlashEffect(level, x, y, z);

        // 2. ВЗРЫВНАЯ ВОЛНА (начинается сразу после вспышки)
        scheduleTask(level, () -> createShockwave(level, x, y, z), 2); // 0.1 секунды задержка

        // 3. ГРИБ ИЗ ДЫМА (формируется постепенно)
        scheduleTask(level, () -> startMushroomCloud(level, x, y, z), 5); // 0.25 секунды задержка
    }

    /**
     * ЭТАП 1: Яркая вспышка при детонации
     */
    private void createFlashEffect(ServerLevel level, double x, double y, double z) {
        // Большая яркая вспышка в центре[7][20]
        level.sendParticles(
                ParticleTypes.FLASH,
                x, y, z,
                1,        // количество
                0, 0, 0,  // разброс
                0         // скорость
        );

        // Дополнительные взрывные частицы для усиления эффекта[20]
        level.sendParticles(
                ParticleTypes.EXPLOSION_EMITTER,
                x, y, z,
                3,
                2.0, 2.0, 2.0,
                0
        );

        // Звук взрыва
        level.playSound(
                null,
                BlockPos.containing(x, y, z),
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.BLOCKS,
                4.0F,
                (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F
        );
    }

    /**
     * ЭТАП 2: Взрывная волна расширяется от центра
     */
    private void createShockwave(ServerLevel level, double x, double y, double z) {
        // Создаем кольцевую взрывную волну с частицами[7][10]
        for (int ring = 1; ring <= 5; ring++) {
            final int currentRing = ring;
            scheduleTask(level, () -> {
                double radius = currentRing * 3.0;
                int particleCount = currentRing * 40; // Больше частиц на внешних кольцах

                for (int i = 0; i < particleCount; i++) {
                    double angle = (2 * Math.PI * i) / particleCount;
                    double offsetX = Math.cos(angle) * radius;
                    double offsetZ = Math.sin(angle) * radius;

                    // Взрывные частицы для волны
                    level.sendParticles(
                            ParticleTypes.EXPLOSION,
                            x + offsetX, y, z + offsetZ,
                            1,
                            0.1, 0.1, 0.1,
                            0.05
                    );

                    // Дымовые частицы после волны[7]
                    level.sendParticles(
                            ParticleTypes.LARGE_SMOKE,
                            x + offsetX, y, z + offsetZ,
                            2,
                            0.2, 0.5, 0.2,
                            0.01
                    );
                }
            }, currentRing * 2); // Каждое кольцо с задержкой
        }
    }

    /**
     * ЭТАП 3: Формирование грибовидного облака
     */
    private void startMushroomCloud(ServerLevel level, double x, double y, double z) {
        // Создаем гриб поэтапно: ножка, затем шляпка[7][10][13]

        // Ножка гриба (столб дыма вверх)
        for (int height = 0; height < 20; height++) {
            final int currentHeight = height;
            scheduleTask(level, () -> {
                createMushroomStem(level, x, y + currentHeight, z, currentHeight);
            }, height);
        }

        // Шляпка гриба (начинает формироваться после ножки)
        scheduleTask(level, () -> {
            for (int i = 0; i < 20; i++) {
                final int iteration = i;
                scheduleTask(level, () -> {
                    createMushroomCap(level, x, y + 20, z);
                }, iteration * 2);
            }
        }, 30);
    }

    /**
     * Создает ножку грибовидного облака[7]
     */
    private void createMushroomStem(ServerLevel level, double x, double y, double z, int height) {
        double radius = 2.0 + (height * 0.05); // Ножка слегка расширяется
        int particleCount = 20;

        for (int i = 0; i < particleCount; i++) {
            double angle = (2 * Math.PI * i) / particleCount;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;

            // Темный дым для ножки[7][20]
            level.sendParticles(
                    ParticleTypes.LARGE_SMOKE,
                    x + offsetX, y, z + offsetZ,
                    2,
                    0.3, 0.3, 0.3,
                    0.02
            );

            // Добавляем огненные частицы внизу ножки
            if (height < 10) {
                level.sendParticles(
                        ParticleTypes.FLAME,
                        x + offsetX, y, z + offsetZ,
                        1,
                        0.1, 0.1, 0.1,
                        0.01
                );
            }
        }
    }

    /**
     * Создает шляпку грибовидного облака[7][10]
     */
    private void createMushroomCap(ServerLevel level, double x, double y, double z) {
        // Создаем округлую шляпку
        for (int layer = 0; layer < 8; layer++) {
            double layerY = y + layer;
            double layerRadius = 8.0 - (layer * 0.5); // Шляпка сужается кверху
            int particleCount = (int)(layerRadius * 10);

            for (int i = 0; i < particleCount; i++) {
                double angle = (2 * Math.PI * i) / particleCount;
                double offsetX = Math.cos(angle) * layerRadius;
                double offsetZ = Math.sin(angle) * layerRadius;

                // Густой дым для шляпки[7][20]
                level.sendParticles(
                        ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        x + offsetX, layerY, z + offsetZ,
                        3,
                        0.5, 0.2, 0.5,
                        0.01
                );

                // Добавляем пепел для реалистичности
                level.sendParticles(
                        ParticleTypes.ASH,
                        x + offsetX, layerY, z + offsetZ,
                        1,
                        0.3, 0.3, 0.3,
                        0.005
                );
            }
        }
    }

    /**
     * Планирует выполнение задачи с задержкой (в тиках)[23][50]
     */
    private void scheduleTask(ServerLevel level, Runnable task, int delayTicks) {
        level.getServer().tell(new net.minecraft.server.TickTask(
                level.getServer().getTickCount() + delayTicks,
                task
        ));
    }
}