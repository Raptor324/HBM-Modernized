package com.hbm_m.item.special;

import java.util.List;

import com.hbm_m.item.LoreTooltipItem;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.items.special.ItemRag}: im Wasser liegend wird der Lappen feucht ({@code rag_damp}),
 * Rechtsklick verbraucht einen und legt einen {@code rag_piss} ins Inventar.
 */
public class ItemRag extends LoreTooltipItem {

    public ItemRag(List<Component> lines, Properties properties) {
        super(lines, properties);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entityItem) {

        if (entityItem != null && !entityItem.level().isClientSide) {

            BlockPos pos = new BlockPos(Mth.floor(entityItem.getX()), Mth.floor(entityItem.getY()), Mth.floor(entityItem.getZ()));
            // Original: Material.water - Wasserblock (Fluessigkeit Wasser)
            if (entityItem.level().getBlockState(pos).is(net.minecraft.world.level.block.Blocks.WATER)) {
                ItemStack old = entityItem.getItem();
                entityItem.setItem(new ItemStack(ModItems.RAG_DAMP.get(), old.getCount()));
                return true;
            }
        }
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        stack.shrink(1);
        player.getInventory().add(new ItemStack(ModItems.RAG_PISS.get()));
        return InteractionResultHolder.sidedSuccess(stack, world.isClientSide);
    }
}
