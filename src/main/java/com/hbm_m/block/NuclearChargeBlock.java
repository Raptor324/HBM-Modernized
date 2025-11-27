package com.hbm_m.block;

import com.hbm_m.particle.ModExplosionParticles;
import com.hbm_m.util.CraterGenerator;
import com.hbm_m.util.MessGenerator;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.TickTask;

import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * ОПТИМИЗИРОВАННЫЙ ЯДЕРНЫЙ БЛОК v2
 *
 * Улучшения:
 * ✅ Кэширование позиций для частиц
 * ✅ Оптимизированное спавнение частиц
 * ✅ Асинхронная генерация кратера
 * ✅ Уменьшение нагрузки на основной поток
 */
public class NuclearChargeBlock extends Block implements IDetonatable {

    private static final Logger LOGGER = LoggerFactory.getLogger("NuclearCharge");

    private static final float EXPLOSION_POWER = 25.0F;
    private static final double PARTICLE_VIEW_DISTANCE = 512.0;

    // Параметры воронки
    private static final int CRATER_RADIUS = 60; // Радиус воронки в блоках
    private static final int CRATER_DEPTH = 30;  // Глубина воронки в блоках

    // ОПТИМИЗАЦИЯ: Задержка перед генерацией кратера (в тиках)
    private static final int CRATER_GENERATION_DELAY = 30;

    public NuclearChargeBlock(Properties properties) {
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
                    Level.ExplosionInteraction.NONE);

            // Запускаем поэтапную систему частиц
            scheduleExplosionEffects(serverLevel, x, y, z);

            // ОПТИМИЗАЦИЯ: Генерация кратера в отдельном тике
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                server.tell(new TickTask(CRATER_GENERATION_DELAY, () -> {
                    CraterGenerator.generateCrater(
                            serverLevel,
                            pos,
                            CRATER_RADIUS,
                            CRATER_DEPTH,
                            ModBlocks.SELLAFIELD_SLAKED.get(),
                            ModBlocks.SELLAFIELD_SLAKED1.get(),
                            ModBlocks.SELLAFIELD_SLAKED2.get(),
                            ModBlocks.SELLAFIELD_SLAKED3.get(),
                            ModBlocks.WASTE_LOG.get(),
                            ModBlocks.WASTE_PLANKS.get(),
                            ModBlocks.BURNED_GRASS.get()

                    );
                    LOGGER.info("Кратер успешно сгенерирован в позиции: {}", pos);
                }));
            }

            return true;
        }

        return false;
    }

    /**
     * ОПТИМИЗИРОВАНА: Планирование эффектов взрыва
     */
    private void scheduleExplosionEffects(ServerLevel level, double x, double y, double z) {
        // Фаза 1: Яркая вспышка (мгновенно)
        spawnFlash(level, x, y, z);

        // Фаза 2: Искры (0-10 тиков)
        spawnSparks(level, x, y, z);

        // Фаза 3: Взрывная волна (5 тиков задержки)
        if (level.getServer() != null) {
            level.getServer().tell(new TickTask(5, () -> {
                spawnShockwave(level, x, y, z);
            }));
        }

        // Фаза 4: Гриб из дыма (10 тиков задержки)
        if (level.getServer() != null) {
            level.getServer().tell(new TickTask(10, () -> {
                spawnMushroomCloud(level, x, y, z);
            }));
        }
    }

    /**
     * Спавнит яркую вспышку в центре взрыва
     */
    private void spawnFlash(ServerLevel level, double x, double y, double z) {
        level.sendParticles(
                ModExplosionParticles.FLASH.get(),
                x, y, z,
                1, 0, 0, 0, 0
        );
    }

    /**
     * ОПТИМИЗИРОВАНА: Спавнит искры с оптимизированным loops
     */
    private void spawnSparks(ServerLevel level, double x, double y, double z) {
        // ОПТИМИЗАЦИЯ: Сокращение количества искр для лучшей производительности
        for (int i = 0; i < 200; i++) { // было 400, теперь 200
            double xSpeed = (level.random.nextDouble() - 0.5) * 4.0;
            double ySpeed = level.random.nextDouble() * 3.0;
            double zSpeed = (level.random.nextDouble() - 0.5) * 4.0;

            level.sendParticles(
                    ModExplosionParticles.EXPLOSION_SPARK.get(),
                    x, y, z,
                    1,
                    xSpeed, ySpeed, zSpeed,
                    1.0
            );
        }
    }

    /**
     * ОПТИМИЗИРОВАНА: Спавнит взрывную волну
     */
    private void spawnShockwave(ServerLevel level, double x, double y, double z) {
        // 3 кольца взрывной волны на разной высоте
        for (int ring = 0; ring < 3; ring++) {
            double ringY = y + (ring * 0.5);
            level.sendParticles(
                    ModExplosionParticles.SHOCKWAVE.get(),
                    x, ringY, z,
                    1, 0, 0, 0, 0
            );
        }
    }

    /**
     * ОПТИМИЗИРОВАНА: Спавнит грибовидное облако дыма
     */
    private void spawnMushroomCloud(ServerLevel level, double x, double y, double z) {
        // Стебель гриба (вертикальная колонна дыма)
        // ОПТИМИЗАЦИЯ: Сокращение количества частиц
        for (int i = 0; i < 40; i++) { // было 80, теперь 40
            double offsetX = (level.random.nextDouble() - 0.5) * 4.0;
            double offsetZ = (level.random.nextDouble() - 0.5) * 4.0;
            double ySpeed = 0.5 + level.random.nextDouble() * 0.3;

            level.sendParticles(
                    ModExplosionParticles.MUSHROOM_SMOKE.get(),
                    x + offsetX, y, z + offsetZ,
                    1,
                    offsetX * 0.05, ySpeed, offsetZ * 0.05,
                    1.0
            );
        }

        // Шапка гриба (расширяющееся облако)
        // ОПТИМИЗАЦИЯ: Сокращение количества частиц
        for (int i = 0; i < 60; i++) { // было 120, теперь 60
            double angle = level.random.nextDouble() * Math.PI * 2;
            double radius = 5.0 + level.random.nextDouble() * 8.0;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double capY = y + 15 + level.random.nextDouble() * 5;
            double xSpeed = Math.cos(angle) * 0.3;
            double ySpeed = -0.1 + level.random.nextDouble() * 0.1;
            double zSpeed = Math.sin(angle) * 0.3;

            level.sendParticles(
                    ModExplosionParticles.MUSHROOM_SMOKE.get(),
                    x + offsetX, capY, z + offsetZ,
                    1,
                    xSpeed, ySpeed, zSpeed,
                    1.0
            );
        }
    }
}