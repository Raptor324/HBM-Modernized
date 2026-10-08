package com.hbm_m.item.tool;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** 1:1 {@code ItemStructureSingle}: gibt genau eine placeBlockAtCurrentPosition-Zeile fuer den Zielblock aus. */
public class ItemStructureSingle extends ItemStructureTool {

    public ItemStructureSingle(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(Component.literal("Click to print exactly one <placeBlockAtCurrentPosition>").withStyle(ChatFormatting.YELLOW));
        list.add(Component.literal("line with the targted block and metadata").withStyle(ChatFormatting.YELLOW));
    }

    @Override
    protected void doTheThing(ItemStack stack, Level world, int x, int y, int z) {

        BlockPos pos = getAnchor(stack);
        if (pos == null) return;

        int ix = x - pos.getX();
        int iy = y - pos.getY();
        int iz = z - pos.getZ();

        BlockState b = world.getBlockState(new BlockPos(x, y, z));

        String message = "placeBlockAtCurrentPosition(world, " + blockName(b) + ", " + metaOf(b) + ", " + ix + ", " + iy + ", " + iz + ", box);\n";
        System.out.print(message);
        writeToFile(message);
    }
}
