package com.hbm_m.block.decorations;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RotatedPillarBlock;

/**
 * 1:1 {@code com.hbm.blocks.generic.BlockPipe}: rein dekoratives Rohr, Ausrichtung nach der angeklickten Seite
 * (Meta 0 = Y, 4 = X, 8 = Z - entspricht {@link RotatedPillarBlock}). Die vier Bauformen (Rohr, Flansch, Rahmen,
 * Vierfachrohr) sind gebackene OBJ-Modelle aus {@code RenderPipe}.
 */
public class DecoPipeBlock extends RotatedPillarBlock {

    public DecoPipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.hbm_m.deco_pipe"));
    }
}
