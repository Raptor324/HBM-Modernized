package com.hbm_m.multiblock;

import com.hbm_m.lib.RefStrings;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * BlockItem для дверей с поддержкой смены скина.
 * Добавляет тултип: «Используй отвёртку, чтобы сменить скин!»
 */
public class DoorBlockItem extends MultiblockBlockItem {

    public DoorBlockItem(Block pBlock, Properties pProperties) {
        super(pBlock, pProperties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip." + RefStrings.MODID + ".door_skin").withStyle(ChatFormatting.GRAY));
    }
}
