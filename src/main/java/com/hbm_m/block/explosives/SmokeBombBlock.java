package com.hbm_m.block.explosives;

import com.hbm_m.block.IDetonatable;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.particle.ModExplosionParticles;
import com.hbm_m.util.ShockwaveGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.TickTask;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;


public class SmokeBombBlock extends Block implements IDetonatable {
    private static final float EXPLOSION_POWER = 25.0F;
    private static final double PARTICLE_VIEW_DISTANCE = 512.0;

    // Параметры воронки
    private static final int CRATER_RADIUS = 25; // Радиус воронки в блоках
    private static final int CRATER_DEPTH = 8; // Глубина воронки в блоках
    private static final int DETONATION_RADIUS = 6;

    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);

    private static final VoxelShape SHAPE = Shapes.box(0, 0, 0, 1, 1, 1); // полный куб (замени при необходимости)

    public SmokeBombBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Блок поворачивается лицом к игроку при установке (противоположно взгляду игрока)
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
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

            // 2. Активируем соседние Detonatable блоки по цепочке
            triggerNearbyDetonations(serverLevel, pos, player);

            // Запускаем поэтапную систему частиц
            scheduleExplosionEffects(serverLevel, x, y, z);

            if (serverLevel.getServer() != null) {
                serverLevel.getServer().tell(new net.minecraft.server.TickTask(30, () -> {
                    // Взрыв (без разрушения блоков - за это отвечает кратер)
                    level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8.0F, Level.ExplosionInteraction.NONE);

                    // Генерация кратера
                    ShockwaveGenerator.generateCrater(
                            serverLevel,
                            pos,
                            CRATER_RADIUS,
                            CRATER_DEPTH,
                            ModBlocks.WASTE_LOG.get(),
                            ModBlocks.WASTE_PLANKS.get(),
                            ModBlocks.BURNED_GRASS.get()
                    );
                }));
            }

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