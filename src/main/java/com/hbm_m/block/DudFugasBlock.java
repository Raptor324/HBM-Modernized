package com.hbm_m.block;

import com.hbm_m.particle.ModExplosionParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DudFugasBlock extends Block implements IDetonatable {
    private static final float EXPLOSION_POWER = 80.0F;
    private static final double PARTICLE_VIEW_DISTANCE = 512.0;

    // Параметры воронки
    private static final int CRATER_RADIUS = 35; // Радиус воронки в блоках
    private static final int CRATER_DEPTH = 12; // Глубина воронки в блоках


    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);

    private static final VoxelShape SHAPE = Shapes.box(0, 0, 0, 1, 1, 1); // полный куб (замени при необходимости)

    public DudFugasBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    // Не ломается поршнями
    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    // Запретить ломать блок инструментом (игроком)
    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return 0.0F; // Нельзя сломать вообще
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
                    Level.ExplosionInteraction.TNT); // NONE = не разрушает блоки

            // Запускаем поэтапную систему частиц
            scheduleExplosionEffects(serverLevel, x, y, z);

            if (serverLevel.getServer() != null) {
                serverLevel.getServer().tell(new TickTask(30, () -> {
                    level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 40.0F, Level.ExplosionInteraction.TNT);


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
        level.getServer().tell(new TickTask(5, () -> {
            spawnShockwave(level, x, y, z);
        }));

        // Фаза 4: Гриб из дыма (10 тиков задержки)
        level.getServer().tell(new TickTask(10, () -> {
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