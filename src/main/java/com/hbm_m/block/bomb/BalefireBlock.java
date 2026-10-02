package com.hbm_m.block.bomb;

import com.hbm_m.effect.ModEffects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code Balefire}: gruenes Hoellenfeuer. Brennt nie von selbst aus (ab Alter 15 ruht es), greift aggressiver
 * auf Brennbares ueber als normales Feuer und verbreitet sich im 7x6x7-Umkreis; das Alter dunkelt die Farbe ab.
 * Wer hineinlaeuft, brennt 10 s und bekommt 5 s Strahlung X.
 */
public class BalefireBlock extends FireBlock {

    public BalefireBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        if (!world.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK)) return;

        if (!state.canSurvive(world, pos)) world.removeBlock(pos, false);

        int meta = state.getValue(AGE);
        if (meta < 15) world.scheduleTick(pos, this, 30 + rand.nextInt(10));

        if (!canNeighborBurn(world, pos) && !world.getBlockState(pos.below()).isFaceSturdy(world, pos.below(), Direction.UP)) {
            world.removeBlock(pos, false);
            return;
        }
        if (meta >= 15) return;

        tryCatchFire(world, pos.east(), 500, rand, meta, Direction.WEST);
        tryCatchFire(world, pos.west(), 500, rand, meta, Direction.EAST);
        tryCatchFire(world, pos.below(), 300, rand, meta, Direction.UP);
        tryCatchFire(world, pos.above(), 300, rand, meta, Direction.DOWN);
        tryCatchFire(world, pos.north(), 500, rand, meta, Direction.SOUTH);
        tryCatchFire(world, pos.south(), 500, rand, meta, Direction.NORTH);

        int h = 3;
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        for (int ix = x - h; ix <= x + h; ++ix) {
            for (int iz = z - h; iz <= z + h; ++iz) {
                for (int iy = y - 1; iy <= y + 4; ++iy) {
                    if (ix == x && iy == y && iz == z) continue;
                    BlockPos p = new BlockPos(ix, iy, iz);
                    int fireLimit = 100;
                    if (iy > y + 1) fireLimit += (iy - (y + 1)) * 100;

                    BlockState there = world.getBlockState(p);
                    if (there.is(this) && there.getValue(AGE) > meta + 1) {
                        world.setBlock(p, withAge(world, p, meta + 1), 3);
                        continue;
                    }

                    int neighborFireChance = getChanceOfNeighborsEncouragingFire(world, p);
                    if (neighborFireChance > 0) {
                        int adjusted = (neighborFireChance + 40 + world.getDifficulty().getId() * 7) / (meta + 30);
                        if (adjusted > 0 && rand.nextInt(fireLimit) <= adjusted) {
                            world.setBlock(p, withAge(world, p, meta + 1), 3);
                        }
                    }
                }
            }
        }
    }

    private BlockState withAge(BlockGetter world, BlockPos pos, int age) {
        return getStateForPlacement(world, pos).setValue(AGE, Math.min(15, age));
    }

    private void tryCatchFire(Level world, BlockPos pos, int chance, RandomSource rand, int meta, Direction face) {
        BlockState state = world.getBlockState(pos);
        int flammability = state.getFlammability(world, pos, face);
        if (rand.nextInt(chance) < flammability) {
            boolean tnt = state.is(Blocks.TNT);
            world.setBlock(pos, withAge(world, pos, meta + 1), 3);
            if (tnt) TntBlock.explode(world, pos);
        }
    }

    private boolean canNeighborBurn(BlockGetter world, BlockPos pos) {
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            if (world.getBlockState(n).isFlammable(world, n, d.getOpposite())) return true;
        }
        return false;
    }

    private int getChanceOfNeighborsEncouragingFire(Level world, BlockPos pos) {
        if (!world.isEmptyBlock(pos)) return 0;
        int spread = 0;
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            spread = Math.max(spread, world.getBlockState(n).getFireSpreadSpeed(world, n, d.getOpposite()));
        }
        return spread;
    }

    @Override
    public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
        entity.setSecondsOnFire(10);
        if (entity instanceof LivingEntity living) living.addEffect(new MobEffectInstance(ModEffects.RADIATION.get(), 5 * 20, 9));
    }

    /** {@code colorMultiplier}: Helligkeit 1 - Alter/30. */
    public static int color(BlockState state) {
        return java.awt.Color.HSBtoRGB(0F, 0F, 1F - state.getValue(AGE) / 30F);
    }
}
