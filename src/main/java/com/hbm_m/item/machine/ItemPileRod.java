package com.hbm_m.item.machine;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.ITooltipProvider;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** 1:1 {@code com.hbm.items.machine.ItemPileRod} (Chicago-Pile-Staebe, LEGACY): zwei $-getrennte Tooltip-Bloecke. */
public class ItemPileRod extends Item implements ITooltipProvider {

    public ItemPileRod(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        for (String loc : Language.getInstance().getOrDefault("desc.item.pileRod").split("\\$")) {
            list.add(Component.literal(loc));
        }
        for (String loc : Language.getInstance().getOrDefault(this.getDescriptionId() + ".desc").split("\\$")) {
            list.add(Component.literal(loc));
        }
    }
}
