package com.hbm_m.item.nuclear;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.PWRPartBlock;
import com.hbm_m.blockentity.machines.PWRControllerBlockEntity;
import com.hbm_m.blockentity.machines.PWRPartBlockEntity.Kind;
import com.hbm_m.network.PWRPrinterScanPacket;
import com.hbm_m.network.ModPacketHandler;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * PWR Printer. 1:1 in purpose to {@code com.hbm.items.machine.ItemPWRPrinter} (1.7.10): right-click
 * an assembled PWR controller to scan its structure and open a per-layer construction diagram
 * (see {@code GUIPWRPrinter}). The original rendered a rotatable 3D slice viewer; this port shows
 * the same information (which block goes where, layer by layer) as a flat 2D grid per Y-layer
 * with next/prev navigation instead - the only UX-only simplification in the PWR feature (the
 * reactor mechanics themselves are a 1:1 port).
 */
public class PWRFuelPrinterItem extends Item implements com.hbm_m.item.ITooltipProvider {

    private static final int MAX_SIZE = 4096;

    public PWRFuelPrinterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        if (!(level.getBlockEntity(pos) instanceof PWRControllerBlockEntity controller) || !controller.assembled) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        scanAndSend(level, pos, serverPlayer);
        return InteractionResult.CONSUME;
    }

    private void scanAndSend(Level level, BlockPos controllerPos, ServerPlayer player) {
        Map<BlockPos, Kind> found = new HashMap<>();
        found.put(controllerPos, null);
        floodFill(level, controllerPos, found);

        int x1 = Integer.MAX_VALUE, y1 = Integer.MAX_VALUE, z1 = Integer.MAX_VALUE;
        int x2 = Integer.MIN_VALUE, y2 = Integer.MIN_VALUE, z2 = Integer.MIN_VALUE;
        for (BlockPos p : found.keySet()) {
            x1 = Math.min(x1, p.getX()); y1 = Math.min(y1, p.getY()); z1 = Math.min(z1, p.getZ());
            x2 = Math.max(x2, p.getX()); y2 = Math.max(y2, p.getY()); z2 = Math.max(z2, p.getZ());
        }

        int sizeX = x2 - x1 + 1, sizeY = y2 - y1 + 1, sizeZ = z2 - z1 + 1;
        byte[] grid = new byte[sizeX * sizeY * sizeZ];
        int bx1 = x1, by1 = y1, bz1 = z1, bsx = sizeX, bsy = sizeY;
        for (Map.Entry<BlockPos, Kind> entry : found.entrySet()) {
            BlockPos p = entry.getKey();
            int idx = (p.getX() - bx1) + (p.getY() - by1) * bsx + (p.getZ() - bz1) * bsx * bsy;
            Kind k = entry.getValue();
            grid[idx] = (byte) (k == null ? 16 /* controller marker */ : k.ordinal() + 1);
        }

        ModPacketHandler.sendToPlayer(player, ModPacketHandler.PWR_PRINTER_SCAN,
                new PWRPrinterScanPacket(sizeX, sizeY, sizeZ, grid));
    }

    private void floodFill(Level level, BlockPos pos, Map<BlockPos, Kind> found) {
        if (found.size() >= MAX_SIZE) return;

        for (Direction dir : Direction.values()) {
            BlockPos n = pos.relative(dir);
            if (found.containsKey(n)) continue;

            Block block = level.getBlockState(n).getBlock();
            if (block instanceof PWRPartBlock partBlock) {
                found.put(n, partBlock.getKind());
                floodFill(level, n, found);
            }
        }
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Use on an assembled PWR controller to generate construction diagrams")
                .withStyle(ChatFormatting.GRAY));
    }
}
