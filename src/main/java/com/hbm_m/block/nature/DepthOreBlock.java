package com.hbm_m.block.nature;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.Explosion;

//? if neoforge {
/*import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.tooltip.TooltipType;
*///?}

//? if forge {
/*import net.minecraft.world.item.TooltipFlag;
*///?}

//? if fabric {
import net.minecraft.world.item.TooltipFlag;
//?}

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DepthOreBlock extends Block {

    public DepthOreBlock(Properties properties) {
        super(properties
                .strength(29.0F, 29.0F)
                .requiresCorrectToolForDrops()
                .pushReaction(PushReaction.BLOCK)
        );
    }

    //? if forge {
    /*@Override
    public void appendHoverText(ItemStack stack,
                                @Nullable BlockGetter level,
                                List<Component> tooltip,
                                TooltipFlag flag) {
        addDepthOreTooltip(tooltip);
    }
    *///?}

    //? if neoforge {
    /*@Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipType type) {
        addDepthOreTooltip(tooltip);
    }
    *///?}

    //? if fabric {
    @Override
    public void appendHoverText(ItemStack stack,
                                @Nullable net.minecraft.world.level.BlockGetter level,
                                List<Component> tooltip,
                                TooltipFlag flag) {
        addDepthOreTooltip(tooltip);
    }
    //?}

    private static void addDepthOreTooltip(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.hbm_m.depthstone.line1")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.hbm_m.depthstone.line4")
                .withStyle(ChatFormatting.YELLOW));
    }

    // Не ломается поршнями — через Properties.pushReaction (совместимо с 1.20–1.21+)

    // Запретить ломать блок инструментом (игроком)
    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return 0.0F;
    }

    //? if forge {
    /*@Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        explodeDropAndRemove(level, pos);
    }
    *///?}

    //? if neoforge {
    /*@Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        explodeDropAndRemove(level, pos);
    }
    *///?}

    //? if fabric {
    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        explodeDropAndRemove(level, pos);
    }
    //?}

    private void explodeDropAndRemove(Level level, BlockPos pos) {
        popResource(level, pos, new ItemStack(this));
        level.removeBlock(pos, false);
    }
}
