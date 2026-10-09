package com.hbm_m.item.special;

import com.hbm_m.platform.StackNbt;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.ITooltipProvider;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** 1:1 {@code ItemFusionShield}: Blanket des alten Fusionsreaktors, Schaden als long in NBT "damage". */
public class ItemFusionShield extends Item implements ITooltipProvider {

    public long maxDamage;
    public int maxTemp;

    public ItemFusionShield(long maxDamage, int maxTemp, Properties properties) {
        super(properties.stacksTo(1));
        this.maxDamage = maxDamage;
        this.maxTemp = maxTemp;
    }

    public static long getShieldDamage(ItemStack stack) {
        return StackNbt.has(stack) ? StackNbt.read(stack).getLong("damage") : 0;
    }

    public static void setShieldDamage(ItemStack stack, long damage) {
        StackNbt.orCreate(stack).putLong("damage", damage);
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        long damage = getShieldDamage(stack);
        int percent = (int) ((maxDamage - damage) * 100 / maxDamage);
        list.add(Component.literal("Durability: " + (maxDamage - damage) + "/" + maxDamage + " (" + percent + "%)"));
        list.add(Component.literal("Melting point: ").append(Component.literal(maxTemp + "\u00b0C").withStyle(ChatFormatting.RED)));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getShieldDamage(stack) != 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F - (float) ((double) getShieldDamage(stack) / (double) maxDamage) * 13.0F);
    }
}
