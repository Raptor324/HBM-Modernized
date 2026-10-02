package com.hbm_m.item.nuclear;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.MachinePWRControllerBlock;
import com.hbm_m.block.machines.PWRBlock;
import com.hbm_m.blockentity.machines.PWRBlockEntity;
import com.hbm_m.blockentity.machines.PWRControllerBlockEntity;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.PWRPrinterScanPacket;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 {@code ItemPWRPrinter}: auf einen PWR-Controller angewandt flutet er die Traeger ab, schickt Grenzen, Richtung und
 * Bauteile an den Client und oeffnet dort den Schnittdrucker ({@code GUIScreenSlicePrinter}), der jede Lage als PNG nach
 * {@code .minecraft/printer/} schreibt.
 */
public class PWRFuelPrinterItem extends Item implements com.hbm_m.item.ITooltipProvider {

    private int x1, y1, z1;
    private int x2, y2, z2;
    private Direction dir;

    private final Set<BlockPos> fill = new HashSet<>();

    /** Original: {@code whitelist} - nur Traeger und Controller werden gedruckt. */
    public static Set<Block> whitelist() {
        Set<Block> set = new HashSet<>();
        set.add(ModBlocks.PWR_BLOCK.get());
        set.add(ModBlocks.PWR_CONTROLLER.get());
        return set;
    }

    public PWRFuelPrinterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockEntity tile = world.getBlockEntity(context.getClickedPos());
        if (!(tile instanceof PWRControllerBlockEntity pwr)) return InteractionResult.PASS;
        if (world.isClientSide) return InteractionResult.SUCCESS;

        if (context.getPlayer() instanceof ServerPlayer player) syncAndScreenshot(world, pwr, player);
        return InteractionResult.SUCCESS;
    }

    public void syncAndScreenshot(Level world, PWRControllerBlockEntity pwr, ServerPlayer player) {
        findBounds(world, pwr);

        int sizeX = x2 - x1 + 1;
        int sizeY = y2 - y1 + 1;
        int sizeZ = z2 - z1 + 1;

        int[] blockSync = new int[sizeX * sizeY * sizeZ];
        int i = 0;

        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    BlockEntity tile = world.getBlockEntity(new BlockPos(x, y, z));
                    if (tile instanceof PWRBlockEntity b && b.block != null) {
                        blockSync[i] = BuiltInRegistries.BLOCK.getId(b.block);
                    }
                    i++;
                }
            }
        }

        ModPacketHandler.sendToPlayer(player, ModPacketHandler.PWR_PRINTER_SCAN,
                new PWRPrinterScanPacket(x1, y1, z1, x2, y2, z2, dir.get3DDataValue(), blockSync));
    }

    public void findBounds(Level world, PWRControllerBlockEntity pwr) {
        BlockPos p = pwr.getBlockPos();
        dir = world.getBlockState(p).getValue(MachinePWRControllerBlock.FACING).getOpposite();

        fill.clear();
        fill.add(p);
        x1 = x2 = p.getX();
        y1 = y2 = p.getY();
        z1 = z2 = p.getZ();
        floodFill(world, p.relative(dir));
    }

    public void floodFill(Level world, BlockPos pos) {
        if (fill.contains(pos)) return;

        if (world.getBlockState(pos).getBlock() instanceof PWRBlock) {
            fill.add(pos);

            x1 = Math.min(x1, pos.getX());
            y1 = Math.min(y1, pos.getY());
            z1 = Math.min(z1, pos.getZ());
            x2 = Math.max(x2, pos.getX());
            y2 = Math.max(y2, pos.getY());
            z2 = Math.max(z2, pos.getZ());

            floodFill(world, pos.offset(1, 0, 0));
            floodFill(world, pos.offset(-1, 0, 0));
            floodFill(world, pos.offset(0, 1, 0));
            floodFill(world, pos.offset(0, -1, 0));
            floodFill(world, pos.offset(0, 0, 1));
            floodFill(world, pos.offset(0, 0, -1));
        }
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Use on a constructed PWR controller to generate construction diagrams"));
    }
}
