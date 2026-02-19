package com.hbm_m.util;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Minimal “explosion vs block” helper used by {@link CraterGenerator}.
 *
 * <p>In the original mod this logic was more elaborate; after merges this class
 * went missing. For now we keep a lightweight heuristic so crater generation
 * can compile and behave reasonably.</p>
 */
public final class BlockExplosionDefense {

    private BlockExplosionDefense() {}

    public static final class ExplosionDefenseResult {
        public final boolean shouldBreak;
        public final float blastPower;
        public final float resistance;

        public ExplosionDefenseResult(boolean shouldBreak, float blastPower, float resistance) {
            this.shouldBreak = shouldBreak;
            this.blastPower = blastPower;
            this.resistance = resistance;
        }
    }

    /**
     * Computes whether a block should be removed by a crater/explosion pass.
     *
     * @param level Level
     * @param pos Block being evaluated
     * @param center Explosion/crater center (for distance falloff)
     * @param radius Horizontal radius used by crater generator
     * @param random RandomSource
     */
    public static ExplosionDefenseResult calculateExplosionDamage(
            Level level,
            BlockPos pos,
            BlockPos center,
            float radius,
            RandomSource random
    ) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return new ExplosionDefenseResult(false, 0F, 0F);
        }

        // Unbreakable blocks (bedrock-like) typically report < 0 destroy speed.
        if (state.getDestroySpeed(level, pos) < 0F) {
            return new ExplosionDefenseResult(false, 0F, Float.POSITIVE_INFINITY);
        }

        float dist = (float) Math.sqrt(pos.distSqr(center));
        float falloff = radius <= 0F ? 0F : Math.max(0F, 1F - (dist / radius));

        // Base “blast power” with a bit of randomness to avoid perfect spheres.
        float blastPower = (20F + random.nextFloat() * 10F) * falloff;

        // Vanilla-like resistance metric; capped to keep values sane.
        float resistance = Math.min(state.getExplosionResistance(level, pos, null), 100F);

        boolean shouldBreak = blastPower > resistance;
        return new ExplosionDefenseResult(shouldBreak, blastPower, resistance);
    }
}

