package com.hbm_m.item.rbmk;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.machines.MachineRbmkConsoleBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKCraneConsoleBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Direct port of the original 1.7.10 {@code ItemRBMKTool}: a two-step linking tool.
 * <ol>
 *   <li>Right-click an RBMK column -&gt; its position is stored in the tool's own NBT (see
 *   {@link #storeColumnPosition}, called from {@code RBMKColumnBlock#use}).</li>
 *   <li>Right-click an RBMK Console -&gt; the stored position becomes that console's reactor grid
 *   origin (see {@link #linkConsole}, called from {@code MachineRbmkConsoleBlock#use}).</li>
 * </ol>
 * Both block classes already unconditionally intercept right-clicks to open their own GUI, so the
 * tool logic is invoked directly from their {@code use()} methods (same pattern already used there
 * for the screwdriver/lid items) rather than via {@link Item#useOn}, which would never be reached.
 * <p>
 * SCOPE-Vereinfachung: the original also supports a separate {@code rotate()} interaction (0/90/
 * 180/270°) so the 15x15 scan can run in any of 4 directions from the linked point; this port's
 * console always scans a fixed +X/+Z grid from {@code reactorOrigin}, so rotation isn't modeled.
 */
public class RBMKToolItem extends Item {

    private static final String NBT_X = "linkX";
    private static final String NBT_Y = "linkY";
    private static final String NBT_Z = "linkZ";

    public RBMKToolItem(Properties properties) {
        super(properties);
    }

    public static void storeColumnPosition(ItemStack stack, Level level, BlockPos pos, Player player) {
        if (level.isClientSide) return;
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(NBT_X, pos.getX());
        tag.putInt(NBT_Y, pos.getY());
        tag.putInt(NBT_Z, pos.getZ());
        player.displayClientMessage(Component.translatable("msg.hbm_m.rbmk_tool.stored",
                pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.YELLOW), true);
    }

    public static void linkConsole(ItemStack stack, Level level, MachineRbmkConsoleBlockEntity console, Player player) {
        if (level.isClientSide) return;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_X)) {
            player.displayClientMessage(
                    Component.translatable("msg.hbm_m.rbmk_tool.no_position").withStyle(ChatFormatting.RED), true);
            return;
        }

        // Use the linked column's OWN Y, not the console's - the console is very often placed at
        // a different Y than the reactor's column base (different terrain height, a floor below/
        // above, etc.), so scanning at the console's Y silently found zero columns and left the
        // whole grid/screen blank even though "linking" reported success.
        BlockPos linked = new BlockPos(tag.getInt(NBT_X), tag.getInt(NBT_Y), tag.getInt(NBT_Z));

        // Validate before accepting: a stale stored position (e.g. left over on the tool from an
        // earlier test, or a column that has since been broken) previously still reported success
        // and silently left the console's grid empty with no indication why. Surfacing the actual
        // coordinates in both the success and failure message lets a wrong link be diagnosed at a
        // glance instead of just staring at a blank grid.
        if (!(level.getBlockEntity(linked) instanceof com.hbm_m.blockentity.machines.rbmk.RBMKColumnBlockEntity)) {
            player.displayClientMessage(
                    Component.translatable("msg.hbm_m.rbmk_console.linked_invalid",
                            linked.getX(), linked.getY(), linked.getZ()).withStyle(ChatFormatting.RED), true);
            return;
        }

        console.reactorOrigin = linked;
        console.setChanged();
        level.sendBlockUpdated(console.getBlockPos(), console.getBlockState(), console.getBlockState(), 3);
        player.displayClientMessage(
                Component.translatable("msg.hbm_m.rbmk_console.linked",
                        linked.getX(), linked.getY(), linked.getZ()).withStyle(ChatFormatting.GREEN), true);
    }

    /**
     * Right-click a crane console with a stored position -&gt; links the crane to that reactor
     * column (see {@code RBMKCraneConsoleBlock#use}). Matches the original tool's second target
     * type alongside the console; the crane's own facing/scan directions are captured from
     * the linking player's orientation, since - unlike the original block - this port's crane
     * console has no blockstate facing property to read from.
     */
    public static void linkCrane(ItemStack stack, Level level, RBMKCraneConsoleBlockEntity crane, Player player) {
        if (level.isClientSide) return;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_X)) {
            player.displayClientMessage(
                    Component.translatable("msg.hbm_m.rbmk_tool.no_position").withStyle(ChatFormatting.RED), true);
            return;
        }

        BlockPos linked = new BlockPos(tag.getInt(NBT_X), tag.getInt(NBT_Y), tag.getInt(NBT_Z));
        Direction facing = player.getDirection().getOpposite();
        crane.setTarget(level, linked, facing);
        player.displayClientMessage(
                Component.translatable("msg.hbm_m.rbmk_crane.linked").withStyle(ChatFormatting.GREEN), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(NBT_X)) {
            tooltip.add(Component.translatable("tooltip.hbm_m.rbmk_tool.stored",
                    tag.getInt(NBT_X), tag.getInt(NBT_Y), tag.getInt(NBT_Z)).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.hbm_m.rbmk_tool.empty").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
