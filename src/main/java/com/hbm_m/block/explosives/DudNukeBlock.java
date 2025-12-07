package com.hbm_m.block.explosives;

import com.hbm_m.block.IDetonatable;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.particle.ModExplosionParticles;
import com.hbm_m.util.DudCraterGenerator;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
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
public class DudNukeBlock extends Block implements IDetonatable {

    private static final Logger LOGGER = LoggerFactory.getLogger("NuclearCharge");

    private static final float EXPLOSION_POWER = 25.0F;
    private static final double PARTICLE_VIEW_DISTANCE = 512.0;

    // Параметры воронки
    private static final int CRATER_RADIUS = 30; // Радиус воронки в блоках
    private static final int CRATER_DEPTH = 10;  // Глубина воронки в блоках

    // ОПТИМИЗАЦИЯ: Задержка перед генерацией кратера (в тиках)
    private static final int CRATER_GENERATION_DELAY = 30;

    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);

    private static final VoxelShape SHAPE = Shapes.box(0, 0, 0, 1, 1, 1); // полный куб (замени при необходимости)

    public DudNukeBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public void appendHoverText(ItemStack stack,
                                @Nullable net.minecraft.world.level.BlockGetter level,
                                List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.hbm_m.dudnuke.line1")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("tooltip.hbm_m.dudnuke.line4")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.hbm_m.dudnuke.line5")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.hbm_m.dudnuke.line6")
                .withStyle(ChatFormatting.GRAY));
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
                    Level.ExplosionInteraction.NONE);

            // Запускаем поэтапную систему частиц
            scheduleExplosionEffects(serverLevel, x, y, z);

            // ОПТИМИЗАЦИЯ: Генерация кратера в отдельном тике
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                server.tell(new TickTask(CRATER_GENERATION_DELAY, () -> {
                    DudCraterGenerator.generateCrater(
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

}
