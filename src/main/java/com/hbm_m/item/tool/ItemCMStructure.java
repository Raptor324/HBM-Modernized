package com.hbm_m.item.tool;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.google.gson.stream.JsonWriter;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.custom.CMBlocks;
import com.hbm_m.config.ConfigPaths;
import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.platform.PlatformHooks;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code ItemCMStructure}: Klick auf den Custom-Machine-Anker setzt den Kern, danach Position 1 und 2;
 * der dritte Klick schreibt alle Bloecke dazwischen (relativ zum Anker, nach dessen Ausrichtung gedreht) als
 * {@code components}-Liste nach {@code CMstructureOutput.txt} im Konfigordner.
 */
public class ItemCMStructure extends Item implements ILookOverlay, ITooltipProvider {

    private static final File file = ConfigPaths.configRoot().resolve("CMstructureOutput.txt").toFile();

    public ItemCMStructure(Properties properties) {
        super(properties.stacksTo(1));
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

    /** Alter Blockname + Metadatum: CM-Bloecke als {@code hbm:tile.cm_*} mit Enum-Ordinal (wie sie {@code CustomMachineConfigJSON} liest). */
    private static Object[] legacyName(BlockState state) {
        Block b = state.getBlock();
        Object[] cm = cmLookup(ModBlocks.CM_BLOCK, "cm_block", b);
        if (cm == null) cm = cmLookup(ModBlocks.CM_SHEET, "cm_sheet", b);
        if (cm == null) cm = cmLookup(ModBlocks.CM_TANK, "cm_tank", b);
        if (cm == null) cm = cmLookup(ModBlocks.CM_PORT, "cm_port", b);
        if (cm == null) cm = cmLookup(ModBlocks.CM_ENGINE, "cm_engine", b);
        if (cm == null) cm = cmLookup(ModBlocks.CM_CIRCUIT, "cm_circuit", b);
        if (cm != null) return cm;
        // Port hat keine Metadaten mehr: Registry-ID, Meta 0
        return new Object[] { BuiltInRegistries.BLOCK.getKey(b).toString(), 0 };
    }

    @Nullable
    private static <E extends Enum<E>> Object[] cmLookup(Map<E, RegistrySupplier<Block>> map, String name, Block b) {
        for (Map.Entry<E, RegistrySupplier<Block>> e : map.entrySet()) {
            if (e.getValue().get() == b) return new Object[] { "hbm:tile." + name, e.getKey().ordinal() };
        }
        return null;
    }

    public static void writeToFile(File config, ItemStack stack, Level world) {
        int anchorX = PlatformHooks.getInt(stack, "anchorX");
        int anchorY = PlatformHooks.getInt(stack, "anchorY");
        int anchorZ = PlatformHooks.getInt(stack, "anchorZ");
        int x1 = PlatformHooks.getInt(stack, "x1");
        int y1 = PlatformHooks.getInt(stack, "y1");
        int z1 = PlatformHooks.getInt(stack, "z1");
        int x2 = PlatformHooks.getInt(stack, "x2");
        int y2 = PlatformHooks.getInt(stack, "y2");
        int z2 = PlatformHooks.getInt(stack, "z2");
        // ForgeDirection.getOrientation(meta des Ankers)
        BlockState anchorState = world.getBlockState(new BlockPos(anchorX, anchorY, anchorZ));
        Direction dir = anchorState.hasProperty(CMBlocks.Anchor.FACING) ? anchorState.getValue(CMBlocks.Anchor.FACING) : Direction.DOWN;
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);

        try {
            config.getParentFile().mkdirs();
            JsonWriter writer = new JsonWriter(new FileWriter(config));
            writer.setIndent("  ");
            writer.beginObject();
            writer.name("components").beginArray();

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {

                        int compY = y - anchorY;
                        int compX = 0;
                        int compZ = 0;

                        if (dir == Direction.SOUTH) {
                            compX = anchorX - x;
                            compZ = anchorZ - z;
                        }
                        if (dir == Direction.NORTH) {
                            compX = x - anchorX;
                            compZ = z - anchorZ;
                        }

                        if (dir == Direction.WEST) {
                            compZ = x - anchorX;
                            compX = anchorZ - z;
                        }
                        if (dir == Direction.EAST) {
                            compZ = anchorX - x;
                            compX = z - anchorZ;
                        }

                        if (x == anchorX && y == anchorY && z == anchorZ) continue;
                        BlockState state = world.getBlockState(new BlockPos(x, y, z));
                        if (state.isAir()) continue;
                        Object[] legacy = legacyName(state);

                        writer.beginObject().setIndent("");
                        writer.name("block").value((String) legacy[0]);
                        writer.name("x").value(compX);
                        writer.name("y").value(compY);
                        writer.name("z").value(compZ);
                        writer.name("metas").beginArray().value((int) (Integer) legacy[1]).endArray();
                        writer.endObject().setIndent("  ");
                    }
                }
            }
            writer.endArray();
            writer.endObject();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level world = ctx.getLevel();
        ItemStack stack = ctx.getItemInHand();
        BlockPos pos = ctx.getClickedPos();
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        if (world.getBlockState(pos).is(ModBlocks.CM_ANCHOR.get())) {
            setAnchor(stack, x, y, z);
            return InteractionResult.sidedSuccess(world.isClientSide);
        }

