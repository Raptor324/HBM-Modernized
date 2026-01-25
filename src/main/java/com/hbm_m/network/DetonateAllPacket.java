package com.hbm_m.network;

import java.util.function.Supplier;

import com.hbm_m.block.custom.explosives.IDetonatable;
import com.hbm_m.item.custom.grenades_and_activators.MultiDetonatorItem;
import com.hbm_m.item.custom.grenades_and_activators.MultiDetonatorItem.PointData;
import com.hbm_m.sound.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;

public class DetonateAllPacket {

    // Поле оставлено для совместимости с encode/decode TODO: Check
    private final CompoundTag tag;

    public DetonateAllPacket(CompoundTag tag) {
        this.tag = (tag == null) ? new CompoundTag() : tag;
    }

    public DetonateAllPacket() {
        this(new CompoundTag());
    }

    public static void encode(DetonateAllPacket msg, net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeNbt(msg.tag);
    }

    public static DetonateAllPacket decode(net.minecraft.network.FriendlyByteBuf buf) {
        return new DetonateAllPacket(buf.readNbt());
    }

    public static boolean handle(DetonateAllPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                handleDetonation(player);
            }
        });
        ctx.setPacketHandled(true);
        return true;
    }

    private static void handleDetonation(ServerPlayer player) {
        Level level = player.serverLevel();
        if (level == null) return;

        MultiDetonatorItem detonatorItem = null;

        var mainItem = player.getMainHandItem();
        var offItem = player.getOffhandItem();
        var detonatorStack = net.minecraft.world.item.ItemStack.EMPTY;

        if (mainItem.getItem() instanceof MultiDetonatorItem) {
            detonatorStack = mainItem;
            detonatorItem = (MultiDetonatorItem) mainItem.getItem();
        } else if (offItem.getItem() instanceof MultiDetonatorItem) {
            detonatorStack = offItem;
            detonatorItem = (MultiDetonatorItem) offItem.getItem();
        }

        if (detonatorStack.isEmpty() || detonatorItem == null) {
            player.displayClientMessage(
                    Component.literal("Multi-Detonator не найден!").withStyle(ChatFormatting.RED),
                    false
            );
            return;
        }

        int successCount = 0;
        final int pointsCount = detonatorItem.getMaxPoints();

        for (int i = 0; i < pointsCount; i++) {
            PointData pointData = detonatorItem.getPointData(detonatorStack, i);
            if (pointData == null || !pointData.hasTarget) continue;

            BlockPos targetPos = new BlockPos(pointData.x, pointData.y, pointData.z);

            if (!level.isLoaded(targetPos)) {
                player.displayClientMessage(
                        Component.literal(pointData.name + " ❌ Позиция не загружена").withStyle(ChatFormatting.RED),
                        false
                );
                continue;
            }

            BlockState state = level.getBlockState(targetPos);
            Block block = state.getBlock();

            if (!(block instanceof IDetonatable detonatable)) {
                player.displayClientMessage(
                        Component.literal(pointData.name + " Блок несовместим").withStyle(ChatFormatting.RED),
                        false
                );
                continue;
            }

            try {
                boolean success = detonatable.onDetonate(level, targetPos, state, player);
                if (success) {
                    player.displayClientMessage(
                            Component.literal(pointData.name + " Успешно активировано").withStyle(ChatFormatting.GREEN),
                            false
                    );

                    if (ModSounds.TOOL_TECH_BLEEP.isPresent()) {
                        SoundEvent soundEvent = ModSounds.TOOL_TECH_BLEEP.get();
                        level.playSound(null, player.getX(), player.getY(), player.getZ(), soundEvent,
                                player.getSoundSource(), 1.0F, 1.0F);
                    }

                    successCount++;
                } else {
                    player.displayClientMessage(
                            Component.literal(pointData.name + " Активация не удалась").withStyle(ChatFormatting.RED),
                            false
                    );
                }
            } catch (Exception e) {
                player.displayClientMessage(
                        Component.literal(pointData.name + " Ошибка при активации").withStyle(ChatFormatting.RED),
                        false
                );
                e.printStackTrace();
            }
        }

        player.displayClientMessage(
                Component.literal("Успешно активировано: " + successCount + "/" + pointsCount)
                        .withStyle(successCount == pointsCount ? ChatFormatting.GREEN : ChatFormatting.YELLOW),
                false
        );
    }
}
