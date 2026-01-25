package com.hbm_m.block.custom.nature;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.Explosion;

import javax.annotation.Nullable;
import java.util.List;

public class DepthOreBlock extends Block {

    public DepthOreBlock(Properties properties) {
        super(properties
                .strength(29.0F, 29.0F)
                .requiresCorrectToolForDrops()
        );
    }
    @Override
    public void appendHoverText(ItemStack stack,
                                @Nullable net.minecraft.world.level.BlockGetter level,
                                List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.hbm_m.depthstone.line1")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.hbm_m.depthstone.line4")
                .withStyle(ChatFormatting.YELLOW));
    }
    // Не ломается поршнями
    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    // Запретить ломать блок инструментом (игроком)
    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return 0.0F; // Нельзя сломать вообще
    }

    // Блок ломается только взрывом
    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        // Если хочешь, чтобы выпадал предмет:
        popResource(level, pos, new ItemStack(this));
        level.removeBlock(pos, false);
    }
}