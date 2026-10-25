package com.hbm_m.item.tool;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.api.energy.Nodespace;
import com.hbm_m.api.energy.PowerConductor;
import com.hbm_m.api.energy.PowerNet;
import com.hbm_m.interfaces.IMultiblockPart;
import com.hbm_m.item.ITooltipProvider;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** 1:1 {@code com.hbm.items.tool.ItemPowerNetTool}: Diagnose des Stromnetzes eines Kabels (Chat + Markierungen). */
public class ItemPowerNetTool extends Item implements ITooltipProvider {

    private static final int radius = 20;

    public ItemPowerNetTool(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level world = ctx.getLevel();
        Player player = ctx.getPlayer();
        BlockPos pos = ctx.getClickedPos();

        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof IMultiblockPart part && part.getControllerPos() != null) {
            pos = part.getControllerPos();
            te = world.getBlockEntity(pos);
        }

        if (world.isClientSide)
            return InteractionResult.SUCCESS;

        if (te instanceof PowerConductor && player != null) {

            Nodespace.PowerNode node = Nodespace.getNode((ServerLevel) world, pos);

            if (node != null && node.hasValidNet()) {

                PowerNet net = node.net;
                String id = Integer.toHexString(net.hashCode());

                player.sendSystemMessage(Component.literal("Start of diagnostic for network " + id).withStyle(ChatFormatting.GOLD));
                player.sendSystemMessage(Component.literal("Links: " + net.links.size()).withStyle(ChatFormatting.YELLOW));
                player.sendSystemMessage(Component.literal("Providers: " + net.providerEntries.size()).withStyle(ChatFormatting.YELLOW));
                player.sendSystemMessage(Component.literal("Receivers: " + net.receiverEntries.size()).withStyle(ChatFormatting.YELLOW));
                player.sendSystemMessage(Component.literal("End of diagnostic for network " + id).withStyle(ChatFormatting.GOLD));

                for (Nodespace.PowerNode link : net.links) {
                    for (BlockPos p : link.positions) {
                        CompoundTag data = new CompoundTag();
                        data.putString("type", "debug");
                        data.putInt("color", 0xffff00);
                        data.putFloat("scale", 0.5F);
                        data.putString("text", id);
                        com.hbm_m.particle.helper.IParticleCreator.sendPacket((ServerLevel) world, p.getX() + 0.5, p.getY() + 1.5, p.getZ() + 0.5, radius, data);
                    }
                }

            } else {
                player.sendSystemMessage(Component.literal("Error: No network found!").withStyle(ChatFormatting.RED));
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("Right-click cable to analyze the power net.").withStyle(ChatFormatting.RED));
        list.add(Component.literal("Links (cables, poles, etc.) are YELLOW").withStyle(ChatFormatting.RED));
        list.add(Component.literal("Subscribers (any receiver) are BLUE").withStyle(ChatFormatting.RED));
        list.add(Component.literal("Links with mismatching network info (BUGGED!) are RED").withStyle(ChatFormatting.RED));
        list.add(Component.literal("Displays stats such as link and subscriber count").withStyle(ChatFormatting.RED));
        list.add(Component.literal("Proxies are connection points for multiblock links (e.g. 4 for substations)").withStyle(ChatFormatting.RED));
        list.add(Component.literal("Particles only spawn in a " + radius + " block radius!").withStyle(ChatFormatting.RED));
    }
}
