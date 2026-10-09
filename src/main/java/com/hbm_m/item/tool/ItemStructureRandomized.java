package com.hbm_m.item.tool;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.platform.PlatformHooks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** 1:1 {@code ItemStructureRandomized}: fillWithRandomizedBlocks-Zeile mit Platzhalter fuer den Blockselektor. */
public class ItemStructureRandomized extends ItemStructureTool {

    public ItemStructureRandomized(Properties properties) {
        super(properties);
    }

    @Override
    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> list, TooltipFlag flag) {
        Level level = com.hbm_m.platform.PlatformHooks.tooltipLevel(hbmTooltipCtx);
    *///?}
        //? if < 1.21.1 {
        super.appendHoverText(stack, level, list, flag);
        //?} else {
        /*super.appendHoverText(stack, hbmTooltipCtx, list, flag);
        *///?}
        list.add(Component.literal("Click to print a <fillWithRandomizedBlocks>").withStyle(ChatFormatting.YELLOW));
        list.add(Component.literal("line with block selector.").withStyle(ChatFormatting.YELLOW));
    }

    @Override
    protected boolean dualUse() {
        return true;
    }

    @Override
    protected void doTheThing(ItemStack stack, Level world, int x, int y, int z) {

        BlockPos pos = getAnchor(stack);
        if (pos == null) return;

        int savedX = PlatformHooks.getInt(stack, "x");
        int savedY = PlatformHooks.getInt(stack, "y");
        int savedZ = PlatformHooks.getInt(stack, "z");

        int minX = Math.min(savedX, x) - pos.getX();
        int minY = Math.min(savedY, y) - pos.getY();
        int minZ = Math.min(savedZ, z) - pos.getZ();
        int maxX = Math.max(savedX, x) - pos.getX();
        int maxY = Math.max(savedY, y) - pos.getY();
        int maxZ = Math.max(savedZ, z) - pos.getZ();

        String message = "fillWithRandomizedBlocks(world, box, " + minX + ", " + minY + ", " + minZ + ", " + maxX + ", " + maxY + ", " + maxZ + ", rand, <block-selector>);\n";
        System.out.print(message);
        writeToFile(message);
    }
}
