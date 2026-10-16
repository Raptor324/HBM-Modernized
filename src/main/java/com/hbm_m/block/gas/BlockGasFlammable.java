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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Горючий газ: вспыхивает от источников огня рядом или от горящей сущности.
 * Порт {@link com.hbm.blocks.gas.BlockGasFlammable} (1.7.10).
 */
public class BlockGasFlammable extends BlockGasBase {

    public BlockGasFlammable() {
        super();
    }

    @Override
    protected void affect(LivingEntity living) {
        // Урона дыханием не наносит
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide()) {
            // Поджиг от горящей сущности или удержания факела/огня в руках
            if (entity.isOnFire() || isHoldingFireSource(entity)) {
                combust(level, pos);
                return;
            }
        }
        super.entityInside(state, level, pos, entity);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide() && isFireSource(level.getBlockState(neighborPos))) {
            level.scheduleTick(pos, this, 2);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Проверяем соседей на источник огня
        for (Direction dir : Direction.values()) {
            if (isFireSource(level.getBlockState(pos.relative(dir)))) {
                combust(level, pos);
                return;
            }
        }

        // 1/20 — рассеивается, если под газом воздух
        if (random.nextInt(20) == 0 && level.getBlockState(pos.below()).isAir()) {
            level.removeBlock(pos, false);
            return;
        }

        super.tick(state, level, pos, random);
    }

    @Override
    public Direction getFirstDirection(Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            return random.nextBoolean() ? Direction.UP : Direction.DOWN;
        }
        return randomHorizontal(random);
    }

    @Override
    public Direction getSecondDirection(Level level, BlockPos pos, RandomSource random) {
        return randomHorizontal(random);
    }

    @Override
    public int getDelay(Level level, RandomSource random) {
        return random.nextInt(5) + 16;
    }

    protected void combust(Level level, BlockPos pos) {
        level.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
    }

    public static boolean isFireSource(BlockState state) {
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
}