        if (getAnchor(stack) == null) {
            return InteractionResult.PASS;
        }
        if (!PlatformHooks.contains(stack, "x1")) {
            PlatformHooks.putInt(stack, "x1", x);
            PlatformHooks.putInt(stack, "y1", y);
            PlatformHooks.putInt(stack, "z1", z);
        } else if (!PlatformHooks.contains(stack, "x2")) {
            PlatformHooks.putInt(stack, "x2", x);
            PlatformHooks.putInt(stack, "y2", y);
            PlatformHooks.putInt(stack, "z2", z);
        } else {
            writeToFile(file, stack, world);
            PlatformHooks.remove(stack, "x1");
            PlatformHooks.remove(stack, "y1");
            PlatformHooks.remove(stack, "z1");
            PlatformHooks.remove(stack, "x2");
            PlatformHooks.remove(stack, "y2");
            PlatformHooks.remove(stack, "z2");
        }
        return InteractionResult.sidedSuccess(world.isClientSide);
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("Click Custom Machine Structure Positioning Anchor to").withStyle(ChatFormatting.YELLOW));
        list.add(Component.literal("Confirm the location of the custom machine core block.").withStyle(ChatFormatting.YELLOW));
        list.add(Component.literal("Output all blocks between Position1 and Position2 with").withStyle(ChatFormatting.YELLOW));
        list.add(Component.literal("metadata to \"CMstructureOutput.txt\" in hbmConfig.").withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public void printHook(GuiGraphics guiGraphics, Level world, BlockPos pos) {
        ItemStack stack = Minecraft.getInstance().player.getMainHandItem();
        List<Component> text = new ArrayList<>();

        BlockPos anchor = getAnchor(stack);

        if (anchor == null) {

            text.add(Component.literal("No Anchor").withStyle(ChatFormatting.RED));
        } else {
            int anchorX = PlatformHooks.getInt(stack, "anchorX");
            int anchorY = PlatformHooks.getInt(stack, "anchorY");
            int anchorZ = PlatformHooks.getInt(stack, "anchorZ");
            text.add(Component.literal("Anchor: " + anchorX + " / " + anchorY + " / " + anchorZ).withStyle(ChatFormatting.GOLD));
            if (PlatformHooks.contains(stack, "x1")) {
                int x1 = PlatformHooks.getInt(stack, "x1");
                int y1 = PlatformHooks.getInt(stack, "y1");
                int z1 = PlatformHooks.getInt(stack, "z1");

                text.add(Component.literal("Position1: " + x1 + " / " + y1 + " / " + z1).withStyle(ChatFormatting.YELLOW));
            }
            if (PlatformHooks.contains(stack, "x2")) {
                int x2 = PlatformHooks.getInt(stack, "x2");
                int y2 = PlatformHooks.getInt(stack, "y2");
                int z2 = PlatformHooks.getInt(stack, "z2");
                text.add(Component.literal("Position2: " + x2 + " / " + y2 + " / " + z2).withStyle(ChatFormatting.YELLOW));
            }
        }

        ILookOverlay.printGeneric(guiGraphics, stack.getHoverName(), 0xffff00, 0x404000, text);
    }
}
