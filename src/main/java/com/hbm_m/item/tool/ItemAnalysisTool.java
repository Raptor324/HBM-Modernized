package com.hbm_m.item.tool;

import java.util.List;

import com.hbm_m.interfaces.IAnalyzable;
import com.hbm_m.interfaces.IMultiblockPart;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** 1:1 {@code com.hbm.items.tool.ItemAnalysisTool}: schreibt die Debug-Zeilen eines {@link IAnalyzable}-Blocks in den Chat. */
public class ItemAnalysisTool extends Item {

    public ItemAnalysisTool(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Block b = world.getBlockState(pos).getBlock();

        if (world.getBlockEntity(pos) instanceof IMultiblockPart part && part.getControllerPos() != null) {
            pos = part.getControllerPos();
        }

        if (b instanceof IAnalyzable analyzable) {
            List<String> debug = analyzable.getDebugInfo(world, pos);

            if (debug != null && !world.isClientSide && ctx.getPlayer() != null) {
                for (String line : debug) {
                    ctx.getPlayer().sendSystemMessage(Component.literal(line).withStyle(ChatFormatting.YELLOW));
                }
            }
            return InteractionResult.sidedSuccess(world.isClientSide);
        }

        return InteractionResult.PASS;
    }
}
