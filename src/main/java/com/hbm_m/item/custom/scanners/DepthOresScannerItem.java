package com.hbm_m.item.custom.scanners;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DepthOresScannerItem extends Item {

    public DepthOresScannerItem(Properties properties) {
        super(properties.stacksTo(1));
    }
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, @Nullable List<Component> tooltip, TooltipFlag flag) {
        if (tooltip == null) return;
        tooltip.add(Component.translatable("tooltip.hbm_m.depth_ores_scanner.scans_chunks").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.hbm_m.depth_ores_scanner.deep_clusters").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.hbm_m.depth_ores_scanner.depth_warning").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.pass(stack);
        }

        BlockPos playerPos = player.blockPosition();

        // Новая проверка на высоту
        if (playerPos.getY() > -30) {
            player.displayClientMessage(
                    Component.translatable("message.hbm_m.depth_ores_scanner.invalid_height").withStyle(ChatFormatting.GRAY),
                    true
            );
            if (ModSounds.TOOL_TECH_BOOP.isPresent()) {
                SoundEvent soundEvent = ModSounds.TOOL_TECH_BOOP.get();
                level.playSound(null, player.getX(), player.getY(), player.getZ(), soundEvent, player.getSoundSource(), 1.0F, 1.0F);
            }
            return InteractionResultHolder.success(stack);
        }

        boolean depthStoneUnderPlayer = checkColumnUnderPlayer(level, playerPos);
        boolean depthStoneFound = scanChunkForDepthStone(level, playerPos);
        boolean depthStoneInAdjacentChunk = scanAdjacentChunksForDepthStone(level, playerPos);

        if (depthStoneUnderPlayer) {
            player.displayClientMessage(
                    Component.translatable("message.hbm_m.depth_ores_scanner.directly_below").withStyle(ChatFormatting.DARK_GREEN),
                    true
            );
            if (ModSounds.TOOL_TECH_BLEEP.isPresent()) {
                SoundEvent soundEvent = ModSounds.TOOL_TECH_BLEEP.get();
                level.playSound(null, player.getX(), player.getY(), player.getZ(), soundEvent, player.getSoundSource(), 1.0F, 1.0F);
            }
        } else if (depthStoneFound) {
            player.displayClientMessage(
                    Component.translatable("message.hbm_m.depth_ores_scanner.in_chunk").withStyle(ChatFormatting.GREEN),
                    true
            );
            if (ModSounds.TOOL_TECH_BOOP.isPresent()) {
                SoundEvent soundEvent = ModSounds.TOOL_TECH_BOOP.get();
                level.playSound(null, player.getX(), player.getY(), player.getZ(), soundEvent, player.getSoundSource(), 1.0F, 1.0F);
            }
        } else if (depthStoneInAdjacentChunk) {
            player.displayClientMessage(
                    Component.translatable("message.hbm_m.depth_ores_scanner.adjacent_chunk").withStyle(ChatFormatting.GOLD),
                    true
            );
            if (ModSounds.TOOL_TECH_BOOP.isPresent()) {
                SoundEvent soundEvent = ModSounds.TOOL_TECH_BOOP.get();
                level.playSound(null, player.getX(), player.getY(), player.getZ(), soundEvent, player.getSoundSource(), 1.0F, 1.0F);
            }
        } else {
            player.displayClientMessage(
                    Component.translatable("message.hbm_m.depth_ores_scanner.none_found").withStyle(ChatFormatting.RED),
                    true
            );
            if (ModSounds.TOOL_TECH_BOOP.isPresent()) {
                SoundEvent soundEvent = ModSounds.TOOL_TECH_BOOP.get();
                level.playSound(null, player.getX(), player.getY(), player.getZ(), soundEvent, player.getSoundSource(), 1.0F, 1.0F);
            }
        }

        return InteractionResultHolder.success(stack);
    }


    private void playSound(Level level, Player player, java.util.Optional<SoundEvent> sound) {
        sound.ifPresent(soundEvent -> level.playSound(null, player.getX(), player.getY(), player.getZ(),
                soundEvent, player.getSoundSource(), 1.0F, 1.0F));
    }

    private boolean scanChunkForDepthStone(Level level, BlockPos playerPos) {
        int chunkX = playerPos.getX() >> 4;
        int chunkZ = playerPos.getZ() >> 4;

        int minX = chunkX << 4;
        int maxX = minX + 15;
        int minZ = chunkZ << 4;
        int maxZ = minZ + 15;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {                for (int y = level.getMinBuildHeight(); y <= level.getMaxBuildHeight(); y++) {
                BlockPos pos = new BlockPos(x, y, z);
                if (isDepthStoneBlock(level, pos)) {
                    return true;
                }
            }
            }
        }
        return false;
    }

    private boolean scanAdjacentChunksForDepthStone(Level level, BlockPos playerPos) {
        int chunkX = playerPos.getX() >> 4;
        int chunkZ = playerPos.getZ() >> 4;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;

                int currentChunkX = chunkX + dx;
                int currentChunkZ = chunkZ + dz;

                int minX = currentChunkX << 4;
                int maxX = minX + 15;
                int minZ = currentChunkZ << 4;
                int maxZ = minZ + 15;

                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        for (int y = level.getMinBuildHeight(); y <= level.getMaxBuildHeight(); y++) {
                            BlockPos pos = new BlockPos(x, y, z);
                            if (isDepthStoneBlock(level, pos)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    // Проверка блока глубинных залежей по всей высоте столба блока под игроком
    private boolean checkColumnUnderPlayer(Level level, BlockPos playerPos) {
        int x = playerPos.getX();
        int z = playerPos.getZ();
        for (int y = level.getMinBuildHeight(); y <= level.getMaxBuildHeight(); y++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (isDepthStoneBlock(level, pos)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDepthStoneBlock(Level level, BlockPos pos) {
        Block block = level.getBlockState(pos).getBlock();
        return block == ModBlocks.DEPTH_STONE.get();
    }
}