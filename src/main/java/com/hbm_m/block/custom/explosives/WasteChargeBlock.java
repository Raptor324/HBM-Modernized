package com.hbm_m.block.custom.explosives;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.particle.ModExplosionParticles;

import com.hbm_m.particle.explosions.basic.ExplosionParticleUtils;
import com.hbm_m.util.explosions.freaky.WasteBlastGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class WasteChargeBlock extends Block implements IDetonatable {

    private static final float EXPLOSION_POWER = 25.0F;
    private static final double PARTICLE_VIEW_DISTANCE = 512.0;
    private static final int DETONATION_RADIUS = 6;
    // Параметры воронки
    private static final int CRATER_RADIUS = 60; // Радиус воронки в блоках
    private static final int CRATER_DEPTH = 20; // Глубина воронки в блоках

    public WasteChargeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onDetonate(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            ServerLevel serverLevel = (ServerLevel) level;
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5;

            level.removeBlock(pos, false);

            // Взрыв (без разрушения блоков - за это отвечает воронка)
            level.explode(null, x, y, z, EXPLOSION_POWER,
                    Level.ExplosionInteraction.NONE); // NONE = не разрушает блоки

            // Запускаем поэтапную систему частиц
            scheduleExplosionEffects(serverLevel, x, y, z);

            // 2. Активируем соседние Detonatable блоки по цепочке
            triggerNearbyDetonations(serverLevel, pos, player);

            // ИСПРАВЛЕНИЕ: Проверка на null для сервера
            if (serverLevel.getServer() != null) {
                // Создаём воронку с задержкой (после взрывной волны)
                serverLevel.getServer().tell(new net.minecraft.server.TickTask(30, () -> {
                    // НОВОЕ: Используем массив из 4 вариантов селлафита
                    Block[] sellafieldBlocks = {
                            ModBlocks.SELLAFIELD_SLAKED.get(),
                            ModBlocks.SELLAFIELD_SLAKED1.get(),
                            ModBlocks.SELLAFIELD_SLAKED2.get(),
                            ModBlocks.SELLAFIELD_SLAKED3.get()
                    };

                    // Анимированная версия (2 секунды)
                    WasteBlastGenerator.generateCrater(
                            serverLevel,
                            pos,
                            CRATER_RADIUS,
                            CRATER_DEPTH,
                            sellafieldBlocks
                    );

                    // ИЛИ мгновенная версия (раскомментируй если нужна)
                    // CraterGenerator.generateCraterInstant(
                    //         serverLevel, pos, CRATER_RADIUS, CRATER_DEPTH,
                    //         sellafieldBlocks
                    // );
                }));
            }

            return true;
        }

        return false;
    }

    private void scheduleExplosionEffects(ServerLevel level, double x, double y, double z) {
        // ✅ Flash - точно те же параметры
        level.sendParticles(
                (SimpleParticleType) ModExplosionParticles.FLASH.get(),
                x, y, z, 1, 0, 0, 0, 0
        );

        // ✅ Sparks - 400 частиц с ТОЧНЫМИ скоростями
        ExplosionParticleUtils.spawnAirBombSparks(level, x, y, z);

        // ✅ Shockwave через 3 тика - точно те же кольца
        level.getServer().tell(new net.minecraft.server.TickTask(3, () ->
                ExplosionParticleUtils.spawnAirBombShockwave(level, x, y, z)));

        // ✅ Mushroom Cloud через 8 тиков - ТОЧНО те же параметры
        level.getServer().tell(new net.minecraft.server.TickTask(8, () ->
                ExplosionParticleUtils.spawnAirBombMushroomCloud(level, x, y, z)));
    }

    private void triggerNearbyDetonations(ServerLevel serverLevel, BlockPos pos, Player player) {
        for (int x = -DETONATION_RADIUS; x <= DETONATION_RADIUS; x++) {
            for (int y = -DETONATION_RADIUS; y <= DETONATION_RADIUS; y++) {
                for (int z = -DETONATION_RADIUS; z <= DETONATION_RADIUS; z++) {
                    double dist = Math.sqrt(x * x + y * y + z * z);
                    if (dist <= DETONATION_RADIUS && dist > 0) {
                        BlockPos checkPos = pos.offset(x, y, z);
                        BlockState checkState = serverLevel.getBlockState(checkPos);
                        Block block = checkState.getBlock();
                        if (block instanceof IDetonatable) {
                            IDetonatable detonatable = (IDetonatable) block;
                            int delay = (int)(dist * 2); // Задержка зависит от расстояния
                            serverLevel.getServer().tell(new TickTask(delay, () -> {
                                detonatable.onDetonate(serverLevel, checkPos, checkState, player);
                            }));
                        }
                    }
                }
            }
        }
    }
}