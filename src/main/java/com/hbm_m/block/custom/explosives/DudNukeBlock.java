package com.hbm_m.block.custom.explosives;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.particle.ModExplosionParticles;
import com.hbm_m.particle.explosions.basic.ExplosionParticleUtils;
import com.hbm_m.util.explosions.nuclear.CraterGenerator;
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
 * ✅ ДАДО-БОЕВАЯ ГОЛОВКА v4
 *
 * Улучшения:
 * ✅ Полностью динамический размер кратера
 * ✅ Радиус определяется ТОЛЬКО силой пробития лучей
 * ✅ Синхронизация лучей исправлена
 */
public class DudNukeBlock extends Block implements IDetonatable {

    private static final Logger LOGGER = LoggerFactory.getLogger("DudNuke");

    private static final float EXPLOSION_POWER = 25.0F;
    private static final double PARTICLE_VIEW_DISTANCE = 512.0;
    private static final int CRATER_GENERATION_DELAY = 30;

    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);

    private static final VoxelShape SHAPE = Shapes.box(0, 0, 0, 1, 1, 1);

    public DudNukeBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public void appendHoverText(ItemStack stack,
                                @Nullable BlockGetter level,
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

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return 0.0F;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
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

            // ✅ ГЕНЕРАЦИЯ КРАТЕРА (радиус определяется лучами!)
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                server.tell(new TickTask(CRATER_GENERATION_DELAY, () -> {
                    CraterGenerator.generateCrater(
                            serverLevel,
                            pos,
                            ModBlocks.SELLAFIELD_SLAKED.get(),
                            ModBlocks.SELLAFIELD_SLAKED1.get(),
                            ModBlocks.SELLAFIELD_SLAKED2.get(),
                            ModBlocks.SELLAFIELD_SLAKED3.get(),
                            ModBlocks.WASTE_LOG.get(),
                            ModBlocks.WASTE_PLANKS.get(),
                            ModBlocks.BURNED_GRASS.get(),
                            ModBlocks.DEAD_DIRT.get()
                    );

                    LOGGER.info("Кратер успешно сгенерирован в позиции: {}", pos);
                }));
            }

            return true;
        }

        return false;
    }

    /**
     * ✅ Планирование эффектов взрыва
     */
    private void scheduleExplosionEffects(ServerLevel level, double x, double y, double z) {
        // ✅ Flash
        level.sendParticles(
                (SimpleParticleType) ModExplosionParticles.FLASH.get(),
                x, y, z, 1, 0, 0, 0, 0
        );

        // ✅ Sparks
        ExplosionParticleUtils.spawnAirBombSparks(level, x, y, z);

        // ✅ Shockwave через 3 тика
        level.getServer().tell(new TickTask(3, () ->
                ExplosionParticleUtils.spawnAirBombShockwave(level, x, y, z)));

        // ✅ Mushroom Cloud через 8 тиков
        level.getServer().tell(new TickTask(8, () ->
                ExplosionParticleUtils.spawnAirBombMushroomCloud(level, x, y, z)));
    }

}