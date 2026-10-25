package com.hbm_m.item.special;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.item.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** 1:1 {@code com.hbm.items.special.ItemDemonCore}: faellt der offene Kern zu Boden, klappt er zu und der Schraubenzieher faellt heraus. */
public class ItemDemonCore extends Item implements ITooltipProvider {

    public ItemDemonCore(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entityItem) {
        if (!entityItem.level().isClientSide && entityItem.onGround()) {
            entityItem.setItem(new ItemStack(ModItems.DEMON_CORE_CLOSED.get()));
            entityItem.level().addFreshEntity(new ItemEntity(entityItem.level(), entityItem.getX(), entityItem.getY(), entityItem.getZ(), new ItemStack(ModItems.SCREWDRIVER.get())));
            return true;
        }
        return false;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("[").append(Component.translatable("trait.drop")).append("]").withStyle(ChatFormatting.RED));
    }
}
