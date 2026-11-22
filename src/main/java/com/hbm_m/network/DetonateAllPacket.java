package com.hbm_m.network;

import com.hbm_m.api.bomb.IBomb.BombReturnCode;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.item.grenades_and_activators.DetonatorItem;
import com.hbm_m.item.grenades_and_activators.MultiDetonatorItem;
import com.hbm_m.item.grenades_and_activators.MultiDetonatorItem.PointData;
import com.hbm_m.main.MainRegistry;
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
        ItemStack detonatorStack = ItemStack.EMPTY;

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
            player.sendSystemMessage(Component.translatable("message.hbm_m.multi_detonator.not_found")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        int pointsCount = detonatorItem.getMaxPoints();
        int totalTargets = 0;
        for (int i = 0; i < pointsCount; i++) {
            PointData pointData = detonatorItem.getPointData(detonatorStack, i);
            if (pointData != null && pointData.hasTarget) {
                totalTargets++;
            }
        }

        if (totalTargets == 0) {
            player.sendSystemMessage(DetonatorItem.formatDetonatorMessage(detonatorItem,
                    Component.translatable("desc.misc.noPos").withStyle(ChatFormatting.RED)));
            return;
        }

        boolean hasValidBomb = false;
        int successCount = 0;

        for (int i = 0; i < pointsCount; i++) {
            PointData pointData = detonatorItem.getPointData(detonatorStack, i);
            if (pointData == null || !pointData.hasTarget) continue;

            BlockPos targetPos = new BlockPos(pointData.x, pointData.y, pointData.z);
            BombReturnCode ret = DetonatorItem.triggerDetonation(level, targetPos, player);

            if (ret != BombReturnCode.ERROR_NO_BOMB && ret != BombReturnCode.UNDEFINED) {
                hasValidBomb = true;
                if (ModClothConfig.get().enableExtendedLogging) {
                    MainRegistry.LOGGER.info("[DET] Tried to detonate block at {} / {} / {} by {}!",
                            targetPos.getX(), targetPos.getY(), targetPos.getZ(), player.getName().getString());
                }

                if (ret.wasSuccessful()) {
                    player.sendSystemMessage(DetonatorItem.formatDetonatorMessage(detonatorItem,
                            Component.translatable("message.hbm_m.multi_detonator.batch_success", pointData.name)
                                    .withStyle(ChatFormatting.GREEN)));
                    successCount++;
                } else {
                    player.sendSystemMessage(DetonatorItem.formatDetonatorMessage(detonatorItem,
                            Component.translatable("message.hbm_m.multi_detonator.batch_failed", pointData.name)
                                    .withStyle(ChatFormatting.RED)));
                }
            } else {
                player.sendSystemMessage(DetonatorItem.formatDetonatorMessage(detonatorItem,
                        Component.translatable("message.hbm_m.multi_detonator.batch_incompatible", pointData.name)
                                .withStyle(ChatFormatting.RED)));
            }
        }

        if (hasValidBomb && ModSounds.TOOL_TECH_BLEEP.isPresent()) {
            SoundEvent soundEvent = ModSounds.TOOL_TECH_BLEEP.get();
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    soundEvent, player.getSoundSource(), 1.0F, 1.0F);
        }

        player.sendSystemMessage(DetonatorItem.formatDetonatorMessage(detonatorItem,
                Component.translatable("message.hbm_m.multi_detonator.batch_summary", successCount, totalTargets)
                        .withStyle(successCount == totalTargets ? ChatFormatting.GREEN : ChatFormatting.YELLOW)));
    }

    // ── Send helper ───────────────────────────────────────────────────────────

    public static void sendToServer() {
        ModPacketHandler.sendToServer(ModPacketHandler.DETONATE_ALL, new DetonateAllPacket());
    }
}