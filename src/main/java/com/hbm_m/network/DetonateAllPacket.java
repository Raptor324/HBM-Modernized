package com.hbm_m.network;

import com.hbm_m.interfaces.IDetonatable;
import com.hbm_m.item.grenades_and_activators.MultiDetonatorItem;
import com.hbm_m.item.grenades_and_activators.MultiDetonatorItem.PointData;
import com.hbm_m.network.C2SPacket;
import com.hbm_m.sound.ModSounds;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class DetonateAllPacket implements C2SPacket {

    private final CompoundTag tag;

    public DetonateAllPacket(CompoundTag tag) {
        this.tag = (tag == null) ? new CompoundTag() : tag;
    }

    public DetonateAllPacket() {
        this(new CompoundTag());
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public static DetonateAllPacket decode(FriendlyByteBuf buf) {
        return new DetonateAllPacket(buf.readNbt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeNbt(tag);
    }

    // ── Handler ───────────────────────────────────────────────────────────────

    public static void handle(DetonateAllPacket msg, PacketContext context) {
        context.queue(() -> {
            // context.getPlayer() на C2S стороне — это ServerPlayer (отправитель)
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;
            handleDetonation(player);
        });
    }

    private static void handleDetonation(ServerPlayer player) {
        Level level = player.serverLevel();

        MultiDetonatorItem detonatorItem = null;
        ItemStack detonatorStack         = ItemStack.EMPTY;

        ItemStack mainItem = player.getMainHandItem();
        ItemStack offItem  = player.getOffhandItem();

        if (mainItem.getItem() instanceof MultiDetonatorItem m) {
            detonatorStack = mainItem;
            detonatorItem  = m;
        } else if (offItem.getItem() instanceof MultiDetonatorItem m) {
            detonatorStack = offItem;
            detonatorItem  = m;
        }

        if (detonatorStack.isEmpty() || detonatorItem == null) {
            player.displayClientMessage(
                    Component.literal("Multi-Detonator не найден!").withStyle(ChatFormatting.RED), false);
            return;
        }

        int successCount  = 0;
        int pointsCount   = detonatorItem.getMaxPoints();

        for (int i = 0; i < pointsCount; i++) {
            PointData pointData = detonatorItem.getPointData(detonatorStack, i);
            if (pointData == null || !pointData.hasTarget) continue;

            BlockPos  targetPos = new BlockPos(pointData.x, pointData.y, pointData.z);

            if (!level.isLoaded(targetPos)) {
                player.displayClientMessage(
                        Component.literal(pointData.name + " ❌ Позиция не загружена")
                                .withStyle(ChatFormatting.RED), false);
                continue;
            }

            BlockState state = level.getBlockState(targetPos);
            Block      block = state.getBlock();

            if (!(block instanceof IDetonatable detonatable)) {
                player.displayClientMessage(
                        Component.literal(pointData.name + " Блок несовместим")
                                .withStyle(ChatFormatting.RED), false);
                continue;
            }

            try {
                boolean success = detonatable.onDetonate(level, targetPos, state, player);
                if (success) {
                    player.displayClientMessage(
                            Component.literal(pointData.name + " Успешно активировано")
                                    .withStyle(ChatFormatting.GREEN), false);

                    if (ModSounds.TOOL_TECH_BLEEP.isPresent()) {
                        SoundEvent soundEvent = ModSounds.TOOL_TECH_BLEEP.get();
                        level.playSound(null,
                                player.getX(), player.getY(), player.getZ(),
                                soundEvent, player.getSoundSource(), 1.0F, 1.0F);
                    }
                    successCount++;
                } else {
                    player.displayClientMessage(
                            Component.literal(pointData.name + " Активация не удалась")
                                    .withStyle(ChatFormatting.RED), false);
                }
            } catch (Exception e) {
                player.displayClientMessage(
                        Component.literal(pointData.name + " Ошибка при активации")
                                .withStyle(ChatFormatting.RED), false);
                e.printStackTrace();
            }
        }

        player.displayClientMessage(
                Component.literal("Успешно активировано: " + successCount + "/" + pointsCount)
                        .withStyle(successCount == pointsCount
                                ? ChatFormatting.GREEN : ChatFormatting.YELLOW),
                false);
    }

    // ── Send helper ───────────────────────────────────────────────────────────

    public static void sendToServer() {
        ModPacketHandler.sendToServer(ModPacketHandler.DETONATE_ALL, new DetonateAllPacket());
    }
}