package com.hbm_m.block.machines.rbmk;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.util.ContaminationUtil;
import com.hbm_m.util.ContaminationUtil.ContaminationType;
import com.hbm_m.util.ContaminationUtil.HazardType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 port of {@code RBMKDebrisRadiating}: the worst of the meltdown rubble - an exposed core
 * fragment.
 *
 * <p>It dumps an enormous dose over a 100-block radius, falling off with distance, and slowly
 * counts its {@code decay} state up until it finally settles into ordinary burning rubble. Boron
 * sand alongside it speeds that decay up forty-fold, which is the intended way to make the site
 * survivable - otherwise it sits there irradiating everything for a very long time.</p>
 */
public class RBMKDebrisRadiatingBlock extends Block {

    /** The original's block metadata, counted 0-15 before it settles down. */
    public static final IntegerProperty DECAY = IntegerProperty.create("decay", 0, 15);

    /**
     * CE emits {@code 100 * chance} rads and {@code 40 * chance} of fire over a 32-block radius,
     * where {@code chance} is the same 1000 (or 25 next to boron sand) that governs how fast the
     * rubble decays - so packing boron around the site cuts both the dose and the heat forty-fold
     * as well as speeding up the decay. The port used a flat 1,000,000 rads over 100 blocks and no
     * fire at all, which was both far fiercer and completely indifferent to boron.
     */
    private static final double RANGE = 32D;
    private static final float RAD_PER_CHANCE = 100F;
    private static final float FIRE_PER_CHANCE = 40F;

    public RBMKDebrisRadiatingBlock(Properties props) {
        super(props);
        registerDefaultState(getStateDefinition().any().setValue(DECAY, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DECAY);
    }

    private static int nextDelay(RandomSource random) {
        return 20 + random.nextInt(20);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) level.scheduleTick(pos, this, nextDelay(level.random));
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // CE picks one random side per tick and lets that single side decide the decay chance,
        // which is also what scales the radiation. Sample it once, up front, so both agree.
        Direction sampled = Direction.values()[random.nextInt(6)];
        int chance = RBMKDebrisBurningBlock.isSuppressant(level.getBlockState(pos.relative(sampled))) ? 25 : 1000;

        radiate(level, pos, RAD_PER_CHANCE * chance);
        RBMKDebrisBurningBlock.scorch(level, pos, RANGE, FIRE_PER_CHANCE * chance);

        if (random.nextInt(5) == 0) {
            spawnFlame(level, pos, random);
            level.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS,
                    1.0F + random.nextFloat(), random.nextFloat() * 0.7F + 0.3F);
        }

        BlockPos side = pos.relative(sampled);
        if (random.nextInt(10) == 0 && level.getBlockState(side).isAir()) {
            level.setBlockAndUpdate(side, ModBlocks.GAS_MELTDOWN.get().defaultBlockState());
        }

        if (random.nextInt(chance) == 0) {
            int decay = state.getValue(DECAY);
            if (decay < 15) {
                level.setBlock(pos, state.setValue(DECAY, decay + 1), 2);
                level.scheduleTick(pos, this, nextDelay(random));
            } else {
                level.setBlockAndUpdate(pos, ModBlocks.RBMK_DEBRIS_BURNING.get().defaultBlockState());
            }
        } else {
            level.scheduleTick(pos, this, nextDelay(random));
        }
    }

    /** Dose falls off with the square of the distance, exactly as CE's {@code radiate} does. */
    private static void radiate(Level level, BlockPos pos, float rads) {
        AABB area = new AABB(pos).inflate(RANGE);
        double cx = pos.getX() + 0.5, cy = pos.getY() + 0.5, cz = pos.getZ() + 0.5;

        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area)) {
            double dx = e.getX() - cx;
            double dy = (e.getY() + e.getEyeHeight()) - cy;
            double dz = e.getZ() - cz;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len > RANGE) continue;

            double dmgLen = Math.max(len, RANGE * 0.05D);
            float dose = (float) (rads / (dmgLen * dmgLen));
            ContaminationUtil.contaminate(e, HazardType.RADIATION, ContaminationType.CREATIVE, dose);
        }
    }

    /**
     * The original fires a "rbmkflame" particle packet from the server on a 1-in-5 tick roll,
     * positioned a little above the rubble; here the server spawns it directly so it reaches
     * every nearby client the same way.
     */
    static void spawnFlame(net.minecraft.server.level.ServerLevel level, BlockPos pos,
                            net.minecraft.util.RandomSource random) {
        level.sendParticles(com.hbm_m.particle.ModParticleTypes.RBMK_FLAME.get(),
                pos.getX() + 0.25 + random.nextDouble() * 0.5,
                pos.getY() + 1.75,
                pos.getZ() + 0.25 + random.nextDouble() * 0.5,
                1, 0, 0, 0, 0);
    }
}
