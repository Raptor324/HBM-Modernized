package com.hbm_m.block.explosives;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.particle.ModExplosionParticles;
import com.hbm_m.particle.explosions.ExplosionParticleUtils;
import com.hbm_m.sound.ModSounds;

import com.hbm_m.util.ShockwaveGenerator;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

import org.jetbrains.annotations.Nullable;


public class MineNukeBlock extends Block implements EntityBlock {

    private static final float EXPLOSION_POWER = 20.0F;
    private static final int CRATER_RADIUS = 20;
    private static final int CRATER_DEPTH = 3;
    private static final DirectionProperty FACING = DirectionProperty.create("facing");
    private static final VoxelShape SHAPE = Shapes.box(0.25, 0, 0.25, 0.75, 0.25, 0.75);
    private static final VoxelShape COLLISION_SHAPE = Shapes.box(0.25, 0.0, 0.25, 0.75, 0.25, 0.75);
    private static final Random RANDOM = new Random();

    // Новые параметры для урона
    private static final float DAMAGE_RADIUS = 30.0f; // Радиус поражения урона (в блоках)
    private static final float DAMAGE_AMOUNT = 200.0f; // 200 урона = 100 полных сердец
    private static final float MAX_DAMAGE_DISTANCE = 25.0f; // Максимальное расстояние для полного урона


    public MineNukeBlock(Properties properties) {
        super(BlockBehaviour.Properties.of().strength(3.5F));
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    public void appendHoverText(ItemStack stack,
                                @Nullable BlockGetter level,
                                List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.hbm_m.mine_nuke.line1")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("tooltip.hbm_m.mine_nuke.line2")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.hbm_m.mine_nuke.line3")
                .withStyle(ChatFormatting.GRAY));
    }


    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return COLLISION_SHAPE;
    }
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return COLLISION_SHAPE;
    }



    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide) {
            if (!(entity instanceof Player player && player.isCreative()) && entity instanceof LivingEntity) {
                // Взорвать мину
                detonate(level, pos);
                level.removeBlock(pos, false);
            }
        }
        super.stepOn(level, pos, state, entity);
    }


    private void detonate(Level level, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        // Повышенная громкость звука
        playRandomDetonationSound(level, pos);

        level.explode(null, x, y, z, EXPLOSION_POWER, false, Level.ExplosionInteraction.TNT);

        // Урон в зоне действия (НОВОЕ)
        if (level instanceof ServerLevel serverLevel) {
            dealExplosionDamage(serverLevel, x, y, z);
        }

        if (level instanceof ServerLevel serverLevel) {
            scheduleExplosionEffects(serverLevel, x, y, z);
            MinecraftServer server = serverLevel.getServer();
            if (server != null) {
                server.tell(new TickTask(5, () -> {
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
        }
    }

    // НОВЫЙ МЕТОД: Нанесение урона в зоне действия мины
    private void dealExplosionDamage(ServerLevel serverLevel, double x, double y, double z) {
        List<LivingEntity> entitiesNearby = serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(x - DAMAGE_RADIUS, y - DAMAGE_RADIUS, z - DAMAGE_RADIUS,
                        x + DAMAGE_RADIUS, y + DAMAGE_RADIUS, z + DAMAGE_RADIUS)
        );

        for (LivingEntity entity : entitiesNearby) {
            double distanceToEntity = Math.sqrt(
                    Math.pow(entity.getX() - x, 2) +
                            Math.pow(entity.getY() - y, 2) +
                            Math.pow(entity.getZ() - z, 2)
            );

            // Проверяем, находится ли сущность в радиусе поражения
            if (distanceToEntity <= DAMAGE_RADIUS) {
                // Вычисляем урон в зависимости от расстояния
                // На близком расстоянии (0-5 блоков): полный урон
                // На среднем расстоянии (5-25 блоков): уменьшающийся урон
                // На дальнем расстоянии (25+ блоков): минимальный урон
                float damage = DAMAGE_AMOUNT;

                if (distanceToEntity > MAX_DAMAGE_DISTANCE) {
                    // Линейное снижение урона на расстоянии
                    float remainingDistance = DAMAGE_RADIUS - MAX_DAMAGE_DISTANCE;
                    float damageDistance = (float) distanceToEntity - MAX_DAMAGE_DISTANCE;
                    damage = DAMAGE_AMOUNT * (1.0f - (damageDistance / remainingDistance)) * 0.5f; // Минимум 50% урона
                }

                // Наносим урон с источником (взрыв)
                entity.hurt(entity.damageSources().explosion(null), damage);

            }
        }
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


    private void playRandomDetonationSound(Level level, BlockPos pos) {
        List<SoundEvent> sounds = Arrays.asList(
                ModSounds.MUKE_EXPLOSION.orElse(null),
                ModSounds.MUKE_EXPLOSION.orElse(null),
                ModSounds.MUKE_EXPLOSION.orElse(null)
        );
        sounds.removeIf(Objects::isNull);
        if (!sounds.isEmpty()) {
            SoundEvent sound = sounds.get(RANDOM.nextInt(sounds.size()));
            // Повышенная громкость: с 1.0F на 4.0F для дальней слышимости
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, sound, SoundSource.BLOCKS, 4.0F, 1.0F);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.MINE_NUKE_BLOCK_ENTITY.get().create(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
}