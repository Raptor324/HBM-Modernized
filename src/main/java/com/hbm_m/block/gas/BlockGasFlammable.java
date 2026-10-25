package com.hbm_m.block.gas;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1-Port von {@code BlockGasFlammable} (1.7.10): brennbares Gas. Liegt eine Zuendquelle daneben
 * oder laeuft eine brennende Kreatur hinein, faengt es Feuer. Ueber Luft loest es sich mit 1/20
 * auf, ansonsten steigt es auf und verteilt sich.
 */
public class BlockGasFlammable extends BlockGasBase {

    public BlockGasFlammable() {
        super(0.8F, 0.8F, 0.2F);
    }

    protected static boolean isFireSource(BlockState state) {
        return state.is(Blocks.FIRE)
                || state.is(Blocks.SOUL_FIRE)
                || state.is(Blocks.LAVA)
                || state.is(Blocks.TORCH)
                || state.is(Blocks.WALL_TORCH)
                || state.is(Blocks.SOUL_TORCH)
                || state.is(Blocks.SOUL_WALL_TORCH)
                || state.is(Blocks.JACK_O_LANTERN)
                || state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.SOUL_CAMPFIRE);
    }

    public static boolean isHoldingFireSource(Entity entity) {
        if (entity instanceof LivingEntity living) {
            return isFireSourceItem(living.getMainHandItem()) || isFireSourceItem(living.getOffhandItem());
        }
        return false;
    }

    public static boolean isFireSourceItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item == Items.TORCH
                || item == Items.SOUL_TORCH
                || item == Items.LAVA_BUCKET
                || item == Items.FLINT_AND_STEEL
                || item == Items.FIRE_CHARGE
                || item == Items.CAMPFIRE
                || item == Items.SOUL_CAMPFIRE
                || stack.is(ItemTags.CANDLES);
    }

    @Override
    public boolean isFlammable(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, Direction direction) {
        return true;
    }

    protected void combust(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
    }

    @Override
    protected boolean onGasTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        for (Direction dir : Direction.values()) {
            if (isFireSource(level.getBlockState(pos.relative(dir)))) {
                combust(level, pos);
                return true;
            }
        }

        if (random.nextInt(20) == 0 && level.getBlockState(pos.below()).isAir()) {
            level.removeBlock(pos, false);
            return true;
        }

        return false;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide() && (entity.isOnFire() || isHoldingFireSource(entity)) && level instanceof ServerLevel serverLevel) {
            combust(serverLevel, pos);
            return;
        }
        super.entityInside(state, level, pos, entity);
    }

    @Override
    protected void affect(LivingEntity living) {
    }

    @Override
    public Direction getFirstDirection(Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) return randomVertical(random);
        return randomHorizontal(random);
    }

    @Override
    public Direction getSecondDirection(Level level, BlockPos pos, RandomSource random) {
        return randomHorizontal(random);
    }

    /** Original: zufaellige Verzoegerung 16..20 Ticks vor der Selbstentzuendung. */
    @Override
    public int getDelay(Level level, RandomSource random) {
        return random.nextInt(5) + 16;
    }
}
