package com.hbm_m.block.custom.explosives;

import com.hbm_m.particle.explosions.basic.ExplosionParticleUtils;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.level.Level;

import com.hbm_m.particle.ModExplosionParticles;
import com.hbm_m.util.explosions.general.BlastExplosionGenerator;
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
        level.sendParticles((SimpleParticleType) ModExplosionParticles.FLASH.get(),
                x, y, z, 1, 0, 0, 0, 0);
        ExplosionParticleUtils.spawnAirBombSparks(level, x, y, z);
        level.getServer().tell(new net.minecraft.server.TickTask(3, () ->
                ExplosionParticleUtils.spawnAirBombShockwave(level, x, y, z)));
        level.getServer().tell(new net.minecraft.server.TickTask(8, () ->
                ExplosionParticleUtils.spawnAirBombMushroomCloud(level, x, y, z)));}

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