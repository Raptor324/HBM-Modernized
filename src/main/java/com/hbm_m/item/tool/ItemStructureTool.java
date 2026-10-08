package com.hbm_m.item.tool;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.config.ConfigPaths;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * 1:1 {@code ItemStructureTool}: Basis der Strukturwerkzeuge (structure_single/solid/pattern/randomized/randomly).
 * Klick auf einen {@code structure_anchor} setzt den Anker; danach wird relativ dazu Quelltext fuer die alten
 * Strukturklassen ausgegeben (Konsole und - im Debugmodus - {@code structureOutput.txt} im Konfigordner).
 */
public abstract class ItemStructureTool extends Item implements ILookOverlay {

    private final File file = ConfigPaths.configRoot().resolve("structureOutput.txt").toFile();
    private FileWriter writer;

    public ItemStructureTool(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** Original {@code GeneralConfig.enableDebugMode}. */
    protected static boolean debugMode() {
        return ModClothConfig.get().enableDebugLogging;
    }

    public void writeToFile(String message) {
        if (!debugMode())
            return;

        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            if (writer == null) writer = new FileWriter(file, true);

            writer.write(message);
            writer.flush();
        } catch (IOException e) {
            System.out.print("ItemStructureWand encountered an IOException!");
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        BlockPos anchor = getAnchor(stack);

        if (anchor == null)
            list.add(Component.literal("No anchor set! Right click an anchor to get started.").withStyle(ChatFormatting.RED));

        if (debugMode())
            list.add(Component.literal("Will write to \"structureOutput.txt\" in hbmConfig.").withStyle(ChatFormatting.GREEN));
    }

    @Nullable
    public static BlockPos getAnchor(ItemStack stack) {
        if (!PlatformHooks.hasItemTag(stack)) {
            return null;
        }
        return new BlockPos(PlatformHooks.getInt(stack, "anchorX"), PlatformHooks.getInt(stack, "anchorY"), PlatformHooks.getInt(stack, "anchorZ"));
    }

    public static void setAnchor(ItemStack stack, int x, int y, int z) {
        PlatformHooks.putInt(stack, "anchorX", x);
        PlatformHooks.putInt(stack, "anchorY", y);
        PlatformHooks.putInt(stack, "anchorZ", z);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level world = ctx.getLevel();
        ItemStack stack = ctx.getItemInHand();
        BlockPos pos = ctx.getClickedPos();
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        if (world.getBlockState(pos).is(ModBlocks.STRUCTURE_ANCHOR.get())) {
            setAnchor(stack, x, y, z);
            return InteractionResult.sidedSuccess(world.isClientSide);
        }

        if (getAnchor(stack) == null) {
            return InteractionResult.PASS;
        }

        if (!this.dualUse() && world.isClientSide) {
            this.doTheThing(stack, world, x, y, z);
        } else {

            if (!PlatformHooks.contains(stack, "x")) {
                PlatformHooks.putInt(stack, "x", x);
                PlatformHooks.putInt(stack, "y", y);
                PlatformHooks.putInt(stack, "z", z);
            } else {
                if (world.isClientSide)
                    this.doTheThing(stack, world, x, y, z);
                PlatformHooks.remove(stack, "x");
                PlatformHooks.remove(stack, "y");
                PlatformHooks.remove(stack, "z");
            }
        }

        return InteractionResult.sidedSuccess(world.isClientSide);
    }

    protected boolean dualUse() {
        return false;
    }

    protected abstract void doTheThing(ItemStack stack, Level world, int x, int y, int z);

    /** Ersatz fuer {@code getUnlocalizedName()} eines Blocks. */
    protected static String blockName(BlockState state) {
        return state.getBlock().getDescriptionId();
    }

    /** Ersatz fuer die 1.7.10-Metadaten: Eigenschaften des Blockzustands, ohne Eigenschaften "0". */
    protected static String metaOf(BlockState state) {
        if (state.getValues().isEmpty()) return "0";
        StringBuilder sb = new StringBuilder("\"");
        boolean first = true;
        for (Map.Entry<Property<?>, Comparable<?>> e : state.getValues().entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append(e.getKey().getName()).append('=').append(e.getValue());
        }
        return sb.append('"').toString();
    }

    @Override
    public void printHook(GuiGraphics guiGraphics, Level world, BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        ItemStack stack = mc.player.getMainHandItem();
        List<Component> text = new ArrayList<>();

        BlockPos anchor = getAnchor(stack);

        if (anchor == null) {
            text.add(Component.literal("No Anchor").withStyle(ChatFormatting.RED));
        } else {

            int dX = pos.getX() - anchor.getX();
            int dY = pos.getY() - anchor.getY();
            int dZ = pos.getZ() - anchor.getZ();
            text.add(Component.literal("Position: " + dX + " / " + dY + " / " + dZ).withStyle(ChatFormatting.YELLOW));

            if (this.dualUse() && PlatformHooks.contains(stack, "x")) {
                int sX = Math.abs(pos.getX() - PlatformHooks.getInt(stack, "x")) + 1;
                int sY = Math.abs(pos.getY() - PlatformHooks.getInt(stack, "y")) + 1;
                int sZ = Math.abs(pos.getZ() - PlatformHooks.getInt(stack, "z")) + 1;
                text.add(Component.literal("Selection: " + sX + " / " + sY + " / " + sZ).withStyle(ChatFormatting.GOLD));
            }
        }

        if (mc.player.isShiftKeyDown()) {
            BlockState state = world.getBlockState(pos);
            text.add(Component.literal("B: " + blockName(state) + ", M: " + metaOf(state)));
        }

        ILookOverlay.printGeneric(guiGraphics, stack.getHoverName(), 0xffff00, 0x404000, text);
    }
}
