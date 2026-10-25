package com.hbm_m.item.tool;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.ModMaterials;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * 1:1 {@code com.hbm.items.tool.ItemSurveyScanner}: tastet ein 11x11-Raster (Abstand 5) von Spielerhoehe+15 bis y=2 ab.
 * Die unterste Lage des Ports ist {@code minBuildHeight} statt 0 (dort liegt das Grundgesteinserz des Originals, das im
 * Port noch fehlt). Auf Berylliumblock mit Verschraenkungskit: ab ins Ende.
 */
public class ItemSurveyScanner extends Item {

    public ItemSurveyScanner(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!world.isClientSide) {
            int x = Mth.floor(player.getX());
            int y = Mth.floor(player.getY());
            int z = Mth.floor(player.getZ());

            boolean hasOil = false;
            boolean hasColtan = false;
            boolean hasBedrockOil = false;
            boolean hasDepth = false;
            boolean hasSchist = false;
            boolean hasAussie = false;

            com.hbm_m.blockentity.nature.OreBedrockBlockEntity tile = null;

            int bottom = world.getMinBuildHeight() + 1;

            for (int a = -5; a <= 5; a++) {
                for (int b = -5; b <= 5; b++) {
                    for (int i = y + 15; i > bottom; i -= 2) {

                        Block block = world.getBlockState(new BlockPos(x + a * 5, i, z + b * 5)).getBlock();

                        //wow, this sucks!
                        if (block == ModBlocks.ORE_OIL.get()) hasOil = true;
                        else if (block == ModBlocks.COLTAN_ORE.get()) hasColtan = true;
                        else if (block == ModBlocks.ORE_BEDROCK_OIL.get()) hasBedrockOil = true;
                        else if (block == ModBlocks.STONE_DEPTH.get()) hasDepth = true;
                        else if (block == ModBlocks.STONE_DEPTH_NETHER.get()) hasDepth = true;
                        else if (block == ModBlocks.STONE_GNEISS.get()) hasSchist = true;
                        else if (block == ModBlocks.ORE_AUSTRALIUM.get()) hasAussie = true;
                    }

                    // Original: Bedrock-Erz in Hoehe 0 (Port: unterste Weltschicht)
                    BlockPos bedrock = new BlockPos(x + a * 2, world.getMinBuildHeight(), z + b * 2);
                    if (world.getBlockState(bedrock).getBlock() == ModBlocks.ORE_BEDROCK.get()
                            && world.getBlockEntity(bedrock) instanceof com.hbm_m.blockentity.nature.OreBedrockBlockEntity ore) {
                        tile = ore;
                    }
                }
            }

            if (hasOil) player.sendSystemMessage(Component.literal("Found OIL!").withStyle(ChatFormatting.BLACK));
            if (hasBedrockOil) player.sendSystemMessage(Component.literal("Found BEDROCK OIL!").withStyle(ChatFormatting.BLACK));
            if (hasColtan) player.sendSystemMessage(Component.literal("Found COLTAN!").withStyle(ChatFormatting.GOLD));
            if (hasDepth) player.sendSystemMessage(Component.literal("Found DEPTH ROCK!").withStyle(ChatFormatting.GRAY));
            if (hasSchist) player.sendSystemMessage(Component.literal("Found SCHIST!").withStyle(ChatFormatting.DARK_AQUA));
            if (hasAussie) player.sendSystemMessage(Component.literal("Found AUSTRALIUM!").withStyle(ChatFormatting.YELLOW));
            if (tile != null && !tile.resource.isEmpty()) player.sendSystemMessage(Component.literal("Found BEDROCK ORE for ").append(tile.resource.getHoverName()).append("!").withStyle(ChatFormatting.RED));
        }

        player.swing(hand);
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        Level world = ctx.getLevel();
        if (player != null && ModBlocks.hasIngotBlock(ModMaterials.BERYLLIUM)
                && world.getBlockState(ctx.getClickedPos()).is(ModBlocks.getIngotBlock(ModMaterials.BERYLLIUM).get())
                && player.getInventory().contains(new ItemStack(ModItems.ENTANGLEMENT_KIT.get()))) {
            if (world instanceof ServerLevel server) {
                ServerLevel end = server.getServer().getLevel(Level.END);
                //? if < 1.21.1 {
                if (end != null) player.changeDimension(end);
                //?} else {
                /*// 1.21.1: Ziel wie der Vanilla-Endportal-Uebergang (Obsidianplattform am End-Spawnpunkt)
                if (end != null) {
                    net.minecraft.world.phys.Vec3 v = ServerLevel.END_SPAWN_POINT.getBottomCenter();
                    net.minecraft.world.level.levelgen.feature.EndPlatformFeature.createEndPlatform(end, net.minecraft.core.BlockPos.containing(v).below(), true);
                    if (player instanceof net.minecraft.server.level.ServerPlayer) v = v.subtract(0.0, 1.0, 0.0);
                    player.changeDimension(new net.minecraft.world.level.portal.DimensionTransition(end, v, player.getDeltaMovement(),
                            net.minecraft.core.Direction.WEST.toYRot(), player.getXRot(),
                            net.minecraft.world.level.portal.DimensionTransition.PLAY_PORTAL_SOUND.then(net.minecraft.world.level.portal.DimensionTransition.PLACE_PORTAL_TICKET)));
                }
                *///?}
            }
            return InteractionResult.sidedSuccess(world.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
