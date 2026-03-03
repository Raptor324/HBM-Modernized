package com.hbm_m.block.nature;

import com.hbm_m.particle.explosions.basic.ExplosionParticleUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * ☠️ ТОКСИЧНЫЙ ГЕЙЗЕР
 *
 * Периодически выбрасывает Agent Orange вертикально вверх
 * Частицы поднимаются на 10 блоков, затем разлетаются в стороны
 */
public class GeysirBlock extends Block {

    //  Свойство: активен ли гейзер в данный момент
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    // ⏱️ НАСТРОЙКИ ПЕРИОДА
    private static final int ERUPTION_INTERVAL = 400;     // 20 секунд (400 тиков)
    private static final int ERUPTION_DURATION = 60;      // 3 секунды активности (60 тиков)
    private static final int ERUPTION_COOLDOWN = 20;      // 1 секунда задержки

    // 🌊 НАСТРОЙКИ СТРУИ
    private static final int SPRAY_HEIGHT = 40;           // ← БЫЛО 10, СТАЛО 40!
    private static final int PARTICLES_PER_TICK = 3;     // ← Увеличил для густоты
    private static final double HORIZONTAL_SPREAD = 15.0; // ← Больше разброс (было 7.0)


    public GeysirBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    // ════════════════════════════════════════════════════════════════
    // ⏱️ ПОСТОЯННЫЙ ТИК (для периодических извержений)
    // ════════════════════════════════════════════════════════════════

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            //  Начинаем тиковать блок
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean isActive = state.getValue(ACTIVE);
        long gameTime = level.getGameTime();

        // ════════════════════════════════════════════════════════════════
        // 🌊 ЛОГИКА ИЗВЕРЖЕНИЯ
        // ════════════════════════════════════════════════════════════════

        // Вычисляем фазу цикла
        long cycleTime = gameTime % (ERUPTION_INTERVAL + ERUPTION_DURATION + ERUPTION_COOLDOWN);

        if (cycleTime >= ERUPTION_INTERVAL && cycleTime < ERUPTION_INTERVAL + ERUPTION_DURATION) {
            //  ФАЗА ИЗВЕРЖЕНИЯ (30 сек прошло, извергается 3 секунды)
            if (!isActive) {
                // Активируем гейзер
                level.setBlock(pos, state.setValue(ACTIVE, true), 3);
                // 🔊 Звук начала извержения
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS,
                        1.5F, 0.5F);
            }

            // ☠️ СПАВНИМ ЧАСТИЦЫ Agent Orange
            spawnAgentOrangePlume(level, pos, random);

        } else if (isActive) {
            // ❌ ФАЗА ПОКОЯ - деактивируем гейзер
            level.setBlock(pos, state.setValue(ACTIVE, false), 3);
        }

        // ⏱️ Планируем следующий тик
        level.scheduleTick(pos, this, 1);
    }

    /**
     * ☠️ СПАВН СТРУИ AGENT ORANGE
     * <p>
     * Выбрасывает частицы вертикально вверх с разбросом
     *  Использует ExplosionParticleUtils.spawnAgentOrange()
     * <p>
     * СТРУКТУРА СТРУИ:
     * - 0-15 блоков: Узкая вертикальная струя
     * - 15-30 блоков: Постепенный разброс в стороны
     * - 30-40 блоков: Максимальный разброс + падение
     */
    private void spawnAgentOrangePlume(ServerLevel level, BlockPos pos, RandomSource random) {
        double centerX = pos.getX() + 0.5;
        double centerY = pos.getY() + 1.0;
        double centerZ = pos.getZ() + 0.5;

        // ════════════════════════════════════════════════════════════════
        // 🌊 НИЖНЯЯ ЧАСТЬ: Узкая мощная струя (0-15 блоков)
        // ════════════════════════════════════════════════════════════════
        for (int height = 0; height < 15; height += 2) { // ← Каждые 2 блока (оптимизация)
            double y = centerY + height;
            int particleCount = 1;
            double verticalSpeed = 0.25 + height * 0.015; // Увеличивается с высотой

            ExplosionParticleUtils.spawnAgentOrangeGeyser(
                    level,
                    centerX,
                    y,
                    centerZ,
                    particleCount,
                    verticalSpeed,
                    0.5 // Минимальный разброс
            );
        }

        // ════════════════════════════════════════════════════════════════
        // 💨 СРЕДНЯЯ ЧАСТЬ: Постепенный разброс (15-30 блоков)
        // ════════════════════════════════════════════════════════════════
        for (int height = 15; height < 30; height += 2) {
            double y = centerY + height;
            int particleCount = 2;
            double progress = (height - 15) / 15.0; // 0.0 - 1.0
            double horizontalSpread = 1.0 + progress * 8.0; // От 1 до 9 блоков
            double verticalSpeed = 0.2 - progress * 0.1; // Замедляется

            ExplosionParticleUtils.spawnAgentOrangeGeyser(
                    level,
                    centerX,
                    y,
                    centerZ,
                    particleCount,
                    verticalSpeed,
                    horizontalSpread
            );
        }

        // ════════════════════════════════════════════════════════════════
        // ☁️ ВЕРХНЯЯ ЧАСТЬ: Максимальный разброс (30-40 блоков)
        // ════════════════════════════════════════════════════════════════
        for (int height = 30; height < SPRAY_HEIGHT; height += 3) { // ← Реже (каждые 3 блока)
            double y = centerY + height;
            int particleCount = 2; // Больше частиц для эффекта облака
            double progress = (height - 30) / 10.0; // 0.0 - 1.0
            double horizontalSpread = 15.0 + progress * 9.0; // От 9 до 15 блоков
            double verticalSpeed = 0.05 - progress * 0.05; // Почти нулевая, потом падение

            ExplosionParticleUtils.spawnAgentOrangeGeyser(
                    level,
                    centerX,
                    y,
                    centerZ,
                    particleCount,
                    verticalSpeed,
                    horizontalSpread
            );
        }

        // 🔊 ПЕРИОДИЧЕСКИЕ ЗВУКИ
        long gameTime = level.getGameTime();

        if (gameTime % 20 == 0) {
            // Основной звук струи
            level.playSound(null, pos, SoundEvents.LAVA_POP, SoundSource.BLOCKS,
                    1.0F, 1.2F);
        }

        if (gameTime % 40 == 0) {
            // Звук давления/выброса
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS,
                    1F, 0.7F);
        }
    }
}

