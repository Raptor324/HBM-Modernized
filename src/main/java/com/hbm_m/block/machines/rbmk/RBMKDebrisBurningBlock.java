package com.hbm_m.block.machines.rbmk;

import com.hbm_m.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 port of {@code RBMKDebrisBurning}: the glowing rubble left where a reactor column burned
 * through.
 *
 * <p>It is not decoration - it keeps working after the meltdown. Every tick it puffs flame, has a
 * chance to push meltdown gas into an adjacent air block, and eventually burns itself out into
 * plain rubble. Foam and boron sand next to it make that burn-out ten times more likely, which is
 * the original's intended way of fighting a meltdown site rather than waiting it out.</p>
 */
public class RBMKDebrisBurningBlock extends Block {

    public RBMKDebrisBurningBlock(Properties props) {
        super(props);
    }

    /**
     * CE's {@code RBMKDebrisBurning.tickRate} is 100-120 ticks, not 20-40. The port ticked five
     * times as fast, so with the same 1-in-100 burn-out roll the fire went out roughly five times
     * sooner than intended.
     */
    private static int nextDelay(RandomSource random) {
        return 100 + random.nextInt(20);
    }

    /** Range and strength of the heat field around burning rubble (CE: {@code radiate(.., 32, 0, 0, 50)}). */
    private static final double FIRE_RANGE = 32D;
    private static final float FIRE_STRENGTH = 50F;

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) level.scheduleTick(pos, this, nextDelay(level.random));
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Burning rubble is genuinely hot: CE bathes a 32-block radius in a fire field. The port
        // only drew flame particles, so you could stand in the middle of a burning crater unharmed.
        scorch(level, pos, FIRE_RANGE, FIRE_STRENGTH);

        if (random.nextInt(5) == 0) {
            spawnFlame(level, pos, random);
            level.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS,
                    1.0F + random.nextFloat(), random.nextFloat() * 0.7F + 0.3F);
        }

        // Meltdown gas creeps outward into open air.
        Direction dir = Direction.values()[random.nextInt(6)];
        BlockPos side = pos.relative(dir);
        BlockState neighbour = level.getBlockState(side);

        if (random.nextInt(10) == 0 && neighbour.isAir()) {
            level.setBlockAndUpdate(side, ModBlocks.GAS_MELTDOWN.get().defaultBlockState());
        }

        // Foam smothers the fire, boron stops the fission - both cut the burn-out timer massively.
        boolean suppressed = isSuppressant(level.getBlockState(side));
        int chance = suppressed ? 10 : 100;

        if (random.nextInt(chance) == 0) {
            level.setBlockAndUpdate(pos, ModBlocks.RBMK_DEBRIS.get().defaultBlockState());
        } else {
            level.scheduleTick(pos, this, nextDelay(random));
        }
    }

    /** CE checks for boron sand and nothing else. */
    static boolean isSuppressant(BlockState state) {
        return state.is(ModBlocks.SAND_BORON.get());
    }

    /**
     * CE's {@code ContaminationUtil.radiate} fire term: damage falls off with the square of the
     * distance and the entity is set alight for five seconds once it is above the noise floor.
     */
    static void scorch(ServerLevel level, BlockPos pos, double range, float strength) {
        double cx = pos.getX() + 0.5, cy = pos.getY() + 0.5, cz = pos.getZ() + 0.5;
        net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(pos).inflate(range);

        for (net.minecraft.world.entity.LivingEntity e :
                level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, area)) {
            if (e.fireImmune()) continue;

            double dx = e.getX() - cx;
            double dy = (e.getY() + e.getEyeHeight()) - cy;
            double dz = e.getZ() - cz;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len > range) continue;

            double dmgLen = Math.max(len, range * 0.05D);
            float damage = (float) (strength / (dmgLen * dmgLen));
            if (damage <= 0.025F) continue;

            e.hurt(level.damageSources().inFire(), damage);
            e.setSecondsOnFire(5);
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
