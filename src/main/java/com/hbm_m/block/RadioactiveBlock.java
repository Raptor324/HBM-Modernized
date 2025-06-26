package com.hbm_m.block;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.List;

public class RadioactiveBlock extends Block {

    private final float radiationLevel;

    public RadioactiveBlock(Properties properties, float radiationLevel) {
        super(properties);
        this.radiationLevel = radiationLevel;
    }

    public float getRadiationLevel() {
        return radiationLevel;
    }

    @Override
    public void appendHoverText(@javax.annotation.Nonnull ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.hbm_m.radioactive").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal(radiationLevel + "RAD/s").withStyle(ChatFormatting.GREEN));
    }
}