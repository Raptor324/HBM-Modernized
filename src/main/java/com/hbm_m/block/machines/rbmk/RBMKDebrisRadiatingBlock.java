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
 * <p>Every tick cycle it dumps a flat 1,000,000 rads, divided by the explosion resistance of the
 * blocks along the ray to each target's eye and by the square of the distance, over a 100-block
 * radius; anything within 5 blocks also takes flat 100 {@code inFire} damage (no ignite), and a
 * marshmallow in hand roasts 1-in-100 within 10. It counts its {@code decay} state up until it
 * finally settles into ordinary burning rubble. Boron sand alongside it speeds that decay up
 * forty-fold (1-in-25 instead of 1-in-1000), which is the intended way to make the site
 * survivable - otherwise it sits there irradiating everything for a very long time.</p>
 */
public class RBMKDebrisRadiatingBlock extends Block {

    /** The original's block metadata, counted 0-15 before it settles down. */
    public static final IntegerProperty DECAY = IntegerProperty.create("decay", 0, 15);

    /** 1.7.10 {@code RBMKDebrisRadiating.radiate}: flat dose and 100-block reach. */
    private static final float RADS = 1_000_000F;
    private static final double RANGE = 100D;

    public RBMKDebrisRadiatingBlock(Properties props) {
        super(props);
        registerDefaultState(getStateDefinition().any().setValue(DECAY, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DECAY);
    }

    /** 1.7.10 {@code RBMKDebrisRadiating.tickRate}: 20-40 ticks between updates. */
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
        radiate(level, pos);
        // 1.7.10 runs the fire term inside radiate's entity loop; as its own helper here, with
        // the same flat inFire-100-within-5 semantics (no ignite).
        RBMKDebrisBurningBlock.scorch(level, pos);

        if (random.nextInt(5) == 0) {
            spawnFlame(level, pos, random);
            level.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS,
                    1.0F + random.nextFloat(), random.nextFloat() * 0.7F + 0.3F);
        }

        // 1.7.10 picks one random side per tick and lets that single side decide gas spread and
        // the decay chance. Sample it once, up front, so both agree.
        Direction sampled = Direction.values()[random.nextInt(6)];
        BlockPos side = pos.relative(sampled);
        BlockState neighbour = level.getBlockState(side);

        if (random.nextInt(10) == 0 && neighbour.isAir()) {
            level.setBlockAndUpdate(side, ModBlocks.GAS_MELTDOWN.get().defaultBlockState());
        }

        // Boron sand helps stop the fission reaction; 0.66% chance every 20-40 ticks for one side
        // (1.7.10 RBMKDebrisRadiating.updateTick: chance = 25 with boron sand, else 1000). The
        // 25-vs-1000 roll only affects the decay speed, never the dose.
        int chance = RBMKDebrisBurningBlock.isBoronSuppressant(neighbour) ? 25 : 1000;

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

    /**
     * 1:1 with 1.7.10 {@code RBMKDebrisRadiating.radiate}: the flat 1,000,000 rads are divided by
     * the explosion resistance of every block marched on the ray to the target's eye (walls and
     * lead shield whatever is behind them) and by the square of the distance; the resistance sum
     * is floored at 1. A marshmallow in hand roasts 1-in-100 within 10. The fire term (flat
     * {@code inFire}-100 within 5 blocks, no ignite) is applied right after, see
     * {@link RBMKDebrisBurningBlock#scorch}.
     */
    private static void radiate(ServerLevel level, BlockPos pos) {
        double cx = pos.getX() + 0.5, cy = pos.getY() + 0.5, cz = pos.getZ() + 0.5;

        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(cx, cy, cz, cx, cy, cz).inflate(RANGE))) {
            double dx = e.getX() - cx;
            double dy = (e.getY() + e.getEyeHeight()) - cy;
            double dz = e.getZ() - cz;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1.0E-4) len = 1.0E-4;
            double nx = dx / len, ny = dy / len, nz = dz / len;

            // Wall occlusion: march the ray block-by-block and sum each traversed block's
            // explosion resistance (1.7.10 :93-105).
            float res = 0;
            for (int i = 1; i < len; i++) {
                int ix = (int) Math.floor(cx + nx * i);
                int iy = (int) Math.floor(cy + ny * i);
                int iz = (int) Math.floor(cz + nz * i);
                BlockState step = level.getBlockState(new BlockPos(ix, iy, iz));
                res += step.getBlock().getExplosionResistance(step, level, new BlockPos(ix, iy, iz), null);
            }
            if (res < 1) res = 1;

            float eRads = RADS;
            eRads /= res;
            eRads /= (float) (len * len);
            ContaminationUtil.contaminate(e, HazardType.RADIATION, ContaminationType.CREATIVE, eRads);

            // 1.7.10 RBMKDebrisRadiating.radiate: ein Marshmallow in der Hand roestet naeher als 10 Bloecke
            // mit 1 % pro Durchlauf (Meta 1 = eigener Gegenstand marshmallow_roasted)
            if (e instanceof net.minecraft.world.entity.player.Player p && len < 10) {
                net.minecraft.world.item.ItemStack held = p.getMainHandItem();
                if (held.is(com.hbm_m.item.ModItems.MARSHMALLOW.get()) && p.getRandom().nextInt(100) == 0) {
                    p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                            new net.minecraft.world.item.ItemStack(com.hbm_m.item.ModItems.MARSHMALLOW_ROASTED.get(), held.getCount()));
                }
            }
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
