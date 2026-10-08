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
import net.minecraft.world.level.block.state.BlockState;

/** 1:1 {@code ItemStructureSolid}: fillWithMetadataBlocks/fillWithBlocks-Zeile fuer die Auswahl (letzter Block zaehlt). */
public class ItemStructureSolid extends ItemStructureTool {

    public ItemStructureSolid(Properties properties) {
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
        list.add(Component.literal("Click to print a <fillWithMetadataBlocks> or <fillWithBlocks>").withStyle(ChatFormatting.YELLOW));
        list.add(Component.literal("line with wildcard block and metadata.").withStyle(ChatFormatting.YELLOW));
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
        // Annahme: der zuletzt gewaehlte Block steht fuer alle
        BlockState b = world.getBlockState(new BlockPos(x, y, z));

        String line;
        if (!b.getValues().isEmpty())
            line = "fillWithMetadataBlocks(world, box, " + minX + ", " + minY + ", " + minZ + ", " + maxX + ", " + maxY + ", " + maxZ + ", " + blockName(b) + ", " + metaOf(b) + ");\n";
        else
            line = "fillWithBlocks(world, box, " + minX + ", " + minY + ", " + minZ + ", " + maxX + ", " + maxY + ", " + maxZ + ", " + blockName(b) + ");\n";

        System.out.print(line);
        writeToFile(line);
    }
}
