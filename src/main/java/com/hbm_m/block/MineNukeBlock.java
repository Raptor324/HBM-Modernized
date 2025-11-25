package com.hbm_m.block;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.particle.ModExplosionParticles;
import com.hbm_m.sound.ModSounds;
import com.hbm_m.util.CraterGenerator;

import com.hbm_m.util.ShockwaveGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import org.jetbrains.annotations.Nullable;

public class MineNukeBlock extends Block implements EntityBlock {

    private static final float EXPLOSION_POWER = 20.0F;
    private static final int CRATER_RADIUS = 20;
    private static final int CRATER_DEPTH = 3;
    private static final DirectionProperty FACING = DirectionProperty.create("facing");
    private static final VoxelShape SHAPE = Shapes.box(0.25, 0, 0.25, 0.75, 0.25, 0.75);

    private static final Random RANDOM = new Random();

    public MineNukeBlock(Properties properties) {
        super(BlockBehaviour.Properties.of().strength(3.5F));
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
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

        playRandomDetonationSound(level, pos);

        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, EXPLOSION_POWER, false, Level.ExplosionInteraction.NONE);


        if (level instanceof ServerLevel serverLevel) {
            spawnExplosionEffects(serverLevel, x, y, z);
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

    private void spawnExplosionEffects(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ModExplosionParticles.FLASH.get(), x, y, z, 1, 0, 0, 0, 0);
        for (int i = 0; i < 200; i++) {
            double dx = (level.random.nextDouble() - 0.5) * 4.0;
            double dy = level.random.nextDouble() * 3.0;
            double dz = (level.random.nextDouble() - 0.5) * 4.0;
            level.sendParticles(ModExplosionParticles.EXPLOSION_SPARK.get(), x, y, z, 1, dx, dy, dz, 1.0);
        }
        for (int ring = 0; ring < 3; ring++) {
            double ringY = y + ring * 0.5;
            level.sendParticles(ModExplosionParticles.SHOCKWAVE.get(), x, ringY, z, 1, 0, 0, 0, 0);
        }
        for (int i = 0; i < 40; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 4.0;
            double offsetZ = (level.random.nextDouble() - 0.5) * 4.0;
            double ySpeed = 0.5 + level.random.nextDouble() * 0.3;
            level.sendParticles(ModExplosionParticles.MUSHROOM_SMOKE.get(), x + offsetX, y, z + offsetZ, 1, offsetX * 0.05, ySpeed, offsetZ * 0.05, 1.0);
        }
        for (int i = 0; i < 60; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double radius = 5.0 + level.random.nextDouble() * 8.0;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double capY = y + 15 + level.random.nextDouble() * 5;
            double xSpeed = Math.cos(angle) * 0.3;
            double ySpeed = -0.1 + level.random.nextDouble() * 0.1;
            double zSpeed = Math.sin(angle) * 0.3;
            level.sendParticles(ModExplosionParticles.MUSHROOM_SMOKE.get(), x + offsetX, capY, z + offsetZ, 1, xSpeed, ySpeed, zSpeed, 1.0);
        }
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
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
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
