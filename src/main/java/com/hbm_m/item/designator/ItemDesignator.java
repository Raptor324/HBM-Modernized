package com.hbm_m.item.designator;

import java.util.List;

import com.hbm_m.api.item.IDesignatorItem;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.sound.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Basic designator: right-click on a block (except launch pad) to set target X/Z.
 * Port from 1.7.10 ItemDesingator.
 */
public class ItemDesignator extends Item implements IDesignatorItem {

    public ItemDesignator(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        if (stack.hasTag() && stack.getTag().contains("xCoord")) {
            tooltip.add(Component.translatable("tooltip.hbm_m.designator.target"));
            tooltip.add(Component.literal("X: " + stack.getTag().getInt("xCoord")).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Z: " + stack.getTag().getInt("zCoord")).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.hbm_m.designator.no_target"));
        }
    }

    @Override
    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        var block = state.getBlock();

        if (block == ModBlocks.LAUNCH_PAD.get() || block == ModBlocks.LAUNCH_PAD_RUSTED.get()) {
            return InteractionResult.PASS;
        }

        ItemStack stack = context.getItemInHand();
        var tag = stack.getOrCreateTag();
        tag.putInt("xCoord", pos.getX());
        tag.putInt("zCoord", pos.getZ());

        if (level.isClientSide()) {
            context.getPlayer().displayClientMessage(Component.translatable("message.hbm_m.designator.position_set"), true);
        }
        level.playSound(context.getPlayer(), context.getPlayer().blockPosition(),
                ModSounds.TOOL_TECH_BLEEP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public boolean isReady(Level level, ItemStack stack, int x, int y, int z) {
        return stack.hasTag() && stack.getTag().contains("xCoord");
    }

    @Override
    public Vec3 getCoords(Level level, ItemStack stack, int x, int y, int z) {
        var tag = stack.getTag();
        if (tag == null || !tag.contains("xCoord")) return Vec3.ZERO;
        return new Vec3(tag.getInt("xCoord"), 0, tag.getInt("zCoord"));
    }
}
