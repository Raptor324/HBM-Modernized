package com.hbm_m.block.explosives;
import java.util.List;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.List;

import com.hbm_m.block.IDetonatable;
import com.hbm_m.particle.ModExplosionParticles;
import com.hbm_m.util.BlastExplosionGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class C4Block extends Block implements IDetonatable {
    private static final float EXPLOSION_POWER = 25.0F;
    private static final double PARTICLE_VIEW_DISTANCE = 512.0;
    private static final int DETONATION_RADIUS = 6;
    // Параметры воронки
    private static final int CRATER_RADIUS = 15; // Радиус воронки в блоках
    private static final int CRATER_DEPTH = 25; // Глубина воронки в блоках

    // Блоки для поверхности воронки (замени на свои)
    private static final Block BLOCK1 = Blocks.BLACK_CONCRETE_POWDER; // Замени на свой
    private static final Block BLOCK2 = Blocks.GRAY_CONCRETE_POWDER; // Замени на свой
    private static final Block BLOCK3 = Blocks.BLACK_CONCRETE; // Замени на свой

    public C4Block(Properties properties) {
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

            // Создаём воронку с задержкой (после взрывной волны)
            serverLevel.getServer().tell(new net.minecraft.server.TickTask(30, () -> {
                // НОВЫЙ ВЫЗОВ - теперь не нужно передавать блоки
               BlastExplosionGenerator.generateNaturalCrater(
                        serverLevel,
                        pos,
                        CRATER_RADIUS,
                        CRATER_DEPTH
                );
            }));


            return true;
        }
        return false;
    }

    private void scheduleExplosionEffects(ServerLevel level, double x, double y, double z) {
        // Фаза 1: Яркая вспышка (мгновенно)
        spawnFlash(level, x, y, z);

        // Фаза 2: Искры (0-10 тиков)
        spawnSparks(level, x, y, z);

        // Фаза 3: Взрывная волна (5 тиков задержки)
        level.getServer().tell(new net.minecraft.server.TickTask(5, () -> {
            spawnShockwave(level, x, y, z);
        }));

        // Фаза 4: Гриб из дыма (10 тиков задержки)
        level.getServer().tell(new net.minecraft.server.TickTask(10, () -> {
            spawnMushroomCloud(level, x, y, z);
        }));
    }

    private void spawnFlash(ServerLevel level, double x, double y, double z) {
        // ✅ Каст к SimpleParticleType
        level.sendParticles(
                (SimpleParticleType) ModExplosionParticles.FLASH.get(),
                x, y, z, 1, 0, 0, 0, 0
        );
    }

    private void spawnSparks(ServerLevel level, double x, double y, double z) {
        for (int i = 0; i < 400; i++) {
            double xSpeed = (level.random.nextDouble() - 0.5) * 6.0;
            double ySpeed = level.random.nextDouble() * 5.0;
            double zSpeed = (level.random.nextDouble() - 0.5) * 6.0;
            level.sendParticles(
                    (SimpleParticleType) ModExplosionParticles.EXPLOSION_SPARK.get(),
                    x, y, z, 1, xSpeed, ySpeed, zSpeed, 1.5
            );
        }
    }

    private void spawnShockwave(ServerLevel level, double x, double y, double z) {
        for (int ring = 0; ring < 6; ring++) {
            double ringY = y + (ring * 0.3);
            level.sendParticles(
                    (SimpleParticleType) ModExplosionParticles.SHOCKWAVE.get(),
                    x, ringY, z, 1, 0, 0, 0, 0
            );
        }
    }

    private void spawnMushroomCloud(ServerLevel level, double x, double y, double z) {
        // Стебель
        for (int i = 0; i < 150; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 6.0;
            double offsetZ = (level.random.nextDouble() - 0.5) * 6.0;
            double ySpeed = 0.8 + level.random.nextDouble() * 0.4;
            level.sendParticles(
                    (SimpleParticleType) ModExplosionParticles.MUSHROOM_SMOKE.get(),
                    x + offsetX, y, z + offsetZ,
                    1,
                    offsetX * 0.08, ySpeed, offsetZ * 0.08,
                    1.5
            );
        }
        // Шапка - ВСЁ ТО ЖЕ САМОЕ
        for (int i = 0; i < 250; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double radius = 8.0 + level.random.nextDouble() * 12.0;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double capY = y + 20 + level.random.nextDouble() * 10;
            double xSpeed = Math.cos(angle) * 0.5;
            double ySpeed = -0.1 + level.random.nextDouble() * 0.2;
            double zSpeed = Math.sin(angle) * 0.5;
            level.sendParticles(
                    (SimpleParticleType) ModExplosionParticles.MUSHROOM_SMOKE.get(),
                    x + offsetX, capY, z + offsetZ,
                    1,
                    xSpeed, ySpeed, zSpeed,
                    1.5
            );
        }
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