package com.hbm_m.block.generic;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

/**
 * Порт {@link com.hbm.blocks.generic.BlockSellafieldOre} — sellafite-руда с оверлеем и кастомным дропом.
 */
public class BlockSellafieldOre extends BlockSellafieldSlaked {

    private final Supplier<Item> dropItem;
    private final int minXp;
    private final int maxXp;

    public BlockSellafieldOre(Properties properties, Supplier<Item> dropItem) {
        this(properties, dropItem, 0, 0);
    }

    public BlockSellafieldOre(Properties properties, Supplier<Item> dropItem, int minXp, int maxXp) {
        super(properties);
        this.dropItem = dropItem;
        this.minXp = minXp;
        this.maxXp = maxXp;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return Collections.singletonList(new ItemStack(dropItem.get()));
    }

    @Override
    public int getExpDrop(BlockState state, LevelReader level, RandomSource random, BlockPos pos, int fortune, int silkTouch) {
        if (silkTouch > 0 || minXp <= 0) {
            return 0;
        }
        return random.nextInt(maxXp - minXp + 1) + minXp;
    }

    public static BlockSellafieldOre diamondOre(Properties properties) {
        return new BlockSellafieldOre(properties, () -> Items.DIAMOND, 3, 7);
    }

    public static BlockSellafieldOre emeraldOre(Properties properties) {
        return new BlockSellafieldOre(properties, () -> Items.EMERALD, 3, 7);
    }

    public static BlockSellafieldOre radgemOre(Properties properties) {
        return new BlockSellafieldOre(properties, () -> Items.DIAMOND, 3, 7);
    }

    /** Uranium / schrabidium sellafite ore — дроп самого блока (как в 1.7.10 без silk). */
    public static BlockSellafieldOre sellafiteOre(Properties properties) {
        return new BlockSellafieldOre(properties, () -> Items.AIR) {
            @Override
            public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
                return Collections.singletonList(new ItemStack(this));
            }
        };
    }
}
