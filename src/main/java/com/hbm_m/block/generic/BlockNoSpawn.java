package com.hbm_m.block.generic;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@link com.hbm.blocks.generic.BlockNoSpawn} ({@code concrete_smooth}, {@code ducrete_smooth}): keine
 * Kreaturen-Spawns darauf, Tooltip {@code tile.nospawn} in Rot.
 */
public class BlockNoSpawn extends Block {

    public BlockNoSpawn(Properties properties) {
        super(properties.isValidSpawn(BlockNoSpawn::never));
    }

    private static boolean never(BlockState state, BlockGetter world, BlockPos pos, EntityType<?> type) {
        return false;
    }

    @Override
    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter world, List<Component> list, TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> list, TooltipFlag flag) {
    *///?}
        list.add(Component.translatable("tile.nospawn").withStyle(ChatFormatting.RED));
    }
}
