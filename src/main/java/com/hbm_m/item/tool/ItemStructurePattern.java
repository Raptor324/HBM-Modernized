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

/** 1:1 {@code ItemStructurePattern}: placeBlockAtCurrentPosition-Zeilen fuer alle nicht leeren Bloecke der Auswahl. */
public class ItemStructurePattern extends ItemStructureTool {

    public ItemStructurePattern(Properties properties) {
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
        list.add(Component.literal("Click to print all <placeBlockAtCurrentPosition>").withStyle(ChatFormatting.YELLOW));
        list.add(Component.literal("lines for the current selection with blocks and metadata.").withStyle(ChatFormatting.YELLOW));
    }

    @Override
    protected boolean dualUse() {
        return true;
    }

    @Override
    protected void doTheThing(ItemStack stack, Level world, int x, int y, int z) {

        BlockPos pos = getAnchor(stack);
        if (pos == null) return;

        StringBuilder message = new StringBuilder();
        int savedX = PlatformHooks.getInt(stack, "x");
        int savedY = PlatformHooks.getInt(stack, "y");
        int savedZ = PlatformHooks.getInt(stack, "z");

        int minX = Math.min(savedX, x) - pos.getX();
        int minY = Math.min(savedY, y) - pos.getY();
        int minZ = Math.min(savedZ, z) - pos.getZ();
        int maxX = Math.max(savedX, x) - pos.getX();
        int maxY = Math.max(savedY, y) - pos.getY();
        int maxZ = Math.max(savedZ, z) - pos.getZ();

        for (int ix = minX; ix <= maxX; ix++) {
            for (int iy = minY; iy <= maxY; iy++) {
                for (int iz = minZ; iz <= maxZ; iz++) {

                    BlockState b = world.getBlockState(new BlockPos(ix + pos.getX(), iy + pos.getY(), iz + pos.getZ()));
                    if (b.isAir()) continue;

                    message.append("placeBlockAtCurrentPosition(world, ").append(blockName(b)).append(", ").append(metaOf(b)).append(", ")
                            .append(ix).append(", ").append(iy).append(", ").append(iz).append(", box);\n");
                }
            }
        }

        System.out.print(message);
        writeToFile(message.toString());
    }
}
