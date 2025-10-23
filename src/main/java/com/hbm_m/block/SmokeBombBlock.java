package com.hbm_m.block;


import com.hbm_m.particle.ModExplosionParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.RandomSource; // Import for RandomSource

public class SmokeBombBlock extends Block implements IDetonatable {
    private static final float EXPLOSION_POWER = 25.0F;

    public SmokeBombBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onDetonate(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            ServerLevel serverLevel = (ServerLevel) level;
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5;

            // Удаляем блок
            level.removeBlock(pos, false);

            // Создаем взрыв (без частиц по умолчанию)
            level.explode(null, x, y, z, EXPLOSION_POWER, Level.ExplosionInteraction.TNT);

            // Генерируем огонь вокруг места взрыва
            spawnFire(serverLevel, pos, 7); // Радиус 7 блоков

            // Запускаем поэтапную систему частиц
            scheduleExplosionEffects(serverLevel, x, y, z);

            return true;
        }
        return false;
    }

    private void spawnFire(ServerLevel level, BlockPos centerPos, int radius) {
        RandomSource random = level.random;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos currentPos = centerPos.offset(x, y, z);
                    // Проверяем, что блок является воздухом и есть блок под ним
                    if (level.getBlockState(currentPos).isAir() && level.getBlockState(currentPos.below()).isSolidRender(level, currentPos.below())) {
                        if (random.nextFloat() < 0.3F) { // 30% шанс создания огня
                            level.setBlockAndUpdate(currentPos, Blocks.FIRE.defaultBlockState());
                        }
                    }
                }
            }
        }
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
        // Центральная вспышка - используем addAlwaysVisibleParticle через sendParticles с force=true
        level.sendParticles(
                ModExplosionParticles.FLASH.get(),
                x, y, z,
                1, // count
                0, 0, 0, // offset
                0 // speed
        );
    }

    private void spawnSparks(ServerLevel level, double x, double y, double z) {
        // 200 искр разлетающихся во все стороны
        for (int i = 0; i < 400; i++) {
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

    private void spawnShockwave(ServerLevel level, double x, double y, double z) {
        // 3 кольца взрывной волны на разной высоте
        for (int ring = 0; ring < 3; ring++) {
            double ringY = y + (ring * 0.5);

            level.sendParticles(
                    ModExplosionParticles.SHOCKWAVE.get(),
                    x, ringY, z,
                    1,
                    0, 0, 0,
                    0
            );
        }
    }

    private void spawnMushroomCloud(ServerLevel level, double x, double y, double z) {
        // Стебель гриба (вертикальная колонна дыма)
        for (int i = 0; i < 80; i++) {
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
        for (int i = 0; i < 120; i++) {
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