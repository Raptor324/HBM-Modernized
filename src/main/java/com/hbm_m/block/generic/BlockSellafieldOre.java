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

    //? if < 1.21.1 {
    @Override
    public int getExpDrop(BlockState state, LevelReader level, RandomSource random, BlockPos pos, int fortune, int silkTouch) {
        if (silkTouch > 0 || minXp <= 0) {
            return 0;
        }
        return random.nextInt(maxXp - minXp + 1) + minXp;
    }
    //?} else {
    /*// 1.21.1 (neoforge): IBlockExtension.getExpDrop(BlockState, LevelAccessor, BlockPos,
    // BlockEntity, Entity, ItemStack) — реальный хук, вызываемый из BlockBehaviour.spawnAfterBreak
    // (см. DropExperienceBlock в NeoForge 21.1). Шёлк проверяем по тулзе, XP 3-7 как в оригинале.
    @Override
    public int getExpDrop(BlockState state, net.minecraft.world.level.LevelAccessor level, BlockPos pos,
            net.minecraft.world.level.block.entity.BlockEntity blockEntity,
            net.minecraft.world.entity.Entity breaker, ItemStack tool) {
        if (minXp <= 0 || getSilkTouchLevel(tool, level) > 0) {
            return 0;
        }
        return level.getRandom().nextInt(maxXp - minXp + 1) + minXp;
    }
    *///?}

    public static BlockSellafieldOre diamondOre(Properties properties) {
        return new BlockSellafieldOre(properties, () -> Items.DIAMOND, 3, 7);
    }

    public static BlockSellafieldOre emeraldOre(Properties properties) {
        return new BlockSellafieldOre(properties, () -> Items.EMERALD, 3, 7);
    }

    public static BlockSellafieldOre radgemOre(Properties properties) {
        return new BlockSellafieldOre(properties, () -> com.hbm_m.item.ModItems.GEM_RAD.get(), 3, 7);
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

    /** Silk-touch check: 1.20.1 takes the raw enchantment, 1.21.1 a Holder. */
    private static int getSilkTouchLevel(net.minecraft.world.item.ItemStack tool, net.minecraft.world.level.LevelAccessor level) {
        return com.hbm_m.platform.ItemHooks.getEnchantmentLevel(tool, level, "minecraft:silk_touch");
    }
}

