package com.hbm_m.item.missile;

import java.util.List;

import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code ItemLootCrate} (loot_10, loot_15, loot_misc): Rechtsklick legt ein zufaelliges Raketenteil ins Inventar -
 * Rumpf mit 1.0er- bzw. 1.5er-Spitze oder ein sonstiges Teil. Seltene Teile werden per Wiederholungswurf ausgesiebt
 * (ungewoehnlich 1/5, selten 1/10, episch 1/25, legendaer 1/50, seltsam 1/100). Bei vollem Inventar geht der Fund
 * wie im Original verloren.
 */
public class ItemLootCrate extends Item {

    /** 10, 15 oder 0 (sonstige Teile) - siehe {@link MissilePartItems#lootList(int)}. */
    private final int list;

    public ItemLootCrate(Properties props, int list) {
        super(props.stacksTo(1));
        this.list = list;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            List<ItemCustomMissilePart> parts = MissilePartItems.lootList(list);
            if (!parts.isEmpty()) player.getInventory().add(new ItemStack(choose(parts, level.random)));
            player.inventoryMenu.broadcastChanges();
        }

        stack.shrink(1);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static ItemCustomMissilePart choose(List<ItemCustomMissilePart> parts, RandomSource rand) {
        boolean flag = true;
        ItemCustomMissilePart item = null;

        while (flag) {
            item = parts.get(rand.nextInt(parts.size()));

            switch (item.rarity) {
                case COMMON -> flag = false;
                case UNCOMMON -> { if (rand.nextInt(5) == 0) flag = false; }
                case RARE -> { if (rand.nextInt(10) == 0) flag = false; }
                case EPIC -> { if (rand.nextInt(25) == 0) flag = false; }
                case LEGENDARY -> { if (rand.nextInt(50) == 0) flag = false; }
                case SEWS_CLOTHES_AND_SUCKS_HORSE_COCK -> { if (rand.nextInt(100) == 0) flag = false; }
            }
        }

        return item;
    }
}
